package net.digitalbebop.indexer;


import org.javafp.parsecj.*;

import java.util.function.BinaryOperator;

import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

abstract class Expr {
    public abstract String toSolr();
}

class And extends Expr {

    private Expr left;
    private Expr right;

    public And(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left.toSolr() + " AND " + right.toSolr();
    }
}

class Or extends Expr {
    private Expr left;
    private Expr right;

    public Or(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left.toSolr() + " OR " + right.toSolr();
    }
}

class Equals extends Expr {
    private Expr left;
    private Expr right;

    public Equals(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left + ":" + right;
    }
}

class LessThan extends Expr {
    private Expr left;
    private Expr right;

    public LessThan(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left + ":[* TO " + right + "]";

    }
}

class GreaterThan extends Expr {
    private Expr left;
    private Expr right;

    public GreaterThan(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left + ":[" + right + " TO *]";
    }
}

class In extends Expr {
    private Expr left;
    private Expr right;

    public In(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toSolr() {
        return left + ":*" + right + "*";
    }
}

class Str extends Expr {
    private String str;

    public Str(String str) {
        this.str = str;
    }

    public String toSolr() {
        return str;
    }
}


/**
 * Lexical Syntax:
 *
 * query      ::= structured | string | nil
 * structured ::= elem join query
 * elem       ::= string binOp string
 * joinOp     ::= 'AND' | 'OR'
 * binOp      ::= '=' | '>' | '<' | 'in'
 */

public final class QueryLanguage {
    // Forward declare expr to allow for circular references.
    final static org.javafp.parsecj.Parser.Ref<Character, Expr> query = Parser.ref();

    final static Parser<Character, BinaryOperator<Expr>> add = retn((l, r) -> new And(l, r));
    final static Parser<Character, BinaryOperator<Expr>> or = retn((l, r) -> new Or(l, r));
    final static Parser<Character, BinaryOperator<Expr>> equals = retn((l, r) -> new Equals(l, r));
    final static Parser<Character, BinaryOperator<Expr>> lessThan = retn((l, r) -> new LessThan(l, r));
    final static Parser<Character, BinaryOperator<Expr>> greaterThan = retn((l, r) -> new GreaterThan(l, r));
    final static Parser<Character, BinaryOperator<Expr>> in = retn((l, r) -> new In(l, r));

    final static Parser<Character, Void> eof = eof();

    final static Parser<Character, BinaryOperator<Expr>> joinOp = choice(
            string("AND").then(add),
            string("OR").then(or));
    final static Parser<Character, BinaryOperator<Expr>> binOp = choice(
            string("=").then(equals),
            string(">").then(lessThan),
            string("<").then(greaterThan),
            string("in").then(in));

    final static Parser<Character, Expr> elem = alphaNum.bind(
            left -> binOp.bind(
                    op -> alphaNum.bind(
                            right -> eof.then(retn(op.apply(new Str(left), new Str(right)))))));

    final static Parser<Character, Expr> structured = elem.bind(
            left -> joinOp.bind(
                    op -> query.bind(
                            right -> eof.then(retn(op.apply(left, right))))));

    final static Parser<Character, Expr> string = alphaNum.bind(str -> eof.then(retn(new Str(str))));

    public static void init() {
        query.set(choice(structured, string));
    }
}
