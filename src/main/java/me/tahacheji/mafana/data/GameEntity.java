package me.tahacheji.mafana.data;

import de.metaphoriker.pathetic.api.pathing.Pathfinder;
import de.metaphoriker.pathetic.api.pathing.configuration.HeuristicWeights;
import de.metaphoriker.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.filters.PassablePathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.filters.SolidGroundPathFilter;
import de.metaphoriker.pathetic.api.pathing.result.PathfinderResult;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;
import de.metaphoriker.pathetic.mapping.PathfinderFactory;
import de.metaphoriker.pathetic.mapping.bukkit.BukkitMapper;
import io.netty.buffer.Unpooled;
import me.tahacheji.mafana.MafanaEntityManager;

import me.tahacheji.mafana.data.filter.GapTraversalFilter;
import me.tahacheji.mafana.data.filter.MinimumHeightFilter;
import me.tahacheji.mafana.data.filter.NavigateAroundObstacleFilter;
import me.tahacheji.mafana.util.CollisionUtil;
import me.tahacheji.mafana.util.MobUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftSound;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.damage.CraftDamageType;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

public class GameEntity {

    private final EntityType entityType;

    private List<GamePlayerEntity> gamePlayerEntityList = new ArrayList<>();

    private double maxHealth = 1;
    private double health = 1;

    private double gravityStrength = 1;
    private double speed = 3;
    private double jumpStrength = 3;
    private double knockBackStrength = 1.8;
    private double knockBackJumpHeight = 0.35;
    private double knockBackResistance = 0;
    private double viewRange = 5;
    private double attackRange = 2;
    private double attackSpeed = 1;

    private double jumpHighStrength = 1;
    private double jumpLengthStrength = 1;

    private double ambientSoundRange = 5;
    private double damageSoundRange = 5;
    private double deathSoundRange = 5;
    private double getDamagedByEntitySoundRange = 5;
    private Sound damageEntitySound = null;
    private Sound getDamagedByEntitySound = null;
    private Sound ambientSound = null;
    private Sound deathSound = null;

    private boolean onGround = true;
    private boolean isDescending = false;
    private boolean canStandOnFluids = false;

    private double entityHeight = 0.0;
    private double entityWidth = 0.0;

    private Location location;

    private boolean gravity = false;

    private CollisionUtil collisionUtil;

    public GameEntity(EntityType entityType) {
        this.entityType = entityType;
        collisionUtil = new CollisionUtil();
    }

    public void setSounds(Location location) {
        ServerLevel craftWorld = ((CraftWorld) location.getWorld()).getHandle();
        LivingEntity livingEntity = (LivingEntity) entityType.create(craftWorld.getLevel());
        if (livingEntity != null) {
            //fix sound
            getDamagedByEntitySound = CraftSound.minecraftToBukkit(livingEntity.getHurtSound0(new DamageSource(CraftDamageType.bukkitToMinecraftHolder(DamageType.MOB_ATTACK))));
            damageEntitySound = CraftSound.minecraftToBukkit(livingEntity.getHurtSound0(new DamageSource(CraftDamageType.bukkitToMinecraftHolder(DamageType.GENERIC))));
            deathSound = CraftSound.minecraftToBukkit(livingEntity.getDeathSound());
            try {
                if (Sound.valueOf("entity." + livingEntity.getName().getString().toLowerCase() + ".ambient") != null) {
                    ambientSound = Sound.valueOf("entity." + livingEntity.getName().getString().toLowerCase() + ".ambient");
                }
            } catch (IllegalArgumentException e) {
                ambientSound = Sound.AMBIENT_CAVE;
            }
        }
    }


    public void spawnClientSideEntity(Player player, Location location) {
        CraftPlayer nmsPlayer = (CraftPlayer) player;
        ServerPlayer sp = nmsPlayer.getHandle();
        ServerGamePacketListenerImpl ps = sp.connection;
        try {
            final double[] x = {(double) location.getX()};
            final double[] y = {(double) location.getY()};
            final double[] z = {(double) location.getZ()};
            int entityID = new MobUtil().generateRandomId();
            ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(entityID, UUID.randomUUID(), x[0], y[0], z[0],
                    location.getPitch(), location.getYaw(), entityType, 0, Vec3.ZERO, location.getYaw());
            ps.send(spawnPacket);
            GamePlayerEntity gamePlayerEntity = new GamePlayerEntity(player, entityID, Math.round(entityType.getWidth()), Math.round(entityType.getHeight()));
            MafanaEntityManager.getInstance().getLogger().log(Level.INFO, entityType.getHeight() + ": HEIGHT " + entityType.getWidth() + ": WIDTH");
            entityWidth = entityType.getWidth();
            entityHeight = entityType.getHeight();
            gamePlayerEntity.setCurrentLocation(location);
            setLocation(location);
            gamePlayerEntity.setEntityType(entityType);
            gamePlayerEntityList.add(gamePlayerEntity);
        } catch (Exception e) {
            Bukkit.getLogger().warning("An error occurred while sending client-side entity packet:");
            e.printStackTrace();
        }
    }

