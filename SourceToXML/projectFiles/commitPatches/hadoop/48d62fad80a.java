From 48d62fad80aaa13ee1a26fca14437722ed46da25 Mon Sep 17 00:00:00 2001
From: Andrew Wang <wang@apache.org>
Date: Mon, 17 Nov 2014 13:59:46 -0800
Subject: [PATCH] HADOOP-11311. Restrict uppercase key names from being created
 with JCEKS.

--
 hadoop-common-project/hadoop-common/CHANGES.txt       |  3 +++
 .../hadoop/crypto/key/JavaKeyStoreProvider.java       |  3 +++
 .../hadoop/crypto/key/TestKeyProviderFactory.java     | 11 +++++++++++
 3 files changed, 17 insertions(+)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index bc63c7507a1..e4cc8e7f0c4 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -444,6 +444,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11157. ZKDelegationTokenSecretManager never shuts down
     listenerThreadPool. (Arun Suresh via atm)
 
    HADOOP-11311. Restrict uppercase key names from being created with JCEKS.
    (wang)

 Release 2.6.0 - 2014-11-18
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/JavaKeyStoreProvider.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/JavaKeyStoreProvider.java
index ac18e1653bb..75981c473ba 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/JavaKeyStoreProvider.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/JavaKeyStoreProvider.java
@@ -18,6 +18,7 @@
 
 package org.apache.hadoop.crypto.key;
 
import com.google.common.base.Preconditions;
 import org.apache.commons.io.IOUtils;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.classification.InterfaceAudience.Private;
@@ -423,6 +424,8 @@ public Metadata getMetadata(String name) throws IOException {
   @Override
   public KeyVersion createKey(String name, byte[] material,
                                Options options) throws IOException {
    Preconditions.checkArgument(name.equals(name.toLowerCase()),
        "Uppercase key names are unsupported: %s", name);
     writeLock.lock();
     try {
       try {
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/crypto/key/TestKeyProviderFactory.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/crypto/key/TestKeyProviderFactory.java
index ec1fc592635..998cd6fe122 100644
-- a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/crypto/key/TestKeyProviderFactory.java
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/crypto/key/TestKeyProviderFactory.java
@@ -33,6 +33,7 @@
 import org.apache.hadoop.security.Credentials;
 import org.apache.hadoop.security.ProviderUtils;
 import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.test.GenericTestUtils;
 import org.junit.Assert;
 import org.junit.Before;
 import org.junit.Test;
@@ -328,6 +329,16 @@ public void testJksProvider() throws Exception {
     // check permission retention after explicit change
     fs.setPermission(path, new FsPermission("777"));
     checkPermissionRetention(conf, ourUrl, path);

    // Check that an uppercase keyname results in an error
    provider = KeyProviderFactory.getProviders(conf).get(0);
    try {
      provider.createKey("UPPERCASE", KeyProvider.options(conf));
      Assert.fail("Expected failure on creating key name with uppercase " +
          "characters");
    } catch (IllegalArgumentException e) {
      GenericTestUtils.assertExceptionContains("Uppercase key names", e);
    }
   }
 
   private void verifyAfterReload(File file, KeyProvider provider)
- 
2.19.1.windows.1

