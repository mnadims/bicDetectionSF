From 135d89c4bddb092a4b880921415a82833154f691 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Sat, 30 Jul 2011 19:18:09 +0000
Subject: [PATCH] SOLR-2685: always use SolrInputDocument in update chain,
 change from String to BytesRef

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1152500 13f79535-47bb-0310-9956-ffa450edef68
--
 .../extraction/ExtractingDocumentLoader.java  |  2 +-
 .../solr/handler/CSVRequestHandler.java       |  2 +-
 .../org/apache/solr/handler/XMLLoader.java    |  3 +-
 .../apache/solr/update/AddUpdateCommand.java  | 51 +++++++-------
 .../solr/update/DeleteUpdateCommand.java      | 26 ++++++++
 .../solr/update/DirectUpdateHandler2.java     | 11 ++--
 .../org/apache/solr/update/UpdateHandler.java | 66 -------------------
 .../processor/LogUpdateProcessorFactory.java  |  4 +-
 .../processor/RunUpdateProcessorFactory.java  |  1 -
 .../apache/solr/search/TestRealTimeGet.java   |  2 +-
 .../DirectUpdateHandlerOptimizeTest.java      |  7 +-
 .../solr/update/TestIndexingPerformance.java  | 42 ++++++------
 12 files changed, 83 insertions(+), 134 deletions(-)

diff --git a/solr/contrib/extraction/src/java/org/apache/solr/handler/extraction/ExtractingDocumentLoader.java b/solr/contrib/extraction/src/java/org/apache/solr/handler/extraction/ExtractingDocumentLoader.java
index 4c1f7f20c57..615a4586141 100644
-- a/solr/contrib/extraction/src/java/org/apache/solr/handler/extraction/ExtractingDocumentLoader.java
++ b/solr/contrib/extraction/src/java/org/apache/solr/handler/extraction/ExtractingDocumentLoader.java
@@ -119,7 +119,7 @@ public class ExtractingDocumentLoader extends ContentStreamLoader {
   }
 
   void addDoc(SolrContentHandler handler) throws IOException {
    templateAdd.indexedId = null;
    templateAdd.clear();
     doAdd(handler, templateAdd);
   }
 
diff --git a/solr/core/src/java/org/apache/solr/handler/CSVRequestHandler.java b/solr/core/src/java/org/apache/solr/handler/CSVRequestHandler.java
index f1bfb118bb2..4f8ceb2acdd 100755
-- a/solr/core/src/java/org/apache/solr/handler/CSVRequestHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/CSVRequestHandler.java
@@ -411,7 +411,7 @@ class SingleThreadedCSVLoader extends CSVLoader {
 
   @Override
   void addDoc(int line, String[] vals) throws IOException {
    templateAdd.indexedId = null;
    templateAdd.clear();
     SolrInputDocument doc = new SolrInputDocument();
     doAdd(line, vals, doc, templateAdd);
   }
diff --git a/solr/core/src/java/org/apache/solr/handler/XMLLoader.java b/solr/core/src/java/org/apache/solr/handler/XMLLoader.java
index d2dea87d280..31e3fd0c008 100644
-- a/solr/core/src/java/org/apache/solr/handler/XMLLoader.java
++ b/solr/core/src/java/org/apache/solr/handler/XMLLoader.java
@@ -211,8 +211,7 @@ class XMLLoader extends ContentStreamLoader {
                     "unexpected XML tag /delete/" + currTag);
           }
           processor.processDelete(deleteCmd);
          deleteCmd.id = null;
          deleteCmd.query = null;
          deleteCmd.clear();
           break;
 
           // Add everything to the text
diff --git a/solr/core/src/java/org/apache/solr/update/AddUpdateCommand.java b/solr/core/src/java/org/apache/solr/update/AddUpdateCommand.java
index cf6ce93083e..4b246a2f3a3 100644
-- a/solr/core/src/java/org/apache/solr/update/AddUpdateCommand.java
++ b/solr/core/src/java/org/apache/solr/update/AddUpdateCommand.java
@@ -20,6 +20,8 @@ package org.apache.solr.update;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Fieldable;
 import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.SolrInputField;
 import org.apache.solr.request.SolrQueryRequest;
