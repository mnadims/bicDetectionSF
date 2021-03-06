From 8b241d9230380b75157ad396c74ca1498f287666 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 8 Jan 2011 16:51:08 +0000
Subject: [PATCH] LUCENE-2831: remove/fix more uses of non top-level readers in
 prep for AtomicReaderContext

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1056734 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/search/ValueSourceParser.java | 11 ++--
 .../solr/search/function/OrdFieldSource.java  | 34 +++++++---
 .../function/ReverseOrdFieldSource.java       | 26 +++++---
 .../search/function/ScaleFloatFunction.java   | 62 +++++++++++++------
 4 files changed, 93 insertions(+), 40 deletions(-)

diff --git a/solr/src/java/org/apache/solr/search/ValueSourceParser.java b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
index 116058fc301..5b1805ee017 100755
-- a/solr/src/java/org/apache/solr/search/ValueSourceParser.java
++ b/solr/src/java/org/apache/solr/search/ValueSourceParser.java
@@ -83,7 +83,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     addParser("ord", new ValueSourceParser() {
       public ValueSource parse(FunctionQParser fp) throws ParseException {
         String field = fp.parseId();
        return new TopValueSource(new OrdFieldSource(field));
        return new OrdFieldSource(field);
       }
     });
     addParser("literal", new ValueSourceParser() {
@@ -94,15 +94,14 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
     addParser("rord", new ValueSourceParser() {
       public ValueSource parse(FunctionQParser fp) throws ParseException {
         String field = fp.parseId();
        return new TopValueSource(new ReverseOrdFieldSource(field));
        return new ReverseOrdFieldSource(field);
       }
     });
     addParser("top", new ValueSourceParser() {
       public ValueSource parse(FunctionQParser fp) throws ParseException {
        // top(vs) is now a no-op
         ValueSource source = fp.parseValueSource();
        // nested top is redundant, and ord and rord get automatically wrapped
        if (source instanceof TopValueSource) return source;
        return new TopValueSource(source);
        return source;
       }
     });
     addParser("linear", new ValueSourceParser() {
@@ -134,7 +133,7 @@ public abstract class ValueSourceParser implements NamedListInitializedPlugin {
         ValueSource source = fp.parseValueSource();
         float min = fp.parseFloat();
         float max = fp.parseFloat();
        return new TopValueSource(new ScaleFloatFunction(source, min, max));
        return new ScaleFloatFunction(source, min, max);
       }
     });
     addParser("div", new ValueSourceParser() {
diff --git a/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java b/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
index 93da97395d1..d68185664cb 100644
-- a/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/OrdFieldSource.java
@@ -18,8 +18,10 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
 import org.apache.solr.search.MutableValue;
 import org.apache.solr.search.MutableValueInt;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Map;
@@ -55,38 +57,52 @@ public class OrdFieldSource extends ValueSource {
 
 
   public DocValues getValues(Map context, IndexReader reader) throws IOException {
    return new StringIndexDocValues(this, reader, field) {
    int offset = 0;
    IndexReader topReader = reader;
    if (topReader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)topReader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      topReader = r;
    }
    final int off = offset;

    final FieldCache.DocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(topReader, field);

    return new DocValues() {
       protected String toTerm(String readableValue) {
         return readableValue;
       }
       
       public float floatVal(int doc) {
        return (float)termsIndex.getOrd(doc);
        return (float)sindex.getOrd(doc+off);
       }
 
       public int intVal(int doc) {
        return termsIndex.getOrd(doc);
        return sindex.getOrd(doc+off);
       }
 
       public long longVal(int doc) {
        return (long)termsIndex.getOrd(doc);
        return (long)sindex.getOrd(doc+off);
       }
 
       public double doubleVal(int doc) {
        return (double)termsIndex.getOrd(doc);
        return (double)sindex.getOrd(doc+off);
       }
 
       public int ordVal(int doc) {
        return termsIndex.getOrd(doc);
        return sindex.getOrd(doc+off);
       }
 
       public int numOrd() {
        return termsIndex.numOrd();
        return sindex.numOrd();
       }
 
       public String strVal(int doc) {
         // the string value of the ordinal, not the string itself
        return Integer.toString(termsIndex.getOrd(doc));
        return Integer.toString(sindex.getOrd(doc+off));
       }
 
       public String toString(int doc) {
@@ -105,7 +121,7 @@ public class OrdFieldSource extends ValueSource {
 
           @Override
           public void fillValue(int doc) {
            mval.value = termsIndex.getOrd(doc);
            mval.value = sindex.getOrd(doc);
             mval.exists = mval.value!=0;
           }
         };
diff --git a/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java b/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
index ef595a59aae..455fc3ab0f8 100644
-- a/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
++ b/solr/src/java/org/apache/solr/search/function/ReverseOrdFieldSource.java
@@ -19,6 +19,7 @@ package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.FieldCache;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Map;
@@ -56,25 +57,36 @@ public class ReverseOrdFieldSource extends ValueSource {
   }
 
   public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final FieldCache.DocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(reader, field);
    int offset = 0;
    IndexReader topReader = reader;
    if (topReader instanceof SolrIndexReader) {
      SolrIndexReader r = (SolrIndexReader)topReader;
      while (r.getParent() != null) {
        offset += r.getBase();
        r = r.getParent();
      }
      topReader = r;
    }
    final int off = offset;
 
    final FieldCache.DocTermsIndex sindex = FieldCache.DEFAULT.getTermsIndex(topReader, field);
     final int end = sindex.numOrd();
 
     return new DocValues() {
       public float floatVal(int doc) {
        return (float)(end - sindex.getOrd(doc));
        return (float)(end - sindex.getOrd(doc+off));
       }
 
       public int intVal(int doc) {
        return (end - sindex.getOrd(doc));
        return (end - sindex.getOrd(doc+off));
       }
 
       public long longVal(int doc) {
        return (long)(end - sindex.getOrd(doc));
        return (long)(end - sindex.getOrd(doc+off));
       }
 
       public int ordVal(int doc) {
        return (end - sindex.getOrd(doc));
        return (end - sindex.getOrd(doc+off));
       }
 
       public int numOrd() {
@@ -82,12 +94,12 @@ public class ReverseOrdFieldSource extends ValueSource {
       }
 
       public double doubleVal(int doc) {
        return (double)(end - sindex.getOrd(doc));
        return (double)(end - sindex.getOrd(doc+off));
       }
 
       public String strVal(int doc) {
         // the string value of the ordinal, not the string itself
        return Integer.toString((end - sindex.getOrd(doc)));
        return Integer.toString((end - sindex.getOrd(doc+off)));
       }
 
       public String toString(int doc) {
diff --git a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
index 6e8cd65c343..74875bedbfc 100755
-- a/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
++ b/solr/src/java/org/apache/solr/search/function/ScaleFloatFunction.java
@@ -18,7 +18,9 @@
 package org.apache.solr.search.function;
 
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
 import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.SolrIndexReader;
 
 import java.io.IOException;
 import java.util.Map;
@@ -49,24 +51,25 @@ public class ScaleFloatFunction extends ValueSource {
     return "scale(" + source.description() + "," + min + "," + max + ")";
   }
 
  public DocValues getValues(Map context, IndexReader reader) throws IOException {
    final DocValues vals =  source.getValues(context, reader);
    int maxDoc = reader.maxDoc();
  private static class ScaleInfo {
    float minVal;
    float maxVal;
  }
 
    // this doesn't take into account deleted docs!
    float minVal=0.0f;
    float maxVal=0.0f;
  private ScaleInfo createScaleInfo(Map context, IndexReader reader) throws IOException {
    IndexReader.ReaderContext ctx = ValueSource.readerToContext(context, reader);
    while (ctx.parent != null) ctx = ctx.parent;
    AtomicReaderContext[] leaves = ctx.leaves();
    if (ctx == null) leaves = new AtomicReaderContext[] {(AtomicReaderContext)ctx};
 
    if (maxDoc>0) {
      minVal = maxVal = vals.floatVal(0);      
    }
    float minVal = Float.POSITIVE_INFINITY;
    float maxVal = Float.NEGATIVE_INFINITY;
 
    // Traverse the complete set of values to get the min and the max.
    // Future alternatives include being able to ask a DocValues for min/max
    // Another memory-intensive option is to cache the values in
    // a float[] on this first pass.
    for (AtomicReaderContext leaf : leaves) {
      int maxDoc = leaf.reader.maxDoc();
      DocValues vals =  source.getValues(context, leaf.reader);
      for (int i=0; i<maxDoc; i++) {
 
    for (int i=0; i<maxDoc; i++) {
       float val = vals.floatVal(i);
       if ((Float.floatToRawIntBits(val) & (0xff<<23)) == 0xff<<23) {
         // if the exponent in the float is all ones, then this is +Inf, -Inf or NaN
@@ -75,14 +78,37 @@ public class ScaleFloatFunction extends ValueSource {
       }
       if (val < minVal) {
         minVal = val;
      } else if (val > maxVal) {
      }
      if (val > maxVal) {
         maxVal = val;
       }
     }
    }

    if (minVal == Float.POSITIVE_INFINITY) {
    // must have been an empty index
      minVal = maxVal = 0;
    }
 
    final float scale = (maxVal-minVal==0) ? 0 : (max-min)/(maxVal-minVal);
    final float minSource = minVal;
    final float maxSource = maxVal;
    ScaleInfo scaleInfo = new ScaleInfo();
    scaleInfo.minVal = minVal;
    scaleInfo.maxVal = maxVal;
    context.put(this.source, scaleInfo);
    return scaleInfo;
  }

  public DocValues getValues(Map context, IndexReader reader) throws IOException {

    ScaleInfo scaleInfo = (ScaleInfo)context.get(source);
    if (scaleInfo == null) {
      scaleInfo = createScaleInfo(context, reader);
    }

    final float scale = (scaleInfo.maxVal-scaleInfo.minVal==0) ? 0 : (max-min)/(scaleInfo.maxVal-scaleInfo.minVal);
    final float minSource = scaleInfo.minVal;
    final float maxSource = scaleInfo.maxVal;

    final DocValues vals =  source.getValues(context, reader);
 
     return new DocValues() {
       public float floatVal(int doc) {
- 
2.19.1.windows.1

