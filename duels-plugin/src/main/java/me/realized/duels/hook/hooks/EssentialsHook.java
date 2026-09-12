package me.realized.duels.hook.hooks;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.config.Config;
import me.realized.duels.util.hook.PluginHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;

public class EssentialsHook extends PluginHook<DuelsPlugin> {
    public static final String NAME = "Essentials";
    private final Config config;
    private Method getUserMethod, isVanishedMethod, setVanishedMethod, setLastLocationMethod;

    public EssentialsHook(final DuelsPlugin plugin) {
        super(plugin, NAME);
        this.config = plugin.getConfiguration();
        try {
            Object essentials = getPlugin();
            this.getUserMethod = essentials.getClass().getMethod("getUser", Player.class);
            Class<?> userClass = Class.forName("com.earth2me.essentials.User");
            this.isVanishedMethod = userClass.getMethod("isVanished");
            this.setVanishedMethod = userClass.getMethod("setVanished", boolean.class);
            this.setLastLocationMethod = userClass.getMethod("setLastLocation", Location.class);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Essentials hook partial: " + e.getMessage());
        }
    }

    private Object getUser(final Player player) {
        if (getUserMethod == null) return null;
        try { return getUserMethod.invoke(getPlugin(), player); } catch (ReflectiveOperationException e) { return null; }
    }

    public void tryUnvanish(final Player player) {
        if (!config.isAutoUnvanish()) return;
        final Object user = getUser(player);
        if (user == null || isVanishedMethod == null || setVanishedMethod == null) return;
        try {
            if (Boolean.TRUE.equals(isVanishedMethod.invoke(user))) setVanishedMethod.invoke(user, false);
        } catch (ReflectiveOperationException ignored) {}
    }

    public void setBackLocation(final Player player, final Location location) {
        if (!config.isSetBackLocation() || setLastLocationMethod == null) return;
        final Object user = getUser(player);
        if (user == null) return;
        try { setLastLocationMethod.invoke(user, location); } catch (ReflectiveOperationException ignored) {}
    }

    public boolean isVanished(final Player player) {
        final Object user = getUser(player);
        if (user == null || isVanishedMethod == null) return false;
        try { return Boolean.TRUE.equals(isVanishedMethod.invoke(user)); } catch (ReflectiveOperationException e) { return false; }
    }
}
