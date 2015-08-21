package net.digitalbebop.indexer;

import static org.junit.Assert.*;
import org.javafp.parsecj.State;
import org.junit.Test;

public class QueryTests {

    @Test
    public void multiToken() throws Exception {
        And and = new And(new Token("test1", "value"), new Token("test2", "value"));
        assertEquals(and, Query.query.parse(State.of("test1:value test2:value")).getResult());
    }

    @Test
    public void simpleString() throws Exception {
        assertEquals(new Str("simpleStr"), Query.query.parse(State.of("simpleStr")).getResult());
    }

    @Test
    public void stringWithSpace() throws Exception {
        assertEquals(new Str("str1 str2"), Query.query.parse(State.of("str1 str2")).getResult());
    }

    @Test
    public void simpleCombination() throws Exception {
        And and = new And(new Token("field", "value"), new Str("strstr"));
        assertEquals(and, Query.query.parse(State.of("field:value, strstr")).getResult());
    }

    @Test
    public void combination() throws Exception {
        And and = new And(new Token("field", "value"), new Str("str str"));
        assertEquals(and, Query.query.parse(State.of("field:value, str str")).getResult());
    }

    @Test
    public void multiCombination() throws Exception {
        And and = new And(new And(new Token("field2", "value2"), new Token("field", "value")), new Str("str str"));
        assertEquals(and, Query.query.parse(State.of("field2:value2 field: value, str str")).getResult());
    }

    @Test
    public void tokenWithPeriod() throws Exception {
        Token token = new Token("tag", "csh.general");
        assertEquals(token, Query.query.parse(State.of("tag : csh.general")).getResult());
    }

    @Test
    public void inToken() throws Exception {
        InToken token = new InToken("tag", "general");
        assertEquals(token, Query.query.parse(State.of("tag :: general")).getResult());
    }

    @Test
    public void tokenMix() throws Exception {
        And and = new And(new And(new Token("tag", "general"), new InToken("test", "value")), new Str("very long string of some kind"));
        assertEquals(and, Query.query.parse(State.of("tag : general test :: value, very long string of some kind")).getResult());
    }
}
