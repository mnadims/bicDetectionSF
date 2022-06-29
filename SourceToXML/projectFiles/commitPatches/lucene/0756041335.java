From 07560413353a46c688faad88044fc447c8eb479f Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Mon, 18 Mar 2013 05:31:29 +0000
Subject: [PATCH] SOLR-4604: remove previous init code

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1457648 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/solr/update/DirectUpdateHandler2.java    | 5 -----
 1 file changed, 5 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index 8d592a11203..f12f5c3a3bb 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -123,11 +123,6 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     int softCommitTimeUpperBound = updateHandlerInfo.autoSoftCommmitMaxTime; // getInt("updateHandler/autoSoftCommit/maxTime", -1);
     softCommitTracker = new CommitTracker("Soft", core, softCommitDocsUpperBound, softCommitTimeUpperBound, updateHandlerInfo.openSearcher, true);
     
    this.ulog = updateHandler.getUpdateLog();
    if (this.ulog != null) {
      this.ulog.init(this, core);
    }
    
     commitWithinSoftCommit = updateHandlerInfo.commitWithinSoftCommit;
   }
 
- 
2.19.1.windows.1

