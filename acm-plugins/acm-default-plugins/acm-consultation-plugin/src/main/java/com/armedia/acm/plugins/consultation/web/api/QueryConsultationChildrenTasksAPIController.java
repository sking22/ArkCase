package com.armedia.acm.plugins.consultation.web.api;

/*-
 * #%L
 * ACM Default Plugin: Consultation
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

import com.armedia.acm.plugins.consultation.service.ConsultationTasksService;
import com.armedia.acm.services.search.exception.SolrException;
import com.armedia.acm.services.search.model.ChildDocumentSearch;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by Vladimir Cherepnalkovski <vladimir.cherepnalkovski@armedia.com> on May, 2020
 */
@Controller
@RequestMapping({ "/api/v1/plugin/consultation", "/api/latest/plugin/consultation" })
public class QueryConsultationChildrenTasksAPIController
{

    private ConsultationTasksService consultationTasksService;

    @RequestMapping(value = "/{consultationId}/tasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findConsultationChildrenTasks(
            @PathVariable("consultationId") Long consultationId,
            @RequestBody ChildDocumentSearch childDocumentSearch,
            Authentication authentication) throws SolrException
    {
        return getConsultationTasksService().getConsultationTasks(consultationId, childDocumentSearch.getParentType(),
                childDocumentSearch.getParentId(), childDocumentSearch.getChildTypes(),
                childDocumentSearch.getSort(), childDocumentSearch.getStartRow(), childDocumentSearch.getMaxRows(), authentication);
    }

    public ConsultationTasksService getConsultationTasksService()
    {
        return consultationTasksService;
    }

    public void setConsultationTasksService(ConsultationTasksService consultationTasksService)
    {
        this.consultationTasksService = consultationTasksService;
    }
}
