package com.coderace.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a Codeforces problem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    private String contestId;
    private String index;
    private String name;
    private String type;
    private Integer rating;
    private List<String> tags;
    
    /**
     * Returns the full problem URL on Codeforces
     */
    public String getProblemUrl() {
        return "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
    }
    
    /**
     * Returns a unique identifier for this problem
     */
    public String getProblemId() {
        return contestId + index;
    }
}
