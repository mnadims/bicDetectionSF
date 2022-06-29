From 5f618b8000d92f650029f465013539daac3fe6f5 Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Mon, 18 Mar 2013 05:27:15 +0000
Subject: [PATCH] SOLR-4604: UpdateLog#init is over called on SolrCore#reload

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1457646 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                                       |  3 +--
 solr/core/src/java/org/apache/solr/core/SolrCore.java  |  4 +++-
 .../org/apache/solr/update/DirectUpdateHandler2.java   |  2 +-
 .../src/java/org/apache/solr/update/UpdateHandler.java | 10 +++++++++-
 4 files changed, 14 insertions(+), 5 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index cca59d07100..702eca293a5 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -157,8 +157,7 @@ Bug Fixes
 * SOLR-4601: A Collection that is only partially created and then deleted will 
   leave pre allocated shard information in ZooKeeper. (Mark Miller)
 
* SOLR-4604: SolrCore is not using the UpdateHandler that is passed to it in 
  SolrCore#reload. (Mark Miller)
* SOLR-4604: UpdateLog#init is over called on SolrCore#reload. (Mark Miller)
 
 Optimizations
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index 82e15c978f9..c82c10035ca 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -804,7 +804,9 @@ public final class SolrCore implements SolrInfoMBean {
         this.updateHandler = createUpdateHandler(updateHandlerClass == null ? DirectUpdateHandler2.class
             .getName() : updateHandlerClass);
       } else {
        this.updateHandler = updateHandler;
        this.updateHandler = createUpdateHandler(
            updateHandlerClass == null ? DirectUpdateHandler2.class.getName()
                : updateHandlerClass, updateHandler);
       }
       infoRegistry.put("updateHandler", this.updateHandler);
       
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index 8a82f79dfbf..8d592a11203 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -110,7 +110,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   }
   
   public DirectUpdateHandler2(SolrCore core, UpdateHandler updateHandler) {
    super(core);
    super(core, updateHandler.getUpdateLog());
     solrCoreState = core.getSolrCoreState();
     
     UpdateHandlerInfo updateHandlerInfo = core.getSolrConfig()
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
index fd22c920838..ec4f72c966f 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
@@ -122,6 +122,10 @@ public abstract class UpdateHandler implements SolrInfoMBean {
   }
 
   public UpdateHandler(SolrCore core)  {
    this(core, null);
  }
  
  public UpdateHandler(SolrCore core, UpdateLog updateLog)  {
     this.core=core;
     schema = core.getSchema();
     idField = schema.getUniqueKeyField();
@@ -131,7 +135,11 @@ public abstract class UpdateHandler implements SolrInfoMBean {
     if (!core.isReloaded() && !core.getDirectoryFactory().isPersistent()) {
       clearLog(ulogPluginInfo);
     }
    initLog(ulogPluginInfo);
    if (updateLog == null) {
      initLog(ulogPluginInfo);
    } else {
      this.ulog = updateLog;
    }
   }
 
   /**
- 
2.19.1.windows.1

