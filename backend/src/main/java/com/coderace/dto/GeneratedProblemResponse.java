package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for Gemini API problem generation response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedProblemResponse {

    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private List<String> constraints;
    private List<SampleTest> sampleTests;
    private List<HiddenTest> hiddenTests;
    private Integer difficulty;
    private List<String> tags;
    private Map<String, String> skeletonCode; // language -> starter code

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleTest {
        private String input;
        private String output;
        private String explanation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HiddenTest {
        private String input;
        private String output;
    }
}
