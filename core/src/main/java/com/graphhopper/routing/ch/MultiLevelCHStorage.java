package com.graphhopper.routing.ch;

import com.graphhopper.storage.CHConfig;

import java.util.*;

/**
 * Multi-Level Contraction Hierarchies storage for planet-scale routing on low-RAM devices.
 * Organizes shortcuts by importance level: 0 (Local), 1 (Regional), 2+ (Global Backbone).
 */
public class MultiLevelCHStorage {
    private static final int GLOBAL_THRESHOLD = 3;
    private static final int REGIONAL_THRESHOLD = 1;

    private Map<Integer, CHShortcutBuffer> levelBuffers;
    private CHConfig config;
    private int[] nodeImportanceRanks;

    public MultiLevelCHStorage(CHConfig config, int numNodes) {
        this.config = config;
        this.nodeImportanceRanks = new int[numNodes];
        this.levelBuffers = new HashMap<>();

        // Initialize buffers for each level
        for (int i = 0; i <= config.getLevels(); i++) {
            levelBuffers.put(i, new CHShortcutBuffer());
        }
    }

    /**
     * Add a shortcut, automatically routing to the appropriate level based on importance rank.
     */
    public void addShortcut(int from, int to, double weight, int fromImportanceRank, int toImportanceRank) {
        int minRank = Math.min(fromImportanceRank, toImportanceRank);
        int level = calculateLevel(minRank);

        CHShortcutBuffer buffer = levelBuffers.get(level);
        buffer.add(from, to, weight, level);

        // Propagate upward to all higher levels (for global queries)
        for (int higherLevel = level + 1; higherLevel <= config.getLevels(); higherLevel++) {
            levelBuffers.get(higherLevel).add(from, to, weight, higherLevel);
        }
    }

    /**
     * Calculate which level this shortcut belongs to based on node importance.
     * 0=Local (residential), 1=Regional (tertiary), 2+=Global (motorway/backbone).
     */
    private int calculateLevel(int importanceRank) {
        if (importanceRank >= GLOBAL_THRESHOLD) return 2;
        if (importanceRank >= REGIONAL_THRESHOLD) return 1;
        return 0;
    }

    /**
     * Set the importance rank for a node (0=residential, 3=motorway).
     */
    public void setNodeImportance(int nodeId, int importanceRank) {
        if (nodeId < nodeImportanceRanks.length) {
            nodeImportanceRanks[nodeId] = importanceRank;
        }
    }

    /**
     * Get the importance rank of a node.
     */
    public int getNodeImportance(int nodeId) {
        if (nodeId < nodeImportanceRanks.length) {
            return nodeImportanceRanks[nodeId];
        }
        return 0;
    }

    /**
     * Get shortcuts for a specific level (used for level-specific queries).
     */
    public CHShortcutBuffer getBufferForLevel(int level) {
        return levelBuffers.get(Math.min(level, config.getLevels()));
    }

    /**
     * Get total shortcuts stored (across all levels).
     */
    public long getTotalShortcuts() {
        return levelBuffers.values().stream().mapToLong(CHShortcutBuffer::size).sum();
    }

    /**
     * Get shortcuts count for a specific level.
     */
    public long getShortcutsForLevel(int level) {
        CHShortcutBuffer buffer = levelBuffers.get(level);
        return buffer != null ? buffer.size() : 0;
    }

    /**
     * Internal buffer for storing shortcuts at a specific level.
     */
    public static class CHShortcutBuffer {
        private List<Shortcut> shortcuts = new ArrayList<>();

        public void add(int from, int to, double weight, int level) {
            shortcuts.add(new Shortcut(from, to, weight, level));
        }

        public List<Shortcut> getShortcuts() {
            return new ArrayList<>(shortcuts);
        }

        public long size() {
            return shortcuts.size();
        }

        public static class Shortcut {
            public final int from;
            public final int to;
            public final double weight;
            public final int level;

            public Shortcut(int from, int to, double weight, int level) {
                this.from = from;
                this.to = to;
                this.weight = weight;
                this.level = level;
            }

            @Override
            public String toString() {
                return String.format("Shortcut[%d->%d, weight=%.2f, level=%d]", from, to, weight, level);
            }
        }
    }
}
