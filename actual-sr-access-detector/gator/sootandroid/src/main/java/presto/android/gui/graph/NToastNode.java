/*
 * NToastNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.Scene;
import soot.SootClass;

public class NToastNode extends NObjectNode {
    @Override
    public SootClass getClassType() {
        return Scene.v().getSootClass("android.widget.Toast");
    }
}
