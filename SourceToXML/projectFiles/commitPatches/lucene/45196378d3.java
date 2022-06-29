From 45196378d3fbd18f610070316b8af2665bad4892 Mon Sep 17 00:00:00 2001
From: Noble Paul <noble@apache.org>
Date: Sun, 8 Mar 2015 18:09:25 +0000
Subject: [PATCH] SOLR-7073: rename the .jar files to .jar.bin so that the
 build scripts don't fail

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1665063 13f79535-47bb-0310-9956-ffa450edef68
--
 .../src/test-files/runtimecode/runtimelibs.jar   |   2 --
 .../test-files/runtimecode/runtimelibs.jar.bin   | Bin 0 -> 6860 bytes
 .../test-files/runtimecode/runtimelibs_v2.jar    |   2 --
 .../runtimecode/runtimelibs_v2.jar.bin           | Bin 0 -> 6582 bytes
 .../org/apache/solr/core/TestDynamicLoading.java |   8 +++-----
 5 files changed, 3 insertions(+), 9 deletions(-)
 delete mode 100644 solr/core/src/test-files/runtimecode/runtimelibs.jar
 create mode 100644 solr/core/src/test-files/runtimecode/runtimelibs.jar.bin
 delete mode 100644 solr/core/src/test-files/runtimecode/runtimelibs_v2.jar
 create mode 100644 solr/core/src/test-files/runtimecode/runtimelibs_v2.jar.bin

diff --git a/solr/core/src/test-files/runtimecode/runtimelibs.jar b/solr/core/src/test-files/runtimecode/runtimelibs.jar
deleted file mode 100644
index c28d361ccb5..00000000000
-- a/solr/core/src/test-files/runtimecode/runtimelibs.jar
++ /dev/null
@@ -1,2 +0,0 @@
AnyObjectId[55c835b234da9cfdd6161938475835af8e85c008] was removed in git history.
Apache SVN contains full history.
\ No newline at end of file
diff --git a/solr/core/src/test-files/runtimecode/runtimelibs.jar.bin b/solr/core/src/test-files/runtimecode/runtimelibs.jar.bin
new file mode 100644
index 0000000000000000000000000000000000000000..55c835b234da9cfdd6161938475835af8e85c008
GIT binary patch
literal 6860
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

literal 0
HcmV?d00001

diff --git a/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar b/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar
deleted file mode 100644
index 96f5ab5198e..00000000000
-- a/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar
++ /dev/null
@@ -1,2 +0,0 @@
AnyObjectId[226a9dbceea9e942e9e91a33225cc97f400416a5] was removed in git history.
Apache SVN contains full history.
\ No newline at end of file
diff --git a/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar.bin b/solr/core/src/test-files/runtimecode/runtimelibs_v2.jar.bin
new file mode 100644
index 0000000000000000000000000000000000000000..226a9dbceea9e942e9e91a33225cc97f400416a5
GIT binary patch
literal 6582
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

literal 0
HcmV?d00001

diff --git a/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java b/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
index b02801fdba2..22a96279724 100644
-- a/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
++ b/solr/core/src/test/org/apache/solr/core/TestDynamicLoading.java
@@ -21,7 +21,6 @@ package org.apache.solr.core;
 import org.apache.solr.client.solrj.SolrClient;
 import org.apache.solr.client.solrj.impl.HttpSolrClient;
 import org.apache.solr.cloud.AbstractFullDistribZkTestBase;
import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.handler.TestBlobHandler;
 import org.apache.solr.util.RESTfulServerProvider;
 import org.apache.solr.util.RestTestHarness;
@@ -35,7 +34,6 @@ import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
@@ -157,10 +155,10 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
     }
     ByteBuffer jar = null;
 
//     jar = persistZip("/tmp/runtimelibs.jar", TestDynamicLoading.class, RuntimeLibReqHandler.class, RuntimeLibResponseWriter.class, RuntimeLibSearchComponent.class);
//     jar = persistZip("/tmp/runtimelibs.jar.bin", TestDynamicLoading.class, RuntimeLibReqHandler.class, RuntimeLibResponseWriter.class, RuntimeLibSearchComponent.class);
 //    if(true) return;
 
    jar = getFileContent("runtimecode/runtimelibs.jar");
    jar = getFileContent("runtimecode/runtimelibs.jar.bin");
     TestBlobHandler.postAndCheck(cloudClient, baseURL, blobName, jar, 1);
 
     payload = "{\n" +
@@ -204,7 +202,7 @@ public class TestDynamicLoading extends AbstractFullDistribZkTestBase {
         "org.apache.solr.core.RuntimeLibSearchComponent", 10);
     compareValues(result, MemClassLoader.class.getName(), asList( "loader"));
 
    jar = getFileContent("runtimecode/runtimelibs_v2.jar");
    jar = getFileContent("runtimecode/runtimelibs_v2.jar.bin");
     TestBlobHandler.postAndCheck(cloudClient, baseURL, blobName, jar, 2);
     payload = "{\n" +
         "'update-runtimelib' : { 'name' : 'colltest' ,'version':2}\n" +
- 
2.19.1.windows.1

