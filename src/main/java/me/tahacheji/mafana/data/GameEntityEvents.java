package me.tahacheji.mafana.data;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public interface GameEntityEvents {

    default boolean onSpawn(){return false;}
    default boolean onDeath(){return false;}
    default boolean whileNear() {
        return false;
    }
    default boolean hitMob(Player player) {return false;}
}
