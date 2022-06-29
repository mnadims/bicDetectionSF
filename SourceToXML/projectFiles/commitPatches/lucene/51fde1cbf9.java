From 51fde1cbf954b6f67283ad945525e8c6b5197fb9 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Fri, 1 Jul 2016 13:16:46 +0530
Subject: [PATCH] SOLR-9262: Connection and read timeouts are being ignored by
 UpdateShardHandler after SOLR-4509

--
 solr/CHANGES.txt                                    |  7 ++++++-
 .../org/apache/solr/update/UpdateShardHandler.java  | 13 ++++++++++++-
 .../solr/client/solrj/impl/HttpClientUtil.java      |  6 +-----
 3 files changed, 19 insertions(+), 7 deletions(-)

diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 0011c766c3d..7a4a86dab6e 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -37,7 +37,12 @@ Upgrading from Solr 6.x
 * HttpSolrClient#setDefaultMaxConnectionsPerHost and
   HttpSolrClient#setMaxTotalConnections have been removed. These now default very
   high and can only be changed via param when creating an HttpClient instance.
  

Bug Fixes
----------------------
* SOLR-9262: Connection and read timeouts are being ignored by UpdateShardHandler after SOLR-4509.
  (Mark Miller, shalin)

 Optimizations
 ----------------------
 
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
index 5cc77d2ecc9..30e31cac6b1 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
@@ -60,8 +60,19 @@ public class UpdateShardHandler {
     }
 
     ModifiableSolrParams clientParams = new ModifiableSolrParams();
    log.info("Creating UpdateShardHandler HTTP client with params: {}", clientParams);
    if (cfg != null)  {
      clientParams.set(HttpClientUtil.PROP_SO_TIMEOUT, cfg.getDistributedSocketTimeout());
      clientParams.set(HttpClientUtil.PROP_CONNECTION_TIMEOUT, cfg.getDistributedConnectionTimeout());
    }
     client = HttpClientUtil.createClient(clientParams, clientConnectionManager);

    // following is done only for logging complete configuration.
    // The maxConnections and maxConnectionsPerHost have already been specified on the connection manager
    if (cfg != null)  {
      clientParams.set(HttpClientUtil.PROP_MAX_CONNECTIONS, cfg.getMaxUpdateConnections());
      clientParams.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, cfg.getMaxUpdateConnectionsPerHost());
    }
    log.info("Created UpdateShardHandler HTTP client with params: {}", clientParams);
   }
   
   public HttpClient getHttpClient() {
diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
index b38d62d89a7..b6fa9bd6cee 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
@@ -221,10 +221,6 @@ public class HttpClientUtil {
       logger.debug("Creating new http client, config:" + config);
     }
  
    if (params.get(PROP_SO_TIMEOUT) != null || params.get(PROP_CONNECTION_TIMEOUT) != null) {
      throw new SolrException(ErrorCode.SERVER_ERROR, "The socket connect and read timeout cannot be set here and must be set");
    }
    
     cm.setMaxTotal(params.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS, 10000));
     cm.setDefaultMaxPerRoute(params.getInt(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 10000));
     cm.setValidateAfterInactivity(Integer.getInteger(VALIDATE_AFTER_INACTIVITY, VALIDATE_AFTER_INACTIVITY_DEFAULT));
@@ -261,7 +257,7 @@ public class HttpClientUtil {
     newHttpClientBuilder = newHttpClientBuilder.setKeepAliveStrategy(keepAliveStrat)
         .evictIdleConnections((long) Integer.getInteger(EVICT_IDLE_CONNECTIONS, EVICT_IDLE_CONNECTIONS_DEFAULT), TimeUnit.MILLISECONDS);
     
    HttpClientBuilder builder = setupBuilder(newHttpClientBuilder, params == null ? new ModifiableSolrParams() : params);
    HttpClientBuilder builder = setupBuilder(newHttpClientBuilder, params);
     
     HttpClient httpClient = builder.setConnectionManager(cm).build();
     
- 
2.19.1.windows.1

