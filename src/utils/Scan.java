package utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Scan {

    //private static String BasePackage;

    //public static HashMap<String, String> getClassesWithAnnotations() throws Exception {
    //    HashMap<String, String> result = new HashMap<>();
    //    String packageName = BasePackage; 
    //    List<Class<?>> classes = getClasses(packageName);

    //    for (Class<?> clazz : classes) {
    //        if (clazz.isAnnotationPresent(Controller.class)) {
    //            Controller annotation = clazz.getAnnotation(Controller.class);
    //            result.put(clazz.getName(), annotation.value());
    //            System.out.println("Classe: " + clazz.getName() + " | value = " + annotation.value());
    //        }
    //    }
    //    return result;
    //}

    public static List<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException {
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) return new ArrayList<>();

        File directory = new File(resource.getFile());
        return findClasses(directory, packageName);
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) return classes;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().replaceAll("\\.class$", "");
                classes.add(Class.forName(className)); 
            }
        }
        return classes;
    }
}
