package ru.leymooo.antirelog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PvpPreStartEvent extends Event implements Cancellable {
   private static final HandlerList handlers = new HandlerList();
   private final Player defender;
   private final Player attacker;
   private final int pvpTime;
   private final PvPStatus pvpStatus;
   private boolean cancelled;

   public PvpPreStartEvent(Player defender, Player attacker, int pvpTime, PvPStatus pvpStatus) {
      this.defender = defender;
      this.attacker = attacker;
      this.pvpTime = pvpTime;
      this.pvpStatus = pvpStatus;
   }

   public Player getDefender() {
      return this.defender;
   }

   public Player getAttacker() {
      return this.attacker;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancel) {
      this.cancelled = cancel;
   }

   public int getPvpTime() {
      return this.pvpTime;
   }

   public PvPStatus getPvpStatus() {
      return this.pvpStatus;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }

   public static enum PvPStatus {
      ATTACKER_IN_PVP,
      DEFENDER_IN_PVP,
      ALL_NOT_IN_PVP;

      // $FF: synthetic method
      private static PvPStatus[] $values() {
         return new PvPStatus[]{ATTACKER_IN_PVP, DEFENDER_IN_PVP, ALL_NOT_IN_PVP};
      }
   }
}
