package ru.leymooo.antirelog.listeners;

import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.event.PvpStartedEvent;
import ru.leymooo.antirelog.event.PvpStoppedEvent;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

public class CooldownListener implements Listener {
   private final CooldownManager cooldownManager;
   private final PvPManager pvpManager;
   private final Settings settings;

   public CooldownListener(Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager, Settings settings) {
      this.cooldownManager = cooldownManager;
      this.pvpManager = pvpManager;
      this.settings = settings;
      this.registerEntityResurrectEvent(plugin);
   }

   private void registerEntityResurrectEvent(Plugin plugin) {
      if (VersionUtils.isVersion(11)) {
         plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(
               ignoreCancelled = true,
               priority = EventPriority.HIGHEST
            )
            public void onResurrect(EntityResurrectEvent event) {
               if (event.getEntityType() == EntityType.PLAYER) {
                  Player player = (Player)event.getEntity();
                  long cooldownTime = (long)CooldownListener.this.settings.getTotemCooldown();
                  if (cooldownTime != 0L && !CooldownListener.this.pvpManager.isBypassed(player)) {
                     if (cooldownTime <= -1L) {
                        CooldownListener.this.cancelEventIfInPvp(event, CooldownManager.CooldownType.TOTEM, player);
                     } else {
                        cooldownTime *= 1000L;
                        if (CooldownListener.this.checkCooldown(player, CooldownManager.CooldownType.TOTEM, cooldownTime)) {
                           event.setCancelled(true);
                        } else {
                           CooldownListener.this.cooldownManager.addCooldown(player, CooldownManager.CooldownType.TOTEM);
                           CooldownListener.this.addItemCooldownIfNeeded(player, CooldownManager.CooldownType.TOTEM);
                        }
                     }
                  }
               }
            }
         }, plugin);
      }

   }

   @EventHandler
   public void onItemEat(PlayerItemConsumeEvent event) {
      ItemStack consumeItem = event.getItem();
      CooldownManager.CooldownType cooldownType = null;
      long cooldownTime = 0L;
      if (this.isChorus(consumeItem)) {
         cooldownType = CooldownManager.CooldownType.CHORUS;
         cooldownTime = (long)this.settings.getСhorusCooldown();
      }

      if (this.isGoldenOrEnchantedApple(consumeItem)) {
         boolean enchanted = this.isEnchantedGoldenApple(consumeItem);
         cooldownType = enchanted ? CooldownManager.CooldownType.ENC_GOLDEN_APPLE : CooldownManager.CooldownType.GOLDEN_APPLE;
         cooldownTime = enchanted ? (long)this.settings.getEnchantedGoldenAppleCooldown() : (long)this.settings.getGoldenAppleCooldown();
      }

      if (cooldownType != null) {
         if (cooldownTime == 0L || this.pvpManager.isBypassed(event.getPlayer())) {
            return;
         }

         if (cooldownTime <= -1L) {
            this.cancelEventIfInPvp(event, cooldownType, event.getPlayer());
            return;
         }

         cooldownTime *= 1000L;
         if (this.checkCooldown(event.getPlayer(), cooldownType, cooldownTime)) {
            event.setCancelled(true);
            return;
         }

         this.cooldownManager.addCooldown(event.getPlayer(), cooldownType);
         this.addItemCooldownIfNeeded(event.getPlayer(), cooldownType);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPerlLaunch(ProjectileLaunchEvent e) {
      if (this.settings.getEnderPearlCooldown() > 0 && e.getEntityType() == EntityType.ENDER_PEARL && e.getEntity().getShooter() instanceof Player) {
         Player p = (Player)e.getEntity().getShooter();
         if (!this.pvpManager.isBypassed(p) && this.inCooldownScope(p)) {
            this.cooldownManager.addCooldown(p, CooldownManager.CooldownType.ENDER_PEARL);
            this.addItemCooldownIfNeeded(p, CooldownManager.CooldownType.ENDER_PEARL);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInteract(PlayerInteractEvent event) {
      // Only right-click use
      Action action = event.getAction();
      if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
         return;
      }
      // Avoid double-fire for off-hand when main hand already handled
      if (event.getHand() == EquipmentSlot.OFF_HAND && event.getItem() == null) {
         return;
      }

      ItemStack item = event.getItem();
      if (item == null || item.getType().isAir()) {
         // try both hands explicitly
         Player player = event.getPlayer();
         item = player.getInventory().getItemInMainHand();
         if (item == null || item.getType().isAir()) {
            item = player.getInventory().getItemInOffHand();
         }
      }
      if (item == null || item.getType().isAir()) {
         return;
      }
      if (this.pvpManager.isBypassed(event.getPlayer())) {
         return;
      }

      Player player = event.getPlayer();

      // Ender pearl
      if (this.settings.getEnderPearlCooldown() != 0 && item.getType() == Material.ENDER_PEARL) {
         if (this.settings.getEnderPearlCooldown() <= -1) {
            this.cancelEventIfInPvp(event, CooldownManager.CooldownType.ENDER_PEARL, player);
            return;
         }
         if (this.checkCooldown(player, CooldownManager.CooldownType.ENDER_PEARL, (long)(this.settings.getEnderPearlCooldown() * 1000))) {
            event.setCancelled(true);
         }
         return;
      }

      // Firework — CD only in combat, only after a real use attempt, never on combat-enter alone
      if (this.settings.getFireworkCooldown() != 0 && this.isFirework(item)) {
         if (!this.inCooldownScope(player)) {
            return; // outside combat: no CD
         }
         if (this.settings.getFireworkCooldown() <= -1) {
            this.cancelEventIfInPvp(event, CooldownManager.CooldownType.FIREWORK, player);
            return;
         }
         long fwMs = (long)(this.settings.getFireworkCooldown() * 1000);
         if (this.checkCooldown(player, CooldownManager.CooldownType.FIREWORK, fwMs)) {
            event.setCancelled(true);
            return;
         }
         // Apply CD once per successful use (do not refresh if somehow already active)
         if (!this.cooldownManager.hasCooldown(player, CooldownManager.CooldownType.FIREWORK, fwMs)) {
            this.cooldownManager.addCooldown(player, CooldownManager.CooldownType.FIREWORK);
            this.addItemCooldownIfNeeded(player, CooldownManager.CooldownType.FIREWORK);
         }
         return;
      }

      // Wind charge — on right-click only block if already on CD.
      // Actual cooldown is applied when the projectile is launched (real throw).
      if (this.settings.getWindChargeCooldown() != 0 && this.isWindCharge(item)) {
         if (this.settings.getWindChargeCooldown() <= -1) {
            this.cancelEventIfInPvp(event, CooldownManager.CooldownType.WIND_CHARGE, player);
            return;
         }
         long cdMs = (long) this.settings.getWindChargeCooldown() * 1000L;
         if (this.checkCooldown(player, CooldownManager.CooldownType.WIND_CHARGE, cdMs)) {
            event.setCancelled(true);
         }
         return;
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onWindChargeLaunch(ProjectileLaunchEvent e) {
      if (this.settings.getWindChargeCooldown() == 0) {
         return;
      }
      String typeName = e.getEntityType().name();
      if (!typeName.contains("WIND")) {
         return;
      }
      if (!(e.getEntity().getShooter() instanceof Player)) {
         return;
      }
      Player p = (Player)e.getEntity().getShooter();
      if (this.pvpManager.isBypassed(p)) {
         return;
      }
      if (this.settings.getWindChargeCooldown() <= -1) {
         if (this.pvpManager.isInPvP(p)) {
            e.setCancelled(true);
            String message = this.settings.getMessages().getItemDisabledInPvp();
            if (message != null && !message.isEmpty()) {
               p.sendMessage(Utils.color(message));
            }
         }
         return;
      }
      long cd = (long)this.settings.getWindChargeCooldown() * 1000L;
      // Already on CD from previous throw — cancel this one
      if (this.checkCooldown(p, CooldownManager.CooldownType.WIND_CHARGE, cd)) {
         e.setCancelled(true);
         return;
      }
      // Real throw happened — start cooldown + client animation
      this.cooldownManager.addCooldown(p, CooldownManager.CooldownType.WIND_CHARGE);
      this.addItemCooldownIfNeeded(p, CooldownManager.CooldownType.WIND_CHARGE);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onMaceAttack(EntityDamageByEntityEvent event) {
      if (this.settings.getMaceCooldown() == 0) {
         return;
      }
      if (!(event.getDamager() instanceof Player)) {
         return;
      }
      Player player = (Player)event.getDamager();
      if (this.pvpManager.isBypassed(player)) {
         return;
      }
      ItemStack hand = player.getInventory().getItemInMainHand();
      if (hand == null || !this.isMace(hand)) {
         return;
      }
      if (this.settings.getMaceCooldown() <= -1) {
         if (this.pvpManager.isInPvP(player)) {
            event.setCancelled(true);
            String message = this.settings.getMessages().getItemDisabledInPvp();
            if (message != null && !message.isEmpty()) {
               player.sendMessage(Utils.color(message));
            }
         }
         return;
      }
      long cd = (long)this.settings.getMaceCooldown() * 1000L;
      if (this.checkCooldown(player, CooldownManager.CooldownType.MACE, cd)) {
         event.setCancelled(true);
         return;
      }
      this.cooldownManager.addCooldown(player, CooldownManager.CooldownType.MACE);
      this.addItemCooldownIfNeeded(player, CooldownManager.CooldownType.MACE);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.cooldownManager.remove(event.getPlayer());
   }

   @EventHandler
   public void onPvpStart(PvpStartedEvent event) {
      switch (event.getPvpStatus()) {
         case ALL_NOT_IN_PVP:
            this.cooldownManager.enteredToPvp(event.getDefender());
            this.cooldownManager.enteredToPvp(event.getAttacker());
            break;
         case ATTACKER_IN_PVP:
            this.cooldownManager.enteredToPvp(event.getDefender());
            break;
         case DEFENDER_IN_PVP:
            this.cooldownManager.enteredToPvp(event.getAttacker());
      }

   }

   @EventHandler
   public void onPvpStop(PvpStoppedEvent event) {
      this.cooldownManager.removedFromPvp(event.getPlayer());
   }

   private boolean isWindCharge(ItemStack itemStack) {
      return itemStack != null && itemStack.getType().name().equals("WIND_CHARGE");
   }

   private boolean isMace(ItemStack itemStack) {
      return itemStack != null && itemStack.getType().name().equals("MACE");
   }

   private boolean isChorus(ItemStack itemStack) {
      return VersionUtils.isVersion(9) && itemStack.getType() == Material.CHORUS_FRUIT;
   }

   private boolean isGoldenOrEnchantedApple(ItemStack itemStack) {
      return this.isGoldenApple(itemStack) || this.isEnchantedGoldenApple(itemStack);
   }

   private boolean isGoldenApple(ItemStack itemStack) {
      return itemStack.getType() == Material.GOLDEN_APPLE;
   }

   private boolean isEnchantedGoldenApple(ItemStack itemStack) {
      return VersionUtils.isVersion(13) && itemStack.getType() == Material.ENCHANTED_GOLDEN_APPLE || this.isGoldenApple(itemStack) && itemStack.getDurability() >= 1;
   }

   private boolean isFirework(ItemStack itemStack) {
      return VersionUtils.isVersion(13) ? itemStack.getType() == Material.FIREWORK_ROCKET : itemStack.getType() == Material.getMaterial("FIREWORK");
   }

   private void cancelEventIfInPvp(Cancellable event, CooldownManager.CooldownType type, Player player) {
      if (this.pvpManager.isInPvP(player)) {
         event.setCancelled(true);
         String message = type == CooldownManager.CooldownType.TOTEM ? this.settings.getMessages().getTotemDisabledInPvp() : this.settings.getMessages().getItemDisabledInPvp();
         if (!message.isEmpty()) {
            player.sendMessage(Utils.color(message));
         }
      }

   }

   /** Cooldowns apply globally only if PvP-mode is off; otherwise only while in combat. */
   private boolean inCooldownScope(Player player) {
      return !this.pvpManager.isPvPModeEnabled() || this.pvpManager.isInPvP(player);
   }

   private boolean checkCooldown(Player player, CooldownManager.CooldownType cooldownType, long cooldownTime) {
      if (this.inCooldownScope(player) && this.cooldownManager.hasCooldown(player, cooldownType, cooldownTime)) {
         long remaining = this.cooldownManager.getRemaining(player, cooldownType, cooldownTime);
         int remainingInt = (int)TimeUnit.MILLISECONDS.toSeconds(remaining);
         String message = cooldownType == CooldownManager.CooldownType.TOTEM ? this.settings.getMessages().getTotemCooldown() : this.settings.getMessages().getItemCooldown();
         if (!message.isEmpty()) {
            player.sendMessage(Utils.color(Utils.replaceTime(message.replace("%time%", Math.round((float)(remaining / 1000L)) + ""), remainingInt)));
         }

         return true;
      } else {
         return false;
      }
   }

   private void addItemCooldownIfNeeded(Player player, CooldownManager.CooldownType cooldownType) {
      int seconds = cooldownType.getCooldown(this.settings);
      if (seconds <= 0) {
         return;
      }
      // Visual CD only while cooldowns are in scope (in PvP when pvp-mode on)
      if (!this.inCooldownScope(player)) {
         return;
      }
      this.cooldownManager.addItemCooldown(player, cooldownType, (long)seconds * 1000L);
   }
}
