# ui/

**Interface layer.** The JavaFX screens: the lesson list, the diagnostic gate, the
tree activity and the progress screen.

## What belongs here

- The tree canvas, the trace panel and the control bar.
- Keyboard handling and the text-based styling that keeps the app usable without colour.
- Calls into `cos212.trace` and `cos212.assess` only. The interface never calls
  `cos212.bst` directly.

## Status

Increment I2 is built: the tree canvas, the trace panel and the step controls.
`SmartieSearchApp` is the entry point. Run it with `make run` or `mvn javafx:run`.
The in-order button and the progress screen are added in increments I3 and I4.

The window is landscape by design: the canvas and trace panel sit side by side, with
a default size of 1280 by 800 and a minimum of 1024 by 680.
