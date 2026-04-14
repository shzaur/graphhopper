package com.graphhopper.storage;

import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;

import static com.graphhopper.config.Profile.validateProfileName;

/**
 * Container to hold properties used for CH preparation Specifies all properties of a CH routing profile.
 *
 * @author easbar
 */
public class CHConfig {
    /**
     * will be used to store and identify the CH graph data on disk
     */
    private final String chGraphName;
    private final Weighting weighting;
    private final boolean edgeBased;
    private int levels = 3; // 0: Local, 1: Regional, 2: Global
    private int globalBackboneThreshold = 100000; // Minimum nodes to form global backbone
    private boolean enableLevelAwareLoading = true;

    public static CHConfig nodeBased(String chGraphName, Weighting weighting) {
        return new CHConfig(chGraphName, weighting, false);
    }

    public static CHConfig edgeBased(String chGraphName, Weighting weighting) {
        return new CHConfig(chGraphName, weighting, true);
    }

    public CHConfig(String chGraphName, Weighting weighting, boolean edgeBased) {
        validateProfileName(chGraphName);
        this.chGraphName = chGraphName;
        this.weighting = weighting;
        this.edgeBased = edgeBased;
    }

    public Weighting getWeighting() {
        return weighting;
    }

    public boolean isEdgeBased() {
        return edgeBased;
    }

    public TraversalMode getTraversalMode() {
        return edgeBased ? TraversalMode.EDGE_BASED : TraversalMode.NODE_BASED;
    }

    public String toFileName() {
        return chGraphName;
    }

    public int getLevels() {
        return levels;
    }

    public CHConfig setLevels(int levels) {
        if (levels < 1) throw new IllegalArgumentException("Levels must be >= 1");
        this.levels = levels;
        return this;
    }

    /**
     * Minimum number of nodes remaining when forming global backbone.
     * Once contraction reduces nodes below this, contraction stops and global level is finalized.
     */
    public int getGlobalBackboneThreshold() {
        return globalBackboneThreshold;
    }

    public CHConfig setGlobalBackboneThreshold(int threshold) {
        if (threshold < 1000) throw new IllegalArgumentException("Threshold must be >= 1000");
        this.globalBackboneThreshold = threshold;
        return this;
    }

    /**
     * Enable/disable level-aware storage loading (mmap with intelligent paging).
     */
    public boolean isLevelAwareLoadingEnabled() {
        return enableLevelAwareLoading;
    }

    public CHConfig setLevelAwareLoadingEnabled(boolean enabled) {
        this.enableLevelAwareLoading = enabled;
        return this;
    }

    public String toString() {
        return chGraphName;
    }

    public String getName() {
        return chGraphName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CHConfig chConfig = (CHConfig) o;
        return getName().equals(chConfig.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
