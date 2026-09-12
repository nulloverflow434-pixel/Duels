package ru.leymooo.antirelog.libs.annotatedyaml.util;

public final class Validate {
   public static <T> T notNull(T obj) {
      if (obj == null) {
         throw new IllegalArgumentException();
      } else {
         return obj;
      }
   }

   public static <T> T notNull(T obj, String msgIfNull) {
      if (obj == null) {
         throw new IllegalArgumentException(msgIfNull);
      } else {
         return obj;
      }
   }

   public static String notEmpty(String obj) {
      if (obj != null && !obj.trim().isEmpty()) {
         return obj;
      } else {
         throw new IllegalArgumentException();
      }
   }

   public static String notEmpty(String obj, String msgIfNull) {
      if (obj != null && !obj.trim().isEmpty()) {
         return obj;
      } else {
         throw new IllegalArgumentException(msgIfNull);
      }
   }

   public static void arrayBounds(int off, int len, int arrayLength, String msgPrefix) {
      if (off < 0 || len < 0 || arrayLength - off < len) {
         throw new ArrayIndexOutOfBoundsException(msgPrefix + ": off: " + off + ", len: " + len + ", array length: " + arrayLength);
      }
   }

   public static boolean isTrue(boolean bool, String msgIfFalse) {
      if (!bool) {
         throw new IllegalArgumentException(msgIfFalse);
      } else {
         return bool;
      }
   }

   private Validate() {
      throw new RuntimeException();
   }
}
