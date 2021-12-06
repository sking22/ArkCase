<%--
  #%L
  ACM Service: User Login and Authentication
  %%
  Copyright (C) 2014 - 2021 ArkCase LLC
  %%
  This file is part of the ArkCase software. 
  
  If the software was purchased under a paid ArkCase license, the terms of 
  the paid license agreement will prevail.  Otherwise, the software is 
  provided under the following open source license terms:
  
  ArkCase is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
   
  ArkCase is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.
  
  You should have received a copy of the GNU Lesser General Public License
  along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
  #L%
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>ACM | ArkCase | User Interface</title>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
    <script src="<%= request.getContextPath()%>/node_modules/@bower_components/bootstrap/dist/js/bootstrap.js"></script>

    <link rel="stylesheet" href="<%= request.getContextPath()%>/node_modules/@bower_components/bootstrap/dist/css/bootstrap.css">
    <link rel="stylesheet" href="<%= request.getContextPath()%>/assets/css/login.css">
    <!-- custom css-->
    <link rel="stylesheet" href="<%= request.getContextPath()%>/branding/customcss">

    <script type="text/javascript">

        function send() {
            var username = $("#username").val();
            $.post("<%= request.getContextPath()%>/generate-otp", {userId: username})
                .always(function (data) {
                    if (data.status === 200) {
                        $('#generate-otp-success').show();
                    } else if (data.status === 404) {
                        $('#generate-otp-error').show();
                    } else {
                        $('#forgot-generic-error').show();
                    }
                }
                );
        }
    </script>
</head>
<body>

<div class="login-wrapper">
    <div class="logo">
        <img src="<%= request.getContextPath()%>/branding/loginlogo.png" style="max-width: 100%;">
    </div>
    <header class="text-center">
        <strong>Verify</strong>
    </header>

    <c:if test="${not empty error}">
        <div id="errorMessage" style="color: red">
                ${error}
        </div>
    </c:if>

    <div id="generate-otp-success" style="display:none" class="alert alert-success">
        A new code has been sent. You should receive it in a few minutes.
    </div>

    <div id="generate-otp-error" style="display:none" class="alert alert-success">
        Valid user with this email does not exist in the system.
    </div>

    <div id="forgot-generic-error" style="display:none" class="alert alert-danger">
      Server error.
    </div>

    <div id="code-resent" class="alert alert-success" style="display:none;">A new code has been sent.</div>

    <form id="login-form" action="<%= request.getContextPath()%>/login_post" method="post">

        <div class="form-group">
            <input id="mfa"
                   name="mfa"
                   type="number"
                   required="required"
                   placeholder="Enter code digits here"
                   class="form-control"/>
            <input id="username" name="username" value="${username}" hidden/>
            <input id="token" type="token" name="token" value="${token}" hidden/>
        </div>
        <button id="submit" type="submit" class="btn btn-lg btn-primary btn-block">Verify</button>
        <p></p>
        <div class="pull-left">
            <a href="<c:url value="/logout"/>">Cancel</a>
        </div>
        <div class="pull-right">
            <a onclick="send();" style="cursor:pointer;">Get New Code?</a>
        </div>
    </form>
</div>

<footer id="footer">
    <div class="text-center padder">
        <p>
            <small><a
                    href="http://www.arkcase.com/"><span>ArkCase</span></a><br>&copy;<span>2014, 2015, 2016, 2017</span>
            </small>
        </p>
    </div>
</footer>
</body>
</html>
