From 7e08c0f23f58aa143f0997f2472e8051175142e9 Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Mon, 15 Sep 2014 19:39:27 -0700
Subject: [PATCH] HADOOP-10868. Addendum

--
 .../authentication/util/ZKSignerSecretProvider.java        | 7 +++++--
 1 file changed, 5 insertions(+), 2 deletions(-)

diff --git a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
index 45d4d65307a..a17b6d495dc 100644
-- a/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
++ b/hadoop-common-project/hadoop-auth/src/main/java/org/apache/hadoop/security/authentication/util/ZKSignerSecretProvider.java
@@ -139,6 +139,9 @@
       ZOOKEEPER_SIGNER_SECRET_PROVIDER_CURATOR_CLIENT_ATTRIBUTE =
       CONFIG_PREFIX + "curator.client";
 
  private static final String JAAS_LOGIN_ENTRY_NAME =
          "ZKSignerSecretProviderClient";

   private static Logger LOG = LoggerFactory.getLogger(
           ZKSignerSecretProvider.class);
   private String path;
@@ -384,7 +387,7 @@ protected CuratorFramework createCuratorClient(Properties config)
               + "and using 'sasl' ACLs");
       String principal = setJaasConfiguration(config);
       System.setProperty(ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY,
              "ZKSignerSecretProviderClient");
              JAAS_LOGIN_ENTRY_NAME);
       System.setProperty("zookeeper.authProvider.1",
               "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
       aclProvider = new SASLOwnerACLProvider(principal);
@@ -417,7 +420,7 @@ private String setJaasConfiguration(Properties config) throws Exception {
     // This is equivalent to writing a jaas.conf file and setting the system
     // property, "java.security.auth.login.config", to point to it
     JaasConfiguration jConf =
            new JaasConfiguration("Client", principal, keytabFile);
            new JaasConfiguration(JAAS_LOGIN_ENTRY_NAME, principal, keytabFile);
     Configuration.setConfiguration(jConf);
     return principal.split("[/@]")[0];
   }
- 
2.19.1.windows.1

