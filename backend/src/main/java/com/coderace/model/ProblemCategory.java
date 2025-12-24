package com.coderace.model;

import java.util.List;

/**
 * Enum representing different competitive programming problem categories
 * Each category has predefined subtypes for more specific problem generation
 */
public enum ProblemCategory {
    DYNAMIC_PROGRAMMING("Dynamic Programming", List.of(
            "Knapsack Variants",
            "Longest Common Subsequence",
            "String DP",
            "State Machine DP",
            "2D Grid DP",
            "Interval DP")),

    GRAPHS("Graphs", List.of(
            "Shortest Path",
            "Minimum Spanning Tree",
            "Topological Sort",
            "Connected Components",
            "Cycle Detection",
            "BFS/DFS Traversal")),

    TREES("Trees", List.of(
            "Binary Search Tree",
            "Tree Traversal",
            "Lowest Common Ancestor",
            "Path Sum Problems",
            "Tree DP")),

    GREEDY("Greedy", List.of(
            "Interval Scheduling",
            "Activity Selection",
            "Huffman Coding",
            "Job Sequencing")),

    BINARY_SEARCH("Binary Search", List.of(
            "Search in Sorted Array",
            "Binary Search on Answer",
            "Element Finding",
            "Lower/Upper Bound")),

    SORTING_SEARCHING("Sorting & Searching", List.of(
            "Custom Comparators",
            "Two Pointers",
            "Sliding Window",
            "Merge Sort Applications")),

    ARRAYS_HASHING("Arrays & Hashing", List.of(
            "Prefix Sum",
            "Kadane's Algorithm",
            "Hash Map Techniques",
            "Subarray Problems")),

    BACKTRACKING("Backtracking", List.of(
            "N-Queens Variants",
            "Subset Generation",
            "Permutations",
            "Combination Sum"));

    private final String displayName;
    private final List<String> subtypes;

    ProblemCategory(String displayName, List<String> subtypes) {
        this.displayName = displayName;
        this.subtypes = subtypes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getSubtypes() {
        return subtypes;
    }
}
