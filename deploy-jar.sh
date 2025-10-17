cd src

javac -d ../bin -cp "../lib/*" *.java

cd ../bin

jar cvf frameworkServlet.jar .
