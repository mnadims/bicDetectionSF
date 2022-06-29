From 36183cad87dfc3fc8f0a1e0b0c210e8bd14a4ce0 Mon Sep 17 00:00:00 2001
From: Varun Thacker <varunthacker1989@gmail.com>
Date: Mon, 27 Jun 2016 12:08:45 +0530
Subject: [PATCH] SOLR-7374: Fixing test failures like build #3366. Index a
 minimum of 1 doc

--
 .../test/org/apache/solr/handler/BackupRestoreUtils.java    | 6 ++----
 1 file changed, 2 insertions(+), 4 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java b/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
index bbc80beb692..6bc7d47b560 100644
-- a/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
++ b/solr/core/src/test/org/apache/solr/handler/BackupRestoreUtils.java
@@ -24,6 +24,7 @@ import java.util.List;
 import java.util.Random;
 
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
 import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.SolrServerException;
 import org.apache.solr.client.solrj.response.QueryResponse;
@@ -40,11 +41,8 @@ public class BackupRestoreUtils extends LuceneTestCase {
     masterClient.deleteByQuery(collectionName, "*:*");
 
     Random random = new Random(docsSeed);// use a constant seed for the whole test run so that we can easily re-index.
    int nDocs = random.nextInt(100);
    int nDocs = TestUtil.nextInt(random, 1, 100);
     log.info("Indexing {} test docs", nDocs);
    if (nDocs == 0) {
      return 0;
    }
 
     List<SolrInputDocument> docs = new ArrayList<>(nDocs);
     for (int i = 0; i < nDocs; i++) {
- 
2.19.1.windows.1

