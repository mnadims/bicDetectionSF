From dc065dd64ca3e101b0c0a7bcc7d7a067b77d6c82 Mon Sep 17 00:00:00 2001
From: Akira Ajisaka <aajisaka@apache.org>
Date: Tue, 19 Jul 2016 16:04:49 -0700
Subject: [PATCH] HADOOP-12991. Conflicting default ports in
 DelegateToFileSystem. Contributed by Kai Sasaki.

--
 .../hadoop/fs/DelegateToFileSystem.java       |   5 +-
 .../hadoop/fs/TestDelegateToFsCheckPath.java  | 126 ++++++++++++++++++
 2 files changed, 129 insertions(+), 2 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDelegateToFsCheckPath.java

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
index 388e64b669c..dd69b08c934 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/DelegateToFileSystem.java
@@ -40,6 +40,7 @@
 @InterfaceAudience.Private
 @InterfaceStability.Unstable
 public abstract class DelegateToFileSystem extends AbstractFileSystem {
  private static final int DELEGATE_TO_FS_DEFAULT_PORT = -1;
   protected final FileSystem fsImpl;
   
   protected DelegateToFileSystem(URI theUri, FileSystem theFsImpl,
@@ -64,7 +65,7 @@ protected DelegateToFileSystem(URI theUri, FileSystem theFsImpl,
    */
   private static int getDefaultPortIfDefined(FileSystem theFsImpl) {
     int defaultPort = theFsImpl.getDefaultPort();
    return defaultPort != 0 ? defaultPort : -1;
    return defaultPort != 0 ? defaultPort : DELEGATE_TO_FS_DEFAULT_PORT;
   }
 
   @Override
@@ -159,7 +160,7 @@ public Path getHomeDirectory() {
 
   @Override
   public int getUriDefaultPort() {
    return 0;
    return DELEGATE_TO_FS_DEFAULT_PORT;
   }
 
   @Override
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDelegateToFsCheckPath.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDelegateToFsCheckPath.java
new file mode 100644
index 00000000000..b1de316cf67
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestDelegateToFsCheckPath.java
@@ -0,0 +1,126 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.junit.Test;

/**
 * The default port of DelegateToFileSystem is set from child file system.
 */
public class TestDelegateToFsCheckPath {
  @Test
  public void testCheckPathWithoutDefaultPorts() throws URISyntaxException,
      IOException {
    URI uri = new URI("dummy://dummy-host");
    AbstractFileSystem afs = new DummyDelegateToFileSystem(uri);
    afs.checkPath(new Path("dummy://dummy-host"));
  }

  private static class DummyDelegateToFileSystem
      extends DelegateToFileSystem {
    public DummyDelegateToFileSystem(URI uri) throws URISyntaxException,
        IOException {
      super(uri, new UnOverrideDefaultPortFileSystem(), new Configuration(),
          "dummy", false);
    }
  }

  /**
   * UnOverrideDefaultPortFileSystem does not define default port.
   * The default port defined by AbstractFilesystem is used in this case.
   * (default 0).
   */
  private static class UnOverrideDefaultPortFileSystem extends FileSystem {
    @Override
    public URI getUri() {
      // deliberately empty
      return null;
    }

    @Override
    public FSDataInputStream open(Path f, int bufferSize) throws IOException {
      // deliberately empty
      return null;
    }
    @Override
    public FSDataOutputStream create(Path f, FsPermission permission,
        boolean overwrite, int bufferSize, short replication, long blockSize,
        Progressable progress) throws IOException {
      // deliberately empty
      return null;
    }

    @Override
    public FSDataOutputStream append(Path f, int bufferSize,
        Progressable progress) throws IOException {
      // deliberately empty
      return null;
    }

    @Override
    public boolean rename(Path src, Path dst) throws IOException {
      // deliberately empty
      return false;
    }

    @Override
    public boolean delete(Path f, boolean recursive) throws IOException {
      // deliberately empty
      return false;
    }

    @Override
    public FileStatus[] listStatus(Path f) throws FileNotFoundException,
        IOException {
      // deliberately empty
      return new FileStatus[0];
    }

    @Override
    public void setWorkingDirectory(Path newDir) {
      // deliberately empty
    }

    @Override
    public Path getWorkingDirectory() {
      // deliberately empty
      return null;
    }

    @Override
    public boolean mkdirs(Path f, FsPermission permission) throws IOException {
      // deliberately empty
      return false;
    }

    @Override
    public FileStatus getFileStatus(Path f) throws IOException {
      // deliberately empty
      return null;
    }
  }
}
- 
2.19.1.windows.1

