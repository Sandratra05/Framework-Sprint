package mg.ririnina.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJson((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sb.append("\"").append(escapeJson(e.getKey().toString())).append("\":");
                sb.append(toJson(e.getValue()));
                if (i < map.size() - 1) sb.append(",");
                i++;
            }
            sb.append("}");
            return sb.toString();
        }
        // Objet personnalisé : sérialiser les champs
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            f.setAccessible(true);
            try {
                Object value = f.get(obj);
                sb.append("\"").append(escapeJson(f.getName())).append("\":");
                sb.append(toJson(value));
                if (i < fields.length - 1) sb.append(",");
            } catch (Exception e) {
                // ignorer
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    
    public static String createResponseJson(Object data, String status, int code) {
        String dataJson = toJson(data);
        boolean isList = data instanceof List;
        String countPart = isList ? ",\"count\":" + ((List<?>) data).size() : "";
        return "{\"status\":\"" + status + "\",\"code\":" + code + countPart + ",\"data\":" + dataJson + "}";
    }
    
}