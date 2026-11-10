# assess/

**Assessment layer.** Decides what the student is asked, scores the answer, and
routes a wrong answer to the lesson for its misconception.

## What belongs here

- Diagnostic items and the gate rule (pass at four of five).
- The router, which maps a misconception identifier (M1 to M6) to a lesson.
- The `Feedback` object, and the `ProgressTracker` that records attempts, correct
  first answers and hints used.
- No user-interface types, and no direct file writes outside the progress store.

## Tests

Tests go in `src/test/java/cos212/assess/`. Each wrong answer must route to its
expected lesson. See the Assessment and Scaffolding section of the SE document.
