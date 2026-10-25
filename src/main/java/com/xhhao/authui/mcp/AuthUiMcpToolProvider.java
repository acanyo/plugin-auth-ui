package com.xhhao.authui.mcp;

import com.xhhao.authui.design.DesignGuide;
import com.xhhao.authui.design.DesignSchema;
import com.xhhao.authui.design.DesignSkill;
import com.xhhao.authui.design.DesignSpecs;
import com.xhhao.authui.template.AuthTemplate;
import com.xhhao.authui.template.DesignCatalog;
import com.xhhao.authui.template.DesignLinter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.exception.ExtensionException;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.mcpserver.api.McpToolAnnotations;
import run.halo.mcpserver.api.McpToolDefinition;
import run.halo.mcpserver.api.McpToolException;
import run.halo.mcpserver.api.McpToolInvocation;
import run.halo.mcpserver.api.McpToolResult;
import run.halo.mcpserver.api.McpToolProvider;

/**
 * Lets AI agents design login pages through Halo MCP Server. Tools are exposed as
 * {@code auth-ui__<name>}; an access key must select each tool before it can be called.
 *
 * <p>Only Halo's MCP Server calls these tools, after it authenticated the access key and checked
 * its tool allow-list, so the permission callback has nothing more to check.</p>
 */
public class AuthUiMcpToolProvider implements McpToolProvider {
    private static final String CATEGORY = "登录界面设计";
    private static final List<String> PREVIEW_VIEWS =
        List.of("login", "signup", "password-reset", "two-factor", "logout");
    /**
     * The design guide followed by the skill's workflow, so an agent connected over MCP works the
     * same way as one with the skill installed.
     */
    private static final String AGENT_GUIDE = DesignGuide.text().strip()
        + "\n\n# Workflow\n\nFollow these steps for every design task.\n\n" + DesignSkill.agentWorkflow() + "\n";
    private static final String GUIDE_FIRST = " Call get_design_guide first in each session.";

    private final DesignCatalog catalog;
    private final DesignSpecs specs;
    private final ExternalUrlSupplier externalUrl;

    public AuthUiMcpToolProvider(DesignCatalog catalog, DesignSpecs specs, ExternalUrlSupplier externalUrl) {
        this.catalog = catalog;
        this.specs = specs;
        this.externalUrl = externalUrl;
    }

    @Override
    public Flux<McpToolDefinition> tools() {
        return Flux.just(getGuide(), listDesigns(), getDesign(), lintDesign(), createDesign(),
            updateDesign(), deleteDesign(), activateDesign(), previewDesign());
    }

    private McpToolDefinition getGuide() {
        return tool("get_design_guide", "Get the login design guide",
            "Read this first and follow it. Explains the AuthTemplate design format (page structure, CSS"
                + " variables, slot HTML rules, motion options, the AuthUI JavaScript API) and the required"
                + " workflow: lint before saving, give preview links, never activate without the user's consent.",
            "获取设计指南", "返回登录界面设计格式、页面结构、CSS 变量、动效，以及 AI 必须遵循的设计流程。",
            DesignSchema.object(null, Map.of()), true,
            invocation -> Mono.just(McpToolResult.success(Map.of("guide", AGENT_GUIDE), AGENT_GUIDE)));
    }

    private McpToolDefinition listDesigns() {
        return tool("list_designs", "List login designs",
            "List stored login page designs with their name, displayName, description, version and"
                + " whether each is active. Call get_design for the full spec." + GUIDE_FIRST,
            "列出登录设计", "列出所有登录界面设计及当前启用的设计。",
            DesignSchema.object(null, Map.of()), true,
            invocation -> catalog.activeName().flatMap(active -> catalog.list()
                .map(template -> summary(template, active))
                .collectList()
                .map(items -> McpToolResult.success(Map.of("active", active, "items", items),
                    items.size() + " designs; active: " + (active.isEmpty() ? "base look" : active)))));
    }

    private McpToolDefinition getDesign() {
        return tool("get_design", "Get a login design",
            "Get one design's full spec and version. Pass the version to update_design to avoid"
                + " overwriting newer edits.",
            "获取登录设计", "读取一个设计的完整内容与版本号。",
            nameInput("Design name from list_designs, e.g. auth-template-abc12."), true,
            invocation -> design(invocation).flatMap(template -> catalog.activeName()
                .map(active -> McpToolResult.success(detail(template, active)))));
    }

