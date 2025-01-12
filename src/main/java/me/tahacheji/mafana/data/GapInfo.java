package me.tahacheji.mafana.data;

public class GapInfo {
    private final boolean hasGap;
    private final double distance;
    private final double height;

    public GapInfo(boolean hasGap, double distance, double height) {
        this.hasGap = hasGap;
        this.distance = distance;
        this.height = height;
    }

    public boolean hasGap() {
        return hasGap;
    }

    public double getDistance() {
        return distance;
    }

    public double getHeight() {
        return height;
    }
}
