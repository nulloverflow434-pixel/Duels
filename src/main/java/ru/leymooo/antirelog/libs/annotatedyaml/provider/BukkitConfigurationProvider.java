package ru.leymooo.antirelog.libs.annotatedyaml.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.leymooo.antirelog.libs.annotatedyaml.ConfigurationProvider;
import ru.leymooo.antirelog.libs.annotatedyaml.ConfigurationSettingsSerializer;
import ru.leymooo.antirelog.libs.annotatedyaml.ConfigurationUtils;

public class BukkitConfigurationProvider implements ConfigurationProvider {
   private File file;
   private BukkitConfigurationSettingsSerializer serializer;
   private YamlConfiguration yamlConfiguration;
   private boolean fileLoadedWithoutErrors;

   public BukkitConfigurationProvider() {
   }

   public BukkitConfigurationProvider(File file) {
      this.file = file;
   }

   public File getConfigFile() {
      return this.file;
   }

   public void reloadFileFromDisk() {
      this.yamlConfiguration = new YamlConfiguration();

      try {
         this.fileLoadedWithoutErrors = false;
         this.yamlConfiguration.load(this.file);
         this.fileLoadedWithoutErrors = true;
      } catch (FileNotFoundException var2) {
      } catch (InvalidConfigurationException | IOException e) {
         ConfigurationUtils.LOGGER.log(Level.SEVERE, "Cannot load " + this.file, e);
      }

      if (this.serializer != null) {
         this.serializer.setYamlConfiguration(this.yamlConfiguration);
      } else {
         this.serializer = new BukkitConfigurationSettingsSerializer(this.yamlConfiguration);
      }

   }

   public YamlConfiguration getYamlConfiguration() {
      if (this.yamlConfiguration == null) {
         this.reloadFileFromDisk();
      }

      return this.yamlConfiguration;
   }

   public boolean isFileSuccessfullyLoaded() {
      return this.fileLoadedWithoutErrors;
   }

   public <T> T get(String path) {
      Object value = this.yamlConfiguration.isList(path) ? this.yamlConfiguration.getList(path) : this.yamlConfiguration.get(path);
      return (T)(value == null ? null : value);
   }

   public void set(String path, Object value) {
      this.yamlConfiguration.set(path, value);
   }

   public void setFile(File file) {
      this.file = file;
   }

   public ConfigurationSettingsSerializer getConfigurationSettingsSerializer() {
      return this.serializer;
   }
}
