package org.popcraft.bolttowny;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.event.town.TownUnclaimEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.SourceTypes;

public final class BoltTowny extends JavaPlugin implements Listener {
    private BoltAPI bolt;
    private Towny towny;

    @Override
    public void onEnable() {
        this.bolt = getServer().getServicesManager().load(BoltAPI.class);
        if (bolt == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.towny = (Towny) getServer().getPluginManager().getPlugin("Towny");
        if (towny == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        bolt.registerPlayerSourceResolver((source, uuid) -> {
            if (!SourceTypes.TOWN.equals(source.getType())) {
                return false;
            }
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return false;
            }
            final String townName = source.getIdentifier();
            final Town town = TownyAPI.getInstance().getTown(townName);
            if (town == null) {
                return false;
            }
            return town.hasResident(uuid);
        });
    }

    @Override
    public void onDisable() {
        this.bolt = null;
        this.towny = null;
    }

    @EventHandler
    public void onTownUnclaim(final TownUnclaimEvent event) {
        removeProtections(event.getWorldCoord());
    }

    @EventHandler
    public void onPlotClear(final PlotClearEvent event) {
        final TownBlock townBlock = event.getTownBlock();
        if (townBlock == null) {
            return;
        }
        removeProtections(townBlock.getWorldCoord());
    }

    @EventHandler
    public void onTownRuin(final TownRuinedEvent event) {
        for (final TownBlock townBlock : event.getTown().getTownBlocks()) {
            removeProtections(townBlock.getWorldCoord());
        }
    }

    private void removeProtections(WorldCoord worldCoord) {
        if (worldCoord == null) {
            return;
        }
        final World world = worldCoord.getBukkitWorld();
        if (world == null) {
            return;
        }
        final int townBlockHeight = world.getMaxHeight() - 1;
        final int townBlockSize = TownySettings.getTownBlockSize();
        for (int x = 0; x < townBlockSize; ++x) {
            for (int z = 0; z < townBlockSize; ++z) {
                for (int y = townBlockHeight; y > 0; --y) {
                    final int blockX = worldCoord.getX() * townBlockSize + x;
                    final int blockZ = worldCoord.getZ() * townBlockSize + z;
                    final Block block = world.getBlockAt(blockX, y, blockZ);
                    if (!bolt.isProtectable(block)) {
                        continue;
                    }
                    final Protection protection = bolt.findProtection(block);
                    if (protection != null) {
                        bolt.removeProtection(protection);
                    }
                }
            }
        }
    }
}
