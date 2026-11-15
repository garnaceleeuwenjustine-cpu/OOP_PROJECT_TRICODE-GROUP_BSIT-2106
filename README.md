# Echoes of Luma

Echoes of Luma is a Java Swing beast-battling RPG. The complete game includes the title screen, story and starter selection, world map, ethical-choice battles, catchable beasts, and the in-game bag, shop, Beast Dex, and storage.

## Run the game

- **Windows:** Double-click `run.bat`, or run it from PowerShell in this folder.
- **VS Code:** Open this repository folder and press **F5** with **Run Echoes of Luma** selected. The launch task builds the project first.
- **Java:** Use JDK 25 or newer. The build script compiles with `--release 25`.

The main project launches through `src/Main.java`. The standalone **Preview Member 3 Pack (Standalone)** launch option is only for previewing the bag/shop panel; use **Run Echoes of Luma** for the full game.

## Controls

- Press **M** or **Escape** during play to open the game menu.
- Open **Bag & Items** from the game menu, or choose **Bag** during battle.
- Use **1–3** to answer an ethics question and **1–8** to select a capsule.
- Use **Enter**, **Space**, or **Z** to advance story and battle dialogue.

## Project structure

- `src/` — Java game source.
- `assets/` — title screen, battle backgrounds, trainers, and beast artwork.
- `build.bat` — compiles source into `out/`.
- `run.bat` — builds and starts the complete game.
- `.vscode/` — VS Code build task and launch configurations.
