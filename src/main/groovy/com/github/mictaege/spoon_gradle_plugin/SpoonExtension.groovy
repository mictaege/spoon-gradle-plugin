package com.github.mictaege.spoon_gradle_plugin

class SpoonExtension {

    /** Set Spoon to build only the source files that have been modified since the latest source code generation, for performance purpose. (default: true) */
    boolean buildOnlyOutdatedFiles = true

    /** List of processor's qualified name to be used. */
    String[] processors = []

    /** List of excluded source sets. */
    String[] exclude = []

    /** Java source code compliance level (1,2,3,4,5, 6, 7 or 8). (default: 8) */
    int compliance = 8

}