@@ -32,10 +34,7 @@ import org.apache.solr.schema.SchemaField;
 public class AddUpdateCommand extends UpdateCommand {
    // optional id in "internal" indexed form... if it is needed and not supplied,
    // it will be obtained from the doc.
   public String indexedId;

   // The Lucene document to be indexed
   public Document doc;
   private BytesRef indexedId;
 
    // Higher level SolrInputDocument, normally used to construct the Lucene Document
    // to index.
@@ -52,7 +51,6 @@ public class AddUpdateCommand extends UpdateCommand {
 
    /** Reset state to reuse this object with a different document in the same request */
    public void clear() {
     doc = null;
      solrDoc = null;
      indexedId = null;
    }
@@ -61,26 +59,32 @@ public class AddUpdateCommand extends UpdateCommand {
      return solrDoc;
    }
 
   public Document getLuceneDocument(IndexSchema schema) {
     if (doc == null && solrDoc != null) {
       // TODO??  build the doc from the SolrDocument?
     }
     return doc;    
  /** Creates and returns a lucene Document to index.  Any changes made to the returned Document
   * will not be reflected in the SolrInputDocument, or future calls to this method.
   */
   public Document getLuceneDocument() {
     return DocumentBuilder.toDocument(getSolrInputDocument(), req.getSchema());
    }
 
   public String getIndexedId(IndexSchema schema) {
  /** Returns the indexed ID for this document.  The returned BytesRef is retained across multiple calls, and should not be modified. */
   public BytesRef getIndexedId() {
      if (indexedId == null) {
       IndexSchema schema = req.getSchema();
        SchemaField sf = schema.getUniqueKeyField();
        if (sf != null) {
         if (doc != null) {
           schema.getUniqueKeyField();
           Fieldable storedId = doc.getFieldable(sf.getName());
           indexedId = sf.getType().storedToIndexed(storedId);
         }
          if (solrDoc != null) {
            SolrInputField field = solrDoc.getField(sf.getName());
           if (field != null) {
             indexedId = sf.getType().toInternal( field.getFirstValue().toString() );

           int count = field==null ? 0 : field.getValueCount();
           if (count == 0) {
             if (overwrite) {
               throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Document is missing mandatory uniqueKey field: " + sf.getName());
             }
           } else if (count  > 1) {
             throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Document contains multiple values for uniqueKey field: " + field);
           } else {
             indexedId = new BytesRef();
             sf.getType().readableToIndexed(field.getFirstValue().toString(), indexedId);
            }
          }
        }
@@ -88,16 +92,9 @@ public class AddUpdateCommand extends UpdateCommand {
      return indexedId;
    }
 
   public String getPrintableId(IndexSchema schema) {
   public String getPrintableId() {
     IndexSchema schema = req.getSchema();
      SchemaField sf = schema.getUniqueKeyField();
     if (indexedId != null && sf != null) {
       return sf.getType().indexedToReadable(indexedId);
     }

     if (doc != null) {
       return schema.printableUniqueKey(doc);
     }

      if (solrDoc != null && sf != null) {
        SolrInputField field = solrDoc.getField(sf.getName());
        if (field != null) {
diff --git a/solr/core/src/java/org/apache/solr/update/DeleteUpdateCommand.java b/solr/core/src/java/org/apache/solr/update/DeleteUpdateCommand.java
index adfd5ca2578..e7fa495e443 100644
-- a/solr/core/src/java/org/apache/solr/update/DeleteUpdateCommand.java
++ b/solr/core/src/java/org/apache/solr/update/DeleteUpdateCommand.java
@@ -17,7 +17,11 @@
 
 package org.apache.solr.update;
 
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrInputField;
 import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
 
 /**
  *
@@ -25,11 +29,33 @@ import org.apache.solr.request.SolrQueryRequest;
 public class DeleteUpdateCommand extends UpdateCommand {
   public String id;    // external (printable) id, for delete-by-id
   public String query; // query string for delete-by-query
  private BytesRef indexedId;

 
   public DeleteUpdateCommand(SolrQueryRequest req) {
     super("delete", req);
   }
 
  public void clear() {
    id = null;
    query = null;
    indexedId = null;
  }

  /** Returns the indexed ID for this delete.  The returned BytesRef is retained across multiple calls, and should not be modified. */
  public BytesRef getIndexedId() {
    if (indexedId == null) {
      IndexSchema schema = req.getSchema();
      SchemaField sf = schema.getUniqueKeyField();
      if (sf != null && id != null) {
        indexedId = new BytesRef();
        sf.getType().readableToIndexed(id, indexedId);
      }
    }
    return indexedId;
  }


   @Override
   public String toString() {
     StringBuilder sb = new StringBuilder(commandName);
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index d2efaef4314..b316b522b98 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -154,10 +154,7 @@ public class DirectUpdateHandler2 extends UpdateHandler {
 			Term updateTerm = null;
 
       if (cmd.overwrite) {
        if (cmd.indexedId == null) {
          cmd.indexedId = getIndexedId(cmd.doc);
        }
        Term idTerm = new Term(idField.getName(), cmd.indexedId);
        Term idTerm = new Term(idField.getName(), cmd.getIndexedId());
         boolean del = false;
         if (cmd.updateTerm == null) {
           updateTerm = idTerm;
@@ -166,7 +163,7 @@ public class DirectUpdateHandler2 extends UpdateHandler {
           updateTerm = cmd.updateTerm;
         }
 
        writer.updateDocument(updateTerm, cmd.getLuceneDocument(schema));
        writer.updateDocument(updateTerm, cmd.getLuceneDocument());
         if(del) { // ensure id remains unique
           BooleanQuery bq = new BooleanQuery();
           bq.add(new BooleanClause(new TermQuery(updateTerm), Occur.MUST_NOT));
@@ -175,7 +172,7 @@ public class DirectUpdateHandler2 extends UpdateHandler {
         }
       } else {
         // allow duplicates
        writer.addDocument(cmd.getLuceneDocument(schema));
        writer.addDocument(cmd.getLuceneDocument());
       }
 
       rc = 1;
@@ -198,7 +195,7 @@ public class DirectUpdateHandler2 extends UpdateHandler {
     deleteByIdCommands.incrementAndGet();
     deleteByIdCommandsCumulative.incrementAndGet();
 
    indexWriterProvider.getIndexWriter().deleteDocuments(new Term(idField.getName(), idFieldType.toInternal(cmd.id)));
    indexWriterProvider.getIndexWriter().deleteDocuments(new Term(idField.getName(), cmd.getIndexedId()));
 
     if (commitTracker.timeUpperBound > 0) {
       commitTracker.scheduleCommitWithin(commitTracker.timeUpperBound);
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
index 0a5e32b81a7..28f2aad53fd 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
@@ -111,34 +111,6 @@ public abstract class UpdateHandler implements SolrInfoMBean {
     idFieldType = idField!=null ? idField.getType() : null;
     parseEventListeners();
   }

  protected final Term idTerm(String readableId) {
    // to correctly create the Term, the string needs to be run
    // through the Analyzer for that field.
    return new Term(idField.getName(), idFieldType.toInternal(readableId));
  }

  protected final String getIndexedId(Document doc) {
    if (idField == null)
      throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Operation requires schema to have a unique key field");

    // Right now, single valued fields that require value transformation from external to internal (indexed)
    // form have that transformation already performed and stored as the field value.
    Fieldable[] id = doc.getFieldables( idField.getName() );
    if (id == null || id.length < 1)
      throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Document is missing mandatory uniqueKey field: " + idField.getName());
    if( id.length > 1 )
      throw new SolrException( SolrException.ErrorCode.BAD_REQUEST,"Document contains multiple values for uniqueKey field: " + idField.getName());

    return idFieldType.storedToIndexed( id[0] );
  }

  protected final String getIndexedIdOptional(Document doc) {
    if (idField == null) return null;
    Fieldable f = doc.getFieldable(idField.getName());
    if (f == null) return null;
    return idFieldType.storedToIndexed(f);
  }
   
   /**
    * Allows the UpdateHandler to create the SolrIndexSearcher after it
@@ -167,44 +139,6 @@ public abstract class UpdateHandler implements SolrInfoMBean {
   public abstract void close() throws IOException;
 
 
  static class DeleteHitCollector extends Collector {
    public int deleted=0;
    public final SolrIndexSearcher searcher;
    private int docBase;

    public DeleteHitCollector(SolrIndexSearcher searcher) {
      this.searcher = searcher;
    }

    @Override
    public void collect(int doc) {
      try {
        searcher.getIndexReader().deleteDocument(doc + docBase);
        deleted++;
      } catch (IOException e) {
        // don't try to close the searcher on failure for now...
        // try { closeSearcher(); } catch (Exception ee) { SolrException.log(log,ee); }
        throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,"Error deleting doc# "+doc,e,false);
      }
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      return false;
    }

    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
      docBase = context.docBase;
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      
    }
  }


   /**
    * NOTE: this function is not thread safe.  However, it is safe to call within the
    * <code>inform( SolrCore core )</code> function for <code>SolrCoreAware</code> classes.
diff --git a/solr/core/src/java/org/apache/solr/update/processor/LogUpdateProcessorFactory.java b/solr/core/src/java/org/apache/solr/update/processor/LogUpdateProcessorFactory.java
index 16df29e66b2..d53314e68bc 100644
-- a/solr/core/src/java/org/apache/solr/update/processor/LogUpdateProcessorFactory.java
++ b/solr/core/src/java/org/apache/solr/update/processor/LogUpdateProcessorFactory.java
@@ -106,9 +106,9 @@ class LogUpdateProcessor extends UpdateRequestProcessor {
     }
 
     if (adds.size() < maxNumToLog) {
      adds.add(cmd.getPrintableId(req.getSchema()));
      adds.add(cmd.getPrintableId());
     }
    if (logDebug) { log.debug("add {}", cmd.getPrintableId(req.getSchema())); }
    if (logDebug) { log.debug("add {}", cmd.getPrintableId()); }
 
     numAdds++;
 
diff --git a/solr/core/src/java/org/apache/solr/update/processor/RunUpdateProcessorFactory.java b/solr/core/src/java/org/apache/solr/update/processor/RunUpdateProcessorFactory.java
index 3a91cda5ed0..c00be6f0284 100644
-- a/solr/core/src/java/org/apache/solr/update/processor/RunUpdateProcessorFactory.java
++ b/solr/core/src/java/org/apache/solr/update/processor/RunUpdateProcessorFactory.java
@@ -57,7 +57,6 @@ class RunUpdateProcessor extends UpdateRequestProcessor
 
   @Override
   public void processAdd(AddUpdateCommand cmd) throws IOException {
    cmd.doc = DocumentBuilder.toDocument(cmd.getSolrInputDocument(), req.getSchema());
     updateHandler.addDoc(cmd);
     super.processAdd(cmd);
   }
diff --git a/solr/core/src/test/org/apache/solr/search/TestRealTimeGet.java b/solr/core/src/test/org/apache/solr/search/TestRealTimeGet.java
index fbee38af0c1..8b6fed09bf2 100644
-- a/solr/core/src/test/org/apache/solr/search/TestRealTimeGet.java
++ b/solr/core/src/test/org/apache/solr/search/TestRealTimeGet.java
@@ -294,7 +294,7 @@ public class TestRealTimeGet extends SolrTestCaseJ4 {
     final boolean tombstones = false;
 
     // query variables
    final AtomicLong operations = new AtomicLong(10000000);  // number of query operations to perform in total       // TODO: temporarily high due to lack of stability
    final AtomicLong operations = new AtomicLong(0);  // number of query operations to perform in total       // TODO: temporarily high due to lack of stability
     int nReadThreads = 10;
 
     initModel(ndocs);
diff --git a/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerOptimizeTest.java b/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerOptimizeTest.java
index 2f425545d65..cabb3626112 100644
-- a/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerOptimizeTest.java
++ b/solr/core/src/test/org/apache/solr/update/DirectUpdateHandlerOptimizeTest.java
@@ -18,6 +18,7 @@ package org.apache.solr.update;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.util.AbstractSolrTestCase;
@@ -55,9 +56,9 @@ public class DirectUpdateHandlerOptimizeTest extends AbstractSolrTestCase {
     //the merge factor is 100 and the maxBufferedDocs is 2, so there should be 50 segments
     for (int i = 0; i < 99; i++) {
       // Add a valid document
      cmd.doc = new Document();
      cmd.doc.add(new Field("id", "id_" + i, Field.Store.YES, Field.Index.NOT_ANALYZED));
      cmd.doc.add(new Field("subject", "subject_" + i, Field.Store.NO, Field.Index.ANALYZED));
      cmd.solrDoc = new SolrInputDocument();
      cmd.solrDoc.addField("id", "id_" + i);
      cmd.solrDoc.addField("subject", "subject_" + i);
       updater.addDoc(cmd);
     }
 
diff --git a/solr/core/src/test/org/apache/solr/update/TestIndexingPerformance.java b/solr/core/src/test/org/apache/solr/update/TestIndexingPerformance.java
index 51a55c4aaf1..5249cda0b16 100755
-- a/solr/core/src/test/org/apache/solr/update/TestIndexingPerformance.java
++ b/solr/core/src/test/org/apache/solr/update/TestIndexingPerformance.java
@@ -20,6 +20,7 @@ package org.apache.solr.update;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.Fieldable;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.util.AbstractSolrTestCase;
@@ -50,7 +51,6 @@ public class TestIndexingPerformance extends AbstractSolrTestCase {
     int iter=1000;
     String iterS = System.getProperty("iter");
     if (iterS != null) iter=Integer.parseInt(iterS);
    boolean includeDoc = Boolean.parseBoolean(System.getProperty("includeDoc","true")); // include the time to create the document
     boolean overwrite = Boolean.parseBoolean(System.getProperty("overwrite","false"));
     String doc = System.getProperty("doc");
     if (doc != null) {
@@ -61,14 +61,15 @@ public class TestIndexingPerformance extends AbstractSolrTestCase {
     SolrQueryRequest req = lrf.makeRequest();
     IndexSchema schema = req.getSchema();
     UpdateHandler updateHandler = req.getCore().getUpdateHandler();

    String[] fields = {"text","simple"
            ,"text","test"
            ,"text","how now brown cow"
            ,"text","what's that?"
            ,"text","radical!"
            ,"text","what's all this about, anyway?"
            ,"text","just how fast is this text indexing?"
    String field = "textgap";

    String[] fields = {field,"simple"
            ,field,"test"
            ,field,"how now brown cow"
            ,field,"what's that?"
            ,field,"radical!"
            ,field,"what's all this about, anyway?"
            ,field,"just how fast is this text indexing?"
     };
 
 
@@ -91,26 +92,21 @@ public class TestIndexingPerformance extends AbstractSolrTestCase {
     long start = System.currentTimeMillis();
 
     AddUpdateCommand add = new AddUpdateCommand(req);

    Field idField=null;
    add.overwrite = overwrite;
 
     for (int i=0; i<iter; i++) {
      if (includeDoc || add.doc==null) {
        add.doc = new Document();
        idField = new Field("id","", Field.Store.YES, Field.Index.NOT_ANALYZED);
        add.doc.add(idField);
        for (int j=0; j<fields.length; j+=2) {
          String field = fields[j];
          String val = fields[j+1];
          Fieldable f = schema.getField(field).createField(val, 1.0f);
          add.doc.add(f);
        }
      add.clear();
      add.solrDoc = new SolrInputDocument();
      add.solrDoc.addField("id", Integer.toString(i));
      for (int j=0; j<fields.length; j+=2) {
        String f = fields[j];
        String val = fields[j+1];
        add.solrDoc.addField(f, val);
       }
      idField.setValue(Integer.toString(i));
       updateHandler.addDoc(add);
     }
     long end = System.currentTimeMillis();
    log.info("includeDoc="+includeDoc+" doc="+ Arrays.toString(fields));
    log.info("doc="+ Arrays.toString(fields));
     log.info("iter="+iter +" time=" + (end-start) + " throughput=" + ((long)iter*1000)/(end-start));
 
     //discard all the changes
- 
2.19.1.windows.1

