package ru.leymooo.antirelog.libs.annotatedyaml.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Files {
   private Files() {
   }

   public static void createParentDirs(File file) throws IOException {
      Objects.requireNonNull(file, "file");
      File parent = file.getCanonicalFile().getParentFile();
      if (parent != null) {
         parent.mkdirs();
         if (!parent.isDirectory()) {
            throw new IOException("Unable to create parent directories of " + file);
         }
      }
   }
}
