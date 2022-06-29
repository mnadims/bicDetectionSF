From 082f8388edda4f99ca74ac299379012fee29f48b Mon Sep 17 00:00:00 2001
From: Chetan Mehrotra <chetanm@apache.org>
Date: Tue, 18 Mar 2014 07:27:00 +0000
Subject: [PATCH] JCR-3751 -  S3Backend fails to initializate from file system
 based configuration file

Applying patch from Shashank. Thanks!



git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1578774 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java    | 6 ++++--
 1 file changed, 4 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
index c14174c8d..ca2e39a23 100644
-- a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
++ b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/S3Backend.java
@@ -112,8 +112,10 @@ public class S3Backend implements Backend {
         //over config provided via file based config
         if(this.properties != null){
             initProps = this.properties;
        } else if (config == null) {
            config = Utils.DEFAULT_CONFIG_FILE;
        } else {
            if(config == null){
                config = Utils.DEFAULT_CONFIG_FILE;
            }
             try{
                 initProps = Utils.readConfig(config);
             }catch(IOException e){
- 
2.19.1.windows.1

