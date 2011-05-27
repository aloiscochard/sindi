package sindi;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface inject {
  String qualifier() default "";
}