    private McpToolDefinition lintDesign() {
        return tool("lint_design", "Check a login design",
            "Report what the renderer would drop or change in a design spec: invalid colours, removed"
                + " HTML, dropped CSS at-rules, slots the layout does not show. Does not save anything.",
            "检查登录设计", "检查设计内容中会被忽略或过滤的部分，不会保存。",
            DesignSchema.object(null, Map.of("spec", DesignSchema.spec(false))), true,
            invocation -> {
                var warnings = DesignLinter.lint(spec(invocation.arguments().get("spec")));
                return Mono.just(McpToolResult.success(Map.of("warnings", warnings),
                    warnings.isEmpty() ? "No problems found." : String.join("\n", warnings)));
            });
    }

    private McpToolDefinition createDesign() {
        return tool("create_design", "Create a login design",
            "Store a new design. It is not used by the site until activate_design is called."
                + " Returns the generated name and any lint warnings." + GUIDE_FIRST,
            "创建登录设计", "新建一个登录界面设计，不会自动启用。",
            DesignSchema.object(null, Map.of("spec", DesignSchema.spec(true))), false,
            invocation -> {
                var spec = spec(invocation.arguments().get("spec"));
                return catalog.create(spec)
                    .onErrorMap(ExtensionException.class, AuthUiMcpToolProvider::invalid)
                    .map(template -> saved(template, spec, "Created"));
            });
    }

    private McpToolDefinition updateDesign() {
        var input = DesignSchema.object(null, Map.of(
            "name", Map.of("type", "string", "minLength", 1, "description", "Design name."),
            "version", Map.of("type", "integer",
                "description", "Version from get_design. The update fails if the design changed since."),
            "spec", DesignSchema.spec(true)));
        input.put("required", List.of("name", "spec"));
        return tool("update_design", "Replace a login design",
            "Replace a design's whole spec (read it with get_design first and send every field you"
                + " keep). If the design is active, the live login changes immediately." + GUIDE_FIRST,
            "更新登录设计", "整体替换一个设计的内容；若该设计已启用，站点立即生效。",
            input, false,
            invocation -> {
                var arguments = invocation.arguments();
                var name = (String) arguments.get("name");
                var spec = spec(arguments.get("spec"));
                var version = arguments.get("version") instanceof Number number ? number.longValue() : null;
                return catalog.update(name, spec, version)
                    .switchIfEmpty(Mono.error(notFound(name)))
                    .onErrorMap(OptimisticLockingFailureException.class, error -> new McpToolException(
                        "CONFLICT", "The design changed since it was read; call get_design and retry."))
                    .onErrorMap(ExtensionException.class, AuthUiMcpToolProvider::invalid)
                    .map(template -> saved(template, spec, "Updated"));
            });
    }

    private McpToolDefinition deleteDesign() {
        return tool("delete_design", "Delete a login design",
            "Delete a design. If it was active, the site returns to the plugin's base look.",
            "删除登录设计", "删除一个设计；若该设计正在使用，站点恢复默认样式。",
            nameInput("Design name."), false,
            invocation -> design(invocation)
                .flatMap(template -> catalog.delete(template.getMetadata().getName()))
                .map(template -> McpToolResult.success(Map.of("name", template.getMetadata().getName()),
                    "Deleted " + template.getMetadata().getName())));
    }

    private McpToolDefinition activateDesign() {
        var input = DesignSchema.object(null, Map.of("name", Map.of("type", "string",
            "description", "Design to use on the site. An empty string restores the plugin's base look.")));
        input.put("required", List.of("name"));
        return tool("activate_design", "Activate a login design",
            "Use a design for the site's login, signup and logout pages. This changes what every visitor"
                + " sees; confirm with the user first.",
            "启用登录设计", "把设计应用到全站登录、注册与退出页面；传空字符串恢复默认样式。",
            input, false,
            invocation -> {
                var name = (String) invocation.arguments().get("name");
                if (name == null || name.isBlank()) {
                    return catalog.deactivate().thenReturn(McpToolResult.success(
                        Map.of("active", ""), "The site uses the base look."));
                }
                return catalog.activate(name)
                    .switchIfEmpty(Mono.error(notFound(name)))
                    // Halo refreshes plugin settings asynchronously; pages switch within a moment.
                    .map(template -> McpToolResult.success(Map.of("active", name),
                        "Activated " + template.getSpec().getDisplayName() + "; the site uses it within a few seconds."));
            });
    }

