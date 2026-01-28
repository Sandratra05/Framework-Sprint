
#!/bin/bash

# Script to build the Ririnina framework JAR
# Run this script from the project root directory

echo "Compiling Java sources..."
javac -parameters -d bin -cp "lib/*" src/*.java src/mg/ririnina/utils/*.java src/mg/ririnina/annotations/*.java src/mg/ririnina/view/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Creating JAR file..."
cd bin
jar cvf ririnina.jar .

echo "Build completed successfully. JAR file created: bin/ririnina.jar"
