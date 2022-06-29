From 05a802b3eb36bcaa6c68eb88b5d7135fed895074 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 26 Jan 2010 15:33:26 +0000
Subject: [PATCH] SOLR-1711: StreamingUpdateSolrServer race

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@903271 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                                  | 3 +++
 .../solr/client/solrj/impl/StreamingUpdateSolrServer.java    | 5 ++++-
 2 files changed, 7 insertions(+), 1 deletion(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index a517b0aae9a..7b185ddbfe9 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -162,6 +162,9 @@ Bug Fixes
 * SOLR-1667: PatternTokenizer does not reset attributes such as positionIncrementGap
   (Robert Muir via shalin)
 
* SOLR-1711: SolrJ - StreamingUpdateSolrServer had a race condition that
  could halt the streaming of documents. (Attila Babo via yonik)

 Other Changes
 ----------------------
 
diff --git a/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java b/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
index b328c861848..8254b93dacf 100644
-- a/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
++ b/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
@@ -99,6 +99,7 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
       log.info( "starting runner: {}" , this );
       PostMethod method = null;
       try {
        do {
         RequestEntity request = new RequestEntity() {
           // we don't know the length
           public long getContentLength() { return -1; }
@@ -159,6 +160,7 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
           msg.append( "request: "+method.getURI() );
           handleError( new Exception( msg.toString() ) );
         }
        }  while( ! queue.isEmpty());
       }
       catch (Throwable e) {
         handleError( e );
@@ -166,6 +168,7 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
       finally {
         try {
           // make sure to release the connection
          if(method != null)
           method.releaseConnection();
         }
         catch( Exception ex ){}
@@ -212,11 +215,11 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
 
       queue.put( req );
       
        synchronized( runners ) {
       if( runners.isEmpty() 
         || (queue.remainingCapacity() < queue.size() 
          && runners.size() < threadCount) ) 
       {
        synchronized( runners ) {
           Runner r = new Runner();
           scheduler.execute( r );
           runners.add( r );
- 
2.19.1.windows.1

