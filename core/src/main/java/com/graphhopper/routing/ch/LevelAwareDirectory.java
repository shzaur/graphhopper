package com.graphhopper.routing.ch;

import java.util.*;

/**
 * Level-aware directory for managing MLCH storage with intelligent RAM paging.
 * Keeps only the active level in RAM, using mmap for disk-based storage of other levels.
 */
public class LevelAwareDirectory {

    private Map<Integer, LevelStorage> levelStorages;
    private int currentActiveLevel = 0;
    private long maxRamPerLevel;
    private Map<Integer, Long> levelSizes;

    public LevelAwareDirectory(long maxRamPerLevel) {
        this.maxRamPerLevel = maxRamPerLevel;
        this.levelStorages = new HashMap<>();
        this.levelSizes = new HashMap<>();
    }

    /**
     * Register storage for a specific level.
     */
    public void registerLevelStorage(int level, LevelStorage storage) {
        levelStorages.put(level, storage);
    }

    /**
     * Switch active level and optimize memory usage.
     * Drops cache for non-active levels using madvise semantics.
     */
    public void setActiveLevel(int level) {
        if (this.currentActiveLevel == level) {
            return; // Already active
        }

        // Drop cache for previously active level
        if (levelStorages.containsKey(currentActiveLevel)) {
            dropCacheForLevel(currentActiveLevel);
        }

        // Ensure new level is cached
        if (levelStorages.containsKey(level)) {
            ensureLevelCached(level);
        }

        this.currentActiveLevel = level;
    }

    /**
     * Mark a level's memory as droppable (MADV_DONTNEED).
     * This signals the OS that this memory can be reclaimed if needed.
     */
    private void dropCacheForLevel(int level) {
        LevelStorage storage = levelStorages.get(level);
        if (storage != null) {
            storage.dropFromCache();
        }
    }

    /**
     * Ensure a level is fully cached in RAM.
     */
    private void ensureLevelCached(int level) {
        LevelStorage storage = levelStorages.get(level);
        if (storage != null && storage.size() <= maxRamPerLevel) {
            storage.loadIntoCache();
        }
    }

    /**
     * Get the current active level.
     */
    public int getActiveLevel() {
        return currentActiveLevel;
    }

    /**
     * Get storage for a specific level.
     */
    public LevelStorage getLevelStorage(int level) {
        return levelStorages.get(level);
    }

    /**
     * Get memory statistics across all levels.
     */
    public MemoryStatistics getMemoryStatistics() {
        MemoryStatistics stats = new MemoryStatistics();
        for (Map.Entry<Integer, LevelStorage> entry : levelStorages.entrySet()) {
            stats.recordLevel(entry.getKey(), entry.getValue().size());
        }
        stats.setActiveLevel(currentActiveLevel);
        return stats;
    }

    /**
     * Storage interface for a level.
     */
    public interface LevelStorage {
        long size();
        void loadIntoCache();
        void dropFromCache();
        byte[] readShortcuts(int offset, int length);
    }

    /**
     * Memory usage statistics.
     */
    public static class MemoryStatistics {
        private Map<Integer, Long> levelSizes = new HashMap<>();
        private int activeLevel = -1;

        private void recordLevel(int level, long size) {
            levelSizes.put(level, size);
        }

        private void setActiveLevel(int level) {
            this.activeLevel = level;
        }

        public long getTotalSize() {
            return levelSizes.values().stream().mapToLong(Long::longValue).sum();
        }

        public long getSizeForLevel(int level) {
            return levelSizes.getOrDefault(level, 0L);
        }

        public int getActiveLevel() {
            return activeLevel;
        }

        @Override
        public String toString() {
            return String.format("MemoryStatistics[total=%d bytes, activeLevel=%d]", getTotalSize(), activeLevel);
        }
    }
}
