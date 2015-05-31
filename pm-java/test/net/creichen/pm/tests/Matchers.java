package net.creichen.pm.tests;

import java.util.Collection;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.core.Project;
import net.creichen.pm.models.function.FunctionModel;
import net.creichen.pm.models.function.FunctionModelEquivalenceChecker;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.hamcrest.*;

public final class Matchers {

    private Matchers() {
    }

    private static final TypeSafeDiagnosingMatcher<Project> HAS_NO_INCONSISTENCIES = new TypeSafeDiagnosingMatcher<Project>() {

        @Override
        public void describeTo(final Description description) {
            description.appendText("a Project containing no inconsistencies");
        }

        @Override
        protected boolean matchesSafely(final Project item, final Description mismatchDescription) {
            ConsistencyValidator validator = new ConsistencyValidator();
            validator.rescanForInconsistencies(item);
            Collection<Inconsistency> inconsistencies = validator.getInconsistencies();
            if (!inconsistencies.isEmpty()) {
                mismatchDescription.appendText("got Project containing " + inconsistencies.size()
                        + " inconsistencies: ");
                mismatchDescription.appendValue(inconsistencies);
                return false;
            }
            return true;
        }
    };
    private static final TypeSafeDiagnosingMatcher<PMCompilationUnit> HAS_NO_PROBLEMS = new TypeSafeDiagnosingMatcher<PMCompilationUnit>() {

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText("a CompilationUnit without Java Problems");
        }

        @Override
        protected boolean matchesSafely(PMCompilationUnit arg0, Description arg1) {
            boolean matches = arg0.getProblems().length == 0;
            if (!matches) {
                arg1.appendText("a CompilationUnit with the following problems:");
                arg1.appendValue(arg0.getProblems());
            }
            return matches;
        }
    };

    public static Matcher<MethodDeclaration> equivalentTo(final MethodDeclaration other) {
        TypeSafeDiagnosingMatcher<MethodDeclaration> equivalentTo = new TypeSafeDiagnosingMatcher<MethodDeclaration>() {
    
            private FunctionModelEquivalenceChecker checker = new FunctionModelEquivalenceChecker();
    
            @Override
            public void describeTo(Description description) {
                description.appendText("a method declaration equivalent to " + other);
            }
    
            @Override
            protected boolean matchesSafely(MethodDeclaration method, Description description) {
                return this.checker.checkEquivalence(new FunctionModel(method),
                        new FunctionModel(other));
            }
        };
        return equivalentTo;
    }

    public static Matcher<Project> hasNoInconsistencies() {
        return HAS_NO_INCONSISTENCIES;
    }

    public static Matcher<PMCompilationUnit> hasNoProblems() {
        return HAS_NO_PROBLEMS;
    }

    public static <E> Matcher<Collection<E>> hasElements(final Collection<E> expected) {
        return new TypeSafeMatcher<Collection<E>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("a collection containing the following elements:").appendValue(expected);
            }

            @Override
            protected boolean matchesSafely(Collection<E> candidate) {
                if (candidate.size() != expected.size()) {
                    return false;
                }
                for (E item : expected) {
                    if (!candidate.contains(item)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public static Matcher<ICompilationUnit> hasSource(final String source) {
        return new TypeSafeMatcher<ICompilationUnit>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("an ICompilationUnit with source \"" + source + "\"");
            }

            @Override
            protected boolean matchesSafely(ICompilationUnit arg0) {
                try {
                    final ASTNode node1 = parseNodeFromSource(arg0.getSource());
                    final ASTNode node2 = parseNodeFromSource(source);
                    return node1.subtreeMatch(new ASTMatcher(), node2);
                } catch (JavaModelException e) {
                    return false;
                }
            }

            protected ASTNode parseNodeFromSource(final String source) {
                final ASTParser parser = ASTParser.newParser(AST.JLS8);
                parser.setSource(source.toCharArray());
                parser.setResolveBindings(true);
                return parser.createAST(null);
            }
        };
    }

    public static Matcher<PMCompilationUnit> hasPMSource(final String source) {
        return new TypeSafeMatcher<PMCompilationUnit>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("an ICompilationUnit with source \"" + source + "\"");
            }

            @Override
            protected boolean matchesSafely(PMCompilationUnit arg0) {
                try {
                    final ASTNode node1 = parseNodeFromSource(arg0.getSource());
                    final ASTNode node2 = parseNodeFromSource(source);
                    return node1.subtreeMatch(new ASTMatcher(), node2);
                } catch (JavaModelException e) {
                    return false;
                }
            }

            protected ASTNode parseNodeFromSource(final String source) {
                final ASTParser parser = ASTParser.newParser(AST.JLS8);
                parser.setSource(source.toCharArray());
                parser.setResolveBindings(true);
                return parser.createAST(null);
            }
        };
    }

}
