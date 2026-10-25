package com.xhhao.authui.template;

import java.util.regex.Pattern;

/**
 * Prepares design JavaScript for an inline script element.
 *
 * <p>The code itself is trusted: only plugin managers can edit designs. It must still never end
 * the script element early, so every {@code </script} and {@code <!--} is broken with an
 * escape that JavaScript reads as the same characters inside strings and regular expressions.</p>
 */
public final class ScriptGuard {
    static final int MAX_LENGTH = 50_000;
    private static final Pattern CLOSING = Pattern.compile("(?i)</(script)");

    private ScriptGuard() {
    }

    /**
     * The design's code wrapped so it runs once the page is ready and an error in it is logged
     * instead of stopping the page's own scripts. Empty when there is no code.
     */
    public static String prepare(String js) {
        if (js == null || js.isBlank()) {
            return "";
        }
        var source = js.length() > MAX_LENGTH ? js.substring(0, MAX_LENGTH) : js;
        var guarded = CLOSING.matcher(source).replaceAll("<\\\\/$1").replace("<!--", "<\\!--");
        return "(function(){'use strict';var run=function(){try{\n" + guarded
            + "\n}catch(e){console.error('[auth-ui] design script failed',e)}};"
            + "document.readyState==='loading'?document.addEventListener('DOMContentLoaded',run):run();})();";
    }
}
