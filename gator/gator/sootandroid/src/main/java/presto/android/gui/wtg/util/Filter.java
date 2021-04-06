/*
 * Filter.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package presto.android.gui.wtg.util;

import soot.SootMethod;
import soot.jimple.Stmt;

public abstract class Filter<E, C> {
  public boolean match(E entity, C context, Integer depthLevel) {
    return false;
  }

  public boolean match(E entity, C context) {
    return false;
  }

  public boolean match(E entity) {
    return false;
  }

  public boolean lookforStartActivity() {
    return false;
  }

  public boolean lookforShowDialog() {
    return false;
  }

  public boolean lookforOpenMenu() {
    return false;
  }

  public boolean lookforFinishActivity() {
    return false;
  }

  public boolean lookforDismissDialog() {
    return false;
  }

  public boolean lookforExitSystem() {
    return false;
  }

  public boolean lookforAcquireResource() {
    return false;
  }

  public boolean lookforReleaseResource() {
    return false;
  }

  // specify if the traversal is meet-and-stop
  // on default it is true
  public boolean isDetectThenStop() {
    return false;
  }

  public boolean lookforICC() { return false;}

  public boolean lookforPermissionProtectedAPI() { return false;}

  public boolean lookforStartService() { return false;}

  public boolean lookforRequestPermission() {return false;}

  public boolean lookforToastCall() {return false;}

  public boolean lookforSnackbarCall() {return false;}

  public static final Filter<Stmt, SootMethod> runtimeRequestPermissionStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforRequestPermission() {
      return true;
    }

    @Override
    public boolean isDetectThenStop() {
      return false;
    }
  };

  public static final Filter<Stmt, SootMethod> permissionStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean isDetectThenStop() {
      return false;
    }

    @Override
    public boolean lookforPermissionProtectedAPI() {
      return true;
    }

    @Override
    public boolean lookforRequestPermission() {
      return true;
    }
  };

  public static final Filter<Stmt, SootMethod> startActivityAndServiceStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean isDetectThenStop() {
      return false;
    }

    @Override
    public boolean lookforStartActivity() {
      return true;
    }

    @Override
    public boolean lookforStartService() {
      return true;
    }

    @Override
    public boolean lookforShowDialog() {
      return true;
    }

    @Override
    public boolean lookforToastCall() { return true;}

    @Override
    public boolean lookforSnackbarCall() {
      return true;
    }
  };

  public static final Filter<Stmt, SootMethod> icc3StmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean isDetectThenStop() {
      return false;
    }

    @Override
    public boolean lookforICC() {
      return true;
    }
  };

  // specify the stmts we are interested: showDialog, startActivity and openMenu
  public static final Filter<Stmt, SootMethod> openWindowStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforStartActivity() {
      return true;
    }

    @Override
    public boolean lookforShowDialog() {
      return true;
    }

    @Override
    public boolean lookforOpenMenu() {
      return true;
    }
  };

  // specify the stmts we are interested: showDialog, startActivity
  public static final Filter<Stmt, SootMethod> openActivityDialogStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforStartActivity() {
      return true;
    }

    @Override
    public boolean lookforShowDialog() {
      return true;
    }
  };

  // specify the stmts we are interested: Activity.finish
  public static final Filter<Stmt, SootMethod> closeActivityStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforFinishActivity() {
      return true;
    }
  };

  // specify the stmts we are interested: Activity.finish
  public static final Filter<Stmt, SootMethod> closeDialogStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforDismissDialog() {
      return true;
    }
  };

  public static final Filter<Stmt, SootMethod> closeActivitySystemStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforFinishActivity() {
      return true;
    }

    @Override
    public boolean lookforExitSystem() {
      return true;
    }
  };

  public static final Filter<Stmt, SootMethod> closeWindowStmtFilter = new Filter<Stmt, SootMethod>() {
    @Override
    public boolean lookforFinishActivity() {
      return true;
    }

    @Override
    public boolean lookforDismissDialog() {
      return true;
    }

    /*
    @Override
    public boolean lookforExitSystem() {
      return true;
    }
    */

    @Override
    public boolean isDetectThenStop() {
      return false;
    }
  };
}
