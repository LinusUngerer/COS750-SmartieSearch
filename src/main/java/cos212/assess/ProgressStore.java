package cos212.assess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Saves the student's progress to a JSON file and loads it again, so closing the program
 * does not lose the objective records or the unlocked state of the diagnostic gate.
 *
 * <p>The file is {@code .smartiesearch/progress.json} in the user's home directory. The
 * system property {@value #PATH_PROPERTY} names a different file. Loading and saving never
 * throw: a missing file means a new student, a damaged file is moved aside to
 * {@code progress.json.bak} and the student starts fresh, and a failed save is reported on
 * standard error so the activity carries on.
 */
public final class ProgressStore {

    /** The system property that overrides the default location of the progress file. */
    public static final String PATH_PROPERTY = "smartiesearch.progress";

    private static final int VERSION = 1;

    private final Path file;

    /** Uses the default location, or the one named by {@value #PATH_PROPERTY}. */
    public ProgressStore() {
        this(defaultPath());
    }

    /**
     * @param file the file to read and write
     */
    public ProgressStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    private static Path defaultPath() {
        String override = System.getProperty(PATH_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".smartiesearch", "progress.json");
    }

    /** @return the file this store reads and writes */
    public Path file() {
        return file;
    }

    /**
     * Fills the tracker and diagnostic from the file, if there is one.
     *
     * @return {@code true} if saved progress was loaded
     */
    public boolean load(ProgressTracker tracker, Diagnostic diagnostic) {
        return load(tracker, diagnostic, new LessonProgress());
    }

    /**
     * Fills the tracker, diagnostic and lesson positions from the file, if there is one.
     *
     * @return {@code true} if saved progress was loaded
     */
    public boolean load(ProgressTracker tracker, Diagnostic diagnostic, LessonProgress lessons) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            Map<String, Object> root = Json.object(
                    Json.parse(Files.readString(file, StandardCharsets.UTF_8)), "progress file");
            if (Json.count(root, "version") != VERSION) {
                throw new IllegalArgumentException("unsupported progress file version");
            }
            Map<String, Object> savedDiagnostic = root.containsKey("diagnostic")
                    ? Json.object(root.get("diagnostic"), "diagnostic") : Map.of();
            Map<String, Object> savedObjectives = root.containsKey("objectives")
                    ? Json.object(root.get("objectives"), "objectives") : Map.of();
            Map<String, Object> savedLessons = root.containsKey("lessons")
                    ? Json.object(root.get("lessons"), "lessons") : Map.of();
            tracker.restore(savedObjectives);
            diagnostic.restore(savedDiagnostic);
            lessons.restore(savedLessons);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not read " + file + " (" + e.getMessage() + "). Starting fresh.");
            tracker.restore(Map.of());
            diagnostic.restore(Map.of());
            lessons.restore(Map.of());
            moveAside();
            return false;
        }
    }

    /**
     * Writes the tracker and diagnostic to the file, replacing the previous contents.
     *
     * @return {@code true} if the file was written
     */
    public boolean save(ProgressTracker tracker, Diagnostic diagnostic) {
        return save(tracker, diagnostic, new LessonProgress());
    }

    /**
     * Writes the tracker, diagnostic and lesson positions to the file, replacing the previous
     * contents.
     *
     * @return {@code true} if the file was written
     */
    public boolean save(ProgressTracker tracker, Diagnostic diagnostic, LessonProgress lessons) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", VERSION);
        root.put("diagnostic", diagnostic.snapshot());
        root.put("objectives", tracker.snapshot());
        root.put("lessons", lessons.snapshot());
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, "progress", ".tmp");
            try {
                Files.writeString(temp, Json.write(root), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Could not save progress to " + file + ": " + e.getMessage());
            return false;
        }
    }

    private void moveAside() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The next save replaces the damaged file anyway.
        }
    }
}
