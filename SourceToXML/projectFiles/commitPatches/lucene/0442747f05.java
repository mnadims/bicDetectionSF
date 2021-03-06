From 0442747f05d870684689be971d58d39b9c4dd526 Mon Sep 17 00:00:00 2001
From: David Wayne Smiley <dsmiley@apache.org>
Date: Thu, 26 Nov 2015 04:56:47 +0000
Subject: [PATCH] LUCENE-6900: Grouping sortWithinGroup shouldn't be null; use
 Sort.RELEVANCE. Enhanced related Solr side a bit.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1716569 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   6 +-
 .../AbstractSecondPassGroupingCollector.java  |  21 ++-
 .../grouping/BlockGroupingCollector.java      |  10 +-
 .../search/grouping/GroupingSearch.java       |   2 +-
 .../lucene/search/grouping/TopGroups.java     |  10 +-
 .../term/TermSecondPassGroupingCollector.java |   7 +-
 .../lucene/search/grouping/TestGrouping.java  |   2 +-
 solr/CHANGES.txt                              |  10 +-
 .../java/org/apache/solr/search/Grouping.java |  42 +++---
 .../SearchGroupShardResponseProcessor.java    |   6 +-
 .../TopGroupsShardResponseProcessor.java      |   5 +-
 .../TopGroupsResultTransformer.java           | 120 ++++++++----------
 .../apache/solr/TestDistributedGrouping.java  |  10 +-
 .../solr/BaseDistributedSearchTestCase.java   |   5 +-
 14 files changed, 129 insertions(+), 127 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 1bcf2783c08..4609a81d9fa 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -104,7 +104,11 @@ Changes in Runtime Behavior
   (Robert Muir, Mike McCandless)
 
 ======================= Lucene 5.5.0 =======================
(No Changes)

API Changes

