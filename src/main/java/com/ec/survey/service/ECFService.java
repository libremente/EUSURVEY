package com.ec.survey.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.survey.exception.ECFException;
import com.ec.survey.model.Answer;
import com.ec.survey.model.AnswerSet;
import com.ec.survey.model.ECFCluster;
import com.ec.survey.model.ECFCompetency;
import com.ec.survey.model.ECFExpectedScore;
import com.ec.survey.model.ECFExpectedScoreToProfileEid;
import com.ec.survey.model.ECFProfile;
import com.ec.survey.model.ECFType;
import com.ec.survey.model.ResultFilter;
import com.ec.survey.model.SqlPagination;
import com.ec.survey.model.survey.ChoiceQuestion;
import com.ec.survey.model.survey.Element;
import com.ec.survey.model.survey.PossibleAnswer;
import com.ec.survey.model.survey.Question;
import com.ec.survey.model.survey.SingleChoiceQuestion;
import com.ec.survey.model.survey.Survey;
import com.ec.survey.model.survey.ecf.ECFGlobalCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFGlobalResult;
import com.ec.survey.model.survey.ecf.ECFGlobalTotalResult;
import com.ec.survey.model.survey.ecf.ECFIndividualCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFIndividualResult;
import com.ec.survey.model.survey.ecf.ECFOrganizationalCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFOrganizationalResult;
import com.ec.survey.model.survey.ecf.ECFProfileCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFProfileResult;
import com.ec.survey.model.survey.ecf.ECFProfileSummaryResult;
import com.ec.survey.model.survey.ecf.ECFSummaryResult;
import com.google.common.primitives.Ints;

@Service("ecfService")
@Configurable
public class ECFService extends BasicService {

	private static final Logger logger = Logger.getLogger(ECFService.class);

	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	public ECFSummaryResult getECFSummaryResult(Survey survey) throws Exception {
		ECFSummaryResult ecfSummaryResult = new ECFSummaryResult();
		List<AnswerSet> answerSets = this.answerService.getAllAnswers(survey.getId(), null);

		Set<ECFProfile> profiles = this.getECFProfiles(survey);
		Map<String, String> profileUidToName = new HashMap<>();
		Map<String, Integer> profileToNumber = new HashMap<>();

		for (ECFProfile profile : profiles) {
			profileUidToName.put(profile.getProfileUid(), profile.getName());
			profileToNumber.put(profile.getProfileUid(), 0);
		}

		for (AnswerSet answerSet : answerSets) {
			String profileUid = this.getECFProfile(survey, answerSet).getProfileUid();
			if (profileToNumber.containsKey(profileUid)) {
				Integer previousNumber = profileToNumber.get(profileUid);
				profileToNumber.put(profileUid, previousNumber + 1);
			} else {
				throw new ECFException("An answerset references a non existing profile : " + profileUid);
			}
		}

		Integer totalContributions = 0;
		for (String profileUid : profileToNumber.keySet()) {
			ECFProfileSummaryResult ecfSummaryProfileResult = new ECFProfileSummaryResult();
			ecfSummaryProfileResult.setProfileName(profileUidToName.get(profileUid));
			ecfSummaryProfileResult.setNumberOfContributions(profileToNumber.get(profileUid));
			ecfSummaryProfileResult.setProfileUid(profileUid);
			ecfSummaryResult.addProfileResult(ecfSummaryProfileResult);

			totalContributions = totalContributions + ecfSummaryProfileResult.getNumberOfContributions();
		}

		ECFProfileSummaryResult ecfSummaryProfileResult = new ECFProfileSummaryResult();
		ecfSummaryProfileResult.setProfileName("All job profile");
		ecfSummaryProfileResult.setNumberOfContributions(totalContributions);
		ecfSummaryProfileResult.setIsSelected(true);
		ecfSummaryResult.addProfileResult(ecfSummaryProfileResult);

		return ecfSummaryResult;
	}

	public ECFOrganizationalResult getECFOrganizationalResult(Survey survey) throws Exception {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}

		ECFOrganizationalResult ecfOrganizationalResult = new ECFOrganizationalResult();

		Map<ECFProfile, Map<ECFCompetency, Integer>> profilesToExpectedScores = getProfilesExpectedScores(
				this.getECFProfiles(survey));
		Map<ECFCompetency, Integer> competencyToMaxTarget = new HashMap<>();

		Map<ECFCompetency, Integer> competencyToTotalTarget = new HashMap<>();
		Map<ECFCompetency, Integer> competencyToNumberTarget = new HashMap<>();

		for (ECFProfile profile : profilesToExpectedScores.keySet()) {
			Map<ECFCompetency, Integer> competencyToTarget = profilesToExpectedScores.get(profile);
			for (ECFCompetency competency : competencyToTarget.keySet()) {
				Integer target = competencyToTarget.get(competency);

				if (competencyToTotalTarget.containsKey(competency)) {
					Integer previousTotalTarget = competencyToTotalTarget.get(competency);
					competencyToTotalTarget.put(competency, previousTotalTarget + target);
				} else {
					competencyToTotalTarget.put(competency, target);
				}

				if (competencyToNumberTarget.containsKey(competency)) {
					Integer previousNumberTarget = competencyToNumberTarget.get(competency);
					competencyToNumberTarget.put(competency, previousNumberTarget + 1);
				} else {
					competencyToNumberTarget.put(competency, 1);
				}

				if (competencyToMaxTarget.containsKey(competency)) {
					Integer previousMaxTarget = competencyToMaxTarget.get(competency);
					if (target > previousMaxTarget) {
						competencyToMaxTarget.put(competency, target);
					}
				} else {
					competencyToMaxTarget.put(competency, target);
				}
			}
		}

		List<AnswerSet> answerSets = this.answerService.getAllAnswers(survey.getId(), null);
		Map<ECFCompetency, List<Integer>> competenciesToScores = this.getCompetenciesToScores(survey, answerSets);

