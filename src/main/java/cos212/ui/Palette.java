package cos212.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The colour palettes a student can pick from in Settings. Each palette maps the colours in
 * app.css (and the few the tree canvas draws with) to replacements; any colour a palette does
 * not name is left as it is. Students pick from this fixed set and cannot enter their own
 * colours, so every palette can be checked in advance (see PaletteTest).
 *
 * <p>The three colour-blindness entries are simulations, not recolourings: they show the whole
 * app as a person with that deficiency sees it, by passing every colour through a matrix in
 * linear RGB. The five core SmartieSearch colours are pinned to the values in the supplied
 * reference palettes (Protan, Deutan and Tritan), and every other colour follows the same
 * matrix, so the whole interface stays consistent with those references.
 *
 * <p>Colour is never the only signal in the app: correct and wrong answers also carry a tick
 * or cross and a word, and the current tree node has a thick outline and the word "current"
 * (NFR5). The palettes make the colours themselves distinguishable as well.
 */
enum Palette {

    STANDARD("Standard colours", "The SmartieSearch colours.", Map.of()),

    DEUTERANOPIA("Red-green (green weak, deuteranopia)",
            "Simulates deuteranopia: how the app looks with no working green-sensitive cones."
                    + " Greens and reds turn to similar olive and brown shades.",
            Simulations.DEUTAN_PINNED, Simulations.DEUTAN),

    PROTANOPIA("Red-green (red weak, protanopia)",
            "Simulates protanopia: how the app looks with no working red-sensitive cones."
                    + " Reds turn dark and muddy and are easily mistaken for greens.",
            Simulations.PROTAN_PINNED, Simulations.PROTAN),

    BLUE_YELLOW("Blue-yellow (tritanopia)",
            "Simulates tritanopia: how the app looks with no working blue-sensitive cones."
                    + " Blues look teal and yellows look pink.",
            Simulations.TRITAN_PINNED, Simulations.TRITAN),

    HIGH_CONTRAST("High contrast",
            "For low vision: white background, black text, dark borders and strong status colours.",
            highContrast()),

    GREYSCALE("Grayscale",
            "For no colour vision (monochromacy). Every colour becomes a grey of the same brightness,"
                    + " so the app relies on ticks, crosses, words and outlines. Also used to check that"
                    + " nothing depends on colour alone.",
            null);

    /**
     * Finds a saved palette by name. Settings saved before red-green was split into its two
     * types used {@code RED_GREEN}, which now means deuteranopia.
     *
     * @throws IllegalArgumentException if the name is not a palette
     */
    static Palette fromName(String name) {
        return "RED_GREEN".equals(name) ? DEUTERANOPIA : valueOf(name);
    }

    final String label;
    final String description;
    private final Map<String, String> replacements;
    /** A 3x3 matrix applied to linear RGB, or null if this palette is not a simulation. */
    private final double[][] simulation;

    Palette(String label, String description, Map<String, String> replacements) {
        this(label, description, replacements, null);
    }

    Palette(String label, String description, Map<String, String> replacements, double[][] simulation) {
        this.label = label;
        this.description = description;
        this.replacements = replacements;
        this.simulation = simulation;
    }

    /**
     * @param hex a colour in the form {@code #RRGGBB}
     * @return the colour this palette uses in its place, in the same form
     */
    String map(String hex) {
        String key = hex.toUpperCase(Locale.ROOT);
        if (this == GREYSCALE) {
            return grey(key);
        }
        String named = replacements.get(key);
        if (named != null) {
            return named;
        }
        return simulation == null ? key : simulate(key, simulation);
    }

    /**
     * The colour-blindness simulations. Each matrix works on linear RGB and keeps white and black
     * unchanged (every row sums to one). The matrices were fitted to the reference palettes
     * (Protan.png, Deutan.png, Tritan.png). The pinned maps hold the exact reference value of each
     * of the five core colours in the default palette. They live in a nested class because enum
     * constants are built before the enum's own static fields.
     */
    private static final class Simulations {
        static final double[][] PROTAN = {
            {0.1590, 1.1227, -0.2817},
            {0.1063, 0.8593, 0.0344},
            {0.0025, 0.0087, 0.9888}};
        static final double[][] DEUTAN = {
            {0.3764, 0.8275, -0.2039},
            {0.2570, 0.6498, 0.0932},
            {-0.0219, 0.0405, 0.9814}};
        static final double[][] TRITAN = {
            {1.0050, 0.0194, -0.0244},
            {-0.0119, 0.9424, 0.0695},
            {0.0659, 0.3937, 0.5404}};

        // Default palette: Midnight Blue, Fresh Sky, Scarlet Rush, Dark Walnut, Soft Apricot.
        static final Map<String, String> PROTAN_PINNED = pinned(
                "#00216F", "#84A8EC", "#62593D", "#453C18", "#D7C99F");
        static final Map<String, String> DEUTAN_PINNED = pinned(
                "#00296E", "#73A0ED", "#8E7C31", "#504414", "#DCCD9E");
        static final Map<String, String> TRITAN_PINNED = pinned(
                "#002F40", "#00B2D8", "#D92950", "#693038", "#F0C0C5");

