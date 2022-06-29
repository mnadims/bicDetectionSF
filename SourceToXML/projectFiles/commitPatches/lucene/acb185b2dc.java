From acb185b2dc7522e6a4fa55d54e82910736668f8d Mon Sep 17 00:00:00 2001
From: Andrzej Bialecki <ab@apache.org>
Date: Mon, 6 Mar 2017 11:09:59 +0100
Subject: [PATCH] SOLR-9999: Instrument DirectUpdateHandler2.

--
 solr/CHANGES.txt                              |   3 +
 .../java/org/apache/solr/core/SolrCore.java   |   3 +
 .../solr/update/DirectUpdateHandler2.java     | 128 ++++++++++++------
 .../solr/update/DirectUpdateHandlerTest.java  | 108 ++++++++++++---
 4 files changed, 185 insertions(+), 57 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 0b0574902fb..4cfcb722d87 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -151,6 +151,9 @@ New Features
 
 * SOLR-10146: Added button to the Admin UI 'Collection' tab for deleting an inactive shard (Amrit Sarkar, janhoy)
 
* SOLR-9999: Instrument DirectUpdateHandler2. This registers existing statistics under metrics API and adds
  more metrics to track the rates of update and delete commands. (ab)

 Bug Fixes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index 1c30e4c1be8..f22c4722222 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -1072,6 +1072,9 @@ public final class SolrCore implements SolrInfoMBean, Closeable {
     } else {
       newUpdateHandler = createUpdateHandler(updateHandlerClass, updateHandler);
     }
    if (newUpdateHandler instanceof SolrMetricProducer) {
      coreMetricManager.registerMetricProducer("updateHandler", (SolrMetricProducer)newUpdateHandler);
    }
     infoRegistry.put("updateHandler", newUpdateHandler);
     return newUpdateHandler;
   }
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index ebff564fbb5..4592bcf980b 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -26,6 +26,8 @@ import java.util.concurrent.ExecutionException;
 import java.util.concurrent.Future;
 import java.util.concurrent.atomic.LongAdder;
 
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.CodecReader;
@@ -49,6 +51,8 @@ import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.SolrConfig.UpdateHandlerInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestInfo;
@@ -71,24 +75,25 @@ import org.slf4j.LoggerFactory;
  * <p>
  * TODO: add soft commitWithin support
  */
