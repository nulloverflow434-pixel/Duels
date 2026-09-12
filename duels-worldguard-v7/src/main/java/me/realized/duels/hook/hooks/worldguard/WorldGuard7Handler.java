package me.realized.duels.hook.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuard7Handler implements WorldGuardHandler {
    @Override
    public String findRegion(final Player player, final Collection<String> regions) {
        final Location location = player.getLocation();
        final BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return null;
        final RegionManager manager = container.get(BukkitAdapter.adapt(player.getWorld()));
        if (manager == null) return null;
        for (final ProtectedRegion region : manager.getApplicableRegions(vector)) {
            if (regions.contains(region.getId())) return region.getId();
        }
        return null;
    }
}
