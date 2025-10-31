package utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import annotations.Controller;
import annotations.Url;

public class Scan {

    public static class MethodInfo {
        public Class<?> clazz;
        public Method method;

        public MethodInfo(Class<?> clazz, Method method) {
            this.clazz = clazz;
            this.method = method;
        }
    }

    public static HashMap<String, MethodInfo> getClassesWithAnnotations(String packageName) throws Exception {
        HashMap<String, MethodInfo> result = new HashMap<>();
        List<Class<?>> classes = getClasses(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Url.class)) {
                        String url = method.getAnnotation(Url.class).value();
                        result.put(url, new MethodInfo(clazz, method));
                    }
                }
            }
        }
        return result;
    }

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
