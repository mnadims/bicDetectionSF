From 9cee40724f739fc5e4591321c1c5adfb5364c5a1 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Fri, 4 Jul 2014 03:14:38 -0400
Subject: [PATCH] ACCUMULO-2974 Hadoop's StringUtils changes across 1 and 2, so
 switch it to commons-lang

--
 .../java/org/apache/accumulo/server/fs/VolumeManagerImpl.java   | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
index 2cdd3fe6f..6e8439fdb 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
++ b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
@@ -44,6 +44,7 @@ import org.apache.accumulo.core.volume.Volume;
 import org.apache.accumulo.core.volume.VolumeConfiguration;
 import org.apache.accumulo.server.conf.ServerConfiguration;
 import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.ContentSummary;
 import org.apache.hadoop.fs.FSDataInputStream;
@@ -56,7 +57,6 @@ import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.hdfs.DFSConfigKeys;
 import org.apache.hadoop.hdfs.DistributedFileSystem;
 import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.StringUtils;
 import org.apache.log4j.Logger;
 
 import com.google.common.collect.HashMultimap;
- 
2.19.1.windows.1

