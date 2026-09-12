package ru.leymooo.antirelog.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ru.leymooo.antirelog.libs.annotatedyaml.Annotations;
import ru.leymooo.antirelog.libs.annotatedyaml.Configuration;

public class Settings extends Configuration {
   @Annotations.Final
   @Annotations.Key("config-version")
   private String configVersion = "1.7";
   private Messages messages = new Messages();
   @Annotations.Comment({"Кулдавн для обычных золотых яблок во время пвп."})
   @Annotations.Key("golden-apple-cooldown")
   private int goldenAppleCooldown = 30;
   @Annotations.Comment({"Кулдавн для зачарованых золотых яблок во время пвп.", "Значение 0 отключает кулдаун; -1 отключает использование во время пвп"})
   @Annotations.Key("enchanted-golden-apple-cooldown")
   private int enchantedGoldenAppleCooldown = 60;
   @Annotations.Comment({"Кулдавн для жемчугов края во время пвп.", "Значение 0 отключает кулдаун; -1 отключает использование во время пвп"})
   @Annotations.Key("ender-pearl-cooldown")
   private int enderPearlCooldown = 15;
   @Annotations.Comment({"Кулдавн для корусов во время пвп.", "https://minecraft-ru.gamepedia.com/Плод_коруса", "Значение 0 отключает кулдаун; -1 отключает использование во время пвп"})
   @Annotations.Key("chorus-cooldown")
   private int сhorusCooldown = 7;
   @Annotations.Comment({"Кулдавн для фейверков во время пвп. (чтобы не убегали на элитрах)", "Значение 0 отключает кулдаун; -1 отключает использование во время пвп"})
   @Annotations.Key("firework-cooldown")
   private int fireworkCooldown = 60;
   @Annotations.Comment({"Кулдавн для тотемов бесмертия во время пвп.", "Значение 0 отключает кулдаун; -1 отключает использование во время пвп"})
   @Annotations.Key("totem-cooldown")
   private int totemCooldown = 60;
   @Annotations.Comment({"Кулдаун на булаву (MACE) в секундах", "0 - отключить, -1 - полностью запретить в PVP"})
   @Annotations.Key("mace-cooldown")
   private int maceCooldown = 10;
   @Annotations.Comment({"Кулдаун на заряд ветра (WIND_CHARGE) в секундах", "0 - отключить, -1 - полностью запретить в PVP"})
   @Annotations.Key("wind-charge-cooldown")
   private int windChargeCooldown = 8;
   @Annotations.Comment({"Длительность пвп"})
   @Annotations.Key("pvp-time")
   private int pvpTime = 12;
   @Annotations.Comment({"Отключить ли возможность писать команды в пвп?"})
   @Annotations.Key("disable-commands-in-pvp")
   private boolean disableCommandsInPvp = true;
   @Annotations.Comment({"Команды которые можно писать во время пвп", "Команды писать без '/' (кол-во '/' - 1)", "Плагин будет пытаться сам определить алисы для команд (msg,tell,m), но для некоторых команд возможно придется самому прописать алиасы", "commands-whitelist:", "- command", "- command2", "- /expand"})
   @Annotations.Key("commands-whitelist")
   private List<String> whiteListedCommands = new ArrayList(0);
   @Annotations.Key("cancel-interact-with-entities")
   @Annotations.Comment({"Отменять ли взаимодействие с энтити, во время пвп"})
   private boolean cancelInteractWithEntities = false;
   @Annotations.Comment({"Убивать ли игрока если он вышел во время пвп?"})
   @Annotations.Key("kill-on-leave")
   private boolean killOnLeave = true;
   @Annotations.Comment({"Убивать ли игрока если его кикнули во время пвп?"})
   @Annotations.Key("kill-on-kick")
   private boolean killOnKick = true;
   @Annotations.Comment({"Выполнять ли команды, если игрока кикнули во время пвп?"})
   @Annotations.Key("run-commands-on-kick")
   private boolean runCommandsOnKick = true;
   @Annotations.Comment({"Какой текст должен быть впричине кика, чтобы его убило/выполнились команды. Если пусто, то будет убивать/выполняться команды всегда"})
   @Annotations.Key("kick-messages")
   private List<String> kickMessages = Arrays.asList("спам", "реклама", "анти-чит");
   @Annotations.Comment({"Какие команды запускать от консоли при выходе игрока во время пвп?", "commands-on-leave:", "- command1", "- command2 %player%"})
   @Annotations.Key("commands-on-leave")
   private List<String> commandsOnLeave = new ArrayList(0);
   @Annotations.Comment({"Отключать ли у игрока который ударил FLY, GM, GOD, VANISH?"})
   @Annotations.Key("disable-powerups")
   private boolean disablePowerups = true;
   @Annotations.Comment({"Какие команды выполнять, если были отключены усиления у игрока", "Данную настройку можно использовать например для того, чтобы наложить на игрока отрицательный эффект, если он начал пвп в ГМ/ФЛАЕ/и тд", "commands-on-powerups-disable: ", "- command1 %player%", "- effect give %player% weakness 10"})
   @Annotations.Key("commands-on-powerups-disable")
   private List<String> commandsOnPowerupsDisable = new ArrayList(0);
   @Annotations.Comment({"Отключать ли возможность телепортироваться во время пвп?"})
   @Annotations.Key("disable-teleports-in-pvp")
   private boolean disableTeleportsInPvp = true;
   @Annotations.Comment({"Игнорировать ли PVP deny во время пвп между игроками?"})
   @Annotations.Key("ignore-worldguard")
   private boolean ignoreWorldGuard = true;
   @Annotations.Comment({"Включать ли игроку, который не участвует в пвп и удрарил другого игрока в pvp, pvp режим", "Если два игрока дерутся на територии где PVP deny и их ударить, то у того кто ударил так-же включится PVP режим"})
   @Annotations.Key("join-pvp-in-worldguard")
   private boolean joinPvPInWorldGuard = false;
   @Annotations.Comment({"В каких регионах не будет работать плагин", "ignored-worldguard-regions:", "- duels1", "- region2"})
   @Annotations.Key("ignored-worldguard-regions")
   private List<String> ignoredWgRegions = new ArrayList(0);
   @Annotations.Ignore
   private Set<String> ignoredWgRegionsSet;
   @Annotations.Comment({"Отключать ли активный ПВП режим когда игрок заходит в игнорируемый регион?"})
   @Annotations.Key("disable-pvp-in-ignored-region")
   private boolean disablePvpInIgnoredRegion = false;
   @Annotations.Comment({"Скрывать ли сообщения о заходе игроков?"})
   @Annotations.Key("hide-join-message")
   private boolean hideJoinMessage = false;
   @Annotations.Comment({"Скрывать ли сообщения о выходе игроков?"})
   @Annotations.Key("hide-leave-message")
   private boolean hideLeaveMessage = false;
   @Annotations.Comment({"Скрывать ли сообщение о смерти игроков?"})
   @Annotations.Key("hide-death-message")
   private boolean hideDeathMessage = false;
   @Annotations.Comment({"Миры в котором плагин не работает"})
   private List<String> disabledWorlds = Arrays.asList("world1", "world2");
   @Annotations.Ignore
   private Set<String> disabledWorldsSet;

