package com.xhhao.authui.template;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Cleans the HTML a design places around Halo's forms.
 *
 * <p>Allows formatting, images, links, lists, layout containers and inline SVG icons with their
 * class and style attributes, so designs can be rich. Scripts, iframes, forms, inputs and every
 * event handler are removed; links and images accept only http(s) and site paths. Behaviour
 * belongs in the design's JavaScript, which only plugin managers can edit.</p>
 */
public final class SlotSanitizer {
    static final int MAX_LENGTH = 20_000;
    private static final Safelist SAFELIST = Safelist.relaxed()
        .addTags("section", "article", "header", "footer", "aside", "nav", "figure", "figcaption",
            "picture", "source", "small", "mark", "time", "hr",
            "svg", "path", "g", "circle", "rect", "line", "polyline", "polygon", "ellipse",
            "defs", "linearGradient", "radialGradient", "stop", "use", "title")
        .addAttributes(":all", "class", "id", "style", "title", "role", "aria-label",
            "aria-hidden", "aria-describedby", "lang", "dir",
            "data-animate", "data-delay", "data-variant", "data-auth")
        .addAttributes("a", "target", "rel")
        .addAttributes("img", "loading", "decoding", "srcset", "sizes")
        .addAttributes("source", "srcset", "type", "media", "sizes")
        .addAttributes("time", "datetime")
        .addAttributes("svg", "viewBox", "width", "height", "fill", "stroke", "stroke-width",
            "stroke-linecap", "stroke-linejoin", "xmlns", "preserveAspectRatio")
        .addAttributes("path", "d", "fill", "stroke", "stroke-width", "stroke-linecap",
            "stroke-linejoin", "opacity", "fill-rule", "clip-rule", "transform")
        .addAttributes("g", "fill", "stroke", "opacity", "transform")
        .addAttributes("circle", "cx", "cy", "r", "fill", "stroke", "stroke-width", "opacity")
        .addAttributes("rect", "x", "y", "width", "height", "rx", "ry", "fill", "stroke", "opacity")
        .addAttributes("line", "x1", "y1", "x2", "y2", "stroke", "stroke-width")
        .addAttributes("polyline", "points", "fill", "stroke", "stroke-width")
        .addAttributes("polygon", "points", "fill", "stroke", "stroke-width")
        .addAttributes("ellipse", "cx", "cy", "rx", "ry", "fill", "stroke")
        .addAttributes("linearGradient", "id", "x1", "y1", "x2", "y2", "gradientUnits")
        .addAttributes("radialGradient", "id", "cx", "cy", "r", "gradientUnits")
        .addAttributes("stop", "offset", "stop-color", "stop-opacity")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https")
        .addProtocols("source", "srcset", "http", "https")
        .preserveRelativeLinks(true);

    private SlotSanitizer() {
    }

    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        var source = html.length() > MAX_LENGTH ? html.substring(0, MAX_LENGTH) : html;
        var output = new Document.OutputSettings().prettyPrint(false);
        // A base URI lets relative site paths such as /upload/logo.png pass the protocol check.
        var cleaned = Jsoup.clean(source, "https://site.invalid", SAFELIST, output)
            .replace("https://site.invalid/", "/");
        return stripUnsafeStyles(cleaned);
    }

    /** {@code style} attributes may not load resources or run legacy expressions. */
    private static String stripUnsafeStyles(String html) {
        var document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        for (var element : document.body().select("[style]")) {
            var style = element.attr("style").toLowerCase(java.util.Locale.ROOT);
            if (style.contains("url(") || style.contains("expression(") || style.contains("@import")
                || style.contains("javascript:") || style.contains("behavior")) {
                element.removeAttr("style");
            }
        }
        // Links opened in a new tab must not get access to this window.
        for (var link : document.body().select("a[target]")) {
            link.attr("rel", "noopener noreferrer");
        }
        return document.body().html();
    }
}
