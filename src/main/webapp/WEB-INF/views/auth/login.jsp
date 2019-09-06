<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib prefix="esapi" uri="http://www.owasp.org/index.php/Category:OWASP_Enterprise_Security_API" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<!-- login version -->
	<title>EUSurvey - <spring:message code="label.Login" /></title>
	<%@ include file="../includes.jsp" %>
	
	<script type="text/javascript"> 
	
		function requestLink()
		{
			if ($("#email").val() == null || $("#email").val() == '' || $("#login").val() == null || $("#login").val() == '' || !validateEmail($("#email").val()) )
			{
				$("#errorMessage").show();
				return;
			}
			
			$("#forgotPasswordForm").submit();
		}
		

		$(document).ready(function(){
			
			//verify if there is only the system login option
			var showEcas = $("#ecasPanel").length;
			
			if(showEcas == 0)
			{
				$("#sysLoginForm").show();
			}
			
			$("#sysLaunch").click(function(){
				switchPanels();
			});
			
			$("#sysCancel").click(function(){
				
				if(showEcas != 0)
				{
					switchPanels();
				}
								
				$("input[name='username'").val("");
				$("input[name='password'").val("");
			});
			
			if(window.location.href.indexOf("error=true")>-1)
			{
				if(showEcas != 0)
				{
					switchPanels();
				}
			}
			
			if(window.location.href.indexOf("sessionexpired")>-1)
			{
				showBasicError('<spring:message code="error.Session" />');
			}
			
			<c:if test='${mode != null && mode.equalsIgnoreCase("forgotPassword")}'>
				$('#forgot-password-dialog').modal('show');
			</c:if>
			
		});
		
		function switchPanels()
		{
			$("#connectionOptions").toggle();
			$("#sysLoginForm").toggle();
			if (!($("#sysLoginForm").css('display') == 'none')) {
				$("#username").focus();
			}
		}	
	
	</script>		
