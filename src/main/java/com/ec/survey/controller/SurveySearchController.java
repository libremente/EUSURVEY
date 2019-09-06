package com.ec.survey.controller;

import com.ec.survey.exception.ForbiddenURLException;
import com.ec.survey.exception.InvalidURLException;
import com.ec.survey.model.Archive;
import com.ec.survey.model.ArchiveFilter;
import com.ec.survey.model.DeletedSurveysFilter;
import com.ec.survey.model.KeyValue;
import com.ec.survey.model.SqlPagination;
import com.ec.survey.model.SurveyFilter;
import com.ec.survey.model.administration.GlobalPrivilege;
import com.ec.survey.model.administration.User;
import com.ec.survey.model.survey.Survey;
import com.ec.survey.service.AdministrationService;
import com.ec.survey.service.SessionService;
import com.ec.survey.service.SurveyService;
import com.ec.survey.service.mapping.PaginationMapper;
import com.ec.survey.tools.ConversionTools;
import com.ec.survey.tools.NotAgreedToTosException;
import com.ec.survey.tools.RestoreExecutor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class SurveySearchController extends BasicController {
	
	@Resource(name="administrationService")
	private AdministrationService administrationService;
	
	@Resource(name="surveyService")
	private SurveyService surveyService;
	
	@Resource(name="sessionService")
	private SessionService sessionService;
    
    @Autowired
	protected PaginationMapper paginationMapper;    
	
	@RequestMapping(value = "/administration/surveysearch", method = {RequestMethod.GET, RequestMethod.HEAD})
	public ModelAndView surveysearch(HttpServletRequest request, Model model, Locale locale) {
		ModelAndView result = new ModelAndView("administration/surveysearch");
		result.addObject("filter", new SurveyFilter());
		result.addObject("archivedfilter", new ArchiveFilter());
    	
    	if (request.getParameter("deleted") != null)
    	{
    		result.addObject("deleted", true);
    		DeletedSurveysFilter filter = (DeletedSurveysFilter) request.getSession().getAttribute("lstdeletedfilter");
    		if (filter == null) filter = new DeletedSurveysFilter();
        	result.addObject("deletedfilter", filter);
        	result.addObject("mode", "deleted");
    	} else {
        	result.addObject("deletedfilter", new DeletedSurveysFilter());
    	}
    	
    	List<KeyValue> domains = ldapDBService.getDomains(true, true, resources, locale);
		result.addObject("domains", domains);
    	
    	return result;
	}
	
	@RequestMapping(value = "/administration/surveysearch", method = {RequestMethod.POST})
	public ModelAndView surveysearchPOST(HttpServletRequest request, Model model, Locale locale) throws NotAgreedToTosException {	
		
		String mode = request.getParameter("surveys");
		
		SurveyFilter filter = new SurveyFilter();	
		ArchiveFilter archivedFilter = new ArchiveFilter();	
		DeletedSurveysFilter deletedSurveysFilter = new DeletedSurveysFilter();	
		
		if (mode.equalsIgnoreCase("archived"))
		{ 
			archivedFilter.setShortname(request.getParameter("archiveshortname"));
			archivedFilter.setTitle(request.getParameter("archivetitle"));
			archivedFilter.setOwner(request.getParameter("archiveowner"));
			archivedFilter.setCreatedFrom(ConversionTools.getDate(request.getParameter("archivecreatedFrom")));
			archivedFilter.setCreatedTo(ConversionTools.getDate(request.getParameter("archivecreatedTo")));
			archivedFilter.setArchivedFrom(ConversionTools.getDate(request.getParameter("archivearchivedFrom")));
			archivedFilter.setArchivedTo(ConversionTools.getDate(request.getParameter("archivearchivedTo")));
			
			request.getSession().setAttribute("lstarchivefilter", archivedFilter);
		} else if (mode.equalsIgnoreCase("deleted"))
		{ 
			deletedSurveysFilter.setShortname(request.getParameter("deletedshortname"));
			deletedSurveysFilter.setTitle(request.getParameter("deletedtitle"));
			deletedSurveysFilter.setOwner(request.getParameter("deletedowner"));
			deletedSurveysFilter.setCreatedFrom(ConversionTools.getDate(request.getParameter("deletedcreatedFrom")));
			deletedSurveysFilter.setCreatedTo(ConversionTools.getDate(request.getParameter("deletedcreatedTo")));
			deletedSurveysFilter.setDeletedFrom(ConversionTools.getDate(request.getParameter("deleteddeletedFrom")));
			deletedSurveysFilter.setDeletedTo(ConversionTools.getDate(request.getParameter("deleteddeletedTo")));
			
			request.getSession().setAttribute("lstdeletedfilter", deletedSurveysFilter);
		} else {
			filter.setUser(sessionService.getCurrentUser(request));
			filter.setShortname(request.getParameter("shortname"));
			filter.setUid(request.getParameter("uid"));
			filter.setTitle(request.getParameter("title"));
			filter.setOwner(request.getParameter("owner"));
			filter.setPublishedFrom(ConversionTools.getDate(request.getParameter("publishedFrom")));
			filter.setPublishedTo(ConversionTools.getDate(request.getParameter("publishedTo")));
			filter.setFirstPublishedFrom(ConversionTools.getDate(request.getParameter("firstPublishedFrom")));
			filter.setFirstPublishedTo(ConversionTools.getDate(request.getParameter("firstPublishedTo")));					
	    	request.getSession().setAttribute("surveysearchfilter", filter);
		}
		
		ModelAndView result = new ModelAndView("administration/surveysearch");
    	result.addObject("mode", mode);
    	result.addObject("filter", filter);  
    	result.addObject("archivedfilter", archivedFilter);
    	result.addObject("deletedfilter", deletedSurveysFilter);
    	
    	List<KeyValue> domains = ldapDBService.getDomains(true, true, resources, locale);
		result.addObject("domains", domains);
    	
    	return result;
	}
	
	@RequestMapping(value = "/administration/surveysearchJSON", method = {RequestMethod.GET, RequestMethod.HEAD})
	public @ResponseBody List<Survey> resultsJSON(HttpServletRequest request) {
		
		try {
		
			String rows = request.getParameter("rows");			
			if (rows == null) return null;			
			String page = request.getParameter("page");			
			if (page == null) return null;
					
			SurveyFilter filter = (SurveyFilter) request.getSession().getAttribute("surveysearchfilter");
			if (filter == null) return null;
			
            SqlPagination sqlPagination = new SqlPagination(Integer.parseInt(page), Integer.parseInt(rows));            
			List<Survey> surveys = surveyService.getSurveysIncludingPublicationDates(filter, sqlPagination);
			
			for (Survey survey: surveys)
			{
				survey.setTitle(survey.cleanTitle());
				survey.setNumberOfDrafts(answerService.getNumberOfDrafts(survey.getId()));
			}
			
			return surveys;
		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
		}
		
		return null;
	}
	
	
	@RequestMapping(value = "/administration/archivedsurveysjson", method = {RequestMethod.GET, RequestMethod.HEAD})
	public @ResponseBody List<Archive> archivedsurveysjson(HttpServletRequest request) {
		
		int itemsPerPage = -1;
		int page = -1;
		
		ArchiveFilter filter = (ArchiveFilter) request.getSession().getAttribute("lstarchivefilter");
		if (filter == null) filter = new ArchiveFilter();
		
		if(request.getParameter("rows") != null && request.getParameter("page") != null)
		{
			String itemsPerPageValue = request.getParameter("rows");		
			itemsPerPage = Integer.parseInt(itemsPerPageValue);
			
			String pageValue = request.getParameter("page");		
			page = Integer.parseInt(pageValue);
		}
	
		List<Archive> archives = null;
		
		archives = archiveService.getAllArchives(filter, page, itemsPerPage, false);
		
		return archives;
	}
	
	
	@RequestMapping(value = "/administration/deletedsurveysjson", method = {RequestMethod.GET, RequestMethod.HEAD})
	public @ResponseBody List<Survey> deletedsurveysjson(HttpServletRequest request) {
		
		int itemsPerPage = -1;
		int page = -1;
		
		DeletedSurveysFilter filter = (DeletedSurveysFilter) request.getSession().getAttribute("lstdeletedfilter");
		if (filter == null) filter = new DeletedSurveysFilter();
		
		if(request.getParameter("rows") != null && request.getParameter("page") != null)
		{
			String itemsPerPageValue = request.getParameter("rows");		
			itemsPerPage = Integer.parseInt(itemsPerPageValue);
			
			String pageValue = request.getParameter("page");		
			page = Integer.parseInt(pageValue);
		}
	
		List<Survey> surveysfromdb = surveyService.getDeletedSurveys(filter, page, itemsPerPage);
		List<Survey> surveys = new ArrayList<>();
		for (Survey original: surveysfromdb)
		{
			Survey survey = new Survey();
			survey.setId(original.getId());
			survey.setUniqueId(original.getUniqueId());
			survey.setShortname(original.getShortname());
			survey.setTitle(original.cleanTitle());
			survey.setCreated(original.getCreated());
			
			User user = new User();			
			user.setId(original.getOwner().getId());
			user.setLogin(original.getOwner().getLogin());
			user.setDisplayName(original.getOwner().getDisplayName());
			survey.setOwner(user);
			
			survey.setNumberOfAnswerSetsPublished(original.getNumberOfAnswerSetsPublished());
			survey.setDeleted(original.getDeleted());
		
			surveys.add(survey);
		}
		
		return surveys;
	}
	
	@RequestMapping(value = "/administration/changeowner", method = {RequestMethod.POST})
	public @ResponseBody boolean changeowner(HttpServletRequest request, Model model, Locale locale) throws NotAgreedToTosException {	
		User u = sessionService.getCurrentUser(request);
		
		if (u.getGlobalPrivileges().get(GlobalPrivilege.SystemManagement) < 2)
		{
			return false;
		}
		
		String ownerid = request.getParameter("userid");
		String surveyuid = request.getParameter("surveyuid");
		
		if (ownerid != null && ownerid.length() > 0 && surveyuid != null && surveyuid.length() > 0)
		{
			return surveyService.changeOwner(surveyuid, Integer.parseInt(ownerid), u.getId());
		}
		
		return false;
	}
	
	@RequestMapping(value = "/archive/restore/{id}")
	public ModelAndView restore(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		String alias = request.getParameter("alias");
		
		if (useworkerserver.equalsIgnoreCase("true") && isworkerserver.equalsIgnoreCase("false"))
		{
			logger.info("calling worker server for restoring archive " + archive.getId());
			
			try {
			
				URL workerurl = new URL(workerserverurl + "worker/startRestore/" + archive.getId());
				URLConnection wc = workerurl.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(wc.getInputStream()));
				String inputLine;
				StringBuilder result = new StringBuilder();
				while ((inputLine = in.readLine()) != null) result.append(inputLine);
				in.close();
				
				if (!result.toString().equals("OK"))
				{
					logger.error("calling worker server for restoring archive " + archive.getId() + " returned " + result);
					return null;
				}
				
				return new ModelAndView("redirect:/dashboard");
			
			} catch (ConnectException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		} 		
		
		RestoreExecutor restore = (RestoreExecutor) context.getBean("restoreExecutor"); 
		restore.init(archive, alias, u);
		restore.prepare();
		taskExecutorLongRestore.execute(restore);
						
		return new ModelAndView("redirect:/dashboard");
	}
	
	@RequestMapping(value = "/archive/surveypdf/{id}")
	public ModelAndView surveypdf(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + ".pdf");
		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + ".pdf");
		
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/pdf");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/archive/resultspdf/{id}")
	public ModelAndView results(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + "results.zip");		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "results.zip");
		
		if (!file.exists())
		{
			file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "results.pdf");
		}
		
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/pdf");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/archive/resultsxls/{id}")
	public ModelAndView resultsxls(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + "results.xls");		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "results.xls");
		
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/msexcel");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/archive/resultsxlszip/{id}")
	public ModelAndView resultsxlszip(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + "results.xls.zip");		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "results.xls.zip");
		
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/zip");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/archive/statspdf/{id}")
	public ModelAndView stats(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + "statistics.pdf");		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "statistics.pdf");
			
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/pdf");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/archive/statsxls/{id}")
	public ModelAndView statsdoc(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Archive archive = archiveService.get(Integer.parseInt(id));
		
		if (archive == null || !archive.getFinished() || archive.getError() != null)
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(archive.getUserId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		java.io.File file = fileService.getArchiveFile(archive.getSurveyUID(), archive.getSurveyUID() + "statistics.xls");		
		if (!file.exists()) file = new java.io.File(archiveFileDir + archive.getSurveyUID() + "statistics.xls");
		
		if (file.exists())
		{
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition","attachment; filename=\"" + file.getName() +"\"");
			response.setContentType("application/msexcel");
			
			try {
				FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
				return null;
			} catch (FileNotFoundException e) {
				logger.error(e.getLocalizedMessage(), e);
			} catch (ClientAbortException e) {
				//ignore
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
		
		throw new InvalidURLException();
	}
	
	@RequestMapping(value = "/administration/restoredeleted/{id}")
	public ModelAndView restoredeleted(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Survey survey = surveyService.getSurvey(Integer.parseInt(id));
		
		if (survey == null || !survey.getIsDeleted())
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(survey.getOwner().getId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
		
		String alias = request.getParameter("alias");
		surveyService.unmarkDeleted(survey.getId(), alias);
		
		return new ModelAndView("redirect:/" + survey.getShortname() + "/management/overview");
	}
	
	@RequestMapping(value = "/administration/finallydelete/{id}")
	public ModelAndView finallydelete(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Survey survey = surveyService.getSurvey(Integer.parseInt(id));
		
		if (survey == null || !survey.getIsDeleted())
		{
			throw new InvalidURLException();
		}
		
		User u = sessionService.getCurrentUser(request);
		if (!u.getId().equals(survey.getOwner().getId()))
		{
			if (u.getGlobalPrivileges().get(GlobalPrivilege.FormManagement) < 2)
			{
				throw new ForbiddenURLException();
			}
		}
	
		surveyService.delete(survey.getId(), true, u.getId());
		
		return new ModelAndView("redirect:/administration/surveysearch?deleted=true");
	}	
}
