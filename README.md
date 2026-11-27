# CodeNote — Java & Python Editor

VS Code-inspired lightweight desktop editor built with Java 21 and Swing. Korean UI.

## Features
- Dark/light theme toggle
- Multi-tab editing
- Line numbers and syntax highlighting for Java and Python
- Workspace file explorer (open a folder, double-click files)
- Automatic save for named files (debounced)
- Find in current document (Ctrl+F)
- Integrated system shell terminal
- Run current Java or Python file with F6; file extension selects the runtime
- Java files are compiled with javac before execution

## Requirements
- JDK 21 or newer to run/build CodeNote
- Python 3 to run .py files (Windows: Python Launcher `py -3`; macOS/Linux: `python3`)

## Build
- Windows: run `build.bat`
- macOS/Linux: run `./build.sh`

The runnable jar is created at `dist/CodeNote.jar`.

## Run
```bash
java -jar dist/CodeNote.jar
```

## Usage
1. Open or create a `.java` or `.py` file.
2. Edit and save it.
3. Press **F6** or choose **실행 → 현재 파일 실행 (Java / Python)**.
4. Program output and errors appear in the integrated terminal.

## Notes
- The terminal executes commands using the host operating system shell. Only run commands you trust.
- Java run currently supports a single source file with a main method and no external dependencies / multi-module build.
- Python execution requires a separately installed Python 3 interpreter.
- Automatic save applies to files that already have a path. Untitled documents must be saved manually.
- Syntax highlighting is lightweight keyword-based highlighting, not a full language server.
