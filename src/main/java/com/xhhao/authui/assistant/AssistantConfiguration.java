package com.xhhao.authui.assistant;

import com.xhhao.authui.design.DesignSpecs;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

/**
 * Wires the design assistant. With AI Foundation on the class path it uses AI Foundation's
 * models; without it the assistant reports itself unavailable.
 */
@Configuration
public class AssistantConfiguration {
    // A string, because the class is missing when AI Foundation is not installed.
    static final String AI_FOUNDATION_API = "run.halo.aifoundation.AiModelService";

    @Bean
    DesignAssistant designAssistant(ExtensionGetter extensionGetter) {
        var available = ClassUtils.isPresent(AI_FOUNDATION_API, AssistantConfiguration.class.getClassLoader());
        // AiFoundationDesignModel is only loaded on this branch, so its AI Foundation imports never resolve otherwise.
        DesignModel model = available ? new AiFoundationDesignModel(extensionGetter) : new UnavailableDesignModel();
        return new DesignAssistant(model, new DesignSpecs());
    }

    /** Used when AI Foundation is not installed. */
    static final class UnavailableDesignModel implements DesignModel {
        static final String MESSAGE = "安装并启用 Halo AI Foundation 插件后即可使用 AI 设计。";
        static final String STORE = "https://www.halo.run/store/apps/app-acslk9nu";

        @Override
        public Mono<Availability> availability(String modelName) {
            return Mono.just(Availability.unavailable(MESSAGE, STORE));
        }

        @Override
        public Mono<Answer> generate(String modelName, String system, String prompt, Map<String, Object> schema) {
            return Mono.error(new DesignModelException("UNAVAILABLE", MESSAGE, null));
        }
    }
}
