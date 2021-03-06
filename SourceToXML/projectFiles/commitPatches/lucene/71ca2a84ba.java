From 71ca2a84bad2495eff3b0b15dc445f3f013ea4af Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@gmail.com>
Date: Thu, 19 Jan 2017 18:12:04 +0100
Subject: [PATCH] LUCENE-7643: Move IndexOrDocValuesQuery to core.

--
 lucene/CHANGES.txt                            |   7 +
 .../document/NumericDocValuesField.java       |  48 +++
 .../lucene/document/SortedDocValuesField.java |  42 +++
 .../document/SortedNumericDocValuesField.java |  54 +++
 .../SortedNumericDocValuesRangeQuery.java     | 144 ++++++++
 .../document/SortedSetDocValuesField.java     |  43 +++
 .../SortedSetDocValuesRangeQuery.java         | 187 +++++++++++
 .../lucene/search/IndexOrDocValuesQuery.java  |  66 +++-
 .../apache/lucene/search/PointRangeQuery.java |   2 +-
 .../lucene/search/TestDocValuesQueries.java   | 238 ++++++++++++++
 .../search/TestIndexOrDocValuesQuery.java     |   4 +-
 .../lucene/search/DocValuesRangeQuery.java    | 276 ----------------
 .../search/TestDocValuesRangeQuery.java       | 307 ------------------
 .../apache/solr/schema/ICUCollationField.java |  10 +-
 .../apache/solr/schema/CollationField.java    |   3 +-
 .../org/apache/solr/schema/EnumField.java     |  20 +-
 .../org/apache/solr/schema/FieldType.java     |  16 +-
 .../org/apache/solr/schema/TrieField.java     |  45 ++-
 18 files changed, 887 insertions(+), 625 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
 rename lucene/{sandbox => core}/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java (56%)
 create mode 100644 lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
 rename lucene/{sandbox => core}/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java (96%)
 delete mode 100644 lucene/sandbox/src/java/org/apache/lucene/search/DocValuesRangeQuery.java
 delete mode 100644 lucene/sandbox/src/test/org/apache/lucene/search/TestDocValuesRangeQuery.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 9d1cbb7b83d..147b0e01d7d 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -74,6 +74,9 @@ API Changes
 * LUCENE-7644: FieldComparatorSource.newComparator() and
   SortField.getComparator() no longer throw IOException (Alan Woodward)
 
