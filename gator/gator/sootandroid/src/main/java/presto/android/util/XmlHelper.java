/*
 * XmlHelper.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.util;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import presto.android.Logger;
import presto.android.gui.PropertyManager;
import soot.Scene;
import soot.SootClass;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class XmlHelper {
    public static Document readXml(String parentDirectory, String xmlFile) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            String filePath = parentDirectory.substring(0, parentDirectory.lastIndexOf("/") + 1) + xmlFile;
            File file = new File(filePath);

            if (file.exists()) {
                Document document = documentBuilder.parse(file);
                document.getDocumentElement().normalize();

                return document;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return null;
    }

    public static File searchImage(String filePath, String resourceLocation) {
        try {
            File resDir = new File(resourceLocation);
            String fullPath = resourceLocation.substring(0, resourceLocation.lastIndexOf("/") + 1) + filePath;
            File file = new File(fullPath);

            if (file.exists()) {
                return file;
            } else {
                for (File resourceDirs : resDir.listFiles()) {
                    if (resourceDirs.getName().startsWith("drawable")) {
                        for (File imageFile : resourceDirs.listFiles()) {
                            if (imageFile.getName().contains(file.getName())) {
                                return imageFile;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    public static void dumpImage(String filePath, String resourceLocation, String outputLocation, String uiType) {
        File imageFile = searchImage(filePath, resourceLocation);

        if (imageFile != null) {
            try {
                boolean isViewGroup = isSubClassOfViewGroup(uiType);
                if (isViewGroup) {
                    FileUtils.copyFile(imageFile, new File(outputLocation + "android.view.ViewGroup" + PropertyManager.SEPARATOR + imageFile.getName()));
                } else {
                    FileUtils.copyFile(imageFile, new File(outputLocation + imageFile.getName()));
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    static boolean isSubClassOfViewGroup(String type) {
        SootClass high = Scene.v().getSootClass("android.view.ViewGroup");
        SootClass low = Scene.v().getSootClass(type);
        return isCompatible(high, low);
    }

    static boolean isCompatible(SootClass high, SootClass low) {
        if (high == null && low != null) {
            Logger.verb("ERROR", "in isCompatible, high is null, low is " + low.toString());
        } else if (low == null && high != null) {
            Logger.verb("ERROR", "in isCompatible, low is null, high is " + high.toString());
        } else if (low == null && high == null) {
            Logger.verb("ERROR", "in isCompatible, both is null");
        }
        if (high.equals(low)) {
            return true;
        }
        if (low.hasSuperclass()) {
            SootClass parent = low.getSuperclass();
            if (isCompatible(high, parent)) {
                return true;
            }
        }
        for (SootClass parent : low.getInterfaces()) {
            if (isCompatible(high, parent)) {
                return true;
            }
        }
        return false;
    }
}
