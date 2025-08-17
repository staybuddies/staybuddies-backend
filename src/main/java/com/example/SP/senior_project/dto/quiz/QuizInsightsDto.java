package com.example.SP.senior_project.dto.quiz;

import lombok.Data;
import java.util.List;

@Data
public class QuizInsightsDto {
    // Roommate Preferences tab
    private List<String> idealTraits;
    private List<String> preferredCharacteristics;
    private List<String> dealBreakers;

    // Compatibility Analysis tab
    private List<String> profileTags;
    private int averageCompatibility;
    private int bestCompatibility;
    private long highCount;     // >= 80%
    private long totalCompared; // number of other users compared
}
