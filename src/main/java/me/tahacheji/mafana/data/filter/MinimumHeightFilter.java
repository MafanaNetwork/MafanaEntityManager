package me.tahacheji.mafana.data.filter;

import de.metaphoriker.pathetic.api.pathing.filter.PathFilter;
import de.metaphoriker.pathetic.api.pathing.filter.PathValidationContext;
import de.metaphoriker.pathetic.api.wrapper.PathPosition;

public class MinimumHeightFilter implements PathFilter {

    private final int minHeight;

    public MinimumHeightFilter(int minHeight) {
        this.minHeight = minHeight;
    }

    @Override
    public boolean filter(PathValidationContext context) {
        PathPosition position = context.getPosition();
        return position.getBlockY() >= minHeight;
    }
}
