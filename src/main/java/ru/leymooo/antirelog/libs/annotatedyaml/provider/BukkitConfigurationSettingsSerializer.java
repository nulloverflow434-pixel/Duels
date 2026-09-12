package ru.leymooo.antirelog.libs.annotatedyaml.provider;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.representer.Representer;
import ru.leymooo.antirelog.libs.annotatedyaml.ConfigurationSettingsSerializer;
import ru.leymooo.antirelog.libs.annotatedyaml.util.Validate;

public class BukkitConfigurationSettingsSerializer implements ConfigurationSettingsSerializer {
   private YamlConfiguration yamlConfiguration;
   private DumperOptions dumperOptions;
   private Representer representer;
   private Yaml yaml;

   public BukkitConfigurationSettingsSerializer(YamlConfiguration yamlConfiguration) {
      this.yamlConfiguration = yamlConfiguration;
   }

   private void updateYamlSettings() {
      if (this.dumperOptions == null || this.representer == null || this.yaml == null) {
         try {
            Class<?> clazz = this.yamlConfiguration.getClass();
            Field dumperOptionsField = getField(clazz, "yamlOptions", "yamlDumperOptions");
            Field representerField = getField(clazz, "yamlRepresenter", "representer");
            Field yamlField = clazz.getDeclaredField("yaml");
            dumperOptionsField.setAccessible(true);
            representerField.setAccessible(true);
            yamlField.setAccessible(true);
            this.dumperOptions = (DumperOptions)dumperOptionsField.get(this.yamlConfiguration);
            this.representer = (Representer)representerField.get(this.yamlConfiguration);
            this.yaml = (Yaml)yamlField.get(this.yamlConfiguration);
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }

      this.dumperOptions.setIndent(this.yamlConfiguration.options().indent());
      this.dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
      this.representer.setDefaultFlowStyle(FlowStyle.BLOCK);
   }

   public void setYamlConfiguration(YamlConfiguration yamlConfiguration) {
      this.yamlConfiguration = yamlConfiguration;
      this.dumperOptions = null;
      this.representer = null;
      this.yaml = null;
      this.updateYamlSettings();
   }

   public int getIndent() {
      return this.yamlConfiguration.options().indent();
   }

   public String serialize(String key, Object object) {
      this.updateYamlSettings();
      Map<String, Object> map = new LinkedHashMap(1);
      if (object instanceof Enum) {
         object = ((Enum)object).name();
      }

      map.put(key, object);
      return this.yaml.dump(map);
   }

   public String getLineBreak() {
      return this.dumperOptions.getLineBreak().getString();
   }

   public Map<String, Object> getValues(boolean deep) {
      return this.yamlConfiguration.getValues(deep);
   }

   public Map<String, Object> getValues(Object section, boolean deep) {
      return (Map<String, Object>)(section instanceof ConfigurationSection ? ((ConfigurationSection)section).getValues(deep) : new HashMap());
   }

   public boolean isConfigurationSection(String path) {
      return this.yamlConfiguration.isConfigurationSection(path);
   }

   public boolean isConfigurationSection(Object object) {
      return object instanceof ConfigurationSection;
   }

   public void registerSerializable(Class clazz) {
      Validate.isTrue(ConfigurationSerializable.class.isAssignableFrom(clazz), "Class " + clazz + " does not implement ConfigurationSerializable");
      ConfigurationSerialization.registerClass(clazz);
   }

   private static Field getField(Class clazz, String oldName, String newName) throws NoSuchFieldException {
      try {
         return clazz.getDeclaredField(oldName);
      } catch (Exception var4) {
         return clazz.getDeclaredField(newName);
      }
   }
}
