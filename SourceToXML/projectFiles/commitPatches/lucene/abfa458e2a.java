From abfa458e2a082eb0f1f7e53d495e0c399499e07d Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 8 Jan 2011 01:45:08 +0000
Subject: [PATCH] LUCENE-2831: remove another erroneous use of a non-atomic
 context

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1056589 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/search/SolrIndexSearcher.java    | 16 ----------------
 1 file changed, 16 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
index a3183f6be5b..a011acf4f01 100644
-- a/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -898,22 +898,6 @@ public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
     return positive ? getDocSetNC(absQ,filter) : filter.andNot(getPositiveDocSet(absQ));
   }
 

  /**
  * Converts a filter into a DocSet.
  * This method is not cache-aware and no caches are checked.
  */
  public DocSet convertFilter(Filter lfilter) throws IOException {
    DocIdSet docSet = lfilter.getDocIdSet(this.reader.getTopReaderContext());
    OpenBitSet obs = new OpenBitSet();
    DocIdSetIterator it = docSet.iterator();
    int doc;
    while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      obs.fastSet(doc);
    }
    return new BitDocSet(obs);
  }

   /**
    * Returns documents matching both <code>query</code> and <code>filter</code>
    * and sorted by <code>sort</code>.
- 
2.19.1.windows.1

