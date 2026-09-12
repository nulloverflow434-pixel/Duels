package ru.leymooo.antirelog.libs.annotatedyaml;

import java.io.File;

public interface ConfigurationProvider {
   File getConfigFile();

   <T> T get(String var1);

   void set(String var1, Object var2);

   void reloadFileFromDisk();

   void setFile(File var1);

   boolean isFileSuccessfullyLoaded();

   ConfigurationSettingsSerializer getConfigurationSettingsSerializer();
}
