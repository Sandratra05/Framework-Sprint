package annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// @Retention : définit si l’annotation est disponible au runtime
@Retention(RetentionPolicy.RUNTIME)
// @Target : définit où l’annotation peut être utilisée (classe, méthode, champ, etc.)
@Target(ElementType.METHOD)
public @interface Url {
    String value() default "";
}

