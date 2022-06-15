package com.armedia.acm.plugins.casefile.service;

/*-
 * #%L
 * ACM Default Plugin: Case File
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

import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.ACM_PARTICIPANTS_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.ASSIGNEE_FIRST_NAME_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.ASSIGNEE_FULL_NAME_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.ASSIGNEE_ID_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.ASSIGNEE_LAST_NAME_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.CREATOR_FULL_NAME_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.DESCRIPTION_NO_HTML_TAGS_PARSEABLE;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.INCIDENT_TYPE_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.MODIFIER_FULL_NAME_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.PRIORITY_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.STATUS_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.TITLE_PARSEABLE;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.TITLE_PARSEABLE_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.SUMMARY_PARSEABLE_LCS;


import com.armedia.acm.plugins.businessprocess.dao.BusinessProcessDao;
import com.armedia.acm.plugins.casefile.dao.CaseFileDao;
import com.armedia.acm.plugins.casefile.model.CaseFile;
import com.armedia.acm.plugins.casefile.model.CaseFileConstants;
import com.armedia.acm.plugins.casefile.model.ChangeCaseStatus;
import com.armedia.acm.plugins.ecm.service.FileAclSolrUpdateHelper;
import com.armedia.acm.plugins.person.model.Person;
import com.armedia.acm.plugins.person.model.PersonAssociation;
import com.armedia.acm.plugins.task.model.TaskConstants;
import com.armedia.acm.services.dataaccess.service.SearchAccessControlFields;
import com.armedia.acm.services.participants.utils.ParticipantUtils;
import com.armedia.acm.services.search.model.solr.SolrAdvancedSearchDocument;
import com.armedia.acm.services.search.service.AcmObjectToSolrDocTransformer;
import com.armedia.acm.services.users.dao.UserDao;
import com.armedia.acm.services.users.model.AcmUser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by marjan.stefanoski on 08.11.2014.
 */
public class CaseFileToSolrTransformer implements AcmObjectToSolrDocTransformer<CaseFile>
{
    private final Logger LOG = LogManager.getLogger(getClass());

    private UserDao userDao;
    private CaseFileDao caseFileDao;
    private FileAclSolrUpdateHelper fileAclSolrUpdateHelper;
    private SearchAccessControlFields searchAccessControlFields;
    private BusinessProcessDao businessProcessDao;

    @Override
    public List<CaseFile> getObjectsModifiedSince(Date lastModified, int start, int pageSize)
    {
        return getCaseFileDao().findModifiedSince(lastModified, start, pageSize);
    }

    private String getFullName(String firstName, String middleName, String lastName) {
        String fullName = "";
        String comma = "";
        if((firstName != null) && !firstName.trim().equals("")) {
            fullName = fullName + firstName;
            comma = " ";
        }
        if((middleName != null) && !middleName.trim().equals("")) {
            fullName = fullName + comma + middleName;
            comma = " ";
        }
        if((lastName != null) && !lastName.trim().equals("")) {
            fullName = fullName + comma + lastName;
        }

        if(fullName.equals("")) {
            return null;
        }
        return fullName;
    }

    @Override
    public SolrAdvancedSearchDocument toSolrAdvancedSearch(CaseFile in)
    {
        SolrAdvancedSearchDocument solrDoc = new SolrAdvancedSearchDocument();
        LOG.debug("Creating Solr advanced search document for CASE_FILE.");

        mapRequiredProperties(solrDoc, in.getId(), in.getCreator(), in.getCreated(), in.getModifier(), in.getModified(),
                CaseFileConstants.OBJECT_TYPE, in.getCaseNumber());

        getSearchAccessControlFields().setAccessControlFields(solrDoc, in);

        solrDoc.setDueDate_tdt(in.getDueDate());
        solrDoc.setIncident_date_tdt(in.getIncidentDate());

        mapAdditionalProperties(in, solrDoc.getAdditionalProperties());

        return solrDoc;
    }

