package com.xhhao.authui.template;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * A login page design stored as data, so the console and future integrations (e.g. an MCP
 * server) can create and edit designs through Halo's generic extension API.
 *
 * <p>A design has three levels, each optional:</p>
 * <ol>
 *   <li>{@link Theme}: colours, shape, layout, background and animations, edited visually;</li>
 *   <li>{@link Slots}: HTML placed around Halo's forms (brand panel, above and below the form);</li>
 *   <li>{@link Code}: free CSS and JavaScript for anything the first two do not cover.</li>
 * </ol>
 * Halo's native forms are always rendered by Halo, so a design can never break sign-in.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@GVK(group = AuthTemplate.GROUP, version = AuthTemplate.VERSION, kind = AuthTemplate.KIND,
    singular = "authtemplate", plural = "authtemplates")
public class AuthTemplate extends AbstractExtension {
    public static final String GROUP = "authui.xhhao.com";
    public static final String VERSION = "v1alpha1";
    public static final String KIND = "AuthTemplate";

    @Schema(requiredMode = REQUIRED)
    private Spec spec = new Spec();

    @Data
    public static class Spec {
        @Schema(requiredMode = REQUIRED, minLength = 1, maxLength = 40)
        private String displayName;

        @Schema(maxLength = 200)
        private String description;

        @Schema(description = "Text on the brand panel for the login page (split layout).", maxLength = 80)
        private String loginSlogan;

        @Schema(description = "Text on the brand panel for the signup page (split layout).", maxLength = 80)
        private String signupSlogan;

        private Theme theme;

        private Slots slots;

        private Code code;

        /** Sort order in the console; lower first. */
        private Integer priority = 0;
    }

    /** Visual settings; every value is validated before it reaches the page. */
    @Data
    public static class Theme {
        @Schema(description = "Primary colour, e.g. #176b62.")
        private String primary;

        @Schema(description = "Text colour on the primary colour.")
        private String onPrimary;

        @Schema(description = "Card background colour.")
        private String surface;

        private String text;

        private String muted;

        private String border;

        @Schema(description = "Input background colour.")
        private String field;

        @Schema(allowableValues = {"light", "dark", "system"})
        private String colorScheme;

        @Schema(description = "Card corner radius in px.", minimum = "0", maximum = "40")
        private Integer radius;

        @Schema(description = "Form width in px.", minimum = "300", maximum = "560")
        private Integer formWidth;

        @Schema(allowableValues = {"none", "soft", "strong"})
        private String shadow;

        @Schema(allowableValues = {"centered", "split", "split-reverse", "fullscreen"})
        private String layout;

        @Schema(description = "Font family stack.", maxLength = 200)
        private String font;

        private Background background;

        private Brand brand;

        private Motion motion;

        /** Extra CSS custom properties, e.g. {@code --auth-brand-gap}; names must start with {@code --}. */
        private Map<String, String> variables = new LinkedHashMap<>();
    }

    @Data
    public static class Background {
        @Schema(allowableValues = {"solid", "gradient", "image", "mesh", "aurora"})
        private String type;

        private String color;

        private String color2;

        private String color3;

        @Schema(description = "Gradient angle in degrees.", minimum = "0", maximum = "360")
        private Integer angle;

        private String image;

        @Schema(description = "Darkens an image background, 0-0.9.", minimum = "0", maximum = "0.9")
        private Double overlay;

        @Schema(description = "Blur applied to an image background in px.", minimum = "0", maximum = "40")
        private Integer blur;
    }

    /** The brand panel shown beside the form in split layouts. */
    @Data
    public static class Brand {
        private String background;

        private String image;

        @Schema(allowableValues = {"orbit", "grid", "waves", "none"})
        private String decoration;
    }

    @Data
    public static class Motion {
        @Schema(description = "How the card enters.", allowableValues = {"none", "fade", "rise", "zoom", "slide", "flip"})
        private String entrance;

        @Schema(description = "Animated background effect.", allowableValues = {"none", "gradient", "float", "particles", "aurora"})
        private String ambient;

        @Schema(description = "Reveal form fields one after another.")
        private Boolean stagger;

        @Schema(description = "Lift, glow and press feedback on inputs and buttons.")
        private Boolean interactive;

        @Schema(allowableValues = {"slow", "normal", "fast"})
        private String speed;
    }

    /**
     * HTML placed around Halo's forms. Sanitised before rendering: no scripts, event handlers or
     * forms, and links are limited to http(s) and site paths.
     */
    @Data
    public static class Slots {
        @Schema(description = "Content of the brand panel (split layouts).", maxLength = 20000)
        private String aside;

        @Schema(description = "Shown above the form title.", maxLength = 10000)
        private String header;

        @Schema(description = "Shown below the form.", maxLength = 10000)
        private String footer;
    }

    @Data
    public static class Code {
        @Schema(description = "CSS, scoped to #auth-ui when rendered.", maxLength = 50000)
        private String css;

        /**
         * JavaScript run after the page is ready. It runs on the site's own login page, so only
         * people who manage plugins can edit it.
         */
        @Schema(description = "JavaScript run on the login page after it is ready.", maxLength = 50000)
        private String js;
    }
}
