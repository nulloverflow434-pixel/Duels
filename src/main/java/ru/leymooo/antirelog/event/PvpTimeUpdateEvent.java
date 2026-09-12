package ru.leymooo.antirelog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PvpTimeUpdateEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final Player player;
   private final int oldTime;
   private final int newTime;
   private Player damagedPlayer;
   private Player damagedBy;

   public PvpTimeUpdateEvent(Player player, int oldTime, int newTime) {
      this.player = player;
      this.oldTime = oldTime;
      this.newTime = newTime;
   }

   public Player getPlayer() {
      return this.player;
   }

   public int getOldTime() {
      return this.oldTime;
   }

   public int getNewTime() {
      return this.newTime;
   }

   public Player getDamagedPlayer() {
      return this.damagedPlayer;
   }

   public void setDamagedPlayer(Player damagedPlayer) {
      this.damagedPlayer = damagedPlayer;
   }

   public Player getDamagedBy() {
      return this.damagedBy;
   }

   public void setDamagedBy(Player damagedBy) {
      this.damagedBy = damagedBy;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }
}
