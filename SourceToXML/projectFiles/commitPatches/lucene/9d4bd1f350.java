From 9d4bd1f35096ba8953f899abb890121c063dcc3f Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Tue, 2 Dec 2014 14:04:35 +0000
Subject: [PATCH] SOLR-6796: distrib.singlePass does not return correct set of
 fields for multi-fl-parameter requests

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1642873 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  3 +
 .../handler/component/QueryComponent.java     | 57 +++++++------------
 ...ributedQueryComponentOptimizationTest.java |  4 ++
 3 files changed, 26 insertions(+), 38 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 8e284ea86ff..63f484525c7 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -506,6 +506,9 @@ Bug Fixes
 * SOLR-6795: distrib.singlePass returns score even though not asked for.
   (Per Steffensen via shalin)
 
* SOLR-6796: distrib.singlePass does not return correct set of fields for multi-fl-parameter
  requests. (Per Steffensen via shalin)

 Other Changes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
index 5d1b8dfe720..3e19b0f88cc 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -30,7 +30,6 @@ import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
import java.util.regex.Pattern;
 
 import org.apache.lucene.index.LeafReaderContext;
 import org.apache.lucene.index.IndexReaderContext;
@@ -839,51 +838,33 @@ public class QueryComponent extends SearchComponent
 
     boolean shardQueryIncludeScore = (rb.getFieldFlags() & SolrIndexSearcher.GET_SCORES) != 0 || rb.getSortSpec().includesScore();
     if (distribSinglePass) {
      String fl = rb.req.getParams().get(CommonParams.FL);
      if (fl == null) {
        if (fields.getRequestedFieldNames() == null && fields.wantsAllFields()) {
          fl = "*";
        } else  {
          fl = "";
          for (String s : fields.getRequestedFieldNames()) {
            fl += s + ",";
          }
        }
      }
      if (!fields.wantsField(keyFieldName))  {
        // the user has not requested the unique key but
        // we still need to add it otherwise mergeIds can't work
        if (fl.endsWith(",")) {
          fl += keyFieldName;
        } else  {
          fl += "," + keyFieldName;
        }
      }
      sreq.params.set(CommonParams.FL, updateFl(fl, shardQueryIncludeScore));
    } else {
      // in this first phase, request only the unique key field and any fields needed for merging.
      if (shardQueryIncludeScore) {
        sreq.params.set(CommonParams.FL, keyFieldName + ",score");
      String[] fls = rb.req.getParams().getParams(CommonParams.FL);
      if (fls != null && fls.length > 0 && (fls.length != 1 || !fls[0].isEmpty())) {
        // If the outer request contains actual FL's use them...
        sreq.params.set(CommonParams.FL, fls);
       } else {
        sreq.params.set(CommonParams.FL, keyFieldName);
        // ... else we need to explicitly ask for all fields, because we are going to add
        // additional fields below
        sreq.params.set(CommonParams.FL, "*");
       }
     }
    StringBuilder additionalFL = new StringBuilder();
    boolean additionalAdded = false;
    if (!distribSinglePass || !fields.wantsField(keyFieldName)) 
      additionalAdded = addFL(additionalFL, keyFieldName, additionalAdded);
    if ((!distribSinglePass || !fields.wantsScore()) && shardQueryIncludeScore) 
      additionalAdded = addFL(additionalFL, "score", additionalAdded);
    if (additionalAdded) sreq.params.add(CommonParams.FL, additionalFL.toString());
 
     rb.addRequest(this, sreq);
   }


  String updateFl(String originalFields, boolean includeScoreIfMissing) {
    if (includeScoreIfMissing && !scorePattern.matcher(originalFields).find()) {
      return originalFields + ",score";
    } else {
      return originalFields;
    }
  
  private boolean addFL(StringBuilder fl, String field, boolean additionalAdded) {
    if (additionalAdded) fl.append(",");
    fl.append(field);
    return true;
   }
 
  private static final Pattern scorePattern = Pattern.compile("\\bscore\\b");


   private void mergeIds(ResponseBuilder rb, ShardRequest sreq) {
       List<MergeStrategy> mergeStrategies = rb.getMergeStrategies();
       if(mergeStrategies != null) {
diff --git a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
index b796dc08b30..158a32a35fa 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/DistributedQueryComponentOptimizationTest.java
@@ -118,12 +118,16 @@ public class DistributedQueryComponentOptimizationTest extends BaseDistributedSe
     verifySinglePass("q", "id:19", "fl", "id,*a_sS", "sort", "payload asc", "distrib.singlePass", "true");
     verifySinglePass("q", "id:19", "fl", "id,dynamic,cat*", "sort", "payload asc", "distrib.singlePass", "true");
 
    // see SOLR-6795, distrib.singlePass=true would return score even when not asked for
     handle.clear();
     handle.put("timestamp", SKIPVAL);
     handle.put("_version_", SKIPVAL);
     // we don't to compare maxScore because most distributed requests return it anyway (just because they have score already)
     handle.put("maxScore", SKIPVAL);
     query("q", "{!func}id", ShardParams.DISTRIB_SINGLE_PASS, "true");

    // fix for a bug where not all fields are returned if using multiple fl parameters, see SOLR-6796
    query("q","*:*", "fl", "id", "fl","dynamic","sort","payload desc", ShardParams.DISTRIB_SINGLE_PASS, "true");
   }
 
   private void verifySinglePass(String... q) throws SolrServerException {
- 
2.19.1.windows.1

