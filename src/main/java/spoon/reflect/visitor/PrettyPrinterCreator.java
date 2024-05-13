package spoon.reflect.visitor;

import static java.util.Arrays.asList;

import spoon.compiler.Environment;

/** PrettyPrinter Factory */
public class PrettyPrinterCreator {

    /** not intended to be instantiated */
    private PrettyPrinterCreator() {
        super();
    }

    /**
     * Create a Pretty Printer
     * @param environment The Environment
     * @return The Pretty Printer
     */
    public static PrettyPrinter createPrettyPrinter(final Environment environment) {
        final DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(environment);
        printer.setIgnoreImplicit(false);
        printer.setPreprocessors(asList(new ForceFullyQualifiedProcessor(), new RemoveAllImportsCleaner()));
        return printer;
    }
}
