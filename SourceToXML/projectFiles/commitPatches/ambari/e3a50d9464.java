From e3a50d9464479d59b5899eef6c7e0c80a2785bce Mon Sep 17 00:00:00 2001
From: Jonathan Hurley <jhurley@hortonworks.com>
Date: Thu, 27 Jul 2017 10:15:46 -0400
Subject: [PATCH] AMBARI-21582 - Stack Tools and Feature Should be Ignored in
 Blueprints (part2) (jonathanhurley)

--
 .../server/topology/ClusterConfigurationRequestTest.java   | 7 +++++++
 1 file changed, 7 insertions(+)

diff --git a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
index 5535256f13..226ceddd48 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/topology/ClusterConfigurationRequestTest.java
@@ -58,6 +58,7 @@ import org.easymock.EasyMock;
 import org.easymock.EasyMockRule;
 import org.easymock.Mock;
 import org.easymock.MockType;
import org.junit.Before;
 import org.junit.Rule;
 import org.junit.Test;
 import org.junit.runner.RunWith;
@@ -111,6 +112,12 @@ public class ClusterConfigurationRequestTest {
   private final String STACK_NAME = "testStack";
   private final String STACK_VERSION = "1";
   private final Map<String, Map<String, String>> stackProperties = new HashMap<>();
  private final Map<String, String> defaultClusterEnvProperties = new HashMap<>();

  @Before
  public void setup() {
    stackProperties.put(ConfigHelper.CLUSTER_ENV, defaultClusterEnvProperties);
  }
 
   /**
    * testConfigType config type should be in updatedConfigTypes, as no custom property in Blueprint
- 
2.19.1.windows.1

