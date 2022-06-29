From d6b40cc39a2f431790be1c26086873567cf68d9b Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 25 Jun 2013 17:18:46 +0000
Subject: [PATCH] SOLR-4960: race condition in shutdown of CoreContainer

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1496546 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  2 ++
 .../java/org/apache/solr/core/SolrCores.java  | 28 ++++++++-----------
 2 files changed, 13 insertions(+), 17 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 696d4fe35b3..7af990ea1a9 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -194,6 +194,8 @@ Bug Fixes
 
 * SOLR-4949: UI Analysis page dropping characters from input box (steffkes)
 
* SOLR-4960: race condition in shutdown of CoreContainer. (yonik)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCores.java b/solr/core/src/java/org/apache/solr/core/SolrCores.java
index e1439924aa3..ea70f60112c 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCores.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCores.java
@@ -100,7 +100,7 @@ class SolrCores {
   // We are shutting down. You can't hold the lock on the various lists of cores while they shut down, so we need to
   // make a temporary copy of the names and shut them down outside the lock.
   protected void close() {
    List<String> coreNames;
    Collection<SolrCore> coreList;
     List<String> transientNames;
     List<SolrCore> pendingToClose;
 
@@ -110,27 +110,21 @@ class SolrCores {
 
     while (true) {
       synchronized (modifyLock) {
        coreNames = new ArrayList<String>(cores.keySet());
        // make a copy of the cores then clear the map so the core isn't handed out to a request again
        coreList = new ArrayList<SolrCore>(cores.values());
        cores.clear();

         transientNames = new ArrayList<String>(transientCores.keySet());
         pendingToClose = new ArrayList<SolrCore>(pendingCloses);
       }
 
      if (coreNames.size() == 0 && transientNames.size() == 0 && pendingToClose.size() == 0) break;
      if (coreList.size() == 0 && transientNames.size() == 0 && pendingToClose.size() == 0) break;
 
      for (String coreName : coreNames) {
        SolrCore core = cores.get(coreName);
        if (core == null) {
          CoreContainer.log.info("Core " + coreName + " moved from core container list before closing.");
        } else {
          try {
            core.close();
          } catch (Throwable t) {
            SolrException.log(CoreContainer.log, "Error shutting down core", t);
          } finally {
            synchronized (modifyLock) {
              cores.remove(coreName);
            }
          }
      for (SolrCore core : coreList) {
        try {
          core.close();
        } catch (Throwable t) {
          SolrException.log(CoreContainer.log, "Error shutting down core", t);
         }
       }
 
- 
2.19.1.windows.1

