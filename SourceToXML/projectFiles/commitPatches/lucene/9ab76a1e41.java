From 9ab76a1e41d7019fd07b16a79a587653cf6d76a4 Mon Sep 17 00:00:00 2001
From: Chris Hostetter <hossman@apache.org>
Date: Wed, 27 Apr 2016 15:40:41 -0700
Subject: [PATCH] SOLR-9040 / SOLR-4509: Fix default SchemaRegistryProvider so
 javax.net.ssl.* system properties are respected by default

--
 .../client/solrj/impl/HttpClientUtil.java     | 30 +++++++++++--------
 1 file changed, 18 insertions(+), 12 deletions(-)

diff --git a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
index 72297b3499e..6318527dc29 100644
-- a/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
++ b/solr/solrj/src/java/org/apache/solr/client/solrj/impl/HttpClientUtil.java
@@ -47,6 +47,7 @@ import org.apache.http.config.RegistryBuilder;
 import org.apache.http.conn.ConnectionKeepAliveStrategy;
 import org.apache.http.conn.socket.ConnectionSocketFactory;
 import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
 import org.apache.http.entity.HttpEntityWrapper;
 import org.apache.http.impl.client.BasicCredentialsProvider;
 import org.apache.http.impl.client.CloseableHttpClient;
@@ -122,8 +123,9 @@ public class HttpClientUtil {
   static {
     resetHttpClientBuilder();
   }
  

   public static abstract class SchemaRegistryProvider {
    /** Must be non-null */
     public abstract Registry<ConnectionSocketFactory> getSchemaRegistry();
   }
   
@@ -172,16 +174,22 @@ public class HttpClientUtil {
   }
   
   public static void resetHttpClientBuilder() {
    schemaRegistryProvider = new SchemaRegistryProvider() {

      @Override
      public Registry<ConnectionSocketFactory> getSchemaRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory> create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
      }
    };
    schemaRegistryProvider = new DefaultSchemaRegistryProvider();
     httpClientBuilder = SolrHttpClientBuilder.create();
  }
 
  private static final class DefaultSchemaRegistryProvider extends SchemaRegistryProvider {
    @Override
    public Registry<ConnectionSocketFactory> getSchemaRegistry() {
      // this mimics PoolingHttpClientConnectionManager's default behavior,
      // except that we explicitly use SSLConnectionSocketFactory.getSystemSocketFactory()
      // to pick up the system level default SSLContext (where javax.net.ssl.* properties
      // related to keystore & truststore are specified)
      RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.<ConnectionSocketFactory>create();
      builder.register("http", PlainConnectionSocketFactory.getSocketFactory());
      builder.register("https", SSLConnectionSocketFactory.getSystemSocketFactory());
      return builder.build();
    }
   }
   
   /**
@@ -192,9 +200,7 @@ public class HttpClientUtil {
    *          configuration (no additional configuration) is created. 
    */
   public static CloseableHttpClient createClient(SolrParams params) {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(schemaRegistryProvider.getSchemaRegistry());

    return createClient(params, cm);
    return createClient(params, new PoolingHttpClientConnectionManager(schemaRegistryProvider.getSchemaRegistry()));
   }
   
   public static CloseableHttpClient createClient(SolrParams params, PoolingHttpClientConnectionManager cm) {
- 
2.19.1.windows.1

