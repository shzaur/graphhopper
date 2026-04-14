package com.graphhopper.routing.ch;

import com.graphhopper.storage.CHConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Partial contraction for Multi-Level Contraction Hierarchies.
 * Contracts nodes level-by-level based on importance rank rather than contracting all at once.
 */
public class PrepareContractionHierarchiesMLCH {

    private int[] nodeImportanceRanks;
    private MultiLevelCHStorage mlchStorage;
    private int totalNodes;
    private boolean[] contractedNodes;
    private CHConfig config;

    public PrepareContractionHierarchiesMLCH(int totalNodes, CHConfig config) {
        this.totalNodes = totalNodes;
        this.config = config;
        this.nodeImportanceRanks = new int[totalNodes];
        this.contractedNodes = new boolean[totalNodes];
        this.mlchStorage = new MultiLevelCHStorage(null, totalNodes);
    }

    /**
     * Initialize node importance ranks based on road class.
     * Motorway/Interstate = 3, Primary/Secondary = 2, Tertiary/Minor = 1, Residential = 0.
     */
    public void initializeNodeImportance(RoadClassAssigner assigner) {
        for (int nodeId = 0; nodeId < totalNodes; nodeId++) {
            nodeImportanceRanks[nodeId] = assigner.assignImportanceRank(nodeId);
            mlchStorage.setNodeImportance(nodeId, nodeImportanceRanks[nodeId]);
        }
    }

    /**
     * Run graph contraction with level-by-level stopping condition.
     * Stops when only "global backbone" nodes remain.
     */
    public void runGraphContractionWithLevels() {
        int currentImportanceLevel = 0;

        // Contract from lowest importance (residential) upward
        while (countActiveNodes() > config.getGlobalBackboneThreshold()) {
            contractNodesAtImportanceLevel(currentImportanceLevel);
            currentImportanceLevel++;
        }

        // Remaining nodes form the global backbone
    }

    /**
     * Contract only nodes of a specific importance level.
     */
    private void contractNodesAtImportanceLevel(int importanceLevel) {
        List<Integer> nodesToContract = new ArrayList<>();

        for (int nodeId = 0; nodeId < totalNodes; nodeId++) {
            if (!contractedNodes[nodeId] && nodeImportanceRanks[nodeId] == importanceLevel) {
                nodesToContract.add(nodeId);
            }
        }

        // Sort by contraction priority within this level (e.g., by degree)
        nodesToContract.sort((a, b) -> Integer.compare(
                estimateContractionCost(b),
                estimateContractionCost(a)
        ));

        for (int nodeId : nodesToContract) {
            contractNode(nodeId);
        }
    }

    /**
     * Contract a single node (create shortcuts to bypass it).
     */
    private void contractNode(int nodeId) {
        // Simplified contraction logic
        int importance = nodeImportanceRanks[nodeId];

        // Find all incoming/outgoing edges
        // For each pair of adjacent nodes, create a shortcut if it doesn't already exist
        // Add shortcut with appropriate level based on node importance

        contractedNodes[nodeId] = true;
    }

    /**
     * Estimate the cost of contracting a node (lower = contract first).
     * Used for ordering contractions within a level.
     */
    private int estimateContractionCost(int nodeId) {
        // Simple heuristic: number of shortcuts created
        return 0; // Placeholder
    }

    /**
     * Count nodes that have not yet been contracted.
     */
    private int countActiveNodes() {
        int count = 0;
        for (boolean contracted : contractedNodes) {
            if (!contracted) count++;
        }
        return count;
    }

    public MultiLevelCHStorage getMLCHStorage() {
        return mlchStorage;
    }

    /**
     * Interface for assigning importance ranks to nodes.
     */
    public interface RoadClassAssigner {
        int assignImportanceRank(int nodeId);
    }
}
