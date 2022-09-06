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

import com.armedia.acm.calendar.config.model.PurgeOptions;
import com.armedia.acm.calendar.config.service.CalendarConfigurationException;
import com.armedia.acm.objectonverter.AcmUnmarshaller;
import com.armedia.acm.objectonverter.ObjectConverter;
import com.armedia.acm.plugins.casefile.model.CaseFile;
import com.armedia.acm.plugins.casefile.model.CaseFileConstants;
import com.armedia.acm.plugins.casefile.utility.CaseFileEventUtility;
import com.armedia.acm.plugins.outlook.service.OutlookContainerCalendarService;
import com.armedia.acm.plugins.person.model.Person;
import com.armedia.acm.plugins.person.model.PersonAssociation;
import com.armedia.acm.service.objecthistory.dao.AcmAssignmentDao;
import com.armedia.acm.service.objecthistory.model.AcmAssignment;
import com.armedia.acm.service.objecthistory.model.AcmObjectHistory;
import com.armedia.acm.service.objecthistory.model.AcmObjectHistoryEvent;
import com.armedia.acm.service.objecthistory.service.AcmObjectHistoryEventPublisher;
import com.armedia.acm.service.objecthistory.service.AcmObjectHistoryService;
import com.armedia.acm.service.outlook.dao.AcmOutlookFolderCreatorDao;
import com.armedia.acm.service.outlook.model.AcmOutlookUser;
import com.armedia.acm.service.outlook.service.OutlookCalendarAdminServiceExtension;
import com.armedia.acm.services.holiday.service.DateTimeService;
import com.armedia.acm.services.participants.model.AcmParticipant;
import com.armedia.acm.services.participants.utils.ParticipantUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;

public class CaseFileEventListener implements ApplicationListener<AcmObjectHistoryEvent>
{
    /**
     * Logger instance.
     */
    private final Logger log = LogManager.getLogger(getClass());

    private AcmObjectHistoryService acmObjectHistoryService;
    private AcmObjectHistoryEventPublisher acmObjectHistoryEventPublisher;
    private CaseFileEventUtility caseFileEventUtility;
    private AcmAssignmentDao acmAssignmentDao;
    private OutlookContainerCalendarService calendarService;
    private boolean shouldDeleteCalendarFolder;
    private List<String> caseFileStatusClosed;
    private ObjectConverter objectConverter;
    private DateTimeService dateTimeService;

    private OutlookCalendarAdminServiceExtension calendarAdminService;

    private AcmOutlookFolderCreatorDao folderCreatorDao;

    @Override
    public void onApplicationEvent(AcmObjectHistoryEvent event)
    {
        if (event != null)
        {
            AcmObjectHistory acmObjectHistory = (AcmObjectHistory) event.getSource();

            boolean isCaseFile = checkExecution(acmObjectHistory.getObjectType());

            if (isCaseFile)
            {
                // Converter for JSON string to Object
                AcmUnmarshaller converter = getObjectConverter().getJsonUnmarshaller();

                String jsonUpdatedCaseFile = acmObjectHistory.getObjectString();
                CaseFile updatedCaseFile = converter.unmarshall(jsonUpdatedCaseFile, CaseFile.class);

                AcmAssignment acmAssignment = createAcmAssignment(updatedCaseFile);

                AcmObjectHistory acmObjectHistoryExisting = getAcmObjectHistoryService().getAcmObjectHistory(updatedCaseFile.getId(),
                        CaseFileConstants.OBJECT_TYPE);

                if (acmObjectHistoryExisting != null)
                {
                    String json = acmObjectHistoryExisting.getObjectString();
                    CaseFile existing = converter.unmarshall(json, CaseFile.class);

                    if (existing != null)
                    {
                        acmAssignment.setOldAssignee(ParticipantUtils.getAssigneeIdFromParticipants(existing.getParticipants()));

                        if (isPriorityChanged(existing, updatedCaseFile))
                        {
                            getCaseFileEventUtility().raiseCaseFileModifiedEvent(updatedCaseFile, event.getIpAddress(), "priority.changed");
                        }

                        String detailsChangeDescription = "- ";
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");

                        //Provider Information
                        String updatedCaseTaxonomy = updatedCaseFile.getCaseTaxonomy();
                        String caseTaxonomy = existing.getCaseTaxonomy();

                        if (!Objects.equals(updatedCaseTaxonomy, caseTaxonomy)) {
                            detailsChangeDescription += "Taxonomy changed from " + caseTaxonomy + " to " + updatedCaseTaxonomy + ", ";
                        }

                        String updatedCaseApplicationType = updatedCaseFile.getCaseApplicationType();
                        String caseApplicationType = existing.getCaseApplicationType();

                        if (!Objects.equals(updatedCaseApplicationType, caseApplicationType)) {
                            detailsChangeDescription += "Application Type changed from " + caseApplicationType + " to " + updatedCaseApplicationType + ", ";
                        }

                        Date updatedCaseReportAlertDate = updatedCaseFile.getCaseReportAlertDate();
                        Date caseReportAlertDate = existing.getCaseReportAlertDate();

                        if (!Objects.equals(updatedCaseReportAlertDate, caseReportAlertDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseReportAlertDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseReportAlertDate);
                            }

                            if(updatedCaseReportAlertDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedCaseReportAlertDate);
                            }

                            detailsChangeDescription += "Report Alert Date changed from " + orgDate + " to " +  updatedDate + ", ";
                        }

                        //Criminal Action
                        String updatedCaseConvictedIndividual = updatedCaseFile.getCaseConvictedIndividual();
                        String caseConvictedIndividual = existing.getCaseConvictedIndividual();

                        if (!Objects.equals(updatedCaseConvictedIndividual, caseConvictedIndividual)) {
                            detailsChangeDescription += "Convicted Individual First Name changed from " + caseConvictedIndividual + " to " + updatedCaseConvictedIndividual + ", ";
                        }

                        String updatedCaseConvictedIndividualLastName = updatedCaseFile.getCaseConvictedIndividualLastName();
                        String caseConvictedIndividualLastName = existing.getCaseConvictedIndividualLastName();

                        if (!Objects.equals(updatedCaseConvictedIndividualLastName, caseConvictedIndividualLastName)) {
                            detailsChangeDescription += "Convicted Individual Last Name changed from " + caseConvictedIndividualLastName + " to " + updatedCaseConvictedIndividualLastName + ", ";
                        }

                        String updatedCaseConvictedIndividualTin = updatedCaseFile.getCaseConvictedIndividualTin();
                        String caseConvictedIndividualTin = existing.getCaseConvictedIndividualTin();

                        if (!Objects.equals(updatedCaseConvictedIndividualTin, caseConvictedIndividualTin)) {
                            detailsChangeDescription += "Convicted Individual's TIN changed from " + caseConvictedIndividualTin + " to " + updatedCaseConvictedIndividualTin + ", ";
                        }

                        String updatedCaseTenYearsConvDate = updatedCaseFile.getCaseTenYearsConvDate();
                        String caseTenYearsConvDate = existing.getCaseTenYearsConvDate();

                        if (!Objects.equals(updatedCaseTenYearsConvDate, caseTenYearsConvDate)) {
                            detailsChangeDescription += "10 Years from Conviction Date changed from " + caseTenYearsConvDate + " to " + updatedCaseTenYearsConvDate + ", ";
                        }

                        //Sanction Action  - ALL PROVIDER INFO

                        //License Action
                        String updatedCaseReportingWeek = updatedCaseFile.getCaseReportingWeek();
                        String caseReportingWeek = existing.getCaseReportingWeek();

                        if (!Objects.equals(updatedCaseReportingWeek, caseReportingWeek)) {
                            detailsChangeDescription += "Reporting Week changed from " + caseReportingWeek + " to " + updatedCaseReportingWeek + ", ";
                        }


                        //OPT Medicaid Action
                        String updatedCaseStateMedicaidAgency = updatedCaseFile.getCaseStateMedicaidAgency();
                        String caseStateMedicaidAgency = existing.getCaseStateMedicaidAgency();

                        if (!Objects.equals(updatedCaseStateMedicaidAgency, caseStateMedicaidAgency)) {
                            detailsChangeDescription += "State Medicaid Agency changed from " + caseStateMedicaidAgency + " to " + updatedCaseStateMedicaidAgency + ", ";
                        }

                        String updatedCaseTerminationType = updatedCaseFile.getCaseTerminationType();
                        String caseTerminationType = existing.getCaseTerminationType();

                        if (!Objects.equals(updatedCaseTerminationType, caseTerminationType)) {
                            detailsChangeDescription += "Termination Type changed from " + caseTerminationType + " to " + updatedCaseTerminationType + ", ";
                        }

                        Date updatedCaseTerminationEffDate = updatedCaseFile.getCaseTerminationEffDate();
                        Date caseTerminationEffDate = existing.getCaseTerminationEffDate();

                        if (!Objects.equals(updatedCaseTerminationEffDate, caseTerminationEffDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseTerminationEffDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseTerminationEffDate);
                            }

                            if(updatedCaseTerminationEffDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedCaseTerminationEffDate);
                            }
                            detailsChangeDescription += "Termination Effective Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        String updatedCaseEnrollmentBarExpDate = updatedCaseFile.getCaseEnrollmentBarExpDate();
                        String caseEnrollmentBarExpDate = existing.getCaseEnrollmentBarExpDate();

                        if (!Objects.equals(updatedCaseEnrollmentBarExpDate, caseEnrollmentBarExpDate)) {
                            detailsChangeDescription += "Enrollment Bar Expiration Date changed from " + caseEnrollmentBarExpDate + " to " + updatedCaseEnrollmentBarExpDate + ", ";
                        }

                        Date updatedCaseReinsTerminationEffDate = updatedCaseFile.getCaseReinsTerminationEffDate();
                        Date caseReinsTerminationEffDate = existing.getCaseReinsTerminationEffDate();

                        if (!Objects.equals(updatedCaseReinsTerminationEffDate, caseReinsTerminationEffDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseReinsTerminationEffDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseReinsTerminationEffDate);
                            }

                            if(updatedCaseReinsTerminationEffDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedCaseReinsTerminationEffDate);
                            }
                            detailsChangeDescription += "Reinstated Termination Effective Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        Date updatedCaseRecindTerminationEffDate = updatedCaseFile.getCaseRecindTerminationEffDate();
                        Date caseRecindTerminationEffDate = existing.getCaseRecindTerminationEffDate();

                        if (!Objects.equals(updatedCaseRecindTerminationEffDate, caseRecindTerminationEffDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseRecindTerminationEffDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseRecindTerminationEffDate);
                            }

                            if(updatedCaseRecindTerminationEffDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedCaseRecindTerminationEffDate);
                            }
                            detailsChangeDescription += "Rescinded Termination Effective Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        String updateCaseAppealsPeriodExpired = updatedCaseFile.getCaseAppealsPeriodExpired();
                        String caseAppealsPeriodExpired = existing.getCaseAppealsPeriodExpired();

