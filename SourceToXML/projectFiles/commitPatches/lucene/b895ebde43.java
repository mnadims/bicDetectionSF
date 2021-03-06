From b895ebde4340ed8ae903c7ae51750da3d9837394 Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Fri, 3 May 2013 13:37:45 +0000
Subject: [PATCH] LUCENE-4946: Refactor SorterTemplate (now Sorter).

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1478785 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   4 +
 lucene/NOTICE.txt                             |   5 -
 .../index/ConcurrentMergeScheduler.java       |   2 +-
 .../lucene/index/DocFieldProcessor.java       |   2 +-
 .../lucene/index/FreqProxTermsWriter.java     |   2 +-
 .../lucene/index/FrozenBufferedDeletes.java   |   2 +-
 .../apache/lucene/index/IndexFileDeleter.java |   2 +-
 .../lucene/search/ConjunctionScorer.java      |   2 +-
 .../search/MinShouldMatchSumScorer.java       |   2 +-
 .../lucene/search/MultiPhraseQuery.java       |   2 +-
 .../org/apache/lucene/search/PhraseQuery.java |   2 +-
 .../apache/lucene/search/TopTermsRewrite.java |   2 +-
 .../lucene/search/spans/NearSpansOrdered.java |   2 +-
 .../lucene/util/ArrayInPlaceMergeSorter.java  |  47 ++
 .../apache/lucene/util/ArrayIntroSorter.java  |  59 +++
 .../apache/lucene/util/ArrayTimSorter.java    |  76 +++
 .../org/apache/lucene/util/ArrayUtil.java     | 310 ++----------
 .../org/apache/lucene/util/BytesRefHash.java  |   4 +-
 .../apache/lucene/util/CollectionUtil.java    | 258 +++-------
 .../lucene/util/InPlaceMergeSorter.java       |  46 ++
 .../org/apache/lucene/util/IntroSorter.java   |  98 ++++
 .../java/org/apache/lucene/util/Sorter.java   | 248 ++++++++++
 .../apache/lucene/util/SorterTemplate.java    | 445 ------------------
 .../org/apache/lucene/util/TimSorter.java     | 373 +++++++++++++++
 .../util/automaton/BasicOperations.java       |   4 +-
 .../apache/lucene/util/automaton/State.java   |   2 +-
 .../apache/lucene/util/BaseSortTestCase.java  | 173 +++++++
 .../org/apache/lucene/util/TestArrayUtil.java |  84 +---
 .../lucene/util/TestCollectionUtil.java       |  95 +---
 .../lucene/util/TestInPlaceMergeSorter.java   |  36 ++
 .../apache/lucene/util/TestIntroSorter.java   |  32 ++
 .../lucene/util/TestSorterTemplate.java       | 181 -------
 .../org/apache/lucene/util/TestTimSorter.java |  31 ++
 .../facet/search/TestDrillSideways.java       |  27 +-
 .../lucene/search/highlight/TokenSources.java |   2 +-
 .../TokenStreamFromTermPositionVector.java    |   2 +-
 .../search/postingshighlight/Passage.java     |  17 +-
 .../PostingsHighlighter.java                  |  19 +-
 .../lucene/index/memory/MemoryIndex.java      |   2 +-
 .../lucene/index/CompoundFileExtractor.java   |   2 +-
 .../apache/lucene/index/sorter/Sorter.java    |  45 +-
 .../index/sorter/SortingAtomicReader.java     | 152 ++++--
 .../search/spell/DirectSpellChecker.java      |   2 +-
 .../lucene/search/suggest/BytesRefArray.java  |   8 +-
 .../handler/AnalysisRequestHandlerBase.java   |   2 +-
 45 files changed, 1514 insertions(+), 1399 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/ArrayInPlaceMergeSorter.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/ArrayIntroSorter.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/ArrayTimSorter.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/InPlaceMergeSorter.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/IntroSorter.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/Sorter.java
 delete mode 100644 lucene/core/src/java/org/apache/lucene/util/SorterTemplate.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/TimSorter.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/util/BaseSortTestCase.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/util/TestInPlaceMergeSorter.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/util/TestIntroSorter.java
 delete mode 100644 lucene/core/src/test/org/apache/lucene/util/TestSorterTemplate.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/util/TestTimSorter.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 00848553c14..28b1536c704 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -55,6 +55,10 @@ Changes in backwards compatibility policy
 * LUCENE-4973: SnapshotDeletionPolicy no longer requires a unique
   String id (Mike McCandless, Shai Erera)
 
* LUCENE-4946: The internal sorting API (SorterTemplate, now Sorter) has been
  completely refactored to allow for a better implementation of TimSort.
  (Adrien Grand, Uwe Schindler, Dawid Weiss)

 Bug Fixes
 
 * LUCENE-4935: CustomScoreQuery wrongly applied its query boost twice 
diff --git a/lucene/NOTICE.txt b/lucene/NOTICE.txt
index 34978650156..0cf70cb5ee2 100644
-- a/lucene/NOTICE.txt
++ b/lucene/NOTICE.txt
@@ -27,11 +27,6 @@ Jean-Philippe Barrette-LaPierre. This library is available under an MIT license,
 see http://sites.google.com/site/rrettesite/moman and 
 http://bitbucket.org/jpbarrette/moman/overview/
 
The class org.apache.lucene.util.SorterTemplate was inspired by CGLIB's class
with the same name. The implementation part is mainly done using pre-existing
Lucene sorting code. In-place stable mergesort was borrowed from CGLIB,
which is Apache-licensed.

 The class org.apache.lucene.util.WeakIdentityMap was derived from
 the Apache CXF project and is Apache License 2.0.
 
diff --git a/lucene/core/src/java/org/apache/lucene/index/ConcurrentMergeScheduler.java b/lucene/core/src/java/org/apache/lucene/index/ConcurrentMergeScheduler.java
index ddda077e432..42212ec23e5 100644
-- a/lucene/core/src/java/org/apache/lucene/index/ConcurrentMergeScheduler.java
++ b/lucene/core/src/java/org/apache/lucene/index/ConcurrentMergeScheduler.java
@@ -184,7 +184,7 @@ public class ConcurrentMergeScheduler extends MergeScheduler {
     }
 
     // Sort the merge threads in descending order.
    CollectionUtil.mergeSort(activeMerges, compareByMergeDocCount);
    CollectionUtil.timSort(activeMerges, compareByMergeDocCount);
     
     int pri = mergeThreadPriority;
     final int activeMergeCount = activeMerges.size();
