package com.xhhao.authui.template;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Unsaved designs the console is previewing, kept in memory for a short time.
 *
 * <p>The design studio posts each edit here and loads the preview page with the returned id,
 * so changes show before they are saved. A draft is readable only by the user who created it.</p>
 */
@Component
public class PreviewDraftStore {
    static final Duration TTL = Duration.ofMinutes(15);
    static final int MAX_DRAFTS = 500;

    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Clock clock;

    public PreviewDraftStore() {
        this(Clock.systemUTC());
    }

    PreviewDraftStore(Clock clock) {
        this.clock = clock;
    }

    public String put(String owner, AuthTemplate.Spec spec) {
        evictExpired();
        if (drafts.size() >= MAX_DRAFTS) {
            // Drop the draft closest to expiry; previews are recreated on the next edit.
            drafts.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> a.expiresAt.compareTo(b.expiresAt)))
                .ifPresent(oldest -> drafts.remove(oldest.getKey()));
        }
        var id = UUID.randomUUID().toString();
        drafts.put(id, new Draft(owner, spec, clock.instant().plus(TTL)));
        return id;
    }

    public Optional<AuthTemplate.Spec> get(String id, String owner) {
        if (id == null || owner == null) {
            return Optional.empty();
        }
        var draft = drafts.get(id);
        if (draft == null || !draft.owner.equals(owner) || draft.expiresAt.isBefore(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(draft.spec);
    }

    private void evictExpired() {
        var now = clock.instant();
        drafts.values().removeIf(draft -> draft.expiresAt.isBefore(now));
    }

    private record Draft(String owner, AuthTemplate.Spec spec, Instant expiresAt) {
    }
}
