/*
 * ImageResourceParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.resources;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import presto.android.util.XmlHelper;

import java.io.IOException;
import java.util.*;

public class ImageResourceParser {
    private ARSCFileParser parser;
    private String resourceDirectory;
    private Collection<ARSCFileParser.AbstractResource> drawableResources = new ArrayList<>();
    private HashMap<String, ArrayList<ARSCFileParser.StringResource>> imageResources = Maps.newHashMap();
    private HashMap<String, Boolean> vectorDrawableResources = Maps.newHashMap();

    public ImageResourceParser(String project, String resourceDirectory) throws IOException {
        this.parser = new ARSCFileParser();
        this.parser.parse(project);
        this.resourceDirectory = resourceDirectory;
        parseImageResources();
    }

    public Set<String> getImageFiles(int resId) {
        Set<String> imageFiles = new HashSet<>();
        Collection<ARSCFileParser.AbstractResource> resourceNames = new ArrayList<>();

        for (ARSCFileParser.ResPackage resPackage : this.parser.getPackages())
        {
            for (ARSCFileParser.ResType resType : resPackage.getDeclaredTypes()) {
                if (resType.getTypeName().contentEquals("drawable"))
                {
                    resourceNames = resType.getAllResources(resId);
                }
            }
        }

        for (ARSCFileParser.AbstractResource resource : resourceNames) {
            imageFiles.addAll(getImageFiles(resource.getResourceName()));
        }

        return imageFiles;
    }

    public Set<String> getImageFiles(String resourceName) {
        if (!this.imageResources.containsKey(resourceName))
            return Sets.newHashSet();

        ArrayList<ARSCFileParser.StringResource> resources = this.imageResources.get(resourceName);
        Set<String> imageFiles = new HashSet<>();
        for (ARSCFileParser.StringResource stringResource : resources) {
            String imageFile = stringResource.getValue();
            if(!imageFile.endsWith(".xml") || isVectorDrawable(imageFile)) {
                imageFiles.add(stringResource.getValue());
            }
        }

        return imageFiles;
    }

    private void parseImageResources() {
        for (ARSCFileParser.ResPackage resPackage : this.parser.getPackages())
        {
            for (ARSCFileParser.ResType resType : resPackage.getDeclaredTypes()) {
                if (resType.getTypeName().contentEquals("drawable"))
                {
                    for (ARSCFileParser.ResConfig config : resType.getConfigurations())
                    {
                        for (ARSCFileParser.AbstractResource resource : config.getResources()) {
                            this.drawableResources.add(resource);
                        }
                    }
                    break;
                }
            }
        }

        // Find all drawables are bitmap image
        for (ARSCFileParser.AbstractResource resource : drawableResources) {
            if (resource instanceof ARSCFileParser.StringResource) {
                ARSCFileParser.StringResource stringResource = (ARSCFileParser.StringResource) resource;

                if (!stringResource.getValue().endsWith(".xml") && !resource.getResourceName().startsWith("abc_")) {
                    ArrayList<ARSCFileParser.StringResource> newImageResources = new ArrayList<>();
                    if (this.imageResources.containsKey(stringResource.getResourceName())) {
                        newImageResources.addAll(this.imageResources.get(stringResource.getResourceName()));
                        newImageResources.add(stringResource);
                        this.imageResources.put(stringResource.getResourceName(), newImageResources);
                    } else {
                        newImageResources.add(stringResource);
                        this.imageResources.put(stringResource.getResourceName(), newImageResources);
                    }
                }
            }
        }

        // Find the image of xml drawable
        for (ARSCFileParser.AbstractResource resource : drawableResources) {
            if (resource instanceof ARSCFileParser.StringResource) {
                ARSCFileParser.StringResource stringResource = (ARSCFileParser.StringResource) resource;

                if (stringResource.getValue().endsWith(".xml") && !resource.getResourceName().startsWith("abc_")) {
                    this.Current_Recursive_Level = 1;
                    Set<String> imageResources = readRecursiveImagePathFromXmlDrawableResource(stringResource.getValue());
                    ArrayList<ARSCFileParser.StringResource> newImageResources = new ArrayList<>();

                    for (String imageName : imageResources) {
                        newImageResources.addAll(this.imageResources.get(imageName));
                    }

                    if (this.imageResources.containsKey(stringResource.getResourceName())) {
                        newImageResources.addAll(this.imageResources.get(stringResource.getResourceName()));
                        this.imageResources.put(stringResource.getResourceName(), newImageResources);
                    } else {
                        newImageResources.add(stringResource);
                        this.imageResources.put(stringResource.getResourceName(), newImageResources);
                    }
                }
            }
        }

    }

    private final int Max_Recursive_Level = 10;
    private int Current_Recursive_Level = 1;
    private Set<String> readRecursiveImagePathFromXmlDrawableResource(String xmlFile) {
        Set<String> imagePaths = new HashSet<String>();
        Set<String> drawables = readDrawableFromXmlDrawableResource(xmlFile);

        this.Current_Recursive_Level += 1;
        if (this.Current_Recursive_Level > Max_Recursive_Level) {
            return imagePaths;
        }

        if (drawables == null)
            return new HashSet<>();
        for (String drawable : drawables) {
            if (this.imageResources.containsKey(drawable)) {
                imagePaths.add(drawable);
            }
            else {
                for (ARSCFileParser.AbstractResource resource : drawableResources) {
                    if (resource instanceof ARSCFileParser.StringResource) {
                        ARSCFileParser.StringResource stringResource = (ARSCFileParser.StringResource) resource;

                        if (stringResource.getResourceName().contentEquals(drawable)) {
                            imagePaths.addAll(readRecursiveImagePathFromXmlDrawableResource(stringResource.getValue()));
                        }
                    }
                }
            }
        }

        return imagePaths;
    }

    private Set<String> readDrawableFromXmlDrawableResource(String xmlFile) {
        Document document = XmlHelper.readXml(this.resourceDirectory, xmlFile);
        if (document != null) {
            return readRecursiveDrawableAttributeFromXmlDrawableResource(document);
        }

        return null;
    }


    private Set<String> readRecursiveDrawableAttributeFromXmlDrawableResource(Node node) {
        Set<String> attributeValues = new HashSet<String>();
        NamedNodeMap nodeAttributes = node.getAttributes();
        String attributeValue = readDrawableAttributeValue(nodeAttributes, "android:src");

        if (attributeValue == null) {
            attributeValue = readDrawableAttributeValue(nodeAttributes, "android:drawable");
        }

        if (attributeValue != null) {
            attributeValues.add(attributeValue);
        }

        NodeList childNodes = node.getChildNodes();
        if (childNodes.getLength() > 0) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode instanceof DeferredElementImpl) {
                    attributeValues.addAll(readRecursiveDrawableAttributeFromXmlDrawableResource(childNode));
                }
            }
        }

        return attributeValues;
    }

    private static final String DRAWABLE_ATT = "@drawable/";

    private String readDrawableAttributeValue(NamedNodeMap namedNodeMap, String name) {
        if (namedNodeMap != null && namedNodeMap.getLength() >= 0) {
            Node attr = namedNodeMap.getNamedItem(name);

            if (attr != null) {
                String attrValue = attr.getTextContent();
                if (attrValue != null
                        && !attrValue.isEmpty()
                        && attrValue.length() > DRAWABLE_ATT.length()) {
                    return attrValue.substring(DRAWABLE_ATT.length());
                }
            }
        }

        return null;
    }

    private boolean isVectorDrawable(String xmlFile) {
        Boolean isVectorXml = vectorDrawableResources.get(xmlFile);
        if (isVectorXml != null)
        {
            return isVectorXml;
        }

        Document document = XmlHelper.readXml(this.resourceDirectory, xmlFile);

        if (document != null) {
            isVectorXml = document.getElementsByTagName("vector") != null;
            vectorDrawableResources.put(xmlFile, isVectorXml);
            return isVectorXml;
        }

        return false;
    }
}
