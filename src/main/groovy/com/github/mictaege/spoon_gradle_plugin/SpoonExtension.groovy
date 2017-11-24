package com.github.mictaege.spoon_gradle_plugin

import java.util.function.Function

class SpoonExtension {

    /** List of processor's qualified name to be used. */
    String[] processors = []

    /** A provider of lazy evaluated extensions. */
    LazySpoonExtensionProvider lazyExtensions = null

    /** List of excluded source sets. */
    String[] exclude = []

    /** Java source code compliance level (1,2,3,4,5, 6, 7 or 8). (default: 8) */
    int compliance = 8

    /**
     * A filter closure to specify which source files should be spooned. (default: all source files)
     *
     *  Example:
     *  <pre>fileFilter = { File src -> src.text.contains("org.junit.Test") }</pre>
     */
    Function<File, Boolean> fileFilter = { File src -> true }

}
