From ca0827a86235dbc4d7e00cc8426ebff9fcc2d421 Mon Sep 17 00:00:00 2001
From: Steve Loughran <stevel@apache.org>
Date: Fri, 11 Sep 2015 15:55:14 +0100
Subject: [PATCH] HADOOP-12324. Better exception reporting in SaslPlainServer. 
  (Mike Yoder via stevel)

--
 hadoop-common-project/hadoop-common/CHANGES.txt                | 3 +++
 .../main/java/org/apache/hadoop/security/SaslPlainServer.java  | 2 +-
 2 files changed, 4 insertions(+), 1 deletion(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index c04bfd06939..6ea2484279e 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -765,6 +765,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12384. Add "-direct" flag option for fs copy so that user can choose
     not to create "._COPYING_" file (J.Andreina via vinayakumarb)
 
    HADOOP-12324. Better exception reporting in SaslPlainServer.
    (Mike Yoder via stevel)

   OPTIMIZATIONS
 
     HADOOP-11785. Reduce the number of listStatus operation in distcp
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslPlainServer.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslPlainServer.java
index 7c74f4ad0b5..270b579324c 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslPlainServer.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/security/SaslPlainServer.java
@@ -105,7 +105,7 @@ public String getMechanismName() {
         authz = ac.getAuthorizedID();
       }
     } catch (Exception e) {
      throw new SaslException("PLAIN auth failed: " + e.getMessage(), e);
      throw new SaslException("PLAIN auth failed: " + e.toString(), e);
     } finally {
       completed = true;
     }
- 
2.19.1.windows.1