                        if (!Objects.equals(updateCaseAppealsPeriodExpired, caseAppealsPeriodExpired)) {
                            detailsChangeDescription += "Appeals Period Expired? changed from " + caseAppealsPeriodExpired + " to " + updateCaseAppealsPeriodExpired + ", ";
                        }

                        String updateCaseTerminationReason = updatedCaseFile.getCaseTerminationReason();
                        String caseTerminationReason = existing.getCaseTerminationReason();

                        if (!Objects.equals(updateCaseTerminationReason, caseTerminationReason)) {
                            detailsChangeDescription += "Termination Reason changed from " + caseTerminationReason + " to " + updateCaseTerminationReason + ", ";
                        }

                        String updateCaseCorrespondenceAddress = updatedCaseFile.getCaseCorrespondenceAddress();
                        String caseCorrespondenceAddress = existing.getCaseCorrespondenceAddress();

                        if (!Objects.equals(updateCaseCorrespondenceAddress, caseCorrespondenceAddress)) {
                            detailsChangeDescription += "Correspondence Address changed from " + caseCorrespondenceAddress + " to " + updateCaseCorrespondenceAddress + ", ";
                        }

                        Date updateCaseCmsAssignDate = updatedCaseFile.getCaseCmsAssignDate();
                        Date caseCmsAssignDate = existing.getCaseCmsAssignDate();

                        if (!Objects.equals(updateCaseCmsAssignDate, caseCmsAssignDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseCmsAssignDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseCmsAssignDate);
                            }

