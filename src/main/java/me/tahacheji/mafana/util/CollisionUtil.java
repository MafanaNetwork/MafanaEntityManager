package me.tahacheji.mafana.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CollisionUtil {

    public double clipOffsetX(BoundingBox entityBB, BoundingBox blockBB, double offsetX) {
        BoundingBox testBB = entityBB.clone().shift(offsetX, 0, 0);

        if (!blockBB.overlaps(testBB)) {
            return offsetX;
        }

        if (offsetX > 0.0 && entityBB.getMaxX() <= blockBB.getMinX()) {
            double allowed = blockBB.getMinX() - entityBB.getMaxX();
            offsetX = Math.min(offsetX, allowed) - 0.0001;
        } else if (offsetX < 0.0 && entityBB.getMinX() >= blockBB.getMaxX()) {
            double allowed = blockBB.getMaxX() - entityBB.getMinX();
            offsetX = Math.max(offsetX, allowed) + 0.0001;
        }

        return offsetX;
    }

    public double clipOffsetY(BoundingBox entityBB, BoundingBox blockBB, double offsetY) {
        BoundingBox testBB = entityBB.clone().shift(0, offsetY, 0);

        if (!blockBB.overlaps(testBB)) {
            return offsetY;
        }

        if (offsetY > 0.0 && entityBB.getMaxY() <= blockBB.getMinY()) {
            double allowed = blockBB.getMinY() - entityBB.getMaxY();
            offsetY = Math.min(offsetY, allowed) - 0.0001;
        } else if (offsetY < 0.0 && entityBB.getMinY() >= blockBB.getMaxY()) {
            double allowed = blockBB.getMaxY() - entityBB.getMinY();
            offsetY = Math.max(offsetY, allowed) + 0.0001;
        }

        return offsetY;
    }

    public double clipOffsetZ(BoundingBox entityBB, BoundingBox blockBB, double offsetZ) {
        BoundingBox testBB = entityBB.clone().shift(0, 0, offsetZ);

        if (!blockBB.overlaps(testBB)) {
            return offsetZ;
        }

        if (offsetZ > 0.0 && entityBB.getMaxZ() <= blockBB.getMinZ()) {
            double allowed = blockBB.getMinZ() - entityBB.getMaxZ();
            offsetZ = Math.min(offsetZ, allowed) - 0.0001;
        } else if (offsetZ < 0.0 && entityBB.getMinZ() >= blockBB.getMaxZ()) {
            double allowed = blockBB.getMaxZ() - entityBB.getMinZ();
            offsetZ = Math.max(offsetZ, allowed) + 0.0001;
        }

        return offsetZ;
    }

    public double adjustX(BoundingBox entityBB, double offsetX, World world) {
        if (offsetX == 0.0) return 0.0;

        BoundingBox movedBB = entityBB.clone().shift(offsetX, 0, 0);
        List<BoundingBox> collisions = getNearbyCollisionBoxes(world, movedBB);

        for (BoundingBox blockBB : collisions) {
            offsetX = clipOffsetX(entityBB, blockBB, offsetX);
        }

        return offsetX;
    }

    public double adjustY(BoundingBox entityBB, double offsetY, World world) {
        if (offsetY == 0.0) return 0.0;

        BoundingBox movedBB = entityBB.clone().shift(0, offsetY, 0);
        List<BoundingBox> collisions = getNearbyCollisionBoxes(world, movedBB);

        for (BoundingBox blockBB : collisions) {
            offsetY = clipOffsetY(entityBB, blockBB, offsetY);
        }

        return offsetY;
    }

    public double adjustZ(BoundingBox entityBB, double offsetZ, World world) {
        if (offsetZ == 0.0) return 0.0;

        BoundingBox movedBB = entityBB.clone().shift(0, 0, offsetZ);
        List<BoundingBox> collisions = getNearbyCollisionBoxes(world, movedBB);

        for (BoundingBox blockBB : collisions) {
            offsetZ = clipOffsetZ(entityBB, blockBB, offsetZ);
        }

        return offsetZ;
    }

    public List<BoundingBox> getNearbyCollisionBoxes(World world, BoundingBox regionBB) {
        List<BoundingBox> boxes = new ArrayList<>();
        int minX = (int) Math.floor(regionBB.getMinX());
        int maxX = (int) Math.ceil(regionBB.getMaxX());
        int minY = (int) Math.floor(regionBB.getMinY());
        int maxY = (int) Math.ceil(regionBB.getMaxY());
        int minZ = (int) Math.floor(regionBB.getMinZ());
        int maxZ = (int) Math.ceil(regionBB.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isEmpty() && block.getType().isSolid()) {
                        BoundingBox blockBB = getBlockBoundingBox(block);
                        if (blockBB != null) {
                            boxes.add(blockBB);
                        }
                    }
                }
            }
        }
        return boxes;
    }

    public BoundingBox getBlockBoundingBox(Block block) {
        int bx = block.getX();
        int by = block.getY();
        int bz = block.getZ();
        return BoundingBox.of(
                new Vector(bx, by, bz),
                new Vector(bx + 1, by + 1, bz + 1)
        );
    }
}

