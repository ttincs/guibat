/*
 * ConstantPreferenceAnalysis.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.permission;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import presto.android.Hierarchy;
import presto.android.Logger;
import presto.android.gui.GUIAnalysisOutput;
import presto.android.gui.JimpleUtil;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.graph.NPreferenceIdNode;
import presto.android.gui.wtg.flowgraph.AndroidCallGraph;
import presto.android.gui.wtg.flowgraph.AndroidCallGraph.Edge;
import presto.android.gui.wtg.flowgraph.FlowgraphRebuilder;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantPreferenceAnalysis {
    // constants
    private final Object ANY = new Object();

    // input
    private GUIAnalysisOutput guiOutput;
    private FlowgraphRebuilder rebuilder;
    private Map<Local, Object> constSolution;
    private Multimap<Local, Local> jumpSolution; // local flow-to set
    // utils
    private Hierarchy hier = Hierarchy.v();
    private AndroidCallGraph cg = AndroidCallGraph.v();
    private JimpleUtil jimpleUtil = JimpleUtil.v();

    public ConstantPreferenceAnalysis(GUIAnalysisOutput output, FlowgraphRebuilder r) {
        guiOutput = output;
        rebuilder = r;
        constSolution = Maps.newHashMap();
        jumpSolution = HashMultimap.create();
    }

    public ConstantPreferenceAnalysis(ConstantPreferenceAnalysis another) {
        guiOutput = another.guiOutput;
        rebuilder = another.rebuilder;
        constSolution = Maps.newHashMap();
        jumpSolution = HashMultimap.create();
    }

    // this API is provided for GUI object constant propagation
    public void doAnalysis(NObjectNode guiObject, SootMethod handler,
                           HashMultimap<Stmt, Stmt> infeasibleEdges,
                           HashMultimap<Stmt, SootMethod> infeasibleCalls) {
        reset();
        Local guiLocal = null;
        if (guiOutput.isOnSharedPreferenceChanged(handler)) {
            guiLocal = jimpleUtil.localForNthParameter(handler, 2);
        } else if (guiOutput.isOnPreferenceClick(handler) || guiOutput.isOnPreferenceChange(handler)) {
            guiLocal = jimpleUtil.localForNthParameter(handler, 1);
        } else {
            return;
        }
        if (guiLocal == null) {
            return;
        }
        if (guiObject.idNode == null) {
            return;
        }

        constSolution.put(guiLocal, ((NPreferenceIdNode)guiObject.idNode).prefKey);
        detectInfeasibleEdge(handler, infeasibleEdges, infeasibleCalls, guiObject, guiLocal);
    }

    private void reset() {
        constSolution.clear();
        jumpSolution.clear();
    }

    private void detectInfeasibleEdge(SootMethod handler, HashMultimap<Stmt, Stmt> infeasibleEdges,
                                      HashMultimap<Stmt, SootMethod> infeasibleCalls, NObjectNode guiObject, Local guiLocal) {
        Set<SootMethod> memberSet = Sets.newHashSet(handler);
        List<SootMethod> workList = Lists.newArrayList(handler);

        List<Local> localList = Lists.newArrayList();

        boolean isTrueValue = false;
        boolean isFound = false;

        while (!workList.isEmpty()) {
            SootMethod mtd = workList.remove(0);
            Body body = null;
            UnitGraph cfg = null;
            synchronized (mtd) {
                body = mtd.retrieveActiveBody();
                cfg = new ExceptionalUnitGraph(body);
            }
            Iterator<Unit> stmts = body.getUnits().iterator();
            while (stmts.hasNext()) {
                Stmt s = (Stmt) stmts.next();
                Set<SootMethod> escapedCallees = addReachableCall(null, s, memberSet, workList);
                infeasibleCalls.putAll(s, escapedCallees);

                if (s instanceof AssignStmt && ((AssignStmt) s).getLeftOp() instanceof Local) {
                    Local lop = (Local) ((AssignStmt) s).getLeftOp();
                    Value rop = ((AssignStmt) s).getRightOp();

                    if (rop instanceof VirtualInvokeExpr) {
                        String ropSig = ((InvokeExpr) rop).getMethodRef().getSignature();
                        Value ropBasebox = ((JVirtualInvokeExpr) rop).getBaseBox().getValue();
                        String preferenceKey = ((NPreferenceIdNode) guiObject.idNode).prefKey;

                        if (ropBasebox == guiLocal) {
                            if (ropSig.contentEquals("<java.lang.String: int hashCode()>")) {
                                int hashCode = preferenceKey.hashCode();
                                constSolution.put(lop, hashCode);
                            } else if (ropSig.contentEquals("<java.lang.String: boolean equals(java.lang.Object)>")) {
                                String ropAgr = ((JVirtualInvokeExpr) rop).getArg(0).toString();

                                if (preferenceKey.contentEquals(ropAgr.replaceAll("\"", ""))) {
                                    constSolution.put(lop, 1);
                                    isTrueValue = true;
                                } else {
                                    constSolution.put(lop, 0);
                                }
                            } else if (ropSig.contentEquals("<android.preference.Preference: java.lang.String getKey()>")) {
                                guiLocal = lop;
                            }
                        }
                    } else if (rop instanceof IntConstant) {
                        localList.add(lop);
                        if (isTrueValue) {
                            constSolution.put(lop, ((IntConstant)rop).value);
                            isTrueValue = false;
                            isFound = true;
                        }
                    }
                }

                if (!isFound && localList.size() > 0) {
                    constSolution.put(localList.remove(0),-1);
                }

                if (!(s instanceof IfStmt) && !(s instanceof TableSwitchStmt)
                        && !(s instanceof LookupSwitchStmt)) {
                    continue;
                }
                if (s instanceof IfStmt) {
                    handleIfStmt(cfg, (IfStmt) s, infeasibleEdges);
                } else if (s instanceof TableSwitchStmt) {
                    handleTableSwitchStmt(cfg, (TableSwitchStmt) s, infeasibleEdges);
                } else if (s instanceof LookupSwitchStmt) {
                    handleLookupSwitchStmt(cfg, (LookupSwitchStmt) s, infeasibleEdges);
                } else {
                    Logger.err(getClass().getSimpleName(), "can not handle the stmt: " + s);
                }
            }
        }
    }

    private void handleIfStmt(UnitGraph cfg, IfStmt s, HashMultimap<Stmt, Stmt> infeasibleEdges) {
        Value conditionValue = s.getCondition();
        GoTo goTo = eval(conditionValue);
        if (goTo == GoTo.unrelated) {
            return;
        } else if (goTo == GoTo.true_branch) {
            for (Unit u : cfg.getSuccsOf(s)) {
                if (u != s.getTarget()) {
                    infeasibleEdges.put(s, (Stmt) u);
                }
            }
        } else {
            infeasibleEdges.put(s, s.getTarget());
        }
    }
    private void handleTableSwitchStmt(UnitGraph cfg, TableSwitchStmt s, HashMultimap<Stmt, Stmt> infeasibleEdges) {
        Value key = s.getKey();
        if (!(key instanceof Local)) {
            return;
        }
        Object o = constSolution.get(key);
        if (o == null || o == ANY) {
            return;
        } else if (!(o instanceof Integer)) {
            Logger.err(getClass().getSimpleName(), "the const value of the key local: " + key + " is type of " + o.getClass());
        } else {
            int lowIdx = s.getLowIndex();
            int highIdx = s.getHighIndex();
            int entry = ((Integer)o).intValue();
            Unit target = null;
            if (entry >= lowIdx && entry <= highIdx) {
                target = s.getTarget(entry - lowIdx);
            } else {
                target = s.getDefaultTarget();
            }
            for (int i = lowIdx; i <= highIdx; i++) {
                Unit u = s.getTarget(i-lowIdx);
                if (u == target) {
                    continue;
                }
                infeasibleEdges.put(s, (Stmt) u);
            }
            Unit u = s.getDefaultTarget();
            if (u != target) {
                infeasibleEdges.put(s, (Stmt) u);
            }
        }
    }
    private void handleLookupSwitchStmt(UnitGraph cfg, LookupSwitchStmt s, HashMultimap<Stmt, Stmt> infeasibleEdges) {
        Value key = s.getKey();
        if (!(key instanceof Local)) {
            return;
        }
        Object o = constSolution.get(key);
        if (o == null || o == ANY) {
            return;
        } else if (!(o instanceof Integer)) {
            Logger.err(getClass().getSimpleName(), "the const value of the key local: " + key + " is type of " + o.getClass());
        } else {
            int entry = ((Integer)o).intValue();
            Unit target = null;
            for (int i = 0; i < s.getLookupValues().size(); i++) {
                int lookupValue = s.getLookupValue(i);
                if (lookupValue == entry) {
                    target = s.getTarget(i);
                    break;
                }
            }
            if (target == null) {
                target = s.getDefaultTarget();
            }
            for (int i = 0; i < s.getLookupValues().size(); i++) {
                Unit u = s.getTarget(i);
                if (u == target) {
                    continue;
                }
                infeasibleEdges.put(s, (Stmt) u);
            }
            Unit u = s.getDefaultTarget();
            if (u != target) {
                infeasibleEdges.put(s, (Stmt) u);
            }
        }
    }

    private GoTo eval(Value v) {
        if (v instanceof Local) {
            Object value = constSolution.get(v);
            if (value == null || value == ANY) {
                return GoTo.unrelated;
            }
            if (value instanceof Integer) {
                if (((Integer) value).intValue() == 1) {
                    return GoTo.true_branch;
                } else {
                    return GoTo.false_branch;
                }
            } else {
                Logger.err(getClass().getSimpleName(), "the const value is not null, any nor Integer, it is " + value.getClass());
                return null;
            }
        } else if (v instanceof ConditionExpr) {
            boolean equalOp = true;
            if (v instanceof EqExpr) {
                equalOp = true;
            } else if (v instanceof NeExpr) {
                equalOp = false;
            } else {
                return GoTo.unrelated;
            }
            Value op1 = ((ConditionExpr) v).getOp1();
            Value op2 = ((ConditionExpr) v).getOp2();
            if (op1.getType() instanceof RefType) {
                if (op1 instanceof Local && op2 instanceof StringConstant) {
                    Object o1 = constSolution.get(op1);
                    Object o2 = ((StringConstant) op2).value;
                    if (o1 == null || o1 == ANY || o2 == null || o2 == ANY) {
                        return GoTo.unrelated;
                    } else if (o1.toString().contentEquals(o2.toString())) {
                        if (equalOp) {
                            return GoTo.true_branch;
                        } else {
                            return GoTo.false_branch;
                        }
                    } else {
                        if (!equalOp) {
                            return GoTo.true_branch;
                        } else {
                            return GoTo.false_branch;
                        }
                    }
                } else {
                    return GoTo.unrelated;
                }
            } else if (op1.getType() instanceof IntType || (op1.getType() instanceof BooleanType)) {
                Integer v1 = null;
                Integer v2 = null;
                if (op1 instanceof Local) {
                    Object o1 = constSolution.get(op1);
                    if (o1 == null || o1 == ANY) {
                        return GoTo.unrelated;
                    } else if (o1 instanceof Integer) {
                        v1 = (Integer)o1;
                    } else {
                        return GoTo.unrelated;
                    }
                } else if (op1 instanceof IntConstant) {
                    v1 = ((IntConstant) op1).value;
                } else {
                    return GoTo.unrelated;
                }
                if (op2 instanceof Local) {
                    Object o2 = constSolution.get(op2);
                    if (o2 == null || o2 == ANY) {
                        return GoTo.unrelated;
                    } else if (o2 instanceof Integer) {
                        v2 = (Integer)o2;
                    } else {
                        return GoTo.unrelated;
                    }
                } else if (op2 instanceof IntConstant) {
                    v2 = ((IntConstant) op2).value;
                } else {
                    return GoTo.unrelated;
                }

                if (v1.intValue() == v2.intValue()) {
                    if (equalOp) {
                        return GoTo.true_branch;
                    } else {
                        return GoTo.false_branch;
                    }
                } else {
                    if (!equalOp) {
                        return GoTo.true_branch;
                    } else {
                        return GoTo.false_branch;
                    }
                }
            } else {
                return GoTo.unrelated;
            }
        } else {
            return GoTo.unrelated;
        }
    }

    private Set<SootMethod> addReachableCall(SootMethod mtd, Stmt s, Set<SootMethod> memberSet, List<SootMethod> reachableMethods) {
        Preconditions.checkArgument((mtd != null && s == null) || (mtd == null && s != null));
        Set<SootMethod> infeasibleCallees = Sets.newHashSet();
        Set<Edge> outgoings = null;
        if (mtd != null) {
            outgoings = cg.getOutgoingEdges(mtd);
        } else {
            outgoings = cg.getEdge(s);
        }
        for (Edge outgoing : outgoings) {
            if (outgoing.callSite.getInvokeExpr() instanceof InterfaceInvokeExpr
                    || outgoing.callSite.getInvokeExpr() instanceof VirtualInvokeExpr) {
                // we don't handle SpecialInvokeExpr
                Local rcv = jimpleUtil.receiver(outgoing.callSite);
                Type rcvType = rcv.getType();
                if (rcvType instanceof RefType) {
                    Object solution = constSolution.get(rcv);
                    if (solution != null && solution != ANY && solution instanceof NObjectNode) {
                        SootClass type = ((NObjectNode) solution).getClassType();
                        if (type == null) {
                            // if we can not find the type for solution, set it to any
                            Logger.err(getClass().getSimpleName(), "can not find the type of solution: " + solution);
                        }
                        if (type.isConcrete()) {
                            // we have constant resolution to refine the call graph
                            SootMethod tgt = hier.virtualDispatch(outgoing.target,
                                    ((NObjectNode) solution).getClassType());
                            if (tgt != null && tgt.getDeclaringClass().isApplicationClass()
                                    && tgt.isConcrete()) {
                                if (memberSet.add(tgt)) {
                                    reachableMethods.add(tgt);
                                }
                            } else {
                                infeasibleCallees.add(tgt);
                            }
                            continue;
                        }
                    }
                }
            }
            if (outgoing.target.getDeclaringClass().isApplicationClass()
                    && outgoing.target.isConcrete()) {
                if (memberSet.add(outgoing.target)) {
                    reachableMethods.add(outgoing.target);
                }
            } else {
                infeasibleCallees.add(outgoing.target);
            }
        }
        return infeasibleCallees;
    }

    enum GoTo {
        unrelated,
        true_branch,
        false_branch,
    }
}