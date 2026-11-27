@echo off
setlocal
cd /d "%~dp0"
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\classes
mkdir dist
for /r src\main\java %%f in (*.java) do echo %%f>>build\sources.txt
javac --release 21 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
(echo Main-Class: com.codenote.CodeNoteApp)>build\MANIFEST.MF
jar cfm dist\CodeNote.jar build\MANIFEST.MF -C build\classes .
echo Built: dist\CodeNote.jar
