From 2635ac0cf7a726d36ff6d7362c12970bb99b3c0c Mon Sep 17 00:00:00 2001
From: Simon Willnauer <simonw@apache.org>
Date: Wed, 2 Feb 2011 22:34:15 +0000
Subject: [PATCH] LUCENE-2831: Use leaf reader slices for parallel execution
 instead of SubSearcher instances.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1066669 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/search/IndexSearcher.java   | 198 ++++++++++--------
 .../search/function/QueryValueSource.java     |   4 +-
 2 files changed, 115 insertions(+), 87 deletions(-)

diff --git a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
index 6e885c003ff..23736d0726c 100644
-- a/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -65,10 +65,11 @@ public class IndexSearcher {
   // in the next release
   protected final ReaderContext readerContext;
   protected final AtomicReaderContext[] leafContexts;
  // used with executor - each slice holds a set of leafs executed within one thread
  protected final LeafSlice[] leafSlices;
 
   // These are only used for multi-threaded search
   private final ExecutorService executor;
  protected final IndexSearcher[] subSearchers;
 
   // the default SimilarityProvider
   private static final SimilarityProvider defaultProvider = new DefaultSimilarity();
@@ -175,47 +176,22 @@ public class IndexSearcher {
     this.closeReader = closeReader;
     this.readerContext = context;
     leafContexts = ReaderUtil.leaves(context);
    
    if (executor == null) {
      subSearchers = null;
    } else {
      subSearchers = new IndexSearcher[this.leafContexts.length];
      for (int i = 0; i < subSearchers.length; i++) {
        if (leafContexts[i].reader == context.reader) {
          subSearchers[i] = this;
        } else {
          subSearchers[i] = new IndexSearcher(context, leafContexts[i]);
        }
      }
    }
  }

  /**
   * Expert: Creates a searcher from a top-level {@link ReaderContext} with and
   * executes searches on the given leave slice exclusively instead of searching
   * over all leaves. This constructor should be used to run one or more leaves
   * within a single thread. Hence, for scorer and filter this looks like an
   * ordinary search in the hierarchy such that there is no difference between
   * single and multi-threaded.
   * 
   * @lucene.experimental
   * */
  public IndexSearcher(ReaderContext topLevel, AtomicReaderContext... leaves) {
    assert assertLeaves(topLevel, leaves);
    readerContext = topLevel;
    reader = topLevel.reader;
    leafContexts = leaves;
    executor = null;
    subSearchers = null;
    closeReader = false;
    this.leafSlices = executor == null ? null : slices(leafContexts);
   }
   
  private boolean assertLeaves(ReaderContext topLevel, AtomicReaderContext... leaves) {
    for (AtomicReaderContext leaf : leaves) {
      assert ReaderUtil.getTopLevelContext(leaf) == topLevel : "leaf context is not a leaf of the given top-level context";
  /**
   * Expert: Creates an array of leaf slices each holding a subset of the given leaves.
   * Each {@link LeafSlice} is executed in a single thread. By default there
   * will be one {@link LeafSlice} per leaf ({@link AtomicReaderContext}).
   */
  protected LeafSlice[] slices(AtomicReaderContext...leaves) {
    LeafSlice[] slices = new LeafSlice[leaves.length];
    for (int i = 0; i < slices.length; i++) {
      slices[i] = new LeafSlice(leaves[i]);
     }
    return true;
    return slices;
   }

   
   /** Return the {@link IndexReader} this searches. */
   public IndexReader getIndexReader() {
@@ -236,11 +212,11 @@ public class IndexSearcher {
       return reader.docFreq(term);
     } else {
       final ExecutionHelper<Integer> runner = new ExecutionHelper<Integer>(executor);
      for(int i = 0; i < subSearchers.length; i++) {
        final IndexSearcher searchable = subSearchers[i];
      for(int i = 0; i < leafContexts.length; i++) {
        final IndexReader leaf = leafContexts[i].reader;
         runner.submit(new Callable<Integer>() {
             public Integer call() throws IOException {
              return Integer.valueOf(searchable.docFreq(term));
              return Integer.valueOf(leaf.docFreq(term));
             }
           });
       }
@@ -324,7 +300,7 @@ public class IndexSearcher {
    */
   public void search(Query query, Filter filter, Collector results)
     throws IOException {
    search(createWeight(query), filter, results);
    search(leafContexts, createWeight(query), filter, results);
   }
 
   /** Lower-level search API.
@@ -342,7 +318,7 @@ public class IndexSearcher {
   */
   public void search(Query query, Collector results)
     throws IOException {
    search(createWeight(query), null, results);
    search(leafContexts, createWeight(query), null, results);
   }
   
   /** Search implementation with arbitrary sorting.  Finds
@@ -382,25 +358,16 @@ public class IndexSearcher {
    * @throws BooleanQuery.TooManyClauses
    */
   protected TopDocs search(Weight weight, Filter filter, int nDocs) throws IOException {

     if (executor == null) {
      // single thread
      int limit = reader.maxDoc();
      if (limit == 0) {
        limit = 1;
      }
      nDocs = Math.min(nDocs, limit);
      TopScoreDocCollector collector = TopScoreDocCollector.create(nDocs, !weight.scoresDocsOutOfOrder());
      search(weight, filter, collector);
      return collector.topDocs();
      return search(leafContexts, weight, filter, nDocs);
     } else {
       final HitQueue hq = new HitQueue(nDocs, false);
       final Lock lock = new ReentrantLock();
       final ExecutionHelper<TopDocs> runner = new ExecutionHelper<TopDocs>(executor);
     
      for (int i = 0; i < subSearchers.length; i++) { // search each sub
      for (int i = 0; i < leafSlices.length; i++) { // search each sub
         runner.submit(
                      new SearcherCallableNoSort(lock, subSearchers[i], weight, filter, nDocs, hq));
                      new SearcherCallableNoSort(lock, this, leafSlices[i], weight, filter, nDocs, hq));
       }
 
       int totalHits = 0;
@@ -418,6 +385,25 @@ public class IndexSearcher {
     }
   }
 
  /** Expert: Low-level search implementation.  Finds the top <code>n</code>
   * hits for <code>query</code>, using the given leaf readers applying <code>filter</code> if non-null.
   *
   * <p>Applications should usually call {@link IndexSearcher#search(Query,int)} or
   * {@link IndexSearcher#search(Query,Filter,int)} instead.
   * @throws BooleanQuery.TooManyClauses
   */
  protected TopDocs search(AtomicReaderContext[] leaves, Weight weight, Filter filter, int nDocs) throws IOException {
    // single thread
    int limit = reader.maxDoc();
    if (limit == 0) {
      limit = 1;
    }
    nDocs = Math.min(nDocs, limit);
    TopScoreDocCollector collector = TopScoreDocCollector.create(nDocs, !weight.scoresDocsOutOfOrder());
    search(leaves, weight, filter, collector);
    return collector.topDocs();
  }

   /** Expert: Low-level search implementation with arbitrary sorting.  Finds
    * the top <code>n</code> hits for <code>query</code>, applying
    * <code>filter</code> if non-null, and sorting the hits by the criteria in
@@ -449,27 +435,18 @@ public class IndexSearcher {
       throws IOException {
 
     if (sort == null) throw new NullPointerException();

    
     if (executor == null) {
      // single thread
      int limit = reader.maxDoc();
      if (limit == 0) {
        limit = 1;
      }
      nDocs = Math.min(nDocs, limit);

      TopFieldCollector collector = TopFieldCollector.create(sort, nDocs,
                                                             fillFields, fieldSortDoTrackScores, fieldSortDoMaxScore, !weight.scoresDocsOutOfOrder());
      search(weight, filter, collector);
      return (TopFieldDocs) collector.topDocs();
      // use all leaves here!
      return search (leafContexts, weight, filter, nDocs, sort, fillFields);
     } else {
       // TODO: make this respect fillFields
       final FieldDocSortedHitQueue hq = new FieldDocSortedHitQueue(nDocs);
       final Lock lock = new ReentrantLock();
       final ExecutionHelper<TopFieldDocs> runner = new ExecutionHelper<TopFieldDocs>(executor);
      for (int i = 0; i < subSearchers.length; i++) { // search each sub
      for (int i = 0; i < leafSlices.length; i++) { // search each leaf slice
         runner.submit(
                      new SearcherCallableWithSort(lock, subSearchers[i], weight, filter, nDocs, hq, sort));
                      new SearcherCallableWithSort(lock, this, leafSlices[i], weight, filter, nDocs, hq, sort));
       }
       int totalHits = 0;
       float maxScore = Float.NEGATIVE_INFINITY;
@@ -484,6 +461,33 @@ public class IndexSearcher {
       return new TopFieldDocs(totalHits, scoreDocs, hq.getFields(), maxScore);
     }
   }
  
  
  /**
   * Just like {@link #search(Weight, Filter, int, Sort)}, but you choose
   * whether or not the fields in the returned {@link FieldDoc} instances should
   * be set by specifying fillFields.
   *
   * <p>NOTE: this does not compute scores by default.  If you
   * need scores, create a {@link TopFieldCollector}
   * instance by calling {@link TopFieldCollector#create} and
   * then pass that to {@link #search(Weight, Filter,
   * Collector)}.</p>
   */
  protected TopFieldDocs search(AtomicReaderContext[] leaves, Weight weight, Filter filter, int nDocs,
      Sort sort, boolean fillFields) throws IOException {
    // single thread
    int limit = reader.maxDoc();
    if (limit == 0) {
      limit = 1;
    }
    nDocs = Math.min(nDocs, limit);

    TopFieldCollector collector = TopFieldCollector.create(sort, nDocs,
                                                           fillFields, fieldSortDoTrackScores, fieldSortDoMaxScore, !weight.scoresDocsOutOfOrder());
    search(leaves, weight, filter, collector);
    return (TopFieldDocs) collector.topDocs();
  }
 
   /**
    * Lower-level search API.
@@ -497,6 +501,12 @@ public class IndexSearcher {
    * documents. The high-level search API ({@link IndexSearcher#search(Query,int)}) is
    * usually more efficient, as it skips non-high-scoring hits.
    * 
   * <p>
   * NOTE: this method executes the searches on all given leaves exclusively.
   * To search across all the searchers leaves use {@link #leafContexts}.
   * 
   * @param leaves 
   *          the searchers leaves to execute the searches on
    * @param weight
    *          to match documents
    * @param filter
@@ -505,7 +515,7 @@ public class IndexSearcher {
    *          to receive hits
    * @throws BooleanQuery.TooManyClauses
    */
  protected void search(Weight weight, Filter filter, Collector collector)
  protected void search(AtomicReaderContext[] leaves, Weight weight, Filter filter, Collector collector)
       throws IOException {
 
     // TODO: should we make this
@@ -513,18 +523,18 @@ public class IndexSearcher {
     ScorerContext scorerContext =  ScorerContext.def().scoreDocsInOrder(true).topScorer(true);
     // always use single thread:
     if (filter == null) {
      for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i]);
      for (int i = 0; i < leaves.length; i++) { // search each subreader
        collector.setNextReader(leaves[i]);
         scorerContext = scorerContext.scoreDocsInOrder(!collector.acceptsDocsOutOfOrder());
        Scorer scorer = weight.scorer(leafContexts[i], scorerContext);
        Scorer scorer = weight.scorer(leaves[i], scorerContext);
         if (scorer != null) {
           scorer.score(collector);
         }
       }
     } else {
      for (int i = 0; i < leafContexts.length; i++) { // search each subreader
        collector.setNextReader(leafContexts[i]);
        searchWithFilter(leafContexts[i], weight, filter, collector);
      for (int i = 0; i < leaves.length; i++) { // search each subreader
        collector.setNextReader(leaves[i]);
        searchWithFilter(leaves[i], weight, filter, collector);
       }
     }
   }
@@ -649,7 +659,7 @@ public class IndexSearcher {
    * Returns this searchers the top-level {@link ReaderContext}.
    * @see IndexReader#getTopReaderContext()
    */
  /* Sugar for .getIndexReader().getTopReaderContext() */
  /* sugar for #getReader().getTopReaderContext() */
   public ReaderContext getTopReaderContext() {
     return readerContext;
   }
@@ -660,24 +670,26 @@ public class IndexSearcher {
   private static final class SearcherCallableNoSort implements Callable<TopDocs> {
 
     private final Lock lock;
    private final IndexSearcher searchable;
    private final IndexSearcher searcher;
     private final Weight weight;
     private final Filter filter;
     private final int nDocs;
     private final HitQueue hq;
    private final LeafSlice slice;
 
    public SearcherCallableNoSort(Lock lock, IndexSearcher searchable, Weight weight,
    public SearcherCallableNoSort(Lock lock, IndexSearcher searcher, LeafSlice slice,  Weight weight,
         Filter filter, int nDocs, HitQueue hq) {
       this.lock = lock;
      this.searchable = searchable;
      this.searcher = searcher;
       this.weight = weight;
       this.filter = filter;
       this.nDocs = nDocs;
       this.hq = hq;
      this.slice = slice;
     }
 
     public TopDocs call() throws IOException {
      final TopDocs docs = searchable.search (weight, filter, nDocs);
      final TopDocs docs = searcher.search (slice.leaves, weight, filter, nDocs);
       final ScoreDoc[] scoreDocs = docs.scoreDocs;
       for (int j = 0; j < scoreDocs.length; j++) { // merge scoreDocs into hq
         final ScoreDoc scoreDoc = scoreDocs[j];
@@ -701,26 +713,28 @@ public class IndexSearcher {
   private static final class SearcherCallableWithSort implements Callable<TopFieldDocs> {
 
     private final Lock lock;
    private final IndexSearcher searchable;
    private final IndexSearcher searcher;
     private final Weight weight;
     private final Filter filter;
     private final int nDocs;
     private final FieldDocSortedHitQueue hq;
     private final Sort sort;
    private final LeafSlice slice;
 
    public SearcherCallableWithSort(Lock lock, IndexSearcher searchable, Weight weight,
    public SearcherCallableWithSort(Lock lock, IndexSearcher searcher, LeafSlice slice, Weight weight,
         Filter filter, int nDocs, FieldDocSortedHitQueue hq, Sort sort) {
       this.lock = lock;
      this.searchable = searchable;
      this.searcher = searcher;
       this.weight = weight;
       this.filter = filter;
       this.nDocs = nDocs;
       this.hq = hq;
       this.sort = sort;
      this.slice = slice;
     }
 
     public TopFieldDocs call() throws IOException {
      final TopFieldDocs docs = searchable.search (weight, filter, nDocs, sort);
      final TopFieldDocs docs = searcher.search (slice.leaves, weight, filter, nDocs, sort, true);
       lock.lock();
       try {
         hq.setFields(docs.fields);
@@ -791,4 +805,18 @@ public class IndexSearcher {
       return this;
     }
   }

  /**
   * A class holding a subset of the {@link IndexSearcher}s leaf contexts to be
   * executed within a single thread.
   * 
   * @lucene.experimental
   */
  public static class LeafSlice {
    final AtomicReaderContext[] leaves;
    
    public LeafSlice(AtomicReaderContext...leaves) {
      this.leaves = leaves;
    }
  }
 }
diff --git a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
index b7d13efb351..ebf40a15dfb 100755
-- a/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
++ b/solr/src/java/org/apache/solr/search/function/QueryValueSource.java
@@ -100,11 +100,11 @@ class QueryDocValues extends DocValues {
     if (w == null) {
       IndexSearcher weightSearcher;
       if(fcontext == null) {
        weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext), readerContext);
        weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext));
       } else {
         weightSearcher = (IndexSearcher)fcontext.get("searcher");
         if (weightSearcher == null) {
          weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext), readerContext);
          weightSearcher = new IndexSearcher(ReaderUtil.getTopLevelContext(readerContext));
         }
       }
       w = q.weight(weightSearcher);
- 
2.19.1.windows.1