* LUCENE-7643: Replaced doc-values queries in lucene/sandbox with factory
  methods on the *DocValuesField classes. (Adrien Grand)

 New Features
 
 * LUCENE-7623: Add FunctionScoreQuery and FunctionMatchQuery (Alan Woodward,
@@ -96,6 +99,10 @@ Improvements
   should be run, eg. using points or doc values depending on costs of other
   parts of the query. (Adrien Grand)
 
* LUCENE-7643: IndexOrDocValuesQuery allows to execute range queries using
  either points or doc values depending on which one is more efficient.
  (Adrien Grand)

 Optimizations
 
 * LUCENE-7641: Optimized point range queries to compute documents that do not
diff --git a/lucene/core/src/java/org/apache/lucene/document/NumericDocValuesField.java b/lucene/core/src/java/org/apache/lucene/document/NumericDocValuesField.java
index 5b6dcc8dce2..6d844925d08 100644
-- a/lucene/core/src/java/org/apache/lucene/document/NumericDocValuesField.java
++ b/lucene/core/src/java/org/apache/lucene/document/NumericDocValuesField.java
@@ -17,7 +17,15 @@
 package org.apache.lucene.document;
 
 
import java.io.IOException;

import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
 
 /**
  * <p>
@@ -54,4 +62,44 @@ public class NumericDocValuesField extends Field {
     super(name, TYPE);
     fieldsData = Long.valueOf(value);
   }

  /**
   * Create a range query that matches all documents whose value is between
   * {@code lowerValue} and {@code upperValue} included.
   * <p>
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting {@code lowerValue = Long.MIN_VALUE} or {@code upperValue = Long.MAX_VALUE}. 
   * <p>
   * Ranges are inclusive. For exclusive ranges, pass {@code Math.addExact(lowerValue, 1)}
   * or {@code Math.addExact(upperValue, -1)}.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link LongPoint#newRangeQuery}.
   */
  public static Query newRangeQuery(String field, long lowerValue, long upperValue) {
    return new SortedNumericDocValuesRangeQuery(field, lowerValue, upperValue) {
      @Override
      SortedNumericDocValues getValues(LeafReader reader, String field) throws IOException {
        NumericDocValues values = reader.getNumericDocValues(field);
        if (values == null) {
          return null;
        }
        return DocValues.singleton(values);
      }
    };
  }

  /** 
   * Create a query for matching an exact long value.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link LongPoint#newExactQuery}.
   */
  public static Query newExactQuery(String field, long value) {
    return newRangeQuery(field, value, value);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedDocValuesField.java b/lucene/core/src/java/org/apache/lucene/document/SortedDocValuesField.java
index bbfb46719f0..feb772501b3 100644
-- a/lucene/core/src/java/org/apache/lucene/document/SortedDocValuesField.java
++ b/lucene/core/src/java/org/apache/lucene/document/SortedDocValuesField.java
@@ -17,7 +17,14 @@
 package org.apache.lucene.document;
 
 
import java.io.IOException;

import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
 import org.apache.lucene.util.BytesRef;
 
 /**
@@ -59,4 +66,39 @@ public class SortedDocValuesField extends Field {
     super(name, TYPE);
     fieldsData = bytes;
   }

  /**
   * Create a range query that matches all documents whose value is between
   * {@code lowerValue} and {@code upperValue} included.
   * <p>
   * You can have half-open ranges by setting {@code lowerValue = null}
   * or {@code upperValue = null}.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link BinaryPoint#newRangeQuery}.
   */
  public static Query newRangeQuery(String field,
      BytesRef lowerValue, BytesRef upperValue,
      boolean lowerInclusive, boolean upperInclusive) {
    return new SortedSetDocValuesRangeQuery(field, lowerValue, upperValue, lowerInclusive, upperInclusive) {
      @Override
      SortedSetDocValues getValues(LeafReader reader, String field) throws IOException {
        return DocValues.singleton(DocValues.getSorted(reader, field));
      }
    };
  }

  /** 
   * Create a query for matching an exact {@link BytesRef} value.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link BinaryPoint#newExactQuery}.
   */
  public static Query newExactQuery(String field, BytesRef value) {
    return newRangeQuery(field, value, value, true, true);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesField.java b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesField.java
index cbba218f7de..6f9a2717267 100644
-- a/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesField.java
++ b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesField.java
@@ -17,7 +17,15 @@
 package org.apache.lucene.document;
 
 
import java.io.IOException;

import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
 
 /**
  * <p>
@@ -63,4 +71,50 @@ public class SortedNumericDocValuesField extends Field {
     super(name, TYPE);
     fieldsData = Long.valueOf(value);
   }

  /**
   * Create a range query that matches all documents whose value is between
   * {@code lowerValue} and {@code upperValue} included.
   * <p>
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting {@code lowerValue = Long.MIN_VALUE} or {@code upperValue = Long.MAX_VALUE}. 
   * <p>
   * Ranges are inclusive. For exclusive ranges, pass {@code Math.addExact(lowerValue, 1)}
   * or {@code Math.addExact(upperValue, -1)}.
   * <p>This query also works with fields that have indexed
   * {@link NumericDocValuesField}s.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link LongPoint#newRangeQuery}.
   */
  public static Query newRangeQuery(String field, long lowerValue, long upperValue) {
    return new SortedNumericDocValuesRangeQuery(field, lowerValue, upperValue) {
      @Override
      SortedNumericDocValues getValues(LeafReader reader, String field) throws IOException {
        FieldInfo info = reader.getFieldInfos().fieldInfo(field);
        if (info == null) {
          // Queries have some optimizations when one sub scorer returns null rather
          // than a scorer that does not match any documents
          return null;
        }
        return DocValues.getSortedNumeric(reader, field);
      }
    };
  }

  /** 
   * Create a query for matching an exact long value.
   * <p>This query also works with fields that have indexed
   * {@link NumericDocValuesField}s.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link LongPoint#newExactQuery}.
   */
  public static Query newExactQuery(String field, long value) {
    return newRangeQuery(field, value, value);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
new file mode 100644
index 00000000000..18805b287c0
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java
@@ -0,0 +1,144 @@
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
package org.apache.lucene.document;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.FieldValueQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;

abstract class SortedNumericDocValuesRangeQuery extends Query {

  private final String field;
  private final long lowerValue;
  private final long upperValue;

  SortedNumericDocValuesRangeQuery(String field, long lowerValue, long upperValue) {
    this.field = Objects.requireNonNull(field);
    this.lowerValue = lowerValue;
    this.upperValue = upperValue;
  }

  @Override
  public boolean equals(Object obj) {
    if (sameClassAs(obj) == false) {
      return false;
    }
    SortedNumericDocValuesRangeQuery that = (SortedNumericDocValuesRangeQuery) obj;
    return Objects.equals(field, that.field)
        && lowerValue == that.lowerValue
        && upperValue == that.upperValue;
  }

  @Override
  public int hashCode() {
    int h = classHash();
    h = 31 * h + field.hashCode();
    h = 31 * h + Long.hashCode(lowerValue);
    h = 31 * h + Long.hashCode(upperValue);
    return h;
  }

  @Override
  public String toString(String field) {
    StringBuilder b = new StringBuilder();
    if (this.field.equals(field) == false) {
      b.append(this.field).append(":");
    }
    return b
        .append("[")
        .append(lowerValue)
        .append(" TO ")
        .append(upperValue)
        .append("]")
        .toString();
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (lowerValue == Long.MIN_VALUE && upperValue == Long.MAX_VALUE) {
      return new FieldValueQuery(field);
    }
    return super.rewrite(reader);
  }

  abstract SortedNumericDocValues getValues(LeafReader reader, String field) throws IOException;

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    return new ConstantScoreWeight(this, boost) {
      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        SortedNumericDocValues values = getValues(context.reader(), field);
        if (values == null) {
          return null;
        }
        final NumericDocValues singleton = DocValues.unwrapSingleton(values);
        final TwoPhaseIterator iterator;
        if (singleton != null) {
          iterator = new TwoPhaseIterator(singleton) {
            @Override
            public boolean matches() throws IOException {
              final long value = singleton.longValue();
              return value >= lowerValue && value <= upperValue;
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }
          };
        } else {
          iterator = new TwoPhaseIterator(values) {
            @Override
            public boolean matches() throws IOException {
              for (int i = 0, count = values.docValueCount(); i < count; ++i) {
                final long value = values.nextValue();
                if (value < lowerValue) {
                  continue;
                }
                // Values are sorted, so the first value that is >= lowerValue is our best candidate
                return value <= upperValue;
              }
              return false; // all values were < lowerValue
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }
          };
        }
        return new ConstantScoreScorer(this, score(), iterator);
      }
    };
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesField.java b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesField.java
index 7a273acd779..26b1907617d 100644
-- a/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesField.java
++ b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesField.java
@@ -17,7 +17,14 @@
 package org.apache.lucene.document;
 
 
import java.io.IOException;

import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
 import org.apache.lucene.util.BytesRef;
 
 /**
@@ -60,4 +67,40 @@ public class SortedSetDocValuesField extends Field {
     super(name, TYPE);
     fieldsData = bytes;
   }

  /**
   * Create a range query that matches all documents whose value is between
   * {@code lowerValue} and {@code upperValue}.
   * <p>This query also works with fields that have indexed
   * {@link SortedDocValuesField}s.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link BinaryPoint#newRangeQuery}.
   */
  public static Query newRangeQuery(String field,
      BytesRef lowerValue, BytesRef upperValue,
      boolean lowerInclusive, boolean upperInclusive) {
    return new SortedSetDocValuesRangeQuery(field, lowerValue, upperValue, lowerInclusive, upperInclusive) {
      @Override
      SortedSetDocValues getValues(LeafReader reader, String field) throws IOException {
        return DocValues.getSortedSet(reader, field);
      }
    };
  }

  /** 
   * Create a query for matching an exact {@link BytesRef} value.
   * <p>This query also works with fields that have indexed
   * {@link SortedDocValuesField}s.
   * <p><b>NOTE</b>: Such queries cannot efficiently advance to the next match,
   * which makes them slow if they are not ANDed with a selective query. As a
   * consequence, they are best used wrapped in an {@link IndexOrDocValuesQuery},
   * alongside a range query that executes on points, such as
   * {@link BinaryPoint#newExactQuery}.
   */
  public static Query newExactQuery(String field, BytesRef value) {
    return newRangeQuery(field, value, value, true, true);
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
new file mode 100644
index 00000000000..30af45f6a64
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java
@@ -0,0 +1,187 @@
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
package org.apache.lucene.document;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.FieldValueQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;

abstract class SortedSetDocValuesRangeQuery extends Query {

  private final String field;
  private final BytesRef lowerValue;
  private final BytesRef upperValue;
  private final boolean lowerInclusive;
  private final boolean upperInclusive;

  SortedSetDocValuesRangeQuery(String field,
      BytesRef lowerValue, BytesRef upperValue,
      boolean lowerInclusive, boolean upperInclusive) {
    this.field = Objects.requireNonNull(field);
    this.lowerValue = lowerValue;
    this.upperValue = upperValue;
    this.lowerInclusive = lowerInclusive && lowerValue != null;
    this.upperInclusive = upperInclusive && upperValue != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (sameClassAs(obj) == false) {
      return false;
    }
    SortedSetDocValuesRangeQuery that = (SortedSetDocValuesRangeQuery) obj;
    return Objects.equals(field, that.field)
        && Objects.equals(lowerValue, that.lowerValue)
        && Objects.equals(upperValue, that.upperValue)
        && lowerInclusive == that.lowerInclusive
        && upperInclusive == that.upperInclusive;
  }

  @Override
  public int hashCode() {
    int h = classHash();
    h = 31 * h + field.hashCode();
    h = 31 * h + Objects.hashCode(lowerValue);
    h = 31 * h + Objects.hashCode(upperValue);
    h = 31 * h + Boolean.hashCode(lowerInclusive);
    h = 31 * h + Boolean.hashCode(upperInclusive);
    return h;
  }

  @Override
  public String toString(String field) {
    StringBuilder b = new StringBuilder();
    if (this.field.equals(field) == false) {
      b.append(this.field).append(":");
    }
    return b
        .append(lowerInclusive ? "[" : "{")
        .append(lowerValue == null ? "*" : lowerValue)
        .append(" TO ")
        .append(upperValue == null ? "*" : upperValue)
        .append(upperInclusive ? "]" : "}")
        .toString();
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (lowerValue == null && upperValue == null) {
      return new FieldValueQuery(field);
    }
    return super.rewrite(reader);
  }

  abstract SortedSetDocValues getValues(LeafReader reader, String field) throws IOException;

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    return new ConstantScoreWeight(this, boost) {
      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        SortedSetDocValues values = getValues(context.reader(), field);
        if (values == null) {
          return null;
        }

        final long minOrd;
        if (lowerValue == null) {
          minOrd = 0;
        } else {
          final long ord = values.lookupTerm(lowerValue);
          if (ord < 0) {
            minOrd = -1 - ord;
          } else if (lowerInclusive) {
            minOrd = ord;
          } else {
            minOrd = ord + 1;
          }
        }

        final long maxOrd;
        if (upperValue == null) {
          maxOrd = values.getValueCount() - 1;
        } else {
          final long ord = values.lookupTerm(upperValue);
          if (ord < 0) {
            maxOrd = -2 - ord;
          } else if (upperInclusive) {
            maxOrd = ord;
          } else {
            maxOrd = ord - 1;
          }
        }

        if (minOrd > maxOrd) {
          return null;
        }

        final SortedDocValues singleton = DocValues.unwrapSingleton(values);
        final TwoPhaseIterator iterator;
        if (singleton != null) {
          iterator = new TwoPhaseIterator(singleton) {
            @Override
            public boolean matches() throws IOException {
              final long ord = singleton.ordValue();
              return ord >= minOrd && ord <= maxOrd;
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }
          };
        } else {
          iterator = new TwoPhaseIterator(values) {
            @Override
            public boolean matches() throws IOException {
              for (long ord = values.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = values.nextOrd()) {
                if (ord < minOrd) {
                  continue;
                }
                // Values are sorted, so the first ord that is >= minOrd is our best candidate
                return ord <= maxOrd;
              }
              return false; // all ords were < minOrd
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }
          };
        }
        return new ConstantScoreScorer(this, score(), iterator);
      }
    };
  }

}
diff --git a/lucene/sandbox/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java b/lucene/core/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java
similarity index 56%
rename from lucene/sandbox/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java
rename to lucene/core/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java
index 0f9e8e3e027..35067d2105d 100644
-- a/lucene/sandbox/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/IndexOrDocValuesQuery.java
@@ -17,29 +17,66 @@
 package org.apache.lucene.search;
 
 import java.io.IOException;
import java.util.Set;
 
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
 
 /**
 * A query that uses either an index (points or terms) or doc values in order
 * to run a range query, depending which one is more efficient.
 * A query that uses either an index structure (points or terms) or doc values
 * in order to run a query, depending which one is more efficient. This is
 * typically useful for range queries, whose {@link Weight#scorer} is costly
 * to create since it usually needs to sort large lists of doc ids. For
 * instance, for a field that both indexed {@link LongPoint}s and
 * {@link SortedNumericDocValuesField}s with the same values, an efficient
 * range query could be created by doing:
 * <pre class="prettyprint">
 *   String field;
 *   long minValue, maxValue;
 *   Query pointQuery = LongPoint.newRangeQuery(field, minValue, maxValue);
 *   Query dvQuery = SortedNumericDocValuesField.newRangeQuery(field, minValue, maxValue);
 *   Query query = new IndexOrDocValuesQuery(pointQuery, dvQuery);
 * </pre>
 * The above query will be efficient as it will use points in the case that they
 * perform better, ie. when we need a good lead iterator that will be almost
 * entirely consumed; and doc values otherwise, ie. in the case that another
 * part of the query is already leading iteration but we still need the ability
 * to verify that some documents match.
 * <p><b>NOTE</b>This query currently only works well with point range/exact
 * queries and their equivalent doc values queries.
 * @lucene.experimental
  */
 public final class IndexOrDocValuesQuery extends Query {
 
   private final Query indexQuery, dvQuery;
 
   /**
   * Constructor that takes both a query that executes on an index structure
   * like the inverted index or the points tree, and another query that
   * executes on doc values. Both queries must match the same documents and
   * attribute constant scores.
   * Create an {@link IndexOrDocValuesQuery}. Both provided queries must match
   * the same documents and give the same scores.
   * @param indexQuery a query that has a good iterator but whose scorer may be costly to create
   * @param dvQuery a query whose scorer is cheap to create that can quickly check whether a given document matches
    */
   public IndexOrDocValuesQuery(Query indexQuery, Query dvQuery) {
     this.indexQuery = indexQuery;
     this.dvQuery = dvQuery;
   }
 
  /** Return the wrapped query that may be costly to initialize but has a good
   *  iterator. */
  public Query getIndexQuery() {
    return indexQuery;
  }

  /** Return the wrapped query that may be slow at identifying all matching
   *  documents, but which is cheap to initialize and can efficiently
   *  verify that some documents match. */
  public Query getRandomAccessQuery() {
    return dvQuery;
  }

   @Override
   public String toString(String field) {
     return indexQuery.toString(field);
@@ -76,16 +113,29 @@ public final class IndexOrDocValuesQuery extends Query {
   public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
     final Weight indexWeight = indexQuery.createWeight(searcher, needsScores, boost);
     final Weight dvWeight = dvQuery.createWeight(searcher, needsScores, boost);
    return new ConstantScoreWeight(this, boost) {
    return new Weight(this) {
      @Override
      public void extractTerms(Set<Term> terms) {
        indexWeight.extractTerms(terms);
      }

      @Override
      public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        // We need to check a single doc, so the dv query should perform better
        return dvWeight.explain(context, doc);
      }

       @Override
       public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
        // Bulk scorers need to consume the entire set of docs, so using an
        // index structure should perform better
         return indexWeight.bulkScorer(context);
       }
 
       @Override
       public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
         final ScorerSupplier indexScorerSupplier = indexWeight.scorerSupplier(context);
        final ScorerSupplier dvScorerSupplier = dvWeight.scorerSupplier(context); 
        final ScorerSupplier dvScorerSupplier = dvWeight.scorerSupplier(context);
         if (indexScorerSupplier == null || dvScorerSupplier == null) {
           return null;
         }
diff --git a/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java b/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
index 7c997caf08a..f1b85519d0b 100644
-- a/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/PointRangeQuery.java
@@ -281,7 +281,7 @@ public abstract class PointRangeQuery extends Query {
 
             @Override
             public Scorer get(boolean randomAccess) throws IOException {
              if (values.getDocCount() == reader.maxDoc()
              if (false && values.getDocCount() == reader.maxDoc()
                   && values.getDocCount() == values.size()
                   && cost() > reader.maxDoc() / 2) {
                 // If all docs have exactly one value and the cost is greater
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java b/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
new file mode 100644
index 00000000000..501538f426f
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/search/TestDocValuesQueries.java
@@ -0,0 +1,238 @@
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
package org.apache.lucene.search;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

public class TestDocValuesQueries extends LuceneTestCase {

  public void testDuelPointRangeSortedNumericRangeQuery() throws IOException {
    doTestDuelPointRangeNumericRangeQuery(true, 1);
  }

  public void testDuelPointRangeMultivaluedSortedNumericRangeQuery() throws IOException {
    doTestDuelPointRangeNumericRangeQuery(true, 3);
  }

  public void testDuelPointRangeNumericRangeQuery() throws IOException {
    doTestDuelPointRangeNumericRangeQuery(false, 1);
  }

  private void doTestDuelPointRangeNumericRangeQuery(boolean sortedNumeric, int maxValuesPerDoc) throws IOException {
    final int iters = atLeast(10);
    for (int iter = 0; iter < iters; ++iter) {
      Directory dir = newDirectory();
      RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
      final int numDocs = atLeast(100);
      for (int i = 0; i < numDocs; ++i) {
        Document doc = new Document();
        final int numValues = TestUtil.nextInt(random(), 0, maxValuesPerDoc);
        for (int j = 0; j < numValues; ++j) {
          final long value = TestUtil.nextLong(random(), -100, 10000);
          if (sortedNumeric) {
            doc.add(new SortedNumericDocValuesField("dv", value));
          } else {
            doc.add(new NumericDocValuesField("dv", value));
          }
          doc.add(new LongPoint("idx", value));
        }
        iw.addDocument(doc);
      }
      if (random().nextBoolean()) {
        iw.deleteDocuments(LongPoint.newRangeQuery("idx", 0L, 10L));
      }
      final IndexReader reader = iw.getReader();
      final IndexSearcher searcher = newSearcher(reader, false);
      iw.close();

      for (int i = 0; i < 100; ++i) {
        final long min = random().nextBoolean() ? Long.MIN_VALUE : TestUtil.nextLong(random(), -100, 10000);
        final long max = random().nextBoolean() ? Long.MAX_VALUE : TestUtil.nextLong(random(), -100, 10000);
        final Query q1 = LongPoint.newRangeQuery("idx", min, max);
        final Query q2;
        if (sortedNumeric) {
          q2 = SortedNumericDocValuesField.newRangeQuery("dv", min, max);
        } else {
          q2 = NumericDocValuesField.newRangeQuery("dv", min, max);
        }
        assertSameMatches(searcher, q1, q2, false);
      }

      reader.close();
      dir.close();
    }
  }

  private void doTestDuelPointRangeSortedRangeQuery(boolean sortedSet, int maxValuesPerDoc) throws IOException {
    final int iters = atLeast(10);
    for (int iter = 0; iter < iters; ++iter) {
      Directory dir = newDirectory();
      RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
      final int numDocs = atLeast(100);
      for (int i = 0; i < numDocs; ++i) {
        Document doc = new Document();
        final int numValues = TestUtil.nextInt(random(), 0, maxValuesPerDoc);
        for (int j = 0; j < numValues; ++j) {
          final long value = TestUtil.nextLong(random(), -100, 10000);
          byte[] encoded = new byte[Long.BYTES];
          LongPoint.encodeDimension(value, encoded, 0);
          if (sortedSet) {
            doc.add(new SortedSetDocValuesField("dv", new BytesRef(encoded)));
          } else {
            doc.add(new SortedDocValuesField("dv", new BytesRef(encoded)));
          }
          doc.add(new LongPoint("idx", value));
        }
        iw.addDocument(doc);
      }
      if (random().nextBoolean()) {
        iw.deleteDocuments(LongPoint.newRangeQuery("idx", 0L, 10L));
      }
      final IndexReader reader = iw.getReader();
      final IndexSearcher searcher = newSearcher(reader, false);
      iw.close();

      for (int i = 0; i < 100; ++i) {
        long min = random().nextBoolean() ? Long.MIN_VALUE : TestUtil.nextLong(random(), -100, 10000);
        long max = random().nextBoolean() ? Long.MAX_VALUE : TestUtil.nextLong(random(), -100, 10000);
        byte[] encodedMin = new byte[Long.BYTES];
        byte[] encodedMax = new byte[Long.BYTES];
        LongPoint.encodeDimension(min, encodedMin, 0);
        LongPoint.encodeDimension(max, encodedMax, 0);
        boolean includeMin = true;
        boolean includeMax = true;
        if (random().nextBoolean()) {
          includeMin = false;
          min++;
        }
        if (random().nextBoolean()) {
          includeMax = false;
          max--;
        }
        final Query q1 = LongPoint.newRangeQuery("idx", min, max);
        final Query q2;
        if (sortedSet) {
          q2 = SortedSetDocValuesField.newRangeQuery("dv",
              min == Long.MIN_VALUE && random().nextBoolean() ? null : new BytesRef(encodedMin),
              max == Long.MAX_VALUE && random().nextBoolean() ? null : new BytesRef(encodedMax),
              includeMin, includeMax);
        } else {
          q2 = SortedDocValuesField.newRangeQuery("dv",
              min == Long.MIN_VALUE && random().nextBoolean() ? null : new BytesRef(encodedMin),
              max == Long.MAX_VALUE && random().nextBoolean() ? null : new BytesRef(encodedMax),
              includeMin, includeMax);
        }
        assertSameMatches(searcher, q1, q2, false);
      }

      reader.close();
      dir.close();
    }
  }

  public void testDuelPointRangeSortedSetRangeQuery() throws IOException {
    doTestDuelPointRangeSortedRangeQuery(true, 1);
  }

  public void testDuelPointRangeMultivaluedSortedSetRangeQuery() throws IOException {
    doTestDuelPointRangeSortedRangeQuery(true, 3);
  }

  public void testDuelPointRangeSortedRangeQuery() throws IOException {
    doTestDuelPointRangeSortedRangeQuery(false, 1);
  }

  private void assertSameMatches(IndexSearcher searcher, Query q1, Query q2, boolean scores) throws IOException {
    final int maxDoc = searcher.getIndexReader().maxDoc();
    final TopDocs td1 = searcher.search(q1, maxDoc, scores ? Sort.RELEVANCE : Sort.INDEXORDER);
    final TopDocs td2 = searcher.search(q2, maxDoc, scores ? Sort.RELEVANCE : Sort.INDEXORDER);
    assertEquals(td1.totalHits, td2.totalHits);
    for (int i = 0; i < td1.scoreDocs.length; ++i) {
      assertEquals(td1.scoreDocs[i].doc, td2.scoreDocs[i].doc);
      if (scores) {
        assertEquals(td1.scoreDocs[i].score, td2.scoreDocs[i].score, 10e-7);
      }
    }
  }

  public void testEquals() {
    Query q1 = SortedNumericDocValuesField.newRangeQuery("foo", 3, 5);
    QueryUtils.checkEqual(q1, SortedNumericDocValuesField.newRangeQuery("foo", 3, 5));
    QueryUtils.checkUnequal(q1, SortedNumericDocValuesField.newRangeQuery("foo", 3, 6));
    QueryUtils.checkUnequal(q1, SortedNumericDocValuesField.newRangeQuery("foo", 4, 5));
    QueryUtils.checkUnequal(q1, SortedNumericDocValuesField.newRangeQuery("bar", 3, 5));

    Query q2 = SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), true, true);
    QueryUtils.checkEqual(q2, SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), true, true));
    QueryUtils.checkUnequal(q2, SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("baz"), new BytesRef("baz"), true, true));
    QueryUtils.checkUnequal(q2, SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("bar"), true, true));
    QueryUtils.checkUnequal(q2, SortedSetDocValuesField.newRangeQuery("quux", new BytesRef("bar"), new BytesRef("baz"), true, true));
  }

  public void testToString() {
    Query q1 = SortedNumericDocValuesField.newRangeQuery("foo", 3, 5);
    assertEquals("foo:[3 TO 5]", q1.toString());
    assertEquals("[3 TO 5]", q1.toString("foo"));
    assertEquals("foo:[3 TO 5]", q1.toString("bar"));

    Query q2 = SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), true, true);
    assertEquals("foo:[[62 61 72] TO [62 61 7a]]", q2.toString());
    q2 = SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), false, true);
    assertEquals("foo:{[62 61 72] TO [62 61 7a]]", q2.toString());
    q2 = SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), false, false);
    assertEquals("foo:{[62 61 72] TO [62 61 7a]}", q2.toString());
    q2 = SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("bar"), null, true, true);
    assertEquals("foo:[[62 61 72] TO *}", q2.toString());
    q2 = SortedSetDocValuesField.newRangeQuery("foo", null, new BytesRef("baz"), true, true);
    assertEquals("foo:{* TO [62 61 7a]]", q2.toString());
    assertEquals("{* TO [62 61 7a]]", q2.toString("foo"));
    assertEquals("foo:{* TO [62 61 7a]]", q2.toString("bar"));
  }

  public void testMissingField() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    iw.addDocument(new Document());
    IndexReader reader = iw.getReader();
    iw.close();
    IndexSearcher searcher = newSearcher(reader);
    for (Query query : Arrays.asList(
        NumericDocValuesField.newRangeQuery("foo", 2, 4),
        SortedNumericDocValuesField.newRangeQuery("foo", 2, 4),
        SortedDocValuesField.newRangeQuery("foo", new BytesRef("abc"), new BytesRef("bcd"), random().nextBoolean(), random().nextBoolean()),
        SortedSetDocValuesField.newRangeQuery("foo", new BytesRef("abc"), new BytesRef("bcd"), random().nextBoolean(), random().nextBoolean()))) {
      Weight w = searcher.createNormalizedWeight(query, random().nextBoolean());
      assertNull(w.scorer(searcher.getIndexReader().leaves().get(0)));
    }
    reader.close();
    dir.close();
  }
}
diff --git a/lucene/sandbox/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java b/lucene/core/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java
similarity index 96%
rename from lucene/sandbox/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java
rename to lucene/core/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java
index de289e7d073..8b81822c455 100644
-- a/lucene/sandbox/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestIndexOrDocValuesQuery.java
@@ -64,7 +64,7 @@ public class TestIndexOrDocValuesQuery extends LuceneTestCase {
     // The term query is more selective, so the IndexOrDocValuesQuery should use doc values
     final Query q1 = new BooleanQuery.Builder()
         .add(new TermQuery(new Term("f1", "foo")), Occur.MUST)
        .add(new IndexOrDocValuesQuery(LongPoint.newExactQuery("f2", 2), new DocValuesNumbersQuery("f2", 2L)), Occur.MUST)
        .add(new IndexOrDocValuesQuery(LongPoint.newExactQuery("f2", 2), NumericDocValuesField.newRangeQuery("f2", 2L, 2L)), Occur.MUST)
         .build();
 
     final Weight w1 = searcher.createNormalizedWeight(q1, random().nextBoolean());
@@ -74,7 +74,7 @@ public class TestIndexOrDocValuesQuery extends LuceneTestCase {
     // The term query is less selective, so the IndexOrDocValuesQuery should use points
     final Query q2 = new BooleanQuery.Builder()
         .add(new TermQuery(new Term("f1", "bar")), Occur.MUST)
        .add(new IndexOrDocValuesQuery(LongPoint.newExactQuery("f2", 42), new DocValuesNumbersQuery("f2", 42L)), Occur.MUST)
        .add(new IndexOrDocValuesQuery(LongPoint.newExactQuery("f2", 42), NumericDocValuesField.newRangeQuery("f2", 42L, 42L)), Occur.MUST)
         .build();
 
     final Weight w2 = searcher.createNormalizedWeight(q2, random().nextBoolean());
diff --git a/lucene/sandbox/src/java/org/apache/lucene/search/DocValuesRangeQuery.java b/lucene/sandbox/src/java/org/apache/lucene/search/DocValuesRangeQuery.java
deleted file mode 100644
index 3d4feb94798..00000000000
-- a/lucene/sandbox/src/java/org/apache/lucene/search/DocValuesRangeQuery.java
++ /dev/null
@@ -1,276 +0,0 @@
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
package org.apache.lucene.search;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.Terms;
import org.apache.lucene.util.BytesRef;

/**
 * A range query that works on top of the doc values APIs. Such queries are
 * usually slow since they do not use an inverted index. However, in the
 * dense case where most documents match this query, it <b>might</b> be as
 * fast or faster than a regular {@link PointRangeQuery}.
 *
 * <b>NOTE:</b> This query is typically best used within a
 * {@link IndexOrDocValuesQuery} alongside a query that uses an indexed
 * structure such as {@link PointValues points} or {@link Terms terms},
 * which allows to run the query on doc values when that would be more
 * efficient, and using an index otherwise.
 *
 * @lucene.experimental
 */
public final class DocValuesRangeQuery extends Query {

  /** Create a new numeric range query on a numeric doc-values field. The field
   *  must has been indexed with either {@link DocValuesType#NUMERIC} or
   *  {@link DocValuesType#SORTED_NUMERIC} doc values. */
  public static Query newLongRange(String field, Long lowerVal, Long upperVal, boolean includeLower, boolean includeUpper) {
    return new DocValuesRangeQuery(field, lowerVal, upperVal, includeLower, includeUpper);
  }

  /** Create a new numeric range query on a numeric doc-values field. The field
   *  must has been indexed with {@link DocValuesType#SORTED} or
   *  {@link DocValuesType#SORTED_SET} doc values. */
  public static Query newBytesRefRange(String field, BytesRef lowerVal, BytesRef upperVal, boolean includeLower, boolean includeUpper) {
    return new DocValuesRangeQuery(field, deepCopyOf(lowerVal), deepCopyOf(upperVal), includeLower, includeUpper);
  }

  private static BytesRef deepCopyOf(BytesRef b) {
    if (b == null) {
      return null;
    } else {
      return BytesRef.deepCopyOf(b);
    }
  }

  private final String field;
  private final Object lowerVal, upperVal;
  private final boolean includeLower, includeUpper;

  private DocValuesRangeQuery(String field, Object lowerVal, Object upperVal, boolean includeLower, boolean includeUpper) {
    this.field = Objects.requireNonNull(field);
    this.lowerVal = lowerVal;
    this.upperVal = upperVal;
    this.includeLower = includeLower;
    this.includeUpper = includeUpper;
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           equalsTo(getClass().cast(other));
  }

  private boolean equalsTo(DocValuesRangeQuery other) {
    return field.equals(other.field) && 
           Objects.equals(lowerVal, other.lowerVal) && 
           Objects.equals(upperVal, other.upperVal) && 
           includeLower == other.includeLower && 
           includeUpper == other.includeUpper;
  }

  @Override
  public int hashCode() {
    return 31 * classHash() + Objects.hash(field, lowerVal, upperVal, includeLower, includeUpper);
  }

  public String getField() {
    return field;
  }

  public Object getLowerVal() {
    return lowerVal;
  }

  public Object getUpperVal() {
    return upperVal;
  }

  public boolean isIncludeLower() {
    return includeLower;
  }

  public boolean isIncludeUpper() {
    return includeUpper;
  }

  @Override
  public String toString(String field) {
    StringBuilder sb = new StringBuilder();
    if (this.field.equals(field) == false) {
      sb.append(this.field).append(':');
    }
    sb.append(includeLower ? '[' : '{');
    sb.append(lowerVal == null ? "*" : lowerVal.toString());
    sb.append(" TO ");
    sb.append(upperVal == null ? "*" : upperVal.toString());
    sb.append(includeUpper ? ']' : '}');
    return sb.toString();
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (lowerVal == null && upperVal == null) {
      return new FieldValueQuery(field);
    }
    return super.rewrite(reader);
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    if (lowerVal == null && upperVal == null) {
      throw new IllegalStateException("Both min and max values must not be null, call rewrite first");
    }

    return new ConstantScoreWeight(DocValuesRangeQuery.this, boost) {

      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        final TwoPhaseIterator iterator = createTwoPhaseIterator(context);
        if (iterator == null) {
          return null;
        }
        return new ConstantScoreScorer(this, score(), iterator);
      }

      private TwoPhaseIterator createTwoPhaseIterator(LeafReaderContext context) throws IOException {
        if (lowerVal instanceof Long || upperVal instanceof Long) {

          final SortedNumericDocValues values = DocValues.getSortedNumeric(context.reader(), field);

          final long min;
          if (lowerVal == null) {
            min = Long.MIN_VALUE;
          } else if (includeLower) {
            min = (long) lowerVal;
          } else {
            if ((long) lowerVal == Long.MAX_VALUE) {
              return null;
            }
            min = 1 + (long) lowerVal;
          }

          final long max;
          if (upperVal == null) {
            max = Long.MAX_VALUE;
          } else if (includeUpper) {
            max = (long) upperVal;
          } else {
            if ((long) upperVal == Long.MIN_VALUE) {
              return null;
            }
            max = -1 + (long) upperVal;
          }

          if (min > max) {
            return null;
          }

          return new TwoPhaseIterator(values) {

            @Override
            public boolean matches() throws IOException {
              final int count = values.docValueCount();
              assert count > 0;
              for (int i = 0; i < count; ++i) {
                final long value = values.nextValue();
                if (value >= min && value <= max) {
                  return true;
                }
              }
              return false;
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }

          };

        } else if (lowerVal instanceof BytesRef || upperVal instanceof BytesRef) {

          final SortedSetDocValues values = DocValues.getSortedSet(context.reader(), field);

          final long minOrd;
          if (lowerVal == null) {
            minOrd = 0;
          } else {
            final long ord = values.lookupTerm((BytesRef) lowerVal);
            if (ord < 0) {
              minOrd = -1 - ord;
            } else if (includeLower) {
              minOrd = ord;
            } else {
              minOrd = ord + 1;
            }
          }

          final long maxOrd;
          if (upperVal == null) {
            maxOrd = values.getValueCount() - 1;
          } else {
            final long ord = values.lookupTerm((BytesRef) upperVal);
            if (ord < 0) {
              maxOrd = -2 - ord;
            } else if (includeUpper) {
              maxOrd = ord;
            } else {
              maxOrd = ord - 1;
            }
          }

          if (minOrd > maxOrd) {
            return null;
          }

          return new TwoPhaseIterator(values) {

            @Override
            public boolean matches() throws IOException {
              for (long ord = values.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = values.nextOrd()) {
                if (ord >= minOrd && ord <= maxOrd) {
                  return true;
                }
              }
              return false;
            }

            @Override
            public float matchCost() {
              return 2; // 2 comparisons
            }
          };

        } else {
          throw new AssertionError();
        }
      }
    };
  }

}
diff --git a/lucene/sandbox/src/test/org/apache/lucene/search/TestDocValuesRangeQuery.java b/lucene/sandbox/src/test/org/apache/lucene/search/TestDocValuesRangeQuery.java
deleted file mode 100644
index c5ca64f3ae7..00000000000
-- a/lucene/sandbox/src/test/org/apache/lucene/search/TestDocValuesRangeQuery.java
++ /dev/null
@@ -1,307 +0,0 @@
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
package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.TestUtil;

public class TestDocValuesRangeQuery extends LuceneTestCase {

  public void testDuelNumericRangeQuery() throws IOException {
    final int iters = atLeast(10);
      for (int iter = 0; iter < iters; ++iter) {
      Directory dir = newDirectory();
      RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
      final int numDocs = atLeast(100);
      for (int i = 0; i < numDocs; ++i) {
        Document doc = new Document();
        final int numValues = random().nextInt(2);
        for (int j = 0; j < numValues; ++j) {
          final long value = TestUtil.nextLong(random(), -100, 10000);
          doc.add(new SortedNumericDocValuesField("dv", value));
          doc.add(new LongPoint("idx", value));
        }
        iw.addDocument(doc);
      }
      if (random().nextBoolean()) {
        iw.deleteDocuments(LongPoint.newRangeQuery("idx", 0L, 10L));
      }
      iw.commit();
      final IndexReader reader = iw.getReader();
      final IndexSearcher searcher = newSearcher(reader, false);
      iw.close();

      for (int i = 0; i < 100; ++i) {
        final Long min = TestUtil.nextLong(random(), -100, 1000);
        final Long max = TestUtil.nextLong(random(), -100, 1000);
        final Query q1 = LongPoint.newRangeQuery("idx", min, max);
        final Query q2 = DocValuesRangeQuery.newLongRange("dv", min, max, true, true);
        assertSameMatches(searcher, q1, q2, false);
      }

      reader.close();
      dir.close();
    }
  }

  private static BytesRef toSortableBytes(Long l) {
    if (l == null) {
      return null;
    } else {
      byte[] bytes = new byte[Long.BYTES];
      NumericUtils.longToSortableBytes(l, bytes, 0);
      return new BytesRef(bytes);
    }
  }

  public void testDuelNumericSorted() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    final int numDocs = atLeast(100);
    for (int i = 0; i < numDocs; ++i) {
      Document doc = new Document();
      final int numValues = random().nextInt(3);
      for (int j = 0; j < numValues; ++j) {
        final long value = TestUtil.nextLong(random(), -100, 10000);
        doc.add(new SortedNumericDocValuesField("dv1", value));
        doc.add(new SortedSetDocValuesField("dv2", toSortableBytes(value)));
      }
      iw.addDocument(doc);
    }
    if (random().nextBoolean()) {
      iw.deleteDocuments(DocValuesRangeQuery.newLongRange("dv1", 0L, 10L, true, true));
    }
    iw.commit();
    final IndexReader reader = iw.getReader();
    final IndexSearcher searcher = newSearcher(reader);
    iw.close();

    for (int i = 0; i < 100; ++i) {
      final Long min = random().nextBoolean() ? null : TestUtil.nextLong(random(), -100, 1000);
      final Long max = random().nextBoolean() ? null : TestUtil.nextLong(random(), -100, 1000);
      final boolean minInclusive = random().nextBoolean();
      final boolean maxInclusive = random().nextBoolean();
      final Query q1 = DocValuesRangeQuery.newLongRange("dv1", min, max, minInclusive, maxInclusive);
      final Query q2 = DocValuesRangeQuery.newBytesRefRange("dv2", toSortableBytes(min), toSortableBytes(max), minInclusive, maxInclusive);
      assertSameMatches(searcher, q1, q2, true);
    }

    reader.close();
    dir.close();
  }

  public void testScore() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    final int numDocs = atLeast(100);
    for (int i = 0; i < numDocs; ++i) {
      Document doc = new Document();
      final int numValues = random().nextInt(3);
      for (int j = 0; j < numValues; ++j) {
        final long value = TestUtil.nextLong(random(), -100, 10000);
        doc.add(new SortedNumericDocValuesField("dv1", value));
        doc.add(new SortedSetDocValuesField("dv2", toSortableBytes(value)));
      }
      iw.addDocument(doc);
    }
    if (random().nextBoolean()) {
      iw.deleteDocuments(DocValuesRangeQuery.newLongRange("dv1", 0L, 10L, true, true));
    }
    iw.commit();
    final IndexReader reader = iw.getReader();
    final IndexSearcher searcher = newSearcher(reader);
    iw.close();

    for (int i = 0; i < 100; ++i) {
      final Long min = random().nextBoolean() ? null : TestUtil.nextLong(random(), -100, 1000);
      final Long max = random().nextBoolean() ? null : TestUtil.nextLong(random(), -100, 1000);
      final boolean minInclusive = random().nextBoolean();
      final boolean maxInclusive = random().nextBoolean();

      final float boost = random().nextFloat() * 10;

      final Query q1 = new BoostQuery(DocValuesRangeQuery.newLongRange("dv1", min, max, minInclusive, maxInclusive), boost);
      final Query csq1 = new BoostQuery(new ConstantScoreQuery(DocValuesRangeQuery.newLongRange("dv1", min, max, minInclusive, maxInclusive)), boost);
      assertSameMatches(searcher, q1, csq1, true);

      final Query q2 = new BoostQuery(DocValuesRangeQuery.newBytesRefRange("dv2", toSortableBytes(min), toSortableBytes(max), minInclusive, maxInclusive), boost);
      final Query csq2 = new BoostQuery(new ConstantScoreQuery(DocValuesRangeQuery.newBytesRefRange("dv2", toSortableBytes(min), toSortableBytes(max), minInclusive, maxInclusive)), boost);
      assertSameMatches(searcher, q2, csq2, true);
    }

    reader.close();
    dir.close();
  }

  public void testApproximation() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    final int numDocs = atLeast(100);
    for (int i = 0; i < numDocs; ++i) {
      Document doc = new Document();
      final int numValues = random().nextInt(3);
      for (int j = 0; j < numValues; ++j) {
        final long value = TestUtil.nextLong(random(), -100, 10000);
        doc.add(new SortedNumericDocValuesField("dv1", value));
        doc.add(new SortedSetDocValuesField("dv2", toSortableBytes(value)));
        doc.add(new LongPoint("idx", value));
        doc.add(new StringField("f", random().nextBoolean() ? "a" : "b", Store.NO));
      }
      iw.addDocument(doc);
    }
    if (random().nextBoolean()) {
      iw.deleteDocuments(LongPoint.newRangeQuery("idx", 0L, 10L));
    }
    iw.commit();
    final IndexReader reader = iw.getReader();
    final IndexSearcher searcher = newSearcher(reader, false);
    iw.close();

    for (int i = 0; i < 100; ++i) {
      final Long min = TestUtil.nextLong(random(), -100, 1000);
      final Long max = TestUtil.nextLong(random(), -100, 1000);

      BooleanQuery.Builder ref = new BooleanQuery.Builder();
      ref.add(LongPoint.newRangeQuery("idx", min, max), Occur.FILTER);
      ref.add(new TermQuery(new Term("f", "a")), Occur.MUST);

      BooleanQuery.Builder bq1 = new BooleanQuery.Builder();
      bq1.add(DocValuesRangeQuery.newLongRange("dv1", min, max, true, true), Occur.FILTER);
      bq1.add(new TermQuery(new Term("f", "a")), Occur.MUST);

      assertSameMatches(searcher, ref.build(), bq1.build(), true);

      BooleanQuery.Builder bq2 = new BooleanQuery.Builder();
      bq2.add(DocValuesRangeQuery.newBytesRefRange("dv2", toSortableBytes(min), toSortableBytes(max), true, true), Occur.FILTER);
      bq2.add(new TermQuery(new Term("f", "a")), Occur.MUST);

      assertSameMatches(searcher, ref.build(), bq2.build(), true);
    }

    reader.close();
    dir.close();
  }

  private void assertSameMatches(IndexSearcher searcher, Query q1, Query q2, boolean scores) throws IOException {
    final int maxDoc = searcher.getIndexReader().maxDoc();
    final TopDocs td1 = searcher.search(q1, maxDoc, scores ? Sort.RELEVANCE : Sort.INDEXORDER);
    final TopDocs td2 = searcher.search(q2, maxDoc, scores ? Sort.RELEVANCE : Sort.INDEXORDER);
    assertEquals(td1.totalHits, td2.totalHits);
    for (int i = 0; i < td1.scoreDocs.length; ++i) {
      assertEquals(td1.scoreDocs[i].doc, td2.scoreDocs[i].doc);
      if (scores) {
        assertEquals(td1.scoreDocs[i].score, td2.scoreDocs[i].score, 10e-7);
      }
    }
  }

  public void testToString() {
    assertEquals("f:[2 TO 5]", DocValuesRangeQuery.newLongRange("f", 2L, 5L, true, true).toString());
    assertEquals("f:{2 TO 5]", DocValuesRangeQuery.newLongRange("f", 2L, 5L, false, true).toString());
    assertEquals("f:{2 TO 5}", DocValuesRangeQuery.newLongRange("f", 2L, 5L, false, false).toString());
    assertEquals("f:{* TO 5}", DocValuesRangeQuery.newLongRange("f", null, 5L, false, false).toString());
    assertEquals("f:[2 TO *}", DocValuesRangeQuery.newLongRange("f", 2L, null, true, false).toString());

    BytesRef min = new BytesRef("a");
    BytesRef max = new BytesRef("b");
    assertEquals("f:[[61] TO [62]]", DocValuesRangeQuery.newBytesRefRange("f", min, max, true, true).toString());
    assertEquals("f:{[61] TO [62]]", DocValuesRangeQuery.newBytesRefRange("f", min, max, false, true).toString());
    assertEquals("f:{[61] TO [62]}", DocValuesRangeQuery.newBytesRefRange("f", min, max, false, false).toString());
    assertEquals("f:{* TO [62]}", DocValuesRangeQuery.newBytesRefRange("f", null, max, false, false).toString());
    assertEquals("f:[[61] TO *}", DocValuesRangeQuery.newBytesRefRange("f", min, null, true, false).toString());
  }

  public void testDocValuesRangeSupportsApproximation() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);
    Document doc = new Document();
    doc.add(new NumericDocValuesField("dv1", 5L));
    doc.add(new SortedDocValuesField("dv2", toSortableBytes(42L)));
    iw.addDocument(doc);
    iw.commit();
    final IndexReader reader = iw.getReader();
    final LeafReaderContext ctx = reader.leaves().get(0);
    final IndexSearcher searcher = newSearcher(reader);
    iw.close();

    Query q1 = DocValuesRangeQuery.newLongRange("dv1", 0L, 100L, random().nextBoolean(), random().nextBoolean());
    Weight w = searcher.createNormalizedWeight(q1, true);
    Scorer s = w.scorer(ctx);
    assertNotNull(s.twoPhaseIterator());

    Query q2 = DocValuesRangeQuery.newBytesRefRange("dv2", toSortableBytes(0L), toSortableBytes(100L), random().nextBoolean(), random().nextBoolean());
    w = searcher.createNormalizedWeight(q2, true);
    s = w.scorer(ctx);
    assertNotNull(s.twoPhaseIterator());

    reader.close();
    dir.close();
  }

  public void testLongRangeBoundaryValues() throws IOException {
    Directory dir = newDirectory();
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir);

    Document doc = new Document();
    doc.add(new SortedNumericDocValuesField("dv", 100l));
    iw.addDocument(doc);

    doc = new Document();
    doc.add(new SortedNumericDocValuesField("dv", 200l));
    iw.addDocument(doc);

    iw.commit();

    final IndexReader reader = iw.getReader();
    final IndexSearcher searcher = newSearcher(reader, false);
    iw.close();

    Long min = Long.MIN_VALUE;
    Long max = Long.MIN_VALUE;
    Query query = DocValuesRangeQuery.newLongRange("dv", min, max, true, false);
    TopDocs td = searcher.search(query, searcher.reader.maxDoc(), Sort.INDEXORDER);
    assertEquals(0, td.totalHits);

    min = Long.MAX_VALUE;
    max = Long.MAX_VALUE;
    query = DocValuesRangeQuery.newLongRange("dv", min, max, false, true);
    td = searcher.search(query, searcher.reader.maxDoc(), Sort.INDEXORDER);
    assertEquals(0, td.totalHits);

    reader.close();
    dir.close();
  }

}
diff --git a/solr/contrib/analysis-extras/src/java/org/apache/solr/schema/ICUCollationField.java b/solr/contrib/analysis-extras/src/java/org/apache/solr/schema/ICUCollationField.java
index 20711632acf..51527682a0f 100644
-- a/solr/contrib/analysis-extras/src/java/org/apache/solr/schema/ICUCollationField.java
++ b/solr/contrib/analysis-extras/src/java/org/apache/solr/schema/ICUCollationField.java
@@ -32,7 +32,6 @@ import org.apache.lucene.collation.ICUCollationKeyAnalyzer;
 import org.apache.lucene.document.SortedDocValuesField;
 import org.apache.lucene.document.SortedSetDocValuesField;
 import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.DocValuesRangeQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermRangeQuery;
