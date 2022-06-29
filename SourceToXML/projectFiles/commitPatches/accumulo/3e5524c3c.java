From 3e5524c3c391d2556492d070710a789510be3532 Mon Sep 17 00:00:00 2001
From: Shawn Walker <accumulo@shawn-walker.net>
Date: Wed, 13 Jul 2016 10:40:52 -0400
Subject: [PATCH] ACCUMULO-4353: Stabilize tablet assignment during transient
 failure

Squashed the following commits, cherry picked them from master to 1.8 and then resolved conflicts.

commit 2e30d9178ec4352eaa724d2a9d5ea033f90e7d67
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Mon Jul 11 12:15:57 2016 -0400

    ACCUMULO-4353: Added short blurb in user manual on rolling restarts and `table.suspend.duration`

commit 24223c6ada605250c1d5b7b1da7abfd57e105085
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Mon Jul 11 11:24:13 2016 -0400

    ACCUMULO-4353: Refactored SuspendedTabletsIT to test for suspension upon both (a) clean tserver shutdown and (b) tserver crash

commit 35fcdd09a542680fc012860fc33151b7933cda02
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Fri Jul 8 12:40:03 2016 -0400

    ACCUMULO-4353: Fixed time unit mismatch in recent change to TabletServer

commit fc861c2b84773567d2fdb2c3e863eeffd5fb701c
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Tue Jul 5 14:22:37 2016 -0400

    ACCUMULO-4353: TServers undergoing "clean" shutdown will suspend their tablets now, too.  `master.metadata.suspendable` is now checked more often than just at startu
p

commit 96e3ddd742ced7010596f994d912662448743a0a
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Wed Jun 29 13:18:58 2016 -0400

    ACCUMULO-4353: Rewrote `TabletLocationState.getState()` to better account for concurrent change

commit e0e1523b96b83ee289dbae5ec785ca9b7d3761e7
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Tue Jun 28 13:27:57 2016 -0400

    ACCUMULO-4353: Now block balancing until live tservers settles to avoid balance/suspend race

commit 8d097c5f4388be83e90325e5b6d674ad21a6121b
Author: Shawn Walker <accumulo@shawn-walker.net>
Date:   Tue Jun 21 13:34:34 2016 -0400

    ACCUMULO-4353: Stabilize tablet assignment during transient failure.

    Added configuration property `table.suspend.duration` (default 0s): When a tablet server dies, instead of immediately placing its tablets in the TabletState.UNASSIGNED state, they are instead moved to the new TabletState.SUSPENDED state.  A suspended tablet will only be reassigned if (a) table.suspend.duration has passed since the tablet was suspended, or (b) the tablet server most recently hosting the tablet has come back online.  In the latter case, the tablet will be assigned back to its previous host.

    Added configuration property `master.metadata.suspendable` (default false): The above functionality is really meant to be used only on user tablets.  Suspending metadata tablets can lead to much more significant loss of availability.  Despite this, it is possible to set `table.suspend.duration` on `accumulo.metadata`.  If one really wishes to allow metadata tablets to be suspended as well, one must also set the `master.metadata.suspendable` to true.

    I chose not to implement suspension of the root tablet.

    Implementation outline:
    * `master.MasterTime` maintains an approximately monotonic clock; this is used by suspension to determine how much time has passed since a tablet was suspended.  `MasterTime` periodically writes its time base to ZooKeeper for persistence.
    * The `server.master.state.TabletState` now has a `TabletState.SUSPENDED` state.  `TabletLocationState`, `MetaDataStateStore` were updated to properly read and write suspensions.
    * `server.master.state.TabletStateStore` now features a `suspend(...)` method, for suspending a tablet, with implementations in `MetaDataStateStore`.  `suspend(...)` acts just as `unassign(...)`, except that it writes additional metadata indicating when each tablet was suspended, and which tablet server it was suspended from.
    * `master.TabletServerWatcher` updated to properly transition to/from `TabletState.SUSPENDED`.
    * `master.Master` updated to avoid balancing while any tablets remain suspended.
--
 .../org/apache/accumulo/core/Constants.java   |   1 +
 .../apache/accumulo/core/conf/Property.java   |   6 +
 .../core/metadata/schema/MetadataSchema.java  |   7 +
 .../thrift/TUnloadTabletGoal.java             |  67 ++++
 .../thrift/TabletClientService.java           | 275 ++++++++++----
 core/src/main/thrift/tabletserver.thrift      |   9 +-
 .../main/asciidoc/chapters/administration.txt |  14 +
 .../impl/MiniAccumuloClusterControl.java      |  25 +-
 .../impl/MiniAccumuloClusterImpl.java         |  15 +-
 .../constraints/MetadataConstraints.java      |   6 +-
 .../server/master/LiveTServerSet.java         |   5 +-
 .../master/state/MetaDataStateStore.java      |  37 ++
 .../master/state/MetaDataTableScanner.java    |   6 +-
 .../master/state/SuspendingTServer.java       |  71 ++++
 .../master/state/TabletLocationState.java     |  44 ++-
 .../server/master/state/TabletState.java      |   2 +-
 .../state/TabletStateChangeIterator.java      |   1 +
 .../server/master/state/TabletStateStore.java |  43 ++-
 .../master/state/ZooTabletStateStore.java     |  14 +-
 .../master/state/TabletLocationStateTest.java |  28 +-
 .../gc/GarbageCollectWriteAheadLogsTest.java  |   4 +-
 .../org/apache/accumulo/master/Master.java    |  82 ++++-
 .../apache/accumulo/master/MasterTime.java    | 108 ++++++
 .../accumulo/master/TabletGroupWatcher.java   |  87 ++++-
 .../accumulo/master/state/MergeStats.java     |   4 +-
 .../accumulo/master/state/TableCounts.java    |   4 +
 .../state/RootTabletStateStoreTest.java       |   4 +-
 .../apache/accumulo/tserver/TabletServer.java |  31 +-
 .../accumulo/test/master/MergeStateIT.java    |   2 +-
 .../test/master/SuspendedTabletsIT.java       | 340 ++++++++++++++++++
 .../test/performance/thrift/NullTserver.java  |   3 +-
 31 files changed, 1158 insertions(+), 187 deletions(-)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TUnloadTabletGoal.java
 create mode 100644 server/base/src/main/java/org/apache/accumulo/server/master/state/SuspendingTServer.java
 create mode 100644 server/master/src/main/java/org/apache/accumulo/master/MasterTime.java
 create mode 100644 test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java

diff --git a/core/src/main/java/org/apache/accumulo/core/Constants.java b/core/src/main/java/org/apache/accumulo/core/Constants.java
index 94ada7a3d..eebd81d29 100644
-- a/core/src/main/java/org/apache/accumulo/core/Constants.java
++ b/core/src/main/java/org/apache/accumulo/core/Constants.java
@@ -50,6 +50,7 @@ public class Constants {
   public static final String ZMASTER_LOCK = ZMASTERS + "/lock";
   public static final String ZMASTER_GOAL_STATE = ZMASTERS + "/goal_state";
   public static final String ZMASTER_REPLICATION_COORDINATOR_ADDR = ZMASTERS + "/repl_coord_addr";
  public static final String ZMASTER_TICK = ZMASTERS + "/tick";
 
   public static final String ZGC = "/gc";
   public static final String ZGC_LOCK = ZGC + "/lock";
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index 600f7125f..c49457f8b 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -30,6 +30,7 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.file.rfile.RFile;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.util.format.DefaultFormatter;
 import org.apache.accumulo.core.util.interpret.DefaultScanInterpreter;
 import org.apache.accumulo.start.classloader.AccumuloClassLoader;
@@ -231,6 +232,8 @@ public enum Property {
       "The time between adjustments of the coordinator thread pool"),
   MASTER_STATUS_THREAD_POOL_SIZE("master.status.threadpool.size", "1", PropertyType.COUNT,
       "The number of threads to use when fetching the tablet server status for balancing."),
  MASTER_METADATA_SUSPENDABLE("master.metadata.suspendable", "false", PropertyType.BOOLEAN, "Allow tablets for the " + MetadataTable.NAME
      + " table to be suspended via table.suspend.duration."),
 
   // properties that are specific to tablet server behavior
   TSERV_PREFIX("tserver.", null, PropertyType.PREFIX, "Properties in this category affect the behavior of the tablet servers"),
@@ -543,6 +546,9 @@ public enum Property {
   TABLE_SAMPLER_OPTS("table.sampler.opt.", null, PropertyType.PREFIX,
       "The property is used to set options for a sampler.  If a sample had two options like hasher and modulous, then the two properties "
           + "table.sampler.opt.hasher=${hash algorithm} and table.sampler.opt.modulous=${mod} would be set."),
  TABLE_SUSPEND_DURATION("table.suspend.duration", "0s", PropertyType.TIMEDURATION,
      "For tablets belonging to this table: When a tablet server dies, allow the tablet server this duration to revive before reassigning its tablets"
          + "to other tablet servers."),
 
   // VFS ClassLoader properties
   VFS_CLASSLOADER_SYSTEM_CLASSPATH_PROPERTY(AccumuloVFSClassLoader.VFS_CLASSLOADER_SYSTEM_CLASSPATH_PROPERTY, "", PropertyType.STRING,
diff --git a/core/src/main/java/org/apache/accumulo/core/metadata/schema/MetadataSchema.java b/core/src/main/java/org/apache/accumulo/core/metadata/schema/MetadataSchema.java
index 7426fede7..c93987dd9 100644
-- a/core/src/main/java/org/apache/accumulo/core/metadata/schema/MetadataSchema.java
++ b/core/src/main/java/org/apache/accumulo/core/metadata/schema/MetadataSchema.java
@@ -136,6 +136,13 @@ public class MetadataSchema {
       public static final Text NAME = new Text("last");
     }
 
    /**
     * Column family for storing suspension location, as a demand for assignment.
     */
    public static class SuspendLocationColumn {
      public static final ColumnFQ SUSPEND_COLUMN = new ColumnFQ(new Text("suspend"), new Text("loc"));
    }

     /**
      * Temporary markers that indicate a tablet loaded a bulk file
      */
diff --git a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TUnloadTabletGoal.java b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TUnloadTabletGoal.java
new file mode 100644
index 000000000..3ce0b3127
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TUnloadTabletGoal.java
@@ -0,0 +1,67 @@
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
/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.core.tabletserver.thrift;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

@SuppressWarnings({"unused"}) public enum TUnloadTabletGoal implements org.apache.thrift.TEnum {
  UNKNOWN(0),
  UNASSIGNED(1),
  SUSPENDED(2),
  DELETED(3);

  private final int value;

  private TUnloadTabletGoal(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static TUnloadTabletGoal findByValue(int value) { 
    switch (value) {
      case 0:
        return UNKNOWN;
      case 1:
        return UNASSIGNED;
      case 2:
        return SUSPENDED;
      case 3:
        return DELETED;
      default:
        return null;
    }
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
index 3d4fa061c..4ce992712 100644
-- a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
++ b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
@@ -89,7 +89,7 @@ public class TabletClientService {
 
     public void loadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent) throws org.apache.thrift.TException;
 
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save) throws org.apache.thrift.TException;
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime) throws org.apache.thrift.TException;
 
     public void flush(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, String tableId, ByteBuffer startRow, ByteBuffer endRow) throws org.apache.thrift.TException;
 
@@ -155,7 +155,7 @@ public class TabletClientService {
 
     public void loadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
     public void flush(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, String tableId, ByteBuffer startRow, ByteBuffer endRow, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
@@ -667,19 +667,20 @@ public class TabletClientService {
       sendBaseOneway("loadTablet", args);
     }
 
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save) throws org.apache.thrift.TException
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime) throws org.apache.thrift.TException
     {
      send_unloadTablet(tinfo, credentials, lock, extent, save);
      send_unloadTablet(tinfo, credentials, lock, extent, goal, requestTime);
     }
 
    public void send_unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save) throws org.apache.thrift.TException
    public void send_unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime) throws org.apache.thrift.TException
     {
       unloadTablet_args args = new unloadTablet_args();
       args.setTinfo(tinfo);
       args.setCredentials(credentials);
       args.setLock(lock);
       args.setExtent(extent);
      args.setSave(save);
      args.setGoal(goal);
      args.setRequestTime(requestTime);
       sendBaseOneway("unloadTablet", args);
     }
 
@@ -1692,9 +1693,9 @@ public class TabletClientService {
       }
     }
 
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
    public void unloadTablet(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
       checkReady();
      unloadTablet_call method_call = new unloadTablet_call(tinfo, credentials, lock, extent, save, resultHandler, this, ___protocolFactory, ___transport);
      unloadTablet_call method_call = new unloadTablet_call(tinfo, credentials, lock, extent, goal, requestTime, resultHandler, this, ___protocolFactory, ___transport);
       this.___currentMethod = method_call;
       ___manager.call(method_call);
     }
@@ -1704,14 +1705,16 @@ public class TabletClientService {
       private org.apache.accumulo.core.security.thrift.TCredentials credentials;
       private String lock;
       private org.apache.accumulo.core.data.thrift.TKeyExtent extent;
      private boolean save;
      public unloadTablet_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, boolean save, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
      private TUnloadTabletGoal goal;
      private long requestTime;
      public unloadTablet_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, String lock, org.apache.accumulo.core.data.thrift.TKeyExtent extent, TUnloadTabletGoal goal, long requestTime, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
         super(client, protocolFactory, transport, resultHandler, true);
         this.tinfo = tinfo;
         this.credentials = credentials;
         this.lock = lock;
         this.extent = extent;
        this.save = save;
        this.goal = goal;
        this.requestTime = requestTime;
       }
 
       public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
@@ -1721,7 +1724,8 @@ public class TabletClientService {
         args.setCredentials(credentials);
         args.setLock(lock);
         args.setExtent(extent);
        args.setSave(save);
        args.setGoal(goal);
        args.setRequestTime(requestTime);
         args.write(prot);
         prot.writeMessageEnd();
       }
@@ -2700,7 +2704,7 @@ public class TabletClientService {
       }
 
       public org.apache.thrift.TBase getResult(I iface, unloadTablet_args args) throws org.apache.thrift.TException {
        iface.unloadTablet(args.tinfo, args.credentials, args.lock, args.extent, args.save);
        iface.unloadTablet(args.tinfo, args.credentials, args.lock, args.extent, args.goal, args.requestTime);
         return null;
       }
     }
