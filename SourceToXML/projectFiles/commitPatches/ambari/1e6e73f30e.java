From 1e6e73f30e32e7e54519db4cb5ed034b984fd570 Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Wed, 26 Jul 2017 16:29:47 -0400
Subject: [PATCH] AMBARI-21582 - Stack Tools and Feature Should be Ignored in
 Blueprints (jonathanhurley)

--
 .../controller/internal/BlueprintConfigurationProcessor.java  | 2 +-
 .../internal/BlueprintConfigurationProcessorTest.java         | 4 ++++
 2 files changed, 5 insertions(+), 1 deletion(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
index 1daf76f10a..144e2e7f8d 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessor.java
@@ -2954,7 +2954,7 @@ public class BlueprintConfigurationProcessor {
       Map<String,String> clusterEnvDefaultProperties = defaultStackProperties.get(CLUSTER_ENV_CONFIG_TYPE_NAME);
 
       for( String property : properties ){
        if (defaultStackProperties.containsKey(property)) {
        if (clusterEnvDefaultProperties.containsKey(property)) {
           configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, property,
               clusterEnvDefaultProperties.get(property));
 
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
index bade23810e..8a5513675b 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/controller/internal/BlueprintConfigurationProcessorTest.java
@@ -99,6 +99,7 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
   private static final Configuration EMPTY_CONFIG = new Configuration(Collections.<String, Map<String, String>>emptyMap(), Collections.<String, Map<String, Map<String, String>>>emptyMap());
   private final Map<String, Collection<String>> serviceComponents = new HashMap<>();
   private final Map<String, Map<String, String>> stackProperties = new HashMap<>();
  private final Map<String, String> defaultClusterEnvProperties = new HashMap<>();
 
   private final String STACK_NAME = "testStack";
   private final String STACK_VERSION = "1";
@@ -239,6 +240,9 @@ public class BlueprintConfigurationProcessorTest extends EasyMockSupport {
     expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
     expect(configHelper.getDefaultStackProperties(
         EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();
  
   stackProperties.put(ConfigHelper.CLUSTER_ENV, defaultClusterEnvProperties);

 
     expect(ambariContext.isClusterKerberosEnabled(1)).andReturn(true).once();
     expect(ambariContext.getClusterName(1L)).andReturn("clusterName").anyTimes();
- 
2.19.1.windows.1

