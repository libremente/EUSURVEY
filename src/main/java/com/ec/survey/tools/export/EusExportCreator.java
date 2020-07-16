package com.ec.survey.tools.export;

import java.io.FileInputStream;
import org.apache.poi.util.IOUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("eusExportCreator")
@Scope("prototype")
public class EusExportCreator extends ExportCreator {
	
	@Override
	void exportContent(boolean sync) throws Exception {
		java.io.File zip = surveyService.exportSurvey(form.getSurvey().getShortname(), surveyService, true);
		IOUtils.copy(new FileInputStream(zip), outputStream);
	}
	
	@Override
	void exportStatistics() throws Exception {}
	
	@Override
	void exportStatisticsQuiz() throws Exception {}

	@Override
	void exportAddressBook() throws Exception {}

	@Override
	void exportActivities() throws Exception {}
	
	@Override
	void exportTokens() throws Exception {}

	@Override
	void exportECFGlobalResults() throws Exception {}

	@Override
	void exportECFProfileResults() throws Exception {}

	@Override
	void exportECFOrganizationalResults() throws Exception {}	

}
