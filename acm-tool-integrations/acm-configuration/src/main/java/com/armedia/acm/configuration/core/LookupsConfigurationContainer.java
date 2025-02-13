package com.armedia.acm.configuration.core;

/*-
 * #%L
 * ACM Tool Integrations: Configuration Library
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

import com.armedia.acm.configuration.api.ConfigurationFacade;
import com.armedia.acm.configuration.api.environment.Environment;
import com.armedia.acm.configuration.client.ConfigurationServiceBootClient;
import com.armedia.acm.configuration.model.ConfigurationClientConfig;
import com.armedia.acm.configuration.model.LookupsUpdatedEvent;
import com.armedia.acm.crypto.exceptions.AcmEncryptionException;
import com.armedia.acm.crypto.properties.AcmEncryptablePropertyUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author mario.gjurcheski
 *
 */
@Configuration
@ManagedResource(objectName = "configuration:name=lookups-service,type=com.armedia.acm.configuration.ConfigurationService,artifactId=lookups-service")
public class LookupsConfigurationContainer implements ConfigurationFacade, ApplicationEventPublisherAware
{

    private static final Logger log = LogManager.getLogger(LookupsConfigurationContainer.class);

    private volatile static LookupsConfigurationContainer INSTANCE;

    @Autowired
    private ConfigurationServiceBootClient configurationServiceBootClient;

    @Autowired
    private ConfigurationClientConfig configurationClientConfig;

    @Autowired
    private AcmEncryptablePropertyUtils encryptablePropertyUtils;

    private ApplicationEventPublisher applicationEventPublisher;

    private Map<String, Object> lookupsDefaultMap = new HashMap<>();

    private Map<String, Object> runtimeConfigurationMap = null;

    @Bean
    public static LookupsConfigurationContainer lookupsConfiguration()
    {

        if (INSTANCE == null)
        {
            initialize();
        }
        return INSTANCE;
    }

    private static synchronized void initialize()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new LookupsConfigurationContainer();
        }
    }

    private synchronized void initializeLookupsMap()
    {
        String lookupsPath = configurationClientConfig.getLookupsPath();

        List<Environment> environments = configurationServiceBootClient
                .getRemoteEnvironment(configurationServiceBootClient.configRestTemplate(), lookupsPath, null);

        Map<String, Object> configurationMap = this.configurationServiceBootClient.loadConfiguration(lookupsPath, null);
        this.runtimeConfigurationMap = this.configurationServiceBootClient.loadRuntimeConfigurationMap(environments);

        lookupsDefaultMap = configurationMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, it -> {
                    if (it.getValue() instanceof String)
                    {
                        try
                        {
                            return encryptablePropertyUtils.decryptPropertyValue(it.getValue().toString());
                        }
                        catch (AcmEncryptionException e)
                        {
                            log.error("Property [{}] can't be decrypted", it.getValue().toString());
                            throw new RuntimeCryptoException("Failed to convert property value. Reason:" + e.getMessage());
                        }
                    }
                    else
                    {
                        return it.getValue();
                    }
                }));
    }

    public Map<String, Object> getLookupsDefaultMap()
    {
        return this.lookupsDefaultMap;
    }

    public Map<String, Object> getRuntimeMap()
    {
        return this.runtimeConfigurationMap;
    }

    @Override
    public Object getProperty(String channel, String stage, String instance, String name)
    {
        return null;
    }

    @Override
    public Object getProperty(String name)
    {
        return this.lookupsDefaultMap.get(name);
    }

    @Override
    public void refresh()
    {
        initializeLookupsMap();

        LookupsUpdatedEvent event = new LookupsUpdatedEvent(getLookupsDefaultMap());
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher)
    {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
