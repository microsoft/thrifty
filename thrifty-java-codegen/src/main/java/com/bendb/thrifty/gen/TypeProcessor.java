package com.bendb.thrifty.gen;

import com.squareup.javapoet.TypeSpec;

/**
 * When specified as part of code generation, processes all types after they
 * are computed, but before they are written to disk.  This allows you to make
 * arbitrary modifications to types such as implementing your own interfaces,
 * renaming fields, or anything you might wish to do.
 */
public interface TypeProcessor {
    TypeSpec process(TypeSpec type);
}
