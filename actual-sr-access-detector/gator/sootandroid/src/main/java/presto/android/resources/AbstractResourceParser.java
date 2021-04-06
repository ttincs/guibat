/*
 * AbstractResourceParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Common base class for all resource parser classes
 *
 * @author Steven Arzt
 */
public abstract class AbstractResourceParser {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Opens the given apk file and provides the given handler with a stream for
     * accessing the contained resource manifest files
     *
     * @param apk
     *            The apk file to process
     * @param fileNameFilter
     *            If this parameter is non-null, only files with a name
     *            (excluding extension) in this set will be analyzed.
     * @param handler
     *            The handler for processing the apk file
     */
    protected void handleAndroidResourceFiles(String apk, Set<String> fileNameFilter, IResourceHandler handler) {
        File apkF = new File(apk);
        if (!apkF.exists())
            throw new RuntimeException("file '" + apk + "' does not exist!");

        try {
            ZipFile archive = null;
            try {
                archive = new ZipFile(apkF);
                Enumeration<?> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String entryName = entry.getName();

                    InputStream is = null;
                    try {
                        is = archive.getInputStream(entry);
                        handler.handleResourceFile(entryName, fileNameFilter, is);
                    } finally {
                        if (is != null)
                            is.close();
                    }
                }
            } finally {
                if (archive != null)
                    archive.close();
            }
        } catch (Exception e) {
            logger.error("Error when looking for XML resource files in apk " + apk, e);
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new RuntimeException(e);
        }
    }

}
