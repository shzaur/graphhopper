/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * MultiLevelCHStorage extends CHStorage to support Multi-Level Contraction Hierarchies (MLCH).
 * 
 * Instead of storing all shortcuts in a single flat array, MLCH segments shortcuts by importance level:
 * - Level 0 (Full Detail): Local level with complete road network and local shortcuts
 * - Level 1+ (Regional/Global): Higher levels with progressively fewer nodes and shortcuts
 * 
 * This allows low-RAM devices to load only the current local level and a global backbone,
 * avoiding the need to load the entire planet's shortcuts into memory.
 * 
 * @author MLCH Implementation
 */
public class MultiLevelCHStorage {
    private final CHStorage baseCHStorage;
    
    // Maps importance level to the shortcuts belonging to that level
    private final Map<Integer, LevelShortcutIndex> levelShortcutIndices;
    
    // Node importance ranks (0=local, higher=more important)
    private final int[] nodeImportanceRank;
    
    // The maximum importance level in this hierarchy
    private final int maxImportanceLevel;
    
    // Threshold for what constitutes a "Global Backbone" node
    private final int globalBackboneThreshold;

    /**
     * Creates a MultiLevelCHStorage from an existing CHStorage
     * @param baseCHStorage the underlying CHStorage
     * @param nodeImportanceRank array mapping node IDs to their importance rank (0=lowest)
     * @param globalBackboneThreshold minimum importance rank to be included in global backbone
     */
    public MultiLevelCHStorage(CHStorage baseCHStorage, int[] nodeImportanceRank, int globalBackboneThreshold) {
        this.baseCHStorage = baseCHStorage;
        this.nodeImportanceRank = nodeImportanceRank;
        this.globalBackboneThreshold = globalBackboneThreshold;
        this.levelShortcutIndices = new HashMap<>();
        
        // Calculate max importance level
        int maxRank = 0;
        for (int rank : nodeImportanceRank) {
            maxRank = Math.max(maxRank, rank);
        }
        this.maxImportanceLevel = maxRank;
        
        // Initialize level indices
        initializeLevelIndices();
    }

    /**
     * Indexes all shortcuts by their importance level based on the source node's rank
     */
    private void initializeLevelIndices() {
        for (int i = 0; i < baseCHStorage.getShortcuts(); i++) {
            long shortcutPtr = baseCHStorage.toShortcutPointer(i);
            int nodeA = baseCHStorage.getNodeA(shortcutPtr);
            int importance = nodeImportanceRank[nodeA];
            
            levelShortcutIndices.computeIfAbsent(importance, k -> new LevelShortcutIndex())
                    .addShortcut(i);
        }
    }

    /**
     * Gets the importance rank of a node
     * @param node the node ID
     * @return the importance rank (0=lowest, higher=more important)
     */
    public int getNodeImportanceRank(int node) {
        return nodeImportanceRank[node];
    }

    /**
     * Checks if a node is part of the global backbone
     * @param node the node ID
     * @return true if the node's importance rank >= globalBackboneThreshold
     */
    public boolean isGlobalBackboneNode(int node) {
        return getNodeImportanceRank(node) >= globalBackboneThreshold;
    }

    /**
     * Gets all shortcuts at a specific importance level
     * @param level the importance level
     * @return the index of shortcuts at this level, or empty index if none
     */
    public LevelShortcutIndex getShortcutsAtLevel(int level) {
        return levelShortcutIndices.getOrDefault(level, new LevelShortcutIndex());
    }

    /**
     * Gets all shortcuts from a given level and above (higher importance)
     * @param minLevel the minimum importance level
     * @return aggregated index of shortcuts at minLevel and above
     */
    public LevelShortcutIndex getShortcutsFromLevelAndAbove(int minLevel) {
        LevelShortcutIndex aggregated = new LevelShortcutIndex();
        for (int level = minLevel; level <= maxImportanceLevel; level++) {
            LevelShortcutIndex index = levelShortcutIndices.get(level);
            if (index != null) {
                aggregated.mergeWith(index);
            }
        }
        return aggregated;
    }

    /**
     * Gets the maximum importance level in this hierarchy
     */
    public int getMaxImportanceLevel() {
        return maxImportanceLevel;
    }

    /**
     * Gets the global backbone threshold
     */
    public int getGlobalBackboneThreshold() {
        return globalBackboneThreshold;
    }

    /**
     * Gets the underlying CHStorage
     */
    public CHStorage getBaseCHStorage() {
        return baseCHStorage;
    }

    /**
     * Index for shortcuts at a particular importance level
     */
    public static class LevelShortcutIndex {
        private final com.carrotsearch.hppc.IntArrayList shortcutIds = new com.carrotsearch.hppc.IntArrayList();

        public void addShortcut(int shortcutId) {
            shortcutIds.add(shortcutId);
        }

        public void mergeWith(LevelShortcutIndex other) {
            shortcutIds.addAll(other.shortcutIds);
        }

        public int[] getShortcutIds() {
            return shortcutIds.toArray();
        }

        public int size() {
            return shortcutIds.size();
        }

        public boolean isEmpty() {
            return shortcutIds.isEmpty();
        }
    }
}