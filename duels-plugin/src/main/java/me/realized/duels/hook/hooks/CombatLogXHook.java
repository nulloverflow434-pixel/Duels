package me.realized.duels.hook.hooks;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.config.Config;
import me.realized.duels.util.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;

public class CombatLogXHook extends PluginHook<DuelsPlugin> {
    public static final String NAME = "CombatLogX";
    private final Config config;
    private final ArenaManagerImpl arenaManager;
    private Method isInCombatMethod;
    private Object combatManager;

    public CombatLogXHook(final DuelsPlugin plugin) {
        super(plugin, NAME);
        this.config = plugin.getConfiguration();
        this.arenaManager = plugin.getArenaManager();
        try {
            Class.forName("com.SirBlobman.combatlogx.api.event.PlayerPreTagEvent");
            Object combatLogX = getPlugin();
            this.combatManager = combatLogX.getClass().getMethod("getCombatManager").invoke(combatLogX);
            this.isInCombatMethod = combatManager.getClass().getMethod("isInCombat", Player.class);
        } catch (ReflectiveOperationException ex) {
            Bukkit.getLogger().warning("[Duels] CombatLogX API partial: " + ex.getMessage());
        }
        try {
            Class<? extends Event> eventClass = Class.forName("com.SirBlobman.combatlogx.api.event.PlayerPreTagEvent").asSubclass(Event.class);
            EventExecutor executor = (listener, event) -> onPreTag(event);
            Bukkit.getPluginManager().registerEvent(eventClass, new Listener() {}, EventPriority.NORMAL, executor, plugin, true);
        } catch (ClassNotFoundException ignored) {}
    }

    public boolean isTagged(final Player player) {
        if (!config.isClxPreventDuel() || isInCombatMethod == null || combatManager == null) return false;
        try {
            return Boolean.TRUE.equals(isInCombatMethod.invoke(combatManager, player));
        } catch (ReflectiveOperationException e) { return false; }
    }

    private void onPreTag(Event event) {
        if (!config.isClxPreventTag()) return;
        try {
            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
            if (player != null && arenaManager.isInMatch(player)) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
            }
        } catch (ReflectiveOperationException ignored) {}
    }
}
