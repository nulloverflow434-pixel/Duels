package ru.leymooo.antirelog.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.config.Settings;

/**
 * Soft-dependency power-up stripper for 1.21.
 * Creative/fly always handled; Essentials/CMI/Vanish via reflection when present.
 */
public class PowerUpsManager {
   private final Settings settings;

   public PowerUpsManager(Settings settings) {
      this.settings = settings;
   }

   public boolean disablePowerUps(Player player) {
      if (player.hasPermission("antirelog.bypass.checks")) {
         return false;
      }
      boolean disabled = false;
      if (player.getGameMode() == GameMode.CREATIVE) {
         player.setGameMode(Bukkit.getDefaultGameMode() == GameMode.ADVENTURE ? GameMode.ADVENTURE : GameMode.SURVIVAL);
         disabled = true;
      }
      if (player.isFlying() || player.getAllowFlight()) {
         player.setFlying(false);
         player.setAllowFlight(false);
         disabled = true;
      }
      // Essentials god/vanish
      try {
         if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            Object ess = Bukkit.getPluginManager().getPlugin("Essentials");
            Object user = ess.getClass().getMethod("getUser", Player.class).invoke(ess, player);
            if (user != null) {
               try {
                  Object god = user.getClass().getMethod("isGodModeEnabled").invoke(user);
                  if (god instanceof Boolean && (Boolean) god) {
                     user.getClass().getMethod("setGodModeEnabled", boolean.class).invoke(user, false);
                     disabled = true;
                  }
               } catch (Throwable ignored) {}
               try {
                  Object van = user.getClass().getMethod("isVanished").invoke(user);
                  if (van instanceof Boolean && (Boolean) van) {
                     user.getClass().getMethod("setVanished", boolean.class).invoke(user, false);
                     disabled = true;
                  }
               } catch (Throwable ignored) {}
            }
         }
      } catch (Throwable ignored) {}
      return disabled;
   }

   public void disablePowerUpsWithRunCommands(Player player) {
      if (this.disablePowerUps(player) && this.settings.getCommandsOnPowerupsDisable() != null
            && !this.settings.getCommandsOnPowerupsDisable().isEmpty()) {
         this.settings.getCommandsOnPowerupsDisable().forEach(command ->
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                     command.replace("%player%", player.getName())));
      }
   }
}
