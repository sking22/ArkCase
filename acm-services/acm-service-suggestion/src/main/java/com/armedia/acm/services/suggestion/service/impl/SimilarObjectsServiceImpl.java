package com.armedia.acm.services.suggestion.service.impl;

/*-
 * #%L
 * acm-service-object-suggestion
 * %%
 * Copyright (C) 2014 - 2019 ArkObject LLC
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

import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.DESCRIPTION_NO_HTML_TAGS_PARSEABLE;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.EXT_S;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.PARENT_NUMBER_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.STATUS_LCS;
import static com.armedia.acm.services.search.model.solr.SolrAdditionalPropertiesConstants.TITLE_PARSEABLE;

import com.armedia.acm.services.search.exception.SolrException;
import com.armedia.acm.services.search.model.solr.SolrCore;
import com.armedia.acm.services.search.service.ExecuteSolrQuery;
import com.armedia.acm.services.search.service.SearchResults;
import com.armedia.acm.services.suggestion.model.CaseSuggestionsRequest;
import com.armedia.acm.services.suggestion.model.SuggestedObject;
import com.armedia.acm.services.suggestion.service.SimilarObjectsService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimilarObjectsServiceImpl implements SimilarObjectsService
{

    private static final int MAX_SIMILAR_OBJECTS = 100;

    private final Logger log = LogManager.getLogger(getClass().getName());

    private ExecuteSolrQuery executeSolrQuery;

    @Override
    public List<SuggestedObject> findSimilarObjects(String title, String objectType, Boolean isPortal, Long objectId, Authentication auth)
            throws ParseException, SolrException
    {
        List<SuggestedObject> similarObjects = new ArrayList<>();

        if (isPortal)
        {
            similarObjects.addAll(findSolrObjectsByFileContent(title, objectType, isPortal, objectId, auth));
            return similarObjects;
        }
        else
        {
            similarObjects.addAll(findSolrObjectsByTitle(title, objectType, isPortal, objectId, auth));
            similarObjects.addAll(findSolrObjectsByFileContent(title, objectType, isPortal, objectId, auth));
            return filterObjectRecordDuplicates(similarObjects);
        }
    }

    private boolean getSimilarObjectsQueryForCases(StringBuilder query, CaseSuggestionsRequest request) {
        boolean hasValues = false;
        String ssn = request.getSsn();
        String npi = request.getNpi();
        String associatedTin = request.getSanctionAssociatedTin();
        String associatedNpi = request.getSanctionAssociatedNpi();
        String convictName =  request.getConvictName();
        String convictTin = request.getConvictTin();
        String sanctionAssociateLegalBusiness = request.getSanctionAssociateLegalBusiness();
        String providerLegalBusiness = request.getProviderLegalBusiness();
        String sanctionAssociateFullName = request.getSanctionAssociateFullName();

        log.debug(String.format("Finding similar objects by ssn to [%s] and npi [%s], of type CASE_FILE", ssn, npi));
        if((ssn != null) && (!ssn.isEmpty()) && (!ssn.equalsIgnoreCase("na")) && (!ssn.equalsIgnoreCase("--")) ) {
            ssn = encodedWord(ssn, false).toString();
            query.append("(case_provider_ssn_lcs:" + ssn);
            hasValues = true;
        }
        if((npi != null) && (!npi.isEmpty()) && (!npi.equalsIgnoreCase("na") && (!npi.equalsIgnoreCase("--")))) {
            npi = encodedWord(npi, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_provider_npi_lcs:" + npi);
            hasValues = true;
        }

        if((associatedTin != null) && (!associatedTin.isEmpty()) && (!associatedTin.equalsIgnoreCase("na")) && (!associatedTin.equalsIgnoreCase("--"))) {
            associatedTin = encodedWord(associatedTin, false).toString();

            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_provider_associated_tin_lcs:" + associatedTin);
            hasValues = true;
        }

        if((associatedNpi != null) && (!associatedNpi.isEmpty()) && (!associatedNpi.equalsIgnoreCase("na")) && (!associatedNpi.equalsIgnoreCase("--"))) {
            associatedNpi = encodedWord(associatedNpi, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_provider_associated_npi_lcs:" + associatedNpi);
            hasValues = true;
        }

        // case_associate_full_name_lcs, sanctionAssociateFullName
        if((sanctionAssociateFullName != null) && (!sanctionAssociateFullName.isEmpty()) && (!sanctionAssociateFullName.equalsIgnoreCase("na")) && (!sanctionAssociateFullName.equalsIgnoreCase("--"))) {
            sanctionAssociateFullName = encodedWord(sanctionAssociateFullName, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_associate_full_name_lcs:" + sanctionAssociateFullName);
            hasValues = true;
        }

        if((sanctionAssociateLegalBusiness != null) && (!sanctionAssociateLegalBusiness.isEmpty()) && (!sanctionAssociateLegalBusiness.equalsIgnoreCase("na"))  && (!sanctionAssociateLegalBusiness.equalsIgnoreCase("--"))) {
            sanctionAssociateLegalBusiness = encodedWord(sanctionAssociateLegalBusiness, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_provider_associated_legal_business_lcs:" + sanctionAssociateLegalBusiness);
            hasValues = true;
        }

        if((providerLegalBusiness != null) && (!providerLegalBusiness.isEmpty()) && (!providerLegalBusiness.equalsIgnoreCase("na"))  && (!providerLegalBusiness.equalsIgnoreCase("--"))) {
            providerLegalBusiness = encodedWord(providerLegalBusiness, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_provider_legal_business_lcs:" +  providerLegalBusiness);
            hasValues = true;
        }

        if((convictName != null) && (!convictName.isEmpty()) && (!convictName.equalsIgnoreCase("na"))  && (!convictName.equalsIgnoreCase("--"))) {
            convictName = encodedWord(convictName, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_conv_ind_lcs:" + convictName);
            hasValues = true;
        }


        if((convictTin != null) && (!convictTin.isEmpty()) && (!convictTin.equalsIgnoreCase("na"))  && (!convictTin.equalsIgnoreCase("--"))) {
            convictTin = encodedWord(convictTin, false).toString();
            String prefix = (hasValues)?" OR ":"(";
            query.append(prefix + "case_convicted_ind_tin_lcs:" + convictTin);
            hasValues = true;
        }

        if(hasValues) {
            query.append(")");
        }
        return hasValues;
    }

    @Override
    public List<SuggestedObject> findSimilarObjects(CaseSuggestionsRequest request, Authentication auth)
            throws ParseException, SolrException {
        Boolean isPortal = false;
        Long objectId = request.getObjectId();
        String objectType = "CASE_FILE";

        List<SuggestedObject> records = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        boolean hasValues = getSimilarObjectsQueryForCases(query, request);
        if (hasValues )
        {
            query.append(" AND object_type_s:").append(objectType);
            if (isPortal)
            {
                query.append(" AND queue_name_s:").append("Release");
            }
            if (objectId != null)
            {
                query.append(" AND -object_id_s:").append(objectId);
            }

            String results = getExecuteSolrQuery().getResultsByPredefinedQuery(auth, SolrCore.ADVANCED_SEARCH, query.toString(),
                    0, MAX_SIMILAR_OBJECTS, "", true, "", false, false, "catch_all");

            SearchResults searchResults = new SearchResults();
            JSONArray docFiles = searchResults.getDocuments(results);
            for (int i = 0; i < docFiles.length(); i++)
            {
                JSONObject docFile = docFiles.getJSONObject(i);

                SuggestedObject suggestedObject = populateCaseSuggestedObject(docFile, request);

                records.add(suggestedObject);
            }
        }

        return records;
    }

    private List<SuggestedObject> findSolrObjectsByTitle(String title, String objectType, Boolean isPortal, Long objectId,
            Authentication auth)
            throws ParseException, SolrException
    {
        List<SuggestedObject> records = new ArrayList<>();

        if (StringUtils.isNotBlank(title))
        {
            log.debug(String.format("Finding similar objects by title to [%s], of type [%s]", title, objectType));

            StringBuilder query = titleToWordsQuery(title, false);

            query.append(" AND object_type_s:").append(objectType);
            if (isPortal && "CASE_FILE".equals(objectType))
            {
                query.append(" AND queue_name_s:").append("Release");
            }
            if (objectId != null)
            {
                query.append(" AND -object_id_s:").append(objectId);
            }

            String results = getExecuteSolrQuery().getResultsByPredefinedQuery(auth, SolrCore.ADVANCED_SEARCH, query.toString(),
                    0, MAX_SIMILAR_OBJECTS, "", true, "", false, false, "catch_all");

            SearchResults searchResults = new SearchResults();
            JSONArray docFiles = searchResults.getDocuments(results);

            for (int i = 0; i < docFiles.length(); i++)
            {
                JSONObject docFile = docFiles.getJSONObject(i);

                SuggestedObject suggestedObject = populateSuggestedObject(docFile);

                records.add(suggestedObject);
            }
        }

        return records;
    }

    private List<SuggestedObject> findSolrObjectsByFileContent(String title, String objectType, Boolean isPortal, Long objectId,
            Authentication auth)
            throws ParseException, SolrException
    {
        log.debug(String.format("Finding similar objects in content to [%s], of type [%s]", title, objectType));

        List<SuggestedObject> suggestedObjectList = new ArrayList<>();

        if (StringUtils.isNotBlank(title))
        {
            StringBuilder fileQuery = titleToWordsQuery(title, true);

            fileQuery.append(" AND object_type_s:FILE");
            fileQuery.append(" AND parent_ref_s:*").append(objectType);
            fileQuery.append(" AND parent_number_lcs:*");
            if (isPortal)
            {
                fileQuery.append(" AND public_flag_b:true");
            }

            String fileResults = getExecuteSolrQuery().getResultsByPredefinedQuery(auth, SolrCore.ADVANCED_SEARCH, fileQuery.toString(),
                    0, MAX_SIMILAR_OBJECTS, "", true, "", false, false, "catch_all");

            SearchResults fileSearchResults = new SearchResults();
            JSONArray fileDocFiles = fileSearchResults.getDocuments(fileResults);

            for (int i = 0; i < fileDocFiles.length(); i++)
            {
                JSONObject docFile = fileDocFiles.getJSONObject(i);

                String objectQuery = String.format("object_type_s:%s AND name:\"%s\"", objectType, docFile.getString(PARENT_NUMBER_LCS));
                if (isPortal && "CASE_FILE".equals(objectType))
                {
                    objectQuery = objectQuery.concat(" AND queue_name_s:Release");
                }

                String objectResults = getExecuteSolrQuery().getResultsByPredefinedQuery(auth, SolrCore.ADVANCED_SEARCH, objectQuery,
                        0, 1, "", true, "", false, false);

                SearchResults objectSearchResults = new SearchResults();
                JSONArray objectDocFiles = objectSearchResults.getDocuments(objectResults);

                if (objectDocFiles.length() < 1)
                {
                    continue;
                }

                JSONObject objectDocFile = objectDocFiles.getJSONObject(0);

                if (objectId != null && Long.valueOf(objectDocFile.getString("object_id_s")).equals(objectId))
                {
                    continue;
                }

                SuggestedObject suggestedObject = populateSuggestedObject(objectDocFile);
                SuggestedObject.File file = populateSuggestedObjectFile(docFile);
                suggestedObject.setFile(file);

                suggestedObjectList.add(suggestedObject);
            }
        }

        return suggestedObjectList.stream().filter(suggestedObject -> Objects.nonNull(suggestedObject.getId()))
                .collect(Collectors.toList());
    }

    private StringBuilder titleToWordsQuery(String title, boolean isContent)
    {

        StringBuilder words = new StringBuilder(Stream.of(title.trim().split(" "))
                .filter(isContent ? word -> word.length() > 2 : word -> word.length() > 0)
                .map(this::encodeWord)
                .map(word -> "\"" + word + "\"")
                .collect(Collectors.joining(" OR ")));

        if (isContent)
        {
            if (words.toString().isEmpty())
            {
                words.append("\"" + encodeWord(title) + "\"");
            }
            else
            {
                words.append(" OR \"" + encodeWord(title) + "\"");
            }
        }
        words.insert(0, "(").append(")");
        return words;
    }

    private StringBuilder encodedWord(String title, boolean isContent)
    {

        StringBuilder words = new StringBuilder(Stream.of(title)
                .filter(isContent ? word -> word.length() > 2 : word -> word.length() > 0)
                .map(this::encodeWord)
                .map(word -> "\"" + word + "\"")
                .collect(Collectors.joining("")));

        if (isContent)
        {
            if (words.toString().isEmpty())
            {
                words.append("\"" + encodeWord(title) + "\"");
            }
            else
            {
                words.append(" OR \"" + encodeWord(title) + "\"");
            }
        }
        words.insert(0, "(").append(")");
        return words;
    }

    private String encodeWord(String word)
    {
        try
        {
            return URLEncoder.encode(word, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            log.warn("Error encoding word [{}]", word);
        }
        return word;
    }

    private SuggestedObject.File populateSuggestedObjectFile(JSONObject docFile)
    {
        SuggestedObject.File file = new SuggestedObject.File();

        file.setFileId(docFile.getString("object_id_s"));
        file.setFileName(docFile.getString(TITLE_PARSEABLE) + docFile.getString(EXT_S));

        if (docFile.has("made_public_date_tdt"))
        {
            LocalDateTime date = LocalDateTime.parse(docFile.getString("made_public_date_tdt"), DateTimeFormatter.ISO_DATE_TIME);
            file.setMadePublicDate(date);
        }

        return file;
    }

    private String updateMatches(JSONObject objectDocFile, String solrKey, String requestValue, String header, String currentMatches) {
        String output = currentMatches;
        if(objectDocFile.has(solrKey)) {
            String responseValue = objectDocFile.getString(solrKey);

            if (
                (responseValue != null)
                && (!responseValue.trim().isEmpty())
                && (responseValue.equalsIgnoreCase(requestValue))
                && (!responseValue.equalsIgnoreCase("na"))
                && (!responseValue.equalsIgnoreCase("--"))
                && (!requestValue.equalsIgnoreCase("na"))
                && (!requestValue.equalsIgnoreCase("--"))
            ) {
                output += ("<br><b>"+header+"</b>: " + requestValue);
            } else if((responseValue != null) && (requestValue != null)
                    && (!requestValue.equalsIgnoreCase("na"))
                    && (!requestValue.equalsIgnoreCase("--")) ) {
                if(  responseValue.replaceAll("^0+", "").equalsIgnoreCase(requestValue.replaceAll("^0+", ""))){
                     output += ("<br><b>"+header+"</b>: " + requestValue);
                }
            }
        }
        return output;
    }

    private SuggestedObject populateCaseSuggestedObject(JSONObject objectDocFile, CaseSuggestionsRequest request) throws ParseException
    {
        String ssn = "";
        if(request.getSsn() != null){
            if(!request.getSsn().equalsIgnoreCase("--")
                    || !request.getSsn().equalsIgnoreCase("na")){
                 ssn = request.getSsn();
            }
        }

        String npi = "";
        if(request.getNpi() != null) {
            if (!request.getNpi().equalsIgnoreCase("--")
                    || !request.getNpi().equalsIgnoreCase("na")) {
                npi = request.getNpi();
            }
        }

        String associateTin = "";
        if(request.getSanctionAssociatedTin() != null) {
            if (!request.getSanctionAssociatedTin().equalsIgnoreCase("--")
                    || !request.getSanctionAssociatedTin().equalsIgnoreCase("na")) {
                associateTin = request.getSanctionAssociatedTin();
            }
        }

        String associateNpi = "";
        if(request.getSanctionAssociatedNpi() != null) {
            if (!request.getSanctionAssociatedNpi().equalsIgnoreCase("--")
                    || !request.getSanctionAssociatedNpi().equalsIgnoreCase("na")) {
                associateNpi = request.getSanctionAssociatedNpi();
            }
        }

        String convictFullName = "";
        if(request.getConvictName() != null) {
            if (!request.getConvictName().equalsIgnoreCase("--")
                    || !request.getConvictName().equalsIgnoreCase("na")) {
                convictFullName = request.getConvictName();
            }
        }

        String convictTin = "";
        if(request.getConvictTin() != null) {
            if (!request.getConvictTin().equalsIgnoreCase("--")
                    || !request.getConvictTin().equalsIgnoreCase("na")) {
                convictTin = request.getConvictTin();
            }
        }

        String sanctionAssociateLegalBusiness = "";
        if(request.getSanctionAssociateLegalBusiness() != null) {
            if (!request.getSanctionAssociateLegalBusiness().equalsIgnoreCase("--")
                    || !request.getSanctionAssociateLegalBusiness().equalsIgnoreCase("na")) {
                sanctionAssociateLegalBusiness = request.getSanctionAssociateLegalBusiness();
            }
        }

        String providerLegalBusiness = "";
        if(request.getProviderLegalBusiness() != null) {
            if (!request.getProviderLegalBusiness().equalsIgnoreCase("--")
                    || !request.getProviderLegalBusiness().equalsIgnoreCase("na")) {
                providerLegalBusiness = request.getProviderLegalBusiness();
            }
        }

        String sanctionAssociateFullName = "";
        if(request.getSanctionAssociateFullName() != null) {
            if (!request.getSanctionAssociateFullName().equalsIgnoreCase("--")
                    || !request.getSanctionAssociateFullName().equalsIgnoreCase("na")) {
                sanctionAssociateFullName = request.getSanctionAssociateFullName();
            }
        }

        SuggestedObject suggestedObject = new SuggestedObject();

        suggestedObject.setId(Long.valueOf(objectDocFile.getString("object_id_s")));
        suggestedObject.setName(objectDocFile.getString("name"));
        suggestedObject.setTitle(objectDocFile.getString("title_parseable"));
        suggestedObject.setModifiedDate(objectDocFile.getString("modified_date_tdt"));
        suggestedObject.setStatus(objectDocFile.getString("status_lcs"));
        suggestedObject.setDescription("");
        suggestedObject.setType(objectDocFile.getString("object_type_s"));

        String matches = "";
        if(objectDocFile.has("case_provider_firstname_lcs")) {
            String providerFirstName = objectDocFile.getString("case_provider_firstname_lcs");
            if ((providerFirstName != null) && (!providerFirstName.trim().isEmpty())
                    && (!providerFirstName.equalsIgnoreCase("na")) && (!providerFirstName.equalsIgnoreCase("--"))) {
                suggestedObject.setProviderFirstName(providerFirstName);
            }
        }
        if(objectDocFile.has("case_provider_lastname_lcs")) {
            String providerLastName = objectDocFile.getString("case_provider_lastname_lcs");
            if((providerLastName != null) && (!providerLastName.trim().isEmpty())
                    && (!providerLastName.equalsIgnoreCase("na")) && (!providerLastName.equalsIgnoreCase("--"))) {
                suggestedObject.setProviderLastName(providerLastName);
            }
        }



        //
        //
        if(objectDocFile.has("case_provider_legal_business_lcs")) {
            String providerLegalBusinessName = objectDocFile.getString("case_provider_legal_business_lcs");
            if((providerLegalBusinessName != null) && (!providerLegalBusinessName.trim().isEmpty())
                    && (!providerLegalBusinessName.equalsIgnoreCase("na")) && (!providerLegalBusinessName.equalsIgnoreCase("--"))) {
                suggestedObject.setProviderLegalBusinessName(providerLegalBusinessName);
            }
        }

        if(objectDocFile.has("case_provider_associated_legal_business_lcs")) {
            String associateLegalBusinessName = objectDocFile.getString("case_provider_associated_legal_business_lcs");
            if((associateLegalBusinessName != null) && (!associateLegalBusinessName.trim().isEmpty())
                    && (!associateLegalBusinessName.equalsIgnoreCase("na")) && (!associateLegalBusinessName.equalsIgnoreCase("--"))) {
                suggestedObject.setAssociatedLegalBusinessName(associateLegalBusinessName);
            }
        }

        if(objectDocFile.has("case_provider_ssn_lcs")) {
            String providerSsn = objectDocFile.getString("case_provider_ssn_lcs");
            if((providerSsn != null) && (!providerSsn.trim().isEmpty())
                    && (!providerSsn.equalsIgnoreCase("na")) && (!providerSsn.equalsIgnoreCase("--"))) {
                suggestedObject.setProviderSsn(providerSsn);
            }
        }

        if(objectDocFile.has("case_provider_npi_lcs")) {
            String providerNpi = objectDocFile.getString("case_provider_npi_lcs");
            if((providerNpi != null) && (!providerNpi.trim().isEmpty())
                    && (!providerNpi.equalsIgnoreCase("na")) && (!providerNpi.equalsIgnoreCase("--"))) {
                suggestedObject.setProviderNpi(providerNpi);
            }
        }

        if(objectDocFile.has("case_convicted_ind_tin_lcs")) {
            String convictedTin = objectDocFile.getString("case_convicted_ind_tin_lcs");
            if((convictedTin != null) && (!convictedTin.trim().isEmpty())
                    && (!convictedTin.equalsIgnoreCase("na")) && (!convictedTin.equalsIgnoreCase("--"))) {
                suggestedObject.setConvictedIndTin(convictedTin);
            }
        }

        // case_provider_associated_tin_lcs, case_provider_associated_npi_lcs
        if(objectDocFile.has("case_provider_associated_tin_lcs")) {
            String associatedTin = objectDocFile.getString("case_provider_associated_tin_lcs");
            if((associatedTin != null) && (!associatedTin.trim().isEmpty())
                    && (!associatedTin.equalsIgnoreCase("na")) && (!associatedTin.equalsIgnoreCase("--"))) {
                suggestedObject.setSanctionedTin(associatedTin);
            }
        }

        if(objectDocFile.has("case_provider_associated_npi_lcs")) {
            String associatedNpi = objectDocFile.getString("case_provider_associated_npi_lcs");
            if((associatedNpi != null) && (!associatedNpi.trim().isEmpty())
                    && (!associatedNpi.equalsIgnoreCase("na")) && (!associatedNpi.equalsIgnoreCase("--"))) {
                suggestedObject.setSanctionedNpi(associatedNpi);
            }
        }

        if(objectDocFile.has("case_conv_ind_lcs")) {
            String convictName = objectDocFile.getString("case_conv_ind_lcs");
            if((convictName != null) && (!convictName.trim().isEmpty())
                    && (!convictName.equalsIgnoreCase("na")) && (!convictName.equalsIgnoreCase("--"))) {
                suggestedObject.setConvictName(convictName);
            }
        }

        matches = this.updateMatches(objectDocFile, "case_provider_ssn_lcs", ssn, "Provider SSN", matches);
        matches = this.updateMatches(objectDocFile, "case_provider_npi_lcs", npi, "Provider NPI", matches);
        matches = this.updateMatches(objectDocFile, "case_provider_legal_business_lcs", providerLegalBusiness, "Provider Legal Business", matches);
        matches = this.updateMatches(objectDocFile, "case_conv_ind_lcs", convictFullName, "Convict Full", matches);
        matches = this.updateMatches(objectDocFile, "case_convicted_ind_tin_lcs", convictTin, "Convict Tin", matches);
        matches = this.updateMatches(objectDocFile, "case_associate_full_name_lcs", sanctionAssociateFullName, "Sanction Associate Full Name", matches);
        matches = this.updateMatches(objectDocFile, "case_provider_associated_npi_lcs", associateNpi, "Sanction Associate NPI", matches);
        matches = this.updateMatches(objectDocFile, "case_provider_associated_tin_lcs", associateTin, "Sanction Associate Tin", matches);
        matches = this.updateMatches(objectDocFile, "case_provider_associated_legal_business_lcs", sanctionAssociateLegalBusiness, "Sanction Associate Legal Business", matches);

        suggestedObject.setMatches(matches);

        if (!objectDocFile.isNull("description_no_html_tags_parseable"))
        {
            suggestedObject.setDescription(objectDocFile.getString("description_no_html_tags_parseable"));
        }
        return suggestedObject;
    }

    private SuggestedObject populateSuggestedObject(JSONObject objectDocFile) throws ParseException
    {
        SuggestedObject suggestedObject = new SuggestedObject();

        suggestedObject.setId(Long.valueOf(objectDocFile.getString("object_id_s")));
        suggestedObject.setName(objectDocFile.getString("name"));
        suggestedObject.setTitle(objectDocFile.getString(TITLE_PARSEABLE));
        suggestedObject.setModifiedDate(objectDocFile.getString("modified_date_tdt"));
        suggestedObject.setStatus(objectDocFile.getString(STATUS_LCS));
        suggestedObject.setDescription("");
        suggestedObject.setType(objectDocFile.getString("object_type_s"));

        if (!objectDocFile.isNull(DESCRIPTION_NO_HTML_TAGS_PARSEABLE))
        {
            suggestedObject.setDescription(objectDocFile.getString(DESCRIPTION_NO_HTML_TAGS_PARSEABLE));
        }
        return suggestedObject;
    }

    private List<SuggestedObject> filterObjectRecordDuplicates(List<SuggestedObject> list)
    {
        Map<Long, SuggestedObject> filter = new HashMap<>();

        list.forEach(suggestedObject -> filter.putIfAbsent(suggestedObject.id, suggestedObject));

        return new ArrayList<>(filter.values());
    }

    public ExecuteSolrQuery getExecuteSolrQuery()
    {
        return executeSolrQuery;
    }

    public void setExecuteSolrQuery(ExecuteSolrQuery executeSolrQuery)
    {
        this.executeSolrQuery = executeSolrQuery;
    }
}
