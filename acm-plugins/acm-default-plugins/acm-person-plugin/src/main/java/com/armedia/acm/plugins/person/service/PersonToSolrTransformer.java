package com.armedia.acm.plugins.person.service;

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

import com.armedia.acm.plugins.addressable.model.ContactMethod;
import com.armedia.acm.plugins.addressable.model.PostalAddress;
import com.armedia.acm.plugins.person.dao.PersonDao;
import com.armedia.acm.plugins.person.model.*;
import com.armedia.acm.services.dataaccess.service.SearchAccessControlFields;
import com.armedia.acm.services.participants.utils.ParticipantUtils;
import com.armedia.acm.services.search.model.solr.SolrAdvancedSearchDocument;
import com.armedia.acm.services.search.model.solr.SolrDocument;
import com.armedia.acm.services.search.service.AcmObjectToSolrDocTransformer;
import com.armedia.acm.services.users.dao.UserDao;
import com.armedia.acm.services.users.model.AcmUser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by armdev on 10/21/14.
 */
public class PersonToSolrTransformer implements AcmObjectToSolrDocTransformer<Person>
{
    private final Logger log = LogManager.getLogger(getClass());
    private PersonDao personDao;
    private UserDao userDao;
    private SearchAccessControlFields searchAccessControlFields;

    @Override
    public List<Person> getObjectsModifiedSince(Date lastModified, int start, int pageSize)
    {
        return getPersonDao().findModifiedSince(lastModified, start, pageSize);
    }

