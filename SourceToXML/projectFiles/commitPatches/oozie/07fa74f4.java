From 07fa74f450a79ccb6385c7c5389c9f56d7136854 Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 7 Jan 2015 15:07:12 -0800
Subject: [PATCH] OOZIE-2104 oozie server dies on startup if oozie-site
 redefines ActionExecutor classes (rkanter)

--
 .../main/java/org/apache/oozie/ErrorCode.java |  1 -
 .../apache/oozie/service/ActionService.java   | 64 ++++++++++++++-----
 .../java/org/apache/oozie/TestDagEngine.java  |  2 +-
 .../oozie/command/wf/TestActionErrors.java    |  2 +-
 .../apache/oozie/service/DummyExecutor1.java  | 50 +++++++++++++++
 .../apache/oozie/service/DummyExecutor2.java  | 50 +++++++++++++++
 .../service/TestActionCheckerService.java     |  2 +-
 .../oozie/service/TestActionService.java      | 51 ++++++++++++++-
 .../service/TestAuthorizationService.java     |  2 +-
 .../oozie/service/TestPurgeService.java       |  2 +-
 .../oozie/service/TestRecoveryService.java    |  2 +-
 release-log.txt                               |  1 +
 12 files changed, 205 insertions(+), 24 deletions(-)
 create mode 100644 core/src/test/java/org/apache/oozie/service/DummyExecutor1.java
 create mode 100644 core/src/test/java/org/apache/oozie/service/DummyExecutor2.java

diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index 28b90c045..4444c87e5 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -56,7 +56,6 @@ public enum ErrorCode {
     E0131(XLog.OPS, "Could not read workflow schemas file/s, {0}"),
     E0140(XLog.OPS, "Could not access database, {0}"),
     E0141(XLog.OPS, "Could not create DataSource connection pool, {0}"),
    E0150(XLog.OPS, "Actionexecutor type already registered [{0}]"),
     E0160(XLog.OPS, "Could not read admin users file [{0}], {1}"),
 
     E0300(XLog.STD, "Invalid content-type [{0}]"),
diff --git a/core/src/main/java/org/apache/oozie/service/ActionService.java b/core/src/main/java/org/apache/oozie/service/ActionService.java
index c8ed26523..becc69b25 100644
-- a/core/src/main/java/org/apache/oozie/service/ActionService.java
++ b/core/src/main/java/org/apache/oozie/service/ActionService.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.service;
 
import com.google.common.annotations.VisibleForTesting;
 import org.apache.hadoop.util.ReflectionUtils;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.control.EndActionExecutor;
@@ -25,17 +26,15 @@ import org.apache.oozie.action.control.ForkActionExecutor;
 import org.apache.oozie.action.control.JoinActionExecutor;
 import org.apache.oozie.action.control.KillActionExecutor;
 import org.apache.oozie.action.control.StartActionExecutor;
import org.apache.oozie.service.Service;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
import org.apache.oozie.ErrorCode;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;
import org.apache.oozie.util.Instrumentable;
import org.apache.oozie.util.Instrumentation;
 
public class ActionService implements Service {
public class ActionService implements Service, Instrumentable {
 
     public static final String CONF_ACTION_EXECUTOR_CLASSES = CONF_PREFIX + "ActionService.executor.classes";
 
@@ -45,7 +44,8 @@ public class ActionService implements Service {
     private Map<String, Class<? extends ActionExecutor>> executors;
     private static XLog LOG = XLog.getLog(ActionService.class);
 
    @SuppressWarnings("unchecked")
    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
     public void init(Services services) throws ServiceException {
         this.services = services;
         ActionExecutor.enableInit();
@@ -64,16 +64,28 @@ public class ActionService implements Service {
         classes = (Class<? extends ActionExecutor>[]) ConfigurationService.getClasses
                 (services.getConf(), CONF_ACTION_EXECUTOR_EXT_CLASSES);
         registerExecutors(classes);

        initExecutors();
     }
 
    private void registerExecutors(Class<? extends ActionExecutor>[] classes) throws ServiceException {
    private void registerExecutors(Class<? extends ActionExecutor>[] classes) {
         if (classes != null) {
             for (Class<? extends ActionExecutor> executorClass : classes) {
                register(executorClass);
                @SuppressWarnings("deprecation")
                ActionExecutor executor = (ActionExecutor) ReflectionUtils.newInstance(executorClass, services.getConf());
                executors.put(executor.getType(), executorClass);
             }
         }
     }
 
    private void initExecutors() {
        for (Class<? extends ActionExecutor> executorClass : executors.values()) {
            initExecutor(executorClass);
        }
        LOG.info("Initialized action types: " + getActionTypes());
    }

    @Override
     public void destroy() {
         ActionExecutor.enableInit();
         ActionExecutor.resetInitInfo();
@@ -81,21 +93,43 @@ public class ActionService implements Service {
         executors = null;
     }
 
    @Override
     public Class<? extends Service> getInterface() {
         return ActionService.class;
     }
 
    public void register(Class<? extends ActionExecutor> klass) throws ServiceException {
    @Override
    public void instrument(Instrumentation instr) {
        instr.addVariable("configuration", "action.types", new Instrumentation.Variable<String>() {
            @Override
            public String getValue() {
                Set<String> actionTypes = getActionTypes();
                if (actionTypes != null) {
                    return actionTypes.toString();
                }
                return "(unavailable)";
            }
        });
    }

    @SuppressWarnings("unchecked")
    @VisibleForTesting
    public void registerAndInitExecutor(Class<? extends ActionExecutor> klass) {
        ActionExecutor.enableInit();
        ActionExecutor.resetInitInfo();
        ActionExecutor.disableInit();
        registerExecutors(new Class[]{klass});
        initExecutors();
    }

    private void initExecutor(Class<? extends ActionExecutor> klass) {
        @SuppressWarnings("deprecation")
         ActionExecutor executor = (ActionExecutor) ReflectionUtils.newInstance(klass, services.getConf());
        LOG.trace("Registering action type [{0}] class [{1}]", executor.getType(), klass);
        if (executors.containsKey(executor.getType())) {
            throw new ServiceException(ErrorCode.E0150, executor.getType());
        }
        LOG.debug("Initializing action type [{0}] class [{1}]", executor.getType(), klass);
         ActionExecutor.enableInit();
         executor.initActionType();
         ActionExecutor.disableInit();
        executors.put(executor.getType(), klass);
        LOG.trace("Registered Action executor for action type [{0}] class [{1}]", executor.getType(), klass);
        LOG.trace("Initialized Executor for action type [{0}] class [{1}]", executor.getType(), klass);
     }
 
     public ActionExecutor getExecutor(String actionType) {
diff --git a/core/src/test/java/org/apache/oozie/TestDagEngine.java b/core/src/test/java/org/apache/oozie/TestDagEngine.java
index 824fdf231..15f86403f 100644
-- a/core/src/test/java/org/apache/oozie/TestDagEngine.java
++ b/core/src/test/java/org/apache/oozie/TestDagEngine.java
@@ -77,7 +77,7 @@ public class TestDagEngine extends XTestCase {
         setSystemProperty(SchemaService.WF_CONF_EXT_SCHEMAS, "wf-ext-schema.xsd");
         services = new Services();
         services.init();
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
     }
 
     protected void tearDown() throws Exception {
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestActionErrors.java b/core/src/test/java/org/apache/oozie/command/wf/TestActionErrors.java
index b88dd3940..8eb98af87 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestActionErrors.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestActionErrors.java
@@ -65,7 +65,7 @@ public class TestActionErrors extends XDataTestCase {
         setSystemProperty(LiteWorkflowStoreService.CONF_USER_RETRY_ERROR_CODE_EXT, ForTestingActionExecutor.TEST_ERROR);
         services = new Services();
         services.init();
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
     }
 
     @Override
diff --git a/core/src/test/java/org/apache/oozie/service/DummyExecutor1.java b/core/src/test/java/org/apache/oozie/service/DummyExecutor1.java
new file mode 100644
index 000000000..ce34e84a2
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/service/DummyExecutor1.java
@@ -0,0 +1,50 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.service;

import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.client.WorkflowAction;

public class DummyExecutor1 extends ActionExecutor {
    public DummyExecutor1() {
        super(TestActionService.TEST_ACTION_TYPE);
    }

    @Override
    public void start(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void end(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void check(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void kill(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public boolean isCompleted(String externalStatus) {
        return true;
    }
}
diff --git a/core/src/test/java/org/apache/oozie/service/DummyExecutor2.java b/core/src/test/java/org/apache/oozie/service/DummyExecutor2.java
new file mode 100644
index 000000000..3ff83761b
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/service/DummyExecutor2.java
@@ -0,0 +1,50 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.service;

import org.apache.oozie.action.ActionExecutor;
import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.client.WorkflowAction;

public class DummyExecutor2 extends ActionExecutor {
    public DummyExecutor2() {
        super(TestActionService.TEST_ACTION_TYPE);
    }

    @Override
    public void start(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void end(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void check(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public void kill(Context context, WorkflowAction action) throws ActionExecutorException {
    }

    @Override
    public boolean isCompleted(String externalStatus) {
        return true;
    }
}
diff --git a/core/src/test/java/org/apache/oozie/service/TestActionCheckerService.java b/core/src/test/java/org/apache/oozie/service/TestActionCheckerService.java
index e12920132..c465c7fa1 100644
-- a/core/src/test/java/org/apache/oozie/service/TestActionCheckerService.java
++ b/core/src/test/java/org/apache/oozie/service/TestActionCheckerService.java
@@ -68,7 +68,7 @@ public class TestActionCheckerService extends XDataTestCase {
         services = new Services();
         setClassesToBeExcluded(services.getConf(), excludedServices);
         services.init();
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
     }
 
     @Override
diff --git a/core/src/test/java/org/apache/oozie/service/TestActionService.java b/core/src/test/java/org/apache/oozie/service/TestActionService.java
index 2f3299372..5b5ac937c 100644
-- a/core/src/test/java/org/apache/oozie/service/TestActionService.java
++ b/core/src/test/java/org/apache/oozie/service/TestActionService.java
@@ -18,12 +18,13 @@
 
 package org.apache.oozie.service;
 
import org.apache.oozie.service.Services;
import org.apache.oozie.service.ActionService;
import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.test.XTestCase;
 
 public class TestActionService extends XTestCase {
 
    static final String TEST_ACTION_TYPE = "TestActionType";

     @Override
     protected void setUp() throws Exception {
         super.setUp();
@@ -45,4 +46,50 @@ public class TestActionService extends XTestCase {
         assertNotNull(as.getExecutor("switch"));
     }
 
    @SuppressWarnings("deprecation")
    public void testDuplicateActionExecutors() throws Exception {
        ActionService as = new ActionService();
        Services.get().getConf().set("oozie.service.ActionService.executor.classes",
                DummyExecutor1.class.getName() + "," + DummyExecutor2.class.getName());
        Services.get().getConf().set("oozie.service.ActionService.executor.ext.classes", "");
        try {
            as.init(Services.get());
            // There are 5 hard-coded control action types + 1 TEST_ACTION_TYPE
            assertEquals(6, as.getActionTypes().size());
            ActionExecutor executor = as.getExecutor(TEST_ACTION_TYPE);
            assertTrue(executor instanceof DummyExecutor2);
            assertFalse(executor instanceof DummyExecutor1);
        } finally {
            as.destroy();
        }

        as = new ActionService();
        Services.get().getConf().set("oozie.service.ActionService.executor.classes", DummyExecutor1.class.getName());
        Services.get().getConf().set("oozie.service.ActionService.executor.ext.classes", DummyExecutor2.class.getName());
        try {
            as.init(Services.get());
            // There are 5 hard-coded control action types + 1 TEST_ACTION_TYPE
            assertEquals(6, as.getActionTypes().size());
            ActionExecutor executor = as.getExecutor(TEST_ACTION_TYPE);
            assertTrue(executor instanceof DummyExecutor2);
            assertFalse(executor instanceof DummyExecutor1);
        } finally {
            as.destroy();
        }

        as = new ActionService();
        Services.get().getConf().set("oozie.service.ActionService.executor.classes", "");
        Services.get().getConf().set("oozie.service.ActionService.executor.ext.classes",
                DummyExecutor1.class.getName() + "," + DummyExecutor2.class.getName());
        try {
            as.init(Services.get());
            // There are 5 hard-coded control action types + 1 TEST_ACTION_TYPE
            assertEquals(6, as.getActionTypes().size());
            ActionExecutor executor = as.getExecutor(TEST_ACTION_TYPE);
            assertTrue(executor instanceof DummyExecutor2);
            assertFalse(executor instanceof DummyExecutor1);
        } finally {
            as.destroy();
        }
    }
 }
diff --git a/core/src/test/java/org/apache/oozie/service/TestAuthorizationService.java b/core/src/test/java/org/apache/oozie/service/TestAuthorizationService.java
index 86eabbfbc..ce461c1f9 100644
-- a/core/src/test/java/org/apache/oozie/service/TestAuthorizationService.java
++ b/core/src/test/java/org/apache/oozie/service/TestAuthorizationService.java
@@ -95,7 +95,7 @@ public class TestAuthorizationService extends XDataTestCase {
         services.init();
         services.getConf().setBoolean(AuthorizationService.CONF_SECURITY_ENABLED, true);
         services.get(AuthorizationService.class).init(services);
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
     }
 
     @Override
diff --git a/core/src/test/java/org/apache/oozie/service/TestPurgeService.java b/core/src/test/java/org/apache/oozie/service/TestPurgeService.java
index ba20c662c..74d34ccff 100644
-- a/core/src/test/java/org/apache/oozie/service/TestPurgeService.java
++ b/core/src/test/java/org/apache/oozie/service/TestPurgeService.java
@@ -74,7 +74,7 @@ public class TestPurgeService extends XDataTestCase {
         services = new Services();
         setClassesToBeExcluded(services.getConf(), excludedServices);
         services.init();
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
     }
 
     @Override
diff --git a/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java b/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
index 7a39e27ee..62d14a0ff 100644
-- a/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
++ b/core/src/test/java/org/apache/oozie/service/TestRecoveryService.java
@@ -95,7 +95,7 @@ public class TestRecoveryService extends XDataTestCase {
         setSystemProperty(SchemaService.WF_CONF_EXT_SCHEMAS, "wf-ext-schema.xsd");
         services = new Services();
         services.init();
        services.get(ActionService.class).register(ForTestingActionExecutor.class);
        services.get(ActionService.class).registerAndInitExecutor(ForTestingActionExecutor.class);
 
     }
 
diff --git a/release-log.txt b/release-log.txt
index 26a02bdbf..006ac7640 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-2104 oozie server dies on startup if oozie-site redefines ActionExecutor classes (rkanter)
 OOZIE-2092 Provide option to supply config to workflow during rerun of coordinator (jaydeepvishwakarma via shwethags)
 OOZIE-2100 Publish oozie-webapp artifact (sureshms via bzhang)
 OOZIE-1889 Convert NamedNativeQueries to JPQL (dvillegas via shwethags)
- 
2.19.1.windows.1

