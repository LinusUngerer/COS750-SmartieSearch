package cos212.assess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagnosticTest {

    @Test
    void fourCorrectOutOfFivePasses() {
        Diagnostic diagnostic = new Diagnostic();
        for (int i = 0; i < 4; i++) {
            diagnostic.recordAnswer(true, "L1");
        }
        diagnostic.recordAnswer(false, "L2");
        assertTrue(diagnostic.isComplete());
        assertTrue(diagnostic.passed());
        assertEquals(4, diagnostic.correct());
    }

    @Test
    void threeCorrectOutOfFiveFailsAndRoutesToTheMissedLessons() {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.recordAnswer(true, "L1");
        diagnostic.recordAnswer(false, "L1");
        diagnostic.recordAnswer(true, "L3");
        diagnostic.recordAnswer(false, "L2");
        diagnostic.recordAnswer(true, "L4");
        assertFalse(diagnostic.passed());
        assertEquals(Set.of("L1", "L2"), diagnostic.missedLessons());
    }

    @Test
    void notPassedUntilEveryItemIsAnswered() {
        Diagnostic diagnostic = new Diagnostic();
        for (int i = 0; i < 4; i++) {
            diagnostic.recordAnswer(true, "L1");
        }
        assertFalse(diagnostic.passed());
    }

    @Test
    void cannotAnswerMoreItemsThanTheGateHas() {
        Diagnostic diagnostic = new Diagnostic();
        for (int i = 0; i < Diagnostic.ITEM_COUNT; i++) {
            diagnostic.recordAnswer(true, "L1");
        }
        assertThrows(IllegalStateException.class, () -> diagnostic.recordAnswer(true, "L1"));
    }

    @Test
    void resetClearsTheAnswers() {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.recordAnswer(false, "L2");
        diagnostic.reset();
        assertEquals(0, diagnostic.answered());
        assertTrue(diagnostic.missedLessons().isEmpty());
    }
}