        private static Map<String, String> pinned(String... simulated) {
            String[] core = {"#021B6E", "#00AEEE", "#D92F3D", "#683519", "#EBC69F"};
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < core.length; i++) {
                map.put(core[i], simulated[i]);
            }
            return Map.copyOf(map);
        }
    }

    /** @return the colour as seen through a simulation matrix applied to linear RGB */
    private static String simulate(String hex, double[][] m) {
        double[] in = {
            linear(Integer.parseInt(hex.substring(1, 3), 16)),
            linear(Integer.parseInt(hex.substring(3, 5), 16)),
            linear(Integer.parseInt(hex.substring(5, 7), 16))};
        int[] out = new int[3];
        for (int row = 0; row < 3; row++) {
            double v = m[row][0] * in[0] + m[row][1] * in[1] + m[row][2] * in[2];
            out[row] = encode(v);
        }
        return String.format("#%02X%02X%02X", out[0], out[1], out[2]);
    }

    /** @return a linear channel value (clamped to 0..1) as an sRGB value 0..255 */
    private static int encode(double linear) {
        double y = Math.max(0, Math.min(1, linear));
        double s = y <= 0.0031308 ? 12.92 * y : 1.055 * Math.pow(y, 1 / 2.4) - 0.055;
        return (int) Math.round(Math.max(0, Math.min(1, s)) * 255);
    }

    /**
     * The status colours in app.css, in the order used by feedback(): strong, text, dark text,
     * panel background, panel border, pill background, card background. They live in a nested
     * class because enum constants are built before the enum's own static fields.
     */
    private static final class Families {
        static final String[] RIGHT = {"#17805A", "#17694A", "#10583A", "#E4F6EC", "#BFE6D0", "#D6F2E4",
            "#F2FBF6"};
        static final String[] WRONG = {"#C8323D", "#B42318", "#8A1C14", "#FDECEE", "#F4C3C8", "#FBDADD",
            "#FFF4F4"};
    }

    /** Replaces the correct (green) and wrong (red) families with the given fourteen colours. */
    private static Map<String, String> feedback(String... colours) {
        Map<String, String> map = new HashMap<>();
        int n = Families.RIGHT.length;
        for (int i = 0; i < n; i++) {
            map.put(Families.RIGHT[i], colours[i]);
            map.put(Families.WRONG[i], colours[n + i]);
        }
        // Other places that use the same green or red.
        map.put("#35A56A", colours[0]);
        map.put("#237A4B", colours[1]);
        map.put("#A8392F", colours[n + 1]);
        map.put("#EB566E", colours[n]);
        map.put("#D92F3D", colours[n + 1]);
        return Map.copyOf(map);
    }

    private static Map<String, String> highContrast() {
        Map<String, String> map = new HashMap<>(feedback(
                "#006B3C", "#00562F", "#003D21", "#E8F5EE", "#006B3C", "#D2EEDD", "#F3FBF6",
                "#B00020", "#8C0019", "#5E0011", "#FCE8EB", "#B00020", "#F8D3DA", "#FFF4F6"));
        // Backgrounds become white or light grey.
        map.put("#EBC69F", "#E6E6E6");
        map.put("#FCF5EE", "#FFFFFF");
        map.put("#FAF8F5", "#FFFFFF");
        map.put("#F1D4B7", "#F2F2F2");
        map.put("#F1EBE2", "#E0E0E0");
        map.put("#EEF0F7", "#FFFFFF");
        map.put("#EBD6BF", "#BDBDBD");
        // Text becomes black or near black.
        map.put("#1D1A2B", "#000000");
        map.put("#683519", "#000000");
        map.put("#3B1F0D", "#000000");
        map.put("#5B5366", "#1F1F1F");
        map.put("#6E6779", "#2E2E2E");
        map.put("#9A8F80", "#3A3A3A");
        map.put("#6B4A00", "#000000");
        map.put("#5A3E00", "#000000");
        // Borders become dark enough to see.
        for (String border : new String[] {"#E5D2BE", "#D8C7B1", "#C88755", "#DCC6B1", "#C9CEDB", "#D5C8B9",
            "#BDB3A8", "#D8D4DE", "#B9C6E4"}) {
            map.put(border, "#4A4A4A");
        }
        map.put("#021B6E", "#000080");
        return Map.copyOf(map);
    }

    /** @return a grey with the same relative luminance as the colour */
    private static String grey(String hex) {
        double r = linear(Integer.parseInt(hex.substring(1, 3), 16));
        double g = linear(Integer.parseInt(hex.substring(3, 5), 16));
        double b = linear(Integer.parseInt(hex.substring(5, 7), 16));
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double s = y <= 0.0031308 ? 12.92 * y : 1.055 * Math.pow(y, 1 / 2.4) - 0.055;
        int v = (int) Math.round(Math.max(0, Math.min(1, s)) * 255);
        return String.format("#%02X%02X%02X", v, v, v);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
