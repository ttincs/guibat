/*
 * NFragmentNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.SootClass;

public class NFragmentNode extends NObjectNode {
    public SootClass ownerActivity;

    public SootClass fragmentClass;

    public Integer parentLayoutId;

    @Override
    public String toString() {
        return "FRAGMENT[" + fragmentClass + "]" + id;
    }

    @Override
    public SootClass getClassType() {
        return fragmentClass;
    }
}