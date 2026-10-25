package com.xhhao.authui.assistant;

import java.time.Duration;
import java.util.Map;
import reactor.core.publisher.Mono;
import run.halo.aifoundation.AiModelService;
import run.halo.aifoundation.chat.GenerateTextRequest;
import run.halo.aifoundation.chat.GenerateTextResult;
import run.halo.aifoundation.chat.LanguageModel;
import run.halo.aifoundation.chat.GenerationTimeouts;
import run.halo.aifoundation.exception.AiGenerationTimeoutException;
import run.halo.aifoundation.exception.DefaultModelNotConfiguredException;
import run.halo.aifoundation.exception.ModelDisabledException;
import run.halo.aifoundation.exception.ModelNotFoundException;
import run.halo.aifoundation.exception.ProviderApiException;
import run.halo.aifoundation.exception.ProviderDisabledException;
import run.halo.aifoundation.exception.StructuredOutputValidationException;
import run.halo.aifoundation.schema.OutputSpec;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

/**
 * {@link DesignModel} backed by Halo AI Foundation: the language model the user chose in the
 * studio, or AI Foundation's default. Only this class refers to AI Foundation types; it is
 * created only when the API is on the class path.
 */
public class AiFoundationDesignModel implements DesignModel {
    static final Duration TIMEOUT = Duration.ofSeconds(150);
    static final int MAX_OUTPUT_TOKENS = 12_000;
    private static final String SETUP = "/console/ai-foundation/defaults";

    private final ExtensionGetter extensionGetter;

    public AiFoundationDesignModel(ExtensionGetter extensionGetter) {
        this.extensionGetter = extensionGetter;
    }

    @Override
    public Mono<Availability> availability(String modelName) {
        return service()
            .flatMap(service -> languageModel(service, modelName))
            .map(model -> Availability.ready())
            .onErrorResume(error -> Mono.just(unconfigured(error)))
            .defaultIfEmpty(Availability.unavailable("未启用 AI Foundation 插件。", "/console/plugins"));
    }

    @Override
    public Mono<Answer> generate(String modelName, String system, String prompt, Map<String, Object> schema) {
        var request = GenerateTextRequest.builder()
            .system(system)
            .prompt(prompt)
            // Non-strict: the design schema has many optional fields; AI Foundation still parses and
            // checks the answer locally, and the assistant lints the result.
            .output(OutputSpec.builder().type(run.halo.aifoundation.schema.OutputType.OBJECT)
                .name("auth_ui_design_change").schema(schema).build())
            .temperature(0.4)
            .maxOutputTokens(MAX_OUTPUT_TOKENS)
            .maxRetries(1)
            .timeouts(GenerationTimeouts.total(TIMEOUT))
            .build();
        return service()
            .switchIfEmpty(Mono.error(() -> new DesignModelException("UNAVAILABLE", "未启用 AI Foundation 插件。", null)))
            .flatMap(service -> languageModel(service, modelName))
            .flatMap(model -> model.generateText(request))
            .map(AiFoundationDesignModel::answer)
            .onErrorMap(error -> !(error instanceof DesignModelException), AiFoundationDesignModel::translate);
    }

    private Mono<AiModelService> service() {
        return extensionGetter.getEnabledExtension(AiModelService.class);
    }

    private static Mono<LanguageModel> languageModel(AiModelService service, String modelName) {
        return modelName == null || modelName.isBlank() ? service.languageModel() : service.languageModel(modelName);
    }

    @SuppressWarnings("unchecked")
    private static Answer answer(GenerateTextResult result) {
        if (!(result.getOutput() instanceof Map<?, ?> output)) {
            throw new DesignModelException("INVALID_OUTPUT", "模型没有返回有效的设计，请重试或换一个模型。", null);
        }
        var usage = result.getTotalUsage() != null ? result.getTotalUsage() : result.getUsage();
        var model = result.getResponse() != null ? result.getResponse().getModel() : null;
        return new Answer((Map<String, Object>) output, model == null ? "" : model,
            usage == null ? null : usage.getTotalTokens());
    }

    private static Availability unconfigured(Throwable error) {
        return Availability.unconfigured(translate(error).getMessage(), SETUP);
    }

    /** Maps AI Foundation failures to messages a site owner can act on. */
    static DesignModelException translate(Throwable error) {
        if (error instanceof DefaultModelNotConfiguredException) {
            return new DesignModelException("NOT_CONFIGURED", "AI Foundation 还没有设置默认语言模型，请选择一个模型或前往设置。", error);
        }
        if (error instanceof ModelNotFoundException || error instanceof ModelDisabledException
            || error instanceof ProviderDisabledException) {
            return new DesignModelException("NOT_CONFIGURED", "所选模型不可用，请换一个模型，或在 AI Foundation 中检查模型与提供商。", error);
        }
        if (error instanceof StructuredOutputValidationException) {
            return new DesignModelException("INVALID_OUTPUT", "模型返回的内容不符合设计格式，请重试或换一个更强的模型。", error);
        }
        if (error instanceof AiGenerationTimeoutException) {
            return new DesignModelException("TIMEOUT", "模型响应超时，请把需求拆小一些再试。", error);
        }
        if (error instanceof ProviderApiException provider) {
            return new DesignModelException("PROVIDER", "模型服务返回错误（" + provider.getStatusCode() + "），请检查 API Key、额度或稍后重试。", error);
        }
        return new DesignModelException("FAILED", "AI 生成失败，请稍后重试。", error);
    }
}
