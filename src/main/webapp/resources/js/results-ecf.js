	function fetchECFResults(pageNumber, pageSize) {
		if (contextpath.charAt(contextpath.length - 1) === '/') {
			contextpath = contextpath.slice(0,-1);
		}
		
		let profileUUID = $( "#select-job-profiles" ).val();
			$.ajax({
				type:'GET',
				url: contextpath + "/" + surveyShortname + "/management/ecfResultsJSON"
				+"?pageNumber=" + pageNumber
				+"&pageSize=" + pageSize
				+"&profile=" + profileUUID,
				cache: false,
				success: function(ecfGlobalResult) {
					if (ecfGlobalResult == null) {
						setTimeout(function(){ fetchECFResults(pageNumber, pageSize); }, 10000);
						return;
					} else {
						displayECFTable(ecfGlobalResult);
						displayECFChart(ecfGlobalResult);
						return ecfGlobalResult;
					}
				}
			});
	}
	
	function displayECFTable(ecfGlobalResult) {
		$("#ecfResultTable > tbody").empty();
		let i = 1;
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
			
			if (i === 1) {
				displayIndividuals(individualResult.scores);
			}
		});
		
		
	}
	
	function displayScoresColumn(competencyScores, competencyScoreGaps) {
		let restOfARow = '';
		let displayGap = $('#display-gap').is(":checked");
		let displayScore = $('#display-score').is(":checked");
		
		let scoreClass = displayScore ? 'score' : 'score hidden';
		let gapClass = displayGap ? 'gap ' : 'gap hidden ';
		
		if (competencyScoreGaps.length === competencyScores.length) {
			let i = 0;
			competencyScores.forEach(competencyScore => {
				let gapColor = (competencyScoreGaps[i]>=0) ? 'greenScore' : 'redScore';
				restOfARow = restOfARow.concat('<th><div>'
						+ '<div class="'
						+ scoreClass
						+ '">'
						+ competencyScore 
						+ '</div>'
						+ '<div class="' 
						+ gapClass
						+ gapColor 
						+ '">&nbsp; (' 
						+ competencyScoreGaps[i] 
						+ ')</div></div></th>');
				i++;
			});
		} else {
			let scoreClass = displayScore ? 'score' : 'score hidden';
			competencyScores.forEach(competencyScore => {
				restOfARow = restOfARow.concat('<th><div>'
						+ '<div class="'
						+ scoreClass
						+ '">'
						+ competencyScore
						+ '</div></div></th>');
			});
		}
		return restOfARow;
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
	
	function displayPreviousPageLinks() {
		$("#previousPagesLinks").empty();
		if (ecfResultPage > 1) {
			$("#previousPagesLinks").append('<a onclick="previousResultPage()" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a>');
		}
		for (let i = 1; i < ecfResultPage; i++){
			$("#previousPagesLinks").append('<a onclick="selectResultPage(' + i +')">' + i +'</a>');
		}
	}
	
	function displayNextPageLinks() {
		$("#nextPagesLinks").empty();
		for (let i = ecfResultPage + 1; i <= ecfMaxResultPage; i++){
			$("#nextPagesLinks").append('<a onclick="selectResultPage(' + i +')">' + i +'</a>');
		}
		
		if (ecfResultPage < ecfMaxResultPage) {
			$("#nextPagesLinks").append('<a onclick="nextResultPage()" aria-label="Next"><span aria-hidden="true">&raquo;</span></a>');
		}
	}
	
	function displayCurrentPageResults() {
		fetchECFResults(ecfResultPage, 10);
		displayPreviousPageLinks();
		$("a#currentResultPage").text(ecfResultPage);
		displayNextPageLinks();
	}
	
	function displayIndividuals(competencyScores) {
		$("th.individual").remove();
		let i = (ecfResultPage - 1) * 10;
		competencyScores.forEach(competencyScore => {
			$(".headerrow").append('<th class="individual">Individual ' + (i + 1) +'</th>');
			i++;
		});
	}