    @Override
    public void mapAdditionalProperties(CaseFile in, Map<String, Object> additionalProperties)
    {
        additionalProperties.put(TITLE_PARSEABLE, in.getTitle());
        additionalProperties.put(DESCRIPTION_NO_HTML_TAGS_PARSEABLE, in.getDetails());
        additionalProperties.put(SUMMARY_PARSEABLE_LCS, in.getCaseDetailsSummary());
        additionalProperties.put(PRIORITY_LCS, in.getPriority());
        additionalProperties.put(INCIDENT_TYPE_LCS, in.getCaseType());
        additionalProperties.put(STATUS_LCS, in.getStatus());

        String assigneeUserId = ParticipantUtils.getAssigneeIdFromParticipants(in.getParticipants());
        additionalProperties.put(ASSIGNEE_ID_LCS, assigneeUserId);

        AcmUser assignee = getUserDao().quietFindByUserId(assigneeUserId);
        if (assignee != null)
        {
            additionalProperties.put(ASSIGNEE_FIRST_NAME_LCS, assignee.getFirstName());
            additionalProperties.put(ASSIGNEE_LAST_NAME_LCS, assignee.getLastName());
            additionalProperties.put(ASSIGNEE_FULL_NAME_LCS, assignee.getFirstName() + " " + assignee.getLastName());
        }

        /** Additional properties for full names instead of ID's */
        AcmUser creator = getUserDao().quietFindByUserId(in.getCreator());
        if (creator != null)
        {
            additionalProperties.put(CREATOR_FULL_NAME_LCS, creator.getFirstName() + " " + creator.getLastName());
        }

        AcmUser modifier = getUserDao().quietFindByUserId(in.getModifier());
        if (modifier != null)
        {
            additionalProperties.put(MODIFIER_FULL_NAME_LCS, modifier.getFirstName() + " " + modifier.getLastName());
        }

        additionalProperties.put("security_field_lcs", in.getSecurityField());
        additionalProperties.put(TITLE_PARSEABLE_LCS, in.getTitle());

        String participantsListJson = ParticipantUtils.createParticipantsListJson(in.getParticipants());

        additionalProperties.put(ACM_PARTICIPANTS_LCS, participantsListJson);

        // The property "assignee_group_id_lcs" is used only for showing/hiding claim/unclaim buttons
        additionalProperties.put("assignee_group_id_lcs", in.getAssigneeGroup());

        // This property is used for showing the owning group for the object
        additionalProperties.put("owning_group_id_lcs", ParticipantUtils.getOwningGroupIdFromParticipants(in.getParticipants()));
        additionalProperties.put("owning_group_id_s", ParticipantUtils.getOwningGroupIdFromParticipants(in.getParticipants()));

        if (in.getOriginator() != null && in.getOriginator().getPerson() != null)
        {
            Person person = in.getOriginator().getPerson();
            additionalProperties.put("initiator_person_id_i", person.getId());
        }

        String ac = "ACTION";
        String blank = "";
        String acm_ac = "acm_case_action_lcs";

        if( in.getStatus().equalsIgnoreCase("Review Approved")
         || in.getStatus().equalsIgnoreCase("Review Approved II")
         || in.getStatus().equalsIgnoreCase("Returned for Revision")
         || in.getStatus().equalsIgnoreCase("Returned for Revision II")
         || in.getStatus().equalsIgnoreCase("CMS Requested Edits" )){
            additionalProperties.put(acm_ac, ac);
        } else {
            additionalProperties.put(acm_ac, blank);
        }

        AcmUser analyst = getUserDao().quietFindByUserId(in.getCasePrevAnalyst());
        additionalProperties.put("acm_case_analyst_lcs", in.getCasePrevAnalyst());
        additionalProperties.put("acm_case_analyst_full_name_lcs", analyst.getFirstName() + " " + analyst.getLastName());



        additionalProperties.put("acm_participants_lcs", participantsListJson);
        // The property "assignee_group_id_lcs" is used only for showing/hiding claim/unclaim buttons
        additionalProperties.put("assignee_group_id_lcs", in.getAssigneeGroup());
        /*
        //additionalProperties.put("case_termination_eff_date_lcs", in.getCaseTerminationEffDate());
        //additionalProperties.put("case_enrollment_bar_exp_date_lcs", in.getCaseEnrollmentBarExpDate());
        //additionalProperties.put("case_reinstated_termination_eff_date_lcs", in.getCaseReinsTerminationEffDate());
        //additionalProperties.put("case_recind_termination_eff_date_lcs", in.getCaseRecindTerminationEffDate());
        //additionalProperties.put("case_appeal_period_expired_lcs", in.getCaseAppealsPeriodExpired());
        //additionalProperties.put("case_revise_reissue_el_lcs", in.getCaseReviseReissueEl());
        //additionalProperties.put("case_orig_rev_letter_date_lcs", in.getCaseOrigRevLetterDate());
        //additionalProperties.put("case_revise_reissue_outcome_lcs", in.getCaseReviseReissueOutcome());
        //additionalProperties.put("case_eff_letter_date_lcs", in.getCaseEffectiveLetterDate());
        //additionalProperties.put("case_reenroll_bar_length_lcs", in.getCaseLengthReEnrollBar());
        //additionalProperties.put("case_rev_eff_date_lcs", in.getCaseRevEffActionDate());
       // additionalProperties.put("case_not_action_reason_lcs", in.getCaseNotActionableReason());
          additionalProperties.put("case_reporting_week_lcs", in.getCaseReportingWeek());
          additionalProperties.put("case_report_date_lcs", in.getCaseReportDate());
        */

        // This property is used for showin the owning group for the object
        additionalProperties.put("owning_group_id_lcs", ParticipantUtils.getOwningGroupIdFromParticipants(in.getParticipants()));
        additionalProperties.put("owning_group_id_s", ParticipantUtils.getOwningGroupIdFromParticipants(in.getParticipants()));

        additionalProperties.put("case_state_medicaid_agency_lcs", in.getCaseStateMedicaidAgency());
        additionalProperties.put("case_termination_type_lcs", in.getCaseTerminationType());

        additionalProperties.put("case_termination_reason_lcs", in.getCaseTerminationReason());
        additionalProperties.put("case_correspondence_address_lcs", in.getCaseCorrespondenceAddress());
        additionalProperties.put("case_original_rev_auth_lcs", in.getCaseOrigRevAuth());

        additionalProperties.put("case_admin_actions_outcome_lcs", in.getCaseAdminActionsOutcome());
        additionalProperties.put("case_rev_auth_cited_action_letter_lcs", in.getCaseRevAuthCitedActionLetter());
        additionalProperties.put("case_taxonomy_lcs", in.getCaseTaxonomy());
        additionalProperties.put("case_application_type_lcs", in.getCaseApplicationType());
        additionalProperties.put("case_conv_ind_lcs", in.getCaseConvictedIndividual());
        additionalProperties.put("case_convicted_ind_tin_lcs", in.getCaseConvictedIndividualTin());

       //cm_case_final_out_admin_act
        additionalProperties.put("case_final_outcome_admin_act_lcs", in.getCaseFinalOutAdminAct());

        String action = "No";
        for(ChangeCaseStatus ccs: in.getChangeCaseStatuses()) {
            if(ccs.getStatus().equalsIgnoreCase("Submitted To CMS") ) {
                action = "Yes";
            }
        }
        additionalProperties.put("case_actionable_sub_to_cms_lcs", action);


        for(PersonAssociation pa: in.getPersonAssociations()) {
            if (pa.getPersonType().equalsIgnoreCase("initiator")) {
                Person person = pa.getPerson();
                if(person != null) {
                    String lbnein = person.getProviderLbnEin();
                    if ((lbnein != null) && !lbnein.equalsIgnoreCase("na") &&
                            !lbnein.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_lbn_ein_lcs", lbnein);
                    }

                    String idState = person.getLicenseState();
                    if ((idState != null) && !idState.equalsIgnoreCase("na") &&
                            !idState.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_license_state_lcs", idState);
                    }

                    Date asd = person.getAssociateSanctionDate();
                    if ((asd != null)) {
                        additionalProperties.put("case_provider_sanctioned_date_lcs", asd);
                    }

                    String ssn = person.getSsn();
                    if ((ssn != null) && !ssn.equalsIgnoreCase("na") &&
                            !ssn.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_ssn_lcs", ssn);
                    }
                    String npi = person.getNpi();
                    if ((npi != null) && !npi.equalsIgnoreCase("na")
                            && !npi.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_npi_lcs", npi);
                    }

                    String peid = person.getPecosEnrollmentID();
                    if ((peid != null) && !peid.equalsIgnoreCase("na")
                            && !peid.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_peid_lcs", peid);
                    }

                    String casenum = person.getCASENUM();
                    if (!casenum.equalsIgnoreCase("na")) {
                        additionalProperties.put("case_provider_id_case_number_lcs", casenum);
                    }

                    String enrollId = person.getEnrollmentID();
                    if ((enrollId != null) && !enrollId.equalsIgnoreCase("na")
                            && !enrollId.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_enrollId_lcs", enrollId);
                    }

                    String licenseNum = person.getLicenseNumber();
                    if ((licenseNum != null) && !licenseNum.equalsIgnoreCase("na")
                            && !licenseNum.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_licenseNum_lcs", licenseNum);
                    }

                    String contractorID = person.geContractorID();
                    if ((contractorID != null) && !contractorID.equalsIgnoreCase("na")
                            && !contractorID.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_contractorID_lcs", contractorID);
                    }

                    String proTIN = person.getTIN();
                    if ((proTIN  != null) && !proTIN .equalsIgnoreCase("na")
                            && !proTIN.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_proTIN_lcs", proTIN );
                    }

                    String proPTAN = person.getPTAN();
                    if ((proPTAN != null) && !proPTAN.equalsIgnoreCase("na")
                            && !proPTAN.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_proPTAN_lcs", proPTAN);
                    }


                    String firstName = person.getGivenName();
                    String lastName = person.getFamilyName();
                    String middleName = person.getMiddleName();
                    String pFirstLastName = firstName + " " + lastName;
                    String fullName = this.getFullName(firstName, middleName, lastName);

                    String legalBusinessName = person.getLegalBusinessName();
                    String associatedLegalBusiness = person.getAssociateLegalBusinessName();
                    if ((associatedLegalBusiness != null) && !associatedLegalBusiness.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_associated_legal_business_lcs", associatedLegalBusiness);
                    }

                    if ((firstName != null) && !firstName.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_firstname_lcs", firstName);
                    }
                    if ((lastName != null) && !lastName.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_lastname_lcs", lastName);
                    }
                    if ((pFirstLastName != null) && !pFirstLastName.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_first_last_name_lcs", pFirstLastName);
                    }
                    if ((fullName != null) && !fullName.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_fullname_lcs", fullName);
                    }

                    if ((legalBusinessName != null) && !legalBusinessName.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_legal_business_lcs", legalBusinessName);
                    }
                    String associatedTin = person.getAssociateTIN();
                    if ((associatedTin != null) && !associatedTin.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_associated_tin_lcs", associatedTin);
                    }
                    String associatedNpi = person.getAssociateNPI();
                    if ((associatedNpi != null) && !associatedNpi.trim().equalsIgnoreCase("")) {
                        additionalProperties.put("case_provider_associated_npi_lcs", associatedNpi);
                    }


                    String assocFirstName = person.getAssociateFirstName();
                    String assoclastName = person.getAssociateLastName();
                    String assocFullName = assocFirstName + ' ' + assoclastName;

                    if ( ((assocFirstName != null) && !assocFirstName.trim().equalsIgnoreCase(""))
                            && (assoclastName != null) && !assoclastName.trim().equalsIgnoreCase("")) {

                        additionalProperties.put("case_associate_full_name_lcs", assocFullName.trim());
                    }

                }
            }
        }

    }

