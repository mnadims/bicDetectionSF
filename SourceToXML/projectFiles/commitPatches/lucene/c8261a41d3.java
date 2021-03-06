From c8261a41d30e3966159e3bb9d4d23df0f8e3cf63 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Sat, 19 Jul 2014 07:10:36 +0000
Subject: [PATCH] SOLR-6259: Reduce CPU usage by avoiding repeated costly calls
 to Document.getField inside DocumentBuilder.toDocument for use-cases with
 large number of fields and copyFields

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1611852 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  4 +
 .../apache/solr/update/DocumentBuilder.java   | 79 +++++++++++--------
 2 files changed, 49 insertions(+), 34 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index a0f4bde4098..6dfb65359a1 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -204,6 +204,10 @@ Optimizations
   indexes with many fields of same type just use one TokenStream per thread.
   (Shay Banon, Uwe Schindler, Robert Muir)
 
* SOLR-6259: Reduce CPU usage by avoiding repeated costly calls to Document.getField inside
  DocumentBuilder.toDocument for use-cases with large number of fields and copyFields.
  (Steven Bower via shalin)

 Other Changes
 ---------------------
 
diff --git a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
index 9c9d7f72ddb..6b58f6d5fda 100644
-- a/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
++ b/solr/core/src/java/org/apache/solr/update/DocumentBuilder.java
@@ -18,6 +18,7 @@
 package org.apache.solr.update;
 
 import java.util.List;
import java.util.Set;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
@@ -30,6 +31,8 @@ import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 
 
import com.google.common.collect.Sets;

 /**
  *
  */
@@ -75,6 +78,7 @@ public class DocumentBuilder {
   { 
     Document out = new Document();
     final float docBoost = doc.getDocumentBoost();
    Set<String> usedFields = Sets.newHashSet();
     
     // Load fields from SolrDocument to Document
     for( SolrInputField field : doc ) {
@@ -103,6 +107,9 @@ public class DocumentBuilder {
       // it ourselves 
       float compoundBoost = fieldBoost * docBoost;
 
      List<CopyField> copyFields = schema.getCopyFieldsList(name);
      if( copyFields.size() == 0 ) copyFields = null;

       // load each field value
       boolean hasField = false;
       try {
@@ -114,48 +121,52 @@ public class DocumentBuilder {
           if (sfield != null) {
             used = true;
             addField(out, sfield, v, applyBoost ? compoundBoost : 1f);
            // record the field as having a value
            usedFields.add(sfield.getName());
           }
   
           // Check if we should copy this field value to any other fields.
           // This could happen whether it is explicit or not.
          List<CopyField> copyFields = schema.getCopyFieldsList(name);
          for (CopyField cf : copyFields) {
            SchemaField destinationField = cf.getDestination();

            final boolean destHasValues = 
              (null != out.getField(destinationField.getName()));

            // check if the copy field is a multivalued or not
            if (!destinationField.multiValued() && destHasValues) {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                      "ERROR: "+getID(doc, schema)+"multiple values encountered for non multiValued copy field " +
                              destinationField.getName() + ": " + v);
            }
          if( copyFields != null ){
            for (CopyField cf : copyFields) {
              SchemaField destinationField = cf.getDestination();
   
            used = true;
            
            // Perhaps trim the length of a copy field
            Object val = v;
            if( val instanceof String && cf.getMaxChars() > 0 ) {
              val = cf.getLimitedValue((String)val);
              final boolean destHasValues = usedFields.contains(destinationField.getName());
  
              // check if the copy field is a multivalued or not
              if (!destinationField.multiValued() && destHasValues) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "ERROR: "+getID(doc, schema)+"multiple values encountered for non multiValued copy field " +
                                destinationField.getName() + ": " + v);
              }
    
              used = true;
              
              // Perhaps trim the length of a copy field
              Object val = v;
              if( val instanceof String && cf.getMaxChars() > 0 ) {
                val = cf.getLimitedValue((String)val);
              }
  
              // we can't copy any boost unless the dest field is 
              // indexed & !omitNorms, but which boost we copy depends
              // on whether the dest field already contains values (we
              // don't want to apply the compounded docBoost more then once)
              final float destBoost = 
                (destinationField.indexed() && !destinationField.omitNorms()) ?
                (destHasValues ? fieldBoost : compoundBoost) : 1.0F;
              
              addField(out, destinationField, val, destBoost);
              // record the field as having a value
              usedFields.add(destinationField.getName());
             }

            // we can't copy any boost unless the dest field is 
            // indexed & !omitNorms, but which boost we copy depends
            // on whether the dest field already contains values (we
            // don't want to apply the compounded docBoost more then once)
            final float destBoost = 
              (destinationField.indexed() && !destinationField.omitNorms()) ?
              (destHasValues ? fieldBoost : compoundBoost) : 1.0F;
             
            addField(out, destinationField, val, destBoost);
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
- 
2.19.1.windows.1

