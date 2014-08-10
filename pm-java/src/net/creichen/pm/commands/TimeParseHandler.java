package net.creichen.pm.commands;

import java.util.Set;

import net.creichen.pm.utils.ASTUtil;
import net.creichen.pm.utils.Timer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class TimeParseHandler extends AbstractCommandHandler {

    private static final int REPETITIONS = 10;

    @Override
    public final void handleEvent(final ExecutionEvent event) {
        for (int i = 0; i < REPETITIONS; i++) {

            Timer.sharedTimer().start("JUST_PARSE");

            // project.updateToNewVersionsOfICompilationUnits();

            parseSource(false);

            Timer.sharedTimer().stop("JUST_PARSE");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey("JUST_PARSE");

            System.out.println("Time to just parse is " + elapsedSeconds);

            Timer.sharedTimer().clear("JUST_PARSE");
        }

        for (int i = 0; i < REPETITIONS; i++) {

            Timer.sharedTimer().start("PARSE_BINDINGS");

            // project.updateToNewVersionsOfICompilationUnits();

            parseSource(true);

            Timer.sharedTimer().stop("PARSE_BINDINGS");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey("PARSE_BINDINGS");

            System.out.println("Time to just parse with bindings is " + elapsedSeconds);

            Timer.sharedTimer().clear("PARSE_BINDINGS");
        }

        for (int i = 0; i < REPETITIONS; i++) {

            Timer.sharedTimer().start("PARSE_BINDINGS_UPDATE");

            // project.updateToNewVersionsOfICompilationUnits();

            getProject().updateToNewVersionsOfICompilationUnits();

            Timer.sharedTimer().stop("PARSE_BINDINGS_UPDATE");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey("PARSE_BINDINGS_UPDATE");

            System.out.println("Time parse and update model is " + elapsedSeconds);

            Timer.sharedTimer().clear("PARSE_BINDINGS_UPDATE");

            System.out.println("Model equivalence time is "
                    + Timer.sharedTimer().accumulatedSecondsForKey("INCONSISTENCIES"));

            Timer.sharedTimer().clear("INCONSISTENCIES");

            System.out.println("DU/UD time is " + Timer.sharedTimer().accumulatedSecondsForKey("DUUD_CHAINS"));

            Timer.sharedTimer().clear("DUUD_CHAINS");

            // System.out.println("NODE_REPLACEMENT time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("NODE_REPLACEMENT"));
            // PMTimer.sharedTimer().clear("NODE_REPLACEMENT");

            System.out.println("PARSE_INTERNAL time is "
                    + Timer.sharedTimer().accumulatedSecondsForKey("PARSE_INTERNAL"));
            Timer.sharedTimer().clear("PARSE_INTERNAL");

            // System.out.println("PUT_HASH time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("PUT_HASH"));
            // PMTimer.sharedTimer().clear("PUT_HASH");

            // System.out.println("SUBTREE_BYTES time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("SUBTREE_BYTES"));
            // PMTimer.sharedTimer().clear("SUBTREE_BYTES");
        }
    }

    private void parseSource(final boolean resolveBindings) {
        final Set<ICompilationUnit> iCompilationUnits = ASTUtil
                .getSourceFilesForProject(getProject().getIJavaProject());

        final ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setProject(getProject().getIJavaProject());
        parser.setResolveBindings(resolveBindings);
        parser.createASTs(iCompilationUnits.toArray(new ICompilationUnit[iCompilationUnits.size()]), new String[0],
                new ASTRequestor() {
            @Override
            public void acceptAST(final ICompilationUnit source, final CompilationUnit ast) {
            }
        }, null);
    }

}
