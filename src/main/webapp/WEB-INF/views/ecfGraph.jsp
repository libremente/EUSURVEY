<div id="canvasContainer">
	<p>
		<spring:message code="label.ECF.PleaseFindResults" />
	</p>
	<c:if test="${!print}">
		<div class="form-group">
			<label for="select-job-profiles">Profile Filter:</label> <select
				onchange="fetchECFResult()" class="form-control"
				name="select-job-profiles" id="select-job-profiles">
				<c:forEach var="profile" items="${ecfProfiles}" varStatus="loop">
					<option value="${profile.profileUid}" selected="selected">
						${profile.name}</option>
				</c:forEach>
			</select>
		</div>
	</c:if>
	<table class="table table-styled table-striped table-bordered"
		id="ecfResultTable" style="margin-bottom: 10px">
		<thead>
			<tr class="headerrow">
				<th><spring:message code="label.ECF.Competencies" /></th>
				<th><spring:message code="label.ECF.Target" /></th>
				<th><spring:message code="label.ECF.Scores" /></th>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
	<canvas id="ecfRespondentChart"></canvas>
</div>


<script type="text/javascript">
	$(document).ready(function() {
		const result = fetchECFResult();
	});
</script>
<script type="text/javascript"
	src="${contextpath}/resources/js/ecfGraph.js"></script>