</head>
<body>

	<%@ include file="../header.jsp" %>	
	
	<div class="page" style="margin-top: 40px">
 		<div class="pageheader">
			<h1><spring:message code="login.title" /></h1>
		</div>
		
		<div id="connectionOptions" style="width:100%; text-align:center">
	 	
		 	<c:if test="${showecas != null}">
		 	
				<div id="ecasPanel" style="text-align: center; width: 400px; margin-left:auto; margin-right:auto; ">
					
					<c:if test="${casoss !=null}">					
						<h3 style="color:rgb(0, 79, 152);">						
							<spring:message code="login.useCasTitle"/>							
						</h3>
					</c:if>					
					
					<div id="ecasPanelContent" class="well" style="padding:51px 51px 51px 51px; margin-bottom:0px;">
						<form:form action="${ecasurl}">
							<input type="hidden" name="service" value="<esapi:encodeForHTMLAttribute>${serviceurl}</esapi:encodeForHTMLAttribute>"/>
							
							<a  onclick="$(this).closest('form').submit()">
								<c:choose>
									<c:when test="${casoss !=null}">
									<img src="${contextpath}/resources/images/cas_logo.png" alt="cas logo" />
									</c:when>
									<c:otherwise>
										<span style="font-size: 25px"><spring:message code="login.useEULoginTitle" /></span>
										<!-- <img src="${contextpath}/resources/images/logo_ecas.png" alt="ecas logo" /> -->
									</c:otherwise>
								</c:choose>
								
							</a><br /><br />
							<c:choose>
								<c:when test="${casoss !=null}">
									<spring:message code="label.CASInfo" /> 			
								</c:when>
								<c:otherwise>
							<spring:message code="label.ECASInfo" />			
								</c:otherwise>
							</c:choose>
							
						</form:form>
					</div>
				</div>

				<div style="padding: 30px; font-size:120%;"><spring:message code="label.or" /></div>
				
				<div id="systemPanel" style=" width: 400px; margin-left:auto; margin-right:auto; ">
					<div id="systemPanelContent"  style=" margin-bottom: 50px;" >			
						<a  id="sysLaunch"><spring:message code="label.LoginSystem" /></a>
					</div>	
				</div>
			</c:if>
		</div>
		
		<div id="sysLoginForm"  class="login" style="display:none; width:90%; margin-left:auto; margin-right:auto; padding:20px 0px 20px 50px; ">
			<h3><spring:message code="login.useSystemTitle" /></h3>

			 	<form:form id="loginForm" action="../login" method="post" >
			 		<fieldset>		
			 			<img src="${contextpath}/resources/images/folder-eusurvey.png" style="float:right; margin-right:75px; width:136px;" alt="login logo">	 				 	
			 			<p>
							<label for="username"><spring:message code="label.UserName" /></label>
							<div class="input-group">
						    	<div class="input-group-addon"><span class="glyphicon glyphicon-user" aria-hidden="true"></span></div>
						    	<input class="form-control" id="username" name="username" type="text" maxlength="255" autocomplete="off" style="width: 300px;" />
						    </div>
						</p>
						<p>
							<label for="password"><spring:message code="label.Password" /></label>
							<div class="input-group">
						    	<div class="input-group-addon"><span class="glyphicon glyphicon-lock" aria-hidden="true"></span></div>
						    	<input class="form-control" id="password" name="password" type="password" maxlength="255" autocomplete="off" style="width: 300px;" />
						    </div>				
						</p>
						<div style="margin-top: 30px;">
							<input id="sysLoginFormSubmitButton" class="btn btn-default" type="submit" value="<spring:message code="label.Login" />"/>
							&nbsp;
							<a id="sysCancel" class="btn btn-default" type="button" ><spring:message code="label.Cancel" /></a>
							&#160;&#160;<spring:message code="label.or" />&#160;&#160;<a class="visiblelink disabled" href="${contextpath}/runner/NewSelfRegistrationSurvey"><spring:message code="label.Register" /></a>
							
							<br />
							<div style="margin-left: 200px"><a class="redlink"  onclick="$('#forgot-password-dialog').modal('show');"><spring:message code="label.ForgotYourPassword" /></a></div>
						</div>
			 		</fieldset>
				</form:form>			
			</div>
	</div>	
	
	<div style="clear: both"></div>
	
	<%@ include file="../footer.jsp" %>
	
	<div class="modal" id="forgot-password-dialog" data-backdrop="static">
		<div class="modal-dialog">
    	<div class="modal-content">
		<form:form id="forgotPasswordForm" action="${contextpath}/auth/forgotPassword" method="post" style="margin: 0px;" >
			<div class="modal-body">
				<spring:message code="label.PleaseEnterYourLogin" /><br />
				<input id="login" type="text" name="login" maxlength="255" /><br /><br />
				<spring:message code="label.PleaseEnterYourEmail" /><br />
				<input id="email" type="text" name="email" maxlength="255" class="email" /><br />
				<span id="errorMessage" style="color: #f00; display: none;"><spring:message code="error.PleaseEnterYourNameAndEmail" /></span>
				<div style="margin-left: 0px; margin-bottom: 20px; margin-top: 20px;">
					<%@ include file="../captcha.jsp" %>	
	        	</div>				
			</div>
			<div class="modal-footer">
				<a  onclick="requestLink();" class="btn btn-info"><spring:message code="label.OK" /></a>
				<a  class="btn btn-default" data-dismiss="modal"><spring:message code="label.Cancel" /></a>			
			</div>	
		</form:form>
		</div>
		</div>
	</div>
	
	<c:if test="${error != null}">
		<script type="text/javascript">
			switchPanels();
			showBasicError('<esapi:encodeForHTML>${error}</esapi:encodeForHTML>');
		</script>
 	</c:if>
 	
 	<c:if test="${info != null}">
 		<script type="text/javascript">
 			switchPanels();
			showBasicInfo('<esapi:encodeForHTML>${info}</esapi:encodeForHTML>');
		</script>
 	</c:if>

</body>
</html>