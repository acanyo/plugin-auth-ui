package com.xhhao.authui.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xhhao.authui.design.DesignSpecs;
import com.xhhao.authui.template.AuthTemplate;
import com.xhhao.authui.template.DesignCatalog;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.mcpserver.api.McpToolDefinition;
import run.halo.mcpserver.api.McpToolException;
import run.halo.mcpserver.api.McpToolInvocation;
import run.halo.mcpserver.api.McpToolResult;

class AuthUiMcpToolProviderTest {
    private DesignCatalog catalog;
    private ExternalUrlSupplier externalUrl;
    private AuthUiMcpToolProvider provider;

    @BeforeEach
    void setUp() {
        catalog = mock(DesignCatalog.class);
        externalUrl = mock(ExternalUrlSupplier.class);
        when(catalog.activeName()).thenReturn(Mono.just("auth-template-a"));
        provider = new AuthUiMcpToolProvider(catalog, new DesignSpecs(), externalUrl);
    }

    private McpToolDefinition tool(String name) {
        return provider.tools().filter(tool -> tool.name().equals(name)).blockFirst();
    }

    private McpToolResult call(String name, Map<String, Object> arguments) {
        return tool(name).handler().execute(new McpToolInvocation(name, arguments)).block();
    }

    private static AuthTemplate template(String name, String displayName) {
        var template = new AuthTemplate();
        var metadata = new Metadata();
        metadata.setName(name);
        metadata.setVersion(3L);
        template.setMetadata(metadata);
        template.getSpec().setDisplayName(displayName);
        return template;
    }

    @Test
    void contributesValidTools() {
        var names = provider.tools().map(McpToolDefinition::name).collectList().block();
        assertEquals(List.of("get_design_guide", "list_designs", "get_design", "lint_design", "create_design",
            "update_design", "delete_design", "activate_design", "preview_design"), names);
        assertTrue(tool("list_designs").annotations().readOnlyHint());
        assertTrue(tool("delete_design").annotations().destructiveHint());
    }

    @Test
    void guideNamesTheProtocolTools() {
        var guide = (String) call("get_design_guide", Map.of()).structuredContent().get("guide");
        assertTrue(guide.contains("auth-ui__lint_design"), guide);
    }

    @Test
    void guideCarriesTheSkillWorkflowSoNoInstallIsNeeded() {
        var guide = (String) call("get_design_guide", Map.of()).structuredContent().get("guide");
        assertTrue(guide.contains("# Workflow"), guide);
        assertTrue(guide.contains("Use the lowest layer that does the job"), "designing principles");
        assertTrue(guide.contains("Activate only with explicit confirmation"), "saving rules");
        assertTrue(guide.contains("## Report"), "report step");
        assertTrue(!guide.contains("## Before You Start"), "setup steps are for agents without the tools");
        assertTrue(tool("create_design").description().contains("get_design_guide first"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsDesignsAndMarksTheActiveOne() {
        when(catalog.list()).thenReturn(Flux.just(template("auth-template-a", "A"), template("auth-template-b", "B")));
        var items = (List<Map<String, Object>>) call("list_designs", Map.of()).structuredContent().get("items");
        assertEquals(true, items.get(0).get("active"));
        assertEquals(false, items.get(1).get("active"));
        assertEquals(3L, items.get(0).get("version"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsADesignAndReportsWarnings() {
        when(catalog.create(any())).thenAnswer(invocation -> {
            var created = template("auth-template-new", "New");
            created.setSpec(invocation.getArgument(0));
            return Mono.just(created);
        });
        var result = call("create_design", Map.of("spec", Map.of(
            "displayName", "New",
            "theme", Map.of("primary", "not a colour!", "motion", Map.of("entrance", "zoom")))));
        assertEquals("auth-template-new", result.structuredContent().get("name"));
        var warnings = (List<String>) result.structuredContent().get("warnings");
        assertTrue(warnings.getFirst().contains("theme.primary"), warnings.toString());
        verify(catalog).create(org.mockito.ArgumentMatchers.argThat(spec ->
            "zoom".equals(spec.getTheme().getMotion().getEntrance())));
    }

    @Test
    void reportsAConflictingUpdate() {
        when(catalog.update(eq("auth-template-a"), any(), eq(2L)))
            .thenReturn(Mono.error(new OptimisticLockingFailureException("changed")));
        var error = assertThrows(McpToolException.class, () -> call("update_design", Map.of(
            "name", "auth-template-a", "version", 2, "spec", Map.of("displayName", "A"))));
        assertEquals("CONFLICT", error.code());
    }

    @Test
    void reportsAMissingDesign() {
        when(catalog.activate("nope")).thenReturn(Mono.empty());
        var error = assertThrows(McpToolException.class, () -> call("activate_design", Map.of("name", "nope")));
        assertEquals("NOT_FOUND", error.code());
    }

    @Test
    void restoresTheBaseLookWithAnEmptyName() {
        when(catalog.deactivate()).thenReturn(Mono.empty());
        assertEquals("", call("activate_design", Map.of("name", "")).structuredContent().get("active"));
        verify(catalog, never()).activate(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void previewLinksUseTheExternalUrl() throws Exception {
        when(catalog.get("auth-template-a")).thenReturn(Mono.just(template("auth-template-a", "A")));
        when(externalUrl.getRaw()).thenReturn(URI.create("https://blog.example.com/").toURL());
        var result = call("preview_design", Map.of("name", "auth-template-a")).structuredContent();
        var login = (Map<String, Object>) ((Map<String, Object>) result.get("previews")).get("login");
        assertEquals("https://blog.example.com/auth-ui/preview/login?auth_ui_display=modal&template=auth-template-a",
            login.get("modal"));
        assertEquals("https://blog.example.com/console/auth-ui/designs/auth-template-a", result.get("studio"));
    }
}
