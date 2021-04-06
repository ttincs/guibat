/*
 * PermissionNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.permission;

import presto.android.gui.graph.NObjectNode;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

public class PermissionNode extends NObjectNode {
    public SootMethod context;
    public Stmt stmt;
    public String sig;
    public Integer depthLevel;
    String permission = null;
    PermissionMap permissionMap = PermissionMap.v();

    public PermissionNode(SootMethod context, Stmt stmt, String sig, Integer depthLevel) {
        this.context = context;
        this.stmt = stmt;
        this.sig = sig;
        this.depthLevel = depthLevel;
    }

    public String getPermissionName() {
        if (permission == null) {
            permission = permissionMap.getPermissionName(sig);
        }
        return permission;
    }

    public String getPermissionGroupName() {
        return permissionMap.getPermissionGroup(getPermissionName());
    }

    @Override
    public SootClass getClassType() {
        return null;
    }

    @Override
    public int hashCode() {
        return context.toString().hashCode() * stmt.toString().hashCode() * sig.hashCode();
    }
}