public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState.IndexWriterCloser {
public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState.IndexWriterCloser, SolrMetricProducer {
   protected final SolrCoreState solrCoreState;
 
   // stats
   LongAdder addCommands = new LongAdder();
  LongAdder addCommandsCumulative = new LongAdder();
  Meter addCommandsCumulative;
   LongAdder deleteByIdCommands= new LongAdder();
  LongAdder deleteByIdCommandsCumulative= new LongAdder();
  LongAdder deleteByQueryCommands= new LongAdder();
  LongAdder deleteByQueryCommandsCumulative= new LongAdder();
  LongAdder expungeDeleteCommands = new LongAdder();
  LongAdder mergeIndexesCommands = new LongAdder();
  LongAdder commitCommands= new LongAdder();
  LongAdder optimizeCommands= new LongAdder();
  LongAdder rollbackCommands= new LongAdder();
  LongAdder numDocsPending= new LongAdder();
  Meter deleteByIdCommandsCumulative;
  LongAdder deleteByQueryCommands = new LongAdder();
  Meter deleteByQueryCommandsCumulative;
  Meter expungeDeleteCommands;
  Meter mergeIndexesCommands;
  Meter commitCommands;
  Meter splitCommands;
  Meter optimizeCommands;
  Meter rollbackCommands;
  LongAdder numDocsPending = new LongAdder();
   LongAdder numErrors = new LongAdder();
  LongAdder numErrorsCumulative = new LongAdder();
  Meter numErrorsCumulative;
 
   // tracks when auto-commit should occur
   protected final CommitTracker commitTracker;
@@ -146,6 +151,35 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     }
   }
 
  @Override
  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    commitCommands = manager.meter(registry, "commits", getCategory().toString(), scope);
    Gauge<Integer> autoCommits = () -> commitTracker.getCommitCount();
    manager.register(registry, autoCommits, true, "autoCommits", getCategory().toString(), scope);
    Gauge<Integer> softAutoCommits = () -> softCommitTracker.getCommitCount();
    manager.register(registry, softAutoCommits, true, "softAutoCommits", getCategory().toString(), scope);
    optimizeCommands = manager.meter(registry, "optimizes", getCategory().toString(), scope);
    rollbackCommands = manager.meter(registry, "rollbacks", getCategory().toString(), scope);
    splitCommands = manager.meter(registry, "splits", getCategory().toString(), scope);
    mergeIndexesCommands = manager.meter(registry, "merges", getCategory().toString(), scope);
    expungeDeleteCommands = manager.meter(registry, "expungeDeletes", getCategory().toString(), scope);
    Gauge<Long> docsPending = () -> numDocsPending.longValue();
    manager.register(registry, docsPending, true, "docsPending", getCategory().toString(), scope);
    Gauge<Long> adds = () -> addCommands.longValue();
    manager.register(registry, adds, true, "adds", getCategory().toString(), scope);
    Gauge<Long> deletesById = () -> deleteByIdCommands.longValue();
    manager.register(registry, deletesById, true, "deletesById", getCategory().toString(), scope);
    Gauge<Long> deletesByQuery = () -> deleteByQueryCommands.longValue();
    manager.register(registry, deletesByQuery, true, "deletesByQuery", getCategory().toString(), scope);
    Gauge<Long> errors = () -> numErrors.longValue();
    manager.register(registry, errors, true, "errors", getCategory().toString(), scope);

    addCommandsCumulative = manager.meter(registry, "cumulativeAdds", getCategory().toString(), scope);
    deleteByIdCommandsCumulative = manager.meter(registry, "cumulativeDeletesById", getCategory().toString(), scope);
    deleteByQueryCommandsCumulative = manager.meter(registry, "cumulativeDeletesByQuery", getCategory().toString(), scope);
    numErrorsCumulative = manager.meter(registry, "cumulativeErrors", getCategory().toString(), scope);
  }

   private void deleteAll() throws IOException {
     log.info(core.getLogId() + "REMOVING ALL DOCUMENTS FROM INDEX");
     RefCounted<IndexWriter> iw = solrCoreState.getIndexWriter(core);
@@ -192,7 +226,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     int rc = -1;
 
     addCommands.increment();
    addCommandsCumulative.increment();
    addCommandsCumulative.mark();
 
     // if there is no ID field, don't overwrite
     if (idField == null) {
@@ -230,7 +264,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     } finally {
       if (rc != 1) {
         numErrors.increment();
        numErrorsCumulative.increment();
        numErrorsCumulative.mark();
       } else {
         numDocsPending.increment();
       }
@@ -368,7 +402,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   @Override
   public void delete(DeleteUpdateCommand cmd) throws IOException {
     deleteByIdCommands.increment();
    deleteByIdCommandsCumulative.increment();
    deleteByIdCommandsCumulative.mark();
 
     Term deleteTerm = new Term(idField.getName(), cmd.getIndexedId());
     // SolrCore.verbose("deleteDocuments",deleteTerm,writer);
@@ -426,7 +460,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   @Override
   public void deleteByQuery(DeleteUpdateCommand cmd) throws IOException {
     deleteByQueryCommands.increment();
    deleteByQueryCommandsCumulative.increment();
    deleteByQueryCommandsCumulative.mark();
     boolean madeIt=false;
     try {
       Query q = getQuery(cmd);
@@ -478,7 +512,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     } finally {
       if (!madeIt) {
         numErrors.increment();
        numErrorsCumulative.increment();
        numErrorsCumulative.mark();
       }
     }
   }
@@ -486,7 +520,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
 
   @Override
   public int mergeIndexes(MergeIndexesCommand cmd) throws IOException {
    mergeIndexesCommands.increment();
    mergeIndexesCommands.mark();
     int rc;
 
     log.info("start " + cmd);
@@ -540,7 +574,10 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
       error=false;
     }
     finally {
      if (error) numErrors.increment();
      if (error) {
        numErrors.increment();
        numErrorsCumulative.mark();
      }
     }
   }
 
@@ -552,10 +589,10 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     }
 
     if (cmd.optimize) {
      optimizeCommands.increment();
      optimizeCommands.mark();
     } else {
      commitCommands.increment();
      if (cmd.expungeDeletes) expungeDeleteCommands.increment();
      commitCommands.mark();
      if (cmd.expungeDeletes) expungeDeleteCommands.mark();
     }
 
     Future[] waitSearcher = null;
@@ -674,7 +711,10 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
       addCommands.reset();
       deleteByIdCommands.reset();
       deleteByQueryCommands.reset();
      if (error) numErrors.increment();
      if (error) {
        numErrors.increment();
        numErrorsCumulative.mark();
      }
     }
 
     // if we are supposed to wait for the searcher to be registered, then we should do it
