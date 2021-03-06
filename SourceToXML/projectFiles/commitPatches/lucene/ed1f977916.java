From ed1f9779161d3c1aa1c9cd13b17b1c13f71b422e Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Thu, 17 Jun 2010 03:05:46 +0000
Subject: [PATCH] SOLR-1885 and SOLR-1711: release connections to prevent hang

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@955471 13f79535-47bb-0310-9956-ffa450edef68
--
 solr/CHANGES.txt                              |   6 +-
 .../solrj/impl/StreamingUpdateSolrServer.java | 126 +++++++++---------
 2 files changed, 68 insertions(+), 64 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 9e1638adf69..afff725a78a 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -280,8 +280,10 @@ Bug Fixes
   (Robert Muir via shalin)
 
 * SOLR-1711: SolrJ - StreamingUpdateSolrServer had a race condition that
  could halt the streaming of documents. (Attila Babo via yonik)
  
  could halt the streaming of documents. The original patch to fix this
  (never officially released) introduced another hanging bug due to
  connections not being released.  (Attila Babo, Erik Hetzner via yonik)  

 * SOLR-1748, SOLR-1747, SOLR-1746, SOLR-1745, SOLR-1744: Streams and Readers
   retrieved from ContentStreams are not closed in various places, resulting
   in file descriptor leaks.
diff --git a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
index 8254b93dacf..f979b84b132 100644
-- a/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
++ b/solr/src/solrj/org/apache/solr/client/solrj/impl/StreamingUpdateSolrServer.java
@@ -100,79 +100,81 @@ public class StreamingUpdateSolrServer extends CommonsHttpSolrServer
       PostMethod method = null;
       try {
         do {
        RequestEntity request = new RequestEntity() {
          // we don't know the length
          public long getContentLength() { return -1; }
          public String getContentType() { return ClientUtils.TEXT_XML; }
          public boolean isRepeatable()  { return false; }
  
          public void writeRequest(OutputStream out) throws IOException {
            try {
              OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
              writer.append( "<stream>" ); // can be anything...
              UpdateRequest req = queue.poll( 250, TimeUnit.MILLISECONDS );
              while( req != null ) {
                log.debug( "sending: {}" , req );
                req.writeXML( writer ); 
                
                // check for commit or optimize
                SolrParams params = req.getParams();
                if( params != null ) {
                  String fmt = null;
                  if( params.getBool( UpdateParams.OPTIMIZE, false ) ) {
                    fmt = "<optimize waitSearcher=\"%s\" waitFlush=\"%s\" />";
                  }
                  else if( params.getBool( UpdateParams.COMMIT, false ) ) {
                    fmt = "<commit waitSearcher=\"%s\" waitFlush=\"%s\" />";
                  }
                  if( fmt != null ) {
                    log.info( fmt );
                    writer.write( String.format( fmt, 
                        params.getBool( UpdateParams.WAIT_SEARCHER, false )+"",
                        params.getBool( UpdateParams.WAIT_FLUSH, false )+"") );
          try {
            RequestEntity request = new RequestEntity() {
              // we don't know the length
              public long getContentLength() { return -1; }
              public String getContentType() { return ClientUtils.TEXT_XML; }
              public boolean isRepeatable()  { return false; }
      
              public void writeRequest(OutputStream out) throws IOException {
                try {
                  OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
                  writer.append( "<stream>" ); // can be anything...
                  UpdateRequest req = queue.poll( 250, TimeUnit.MILLISECONDS );
                  while( req != null ) {
                    log.debug( "sending: {}" , req );
                    req.writeXML( writer ); 
                    
                    // check for commit or optimize
                    SolrParams params = req.getParams();
                    if( params != null ) {
                      String fmt = null;
                      if( params.getBool( UpdateParams.OPTIMIZE, false ) ) {
                        fmt = "<optimize waitSearcher=\"%s\" waitFlush=\"%s\" />";
                      }
                      else if( params.getBool( UpdateParams.COMMIT, false ) ) {
                        fmt = "<commit waitSearcher=\"%s\" waitFlush=\"%s\" />";
                      }
                      if( fmt != null ) {
                        log.info( fmt );
                        writer.write( String.format( fmt, 
                            params.getBool( UpdateParams.WAIT_SEARCHER, false )+"",
                            params.getBool( UpdateParams.WAIT_FLUSH, false )+"") );
                      }
                    }
                    
                    writer.flush();
                    req = queue.poll( 250, TimeUnit.MILLISECONDS );
                   }
                  writer.append( "</stream>" );
                  writer.flush();
                }
                catch (InterruptedException e) {
                  e.printStackTrace();
                 }
                
                writer.flush();
                req = queue.poll( 250, TimeUnit.MILLISECONDS );
               }
              writer.append( "</stream>" );
              writer.flush();
            };
          
            method = new PostMethod(_baseURL+updateUrl );
            method.setRequestEntity( request );
            method.setFollowRedirects( false );
            method.addRequestHeader( "User-Agent", AGENT );
            
            int statusCode = getHttpClient().executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
              StringBuilder msg = new StringBuilder();
              msg.append( method.getStatusLine().getReasonPhrase() );
              msg.append( "\n\n" );
              msg.append( method.getStatusText() );
              msg.append( "\n\n" );
              msg.append( "request: "+method.getURI() );
              handleError( new Exception( msg.toString() ) );
             }
            catch (InterruptedException e) {
              e.printStackTrace();
          } finally {
            try {
              // make sure to release the connection
              if(method != null)
                method.releaseConnection();
             }
            catch( Exception ex ){}
           }
        };
        
        method = new PostMethod(_baseURL+updateUrl );
        method.setRequestEntity( request );
        method.setFollowRedirects( false );
        method.addRequestHeader( "User-Agent", AGENT );
        
        int statusCode = getHttpClient().executeMethod(method);
        if (statusCode != HttpStatus.SC_OK) {
          StringBuilder msg = new StringBuilder();
          msg.append( method.getStatusLine().getReasonPhrase() );
          msg.append( "\n\n" );
          msg.append( method.getStatusText() );
          msg.append( "\n\n" );
          msg.append( "request: "+method.getURI() );
          handleError( new Exception( msg.toString() ) );
        }
        }  while( ! queue.isEmpty());
        } while( ! queue.isEmpty());
       }
       catch (Throwable e) {
         handleError( e );
       } 
       finally {
        try {
          // make sure to release the connection
          if(method != null)
          method.releaseConnection();
        }
        catch( Exception ex ){}
        
         // remove it from the list of running things...
         synchronized (runners) {
           runners.remove( this );
- 
2.19.1.windows.1

