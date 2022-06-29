From 14738859ccdffa64ba5d66a5cdf3757723826736 Mon Sep 17 00:00:00 2001
From: Swapan Shridhar <sshridhar@hortonworks.com>
Date: Tue, 7 Feb 2017 12:58:40 -0800
Subject: [PATCH] AMBARI-19903. In UpgradeCatalog250.java, (1). Fix value for
 config "hive.llap.daemon.rpc.port" to be updated as "0" and (2). Remove
 update for config 'hive.llap.execution.mode'.

--
 .../org/apache/ambari/server/upgrade/UpgradeCatalog250.java  | 5 +----
 1 file changed, 1 insertion(+), 4 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
index 18e97448fa..8f0d218d80 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/UpgradeCatalog250.java
@@ -654,16 +654,13 @@ public class UpgradeCatalog250 extends AbstractUpgradeCatalog {
 
             updateConfigurationProperties(HIVE_INTERACTIVE_SITE, Collections.singletonMap("hive.auto.convert.join.noconditionaltask.size",
                 "1000000000"), true, true);
            updateConfigurationProperties(HIVE_INTERACTIVE_SITE,
                Collections.singletonMap("hive.llap.execution.mode", "only"),
                true, true);
             String llapRpcPortString = hiveInteractiveSite.getProperties().get("hive.llap.daemon.rpc.port");
             if (StringUtils.isNotBlank(llapRpcPortString)) {
               try {
                 int llapRpcPort = Integer.parseInt(llapRpcPortString);
                 if (llapRpcPort == 15001) {
                   updateConfigurationProperties(HIVE_INTERACTIVE_SITE,
                      Collections.singletonMap("hive.llap.daemon.rpc.port", "only"),
                      Collections.singletonMap("hive.llap.daemon.rpc.port", "0"),
                       true, true);
                 }
               } catch (NumberFormatException e) {
- 
2.19.1.windows.1

