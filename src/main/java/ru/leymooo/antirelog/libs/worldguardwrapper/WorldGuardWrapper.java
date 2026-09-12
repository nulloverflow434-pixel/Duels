package ru.leymooo.antirelog.libs.worldguardwrapper;

import org.bukkit.plugin.java.JavaPlugin;

/** Minimal stub – full WG integration disabled in this 1.21 rebuild if impl missing. */
public class WorldGuardWrapper {
   private static final WorldGuardWrapper INSTANCE = new WorldGuardWrapper();

   public static WorldGuardWrapper getInstance() {
      return INSTANCE;
   }

   public void registerEvents(JavaPlugin plugin) {
      // no-op stub for builds without full WG wrapper implementations
   }
}