    @Override
    public SolrAdvancedSearchDocument toSolrAdvancedSearch(Person person)
    {
        SolrAdvancedSearchDocument solrDoc = new SolrAdvancedSearchDocument();

        getSearchAccessControlFields().setAccessControlFields(solrDoc, person);

        solrDoc.setId(person.getId() + "-PERSON");
        solrDoc.setObject_type_s("PERSON");
        solrDoc.setObject_id_s(person.getId() + "");
        solrDoc.setPerson_title_lcs(person.getTitle());
        solrDoc.setFirst_name_lcs(person.getGivenName());
        solrDoc.setLast_name_lcs(person.getFamilyName());

        solrDoc.setFull_name_lcs(person.getGivenName() + " " + person.getFamilyName());

        solrDoc.setCreate_date_tdt(person.getCreated());
        solrDoc.setCreator_lcs(person.getCreator());
        solrDoc.setModified_date_tdt(person.getModified());
        solrDoc.setModifier_lcs(person.getModifier());

        solrDoc.setName(person.getGivenName() + " " + person.getFamilyName());

        solrDoc.setTitle_parseable(person.getFamilyName() + " " + person.getGivenName());
        solrDoc.setTitle_parseable_lcs(person.getFamilyName() + " " + person.getGivenName());
        solrDoc.setStatus_lcs(person.getStatus());

        addContactMethods(person, solrDoc);

        addOrganizations(person, solrDoc);

        addAddresses(person, solrDoc);

        addAliases(person, solrDoc);

        /** Additional properties for full names instead of ID's */
        AcmUser creator = getUserDao().quietFindByUserId(person.getCreator());
        if (creator != null)
        {
            solrDoc.setAdditionalProperty("creator_full_name_lcs", creator.getFirstName() + " " + creator.getLastName());
        }

        AcmUser modifier = getUserDao().quietFindByUserId(person.getModifier());
        if (modifier != null)
        {
            solrDoc.setAdditionalProperty("modifier_full_name_lcs", modifier.getFirstName() + " " + modifier.getLastName());
        }

        solrDoc.setAdditionalProperty("default_organization_s",
                person.getDefaultOrganization() != null ? person.getDefaultOrganization().getOrganization().getOrganizationValue() : null);
        solrDoc.setAdditionalProperty("default_phone_s", getDefaultPhone(person));
        solrDoc.setAdditionalProperty("default_location_s", getDefaultAddress(person));
        solrDoc.setAdditionalProperty("default_email_lcs", getDefaultEmail(person));

        String participantsListJson = ParticipantUtils.createParticipantsListJson(person.getParticipants());
        solrDoc.setAdditionalProperty("acm_participants_lcs", participantsListJson);


        //new fields
        solrDoc.setAdditionalProperty("legal_business_name_lcs", person.getLegalBusinessName());
        solrDoc.setAdditionalProperty("provider_lbn_ein_lcs", person.getProviderLbnEin());
        solrDoc.setAdditionalProperty("associate_last_name_lcs", person.getAssociateLastName());
        solrDoc.setAdditionalProperty("associate_middle_name_lcs", person.getAssociateMiddleName());
        solrDoc.setAdditionalProperty("associate_first_name_lcs", person.getAssociateFirstName());
        solrDoc.setAdditionalProperty("associate_legal_business_name_lcs", person.getAssociateLegalBusinessName());
        solrDoc.setAdditionalProperty("associate_enrollment_id_lcs", person.getAssociateEnrollmentId());
        solrDoc.setAdditionalProperty("associate_npi_lcs", person.getAssociateNPI());
        solrDoc.setAdditionalProperty("associate_tin_lcs", person.getAssociateTIN());
        solrDoc.setAdditionalProperty("associate_tin_type_lcs", person.getAssociateTinType());
        solrDoc.setAdditionalProperty("associate_role_lcs", person.getAssociateRole());
        solrDoc.setAdditionalProperty("associate_sanction_code_lcs", person.getAssociateSanctionCode());
        solrDoc.setAdditionalProperty("associate_sanction_date_lcs", person.getAssociateSanctionDate());
        solrDoc.setAdditionalProperty("provider_specialty_lcs", person.getProviderSpecialty());

        List<Identification> ids = person.getIdentifications();
        /*for(int i = 0; i < ids.size(); i++){
            Identification id = ids.get(i);*/
        for(Identification id: ids) {
          /*Identification id = ids.get(i);*/
          //  PersonAssociation personAssociation: in.getPersonAssociations()
            log.error("id.getIdentificationType(): " + id.getIdentificationType());
            if ( id.getIdentificationType().equalsIgnoreCase("PECOS Enrollment ID")) {
                solrDoc.setAdditionalProperty("provider_pecos_id_lcs", id.getIdentificationID());
                solrDoc.setAdditionalProperty("provider_pecos_id_state_lcs", id.getIdState());
                solrDoc.setAdditionalProperty("provider_pecos_id_status_lcs", id.getIdStatus());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("Contractor ID/Contractor Name")) {
                solrDoc.setAdditionalProperty("provider_contractor_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("NPI")) {
                solrDoc.setAdditionalProperty("provider_npi_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("SSN/EIN")) {
                solrDoc.setAdditionalProperty("provider_ssn_ein_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("PTAN")) {
                solrDoc.setAdditionalProperty("provider_ptan_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("TIN")) {
                solrDoc.setAdditionalProperty("provider_tin_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("Enrollment ID")) {
                solrDoc.setAdditionalProperty("provider_enrollment_id_lcs", id.getIdentificationID());
            }

            if ( id.getIdentificationType().equalsIgnoreCase("License Number")) {
                solrDoc.setAdditionalProperty("provider_license_id_lcs", id.getIdentificationID());
                solrDoc.setAdditionalProperty("provider_license_id_status_lcs", id.getIdStatus());
                solrDoc.setAdditionalProperty("provider_license_id_expiration_lcs", id.getIdExpirationDate());
                solrDoc.setAdditionalProperty("provider_license_id_qualifier_sanction_lcs", id.getIdQualifierSanction());
                solrDoc.setAdditionalProperty("provider_license_id_alert_date_lcs", id.getIdAlertDate());
            }
        }

        Identification defId = person.getDefaultIdentification();
        solrDoc.setAdditionalProperty("provider_case_type_id_lcs", defId.getIdCaseType());
        solrDoc.setAdditionalProperty("provider_case_number_id_lcs", defId.getIdCaseNumber());
        solrDoc.setAdditionalProperty("provider_offense_type_id_lcs", defId.getIdOffenseType());
        solrDoc.setAdditionalProperty("provider_conviction_date_id_lcs", defId.getIdConvictionDate());
        solrDoc.setAdditionalProperty("provider_docket_request_date_id_lcs", defId.getIdDocketRequestDate());
        solrDoc.setAdditionalProperty("provider_docket_response_date_id_lcs", defId.getIdDocketResponseDate());
        solrDoc.setAdditionalProperty("provider_docket_status_id_lcs", defId.getIdDocketStatus());

        return solrDoc;
    }

    private String getDefaultEmail(Person person)
    {
        if (person.getDefaultEmail() != null)
        {
            return person.getDefaultEmail().getValue();
        }
        else
        {
            return person.getContactMethods().stream()
                    .filter(cm -> cm.getType().equalsIgnoreCase("email"))
                    .findFirst()
                    .map(ContactMethod::getValue)
                    .orElse(null);
        }
    }

    private String getDefaultPhone(Person person)
    {
        if (person.getDefaultPhone() == null)
        {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(person.getDefaultPhone().getValue());
        if (person.getDefaultPhone().getSubType() != null)
        {
            sb.append(" [").append(person.getDefaultPhone().getSubType()).append("]");
        }
        return sb.toString();
    }

    private String getDefaultAddress(Person person)
    {
        if (person.getDefaultAddress() == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (person.getDefaultAddress().getCity() != null)
        {
            sb.append(person.getDefaultAddress().getCity());
        }
        if (person.getDefaultAddress().getState() != null)
        {
            if (sb.length() > 0)
            {
                sb.append(", ");
            }
            sb.append(person.getDefaultAddress().getState());
        }
        return sb.toString();
    }

    private void addAddresses(Person person, SolrAdvancedSearchDocument solrDoc)
    {
        List<String> addressIds = new ArrayList<>();
        if (person.getAddresses() != null)
        {
            for (PostalAddress address : person.getAddresses())
            {
                addressIds.add(address.getId() + "-LOCATION");
            }

        }
        solrDoc.setPostal_address_id_ss(addressIds);
    }

    private void addOrganizations(Person person, SolrAdvancedSearchDocument solrDoc)
    {
        List<String> organizationIds = new ArrayList<>();
        if (person.getOrganizations() != null)
        {
            for (Organization org : person.getOrganizations())
            {
                organizationIds.add(org.getOrganizationId() + "-ORGANIZATION");
            }
        }
        solrDoc.setOrganization_id_ss(organizationIds);
    }

    private void addContactMethods(Person person, SolrAdvancedSearchDocument solrDoc)
    {
        List<String> contactMethodIds = new ArrayList<>();
        if (person.getContactMethods() != null)
        {
            for (ContactMethod cm : person.getContactMethods())
            {
                contactMethodIds.add(cm.getId() + "-CONTACT-METHOD");
            }
        }
        solrDoc.setContact_method_ss(contactMethodIds);
    }

    private void addAliases(Person person, SolrAdvancedSearchDocument solrDoc)
    {
        List<String> aliasIds = new ArrayList<>();
        if (person.getPersonAliases() != null)
        {
            for (PersonAlias pa : person.getPersonAliases())
            {
                aliasIds.add(pa.getId() + "-PERSON-ALIAS");
            }
        }
        solrDoc.setPerson_alias_ss(aliasIds);
    }

    @Override
    public SolrDocument toSolrQuickSearch(Person in)
    {
        SolrDocument solrDoc = new SolrDocument();

        getSearchAccessControlFields().setAccessControlFields(solrDoc, in);

        solrDoc.setId(in.getId() + "-PERSON");
        solrDoc.setObject_type_s("PERSON");
        solrDoc.setName(in.getGivenName() + " " + in.getFamilyName());
        solrDoc.setObject_id_s(in.getId() + "");

        solrDoc.setCreate_tdt(in.getCreated());
        solrDoc.setAuthor_s(in.getCreator());
        solrDoc.setLast_modified_tdt(in.getModified());
        solrDoc.setModifier_s(in.getModifier());

        solrDoc.setTitle_parseable(in.getFamilyName() + " " + in.getGivenName());
        solrDoc.setTitle_parseable_lcs(in.getFamilyName() + " " + in.getGivenName());
        solrDoc.setStatus_s(in.getStatus());

        //new fields
        solrDoc.setAdditionalProperty("legal_business_name_lcs", in.getLegalBusinessName());
        solrDoc.setAdditionalProperty("provider_lbn_ein_lcs", in.getProviderLbnEin());
        solrDoc.setAdditionalProperty("associate_last_name_lcs", in.getAssociateLastName());
        solrDoc.setAdditionalProperty("associate_middle_name_lcs", in.getAssociateMiddleName());
        solrDoc.setAdditionalProperty("associate_first_name_lcs", in.getAssociateFirstName());
        solrDoc.setAdditionalProperty("associate_legal_business_name_lcs", in.getAssociateLegalBusinessName());
        solrDoc.setAdditionalProperty("associate_enrollment_id_lcs", in.getAssociateEnrollmentId());
        solrDoc.setAdditionalProperty("associate_npi_lcs", in.getAssociateNPI());
        solrDoc.setAdditionalProperty("associate_tin_lcs", in.getAssociateTIN());
        solrDoc.setAdditionalProperty("associate_tin_type_lcs", in.getAssociateTinType());
        solrDoc.setAdditionalProperty("associate_role_lcs", in.getAssociateRole());
        solrDoc.setAdditionalProperty("associate_sanction_code_lcs", in.getAssociateSanctionCode());
        solrDoc.setAdditionalProperty("associate_sanction_date_lcs", in.getAssociateSanctionDate());
        solrDoc.setAdditionalProperty("provider_specialty_lcs", in.getProviderSpecialty());

       List<Identification> ids = in.getIdentifications();
        /*for(int i = 0; i < ids.size(); i++){
            Identification id = ids.get(i);*/
        for(Identification id: ids) {
            log.error("idtest: " + id.getIdentificationType());
            if ( id.getIdentificationType().equalsIgnoreCase("PECOS Enrollment ID")) {
                solrDoc.setAdditionalProperty("provider_pecos_id_lcs", id.getIdentificationID());
                solrDoc.setAdditionalProperty("provider_pecos_id_state_lcs", id.getIdState());
                solrDoc.setAdditionalProperty("provider_pecos_id_status_lcs", id.getIdStatus());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("Contractor ID/Contractor Name")) {
                solrDoc.setAdditionalProperty("provider_contractor_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("NPI")) {
                solrDoc.setAdditionalProperty("provider_npi_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("SSN/EIN")) {
                solrDoc.setAdditionalProperty("provider_ssn_ein_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("PTAN")) {
                solrDoc.setAdditionalProperty("provider_ptan_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("TIN")) {
                solrDoc.setAdditionalProperty("provider_tin_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("Enrollment ID")) {
                solrDoc.setAdditionalProperty("provider_enrollment_id_lcs", id.getIdentificationID());
            }
            if ( id.getIdentificationType().equalsIgnoreCase("License Number")) {
                solrDoc.setAdditionalProperty("provider_license_id_lcs", id.getIdentificationID());
                solrDoc.setAdditionalProperty("provider_license_id_status_lcs", id.getIdStatus());
                solrDoc.setAdditionalProperty("provider_license_id_expiration_lcs", id.getIdExpirationDate());
                solrDoc.setAdditionalProperty("provider_license_id_qualifier_sanction_lcs", id.getIdQualifierSanction());
                solrDoc.setAdditionalProperty("provider_license_id_alert_date_lcs", id.getIdAlertDate());
            }
        }
        Identification defId = in.getDefaultIdentification();
        solrDoc.setAdditionalProperty("provider_case_type_id_lcs", defId.getIdCaseType());
        solrDoc.setAdditionalProperty("provider_case_number_id_lcs", defId.getIdCaseNumber());
        solrDoc.setAdditionalProperty("provider_offense_type_id_lcs", defId.getIdOffenseType());
        solrDoc.setAdditionalProperty("provider_conviction_date_id_lcs", defId.getIdConvictionDate());
        solrDoc.setAdditionalProperty("provider_docket_request_date_id_lcs", defId.getIdDocketRequestDate());
        solrDoc.setAdditionalProperty("provider_docket_response_date_id_lcs", defId.getIdDocketResponseDate());
        solrDoc.setAdditionalProperty("provider_docket_status_id_lcs", defId.getIdDocketStatus());

        return solrDoc;
    }

    @Override
    public boolean isAcmObjectTypeSupported(Class acmObjectType)
    {
        return Person.class.equals(acmObjectType);
    }

    public PersonDao getPersonDao()
    {
        return personDao;
    }

    public void setPersonDao(PersonDao personDao)
    {
        this.personDao = personDao;
    }

    public UserDao getUserDao()
    {
        return userDao;
    }

    public void setUserDao(UserDao userDao)
    {
        this.userDao = userDao;
    }

    @Override
    public Class<?> getAcmObjectTypeSupported()
    {
        return Person.class;
    }

    public SearchAccessControlFields getSearchAccessControlFields()
    {
        return searchAccessControlFields;
    }

    public void setSearchAccessControlFields(SearchAccessControlFields searchAccessControlFields)
    {
        this.searchAccessControlFields = searchAccessControlFields;
    }
}
