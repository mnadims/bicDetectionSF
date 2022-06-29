From c672b5630502793455a339d23f046d20ca354714 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 25 Jun 2013 19:57:17 +0000
Subject: [PATCH] SOLR-4960: fix race in CoreContainer.getCore

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1496620 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  5 ++++-
 .../org/apache/solr/core/CoreContainer.java   |  5 ++---
 .../java/org/apache/solr/core/SolrCores.java  | 22 +++++++++----------
 3 files changed, 17 insertions(+), 15 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 016e734c633..3e72788b93e 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -192,7 +192,10 @@ Bug Fixes
 
 * SOLR-4949: UI Analysis page dropping characters from input box (steffkes)
 
* SOLR-4960: race condition in shutdown of CoreContainer. (yonik)
* SOLR-4960: Fix race conditions in shutdown of CoreContainer
  and getCore that could cause a request to attempt to use a core that
  has shut down. (yonik)

 
 Optimizations
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 38390784aa6..69bae0d05b5 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -844,7 +844,7 @@ public class CoreContainer
     try {
       name = checkDefault(name);
 
      SolrCore core = solrCores.getCoreFromAnyList(name);
      SolrCore core = solrCores.getCoreFromAnyList(name, false);
       if (core == null)
         throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "No such core: " + name );
 
@@ -952,10 +952,9 @@ public class CoreContainer
     name = checkDefault(name);
 
     // Do this in two phases since we don't want to lock access to the cores over a load.
    SolrCore core = solrCores.getCoreFromAnyList(name);
    SolrCore core = solrCores.getCoreFromAnyList(name, true);
 
     if (core != null) {
      core.open();
       return core;
     }
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCores.java b/solr/core/src/java/org/apache/solr/core/SolrCores.java
index fe2ab65422c..fd233376d27 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCores.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCores.java
@@ -303,20 +303,20 @@ class SolrCores {
     }
   }
 
  protected SolrCore getCoreFromAnyList(String name) {
    SolrCore core;

  /* If you don't increment the reference count, someone could close the core before you use it. */
  protected SolrCore getCoreFromAnyList(String name, boolean incRefCount) {
     synchronized (modifyLock) {
      core = cores.get(name);
      if (core != null) {
        return core;
      SolrCore core = cores.get(name);

      if (core == null) {
        core = transientCores.get(name);
       }
 
      if (dynamicDescriptors.size() == 0) {
        return null; // Nobody even tried to define any transient cores, so we're done.
      if (core != null && incRefCount) {
        core.open();
       }
      // Now look for already loaded transient cores.
      return transientCores.get(name);

      return core;
     }
   }
 
@@ -429,7 +429,7 @@ class SolrCores {
         if (! pendingCoreOps.add(name)) {
           CoreContainer.log.warn("Replaced an entry in pendingCoreOps {}, we should not be doing this", name);
         }
        return getCoreFromAnyList(name); // we might have been _unloading_ the core, so return the core if it was loaded.
        return getCoreFromAnyList(name, false); // we might have been _unloading_ the core, so return the core if it was loaded.
       }
     }
     return null;
- 
2.19.1.windows.1

