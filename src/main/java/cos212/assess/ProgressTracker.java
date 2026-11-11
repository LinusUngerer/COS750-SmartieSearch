package cos212.assess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Records each answer for an objective and works out the objective's mastery status, using
 * the standard in the progress tracking section of the ID document. An objective is
 * practised after {@value #PRACTISED_AFTER} answers, and secure when at least
 * {@value #SECURE_CORRECT} of the last {@value #SECURE_WINDOW} first answers were correct
 * with no hints.
 */
public final class ProgressTracker {

    public static final int PRACTISED_AFTER = 5;
    public static final int SECURE_WINDOW = 5;
    public static final int SECURE_CORRECT = 4;

    /** Where an objective stands. */
    public enum Status {
        UNTRIED,
        STARTED,
        PRACTISED,
        SECURE
    }

    private record Answer(boolean firstCorrect, int hints) {
    }

    private static final class Record {
        int attempts;
        int correctFirst;
        int hints;
        final Deque<Answer> recent = new ArrayDeque<>();
    }

    private final Map<String, Record> records = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    /**
     * Registers a callback that runs after every recorded answer, so the answers can be saved.
     *
     * @param listener the callback to run
     */
    public void addChangeListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Records one answer.
     *
     * @param objective the objective the answer is for
     * @param firstCorrect whether the answer was correct on the first attempt
     * @param hintsUsed the number of hints the student used before answering
     */
    public void record(String objective, boolean firstCorrect, int hintsUsed) {
        Objects.requireNonNull(objective, "objective");
        if (hintsUsed < 0) {
            throw new IllegalArgumentException("hintsUsed must not be negative");
        }
        Record r = records.computeIfAbsent(objective, key -> new Record());
        r.attempts++;
        if (firstCorrect) {
            r.correctFirst++;
        }
        r.hints += hintsUsed;
        r.recent.addLast(new Answer(firstCorrect, hintsUsed));
        while (r.recent.size() > SECURE_WINDOW) {
            r.recent.removeFirst();
        }
        listeners.forEach(Runnable::run);
    }

    /**
     * @return the recorded answers as plain maps and lists, ready to be written as JSON
     */
    Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Record> entry : records.entrySet()) {
            Record r = entry.getValue();
            List<Object> recent = new ArrayList<>();
            for (Answer answer : r.recent) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("firstCorrect", answer.firstCorrect());
                item.put("hints", answer.hints());
                recent.add(item);
            }
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("attempts", r.attempts);
            record.put("correctFirst", r.correctFirst);
            record.put("hints", r.hints);
            record.put("recent", recent);
            out.put(entry.getKey(), record);
        }
        return out;
    }

    /**
     * Replaces every recorded answer with the saved ones. Nothing changes if the data is
     * invalid. An empty map clears the tracker.
     *
     * @param data a map in the form {@link #snapshot()} produces
     * @throws IllegalArgumentException if the data is malformed
     */
    void restore(Map<String, Object> data) {
        Map<String, Record> loaded = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Map<String, Object> saved = Json.object(entry.getValue(), "objective " + entry.getKey());
            Record r = new Record();
            r.attempts = Json.count(saved, "attempts");
            r.correctFirst = Json.count(saved, "correctFirst");
            r.hints = Json.count(saved, "hints");
            if (r.correctFirst > r.attempts) {
                throw new IllegalArgumentException("correctFirst exceeds attempts for " + entry.getKey());
            }
            for (Object item : Json.list(saved, "recent")) {
                Map<String, Object> answer = Json.object(item, "answer");
                r.recent.addLast(new Answer(Json.flag(answer, "firstCorrect"), Json.count(answer, "hints")));
            }
            while (r.recent.size() > SECURE_WINDOW) {
                r.recent.removeFirst();
            }
            loaded.put(entry.getKey(), r);
        }
        records.clear();
        records.putAll(loaded);
    }

    /** @return the mastery status of the objective; untried if it has no answers */
    public Status status(String objective) {
        Record r = records.get(objective);
        if (r == null || r.attempts == 0) {
            return Status.UNTRIED;
        }
        if (r.attempts < PRACTISED_AFTER) {
            return Status.STARTED;
        }
        return isSecure(r) ? Status.SECURE : Status.PRACTISED;
    }

    private static boolean isSecure(Record r) {
        if (r.recent.size() < SECURE_WINDOW) {
            return false;
        }
        int correct = 0;
        int hints = 0;
        for (Answer answer : r.recent) {
            if (answer.firstCorrect()) {
                correct++;
            }
            hints += answer.hints();
        }
        return correct >= SECURE_CORRECT && hints == 0;
    }

    /** @return the number of answers recorded for the objective */
    public int attempts(String objective) {
        return count(objective, r -> r.attempts);
    }

    /** @return the number of answers that were correct on the first attempt */
    public int correctFirst(String objective) {
        return count(objective, r -> r.correctFirst);
    }

    /** @return the number of hints used for the objective */
    public int hints(String objective) {
        return count(objective, r -> r.hints);
    }

    /**
     * @return the progress bar value for the objective, from 0 to 100. Secure objectives
     *         are shown as complete.
     */
    public int progressPercent(String objective) {
        if (status(objective) == Status.SECURE) {
            return 100;
        }
        return Math.min(100, attempts(objective) * 100 / PRACTISED_AFTER);
    }

    private int count(String objective, ToIntFunction<Record> field) {
        Record r = records.get(objective);
        return r == null ? 0 : field.applyAsInt(r);
    }
}
