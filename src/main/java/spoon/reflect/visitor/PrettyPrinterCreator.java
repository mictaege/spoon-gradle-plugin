package spoon.reflect.visitor;

import static java.util.Arrays.asList;

import spoon.compiler.Environment;

public class PrettyPrinterCreator {

    private static DefaultJavaPrettyPrinter printer;
    
    public static PrettyPrinter createPrettyPrinter(final Environment environment) {
        if (printer == null) {
            printer = new DefaultJavaPrettyPrinter(environment);
            printer.setIgnoreImplicit(false);
            printer.setPreprocessors(asList(new ForceFullyQualifiedProcessor(), new RemoveAllImportsCleaner()));
        }
        return printer;
    }
}
