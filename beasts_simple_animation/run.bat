@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\echoesofluma\BattleDemo.java
if errorlevel 1 exit /b 1
java -cp bin echoesofluma.BattleDemo
