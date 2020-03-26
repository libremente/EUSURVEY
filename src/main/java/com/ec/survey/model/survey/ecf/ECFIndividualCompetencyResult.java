package com.ec.survey.model.survey.ecf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ECFIndividualCompetencyResult implements Comparable {

    @JsonProperty("name")
    private String competencyName;

    @JsonProperty("score")
    private Integer competencyScore;

    @JsonProperty("targetScore")
    private Integer competencyTargetScore;

    @JsonProperty("scoreGap")
    private Integer competencyScoreGap;

    @JsonIgnore
    private List<Integer> questionsScores = new ArrayList<>();

    public String getCompetencyName() {
        return competencyName;
    }

    public void setCompetencyName(String competencyName) {
        this.competencyName = competencyName;
    }

    public void addCompetencyScore(Integer oneQuestionScore) {
        this.questionsScores.add(oneQuestionScore);
    }

    public Integer getCompetencyScore() {
        Integer sum = 0;
        for (Integer questionScore : questionsScores) {
            sum += questionScore;
        }
        this.competencyScore = sum / questionsScores.size();
        return competencyScore;
    }

    public void setCompetencyTargetScore(Integer competencyTargetScore) {
        this.competencyTargetScore = competencyTargetScore;
    }

    public Integer getCompetencyTargetScore() {
        return competencyTargetScore;
    }

    public Integer getCompetencyScoreGap() {
        this.competencyScoreGap = this.competencyScore - this.competencyTargetScore; 
        return this.competencyScoreGap;
    }

	@Override
	public String toString() {
		return "ECFIndividualCompetencyResult [competencyName=" + competencyName + ", competencyScore="
				+ competencyScore + ", competencyTargetScore=" + competencyTargetScore + ", competencyScoreGap="
				+ competencyScoreGap + ", questionsScores=" + questionsScores + "]";
	}

	@Override
	public int compareTo(Object otherObject) {
		if (otherObject instanceof ECFIndividualCompetencyResult) {
			ECFIndividualCompetencyResult otherResult = (ECFIndividualCompetencyResult) otherObject;
			return this.getCompetencyName().compareTo(otherResult.getCompetencyName());
		} else {
			return 0;
		}
	}

}