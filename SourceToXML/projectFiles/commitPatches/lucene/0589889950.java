From 05898899508b7a895f15f816caafabe1e6965d40 Mon Sep 17 00:00:00 2001
From: Grant Ingersoll <gsingers@apache.org>
Date: Sat, 12 Dec 2009 23:00:27 +0000
Subject: [PATCH] SOLR-1297: Added Sort By Function capability

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@889997 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |   2 +
 .../org/apache/solr/search/QueryParsing.java  | 157 ++++++++++++++----
 .../solr/search/function/ValueSource.java     | 125 ++++++++++++--
 .../apache/solr/search/QueryParsingTest.java  |  95 ++++++++++-
 .../search/function/SortByFunctionTest.java   |  96 +++++++++++
 5 files changed, 425 insertions(+), 50 deletions(-)
 create mode 100644 src/test/org/apache/solr/search/function/SortByFunctionTest.java

diff --git a/CHANGES.txt b/CHANGES.txt
index ee59a697d2d..4b3fd6cfd38 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -58,6 +58,8 @@ New Features
 
 * SOLR-1625: Add regexp support for TermsComponent (Uri Boness via noble)
 
* SOLR-1297: Add sort by Function capability (gsingers)

 Optimizations
 ----------------------
 
diff --git a/src/java/org/apache/solr/search/QueryParsing.java b/src/java/org/apache/solr/search/QueryParsing.java
index cd3a9c7c2fa..ea721f72a94 100644
-- a/src/java/org/apache/solr/search/QueryParsing.java
++ b/src/java/org/apache/solr/search/QueryParsing.java
@@ -41,6 +41,7 @@ import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.search.function.FunctionQuery;
import org.apache.solr.search.function.ValueSource;
 
 import java.io.IOException;
 import java.util.ArrayList;
