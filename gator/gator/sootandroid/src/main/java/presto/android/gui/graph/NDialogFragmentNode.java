/*
 * NDialogFragmentNode.java - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

public class NDialogFragmentNode extends NObjectNode {
    SootClass dialogClass;
    Stmt allocStmt;
    SootMethod allocMethod;
    public NDialogFragmentNode(SootClass dialogClass, Stmt allocStmt, SootMethod allocMethod) {
        this.dialogClass = dialogClass;
        this.allocStmt = allocStmt;
        this.allocMethod = allocMethod;
    }

    @Override
    public SootClass getClassType() {
        return this.dialogClass;
    }
}
