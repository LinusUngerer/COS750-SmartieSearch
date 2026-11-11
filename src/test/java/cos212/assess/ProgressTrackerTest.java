package cos212.assess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cos212.assess.ProgressTracker.Status;
import org.junit.jupiter.api.Test;

class ProgressTrackerTest {

    @Test
    void objectiveWithNoAnswersIsUntried() {
        ProgressTracker tracker = new ProgressTracker();
        assertEquals(Status.UNTRIED, tracker.status("define-bst"));
        assertEquals(0, tracker.progressPercent("define-bst"));
    }

    @Test
    void oneAnswerStartsTheObjective() {
        ProgressTracker tracker = new ProgressTracker();
        tracker.record("define-bst", true, 0);
        assertEquals(Status.STARTED, tracker.status("define-bst"));
        assertEquals(20, tracker.progressPercent("define-bst"));
    }

    @Test
    void fiveAnswersMakeTheObjectivePractised() {
        ProgressTracker tracker = new ProgressTracker();
        for (int i = 0; i < 5; i++) {
            tracker.record("trace-search", false, 0);
        }
        assertEquals(Status.PRACTISED, tracker.status("trace-search"));
    }

    @Test
    void fiveCorrectFirstAnswersWithNoHintsMakeItSecure() {
        ProgressTracker tracker = new ProgressTracker();
        for (int i = 0; i < 5; i++) {
            tracker.record("define-bst", true, 0);
        }
        assertEquals(Status.SECURE, tracker.status("define-bst"));
        assertEquals(100, tracker.progressPercent("define-bst"));
    }

    @Test
    void fourOfLastFiveCorrectWithNoHintsIsSecure() {
        ProgressTracker tracker = new ProgressTracker();
        tracker.record("ordering-rule", false, 0);
        for (int i = 0; i < 4; i++) {
            tracker.record("ordering-rule", true, 0);
        }
        assertEquals(Status.SECURE, tracker.status("ordering-rule"));
    }

    @Test
    void aHintInTheLastFiveStopsTheObjectiveBeingSecure() {
        ProgressTracker tracker = new ProgressTracker();
        for (int i = 0; i < 4; i++) {
            tracker.record("trace-search", true, 0);
        }
        tracker.record("trace-search", true, 1);
        assertEquals(Status.PRACTISED, tracker.status("trace-search"));
        assertEquals(1, tracker.hints("trace-search"));
    }

    @Test
    void threeOfLastFiveCorrectIsNotSecure() {
        ProgressTracker tracker = new ProgressTracker();
        tracker.record("trace-insert", false, 0);
        tracker.record("trace-insert", false, 0);
        for (int i = 0; i < 3; i++) {
            tracker.record("trace-insert", true, 0);
        }
        assertEquals(Status.PRACTISED, tracker.status("trace-insert"));
    }

    @Test
    void countsAreKeptPerObjective() {
        ProgressTracker tracker = new ProgressTracker();
        tracker.record("shape-cost", true, 0);
        tracker.record("shape-cost", false, 2);
        assertEquals(2, tracker.attempts("shape-cost"));
        assertEquals(1, tracker.correctFirst("shape-cost"));
        assertEquals(2, tracker.hints("shape-cost"));
        assertEquals(0, tracker.attempts("define-bst"));
    }
}
