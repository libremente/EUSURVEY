
/**
 * !!!!!!!!!!!!!!!!!
 * Needs as VAR
 * contextpath
 * surveyShortname
 * uniqueCode
 * !!!!!!!!!!!!!!!!!
 */
function fetchECFResult() {
	if (contextpath.charAt(contextpath.length - 1) === '/') {
		contextpath = contextpath.slice(0,-1);
	}
	
	let profileUUID = $( "#select-job-profiles" ).val();
	let profileUUIDParameter = profileUUID != null ? "&profileUUID=" + profileUUID : "";
	
	$.ajax({
		type:'GET',
		url: contextpath + "/ecfResultJSON?answerSetId=" + uniqueCode + profileUUIDParameter,
		cache: false,
		success: function(ecfResult) {
			if (ecfResult == null) {
				setTimeout(function(){ fetchECFResult(); }, 10000);
				return;
			} else {
				changeSelectedValue(ecfResult);
				displayECFTable(ecfResult);
				displayECFChart(ecfResult);
				return ecfResult;
			}
		}
	});
}

function changeSelectedValue(result) {
	if (result && result.profileUUID) {
		$( "#select-job-profiles" ).val(result.profileUUID);
	}
}

function displayECFTable(result) {
	$("#ecfResultTable > tbody").empty();
	result.competencies.forEach(competency => {
	let gapColor = (competency.scoreGap>=0) ? 'greenScore' : 'redScore';
	$("#ecfResultTable > tbody:last-child").append('<tr><td>' + competency.name + '</td>'
			+ '<td>' + competency.targetScore + '</td>'
			+ '<td>' + '<div class="score">' + competency.score + '</div>'
			+ '<div class="gap ' + gapColor + '">&nbsp;('
			+ competency.scoreGap + ')</div>' + '</td>'
			+ '</tr>')
			});
}

function displayECFChart(result) {
	if(result) {
		profileName = result.name;		
		scores = [];
		competencies = [];
		targetScores = [];
		result.competencies.forEach(competency => {
			scores.push(competency.score);
			competencies.push(competency.name);
			targetScores.push(competency.targetScore);
		});
		
		var ctx = $("#ecfRespondentChart");
		var options = {
			scale: {
				angleLines: {
			         display: false
			       },
			        ticks: {
			            suggestedMin: 0,
			            suggestedMax: 4
			        }
				},
			maintainAspectRatio: true,
			spanGaps: false,
			elements: {
				line: {
					tension: 0.000001
				}
			},
			plugins: {
				filler: {
					propagate: false
				},
				'samples-filler-analyser': {
					target: 'chart-analyser'
				}
			}
		};
	
		var myRadarChart = new Chart(ctx, {
			type: 'radar',
			data: {
				labels: competencies,
				datasets: [{
					label: 'Your results',
					data: scores,
					backgroundColor: 'rgba(255, 99, 132, 0.2)',
					borderColor: 'rgba(255, 99, 132, 1)',
					borderWidth: 1
				},
				{
					label: 'Target results for profile ' + profileName,
					data: targetScores,
					backgroundColor: 'rgba(97, 197, 255, 0.2)',
					borderColor: 'rgba(97, 197, 255, 1)',
					borderWidth: 1
				}
				]
			},
			options: options
		});
	}
}