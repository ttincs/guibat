/*
 * Algorithm.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package presto.android.gui.wtg.algo;

public interface Algorithm {
  AlgorithmOutput execute(AlgorithmInput input);
}
