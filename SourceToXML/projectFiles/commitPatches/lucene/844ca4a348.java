From 844ca4a348e282b5f857aa7ce4de6f9781766ef9 Mon Sep 17 00:00:00 2001
From: Erick Erickson <erick@apache.org>
Date: Sat, 11 Jun 2016 17:38:19 -0700
Subject: [PATCH] SOLR-9187: Support dates and booleans in /export handler,
 support boolean DocValues fields

--
 solr/CHANGES.txt                              |  13 ++
 .../org/apache/solr/request/SimpleFacets.java |   4 +-
 .../solr/response/SortingResponseWriter.java  | 126 +++++++++++++++--
 .../org/apache/solr/schema/BoolField.java     |  43 +++++-
 .../apache/solr/search/SolrIndexSearcher.java |  12 +-
 .../collection1/conf/schema-docValues.xml     |   5 +-
 .../conf/schema-docValuesMissing.xml          |  13 ++
 .../conf/schema-docValuesMulti.xml            |   1 +
 .../solr/collection1/conf/schema15.xml        |   1 +
 .../solr/schema/DocValuesMissingTest.java     | 129 ++++++++++++++++++
 .../solr/schema/DocValuesMultiTest.java       | 103 +++++++++++++-
 .../org/apache/solr/schema/DocValuesTest.java |  87 ++++++++++--
 .../apache/solr/client/solrj/io/Tuple.java    |  53 ++++++-
 .../solr/configsets/streaming/conf/schema.xml |  13 +-
 .../client/solrj/io/stream/StreamingTest.java | 128 +++++++++++++++++
 15 files changed, 692 insertions(+), 39 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index b9b88d35fa7..659a1d7dbe4 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -47,6 +47,19 @@ Optimizations
 
 ==================  6.2.0 ==================
 

Upgrading from Solr any prior release
----------------------

Detailed Change List
----------------------

New Features
----------------------