                            if(updateCaseCmsAssignDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updateCaseCmsAssignDate);
                            }
                            detailsChangeDescription += "CMS Assign Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        Date updateCaseDexVerifiedDate = updatedCaseFile.getCaseDexVerifiedDate();
                        Date caseDexVerifiedDate = existing.getCaseDexVerifiedDate();

                        if (!Objects.equals(updateCaseDexVerifiedDate, caseDexVerifiedDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseDexVerifiedDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseDexVerifiedDate);
                            }

                            if(updateCaseDexVerifiedDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updateCaseDexVerifiedDate);
                            }
                            detailsChangeDescription += "DEX Verified Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }


                        //Revise & Reissue Revocation
                        String updateCaseOrigRevAuth = updatedCaseFile.getCaseOrigRevAuth();
                        String caseOrigRevAuth = existing.getCaseOrigRevAuth();

                        if (!Objects.equals(updateCaseOrigRevAuth, caseOrigRevAuth)) {
                            detailsChangeDescription += "Original Revocation Authorities changed from " + caseOrigRevAuth + " to " + updateCaseOrigRevAuth + ", ";
                        }

                        Date updateCaseOrigRevLetterDate = updatedCaseFile.getCaseOrigRevLetterDate();
                        Date caseOrigRevLetterDate = existing.getCaseOrigRevLetterDate();

                        if (!Objects.equals(updateCaseOrigRevLetterDate, caseOrigRevLetterDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseOrigRevLetterDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseOrigRevLetterDate);
                            }

                            if(updateCaseOrigRevLetterDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updateCaseOrigRevLetterDate);
                            }
                            detailsChangeDescription += "Date of Original Revocation Letter changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        String updateCaseReviseReissueEl = updatedCaseFile.getCaseReviseReissueEl();
                        String caseReviseReissueEl = existing.getCaseReviseReissueEl();

                        if (!Objects.equals(updateCaseReviseReissueEl, caseReviseReissueEl)) {
                            detailsChangeDescription += "Revise & Reissue Eligible (Y/N) changed from " + caseReviseReissueEl + " to " + updateCaseReviseReissueEl + ", ";
                        }

                        Date updateCaseEffectiveLetterDate = updatedCaseFile.getCaseEffectiveLetterDate();
                        Date caseEffectiveLetterDate = existing.getCaseEffectiveLetterDate();

                        if (!Objects.equals(updateCaseEffectiveLetterDate, caseEffectiveLetterDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseEffectiveLetterDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseEffectiveLetterDate);
                            }

                            if(updateCaseEffectiveLetterDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updateCaseEffectiveLetterDate);
                            }
                            detailsChangeDescription += "Effective Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        String updateCaseReviseReissueOutcome = updatedCaseFile.getCaseReviseReissueOutcome();
                        String caseReviseReissueOutcome = existing.getCaseReviseReissueOutcome();

                        if (!Objects.equals(updateCaseReviseReissueOutcome, caseReviseReissueOutcome)) {
                            detailsChangeDescription += "Outcome of Revise & Reissue changed from " + caseReviseReissueOutcome + " to " + updateCaseReviseReissueOutcome + ", ";
                        }

                        Date updateCaseRevreiActionDate = updatedCaseFile.getCaseRevreiActionDate();
                        Date caseRevreiActionDate = existing.getCaseRevreiActionDate();

                        if (!Objects.equals(updateCaseRevreiActionDate, caseRevreiActionDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseRevreiActionDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseRevreiActionDate);
                            }

                            if(updateCaseRevreiActionDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updateCaseRevreiActionDate);
                            }
                            detailsChangeDescription += "Action Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }


                        //Not Actionable Reason

                        String updateCaseNotActionableReason = updatedCaseFile.getCaseNotActionableReason();
                        String caseNotActionableReason = existing.getCaseNotActionableReason();

                        if (!Objects.equals(updateCaseNotActionableReason, caseNotActionableReason)) {
                            detailsChangeDescription += "Not Actionable Reason changed from " + caseNotActionableReason + " to " + updateCaseNotActionableReason + ", ";
                        }


                        //Recommended Outcome
                        String updateCaseAdminActionsOutcome = updatedCaseFile.getCaseAdminActionsOutcome();
                        String caseAdminActionsOutcome = existing.getCaseAdminActionsOutcome();

                        if (!Objects.equals(updateCaseAdminActionsOutcome, caseAdminActionsOutcome)) {
                            detailsChangeDescription += "Recommended Outcome of Admin Actions changed from " + caseAdminActionsOutcome + " to " + updateCaseAdminActionsOutcome + ", ";
                        }

                        String updateCaseRecLengthEnrollBar = updatedCaseFile.getCaseRecLengthEnrollBar();
                        String caseRecLengthEnrollBar = existing.getCaseRecLengthEnrollBar();

                        if (!Objects.equals(updateCaseRecLengthEnrollBar, caseRecLengthEnrollBar)) {
                            detailsChangeDescription += "Recommended Length of Enrollment Bar changed from " + caseRecLengthEnrollBar + " to " + updateCaseRecLengthEnrollBar + ", ";
                        }

                        String updateCaseRecAuthCitedActionLetter = updatedCaseFile.getCaseRecAuthCitedActionLetter();
                        String caseRecAuthCitedActionLetter = existing.getCaseRecAuthCitedActionLetter();

                        if (!Objects.equals(updateCaseRecAuthCitedActionLetter, caseRecAuthCitedActionLetter)) {
                            detailsChangeDescription += "Recommended Authorities Cited in Action Letter changed from " + caseRecAuthCitedActionLetter + " to " + updateCaseRecAuthCitedActionLetter + ", ";
                        }


                        String updateCaseRecEffDate = updatedCaseFile.getCaseRecEffDate();
                        String caseRecEffDate = existing.getCaseRecEffDate();

                        if (!Objects.equals(updateCaseRecEffDate, caseRecEffDate)) {
                            detailsChangeDescription += "Recommended Effective Date changed from " + caseRecEffDate + " to " + updateCaseRecEffDate + ", ";
                        }

                    //FINAL OUTCOME
                        String updatedFinalOutAdminAct = updatedCaseFile.getCaseFinalOutAdminAct();
                        String finalOutAdminAct = existing.getCaseFinalOutAdminAct();

                        if (!Objects.equals(updatedFinalOutAdminAct, finalOutAdminAct)) {
                            detailsChangeDescription += "Final Outcome of Admin Actions changed from " + finalOutAdminAct + " to " + updatedFinalOutAdminAct + ", ";
                        }

                        String updatedCaseOptCmsDecision = updatedCaseFile.getCaseOptCmsDecision();
                        String caseOptCmsDecision = existing.getCaseOptCmsDecision();

                        if (!Objects.equals(updatedCaseOptCmsDecision, caseOptCmsDecision)) {
                            detailsChangeDescription += "OPT CMS Decision changed from " + caseOptCmsDecision + " to " + updatedCaseOptCmsDecision + ", ";
                        }

                        String updatedCaseRevAuthCitedActionLetter = updatedCaseFile.getCaseRevAuthCitedActionLetter();
                        String caseRevAuthCitedActionLetter = existing.getCaseRevAuthCitedActionLetter();

                        if (!Objects.equals(updatedCaseRevAuthCitedActionLetter, caseRevAuthCitedActionLetter)) {
                            detailsChangeDescription += "Authorities Cited in Final Letter changed from " + caseRevAuthCitedActionLetter + " to " + updatedCaseRevAuthCitedActionLetter + ", ";
                        }

                        String updatedCaseLengthReEnrollBar = updatedCaseFile.getCaseLengthReEnrollBar();
                        String caseLengthReEnrollBar = existing.getCaseLengthReEnrollBar();

                        if (!Objects.equals(updatedCaseLengthReEnrollBar, caseLengthReEnrollBar)) {
                            detailsChangeDescription += "Length of Re-Enrollment Bar changed from " + caseLengthReEnrollBar + " to " + updatedCaseLengthReEnrollBar + ", ";
                        }

                        String updatedCaseRevEffActionDate = updatedCaseFile.getCaseRevEffActionDate();
                        String caseRevEffActionDate = existing.getCaseRevEffActionDate();

                        if (!Objects.equals(updatedCaseRevEffActionDate, caseRevEffActionDate)) {
                            detailsChangeDescription += "Effective Date changed from " + caseRevEffActionDate + " to " + updatedCaseRevEffActionDate + ", ";
                        }

                        Date updatedCaseFinalOutActionDate = updatedCaseFile.getCaseFinalOutActionDate();
                        Date caseFinalOutActionDate = existing.getCaseFinalOutActionDate();

                        if (!Objects.equals(updatedCaseFinalOutActionDate, caseFinalOutActionDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseFinalOutActionDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseFinalOutActionDate);
                            }

                            if(updatedCaseFinalOutActionDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedCaseFinalOutActionDate);
                            }
                            detailsChangeDescription += "Action Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }


                    //Case History
                        Date updatedResubDueDate = updatedCaseFile.getCaseResubDueDate();
                        Date caseResubDueDate = existing.getCaseResubDueDate();

                        if (!Objects.equals(updatedResubDueDate, caseResubDueDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseResubDueDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseResubDueDate);
                            }

                            if(updatedResubDueDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedResubDueDate);
                            }
                            detailsChangeDescription += "Resubmission Due Date changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        Date updatedReqDocumentationDate = updatedCaseFile.getCaseReqDocumentationDate();
                        Date caseReqDocumentationDate = existing.getCaseReqDocumentationDate();

                        if (!Objects.equals(updatedReqDocumentationDate, caseReqDocumentationDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseReqDocumentationDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseReqDocumentationDate);
                            }

                            if(updatedReqDocumentationDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedReqDocumentationDate);
                            }
                            detailsChangeDescription += "Date Requested Documentation changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        Date updatedDocumentationRecDate = updatedCaseFile.getCaseDocumentationRecDate();
                        Date caseReqDocumentationRecDate = existing.getCaseDocumentationRecDate();

                        if (!Objects.equals(updatedDocumentationRecDate, caseReqDocumentationRecDate)) {
                            String updatedDate = "";
                            String orgDate = "";
                            if(caseReqDocumentationRecDate == null){
                                orgDate = "n/a";
                            } else {
                                orgDate = simpleDateFormat.format(caseReqDocumentationRecDate);
                            }

                            if(updatedDocumentationRecDate == null){
                                updatedDate = "n/a";
                            } else {
                                updatedDate = simpleDateFormat.format(updatedDocumentationRecDate);
                            }
                            detailsChangeDescription += "Date Documentation Received changed from " + orgDate + " to " + updatedDate + ", ";
                        }

                        String updatedAgencyRequested = updatedCaseFile.getCaseAgencyRequested();
                        String caseAgencyRequested = existing.getCaseAgencyRequested();

                        if (!Objects.equals(updatedAgencyRequested, caseAgencyRequested)) {
                            detailsChangeDescription += "Agency Requested changed from " + caseAgencyRequested + " to " + updatedAgencyRequested + ", ";
                        }

                        Boolean updatedBoardDocument = updatedCaseFile.getCaseBoardDocument();
                        Boolean caseBoardDocument = existing.getCaseBoardDocument();

                        if (!Objects.equals(updatedBoardDocument, caseBoardDocument)) {
                            detailsChangeDescription += "Board Documents changed from " + caseBoardDocument + " to " + updatedBoardDocument + ", ";
                        }

                        Boolean updatedCourtDocument = updatedCaseFile.getCaseCourtDocument();
                        Boolean caseCourtDocument = existing.getCaseCourtDocument();

                        if (!Objects.equals(updatedCourtDocument, caseCourtDocument)) {
                            detailsChangeDescription += "Court Documents changed from " + caseCourtDocument + " to " + updatedCourtDocument + ", ";
                        }

                        Boolean updatedSelectedForInlineReview = updatedCaseFile.getCaseSelectedForInlineReview();
                        Boolean caseSelectedForInlineReview = existing.getCaseSelectedForInlineReview();

                        if (!Objects.equals(updatedSelectedForInlineReview, caseSelectedForInlineReview)) {
                            detailsChangeDescription += "Selected for In-Line Review changed from " + caseSelectedForInlineReview + " to " + updatedSelectedForInlineReview + ", ";
                        }

                        String updatedMultipleAlert = updatedCaseFile.getCaseMultipleAlert();
                        String caseMultipleAlert = existing.getCaseMultipleAlert();

                        if (!Objects.equals(updatedMultipleAlert, caseMultipleAlert)) {
                            detailsChangeDescription += "Multiple Alert changed from " + caseMultipleAlert + " to " + updatedMultipleAlert + ", ";
                        }

                        String updatedPrevCMSAnalyst = updatedCaseFile.getCasePrevCMSAnalyst();
                        String casePrevCMSAnalyst = existing.getCasePrevCMSAnalyst();

                        if (!Objects.equals(updatedPrevCMSAnalyst, casePrevCMSAnalyst)) {
                            detailsChangeDescription += "Multiple Alert changed from " + casePrevCMSAnalyst + " to " + updatedPrevCMSAnalyst + ", ";
                        }


                    //Provider
                        Date updatedConvictionDate, caseConvictionDate, updatedDocketRequestDate, caseDocketRequestDate, updatedDocketResponseDate,
                                caseDocketResponseDate, updatedAssociateSanctionDate, caseAssociateSanctionDate, updatedLicenseExpirationDate,
                                caseLicenseExpirationDate, updatedLicenseAlertDate, caseLicenseAlertDate;
                        updatedConvictionDate = caseConvictionDate = updatedDocketRequestDate = caseDocketRequestDate = updatedDocketResponseDate =
                                caseDocketResponseDate  = updatedAssociateSanctionDate = caseAssociateSanctionDate = updatedLicenseExpirationDate =
                                caseLicenseExpirationDate = updatedLicenseAlertDate = caseLicenseAlertDate =  null;


                        String updatedSSN, caseSSN, updatedNPI , caseNPI, updatedPTAN, casePTAN, updatedPecosID, casePecosID,updatedPecosState, casePecosState,
                                updatedPecosStatus, casePecosStatus, updatedContractorID, caseContractorID, updatedSpecialty, caseSpecialty,
                                updatedContractorName, caseContractorName, updatedLBN, caseLBN, updatedLbnEin, caseLbnEin, updatedFedstateCaseType,
                                caseFedstateCaseType, updatedCaseNumber, caseCaseNumber, updatedOffenseType, caseOffenseType, updatedCourtName, caseCourtName,
                                updatedDocketStatus, caseDocketStatus, updatedAssociateFirstName, caseAssociateFirstName, updatedAssociateLegalBusinessName,
                                caseAssociateLegalBusinessName, updatedAssociateNPI, caseAssociateNPI, updatedAssociateTIN, caseAssociateTIN, updatedAssociateTinType,
                                caseAssociateTinType, updatedAssociateSanctionCode, caseAssociateSanctionCode, updatedAssociateRole, caseAssociateRole,
                                updatedAssociateLastName, caseAssociateLastName, updatedLicenseNumber, caseLicenseNumber, updatedLicenseStatus, caseLicenseStatus,
                                updatedLicenseQualifierSanction, caseLicenseQualifierSanction, updatedLicenseState, caseLicenseState;

                        updatedSSN = caseSSN =  updatedNPI = caseNPI = updatedPTAN = casePTAN = updatedPecosID = casePecosID = updatedPecosState = casePecosState =
                                updatedPecosStatus = casePecosStatus = updatedContractorID = caseContractorID = updatedSpecialty = caseSpecialty =
                                updatedContractorName = caseContractorName = updatedLBN = caseLBN = updatedLbnEin = caseLbnEin = updatedFedstateCaseType = caseFedstateCaseType =
                                updatedCaseNumber = caseCaseNumber = updatedOffenseType = caseOffenseType = updatedCourtName = caseCourtName = updatedDocketStatus =
                                updatedAssociateLastName = caseAssociateLastName = caseDocketStatus = updatedAssociateFirstName = caseAssociateFirstName =
                                updatedAssociateLegalBusinessName = caseAssociateLegalBusinessName = updatedAssociateNPI = caseAssociateNPI =  updatedAssociateTIN =
                                caseAssociateTIN = updatedAssociateTinType = caseAssociateTinType = updatedAssociateSanctionCode  = caseAssociateSanctionCode =
                                updatedAssociateRole = caseAssociateRole = updatedLicenseNumber = caseLicenseNumber = updatedLicenseStatus = caseLicenseStatus =
                                updatedLicenseQualifierSanction = caseLicenseQualifierSanction = updatedLicenseState = caseLicenseState = "";
                        for(PersonAssociation ex: existing.getPersonAssociations()) {
                            for(PersonAssociation up: updatedCaseFile.getPersonAssociations()) {

                                Person existingPerson = null;
                                Person updatedPerson = null;
                                if (ex.getPersonType().equalsIgnoreCase("initiator")) {
                                    existingPerson = ex.getPerson();
                                }
                                if (up.getPersonType().equalsIgnoreCase("initiator")) {
                                    updatedPerson = up.getPerson();
                                }

                            //Provider Information
                                updatedSSN = updatedPerson.getSsn();
                                caseSSN = existingPerson.getSsn();

                                if (!Objects.equals(updatedSSN, caseSSN)) {
                                    detailsChangeDescription += "SSN/EIN changed from " + caseSSN + " to " + updatedSSN + ", ";
                                }


                                updatedNPI = updatedPerson.getNpi();
                                caseNPI = existingPerson.getNpi();

                                if (!Objects.equals(updatedNPI, caseNPI)) {
                                    detailsChangeDescription += "NPI changed from " + caseNPI + " to " + updatedNPI + ", ";
                                }

                                updatedPTAN = updatedPerson.getPTAN();
                                casePTAN = existingPerson.getPTAN();

                                if (!Objects.equals(updatedPTAN, casePTAN)) {
                                    detailsChangeDescription += "PTAN changed from " + casePTAN + " to " + updatedPTAN + ", ";
                                }

                                updatedPecosID = updatedPerson.getPecosEnrollmentID();
                                casePecosID = existingPerson.getPecosEnrollmentID();

                                if (!Objects.equals(updatedPecosID, casePecosID)) {
                                    detailsChangeDescription += "PECOS Enrollment ID changed from " + casePecosID + " to " + updatedPecosID + ", ";
                                }

                                updatedPecosState = updatedPerson.getPecosEnrollmentState();
                                casePecosState = existingPerson.getPecosEnrollmentState();

                                if (!Objects.equals(updatedPecosState, casePecosState)) {
                                    detailsChangeDescription += "PECOS Enrollment State changed from " + casePecosState + " to " + updatedPecosState + ", ";
                                }

                                updatedPecosStatus = updatedPerson.getPecosEnrollmentStatus();
                                casePecosStatus = existingPerson.getPecosEnrollmentStatus();

                                if (!Objects.equals(updatedPecosStatus, casePecosStatus)) {
                                    detailsChangeDescription += "PECOS Enrollment Status changed from " + casePecosStatus + " to " + updatedPecosStatus + ", ";
                                }

                                updatedContractorID = updatedPerson.getContractorID();
                                caseContractorID = existingPerson.getContractorID();

                                if (!Objects.equals(updatedContractorID, caseContractorID)) {
                                    detailsChangeDescription += "Contractor ID changed from " + caseContractorID + " to " + updatedContractorID + ", ";
                                }

                                updatedSpecialty = updatedPerson.getProviderSpecialty();
                                caseSpecialty = existingPerson.getProviderSpecialty();

                                if (!Objects.equals(updatedSpecialty, caseSpecialty)) {
                                    detailsChangeDescription += "Specialty changed from " + caseSpecialty + " to " + updatedSpecialty + ", ";
                                }

                                updatedContractorName = updatedPerson.getProviderContractorName();
                                caseContractorName = existingPerson.getProviderContractorName();

                                if (!Objects.equals(updatedContractorName, caseContractorName)) {
                                    detailsChangeDescription += "Contractor Name changed from " + caseContractorName + " to " + updatedContractorName + ", ";
                                }

                                updatedLBN = updatedPerson.getLegalBusinessName();
                                caseLBN = existingPerson.getLegalBusinessName();

                                if (!Objects.equals(updatedLBN, caseLBN)) {
                                    detailsChangeDescription += "Legal Business Name changed from " + caseLBN + " to " + updatedLBN + ", ";
                                }


                                updatedLbnEin = updatedPerson.getProviderLbnEin();
                                caseLbnEin = existingPerson.getProviderLbnEin();

                                if (!Objects.equals(updatedLbnEin, caseLbnEin)) {
                                    detailsChangeDescription += "Legal Business Name EIN changed from " + caseLbnEin + " to " + updatedLbnEin + ", ";
                                }


                            //Criminal Action
                                updatedFedstateCaseType = updatedPerson.getDefaultIdentification().getIdCaseType();
                                caseFedstateCaseType = existingPerson.getDefaultIdentification().getIdCaseType();

                                if (!Objects.equals(updatedFedstateCaseType, caseFedstateCaseType)) {
                                    detailsChangeDescription += "Federal or State Crime (Case Type) changed from " + caseFedstateCaseType + " to " + updatedFedstateCaseType + ", ";
                                }

                                updatedCaseNumber = updatedPerson.getDefaultIdentification().getIdCaseNumber();
                                caseCaseNumber = existingPerson.getDefaultIdentification().getIdCaseNumber();

                                if (!Objects.equals(updatedCaseNumber, caseCaseNumber)) {
                                    detailsChangeDescription += "Case Number changed from " + caseCaseNumber + " to " + updatedCaseNumber + ", ";
                                }

                                updatedOffenseType = updatedPerson.getDefaultIdentification().getIdOffenseType();
                                caseOffenseType = existingPerson.getDefaultIdentification().getIdOffenseType();

                                if (!Objects.equals(updatedOffenseType, caseOffenseType)) {
                                    detailsChangeDescription += "Offense Type changed from " + caseOffenseType + " to " + updatedOffenseType + ", ";
                                }

                                updatedConvictionDate = updatedPerson.getDefaultIdentification().getIdConvictionDate();
                                caseConvictionDate = existingPerson.getDefaultIdentification().getIdConvictionDate();

                                if (!Objects.equals(updatedConvictionDate, caseConvictionDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseConvictionDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseConvictionDate);
                                    }

                                    if(updatedConvictionDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedConvictionDate);
                                    }
                                    detailsChangeDescription += "Conviction Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }

                                updatedCourtName = updatedPerson.getDefaultIdentification().getIdCourtName();
                                caseCourtName = existingPerson.getDefaultIdentification().getIdCourtName();

                                if (!Objects.equals(updatedCourtName, caseCourtName)) {
                                    detailsChangeDescription += "Court Name changed from " + caseCourtName + " to " + updatedCourtName + ", ";
                                }


                                updatedDocketRequestDate = updatedPerson.getDefaultIdentification().getIdDocketRequestDate();
                                caseDocketRequestDate = existingPerson.getDefaultIdentification().getIdDocketRequestDate();

                                if (!Objects.equals(updatedDocketRequestDate, caseDocketRequestDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseDocketRequestDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseDocketRequestDate);
                                    }

                                    if(updatedDocketRequestDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedDocketRequestDate);
                                    }
                                    detailsChangeDescription += "Docket Request Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }


                                updatedDocketResponseDate = updatedPerson.getDefaultIdentification().getIdDocketResponseDate();
                                caseDocketResponseDate = existingPerson.getDefaultIdentification().getIdDocketResponseDate();

                                if (!Objects.equals(updatedDocketResponseDate, caseDocketResponseDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseDocketResponseDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseDocketResponseDate);
                                    }

                                    if(updatedDocketResponseDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedDocketResponseDate);
                                    }
                                    detailsChangeDescription += "Docket Response Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }

                                updatedDocketStatus = updatedPerson.getDefaultIdentification().getIdDocketStatus();
                                caseDocketStatus = existingPerson.getDefaultIdentification().getIdDocketStatus();

                                if (!Objects.equals(updatedDocketStatus, caseDocketStatus)) {
                                    detailsChangeDescription += "Docket Status changed from " + caseDocketStatus + " to " + updatedDocketStatus + ", ";
                                }

                            //Sanction Action  - ALL PROVIDER INFO
                                updatedAssociateFirstName = updatedPerson.getAssociateFirstName();
                                caseAssociateFirstName = existingPerson.getAssociateFirstName();

                                if (!Objects.equals(updatedAssociateFirstName, caseAssociateFirstName)) {
                                    detailsChangeDescription += "Sanctioned Associate First Name changed from " + caseAssociateFirstName + " to " + updatedAssociateFirstName + ", ";
                                }

                                updatedAssociateLastName = updatedPerson.getAssociateLastName();
                                caseAssociateLastName = existingPerson.getAssociateLastName();

                                if (!Objects.equals(updatedAssociateLastName, caseAssociateLastName)) {
                                    detailsChangeDescription += "Sanctioned Associate Last Name changed from " + caseAssociateLastName + " to " + updatedAssociateLastName + ", ";
                                }

                                updatedAssociateLegalBusinessName = updatedPerson.getAssociateLegalBusinessName();
                                caseAssociateLegalBusinessName = existingPerson.getAssociateLegalBusinessName();

                                if (!Objects.equals(updatedAssociateLegalBusinessName, caseAssociateLegalBusinessName)) {
                                    detailsChangeDescription += "OIG Sanctioned Business Name changed from " + caseAssociateLegalBusinessName + " to " + updatedAssociateLegalBusinessName + ", ";
                                }

                                updatedAssociateNPI = updatedPerson.getAssociateNPI();
                                caseAssociateNPI = existingPerson.getAssociateNPI();

                                if (!Objects.equals(updatedAssociateNPI, caseAssociateNPI)) {
                                    detailsChangeDescription += "Sanctioned Associate NPI changed from " + caseAssociateNPI + " to " + updatedAssociateNPI + ", ";
                                }

                                updatedAssociateTIN = updatedPerson.getAssociateTIN();
                                caseAssociateTIN = existingPerson.getAssociateTIN();

                                if (!Objects.equals(updatedAssociateTIN, caseAssociateTIN)) {
                                    detailsChangeDescription += "Sanctioned Associate TIN changed from " + caseAssociateTIN + " to " + updatedAssociateTIN + ", ";
                                }

                                updatedAssociateTinType = updatedPerson.getAssociateTinType();
                                caseAssociateTinType = existingPerson.getAssociateTinType();

                                if (!Objects.equals(updatedAssociateTinType, caseAssociateTinType)) {
                                    detailsChangeDescription += "Sanctioned Associate TIN Type changed from " + caseAssociateTinType + " to " + updatedAssociateTinType + ", ";
                                }

                                updatedAssociateSanctionCode = updatedPerson.getAssociateSanctionCode();
                                caseAssociateSanctionCode = existingPerson.getAssociateSanctionCode();

                                if (!Objects.equals(updatedAssociateSanctionCode, caseAssociateSanctionCode)) {
                                    detailsChangeDescription += "Exclusion Type/Sanction Code changed from " + caseAssociateSanctionCode + " to " + updatedAssociateSanctionCode + ", ";
                                }

                                updatedAssociateRole = updatedPerson.getAssociateRole();
                                caseAssociateRole = existingPerson.getAssociateRole();

                                if (!Objects.equals(updatedAssociateRole, caseAssociateRole)) {
                                    detailsChangeDescription += "Sanctioned Associate Role in Affiliated Enrollment changed from " + caseAssociateRole + " to " + updatedAssociateRole + ", ";
                                }

                                updatedAssociateSanctionDate = updatedPerson.getAssociateSanctionDate();
                                caseAssociateSanctionDate = existingPerson.getAssociateSanctionDate();

                                if (!Objects.equals(updatedAssociateSanctionDate, caseAssociateSanctionDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseAssociateSanctionDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseAssociateSanctionDate);
                                    }

                                    if(updatedAssociateSanctionDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedAssociateSanctionDate);
                                    }
                                    detailsChangeDescription += "Sanctioned Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }


                            //License Action
                                updatedLicenseNumber = updatedPerson.getLicenseNumber();
                                caseLicenseNumber = existingPerson.getLicenseNumber();

                                if (!Objects.equals(updatedLicenseNumber, caseLicenseNumber)) {
                                    detailsChangeDescription += "License Number changed from " + caseLicenseNumber + " to " + updatedLicenseNumber + ", ";
                                }

                                updatedLicenseStatus = updatedPerson.getLicenseStatus();
                                caseLicenseStatus = existingPerson.getLicenseStatus();

                                if (!Objects.equals(updatedLicenseStatus, caseLicenseStatus)) {
                                    detailsChangeDescription += "License Status changed from " + caseLicenseStatus + " to " + updatedLicenseStatus + ", ";
                                }

                                updatedLicenseExpirationDate = updatedPerson.getLicenseExpirationDate();
                                caseLicenseExpirationDate = existingPerson.getLicenseExpirationDate();

                                if (!Objects.equals(updatedLicenseExpirationDate, caseLicenseExpirationDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseLicenseExpirationDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseLicenseExpirationDate);
                                    }

                                    if(updatedLicenseExpirationDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedLicenseExpirationDate);
                                    }
                                    detailsChangeDescription += "License Expiration Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }

                                updatedLicenseQualifierSanction = updatedPerson.getLicenseQualifierSanction();
                                caseLicenseQualifierSanction = existingPerson.getLicenseQualifierSanction();

                                if (!Objects.equals(updatedLicenseQualifierSanction, caseLicenseQualifierSanction)) {
                                    detailsChangeDescription += "License Qualifier/Sanction changed from " + caseLicenseQualifierSanction + " to " + updatedLicenseQualifierSanction + ", ";
                                }


                                updatedLicenseAlertDate = updatedPerson.getLicenseAlertDate();
                                caseLicenseAlertDate = existingPerson.getLicenseAlertDate();

                                if (!Objects.equals(updatedLicenseAlertDate, caseLicenseAlertDate)) {
                                    String updatedDate = "";
                                    String orgDate = "";
                                    if(caseReportAlertDate == null){
                                        orgDate = "n/a";
                                    } else {
                                        orgDate = simpleDateFormat.format(caseLicenseExpirationDate);
                                    }

                                    if(updatedLicenseAlertDate == null){
                                        updatedDate = "n/a";
                                    } else {
                                        updatedDate = simpleDateFormat.format(updatedLicenseAlertDate);
                                    }
                                    detailsChangeDescription += "License Alert Date changed from " + orgDate + " to " + updatedDate + ", ";
                                }

                                updatedLicenseState = updatedPerson.getLicenseState();
                                caseLicenseState = existingPerson.getLicenseState();

                                if (!Objects.equals(updatedLicenseState, caseLicenseState)) {
                                    detailsChangeDescription += "License State changed from " + caseLicenseState + " to " + updatedLicenseState + ", ";
                                }

                            }
                        }



                        if(!Objects.equals(updatedFinalOutAdminAct, finalOutAdminAct) || !Objects.equals(updatedCaseOptCmsDecision, caseOptCmsDecision)
                        || !Objects.equals(updatedCaseRevAuthCitedActionLetter, caseRevAuthCitedActionLetter) || !Objects.equals(updatedCaseLengthReEnrollBar, caseLengthReEnrollBar)
                        || !Objects.equals(updatedCaseRevEffActionDate, caseRevEffActionDate) || !Objects.equals(updatedCaseFinalOutActionDate, caseFinalOutActionDate)
                        || !Objects.equals(updatedCaseTaxonomy, caseTaxonomy) || !Objects.equals(updatedCaseApplicationType, caseApplicationType) || !Objects.equals(updatedCaseReportAlertDate, caseReportAlertDate)
                        || !Objects.equals(updatedCaseConvictedIndividual, caseConvictedIndividual) || !Objects.equals(updatedCaseConvictedIndividualLastName, caseConvictedIndividualLastName)
                        || !Objects.equals(updatedCaseConvictedIndividualTin, caseConvictedIndividualTin) || !Objects.equals(updatedCaseTenYearsConvDate, caseTenYearsConvDate)
                        || !Objects.equals(updatedCaseReportingWeek, caseReportingWeek) || !Objects.equals(updatedCaseStateMedicaidAgency, caseStateMedicaidAgency)
                        || !Objects.equals(updatedCaseTerminationType, caseTerminationType) || !Objects.equals(updatedCaseTerminationEffDate, caseTerminationEffDate)
                        || !Objects.equals(updatedCaseEnrollmentBarExpDate, caseEnrollmentBarExpDate) || !Objects.equals(updatedCaseReinsTerminationEffDate, caseReinsTerminationEffDate)
                        || !Objects.equals(updatedCaseRecindTerminationEffDate, caseRecindTerminationEffDate) || !Objects.equals(updateCaseAppealsPeriodExpired, caseAppealsPeriodExpired)
                        || !Objects.equals(updateCaseTerminationReason, caseTerminationReason) || !Objects.equals(updateCaseCorrespondenceAddress, caseCorrespondenceAddress)
                        || !Objects.equals(updateCaseCmsAssignDate, caseCmsAssignDate) || !Objects.equals(updateCaseDexVerifiedDate, caseDexVerifiedDate)
                        || !Objects.equals(updateCaseOrigRevAuth, caseOrigRevAuth) || !Objects.equals(updateCaseOrigRevLetterDate, caseOrigRevLetterDate)
                        || !Objects.equals(updateCaseReviseReissueEl, caseReviseReissueEl) || !Objects.equals(updateCaseEffectiveLetterDate, caseEffectiveLetterDate)
                        || !Objects.equals(updateCaseReviseReissueOutcome, caseReviseReissueOutcome) || !Objects.equals(updateCaseRevreiActionDate, caseRevreiActionDate)
                        || !Objects.equals(updateCaseAdminActionsOutcome, caseAdminActionsOutcome) || !Objects.equals(updateCaseRecLengthEnrollBar, caseRecLengthEnrollBar)
                        || !Objects.equals(updateCaseRecAuthCitedActionLetter, caseRecAuthCitedActionLetter) || !Objects.equals(updateCaseRecEffDate, caseRecEffDate)
                        || !Objects.equals(updatedSSN, caseSSN) || !Objects.equals(updatedNPI, caseNPI) || !Objects.equals(updatedPTAN, casePTAN)
                        || !Objects.equals(updatedPecosID, casePecosID) || !Objects.equals(updatedPecosState, casePecosState) || !Objects.equals(updatedPecosStatus, casePecosStatus)
                        || !Objects.equals(updatedSpecialty, caseSpecialty) || !Objects.equals(updatedContractorName, caseContractorName) || !Objects.equals(updatedLBN, caseLBN)
                        || !Objects.equals(updatedLbnEin, caseLbnEin) || !Objects.equals(updatedFedstateCaseType, caseFedstateCaseType) || !Objects.equals(updatedCaseNumber, caseCaseNumber)
                        || !Objects.equals(updatedOffenseType, caseOffenseType) || !Objects.equals(updatedConvictionDate, caseConvictionDate)
                        || !Objects.equals(updatedCourtName, caseCourtName) || !Objects.equals(updatedDocketRequestDate, caseDocketRequestDate)
                        || !Objects.equals(updatedDocketResponseDate, caseDocketResponseDate) || !Objects.equals(updatedAssociateFirstName, caseAssociateFirstName)
                        || !Objects.equals(updatedAssociateLegalBusinessName, caseAssociateLegalBusinessName) || !Objects.equals(updatedAssociateNPI, caseAssociateNPI)
                        || !Objects.equals(updatedAssociateTIN, caseAssociateTIN) || !Objects.equals(updatedAssociateTinType, caseAssociateTinType) || !Objects.equals(updatedAssociateSanctionCode, caseAssociateSanctionCode)
                        || !Objects.equals(updatedAssociateRole, caseAssociateRole) || !Objects.equals(updatedAssociateSanctionDate, caseAssociateSanctionDate) || !Objects.equals(updatedLicenseNumber, caseLicenseNumber)
                        || !Objects.equals(updatedLicenseStatus, caseLicenseStatus) || !Objects.equals(updatedLicenseExpirationDate, caseLicenseExpirationDate)
                        || !Objects.equals(updatedLicenseQualifierSanction, caseLicenseQualifierSanction) || !Objects.equals(updatedLicenseAlertDate, caseLicenseAlertDate)
                        || !Objects.equals(updatedLicenseState, caseLicenseState) || !Objects.equals(updatedResubDueDate, caseResubDueDate) || !Objects.equals(updatedReqDocumentationDate, caseReqDocumentationDate)
                        || !Objects.equals(updatedDocumentationRecDate, caseReqDocumentationRecDate) || !Objects.equals(updatedAgencyRequested, caseAgencyRequested) || !Objects.equals(updatedBoardDocument, caseBoardDocument)
                        || !Objects.equals(updatedCourtDocument, caseCourtDocument) || !Objects.equals(updatedSelectedForInlineReview, caseSelectedForInlineReview) ||  !Objects.equals(updatedMultipleAlert, caseMultipleAlert)
                        || !Objects.equals(updatedPrevCMSAnalyst, casePrevCMSAnalyst) || !Objects.equals(updateCaseNotActionableReason, caseNotActionableReason))
                        {
                            log.error("TEST");
                            getCaseFileEventUtility().raiseCaseFileModifiedEvent(updatedCaseFile, event.getIpAddress(), "details.changed", detailsChangeDescription);
                        }

                        if (isDetailsChanged(existing, updatedCaseFile))
                        {
                            getCaseFileEventUtility().raiseCaseFileModifiedEvent(updatedCaseFile, event.getIpAddress(), "details.changed");
                        }

                        String title = existing.getTitle();
                        String updatedTitle = updatedCaseFile.getTitle();
                        if (!Objects.equals(title, updatedTitle))
                        {
                            getCaseFileEventUtility().raiseCaseFileModifiedEvent(updatedCaseFile, event.getIpAddress(), "title.changed",
                                    "Case File Title changed from " + title + " to " + updatedTitle);
                        }

                        String owningGroup = ParticipantUtils.getOwningGroupIdFromParticipants(existing.getParticipants());
                        String updatedOwningGroup = ParticipantUtils.getOwningGroupIdFromParticipants(updatedCaseFile.getParticipants());
                        if (!Objects.equals(owningGroup, updatedOwningGroup))
                        {
                            AcmParticipant updatedParticipant = updatedCaseFile.getParticipants().stream()
                                    .filter(p -> "owning group".equals(p.getParticipantType())).findFirst().orElse(null);
                            getCaseFileEventUtility().raiseParticipantsModifiedInCaseFile(updatedParticipant, updatedCaseFile,
                                    event.getIpAddress(), "changed",
                                    "Owning Group Changed from " + owningGroup + " to " + updatedOwningGroup);
                        }

                        checkPersonAssociation(existing, updatedCaseFile, event.getIpAddress());

                        checkParticipants(existing, updatedCaseFile, event.getIpAddress());

                        if (isStatusChanged(existing, updatedCaseFile))
                        {
                            String calId = updatedCaseFile.getContainer().getCalendarFolderId();
                            if (shouldDeleteOnClose() && calId != null && caseFileStatusClosed.contains(updatedCaseFile.getStatus()))
                            {

                                // delete shared calendar if case closed
                                Optional<AcmOutlookUser> user = calendarAdminService
                                        .getEventListenerOutlookUser(CaseFileConstants.OBJECT_TYPE);
                                // if integration is not enabled the user will be null.
                                if (user.isPresent())
                                {
                                    getCalendarService().deleteFolder(user.get(), updatedCaseFile.getContainer(),
                                            DeleteMode.MoveToDeletedItems);
                                    folderCreatorDao.deleteObjectReference(updatedCaseFile.getId(), updatedCaseFile.getObjectType());
                                }
                            }
                            getCaseFileEventUtility().raiseCaseFileModifiedEvent(updatedCaseFile, event.getIpAddress(), "status.changed",
                                    "from " + existing.getStatus() + " to " + updatedCaseFile.getStatus());

                        }
                        if (updatedCaseFile.getDueDate() != null && existing.getDueDate() != null)
                        {
                            if (isDueDateChanged(updatedCaseFile, existing))
                            {
                                String newDate = getDateString(getDateTimeService().fromDateToClientLocalDateTime(updatedCaseFile.getDueDate()));
                                String oldDate = getDateString(getDateTimeService().fromDateToClientLocalDateTime(existing.getDueDate()));
                                String timeZone = getDateTimeService().getDefaultClientZoneId().getId();

                                getCaseFileEventUtility().raiseDueDateUpdatedEvent(updatedCaseFile, oldDate, newDate, timeZone, event.getIpAddress());
                            }
                        }
                    }
                }

                if (isAssigneeChanged(acmAssignment))
                {
                    // Save assignment change in the database
                    AcmAssignment assignmentSaved = getAcmAssignmentDao().save(acmAssignment);

                    // Raise an event
                    getAcmObjectHistoryEventPublisher().publishAssigneeChangeEvent(assignmentSaved, event.getUserId(),
                            event.getIpAddress());
                }
            }
        }
    }

    protected boolean shouldDeleteOnClose()
    {
        boolean purgeOption;
        try
        {
            purgeOption = PurgeOptions.CLOSED.equals(calendarAdminService.readConfiguration(false)
                    .getConfiguration(CaseFileConstants.OBJECT_TYPE).getPurgeOptions());
        }
        catch (CalendarConfigurationException e)
        {
            purgeOption = true;
        }
        return purgeOption && shouldDeleteCalendarFolder;
    }

    public boolean isAssigneeChanged(AcmAssignment assignment)
    {
        String newAssignee = assignment.getNewAssignee();
        String oldAssignee = assignment.getOldAssignee();
        return !Objects.equals(newAssignee, oldAssignee);
    }

    protected AcmAssignment createAcmAssignment(CaseFile updatedCaseFile)
    {
        AcmAssignment assignment = new AcmAssignment();
        assignment.setObjectId(updatedCaseFile.getId());
        assignment.setObjectTitle(updatedCaseFile.getTitle());
        assignment.setObjectName(updatedCaseFile.getCaseNumber());
        assignment.setNewAssignee(ParticipantUtils.getAssigneeIdFromParticipants(updatedCaseFile.getParticipants()));
        assignment.setObjectType(CaseFileConstants.OBJECT_TYPE);

        return assignment;
    }

    private boolean isDueDateChanged(CaseFile caseFile, CaseFile updatedCaseFile)
    {
        return !caseFile.getDueDate().equals(updatedCaseFile.getDueDate());
    }

    protected boolean isPriorityChanged(CaseFile caseFile, CaseFile updatedCaseFile)
    {
        String updatedPriority = updatedCaseFile.getPriority();
        String priority = caseFile.getPriority();
        return !Objects.equals(updatedPriority, priority);
    }

    protected boolean isDetailsChanged(CaseFile caseFile, CaseFile updatedCaseFile)
    {
        String updatedDetails = updatedCaseFile.getDetails();
        String details = caseFile.getDetails();
        return !Objects.equals(details, updatedDetails);
    }

    public void checkPersonAssociation(CaseFile existingCaseFile, CaseFile updatedCaseFile, String ipAddress)
    {
        List<PersonAssociation> existingPersons = existingCaseFile.getPersonAssociations();
        List<PersonAssociation> updatedPersons = updatedCaseFile.getPersonAssociations();

        for (PersonAssociation person : existingPersons)
        {
            if (!updatedPersons.contains(person))
            {
                getCaseFileEventUtility().raisePersonAssociationsDeletedEvent(person,updatedCaseFile, ipAddress);
            }
        }

        for (PersonAssociation person : updatedPersons)
        {
            if (!existingPersons.contains(person))
            {
                getCaseFileEventUtility().raisePersonAssociationsAddEvent(person, updatedCaseFile, ipAddress);
            }
        }
    }

    public void checkParticipants(CaseFile caseFile, CaseFile updatedCaseFile, String ipAddress)
    {
        List<AcmParticipant> existing = caseFile.getParticipants();
        List<AcmParticipant> updated = updatedCaseFile.getParticipants();

        for (AcmParticipant participant : existing)
        {
            if (!updated.contains(participant))
            {
                // participant deleted
                getCaseFileEventUtility().raiseParticipantsModifiedInCaseFile(participant, updatedCaseFile, ipAddress, "deleted");
            }
        }

        for (AcmParticipant participant : updated)
        {
            if (!existing.contains(participant))
            {
                // participant added
                getCaseFileEventUtility().raiseParticipantsModifiedInCaseFile(participant, updatedCaseFile, ipAddress, "added");
            }
        }

        for (AcmParticipant existingParticipant : existing)
        {
            for (AcmParticipant updatedParticipant : updated)
            {
                if (existingParticipant.getId() != null && updatedParticipant.getId() != null)
                {
                    if (existingParticipant.getId().equals(updatedParticipant.getId()))
                    {
                        if (!existingParticipant.getParticipantType().equals(updatedParticipant.getParticipantType()))
                        {
                            // participant changed
                            getCaseFileEventUtility().raiseParticipantsModifiedInCaseFile(updatedParticipant, updatedCaseFile, ipAddress,
                                    "changed");
                        }
                    }
                }
            }
        }
    }

    protected boolean isStatusChanged(CaseFile caseFile, CaseFile updatedCaseFile)
    {
        String updatedStatus = updatedCaseFile.getStatus();
        String status = caseFile.getStatus();
        return !Objects.equals(updatedStatus, status);
    }

    protected boolean checkExecution(String objectType)
    {
        return objectType.equals(CaseFileConstants.OBJECT_TYPE);
    }

    private String getDateString(LocalDateTime date)
    {
        if (date != null)
        {
            DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
            return date.format(datePattern);
        }

        return "None";
    }

    public AcmObjectHistoryService getAcmObjectHistoryService()
    {

        return acmObjectHistoryService;
    }

    public void setAcmObjectHistoryService(AcmObjectHistoryService acmObjectHistoryService)
    {

        this.acmObjectHistoryService = acmObjectHistoryService;
    }

    public CaseFileEventUtility getCaseFileEventUtility()
    {

        return caseFileEventUtility;
    }

    public void setCaseFileEventUtility(CaseFileEventUtility caseFileEventUtility)
    {

        this.caseFileEventUtility = caseFileEventUtility;
    }

    public AcmObjectHistoryEventPublisher getAcmObjectHistoryEventPublisher()
    {
        return acmObjectHistoryEventPublisher;
    }

    public void setAcmObjectHistoryEventPublisher(AcmObjectHistoryEventPublisher acmObjectHistoryEventPublisher)
    {
        this.acmObjectHistoryEventPublisher = acmObjectHistoryEventPublisher;
    }

    public AcmAssignmentDao getAcmAssignmentDao()
    {
        return acmAssignmentDao;
    }

    public void setAcmAssignmentDao(AcmAssignmentDao acmAssignmentDao)
    {
        this.acmAssignmentDao = acmAssignmentDao;
    }

    public OutlookContainerCalendarService getCalendarService()
    {
        return calendarService;
    }

    public void setCalendarService(OutlookContainerCalendarService calendarService)
    {
        this.calendarService = calendarService;
    }

    public boolean isShouldDeleteCalendarFolder()
    {
        return shouldDeleteCalendarFolder;
    }

    public void setShouldDeleteCalendarFolder(boolean shouldDeleteCalendarFolder)
    {
        this.shouldDeleteCalendarFolder = shouldDeleteCalendarFolder;
    }

    public List<String> getCaseFileStatusClosed() {
        return caseFileStatusClosed;
    }

    public void setCaseFileStatusClosed(List<String> caseFileStatusClosed) {
        this.caseFileStatusClosed = caseFileStatusClosed;
    }

    public void setCaseFileStatusClosed(String caseFileStatusClosed) {
        this.caseFileStatusClosed = Arrays.asList(caseFileStatusClosed.split(","));
    }

    public OutlookCalendarAdminServiceExtension getCalendarAdminService() {
        return calendarAdminService;
    }

    /**
     * @param calendarAdminService
     *            the calendarAdminService to set
     */
    public void setCalendarAdminService(OutlookCalendarAdminServiceExtension calendarAdminService)
    {
        this.calendarAdminService = calendarAdminService;
    }

    public ObjectConverter getObjectConverter()
    {
        return objectConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter)
    {
        this.objectConverter = objectConverter;
    }

    public AcmOutlookFolderCreatorDao getFolderCreatorDao() {
        return folderCreatorDao;
    }

    /**
     * @param folderCreatorDao
     *            the folderCreatorDao to set
     */
    public void setFolderCreatorDao(AcmOutlookFolderCreatorDao folderCreatorDao)
    {
        this.folderCreatorDao = folderCreatorDao;
    }

    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }


}
