@echo off

REM Script to build the Ririnina framework JAR
REM Run this script from the project root directory

echo Compiling Java sources...
javac -parameters -d bin -cp "lib/*" src/*.java src\mg\ririnina\utils\*.java src\mg\ririnina\annotations\*.java src\mg\ririnina\view\*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo Creating JAR file...
cd bin
jar cvf ririnina.jar .

echo Build completed successfully. JAR file created: bin\ririnina.jar