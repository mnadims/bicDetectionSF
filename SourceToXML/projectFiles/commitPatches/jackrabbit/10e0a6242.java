From 10e0a6242a1ca4526fdb533150a8b7f78fa971e4 Mon Sep 17 00:00:00 2001
From: Michael Duerig <mduerig@apache.org>
Date: Thu, 27 Jan 2011 09:33:13 +0000
Subject: [PATCH] JCR-2415: Update Lucene to 3.0 - Merge all from
 JCR-2415-lucene-3.0 at 1064038 (reintegration)

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1064058 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/PropertyTypeRegistry.java      |   2 +-
 .../core/query/lucene/AbstractExcerpt.java    |  14 +-
 .../core/query/lucene/AbstractIndex.java      |  53 +--
 .../lucene/AbstractScoreDocComparator.java    | 124 ------
 .../core/query/lucene/AbstractWeight.java     |  14 +-
 .../core/query/lucene/CachingIndexReader.java |   3 +-
 .../query/lucene/CachingMultiIndexReader.java |   2 +-
 .../core/query/lucene/CaseTermQuery.java      |  31 +-
 .../core/query/lucene/ChildAxisQuery.java     |  76 ++--
 .../core/query/lucene/DefaultHighlighter.java |  11 +-
 .../core/query/lucene/DefaultQueryHits.java   |   4 +-
 .../core/query/lucene/DerefQuery.java         |  76 ++--
 .../query/lucene/DescendantSelfAxisQuery.java |  99 +++--
 .../lucene/DocOrderScoreNodeIterator.java     |   1 +
 .../query/lucene/FieldComparatorBase.java     | 105 +++++
 .../lucene/FieldComparatorDecorator.java      |  62 +++
 .../core/query/lucene/FieldSelectors.java     |   2 +
 .../jackrabbit/core/query/lucene/IDField.java |   2 +-
 .../query/lucene/IndexDeletionPolicyImpl.java |   2 +-
 .../core/query/lucene/IndexFormatVersion.java |   3 +-
 .../core/query/lucene/IndexHistory.java       |   6 +-
 .../core/query/lucene/IndexInfos.java         |  16 +-
 .../core/query/lucene/IndexMerger.java        |   5 +-
 .../core/query/lucene/IndexMigration.java     |   2 +-
 .../lucene/IndexingConfigurationImpl.java     |   3 +-
 .../core/query/lucene/IndexingQueue.java      |   4 +-
 .../core/query/lucene/JackrabbitAnalyzer.java |   4 +-
 .../query/lucene/JackrabbitIndexReader.java   |   5 +
 .../core/query/lucene/JackrabbitParser.java   |   1 +
 .../query/lucene/JackrabbitQueryParser.java   |   3 +-
 .../query/lucene/JackrabbitTermQuery.java     |   7 +-
 .../query/lucene/LazyTextExtractorField.java  |   1 +
 .../query/lucene/LengthSortComparator.java    |  66 +--
 .../core/query/lucene/LocalNameQuery.java     |   1 +
 .../query/lucene/LocalNameRangeQuery.java     |   1 +
 .../query/lucene/LowerCaseSortComparator.java |  70 +---
 .../core/query/lucene/LuceneQueryHits.java    |  18 +-
 .../core/query/lucene/MatchAllDocsQuery.java  |   1 +
 .../core/query/lucene/MatchAllQuery.java      |   3 +-
 .../core/query/lucene/MatchAllScorer.java     |  70 ++--
 .../core/query/lucene/MatchAllWeight.java     |   4 +-
 .../core/query/lucene/MoreLikeThis.java       |  64 +--
 .../core/query/lucene/MultiScorer.java        |  75 ++--
 .../core/query/lucene/NameQuery.java          |   1 +
 .../core/query/lucene/NameRangeQuery.java     |   1 +
 .../core/query/lucene/NodeIndexer.java        |  43 +-
 .../query/lucene/NodeTraversingQueryHits.java |   2 +-
 .../core/query/lucene/NotQuery.java           |  72 ++--
 .../core/query/lucene/OffsetCharSequence.java |   5 +-
 .../core/query/lucene/Ordering.java           |  25 +-
 .../core/query/lucene/ParentAxisQuery.java    | 122 +++---
 .../core/query/lucene/PersistentIndex.java    |   2 +-
 .../query/lucene/PredicateDerefQuery.java     | 169 ++++----
 .../core/query/lucene/QueryHitsQuery.java     |  61 ++-
 .../core/query/lucene/RangeQuery.java         |  73 ++--
 .../query/lucene/ReadOnlyIndexReader.java     |  10 +-
 .../query/lucene/RefCountingIndexReader.java  |   2 +-
 .../core/query/lucene/ScoreNode.java          |   1 -
 .../core/query/lucene/SearchIndex.java        |  82 ++--
 .../core/query/lucene/SharedFieldCache.java   |  19 +-
 .../lucene/SharedFieldComparatorSource.java   | 379 ++++++++++++++++++
 .../lucene/SharedFieldSortComparator.java     | 242 -----------
 .../core/query/lucene/SimilarityQuery.java    |  12 +-
 .../query/lucene/SimpleExcerptProvider.java   |   4 +-
 .../query/lucene/SingletonTokenStream.java    |  59 +--
 .../query/lucene/SortedLuceneQueryHits.java   |  20 +-
 .../lucene/SortedMultiColumnQueryHits.java    |  48 ++-
 .../core/query/lucene/TermDocsCache.java      |   7 +-
 .../query/lucene/UpperCaseSortComparator.java |  66 +--
 .../core/query/lucene/VolatileIndex.java      |   3 +-
 .../query/lucene/WeightedHighlighter.java     |   4 +-
 .../core/query/lucene/WildcardNameQuery.java  |   9 -
 .../core/query/lucene/WildcardQuery.java      |  92 +++--
 .../lucene/directory/FSDirectoryManager.java  |  44 +-
 .../lucene/hits/AbstractHitCollector.java     |  60 +++
 .../core/query/lucene/hits/ScorerHits.java    |  11 +-
 .../query/lucene/join/SameNodeJoinMerger.java |   2 -
 .../core/query/lucene/join/ScoreNodeMap.java  |   5 +-
 .../query/lucene/ChainedTermEnumTest.java     |  18 +-
 .../core/query/lucene/IndexMigrationTest.java |  12 +-
 jackrabbit-parent/pom.xml                     |   2 +-
 81 files changed, 1546 insertions(+), 1359 deletions(-)
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractScoreDocComparator.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorBase.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorDecorator.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldComparatorSource.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/AbstractHitCollector.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
index 3ea0e4050..12d369776 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
@@ -123,7 +123,7 @@ public class PropertyTypeRegistry implements NodeTypeRegistryListener {
         // remove all TypeMapping instances refering to this ntName
         synchronized (typeMapping) {
             Map<Name, TypeMapping[]> modified = new HashMap<Name, TypeMapping[]>();
            for (Iterator it = typeMapping.keySet().iterator(); it.hasNext();) {
            for (Iterator<Name> it = typeMapping.keySet().iterator(); it.hasNext();) {
                 Name propName = (Name) it.next();
                 TypeMapping[] mapping = typeMapping.get(propName);
                 List<TypeMapping> remove = null;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractExcerpt.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractExcerpt.java
index 819c935aa..1e84bbc1f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractExcerpt.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractExcerpt.java
@@ -28,7 +28,8 @@ import org.apache.lucene.index.TermVectorOffsetInfo;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.jackrabbit.core.id.NodeId;
 
 import java.io.IOException;
@@ -231,10 +232,11 @@ public abstract class AbstractExcerpt implements HighlightingExcerptProvider {
             new TreeMap<String, TermVectorOffsetInfo[]>();
         Reader r = new StringReader(text);
         TokenStream ts = index.getTextAnalyzer().tokenStream("", r);
        Token t = new Token();
         try {
            while ((t = ts.next(t)) != null) {
                String termText = t.term();
            while (ts.incrementToken()) {
                OffsetAttribute offset = ts.getAttribute(OffsetAttribute.class);
                TermAttribute term = ts.getAttribute(TermAttribute.class);
                String termText = term.term();
                 TermVectorOffsetInfo[] info = termMap.get(termText);
                 if (info == null) {
                     info = new TermVectorOffsetInfo[1];
@@ -244,9 +246,11 @@ public abstract class AbstractExcerpt implements HighlightingExcerptProvider {
                     System.arraycopy(tmp, 0, info, 0, tmp.length);
                 }
                 info[info.length - 1] = new TermVectorOffsetInfo(
                        t.startOffset(), t.endOffset());
                    offset.startOffset(), offset.endOffset());
                 termMap.put(termText, info);
             }
            ts.end();
            ts.close();
         } catch (IOException e) {
             // should never happen, we are reading from a string
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
index 28ad9f7b6..11bcad0f7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractIndex.java
@@ -16,16 +16,6 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
@@ -40,6 +30,16 @@ import org.apache.tika.io.IOExceptionWithCause;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

 /**
  * Implements common functionality for a lucene index.
  * <p/>
@@ -248,13 +248,7 @@ abstract class AbstractIndex {
         }
         if (indexReader == null) {
             IndexDeletionPolicy idp = getIndexDeletionPolicy();
            IndexReader reader;
            if (idp != null) {
                reader = IndexReader.open(getDirectory(), idp);
            } else {
                reader = IndexReader.open(getDirectory());
            }
            reader.setTermInfosIndexDivisor(termInfosIndexDivisor);
            IndexReader reader = IndexReader.open(getDirectory(), idp, false, termInfosIndexDivisor);
             indexReader = new CommittableIndexReader(reader);
         }
         return indexReader;
@@ -318,8 +312,7 @@ abstract class AbstractIndex {
         }
         if (sharedReader == null) {
             // create new shared reader
            IndexReader reader = IndexReader.open(getDirectory(), true);
            reader.setTermInfosIndexDivisor(termInfosIndexDivisor);
            IndexReader reader = IndexReader.open(getDirectory(), null, true, termInfosIndexDivisor);
             CachingIndexReader cr = new CachingIndexReader(
                     reader, cache, initCache);
             sharedReader = new SharedIndexReader(cr);
@@ -496,10 +489,10 @@ abstract class AbstractIndex {
             // mark the document that reindexing is required
             copy.add(new Field(FieldNames.REINDEXING_REQUIRED, "",
                     Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
            for (Fieldable f : (List<Fieldable>) doc.getFields()) {
            for (Fieldable f : doc.getFields()) {
                 Fieldable field = null;
                 Field.TermVector tv = getTermVectorParameter(f);
                Field.Store stored = getStoreParameter(f);
                Field.Store stored = f.isStored() ? Field.Store.YES : Field.Store.NO;
                 Field.Index indexed = getIndexParameter(f);
                 if (f instanceof LazyTextExtractorField || f.readerValue() != null) {
                     // replace all readers with empty string reader
@@ -508,7 +501,7 @@ abstract class AbstractIndex {
                     field = new Field(f.name(), f.stringValue(),
                             stored, indexed, tv);
                 } else if (f.isBinary()) {
                    field = new Field(f.name(), f.binaryValue(), stored);
                    field = new Field(f.name(), f.getBinaryValue(), stored);
                 }
                 if (field != null) {
                     field.setOmitNorms(f.getOmitNorms());
@@ -584,22 +577,6 @@ abstract class AbstractIndex {
         }
     }
 
    /**
     * Returns the store parameter set on <code>f</code>.
     *
     * @param f a lucene field.
     * @return the store parameter on <code>f</code>.
     */
    private Field.Store getStoreParameter(Fieldable f) {
        if (f.isCompressed()) {
            return Field.Store.COMPRESS;
        } else if (f.isStored()) {
            return Field.Store.YES;
        } else {
            return Field.Store.NO;
        }
    }

     /**
      * Returns the term vector parameter set on <code>f</code>.
      *
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractScoreDocComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractScoreDocComparator.java
deleted file mode 100644
index 45b54c318..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractScoreDocComparator.java
++ /dev/null
@@ -1,124 +0,0 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.index.IndexReader;

/**
 * Abstract base class of {@link ScoreDocComparator} implementations.
 */
abstract class AbstractScoreDocComparator implements ScoreDocComparator {

    /**
     * The index readers.
     */
    protected final List<IndexReader> readers = new ArrayList<IndexReader>();

    /**
     * The document number starts for the {@link #readers}.
     */
    protected final int[] starts;

    public AbstractScoreDocComparator(IndexReader reader)
            throws IOException {
        getIndexReaders(readers, reader);

        int maxDoc = 0;
        this.starts = new int[readers.size() + 1];

        for (int i = 0; i < readers.size(); i++) {
            IndexReader r = readers.get(i);
            starts[i] = maxDoc;
            maxDoc += r.maxDoc();
        }
        starts[readers.size()] = maxDoc;
    }

    /**
     * Compares sort values of <code>i</code> and <code>j</code>. If the
     * sort values have differing types, then the sort order is defined on
     * the type itself by calling <code>compareTo()</code> on the respective
     * type class names.
     *
     * @param i first score doc.
     * @param j second score doc.
     * @return a negative integer if <code>i</code> should come before
     *         <code>j</code><br> a positive integer if <code>i</code>
     *         should come after <code>j</code><br> <code>0</code> if they
     *         are equal
     */
    public int compare(ScoreDoc i, ScoreDoc j) {
        return Util.compare(sortValue(i), sortValue(j));
    }

    public int sortType() {
        return SortField.CUSTOM;
    }

    /**
     * Returns the reader index for document <code>n</code>.
     *
     * @param n document number.
     * @return the reader index.
     */
    protected int readerIndex(int n) {
        int lo = 0;
        int hi = readers.size() - 1;

        while (hi >= lo) {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue) {
                hi = mid - 1;
            } else if (n > midValue) {
                lo = mid + 1;
            } else {
                while (mid + 1 < readers.size() && starts[mid + 1] == midValue) {
                    mid++;
                }
                return mid;
            }
        }
        return hi;
    }

    /**
     * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
     * that's the case calls this method recursively for each reader within the
     * multi index reader; otherwise the reader is simply added to the list.
     *
     * @param readers the list of index readers.
     * @param reader  the reader to check.
     */
    private static void getIndexReaders(List<IndexReader> readers,
                                        IndexReader reader) {
        if (reader instanceof MultiIndexReader) {
            for (IndexReader r : ((MultiIndexReader) reader).getIndexReaders()) {
                getIndexReaders(readers, r);
            }
        } else {
            readers.add(reader);
        }
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractWeight.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractWeight.java
index b4ae1c501..70792b63b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractWeight.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractWeight.java
@@ -27,7 +27,8 @@ import java.io.IOException;
  * <code>AbstractWeight</code> implements base functionality for custom lucene
  * weights in jackrabbit.
  */
abstract class AbstractWeight implements Weight {
@SuppressWarnings("serial")
abstract class AbstractWeight extends Weight {
 
     /**
      * The searcher for this weight.
@@ -52,8 +53,8 @@ abstract class AbstractWeight implements Weight {
      * @return the scorer instance
      * @throws IOException if an error occurs while reading from the index
      */
    protected abstract Scorer createScorer(IndexReader reader)
            throws IOException;
    protected abstract Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException;
 
     /**
      * {@inheritDoc}
@@ -61,7 +62,8 @@ abstract class AbstractWeight implements Weight {
      * Returns a {@link MultiScorer} if the passed <code>reader</code> is of
      * type {@link MultiIndexReader}.
      */
    public Scorer scorer(IndexReader reader) throws IOException {
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException {
         if (reader instanceof MultiIndexReader) {
             MultiIndexReader mir = (MultiIndexReader) reader;
             IndexReader[] readers = mir.getIndexReaders();
@@ -75,12 +77,12 @@ abstract class AbstractWeight implements Weight {
             starts[readers.length] = maxDoc;
             Scorer[] scorers = new Scorer[readers.length];
             for (int i = 0; i < readers.length; i++) {
                scorers[i] = scorer(readers[i]);
                scorers[i] = scorer(readers[i], scoreDocsInOrder, topScorer);
             }
 
             return new MultiScorer(searcher.getSimilarity(), scorers, starts);
         } else {
            return createScorer(reader);
            return createScorer(reader, scoreDocsInOrder, topScorer);
         }
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingIndexReader.java
index a59960700..1075a40e9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingIndexReader.java
@@ -17,7 +17,6 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldSelector;
 import org.apache.lucene.index.FilterIndexReader;
 import org.apache.lucene.index.IndexReader;
@@ -287,7 +286,7 @@ class CachingIndexReader extends FilterIndexReader {
      * @throws IOException if an error occurs while reading from the index.
      */
     public TermDocs termDocs(Term term) throws IOException {
        if (term.field() == FieldNames.UUID) {
        if (term != null && term.field() == FieldNames.UUID) {
             // check cache if we have one
             if (cache != null) {
                 DocNumberCache.Entry e = cache.get(term.text());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingMultiIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingMultiIndexReader.java
index 612acc574..026792f46 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingMultiIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CachingMultiIndexReader.java
@@ -110,7 +110,7 @@ public final class CachingMultiIndexReader
      * {@inheritDoc}
      */
     public TermDocs termDocs(Term term) throws IOException {
        if (term.field() == FieldNames.UUID) {
        if (term != null && term.field() == FieldNames.UUID) {
             // check cache
             DocNumberCache.Entry e = cache.get(term.text());
             if (e != null) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CaseTermQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CaseTermQuery.java
index 24c661c59..76a769d89 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CaseTermQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/CaseTermQuery.java
@@ -21,6 +21,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.TermEnum;
 import org.apache.lucene.search.MultiTermQuery;
 import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.util.ToStringUtils;
 
 import java.io.IOException;
 import java.util.List;
@@ -33,6 +34,7 @@ import java.util.Map;
  * <code>CaseTermQuery</code> implements a term query which convert the term
  * from the index either to upper or lower case before it is matched.
  */
@SuppressWarnings("serial")
 abstract class CaseTermQuery extends MultiTermQuery implements TransformConstants {
 
     /**
@@ -40,19 +42,34 @@ abstract class CaseTermQuery extends MultiTermQuery implements TransformConstant
      * upper-cased.
      */
     protected final int transform;
    private final Term term;
 
     CaseTermQuery(Term term, int transform) {
        super(term);
        this.term = term;
         this.transform = transform;
     }
 
     /**
      * {@inheritDoc}
      */
    @Override
     protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
         return new CaseTermEnum(reader);
     }
 
    /** Prints a user-readable version of this query. */
    @Override
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(':');
        }
        buffer.append(term.text());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

     static final class Upper extends CaseTermQuery {
 
         Upper(Term term) {
@@ -65,7 +82,6 @@ abstract class CaseTermQuery extends MultiTermQuery implements TransformConstant
         Lower(Term term) {
             super(term, TRANSFORM_LOWER_CASE);
         }

     }
 
     private final class CaseTermEnum extends FilteredTermEnum {
@@ -76,9 +92,7 @@ abstract class CaseTermQuery extends MultiTermQuery implements TransformConstant
             final Map<Term, Integer> orderedTerms =
                 new LinkedHashMap<Term, Integer>();
 
            Term term = getTerm();

            // there are always two range scanse: one with an initial
            // there are always two range scans: one with an initial
             // lower case character and another one with an initial upper case
             // character
             List<RangeScan> rangeScans = new ArrayList<RangeScan>(2);
@@ -158,20 +172,24 @@ abstract class CaseTermQuery extends MultiTermQuery implements TransformConstant
                     getNext();
                 }
 
                @Override
                 public boolean next() {
                     getNext();
                     return current != null;
                 }
 
                @Override
                 public Term term() {
                     return current;
                 }
 
                @Override
                 public int docFreq() {
                     Integer docFreq = orderedTerms.get(current);
                     return docFreq != null ? docFreq : 0;
                 }
 
                @Override
                 public void close() {
                     // nothing to close
                 }
@@ -182,15 +200,18 @@ abstract class CaseTermQuery extends MultiTermQuery implements TransformConstant
             });
         }
 
        @Override
         protected boolean termCompare(Term term) {
             // they all match
             return true;
         }
 
        @Override
         public float difference() {
             return 1.0f;
         }
 
        @Override
         protected boolean endEnum() {
             // todo correct?
             return false;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
index e5ba9848b..52f6fa641 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.jackrabbit.core.query.lucene.hits.AdaptingHits;
 import org.apache.jackrabbit.core.query.lucene.hits.Hits;
 import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
@@ -32,7 +33,6 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
@@ -54,6 +54,7 @@ import java.util.ArrayList;
  * Implements a lucene <code>Query</code> which returns the child nodes of the
  * nodes selected by another <code>Query</code>.
  */
@SuppressWarnings("serial")
 class ChildAxisQuery extends Query implements JackrabbitQuery {
 
     /**
@@ -192,7 +193,7 @@ class ChildAxisQuery extends Query implements JackrabbitQuery {
      * @param searcher the <code>Searcher</code> instance to use.
      * @return a <code>ChildAxisWeight</code>.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new ChildAxisWeight(searcher);
     }
 
@@ -273,7 +274,7 @@ class ChildAxisQuery extends Query implements JackrabbitQuery {
     /**
      * The <code>Weight</code> implementation for this <code>ChildAxisQuery</code>.
      */
    private class ChildAxisWeight implements Weight {
    private class ChildAxisWeight extends Weight {
 
         /**
          * The searcher in use
@@ -326,10 +327,11 @@ class ChildAxisQuery extends Query implements JackrabbitQuery {
          * @return a <code>ChildAxisScorer</code>.
          * @throws IOException if an error occurs while reading from the index.
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
        @Override
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             if (nameTest != null) {
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader);
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             }
             return new ChildAxisScorer(searcher.getSimilarity(),
                     reader, (HierarchyResolver) reader);
@@ -385,52 +387,48 @@ class ChildAxisQuery extends Query implements JackrabbitQuery {
             this.hResolver = hResolver;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             do {
                 nextDoc = hits.next();
             } while (nextDoc > -1 && !indexIsValid(nextDoc));
 
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             nextDoc = hits.skipTo(target);
             while (nextDoc > -1 && !indexIsValid(nextDoc)) {
                next();
                nextDoc();
             }
            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
         private void calculateChildren() throws IOException {
@@ -440,19 +438,21 @@ class ChildAxisQuery extends Query implements JackrabbitQuery {
                 if (nameTestScorer == null) {
                     // always use simple in that case
                     calc[0] = new SimpleChildrenCalculator(reader, hResolver);
                    contextScorer.score(new HitCollector() {
                        public void collect(int doc, float score) {
                    contextScorer.score(new AbstractHitCollector() {
                        @Override
                        protected void collect(int doc, float score) {
                             calc[0].collectContextHit(doc);
                         }
                     });
                 } else {
                     // start simple but switch once threshold is reached
                     calc[0] = new SimpleChildrenCalculator(reader, hResolver);
                    contextScorer.score(new HitCollector() {
                    contextScorer.score(new AbstractHitCollector() {
 
                         private List<Integer> docIds = new ArrayList<Integer>();
 
                        public void collect(int doc, float score) {
                        @Override
                        protected void collect(int doc, float score) {
                             calc[0].collectContextHit(doc);
                             if (docIds != null) {
                                 docIds.add(doc);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultHighlighter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultHighlighter.java
index 6c70ca251..6fa989203 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultHighlighter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultHighlighter.java
@@ -300,8 +300,8 @@ public class DefaultHighlighter {
                     new String(cbuf, skippedChars, cbuf.length - skippedChars)));
 
             // iterate terms
            for (Iterator iter = fi.iterator(); iter.hasNext();) {
                TermVectorOffsetInfo ti = (TermVectorOffsetInfo) iter.next();
            for (Iterator<TermVectorOffsetInfo> iter = fi.iterator(); iter.hasNext();) {
                TermVectorOffsetInfo ti = iter.next();
                 nextStart = ti.getStartOffset();
                 if (nextStart - pos > 0) {
                     cbuf = new char[nextStart - pos];
@@ -420,11 +420,10 @@ public class DefaultHighlighter {
             }
             offsetInfosList.add(offsetinfo);
             numTerms++;
            endOffset = offsetinfo.getEndOffset();
             return true;
         }
 
        public Iterator iterator() {
        public Iterator<TermVectorOffsetInfo> iterator() {
             return offsetInfosList.iterator();
         }
 
@@ -432,10 +431,6 @@ public class DefaultHighlighter {
             return startOffset;
         }
 
        public int getEndOffset() {
            return endOffset;
        }

         public int numTerms() {
             return numTerms;
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultQueryHits.java
index ad400e0ce..f602087d6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DefaultQueryHits.java
@@ -34,7 +34,7 @@ public class DefaultQueryHits extends AbstractQueryHits {
     /**
      * An iterator over the query nodes.
      */
    private final Iterator scoreNodes;
    private final Iterator<ScoreNode> scoreNodes;
 
     /**
      * Creates a new <code>DefaultQueryHits</code> instance based on the passed
@@ -42,7 +42,7 @@ public class DefaultQueryHits extends AbstractQueryHits {
      *
      * @param scoreNodes a collection of {@link ScoreNode}s.
      */
    public DefaultQueryHits(Collection scoreNodes) {
    public DefaultQueryHits(Collection<ScoreNode> scoreNodes) {
         this.size = scoreNodes.size();
         this.scoreNodes = scoreNodes.iterator();
     }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DerefQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DerefQuery.java
index 2f1d18461..b72886139 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DerefQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DerefQuery.java
@@ -17,15 +17,14 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Weight;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.jackrabbit.spi.Name;
 
 import java.io.IOException;
@@ -38,6 +37,7 @@ import java.util.Set;
  * Implements a lucene <code>Query</code> which returns the nodes selected by
  * a reference property of the context node.
  */
@SuppressWarnings("serial")
 class DerefQuery extends Query {
 
     /**
@@ -102,7 +102,7 @@ class DerefQuery extends Query {
      * @param searcher the <code>Searcher</code> instance to use.
      * @return a <code>DerefWeight</code>.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new DerefWeight(searcher);
     }
 
@@ -148,7 +148,7 @@ class DerefQuery extends Query {
     /**
      * The <code>Weight</code> implementation for this <code>DerefQuery</code>.
      */
    private class DerefWeight implements Weight {
    private class DerefWeight extends Weight {
 
         /**
          * The searcher in use
@@ -201,10 +201,12 @@ class DerefQuery extends Query {
          * @return a <code>DerefScorer</code>.
          * @throws IOException if an error occurs while reading from the index.
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
        @Override
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             if (nameTest != null) {
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader);
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             }
             return new DerefScorer(searcher.getSimilarity(), reader);
         }
@@ -256,46 +258,42 @@ class DerefQuery extends Query {
             this.hits = new BitSet(reader.maxDoc());
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
         /**
@@ -311,8 +309,9 @@ class DerefQuery extends Query {
         private void calculateChildren() throws IOException {
             if (uuids == null) {
                 uuids = new ArrayList<String>();
                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                contextScorer.score(new AbstractHitCollector() {
                    @Override
                    protected void collect(int doc, float score) {
                         hits.set(doc);
                     }
                 });
@@ -320,8 +319,9 @@ class DerefQuery extends Query {
                 // collect nameTest hits
                 final BitSet nameTestHits = new BitSet();
                 if (nameTestScorer != null) {
                    nameTestScorer.score(new HitCollector() {
                        public void collect(int doc, float score) {
                    nameTestScorer.score(new AbstractHitCollector() {
                        @Override
                        protected void collect(int doc, float score) {
                             nameTestHits.set(doc);
                         }
                     });
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
index ace8da743..894444da9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
@@ -16,16 +16,16 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
 import org.apache.lucene.search.Sort;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.lucene.search.Weight;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -33,16 +33,17 @@ import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import java.io.IOException;
 import java.util.BitSet;
import java.util.Set;
import java.util.Iterator;
 import java.util.Map;
import java.util.Set;
 import java.util.TreeMap;
import java.util.Iterator;
 
 /**
  * Implements a lucene <code>Query</code> which filters a sub query by checking
  * whether the nodes selected by that sub query are descendants or self of
  * nodes selected by a context query.
  */
@SuppressWarnings("serial")
 class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
 
     /**
@@ -171,7 +172,7 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
      * @param searcher the <code>Searcher</code> instance to use.
      * @return a <code>DescendantSelfAxisWeight</code>.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new DescendantSelfAxisWeight(searcher);
     }
 
@@ -321,7 +322,7 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
      * The <code>Weight</code> implementation for this
      * <code>DescendantSelfAxisWeight</code>.
      */
    private class DescendantSelfAxisWeight implements Weight {
    private class DescendantSelfAxisWeight extends Weight {
 
         /**
          * The searcher in use
@@ -376,9 +377,10 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
          * @return a <code>DescendantSelfAxisScorer</code>.
          * @throws IOException if an error occurs while reading from the index.
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
            subScorer = subQuery.weight(searcher).scorer(reader);
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
            subScorer = subQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             HierarchyResolver resolver = (HierarchyResolver) reader;
             return new DescendantSelfAxisScorer(searcher.getSimilarity(), reader, resolver);
         }
@@ -428,6 +430,11 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
          */
         private final int[] singleDoc = new int[1];
 
        /**
         * The next document id to be returned
         */
        private int currentDoc = -1;

         /**
          * Creates a new <code>DescendantSelfAxisScorer</code>.
          *
@@ -444,59 +451,59 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
             this.contextHits = new BitSet(reader.maxDoc());
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            collectContextHits();
            if (!subScorer.next() || contextHits.isEmpty()) {
                return false;
        @Override
        public int nextDoc() throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
             }
            int nextDoc = subScorer.doc();
            while (nextDoc > -1) {
 
                if (isValid(nextDoc)) {
                    return true;
            collectContextHits();
            currentDoc = subScorer.nextDoc();
            if (contextHits.isEmpty()) {
                currentDoc = NO_MORE_DOCS;
            }
            while (currentDoc != NO_MORE_DOCS) {
                if (isValid(currentDoc)) {
                    return currentDoc;
                 }
 
                 // try next
                nextDoc = subScorer.next() ? subScorer.doc() : -1;
                currentDoc = subScorer.nextDoc();
             }
            return false;
            return currentDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
            return subScorer.doc();
        @Override
        public int docID() {
            return currentDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             return subScorer.score();
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
            boolean match = subScorer.skipTo(target);
            if (match) {
                collectContextHits();
                return isValid(subScorer.doc()) || next();
        @Override
        public int advance(int target) throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
            }

            currentDoc = subScorer.nextDoc();
            if (currentDoc == NO_MORE_DOCS) {
                return NO_MORE_DOCS;
             } else {
                return false;
                collectContextHits();
                return isValid(currentDoc) ? currentDoc : nextDoc();
             }
         }
 
         private void collectContextHits() throws IOException {
             if (!contextHitsCalculated) {
                 long time = System.currentTimeMillis();
                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                contextScorer.score(new AbstractHitCollector() {
                    @Override
                    protected void collect(int doc, float score) {
                         contextHits.set(doc);
                     }
                 }); // find all
@@ -513,14 +520,6 @@ class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
             }
         }
 
        /**
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }

         /**
          * Returns <code>true</code> if <code>doc</code> is a valid match from
          * the sub scorer against the context hits. The caller must ensure
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DocOrderScoreNodeIterator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DocOrderScoreNodeIterator.java
index af3315b3e..2ab79f78b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DocOrderScoreNodeIterator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DocOrderScoreNodeIterator.java
@@ -270,6 +270,7 @@ class DocOrderScoreNodeIterator implements ScoreNodeIterator {
     /**
      * Indicates that sorting failed.
      */
    @SuppressWarnings("serial")
     private static final class SortFailedException extends RuntimeException {
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorBase.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorBase.java
new file mode 100644
index 000000000..5b572c58c
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorBase.java
@@ -0,0 +1,105 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.FieldComparator;

import java.io.IOException;

/**
 * Abstract base class for <code>FieldComparator</code> implementations
 * which are based on values in the form of <code>Comparables</code>.
 */
abstract class FieldComparatorBase extends FieldComparator {

    /**
     * The bottom value.
     */
    private Comparable bottom;

    /**
     * Value for a document
     *
     * @param doc  id of the document
     * @return  the value for the given id
     */
    protected abstract Comparable sortValue(int doc);

    /**
     * Retrieves the value of a given slot
     *
     * @param slot  index of the value to retrieve
     * @return  the value in the given slot
     */
    protected abstract Comparable getValue(int slot);

    /**
     * Puts a value into a given slot
     *
     * @param slot  index where to put the value
     * @param value  the value to put into the given slot
     */
    protected abstract void setValue(int slot, Comparable value);

    @Override
    public int compare(int slot1, int slot2) {
        return compare(getValue(slot1), getValue(slot2));
    }

    @Override
    public int compareBottom(int doc) throws IOException {
        return compare(bottom, sortValue(doc));
    }

    @Override
    public void setBottom(int slot) {
        bottom = getValue(slot);
    }

    /**
     * Compare two values
     *
     * @param val1  first value
     * @param val2  second value
     * @return  A negative integer if <code>val1</code> comes before <code>val2</code>,
     *   a positive integer if <code>val1</code> comes after <code>val2</code> and
     *   <code>0</code> if <code>val1</code> and <code>val2</code> are equal.
     */
    protected int compare(Comparable val1, Comparable val2) {
        if (val1 == null) {
            if (val2 == null) {
                return 0;
            }
            return -1;
        }
        else if (val2 == null) {
            return 1;
        }
        return Util.compare(val1, val2);
    }

    @Override
    public void copy(int slot, int doc) throws IOException {
        setValue(slot, sortValue(doc));
    }

    @Override
    public Comparable value(int slot) {
        return getValue(slot);
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorDecorator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorDecorator.java
new file mode 100644
index 000000000..d3ea3e1ac
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldComparatorDecorator.java
@@ -0,0 +1,62 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Implements a <code>FieldComparator</code> which decorates a
 * base comparator.
 */
abstract class FieldComparatorDecorator extends FieldComparatorBase {

    /**
     * The base comparator
     */
    private final FieldComparatorBase base;

    /**
     * Create a new instance which delegates to a base comparator.
     * @param base  delegatee
     */
    public FieldComparatorDecorator(FieldComparatorBase base) {
        this.base = base;
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        base.setNextReader(reader, docBase);
    }

    @Override
    protected Comparable sortValue(int doc) {
        return base.sortValue(doc);
    }

    @Override
    protected Comparable getValue(int slot) {
        return base.getValue(slot);
    }

    @Override
    protected void setValue(int slot, Comparable value) {
        base.setValue(slot, value);
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldSelectors.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldSelectors.java
index 87a63ea4a..01796033e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldSelectors.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FieldSelectors.java
@@ -30,6 +30,7 @@ public class FieldSelectors {
     private FieldSelectors() {
     }
 
    @SuppressWarnings("serial")
     public static final FieldSelector UUID = new FieldSelector() {
         /**
          * Only accepts {@link FieldNames#UUID}.
@@ -46,6 +47,7 @@ public class FieldSelectors {
         }
     };
 
    @SuppressWarnings("serial")
     public static final FieldSelector UUID_AND_PARENT = new FieldSelector() {
         /**
          * Accepts {@link FieldNames#UUID} and {@link FieldNames#PARENT}.
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IDField.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IDField.java
index fd97d0e18..9105980bc 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IDField.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IDField.java
@@ -37,7 +37,7 @@ public class IDField extends AbstractField {
         this.isStored = true;
         this.isTokenized = false;
         this.omitNorms = true;
        this.omitTf = true;
        this.omitTermFreqAndPositions = true;
     }
 
     public String stringValue() {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexDeletionPolicyImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexDeletionPolicyImpl.java
index db14989ab..ff29e6a18 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexDeletionPolicyImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexDeletionPolicyImpl.java
@@ -79,7 +79,7 @@ public class IndexDeletionPolicyImpl implements IndexDeletionPolicy {
 
     void readCurrentGeneration() throws IOException {
         Directory dir = index.getDirectory();
        String[] names = dir.list();
        String[] names = dir.listAll();
         long max = 0;
         if (names != null) {
             for (String name : names) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
index b55625c40..6596051d2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
@@ -102,7 +102,8 @@ public class IndexFormatVersion {
      * index reader.
      */
     public static IndexFormatVersion getVersion(IndexReader indexReader) {
        Collection fields = indexReader.getFieldNames(
        @SuppressWarnings("unchecked")
        Collection<String> fields = indexReader.getFieldNames(
                 IndexReader.FieldOption.ALL);
         if (fields.contains(FieldNames.LOCAL_NAME) || indexReader.numDocs() == 0) {
             return IndexFormatVersion.V3;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexHistory.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexHistory.java
index bc64f59c6..d5b28d531 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexHistory.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexHistory.java
@@ -72,7 +72,7 @@ class IndexHistory {
         this.indexDir = dir;
         this.maxAge = maxAge;
         // read all index infos
        String[] names = dir.list();
        String[] names = dir.listAll();
         if (names != null) {
             for (String name : names) {
                 if (name.startsWith(INDEXES)) {
@@ -131,10 +131,10 @@ class IndexHistory {
     void pruneOutdated() {
         long threshold = System.currentTimeMillis() - maxAge;
         log.debug("Pruning index infos older than: " + threshold + "(" + indexDir + ")");
        Iterator it = indexInfosMap.values().iterator();
        Iterator<IndexInfos> it = indexInfosMap.values().iterator();
         // never prune the current generation
         if (it.hasNext()) {
            IndexInfos infos = (IndexInfos) it.next();
            IndexInfos infos = it.next();
             log.debug("Skipping first index infos. generation=" + infos.getGeneration());
         }
         while (it.hasNext()) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexInfos.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexInfos.java
index ae643ed9b..693d693d2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexInfos.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexInfos.java
@@ -157,8 +157,8 @@ class IndexInfos implements Cloneable {
                 dataOut.writeInt(WITH_GENERATION);
                 dataOut.writeInt(counter);
                 dataOut.writeInt(indexes.size());
                for (Iterator it = iterator(); it.hasNext(); ) {
                    IndexInfo info = (IndexInfo) it.next();
                for (Iterator<IndexInfo> it = iterator(); it.hasNext(); ) {
                    IndexInfo info = it.next();
                     dataOut.writeUTF(info.getName());
                     dataOut.writeLong(info.getGeneration());
                     log.debug("  + {}:{}", info.getName(), info.getGeneration());
@@ -185,7 +185,7 @@ class IndexInfos implements Cloneable {
      * @return an iterator over the {@link IndexInfo}s contained in this index
      *          infos.
      */
    Iterator iterator() {
    Iterator<IndexInfo> iterator() {
         return indexes.values().iterator();
     }
 
@@ -267,10 +267,11 @@ class IndexInfos implements Cloneable {
      *
      * @return a clone of this index infos.
      */
    @SuppressWarnings("unchecked")
     public IndexInfos clone() {
         try {
             IndexInfos clone = (IndexInfos) super.clone();
            clone.indexes = (LinkedHashMap) indexes.clone();
            clone.indexes = (LinkedHashMap<String, IndexInfo>) indexes.clone();
             for (Map.Entry<String, IndexInfo> entry : clone.indexes.entrySet()) {
                 entry.setValue(entry.getValue().clone());
             }
@@ -342,12 +343,15 @@ class IndexInfos implements Cloneable {
      * @return names of all generation files of this index infos.
      */
     private static String[] getFileNames(Directory directory, final String base) {
        String[] names = new String[0];
        String[] names = null;
         try {
            names = directory.list();
            names = directory.listAll();
         } catch (IOException e) {
             // TODO: log warning? or throw?
         }
        if (names == null) {
            return new String[0];
        }
         List<String> nameList = new ArrayList<String>(names.length);
         for (String n : names) {
             if (n.startsWith(base)) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMerger.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMerger.java
index 13ec1e177..fa0563b70 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMerger.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMerger.java
@@ -329,7 +329,7 @@ class IndexMerger implements IndexListener {
      * many document it contains. <code>Index</code> is comparable using the
      * number of documents it contains.
      */
    private static final class Index implements Comparable {
    private static final class Index implements Comparable<Index> {
 
         /**
          * The name of the index.
@@ -361,8 +361,7 @@ class IndexMerger implements IndexListener {
          *         Index is less than, equal to, or greater than the specified
          *         Index.
          */
        public int compareTo(Object o) {
            Index other = (Index) o;
        public int compareTo(Index other) {
             int val = numDocs < other.numDocs ? -1 : (numDocs == other.numDocs ? 0 : 1);
             if (val != 0) {
                 return val;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
index 2db7c3060..cb2468875 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexMigration.java
@@ -113,7 +113,7 @@ public class IndexMigration {
                     IndexWriter.MaxFieldLength.UNLIMITED);
             try {
                 IndexReader r = new MigrationIndexReader(
                        IndexReader.open(index.getDirectory()),
                        IndexReader.open(index.getDirectory(), true),
                         oldSeparatorChar);
                 try {
                     writer.addIndexes(new IndexReader[]{r});
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
index 96f82a454..f81186768 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
@@ -151,7 +151,8 @@ public class IndexingConfigurationImpl
                     if (analyzerNode.getNodeName().equals("analyzer")) {
                         String analyzerClassName = analyzerNode.getAttributes().getNamedItem("class").getNodeValue();
                         try {
                        Class clazz = Class.forName(analyzerClassName);
                            @SuppressWarnings("rawtypes")
                            Class clazz = Class.forName(analyzerClassName);
                             if (clazz == JackrabbitAnalyzer.class) {
                                 log.warn("Not allowed to configure " + JackrabbitAnalyzer.class.getName() +  " for a property. "
                                         + "Using default analyzer for that property.");
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingQueue.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingQueue.java
index 6a994094c..4be7f649d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingQueue.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingQueue.java
@@ -124,9 +124,9 @@ class IndexingQueue {
             finished.addAll(pendingDocuments.values());
         }
 
        Iterator it = finished.iterator();
        Iterator<Document> it = finished.iterator();
         while (it.hasNext()) {
            Document doc = (Document) it.next();
            Document doc = it.next();
             if (!Util.isDocumentReady(doc)) {
                 it.remove();
             }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
index cb9b4c045..9a7fbeeec 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitAnalyzer.java
@@ -18,10 +18,12 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import java.io.IOException;
 import java.io.Reader;
import java.util.Collections;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
 
 /**
  * This is the global jackrabbit lucene analyzer. By default, all
@@ -39,7 +41,7 @@ public class JackrabbitAnalyzer  extends Analyzer {
      * The default Jackrabbit analyzer if none is configured in <code><SearchIndex></code>
      * configuration.
      */
    private Analyzer defaultAnalyzer =  new StandardAnalyzer(new String[]{});
    private Analyzer defaultAnalyzer =  new StandardAnalyzer(Version.LUCENE_24, Collections.emptySet());
 
     /**
      * The indexing configuration.
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexReader.java
index 9410324d7..0f75f267d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexReader.java
@@ -106,6 +106,11 @@ public final class JackrabbitIndexReader
         return reader.getIndexReaders();
     }
 
    public IndexReader[] getSequentialSubReaders() {
      // No sequential sub-readers
      return null;
    }

     /**
      * {@inheritDoc}
      */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitParser.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitParser.java
index ae106333a..432e854c4 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitParser.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitParser.java
@@ -51,6 +51,7 @@ import org.xml.sax.SAXException;
  *
  * @since Apache Jackrabbit 2.0
  */
@SuppressWarnings("serial")
 class JackrabbitParser implements Parser {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQueryParser.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQueryParser.java
index 335d82b1f..03b60cee9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQueryParser.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQueryParser.java
@@ -24,6 +24,7 @@ import org.apache.lucene.queryParser.QueryParser;
 import org.apache.lucene.queryParser.ParseException;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.util.Version;
 
 /**
  * <code>JackrabbitQueryParser</code> extends the standard lucene query parser
@@ -50,7 +51,7 @@ public class JackrabbitQueryParser extends QueryParser {
                                  Analyzer analyzer,
                                  SynonymProvider synonymProvider,
                                  PerQueryCache cache) {
        super(fieldName, analyzer);
        super(Version.LUCENE_24, fieldName, analyzer);
         this.synonymProvider = synonymProvider;
         this.cache = cache;
         setAllowLeadingWildcard(true);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitTermQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitTermQuery.java
index 27e21f514..016aeda5f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitTermQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitTermQuery.java
@@ -39,7 +39,7 @@ public class JackrabbitTermQuery extends TermQuery {
         super(t);
     }
 
    protected Weight createWeight(Searcher searcher) throws IOException {
    public Weight createWeight(Searcher searcher) throws IOException {
         return new JackrabbitTermWeight(searcher, super.createWeight(searcher));
     }
 
@@ -63,8 +63,9 @@ public class JackrabbitTermQuery extends TermQuery {
         /**
          * {@inheritDoc}
          */
        protected Scorer createScorer(IndexReader reader) throws IOException {
            return weight.scorer(reader);
        protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            return weight.scorer(reader, scoreDocsInOrder, topScorer);
         }
 
         /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LazyTextExtractorField.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LazyTextExtractorField.java
index 41692c12d..aecd6d891 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LazyTextExtractorField.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LazyTextExtractorField.java
@@ -42,6 +42,7 @@ import org.xml.sax.helpers.DefaultHandler;
  *
  * @see #isExtractorFinished()
  */
@SuppressWarnings("serial")
 public class LazyTextExtractorField extends AbstractField {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LengthSortComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LengthSortComparator.java
index 377fb084b..408f5977e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LengthSortComparator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LengthSortComparator.java
@@ -16,23 +16,20 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;

import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.query.lucene.SharedFieldComparatorSource.SimpleFieldComparator;
 import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

import java.io.IOException;
 
 /**
 * <code>LengthSortComparator</code> implements a sort comparator source that
 * <code>LengthSortComparator</code> implements a <code>FieldComparator</code> which
  * sorts on the length of property values.
  */
public class LengthSortComparator implements SortComparatorSource {

    private static final long serialVersionUID = 2513564768671391632L;
public class LengthSortComparator extends FieldComparatorSource {
 
     /**
      * The index internal namespace mappings.
@@ -43,52 +40,15 @@ public class LengthSortComparator implements SortComparatorSource {
         this.nsMappings = nsMappings;
     }
 
    /**
     * Creates a new comparator.
     *
     * @param reader    the current index reader.
     * @param fieldname the name of the property to sort on. This is the string
     *                  representation of {@link org.apache.jackrabbit.spi.Name
     *                  Name}.
     * @return the score doc comparator.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String fieldname)
            throws IOException {
    @Override
    public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
         NameFactory factory = NameFactoryImpl.getInstance();
         try {
            return new Comparator(reader,
                    nsMappings.translateName(factory.create(fieldname)));
        } catch (IllegalNameException e) {
            return new SimpleFieldComparator(nsMappings.translateName(factory.create(fieldname)), FieldNames.PROPERTY_LENGTHS, numHits);
        }
        catch (IllegalNameException e) {
             throw Util.createIOException(e);
         }
     }
 
    private final class Comparator extends AbstractScoreDocComparator {

        /**
         * The term look ups of the index segments.
         */
        protected final SharedFieldCache.ValueIndex[] indexes;

        public Comparator(IndexReader reader,
                          String propertyName) throws IOException {
            super(reader);
            this.indexes = new SharedFieldCache.ValueIndex[readers.size()];

            String namedLength = FieldNames.createNamedValue(propertyName, "");
            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(
                        r, FieldNames.PROPERTY_LENGTHS,
                        namedLength, LengthSortComparator.this);
            }
        }

        public Comparable sortValue(ScoreDoc i) {
            int idx = readerIndex(i.doc);
            return indexes[idx].getValue(i.doc - starts[idx]);
        }
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameQuery.java
index db84203ca..45d79612f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameQuery.java
@@ -26,6 +26,7 @@ import java.util.Set;
 /**
  * <code>LocalNameQuery</code> implements a query for the local name of a node.
  */
@SuppressWarnings("serial")
 public class LocalNameQuery extends Query {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameRangeQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameRangeQuery.java
index f1036dc3a..db881fb72 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameRangeQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LocalNameRangeQuery.java
@@ -22,6 +22,7 @@ import org.apache.lucene.index.Term;
  * <code>LocalNameRangeQuery</code> implements a range query on the local name
  * of nodes.
  */
@SuppressWarnings("serial")
 public class LocalNameRangeQuery extends RangeQuery {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LowerCaseSortComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LowerCaseSortComparator.java
index a8b57ae70..6b65c48dd 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LowerCaseSortComparator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LowerCaseSortComparator.java
@@ -16,71 +16,43 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
 
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
 
 /**
 * <code>LowerCaseSortComparator</code> implements a sort comparator that
 * compares the lower-cased string values of a base sort comparator.
 * <code>LowerCaseSortComparator</code> implements a <code>FieldComparator</code> which
 * compares the lower-cased string values of a base comparator.
  */
public class LowerCaseSortComparator implements SortComparatorSource {

    private static final long serialVersionUID = 5396206509020979445L;
public class LowerCaseSortComparator extends FieldComparatorSource {
 
     /**
     * The base sort comparator.
     * The base comparator.
      */
    private final SortComparatorSource base;
    private final FieldComparatorSource base;
 
     /**
     * Creates a new lower case sort comparator.
     * Creates a new upper case sort comparator.
      *
      * @param base the base sort comparator source.
      */
    public LowerCaseSortComparator(SortComparatorSource base) {
    public LowerCaseSortComparator(FieldComparatorSource base) {
         this.base = base;
     }
 
    /**
     * {@inheritDoc}
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String fieldname)
            throws IOException {
        return new Comparator(base.newComparator(reader, fieldname));
    }

    private static final class Comparator implements ScoreDocComparator {

        private ScoreDocComparator base;

        private Comparator(ScoreDocComparator base) {
            this.base = base;
        }
    @Override
    public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
        FieldComparator comparator = base.newComparator(fieldname, numHits, sortPos, reversed);
        assert comparator instanceof FieldComparatorBase;
 
        /**
         * @see Util#compare(Comparable, Comparable)
         */
        public int compare(ScoreDoc i, ScoreDoc j) {
            return Util.compare(sortValue(i), sortValue(j));
        }

        public Comparable sortValue(ScoreDoc i) {
            Comparable c = base.sortValue(i);
            if (c != null) {
                return c.toString().toLowerCase();
            } else {
                return null;
        return new FieldComparatorDecorator((FieldComparatorBase) comparator) {
            @Override
            protected Comparable sortValue(int doc) {
                Comparable c = super.sortValue(doc);
                return c == null ? null : c.toString().toLowerCase();
             }
        }

        public int sortType() {
            return SortField.CUSTOM;
        }
        };
     }

 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
index 155168627..26515a696 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import java.io.IOException;
 
import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Scorer;
@@ -46,17 +47,22 @@ public class LuceneQueryHits implements QueryHits {
                            Query query)
             throws IOException {
         this.reader = reader;
        this.scorer = query.weight(searcher).scorer(reader);
        // We rely on Scorer#nextDoc() and Scorer#advance(int) so enable
        // scoreDocsInOrder
        this.scorer = query.weight(searcher).scorer(reader, true, false);
     }
 
     /**
      * {@inheritDoc}
      */
     public ScoreNode nextScoreNode() throws IOException {
        if (!scorer.next()) {
        if (scorer == null) {
            return null;
        }
        int doc = scorer.nextDoc();
        if (doc == DocIdSetIterator.NO_MORE_DOCS) {
             return null;
         }
        int doc = scorer.doc();
         NodeId id = new NodeId(reader.document(
                 doc, FieldSelectors.UUID).get(FieldNames.UUID));
         return new ScoreNode(id, scorer.score(), doc);
@@ -66,8 +72,10 @@ public class LuceneQueryHits implements QueryHits {
      * {@inheritDoc}
      */
     public void close() throws IOException {
        // make sure scorer frees resources
        scorer.skipTo(Integer.MAX_VALUE);
        if (scorer != null) {
            // make sure scorer frees resources
            scorer.advance(Integer.MAX_VALUE);
        }
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java
index 175abc1e3..5dcfa9803 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java
@@ -26,6 +26,7 @@ import java.io.IOException;
  * <code>MatchAllDocsQuery</code> extends the lucene <code>MatchAllDocsQuery</code>
  * and in addition implements {@link JackrabbitQuery}.
  */
@SuppressWarnings("serial")
 public class MatchAllDocsQuery
         extends org.apache.lucene.search.MatchAllDocsQuery
         implements JackrabbitQuery {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllQuery.java
index 3a4d1c9c3..a5c2c95e5 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllQuery.java
@@ -26,6 +26,7 @@ import java.util.Set;
  * Specialized query that returns / scores all pages in the search index.
  * <p>Use this Query to perform a match '*'.
  */
@SuppressWarnings("serial")
 class MatchAllQuery extends Query {
 
     private final String field;
@@ -54,7 +55,7 @@ class MatchAllQuery extends Query {
      * @param searcher the current searcher.
      * @return the <code>Weight</code> for this Query.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new MatchAllWeight(this, searcher, field, cache);
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllScorer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllScorer.java
index f0209bc4c..5ff98730e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllScorer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllScorer.java
@@ -20,15 +20,14 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Similarity;
 
 import java.io.IOException;
 import java.util.BitSet;
import java.util.Map;
 import java.util.HashMap;
import java.util.Map;
 
 /**
  * The MatchAllScorer implements a Scorer that scores / collects all
@@ -56,11 +55,6 @@ class MatchAllScorer extends Scorer {
      */
     private BitSet docFilter;
 
    /**
     * Explanation object. the same for all docs
     */
    private final Explanation matchExpl;

     /**
      * Creates a new MatchAllScorer.
      *
@@ -74,57 +68,49 @@ class MatchAllScorer extends Scorer {
         super(Similarity.getDefault());
         this.reader = reader;
         this.field = field;
        matchExpl
                = new Explanation(Similarity.getDefault().idf(reader.maxDoc(),
                        reader.maxDoc()),
                        "matchAll");
         calculateDocFilter(cache);
     }
 
    /**
     * {@inheritDoc}
     */
    public void score(HitCollector hc) throws IOException {
        while (next()) {
            hc.collect(doc(), score());
    @Override
    public void score(Collector collector) throws IOException {
        collector.setScorer(this);

        while (nextDoc() != NO_MORE_DOCS) {
            collector.collect(docID());
         }
     }
 
    /**
     * {@inheritDoc}
     */
    public boolean next() throws IOException {
    @Override
    public int nextDoc() throws IOException {
        if (nextDoc == NO_MORE_DOCS) {
            return nextDoc;
        }

         nextDoc = docFilter.nextSetBit(nextDoc + 1);
        return nextDoc > -1;
        if (nextDoc < 0) {
            nextDoc = NO_MORE_DOCS;
        }
        return nextDoc;
     }
 
    /**
     * {@inheritDoc}
     */
    public int doc() {
    @Override
    public int docID() {
         return nextDoc;
     }
 
    /**
     * {@inheritDoc}
     */
    @Override
     public float score() throws IOException {
         return 1.0f;
     }
 
    /**
     * {@inheritDoc}
     */
    public boolean skipTo(int target) throws IOException {
        nextDoc = target - 1;
        return next();
    }
    @Override
    public int advance(int target) throws IOException {
        if (nextDoc == NO_MORE_DOCS) {
            return nextDoc;
        }
 
    /**
     * {@inheritDoc}
     */
    public Explanation explain(int doc) {
        return matchExpl;
        nextDoc = target - 1;
        return nextDoc();
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllWeight.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllWeight.java
index 2f112ae6d..40c326860 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllWeight.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllWeight.java
@@ -28,6 +28,7 @@ import java.io.IOException;
 /**
  * This class implements the Weight calculation for the MatchAllQuery.
  */
@SuppressWarnings("serial")
 class MatchAllWeight extends AbstractWeight {
 
     /**
@@ -76,7 +77,8 @@ class MatchAllWeight extends AbstractWeight {
      * @param reader index reader
      * @return a {@link MatchAllScorer} instance
      */
    protected Scorer createScorer(IndexReader reader) throws IOException {
    protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException {
         return new MatchAllScorer(reader, field, cache);
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MoreLikeThis.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MoreLikeThis.java
index bcd8ff2af..e539ec091 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MoreLikeThis.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MoreLikeThis.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.Version;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermFreqVector;
@@ -27,11 +28,12 @@ import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.document.Document;
 
import java.util.List;
 import java.util.Set;
 import java.util.HashMap;
 import java.util.Map;
@@ -152,7 +154,7 @@ public final class MoreLikeThis {
      * Default analyzer to parse source doc with.
      * @see #getAnalyzer
      */
    public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();
    public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer(Version.LUCENE_24);
 
     /**
      * Ignore terms with less than this frequency in the source doc.
@@ -202,12 +204,12 @@ public final class MoreLikeThis {
      * @see #setStopWords
      * @see #getStopWords
      */
    public static final Set DEFAULT_STOP_WORDS = null;
    public static final Set<String> DEFAULT_STOP_WORDS = null;
 
     /**
      * Current set of stop words.
      */
    private Set stopWords = DEFAULT_STOP_WORDS;
    private Set<String> stopWords = DEFAULT_STOP_WORDS;
 
     /**
      * Return a Query with no more than this many terms.
@@ -448,7 +450,7 @@ public final class MoreLikeThis {
      * @see org.apache.lucene.analysis.StopFilter#makeStopSet StopFilter.makeStopSet()
      * @see #getStopWords
      */
    public void setStopWords(Set stopWords) {
    public void setStopWords(Set<String> stopWords) {
         this.stopWords = stopWords;
     }
 
@@ -456,7 +458,7 @@ public final class MoreLikeThis {
      * Get the current stop words being used.
      * @see #setStopWords
      */
    public Set getStopWords() {
    public Set<String> getStopWords() {
         return stopWords;
     }
 
@@ -504,8 +506,9 @@ public final class MoreLikeThis {
     public Query like(int docNum) throws IOException {
         if (fieldNames == null) {
             // gather list of valid fields from lucene
            Collection fields = ir.getFieldNames( IndexReader.FieldOption.INDEXED);
            fieldNames = (String[]) fields.toArray(new String[fields.size()]);
            @SuppressWarnings("unchecked")
            Collection<String> fields = ir.getFieldNames(IndexReader.FieldOption.INDEXED);
            fieldNames = fields.toArray(new String[fields.size()]);
         }
 
         return createQuery(retrieveTerms(docNum));
@@ -519,8 +522,9 @@ public final class MoreLikeThis {
     public Query like(File f) throws IOException {
         if (fieldNames == null) {
             // gather list of valid fields from lucene
            Collection fields = ir.getFieldNames( IndexReader.FieldOption.INDEXED);
            fieldNames = (String[]) fields.toArray(new String[fields.size()]);
            @SuppressWarnings("unchecked")
            Collection<String> fields = ir.getFieldNames(IndexReader.FieldOption.INDEXED);
            fieldNames = fields.toArray(new String[fields.size()]);
         }
 
         return like(new FileReader(f));
@@ -596,14 +600,14 @@ public final class MoreLikeThis {
      *
      * @param words a map of words keyed on the word(String) with Int objects as the values.
      */
    private PriorityQueue createQueue(Map words) throws IOException {
    private PriorityQueue createQueue(Map<String, Int> words) throws IOException {
         // have collected all words in doc and their freqs
         int numDocs = ir.numDocs();
         FreqQ res = new FreqQ(words.size()); // will order words by score
 
        Iterator it = words.keySet().iterator();
        Iterator<String> it = words.keySet().iterator();
         while (it.hasNext()) { // for every word
            String word = (String) it.next();
            String word = it.next();
 
             int tf = ((Int) words.get(word)).x; // term freq in the source doc
             if (minTermFreq > 0 && tf < minTermFreq) {
@@ -631,7 +635,7 @@ public final class MoreLikeThis {
             float score = tf * idf;
 
             // only really need 1st 3 entries, other ones are for troubleshooting
            res.insert(new Object[]{word,                   // the word
            res.insertWithOverflow(new Object[]{word,                   // the word
                                     topField,               // the top field
                                     new Float(score),       // overall score
                                     new Float(idf),         // idf
@@ -670,7 +674,7 @@ public final class MoreLikeThis {
      * @param docNum the id of the lucene document from which to find terms
      */
     public PriorityQueue retrieveTerms(int docNum) throws IOException {
        Map termFreqMap = new HashMap();
        Map<String, Int> termFreqMap = new HashMap<String, Int>();
         for (int i = 0; i < fieldNames.length; i++) {
             String fieldName = fieldNames[i];
             TermFreqVector vector = ir.getTermFreqVector(docNum, fieldName);
@@ -699,7 +703,7 @@ public final class MoreLikeThis {
      * @param termFreqMap a Map of terms and their frequencies
      * @param vector List of terms and their frequencies for a doc/field
      */
    private void addTermFrequencies(Map termFreqMap, TermFreqVector vector) {
    private void addTermFrequencies(Map<String, Int> termFreqMap, TermFreqVector vector) {
         String[] terms = vector.getTerms();
         int[] freqs = vector.getTermFrequencies();
         for (int j = 0; j < terms.length; j++) {
@@ -727,14 +731,14 @@ public final class MoreLikeThis {
      * @param termFreqMap a Map of terms and their frequencies
      * @param fieldName Used by analyzer for any special per-field analysis
      */
    private void addTermFrequencies(Reader r, Map termFreqMap, String fieldName)
    private void addTermFrequencies(Reader r, Map<String, Int> termFreqMap, String fieldName)
             throws IOException {
         TokenStream ts = analyzer.tokenStream(fieldName, r);
         int tokenCount = 0;
         // for every token
        final Token reusableToken = new Token();
        for (Token nextToken = ts.next(reusableToken); nextToken != null; nextToken = ts.next(reusableToken)) {
            String word = nextToken.term();
        while (ts.incrementToken()) {
            TermAttribute term = ts.getAttribute(TermAttribute.class);
            String word =  term.term();
             tokenCount++;
             if (tokenCount > maxNumTokensParsed) {
                 break;
@@ -744,13 +748,15 @@ public final class MoreLikeThis {
             }
 
             // increment frequency
            Int cnt = (Int) termFreqMap.get(word);
            Int cnt = termFreqMap.get(word);
             if (cnt == null) {
                 termFreqMap.put(word, new Int());
             } else {
                 cnt.x++;
             }
         }
        ts.end();
        ts.close();
     }
 
     /** determines if the passed term is likely to be of interest in "more like" comparisons
@@ -796,7 +802,7 @@ public final class MoreLikeThis {
      * @see #retrieveInterestingTerms
      */
     public PriorityQueue retrieveTerms(Reader r) throws IOException {
        Map words = new HashMap();
        Map<String, Int> words = new HashMap<String, Int>();
         for (int i = 0; i < fieldNames.length; i++) {
             String fieldName = fieldNames[i];
             addTermFrequencies(r, words, fieldName);
@@ -808,17 +814,16 @@ public final class MoreLikeThis {
      * @see #retrieveInterestingTerms(java.io.Reader)
      */
     public String[] retrieveInterestingTerms(int docNum) throws IOException {
        ArrayList al = new ArrayList(maxQueryTerms);
        List<String> al = new ArrayList<String>(maxQueryTerms);
         PriorityQueue pq = retrieveTerms(docNum);
         Object cur;
         int lim = maxQueryTerms; // have to be careful, retrieveTerms returns all words but that's probably not useful to our caller...
         // we just want to return the top words
         while (((cur = pq.pop()) != null) && lim-- > 0) {
             Object[] ar = (Object[]) cur;
            al.add(ar[0]); // the 1st entry is the interesting word
            al.add((String) ar[0]); // the 1st entry is the interesting word
         }
        String[] res = new String[al.size()];
        return (String[]) al.toArray(res);
        return al.toArray(new String[al.size()]);
     }
 
     /**
@@ -831,17 +836,16 @@ public final class MoreLikeThis {
      * @see #setMaxQueryTerms
      */
     public String[] retrieveInterestingTerms(Reader r) throws IOException {
        ArrayList al = new ArrayList(maxQueryTerms);
        List<String> al = new ArrayList<String>(maxQueryTerms);
         PriorityQueue pq = retrieveTerms(r);
         Object cur;
         int lim = maxQueryTerms; // have to be careful, retrieveTerms returns all words but that's probably not useful to our caller...
         // we just want to return the top words
         while (((cur = pq.pop()) != null) && lim-- > 0) {
             Object[] ar = (Object[]) cur;
            al.add(ar[0]); // the 1st entry is the interesting word
            al.add((String) ar[0]); // the 1st entry is the interesting word
         }
        String[] res = new String[al.size()];
        return (String[]) al.toArray(res);
        return al.toArray(new String[al.size()]);
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiScorer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiScorer.java
index 9efa0de38..528f73514 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiScorer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MultiScorer.java
@@ -17,7 +17,6 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.Similarity;
 
 import java.io.IOException;
@@ -42,12 +41,12 @@ class MultiScorer extends Scorer {
     /**
      * Index of the current scorer.
      */
    private int current = 0;
    private int currentScorer;
 
     /**
     * Indicates if there are more documents.
     * The next document id to be returned
      */
    private boolean hasNext = true;
    private int currentDoc = -1;
 
     /**
      * Creates a new <code>MultiScorer</code> that spans multiple
@@ -63,64 +62,56 @@ class MultiScorer extends Scorer {
         this.starts = starts;
     }
 
    /**
     * {@inheritDoc}
     */
    public boolean next() throws IOException {
        while (hasNext) {
            if (scorers[current].next()) {
                return true;
            } else if (++current < scorers.length) {
    @Override
    public int nextDoc() throws IOException {
        while (currentDoc != NO_MORE_DOCS) {
            if (scorers[currentScorer].nextDoc() != NO_MORE_DOCS) {
                currentDoc = scorers[currentScorer].docID() + starts[currentScorer];
                return currentDoc;
            } else if (++currentScorer < scorers.length) {
                 // advance to next scorer
             } else {
                 // no more scorers
                hasNext = false;
                currentDoc = NO_MORE_DOCS;
             }
         }
        return hasNext;

        return currentDoc;
     }
 
    /**
     * {@inheritDoc}
     */
    public int doc() {
        return scorers[current].doc() + starts[current];
    @Override
    public int docID() {
        return currentDoc;
     }
 
    /**
     * {@inheritDoc}
     */
    @Override
     public float score() throws IOException {
        return scorers[current].score();
        return scorers[currentScorer].score();
     }
 
    /**
     * {@inheritDoc}
     */
    public boolean skipTo(int target) throws IOException {
        current = scorerIndex(target);
        if (scorers[current].skipTo(target - starts[current])) {
            return true;
    @Override
    public int advance(int target) throws IOException {
        if (currentDoc == NO_MORE_DOCS) {
            return currentDoc;
        }

        currentScorer = scorerIndex(target);
        if (scorers[currentScorer].advance(target - starts[currentScorer]) != NO_MORE_DOCS) {
            currentDoc = scorers[currentScorer].docID() + starts[currentScorer];
            return currentDoc;
         } else {
            if (++current < scorers.length) {
            if (++currentScorer < scorers.length) {
                 // simply move to the next if there is any
                return next();
                currentDoc = nextDoc();
                return currentDoc;
             } else {
                 // no more document
                hasNext = false;
                return hasNext;
                currentDoc = NO_MORE_DOCS;
                return currentDoc;
             }
         }
     }
 
    /**
     * {@inheritDoc}
     */
    public Explanation explain(int doc) throws IOException {
        int scorerIndex = scorerIndex(doc);
        return scorers[scorerIndex].explain(doc - starts[scorerIndex]);
    }

     //--------------------------< internal >------------------------------------
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
index 3c2f8e625..ff6b3b423 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
@@ -30,6 +30,7 @@ import java.util.Set;
 /**
  * <code>NameQuery</code> implements a query for the name of a node.
  */
@SuppressWarnings("serial")
 public class NameQuery extends Query {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
index 1fb5dede2..5232ae926 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
@@ -30,6 +30,7 @@ import java.io.IOException;
 /**
  * <code>NameRangeQuery</code>...
  */
@SuppressWarnings("serial")
 public class NameRangeQuery extends Query {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeIndexer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeIndexer.java
index d15a29a29..809dada7c 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeIndexer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeIndexer.java
@@ -16,19 +16,6 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
@@ -50,6 +37,18 @@ import org.apache.tika.parser.Parser;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

 /**
  * Creates a lucene <code>Document</code> object from a {@link javax.jcr.Node}.
  */
@@ -369,7 +368,7 @@ public class NodeIndexer {
                     // never fulltext index jcr:uuid String
                     if (name.equals(NameConstants.JCR_UUID)) {
                         addStringValue(doc, fieldName, value.getString(),
                                false, false, DEFAULT_BOOST);
                                false, false, DEFAULT_BOOST, true);
                     } else {
                         addStringValue(doc, fieldName, value.getString(),
                                 true, isIncludedInNodeIndex(name),
@@ -666,7 +665,7 @@ public class NodeIndexer {
      *             addStringValue(Document, String, Object, boolean)} instead.
      */
     protected void addStringValue(Document doc, String fieldName, Object internalValue) {
        addStringValue(doc, fieldName, internalValue, true, true, DEFAULT_BOOST);
        addStringValue(doc, fieldName, internalValue, true, true, DEFAULT_BOOST, true);
     }
 
     /**
@@ -682,7 +681,7 @@ public class NodeIndexer {
      */
     protected void addStringValue(Document doc, String fieldName,
                                   Object internalValue, boolean tokenized) {
        addStringValue(doc, fieldName, internalValue, tokenized, true, DEFAULT_BOOST);
        addStringValue(doc, fieldName, internalValue, tokenized, true, DEFAULT_BOOST, true);
     }
 
     /**
@@ -814,14 +813,10 @@ public class NodeIndexer {
             tv = Field.TermVector.NO;
         }
         if (store) {
            // store field compressed if greater than 16k
            Field.Store stored;
            if (value.length() > 0x4000) {
                stored = Field.Store.COMPRESS;
            } else {
                stored = Field.Store.YES;
            }
            return new Field(FieldNames.FULLTEXT, value, stored,
            // We would be able to store the field compressed or not depending
            // on a criterion but then we could not determine later is this field
            // has been compressed or not, so we choose to store it uncompressed
            return new Field(FieldNames.FULLTEXT, value, Field.Store.YES,
                     Field.Index.ANALYZED, tv);
         } else {
             return new Field(FieldNames.FULLTEXT, value,
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
index 3d8d0a087..b80bcf432 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
@@ -37,7 +37,7 @@ public class NodeTraversingQueryHits extends AbstractQueryHits {
     /**
      * The nodes to traverse.
      */
    private final Iterator nodes;
    private final Iterator<Node> nodes;
 
     /**
      * Creates query hits that consist of the nodes that are traversed from a
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NotQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NotQuery.java
index 5940a5a01..9570727f7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NotQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NotQuery.java
@@ -33,6 +33,7 @@ import java.util.Set;
  * Documents that did not match the context query will be selected by this
  * <code>NotQuery</code>.
  */
@SuppressWarnings("serial")
 class NotQuery extends Query {
 
     /**
@@ -56,7 +57,7 @@ class NotQuery extends Query {
     /**
      * {@inheritDoc}
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new NotQueryWeight(searcher);
     }
 
@@ -89,7 +90,7 @@ class NotQuery extends Query {
     /**
      * Implements a weight for this <code>NotQuery</code>.
      */
    private class NotQueryWeight implements Weight {
    private class NotQueryWeight extends Weight {
 
         /**
          * The searcher to access the index.
@@ -134,8 +135,9 @@ class NotQuery extends Query {
         /**
          * @inheritDoc
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = context.weight(searcher).scorer(reader);
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = context.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             return new NotQueryScorer(reader);
         }
 
@@ -177,14 +179,17 @@ class NotQuery extends Query {
             this.reader = reader;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (docNo == NO_MORE_DOCS) {
                return docNo;
            }

             if (docNo == -1) {
                 // get first doc of context scorer
                if (contextScorer.next()) {
                    contextNo = contextScorer.doc();
                int docId = contextScorer.nextDoc();
                if (docId != NO_MORE_DOCS) {
                    contextNo = docId;
                 }
             }
             // move to next candidate
@@ -195,49 +200,38 @@ class NotQuery extends Query {
             // check with contextScorer
             while (contextNo != -1 && contextNo == docNo) {
                 docNo++;
                if (contextScorer.next()) {
                    contextNo = contextScorer.doc();
                } else {
                    contextNo = -1;
                }
                int docId = contextScorer.nextDoc();
                contextNo = docId == NO_MORE_DOCS ? -1 : docId;
            }
            if (docNo >= reader.maxDoc()) {
                docNo = NO_MORE_DOCS;
             }
            return docNo < reader.maxDoc();
            return docNo;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return docNo;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (docNo == NO_MORE_DOCS) {
                return docNo;
            }

             if (contextNo != -1 && contextNo < target) {
                if (contextScorer.skipTo(target)) {
                    contextNo = contextScorer.doc();
                } else {
                    contextNo = -1;
                }
                int docId = contextScorer.advance(target);
                contextNo = docId == NO_MORE_DOCS ? -1 : docId;
             }
             docNo = target - 1;
            return next();
            return nextDoc();
         }
 
        /**
         * @throws UnsupportedOperationException always
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/OffsetCharSequence.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/OffsetCharSequence.java
index dd5563b85..ddebe7f47 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/OffsetCharSequence.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/OffsetCharSequence.java
@@ -20,7 +20,7 @@ package org.apache.jackrabbit.core.query.lucene;
  * CharSequence that applies an offset to a base CharSequence. The base
  * CharSequence can be replaced without creating a new CharSequence.
  */
final class OffsetCharSequence implements CharSequence, Comparable, TransformConstants {
final class OffsetCharSequence implements CharSequence, Comparable<OffsetCharSequence>, TransformConstants {
 
     /**
      * Indicates how the underlying char sequence is exposed / tranformed.
@@ -128,8 +128,7 @@ final class OffsetCharSequence implements CharSequence, Comparable, TransformCon
      * @return as defined in {@link String#compareTo(Object)} but also takes
      *         {@link #transform} into account.
      */
    public int compareTo(Object o) {
        OffsetCharSequence other = (OffsetCharSequence) o;
    public int compareTo(OffsetCharSequence other) {
         int len1 = length();
         int len2 = other.length();
         int lim = Math.min(len1, len2);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Ordering.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Ordering.java
index db2cb5607..a660adafa 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Ordering.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/Ordering.java
@@ -16,22 +16,21 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.commons.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.LengthImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LowerCaseImpl;
 import org.apache.jackrabbit.spi.commons.query.qom.DynamicOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.UpperCaseImpl;
 import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchScoreImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LengthImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LowerCaseImpl;
 import org.apache.jackrabbit.spi.commons.query.qom.NodeLocalNameImpl;
 import org.apache.jackrabbit.spi.commons.query.qom.NodeNameImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.UpperCaseImpl;
 import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortComparatorSource;

import javax.jcr.RepositoryException;
 
 /**
  * <code>Ordering</code> implements a single ordering specification.
@@ -84,7 +83,7 @@ public class Ordering {
      *                             QOM ordering.
      */
     public static Ordering fromQOM(final OrderingImpl ordering,
                                    final SortComparatorSource scs,
                                    final SharedFieldComparatorSource scs,
                                     final NamespaceMappings nsMappings)
             throws RepositoryException {
         final Name[] selectorName = new Name[1];
@@ -103,7 +102,7 @@ public class Ordering {
                 SortField sf = (SortField) ((DynamicOperandImpl) node.getOperand()).accept(this, data);
                 selectorName[0] = node.getSelectorQName();
                 return new SortField(sf.getField(),
                        new LowerCaseSortComparator(sf.getFactory()),
                        new LowerCaseSortComparator(sf.getComparatorSource()),
                         !ordering.isAscending());
             }
 
@@ -112,7 +111,7 @@ public class Ordering {
                 SortField sf = (SortField) ((DynamicOperandImpl) node.getOperand()).accept(this, data);
                 selectorName[0] = node.getSelectorQName();
                 return new SortField(sf.getField(),
                        new UpperCaseSortComparator(sf.getFactory()),
                        new UpperCaseSortComparator(sf.getComparatorSource()),
                         !ordering.isAscending());
             }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ParentAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ParentAxisQuery.java
index 9315cf575..4eb93789a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ParentAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ParentAxisQuery.java
@@ -16,27 +16,28 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.jackrabbit.core.query.lucene.hits.Hits;
 import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Weight;
 
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

 /**
  * <code>ParentAxisQuery</code> selects the parent nodes of a context query.
  */
@SuppressWarnings("serial")
 class ParentAxisQuery extends Query {
 
     /**
@@ -89,7 +90,7 @@ class ParentAxisQuery extends Query {
      * @param searcher the <code>Searcher</code> instance to use.
      * @return a <code>ParentAxisWeight</code>.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new ParentAxisWeight(searcher);
     }
 
@@ -133,7 +134,7 @@ class ParentAxisQuery extends Query {
     /**
      * The <code>Weight</code> implementation for this <code>ParentAxisQuery</code>.
      */
    private class ParentAxisWeight implements Weight {
    private class ParentAxisWeight extends Weight {
 
         /**
          * The searcher in use
@@ -186,8 +187,9 @@ class ParentAxisQuery extends Query {
          * @return a <code>ParentAxisScorer</code>.
          * @throws IOException if an error occurs while reading from the index.
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             HierarchyResolver resolver = (HierarchyResolver) reader;
             return new ParentAxisScorer(searcher.getSimilarity(),
                     reader, searcher, resolver);
@@ -266,25 +268,26 @@ class ParentAxisQuery extends Query {
             this.hResolver = resolver;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateParent();
             nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             Float score = scores.get(nextDoc);
             if (score == null) {
@@ -293,23 +296,18 @@ class ParentAxisQuery extends Query {
             return score;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateParent();
             nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
         private void calculateParent() throws IOException {
@@ -317,36 +315,38 @@ class ParentAxisQuery extends Query {
                 hits = new BitSet(reader.maxDoc());
 
                 final IOException[] ex = new IOException[1];
                contextScorer.score(new HitCollector() {

                    private int[] docs = new int[1];

                    public void collect(int doc, float score) {
                        try {
                            docs = hResolver.getParents(doc, docs);
                            if (docs.length == 1) {
                                // optimize single value
                                hits.set(docs[0]);
                                if (firstScore == null) {
                                    firstScore = score;
                                } else if (firstScore != score) {
                                    scores.put(doc, score);
                                }
                            } else {
                                for (int docNum : docs) {
                                    hits.set(docNum);
                if (contextScorer != null) {
                    contextScorer.score(new AbstractHitCollector() {
                        private int[] docs = new int[1];

                        @Override
                        protected void collect(int doc, float score) {
                            try {
                                docs = hResolver.getParents(doc, docs);
                                if (docs.length == 1) {
                                    // optimize single value
                                    hits.set(docs[0]);
                                     if (firstScore == null) {
                                         firstScore = score;
                                     } else if (firstScore != score) {
                                         scores.put(doc, score);
                                     }
                                } else {
                                    for (int docNum : docs) {
                                        hits.set(docNum);
                                        if (firstScore == null) {
                                            firstScore = score;
                                        } else if (firstScore != score) {
                                            scores.put(doc, score);
                                        }
                                    }
                                 }
                            } catch (IOException e) {
                                ex[0] = e;
                             }
                        } catch (IOException e) {
                            ex[0] = e;
                         }
                    }
                });
                    });
                }
 
                 if (ex[0] != null) {
                     throw ex[0];
@@ -355,7 +355,7 @@ class ParentAxisQuery extends Query {
                 // filter out documents that do not match the name test
                 if (nameTest != null) {
                     Query nameQuery = new NameQuery(nameTest, version, nsMappings);
                    Hits nameHits = new ScorerHits(nameQuery.weight(searcher).scorer(reader));
                    Hits nameHits = new ScorerHits(nameQuery.weight(searcher).scorer(reader, true, false));
                     for (int i = hits.nextSetBit(0); i >= 0; i = hits.nextSetBit(i + 1)) {
                         int doc = nameHits.skipTo(i);
                         if (doc == -1) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
index 28adc2a31..6f5696cf7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PersistentIndex.java
@@ -130,7 +130,7 @@ class PersistentIndex extends AbstractIndex {
         byte[] buffer = new byte[1024];
         Directory dir = index.getDirectory();
         Directory dest = getDirectory();
        String[] files = dir.list();
        String[] files = dir.listAll();
         for (String file : files) {
             IndexInput in = dir.openInput(file);
             try {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PredicateDerefQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PredicateDerefQuery.java
index b2d874548..605983ea3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PredicateDerefQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/PredicateDerefQuery.java
@@ -16,28 +16,27 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Weight;
 
import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

 /**
  * Implements a Lucene <code>Query</code> which returns the nodes which have a 
  * reference property which matches the nodes of the subquery.
  */
@SuppressWarnings("serial")
 public class PredicateDerefQuery extends Query {
 
     /**
@@ -101,7 +100,7 @@ public class PredicateDerefQuery extends Query {
      * @param searcher the <code>Searcher</code> instance to use.
      * @return a <code>DerefWeight</code>.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new DerefWeight(searcher);
     }
 
@@ -148,7 +147,7 @@ public class PredicateDerefQuery extends Query {
     /**
      * The <code>Weight</code> implementation for this <code>DerefQuery</code>.
      */
    private class DerefWeight implements Weight {
    private class DerefWeight extends Weight {
 
         /**
          * The searcher in use
@@ -201,10 +200,11 @@ public class PredicateDerefQuery extends Query {
          * @return a <code>DerefScorer</code>.
          * @throws IOException if an error occurs while reading from the index.
          */
        public Scorer scorer(IndexReader reader) throws IOException {
            subQueryScorer = subQuery.weight(searcher).scorer(reader);
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            subQueryScorer = subQuery.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             if (nameTest != null) {
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader);
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
             }
             return new DerefScorer(searcher.getSimilarity(), reader);
         }
@@ -233,18 +233,13 @@ public class PredicateDerefQuery extends Query {
          * BitSet storing the id's of selected documents
          */
         private final BitSet subQueryHits;
        

         /**
          * BitSet storing the id's of selected documents
          */
         private final BitSet hits;
 
        /**
         * List of UUIDs of selected nodes
         */
        private List uuids = null;
 
        
         /**
          * The next document id to return
          */
@@ -263,49 +258,44 @@ public class PredicateDerefQuery extends Query {
             this.subQueryHits = new BitSet(reader.maxDoc());
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() throws IOException {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateChildren();
             nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 

         /**
          * Perform the sub query
          * For each reference property UUID
@@ -315,59 +305,58 @@ public class PredicateDerefQuery extends Query {
          * @throws IOException
          */
         private void calculateChildren() throws IOException {
            if (uuids == null) {
                uuids = new ArrayList();
 //                subQueryHits.clear();
 //                hits.clear();
                subQueryScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        subQueryHits.set(doc);
                    }
                });
            subQueryScorer.score(new AbstractHitCollector() {
                @Override
                protected void collect(int doc, float score) {
                    subQueryHits.set(doc);
                }
            });
 
                TermDocs termDocs = reader.termDocs(new Term(FieldNames.PROPERTIES_SET, refProperty));
                String prefix = FieldNames.createNamedValue(refProperty, "");
                while (termDocs.next()) {
                    int doc = termDocs.doc();
                     
                    String[] values = reader.document(doc).getValues(FieldNames.PROPERTIES);
                    if (values == null) {
                        // no reference properties at all on this node
                        continue;
                    }
                    for (int v = 0; v < values.length; v++) {
                        if (values[v].startsWith(prefix)) {
                            String uuid = values[v].substring(prefix.length());
                            
                            TermDocs node = reader.termDocs(TermFactory.createUUIDTerm(uuid));
                            try {
                                while (node.next()) {
                                    if (subQueryHits.get(node.doc())) {
                                        hits.set(doc);
                                    }
            TermDocs termDocs = reader.termDocs(new Term(FieldNames.PROPERTIES_SET, refProperty));
            String prefix = FieldNames.createNamedValue(refProperty, "");
            while (termDocs.next()) {
                int doc = termDocs.doc();

                String[] values = reader.document(doc).getValues(FieldNames.PROPERTIES);
                if (values == null) {
                    // no reference properties at all on this node
                    continue;
                }
                for (int v = 0; v < values.length; v++) {
                    if (values[v].startsWith(prefix)) {
                        String uuid = values[v].substring(prefix.length());

                        TermDocs node = reader.termDocs(TermFactory.createUUIDTerm(uuid));
                        try {
                            while (node.next()) {
                                if (subQueryHits.get(node.doc())) {
                                    hits.set(doc);
                                 }
                            } finally {
                                node.close();
                             }
                        } finally {
                            node.close();
                         }
                     }
                 }
                
                // collect nameTest hits
                final BitSet nameTestHits = new BitSet();
                if (nameTestScorer != null) {
                    nameTestScorer.score(new HitCollector() {
                        public void collect(int doc, float score) {
                            nameTestHits.set(doc);
                        }
                    });
                }
            }
 
                // filter out the target nodes that do not match the name test
                // if there is any name test at all.
                if (nameTestScorer != null) {
                    hits.and(nameTestHits);
                }
            // collect nameTest hits
            final BitSet nameTestHits = new BitSet();
            if (nameTestScorer != null) {
                nameTestScorer.score(new AbstractHitCollector() {
                    @Override
                    protected void collect(int doc, float score) {
                        nameTestHits.set(doc);
                    }
                });
            }

            // filter out the target nodes that do not match the name test
            // if there is any name test at all.
            if (nameTestScorer != null) {
                hits.and(nameTestHits);
             }
         }
     }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryHitsQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryHitsQuery.java
index b1f1fa332..ae2502426 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryHitsQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryHitsQuery.java
@@ -39,6 +39,7 @@ import java.util.TreeSet;
  * <code>QueryHitsQuery</code> exposes a {@link QueryHits} implementation again
  * as a Lucene Query.
  */
@SuppressWarnings("serial")
 public class QueryHitsQuery extends Query implements JackrabbitQuery{
 
     /**
@@ -58,7 +59,7 @@ public class QueryHitsQuery extends Query implements JackrabbitQuery{
     /**
      * {@inheritDoc}
      */
    protected Weight createWeight(Searcher searcher) throws IOException {
    public Weight createWeight(Searcher searcher) throws IOException {
         return new QueryHitsQueryWeight(searcher.getSimilarity());
     }
 
@@ -96,7 +97,7 @@ public class QueryHitsQuery extends Query implements JackrabbitQuery{
     /**
      * The Weight implementation for this query.
      */
    public class QueryHitsQueryWeight implements Weight {
    public class QueryHitsQueryWeight extends Weight {
 
         /**
          * The similarity.
@@ -142,7 +143,8 @@ public class QueryHitsQuery extends Query implements JackrabbitQuery{
         /**
          * {@inheritDoc}
          */
        public Scorer scorer(IndexReader reader) throws IOException {
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
             return new QueryHitsQueryScorer(reader, similarity);
         }
 
@@ -165,7 +167,7 @@ public class QueryHitsQuery extends Query implements JackrabbitQuery{
          * Iterator over <code>Integer</code> instances identifying the
          * lucene documents. Document numbers are iterated in ascending order.
          */
        private final Iterator docs;
        private final Iterator<Integer> docs;
 
         /**
          * Maps <code>Integer</code> document numbers to <code>Float</code>
@@ -212,48 +214,39 @@ public class QueryHitsQuery extends Query implements JackrabbitQuery{
             docs = sortedDocs.iterator();
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            if (docs.hasNext()) {
                currentDoc = (Integer) docs.next();
                return true;
        @Override
        public int nextDoc() throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
             }
            return false;
        }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
            currentDoc = docs.hasNext() ? docs.next() : NO_MORE_DOCS;
             return currentDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
        public int docID() {
            return currentDoc == null ? -1 : currentDoc;
        }

        @Override
         public float score() throws IOException {
             return scores.get(currentDoc);
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
            }

             do {
                if (!next()) {
                    return false;
                if (nextDoc() == NO_MORE_DOCS) {
                    return NO_MORE_DOCS;
                 }
            } while (target > doc());
            return true;
            } while (target > docID());
            return docID();
         }
 
        /**
         * {@inheritDoc}
         */
        public Explanation explain(int doc) throws IOException {
            return new Explanation();
        }
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RangeQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RangeQuery.java
index b5b71c681..2c8fb335b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RangeQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RangeQuery.java
@@ -16,14 +16,6 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
@@ -34,16 +26,26 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.Weight;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /**
  * Implements a variant of the lucene class {@link org.apache.lucene.search.RangeQuery}.
  * This class does not rewrite to basic {@link org.apache.lucene.search.TermQuery}
  * but will calculate the matching documents itself. That way a
  * <code>TooManyClauses</code> can be avoided.
  */
@SuppressWarnings("serial")
 public class RangeQuery extends Query implements Transformable {
 
     /**
@@ -147,7 +149,7 @@ public class RangeQuery extends Query implements Transformable {
     public Query rewrite(IndexReader reader) throws IOException {
         if (transform == TRANSFORM_NONE) {
             Query stdRangeQueryImpl
                    = new org.apache.lucene.search.RangeQuery(lowerTerm, upperTerm, inclusive);
                    = new TermRangeQuery(lowerTerm.field(), lowerTerm.text(), upperTerm.text(), inclusive, inclusive);
             try {
                 stdRangeQuery = stdRangeQueryImpl.rewrite(reader);
                 return stdRangeQuery;
@@ -169,7 +171,7 @@ public class RangeQuery extends Query implements Transformable {
      * @param searcher the searcher to use for the <code>Weight</code>.
      * @return the <code>Weigth</code> for this query.
      */
    protected Weight createWeight(Searcher searcher) {
    public Weight createWeight(Searcher searcher) {
         return new RangeQueryWeight(searcher, cache);
     }
 
@@ -238,7 +240,8 @@ public class RangeQuery extends Query implements Transformable {
          * @param reader index reader
          * @return a {@link RangeQueryScorer} instance
          */
        protected Scorer createScorer(IndexReader reader) {
        protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) {
             return new RangeQueryScorer(searcher.getSimilarity(), reader, cache);
         }
 
@@ -355,44 +358,42 @@ public class RangeQuery extends Query implements Transformable {
             hits = result;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateHits();
             nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateHits();
             nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * Returns an empty Explanation object.
         * @return an empty Explanation object.
         */
        public Explanation explain(int doc) {
            return new Explanation();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
         /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
index 87344a7df..a865594b3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ReadOnlyIndexReader.java
@@ -16,13 +16,14 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.BitSet;

 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.index.TermPositions;
 
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

 /**
  * Overwrites the methods that would modify the index and throws an
  * {@link UnsupportedOperationException} in each of those methods. A
@@ -175,7 +176,8 @@ class ReadOnlyIndexReader extends RefCountingIndexReader {
     /**
      * @exception UnsupportedOperationException always
      */
    protected final void doCommit() {
    @Override
    protected void doCommit(Map commitUserData) throws IOException { 
         throw new UnsupportedOperationException("IndexReader is read-only");
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RefCountingIndexReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RefCountingIndexReader.java
index 9fd3d06a8..c68c48fbf 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RefCountingIndexReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/RefCountingIndexReader.java
@@ -48,7 +48,7 @@ public class RefCountingIndexReader
     /**
      * @return the current reference count value.
      */
    synchronized int getRefCount() {
    public synchronized int getRefCount() {
         return refCount;
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ScoreNode.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ScoreNode.java
index f81c57365..c0919b9f6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ScoreNode.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ScoreNode.java
@@ -20,7 +20,6 @@ import java.io.IOException;
 
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
 
 /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
index ad610b6be..0c06d9b6b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
@@ -16,27 +16,6 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

 import org.apache.jackrabbit.core.HierarchyManager;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.fs.FileSystem;
@@ -50,6 +29,7 @@ import org.apache.jackrabbit.core.query.QueryHandler;
 import org.apache.jackrabbit.core.query.QueryHandlerContext;
 import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
 import org.apache.jackrabbit.core.query.lucene.directory.FSDirectoryManager;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
 import org.apache.jackrabbit.core.session.SessionContext;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.ItemStateManager;
@@ -63,20 +43,21 @@ import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
 import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeFactory;
 import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Payload;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.HitCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortComparatorSource;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermQuery;
 import org.apache.tika.parser.Parser;
@@ -85,6 +66,26 @@ import org.slf4j.LoggerFactory;
 import org.w3c.dom.Element;
 import org.xml.sax.SAXException;
 
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /**
  * Implements a {@link org.apache.jackrabbit.core.query.QueryHandler} using
  * Lucene.
@@ -440,9 +441,9 @@ public class SearchIndex extends AbstractQueryHandler {
     private int termInfosIndexDivisor = DEFAULT_TERM_INFOS_INDEX_DIVISOR;
 
     /**
     * The sort comparator source for indexed properties.
     * The field comparator source for indexed properties.
      */
    private SortComparatorSource scs;
    private SharedFieldComparatorSource scs;
 
     /**
      * Flag that indicates whether the hierarchy cache should be initialized
@@ -507,7 +508,7 @@ public class SearchIndex extends AbstractQueryHandler {
             }
         }
 
        scs = new SharedFieldSortComparator(
        scs = new SharedFieldComparatorSource(
                 FieldNames.PROPERTIES, context.getItemStateManager(),
                 context.getHierarchyManager(), nsMappings);
         indexingConfig = createIndexingConfiguration(nsMappings);
@@ -691,7 +692,8 @@ public class SearchIndex extends AbstractQueryHandler {
             try {
                 Query q = new TermQuery(new Term(
                         FieldNames.WEAK_REFS, id.toString()));
                searcher.search(q, new HitCollector() {
                searcher.search(q, new AbstractHitCollector() {
                    @Override
                     public void collect(int doc, float score) {
                         docs.add(doc);
                     }
@@ -1096,9 +1098,9 @@ public class SearchIndex extends AbstractQueryHandler {
     }
 
     /**
     * @return the sort comparator source for this index.
     * @return the field comparator source for this index.
      */
    protected SortComparatorSource getSortComparatorSource() {
    protected SharedFieldComparatorSource getSortComparatorSource() {
         return scs;
     }
 
@@ -1376,11 +1378,17 @@ public class SearchIndex extends AbstractQueryHandler {
                             try {
                                 // find the right fields to transfer
                                 Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                                Token t = new Token();
                                 for (Fieldable field : fields) {

                                     // assume properties fields use SingleTokenStream
                                    t = field.tokenStreamValue().next(t);
                                    String value = new String(t.termBuffer(), 0, t.termLength());
                                    TokenStream tokenStream = field.tokenStreamValue();
                                    TermAttribute termAttribute = tokenStream.addAttribute(TermAttribute.class);
                                    PayloadAttribute payloadAttribute = tokenStream.addAttribute(PayloadAttribute.class);
                                    tokenStream.incrementToken();
                                    tokenStream.end();
                                    tokenStream.close();

                                    String value = new String(termAttribute.termBuffer(), 0, termAttribute.termLength());
                                     if (value.startsWith(namePrefix)) {
                                         // extract value
                                         value = value.substring(namePrefix.length());
@@ -1388,9 +1396,11 @@ public class SearchIndex extends AbstractQueryHandler {
                                         Path p = getRelativePath(state, propState);
                                         String path = getNamespaceMappings().translatePath(p);
                                         value = FieldNames.createNamedValue(path, value);
                                        t.setTermBuffer(value);
                                        doc.add(new Field(field.name(), new SingletonTokenStream(t)));
                                        doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID, parent.getNodeId().toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                                        termAttribute.setTermBuffer(value);
                                        doc.add(new Field(field.name(),
                                                new SingletonTokenStream(value, (Payload) payloadAttribute.getPayload().clone())));
                                        doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                                                parent.getNodeId().toString(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                                     }
                                 }
                             } finally {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
index 6f5820749..accb3a00b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
@@ -18,18 +18,17 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.index.TermEnum;
 import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.FieldComparator;
 
import javax.jcr.PropertyType;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.WeakHashMap;
 
import javax.jcr.PropertyType;

 /**
  * Implements a variant of the lucene class <code>org.apache.lucene.search.FieldCacheImpl</code>.
  * The lucene FieldCache class has some sort of support for custom comparators
@@ -139,7 +138,7 @@ public class SharedFieldCache {
      * @param reader     the <code>IndexReader</code>.
      * @param field      name of the shared field.
      * @param prefix     the property name, will be used as term prefix.
     * @param comparator the sort comparator instance.
     * @param comparator the field comparator instance.
      * @return a ValueIndex that contains the field values and order
      *         information.
      * @throws IOException if an error occurs while reading from the index.
@@ -147,7 +146,7 @@ public class SharedFieldCache {
     public ValueIndex getValueIndex(IndexReader reader,
                                     String field,
                                     String prefix,
                                    SortComparatorSource comparator)
                                    FieldComparator comparator)
             throws IOException {
 
         if (reader instanceof ReadOnlyIndexReader) {
@@ -225,7 +224,7 @@ public class SharedFieldCache {
      * See if a <code>ValueIndex</code> object is in the cache.
      */
     ValueIndex lookup(IndexReader reader, String field,
                      String prefix, SortComparatorSource comparer) {
                      String prefix, FieldComparator comparer) {
         Key key = new Key(field, prefix, comparer);
         synchronized (this) {
             Map<Key, ValueIndex> readerCache = cache.get(reader);
@@ -240,7 +239,7 @@ public class SharedFieldCache {
      * Put a <code>ValueIndex</code> <code>value</code> to cache.
      */
     ValueIndex store(IndexReader reader, String field, String prefix,
                 SortComparatorSource comparer, ValueIndex value) {
                 FieldComparator comparer, ValueIndex value) {
         Key key = new Key(field, prefix, comparer);
         synchronized (this) {
             Map<Key, ValueIndex> readerCache = cache.get(reader);
@@ -285,12 +284,12 @@ public class SharedFieldCache {
 
         private final String field;
         private final String prefix;
        private final SortComparatorSource comparator;
        private final Object comparator;
 
         /**
          * Creates <code>Key</code> for ValueIndex lookup.
          */
        Key(String field, String prefix, SortComparatorSource comparator) {
        Key(String field, String prefix, FieldComparator comparator) { 
             this.field = field.intern();
             this.prefix = prefix.intern();
             this.comparator = comparator;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldComparatorSource.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldComparatorSource.java
new file mode 100644
index 000000000..e3ea3cc70
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldComparatorSource.java
@@ -0,0 +1,379 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a <code>FieldComparatorSource</code> for <code>FieldComparator</code>s which
 * know how to sort on a lucene field that contains values for multiple properties.
 */
public class SharedFieldComparatorSource extends FieldComparatorSource {

    /**
     * The name of the shared field in the lucene index.
     */
    private final String field;

    /**
     * The item state manager.
     */
    private final ItemStateManager ism;

    /**
     * The hierarchy manager on top of {@link #ism}.
     */
    private final HierarchyManager hmgr;

    /**
     * The index internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    /**
     * Create a new <code>SharedFieldComparatorSource</code> for a given shared field.
     *
     * @param fieldname the shared field.
     * @param ism       the item state manager of this workspace.
     * @param hmgr      the hierarchy manager of this workspace.
     * @param nsMappings the index internal namespace mappings.
     */
    public SharedFieldComparatorSource(String fieldname, ItemStateManager ism,
                                       HierarchyManager hmgr, NamespaceMappings nsMappings) {
        this.field = fieldname;
        this.ism = ism;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
    }

    /**
     * Create a new <code>FieldComparator</code> for an embedded <code>propertyName</code>
     * and a <code>reader</code>.
     *
     * @param propertyName the relative path to the property to sort on as returned
     *          by {@link org.apache.jackrabbit.spi.Path#getString()}.
     * @return a <code>FieldComparator</code>
     * @throws java.io.IOException if an error occurs
     */
    @Override
    public FieldComparator newComparator(String propertyName, int numHits, int sortPos,
                                         boolean reversed) throws IOException {

        PathFactory factory = PathFactoryImpl.getInstance();
        Path path = factory.create(propertyName);

        try {
            SimpleFieldComparator simple = new SimpleFieldComparator(nsMappings.translatePath(path), field, numHits);

            return path.getLength() == 1
                ? simple
                : new CompoundScoreFieldComparator(
                        new FieldComparator[] { simple, new RelPathFieldComparator(path, numHits) }, numHits);

        }
        catch (IllegalNameException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * Abstract base class for <code>FieldComparator</code>s which keep their values
     * (<code>Comparable</code>s) in an array.
     */
    private abstract static class AbstractFieldComparator extends FieldComparatorBase {

        /**
         * The values for comparing.
         */
        private final Comparable[] values;

        /**
         * The index readers.
         */

        protected final List<IndexReader> readers = new ArrayList<IndexReader>();
        /**
         * The document number starts for the {@link #readers}.
         */
        protected int[] starts;

        /**
         * Create a new instance with the given number of values.
         *
         * @param numHits  the number of values
         */
        protected AbstractFieldComparator(int numHits) {
            values = new Comparable[numHits];
        }

        /**
         * Returns the reader index for document <code>n</code>.
         *
         * @param n document number.
         * @return the reader index.
         */
        protected final int readerIndex(int n) {
            int lo = 0;
            int hi = readers.size() - 1;

            while (hi >= lo) {
                int mid = (lo + hi) >> 1;
                int midValue = starts[mid];
                if (n < midValue) {
                    hi = mid - 1;
                }
                else if (n > midValue) {
                    lo = mid + 1;
                }
                else {
                    while (mid + 1 < readers.size() && starts[mid + 1] == midValue) {
                        mid++;
                    }
                    return mid;
                }
            }
            return hi;
        }

        /**
         * Add the given value to the values array
         *
         * @param slot   index into values
         * @param value  value for adding
         */
        @Override
        public void setValue(int slot, Comparable value) {
            values[slot] = value;
        }

        /**
         * Return a value from the values array
         *
         * @param slot  index to retrieve
         * @return  the retrieved value
         */
        @Override
        public Comparable getValue(int slot) {
            return values[slot];
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            getIndexReaders(readers, reader);

            int maxDoc = 0;
            starts = new int[readers.size() + 1];

            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                starts[i] = maxDoc;
                maxDoc += r.maxDoc();
            }
            starts[readers.size()] = maxDoc;
        }

        /**
         * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
         * so calls itself recursively for each reader within the
         * <code>MultiIndexReader</code> or otherwise adds the reader to the list.
         *
         * @param readers  list of index readers.
         * @param reader   reader to decompose
         */
        private static void getIndexReaders(List<IndexReader> readers, IndexReader reader) {
            if (reader instanceof MultiIndexReader) {
                for (IndexReader r : ((MultiIndexReader) reader).getIndexReaders()) {
                    getIndexReaders(readers, r);
                }
            }
            else {
                readers.add(reader);
            }
        }
    }

    /**
     * A <code>FieldComparator</code> which works for order by clauses with properties
     * directly on the result nodes.
     */
    static final class SimpleFieldComparator extends AbstractFieldComparator {

        /**
         * The term look ups of the index segments.
         */
        protected SharedFieldCache.ValueIndex[] indexes;

        /**
         * The name of the property
         */
        private final String propertyName;

        /**
         * The name of the field in the index
         */
        private final String fieldName;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param propertyName  the name of the property
         * @param fieldName     the name of the field in the index
         * @param numHits       the number of values 
         */
        public SimpleFieldComparator(String propertyName, String fieldName, int numHits) {
            super(numHits);
            this.propertyName = propertyName;
            this.fieldName = fieldName;
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            super.setNextReader(reader, docBase);

            indexes = new SharedFieldCache.ValueIndex[readers.size()];

            String namedValue = FieldNames.createNamedValue(propertyName, "");
            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, fieldName, namedValue, this);
            }
        }

        @Override
        protected Comparable sortValue(int doc) {
            int idx = readerIndex(doc);
            return indexes[idx].getValue(doc - starts[idx]);
        }

    }

    /**
     * A <code>FieldComparator</code> which works with order by clauses that use a
     * relative path to a property to sort on.
     */
    private final class RelPathFieldComparator extends AbstractFieldComparator {

        /**
         * Relative path to the property
         */
        private final Path propertyName;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param propertyName  relative path of the property
         * @param numHits       the number of values
         */
        public RelPathFieldComparator(Path propertyName, int numHits) {
            super(numHits);
            this.propertyName = propertyName;
        }

        @Override
        protected Comparable sortValue(int doc) {
            try {
                int idx = readerIndex(doc);
                IndexReader reader = readers.get(idx);
                Document document = reader.document(doc - starts[idx], FieldSelectors.UUID);
                String uuid = document.get(FieldNames.UUID);
                Path path = hmgr.getPath(new NodeId(uuid));
                PathBuilder builder = new PathBuilder(path);
                builder.addAll(propertyName.getElements());
                PropertyId id = hmgr.resolvePropertyPath(builder.getPath());

                if (id == null) {
                    return null;
                }

                PropertyState state = (PropertyState) ism.getItemState(id);
                if (state == null) {
                    return null;
                }

                InternalValue[] values = state.getValues();
                if (values.length > 0) {
                    return Util.getComparable(values[0]);
                }
            }
            catch (Exception ignore) { }

            return null;
        }

    }

    /**
     * Implements a compound <code>FieldComparator</code> which delegates to several
     * other comparators. The comparators are asked for a sort value in the
     * sequence they are passed to the constructor.
     */
    private static final class CompoundScoreFieldComparator extends AbstractFieldComparator {
        private final FieldComparator[] fieldComparators;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param fieldComparators  delegatees
         * @param numHits           the number of values
         */
        public CompoundScoreFieldComparator(FieldComparator[] fieldComparators, int numHits) {
            super(numHits);
            this.fieldComparators = fieldComparators;
        }

        @Override
        public Comparable sortValue(int doc) {
            for (FieldComparator fieldComparator : fieldComparators) {
                if (fieldComparator instanceof FieldComparatorBase) {
                    Comparable c = ((FieldComparatorBase) fieldComparator).sortValue(doc);

                    if (c != null) {
                        return c;
                    }
                }
            }
            return null;
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            for (FieldComparator fieldComparator : fieldComparators) {
                fieldComparator.setNextReader(reader, docBase);
            }
        }
    }
    
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
deleted file mode 100644
index 1b4cfcd27..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
++ /dev/null
@@ -1,242 +0,0 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;
import org.apache.lucene.document.Document;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;

/**
 * Implements a <code>SortComparator</code> which knows how to sort on a lucene
 * field that contains values for multiple properties.
 */
public class SharedFieldSortComparator extends SortComparator {

    private static final long serialVersionUID = 2609351820466200052L;

    /**
     * The name of the shared field in the lucene index.
     */
    private final String field;

    /**
     * The item state manager.
     */
    private final ItemStateManager ism;

    /**
     * The hierarchy manager on top of {@link #ism}.
     */
    private final HierarchyManager hmgr;

    /**
     * The index internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    /**
     * Creates a new <code>SharedFieldSortComparator</code> for a given shared
     * field.
     *
     * @param fieldname the shared field.
     * @param ism       the item state manager of this workspace.
     * @param hmgr      the hierarchy manager of this workspace.
     * @param nsMappings the index internal namespace mappings.
     */
    public SharedFieldSortComparator(String fieldname,
                                     ItemStateManager ism,
                                     HierarchyManager hmgr,
                                     NamespaceMappings nsMappings) {
        this.field = fieldname;
        this.ism = ism;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
    }

    /**
     * Creates a new <code>ScoreDocComparator</code> for an embedded
     * <code>propertyName</code> and a <code>reader</code>.
     *
     * @param reader the index reader.
     * @param relPath the relative path to the property to sort on as returned
     *          by {@link Path#getString()}.
     * @return a <code>ScoreDocComparator</code> for the
     * @throws IOException if an error occurs while reading from the index.
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String relPath)
            throws IOException {
        PathFactory factory = PathFactoryImpl.getInstance();
        Path p = factory.create(relPath);
        try {
            ScoreDocComparator simple = new SimpleScoreDocComparator(
                    reader, nsMappings.translatePath(p));
            if (p.getLength() == 1) {
                return simple;
            } else {
                return new CompoundScoreDocComparator(reader,
                        new ScoreDocComparator[]{
                                simple,
                                new RelPathScoreDocComparator(reader, p)
                        });
            }
        } catch (IllegalNameException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    protected Comparable getComparable(String termtext) {
        throw new UnsupportedOperationException();
    }

    /**
     * A score doc comparator that works for order by clauses with properties
     * directly on the result nodes.
     */
    private final class SimpleScoreDocComparator extends AbstractScoreDocComparator {

        /**
         * The term look ups of the index segments.
         */
        protected final SharedFieldCache.ValueIndex[] indexes;

        public SimpleScoreDocComparator(IndexReader reader,
                                        String propertyName)
                throws IOException {
            super(reader);
            this.indexes = new SharedFieldCache.ValueIndex[readers.size()];

            String namedValue = FieldNames.createNamedValue(propertyName, "");
            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, field,
                        namedValue, SharedFieldSortComparator.this);
            }
        }

        /**
         * Returns the index term for the score doc <code>i</code>.
         *
         * @param i the score doc.
         * @return the sort value if available.
         */
        public Comparable sortValue(ScoreDoc i) {
            int idx = readerIndex(i.doc);
            return indexes[idx].getValue(i.doc - starts[idx]);
        }
    }

    /**
     * A score doc comparator that works with order by clauses that use a
     * relative path to a property to sort on.
     */
    private final class RelPathScoreDocComparator extends AbstractScoreDocComparator {

        private final Path relPath;

        public RelPathScoreDocComparator(IndexReader reader,
                                         Path relPath)
                throws IOException {
            super(reader);
            this.relPath = relPath;
        }

        /**
         * Returns the sort value for the given {@link ScoreDoc}. The value is
         * retrieved from the item state manager.
         *
         * @param i the score doc.
         * @return the sort value for the score doc.
         */
        public Comparable sortValue(ScoreDoc i) {
            try {
                int idx = readerIndex(i.doc);
                IndexReader reader = readers.get(idx);
                Document doc = reader.document(i.doc - starts[idx], FieldSelectors.UUID);
                String uuid = doc.get(FieldNames.UUID);
                Path path = hmgr.getPath(new NodeId(uuid));
                PathBuilder builder = new PathBuilder(path);
                builder.addAll(relPath.getElements());
                PropertyId id = hmgr.resolvePropertyPath(builder.getPath());
                if (id == null) {
                    return null;
                }
                PropertyState state = (PropertyState) ism.getItemState(id);
                if (state == null) {
                    return null;
                }
                InternalValue[] values = state.getValues();
                if (values.length > 0) {
                    return Util.getComparable(values[0]);
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Implements a compound score doc comparator that delegates to several
     * other comparators. The comparators are asked for a sort value in the
     * sequence they are passed to the constructor. The first non-null value
     * will be returned by {@link #sortValue(ScoreDoc)}.
     */
    private final class CompoundScoreDocComparator
            extends AbstractScoreDocComparator {

        private final ScoreDocComparator[] comparators;

        public CompoundScoreDocComparator(IndexReader reader,
                                          ScoreDocComparator[] comparators)
                throws IOException {
            super(reader);
            this.comparators = comparators;
        }

        /**
         * {@inheritDoc}
         */
        public Comparable sortValue(ScoreDoc i) {
            for (ScoreDocComparator comparator : comparators) {
                Comparable c = comparator.sortValue(i);
                if (c != null) {
                    return c;
                }
            }
            return null;
        }
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimilarityQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimilarityQuery.java
index e5b5aed4c..d6b6374e1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimilarityQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimilarityQuery.java
@@ -16,19 +16,19 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermDocs;
import org.apache.lucene.analysis.Analyzer;

import java.io.IOException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
 
 /**
  * <code>SimilarityQuery</code> implements a query that returns similar nodes
  * for a given node UUID.
  */
@SuppressWarnings("serial")
 public class SimilarityQuery extends Query {
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimpleExcerptProvider.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimpleExcerptProvider.java
index 998105afa..06ff2e292 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimpleExcerptProvider.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SimpleExcerptProvider.java
@@ -60,9 +60,9 @@ public class SimpleExcerptProvider implements ExcerptProvider {
         try {
             NodeState nodeState = (NodeState) ism.getItemState(id);
             String separator = "";
            Iterator it = nodeState.getPropertyNames().iterator();
            Iterator<Name> it = nodeState.getPropertyNames().iterator();
             while (it.hasNext() && text.length() < maxFragmentSize) {
                PropertyId propId = new PropertyId(id, (Name) it.next());
                PropertyId propId = new PropertyId(id, it.next());
                 PropertyState propState = (PropertyState) ism.getItemState(propId);
                 if (propState.getType() == PropertyType.STRING) {
                     text.append(separator);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
index aa087c787..86d836323 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
@@ -16,12 +16,13 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;

import org.apache.lucene.analysis.Token;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.index.Payload;
 
import java.io.IOException;

 /**
  * <code>SingletonTokenStream</code> implements a token stream that wraps a
  * single value with a given property type. The property type is stored as a
@@ -40,40 +41,50 @@ public final class SingletonTokenStream extends TokenStream {
     private final Payload payload;
 
     /**
     * Creates a new SingleTokenStream with the given value and a property
     * <code>type</code>.
     * The term attribute of the current token
     */
    private TermAttribute termAttribute;

    /**
     * The payload attribute of the current token
     */
    private PayloadAttribute payloadAttribute;

    /**
     * Creates a new SingleTokenStream with the given value and payload.
      *
      * @param value the string value that will be returned with the token.
     * @param type the JCR property type.
     * @param payload the payload that will be attached to this token
      */
    public SingletonTokenStream(String value, int type) {
    public SingletonTokenStream(String value, Payload payload) {
         this.value = value;
        this.payload = new Payload(new PropertyMetaData(type).toByteArray());
        this.payload = payload;
        termAttribute = (TermAttribute) addAttribute(TermAttribute.class);
        payloadAttribute = (PayloadAttribute) addAttribute(PayloadAttribute.class);
     }
 
     /**
     * Creates a new SingleTokenStream with the given token.
     * Creates a new SingleTokenStream with the given value and a property
     * <code>type</code>.
      *
     * @param t the token.
     * @param value the string value that will be returned with the token.
     * @param type the JCR property type.
      */
    public SingletonTokenStream(Token t) {
        this.value = t.term();
        this.payload = t.getPayload();
    public SingletonTokenStream(String value, int type) {
        this(value, new Payload(new PropertyMetaData(type).toByteArray()));
     }
 
    /**
     * {@inheritDoc}
     */
    public Token next(Token reusableToken) throws IOException {
    @Override
    public boolean incrementToken() throws IOException {
         if (value == null) {
            return null;
            return false;
         }
        reusableToken.clear();
        reusableToken.setTermBuffer(value);
        reusableToken.setPayload(payload);
        reusableToken.setStartOffset(0);
        reusableToken.setEndOffset(value.length());

        clearAttributes();
        termAttribute.setTermBuffer(value);
        payloadAttribute.setPayload(payload);

         value = null;
        return reusableToken;
        return true;
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedLuceneQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedLuceneQueryHits.java
index eb1248336..2ac05887c 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedLuceneQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedLuceneQueryHits.java
@@ -16,19 +16,20 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocCollector;
import org.apache.lucene.search.TopFieldCollector;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

 /**
  * Wraps a lucene query result and adds a close method that allows to release
  * resources after a query has been executed and the results have been read
@@ -155,15 +156,14 @@ public final class SortedLuceneQueryHits extends AbstractQueryHits {
     //-------------------------------< internal >-------------------------------
 
     private void getHits() throws IOException {
        TopFieldDocCollector collector = new TopFieldDocCollector(reader, sort, numHits);
        TopFieldCollector collector = TopFieldCollector.create(sort, numHits, false, true, false, false);
         searcher.search(query, collector);
        this.size = collector.getTotalHits();
        size = collector.getTotalHits();
         ScoreDoc[] docs = collector.topDocs().scoreDocs;
        for (int i = scoreDocs.size(); i < docs.length; i++) {
            scoreDocs.add(docs[i]);
        }
        scoreDocs.addAll(Arrays.asList(docs).subList(scoreDocs.size(), docs.length));
         log.debug("getHits() {}/{}", scoreDocs.size(), numHits);
         // double hits for next round
         numHits *= 2;
     }

 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedMultiColumnQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedMultiColumnQueryHits.java
index 1db931773..e166b5a9d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedMultiColumnQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SortedMultiColumnQueryHits.java
@@ -16,19 +16,19 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;

 import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Arrays;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.jackrabbit.spi.Name;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
 
 /**
  * <code>SortedMultiColumnQueryHits</code> implements sorting of query hits
@@ -61,7 +61,7 @@ public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {
         }
         try {
             Collections.sort(sortedHits, new ScoreNodeComparator(
                    reader, orderings, hits.getSelectorNames()));
                    reader, orderings, hits.getSelectorNames(), sortedHits.size()));
         } catch (RuntimeException e) {
             // might be thrown by ScoreNodeComparator#compare
             throw Util.createIOException(e);
@@ -142,7 +142,8 @@ public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {
          */
         private ScoreNodeComparator(IndexReader reader,
                                     Ordering[] orderings,
                                    Name[] selectorNames)
                                    Name[] selectorNames,
                                    int numHits)
                 throws IOException {
             this.reader = reader;
             this.orderings = orderings;
@@ -153,8 +154,11 @@ public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {
             for (int i = 0; i < orderings.length; i++) {
                 idx[i] = names.indexOf(orderings[i].getSelectorName());
                 SortField sf = orderings[i].getSortField();
                if (sf.getFactory() != null) {
                    comparators[i] = sf.getFactory().newComparator(reader, sf.getField());
                if (sf.getComparatorSource() != null) {
                    FieldComparator c = sf.getComparatorSource().newComparator(sf.getField(), numHits, 0, false);
                    assert c instanceof FieldComparatorBase;
                    comparators[i] = new ScoreDocComparator((FieldComparatorBase) c);
                    comparators[i].setNextReader(reader, 0);
                 }
                 isReverse[i] = sf.getReverse();
             }
@@ -184,7 +188,7 @@ public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {
                     } catch (IOException e) {
                         throw new RuntimeException(e.getMessage(), e);
                     }
                    c = comparators[i].compare(doc1, doc2);
                    c = comparators[i].compareDocs(doc1.doc, doc2.doc);
                 } else {
                     // compare score
                     c = new Float(n1.getScore()).compareTo(n2.getScore());
@@ -198,5 +202,19 @@ public class SortedMultiColumnQueryHits extends FilterMultiColumnQueryHits {
             }
             return 0;
         }

     }

    private static final class ScoreDocComparator extends FieldComparatorDecorator {

        public ScoreDocComparator(FieldComparatorBase base) {
            super(base);
        }

        public int compareDocs(int doc1, int doc2) {
            return compare(sortValue(doc1), sortValue(doc2));
        }

    }

 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/TermDocsCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/TermDocsCache.java
index 7f223a166..153e8e95c 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/TermDocsCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/TermDocsCache.java
@@ -94,7 +94,7 @@ public class TermDocsCache {
      * @throws IOException if an error occurs while reading from the index.
      */
     public TermDocs termDocs(final Term t) throws IOException {
        if (t.field() != field) {
        if (t == null || t.field() != field) {
             return reader.termDocs(t);
         }
 
@@ -270,14 +270,13 @@ public class TermDocsCache {
         }
     }
 
    private static final class CacheEntry implements Comparable {
    private static final class CacheEntry implements Comparable<CacheEntry> {
 
         private volatile int numAccessed = 1;
 
         private volatile BitSet bits;
 
        public int compareTo(Object o) {
            CacheEntry other = (CacheEntry) o;
        public int compareTo(CacheEntry other) {
             return (numAccessed < other.numAccessed ? -1 : (numAccessed == other.numAccessed ? 0 : 1));
         }
     }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/UpperCaseSortComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/UpperCaseSortComparator.java
index c7a09a883..1317a4f5b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/UpperCaseSortComparator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/UpperCaseSortComparator.java
@@ -16,71 +16,43 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
 
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
 
 /**
 * <code>UpperCaseSortComparator</code> implements a sort comparator that
 * <code>UpperCaseSortComparator</code> implements a <code>FieldComparator</code> which
  * compares the upper-cased string values of a base sort comparator.
  */
public class UpperCaseSortComparator implements SortComparatorSource {

    private static final long serialVersionUID = 2562371983498948119L;
public class UpperCaseSortComparator extends FieldComparatorSource {
     
     /**
      * The base sort comparator.
      */
    private final SortComparatorSource base;
    private final FieldComparatorSource base;
 
     /**
      * Creates a new upper case sort comparator.
      *
      * @param base the base sort comparator source.
      */
    public UpperCaseSortComparator(SortComparatorSource base) {
    public UpperCaseSortComparator(FieldComparatorSource base) {
         this.base = base;
     }
 
    /**
     * {@inheritDoc}
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String fieldname)
            throws IOException {
        return new Comparator(base.newComparator(reader, fieldname));
    }

    private static final class Comparator implements ScoreDocComparator {

        private ScoreDocComparator base;

        private Comparator(ScoreDocComparator base) {
            this.base = base;
        }
    @Override
    public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
        FieldComparator comparator = base.newComparator(fieldname, numHits, sortPos, reversed);
        assert comparator instanceof FieldComparatorBase;
 
        /**
         * @see Util#compare(Comparable, Comparable)
         */
        public int compare(ScoreDoc i, ScoreDoc j) {
            return Util.compare(sortValue(i), sortValue(j));
        }

        public Comparable sortValue(ScoreDoc i) {
            Comparable c = base.sortValue(i);
            if (c != null) {
                return c.toString().toUpperCase();
            } else {
                return null;
        return new FieldComparatorDecorator((FieldComparatorBase) comparator) {
            @Override
            protected Comparable sortValue(int doc) {
                Comparable c = super.sortValue(doc);
                return c == null ? null : c.toString().toUpperCase();
             }
        }

        public int sortType() {
            return SortField.CUSTOM;
        }
        };
     }
}
\ No newline at end of file

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/VolatileIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/VolatileIndex.java
index 53900d62f..9974c5130 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/VolatileIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/VolatileIndex.java
@@ -39,7 +39,8 @@ class VolatileIndex extends AbstractIndex {
     /**
      * Map of pending documents to add to the index
      */
    private final Map pending = new LinkedMap();
    @SuppressWarnings("unchecked")
    private final Map<String, Document> pending = new LinkedMap();
 
     /**
      * Number of documents that are buffered before they are added to the index.
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WeightedHighlighter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WeightedHighlighter.java
index f7d2f406c..c70f85bca 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WeightedHighlighter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WeightedHighlighter.java
@@ -140,7 +140,7 @@ public class WeightedHighlighter extends DefaultHighlighter {
                         break;
                     }
                 }
                bestFragments.insert(fi);
                bestFragments.insertWithOverflow(fi);
             }
         }
 
@@ -189,7 +189,7 @@ public class WeightedHighlighter extends DefaultHighlighter {
             int limit = Math.max(0, fi.getStartOffset() / 2 + fi.getEndOffset() / 2 - surround);
             int len = startFragment(sb, text, fi.getStartOffset(), limit);
             TermVectorOffsetInfo lastOffsetInfo = null;
            Iterator fIt = fi.iterator();
            Iterator<TermVectorOffsetInfo> fIt = fi.iterator();
             while (fIt.hasNext()) {
                 TermVectorOffsetInfo oi = (TermVectorOffsetInfo) fIt.next();
                 if (lastOffsetInfo != null) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardNameQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardNameQuery.java
index 8bc8767f6..6b5a73a74 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardNameQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardNameQuery.java
@@ -16,18 +16,9 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;

 import javax.jcr.NamespaceException;
 
import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardQuery.java
index f2145e824..c9a226040 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/WildcardQuery.java
@@ -17,7 +17,6 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermEnum;
 import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.search.FilteredTermEnum;
@@ -29,6 +28,7 @@ import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.ToStringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -48,6 +48,7 @@ import java.util.Set;
  * <li><code>_</code> : matches exactly one character</li>
  * </ul>
  */
@SuppressWarnings("serial")
 public class WildcardQuery extends Query implements Transformable {
 
     /**
@@ -140,11 +141,22 @@ public class WildcardQuery extends Query implements Transformable {
      * @return the rewritten query.
      * @throws IOException if an error occurs while reading from the index.
      */
    @Override
     public Query rewrite(IndexReader reader) throws IOException {
        Query stdWildcardQuery = new MultiTermQuery(new Term(field, pattern)) {
        Query stdWildcardQuery = new MultiTermQuery() {
             protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
                 return new WildcardTermEnum(reader, field, tvf, pattern, transform);
             }

            /** Prints a user-readable version of this query. */
            @Override
            public String toString(String field) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(field);
                buffer.append(':');
                buffer.append(ToStringUtils.boost(getBoost()));
                return buffer.toString();
            }
         };
         try {
             multiTermQuery = stdWildcardQuery.rewrite(reader);
@@ -162,7 +174,8 @@ public class WildcardQuery extends Query implements Transformable {
      * @param searcher the searcher to use for the <code>Weight</code>.
      * @return the <code>Weigth</code> for this query.
      */
    protected Weight createWeight(Searcher searcher) {
    @Override
    public Weight createWeight(Searcher searcher) {
         return new WildcardQueryWeight(searcher, cache);
     }
 
@@ -172,13 +185,12 @@ public class WildcardQuery extends Query implements Transformable {
      * @param field the field name for which to create a string representation.
      * @return a string representation of this query.
      */
    @Override
     public String toString(String field) {
         return field + ":" + tvf.createValue(pattern);
     }
 
    /**
     * {@inheritDoc}
     */
    @Override
     public void extractTerms(Set terms) {
         if (multiTermQuery != null) {
             multiTermQuery.extractTerms(terms);
@@ -209,7 +221,8 @@ public class WildcardQuery extends Query implements Transformable {
          * @param reader index reader
          * @return a {@link WildcardQueryScorer} instance
          */
        protected Scorer createScorer(IndexReader reader) {
        protected Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) {
             return new WildcardQueryScorer(searcher.getSimilarity(), reader, cache);
         }
 
@@ -218,33 +231,26 @@ public class WildcardQuery extends Query implements Transformable {
          *
          * @return this <code>WildcardQuery</code>.
          */
        @Override
         public Query getQuery() {
             return WildcardQuery.this;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float getValue() {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float sumOfSquaredWeights() throws IOException {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public void normalize(float norm) {
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public Explanation explain(IndexReader reader, int doc) throws IOException {
             return new Explanation();
         }
@@ -315,44 +321,42 @@ public class WildcardQuery extends Query implements Transformable {
             hits = result;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateHits();
             nextDoc = hits.nextSetBit(nextDoc + 1);
            return nextDoc > -1;
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        public int doc() {
        @Override
        public int docID() {
             return nextDoc;
         }
 
        /**
         * {@inheritDoc}
         */
        @Override
         public float score() {
             return 1.0f;
         }
 
        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

             calculateHits();
             nextDoc = hits.nextSetBit(target);
            return nextDoc > -1;
        }

        /**
         * Returns an empty Explanation object.
         * @return an empty Explanation object.
         */
        public Explanation explain(int doc) {
            return new Explanation();
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
         }
 
         /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/directory/FSDirectoryManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/directory/FSDirectoryManager.java
index cebca00bf..15c7ebac5 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/directory/FSDirectoryManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/directory/FSDirectoryManager.java
@@ -16,10 +16,6 @@
  */
 package org.apache.jackrabbit.core.query.lucene.directory;
 
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

 import org.apache.jackrabbit.core.query.lucene.IOCounters;
 import org.apache.jackrabbit.core.query.lucene.SearchIndex;
 import org.apache.lucene.store.Directory;
@@ -30,6 +26,10 @@ import org.apache.lucene.store.Lock;
 import org.apache.lucene.store.LockFactory;
 import org.apache.lucene.store.NativeFSLockFactory;
 
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

 /**
  * <code>FSDirectoryManager</code> implements a directory manager for
  * {@link FSDirectory} instances.
@@ -141,11 +141,17 @@ public class FSDirectoryManager implements DirectoryManager {
         private final FSDirectory directory;
 
         public FSDir(File dir) throws IOException {
            directory = FSDirectory.getDirectory(dir,
            if (!dir.mkdirs()) {
                if (!dir.isDirectory()) {
                    throw new IOException("Unable to create directory: '" + dir + "'");
                }
            }
            directory = FSDirectory.open(dir,
                     new NativeFSLockFactory(dir));
         }
 
        public String[] list() throws IOException {
        @Override
        public String[] listAll() throws IOException {
             File[] files = directory.getFile().listFiles(FILTER);
             if (files == null) {
                 return null;
@@ -157,71 +163,81 @@ public class FSDirectoryManager implements DirectoryManager {
             return names;
         }
 
        @Override
         public boolean fileExists(String name) throws IOException {
             return directory.fileExists(name);
         }
 
        @Override
         public long fileModified(String name) throws IOException {
             return directory.fileModified(name);
         }
 
        @Override
         public void touchFile(String name) throws IOException {
             directory.touchFile(name);
         }
 
        @Override
         public void deleteFile(String name) throws IOException {
             directory.deleteFile(name);
         }
 
        public void renameFile(String from, String to) throws IOException {
            directory.renameFile(from, to);
        }

        @Override
         public long fileLength(String name) throws IOException {
             return directory.fileLength(name);
         }
 
        @Override
         public IndexOutput createOutput(String name) throws IOException {
             return directory.createOutput(name);
         }
 
        @Override
         public IndexInput openInput(String name) throws IOException {
             IndexInput in = directory.openInput(name);
             return new IndexInputLogWrapper(in);
         }
 
        @Override
         public void close() throws IOException {
             directory.close();
         }
 
        @Override
         public IndexInput openInput(String name, int bufferSize)
                 throws IOException {
             IndexInput in = directory.openInput(name, bufferSize);
             return new IndexInputLogWrapper(in);
         }
 
        @Override
         public Lock makeLock(String name) {
             return directory.makeLock(name);
         }
 
        @Override
         public void clearLock(String name) throws IOException {
             directory.clearLock(name);
         }
 
        @Override
         public void setLockFactory(LockFactory lockFactory) {
             directory.setLockFactory(lockFactory);
         }
 
        @Override
         public LockFactory getLockFactory() {
             return directory.getLockFactory();
         }
 
        @Override
         public String getLockID() {
             return directory.getLockID();
         }
 
         public String toString() {
            return this.getClass().getName() + "@" + directory;
            return getClass().getName() + '@' + directory;
         }
     }
 
@@ -237,27 +253,33 @@ public class FSDirectoryManager implements DirectoryManager {
             this.in = in;
         }
 
        @Override
         public byte readByte() throws IOException {
             return in.readByte();
         }
 
        @Override
         public void readBytes(byte[] b, int offset, int len) throws IOException {
             IOCounters.incrRead();
             in.readBytes(b, offset, len);
         }
 
        @Override
         public void close() throws IOException {
             in.close();
         }
 
        @Override
         public long getFilePointer() {
             return in.getFilePointer();
         }
 
        @Override
         public void seek(long pos) throws IOException {
             in.seek(pos);
         }
 
        @Override
         public long length() {
             return in.length();
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/AbstractHitCollector.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/AbstractHitCollector.java
new file mode 100644
index 000000000..8e2ab5421
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/AbstractHitCollector.java
@@ -0,0 +1,60 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene.hits;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Collector implementation which simply provides the collection
 * of re-based doc base with scorer.
 */
public abstract class AbstractHitCollector extends Collector {
    protected int base = 0;
    protected Scorer scorer = null;

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        base = docBase;
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer;
    }

    @Override
    public void collect(int doc) throws IOException {
        collect(base + doc, scorer.score());
    }

    /**
     * Called once for every document matching a query, with the re-based document
     * number and its computed score.
     * @param doc the re-based document number.
     * @param doc the document's score.
     */
    protected abstract void collect(int doc, float score);

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/ScorerHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/ScorerHits.java
index 9e4f0ee1e..11e7c0719 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/ScorerHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/hits/ScorerHits.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.core.query.lucene.hits;
 
 import java.io.IOException;
 
import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Scorer;
 
 /**
@@ -42,8 +43,9 @@ public class ScorerHits implements Hits {
      * {@inheritDoc}
      */
     public int next() throws IOException {
        if (scorer.next()) {
            return scorer.doc();
        int docId = scorer.nextDoc();
        if (docId != DocIdSetIterator.NO_MORE_DOCS) {
            return docId;
         } else {
             return -1;
         }
@@ -53,8 +55,9 @@ public class ScorerHits implements Hits {
      * {@inheritDoc}
      */
     public int skipTo(int target) throws IOException {
        if (scorer.skipTo(target)) {
            return scorer.doc();
        int docId = scorer.advance(target);
        if (docId != DocIdSetIterator.NO_MORE_DOCS) {
            return docId;
         } else {
             return -1;
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/SameNodeJoinMerger.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/SameNodeJoinMerger.java
index 7edd5670c..a486b4bcd 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/SameNodeJoinMerger.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/SameNodeJoinMerger.java
@@ -33,8 +33,6 @@ import javax.jcr.query.qom.PropertyValue;
 import javax.jcr.query.qom.QueryObjectModelFactory;
 import javax.jcr.query.qom.SameNodeJoinCondition;
 
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

 class SameNodeJoinMerger extends JoinMerger {
 
     private final String selector1;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/ScoreNodeMap.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/ScoreNodeMap.java
index 55dfbd939..e9f8168ed 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/ScoreNodeMap.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/join/ScoreNodeMap.java
@@ -59,7 +59,9 @@ public final class ScoreNodeMap {
             existing = nodes;
             map.put(key, existing);
         } else if (existing instanceof List) {
            ((List) existing).add(nodes);
            @SuppressWarnings("unchecked")
            List<ScoreNode[]> existingNodes = (List<ScoreNode[]>) existing;
            existingNodes.add(nodes);
         } else {
             // ScoreNode[]
             List<ScoreNode[]> tmp = new ArrayList<ScoreNode[]>();
@@ -83,6 +85,7 @@ public final class ScoreNodeMap {
         if (sn == null) {
             return null;
         } else if (sn instanceof List) {
            @SuppressWarnings("unchecked")
             List<ScoreNode[]> list = (List<ScoreNode[]>) sn;
             return list.toArray(new ScoreNode[list.size()][]);
         } else {
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java
index 995ac20a1..e1e2317e9 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/ChainedTermEnumTest.java
@@ -16,12 +16,7 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
@@ -31,8 +26,13 @@ import org.apache.lucene.index.Term;
 import org.apache.lucene.index.TermEnum;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
 
import junit.framework.TestCase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
 
 /**
  * <code>ChainedTermEnumTest</code> implements a test for JCR-2410.
@@ -61,7 +61,7 @@ public class ChainedTermEnumTest extends TestCase {
     protected TermEnum createTermEnum(String prefix, int numTerms)
             throws IOException {
         Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(),
        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_24),
                 true, IndexWriter.MaxFieldLength.UNLIMITED);
         for (int i = 0; i < numTerms; i++) {
             Document doc = new Document();
@@ -70,7 +70,7 @@ public class ChainedTermEnumTest extends TestCase {
             writer.addDocument(doc);
         }
         writer.close();
        IndexReader reader = IndexReader.open(dir);
        IndexReader reader = IndexReader.open(dir, false);
         TermEnum terms = reader.terms();
         if (terms.term() == null) {
             // position at first term
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java
index 2e37c6cea..5172a9360 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/lucene/IndexMigrationTest.java
@@ -16,10 +16,7 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
 import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
 import org.apache.jackrabbit.core.query.lucene.directory.RAMDirectoryManager;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
@@ -27,8 +24,11 @@ import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
 
import junit.framework.TestCase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
 /**
  * <code>IndexMigrationTest</code> contains a test case for JCR-2393.
@@ -50,7 +50,7 @@ public class IndexMigrationTest extends TestCase {
         DirectoryManager dirMgr = new RAMDirectoryManager();
 
         PersistentIndex idx = new PersistentIndex("index",
                new StandardAnalyzer(), Similarity.getDefault(),
                new StandardAnalyzer(Version.LUCENE_24), Similarity.getDefault(),
                 new DocNumberCache(100),
                 new IndexingQueue(new IndexingQueueStore(new RAMDirectory())),
                 dirMgr, 0);
diff --git a/jackrabbit-parent/pom.xml b/jackrabbit-parent/pom.xml
index 70784a084..7bab0c5bd 100644
-- a/jackrabbit-parent/pom.xml
++ b/jackrabbit-parent/pom.xml
@@ -257,7 +257,7 @@
       <dependency>
         <groupId>org.apache.lucene</groupId>
         <artifactId>lucene-core</artifactId>
        <version>2.4.1</version>
        <version>3.0.3</version>
       </dependency>
       <dependency>
         <groupId>org.apache.tika</groupId>
- 
2.19.1.windows.1

