package me.tahacheji.mafana;

import de.metaphoriker.pathetic.mapping.PatheticFacade;
import me.tahacheji.mafana.Command.TestCommands;
import me.tahacheji.mafana.Command.TestSpawning;
import me.tahacheji.mafana.commandExecutor.CommandHandler;
import me.tahacheji.mafana.data.GameEntity;
import me.tahacheji.mafana.event.PlayerJoin;
import me.tahacheji.mafana.event.PlayerLeave;
import me.tahacheji.mafana.event.PlayerRightClick;
import net.minecraft.world.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MafanaEntityManager extends JavaPlugin {

    private static MafanaEntityManager instance;
    private GameEntity gameEntity;
    private List<GameEntity> gameEntityList = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        PatheticFacade.initialize(this);
        CommandHandler.registerCommands(TestSpawning.class, this);
        getServer().getPluginManager().registerEvents(new PlayerRightClick(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new PlayerLeave(), this);
        getServer().getPluginCommand("memtest").setExecutor(new TestCommands());
        gameEntity = new GameEntity(EntityType.ZOMBIE);
    }

    @Override
    public void onDisable() {
        PatheticFacade.shutdown();
    }

    public List<GameEntity> getGameEntityList() {
        return gameEntityList;
    }

    public GameEntity getGameEntity() {
        return gameEntity;
    }

    public static MafanaEntityManager getInstance() {
        return instance;
    }
}
