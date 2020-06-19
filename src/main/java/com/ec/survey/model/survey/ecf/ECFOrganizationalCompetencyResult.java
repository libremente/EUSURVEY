package com.ec.survey.model.survey.ecf;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the target result, maximum result and average result for all profiles or a specific one
 */
public class ECFOrganizationalCompetencyResult implements Comparable {

    @JsonProperty("competencyName")
    private String competencyName;
    
    @JsonProperty("competencyAverageTarget")
    private Float competencyAverageTarget;
    
    @JsonProperty("competencyAverageScore")
    private Float competencyAverageScore;

    @JsonProperty("competencyMaxTarget")
    private Integer competencyMaxTarget;

    @JsonProperty("competencyMaxScore")
    private Integer competencyMaxScore;
    
    private Integer order;

	public String getCompetencyName() {
		return competencyName;
	}

	public void setCompetencyName(String competencyName) {
		this.competencyName = competencyName;
	}

	public Float getCompetencyAverageTarget() {
		return competencyAverageTarget;
	}

	public void setCompetencyAverageTarget(Float competencyAverageTarget) {
		this.competencyAverageTarget = competencyAverageTarget;
	}

	public Float getCompetencyAverageScore() {
		return competencyAverageScore;
	}

	public void setCompetencyAverageScore(Float competencyAverageScore) {
		this.competencyAverageScore = competencyAverageScore;
	}

	public Integer getCompetencyMaxTarget() {
		return competencyMaxTarget;
	}

	public void setCompetencyMaxTarget(Integer competencyMaxTarget) {
		this.competencyMaxTarget = competencyMaxTarget;
	}

	public Integer getCompetencyMaxScore() {
		return competencyMaxScore;
	}

	public void setCompetencyMaxScore(Integer competencyMaxScore) {
		this.competencyMaxScore = competencyMaxScore;
	}
	

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}


	@Override
	public String toString() {
		return "ECFOrganizationalCompetencyResult [competencyName=" + competencyName + ", competencyAverageTarget="
				+ competencyAverageTarget + ", competencyAverageScore=" + competencyAverageScore
				+ ", competencyMaxTarget=" + competencyMaxTarget + ", competencyMaxScore=" + competencyMaxScore
				+ ", order=" + order + "]";
	}

	@Override
	public int compareTo(Object otherObject) {
		if (otherObject instanceof ECFOrganizationalCompetencyResult) {
			ECFOrganizationalCompetencyResult otherResult = (ECFOrganizationalCompetencyResult) otherObject;
			return this.getOrder().compareTo(otherResult.getOrder());
		} else {
			return 0;
		}
	}
}