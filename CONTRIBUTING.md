# Contributing

## Commits

- **Author your own commits.** Every commit should be attributable to the
  student who wrote it. Do not add co-author trailers for tools, editors or
  assistants. This is assessed work, and the history is part of what is assessed.
- Branch off `main` and merge through a pull request that another group member
  reviews. Do not commit straight to `main`.
- Write messages that explain *why*. The diff already shows what changed.

## Before you push

```bash
mvn -B verify    # must pass
```

## Behaviour changes need a failing test first

A change to the behaviour of a BST operation must come with a test that fails
before the change and passes after it. This protects the correctness requirement
NFR2 in the SE document. See the Testing and Verification section of the SE
document for the full test strategy.

## Layer boundaries

The dependencies between packages are one way only:

- `bst` imports nothing from the application.
- `trace` imports `bst` only.
- `assess` imports `trace` and the lesson resources.
- `ui` may import all of the above.

A change that breaks this direction is rejected in review. The architecture is
described in the System Architecture section of the SE document.

## Where things live

- [`README.md`](README.md): stack, layout, setup and day-to-day commands
- [`docs/README.md`](docs/README.md): the two documents and how they are built
- [`evaluation/README.md`](evaluation/README.md): usability study materials
- Each `src/main/java/cos212/*/README.md`: what belongs in that layer
