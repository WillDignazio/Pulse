package net.digitalbebop.indexer;

import static org.junit.Assert.*;
import org.javafp.parsecj.State;
import org.junit.Test;

public class QueryTests {

    @Test
    public void multiToken() throws Exception {
        String answer = "test1:value AND test2:value";
        assertEquals(answer, Query.query.parse(State.of("test1=value test2=value")).getResult());
    }

    @Test
    public void simpleString() throws Exception {
        assertEquals("simpleStr", Query.query.parse(State.of("simpleStr")).getResult());
    }

    @Test
    public void stringWithSpace() throws Exception {
        assertEquals("str1 str2", Query.query.parse(State.of("str1 str2")).getResult());
    }

    @Test
    public void simpleCombination() throws Exception {
        String answer = "field:value AND strstr";
        assertEquals(answer, Query.query.parse(State.of("field=value, strstr")).getResult());
    }

    @Test
    public void combination() throws Exception {
        String answer = "field:value AND str str";
        assertEquals(answer, Query.query.parse(State.of("field=value, str str")).getResult());
    }

    @Test
    public void multiCombination() throws Exception {
        String answer = "field2:value2 AND field:value AND str str";
        assertEquals(answer, Query.query.parse(State.of("field2=value2 field= value, str str"))
                .getResult());
    }

    @Test
    public void tokenWithPeriod() throws Exception {
        assertEquals("tag:csh.general", Query.query.parse(State.of("tag = csh.general"))
                .getResult());
    }

    @Test
    public void inToken() throws Exception {
        assertEquals("tag:*general*", Query.query.parse(State.of("tag ~ general"))
                .getResult());
    }

    @Test
    public void tokenMix() throws Exception {
        String answer = "tag:general AND test:*value* AND very long string of some kind";
        assertEquals(answer,
                Query.query.parse(
                        State.of("tag = general test ~ value, very long string of some kind"))
                        .getResult());
    }

    @Test
    public void notToken() throws Exception {
        String answer = "-field:value";
        assertEquals(answer, Query.query.parse(State.of("field -= value")).getResult());
    }

    @Test
    public void notInToken() throws Exception {
        String answer = "-field:*value*";
        assertEquals(answer, Query.query.parse(State.of("field -~ value")).getResult());
    }
}
