package org.tervel.plexus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * An integer expression over names: integers and names joined by {@code +} and {@code -}, such as
 * {@code value - flag}. Names are letters, digits and underscores, starting with a letter; what a name
 * is worth is the caller's to say when it is evaluated.
 */
public final class Expression {

    private final List<String> terms;   // each term a name or an integer
    private final List<Integer> signs;  // +1 or -1 per term
    private final String text;

    private Expression(String text, List<String> terms, List<Integer> signs) {
        this.text = text; this.terms = terms; this.signs = signs;
    }

    /** Parse {@code text}. @throws IllegalArgumentException with the reason when it is not an expression. */
    public static Expression parse(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("empty expression");
        final var terms = new ArrayList<String>();
        final var signs = new ArrayList<Integer>();
        var sign = 1;
        var expectTerm = true;
        var i = 0;
        final var s = text.trim();
        while (i < s.length()) {
            final var ch = s.charAt(i);
            if (Character.isWhitespace(ch)) { i++; continue; }
            if (expectTerm) {
                if (ch == '-' || ch == '+') { if (ch == '-') sign = -sign; i++; continue; }
                var j = i;
                if (Character.isDigit(ch)) while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                else if (Character.isLetter(ch))
                    while (j < s.length() && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) j++;
                else throw new IllegalArgumentException("'" + text + "': unexpected '" + ch + "'");
                terms.add(s.substring(i, j));
                signs.add(sign);
                sign = 1;
                expectTerm = false;
                i = j;
            } else {
                if (ch != '+' && ch != '-') throw new IllegalArgumentException("'" + text + "': expected + or - at '" + ch + "'");
                if (ch == '-') sign = -1;
                expectTerm = true;
                i++;
            }
        }
        if (expectTerm) throw new IllegalArgumentException("'" + text + "': ends without a term");
        return new Expression(text, List.copyOf(terms), List.copyOf(signs));
    }

    /** The names it refers to. */
    public Set<String> names() {
        final var out = new LinkedHashSet<String>();
        for (final var t : terms) if (!Character.isDigit(t.charAt(0))) out.add(t);
        return out;
    }

    /** Its value, with each name worth what {@code names} says. */
    public int eval(ToIntFunction<String> names) {
        var sum = 0;
        for (var k = 0; k < terms.size(); k++) {
            final var t = terms.get(k);
            sum += signs.get(k) * (Character.isDigit(t.charAt(0)) ? Integer.parseInt(t) : names.applyAsInt(t));
        }
        return sum;
    }

    @Override public String toString() { return text; }
}
