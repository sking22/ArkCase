package com.armedia.acm.plugins.person.model;

/*-
 * #%L
 * ACM Default Plugin: Person
 * %%
 * Copyright (C) 2014 - 2018 ArkCase LLC
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

import com.armedia.acm.core.AcmObject;
import com.armedia.acm.data.AcmEntity;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by marjan.stefanoski on 09.12.2014.
 */
@Entity
@Table(name = "acm_identification")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "className", defaultImpl = Identification.class)
@DiscriminatorColumn(name = "cm_class_name", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("com.armedia.acm.plugins.person.model.Identification")
@JsonIdentityInfo(generator = JSOGGenerator.class)
public class Identification implements Serializable, AcmEntity, AcmObject
{

    private static final long serialVersionUID = 3413715007864370940L;
    private transient final Logger log = LogManager.getLogger(getClass());

    @Id
    @TableGenerator(name = "acm_identification_gen", table = "acm_identification_id", pkColumnName = "cm_seq_name", valueColumnName = "cm_seq_num", pkColumnValue = "acm_identification", initialValue = 100, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "acm_identification_gen")
    @Column(name = "cm_identification_id")
    private Long identificationID;

    @Column(name = "cm_id_type")
    private String identificationType;

    @Column(name = "cm_id_number")
    private String identificationNumber;

    @Column(name = "cm_id_issuer")
    private String identificationIssuer;

    @Column(name = "cm_year_issued")
    @Temporal(TemporalType.TIMESTAMP)
    private Date identificationYearIssued;

    @Column(name = "cm_created", nullable = false, insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "cm_creator", insertable = true, updatable = false)
    private String creator;

    @Column(name = "cm_modified", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date modified;

    @Column(name = "cm_modifier")
    private String modifier;


    @Column(name = "cm_id_status")
    private String idStatus;

    @Column(name = "cm_id_state")
    private String idState;

    //NLM
    //License Expiration Date  cm_id_expiration_date
    @Column(name = "cm_id_expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idExpirationDate;

    //Grace Period Expiration Date  cm_id_grace_expiration_date
    @Column(name = "cm_id_grace_expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idGraceExpirationDate;

    //Alert Date cm_id_alert_date
    @Column(name = "cm_id_alert_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idAlertDate;

    //Grace Period Days  cm_id_grace_expiration_days
    @Column(name = "cm_id_grace_expiration_days")
    private String idGraceExpirationDays;


    //License Qualifier/Sanction cm_id_qualifier_sanction
    @Column(name = "cm_id_qualifier_sanction")
    private String idQualifierSanction;

    //DNP
    // Role Code/Type cm_id_role_code_type
    @Column(name = "cm_id_role_code_type")
    private String idRoleCodeType;

    // Screening Date cm_id_screening_date
    @Column(name = "cm_id_screening_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idScreeningDate;

    // Exclusion Type cm_id_exclusion_type
    @Column(name = "cm_id_exclusion_type")
    private String idExclusionType;

    // Sanction Date cm_id_sanction_date
    @Column(name = "cm_id_sanction_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idSanctionDate;

    //NPPES
    // Case Type cm_id_case_type
    @Column(name = "cm_id_case_type")
    private String idCaseType;

    // Offense Type cm_id_offense_type
    @Column(name = "cm_id_offense_type")
    private String idOffenseType;

    // Disposition Date cm_id_conv_date
    @Column(name = "cm_id_conv_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idConvictionDate;

    // Case Number  cm_id_case_number
    @Column(name = "cm_id_case_number")
    private String idCaseNumber;

    // Court Name cm_id_court_name
    @Column(name = "cm_id_court_name")
    private String idCourtName;

    // Docket Request Date
    @Column(name = "cm_id_docket_request_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idDocketRequestDate;

    // Docket Response Date
    @Column(name = "cm_id_docket_response_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date idDocketResponseDate;

    //cm_id_docket_status
    @Column(name = "cm_id_docket_status")
    private String idDocketStatus;

    //LCM --None
    //CCM --none

    //MED
    //Provider/Supplier Type cm_id_provider_supplier_type
    @Column(name = "cm_id_provider_supplier_type")
    private String idProviderSupplierType;

    //cm_id_provider_tin_type
    @Column(name = "cm_id_provider_tin_type")
    private String idProviderTinType;

    @Column(name = "cm_class_name")
    private String className = getClass().getName();

    @Override
    public Date getCreated()
    {
        return created;
    }

    @Override
    public void setCreated(Date created)
    {
        this.created = created;
    }

    @Override
    public String getCreator()
    {
        return creator;
    }

    @Override
    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    @Override
    public Date getModified()
    {
        return modified;
    }

    @Override
    public void setModified(Date modified)
    {
        this.modified = modified;
    }

    @Override
    public String getModifier()
    {
        return modifier;
    }

    @Override
    public void setModifier(String modifier)
    {
        this.modifier = modifier;
    }

    public String getIdStatus()
    {
        return idStatus;
    }

    public void setIdStatus(String idStatus)
    {
        this.idStatus = idStatus;
    }

    public String getIdState()
    {
        return idState;
    }

    public void setIdState(String idState) { this.idState = idState; }

    public Date getIdExpirationDate()
    {
        return idExpirationDate;
    }

    public void setIdExpirationDate(Date idExpirationDate) { this.idExpirationDate = idExpirationDate; }

    public Date getIdGraceExpirationDate()
    {
        return idGraceExpirationDate;
    }

    public void setIdGraceExpirationDate(Date idGraceExpirationDate) { this.idGraceExpirationDate = idGraceExpirationDate; }

    public Date getIdAlertDate()
    {
        return idAlertDate;
    }

    public void setIdAlertDate(Date idAlertDate) { this.idAlertDate = idAlertDate; }

    public String getIdGraceExpirationDays()
    {
        return idGraceExpirationDays;
    }

    public void setIdGraceExpirationDays(String idGraceExpirationDays) { this.idGraceExpirationDays = idGraceExpirationDays; }



    public String getIdQualifierSanction()
    {
        return idQualifierSanction;
    }

    public void setIdQualifierSanction(String idQualifierSanction) { this.idQualifierSanction = idQualifierSanction; }

    public String getIdRoleCodeType()
    {
        return idRoleCodeType;
    }

    public void setIdRoleCodeType(String idRoleCodeType) { this.idRoleCodeType = idRoleCodeType; }

    public Date getIdScreeningDate()
    {
        return idScreeningDate;
    }

    public void setIdScreeningDate(Date idScreeningDate) { this.idScreeningDate = idScreeningDate; }

    public String getIdExclusionType()
    {
        return idExclusionType;
    }

    public void setIdExclusionType(String idExclusionType) { this.idExclusionType = idExclusionType; }

    public Date getIdSanctionDate()
    {
        return idSanctionDate;
    }

    public void setIdSanctionDate(Date idSanctionDate) { this.idSanctionDate = idSanctionDate; }

    public String getIdCaseType()
    {
        return idCaseType;
    }

    public void setIdCaseType(String idCaseType) { this.idCaseType = idCaseType; }

    public String getIdOffenseType()
    {
        return idOffenseType;
    }

    public void setIdOffenseType(String idOffenseType) { this.idOffenseType = idOffenseType; }

    public Date getIdConvictionDate()
    {
        return idConvictionDate;
    }

    public void setIdConvictionDate(Date idConvictionDate) { this.idConvictionDate = idConvictionDate; }

    public String getIdCaseNumber()
    {
        return idCaseNumber;
    }

    public void setIdCaseNumber(String idCaseNumber) { this.idCaseNumber = idCaseNumber; }

    public String getIdCourtName()
    {
        return idCourtName;
    }

    public void setIdCourtName(String idCourtName) { this.idCourtName = idCourtName; }

    public Date getIdDocketRequestDate()
    {
        return idDocketRequestDate;
    }

    public void setIdDocketRequestDate(Date idDocketRequestDate) { this.idDocketRequestDate = idDocketRequestDate; }

    public Date getIdDocketResponseDate()
    {
        return idDocketResponseDate;
    }

    public void setIdDocketResponseDate(Date idDocketResponseDate) { this.idDocketResponseDate = idDocketResponseDate; }

    public String getIdProviderSupplierType()
    {
        return idProviderSupplierType;
    }

    public void setProviderSupplierType(String idProviderSupplierType) { this.idProviderSupplierType = idProviderSupplierType; }

    public String getIdProviderTinType()
    {
        return idProviderTinType;
    }

    public void setIdProviderTinType(String idProviderTinType) { this.idProviderTinType = idProviderTinType; }

    public String getIdDocketStatus()
    {
        return idDocketStatus;
    }

    public void setIdDocketStatus(String idDocketStatus) { this.idDocketStatus = idDocketStatus; }

    public Long getIdentificationID()
    {
        return identificationID;
    }

    public void setIdentificationID(Long identificationID)
    {
        this.identificationID = identificationID;
    }

    public String getIdentificationType()
    {
        return identificationType;
    }

    public void setIdentificationType(String identificationType)
    {
        this.identificationType = identificationType;
    }

    public String getIdentificationNumber()
    {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber)
    {
        this.identificationNumber = identificationNumber;
    }

    public String getIdentificationIssuer()
    {
        return identificationIssuer;
    }

    public void setIdentificationIssuer(String identificationIssuer)
    {
        this.identificationIssuer = identificationIssuer;
    }

    public Date getIdentificationYearIssued()
    {
        return identificationYearIssued;
    }

    public void setIdentificationYearIssued(Date identificationYearIssued)
    {
        this.identificationYearIssued = identificationYearIssued;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof Identification))
        {
            return false;
        }
        return getIdentificationID() == ((Identification) obj).getIdentificationID();
    }

    @Override
    @JsonIgnore
    public String getObjectType()
    {
        return IdentificationConstants.OBJECT_TYPE;
    }

    @Override
    @JsonIgnore
    public Long getId()
    {
        return getIdentificationID();
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
