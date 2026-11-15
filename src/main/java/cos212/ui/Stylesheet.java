package cos212.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the app's stylesheet from app.css and the current settings. Every font size (and the
 * fixed heights of controls that hold text) is multiplied by the text size scale, and every
 * colour (hex and rgba) is passed through the chosen palette, so one change applies to every screen at once.
 * The result is handed to JavaFX as a data URI.
 */
final class Stylesheet {

    private static final Pattern SIZE =
            Pattern.compile("(-fx-(?:font-size|pref-height|min-height):\\s*)(\\d+(?:\\.\\d+)?)(px)?");
    private static final Pattern COLOUR = Pattern.compile("#[0-9A-Fa-f]{6}\\b");
    /** The colour part of rgba(r, g, b, a): shadows, overlays and glows, which also need the palette. */
    private static final Pattern RGBA =
            Pattern.compile("rgba\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,");

    /** A thick amber outline on whatever has keyboard focus, for keyboard-only and low-vision use. */
    static final String STRONG_FOCUS = """

            /* Strong keyboard focus outline (Settings) */
            .button:focused, .option:focused, .text-field:focused, .settings-option:focused,
            .check-box:focused, .radio-button:focused, .nav-item:focused {
                -fx-border-color: #FFB000;
                -fx-border-width: 3;
                -fx-border-radius: 12;
            }
            """;

    private static String base;

    private Stylesheet() {
    }

    /** @return the stylesheet for the current settings, as a data URI JavaFX can load */
    static String uri(Settings settings) {
        String css = transform(base(), settings.textSize(), settings.palette(),
                settings.strongFocusProperty().get());
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Applies a text size and palette to stylesheet text.
     *
     * @param css the stylesheet
     * @param size the text size; its scale multiplies font sizes and the heights of text controls
     * @param palette the palette every colour is mapped through
     * @param strongFocus whether to add the strong keyboard focus outline
     * @return the changed stylesheet
     */
    static String transform(String css, Settings.TextSize size, Palette palette, boolean strongFocus) {
        String text = strongFocus ? css + STRONG_FOCUS : css;
        Matcher sizes = SIZE.matcher(text);
        StringBuilder scaled = new StringBuilder();
        while (sizes.find()) {
            double value = Double.parseDouble(sizes.group(2)) * size.scale;
            String unit = sizes.group(3) == null ? "" : sizes.group(3);
            sizes.appendReplacement(scaled, Matcher.quoteReplacement(
                    sizes.group(1) + String.format(Locale.ROOT, "%.1f", value) + unit));
        }
        sizes.appendTail(scaled);

        Matcher colours = COLOUR.matcher(scaled);
        StringBuilder mapped = new StringBuilder();
        while (colours.find()) {
            colours.appendReplacement(mapped, Matcher.quoteReplacement(palette.map(colours.group())));
        }
        colours.appendTail(mapped);
        return mapRgba(mapped.toString(), palette);
    }

    /** Passes the red, green and blue of every rgba(...) colour through the palette; alpha is kept. */
    private static String mapRgba(String css, Palette palette) {
        if (palette == Palette.STANDARD) {
            return css;
        }
        Matcher rgba = RGBA.matcher(css);
        StringBuilder out = new StringBuilder();
        while (rgba.find()) {
            String hex = String.format("#%02X%02X%02X", Math.min(255, Integer.parseInt(rgba.group(1))),
                    Math.min(255, Integer.parseInt(rgba.group(2))), Math.min(255, Integer.parseInt(rgba.group(3))));
            String mappedHex = palette.map(hex);
            rgba.appendReplacement(out, Matcher.quoteReplacement("rgba(" + Integer.parseInt(mappedHex.substring(1, 3), 16)
                    + ", " + Integer.parseInt(mappedHex.substring(3, 5), 16)
                    + ", " + Integer.parseInt(mappedHex.substring(5, 7), 16) + ","));
        }
        rgba.appendTail(out);
        return out.toString();
    }

    private static synchronized String base() {
        if (base == null) {
            try (InputStream in = Stylesheet.class.getResourceAsStream("app.css")) {
                if (in == null) {
                    throw new IllegalStateException("app.css is missing from the build");
                }
                base = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return base;
    }
}