    public void startFollowingPlayer(GamePlayerEntity follower, Player player) {

        // Tracks if the follower is navigating *and* the future that’s doing it
        final boolean[] isNavigating = {false};
        final CompletableFuture<Void>[] currentPathFuture = new CompletableFuture[]{null};

        // We also track where the player was when we started the current path
        final Location[] lastKnownTargetLoc = {player.getLocation().clone()};

        new BukkitRunnable() {
            @Override
            public void run() {
                // 1) Safety checks
                if (follower == null
                        || follower.getPlayer() == null
                        || !follower.getPlayer().isOnline()
                        || player == null
                        || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 2) Distance check
                Location followerLoc = follower.getCurrentLocation();
                Location currentPlayerLoc = player.getLocation();
                double distance = followerLoc.distance(currentPlayerLoc);

                // If within attackRange, do nothing
                if (distance <= attackRange) {
                    return;
                }

                // 3) If we are already in the middle of navigating
                if (isNavigating[0]) {
                    // Has the player moved significantly from the location we planned for?
                    // e.g., > 2 blocks difference (adjust as you wish).
                    if (lastKnownTargetLoc[0].distance(currentPlayerLoc) > 2.0) {
                        // Player has moved a lot, so the old path is stale
                        // Cancel the old path so we can start a fresh one
                        if (currentPathFuture[0] != null && !currentPathFuture[0].isDone()) {
                            currentPathFuture[0].completeExceptionally(
                                    new RuntimeException("Target moved; path cancelled")
                            );
                        }
                        // Mark as free to navigate
                        isNavigating[0] = false;
                    } else {
                        // The player hasn't moved enough to matter; let the old path finish
                        return;
                    }
                }

                isNavigating[0] = true;
                lastKnownTargetLoc[0] = currentPlayerLoc.clone();

                // 5) Start a new path to the player's *current* location
                currentPathFuture[0] = moveEntityWithPatheticMapper(follower, currentPlayerLoc)
                        .thenRun(() -> {
                            // Once done, mark free to navigate
                            isNavigating[0] = false;
                        })
                        .exceptionally(ex -> {
                            // If it was canceled or errored, also mark free to navigate
                            isNavigating[0] = false;
                            return null;
                        });
            }
        }.runTaskTimerAsynchronously(
                MafanaEntityManager.getInstance(),
                0L,
                2L  // check/follow every second
        );
    }

    public CompletableFuture<Void> moveEntityToLocation(GamePlayerEntity gamePlayerEntity, Location destination) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        gravity = false;
        double pointX = gamePlayerEntity.getCurrentLocation().clone().getX() - Math.floor(gamePlayerEntity.getCurrentLocation().clone().getX());
        double pointZ = gamePlayerEntity.getCurrentLocation().clone().getZ() - Math.floor(gamePlayerEntity.getCurrentLocation().clone().getZ());
        double deltaX = (destination.getX() + pointX) - gamePlayerEntity.getCurrentLocation().getX();
        double deltaZ = (destination.getZ() + pointZ) - gamePlayerEntity.getCurrentLocation().getZ();

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double speedPerTick = speed / 20.0;
        final double[] totalDistanceMoved = {0};
        final Vector[] direction = {new Vector(deltaX, 0, deltaZ).normalize().multiply(speed / 20)};

        final boolean[] isJumping = {false};

