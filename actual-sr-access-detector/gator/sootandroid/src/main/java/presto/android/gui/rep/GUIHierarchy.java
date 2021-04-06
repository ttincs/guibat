/*
 * GUIHierarchy.java - part of the GATOR project
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
import org.apache.commons.lang.StringUtils;
import presto.android.Configs;
import presto.android.gui.GUIAnalysisOutput;
import presto.android.gui.PropertyManager;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.graph.NStringConstantNode;
import presto.android.permission.PermissionNode;
import presto.android.util.XmlHelper;
import soot.Scene;
import soot.SootMethod;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GUIHierarchy {
  // Data
  public String app;
  public List<Activity> activities = Lists.newArrayList();
  public List<Dialog> dialogs = Lists.newArrayList();
  private Map<String, String> activitiyLabels = Maps.newHashMap();
  private static final int indentation = 2;
  private static Set<String> printedString = Sets.newHashSet();
  public static boolean hasUIPermission = false;

  private static void printlnIndented(String s, PrintStream out, int indent) {
    for (int i = 0; i < indent; i++) {
      out.print(" ");
    }
    out.println(s);
  }

  private static void println(String s, PrintStream out) {
    if (!printedString.contains(s)) {
      printedString.add(s);
      out.println(s);
    }
  }

  public void dumpXML(GUIAnalysisOutput guiAnalysisOutput, PrintStream out, boolean showDialog, Map<NObjectNode, Set<NObjectNode>> permissionMaps, Set<NObjectNode> upfrontPermissions) {
    int indent = 0;
    printlnIndented(String.format("<GUIHierarchy app=\"%s\">", this.app), out, indent);
    for (Activity activity : activities) {
      activity.dumpXML(indent + indentation, guiAnalysisOutput, out, permissionMaps);
    }
    if (showDialog) {
      for (Dialog dialog : dialogs) {
        dialog.dumpXML(indent + indentation, guiAnalysisOutput, out, permissionMaps);
      }
    }

    printlnIndented("<uses-permissions>", out, indent);
    Set<PermissionNode> usedPermissionsInAppCode = guiAnalysisOutput.getUsedPermissions();
    if (usedPermissionsInAppCode != null
            && usedPermissionsInAppCode.size() > 0) {
      for (PermissionNode perNode : usedPermissionsInAppCode) {
        printlnIndented(String.format("<permission method=\"%s\" handler=\"%s\" name=\"%s\"/>", xmlSafe(perNode.context.getSignature()), xmlSafe(perNode.stmt.toString()), perNode.getPermissionName()), out, indent);
      }
    }
    printlnIndented("</uses-permissions>", out, indent);

    printlnIndented("<request-permission-calls>", out, indent);
    Set<String> requestPermissionStmt = guiAnalysisOutput.getRequestPermissionStmts();
    if (requestPermissionStmt != null
            && requestPermissionStmt.size() > 0) {
      for (String stmt : requestPermissionStmt) {
        printlnIndented(String.format("<request-permission-call handler=\"%s\"/>", xmlSafe(stmt.toString())), out, indent);
      }
    }
    printlnIndented("</request-permission-calls>", out, indent);

    printlnIndented("<upfront-permission-requests>", out, indent);
    if (upfrontPermissions.size() > 0) {
      Set<String> permissions = Sets.newHashSet();
      for (NObjectNode perNode : upfrontPermissions) {
        permissions.add(((NStringConstantNode)perNode).value);
      }
      for (String per : permissions) {
        printlnIndented(String.format("<permission name=\"%s\"/>", xmlSafe(per)), out, indent);
      }
    }
    printlnIndented("</upfront-permission-requests>", out, indent);

    printlnIndented("</GUIHierarchy>", out, indent);
  }

  public static abstract class ViewContainer {
    protected ArrayList<View> views = Lists.newArrayList();

    public void addChild(View v) {
      views.add(v);
    }

    public List<View> getChildren() {
      return views;
    }

    //TODO:
    public List<View> getChildrenCascade() {
      List<View> retList = Lists.newArrayList();
      retList.addAll(this.views);
      for (View v : this.views) {
        retList.addAll(v.getChildrenCascade());
      }
      return retList;
    }
  }

  public static class EventAndHandler {
    protected String event;
    protected String handler;
    public SootMethod sootMethod;

    public String getEvent() {
      return event;
    }

    public String getHandler() {
      return handler;
    }

    //TODO:
    public SootMethod getEventHandlerMethod() {
      return sootMethod;
    }
  }

  public static class View extends ViewContainer {
    protected String type;
    protected int id;
    protected String idName;
    public NObjectNode viewNode;

    protected Set<String> title = Sets.newHashSet();

    protected Set<String> feedbacks = Sets.newHashSet();

    //public String text;
    protected Set<String> hint = Sets.newHashSet();

    protected Set<String> tooltips = Sets.newHashSet();

    protected Set<String> contentDescriptions = Sets.newHashSet();

    protected Set<String> imageResources = Sets.newHashSet();

    protected ArrayList<EventAndHandler> eventAndHandlers = Lists.newArrayList();

    public void addEventAndHandlerPair(EventAndHandler eventAndHandler) {
      eventAndHandlers.add(eventAndHandler);
    }

    public String getType() {
      return type;
    }

    public int getId() {
      return id;
    }

    public String getIdName() {
      return idName;
    }

    public Set<String> getTitle() {
      return title;
    }

    public Set<String> getFeedbacks() {
      return feedbacks;
    }

    public Set<String> getHint() {
      return hint;
    }

    public String getViewTexts() {
      String texts = "";
      {
        if (this.title != null && type.contains("MenuItem")) {
          for (String t : this.title) {
            for (String s : t.split(PropertyManager.SEPARATOR)) {
              texts += xmlSafe(s) + " ";
            }
          }
        } else if (this.title != null) { // reuse title for text of other views
          for (String t : this.title) {
            for (String s : t.split(PropertyManager.SEPARATOR)) {
              texts += xmlSafe(s) + " ";
            }
          }
        }
        if (this.hint != null) {
          for (String h : this.hint) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              texts += xmlSafe(s) + " ";
            }
          }
        }
        if (this.tooltips != null) {
          for (String h : this.tooltips) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              texts += xmlSafe(s) + " ";
            }
          }
        }
        if (this.contentDescriptions != null) {
          for (String h : this.contentDescriptions) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              texts += xmlSafe(s) + " ";
            }
          }
        }
      }
      return texts;
    }

    public String getImages() {
      Set<String> images = Sets.newHashSet();
      if (this.imageResources != null) {
        for (String h : this.imageResources) {
          for (String s : h.split(PropertyManager.SEPARATOR)) {
            images.add(xmlSafe(s));
          }
        }
      }
      return String.join(",", images);
    }

    public String getViewFeedbacks(){
      String texts = "";
      if (this.feedbacks != null) {
        for (String t : this.feedbacks) {
          for (String s : t.split(PropertyManager.SEPARATOR)) {
            texts += xmlSafe(s) + " ";
          }
        }
      }

      return texts;
    }

    public void dumpXML(int indent, GUIAnalysisOutput guiAnalysisOutput, PrintStream out, Map<NObjectNode, Set<NObjectNode>> permissionMaps) {
      String type = String.format(" type=\"%s\"", this.type);
      String id = String.format(" id=\"%d\"", this.id);
      String idName = String.format(" idName=\"%s\"", this.idName);
      // TODO(tony): add the text attribute for TextView and so on
      String head = String.format("<View%s%s%s>", type, id, idName);
      printlnIndented(head, out, indent);

      {
        indent += indentation;
        if (this.title != null && type.contains("MenuItem")) {
          for (String t : this.title) {
            for (String s : t.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Title>%s</Title>", xmlSafe(s)), out, indent);
            }
          }
        } else if (this.title != null) { // reuse title for text of other views
          for (String t : this.title) {
            for (String s : t.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Text>%s</Text>", xmlSafe(s)), out, indent);
            }
          }
        }
        if (this.feedbacks != null) {
          for (String t : this.feedbacks) {
            for (String s : t.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Feedback>%s</Feedback>", xmlSafe(s)), out, indent);
            }
          }
        }
        if (this.hint != null) {
          for (String h : this.hint) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Hint>%s</Hint>", xmlSafe(s)), out, indent);
            }
          }
        }
        if (this.tooltips != null) {
          for (String h : this.tooltips) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Tooltip>%s</Tooltip>", xmlSafe(s)), out, indent);
            }
          }
        }
        if (this.contentDescriptions != null) {
          for (String h : this.contentDescriptions) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<ContentDescription>%s</ContentDescription>", xmlSafe(s)), out, indent);
            }
          }
        }
        if (this.imageResources != null) {
          for (String h : this.imageResources) {
            for (String s : h.split(PropertyManager.SEPARATOR)) {
              printlnIndented(String.format("<Image>%s</Image>", xmlSafe(s)), out, indent);
            }
          }
        }

        indent -= indentation;
      }

      if (!this.getChildren().isEmpty()) {
        indent += indentation;
        printlnIndented("<Child>", out, indent);
        // This includes both children and context menus
        for (View child : this.getChildren()) {
          child.dumpXML(indent + indentation, guiAnalysisOutput, out, permissionMaps);
        }
        printlnIndented("</Child>", out, indent);
        indent -= indentation;
      }

      {
        // Events and handlers
        for (EventAndHandler eventAndHandler : this.eventAndHandlers) {
          String handler = eventAndHandler.getHandler();
          String safeRealHandler = "";
          if (handler.startsWith("<FakeName_")) {
            SootMethod fake = Scene.v().getMethod(handler);
            SootMethod real = guiAnalysisOutput.getRealHandler(fake);
            safeRealHandler = String.format(
                    " realHandler=\"%s\"", xmlSafe(real.getSignature()));
          }
          printlnIndented(String.format("<EventAndHandler event=\"%s\" handler=\"%s\"%s />",
                  eventAndHandler.getEvent(), xmlSafe(eventAndHandler.getHandler()),
                  safeRealHandler), out, indent + indentation);
        }
      }

      {
        Set<String> permissionRequests = Sets.newHashSet();
        Set<NObjectNode> permissionNodes = permissionMaps.get(this.viewNode);
        if (permissionNodes != null) {
          for (NObjectNode node : permissionNodes) {
            if (node instanceof PermissionNode) {
              PermissionNode permissionNode = (PermissionNode) node;
              printlnIndented(String.format("<PermissionAPI method=\"%s\" handler=\"%s\" name=\"%s\"/>",
                      xmlSafe(permissionNode.context.getSignature()), xmlSafe(permissionNode.sig),
                      permissionNode.getPermissionName()), out, indent + indentation);
            } else if (node instanceof NStringConstantNode) {
              permissionRequests.add(((NStringConstantNode) node).value.replace("android.permission.",""));
            }
          }
        }

        for (String per : permissionRequests) {
          printlnIndented(String.format("<PermissionRequest name=\"%s\"/>",
                  xmlSafe(per)), out, indent + indentation);
        }
      }

      if (this.imageResources != null) {
        for (String h : this.imageResources) {
          for (String s : h.split(PropertyManager.SEPARATOR)) {
            XmlHelper.dumpImage(s, Configs.resourceLocation, Configs.pathoutfilename + Configs.benchmarkName + "/", this.type);
          }
        }
      }

      printlnIndented("</View>", out, indent);
    }

  }

  public static class Window extends ViewContainer {
    protected String name;

    public String getName() {
      return name;
    }
  }

  public static class Activity extends Window {

    protected String text;

    public String getName() {
      return text;
    }

    public void dumpXML(int indent, GUIAnalysisOutput guiAnalysisOutput, PrintStream out, Map<NObjectNode, Set<NObjectNode>> permissionMaps) {
      printlnIndented(String.format("<Activity name=\"%s\">", this.name), out, indent);
      for (View rootView : this.getChildren()) {
        rootView.dumpXML(indent + indentation, guiAnalysisOutput, out, permissionMaps);
      }
      printlnIndented("</Activity>", out, indent);
    }

  }

  public static class Dialog extends Window {
    protected int allocLineNumber;
    protected String allocStmt;
    protected String allocMethod;
    protected Set<NObjectNode> triggerWidgets = Sets.newHashSet();

    public int getAllocLineNumber() {
      return allocLineNumber;
    }

    public String getAllocStmt() {
      return allocStmt;
    }

    public String getAllcMethod() {
      return allocMethod;
    }

    public void addTriggerWidget(NObjectNode widget) {
      this.triggerWidgets.add(widget);
    }

    public String getTriggerWidgetIds() {
      Set<String> triggerWidgetIds = Sets.newHashSet();
      if (triggerWidgets.size() > 0) {
        for (NObjectNode objectNode : triggerWidgets) {
          if (objectNode.idNode != null) {
            triggerWidgetIds.add(objectNode.idNode.getIdName());
          }
        }
      }
      return StringUtils.join(triggerWidgetIds, ",");
    }

    public void dumpXML(int indent, GUIAnalysisOutput output, PrintStream out, Map<NObjectNode, Set<NObjectNode>> permissionMaps) {

      printlnIndented(String.format("<Dialog name=\"%s\" allocLineNumber=\"%d\" allocStmt=\"%s\" allocMethod=\"%s\" triggerWidgets=\"%s\">",
              this.name, this.allocLineNumber, xmlSafe(this.allocStmt), xmlSafe(this.allocMethod), xmlSafe(getTriggerWidgetIds())), out, indent);

      for (View rootView : this.getChildren()) {
        rootView.dumpXML(indent + indentation, output, out, permissionMaps);
      }
      printlnIndented("</Dialog>", out, indent);
    }

  }

  public static String xmlSafe(String s) {
    return s.replaceAll("%", "%%")
            .replaceAll("&", "&amp;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&apos;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");

  }
}
