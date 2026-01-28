package mg.ririnina.utils;

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

import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.annotations.PostMapping;
import mg.ririnina.annotations.Url;

public class Scan {

    public static class MethodInfo {
        public Class<?> clazz;
        public Method method;
        public String methodeHttp; // "GET", "POST", etc.
        public Map<String, String> parametresUrl = new HashMap<>();

        public MethodInfo(Class<?> clazz, Method method, String methodeHttp) {
            this.clazz = clazz;
            this.method = method;
            this.methodeHttp = methodeHttp;
        }
    }

    public static HashMap<String, List<MethodInfo>> getClassesWithAnnotations(String packageName) throws Exception {
        HashMap<String, List<MethodInfo>> result = new HashMap<>();
        List<Class<?>> classes = getClasses(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    String url = null;
                    String methodeHttp = null;
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        url = method.getAnnotation(GetMapping.class).value();
                        methodeHttp = "GET";
                    } else if (method.isAnnotationPresent(PostMapping.class)) {
                        url = method.getAnnotation(PostMapping.class).value();
                        methodeHttp = "POST";
                    } else if (method.isAnnotationPresent(Url.class)) {
                        url = method.getAnnotation(Url.class).value();
                        methodeHttp = "ANY"; // pour compatibilité avec l'ancien @Url
                    }
                    if (url != null) {
                        result.computeIfAbsent(url, k -> new ArrayList<>()).add(new MethodInfo(clazz, method, methodeHttp));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Trouve la première MethodInfo dont l'url matche et la méthode HTTP correspond.
     * Supporte des patterns de la forme "/ressources/{id}/sub".
     */
    public static MethodInfo findMatching(HashMap<String, List<MethodInfo>> map, String path, String methodeHttp) {
        if (map == null) return null;
        // match exact d'abord
        List<MethodInfo> liste = map.get(path);
        if (liste != null) {
            for (MethodInfo info : liste) {
                if (methodeHttp.equals(info.methodeHttp) || "ANY".equals(info.methodeHttp)) {
                    return info;
                }
            }
        }

        for (Map.Entry<String, List<MethodInfo>> e : map.entrySet()) {
            String pattern = e.getKey();
            String regex = motifEnRegex(pattern);
            try {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    for (MethodInfo info : e.getValue()) {
                        if (methodeHttp.equals(info.methodeHttp) || "ANY".equals(info.methodeHttp)) {
                            // extraire les noms des paramètres et remplir parametresUrl
                            List<String> noms = extraireNomsParametres(pattern);
                            for (int i = 0; i < noms.size(); i++) {
                                String val = m.group(i + 1);
                                info.parametresUrl.put(noms.get(i), val);
                            }
                            return info;
                        }
                    }
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
