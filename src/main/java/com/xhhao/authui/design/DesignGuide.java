package com.xhhao.authui.design;

/**
 * The design reference shared by every AI integration: the MCP guide tool returns it and the
 * console assistant puts it in the system prompt. It is also served as a plugin asset.
 */
public final class DesignGuide {
    static final String PATH = "mcp/design-guide.md";
    private static final String TEXT = ClasspathText.read(PATH);

    private DesignGuide() {
    }

    public static String text() {
        return TEXT;
    }
}
