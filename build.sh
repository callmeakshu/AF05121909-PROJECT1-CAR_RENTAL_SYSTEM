@echo off
REM ---------------------------------------------------------------------
REM Compile + run on Windows.
REM Drop the JDBC driver jar in .\lib  (e.g. mysql-connector-j-8.4.0.jar)
REM ---------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist out mkdir out

set JARS=
for %%f in (lib\*.jar) do call :addjar %%f
set CP=out;src\main\resources;%JARS%

echo ^>^> Compiling...
dir /s /b src\main\java\*.java > sources.txt
javac -d out -cp "%JARS%" @sources.txt
del sources.txt

echo ^>^> Running...
java -cp "%CP%" com.carrental.Main
goto :eof

:addjar
set JARS=%JARS%%1;
goto :eof
