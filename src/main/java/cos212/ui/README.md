# ui/

**Interface layer.** The JavaFX screens: the lesson list, the diagnostic gate, the
tree activity and the progress screen.

## What belongs here

- The tree canvas, the trace panel and the control bar.
- Keyboard handling and the text-based styling that keeps the app usable without colour.
- Calls into `cos212.trace` and `cos212.assess` only. The interface never calls
  `cos212.bst` directly.

## Status

Empty until increment I2. The JavaFX dependency is added to `pom.xml` at the same time.
