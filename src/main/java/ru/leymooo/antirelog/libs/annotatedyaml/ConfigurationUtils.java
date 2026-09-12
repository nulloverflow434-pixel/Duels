package ru.leymooo.antirelog.libs.annotatedyaml;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.leymooo.antirelog.libs.annotatedyaml.util.Files;
import ru.leymooo.antirelog.libs.annotatedyaml.util.Validate;

public class ConfigurationUtils {
   public static Logger LOGGER = Logger.getLogger("AnnotatedYaml");

   public static void save(Configuration configuration, ConfigurationProvider provider) {
      Validate.notNull(configuration, "configuration");
      Validate.notNull(provider, "provider");
      Validate.notNull(provider.getConfigFile(), "provider.getConfigFile()");
      List<String> outList = new ArrayList();
      dump(configuration, provider.getConfigurationSettingsSerializer(), outList, 0);

      try {
         File out = provider.getConfigFile();
         Files.createParentDirs(out);
         File tmpfile = new File(out.getParentFile(), "___tmpconfig");
         java.nio.file.Files.write(tmpfile.toPath(), outList, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

         try {
            java.nio.file.Files.move(tmpfile.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var6) {
            java.nio.file.Files.move(tmpfile.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         LOGGER.log(Level.SEVERE, "Cannot save file ", e);
      }

   }

   private static List<String> dump(ConfigurationSection configuration, ConfigurationSettingsSerializer serializer, List<String> out, int currIndent) {
      try {
         String indent = repeat(" ", currIndent);
         Annotations.Comment comments = (Annotations.Comment)configuration.getClass().getAnnotation(Annotations.Comment.class);
         if (comments != null) {
            processComment(comments, indent, out);
         }

         for(Field field : configuration.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Annotations.Ignore.class) == null) {
               field.setAccessible(true);
               Annotations.Key configKey = (Annotations.Key)field.getAnnotation(Annotations.Key.class);
               String key = configKey == null ? field.getName() : configKey.value();
               comments = (Annotations.Comment)field.getAnnotation(Annotations.Comment.class);
               if (comments != null) {
                  processComment(comments, indent, out);
               }

               Object value = field.get(configuration);
               if (value instanceof ConfigurationSection) {
                  out.add("");
                  out.add(indent + key + ":");
                  dump((ConfigurationSection)value, serializer, out, currIndent + serializer.getIndent());
               } else {
                  out.addAll(Arrays.asList(upgradeIndent(serializer.serialize(key, value), serializer.getLineBreak(), indent)));
               }
            }
         }

         return out;
      } catch (Exception e) {
         throw new RuntimeException("Could not dump config", e);
      }
   }

   private static void processComment(Annotations.Comment comment, String indent, List<String> out) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      String[] comments = comment.value();
      if (comment.enumClass() != Object.class) {
         Enum en = Enum.valueOf(comment.enumClass(), comment.value()[0]);
         comments = (String[])en.getClass().getMethod("getComment").invoke(en);
      }

      for(String cmnt : comments) {
         out.add(indent + (cmnt.startsWith("#") ? cmnt : "#" + cmnt));
      }

   }

   public static void load(Configuration configuration, ConfigurationProvider provider) {
      Validate.notNull(configuration, "configuration");
      Validate.notNull(provider, "provider");
      loadRecursive(configuration, provider, "");
   }

   private static void loadRecursive(ConfigurationSection configuration, ConfigurationProvider provider, String currPath) {
      ConfigurationSettingsSerializer serializer = provider.getConfigurationSettingsSerializer();

      for(Field field : configuration.getClass().getDeclaredFields()) {
         try {
            if (field.getAnnotation(Annotations.Ignore.class) == null && field.getAnnotation(Annotations.Final.class) == null) {
               field.setAccessible(true);
               Class<?> clazz = field.getType();
               Annotations.Key key = (Annotations.Key)field.getAnnotation(Annotations.Key.class);
               String path = (currPath.isEmpty() ? "" : currPath + ".") + (key == null ? field.getName() : key.value());
               Object value = provider.get(path);
               if (serializer.isConfigurationSection(path) && ConfigurationSection.class.isAssignableFrom(clazz)) {
                  loadRecursive((ConfigurationSection)field.get(configuration), provider, path);
               } else {
                  if (serializer.isConfigurationSection(path) && Map.class.isAssignableFrom(clazz)) {
                     value = transformMemorySectionToMap(serializer.getValues(value, true), serializer);
                  } else if (clazz.isEnum() && value instanceof String) {
                     value = tryGetEnum(clazz, (String)value, false);
                  } else if (clazz.isArray() && value instanceof List) {
                     Class<?> clazz1 = field.getType().getComponentType();
                     List list = (List)value;
                     value = Array.newInstance(clazz1, list.size());
                     Object[] arr = (Object[]) value;

                     for(int i = 0; i < list.size(); ++i) {
                        arr[i] = list.get(i);
                     }
                  }

                  if (value == null) {
                     LOGGER.log(Level.WARNING, "Can't set value to '" + field + " (" + clazz.getSimpleName() + ")', because '" + path + "' is not set or null");
                  } else if (clazz.isInstance(value)) {
                     setFieldValue(configuration, field, value);
                  } else {
                     try {
                        setFieldValue(configuration, field, value);
                     } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Can't set '" + value + " (" + value.getClass().getSimpleName() + ")' to '" + field + " (" + clazz.getSimpleName() + ")' for path '" + path + "': " + e.getMessage());
                     }
                  }
               }
            }
         } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Can't set value to '" + field + " (" + field.getType().getSimpleName() + ")': " + e.getMessage());
         }
      }

