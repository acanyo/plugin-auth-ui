package com.xhhao.authui.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.halo.aifoundation.AiModelService;
import run.halo.aifoundation.chat.LanguageModel;
import run.halo.aifoundation.exception.DefaultModelNotConfiguredException;
import run.halo.aifoundation.exception.ProviderApiException;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

class AiFoundationDesignModelTest {

    @Test
    void reportsMissingPlugin() {
        var getter = mock(ExtensionGetter.class);
        when(getter.getEnabledExtension(AiModelService.class)).thenReturn(Mono.empty());
        var availability = new AiFoundationDesignModel(getter).availability(null).block();
        assertEquals(false, availability.available());
        assertEquals(false, availability.canChooseModel());
    }

    @Test
    void reportsMissingDefaultModel() {
        var getter = mock(ExtensionGetter.class);
        var service = mock(AiModelService.class);
        when(getter.getEnabledExtension(AiModelService.class)).thenReturn(Mono.just(service));
        when(service.languageModel()).thenReturn(Mono.error(new DefaultModelNotConfiguredException("none")));
        var availability = new AiFoundationDesignModel(getter).availability("").block();
        assertEquals(false, availability.available());
        assertEquals(true, availability.canChooseModel(), "another model can still be picked");
        assertTrue(availability.reason().startsWith("AI Foundation 还没有设置默认语言模型"));
        assertEquals("/console/ai-foundation/defaults", availability.setupHint());
    }

    @Test
    void usesTheNamedModel() {
        var getter = mock(ExtensionGetter.class);
        var service = mock(AiModelService.class);
        when(getter.getEnabledExtension(AiModelService.class)).thenReturn(Mono.just(service));
        when(service.languageModel("chosen")).thenReturn(Mono.just(mock(LanguageModel.class)));
        assertEquals(true, new AiFoundationDesignModel(getter).availability("chosen").block().available());
        verify(service, never()).languageModel();
    }

    @Test
    void explainsProviderErrors() {
        var error = AiFoundationDesignModel.translate(new ProviderApiException("bad key", 401, "openailike"));
        assertEquals("PROVIDER", error.code());
        assertEquals(true, error.getMessage().contains("401"));
    }
}