    private McpToolDefinition previewDesign() {
        return tool("preview_design", "Get preview links for a login design",
            "Return console preview links for each page of a stored design, as a full page and in the"
                + " modal. Only a signed-in plugin manager can open them; give them to the user.",
            "预览登录设计", "返回设计在各页面（独立页与弹窗）的预览链接。",
            nameInput("Design name."), true,
            invocation -> design(invocation).map(template -> {
                var name = template.getMetadata().getName();
                var links = new LinkedHashMap<String, Object>();
                for (var view : PREVIEW_VIEWS) {
                    links.put(view, Map.of(
                        "page", previewLink(view, name, "page"),
                        "modal", previewLink(view, name, "modal")));
                }
                return McpToolResult.success(Map.of(
                    "name", name,
                    "studio", absolute("/console/auth-ui/designs/" + name),
                    "previews", links), "Open " + absolute("/console/auth-ui/designs/" + name));
            }));
    }

    private AuthTemplate.Spec spec(Object value) {
        try {
            return specs.read(value);
        } catch (IllegalArgumentException error) {
            throw new McpToolException("INVALID_ARGUMENT", error.getMessage());
        }
    }

    private Mono<AuthTemplate> design(McpToolInvocation invocation) {
        var name = (String) invocation.arguments().get("name");
        return catalog.get(name).switchIfEmpty(Mono.error(notFound(name)));
    }

    private McpToolResult saved(AuthTemplate template, AuthTemplate.Spec spec, String verb) {
        var warnings = DesignLinter.lint(spec);
        var name = template.getMetadata().getName();
        return McpToolResult.success(Map.of(
            "name", name,
            "version", template.getMetadata().getVersion(),
            "warnings", warnings,
            "studio", absolute("/console/auth-ui/designs/" + name)),
            verb + " " + name + (warnings.isEmpty() ? "" : " with " + warnings.size() + " warnings"));
    }

    private Map<String, Object> summary(AuthTemplate template, String active) {
        var spec = template.getSpec();
        var item = new LinkedHashMap<String, Object>();
        item.put("name", template.getMetadata().getName());
        item.put("displayName", spec.getDisplayName());
        item.put("description", spec.getDescription() == null ? "" : spec.getDescription());
        item.put("version", template.getMetadata().getVersion());
        item.put("active", template.getMetadata().getName().equals(active));
        return item;
    }

    private Map<String, Object> detail(AuthTemplate template, String active) {
        var item = summary(template, active);
        item.put("spec", specs.write(template.getSpec()));
        return item;
    }

    private String previewLink(String view, String name, String display) {
        return absolute("/auth-ui/preview/" + view + "?auth_ui_display=" + display + "&template=" + name);
    }

    /** Absolute link when the site's external URL is configured, otherwise a site path. */
    private String absolute(String path) {
        var base = externalUrl.getRaw();
        if (base == null) {
            return path;
        }
        var root = base.toString();
        return (root.endsWith("/") ? root.substring(0, root.length() - 1) : root) + path;
    }

    private static Map<String, Object> nameInput(String description) {
        var input = DesignSchema.object(null, Map.of("name",
            Map.of("type", "string", "minLength", 1, "description", description)));
        input.put("required", List.of("name"));
        return input;
    }

    private static McpToolDefinition tool(String name, String title, String description, String displayTitle,
        String displayDescription, Map<String, Object> input, boolean readOnly,
        run.halo.mcpserver.api.McpToolHandler handler) {
        return McpToolDefinition.builder()
            .name(name)
            .title(title)
            .description(description)
            .displayTitle(displayTitle)
            .displayDescription(displayDescription)
            .category(CATEGORY)
            .inputSchema(input)
            .annotations(readOnly
                ? McpToolAnnotations.readOnly(title)
                : new McpToolAnnotations(false, name.startsWith("delete"), name.startsWith("update")
                    || name.startsWith("activate"), false, title))
            .permission(invocation -> Mono.just(true))
            .handler(handler)
            .build();
    }

    private static McpToolException notFound(String name) {
        return new McpToolException("NOT_FOUND", "No design named " + name + "; call list_designs.");
    }

    /** Halo rejects a spec that breaks the AuthTemplate schema (e.g. a too long name). */
    private static McpToolException invalid(ExtensionException error) {
        return new McpToolException("INVALID_ARGUMENT", "The design was rejected: " + error.getMessage());
    }

}
