package ru.leymooo.antirelog.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.config.Messages;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

public class PvPListener implements Listener {
   private static final String META_KEY = "ar-f-shooter";
   private final Plugin plugin;
   private final PvPManager pvpManager;
   private final Messages messages;
   private final Settings settings;
   private final Map<Player, AtomicInteger> allowedTeleports = new HashMap();

   public PvPListener(Plugin plugin, PvPManager pvpManager, Settings settings) {
      this.plugin = plugin;
      this.pvpManager = pvpManager;
      this.settings = settings;
      this.messages = settings.getMessages();
      plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
         this.allowedTeleports.values().forEach((ai) -> ai.set(ai.get() + 1));
         this.allowedTeleports.values().removeIf((ai) -> ai.get() >= 5);
      }, 1L, 1L);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getEntity().getType() == EntityType.PLAYER) {
         Player target = (Player)event.getEntity();
         Player damager = this.getDamager(event.getDamager());
         this.pvpManager.playerDamagedByPlayer(damager, target);
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = true
   )
   public void onInteractWithEntity(PlayerInteractEntityEvent event) {
      if (this.settings.isCancelInteractWithEntities() && this.pvpManager.isPvPModeEnabled() && this.pvpManager.isInPvP(event.getPlayer())) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onCombust(EntityCombustByEntityEvent event) {
      if (event.getEntity() instanceof Player) {
         Player target = (Player)event.getEntity();
         Player damager = this.getDamager(event.getCombuster());
         this.pvpManager.playerDamagedByPlayer(damager, target);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onShootBow(EntityShootBowEvent event) {
      if (VersionUtils.isVersion(14) && event.getProjectile() instanceof Firework && event.getEntity().getType() == EntityType.PLAYER) {
         event.getProjectile().setMetadata("ar-f-shooter", new FixedMetadataValue(this.plugin, event.getEntity().getUniqueId()));
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPotionSplash(PotionSplashEvent e) {
      if (e.getPotion() != null && e.getPotion().getShooter() instanceof Player) {
         Player shooter = (Player)e.getPotion().getShooter();

         for(LivingEntity en : e.getAffectedEntities()) {
            if (en.getType() == EntityType.PLAYER && en != shooter) {
               for(PotionEffect ef : e.getPotion().getEffects()) {
                  if (ef.getType().equals(PotionEffectType.POISON)) {
                     this.pvpManager.playerDamagedByPlayer(shooter, (Player)en);
                  }
               }
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = true
   )
   public void onTeleport(PlayerTeleportEvent ev) {
      if (this.settings.isDisableTeleportsInPvp() && this.pvpManager.isInPvP(ev.getPlayer())) {
         if (this.allowedTeleports.containsKey(ev.getPlayer())) {
            return;
         }

         if (VersionUtils.isVersion(9) && ev.getCause() == TeleportCause.CHORUS_FRUIT || ev.getCause() == TeleportCause.ENDER_PEARL) {
            this.allowedTeleports.put(ev.getPlayer(), new AtomicInteger(0));
            return;
         }

         if (ev.getFrom().getWorld() != ev.getTo().getWorld()) {
            ev.setCancelled(true);
            return;
         }

         if (ev.getFrom().distanceSquared(ev.getTo()) > (double)100.0F) {
            ev.setCancelled(true);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = true
   )
   public void onCommand(PlayerCommandPreprocessEvent e) {
      if (this.settings.isDisableCommandsInPvp() && this.pvpManager.isInPvP(e.getPlayer())) {
         String command = e.getMessage().split(" ")[0].replaceFirst("/", "");
         if (this.pvpManager.isCommandWhiteListed(command)) {
            return;
         }

         e.setCancelled(true);
         String message = Utils.color(this.messages.getCommandsDisabled());
         if (!message.isEmpty()) {
            e.getPlayer().sendMessage(Utils.replaceTime(message, this.pvpManager.getTimeRemainingInPvP(e.getPlayer())));
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onKick(PlayerKickEvent e) {
      Player player = e.getPlayer();
      if (this.pvpManager.isInSilentPvP(player)) {
         this.pvpManager.stopPvPSilent(player);
      } else if (this.pvpManager.isInPvP(player)) {
         this.pvpManager.stopPvPSilent(player);
         if (this.settings.getKickMessages().isEmpty()) {
            this.kickedInPvp(player);
         } else if (e.getReason() != null) {
            String reason = ChatColor.stripColor(e.getReason().toLowerCase());

            for(String killReason : this.settings.getKickMessages()) {
               if (reason.contains(killReason.toLowerCase())) {
                  this.kickedInPvp(player);
                  return;
               }
            }

         }
      }
   }

   private void kickedInPvp(Player player) {
      if (this.settings.isKillOnKick()) {
         player.setHealth((double)0.0F);
         this.sendLeavedInPvpMessage(player);
      }

      if (this.settings.isRunCommandsOnKick()) {
         this.runCommands(player);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onQuit(PlayerQuitEvent e) {
      this.allowedTeleports.remove(e.getPlayer());
      if (this.settings.isHideLeaveMessage()) {
         e.setQuitMessage((String)null);
      }

      if (this.pvpManager.isInPvP(e.getPlayer())) {
         this.pvpManager.stopPvPSilent(e.getPlayer());
         if (this.settings.isKillOnLeave()) {
            this.sendLeavedInPvpMessage(e.getPlayer());
            e.getPlayer().setHealth((double)0.0F);
         } else {
            this.pvpManager.stopPvPSilent(e.getPlayer());
         }

         this.runCommands(e.getPlayer());
      }

      if (this.pvpManager.isInSilentPvP(e.getPlayer())) {
         this.pvpManager.stopPvPSilent(e.getPlayer());
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onDeath(PlayerDeathEvent e) {
      if (this.settings.isHideDeathMessage()) {
         e.setDeathMessage((String)null);
      }

      if (this.pvpManager.isInSilentPvP(e.getEntity()) || this.pvpManager.isInPvP(e.getEntity())) {
         this.pvpManager.stopPvPSilent(e.getEntity());
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onJoin(PlayerJoinEvent e) {
      if (this.settings.isHideJoinMessage()) {
         e.setJoinMessage((String)null);
      }

   }

   private void sendLeavedInPvpMessage(Player p) {
      String message = Utils.color(this.messages.getPvpLeaved()).replace("%player%", p.getName());
      if (!message.isEmpty()) {
         for(Player pl : Bukkit.getOnlinePlayers()) {
            pl.sendMessage(message);
         }
      }

   }

   private void runCommands(Player leaved) {
      if (!this.settings.getCommandsOnLeave().isEmpty()) {
         this.settings.getCommandsOnLeave().forEach((command) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Utils.color(command).replace("%player%", leaved.getName())));
      }

   }

   private Player getDamager(Entity damager) {
      if (damager instanceof Player) {
         return (Player)damager;
      } else {
         if (damager instanceof Projectile) {
            Projectile proj = (Projectile)damager;
            if (proj.getShooter() instanceof Player) {
               return (Player)proj.getShooter();
            }
         } else {
            if (damager instanceof TNTPrimed) {
               TNTPrimed tntPrimed = (TNTPrimed)damager;
               return this.getDamager(tntPrimed.getSource());
            }

            if (VersionUtils.isVersion(9) && damager instanceof AreaEffectCloud) {
               AreaEffectCloud aec = (AreaEffectCloud)damager;
               if (aec.getSource() instanceof Player) {
                  return (Player)aec.getSource();
               }
            } else if (VersionUtils.isVersion(14) && damager instanceof Firework && damager.hasMetadata("ar-f-shooter")) {
               MetadataValue metadata = null;

               for(MetadataValue metadataValue : damager.getMetadata("ar-f-shooter")) {
                  if (metadataValue.getOwningPlugin() == this.plugin) {
                     metadata = metadataValue;
                     break;
                  }
               }

               if (metadata != null) {
                  damager.removeMetadata("ar-f-shooter", this.plugin);
                  return Bukkit.getPlayer((UUID)metadata.value());
               }
            }
         }

         return null;
      }
   }
}