@@ -216,7 +217,6 @@ public class QueryParsing {
   }
 
 
  private static Pattern sortSep = Pattern.compile(",");
 
   /**
    * Returns null if the sortSpec is the standard sort desc.
@@ -240,58 +240,145 @@ public class QueryParsing {
    */
   public static Sort parseSort(String sortSpec, IndexSchema schema) {
     if (sortSpec == null || sortSpec.length() == 0) return null;
    char[] chars = sortSpec.toCharArray();
    int i = 0;
    StringBuilder buffer = new StringBuilder(sortSpec.length());
    String sort = null;
    String order = null;
    int functionDepth = 0;
    boolean score = true;
    List<SortField> lst = new ArrayList<SortField>(5);
    boolean needOrder = false;
    while (i < chars.length) {
      if (Character.isWhitespace(chars[i]) && functionDepth == 0) {
        if (buffer.length() == 0) {
          //do nothing
        } else {
          if (needOrder == false) {
            sort = buffer.toString().trim();
            buffer.setLength(0);
            needOrder = true;
          } else {
            order = buffer.toString().trim();
            buffer.setLength(0);
            needOrder = false;
          }
        }
      } else if (chars[i] == '(' && functionDepth == 0) {
        buffer.append(chars[i]);
        functionDepth++;
      } else if (chars[i] == ')' && functionDepth > 0) {
        buffer.append(chars[i]);
        functionDepth--;//close up one layer
      } else if (chars[i] == ',' && functionDepth == 0) {//can either be a separator of sort declarations, or a separator in a function
        //we have a separator between sort declarations,
        // We may need an order still, but then evaluate it, as we should have everything we need
        if (needOrder == true && buffer.length() > 0){
          order = buffer.toString().trim();
          buffer.setLength(0);
          needOrder = false;
        }
        score = processSort(schema, sort, order, lst);
        sort = null;
        order = null;
        buffer.setLength(0);//get ready for the next one, if there is one
      } else if (chars[i] == ',' && functionDepth > 0) {
        //we are in a function
        buffer.append(chars[i]);
      } else {
        //just a regular old char, add it to the buffer
        buffer.append(chars[i]);
      }
      i++;
    }
    if (buffer.length() > 0 && needOrder){//see if we have anything left, at most it should be an order
      order = buffer.toString().trim();
      buffer.setLength(0);
      needOrder = false;
    }
 
    String[] parts = sortSep.split(sortSpec.trim());
    if (parts.length == 0) return null;
    //do some sanity checks
    if (functionDepth != 0){
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to parse sort spec, mismatched parentheses: " + sortSpec);
    }
    if (buffer.length() > 0){//there's something wrong, as everything should have been parsed by now
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to parse sort spec: " + sortSpec);
    }
    if (needOrder == false && sort != null && sort.equals("") == false && order != null && order.equals("") == false){//handle the last declaration
      score = processSort(schema, sort, order, lst);
    }
    //If the normal case (by score desc) do nothing
    if (lst.size() == 1 && score == true && lst.get(0).getReverse() == false) {
      return null; // do normal scoring...
    }
    return new Sort((SortField[]) lst.toArray(new SortField[lst.size()]));
  }
 
    SortField[] lst = new SortField[parts.length];
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i].trim();
  private static boolean processSort(IndexSchema schema, String sort, String order, List<SortField> lst) {
    boolean score = false;
    if (sort != null && order != null) {
       boolean top = true;
      //determine the ordering, ascending or descending
      int idx = part.indexOf(' ');
      if (idx > 0) {
        String order = part.substring(idx + 1).trim();
        if ("desc".equals(order) || "top".equals(order)) {
          top = true;
        } else if ("asc".equals(order) || "bottom".equals(order)) {
          top = false;
        } else {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown sort order: " + order);
        }
        part = part.substring(0, idx).trim();
      if ("desc".equals(order) || "top".equals(order)) {
        top = true;
      } else if ("asc".equals(order) || "bottom".equals(order)) {
        top = false;
       } else {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Missing sort order.");
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown sort order: " + order);
       }
      //figure out the field or score
      if ("score".equals(part)) {
      //we got the order, now deal with the sort
      if ("score".equals(sort)) {
        score = true;
         if (top) {
          // If there is only one thing in the list, just do the regular thing...
          if (parts.length == 1) {
            return null; // do normal scoring...
          }
          lst[i] = SortField.FIELD_SCORE;
          lst.add(SortField.FIELD_SCORE);
         } else {
          lst[i] = new SortField(null, SortField.SCORE, true);
          lst.add(new SortField(null, SortField.SCORE, true));
         }
      } else if (DOCID.equals(part)) {
        lst[i] = new SortField(null, SortField.DOC, top);
      } else if (DOCID.equals(sort)) {
        lst.add(new SortField(null, SortField.DOC, top));
       } else {
        //See if we have a Field first, then see if it is a function, then throw an exception
         // getField could throw an exception if the name isn't found
         SchemaField f = null;
         try {
          f = schema.getField(part);
          f = schema.getField(sort);
         }
         catch (SolrException e) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on undefined field: " + part, e);
          //Not an error just yet
         }
        if (f == null || !f.indexed()) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on unindexed field: " + part);
        if (f != null) {
          if (f == null || !f.indexed()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on unindexed field: " + sort);
          }
          lst.add(f.getType().getSortField(f, top));
        } else {
          //See if we have a function:
          FunctionQuery query = null;
          try {
            query = parseFunction(sort, schema);
            if (query != null) {
              ValueSource valueSource = query.getValueSource();
              //We have a function query
              try {
                lst.add(valueSource.getSortField(top));
              } catch (IOException e) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error getting the sort for this function: " + sort, e);
              }
            } else {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on undefined function: " + sort);
            }
          } catch (ParseException e) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "can not sort on undefined field or function: " + sort, e);
          }

         }
        lst[i] = f.getType().getSortField(f, top);
       }
    } else if (sort == null) {//no sort value
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
              "Must declare sort field or function");
    } else if (order == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Missing sort order: ");
     }
    return new Sort(lst);
    return score;
   }
 
 
