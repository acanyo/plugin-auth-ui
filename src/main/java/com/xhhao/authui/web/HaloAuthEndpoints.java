package com.xhhao.authui.web;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Halo's native login, signup and logout handlers plus the codecs and view resolvers needed to
 * render their responses.
 *
 * <p>These beans live in Halo's root context, which plugins cannot inject. Web filters still
 * receive the root context on the exchange, so they are looked up there once. A handler missing
 * in a future Halo version leaves that page to Halo.</p>
 */
@Slf4j
@Component
public class HaloAuthEndpoints {
    private final AtomicReference<Optional<Endpoints>> cached = new AtomicReference<>();

    public Optional<Endpoints> resolve(ServerWebExchange exchange) {
        var current = cached.get();
        if (current != null) {
            return current;
        }
        var resolved = lookup(exchange.getApplicationContext());
        // Only cache a success; the context may simply be missing on this exchange.
        if (resolved.isPresent()) {
            cached.compareAndSet(null, resolved);
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private static Optional<Endpoints> lookup(ApplicationContext context) {
        if (context == null) {
            return Optional.empty();
        }
        try {
            var routers = new EnumMap<AuthView, RouterFunction<ServerResponse>>(AuthView.class);
            for (var view : AuthView.values()) {
                if (context.containsBean(view.routerBean())) {
                    routers.put(view, (RouterFunction<ServerResponse>)
                        context.getBean(view.routerBean(), RouterFunction.class));
                }
            }
            var codecs = context.getBean(ServerCodecConfigurer.class);
            var viewResolvers = context.getBeanProvider(ViewResolver.class).orderedStream().toList();
            if (viewResolvers.isEmpty() || !routers.containsKey(AuthView.LOGIN)) {
                return Optional.empty();
            }
            return Optional.of(new Endpoints(Map.copyOf(routers), codecs, viewResolvers));
        } catch (BeansException | ClassCastException e) {
            log.warn("Halo authentication handlers are unavailable; keeping Halo pages", e);
            return Optional.empty();
        }
    }

    public record Endpoints(Map<AuthView, RouterFunction<ServerResponse>> routers,
                            ServerCodecConfigurer codecs,
                            List<ViewResolver> viewResolvers) implements ServerResponse.Context {
        public List<HttpMessageReader<?>> readers() {
            return codecs.getReaders();
        }

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return codecs.getWriters();
        }

        Optional<RouterFunction<ServerResponse>> router(AuthView view) {
            return Optional.ofNullable(routers.get(view));
        }
    }
}
