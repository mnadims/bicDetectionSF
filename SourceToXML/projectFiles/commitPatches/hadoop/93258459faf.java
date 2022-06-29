From 93258459faf56bc84121ba99d20eaef95273329e Mon Sep 17 00:00:00 2001
From: Chris Nauroth <cnauroth@apache.org>
Date: Thu, 19 May 2016 22:00:21 -0700
Subject: [PATCH] HADOOP-13183. S3A proxy tests fail after httpclient/httpcore
 upgrade. Contributed by Steve Loughran.

--
 .../apache/hadoop/fs/s3a/TestS3AConfiguration.java   | 12 +++---------
 1 file changed, 3 insertions(+), 9 deletions(-)

diff --git a/hadoop-tools/hadoop-aws/src/test/java/org/apache/hadoop/fs/s3a/TestS3AConfiguration.java b/hadoop-tools/hadoop-aws/src/test/java/org/apache/hadoop/fs/s3a/TestS3AConfiguration.java
index 7928ea91474..4f3c7ae80be 100644
-- a/hadoop-tools/hadoop-aws/src/test/java/org/apache/hadoop/fs/s3a/TestS3AConfiguration.java
++ b/hadoop-tools/hadoop-aws/src/test/java/org/apache/hadoop/fs/s3a/TestS3AConfiguration.java
@@ -123,9 +123,7 @@ public void testProxyConnection() throws Exception {
       fs = S3ATestUtils.createTestFileSystem(conf);
       fail("Expected a connection error for proxy server at " + proxy);
     } catch (AmazonClientException e) {
      if (!e.getMessage().contains(proxy + " refused")) {
        throw e;
      }
      // expected
     }
   }
 
@@ -158,18 +156,14 @@ public void testAutomaticProxyPortSelection() throws Exception {
       fs = S3ATestUtils.createTestFileSystem(conf);
       fail("Expected a connection error for proxy server");
     } catch (AmazonClientException e) {
      if (!e.getMessage().contains("443")) {
        throw e;
      }
      // expected
     }
     conf.set(Constants.SECURE_CONNECTIONS, "false");
     try {
       fs = S3ATestUtils.createTestFileSystem(conf);
       fail("Expected a connection error for proxy server");
     } catch (AmazonClientException e) {
      if (!e.getMessage().contains("80")) {
        throw e;
      }
      // expected
     }
   }
 
- 
2.19.1.windows.1

