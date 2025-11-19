package utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    /**
     * Trouve la première MethodInfo dont la clé (pattern) matche l'url fournie.
     * Supporte des patterns de la forme "/ressources/{id}/sub" où chaque
     * "{...}" correspond à un segment générique (ne contient pas '/').
     * Ne récupère pas encore les valeurs des paramètres, seulement la correspondance.
     */
    public static MethodInfo findMatching(HashMap<String, MethodInfo> map, String path) {
        if (map == null) return null;
        // match exact d'abord
        if (map.containsKey(path)) return map.get(path);

        for (Map.Entry<String, MethodInfo> e : map.entrySet()) {
            String pattern = e.getKey();
            String regex = patternToRegex(pattern);
            try {
                if (Pattern.matches(regex, path)) {
                    return e.getValue();
                }
            } catch (Exception ex) {
                // ignorer pattern invalide
            }
        }
        return null;
    }

    private static String patternToRegex(String pattern) {
        // Construire le regex en échappant les parties littérales et en
        // remplaçant les {param} par un segment capture qui n'inclut pas '/'.
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (idx < pattern.length()) {
            int open = pattern.indexOf('{', idx);
            if (open == -1) {
                // pas d'accolade, ajouter la queue échappée
                sb.append(Pattern.quote(pattern.substring(idx)));
                break;
            }
            // ajouter la partie littérale avant '{'
            if (open > idx) {
                sb.append(Pattern.quote(pattern.substring(idx, open)));
            }
            int close = pattern.indexOf('}', open + 1);
            if (close == -1) {
                // accolade fermante manquante, traiter le reste comme littéral
                sb.append(Pattern.quote(pattern.substring(open)));
                break;
            }
            // remplacer {name} par un segment qui n'inclut pas '/'
            sb.append("([^/]+)");
            idx = close + 1;
        }
        return "^" + sb.toString() + "$";
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
