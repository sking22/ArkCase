package com.armedia.acm.auth.web;

/*-
 * #%L
 * ACM Service: User Login and Authentication
 * %%
 * Copyright (C) 2014 - 2021 ArkCase LLC
 * %%
 * This file is part of the ArkCase software. 
 * 
 * If the software was purchased under a paid ArkCase license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.armedia.acm.auth.*;
import com.armedia.acm.auth.utils.EncryptionUtils;
import com.armedia.acm.services.users.dao.UserDao;
import com.armedia.acm.services.users.model.AcmUser;
import com.armedia.acm.services.users.model.AcmUserState;
import com.armedia.acm.services.users.service.ldap.AcmLdapAuthenticationProvider;
import com.armedia.acm.web.api.MDCConstants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// @RestController
@Controller
public class GenerateOtpUiController implements ApplicationEventPublisherAware {
    private UserDao userDao;
    private ApplicationEventPublisher eventPublisher;

    private final Logger log = LogManager.getLogger(getClass());
    private ApplicationEventPublisher applicationEventPublisher;
    public static final long MFA_EXPIRY_IN_SECONDS = 900l;
    private String usernameParameter = "username";
    private String passwordParameter = "password";
    private AcmAuthenticationManager acmAuthenticationManager;
    private AcmAuthenticationDetailsFactory authenticationDetailsSource;


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher)
    {
        eventPublisher = applicationEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
    }


    public ApplicationEventPublisher getApplicationEventPublisher()
    {
        return applicationEventPublisher;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public AcmAuthenticationManager getAcmAuthenticationManager() {
        return acmAuthenticationManager;
    }

    public void setAcmAuthenticationManager(AcmAuthenticationManager acmAuthenticationManager) {
        this.acmAuthenticationManager = acmAuthenticationManager;
    }

    public AcmAuthenticationDetailsFactory getAuthenticationDetailsSource() {
        return authenticationDetailsSource;
    }

    public void setAuthenticationDetailsSource(AcmAuthenticationDetailsFactory authenticationDetailsSource) {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    private String obtainMfaToken(HttpServletRequest request) {
        return request.getParameter("mfa");
    }

    @Nullable
    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(this.passwordParameter);
    }

    @Nullable
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter(this.usernameParameter);
    }

    /**
     * This controller handles the verify call to OKTA so that it can
     * send a verify code to the user's phone. This method is called by
     * Spring MVC framework (GET)
     *
     * @return
     */
    @RequestMapping(value = "/acm_verify", method = RequestMethod.GET)
    public ModelAndView verify(Model model, HttpServletRequest request)
    {
        Map<String, ?> flashInputMap = RequestContextUtils.getInputFlashMap(request);
        if(flashInputMap == null) {
            // If someone refreshes this page, redirect to select auth page, should never come to verify page manually
            return new ModelAndView("redirect:/login");
        } else {
            return new ModelAndView("acm_verify");
        }
    }

    @RequestMapping(value = "/acm_auth", method = RequestMethod.POST)
    public RedirectView authenticateUser(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String username = this.obtainUsername(request);
        username = username != null ? username : "";
        username = username.trim();
        String password = this.obtainPassword(request);
        password = password != null ? password : "";
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        usernamePasswordAuthenticationToken.setDetails(this.authenticationDetailsSource.buildDetails(request));
        Map<String, AuthenticationProvider> providerMap = this.acmAuthenticationManager.getSpringContextHolder().getAllBeansOfType(AuthenticationProvider.class);
        //Authentication providerAuthentication = null;
        AcmAuthentication acmAuthentication = new AcmAuthentication(usernamePasswordAuthenticationToken);
        Authentication authentication = null;
        boolean sentOtp = false, isOtpRequired = true;
        request.getSession().setAttribute("login_error", "");
        for (Map.Entry<String, AuthenticationProvider> providerEntry : providerMap.entrySet()) {
            try {
                if (providerEntry.getValue() instanceof AcmLdapAuthenticationProvider)
                {
                    AcmLdapAuthenticationProvider provider = (AcmLdapAuthenticationProvider) providerEntry.getValue();
                    String userDomain = provider.getAcmLdapSyncConfig().getUserDomain();
                    String principal = acmAuthentication.getName();
                    if (principal.endsWith(userDomain))
                    {
                        authentication = provider.authenticate(usernamePasswordAuthenticationToken);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        if((authentication != null) && !sentOtp) {
                            AcmUser user = userDao.findByUserIdAnyCase(username);
                            long offset = 20 * 60 * 60 - 60;
                            LocalDateTime lastMfaValidation = user.getLastMfaAuthentication();
                            if(lastMfaValidation != null) {
                                lastMfaValidation = lastMfaValidation.plusSeconds(offset);
                                LocalDateTime now = LocalDateTime.now();
                                if(now.isBefore(lastMfaValidation)) {
                                    sentOtp = false;
                                    isOtpRequired = false;
                                }
                            }
                            if(isOtpRequired) {
                                MDC.put(MDCConstants.EVENT_MDC_REQUEST_USER_ID_KEY, user.getUserId());

                                GenerateOtpEvent generateOtpEvent = new GenerateOtpEvent(user, AuthenticationUtils.getUserIpAddress());
                                generateOtpEvent.setSucceeded(true);
                                eventPublisher.publishEvent(generateOtpEvent);
                                sentOtp = true;
                            }
                        }
                    }
                } else {
                    //providerAuthentication = providerEntry.getValue().authenticate(acmAuthentication);
                }
            } catch (Exception e) {
                String s  = ExceptionUtils.getStackTrace(e);
                log.error(s);
                request.getSession().setAttribute("login_error", "BadCredentialsException: Bad credentials");
                return new RedirectView("login");// ModelAndView("redirect:/login.html");
            }
        }
        if(!sentOtp && isOtpRequired) {
            request.getSession().setAttribute("login_error", "Failed Sending Code. Please try again.");
            return new RedirectView("login");// ModelAndView("redirect:/login.html");
        }
        redirectAttributes.addFlashAttribute("username", username);
        try {
            String encryptedPassword = EncryptionUtils.encryptString(password);
            redirectAttributes.addFlashAttribute("token", encryptedPassword);
            if(isOtpRequired)
            {
                redirectAttributes.addFlashAttribute("opt_required", "true");
            } else {
                redirectAttributes.addFlashAttribute("opt_required", "false");
            }
            return new RedirectView("acm_verify");
        } catch (Exception e) {
            String s  = ExceptionUtils.getStackTrace(e);
            log.error(s);
            request.getSession().setAttribute("login_error", "Unknown Exception. Please try again.");
            return new RedirectView("login");
        }
    }

    @PostMapping(value = "/generate-otp")
    public ResponseEntity<String> generateOtp(@RequestParam String userId) //, @RequestParam String email)
    {
        AcmUser user = userDao.findByUserIdAnyCase(userId); //userDao.findByUserIdAndEmailAddress(userId, email);
        if (user == null || user.getUserState() != AcmUserState.VALID)
        {
            return new ResponseEntity<>("Error", HttpStatus.NOT_FOUND);
        }
        else
        {
            MDC.put(MDCConstants.EVENT_MDC_REQUEST_USER_ID_KEY, user.getUserId());
            GenerateOtpEvent generateOtpEvent = new GenerateOtpEvent(user, AuthenticationUtils.getUserIpAddress());
            generateOtpEvent.setSucceeded(true);
            eventPublisher.publishEvent(generateOtpEvent);
        }
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

}
