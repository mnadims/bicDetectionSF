From 4ab49a436522f87cd462a9eae20fe10fe5c28fb2 Mon Sep 17 00:00:00 2001
From: Colin Patrick Mccabe <cmccabe@cloudera.com>
Date: Wed, 5 Aug 2015 09:46:51 -0700
Subject: [PATCH] HADOOP-12302. Fix native compilation on Windows after
 HADOOP-7824 (Vinayakumar B via Colin P. McCabe)

--
 hadoop-common-project/hadoop-common/CHANGES.txt                | 3 +++
 .../main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c   | 2 ++
 2 files changed, 5 insertions(+)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 36d7321c6a5..3951a994ab8 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -1050,6 +1050,9 @@ Release 2.8.0 - UNRELEASED
     HADOOP-12274. Remove direct download link from BULIDING.txt.
     (Caleb Severn via aajisaka)
 
    HADOOP-12302. Fix native compilation on Windows after HADOOP-7824
    (Vinayakumar B via Colin P. McCabe)

 Release 2.7.2 - UNRELEASED
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
index a716a02a8f7..bd7784eff70 100644
-- a/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
++ b/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/io/nativeio/NativeIO.c
@@ -132,6 +132,7 @@ static void setStaticInt(JNIEnv *env, jclass clazz, char *field,
     }
 }
 
#ifdef UNIX
 /**
  * Initialises a list of java constants that are platform specific.
  * These are only initialized in UNIX.
@@ -187,6 +188,7 @@ static void consts_init(JNIEnv *env) {
   SET_INT_OR_RETURN(env, clazz, S_IWUSR);
   SET_INT_OR_RETURN(env, clazz, S_IXUSR);
 }
#endif
 
 static void stat_init(JNIEnv *env, jclass nativeio_class) {
   jclass clazz = NULL;
- 
2.19.1.windows.1

