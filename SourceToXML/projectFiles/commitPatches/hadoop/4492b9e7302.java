From 4492b9e7302b8c84dddec9713d8148cc6183f46b Mon Sep 17 00:00:00 2001
From: Haohui Mai <wheat9@apache.org>
Date: Sun, 22 Nov 2015 16:47:44 -0800
Subject: [PATCH] HADOOP-12294. Remove the support of the deprecated dfs.umask.
 Contributed by Chang Li.

--
 .../hadoop-common/CHANGES.txt                  |  3 +++
 .../hadoop/fs/permission/FsPermission.java     | 18 ++----------------
 2 files changed, 5 insertions(+), 16 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 52dea6ef8b8..ebde7b7f309 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -35,6 +35,9 @@ Trunk (Unreleased)
     HADOOP-10787 Rename/remove non-HADOOP_*, etc from the shell scripts.
     (aw via vvasudev)
 
    HADOOP-12294. Remove the support of the deprecated dfs.umask.
    (Chang Li vha wheat9)

   NEW FEATURES
 
     HADOOP-6590. Add a username check for hadoop sub-commands (John Smith via
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
index 0258293823e..d4adbb59d94 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/permission/FsPermission.java
@@ -258,22 +258,8 @@ public static FsPermission getUMask(Configuration conf) {
         String error = "Unable to parse configuration " + UMASK_LABEL
             + " with value " + confUmask + " as " + type + " umask.";
         LOG.warn(error);
        
        // If oldUmask is not set, then throw the exception
        if (oldUmask == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(error);
        }
      }
        
      if(oldUmask != Integer.MIN_VALUE) { // Property was set with old key
        if (umask != oldUmask) {
          LOG.warn(DEPRECATED_UMASK_LABEL
              + " configuration key is deprecated. " + "Convert to "
              + UMASK_LABEL + ", using octal or symbolic umask "
              + "specifications.");
          // Old and new umask values do not match - Use old umask
          umask = oldUmask;
        }

        throw new IllegalArgumentException(error);
       }
     }
     
- 
2.19.1.windows.1

