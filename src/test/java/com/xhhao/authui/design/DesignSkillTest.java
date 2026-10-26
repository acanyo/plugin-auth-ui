package com.xhhao.authui.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesignSkillTest {

    @Test
    void readsTheDesigningSectionOfTheBundledSkill() {
        var principles = DesignSkill.principles();
        assertTrue(principles.contains("Use the lowest layer that does the job"), principles);
        assertTrue(principles.contains("prefers-reduced-motion"));
        assertFalse(principles.contains("## "), "only one section is taken");
        assertFalse(principles.contains("auth-ui__"), "MCP tool steps do not apply in the studio");
    }

    @Test
    void agentWorkflowKeepsTheMcpStepsInOrder() {
        var workflow = DesignSkill.agentWorkflow();
        assertTrue(workflow.startsWith("## Designing"), workflow);
        assertTrue(workflow.indexOf("## Saving") < workflow.indexOf("## Report"));
        assertTrue(workflow.contains("auth-ui__lint_design"));
        assertFalse(workflow.contains("## Before You Start"));
    }

    @Test
    void stopsAtTheNextSection() {
        var markdown = "# Title\n\n## Designing\n\n1. one\n2. two\n\n## Saving\n\nsave\n";
        assertEquals("1. one\n2. two", DesignSkill.section(markdown, "## Designing"));
        assertEquals("save", DesignSkill.section(markdown, "## Saving"));
    }

    @Test
    void failsLoudlyWhenTheSectionIsRenamed() {
        assertThrows(IllegalStateException.class, () -> DesignSkill.section("## Other\n", "## Designing"));
    }
}
