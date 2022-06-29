From a46933e8ce4c1715c11e3e3283bf0e8c2b53b837 Mon Sep 17 00:00:00 2001
From: Xiaoyu Yao <xyao@apache.org>
Date: Wed, 25 Jan 2017 13:26:50 -0800
Subject: [PATCH] HADOOP-13988. KMSClientProvider does not work with WebHDFS
 and Apache Knox w/ProxyUser. Contributed by Greg Senia and Xiaoyu Yao.

--
 .../crypto/key/kms/KMSClientProvider.java       | 17 ++++++++++-------
 1 file changed, 10 insertions(+), 7 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
index df6768dac15..ccc896821fc 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/crypto/key/kms/KMSClientProvider.java
@@ -1071,10 +1071,9 @@ private Text getDelegationTokenService() throws IOException {
     return dtService;
   }
 
  private boolean currentUgiContainsKmsDt() throws IOException {
    // Add existing credentials from current UGI, since provider is cached.
    Credentials creds = UserGroupInformation.getCurrentUser().
        getCredentials();
  private boolean containsKmsDt(UserGroupInformation ugi) throws IOException {
    // Add existing credentials from the UGI, since provider is cached.
    Credentials creds = ugi.getCredentials();
     if (!creds.getAllTokens().isEmpty()) {
       org.apache.hadoop.security.token.Token<? extends TokenIdentifier>
           dToken = creds.getToken(getDelegationTokenService());
@@ -1096,11 +1095,15 @@ private UserGroupInformation getActualUgi() throws IOException {
     if (currentUgi.getRealUser() != null) {
       // Use real user for proxy user
       actualUgi = currentUgi.getRealUser();
    } else if (!currentUgiContainsKmsDt() &&
        !currentUgi.hasKerberosCredentials()) {
    }

    if (!containsKmsDt(actualUgi) &&
        !actualUgi.hasKerberosCredentials()) {
       // Use login user for user that does not have either
       // Kerberos credential or KMS delegation token for KMS operations
      actualUgi = currentUgi.getLoginUser();
      LOG.debug("using loginUser no KMS Delegation Token "
          + "no Kerberos Credentials");
      actualUgi = UserGroupInformation.getLoginUser();
     }
     return actualUgi;
   }
- 
2.19.1.windows.1

