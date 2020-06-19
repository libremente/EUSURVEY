
	function selectProfile(profileUid) {
		$("#select-profile-filter").val(profileUid);
		ecfResultPage = 1;
		displayCurrentPageResults();
		$("#ecfSelectContributionsTable > tbody > .selectedrow").removeClass("selectedrow");
		$("#ecfSelectContributionsTable > tbody > tr[data-profile-uid='" + profileUid + "']").addClass("selectedrow");
	}

	function fetchECFResults(pageNumber, pageSize) {
		if (contextpath.charAt(contextpath.length - 1) === '/') {
			contextpath = contextpath.slice(0,-1);
		}
		
		let orderBy = $( "#select-orderBy" ).val();
		let orderByParam = "";
		if (orderBy && orderBy != "") {
			orderByParam = "&orderBy=" + orderBy;
		}
		
		let profileComparisonUUID = $( "#select-job-profiles" ).val();
		let profileComparisonUUIDParam = "";
		if (profileComparisonUUID && profileComparisonUUID != "") {
			profileComparisonUUIDParam = "&profileComparison=" + profileComparisonUUID;
		}
		
		let profileFilterUUID = $( "#select-profile-filter" ).val();
		let profileFilterUUIDParam = "";
		if (profileFilterUUID && profileFilterUUID != "") {
			profileFilterUUIDParam = "&profileFilter=" + profileFilterUUID;
		}
		
			$.ajax({
				type:'GET',
				url: contextpath + "/" + surveyShortname + "/management/ecfResultsJSON"
				+"?pageNumber=" + pageNumber
				+"&pageSize=" + pageSize
				+ profileComparisonUUIDParam
				+ profileFilterUUIDParam
				+ orderByParam,
				cache: false,
				success: function(ecfGlobalResult) {
					if (ecfGlobalResult == null) {
						setTimeout(function(){ fetchECFResults(pageNumber, pageSize); }, 10000);
						return;
					} else {
						displayECFTable(ecfGlobalResult);
						displayECFChart(ecfGlobalResult);
						displayNumberOfResults(ecfGlobalResult)
						return ecfGlobalResult;
					}
				}
			});
	}
	
	function displayNumberOfResults(ecfGlobalResult) {
		if (ecfGlobalResult && ecfGlobalResult.numberOfResults) {
			$("#individualNumberOfAnswers").text(ecfGlobalResult.numberOfResults);
		}
	}
	
	function displayECFTable(ecfGlobalResult) {
		$("#ecfResultTable > tbody").empty();
		let totalResults = ecfGlobalResult.totalResults;
		ecfMaxResultPage = ecfGlobalResult.numberOfPages; 
		
		displayPreviousPageLinks();
		displayCurrentPageLink();
		displayNextPageLinks();
		
		let totalTarget = (totalResults.totalTargetScore != null && totalResults.totalTargetScore != undefined) ? totalResults.totalTargetScore : "";
		
		$("#ecfResultTable > tbody:last-child")
		.append('<tr class="bodyrow">'
				+ '<th>' + totalResults.competencyName + '</th>'
				+ '<th>' + totalTarget + '</th>'
				+ displayScoresColumn(totalResults.totalScores, totalResults.totalGaps)
				+ '</tr>');
		
		
		ecfGlobalResult.individualResults.forEach(individualResult => {
			let targetScore = "";
			if (individualResult.targetScore) {
				targetScore = individualResult.targetScore;
			}
			
			$("#ecfResultTable > tbody:last-child")
			.append('<tr class="bodyrow">'
					+ '<th>' + individualResult.name + '</th>'
					+ '<th>' + targetScore + '</th>'
					+ displayScoresColumn(individualResult.scores, individualResult.scoreGaps)
					+ '</tr>');
			displayIndividuals(individualResult.participantNames);
		});
		
		
	}
	
	function displayScoresColumn(competencyScores, competencyScoreGaps) {
		let restOfARow = '';
		let i = 0;
		competencyScores.forEach(competencyScore => {
			restOfARow = restOfARow.concat(oneScoreWithGapTH(competencyScore,competencyScoreGaps[i]));
			i++;
		});
		return restOfARow;
	}
	
	function oneScoreWithGapTH(score, gap) {
		let oneTh = '';
		let displayGap = $('#display-gap').is(":checked");
		let displayScore = $('#display-score').is(":checked");
		
		let scoreClass = displayScore ? 'score' : 'score hidden';
		let gapClass = displayGap ? 'gap ' : 'gap hidden ';
		let gapColor = (gap>=0) ? 'greenScore' : 'redScore';
		let gapDisplayed = (gap>0) ? '+' + gap : gap; 
		
		let scoreDiv = '<div class="'
			+ scoreClass
			+ '">'
			+ score 
			+ '</div>';
		
		let gapDiv = '';
		
		if (gap != null && gap != undefined) {
			gapDiv = '<div class="' 
				+ gapClass
				+ gapColor 
				+ '">&nbsp; (' 
				+ gapDisplayed
				+ ')</div>';
		}
		
		oneTh = oneTh.concat('<th><div>'
				+ scoreDiv
				+ gapDiv
				+ '</div></th>');
		
		return oneTh;
	}
	
	function displayECFChart(ecfResult) {
		
	}
	
	function toggleDisplayScore() {
		if ($('#display-score').is(":checked")) {
			$(".score").removeClass("hidden");
		} else {
			$(".score").addClass("hidden");
		}
	}
	
	function toggleDisplayGap() {
		if ($('#display-gap').is(":checked")) {
			$(".gap").removeClass("hidden");
		} else {
			$(".gap").addClass("hidden");
		}
	}
	
	function nextResultPage() {
		if (ecfResultPage < ecfMaxResultPage) {
			ecfResultPage = ecfResultPage + 1;
			displayCurrentPageResults();
		}
	}
	
	function previousResultPage() {
		if (ecfResultPage > 1) {
			ecfResultPage = ecfResultPage - 1;
			displayCurrentPageResults();
		}
	}
	
	function selectResultPage(pageNumber) {
		ecfResultPage = pageNumber;
		displayCurrentPageResults();
	}
	
	function displayCurrentPageLink() {
		$("a.currentResultPage").text(ecfResultPage);
	}
	
	function displayPreviousPageLinks() {
		$(".previousPagesLinks").empty();
		if (ecfResultPage > 1) {
			$(".previousPagesLinks").append('<a onclick="previousResultPage()" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a>');
		}
		for (let i = 1; i < ecfResultPage; i++){
			$(".previousPagesLinks").append('<a onclick="selectResultPage(' + i +')">' + i +'</a>');
		}
	}
	
	function displayNextPageLinks() {
		$(".nextPagesLinks").empty();
		console.log(ecfResultPage + 1);
		console.log(ecfMaxResultPage);
		for (let i = (ecfResultPage + 1); i <= ecfMaxResultPage; i++){
			$(".nextPagesLinks").append('<a onclick="selectResultPage(' + i +')">' + i +'</a>');
		}
		
		if (ecfResultPage < ecfMaxResultPage) {
			$(".nextPagesLinks").append('<a onclick="nextResultPage()" aria-label="Next"><span aria-hidden="true">&raquo;</span></a>');
		}
	}
	
	function displayCurrentPageResults() {
		fetchECFResults(ecfResultPage, 10);
	}
	
	function displayIndividuals(participantNames) {
		$("#ecfResultTable > thead > .headerrow > th.individual").remove();
		let i = (ecfResultPage - 1) * 10;
		participantNames.forEach(participantName => {
			$("#ecfResultTable > thead > .headerrow").append('<th class="individual">' + participantName +'</th>');
			i++;
		});
	}
