From e4ed871c5e6f2b5882ef8a8327f7256508f1e568 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Fri, 7 Feb 2014 08:13:47 +0000
Subject: [PATCH] SOLR-5659: Add test for compositeId ending with a separator
 char

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1565572 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  3 ++
 .../apache/solr/cloud/ShardRoutingTest.java   | 30 +++++++++++++++++++
 .../solr/common/cloud/CompositeIdRouter.java  | 13 +++++---
 3 files changed, 42 insertions(+), 4 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 69c11bbaa5b..c424074cb64 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -377,6 +377,9 @@ Other Changes
 * SOLR-5702: Log config name found for collection at info level.
   (Christine Poerschke via Mark Miller)
 
* SOLR-5659: Add test for compositeId ending with an '!'.
  (Markus Jelsma, Anshum Gupta via shalin)

 ==================  4.6.1  ==================
 
 Versions of Major Components
diff --git a/solr/core/src/test/org/apache/solr/cloud/ShardRoutingTest.java b/solr/core/src/test/org/apache/solr/cloud/ShardRoutingTest.java
index 4275aad7f00..f432ccd1207 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ShardRoutingTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ShardRoutingTest.java
@@ -215,6 +215,36 @@ public class ShardRoutingTest extends AbstractFullDistribZkTestBase {
     doAddDoc("f1!f2!doc5");
 
     commit();

    doDBQ("*:*");
    commit();

    doAddDoc("b!");
    doAddDoc("c!doc1");
    commit();
    doQuery("b!,c!doc1", "q","*:*");
    UpdateRequest req = new UpdateRequest();
    req.deleteById("b!");
    req.process(cloudClient);
    commit();
    doQuery("c!doc1", "q","*:*");

    doDBQ("id:b!");
    commit();
    doQuery("c!doc1", "q","*:*");

    doDBQ("*:*");
    commit();

    doAddDoc("a!b!");
    doAddDoc("b!doc1");
    doAddDoc("c!doc2");
    doAddDoc("d!doc3");
    doAddDoc("e!doc4");
    doAddDoc("f1!f2!doc5");
    doAddDoc("f1!f2!doc5/5");
    commit();
    doQuery("a!b!,b!doc1,c!doc2,d!doc3,e!doc4,f1!f2!doc5,f1!f2!doc5/5", "q","*:*");
   }
 
 
diff --git a/solr/solrj/src/java/org/apache/solr/common/cloud/CompositeIdRouter.java b/solr/solrj/src/java/org/apache/solr/common/cloud/CompositeIdRouter.java
index 766b115d605..e649ad112a7 100644
-- a/solr/solrj/src/java/org/apache/solr/common/cloud/CompositeIdRouter.java
++ b/solr/solrj/src/java/org/apache/solr/common/cloud/CompositeIdRouter.java
@@ -191,10 +191,11 @@ public class CompositeIdRouter extends HashBasedRouter {
       String[] parts = key.split(SEPARATOR);
       this.key = key;
       pieces = parts.length;
      hashes = new int[pieces];
       numBits = new int[2];
      if (key.endsWith("!"))
      if (key.endsWith("!") && pieces < 3)
         pieces++;
      hashes = new int[pieces];

       if (pieces == 3) {
         numBits[0] = 8;
         numBits[1] = 8;
@@ -204,7 +205,7 @@ public class CompositeIdRouter extends HashBasedRouter {
         triLevel = false;
       }
 
      for (int i = 0; i < parts.length; i++) {
      for (int i = 0; i < pieces; i++) {
         if (i < pieces - 1) {
           int commaIdx = parts[i].indexOf(bitsSeparator);
 
@@ -213,7 +214,11 @@ public class CompositeIdRouter extends HashBasedRouter {
             parts[i] = parts[i].substring(0, commaIdx);
           }
         }
        hashes[i] = Hash.murmurhash3_x86_32(parts[i], 0, parts[i].length(), 0);
        //Last component of an ID that ends with a '!'
        if(i >= parts.length)
          hashes[i] = Hash.murmurhash3_x86_32("", 0, "".length(), 0);
        else
          hashes[i] = Hash.murmurhash3_x86_32(parts[i], 0, parts[i].length(), 0);
       }
       masks = getMasks();
     }
- 
2.19.1.windows.1

