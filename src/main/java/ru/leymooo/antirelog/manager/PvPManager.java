package ru.leymooo.antirelog.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.event.PvpPreStartEvent;
import ru.leymooo.antirelog.event.PvpStartedEvent;
import ru.leymooo.antirelog.event.PvpStoppedEvent;
import ru.leymooo.antirelog.event.PvpTimeUpdateEvent;
import ru.leymooo.antirelog.util.ActionBar;
import ru.leymooo.antirelog.util.CommandMapUtils;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

public class PvPManager {
   private final Settings settings;
   private final Antirelog plugin;
   private final Map<Player, Integer> pvpMap = new HashMap();
   private final Map<Player, Integer> silentPvpMap = new HashMap();
   private final PowerUpsManager powerUpsManager;
   private final BossbarManager bossbarManager;
   private final Set<String> whiteListedCommands = new HashSet();

   public PvPManager(Settings settings, Antirelog plugin) {
      this.settings = settings;
      this.plugin = plugin;
      this.powerUpsManager = new PowerUpsManager(settings);
      this.bossbarManager = new BossbarManager(settings);
      this.onPluginEnable();
   }

   public void onPluginDisable() {
      this.pvpMap.clear();
      this.bossbarManager.clearBossbars();
   }

   public void onPluginEnable() {
      this.whiteListedCommands.clear();
      if (this.settings.isDisableCommandsInPvp() && !this.settings.getWhiteListedCommands().isEmpty()) {
         this.settings.getWhiteListedCommands().forEach((wcommand) -> {
            Command command = CommandMapUtils.getCommand(wcommand);
            this.whiteListedCommands.add(wcommand.toLowerCase());
            if (command != null) {
               this.whiteListedCommands.add(command.getName().toLowerCase());
               command.getAliases().forEach((alias) -> this.whiteListedCommands.add(alias.toLowerCase()));
            }

         });
      }

      this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
         if (!this.pvpMap.isEmpty() || !this.silentPvpMap.isEmpty()) {
            this.iterateMap(this.pvpMap, false);
            this.iterateMap(this.silentPvpMap, true);
         }
      }, 20L, 20L);
      this.bossbarManager.createBossBars();
   }

   private void iterateMap(Map<Player, Integer> map, boolean bypassed) {
      if (!map.isEmpty()) {
         for(Player player : new ArrayList<Player>(map.keySet())) {
            int currentTime = bypassed ? this.getTimeRemainingInPvPSilent(player) : this.getTimeRemainingInPvP(player);
            int timeRemaining = currentTime - 1;
            if (timeRemaining <= 0 || this.settings.isDisablePvpInIgnoredRegion() && this.isInIgnoredRegion(player)) {
               if (bypassed) {
                  this.stopPvPSilent(player);
               } else {
                  this.stopPvP(player);
               }
            } else {
               this.updatePvpMode(player, bypassed, timeRemaining);
               this.callUpdateEvent(player, currentTime, timeRemaining);
            }
         }
      }

   }

   public boolean isInPvP(Player player) {
      return this.pvpMap.containsKey(player);
   }

   public boolean isInSilentPvP(Player player) {
      return this.silentPvpMap.containsKey(player);
   }

   public int getTimeRemainingInPvP(Player player) {
      return (Integer)this.pvpMap.getOrDefault(player, 0);
   }

   public int getTimeRemainingInPvPSilent(Player player) {
      return (Integer)this.silentPvpMap.getOrDefault(player, 0);
   }

   public void playerDamagedByPlayer(Player attacker, Player defender) {
      if (defender != attacker && attacker != null && defender != null && attacker.getWorld() == defender.getWorld()) {
         if (defender.getGameMode() == GameMode.CREATIVE) {
            return;
         }

         if (attacker.hasMetadata("NPC") || defender.hasMetadata("NPC")) {
            return;
         }

         if (defender.isDead() || attacker.isDead()) {
            return;
         }

         this.tryStartPvP(attacker, defender);
      }

   }

   private void tryStartPvP(Player attacker, Player defender) {
      if (!this.isInIgnoredWorld(attacker)) {
         if (!this.isInIgnoredRegion(attacker) && !this.isInIgnoredRegion(defender)) {
            if (!this.isPvPModeEnabled() && this.settings.isDisablePowerups()) {
               if (!this.isHasBypassPermission(attacker)) {
                  this.powerUpsManager.disablePowerUpsWithRunCommands(attacker);
               }

               if (!this.isHasBypassPermission(defender)) {
                  this.powerUpsManager.disablePowerUps(defender);
               }

            } else if (this.isPvPModeEnabled()) {
               boolean attackerBypassed = this.isHasBypassPermission(attacker);
               boolean defenderBypassed = this.isHasBypassPermission(defender);
               if (!attackerBypassed || !defenderBypassed) {
                  boolean attackerInPvp = this.isInPvP(attacker) || this.isInSilentPvP(attacker);
                  boolean defenderInPvp = this.isInPvP(defender) || this.isInSilentPvP(defender);
                  PvpPreStartEvent.PvPStatus pvpStatus = PvpPreStartEvent.PvPStatus.ALL_NOT_IN_PVP;
                  if (attackerInPvp && defenderInPvp) {
                     this.updateAttackerAndCallEvent(attacker, defender, attackerBypassed);
                     this.updateDefenderAndCallEvent(defender, attacker, defenderBypassed);
                  } else {
                     if (attackerInPvp) {
                        pvpStatus = PvpPreStartEvent.PvPStatus.ATTACKER_IN_PVP;
                     } else if (defenderInPvp) {
                        pvpStatus = PvpPreStartEvent.PvPStatus.DEFENDER_IN_PVP;
                     }

                     if (pvpStatus != PvpPreStartEvent.PvPStatus.ATTACKER_IN_PVP && pvpStatus != PvpPreStartEvent.PvPStatus.DEFENDER_IN_PVP) {
                        if (this.callPvpPreStartEvent(defender, attacker, pvpStatus)) {
                           this.startPvp(attacker, attackerBypassed, true);
                           this.startPvp(defender, defenderBypassed, false);
                           Bukkit.getPluginManager().callEvent(new PvpStartedEvent(defender, attacker, this.settings.getPvpTime(), pvpStatus));
                        }

                     } else {
                        if (this.callPvpPreStartEvent(defender, attacker, pvpStatus)) {
                           if (attackerInPvp) {
                              this.updateAttackerAndCallEvent(attacker, defender, attackerBypassed);
                              this.startPvp(defender, defenderBypassed, false);
                           } else {
                              this.updateDefenderAndCallEvent(defender, attacker, defenderBypassed);
                              this.startPvp(attacker, attackerBypassed, true);
                           }

                           Bukkit.getPluginManager().callEvent(new PvpStartedEvent(defender, attacker, this.settings.getPvpTime(), pvpStatus));
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void startPvp(Player player, boolean bypassed, boolean attacker) {
      if (!bypassed) {
         String message = Utils.color(this.settings.getMessages().getPvpStarted());
         if (!message.isEmpty()) {
            player.sendMessage(message);
         }

         if (attacker && this.settings.isDisablePowerups()) {
            this.powerUpsManager.disablePowerUpsWithRunCommands(player);
         }

         this.sendTitles(player, true);
      }

      this.updatePvpMode(player, bypassed, this.settings.getPvpTime());
      player.setNoDamageTicks(0);
   }

   private void updatePvpMode(Player player, boolean bypassed, int newTime) {
      if (bypassed) {
         this.silentPvpMap.put(player, newTime);
      } else {
         this.pvpMap.put(player, newTime);
         this.bossbarManager.setBossBar(player, newTime);
         String actionBar = this.settings.getMessages().getInPvpActionbar();
         if (!actionBar.isEmpty()) {
            this.sendActionBar(player, Utils.color(Utils.replaceTime(actionBar, newTime)));
         }

         if (this.settings.isDisablePowerups()) {
            this.powerUpsManager.disablePowerUps(player);
         }
      }

   }

   private boolean callPvpPreStartEvent(Player defender, Player attacker, PvpPreStartEvent.PvPStatus pvpStatus) {
      PvpPreStartEvent pvpPreStartEvent = new PvpPreStartEvent(defender, attacker, this.settings.getPvpTime(), pvpStatus);
      Bukkit.getPluginManager().callEvent(pvpPreStartEvent);
      return !pvpPreStartEvent.isCancelled();
   }

   private void updateAttackerAndCallEvent(Player attacker, Player defender, boolean bypassed) {
      int oldTime = bypassed ? this.getTimeRemainingInPvPSilent(attacker) : this.getTimeRemainingInPvP(attacker);
      this.updatePvpMode(attacker, bypassed, this.settings.getPvpTime());
      PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(attacker, oldTime, this.settings.getPvpTime());
      pvpTimeUpdateEvent.setDamagedPlayer(defender);
      Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
   }

   private void updateDefenderAndCallEvent(Player defender, Player attackedBy, boolean bypassed) {
      int oldTime = bypassed ? this.getTimeRemainingInPvPSilent(defender) : this.getTimeRemainingInPvP(defender);
      this.updatePvpMode(defender, bypassed, this.settings.getPvpTime());
      PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(defender, oldTime, this.settings.getPvpTime());
      pvpTimeUpdateEvent.setDamagedBy(attackedBy);
      Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
   }

   private void callUpdateEvent(Player player, int oldTime, int newTime) {
      PvpTimeUpdateEvent pvpTimeUpdateEvent = new PvpTimeUpdateEvent(player, oldTime, newTime);
      Bukkit.getPluginManager().callEvent(pvpTimeUpdateEvent);
   }

   public void stopPvP(Player player) {
      this.stopPvPSilent(player);
      this.sendTitles(player, false);
      String message = Utils.color(this.settings.getMessages().getPvpStopped());
      if (!message.isEmpty()) {
         player.sendMessage(message);
      }

      String actionBar = this.settings.getMessages().getPvpStoppedActionbar();
      if (!actionBar.isEmpty()) {
         this.sendActionBar(player, Utils.color(actionBar));
      }

   }

   public void stopPvPSilent(Player player) {
      this.pvpMap.remove(player);
      this.bossbarManager.clearBossbar(player);
      this.silentPvpMap.remove(player);
      Bukkit.getPluginManager().callEvent(new PvpStoppedEvent(player));
   }

   public boolean isCommandWhiteListed(String command) {
      return this.whiteListedCommands.isEmpty() ? false : this.whiteListedCommands.contains(command.toLowerCase());
   }

   public PowerUpsManager getPowerUpsManager() {
      return this.powerUpsManager;
   }

   public BossbarManager getBossbarManager() {
      return this.bossbarManager;
   }

   private void sendTitles(Player player, boolean isPvpStarted) {
      String title = isPvpStarted ? this.settings.getMessages().getPvpStartedTitle() : this.settings.getMessages().getPvpStoppedTitle();
      String subtitle = isPvpStarted ? this.settings.getMessages().getPvpStartedSubtitle() : this.settings.getMessages().getPvpStoppedSubtitle();
      title = title.isEmpty() ? null : Utils.color(title);
      subtitle = subtitle.isEmpty() ? null : Utils.color(subtitle);
      if (title != null || subtitle != null) {
         if (VersionUtils.isVersion(11)) {
            player.sendTitle(title, subtitle, 10, 30, 10);
         } else {
            player.sendTitle(title, subtitle);
         }

      }
   }

   private void sendActionBar(Player player, String message) {
      ActionBar.sendAction(player, message);
   }

   public boolean isPvPModeEnabled() {
      return this.settings.getPvpTime() > 0;
   }

   public boolean isBypassed(Player player) {
      return this.isHasBypassPermission(player) || this.isInIgnoredWorld(player);
   }

   public boolean isHasBypassPermission(Player player) {
      return player.hasPermission("antirelog.bypass");
   }

   public boolean isInIgnoredWorld(Player player) {
      return this.settings.getDisabledWorlds().contains(player.getWorld().getName().toLowerCase());
   }

   public boolean isInIgnoredRegion(Player player) {
      // WorldGuard region checks disabled in this 1.21 rebuild (wrapper stripped)
      return false;
   }
}
