package spoon.reflect.visitor;

import static java.util.Arrays.asList;

import spoon.compiler.Environment;

public class PrettyPrinterCreator {

    public static PrettyPrinter createPrettyPrinter(final Environment environment) {
        final DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(environment);
        printer.setIgnoreImplicit(false);
        printer.setPreprocessors(asList(new ForceFullyQualifiedProcessor(), new RemoveAllImportsCleaner()));
        return printer;
    }
}
