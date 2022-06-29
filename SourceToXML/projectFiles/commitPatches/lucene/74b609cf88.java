From 74b609cf884d8a545b90d2e0293182a86e5b778d Mon Sep 17 00:00:00 2001
From: Varun Thacker <varun@apache.org>
Date: Thu, 6 Jul 2017 12:18:08 -0700
Subject: [PATCH] SOLR-10826: Fix CloudSolrClient to expand the collection
 parameter correctly

--
 solr/CHANGES.txt                              |  2 +
 .../client/solrj/impl/CloudSolrClient.java    |  2 +-
 .../solrj/impl/CloudSolrClientTest.java       | 39 +++++++++++++++++--
 3 files changed, 38 insertions(+), 5 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 8dc14b1d721..abfe079df93 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -321,6 +321,8 @@ Bug Fixes
 
 * SOLR-10983: Fix DOWNNODE -> queue-work explosion (Scott Blum, Joshua Humphries)
 
* SOLR-10826: Fix CloudSolrClient to expand the collection parameter correctly (Tim Owen via Varun Thacker)

 Optimizations
 ----------------------
 
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
index 9948857e4a2..eeb96af84e5 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/CloudSolrClient.java
@@ -1129,7 +1129,7 @@ public class CloudSolrClient extends SolrClient {
     // validate collections
     for (String collectionName : rawCollectionsList) {
       if (stateProvider.getState(collectionName) == null) {
        String alias = stateProvider.getAlias(collection);
        String alias = stateProvider.getAlias(collectionName);
         if (alias != null) {
           List<String> aliasList = StrUtils.splitSmart(alias, ",", true);
           collectionNames.addAll(aliasList);
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
index aadf0e2e394..2a441b90ab3 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
@@ -85,6 +85,7 @@ import org.slf4j.LoggerFactory;
 public class CloudSolrClientTest extends SolrCloudTestCase {
 
   private static final String COLLECTION = "collection1";
  private static final String COLLECTION2 = "2nd_collection";
 
   private static final String id = "id";
 
@@ -173,19 +174,49 @@ public class CloudSolrClientTest extends SolrCloudTestCase {
 
   @Test
   public void testAliasHandling() throws Exception {

    CollectionAdminRequest.createCollection(COLLECTION2, "conf", 2, 1).process(cluster.getSolrClient());
    AbstractDistribZkTestBase.waitForRecoveriesToFinish(COLLECTION2, cluster.getSolrClient().getZkStateReader(),
        false, true, TIMEOUT);

     CloudSolrClient client = getRandomClient();
     SolrInputDocument doc = new SolrInputDocument("id", "1", "title_s", "my doc");
     client.add(COLLECTION, doc);
     client.commit(COLLECTION);

     CollectionAdminRequest.createAlias("testalias", COLLECTION).process(cluster.getSolrClient());
 
    // ensure that the alias has been registered
    assertEquals(COLLECTION,
        new CollectionAdminRequest.ListAliases().process(cluster.getSolrClient()).getAliases().get("testalias"));
    SolrInputDocument doc2 = new SolrInputDocument("id", "2", "title_s", "my doc too");
    client.add(COLLECTION2, doc2);
    client.commit(COLLECTION2);
    CollectionAdminRequest.createAlias("testalias2", COLLECTION2).process(cluster.getSolrClient());

    CollectionAdminRequest.createAlias("testaliascombined", COLLECTION + "," + COLLECTION2).process(cluster.getSolrClient());

    // ensure that the aliases have been registered
    Map<String, String> aliases = new CollectionAdminRequest.ListAliases().process(cluster.getSolrClient()).getAliases();
    assertEquals(COLLECTION, aliases.get("testalias"));
    assertEquals(COLLECTION2, aliases.get("testalias2"));
    assertEquals(COLLECTION + "," + COLLECTION2, aliases.get("testaliascombined"));
 
     assertEquals(1, client.query(COLLECTION, params("q", "*:*")).getResults().getNumFound());
     assertEquals(1, client.query("testalias", params("q", "*:*")).getResults().getNumFound());

    assertEquals(1, client.query(COLLECTION2, params("q", "*:*")).getResults().getNumFound());
    assertEquals(1, client.query("testalias2", params("q", "*:*")).getResults().getNumFound());

    assertEquals(2, client.query("testaliascombined", params("q", "*:*")).getResults().getNumFound());

    ModifiableSolrParams paramsWithBothCollections = params("q", "*:*", "collection", COLLECTION + "," + COLLECTION2);
    assertEquals(2, client.query(null, paramsWithBothCollections).getResults().getNumFound());

    ModifiableSolrParams paramsWithBothAliases = params("q", "*:*", "collection", "testalias,testalias2");
    assertEquals(2, client.query(null, paramsWithBothAliases).getResults().getNumFound());

    ModifiableSolrParams paramsWithCombinedAlias = params("q", "*:*", "collection", "testaliascombined");
    assertEquals(2, client.query(null, paramsWithCombinedAlias).getResults().getNumFound());

    ModifiableSolrParams paramsWithMixedCollectionAndAlias = params("q", "*:*", "collection", "testalias," + COLLECTION2);
    assertEquals(2, client.query(null, paramsWithMixedCollectionAndAlias).getResults().getNumFound());
   }
 
   @Test
- 
2.19.1.windows.1