@@ -3953,7 +3957,7 @@ public class TabletClientService {
       }
 
       public void start(I iface, unloadTablet_args args, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws TException {
        iface.unloadTablet(args.tinfo, args.credentials, args.lock, args.extent, args.save,resultHandler);
        iface.unloadTablet(args.tinfo, args.credentials, args.lock, args.extent, args.goal, args.requestTime,resultHandler);
       }
     }
 
@@ -24718,7 +24722,8 @@ public class TabletClientService {
     private static final org.apache.thrift.protocol.TField CREDENTIALS_FIELD_DESC = new org.apache.thrift.protocol.TField("credentials", org.apache.thrift.protocol.TType.STRUCT, (short)1);
     private static final org.apache.thrift.protocol.TField LOCK_FIELD_DESC = new org.apache.thrift.protocol.TField("lock", org.apache.thrift.protocol.TType.STRING, (short)4);
     private static final org.apache.thrift.protocol.TField EXTENT_FIELD_DESC = new org.apache.thrift.protocol.TField("extent", org.apache.thrift.protocol.TType.STRUCT, (short)2);
    private static final org.apache.thrift.protocol.TField SAVE_FIELD_DESC = new org.apache.thrift.protocol.TField("save", org.apache.thrift.protocol.TType.BOOL, (short)3);
    private static final org.apache.thrift.protocol.TField GOAL_FIELD_DESC = new org.apache.thrift.protocol.TField("goal", org.apache.thrift.protocol.TType.I32, (short)6);
    private static final org.apache.thrift.protocol.TField REQUEST_TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("requestTime", org.apache.thrift.protocol.TType.I64, (short)7);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -24730,7 +24735,12 @@ public class TabletClientService {
     public org.apache.accumulo.core.security.thrift.TCredentials credentials; // required
     public String lock; // required
     public org.apache.accumulo.core.data.thrift.TKeyExtent extent; // required
    public boolean save; // required
    /**
     * 
     * @see TUnloadTabletGoal
     */
    public TUnloadTabletGoal goal; // required
    public long requestTime; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     public enum _Fields implements org.apache.thrift.TFieldIdEnum {
@@ -24738,7 +24748,12 @@ public class TabletClientService {
       CREDENTIALS((short)1, "credentials"),
       LOCK((short)4, "lock"),
       EXTENT((short)2, "extent"),
      SAVE((short)3, "save");
      /**
       * 
       * @see TUnloadTabletGoal
       */
      GOAL((short)6, "goal"),
      REQUEST_TIME((short)7, "requestTime");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -24761,8 +24776,10 @@ public class TabletClientService {
             return LOCK;
           case 2: // EXTENT
             return EXTENT;
          case 3: // SAVE
            return SAVE;
          case 6: // GOAL
            return GOAL;
          case 7: // REQUEST_TIME
            return REQUEST_TIME;
           default:
             return null;
         }
@@ -24803,7 +24820,7 @@ public class TabletClientService {
     }
 
     // isset id assignments
    private static final int __SAVE_ISSET_ID = 0;
    private static final int __REQUESTTIME_ISSET_ID = 0;
     private byte __isset_bitfield = 0;
     public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
     static {
@@ -24816,8 +24833,10 @@ public class TabletClientService {
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
       tmpMap.put(_Fields.EXTENT, new org.apache.thrift.meta_data.FieldMetaData("extent", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.data.thrift.TKeyExtent.class)));
      tmpMap.put(_Fields.SAVE, new org.apache.thrift.meta_data.FieldMetaData("save", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
      tmpMap.put(_Fields.GOAL, new org.apache.thrift.meta_data.FieldMetaData("goal", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, TUnloadTabletGoal.class)));
      tmpMap.put(_Fields.REQUEST_TIME, new org.apache.thrift.meta_data.FieldMetaData("requestTime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(unloadTablet_args.class, metaDataMap);
     }
@@ -24830,15 +24849,17 @@ public class TabletClientService {
       org.apache.accumulo.core.security.thrift.TCredentials credentials,
       String lock,
       org.apache.accumulo.core.data.thrift.TKeyExtent extent,
      boolean save)
      TUnloadTabletGoal goal,
      long requestTime)
     {
       this();
       this.tinfo = tinfo;
       this.credentials = credentials;
       this.lock = lock;
       this.extent = extent;
      this.save = save;
      setSaveIsSet(true);
      this.goal = goal;
      this.requestTime = requestTime;
      setRequestTimeIsSet(true);
     }
 
     /**
@@ -24858,7 +24879,10 @@ public class TabletClientService {
       if (other.isSetExtent()) {
         this.extent = new org.apache.accumulo.core.data.thrift.TKeyExtent(other.extent);
       }
      this.save = other.save;
      if (other.isSetGoal()) {
        this.goal = other.goal;
      }
      this.requestTime = other.requestTime;
     }
 
     public unloadTablet_args deepCopy() {
@@ -24871,8 +24895,9 @@ public class TabletClientService {
       this.credentials = null;
       this.lock = null;
       this.extent = null;
      setSaveIsSet(false);
      this.save = false;
      this.goal = null;
      setRequestTimeIsSet(false);
      this.requestTime = 0;
     }
 
     public org.apache.accumulo.core.trace.thrift.TInfo getTinfo() {
@@ -24971,27 +24996,59 @@ public class TabletClientService {
       }
     }
 
    public boolean isSave() {
      return this.save;
    /**
     * 
     * @see TUnloadTabletGoal
     */
    public TUnloadTabletGoal getGoal() {
      return this.goal;
    }

    /**
     * 
     * @see TUnloadTabletGoal
     */
    public unloadTablet_args setGoal(TUnloadTabletGoal goal) {
      this.goal = goal;
      return this;
    }

    public void unsetGoal() {
      this.goal = null;
    }

    /** Returns true if field goal is set (has been assigned a value) and false otherwise */
    public boolean isSetGoal() {
      return this.goal != null;
    }

    public void setGoalIsSet(boolean value) {
      if (!value) {
        this.goal = null;
      }
    }

    public long getRequestTime() {
      return this.requestTime;
     }
 
    public unloadTablet_args setSave(boolean save) {
      this.save = save;
      setSaveIsSet(true);
    public unloadTablet_args setRequestTime(long requestTime) {
      this.requestTime = requestTime;
      setRequestTimeIsSet(true);
       return this;
     }
 
    public void unsetSave() {
      __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __SAVE_ISSET_ID);
    public void unsetRequestTime() {
      __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __REQUESTTIME_ISSET_ID);
     }
 
    /** Returns true if field save is set (has been assigned a value) and false otherwise */
    public boolean isSetSave() {
      return EncodingUtils.testBit(__isset_bitfield, __SAVE_ISSET_ID);
    /** Returns true if field requestTime is set (has been assigned a value) and false otherwise */
    public boolean isSetRequestTime() {
      return EncodingUtils.testBit(__isset_bitfield, __REQUESTTIME_ISSET_ID);
     }
 
    public void setSaveIsSet(boolean value) {
      __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __SAVE_ISSET_ID, value);
    public void setRequestTimeIsSet(boolean value) {
      __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __REQUESTTIME_ISSET_ID, value);
     }
 
     public void setFieldValue(_Fields field, Object value) {
@@ -25028,11 +25085,19 @@ public class TabletClientService {
         }
         break;
 
      case SAVE:
      case GOAL:
         if (value == null) {
          unsetSave();
          unsetGoal();
         } else {
          setSave((Boolean)value);
          setGoal((TUnloadTabletGoal)value);
        }
        break;

      case REQUEST_TIME:
        if (value == null) {
          unsetRequestTime();
        } else {
          setRequestTime((Long)value);
         }
         break;
 
@@ -25053,8 +25118,11 @@ public class TabletClientService {
       case EXTENT:
         return getExtent();
 
      case SAVE:
        return isSave();
      case GOAL:
        return getGoal();

      case REQUEST_TIME:
        return getRequestTime();
 
       }
       throw new IllegalStateException();
@@ -25075,8 +25143,10 @@ public class TabletClientService {
         return isSetLock();
       case EXTENT:
         return isSetExtent();
      case SAVE:
        return isSetSave();
      case GOAL:
        return isSetGoal();
      case REQUEST_TIME:
        return isSetRequestTime();
       }
       throw new IllegalStateException();
     }
@@ -25130,12 +25200,21 @@ public class TabletClientService {
           return false;
       }
 
      boolean this_present_save = true;
      boolean that_present_save = true;
      if (this_present_save || that_present_save) {
        if (!(this_present_save && that_present_save))
      boolean this_present_goal = true && this.isSetGoal();
      boolean that_present_goal = true && that.isSetGoal();
      if (this_present_goal || that_present_goal) {
        if (!(this_present_goal && that_present_goal))
           return false;
        if (this.save != that.save)
        if (!this.goal.equals(that.goal))
          return false;
      }

      boolean this_present_requestTime = true;
      boolean that_present_requestTime = true;
      if (this_present_requestTime || that_present_requestTime) {
        if (!(this_present_requestTime && that_present_requestTime))
          return false;
        if (this.requestTime != that.requestTime)
           return false;
       }
 
@@ -25166,10 +25245,15 @@ public class TabletClientService {
       if (present_extent)
         list.add(extent);
 
      boolean present_save = true;
      list.add(present_save);
      if (present_save)
        list.add(save);
      boolean present_goal = true && (isSetGoal());
      list.add(present_goal);
      if (present_goal)
        list.add(goal.getValue());

      boolean present_requestTime = true;
      list.add(present_requestTime);
      if (present_requestTime)
        list.add(requestTime);
 
       return list.hashCode();
     }
@@ -25222,12 +25306,22 @@ public class TabletClientService {
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetSave()).compareTo(other.isSetSave());
      lastComparison = Boolean.valueOf(isSetGoal()).compareTo(other.isSetGoal());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetGoal()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.goal, other.goal);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetRequestTime()).compareTo(other.isSetRequestTime());
       if (lastComparison != 0) {
         return lastComparison;
       }
      if (isSetSave()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.save, other.save);
      if (isSetRequestTime()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.requestTime, other.requestTime);
         if (lastComparison != 0) {
           return lastComparison;
         }
@@ -25284,8 +25378,16 @@ public class TabletClientService {
       }
       first = false;
       if (!first) sb.append(", ");
      sb.append("save:");
      sb.append(this.save);
      sb.append("goal:");
      if (this.goal == null) {
        sb.append("null");
      } else {
        sb.append(this.goal);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("requestTime:");
      sb.append(this.requestTime);
       first = false;
       sb.append(")");
       return sb.toString();
@@ -25376,10 +25478,18 @@ public class TabletClientService {
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 3: // SAVE
              if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
                struct.save = iprot.readBool();
                struct.setSaveIsSet(true);
            case 6: // GOAL
              if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
                struct.goal = org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal.findByValue(iprot.readI32());
                struct.setGoalIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 7: // REQUEST_TIME
              if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
                struct.requestTime = iprot.readI64();
                struct.setRequestTimeIsSet(true);
               } else { 
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
@@ -25409,9 +25519,6 @@ public class TabletClientService {
           struct.extent.write(oprot);
           oprot.writeFieldEnd();
         }
        oprot.writeFieldBegin(SAVE_FIELD_DESC);
        oprot.writeBool(struct.save);
        oprot.writeFieldEnd();
         if (struct.lock != null) {
           oprot.writeFieldBegin(LOCK_FIELD_DESC);
           oprot.writeString(struct.lock);
@@ -25422,6 +25529,14 @@ public class TabletClientService {
           struct.tinfo.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.goal != null) {
          oprot.writeFieldBegin(GOAL_FIELD_DESC);
          oprot.writeI32(struct.goal.getValue());
          oprot.writeFieldEnd();
        }
        oprot.writeFieldBegin(REQUEST_TIME_FIELD_DESC);
        oprot.writeI64(struct.requestTime);
        oprot.writeFieldEnd();
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -25452,10 +25567,13 @@ public class TabletClientService {
         if (struct.isSetExtent()) {
           optionals.set(3);
         }
        if (struct.isSetSave()) {
        if (struct.isSetGoal()) {
           optionals.set(4);
         }
        oprot.writeBitSet(optionals, 5);
        if (struct.isSetRequestTime()) {
          optionals.set(5);
        }
        oprot.writeBitSet(optionals, 6);
         if (struct.isSetTinfo()) {
           struct.tinfo.write(oprot);
         }
@@ -25468,15 +25586,18 @@ public class TabletClientService {
         if (struct.isSetExtent()) {
           struct.extent.write(oprot);
         }
        if (struct.isSetSave()) {
          oprot.writeBool(struct.save);
        if (struct.isSetGoal()) {
          oprot.writeI32(struct.goal.getValue());
        }
        if (struct.isSetRequestTime()) {
          oprot.writeI64(struct.requestTime);
         }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, unloadTablet_args struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(5);
        BitSet incoming = iprot.readBitSet(6);
         if (incoming.get(0)) {
           struct.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo();
           struct.tinfo.read(iprot);
@@ -25497,8 +25618,12 @@ public class TabletClientService {
           struct.setExtentIsSet(true);
         }
         if (incoming.get(4)) {
          struct.save = iprot.readBool();
          struct.setSaveIsSet(true);
          struct.goal = org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal.findByValue(iprot.readI32());
          struct.setGoalIsSet(true);
        }
        if (incoming.get(5)) {
          struct.requestTime = iprot.readI64();
          struct.setRequestTimeIsSet(true);
         }
       }
     }
diff --git a/core/src/main/thrift/tabletserver.thrift b/core/src/main/thrift/tabletserver.thrift
index 6a455626f..7697a2d25 100644
-- a/core/src/main/thrift/tabletserver.thrift
++ b/core/src/main/thrift/tabletserver.thrift
@@ -146,6 +146,13 @@ struct TSamplerConfiguration {
    2:map<string, string> options
 }
 
enum TUnloadTabletGoal {
   UNKNOWN,
   UNASSIGNED,
   SUSPENDED,
   DELETED
}

 service TabletClientService extends client.ClientService {
   // scan a range of keys
   data.InitialScan startScan(11:trace.TInfo tinfo,
@@ -207,7 +214,7 @@ service TabletClientService extends client.ClientService {
   void splitTablet(4:trace.TInfo tinfo, 1:security.TCredentials credentials, 2:data.TKeyExtent extent, 3:binary splitPoint) throws (1:client.ThriftSecurityException sec, 2:NotServingTabletException nste)
  
   oneway void loadTablet(5:trace.TInfo tinfo, 1:security.TCredentials credentials, 4:string lock, 2:data.TKeyExtent extent),
  oneway void unloadTablet(5:trace.TInfo tinfo, 1:security.TCredentials credentials, 4:string lock, 2:data.TKeyExtent extent, 3:bool save),
  oneway void unloadTablet(5:trace.TInfo tinfo, 1:security.TCredentials credentials, 4:string lock, 2:data.TKeyExtent extent, 6:TUnloadTabletGoal goal, 7:i64 requestTime),
   oneway void flush(4:trace.TInfo tinfo, 1:security.TCredentials credentials, 3:string lock, 2:string tableId, 5:binary startRow, 6:binary endRow),
   oneway void flushTablet(1:trace.TInfo tinfo, 2:security.TCredentials credentials, 3:string lock, 4:data.TKeyExtent extent),
   oneway void chop(1:trace.TInfo tinfo, 2:security.TCredentials credentials, 3:string lock, 4:data.TKeyExtent extent),
diff --git a/docs/src/main/asciidoc/chapters/administration.txt b/docs/src/main/asciidoc/chapters/administration.txt
index 1935181d5..a2dab8e50 100644
-- a/docs/src/main/asciidoc/chapters/administration.txt
++ b/docs/src/main/asciidoc/chapters/administration.txt
@@ -476,6 +476,20 @@ from the +$ACCUMULO_HOME/conf/slaves+ file) to gracefully stop a node. This will
 ensure that the tabletserver is cleanly stopped and recovery will not need to be performed
 when the tablets are re-hosted.
 
===== A note on rolling restarts

For sufficiently large Accumulo clusters, restarting multiple TabletServers within a short window can place significant 
load on the Master server.  If slightly lower availability is acceptable, this load can be reduced by globally setting 
+table.suspend.duration+ to a positive value.  

With +table.suspend.duration+ set to, say, +5m+, Accumulo will wait 
for 5 minutes for any dead TabletServer to return before reassigning that TabletServer's responsibilities to other TabletServers.
If the TabletServer returns to the cluster before the specified timeout has elapsed, Accumulo will assign the TabletServer 
its original responsibilities.

It is important not to choose too large a value for +table.suspend.duration+, as during this time, all scans against the 
data that TabletServer had hosted will block (or time out).

 ==== Running multiple TabletServers on a single node
 
 With very powerful nodes, it may be beneficial to run more than one TabletServer on a given
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterControl.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterControl.java
index 8cc7950e3..688cb5d2d 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterControl.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterControl.java
@@ -42,6 +42,8 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
 
 /**
  *
@@ -132,37 +134,46 @@ public class MiniAccumuloClusterControl implements ClusterControl {
 
   @Override
   public synchronized void start(ServerType server, String hostname) throws IOException {
    start(server, hostname, Collections.EMPTY_MAP, Integer.MAX_VALUE);
  }

  public synchronized void start(ServerType server, String hostname, Map<String,String> configOverrides, int limit) throws IOException {
    if (limit <= 0) {
      return;
    }

     switch (server) {
       case TABLET_SERVER:
         synchronized (tabletServerProcesses) {
          for (int i = tabletServerProcesses.size(); i < cluster.getConfig().getNumTservers(); i++) {
            tabletServerProcesses.add(cluster._exec(TabletServer.class, server));
          int count = 0;
          for (int i = tabletServerProcesses.size(); count < limit && i < cluster.getConfig().getNumTservers(); i++, ++count) {
            tabletServerProcesses.add(cluster._exec(TabletServer.class, server, configOverrides));
           }
         }
         break;
       case MASTER:
         if (null == masterProcess) {
          masterProcess = cluster._exec(Master.class, server);
          masterProcess = cluster._exec(Master.class, server, configOverrides);
         }
         break;
       case ZOOKEEPER:
         if (null == zooKeeperProcess) {
          zooKeeperProcess = cluster._exec(ZooKeeperServerMain.class, server, cluster.getZooCfgFile().getAbsolutePath());
          zooKeeperProcess = cluster._exec(ZooKeeperServerMain.class, server, configOverrides, cluster.getZooCfgFile().getAbsolutePath());
         }
         break;
       case GARBAGE_COLLECTOR:
         if (null == gcProcess) {
          gcProcess = cluster._exec(SimpleGarbageCollector.class, server);
          gcProcess = cluster._exec(SimpleGarbageCollector.class, server, configOverrides);
         }
         break;
       case MONITOR:
         if (null == monitor) {
          monitor = cluster._exec(Monitor.class, server);
          monitor = cluster._exec(Monitor.class, server, configOverrides);
         }
         break;
       case TRACER:
         if (null == tracer) {
          tracer = cluster._exec(TraceServer.class, server);
          tracer = cluster._exec(TraceServer.class, server, configOverrides);
         }
         break;
       default:
diff --git a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
index fd017bec2..3e66acfee 100644
-- a/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
++ b/minicluster/src/main/java/org/apache/accumulo/minicluster/impl/MiniAccumuloClusterImpl.java
@@ -111,7 +111,7 @@ import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Joiner;
 import com.google.common.base.Predicate;
 import com.google.common.collect.Maps;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import com.google.common.util.concurrent.Uninterruptibles;
 
 /**
  * This class provides the backing implementation for {@link MiniAccumuloCluster}, and may contain features for internal testing which have not yet been
@@ -342,10 +342,17 @@ public class MiniAccumuloClusterImpl implements AccumuloCluster {
     return process;
   }
 
  Process _exec(Class<?> clazz, ServerType serverType, String... args) throws IOException {

  Process _exec(Class<?> clazz, ServerType serverType, Map<String,String> configOverrides, String... args) throws IOException {
     List<String> jvmOpts = new ArrayList<>();
     jvmOpts.add("-Xmx" + config.getMemory(serverType));
    if (configOverrides != null && !configOverrides.isEmpty()) {
      File siteFile = File.createTempFile("accumulo-site", ".xml", config.getConfDir());
      Map<String,String> confMap = new HashMap<>();
      confMap.putAll(config.getSiteConfig());
      confMap.putAll(configOverrides);
      writeConfig(siteFile, confMap.entrySet());
      jvmOpts.add("-Dorg.apache.accumulo.config.file=" + siteFile.getName());
    }
 
     if (config.isJDWPEnabled()) {
       Integer port = PortUtils.getRandomFreePort();
@@ -622,7 +629,7 @@ public class MiniAccumuloClusterImpl implements AccumuloCluster {
       ret = exec(Main.class, SetGoalState.class.getName(), MasterGoalState.NORMAL.toString()).waitFor();
       if (ret == 0)
         break;
      sleepUninterruptibly(1, TimeUnit.SECONDS);
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
     }
     if (ret != 0) {
       throw new RuntimeException("Could not set master goal state, process returned " + ret + ". Check the logs in " + config.getLogDir() + " for errors.");
diff --git a/server/base/src/main/java/org/apache/accumulo/server/constraints/MetadataConstraints.java b/server/base/src/main/java/org/apache/accumulo/server/constraints/MetadataConstraints.java
index 7815a3df6..98f8c3fda 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/constraints/MetadataConstraints.java
++ b/server/base/src/main/java/org/apache/accumulo/server/constraints/MetadataConstraints.java
@@ -65,9 +65,9 @@ public class MetadataConstraints implements Constraint {
   }
 
   private static final HashSet<ColumnFQ> validColumnQuals = new HashSet<>(Arrays.asList(TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN,
      TabletsSection.TabletColumnFamily.OLD_PREV_ROW_COLUMN, TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN,
      TabletsSection.TabletColumnFamily.SPLIT_RATIO_COLUMN, TabletsSection.ServerColumnFamily.TIME_COLUMN, TabletsSection.ServerColumnFamily.LOCK_COLUMN,
      TabletsSection.ServerColumnFamily.FLUSH_COLUMN, TabletsSection.ServerColumnFamily.COMPACT_COLUMN));
      TabletsSection.TabletColumnFamily.OLD_PREV_ROW_COLUMN, TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN,
      TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN, TabletsSection.TabletColumnFamily.SPLIT_RATIO_COLUMN, TabletsSection.ServerColumnFamily.TIME_COLUMN,
      TabletsSection.ServerColumnFamily.LOCK_COLUMN, TabletsSection.ServerColumnFamily.FLUSH_COLUMN, TabletsSection.ServerColumnFamily.COMPACT_COLUMN));
 
   private static final HashSet<Text> validColumnFams = new HashSet<>(Arrays.asList(TabletsSection.BulkFileColumnFamily.NAME, LogColumnFamily.NAME,
       ScanFileColumnFamily.NAME, DataFileColumnFamily.NAME, TabletsSection.CurrentLocationColumnFamily.NAME, TabletsSection.LastLocationColumnFamily.NAME,
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java b/server/base/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
index 46f5f8b4e..7d1d6e117 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/LiveTServerSet.java
@@ -57,6 +57,7 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.net.HostAndPort;
import org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal;
 
 public class LiveTServerSet implements Watcher {
 
@@ -105,10 +106,10 @@ public class LiveTServerSet implements Watcher {
       }
     }
 
    public void unloadTablet(ZooLock lock, KeyExtent extent, boolean save) throws TException {
    public void unloadTablet(ZooLock lock, KeyExtent extent, TUnloadTabletGoal goal, long requestTime) throws TException {
       TabletClientService.Client client = ThriftUtil.getClient(new TabletClientService.Client.Factory(), address, context);
       try {
        client.unloadTablet(Tracer.traceInfo(), context.rpcCreds(), lockString(lock), extent.toThrift(), save);
        client.unloadTablet(Tracer.traceInfo(), context.rpcCreds(), lockString(lock), extent.toThrift(), goal, requestTime);
       } finally {
         ThriftUtil.returnClient(client);
       }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
index 7763c258b..c549adce9 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataStateStore.java
@@ -74,6 +74,7 @@ public class MetaDataStateStore extends TabletStateStore {
         Mutation m = new Mutation(assignment.tablet.getMetadataEntry());
         assignment.server.putLocation(m);
         assignment.server.clearFutureLocation(m);
        SuspendingTServer.clearSuspension(m);
         writer.addMutation(m);
       }
     } catch (Exception ex) {
@@ -105,6 +106,7 @@ public class MetaDataStateStore extends TabletStateStore {
     try {
       for (Assignment assignment : assignments) {
         Mutation m = new Mutation(assignment.tablet.getMetadataEntry());
        SuspendingTServer.clearSuspension(m);
         assignment.server.putFutureLocation(m);
         writer.addMutation(m);
       }
@@ -121,7 +123,12 @@ public class MetaDataStateStore extends TabletStateStore {
 
   @Override
   public void unassign(Collection<TabletLocationState> tablets, Map<TServerInstance,List<Path>> logsForDeadServers) throws DistributedStoreException {
    suspend(tablets, logsForDeadServers, -1);
  }
 
  @Override
  public void suspend(Collection<TabletLocationState> tablets, Map<TServerInstance,List<Path>> logsForDeadServers, long suspensionTimestamp)
      throws DistributedStoreException {
     BatchWriter writer = createBatchWriter();
     try {
       for (TabletLocationState tls : tablets) {
@@ -137,6 +144,13 @@ public class MetaDataStateStore extends TabletStateStore {
               }
             }
           }
          if (suspensionTimestamp >= 0) {
            SuspendingTServer suspender = new SuspendingTServer(tls.current.getLocation(), suspensionTimestamp);
            suspender.setSuspension(m);
          }
        }
        if (tls.suspend != null && suspensionTimestamp < 0) {
          SuspendingTServer.clearSuspension(m);
         }
         if (tls.future != null) {
           tls.future.clearFutureLocation(m);
@@ -154,6 +168,29 @@ public class MetaDataStateStore extends TabletStateStore {
     }
   }
 
  @Override
  public void unsuspend(Collection<TabletLocationState> tablets) throws DistributedStoreException {
    BatchWriter writer = createBatchWriter();
    try {
      for (TabletLocationState tls : tablets) {
        if (tls.suspend != null) {
          continue;
        }
        Mutation m = new Mutation(tls.extent.getMetadataEntry());
        SuspendingTServer.clearSuspension(m);
        writer.addMutation(m);
      }
    } catch (Exception ex) {
      throw new DistributedStoreException(ex);
    } finally {
      try {
        writer.close();
      } catch (MutationsRejectedException e) {
        throw new DistributedStoreException(e);
      }
    }
  }

   @Override
   public String name() {
     return "Normal Tablets";
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataTableScanner.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataTableScanner.java
index 14486a635..5d6052ffb 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataTableScanner.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/MetaDataTableScanner.java
@@ -78,6 +78,7 @@ public class MetaDataTableScanner implements ClosableIterator<TabletLocationStat
     scanner.fetchColumnFamily(TabletsSection.CurrentLocationColumnFamily.NAME);
     scanner.fetchColumnFamily(TabletsSection.FutureLocationColumnFamily.NAME);
     scanner.fetchColumnFamily(TabletsSection.LastLocationColumnFamily.NAME);
    scanner.fetchColumnFamily(TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN.getColumnFamily());
     scanner.fetchColumnFamily(LogColumnFamily.NAME);
     scanner.fetchColumnFamily(ChoppedColumnFamily.NAME);
     scanner.addScanIterator(new IteratorSetting(1000, "wholeRows", WholeRowIterator.class));
@@ -136,6 +137,7 @@ public class MetaDataTableScanner implements ClosableIterator<TabletLocationStat
     TServerInstance future = null;
     TServerInstance current = null;
     TServerInstance last = null;
    SuspendingTServer suspend = null;
     long lastTimestamp = 0;
     List<Collection<String>> walogs = new ArrayList<>();
     boolean chopped = false;
@@ -171,6 +173,8 @@ public class MetaDataTableScanner implements ClosableIterator<TabletLocationStat
         chopped = true;
       } else if (TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN.equals(cf, cq)) {
         extent = new KeyExtent(row, entry.getValue());
      } else if (TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN.equals(cf, cq)) {
        suspend = SuspendingTServer.fromValue(entry.getValue());
       }
     }
     if (extent == null) {
@@ -178,7 +182,7 @@ public class MetaDataTableScanner implements ClosableIterator<TabletLocationStat
       log.error(msg);
       throw new BadLocationStateException(msg, k.getRow());
     }
    return new TabletLocationState(extent, future, current, last, walogs, chopped);
    return new TabletLocationState(extent, future, current, last, suspend, walogs, chopped);
   }
 
   private TabletLocationState fetch() {
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/SuspendingTServer.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/SuspendingTServer.java
new file mode 100644
index 000000000..3f4e49e33
-- /dev/null
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/SuspendingTServer.java
@@ -0,0 +1,71 @@
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
package org.apache.accumulo.server.master.state;

import com.google.common.net.HostAndPort;
import java.util.Objects;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN;

/** For a suspended tablet, the time of suspension and the server it was suspended from. */
public class SuspendingTServer {
  public final HostAndPort server;
  public final long suspensionTime;

  SuspendingTServer(HostAndPort server, long suspensionTime) {
    this.server = Objects.requireNonNull(server);
    this.suspensionTime = suspensionTime;
  }

  public static SuspendingTServer fromValue(Value value) {
    String valStr = value.toString();
    String[] parts = valStr.split("[|]", 2);
    return new SuspendingTServer(HostAndPort.fromString(parts[0]), Long.parseLong(parts[1]));
  }

  public Value toValue() {
    return new Value(server.toString() + "|" + suspensionTime);
  }

  @Override
  public boolean equals(Object rhsObject) {
    if (!(rhsObject instanceof SuspendingTServer)) {
      return false;
    }
    SuspendingTServer rhs = (SuspendingTServer) rhsObject;
    return server.equals(rhs.server) && suspensionTime == rhs.suspensionTime;
  }

  public void setSuspension(Mutation m) {
    m.put(SUSPEND_COLUMN.getColumnFamily(), SUSPEND_COLUMN.getColumnQualifier(), toValue());
  }

  public static void clearSuspension(Mutation m) {
    m.putDelete(SUSPEND_COLUMN.getColumnFamily(), SUSPEND_COLUMN.getColumnQualifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(server, suspensionTime);
  }

  @Override
  public String toString() {
    return server.toString() + "[" + suspensionTime + "]";
  }
}
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletLocationState.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletLocationState.java
index 8116ecf9e..784bd337a 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletLocationState.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletLocationState.java
@@ -46,12 +46,13 @@ public class TabletLocationState {
     }
   }
 
  public TabletLocationState(KeyExtent extent, TServerInstance future, TServerInstance current, TServerInstance last, Collection<Collection<String>> walogs,
      boolean chopped) throws BadLocationStateException {
  public TabletLocationState(KeyExtent extent, TServerInstance future, TServerInstance current, TServerInstance last, SuspendingTServer suspend,
      Collection<Collection<String>> walogs, boolean chopped) throws BadLocationStateException {
     this.extent = extent;
     this.future = future;
     this.current = current;
     this.last = last;
    this.suspend = suspend;
     if (walogs == null)
       walogs = Collections.emptyList();
     this.walogs = walogs;
@@ -65,6 +66,7 @@ public class TabletLocationState {
   final public TServerInstance future;
   final public TServerInstance current;
   final public TServerInstance last;
  final public SuspendingTServer suspend;
   final public Collection<Collection<String>> walogs;
   final public boolean chopped;
 
@@ -92,23 +94,29 @@ public class TabletLocationState {
     return result;
   }
 
  private static final int _HAS_CURRENT = 1 << 0;
  private static final int _HAS_FUTURE = 1 << 1;
  private static final int _HAS_SUSPEND = 1 << 2;

   public TabletState getState(Set<TServerInstance> liveServers) {
    TServerInstance server = getServer();
    if (server == null)
      return TabletState.UNASSIGNED;
    if (server.equals(current) || server.equals(future)) {
      if (liveServers.contains(server))
        if (server.equals(future)) {
          return TabletState.ASSIGNED;
        } else {
          return TabletState.HOSTED;
        }
      else {
        return TabletState.ASSIGNED_TO_DEAD_SERVER;
      }
    switch ((current == null ? 0 : _HAS_CURRENT) | (future == null ? 0 : _HAS_FUTURE) | (suspend == null ? 0 : _HAS_SUSPEND)) {
      case 0:
        return TabletState.UNASSIGNED;

      case _HAS_SUSPEND:
        return TabletState.SUSPENDED;

      case _HAS_FUTURE:
      case (_HAS_FUTURE | _HAS_SUSPEND):
        return liveServers.contains(future) ? TabletState.ASSIGNED : TabletState.ASSIGNED_TO_DEAD_SERVER;

      case _HAS_CURRENT:
      case (_HAS_CURRENT | _HAS_SUSPEND):
        return liveServers.contains(current) ? TabletState.HOSTED : TabletState.ASSIGNED_TO_DEAD_SERVER;

      default:
        // Both current and future are set, which is prevented by constructor.
        throw new IllegalStateException();
     }
    // server == last
    return TabletState.UNASSIGNED;
   }

 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletState.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletState.java
index d69ca198e..bd0e8858a 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletState.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletState.java
@@ -17,5 +17,5 @@
 package org.apache.accumulo.server.master.state;
 
 public enum TabletState {
  UNASSIGNED, ASSIGNED, HOSTED, ASSIGNED_TO_DEAD_SERVER
  UNASSIGNED, ASSIGNED, HOSTED, ASSIGNED_TO_DEAD_SERVER, SUSPENDED
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
index 973bbd2e3..00f86c632 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
@@ -196,6 +196,7 @@ public class TabletStateChangeIterator extends SkippingIterator {
           break;
         case ASSIGNED_TO_DEAD_SERVER:
           return;
        case SUSPENDED:
         case UNASSIGNED:
           if (shouldBeOnline)
             return;
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateStore.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateStore.java
index 3ead237af..6872466e9 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateStore.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/TabletStateStore.java
@@ -20,6 +20,7 @@ import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
import org.apache.accumulo.core.data.impl.KeyExtent;
 
 import org.apache.accumulo.server.AccumuloServerContext;
 import org.apache.hadoop.fs.Path;
@@ -64,28 +65,38 @@ public abstract class TabletStateStore implements Iterable<TabletLocationState>
    */
   abstract public void unassign(Collection<TabletLocationState> tablets, Map<TServerInstance,List<Path>> logsForDeadServers) throws DistributedStoreException;
 
  /**
   * Mark tablets as having no known or future location, but desiring to be returned to their previous tserver.
   */
  abstract public void suspend(Collection<TabletLocationState> tablets, Map<TServerInstance,List<Path>> logsForDeadServers, long suspensionTimestamp)
      throws DistributedStoreException;

  /**
   * Remove a suspension marker for a collection of tablets, moving them to being simply unassigned.
   */
  abstract public void unsuspend(Collection<TabletLocationState> tablets) throws DistributedStoreException;

   public static void unassign(AccumuloServerContext context, TabletLocationState tls, Map<TServerInstance,List<Path>> logsForDeadServers)
       throws DistributedStoreException {
    TabletStateStore store;
    if (tls.extent.isRootTablet()) {
      store = new ZooTabletStateStore();
    } else if (tls.extent.isMeta()) {
      store = new RootTabletStateStore(context);
    } else {
      store = new MetaDataStateStore(context);
    }
    store.unassign(Collections.singletonList(tls), logsForDeadServers);
    getStoreForTablet(tls.extent, context).unassign(Collections.singletonList(tls), logsForDeadServers);
  }

  public static void suspend(AccumuloServerContext context, TabletLocationState tls, Map<TServerInstance,List<Path>> logsForDeadServers,
      long suspensionTimestamp) throws DistributedStoreException {
    getStoreForTablet(tls.extent, context).suspend(Collections.singletonList(tls), logsForDeadServers, suspensionTimestamp);
   }
 
   public static void setLocation(AccumuloServerContext context, Assignment assignment) throws DistributedStoreException {
    TabletStateStore store;
    if (assignment.tablet.isRootTablet()) {
      store = new ZooTabletStateStore();
    } else if (assignment.tablet.isMeta()) {
      store = new RootTabletStateStore(context);
    getStoreForTablet(assignment.tablet, context).setLocations(Collections.singletonList(assignment));
  }

  protected static TabletStateStore getStoreForTablet(KeyExtent extent, AccumuloServerContext context) throws DistributedStoreException {
    if (extent.isRootTablet()) {
      return new ZooTabletStateStore();
    } else if (extent.isMeta()) {
      return new RootTabletStateStore(context);
     } else {
      store = new MetaDataStateStore(context);
      return new MetaDataStateStore(context);
     }
    store.setLocations(Collections.singletonList(assignment));
   }
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/state/ZooTabletStateStore.java b/server/base/src/main/java/org/apache/accumulo/server/master/state/ZooTabletStateStore.java
index 8f92db5cf..148b6cc9b 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/state/ZooTabletStateStore.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/state/ZooTabletStateStore.java
@@ -93,7 +93,7 @@ public class ZooTabletStateStore extends TabletStateStore {
               log.debug("root tablet log " + logEntry.filename);
             }
           }
          TabletLocationState result = new TabletLocationState(RootTable.EXTENT, futureSession, currentSession, lastSession, logs, false);
          TabletLocationState result = new TabletLocationState(RootTable.EXTENT, futureSession, currentSession, lastSession, null, logs, false);
           log.debug("Returning root tablet state: " + result);
           return result;
         } catch (Exception ex) {
@@ -189,6 +189,18 @@ public class ZooTabletStateStore extends TabletStateStore {
     log.debug("unassign root tablet location");
   }
 
  @Override
  public void suspend(Collection<TabletLocationState> tablets, Map<TServerInstance,List<Path>> logsForDeadServers, long suspensionTimestamp)
      throws DistributedStoreException {
    // No support for suspending root tablet.
    unassign(tablets, logsForDeadServers);
  }

  @Override
  public void unsuspend(Collection<TabletLocationState> tablets) throws DistributedStoreException {
    // no support for suspending root tablet.
  }

   @Override
   public String name() {
     return "Root Table";
diff --git a/server/base/src/test/java/org/apache/accumulo/server/master/state/TabletLocationStateTest.java b/server/base/src/test/java/org/apache/accumulo/server/master/state/TabletLocationStateTest.java
index f270fe485..bd81267b1 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/master/state/TabletLocationStateTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/master/state/TabletLocationStateTest.java
@@ -60,7 +60,7 @@ public class TabletLocationStateTest {
 
   @Test
   public void testConstruction_NoFuture() throws Exception {
    tls = new TabletLocationState(keyExtent, null, current, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, current, last, null, walogs, true);
     assertSame(keyExtent, tls.extent);
     assertNull(tls.future);
     assertSame(current, tls.current);
@@ -71,7 +71,7 @@ public class TabletLocationStateTest {
 
   @Test
   public void testConstruction_NoCurrent() throws Exception {
    tls = new TabletLocationState(keyExtent, future, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, future, null, last, null, walogs, true);
     assertSame(keyExtent, tls.extent);
     assertSame(future, tls.future);
     assertNull(tls.current);
@@ -85,7 +85,7 @@ public class TabletLocationStateTest {
     expect(keyExtent.getMetadataEntry()).andReturn(new Text("entry"));
     replay(keyExtent);
     try {
      new TabletLocationState(keyExtent, future, current, last, walogs, true);
      new TabletLocationState(keyExtent, future, current, last, null, walogs, true);
     } catch (TabletLocationState.BadLocationStateException e) {
       assertEquals(new Text("entry"), e.getEncodedEndRow());
       throw (e);
@@ -94,44 +94,44 @@ public class TabletLocationStateTest {
 
   @Test
   public void testConstruction_NoFuture_NoWalogs() throws Exception {
    tls = new TabletLocationState(keyExtent, null, current, last, null, true);
    tls = new TabletLocationState(keyExtent, null, current, last, null, null, true);
     assertNotNull(tls.walogs);
     assertEquals(0, tls.walogs.size());
   }
 
   @Test
   public void testGetServer_Current() throws Exception {
    tls = new TabletLocationState(keyExtent, null, current, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, current, last, null, walogs, true);
     assertSame(current, tls.getServer());
   }
 
   @Test
   public void testGetServer_Future() throws Exception {
    tls = new TabletLocationState(keyExtent, future, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, future, null, last, null, walogs, true);
     assertSame(future, tls.getServer());
   }
 
   @Test
   public void testGetServer_Last() throws Exception {
    tls = new TabletLocationState(keyExtent, null, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, null, last, null, walogs, true);
     assertSame(last, tls.getServer());
   }
 
   @Test
   public void testGetServer_None() throws Exception {
    tls = new TabletLocationState(keyExtent, null, null, null, walogs, true);
    tls = new TabletLocationState(keyExtent, null, null, null, null, walogs, true);
     assertNull(tls.getServer());
   }
 
   @Test
   public void testGetState_Unassigned1() throws Exception {
    tls = new TabletLocationState(keyExtent, null, null, null, walogs, true);
    tls = new TabletLocationState(keyExtent, null, null, null, null, walogs, true);
     assertEquals(TabletState.UNASSIGNED, tls.getState(null));
   }
 
   @Test
   public void testGetState_Unassigned2() throws Exception {
    tls = new TabletLocationState(keyExtent, null, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, null, last, null, walogs, true);
     assertEquals(TabletState.UNASSIGNED, tls.getState(null));
   }
 
@@ -139,7 +139,7 @@ public class TabletLocationStateTest {
   public void testGetState_Assigned() throws Exception {
     Set<TServerInstance> liveServers = new java.util.HashSet<>();
     liveServers.add(future);
    tls = new TabletLocationState(keyExtent, future, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, future, null, last, null, walogs, true);
     assertEquals(TabletState.ASSIGNED, tls.getState(liveServers));
   }
 
@@ -147,7 +147,7 @@ public class TabletLocationStateTest {
   public void testGetState_Hosted() throws Exception {
     Set<TServerInstance> liveServers = new java.util.HashSet<>();
     liveServers.add(current);
    tls = new TabletLocationState(keyExtent, null, current, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, current, last, null, walogs, true);
     assertEquals(TabletState.HOSTED, tls.getState(liveServers));
   }
 
@@ -155,7 +155,7 @@ public class TabletLocationStateTest {
   public void testGetState_Dead1() throws Exception {
     Set<TServerInstance> liveServers = new java.util.HashSet<>();
     liveServers.add(current);
    tls = new TabletLocationState(keyExtent, future, null, last, walogs, true);
    tls = new TabletLocationState(keyExtent, future, null, last, null, walogs, true);
     assertEquals(TabletState.ASSIGNED_TO_DEAD_SERVER, tls.getState(liveServers));
   }
 
@@ -163,7 +163,7 @@ public class TabletLocationStateTest {
   public void testGetState_Dead2() throws Exception {
     Set<TServerInstance> liveServers = new java.util.HashSet<>();
     liveServers.add(future);
    tls = new TabletLocationState(keyExtent, null, current, last, walogs, true);
    tls = new TabletLocationState(keyExtent, null, current, last, null, walogs, true);
     assertEquals(TabletState.ASSIGNED_TO_DEAD_SERVER, tls.getState(liveServers));
   }
 }
diff --git a/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java b/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
index 691d02d79..4665836c8 100644
-- a/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
++ b/server/gc/src/test/java/org/apache/accumulo/gc/GarbageCollectWriteAheadLogsTest.java
@@ -65,8 +65,8 @@ public class GarbageCollectWriteAheadLogsTest {
   private final TabletLocationState tabletAssignedToServer2;
   {
     try {
      tabletAssignedToServer1 = new TabletLocationState(extent, (TServerInstance) null, server1, (TServerInstance) null, walogs, false);
      tabletAssignedToServer2 = new TabletLocationState(extent, (TServerInstance) null, server2, (TServerInstance) null, walogs, false);
      tabletAssignedToServer1 = new TabletLocationState(extent, (TServerInstance) null, server1, (TServerInstance) null, null, walogs, false);
      tabletAssignedToServer2 = new TabletLocationState(extent, (TServerInstance) null, server2, (TServerInstance) null, null, walogs, false);
     } catch (Exception ex) {
       throw new RuntimeException(ex);
     }
diff --git a/server/master/src/main/java/org/apache/accumulo/master/Master.java b/server/master/src/main/java/org/apache/accumulo/master/Master.java
index 10d9152f5..76fafb59d 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/Master.java
++ b/server/master/src/main/java/org/apache/accumulo/master/Master.java
@@ -74,6 +74,7 @@ import org.apache.accumulo.core.replication.thrift.ReplicationCoordinator;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.NamespacePermission;
 import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal;
 import org.apache.accumulo.core.trace.DistributedTrace;
 import org.apache.accumulo.core.trace.thrift.TInfo;
 import org.apache.accumulo.core.util.Daemon;
@@ -196,6 +197,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
   private ReplicationDriver replicationWorkDriver;
   private WorkDriver replicationWorkAssigner;
   RecoveryManager recoveryManager = null;
  private final MasterTime timeKeeper;
 
   // Delegation Token classes
   private final boolean delegationTokensAvailable;
@@ -533,7 +535,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     int result = 0;
     for (TabletGroupWatcher watcher : watchers) {
       for (TableCounts counts : watcher.getStats().values()) {
        result += counts.assigned() + counts.assignedToDeadServers();
        result += counts.assigned() + counts.assignedToDeadServers() + counts.suspended();
       }
     }
     return result;
@@ -552,7 +554,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
             TableCounts counts = entry.getValue();
             TableState tableState = manager.getTableState(tableId);
             if (tableState != null && tableState.equals(TableState.ONLINE)) {
              result += counts.unassigned() + counts.assignedToDeadServers() + counts.assigned();
              result += counts.unassigned() + counts.assignedToDeadServers() + counts.assigned() + counts.suspended();
             }
           }
         }
@@ -560,13 +562,15 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
       case SAFE_MODE:
         // Count offline tablets for the metadata table
         for (TabletGroupWatcher watcher : watchers) {
          result += watcher.getStats(MetadataTable.ID).unassigned();
          TableCounts counts = watcher.getStats(MetadataTable.ID);
          result += counts.unassigned() + counts.suspended();
         }
         break;
       case UNLOAD_METADATA_TABLETS:
       case UNLOAD_ROOT_TABLET:
         for (TabletGroupWatcher watcher : watchers) {
          result += watcher.getStats(MetadataTable.ID).unassigned();
          TableCounts counts = watcher.getStats(MetadataTable.ID);
          result += counts.unassigned() + counts.suspended();
         }
         break;
       default:
@@ -591,6 +595,8 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
 
     log.info("Version " + Constants.VERSION);
     log.info("Instance " + getInstance().getInstanceID());
    timeKeeper = new MasterTime(this);

     ThriftTransportPool.getInstance().setIdleTime(aconf.getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
     tserverSet = new LiveTServerSet(this, this);
     this.tabletBalancer = aconf.instantiateClassProperty(Property.MASTER_TABLET_BALANCER, TabletBalancer.class, new DefaultLoadBalancer());
@@ -626,6 +632,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
       log.info("SASL is not enabled, delegation tokens will not be available");
       delegationTokensAvailable = false;
     }

   }
 
   public TServerConnection getConnection(TServerInstance server) {
@@ -726,8 +733,19 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
   }
 
   static enum TabletGoalState {
    HOSTED, UNASSIGNED, DELETED
  }
    HOSTED(TUnloadTabletGoal.UNKNOWN), UNASSIGNED(TUnloadTabletGoal.UNASSIGNED), DELETED(TUnloadTabletGoal.DELETED), SUSPENDED(TUnloadTabletGoal.SUSPENDED);

    private final TUnloadTabletGoal unloadGoal;

    TabletGoalState(TUnloadTabletGoal unloadGoal) {
      this.unloadGoal = unloadGoal;
    }

    /** The purpose of unloading this tablet. */
    public TUnloadTabletGoal howUnload() {
      return unloadGoal;
    }
  };
 
   TabletGoalState getSystemGoalState(TabletLocationState tls) {
     switch (getMasterState()) {
@@ -773,7 +791,7 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     TabletGoalState state = getSystemGoalState(tls);
     if (state == TabletGoalState.HOSTED) {
       if (tls.current != null && serversToShutdown.contains(tls.current)) {
        return TabletGoalState.UNASSIGNED;
        return TabletGoalState.SUSPENDED;
       }
       // Handle merge transitions
       if (mergeInfo.getExtent() != null) {
@@ -974,7 +992,8 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     }
 
     private long updateStatus() throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
      tserverStatus = Collections.synchronizedSortedMap(gatherTableInformation());
      Set<TServerInstance> currentServers = tserverSet.getCurrentServers();
      tserverStatus = Collections.synchronizedSortedMap(gatherTableInformation(currentServers));
       checkForHeldServer(tserverStatus);
 
       if (!badServers.isEmpty()) {
@@ -986,6 +1005,12 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
       } else if (!serversToShutdown.isEmpty()) {
         log.debug("not balancing while shutting down servers " + serversToShutdown);
       } else {
        for (TabletGroupWatcher tgw : watchers) {
          if (!tgw.isSameTserversAsLastScan(currentServers)) {
            log.debug("not balancing just yet, as collection of live tservers is in flux");
            return DEFAULT_WAIT_FOR_WATCHER;
          }
        }
         return balanceTablets();
       }
       return DEFAULT_WAIT_FOR_WATCHER;
@@ -1042,12 +1067,11 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
 
   }
 
  private SortedMap<TServerInstance,TabletServerStatus> gatherTableInformation() {
  private SortedMap<TServerInstance,TabletServerStatus> gatherTableInformation(Set<TServerInstance> currentServers) {
     long start = System.currentTimeMillis();
     int threads = Math.max(getConfiguration().getCount(Property.MASTER_STATUS_THREAD_POOL_SIZE), 1);
     ExecutorService tp = Executors.newFixedThreadPool(threads);
     final SortedMap<TServerInstance,TabletServerStatus> result = new TreeMap<>();
    Set<TServerInstance> currentServers = tserverSet.getCurrentServers();
     for (TServerInstance serverInstance : currentServers) {
       final TServerInstance server = serverInstance;
       tp.submit(new Runnable() {
@@ -1133,9 +1157,30 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
       }
     });
 
    watchers.add(new TabletGroupWatcher(this, new MetaDataStateStore(this, this), null));
    watchers.add(new TabletGroupWatcher(this, new RootTabletStateStore(this, this), watchers.get(0)));
    watchers.add(new TabletGroupWatcher(this, new ZooTabletStateStore(new ZooStore(zroot)), watchers.get(1)));
    watchers.add(new TabletGroupWatcher(this, new MetaDataStateStore(this, this), null) {
      @Override
      boolean canSuspendTablets() {
        // Always allow user data tablets to enter suspended state.
        return true;
      }
    });

    watchers.add(new TabletGroupWatcher(this, new RootTabletStateStore(this, this), watchers.get(0)) {
      @Override
      boolean canSuspendTablets() {
        // Allow metadata tablets to enter suspended state only if so configured. Generally we'll want metadata tablets to
        // be immediately reassigned, even if there's a global table.suspension.duration setting.
        return getConfiguration().getBoolean(Property.MASTER_METADATA_SUSPENDABLE);
      }
    });

    watchers.add(new TabletGroupWatcher(this, new ZooTabletStateStore(new ZooStore(zroot)), watchers.get(1)) {
      @Override
      boolean canSuspendTablets() {
        // Never allow root tablet to enter suspended state.
        return false;
      }
    });
     for (TabletGroupWatcher watcher : watchers) {
       watcher.start();
     }
@@ -1248,6 +1293,9 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
     log.info("Shutting down fate.");
     fate.shutdown();
 
    log.info("Shutting down timekeeping.");
    timeKeeper.shutdown();

     final long deadline = System.currentTimeMillis() + MAX_CLEANUP_WAIT_TIME;
     statusThread.join(remaining(deadline));
     replicationWorkAssigner.join(remaining(deadline));
@@ -1620,4 +1668,12 @@ public class Master extends AccumuloServerContext implements LiveTServerSet.List
   public void removeBulkImportStatus(String directory) {
     bulkImportStatus.removeBulkImportStatus(Collections.singletonList(directory));
   }

  /**
   * Return how long (in milliseconds) there has been a master overseeing this cluster. This is an approximately monotonic clock, which will be approximately
   * consistent between different masters or different runs of the same master.
   */
  public Long getSteadyTime() {
    return timeKeeper.getTime();
  }
 }
diff --git a/server/master/src/main/java/org/apache/accumulo/master/MasterTime.java b/server/master/src/main/java/org/apache/accumulo/master/MasterTime.java
new file mode 100644
index 000000000..27c57f0cf
-- /dev/null
++ b/server/master/src/main/java/org/apache/accumulo/master/MasterTime.java
@@ -0,0 +1,108 @@
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
package org.apache.accumulo.master;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keep a persistent roughly monotone view of how long a master has been overseeing this cluster. */
public class MasterTime extends TimerTask {
  private static final Logger log = LoggerFactory.getLogger(MasterTime.class);

  private final String zPath;
  private final ZooReaderWriter zk;
  private final Master master;
  private final Timer timer;

  /** Difference between time stored in ZooKeeper and System.nanoTime() when we last read from ZooKeeper. */
  private long skewAmount;

  public MasterTime(Master master) throws IOException {
    this.zPath = ZooUtil.getRoot(master.getInstance()) + Constants.ZMASTER_TICK;
    this.zk = ZooReaderWriter.getInstance();
    this.master = master;

    try {
      zk.putPersistentData(zPath, "0".getBytes(StandardCharsets.UTF_8), NodeExistsPolicy.SKIP);
      skewAmount = Long.parseLong(new String(zk.getData(zPath, null), StandardCharsets.UTF_8)) - System.nanoTime();
    } catch (Exception ex) {
      throw new IOException("Error updating master time", ex);
    }

    this.timer = new Timer();
    timer.schedule(this, 0, MILLISECONDS.convert(10, SECONDS));
  }

  /**
   * How long has this cluster had a Master?
   *
   * @returns Approximate total duration this cluster has had a Master, in milliseconds.
   */
  public synchronized long getTime() {
    return MILLISECONDS.convert(System.nanoTime() + skewAmount, NANOSECONDS);
  }

  /** Shut down the time keeping. */
  public void shutdown() {
    timer.cancel();
  }

  @Override
  public void run() {
    switch (master.getMasterState()) {
    // If we don't have the lock, periodically re-read the value in ZooKeeper, in case there's another master we're
    // shadowing for.
      case INITIAL:
      case STOP:
        try {
          long zkTime = Long.parseLong(new String(zk.getData(zPath, null), StandardCharsets.UTF_8));
          synchronized (this) {
            skewAmount = zkTime - System.nanoTime();
          }
        } catch (Exception ex) {
          if (log.isDebugEnabled()) {
            log.debug("Failed to retrieve master tick time", ex);
          }
        }
        break;
      // If we do have the lock, periodically write our clock to ZooKeeper.
      case HAVE_LOCK:
      case SAFE_MODE:
      case NORMAL:
      case UNLOAD_METADATA_TABLETS:
      case UNLOAD_ROOT_TABLET:
        try {
          zk.putPersistentData(zPath, Long.toString(System.nanoTime() + skewAmount).getBytes(StandardCharsets.UTF_8), NodeExistsPolicy.OVERWRITE);
        } catch (Exception ex) {
          if (log.isDebugEnabled()) {
            log.debug("Failed to update master tick time", ex);
          }
        }
    }
  }
}
diff --git a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
index e20335bed..2cf7d9d04 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
++ b/server/master/src/main/java/org/apache/accumulo/master/TabletGroupWatcher.java
@@ -16,8 +16,8 @@
  */
 package org.apache.accumulo.master;
 
import com.google.common.collect.ImmutableSortedSet;
 import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.Math.min;
 
 import java.io.IOException;
 import java.util.ArrayList;
@@ -92,8 +92,13 @@ import org.apache.thrift.TException;
 
 import com.google.common.base.Optional;
 import com.google.common.collect.Iterators;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.conf.TableConfiguration;
import static java.lang.Math.min;
import java.util.SortedSet;
import static java.lang.Math.min;
 
class TabletGroupWatcher extends Daemon {
abstract class TabletGroupWatcher extends Daemon {
   // Constants used to make sure assignment logging isn't excessive in quantity or size
   private static final String ASSIGNMENT_BUFFER_SEPARATOR = ", ";
   private static final int ASSINGMENT_BUFFER_MAX_LENGTH = 4096;
@@ -101,9 +106,11 @@ class TabletGroupWatcher extends Daemon {
   private final Master master;
   final TabletStateStore store;
   final TabletGroupWatcher dependentWatcher;

   private MasterState masterState;
 
   final TableStats stats = new TableStats();
  private SortedSet<TServerInstance> lastScanServers = ImmutableSortedSet.of();
 
   TabletGroupWatcher(Master master, TabletStateStore store, TabletGroupWatcher dependentWatcher) {
     this.master = master;
@@ -111,6 +118,9 @@ class TabletGroupWatcher extends Daemon {
     this.dependentWatcher = dependentWatcher;
   }
 
  /** Should this {@code TabletGroupWatcher} suspend tablets? */
  abstract boolean canSuspendTablets();

   Map<String,TableCounts> getStats() {
     return stats.getLast();
   }
@@ -124,9 +134,13 @@ class TabletGroupWatcher extends Daemon {
     return stats.getLast(tableId);
   }
 
  /** True if the collection of live tservers specified in 'candidates' hasn't changed since the last time an assignment scan was started. */
  public synchronized boolean isSameTserversAsLastScan(Set<TServerInstance> candidates) {
    return candidates.equals(lastScanServers);
  }

   @Override
   public void run() {

     Thread.currentThread().setName("Watching " + store.name());
     int[] oldCounts = new int[TabletState.values().length];
     EventCoordinator.Listener eventListener = this.master.nextEvent.getListener();
@@ -158,6 +172,9 @@ class TabletGroupWatcher extends Daemon {
 
         if (currentTServers.size() == 0) {
           eventListener.waitForEvents(Master.TIME_TO_WAIT_BETWEEN_SCANS);
          synchronized (this) {
            lastScanServers = ImmutableSortedSet.of();
          }
           continue;
         }
 
@@ -165,9 +182,10 @@ class TabletGroupWatcher extends Daemon {
         SortedMap<TServerInstance,TabletServerStatus> destinations = new TreeMap<>(currentTServers);
         destinations.keySet().removeAll(this.master.serversToShutdown);
 
        List<Assignment> assignments = new ArrayList<>();
        List<Assignment> assigned = new ArrayList<>();
        List<Assignment> assignments = new ArrayList<Assignment>();
        List<Assignment> assigned = new ArrayList<Assignment>();
         List<TabletLocationState> assignedToDeadServers = new ArrayList<>();
        List<TabletLocationState> suspendedToGoneServers = new ArrayList<>();
         Map<KeyExtent,TServerInstance> unassigned = new HashMap<>();
         Map<TServerInstance,List<Path>> logsForDeadServers = new TreeMap<>();
 
@@ -192,15 +210,18 @@ class TabletGroupWatcher extends Daemon {
 
           // Don't overwhelm the tablet servers with work
           if (unassigned.size() + unloaded > Master.MAX_TSERVER_WORK_CHUNK * currentTServers.size()) {
            flushChanges(destinations, assignments, assigned, assignedToDeadServers, logsForDeadServers, unassigned);
            flushChanges(destinations, assignments, assigned, assignedToDeadServers, logsForDeadServers, suspendedToGoneServers, unassigned);
             assignments.clear();
             assigned.clear();
             assignedToDeadServers.clear();
            suspendedToGoneServers.clear();
             unassigned.clear();
             unloaded = 0;
             eventListener.waitForEvents(Master.TIME_TO_WAIT_BETWEEN_SCANS);
           }
           String tableId = tls.extent.getTableId();
          TableConfiguration tableConf = this.master.getConfigurationFactory().getTableConfiguration(tableId);

           MergeStats mergeStats = mergeStatsCache.get(tableId);
           if (mergeStats == null) {
             mergeStats = currentMerges.get(tableId);
@@ -226,7 +247,7 @@ class TabletGroupWatcher extends Daemon {
           }
 
           // if we are shutting down all the tabletservers, we have to do it in order
          if (goal == TabletGoalState.UNASSIGNED && state == TabletState.HOSTED) {
          if (goal == TabletGoalState.SUSPENDED && state == TabletState.HOSTED) {
             if (this.master.serversToShutdown.equals(currentTServers.keySet())) {
               if (dependentWatcher != null && dependentWatcher.assignedOrHosted() > 0) {
                 goal = TabletGoalState.HOSTED;
@@ -253,6 +274,29 @@ class TabletGroupWatcher extends Daemon {
                   logsForDeadServers.put(tserver, wals.getWalsInUse(tserver));
                 }
                 break;
              case SUSPENDED:
                if (master.getSteadyTime() - tls.suspend.suspensionTime < tableConf.getTimeInMillis(Property.TABLE_SUSPEND_DURATION)) {
                  // Tablet is suspended. See if its tablet server is back.
                  TServerInstance returnInstance = null;
                  Iterator<TServerInstance> find = destinations.tailMap(new TServerInstance(tls.suspend.server, " ")).keySet().iterator();
                  if (find.hasNext()) {
                    TServerInstance found = find.next();
                    if (found.getLocation().equals(tls.suspend.server)) {
                      returnInstance = found;
                    }
                  }

                  // Old tablet server is back. Return this tablet to its previous owner.
                  if (returnInstance != null) {
                    assignments.add(new Assignment(tls.extent, returnInstance));
                  } else {
                    // leave suspended, don't ask for a new assignment.
                  }
                } else {
                  // Treat as unassigned, ask for a new assignment.
                  unassigned.put(tls.extent, server);
                }
                break;
               case UNASSIGNED:
                 // maybe it's a finishing migration
                 TServerInstance dest = this.master.migrations.get(tls.extent);
@@ -276,6 +320,10 @@ class TabletGroupWatcher extends Daemon {
             }
           } else {
             switch (state) {
              case SUSPENDED:
                // Request a move to UNASSIGNED, so as to allow balancing to continue.
                suspendedToGoneServers.add(tls);
                // Fall through to unassigned to cancel migrations.
               case UNASSIGNED:
                 TServerInstance dest = this.master.migrations.get(tls.extent);
                 TableState tableState = TableManager.getInstance().getTableState(tls.extent.getTableId());
@@ -292,7 +340,7 @@ class TabletGroupWatcher extends Daemon {
               case HOSTED:
                 TServerConnection conn = this.master.tserverSet.getConnection(server);
                 if (conn != null) {
                  conn.unloadTablet(this.master.masterLock, tls.extent, goal != TabletGoalState.DELETED);
                  conn.unloadTablet(this.master.masterLock, tls.extent, goal.howUnload(), master.getSteadyTime());
                   unloaded++;
                   totalUnloaded++;
                 } else {
@@ -306,7 +354,7 @@ class TabletGroupWatcher extends Daemon {
           counts[state.ordinal()]++;
         }
 
        flushChanges(destinations, assignments, assigned, assignedToDeadServers, logsForDeadServers, unassigned);
        flushChanges(destinations, assignments, assigned, assignedToDeadServers, logsForDeadServers, suspendedToGoneServers, unassigned);
 
         // provide stats after flushing changes to avoid race conditions w/ delete table
         stats.end(masterState);
@@ -326,6 +374,9 @@ class TabletGroupWatcher extends Daemon {
 
         updateMergeState(mergeStatsCache);
 
        synchronized (this) {
          lastScanServers = ImmutableSortedSet.copyOf(currentTServers.keySet());
        }
         if (this.master.tserverSet.getCurrentServers().equals(currentTServers.keySet())) {
           Master.log.debug(String.format("[%s] sleeping for %.2f seconds", store.name(), Master.TIME_TO_WAIT_BETWEEN_SCANS / 1000.));
           eventListener.waitForEvents(Master.TIME_TO_WAIT_BETWEEN_SCANS);
@@ -749,15 +800,25 @@ class TabletGroupWatcher extends Daemon {
   }
 
   private void flushChanges(SortedMap<TServerInstance,TabletServerStatus> currentTServers, List<Assignment> assignments, List<Assignment> assigned,
      List<TabletLocationState> assignedToDeadServers, Map<TServerInstance,List<Path>> logsForDeadServers, Map<KeyExtent,TServerInstance> unassigned)
      throws DistributedStoreException, TException, WalMarkerException {
      List<TabletLocationState> assignedToDeadServers, Map<TServerInstance,List<Path>> logsForDeadServers, List<TabletLocationState> suspendedToGoneServers,
      Map<KeyExtent,TServerInstance> unassigned) throws DistributedStoreException, TException, WalMarkerException {
    boolean tabletsSuspendable = canSuspendTablets();
     if (!assignedToDeadServers.isEmpty()) {
       int maxServersToShow = min(assignedToDeadServers.size(), 100);
       Master.log.debug(assignedToDeadServers.size() + " assigned to dead servers: " + assignedToDeadServers.subList(0, maxServersToShow) + "...");
       Master.log.debug("logs for dead servers: " + logsForDeadServers);
      store.unassign(assignedToDeadServers, logsForDeadServers);
      if (tabletsSuspendable) {
        store.suspend(assignedToDeadServers, logsForDeadServers, master.getSteadyTime());
      } else {
        store.unassign(assignedToDeadServers, logsForDeadServers);
      }
       this.master.markDeadServerLogsAsClosed(logsForDeadServers);
      this.master.nextEvent.event("Marked %d tablets as unassigned because they don't have current servers", assignedToDeadServers.size());
      this.master.nextEvent.event("Marked %d tablets as suspended because they don't have current servers", assignedToDeadServers.size());
    }
    if (!suspendedToGoneServers.isEmpty()) {
      int maxServersToShow = min(assignedToDeadServers.size(), 100);
      Master.log.debug(assignedToDeadServers.size() + " suspended to gone servers: " + assignedToDeadServers.subList(0, maxServersToShow) + "...");
      store.unsuspend(suspendedToGoneServers);
     }
 
     if (!currentTServers.isEmpty()) {
diff --git a/server/master/src/main/java/org/apache/accumulo/master/state/MergeStats.java b/server/master/src/main/java/org/apache/accumulo/master/state/MergeStats.java
index a89100eb9..4cb858c1b 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/state/MergeStats.java
++ b/server/master/src/main/java/org/apache/accumulo/master/state/MergeStats.java
@@ -99,7 +99,7 @@ public class MergeStats {
     this.total++;
     if (state.equals(TabletState.HOSTED))
       this.hosted++;
    if (state.equals(TabletState.UNASSIGNED))
    if (state.equals(TabletState.UNASSIGNED) || state.equals(TabletState.SUSPENDED))
       this.unassigned++;
   }
 
@@ -217,7 +217,7 @@ public class MergeStats {
           return false;
         }
 
        if (tls.getState(master.onlineTabletServers()) != TabletState.UNASSIGNED) {
        if (tls.getState(master.onlineTabletServers()) != TabletState.UNASSIGNED && tls.getState(master.onlineTabletServers()) != TabletState.SUSPENDED) {
           log.debug("failing consistency: assigned or hosted " + tls);
           return false;
         }
diff --git a/server/master/src/main/java/org/apache/accumulo/master/state/TableCounts.java b/server/master/src/main/java/org/apache/accumulo/master/state/TableCounts.java
index 73395eaf0..dd44bc625 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/state/TableCounts.java
++ b/server/master/src/main/java/org/apache/accumulo/master/state/TableCounts.java
@@ -36,4 +36,8 @@ public class TableCounts {
   public int hosted() {
     return counts[TabletState.HOSTED.ordinal()];
   }

  public int suspended() {
    return counts[TabletState.SUSPENDED.ordinal()];
  }
 }
diff --git a/server/master/src/test/java/org/apache/accumulo/master/state/RootTabletStateStoreTest.java b/server/master/src/test/java/org/apache/accumulo/master/state/RootTabletStateStoreTest.java
index 0bc989e39..6497f96a1 100644
-- a/server/master/src/test/java/org/apache/accumulo/master/state/RootTabletStateStoreTest.java
++ b/server/master/src/test/java/org/apache/accumulo/master/state/RootTabletStateStoreTest.java
@@ -176,7 +176,7 @@ public class RootTabletStateStoreTest {
     assertEquals(count, 1);
     TabletLocationState assigned = null;
     try {
      assigned = new TabletLocationState(root, server, null, null, null, false);
      assigned = new TabletLocationState(root, server, null, null, null, null, false);
     } catch (BadLocationStateException e) {
       fail("Unexpected error " + e);
     }
@@ -203,7 +203,7 @@ public class RootTabletStateStoreTest {
 
     TabletLocationState broken = null;
     try {
      broken = new TabletLocationState(notRoot, server, null, null, null, false);
      broken = new TabletLocationState(notRoot, server, null, null, null, null, false);
     } catch (BadLocationStateException e) {
       fail("Unexpected error " + e);
     }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index 94e2ed919..c4df66d71 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -258,6 +258,9 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.net.HostAndPort;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal;
 
 public class TabletServer extends AccumuloServerContext implements Runnable {
 
@@ -329,7 +332,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
   private final ZooAuthenticationKeyWatcher authKeyWatcher;
   private final WalStateManager walMarker;
 
  public TabletServer(ServerConfigurationFactory confFactory, VolumeManager fs) {
  public TabletServer(ServerConfigurationFactory confFactory, VolumeManager fs) throws IOException {
     super(confFactory);
     this.confFactory = confFactory;
     this.fs = fs;
@@ -1549,7 +1552,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
     }
 
     @Override
    public void unloadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent textent, boolean save) {
    public void unloadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent textent, TUnloadTabletGoal goal, long requestTime) {
       try {
         checkPermission(credentials, lock, "unloadTablet");
       } catch (ThriftSecurityException e) {
@@ -1559,7 +1562,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
 
       KeyExtent extent = new KeyExtent(textent);
 
      resourceManager.addMigration(extent, new LoggingRunnable(log, new UnloadTabletHandler(extent, save)));
      resourceManager.addMigration(extent, new LoggingRunnable(log, new UnloadTabletHandler(extent, goal, requestTime)));
     }
 
     @Override
@@ -1939,11 +1942,13 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
 
   private class UnloadTabletHandler implements Runnable {
     private final KeyExtent extent;
    private final boolean saveState;
    private final TUnloadTabletGoal goalState;
    private final long requestTimeSkew;
 
    public UnloadTabletHandler(KeyExtent extent, boolean saveState) {
    public UnloadTabletHandler(KeyExtent extent, TUnloadTabletGoal goalState, long requestTime) {
       this.extent = extent;
      this.saveState = saveState;
      this.goalState = goalState;
      this.requestTimeSkew = requestTime - MILLISECONDS.convert(System.nanoTime(), NANOSECONDS);
     }
 
     @Override
@@ -1982,7 +1987,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       }
 
       try {
        t.close(saveState);
        t.close(!goalState.equals(TUnloadTabletGoal.DELETED));
       } catch (Throwable e) {
 
         if ((t.isClosing() || t.isClosed()) && e instanceof IllegalStateException) {
@@ -2003,12 +2008,18 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
         TServerInstance instance = new TServerInstance(clientAddress, getLock().getSessionId());
         TabletLocationState tls = null;
         try {
          tls = new TabletLocationState(extent, null, instance, null, null, false);
          tls = new TabletLocationState(extent, null, instance, null, null, null, false);
         } catch (BadLocationStateException e) {
           log.error("Unexpected error ", e);
         }
        log.debug("Unassigning " + tls);
        TabletStateStore.unassign(TabletServer.this, tls, null);
        if (!goalState.equals(TUnloadTabletGoal.SUSPENDED) || extent.isRootTablet()
            || (extent.isMeta() && !getConfiguration().getBoolean(Property.MASTER_METADATA_SUSPENDABLE))) {
          log.debug("Unassigning " + tls);
          TabletStateStore.unassign(TabletServer.this, tls, null);
        } else {
          log.debug("Suspending " + tls);
          TabletStateStore.suspend(TabletServer.this, tls, null, requestTimeSkew + MILLISECONDS.convert(System.nanoTime(), NANOSECONDS));
        }
       } catch (DistributedStoreException ex) {
         log.warn("Unable to update storage", ex);
       } catch (KeeperException e) {
diff --git a/test/src/main/java/org/apache/accumulo/test/master/MergeStateIT.java b/test/src/main/java/org/apache/accumulo/test/master/MergeStateIT.java
index 30584a6d5..2d233c416 100644
-- a/test/src/main/java/org/apache/accumulo/test/master/MergeStateIT.java
++ b/test/src/main/java/org/apache/accumulo/test/master/MergeStateIT.java
@@ -185,7 +185,7 @@ public class MergeStateIT extends ConfigurableMacBase {
     // take it offline
     m = tablet.getPrevRowUpdateMutation();
     Collection<Collection<String>> walogs = Collections.emptyList();
    metaDataStateStore.unassign(Collections.singletonList(new TabletLocationState(tablet, null, state.someTServer, null, walogs, false)), null);
    metaDataStateStore.unassign(Collections.singletonList(new TabletLocationState(tablet, null, state.someTServer, null, null, walogs, false)), null);
 
     // now we can split
     stats = scan(state, metaDataStateStore);
diff --git a/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java b/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java
new file mode 100644
index 000000000..edd1aff6b
-- /dev/null
++ b/test/src/main/java/org/apache/accumulo/test/master/SuspendedTabletsIT.java
@@ -0,0 +1,340 @@
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
package org.apache.accumulo.test.master;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ClientExec;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.MasterClient;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.minicluster.impl.ProcessReference;
import org.apache.accumulo.server.master.state.MetaDataTableScanner;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.test.functional.ConfigurableMacBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.common.net.HostAndPort;

public class SuspendedTabletsIT extends ConfigurableMacBase {
  private static final Logger log = LoggerFactory.getLogger(SuspendedTabletsIT.class);
  private static final Random RANDOM = new Random();
  private static ExecutorService THREAD_POOL;

  public static final int TSERVERS = 5;
  public static final long SUSPEND_DURATION = MILLISECONDS.convert(30, SECONDS);
  public static final int TABLETS = 100;

  @Override
  public void configure(MiniAccumuloConfigImpl cfg, Configuration fsConf) {
    cfg.setProperty(Property.TABLE_SUSPEND_DURATION, SUSPEND_DURATION + "ms");
    cfg.setProperty(Property.INSTANCE_ZK_TIMEOUT, "5s");
    cfg.setNumTservers(TSERVERS);
  }

  @Test
  public void crashAndResumeTserver() throws Exception {
    // Run the test body. When we get to the point where we need a tserver to go away, get rid of it via crashing
    suspensionTestBody(new TServerKiller() {
      @Override
      public void eliminateTabletServers(ClientContext ctx, TabletLocations locs, int count) throws Exception {
        List<ProcessReference> procs = new ArrayList<>(getCluster().getProcesses().get(ServerType.TABLET_SERVER));
        Collections.shuffle(procs);

        for (int i = 0; i < count; ++i) {
          ProcessReference pr = procs.get(i);
          log.info("Crashing {}", pr.getProcess());
          getCluster().killProcess(ServerType.TABLET_SERVER, pr);
        }
      }
    });
  }

  @Test
  public void shutdownAndResumeTserver() throws Exception {
    // Run the test body. When we get to the point where we need tservers to go away, stop them via a clean shutdown.
    suspensionTestBody(new TServerKiller() {
      @Override
      public void eliminateTabletServers(final ClientContext ctx, TabletLocations locs, int count) throws Exception {
        Set<TServerInstance> tserversSet = new HashSet<>();
        for (TabletLocationState tls : locs.locationStates.values()) {
          if (tls.current != null) {
            tserversSet.add(tls.current);
          }
        }
        List<TServerInstance> tserversList = new ArrayList<>(tserversSet);
        Collections.shuffle(tserversList, RANDOM);

        for (int i = 0; i < count; ++i) {
          final String tserverName = tserversList.get(i).toString();
          MasterClient.execute(ctx, new ClientExec<MasterClientService.Client>() {
            @Override
            public void execute(MasterClientService.Client client) throws Exception {
              log.info("Sending shutdown command to {} via MasterClientService", tserverName);
              client.shutdownTabletServer(null, ctx.rpcCreds(), tserverName, false);
            }
          });
        }

        log.info("Waiting for tserver process{} to die", count == 1 ? "" : "es");
        for (int i = 0; i < 10; ++i) {
          List<ProcessReference> deadProcs = new ArrayList<>();
          for (ProcessReference pr : getCluster().getProcesses().get(ServerType.TABLET_SERVER)) {
            Process p = pr.getProcess();
            if (!p.isAlive()) {
              deadProcs.add(pr);
            }
          }
          for (ProcessReference pr : deadProcs) {
            log.info("Process {} is dead, informing cluster control about this", pr.getProcess());
            getCluster().getClusterControl().killProcess(ServerType.TABLET_SERVER, pr);
            --count;
          }
          if (count == 0) {
            return;
          } else {
            Thread.sleep(MILLISECONDS.convert(2, SECONDS));
          }
        }
        throw new IllegalStateException("Tablet servers didn't die!");
      }
    });
  }

  /**
   * Main test body for suspension tests.
   *
   * @param serverStopper
   *          callback which shuts down some tablet servers.
   */
  private void suspensionTestBody(TServerKiller serverStopper) throws Exception {
    Credentials creds = new Credentials("root", new PasswordToken(ROOT_PASSWORD));
    Instance instance = new ZooKeeperInstance(getCluster().getClientConfig());
    ClientContext ctx = new ClientContext(instance, creds, getCluster().getClientConfig());

    String tableName = getUniqueNames(1)[0];

    Connector conn = ctx.getConnector();

    // Create a table with a bunch of splits
    log.info("Creating table " + tableName);
    conn.tableOperations().create(tableName);
    SortedSet<Text> splitPoints = new TreeSet<>();
    for (int i = 1; i < TABLETS; ++i) {
      splitPoints.add(new Text("" + i));
    }
    conn.tableOperations().addSplits(tableName, splitPoints);

    // Wait for all of the tablets to hosted ...
    log.info("Waiting on hosting and balance");
    TabletLocations ds;
    for (ds = TabletLocations.retrieve(ctx, tableName); ds.hostedCount != TABLETS; ds = TabletLocations.retrieve(ctx, tableName)) {
      Thread.sleep(1000);
    }

    // ... and balanced.
    conn.instanceOperations().waitForBalance();
    do {
      // Give at least another 5 seconds for migrations to finish up
      Thread.sleep(5000);
      ds = TabletLocations.retrieve(ctx, tableName);
    } while (ds.hostedCount != TABLETS);

    // Pray all of our tservers have at least 1 tablet.
    Assert.assertEquals(TSERVERS, ds.hosted.keySet().size());

    // Kill two tablet servers hosting our tablets. This should put tablets into suspended state, and thus halt balancing.

    TabletLocations beforeDeathState = ds;
    log.info("Eliminating tablet servers");
    serverStopper.eliminateTabletServers(ctx, beforeDeathState, 2);

    // Eventually some tablets will be suspended.
    log.info("Waiting on suspended tablets");
    ds = TabletLocations.retrieve(ctx, tableName);
    // Until we can scan the metadata table, the master probably can't either, so won't have been able to suspend the tablets.
    // So we note the time that we were first able to successfully scan the metadata table.
    long killTime = System.nanoTime();
    while (ds.suspended.keySet().size() != 2) {
      Thread.sleep(1000);
      ds = TabletLocations.retrieve(ctx, tableName);
    }

    SetMultimap<HostAndPort,KeyExtent> deadTabletsByServer = ds.suspended;

    // By this point, all tablets should be either hosted or suspended. All suspended tablets should
    // "belong" to the dead tablet servers, and should be in exactly the same place as before any tserver death.
    for (HostAndPort server : deadTabletsByServer.keySet()) {
      Assert.assertEquals(deadTabletsByServer.get(server), beforeDeathState.hosted.get(server));
    }
    Assert.assertEquals(TABLETS, ds.hostedCount + ds.suspendedCount);

    // Restart the first tablet server, making sure it ends up on the same port
    HostAndPort restartedServer = deadTabletsByServer.keySet().iterator().next();
    log.info("Restarting " + restartedServer);
    getCluster().getClusterControl().start(ServerType.TABLET_SERVER, null,
        ImmutableMap.of(Property.TSERV_CLIENTPORT.getKey(), "" + restartedServer.getPort(), Property.TSERV_PORTSEARCH.getKey(), "false"), 1);

    // Eventually, the suspended tablets should be reassigned to the newly alive tserver.
    log.info("Awaiting tablet unsuspension for tablets belonging to " + restartedServer);
    for (ds = TabletLocations.retrieve(ctx, tableName); ds.suspended.containsKey(restartedServer) || ds.assignedCount != 0; ds = TabletLocations.retrieve(ctx,
        tableName)) {
      Thread.sleep(1000);
    }
    Assert.assertEquals(deadTabletsByServer.get(restartedServer), ds.hosted.get(restartedServer));

    // Finally, after much longer, remaining suspended tablets should be reassigned.
    log.info("Awaiting tablet reassignment for remaining tablets");
    for (ds = TabletLocations.retrieve(ctx, tableName); ds.hostedCount != TABLETS; ds = TabletLocations.retrieve(ctx, tableName)) {
      Thread.sleep(1000);
    }

    long recoverTime = System.nanoTime();
    Assert.assertTrue(recoverTime - killTime >= NANOSECONDS.convert(SUSPEND_DURATION, MILLISECONDS));
  }

  private static interface TServerKiller {
    public void eliminateTabletServers(ClientContext ctx, TabletLocations locs, int count) throws Exception;
  }

  private static final AtomicInteger threadCounter = new AtomicInteger(0);

  @BeforeClass
  public static void init() {
    THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Scanning deadline thread #" + threadCounter.incrementAndGet());
      }
    });
  }

  @AfterClass
  public static void cleanup() {
    THREAD_POOL.shutdownNow();
  }

  private static class TabletLocations {
    public final Map<KeyExtent,TabletLocationState> locationStates = new HashMap<>();
    public final SetMultimap<HostAndPort,KeyExtent> hosted = HashMultimap.create();
    public final SetMultimap<HostAndPort,KeyExtent> suspended = HashMultimap.create();
    public int hostedCount = 0;
    public int assignedCount = 0;
    public int suspendedCount = 0;
    public int unassignedCount = 0;

    private TabletLocations() {}

    public static TabletLocations retrieve(final ClientContext ctx, final String tableName) throws Exception {
      int sleepTime = 200;
      int remainingAttempts = 30;

      while (true) {
        try {
          FutureTask<TabletLocations> tlsFuture = new FutureTask<>(new Callable<TabletLocations>() {
            @Override
            public TabletLocations call() throws Exception {
              TabletLocations answer = new TabletLocations();
              answer.scan(ctx, tableName);
              return answer;
            }
          });
          THREAD_POOL.submit(tlsFuture);
          return tlsFuture.get(5, SECONDS);
        } catch (TimeoutException ex) {
          log.debug("Retrieval timed out", ex);
        } catch (Exception ex) {
          log.warn("Failed to scan metadata", ex);
        }
        sleepTime = Math.min(2 * sleepTime, 10000);
        Thread.sleep(sleepTime);
        --remainingAttempts;
        if (remainingAttempts == 0) {
          Assert.fail("Scanning of metadata failed, aborting");
        }
      }
    }

    private void scan(ClientContext ctx, String tableName) throws Exception {
      Map<String,String> idMap = ctx.getConnector().tableOperations().tableIdMap();
      String tableId = Objects.requireNonNull(idMap.get(tableName));
      try (MetaDataTableScanner scanner = new MetaDataTableScanner(ctx, new Range())) {
        while (scanner.hasNext()) {
          TabletLocationState tls = scanner.next();

          if (!tls.extent.getTableId().equals(tableId)) {
            continue;
          }
          locationStates.put(tls.extent, tls);
          if (tls.suspend != null) {
            suspended.put(tls.suspend.server, tls.extent);
            ++suspendedCount;
          } else if (tls.current != null) {
            hosted.put(tls.current.getLocation(), tls.extent);
            ++hostedCount;
          } else if (tls.future != null) {
            ++assignedCount;
          } else {
            unassignedCount += 1;
          }
        }
      }
    }
  }
}
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index 63a77712b..05a0c54c0 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -79,6 +79,7 @@ import com.beust.jcommander.Parameter;
 import com.google.common.net.HostAndPort;
 
 import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import org.apache.accumulo.core.tabletserver.thrift.TUnloadTabletGoal;
 
 /**
  * The purpose of this class is to server as fake tserver that is a data sink like /dev/null. NullTserver modifies the metadata location entries for a table to
@@ -179,7 +180,7 @@ public class NullTserver {
     public void loadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent extent) throws TException {}
 
     @Override
    public void unloadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent extent, boolean save) throws TException {}
    public void unloadTablet(TInfo tinfo, TCredentials credentials, String lock, TKeyExtent extent, TUnloadTabletGoal goal, long requestTime) throws TException {}
 
     @Override
     public List<ActiveScan> getActiveScans(TInfo tinfo, TCredentials credentials) throws ThriftSecurityException, TException {
- 
2.19.1.windows.1

