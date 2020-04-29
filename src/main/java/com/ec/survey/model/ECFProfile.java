package com.ec.survey.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Represents a profile
 */
@Entity
@Table(name = "ECF_PROFILE")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ECFProfile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int id;
	private String profileUid;
	private String description;
	private String name;
	private List<ECFExpectedScore> expectedScores = new ArrayList<>();

	protected static final Logger logger = Logger.getLogger(ECFProfile.class);

	private ECFProfile() {

	}

	public ECFProfile(String profileUid, String name, String description) {
		this.setProfileUid(profileUid);
		this.setDescription(description);
		this.setName(name);
	}

	@Id
	@Column(name = "PROFILE_ID")
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "PROFILE_UID")
	public String getProfileUid() {
		return profileUid;
	}

	public void setProfileUid(String profileUid) {
		this.profileUid = profileUid;
	}

	@Column(name = "PROFILE_DESC")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "PROFILE_NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	@OneToMany(mappedBy = "id.profile", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(value = FetchMode.SELECT)
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public List<ECFExpectedScore> getECFExpectedScores() {
		return expectedScores;
	}

	public void setECFExpectedScores(List<ECFExpectedScore> expectedScores) {
		this.expectedScores = expectedScores;
	}

	public void addECFExpectedScore(ECFExpectedScore expectedScore) {
		this.expectedScores.add(expectedScore);
	}
	
	public ECFProfile replaceScore(ECFCompetency previousScoreId, ECFExpectedScore copiedScore) {
		this.setECFExpectedScores(this.expectedScores.stream().map(expectedScore -> {
			if (expectedScore.getECFExpectedScoreToProfileEid().getECFCompetency().equals(previousScoreId)) {
				return copiedScore;
			} else {
				return expectedScore;
			}
		}).collect(Collectors.toList()));
		return this;
	}

	public ECFProfile copy() {
		ECFProfile profileCopy = new ECFProfile(UUID.randomUUID().toString(),
				this.getName(), this.getDescription());
		return profileCopy;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((profileUid == null) ? 0 : profileUid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ECFProfile other = (ECFProfile) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (profileUid == null) {
			if (other.profileUid != null)
				return false;
		} else if (!profileUid.equals(other.profileUid))
			return false;
		return true;
	}

}
