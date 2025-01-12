package me.tahacheji.mafana.data.filter;

import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.snapshot.SnapshotManager;
import de.metaphoriker.pathetic.api.wrapper.PathBlock;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;

public class NavigateAroundObstacleFilter implements PathFilter {

    @Override
    public boolean filter(PathValidationContext context) {
        PathPosition currentPosition = context.getPosition();
        PathPosition parentPosition = context.getParent();
        SnapshotManager snapshotManager = context.getSnapshotManager();

        if (isDiagonalMovement(currentPosition, parentPosition)) {
            PathPosition straightX = new PathPosition(context.getAbsoluteStart().getPathEnvironment(),parentPosition.getBlockX(), currentPosition.getBlockY(), currentPosition.getBlockZ());
            PathPosition straightZ = new PathPosition(context.getAbsoluteStart().getPathEnvironment(),currentPosition.getBlockX(), currentPosition.getBlockY(), parentPosition.getBlockZ());

            boolean xBlocked = isObstructed(snapshotManager, straightX);
            boolean zBlocked = isObstructed(snapshotManager, straightZ);

            return !xBlocked && !zBlocked;
        }

        return true;
    }

    private boolean isDiagonalMovement(PathPosition current, PathPosition parent) {
        int deltaX = Math.abs(current.getBlockX() - parent.getBlockX());
        int deltaZ = Math.abs(current.getBlockZ() - parent.getBlockZ());
        return deltaX == 1 && deltaZ == 1; // Movement is diagonal if both X and Z change
    }

    private boolean isObstructed(SnapshotManager snapshotManager, PathPosition position) {
        PathBlock block = snapshotManager.getBlock(position);
        return block != null && block.isSolid();
    }
}


