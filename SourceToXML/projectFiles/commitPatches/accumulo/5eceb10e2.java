From 5eceb10e281b61e1f2b8a27a9b1c28746c2f0fc3 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Wed, 2 Jul 2014 17:30:25 -0400
Subject: [PATCH] ACCUMULO-2974 Include the table id when constructing an
 absolute path from a relative.

Testing that the TabletGroupWatcher does the correct path is difficult, and also doesn't
prevent other callers from writing the same bug, so the fix is added to VolumeManagerImpl
with appropriate tests added to ensure failure happens.
--
 .../accumulo/server/fs/VolumeManagerImpl.java | 26 +++++-
 .../server/fs/VolumeManagerImplTest.java      | 85 +++++++++++++++++++
 .../accumulo/master/TabletGroupWatcher.java   |  6 +-
 3 files changed, 114 insertions(+), 3 deletions(-)
 create mode 100644 server/base/src/test/java/org/apache/accumulo/server/fs/VolumeManagerImplTest.java

diff --git a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
index 9ebdef481..2cdd3fe6f 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
++ b/server/base/src/main/java/org/apache/accumulo/server/fs/VolumeManagerImpl.java
@@ -37,6 +37,7 @@ import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyExtent;
import org.apache.accumulo.core.file.rfile.RFile;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.volume.NonConfiguredVolume;
 import org.apache.accumulo.core.volume.Volume;
@@ -55,6 +56,7 @@ import org.apache.hadoop.fs.permission.FsPermission;
 import org.apache.hadoop.hdfs.DFSConfigKeys;
 import org.apache.hadoop.hdfs.DistributedFileSystem;
 import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.StringUtils;
 import org.apache.log4j.Logger;
 
 import com.google.common.collect.HashMultimap;
@@ -538,10 +540,30 @@ public class VolumeManagerImpl implements VolumeManager {
       }
     }
 
    // normalize the path
    Path fullPath = new Path(defaultVolume.getBasePath(), fileType.getDirectory());
     if (path.startsWith("/"))
       path = path.substring(1);

    // ACCUMULO-2974 To ensure that a proper absolute path is created, the caller needs to include the table ID
    // in the relative path. Fail when this doesn't appear to happen.
    if (FileType.TABLE == fileType) {
      // Trailing slash doesn't create an additional element
      String[] pathComponents = StringUtils.split(path, Path.SEPARATOR_CHAR);

      // Is an rfile
      if (path.endsWith(RFile.EXTENSION)) {
        if (pathComponents.length < 3) {
          throw new IllegalArgumentException("Fewer components in file path than expected");
        }
      } else {
        // is a directory
        if (pathComponents.length < 2) {
          throw new IllegalArgumentException("Fewer components in directory path than expected");
        }
      }
    }

    // normalize the path
    Path fullPath = new Path(defaultVolume.getBasePath(), fileType.getDirectory());
     fullPath = new Path(fullPath, path);
 
     FileSystem fs = getVolumeByPath(fullPath).getFileSystem();
diff --git a/server/base/src/test/java/org/apache/accumulo/server/fs/VolumeManagerImplTest.java b/server/base/src/test/java/org/apache/accumulo/server/fs/VolumeManagerImplTest.java
new file mode 100644
index 000000000..f29d2208a
-- /dev/null
++ b/server/base/src/test/java/org/apache/accumulo/server/fs/VolumeManagerImplTest.java
@@ -0,0 +1,85 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.server.fs;

import java.util.Arrays;
import java.util.List;

import org.apache.accumulo.server.fs.VolumeManager.FileType;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class VolumeManagerImplTest {

  protected VolumeManager fs;

  @Before
  public void setup() throws Exception {
    fs = VolumeManagerImpl.getLocal(System.getProperty("user.dir"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void defaultTabletDirWithoutTableId() throws Exception {
    fs.getFullPath(FileType.TABLE, "/default_tablet/");
  }

  @Test(expected = IllegalArgumentException.class)
  public void tabletDirWithoutTableId() throws Exception {
    fs.getFullPath(FileType.TABLE, "/t-0000001/");
  }

  @Test(expected = IllegalArgumentException.class)
  public void defaultTabletFileWithoutTableId() throws Exception {
    fs.getFullPath(FileType.TABLE, "/default_tablet/C0000001.rf");
  }

  @Test(expected = IllegalArgumentException.class)
  public void tabletFileWithoutTableId() throws Exception {
    fs.getFullPath(FileType.TABLE, "/t-0000001/C0000001.rf");
  }

  @Test
  public void tabletDirWithTableId() throws Exception {
    String basePath = fs.getDefaultVolume().getBasePath();
    String scheme = fs.getDefaultVolume().getFileSystem().getUri().toURL().getProtocol();
    System.out.println(basePath);
    Path expectedBase = new Path(scheme + ":" + basePath, FileType.TABLE.getDirectory()); 
    List<String> pathsToTest = Arrays.asList("1/default_tablet", "1/default_tablet/", "1/t-0000001");
    for (String pathToTest : pathsToTest) {
      Path fullPath = fs.getFullPath(FileType.TABLE, pathToTest);
      Assert.assertEquals(new Path(expectedBase, pathToTest), fullPath);
    }
  }

  @Test
  public void tabletFileWithTableId() throws Exception {
    String basePath = fs.getDefaultVolume().getBasePath();
    String scheme = fs.getDefaultVolume().getFileSystem().getUri().toURL().getProtocol();
    System.out.println(basePath);
    Path expectedBase = new Path(scheme + ":" + basePath, FileType.TABLE.getDirectory()); 
    List<String> pathsToTest = Arrays.asList("1/default_tablet/C0000001.rf", "1/t-0000001/C0000001.rf");
    for (String pathToTest : pathsToTest) {
      Path fullPath = fs.getFullPath(FileType.TABLE, pathToTest);
      Assert.assertEquals(new Path(expectedBase, pathToTest), fullPath);
    }
  }
}
diff --git a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
index d72abd29d..fbc97381f 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
++ b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
@@ -80,6 +80,7 @@ import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tables.TableManager;
 import org.apache.accumulo.server.tablets.TabletTime;
 import org.apache.accumulo.server.util.MetadataTableUtil;
import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Text;
 import org.apache.thrift.TException;
 
@@ -512,7 +513,10 @@ class TabletGroupWatcher extends Daemon {
         } else if (key.compareColumnFamily(TabletsSection.CurrentLocationColumnFamily.NAME) == 0) {
           throw new IllegalStateException("Tablet " + key.getRow() + " is assigned during a merge!");
         } else if (TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.hasColumns(key)) {
          datafiles.add(new FileRef(entry.getValue().toString(), this.master.fs.getFullPath(FileType.TABLE, entry.getValue().toString())));
          // ACCUMULO-2974 Need to include the TableID when converting a relative path to an absolute path.
          // The value has the leading path separator already included so it doesn't need it included.
          datafiles.add(new FileRef(entry.getValue().toString(), this.master.fs.getFullPath(FileType.TABLE, Path.SEPARATOR + extent.getTableId()
              + entry.getValue().toString())));
           if (datafiles.size() > 1000) {
             MetadataTableUtil.addDeleteEntries(extent, datafiles, SystemCredentials.get());
             datafiles.clear();
- 
2.19.1.windows.1

