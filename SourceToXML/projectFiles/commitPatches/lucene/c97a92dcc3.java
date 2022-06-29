From c97a92dcc3704ba0687de454e8a7f4567bad361b Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Fri, 22 May 2015 23:45:24 +0000
Subject: [PATCH] SOLR-7335: Fix doc boosts to no longer be multiplied in each
 field value in multivalued fields that are not used in copyFields

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1681249 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  7 +++++
 .../apache/solr/update/DocumentBuilder.java   | 12 ++++-----
 .../solr/update/DocumentBuilderTest.java      | 27 +++++++++++++++++--
 3 files changed, 38 insertions(+), 8 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index ea155d80839..e5cea0b0cdf 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -107,6 +107,11 @@ Jetty 9.2.10.v20150310
 Upgrading from Solr 5.1
 -----------------------
 
* A bug was introduced in Solr 4.10 that caused index time document boosts to trigger excessive field
  boosts in multivalued fields -- the result being that some field norms might be excessively large.
  This bug has now been fixed, but users of document boosts are strongly encouraged to re-index.
  See SOLR-7335 for more details.

 * The Slice and Replica classes have been changed to use State enums instead of string constants 
   to track the respective stats.  Advanced users with client code manipulating these objects will 
   need to update their code accordingly.  See SOLR-7325 and SOLR-7336 for more info.
@@ -343,6 +348,8 @@ Bug Fixes
   using the thread-pool managed by ZkContainer instead of a single thread.
   (Jessica Cheng Mallet, Timothy Potter, shalin, Mark Miller)
 
* SOLR-7335: Fix doc boosts to no longer be multiplied in each field value in multivalued fields that
  are not used in copyFields (Shingo Sasaki via hossman)
 
 Optimizations
 ----------------------
diff --git a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
index 5e8980af5dc..239731c1635 100644
-- a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
++ b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
@@ -160,13 +160,13 @@ public class DocumentBuilder {
               // record the field as having a value
               usedFields.add(destinationField.getName());
             }
            
            // The final boost for a given field named is the product of the 
            // *all* boosts on values of that field. 
            // For multi-valued fields, we only want to set the boost on the
            // first field.
            fieldBoost = compoundBoost = 1.0f;
           }

          // The final boost for a given field named is the product of the 
          // *all* boosts on values of that field. 
          // For multi-valued fields, we only want to set the boost on the
          // first field.
          fieldBoost = compoundBoost = 1.0f;
         }
       }
       catch( SolrException ex ) {
diff --git a/solr/core/src/test/org/apache/solr/update/DocumentBuilderTest.java b/solr/core/src/test/org/apache/solr/update/DocumentBuilderTest.java
index 058b6423960..81a49524e88 100644
-- a/solr/core/src/test/org/apache/solr/update/DocumentBuilderTest.java
++ b/solr/core/src/test/org/apache/solr/update/DocumentBuilderTest.java
@@ -17,6 +17,8 @@
 
 package org.apache.solr.update;
 
import java.util.List;

 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.index.LeafReader;
@@ -33,6 +35,7 @@ import org.apache.solr.common.SolrInputField;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.search.DocList;
import org.apache.solr.schema.CopyField;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.request.SolrQueryRequest;
@@ -221,12 +224,11 @@ public class DocumentBuilderTest extends SolrTestCaseJ4 {
     assertNull(h.validateUpdate(add(xml, new String[0])));
   }
   
  public void testMultiValuedFieldAndDocBoosts() throws Exception {
  private void assertMultiValuedFieldAndDocBoosts(SolrInputField field) throws Exception {
     SolrCore core = h.getCore();
     IndexSchema schema = core.getLatestSchema();
     SolrInputDocument doc = new SolrInputDocument();
     doc.setDocumentBoost(3.0f);
    SolrInputField field = new SolrInputField( "foo_t" );
     field.addValue( "summer time" , 1.0f );
     field.addValue( "in the city" , 5.0f ); // using boost
     field.addValue( "living is easy" , 1.0f );
@@ -247,6 +249,27 @@ public class DocumentBuilderTest extends SolrTestCaseJ4 {
     
   }
 
  public void testMultiValuedFieldAndDocBoostsWithCopy() throws Exception {
    SolrCore core = h.getCore();
    IndexSchema schema = core.getLatestSchema();
    SolrInputField field = new SolrInputField( "foo_t" );
    List<CopyField> copyFields = schema.getCopyFieldsList(field.getName());
    
    assertNotNull( copyFields );
    assertFalse( copyFields.isEmpty() );
    assertMultiValuedFieldAndDocBoosts( field );
  }
  
  public void testMultiValuedFieldAndDocBoostsNoCopy() throws Exception {
    SolrCore core = h.getCore();
    IndexSchema schema = core.getLatestSchema();
    SolrInputField field = new SolrInputField( "t_foo" );
    List<CopyField> copyFields = schema.getCopyFieldsList(field.getName());

    assertTrue( copyFields == null || copyFields.isEmpty() );
    assertMultiValuedFieldAndDocBoosts( field );
  }

   public void testCopyFieldsAndFieldBoostsAndDocBoosts() throws Exception {
     SolrCore core = h.getCore();
     IndexSchema schema = core.getLatestSchema();
- 
2.19.1.windows.1

