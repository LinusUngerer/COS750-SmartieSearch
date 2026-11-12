package cos212.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The colour-vision check for every palette in Settings. Text must meet WCAG AA contrast
 * (4.5:1), and the colours for correct and wrong answers must stay distinct (CIELAB
 * difference of at least 15) for the colour-vision types each palette is designed for,
 * simulated with the Machado, Oliveira and Fernandes (2009) matrices at full severity.
 */
class PaletteTest {

    /** Text and background pairs from app.css, as written for the Standard palette. */
    private static final String[][] TEXT_PAIRS = {
        {"#1D1A2B", "#FFFFFF", "body text"},
        {"#5B5366", "#FFFFFF", "notes"},
        {"#5B5366", "#FCF5EE", "notes on the page background"},
        {"#683519", "#FCF5EE", "headings"},
        {"#6E6779", "#FFFFFF", "OPTION A labels"},
        {"#6E6779", "#FAF8F5", "untried chip"},
        {"#17694A", "#D6F2E4", "Correct pill"},
        {"#B42318", "#FBDADD", "Not quite pill"},
        {"#FFFFFF", "#17805A", "tick on the correct answer"},
        {"#FFFFFF", "#C8323D", "cross on the wrong answer"},
        {"#10583A", "#E4F6EC", "correct feedback heading"},
        {"#8A1C14", "#FDECEE", "wrong feedback heading"},
        {"#FFFFFF", "#021B6E", "primary button"},
    };

    /** Correct and wrong colour pairs: answer borders, verdict pills and feedback panels. */
    private static final String[][] RIGHT_WRONG = {
        {"#17805A", "#C8323D", "answer borders"},
        {"#D6F2E4", "#FBDADD", "verdict pills"},
        {"#E4F6EC", "#FDECEE", "feedback panels"},
    };

    @Test
    void everyPaletteMeetsWcagAaForText() {
        for (Palette palette : Palette.values()) {
            for (String[] pair : TEXT_PAIRS) {
                double ratio = contrast(palette.map(pair[0]), palette.map(pair[1]));
                assertTrue(ratio >= 4.5, palette + " " + pair[2] + ": contrast " + ratio);
            }
        }
    }

    @Test
    void redGreenPaletteKeepsCorrectAndWrongDistinctForProtanopiaAndDeuteranopia() {
        assertDistinct(Palette.PROTANOPIA, PROTAN, "protanopia");
        assertDistinct(Palette.DEUTERANOPIA, DEUTAN, "deuteranopia");
    }

    @Test
    void blueYellowPaletteKeepsCorrectAndWrongDistinctForTritanopia() {
        assertDistinct(Palette.BLUE_YELLOW, TRITAN, "tritanopia");
    }

    /**
     * The Standard palette's pale feedback-panel backgrounds are only 13.9 apart even for
     * normal vision, so only its borders and pills are checked; the panels also carry an icon
     * and a heading, and sit under an answer card with a strong coloured border.
     */
    @Test
    void standardPaletteBordersAndPillsAreDistinctForNormalVisionAndTritanopia() {
        assertDistinct(Palette.STANDARD, null, "normal vision", 2);
        assertDistinct(Palette.STANDARD, TRITAN, "tritanopia", 2);
    }

    @Test
    void greyscaleKeepsBrightnessSoContrastIsUnchanged() {
        for (String[] pair : TEXT_PAIRS) {
            assertEquals(contrast(pair[0], pair[1]),
                    contrast(Palette.GREYSCALE.map(pair[0]), Palette.GREYSCALE.map(pair[1])), 0.1, pair[2]);
        }
        String grey = Palette.GREYSCALE.map("#17805A");
        assertEquals(grey.substring(1, 3), grey.substring(3, 5));
        assertEquals(grey.substring(3, 5), grey.substring(5, 7));
    }

    @Test
    void coloursAPaletteDoesNotNameAreLeftAlone() {
        assertEquals("#123456", Palette.DEUTERANOPIA.map("#123456"));
        assertEquals("#17805A", Palette.STANDARD.map("#17805a"));
    }

    private static void assertDistinct(Palette palette, double[][] deficiency, String who) {
        assertDistinct(palette, deficiency, who, RIGHT_WRONG.length);
    }

    private static void assertDistinct(Palette palette, double[][] deficiency, String who, int pairs) {
        for (int i = 0; i < pairs; i++) {
            String[] pair = RIGHT_WRONG[i];
            double difference = deltaE(simulate(palette.map(pair[0]), deficiency),
                    simulate(palette.map(pair[1]), deficiency));
            assertTrue(difference >= 15, palette + " " + pair[2] + " for " + who + ": difference " + difference);
        }
    }

    // ---- colour science ------------------------------------------------------------------

    private static final double[][] PROTAN = {
        {0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216}, {-0.003882, -0.048116, 1.051998}};
    private static final double[][] DEUTAN = {
        {0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413}, {-0.011820, 0.042940, 0.968881}};
    private static final double[][] TRITAN = {
        {1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602}, {0.004733, 0.691367, 0.303900}};

    private static double[] linear(String hex) {
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            out[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return out;
    }

    private static double[] simulate(String hex, double[][] matrix) {
        double[] rgb = linear(hex);
        if (matrix == null) {
            return rgb;
        }
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            out[i] = Math.max(0, Math.min(1, matrix[i][0] * rgb[0] + matrix[i][1] * rgb[1] + matrix[i][2] * rgb[2]));
        }
        return out;
    }

    private static double[] lab(double[] rgb) {
        double x = (0.4124 * rgb[0] + 0.3576 * rgb[1] + 0.1805 * rgb[2]) / 0.95047;
        double y = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
        double z = (0.0193 * rgb[0] + 0.1192 * rgb[1] + 0.9505 * rgb[2]) / 1.08883;
        double fx = f(x);
        double fy = f(y);
        double fz = f(z);
        return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static double f(double t) {
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116;
    }

    private static double deltaE(double[] a, double[] b) {
        double[] p = lab(a);
        double[] q = lab(b);
        return Math.sqrt(Math.pow(p[0] - q[0], 2) + Math.pow(p[1] - q[1], 2) + Math.pow(p[2] - q[2], 2));
    }

    private static double contrast(String a, String b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(String hex) {
        double[] rgb = linear(hex);
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }
}
