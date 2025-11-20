package utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import annotations.Controller;
import annotations.Url;

public class Scan {

    public static class MethodInfo {
        public Class<?> clazz;
        public Method method;
        public Map<String, String> parametresUrl = new HashMap<>();

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
            String regex = motifEnRegex(pattern);
            try {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    MethodInfo info = e.getValue();
                    // extraire les noms des paramètres et remplir parametresUrl
                    List<String> noms = extraireNomsParametres(pattern);
                    for (int i = 0; i < noms.size(); i++) {
                        String val = m.group(i + 1);
                        info.parametresUrl.put(noms.get(i), val);
                    }
                    return info;
                }
            } catch (Exception ex) {
                // ignorer motif invalide
            }
        }
        return null;
    }

    private static List<String> extraireNomsParametres(String pattern) {
        List<String> noms = new ArrayList<>();
        int idx = 0;
        while ((idx = pattern.indexOf('{', idx)) != -1) {
            int close = pattern.indexOf('}', idx + 1);
            if (close != -1) {
                noms.add(pattern.substring(idx + 1, close));
                idx = close + 1;
            } else {
                break;
            }
        }
        return noms;
    }

    private static String motifEnRegex(String pattern) {
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