diff --git a/src/java/org/apache/solr/search/function/ValueSource.java b/src/java/org/apache/solr/search/function/ValueSource.java
index 7862a08a034..1d1e985e03f 100644
-- a/src/java/org/apache/solr/search/function/ValueSource.java
++ b/src/java/org/apache/solr/search/function/ValueSource.java
@@ -18,13 +18,19 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.search.function.DocValues;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.SortField;
 
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.IdentityHashMap;
 import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
 
 /**
  * Instantiates {@link org.apache.solr.search.function.DocValues} for a particular reader.
@@ -40,7 +46,8 @@ public abstract class ValueSource implements Serializable {
     return getValues(null, reader);
   }
 
  /** Gets the values for this reader and the context that was previously
  /**
   * Gets the values for this reader and the context that was previously
    * passed to createWeight()
    */
   public DocValues getValues(Map context, IndexReader reader) throws IOException {
@@ -51,23 +58,113 @@ public abstract class ValueSource implements Serializable {
 
   public abstract int hashCode();
 
  /** description of field, used in explain() */
  /**
   * description of field, used in explain()
   */
   public abstract String description();
 
   public String toString() {
     return description();
   }
 
  /** Implementations should propagate createWeight to sub-ValueSources which can optionally store
  /**
   * Get the SortField for this ValueSource.  Uses the {@link #getValues(java.util.Map, org.apache.lucene.index.IndexReader)}
   * to populate the SortField.
   * 
   * @param reverse true if the order should be reversed.
   * @return The {@link org.apache.lucene.search.SortField} for the ValueSource
   * @throws IOException if there was a problem reading the values.
   */
  public SortField getSortField(boolean reverse) throws IOException {
    //should we pass in the description for the field name?
    //Hmm, Lucene is going to intern whatever we pass in, not sure I like that
    //and we can't pass in null, either, as that throws an illegal arg. exception
    return new SortField(description(), new ValueSourceComparatorSource(), reverse);
  }


  /**
   * Implementations should propagate createWeight to sub-ValueSources which can optionally store
    * weight info in the context. The context object will be passed to getValues()
   * where this info can be retrieved. */
   * where this info can be retrieved.
   */
   public void createWeight(Map context, Searcher searcher) throws IOException {
   }
 
  /** Returns a new non-threadsafe context map. */
  /**
   * Returns a new non-threadsafe context map.
   */
   public static Map newContext() {
     return new IdentityHashMap();
   }

  class ValueSourceComparatorSource extends FieldComparatorSource {


    public ValueSourceComparatorSource() {

    }

    public FieldComparator newComparator(String fieldname, int numHits,
                                         int sortPos, boolean reversed) throws IOException {
      return new ValueSourceComparator(numHits);
    }
  }

  /**
   * Implement a {@link org.apache.lucene.search.FieldComparator} that works
   * off of the {@link org.apache.solr.search.function.DocValues} for a ValueSource
   * instead of the normal Lucene FieldComparator that works off of a FieldCache.
   */
  class ValueSourceComparator extends FieldComparator {
    private final double[] values;
    private DocValues docVals;
    private double bottom;

    ValueSourceComparator(int numHits) {
      values = new double[numHits];
    }

    public int compare(int slot1, int slot2) {
      final double v1 = values[slot1];
      final double v2 = values[slot2];
      if (v1 > v2) {
        return 1;
      } else if (v1 < v2) {
        return -1;
      } else {
        return 0;
      }

    }

    public int compareBottom(int doc) {
      final double v2 = docVals.doubleVal(doc);
      if (bottom > v2) {
        return 1;
      } else if (bottom < v2) {
        return -1;
      } else {
        return 0;
      }
    }

    public void copy(int slot, int doc) {
      values[slot] = docVals.doubleVal(doc);
    }

    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      docVals = getValues(Collections.emptyMap(), reader);
    }

    public void setBottom(final int bottom) {
      this.bottom = values[bottom];
    }

    public Comparable value(int slot) {
      return Double.valueOf(values[slot]);
    }
  }
 }
 
 
