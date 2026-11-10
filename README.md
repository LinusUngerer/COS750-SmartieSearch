# COS750 Smartie Search

A prototype pre-lecture tutor for binary search trees (BSTs) in Java, built for
COS212 students as part of COS750 Educational Software Development. The app
teaches the BST ordering rule, search, insertion and in-order traversal, and it
gives students a diagnostic gate, predicted-step questions and graduated hints.

Group: Brown Smarties.

## Stack

- **Language and runtime:** Java 17, with the JDK as the only installation
  requirement
- **Build:** Maven
- **Tests:** JUnit 5
- **Interface:** JavaFX (added in increment I2)
- **Documentation:** LaTeX, edited on Overleaf and kept in `docs/`

## Layout

```
COS750-SmartieSearch/
├── src/main/java/cos212/
│   ├── bst/              domain layer: BSTNode, BinarySearchTree (no UI imports)
│   ├── trace/            trace layer: records each search and insertion step
│   ├── assess/           assessment layer: diagnostic, routing, feedback, progress
│   └── ui/               JavaFX interface
├── src/main/resources/
│   └── lessons/          lesson text, diagnostic items and feedback messages
├── src/test/java/cos212/ JUnit 5 tests, mirroring the main packages
├── docs/                 SE and ID documents (LaTeX sources)
├── evaluation/           usability study and questionnaire materials
├── .github/workflows/    CI
├── Makefile              same targets as the Maven build, for macOS, Linux and WSL
└── pom.xml
```

Each layer folder has a README that says what belongs there and where its tests go.
Read it before adding files.

## First-time setup

1. Install JDK 17 or later, and Maven 3.9 or later.
2. Clone the repository.

   ```bash
   git clone https://github.com/<owner>/COS750-SmartieSearch.git
   cd COS750-SmartieSearch
   ```

3. Run the build and the tests.

   ```bash
   mvn -B verify
   ```

## Day-to-day

```bash
make test     # run the JUnit tests
make build    # package the runnable JAR into target/
make clean    # remove build output
```

The packaged JAR is started with `java -jar target/cos212-app-<version>.jar`.
The JAR is produced once the packaging step is added in increment I5.

## Documentation

The two documents are written in LaTeX on Overleaf. The Overleaf project is the
master copy, and `docs/` holds a snapshot of its sources. See [docs/README.md](docs/README.md).

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
