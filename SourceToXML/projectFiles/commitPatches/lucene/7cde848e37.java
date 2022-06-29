From 7cde848e37bd7a7a38197d86ad520a14a59a364b Mon Sep 17 00:00:00 2001
From: Steven Rowe <sarowe@apache.org>
Date: Wed, 18 Mar 2015 15:54:47 +0000
Subject: [PATCH] SOLR-6141: fix schema update lock usage

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1667579 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/java/org/apache/solr/schema/SchemaManager.java    | 8 +++-----
 .../java/org/apache/solr/schema/ZkIndexSchemaReader.java  | 4 ++--
 2 files changed, 5 insertions(+), 7 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/schema/SchemaManager.java b/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
index c4c1e5b6a34..ae73d4f8ae4 100644
-- a/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
++ b/solr/core/src/java/org/apache/solr/schema/SchemaManager.java
@@ -421,11 +421,9 @@ public class SchemaManager {
       if (in instanceof ZkSolrResourceLoader.ZkByteArrayInputStream) {
         int version = ((ZkSolrResourceLoader.ZkByteArrayInputStream) in).getStat().getVersion();
         log.info("managed schema loaded . version : {} ", version);
        return new ManagedIndexSchema(req.getCore().getSolrConfig(),
            req.getSchema().getResourceName() ,new InputSource(in),
            true,
            req.getSchema().getResourceName(),
            version,new Object());
        return new ManagedIndexSchema
            (req.getCore().getSolrConfig(), req.getSchema().getResourceName(), new InputSource(in), 
                true, req.getSchema().getResourceName(), version, req.getSchema().getSchemaUpdateLock());
       } else {
         return (ManagedIndexSchema) req.getCore().getLatestSchema();
       }
diff --git a/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java b/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
index 5b44cd57925..7d08b297e35 100644
-- a/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
++ b/solr/core/src/java/org/apache/solr/schema/ZkIndexSchemaReader.java
@@ -108,8 +108,8 @@ public class ZkIndexSchemaReader implements OnReconnect {
           InputSource inputSource = new InputSource(new ByteArrayInputStream(data));
           String resourceName = managedIndexSchemaFactory.getManagedSchemaResourceName();
           ManagedIndexSchema newSchema = new ManagedIndexSchema
              (managedIndexSchemaFactory.getConfig(), resourceName, inputSource,
                  managedIndexSchemaFactory.isMutable(), resourceName, stat.getVersion(), new Object());
              (managedIndexSchemaFactory.getConfig(), resourceName, inputSource, managedIndexSchemaFactory.isMutable(), 
                  resourceName, stat.getVersion(), oldSchema.getSchemaUpdateLock());
           managedIndexSchemaFactory.setSchema(newSchema);
           long stop = System.nanoTime();
           log.info("Finished refreshing schema in " + TimeUnit.MILLISECONDS.convert(stop - start, TimeUnit.NANOSECONDS) + " ms");
- 
2.19.1.windows.1