@@ -86,7 +183,9 @@ class ValueSourceScorer extends Scorer {
     setCheckDeletes(true);
   }
 
  public IndexReader getReader() { return reader; }
  public IndexReader getReader() {
    return reader;
  }
 
   public void setCheckDeletes(boolean checkDeletes) {
     this.checkDeletes = checkDeletes && reader.hasDeletions();
@@ -107,9 +206,9 @@ class ValueSourceScorer extends Scorer {
 
   @Override
   public int nextDoc() throws IOException {
    for(;;) {
    for (; ;) {
       doc++;
      if (doc >= maxDoc) return doc=NO_MORE_DOCS;
      if (doc >= maxDoc) return doc = NO_MORE_DOCS;
       if (matches(doc)) return doc;
     }
   }
@@ -117,7 +216,7 @@ class ValueSourceScorer extends Scorer {
   @Override
   public int advance(int target) throws IOException {
     // also works fine when target==NO_MORE_DOCS
    doc = target-1;
    doc = target - 1;
     return nextDoc();
   }
 
@@ -126,7 +225,7 @@ class ValueSourceScorer extends Scorer {
   }
 
   public boolean next() {
    for(;;) {
    for (; ;) {
       doc++;
       if (doc >= maxDoc) return false;
       if (matches(doc)) return true;
@@ -134,7 +233,7 @@ class ValueSourceScorer extends Scorer {
   }
 
   public boolean skipTo(int target) {
    doc = target-1;
    doc = target - 1;
     return next();
   }
 
diff --git a/src/test/org/apache/solr/search/QueryParsingTest.java b/src/test/org/apache/solr/search/QueryParsingTest.java
index 1ba6970d5b4..af4ec9daea2 100644
-- a/src/test/org/apache/solr/search/QueryParsingTest.java
++ b/src/test/org/apache/solr/search/QueryParsingTest.java
@@ -43,8 +43,14 @@ public class QueryParsingTest extends AbstractSolrTestCase {
     IndexSchema schema = h.getCore().getSchema();
     sort = QueryParsing.parseSort("score desc", schema);
     assertNull("sort", sort);//only 1 thing in the list, no Sort specified
    sort = QueryParsing.parseSort("weight desc", schema);

    sort = QueryParsing.parseSort("score asc", schema);
     SortField[] flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.SCORE);
    assertTrue(flds[0].getReverse());

    sort = QueryParsing.parseSort("weight desc", schema);
    flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
     assertEquals(flds[0].getReverse(), true);
@@ -79,13 +85,71 @@ public class QueryParsingTest extends AbstractSolrTestCase {
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");
    assertEquals(flds[1].getType(), SortField.LONG);
     assertEquals(flds[1].getField(), "bday");
    assertEquals(flds[1].getType(), SortField.LONG);
     //handles trailing commas
     sort = QueryParsing.parseSort("weight desc,", schema);
     flds = sort.getSort();
     assertEquals(flds[0].getType(), SortField.FLOAT);
     assertEquals(flds[0].getField(), "weight");

    //test functions
    sort = QueryParsing.parseSort("pow(weight, 2) desc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.CUSTOM);
    //Not thrilled about the fragility of string matching here, but...
    //the value sources get wrapped, so the out field is different than the input
    assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");

    sort = QueryParsing.parseSort("pow(weight,                 2)         desc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.CUSTOM);
    //Not thrilled about the fragility of string matching here, but...
    //the value sources get wrapped, so the out field is different than the input
    assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");


    sort = QueryParsing.parseSort("pow(weight, 2) desc, weight    desc,   bday    asc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.CUSTOM);

    //Not thrilled about the fragility of string matching here, but...
    //the value sources get wrapped, so the out field is different than the input
    assertEquals(flds[0].getField(), "pow(float(weight),const(2.0))");

    assertEquals(flds[1].getType(), SortField.FLOAT);
    assertEquals(flds[1].getField(), "weight");
    assertEquals(flds[2].getField(), "bday");
    assertEquals(flds[2].getType(), SortField.LONG);
    
    //handles trailing commas
    sort = QueryParsing.parseSort("weight desc,", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.FLOAT);
    assertEquals(flds[0].getField(), "weight");

    try {
      //bad number of parens, but the function parser can handle an extra close
      sort = QueryParsing.parseSort("pow(weight,2)) desc, bday asc", schema);
    } catch (SolrException e) {
      assertTrue(false);
    }
    //Test literals in functions
    sort = QueryParsing.parseSort("strdist(foo_s, \"junk\", jw) desc", schema);
    flds = sort.getSort();
    assertEquals(flds[0].getType(), SortField.CUSTOM);
    //the value sources get wrapped, so the out field is different than the input
    assertEquals(flds[0].getField(), "strdist(str(foo_s),literal(junk), dist=org.apache.lucene.search.spell.JaroWinklerDistance)");

    sort = QueryParsing.parseSort("", schema);
    assertNull(sort);

  }

  public void testBad() throws Exception {
    Sort sort;

    IndexSchema schema = h.getCore().getSchema();
     //test some bad vals
     try {
       sort = QueryParsing.parseSort("weight, desc", schema);
@@ -93,12 +157,39 @@ public class QueryParsingTest extends AbstractSolrTestCase {
     } catch (SolrException e) {
       //expected
     }
    try {
      sort = QueryParsing.parseSort("w", schema);
      assertTrue(false);
    } catch (SolrException e) {
      //expected
    }
     try {
       sort = QueryParsing.parseSort("weight desc, bday", schema);
       assertTrue(false);
     } catch (SolrException e) {
     }
 
    try {
      //bad number of commas
      sort = QueryParsing.parseSort("pow(weight,,2) desc, bday asc", schema);
      assertTrue(false);
    } catch (SolrException e) {
    }

    try {
      //bad function
      sort = QueryParsing.parseSort("pow() desc, bday asc", schema);
      assertTrue(false);
    } catch (SolrException e) {
    }

    try {
      //bad number of parens
      sort = QueryParsing.parseSort("pow((weight,2) desc, bday asc", schema);
      assertTrue(false);
    } catch (SolrException e) {
    }

   }
 
 }
diff --git a/src/test/org/apache/solr/search/function/SortByFunctionTest.java b/src/test/org/apache/solr/search/function/SortByFunctionTest.java
new file mode 100644
index 00000000000..6ec278bac0c
-- /dev/null
++ b/src/test/org/apache/solr/search/function/SortByFunctionTest.java
@@ -0,0 +1,96 @@
package org.apache.solr.search.function;
/**
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

import org.apache.solr.util.AbstractSolrTestCase;


/**
 *
 *
 **/
public class SortByFunctionTest extends AbstractSolrTestCase {
  public String getSchemaFile() {
    return "schema.xml";
  }

  public String getSolrConfigFile() {
    return "solrconfig.xml";
  }

  public void test() throws Exception {
    assertU(adoc("id", "1", "x_td", "0", "y_td", "2", "w_td", "25", "z_td", "5", "f_t", "ipod"));
    assertU(adoc("id", "2", "x_td", "2", "y_td", "2", "w_td", "15", "z_td", "5", "f_t", "ipod ipod ipod ipod ipod"));
    assertU(adoc("id", "3", "x_td", "3", "y_td", "2", "w_td", "55", "z_td", "5", "f_t", "ipod ipod ipod ipod ipod ipod ipod ipod ipod"));
    assertU(adoc("id", "4", "x_td", "4", "y_td", "2", "w_td", "45", "z_td", "5", "f_t", "ipod ipod ipod ipod ipod ipod ipod"));
    assertU(commit());

    assertQ(req("fl", "*,score", "q", "*:*"),
            "//*[@numFound='4']",
            "//float[@name='score']='1.0'",
            "//result/doc[1]/int[@name='id'][.='1']",
            "//result/doc[2]/int[@name='id'][.='2']",
            "//result/doc[3]/int[@name='id'][.='3']",
            "//result/doc[4]/int[@name='id'][.='4']"
    );
    assertQ(req("fl", "*,score", "q", "*:*", "sort", "score desc"),
            "//*[@numFound='4']",
            "//float[@name='score']='1.0'",
            "//result/doc[1]/int[@name='id'][.='1']",
            "//result/doc[2]/int[@name='id'][.='2']",
            "//result/doc[3]/int[@name='id'][.='3']",
            "//result/doc[4]/int[@name='id'][.='4']"
    );
    assertQ(req("fl", "id,score", "q", "f_t:ipod", "sort", "score desc"),
            "//*[@numFound='4']",
            "//result/doc[1]/int[@name='id'][.='1']",
            "//result/doc[2]/int[@name='id'][.='4']",
            "//result/doc[3]/int[@name='id'][.='2']",
            "//result/doc[4]/int[@name='id'][.='3']"
    );


    assertQ(req("fl", "*,score", "q", "*:*", "sort", "sum(x_td, y_td) desc"),
            "//*[@numFound='4']",
            "//float[@name='score']='1.0'",
            "//result/doc[1]/int[@name='id'][.='4']",
            "//result/doc[2]/int[@name='id'][.='3']",
            "//result/doc[3]/int[@name='id'][.='2']",
            "//result/doc[4]/int[@name='id'][.='1']"
    );
    assertQ(req("fl", "*,score", "q", "*:*", "sort", "sum(x_td, y_td) asc"),
            "//*[@numFound='4']",
            "//float[@name='score']='1.0'",
            "//result/doc[1]/int[@name='id'][.='1']",
            "//result/doc[2]/int[@name='id'][.='2']",
            "//result/doc[3]/int[@name='id'][.='3']",
            "//result/doc[4]/int[@name='id'][.='4']"
    );
    //the function is equal, w_td separates
    assertQ(req("q", "*:*", "fl", "id", "sort", "sum(z_td, y_td) asc, w_td asc"),
            "//*[@numFound='4']",
            "//result/doc[1]/int[@name='id'][.='2']",
            "//result/doc[2]/int[@name='id'][.='1']",
            "//result/doc[3]/int[@name='id'][.='4']",
            "//result/doc[4]/int[@name='id'][.='3']"
    );
  }
}

/*
<lst name="responseHeader"><int name="status">0</int><int name="QTime">93</int></lst><result name="response" numFound="4" start="0" maxScore="1.0"><doc><float name="score">1.0</float><int name="id">4</int><int name="intDefault">42</int><arr name="multiDefault"><str>muLti-Default</str></arr><date name="timestamp">2009-12-12T12:59:46.412Z</date><arr name="x_td"><double>4.0</double></arr><arr name="y_td"><double>2.0</double></arr></doc><doc><float name="score">1.0</float><int name="id">3</int><int name="intDefault">42</int><arr name="multiDefault"><str>muLti-Default</str></arr><date name="timestamp">2009-12-12T12:59:46.409Z</date><arr name="x_td"><double>3.0</double></arr><arr name="y_td"><double>2.0</double></arr></doc><doc><float name="score">1.0</float><int name="id">2</int><int name="intDefault">42</int><arr name="multiDefault"><str>muLti-Default</str></arr><date name="timestamp">2009-12-12T12:59:46.406Z</date><arr name="x_td"><double>2.0</double></arr><arr name="y_td"><double>2.0</double></arr></doc><doc><float name="score">1.0</float><int name="id">1</int><int name="intDefault">42</int><arr name="multiDefault"><str>muLti-Default</str></arr><date name="timestamp">2009-12-12T12:59:46.361Z</date><arr name="x_td"><double>0.0</double></arr><arr name="y_td"><double>2.0</double></arr></doc></result>
*/
\ No newline at end of file
- 
2.19.1.windows.1

