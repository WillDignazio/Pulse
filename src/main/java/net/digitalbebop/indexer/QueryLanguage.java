package net.digitalbebop.indexer;

import org.javafp.parsecj.*;

import java.util.function.BinaryOperator;

import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

abstract class Expr {
    public abstract String toString();

    @Override
    public boolean equals(Object other) {
        if (other instanceof Expr) {
            return toString().equals(other.toString());
        } else {
            return false;
        }
    }
}

class And extends Expr {

    private Expr left;
    private Expr right;

    public And(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toString() {
        return left + " AND " + right;
    }
}

class Str extends Expr {
    private String str;

    public Str(String str) {
        this.str = str.trim();
    }

    public String toString() {
        return str;
    }
}

class Token extends Expr {
    private String field;
    private String value;

    public Token(String f, String v) {
        field = f;
        value = v;
    }

    public String toString() {
        return field + ":*" + value + "*";
    }
}


/**
 * Lexical Syntax:
 *
 * query ::= tokens ',' | string
 * tokens ::= alphaNum ':' alphaNum tokens | nil
 */
public final class QueryLanguage {

    private final static BinaryOperator<Expr> combineExprs = And::new;

    private final static Parser<Character, Expr> string = alphaNum.sepBy(wspace).bind(
            strs -> retn(new Str(strs.foldl((builder, str) -> builder.append(str + " "), new StringBuilder()).toString())));

    final static Parser<Character, Expr> token = alphaNum.bind(
            field -> wspaces.then(
                    chr(':').then(
                            wspaces.then(alphaNum.bind(
                                    value -> retn((Expr) new Token(field, value)))))));

    private final static Parser<Character, Expr> tokens = token.sepBy(wspace).bind(
            tks -> retn(tks.foldl1(combineExprs))).attempt();

    final static Parser<Character, Expr> query = choice(
            tokens.bind(
                    tks -> or(chr(',').then(
                            wspaces.then(
                                    string.bind(
                                            str -> retn((Expr) new And(tks, str))))).attempt(), retn(tks))).attempt(),
            string);
}
