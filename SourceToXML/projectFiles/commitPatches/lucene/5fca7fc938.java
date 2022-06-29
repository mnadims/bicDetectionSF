From 5fca7fc9381dd6ad8cc5b2252cb4c255b0e42929 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Mon, 1 Aug 2011 06:01:35 +0000
Subject: [PATCH] SOLR-2382 - regression . debug not working

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1152692 13f79535-47bb-0310-9956-ffa450edef68
--
 .../handler/dataimport/DataImportHandler.java | 21 +++++++------------
 .../solr/handler/dataimport/DataImporter.java |  8 ++++++-
 .../solr/handler/dataimport/DocBuilder.java   | 13 +++++++++++-
 .../solr/handler/dataimport/SolrWriter.java   |  2 --
 4 files changed, 27 insertions(+), 17 deletions(-)

diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
index bbf201bc34e..fd04012a978 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
@@ -74,8 +74,6 @@ public class DataImportHandler extends RequestHandlerBase implements
 
   private Map<String, Properties> dataSources = new HashMap<String, Properties>();
 
  private List<SolrInputDocument> debugDocuments;

   private boolean debugEnabled = true;
 
   private String myName = "dataimport";
@@ -197,16 +195,18 @@ public class DataImportHandler extends RequestHandlerBase implements
         UpdateRequestProcessor processor = processorChain.createProcessor(req, rsp);
         SolrResourceLoader loader = req.getCore().getResourceLoader();
         SolrWriter sw = getSolrWriter(processor, loader, requestParams, req);

        
         if (requestParams.debug) {
           if (debugEnabled) {
             // Synchronous request for the debug mode
             importer.runCmd(requestParams, sw);
             rsp.add("mode", "debug");
            rsp.add("documents", debugDocuments);
            if (sw.debugLogger != null)
              rsp.add("verbose-output", sw.debugLogger.output);
            debugDocuments = null;
            rsp.add("documents", requestParams.debugDocuments);
            if (requestParams.debugVerboseOutput != null) {
            	rsp.add("verbose-output", requestParams.debugVerboseOutput);
            }
            requestParams.debugDocuments = new ArrayList<SolrInputDocument>(0);
            requestParams.debugVerboseOutput = null;
           } else {
             message = DataImporter.MSG.DEBUG_NOT_ENABLED;
           }
@@ -215,7 +215,7 @@ public class DataImportHandler extends RequestHandlerBase implements
           if(requestParams.contentStream == null && !requestParams.syncMode){
             importer.runAsync(requestParams, sw);
           } else {
              importer.runCmd(requestParams, sw);
            importer.runCmd(requestParams, sw);
           }
         }
       } else if (DataImporter.RELOAD_CONF_CMD.equals(command)) {
@@ -285,11 +285,6 @@ public class DataImportHandler extends RequestHandlerBase implements
       @Override
       public boolean upload(SolrInputDocument document) {
         try {
          if (requestParams.debug) {
            if (debugDocuments == null)
              debugDocuments = new ArrayList<SolrInputDocument>();
            debugDocuments.add(document);
          }
           return super.upload(document);
         } catch (RuntimeException e) {
           LOG.error( "Exception while adding: " + document, e);
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
index 56ae340f1ae..78be6be8954 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImporter.java
@@ -18,11 +18,13 @@
 package org.apache.solr.handler.dataimport;
 
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.core.SolrConfig;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.common.util.SystemIdResolver;
 import org.apache.solr.common.util.XMLErrorLogger;
@@ -515,7 +517,7 @@ public class DataImporter {
     public String command = null;
 
     public boolean debug = false;

    
     public boolean verbose = false;
 
     public boolean syncMode = false;
@@ -537,6 +539,10 @@ public class DataImporter {
     public String dataConfig;
 
     public ContentStream contentStream;
    
    public List<SolrInputDocument> debugDocuments = new ArrayList<SolrInputDocument>(0);
    
    public NamedList debugVerboseOutput = null;
 
     public RequestParams() {
     }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
index 979de008071..dd112b87b95 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -72,11 +72,13 @@ public class DocBuilder {
   private static final String PARAM_WRITER_IMPL = "writerImpl";
   private static final String DEFAULT_WRITER_NAME = "SolrWriter";
   private DebugLogger debugLogger;

  private DataImporter.RequestParams reqParams;
  
     @SuppressWarnings("unchecked")
   public DocBuilder(DataImporter dataImporter, SolrWriter solrWriter, DIHPropertiesWriter propWriter, DataImporter.RequestParams reqParams) {
     INSTANCE.set(this);
     this.dataImporter = dataImporter;
    this.reqParams = reqParams;
     this.propWriter = propWriter;
     DataImporter.QUERY_COUNT.set(importStatistics.queryCount);
     requestParameters = reqParams;
@@ -262,6 +264,9 @@ public class DocBuilder {
 			if (writer != null) {
 	      writer.close();
 	    }
			if(requestParameters.debug) {
				requestParameters.debugVerboseOutput = getDebugLogger().output;	
			}
 		}
   }
 
@@ -514,6 +519,9 @@ public class DocBuilder {
                   LOG.debug("adding a doc "+docWrapper);
                 }
                 boolean result = writer.upload(docWrapper);
                if(reqParams.debug) {
                	reqParams.debugDocuments.add(docWrapper);
                }
                 docWrapper = null;
                 if (result){
                   importStatistics.docCount.incrementAndGet();
@@ -672,6 +680,9 @@ public class DocBuilder {
               return;
             if (!doc.isEmpty()) {
               boolean result = writer.upload(doc);
              if(reqParams.debug) {
              	reqParams.debugDocuments.add(doc);
              }
               doc = null;
               if (result){
                 importStatistics.docCount.incrementAndGet();
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
index 14b3c486ed6..8944a4cd988 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/SolrWriter.java
@@ -44,8 +44,6 @@ public class SolrWriter implements DIHWriter {
 
   private final UpdateRequestProcessor processor;
 
  DebugLogger debugLogger;

   SolrQueryRequest req;
 
   public SolrWriter(UpdateRequestProcessor processor, SolrQueryRequest req) {
- 
2.19.1.windows.1

