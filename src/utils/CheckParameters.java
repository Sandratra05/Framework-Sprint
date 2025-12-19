package utils;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import annotations.RequestParam;

public class CheckParameters {
    
    public Object[] checkArgs(Scan.MethodInfo info, HttpServletRequest req) {
        LinkedHashMap<String, String> parametresSimples = new LinkedHashMap<>();
        // Mettre d'abord les paramètres extraits de l'URL pour qu'ils priment sur les paramètres de recdquête
        if (info != null && info.parametresUrl != null) {
            for (Map.Entry<String, String> e : info.parametresUrl.entrySet()) {
                parametresSimples.put(e.getKey(), e.getValue());
            }
        }
        // puis ajouter les paramètres de la requête s'ils ne sont pas déjà présents
        LinkedHashMap<String, String> parametresRequete = extractSingleParams(req);
        for (Map.Entry<String, String> e : parametresRequete.entrySet()) {
            parametresSimples.putIfAbsent(e.getKey(), e.getValue());
        }
        Map<String, List<Part>> fichiers = extractParts(req); // <-- nouveau
        HashSet<String> clesUtilisees = new HashSet<>();
        Parameter[] parametresMethode = info.method.getParameters();
        Object[] arguments = new Object[parametresMethode.length];
        for (int i = 0; i < parametresMethode.length; i++) {
            Parameter parametre = parametresMethode[i];
            Class<?> type = parametre.getType();
            String nomParam = parametre.getName();

            // 1) Part simple
            if (type.equals(Part.class)) {
                String partName = nomParam;  // nom par défaut
                // Vérifier @RequestParam pour override
                if (parametre.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = parametre.getAnnotation(RequestParam.class);
                    if (!rp.value().isEmpty()) {
                        partName = rp.value();
                    }
                }
                List<Part> l = fichiers.get(partName);
                arguments[i] = (l != null && !l.isEmpty()) ? l.get(0) : null;
                continue;
            }

            // 2) Part[] 
            if (type.equals(Part[].class)) {
                String partName = nomParam;  // nom par défaut
                // Vérifier @RequestParam pour override
                if (parametre.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = parametre.getAnnotation(RequestParam.class);
                    if (!rp.value().isEmpty()) {
                        partName = rp.value();
                    }
                }
                List<Part> l = fichiers.get(partName);
                arguments[i] = (l != null) ? l.toArray(new Part[0]) : new Part[0];
                continue;
            }

            // 3) List<Part>
            if (type.equals(java.util.List.class)) {
                // vérifier generic type
                if (parametre.getParameterizedType() instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) parametre.getParameterizedType();
                    if (pt.getActualTypeArguments().length == 1 &&
                        pt.getActualTypeArguments()[0].getTypeName().equals(Part.class.getName())) {
                        String partName = nomParam;  // nom par défaut
                        // Vérifier @RequestParam pour override
                        if (parametre.isAnnotationPresent(RequestParam.class)) {
                            RequestParam rp = parametre.getAnnotation(RequestParam.class);
                            if (!rp.value().isEmpty()) {
                                partName = rp.value();
                            }
                        }
                        List<Part> l = fichiers.get(partName);
                        arguments[i] = (l != null) ? l : new ArrayList<Part>();
                        continue;
                    }
                }
            }

            // 4) Map<String,Object> ou Map<String,byte[]> — inclure fichiers et valeurs string
            if (type.equals(Map.class)) {
                // Vérifier si c'est Map<String, byte[]>
                boolean isByteArrayMap = false;
                if (parametre.getParameterizedType() instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) parametre.getParameterizedType();
                    if (pt.getActualTypeArguments().length == 2) {
                        java.lang.reflect.Type keyType = pt.getActualTypeArguments()[0];
                        java.lang.reflect.Type valType = pt.getActualTypeArguments()[1];
                        if (keyType instanceof Class
                                && ((Class<?>) keyType).equals(String.class)
                                && valType instanceof Class
                                && ((Class<?>) valType).isArray()
                                && ((Class<?>) valType).getComponentType().equals(byte.class)) {
                            isByteArrayMap = true;
                        }
                    }
                }
                // Vérifier @RequestParam pour filtrer les fichiers
                String fileFilter = null;
                if (parametre.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = parametre.getAnnotation(RequestParam.class);
                    if (!rp.value().isEmpty()) {
                        fileFilter = rp.value();
                    }
                }
                if (isByteArrayMap) {
                    // Map<String, byte[]> : clé = nom du fichier (submittedFileName), valeur = contenu en byte[]
                    HashMap<String, byte[]> byteMap = new HashMap<>();
                    for (Map.Entry<String, List<Part>> fe : fichiers.entrySet()) {
                        if (fileFilter == null || fe.getKey().equals(fileFilter)) {
                            for (Part p : fe.getValue()) {
                                try {
                                    String fileName = p.getSubmittedFileName();
                                    if (fileName == null || fileName.isEmpty()) {
                                        // fallback : utiliser le nom du champ ; ajouter index si besoin
                                        fileName = fe.getKey();
                                    }
                                    // assurer unicité de la clé si plusieurs fichiers ont même nom
                                    String key = fileName;
                                    int dup = 1;
                                    while (byteMap.containsKey(key)) {
                                        key = fileName + "_" + (dup++);
                                    }
                                    byte[] content = p.getInputStream().readAllBytes();
                                    byteMap.put(key, content);
                                } catch (Exception e) {
                                    System.out.println("[CheckParameters] error reading part: " + e);
                                }
                            }
                        }
                    }
                    System.out.println("[CheckParameters] Assigned Map<String,byte[]> size=" + byteMap.size() + " filter=" + fileFilter);
                    arguments[i] = byteMap;
                } else {
                    // Map<String, Object> normal
                    HashMap<String, Object> paramsMap = new HashMap<>();
                    for (Map.Entry<String, String> e : parametresSimples.entrySet()) {
                        paramsMap.put(e.getKey(), convertStringToObject(e.getValue()));
                    }
                    // merge fichiers : si un seul Part -> Part, si plusieurs -> List<Part>
                    for (Map.Entry<String, List<Part>> fe : fichiers.entrySet()) {
                        if (fileFilter == null || fe.getKey().equals(fileFilter)) {
                            if (fe.getValue().size() == 1) paramsMap.put(fe.getKey(), fe.getValue().get(0));
                            else paramsMap.put(fe.getKey(), new ArrayList<>(fe.getValue()));
                        }
                    }
                    arguments[i] = paramsMap;
                }
                continue;
            }

