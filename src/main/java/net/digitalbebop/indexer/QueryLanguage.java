package net.digitalbebop.indexer;


import org.javafp.parsecj.*;

import java.util.function.BinaryOperator;

import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

abstract class Expr {
    public abstract String toString();
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

class Or extends Expr {
    private Expr left;
    private Expr right;

    public Or(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toString() {
        return left + " OR " + right;
    }
}

class Equals extends Expr {
    private Expr left;
    private Expr right;

    public Equals(Expr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public String toString() {
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

    public String toString() {
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

    public String toString() {
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

    public String toString() {
        return right + ":*" + left + "*";
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


/**
 * Lexical Syntax:
 *
 * query      ::= elem joinOp query | string
 * elem       ::= string binOp string
 * joinOp     ::= 'AND' | 'OR'
 * binOp      ::= '=' | '>' | '<' | 'in'
 */

public final class QueryLanguage {
    // Forward declare expr to allow for circular references.
    final static org.javafp.parsecj.Parser.Ref<Character, Expr> query = Parser.ref();

    final static Parser<Character, BinaryOperator<Expr>> add = retn((l, r) -> {
        System.out.println("---------------and test");
        return new And(l, r);
    });
    final static Parser<Character, BinaryOperator<Expr>> or = retn((l, r) -> new Or(l, r));
    final static Parser<Character, BinaryOperator<Expr>> equals = retn((l, r) -> new Equals(l, r));
    final static Parser<Character, BinaryOperator<Expr>> lessThan = retn((l, r) -> new LessThan(l, r));
    final static Parser<Character, BinaryOperator<Expr>> greaterThan = retn((l, r) -> new GreaterThan(l, r));
    final static Parser<Character, BinaryOperator<Expr>> in = retn((l, r) -> new In(l, r));
    // TODO replace with StringBuilder
    final static BinaryOperator<String> combine = ((l, r) -> l + " " + r);

    final static Parser<Character, BinaryOperator<Expr>> joinOp = choice(
            string("AND").then(add),
            string("OR").then(or));
    final static Parser<Character, BinaryOperator<Expr>> binOp = choice(
            string("=").then(equals),
            string(">").then(greaterThan),
            string("<").then(lessThan),
            string("in").then(in));

    final static Parser<Character, Expr> elem = alphaNum.bind(
            left -> skipMany(space).then(binOp.bind(
                    op -> skipMany(space).then(alphaNum.bind(
                            right -> retn(op.apply(new Str(left), new Str(right))))))));

    final static Parser<Character, Expr> structured = elem.bind(
            left -> skipMany(space).then(joinOp.bind(
                    op -> skipMany(space).then(query.bind(
                            right -> retn(op.apply(left, right)))))));

    final static Parser<Character, Void> eof = eof();

    final static Parser<Character, Expr> end = eof.then(retn(new Str("")));

    final static Parser.Ref<Character, Expr> string = Parser.ref();

    public static void init() {

        string.set((end).or(alphaNum.bind(
                str -> skipMany(space).then(string.bind(
                        str1 -> retn(new Str(str + " " + str1)))))));

        /*
        query.set(elem.bind(
                e -> skipMany(space).then(joinOp.bind(
                        op -> skipMany(space).then(elem.bind(
                                e1 -> retn(op.apply(e, e1))
                        ))
                ))
        ));
        */

        query.set(elem.bind(
                e -> skipMany(space).then(joinOp.bind(
                        op -> skipMany(space).then(elem.bind(
                                e1 -> retn(op.apply(e, e1))
                        ).or(string))))));

    }
}
