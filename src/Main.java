import java.lang.ModuleLayer.Controller;

import annotations.TestUrl;
import annotations.Url;

public class Main {
    public static void main(String[] args) throws NoSuchMethodException, SecurityException {
        
        Class<?> clazz = TestUrl.class;
        
        if (clazz.getMethod("greeting").isAnnotationPresent(Url.class)) {
            Url url = clazz.getMethod("greeting").getAnnotation(Url.class);
            System.out.println("Valeur de l'url : " + url.value());
            System.out.println("Nom du methode : " + clazz.getMethod("greeting").getName());
        }
    }
}
