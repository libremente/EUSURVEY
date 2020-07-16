package com.ec.survey.tools.export;

import java.io.File;
import java.io.FileInputStream;
import org.apache.poi.util.IOUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("pdfExportCreator")
@Scope("prototype")
public class PdfExportCreator extends ExportCreator {
	
	@Override
	void exportContent(boolean sync) throws Exception {
		File file = pdfService.createAllIndividualResultsPDF(form.getSurvey(), export.getResultFilter());
		FileInputStream fis = new FileInputStream(file);
		IOUtils.copy(fis, outputStream);
		fis.close();
		file.delete();
	}
	
	@Override
	void exportStatistics() throws Exception {
		File file = pdfService.createStatisticsPDF(form.getSurvey(), export.getId().toString());
		FileInputStream fis = new FileInputStream(file);
		IOUtils.copy(fis, outputStream);
		fis.close();
		file.delete();
	}
	
	@Override
	void exportStatisticsQuiz() throws Exception {
		File file = pdfService.createStatisticsQuizPDF(form.getSurvey(), export.getId().toString());
		FileInputStream fis = new FileInputStream(file);
		IOUtils.copy(fis, outputStream);
		fis.close();
		file.delete();
	}

	@Override
	void exportAddressBook() throws Exception {}

	@Override
	void exportActivities() throws Exception {}
	
	@Override
	void exportTokens() throws Exception {}

	@Override
	void exportECFGlobalResults() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	void exportECFProfileResults() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	void exportECFOrganizationalResults() throws Exception {
		// TODO Auto-generated method stub
		
	}	

}
