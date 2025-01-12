package me.tahacheji.mafana.event;

import me.tahacheji.mafana.MafanaEntityManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeave implements Listener {


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList().clear();
    }
}
