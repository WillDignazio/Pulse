package net.digitalbebop.indexer;

import org.javafp.parsecj.*;

import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

abstract class Expr {
    public abstract String toString();

    @Override
    public boolean equals(Object other) {
        return other instanceof Expr && toString().equals(other.toString());
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
        this.str = str;
    }

    public String toString() {
        return str;
    }
}

class Token extends Expr {
    protected String field;
    protected String value;

    public Token(String f, String v) {
        field = f;
        value = v;
    }

    public String toString() {
        return field + ":" + value + "";
    }
}

class InToken extends Token {

    public InToken(String field, String value) {
        super(field, value);
    }

    public String toString() {
        return field + ":*" + value + "*";
    }
}

/**
 * Lexical Syntax:
 *
 * query  ::= tokens ',' | string
 * tokens ::= alphaNum types alphaNum tokens | nil
 * types  ::= ':' | '::'
 */
public final class Query {

    /** Copied from [[org.javafp.parsecj.Text.alphaNum]] to allow for periods '.' as well */
    private static final Parser<Character, String> alphaNum =
            state -> {
                if (state.end()) {
                    return ConsumedT.empty(endOfInput(state, "alphaNum"));
                }

                char c = state.current();
                if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '.') {
                    final State<Character> tail = state;
                    return ConsumedT.empty(
                            Reply.error(
                                    Message.lazy(() -> Message.of(tail.position(), tail.current(), "string"))
                            )
                    );
                }

                final StringBuilder sb = new StringBuilder();
                do {
                    sb.append(c);
                    state = state.next();
                    if (state.end()) {
                        break;
                    }
                    c = state.current();

                } while (Character.isAlphabetic(c) || Character.isDigit(c) || c == '.');

                final State<Character> tail = state;
                return ConsumedT.consumed(
                        () -> Reply.ok(
                                sb.toString(),
                                tail,
                                Message.lazy(() -> Message.of(tail.position()))
                        )
                );
            };

    /** Copied from [[org.javafp.parsecj.Text.alphaNum]] to allow for periods '.' and spaces ' ' as well */
    private static final Parser<Character, String> string =
            state -> {
                if (state.end()) {
                    return ConsumedT.empty(endOfInput(state, "alphaNum"));
                }

                char c = state.current();
                if (!Character.isAlphabetic(c) && !Character.isDigit(c) && !Character.isWhitespace(c) && c != '.') {
                    final State<Character> tail = state;
                    return ConsumedT.empty(
                            Reply.error(
                                    Message.lazy(() -> Message.of(tail.position(), tail.current(), "string"))
                            )
                    );
                }

                final StringBuilder sb = new StringBuilder();
                do {
                    sb.append(c);
                    state = state.next();
                    if (state.end()) {
                        break;
                    }
                    c = state.current();

                } while (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c) || c == '.');

                final State<Character> tail = state;
                return ConsumedT.consumed(
                        () -> Reply.ok(
                                sb.toString(),
                                tail,
                                Message.lazy(() -> Message.of(tail.position()))
                        )
                );
            };


    private final static Parser<Character, Expr> token = alphaNum.bind(
            field -> wspaces.then(
                    or(
                            chr(':').then(
                                    wspaces.then(alphaNum.bind(
                                            value -> retn((Expr) new Token(field, value))))).attempt(),
                            string("::").then(
                                    wspaces.then(alphaNum.bind(
                                            value -> retn((Expr) new InToken(field, value))))).attempt())));

    private final static Parser<Character, Expr> tokens = token.sepBy(wspace).bind(
            tks -> retn(tks.foldl1(And::new))).attempt();

    final static Parser<Character, Expr> query = choice(
            tokens.bind(
                    tks -> or(
                            chr(',').then( // attempts to parse the rest of the search string
                                    wspaces.then(
                                            string.bind(
                                                    str -> retn((Expr) new And(tks, new Str(str)))))).attempt(),
                            retn(tks))).attempt(),
                            string.bind(
                                    str -> retn(new Str(str))));
}
