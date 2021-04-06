/*
 * NPreferencesFromResourceOpNode.java - part of the GATOR project
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

public class NPreferencesFromResourceOpNode extends NOpNode {
    public Value preference;
    public Integer resourceId;

    public NPreferencesFromResourceOpNode(Pair<Stmt, SootMethod> callSite, Value preference, Integer resourceId, boolean artificial) {
        super(callSite, artificial);
        this.preference = preference;
        this.resourceId = resourceId;
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
