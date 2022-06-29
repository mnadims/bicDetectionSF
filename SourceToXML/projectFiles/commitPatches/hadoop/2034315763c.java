From 2034315763cd7b1eb77e96c719918fc14e2dabf6 Mon Sep 17 00:00:00 2001
From: Xiaoyu Yao <xyao@apache.org>
Date: Thu, 26 Jan 2017 20:34:32 -0800
Subject: [PATCH] HADOOP-14029. Fix KMSClientProvider for non-secure proxyuser
 use case. Contributed by Xiaoyu Yao.

--
 .../hadoop/crypto/key/kms/KMSClientProvider.java      | 11 ++++++-----
 .../apache/hadoop/crypto/key/kms/server/TestKMS.java  |  6 +++++-
 2 files changed, 11 insertions(+), 6 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
index ccc896821fc..4c6b62524e0 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
@@ -1096,13 +1096,14 @@ private UserGroupInformation getActualUgi() throws IOException {
       // Use real user for proxy user
       actualUgi = currentUgi.getRealUser();
     }

    if (!containsKmsDt(actualUgi) &&
    if (UserGroupInformation.isSecurityEnabled() &&
        !containsKmsDt(actualUgi) &&
         !actualUgi.hasKerberosCredentials()) {
      // Use login user for user that does not have either
      // Use login user is only necessary when Kerberos is enabled
      // but the actual user does not have either
       // Kerberos credential or KMS delegation token for KMS operations
      LOG.debug("using loginUser no KMS Delegation Token "
          + "no Kerberos Credentials");
      LOG.debug("Using loginUser when Kerberos is enabled but the actual user" +
          " does not have either KMS Delegation Token or Kerberos Credentials");
       actualUgi = UserGroupInformation.getLoginUser();
     }
     return actualUgi;
diff --git a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
index 3a2d53c42da..72301db1f24 100644
-- a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
++ b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
@@ -2419,7 +2419,11 @@ public Void run() throws Exception {
 
   public void doWebHDFSProxyUserTest(final boolean kerberos) throws Exception {
     Configuration conf = new Configuration();
    conf.set("hadoop.security.authentication", "kerberos");
    if (kerberos) {
      conf.set("hadoop.security.authentication", "kerberos");
    }
    UserGroupInformation.setConfiguration(conf);

     final File testDir = getTestDir();
     conf = createBaseKMSConf(testDir, conf);
     if (kerberos) {
- 
2.19.1.windows.1

