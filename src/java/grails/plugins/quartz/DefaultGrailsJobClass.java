/*
 * Copyright (c) 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.quartz;

import grails.plugins.quartz.config.TriggersConfigBuilder;
import grails.util.GrailsUtil;
import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.quartz.JobExecutionContext;

import java.util.HashMap;
import java.util.Map;

import static grails.plugins.quartz.GrailsJobClassConstants.*;


/**
 * Grails artifact class which represents a Quartz job.
 *
 * @author Micha?? K??ujszo
 * @author Marcel Overdijk
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 * @since 0.1
 */
public class DefaultGrailsJobClass extends AbstractInjectableGrailsClass implements GrailsJobClass {

    public static final String JOB = "Job";
    private Map triggers = new HashMap();


    public DefaultGrailsJobClass(Class clazz) {
        super(clazz, JOB);
        evaluateTriggers();
    }

    private void evaluateTriggers() {
        // registering additional triggersClosure from 'triggersClosure' closure if present
        Closure triggersClosure = (Closure) GrailsClassUtils.getStaticPropertyValue(getClazz(), "triggers");

        TriggersConfigBuilder builder = new TriggersConfigBuilder(getFullName());

        if (triggersClosure != null) {
            builder.build(triggersClosure);
            triggers = (Map) builder.getTriggers();
        } else {
            // backward compatibility
            if (isCronExpressionConfigured()) {
                GrailsUtil.deprecated("You're using deprecated 'def cronExpression = ...' parameter in the " + getFullName() + ", use 'static triggers = { cron cronExpression: ...} instead.");
                triggers = builder.createEmbeddedCronTrigger(getStartDelay(), getCronExpression());
            } else {
                GrailsUtil.deprecated("You're using deprecated 'def startDelay = ...; def timeout = ...' parameters in the" + getFullName() + ", use 'static triggers = { simple startDelay: ..., repeatInterval: ...} instead.");
                triggers = builder.createEmbeddedSimpleTrigger(getStartDelay(), getTimeout(), getRepeatCount());
            }
        }
    }

    public void execute() {
        getMetaClass().invokeMethod(getReferenceInstance(), EXECUTE, new Object[]{});
    }

    public void execute(JobExecutionContext context) {
        getMetaClass().invokeMethod(getReferenceInstance(), EXECUTE, new Object[]{context});
    }

    // TODO: ============== start of deprecated methods =================
    @Deprecated
    public long getTimeout() {
        Object obj = getPropertyValue(TIMEOUT);
        if (obj == null) return DEFAULT_TIMEOUT;
        else if (!(obj instanceof Number)) {
            throw new IllegalArgumentException(
                    "timeout trigger property in the job class " +
                            getShortName() + " must be Integer or Long");
        }

        return ((Number) obj).longValue();
    }

    @Deprecated
    public long getStartDelay() {
        Object obj = getPropertyValue(START_DELAY);
        if (obj == null) return DEFAULT_START_DELAY;
        else if (!(obj instanceof Number)) {
            throw new IllegalArgumentException(
                    "startDelay trigger property in the job class " +
                            getShortName() + " must be Integer or Long");
        }

        return ((Number) obj).longValue();
    }

    @Deprecated
    public int getRepeatCount() {
        Object obj = getPropertyValue(REPEAT_COUNT);
        if (obj == null) return DEFAULT_REPEAT_COUNT;
        else if (!(obj instanceof Number)) {
            throw new IllegalArgumentException(
                    "repeatCount trigger property in the job class " +
                            getShortName() + " must be Integer or Long");
        }

        return ((Number) obj).intValue();
    }

    @Deprecated
    public String getCronExpression() {
        String cronExpression = (String) getPropertyOrStaticPropertyOrFieldValue(CRON_EXPRESSION, String.class);
        if (cronExpression == null || "".equals(cronExpression)) return DEFAULT_CRON_EXPRESSION;
        return cronExpression;
    }

    @Deprecated
    public String getGroup() {
        String group = (String) getPropertyOrStaticPropertyOrFieldValue(GROUP, String.class);
        if (group == null || "".equals(group)) return DEFAULT_GROUP;
        return group;
    }

    // not certain about this... feels messy
    @Deprecated
    public boolean isCronExpressionConfigured() {
        String cronExpression = (String) getPropertyOrStaticPropertyOrFieldValue(CRON_EXPRESSION, String.class);
        return cronExpression != null;
    }
    // TODO: ============== end of deprecated methods =================

    public boolean isConcurrent() {
        Boolean concurrent = getPropertyValue(CONCURRENT, Boolean.class);
        return concurrent == null ? DEFAULT_CONCURRENT : concurrent;
    }

    public boolean isSessionRequired() {
        Boolean sessionRequired = getPropertyValue(SESSION_REQUIRED, Boolean.class);
        return sessionRequired == null ? DEFAULT_SESSION_REQUIRED : sessionRequired;
    }

    public boolean isDurability() {
        Boolean durability = getPropertyValue(DURABILITY, Boolean.class);
        return durability == null ? DEFAULT_DURABILITY : durability;
    }

    public boolean isRequestsRecovery() {
        Boolean requestsRecovery = getPropertyValue(REQUESTS_RECOVERY, Boolean.class);
        return requestsRecovery == null ? DEFAULT_REQUESTS_RECOVERY : requestsRecovery;
    }

    public String getDescription() {
        String description = (String) getPropertyOrStaticPropertyOrFieldValue(DESCRIPTION, String.class);
        if (description == null || "".equals(description)) return DEFAULT_DESCRIPTION;
        return description;
    }

    public Map getTriggers() {
        return triggers;
    }
}
