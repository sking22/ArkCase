package gov.foia.dao;

/*-
 * #%L
 * ACM Standard Application: Freedom of Information Act
 * %%
 * Copyright (C) 2014 - 2020 ArkCase LLC
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

import com.armedia.acm.data.AcmAbstractDao;
import com.armedia.acm.plugins.casefile.dao.CaseFileDao;
import com.armedia.acm.plugins.ecm.model.EcmFile;
import com.armedia.acm.plugins.ecm.service.AcmFolderService;
import gov.foia.model.ExemptionStatute;
import gov.foia.model.FOIARequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FOIAExemptionStatuteDao extends AcmAbstractDao<ExemptionStatute>
{

    private AcmFolderService acmFolderService;
    private CaseFileDao caseFileDao;

    @Override
    protected Class<ExemptionStatute> getPersistenceClass()
    {
        return ExemptionStatute.class;
    }

    public List<ExemptionStatute> getExemptionStatutesForFilesInResponseFolder(Long parentObjectId, String parentObjectType) {

        FOIARequest request = (FOIARequest) caseFileDao.find(parentObjectId);
        List<ExemptionStatute> listStatutesOnDocuments = new ArrayList<>();

        if (request.getQueue().getName().equals("Release")
                || (request.getGeneratedZipFlag() != null && request.getGeneratedZipFlag() == true)
                || request.getDispositionClosedDate() != null) {

            String queryText = "SELECT af.id " +
                    "FROM AcmContainer ac " +
                    "JOIN AcmFolder af ON af.parentFolder.id = ac.folder.id " +
                    "WHERE ac.containerObjectId = :parentObjectId " +
                    "AND ac.containerObjectType = :parentObjectType " +
                    "AND af.name = '03 Response'";

            Query query = getEm().createQuery(queryText);
            query.setParameter("parentObjectId", parentObjectId);
            query.setParameter("parentObjectType", parentObjectType);

            Long result = (Long) query.getSingleResult();
            List<EcmFile> files = getAcmFolderService().getFilesInFolderAndSubfolders(result);

            if (files != null) {
                List<Long> fileIds = files.stream()
                        .map(EcmFile::getFileId)
                        .collect(Collectors.toList());

                for (Long fileId : fileIds) {
                    listStatutesOnDocuments.addAll(findExemptionStatutesByFileId(fileId));
                }
            }
        }
        return listStatutesOnDocuments;

    }

    public List<ExemptionStatute> getExemptionStatutesOnRequestLevel(Long parentObjectId)
    {
        String queryText = "SELECT exemptionStatute " +
                "FROM ExemptionStatute exemptionStatute " +
                "JOIN FOIARequest request ON request.id = exemptionStatute.parentObjectId " +
                "WHERE exemptionStatute.parentObjectId = :parentObjectId " +
                "ORDER BY exemptionStatute.exemptionStatute";

        TypedQuery<ExemptionStatute> exemptionStatuteTypedQuery = getEm().createQuery(queryText,
                ExemptionStatute.class);

        exemptionStatuteTypedQuery.setParameter("parentObjectId", parentObjectId);
        List<ExemptionStatute> exemptionStatuteList = exemptionStatuteTypedQuery.getResultList();
        if (null == exemptionStatuteList)
        {
            exemptionStatuteList = new ArrayList<>();
        }
        return exemptionStatuteList;

    }

    public List<ExemptionStatute> findExemptionStatutesByFileId(Long fileId)
    {
        String queryText = "SELECT statute FROM ExemptionStatute statute WHERE statute.fileId = :fileId";
        TypedQuery<ExemptionStatute> query = getEm().createQuery(queryText, ExemptionStatute.class);
        query.setParameter("fileId", fileId);

        List<ExemptionStatute> exemptionStatuteList = query.getResultList();
        if (exemptionStatuteList == null)
        {
            exemptionStatuteList = new ArrayList<>();
        }
        return exemptionStatuteList;
    }

    @Transactional
    public void deleteExemptionStatute(Long id)
    {
        ExemptionStatute exemptionStatute = getEm().find(ExemptionStatute.class, id);
        if (exemptionStatute != null)
        {
            getEm().remove(exemptionStatute);
        }
    }

    public AcmFolderService getAcmFolderService() {
        return acmFolderService;
    }

    public void setAcmFolderService(AcmFolderService acmFolderService) {
        this.acmFolderService = acmFolderService;
    }

    public CaseFileDao getCaseFileDao() {
        return caseFileDao;
    }

    public void setCaseFileDao(CaseFileDao caseFileDao) {
        this.caseFileDao = caseFileDao;
    }
}
