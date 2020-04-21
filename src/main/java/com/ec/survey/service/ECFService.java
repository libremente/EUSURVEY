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
import com.ec.survey.model.ECFCompetency;
import com.ec.survey.model.ECFExpectedScore;
import com.ec.survey.model.ECFExpectedScoreToProfileEid;
import com.ec.survey.model.ECFProfile;
import com.ec.survey.model.SqlPagination;
import com.ec.survey.model.survey.ChoiceQuestion;
import com.ec.survey.model.survey.Element;
import com.ec.survey.model.survey.PossibleAnswer;
import com.ec.survey.model.survey.Question;
import com.ec.survey.model.survey.SingleChoiceQuestion;
import com.ec.survey.model.survey.Survey;
import com.ec.survey.model.survey.ecf.ECFGlobalCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFGlobalResult;
import com.ec.survey.model.survey.ecf.ECFIndividualCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFIndividualResult;
import com.ec.survey.model.survey.ecf.ECFOrganizationalCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFOrganizationalResult;
import com.ec.survey.model.survey.ecf.ECFProfileCompetencyResult;
import com.ec.survey.model.survey.ecf.ECFProfileResult;

@Service("ecfService")
@Configurable
public class ECFService extends BasicService {

	private static final Logger logger = Logger.getLogger(ECFService.class);

	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

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

	/**
	 * Returns the ECFGlobalResult for the given Survey, Pagination, and optional
	 * EcfProfile
	 */
	public ECFGlobalResult getECFGlobalResult(Survey survey, SqlPagination sqlPagination, ECFProfile ecfProfile)
			throws Exception {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}

		ECFGlobalResult result = new ECFGlobalResult();
		List<AnswerSet> answerSets = this.answerService.getAnswersFromReporting(survey, sqlPagination);
		Integer countAnswers = this.reportingService.getCount(survey);

		Map<ECFCompetency, Integer> competencyToExpectedScore = new HashMap<ECFCompetency, Integer>();

		if (ecfProfile != null) {
			result.setProfileName(ecfProfile.getName());
			competencyToExpectedScore = this.getProfileExpectedScores(ecfProfile);
		}

		Map<ECFCompetency, List<Integer>> competenciesToScores = this.getCompetenciesToScores(survey, answerSets);

		for (ECFCompetency competency : competenciesToScores.keySet()) {
			List<Integer> competencyScores = competenciesToScores.get(competency);

			ECFGlobalCompetencyResult globalCompetencyResult = new ECFGlobalCompetencyResult();
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
	private ECFProfile getECFProfile(Survey survey, AnswerSet answerSet) throws ECFException {
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

	/**
	 * Creates the ECF profiles defined for the matrix
	 */
	@Transactional
	public Set<ECFProfile> createECFProfiles() {
		Session session = sessionFactory.getCurrentSession();
		Set<ECFProfile> ecfProfiles = new HashSet<ECFProfile>();

		// TODO: configuration for this
		String[] profiles = new String[] { "Procurement support officer", "Standalone public buyer",
				"Public procurement specialist", "Category specialist", "Contract manager", "Department manager" };

		for (String profileName : profiles) {
			ECFProfile ecfProfile = new ECFProfile(UUID.randomUUID().toString(), profileName, "profile_description");
			ecfProfiles.add(ecfProfile);
			session.saveOrUpdate(ecfProfile);
		}

		return ecfProfiles;
	}

	/**
	 * Creates the ECF competencies defined for the matrix
	 */
	@Transactional
	public Set<ECFCompetency> createECFCompetencies(Set<ECFProfile> ecfProfiles) {
		Session session = sessionFactory.getCurrentSession();
		Set<ECFCompetency> ecfCompetencies = new HashSet<ECFCompetency>();

		// Iterate on the competencies
		for (int i = 0; i < 5; i++) {
			ECFCompetency ecfCompetency = new ECFCompetency(UUID.randomUUID().toString(), "competency_name" + i,
					"competency_description");
			session.saveOrUpdate(ecfCompetency);

			// Iterate on the profiles to set the scores
			List<ECFExpectedScore> expectedScores = new ArrayList<>();
			for (ECFProfile ecfProfile : ecfProfiles) {
				ECFExpectedScoreToProfileEid eid = new ECFExpectedScoreToProfileEid();
				eid.setECFCompetency(ecfCompetency);
				eid.setECFProfile(ecfProfile);
				ECFExpectedScore expectedScore = new ECFExpectedScore(eid, 2);
				session.saveOrUpdate(expectedScore);

				ecfProfile.addECFExpectedScore(expectedScore);
				expectedScores.add(expectedScore);
			}
			ecfCompetency.setECFExpectedScores(expectedScores);

			ecfCompetencies.add(ecfCompetency);
		}
		return ecfCompetencies;
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
	public Survey copySurveyECFElements(Survey alreadyCopiedSurvey) {
		Session session = sessionFactory.getCurrentSession();
		if (alreadyCopiedSurvey == null || !alreadyCopiedSurvey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to copy its ECF elements");
		}

		Map<ECFCompetency, ECFCompetency> oldECFCompetencyToNew = new HashMap<>();
		Map<ECFProfile, ECFProfile> oldECFProfileToNew = new HashMap<>();

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
						newCompetency = oldCompetency.copy();
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
					ECFProfile newProfile = oldProfile.copy();
					session.saveOrUpdate(newProfile);

					oldECFProfileToNew.put(oldProfile, newProfile);

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

}