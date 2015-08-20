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
    public void stringTest() throws Exception {
        assertEquals("teststring", QueryLanguage.string.parse(State.of("teststring")).getResult().toString());
    }

    @Test
    public void stringWhiteSpaceTest() throws Exception {
        assertEquals("test string one", QueryLanguage.string.parse(State.of("test string one")).getResult().toString());

        assertEquals("test string", QueryLanguage.string.parse(State.of("test string")).getResult().toString());

        assertEquals("test string", QueryLanguage.string.parse(State.of("test      string")).getResult().toString());
    }

    @Test
    public void elemTest() throws Exception {
        assertEquals("key:value", QueryLanguage.elem.parse(State.of("key = value")).getResult().toString());

        assertEquals("key:[value TO *]", QueryLanguage.elem.parse(State.of("key > value")).getResult().toString());

        assertEquals("key:[* TO value]", QueryLanguage.elem.parse(State.of("key < value")).getResult().toString());

        assertEquals("value:*key*", QueryLanguage.elem.parse(State.of("key in value")).getResult().toString());
    }

    @Test
    public void elemNoWhiteSpaceTest() throws Exception {
        assertEquals("key:value", QueryLanguage.elem.parse(State.of("key=value")).getResult().toString());

        assertEquals("key:[value TO *]", QueryLanguage.elem.parse(State.of("key>value")).getResult().toString());

        assertEquals("key:[* TO value]", QueryLanguage.elem.parse(State.of("key<value")).getResult().toString());
    }

    @Test
    public void structuredTest() throws Exception {
        assertEquals("key:value AND key1:value1", QueryLanguage.query.parse(State.of("key = value AND key1 = value1")).getResult().toString());

        assertEquals("key:value AND random", QueryLanguage.query.parse(State.of("key = value AND random")).getResult().toString());
    }
}
