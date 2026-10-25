package com.xhhao.authui.design;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/** Reads a UTF-8 text file bundled with the plugin. */
final class ClasspathText {

    private ClasspathText() {
    }

    static String read(String path) {
        try (var input = new ClassPathResource(path, ClasspathText.class.getClassLoader()).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("Cannot read " + path, error);
        }
    }
}