   public void loaded() {
      this.ignoredWgRegionsSet = (Set)this.ignoredWgRegions.stream().map(String::toLowerCase).collect(Collectors.toSet());
      this.disabledWorldsSet = (Set)this.disabledWorlds.stream().map(String::toLowerCase).collect(Collectors.toSet());
   }

   public String getConfigVersion() {
      return this.configVersion;
   }

   public Messages getMessages() {
      return this.messages;
   }

   public int getGoldenAppleCooldown() {
      return this.goldenAppleCooldown;
   }

   public int getEnchantedGoldenAppleCooldown() {
      return this.enchantedGoldenAppleCooldown;
   }

   public int getEnderPearlCooldown() {
      return this.enderPearlCooldown;
   }

   public int getСhorusCooldown() {
      return this.сhorusCooldown;
   }

   public int getFireworkCooldown() {
      return this.fireworkCooldown;
   }

   public int getTotemCooldown() {
      return this.totemCooldown;
   }

   public int getMaceCooldown() {
      return this.maceCooldown;
   }

   public int getWindChargeCooldown() {
      return this.windChargeCooldown;
   }

   public int getPvpTime() {
      return this.pvpTime;
   }

   public boolean isDisableCommandsInPvp() {
      return this.disableCommandsInPvp;
   }

