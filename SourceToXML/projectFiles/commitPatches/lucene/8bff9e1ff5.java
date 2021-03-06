From 8bff9e1ff5bbc7310df92c53a71e0fb91f999e8d Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Tue, 15 Feb 2011 21:29:01 +0000
Subject: [PATCH] SOLR-1711: fix SUSS deadlock

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1071074 13f79535-47bb-0310-9956-ffa450edef68
--
 .../solrj/impl/StreamingUpdateSolrServer.java | 42 +++++++++++++++----
 1 file changed, 33 insertions(+), 9 deletions(-)

diff --git a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
index c47f4a09957..607480ed69f 100644
-- a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
++ b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
@@ -178,6 +178,8 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
 
         // remove it from the list of running things unless we are the last runner and the queue is full...
         // in which case, the next queue.put() would block and there would be no runners to handle it.
        // This case has been further handled by using offer instead of put, and using a retry loop
        // to avoid blocking forever (see request()).
         synchronized (runners) {
           if (runners.size() == 1 && queue.remainingCapacity() == 0) {
            // keep this runner alive
@@ -223,18 +225,40 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
         tmpLock.await();
       }
 
      queue.put( req );
      boolean success = queue.offer(req);
 
      synchronized( runners ) {
        if( runners.isEmpty()
                || (queue.remainingCapacity() < queue.size()
                && runners.size() < threadCount) )
        {
          Runner r = new Runner();
          scheduler.execute( r );
          runners.add( r );
      for(;;) {
        synchronized( runners ) {
          if( runners.isEmpty()
                  || (queue.remainingCapacity() < queue.size()    // queue is half full and we can add more runners
                  && runners.size() < threadCount) )
          {
            // We need more runners, so start a new one.
            Runner r = new Runner();
            runners.add( r );
            scheduler.execute( r );
          } else {
            // break out of the retry loop if we added the element to the queue successfully, *and*
            // while we are still holding the runners lock to prevent race conditions.
            // race conditions.
            if (success) break;
          }
        }

        // Retry to add to the queue w/o the runners lock held (else we risk temporary deadlock)
        // This retry could also fail because
        // 1) existing runners were not able to take off any new elements in the queue
        // 2) the queue was filled back up since our last try
        // If we succeed, the queue may have been completely emptied, and all runners stopped.
        // In all cases, we should loop back to the top to see if we need to start more runners.
        //
        if (!success) {
          success = queue.offer(req, 100, TimeUnit.MILLISECONDS);
         }

       }


     }
     catch (InterruptedException e) {
       log.error( "interrupted", e );
- 
2.19.1.windows.1

