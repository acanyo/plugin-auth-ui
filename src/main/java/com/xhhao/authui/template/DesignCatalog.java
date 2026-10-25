package com.xhhao.authui.template;

import com.xhhao.authui.settings.ActiveTemplateWriter;
import com.xhhao.authui.settings.AuthUiSettings;
import com.xhhao.authui.settings.AuthUiSettingsService;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * Reads and writes stored designs and the active design, for integrations such as MCP tools.
 * The console does the same through Halo's extension REST API.
 */
@Service
public class DesignCatalog {
    static final String GENERATE_NAME = "auth-template-";

    private final ReactiveExtensionClient client;
    private final AuthUiSettingsService settings;
    private final ActiveTemplateWriter activeTemplate;

    public DesignCatalog(ReactiveExtensionClient client, AuthUiSettingsService settings,
        ActiveTemplateWriter activeTemplate) {
        this.client = client;
        this.settings = settings;
        this.activeTemplate = activeTemplate;
    }

    /** Stored designs in console order: priority, then name. Designs being deleted are left out. */
    public Flux<AuthTemplate> list() {
        return client.list(AuthTemplate.class, DesignCatalog::isLive,
            Comparator.comparing((AuthTemplate t) -> Objects.requireNonNullElse(t.getSpec().getPriority(), 0))
                .thenComparing(t -> Objects.requireNonNullElse(t.getSpec().getDisplayName(), "")));
    }

    /** The design, or empty when it does not exist or is being deleted. */
    public Mono<AuthTemplate> get(String name) {
        return client.fetch(AuthTemplate.class, name).filter(DesignCatalog::isLive);
    }

    public Mono<AuthTemplate> create(AuthTemplate.Spec spec) {
        var template = new AuthTemplate();
        var metadata = new Metadata();
        metadata.setGenerateName(GENERATE_NAME);
        template.setMetadata(metadata);
        template.setSpec(spec);
        return client.create(template);
    }

    /**
     * Replaces the design's spec. With {@code expectedVersion}, fails with
     * {@link OptimisticLockingFailureException} when someone saved the design in between.
     */
    public Mono<AuthTemplate> update(String name, AuthTemplate.Spec spec, Long expectedVersion) {
        return get(name).flatMap(template -> {
            if (expectedVersion != null && !expectedVersion.equals(template.getMetadata().getVersion())) {
                return Mono.error(new OptimisticLockingFailureException(
                    "Design " + name + " changed since version " + expectedVersion));
            }
            template.setSpec(spec);
            return client.update(template);
        });
    }

    /** Deletes the design; when it was active, the site returns to the base look. */
    public Mono<AuthTemplate> delete(String name) {
        return get(name).flatMap(template -> activeName()
            .flatMap(active -> active.equals(name) ? activeTemplate.setActive("") : Mono.empty())
            .then(client.delete(template)));
    }

    /** Name of the design the site uses; empty for the base look. */
    public Mono<String> activeName() {
        return settings.get().map(AuthUiSettings::getTemplateName);
    }

    /** Makes the design live on the site. Emits empty when it does not exist. */
    public Mono<AuthTemplate> activate(String name) {
        return get(name).flatMap(template -> activeTemplate.setActive(name).thenReturn(template));
    }

    /** Restores the plugin's base look. */
    public Mono<Void> deactivate() {
        return activeTemplate.setActive("");
    }

    private static boolean isLive(AuthTemplate template) {
        // Halo deletes in two steps; a design being deleted no longer counts.
        return template.getMetadata().getDeletionTimestamp() == null;
    }
}
