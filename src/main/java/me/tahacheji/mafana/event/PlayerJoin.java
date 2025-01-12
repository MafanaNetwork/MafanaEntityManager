package me.tahacheji.mafana.event;

import me.tahacheji.mafana.MafanaEntityManager;
import me.tahacheji.mafana.data.GameEntity;
import me.tahacheji.mafana.util.MobUtil;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoin implements Listener {


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                /*
                for(int i = 0; i < 1000; i++) {
                    GameEntity gameEntity = new GameEntity(EntityType.ZOMBIE);
                    gameEntity.spawnClientSideEntity(event.getPlayer(), new Location(Bukkit.getWorld("world"), -12.3,1,6.5));
                    MafanaEntityManager.getInstance().getGameEntityList().add(gameEntity);
                }

                 */
                MafanaEntityManager.getInstance().getGameEntity().spawnClientSideEntity(event.getPlayer(), new Location(Bukkit.getWorld("world"), -12.3,1,6.5));
            }
        }.runTaskAsynchronously(MafanaEntityManager.getInstance());
    }
}
