package cos212.assess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cos212.assess.ProgressTracker.Status;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressStoreTest {

    @TempDir
    Path dir;

    private static void pass(Diagnostic diagnostic) {
        for (int i = 0; i < Diagnostic.ITEM_COUNT; i++) {
            diagnostic.recordAnswer(true, "L1");
        }
    }

    @Test
    void missingFileMeansNewStudent() {
        ProgressStore store = new ProgressStore(dir.resolve("none.json"));
        ProgressTracker tracker = new ProgressTracker();
        Diagnostic diagnostic = new Diagnostic();
        assertFalse(store.load(tracker, diagnostic));
        assertFalse(diagnostic.unlocked());
        assertEquals(Status.UNTRIED, tracker.status("define-bst"));
    }

    @Test
    void passedGateAndAnswersSurviveARestart() {
        ProgressStore store = new ProgressStore(dir.resolve("sub/progress.json"));
        ProgressTracker tracker = new ProgressTracker();
        Diagnostic diagnostic = new Diagnostic();
        pass(diagnostic);
        for (int i = 0; i < 5; i++) {
            tracker.record("trace-search", true, 0);
        }
        tracker.record("trace-insert", false, 2);
        assertTrue(store.save(tracker, diagnostic));

        ProgressTracker reloaded = new ProgressTracker();
        Diagnostic reloadedDiagnostic = new Diagnostic();
        assertTrue(store.load(reloaded, reloadedDiagnostic));
        assertTrue(reloadedDiagnostic.unlocked());
        assertEquals(Status.SECURE, reloaded.status("trace-search"));
        assertEquals(5, reloaded.attempts("trace-search"));
        assertEquals(5, reloaded.correctFirst("trace-search"));
        assertEquals(1, reloaded.attempts("trace-insert"));
        assertEquals(2, reloaded.hints("trace-insert"));
    }

    @Test
    void retakingTheDiagnosticDoesNotRelockTheGate() {
        Diagnostic diagnostic = new Diagnostic();
        pass(diagnostic);
        diagnostic.reset();
        assertFalse(diagnostic.passed());
        assertTrue(diagnostic.unlocked());
    }

    @Test
    void failedGateIsNotSavedAsPassed() {
        ProgressStore store = new ProgressStore(dir.resolve("progress.json"));
        Diagnostic diagnostic = new Diagnostic();
        for (int i = 0; i < Diagnostic.ITEM_COUNT; i++) {
            diagnostic.recordAnswer(false, "L1");
        }
        store.save(new ProgressTracker(), diagnostic);
        Diagnostic reloaded = new Diagnostic();
        store.load(new ProgressTracker(), reloaded);
        assertFalse(reloaded.unlocked());
    }

    @Test
    void diagnosticAttemptInProgressSurvivesARestart() {
        ProgressStore store = new ProgressStore(dir.resolve("progress.json"));
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.recordAnswer(true, "L1");
        diagnostic.recordAnswer(false, "L2");
        assertTrue(store.save(new ProgressTracker(), diagnostic));

        Diagnostic reloaded = new Diagnostic();
        assertTrue(store.load(new ProgressTracker(), reloaded));
        assertEquals(2, reloaded.answered());
        assertEquals(1, reloaded.correct());
        assertEquals(java.util.Set.of("L2"), reloaded.missedLessons());
        assertFalse(reloaded.isComplete());

        for (int i = 0; i < 3; i++) {
            reloaded.recordAnswer(true, "L1");
        }
        assertTrue(reloaded.passed());
    }

    @Test
    void lessonPositionsSurviveARestart() {
        ProgressStore store = new ProgressStore(dir.resolve("progress.json"));
        LessonProgress lessons = new LessonProgress();
        lessons.setPosition("L1", 4);
        lessons.setPosition("L2", 10);
        lessons.complete("L2");
        assertTrue(store.save(new ProgressTracker(), new Diagnostic(), lessons));

        LessonProgress reloaded = new LessonProgress();
        assertTrue(store.load(new ProgressTracker(), new Diagnostic(), reloaded));
        assertEquals(4, reloaded.position("L1"));
        assertEquals(10, reloaded.position("L2"));
        assertEquals(0, reloaded.position("L3"));
        assertTrue(reloaded.isComplete("L2"));
        assertFalse(reloaded.isComplete("L1"));
    }

    @Test
    void diagnosticAttemptCountsOutOfRangeAreRejected() throws IOException {
        Path file = dir.resolve("progress.json");
        Files.writeString(file, "{\"version\":1,\"diagnostic\":{\"answered\":2,\"correct\":3}}");
        Diagnostic diagnostic = new Diagnostic();
        assertFalse(new ProgressStore(file).load(new ProgressTracker(), diagnostic));
        assertEquals(0, diagnostic.answered());
    }

    @Test
    void damagedFileIsMovedAsideAndTheStudentStartsFresh() throws IOException {
        Path file = dir.resolve("progress.json");
        Files.writeString(file, "{ this is not json");
        ProgressTracker tracker = new ProgressTracker();
        Diagnostic diagnostic = new Diagnostic();
        assertFalse(new ProgressStore(file).load(tracker, diagnostic));
        assertFalse(diagnostic.unlocked());
        assertTrue(Files.exists(dir.resolve("progress.json.bak")));
    }

    @Test
    void invalidValuesAreRejectedWithoutChangingState() throws IOException {
        Path file = dir.resolve("progress.json");
        Files.writeString(file, "{\"version\":1,\"diagnostic\":{\"passedBefore\":true},"
                + "\"objectives\":{\"define-bst\":{\"attempts\":-3}}}");
        ProgressTracker tracker = new ProgressTracker();
        Diagnostic diagnostic = new Diagnostic();
        assertFalse(new ProgressStore(file).load(tracker, diagnostic));
        assertFalse(diagnostic.unlocked());
        assertEquals(0, tracker.attempts("define-bst"));
    }

    @Test
    void trackerNotifiesListenersOnEveryAnswer() {
        ProgressTracker tracker = new ProgressTracker();
        int[] calls = {0};
        tracker.addChangeListener(() -> calls[0]++);
        tracker.record("define-bst", true, 0);
        tracker.record("define-bst", false, 1);
        assertEquals(2, calls[0]);
    }

    @Test
    void jsonRoundTripsEscapesAndNesting() {
        Object parsed = Json.parse(Json.write(java.util.Map.of("a",
                java.util.List.of("quo\"te\\n", 7L, true))));
        assertEquals(java.util.Map.of("a", java.util.List.of("quo\"te\\n", 7L, true)), parsed);
    }
}
