From 5b3610693454d86909ab37391ea0c07014041e8c Mon Sep 17 00:00:00 2001
From: Alejandro Abdelnur <tucu@apache.org>
Date: Mon, 25 Jun 2012 21:36:17 +0000
Subject: [PATCH] MAPREDUCE-2289. Permissions race can make getStagingDir fail
 on local filesystem (ahmed via tucu)

git-svn-id: https://svn.apache.org/repos/asf/hadoop/common/trunk@1353750 13f79535-47bb-0310-9956-ffa450edef68
--
 hadoop-mapreduce-project/CHANGES.txt          |  3 +++
 .../hadoop/mapreduce/JobSubmissionFiles.java  | 26 +++++++++++++------
 2 files changed, 21 insertions(+), 8 deletions(-)

diff --git a/hadoop-mapreduce-project/CHANGES.txt b/hadoop-mapreduce-project/CHANGES.txt
index bc4a33d73c4..ab014f63560 100644
-- a/hadoop-mapreduce-project/CHANGES.txt
++ b/hadoop-mapreduce-project/CHANGES.txt
@@ -189,6 +189,9 @@ Branch-2 ( Unreleased changes )
     MAPREDUCE-4290. Fix Yarn Applicaiton Status to MR JobState conversion. 
     (Devaraj K via sseth)
 
    MAPREDUCE-2289. Permissions race can make getStagingDir fail on local filesystem 
    (ahmed via tucu)

 Release 2.0.0-alpha - 05-23-2012
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmissionFiles.java b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmissionFiles.java
index b084d1ccde1..a4ea1d80a08 100644
-- a/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmissionFiles.java
++ b/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/JobSubmissionFiles.java
@@ -27,12 +27,18 @@
 import org.apache.hadoop.security.UserGroupInformation;
 import org.apache.hadoop.classification.InterfaceAudience;
 import org.apache.hadoop.conf.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 /**
  * A utility to manage job submission files.
  */
 @InterfaceAudience.Private
 public class JobSubmissionFiles {
 
  private final static Log LOG = LogFactory.getLog(JobSubmissionFiles.class);

   // job submission directory is private!
   final public static FsPermission JOB_DIR_PERMISSION =
     FsPermission.createImmutable((short) 0700); // rwx--------
@@ -102,14 +108,18 @@ public static Path getStagingDir(Cluster cluster, Configuration conf)
     if (fs.exists(stagingArea)) {
       FileStatus fsStatus = fs.getFileStatus(stagingArea);
       String owner = fsStatus.getOwner();
      if (!(owner.equals(currentUser) || owner.equals(realUser)) || 
          !fsStatus.getPermission().equals(JOB_DIR_PERMISSION)) {
         throw new IOException("The ownership/permissions on the staging " +
                      "directory " + stagingArea + " is not as expected. " + 
                      "It is owned by " + owner + " and permissions are "+ 
                      fsStatus.getPermission() + ". The directory must " +
      if (!(owner.equals(currentUser) || owner.equals(realUser))) {
         throw new IOException("The ownership on the staging directory " +
                      stagingArea + " is not as expected. " +
                      "It is owned by " + owner + ". The directory must " +
                       "be owned by the submitter " + currentUser + " or " +
                      "by " + realUser + " and permissions must be rwx------");
                      "by " + realUser);
      }
      if (!fsStatus.getPermission().equals(JOB_DIR_PERMISSION)) {
        LOG.info("Permissions on staging directory " + stagingArea + " are " +
          "incorrect: " + fsStatus.getPermission() + ". Fixing permissions " +
          "to correct value " + JOB_DIR_PERMISSION);
        fs.setPermission(stagingArea, JOB_DIR_PERMISSION);
       }
     } else {
       fs.mkdirs(stagingArea, 
@@ -118,4 +128,4 @@ public static Path getStagingDir(Cluster cluster, Configuration conf)
     return stagingArea;
   }
   
}
\ No newline at end of file
}
- 
2.19.1.windows.1

