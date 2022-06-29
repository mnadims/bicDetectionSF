From b9d49761f72078a0a83137ba8197d08b71f385e0 Mon Sep 17 00:00:00 2001
From: Jason Lowe <jlowe@apache.org>
Date: Thu, 18 Dec 2014 21:27:28 +0000
Subject: [PATCH] HADOOP-11409. FileContext.getFileContext can stack overflow
 if default fs misconfigured. Contributed by Gera Shegalov

--
 .../hadoop-common/CHANGES.txt                 |  3 ++
 .../apache/hadoop/fs/AbstractFileSystem.java  | 11 +++--
 .../org/apache/hadoop/fs/FileContext.java     | 12 ++++--
 .../org/apache/hadoop/fs/TestFileContext.java | 41 +++++++++++++++++++
 4 files changed, 60 insertions(+), 7 deletions(-)
 create mode 100644 hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileContext.java

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 7cbac149f63..a81301bfc6e 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -617,6 +617,9 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11385. Prevent cross site scripting attack on JMXJSONServlet.
     (wheat9)
 
    HADOOP-11409. FileContext.getFileContext can stack overflow if default fs
    misconfigured (Gera Shegalov via jlowe)

 Release 2.6.0 - 2014-11-18
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/AbstractFileSystem.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/AbstractFileSystem.java
index a9a19cdc29b..f8ae27b3125 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/AbstractFileSystem.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/AbstractFileSystem.java
@@ -148,11 +148,14 @@ public boolean isValidName(String src) {
    */
   public static AbstractFileSystem createFileSystem(URI uri, Configuration conf)
       throws UnsupportedFileSystemException {
    Class<?> clazz = conf.getClass("fs.AbstractFileSystem." + 
                                uri.getScheme() + ".impl", null);
    final String fsImplConf = String.format("fs.AbstractFileSystem.%s.impl",
        uri.getScheme());

    Class<?> clazz = conf.getClass(fsImplConf, null);
     if (clazz == null) {
      throw new UnsupportedFileSystemException(
          "No AbstractFileSystem for scheme: " + uri.getScheme());
      throw new UnsupportedFileSystemException(String.format(
          "%s=null: No AbstractFileSystem configured for scheme: %s",
          fsImplConf, uri.getScheme()));
     }
     return (AbstractFileSystem) newInstance(clazz, uri, conf);
   }
diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
index 85f8136c0ac..e710ec02612 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/FileContext.java
@@ -457,9 +457,15 @@ public static FileContext getFileContext(final URI defaultFsUri,
    */
   public static FileContext getFileContext(final Configuration aConf)
       throws UnsupportedFileSystemException {
    return getFileContext(
      URI.create(aConf.get(FS_DEFAULT_NAME_KEY, FS_DEFAULT_NAME_DEFAULT)), 
      aConf);
    final URI defaultFsUri = URI.create(aConf.get(FS_DEFAULT_NAME_KEY,
        FS_DEFAULT_NAME_DEFAULT));
    if (   defaultFsUri.getScheme() != null
        && !defaultFsUri.getScheme().trim().isEmpty()) {
      return getFileContext(defaultFsUri, aConf);
    }
    throw new UnsupportedFileSystemException(String.format(
        "%s: URI configured via %s carries no scheme",
        defaultFsUri, FS_DEFAULT_NAME_KEY));
   }
 
   /**
diff --git a/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileContext.java b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileContext.java
new file mode 100644
index 00000000000..584ca40a3af
-- /dev/null
++ b/hadoop-common-project/hadoop-common/src/test/java/org/apache/hadoop/fs/TestFileContext.java
@@ -0,0 +1,41 @@
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TestFileContext {
  private static final Log LOG = LogFactory.getLog(TestFileContext.class);

  @Test
  public void testDefaultURIWithoutScheme() throws Exception {
    final Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, "/");
    try {
      FileContext.getFileContext(conf);
      fail(UnsupportedFileSystemException.class + " not thrown!");
    } catch (UnsupportedFileSystemException ufse) {
      LOG.info("Expected exception: ", ufse);
    }
  }
}
- 
2.19.1.windows.1

