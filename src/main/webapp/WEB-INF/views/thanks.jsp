<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!DOCTYPE html>
<html>
<head>
	<title>EUSurvey - <spring:message code="label.Thanks" /></title>	
	<%@ include file="includes.jsp" %>
	<script type="text/javascript" src="${contextpath}/resources/js/Chart.min.js?version=<%@include file="version.txt" %>"></script>
	<script type="text/javascript" src="${contextpath}/resources/js/runner.js?version=<%@include file="version.txt" %>"></script>
	<script type="text/javascript"> 
		$(function() {					
			clearAllCookies('${surveyprefix}');
			<c:if test="${redirect != null}">
				window.location = "${redirect}";
			</c:if>		
		});			
	</script>
	
	<c:if test='${form.survey.skin != null && form.survey.skin.name.equals("New Official EC Skin")}'>
		<link href="${contextpath}/resources/css/ecnew.css" rel="stylesheet" type="text/css"></link>
	</c:if>
	<c:if test='${form.survey.skin != null && form.survey.skin.name.equals("ECA Skin")}'>
		<link href="${contextpath}/resources/css/ecanew.css" rel="stylesheet" type="text/css"></link>
	</c:if>
</head>
<body>
	<div class="page-wrap">
		<c:choose>
			<c:when test='${responsive == null && form.survey.skin != null && form.survey.skin.name.equals("New Official EC Skin")}'>
				<div id="top-page" style="width: 1302px; margin-left: auto; margin-right: auto;">
				<%@ include file="headerecnew.jsp" %>	 
			</c:when>
			<c:when test='${responsive == null && form.survey.skin != null && form.survey.skin.name.equals("ECA Skin")}'>
				<div id="top-page" style="width: 1302px; margin-left: auto; margin-right: auto;">
				<%@ include file="headerECAnew.jsp" %>	 
			</c:when>
			<c:otherwise>
				<%@ include file="header.jsp" %>	 
			</c:otherwise>
		</c:choose>
	
		<div class="fullpage">
			<%@ include file="thanksinner.jsp" %>
			<%@ include file="generic-messages.jsp" %>
		</div>
	</div>
	
	<c:choose>
		<c:when test='${responsive == null && form.survey.skin != null && form.survey.skin.name.equals("New Official EC Skin")}'>
			</div>  
			<div style="text-align: center">
				<%@ include file="footerNoLanguagesECnew.jsp" %>
			</div>
		</c:when>
		<c:when test='${responsive == null && form.survey.skin != null && form.survey.skin.name.equals("ECA Skin")}'>
			</div>  
			<div style="text-align: center">
				<%@ include file="footerNoLanguagesECAnew.jsp" %>
			</div>
		</c:when>
		<c:when test="${responsive == null && runnermode == true}">
			<%@ include file="footerSurveyLanguages.jsp" %>
		</c:when>
		<c:otherwise>
			<%@ include file="footer.jsp" %>
		</c:otherwise>
	</c:choose>		

</body>
</html>