    @Override
    public JSONArray childrenUpdatesToSolr(CaseFile in)
    {
        JSONArray docUpdates = fileAclSolrUpdateHelper.buildFileAclUpdates(in.getContainer().getId(), in);
        List<Long> childTasks = businessProcessDao.findTasksIdsForParentObjectIdAndParentObjectType(in.getObjectType(), in.getId());
        childTasks.forEach(it -> {
            JSONObject doc = searchAccessControlFields.buildParentAccessControlFieldsUpdate(in, String.format("%d-%s", it,
                    TaskConstants.OBJECT_TYPE));
            docUpdates.put(doc);
        });
        return docUpdates;
    }

    @Override
    public boolean isAcmObjectTypeSupported(Class acmObjectType)
    {
        return CaseFile.class.equals(acmObjectType);
    }

    public UserDao getUserDao()
    {
        return userDao;
    }

    public void setUserDao(UserDao userDao)
    {
        this.userDao = userDao;
    }

    public CaseFileDao getCaseFileDao()
    {
        return caseFileDao;
    }

    public void setCaseFileDao(CaseFileDao caseFileDao)
    {
        this.caseFileDao = caseFileDao;
    }

    public SearchAccessControlFields getSearchAccessControlFields()
    {
        return searchAccessControlFields;
    }

    public void setSearchAccessControlFields(SearchAccessControlFields searchAccessControlFields)
    {
        this.searchAccessControlFields = searchAccessControlFields;
    }

    @Override
    public Class<?> getAcmObjectTypeSupported()
    {
        return CaseFile.class;
    }

    public FileAclSolrUpdateHelper getFileAclSolrUpdateHelper()
    {
        return fileAclSolrUpdateHelper;
    }

    public void setFileAclSolrUpdateHelper(FileAclSolrUpdateHelper fileAclSolrUpdateHelper)
    {
        this.fileAclSolrUpdateHelper = fileAclSolrUpdateHelper;
    }

    public BusinessProcessDao getBusinessProcessDao()
    {
        return businessProcessDao;
    }

    public void setBusinessProcessDao(BusinessProcessDao businessProcessDao)
    {
        this.businessProcessDao = businessProcessDao;
    }
}
