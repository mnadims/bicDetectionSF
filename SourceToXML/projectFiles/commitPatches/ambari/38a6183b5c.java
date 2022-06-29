From 38a6183b5c273c6b5ffe52fda40edf99ee7a57ba Mon Sep 17 00:00:00 2001
From: Dipayan Bhowmick <dipayan.bhowmick@gmail.com>
Date: Wed, 13 Jul 2016 12:56:36 +0530
Subject: [PATCH] AMBARI-17684. Minor refactoring and java doc for code
 introduced in AMBARI-17317. (dipayanb)

--
 .../server/upgrade/AbstractUpgradeCatalog.java     | 14 +++++++++++++-
 1 file changed, 13 insertions(+), 1 deletion(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
index b5b486e8e8..eeaddff5e9 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/upgrade/AbstractUpgradeCatalog.java
@@ -917,7 +917,7 @@ public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
             Config tezSite = cluster.getDesiredConfigByType("tez-site");
             if (tezSite != null) {
               String currentTezHistoryUrlBase = tezSite.getProperties().get("tez.tez-ui.history-url.base");
              if(currentTezHistoryUrlBase != null && !currentTezHistoryUrlBase.isEmpty()) {
              if (!StringUtils.isEmpty(currentTezHistoryUrlBase)) {
                 String newTezHistoryUrlBase = getUpdatedTezHistoryUrlBase(currentTezHistoryUrlBase);
                 updateConfigurationProperties("tez-site", Collections.singletonMap("tez.tez-ui.history-url.base", newTezHistoryUrlBase), true, false);
               }
@@ -928,6 +928,12 @@ public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
     }
   }
 
  /**
   * Transforms the existing tez history url base to the new url considering the latest tez view version.
   * @param currentTezHistoryUrlBase Existing value of the tez history url base
   * @return the updated tez history url base
   * @throws AmbariException if currentTezHistoryUrlBase is malformed or is not compatible with the Tez View url REGEX
     */
   protected String getUpdatedTezHistoryUrlBase(String currentTezHistoryUrlBase) throws AmbariException{
     String pattern = "(.*\\/TEZ\\/)(.*)(\\/TEZ_CLUSTER_INSTANCE)";
     Pattern regex = Pattern.compile(pattern);
@@ -948,6 +954,12 @@ public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
     return prefix + latestTezViewVersion + suffix;
   }
 
  /**
   * Given the old configured version, this method tries to get the new version of tez view by reading the tez-view jar.
   * Assumption - only a single tez-view jar will be present in the views directory.
   * @param oldVersion It is returned if there is a failure in finding the new version
   * @return newVersion of the tez view. Returns oldVersion if there error encountered if finding the new version number.
   */
   protected String getLatestTezViewVersion(String oldVersion) {
     File viewsDirectory = configuration.getViewsDir();
     File[] files = viewsDirectory.listFiles(new FilenameFilter() {
- 
2.19.1.windows.1

