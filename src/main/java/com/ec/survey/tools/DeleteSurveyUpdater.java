package com.ec.survey.tools;

import com.ec.survey.service.SurveyService;
import com.ec.survey.service.SystemService;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.annotation.Resource;

@Service("deleteSurveysWorker")
@Scope("singleton")
public class DeleteSurveyUpdater implements Runnable {

	protected static final Logger logger = Logger.getLogger(DeleteSurveyUpdater.class);
	
	@Resource(name="surveyService")
	private SurveyService surveyService;
	
	@Resource(name="systemService")
	private SystemService systemService;
	
	@Override
	public void run() {
		int lastId = 0;
		try {
			logger.debug("DeleteSurveyUpdater started");			
			List<Integer> surveys = surveyService.getSurveysMarkedDeleted();
			
			for (Integer id : surveys) {
				lastId = id;
				surveyService.delete(id, true, -1);
			}			
			
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
			systemService.sendAdminErrorMessage("Error during deletion of Survey " + lastId + " " + e.getLocalizedMessage());
		}		
		logger.debug("DeleteSurveyUpdater completed");
	}
	
}
