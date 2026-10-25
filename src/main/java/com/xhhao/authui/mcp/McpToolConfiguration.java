package com.xhhao.authui.mcp;

import com.xhhao.authui.design.DesignSpecs;
import com.xhhao.authui.template.DesignCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.halo.app.infra.ExternalUrlSupplier;

/**
 * Registers the MCP tools only when Halo MCP Server is installed and enabled. The class name is
 * a string because the API is missing from the class path without it; the plugin then runs as
 * usual without tools.
 */
@Configuration
@ConditionalOnClass(name = "run.halo.mcpserver.api.McpToolProvider")
public class McpToolConfiguration {

    @Bean
    AuthUiMcpToolProvider authUiMcpToolProvider(DesignCatalog catalog, ExternalUrlSupplier externalUrl) {
        return new AuthUiMcpToolProvider(catalog, new DesignSpecs(), externalUrl);
    }
}