   public boolean isCancelInteractWithEntities() {
      return this.cancelInteractWithEntities;
   }

   public List<String> getCommandsOnPowerupsDisable() {
      return this.commandsOnPowerupsDisable;
   }

   public List<String> getWhiteListedCommands() {
      return this.whiteListedCommands;
   }

   public boolean isKillOnLeave() {
      return this.killOnLeave;
   }

   public boolean isKillOnKick() {
      return this.killOnKick;
   }

   public boolean isRunCommandsOnKick() {
      return this.runCommandsOnKick;
   }

   public List<String> getKickMessages() {
      return this.kickMessages;
   }

   public boolean isDisablePowerups() {
      return this.disablePowerups;
   }

   public boolean isDisableTeleportsInPvp() {
      return this.disableTeleportsInPvp;
   }

   public boolean isIgnoreWorldGuard() {
      return this.ignoreWorldGuard;
   }

   public Set<String> getIgnoredWgRegions() {
      return this.ignoredWgRegionsSet;
   }

   public boolean isDisablePvpInIgnoredRegion() {
      return this.disablePvpInIgnoredRegion;
   }

   public boolean isJoinPvPInWorldGuard() {
      return this.joinPvPInWorldGuard;
   }

   public boolean isHideJoinMessage() {
      return this.hideJoinMessage;
   }

   public boolean isHideLeaveMessage() {
      return this.hideLeaveMessage;
   }

   public boolean isHideDeathMessage() {
      return this.hideDeathMessage;
   }

   public List<String> getCommandsOnLeave() {
      return this.commandsOnLeave;
   }

   public Set<String> getDisabledWorlds() {
      return this.disabledWorldsSet;
   }

   public String toString() {
      return "Settings{configVersion='" + this.configVersion + '\'' + ", messages=" + this.messages + ", goldenAppleCooldown=" + this.goldenAppleCooldown + ", enchantedGoldenAppleCooldown=" + this.enchantedGoldenAppleCooldown + ", enderPearlCooldown=" + this.enderPearlCooldown + ", сhorusCooldown=" + this.сhorusCooldown + ", fireworkCooldown=" + this.fireworkCooldown + ", totemCooldown=" + this.totemCooldown + ", maceCooldown=" + this.maceCooldown + ", windChargeCooldown=" + this.windChargeCooldown + ", pvpTime=" + this.pvpTime + ", disableCommandsInPvp=" + this.disableCommandsInPvp + ", whiteListedCommands=" + this.whiteListedCommands + ", cancelInteractWithEntities=" + this.cancelInteractWithEntities + ", killOnLeave=" + this.killOnLeave + ", killOnKick=" + this.killOnKick + ", runCommandsOnKick=" + this.runCommandsOnKick + ", kickMessages=" + this.kickMessages + ", commandsOnLeave=" + this.commandsOnLeave + ", disablePowerups=" + this.disablePowerups + ", commandsOnPowerupsDisable=" + this.commandsOnPowerupsDisable + ", disableTeleportsInPvp=" + this.disableTeleportsInPvp + ", ignoreWorldGuard=" + this.ignoreWorldGuard + ", joinPvPInWorldGuard=" + this.joinPvPInWorldGuard + ", ignoredWgRegions=" + this.ignoredWgRegions + ", disablePvpInIgnoredRegion=" + this.disablePvpInIgnoredRegion + ", hideJoinMessage=" + this.hideJoinMessage + ", hideLeaveMessage=" + this.hideLeaveMessage + ", hideDeathMessage=" + this.hideDeathMessage + ", disabledWorlds=" + this.disabledWorlds + '}';
   }
}
