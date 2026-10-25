package com.xhhao.authui.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/** Resolves the design a page should render. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTemplateService {
    static final String DRAFT_NAME = "preview";

    private final ReactiveExtensionClient client;

    /**
     * The named template, or the plugin's base look when it is missing or unreadable.
     * A deleted or broken template must never prevent sign-in.
     */
    public Mono<RenderedTemplate> resolve(String name) {
        if (name == null || name.isBlank()) {
            return Mono.just(RenderedTemplate.BASE);
        }
        return client.fetch(AuthTemplate.class, name)
            // Halo deletes in two steps; a template being deleted no longer applies.
            .filter(template -> template.getMetadata().getDeletionTimestamp() == null)
            .map(RenderedTemplate::from)
            .defaultIfEmpty(RenderedTemplate.BASE)
            .onErrorResume(error -> {
                log.warn("Cannot read login template {}; using the base look", name, error);
                return Mono.just(RenderedTemplate.BASE);
            });
    }

    /** Renders an unsaved design for the console preview. */
    public RenderedTemplate render(AuthTemplate.Spec draft) {
        var template = new AuthTemplate();
        var metadata = new Metadata();
        metadata.setName(DRAFT_NAME);
        template.setMetadata(metadata);
        template.setSpec(draft);
        return RenderedTemplate.from(template);
    }
}
