/*
 * TestMain.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android;

import org.junit.Test;
import soot.jimple.infoflow.android.iccta.Ic3Provider;
import soot.jimple.infoflow.android.iccta.IccLink;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

public class TestMain {
  @Test
  public void testMain() throws Exception {
    String home = System.getProperty("user.home");
    String ADK = home + "/Library/Android/Sdk";
    String apk = home + "/Downloads/app-debug.apk";
    String resDir = home + "/Downloads/app-debug/app-debug";
    String apk_name = "app-debug.apk";

    String APKTOOL = System.getProperty("user.dir") + "/../tools/apktool_2.4.0.jar";

    runProcess("java -Xmx8g -jar " + APKTOOL + " d -f -o " + resDir + " " + apk);

    String[] args = new String[]{
            "-project", home + "/develop/guibat/gator/AndroidBench/evaluate_apks/a2dp.Vol.apk",
            "-apiLevel", "android-30",
            "-manifestFile", resDir + "/AndroidManifest.xml",
            "-resourcePath", resDir + "/res",
            "-guiAnalysis",
            "-sootandroidDir", System.getProperty("user.dir"),
            "-benchmarkName", apk_name,
            "-sdkDir", ADK,
            "-android", ADK + "/platforms/android-30/android.jar",
            "-listenerSpecFile", "listeners.xml",
            "-wtgSpecFile", "wtg.xml",
            "-clientParam", "printXml",
            "-client", "GUIHierarchyPrinterClient",
            "-enableSetTextAnalysis",
            //"-enableStringAnalysis",
            "-permissionMapFile","protected_apis.txt",
            "-outputFile", home + "/Downloads/dump/",
            "-minSdkVersion","1",
            "-maxSdkVersion","2",
            "-targetSdkVersion","3"
    };

    Main.main(args);

    runProcess("rm -rf " + resDir);
  }

  private static void printLines(PrintStream stream, InputStream ins) throws Exception {
    String line = null;
    BufferedReader in = new BufferedReader(
            new InputStreamReader(ins));
    while ((line = in.readLine()) != null) {
      stream.println(line);
    }
  }

  private static void runProcess(String command) throws Exception {
    Process pro = Runtime.getRuntime().exec(command);
    printLines(System.out, pro.getInputStream());
    printLines(System.err, pro.getErrorStream());
    pro.waitFor();
  }
}
