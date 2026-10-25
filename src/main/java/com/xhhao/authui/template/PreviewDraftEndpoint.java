package com.xhhao.authui.template;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * {@code POST /apis/api.authui.xhhao.com/v1alpha1/previews} stores an unsaved design and returns
 * an id for {@code /auth-ui/preview/{view}?draft={id}}. Access follows the role template.
 */
@Component
@RequiredArgsConstructor
public class PreviewDraftEndpoint implements CustomEndpoint {
    private final PreviewDraftStore drafts;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
            .POST("previews", this::create)
            .build();
    }

    private Mono<ServerResponse> create(ServerRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> context.getAuthentication().getName())
            .zipWith(request.bodyToMono(AuthTemplate.Spec.class)
                .switchIfEmpty(Mono.error(new ServerWebInputException("A design is required"))))
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", drafts.put(result.getT1(), result.getT2()))));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.authui.xhhao.com/v1alpha1");
    }
}
