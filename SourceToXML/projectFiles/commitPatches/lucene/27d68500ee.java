From 27d68500eeeb7215a00c09c213b3d0b62512f430 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Mon, 21 Jul 2014 07:25:24 +0000
Subject: [PATCH] SOLR-5968: BinaryResponseWriter fetches unnecessary stored
 fields when only pseudo-fields are requested

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1612200 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                                |  3 +++
 .../solr/response/BinaryResponseWriter.java     | 17 +++++++++++++----
 2 files changed, 16 insertions(+), 4 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 1054ede2316..e3d2c9c8310 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -215,6 +215,9 @@ Optimizations
   DocumentBuilder.toDocument for use-cases with large number of fields and copyFields.
   (Steven Bower via shalin)
 
* SOLR-5968: BinaryResponseWriter fetches unnecessary stored fields when only pseudo-fields
  are requested. (Gregg Donovan via shalin)

 Other Changes
 ---------------------
 
diff --git a/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java b/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
index 1f91281bade..25b1909922e 100644
-- a/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
++ b/solr/core/src/java/org/apache/solr/response/BinaryResponseWriter.java
@@ -34,6 +34,7 @@ import org.apache.solr.schema.*;
 import org.apache.solr.search.DocList;
 import org.apache.solr.search.ReturnFields;
 import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SolrReturnFields;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -141,11 +142,19 @@ public class BinaryResponseWriter implements BinaryQueryResponseWriter {
       }
       
       Set<String> fnames = returnFields.getLuceneFieldNames();
      boolean onlyPseudoFields = (fnames == null && !returnFields.wantsAllFields())
          || (fnames != null && fnames.size() == 1 && SolrReturnFields.SCORE.equals(fnames.iterator().next()));
       context.iterator = ids.iterator();
       for (int i = 0; i < sz; i++) {
         int id = context.iterator.nextDoc();
        StoredDocument doc = searcher.doc(id, fnames);
        SolrDocument sdoc = getDoc(doc);
        SolrDocument sdoc;
        if (onlyPseudoFields) {
          // no need to get stored fields of the document, see SOLR-5968
          sdoc = new SolrDocument();
        } else {
          StoredDocument doc = searcher.doc(id, fnames);
          sdoc = getDoc(doc);
        }
         if( transformer != null ) {
           transformer.transform(sdoc, id);
         }
@@ -178,9 +187,9 @@ public class BinaryResponseWriter implements BinaryQueryResponseWriter {
       SolrDocument solrDoc = new SolrDocument();
       for (StorableField f : doc) {
         String fieldName = f.name();
        if( !returnFields.wantsField(fieldName) ) 
        if( !returnFields.wantsField(fieldName) )
           continue;
        

         SchemaField sf = schema.getFieldOrNull(fieldName);
         Object val = null;
         try {
- 
2.19.1.windows.1