diff --git a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
index 30a4fe461e4..5584dfdbd40 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
++ b/lucene/core/src/java/org/apache/lucene/index/DocFieldProcessor.java
@@ -213,7 +213,7 @@ final class DocFieldProcessor extends DocConsumer {
     // sort the subset of fields that have vectors
     // enabled; we could save [small amount of] CPU
     // here.
    ArrayUtil.quickSort(fields, 0, fieldCount, fieldsComp);
    ArrayUtil.introSort(fields, 0, fieldCount, fieldsComp);
     for(int i=0;i<fieldCount;i++) {
       final DocFieldProcessorPerField perField = fields[i];
       perField.consumer.processFields(perField.fields, perField.fieldCount);
diff --git a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
index 6937a09a521..476ac2aecd7 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
++ b/lucene/core/src/java/org/apache/lucene/index/FreqProxTermsWriter.java
@@ -54,7 +54,7 @@ final class FreqProxTermsWriter extends TermsHashConsumer {
     final int numAllFields = allFields.size();
 
     // Sort by field name
    CollectionUtil.quickSort(allFields);
    CollectionUtil.introSort(allFields);
 
     final FieldsConsumer consumer = state.segmentInfo.getCodec().postingsFormat().fieldsConsumer(state);
 
diff --git a/lucene/core/src/java/org/apache/lucene/index/FrozenBufferedDeletes.java b/lucene/core/src/java/org/apache/lucene/index/FrozenBufferedDeletes.java
index 916a53f98e7..7362957dd9d 100644
-- a/lucene/core/src/java/org/apache/lucene/index/FrozenBufferedDeletes.java
++ b/lucene/core/src/java/org/apache/lucene/index/FrozenBufferedDeletes.java
@@ -56,7 +56,7 @@ class FrozenBufferedDeletes {
     assert !isSegmentPrivate || deletes.terms.size() == 0 : "segment private package should only have del queries"; 
     Term termsArray[] = deletes.terms.keySet().toArray(new Term[deletes.terms.size()]);
     termCount = termsArray.length;
    ArrayUtil.mergeSort(termsArray);
    ArrayUtil.timSort(termsArray);
     PrefixCodedTerms.Builder builder = new PrefixCodedTerms.Builder();
     for (Term term : termsArray) {
       builder.add(term);
diff --git a/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java b/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
index 9e8dc76f0c6..c6ec5d03e23 100644
-- a/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
++ b/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
@@ -232,7 +232,7 @@ final class IndexFileDeleter implements Closeable {
     }
 
     // We keep commits list in sorted order (oldest to newest):
    CollectionUtil.mergeSort(commits);
    CollectionUtil.timSort(commits);
 
     // Now delete anything with ref count at 0.  These are
     // presumably abandoned files eg due to crash of
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java b/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
index d285c481c27..22476e777b6 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
@@ -44,7 +44,7 @@ class ConjunctionScorer extends Scorer {
     }
     // Sort the array the first time to allow the least frequent DocsEnum to
     // lead the matching.
    ArrayUtil.mergeSort(docsAndFreqs, new Comparator<DocsAndFreqs>() {
    ArrayUtil.timSort(docsAndFreqs, new Comparator<DocsAndFreqs>() {
       @Override
       public int compare(DocsAndFreqs o1, DocsAndFreqs o2) {
         return Long.compare(o1.cost, o2.cost);
diff --git a/lucene/core/src/java/org/apache/lucene/search/MinShouldMatchSumScorer.java b/lucene/core/src/java/org/apache/lucene/search/MinShouldMatchSumScorer.java
index 5b88e5fd9a9..061ebcc499b 100644
-- a/lucene/core/src/java/org/apache/lucene/search/MinShouldMatchSumScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/MinShouldMatchSumScorer.java
@@ -87,7 +87,7 @@ class MinShouldMatchSumScorer extends Scorer {
     this.sortedSubScorers = subScorers.toArray(new Scorer[this.numScorers]);
     // sorting by decreasing subscorer cost should be inversely correlated with
     // next docid (assuming costs are due to generating many postings)
    ArrayUtil.mergeSort(sortedSubScorers, new Comparator<Scorer>() {
    ArrayUtil.timSort(sortedSubScorers, new Comparator<Scorer>() {
       @Override
       public int compare(Scorer o1, Scorer o2) {
         return Long.signum(o2.cost() - o1.cost());
diff --git a/lucene/core/src/java/org/apache/lucene/search/MultiPhraseQuery.java b/lucene/core/src/java/org/apache/lucene/search/MultiPhraseQuery.java
index 0bcac051f1e..ce446a82049 100644
-- a/lucene/core/src/java/org/apache/lucene/search/MultiPhraseQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/MultiPhraseQuery.java
@@ -241,7 +241,7 @@ public class MultiPhraseQuery extends Query {
 
       // sort by increasing docFreq order
       if (slop == 0) {
        ArrayUtil.mergeSort(postingsFreqs);
        ArrayUtil.timSort(postingsFreqs);
       }
 
       if (slop == 0) {
diff --git a/lucene/core/src/java/org/apache/lucene/search/PhraseQuery.java b/lucene/core/src/java/org/apache/lucene/search/PhraseQuery.java
index 5ee4c8a56a5..0911af45b7c 100644
-- a/lucene/core/src/java/org/apache/lucene/search/PhraseQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/PhraseQuery.java
@@ -278,7 +278,7 @@ public class PhraseQuery extends Query {
 
       // sort by increasing docFreq order
       if (slop == 0) {
        ArrayUtil.mergeSort(postingsFreqs);
        ArrayUtil.timSort(postingsFreqs);
       }
 
       if (slop == 0) {  // optimize exact case
diff --git a/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java b/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
index 3e0cc556c57..b3c6ec4b09a 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
++ b/lucene/core/src/java/org/apache/lucene/search/TopTermsRewrite.java
@@ -156,7 +156,7 @@ public abstract class TopTermsRewrite<Q extends Query> extends TermCollectingRew
     
     final Q q = getTopLevelQuery();
     final ScoreTerm[] scoreTerms = stQueue.toArray(new ScoreTerm[stQueue.size()]);
    ArrayUtil.mergeSort(scoreTerms, scoreTermSortByTermComp);
    ArrayUtil.timSort(scoreTerms, scoreTermSortByTermComp);
     
     for (final ScoreTerm st : scoreTerms) {
       final Term term = new Term(query.field, st.bytes);
diff --git a/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java b/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
index 959253ab5bb..1ee44a566af 100644
-- a/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
++ b/lucene/core/src/java/org/apache/lucene/search/spans/NearSpansOrdered.java
@@ -204,7 +204,7 @@ public class NearSpansOrdered extends Spans {
 
   /** Advance the subSpans to the same document */
   private boolean toSameDoc() throws IOException {
    ArrayUtil.mergeSort(subSpansByDoc, spanDocComparator);
    ArrayUtil.timSort(subSpansByDoc, spanDocComparator);
     int firstIndex = 0;
     int maxDoc = subSpansByDoc[subSpansByDoc.length - 1].doc();
     while (subSpansByDoc[firstIndex].doc() != maxDoc) {
diff --git a/lucene/core/src/java/org/apache/lucene/util/ArrayInPlaceMergeSorter.java b/lucene/core/src/java/org/apache/lucene/util/ArrayInPlaceMergeSorter.java
new file mode 100644
index 00000000000..cacaceefbb6
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/ArrayInPlaceMergeSorter.java
@@ -0,0 +1,47 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Comparator;

/**
 * An {@link InPlaceMergeSorter} for object arrays.
 * @lucene.internal
 */
final class ArrayInPlaceMergeSorter<T> extends InPlaceMergeSorter {

  private final T[] arr;
  private final Comparator<? super T> comparator;

  /** Create a new {@link ArrayInPlaceMergeSorter}. */
  public ArrayInPlaceMergeSorter(T[] arr, Comparator<? super T> comparator) {
    this.arr = arr;
    this.comparator = comparator;
  }

  @Override
  protected int compare(int i, int j) {
    return comparator.compare(arr[i], arr[j]);
  }

  @Override
  protected void swap(int i, int j) {
    ArrayUtil.swap(arr, i, j);
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/ArrayIntroSorter.java b/lucene/core/src/java/org/apache/lucene/util/ArrayIntroSorter.java
new file mode 100644
index 00000000000..072abf54367
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/ArrayIntroSorter.java
@@ -0,0 +1,59 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Comparator;

/**
 * An {@link IntroSorter} for object arrays.
 * @lucene.internal
 */
final class ArrayIntroSorter<T> extends IntroSorter {

  private final T[] arr;
  private final Comparator<? super T> comparator;
  private T pivot;

  /** Create a new {@link ArrayInPlaceMergeSorter}. */
  public ArrayIntroSorter(T[] arr, Comparator<? super T> comparator) {
    this.arr = arr;
    this.comparator = comparator;
    pivot = null;
  }

  @Override
  protected int compare(int i, int j) {
    return comparator.compare(arr[i], arr[j]);
  }

  @Override
  protected void swap(int i, int j) {
    ArrayUtil.swap(arr, i, j);
  }

  @Override
  protected void setPivot(int i) {
    pivot = arr[i];
  }

  @Override
  protected int comparePivot(int i) {
    return comparator.compare(pivot, arr[i]);
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/ArrayTimSorter.java b/lucene/core/src/java/org/apache/lucene/util/ArrayTimSorter.java
new file mode 100644
index 00000000000..a9befe16b9e
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/ArrayTimSorter.java
@@ -0,0 +1,76 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Comparator;

/**
 * A {@link TimSorter} for object arrays.
 * @lucene.internal
 */
final class ArrayTimSorter<T> extends TimSorter {

  private final Comparator<? super T> comparator;
  private final T[] arr;
  private final T[] tmp;

  /** Create a new {@link ArrayTimSorter}. */
  public ArrayTimSorter(T[] arr, Comparator<? super T> comparator, int maxTempSlots) {
    super(maxTempSlots);
    this.arr = arr;
    this.comparator = comparator;
    if (maxTempSlots > 0) {
      @SuppressWarnings("unchecked")
      final T[] tmp = (T[]) new Object[maxTempSlots];
      this.tmp = tmp;
    } else {
      this.tmp = null;
    }
  }

  @Override
  protected int compare(int i, int j) {
    return comparator.compare(arr[i], arr[j]);
  }

  @Override
  protected void swap(int i, int j) {
    ArrayUtil.swap(arr, i, j);
  }

  @Override
  protected void copy(int src, int dest) {
    arr[dest] = arr[src];
  }

  @Override
  protected void save(int start, int len) {
    System.arraycopy(arr, start, tmp, 0, len);
  }

  @Override
  protected void restore(int src, int dest) {
    arr[dest] = tmp[src];
  }

  @Override
  protected int compareSaved(int i, int j) {
    return comparator.compare(tmp[i], arr[j]);
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/ArrayUtil.java b/lucene/core/src/java/org/apache/lucene/util/ArrayUtil.java
index 24da5101def..1d91c38a023 100644
-- a/lucene/core/src/java/org/apache/lucene/util/ArrayUtil.java
++ b/lucene/core/src/java/org/apache/lucene/util/ArrayUtil.java
@@ -28,11 +28,6 @@ import java.util.Comparator;
 
 public final class ArrayUtil {
 
  // affordable memory overhead to merge sorted arrays
  static final float MERGE_OVERHEAD_RATIO = 0.01f;
  // arrays below this size will always be sorted in-place
  static final int MERGE_EXTRA_MEMORY_THRESHOLD = (int) (15 / MERGE_OVERHEAD_RATIO);

   private ArrayUtil() {} // no instance
 
   /*
@@ -610,237 +605,85 @@ public final class ArrayUtil {
     return result;
   }
 
  private static abstract class ArraySorterTemplate<T> extends SorterTemplate {

    protected final T[] a;

    ArraySorterTemplate(T[] a) {
      this.a = a;
    }

    protected abstract int compare(T a, T b);

    @Override
    protected void swap(int i, int j) {
      final T o = a[i];
      a[i] = a[j];
      a[j] = o;
    }

    @Override
    protected int compare(int i, int j) {
      return compare(a[i], a[j]);
    }

  private static class NaturalComparator<T extends Comparable<? super T>> implements Comparator<T> {
    NaturalComparator() {}
     @Override
    protected void setPivot(int i) {
      pivot = a[i];
    }

    @Override
    protected int comparePivot(int j) {
      return compare(pivot, a[j]);
    }

    private T pivot;

  }

  // a template for merge-based sorts which uses extra memory to speed up merging
  private static abstract class ArrayMergeSorterTemplate<T> extends ArraySorterTemplate<T> {

    private final int threshold; // maximum length of a merge that can be made using extra memory
    private final T[] tmp;

    ArrayMergeSorterTemplate(T[] a, float overheadRatio) {
      super(a);
      this.threshold = (int) (a.length * overheadRatio);
      @SuppressWarnings("unchecked")
      final T[] tmpBuf = (T[]) new Object[threshold];
      this.tmp = tmpBuf;
    }

    private void mergeWithExtraMemory(int lo, int pivot, int hi, int len1, int len2) {
      System.arraycopy(a, lo, tmp, 0, len1);
      int i = 0, j = pivot, dest = lo;
      while (i < len1 && j < hi) {
        if (compare(tmp[i], a[j]) <= 0) {
          a[dest++] = tmp[i++];
        } else {
          a[dest++] = a[j++];
        }
      }
      while (i < len1) {
        a[dest++] = tmp[i++];
      }
      assert j == dest;
    }

    @Override
    protected void merge(int lo, int pivot, int hi, int len1, int len2) {
      if (len1 <= threshold) {
        mergeWithExtraMemory(lo, pivot, hi, len1, len2);
      } else {
        // since this method recurses to run merge on smaller arrays, it will
        // end up using mergeWithExtraMemory
        super.merge(lo, pivot, hi, len1, len2);
      }
    public int compare(T o1, T o2) {
      return o1.compareTo(o2);
     }

  }

  /** SorterTemplate with custom {@link Comparator} */
  private static <T> SorterTemplate getSorter(final T[] a, final Comparator<? super T> comp) {
    return new ArraySorterTemplate<T>(a) {

      @Override
      protected int compare(T a, T b) {
        return comp.compare(a, b);
      }

    };
   }
 
  /** Natural SorterTemplate */
  private static <T extends Comparable<? super T>> SorterTemplate getSorter(final T[] a) {
    return new ArraySorterTemplate<T>(a) {

      @Override
      protected int compare(T a, T b) {
        return a.compareTo(b);
      }
  private static final Comparator<?> NATURAL_COMPARATOR = new NaturalComparator<>();
 
    };
  /** Get the natural {@link Comparator} for the provided object class. */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Comparator<T> naturalComparator() {
    return (Comparator<T>) NATURAL_COMPARATOR;
   }
 
  /** SorterTemplate with custom {@link Comparator} for merge-based sorts. */
  private static <T> SorterTemplate getMergeSorter(final T[] a, final Comparator<? super T> comp) {
    if (a.length < MERGE_EXTRA_MEMORY_THRESHOLD) {
      return getSorter(a, comp);
    } else {
      return new ArrayMergeSorterTemplate<T>(a, MERGE_OVERHEAD_RATIO) {

        @Override
        protected int compare(T a, T b) {
          return comp.compare(a, b);
        }

      };
    }
  /** Swap values stored in slots <code>i</code> and <code>j</code> */
  public static <T> void swap(T[] arr, int i, int j) {
    final T tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
   }
 
  /** Natural SorterTemplate for merge-based sorts. */
  private static <T extends Comparable<? super T>> SorterTemplate getMergeSorter(final T[] a) {
    if (a.length < MERGE_EXTRA_MEMORY_THRESHOLD) {
      return getSorter(a);
    } else {
      return new ArrayMergeSorterTemplate<T>(a, MERGE_OVERHEAD_RATIO) {

        @Override
        protected int compare(T a, T b) {
          return a.compareTo(b);
        }

      };
    }
  }

  // quickSorts (endindex is exclusive!):
  // intro-sorts
   
   /**
   * Sorts the given array slice using the {@link Comparator}. This method uses the quick sort
   * Sorts the given array slice using the {@link Comparator}. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small arrays.
    * @param fromIndex start index (inclusive)
    * @param toIndex end index (exclusive)
    */
  public static <T> void quickSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
  public static <T> void introSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
     if (toIndex-fromIndex <= 1) return;
    getSorter(a, comp).quickSort(fromIndex, toIndex-1);
    new ArrayIntroSorter<>(a, comp).sort(fromIndex, toIndex);
   }
   
   /**
   * Sorts the given array using the {@link Comparator}. This method uses the quick sort
   * Sorts the given array using the {@link Comparator}. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small arrays.
    */
  public static <T> void quickSort(T[] a, Comparator<? super T> comp) {
    quickSort(a, 0, a.length, comp);
  public static <T> void introSort(T[] a, Comparator<? super T> comp) {
    introSort(a, 0, a.length, comp);
   }
   
   /**
   * Sorts the given array slice in natural order. This method uses the quick sort
   * Sorts the given array slice in natural order. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small arrays.
    * @param fromIndex start index (inclusive)
    * @param toIndex end index (exclusive)
    */
  public static <T extends Comparable<? super T>> void quickSort(T[] a, int fromIndex, int toIndex) {
  public static <T extends Comparable<? super T>> void introSort(T[] a, int fromIndex, int toIndex) {
     if (toIndex-fromIndex <= 1) return;
    getSorter(a).quickSort(fromIndex, toIndex-1);
    final Comparator<T> comp = naturalComparator();
    introSort(a, fromIndex, toIndex, comp);
   }
   
   /**
   * Sorts the given array in natural order. This method uses the quick sort
   * Sorts the given array in natural order. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small arrays.
    */
  public static <T extends Comparable<? super T>> void quickSort(T[] a) {
    quickSort(a, 0, a.length);
  public static <T extends Comparable<? super T>> void introSort(T[] a) {
    introSort(a, 0, a.length);
   }
 
  // mergeSorts:
  
  /**
   * Sorts the given array slice using the {@link Comparator}. This method uses the merge sort
   * algorithm, but falls back to insertion sort for small arrays.
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T> void mergeSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
    if (toIndex-fromIndex <= 1) return;
    //System.out.println("SORT: " + (toIndex-fromIndex));
    getMergeSorter(a, comp).mergeSort(fromIndex, toIndex-1);
  }
  
  /**
   * Sorts the given array using the {@link Comparator}. This method uses the merge sort
   * algorithm, but falls back to insertion sort for small arrays.
   */
  public static <T> void mergeSort(T[] a, Comparator<? super T> comp) {
    mergeSort(a, 0, a.length, comp);
  }
  
  /**
   * Sorts the given array slice in natural order. This method uses the merge sort
   * algorithm, but falls back to insertion sort for small arrays.
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T extends Comparable<? super T>> void mergeSort(T[] a, int fromIndex, int toIndex) {
    if (toIndex-fromIndex <= 1) return;
    getMergeSorter(a).mergeSort(fromIndex, toIndex-1);
  }
  // tim sorts:
   
   /**
   * Sorts the given array in natural order. This method uses the merge sort
   * algorithm, but falls back to insertion sort for small arrays.
   */
  public static <T extends Comparable<? super T>> void mergeSort(T[] a) {
    mergeSort(a, 0, a.length);
  }

  // timSorts:

  /**
   * Sorts the given array slice using the {@link Comparator}. This method uses the TimSort
   * Sorts the given array slice using the {@link Comparator}. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small arrays.
    * @param fromIndex start index (inclusive)
    * @param toIndex end index (exclusive)
    */
   public static <T> void timSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
     if (toIndex-fromIndex <= 1) return;
    getMergeSorter(a, comp).timSort(fromIndex, toIndex-1);
    new ArrayTimSorter<>(a, comp, a.length / 64).sort(fromIndex, toIndex);
   }
   
   /**
   * Sorts the given array using the {@link Comparator}. This method uses the TimSort
   * Sorts the given array using the {@link Comparator}. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small arrays.
    */
   public static <T> void timSort(T[] a, Comparator<? super T> comp) {
@@ -848,102 +691,23 @@ public final class ArrayUtil {
   }
   
   /**
   * Sorts the given array slice in natural order. This method uses the TimSort
   * Sorts the given array slice in natural order. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small arrays.
    * @param fromIndex start index (inclusive)
    * @param toIndex end index (exclusive)
    */
   public static <T extends Comparable<? super T>> void timSort(T[] a, int fromIndex, int toIndex) {
     if (toIndex-fromIndex <= 1) return;
    getMergeSorter(a).timSort(fromIndex, toIndex-1);
    final Comparator<T> comp = naturalComparator();
    timSort(a, fromIndex, toIndex, comp);
   }
   
   /**
   * Sorts the given array in natural order. This method uses the TimSort
   * Sorts the given array in natural order. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small arrays.
    */
   public static <T extends Comparable<? super T>> void timSort(T[] a) {
     timSort(a, 0, a.length);
   }
 
  // insertionSorts:
  
  /**
   * Sorts the given array slice using the {@link Comparator}. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small arrays!
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T> void insertionSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
    if (toIndex-fromIndex <= 1) return;
    getSorter(a, comp).insertionSort(fromIndex, toIndex-1);
  }
  
  /**
   * Sorts the given array using the {@link Comparator}. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small arrays!
   */
  public static <T> void insertionSort(T[] a, Comparator<? super T> comp) {
    insertionSort(a, 0, a.length, comp);
  }
  
  /**
   * Sorts the given array slice in natural order. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small arrays!
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T extends Comparable<? super T>> void insertionSort(T[] a, int fromIndex, int toIndex) {
    if (toIndex-fromIndex <= 1) return;
    getSorter(a).insertionSort(fromIndex, toIndex-1);
  }
  
  /**
   * Sorts the given array in natural order. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small arrays!
   */
  public static <T extends Comparable<? super T>> void insertionSort(T[] a) {
    insertionSort(a, 0, a.length);
  }

  // binarySorts:

  /**
   * Sorts the given array slice using the {@link Comparator}. This method uses the binary sort
   * algorithm. It is only recommended to use this algorithm for small arrays!
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T> void binarySort(T[] a, int fromIndex, int toIndex, Comparator<? super T> comp) {
    if (toIndex-fromIndex <= 1) return;
    getSorter(a, comp).binarySort(fromIndex, toIndex-1);
  }
  
  /**
   * Sorts the given array using the {@link Comparator}. This method uses the binary sort
   * algorithm. It is only recommended to use this algorithm for small arrays!
   */
  public static <T> void binarySort(T[] a, Comparator<? super T> comp) {
    binarySort(a, 0, a.length, comp);
  }
  
  /**
   * Sorts the given array slice in natural order. This method uses the binary sort
   * algorithm. It is only recommended to use this algorithm for small arrays!
   * @param fromIndex start index (inclusive)
   * @param toIndex end index (exclusive)
   */
  public static <T extends Comparable<? super T>> void binarySort(T[] a, int fromIndex, int toIndex) {
    if (toIndex-fromIndex <= 1) return;
    getSorter(a).binarySort(fromIndex, toIndex-1);
  }
  
  /**
   * Sorts the given array in natural order. This method uses the binary sort
   * algorithm. It is only recommended to use this algorithm for small arrays!
   */
  public static <T extends Comparable<? super T>> void binarySort(T[] a) {
    binarySort(a, 0, a.length);
  }

}
\ No newline at end of file
}
diff --git a/lucene/core/src/java/org/apache/lucene/util/BytesRefHash.java b/lucene/core/src/java/org/apache/lucene/util/BytesRefHash.java
index f757f965976..ed28c7078b2 100644
-- a/lucene/core/src/java/org/apache/lucene/util/BytesRefHash.java
++ b/lucene/core/src/java/org/apache/lucene/util/BytesRefHash.java
@@ -163,7 +163,7 @@ public final class BytesRefHash {
    */
   public int[] sort(final Comparator<BytesRef> comp) {
     final int[] compact = compact();
    new SorterTemplate() {
    new IntroSorter() {
       @Override
       protected void swap(int i, int j) {
         final int o = compact[i];
@@ -197,7 +197,7 @@ public final class BytesRefHash {
       
       private final BytesRef pivot = new BytesRef(),
         scratch1 = new BytesRef(), scratch2 = new BytesRef();
    }.quickSort(0, count - 1);
    }.sort(0, count);
     return compact;
   }
 
diff --git a/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java b/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
index 74877cca488..c8aef094912 100644
-- a/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
++ b/lucene/core/src/java/org/apache/lucene/util/CollectionUtil.java
@@ -17,9 +17,6 @@ package org.apache.lucene.util;
  * limitations under the License.
  */
 
import static org.apache.lucene.util.ArrayUtil.MERGE_EXTRA_MEMORY_THRESHOLD;
import static org.apache.lucene.util.ArrayUtil.MERGE_OVERHEAD_RATIO;

 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
@@ -37,16 +34,22 @@ import java.util.RandomAccess;
 public final class CollectionUtil {
 
   private CollectionUtil() {} // no instance
  private static final class ListIntroSorter<T> extends IntroSorter {
 
  private static abstract class ListSorterTemplate<T> extends SorterTemplate {

    protected final List<T> list;
    T pivot;
    final List<T> list;
    final Comparator<? super T> comp;
 
    ListSorterTemplate(List<T> list) {
    ListIntroSorter(List<T> list, Comparator<? super T> comp) {
      super();
       this.list = list;
      this.comp = comp;
     }
 
    protected abstract int compare(T a, T b);
    @Override
    protected void setPivot(int i) {
      pivot = list.get(i);
    }
 
     @Override
     protected void swap(int i, int j) {
@@ -55,257 +58,118 @@ public final class CollectionUtil {
 
     @Override
     protected int compare(int i, int j) {
      return compare(list.get(i), list.get(j));
    }

    @Override
    protected void setPivot(int i) {
      pivot = list.get(i);
      return comp.compare(list.get(i), list.get(j));
     }
 
     @Override
     protected int comparePivot(int j) {
      return compare(pivot, list.get(j));
      return comp.compare(pivot, list.get(j));
     }
 
    private T pivot;

   }
 
  // a template for merge-based sorts which uses extra memory to speed up merging
  private static abstract class ListMergeSorterTemplate<T> extends ListSorterTemplate<T> {
  private static final class ListTimSorter<T> extends TimSorter {
 
    private final int threshold; // maximum length of a merge that can be made using extra memory
    private final T[] tmp;
    final List<T> list;
    final Comparator<? super T> comp;
    final T[] tmp;
 
    ListMergeSorterTemplate(List<T> list, float overheadRatio) {
      super(list);
      this.threshold = (int) (list.size() * overheadRatio);
      @SuppressWarnings("unchecked")
      final T[] tmpBuf = (T[]) new Object[threshold];
      this.tmp = tmpBuf;
    }

    private void mergeWithExtraMemory(int lo, int pivot, int hi, int len1, int len2) {
      for (int i = 0; i < len1; ++i) {
        tmp[i] = list.get(lo + i);
      }
      int i = 0, j = pivot, dest = lo;
      while (i < len1 && j < hi) {
        if (compare(tmp[i], list.get(j)) <= 0) {
          list.set(dest++, tmp[i++]);
        } else {
          list.set(dest++, list.get(j++));
        }
      }
      while (i < len1) {
        list.set(dest++, tmp[i++]);
    @SuppressWarnings("unchecked")
    ListTimSorter(List<T> list, Comparator<? super T> comp, int maxTempSlots) {
      super(maxTempSlots);
      this.list = list;
      this.comp = comp;
      if (maxTempSlots > 0) {
        this.tmp = (T[]) new Object[maxTempSlots];
      } else {
        this.tmp = null;
       }
      assert j == dest;
     }
 
     @Override
    protected void merge(int lo, int pivot, int hi, int len1, int len2) {
      if (len1 <= threshold) {
        mergeWithExtraMemory(lo, pivot, hi, len1, len2);
      } else {
        // since this method recurses to run merge on smaller arrays, it will
        // end up using mergeWithExtraMemory
        super.merge(lo, pivot, hi, len1, len2);
      }
    protected void swap(int i, int j) {
      Collections.swap(list, i, j);
     }
 
  }

  /** SorterTemplate with custom {@link Comparator} */
  private static <T> SorterTemplate getSorter(final List<T> list, final Comparator<? super T> comp) {
    if (!(list instanceof RandomAccess))
      throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
    return new ListSorterTemplate<T>(list) {

      @Override
      protected int compare(T a, T b) {
        return comp.compare(a, b);
      }

    };
  }
  
  /** Natural SorterTemplate */
  private static <T extends Comparable<? super T>> SorterTemplate getSorter(final List<T> list) {
    if (!(list instanceof RandomAccess))
      throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
    return new ListSorterTemplate<T>(list) {
    @Override
    protected void copy(int src, int dest) {
      list.set(dest, list.get(src));
    }
 
      @Override
      protected int compare(T a, T b) {
        return a.compareTo(b);
    @Override
    protected void save(int i, int len) {
      for (int j = 0; j < len; ++j) {
        tmp[j] = list.get(i + j);
       }
    }
 
    };
  }

  /** SorterTemplate with custom {@link Comparator} for merge-based sorts. */
  private static <T> SorterTemplate getMergeSorter(final List<T> list, final Comparator<? super T> comp) {
    if (!(list instanceof RandomAccess))
      throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
    if (list.size() < MERGE_EXTRA_MEMORY_THRESHOLD) {
      return getSorter(list, comp);
    } else {
      return new ListMergeSorterTemplate<T>(list, MERGE_OVERHEAD_RATIO) {

        @Override
        protected int compare(T a, T b) {
          return comp.compare(a, b);
        }

      };
    @Override
    protected void restore(int i, int j) {
      list.set(j, tmp[i]);
     }
  }
  
  /** Natural SorterTemplate for merge-based sorts. */
  private static <T extends Comparable<? super T>> SorterTemplate getMergeSorter(final List<T> list) {
    if (!(list instanceof RandomAccess))
      throw new IllegalArgumentException("CollectionUtil can only sort random access lists in-place.");
    if (list.size() < MERGE_EXTRA_MEMORY_THRESHOLD) {
      return getSorter(list);
    } else {
      return new ListMergeSorterTemplate<T>(list, MERGE_OVERHEAD_RATIO) {
 
        @Override
        protected int compare(T a, T b) {
          return a.compareTo(b);
        }
    @Override
    protected int compare(int i, int j) {
      return comp.compare(list.get(i), list.get(j));
    }
 
      };
    @Override
    protected int compareSaved(int i, int j) {
      return comp.compare(tmp[i], list.get(j));
     }
  }
 
  /**
   * Sorts the given random access {@link List} using the {@link Comparator}.
   * The list must implement {@link RandomAccess}. This method uses the quick sort
   * algorithm, but falls back to insertion sort for small lists.
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T> void quickSort(List<T> list, Comparator<? super T> comp) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list, comp).quickSort(0, size-1);
  }
  
  /**
   * Sorts the given random access {@link List} in natural order.
   * The list must implement {@link RandomAccess}. This method uses the quick sort
   * algorithm, but falls back to insertion sort for small lists.
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T extends Comparable<? super T>> void quickSort(List<T> list) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list).quickSort(0, size-1);
   }
 
  // mergeSorts:
  
   /**
    * Sorts the given random access {@link List} using the {@link Comparator}.
   * The list must implement {@link RandomAccess}. This method uses the merge sort
   * The list must implement {@link RandomAccess}. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small lists.
    * @throws IllegalArgumentException if list is e.g. a linked list without random access.
    */
  public static <T> void mergeSort(List<T> list, Comparator<? super T> comp) {
  public static <T> void introSort(List<T> list, Comparator<? super T> comp) {
     final int size = list.size();
     if (size <= 1) return;
    getMergeSorter(list, comp).mergeSort(0, size-1);
    new ListIntroSorter<>(list, comp).sort(0, size);
   }
   
   /**
    * Sorts the given random access {@link List} in natural order.
   * The list must implement {@link RandomAccess}. This method uses the merge sort
   * The list must implement {@link RandomAccess}. This method uses the intro sort
    * algorithm, but falls back to insertion sort for small lists.
    * @throws IllegalArgumentException if list is e.g. a linked list without random access.
    */
  public static <T extends Comparable<? super T>> void mergeSort(List<T> list) {
  public static <T extends Comparable<? super T>> void introSort(List<T> list) {
     final int size = list.size();
     if (size <= 1) return;
    getMergeSorter(list).mergeSort(0, size-1);
    final Comparator<T> comp = ArrayUtil.naturalComparator();
    introSort(list, comp);
   }
 
  // timSorts:
  // Tim sorts:
   
   /**
    * Sorts the given random access {@link List} using the {@link Comparator}.
   * The list must implement {@link RandomAccess}. This method uses the TimSort
   * The list must implement {@link RandomAccess}. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small lists.
    * @throws IllegalArgumentException if list is e.g. a linked list without random access.
    */
   public static <T> void timSort(List<T> list, Comparator<? super T> comp) {
     final int size = list.size();
     if (size <= 1) return;
    getMergeSorter(list, comp).timSort(0, size-1);
    new ListTimSorter<>(list, comp, list.size() / 64).sort(0, size);
   }
   
   /**
    * Sorts the given random access {@link List} in natural order.
   * The list must implement {@link RandomAccess}. This method uses the TimSort
   * The list must implement {@link RandomAccess}. This method uses the Tim sort
    * algorithm, but falls back to binary sort for small lists.
    * @throws IllegalArgumentException if list is e.g. a linked list without random access.
    */
   public static <T extends Comparable<? super T>> void timSort(List<T> list) {
     final int size = list.size();
     if (size <= 1) return;
    getMergeSorter(list).timSort(0, size-1);
    final Comparator<T> comp = ArrayUtil.naturalComparator();
    timSort(list, comp);
   }
 
  // insertionSorts:
  
  /**
   * Sorts the given random access {@link List} using the {@link Comparator}.
   * The list must implement {@link RandomAccess}. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small lists!
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T> void insertionSort(List<T> list, Comparator<? super T> comp) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list, comp).insertionSort(0, size-1);
  }
  
  /**
   * Sorts the given random access {@link List} in natural order.
   * The list must implement {@link RandomAccess}. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for partially sorted small lists!
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T extends Comparable<? super T>> void insertionSort(List<T> list) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list).insertionSort(0, size-1);
  }

  // binarySorts:
  
  /**
   * Sorts the given random access {@link List} using the {@link Comparator}.
   * The list must implement {@link RandomAccess}. This method uses the binary sort
   * algorithm. It is only recommended to use this algorithm for small lists!
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T> void binarySort(List<T> list, Comparator<? super T> comp) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list, comp).binarySort(0, size-1);
  }
  
  /**
   * Sorts the given random access {@link List} in natural order.
   * The list must implement {@link RandomAccess}. This method uses the insertion sort
   * algorithm. It is only recommended to use this algorithm for small lists!
   * @throws IllegalArgumentException if list is e.g. a linked list without random access.
   */
  public static <T extends Comparable<? super T>> void binarySort(List<T> list) {
    final int size = list.size();
    if (size <= 1) return;
    getSorter(list).binarySort(0, size-1);
  }
}
\ No newline at end of file
}
diff --git a/lucene/core/src/java/org/apache/lucene/util/InPlaceMergeSorter.java b/lucene/core/src/java/org/apache/lucene/util/InPlaceMergeSorter.java
new file mode 100644
index 00000000000..54e5bde40ea
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/InPlaceMergeSorter.java
@@ -0,0 +1,46 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** {@link Sorter} implementation based on the merge-sort algorithm that merges
 *  in place (no extra memory will be allocated). Small arrays are sorted with
 *  insertion sort.
 *  @lucene.internal */
public abstract class InPlaceMergeSorter extends Sorter {

  /** Create a new {@link InPlaceMergeSorter} */
  public InPlaceMergeSorter() {}

  @Override
  public final void sort(int from, int to) {
    checkRange(from, to);
    mergeSort(from, to);
  }

  void mergeSort(int from, int to) {
    if (to - from < THRESHOLD) {
      insertionSort(from, to);
    } else {
      final int mid = (from + to) >>> 1;
      mergeSort(from, mid);
      mergeSort(mid, to);
      mergeInPlace(from, mid, to);
    }
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/IntroSorter.java b/lucene/core/src/java/org/apache/lucene/util/IntroSorter.java
new file mode 100644
index 00000000000..9efc4e8efe4
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/IntroSorter.java
@@ -0,0 +1,98 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * {@link Sorter} implementation based on a variant of the quicksort algorithm
 * called <a href="http://en.wikipedia.org/wiki/Introsort">introsort</a>: when
 * the recursion level exceeds the log of the length of the array to sort, it
 * falls back to heapsort. This prevents quicksort from running into its
 * worst-case quadratic runtime. Small arrays are sorted with
 * insertion sort.
 * @lucene.internal
 */
public abstract class IntroSorter extends Sorter {

  static int ceilLog2(int n) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(n - 1);
  }

  /** Create a new {@link IntroSorter}. */
  public IntroSorter() {}

  @Override
  public final void sort(int from, int to) {
    checkRange(from, to);
    quicksort(from, to, ceilLog2(to - from));
  }

  void quicksort(int from, int to, int maxDepth) {
    if (to - from < THRESHOLD) {
      insertionSort(from, to);
      return;
    } else if (--maxDepth < 0) {
      heapSort(from, to);
      return;
    }

    final int mid = (from + to) >>> 1;

    if (compare(from, mid) > 0) {
      swap(from, mid);
    }

    if (compare(mid, to - 1) > 0) {
      swap(mid, to - 1);
      if (compare(from, mid) > 0) {
        swap(from, mid);
      }
    }

    int left = from + 1;
    int right = to - 2;

    setPivot(mid);
    for (;;) {
      while (comparePivot(right) < 0) {
        --right;
      }

      while (left < right && comparePivot(left) >= 0) {
        ++left;
      }

      if (left < right) {
        swap(left, right);
        --right;
      } else {
        break;
      }
    }

    quicksort(from, left + 1, maxDepth);
    quicksort(left + 1, to, maxDepth);
  }

  /** Save the value at slot <code>i</code> so that it can later be used as a
   * pivot, see {@link #comparePivot(int)}. */
  protected abstract void setPivot(int i);

  /** Compare the pivot with the slot at <code>j</code>, similarly to
   *  {@link #compare(int, int) compare(i, j)}. */
  protected abstract int comparePivot(int j);
}
diff --git a/lucene/core/src/java/org/apache/lucene/util/Sorter.java b/lucene/core/src/java/org/apache/lucene/util/Sorter.java
new file mode 100644
index 00000000000..e54615cd1a8
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/Sorter.java
@@ -0,0 +1,248 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Comparator;

/** Base class for sorting algorithms implementations.
 * @lucene.internal */
public abstract class Sorter {

  static final int THRESHOLD = 20;

  /** Sole constructor, used for inheritance. */
  protected Sorter() {}

  /** Compare entries found in slots <code>i</code> and <code>j</code>.
   *  The contract for the returned value is the same as
   *  {@link Comparator#compare(Object, Object)}. */
  protected abstract int compare(int i, int j);

  /** Swap values at slots <code>i</code> and <code>j</code>. */
  protected abstract void swap(int i, int j);

  /** Sort the slice which starts at <code>from</code> (inclusive) and ends at
   *  <code>to</code> (exclusive). */
  public abstract void sort(int from, int to);

  void checkRange(int from, int to) {
    if (to < from) {
      throw new IllegalArgumentException("'to' must be >= 'from', got from=" + from + " and to=" + to);
    }
  }

  void mergeInPlace(int from, int mid, int to) {
    if (from == mid || mid == to || compare(mid - 1, mid) <= 0) {
      return;
    } else if (to - from == 2) {
      swap(mid - 1, mid);
      return;
    }
    while (compare(from, mid) <= 0) {
      ++from;
    }
    while (compare(mid - 1, to - 1) <= 0) {
      --to;
    }
    int first_cut, second_cut;
    int len11, len22;
    if (mid - from > to - mid) {
      len11 = (mid - from) >>> 1;
      first_cut = from + len11;
      second_cut = lower(mid, to, first_cut);
      len22 = second_cut - mid;
    } else {
      len22 = (to - mid) >>> 1;
      second_cut = mid + len22;
      first_cut = upper(from, mid, second_cut);
      len11 = first_cut - from;
    }
    rotate( first_cut, mid, second_cut);
    final int new_mid = first_cut + len22;
    mergeInPlace(from, first_cut, new_mid);
    mergeInPlace(new_mid, second_cut, to);
  }

  int lower(int from, int to, int val) {
    int len = to - from;
    while (len > 0) {
      final int half = len >>> 1;
      final int mid = from + half;
      if (compare(mid, val) < 0) {
        from = mid + 1;
        len = len - half -1;
      } else {
        len = half;
      }
    }
    return from;
  }

  int upper(int from, int to, int val) {
    int len = to - from;
    while (len > 0) {
      final int half = len >>> 1;
      final int mid = from + half;
      if (compare(val, mid) < 0) {
        len = half;
      } else {
        from = mid + 1;
        len = len - half -1;
      }
    }
    return from;
  }

  // faster than lower when val is at the end of [from:to[
  int lower2(int from, int to, int val) {
    int f = to - 1, t = to;
    while (f > from) {
      if (compare(f, val) < 0) {
        return lower(f, t, val);
      }
      final int delta = t - f;
      t = f;
      f -= delta << 1;
    }
    return lower(from, t, val);
  }

  // faster than upper when val is at the beginning of [from:to[
  int upper2(int from, int to, int val) {
    int f = from, t = f + 1;
    while (t < to) {
      if (compare(t, val) > 0) {
        return upper(f, t, val);
      }
      final int delta = t - f;
      f = t;
      t += delta << 1;
    }
    return upper(f, to, val);
  }

  final void reverse(int from, int to) {
    for (--to; from < to; ++from, --to) {
      swap(from, to);
    }
  }

  void rotate(int lo, int mid, int hi) {
    if (mid - lo == hi - mid) {
      // happens rarely but saves n/2 swaps
      while (mid < hi) {
        swap(lo++, mid++);
      }
    } else {
      reverse(lo, mid);
      reverse(mid, hi);
      reverse(lo, hi);
    }
  }

  void insertionSort(int from, int to) {
    for (int i = from + 1; i < to; ++i) {
      for (int j = i; j > from; --j) {
        if (compare(j - 1, j) > 0) {
          swap(j - 1, j);
        } else {
          break;
        }
      }
    }
  }

  void binarySort(int from, int to) {
    binarySort(from, to, from + 1);
  }

  void binarySort(int from, int to, int i) {
    for ( ; i < to; ++i) {
      int l = from;
      int h = i - 1;
      while (l <= h) {
        final int mid = (l + h) >>> 1;
        final int cmp = compare(i, mid);
        if (cmp < 0) {
          h = mid - 1;
        } else {
          l = mid + 1;
        }
      }
      switch (i - l) {
      case 2:
        swap(l + 1, l + 2);
      case 1:
        swap(l, l + 1);
      case 0:
        break;
      default:
        for (int j = i; j > l; --j) {
          swap(j - 1, j);
        }
        break;
      }
    }
  }

  void heapSort(int from, int to) {
    if (to - from <= 1) {
      return;
    }
    heapify(from, to);
    for (int end = to - 1; end > from; --end) {
      swap(from, end);
      siftDown(from, from, end);
    }
  }

  void heapify(int from, int to) {
    for (int i = heapParent(from, to - 1); i >= from; --i) {
      siftDown(i, from, to);
    }
  }

  void siftDown(int i, int from, int to) {
    for (int leftChild = heapChild(from, i); leftChild < to; leftChild = heapChild(from, i)) {
      final int rightChild = leftChild + 1;
      if (compare(i, leftChild) < 0) {
        if (rightChild < to && compare(leftChild, rightChild) < 0) {
          swap(i, rightChild);
          i = rightChild;
        } else {
          swap(i, leftChild);
          i = leftChild;
        }
      } else if (rightChild < to && compare(i, rightChild) < 0) {
        swap(i, rightChild);
        i = rightChild;
      } else {
        break;
      }
    }
  }

  static int heapParent(int from, int i) {
    return ((i - 1 - from) >>> 1) + from;
  }

  static int heapChild(int from, int i) {
    return ((i - from) << 1) + 1 + from;
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/SorterTemplate.java b/lucene/core/src/java/org/apache/lucene/util/SorterTemplate.java
deleted file mode 100644
index 7ee69d997a3..00000000000
-- a/lucene/core/src/java/org/apache/lucene/util/SorterTemplate.java
++ /dev/null
@@ -1,445 +0,0 @@
package org.apache.lucene.util;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This class was inspired by CGLIB, but provides a better
 * QuickSort algorithm without additional InsertionSort
 * at the end.
 * To use, subclass and override the four abstract methods
 * which compare and modify your data.
 * Allows custom swap so that two arrays can be sorted
 * at the same time.
 * @lucene.internal
 */
public abstract class SorterTemplate {

  private static final int TIMSORT_MINRUN = 32;
  private static final int TIMSORT_THRESHOLD = 64;
  private static final int TIMSORT_STACKSIZE = 40; // change if you change TIMSORT_MINRUN
  private static final int MERGESORT_THRESHOLD = 12;
  private static final int QUICKSORT_THRESHOLD = 7;

  static {
    // check whether TIMSORT_STACKSIZE is large enough
    // for a run length of TIMSORT_MINRUN and an array
    // of 2B values when TimSort invariants are verified
    final long[] lengths = new long[TIMSORT_STACKSIZE];
    lengths[0] = TIMSORT_MINRUN;
    lengths[1] = lengths[0] + 1;
    for (int i = 2; i < TIMSORT_STACKSIZE; ++i) {
      lengths[i] = lengths[i-2] + lengths[i-1] + 1;
    }
    if (lengths[TIMSORT_STACKSIZE - 1] < Integer.MAX_VALUE) {
      throw new Error("TIMSORT_STACKSIZE is too small");
    }
  }

  /** Implement this method, that swaps slots {@code i} and {@code j} in your data */
  protected abstract void swap(int i, int j);
  
  /** Compares slots {@code i} and {@code j} of you data.
   * Should be implemented like <code><em>valueOf(i)</em>.compareTo(<em>valueOf(j)</em>)</code> */
  protected abstract int compare(int i, int j);

  /** Implement this method, that stores the value of slot {@code i} as pivot value */
  protected abstract void setPivot(int i);
  
  /** Implements the compare function for the previously stored pivot value.
   * Should be implemented like <code>pivot.compareTo(<em>valueOf(j)</em>)</code> */
  protected abstract int comparePivot(int j);
  
  /** Sorts via stable in-place InsertionSort algorithm (O(n<sup>2</sup>))
   *(ideal for small collections which are mostly presorted). */
  public final void insertionSort(int lo, int hi) {
    for (int i = lo + 1 ; i <= hi; i++) {
      for (int j = i; j > lo; j--) {
        if (compare(j - 1, j) > 0) {
          swap(j - 1, j);
        } else {
          break;
        }
      }
    }
  }

  /** Sorts via stable in-place BinarySort algorithm (O(n<sup>2</sup>))
   * (ideal for small collections which are in random order). */
  public final void binarySort(int lo, int hi) {
    for (int i = lo + 1; i <= hi; ++i) {
      int l = lo;
      int h = i - 1;
      setPivot(i);
      while (l <= h) {
        final int mid = (l + h) >>> 1;
        final int cmp = comparePivot(mid);
        if (cmp < 0) {
          h = mid - 1;
        } else {
          l = mid + 1;
        }
      }
      for (int j = i; j > l; --j) {
        swap(j - 1, j);
      }
    }
  }

  /** Sorts via in-place, but unstable, QuickSort algorithm.
   * For small collections falls back to {@link #insertionSort(int,int)}. */
  public final void quickSort(final int lo, final int hi) {
    if (hi <= lo) return;
    // from Integer's Javadocs: ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
    quickSort(lo, hi, (Integer.SIZE - Integer.numberOfLeadingZeros(hi - lo)) << 1);
  }
  
  private void quickSort(int lo, int hi, int maxDepth) {
    // fall back to insertion when array has short length
    final int diff = hi - lo;
    if (diff <= QUICKSORT_THRESHOLD) {
      insertionSort(lo, hi);
      return;
    }
    
    // fall back to merge sort when recursion depth gets too big
    if (--maxDepth == 0) {
      mergeSort(lo, hi);
      return;
    }
    
    final int mid = lo + (diff >>> 1);
    
    if (compare(lo, mid) > 0) {
      swap(lo, mid);
    }

    if (compare(mid, hi) > 0) {
      swap(mid, hi);
      if (compare(lo, mid) > 0) {
        swap(lo, mid);
      }
    }
    
    int left = lo + 1;
    int right = hi - 1;

    setPivot(mid);
    for (;;) {
      while (comparePivot(right) < 0)
        --right;

      while (left < right && comparePivot(left) >= 0)
        ++left;

      if (left < right) {
        swap(left, right);
        --right;
      } else {
        break;
      }
    }

    quickSort(lo, left, maxDepth);
    quickSort(left + 1, hi, maxDepth);
  }

  /** TimSort implementation. The only difference with the spec is that this
   *  impl reuses {@link SorterTemplate#merge(int, int, int, int, int)} to
   *  merge runs (in place) instead of the original merging routine from
   *  TimSort (which requires extra memory but might be slightly faster). */
  private class TimSort {

    final int hi;
    final int minRun;
    final int[] runEnds;
    int stackSize;

    TimSort(int lo, int hi) {
      assert hi > lo;
      // +1 because the first slot is reserved and always lo
      runEnds = new int[TIMSORT_STACKSIZE + 1];
      runEnds[0] = lo;
      stackSize = 0;
      this.hi = hi;
      minRun = minRun(hi - lo + 1);
    }

    /** Minimum run length for an array of length <code>length</code>. */
    int minRun(int length) {
      assert length >= TIMSORT_MINRUN;
      int n = length;
      int r = 0;
      while (n >= 64) {
        r |= n & 1;
        n >>>= 1;
      }
      final int minRun = n + r;
      assert minRun >= TIMSORT_MINRUN && minRun <= 64;
      return minRun;
    }

    int runLen(int i) {
      final int off = stackSize - i;
      return runEnds[off] - runEnds[off - 1];
    }

    int runBase(int i) {
      return runEnds[stackSize - i - 1];
    }

    int runEnd(int i) {
      return runEnds[stackSize - i];
    }

    void setRunEnd(int i, int runEnd) {
      runEnds[stackSize - i] = runEnd;
    }

    void pushRunLen(int len) {
      runEnds[stackSize + 1] = runEnds[stackSize] + len;
      ++stackSize;
    }

    /** Merge run i with run i+1 */
    void mergeAt(int i) {
      assert stackSize > i + 1;
      final int l = runBase(i+1);
      final int pivot = runBase(i);
      final int h = runEnd(i);
      runMerge(l, pivot, h, pivot - l, h - pivot);
      for (int j = i + 1; j > 0; --j) {
        setRunEnd(j, runEnd(j-1));
      }
      --stackSize;
    }

    /** Compute the length of the next run, make the run sorted and return its
     *  length. */
    int nextRun() {
      final int runBase = runEnd(0);
      if (runBase == hi) {
        return 1;
      }
      int l = 1; // length of the run
      if (compare(runBase, runBase+1) > 0) {
        // run must be strictly descending
        while (runBase + l <= hi && compare(runBase + l - 1, runBase + l) > 0) {
          ++l;
        }
        if (l < minRun && runBase + l <= hi) {
          l = Math.min(hi - runBase + 1, minRun);
          binarySort(runBase, runBase + l - 1);
        } else {
          // revert
          for (int i = 0, halfL = l >>> 1; i < halfL; ++i) {
            swap(runBase + i, runBase + l - i - 1);
          }
        }
      } else {
        // run must be non-descending
        while (runBase + l <= hi && compare(runBase + l - 1, runBase + l) <= 0) {
          ++l;
        }
        if (l < minRun && runBase + l <= hi) {
          l = Math.min(hi - runBase + 1, minRun);
          binarySort(runBase, runBase + l - 1);
        } // else nothing to do, the run is already sorted
      }
      return l;
    }

    void ensureInvariants() {
      while (stackSize > 1) {
        final int runLen0 = runLen(0);
        final int runLen1 = runLen(1);

        if (stackSize > 2) {
          final int runLen2 = runLen(2);

          if (runLen2 <= runLen1 + runLen0) {
            // merge the smaller of 0 and 2 with 1
            if (runLen2 < runLen0) {
              mergeAt(1);
            } else {
              mergeAt(0);
            }
            continue;
          }
        }

        if (runLen1 <= runLen0) {
          mergeAt(0);
          continue;
        }

        break;
      }
    }

    void exhaustStack() {
      while (stackSize > 1) {
        mergeAt(0);
      }
    }

    void sort() {
      do {
        ensureInvariants();

        // Push a new run onto the stack
        pushRunLen(nextRun());

      } while (runEnd(0) <= hi);

      exhaustStack();
      assert runEnd(0) == hi + 1;
    }

  }

  /** Sorts using <a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">TimSort</a>, see 
   *  also <a href="http://svn.python.org/projects/python/trunk/Objects/listobject.c">source code</a>.
   *  TimSort is a stable sorting algorithm based on MergeSort but known to
   *  perform extremely well on partially-sorted inputs.
   *  For small collections, falls back to {@link #binarySort(int, int)}. */
  public final void timSort(int lo, int hi) {
    if (hi - lo <= TIMSORT_THRESHOLD) {
      binarySort(lo, hi);
      return;
    }

    new TimSort(lo, hi).sort();
  }

  /** Sorts via stable in-place MergeSort algorithm
   * For small collections falls back to {@link #insertionSort(int,int)}. */
  public final void mergeSort(int lo, int hi) {
    final int diff = hi - lo;
    if (diff <= MERGESORT_THRESHOLD) {
      insertionSort(lo, hi);
      return;
    }
    
    final int mid = lo + (diff >>> 1);
    
    mergeSort(lo, mid);
    mergeSort(mid, hi);
    runMerge(lo, mid, hi, mid - lo, hi - mid);
  }

  /** Sort out trivial cases and reduce the scope of the merge as much as
   *  possible before calling {@link #merge}/ */
  private void runMerge(int lo, int pivot, int hi, int len1, int len2) {
    if (len1 == 0 || len2 == 0) {
      return;
    }
    setPivot(pivot - 1);
    if (comparePivot(pivot) <= 0) {
      // all values from the first run are below all values from the 2nd run
      // this shortcut makes mergeSort run in linear time on sorted arrays
      return;
    }
    while (comparePivot(hi - 1) <= 0) {
      --hi;
      --len2;
    }
    setPivot(pivot);
    while (comparePivot(lo) >= 0) {
      ++lo;
      --len1;
    }
    if (len1 + len2 == 2) {
      assert len1 == len2;
      assert compare(lo, pivot) > 0;
      swap(pivot, lo);
      return;
    }
    merge(lo, pivot, hi, len1, len2);
  }

  /** Merge the slices [lo-pivot[ (of length len1) and [pivot-hi[ (of length
   *  len2) which are already sorted. This method merges in-place but can be
   *  extended to provide a faster implementation using extra memory. */
  protected void merge(int lo, int pivot, int hi, int len1, int len2) {
    int first_cut, second_cut;
    int len11, len22;
    if (len1 > len2) {
      len11 = len1 >>> 1;
      first_cut = lo + len11;
      second_cut = lower(pivot, hi, first_cut);
      len22 = second_cut - pivot;
    } else {
      len22 = len2 >>> 1;
      second_cut = pivot + len22;
      first_cut = upper(lo, pivot, second_cut);
      len11 = first_cut - lo;
    }
    rotate(first_cut, pivot, second_cut);
    final int new_mid = first_cut + len22;
    runMerge(lo, first_cut, new_mid, len11, len22);
    runMerge(new_mid, second_cut, hi, len1 - len11, len2 - len22);
  }

  private void rotate(int lo, int mid, int hi) {
    int lot = lo;
    int hit = mid - 1;
    while (lot < hit) {
      swap(lot++, hit--);
    }
    lot = mid; hit = hi - 1;
    while (lot < hit) {
      swap(lot++, hit--);
    }
    lot = lo; hit = hi - 1;
    while (lot < hit) {
      swap(lot++, hit--);
    }
  }

  private int lower(int lo, int hi, int val) {
    int len = hi - lo;
    while (len > 0) {
      final int half = len >>> 1,
        mid = lo + half;
      if (compare(mid, val) < 0) {
        lo = mid + 1;
        len = len - half -1;
      } else {
        len = half;
      }
    }
    return lo;
  }

  private int upper(int lo, int hi, int val) {
    int len = hi - lo;
    while (len > 0) {
      final int half = len >>> 1,
        mid = lo + half;
      if (compare(val, mid) < 0) {
        len = half;
      } else {
        lo = mid + 1;
        len = len - half -1;
      }
    }
    return lo;
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/TimSorter.java b/lucene/core/src/java/org/apache/lucene/util/TimSorter.java
new file mode 100644
index 00000000000..57e2f8d8c18
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/TimSorter.java
@@ -0,0 +1,373 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

/**
 * {@link Sorter} implementation based on the
 * <a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">TimSort</a>
 * algorithm.
 * <p>This implementation is especially good at sorting partially-sorted
 * arrays and sorts small arrays with binary sort.
 * <p><b>NOTE</b>:There are a few differences with the original implementation:<ul>
 * <li><a name="maxTempSlots"/>The extra amount of memory to perform merges is
 * configurable. This allows small merges to be very fast while large merges
 * will be performed in-place (slightly slower). You can make sure that the
 * fast merge routine will always be used by having <code>maxTempSlots</code>
 * equal to half of the length of the slice of data to sort.
 * <li>Only the fast merge routine can gallop (the one that doesn't run
 * in-place) and it only gallops on the longest slice.
 * </ul>
 * @lucene.internal
 */
public abstract class TimSorter extends Sorter {

  static final int MINRUN = 32;
  static final int THRESHOLD = 64;
  static final int STACKSIZE = 40; // depends on MINRUN
  static final int MIN_GALLOP = 7;

  final int maxTempSlots;
  int minRun;
  int to;
  int stackSize;
  int[] runEnds;

  /**
   * Create a new {@link TimSorter}.
   * @param maxTempSlots the <a href="#maxTempSlots">maximum amount of extra memory to run merges</a>
   */
  protected TimSorter(int maxTempSlots) {
    super();
    runEnds = new int[1 + STACKSIZE];
    this.maxTempSlots = maxTempSlots;
  }

  /** Minimum run length for an array of length <code>length</code>. */
  static int minRun(int length) {
    assert length >= MINRUN;
    int n = length;
    int r = 0;
    while (n >= 64) {
      r |= n & 1;
      n >>>= 1;
    }
    final int minRun = n + r;
    assert minRun >= MINRUN && minRun <= THRESHOLD;
    return minRun;
  }

  int runLen(int i) {
    final int off = stackSize - i;
    return runEnds[off] - runEnds[off - 1];
  }

  int runBase(int i) {
    return runEnds[stackSize - i - 1];
  }

  int runEnd(int i) {
    return runEnds[stackSize - i];
  }

  void setRunEnd(int i, int runEnd) {
    runEnds[stackSize - i] = runEnd;
  }

  void pushRunLen(int len) {
    runEnds[stackSize + 1] = runEnds[stackSize] + len;
    ++stackSize;
  }

  /** Compute the length of the next run, make the run sorted and return its
   *  length. */
  int nextRun() {
    final int runBase = runEnd(0);
    assert runBase < to;
    if (runBase == to - 1) {
      return 1;
    }
    int o = runBase + 2;
    if (compare(runBase, runBase+1) > 0) {
      // run must be strictly descending
      while (o < to && compare(o - 1, o) > 0) {
        ++o;
      }
      reverse(runBase, o);
    } else {
      // run must be non-descending
      while (o < to && compare(o - 1, o) <= 0) {
        ++o;
      }
    }
    final int runHi = Math.max(o, Math.min(to, runBase + minRun));
    binarySort(runBase, runHi, o);
    return runHi - runBase;
  }

  void ensureInvariants() {
    while (stackSize > 1) {
      final int runLen0 = runLen(0);
      final int runLen1 = runLen(1);

      if (stackSize > 2) {
        final int runLen2 = runLen(2);

        if (runLen2 <= runLen1 + runLen0) {
          // merge the smaller of 0 and 2 with 1
          if (runLen2 < runLen0) {
            mergeAt(1);
          } else {
            mergeAt(0);
          }
          continue;
        }
      }

      if (runLen1 <= runLen0) {
        mergeAt(0);
        continue;
      }

      break;
    }
  }

  void exhaustStack() {
    while (stackSize > 1) {
      mergeAt(0);
    }
  }

  void reset(int from, int to) {
    stackSize = 0;
    Arrays.fill(runEnds, 0);
    runEnds[0] = from;
    this.to = to;
    final int length = to - from;
    this.minRun = length <= THRESHOLD ? length : minRun(length);
  }

  void mergeAt(int n) {
    assert stackSize >= 2;
    merge(runBase(n + 1), runBase(n), runEnd(n));
    for (int j = n + 1; j > 0; --j) {
      setRunEnd(j, runEnd(j-1));
    }
    --stackSize;
  }

  void merge(int lo, int mid, int hi) {
    if (compare(mid - 1, mid) <= 0) {
      return;
    }
    lo = upper2(lo, mid, mid);
    hi = lower2(mid, hi, mid - 1);

    if (hi - mid <= mid - lo && hi - mid <= maxTempSlots) {
      mergeHi(lo, mid, hi);
    } else if (mid - lo <= maxTempSlots) {
      mergeLo(lo, mid, hi);
    } else {
      mergeInPlace(lo, mid, hi);
    }
  }

  @Override
  public void sort(int from, int to) {
    checkRange(from, to);
    if (to - from <= 1) {
      return;
    }
    reset(from, to);
    do {
      ensureInvariants();
      pushRunLen(nextRun());
    } while (runEnd(0) < to);
    exhaustStack();
    assert runEnd(0) == to;
  }

  @Override
  void rotate(int lo, int mid, int hi) {
    int len1 = mid - lo;
    int len2 = hi - mid;
    if (len1 == len2) {
      while (mid < hi) {
        swap(lo++, mid++);
      }
    } else if (len2 < len1 && len2 <= maxTempSlots) {
      save(mid, len2);
      for (int i = lo + len1 - 1, j = hi - 1; i >= lo; --i, --j) {
        copy(i, j);
      }
      for (int i = 0, j = lo; i < len2; ++i, ++j) {
        restore(i, j);
      }
    } else if (len1 <= maxTempSlots) {
      save(lo, len1);
      for (int i = mid, j = lo; i < hi; ++i, ++j) {
        copy(i, j);
      }
      for (int i = 0, j = lo + len2; j < hi; ++i, ++j) {
        restore(i, j);
      }
    } else {
      reverse(lo, mid);
      reverse(mid, hi);
      reverse(lo, hi);
    }
  }

  void mergeLo(int lo, int mid, int hi) {
    assert compare(lo, mid) > 0;
    int len1 = mid - lo;
    save(lo, len1);
    copy(mid, lo);
    int i = 0, j = mid + 1, dest = lo + 1;
    outer: for (;;) {
      for (int count = 0; count < MIN_GALLOP; ) {
        if (i >= len1 || j >= hi) {
          break outer;
        } else if (compareSaved(i, j) <= 0) {
          restore(i++, dest++);
          count = 0;
        } else {
          copy(j++, dest++);
          ++count;
        }
      }
      // galloping...
      int next = lowerSaved3(j, hi, i);
      for (; j < next; ++dest) {
        copy(j++, dest);
      }
      restore(i++, dest++);
    }
    for (; i < len1; ++dest) {
      restore(i++, dest);
    }
    assert j == dest;
  }

  void mergeHi(int lo, int mid, int hi) {
    assert compare(mid - 1, hi - 1) > 0;
    int len2 = hi - mid;
    save(mid, len2);
    copy(mid - 1, hi - 1);
    int i = mid - 2, j = len2 - 1, dest = hi - 2;
    outer: for (;;) {
      for (int count = 0; count < MIN_GALLOP; ) {
        if (i < lo || j < 0) {
          break outer;
        } else if (compareSaved(j, i) >= 0) {
          restore(j--, dest--);
          count = 0;
        } else {
          copy(i--, dest--);
          ++count;
        }
      }
      // galloping
      int next = upperSaved3(lo, i + 1, j);
      while (i >= next) {
        copy(i--, dest--);
      }
      restore(j--, dest--);
    }
    for (; j >= 0; --dest) {
      restore(j--, dest);
    }
    assert i == dest;
  }

  int lowerSaved(int from, int to, int val) {
    int len = to - from;
    while (len > 0) {
      final int half = len >>> 1;
      final int mid = from + half;
      if (compareSaved(val, mid) > 0) {
        from = mid + 1;
        len = len - half -1;
      } else {
        len = half;
      }
    }
    return from;
  }

  int upperSaved(int from, int to, int val) {
    int len = to - from;
    while (len > 0) {
      final int half = len >>> 1;
      final int mid = from + half;
      if (compareSaved(val, mid) < 0) {
        len = half;
      } else {
        from = mid + 1;
        len = len - half -1;
      }
    }
    return from;
  }

  // faster than lowerSaved when val is at the beginning of [from:to[
  int lowerSaved3(int from, int to, int val) {
    int f = from, t = f + 1;
    while (t < to) {
      if (compareSaved(val, t) <= 0) {
        return lowerSaved(f, t, val);
      }
      int delta = t - f;
      f = t;
      t += delta << 1;
    }
    return lowerSaved(f, to, val);
  }

  //faster than upperSaved when val is at the end of [from:to[
  int upperSaved3(int from, int to, int val) {
    int f = to - 1, t = to;
    while (f > from) {
      if (compareSaved(val, f) >= 0) {
        return upperSaved(f, t, val);
      }
      final int delta = t - f;
      t = f;
      f -= delta << 1;
    }
    return upperSaved(from, t, val);
  }

  /** Copy data from slot <code>src</code> to slot <code>dest</code>. */
  protected abstract void copy(int src, int dest);

  /** Save all elements between slots <code>i</code> and <code>i+len</code>
   *  into the temporary storage. */
  protected abstract void save(int i, int len);

  /** Restore element <code>j</code> from the temporary storage into slot <code>i</code>. */
  protected abstract void restore(int i, int j);

  /** Compare element <code>i</code> from the temporary storage with element
   *  <code>j</code> from the slice to sort, similarly to
   *  {@link #compare(int, int)}. */
  protected abstract int compareSaved(int i, int j);

}
diff --git a/lucene/core/src/java/org/apache/lucene/util/automaton/BasicOperations.java b/lucene/core/src/java/org/apache/lucene/util/automaton/BasicOperations.java
index 7424615eb0f..781a4e262e3 100644
-- a/lucene/core/src/java/org/apache/lucene/util/automaton/BasicOperations.java
++ b/lucene/core/src/java/org/apache/lucene/util/automaton/BasicOperations.java
@@ -557,8 +557,8 @@ final public class BasicOperations {
     }
 
     public void sort() {
      // mergesort seems to perform better on already sorted arrays:
      if (count > 1) ArrayUtil.mergeSort(points, 0, count);
      // Tim sort performs well on already sorted arrays:
      if (count > 1) ArrayUtil.timSort(points, 0, count);
     }
 
     public void add(Transition t) {
diff --git a/lucene/core/src/java/org/apache/lucene/util/automaton/State.java b/lucene/core/src/java/org/apache/lucene/util/automaton/State.java
index fb185a3f2a9..9dc229fd7db 100644
-- a/lucene/core/src/java/org/apache/lucene/util/automaton/State.java
++ b/lucene/core/src/java/org/apache/lucene/util/automaton/State.java
@@ -239,7 +239,7 @@ public class State implements Comparable<State> {
   /** Sorts transitions array in-place. */
   public void sortTransitions(Comparator<Transition> comparator) {
     // mergesort seems to perform better on already sorted arrays:
    if (numTransitions > 1) ArrayUtil.mergeSort(transitionsArray, 0, numTransitions, comparator);
    if (numTransitions > 1) ArrayUtil.timSort(transitionsArray, 0, numTransitions, comparator);
   }
   
   /**
diff --git a/lucene/core/src/test/org/apache/lucene/util/BaseSortTestCase.java b/lucene/core/src/test/org/apache/lucene/util/BaseSortTestCase.java
new file mode 100644
index 00000000000..8646f23f475
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/util/BaseSortTestCase.java
@@ -0,0 +1,173 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

public abstract class BaseSortTestCase extends LuceneTestCase {

  public static class Entry implements java.lang.Comparable<Entry> {

    public final int value;
    public final int ord;

    public Entry(int value, int ord) {
      this.value = value;
      this.ord = ord;
    }

    @Override
    public int compareTo(Entry other) {
      return value < other.value ? -1 : value == other.value ? 0 : 1;
    }

  }

  private final boolean stable;

  public BaseSortTestCase(boolean stable) {
    this.stable = stable;
  }

  public abstract Sorter newSorter(Entry[] arr);

  public void assertSorted(Entry[] original, Entry[] sorted) {
    assertEquals(original.length, sorted.length);
    Entry[] actuallySorted = Arrays.copyOf(original, original.length);
    Arrays.sort(actuallySorted);
    for (int i = 0; i < original.length; ++i) {
      assertEquals(actuallySorted[i].value, sorted[i].value);
      if (stable) {
        assertEquals(actuallySorted[i].ord, sorted[i].ord);
      }
    }
  }

  public void test(Entry[] arr) {
    final int o = random().nextInt(1000);
    final Entry[] toSort = new Entry[o + arr.length + random().nextInt(3)];
    System.arraycopy(arr, 0, toSort, o, arr.length);
    final Sorter sorter = newSorter(toSort);
    sorter.sort(o, o + arr.length);
    assertSorted(arr, Arrays.copyOfRange(toSort, o, o + arr.length));
  }

  enum Strategy {
    RANDOM {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = new Entry(random().nextInt(), i);
      }
    },
    RANDOM_LOW_CARDINALITY {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = new Entry(random().nextInt(6), i);
      }
    },
    ASCENDING {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = i == 0
            ? new Entry(random().nextInt(6), 0)
            : new Entry(arr[i - 1].value + random().nextInt(6), i);
      }
    },
    DESCENDING {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = i == 0
            ? new Entry(random().nextInt(6), 0)
            : new Entry(arr[i - 1].value - random().nextInt(6), i);
      }
    },
    STRICTLY_DESCENDING {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = i == 0
            ? new Entry(random().nextInt(6), 0)
            : new Entry(arr[i - 1].value - _TestUtil.nextInt(random(), 1, 5), i);
      }
    },
    ASCENDING_SEQUENCES {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = i == 0
            ? new Entry(random().nextInt(6), 0)
            : new Entry(rarely() ? random().nextInt(1000) : arr[i - 1].value + random().nextInt(6), i);
      }
    },
    MOSTLY_ASCENDING {
      @Override
      public void set(Entry[] arr, int i) {
        arr[i] = i == 0
            ? new Entry(random().nextInt(6), 0)
            : new Entry(arr[i - 1].value + _TestUtil.nextInt(random(), -8, 10), i);
      }
    };
    public abstract void set(Entry[] arr, int i);
  }

  public void test(Strategy strategy, int length) {
    final Entry[] arr = new Entry[length];
    for (int i = 0; i < arr.length; ++i) {
      strategy.set(arr, i);
    }
    test(arr);
  }

  public void test(Strategy strategy) {
    test(strategy, random().nextInt(20000));
  }

  public void testEmpty() {
    test(new Entry[0]);
  }

  public void testOne() {
    test(Strategy.RANDOM, 1);
  }

  public void testTwo() {
    test(Strategy.RANDOM_LOW_CARDINALITY, 2);
  }

  public void testRandom() {
    test(Strategy.RANDOM);
  }

  public void testRandomLowCardinality() {
    test(Strategy.RANDOM_LOW_CARDINALITY);
  }

  public void testAscending() {
    test(Strategy.ASCENDING);
  }

  public void testAscendingSequences() {
    test(Strategy.ASCENDING_SEQUENCES);
  }

  public void testDescending() {
    test(Strategy.DESCENDING);
  }

  public void testStrictlyDescending() {
    test(Strategy.STRICTLY_DESCENDING);
  }
}
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestArrayUtil.java b/lucene/core/src/test/org/apache/lucene/util/TestArrayUtil.java
index f59af0e06dc..5d3bdc92723 100644
-- a/lucene/core/src/test/org/apache/lucene/util/TestArrayUtil.java
++ b/lucene/core/src/test/org/apache/lucene/util/TestArrayUtil.java
@@ -128,21 +128,21 @@ public class TestArrayUtil extends LuceneTestCase {
     return a;
   }
   
  public void testQuickSort() {
  public void testIntroSort() {
     int num = atLeast(50);
     for (int i = 0; i < num; i++) {
       Integer[] a1 = createRandomArray(2000), a2 = a1.clone();
      ArrayUtil.quickSort(a1);
      ArrayUtil.introSort(a1);
       Arrays.sort(a2);
       assertArrayEquals(a2, a1);
       
       a1 = createRandomArray(2000);
       a2 = a1.clone();
      ArrayUtil.quickSort(a1, Collections.reverseOrder());
      ArrayUtil.introSort(a1, Collections.reverseOrder());
       Arrays.sort(a2, Collections.reverseOrder());
       assertArrayEquals(a2, a1);
       // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      ArrayUtil.quickSort(a1);
      ArrayUtil.introSort(a1);
       Arrays.sort(a2);
       assertArrayEquals(a2, a1);
     }
@@ -158,38 +158,18 @@ public class TestArrayUtil extends LuceneTestCase {
   }
   
   // This is a test for LUCENE-3054 (which fails without the merge sort fall back with stack overflow in most cases)
  public void testQuickToMergeSortFallback() {
  public void testQuickToHeapSortFallback() {
     int num = atLeast(50);
     for (int i = 0; i < num; i++) {
       Integer[] a1 = createSparseRandomArray(40000), a2 = a1.clone();
      ArrayUtil.quickSort(a1);
      ArrayUtil.introSort(a1);
       Arrays.sort(a2);
       assertArrayEquals(a2, a1);
     }
   }
   
  public void testMergeSort() {
    int num = atLeast(50);
    for (int i = 0; i < num; i++) {
      Integer[] a1 = createRandomArray(2000), a2 = a1.clone();
      ArrayUtil.mergeSort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
      
      a1 = createRandomArray(2000);
      a2 = a1.clone();
      ArrayUtil.mergeSort(a1, Collections.reverseOrder());
      Arrays.sort(a2, Collections.reverseOrder());
      assertArrayEquals(a2, a1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      ArrayUtil.mergeSort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
    }
  }

   public void testTimSort() {
    int num = atLeast(65);
    int num = atLeast(50);
     for (int i = 0; i < num; i++) {
       Integer[] a1 = createRandomArray(2000), a2 = a1.clone();
       ArrayUtil.timSort(a1);
@@ -207,44 +187,6 @@ public class TestArrayUtil extends LuceneTestCase {
       assertArrayEquals(a2, a1);
     }
   }

  public void testInsertionSort() {
    for (int i = 0, c = atLeast(500); i < c; i++) {
      Integer[] a1 = createRandomArray(30), a2 = a1.clone();
      ArrayUtil.insertionSort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
      
      a1 = createRandomArray(30);
      a2 = a1.clone();
      ArrayUtil.insertionSort(a1, Collections.reverseOrder());
      Arrays.sort(a2, Collections.reverseOrder());
      assertArrayEquals(a2, a1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      ArrayUtil.insertionSort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
    }
  }
  
  public void testBinarySort() {
    for (int i = 0, c = atLeast(500); i < c; i++) {
      Integer[] a1 = createRandomArray(30), a2 = a1.clone();
      ArrayUtil.binarySort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
      
      a1 = createRandomArray(30);
      a2 = a1.clone();
      ArrayUtil.binarySort(a1, Collections.reverseOrder());
      Arrays.sort(a2, Collections.reverseOrder());
      assertArrayEquals(a2, a1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      ArrayUtil.binarySort(a1);
      Arrays.sort(a2);
      assertArrayEquals(a2, a1);
    }
  }
   
   static class Item implements Comparable<Item> {
     final int val, order;
@@ -279,7 +221,7 @@ public class TestArrayUtil extends LuceneTestCase {
     
     if (VERBOSE) System.out.println("Before: " + Arrays.toString(items));
     // if you replace this with ArrayUtil.quickSort(), test should fail:
    ArrayUtil.mergeSort(items);
    ArrayUtil.timSort(items);
     if (VERBOSE) System.out.println("Sorted: " + Arrays.toString(items));
     
     Item last = items[0];
@@ -326,16 +268,10 @@ public class TestArrayUtil extends LuceneTestCase {
   // should produce no exceptions
   public void testEmptyArraySort() {
     Integer[] a = new Integer[0];
    ArrayUtil.quickSort(a);
    ArrayUtil.mergeSort(a);
    ArrayUtil.insertionSort(a);
    ArrayUtil.binarySort(a);
    ArrayUtil.introSort(a);
     ArrayUtil.timSort(a);
    ArrayUtil.quickSort(a, Collections.reverseOrder());
    ArrayUtil.mergeSort(a, Collections.reverseOrder());
    ArrayUtil.introSort(a, Collections.reverseOrder());
     ArrayUtil.timSort(a, Collections.reverseOrder());
    ArrayUtil.insertionSort(a, Collections.reverseOrder());
    ArrayUtil.binarySort(a, Collections.reverseOrder());
   }
   
 }
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestCollectionUtil.java b/lucene/core/src/test/org/apache/lucene/util/TestCollectionUtil.java
index c45130f5b4b..36558562e46 100644
-- a/lucene/core/src/test/org/apache/lucene/util/TestCollectionUtil.java
++ b/lucene/core/src/test/org/apache/lucene/util/TestCollectionUtil.java
@@ -35,39 +35,20 @@ public class TestCollectionUtil extends LuceneTestCase {
     return Arrays.asList(a);
   }
   
  public void testQuickSort() {
  public void testIntroSort() {
     for (int i = 0, c = atLeast(500); i < c; i++) {
       List<Integer> list1 = createRandomList(2000), list2 = new ArrayList<Integer>(list1);
      CollectionUtil.quickSort(list1);
      CollectionUtil.introSort(list1);
       Collections.sort(list2);
       assertEquals(list2, list1);
       
       list1 = createRandomList(2000);
       list2 = new ArrayList<Integer>(list1);
      CollectionUtil.quickSort(list1, Collections.reverseOrder());
      CollectionUtil.introSort(list1, Collections.reverseOrder());
       Collections.sort(list2, Collections.reverseOrder());
       assertEquals(list2, list1);
       // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      CollectionUtil.quickSort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
    }
  }
  
  public void testMergeSort() {
    for (int i = 0, c = atLeast(500); i < c; i++) {
      List<Integer> list1 = createRandomList(2000), list2 = new ArrayList<Integer>(list1);
      CollectionUtil.mergeSort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
      
      list1 = createRandomList(2000);
      list2 = new ArrayList<Integer>(list1);
      CollectionUtil.mergeSort(list1, Collections.reverseOrder());
      Collections.sort(list2, Collections.reverseOrder());
      assertEquals(list2, list1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      CollectionUtil.mergeSort(list1);
      CollectionUtil.introSort(list1);
       Collections.sort(list2);
       assertEquals(list2, list1);
     }
@@ -92,86 +73,30 @@ public class TestCollectionUtil extends LuceneTestCase {
     }
   }
 
  public void testInsertionSort() {
    for (int i = 0, c = atLeast(500); i < c; i++) {
      List<Integer> list1 = createRandomList(30), list2 = new ArrayList<Integer>(list1);
      CollectionUtil.insertionSort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
      
      list1 = createRandomList(30);
      list2 = new ArrayList<Integer>(list1);
      CollectionUtil.insertionSort(list1, Collections.reverseOrder());
      Collections.sort(list2, Collections.reverseOrder());
      assertEquals(list2, list1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      CollectionUtil.insertionSort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
    }
  }

  public void testBinarySort() {
    for (int i = 0, c = atLeast(500); i < c; i++) {
      List<Integer> list1 = createRandomList(30), list2 = new ArrayList<Integer>(list1);
      CollectionUtil.binarySort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
      
      list1 = createRandomList(30);
      list2 = new ArrayList<Integer>(list1);
      CollectionUtil.binarySort(list1, Collections.reverseOrder());
      Collections.sort(list2, Collections.reverseOrder());
      assertEquals(list2, list1);
      // reverse back, so we can test that completely backwards sorted array (worst case) is working:
      CollectionUtil.binarySort(list1);
      Collections.sort(list2);
      assertEquals(list2, list1);
    }
  }

   public void testEmptyListSort() {
     // should produce no exceptions
     List<Integer> list = Arrays.asList(new Integer[0]); // LUCENE-2989
    CollectionUtil.quickSort(list);
    CollectionUtil.mergeSort(list);
    CollectionUtil.introSort(list);
     CollectionUtil.timSort(list);
    CollectionUtil.insertionSort(list);
    CollectionUtil.binarySort(list);
    CollectionUtil.quickSort(list, Collections.reverseOrder());
    CollectionUtil.mergeSort(list, Collections.reverseOrder());
    CollectionUtil.introSort(list, Collections.reverseOrder());
     CollectionUtil.timSort(list, Collections.reverseOrder());
    CollectionUtil.insertionSort(list, Collections.reverseOrder());
    CollectionUtil.binarySort(list, Collections.reverseOrder());
     
     // check that empty non-random access lists pass sorting without ex (as sorting is not needed)
     list = new LinkedList<Integer>();
    CollectionUtil.quickSort(list);
    CollectionUtil.mergeSort(list);
    CollectionUtil.introSort(list);
     CollectionUtil.timSort(list);
    CollectionUtil.insertionSort(list);
    CollectionUtil.binarySort(list);
    CollectionUtil.quickSort(list, Collections.reverseOrder());
    CollectionUtil.mergeSort(list, Collections.reverseOrder());
    CollectionUtil.introSort(list, Collections.reverseOrder());
     CollectionUtil.timSort(list, Collections.reverseOrder());
    CollectionUtil.insertionSort(list, Collections.reverseOrder());
    CollectionUtil.binarySort(list, Collections.reverseOrder());
   }
   
   public void testOneElementListSort() {
     // check that one-element non-random access lists pass sorting without ex (as sorting is not needed)
     List<Integer> list = new LinkedList<Integer>();
     list.add(1);
    CollectionUtil.quickSort(list);
    CollectionUtil.mergeSort(list);
    CollectionUtil.introSort(list);
     CollectionUtil.timSort(list);
    CollectionUtil.insertionSort(list);
    CollectionUtil.binarySort(list);
    CollectionUtil.quickSort(list, Collections.reverseOrder());
    CollectionUtil.mergeSort(list, Collections.reverseOrder());
    CollectionUtil.introSort(list, Collections.reverseOrder());
     CollectionUtil.timSort(list, Collections.reverseOrder());
    CollectionUtil.insertionSort(list, Collections.reverseOrder());
    CollectionUtil.binarySort(list, Collections.reverseOrder());
   }
   
 }
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestInPlaceMergeSorter.java b/lucene/core/src/test/org/apache/lucene/util/TestInPlaceMergeSorter.java
new file mode 100644
index 00000000000..6c1fe5762b1
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/util/TestInPlaceMergeSorter.java
@@ -0,0 +1,36 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;

@RunWith(RandomizedRunner.class)
public class TestInPlaceMergeSorter extends BaseSortTestCase {

  public TestInPlaceMergeSorter() {
    super(true);
  }

  @Override
  public Sorter newSorter(Entry[] arr) {
    return new ArrayInPlaceMergeSorter<Entry>(arr, ArrayUtil.<Entry>naturalComparator());
  }

}
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestIntroSorter.java b/lucene/core/src/test/org/apache/lucene/util/TestIntroSorter.java
new file mode 100644
index 00000000000..63e9f9548e8
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/util/TestIntroSorter.java
@@ -0,0 +1,32 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


public class TestIntroSorter extends BaseSortTestCase {

  public TestIntroSorter() {
    super(false);
  }

  @Override
  public Sorter newSorter(Entry[] arr) {
    return new ArrayIntroSorter<Entry>(arr, ArrayUtil.<Entry>naturalComparator());
  }

}
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestSorterTemplate.java b/lucene/core/src/test/org/apache/lucene/util/TestSorterTemplate.java
deleted file mode 100644
index ca824fc3f94..00000000000
-- a/lucene/core/src/test/org/apache/lucene/util/TestSorterTemplate.java
++ /dev/null
@@ -1,181 +0,0 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

public class TestSorterTemplate extends LuceneTestCase {

  private static final int SLOW_SORT_THRESHOLD = 1000;

  // A sorter template that compares only the last 32 bits
  static class Last32BitsSorterTemplate extends SorterTemplate {

    final long[] arr;
    long pivot;

    Last32BitsSorterTemplate(long[] arr) {
      this.arr = arr;
    }

    @Override
    protected void swap(int i, int j) {
      final long tmp = arr[i];
      arr[i] = arr[j];
      arr[j] = tmp;
    }

    private int compareValues(long i, long j) {
      // only compare the last 32 bits
      final long a = i & 0xFFFFFFFFL;
      final long b = j & 0xFFFFFFFFL;
      return Long.compare(a, b);
    }

    @Override
    protected int compare(int i, int j) {
      return compareValues(arr[i], arr[j]);
    }

    @Override
    protected void setPivot(int i) {
      pivot = arr[i];
    }

    @Override
    protected int comparePivot(int j) {
      return compareValues(pivot, arr[j]);
    }

    @Override
    protected void merge(int lo, int pivot, int hi, int len1, int len2) {
      // timSort and mergeSort should call runMerge to sort out trivial cases
      assertTrue(len1 >= 1);
      assertTrue(len2 >= 1);
      assertTrue(len1 + len2 >= 3);
      assertTrue(compare(lo, pivot) > 0);
      assertTrue(compare(pivot - 1, hi - 1) > 0);
      assertFalse(compare(pivot - 1, pivot) <= 0);
      super.merge(lo, pivot, hi, len1, len2);
    }

  }

  void testSort(int[] intArr) {
    // we modify the array as a long[] and store the original ord in the first 32 bits
    // to be able to check stability
    final long[] arr = toLongsAndOrds(intArr);

    // use MergeSort as a reference
    // assertArrayEquals checks for sorting + stability
    // assertArrayEquals(toInts) checks for sorting only
    final long[] mergeSorted = Arrays.copyOf(arr, arr.length);
    new Last32BitsSorterTemplate(mergeSorted).mergeSort(0, arr.length - 1);

    if (arr.length < SLOW_SORT_THRESHOLD) {
      final long[] insertionSorted = Arrays.copyOf(arr, arr.length);
      new Last32BitsSorterTemplate(insertionSorted).insertionSort(0, arr.length - 1);
      assertArrayEquals(mergeSorted, insertionSorted);
      
      final long[] binarySorted = Arrays.copyOf(arr, arr.length);
      new Last32BitsSorterTemplate(binarySorted).binarySort(0, arr.length - 1);
      assertArrayEquals(mergeSorted, binarySorted);
    }

    final long[] quickSorted = Arrays.copyOf(arr, arr.length);
    new Last32BitsSorterTemplate(quickSorted).quickSort(0, arr.length - 1);
    assertArrayEquals(toInts(mergeSorted), toInts(quickSorted));

    final long[] timSorted = Arrays.copyOf(arr, arr.length);
    new Last32BitsSorterTemplate(timSorted).timSort(0, arr.length - 1);
    assertArrayEquals(mergeSorted, timSorted);
  }

  private int[] toInts(long[] longArr) {
    int[] arr = new int[longArr.length];
    for (int i = 0; i < longArr.length; ++i) {
      arr[i] = (int) longArr[i];
    }
    return arr;
  }

  private long[] toLongsAndOrds(int[] intArr) {
    final long[] arr = new long[intArr.length];
    for (int i = 0; i < intArr.length; ++i) {
      arr[i] = (((long) i) << 32) | (intArr[i] & 0xFFFFFFFFL);
    }
    return arr;
  }

  int randomLength() {
    return _TestUtil.nextInt(random(), 1, random().nextBoolean() ? SLOW_SORT_THRESHOLD : 100000);
  }

  public void testEmpty() {
    testSort(new int[0]);
  }

  public void testAscending() {
    final int length = randomLength();
    final int[] arr = new int[length];
    arr[0] = random().nextInt(10);
    for (int i = 1; i < arr.length; ++i) {
      arr[i] = arr[i-1] + _TestUtil.nextInt(random(), 0, 10);
    }
    testSort(arr);
  }

  public void testDescending() {
    final int length = randomLength();
    final int[] arr = new int[length];
    arr[0] = random().nextInt(10);
    for (int i = 1; i < arr.length; ++i) {
      arr[i] = arr[i-1] - _TestUtil.nextInt(random(), 0, 10);
    }
    testSort(arr);
  }

  public void testStrictlyDescending() {
    final int length = randomLength();
    final int[] arr = new int[length];
    arr[0] = random().nextInt(10);
    for (int i = 1; i < arr.length; ++i) {
      arr[i] = arr[i-1] - _TestUtil.nextInt(random(), 1, 10);
    }
    testSort(arr);
  }

  public void testRandom1() {
    final int length = randomLength();
    final int[] arr = new int[length];
    for (int i = 1; i < arr.length; ++i) {
      arr[i] = random().nextInt();
    }
    testSort(arr);
  }

  public void testRandom2() {
    final int length = randomLength();
    final int[] arr = new int[length];
    for (int i = 1; i < arr.length; ++i) {
      arr[i] = random().nextInt(10);
    }
    testSort(arr);
  }

}
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestTimSorter.java b/lucene/core/src/test/org/apache/lucene/util/TestTimSorter.java
new file mode 100644
index 00000000000..df1899644e9
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/util/TestTimSorter.java
@@ -0,0 +1,31 @@
package org.apache.lucene.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TestTimSorter extends BaseSortTestCase {

  public TestTimSorter() {
    super(true);
  }

  @Override
  public Sorter newSorter(Entry[] arr) {
    return new ArrayTimSorter<Entry>(arr, ArrayUtil.<Entry>naturalComparator(), random().nextInt(arr.length));
  }

}
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/search/TestDrillSideways.java b/lucene/facet/src/test/org/apache/lucene/facet/search/TestDrillSideways.java
index b2e403cb983..79b62c79e70 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/search/TestDrillSideways.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/search/TestDrillSideways.java
@@ -65,8 +65,8 @@ import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.InPlaceMergeSorter;
 import org.apache.lucene.util.InfoStream;
import org.apache.lucene.util.SorterTemplate;
 import org.apache.lucene.util._TestUtil;
 
 public class TestDrillSideways extends FacetTestCase {
@@ -875,9 +875,7 @@ public class TestDrillSideways extends FacetTestCase {
 
     // Naive (on purpose, to reduce bug in tester/gold):
     // sort all ids, then return top N slice:
    new SorterTemplate() {

      private int pivot;
    new InPlaceMergeSorter() {
 
       @Override
       protected void swap(int i, int j) {
@@ -901,26 +899,7 @@ public class TestDrillSideways extends FacetTestCase {
         }
       }
 
      @Override
      protected void setPivot(int i) {
        pivot = ids[i];
      }

      @Override
      protected int comparePivot(int j) {
        int counti = counts[pivot];
        int countj = counts[ids[j]];
        // Sort by count descending...
        if (counti > countj) {
          return -1;
        } else if (counti < countj) {
          return 1;
        } else {
          // ... then by ord ascending:
          return new BytesRef(values[pivot]).compareTo(new BytesRef(values[ids[j]]));
        }
      }
    }.mergeSort(0, ids.length-1);
    }.sort(0, ids.length);
 
     if (topN > ids.length) {
       topN = ids.length;
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenSources.java b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenSources.java
index 4d1d11b3c89..db45d8bdd76 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenSources.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenSources.java
@@ -251,7 +251,7 @@ public class TokenSources {
     if (unsortedTokens != null) {
       tokensInOriginalOrder = unsortedTokens.toArray(new Token[unsortedTokens
           .size()]);
      ArrayUtil.mergeSort(tokensInOriginalOrder, new Comparator<Token>() {
      ArrayUtil.timSort(tokensInOriginalOrder, new Comparator<Token>() {
         @Override
         public int compare(Token t1, Token t2) {
           if (t1.startOffset() == t2.startOffset()) return t1.endOffset()
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenStreamFromTermPositionVector.java b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenStreamFromTermPositionVector.java
index dd07e296640..4057bd96950 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenStreamFromTermPositionVector.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/highlight/TokenStreamFromTermPositionVector.java
@@ -86,7 +86,7 @@ public final class TokenStreamFromTermPositionVector extends TokenStream {
         this.positionedTokens.add(token);
       }
     }
    CollectionUtil.mergeSort(this.positionedTokens, tokenComparator);
    CollectionUtil.timSort(this.positionedTokens, tokenComparator);
     int lastPosition = -1;
     for (final Token token : this.positionedTokens) {
       int thisPosition = token.getPositionIncrement();
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/Passage.java b/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/Passage.java
index 734869bece5..aea1a179f19 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/Passage.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/Passage.java
@@ -19,8 +19,8 @@ package org.apache.lucene.search.postingshighlight;
 
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.InPlaceMergeSorter;
 import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.SorterTemplate;
 
 /**
  * Represents a passage (typically a sentence of the document). 
@@ -64,7 +64,7 @@ public final class Passage {
     final int starts[] = matchStarts;
     final int ends[] = matchEnds;
     final BytesRef terms[] = matchTerms;
    new SorterTemplate() {
    new InPlaceMergeSorter() {
       @Override
       protected void swap(int i, int j) {
         int temp = starts[i];
@@ -85,18 +85,7 @@ public final class Passage {
         return Integer.compare(starts[i], starts[j]);
       }
 
      @Override
      protected void setPivot(int i) {
        pivot = starts[i];
      }

      @Override
      protected int comparePivot(int j) {
        return Integer.compare(pivot, starts[j]);
      }
      
      int pivot;
    }.mergeSort(0, numMatches-1);
    }.sort(0, numMatches);
   }
   
   void reset() {
diff --git a/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java b/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java
index d89b191edef..850c77a437d 100644
-- a/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java
++ b/lucene/highlighter/src/java/org/apache/lucene/search/postingshighlight/PostingsHighlighter.java
@@ -48,7 +48,7 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SorterTemplate;
import org.apache.lucene.util.InPlaceMergeSorter;
 import org.apache.lucene.util.UnicodeUtil;
 
 /**
@@ -313,9 +313,8 @@ public class PostingsHighlighter {
 
     // sort for sequential io
     Arrays.sort(docids);
    new SorterTemplate() {
      String pivot;
      
    new InPlaceMergeSorter() {

       @Override
       protected void swap(int i, int j) {
         String tmp = fields[i];
@@ -330,18 +329,8 @@ public class PostingsHighlighter {
       protected int compare(int i, int j) {
         return fields[i].compareTo(fields[j]);
       }

      @Override
      protected void setPivot(int i) {
        pivot = fields[i];
      }

      @Override
      protected int comparePivot(int j) {
        return pivot.compareTo(fields[j]);
      }
       
    }.mergeSort(0, fields.length-1);
    }.sort(0, fields.length);
     
     // pull stored data:
     String[][] contents = loadFieldValues(searcher, fields, docids, maxLength);
diff --git a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index d6a31ae03ad..43feb828fb5 100644
-- a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -572,7 +572,7 @@ public class MemoryIndex {
       entries[i] = iter.next();
     }
     
    if (size > 1) ArrayUtil.quickSort(entries, termComparator);
    if (size > 1) ArrayUtil.introSort(entries, termComparator);
     return entries;
   }
   
diff --git a/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java b/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
index 2b80764632a..f12d5f8e42f 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
++ b/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
@@ -87,7 +87,7 @@ public class CompoundFileExtractor {
       cfr = new CompoundFileDirectory(dir, filename, IOContext.DEFAULT, false);
 
       String [] files = cfr.listAll();
      ArrayUtil.mergeSort(files);   // sort the array of filename so that the output is more readable
      ArrayUtil.timSort(files);   // sort the array of filename so that the output is more readable
 
       for (int i = 0; i < files.length; ++i) {
         long len = cfr.fileLength(files[i]);
diff --git a/lucene/misc/src/java/org/apache/lucene/index/sorter/Sorter.java b/lucene/misc/src/java/org/apache/lucene/index/sorter/Sorter.java
index 325b2dc3df0..05e8563ff4e 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/sorter/Sorter.java
++ b/lucene/misc/src/java/org/apache/lucene/index/sorter/Sorter.java
@@ -22,7 +22,7 @@ import java.util.Comparator;
 
 import org.apache.lucene.index.AtomicReader;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.SorterTemplate;
import org.apache.lucene.util.TimSorter;
 import org.apache.lucene.util.packed.MonotonicAppendingLongBuffer;
 
 /**
@@ -113,16 +113,17 @@ public abstract class Sorter {
     }
   };
   
  private static final class DocValueSorterTemplate extends SorterTemplate {
  private static final class DocValueSorter extends TimSorter {
     
     private final int[] docs;
     private final Sorter.DocComparator comparator;
    private final int[] tmp;
     
    private int pivot;
    
    public DocValueSorterTemplate(int[] docs, Sorter.DocComparator comparator) {
    public DocValueSorter(int[] docs, Sorter.DocComparator comparator) {
      super(docs.length / 64);
       this.docs = docs;
       this.comparator = comparator;
      tmp = new int[docs.length / 64];
     }
     
     @Override
@@ -130,22 +131,32 @@ public abstract class Sorter {
       return comparator.compare(docs[i], docs[j]);
     }
     
    @Override
    protected int comparePivot(int j) {
      return comparator.compare(pivot, docs[j]);
    }
    
    @Override
    protected void setPivot(int i) {
      pivot = docs[i];
    }
    
     @Override
     protected void swap(int i, int j) {
       int tmpDoc = docs[i];
       docs[i] = docs[j];
       docs[j] = tmpDoc;
     }

    @Override
    protected void copy(int src, int dest) {
      docs[dest] = docs[src];
    }

    @Override
    protected void save(int i, int len) {
      System.arraycopy(docs, i, tmp, 0, len);
    }

    @Override
    protected void restore(int i, int j) {
      docs[j] = tmp[i];
    }

    @Override
    protected int compareSaved(int i, int j) {
      return comparator.compare(tmp[i], docs[j]);
    }
   }
 
   /** Computes the old-to-new permutation over the given comparator. */
@@ -168,10 +179,10 @@ public abstract class Sorter {
       docs[i] = i;
     }
     
    SorterTemplate sorter = new DocValueSorterTemplate(docs, comparator);
    DocValueSorter sorter = new DocValueSorter(docs, comparator);
     // It can be common to sort a reader, add docs, sort it again, ... and in
     // that case timSort can save a lot of time
    sorter.timSort(0, docs.length - 1); // docs is now the newToOld mapping
    sorter.sort(0, docs.length); // docs is now the newToOld mapping
 
     // The reason why we use MonotonicAppendingLongBuffer here is that it
     // wastes very little memory if the index is in random order but can save
diff --git a/lucene/misc/src/java/org/apache/lucene/index/sorter/SortingAtomicReader.java b/lucene/misc/src/java/org/apache/lucene/index/sorter/SortingAtomicReader.java
index 62a681f3e95..f7cce128692 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/sorter/SortingAtomicReader.java
++ b/lucene/misc/src/java/org/apache/lucene/index/sorter/SortingAtomicReader.java
@@ -43,7 +43,7 @@ import org.apache.lucene.store.RAMOutputStream;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SorterTemplate;
import org.apache.lucene.util.TimSorter;
 import org.apache.lucene.util.automaton.CompiledAutomaton;
 
 /**
@@ -157,7 +157,7 @@ public class SortingAtomicReader extends FilterAtomicReader {
 
       final DocsEnum inDocs = in.docs(newToOld(liveDocs), inReuse, flags);
       final boolean withFreqs = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >=0 && (flags & DocsEnum.FLAG_FREQS) != 0;
      return new SortingDocsEnum(wrapReuse, inDocs, withFreqs, docMap);
      return new SortingDocsEnum(docMap.size(), wrapReuse, inDocs, withFreqs, docMap);
     }
 
     @Override
@@ -184,7 +184,7 @@ public class SortingAtomicReader extends FilterAtomicReader {
       // ask for everything. if that assumption changes in the future, we can
       // factor in whether 'flags' says offsets are not required.
       final boolean storeOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
      return new SortingDocsAndPositionsEnum(wrapReuse, inDocsAndPositions, docMap, storeOffsets);
      return new SortingDocsAndPositionsEnum(docMap.size(), wrapReuse, inDocsAndPositions, docMap, storeOffsets);
     }
 
   }
@@ -295,33 +295,31 @@ public class SortingAtomicReader extends FilterAtomicReader {
 
   static class SortingDocsEnum extends FilterDocsEnum {
     
    private static final class DocFreqSorterTemplate extends SorterTemplate {
    private static final class DocFreqSorter extends TimSorter {
       
      private final int[] docs;
      private final int[] freqs;
      private int[] docs;
      private int[] freqs;
      private final int[] tmpDocs;
      private int[] tmpFreqs;
       
      private int pivot;
      
      public DocFreqSorterTemplate(int[] docs, int[] freqs) {
      public DocFreqSorter(int maxDoc) {
        super(maxDoc / 64);
        this.tmpDocs = new int[maxDoc / 64];
      }

      public void reset(int[] docs, int[] freqs) {
         this.docs = docs;
         this.freqs = freqs;
        if (freqs != null && tmpFreqs == null) {
          tmpFreqs = new int[tmpDocs.length];
        }
       }
      

       @Override
       protected int compare(int i, int j) {
         return docs[i] - docs[j];
       }
       
      @Override
      protected int comparePivot(int j) {
        return pivot - docs[j];
      }
      
      @Override
      protected void setPivot(int i) {
        pivot = docs[i];
      }
      
       @Override
       protected void swap(int i, int j) {
         int tmpDoc = docs[i];
@@ -334,22 +332,60 @@ public class SortingAtomicReader extends FilterAtomicReader {
           freqs[j] = tmpFreq;
         }
       }

      @Override
      protected void copy(int src, int dest) {
        docs[dest] = docs[src];
        if (freqs != null) {
          freqs[dest] = freqs[src];
        }
      }

      @Override
      protected void save(int i, int len) {
        System.arraycopy(docs, i, tmpDocs, 0, len);
        if (freqs != null) {
          System.arraycopy(freqs, i, tmpFreqs, 0, len);
        }
      }

      @Override
      protected void restore(int i, int j) {
        docs[j] = tmpDocs[i];
        if (freqs != null) {
          freqs[j] = tmpFreqs[i];
        }
      }

      @Override
      protected int compareSaved(int i, int j) {
        return tmpDocs[i] - docs[j];
      }
     }
    

    private final int maxDoc;
    private final DocFreqSorter sorter;
     private int[] docs;
     private int[] freqs;
     private int docIt = -1;
     private final int upto;
     private final boolean withFreqs;
 
    SortingDocsEnum(SortingDocsEnum reuse, final DocsEnum in, boolean withFreqs, final Sorter.DocMap docMap) throws IOException {
    SortingDocsEnum(int maxDoc, SortingDocsEnum reuse, final DocsEnum in, boolean withFreqs, final Sorter.DocMap docMap) throws IOException {
       super(in);
      this.maxDoc = maxDoc;
       this.withFreqs = withFreqs;
       if (reuse != null) {
        if (reuse.maxDoc == maxDoc) {
          sorter = reuse.sorter;
        } else {
          sorter = new DocFreqSorter(maxDoc);
        }
         docs = reuse.docs;
         freqs = reuse.freqs; // maybe null
       } else {
         docs = new int[64];
        sorter = new DocFreqSorter(maxDoc);
       }
       docIt = -1;
       int i = 0;
@@ -378,7 +414,8 @@ public class SortingAtomicReader extends FilterAtomicReader {
       }
       // TimSort can save much time compared to other sorts in case of
       // reverse sorting, or when sorting a concatenation of sorted readers
      new DocFreqSorterTemplate(docs, freqs).timSort(0, i - 1);
      sorter.reset(docs, freqs);
      sorter.sort(0, i);
       upto = i;
     }
 
@@ -422,37 +459,33 @@ public class SortingAtomicReader extends FilterAtomicReader {
   static class SortingDocsAndPositionsEnum extends FilterDocsAndPositionsEnum {
     
     /**
     * A {@link SorterTemplate} which sorts two parallel arrays of doc IDs and
     * A {@link Sorter} which sorts two parallel arrays of doc IDs and
      * offsets in one go. Everytime a doc ID is 'swapped', its correponding offset
      * is swapped too.
      */
    private static final class DocOffsetSorterTemplate extends SorterTemplate {
    private static final class DocOffsetSorter extends TimSorter {
       
      private final int[] docs;
      private final long[] offsets;
      private int[] docs;
      private long[] offsets;
      private final int[] tmpDocs;
      private final long[] tmpOffsets;
       
      private int pivot;
      
      public DocOffsetSorterTemplate(int[] docs, long[] offsets) {
      public DocOffsetSorter(int maxDoc) {
        super(maxDoc / 64);
        this.tmpDocs = new int[maxDoc / 64];
        this.tmpOffsets = new long[maxDoc / 64];
      }

      public void reset(int[] docs, long[] offsets) {
         this.docs = docs;
         this.offsets = offsets;
       }
      

       @Override
       protected int compare(int i, int j) {
         return docs[i] - docs[j];
       }
       
      @Override
      protected int comparePivot(int j) {
        return pivot - docs[j];
      }
      
      @Override
      protected void setPivot(int i) {
        pivot = docs[i];
      }
      
       @Override
       protected void swap(int i, int j) {
         int tmpDoc = docs[i];
@@ -463,8 +496,33 @@ public class SortingAtomicReader extends FilterAtomicReader {
         offsets[i] = offsets[j];
         offsets[j] = tmpOffset;
       }

      @Override
      protected void copy(int src, int dest) {
        docs[dest] = docs[src];
        offsets[dest] = offsets[src];
      }

      @Override
      protected void save(int i, int len) {
        System.arraycopy(docs, i, tmpDocs, 0, len);
        System.arraycopy(offsets, i, tmpOffsets, 0, len);
      }

      @Override
      protected void restore(int i, int j) {
        docs[j] = tmpDocs[i];
        offsets[j] = tmpOffsets[i];
      }

      @Override
      protected int compareSaved(int i, int j) {
        return tmpDocs[i] - docs[j];
      }
     }
     
    private final int maxDoc;
    private final DocOffsetSorter sorter;
     private int[] docs;
     private long[] offsets;
     private final int upto;
@@ -481,19 +539,26 @@ public class SortingAtomicReader extends FilterAtomicReader {
 
     private final RAMFile file;
 
    SortingDocsAndPositionsEnum(SortingDocsAndPositionsEnum reuse, final DocsAndPositionsEnum in, Sorter.DocMap docMap, boolean storeOffsets) throws IOException {
    SortingDocsAndPositionsEnum(int maxDoc, SortingDocsAndPositionsEnum reuse, final DocsAndPositionsEnum in, Sorter.DocMap docMap, boolean storeOffsets) throws IOException {
       super(in);
      this.maxDoc = maxDoc;
       this.storeOffsets = storeOffsets;
       if (reuse != null) {
         docs = reuse.docs;
         offsets = reuse.offsets;
         payload = reuse.payload;
         file = reuse.file;
        if (reuse.maxDoc == maxDoc) {
          sorter = reuse.sorter;
        } else {
          sorter = new DocOffsetSorter(maxDoc);
        }
       } else {
         docs = new int[32];
         offsets = new long[32];
         payload = new BytesRef(32);
         file = new RAMFile();
        sorter = new DocOffsetSorter(maxDoc);
       }
       final IndexOutput out = new RAMOutputStream(file);
       int doc;
@@ -510,7 +575,8 @@ public class SortingAtomicReader extends FilterAtomicReader {
         i++;
       }
       upto = i;
      new DocOffsetSorterTemplate(docs, offsets).timSort(0, upto - 1);
      sorter.reset(docs, offsets);
      sorter.sort(0, upto);
       out.close();
       this.postingInput = new RAMInputStream("", file);
     }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/spell/DirectSpellChecker.java b/lucene/suggest/src/java/org/apache/lucene/search/spell/DirectSpellChecker.java
index b88a8c0ba6f..4df757b05b8 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/spell/DirectSpellChecker.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/spell/DirectSpellChecker.java
@@ -376,7 +376,7 @@ public class DirectSpellChecker {
       suggestions[index--] = suggestion;
     }
     
    ArrayUtil.mergeSort(suggestions, Collections.reverseOrder(comparator));
    ArrayUtil.timSort(suggestions, Collections.reverseOrder(comparator));
     if (numSug < suggestions.length) {
       SuggestWord trimmed[] = new SuggestWord[numSug];
       System.arraycopy(suggestions, 0, trimmed, 0, numSug);
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
index 98f5291e9cc..9fa96bac55a 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/BytesRefArray.java
@@ -21,12 +21,12 @@ import java.util.Arrays;
 import java.util.Comparator;
 
 import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefIterator;
 import org.apache.lucene.util.Counter;
import org.apache.lucene.util.IntroSorter;
 import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.SorterTemplate;
 
 /**
  * A simple append only random-access {@link BytesRef} array that stores full
@@ -120,7 +120,7 @@ public final class BytesRefArray {
     for (int i = 0; i < orderedEntries.length; i++) {
       orderedEntries[i] = i;
     }
    new SorterTemplate() {
    new IntroSorter() {
       @Override
       protected void swap(int i, int j) {
         final int o = orderedEntries[i];
@@ -148,7 +148,7 @@ public final class BytesRefArray {
       
       private final BytesRef pivot = new BytesRef(), scratch1 = new BytesRef(),
           scratch2 = new BytesRef();
    }.quickSort(0, size() - 1);
    }.sort(0, size());
     return orderedEntries;
   }
   
diff --git a/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
index 35f8678929b..7cf5f830fa1 100644
-- a/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/AnalysisRequestHandlerBase.java
@@ -211,7 +211,7 @@ public abstract class AnalysisRequestHandlerBase extends RequestHandlerBase {
     final AttributeSource[] tokens = tokenList.toArray(new AttributeSource[tokenList.size()]);
     
     // sort the tokens by absoulte position
    ArrayUtil.mergeSort(tokens, new Comparator<AttributeSource>() {
    ArrayUtil.timSort(tokens, new Comparator<AttributeSource>() {
       @Override
       public int compare(AttributeSource a, AttributeSource b) {
         return arrayCompare(
- 
2.19.1.windows.1

