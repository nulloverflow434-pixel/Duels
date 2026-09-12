package ru.leymooo.antirelog.libs.annotatedyaml;

import java.util.Map;

public interface ConfigurationSettingsSerializer {
   int getIndent();

   String serialize(String var1, Object var2);

   String getLineBreak();

   Map<String, Object> getValues(boolean var1);

   Map<String, Object> getValues(Object var1, boolean var2);

   boolean isConfigurationSection(String var1);

   boolean isConfigurationSection(Object var1);

   void registerSerializable(Class var1);
}
