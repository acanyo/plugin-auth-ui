package com.xhhao.authui.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColorContrastTest {

    @Test
    void measuresOpaqueColours() {
        assertEquals(21.0, ColorContrast.ratio("#000", "#ffffff").orElseThrow(), 0.01);
        assertEquals(1.0, ColorContrast.ratio("rgb(10 20 30)", "#0a141e").orElseThrow(), 0.01);
        assertEquals(5.57, ColorContrast.ratio("#ffffff", "#0066cc").orElseThrow(), 0.01);
    }

    @Test
    void skipsColoursItCannotJudge() {
        assertTrue(ColorContrast.ratio("rgb(255 255 255 / .5)", "#000").isEmpty());
        assertTrue(ColorContrast.ratio("white", "#000").isEmpty());
        assertTrue(ColorContrast.ratio("color-mix(in srgb, red, blue)", "#000").isEmpty());
    }
}
