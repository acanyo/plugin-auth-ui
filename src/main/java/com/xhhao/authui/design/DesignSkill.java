package com.xhhao.authui.design;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The auth-ui-design agent skill, shared by every AI integration so they all follow the same rules.
 * Agents on MCP get its workflow from the guide tool, so they need not install the skill; the
 * console assistant only takes the design principles, because the studio's own buttons replace the
 * save, preview and activate steps.
 */
public final class DesignSkill {
    /** Copied from .agents/skills/auth-ui-design by the build. */
    static final String PATH = "mcp/skill/SKILL.md";
    static final String DESIGNING = "## Designing";
    /** "Before You Start" is left out: an agent that reads this already has the tools. */
    static final List<String> WORKFLOW = List.of(DESIGNING, "## Saving", "## Report");

    private static final String SKILL = ClasspathText.read(PATH);
    private static final String PRINCIPLES = section(SKILL, DESIGNING);
    private static final String AGENT_WORKFLOW = WORKFLOW.stream()
        .map(heading -> heading + "\n\n" + section(SKILL, heading))
        .collect(Collectors.joining("\n\n"));

    private DesignSkill() {
    }

    /** The design principles, for a model that only edits the design. */
    public static String principles() {
        return PRINCIPLES;
    }

    /** Designing, saving and reporting steps, for an agent that works through the MCP tools. */
    public static String agentWorkflow() {
        return AGENT_WORKFLOW;
    }

    /** The body of a level-two section, up to the next level-two heading. */
    static String section(String markdown, String heading) {
        int start = markdown.indexOf("\n" + heading + "\n");
        if (start < 0) {
            throw new IllegalStateException(PATH + " has no \"" + heading + "\" section");
        }
        int body = start + heading.length() + 2;
        int end = markdown.indexOf("\n## ", body);
        return markdown.substring(body, end < 0 ? markdown.length() : end).strip();
    }
}
