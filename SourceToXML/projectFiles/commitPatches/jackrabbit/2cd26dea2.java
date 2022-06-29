From 2cd26dea24f4a5a5c78aa4badd07ee8b3e35b36a Mon Sep 17 00:00:00 2001
From: Chetan Mehrotra <chetanm@apache.org>
Date: Fri, 14 Mar 2014 11:15:58 +0000
Subject: [PATCH] JCR-3748 - Allow configuring S3Backend programatically

As CachingDataStore invokes init directly its not possible to use the other init method. Added another way where clients can provide the required properties while creating S3DataStore

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1577481 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/aws/ext/ds/S3Backend.java      | 38 +++++++++++++------
 .../jackrabbit/aws/ext/ds/S3DataStore.java    | 15 +++++++-
 2 files changed, 40 insertions(+), 13 deletions(-)

diff --git a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
index d8d40e08b..c14174c8d 100644
-- a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
++ b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
@@ -96,7 +96,7 @@ public class S3Backend implements Backend {
 
     private CachingDataStore store;
 
    private Properties prop;
    private Properties properties;
 
     private Date startTime;
 
@@ -107,17 +107,22 @@ public class S3Backend implements Backend {
     @Override
     public void init(CachingDataStore store, String homeDir, String config)
             throws DataStoreException {
        if (config == null) {
        Properties initProps = null;
        //Check is configuration is already provided. That takes precedence
        //over config provided via file based config
        if(this.properties != null){
            initProps = this.properties;
        } else if (config == null) {
             config = Utils.DEFAULT_CONFIG_FILE;
            try{
                initProps = Utils.readConfig(config);
            }catch(IOException e){
                throw new DataStoreException("Could not initialize S3 from "
                        + config, e);
            }
            this.properties = initProps;
         }
        Properties properties = null;
        try{
            properties = Utils.readConfig(config);
        }catch(IOException e){
            throw new DataStoreException("Could not initialize S3 from "
                    + config, e);
        }
        init(store, homeDir, properties);
        init(store, homeDir, initProps);
     }
 
     public void init(CachingDataStore store, String homeDir, Properties prop)
@@ -128,7 +133,6 @@ public class S3Backend implements Backend {
             startTime = new Date();
             Thread.currentThread().setContextClassLoader(
                 getClass().getClassLoader());
            this.prop = prop;
             if (LOG.isDebugEnabled()) {
                 LOG.debug("init");
             }
@@ -548,6 +552,16 @@ public class S3Backend implements Backend {
         this.bucket = bucket;
     }
 
    /**
     * Properties used to configure the backend. If provided explicitly
     * before init is invoked then these take precedence
     *
     * @param properties  to configure S3Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

     private void write(DataIdentifier identifier, File file,
             boolean asyncUpload, AsyncUploadCallback callback)
             throws DataStoreException {
@@ -650,7 +664,7 @@ public class S3Backend implements Backend {
             ObjectListing prevObjectListing = s3service.listObjects(bucket,
                 KEY_PREFIX);
             List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
            int nThreads = Integer.parseInt(prop.getProperty("maxConnections"));
            int nThreads = Integer.parseInt(properties.getProperty("maxConnections"));
             ExecutorService executor = Executors.newFixedThreadPool(nThreads,
                 new NamedThreadFactory("s3-object-rename-worker"));
             boolean taskAdded = false;
diff --git a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3DataStore.java b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3DataStore.java
index da3e90754..8253572b2 100644
-- a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3DataStore.java
++ b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3DataStore.java
@@ -16,6 +16,8 @@
  */
 package org.apache.jackrabbit.aws.ext.ds;
 
import java.util.Properties;

 import org.apache.jackrabbit.core.data.Backend;
 import org.apache.jackrabbit.core.data.CachingDataStore;
 
@@ -23,10 +25,15 @@ import org.apache.jackrabbit.core.data.CachingDataStore;
  * An Amazon S3 data store.
  */
 public class S3DataStore extends CachingDataStore {
    private Properties properties;
 
     @Override
     protected Backend createBackend() {
        return new S3Backend();
        S3Backend backend = new S3Backend();
        if(properties != null){
            backend.setProperties(properties);
        }
        return backend;
     }
 
     @Override
@@ -34,4 +41,10 @@ public class S3DataStore extends CachingDataStore {
         return "s3.init.done";
     }
 
    /**
     * Properties required to configure the S3Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
 }
- 
2.19.1.windows.1

