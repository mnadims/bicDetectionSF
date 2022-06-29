From a77190dbaa033ed37a882d847da99d5ba4c1571f Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Tue, 15 Nov 2011 22:50:46 +0000
Subject: [PATCH] OOZIE-480 In Oozie-site.xml, if we specify oozie.services.ext
 property is not overriding the services.

git-svn-id: https://svn.apache.org/repos/asf/incubator/oozie/trunk@1202454 13f79535-47bb-0310-9956-ffa450edef68
--
 .../main/java/org/apache/oozie/ErrorCode.java |  7 +-
 .../org/apache/oozie/service/Services.java    | 78 +++++++++++++++----
 .../apache/oozie/service/TestServices.java    | 16 +++-
 release-log.txt                               |  1 +
 4 files changed, 83 insertions(+), 19 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index b9497c660..9da371a29 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -39,6 +39,9 @@ public enum ErrorCode {
     E0026(XLog.OPS, "Missing required configuration property [{0}]"),
 
     E0100(XLog.OPS, "Could not initialize service [{0}], {1}"),
    E0101(XLog.OPS, "Service [{0}] does not implement declared interface [{1}]"),
    E0102(XLog.OPS, "Could not instanciate service class [{0}], {1}"),
    E0103(XLog.OPS, "Could not load service classes, {0}"),
     E0110(XLog.OPS, "Could not parse or validate EL definition [{0}], {1}"),
     E0111(XLog.OPS, "class#method not found [{0}#{1}]"),
     E0112(XLog.OPS, "class#method does not have PUBLIC or STATIC modifier [{0}#{1}]"),
diff --git a/core/src/main/java/org/apache/oozie/service/Services.java b/core/src/main/java/org/apache/oozie/service/Services.java
index de62b5a4a..7c627a54d 100644
-- a/core/src/main/java/org/apache/oozie/service/Services.java
++ b/core/src/main/java/org/apache/oozie/service/Services.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -199,19 +199,7 @@ public class Services {
         log.trace("Initializing");
         SERVICES = this;
         try {
            Class<? extends Service>[] serviceClasses = (Class<? extends Service>[]) conf.getClasses(
                    CONF_SERVICE_CLASSES);
            if (serviceClasses != null) {
                for (Class<? extends Service> serviceClass : serviceClasses) {
                    setService(serviceClass);
                }
            }
            serviceClasses = (Class<? extends Service>[]) conf.getClasses(CONF_SERVICE_EXT_CLASSES);
            if (serviceClasses != null) {
                for (Class<? extends Service> serviceClass : serviceClasses) {
                    setService(serviceClass);
                }
            }
            loadServices();
         }
         catch (RuntimeException ex) {
             XLog.getLog(getClass()).fatal(ex.getMessage(), ex);
@@ -234,6 +222,66 @@ public class Services {
         log.info("Oozie System ID [{0}] started!", getSystemId());
     }
 
    /**
     * Loads the specified services.
     *
     * @param classes services classes to load.
     * @param list    list of loaded service in order of appearance in the
     *                configuration.
     * @throws ServiceException thrown if a service class could not be loaded.
     */
    private void loadServices(Class[] classes, List<Service> list) throws ServiceException {
        XLog log = new XLog(LogFactory.getLog(getClass()));
        for (Class klass : classes) {
            try {
                Service service = (Service) klass.newInstance();
                log.debug("Loading service [{}] implementation [{}]", service.getInterface(),
                        service.getClass());
                if (!service.getInterface().isInstance(service)) {
                    throw new ServiceException(ErrorCode.E0101, klass, service.getInterface().getName());
                }
                list.add(service);
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ServiceException(ErrorCode.E0102, klass, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Loads services defined in <code>services</code> and
     * <code>services.ext</code> and de-dups them.
     *
     * @return List of final services to initialize.
     * @throws ServiceException throw if the services could not be loaded.
     */
    private void loadServices() throws ServiceException {
        XLog log = new XLog(LogFactory.getLog(getClass()));
        try {
            Map<Class, Service> map = new LinkedHashMap<Class, Service>();
            Class[] classes = conf.getClasses(CONF_SERVICE_CLASSES);
            Class[] classesExt = conf.getClasses(CONF_SERVICE_EXT_CLASSES);
            List<Service> list = new ArrayList<Service>();
            loadServices(classes, list);
            loadServices(classesExt, list);

            //removing duplicate services, strategy: last one wins
            for (Service service : list) {
                if (map.containsKey(service.getInterface())) {
                    log.debug("Replacing service [{}] implementation [{}]", service.getInterface(),
                            service.getClass());
                }
                map.put(service.getInterface(), service);
            }
            for (Map.Entry<Class, Service> entry : map.entrySet()) {
                setService(entry.getValue().getClass());
            }
        } catch (RuntimeException ex) {
            throw new ServiceException(ErrorCode.E0103, ex.getMessage(), ex);
        }
    }

     /**
      * Destroy all services.
      */
diff --git a/core/src/test/java/org/apache/oozie/service/TestServices.java b/core/src/test/java/org/apache/oozie/service/TestServices.java
index fbd283157..497f8b317 100644
-- a/core/src/test/java/org/apache/oozie/service/TestServices.java
++ b/core/src/test/java/org/apache/oozie/service/TestServices.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -41,9 +41,11 @@ public class TestServices extends XTestCase {
     }
 
     public static class S1 implements Service {
        public static boolean INITED_S1 = false;
 
         @Override
         public void init(Services services) throws ServiceException {
            INITED_S1 = true;
         }
 
         @Override
@@ -73,6 +75,12 @@ public class TestServices extends XTestCase {
     }
 
     public static class S1Ext extends S1 {
        public static boolean INITED_S1EXT = false;

        @Override
        public void init(Services services) throws ServiceException {
            INITED_S1EXT = true;
        }
     }
 
     private static final String SERVICES = S1.class.getName() + "," + S2.class.getName();
@@ -88,11 +96,15 @@ public class TestServices extends XTestCase {
     private static final String SERVICES_EXT = S1Ext.class.getName();
 
     public void testServicesExtLoading() throws Exception {
        S1.INITED_S1 = false;
        S1Ext.INITED_S1EXT = false;
         setSystemProperty(Services.CONF_SERVICE_CLASSES, SERVICES);
         setSystemProperty(Services.CONF_SERVICE_EXT_CLASSES, SERVICES_EXT);
         Services services = new Services();
         services.init();
         assertEquals(S1Ext.class,  services.get(S1.class).getClass());
         assertEquals(S2.class,  services.get(S2.class).getClass());
        assertFalse(S1.INITED_S1);
        assertTrue(S1Ext.INITED_S1EXT);
     }
 }
diff --git a/release-log.txt b/release-log.txt
index 1c838ee17..7124dd066 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 3.2.0 release
 
OOZIE-480 In Oozie-site.xml, if we specify oozie.services.ext property is not overriding the services.
 OOZIE-602 Update the Hadoop version to be an Apache Hadoop version.
 OOZIE-557 Simplify/normalize testing configuration when using different databases.
 OOZIE-590 Log Retrieval from multiple .gz archive files
- 
2.19.1.windows.1

