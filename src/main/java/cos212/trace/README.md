# trace/

**Trace layer.** Wraps search and insertion, and records every comparison as a
step. The tree itself is not changed by a trace.

## What belongs here

- The trace operations, each returning a list of steps.
- A `TraceStep` for each comparison: the node, the target, the result and the
  direction taken.
- Imports from `cos212.bst` only.

## Tests

Tests go in `src/test/java/cos212/trace/`. Each step must match the comparison the
tree actually made. See the Testing and Verification section of the SE document.
