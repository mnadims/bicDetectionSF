From d0156b1126f094e4e469172d55842ed77cb82943 Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Sat, 2 Apr 2016 20:13:43 +0200
Subject: [PATCH] SOLR-4509: Fix test failures with Java 9 module system by
 doing a correct cleanup

--
 .../apache/solr/cloud/TestCloudDeleteByQuery.java    | 12 ++++++------
 .../solr/cloud/TestTolerantUpdateProcessorCloud.java | 11 ++++++-----
 .../TestTolerantUpdateProcessorRandomCloud.java      |  5 +++++
 3 files changed, 17 insertions(+), 11 deletions(-)

diff --git a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
index 26db949e5c3..9ded40c60a5 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestCloudDeleteByQuery.java
@@ -91,12 +91,12 @@ public class TestCloudDeleteByQuery extends SolrCloudTestCase {
   
   @AfterClass
   private static void afterClass() throws Exception {
    CLOUD_CLIENT.close();
    S_ONE_LEADER_CLIENT.close();
    S_TWO_LEADER_CLIENT.close();
    S_ONE_NON_LEADER_CLIENT.close();
    S_TWO_NON_LEADER_CLIENT.close();
    NO_COLLECTION_CLIENT.close();
    CLOUD_CLIENT.close(); CLOUD_CLIENT = null;
    S_ONE_LEADER_CLIENT.close(); S_ONE_LEADER_CLIENT = null;
    S_TWO_LEADER_CLIENT.close(); S_TWO_LEADER_CLIENT = null;
    S_ONE_NON_LEADER_CLIENT.close(); S_ONE_NON_LEADER_CLIENT = null;
    S_TWO_NON_LEADER_CLIENT.close(); S_TWO_NON_LEADER_CLIENT = null;
    NO_COLLECTION_CLIENT.close(); NO_COLLECTION_CLIENT = null;
   }
   
   @BeforeClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
index 929d736d79f..6c816733e68 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorCloud.java
@@ -206,11 +206,12 @@ public class TestTolerantUpdateProcessorCloud extends SolrCloudTestCase {
   
   @AfterClass
   public static void afterClass() throws IOException {
   close(S_ONE_LEADER_CLIENT);
   close(S_TWO_LEADER_CLIENT);
   close(S_ONE_NON_LEADER_CLIENT);
   close(S_TWO_NON_LEADER_CLIENT);
   close(NO_COLLECTION_CLIENT);
   close(S_ONE_LEADER_CLIENT); S_ONE_LEADER_CLIENT = null;
   close(S_TWO_LEADER_CLIENT); S_TWO_LEADER_CLIENT = null;
   close(S_ONE_NON_LEADER_CLIENT); S_ONE_NON_LEADER_CLIENT = null;
   close(S_TWO_NON_LEADER_CLIENT); S_TWO_NON_LEADER_CLIENT = null;
   close(NO_COLLECTION_CLIENT); NO_COLLECTION_CLIENT = null;
   close(CLOUD_CLIENT); CLOUD_CLIENT = null;
   }
   
   private static void close(SolrClient client) throws IOException {
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
index a722ad2cdb0..3a9680e9a10 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestTolerantUpdateProcessorRandomCloud.java
@@ -138,6 +138,11 @@ public class TestTolerantUpdateProcessorRandomCloud extends SolrCloudTestCase {
         client.close();
       }
     }
    NODE_CLIENTS = null;
    if (CLOUD_CLIENT != null) {
      CLOUD_CLIENT.close();
    }
    CLOUD_CLIENT = null;
   }
   
   public void testRandomUpdates() throws Exception {
- 
2.19.1.windows.1

