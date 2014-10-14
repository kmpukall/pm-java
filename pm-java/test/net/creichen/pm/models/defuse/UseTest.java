package net.creichen.pm.models.defuse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import net.creichen.pm.utils.factories.ASTNodeFactory;

import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Before;
import org.junit.Test;

public class UseTest {

    private Use use;
    private SimpleName simpleName;

    @Before
    public void setUp() {
        this.simpleName = ASTNodeFactory.createSimpleName("x");
        this.use = new Use(this.simpleName);
    }

    @Test
    public void itShouldStoreTheSimpleName() {
        assertThat(this.use.getSimpleName(), is(equalTo(this.simpleName)));
    }

    @Test
    public void itShouldStoreDefs() {
        Def def1 = new Def(null);
        Def def2 = new Def(null);

        this.use.addReachingDefinition(def1);
        this.use.addReachingDefinition(def2);

        assertThat(this.use.getReachingDefinitions(), containsInAnyOrder(def1, def2));
    }
}
