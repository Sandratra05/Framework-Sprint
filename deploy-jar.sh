#cd src

#javac -d ../bin -cp "../lib/*" *.java

#cd ../bin

#jar cvf frameworkServlet.jar .

#rm /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/frameworkServlet.jar

#cp frameworkServlet.jar /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/frameworkServlet.jar

cd /home/zark/Bureau/ITU/Annee-3/Framework/Framework && 
javac -parameters -d bin -cp "lib/*" src/*.java src/utils/*.java src/annotations/*.java src/view/*.java
cd bin && 
jar cvf frameworkServlet.jar . && 
cp frameworkServlet.jar /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/