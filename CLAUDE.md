# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Compile and package
./gradlew run            # Launch the JavaFX application
./gradlew test           # Run JUnit 5 tests
./gradlew clean build    # Clean rebuild
```

Requires **JDK 21**. The project uses the Gradle wrapper (`gradlew`/`gradlew.bat`).

## Architecture

This is an **educational concurrency simulator** for an Operating Systems course, built with JavaFX 21. It visualizes two classic synchronization problems with an HD-2D pixel art aesthetic.

### Module Structure

All code lives under `gt.edu.url.so.proyectosistemasoperativos` (Java 9+ module system via `module-info.java`).

- **`Main.java`** — Entry point. Renders the pixel art menu and handles scene navigation between the two problems.
- **`common/`** — Shared UI components:
  - `PixelGameCanvas` — Core pixel art rendering engine (custom Canvas, ~1200 lines). Draws all backgrounds, characters, decorations.
  - `LogPanel` — Real-time timestamped event log (used by both views).
  - `AnimationUtils` — Reusable animation helpers (pulse, glow, pop-in, conveyor belt).
- **`producerconsumer/`** — Producer-Consumer problem simulation.
- **`philosophers/`** — Dining Philosophers problem simulation.

### Concurrency Design

Both problems follow the same pattern: **Controller** (synchronization logic) + **Thread subclasses** (actors) + **AnimationView** (JavaFX UI).

**Producer-Consumer** uses three counting semaphores (`empty`, `full`, `mutex`) on a `SharedBuffer` (circular buffer, capacity 12). One producer reads from `data/numeros.txt` (40 numbers). Three consumers extract by type: even (C1), odd (C2), prime (C3). Prime classification takes priority (e.g., 2 is prime, not even).

**Dining Philosophers** uses Dijkstra's monitor solution: a shared `estado[]` array protected by a `mutex` semaphore, plus one semaphore per philosopher. `test(i)` atomically checks both neighbors before granting forks. Configurable 2-10 philosophers.

All thread-to-UI communication goes through `Platform.runLater()`. State is read via immutable snapshots taken inside the mutex.

### UI Rendering

The UI is entirely **Canvas-based** (no FXML, no scene graph nodes for game elements). `PixelGameCanvas` handles all pixel art rendering with a scale factor. CSS files in `resources/` style the JavaFX controls (buttons, panels, scrollbars) with a dark retro theme using the "Press Start 2P" font.

## Key Conventions

- **Language**: Code, comments, UI text, and log messages are in **Spanish**.
- **Thread safety**: Never modify JavaFX nodes from simulation threads — always use `Platform.runLater()`.
- **Shutdown**: Threads use `volatile boolean running` flags and `Thread.interrupt()` for clean termination.
- **No FXML**: All UI is built programmatically in Java.
- **Synchronization primitives**: Uses `java.util.concurrent.Semaphore` intentionally (not `BlockingQueue` or `synchronized`) to demonstrate manual synchronization for the course.

## Dependencies

- JavaFX 21.0.1 (`javafx.controls` module)
- Ikonli 12.3.1 (Material Design 2 icons for JavaFX)
- JUnit 5.10.0 (testing)
