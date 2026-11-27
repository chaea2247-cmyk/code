# CodeNote — Java Editor

VS Code-inspired lightweight desktop editor built with Java 21 and Swing. Korean UI.

## Features
- Dark/light theme toggle
- Multi-tab editing
- Line numbers and lightweight Java syntax highlighting
- Workspace file explorer (open a folder, double-click files)
- Automatic save for named files (debounced)
- Find in current document (Ctrl+F)
- Integrated system shell terminal
- Compile and run the current .java file (F6)

## Requirements
- JDK 21 or newer

## Build
- Windows: run `build.bat`
- macOS/Linux: run `./build.sh`

The runnable jar is created at `dist/CodeNote.jar`.

## Run
```bash
java -jar dist/CodeNote.jar
```

## Notes
- The terminal executes commands using the host operating system shell. Only run commands you trust.
- Java run currently supports a single source file with a main method and no external dependencies / multi-module build.
- Automatic save applies to files that already have a path. Untitled documents must be saved manually.
- Syntax highlighting is keyword-based, not a full language server.
