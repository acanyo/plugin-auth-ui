package com.xhhao.authui;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.xhhao.authui.template.AuthTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.PluginContext;

@ExtendWith(MockitoExtension.class)
class AuthUiPluginTest {

    @Mock
    PluginContext context;

    @Mock
    SchemeManager schemeManager;

    @InjectMocks
    AuthUiPlugin plugin;

    @Test
    void registersTemplateSchemeForPluginLifetime() {
        plugin.start();
        verify(schemeManager).register(AuthTemplate.class);
        plugin.stop();
        verify(schemeManager).unregister(argThat(scheme -> scheme.type() == AuthTemplate.class));
    }
}
