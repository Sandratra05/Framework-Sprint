
import annotations.TestUrl;
import annotations.Url;
import annotations.TestController1;
import annotations.TestController2;
import annotations.TestController3;
import annotations.Controller;

public class Main {
    public static void main(String[] args) throws NoSuchMethodException, SecurityException {

        // --- Existing example: inspect TestUrl#greeting for @Url ---
        Class<?> clazz = TestUrl.class;

        if (clazz.getMethod("greeting").isAnnotationPresent(Url.class)) {
            Url url = clazz.getMethod("greeting").getAnnotation(Url.class);
            System.out.println("Valeur de l'url : " + url.value());
            System.out.println("Nom du methode : " + clazz.getMethod("greeting").getName());
        }

        // --- New: instancier les controllers et vérifier l'annotation @Controller via reflection ---
        Object c1 = new TestController1();
        Object c2 = new TestController2();
        Object c3 = new TestController3();

        checkControllerAnnotation(c1);
        checkControllerAnnotation(c2);
        checkControllerAnnotation(c3);
    }

    private static void checkControllerAnnotation(Object obj) {
        Class<?> cls = obj.getClass();
        boolean isController = cls.isAnnotationPresent(Controller.class);
        if (isController) {
            System.out.println("La classe " + cls.getName() + " est annotée @Controller.");
        } else {
            System.out.println("La classe " + cls.getName() + " n'est pas annotée @Controller.");
        }
    }
}
