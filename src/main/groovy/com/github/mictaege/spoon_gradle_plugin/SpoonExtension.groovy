package com.github.mictaege.spoon_gradle_plugin

import java.util.function.Function

class SpoonExtension {

    /** List of processor's qualified name to be used. */
    String[] processors = []

    /** A provider of lazy evaluated extensions. */
    LazySpoonExtensionProvider lazyExtensions = null

    /** List of excluded source sets. */
    String[] exclude = []

    /** Java source code compliance level (1,2,3,4,5, 6, 7, 8, 11 ...). (default: 11) */
    int compliance = 11

    /**
     * A filter closure to specify which source files should be spooned. (default: all source files)
     *
     *  Example:
     *  <pre>fileFilter = { File src -> src.text.contains("org.junit.Test") }</pre>
     */
    Function<File, Boolean> fileFilter = { File src -> true }

}
