package ru.leymooo.antirelog.util;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;

public class ProtocolLibUtils {
   private static final boolean hasProtocolLib =
         Bukkit.getPluginManager().isPluginEnabled("ProtocolLib") && VersionUtils.isVersion(9);

   private static MinecraftKey toMinecraftKey(Material material) {
      NamespacedKey key = material.getKey();
      return new MinecraftKey(key.getNamespace(), key.getKey());
   }


   private static Material fromMinecraftKey(MinecraftKey key) {
      if (key == null) {
         return null;
      }
      // ProtocolLib MinecraftKey uses getPrefix()/getKey()
      return Material.matchMaterial(key.getPrefix() + ":" + key.getKey());
   }

   /**
    * Send item cooldown via Bukkit API (works on 1.9–1.21+).
    * Avoids ProtocolLib SET_COOLDOWN which has empty field lists on some 1.21 builds.
    */
   public static void sendItemCooldown(Player player, Material material, int ticks) {
      if (player == null || material == null) {
         return;
      }
      try {
         player.setCooldown(material, Math.max(0, ticks));
      } catch (Throwable t) {
         t.printStackTrace();
      }
   }

   /**
    * Kept for compatibility. Prefer {@link #sendItemCooldown}.
    * Returns null when packet cannot be built safely.
    */
   public static PacketContainer createCooldownPacket(Material material, int ticks) {
      if (!hasProtocolLib) {
         return null;
      }
      try {
         PacketContainer packet = new PacketContainer(Server.SET_COOLDOWN);
         packet.getModifier().writeDefaults();
         boolean written = false;
         try {
            if (packet.getMinecraftKeys().size() > 0) {
               packet.getMinecraftKeys().write(0, toMinecraftKey(material));
               written = true;
            }
         } catch (Throwable ignored) {
         }
         if (!written) {
            return null;
         }
         if (packet.getIntegers().size() > 0) {
            packet.getIntegers().write(0, ticks);
         }
         return packet;
      } catch (Throwable t) {
         return null;
      }
   }

   public static void sendPacket(PacketContainer packet, Player player) {
      if (packet == null || player == null || !hasProtocolLib) {
         return;
      }
      try {
         ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
      } catch (Throwable t) {
         t.printStackTrace();
      }
   }

   public static void createListener(final CooldownManager cooldownManager, final PvPManager pvpManager,
                                     final Settings settings, Plugin plugin) {
      if (!hasProtocolLib) {
         return;
      }
      try {
         ProtocolLibrary.getProtocolManager().addPacketListener(
               new PacketAdapter(plugin, ListenerPriority.HIGHEST, Server.SET_COOLDOWN) {
                  private final List<CooldownManager.CooldownType> types =
                        Arrays.asList(CooldownManager.CooldownType.CHORUS, CooldownManager.CooldownType.ENDER_PEARL, CooldownManager.CooldownType.WIND_CHARGE, CooldownManager.CooldownType.MACE);

                  @Override
                  public void onPacketSending(PacketEvent event) {
                     try {
                        Material material = null;
                        try {
                           if (event.getPacket().getMinecraftKeys().size() > 0) {
                              material = fromMinecraftKey(event.getPacket().getMinecraftKeys().read(0));
                           }
                        } catch (Throwable ignored) {
                        }
                        if (material == null) {
                           return;
                        }
                        int ticks = 0;
                        try {
                           if (event.getPacket().getIntegers().size() > 0) {
                              ticks = event.getPacket().getIntegers().read(0);
                           }
                        } catch (Throwable ignored) {
                        }
                        int ms = ticks * 50;
                        for (CooldownManager.CooldownType type : types) {
                           if (material != type.getMaterial()) {
                              continue;
                           }
                           long durationMs = (long) type.getCooldown(settings) * 1000L;
                           if (!cooldownManager.hasCooldown(event.getPlayer(), type, durationMs)) {
                              continue;
                           }
                           long remaining = cooldownManager.getRemaining(event.getPlayer(), type, durationMs);
                           if (Math.abs(remaining - (long) ms) > 100L
                                 && (!pvpManager.isPvPModeEnabled() || pvpManager.isInPvP(event.getPlayer()))) {
                              if (ms == 0) {
                                 event.setCancelled(true);
                                 return;
                              }
                              if (event.getPacket().getIntegers().size() > 0) {
                                 event.getPacket().getIntegers()
                                       .write(0, (int) Math.ceil((double) remaining / 50.0F));
                              }
                           }
                        }
                     } catch (Throwable ignored) {
                     }
                  }
               });
      } catch (Throwable t) {
         plugin.getLogger().warning("Failed to register SET_COOLDOWN listener: " + t.getMessage());
      }
   }


   public static void createListener(CooldownManager cooldownManager, PvPManager pvpManager, Antirelog plugin) {
      // settings is package-private access via reflection-free: use plugin field through public API if any
      try {
         java.lang.reflect.Field f = Antirelog.class.getDeclaredField("settings");
         f.setAccessible(true);
         Settings settings = (Settings) f.get(plugin);
         createListener(cooldownManager, pvpManager, settings, plugin);
      } catch (Exception e) {
         plugin.getLogger().warning("Cannot register cooldown packet listener: " + e.getMessage());
      }
   }

   public static boolean isHasProtocolLib() {
      return hasProtocolLib;
   }
}
