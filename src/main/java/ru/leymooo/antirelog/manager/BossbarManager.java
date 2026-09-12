package ru.leymooo.antirelog.manager;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

public class BossbarManager {
   private final Map<Integer, BossBar> bossBars = new HashMap();
   private final Settings settings;

   public BossbarManager(Settings settings) {
      this.settings = settings;
   }

   public void createBossBars() {
      this.bossBars.clear();
      if (VersionUtils.isVersion(9) && this.settings.getPvpTime() > 0) {
         String title = Utils.color(this.settings.getMessages().getInPvpBossbar());
         if (!title.isEmpty()) {
            double add = (double)1.0F / (double)this.settings.getPvpTime();
            double progress = add;

            for(int i = 1; i <= this.settings.getPvpTime(); ++i) {
               String actualTitle = Utils.replaceTime(title, i);
               BossBar bar = Bukkit.createBossBar(actualTitle, BarColor.RED, BarStyle.SOLID, new BarFlag[0]);
               bar.setProgress(progress);
               progress += add;
               this.bossBars.put(i, bar);
               if (progress > (double)1.0F) {
                  progress = (double)1.0F;
               }
            }
         }
      }

   }

   public void setBossBar(Player player, int time) {
      if (!this.bossBars.isEmpty()) {
         for(BossBar bar : this.bossBars.values()) {
            bar.removePlayer(player);
         }

         ((BossBar)this.bossBars.get(time)).addPlayer(player);
      }

   }

   public void clearBossbar(Player player) {
      for(BossBar bar : this.bossBars.values()) {
         bar.removePlayer(player);
      }

   }

   public void clearBossbars() {
      if (!this.bossBars.isEmpty()) {
         for(BossBar bar : this.bossBars.values()) {
            bar.removeAll();
         }
      }

      this.bossBars.clear();
   }
}
