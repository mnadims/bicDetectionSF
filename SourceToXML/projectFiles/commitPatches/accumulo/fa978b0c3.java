From fa978b0c3097e7d94fe1c8e9a49db24175711221 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 6 Nov 2014 23:38:37 -0500
Subject: [PATCH] ACCUMULO-3312 Fix incorrect path generation during clone
 table.

Expanded some ITs to actually cover this in the future.
--
 .../server/util/MetadataTableUtil.java        |  2 +-
 .../accumulo/test/functional/CloneTestIT.java | 90 ++++++++++++++++++-
 2 files changed, 90 insertions(+), 2 deletions(-)

diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java b/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
index 9cd02eb6a..6efd68c0d 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/MetadataTableUtil.java
@@ -882,7 +882,7 @@ public class MetadataTableUtil {
       Key k = entry.getKey();
       Mutation m = new Mutation(k.getRow());
       m.putDelete(k.getColumnFamily(), k.getColumnQualifier());
      String dir = volumeManager.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + tableId
      String dir = volumeManager.choose(ServerConstants.getBaseUris()) + Constants.HDFS_TABLES_DIR + Path.SEPARATOR + tableId + Path.SEPARATOR
           + new String(FastFormat.toZeroPaddedString(dirCount++, 8, 16, Constants.CLONE_PREFIX_BYTES));
       TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.put(m, new Value(dir.getBytes(UTF_8)));
 
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/CloneTestIT.java b/test/src/test/java/org/apache/accumulo/test/functional/CloneTestIT.java
index fc6983163..505dd5af3 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/CloneTestIT.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/CloneTestIT.java
@@ -19,6 +19,8 @@ package org.apache.accumulo.test.functional;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertTrue;
 
import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
@@ -26,6 +28,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
import java.util.TreeSet;
 
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
@@ -37,17 +40,22 @@ import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
 import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.server.ServerConstants;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
 import org.junit.Assert;
 import org.junit.Test;
 
 /**
 * 
 *
  */
 public class CloneTestIT extends SimpleMacIT {
 
@@ -88,6 +96,8 @@ public class CloneTestIT extends SimpleMacIT {
 
     checkData(table2, c);
 
    checkMetadata(table2, c);

     HashMap<String,String> tableProps = new HashMap<String,String>();
     for (Entry<String,String> prop : c.tableOperations().getProperties(table2)) {
       tableProps.put(prop.getKey(), prop.getValue());
@@ -119,6 +129,48 @@ public class CloneTestIT extends SimpleMacIT {
     Assert.assertEquals(expected, actual);
   }
 
  private void checkMetadata(String table, Connector conn) throws Exception {
    Scanner s = conn.createScanner(MetadataTable.NAME, Authorizations.EMPTY);

    s.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);
    MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.fetch(s);
    String tableId = conn.tableOperations().tableIdMap().get(table);

    Assert.assertNotNull("Could not get table id for " + table, tableId);

    s.setRange(Range.prefix(tableId));

    Key k;
    Text cf = new Text(), cq = new Text();
    Configuration conf = new Configuration();
    int itemsInspected = 0;
    for (Entry<Key,Value> entry : s) {
      itemsInspected++;
      k = entry.getKey();
      k.getColumnFamily(cf);
      k.getColumnQualifier(cq);

      if (cf.equals(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME)) {
        Path p = new Path(cq.toString());
        // Will this actually work against HDFS?
        FileSystem fs = p.getFileSystem(conf);
        Assert.assertTrue("File does not exist: " + p, fs.exists(p));
      } else if (cf.equals(MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.getColumnFamily())) {
        Assert.assertEquals("Saw unexpected cq", MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN.getColumnQualifier(), cq);
        Path tabletDir = new Path(entry.getValue().toString());
        Path tableDir = tabletDir.getParent();
        Path tablesDir = tableDir.getParent();

        Assert.assertEquals(ServerConstants.TABLE_DIR, tablesDir.getName());
      } else {
        Assert.fail("Got unexpected key-value: " + entry);
        throw new RuntimeException();
      }
    }

    Assert.assertTrue("Expected to find metadata entries", itemsInspected > 0);
  }

   private BatchWriter writeData(String table1, Connector c) throws TableNotFoundException, MutationsRejectedException {
     BatchWriter bw = c.createBatchWriter(table1, new BatchWriterConfig());
 
@@ -199,4 +251,40 @@ public class CloneTestIT extends SimpleMacIT {
 
   }
 
  @Test
  public void testCloneWithSplits() throws Exception {
    Connector conn = getConnector();

    List<Mutation> mutations = new ArrayList<Mutation>();
    TreeSet<Text> splits = new TreeSet<Text>();
    for (int i = 0; i < 10; i++) {
      splits.add(new Text(Integer.toString(i)));
      Mutation m = new Mutation(Integer.toString(i));
      m.put("", "", "");
      mutations.add(m);
    }

    String[] tables = getUniqueNames(2);

    conn.tableOperations().create(tables[0]);

    conn.tableOperations().addSplits(tables[0], splits);

    BatchWriter bw = conn.createBatchWriter(tables[0], new BatchWriterConfig());
    bw.addMutations(mutations);
    bw.close();

    conn.tableOperations().clone(tables[0], tables[1], true, null, null);

    conn.tableOperations().deleteRows(tables[1], new Text("4"), new Text("8"));

    List<String> rows = Arrays.asList("0", "1", "2", "3", "4", "9");
    List<String> actualRows = new ArrayList<String>();
    for (Entry<Key,Value> entry : conn.createScanner(tables[1], Authorizations.EMPTY)) {
      actualRows.add(entry.getKey().getRow().toString());
    }
    
    Assert.assertEquals(rows, actualRows);
  }

 }
- 
2.19.1.windows.1

