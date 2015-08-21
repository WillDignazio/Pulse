package net.digitalbebop.indexer;

import static org.junit.Assert.*;
import org.javafp.parsecj.State;
import org.junit.Test;

public class QueryTests {

    @Test
    public void simpleToken() throws Exception {
        Token token = new Token("test", "value");
        assertEquals(token, QueryLanguage.token.parse(State.of("test:value")).getResult());
    }

    @Test
    public void tokenWithSpace() throws Exception {
        Token token = new Token("test", "value");
        assertEquals(token, QueryLanguage.token.parse(State.of("test : value")).getResult());
    }

    @Test
    public void multiToken() throws Exception {
        And and = new And(new Token("test1", "value"), new Token("test2", "value"));
        assertEquals(and, QueryLanguage.query.parse(State.of("test1:value test2:value")).getResult());
    }

    @Test
    public void simpleString() throws Exception {
        assertEquals(new Str("simpleStr"), QueryLanguage.query.parse(State.of("simpleStr")).getResult());
    }

    @Test
    public void stringWithSpace() throws Exception {
        assertEquals(new Str("str1 str2"), QueryLanguage.query.parse(State.of("str1 str2")).getResult());
    }

    @Test
    public void simpleCombination() throws Exception {
        And and = new And(new Token("field", "value"), new Str("strstr"));
        assertEquals(and, QueryLanguage.query.parse(State.of("field:value, strstr")).getResult());
    }

    @Test
    public void combination() throws Exception {
        And and = new And(new Token("field", "value"), new Str("str str"));
        assertEquals(and, QueryLanguage.query.parse(State.of("field:value, str str")).getResult());
    }

    @Test
    public void multiCombination() throws Exception {
        And and = new And(new And(new Token("field2", "value2"), new Token("field", "value")), new Str("str str"));
        assertEquals(and, QueryLanguage.query.parse(State.of("field2:value2 field: value, str str")).getResult());
    }

    @Test
    public void tokenWithPeriod() throws Exception {
        Token token = new Token("tag", "csh.general");
        assertEquals(token, QueryLanguage.query.parse(State.of("tag : csh.general")).getResult());
    }
}
