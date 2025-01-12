package me.tahacheji.mafana.data.filter;

import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.snapshot.SnapshotManager;
import de.metaphoriker.pathetic.api.wrapper.PathBlock;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;

public class GapTraversalFilter implements PathFilter {

    private final int jumpLengthInt;

    /**
     * Constructor to initialize the filter with a maximum jump length.
     *
     * @param jumpLengthInt The maximum distance the entity can jump across.
     */
    public GapTraversalFilter(int jumpLengthInt) {
        this.jumpLengthInt = jumpLengthInt;
    }

    /**
     * Determines if the current position is valid for traversal, considering gaps.
     *
     * @param context The context of the current pathfinding validation.
     * @return true if the entity can traverse the gap, false otherwise.
     */
    @Override
    public boolean filter(PathValidationContext context) {
        PathPosition currentPosition = context.getPosition();
        SnapshotManager snapshotManager = context.getSnapshotManager();

        if (!hasGround(snapshotManager.getBlock(currentPosition), snapshotManager)) {
            return canJumpAcrossGap(snapshotManager, currentPosition);
        }

        return true;
    }

    private boolean canJumpAcrossGap(SnapshotManager snapshotManager, PathPosition currentPosition) {
        for (int distance = 1; distance <= jumpLengthInt; distance++) {
            PathPosition checkPosition = currentPosition.add(distance, -1, 0);
            if (hasGround(snapshotManager.getBlock(checkPosition), snapshotManager)) {

                return true;
            }
        }
        return false;
    }
    private boolean hasGround(PathBlock block, SnapshotManager snapshotManager) {
        if (block == null) {
            return false;
        }
        PathBlock below = snapshotManager.getBlock(block.getPathPosition().add(0.0, -1.0, 0.0));
        return below != null && below.isSolid();
    }
}