* SOLR-9187: Support dates and booleans in /export handler, support boolean DocValues fields


 Bug Fixes
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
index c804b74ff03..017deb4f617 100644
-- a/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
++ b/solr/core/src/java/org/apache/solr/request/SimpleFacets.java
@@ -554,8 +554,8 @@ public class SimpleFacets {
 
      /*The user did not specify any preference*/
      if (method == null) {
      /* Always use filters for booleans... we know the number of values is very small. */
       if (type instanceof BoolField) {
       /* Always use filters for booleans if not DocValues only... we know the number of values is very small. */
       if (type instanceof BoolField && (field.indexed() == true || field.hasDocValues() == false)) {
          method = FacetMethod.ENUM;
        } else if (type.getNumericType() != null && !field.multiValued()) {
         /* the per-segment approach is optimal for numeric field types since there
diff --git a/solr/core/src/java/org/apache/solr/response/SortingResponseWriter.java b/solr/core/src/java/org/apache/solr/response/SortingResponseWriter.java
index 8daf90f93e1..b3752a87f08 100644
-- a/solr/core/src/java/org/apache/solr/response/SortingResponseWriter.java
++ b/solr/core/src/java/org/apache/solr/response/SortingResponseWriter.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.Writer;
 import java.lang.invoke.MethodHandles;
import java.util.Date;
 import java.util.List;
 import java.util.ArrayList;
 
@@ -44,10 +45,12 @@ import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.schema.BoolField;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.schema.StrField;
import org.apache.solr.schema.TrieDateField;
 import org.apache.solr.schema.TrieDoubleField;
 import org.apache.solr.schema.TrieFloatField;
 import org.apache.solr.schema.TrieIntField;
@@ -99,14 +102,24 @@ public class SortingResponseWriter implements QueryResponseWriter {
       exception = new IOException(new SyntaxError("Scoring is not currently supported with xsort."));
     }
 
    FixedBitSet[] sets = (FixedBitSet[])req.getContext().get("export");
    Integer th = (Integer)req.getContext().get("totalHits");

    if(sets == null) {
      exception = new IOException(new SyntaxError("xport RankQuery is required for xsort: rq={!xport}"));
    // There is a bailout in SolrIndexSearcher.getDocListNC when there are _no_ docs in the index at all.
    // if (lastDocRequested <= 0) {
    // That causes the totalHits and export entries in the context to _not_ get set.
    // The only time that really matters is when we search against an _empty_ set. That's too obscure
    // a condition to handle as part of this patch, if someone wants to pursue it it can be reproduced with:
    // ant test  -Dtestcase=StreamingTest -Dtests.method=testAllValidExportTypes -Dtests.seed=10F13879D0D1D6AD -Dtests.slow=true -Dtests.locale=es-PA -Dtests.timezone=America/Bahia_Banderas -Dtests.asserts=true -Dtests.file.encoding=ISO-8859-1
    // You'll have to uncomment the if below to hit the null pointer exception.
    // This is such an unusual case (i.e. an empty index) that catching this concdition here is probably OK.
    // This came to light in the very artifical case of indexing a single doc to Cloud.
    int totalHits = 0;
    FixedBitSet[] sets = null;
    if (req.getContext().get("totalHits") != null) {
      totalHits = ((Integer)req.getContext().get("totalHits")).intValue();
      sets = (FixedBitSet[]) req.getContext().get("export");
      if (sets == null) {
        exception = new IOException(new SyntaxError("xport RankQuery is required for xsort: rq={!xport}"));
      }
     }

    int totalHits = th.intValue();
     SolrParams params = req.getParams();
     String fl = params.get("fl");
 
@@ -132,7 +145,7 @@ public class SortingResponseWriter implements QueryResponseWriter {
 
     try {
       fieldWriters = getFieldWriters(fields, req.getSearcher());
    }catch(Exception e) {
    } catch (Exception e) {
       exception = e;
     }
 
@@ -309,8 +322,21 @@ public class SortingResponseWriter implements QueryResponseWriter {
         } else {
           writers[i] = new StringFieldWriter(field, fieldType);
         }
      } else {
        throw new IOException("Export fields must either be one of the following types: int,float,long,double,string");
      } else if (fieldType instanceof TrieDateField) {
        if (multiValued) {
          writers[i] = new MultiFieldWriter(field, fieldType, false);
        } else {
          writers[i] = new DateFieldWriter(field);
        }
      } else if(fieldType instanceof BoolField) {
        if(multiValued) {
          writers[i] = new MultiFieldWriter(field, fieldType, true);
        } else {
          writers[i] = new BoolFieldWriter(field, fieldType);
        }
      }
      else {
        throw new IOException("Export fields must either be one of the following types: int,float,long,double,string,date,boolean");
       }
     }
     return writers;
@@ -362,8 +388,25 @@ public class SortingResponseWriter implements QueryResponseWriter {
         } else {
           sortValues[i] = new StringValue(vals, field, new IntAsc());
         }
      } else if (ft instanceof TrieDateField) {
        if (reverse) {
          sortValues[i] = new LongValue(field, new LongDesc());
        } else {
          sortValues[i] = new LongValue(field, new LongAsc());
        }
      } else if (ft instanceof BoolField) {
        // This is a bit of a hack, but since the boolean field stores ByteRefs, just like Strings
        // _and_ since "F" happens to sort before "T" (thus false sorts "less" than true)
        // we can just use the existing StringValue here.
        LeafReader reader = searcher.getLeafReader();
        SortedDocValues vals =  reader.getSortedDocValues(field);
        if(reverse) {
          sortValues[i] = new StringValue(vals, field, new IntDesc());
        } else {
          sortValues[i] = new StringValue(vals, field, new IntAsc());
        }
       } else {
        throw new IOException("Sort fields must be one of the following types: int,float,long,double,string");
        throw new IOException("Sort fields must be one of the following types: int,float,long,double,string,date,boolean");
       }
     }
 
@@ -1296,6 +1339,65 @@ public class SortingResponseWriter implements QueryResponseWriter {
     }
   }
 
  class DateFieldWriter extends FieldWriter {
    private String field;

    public DateFieldWriter(String field) {
      this.field = field;
    }

    public boolean write(int docId, LeafReader reader, Writer out, int fieldIndex) throws IOException {
      NumericDocValues vals = DocValues.getNumeric(reader, this.field);
      long val = vals.get(docId);

      if (fieldIndex > 0) {
        out.write(',');
      }
      out.write('"');
      out.write(this.field);
      out.write('"');
      out.write(':');
      out.write('"');
      writeStr(new Date(val).toInstant().toString(), out);
      out.write('"');
      return true;
    }
  }

  class BoolFieldWriter extends FieldWriter {
    private String field;
    private FieldType fieldType;
    private CharsRefBuilder cref = new CharsRefBuilder();

    public BoolFieldWriter(String field, FieldType fieldType) {
      this.field = field;
      this.fieldType = fieldType;
    }

    public boolean write(int docId, LeafReader reader, Writer out, int fieldIndex) throws IOException {
      SortedDocValues vals = DocValues.getSorted(reader, this.field);
      int ord = vals.getOrd(docId);
      if(ord == -1) {
        return false;
      }

      BytesRef ref = vals.lookupOrd(ord);
      fieldType.indexedToReadable(ref, cref);

      if (fieldIndex > 0) {
        out.write(',');
      }
      out.write('"');
      out.write(this.field);
      out.write('"');
      out.write(':');
      //out.write('"');
      writeStr(cref.toString(), out);
      //out.write('"');
      return true;
    }
  }

   class FloatFieldWriter extends FieldWriter {
     private String field;
 
@@ -1614,4 +1716,4 @@ public class SortingResponseWriter implements QueryResponseWriter {
       return (Object[]) heap;
     }
   }
}
\ No newline at end of file
}
diff --git a/solr/core/src/java/org/apache/solr/schema/BoolField.java b/solr/core/src/java/org/apache/solr/schema/BoolField.java
index 01161e72ada..1ecdb5961bd 100644
-- a/solr/core/src/java/org/apache/solr/schema/BoolField.java
++ b/solr/core/src/java/org/apache/solr/schema/BoolField.java
@@ -17,11 +17,16 @@
 package org.apache.solr.schema;
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 import java.util.Map;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
 import org.apache.lucene.index.DocValues;
 import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.LeafReaderContext;
@@ -123,7 +128,11 @@ public class BoolField extends PrimitiveFieldType {
 
   @Override
   public String toExternal(IndexableField f) {
    return indexedToReadable(f.stringValue());
    if (f.binaryValue() == null) {
      return null;
    }

    return indexedToReadable(f.binaryValue().utf8ToString());
   }
 
   @Override
@@ -144,7 +153,7 @@ public class BoolField extends PrimitiveFieldType {
 
   private static final CharsRef TRUE = new CharsRef("true");
   private static final CharsRef FALSE = new CharsRef("false");
  

   @Override
   public CharsRef indexedToReadable(BytesRef input, CharsRefBuilder charsRef) {
     if (input.length > 0 && input.bytes[input.offset] == 'T') {
@@ -169,6 +178,36 @@ public class BoolField extends PrimitiveFieldType {
   public Object unmarshalSortValue(Object value) {
     return unmarshalStringSortValue(value);
   }

  @Override
  public List<IndexableField> createFields(SchemaField field, Object value, float boost) {
    IndexableField fval = createField(field, value, boost);

    if (field.hasDocValues()) {
      IndexableField docval;
      final BytesRef bytes = new BytesRef(toInternal(value.toString()));
      if (field.multiValued()) {
        docval = new SortedSetDocValuesField(field.getName(), bytes);
      } else {
        docval = new SortedDocValuesField(field.getName(), bytes);
      }

      // Only create a list of we have 2 values...
      if (fval != null) {
        List<IndexableField> fields = new ArrayList<>(2);
        fields.add(fval);
        fields.add(docval);
        return fields;
      }

      fval = docval;
    }
    return Collections.singletonList(fval);
  }

  @Override
  public void checkSchemaField(final SchemaField field) {
  }
 }
 
 // TODO - this can be much more efficient - use FixedBitSet or Bits
diff --git a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
index 33d616ec808..213f7583540 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -112,6 +112,7 @@ import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestInfo;
 import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.BoolField;
 import org.apache.solr.schema.EnumField;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
@@ -841,8 +842,15 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
             break;
           case SORTED:
             SortedDocValues sdv = leafReader.getSortedDocValues(fieldName);
            if (sdv.getOrd(docid) >= 0) {
              doc.addField(fieldName, sdv.get(docid).utf8ToString());
            int ord = sdv.getOrd(docid);
            if (ord >= 0) {
              // Special handling for Boolean fields since they're stored as 'T' and 'F'.
              if (schemaField.getType() instanceof BoolField) {
                final BytesRef bRef = sdv.lookupOrd(ord);
                doc.addField(fieldName, schemaField.getType().toObject(schemaField, bRef));
              } else {
                doc.addField(fieldName, sdv.get(docid).utf8ToString());
              }
             }
             break;
           case SORTED_NUMERIC:
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml b/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
index 59fe99a5373..c7b7de8c6b3 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema-docValues.xml
@@ -61,7 +61,8 @@
   <field name="longdv" type="long" indexed="false" stored="false" docValues="true" default="4"/>
   <field name="datedv" type="date" indexed="false" stored="false" docValues="true" default="1995-12-31T23:59:59.999Z"/>
 
  <field name="stringdv" type="string" indexed="false" stored="false" docValues="true" default="solr"/>
  <field name="stringdv" type="string" indexed="false" stored="false" docValues="true" default="solr" />
  <field name="booldv" type="boolean" indexed="false" stored="false" docValues="true" default="true" />
 
   <field name="floatdvs" type="float" indexed="false" stored="false" docValues="true" default="1"/>
   <field name="intdvs" type="int" indexed="false" stored="false" docValues="true" default="2"/>
@@ -69,7 +70,7 @@
   <field name="longdvs" type="long" indexed="false" stored="false" docValues="true" default="4"/>
   <field name="datedvs" type="date" indexed="false" stored="false" docValues="true" default="1995-12-31T23:59:59.999Z"/>
   <field name="stringdvs" type="string" indexed="false" stored="false" docValues="true" default="solr"/>

  <field name="booldvs" type="boolean" indexed="false" stored="false" docValues="true" default="true"/>
 
   <uniqueKey>id</uniqueKey>
 
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMissing.xml b/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMissing.xml
index 22047494eb1..ac319e29ecc 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMissing.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMissing.xml
@@ -25,6 +25,7 @@
   <fieldType name="double" class="solr.TrieDoubleField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
   <fieldType name="date" class="solr.TrieDateField" precisionStep="0" omitNorms="true" positionIncrementGap="0"/>
   <fieldType name="string" class="solr.StrField"/>
  <fieldType name="boolean" class="solr.BoolField"/>
 
   <field name="id" type="string" required="true"/>
 
@@ -61,6 +62,13 @@
          sortMissingLast="true"/>
 
 
  <field name="booldv" type="boolean" indexed="false" stored="false" docValues="true"/>
  <field name="booldv_missingfirst" type="boolean" indexed="false" stored="false" docValues="true"
         sortMissingFirst="true"/>
  <field name="booldv_missinglast" type="boolean" indexed="false" stored="false" docValues="true"
         sortMissingLast="true"/>


   <dynamicField name="*_floatdv" type="float" indexed="false" stored="false" docValues="true"/>
   <dynamicField name="*_floatdv_missingfirst" type="float" indexed="false" stored="false" docValues="true"
                 sortMissingFirst="true"/>
@@ -97,6 +105,11 @@
   <dynamicField name="*_stringdv_missinglast" type="string" indexed="false" stored="false" docValues="true"
                 sortMissingLast="true"/>
 
  <dynamicField name="*_booldv" type="boolean" indexed="false" stored="false" docValues="true"/>
  <dynamicField name="*_booldv_missingfirst" type="boolean" indexed="false" stored="false" docValues="true"
                sortMissingFirst="true"/>
  <dynamicField name="*_booldv_missinglast" type="boolean" indexed="false" stored="false" docValues="true"
                sortMissingLast="true"/>
 
   <uniqueKey>id</uniqueKey>
 
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMulti.xml b/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMulti.xml
index 93a8588fcb7..81c78364369 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMulti.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema-docValuesMulti.xml
@@ -42,6 +42,7 @@
   <field name="datedv" type="date" indexed="false" stored="false" docValues="true" multiValued="true"/>
 
   <field name="stringdv" type="string" indexed="false" stored="false" docValues="true" multiValued="true"/>
  <field name="booldv" type="boolean" indexed="false" stored="false" docValues="true" multiValued="true"/>
 
   <uniqueKey>id</uniqueKey>
 
diff --git a/solr/core/src/test-files/solr/collection1/conf/schema15.xml b/solr/core/src/test-files/solr/collection1/conf/schema15.xml
index 82faa6a76cc..d545149f366 100644
-- a/solr/core/src/test-files/solr/collection1/conf/schema15.xml
++ b/solr/core/src/test-files/solr/collection1/conf/schema15.xml
@@ -452,6 +452,7 @@
   <field name="bind" type="boolean" indexed="true" stored="false"/>
   <field name="bsto" type="boolean" indexed="false" stored="true"/>
   <field name="bindsto" type="boolean" indexed="true" stored="true"/>
  <field name="bindstom" type="boolean" indexed="true" stored="true" multiValued="true"/>
   <field name="isto" type="int" indexed="false" stored="true"/>
   <field name="iind" type="int" indexed="true" stored="false"/>
   <field name="ssto" type="string" indexed="false" stored="true"/>
diff --git a/solr/core/src/test/org/apache/solr/schema/DocValuesMissingTest.java b/solr/core/src/test/org/apache/solr/schema/DocValuesMissingTest.java
index 847130b4cb4..04d38fdf565 100644
-- a/solr/core/src/test/org/apache/solr/schema/DocValuesMissingTest.java
++ b/solr/core/src/test/org/apache/solr/schema/DocValuesMissingTest.java
@@ -18,6 +18,7 @@ package org.apache.solr.schema;
 
 import org.apache.solr.SolrTestCaseJ4;
 import org.junit.BeforeClass;
import org.junit.Test;
 
 /**
  * Tests things like sorting on docvalues with missing values
@@ -123,239 +124,290 @@ public class DocValuesMissingTest extends SolrTestCaseJ4 {
   }
 
   /** float with default lucene sort (treats as 0) */
  @Test
   public void testFloatSort() throws Exception {
     checkSortMissingDefault("floatdv", "-1.3", "4.2");
   }
   /** dynamic float with default lucene sort (treats as 0) */
  @Test
   public void testDynFloatSort() throws Exception {
     checkSortMissingDefault("dyn_floatdv", "-1.3", "4.2");
   }
 
   /** float with sort missing always first */
  @Test
   public void testFloatSortMissingFirst() throws Exception {
     checkSortMissingFirst("floatdv_missingfirst", "-1.3", "4.2");
   }
   /** dynamic float with sort missing always first */
  @Test
   public void testDynFloatSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_floatdv_missingfirst", "-1.3", "4.2");
   }
 
   /** float with sort missing always last */
  @Test
   public void testFloatSortMissingLast() throws Exception {
     checkSortMissingLast("floatdv_missinglast", "-1.3", "4.2");
   }
   /** dynamic float with sort missing always last */
  @Test
   public void testDynFloatSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_floatdv_missinglast", "-1.3", "4.2");
   }
   
   /** float function query based on missing */
  @Test
   public void testFloatMissingFunction() throws Exception {
     checkSortMissingFunction("floatdv", "-1.3", "4.2");
   }
   /** dyanmic float function query based on missing */
  @Test
   public void testDynFloatMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_floatdv", "-1.3", "4.2");
   }
   
   /** float missing facet count */
  @Test
   public void testFloatMissingFacet() throws Exception {
     checkSortMissingFacet("floatdv", "-1.3", "4.2");
   }
   /** dynamic float missing facet count */
  @Test
   public void testDynFloatMissingFacet() throws Exception {
     checkSortMissingFacet("dyn_floatdv", "-1.3", "4.2");
   }
 
   /** int with default lucene sort (treats as 0) */
  @Test
   public void testIntSort() throws Exception {
     checkSortMissingDefault("intdv", "-1", "4");
   }
   /** dynamic int with default lucene sort (treats as 0) */
  @Test
   public void testDynIntSort() throws Exception {
     checkSortMissingDefault("dyn_intdv", "-1", "4");
   }
   
   /** int with sort missing always first */
  @Test
   public void testIntSortMissingFirst() throws Exception {
     checkSortMissingFirst("intdv_missingfirst", "-1", "4");
   }
   /** dynamic int with sort missing always first */
  @Test
   public void testDynIntSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_intdv_missingfirst", "-1", "4");
   }
   
   /** int with sort missing always last */
  @Test
   public void testIntSortMissingLast() throws Exception {
     checkSortMissingLast("intdv_missinglast", "-1", "4");
   }
   /** dynamic int with sort missing always last */
  @Test
   public void testDynIntSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_intdv_missinglast", "-1", "4");
   }
   
   /** int function query based on missing */
  @Test
   public void testIntMissingFunction() throws Exception {
     checkSortMissingFunction("intdv", "-1", "4");
   }
   /** dynamic int function query based on missing */
  @Test
   public void testDynIntMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_intdv", "-1", "4");
   }
   
   /** int missing facet count */
  @Test
   public void testIntMissingFacet() throws Exception {
     checkSortMissingFacet("intdv", "-1", "4");
   }
   /** dynamic int missing facet count */
  @Test
   public void testDynIntMissingFacet() throws Exception {
     checkSortMissingFacet("dyn_intdv", "-1", "4");
   }
   
   /** double with default lucene sort (treats as 0) */
  @Test
   public void testDoubleSort() throws Exception {
     checkSortMissingDefault("doubledv", "-1.3", "4.2");
   }
   /** dynamic double with default lucene sort (treats as 0) */
  @Test
   public void testDynDoubleSort() throws Exception {
     checkSortMissingDefault("dyn_doubledv", "-1.3", "4.2");
   }
   
   /** double with sort missing always first */
  @Test
   public void testDoubleSortMissingFirst() throws Exception {
     checkSortMissingFirst("doubledv_missingfirst", "-1.3", "4.2");
   }
   /** dynamic double with sort missing always first */
  @Test
   public void testDynDoubleSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_doubledv_missingfirst", "-1.3", "4.2");
   }
 
   /** double with sort missing always last */
  @Test
   public void testDoubleSortMissingLast() throws Exception {
     checkSortMissingLast("doubledv_missinglast", "-1.3", "4.2");
   }
   /** dynamic double with sort missing always last */
  @Test
   public void testDynDoubleSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_doubledv_missinglast", "-1.3", "4.2");
   }
   
   /** double function query based on missing */
  @Test
   public void testDoubleMissingFunction() throws Exception {
     checkSortMissingFunction("doubledv", "-1.3", "4.2");
   }
   /** dyanmic double function query based on missing */
  @Test
   public void testDynDoubleMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_doubledv", "-1.3", "4.2");
   }
   
   /** double missing facet count */
  @Test
   public void testDoubleMissingFacet() throws Exception {
     checkSortMissingFacet("doubledv", "-1.3", "4.2");
   }
   /** dynamic double missing facet count */
  @Test
   public void testDynDoubleMissingFacet() throws Exception {
     checkSortMissingFacet("dyn_doubledv", "-1.3", "4.2");
   }
   
   /** long with default lucene sort (treats as 0) */
  @Test
   public void testLongSort() throws Exception {
     checkSortMissingDefault("longdv", "-1", "4");
   }
   /** dynamic long with default lucene sort (treats as 0) */
  @Test
   public void testDynLongSort() throws Exception {
     checkSortMissingDefault("dyn_longdv", "-1", "4");
   }
 
   /** long with sort missing always first */
  @Test
   public void testLongSortMissingFirst() throws Exception {
     checkSortMissingFirst("longdv_missingfirst", "-1", "4");
   }
   /** dynamic long with sort missing always first */
  @Test
   public void testDynLongSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_longdv_missingfirst", "-1", "4");
   }
 
   /** long with sort missing always last */
  @Test
   public void testLongSortMissingLast() throws Exception {
     checkSortMissingLast("longdv_missinglast", "-1", "4");
   }
   /** dynamic long with sort missing always last */
  @Test
   public void testDynLongSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_longdv_missinglast", "-1", "4");
   }
   
   /** long function query based on missing */
  @Test
   public void testLongMissingFunction() throws Exception {
     checkSortMissingFunction("longdv", "-1", "4");
   }
   /** dynamic long function query based on missing */
  @Test
   public void testDynLongMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_longdv", "-1", "4");
   }
   
   /** long missing facet count */
  @Test
   public void testLongMissingFacet() throws Exception {
     checkSortMissingFacet("longdv", "-1", "4");
   }
   /** dynamic long missing facet count */
  @Test
   public void testDynLongMissingFacet() throws Exception {
     checkSortMissingFacet("dyn_longdv", "-1", "4");
   }
   
   /** date with default lucene sort (treats as 1970) */
  @Test
   public void testDateSort() throws Exception {
     checkSortMissingDefault("datedv", "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   /** dynamic date with default lucene sort (treats as 1970) */
  @Test
   public void testDynDateSort() throws Exception {
     checkSortMissingDefault("dyn_datedv", "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   
   /** date with sort missing always first */
  @Test
   public void testDateSortMissingFirst() throws Exception {
     checkSortMissingFirst("datedv_missingfirst", 
                           "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   /** dynamic date with sort missing always first */
  @Test
   public void testDynDateSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_datedv_missingfirst", 
                           "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   
   /** date with sort missing always last */
  @Test
   public void testDateSortMissingLast() throws Exception {
     checkSortMissingLast("datedv_missinglast", 
                           "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   /** dynamic date with sort missing always last */
  @Test
   public void testDynDateSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_datedv_missinglast", 
                          "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   
   /** date function query based on missing */
  @Test
   public void testDateMissingFunction() throws Exception {
     checkSortMissingFunction("datedv", 
                              "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   /** dynamic date function query based on missing */
  @Test
   public void testDynDateMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_datedv", 
                              "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   
   /** date missing facet count */
  @Test
   public void testDateMissingFacet() throws Exception {
     checkSortMissingFacet("datedv", 
                           "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   /** dynamic date missing facet count */
  @Test
   public void testDynDateMissingFacet() throws Exception {
     checkSortMissingFacet("dyn_datedv", 
                           "1900-12-31T23:59:59.999Z", "2005-12-31T23:59:59.999Z");
   }
   
   /** string (and dynamic string) with default lucene sort (treats as "") */
  @Test
   public void testStringSort() throws Exception {
 
     // note: cant use checkSortMissingDefault because 
@@ -377,33 +429,40 @@ public class DocValuesMissingTest extends SolrTestCaseJ4 {
   }
   
   /** string with sort missing always first */
  @Test
   public void testStringSortMissingFirst() throws Exception {
     checkSortMissingFirst("stringdv_missingfirst", "a", "z");
   }
   /** dynamic string with sort missing always first */
  @Test
   public void testDynStringSortMissingFirst() throws Exception {
     checkSortMissingFirst("dyn_stringdv_missingfirst", "a", "z");
   }
   
   /** string with sort missing always last */
  @Test
   public void testStringSortMissingLast() throws Exception {
     checkSortMissingLast("stringdv_missinglast", "a", "z");
   }
   /** dynamic string with sort missing always last */
  @Test
   public void testDynStringSortMissingLast() throws Exception {
     checkSortMissingLast("dyn_stringdv_missinglast", "a", "z");
   }
 
   /** string function query based on missing */
  @Test
   public void testStringMissingFunction() throws Exception {
     checkSortMissingFunction("stringdv", "a", "z");
   }
   /** dynamic string function query based on missing */
  @Test
   public void testDynStringMissingFunction() throws Exception {
     checkSortMissingFunction("dyn_stringdv", "a", "z");
   }
   
   /** string missing facet count */
  @Test
   public void testStringMissingFacet() throws Exception {
     assertU(adoc("id", "0")); // missing
     assertU(adoc("id", "1")); // missing
@@ -415,4 +474,74 @@ public class DocValuesMissingTest extends SolrTestCaseJ4 {
         "//lst[@name='facet_fields']/lst[@name='stringdv']/int[@name='z'][.=1]",
         "//lst[@name='facet_fields']/lst[@name='stringdv']/int[.=2]");
   }

  /** bool (and dynamic bool) with default lucene sort (treats as "") */
  @Test
  public void testBoolSort() throws Exception {
    // note: cant use checkSortMissingDefault because 
    // nothing sorts lower then the default of "" and
    // bool fields are, at root, string fields.
    for (String field : new String[] {"booldv","dyn_booldv"}) {
      assertU(adoc("id", "0")); // missing
      assertU(adoc("id", "1", field, "false"));
      assertU(adoc("id", "2", field, "true"));
      assertU(commit());
      assertQ(req("q", "*:*", "sort", field+" asc"),
          "//result/doc[1]/str[@name='id'][.=0]",
          "//result/doc[2]/str[@name='id'][.=1]",
          "//result/doc[3]/str[@name='id'][.=2]");
      assertQ(req("q", "*:*", "sort", field+" desc"),
          "//result/doc[1]/str[@name='id'][.=2]",
          "//result/doc[2]/str[@name='id'][.=1]",
          "//result/doc[3]/str[@name='id'][.=0]");
    }
  }

  /** bool with sort missing always first */
  @Test
  public void testBoolSortMissingFirst() throws Exception {
    checkSortMissingFirst("booldv_missingfirst", "false", "ture");
  }
  /** dynamic bool with sort missing always first */
  @Test
  public void testDynBoolSortMissingFirst() throws Exception {
    checkSortMissingFirst("dyn_booldv_missingfirst", "false", "true");
  }

  /** bool with sort missing always last */
  @Test
  public void testBoolSortMissingLast() throws Exception {
    checkSortMissingLast("booldv_missinglast", "false", "true");
  }
  /** dynamic bool with sort missing always last */
  @Test
  public void testDynBoolSortMissingLast() throws Exception {
    checkSortMissingLast("dyn_booldv_missinglast", "false", "true");
  }

  /** bool function query based on missing */
  @Test
  public void testBoolMissingFunction() throws Exception {
    checkSortMissingFunction("booldv", "false", "true");
  }
  /** dynamic bool function query based on missing */
  @Test
  public void testDynBoolMissingFunction() throws Exception {
    checkSortMissingFunction("dyn_booldv", "false", "true");
  }

  /** bool missing facet count */
  @Test
  public void testBoolMissingFacet() throws Exception {
    assertU(adoc("id", "0")); // missing
    assertU(adoc("id", "1")); // missing
    assertU(adoc("id", "2", "booldv", "false"));
    assertU(adoc("id", "3", "booldv", "true"));
    assertU(commit());
    assertQ(req("q", "*:*", "facet", "true", "facet.field", "booldv", "facet.mincount", "1", "facet.missing", "true"),
        "//lst[@name='facet_fields']/lst[@name='booldv']/int[@name='false'][.=1]",
        "//lst[@name='facet_fields']/lst[@name='booldv']/int[@name='true'][.=1]",
        "//lst[@name='facet_fields']/lst[@name='booldv']/int[.=2]");
  }

 }
diff --git a/solr/core/src/test/org/apache/solr/schema/DocValuesMultiTest.java b/solr/core/src/test/org/apache/solr/schema/DocValuesMultiTest.java
index 7b4a5db3ea8..90c8b7363ff 100644
-- a/solr/core/src/test/org/apache/solr/schema/DocValuesMultiTest.java
++ b/solr/core/src/test/org/apache/solr/schema/DocValuesMultiTest.java
@@ -25,6 +25,7 @@ import org.apache.solr.core.SolrCore;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.util.RefCounted;
 import org.junit.BeforeClass;
import org.junit.Test;
 
 import java.io.IOException;
 
@@ -36,7 +37,7 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
     
     // sanity check our schema meets our expectations
     final IndexSchema schema = h.getCore().getLatestSchema();
    for (String f : new String[] {"floatdv", "intdv", "doubledv", "longdv", "datedv", "stringdv"}) {
    for (String f : new String[] {"floatdv", "intdv", "doubledv", "longdv", "datedv", "stringdv", "booldv"}) {
       final SchemaField sf = schema.getField(f);
       assertTrue(f + " is not multiValued, test is useless, who changed the schema?",
                  sf.multiValued());
@@ -52,8 +53,11 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
     assertU(delQ("*:*"));
   }
 
  @Test
   public void testDocValues() throws IOException {
    assertU(adoc("id", "1", "floatdv", "4.5", "intdv", "-1", "intdv", "3", "stringdv", "value1", "stringdv", "value2"));
    assertU(adoc("id", "1", "floatdv", "4.5", "intdv", "-1", "intdv", "3",
        "stringdv", "value1", "stringdv", "value2",
        "booldv", "false", "booldv", "true"));
     assertU(commit());
     try (SolrCore core = h.getCoreInc()) {
       final RefCounted<SolrIndexSearcher> searcherRef = core.openNewSearcher(true, true);
@@ -63,6 +67,7 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
         assertEquals(1, reader.numDocs());
         final FieldInfos infos = reader.getFieldInfos();
         assertEquals(DocValuesType.SORTED_SET, infos.fieldInfo("stringdv").getDocValuesType());
        assertEquals(DocValuesType.SORTED_SET, infos.fieldInfo("booldv").getDocValuesType());
         assertEquals(DocValuesType.SORTED_SET, infos.fieldInfo("floatdv").getDocValuesType());
         assertEquals(DocValuesType.SORTED_SET, infos.fieldInfo("intdv").getDocValuesType());
 
@@ -71,6 +76,14 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
         assertEquals(0, dv.nextOrd());
         assertEquals(1, dv.nextOrd());
         assertEquals(SortedSetDocValues.NO_MORE_ORDS, dv.nextOrd());

        dv = reader.getSortedSetDocValues("booldv");
        dv.setDocument(0);
        assertEquals(0, dv.nextOrd());
        assertEquals(1, dv.nextOrd());
        assertEquals(SortedSetDocValues.NO_MORE_ORDS, dv.nextOrd());


       } finally {
         searcherRef.decref();
       }
@@ -80,6 +93,7 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
   /** Tests the ability to do basic queries (without scoring, just match-only) on
    *  string docvalues fields that are not inverted (indexed "forward" only)
    */
  @Test
   public void testStringDocValuesMatch() throws Exception {
     assertU(adoc("id", "1", "stringdv", "b"));
     assertU(adoc("id", "2", "stringdv", "a"));
@@ -123,10 +137,49 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
         "//result/doc[1]/str[@name='id'][.=4]"
     );
   }
  

  /** Tests the ability to do basic queries (without scoring, just match-only) on
   *  boolean docvalues fields that are not inverted (indexed "forward" only)
   */
  @Test
  public void testBoolDocValuesMatch() throws Exception {
    assertU(adoc("id", "1", "booldv", "true"));
    assertU(adoc("id", "2", "booldv", "false"));
    assertU(adoc("id", "3", "booldv", "true"));
    assertU(adoc("id", "4", "booldv", "false"));
    assertU(adoc("id", "5", "booldv", "true", "booldv", "false"));
    assertU(commit());

    // string: termquery
    assertQ(req("q", "booldv:true", "sort", "id asc"),
        "//*[@numFound='3']",
        "//result/doc[1]/str[@name='id'][.=1]",
        "//result/doc[2]/str[@name='id'][.=3]",
        "//result/doc[3]/str[@name='id'][.=5]"
    );

    // boolean: range query, 
    assertQ(req("q", "booldv:[false TO false]", "sort", "id asc"),
        "//*[@numFound='3']",
        "//result/doc[1]/str[@name='id'][.=2]",
        "//result/doc[2]/str[@name='id'][.=4]",
        "//result/doc[3]/str[@name='id'][.=5]");


    assertQ(req("q", "*:*", "sort", "id asc", "rows", "10", "fl", "booldv"),
        "//result/doc[1]/arr[@name='booldv']/bool[1][.='true']",
        "//result/doc[2]/arr[@name='booldv']/bool[1][.='false']",
        "//result/doc[3]/arr[@name='booldv']/bool[1][.='true']",
        "//result/doc[4]/arr[@name='booldv']/bool[1][.='false']",
        "//result/doc[5]/arr[@name='booldv']/bool[1][.='false']",
        "//result/doc[5]/arr[@name='booldv']/bool[2][.='true']"
    );

  }
   /** Tests the ability to do basic queries (without scoring, just match-only) on
    *  float docvalues fields that are not inverted (indexed "forward" only)
    */
  @Test
   public void testFloatDocValuesMatch() throws Exception {
     assertU(adoc("id", "1", "floatdv", "2"));
     assertU(adoc("id", "2", "floatdv", "-5"));
@@ -166,6 +219,7 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
   /** Tests the ability to do basic queries (without scoring, just match-only) on
    *  double docvalues fields that are not inverted (indexed "forward" only)
    */
  @Test
   public void testDoubleDocValuesMatch() throws Exception {
     assertU(adoc("id", "1", "doubledv", "2"));
     assertU(adoc("id", "2", "doubledv", "-5"));
@@ -201,17 +255,34 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
             "//result/doc[1]/str[@name='id'][.=2]"
             );
   }
  
  @Test
   public void testDocValuesFacetingSimple() {
     // this is the random test verbatim from DocValuesTest, so it populates with the default values defined in its schema.
     for (int i = 0; i < 50; ++i) {
      assertU(adoc("id", "" + i, "floatdv", "1", "intdv", "2", "doubledv", "3", "longdv", "4", "datedv", "1995-12-31T23:59:59.999Z"));
      assertU(adoc("id", "" + i, "floatdv", "1", "intdv", "2", "doubledv", "3", "longdv", "4", 
          "datedv", "1995-12-31T23:59:59.999Z",
          "stringdv", "abc", "booldv", "true"));
     }
     for (int i = 0; i < 50; ++i) {
       if (rarely()) {
         assertU(commit()); // to have several segments
       }
      assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i, "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i));
      switch (i % 3) {
        case 0:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i, 
              "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i, "booldv", "true", "booldv", "true"));
          break;
        case 1:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i,
              "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i, "booldv", "false", "booldv", "false"));
          break;
        case 2:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i,
              "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i, "booldv", "true", "booldv", "false"));
          break;
      }


     }
     assertU(commit());
     assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "longdv", "facet.sort", "count", "facet.limit", "1"),
@@ -248,5 +319,25 @@ public class DocValuesMultiTest extends SolrTestCaseJ4 {
         "//lst[@name='datedv']/int[@name='1900-12-31T23:59:59.999Z'][.='1']");
     assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "datedv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
         "//lst[@name='datedv']/int[@name='1933-12-31T23:59:59.999Z'][.='1']");


    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "stringdv", "facet.sort", "count", "facet.limit", "1"),
        "//lst[@name='stringdv']/int[@name='abc'][.='50']");
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "stringdv", "facet.sort", "count", "facet.offset", "1", "facet.limit", "-1", "facet.mincount", "1"),
        "//lst[@name='stringdv']/int[@name='abc1'][.='1']",
        "//lst[@name='stringdv']/int[@name='abc13'][.='1']",
        "//lst[@name='stringdv']/int[@name='abc19'][.='1']",
        "//lst[@name='stringdv']/int[@name='abc49'][.='1']"
        );
    
    // Even though offseting by 33, the sort order is abc1 abc11....abc2 so it throws the position in the return list off.
    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "stringdv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
        "//lst[@name='stringdv']/int[@name='abc38'][.='1']");


    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "booldv", "facet.sort", "count"),
        "//lst[@name='booldv']/int[@name='true'][.='83']",
        "//lst[@name='booldv']/int[@name='false'][.='33']");

   }
 }
diff --git a/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java b/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java
index b4248da4b1d..d59d3267843 100644
-- a/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java
++ b/solr/core/src/test/org/apache/solr/schema/DocValuesTest.java
@@ -27,6 +27,7 @@ import org.apache.solr.core.SolrCore;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.util.RefCounted;
 import org.junit.BeforeClass;
import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -49,7 +50,7 @@ public class DocValuesTest extends SolrTestCaseJ4 {
 
     // sanity check our schema meets our expectations
     final IndexSchema schema = h.getCore().getLatestSchema();
    for (String f : new String[] {"floatdv", "intdv", "doubledv", "longdv", "datedv", "stringdv"}) {
    for (String f : new String[] {"floatdv", "intdv", "doubledv", "longdv", "datedv", "stringdv", "booldv"}) {
       final SchemaField sf = schema.getField(f);
       assertFalse(f + " is multiValued, test is useless, who changed the schema?",
                   sf.multiValued());
@@ -65,6 +66,7 @@ public class DocValuesTest extends SolrTestCaseJ4 {
     assertU(delQ("*:*"));
   }
 
  @Test
   public void testDocValues() throws IOException {
     assertU(adoc("id", "1"));
     assertU(commit());
@@ -80,17 +82,21 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("doubledv").getDocValuesType());
         assertEquals(DocValuesType.NUMERIC, infos.fieldInfo("longdv").getDocValuesType());
         assertEquals(DocValuesType.SORTED, infos.fieldInfo("stringdv").getDocValuesType());
        assertEquals(DocValuesType.SORTED, infos.fieldInfo("booldv").getDocValuesType());
 
         assertEquals((long) Float.floatToIntBits(1), reader.getNumericDocValues("floatdv").get(0));
         assertEquals(2L, reader.getNumericDocValues("intdv").get(0));
         assertEquals(Double.doubleToLongBits(3), reader.getNumericDocValues("doubledv").get(0));
         assertEquals(4L, reader.getNumericDocValues("longdv").get(0));
        assertEquals("solr", reader.getSortedDocValues("stringdv").get(0).utf8ToString());
        assertEquals("T", reader.getSortedDocValues("booldv").get(0).utf8ToString());
 
         final IndexSchema schema = core.getLatestSchema();
         final SchemaField floatDv = schema.getField("floatdv");
         final SchemaField intDv = schema.getField("intdv");
         final SchemaField doubleDv = schema.getField("doubledv");
         final SchemaField longDv = schema.getField("longdv");
        final SchemaField boolDv = schema.getField("booldv");
 
         FunctionValues values = floatDv.getType().getValueSource(floatDv, null).getValues(null, searcher.getLeafReader().leaves().get(0));
         assertEquals(1f, values.floatVal(0), 0f);
@@ -104,6 +110,10 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         values = longDv.getType().getValueSource(longDv, null).getValues(null, searcher.getLeafReader().leaves().get(0));
         assertEquals(4L, values.longVal(0));
         assertEquals(4L, values.objectVal(0));
        
        values = boolDv.getType().getValueSource(boolDv, null).getValues(null, searcher.getLeafReader().leaves().get(0));
        assertEquals("true", values.strVal(0));
        assertEquals(true, values.objectVal(0));
 
         // check reversibility of created fields
         tstToObj(schema.getField("floatdv"), -1.5f);
@@ -118,6 +128,8 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         tstToObj(schema.getField("datedvs"), new Date(1000));
         tstToObj(schema.getField("stringdv"), "foo");
         tstToObj(schema.getField("stringdvs"), "foo");
        tstToObj(schema.getField("booldv"), true);
        tstToObj(schema.getField("booldvs"), true);
 
       } finally {
         searcherRef.decref();
@@ -132,10 +144,11 @@ public class DocValuesTest extends SolrTestCaseJ4 {
     }
   }
 
  @Test
   public void testDocValuesSorting() {
    assertU(adoc("id", "1", "floatdv", "2", "intdv", "3", "doubledv", "4", "longdv", "5", "datedv", "1995-12-31T23:59:59.999Z", "stringdv", "b"));
    assertU(adoc("id", "2", "floatdv", "5", "intdv", "4", "doubledv", "3", "longdv", "2", "datedv", "1997-12-31T23:59:59.999Z", "stringdv", "a"));
    assertU(adoc("id", "3", "floatdv", "3", "intdv", "1", "doubledv", "2", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "c"));
    assertU(adoc("id", "1", "floatdv", "2", "intdv", "3", "doubledv", "4", "longdv", "5", "datedv", "1995-12-31T23:59:59.999Z", "stringdv", "b", "booldv", "true"));
    assertU(adoc("id", "2", "floatdv", "5", "intdv", "4", "doubledv", "3", "longdv", "2", "datedv", "1997-12-31T23:59:59.999Z", "stringdv", "a", "booldv", "false"));
    assertU(adoc("id", "3", "floatdv", "3", "intdv", "1", "doubledv", "2", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "c", "booldv", "true"));
     assertU(adoc("id", "4"));
     assertU(commit());
     assertQ(req("q", "*:*", "sort", "floatdv desc", "rows", "1", "fl", "id"),
@@ -146,8 +159,10 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         "//int[@name='id'][.='1']");
     assertQ(req("q", "*:*", "sort", "longdv desc", "rows", "1", "fl", "id"),
         "//int[@name='id'][.='1']");
    assertQ(req("q", "*:*", "sort", "datedv desc", "rows", "1", "fl", "id"),
        "//int[@name='id'][.='2']");
    assertQ(req("q", "*:*", "sort", "datedv desc", "rows", "1", "fl", "id,datedv"),
        "//int[@name='id'][.='2']",
        "//result/doc[1]/date[@name='datedv'][.='1997-12-31T23:59:59.999Z']"
        );
     assertQ(req("q", "*:*", "sort", "stringdv desc", "rows", "1", "fl", "id"),
         "//int[@name='id'][.='4']");
     assertQ(req("q", "*:*", "sort", "floatdv asc", "rows", "1", "fl", "id"),
@@ -162,8 +177,17 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         "//int[@name='id'][.='1']");
     assertQ(req("q", "*:*", "sort", "stringdv asc", "rows", "1", "fl", "id"),
         "//int[@name='id'][.='2']");
    assertQ(req("q", "*:*", "sort", "booldv asc", "rows", "10", "fl", "booldv,stringdv"),
        "//result/doc[1]/bool[@name='booldv'][.='false']",
        "//result/doc[2]/bool[@name='booldv'][.='true']",
        "//result/doc[3]/bool[@name='booldv'][.='true']",
        "//result/doc[4]/bool[@name='booldv'][.='true']"
        );
        

   }
   
  @Test
   public void testDocValuesSorting2() {
     assertU(adoc("id", "1", "doubledv", "12"));
     assertU(adoc("id", "2", "doubledv", "50.567"));
@@ -184,6 +208,7 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         );
   }
 
  @Test
   public void testDocValuesFaceting() {
     for (int i = 0; i < 50; ++i) {
       assertU(adoc("id", "" + i));
@@ -192,7 +217,20 @@ public class DocValuesTest extends SolrTestCaseJ4 {
       if (rarely()) {
         assertU(commit()); // to have several segments
       }
      assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i, "datedv", (1900+i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i));
      switch (i % 3) {
        case 0:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i,
              "datedv", (1900 + i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i, "booldv", "false"));
          break;
        case 1:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i,
              "datedv", (1900 + i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i, "booldv", "true"));
          break;
        case 2:
          assertU(adoc("id", "1000" + i, "floatdv", "" + i, "intdv", "" + i, "doubledv", "" + i, "longdv", "" + i,
              "datedv", (1900 + i) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i));
          break;
      }
     }
     assertU(commit());
     assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "longdv", "facet.sort", "count", "facet.limit", "1"),
@@ -229,8 +267,20 @@ public class DocValuesTest extends SolrTestCaseJ4 {
         "//lst[@name='datedv']/int[@name='1900-12-31T23:59:59.999Z'][.='1']");
     assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "datedv", "facet.sort", "index", "facet.offset", "33", "facet.limit", "1", "facet.mincount", "1"),
         "//lst[@name='datedv']/int[@name='1933-12-31T23:59:59.999Z'][.='1']");

    assertQ(req("q", "booldv:true"),
        "//*[@numFound='83']");

    assertQ(req("q", "booldv:false"),
        "//*[@numFound='17']");

    assertQ(req("q", "*:*", "facet", "true", "rows", "0", "facet.field", "booldv", "facet.sort", "index", "facet.mincount", "1"),
        "//lst[@name='booldv']/int[@name='false'][.='17']",
        "//lst[@name='booldv']/int[@name='true'][.='83']");

   }
 
  @Test
   public void testDocValuesStats() {
     for (int i = 0; i < 50; ++i) {
       assertU(adoc("id", "1000" + i, "floatdv", "" + i%2, "intdv", "" + i%3, "doubledv", "" + i%4, "longdv", "" + i%5, "datedv", (1900+i%6) + "-12-31T23:59:59.999Z", "stringdv", "abc" + i%7));
@@ -293,10 +343,11 @@ public class DocValuesTest extends SolrTestCaseJ4 {
   /** Tests the ability to do basic queries (without scoring, just match-only) on
    *  docvalues fields that are not inverted (indexed "forward" only)
    */
  @Test
   public void testDocValuesMatch() throws Exception {
    assertU(adoc("id", "1", "floatdv", "2", "intdv", "3", "doubledv", "3.1", "longdv", "5", "datedv", "1995-12-31T23:59:59.999Z", "stringdv", "b"));
    assertU(adoc("id", "2", "floatdv", "-5", "intdv", "4", "doubledv", "-4.3", "longdv", "2", "datedv", "1997-12-31T23:59:59.999Z", "stringdv", "a"));
    assertU(adoc("id", "3", "floatdv", "3", "intdv", "1", "doubledv", "2.1", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "c"));
    assertU(adoc("id", "1", "floatdv", "2", "intdv", "3", "doubledv", "3.1", "longdv", "5", "datedv", "1995-12-31T23:59:59.999Z", "stringdv", "b", "booldv", "false"));
    assertU(adoc("id", "2", "floatdv", "-5", "intdv", "4", "doubledv", "-4.3", "longdv", "2", "datedv", "1997-12-31T23:59:59.999Z", "stringdv", "a", "booldv", "true"));
    assertU(adoc("id", "3", "floatdv", "3", "intdv", "1", "doubledv", "2.1", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "c", "booldv", "false"));
     assertU(adoc("id", "4", "floatdv", "3", "intdv", "-1", "doubledv", "1.5", "longdv", "1", "datedv", "1996-12-31T23:59:59.999Z", "stringdv", "car"));
     assertU(commit());
 
@@ -439,8 +490,23 @@ public class DocValuesTest extends SolrTestCaseJ4 {
             "//result/doc[1]/int[@name='id'][.=2]",
             "//result/doc[2]/int[@name='id'][.=4]"
             );
    // boolean basic queries:

    assertQ(req("q", "booldv:false", "sort", "id asc"),
        "//*[@numFound='2']",
        "//result/doc[1]/int[@name='id'][.=1]",
        "//result/doc[2]/int[@name='id'][.=3]"
    );

    assertQ(req("q", "booldv:true", "sort", "id asc"),
        "//*[@numFound='2']",
        "//result/doc[1]/int[@name='id'][.=2]",
        "//result/doc[2]/int[@name='id'][.=4]"
    );

   }
 
  @Test
   public void testFloatAndDoubleRangeQueryRandom() throws Exception {
 
     String fieldName[] = new String[] {"floatdv", "doubledv"};
@@ -556,6 +622,7 @@ public class DocValuesTest extends SolrTestCaseJ4 {
     }
   }
 
  @Test
   public void testFloatAndDoubleRangeQuery() throws Exception {
     String fieldName[] = new String[] {"floatdv", "doubledv"};
     String largestNegative[] = new String[] {String.valueOf(0f-Float.MIN_NORMAL), String.valueOf(0f-Double.MIN_NORMAL)};
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/io/Tuple.java b/solr/solrj/src/java/org/apache/solr/client/solrj/io/Tuple.java
index dee19abea60..2f646519f63 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/io/Tuple.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/io/Tuple.java
@@ -16,10 +16,12 @@
  */
 package org.apache.solr.client.solrj.io;
 
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.List;
import java.util.Iterator;
 
 
 /**
@@ -87,6 +89,53 @@ public class Tuple implements Cloneable {
     }
   }
 
  // Convenience method since Booleans can be pased around as Strings.
  public Boolean getBool(Object key) {
    Object o = this.fields.get(key);

    if (o == null) {
      return null;
    }

    if (o instanceof Boolean) {
      return (Boolean) o;
    } else {
      //Attempt to parse the Boolean
      return Boolean.parseBoolean(o.toString());
    }
  }

  public List<Boolean> getBools(Object key) {
    return (List<Boolean>) this.fields.get(key);
  }

  // Convenience methods since the dates are actually shipped around as Strings.
  public Date getDate(Object key) {
    Object o = this.fields.get(key);

    if (o == null) {
      return null;
    }

    if (o instanceof Date) {
      return (Date) o;
    } else {
      //Attempt to parse the Date from a String
      return new Date(Instant.parse(o.toString()).toEpochMilli());
    }
  }

  public List<Date> getDates(Object key) {
    List<String> vals = (List<String>) this.fields.get(key);
    if (vals == null) return null;
    
    List<Date> ret = new ArrayList<>();
    for (String dateStr : (List<String>) this.fields.get(key)) {
      ret.add(new Date(Instant.parse(dateStr).toEpochMilli()));
    }
    return ret;
  }

   public Double getDouble(Object key) {
     Object o = this.fields.get(key);
 
@@ -144,4 +193,4 @@ public class Tuple implements Cloneable {
   public void merge(Tuple other){
     fields.putAll(other.getMap());
   }
}
\ No newline at end of file
}
diff --git a/solr/solrj/src/test-files/solrj/solr/configsets/streaming/conf/schema.xml b/solr/solrj/src/test-files/solrj/solr/configsets/streaming/conf/schema.xml
index c10f6cfbf0f..34ecdcbb76b 100644
-- a/solr/solrj/src/test-files/solrj/solr/configsets/streaming/conf/schema.xml
++ b/solr/solrj/src/test-files/solrj/solr/configsets/streaming/conf/schema.xml
@@ -395,8 +395,19 @@
     <field name="f_multi" type="float" indexed="true" stored="true" docValues="true" multiValued="true"/>
     <field name="l_multi" type="long" indexed="true" stored="true" docValues="true" multiValued="true"/>
     <field name="d_multi" type="double" indexed="true" stored="true" docValues="true" multiValued="true"/>
    <field name="dt_multi" type="date" indexed="true" stored="true" docValues="true" multiValued="true"/>
    <field name="b_multi" type="boolean" indexed="true" stored="true" docValues="true" multiValued="true"/>
 
    <field name="uuid" type="uuid" stored="true" />
    <field name="s_sing" type="string" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="i_sing" type="int" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="f_sing" type="float" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="l_sing" type="long" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="d_sing" type="double" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="dt_sing" type="tdate" indexed="true" stored="true" docValues="true" multiValued="false"/>
    <field name="b_sing" type="boolean" indexed="true" stored="true" docValues="true" multiValued="false"/>


  <field name="uuid" type="uuid" stored="true" />
     <field name="name" type="nametext" indexed="true" stored="true"/>
     <field name="text" type="text" indexed="true" stored="false"/>
     <field name="subject" type="text" indexed="true" stored="true"/>
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/io/stream/StreamingTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/io/stream/StreamingTest.java
index 1cea3112621..9685b7414e4 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/io/stream/StreamingTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/io/stream/StreamingTest.java
@@ -17,7 +17,9 @@
 package org.apache.solr.client.solrj.io.stream;
 
 import java.io.IOException;
import java.time.Instant;
 import java.util.ArrayList;
import java.util.Date;
 import java.util.List;
 import java.util.Map;
 
@@ -1740,6 +1742,132 @@ public class StreamingTest extends SolrCloudTestCase {
 
   }
 
  @Test
  public void testDateBoolSorting() throws Exception {

    new UpdateRequest()
        .add(id, "0", "b_sing", "false", "dt_sing", "1981-03-04T01:02:03.78Z")
        .add(id, "3", "b_sing", "true", "dt_sing", "1980-03-04T01:02:03.78Z")
        .add(id, "2", "b_sing", "false", "dt_sing", "1981-04-04T01:02:03.78Z")
        .add(id, "1", "b_sing", "true", "dt_sing", "1980-04-04T01:02:03.78Z")
        .add(id, "4", "b_sing", "true", "dt_sing", "1980-04-04T01:02:03.78Z")
        .commit(cluster.getSolrClient(), COLLECTION);


    trySortWithQt("/export");
    trySortWithQt("/select");
  }
  private void trySortWithQt(String which) throws Exception {
    //Basic CloudSolrStream Test bools desc

    SolrParams sParams = mapParams("q", "*:*", "qt", which, "fl", "id,b_sing", "sort", "b_sing asc,id asc");
    CloudSolrStream stream = new CloudSolrStream(zkHost, COLLECTION, sParams);
    try  {
      List<Tuple> tuples = getTuples(stream);

      assert (tuples.size() == 5);
      assertOrder(tuples, 0, 2, 1, 3, 4);

      //Basic CloudSolrStream Test bools desc
      sParams = mapParams("q", "*:*", "qt", which, "fl", "id,b_sing", "sort", "b_sing desc,id desc");
      stream = new CloudSolrStream(zkHost, COLLECTION, sParams);
      tuples = getTuples(stream);

      assert (tuples.size() == 5);
      assertOrder(tuples, 4, 3, 1, 2, 0);

      //Basic CloudSolrStream Test dates desc
      sParams = mapParams("q", "*:*", "qt", which, "fl", "id,dt_sing", "sort", "dt_sing desc,id asc");
      stream = new CloudSolrStream(zkHost, COLLECTION, sParams);
      tuples = getTuples(stream);

      assert (tuples.size() == 5);
      assertOrder(tuples, 2, 0, 1, 4, 3);

      //Basic CloudSolrStream Test ates desc
      sParams = mapParams("q", "*:*", "qt", which, "fl", "id,dt_sing", "sort", "dt_sing asc,id desc");
      stream = new CloudSolrStream(zkHost, COLLECTION, sParams);
      tuples = getTuples(stream);

      assert (tuples.size() == 5);
      assertOrder(tuples, 3, 4, 1, 0, 2);
    } finally {
      if (stream != null) {
        stream.close();
      }
    }

  }


  @Test
  public void testAllValidExportTypes() throws Exception {

    //Test whether all the expected types are actually returned, including booleans and dates.
    // The contract is that the /select and /export handlers return the same format, so we can test this once each
    // way
    new UpdateRequest()
        .add(id, "0", "i_sing", "11", "i_multi", "12", "i_multi", "13",
            "l_sing", "14", "l_multi", "15", "l_multi", "16",
            "f_sing", "1.70", "f_multi", "1.80", "f_multi", "1.90",
            "d_sing", "1.20", "d_multi", "1.21", "d_multi", "1.22",
            "s_sing", "single", "s_multi", "sm1", "s_multi", "sm2",
            "dt_sing", "1980-01-02T11:11:33.89Z", "dt_multi", "1981-03-04T01:02:03.78Z", "dt_multi", "1981-05-24T04:05:06.99Z",
            "b_sing", "true", "b_multi", "false", "b_multi", "true"
        )
        .commit(cluster.getSolrClient(), COLLECTION);

    tryWithQt("/export");
    tryWithQt("/select");
  }
  
  // We should be getting the exact same thing back with both the export and select handlers, so test
  private void tryWithQt(String which) throws IOException {
    SolrParams sParams = StreamingTest.mapParams("q", "*:*", "qt", which, "fl", 
        "id,i_sing,i_multi,l_sing,l_multi,f_sing,f_multi,d_sing,d_multi,dt_sing,dt_multi,s_sing,s_multi,b_sing,b_multi", 
        "sort", "i_sing asc");
    try (CloudSolrStream stream = new CloudSolrStream(zkHost, COLLECTION, sParams)) {

      Tuple tuple = getTuple(stream); // All I really care about is that all the fields are returned. There's

      assertTrue("Integers should be returned", tuple.getLong("i_sing") == 11L);
      assertTrue("MV should be returned for i_multi", tuple.getLongs("i_multi").get(0) == 12);
      assertTrue("MV should be returned for i_multi", tuple.getLongs("i_multi").get(1) == 13);

      assertTrue("longs should be returned", tuple.getLong("l_sing") == 14L);
      assertTrue("MV should be returned for l_multi", tuple.getLongs("l_multi").get(0) == 15);
      assertTrue("MV should be returned for l_multi", tuple.getLongs("l_multi").get(1) == 16);

      assertTrue("floats should be returned", tuple.getDouble("f_sing") == 1.7);
      assertTrue("MV should be returned for f_multi", tuple.getDoubles("f_multi").get(0) == 1.8);
      assertTrue("MV should be returned for f_multi", tuple.getDoubles("f_multi").get(1) == 1.9);

      assertTrue("doubles should be returned", tuple.getDouble("d_sing") == 1.2);
      assertTrue("MV should be returned for d_multi", tuple.getDoubles("d_multi").get(0) == 1.21);
      assertTrue("MV should be returned for d_multi", tuple.getDoubles("d_multi").get(1) == 1.22);

      assertTrue("Strings should be returned", tuple.getString("s_sing").equals("single"));
      assertTrue("MV should be returned for s_multi", tuple.getStrings("s_multi").get(0).equals("sm1"));
      assertTrue("MV should be returned for s_multi", tuple.getStrings("s_multi").get(1).equals("sm2"));

      assertTrue("Dates should be returned as Strings", tuple.getString("dt_sing").equals("1980-01-02T11:11:33.890Z"));
      assertTrue("MV dates should be returned as Strings for dt_multi", tuple.getStrings("dt_multi").get(0).equals("1981-03-04T01:02:03.780Z"));
      assertTrue("MV dates should be returned as Strings for dt_multi", tuple.getStrings("dt_multi").get(1).equals("1981-05-24T04:05:06.990Z"));

      // Also test native type conversion
      Date dt = new Date(Instant.parse("1980-01-02T11:11:33.890Z").toEpochMilli());
      assertTrue("Dates should be returned as Dates", tuple.getDate("dt_sing").equals(dt));
      dt = new Date(Instant.parse("1981-03-04T01:02:03.780Z").toEpochMilli());
      assertTrue("MV dates should be returned as Dates for dt_multi", tuple.getDates("dt_multi").get(0).equals(dt));
      dt = new Date(Instant.parse("1981-05-24T04:05:06.990Z").toEpochMilli());
      assertTrue("MV dates should be returned as Dates  for dt_multi", tuple.getDates("dt_multi").get(1).equals(dt));
      
      assertTrue("Booleans should be returned", tuple.getBool("b_sing"));
      assertFalse("MV boolean should be returned for b_multi", tuple.getBools("b_multi").get(0));
      assertTrue("MV boolean should be returned for b_multi", tuple.getBools("b_multi").get(1));
    }

  }
   protected List<Tuple> getTuples(TupleStream tupleStream) throws IOException {
     tupleStream.open();
     List<Tuple> tuples = new ArrayList();
- 
2.19.1.windows.1

