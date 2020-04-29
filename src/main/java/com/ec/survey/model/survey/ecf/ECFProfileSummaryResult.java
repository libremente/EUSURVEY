package com.ec.survey.model.survey.ecf;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ECFProfileSummaryResult {

	@JsonProperty("profileUid")
	private String profileUid;

	@JsonProperty("profileName")
	private String profileName;

	@JsonProperty("numberOfContributions")
	private Integer numberOfContributions;
	
	@JsonProperty("isSelected")
	private Boolean isSelected;

	public String getProfileUid() {
		return profileUid;
	}

	public void setProfileUid(String profileUid) {
		this.profileUid = profileUid;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public Integer getNumberOfContributions() {
		return numberOfContributions;
	}

	public void setNumberOfContributions(Integer numberOfContributions) {
		this.numberOfContributions = numberOfContributions;
	}
	
	public Boolean getIsSelected() {
		return isSelected;
	}

	public void setIsSelected(Boolean isSelected) {
		this.isSelected = isSelected;
	}

	@Override
	public String toString() {
		return "ECFProfileSummaryResult [profileUid=" + profileUid + ", profileName=" + profileName
				+ ", numberOfContributions=" + numberOfContributions + ", isSelected=" + isSelected + "]";
	}


}
