From 3da0bedaec38714a62c3b7244e84bc4f981ddf45 Mon Sep 17 00:00:00 2001
From: cnauroth <cnauroth@apache.org>
Date: Wed, 5 Aug 2015 14:56:41 -0700
Subject: [PATCH] HADOOP-12304. Applications using FileContext fail with the
 default file system configured to be wasb/s3/etc. Contributed by Chris
 Nauroth.

--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../hadoop/fs/DelegateToFileSystem.java       | 19 ++++++++++--
 .../fs/azure/TestWasbUriAndConfiguration.java | 30 +++++++++++++++++--
 .../src/test/resources/azure-test.xml         |  4 +++
 4 files changed, 52 insertions(+), 4 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 3951a994ab8..7571cc501c0 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1076,6 +1076,9 @@ Release 2.7.2 - UNRELEASED
     HDFS-8767. RawLocalFileSystem.listStatus() returns null for UNIX pipefile.
     (kanaka kumar avvaru via wheat9)
 
    HADOOP-12304. Applications using FileContext fail with the default file
    system configured to be wasb/s3/etc. (cnauroth)

 Release 2.7.1 - 2015-07-06 
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
index 6b7f387b0fb..388e64b669c 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
@@ -46,12 +46,27 @@ protected DelegateToFileSystem(URI theUri, FileSystem theFsImpl,
       Configuration conf, String supportedScheme, boolean authorityRequired)
       throws IOException, URISyntaxException {
     super(theUri, supportedScheme, authorityRequired, 
        theFsImpl.getDefaultPort());
        getDefaultPortIfDefined(theFsImpl));
     fsImpl = theFsImpl;
     fsImpl.initialize(theUri, conf);
     fsImpl.statistics = getStatistics();
   }
 
  /**
   * Returns the default port if the file system defines one.
   * {@link FileSystem#getDefaultPort()} returns 0 to indicate the default port
   * is undefined.  However, the logic that consumes this value expects to
   * receive -1 to indicate the port is undefined, which agrees with the
   * contract of {@link URI#getPort()}.
   *
   * @param theFsImpl file system to check for default port
   * @return default port, or -1 if default port is undefined
   */
  private static int getDefaultPortIfDefined(FileSystem theFsImpl) {
    int defaultPort = theFsImpl.getDefaultPort();
    return defaultPort != 0 ? defaultPort : -1;
  }

   @Override
   public Path getInitialWorkingDirectory() {
     return fsImpl.getInitialWorkingDirectory();
@@ -239,4 +254,4 @@ public String getCanonicalServiceName() {
   public List<Token<?>> getDelegationTokens(String renewer) throws IOException {
     return Arrays.asList(fsImpl.addDelegationTokens(renewer, null));
   }
}
\ No newline at end of file
}
diff --git a/hadoop-tools/hadoop-azure/src/test/java/org/apache/hadoop/fs/azure/TestWasbUriAndConfiguration.java b/hadoop-tools/hadoop-azure/src/test/java/org/apache/hadoop/fs/azure/TestWasbUriAndConfiguration.java
index a4ca6fdda4e..25eee474de5 100644
-- a/hadoop-tools/hadoop-azure/src/test/java/org/apache/hadoop/fs/azure/TestWasbUriAndConfiguration.java
++ b/hadoop-tools/hadoop-azure/src/test/java/org/apache/hadoop/fs/azure/TestWasbUriAndConfiguration.java
@@ -18,6 +18,7 @@
 
 package org.apache.hadoop.fs.azure;
 
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertNotNull;
@@ -35,6 +36,8 @@
 import java.util.EnumSet;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.FileContext;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.fs.azure.AzureBlobStorageTestAccount.CreateOptions;
@@ -362,7 +365,7 @@ public void testNoUriAuthority() throws Exception {
         Configuration conf = testAccount.getFileSystem().getConf();
         String authority = testAccount.getFileSystem().getUri().getAuthority();
         URI defaultUri = new URI(defaultScheme, authority, null, null, null);
        conf.set("fs.default.name", defaultUri.toString());
        conf.set(FS_DEFAULT_NAME_KEY, defaultUri.toString());
         
         // Add references to file system implementations for wasb and wasbs.
         conf.addResource("azure-test.xml");
@@ -385,11 +388,34 @@ public void testNoUriAuthority() throws Exception {
     // authority for the Azure file system should throw.
     testAccount = AzureBlobStorageTestAccount.createMock();
     Configuration conf = testAccount.getFileSystem().getConf();
    conf.set("fs.default.name", "file:///");
    conf.set(FS_DEFAULT_NAME_KEY, "file:///");
     try {
       FileSystem.get(new URI("wasb:///random/path"), conf);
       fail("Should've thrown.");
     } catch (IllegalArgumentException e) {
     }
   }

  @Test
  public void testWasbAsDefaultFileSystemHasNoPort() throws Exception {
    try {
      testAccount = AzureBlobStorageTestAccount.createMock();
      Configuration conf = testAccount.getFileSystem().getConf();
      String authority = testAccount.getFileSystem().getUri().getAuthority();
      URI defaultUri = new URI("wasb", authority, null, null, null);
      conf.set(FS_DEFAULT_NAME_KEY, defaultUri.toString());
      conf.addResource("azure-test.xml");

      FileSystem fs = FileSystem.get(conf);
      assertTrue(fs instanceof NativeAzureFileSystem);
      assertEquals(-1, fs.getUri().getPort());

      AbstractFileSystem afs = FileContext.getFileContext(conf)
          .getDefaultFileSystem();
      assertTrue(afs instanceof Wasb);
      assertEquals(-1, afs.getUri().getPort());
    } finally {
      FileSystem.closeAll();
    }
  }
 }
diff --git a/hadoop-tools/hadoop-azure/src/test/resources/azure-test.xml b/hadoop-tools/hadoop-azure/src/test/resources/azure-test.xml
index 98e68c47824..75b466d5c8d 100644
-- a/hadoop-tools/hadoop-azure/src/test/resources/azure-test.xml
++ b/hadoop-tools/hadoop-azure/src/test/resources/azure-test.xml
@@ -35,4 +35,8 @@
     <value>true</value>
   </property>
   -->
  <property>
    <name>fs.AbstractFileSystem.wasb.impl</name>
    <value>org.apache.hadoop.fs.azure.Wasb</value>
  </property>
 </configuration>
- 
2.19.1.windows.1

