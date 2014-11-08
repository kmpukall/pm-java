package net.creichen.pm.tests;

import java.util.Collection;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.core.Project;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;

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
}