@@ -702,7 +742,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
       throw new UnsupportedOperationException("Rollback is currently not supported in SolrCloud mode. (SOLR-4895)");
     }
 
    rollbackCommands.increment();
    rollbackCommands.mark();
 
     boolean error=true;
 
@@ -722,10 +762,13 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
       error=false;
     }
     finally {
      addCommandsCumulative.add(-addCommands.sumThenReset());
      deleteByIdCommandsCumulative.add(-deleteByIdCommands.sumThenReset());
      deleteByQueryCommandsCumulative.add(-deleteByQueryCommands.sumThenReset());
      if (error) numErrors.increment();
      addCommandsCumulative.mark(-addCommands.sumThenReset());
      deleteByIdCommandsCumulative.mark(-deleteByIdCommands.sumThenReset());
      deleteByQueryCommandsCumulative.mark(-deleteByQueryCommands.sumThenReset());
      if (error) {
        numErrors.increment();
        numErrorsCumulative.mark();
      }
     }
   }
 
@@ -834,7 +877,13 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   public void split(SplitIndexCommand cmd) throws IOException {
     commit(new CommitUpdateCommand(cmd.req, false));
     SolrIndexSplitter splitter = new SolrIndexSplitter(cmd);
    splitter.split();
    splitCommands.mark();
    try {
      splitter.split();
    } catch (IOException e) {
      numErrors.increment();
      numErrorsCumulative.mark();
    }
   }
 
   /**
@@ -873,10 +922,10 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   }
 
   private void updateDocument(AddUpdateCommand cmd, IndexWriter writer, Term updateTerm) throws IOException {
    if(cmd.isBlock()){
    if (cmd.isBlock()) {
       log.debug("updateDocuments({})", cmd);
       writer.updateDocuments(updateTerm, cmd);
    }else{
    } else {
       Document luceneDocument = cmd.getLuceneDocument(false);
       log.debug("updateDocument({})", cmd);
       writer.updateDocument(updateTerm, luceneDocument);
@@ -916,7 +965,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   @Override
   public NamedList getStatistics() {
     NamedList lst = new SimpleOrderedMap();
    lst.add("commits", commitCommands.longValue());
    lst.add("commits", commitCommands.getCount());
     if (commitTracker.getDocsUpperBound() > 0) {
       lst.add("autocommit maxDocs", commitTracker.getDocsUpperBound());
     }
@@ -931,9 +980,9 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
       lst.add("soft autocommit maxTime", "" + softCommitTracker.getTimeUpperBound() + "ms");
     }
     lst.add("soft autocommits", softCommitTracker.getCommitCount());
    lst.add("optimizes", optimizeCommands.longValue());
    lst.add("rollbacks", rollbackCommands.longValue());
    lst.add("expungeDeletes", expungeDeleteCommands.longValue());
    lst.add("optimizes", optimizeCommands.getCount());
    lst.add("rollbacks", rollbackCommands.getCount());
    lst.add("expungeDeletes", expungeDeleteCommands.getCount());
     lst.add("docsPending", numDocsPending.longValue());
     // pset.size() not synchronized, but it should be fine to access.
     // lst.add("deletesPending", pset.size());
@@ -941,10 +990,10 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     lst.add("deletesById", deleteByIdCommands.longValue());
     lst.add("deletesByQuery", deleteByQueryCommands.longValue());
     lst.add("errors", numErrors.longValue());
    lst.add("cumulative_adds", addCommandsCumulative.longValue());
    lst.add("cumulative_deletesById", deleteByIdCommandsCumulative.longValue());
    lst.add("cumulative_deletesByQuery", deleteByQueryCommandsCumulative.longValue());
    lst.add("cumulative_errors", numErrorsCumulative.longValue());
    lst.add("cumulative_adds", addCommandsCumulative.getCount());
    lst.add("cumulative_deletesById", deleteByIdCommandsCumulative.getCount());
    lst.add("cumulative_deletesByQuery", deleteByQueryCommandsCumulative.getCount());
    lst.add("cumulative_errors", numErrorsCumulative.getCount());
     if (this.ulog != null) {
       lst.add("transaction_logs_total_size", ulog.getTotalLogsSize());
       lst.add("transaction_logs_total_number", ulog.getTotalLogsNumber());
@@ -971,4 +1020,5 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
   public CommitTracker getSoftCommitTracker() {
     return softCommitTracker;
   }

 }
diff --git a/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java b/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
index ca604fed5f8..281635435e4 100644
-- a/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerTest.java
@@ -23,6 +23,9 @@ import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
 import org.apache.lucene.index.TieredMergePolicy;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.store.Directory;
@@ -99,7 +102,29 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testBasics() throws Exception {
    

    // get initial metrics
    Map<String, Metric> metrics = h.getCoreContainer().getMetricManager()
        .registry(h.getCore().getCoreMetricManager().getRegistryName()).getMetrics();

    String PREFIX = "UPDATE.updateHandler.";

    String commitsName = PREFIX + "commits";
    assertTrue(metrics.containsKey(commitsName));
    String addsName = PREFIX + "adds";
    assertTrue(metrics.containsKey(addsName));
    String cumulativeAddsName = PREFIX + "cumulativeAdds";
    String delsIName = PREFIX + "deletesById";
    String cumulativeDelsIName = PREFIX + "cumulativeDeletesById";
    String delsQName = PREFIX + "deletesByQuery";
    String cumulativeDelsQName = PREFIX + "cumulativeDeletesByQuery";
    long commits = ((Meter) metrics.get(commitsName)).getCount();
    long adds = ((Gauge<Long>) metrics.get(addsName)).getValue();
    long cumulativeAdds = ((Meter) metrics.get(cumulativeAddsName)).getCount();
    long cumulativeDelsI = ((Meter) metrics.get(cumulativeDelsIName)).getCount();
    long cumulativeDelsQ = ((Meter) metrics.get(cumulativeDelsQName)).getCount();


     assertNull("This test requires a schema that has no version field, " +
                "it appears the schema file in use has been edited to violate " +
                "this requirement",
@@ -112,8 +137,23 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     assertQ(req("q","id:5"), "//*[@numFound='0']");
     assertQ(req("q","id:6"), "//*[@numFound='0']");
 
    long newAdds = ((Gauge<Long>) metrics.get(addsName)).getValue();
    long newCumulativeAdds = ((Meter) metrics.get(cumulativeAddsName)).getCount();
    assertEquals("new adds", 2, newAdds - adds);
    assertEquals("new cumulative adds", 2, newCumulativeAdds - cumulativeAdds);

     assertU(commit());
 
    long newCommits = ((Meter) metrics.get(commitsName)).getCount();
    assertEquals("new commits", 1, newCommits - commits);

    newAdds = ((Gauge<Long>) metrics.get(addsName)).getValue();
    newCumulativeAdds = ((Meter) metrics.get(cumulativeAddsName)).getCount();
    // adds should be reset to 0 after commit
    assertEquals("new adds after commit", 0, newAdds);
    // not so with cumulative ones!
    assertEquals("new cumulative adds after commit", 2, newCumulativeAdds - cumulativeAdds);

     // now they should be there
     assertQ(req("q","id:5"), "//*[@numFound='1']");
     assertQ(req("q","id:6"), "//*[@numFound='1']");
@@ -121,11 +161,21 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     // now delete one
     assertU(delI("5"));
 
    long newDelsI = ((Gauge<Long>) metrics.get(delsIName)).getValue();
    long newCumulativeDelsI = ((Meter) metrics.get(cumulativeDelsIName)).getCount();
    assertEquals("new delsI", 1, newDelsI);
    assertEquals("new cumulative delsI", 1, newCumulativeDelsI - cumulativeDelsI);

     // not committed yet
     assertQ(req("q","id:5"), "//*[@numFound='1']");
 
     assertU(commit());
    
    // delsI should be reset to 0 after commit
    newDelsI = ((Gauge<Long>) metrics.get(delsIName)).getValue();
    newCumulativeDelsI = ((Meter) metrics.get(cumulativeDelsIName)).getCount();
    assertEquals("new delsI after commit", 0, newDelsI);
    assertEquals("new cumulative delsI after commit", 1, newCumulativeDelsI - cumulativeDelsI);

     // 5 should be gone
     assertQ(req("q","id:5"), "//*[@numFound='0']");
     assertQ(req("q","id:6"), "//*[@numFound='1']");
@@ -133,14 +183,36 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     // now delete all
     assertU(delQ("*:*"));
 
    long newDelsQ = ((Gauge<Long>) metrics.get(delsQName)).getValue();
    long newCumulativeDelsQ = ((Meter) metrics.get(cumulativeDelsQName)).getCount();
    assertEquals("new delsQ", 1, newDelsQ);
    assertEquals("new cumulative delsQ", 1, newCumulativeDelsQ - cumulativeDelsQ);

     // not committed yet
     assertQ(req("q","id:6"), "//*[@numFound='1']");
 
     assertU(commit());
 
    newDelsQ = ((Gauge<Long>) metrics.get(delsQName)).getValue();
    newCumulativeDelsQ = ((Meter) metrics.get(cumulativeDelsQName)).getCount();
    assertEquals("new delsQ after commit", 0, newDelsQ);
    assertEquals("new cumulative delsQ after commit", 1, newCumulativeDelsQ - cumulativeDelsQ);

     // 6 should be gone
     assertQ(req("q","id:6"), "//*[@numFound='0']");
 
    // verify final metrics
    newCommits = ((Meter) metrics.get(commitsName)).getCount();
    assertEquals("new commits", 3, newCommits - commits);
    newAdds = ((Gauge<Long>) metrics.get(addsName)).getValue();
    assertEquals("new adds", 0, newAdds);
    newCumulativeAdds = ((Meter) metrics.get(cumulativeAddsName)).getCount();
    assertEquals("new cumulative adds", 2, newCumulativeAdds - cumulativeAdds);
    newDelsI = ((Gauge<Long>) metrics.get(delsIName)).getValue();
    assertEquals("new delsI", 0, newDelsI);
    newCumulativeDelsI = ((Meter) metrics.get(cumulativeDelsIName)).getCount();
    assertEquals("new cumulative delsI", 1, newCumulativeDelsI - cumulativeDelsI);

   }
 
 
@@ -161,12 +233,12 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     CommitUpdateCommand cmtCmd = new CommitUpdateCommand(ureq, false);
     cmtCmd.waitSearcher = true;
     assertEquals( 1, duh2.addCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.longValue() );
    assertEquals( 0, duh2.commitCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.getCount() );
    assertEquals( 0, duh2.commitCommands.getCount() );
     updater.commit(cmtCmd);
     assertEquals( 0, duh2.addCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.longValue() );
    assertEquals( 1, duh2.commitCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.getCount() );
    assertEquals( 1, duh2.commitCommands.getCount() );
     ureq.close();
 
     assertU(adoc("id","B"));
@@ -175,12 +247,12 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     ureq = req();
     RollbackUpdateCommand rbkCmd = new RollbackUpdateCommand(ureq);
     assertEquals( 1, duh2.addCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.longValue() );
    assertEquals( 0, duh2.rollbackCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.getCount() );
    assertEquals( 0, duh2.rollbackCommands.getCount() );
     updater.rollback(rbkCmd);
     assertEquals( 0, duh2.addCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.longValue() );
    assertEquals( 1, duh2.rollbackCommands.longValue() );
    assertEquals( 1, duh2.addCommandsCumulative.getCount() );
    assertEquals( 1, duh2.rollbackCommands.getCount() );
     ureq.close();
     
     // search - "B" should not be found.
@@ -221,12 +293,12 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     CommitUpdateCommand cmtCmd = new CommitUpdateCommand(ureq, false);
     cmtCmd.waitSearcher = true;
     assertEquals( 2, duh2.addCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.longValue() );
    assertEquals( 0, duh2.commitCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.getCount() );
    assertEquals( 0, duh2.commitCommands.getCount() );
     updater.commit(cmtCmd);
     assertEquals( 0, duh2.addCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.longValue() );
    assertEquals( 1, duh2.commitCommands.longValue() );
    assertEquals( 2, duh2.addCommandsCumulative.getCount() );
    assertEquals( 1, duh2.commitCommands.getCount() );
     ureq.close();
 
     // search - "A","B" should be found.
@@ -254,13 +326,13 @@ public class DirectUpdateHandlerTest extends SolrTestCaseJ4 {
     ureq = req();
     RollbackUpdateCommand rbkCmd = new RollbackUpdateCommand(ureq);
     assertEquals( 1, duh2.deleteByIdCommands.longValue() );
    assertEquals( 1, duh2.deleteByIdCommandsCumulative.longValue() );
    assertEquals( 0, duh2.rollbackCommands.longValue() );
    assertEquals( 1, duh2.deleteByIdCommandsCumulative.getCount() );
    assertEquals( 0, duh2.rollbackCommands.getCount() );
     updater.rollback(rbkCmd);
     ureq.close();
     assertEquals( 0, duh2.deleteByIdCommands.longValue() );
    assertEquals( 0, duh2.deleteByIdCommandsCumulative.longValue() );
    assertEquals( 1, duh2.rollbackCommands.longValue() );
    assertEquals( 0, duh2.deleteByIdCommandsCumulative.getCount() );
    assertEquals( 1, duh2.rollbackCommands.getCount() );
     
     // search - "B" should be found.
     assertQ("\"B\" should be found.", req
- 
2.19.1.windows.1

