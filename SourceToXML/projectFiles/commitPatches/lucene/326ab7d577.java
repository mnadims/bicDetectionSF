From 326ab7d5774b57a001db2f64f4df9d0ae6a84a5a Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 26 Jan 2011 20:57:05 +0000
Subject: [PATCH] SOLR-1711: fix hang when queue is full but there are no
 runners

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1063869 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |  3 +-
 .../solrj/impl/StreamingUpdateSolrServer.java | 30 ++++++++++++-------
 2 files changed, 21 insertions(+), 12 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 683a722329b..a4308254de6 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -516,7 +516,8 @@ Bug Fixes
 * SOLR-1711: SolrJ - StreamingUpdateSolrServer had a race condition that
   could halt the streaming of documents. The original patch to fix this
   (never officially released) introduced another hanging bug due to
  connections not being released.  (Attila Babo, Erik Hetzner via yonik)
  connections not being released.
  (Attila Babo, Erik Hetzner, Johannes Tuchscherer via yonik)
   
 * SOLR-1748, SOLR-1747, SOLR-1746, SOLR-1745, SOLR-1744: Streams and Readers
   retrieved from ContentStreams are not closed in various places, resulting
diff --git a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
index 4460dfb2ce0..c47f4a09957 100644
-- a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
++ b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
@@ -173,12 +173,20 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
       }
       catch (Throwable e) {
         handleError( e );
      } 
      }
       finally {
        // remove it from the list of running things...

        // remove it from the list of running things unless we are the last runner and the queue is full...
        // in which case, the next queue.put() would block and there would be no runners to handle it.
         synchronized (runners) {
          runners.remove( this );
          if (runners.size() == 1 && queue.remainingCapacity() == 0) {
           // keep this runner alive
           scheduler.execute(this);
          } else {
            runners.remove( this );
          }
         }

         log.info( "finished: {}" , this );
         runnerLock.unlock();
       }
@@ -208,7 +216,7 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
         return super.request( request );
       }
     }
    

     try {
       CountDownLatch tmpLock = lock;
       if( tmpLock != null ) {
@@ -216,18 +224,18 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
       }
 
       queue.put( req );
      
        synchronized( runners ) {
      if( runners.isEmpty() 
        || (queue.remainingCapacity() < queue.size() 
         && runners.size() < threadCount) ) 
      {

      synchronized( runners ) {
        if( runners.isEmpty()
                || (queue.remainingCapacity() < queue.size()
                && runners.size() < threadCount) )
        {
           Runner r = new Runner();
           scheduler.execute( r );
           runners.add( r );
         }
       }
    } 
    }
     catch (InterruptedException e) {
       log.error( "interrupted", e );
       throw new IOException( e.getLocalizedMessage() );
- 
2.19.1.windows.1

