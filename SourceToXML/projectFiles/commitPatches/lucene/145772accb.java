From 145772accbd21cf64216ce62ce713308b4381d76 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Fri, 31 May 2013 18:37:24 +0000
Subject: [PATCH] SOLR-4858: call ulog.init on core reopen

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1488349 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  4 +++
 .../java/org/apache/solr/core/SolrCore.java   |  4 +++
 .../solr/update/DirectUpdateHandler2.java     |  9 +++++
 .../org/apache/solr/update/UpdateHandler.java | 14 ++++----
 .../org/apache/solr/update/UpdateLog.java     |  4 +++
 .../org/apache/solr/search/TestRecovery.java  | 34 +++++++++++++++++++
 6 files changed, 61 insertions(+), 8 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index e022c2e3339..3303a7a05ef 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -220,6 +220,10 @@ Bug Fixes
   EmptyEntityResolver to prevent loading of external entities like
   UpdateRequestHandler does.  (Hossman, Uwe Schindler)
 
* SOLR-4858: SolrCore reloading was broken when the UpdateLog
  was enabled.  (Hossman, Anshum Gupta, Alexey Serba, Mark Miller, yonik)


 Other Changes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index cac711f832d..15f37fdeaa8 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -1352,6 +1352,10 @@ public final class SolrCore implements SolrInfoMBean {
    * This method acquires openSearcherLock - do not call with searckLock held!
    */
   public RefCounted<SolrIndexSearcher>  openNewSearcher(boolean updateHandlerReopens, boolean realtime) {
    if (isClosed()) { // catch some errors quicker
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "openNewSearcher called on closed core");
    }

     SolrIndexSearcher tmp;
     RefCounted<SolrIndexSearcher> newestSearcher = null;
     boolean nrt = solrConfig.reopenReaders && updateHandlerReopens;
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index 9fc36b44e76..6f185a5f640 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -108,6 +108,8 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     softCommitTracker = new CommitTracker("Soft", core, softCommitDocsUpperBound, softCommitTimeUpperBound, true, true);
     
     commitWithinSoftCommit = updateHandlerInfo.commitWithinSoftCommit;


   }
   
   public DirectUpdateHandler2(SolrCore core, UpdateHandler updateHandler) {
@@ -125,6 +127,13 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     softCommitTracker = new CommitTracker("Soft", core, softCommitDocsUpperBound, softCommitTimeUpperBound, updateHandlerInfo.openSearcher, true);
     
     commitWithinSoftCommit = updateHandlerInfo.commitWithinSoftCommit;

    UpdateLog existingLog = updateHandler.getUpdateLog();
    if (this.ulog != null && this.ulog == existingLog) {
      // If we are reusing the existing update log, inform the log that it's update handler has changed.
      // We do this as late as possible.
      this.ulog.init(this, core);
    }
   }
 
   private void deleteAll() throws IOException {
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
index e79b15f06ad..380c251dd49 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
@@ -52,7 +52,7 @@ public abstract class UpdateHandler implements SolrInfoMBean {
   protected Vector<SolrEventListener> softCommitCallbacks = new Vector<SolrEventListener>();
   protected Vector<SolrEventListener> optimizeCallbacks = new Vector<SolrEventListener>();
 
  protected volatile UpdateLog ulog;
  protected UpdateLog ulog;
 
   private void parseEventListeners() {
     final Class<SolrEventListener> clazz = SolrEventListener.class;
@@ -72,13 +72,15 @@ public abstract class UpdateHandler implements SolrInfoMBean {
   }
 
 
  private void initLog(PluginInfo ulogPluginInfo) {
    if (ulogPluginInfo != null && ulogPluginInfo.isEnabled()) {
  private void initLog(PluginInfo ulogPluginInfo, UpdateLog existingUpdateLog) {
    ulog = existingUpdateLog;
    if (ulog == null && ulogPluginInfo != null && ulogPluginInfo.isEnabled()) {
       ulog = new UpdateLog();
       ulog.init(ulogPluginInfo);
       // ulog = core.createInitInstance(ulogPluginInfo, UpdateLog.class, "update log", "solr.NullUpdateLog");
       ulog.init(this, core);
     }
    // ulog.init() when reusing an existing log is deferred (currently at the end of the DUH2 constructor
   }
 
   // not thread safe - for startup
@@ -130,11 +132,7 @@ public abstract class UpdateHandler implements SolrInfoMBean {
     if (!core.isReloaded() && !core.getDirectoryFactory().isPersistent()) {
       clearLog(ulogPluginInfo);
     }
    if (updateLog == null) {
      initLog(ulogPluginInfo);
    } else {
      this.ulog = updateLog;
    }
    initLog(ulogPluginInfo, updateLog);
   }
 
   /**
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateLog.java b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
index 989f3a06e8e..c7c55304ef8 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateLog.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
@@ -189,6 +189,10 @@ public class UpdateLog implements PluginInfoInitialized {
     defaultSyncLevel = SyncLevel.getSyncLevel((String)info.initArgs.get("syncLevel"));
   }
 
  /* Note, when this is called, uhandler is not completely constructed.
   * This must be called when a new log is created, or
   * for an existing log whenever the core or update handler changes.
   */
   public void init(UpdateHandler uhandler, SolrCore core) {
     // ulogDir from CoreDescriptor overrides
     String ulogDir = core.getCoreDescriptor().getUlogDir();
diff --git a/solr/core/src/test/org/apache/solr/search/TestRecovery.java b/solr/core/src/test/org/apache/solr/search/TestRecovery.java
index 5a8e8c0d748..5555dfd5aab 100644
-- a/solr/core/src/test/org/apache/solr/search/TestRecovery.java
++ b/solr/core/src/test/org/apache/solr/search/TestRecovery.java
@@ -17,6 +17,7 @@
 package org.apache.solr.search;
 
 
import org.apache.solr.common.SolrException;
 import org.noggit.ObjectBuilder;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.request.SolrQueryRequest;
@@ -487,7 +488,40 @@ public class TestRecovery extends SolrTestCaseJ4 {
   }
 
 

  // we need to make sure that the log is informed of a core reload
   @Test
  public void testReload() throws Exception {
    long version = addAndGetVersion(sdoc("id","reload1") , null);

    h.reload();

    version = addAndGetVersion(sdoc("id","reload1", "_version_", Long.toString(version)), null);

    assertU(commit());

    // if we try the optimistic concurrency again, the tlog lookup maps should be clear
    // and we should go to the index to check the version.  This indirectly tests that
    // the update log was informed of the reload.  See SOLR-4858

    version = addAndGetVersion(sdoc("id","reload1", "_version_", Long.toString(version)), null);

    // a deleteByQuery currently forces open a new realtime reader via the update log.
    // This also tests that the update log was informed of the new udpate handler.

    deleteByQueryAndGetVersion("foo_t:hownowbrowncow", null);

    version = addAndGetVersion(sdoc("id","reload1", "_version_", Long.toString(version)), null);

    // if the update log was not informed of the new update handler, then the old core will
    // incorrectly be used for some of the operations above and opened searchers
    // will never be closed.  This used to cause the test framework to fail because of unclosed directory checks.
    // SolrCore.openNewSearcher was modified to throw an error if the core is closed, resulting in
    // a faster fail.
  }


    @Test
   public void testBufferingFlags() throws Exception {
 
     DirectUpdateHandler2.commitOnClose = false;
- 
2.19.1.windows.1

