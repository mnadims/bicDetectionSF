From 81a77e1d6af5863bd42621329d0ad671327dbdfb Mon Sep 17 00:00:00 2001
From: "Eric C. Newton" <eric.newton@gmail.com>
Date: Wed, 6 Aug 2014 15:05:04 -0400
Subject: [PATCH] ACCUMULO-3047 delete entries created by deleterows were
 incorrectly de-relativized

--
 .../accumulo/master/TabletGroupWatcher.java   |  8 +-
 .../apache/accumulo/test/Accumulo3047IT.java  | 74 +++++++++++++++++++
 2 files changed, 80 insertions(+), 2 deletions(-)
 create mode 100644 test/src/test/java/org/apache/accumulo/test/Accumulo3047IT.java

diff --git a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
index fbc97381f..0a3d1d0b1 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
++ b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
@@ -515,8 +515,12 @@ class TabletGroupWatcher extends Daemon {
         } else if (TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.hasColumns(key)) {
           // ACCUMULO-2974 Need to include the TableID when converting a relative path to an absolute path.
           // The value has the leading path separator already included so it doesn't need it included.
          datafiles.add(new FileRef(entry.getValue().toString(), this.master.fs.getFullPath(FileType.TABLE, Path.SEPARATOR + extent.getTableId()
              + entry.getValue().toString())));
          String path = entry.getValue().toString();
          if (path.contains(":")) {
            datafiles.add(new FileRef(path));
          } else {
            datafiles.add(new FileRef(path, this.master.fs.getFullPath(FileType.TABLE, Path.SEPARATOR + extent.getTableId() + path)));
          }
           if (datafiles.size() > 1000) {
             MetadataTableUtil.addDeleteEntries(extent, datafiles, SystemCredentials.get());
             datafiles.clear();
diff --git a/test/src/test/java/org/apache/accumulo/test/Accumulo3047IT.java b/test/src/test/java/org/apache/accumulo/test/Accumulo3047IT.java
new file mode 100644
index 000000000..74730b2d2
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/Accumulo3047IT.java
@@ -0,0 +1,74 @@
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
package org.apache.accumulo.test;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.test.functional.ConfigurableMacIT;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.junit.Assert;
import org.junit.Test;

public class Accumulo3047IT extends ConfigurableMacIT {
  
  @Override
  public void configure(MiniAccumuloConfigImpl cfg, Configuration hadoopCoreSite) {
    cfg.setNumTservers(1);
    cfg.setProperty(Property.GC_CYCLE_DELAY, "1s");
    cfg.setProperty(Property.GC_CYCLE_START, "0s");
  }

  @Test(timeout= 60 * 1000)
  public void test() throws Exception {
    // make a table
    String tableName = getUniqueNames(1)[0];
    Connector c = getConnector();
    c.tableOperations().create(tableName);
    // add some splits
    SortedSet<Text> splits = new TreeSet<Text>();
    for (int i = 0; i < 10; i++) {
      splits.add(new Text("" + i));
    }
    c.tableOperations().addSplits(tableName, splits);
    // get rid of all the splits
    c.tableOperations().deleteRows(tableName, null, null);
    // get rid of the table
    c.tableOperations().delete(tableName);
    // let gc run
    UtilWaitThread.sleep(5 * 1000);
    // look for delete markers
    Scanner scanner = c.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
    scanner.setRange(MetadataSchema.DeletesSection.getRange());
    for (Entry<Key,Value> entry : scanner) {
      Assert.fail(entry.getKey().getRow().toString());
    }
  }
  
}
- 
2.19.1.windows.1

