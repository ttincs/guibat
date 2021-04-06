/*
 * NPreferenceIdNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

public class NPreferenceIdNode extends NIdNode {
    public String prefKey;

    public NPreferenceIdNode(Integer i) {
        super(i, "PRE-ID");
    }

    @Override
    public String getIdName() {
        return prefKey;
    }
}