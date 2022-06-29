From 699b8bf0662be2261d64d0fb912c87cebd89c8b6 Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Thu, 11 Jun 2015 17:02:28 -0400
Subject: [PATCH] ACCUMULO-3897 Only request shutdown to master once for
 tserver

--
 .../master/tserverOps/ShutdownTServer.java    |  1 +
 .../master/tableOps/ShutdownTServerTest.java  | 82 +++++++++++++++++++
 2 files changed, 83 insertions(+)
 create mode 100644 server/master/src/test/java/org/apache/accumulo/master/tableOps/ShutdownTServerTest.java

diff --git a/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java b/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
index 11cd91bee..171e31206 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
++ b/server/master/src/main/java/org/apache/accumulo/master/tserverOps/ShutdownTServer.java
@@ -58,6 +58,7 @@ public class ShutdownTServer extends MasterRepo {
     // only send this request once
     if (!requestedShutdown) {
       master.shutdownTServer(server);
      requestedShutdown = true;
     }
 
     if (master.onlineTabletServers().contains(server)) {
diff --git a/server/master/src/test/java/org/apache/accumulo/master/tableOps/ShutdownTServerTest.java b/server/master/src/test/java/org/apache/accumulo/master/tableOps/ShutdownTServerTest.java
new file mode 100644
index 000000000..2fc51b82d
-- /dev/null
++ b/server/master/src/test/java/org/apache/accumulo/master/tableOps/ShutdownTServerTest.java
@@ -0,0 +1,82 @@
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
package org.apache.accumulo.master.tableOps;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;

import org.apache.accumulo.core.master.thrift.TableInfo;
import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tserverOps.ShutdownTServer;
import org.apache.accumulo.server.master.LiveTServerSet.TServerConnection;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.easymock.EasyMock;
import org.junit.Test;

public class ShutdownTServerTest {

  @Test
  public void testSingleShutdown() throws Exception {
    final TServerInstance tserver = EasyMock.createMock(TServerInstance.class);
    final boolean force = false;

    final ShutdownTServer op = new ShutdownTServer(tserver, force);

    final Master master = EasyMock.createMock(Master.class);
    final long tid = 1l;

    final TServerConnection tserverCnxn = EasyMock.createMock(TServerConnection.class);
    final TabletServerStatus status = new TabletServerStatus();
    status.tableMap = new HashMap<>();
    // Put in a table info record, don't care what
    status.tableMap.put("a_table", new TableInfo());

    master.shutdownTServer(tserver);
    EasyMock.expectLastCall().once();
    EasyMock.expect(master.onlineTabletServers()).andReturn(Collections.singleton(tserver));
    EasyMock.expect(master.getConnection(tserver)).andReturn(tserverCnxn);
    EasyMock.expect(tserverCnxn.getTableMap(false)).andReturn(status);

    EasyMock.replay(tserver, tserverCnxn, master);

    // FATE op is not ready
    long wait = op.isReady(tid, master);
    assertTrue("Expected wait to be greater than 0", wait > 0);

    EasyMock.verify(tserver, tserverCnxn, master);

    // Reset the mocks
    EasyMock.reset(tserver, tserverCnxn, master);

    // The same as above, but should not expect call shutdownTServer on master again
    EasyMock.expect(master.onlineTabletServers()).andReturn(Collections.singleton(tserver));
    EasyMock.expect(master.getConnection(tserver)).andReturn(tserverCnxn);
    EasyMock.expect(tserverCnxn.getTableMap(false)).andReturn(status);

    EasyMock.replay(tserver, tserverCnxn, master);

    // FATE op is not ready
    wait = op.isReady(tid, master);
    assertTrue("Expected wait to be greater than 0", wait > 0);

    EasyMock.verify(tserver, tserverCnxn, master);
  }

}
- 
2.19.1.windows.1

