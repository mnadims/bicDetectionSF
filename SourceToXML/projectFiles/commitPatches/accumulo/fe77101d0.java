From fe77101d08ee7ae7b8061c238f4169cb6869bafa Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Wed, 11 Feb 2015 21:10:52 -0500
Subject: [PATCH] ACCUMULO-3580 Add some testing to exercise the
 TabletStateChangeIterator

--
 .../state/TabletStateChangeIterator.java      |  13 +-
 .../TabletStateChangeIteratorIT.java          | 170 ++++++++++++++++++
 2 files changed, 182 insertions(+), 1 deletion(-)
 create mode 100644 test/src/test/java/org/apache/accumulo/test/functional/TabletStateChangeIteratorIT.java

diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
index 687dddc7f..7ad74fe77 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
@@ -42,17 +42,21 @@ import org.apache.accumulo.server.master.state.TabletLocationState.BadLocationSt
 import org.apache.hadoop.io.DataInputBuffer;
 import org.apache.hadoop.io.DataOutputBuffer;
 import org.apache.hadoop.io.Text;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
 
 public class TabletStateChangeIterator extends SkippingIterator {
 
   private static final String SERVERS_OPTION = "servers";
   private static final String TABLES_OPTION = "tables";
   private static final String MERGES_OPTION = "merges";
  // private static final Logger log = Logger.getLogger(TabletStateChangeIterator.class);
  private static final String DEBUG_OPTION = "debug";
  private static final Logger log = Logger.getLogger(TabletStateChangeIterator.class);
 
   Set<TServerInstance> current;
   Set<String> onlineTables;
   Map<Text,MergeInfo> merges;
  boolean debug = false;
 
   @Override
   public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
@@ -60,6 +64,7 @@ public class TabletStateChangeIterator extends SkippingIterator {
     current = parseServers(options.get(SERVERS_OPTION));
     onlineTables = parseTables(options.get(TABLES_OPTION));
     merges = parseMerges(options.get(MERGES_OPTION));
    debug = options.containsKey(DEBUG_OPTION);
   }
 
   private Set<String> parseTables(String tables) {
@@ -134,6 +139,12 @@ public class TabletStateChangeIterator extends SkippingIterator {
       // is the table supposed to be online or offline?
       boolean shouldBeOnline = onlineTables.contains(tls.extent.getTableId().toString());
 
      if (debug) {
        Level oldLevel = log.getLevel();
        log.setLevel(Level.DEBUG);
        log.debug(tls.extent + " is " + tls.getState(current) + " and should be " + (shouldBeOnline ? "on" : "off") + "line");
        log.setLevel(oldLevel);
      }
       switch (tls.getState(current)) {
         case ASSIGNED:
           // we always want data about assigned tablets
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/TabletStateChangeIteratorIT.java b/test/src/test/java/org/apache/accumulo/test/functional/TabletStateChangeIteratorIT.java
new file mode 100644
index 000000000..ffd763699
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/functional/TabletStateChangeIteratorIT.java
@@ -0,0 +1,170 @@
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
package org.apache.accumulo.test.functional;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyExtent;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.harness.SharedMiniClusterIT;
import org.apache.accumulo.server.master.state.CurrentState;
import org.apache.accumulo.server.master.state.MergeInfo;
import org.apache.accumulo.server.master.state.MetaDataTableScanner;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletStateChangeIterator;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.hadoop.io.Text;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Test to ensure that the {@link TabletStateChangeIterator} properly skips over tablet information in the metadata table when there is no work to be done on
 * the tablet (see ACCUMULO-3580)
 */
public class TabletStateChangeIteratorIT extends SharedMiniClusterIT {

  @Test
  public void test() throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
    String[] tables = getUniqueNames(4);
    final String t1 = tables[0];
    final String t2 = tables[1];
    final String t3 = tables[2];
    final String cloned = tables[3];

    // create some metadata
    createTable(t1, true);
    createTable(t2, false);
    createTable(t3, true);

    // examine a clone of the metadata table, so we can manipulate it
    cloneMetadataTable(cloned);

    assertEquals("No tables should need attention", 0, findTabletsNeedingAttention(cloned));

    // test the assigned case (no location)
    removeLocation(cloned, t3);
    assertEquals("Should have one tablet without a loc", 1, findTabletsNeedingAttention(cloned));

    // TODO test the cases where the assignment is to a dead tserver
    // TODO test the cases where there is ongoing merges
    // TODO test the bad tablet location state case (active split, inconsistent metadata)

    // clean up
    dropTables(t1, t2, t3);
  }

  private void removeLocation(String table, String tableNameToModify) throws TableNotFoundException, MutationsRejectedException {
    String tableIdToModify = getConnector().tableOperations().tableIdMap().get(tableNameToModify);
    BatchDeleter deleter = getConnector().createBatchDeleter(table, Authorizations.EMPTY, 1, new BatchWriterConfig());
    deleter.setRanges(Collections.singleton(new KeyExtent(new Text(tableIdToModify), null, null).toMetadataRange()));
    deleter.fetchColumnFamily(MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME);
    deleter.delete();
    deleter.close();
  }

  private int findTabletsNeedingAttention(String table) throws TableNotFoundException {
    int results = 0;
    Scanner scanner = getConnector().createScanner(table, Authorizations.EMPTY);
    MetaDataTableScanner.configureScanner(scanner, new State());
    scanner.updateScanIteratorOption("tabletChange", "debug", "1");
    for (Entry<Key,Value> e : scanner) {
      if (e != null)
        results++;
    }
    return results;
  }

  private void createTable(String t, boolean online) throws AccumuloSecurityException, AccumuloException, TableNotFoundException, TableExistsException {
    Connector conn = getConnector();
    conn.tableOperations().create(t);
    conn.tableOperations().online(t, true);
    if (!online) {
      conn.tableOperations().offline(t, true);
    }
  }

  private void cloneMetadataTable(String cloned) throws AccumuloException, AccumuloSecurityException, TableNotFoundException, TableExistsException {
    getConnector().tableOperations().clone(MetadataTable.NAME, cloned, true, null, null);
  }

  private void dropTables(String... tables) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
    for (String t : tables) {
      getConnector().tableOperations().delete(t);
    }
  }

  private final class State implements CurrentState {

    @Override
    public Set<TServerInstance> onlineTabletServers() {
      HashSet<TServerInstance> tservers = new HashSet<TServerInstance>();
      for (String tserver : getConnector().instanceOperations().getTabletServers()) {
        try {
          String zPath = ZooUtil.getRoot(getConnector().getInstance()) + Constants.ZTSERVERS + "/" + tserver;
          long sessionId = ZooLock.getSessionId(new ZooCache(getCluster().getZooKeepers(), getConnector().getInstance().getZooKeepersSessionTimeOut()), zPath);
          tservers.add(new TServerInstance(tserver, sessionId));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      return tservers;
    }

    @Override
    public Set<String> onlineTables() {
      HashSet<String> onlineTables = new HashSet<String>(getConnector().tableOperations().tableIdMap().values());
      return Sets.filter(onlineTables, new Predicate<String>() {
        @Override
        public boolean apply(String tableId) {
          return Tables.getTableState(getConnector().getInstance(), tableId) == TableState.ONLINE;
        }
      });
    }

    @Override
    public Collection<MergeInfo> merges() {
      return Collections.emptySet();
    }
  }

}
- 
2.19.1.windows.1

