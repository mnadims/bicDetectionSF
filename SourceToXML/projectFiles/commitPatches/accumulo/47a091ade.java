From 47a091adec4a40cdf6852cc0f8c432c15034cb5e Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Thu, 15 Jan 2015 10:20:52 -0500
Subject: [PATCH] ACCUMULO-3467 fixed bug with concurrent compactions

--
 .../core/client/admin/CompactionConfig.java   |  9 ++---
 .../admin/CompactionStrategyConfig.java       | 10 ++++++
 .../core/client/admin/TableOperations.java    |  3 +-
 .../impl/CompactionStrategyConfigUtil.java    |  9 +++++
 .../master/tableOps/UserCompactionConfig.java |  1 +
 .../master/tableOps/CompactRange.java         | 12 +++++--
 .../test/UserCompactionStrategyIT.java        | 35 +++++++++++++++++++
 7 files changed, 69 insertions(+), 10 deletions(-)

diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
index 38e5efd6a..064d8363d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
@@ -20,9 +20,9 @@ package org.apache.accumulo.core.client.admin;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
import java.util.Map;
 
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
 import org.apache.hadoop.io.Text;
 
 import com.google.common.base.Preconditions;
@@ -39,12 +39,7 @@ public class CompactionConfig {
   private boolean flush = true;
   private boolean wait = true;
   private List<IteratorSetting> iterators = Collections.emptyList();
  private CompactionStrategyConfig compactionStrategy = new CompactionStrategyConfig("org.apache.accumulo.tserver.compaction.EverythingCompactionStrategy") {
    @Override
    public CompactionStrategyConfig setOptions(Map<String,String> opts) {
      throw new UnsupportedOperationException();
    }
  };
  private CompactionStrategyConfig compactionStrategy = CompactionStrategyConfigUtil.DEFAULT_STRATEGY;
 
   /**
    * @param start
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
index c23b5115b..0992ba9c8 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
@@ -71,4 +71,14 @@ public class CompactionStrategyConfig {
   public Map<String,String> getOptions() {
     return Collections.unmodifiableMap(options);
   }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CompactionStrategyConfig) {
      CompactionStrategyConfig ocsc = (CompactionStrategyConfig) o;
      return className.equals(ocsc.className) && options.equals(ocsc.options);
    }

    return false;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
index 5c1260ccf..41021b1e9 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
@@ -300,7 +300,8 @@ public interface TableOperations {
    * @param end
    *          last tablet to be merged contains this row, null means the last tablet in table
    * @param iterators
   *          A set of iterators that will be applied to each tablet compacted
   *          A set of iterators that will be applied to each tablet compacted. If two or more concurrent calls to compact pass iterators, then only one will
   *          succeed and the others will fail.
    * @param flush
    *          when true, table memory is flushed before compaction starts
    * @param wait
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java b/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
index 8dce87764..758f44547 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
@@ -25,12 +25,21 @@ import java.io.DataOutput;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.util.HashMap;
import java.util.Map;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
 
 public class CompactionStrategyConfigUtil {
 
  public static final CompactionStrategyConfig DEFAULT_STRATEGY = new CompactionStrategyConfig(
      "org.apache.accumulo.tserver.compaction.EverythingCompactionStrategy") {
    @Override
    public CompactionStrategyConfig setOptions(Map<String,String> opts) {
      throw new UnsupportedOperationException();
    }
  };

   private static final int MAGIC = 0xcc5e6024;
 
   public static void encode(DataOutput dout, CompactionStrategyConfig csc) throws IOException {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java b/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
index 98d7fd71e..02c6ac354 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
@@ -46,6 +46,7 @@ public class UserCompactionConfig implements Writable {
     startRow = null;
     endRow = null;
     iterators = Collections.emptyList();
    compactionStrategy = CompactionStrategyConfigUtil.DEFAULT_STRATEGY;
   }
 
   @Override
diff --git a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
index fd7decff7..580852d80 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
++ b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
@@ -31,6 +31,7 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.thrift.TableOperation;
 import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
@@ -61,6 +62,8 @@ import org.apache.log4j.Logger;
 import org.apache.thrift.TException;
 import org.apache.zookeeper.KeeperException.NoNodeException;
 
import com.google.common.base.Preconditions;

 class CompactionDriver extends MasterRepo {
 
   private static final long serialVersionUID = 1L;
@@ -207,11 +210,16 @@ public class CompactRange extends MasterRepo {
 
   public CompactRange(String tableId, byte[] startRow, byte[] endRow, List<IteratorSetting> iterators, CompactionStrategyConfig compactionStrategy)
       throws ThriftTableOperationException {

    Preconditions.checkNotNull(tableId, "Invalid argument: null tableId");
    Preconditions.checkNotNull(iterators, "Invalid argument: null iterator list");
    Preconditions.checkNotNull(compactionStrategy, "Invalid argument: null compactionStrategy");

     this.tableId = tableId;
     this.startRow = startRow.length == 0 ? null : startRow;
     this.endRow = endRow.length == 0 ? null : endRow;
 
    if (iterators.size() > 0 || compactionStrategy != null) {
    if (iterators.size() > 0 || !compactionStrategy.equals(CompactionStrategyConfigUtil.DEFAULT_STRATEGY)) {
       this.config = WritableUtils.toByteArray(new UserCompactionConfig(this.startRow, this.endRow, iterators, compactionStrategy));
     }
 
@@ -249,7 +257,7 @@ public class CompactRange extends MasterRepo {
               continue; // skip self
 
             throw new ThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.OTHER,
                "Another compaction with iterators is running");
                "Another compaction with iterators and/or a compaction strategy is running");
           }
 
           StringBuilder encodedIterators = new StringBuilder();
diff --git a/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java b/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java
index 5421f520b..7a3162bb7 100644
-- a/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java
++ b/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java
@@ -26,6 +26,7 @@ import java.util.Random;
 import java.util.Set;
 import java.util.TreeSet;
 
import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
@@ -44,6 +45,7 @@ import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.harness.AccumuloClusterIT;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.test.functional.FunctionalTestUtils;
import org.apache.accumulo.test.functional.SlowIterator;
 import org.apache.accumulo.tserver.compaction.CompactionPlan;
 import org.apache.accumulo.tserver.compaction.CompactionStrategy;
 import org.apache.accumulo.tserver.compaction.MajorCompactionRequest;
@@ -300,6 +302,39 @@ public class UserCompactionStrategyIT extends AccumuloClusterIT {
 
   }
 
  @Test
  public void testConcurrent() throws Exception {
    // two compactions without iterators or strategy should be able to run concurrently

    Connector c = getConnector();

    String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);

    // write random data because its very unlikely it will compress
    writeRandomValue(c, tableName, 1 << 16);
    writeRandomValue(c, tableName, 1 << 16);

    c.tableOperations().compact(tableName, new CompactionConfig().setWait(false));
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    Assert.assertEquals(1, FunctionalTestUtils.countRFiles(c, tableName));

    writeRandomValue(c, tableName, 1 << 16);

    IteratorSetting iterConfig = new IteratorSetting(30, SlowIterator.class);
    SlowIterator.setSleepTime(iterConfig, 1000);

    long t1 = System.currentTimeMillis();
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(false).setIterators(Arrays.asList(iterConfig)));
    try {
      // this compaction should fail because previous one set iterators
      c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));
      if (System.currentTimeMillis() - t1 < 2000)
        Assert.fail("Expected compaction to fail because another concurrent compaction set iterators");
    } catch (AccumuloException e) {}
  }

   void writeRandomValue(Connector c, String tableName, int size) throws Exception {
     Random rand = new Random();
 
- 
2.19.1.windows.1

