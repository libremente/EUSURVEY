<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:choose>
	<c:when test="${forPDF != null}">
		<div id="ecf-results2" style="margin-top: 70px">
	</c:when>
	<c:when test="${publication != null}">
		<div id="ecf-results2" class="hidden" style="margin-top: 50px">
	</c:when>
	<c:otherwise>
		<div id="ecf-results2" class="hidden" style="margin-top: 10px">
			<div style="text-align: center; margin-bottom: 10px;"></div>
	</c:otherwise>
</c:choose>

<div class="container-fluid">
	<div class="row">
		<div class="col-xs-12 col-sm-9 col-md-4 col-lg-4">
			<h1>
				<spring:message code="label.ECF.Big" />
			</h1>
			<h2>
				<spring:message code="label.ECF.Results2" />
			</h2>

			<div class="form-group">
				<label for="select-job-profiles2">Profile Filter:</label> <select
					onchange="fetchECFProfileAssessmentResults()" class="form-control"
					name="select-job-profiles2" id="select-job-profiles2">
					<c:forEach var="profile" items="${ecfProfiles}" varStatus="loop">
						<option value="${profile.profileUid}" selected="selected">
							${profile.name}</option>
					</c:forEach>
					<option value="" selected="selected">All Job Profile</option>
				</select>
			</div>
			<label class="labelInfoDarker" >
				<spring:message code="label.ECF.NbInviduals" />&nbsp;
				<div class="numberOfAnswers" id="numberOfAnswers">${ecfProfileResult.numberOfAnswers}</div>
			</label>
		</div>
	</div>

	<div class="row">
		<div id="resultsContainerMax"
			class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
			<table class="table table-styled table-striped table-bordered"
				id="ecfResultTable2">
				<thead>
					<tr class="headerrow">
						<th><spring:message code="label.ECF.Competencies" /></th>
						<th><spring:message code="label.ECF.Target" /></th>
						<th><spring:message code="label.ECF.Average" /></th>
						<th><spring:message code="label.ECF.Max" /></th>
					</tr>
				</thead>
				<tbody>
					<c:if test="${!empty ecfProfileResult.competencyResults}">
						<c:forEach var="competencyResult"
							items="${ecfProfileResult.competencyResults}">
							<tr class="bodyrow">
								<th>${competencyResult.competencyName}</th>
								<th>${competencyResult.competencyTargetScore}</th>
								<th>${competencyResult.competencyAverageScore}</th>
								<th>${competencyResult.competencyMaxScore}</th>
							</tr>
						</c:forEach>
					</c:if>
				</tbody>
			</table>
		</div>
	</div>
</div>
</div>


<script>
	var ecfResultPage = 1;
	var surveyShortname = "${surveyShortname}";
</script>
<script type="text/javascript"
	src="${contextpath}/resources/js/results-ecf.js"></script>
