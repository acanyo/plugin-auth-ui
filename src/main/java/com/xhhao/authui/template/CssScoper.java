package com.xhhao.authui.template;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Confines template CSS to the plugin container before it is written into the page.
 *
 * <p>Templates are data editable through the API, so their CSS is treated as untrusted:</p>
 * <ul>
 *   <li>every {@code <} becomes the CSS escape {@code \3c }, so the CSS can never close the
 *   style element or open a tag. {@code <} is only meaningful inside CSS strings, where the
 *   escape keeps the same character;</li>
 *   <li>at-rules other than {@code @media}, {@code @supports}, {@code @keyframes} and
 *   {@code @font-face} are dropped ({@code @import} could load external CSS);</li>
 *   <li>every selector is prefixed with {@code #auth-ui}; {@code :root}, {@code html} and
 *   {@code body} map to the container itself.</li>
 * </ul>
 * The browser's CSS parser is the final judge; this keeps the result inside one element.
 */
public final class CssScoper {
    static final String SCOPE = "#auth-ui";
    static final int MAX_LENGTH = 20_000;
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern ROOT_SELECTOR =
        Pattern.compile("^(?::root|html|body)(?=$|[\\s.:#\\[>+~])", Pattern.CASE_INSENSITIVE);

    private CssScoper() {
    }

    public static String scope(String css) {
        if (css == null || css.isBlank()) {
            return "";
        }
        var source = css.length() > MAX_LENGTH ? css.substring(0, MAX_LENGTH) : css;
        source = COMMENT.matcher(source).replaceAll("").replace("<", "\\3c ");
        var out = new StringBuilder();
        scopeBlock(source, 0, source.length(), out);
        return out.toString();
    }

    /** Appends the scoped rules between {@code start} and {@code end} to {@code out}. */
    private static void scopeBlock(String css, int start, int end, StringBuilder out) {
        int i = start;
        while (i < end) {
            int open = css.indexOf('{', i);
            int semicolon = css.indexOf(';', i);
            if (open < 0 || open >= end) {
                return;
            }
            // Statement at-rules without a block (e.g. @import url(...);) are dropped.
            if (semicolon >= 0 && semicolon < open && css.substring(i, semicolon).trim().startsWith("@")) {
                i = semicolon + 1;
                continue;
            }
            int close = matchingBrace(css, open, end);
            if (close < 0) {
                return;
            }
            var prelude = css.substring(i, open).trim();
            var body = css.substring(open + 1, close);
            if (prelude.startsWith("@")) {
                var name = prelude.split("[\\s(]", 2)[0].toLowerCase(Locale.ROOT);
                switch (name) {
                    case "@media", "@supports" -> {
                        out.append(prelude).append('{');
                        scopeBlock(body, 0, body.length(), out);
                        out.append('}');
                    }
                    case "@keyframes", "@-webkit-keyframes", "@font-face" ->
                        out.append(prelude).append('{').append(body).append('}');
                    default -> {
                        // Unsupported at-rules are ignored.
                    }
                }
            } else if (!prelude.isEmpty() && body.indexOf('{') < 0) {
                out.append(scopeSelectors(prelude)).append('{').append(body.trim()).append('}');
            }
            i = close + 1;
        }
    }

    private static int matchingBrace(String css, int open, int end) {
        int depth = 0;
        for (int i = open; i < end; i++) {
            char c = css.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    static String scopeSelectors(String selectors) {
        var scoped = new StringBuilder();
        for (var part : splitTopLevel(selectors)) {
            var selector = part.trim();
            if (selector.isEmpty()) {
                continue;
            }
            if (!scoped.isEmpty()) {
                scoped.append(',');
            }
            var root = ROOT_SELECTOR.matcher(selector);
            if (root.find()) {
                scoped.append(SCOPE).append(selector.substring(root.end()));
            } else if (selector.startsWith(SCOPE)) {
                scoped.append(selector);
            } else {
                scoped.append(SCOPE).append(' ').append(selector);
            }
        }
        return scoped.toString();
    }

    /** Splits on commas outside parentheses and brackets, e.g. {@code :is(a, b), c}. */
    private static java.util.List<String> splitTopLevel(String selectors) {
        var parts = new java.util.ArrayList<String>();
        int depth = 0;
        int last = 0;
        for (int i = 0; i < selectors.length(); i++) {
            char c = selectors.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if ((c == ')' || c == ']') && depth > 0) {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(selectors.substring(last, i));
                last = i + 1;
            }
        }
        parts.add(selectors.substring(last));
        return parts;
    }
}
