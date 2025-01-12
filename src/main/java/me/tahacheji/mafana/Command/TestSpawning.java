package me.tahacheji.mafana.Command;

import me.tahacheji.mafana.MafanaEntityManager;
import me.tahacheji.mafana.commandExecutor.Command;
import me.tahacheji.mafana.commandExecutor.paramter.Param;
import me.tahacheji.mafana.data.GameEntity;
import me.tahacheji.mafana.data.GamePlayerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TestSpawning {

    @Command(names = "mem moveTo", permission = "")
    public void spawn(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().moveEntityToLocation(g, player.getLocation());
        }
    }

    @Command(names = "mem setGravityStrength", permission = "")
    public void gravity(Player player, @Param(name = "strength") double strength) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setGravityStrength(strength);
        }
    }

    @Command(names = "mem knockBack", permission = "")
    public void knockBack(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().applyKnockBackToEntity(g, player.getLocation().getDirection());
        }
    }

    @Command(names = "mem setSounds", permission = "")
    public void setSounds(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setSounds(player.getLocation());
        }
    }

    @Command(names = "mem setKnockBack", permission = "")
    public void setKnockBack(Player player, @Param(name = "knockBackStrength") double knockBackStrength,
                             @Param(name = "knockBackJumpHeight") double knockBackJumpHeight,
                             @Param(name = "knockBackResistance") double knockBackResistance) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setKnockBackResistance(knockBackResistance);
            MafanaEntityManager.getInstance().getGameEntity().setKnockBackJumpHeight(knockBackJumpHeight);
            MafanaEntityManager.getInstance().getGameEntity().setKnockBackStrength(knockBackStrength);
        }
    }

    @Command(names = "mem hurt", permission = "")
    public void hurt(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().damageAnimation(g, player.getLocation().getYaw());
        }
    }

    @Command(names = "mem print", permission = "")
    public void print(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            player.sendMessage(g.getCurrentLocation().toString());
        }
    }

    @Command(names = "mem hurtSound", permission = "")
    public void hurtSound(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().hurtGenericSoundEffect(g);
        }
    }

    @Command(names = "mem jumpForward", permission = "")
    public void jumpForward(Player player, @Param(name = "jumpStrength") double jumpStrength, @Param(name = "speed") double speed) {
        for (GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            Location currentLocation = g.getCurrentLocation();
            Location destination = currentLocation.clone().add(-1, 1, 0);

            // Check if the destination is the same as the current location
            if (currentLocation.equals(destination)) {
                player.sendMessage("The destination is the same as the current location. Adjusting the destination.");
                destination.add(0.1, 0, 0.1); // Slightly adjust the destination
            }

            MafanaEntityManager.getInstance().getGameEntity().entityJumpForwardToLocation(g, destination, jumpStrength, speed);
        }
    }


    @Command(names = "mem setHealth", permission = "")
    public void setHealth(Player player, @Param(name = "health") double health) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setHealth(health);
        }
    }

    @Command(names = "mem setGravityOff", permission = "")
    public void gravityOff(Player player, @Param(name = "boolean") boolean b) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setGravity(b);
        }
    }

    @Command(names = "mem setJump", permission = "")
    public void setJump(Player player, @Param(name = "height") double height, @Param(name = "length") double length) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().setJumpHighStrength(height);
            MafanaEntityManager.getInstance().getGameEntity().setJumpLengthStrength(length);
        }
    }

    @Command(names = "mem remove", permission = "")
    public void remove(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().removeEntity(g);
        }
    }

    @Command(names = "mem spawn", permission = "")
    public void spawn(Player player, @Param(name = "loc") Location location) {
        location.setWorld(player.getWorld());
        MafanaEntityManager.getInstance().getGameEntity().spawnClientSideEntity(player, location);
    }


    @Command(names = "mem jump", permission = "")
    public void jump(Player player, @Param(name = "strength") double strength) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().entityJump(g, strength);
        }
    }

    @Command(names = "mem animation", permission = "")
    public void animation(Player player, @Param(name = "animation") double animation) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().entitySwingAnimation(g, (int) animation);
        }
    }


    @Command(names = "mem moveTest", permission = "")
    public void moveTest(Player player, @Param(name = "x") int x) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    MafanaEntityManager.getInstance().getGameEntity().moveEntityToLocation(g, g.getCurrentLocation().clone().add(x, 0, 0));
                }
            }.runTaskAsynchronously(MafanaEntityManager.getInstance());
        }
    }

    @Command(names = "mem death", permission = "")
    public void death(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().entityDeathAnimation(g);
        }
    }

    @Command(names = "mem tp", permission = "")
    public void teleport(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().teleportEntity(g, player.getLocation(), true);
        }
    }

    @Command(names = "mem path", permission = "")
    public void path(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().moveEntityWithPatheticMapper(g, player.getLocation());
        }
    }

    @Command(names = "mem pathOne", permission = "")
    public void pathOne(Player player) {
        for(GameEntity g : MafanaEntityManager.getInstance().getGameEntityList()) {
            g.moveEntityWithPatheticMapper(g.getGamePlayerEntityList().get(0), player.getLocation());
            //g.getGamePlayerEntityList().forEach(gamePlayerEntity -> MafanaEntityManager.getInstance().getGameEntity().moveEntityWithPatheticMapper(gamePlayerEntity, player.getLocation()));
        }
    }

    @Command(names = "mem pathT", permission = "")
    public void pathT(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().moveEntityWithPatheticMapper(g, new Location(Bukkit.getWorld("world"), -12.3,1,10.5));
        }
    }

    @Command(names = "mem follow", permission = "")
    public void follow(Player player) {
        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            MafanaEntityManager.getInstance().getGameEntity().startFollowingPlayer(g, player);
        }
    }

    @Command(names = "mem pathJump", permission = "")
    public void pathJump(Player player) {

        for(GamePlayerEntity g : MafanaEntityManager.getInstance().getGameEntity().getGamePlayerEntityList()) {
            List<Location> x = new ArrayList<>();
            x.add(g.getCurrentLocation().clone().add(1, 1, 0));
            x.add(g.getCurrentLocation().clone().add(2, 1, 0));
            x.add(g.getCurrentLocation().clone().add(3, 1, 0));
            x.add(g.getCurrentLocation().clone().add(4, 1, 0));
            x.add(g.getCurrentLocation().clone().add(5, 1, 0));
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (Location l : x) {
                chain = chain.thenCompose(v -> MafanaEntityManager.getInstance().getGameEntity().moveEntityToLocation(g, l));
            }
        }
    }


}
