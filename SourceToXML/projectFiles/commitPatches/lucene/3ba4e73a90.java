From 3ba4e73a9052b94d0d878fcd2fdd5f050232a41d Mon Sep 17 00:00:00 2001
From: Andrzej Bialecki <ab@apache.org>
Date: Wed, 12 Apr 2017 10:40:58 +0200
Subject: [PATCH] SOLR-9959 Increase the timeout to allow searcher to register
 metrics.

--
 .../test/org/apache/solr/handler/admin/StatsReloadRaceTest.java | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java b/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
index ca3b76e6bef..c455b6919ea 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
@@ -126,7 +126,7 @@ public class StatsReloadRaceTest extends SolrTestCaseJ4 {
         assertTrue(metrics.get(key) instanceof Long);
         break;
       } else {
        Thread.sleep(500);
        Thread.sleep(1000);
       }
     }
     assertTrue("Key " + key + " not found in registry " + registry, found);
- 
2.19.1.windows.1

