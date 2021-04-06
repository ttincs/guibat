/*
 * NFragmentTransactionOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NFragmentTransactionOpNode extends NOpNode {

    public Value fragment;
    public Value container;

    public NFragmentTransactionOpNode(Pair<Stmt, SootMethod> callSite, Value fragment, Value container, boolean artificial) {
        super(callSite, artificial);
        this.container = container;
        this.fragment = fragment;
    }

    @Override
    public boolean hasReceiver() {
        return false;
    }

    @Override
    public boolean hasParameter() {
        return false;
    }

    @Override
    public boolean hasLhs() {
        return false;
    }
}
