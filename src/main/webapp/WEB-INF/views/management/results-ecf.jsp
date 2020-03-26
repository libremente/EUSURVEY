<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:choose>
	<c:when test="${forPDF != null}">
		<div id="ecf-results" style="margin-top: 70px">
	</c:when>
	<c:when test="${publication != null}">
		<div id="ecf-results" class="hidden" style="margin-top: 50px">
	</c:when>
	<c:otherwise>
		<div id="ecf-results" class="hidden" style="margin-top: 10px">
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
				<spring:message code="label.ECF.Results" />
			</h2>


			<div class="form-group">
				<label for="select-job-profiles">Display options:</label>
				<div class="under">
					<div class="checkbox-inline">
						<input id="display-score" checked onchange="toggleDisplayScore()"
							type="checkbox">Display score
					</div>
					<div class="checkbox-inline">
						<input id="display-gap" checked onchange="toggleDisplayGap()"
							type="checkbox">Display gaps
					</div>
				</div>
			</div>
			<div class="form-group">
				<label for="select-job-profiles">Profile Filter:</label> <select
					onchange="displayCurrentPageResults()" class="form-control"
					name="select-job-profiles" id="select-job-profiles">
					<c:forEach var="profile" items="${ecfProfiles}" varStatus="loop">
						<option value="${profile.profileUid}" selected="selected">
							${profile.name}</option>
					</c:forEach>
					<option value="" selected="selected">All Job Profile</option>
				</select>
			</div>
		</div>
	</div>

	<div class="row">
		<div id="resultsContainer"
			class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
			<table class="table table-styled table-striped table-bordered"
				id="ecfResultTable">
				<thead>
					<tr class="headerrow">
						<th>Competencies</th>
						<th>Target</th>
						<c:if test="${!empty ecfGlobalResult.individualResults}">
							<c:forEach var="competencyScore"
								items="${ecfGlobalResult.individualResults[0].competencyScores}"
								varStatus="loop">
								<th class="individual">Individual ${loop.index + 1}</th>
							</c:forEach>
						</c:if>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="individualResult"
						items="${ecfGlobalResult.individualResults}" varStatus="loop">
						<tr class="bodyrow">
							<th>${individualResult.competencyName}</th>
							<th>${individualResult.competencyTargetScore}</th>
							<c:forEach var="competencyScore"
								items="${individualResult.competencyScores}" varStatus="loop">
								<th>
									<div>
										<div class="score">${competencyScore}</div>
										<c:if test="${!empty individualResult.competencyScoreGaps}">
											<div
												class=${individualResult.competencyScoreGaps[loop.index]>=0 ? "score greenScore" : "score redScore"}>
												&nbsp; (${individualResult.competencyScoreGaps[loop.index]})</div>
										</c:if>
									</div>
								</th>
							</c:forEach>
						</tr>
					</c:forEach>
				</tbody>
			</table>
			<c:if test = "${ecfGlobalResult.numberOfPages > 1}">
				<div class="col-md-4 col-md-offset-5">
					<nav aria-label="table-navigator">
						<ul class="pagination">
							<li id="previousPagesLinks" ></li>
							<li class="active" ><a id="currentResultPage">1</a>
							</li>
							<li id="nextPagesLinks">
								<c:forEach var = "i" begin = "${ecfGlobalResult.pageNumber + 1}" end = "${ecfGlobalResult.numberOfPages}">
	         						<a onclick="selectResultPage(${i})"><c:out value = "${i}"/></a>
	      						</c:forEach>
	      						<a onclick="nextResultPage()"
								aria-label="Next"><span aria-hidden="true">&raquo;</span></a>
							</li>
						</ul>
					</nav>
				</div>
			</c:if>
		</div>
	</div>
</div>
</div>


<script>
	var ecfResultPage = 1;
	var surveyShortname = "${surveyShortname}";
	var ecfMaxResultPage = "${ecfGlobalResult.numberOfPages}";
</script>
<script type="text/javascript"
	src="${contextpath}/resources/js/results-ecf.js"></script>