		for (ECFCompetency competency : competencyToMaxTarget.keySet()) {
			ECFOrganizationalCompetencyResult competencyResult = new ECFOrganizationalCompetencyResult();
			competencyResult.setCompetencyName(competency.getName());
			competencyResult.setOrder(competency.getOrderNumber());

			competencyResult.setCompetencyMaxTarget(competencyToMaxTarget.get(competency));

			Integer totalTarget = competencyToTotalTarget.get(competency);
			Integer numberOfTargets = competencyToNumberTarget.get(competency);
			Float averageTarget = this.roundedAverage(totalTarget, numberOfTargets);
			competencyResult.setCompetencyAverageTarget(averageTarget);

			List<Integer> competencyScores = competenciesToScores.get(competency);
			if (competencyScores.size() > 0) {
				Integer competencyMaxScore = competencyScores.stream().mapToInt(i -> i).max()
						.orElseThrow(NoSuchElementException::new);
				competencyResult.setCompetencyMaxScore(competencyMaxScore);

				Integer competencySumScores = competencyScores.stream().reduce(0, Integer::sum);
				Float competencyAverageScore = this.roundedAverage(competencySumScores, competencyScores.size());
				competencyResult.setCompetencyAverageScore(competencyAverageScore);
			}

			ecfOrganizationalResult.addCompetencyResult(competencyResult);
		}

		ecfOrganizationalResult.setCompetencyResults(
				ecfOrganizationalResult.getCompetencyResults().stream().sorted().collect(Collectors.toList()));

