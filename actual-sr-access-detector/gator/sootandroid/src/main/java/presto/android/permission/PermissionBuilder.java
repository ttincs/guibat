/*
 * PermissionBuilder.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.permission;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import presto.android.Configs;
import presto.android.Hierarchy;
import presto.android.MethodNames;
import presto.android.gui.GraphUtil;
import presto.android.gui.PropertyManager;
import presto.android.gui.graph.*;
import presto.android.gui.rep.GUIHierarchy;
import presto.android.gui.rep.StaticGUIHierarchy;
import presto.android.gui.wtg.analyzer.CFGAnalyzerInput;
import presto.android.gui.wtg.analyzer.CFGAnalyzerOutput;
import presto.android.gui.wtg.flowgraph.FlowgraphRebuilder;
import presto.android.gui.wtg.parallel.CFGScheduler;
import presto.android.gui.wtg.util.Filter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.iccta.IccLink;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class PermissionBuilder {
    StaticGUIHierarchy guiHierarchy;
    FlowgraphRebuilder flowgraphRebuilder;
    Hierarchy hierarchy;
    GraphUtil graphUtil;
    public Map<NObjectNode, Set<NObjectNode>> permissionMaps = Maps.newHashMap();
    public Map<NObjectNode, Set<NObjectNode>> dialogMaps = Maps.newHashMap();

    public PermissionBuilder(GUIHierarchy guiHierarchy) {
        this.guiHierarchy = (StaticGUIHierarchy)guiHierarchy;
        this.flowgraphRebuilder = FlowgraphRebuilder.v(this.guiHierarchy.analysisOutput);
        hierarchy = Hierarchy.v();
        graphUtil = GraphUtil.v();
    }

    public Set<NObjectNode> getUpFrontRequestPermissions() {
        SootClass mainActivity = guiHierarchy.analysisOutput.getMainActivity();
        if (mainActivity == null) return Sets.newHashSet();

        Set<CFGAnalyzerInput> inputSet = Sets.newHashSet();

        for (SootMethod method : mainActivity.getMethods()) {
            String subSig = method.getSubSignature();
            if (subSig.contentEquals(MethodNames.onActivityCreateSubSig) ||
                    subSig.contentEquals(MethodNames.onActivityStartSubSig) ||
                    subSig.contentEquals(MethodNames.onActivityResumeSubSig)) {
                NInflNode dummyNode = new NInflNode();
                dummyNode.c = mainActivity;
                CFGAnalyzerInput analyzerInput = new CFGAnalyzerInput(dummyNode, method, Filter.runtimeRequestPermissionStmtFilter);
                inputSet.add(analyzerInput);
            }
        }

        CFGScheduler scheduler = new CFGScheduler(guiHierarchy.analysisOutput, flowgraphRebuilder);
        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSet);
        Set<NObjectNode> permissions = Sets.newHashSet();
        for (Map.Entry<CFGAnalyzerInput, CFGAnalyzerOutput> entry : analyzeOutput.entrySet()) {
            CFGAnalyzerOutput output = entry.getValue();

            if (output.targets != null && !output.targets.isEmpty()) {
                permissions.addAll(output.targets.keySet());
            }
        }
        return permissions;
    }

    public void build() {
        Map<NObjectNode, Set<SootMethod>> guiHandlers = buildGuiEventHandlers();
        Set<CFGAnalyzerInput> inputSet = Sets.newHashSet();
        for (NObjectNode window : guiHandlers.keySet()) {
            for (SootMethod handler : guiHandlers.get(window)) {
                CFGAnalyzerInput analyzerInput = new CFGAnalyzerInput(window, handler, Filter.permissionStmtFilter);
                inputSet.add(analyzerInput);
            }
        }
        CFGScheduler scheduler = new CFGScheduler(guiHierarchy.analysisOutput, flowgraphRebuilder);
//
//        Set<CFGAnalyzerInput> inputSetTest = Sets.newHashSet();
//        for (CFGAnalyzerInput input : inputSet) {
//            if (input.widget.idNode != null
//                    //&& input.widget.idNode.getIdValue() == 1
//                    //&& input.handler.getSignature().contains("<vn.dev.batterydoctorpro.MainActivity: boolean onNavigationItemSelected(android.view.MenuItem)>")
//                    && input.widget.idNode.getIdName().contains("btnSetWallpaper")
//                    ) {
//
//                inputSetTest.add(input);
//            }
//        }
//
//
//        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSetTest);

        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSet);

        for (Map.Entry<CFGAnalyzerInput, CFGAnalyzerOutput> entry : analyzeOutput.entrySet()) {
            CFGAnalyzerInput input = entry.getKey();
            CFGAnalyzerOutput output = entry.getValue();

            Set<NObjectNode> permissions = permissionMaps.get(input.widget);
            if (permissions == null) {
                permissions = Sets.newHashSet();
            }

            if (output.targets != null && !output.targets.isEmpty()) {
                permissions.addAll(output.targets.keySet());
            }
            permissionMaps.put(input.widget, permissions);
        }
    }

    private Map<NObjectNode, Set<SootMethod>> buildGuiEventHandlers() {

        Map<NObjectNode, Set<SootMethod>> results;

        if (!Configs.iccModelFile.isEmpty()) {
            results = analyzeICC(guiHierarchy.handlerMethodMaps, flowgraphRebuilder.getIccLinks());
        } else {
            results = analyzeICC(guiHierarchy.handlerMethodMaps);
        }

        for (NObjectNode guiWidget : guiHierarchy.handlerMethodMaps.keySet()) {
            if (guiWidget == null) continue;
            Set<SootMethod> guiHandlers = results.get(guiWidget);
            if (guiHandlers == null) {
                guiHandlers = Sets.newHashSet();
            } else {
                guiHandlers.addAll(guiHierarchy.handlerMethodMaps.get(guiWidget));
            }
            results.put(guiWidget, guiHandlers);
        }

        return results;
    }

    private Map<NObjectNode, Set<SootMethod>> analyzeICC(HashMap<NObjectNode, Set<SootMethod>> handlerMethodMaps) {
        Map<NObjectNode, Set<SootMethod>> iccResults = Maps.newHashMap();

        Set<CFGAnalyzerInput> inputSet = Sets.newHashSet();
        for (NObjectNode window : handlerMethodMaps.keySet()) {
            if (window == null) continue;

            for (SootMethod handler : handlerMethodMaps.get(window)) {
                CFGAnalyzerInput analyzerInput = new CFGAnalyzerInput(window, handler, Filter.startActivityAndServiceStmtFilter);
                inputSet.add(analyzerInput);
            }
        }
        CFGScheduler scheduler = new CFGScheduler(guiHierarchy.analysisOutput, flowgraphRebuilder);

//        Set<CFGAnalyzerInput> inputSetTest = Sets.newHashSet();
//        for (CFGAnalyzerInput input : inputSet) {
//            if (input.widget.idNode != null
//                    //&& input.widget.idNode.getIdValue() == 1
//                    //&& input.handler.getSignature().contains("<com.angrydoughnuts.android.alarmclock.AlarmClockActivity: boolean onOptionsItemSelected(android.view.MenuItem)>")
//                    && input.widget.idNode.getIdName().contains("default_options")
//            ) {
//                inputSetTest.add(input);
//            }
//        }
//        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSetTest);

        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSet);

        for (Map.Entry<CFGAnalyzerInput, CFGAnalyzerOutput> entry : analyzeOutput.entrySet()) {
            CFGAnalyzerInput input = entry.getKey();
            CFGAnalyzerOutput output = entry.getValue();
            if (input.widget == null) continue;

            if (!iccResults.containsKey(input.widget)) {
                iccResults.put(input.widget, Sets.newHashSet());
            }

            Set<SootMethod> guiHandlers = iccResults.get(input.widget);
            if (guiHandlers == null) {
                guiHandlers = Sets.newHashSet();
            }

            if (output.targets != null && !output.targets.isEmpty()) {
                for (NObjectNode targetNode : output.targets.keySet()) {
                    if (targetNode instanceof NDialogNode) {
                        Set<NObjectNode> dialogs = dialogMaps.get(input.widget);
                        if (dialogs == null) {
                            dialogs = Sets.newHashSet();
                        }
                        dialogs.add(targetNode);
                        dialogMaps.put(input.widget, dialogs);
                        mapDialogToTriggerWidget(input.widget, (NDialogNode)targetNode);

                        Iterator<NNode> textNodes = targetNode.getTextNodes();
                        while (textNodes.hasNext()) {
                            addTextNodeToView(input.widget, textNodes.next());
                        }

                    } else if (targetNode instanceof NDialogFragmentNode) {
                        Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                targetNode.getClassType(),
                                Lists.newArrayList(new String[] {MethodNames.dialogFragmentOnCreateDialogSubSig}));
                        guiHandlers.addAll(handlers);
                    } else if (targetNode instanceof NServiceNode) {
                        Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                ((NServiceNode) targetNode).c,
                                Lists.newArrayList(new String[] {MethodNames.onStartCommandSubSig, MethodNames.serviceOnCreateSubSig }));
                        guiHandlers.addAll(handlers);
                    } else if (targetNode instanceof NToastNode
                            || targetNode instanceof NSnackbarNode) {
                        if (targetNode.getTextNodes().hasNext()) {
                            addTextNodeToView(input.widget, targetNode.getTextNodes().next());
                        }
                        if (targetNode instanceof NSnackbarNode) {
                            if (((NSnackbarNode) targetNode).listenerClass != null) {
                                Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                        ((NSnackbarNode) targetNode).listenerClass,
                                        Lists.newArrayList(new String[] {MethodNames.onActivityCreateSubSig}));
                                guiHandlers.addAll(handlers);
                            }
                        }
                    } else {
                        Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                targetNode.getClassType(),
                                Lists.newArrayList(new String[] {MethodNames.onActivityCreateSubSig, MethodNames.onActivityStartSubSig, MethodNames.onActivityResumeSubSig}));
                        guiHandlers.addAll(handlers);
                    }
                }
            }
        }

        return iccResults;
    }

    private Map<NObjectNode, Set<SootMethod>> analyzeICC(HashMap<NObjectNode, Set<SootMethod>> handlerMethodMaps, List<IccLink> iccLinks) {
        Map<NObjectNode, Set<SootMethod>> iccResults = Maps.newHashMap();

        Set<CFGAnalyzerInput> inputSet = Sets.newHashSet();
        for (NObjectNode window : handlerMethodMaps.keySet()) {
            if (window == null) continue;

            for (SootMethod handler : handlerMethodMaps.get(window)) {
                CFGAnalyzerInput analyzerInput = new CFGAnalyzerInput(window, handler, Filter.icc3StmtFilter);
                inputSet.add(analyzerInput);
            }
        }
        CFGScheduler scheduler = new CFGScheduler(guiHierarchy.analysisOutput, flowgraphRebuilder);
        Map<CFGAnalyzerInput, CFGAnalyzerOutput> analyzeOutput = scheduler.schedule(inputSet);

        for (Map.Entry<CFGAnalyzerInput, CFGAnalyzerOutput> entry : analyzeOutput.entrySet()) {
            CFGAnalyzerInput input = entry.getKey();
            CFGAnalyzerOutput output = entry.getValue();
            if (input.widget == null) continue;

            if (!iccResults.containsKey(input.widget)) {
                iccResults.put(input.widget, Sets.newHashSet());
            }

            Set<SootMethod> guiHandlers = iccResults.get(input.widget);
            if (guiHandlers == null) {
                guiHandlers = Sets.newHashSet();
            }

            if (output.targets != null && !output.targets.isEmpty()) {
                for (Pair<Stmt, SootMethod> target : output.targets.values()) {
                    SootClass desSootClass = findDestinationClass(target, iccLinks);
                    if (desSootClass == null) continue;

                    if (hierarchy.isSubclassOf(desSootClass, Scene.v().getSootClass("android.app.Service"))) {
                        Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                desSootClass,
                                Lists.newArrayList(new String[] {MethodNames.onStartCommandSubSig, MethodNames.serviceOnCreateSubSig }));
                        guiHandlers.addAll(handlers);
                    } else if (hierarchy.isSubclassOf(desSootClass, Scene.v().getSootClass("android.app.Activity"))) {
                        Set<SootMethod> handlers = guiHierarchy.analysisOutput.getActivityHandlers(
                                desSootClass,
                                Lists.newArrayList(new String[] {MethodNames.onActivityCreateSubSig}));
                        guiHandlers.addAll(handlers);
                    }
                }
            }
        }

        return iccResults;
    }

    private SootClass findDestinationClass(Pair<Stmt, SootMethod> target, List<IccLink> iccLinks) {
        for (IccLink iccLink : iccLinks) {
            if (iccLink.getFromSM().equals(target.getO2())
                    && iccLink.getFromU().equals(target.getO1())) {
                return iccLink.getDestinationC();
            }
        }

        return null;
    }

    private void addTextNodeToView(NObjectNode widget, NNode textNode) {
        Set<NNode> textNodes = Sets.newHashSet();
        if (textNode instanceof NStringConstantNode) {
            textNodes.add(textNode);
        } else {
            Iterator<NNode> preNodes = graphUtil.backwardReachableNodes(textNode).iterator();
            while (preNodes.hasNext()) {
                NNode nNode = preNodes.next();
                if( nNode instanceof NStringConstantNode) {
                    textNodes.add(nNode);
                }
            }
        }

        for (GUIHierarchy.View view : guiHierarchy.views) {
            if (view.viewNode == widget) {
                for (NNode nNode : textNodes) {
                    String text = PropertyManager.v().textNodeToString(nNode);
                    view.getFeedbacks().add(text);
                }
            }
        }
    }

    private void mapDialogToTriggerWidget(NObjectNode widget, NDialogNode dialogNode) {
        for (GUIHierarchy.Dialog dialog : guiHierarchy.dialogs) {
            if (dialog.getAllocStmt().contentEquals(dialogNode.allocStmt.toString())
            && dialog.getAllcMethod().contentEquals(dialogNode.allocMethod.getSignature())) {
                dialog.addTriggerWidget(widget);
            }
        }
    }
}
