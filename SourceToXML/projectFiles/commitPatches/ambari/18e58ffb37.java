From 18e58ffb3759819155261f7e3e68491f330ecd02 Mon Sep 17 00:00:00 2001
From: oleewere <oleewere@gmail.com>
Date: Tue, 24 Jan 2017 15:24:59 +0100
Subject: [PATCH] AMBARI-19692. LDAP regression in Ambari 2.4: Login alias is
 not resolved during authentication (oleewere)

Change-Id: I91da4344bc8cbfdb4863c973312c75ac21464066
--
 .../authorization/AmbariLdapAuthenticationProvider.java     | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
index 552be1ec3b..a35e7ebf22 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/security/authorization/AmbariLdapAuthenticationProvider.java
@@ -194,18 +194,18 @@ public class AmbariLdapAuthenticationProvider implements AuthenticationProvider
   }
 
   private Integer getUserId(Authentication authentication) {
    String userName = authentication.getName();
    String userName = AuthorizationHelper.resolveLoginAliasToUserName(authentication.getName());
 
     UserEntity userEntity = userDAO.findLdapUserByName(userName);
 
     // lookup is case insensitive, so no need for string comparison
     if (userEntity == null) {
      LOG.info("user not found ");
      LOG.info("user not found ('{}')", userName);
       throw new InvalidUsernamePasswordCombinationException();
     }
 
     if (!userEntity.getActive()) {
      LOG.debug("User account is disabled");
      LOG.debug("User account is disabled ('{}')", userName);
 
       throw new InvalidUsernamePasswordCombinationException();
     }
- 
2.19.1.windows.1

