From d1aaecba9127428cbde324aec20c7d58c24953c2 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 27 Feb 2015 17:05:10 -0500
Subject: [PATCH] ACCUMULO-3465 Ensure ZK hosts can be set from clietn config

--
 .../main/java/org/apache/accumulo/core/util/shell/Shell.java  | 3 +++
 .../apache/accumulo/core/util/shell/ShellSetInstanceTest.java | 4 ++++
 2 files changed, 7 insertions(+)

diff --git a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
index 21e047022..1f14b6fa0 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
++ b/core/src/main/java/org/apache/accumulo/core/util/shell/Shell.java
@@ -440,6 +440,9 @@ public class Shell extends ShellOptions {
     if (instanceName == null) {
       instanceName = clientConfig.get(ClientProperty.INSTANCE_NAME);
     }
    if (keepers == null) {
      keepers = clientConfig.get(ClientProperty.INSTANCE_ZK_HOST);
    }
     if (instanceName == null || keepers == null) {
       AccumuloConfiguration conf = SiteConfiguration.getInstance(ServerConfigurationUtil.convertClientConfig(DefaultConfiguration.getInstance(), clientConfig));
       if (instanceName == null) {
diff --git a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
index d92eff046..0959c35d8 100644
-- a/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
++ b/core/src/test/java/org/apache/accumulo/core/util/shell/ShellSetInstanceTest.java
@@ -167,6 +167,10 @@ public class ShellSetInstanceTest {
       expect(clientConf.get(ClientProperty.INSTANCE_NAME)).andReturn(null);
     }
 
    if (!onlyHosts) {
      expect(clientConf.get(ClientProperty.INSTANCE_ZK_HOST)).andReturn(null);
    }

     mockStatic(ConfigSanityCheck.class);
     ConfigSanityCheck.validate(EasyMock.<AccumuloConfiguration> anyObject());
     expectLastCall().atLeastOnce();
- 
2.19.1.windows.1

