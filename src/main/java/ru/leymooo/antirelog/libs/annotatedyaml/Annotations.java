package ru.leymooo.antirelog.libs.annotatedyaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Annotations {
   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.FIELD, ElementType.TYPE})
   public @interface Comment {
      String[] value();

      Class enumClass() default Object.class;
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.FIELD})
   public @interface Final {
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.FIELD})
   public @interface Ignore {
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.FIELD, ElementType.TYPE})
   public @interface Key {
      String value();
   }
}
