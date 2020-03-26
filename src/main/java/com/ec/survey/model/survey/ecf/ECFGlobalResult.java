package com.ec.survey.model.survey.ecf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the results for all answerers, and for a specific profile (possibly null)
 * If this is the case, do not display the individualResults gaps
 */
public class ECFGlobalResult {

    @JsonProperty("name")
    private String profileName;

    @JsonProperty("individualResults")
    private List<ECFGlobalCompetencyResult> individualResults = new ArrayList<>();
    
    @JsonProperty("pageNumber")
    private Integer pageNumber;
    
    @JsonProperty("pageSize")
    private Integer pageSize;
    
    @JsonProperty("numberOfPages")
    private Integer numberOfPages;
    
	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public List<ECFGlobalCompetencyResult> getIndividualResults() {
		return individualResults;
	}

	public void setIndividualResults(List<ECFGlobalCompetencyResult> individualResults) {
		this.individualResults = individualResults;
	}
	
	public void addIndividualResults(ECFGlobalCompetencyResult individualResult) {
		this.individualResults.add(individualResult);
	}
	

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getNumberOfPages() {
		return numberOfPages;
	}

	public void setNumberOfPages(Integer numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	@Override
	public String toString() {
		return "ECFGlobalResult [profileName=" + profileName + ", individualResults=" + individualResults
				+ ", pageNumber=" + pageNumber + ", pageSize=" + pageSize + ", numberOfPages=" + numberOfPages + "]";
	}

	
}