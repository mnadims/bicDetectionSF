From 6aea26168a5b2c52cf5a2c3bc4b1f09351d4d153 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Fri, 22 Jul 2011 09:54:10 +0000
Subject: [PATCH] SOLR-2382-abstracted DIHWriter

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1149534 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solr/handler/dataimport/ContextImpl.java  |  2 +-
 .../solr/handler/dataimport/DocBuilder.java   | 22 +++++++++----------
 .../dataimport/EntityProcessorWrapper.java    |  2 +-
 3 files changed, 13 insertions(+), 13 deletions(-)

diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ContextImpl.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ContextImpl.java
index 1b32005ff69..899ced532c0 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ContextImpl.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/ContextImpl.java
@@ -100,7 +100,7 @@ public class ContextImpl extends Context {
     if (entity.dataSrc != null && docBuilder != null && docBuilder.verboseDebug &&
              Context.FULL_DUMP.equals(currentProcess())) {
       //debug is not yet implemented properly for deltas
      entity.dataSrc = docBuilder.writer.getDebugLogger().wrapDs(entity.dataSrc);
      entity.dataSrc = docBuilder.getDebugLogger().wrapDs(entity.dataSrc);
     }
     return entity.dataSrc;
   }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
index f1ceba4ef28..979de008071 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -105,7 +105,7 @@ public class DocBuilder {
 
 
 
  private DebugLogger getDebubLogger(){
  DebugLogger getDebugLogger(){
     if (debugLogger == null) {
       debugLogger = new DebugLogger();
     }
@@ -596,11 +596,11 @@ public class DocBuilder {
     Context.CURRENT_CONTEXT.set(ctx);
     
     if (requestParameters.start > 0) {
      getDebubLogger().log(DIHLogLevels.DISABLE_LOGGING, null, null);
      getDebugLogger().log(DIHLogLevels.DISABLE_LOGGING, null, null);
     }
 
     if (verboseDebug) {
      getDebubLogger().log(DIHLogLevels.START_ENTITY, entity.name, null);
      getDebugLogger().log(DIHLogLevels.START_ENTITY, entity.name, null);
     }
 
     int seenDocCount = 0;
@@ -614,11 +614,11 @@ public class DocBuilder {
           seenDocCount++;
 
           if (seenDocCount > requestParameters.start) {
            getDebubLogger().log(DIHLogLevels.ENABLE_LOGGING, null, null);
            getDebugLogger().log(DIHLogLevels.ENABLE_LOGGING, null, null);
           }
 
           if (verboseDebug && entity.isDocRoot) {
            getDebubLogger().log(DIHLogLevels.START_DOC, entity.name, null);
            getDebugLogger().log(DIHLogLevels.START_DOC, entity.name, null);
           }
           if (doc == null && entity.isDocRoot) {
             doc = new DocWrapper();
@@ -647,7 +647,7 @@ public class DocBuilder {
           }
 
           if (verboseDebug) {
            getDebubLogger().log(DIHLogLevels.ENTITY_OUT, entity.name, arow);
            getDebugLogger().log(DIHLogLevels.ENTITY_OUT, entity.name, arow);
           }
           importStatistics.rowsCount.incrementAndGet();
           if (doc != null) {
@@ -683,7 +683,7 @@ public class DocBuilder {
 
         } catch (DataImportHandlerException e) {
           if (verboseDebug) {
            getDebubLogger().log(DIHLogLevels.ENTITY_EXCEPTION, entity.name, e);
            getDebugLogger().log(DIHLogLevels.ENTITY_EXCEPTION, entity.name, e);
           }
           if(e.getErrCode() == DataImportHandlerException.SKIP_ROW){
             continue;
@@ -702,21 +702,21 @@ public class DocBuilder {
             throw e;
         } catch (Throwable t) {
           if (verboseDebug) {
            getDebubLogger().log(DIHLogLevels.ENTITY_EXCEPTION, entity.name, t);
            getDebugLogger().log(DIHLogLevels.ENTITY_EXCEPTION, entity.name, t);
           }
           throw new DataImportHandlerException(DataImportHandlerException.SEVERE, t);
         } finally {
           if (verboseDebug) {
            getDebubLogger().log(DIHLogLevels.ROW_END, entity.name, null);
            getDebugLogger().log(DIHLogLevels.ROW_END, entity.name, null);
             if (entity.isDocRoot)
              getDebubLogger().log(DIHLogLevels.END_DOC, null, null);
              getDebugLogger().log(DIHLogLevels.END_DOC, null, null);
             Context.CURRENT_CONTEXT.remove();
           }
         }
       }
     } finally {
       if (verboseDebug) {
        getDebubLogger().log(DIHLogLevels.END_ENTITY, null, null);
        getDebugLogger().log(DIHLogLevels.END_ENTITY, null, null);
       }
       entityProcessor.destroy();
     }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorWrapper.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorWrapper.java
index c85dec109a8..db9d896bbd8 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorWrapper.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/EntityProcessorWrapper.java
@@ -83,7 +83,7 @@ public class EntityProcessorWrapper extends EntityProcessor {
       @Override
       public boolean add(Transformer transformer) {
         if (docBuilder != null && docBuilder.verboseDebug) {
          transformer = docBuilder.writer.getDebugLogger().wrapTransformer(transformer);
          transformer = docBuilder.getDebugLogger().wrapTransformer(transformer);
         }
         return super.add(transformer);
       }
- 
2.19.1.windows.1

