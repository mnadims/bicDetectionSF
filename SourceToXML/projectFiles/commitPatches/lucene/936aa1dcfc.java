From 936aa1dcfc914043e90c4a0ef157374f7e3616cb Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Thu, 26 Feb 2015 06:47:52 +0000
Subject: [PATCH] SOLR-7128: Two phase distributed search is fetching extra
 fields in GET_TOP_IDS phase

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1662366 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   3 +
 .../handler/component/QueryComponent.java     |  10 +-
 ...ributedQueryComponentOptimizationTest.java | 222 ++++++++++++++----
 3 files changed, 184 insertions(+), 51 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 865f5772c70..7e40b273e18 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -153,6 +153,9 @@ Bug Fixes
 * SOLR-7113: Multiple calls to UpdateLog#init is not thread safe with respect to the
   HDFS FileSystem client object usage. (Mark Miller, Vamsee Yarlagadda)
 
* SOLR-7128: Two phase distributed search is fetching extra fields in GET_TOP_IDS phase.
  (Pablo Queixalos, shalin)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
index a260ca70303..b2f9a84e3d5 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -883,12 +883,16 @@ public class QueryComponent extends SearchComponent
     sreq.params.set(ResponseBuilder.FIELD_SORT_VALUES,"true");
 
     // TODO: should this really sendGlobalDfs if just includeScore?
    if ( (rb.getFieldFlags() & SolrIndexSearcher.GET_SCORES)!=0 || rb.getSortSpec().includesScore()) {
    boolean shardQueryIncludeScore = (rb.getFieldFlags() & SolrIndexSearcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore();
    if (shardQueryIncludeScore) {
       sreq.params.set(CommonParams.FL, rb.req.getSchema().getUniqueKeyField().getName() + ",score");
       StatsCache statsCache = rb.req.getCore().getStatsCache();
       statsCache.sendGlobalStats(rb, sreq);
    } else  {
      // reset so that only unique key is requested in shard requests
      sreq.params.set(CommonParams.FL, rb.req.getSchema().getUniqueKeyField().getName());
     }
    boolean shardQueryIncludeScore = (rb.getFieldFlags() & SolrIndexSearcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore();

     if (distribSinglePass) {
       String[] fls = rb.req.getParams().getParams(CommonParams.FL);
       if (fls != null && fls.length > 0 && (fls.length != 1 || !fls[0].isEmpty())) {
@@ -902,7 +906,7 @@ public class QueryComponent extends SearchComponent
     }
     StringBuilder additionalFL = new StringBuilder();
     boolean additionalAdded = false;
    if (!distribSinglePass || !fields.wantsField(keyFieldName)) 
    if (!distribSinglePass || !fields.wantsField(keyFieldName))
       additionalAdded = addFL(additionalFL, keyFieldName, additionalAdded);
     if ((!distribSinglePass || !fields.wantsScore()) && shardQueryIncludeScore) 
       additionalAdded = addFL(additionalFL, "score", additionalAdded);
diff --git a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
index ed328fb75e8..57733e38aa1 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
@@ -17,18 +17,20 @@ package org.apache.solr.handler.component;
  * limitations under the License.
  */
 
import org.apache.solr.BaseDistributedSearchTestCase;
import org.apache.solr.client.solrj.SolrServerException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.cloud.AbstractFullDistribZkTestBase;
import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.ShardParams;
 import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.BeforeClass;
import org.apache.solr.common.util.StrUtils;
 import org.junit.Test;
 
import java.nio.ByteBuffer;
import java.util.Map;

 /**
  * Test for QueryComponent's distributed querying optimization.
  * If the "fl" param is just "id" or just "id,score", all document data to return is already fetched by STAGE_EXECUTE_QUERY.
@@ -37,37 +39,39 @@ import java.util.Map;
  *
  * @see QueryComponent
  */
public class DistributedQueryComponentOptimizationTest extends BaseDistributedSearchTestCase {
public class DistributedQueryComponentOptimizationTest extends AbstractFullDistribZkTestBase {
 
   public DistributedQueryComponentOptimizationTest() {
     stress = 0;
    schemaString = "schema-custom-field.xml";
   }
 
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    initCore("solrconfig.xml", "schema-custom-field.xml");
  @Override
  protected String getSolrXml() {
    return "solr-trackingshardhandler.xml";
   }
 
   @Test
   @ShardsFixed(num = 3)
   public void test() throws Exception {
    waitForThingsToLevelOut(30);
     del("*:*");
 
    index(id, "1", "text", "a", "test_sS", "21", "payload", ByteBuffer.wrap(new byte[] { 0x12, 0x62, 0x15 }),                     //  2
          // quick check to prove "*" dynamicField hasn't been broken by somebody mucking with schema
          "asdfasdf_field_should_match_catchall_dynamic_field_adsfasdf", "value");
    index(id, "2", "text", "b", "test_sS", "22", "payload", ByteBuffer.wrap(new byte[] { 0x25, 0x21, 0x16 }));                    //  5
    index(id, "3", "text", "a", "test_sS", "23", "payload", ByteBuffer.wrap(new byte[] { 0x35, 0x32, 0x58 }));                    //  8
    index(id, "4", "text", "b", "test_sS", "24", "payload", ByteBuffer.wrap(new byte[] { 0x25, 0x21, 0x15 }));                    //  4
    index(id, "5", "text", "a", "test_sS", "25", "payload", ByteBuffer.wrap(new byte[] { 0x35, 0x35, 0x10, 0x00 }));              //  9
    index(id, "6", "text", "c", "test_sS", "26", "payload", ByteBuffer.wrap(new byte[] { 0x1a, 0x2b, 0x3c, 0x00, 0x00, 0x03 }));  //  3
    index(id, "7", "text", "c", "test_sS", "27", "payload", ByteBuffer.wrap(new byte[] { 0x00, 0x3c, 0x73 }));                    //  1
    index(id, "8", "text", "c", "test_sS", "28", "payload", ByteBuffer.wrap(new byte[] { 0x59, 0x2d, 0x4d }));                    // 11
    index(id, "9", "text", "a", "test_sS", "29", "payload", ByteBuffer.wrap(new byte[] { 0x39, 0x79, 0x7a }));                    // 10
    index(id, "10", "text", "b", "test_sS", "30", "payload", ByteBuffer.wrap(new byte[] { 0x31, 0x39, 0x7c }));                   //  6
    index(id, "11", "text", "d", "test_sS", "31", "payload", ByteBuffer.wrap(new byte[] { (byte)0xff, (byte)0xaf, (byte)0x9c })); // 13
    index(id, "12", "text", "d", "test_sS", "32", "payload", ByteBuffer.wrap(new byte[] { 0x34, (byte)0xdd, 0x4d }));             //  7
    index(id, "13", "text", "d", "test_sS", "33", "payload", ByteBuffer.wrap(new byte[] { (byte)0x80, 0x11, 0x33 }));             // 12
    index(id, "1", "text", "a", "test_sS", "21", "payload", ByteBuffer.wrap(new byte[]{0x12, 0x62, 0x15}),                     //  2
        // quick check to prove "*" dynamicField hasn't been broken by somebody mucking with schema
        "asdfasdf_field_should_match_catchall_dynamic_field_adsfasdf", "value");
    index(id, "2", "text", "b", "test_sS", "22", "payload", ByteBuffer.wrap(new byte[]{0x25, 0x21, 0x16}));                    //  5
    index(id, "3", "text", "a", "test_sS", "23", "payload", ByteBuffer.wrap(new byte[]{0x35, 0x32, 0x58}));                    //  8
    index(id, "4", "text", "b", "test_sS", "24", "payload", ByteBuffer.wrap(new byte[]{0x25, 0x21, 0x15}));                    //  4
    index(id, "5", "text", "a", "test_sS", "25", "payload", ByteBuffer.wrap(new byte[]{0x35, 0x35, 0x10, 0x00}));              //  9
    index(id, "6", "text", "c", "test_sS", "26", "payload", ByteBuffer.wrap(new byte[]{0x1a, 0x2b, 0x3c, 0x00, 0x00, 0x03}));  //  3
    index(id, "7", "text", "c", "test_sS", "27", "payload", ByteBuffer.wrap(new byte[]{0x00, 0x3c, 0x73}));                    //  1
    index(id, "8", "text", "c", "test_sS", "28", "payload", ByteBuffer.wrap(new byte[]{0x59, 0x2d, 0x4d}));                    // 11
    index(id, "9", "text", "a", "test_sS", "29", "payload", ByteBuffer.wrap(new byte[]{0x39, 0x79, 0x7a}));                    // 10
    index(id, "10", "text", "b", "test_sS", "30", "payload", ByteBuffer.wrap(new byte[]{0x31, 0x39, 0x7c}));                   //  6
    index(id, "11", "text", "d", "test_sS", "31", "payload", ByteBuffer.wrap(new byte[]{(byte) 0xff, (byte) 0xaf, (byte) 0x9c})); // 13
    index(id, "12", "text", "d", "test_sS", "32", "payload", ByteBuffer.wrap(new byte[]{0x34, (byte) 0xdd, 0x4d}));             //  7
    index(id, "13", "text", "d", "test_sS", "33", "payload", ByteBuffer.wrap(new byte[]{(byte) 0x80, 0x11, 0x33}));             // 12
     commit();
 
     QueryResponse rsp;
@@ -95,27 +99,27 @@ public class DistributedQueryComponentOptimizationTest extends BaseDistributedSe
     compareResponses(rsp, nonDistribRsp); // make sure distrib and distrib.singlePass return the same thing
 
     // verify that the optimization actually works
    verifySinglePass("q", "*:*", "fl", "id", "sort", "payload desc", "rows", "20"); // id only is optimized by default
    verifySinglePass("q", "*:*", "fl", "id,score", "sort", "payload desc", "rows", "20"); // id,score only is optimized by default
    verifySinglePass("q", "*:*", "fl", "score", "sort", "payload asc", "rows", "20", "distrib.singlePass", "true");
    queryWithAsserts("q", "*:*", "fl", "id", "sort", "payload desc", "rows", "20"); // id only is optimized by default
    queryWithAsserts("q", "*:*", "fl", "id,score", "sort", "payload desc", "rows", "20"); // id,score only is optimized by default
    queryWithAsserts("q", "*:*", "fl", "score", "sort", "payload asc", "rows", "20", "distrib.singlePass", "true");
 
     // SOLR-6545, wild card field list
    index(id, "19", "text", "d", "cat_a_sS", "1" ,"dynamic", "2", "payload", ByteBuffer.wrap(new byte[] { (byte)0x80, 0x11, 0x33 }));
    index(id, "19", "text", "d", "cat_a_sS", "1", "dynamic", "2", "payload", ByteBuffer.wrap(new byte[]{(byte) 0x80, 0x11, 0x34}));
     commit();
 
    nonDistribRsp = query("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc");
    rsp = query("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc", "distrib.singlePass", "true");
    nonDistribRsp = queryWithAsserts("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc");
    rsp = queryWithAsserts("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc", "distrib.singlePass", "true");
 
     assertFieldValues(nonDistribRsp.getResults(), "id", 19);
     assertFieldValues(rsp.getResults(), "id", 19);
 
    nonDistribRsp = query("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc");
    rsp = query("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc", "distrib.singlePass", "true");
    nonDistribRsp = queryWithAsserts("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc");
    rsp = queryWithAsserts("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc", "distrib.singlePass", "true");
     assertFieldValues(nonDistribRsp.getResults(), "id", 19);
     assertFieldValues(rsp.getResults(), "id", 19);
 
    verifySinglePass("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc", "distrib.singlePass", "true");
    verifySinglePass("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc", "distrib.singlePass", "true");
    queryWithAsserts("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc", "distrib.singlePass", "true");
    queryWithAsserts("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc", "distrib.singlePass", "true");
 
     // see SOLR-6795, distrib.singlePass=true would return score even when not asked for
     handle.clear();
@@ -123,24 +127,146 @@ public class DistributedQueryComponentOptimizationTest extends BaseDistributedSe
     handle.put("_version_", SKIPVAL);
     // we don't to compare maxScore because most distributed requests return it anyway (just because they have score already)
     handle.put("maxScore", SKIPVAL);
    // this trips the queryWithAsserts function because it uses a custom parser, so just query directly
     query("q", "{!func}id", ShardParams.DISTRIB_SINGLE_PASS, "true");
 
     // fix for a bug where not all fields are returned if using multiple fl parameters, see SOLR-6796
    query("q","*:*", "fl", "id", "fl","dynamic","sort","payload desc", ShardParams.DISTRIB_SINGLE_PASS, "true");
    queryWithAsserts("q", "*:*", "fl", "id", "fl", "dynamic", "sort", "payload desc", ShardParams.DISTRIB_SINGLE_PASS, "true");
   }
 
  private void verifySinglePass(String... q) throws SolrServerException {
    QueryResponse rsp;ModifiableSolrParams params = new ModifiableSolrParams();
  /**
   * This test now asserts that every distrib.singlePass query:
   * <ol>
   * <li>Makes exactly 'numSlices' number of shard requests</li>
   * <li>Makes no GET_FIELDS requests</li>
   * <li>Must request the unique key field from shards</li>
   * <li>Must request the score if 'fl' has score or sort by score is requested</li>
   * <li>Requests all fields that are present in 'fl' param</li>
   * </ol>
   * <p>
   * It also asserts that every regular two phase distribtued search:
   * <ol>
   * <li>Makes at most 2 * 'numSlices' number of shard requests</li>
   * <li>Must request the unique key field from shards</li>
   * <li>Must request the score if 'fl' has score or sort by score is requested</li>
   * <li>Requests no fields other than id and score in GET_TOP_IDS request</li>
   * <li>Requests exactly the fields that are present in 'fl' param in GET_FIELDS request and no others</li>
   * </ol>
   * <p>
   * and also asserts that each query which requests id or score or both behaves exactly like a single pass query
   */
  private QueryResponse queryWithAsserts(Object... q) throws Exception {
    TrackingShardHandlerFactory.RequestTrackingQueue trackingQueue = new TrackingShardHandlerFactory.RequestTrackingQueue();
    // the jettys doesn't include the control jetty which is exactly what we need here
    TrackingShardHandlerFactory.setTrackingQueue(jettys, trackingQueue);

    // let's add debug=track to such requests so we can use DebugComponent responses for assertions
    Object[] qq = new Object[q.length + 2];
    System.arraycopy(q, 0, qq, 0, q.length);
    qq[qq.length - 2] = "debug";
    qq[qq.length - 1] = "track";
    handle.put("debug", SKIPVAL);
    QueryResponse response = query(qq);

    Map<String, List<TrackingShardHandlerFactory.ShardRequestAndParams>> requests = trackingQueue.getAllRequests();
    int numRequests = getNumRequests(requests);

    boolean distribSinglePass = false;

    Set<String> fls = new HashSet<>();
    Set<String> sortFields = new HashSet<>();
     for (int i = 0; i < q.length; i += 2) {
      params.add(q[i].toString(), q[i + 1].toString());
      if (ShardParams.DISTRIB_SINGLE_PASS.equals(q[i].toString()) && Boolean.parseBoolean(q[i + 1].toString())) {
        assertTrue("distrib.singlePass=true made more requests than number of shards",
            numRequests == sliceCount);
        distribSinglePass = true;
      }
      if (CommonParams.FL.equals(q[i].toString())) {
        fls.addAll(StrUtils.splitSmart(q[i + 1].toString(), ','));
      }
      if (CommonParams.SORT.equals(q[i].toString())) {
        String val = q[i + 1].toString().trim();
        // take care of asc/desc decorators
        sortFields.addAll(StrUtils.splitSmart(StrUtils.splitSmart(val, ' ').get(0), ','));
      }
    }

    Set<String> idScoreFields = new HashSet<>(2);
    idScoreFields.add("id"); // id is always requested in GET_TOP_IDS phase
    // score is optional, requested only if sorted by score
    if (fls.contains("score") || sortFields.contains("score")) idScoreFields.add("score");

    if (idScoreFields.containsAll(fls)) {
      // if id and/or score are the only fields being requested then we implicitly turn on distribSinglePass=true
      distribSinglePass = true;
    }

    if (distribSinglePass) {
      Map<String, Object> debugMap = response.getDebugMap();
      SimpleOrderedMap<Object> track = (SimpleOrderedMap<Object>) debugMap.get("track");
      assertNotNull(track);
      assertNotNull(track.get("EXECUTE_QUERY"));
      assertNull("A single pass request should not have a GET_FIELDS phase", track.get("GET_FIELDS"));

      // all fields should be requested in one go but even if 'id' is not requested by user
      // it must still be fetched in this phase to merge correctly
      Set<String> reqAndIdScoreFields = new HashSet<>(fls);
      reqAndIdScoreFields.addAll(idScoreFields);
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD1,
          CommonParams.FL, ShardRequest.PURPOSE_GET_TOP_IDS, reqAndIdScoreFields.toArray(new String[reqAndIdScoreFields.size()]));
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD2,
          CommonParams.FL, ShardRequest.PURPOSE_GET_TOP_IDS, reqAndIdScoreFields.toArray(new String[reqAndIdScoreFields.size()]));
    } else {
      // we are assuming there are facet refinement or distributed idf requests here
      assertTrue("distrib.singlePass=false made more requests than 2 * number of shards." +
              " Actual: " + numRequests + " but expected <= " + sliceCount * 2,
          numRequests <= sliceCount * 2);

      // only id and/or score should be requested
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD1,
          CommonParams.FL, ShardRequest.PURPOSE_GET_TOP_IDS, idScoreFields.toArray(new String[idScoreFields.size()]));
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD2,
          CommonParams.FL, ShardRequest.PURPOSE_GET_TOP_IDS, idScoreFields.toArray(new String[idScoreFields.size()]));

      // only originally requested fields must be requested in GET_FIELDS request
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD1,
          CommonParams.FL, ShardRequest.PURPOSE_GET_FIELDS, fls.toArray(new String[fls.size()]));
      assertParamsEquals(trackingQueue, DEFAULT_COLLECTION, SHARD2,
          CommonParams.FL, ShardRequest.PURPOSE_GET_FIELDS, fls.toArray(new String[fls.size()]));
    }

    return response;
  }

  private int getNumRequests(Map<String, List<TrackingShardHandlerFactory.ShardRequestAndParams>> requests) {
    int beforeNumRequests = 0;
    for (Map.Entry<String, List<TrackingShardHandlerFactory.ShardRequestAndParams>> entry : requests.entrySet()) {
      beforeNumRequests += entry.getValue().size();
    }
    return beforeNumRequests;
  }

  private void assertParamsEquals(TrackingShardHandlerFactory.RequestTrackingQueue trackingQueue, String collection, String shard, String paramName, int purpose, String... values) {
    TrackingShardHandlerFactory.ShardRequestAndParams getByIdRequest = trackingQueue.getShardRequestByPurpose(cloudClient.getZkStateReader(), collection, shard, purpose);
    assertParamsEquals(getByIdRequest, paramName, values);
  }

  private void assertParamsEquals(TrackingShardHandlerFactory.ShardRequestAndParams requestAndParams, String paramName, String... values) {
    if (requestAndParams == null) return;
    int expectedCount = values.length;
    String[] params = requestAndParams.params.getParams(paramName);
    if (expectedCount > 0 && (params == null || params.length == 0)) {
      fail("Expected non-zero number of '" + paramName + "' parameters in request");
    }
    Set<String> requestedFields = new HashSet<>();
    for (String p : params) {
      requestedFields.addAll(StrUtils.splitSmart(p, ','));
    }
    assertEquals("Number of requested fields do not match with expectations", expectedCount, requestedFields.size());
    for (String field : values) {
      if (!requestedFields.contains(field)) {
        fail("Field " + field + " not found in param: " + paramName + " request had " + paramName + "=" + requestedFields);
      }
     }
    params.add("shards", getShardsString());
    params.add("debug", "track");
    rsp = queryServer(new ModifiableSolrParams(params));
    Map<String, Object> debugMap = rsp.getDebugMap();
    SimpleOrderedMap<Object> track = (SimpleOrderedMap<Object>) debugMap.get("track");
    assertNotNull(track);
    assertNotNull(track.get("EXECUTE_QUERY"));
    assertNull("A single pass request should not have a GET_FIELDS phase", track.get("GET_FIELDS"));
   }
 }
- 
2.19.1.windows.1