// ======================= PAGE 2 ============================= // 
	function selectProfile2(profileUid) {
		$("#select-job-profiles2").val(profileUid);
		fetchECFProfileAssessmentResults();
		$("#ecfSelectContributionsTable2 > tbody > .selectedrow").removeClass("selectedrow");
		$("#ecfSelectContributionsTable2 > tbody > tr[data-profile-uid='" + profileUid + "']").addClass("selectedrow");
	}
	
	function fetchECFProfileAssessmentResults() {
		if (contextpath.charAt(contextpath.length - 1) === '/') {
			contextpath = contextpath.slice(0,-1);
		}
		
		let profileUUID = $( "#select-job-profiles2" ).val();
			$.ajax({
				type:'GET',
				url: contextpath + "/" + surveyShortname + "/management/ecfProfileAssessmentResultsJSON"
				+"?profile=" + profileUUID,
				cache: false,
				success: function(profileAssessmentResult) {
					if (profileAssessmentResult == null) {
						setTimeout(function(){ fetchECFProfileAssessmentResults(); }, 10000);
						return;
					} else {
						displayECFTable2(profileAssessmentResult);
						return profileAssessmentResult;
					}
				}
			});
	}
	
	function displayECFTable2(profileAssessmentResult) {
		$("#ecfResultTable2 > tbody").empty();
		profileAssessmentResult.competencyResults.forEach(competencyResult => {
			let targetScore = "";
			let averageScore = "";
			
			if (competencyResult.competencyTargetScore) {
				targetScore = competencyResult.competencyTargetScore;
			}
			
			if (competencyResult.competencyAverageScore) {
				averageScore = competencyResult.competencyAverageScore;
			}
			
			let competencyMaxScoreDiv = '';
				
			if (competencyResult.competencyMaxScore) {
				competencyMaxScoreDiv = '<div class="'
					+ 'score'
					+ '">'
					+ competencyResult.competencyMaxScore 
					+ '</div>';
			}
			
			let gapDiv = '';
			
			let gap = competencyResult.competencyScoreGap;
			let gapColor = (gap>=0) ? 'greenScore' : 'redScore';
			let gapDisplayed = (gap>0) ? '+' + gap : gap; 
			
			if (gap != null && gap != undefined) {
				gapDiv = '<div class="' 
					+ 'gap '
					+ gapColor 
					+ '">&nbsp; (' 
					+ gapDisplayed
					+ ')</div>';
			}
			
			$("#ecfResultTable2 > tbody:last-child")
			.append('<tr class="bodyrow">'
					+ '<th>' + competencyResult.competencyName + '</th>'
					+ '<th>' + targetScore + '</th>'
					+ '<th>' + averageScore + '</th>'
					+ '<th>' + '<div>' + competencyMaxScoreDiv + gapDiv + '</div>' + '</th>'
					+ '</tr>');
		});
	}
	// ======================= PAGE 3 ============================= // 
	function fetchECFOrganizationalResults() {
		if (contextpath.charAt(contextpath.length - 1) === '/') {
			contextpath = contextpath.slice(0,-1);
		}
		
		$.ajax({
			type:'GET',
			url: contextpath + "/" + surveyShortname + "/management/ecfOrganizationalResultsJSON",
			cache: false,
			success: function(organizationalResult) {
				if (organizationalResult == null) {
					setTimeout(function(){ fetchECFOrganizationalResults(); }, 10000);
					return;
				} else {
					displayECFMaxChartByOrganizationalResult(organizationalResult);
					displayECFAverageChartByOrganizationalResult(organizationalResult);
					return organizationalResult;
				}
			}
		});
	}	
	
	function displayECFAverageChartByOrganizationalResult(organizationalResult) {
		if (organizationalResult) {
			averageScores = [];
			competencies = [];
			averageTargetScores = [];
			organizationalResult.competencyResults.forEach(competencyResult => {
				averageScores.push(competencyResult.competencyAverageScore);
				competencies.push(competencyResult.competencyName);
				averageTargetScores.push(competencyResult.competencyAverageTarget);
			});
			
			displayECFChart("Average scores vs Average target scores", "#ecfAverageChart", averageScores, averageTargetScores, competencies, "Average scores", "Average target scores");
		}
	}
	
	
	function displayECFMaxChartByOrganizationalResult(organizationalResult) {
		if (organizationalResult) {
			maxScores = [];
			competencies = [];
			maxTargetScores = [];
			organizationalResult.competencyResults.forEach(competencyResult => {
				maxScores.push(competencyResult.competencyMaxScore);
				competencies.push(competencyResult.competencyName);
				maxTargetScores.push(competencyResult.competencyMaxTarget);
			});
			
			displayECFChart("Max scores vs Max target scores", "#ecfMaxChart", maxScores, maxTargetScores, competencies, "Max scores", "Max target scores");
		}
	}
	
	function displayECFChart(chartTitle, canvasIdCssSelector, firstLineArray, secondLineArray, headerNamesArray, firstLineLegendName, secondLineLegendName) {
		if (chartTitle && canvasIdCssSelector && firstLineArray && secondLineArray && headerNamesArray && firstLineLegendName && secondLineLegendName) {
			if (firstLineArray.length === secondLineArray.length && secondLineArray.length === headerNamesArray.length) {
				var ctx = $(canvasIdCssSelector);
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
					title: {
						display: true,
				        text: chartTitle
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
						labels: headerNamesArray,
						datasets: [{
							label: firstLineLegendName,
							data: firstLineArray,
							backgroundColor: 'rgba(255, 99, 132, 0.2)',
							borderColor: 'rgba(255, 99, 132, 1)',
							borderWidth: 1
						},
						{
							label: secondLineLegendName,
							data: secondLineArray,
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
	}
	