            // 5) Objet personnalisé (récursion) — transmettre map de fichiers comme param additionnel
            if (isCustomObject(type)) {
                String prefix = parametre.getName() + ".";  // nom par défaut
                // Vérifier @RequestParam pour override le préfixe
                if (parametre.isAnnotationPresent(RequestParam.class)) {
                    RequestParam rp = parametre.getAnnotation(RequestParam.class);
                    if (!rp.value().isEmpty()) {
                        prefix = rp.value() + ".";
                    }
                }
                arguments[i] = createAndPopulateObject(type, parametresSimples, clesUtilisees, prefix, fichiers);
                continue;
            }

            // 6) type simple existant (String, int, etc.)
            String valeurBrute = resolveValueForParam(parametre, parametresSimples, clesUtilisees);
            arguments[i] = convertValueOrDefault(valeurBrute, type);
        }

        return arguments;
    }

    private boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive() && !type.equals(String.class) && !type.equals(Map.class) && !type.isArray();
    }

    // Extrait les Parts et les regroupe par name
    public Map<String, List<Part>> extractParts(HttpServletRequest req) {
        Map<String, List<Part>> map = new HashMap<>();
        try {
            for (Part p : req.getParts()) {
                String name = p.getName();
                map.computeIfAbsent(name, k -> new ArrayList<>()).add(p);
            }
        } catch (Exception e) {
            // si non-multipart ou erreur, retourner map vide
        }
        return map;
    }

    // signature modifiée pour accepter fichiers
    private Object createAndPopulateObject(Class<?> type, LinkedHashMap<String, String> singleParams, Set<String> used, String prefix, Map<String, List<Part>> fichiers) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                String fullKey = prefix + fieldName;

                if (isCustomObject(fieldType)) {
                    // objet imbriqué : récursion
                    Object nested = createAndPopulateObject(fieldType, singleParams, used, fullKey + ".", fichiers);
                    field.setAccessible(true);
                    field.set(instance, nested);
                } else if (fieldType.equals(Part.class)) {
                    // si champ de type Part -> prendre le premier Part correspondant
                    List<Part> l = fichiers.get(fullKey);
                    if (l != null && !l.isEmpty()) {
                        field.setAccessible(true);
                        field.set(instance, l.get(0));
                        used.add(fullKey);
                    }
                } else if (fieldType.equals(java.util.List.class)) {
                    // si List<Part> (vérifier generic)
                    java.lang.reflect.Type gtype = field.getGenericType();
                    if (gtype instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) gtype;
                        if (pt.getActualTypeArguments().length == 1 &&
                            pt.getActualTypeArguments()[0].getTypeName().equals(Part.class.getName())) {
                            List<Part> l = fichiers.get(fullKey);
                            field.setAccessible(true);
                            field.set(instance, (l != null) ? l : new ArrayList<Part>());
                            used.add(fullKey);
                            continue;
                        }
                    }
                    // sinon champ List<T> non-Part : ignorer ici (ou gérer selon besoin)
                } else {
                    // champ simple (int, String...) : chercher singleParams avec clé fullKey
                    if (singleParams.containsKey(fullKey)) {
                        field.setAccessible(true);
                        String valueStr = singleParams.get(fullKey);
                        Object converted = convertValueOrDefault(valueStr, fieldType);
                        field.set(instance, converted);
                        used.add(fullKey);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            return null;
        }
    }

    public LinkedHashMap<String, String> extractSingleParams(HttpServletRequest req) {
        LinkedHashMap<String, String> flat = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if (e.getValue() != null && e.getValue().length > 0) {
                flat.put(e.getKey(), e.getValue()[0]);
            }
        }
        return flat;
    }

    public boolean isSyntheticName(String name) {
        return name != null && name.matches("arg\\d+");
    }

    public String resolveValueForParam(Parameter param, LinkedHashMap<String, String> singleParams, Set<String> used) {
        String name = param.getName();
        Class<?> type = param.getType();
        // Priorité : annotation @RequestParam si présente et non vide
        RequestParam rp = param.getAnnotation(RequestParam.class);
        if (rp != null) {
            String annotatedName = rp.value();
            if (annotatedName != null && !annotatedName.isEmpty() && singleParams.containsKey(annotatedName)) {
                used.add(annotatedName);
                return singleParams.get(annotatedName);
            }
        }
        // exact match if available
        if (singleParams.containsKey(name)) {
            used.add(name);
            return singleParams.get(name);
        }
        if (!isSyntheticName(name)) {
            return null; // non synthétique mais pas trouvé
        }
        // Heuristique pour paramètres synthétiques
        String candidate = guessSyntheticParamValue(type, singleParams, used);
        return candidate;
    }

    public String guessSyntheticParamValue(Class<?> type, LinkedHashMap<String, String> singleParams, Set<String> used) {
        // priorité sur clés fréquentes
        if ((type == int.class || type == Integer.class) && singleParams.containsKey("id") && !used.contains("id")) {
            used.add("id");
            return singleParams.get("id");
        }
        // Parcours des clés restantes
        List<String> remainingKeys = new ArrayList<>();
        for (String k : singleParams.keySet()) if (!used.contains(k)) remainingKeys.add(k);
        // typage
        for (String k : remainingKeys) {
            String v = singleParams.get(k);
            if (type == int.class || type == Integer.class) {
                try { Integer.parseInt(v); used.add(k); return v; } catch (Exception ignored) {}
            } else if (type == double.class || type == Double.class) {
                try { Double.parseDouble(v); used.add(k); return v; } catch (Exception ignored) {}
            } else if (type == boolean.class || type == Boolean.class) {
                if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v) || "1".equals(v) || "0".equals(v)) { used.add(k); return v; }
            }
        }
        // Pour String / autres: si une seule clé restante, la prendre
        if (!(type.isPrimitive()) && type == String.class) {
            if (remainingKeys.size() == 1) {
                String k = remainingKeys.get(0); used.add(k); return singleParams.get(k);
            }
        }
        return null;
    }

    public Object convertValueOrDefault(String value, Class<?> type) {
        if (value == null) return getDefaultValue(type);
        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == boolean.class || type == Boolean.class) return ("on".equalsIgnoreCase(value) || "1".equals(value)) ? true : Boolean.parseBoolean(value);
            return value; // String ou autre
        } catch (Exception e) {
            return getDefaultValue(type);
        }
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }

    private Object convertStringToObject(String value) {
        if (value == null) return null;
        // Essayer int
        try { return Integer.parseInt(value); } catch (Exception e) {}
        // Essayer double
        try { return Double.parseDouble(value); } catch (Exception e) {}
        // Essayer boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "1".equals(value) || "0".equals(value)) {
            return ("on".equalsIgnoreCase(value) || "1".equals(value)) ? true : Boolean.parseBoolean(value);
        }
        // Sinon, rester String
        return value;
    }
}
