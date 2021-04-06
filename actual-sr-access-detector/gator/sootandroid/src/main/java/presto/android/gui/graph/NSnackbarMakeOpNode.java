/*
 * NSnackbarMakeOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NSnackbarMakeOpNode extends NOpNode {
    public NNode textNode;

    public NSnackbarMakeOpNode(NNode textNode, Pair<Stmt, SootMethod> callSite, boolean artificial) {
        super(callSite, artificial);
        this.textNode = textNode;
    }

    @Override
    public boolean hasReceiver() {
        return false;
    }

    @Override
    public boolean hasParameter() {
        return true;
    }

    @Override
    public boolean hasLhs() {
        return true;
    }
}
