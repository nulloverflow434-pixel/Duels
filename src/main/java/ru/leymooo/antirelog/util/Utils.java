package ru.leymooo.antirelog.util;

import org.bukkit.ChatColor;

public class Utils {
   public static String formatTimeUnit(String ed, String a, String b, String c, int n) {
      if (n < 0) {
         n = -n;
      }

      int last = n % 100;
      if (last > 10 && last < 21) {
         return ed + c;
      } else {
         last = n % 10;
         if (last != 0 && last <= 4) {
            if (last == 1) {
               return ed + a;
            } else {
               return last < 5 ? ed + b : ed + c;
            }
         } else {
            return ed + c;
         }
      }
   }

   public static String color(String message) {
      return ChatColor.translateAlternateColorCodes('&', message);
   }

   public static String replaceTime(String message, int time) {
      return message.replace("%time%", Integer.toString(time)).replace("%formated-sec%", formatTimeUnit("секунд", "у", "ы", "", time));
   }
}
