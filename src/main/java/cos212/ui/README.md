# ui/

**Interface layer.** The JavaFX screens: the lesson list, the diagnostic gate, the
tree activity and the progress screen.

## What belongs here

- The tree canvas, the trace panel and the control bar.
- Keyboard handling and the text-based styling that keeps the app usable without colour.
- Calls into `cos212.trace` and `cos212.assess` only. The interface never calls
  `cos212.bst` directly.

## Status

All four screens are built from the team's wireframes: Lessons, Diagnostic check,
Tree activity and Progress. `SmartieSearchApp` is the entry point. Run it with
`make run` or `mvn javafx:run`.

- `TreeActivityScreen`: Build, Search, Traverse (In-order), the tree, the trace and the
  predict panel with Left, Right and three hints. Predictions and hints are recorded.
- `DiagnosticScreen`: five questions in `DiagnosticBank`. Four correct passes the gate and
  unlocks the tree activity; a failed result names the lessons to revisit.
- `LessonsScreen`: the diagnostic banner and the five lessons (L0 to L4), open from the start; only the
  tree activity waits for the gate. The Open lesson buttons open each lesson; L0 is placeholder scaffolding until its content is written.
- `ProgressScreen`: one row per objective, with the mastery status from `ProgressTracker`.

Styles are in `src/main/resources/cos212/ui/app.css`. The colours and sizes follow the
wireframes. Progress is held in memory; saving it to a local file is still to do.

The window is landscape by design: the canvas and trace panel sit side by side, with
a default size of 1280 by 800 and a minimum of 1024 by 680.
