package com.ec.survey.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

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

@Service("ecfService")
@Configurable
public class ECFService extends BasicService {
	
	@Resource(name="sessionFactory")
	private SessionFactory sessionFactory;

	
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
		
		if (ecfProfile != null) {
			result.setProfileName(ecfProfile.getName());

			Map<ECFCompetency, Integer> competencyToExpectedScore = new HashMap<ECFCompetency, Integer>();
			for (ECFExpectedScore expectedScore : ecfProfile.getECFExpectedScores()) {
				ECFCompetency competency = expectedScore.getECFExpectedScoreToProfileEid().getECFCompetency();
				if (competency == null) {
					throw new ECFException("A score must be linked to a competency");
				}
				competencyToExpectedScore.put(competency, expectedScore.getScore());
			}

			for (Entry<ECFCompetency, Integer> competencyToScore : competencyToExpectedScore.entrySet()) {
				ECFGlobalCompetencyResult oneResult = getECFGlobalCompetencyResult(survey, answerSets,
						competencyToScore);
				result.addIndividualResults(oneResult);
			}

		} else {
			result = this.getECFGlobalResultWithoutProfile(survey, answerSets);
		}
		
		result.setPageNumber(sqlPagination.getCurrentPage());
		result.setPageSize(sqlPagination.getRowsPerPage());
		result.setNumberOfPages((countAnswers / result.getPageSize())+1);
		
		logger.info("count " + countAnswers);
		logger.info("pageSize " + result.getPageSize());
		
		result.setIndividualResults(result.getIndividualResults().stream().sorted().collect(Collectors.toList()));

