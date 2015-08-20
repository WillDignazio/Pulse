package net.digitalbebop.indexer;

import static org.junit.Assert.*;
import org.javafp.parsecj.State;
import org.junit.Before;
import org.junit.Test;

public class QueryTests {

    @Before
    public void before() {
        QueryLanguage.init();
    }

    @Test
    public void elemTest() throws Exception {
        assertEquals("key:value", QueryLanguage.elem.parse(State.of("key = value")).getResult().toString());

        assertEquals("key:value", QueryLanguage.elem.parse(State.of("key > value")).getResult().toString());

        assertEquals("key:value", QueryLanguage.elem.parse(State.of("key < value")).getResult().toString());

        assertEquals("key:*value*", QueryLanguage.elem.parse(State.of("key in value")).getResult().toString());
    }

    @Test
    public void simpleStringTest() throws Exception {
        Expr expr = QueryLanguage.query.parse(State.of("test")).getResult();
    }
}
