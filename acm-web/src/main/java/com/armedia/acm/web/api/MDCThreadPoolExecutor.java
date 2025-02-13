package com.armedia.acm.web.api;

/*-
 * #%L
 * ACM Shared Web Artifacts
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

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A SLF4J MDC-compatible {@link ThreadPoolExecutor}.
 * <p>
 * In general, MDC is used to store diagnostic information in per-thread variables, to facilitate logging. However,
 * although MDC data is passed to thread children, this doesn't work when threads are reused in a thread pool. This is a
 * replacement for {@link ThreadPoolTaskExecutor} that sets MDC data before each task appropriately.
 * <p>
 * Created by Bojan Milenkoski on 18.1.2016.
 */
public class MDCThreadPoolExecutor extends ThreadPoolExecutor
{

    public MDCThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler rejectedExecutionHandler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler);
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context)
    {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();

            if (context == null)
            {
                MDC.clear();
            }
            else
            {
                MDC.setContextMap(context);
            }

            try
            {
                runnable.run();
            }
            finally
            {
                if (previous == null)
                {
                    MDC.clear();
                }
                else
                {
                    MDC.setContextMap(previous);
                }
            }
        };
    }

    /**
     * All executions will have MDC injected. {@code ThreadPoolExecutor}'s submission methods ({@code submit()} etc.)
     * all delegate to this.
     */
    @Override
    public void execute(Runnable command)
    {
        super.execute(wrap(command, MDC.getCopyOfContextMap()));
    }
}
