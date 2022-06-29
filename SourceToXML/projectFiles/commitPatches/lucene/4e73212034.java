From 4e73212034321bdcd7d0f710b36219911195c230 Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Thu, 16 Apr 2015 22:47:11 +0000
Subject: [PATCH] SOLR-7411: fix threadsafety bug in SearchHandler introduced
 in SOLR-7380

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1674163 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/handler/component/SearchHandler.java   | 10 ++++++----
 1 file changed, 6 insertions(+), 4 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java b/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
index 2c1346a52a5..6acc2d13a66 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SearchHandler.java
@@ -71,7 +71,7 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
   
   protected static Logger log = LoggerFactory.getLogger(SearchHandler.class);
 
  protected List<SearchComponent> components = null;
  protected volatile List<SearchComponent> components;
   private ShardHandlerFactory shardHandlerFactory ;
   private PluginInfo shfInfo;
   private SolrCore core;
@@ -191,20 +191,22 @@ public class SearchHandler extends RequestHandlerBase implements SolrCoreAware ,
   }
 
   public List<SearchComponent> getComponents() {
    if (components == null) {
    List<SearchComponent> result = components;  // volatile read
    if (result == null) {
       synchronized (this) {
         if (components == null) {
           initComponents();
         }
        result = components;
       }
     }
    return components;
    return result;
   }
 
   @Override
   public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception
   {
    if (components == null) getComponents();
    List<SearchComponent> components  = getComponents();
     ResponseBuilder rb = new ResponseBuilder(req, rsp, components);
     if (rb.requestInfo != null) {
       rb.requestInfo.setResponseBuilder(rb);
- 
2.19.1.windows.1

