package com.armedia.acm.portalgateway.web.api;

/*-
 * #%L
 * ACM Service: Portal Gateway Service
 * %%
 * Copyright (C) 2014 - 2022 ArkCase LLC
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

import com.armedia.acm.portalgateway.model.PortalUserConfig;
import com.armedia.acm.portalgateway.service.PortalUserConfigurationService;
import com.armedia.acm.services.users.web.api.SecureLdapController;
import com.armedia.acm.spring.SpringContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping(value = {"/api/v1/service/portalgateway", "/api/latest/service/portalgateway"})
public class ArkCasePortalUserAPIController {

    private transient final Logger log = LogManager.getLogger(getClass());

    private PortalUserConfigurationService portalUserConfigurationService;

    @RequestMapping(value= "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PortalUserConfig getPortalUserConfiguration(Authentication auth){
        log.debug("User [{}] is getting a portal configuration", auth.getName());
        return getPortalUserConfigurationService().getPortalUserConfiguration();
    }

    public PortalUserConfigurationService getPortalUserConfigurationService()
    {
        return portalUserConfigurationService;
    }

    public void setPortalUserConfigurationService(PortalUserConfigurationService portalUserConfigurationService)
    {
        this.portalUserConfigurationService = portalUserConfigurationService;
    }
}