		return ecfOrganizationalResult;
	}

	public ECFProfileResult getECFProfileResult(Survey survey) throws Exception {
		return this.getECFProfileResult(survey, null);
	}

	/**
	 * Returns the ECFProfileResult for the given Survey, Pagination, and optional
	 * EcfProfile
	 */
	public ECFProfileResult getECFProfileResult(Survey survey, ECFProfile ecfProfile) throws Exception {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}

		ECFProfileResult ecfProfileResult = null;

		List<AnswerSet> answerSets = this.answerService.getAllAnswers(survey.getId(), null);

		if (ecfProfile != null) {
			List<AnswerSet> profileFilteredAnswerSets = new ArrayList<>();
			for (AnswerSet answer : answerSets) {
				if (this.getECFProfile(survey, answer).equals(ecfProfile)) {
					profileFilteredAnswerSets.add(answer);
				}
			}
			answerSets = profileFilteredAnswerSets;

			Map<ECFCompetency, Integer> competencyToExpectedScore = this.getProfileExpectedScores(ecfProfile);

			ecfProfileResult = this.getECFProfileCompetencyResult(survey, answerSets, competencyToExpectedScore,
					ecfProfile.getName());
		} else {
			ecfProfileResult = this.getECFProfileCompetencyResult(survey, answerSets);
		}

		ecfProfileResult.setCompetencyResults(
				ecfProfileResult.getCompetencyResults().stream().sorted().collect(Collectors.toList()));
		return ecfProfileResult;
	}

	public ECFProfileResult getECFProfileCompetencyResult(Survey survey, List<AnswerSet> answerSets)
			throws ECFException {
		return this.getECFProfileCompetencyResult(survey, answerSets, new HashMap<>(), null);
	}

	private ECFProfileResult getECFProfileCompetencyResult(Survey survey, List<AnswerSet> answerSets,
			Map<ECFCompetency, Integer> competencyToTargetScore, String profileName) throws ECFException {
		ECFProfileResult profileResult = new ECFProfileResult();
		profileResult.setProfileName(profileName);
		profileResult.setNumberOfAnswers(answerSets.size());

		Map<ECFCompetency, List<Integer>> competenciesToScores = this.getCompetenciesToScores(survey, answerSets);
		for (ECFCompetency competency : competenciesToScores.keySet()) {
			ECFProfileCompetencyResult profileCompetencyResult = new ECFProfileCompetencyResult();
			profileCompetencyResult.setCompetencyName(competency.getName());
			profileCompetencyResult.setOrder(competency.getOrderNumber());
			List<Integer> scores = competenciesToScores.get(competency);

			if (scores.size() != 0) {
				Integer maxScore = 0;
				Integer totalScore = 0;
				for (Integer score : scores) {
					if (score > maxScore) {
						maxScore = score;
					}
					totalScore = totalScore + score;
				}

				float averageScore = (float) Math.round((totalScore.floatValue() / scores.size()) * 10) / 10;
				profileCompetencyResult.setCompetencyAverageScore(averageScore);
				profileCompetencyResult.setCompetencyMaxScore(maxScore);
			}
			profileCompetencyResult.setCompetencyTargetScore(competencyToTargetScore.get(competency));

			profileResult.addIndividualResults(profileCompetencyResult);
		}
		return profileResult;
	}

	private Map<ECFProfile, Map<ECFCompetency, Integer>> getProfilesExpectedScores(Set<ECFProfile> profiles)
			throws ECFException {
		if (profiles == null) {
			throw new IllegalArgumentException("profiles cannot be null");
		}

		Map<ECFProfile, Map<ECFCompetency, Integer>> profileToCompetencyToExpectedScore = new HashMap<>();

		for (ECFProfile profile : profiles) {
			profileToCompetencyToExpectedScore.put(profile, this.getProfileExpectedScores(profile));
		}

		return profileToCompetencyToExpectedScore;
	}

	private Map<ECFCompetency, Integer> getProfileExpectedScores(ECFProfile ecfProfile) throws ECFException {
		if (ecfProfile == null) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}
		Map<ECFCompetency, Integer> competencyToExpectedScore = new HashMap<>();
		for (ECFExpectedScore expectedScore : ecfProfile.getECFExpectedScores()) {
			ECFCompetency competency = expectedScore.getECFExpectedScoreToProfileEid().getECFCompetency();
			if (competency == null) {
				throw new ECFException("A score must be linked to a competency");
			}
			competencyToExpectedScore.put(competency, expectedScore.getScore());
		}
		return competencyToExpectedScore;
	}

	/**
	 * Returns the mapping between the competencies and the scores the answerSets
	 * could get
	 */
	private Map<ECFCompetency, List<Integer>> getCompetenciesToScores(Survey survey, List<AnswerSet> answerSets)
			throws ECFException {
		Map<ECFCompetency, List<Integer>> competencyToScores = new HashMap<>();
		Set<Question> ecfQuestions = new HashSet<>();

		// Target the competency questions
		for (Element element : survey.getElements()) {
			if (element instanceof Question) {
				Question question = (Question) element;
				if (question instanceof SingleChoiceQuestion && question.getEcfCompetency() != null) {
					competencyToScores.put(question.getEcfCompetency(), new ArrayList<>());
					ecfQuestions.add(question);
				}
			}
		}

		// For each individual
		for (int i = 0; i < answerSets.size(); i++) {
			AnswerSet answerSet = answerSets.get(i);
			Map<ECFCompetency, Integer> competencyToNumberOfAnswers = new HashMap<>();
			Map<ECFCompetency, Integer> competencyToTotalAnsweredNumbers = new HashMap<>();

			// Pass through his answers
			for (Question question : ecfQuestions) {
				List<Answer> answers = answerSet.getAnswers(question.getId(), question.getUniqueId());
				if (answers.size() == 0)
					continue;
				Answer answer = answers.get(0);
				SingleChoiceQuestion choiceQuestion = (SingleChoiceQuestion) question;
				PossibleAnswer answeredPossibleAnswer = choiceQuestion
						.getPossibleAnswerByUniqueId(answer.getPossibleAnswerUniqueId());
				if (answeredPossibleAnswer != null) {
					char lastCharInShortName = answeredPossibleAnswer.getShortname()
							.charAt(answeredPossibleAnswer.getShortname().length() - 1);
					Integer answeredNumber = answeredPossibleAnswer.getEcfScore();
					if (answeredNumber > 4 || answeredNumber < 0) {
						throw new ECFException("An ECF possible answer cannot be over 4");
					}

					ECFCompetency questionCompetency = question.getEcfCompetency();

					if (competencyToTotalAnsweredNumbers.containsKey(questionCompetency)) {
						Integer previousNumber = competencyToTotalAnsweredNumbers.get(questionCompetency);
						competencyToTotalAnsweredNumbers.put(questionCompetency, previousNumber + answeredNumber);
					} else {
						competencyToTotalAnsweredNumbers.put(questionCompetency, answeredNumber);
					}

					if (competencyToNumberOfAnswers.containsKey(question.getEcfCompetency())) {
						Integer previousNumber = competencyToNumberOfAnswers.get(question.getEcfCompetency());
						competencyToNumberOfAnswers.put(questionCompetency, previousNumber + 1);
					} else {
						competencyToNumberOfAnswers.put(questionCompetency, 1);
					}
				}
			}
			for (ECFCompetency competency : competencyToNumberOfAnswers.keySet()) {
				Integer numberOfAnswers = competencyToNumberOfAnswers.get(competency);
				Integer totalNumber = competencyToTotalAnsweredNumbers.get(competency);
				List<Integer> listOfScores = competencyToScores.get(competency);
				listOfScores.add(totalNumber / numberOfAnswers);
				competencyToScores.put(competency, listOfScores);
			}
			// next individual
		}
		return competencyToScores;
	}

	public ECFGlobalResult getECFGlobalResult(Survey survey, SqlPagination sqlPagination) throws Exception {
		return this.getECFGlobalResult(survey, sqlPagination, null);
	}

	public ECFGlobalResult getECFGlobalResult(Survey survey, SqlPagination sqlPagination, ECFProfile profileComparison)
			throws Exception {
		return this.getECFGlobalResult(survey, sqlPagination, profileComparison, null);
	}

	/**
	 * Returns the ECFGlobalResult for the given Survey, Pagination, and optional
	 * EcfProfile
	 */
	public ECFGlobalResult getECFGlobalResult(Survey survey, SqlPagination sqlPagination, ECFProfile profileComparison,
			ECFProfile profileFilter) throws Exception {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}

		ECFGlobalResult result = new ECFGlobalResult();
		if (profileComparison != null) {
			result.setProfileComparisonUid(profileComparison.getProfileUid());
		}
		if (profileFilter != null) {
			result.setProfileFilterUid(profileFilter.getProfileUid());
		}

		List<AnswerSet> answerSets = profileFilter != null ? this.getAnswers(survey, profileFilter, sqlPagination)
				: this.getAnswers(survey, sqlPagination);

		Integer countAnswers = profileFilter != null ? this.getCount(survey, profileFilter) : this.getCount(survey);

		Map<ECFCompetency, Integer> competencyToExpectedScore = new HashMap<ECFCompetency, Integer>();

		if (profileComparison != null) {
			result.setProfileComparisonUid(profileComparison.getProfileUid());
			competencyToExpectedScore = this.getProfileExpectedScores(profileComparison);
		}

		Map<ECFCompetency, List<Integer>> competenciesToScores = this.getCompetenciesToScores(survey, answerSets);

		for (ECFCompetency competency : competenciesToScores.keySet()) {
			List<Integer> competencyScores = competenciesToScores.get(competency);

			ECFGlobalCompetencyResult globalCompetencyResult = new ECFGlobalCompetencyResult();
			globalCompetencyResult.setOrder(competency.getOrderNumber());
			globalCompetencyResult.setCompetencyName(competency.getName());
			globalCompetencyResult.setCompetencyScores(competencyScores);

			if (competencyToExpectedScore.containsKey(competency)) {
				Integer targetScore = competencyToExpectedScore.get(competency);
				globalCompetencyResult.setCompetencyTargetScore(targetScore);

				for (Integer competencyScore : competencyScores) {
					globalCompetencyResult.addCompetencyScoreGap(competencyScore - targetScore);
					;
				}
			}
			result.addIndividualResults(globalCompetencyResult);
		}

		ECFGlobalTotalResult totalResult = new ECFGlobalTotalResult();
		Integer totalExpectedScore = null;
		List<Integer> totalScores = new ArrayList<>();
		List<Integer> totalGaps = new ArrayList<>();

		if (!result.getIndividualResults().isEmpty()) {
			int[] totalScoresArray = new int[result.getIndividualResults().get(0).getCompetencyScores().size()];
			int[] totalGapsArray = new int[result.getIndividualResults().get(0).getCompetencyScoreGaps().size()];

			for (ECFGlobalCompetencyResult competencyResult : result.getIndividualResults()) {
				if (competencyResult.getCompetencyTargetScore() != null) {
					totalExpectedScore = totalExpectedScore == null ? competencyResult.getCompetencyTargetScore()
							: totalExpectedScore + competencyResult.getCompetencyTargetScore();
				}

				for (int i = 0; i < competencyResult.getCompetencyScores().size(); i++) {
					totalScoresArray[i] = totalScoresArray[i] + competencyResult.getCompetencyScores().get(i);

					if (totalGapsArray.length > 0) {
						totalGapsArray[i] = totalGapsArray[i] + competencyResult.getCompetencyScoreGaps().get(i);
					}
				}
			}
			totalScores = Ints.asList(totalScoresArray);
			totalGaps = Ints.asList(totalGapsArray);
		}

		totalResult.setTotalTargetScore(totalExpectedScore);
		totalResult.setTotalScores(totalScores);
		totalResult.setTotalGaps(totalGaps);

		result.setTotalResult(totalResult);

		result.setPageNumber(sqlPagination.getCurrentPage());
		result.setPageSize(sqlPagination.getRowsPerPage());
		result.setNumberOfPages((countAnswers / result.getPageSize()) + 1);
		result.setNumberOfResults(countAnswers);

		result.setIndividualResults(result.getIndividualResults().stream().sorted().collect(Collectors.toList()));

		return result;
	}

	public ECFIndividualResult getECFIndividualResult(Survey survey, AnswerSet answerSet) throws ECFException {
		return this.getECFIndividualResult(survey, answerSet, null);
	}

	/**
	 * Returns the individual result for the given answerSet compared to the
	 * expected scores of the profile Or to the profile specified in the AnswerSet
	 * itself.
	 * 
	 * @throws ECFException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Transactional
	public ECFIndividualResult getECFIndividualResult(Survey survey, AnswerSet answerSet, ECFProfile profile)
			throws ECFException {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}
		if (answerSet == null) {
			throw new IllegalArgumentException("answer set is not null");
		}

		ECFIndividualResult ecfIndividualResult = new ECFIndividualResult();
		ECFProfile answererProfile = profile != null ? profile : getECFProfile(survey, answerSet);
		ecfIndividualResult.setProfileName(answererProfile.getName());
		ecfIndividualResult.setProfileUUID(answererProfile.getProfileUid());

		List<AnswerSet> answerSets = Arrays.asList(answerSet);
		Map<ECFCompetency, List<Integer>> competenciesToScores = this.getCompetenciesToScores(survey, answerSets);
		Map<ECFCompetency, Integer> competenciesToExpectedScores = this.getProfileExpectedScores(answererProfile);

		for (ECFCompetency competency : competenciesToScores.keySet()) {
			ECFIndividualCompetencyResult competencyResult = new ECFIndividualCompetencyResult();
			competencyResult.setCompetencyName(competency.getName());
			competencyResult.setOrder(competency.getOrderNumber());
			competencyResult.setCompetencyTargetScore(competenciesToExpectedScores.get(competency));
			competencyResult.setCompetencyScore(competenciesToScores.get(competency).get(0));
			ecfIndividualResult.addCompetencyResult(competencyResult);
		}
		ecfIndividualResult.setCompetencyResultList(
				ecfIndividualResult.getCompetencyResultList().stream().sorted().collect(Collectors.toList()));
		return ecfIndividualResult;
	}

	/**
	 * Returns the ECFProfile an answerer has entered in the answerSet, for a
	 * specific ECF survey
	 * 
	 * @throws ECFException if no ECFProfile could be found answered by the user
	 */
	public ECFProfile getECFProfile(Survey survey, AnswerSet answerSet) throws ECFException {
		if (answerSet.getEcfProfileUid() != null) {
			return this.getECFProfileByUUID(answerSet.getEcfProfileUid());
		}
		for (Element element : survey.getElements()) {
			if (element instanceof Question) {
				Question question = (Question) element;
				List<Answer> answers = answerSet.getAnswers(question.getId(), question.getUniqueId());

				if (question instanceof SingleChoiceQuestion) {
					if (answers.size() == 0)
						continue;
					// get points if answer is correct
					Answer answer = answers.get(0);
					ChoiceQuestion choice = (ChoiceQuestion) question;
					PossibleAnswer possibleAnswer = choice
							.getPossibleAnswerByUniqueId(answer.getPossibleAnswerUniqueId());
					if (possibleAnswer.getEcfProfile() != null) {
						return possibleAnswer.getEcfProfile();
					}
				}
			}
		}
		throw new ECFException("An answers set must reference a profile");
	}

	public Map<String, String> defaultClusterToType() {
		Map<String, String> clusterNameToType = new HashMap<>();
		clusterNameToType.put("Horizontal", "Procurement specific competencies");
		clusterNameToType.put("Pre-award", "Procurement specific competencies");
		clusterNameToType.put("Post-award", "Procurement specific competencies");

		clusterNameToType.put("Self", "Professional competencies");
		clusterNameToType.put("People", "Professional competencies");
		clusterNameToType.put("Performance", "Professional competencies");
		return clusterNameToType;

	}

	public Map<String, String> defaultCompetencyToCluster() {
		Map<String, String> competencyToCluster = new HashMap<>();
		competencyToCluster.put("C1 - Planning", "Horizontal");
		competencyToCluster.put("C2 - Lifecycle", "Horizontal");
		competencyToCluster.put("C3 - Legislation", "Horizontal");
		competencyToCluster.put("C4 - e-Procurement & other IT tools", "Horizontal");
		competencyToCluster.put("C5 - Sustainable procurement", "Horizontal");
		competencyToCluster.put("C6 - Innovation Procurement", "Horizontal");
		competencyToCluster.put("C7 - Category specific", "Horizontal");
		competencyToCluster.put("C8 - Supplier management", "Horizontal");
		competencyToCluster.put("C9 - Negotiations", "Horizontal");
		competencyToCluster.put("C10 - Negotiations", "Pre-award");
		competencyToCluster.put("C11 - Market analysis and market engagement", "Pre-award");
		competencyToCluster.put("C12 - Procurement strategy", "Pre-award");
		competencyToCluster.put("C13 - Technical specifications", "Pre-award");
		competencyToCluster.put("C14 - Tender documentation", "Pre-award");
		competencyToCluster.put("C15 - Tender evaluation", "Pre-award");
		competencyToCluster.put("C16 - Contract management", "Post-award");
		competencyToCluster.put("C17 - Certification and payment", "Post-award");
		competencyToCluster.put("C18 - Reporting and evaluation", "Post-award");
		competencyToCluster.put("C19 - Conflict resolution / mediation", "Post-award");
		competencyToCluster.put("C20 - Adaptability and modernisation", "Self");
		competencyToCluster.put("C21 - Analytical and critical thinking", "Self");
		competencyToCluster.put("C22 - Communication", "Self");
		competencyToCluster.put("C23 - Ethics and compliance", "Self");
		competencyToCluster.put("C24 - Collaboration", "People");
		competencyToCluster.put("C25 - Stakeholder relationship management", "People");
		competencyToCluster.put("C26 - Team management and Leadership", "People");
		competencyToCluster.put("C27 - Organisational awareness", "Performance");
		competencyToCluster.put("C28 - Project management", "Performance");
		competencyToCluster.put("C29 - Performance orientation", "Performance");
		competencyToCluster.put("C30 - Risk management and internal control", "Performance");
		return competencyToCluster;
	}

	public Map<String, Integer> defaultCompetenciesOrder() {
		Map<String, Integer> competencyToOrder = new HashMap<>();
		competencyToOrder.put("C1 - Planning", 1);
		competencyToOrder.put("C2 - Lifecycle", 2);
		competencyToOrder.put("C3 - Legislation", 3);
		competencyToOrder.put("C4 - e-Procurement & other IT tools", 4);
		competencyToOrder.put("C5 - Sustainable procurement", 5);
		competencyToOrder.put("C6 - Innovation Procurement", 6);
		competencyToOrder.put("C7 - Category specific", 7);
		competencyToOrder.put("C8 - Supplier management", 8);
		competencyToOrder.put("C9 - Negotiations", 9);
		competencyToOrder.put("C10 - Negotiations", 10);
		competencyToOrder.put("C11 - Market analysis and market engagement", 11);
		competencyToOrder.put("C12 - Procurement strategy", 12);
		competencyToOrder.put("C13 - Technical specifications", 13);
		competencyToOrder.put("C14 - Tender documentation", 14);
		competencyToOrder.put("C15 - Tender evaluation", 15);
		competencyToOrder.put("C16 - Contract management", 16);
		competencyToOrder.put("C17 - Certification and payment", 17);
		competencyToOrder.put("C18 - Reporting and evaluation", 18);
		competencyToOrder.put("C19 - Conflict resolution / mediation", 19);
		competencyToOrder.put("C20 - Adaptability and modernisation", 20);
		competencyToOrder.put("C21 - Analytical and critical thinking", 21);
		competencyToOrder.put("C22 - Communication", 22);
		competencyToOrder.put("C23 - Ethics and compliance", 23);
		competencyToOrder.put("C24 - Collaboration", 24);
		competencyToOrder.put("C25 - Stakeholder relationship management", 25);
		competencyToOrder.put("C26 - Team management and Leadership", 26);
		competencyToOrder.put("C27 - Organisational awareness", 27);
		competencyToOrder.put("C28 - Project management", 28);
		competencyToOrder.put("C29 - Performance orientation", 29);
		competencyToOrder.put("C30 - Risk management and internal control", 30);
		return competencyToOrder;
	}

	public Map<String, Map<String, Integer>> defaultProfileNameToCompetencyName() {
		Map<String, Map<String, Integer>> profileToCompetencyToScore = new HashMap<>();
		profileToCompetencyToScore.put("Procurement support officer", this.defaultProcurementSupportOfficer());
		profileToCompetencyToScore.put("Standalone public buyer", this.defaultStandalonePublicBuyer());
		profileToCompetencyToScore.put("Public procurement specialist", this.defaultPublicProcurementSpecialist());
		profileToCompetencyToScore.put("Category specialist", this.defaultCategorySpecialist());
		profileToCompetencyToScore.put("Contract manager", this.defaultContractManager());
		profileToCompetencyToScore.put("Department manager", this.defaultDepartmentManager());
		return profileToCompetencyToScore;
	}

	private Map<String, Integer> defaultDepartmentManager() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 3);
		competencyToScore.put("C2 - Lifecycle", 3);
		competencyToScore.put("C3 - Legislation", 4);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 2);
		competencyToScore.put("C5 - Sustainable procurement", 3);
		competencyToScore.put("C6 - Innovation Procurement", 3);
		competencyToScore.put("C7 - Category specific", 0);
		competencyToScore.put("C8 - Supplier management", 2);
		competencyToScore.put("C9 - Negotiations", 3);
		competencyToScore.put("C10 - Negotiations", 3);
		competencyToScore.put("C11 - Market analysis and market engagement", 0);
		competencyToScore.put("C12 - Procurement strategy", 3);
		competencyToScore.put("C13 - Technical specifications", 0);
		competencyToScore.put("C14 - Tender documentation", 0);
		competencyToScore.put("C15 - Tender evaluation", 2);
		competencyToScore.put("C16 - Contract management", 2);
		competencyToScore.put("C17 - Certification and payment", 2);
		competencyToScore.put("C18 - Reporting and evaluation", 3);
		competencyToScore.put("C19 - Conflict resolution / mediation", 2);
		competencyToScore.put("C20 - Adaptability and modernisation", 3);
		competencyToScore.put("C21 - Analytical and critical thinking", 4);
		competencyToScore.put("C22 - Communication", 3);
		competencyToScore.put("C23 - Ethics and compliance", 4);
		competencyToScore.put("C24 - Collaboration", 3);
		competencyToScore.put("C25 - Stakeholder relationship management", 4);
		competencyToScore.put("C26 - Team management and Leadership", 3);
		competencyToScore.put("C27 - Organisational awareness", 4);
		competencyToScore.put("C28 - Project management", 3);
		competencyToScore.put("C29 - Performance orientation", 3);
		competencyToScore.put("C30 - Risk management and internal control", 4);
		return competencyToScore;
	}

	private Map<String, Integer> defaultContractManager() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 2);
		competencyToScore.put("C2 - Lifecycle", 2);
		competencyToScore.put("C3 - Legislation", 2);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 1);
		competencyToScore.put("C5 - Sustainable procurement", 2);
		competencyToScore.put("C6 - Innovation Procurement", 2);
		competencyToScore.put("C7 - Category specific", 0);
		competencyToScore.put("C8 - Supplier management", 2);
		competencyToScore.put("C9 - Negotiations", 2);
		competencyToScore.put("C10 - Negotiations", 0);
		competencyToScore.put("C11 - Market analysis and market engagement", 0);
		competencyToScore.put("C12 - Procurement strategy", 0);
		competencyToScore.put("C13 - Technical specifications", 0);
		competencyToScore.put("C14 - Tender documentation", 0);
		competencyToScore.put("C15 - Tender evaluation", 0);
		competencyToScore.put("C16 - Contract management", 3);
		competencyToScore.put("C17 - Certification and payment", 3);
		competencyToScore.put("C18 - Reporting and evaluation", 2);
		competencyToScore.put("C19 - Conflict resolution / mediation", 2);
		competencyToScore.put("C20 - Adaptability and modernisation", 2);
		competencyToScore.put("C21 - Analytical and critical thinking", 3);
		competencyToScore.put("C22 - Communication", 3);
		competencyToScore.put("C23 - Ethics and compliance", 3);
		competencyToScore.put("C24 - Collaboration", 2);
		competencyToScore.put("C25 - Stakeholder relationship management", 3);
		competencyToScore.put("C26 - Team management and Leadership", 0);
		competencyToScore.put("C27 - Organisational awareness", 2);
		competencyToScore.put("C28 - Project management", 2);
		competencyToScore.put("C29 - Performance orientation", 3);
		competencyToScore.put("C30 - Risk management and internal control", 3);
		return competencyToScore;
	}

	private Map<String, Integer> defaultCategorySpecialist() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 2);
		competencyToScore.put("C2 - Lifecycle", 3);
		competencyToScore.put("C3 - Legislation", 1);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 1);
		competencyToScore.put("C5 - Sustainable procurement", 3);
		competencyToScore.put("C6 - Innovation Procurement", 3);
		competencyToScore.put("C7 - Category specific", 3);
		competencyToScore.put("C8 - Supplier management", 2);
		competencyToScore.put("C9 - Negotiations", 0);
		competencyToScore.put("C10 - Negotiations", 2);
		competencyToScore.put("C11 - Market analysis and market engagement", 2);
		competencyToScore.put("C12 - Procurement strategy", 2);
		competencyToScore.put("C13 - Technical specifications", 3);
		competencyToScore.put("C14 - Tender documentation", 1);
		competencyToScore.put("C15 - Tender evaluation", 1);
		competencyToScore.put("C16 - Contract management", 2);
		competencyToScore.put("C17 - Certification and payment", 0);
		competencyToScore.put("C18 - Reporting and evaluation", 0);
		competencyToScore.put("C19 - Conflict resolution / mediation", 0);
		competencyToScore.put("C20 - Adaptability and modernisation", 2);
		competencyToScore.put("C21 - Analytical and critical thinking", 2);
		competencyToScore.put("C22 - Communication", 1);
		competencyToScore.put("C23 - Ethics and compliance", 1);
		competencyToScore.put("C24 - Collaboration", 1);
		competencyToScore.put("C25 - Stakeholder relationship management", 1);
		competencyToScore.put("C26 - Team management and Leadership", 0);
		competencyToScore.put("C27 - Organisational awareness", 1);
		competencyToScore.put("C28 - Project management", 0);
		competencyToScore.put("C29 - Performance orientation", 2);
		competencyToScore.put("C30 - Risk management and internal control", 1);
		return competencyToScore;
	}

	private Map<String, Integer> defaultPublicProcurementSpecialist() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 1);
		competencyToScore.put("C2 - Lifecycle", 3);
		competencyToScore.put("C3 - Legislation", 1);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 2);
		competencyToScore.put("C5 - Sustainable procurement", 1);
		competencyToScore.put("C6 - Innovation Procurement", 1);
		competencyToScore.put("C7 - Category specific", 1);
		competencyToScore.put("C8 - Supplier management", 1);
		competencyToScore.put("C9 - Negotiations", 2);
		competencyToScore.put("C10 - Negotiations", 2);
		competencyToScore.put("C11 - Market analysis and market engagement", 2);
		competencyToScore.put("C12 - Procurement strategy", 2);
		competencyToScore.put("C13 - Technical specifications", 2);
		competencyToScore.put("C14 - Tender documentation", 2);
		competencyToScore.put("C15 - Tender evaluation", 2);
		competencyToScore.put("C16 - Contract management", 1);
		competencyToScore.put("C17 - Certification and payment", 1);
		competencyToScore.put("C18 - Reporting and evaluation", 2);
		competencyToScore.put("C19 - Conflict resolution / mediation", 1);
		competencyToScore.put("C20 - Adaptability and modernisation", 1);
		competencyToScore.put("C21 - Analytical and critical thinking", 2);
		competencyToScore.put("C22 - Communication", 2);
		competencyToScore.put("C23 - Ethics and compliance", 2);
		competencyToScore.put("C24 - Collaboration", 2);
		competencyToScore.put("C25 - Stakeholder relationship management", 2);
		competencyToScore.put("C26 - Team management and Leadership", 1);
		competencyToScore.put("C27 - Organisational awareness", 2);
		competencyToScore.put("C28 - Project management", 2);
		competencyToScore.put("C29 - Performance orientation", 2);
		competencyToScore.put("C30 - Risk management and internal control", 2);
		return competencyToScore;
	}

	private Map<String, Integer> defaultStandalonePublicBuyer() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 1);
		competencyToScore.put("C2 - Lifecycle", 2);
		competencyToScore.put("C3 - Legislation", 2);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 2);
		competencyToScore.put("C5 - Sustainable procurement", 1);
		competencyToScore.put("C6 - Innovation Procurement", 1);
		competencyToScore.put("C7 - Category specific", 1);
		competencyToScore.put("C8 - Supplier management", 1);
		competencyToScore.put("C9 - Negotiations", 2);
		competencyToScore.put("C10 - Negotiations", 1);
		competencyToScore.put("C11 - Market analysis and market engagement", 2);
		competencyToScore.put("C12 - Procurement strategy", 2);
		competencyToScore.put("C13 - Technical specifications", 2);
		competencyToScore.put("C14 - Tender documentation", 2);
		competencyToScore.put("C15 - Tender evaluation", 2);
		competencyToScore.put("C16 - Contract management", 2);
		competencyToScore.put("C17 - Certification and payment", 2);
		competencyToScore.put("C18 - Reporting and evaluation", 2);
		competencyToScore.put("C19 - Conflict resolution / mediation", 1);
		competencyToScore.put("C20 - Adaptability and modernisation", 2);
		competencyToScore.put("C21 - Analytical and critical thinking", 2);
		competencyToScore.put("C22 - Communication", 2);
		competencyToScore.put("C23 - Ethics and compliance", 3);
		competencyToScore.put("C24 - Collaboration", 1);
		competencyToScore.put("C25 - Stakeholder relationship management", 1);
		competencyToScore.put("C26 - Team management and Leadership", 1);
		competencyToScore.put("C27 - Organisational awareness", 2);
		competencyToScore.put("C28 - Project management", 2);
		competencyToScore.put("C29 - Performance orientation", 2);
		competencyToScore.put("C30 - Risk management and internal control", 2);
		return competencyToScore;
	}

	private Map<String, Integer> defaultProcurementSupportOfficer() {
		Map<String, Integer> competencyToScore = new HashMap<>();
		competencyToScore.put("C1 - Planning", 0);
		competencyToScore.put("C2 - Lifecycle", 1);
		competencyToScore.put("C3 - Legislation", 0);
		competencyToScore.put("C4 - e-Procurement & other IT tools", 1);
		competencyToScore.put("C5 - Sustainable procurement", 0);
		competencyToScore.put("C6 - Innovation Procurement", 0);
		competencyToScore.put("C7 - Category specific", 0);
		competencyToScore.put("C8 - Supplier management", 1);
		competencyToScore.put("C9 - Negotiations", 0);
		competencyToScore.put("C10 - Negotiations", 1);
		competencyToScore.put("C11 - Market analysis and market engagement", 1);
		competencyToScore.put("C12 - Procurement strategy", 0);
		competencyToScore.put("C13 - Technical specifications", 1);
		competencyToScore.put("C14 - Tender documentation", 1);
		competencyToScore.put("C15 - Tender evaluation", 0);
		competencyToScore.put("C16 - Contract management", 1);
		competencyToScore.put("C17 - Certification and payment", 1);
		competencyToScore.put("C18 - Reporting and evaluation", 1);
		competencyToScore.put("C19 - Conflict resolution / mediation", 0);
		competencyToScore.put("C20 - Adaptability and modernisation", 0);
		competencyToScore.put("C21 - Analytical and critical thinking", 1);
		competencyToScore.put("C22 - Communication", 1);
		competencyToScore.put("C23 - Ethics and compliance", 2);
		competencyToScore.put("C24 - Collaboration", 2);
		competencyToScore.put("C25 - Stakeholder relationship management", 1);
		competencyToScore.put("C26 - Team management and Leadership", 0);
		competencyToScore.put("C27 - Organisational awareness", 2);
		competencyToScore.put("C28 - Project management", 1);
		competencyToScore.put("C29 - Performance orientation", 1);
		competencyToScore.put("C30 - Risk management and internal control", 1);
		return competencyToScore;
	}

	/**
	 * Creates the ECF profiles defined for the matrix
	 */
	@Transactional
	public Map<ECFProfile, Map<String, Integer>> createECFProfileToCompetencyNameToScore(
			Map<String, Map<String, Integer>> profileNameToCompetencyToScore) {
		Session session = sessionFactory.getCurrentSession();

		Map<ECFProfile, Map<String, Integer>> profileToCompetencyToScore = new HashMap<>();

		for (String profileName : profileNameToCompetencyToScore.keySet()) {
			ECFProfile ecfProfile = new ECFProfile(UUID.randomUUID().toString(), profileName,
					profileName + " description");
			session.saveOrUpdate(ecfProfile);

			Map<String, Integer> competencyNameToScore = profileNameToCompetencyToScore.get(profileName);
			profileToCompetencyToScore.put(ecfProfile, competencyNameToScore);
		}

		return profileToCompetencyToScore;
	}

	@Transactional
	public Map<String, ECFType> createClusterNameToType(Map<String, String> clusterToTypeNames) {
		Session session = sessionFactory.getCurrentSession();

		Map<String, ECFType> clusterNameToType = new HashMap<>();
		for (String clusterName : clusterToTypeNames.keySet()) {
			String typeName = clusterToTypeNames.get(clusterName);
			ECFType type = new ECFType(UUID.randomUUID().toString(), typeName);
			session.saveOrUpdate(type);
			clusterNameToType.put(clusterName, type);
		}

		return clusterNameToType;
	}

	@Transactional
	public Map<String, ECFCluster> createCompetencyNameToCluster(Map<String, ECFType> clusterNameToType,
			Map<String, String> competencyToClusterNames) {
		Session session = sessionFactory.getCurrentSession();
		Map<String, ECFCluster> clusterNameToCluster = new HashMap<>();
		for (String clusterName : clusterNameToType.keySet()) {
			ECFType ecfType = clusterNameToType.get(clusterName);
			ECFCluster cluster = new ECFCluster(UUID.randomUUID().toString(), clusterName, ecfType);

			session.saveOrUpdate(cluster);
			clusterNameToCluster.put(clusterName, cluster);
		}

		Map<String, ECFCluster> competencyNameToCluster = new HashMap<>();
		for (String competencyName : competencyToClusterNames.keySet()) {
			String clusterName = competencyToClusterNames.get(competencyName);
			ECFCluster cluster = clusterNameToCluster.get(clusterName);
			competencyNameToCluster.put(competencyName, cluster);
		}

		return competencyNameToCluster;
	}

	/**
	 * Creates the ECF competencies defined for the matrix
	 */
	@Transactional
	public Set<ECFCompetency> createECFCompetencies(Map<ECFProfile, Map<String, Integer>> profileToCompetencyToScore,
			Map<String, ECFCluster> competencyToCluster, Map<String, Integer> competencyToOrder) {
		Session session = sessionFactory.getCurrentSession();
		Set<ECFCompetency> ecfCompetencies = new HashSet<ECFCompetency>();

		Map<String, ECFCompetency> nameToCompetency = new HashMap<>();

		// Go through the profiles
		for (ECFProfile profile : profileToCompetencyToScore.keySet()) {
			Map<String, Integer> competencyNameToScore = profileToCompetencyToScore.get(profile);

			// Go through the competencies and their scores
			for (String competencyName : competencyNameToScore.keySet()) {
				Integer expectedScoreI = competencyNameToScore.get(competencyName);
				ECFCompetency ecfCompetency = null;
				ECFCluster cluster = competencyToCluster.get(competencyName);
				if (nameToCompetency.containsKey(competencyName)) {
					ecfCompetency = nameToCompetency.get(competencyName);
				} else {
					ecfCompetency = new ECFCompetency(UUID.randomUUID().toString(), competencyName,
							competencyName + " description", cluster, competencyToOrder.get(competencyName));
					session.saveOrUpdate(ecfCompetency);
					nameToCompetency.put(competencyName, ecfCompetency);
				}

				ECFExpectedScoreToProfileEid eid = new ECFExpectedScoreToProfileEid();
				eid.setECFCompetency(ecfCompetency);
				eid.setECFProfile(profile);
				ECFExpectedScore expectedScore = new ECFExpectedScore(eid, expectedScoreI);
				session.saveOrUpdate(expectedScore);

				profile.addECFExpectedScore(expectedScore);
				ecfCompetency.addECFExpectedScore(expectedScore);
			}
		}

		return nameToCompetency.values().stream().collect(Collectors.toSet());
	}

	public Set<ECFProfile> getECFProfiles(Survey ecfSurvey) {
		if (ecfSurvey == null || !ecfSurvey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to get its profile");
		}

		Set<ECFProfile> profileSet = new HashSet<>();
		for (Element element : ecfSurvey.getElementsRecursive(true)) {
			if (element instanceof PossibleAnswer) {
				PossibleAnswer possibleAnswer = (PossibleAnswer) element;
				if (possibleAnswer.getEcfProfile() != null) {
					profileSet.add(possibleAnswer.getEcfProfile());
				}
			}
		}

		return profileSet;
	}

	@Transactional
	public Survey copySurveyECFElements(Survey alreadyCopiedSurvey) throws ECFException {
		Session session = sessionFactory.getCurrentSession();
		if (alreadyCopiedSurvey == null || !alreadyCopiedSurvey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to copy its ECF elements");
		}

		Map<ECFCompetency, ECFCompetency> oldECFCompetencyToNew = new HashMap<>();
		Map<ECFProfile, ECFProfile> oldECFProfileToNew = new HashMap<>();
		Map<ECFCluster, ECFCluster> oldECFClusterToNew = new HashMap<>();
		Map<ECFType, ECFType> oldECFTypeToNew = new HashMap<>();

		Set<ECFExpectedScore> encounteredScores = new HashSet<>();

		for (Element element : alreadyCopiedSurvey.getElementsRecursive(true)) {
			if (element instanceof Question) {
				Question question = (Question) element;
				if (question instanceof SingleChoiceQuestion && question.getEcfCompetency() != null) {
					for (ECFExpectedScore score : question.getEcfCompetency().getECFExpectedScores()) {
						encounteredScores.add(score);
					}
					ECFCompetency oldCompetency = question.getEcfCompetency();

					ECFCompetency newCompetency = null;

					if (!oldECFCompetencyToNew.containsKey(oldCompetency)) {
						// COPYING THE COMPETENCY AND ALL ITS INNER COMPONENTS
						newCompetency = oldCompetency.copy();

						ECFCluster oldCluster = oldCompetency.getEcfCluster();
						ECFCluster newCluster = null;
						if (!oldECFClusterToNew.containsKey(oldCluster)) {
							newCluster = oldCluster.copy();

							ECFType oldType = oldCluster.getEcfType();
							ECFType newType = null;

							if (!oldECFTypeToNew.containsKey(oldType)) {
								newType = oldType.copy();

								// SAVING TYPE
								session.saveOrUpdate(newType);
								oldECFTypeToNew.put(oldType, newType);
							} else {
								newType = oldECFTypeToNew.get(oldType);
							}

							newCluster.setEcfType(newType);
							newType.addCluster(newCluster);

							// SAVING CLUSTER
							session.saveOrUpdate(newCluster);
							oldECFClusterToNew.put(oldCluster, newCluster);
						} else {
							newCluster = oldECFClusterToNew.get(oldCluster);
						}
						newCompetency.setEcfCluster(newCluster);
						newCluster.addCompetency(newCompetency);

						// SAVING COMPETENCY
						session.saveOrUpdate(newCompetency);
						oldECFCompetencyToNew.put(oldCompetency, newCompetency);
					} else {
						newCompetency = oldECFCompetencyToNew.get(oldCompetency);
					}

					question.setEcfCompetency(newCompetency);
				}
			}
			if (element instanceof PossibleAnswer) {
				PossibleAnswer possibleAnswer = (PossibleAnswer) element;

				if (possibleAnswer.getEcfProfile() != null) {
					for (ECFExpectedScore score : possibleAnswer.getEcfProfile().getECFExpectedScores()) {
						encounteredScores.add(score);
					}

					ECFProfile oldProfile = possibleAnswer.getEcfProfile();

					ECFProfile newProfile = null;
					if (!oldECFProfileToNew.containsKey(oldProfile)) {
						newProfile = oldProfile.copy();
						session.saveOrUpdate(newProfile);
						oldECFProfileToNew.put(oldProfile, newProfile);
					} else {
						newProfile = oldECFProfileToNew.get(oldProfile);
					}

					possibleAnswer.setEcfProfile(newProfile);
				}
			}
		}

		for (ECFExpectedScore score : encounteredScores) {
			ECFCompetency oldScoreCompetency = score.getECFExpectedScoreToProfileEid().getECFCompetency();
			ECFProfile oldScoreProfile = score.getECFExpectedScoreToProfileEid().getECFProfile();
			Integer oldScore = score.getScore();

			ECFCompetency newScoreCompetency = oldECFCompetencyToNew.get(oldScoreCompetency);
			ECFProfile newScoreProfile = oldECFProfileToNew.get(oldScoreProfile);

			if (newScoreCompetency == null) {
				throw new ECFException("Competency " + oldScoreCompetency.getCompetenceUid() + " was not well copied");
			}
			if (newScoreProfile == null) {
				throw new ECFException("Profile " + oldScoreProfile.getProfileUid() + " was not well copied");
			}

			ECFExpectedScoreToProfileEid eid = new ECFExpectedScoreToProfileEid();
			eid.setECFCompetency(newScoreCompetency);
			eid.setECFProfile(newScoreProfile);
			ECFExpectedScore newScore = new ECFExpectedScore(eid, oldScore);

			session.saveOrUpdate(newScore);

			newScoreCompetency.addECFExpectedScore(newScore);
			newScoreProfile.addECFExpectedScore(newScore);

			session.saveOrUpdate(newScoreCompetency);
			session.saveOrUpdate(newScoreProfile);
		}

		return alreadyCopiedSurvey;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public ECFProfile getECFProfileByUUID(final String profileUid) {
		Session session = sessionFactory.getCurrentSession();

		String hql = "FROM ECFProfile E WHERE E.profileUid = :profileUid";
		Query query = session.createQuery(hql);
		query.setParameter("profileUid", profileUid);

		List<ECFProfile> result = query.list();
		return result.isEmpty() ? null : result.get(0);
	}

	private float roundedAverage(Integer totalScore, Integer numberOfScores) {
		return (float) Math.round((totalScore.floatValue() / numberOfScores) * 10) / 10;
	}

	private List<AnswerSet> getAnswers(Survey survey, ECFProfile profile, SqlPagination sqlPagination)
			throws Exception {
		ResultFilter resultFilter = new ResultFilter();
		resultFilter.setEcfProfileUid(profile.getProfileUid());
		return answerService.getAnswers(survey, resultFilter, sqlPagination, false, false, true);
	}

	private List<AnswerSet> getAnswers(Survey survey, SqlPagination sqlPagination) throws Exception {
		return answerService.getAnswers(survey, null, sqlPagination, false, false, true);
	}

	private Integer getCount(Survey survey, ECFProfile profile) throws Exception {
		ResultFilter resultFilter = new ResultFilter();
		resultFilter.setEcfProfileUid(profile.getProfileUid());
		return this.answerService.getNumberOfAnswerSets(survey, resultFilter);
	}

	private Integer getCount(Survey survey) throws Exception {
		ResultFilter resultFilter = new ResultFilter();
		return this.answerService.getNumberOfAnswerSets(survey, resultFilter);
	}

}