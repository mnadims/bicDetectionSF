From 04cc1d614d0783ba3302f9de239d5c3b41f2b2db Mon Sep 17 00:00:00 2001
From: Todd Lipcon <todd@apache.org>
Date: Fri, 6 Apr 2012 04:47:05 +0000
Subject: [PATCH] HADOOP-8251. Fix SecurityUtil.fetchServiceTicket after
 HADOOP-6941. Contributed by Todd Lipcon.

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1310168 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-common-project/hadoop-common/CHANGES.txt                 | 2 ++
 .../src/main/java/org/apache/hadoop/security/SecurityUtil.java  | 2 +-
 2 files changed, 3 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 863560552d5..1f839866277 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -324,6 +324,8 @@ Release 2.0.0 - UNRELEASED
     HADOOP-8243. Security support broken in CLI (manual) failover controller
     (todd)
 
    HADOOP-8251. Fix SecurityUtil.fetchServiceTicket after HADOOP-6941 (todd)

   BREAKDOWN OF HADOOP-7454 SUBTASKS
 
     HADOOP-7455. HA: Introduce HA Service Protocol Interface. (suresh)
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
index ad982bc56d3..b7d268699c9 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SecurityUtil.java
@@ -171,7 +171,7 @@ public static void fetchServiceTicket(URL remoteHost) throws IOException {
       } else {
         principalClass = Class.forName("sun.security.krb5.PrincipalName");
         credentialsClass = Class.forName("sun.security.krb5.Credentials");
        krb5utilClass = Class.forName("sun.security.jgss.krb5");
        krb5utilClass = Class.forName("sun.security.jgss.krb5.Krb5Util");
       }
       @SuppressWarnings("rawtypes")
       Constructor principalConstructor = principalClass.getConstructor(String.class, 
- 
2.19.1.windows.1

