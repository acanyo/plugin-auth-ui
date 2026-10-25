package com.xhhao.authui.web;

import com.xhhao.authui.settings.AuthUiSettings;
import com.xhhao.authui.template.AuthTemplateService;
import com.xhhao.authui.template.RenderedTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.function.server.RenderingResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Halo 2.27 adapter. Native handlers prepare every model and process each form; only the
 * template used to render the result changes.
 */
@Slf4j
@Component
public class AuthPageRenderer {
    static final String TEMPLATE = "plugin:auth-ui:authentication";

    private final HaloAuthEndpoints endpoints;
    private final AuthTemplateService templates;

    public AuthPageRenderer(HaloAuthEndpoints endpoints, AuthTemplateService templates) {
        this.endpoints = endpoints;
        this.templates = templates;
    }

    /**
     * Runs Halo's native handler for {@code view} and restyles its page.
     *
     * @param fallback used when Halo's handlers are unavailable or do not accept the request
     */
    public Mono<Void> render(ServerWebExchange exchange, AuthView view, AuthUiSettings config,
        RenderOptions options, Mono<Void> fallback) {
        var halo = endpoints.resolve(exchange);
        var router = halo.flatMap(value -> value.router(view));
        if (router.isEmpty()) {
            return fallback;
        }
        var request = ServerRequest.create(exchange, halo.get().readers());
        var template = options.draft() != null
            ? Mono.just(templates.render(options.draft()))
            : templates.resolve(options.templateName() != null
                ? options.templateName() : config.getTemplateName());
        return router.get().route(request)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .flatMap(handler -> handler.isEmpty() ? fallback
                : write(exchange, Mono.zip(handler.get().handle(request), template)
                    .flatMap(result -> restyle(result.getT1(), view, config, result.getT2(), options)))
                    // A design that fails to render must never block sign-in: show Halo's page.
                    .onErrorResume(error -> {
                        if (exchange.getResponse().isCommitted()) {
                            return Mono.error(error);
                        }
                        log.warn("Cannot render the {} page with the design; using Halo's page", view.id(), error);
                        return fallback;
                    }));
    }

    /** Writes a response with Halo's codecs and view resolvers; redirects need neither. */
    public Mono<Void> write(ServerWebExchange exchange, Mono<ServerResponse> response) {
        ServerResponse.Context context = endpoints.resolve(exchange)
            .<ServerResponse.Context>map(value -> value).orElse(EMPTY_CONTEXT);
        return response.flatMap(value -> value.writeTo(exchange, context));
    }

    private static final ServerResponse.Context EMPTY_CONTEXT = new ServerResponse.Context() {
        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return List.of();
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return List.of();
        }
    };

    private static Mono<ServerResponse> restyle(ServerResponse response, AuthView view,
        AuthUiSettings config, RenderedTemplate template, RenderOptions options) {
        // Redirects (e.g. a successful signup) and unexpected templates keep Halo's response.
        if (!(response instanceof RenderingResponse rendering) || !view.renders(rendering.name())) {
            return Mono.just(response);
        }
        var model = new HashMap<>(rendering.model());
        model.put("authUi", config);
        model.put("authUiTemplate", template);
        var pageId = view.pageId(rendering.name());
        model.put("authUiView", pageId);
        model.put("authUiCopy", PageCopy.of(pageId));
        model.put("authUiLogin", view == AuthView.LOGIN);
        model.put("authUiPreview", options.preview());
        model.put("authUiAutoSubmit", options.autoSubmit());
        model.put("_templateId", TEMPLATE);
        return RenderingResponse.create(TEMPLATE)
            .status(response.statusCode()).headers(response.headers())
            .cookies(cookies -> cookies.addAll(response.cookies()))
            .modelAttributes(model).build().cast(ServerResponse.class);
    }
}
