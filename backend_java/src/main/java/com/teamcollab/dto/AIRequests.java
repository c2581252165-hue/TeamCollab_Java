package com.teamcollab.dto;
import lombok.Data;
@Data
public class AIRequests {
    @Data public static class Breakdown { private String goal; private Integer projectId; }
    @Data public static class WeeklyReport { private Integer projectId; }
    @Data public static class Chat { private String message; }
    @Data public static class CodeReview { private Integer taskId; private String codeSnippet; }
    @Data public static class GradePrediction { private Integer projectId; }
    @Data public static class FormatStandup { private String rawText; }
    @Data public static class GenerateCode { private String prompt; private String filename; }
}
