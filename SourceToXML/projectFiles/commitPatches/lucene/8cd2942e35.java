From 8cd2942e354687b6f655ef831d5d525d63dd46a8 Mon Sep 17 00:00:00 2001
From: Varun Thacker <varun@apache.org>
Date: Thu, 3 Aug 2017 09:50:00 -0700
Subject: [PATCH] SOLR-11182: A split shard failure on IOException should be
 logged

--
 solr/CHANGES.txt                                                | 2 ++
 .../src/java/org/apache/solr/update/DirectUpdateHandler2.java   | 1 +
 2 files changed, 3 insertions(+)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index be89d0632ee..012ac868224 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -420,6 +420,8 @@ Bug Fixes
 * SOLR-11163: Fix contrib/ltr Normalizer persistence after solr core reload or restart.
   (Yuki Yano via Christine Poerschke)
 
* SOLR-11182: A split shard failure on IOException should be logged (Varun Thacker)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index 3efb748fd02..bfed1c17d9a 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -911,6 +911,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     } catch (IOException e) {
       numErrors.increment();
       numErrorsCumulative.mark();
      throw e;
     }
   }
 
- 
2.19.1.windows.1

