package utils;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import annotations.RequestParam;

public class CheckParameters {
    
    public Object[] checkArgs(Scan.MethodInfo info, HttpServletRequest req) {
        LinkedHashMap<String, String> parametresSimples = new LinkedHashMap<>();
        // Mettre d'abord les paramètres extraits de l'URL pour qu'ils priment sur les paramètres de requête
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
        HashSet<String> clesUtilisees = new HashSet<>();
        Parameter[] parametresMethode = info.method.getParameters();
        Object[] arguments = new Object[parametresMethode.length];
        for (int i = 0; i < parametresMethode.length; i++) {
            Parameter parametre = parametresMethode[i];
            Class<?> type = parametre.getType();
            if (type.equals(Map.class)) {
                // Pour Map<String, Object>, passer tous les paramètres
                HashMap<String, Object> paramsMap = new HashMap<>();
                for (Map.Entry<String, String> e : parametresSimples.entrySet()) {
                    paramsMap.put(e.getKey(), convertStringToObject(e.getValue()));
                }
                arguments[i] = paramsMap;
            } else if (isCustomObject(type)) {
                // Pour objets personnalisés, utiliser le nom du paramètre comme préfixe
                String prefix = parametre.getName() + ".";
                arguments[i] = createAndPopulateObject(type, parametresSimples, clesUtilisees, prefix);
            } else {
                String valeurBrute = resolveValueForParam(parametre, parametresSimples, clesUtilisees);
                arguments[i] = convertValueOrDefault(valeurBrute, type);
            }
        }
        return arguments;
    }

    private boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive() && !type.equals(String.class) && !type.equals(Map.class) && !type.isArray();
    }

    private Object createAndPopulateObject(Class<?> type, LinkedHashMap<String, String> singleParams, Set<String> used, String prefix) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                if (isCustomObject(fieldType)) {
                    // Objet imbriqué : récursion avec préfixe étendu
                    String nestedPrefix = prefix + fieldName + ".";
                    Object nestedObject = createAndPopulateObject(fieldType, singleParams, used, nestedPrefix);
                    field.setAccessible(true);
                    field.set(instance, nestedObject);
                } else {
                    // Champ simple : chercher la clé avec préfixe
                    String fullKey = prefix + fieldName;
                    if (singleParams.containsKey(fullKey)) {
                        field.setAccessible(true);
                        String valueStr = singleParams.get(fullKey);
                        Object convertedValue = convertValueOrDefault(valueStr, fieldType);
                        field.set(instance, convertedValue);
                        used.add(fullKey);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            // En cas d'erreur (pas de constructeur par défaut, etc.), retourner null
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