@@ -272,13 +271,8 @@ public class ICUCollationField extends FieldType {
     BytesRef low = part1 == null ? null : getCollationKey(f, part1);
     BytesRef high = part2 == null ? null : getCollationKey(f, part2);
     if (!field.indexed() && field.hasDocValues()) {
      if (field.multiValued()) {
          return DocValuesRangeQuery.newBytesRefRange(
              field.getName(), low, high, minInclusive, maxInclusive);
        } else {
          return DocValuesRangeQuery.newBytesRefRange(
              field.getName(), low, high, minInclusive, maxInclusive);
        } 
      return SortedSetDocValuesField.newRangeQuery(
          field.getName(), low, high, minInclusive, maxInclusive);
     } else {
       return new TermRangeQuery(field.getName(), low, high, minInclusive, maxInclusive);
     }
diff --git a/solr/core/src/java/org/apache/solr/schema/CollationField.java b/solr/core/src/java/org/apache/solr/schema/CollationField.java
index 998db2ac336..805e20498c4 100644
-- a/solr/core/src/java/org/apache/solr/schema/CollationField.java
++ b/solr/core/src/java/org/apache/solr/schema/CollationField.java
@@ -36,7 +36,6 @@ import org.apache.lucene.collation.CollationKeyAnalyzer;
 import org.apache.lucene.document.SortedDocValuesField;
 import org.apache.lucene.document.SortedSetDocValuesField;
 import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.DocValuesRangeQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermRangeQuery;
@@ -242,7 +241,7 @@ public class CollationField extends FieldType {
     BytesRef low = part1 == null ? null : getCollationKey(f, part1);
     BytesRef high = part2 == null ? null : getCollationKey(f, part2);
     if (!field.indexed() && field.hasDocValues()) {
      return DocValuesRangeQuery.newBytesRefRange(
      return SortedSetDocValuesField.newRangeQuery(
           field.getName(), low, high, minInclusive, maxInclusive);
     } else {
       return new TermRangeQuery(field.getName(), low, high, minInclusive, maxInclusive);
diff --git a/solr/core/src/java/org/apache/solr/schema/EnumField.java b/solr/core/src/java/org/apache/solr/schema/EnumField.java
index 967070c8e9d..5723206563e 100644
-- a/solr/core/src/java/org/apache/solr/schema/EnumField.java
++ b/solr/core/src/java/org/apache/solr/schema/EnumField.java
@@ -43,7 +43,6 @@ import org.apache.lucene.legacy.LegacyNumericUtils;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.EnumFieldSource;
 import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocValuesRangeQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.util.BytesRef;
@@ -253,10 +252,21 @@ public class EnumField extends PrimitiveFieldType {
     Query query = null;
     final boolean matchOnly = field.hasDocValues() && !field.indexed();
     if (matchOnly) {
      query = new ConstantScoreQuery(DocValuesRangeQuery.newLongRange(field.getName(),
              min == null ? null : minValue.longValue(),
              max == null ? null : maxValue.longValue(),
              minInclusive, maxInclusive));
      long lowerValue = Long.MIN_VALUE;
      long upperValue = Long.MAX_VALUE;
      if (minValue != null) {
        lowerValue = minValue.longValue();
        if (minInclusive == false) {
          ++lowerValue;
        }
      }
      if (maxValue != null) {
        upperValue = maxValue.longValue();
        if (maxInclusive == false) {
          --upperValue;
        }
      }
      query = new ConstantScoreQuery(NumericDocValuesField.newRangeQuery(field.getName(), lowerValue, upperValue));
     } else {
       query = LegacyNumericRangeQuery.newIntRange(field.getName(), DEFAULT_PRECISION_STEP,
           min == null ? null : minValue,
diff --git a/solr/core/src/java/org/apache/solr/schema/FieldType.java b/solr/core/src/java/org/apache/solr/schema/FieldType.java
index 3922edc1060..54f882f178b 100644
-- a/solr/core/src/java/org/apache/solr/schema/FieldType.java
++ b/solr/core/src/java/org/apache/solr/schema/FieldType.java
@@ -36,13 +36,13 @@ import org.apache.lucene.analysis.util.CharFilterFactory;
 import org.apache.lucene.analysis.util.TokenFilterFactory;
 import org.apache.lucene.analysis.util.TokenizerFactory;
 import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedSetDocValuesField;
 import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.legacy.LegacyNumericType;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocValuesRangeQuery;
 import org.apache.lucene.search.DocValuesRewriteMethod;
 import org.apache.lucene.search.MultiTermQuery;
 import org.apache.lucene.search.PrefixQuery;
@@ -720,17 +720,17 @@ public abstract class FieldType extends FieldProperties {
    */
   public Query getRangeQuery(QParser parser, SchemaField field, String part1, String part2, boolean minInclusive, boolean maxInclusive) {
     // TODO: change these all to use readableToIndexed/bytes instead (e.g. for unicode collation)
    final BytesRef miValue = part1 == null ? null : new BytesRef(toInternal(part1));
    final BytesRef maxValue = part2 == null ? null : new BytesRef(toInternal(part2));
     if (field.hasDocValues() && !field.indexed()) {
      return DocValuesRangeQuery.newBytesRefRange(
          field.getName(),
          part1 == null ? null : new BytesRef(toInternal(part1)),
          part2 == null ? null : new BytesRef(toInternal(part2)),
          minInclusive, maxInclusive);
      return SortedSetDocValuesField.newRangeQuery(
            field.getName(),
            miValue, maxValue,
            minInclusive, maxInclusive);
     } else {
       SolrRangeQuery rangeQuery = new SolrRangeQuery(
             field.getName(),
            part1 == null ? null : new BytesRef(toInternal(part1)),
            part2 == null ? null : new BytesRef(toInternal(part2)),
            miValue, maxValue,
             minInclusive, maxInclusive);
       return rangeQuery;
     }
diff --git a/solr/core/src/java/org/apache/solr/schema/TrieField.java b/solr/core/src/java/org/apache/solr/schema/TrieField.java
index 0e8324cd826..57dbefffb62 100644
-- a/solr/core/src/java/org/apache/solr/schema/TrieField.java
++ b/solr/core/src/java/org/apache/solr/schema/TrieField.java
@@ -43,7 +43,7 @@ import org.apache.lucene.queries.function.valuesource.DoubleFieldSource;
 import org.apache.lucene.queries.function.valuesource.FloatFieldSource;
 import org.apache.lucene.queries.function.valuesource.IntFieldSource;
 import org.apache.lucene.queries.function.valuesource.LongFieldSource;
import org.apache.lucene.search.DocValuesRangeQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.SortedSetSelector;
@@ -376,9 +376,9 @@ public class TrieField extends PrimitiveFieldType {
     switch (type) {
       case INTEGER:
         if (matchOnly) {
          query = DocValuesRangeQuery.newLongRange(field.getName(),
                min == null ? null : (long) Integer.parseInt(min),
                max == null ? null : (long) Integer.parseInt(max),
          query = numericDocValuesRangeQuery(field.getName(),
                min == null ? null : Integer.parseInt(min),
                max == null ? null : Integer.parseInt(max),
                 minInclusive, maxInclusive);
         } else {
           query = LegacyNumericRangeQuery.newIntRange(field.getName(), ps,
@@ -399,7 +399,7 @@ public class TrieField extends PrimitiveFieldType {
         break;
       case LONG:
         if (matchOnly) {
          query = DocValuesRangeQuery.newLongRange(field.getName(),
          query = numericDocValuesRangeQuery(field.getName(),
                 min == null ? null : Long.parseLong(min),
                 max == null ? null : Long.parseLong(max),
                 minInclusive, maxInclusive);
@@ -422,7 +422,7 @@ public class TrieField extends PrimitiveFieldType {
         break;
       case DATE:
         if (matchOnly) {
          query = DocValuesRangeQuery.newLongRange(field.getName(),
          query = numericDocValuesRangeQuery(field.getName(),
                 min == null ? null : DateMathParser.parseMath(null, min).getTime(),
                 max == null ? null : DateMathParser.parseMath(null, max).getTime(),
                 minInclusive, maxInclusive);
@@ -440,6 +440,35 @@ public class TrieField extends PrimitiveFieldType {
     return query;
   }
 
  private static Query numericDocValuesRangeQuery(
      String field,
      Number lowerValue, Number upperValue,
      boolean lowerInclusive, boolean upperInclusive) {

    long actualLowerValue = Long.MIN_VALUE;
    if (lowerValue != null) {
      actualLowerValue = lowerValue.longValue();
      if (lowerInclusive == false) {
        if (actualLowerValue == Long.MAX_VALUE) {
          return new MatchNoDocsQuery();
        }
        ++actualLowerValue;
      }
    }

    long actualUpperValue = Long.MAX_VALUE;
    if (upperValue != null) {
      actualUpperValue = upperValue.longValue();
      if (upperInclusive == false) {
        if (actualUpperValue == Long.MIN_VALUE) {
          return new MatchNoDocsQuery();
        }
        --actualUpperValue;
      }
    }
    return NumericDocValuesField.newRangeQuery(field, actualLowerValue, actualUpperValue);
  }

   private static long FLOAT_NEGATIVE_INFINITY_BITS = (long)Float.floatToIntBits(Float.NEGATIVE_INFINITY);
   private static long DOUBLE_NEGATIVE_INFINITY_BITS = Double.doubleToLongBits(Double.NEGATIVE_INFINITY);
   private static long FLOAT_POSITIVE_INFINITY_BITS = (long)Float.floatToIntBits(Float.POSITIVE_INFINITY);
@@ -476,10 +505,10 @@ public class TrieField extends PrimitiveFieldType {
     } else { // If both max and min are negative (or -0d), then issue range query with max and min reversed
       if ((minVal == null || minVal.doubleValue() < 0d || minBits == minusZeroBits) &&
           (maxVal != null && (maxVal.doubleValue() < 0d || maxBits == minusZeroBits))) {
        query = DocValuesRangeQuery.newLongRange
        query = numericDocValuesRangeQuery
             (fieldName, maxBits, (min == null ? negativeInfinityBits : minBits), maxInclusive, minInclusive);
       } else { // If both max and min are positive, then issue range query
        query = DocValuesRangeQuery.newLongRange
        query = numericDocValuesRangeQuery
             (fieldName, minBits, (max == null ? positiveInfinityBits : maxBits), minInclusive, maxInclusive);
       }
     }
- 
2.19.1.windows.1

