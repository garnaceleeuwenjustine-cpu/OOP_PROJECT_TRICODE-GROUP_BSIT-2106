@echo off
cd /d "%~dp0"
set "JDK_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_BIN=%JAVA_HOME%\bin"
if not defined JDK_BIN if exist "%ProgramFiles%\Java\jdk-27\bin\javac.exe" set "JDK_BIN=%ProgramFiles%\Java\jdk-27\bin"
if defined JDK_BIN (
  set "JAVAC_EXE=%JDK_BIN%\javac.exe"
  set "JAVA_EXE=%JDK_BIN%\java.exe"
) else (
  set "JAVAC_EXE=javac"
  set "JAVA_EXE=java"
)
if not exist out mkdir out
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -d out src\Main.java src\echoesofluma\BattleDemo.java src\echoesofluma\WorldMapPanel.java src\echoesofluma\StoryIntroPanel.java src\echoesofluma\StarterChoicePanel.java src\echoesofluma\BagPanel.java src\echoesofluma\CreaturePC.java src\echoesofluma\PixelBitmapFont.java src\echoesofluma\BeastIllustrations.java src\echoesofluma\BeastAnimator.java src\echoesofluma\BeastDemo.java
exit /b %ERRORLEVEL%
