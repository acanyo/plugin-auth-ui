package com.xhhao.authui.theme;

import com.xhhao.authui.settings.AuthUiSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
@RequiredArgsConstructor
public class AuthUiHeadProcessor implements TemplateHeadProcessor {
    private final AuthUiSettingsService settings;

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler handler) {
        return settings.get().doOnNext(value -> {
            if (value.isModalEnabled()) {
                model.add(context.getModelFactory().createText(
                    "<script defer src=\"/plugins/auth-ui/assets/auth/modal.js?v=11\"></script>"));
            }
        }).then();
    }
}
