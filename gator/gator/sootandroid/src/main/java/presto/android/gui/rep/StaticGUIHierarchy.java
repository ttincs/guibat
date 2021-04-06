/*
 * StaticGUIHierarchy.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package presto.android.gui.rep;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import presto.android.*;
import presto.android.gui.*;
import presto.android.gui.graph.*;
import presto.android.gui.listener.EventType;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class StaticGUIHierarchy extends GUIHierarchy {
  // Quick hack to save all handler signatures
  public Set<SootMethod> handlerMethods = Sets.newHashSet();

  // Quick hack to save all view
  public Set<View> views = Sets.newHashSet();


  public HashMap<NObjectNode, Set<SootMethod>> handlerMethodMaps = Maps.newHashMap();

  // Helpers
  public GUIAnalysisOutput analysisOutput;
  Flowgraph flowgraph;
  Hierarchy hier = Hierarchy.v();
  JimpleUtil jimpleUtil = JimpleUtil.v();
  PropertyManager prop = PropertyManager.v();
  IDNameExtractor idNameExtractor = IDNameExtractor.v();

  public StaticGUIHierarchy(GUIAnalysisOutput output) {
    this.analysisOutput = output;
    this.flowgraph = output.getFlowgraph();
    build();
  }

  void build() {
    app = Configs.benchmarkName;
    buildActivities();
    buildDialogs();
  }

  void traverseRootViewAndHierarchy(ViewContainer parent, Set<NNode> roots) {
    // Roots & view hierarchy
    if (roots != null && !roots.isEmpty()) {
      for (NNode n : roots) {
        buildView(parent, n);
      }
    }
  }

  SootClass currentActivity;

  void buildActivities() {
    for (SootClass activityClass : analysisOutput.getActivities()) {
      currentActivity = activityClass;
      Activity act = new Activity();
      activities.add(act);
      act.name = activityClass.getName();
      act.text = analysisOutput.getActivityLabels().get(activityClass.getName());

      traverseRootViewAndHierarchy(
              act, analysisOutput.getActivityRoots(activityClass));

      // Options menu
      buildOptionsMenu(act, activityClass);

      buildPreferences(act, activityClass);
    }
    currentActivity = null;
  }

  void buildPreferences(Activity act, SootClass activityClass){
    NPreferenceNode preferenceNode =
            flowgraph.activityClassToPreference.get(activityClass);
    if (preferenceNode != null) {

      buildView(act, preferenceNode);
      // Add handlers for menu items
    }
  }

  void buildDialogs() {
    for (NDialogNode dialogNode : analysisOutput.getDialogs()) {
      Dialog dialog = new Dialog();
      dialogs.add(dialog);
      dialog.name = dialogNode.c.getName();
      dialog.allocStmt = dialogNode.allocStmt.toString();
      dialog.allocMethod = dialogNode.allocMethod.getSignature();
      dialog.allocLineNumber = jimpleUtil.getLineNumber(dialogNode.allocStmt);
      Debug.v().printf(
              "%s @ %s -> %d\n", dialog.allocStmt, dialog.allocMethod, dialog.allocLineNumber);

      traverseRootViewAndHierarchy(
              dialog, analysisOutput.getDialogRoots(dialogNode));
    }
  }

  void buildEventAndHandlers(View view, SootMethod event, EventType eventType) {
    EventAndHandler eventAndHandler = new EventAndHandler();
    eventAndHandler.event = eventType.toString();
    eventAndHandler.handler = event.getSignature();
    eventAndHandler.sootMethod = event;
    view.eventAndHandlers.add(eventAndHandler);
    handlerMethods.add(event);

    Set<SootMethod> handlers = handlerMethodMaps.get(view.viewNode);
    if (handlers == null) {
      handlers = new HashSet<>();
      handlerMethodMaps.put(view.viewNode, handlers);
    }
    handlers.add(event);
  }


  void buildOptionsMenu(Activity act, SootClass activityClass) {
    NOptionsMenuNode optionsMenu =
            flowgraph.activityClassToOptionsMenu.get(activityClass);
    if (optionsMenu != null) {
      buildView(act, optionsMenu);
      // Add handlers for menu items
      View optionsMenuView = act.views.get(act.views.size() - 1);
      if (!optionsMenuView.type.equals("android.view.Menu")) {
        throw new RuntimeException(optionsMenuView.type + " is not Menu!");
      }
      Set<SootMethod> handlers = analysisOutput.getActivityHandlers(
              activityClass,
              Lists.newArrayList(
                      MethodNames.onMenuItemSelectedSubSig,
                      MethodNames.onOptionsItemSelectedSubSig));
      for (View menuItem : optionsMenuView.views) {
        for (SootMethod m : handlers) {
          buildEventAndHandlers(menuItem, m, EventType.item_selected);
        }
      }
    }
  }

  Set<NNode> visitingNodes = Sets.newHashSet();

  void buildView(ViewContainer parent, NNode node) {
    if (!(node instanceof NObjectNode)) {
      throw new RuntimeException(node.toString());
    }
    if (visitingNodes.contains(node)) {
      Logger.trace(this.getClass().getSimpleName(), "[WARNING] Node "
              + node + " already printed. Cycle!!!");
      return;
    }
    visitingNodes.add(node);

    NObjectNode objectNode = (NObjectNode) node;
    Set<NNode> childSet = objectNode.getChildren();
    Set<NContextMenuNode> contextMenuSet =
            analysisOutput.getContextMenus(objectNode);
    boolean noChild = childSet.isEmpty() && contextMenuSet.isEmpty();
    SootClass type = objectNode.getClassType();

    if (objectNode instanceof NInflNode) {
      NInflNode inflNode = (NInflNode) objectNode;
      if (type != null && hier.isMenuItemClass(type)) { // Is it menu item?
        if (!noChild) {
          throw new RuntimeException(
                  "MenuItem: " + inflNode + " is not a leaf!");
        }
//        String title = prop.getSpeciallySeparatedTextOrTitlesOfView(inflNode);
        Set<String> title = prop.getTextsOrTitlesOfView(inflNode);
//        if (title == null) {
//          title = NO_TITLE;
//        }
        View view = new View();
        view.type = type.getName();
//        view.title = title;
        view.title.addAll(title);
        view.viewNode = objectNode;
//        view.hint = prop.getSpeciallySeparatedHintOfView(inflNode);
        view.hint.addAll(prop.getHintOfView(inflNode));
        view.tooltips.addAll(prop.getTooltipOfView(inflNode));
        view.contentDescriptions.addAll(prop.getContentDescriptionOfView(inflNode));
        view.imageResources.addAll(prop.getImageOfView(inflNode));


        Pair<Integer, String> idAndName = getIdAndName(node.idNode);
        view.id = idAndName.getO1().intValue();
        view.idName = idAndName.getO2();
        parent.addChild(view);
        buildEventAndHandlers(view, node);
        buildEventAndHandlerForNavigationDrawerMenuItem(view, node);
        visitingNodes.remove(node);
        views.add(view);
        return;
      }
    }

    // Now, print other types of nodes. First, the open tag.
    Pair<Integer, String> idAndName = getIdAndName(node.idNode);
    View view = new View();
    parent.addChild(view);

    view.viewNode = objectNode;

    if (!hier.isNavigationViewClass(type)) {
      buildEventAndHandlers(view, node);
    }

    view.type = type != null ? type.getName() : "?";
    view.id = idAndName.getO1();
    view.idName = idAndName.getO2();

//    view.title = type != null && node instanceof NInflNode ?
//            prop.getSpeciallySeparatedTextOrTitlesOfView(objectNode) :
//            null;
    //if (type != null && node instanceof NInflNode) {
      view.title.addAll(prop.getTextsOrTitlesOfView(objectNode));
    //}
//    view.hint = prop.getSpeciallySeparatedHintOfView(objectNode);
    view.hint.addAll(prop.getHintOfView(objectNode));
    view.tooltips.addAll(prop.getTooltipOfView(objectNode));
    view.contentDescriptions.addAll(prop.getContentDescriptionOfView(objectNode));
    view.imageResources.addAll(prop.getImageOfView(objectNode));

    // print children
    for (NNode n : objectNode.getChildren()) {
      buildView(view, n);
    }
    // special child
    for (NContextMenuNode contextMenu : contextMenuSet) {
      buildView(view, contextMenu);
      // Add event handlers for menu item in the context menu
      View contextMenuView = view.views.get(view.views.size() - 1);
      if (!contextMenuView.type.equals("android.view.ContextMenu")) {
        throw new RuntimeException(
                contextMenuView.type + " is not ContextMenu!");
      }
      Set<SootMethod> handlers = analysisOutput.getActivityHandlers(
              currentActivity, Lists.newArrayList(MethodNames.onMenuItemSelectedSubSig,
                      MethodNames.onContextItemSelectedSubSig));
      for (View menuItem : contextMenuView.views) {
        for (SootMethod m : handlers) {
          buildEventAndHandlers(menuItem, m, EventType.item_selected);
        }
      }
    }

    visitingNodes.remove(node);
    views.add(view);
  }

  void buildEventAndHandlers(View view, NNode node) {
    NObjectNode guiObject = (NObjectNode) node;
    // Explicit
    Map<EventType, Set<SootMethod>> explicitEventsAndHandlers =
            analysisOutput.getExplicitEventsAndTheirHandlers(guiObject);
    for (Map.Entry<EventType, Set<SootMethod>> entry : explicitEventsAndHandlers.entrySet()) {
      EventType event = entry.getKey();
      for (SootMethod m : entry.getValue()) {
        buildEventAndHandlers(view, m, event);
      }
    }
    // Context menus
    Set<NContextMenuNode> contextMenus =
            analysisOutput.getContextMenus(guiObject);
    for (NContextMenuNode context : contextMenus) {
      SootMethod m = analysisOutput.getOnCreateContextMenuMethod(context);
      buildEventAndHandlers(view, m, EventType.implicit_create_context_menu);
    }
  }

  final String NO_TITLE = "NO_TITLE";
  final String NO_ID_NAME = "NO_ID";
  final Pair<Integer, String> NO_ID = new Pair<Integer, String>(-1, NO_ID_NAME);

  Pair<Integer, String> getIdAndName(NIdNode idNode) {
    if (idNode == null) {
      return NO_ID;
    }
    Integer id = idNode.getIdValue();
    String name = idNode.getIdName();
    if (idNameExtractor.isUnknown(name)) {
      name = NO_ID_NAME;
    }
    if (name.equals(NO_ID_NAME)) {
      return NO_ID;
    } else {
      return new Pair<Integer, String>(id, name);
    }
  }

  NObjectNode getNavigationView(NObjectNode node) {
    if (!node.getParents().hasNext()) return null;

    LinkedList<NObjectNode> workList = new LinkedList<>();
    workList.add((NObjectNode)node.getParents().next());

    while (!workList.isEmpty()) {
      NObjectNode pNode = workList.remove();
      if (hier.isNavigationViewClass(pNode.getClassType())) {
        return pNode;
      }

      if (!pNode.getParents().hasNext()) return null;

      workList.add((NObjectNode)pNode.getParents().next());
    }

    return null;
  }

  void buildEventAndHandlerForNavigationDrawerMenuItem(View view, NNode node) {
    NObjectNode guiObject = (NObjectNode) node;

    if (hier.isMenuItemClass(guiObject.getClassType())) {
      NObjectNode navigationNode = getNavigationView(guiObject);
      if (navigationNode != null) {
        buildEventAndHandlers(view, navigationNode);
      }
    }
  }
}
