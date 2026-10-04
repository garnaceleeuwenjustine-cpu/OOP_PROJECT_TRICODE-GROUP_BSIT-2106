# Echoes of Luma

A 2D beast-battle game demo written in Java Swing. The project uses pixel-art sprites, capsule selection, type-based moves, and animated battle effects.

## Folder layout

```text
OOP_PROJECT_TRICODE-GROUP_BSIT-2106/
├── src/
│   ├── Main.java
│   └── echoesofluma/
│       ├── BattleDemo.java
│       ├── BeastAnimator.java
│       └── BeastDemo.java
├── assets/
│   └── beasts/ (PNG sprites, sprite sheets, and editable SVGs)
├── docs/
│   └── Project-Proposal.docx
├── screenshots/
├── tools/
│   └── BeastSvgExporter.java
├── .vscode/
├── .gitignore
├── build.bat
├── run.bat
└── README.md
```

## Run the game

- **Windows:** Double-click `run.bat` or run it from a terminal. It builds the Java sources into `bin/` and starts the game.
- **VS Code:** Open this repository folder and press **F5**. If prompted, install the Java extension pack. The build task calls `build.bat` before launch.
- **Command line:** From the repository root, run `run.bat`.

Use Java 17 or later. Set `JAVA_HOME` to a JDK installation or make `javac` and `java` available on `PATH`. The scripts also recognize the JDK 27 installation under `Program Files\Java`. Generated output in `bin/` and all `.class` files are ignored by Git.

## Project files

- `src/` contains the Java source code. `Main.java` starts the app through the packaged game in `echoesofluma/`.
- `assets/` contains the beast sprites, sprite sheets, and editable SVGs.
- `docs/` contains the supplied project proposal.
- `screenshots/` is for screenshots of the running game and project.
- `tools/` contains the SVG exporter utility.

