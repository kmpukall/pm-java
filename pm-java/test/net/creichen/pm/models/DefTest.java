package net.creichen.pm.models;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DefTest {

    @Test
    public void test() {
        Use use = new Use(null);

        Def def = new Def(null);
        def.addUse(use);

        assertThat(def.getUses(), hasItem(use));
    }

}
