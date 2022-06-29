From 957f0311a160afb40dbb0619f455445b4f5d1e32 Mon Sep 17 00:00:00 2001
From: cnauroth <cnauroth@apache.org>
Date: Mon, 2 Nov 2015 22:25:05 -0800
Subject: [PATCH] HADOOP-12542. TestDNS fails on Windows after HADOOP-12437.
 Contributed by Chris Nauroth.

--
 hadoop-common-project/hadoop-common/CHANGES.txt      |  2 ++
 .../src/test/java/org/apache/hadoop/net/TestDNS.java | 12 +++++++++---
 2 files changed, 11 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index b0550693bda..0d1bce27d56 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1312,6 +1312,8 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12508. delete fails with exception when lease is held on blob.
     (Gaurav Kanade via cnauroth)
 
    HADOOP-12542. TestDNS fails on Windows after HADOOP-12437. (cnauroth)

   OPTIMIZATIONS
 
     HADOOP-12051. ProtobufRpcEngine.invoke() should use Exception.toString()
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
index b26c7caa61b..a0bfe73f9ae 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/net/TestDNS.java
@@ -30,6 +30,7 @@
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.Shell;
 import org.apache.hadoop.util.Time;
 
 import org.junit.Test;
@@ -37,6 +38,7 @@
 import static org.hamcrest.CoreMatchers.not;
 import static org.hamcrest.core.Is.is;
 import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
 
 /**
  * Test host name and IP resolution and caching.
@@ -185,13 +187,17 @@ public void testRDNS() throws Exception {
    *
    * This test may fail on some misconfigured test machines that don't have
    * an entry for "localhost" in their hosts file. This entry is correctly
   * configured out of the box on common Linux distributions, OS X and
   * Windows.
   * configured out of the box on common Linux distributions and OS X.
   *
   * Windows refuses to resolve 127.0.0.1 to "localhost" despite the presence of
   * this entry in the hosts file.  We skip the test on Windows to avoid
   * reporting a spurious failure.
    *
    * @throws Exception
    */
   @Test (timeout=60000)
   public void testLookupWithHostsFallback() throws Exception {
    assumeTrue(!Shell.WINDOWS);
     final String oldHostname = changeDnsCachedHostname(DUMMY_HOSTNAME);
 
     try {
@@ -231,7 +237,7 @@ public void testLookupWithoutHostsFallback() throws Exception {
 
   private String getLoopbackInterface() throws SocketException {
     return NetworkInterface.getByInetAddress(
        InetAddress.getLoopbackAddress()).getDisplayName();
        InetAddress.getLoopbackAddress()).getName();
   }
 
   /**
- 
2.19.1.windows.1

