package com.armedia.acm.portalgateway.model;

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

import com.armedia.acm.core.DynamicApplicationConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.beans.factory.annotation.Value;

@JsonSerialize(as = PortalUserConfig.class)
public class PortalUserConfig implements DynamicApplicationConfig
{

    @JsonProperty("portal.authenticatedMode")
    @Value("${portal.authenticatedMode}")
    private Boolean authenticatedMode;

    @JsonProperty("portal.responseInstallment.numOfAvailableDays")
    @Value("${portal.responseInstallment.numOfAvailableDays}")
    private Integer numOfAvailableDays;

    @JsonProperty("portal.responseInstallment.maxDownloadAttempts")
    @Value("${portal.responseInstallment.maxDownloadAttempts}")
    private Integer maxDownloadAttempts;

    @JsonProperty("portal.id")
    @Value("${portal.id}")
    private String portalId;

    public Boolean getAuthenticatedMode()
    {
        return authenticatedMode;
    }

    public void setAuthenticatedMode(Boolean authenticatedMode)
    {
        this.authenticatedMode = authenticatedMode;
    }

    public Integer getNumOfAvailableDays()
    {
        return numOfAvailableDays;
    }

    public void setNumOfAvailableDays(Integer numOfAvailableDays)
    {
        this.numOfAvailableDays = numOfAvailableDays;
    }

    public Integer getMaxDownloadAttempts()
    {
        return maxDownloadAttempts;
    }

    public void setMaxDownloadAttempts(Integer maxDownloadAttempts)
    {
        this.maxDownloadAttempts = maxDownloadAttempts;
    }

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

}
