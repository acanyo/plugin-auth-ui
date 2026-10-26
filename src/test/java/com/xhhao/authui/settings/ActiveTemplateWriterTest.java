package com.xhhao.authui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pf4j.RuntimeMode;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.PluginContext;

class ActiveTemplateWriterTest {
    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);
    private final ActiveTemplateWriter writer = new ActiveTemplateWriter(client,
        new PluginContext("auth-ui", "auth-ui-config", "1.0.0", RuntimeMode.DEVELOPMENT));

    private static ConfigMap configMap() {
        var configMap = new ConfigMap();
        var metadata = new Metadata();
        metadata.setName("auth-ui-config");
        configMap.setMetadata(metadata);
        configMap.setData(Map.of("basic", "{\"displayMode\":\"modal\"}", "template", "{\"name\":\"old\"}"));
        return configMap;
    }

    @Test
    void replacesOnlyTheTemplateGroup() {
        when(client.fetch(ConfigMap.class, "auth-ui-config")).thenReturn(Mono.just(configMap()));
        when(client.update(any(ConfigMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        writer.setActive("auth-template-a").block();
        var saved = ArgumentCaptor.forClass(ConfigMap.class);
        verify(client).update(saved.capture());
        assertEquals("{\"name\":\"auth-template-a\"}", saved.getValue().getData().get("template"));
        assertEquals("{\"displayMode\":\"modal\"}", saved.getValue().getData().get("basic"));
    }

    @Test
    void retriesWhenTheConsoleSavedAtTheSameTime() {
        when(client.fetch(ConfigMap.class, "auth-ui-config")).thenAnswer(invocation -> Mono.just(configMap()));
        when(client.update(any(ConfigMap.class)))
            .thenReturn(Mono.error(new OptimisticLockingFailureException("conflict")))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        writer.setActive("").block();
        verify(client, times(2)).update(any(ConfigMap.class));
    }

    @Test
    void createsTheConfigMapWhenMissing() {
        when(client.fetch(ConfigMap.class, "auth-ui-config")).thenReturn(Mono.empty());
        when(client.create(any(ConfigMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        writer.setActive("auth-template-a").block();
        verify(client).create(any(ConfigMap.class));
    }
}
