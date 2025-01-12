package me.tahacheji.mafana.event;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import me.tahacheji.mafana.MafanaEntityManager;
import me.tahacheji.mafana.data.GamePlayerEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerRightClick implements Listener {


    @EventHandler
    public void onClick(PlayerUseUnknownEntityEvent event) {
        Player player = event.getPlayer();
        if(event.isAttack()) {
            for (GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
                MafanaEntityManager.getInstance().getGameEntity().playerHurtEntity(player, g, 0);
            }
        }
    }
}
