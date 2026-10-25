package com.xhhao.authui.settings;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.json.JsonMapper;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.PluginContext;

/**
 * Writes the active design name to the {@code template} group of the plugin's ConfigMap, the
 * same value the console's design gallery saves. Other groups (Logo, display mode) are kept.
 */
@Component
public class ActiveTemplateWriter {
    static final String GROUP = "template";

    private final ReactiveExtensionClient client;
    private final String configMapName;

    public ActiveTemplateWriter(ReactiveExtensionClient client, PluginContext pluginContext) {
        this.client = client;
        this.configMapName = pluginContext.getConfigMapName();
    }

    /** Activates {@code name}; an empty name restores the plugin's base look. */
    public Mono<Void> setActive(String name) {
        // Halo stores each config group as a JSON string, e.g. {"name":"auth-template-abc"}.
        var value = JsonMapper.shared().writeValueAsString(Map.of("name", name == null ? "" : name));
        return Mono.defer(() -> client.fetch(ConfigMap.class, configMapName)
                .flatMap(configMap -> {
                    var data = configMap.getData() == null
                        ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(configMap.getData());
                    data.put(GROUP, value);
                    configMap.setData(data);
                    return client.update(configMap);
                })
                .switchIfEmpty(Mono.defer(() -> client.create(newConfigMap(value)))))
            // The console may save settings at the same time; retry on a version conflict.
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance))
            .then();
    }

    private ConfigMap newConfigMap(String value) {
        var configMap = new ConfigMap();
        var metadata = new Metadata();
        metadata.setName(configMapName);
        configMap.setMetadata(metadata);
        configMap.setData(new LinkedHashMap<>(Map.of(GROUP, value)));
        return configMap;
    }
}
