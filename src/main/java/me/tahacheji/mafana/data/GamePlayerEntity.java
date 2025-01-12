package me.tahacheji.mafana.data;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GamePlayerEntity {

    private final Player player;
    private final int entityID;

    private final double hitboxWidth;
    private final double hitboxHeight;

    private Location currentLocation;

    private EntityType entityType;

    public GamePlayerEntity(Player player, int entityID, double hitboxWidth, double hitboxHeight) {
        this.player = player;
        this.entityID = entityID;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
    }

    public Player getPlayer() {
        return player;
    }

    public int getEntityID() {
        return entityID;
    }

    public double getHitboxWidth() {
        return hitboxWidth;
    }

    public double getHitboxHeight() {
        return hitboxHeight;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }


    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public float getNameTagOffsetY() {
        return (float) (hitboxHeight + 0.5F);
    }


}

