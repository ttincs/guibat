/*
 * NServiceNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.SootClass;

public class NServiceNode extends NObjectNode {
    public SootClass c;

    @Override
    public String toString() {
        return "SERVICE[" + c + "]" + id;
    }

    @Override
    public SootClass getClassType() {
        return c;
    }
}
