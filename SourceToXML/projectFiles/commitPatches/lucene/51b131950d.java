From 51b131950de0357fc64e0e951b887eb30a704cd1 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Thu, 5 May 2016 21:00:20 +0530
Subject: [PATCH] SOLR-9036: Solr slave is doing full replication (entire
 index) of index after master restart

--
 solr/CHANGES.txt                              |  3 +
 .../org/apache/solr/handler/IndexFetcher.java |  5 +-
 .../solr/handler/TestReplicationHandler.java  | 77 +++++++++++++++++++
 3 files changed, 84 insertions(+), 1 deletion(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 9c91a5ff0bf..9dc28d91108 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -184,6 +184,9 @@ Bug Fixes
 * SOLR-9030: The 'downnode' overseer command can trip asserts in ZkStateWriter.
   (Scott Blum, Mark Miller, shalin)
 
* SOLR-9036: Solr slave is doing full replication (entire index) of index after master restart.
  (Lior Sapir, Mark Miller, shalin)

 Optimizations
 ----------------------
 * SOLR-8722: Don't force a full ZkStateReader refresh on every Overseer operation.
diff --git a/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java b/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
index 224cc641382..9c1cbb60b97 100644
-- a/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
++ b/solr/core/src/java/org/apache/solr/handler/IndexFetcher.java
@@ -565,7 +565,10 @@ public class IndexFetcher {
         }
       }
 
      core.getUpdateHandler().getSolrCoreState().setLastReplicateIndexSuccess(successfulInstall);
      if (core.getCoreDescriptor().getCoreContainer().isZooKeeperAware()) {
        // we only track replication success in SolrCloud mode
        core.getUpdateHandler().getSolrCoreState().setLastReplicateIndexSuccess(successfulInstall);
      }
 
       filesToDownload = filesDownloaded = confFilesDownloaded = confFilesToDownload = tlogFilesToDownload = tlogFilesDownloaded = null;
       markReplicationStop();
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
index cf40d4b45ad..66b2ba20d8e 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
@@ -58,6 +58,7 @@ import org.apache.solr.common.SolrDocument;
 import org.apache.solr.common.SolrDocumentList;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
@@ -586,6 +587,82 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
     assertEquals(nDocs+1, numFound(rQuery(nDocs+1, "*:*", slaveClient)));
   }
 
  /**
   * We assert that if master is down for more than poll interval,
   * the slave doesn't re-fetch the whole index from master again if
   * the index hasn't changed. See SOLR-9036
   */
  @Test
  public void doTestIndexFetchOnMasterRestart() throws Exception  {
    useFactory(null);
    try {
      clearIndexWithReplication();
      // change solrconfig having 'replicateAfter startup' option on master
      master.copyConfigFile(CONF_DIR + "solrconfig-master2.xml",
          "solrconfig.xml");

      masterJetty.stop();
      masterJetty.start();

      nDocs--;
      for (int i = 0; i < nDocs; i++)
        index(masterClient, "id", i, "name", "name = " + i);

      masterClient.commit();

      NamedList masterQueryRsp = rQuery(nDocs, "*:*", masterClient);
      SolrDocumentList masterQueryResult = (SolrDocumentList) masterQueryRsp.get("response");
      assertEquals(nDocs, numFound(masterQueryRsp));

      //get docs from slave and check if number is equal to master
      NamedList slaveQueryRsp = rQuery(nDocs, "*:*", slaveClient);
      SolrDocumentList slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
      assertEquals(nDocs, numFound(slaveQueryRsp));

      //compare results
      String cmp = BaseDistributedSearchTestCase.compare(masterQueryResult, slaveQueryResult, 0, null);
      assertEquals(null, cmp);

      assertEquals(1, Integer.parseInt(getSlaveDetails("timesIndexReplicated")));
      String timesFailed = getSlaveDetails("timesFailed");
      assertEquals(0, Integer.parseInt(timesFailed != null ?  timesFailed : "0"));

      masterJetty.stop();

      // poll interval on slave is 1 second, so we just sleep for a few seconds
      Thread.sleep(2000);

      masterJetty.start();

      // poll interval on slave is 1 second, so we just sleep for a few seconds
      Thread.sleep(2000);

      //get docs from slave and assert that they are still the same as before
      slaveQueryRsp = rQuery(nDocs, "*:*", slaveClient);
      slaveQueryResult = (SolrDocumentList) slaveQueryRsp.get("response");
      assertEquals(nDocs, numFound(slaveQueryRsp));

      int failed = Integer.parseInt(getSlaveDetails("timesFailed"));
      assertTrue(failed > 0);
      assertEquals(1, Integer.parseInt(getSlaveDetails("timesIndexReplicated")) - failed);
    } finally {
      resetFactory();
    }
  }

  private String getSlaveDetails(String keyName) throws SolrServerException, IOException {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(CommonParams.QT, "/replication");
    params.set("command", "details");
    QueryResponse response = slaveClient.query(params);
    System.out.println("SHALIN: " + response.getResponse());
    // details/slave/timesIndexReplicated
    NamedList<Object> details = (NamedList<Object>) response.getResponse().get("details");
    NamedList<Object> slave = (NamedList<Object>) details.get("slave");
    Object o = slave.get(keyName);
    return o != null ? o.toString() : null;
  }

   @Test
   public void doTestIndexFetchWithMasterUrl() throws Exception {
     //change solrconfig on slave
- 
2.19.1.windows.1

