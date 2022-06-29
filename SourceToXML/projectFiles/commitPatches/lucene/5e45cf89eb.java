From 5e45cf89eb60cbe62aa5e809dd432af4fa4fd77c Mon Sep 17 00:00:00 2001
From: Mark Robert Miller <markrmiller@apache.org>
Date: Sat, 20 Aug 2011 21:58:14 +0000
Subject: [PATCH] SOLR-2331: fix too many close on SolrCore test issue

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1159921 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/test/org/apache/solr/core/TestCoreContainer.java | 9 ++-------
 1 file changed, 2 insertions(+), 7 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
index 45717ded9d4..94bf50c5e8a 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
++ b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
@@ -130,6 +130,8 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
           FileUtils.contentEquals(threeXml, fourXml));
       
     } finally {
      // y is closed by the container, but
      // x has been removed from the container
       if (x != null) {
         try {
           x.close();
@@ -137,13 +139,6 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
           log.error("", e);
         }
       }
      if (y != null) {
        try {
          y.close();
        } catch (Exception e) {
          log.error("", e);
        }
      }
     }
   }
   
- 
2.19.1.windows.1

