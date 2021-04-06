/*
 * GUIHierarchyPrinterClient.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package presto.android.gui.clients;

import org.apache.commons.io.FileUtils;
import presto.android.Configs;
import presto.android.Logger;
import presto.android.gui.GUIAnalysisClient;
import presto.android.gui.GUIAnalysisOutput;
import presto.android.gui.graph.NObjectNode;
import presto.android.gui.rep.GUIHierarchy;
import presto.android.gui.rep.StaticGUIHierarchy;
import presto.android.permission.PermissionBuilder;

import java.io.File;
import java.io.PrintStream;
import java.util.Set;

public class GUIHierarchyPrinterClient implements GUIAnalysisClient {
  private String TAG = GUIHierarchyPrinterClient.class.getSimpleName();
  GUIAnalysisOutput output;
  GUIHierarchy guiHier;

  private PrintStream out = null;

  @Override
  public void run(GUIAnalysisOutput output) {
    this.output = output;
    guiHier = new StaticGUIHierarchy(output);

    PermissionBuilder permissionBuilder = new PermissionBuilder(guiHier);
    permissionBuilder.build();
    Set<NObjectNode> upFrontRequestPermissions = permissionBuilder.getUpFrontRequestPermissions();

    boolean showDialog = true;
    boolean printXml = false;

    // Init the file io
    for (String param : Configs.clientParams) {
      if (param.equals("print2stdout")) {
        out = System.out;
      } else if (param.equals("nodialog")) {
        showDialog = false;
      } else if (param.equals("printXml")) {
        printXml = true;
      }
    }
    if (out == null && printXml) {
      try {
        File outputDir = new File(Configs.pathoutfilename);
        outputDir.mkdirs();
        File file = new File(Configs.pathoutfilename + Configs.benchmarkName + ".xml");

        Logger.verb(TAG, "XML file: " + file.getAbsolutePath());
        out = new PrintStream(file);

        guiHier.dumpXML(output, out, showDialog, permissionBuilder.permissionMaps, upFrontRequestPermissions);
        out.flush();
        out.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
