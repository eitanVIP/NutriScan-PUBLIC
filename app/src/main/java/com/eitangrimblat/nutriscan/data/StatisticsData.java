package com.eitangrimblat.nutriscan.data;

/**
 * Data model for user nutrition statistics and AI-generated health insights.
 */
public class StatisticsData {
    public int score;
    public String nutrient1Value;
    public String nutrient2Value;
    public String nutrient3Value;
    public String insight1Title;
    public String insight1Content;
    public String insight2Title;
    public String insight2Content;
    public String insight3Title;
    public String insight3Content;
    public int specialScore;
    public String specialScoreTitle;

    /**
     * Initializes default statistics with zeroed values.
     */
    public StatisticsData() {
        score = 0;
        nutrient1Value = "0%";
        nutrient2Value = "0%";
        nutrient3Value = "0%";
        insight1Title = "";
        insight1Content = "";
        insight2Title = "";
        insight2Content = "";
        insight3Title = "";
        insight3Content = "";
        specialScore = 0;
        specialScoreTitle = "";
    }
}