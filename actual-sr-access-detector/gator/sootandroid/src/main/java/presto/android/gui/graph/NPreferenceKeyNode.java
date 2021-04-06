/*
 * NPreferenceKeyNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

public class NPreferenceKeyNode extends NNode {
    public String key;

    public NPreferenceKeyNode(String key) {
        if (key == null) {
            throw new RuntimeException("Null key!");
        }
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
