package me.tahacheji.mafana.data;

import org.bukkit.Location;

public class ObstacleInfo {
    private final boolean hasObstacle;
    private final double distance;
    private final double height;
    private boolean jump;
    private Location location;
    private double ticks;

    public ObstacleInfo(boolean hasObstacle, double distance, double height, Location location) {
        this.hasObstacle = hasObstacle;
        this.distance = distance;
        this.height = height;
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean hasObstacle() {
        return hasObstacle;
    }

    public double getDistance() {
        return distance;
    }

    public double getHeight() {
        return height;
    }

    public double getTicks() {
        return ticks;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void setTicks(double ticks) {
        this.ticks = ticks;
    }
}

