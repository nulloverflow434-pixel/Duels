package ru.leymooo.antirelog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.libs.annotatedyaml.Configuration;
import ru.leymooo.antirelog.libs.annotatedyaml.ConfigurationProvider;
import ru.leymooo.antirelog.libs.annotatedyaml.provider.BukkitConfigurationProvider;
import ru.leymooo.antirelog.libs.worldguardwrapper.WorldGuardWrapper;
import ru.leymooo.antirelog.listeners.CooldownListener;
import ru.leymooo.antirelog.listeners.EssentialsTeleportListener;
import ru.leymooo.antirelog.listeners.PvPListener;
import ru.leymooo.antirelog.listeners.WorldGuardListener;
import ru.leymooo.antirelog.manager.BossbarManager;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PowerUpsManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.ProtocolLibUtils;
import ru.leymooo.antirelog.util.VersionUtils;

public class Antirelog extends JavaPlugin {
   private Settings settings;
   private PvPManager pvpManager;
   private CooldownManager cooldownManager;
   private boolean protocolLib;
   private boolean worldguard;

   public void onEnable() {
      this.loadConfig();
      this.pvpManager = new PvPManager(this.settings, this);
      this.detectPlugins();
      this.cooldownManager = new CooldownManager(this, this.settings);
      if (this.protocolLib) {
         ProtocolLibUtils.createListener(this.cooldownManager, this.pvpManager, this);
      }

      this.getServer().getPluginManager().registerEvents(new PvPListener(this, this.pvpManager, this.settings), this);
      this.getServer().getPluginManager().registerEvents(new CooldownListener(this, this.cooldownManager, this.pvpManager, this.settings), this);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("antirelog.reload")) {
         this.reloadSettings();
         sender.sendMessage("§aReloaded");
         this.getLogger().info(this.settings.toString());
      }

      return true;
   }

   private void loadConfig() {
      this.fixFolder();
      this.settings = (Settings)Configuration.builder(Settings.class).file(new File(this.getDataFolder(), "config.yml")).provider(BukkitConfigurationProvider.class).build();
      ConfigurationProvider provider = this.settings.getConfigurationProvider();
      provider.reloadFileFromDisk();
      File file = provider.getConfigFile();
      if (file.exists() && provider.get("config-version") == null) {
         try {
            Files.move(file.toPath(), (new File(file.getParentFile(), "config.old." + System.nanoTime())).toPath(), StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            e.printStackTrace();
         }

         provider.reloadFileFromDisk();
      }

      if (!file.exists()) {
         this.settings.save();
         this.settings.loaded();
         this.getLogger().info("config.yml успешно создан");
      } else if (provider.isFileSuccessfullyLoaded()) {
         if (this.settings.load()) {
            if (!((String)provider.get("config-version")).equals(this.settings.getConfigVersion())) {
               this.getLogger().info("Конфиг был обновлен. Проверьте новые значения");
               this.settings.save();
            }

            this.getLogger().info("Конфиг успешно загружен");
         } else {
            this.getLogger().warning("Не удалось загрузить конфиг");
            this.settings.loaded();
         }
      } else {
         this.getLogger().warning("Can't load settings from file, using default...");
      }

   }

   private void fixFolder() {
      File oldFolder = new File(this.getDataFolder().getParentFile(), "Antirelog");
      if (oldFolder.exists()) {
         try {
            File actualFolder = oldFolder.getCanonicalFile();
            if (actualFolder.getName().equals("Antirelog")) {
               File oldConfig = new File(actualFolder, "config.yml");
               if (!oldConfig.exists()) {
                  this.deleteFolder(actualFolder.toPath());
                  return;
               }

               List<String> oldConfigLines = Files.readAllLines(oldConfig.toPath(), StandardCharsets.UTF_8);
               String firstLine = oldConfigLines.size() > 0 ? (String)oldConfigLines.get(0) : null;
               this.deleteFolder(actualFolder.toPath());
               File newFolder = this.getDataFolder();
               if (!newFolder.exists()) {
                  newFolder.mkdir();
               }

               File oldConfigInNewFolder = new File(newFolder, "config.yml");
               if (firstLine != null && firstLine.startsWith("config-version")) {
                  if (oldConfigInNewFolder.exists()) {
                     Files.move(oldConfigInNewFolder.toPath(), (new File(oldConfigInNewFolder.getParentFile(), "config.old." + System.nanoTime())).toPath());
                  }

                  Files.write(oldConfigInNewFolder.toPath(), oldConfigLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                  this.getLogger().log(Level.WARNING, "Old config.yml file from folder 'Antirelog' was moved to 'AntiRelog' folder");
               } else {
                  Files.write((new File(oldConfigInNewFolder.getParentFile(), "config.old." + System.nanoTime())).toPath(), oldConfigLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                  this.getLogger().log(Level.WARNING, "Old config.yml file from folder 'Antirelog' was moved to 'AntiRelog' folder with different name");
               }
            }
         } catch (IOException e) {
            this.getLogger().log(Level.WARNING, "Something going wrong while renaming folder Antirelog -> AntiRelog", e);
         }

      }
   }

   private void deleteFolder(Path folder) throws IOException {
      Stream<Path> walk = Files.walk(folder);

      try {
         walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      } catch (Throwable var6) {
         if (walk != null) {
            try {
               walk.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (walk != null) {
         walk.close();
      }

   }

   public void reloadSettings() {
      this.settings.getConfigurationProvider().reloadFileFromDisk();
      if (this.settings.getConfigurationProvider().isFileSuccessfullyLoaded()) {
         this.settings.load();
      }

      this.getServer().getScheduler().cancelTasks(this);
      this.pvpManager.onPluginDisable();
      this.pvpManager.onPluginEnable();
      this.cooldownManager.clearAll();
   }

   public boolean isProtocolLibEnabled() {
      return this.protocolLib;
   }

   public boolean isWorldguardEnabled() {
      return this.worldguard;
   }

   private void detectPlugins() {
      if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
         WorldGuardWrapper.getInstance().registerEvents(this);
         Bukkit.getPluginManager().registerEvents(new WorldGuardListener(this.settings, this.pvpManager), this);
         this.worldguard = true;
      }

      try {
         Class.forName("net.ess3.api.events.teleport.PreTeleportEvent");
         Bukkit.getPluginManager().registerEvents(new EssentialsTeleportListener(this.pvpManager, this.settings), this);
      } catch (ClassNotFoundException var2) {
      }

      this.protocolLib = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib") && VersionUtils.isVersion(9);
   }

   public Settings getSettings() {
      return this.settings;
   }

   public PvPManager getPvpManager() {
      return this.pvpManager;
   }

   public PowerUpsManager getPowerUpsManager() {
      return this.pvpManager.getPowerUpsManager();
   }

   public BossbarManager getBossbarManager() {
      return this.pvpManager.getBossbarManager();
   }

   public CooldownManager getCooldownManager() {
      return this.cooldownManager;
   }
}
