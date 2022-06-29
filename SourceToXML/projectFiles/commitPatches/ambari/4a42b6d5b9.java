From 4a42b6d5b9c69e8822e6bf35ef656e3c7f8ae0ac Mon Sep 17 00:00:00 2001
From: Andrew Onishuk <aonishuk@hortonworks.com>
Date: Mon, 8 Dec 2014 20:21:46 +0200
Subject: [PATCH] AMBARI-8561. Install Wizard: Falcon Server properties are
 empty  (aonishuk)

--
 .../ambari/server/stack/ServiceModule.java    | 15 ++++-----
 .../ambari/server/state/ServiceInfo.java      | 19 ++++++-----
 .../server/stack/ServiceModuleTest.java       | 33 +++++++++----------
 ambari-web/app/data/HDP2.2/site_properties.js | 16 +++++++++
 ambari-web/app/data/HDP2/site_properties.js   | 16 ---------
 5 files changed, 49 insertions(+), 50 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java b/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
index e95e767a11..452d3dd6b0 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
@@ -18,19 +18,18 @@
 
 package org.apache.ambari.server.stack;
 
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;

 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
 
 /**
  * Service module which provides all functionality related to parsing and fully
@@ -177,12 +176,10 @@ public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> {
 
     if (configDirectory != null) {
       for (ConfigurationModule config : configDirectory.getConfigurationModules()) {
        if (! serviceInfo.getExcludedConfigTypes().contains(config.getConfigType())) {
           ConfigurationInfo info = config.getModuleInfo();
           serviceInfo.getProperties().addAll(info.getProperties());
           serviceInfo.setTypeAttributes(config.getConfigType(), info.getAttributes());
           configurationModules.put(config.getConfigType(), config);
        }
       }
 
       for (String excludedType : serviceInfo.getExcludedConfigTypes()) {
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceInfo.java b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceInfo.java
index 9277ec6690..5224aaa605 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceInfo.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/state/ServiceInfo.java
@@ -286,9 +286,14 @@ public class ServiceInfo {
    * @return unmodifiable map of config types associated with this service
    */
   public synchronized Map<String, Map<String, Map<String, String>>> getConfigTypeAttributes() {
    return configTypes == null ?
        Collections.<String, Map<String, Map<String, String>>>emptyMap() :
        Collections.unmodifiableMap(configTypes);
    Map<String, Map<String, Map<String, String>>> tmpConfigTypes = configTypes == null ?
        new HashMap<String, Map<String, Map<String, String>>>() : configTypes;

    for(String excludedtype : excludedConfigTypes){
      tmpConfigTypes.remove(excludedtype);
    }

    return Collections.unmodifiableMap(tmpConfigTypes);
   }
 
   /**
@@ -302,10 +307,7 @@ public class ServiceInfo {
     if (this.configTypes == null) {
       configTypes = new HashMap<String, Map<String, Map<String, String>>>();
     }

    if (! excludedConfigTypes.contains(type)) {
      configTypes.put(type, typeAttributes);
    }
    configTypes.put(type, typeAttributes);
   }
 
   /**
@@ -336,7 +338,8 @@ public class ServiceInfo {
    * @return true if the service has the specified config type; false otherwise
    */
   public boolean hasConfigType(String type) {
    return configTypes != null && configTypes.containsKey(type);
    return configTypes != null && configTypes.containsKey(type)
        && !excludedConfigTypes.contains(type);
   }
 
   /**
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/stack/ServiceModuleTest.java b/ambari-server/src/test/java/org/apache/ambari/server/stack/ServiceModuleTest.java
index 529def505c..5262c7719d 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/stack/ServiceModuleTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/stack/ServiceModuleTest.java
@@ -18,14 +18,13 @@
 
 package org.apache.ambari.server.stack;
 
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.junit.Test;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
 
 import java.io.File;
 import java.lang.reflect.Field;
@@ -35,14 +34,14 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.junit.Test;
 
 /**
  * ServiceModule unit tests.
@@ -775,7 +774,7 @@ public class ServiceModuleTest {
     ServiceModule service = createServiceModule(info, configModules);
 
     List<PropertyInfo> properties = service.getModuleInfo().getProperties();
    assertEquals(3, properties.size());
    assertEquals(4, properties.size());
 
     Map<String, Map<String, Map<String, String>>> attributes = service.getModuleInfo().getConfigTypeAttributes();
     assertEquals(2, attributes.size());
diff --git a/ambari-web/app/data/HDP2.2/site_properties.js b/ambari-web/app/data/HDP2.2/site_properties.js
index f5b001c1a5..589057f514 100644
-- a/ambari-web/app/data/HDP2.2/site_properties.js
++ b/ambari-web/app/data/HDP2.2/site_properties.js
@@ -87,6 +87,22 @@ hdp22properties.push(
     "serviceName": "HDFS",
     "filename": "hdfs-site.xml",
     "index": 1
  },
  {
    "id": "site property",
    "name": "*.falcon.graph.blueprints.graph",
    "displayName": "*.falcon.graph.blueprints.graph",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "id": "site property",
    "name": "*.falcon.graph.storage.backend",
    "displayName": "*.falcon.graph.storage.backend",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
   });
 
 var additionalProperties = [];
diff --git a/ambari-web/app/data/HDP2/site_properties.js b/ambari-web/app/data/HDP2/site_properties.js
index 3cf97ae184..49be514329 100644
-- a/ambari-web/app/data/HDP2/site_properties.js
++ b/ambari-web/app/data/HDP2/site_properties.js
@@ -1915,14 +1915,6 @@ module.exports =
     },
 
     // Falcon Graph and Storage
    {
      "id": "site property",
      "name": "*.falcon.graph.blueprints.graph",
      "displayName": "*.falcon.graph.blueprints.graph",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
     {
       "id": "site property",
       "name": "*.falcon.graph.storage.directory",
@@ -1933,14 +1925,6 @@ module.exports =
       "serviceName": "FALCON",
       "filename": "falcon-startup.properties.xml"
     },
    {
      "id": "site property",
      "name": "*.falcon.graph.storage.backend",
      "displayName": "*.falcon.graph.storage.backend",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
     {
       "id": "site property",
       "name": "*.falcon.graph.serialize.path",
- 
2.19.1.windows.1

