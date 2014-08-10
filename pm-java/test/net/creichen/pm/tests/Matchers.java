package net.creichen.pm.tests;

import java.util.Collection;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.core.Project;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class Matchers {

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

    public static Matcher<Project> hasNoInconsistencies() {
        return HAS_NO_INCONSISTENCIES;
    }
}
