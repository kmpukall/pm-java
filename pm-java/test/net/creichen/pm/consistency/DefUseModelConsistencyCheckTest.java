package net.creichen.pm.consistency;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.UnknownUse;
import net.creichen.pm.tests.PMTest;

import org.junit.Ignore;
import org.junit.Test;

public class DefUseModelConsistencyCheckTest extends PMTest {

    @Test
    @Ignore
    public void test() {
        createCompilationUnit("", "S.java", "public class S { int m() {return x;} }");

        Collection<Inconsistency> inconsistencies = new DefUseModelConsistencyCheck(getProject())
        .calculateInconsistencies(getProject().getUDModel());

        assertThat(inconsistencies.size(), is(1));
        Inconsistency inconsistency = inconsistencies.iterator().next();
        assertThat(inconsistency, is(instanceOf(UnknownUse.class)));
    }

}
