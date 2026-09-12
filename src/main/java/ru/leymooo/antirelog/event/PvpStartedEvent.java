package ru.leymooo.antirelog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PvpStartedEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final Player defender;
   private final Player attacker;
   private final int pvpTime;
   private final PvpPreStartEvent.PvPStatus pvpStatus;

   public PvpStartedEvent(Player defender, Player attacker, int pvpTime, PvpPreStartEvent.PvPStatus pvpStatus) {
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

   public int getPvpTime() {
      return this.pvpTime;
   }

   public PvpPreStartEvent.PvPStatus getPvpStatus() {
      return this.pvpStatus;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }
}