      configuration.loaded();
   }

   private static Map<String, Object> transformMemorySectionToMap(Map<String, Object> map, ConfigurationSettingsSerializer serializer) {
      for(Map.Entry<String, Object> entry : map.entrySet()) {
         if (serializer.isConfigurationSection(entry.getValue())) {
            entry.setValue(serializer.getValues(entry.getValue(), true));
            transformMemorySectionToMap((Map)entry.getValue(), serializer);
         }
      }

      return map;
   }

   private static Object tryGetEnum(Class clazz, String enumField, boolean fail) {
      try {
         return Enum.valueOf(clazz, enumField);
      } catch (Exception e) {
         if (fail) {
            throw e;
         } else {
            return tryGetEnum(clazz, enumField.toUpperCase(), true);
         }
      }
   }

   private static <T> void setFieldValue(Object object, Field field, T value) throws NoSuchFieldException, IllegalAccessException {
      Field modifiersField;
      boolean isModifiersAccessible;
      try {
         modifiersField = Field.class.getDeclaredField("modifiers");
         isModifiersAccessible = modifiersField.isAccessible();
         modifiersField.setAccessible(true);
      } catch (NoSuchFieldException var7) {
         modifiersField = null;
         isModifiersAccessible = false;
      }

      boolean isFieldAccessible = field.isAccessible();
      field.setAccessible(true);
      if (modifiersField != null) {
         int modifiers = field.getModifiers();
         modifiersField.setInt(field, modifiers & -17);
         field.set(object, value);
         modifiersField.setInt(field, modifiers);
      } else {
         field.set(object, value);
      }

      field.setAccessible(isFieldAccessible);
      if (modifiersField != null) {
         modifiersField.setAccessible(isModifiersAccessible);
      }

   }

   private static String repeat(String s, int n) {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < n; ++i) {
         sb.append(s);
      }

      return sb.toString();
   }

   private static String[] upgradeIndent(String original, String lineBreak, String indent) {
      String[] lines = original.split(lineBreak);

      for(int i = 0; i < lines.length; ++i) {
         lines[i] = indent + lines[i];
      }

      return lines;
   }
}
