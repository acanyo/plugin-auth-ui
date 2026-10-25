package com.xhhao.authui;

import com.xhhao.authui.template.AuthTemplate;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * Registers the plugin's custom models for the plugin's lifetime.
 *
 * @author Handsome
 * @since 1.0.0
 */
@Component
public class AuthUiPlugin extends BasePlugin {
    private final SchemeManager schemeManager;

    public AuthUiPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(AuthTemplate.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(AuthTemplate.class));
    }
}
