/*
 * JimpleHelper.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.util;

import com.google.common.collect.Sets;
import presto.android.Hierarchy;
import presto.android.MethodNames;
import presto.android.gui.JimpleUtil;
import presto.android.gui.graph.NAllocNode;
import presto.android.gui.graph.NFieldNode;
import presto.android.gui.graph.NNode;
import presto.android.gui.graph.NStringBuilderNode;
import presto.android.gui.wtg.flowgraph.FlowgraphRebuilder;
import presto.android.gui.wtg.util.QueryHelper;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.Units;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

import java.util.*;

public class JimpleHelper {

    public static boolean isRequestPermissionSig(Stmt unit) {
        if (unit.containsInvokeExpr()) {
            SootMethod method = unit.getInvokeExpr().getMethod();
            String sig = method.getSignature();
            if (sig.contentEquals(MethodNames.requestRequestPermissionsSig1)
                    || sig.contentEquals(MethodNames.requestRequestPermissionsSig2)
                    || sig.contentEquals(MethodNames.requestRequestPermissionsFragmentSig)) {
                return true;
            } else {
                String packageName = method.getDeclaringClass().getPackageName();
                if (method.getParameterCount() == 3 && packageName.startsWith("android.support.v4")) { // android.app.Activity,java.lang.String[],int
                    if (method.getParameterType(0).toString().contentEquals("android.app.Activity")
                    && method.getParameterType(1).toString().contentEquals("java.lang.String[]")
                    && method.getParameterType(2).toString().contentEquals("int")) {
                        return true;
                    }

                } else if (method.getParameterCount() == 2
                        && (packageName.startsWith("android.app.Activity") || packageName.startsWith("android.app.Fragment"))) { // java.lang.String[],int
                    if (method.getParameterType(0).toString().contentEquals("java.lang.String[]")
                            && method.getParameterType(1).toString().contentEquals("int")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static SootClass getDialogFragmentShow(Stmt stmt) {
        if (!stmt.containsInvokeExpr()) return null;
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        SootMethod method = invokeExpr.getMethod();
        if (method == null) return null;
        if (method.getDeclaringClass() == Scene.v().getSootClass("android.app.DialogFragment")) {
            String sig = method.getSignature();
            if (sig.contentEquals(MethodNames.dialogFragmentShowSig1)
                    || sig.contentEquals(MethodNames.dialogFragmentShowSig2)) {
                return invokeExpr.getMethodRef().getDeclaringClass();
            }
        }

        return null;
    }

    public static String getExternalStoragePermissionAPI(Stmt stmt, SootMethod context, FlowgraphRebuilder rebuilder, QueryHelper queryHelper) {
        SootMethod method =  stmt.getInvokeExpr().getMethod();
        String sig = method.getSignature();
        if (sig != null && sig.startsWith("<java.io.File: void <init>")) {
            for (Value arg : stmt.getInvokeExpr().getArgs()) {
                NNode argNode = rebuilder.lookupNode(arg);
                Iterator<NNode> allNodes = queryHelper.allVariableValues(argNode).iterator();
                while (allNodes.hasNext()) {
                    NNode fNode = allNodes.next();
                    if (fNode instanceof NFieldNode) {
                        Iterator<NNode> allVariableNode = queryHelper.allVariableValues(fNode).iterator();
                        while (allVariableNode.hasNext()) {
                            NNode vNode = allVariableNode.next();
                            if (vNode instanceof NStringBuilderNode) {
                                for (String value : ((NStringBuilderNode) vNode).possibleValues) {
                                    if (value.startsWith("<android.os.Environment:")) {
                                       return value;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }


    public static SootClass getSootClass(Value value, Stmt stmt, SootMethod method) {
        UnitGraph g = new ExceptionalUnitGraph(method.getActiveBody());
        SmartLocalDefs ld = new SmartLocalDefs(g, new SimpleLiveLocals(g));

        List<Unit> units = ld.getDefsOfAt((Local)value, stmt);
        if (units != null && units.size() > 0) {
            Unit localDef = units.get(0);
            if (localDef instanceof JAssignStmt) {
                Value lefOp = ((JAssignStmt) localDef).leftBox.getValue();
                Value rightOp = ((JAssignStmt) localDef).rightBox.getValue();

                if (rightOp instanceof JNewExpr) {
                    return ((JNewExpr) rightOp).getBaseType().getSootClass();
                } else if (rightOp instanceof JStaticInvokeExpr) {
                    if (((JStaticInvokeExpr) rightOp).getMethodRef().getName().contentEquals("newInstance")) {
                        return ((JStaticInvokeExpr) rightOp).getMethodRef().getDeclaringClass();
                    }
                }
            } else if (localDef instanceof JIdentityStmt) {
                return ((RefType)(((JIdentityStmt)localDef).rightBox.getValue().getType())).getSootClass();
            }
        }

        return null;
    }

    public static Integer getResourceId(Hierarchy hier, SootClass sootClass, String methodSig, String invokeSig) {
        SootMethod matchedMethod = null;
        SootClass matchedClass = hier.matchForVirtualDispatch(methodSig, sootClass);
        if (matchedClass != null && matchedClass.isApplicationClass()) {
            matchedMethod= matchedClass.getMethod(methodSig);
        }
        if (matchedMethod == null) {
            return null;
        }

        LinkedList<SootMethod> workList = new LinkedList<SootMethod>();
        Set<SootMethod> memberSet = new HashSet<SootMethod>();
        workList.add(matchedMethod);
        memberSet.add(matchedMethod);

        while (!workList.isEmpty()) {
            SootMethod currentMethod = workList.remove();
            Body body = currentMethod.getActiveBody();
            Iterator<Unit> units = body.getUnits().iterator();
            while (units.hasNext()) {
                Stmt stmt = (Stmt)units.next();

                if (stmt.containsInvokeExpr()) {
                    SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
                    String sig = invokeMethod.getSubSignature();
                    if (sig.contentEquals(invokeSig)) {
                        Value prefHeaderValue = stmt.getInvokeExpr().getUseBoxes().get(0).getValue();
                        if (prefHeaderValue instanceof IntConstant) {
                            return ((IntConstant) prefHeaderValue).value;
                        }
                    } else if (invokeMethod.getDeclaringClass().isApplicationClass()
                            && memberSet.add(invokeMethod)
                            && invokeMethod.hasActiveBody()) {
                        workList.add(invokeMethod);
                    }
                }
            }
        }
        return null;
    }


    public static SootClass findAsyncTaskClassAssignedToField(SootMethod currentCxt, Stmt currentStmt) {
        UnitGraph g = new BriefUnitGraph(currentCxt.getActiveBody());
        SmartLocalDefs ld = new SmartLocalDefs(g, new SimpleLiveLocals(g));

        try {
            List<Unit> localUnits = ld.getDefsOfAt((Local) ((VirtualInvokeExpr)currentStmt.getInvokeExpr()).getBase(), currentStmt);
            if (localUnits == null || localUnits.size() < 0) {
                return null;
            }
            Unit refLocal = localUnits.get(0);

            if (refLocal instanceof AssignStmt) {
                Value rigValue = ((AssignStmt) refLocal).getRightOpBox().getValue();

                if (rigValue instanceof InstanceFieldRef) {
                    SootField field = ((JInstanceFieldRef)(rigValue)).getField();

                    LinkedList<SootMethod> worklist = new LinkedList<SootMethod>();
                    worklist.add(currentCxt);

                    while (!worklist.isEmpty()) {
                        SootMethod currentMethod = worklist.remove();
                        UnitGraph currentGraph = new BriefUnitGraph(currentMethod.getActiveBody());
                        SmartLocalDefs smartLocalDefs = new SmartLocalDefs(currentGraph, new SimpleLiveLocals(currentGraph));

                        Body body = currentMethod.getActiveBody();
                        Iterator<Unit> units = body.getUnits().iterator();

                        while (units.hasNext()) {
                            Stmt stmt = (Stmt) units.next();

                            if (!(stmt instanceof AssignStmt)) {
                                continue;
                            }

                            Value lefLocal = ((AssignStmt) stmt).getLeftOpBox().getValue();
                            Value rigLocal = ((AssignStmt) stmt).getRightOpBox().getValue();
                            if (lefLocal instanceof InstanceFieldRef) {
                                SootField currentField = ((JInstanceFieldRef)(lefLocal)).getField();

                                if (field == currentField) {
                                    System.out.println(field);
                                    List<Unit> list = smartLocalDefs.getDefsOfAt((Local)rigLocal, stmt);
                                    if (list == null || list.size() < 0) {
                                        return null;
                                    }
                                    Stmt local = (Stmt) list.get(0);
                                    return ((RefType)(((JAssignStmt) local).getRightOpBox().getValue().getType())).getSootClass();
                                }
                            }
                        }

                        SootClass sootClass = currentCxt.getDeclaringClass();
                        worklist.add(sootClass.getMethod(MethodNames.onActivityCreateSubSig));
                        worklist.add(sootClass.getMethod(MethodNames.initSig));
                    }
                } else if (rigValue instanceof NewExpr) {
                    return ((RefType)(rigValue).getType()).getSootClass();
                }
            }
        } catch (Exception ex) {
            System.out.println("[WARNING AsyncTask]:" + ex.getMessage());
        }

        return null;
    }

    public static String getContentProviderFieldSig(Stmt unit, SootMethod context) {
        Value rightOp = ((JAssignStmt)unit).getRightOp();

        return rightOp.toString();
    }

    public static String getContentProviderUri(Stmt unit, SootMethod context) {
        try {
            // UnitGraph g = new BriefUnitGraph(context.getActiveBody());
            // SmartLocalDefs ld = new SmartLocalDefs(g, new SimpleLiveLocals(g));
            InvokeExpr expr = unit.getInvokeExpr();
            Value arg = expr.getArg(0);

            if (arg instanceof StringConstant) {
                return ((StringConstant) arg).value;
            }

//            // Get where the argument was declared
//            List<Unit> unitList = ld.getDefsOfAt((Local) arg, unit);
//
//            if (unitList.size() > 0) {
//                List<ValueBox> valueBoxList = unitList.get(0).getUseBoxes();
//                if (valueBoxList.size() > 0) {
//
//                    Value directContentProviderFieldValue = valueBoxList.get(0).getValue();
//
//                    if (directContentProviderFieldValue.getUseBoxes().size() > 0) {
//                        Value uriStringContentProviderValue = directContentProviderFieldValue.getUseBoxes().get(0).getValue();
//
//                        return uriStringContentProviderValue.toString();
//                    }
//                }
//            }
        } catch (Exception ex) {
            System.out.println("[ERROR]:" + ex.getMessage());
            System.out.println("[ERROR]:" + context.getActiveBody().toString());
        }

        return null;
    }

    public static Set<String> getPermissionsFromRequestPermissions(Stmt unit, SootMethod method, FlowgraphRebuilder rebuilder, QueryHelper queryHelper, JimpleUtil jimpleUtil) {
        Set<String> permissions = Sets.newHashSet();
       /* UnitGraph g = new BriefUnitGraph(method.getActiveBody());
        SmartLocalDefs ld = new SmartLocalDefs(g, new SimpleLiveLocals(g));

        InvokeExpr expr = unit.getInvokeExpr();
        Value arg = expr.getArg(1);
        List<Unit> units = ld.getDefsOfAt((Local) arg, unit);
        if (units.size() > 0) {
            Stmt defUnit = (Stmt) units.get(0);
            String rightOpType = ((JAssignStmt) defUnit).getRightOp().getType().toString();
            if (rightOpType.contentEquals("java.lang.String[]")) {
                permissions = getAssignedPermissions(defUnit, unit, method);
            }
        }*/
        InvokeExpr expr = unit.getInvokeExpr();
        Value arg = expr.getArg(1);

        if (arg instanceof IntConstant) {
            arg = expr.getArg(0);
        }

        NNode argNode = rebuilder.lookupNode(arg);
        Iterator<NNode> allNodes = queryHelper.allVariableValues(argNode).iterator();
        while (allNodes.hasNext()) {
            NNode node = allNodes.next();
            if (node instanceof NAllocNode) {
                String type = (((NAllocNode) node).e).getType().toString();
                if (type.contentEquals("java.lang.String[]")) {
                    Stmt stmt =  jimpleUtil.lookup(((NAllocNode) node).e);
                    SootMethod sootMethod = jimpleUtil.lookup(stmt);
                    permissions.addAll(getAssignedPermissions(stmt, sootMethod));
                }
            }
        }

        return permissions;
    }

    private static Set<String> getAssignedPermissions(Stmt fromUnit, SootMethod method) {
        Set<String> values = Sets.newHashSet();
        Body body = method.getActiveBody();
        Iterator<Unit> units = body.getUnits().iterator();
        Value fromLocal = ((JAssignStmt)fromUnit).leftBox.getValue();

        while (units.hasNext()) {
            Stmt currentStmt = (Stmt)units.next();
            if (currentStmt != fromUnit) continue;

            while (units.hasNext()) {
                Stmt nextStmt = (Stmt)units.next();
                if (nextStmt instanceof JAssignStmt) {
                    Value leftLocal = ((JAssignStmt) nextStmt).leftBox.getValue();
                    if (leftLocal instanceof JArrayRef) {
                        Value currentLocal = ((JArrayRef)leftLocal).getBase();
                        if (currentLocal != fromLocal) {
                            return values;
                        }
                        Value value = ((JAssignStmt) nextStmt).getRightOp();
                        if (value instanceof StringConstant) {
                            String name = ((StringConstant) value).value;
                            if (name.contains("android.permission.")) {
                                values.add(name);
                            }
                        }
                    } else {
                        return values;
                    }
                }
            }

        }
        return values;
    }
}