		return result;
	}

	public ECFGlobalResult getECFGlobalResultWithoutProfile(Survey survey, List<AnswerSet> answerSets)
			throws ECFException {
		Map<String, List<Integer>> competencyToScores = new HashMap<>();
		Set<Question> ecfQuestions = new HashSet<>();

		for (Element element : survey.getElements()) {
			if (element instanceof Question) {
				Question question = (Question) element;
				if (question instanceof SingleChoiceQuestion && question.getEcfCompetency() != null
						&& question.getEcfCompetency().getName() != null) {
					competencyToScores.put(question.getEcfCompetency().getName(), new ArrayList<>());
					ecfQuestions.add(question);
				}
			}
		}

		// For each individual
		for (int i = 0; i < answerSets.size(); i++) {
			AnswerSet answerSet = answerSets.get(i);
			Map<String, Integer> competencyToNumberOfAnswers = new HashMap<>();
			Map<String, Integer> competencyToTotalAnsweredNumbers = new HashMap<>();

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
					Integer answeredNumber = Integer.parseInt(String.valueOf(lastCharInShortName));
					if (answeredNumber > 4 || answeredNumber < 0) {
						throw new ECFException("An ECF possible answer cannot be over 4");
					}

					String questionCompetencyName = question.getEcfCompetency().getName();

					if (questionCompetencyName == null) {
						throw new ECFException("An ECF Competency must have a name");
					}
					if (competencyToNumberOfAnswers.containsKey(questionCompetencyName)) {
						Integer numberOfQuestions = competencyToNumberOfAnswers.get(questionCompetencyName);
						competencyToNumberOfAnswers.put(questionCompetencyName, numberOfQuestions + 1);

						Integer previousTotal = competencyToTotalAnsweredNumbers.get(questionCompetencyName);
						competencyToTotalAnsweredNumbers.put(questionCompetencyName,  previousTotal + answeredNumber);
					} else {
						competencyToNumberOfAnswers.put(questionCompetencyName, 1);
						competencyToTotalAnsweredNumbers.put(questionCompetencyName, answeredNumber);
					}
				}
			}

			for (String questionCompetencyName: competencyToTotalAnsweredNumbers.keySet()) {
				Integer total = competencyToTotalAnsweredNumbers.get(questionCompetencyName);
				Integer numberOfQuestions = competencyToNumberOfAnswers.get(questionCompetencyName);
				competencyToScores.get(questionCompetencyName).add(total / numberOfQuestions);
			}
		}


		ECFGlobalResult globalResult = new ECFGlobalResult();
		List<ECFGlobalCompetencyResult> globalCompetencyResults = new ArrayList<>();
		for (String competencyName : competencyToScores.keySet()) {
			ECFGlobalCompetencyResult competencyResult = new ECFGlobalCompetencyResult();
			competencyResult.setCompetencyName(competencyName);
			competencyResult.setCompetencyScores(competencyToScores.get(competencyName));
			globalCompetencyResults.add(competencyResult);
		}
		globalResult.setIndividualResults(globalCompetencyResults);
		return globalResult;
	}

	public ECFGlobalCompetencyResult getECFGlobalCompetencyResult(Survey survey, List<AnswerSet> answerSets,
			Entry<ECFCompetency, Integer> competencyToScore) throws ECFException {
		// Find the questions for this competency
		List<Question> listOfRelevantQuestions = new ArrayList<>();
		for (Element element : survey.getElements()) {
			if (element instanceof Question) {
				Question question = (Question) element;
				if (question instanceof SingleChoiceQuestion && question.getEcfCompetency() != null
						&& question.getEcfCompetency().equals(competencyToScore.getKey())) {
					listOfRelevantQuestions.add(question);
				}
			}
		}

		// Go through the answerSets and set up all the answers for this competency
		ECFGlobalCompetencyResult ecfGlobalCompetencyResult = new ECFGlobalCompetencyResult();
		ecfGlobalCompetencyResult.setCompetencyName(competencyToScore.getKey().getName());
		ecfGlobalCompetencyResult.setCompetencyTargetScore(competencyToScore.getValue());
		for (AnswerSet answerSet : answerSets) {
			ECFIndividualCompetencyResult competencyResult = new ECFIndividualCompetencyResult();
			competencyResult.setCompetencyName(competencyToScore.getKey().getName());
			competencyResult.setCompetencyTargetScore(competencyToScore.getValue());

			boolean answeredToThisCompetency = false;
			for (Question question : listOfRelevantQuestions) {
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
					Integer answeredNumber = Integer.parseInt(String.valueOf(lastCharInShortName));
					if (answeredNumber > 4 || answeredNumber < 0) {
						throw new ECFException("An ECF possible answer cannot be over 4");
					}
					competencyResult.addCompetencyScore(answeredNumber);
					answeredToThisCompetency = true;
				}
			}

			if (answeredToThisCompetency) {
				ecfGlobalCompetencyResult.addCompetencyScore(competencyResult.getCompetencyScore());
				ecfGlobalCompetencyResult.addCompetencyScoreGap(competencyResult.getCompetencyScoreGap());
			}

		}

		return ecfGlobalCompetencyResult;

	}

	/**
	 * 1/ Finds the answerSet profile. 2/ Then calculates the GAPS between this
	 * profile and the actual score
	 * 
	 * @throws ECFException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Transactional
	public ECFIndividualResult getECFIndividualResult(Survey survey, AnswerSet answerSet) throws ECFException {
		if (survey == null || !survey.getIsECF()) {
			throw new IllegalArgumentException("survey needs to be ECF to parse ECF results");
		}
		if (answerSet == null) {
			throw new IllegalArgumentException("answer set is not null");
		}

		ECFIndividualResult ecfResult = new ECFIndividualResult();
		ECFProfile answererProfile = getAnswerSetProfile(survey, answerSet);
		ecfResult.setProfileName(answererProfile.getName());

		HashMap<String, ECFIndividualCompetencyResult> competencyNameToResult = new HashMap<>();

		for (Element element : survey.getElements()) {
			if (element instanceof Question) {
				Question question = (Question) element;
				if (question instanceof SingleChoiceQuestion && question.getEcfCompetency() != null
						&& question.getEcfCompetency().getName() != null) {

					List<Answer> answers = answerSet.getAnswers(question.getId(), question.getUniqueId());
					if (answers.size() == 0)
						continue;
					Answer answer = answers.get(0);
					SingleChoiceQuestion choiceQuestion = (SingleChoiceQuestion) question;
					ECFIndividualCompetencyResult ecfCompetencyResult = null;

					if (competencyNameToResult.containsKey(question.getEcfCompetency().getName())) {
						ECFIndividualCompetencyResult previousResultSameQuestion = competencyNameToResult
								.get(question.getEcfCompetency().getName());
						ECFIndividualCompetencyResult newResult = this.getAnswerCompetencyResult(choiceQuestion, answer,
								previousResultSameQuestion);
						competencyNameToResult.put(question.getEcfCompetency().getName(), newResult);
					} else {
						ecfCompetencyResult = this.getAnswerCompetencyResult(choiceQuestion, answer, answererProfile);
						competencyNameToResult.put(question.getEcfCompetency().getName(), ecfCompetencyResult);
					}
				}
			}
		}
		ecfResult.setCompetencyResultList(
				competencyNameToResult.values().stream().sorted().collect(Collectors.toList()));
		return ecfResult;
	}

	/**
	 * Calculates the ECFCompetencyResult for the singleChoiceQuestion's answer,
	 * while taking into account a previous result for a question on the same
	 * ECFCompetency
	 * 
	 * @throws ECFException if an answer is not between 0 and 4 OR <br>
	 *                      if the answer does not correspond to the question
	 */
	private ECFIndividualCompetencyResult getAnswerCompetencyResult(SingleChoiceQuestion singleChoiceQuestion,
			Answer answer, ECFIndividualCompetencyResult previousResultSameQuestion) throws ECFException {
		PossibleAnswer answeredPossibleAnswer = singleChoiceQuestion
				.getPossibleAnswerByUniqueId(answer.getPossibleAnswerUniqueId());
		if (answeredPossibleAnswer != null) {
			char lastCharInShortName = answeredPossibleAnswer.getShortname()
					.charAt(answeredPossibleAnswer.getShortname().length() - 1);

			Integer answeredNumber = Integer.parseInt(String.valueOf(lastCharInShortName));

			if (answeredNumber > 4 || answeredNumber < 0) {
				throw new ECFException("An ECF possible answer cannot be over 4");
			}

			previousResultSameQuestion.addCompetencyScore(answeredNumber);
		} else {
			throw new ECFException("No possible answer for this unique id");
		}
		return previousResultSameQuestion;
	}

	/**
	 * Calculates the ECFCompetencyResult for the singleChoiceQuestion's answer and
	 * for the selected profile
	 * 
	 * @throws ECFException if an expected score cannot be found for the
	 *                      singleChoiceQuestion's competency and the
	 *                      answererProfile OR <br>
	 *                      if an answer is not between 0 and 4 OR <br>
	 *                      if the answer does not correspond to the question
	 */
	private ECFIndividualCompetencyResult getAnswerCompetencyResult(SingleChoiceQuestion singleChoiceQuestion,
			Answer answer, ECFProfile answererProfile) throws ECFException {
		ECFIndividualCompetencyResult ecfCompetencyResult = new ECFIndividualCompetencyResult();

		ECFCompetency ecfCompetency = singleChoiceQuestion.getEcfCompetency();
		ecfCompetencyResult.setCompetencyName(ecfCompetency.getName());

		Integer actualExpectedScore = null;
		for (ECFExpectedScore expectedScore : ecfCompetency.getECFExpectedScores()) {
			if (expectedScore.getECFExpectedScoreToProfileEid().getECFProfile().equals(answererProfile)) {
				actualExpectedScore = expectedScore.getScore();
			}
		}

		if (actualExpectedScore == null) {
			throw new ECFException("Expected score not found for this profile-competency combination");
		}

		ecfCompetencyResult.setCompetencyTargetScore(actualExpectedScore);

		PossibleAnswer answeredPossibleAnswer = singleChoiceQuestion
				.getPossibleAnswerByUniqueId(answer.getPossibleAnswerUniqueId());
		if (answeredPossibleAnswer != null) {
			char lastCharInShortName = answeredPossibleAnswer.getShortname()
					.charAt(answeredPossibleAnswer.getShortname().length() - 1);

			Integer answeredNumber = Integer.parseInt(String.valueOf(lastCharInShortName));

			if (answeredNumber > 4 || answeredNumber < 0) {
				throw new ECFException("An ECF possible answer cannot be over 4");
			}

			ecfCompetencyResult.addCompetencyScore(answeredNumber);
		} else {
			throw new ECFException("No possible answer for this unique id");
		}

		return ecfCompetencyResult;
	}

	/**
	 * Returns the ECFProfile an answerer has entered in the answerSet, for a
	 * specific ECF survey
	 * 
	 * @throws ECFException if no ECFProfile could be found answered by the user
	 */
	private ECFProfile getAnswerSetProfile(Survey survey, AnswerSet answerSet) throws ECFException {
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
	public List<ECFProfile> createDummyEcfProfiles() {
		Session session = sessionFactory.getCurrentSession();
		List<ECFProfile> ecfProfiles = new LinkedList<ECFProfile>();

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
	public List<ECFCompetency> createCompetencies(List<ECFProfile> ecfProfiles) {
		Session session = sessionFactory.getCurrentSession();
		List<ECFCompetency> ecfCompetencies = new LinkedList<ECFCompetency>();

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

		logger.info("There are " + oldECFCompetencyToNew.size() + " old competencies");
		logger.info("There are " + oldECFProfileToNew.size() + " old profiles");
		logger.info("There are " + encounteredScores.size() + " old scores");

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
	public ECFProfile getProfileByUUID(final String profileUid) {
		Session session = sessionFactory.getCurrentSession();
		
		String hql = "FROM ECFProfile E WHERE E.profileUid = :profileUid";
		Query query = session.createQuery(hql);
		query.setParameter("profileUid", profileUid);
		
		List<ECFProfile> result = query.list();
		return result.isEmpty() ? null : result.get(0);
	}

}