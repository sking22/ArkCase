package com.armedia.acm.services.suggestion.model;

/*-
 * #%L
 * ACM Service: Suggestion
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

public class CaseSuggestionsRequest {
    private String npi;
    private String ssn;
    private Long objectId;
    private String providerLegalBusiness;
    private String sanctionAssociatedTin;
    private String sanctionAssociatedNpi;
    private String sanctionAssociateLegalBusiness;
    private String sanctionAssociateFullName;

    private String convictName;
    private String convictTin;

    public String getSanctionAssociateFullName() {
        return sanctionAssociateFullName;
    }

    public String getProviderLegalBusiness() {
        return providerLegalBusiness;
    }

    public void setProviderLegalBusiness(String providerLegalBusiness) {
        this.providerLegalBusiness = providerLegalBusiness;
    }

    public void setSanctionAssociateFullName(String sanctionAssociateFullName) {
        this.sanctionAssociateFullName = sanctionAssociateFullName;
    }

    public String getSanctionAssociateLegalBusiness() {
        return sanctionAssociateLegalBusiness;
    }

    public void setSanctionAssociateLegalBusiness(String sanctionAssociateLegalBusinees) {
        this.sanctionAssociateLegalBusiness = sanctionAssociateLegalBusinees;
    }

    public String getConvictName() {
        return convictName;
    }

    public void setConvictName(String convictName) {
        this.convictName = convictName;
    }

    public String getConvictTin() {
        return convictTin;
    }

    public void setConvictTin(String convictTin) {
        this.convictTin = convictTin;
    }

    public String getSanctionAssociatedNpi() {
        return sanctionAssociatedNpi;
    }

    public void setSanctionAssociatedNpi(String sanctionAssociatedNpi) {
        this.sanctionAssociatedNpi = sanctionAssociatedNpi;
    }

    public String getNpi() {
        return npi;
    }

    public void setNpi(String npi) {
        this.npi = npi;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getSanctionAssociatedTin() {
        return sanctionAssociatedTin;
    }

    public void setSanctionAssociatedTin(String sanctionAssociatedTin) {
        this.sanctionAssociatedTin = sanctionAssociatedTin;
    }
}
