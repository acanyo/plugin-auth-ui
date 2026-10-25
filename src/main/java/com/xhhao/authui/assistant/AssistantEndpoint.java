package com.xhhao.authui.assistant;

import com.xhhao.authui.design.DesignSpecs;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * The design studio's AI assistant, under {@code /apis/api.authui.xhhao.com/v1alpha1}.
 *
 * <ul>
 *   <li>{@code GET assistant?modelName=} reports whether the model (default when omitted) is
 *   available;</li>
 *   <li>{@code POST suggestions} returns a new draft for
 *   {@code {design, instruction, history, allowScript, modelName}}. Nothing is saved.</li>
 * </ul>
 * Access follows the role template: only users who manage plugins.
 */
@Component
public class AssistantEndpoint implements CustomEndpoint {
    private final DesignAssistant assistant;
    private final DesignSpecs specs = new DesignSpecs();

    public AssistantEndpoint(DesignAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
            .GET("assistant", this::status)
            .POST("suggestions", this::suggest)
            .build();
    }

    private Mono<ServerResponse> status(ServerRequest request) {
        var modelName = request.queryParam("modelName").orElse("");
        return assistant.availability(modelName).flatMap(availability -> json(HttpStatus.OK, Map.of(
            "available", availability.available(),
            "canChooseModel", availability.canChooseModel(),
            "reason", availability.reason(),
            "setupUrl", availability.setupHint())));
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> suggest(ServerRequest request) {
        return request.bodyToMono(Map.class)
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("A request body is required.")))
            .flatMap(body -> {
                var design = specs.read(body.getOrDefault("design", Map.of()));
                return assistant.suggest(new DesignAssistant.Request(design, text(body.get("instruction")),
                    history(body.get("history")), Boolean.TRUE.equals(body.get("allowScript")),
                    text(body.get("modelName"))));
            })
            .flatMap(suggestion -> {
                var result = new LinkedHashMap<String, Object>();
                result.put("design", suggestion.design());
                result.put("summary", suggestion.summary());
                result.put("changed", suggestion.changed());
                result.put("warnings", suggestion.warnings());
                result.put("model", suggestion.model());
                result.put("tokens", suggestion.tokens());
                result.put("turns", suggestion.turns());
                return json(HttpStatus.OK, result);
            })
            .onErrorResume(DesignModel.DesignModelException.class, error ->
                json(HttpStatus.UNPROCESSABLE_CONTENT, Map.of("code", error.code(), "message", error.getMessage())))
            .onErrorResume(IllegalArgumentException.class, error ->
                json(HttpStatus.BAD_REQUEST, Map.of("code", "INVALID_REQUEST", "message", error.getMessage())));
    }

    private static String text(Object value) {
        return value instanceof String text ? text : "";
    }

    private static List<String> history(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        return items.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static Mono<ServerResponse> json(HttpStatus status, Map<String, ?> body) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.authui.xhhao.com/v1alpha1");
    }
}