        new BukkitRunnable() {
            @Override
            public void run() {
                Location currentLocation = gamePlayerEntity.getCurrentLocation();
                double remainingDistance = horizontalDistance - totalDistanceMoved[0];

                if (remainingDistance <= 0) {
                    future.complete(null);
                    this.cancel();
                    return;
                }

                if (remainingDistance < speedPerTick) {
                    direction[0] = new Vector(deltaX, 0, deltaZ).normalize().multiply(remainingDistance);
                }

                if (!isJumping[0] && !gravity) {
                    ObstacleInfo obstacleInfo = checkObstacles(gamePlayerEntity, direction[0]);
                    GapInfo gapInfo = checkGaps(gamePlayerEntity, direction[0]);

                    if (obstacleInfo.hasObstacle()) {
                        handleObstacle(gamePlayerEntity, direction[0], obstacleInfo);
                    } else if (gapInfo.hasGap()) {
                        handleGap(gamePlayerEntity, direction[0], gapInfo);
                    } else {
                        totalDistanceMoved[0] += direction[0].length();
                        moveEntity(gamePlayerEntity, currentLocation.clone().add(direction[0]));
                        System.out.println("MOVING...");
                        handleFall(currentLocation);
                    }
                }

            }

            private ObstacleInfo checkObstacles(GamePlayerEntity gamePlayerEntity, Vector direction) {
                return new MobUtil().isObstacleAhead(gamePlayerEntity, direction.clone(), .6, jumpHighStrength);
            }

            private GapInfo checkGaps(GamePlayerEntity gamePlayerEntity, Vector direction) {
                return new MobUtil().hasGapInFront(gamePlayerEntity, direction.clone().normalize().multiply(0.6), jumpLengthStrength, jumpHighStrength);
            }

            private void handleFall(Location currentLocation) {
                Block blockBelow = currentLocation.clone().subtract(0, 0.01, 0).getBlock();
                boolean isSolidBelow = blockBelow.getType().isSolid();
                if (!isSolidBelow) {
                    if (!new MobUtil().hasGapInFront(gamePlayerEntity, direction[0].clone().normalize().multiply(0.7), jumpLengthStrength, jumpHighStrength).hasGap() && (!isJumping[0] && !gravity)) {
                        System.out.println("FALLING...");
                        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
                            applyGravityToEntities(gamePlayerEntity, 0).thenAcceptAsync(x -> {
                            });
                        });
                    }
                }
            }

            private void handleObstacle(GamePlayerEntity gamePlayerEntity, Vector direction, ObstacleInfo obstacleInfo) {
                isJumping[0] = true;
                if (obstacleInfo.getDistance() <= jumpLengthStrength && obstacleInfo.getHeight() <= jumpHighStrength) {
                    System.out.println("JUMP STARTED...");
                    entityJumpForwardToLocation(gamePlayerEntity, obstacleInfo.getLocation().clone().add(0,1,0), Math.round(obstacleInfo.getHeight()) + .2 , 1.2).thenAcceptAsync(x -> {
                        isJumping[0] = false;
                        System.out.println("JUMP ENDED...");
                        //totalDistanceMoved[0] += (x * speedPerTick);
                    });
                    //entityJumpForward(gamePlayerEntity, direction.clone(), obstacleInfo.getHeight(), 1).thenAcceptAsync(x -> {
                        //isJumping[0] = false;
                        //totalDistanceMoved[0] += (x * speedPerTick) + 0.91404986706489375;
                    //});
                }
            }

            private void handleGap(GamePlayerEntity gamePlayerEntity, Vector direction, GapInfo gapInfo) {
                isJumping[0] = true;
                if (gapInfo.getDistance() <= jumpLengthStrength && gapInfo.getHeight() <= jumpHighStrength) {
                    entityJumpForward(gamePlayerEntity, direction.clone(), gapInfo.getHeight(), gapInfo.getDistance()).thenAcceptAsync(x -> {
                        isJumping[0] = false;
                        totalDistanceMoved[0] += (x * speedPerTick) + 0.91404986706489375;
                    });
                }
            }


        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);
        return future;
    }

    public CompletableFuture<Void> moveEntityWithPatheticMapper(GamePlayerEntity gamePlayerEntity, Location destination) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        PathPosition start = BukkitMapper.toPathPosition(gamePlayerEntity.getCurrentLocation());
        PathPosition target = BukkitMapper.toPathPosition(destination);

        Pathfinder reusablePathfinder =
                PathfinderFactory.createPathfinder(
                        PathfinderConfiguration.createConfiguration()
                                .withAsync(true)
                                .withAllowingFailFast(true)
                                .withAllowingFallback(true)
                                .withLoadingChunks(true)
                                .withHeuristicWeights(HeuristicWeights.NATURAL_PATH_WEIGHTS)

                );

        List<PathFilter> p = new ArrayList<>();
        p.add(new PassablePathFilter());
        p.add(new MinimumHeightFilter((int) jumpHighStrength));
        p.add(new NavigateAroundObstacleFilter());
        p.add(new SolidGroundPathFilter());
        //p.add(new GapTraversalFilter((int) jumpLengthStrength));
        CompletionStage<PathfinderResult> pathfindingResult =
                reusablePathfinder.findPath(
                        start,
                        target,
                        p
                );

        /*
        Player player = Bukkit.getPlayer("msked");
        pathfindingResult.thenAccept(
                result -> {
                    player.sendMessage("State: " + result.getPathState().name());
                    player.sendMessage("Path length: " + result.getPath().length());

                    // If pathfinding is successful, show the path to the player
                    if (result.successful() || result.hasFallenBack()) {
                        result
                                .getPath()
                                .forEach(
                                        position -> {
                                            Location location = BukkitMapper.toLocation(position);
                                            player.sendBlockChange(
                                                    location, Material.GLASS.createBlockData());
                                        });
                    } else {
                        player.sendMessage("Path not found!");
                    }
                });



         */

        pathfindingResult.thenAcceptAsync(result -> {
            if (result.successful() || result.hasFallenBack()) {
                List<Location> locations = new ArrayList<>();
                result.getPath().forEach(position -> {
                    Location location = BukkitMapper.toLocation(position);
                    locations.add(location);
                });
                locations.remove(0);
                CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                for (Location loc : locations) {
                    chain = chain.thenCompose(v -> moveEntityToLocation(gamePlayerEntity, loc));
                }

                chain.thenRun(() -> future.complete(null))
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });
            } else {
                future.completeExceptionally(new RuntimeException("Path not found!"));
            }
        });


        return future;
    }

    public void teleportEntity(GamePlayerEntity gamePlayerEntity, Location destination, boolean applyGravity) {
        int entityID = gamePlayerEntity.getEntityID();
        CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
        ServerPlayer sp = craftPlayer.getHandle();
        ServerGamePacketListenerImpl ps = sp.connection;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeVarInt(entityID);
        buf.writeDouble(destination.getX());
        buf.writeDouble(destination.getY());
        buf.writeDouble(destination.getZ());
        buf.writeByte((byte) destination.getYaw());
        buf.writeByte((byte) destination.getPitch());
        buf.writeBoolean(true);

        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(buf);

        ps.send(teleportPacket);

        setLocation(destination);
        gamePlayerEntity.setCurrentLocation(destination);
        if (applyGravity) {
            applyGravityToEntities(gamePlayerEntity, 0);
        }
    }

    public CompletableFuture<Double> applyGravityToEntities(GamePlayerEntity gamePlayerEntity, double x) {
        if (!gravity) {
            CompletableFuture<Double> future = new CompletableFuture<>();
            final double verticalSpeed = -((gravityStrength + x) * 10) / 20;
            gravity = true;

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    ticks++;
                    Location currentLocation = gamePlayerEntity.getCurrentLocation();
                    World world = currentLocation.getWorld();

                    double halfWidth = entityWidth / 2.0;
                    double height = entityHeight;
                    BoundingBox entityBB = BoundingBox.of(
                            new Vector(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ()),
                            new Vector(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ())
                    ).expand(halfWidth, height / 2.0, halfWidth);

                    Location belowLocation = currentLocation.clone().add(0, -0.1, 0);
                    Block blockBelow = belowLocation.getBlock();
                    boolean isSolidBelow = blockBelow.getType().isSolid();

                    if (isSolidBelow) {
                        gravity = false;
                        this.cancel();
                        future.complete((double) ticks);

                        double finalY = blockBelow.getY() + 1;
                        currentLocation.setY(finalY);

                        teleportEntity(gamePlayerEntity, currentLocation, false);
                        onGround = true;
                        return;
                    }
                    double moveY = collisionUtil.adjustY(entityBB, verticalSpeed, world);
                    if (Math.abs(moveY) < 0.001) {
                        gravity = false;
                        this.cancel();
                        future.complete((double) ticks);

                        Block solidBlockBelow = new MobUtil().findSolidBlockBelow(currentLocation);
                        double finalY = solidBlockBelow.getY() + 1;
                        currentLocation.setY(finalY);

                        teleportEntity(gamePlayerEntity, currentLocation, false);
                        onGround = true;
                        return;
                    }

                    Location newLocation = currentLocation.clone().add(0, moveY, 0);
                    sendMovementPacket(gamePlayerEntity, currentLocation, newLocation);
                    setLocation(newLocation);
                    gamePlayerEntity.setCurrentLocation(newLocation);
                }

                private void sendMovementPacket(GamePlayerEntity gamePlayerEntity, Location oldLocation, Location newLocation) {
                    int entityID = gamePlayerEntity.getEntityID();
                    CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
                    ServerPlayer sp = craftPlayer.getHandle();
                    ServerGamePacketListenerImpl ps = sp.connection;

                    short deltaXShort = (short) (((newLocation.getX() - oldLocation.getX()) * 32) * 128);
                    short deltaYShort = (short) (((newLocation.getY() - oldLocation.getY()) * 32) * 128);
                    short deltaZShort = (short) (((newLocation.getZ() - oldLocation.getZ()) * 32) * 128);

                    ClientboundMoveEntityPacket.PosRot movePacket = new ClientboundMoveEntityPacket.PosRot(
                            entityID,
                            deltaXShort,
                            deltaYShort,
                            deltaZShort,
                            (byte) newLocation.getYaw(),
                            (byte) newLocation.getPitch(),
                            false
                    );

                    ps.send(movePacket);
                }
            }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

            return future;
        }
        return new CompletableFuture<>();
    }

    public CompletableFuture<Double> entityJumpForward(GamePlayerEntity gamePlayerEntity, Vector direction, double jhs, double jls) {
        CompletableFuture<Double> future = new CompletableFuture<>();

        direction = direction.setY(0).normalize();

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
        double distancePerTick = totalDistance / 5;

        new BukkitRunnable() {
            private double distanceMoved = 0;

            @Override
            public void run() {
                if (distanceMoved >= totalDistance) {
                    this.cancel();
                    future.complete(totalDistance);
                    applyGravityToEntities(gamePlayerEntity, 0);
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

                int entityID = gamePlayerEntity.getEntityID();
                CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
                ServerPlayer sp = craftPlayer.getHandle();
                ServerGamePacketListenerImpl ps = sp.connection;

                short deltaXShort = (short) ((newLocation.getX() * 32 - gamePlayerEntity.getCurrentLocation().getX() * 32) * 128);
                short deltaYShort = (short) ((newLocation.getY() * 32 - gamePlayerEntity.getCurrentLocation().getY() * 32) * 128);
                short deltaZShort = (short) ((newLocation.getZ() * 32 - gamePlayerEntity.getCurrentLocation().getZ() * 32) * 128);

                ClientboundMoveEntityPacket.PosRot movePacket = new ClientboundMoveEntityPacket.PosRot(
                        entityID,
                        deltaXShort,
                        deltaYShort,
                        deltaZShort,
                        (byte) newLocation.getYaw(),
                        (byte) newLocation.getPitch(),
                        false
                );

                ps.send(movePacket);
                setLocation(newLocation);
                gamePlayerEntity.setCurrentLocation(newLocation);

                distanceMoved += distancePerTick;
            }
        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

        return future;
    }

    public CompletableFuture<Double> entityJumpToLocation(GamePlayerEntity gamePlayerEntity, Location destination, double jumpHeight, double speed) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        Location currentLocation = gamePlayerEntity.getCurrentLocation();
        destination.add(0, 0.0001, 0);

        Vector direction = destination.toVector().subtract(currentLocation.toVector());
        direction = direction.setY(0).normalize();

        double horizontalDistance = currentLocation.distance(destination);
        double totalTicks = speed * 20;
        double jumpVelocity = (0.42 * jumpHeight);
        double gravity = 0.08 * jumpHeight;
        double distancePerTick = horizontalDistance / totalTicks;

        Vector finalDirection = direction.clone().multiply(distancePerTick);

        new BukkitRunnable() {
            private boolean jumpStarted = false;
            private double ticksElapsed = 0;
            private double verticalVelocity = jumpVelocity;

            @Override
            public void run() {
                if (ticksElapsed >= totalTicks) {
                    teleportEntity(gamePlayerEntity, destination, false);
                    this.cancel();
                    future.complete(totalTicks);
                    return;
                }

                Location newLocation = getLocation();

                moveEntity(gamePlayerEntity, newLocation);

                if (!jumpStarted && ticksElapsed + 2 >= totalTicks / 2) {
                    jumpStarted = true;
                }

                ticksElapsed++;
            }

            private @NotNull Location getLocation() {
                double deltaX = finalDirection.getX();
                double deltaZ = finalDirection.getZ();
                double deltaY = getDeltaY();

                Location current = gamePlayerEntity.getCurrentLocation();
                return new Location(
                        current.getWorld(),
                        current.getX() + deltaX,
                        current.getY() + deltaY,
                        current.getZ() + deltaZ,
                        current.getYaw(),
                        current.getPitch()
                );
            }

            private double getDeltaY() {
                if (!jumpStarted) return 0;

                double deltaY = verticalVelocity;
                verticalVelocity -= gravity;

                if (ticksElapsed == totalTicks - 1) {
                    deltaY = destination.getY() - gamePlayerEntity.getCurrentLocation().getY();
                }

                return deltaY;
            }
        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

        return future;
    }

    public CompletableFuture<Void> entityJumpForwardToLocation(GamePlayerEntity gamePlayerEntity, Location destination, double jumpHeight, double speed) {
        System.out.println("Starting Location: " + gamePlayerEntity.getCurrentLocation());
        CompletableFuture<Void> future = new CompletableFuture<>();
        Location currentLocation = gamePlayerEntity.getCurrentLocation();
        destination.add(0, 0.001, 0);

        Vector direction = destination.toVector().subtract(currentLocation.toVector());
        direction = direction.setY(0).normalize();

        double horizontalDistance = currentLocation.distance(destination);
        double jumpVelocity = 0.42 * jumpHeight;
        double gravity = 0.08;

        double totalTicks = (speed * 20);
        double distancePerTick = horizontalDistance / totalTicks;

        Vector forwardDirection = direction.clone().multiply(distancePerTick);

        Vector finalDirection = direction;
        new BukkitRunnable() {
            private boolean jumpStarted = false;
            private double verticalVelocity = jumpVelocity;
            private double t = 0;

            @Override
            public void run() {
                if (t >= totalTicks) {
                    System.out.println("Current Location: " + gamePlayerEntity.getCurrentLocation());
                    System.out.println("Destination: " + destination);
                    //teleportEntity(gamePlayerEntity, destination, true);
                    this.cancel();
                    future.complete(null);
                    return;
                }

                if (new MobUtil().isBlockAhead(gamePlayerEntity, finalDirection, 0.5) && !jumpStarted) {
                    jumpStarted = true;
                    verticalVelocity = jumpVelocity;
                }

                Location newLocation = getLocation();
                moveEntity(gamePlayerEntity, newLocation);
                t++;
            }

            private @NotNull Location getLocation() {
                double deltaX = forwardDirection.getX();
                double deltaZ = forwardDirection.getZ();
                double deltaY = getDeltaY();

                Location current = gamePlayerEntity.getCurrentLocation();
                return new Location(
                        current.getWorld(),
                        current.getX() + deltaX,
                        current.getY() + deltaY,
                        current.getZ() + deltaZ,
                        current.getYaw(),
                        current.getPitch()
                );
            }

            private double getDeltaY() {
                if (!jumpStarted) return 0;

                double deltaY = verticalVelocity;
                verticalVelocity -= gravity;

                if (verticalVelocity <= 0 && gamePlayerEntity.getCurrentLocation().getY() <= destination.getY()) {
                    jumpStarted = false;
                    deltaY = destination.getY() - gamePlayerEntity.getCurrentLocation().getY();
                }

                return deltaY;
            }

        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

        return future;
    }



    public CompletableFuture<Void> entityJump(GamePlayerEntity gamePlayerEntity, double jumpHeight) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        double jumpVelocity = 0.42 * jumpHeight;
        double gravity = 0.08 * jumpHeight;
        double totalTicks = (2 * jumpVelocity) / gravity;

        new BukkitRunnable() {
            private double ticksElapsed = 0;
            private double verticalVelocity = jumpVelocity;

            @Override
            public void run() {
                if (ticksElapsed >= totalTicks) {
                    this.cancel();
                    future.complete(null);
                    applyGravityToEntities(gamePlayerEntity, 0);
                    return;
                }

                double deltaY = getDeltaY();
                Location currentLocation = gamePlayerEntity.getCurrentLocation();
                Location newLocation = new Location(
                        currentLocation.getWorld(),
                        currentLocation.getX(),
                        currentLocation.getY() + deltaY,
                        currentLocation.getZ(),
                        currentLocation.getYaw(),
                        currentLocation.getPitch()
                );

                moveEntity(gamePlayerEntity, newLocation);
                ticksElapsed++;
            }

            private double getDeltaY() {
                double deltaY = verticalVelocity;
                verticalVelocity -= gravity;
                return deltaY;
            }
        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

        return future;
    }



    public CompletableFuture<Void> applyKnockBackToEntity(GamePlayerEntity gamePlayerEntity, Vector direction) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        direction = direction.normalize().multiply(-1);

        double kBS = knockBackStrength;
        double kBR = knockBackResistance;
        double kBHJ = knockBackJumpHeight;
        double resistanceFactor = 1.0 - (kBR / 100.0);
        kBS *= resistanceFactor;
        kBS = kBS / 2;

        double velocityX = -direction.getX() * kBS;
        double velocityY = kBS * kBHJ;
        double velocityZ = -direction.getZ() * kBS;

        short deltaXShort = (short) (velocityX * 8000);
        short deltaYShort = (short) (velocityY * 8000);
        short deltaZShort = (short) (velocityZ * 8000);

        double totalHeight = deltaYShort / 8000.0;

        double verticalHeightPerTick = totalHeight / 4.0;

        new BukkitRunnable() {
            private double ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= 5.0) {
                    this.cancel();
                    future.complete(null);
                    applyGravityToEntities(gamePlayerEntity, -0.4);
                    return;
                }

                double fraction = ticksElapsed / 5.0;

                double deltaX = deltaXShort * fraction / 8000.0;
                double deltaZ = deltaZShort * fraction / 8000.0;

                double deltaY = verticalHeightPerTick * ticksElapsed;

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
                ticksElapsed++;
            }
        }.runTaskTimerAsynchronously(MafanaEntityManager.getInstance(), 0L, 1L);

        return future;
    }

    public void moveEntity(GamePlayerEntity gamePlayerEntity, Location newLocation) {
        Location currentLocation = gamePlayerEntity.getCurrentLocation();
        double halfWidth = entityWidth / 2.0;
        double centerY = currentLocation.getY() + (entityHeight / 2.0);

        BoundingBox entityBB = BoundingBox.of(
                new Vector(currentLocation.getX(), centerY, currentLocation.getZ()),
                new Vector(currentLocation.getX(), centerY, currentLocation.getZ())
        ).expand(halfWidth, entityHeight / 2.0, halfWidth);

        double moveX = newLocation.getX() - currentLocation.getX();
        double moveZ = newLocation.getZ() - currentLocation.getZ();
        double moveY = newLocation.getY() - currentLocation.getY();

        double adjustedX = collisionUtil.adjustX(entityBB, moveX, currentLocation.getWorld());
        entityBB = entityBB.shift(adjustedX, 0, 0);

        double adjustedZ = collisionUtil.adjustZ(entityBB, moveZ, currentLocation.getWorld());
        entityBB = entityBB.shift(0, 0, adjustedZ);

        double adjustedY = collisionUtil.adjustY(entityBB, moveY, currentLocation.getWorld());
        entityBB = entityBB.shift(0, adjustedY, 0);

        double finalX = currentLocation.getX() + adjustedX;
        double finalY = currentLocation.getY() + adjustedY;
        double finalZ = currentLocation.getZ() + adjustedZ;

        Location adjustedLocation = new Location(
                currentLocation.getWorld(),
                finalX,
                finalY,
                finalZ,
                newLocation.getYaw(),
                newLocation.getPitch()
        );

        int entityID = gamePlayerEntity.getEntityID();
        CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
        ServerPlayer sp = craftPlayer.getHandle();
        ServerGamePacketListenerImpl ps = sp.connection;

        short deltaXShort = (short) (((finalX * 32) - (currentLocation.getX() * 32)) * 128);
        short deltaYShort = (short) (((finalY * 32) - (currentLocation.getY() * 32)) * 128);
        short deltaZShort = (short) (((finalZ * 32) - (currentLocation.getZ() * 32)) * 128);

        ClientboundMoveEntityPacket.PosRot movePacket = new ClientboundMoveEntityPacket.PosRot(
                entityID,
                deltaXShort,
                deltaYShort,
                deltaZShort,
                (byte) newLocation.getYaw(),
                (byte) newLocation.getPitch(),
                false
        );
        ps.send(movePacket);

        setLocation(adjustedLocation);
        gamePlayerEntity.setCurrentLocation(adjustedLocation);
    }


    public void damageAnimation(GamePlayerEntity gamePlayerEntity, float yaw) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            int entityID = gamePlayerEntity.getEntityID();
            CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
            ServerPlayer sp = craftPlayer.getHandle();
            ServerGamePacketListenerImpl ps = sp.connection;
            ClientboundHurtAnimationPacket animationPacket = new ClientboundHurtAnimationPacket(entityID, yaw);
            ps.send(animationPacket);
        });
    }

    public void entityDeathAnimation(GamePlayerEntity gamePlayerEntity) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            int entityID = gamePlayerEntity.getEntityID();
            CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
            ServerPlayer sp = craftPlayer.getHandle();
            ServerGamePacketListenerImpl ps = sp.connection;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(entityID);
            buf.writeByte((byte) 3);
            ps.send(new ClientboundEntityEventPacket(buf));
            killGenericSoundEffect(gamePlayerEntity);
            Bukkit.getScheduler().runTaskLaterAsynchronously(MafanaEntityManager.getInstance(), () -> {
                FriendlyByteBuf x = new FriendlyByteBuf(Unpooled.buffer());
                x.writeInt(entityID);
                x.writeByte((byte) 60);
                ps.send(new ClientboundEntityEventPacket(x));
                removeEntity(gamePlayerEntity);
            }, 20L);
        });
    }

    public void removeEntity(GamePlayerEntity gamePlayerEntity) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            int entityID = gamePlayerEntity.getEntityID();
            CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
            ServerPlayer sp = craftPlayer.getHandle();
            ServerGamePacketListenerImpl ps = sp.connection;
            ps.send(new ClientboundRemoveEntitiesPacket(entityID));
        });
    }

    public void hurtGenericSoundEffect(GamePlayerEntity gamePlayerEntity) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            ServerLevel craftWorld = ((CraftWorld) gamePlayerEntity.getCurrentLocation().getWorld()).getHandle();
            LivingEntity livingEntity = (LivingEntity) entityType.create(craftWorld.getLevel());

            List<Player> nearbyPlayers = new ArrayList<>();
            Bukkit.getScheduler().runTask(MafanaEntityManager.getInstance(), () -> nearbyPlayers.addAll(gamePlayerEntity.getCurrentLocation().getNearbyPlayers(damageSoundRange)));

            for (Player player : nearbyPlayers) {
                CraftPlayer craftPlayer = (CraftPlayer) player;
                if (livingEntity != null) {
                    craftPlayer.playSound(gamePlayerEntity.getCurrentLocation(), damageEntitySound, 1.0f, 1.0f);
                }
            }
        });
    }


    public void killGenericSoundEffect(GamePlayerEntity gamePlayerEntity) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            ServerLevel craftWorld = ((CraftWorld) gamePlayerEntity.getCurrentLocation().getWorld()).getHandle();
            LivingEntity livingEntity = (LivingEntity) entityType.create(craftWorld.getLevel());

            List<Player> nearbyPlayers = new ArrayList<>();
            Bukkit.getScheduler().runTask(MafanaEntityManager.getInstance(), () -> nearbyPlayers.addAll(gamePlayerEntity.getCurrentLocation().getNearbyPlayers(damageSoundRange)));
            for (Player player : nearbyPlayers) {
                CraftPlayer craftPlayer = (CraftPlayer) player;
                if (livingEntity != null) {
                    craftPlayer.playSound(gamePlayerEntity.getCurrentLocation(), deathSound, 1.0f, 1.0f);
                }
            }
        });
    }


    public void entitySwingAnimation(GamePlayerEntity gamePlayerEntity, int byteX) {
        Bukkit.getScheduler().runTaskAsynchronously(MafanaEntityManager.getInstance(), () -> {
            int entityID = gamePlayerEntity.getEntityID();
            CraftPlayer craftPlayer = (CraftPlayer) gamePlayerEntity.getPlayer();
            ServerPlayer sp = craftPlayer.getHandle();
            ServerGamePacketListenerImpl ps = sp.connection;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeInt(entityID);
            buf.writeByte((byte) byteX);
            ps.send(new ClientboundEntityEventPacket(buf));
        });
    }

    public void playerHurtEntity(Player player, GamePlayerEntity gamePlayerEntity, double damage) {
        hurtGenericSoundEffect(gamePlayerEntity);
        damageAnimation(gamePlayerEntity, player.getLocation().getYaw());
        applyKnockBackToEntity(gamePlayerEntity, player.getLocation().getDirection());
        if (damage > 0) {
            health = health - damage;
            if (health <= 0) {
                entityDeathAnimation(gamePlayerEntity);
            }
        }
    }

    public List<GamePlayerEntity> getGamePlayerEntityList() {
        return gamePlayerEntityList;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getHealth() {
        return health;
    }

    public double getGravityStrength() {
        return gravityStrength;
    }

    public double getSpeed() {
        return speed;
    }

    public double getJumpStrength() {
        return jumpStrength;
    }

    public double getKnockBackStrength() {
        return knockBackStrength;
    }

    public double getKnockBackJumpHeight() {
        return knockBackJumpHeight;
    }

    public double getKnockBackResistance() {
        return knockBackResistance;
    }

    public double getViewRange() {
        return viewRange;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public double getAmbientSoundRange() {
        return ambientSoundRange;
    }

    public double getDamageSoundRange() {
        return damageSoundRange;
    }

    public double getDeathSoundRange() {
        return deathSoundRange;
    }

    public double getGetDamagedByEntitySoundRange() {
        return getDamagedByEntitySoundRange;
    }

    public Sound getDamageEntitySound() {
        return damageEntitySound;
    }

    public Sound getGetDamagedByEntitySound() {
        return getDamagedByEntitySound;
    }

    public Sound getAmbientSound() {
        return ambientSound;
    }

    public Sound getDeathSound() {
        return deathSound;
    }

    public Location getLocation() {
        return location;
    }

    public double getX() {
        return location.getX();
    }

    public double getY() {
        return location.getY();
    }

    public double getZ() {
        return location.getZ();
    }

    public double getEntityHeight() {
        return entityHeight;
    }

    public double getEntityWidth() {
        return entityWidth;
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int) location.getX(), (int) location.getY(), (int) location.getZ());
    }

    public net.minecraft.world.level.Level getLevel() {
        ServerLevel craftWorld = ((CraftWorld) location.getWorld()).getHandle();
        return craftWorld.getLevel();
    }

    public void setGravityStrength(double gravityStrength) {
        this.gravityStrength = gravityStrength;
    }

    public void setGamePlayerEntityList(List<GamePlayerEntity> gamePlayerEntityList) {
        this.gamePlayerEntityList = gamePlayerEntityList;
    }

    public void setEntityHeight(double entityHeight) {
        this.entityHeight = entityHeight;
    }

    public void setEntityWidth(double entityWidth) {
        this.entityWidth = entityWidth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setJumpStrength(double jumpStrength) {
        this.jumpStrength = jumpStrength;
    }

    public void setKnockBackStrength(double knockBackStrength) {
        this.knockBackStrength = knockBackStrength;
    }

    public void setKnockBackJumpHeight(double knockBackJumpHeight) {
        this.knockBackJumpHeight = knockBackJumpHeight;
    }

    public void setKnockBackResistance(double knockBackResistance) {
        this.knockBackResistance = knockBackResistance;
    }

    public void setViewRange(double viewRange) {
        this.viewRange = viewRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = attackRange;
    }

    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public void setAmbientSoundRange(double ambientSoundRange) {
        this.ambientSoundRange = ambientSoundRange;
    }

    public void setDamageSoundRange(double damageSoundRange) {
        this.damageSoundRange = damageSoundRange;
    }

    public void setDeathSoundRange(double deathSoundRange) {
        this.deathSoundRange = deathSoundRange;
    }

    public void setGetDamagedByEntitySoundRange(double getDamagedByEntitySoundRange) {
        this.getDamagedByEntitySoundRange = getDamagedByEntitySoundRange;
    }

    public void setDamageEntitySound(Sound damageEntitySound) {
        this.damageEntitySound = damageEntitySound;
    }

    public void setJumpHighStrength(double jumpHighStrength) {
        this.jumpHighStrength = jumpHighStrength;
    }

    public void setJumpLengthStrength(double jumpLengthStrength) {
        this.jumpLengthStrength = jumpLengthStrength;
    }

    public void setGetDamagedByEntitySound(Sound getDamagedByEntitySound) {
        this.getDamagedByEntitySound = getDamagedByEntitySound;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isDescending() {
        return isDescending;
    }

    public void setDescending(boolean descending) {
        isDescending = descending;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void setAmbientSound(Sound ambientSound) {
        this.ambientSound = ambientSound;
    }

    public void setDeathSound(Sound deathSound) {
        this.deathSound = deathSound;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setCanStandOnFluids(boolean canStandOnFluids) {
        this.canStandOnFluids = canStandOnFluids;
    }

    public boolean isCanStandOnFluids() {
        return canStandOnFluids;
    }

    public boolean isGravity() {
        return gravity;
    }

    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }
}
