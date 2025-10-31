#cd src

#javac -d ../bin -cp "../lib/*" *.java

#cd ../bin

#jar cvf frameworkServlet.jar .

#rm /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/frameworkServlet.jar

#cp frameworkServlet.jar /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/frameworkServlet.jar

cd /home/zark/Bureau/ITU/Annee-3/Framework/Sprint && javac -d bin -cp "lib/*" src/FrontServlet.java src/utils/Scan.java src/annotations/Controller.java src/annotations/Url.java && cd bin && jar cvf frameworkServlet.jar . && cp frameworkServlet.jar /home/zark/Bureau/ITU/Annee-3/Framework/Test/lib/