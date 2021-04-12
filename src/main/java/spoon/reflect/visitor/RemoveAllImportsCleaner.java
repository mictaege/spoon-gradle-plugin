package spoon.reflect.visitor;

import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;


public class RemoveAllImportsCleaner extends ImportAnalyzer<RemoveAllImportsCleaner.Context> {

    @Override
    protected ImportCleanerScanner createScanner() {
        return new ImportCleanerScanner();
    }

    @Override
    protected Context getScannerContextInformation() {
        return ((ImportCleanerScanner) scanner).context;
    }

    @Override
    protected void handleTargetedExpression(CtTargetedExpression<?, ?> targetedExpression, Context context) {
    }

    @Override
    protected void handleTypeReference(CtTypeReference<?> reference, Context context, CtRole role) {
    }

    /** a set of imports for a given compilation unit */
    static class Context {
        void onCompilationUnitProcessed(CtCompilationUnit compilationUnit) {
            compilationUnit.getImports().clear();
        }
    }

    /**
     * A scanner that initializes context for a compilation unit.
     */
    static class ImportCleanerScanner extends EarlyTerminatingScanner<Void> {
        Context context;
        @Override
        protected void enter(CtElement e) {
            if (e instanceof CtCompilationUnit) {
                context = new Context();
            }
        }

        @Override
        protected void exit(CtElement e) {
            if (e instanceof CtCompilationUnit) {
                context.onCompilationUnitProcessed((CtCompilationUnit) e);
            }
        }
    }
}