* LUCENE-6900: Grouping sortWithinGroup variables used to allow null to mean
  Sort.RELEVANCE.  Null is no longer permitted.  (David Smiley)
 
 ======================= Lucene 5.4.0 =======================
 
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
index 0634ee14ec4..ea3812c30ff 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
@@ -24,6 +24,7 @@ import java.io.IOException;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
import java.util.Objects;
 
 /**
  * SecondPassGroupingCollector is the second of two passes
@@ -54,29 +55,27 @@ public abstract class AbstractSecondPassGroupingCollector<GROUP_VALUE_TYPE> exte
     throws IOException {
 
     //System.out.println("SP init");
    if (groups.size() == 0) {
      throw new IllegalArgumentException("no groups to collect (groups.size() is 0)");
    if (groups.isEmpty()) {
      throw new IllegalArgumentException("no groups to collect (groups is empty)");
     }
 
    this.groupSort = groupSort;
    this.withinGroupSort = withinGroupSort;
    this.groups = groups;
    this.groupSort = Objects.requireNonNull(groupSort);
    this.withinGroupSort = Objects.requireNonNull(withinGroupSort);
    this.groups = Objects.requireNonNull(groups);
     this.maxDocsPerGroup = maxDocsPerGroup;
    groupMap = new HashMap<>(groups.size());
    this.groupMap = new HashMap<>(groups.size());
 
     for (SearchGroup<GROUP_VALUE_TYPE> group : groups) {
       //System.out.println("  prep group=" + (group.groupValue == null ? "null" : group.groupValue.utf8ToString()));
       final TopDocsCollector<?> collector;
      if (withinGroupSort == null) {
      if (withinGroupSort.equals(Sort.RELEVANCE)) { // optimize to use TopScoreDocCollector
         // Sort by score
         collector = TopScoreDocCollector.create(maxDocsPerGroup);
       } else {
         // Sort by fields
         collector = TopFieldCollector.create(withinGroupSort, maxDocsPerGroup, fillSortFields, getScores, getMaxScores);
       }
      groupMap.put(group.groupValue,
          new SearchGroupDocs<>(group.groupValue,
              collector));
      groupMap.put(group.groupValue, new SearchGroupDocs<>(group.groupValue, collector));
     }
   }
 
@@ -133,7 +132,7 @@ public abstract class AbstractSecondPassGroupingCollector<GROUP_VALUE_TYPE> exte
     }
 
     return new TopGroups<>(groupSort.getSort(),
                                           withinGroupSort == null ? null : withinGroupSort.getSort(),
                                           withinGroupSort.getSort(),
                                            totalHitCount, totalGroupedHitCount, groupDocsResult,
                                            maxScore);
   }
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
index c501c5bae35..9bcfa4da527 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
@@ -231,8 +231,7 @@ public class BlockGroupingCollector extends SimpleCollector {
 
     this.needsScores = needsScores;
     this.lastDocPerGroup = lastDocPerGroup;
    // TODO: allow null groupSort to mean "by relevance",
    // and specialize it?

     this.groupSort = groupSort;
     
     this.topNGroups = topNGroups;
@@ -265,8 +264,7 @@ public class BlockGroupingCollector extends SimpleCollector {
    *  DocValues, etc.)
    *
    *  @param withinGroupSort The {@link Sort} used to sort
   *    documents within each group.  Passing null is
   *    allowed, to sort by relevance.
   *    documents within each group.
    *  @param groupOffset Which group to start from
    *  @param withinGroupOffset Which document to start from
    *    within each group
@@ -300,7 +298,7 @@ public class BlockGroupingCollector extends SimpleCollector {
       // At this point we hold all docs w/ in each group,
       // unsorted; we now sort them:
       final TopDocsCollector<?> collector;
      if (withinGroupSort == null) {
      if (withinGroupSort.equals(Sort.RELEVANCE)) {
         // Sort by score
         if (!needsScores) {
           throw new IllegalArgumentException("cannot sort by relevance within group: needsScores=false");
@@ -356,7 +354,7 @@ public class BlockGroupingCollector extends SimpleCollector {
     */
 
     return new TopGroups<>(new TopGroups<>(groupSort.getSort(),
                                       withinGroupSort == null ? null : withinGroupSort.getSort(),
                                       withinGroupSort.getSort(),
                                        totalHitCount, totalGroupedHitCount, groups, maxScore),
                          totalGroupCount);
   }
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/GroupingSearch.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/GroupingSearch.java
index bc6aba1da18..affa5c087f7 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/GroupingSearch.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/GroupingSearch.java
@@ -58,7 +58,7 @@ public class GroupingSearch {
   private final Query groupEndDocs;
 
   private Sort groupSort = Sort.RELEVANCE;
  private Sort sortWithinGroup;
  private Sort sortWithinGroup = Sort.RELEVANCE;
 
   private int groupDocsOffset;
   private int groupDocsLimit = 1;
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/TopGroups.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/TopGroups.java
index 1c9fa8a296e..981aef0d381 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/TopGroups.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/TopGroups.java
@@ -132,7 +132,7 @@ public class TopGroups<GROUP_VALUE_TYPE> {
     final GroupDocs<T>[] mergedGroupDocs = new GroupDocs[numGroups];
 
     final TopDocs[] shardTopDocs;
    if (docSort == null) {
    if (docSort.equals(Sort.RELEVANCE)) {
       shardTopDocs = new TopDocs[shardGroups.length];
     } else {
       shardTopDocs = new TopFieldDocs[shardGroups.length];
@@ -163,7 +163,7 @@ public class TopGroups<GROUP_VALUE_TYPE> {
         }
         */
 
        if (docSort == null) {
        if (docSort.equals(Sort.RELEVANCE)) {
           shardTopDocs[shardIDX] = new TopDocs(shardGroupDocs.totalHits,
                                                shardGroupDocs.scoreDocs,
                                                shardGroupDocs.maxScore);
@@ -179,7 +179,7 @@ public class TopGroups<GROUP_VALUE_TYPE> {
       }
 
       final TopDocs mergedTopDocs;
      if (docSort == null) {
      if (docSort.equals(Sort.RELEVANCE)) {
         mergedTopDocs = TopDocs.merge(docOffset + docTopN, shardTopDocs);
       } else {
         mergedTopDocs = TopDocs.merge(docSort, docOffset + docTopN, (TopFieldDocs[]) shardTopDocs);
@@ -231,7 +231,7 @@ public class TopGroups<GROUP_VALUE_TYPE> {
 
     if (totalGroupCount != null) {
       TopGroups<T> result = new TopGroups<>(groupSort.getSort(),
                              docSort == null ? null : docSort.getSort(),
                              docSort.getSort(),
                               totalHitCount,
                               totalGroupedHitCount,
                               mergedGroupDocs,
@@ -239,7 +239,7 @@ public class TopGroups<GROUP_VALUE_TYPE> {
       return new TopGroups<>(result, totalGroupCount);
     } else {
       return new TopGroups<>(groupSort.getSort(),
                              docSort == null ? null : docSort.getSort(),
                              docSort.getSort(),
                               totalHitCount,
                               totalGroupedHitCount,
                               mergedGroupDocs,
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
index 236781a557a..3f6744411c2 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
@@ -38,18 +38,19 @@ import org.apache.lucene.util.SentinelIntSet;
  */
 public class TermSecondPassGroupingCollector extends AbstractSecondPassGroupingCollector<BytesRef> {
 
  private final String groupField;
   private final SentinelIntSet ordSet;

   private SortedDocValues index;
  private final String groupField;
 
   @SuppressWarnings({"unchecked", "rawtypes"})
   public TermSecondPassGroupingCollector(String groupField, Collection<SearchGroup<BytesRef>> groups, Sort groupSort, Sort withinGroupSort,
                                          int maxDocsPerGroup, boolean getScores, boolean getMaxScores, boolean fillSortFields)
       throws IOException {
     super(groups, groupSort, withinGroupSort, maxDocsPerGroup, getScores, getMaxScores, fillSortFields);
    ordSet = new SentinelIntSet(groupMap.size(), -2);
     this.groupField = groupField;
    groupDocs = (SearchGroupDocs<BytesRef>[]) new SearchGroupDocs[ordSet.keys.length];
    this.ordSet = new SentinelIntSet(groupMap.size(), -2);
    super.groupDocs = (SearchGroupDocs<BytesRef>[]) new SearchGroupDocs[ordSet.keys.length];
   }
 
   @Override
diff --git a/lucene/grouping/src/test/org/apache/lucene/search/grouping/TestGrouping.java b/lucene/grouping/src/test/org/apache/lucene/search/grouping/TestGrouping.java
index c7aa9380140..0b8db3cd883 100644
-- a/lucene/grouping/src/test/org/apache/lucene/search/grouping/TestGrouping.java
++ b/lucene/grouping/src/test/org/apache/lucene/search/grouping/TestGrouping.java
@@ -150,7 +150,7 @@ public class TestGrouping extends LuceneTestCase {
     final AbstractFirstPassGroupingCollector<?> c1 = createRandomFirstPassCollector(groupField, groupSort, 10);
     indexSearcher.search(new TermQuery(new Term("content", "random")), c1);
 
    final AbstractSecondPassGroupingCollector<?> c2 = createSecondPassCollector(c1, groupField, groupSort, null, 0, 5, true, true, true);
    final AbstractSecondPassGroupingCollector<?> c2 = createSecondPassCollector(c1, groupField, groupSort, Sort.RELEVANCE, 0, 5, true, true, true);
     indexSearcher.search(new TermQuery(new Term("content", "random")), c2);
 
     final TopGroups<?> groups = c2.getTopGroups(0);
diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index d93bcb3de37..318a5c85d79 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -159,8 +159,14 @@ Other Changes
 * SOLR-8179: SQL JDBC - DriverImpl loadParams doesn't support keys with no values in the connection string
   (Kevin Risden, Joel Bernstein)
 
==================  5.5.0 ==================
(No Changes)
======================= Lucene 5.5.0 =======================

Other Changes
----------------------

* LUCENE-6900: Added test for score ordered grouping, and refactored TopGroupsResultTransformer.
  (David Smiley)

 
 ==================  5.4.0 ==================
 
diff --git a/solr/core/src/java/org/apache/solr/search/Grouping.java b/solr/core/src/java/org/apache/solr/search/Grouping.java
index 410142303e3..b6730a68478 100644
-- a/solr/core/src/java/org/apache/solr/search/Grouping.java
++ b/solr/core/src/java/org/apache/solr/search/Grouping.java
@@ -40,7 +40,6 @@ import org.apache.lucene.search.MultiCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TimeLimitingCollector;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.TopDocsCollector;
@@ -74,7 +73,7 @@ import org.slf4j.LoggerFactory;
 
 /**
  * Basic Solr Grouping infrastructure.
 * Warning NOT thread save!
 * Warning NOT thread safe!
  *
  * @lucene.experimental
  */
@@ -109,7 +108,7 @@ public class Grouping {
   private NamedList grouped = new SimpleOrderedMap();
   private Set<Integer> idSet = new LinkedHashSet<>();  // used for tracking unique docs when we need a doclist
   private int maxMatches;  // max number of matches from any grouping command
  private float maxScore = Float.NEGATIVE_INFINITY;  // max score seen in any doclist
  private float maxScore = Float.NaN;  // max score seen in any doclist
   private boolean signalCacheWarning = false;
   private TimeLimitingCollector timeLimitingCollector;
 
@@ -311,16 +310,8 @@ public class Grouping {
     boolean cacheScores = false;
     // NOTE: Change this when withinGroupSort can be specified per group
     if (!needScores && !commands.isEmpty()) {
      if (commands.get(0).withinGroupSort == null) {
        cacheScores = true;
      } else {
        for (SortField field : commands.get(0).withinGroupSort.getSort()) {
          if (field.getType() == SortField.Type.SCORE) {
            cacheScores = true;
            break;
          }
        }
      }
      Sort withinGroupSort = commands.get(0).withinGroupSort;
      cacheScores = withinGroupSort == null || withinGroupSort.needsScores();
     } else if (needScores) {
       cacheScores = needScores;
     }
@@ -638,7 +629,7 @@ public class Grouping {
       }
 
       float score = groups.maxScore;
      maxScore = Math.max(maxScore, score);
      maxScore = maxAvoidNaN(score, maxScore);
       DocSlice docs = new DocSlice(off, Math.max(0, ids.length - off), ids, scores, groups.totalHits, score);
 
       if (getDocList) {
@@ -661,13 +652,11 @@ public class Grouping {
       List<Float> scores = new ArrayList<>();
       int docsToGather = getMax(offset, numGroups, maxDoc);
       int docsGathered = 0;
      float maxScore = Float.NEGATIVE_INFINITY;
      float maxScore = Float.NaN;
 
       outer:
       for (GroupDocs group : groups) {
        if (group.maxScore > maxScore) {
          maxScore = group.maxScore;
        }
        maxScore = maxAvoidNaN(maxScore, group.maxScore);
 
         for (ScoreDoc scoreDoc : group.scoreDocs) {
           if (docsGathered >= docsToGather) {
@@ -696,6 +685,15 @@ public class Grouping {
 
   }
 
  /** Differs from {@link Math#max(float, float)} in that if only one side is NaN, we return the other. */
  private float maxAvoidNaN(float valA, float valB) {
    if (Float.isNaN(valA) || valB > valA) {
      return valB;
    } else {
      return valA;
    }
  }

   /**
    * A group command for grouping on a field.
    */
@@ -759,6 +757,7 @@ public class Grouping {
 
       int groupedDocsToCollect = getMax(groupOffset, docsPerGroup, maxDoc);
       groupedDocsToCollect = Math.max(groupedDocsToCollect, 1);
      Sort withinGroupSort = this.withinGroupSort != null ? this.withinGroupSort : Sort.RELEVANCE;
       secondPass = new TermSecondPassGroupingCollector(
           groupBy, topGroups, groupSort, withinGroupSort, groupedDocsToCollect, needScores, needScores, false
       );
@@ -776,7 +775,7 @@ public class Grouping {
      */
     @Override
     public AbstractAllGroupHeadsCollector<?> createAllGroupCollector() throws IOException {
      Sort sortWithinGroup = withinGroupSort != null ? withinGroupSort : new Sort();
      Sort sortWithinGroup = withinGroupSort != null ? withinGroupSort : Sort.RELEVANCE;
       return TermAllGroupHeadsCollector.create(groupBy, sortWithinGroup);
     }
 
@@ -882,7 +881,7 @@ public class Grouping {
 
     TopDocsCollector newCollector(Sort sort, boolean needScores) throws IOException {
       int groupDocsToCollect = getMax(groupOffset, docsPerGroup, maxDoc);
      if (sort == null || sort == Sort.RELEVANCE) {
      if (sort == null || sort.equals(Sort.RELEVANCE)) {
         return TopScoreDocCollector.create(groupDocsToCollect);
       } else {
         return TopFieldCollector.create(searcher.weightSort(sort), groupDocsToCollect, false, needScores, needScores);
@@ -979,6 +978,7 @@ public class Grouping {
 
       int groupdDocsToCollect = getMax(groupOffset, docsPerGroup, maxDoc);
       groupdDocsToCollect = Math.max(groupdDocsToCollect, 1);
      Sort withinGroupSort = this.withinGroupSort != null ? this.withinGroupSort : Sort.RELEVANCE;
       secondPass = new FunctionSecondPassGroupingCollector(
           topGroups, groupSort, withinGroupSort, groupdDocsToCollect, needScores, needScores, false, groupBy, context
       );
@@ -993,7 +993,7 @@ public class Grouping {
 
     @Override
     public AbstractAllGroupHeadsCollector<?> createAllGroupCollector() throws IOException {
      Sort sortWithinGroup = withinGroupSort != null ? withinGroupSort : new Sort();
      Sort sortWithinGroup = withinGroupSort != null ? withinGroupSort : Sort.RELEVANCE;
       return new FunctionAllGroupHeadsCollector(groupBy, context, sortWithinGroup);
     }
 
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/SearchGroupShardResponseProcessor.java b/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/SearchGroupShardResponseProcessor.java
index 79f87b768c7..fac56966cca 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/SearchGroupShardResponseProcessor.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/SearchGroupShardResponseProcessor.java
@@ -51,6 +51,10 @@ public class SearchGroupShardResponseProcessor implements ShardResponseProcessor
     SortSpec ss = rb.getSortSpec();
     Sort groupSort = rb.getGroupingSpec().getGroupSort();
     String[] fields = rb.getGroupingSpec().getFields();
    Sort sortWithinGroup = rb.getGroupingSpec().getSortWithinGroup();
    if (sortWithinGroup == null) { // TODO prevent it from being null in the first place
      sortWithinGroup = Sort.RELEVANCE;
    }
 
     Map<String, List<Collection<SearchGroup<BytesRef>>>> commandSearchGroups = new HashMap<>();
     Map<String, Map<SearchGroup<BytesRef>, Set<String>>> tempSearchGroupToShards = new HashMap<>();
@@ -106,7 +110,7 @@ public class SearchGroupShardResponseProcessor implements ShardResponseProcessor
         maxElapsedTime = (int) Math.max(maxElapsedTime, srsp.getSolrResponse().getElapsedTime());
         @SuppressWarnings("unchecked")
         NamedList<NamedList> firstPhaseResult = (NamedList<NamedList>) srsp.getSolrResponse().getResponse().get("firstPhase");
        final Map<String, SearchGroupsFieldCommandResult> result = serializer.transformToNative(firstPhaseResult, groupSort, null, srsp.getShard());
        final Map<String, SearchGroupsFieldCommandResult> result = serializer.transformToNative(firstPhaseResult, groupSort, sortWithinGroup, srsp.getShard());
         for (String field : commandSearchGroups.keySet()) {
           final SearchGroupsFieldCommandResult firstPhaseCommandResult = result.get(field);
 
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/TopGroupsShardResponseProcessor.java b/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/TopGroupsShardResponseProcessor.java
index 7c564b92838..abe4cc2e562 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/TopGroupsShardResponseProcessor.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/distributed/responseprocessor/TopGroupsShardResponseProcessor.java
@@ -61,6 +61,9 @@ public class TopGroupsShardResponseProcessor implements ShardResponseProcessor {
     String[] fields = rb.getGroupingSpec().getFields();
     String[] queries = rb.getGroupingSpec().getQueries();
     Sort sortWithinGroup = rb.getGroupingSpec().getSortWithinGroup();
    if (sortWithinGroup == null) { // TODO prevent it from being null in the first place
      sortWithinGroup = Sort.RELEVANCE;
    }
 
     // If group.format=simple group.offset doesn't make sense
     int groupOffsetDefault;
@@ -173,7 +176,7 @@ public class TopGroupsShardResponseProcessor implements ShardResponseProcessor {
 
         int topN = rb.getGroupingSpec().getOffset() + rb.getGroupingSpec().getLimit();
         final TopDocs mergedTopDocs;
        if (sortWithinGroup == null) {
        if (sortWithinGroup.equals(Sort.RELEVANCE)) {
           mergedTopDocs = TopDocs.merge(topN, topDocs.toArray(new TopDocs[topDocs.size()]));
         } else {
           mergedTopDocs = TopDocs.merge(sortWithinGroup, topN, topDocs.toArray(new TopFieldDocs[topDocs.size()]));
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java b/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
index df0aaf1c2fd..9589896cc14 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/distributed/shardresultserializer/TopGroupsResultTransformer.java
@@ -17,6 +17,12 @@ package org.apache.solr.search.grouping.distributed.shardresultserializer;
  * limitations under the License.
  */
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 import org.apache.lucene.document.DocumentStoredFieldVisitor;
 import org.apache.lucene.index.StoredDocument;
 import org.apache.lucene.search.FieldDoc;
@@ -27,8 +33,6 @@ import org.apache.lucene.search.TopFieldDocs;
 import org.apache.lucene.search.grouping.GroupDocs;
 import org.apache.lucene.search.grouping.TopGroups;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.UnicodeUtil;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.handler.component.ResponseBuilder;
 import org.apache.solr.handler.component.ShardDoc;
@@ -42,12 +46,6 @@ import org.apache.solr.search.grouping.distributed.command.TopGroupsFieldCommand
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 /**
  * Implementation for transforming {@link TopGroups} and {@link TopDocs} into a {@link NamedList} structure and
  * visa versa.
@@ -110,40 +108,9 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
 
         @SuppressWarnings("unchecked")
         List<NamedList<Object>> documents = (List<NamedList<Object>>) commandResult.get("documents");
        ScoreDoc[] scoreDocs = new ScoreDoc[documents.size()];
        int j = 0;
        for (NamedList<Object> document : documents) {
          Object docId = document.get("id");
          Object uniqueId = null;
          if (docId != null)
            uniqueId = docId.toString();
          else
            log.warn("doc {} has null 'id'", document);
          Float score = (Float) document.get("score");
          if (score == null) {
            score = Float.NaN;
          }
          Object[] sortValues = null;
          Object sortValuesVal = document.get("sortValues");
          if (sortValuesVal != null) {
            sortValues = ((List) sortValuesVal).toArray();
            for (int k = 0; k < sortValues.length; k++) {
              SchemaField field = groupSort.getSort()[k].getField() != null ? schema.getFieldOrNull(groupSort.getSort()[k].getField()) : null;
              if (field != null) {
                FieldType fieldType = field.getType();
                if (sortValues[k] != null) {
                  sortValues[k] = fieldType.unmarshalSortValue(sortValues[k]);
                }
              }
            }
          }
          else {
            log.warn("doc {} has null 'sortValues'", document);
          }
          scoreDocs[j++] = new ShardDoc(score, sortValues, uniqueId, shard);
        }
        ScoreDoc[] scoreDocs = transformToNativeShardDoc(documents, groupSort, shard, schema);
         final TopDocs topDocs;
        if (sortWithinGroup == null) {
        if (sortWithinGroup.equals(Sort.RELEVANCE)) {
           topDocs = new TopDocs(totalHits, scoreDocs, maxScore);
         } else {
           topDocs = new TopFieldDocs(totalHits, scoreDocs, sortWithinGroup.getSort(), maxScore);
@@ -167,26 +134,7 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
 
         @SuppressWarnings("unchecked")
         List<NamedList<Object>> documents = (List<NamedList<Object>>) groupResult.get("documents");
        ScoreDoc[] scoreDocs = new ScoreDoc[documents.size()];
        int j = 0;
        for (NamedList<Object> document : documents) {
          Object uniqueId = document.get("id").toString();
          Float score = (Float) document.get("score");
          if (score == null) {
            score = Float.NaN;
          }
          Object[] sortValues = ((List) document.get("sortValues")).toArray();
          for (int k = 0; k < sortValues.length; k++) {
            SchemaField field = sortWithinGroup.getSort()[k].getField() != null ? schema.getFieldOrNull(sortWithinGroup.getSort()[k].getField()) : null;
            if (field != null) {
              FieldType fieldType = field.getType();
              if (sortValues[k] != null) {
                sortValues[k] = fieldType.unmarshalSortValue(sortValues[k]);
              }
            }
          }
          scoreDocs[j++] = new ShardDoc(score, sortValues, uniqueId, shard);
        }
        ScoreDoc[] scoreDocs = transformToNativeShardDoc(documents, groupSort, shard, schema);
 
         BytesRef groupValueRef = groupValue != null ? new BytesRef(groupValue) : null;
         groupDocs.add(new GroupDocs<>(Float.NaN, maxScore, totalGroupHits, scoreDocs, groupValueRef, null));
@@ -204,6 +152,43 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
     return result;
   }
 
  protected ScoreDoc[] transformToNativeShardDoc(List<NamedList<Object>> documents, Sort groupSort, String shard,
                                                 IndexSchema schema) {
    ScoreDoc[] scoreDocs = new ScoreDoc[documents.size()];
    int j = 0;
    for (NamedList<Object> document : documents) {
      Object docId = document.get("id");
      if (docId != null) {
        docId = docId.toString();
      } else {
        log.error("doc {} has null 'id'", document);
      }
      Float score = (Float) document.get("score");
      if (score == null) {
        score = Float.NaN;
      }
      Object[] sortValues = null;
      Object sortValuesVal = document.get("sortValues");
      if (sortValuesVal != null) {
        sortValues = ((List) sortValuesVal).toArray();
        for (int k = 0; k < sortValues.length; k++) {
          SchemaField field = groupSort.getSort()[k].getField() != null
              ? schema.getFieldOrNull(groupSort.getSort()[k].getField()) : null;
          if (field != null) {
            FieldType fieldType = field.getType();
            if (sortValues[k] != null) {
              sortValues[k] = fieldType.unmarshalSortValue(sortValues[k]);
            }
          }
        }
      } else {
        log.debug("doc {} has null 'sortValues'", document);
      }
      scoreDocs[j++] = new ShardDoc(score, sortValues, docId, shard);
    }
    return scoreDocs;
  }

   protected NamedList serializeTopGroups(TopGroups<BytesRef> data, SchemaField groupField) throws IOException {
     NamedList<Object> result = new NamedList<>();
     result.add("totalGroupedHitCount", data.totalGroupedHitCount);
@@ -211,7 +196,6 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
     if (data.totalGroupCount != null) {
       result.add("totalGroupCount", data.totalGroupCount);
     }
    CharsRef spare = new CharsRef();
 
     final IndexSchema schema = rb.req.getSearcher().getSchema();
     SchemaField uniqueField = schema.getUniqueKeyField();
@@ -233,7 +217,7 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
           document.add("score", searchGroup.scoreDocs[i].score);
         }
         if (!(searchGroup.scoreDocs[i] instanceof FieldDoc)) {
          continue;
          continue; // thus don't add sortValues below
         }
 
         FieldDoc fieldDoc = (FieldDoc) searchGroup.scoreDocs[i];
@@ -264,7 +248,8 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
     NamedList<Object> queryResult = new NamedList<>();
     queryResult.add("matches", result.getMatches());
     queryResult.add("totalHits", result.getTopDocs().totalHits);
    if (rb.getGroupingSpec().isNeedScore()) {
    // debug: assert !Float.isNaN(result.getTopDocs().getMaxScore()) == rb.getGroupingSpec().isNeedScore();
    if (!Float.isNaN(result.getTopDocs().getMaxScore())) {
       queryResult.add("maxScore", result.getTopDocs().getMaxScore());
     }
     List<NamedList> documents = new ArrayList<>();
@@ -272,18 +257,17 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
 
     final IndexSchema schema = rb.req.getSearcher().getSchema();
     SchemaField uniqueField = schema.getUniqueKeyField();
    CharsRef spare = new CharsRef();
     for (ScoreDoc scoreDoc : result.getTopDocs().scoreDocs) {
       NamedList<Object> document = new NamedList<>();
       documents.add(document);
 
       StoredDocument doc = retrieveDocument(uniqueField, scoreDoc.doc);
       document.add("id", uniqueField.getType().toExternal(doc.getField(uniqueField.getName())));
      if (rb.getGroupingSpec().isNeedScore())  {
      if (!Float.isNaN(scoreDoc.score))  {
         document.add("score", scoreDoc.score);
       }
       if (!FieldDoc.class.isInstance(scoreDoc)) {
        continue;
        continue; // thus don't add sortValues below
       }
 
       FieldDoc fieldDoc = (FieldDoc) scoreDoc;
@@ -291,7 +275,7 @@ public class TopGroupsResultTransformer implements ShardResultTransformer<List<C
       for (int j = 0; j < fieldDoc.fields.length; j++) {
         Object sortValue  = fieldDoc.fields[j];
         Sort groupSort = rb.getGroupingSpec().getGroupSort();
        SchemaField field = groupSort.getSort()[j].getField() != null 
        SchemaField field = groupSort.getSort()[j].getField() != null
                           ? schema.getFieldOrNull(groupSort.getSort()[j].getField()) : null;
         if (field != null) {
           FieldType fieldType = field.getType();
diff --git a/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java b/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
index db290c4d837..bce93c012c2 100644
-- a/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
++ b/solr/core/src/test/org/apache/solr/TestDistributedGrouping.java
@@ -259,10 +259,12 @@ public class TestDistributedGrouping extends BaseDistributedSearchTestCase {
     assertEquals(shardsArr.length, groupCount);
 
 
    // We cannot validate distributed grouping with scoring as first sort. since there is no global idf. We can check if no errors occur
    simpleQuery("q", "*:*", "rows", 100, "fl", "id," + i1, "group", "true", "group.field", i1, "group.limit", 10, "sort", i1 + " desc", "group.sort", "score desc"); // SOLR-2955
    simpleQuery("q", "*:*", "rows", 100, "fl", "id," + i1, "group", "true", "group.field", i1, "group.limit", 10, "sort", "score desc, _docid_ asc, id asc");
    simpleQuery("q", "*:*", "rows", 100, "fl", "id," + i1, "group", "true", "group.field", i1, "group.limit", 10);
    // We validate distributed grouping with scoring as first sort.
    // note: this 'q' matches all docs and returns the 'id' as the score, which is unique and so our results should be deterministic.
    handle.put("maxScore", SKIP);// TODO see SOLR-6612
    query("q", "{!func}id", "rows", 100, "fl", "score,id," + i1, "group", "true", "group.field", i1, "group.limit", 10, "sort", i1 + " desc", "group.sort", "score desc"); // SOLR-2955
    query("q", "{!func}id", "rows", 100, "fl", "score,id," + i1, "group", "true", "group.field", i1, "group.limit", 10, "sort", "score desc, _docid_ asc, id asc");
    query("q", "{!func}id", "rows", 100, "fl", "score,id," + i1, "group", "true", "group.field", i1, "group.limit", 10);
 
     // Can't validate the response, but can check if no errors occur.
     simpleQuery("q", "*:*", "rows", 100, "fl", "id," + i1, "group", "true", "group.query", t1 + ":kings OR " + t1 + ":eggs", "group.limit", 10, "sort", i1 + " asc, id asc", CommonParams.TIME_ALLOWED, 1);
diff --git a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
index 89a12ea1315..2bad5ff89e4 100644
-- a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
++ b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
@@ -778,10 +778,11 @@ public abstract class BaseDistributedSearchTestCase extends SolrTestCaseJ4 {
 
     String cmp;
     int f = flags(handle, "maxScore");
    if ((f & SKIPVAL) == 0) {
    if (f == 0) {
       cmp = compare(a.getMaxScore(), b.getMaxScore(), 0, handle);
       if (cmp != null) return ".maxScore" + cmp;
    } else {
    } else if ((f & SKIP) == 0) { // so we skip val but otherwise both should be present
      assert (f & SKIPVAL) != 0;
       if (b.getMaxScore() != null) {
         if (a.getMaxScore() == null) {
           return ".maxScore missing";
- 
2.19.1.windows.1

