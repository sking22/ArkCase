package com.armedia.acm.activiti;

/*-
 * #%L
 * Tool Integrations: Activiti Configuration
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

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.List;
import java.util.Map;

/**
 * Created by armdev on 5/30/14.
 */
public class AcmProcessEventHandler implements ApplicationEventPublisherAware
{
    private Logger log = LogManager.getLogger(getClass());
    private RuntimeService runtimeService;
    private ApplicationEventPublisher applicationEventPublisher;
    private HistoryService historyService;

    public void handleProcessEvent(String eventName, ProcessInstance execution)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Got an Activiti event; eventName: " + eventName + "; " + "process instance id: " +
                    execution.getProcessInstanceId());
        }

        Map<String, Object> processVariables = getRuntimeService().getVariables(execution.getId());

        String acmUser = findUserThatInitiatedThisEvent(eventName, execution, processVariables);

        AcmBusinessProcessEvent event = new AcmBusinessProcessEvent(execution);
        event.setEventType("com.armedia.acm.activiti.businessProcess." + eventName);
        event.setProcessVariables(processVariables);
        event.setEventProperties(processVariables);
        event.setUserId(acmUser);

        applicationEventPublisher.publishEvent(event);

    }

    protected String findUserThatInitiatedThisEvent(String eventName, ProcessInstance execution, Map<String, Object> processVariables)
    {
        String acmUser = "ACTIVITI_SYSTEM";
        if ("start".equals(eventName))
        {
            // when the process is started, the ACM_USER pvar is set to the user who started the process.
            if (processVariables != null && processVariables.containsKey("ACM_USER"))
            {
                acmUser = (String) processVariables.get("ACM_USER");
            }
        }
        else
        {
            acmUser = findLastUserToCompleteATask(execution.getProcessInstanceId());
        }
        return acmUser;
    }

    protected String findLastUserToCompleteATask(String processInstanceId)
    {
        List<HistoricTaskInstance> completedTasks = getHistoryService().createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .listPage(0, 1);
        String acmUser = "ACTIVITI_SYSTEM";
        if (!completedTasks.isEmpty())
        {
            HistoricTaskInstance lastCompletedTask = completedTasks.get(0);

            if (lastCompletedTask.getAssignee() != null && lastCompletedTask.getEndTime() != null)
            {
                log.debug("Found the last assignee to complete a task: " + lastCompletedTask.getAssignee() +
                        "; task end time: " + lastCompletedTask.getEndTime());
                acmUser = lastCompletedTask.getAssignee();
            }
        }
        return acmUser;
    }

    public RuntimeService getRuntimeService()
    {
        return runtimeService;
    }

    public void setRuntimeService(RuntimeService runtimeService)
    {
        this.runtimeService = runtimeService;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher)
    {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public HistoryService getHistoryService()
    {
        return historyService;
    }

    public void setHistoryService(HistoryService historyService)
    {
        this.historyService = historyService;
    }
}
