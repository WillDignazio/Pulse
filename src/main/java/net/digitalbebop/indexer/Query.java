package net.digitalbebop.indexer;

import org.javafp.parsecj.*;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import static org.javafp.parsecj.Combinators.*;
import static org.javafp.parsecj.Text.*;

/**
 * Lexical Syntax:
 *
 * query  ::= tokens ',' | string
 * tokens ::= alphaNum types alphaNum tokens | nil
 * types  ::= '=' | '~' | '-=' | '-~'
 */
public final class Query {

    /** Copied from [[org.javafp.parsecj.Text.alphaNum]] to allow for periods '.' as well */
    private static final Parser<Character, String> alphaNum =
            state -> {
                if (state.end()) {
                    return ConsumedT.empty(endOfInput(state, "alphaNum"));
                }

                char c = state.current();
                if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '.' && c != '/' && c != '-') {
                    final State<Character> tail = state;
                    return ConsumedT.empty(
                            Reply.error(
                                    Message.lazy(() ->
                                            Message.of(tail.position(), tail.current(), "string"))
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

                } while (Character.isAlphabetic(c) || Character.isDigit(c) || c == '.' || c == '/' || c == '-');

                final State<Character> tail = state;
                return ConsumedT.consumed(
                        () -> Reply.ok(
                                sb.toString(),
                                tail,
                                Message.lazy(() -> Message.of(tail.position()))
                        )
                );
            };

    /** Copied from [[org.javafp.parsecj.Text.alphaNum]] to allow for periods '.'
     * and spaces ' ' as well */
    private static final Parser<Character, String> string =
            state -> {
                if (state.end()) {
                    return ConsumedT.empty(endOfInput(state, "string"));
                }

                char c = state.current();
                if (!Character.isAlphabetic(c) && !Character.isDigit(c) &&
                        !Character.isWhitespace(c) && c != '.') {
                    final State<Character> tail = state;
                    return ConsumedT.empty(
                            Reply.error(
                                    Message.lazy(() ->
                                            Message.of(tail.position(), tail.current(), "string"))
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

                } while (Character.isAlphabetic(c) || Character.isDigit(c) ||
                        Character.isWhitespace(c) || c == '.');

                final State<Character> tail = state;
                return ConsumedT.consumed(
                        () -> Reply.ok(
                                sb.toString(),
                                tail,
                                Message.lazy(() -> Message.of(tail.position()))
                        )
                );
            };

    private final static Parser<Character, BinaryOperator<String>> inOp =
            retn((f, v) -> f + ":*" + v + "*");
    private final static Parser<Character, BinaryOperator<String>> isOp =
            retn((f, v) -> f + ":" + v);
    private final static Parser<Character, BinaryOperator<String>> notIsOp =
            retn((f, v) -> "-" + f + ":" + v);
    private final static Parser<Character, BinaryOperator<String>> notInOp =
            retn((f, v) -> "-" + f + ":*" + v + "*");

    private final static Parser<Character, BinaryOperator<String>> ops = choice(
            chr('=').then(isOp),
            chr('~').then(inOp),
            string("-=").then(notIsOp).attempt(),
            string("-~").then(notInOp).attempt()
    );

    private final static Parser<Character, String> token = alphaNum.bind(
            field -> wspaces.then(
                    ops.bind(
                            op -> wspaces.then(
                                    alphaNum.bind(
                                            value -> retn(op.apply(field, value))
                                    )
                            )
                    )
            )
    );

    private final static BiFunction<StringBuilder, String, StringBuilder> combine = ((builder, expr) -> {
        if (builder == null) {
            return new StringBuilder().append(expr);
        } else {
            return builder.append(" AND " + expr);
        }
    });

    private final static Parser<Character, StringBuilder> tokens =
            token.sepBy(wspace).bind(
                    tks -> retn(tks.foldl(combine, null))).attempt();

    final static Parser<Character, String> query = choice(
            tokens.bind(
                    tks -> or(
                            chr(',').then( // attempts to parse the rest of the search string
                                    wspaces.then(
                                            string.bind(
                                                    str -> retn((String) tks.append(" AND (" + str + ")").toString())))).attempt(),
                            retn(tks.toString()))).attempt(),
            string.bind(
                    str -> retn(str)));
}
