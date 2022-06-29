From a5fedd9dc16f3f024257b36048f5d850669ed6dd Mon Sep 17 00:00:00 2001
From: Chetan Mehrotra <chetanm@apache.org>
Date: Fri, 14 Mar 2014 10:33:48 +0000
Subject: [PATCH] JCR-3748 - Allow configuring S3Backend programatically

Added an overloaded method which takes Properties instance

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1577475 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/aws/ext/ds/S3Backend.java       | 18 ++++++++++++++++--
 1 file changed, 16 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
index 8c302bed6..d8d40e08b 100644
-- a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
++ b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
@@ -18,6 +18,7 @@
 package org.apache.jackrabbit.aws.ext.ds;
 
 import java.io.File;
import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Collections;
@@ -109,12 +110,25 @@ public class S3Backend implements Backend {
         if (config == null) {
             config = Utils.DEFAULT_CONFIG_FILE;
         }
        Properties properties = null;
        try{
            properties = Utils.readConfig(config);
        }catch(IOException e){
            throw new DataStoreException("Could not initialize S3 from "
                    + config, e);
        }
        init(store, homeDir, properties);
    }

    public void init(CachingDataStore store, String homeDir, Properties prop)
            throws DataStoreException {

         ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
         try {
             startTime = new Date();
             Thread.currentThread().setContextClassLoader(
                 getClass().getClassLoader());
            prop = Utils.readConfig(config);
            this.prop = prop;
             if (LOG.isDebugEnabled()) {
                 LOG.debug("init");
             }
@@ -187,7 +201,7 @@ public class S3Backend implements Backend {
                 LOG.debug("  error ", e);
             }
             throw new DataStoreException("Could not initialize S3 from "
                + config, e);
                + prop, e);
         } finally {
             if (contextClassLoader != null) {
                 Thread.currentThread().setContextClassLoader(contextClassLoader);
- 
2.19.1.windows.1

