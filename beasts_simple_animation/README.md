# Echoes of Luma — Java Beast Arena

A playable Java Swing battle demo with 27 pixel-art beasts, capsule animations, named type-based moves, and a turn-based command menu. The battle scene uses a compact low-resolution forest arena, pixel-scaled sprites, corner status panels, and an in-scene command box.

## Run in VS Code

1. Extract the project ZIP.
2. In VS Code, select **File → Open Folder...** and choose `Echoes-of-Luma-Java-Battle-Demo`.
3. Install a JDK and the **Extension Pack for Java** if VS Code prompts you.
4. Press **F5**, then choose **Run Echoes of Luma Battle Demo**.

You can also double-click `run.bat` on Windows if both `java` and `javac` are on your PATH.

## Battle flow

1. Choose a capsule. It contains the beast shown in the selection preview.
2. Choose an enemy and select **Wild encounter · no trainer** or **Trainer battle**.
3. Select **Open capsule** to see your beast emerge in particles.
4. Click **Fight**, **Bag**, **Run**, or **Capture** in the lower-left command box. Capture is enabled only for wild beasts without trainers; trainer battles show it as locked.
5. **Fight** opens the five named move slots in the same pixel menu. Each beast has three damaging moves, one self-buff, and one enemy debuff. Their move types are limited to the beast's assigned type or types.

The bag starts with two potions. Each restores 32 HP and uses your turn. Run recalls your beast to its capsule. When a beast faints, it dissolves into particles and returns to its capsule; you can then choose another encounter.

## Attack visuals

Moves use distinct, type-colored pixel projectiles and impact shapes—for example, fire arcs, water drops, rock shards, leaves, psychic rings, electric bolts, steel slashes, and ghost wisps. Hits add a brief flash, a squish, a short lunge, and light screen shake. Buffs rise in bright runes; debuffs wrap the target in a marked ring. Beasts remain still between actions.

The source file is `src/echoesofluma/BattleDemo.java`; the 27 sprite files are in `assets/beasts/`.
