package com.armedia.acm.plugins.ecm.service.sync;

/*-
 * #%L
 * ACM Service: Enterprise Content Management
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.armedia.acm.plugins.ecm.model.sync.EcmEvent;
import com.armedia.acm.plugins.ecm.model.sync.EcmEventType;
import com.armedia.acm.plugins.ecm.service.sync.impl.AlfrescoNodeServiceDeleteNodeAuditResponseReader;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Created by dmiller on 5/12/17.
 */
public class AlfrescoNodeServiceDeleteNodeAuditResponseReaderTest
{

    private JSONObject alfrescoNodeServiceDeleteNodeAuditResponseJson;
    private EcmAuditResponseReader unit = new AlfrescoNodeServiceDeleteNodeAuditResponseReader();

    @Before
    public void setUp() throws Exception
    {
        final Resource alfrescoNodeServiceDeleteNodeAuditResponseResource = new ClassPathResource(
                "json/SampleAlfrescoNodeServiceDeleteNodeAuditResponse.json");
        String deleteNodesAuditResponseString = FileUtils.readFileToString(alfrescoNodeServiceDeleteNodeAuditResponseResource.getFile());
        alfrescoNodeServiceDeleteNodeAuditResponseJson = new JSONObject(deleteNodesAuditResponseString);
    }

    @Test
    public void readResponse() throws Exception
    {
        List<EcmEvent> deleteEvents = unit.read(alfrescoNodeServiceDeleteNodeAuditResponseJson);

        assertNotNull(deleteEvents);
        assertEquals(3, deleteEvents.size());

        Object[][] expectedData = {
                // event type, audit id, userid, node id
                { EcmEventType.DELETE, 44L, "admin", "workspace://SpacesStore/faf7562e-0731-4dfc-8b94-36a2f5df0f0d" },
                { EcmEventType.DELETE, 54L, "admin", "workspace://SpacesStore/b021e743-3d85-4ff9-8c0b-cc318376ee70" },
                { EcmEventType.DELETE, 62L, "admin", "workspace://SpacesStore/b2f2bde5-2499-48ac-8bab-907e6d031e58" }
        };

        int index = 0;
        for (EcmEvent ede : deleteEvents)
        {
            assertEquals("Event type #" + index, expectedData[index][0], ede.getEcmEventType());
            assertEquals("audit id #" + index, expectedData[index][1], ede.getAuditId());
            assertEquals("User #" + index, expectedData[index][2], ede.getUserId());
            assertEquals("nodeId #" + index, expectedData[index][3], ede.getNodeId());

            ++index;
        }
    }
}
