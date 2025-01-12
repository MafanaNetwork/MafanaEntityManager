package me.tahacheji.mafana.util;


import me.tahacheji.mafana.MafanaEntityManager;
import me.tahacheji.mafana.data.GamePlayerEntity;
import me.tahacheji.mafana.data.GapInfo;
import me.tahacheji.mafana.data.ObstacleInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class MobUtil {

    /*
    public void moveEntitiesToLocation(Location destination, double speed, int durationInSeconds) {
        for (GamePlayerEntity gamePlayerEntity : gamePlayerEntityList) {
            Player player = gamePlayerEntity.getPlayer();
            int entityID = gamePlayerEntity.getEntityID();

            CraftPlayer craftPlayer = (CraftPlayer) player;
            ServerPlayer sp = craftPlayer.getHandle();
            ServerGamePacketListenerImpl ps = sp.connection;

            double[] currentPosition = {
                    gamePlayerEntity.getCurrentLocation().getX(),
                    gamePlayerEntity.getCurrentLocation().getY(),
                    gamePlayerEntity.getCurrentLocation().getZ()
            };

            double deltaX = destination.getX() - currentPosition[0];
            double deltaY = destination.getY() - currentPosition[1];
            double deltaZ = destination.getZ() - currentPosition[2];

            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ); // Total distance to travel
            double ticksNeeded = distance / (speed / 20); // Total ticks needed to complete the journey at the given speed
            double ticksPerSecond = ticksNeeded / durationInSeconds; // Ticks per second required to reach the destination within the given duration

            Vector direction = new Vector(deltaX, deltaY, deltaZ).normalize().multiply(speed / 20);

            new BukkitRunnable() {
                private int ticksElapsed = 0;

                @Override
                public void run() {
                    ticksElapsed++;

                    double[] intermediatePosition = {
                            currentPosition[0] + direction.getX(),
                            currentPosition[1] + direction.getY(),
                            currentPosition[2] + direction.getZ()
                    };

                    // Calculate delta position for the packet
                    short deltaXShort = (short) ((intermediatePosition[0] * 32 - currentPosition[0] * 32) * 128);
                    short deltaYShort = (short) ((intermediatePosition[1] * 32 - currentPosition[1] * 32) * 128);
                    short deltaZShort = (short) ((intermediatePosition[2] * 32 - currentPosition[2] * 32) * 128);

                    // Send the move packet to the client
                    ClientboundMoveEntityPacket.PosRot movePacket = new ClientboundMoveEntityPacket.PosRot(
                            entityID,
                            deltaXShort,
                            deltaYShort,
                            deltaZShort,
                            (byte) destination.getYaw(),
                            (byte) destination.getPitch(),
                            true
                    );

                    // Update the current position
                    currentPosition[0] = intermediatePosition[0];
                    currentPosition[1] = intermediatePosition[1];
                    currentPosition[2] = intermediatePosition[2];

                    if (ticksElapsed >= ticksNeeded) {
                        gamePlayerEntity.setCurrentLocation(destination);
                        this.cancel();
                        System.out.println("Entity has reached the destination.");
                    } else {
                        ps.send(movePacket);
                    }
                }
            }.runTaskTimer(MafanaEntityManager.getInstance(), 0L, (long) (20 / ticksPerSecond));
        }
    }

    public CompletableFuture<Void> entityJump(GamePlayerEntity gamePlayerEntity) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        double jumpVelocity = jumpStrength * 0.1;
        double velocityX = 0;
        double velocityY = jumpVelocity;
        double velocityZ = 0;

        short deltaXShort = (short) (velocityX * 8000);
        short deltaYShort = (short) (velocityY * 8000);
        short deltaZShort = (short) (velocityZ * 8000);

        double totalDistance = Math.sqrt(deltaXShort * deltaXShort + deltaYShort * deltaYShort + deltaZShort * deltaZShort) / 8000.0;

        double distancePerTick = totalDistance / 5;
        new BukkitRunnable() {
            private double distanceMoved = 0;

            @Override
            public void run() {
                if (distanceMoved >= totalDistance) {
                    this.cancel();
                    future.complete(null);
                    applyGravityToEntities(gamePlayerEntity);
                    return;
                }
                isDescending = false;
                onGround = false;

                double fraction = distanceMoved / totalDistance;
                double deltaX = deltaXShort * fraction / 8000.0;
                double deltaY = deltaYShort / 8000.0;
                double deltaZ = deltaZShort * fraction / 8000.0;

                Location currentLocation = gamePlayerEntity.getCurrentLocation();
                Location newLocation = new Location(
                        currentLocation.getWorld(),
                        currentLocation.getX() + deltaX,
                        currentLocation.getY() + deltaY,
                        currentLocation.getZ() + deltaZ,
                        currentLocation.getYaw(),
                        currentLocation.getPitch()
                );

                moveEntity(gamePlayerEntity, newLocation);

                distanceMoved += distancePerTick;
            }
        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);
        return future;
    }



     */


    public boolean isBlockAhead(GamePlayerEntity entity, Vector direction, double maxDistance) {
        Location currentLocation = entity.getCurrentLocation();
        double increment = 0.1;

        for (double distance = increment; distance <= maxDistance; distance += increment) {
            Vector aheadVector = direction.clone().multiply(distance);
            Location aheadLocation = currentLocation.clone().add(aheadVector);

            if (aheadLocation.getBlock().getType().isSolid()) {
                return true;
            }
        }

        return false;
    }

    public boolean areLocationsClose(Location loc1, Location loc2, double radius) {
        // Calculate the Euclidean distance between the two locations
        double distance = Math.sqrt(
                Math.pow(loc1.getX() - loc2.getX(), 2) +
                        Math.pow(loc1.getY() - loc2.getY(), 2) +
                        Math.pow(loc1.getZ() - loc2.getZ(), 2)
        );

        // Check if the distance is within the specified radius
        return distance <= radius;
    }




    public ObstacleInfo isObstacleAhead(GamePlayerEntity gamePlayerEntity, Vector direction, double maxDistance, double jumpHighStrength) {
        Location currentLocation = gamePlayerEntity.getCurrentLocation();
        double increment = 0.1;

        for (double distance = increment; distance <= maxDistance; distance += increment) {
            Vector aheadVector = direction.clone().normalize().multiply(distance);
            Location blockInFrontLocation = currentLocation.clone().add(aheadVector);
            Material blockType = blockInFrontLocation.getBlock().getType();

            if (!blockType.isAir() && blockType.isSolid()) {
                for (double j = 0; j <= jumpHighStrength; j += 0.1) {
                    Location blockAboveObstacleLocation = blockInFrontLocation.clone().add(0, j, 0);
                    Material blockAboveObstacleType = blockAboveObstacleLocation.getBlock().getType();

                    if (blockAboveObstacleType.isAir()) {
                        // Found a jumpable obstacle, return its info
                        return new ObstacleInfo(true, distance, j, blockInFrontLocation);
                    }
                }

                // Found an obstacle but no space to jump over it
                return new ObstacleInfo(true, distance, 0, blockInFrontLocation);
            }
        }

        // No obstacle detected
        return new ObstacleInfo(false, 0, 0, null);
    }



    public GapInfo hasGapInFront(GamePlayerEntity gamePlayerEntity, Vector direction, double jumpLengthStrength, double jumpHighStrength) {
        Location currentLocation = gamePlayerEntity.getCurrentLocation();
        Material blockBelowType = currentLocation.clone().subtract(0, 1, 0).getBlock().getType();

        if ((blockBelowType.isAir() || blockBelowType.isTransparent())) {

            double directionLength = direction.length();
            double maxDistance = Math.ceil(Math.max(jumpLengthStrength, directionLength));

            double bufferDistance = 4;
            double startDistance = Math.max(1, maxDistance - bufferDistance * directionLength);

            double gapDistance;
            double gapHeight = 0;

            for (double i = startDistance; i <= maxDistance; i++) {
                Location blockInFrontLocation = currentLocation.clone().add(direction.clone().normalize().multiply(i + 0.3));

                if (blockInFrontLocation.getBlock().getType().isAir() || blockInFrontLocation.getBlock().getType().isTransparent()) {
                    boolean canJump = false;
                    for (double j = 1; j <= jumpHighStrength; j++) {
                        Location blockAboveJumpLocation = blockInFrontLocation.clone().add(0, j, 0);
                        Material blockAboveJumpType = blockAboveJumpLocation.getBlock().getType();
                        boolean isConnected = !blockAboveJumpLocation.clone().add(0, 1, 0).getBlock().getType().isAir();
                        if (!(blockAboveJumpType.isAir() || blockAboveJumpType.isTransparent()) && !isConnected) {
                            canJump = true;
                            gapHeight = j + 1;
                            break;
                        }
                    }
                    if (canJump) {
                        gapDistance = i;
                        return new GapInfo(true, gapDistance, gapHeight);
                    }
                } else {
                    break;
                }
            }

            for (double i = startDistance; i <= maxDistance; i++) {
                Location blockInFrontLocation = currentLocation.clone().add(direction.clone().normalize().multiply(i + 0.3));

                if (blockInFrontLocation.getBlock().getType().isAir() || blockInFrontLocation.getBlock().getType().isTransparent()) {
                    boolean canJump = false;
                    for (double j = 1; j <= jumpHighStrength; j++) {
                        Location blockBelowJumpLocation = blockInFrontLocation.clone().subtract(0, j, 0);
                        Material blockBelowJumpType = blockBelowJumpLocation.getBlock().getType();
                        if (!(blockBelowJumpType.isAir() || blockBelowJumpType.isTransparent())) {
                            canJump = true;
                            gapHeight = 1;
                            break;
                        }
                    }
                    if (canJump) {
                        gapDistance = i;
                        return new GapInfo(true, gapDistance, gapHeight);
                    }
                } else {
                    break;
                }
            }
        }
        return new GapInfo(false, 0, 0);
    }


    public Block findSolidBlockBelow(Location location) {
        World world = location.getWorld();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        while (blockY >= 0) {
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block.getType().isSolid()) {
                return block;
            }
            blockY--;
        }

        return world.getBlockAt(blockX, location.getBlockY(), blockZ);
    }

    public double calculateJumpDistance(Vector direction, double jhs, double jls) {
        double jumpStrength = 2.8 * jhs;
        double forwardStrength = 5.0 * jls;

        double jumpVelocity = jumpStrength * 0.1;
        double velocityX = direction.getX() * forwardStrength * 0.1;
        double velocityY = jumpVelocity;
        double velocityZ = direction.getZ() * forwardStrength * 0.1;

        short deltaXShort = (short) (velocityX * 8000);
        short deltaYShort = (short) (velocityY * 8000);
        short deltaZShort = (short) (velocityZ * 8000);

        double totalDistance = Math.sqrt(deltaXShort * deltaXShort + deltaYShort * deltaYShort + deltaZShort * deltaZShort) / 8000.0;

        return totalDistance / 5;
    }


    public int generateRandomId() {
        Random random = new Random();
        return random.nextInt(999999999 - 100000000 + 1) + 100000000;
    }



}
