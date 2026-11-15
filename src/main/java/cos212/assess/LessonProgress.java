package cos212.assess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Where the student is in each lesson: the next page to show, and whether they have reached
 * the end of the lesson at least once. Saved with the rest of the progress, so opening a
 * lesson again carries on from the page the student stopped at.
 */
public final class LessonProgress {

    private final Map<String, Integer> positions = new LinkedHashMap<>();
    private final Set<String> completed = new LinkedHashSet<>();

    /** @return the index of the next page to show in the lesson; 0 if it has not been opened */
    public int position(String lesson) {
        return positions.getOrDefault(lesson, 0);
    }

    /**
     * Records the next page to show in the lesson.
     *
     * @throws IllegalArgumentException if the page is negative
     */
    public void setPosition(String lesson, int page) {
        Objects.requireNonNull(lesson, "lesson");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        positions.put(lesson, page);
    }

    /** Marks the lesson as finished. */
    public void complete(String lesson) {
        completed.add(Objects.requireNonNull(lesson, "lesson"));
    }

    /** @return whether the student has reached the end of the lesson at least once */
    public boolean isComplete(String lesson) {
        return completed.contains(lesson);
    }

    /** @return the saved form: each lesson's position and the list of finished lessons */
    Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("positions", new LinkedHashMap<String, Object>(positions));
        out.put("completed", new ArrayList<Object>(completed));
        return out;
    }

    /**
     * Replaces the state with the saved one. Nothing changes if the data is invalid. An empty
     * map clears every lesson.
     *
     * @throws IllegalArgumentException if the data is malformed
     */
    void restore(Map<String, Object> data) {
        Map<String, Integer> savedPositions = new LinkedHashMap<>();
        if (data.containsKey("positions")) {
            Map<String, Object> saved = Json.object(data.get("positions"), "positions");
            for (String lesson : saved.keySet()) {
                savedPositions.put(lesson, Json.count(saved, lesson));
            }
        }
        Set<String> savedCompleted = new LinkedHashSet<>();
        List<Object> done = Json.list(data, "completed");
        for (Object lesson : done) {
            if (!(lesson instanceof String name)) {
                throw new IllegalArgumentException("completed must hold lesson names");
            }
            savedCompleted.add(name);
        }
        positions.clear();
        positions.putAll(savedPositions);
        completed.clear();
        completed.addAll(savedCompleted);
    }
}
