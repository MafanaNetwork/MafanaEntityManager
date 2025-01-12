package me.tahacheji.mafana.Command;

import me.tahacheji.mafana.MafanaEntityManager;
import me.tahacheji.mafana.data.GamePlayerEntity;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(s.equalsIgnoreCase("memtest")) {
            Player player = (Player) commandSender;
            if(strings[0].equalsIgnoreCase("path")) {
                for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
                    try {
                        //MafanaEntityManager.getInstance().getGameEntity().startPathfindingAI(player.getLocation(), g);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }
}
