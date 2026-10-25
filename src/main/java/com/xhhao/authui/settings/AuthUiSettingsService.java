package com.xhhao.authui.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUiSettingsService {
    private final ReactiveSettingFetcher fetcher;

    public Mono<AuthUiSettings> get() {
        return fetcher.getSettingValues()
            .map(AuthUiSettings::from)
            .defaultIfEmpty(new AuthUiSettings())
            .onErrorResume(error -> {
                log.warn("Cannot read authentication appearance; using Halo pages", error);
                return Mono.just(AuthUiSettings.disabled());
            });
    }
}
