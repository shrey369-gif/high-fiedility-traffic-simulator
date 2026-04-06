@echo off
echo Compiling Traffic Simulation Engine...
if not exist "build" mkdir build
javac -d build src\main\java\com\traffic\model\*.java src\main\java\com\traffic\engine\*.java src\main\java\com\traffic\algorithm\*.java src\main\java\com\traffic\util\*.java src\main\java\com\traffic\server\*.java src\main\java\com\traffic\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)
echo Compilation successful!
echo Run with: java -cp build com.traffic.Main
