package ru.leymooo.antirelog.manager;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.util.ProtocolLibUtils;
import ru.leymooo.antirelog.util.VersionUtils;

public class CooldownManager {
   private final Antirelog plugin;
   private final Settings settings;
   private final ScheduledExecutorService scheduledExecutorService;
   private final Table<Player, CooldownType, Long> cooldowns = HashBasedTable.create();
   private final Table<Player, CooldownType, ScheduledFuture> futures = HashBasedTable.create();

   public CooldownManager(Antirelog plugin, Settings settings) {
      this.plugin = plugin;
      this.settings = settings;
      this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

   }

   public void addCooldown(Player player, CooldownType type) {
      this.cooldowns.put(player, type, System.currentTimeMillis());
   }

   public void addItemCooldown(Player player, CooldownType type, long duration) {
      if (duration <= 0L || type.getMaterial() == null || type.getMaterial().isAir()) {
         return;
      }
      // Cap visual CD to avoid "infinite" feel from bad config (max 5 min)
      long capped = Math.min(duration, 300000L);
      int durationInTicks = (int)Math.ceil((double)capped / 50.0D);
      // Cancel previous remove-task so CD cannot stack / stick forever
      if (this.scheduledExecutorService != null) {
         ScheduledFuture prev = (ScheduledFuture)this.futures.get(player, type);
         if (prev != null && !prev.isDone()) {
            prev.cancel(false);
         }
      }
      ProtocolLibUtils.sendItemCooldown(player, type.getMaterial(), durationInTicks);
      if (this.scheduledExecutorService != null) {
         ScheduledFuture future = this.scheduledExecutorService.schedule(() -> this.removeItemCooldown(player, type), capped, TimeUnit.MILLISECONDS);
         this.futures.put(player, type, future);
      }
   }

   public void removeItemCooldown(Player player, CooldownType type) {
      ProtocolLibUtils.sendItemCooldown(player, type.getMaterial(), 0);
      if (this.scheduledExecutorService != null) {
         ScheduledFuture future = (ScheduledFuture)this.futures.get(player, type);
         if (future != null && !future.isCancelled()) {
            future.cancel(false);
            this.futures.remove(player, type);
         }
      }
   }

   public void enteredToPvp(Player player) {
      for(CooldownType cooldownType : CooldownManager.CooldownType.values) {
         int cooldown = cooldownType.getCooldown(this.settings);
         if (cooldown == 0) {
            continue;
         }
         // Never pre-apply firework CD on combat start — only after real use
         if (cooldownType == CooldownType.FIREWORK) {
            continue;
         }
         if (cooldown > 0 && this.hasCooldown(player, cooldownType, (long)(cooldown * 1000))) {
            this.addItemCooldown(player, cooldownType, this.getRemaining(player, cooldownType, (long)(cooldown * 1000)));
         }
         // disabled-in-pvp items (cooldown < 0): long visual lock
         if (cooldown < 0) {
            this.addItemCooldown(player, cooldownType, 300000L);
         }
      }
   }

   public void removedFromPvp(Player player) {
      // Clear visual AND internal cooldowns when leaving combat.
      // Otherwise on next PvP enter enteredToPvp() re-applies leftover CD
      // even if the player did not use the item again (e.g. fireworks).
      for(CooldownType cooldownType : CooldownManager.CooldownType.values) {
         int cooldown = cooldownType.getCooldown(this.settings);
         if (cooldown != 0) {
            this.removeItemCooldown(player, cooldownType);
            this.cooldowns.remove(player, cooldownType);
         }
      }
   }

   public boolean hasCooldown(Player player, CooldownType type, long duration) {
      Long added = (Long)this.cooldowns.get(player, type);
      if (added == null) {
         return false;
      } else {
         return System.currentTimeMillis() - added < duration;
      }
   }

   public long getRemaining(Player player, CooldownType type, long duration) {
      Long added = (Long)this.cooldowns.get(player, type);
      return duration - (System.currentTimeMillis() - added);
   }

   public void remove(Player player) {
      this.cooldowns.row(player).clear();
      this.futures.row(player).forEach((ignore, future) -> future.cancel(false));
      this.futures.row(player).clear();
   }

   public void clearAll() {
      this.futures.rowMap().forEach((p, map) -> map.forEach((i, f) -> {
            f.cancel(true);
            this.removeItemCooldown(p, i);
         }));
      this.futures.clear();
      this.cooldowns.clear();
   }

   public Settings getSettings() {
      return this.settings;
   }

   public static enum CooldownType {
      GOLDEN_APPLE(Material.GOLDEN_APPLE, Settings::getGoldenAppleCooldown),
      ENC_GOLDEN_APPLE(VersionUtils.isVersion(13) ? Material.ENCHANTED_GOLDEN_APPLE : Material.GOLDEN_APPLE, Settings::getEnchantedGoldenAppleCooldown),
      ENDER_PEARL(Material.ENDER_PEARL, Settings::getEnderPearlCooldown),
      CHORUS(Material.matchMaterial("CHORUS_FRUIT"), Settings::getСhorusCooldown),
      TOTEM(VersionUtils.isVersion(13) ? Material.TOTEM_OF_UNDYING : Material.matchMaterial("TOTEM"), Settings::getTotemCooldown),
      FIREWORK(VersionUtils.isVersion(13) ? Material.FIREWORK_ROCKET : Material.matchMaterial("FIREWORK"), Settings::getFireworkCooldown),
      MACE(Material.matchMaterial("MACE") != null ? Material.matchMaterial("MACE") : Material.AIR, Settings::getMaceCooldown),
      WIND_CHARGE(Material.matchMaterial("WIND_CHARGE") != null ? Material.matchMaterial("WIND_CHARGE") : Material.AIR, Settings::getWindChargeCooldown);

      public static CooldownType[] values = values();
      Material material;
      Function<Settings, Integer> cooldown;

      private CooldownType(Material material, Function<Settings, Integer> cooldown) {
         this.material = material;
         this.cooldown = cooldown;
      }

      public int getCooldown(Settings settings) {
         return (Integer)this.cooldown.apply(settings);
      }

      public Material getMaterial() {
         return this.material;
      }

      // $FF: synthetic method
      private static CooldownType[] $values() {
         return new CooldownType[]{GOLDEN_APPLE, ENC_GOLDEN_APPLE, ENDER_PEARL, CHORUS, TOTEM, FIREWORK, MACE, WIND_CHARGE};
      }
   }
}
