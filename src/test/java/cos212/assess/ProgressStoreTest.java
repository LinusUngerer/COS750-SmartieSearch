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
