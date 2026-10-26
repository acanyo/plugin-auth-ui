package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PreviewDraftStoreTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-24T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static AuthTemplate.Spec spec(String name) {
        var spec = new AuthTemplate.Spec();
        spec.setDisplayName(name);
        return spec;
    }

    @Test
    void returnsDraftToItsOwner() {
        var store = new PreviewDraftStore(new MutableClock());
        var id = store.put("admin", spec("草稿"));
        assertEquals("草稿", store.get(id, "admin").orElseThrow().getDisplayName());
    }

    @Test
    void hidesDraftFromOtherUsers() {
        var store = new PreviewDraftStore(new MutableClock());
        var id = store.put("admin", spec("草稿"));
        assertTrue(store.get(id, "editor").isEmpty());
        assertTrue(store.get(null, "admin").isEmpty());
    }

    @Test
    void expiresDrafts() {
        var clock = new MutableClock();
        var store = new PreviewDraftStore(clock);
        var id = store.put("admin", spec("草稿"));
        clock.now = clock.now.plus(PreviewDraftStore.TTL).plusSeconds(1);
        assertTrue(store.get(id, "admin").isEmpty());
    }
}
