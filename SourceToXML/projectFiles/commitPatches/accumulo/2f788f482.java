From 2f788f4826d0fd6d96ad04def79320e8479c9108 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Thu, 4 Dec 2014 22:33:00 -0500
Subject: [PATCH] ACCUMULO-1798 Add ability to specify compaction strategy for
 user specificed compactions.

--
 .../core/client/admin/CompactionConfig.java   |  155 ++
 .../admin/CompactionStrategyConfig.java       |   74 +
 .../core/client/admin/TableOperations.java    |   17 +
 .../impl/CompactionStrategyConfigUtil.java    |   98 ++
 .../core/client/impl/TableOperationsImpl.java |   20 +-
 .../core/client/mock/MockTableOperations.java |   13 +
 .../impl/TableOperationsHelperTest.java       |    5 +
 proxy/src/main/cpp/AccumuloProxy.cpp          | 1479 ++++++++--------
 proxy/src/main/cpp/AccumuloProxy.h            |   25 +-
 .../cpp/AccumuloProxy_server.skeleton.cpp     |    2 +-
 proxy/src/main/cpp/proxy_types.cpp            |   99 ++
 proxy/src/main/cpp/proxy_types.h              |   51 +
 .../apache/accumulo/proxy/ProxyServer.java    |   18 +-
 .../accumulo/proxy/thrift/AccumuloProxy.java  | 1545 +++++++++--------
 .../thrift/CompactionStrategyConfig.java      |  556 ++++++
 proxy/src/main/python/AccumuloProxy-remote    |    8 +-
 proxy/src/main/python/AccumuloProxy.py        |  560 +++---
 proxy/src/main/python/ttypes.py               |   82 +
 proxy/src/main/ruby/accumulo_proxy.rb         |   14 +-
 proxy/src/main/ruby/proxy_types.rb            |   18 +
 proxy/src/main/thrift/proxy.thrift            |    9 +-
 ...erators.java => UserCompactionConfig.java} |   44 +-
 .../accumulo/master/FateServiceHandler.java   |    5 +-
 .../master/tableOps/CompactRange.java         |   99 +-
 .../apache/accumulo/tserver/TabletServer.java |    9 +-
 .../tserver/TabletServerResourceManager.java  |   10 +-
 .../tserver/compaction/CompactionPlan.java    |    3 +-
 .../compaction/CompactionStrategy.java        |   10 +
 .../compaction/DefaultCompactionStrategy.java |    5 +-
 .../EverythingCompactionStrategy.java         |   39 +
 .../SizeLimitCompactionStrategy.java          |    4 +-
 .../accumulo/tserver/tablet/Tablet.java       |  134 +-
 .../shell/commands/CompactCommand.java        |   55 +-
 .../apache/accumulo/proxy/SimpleProxyIT.java  |   47 +-
 .../apache/accumulo/test/ShellServerIT.java   |   13 +-
 .../test/UserCompactionStrategyIT.java        |  337 ++++
 .../test/functional/FunctionalTestUtils.java  |   13 +-
 37 files changed, 3743 insertions(+), 1932 deletions(-)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
 create mode 100644 proxy/src/main/java/org/apache/accumulo/proxy/thrift/CompactionStrategyConfig.java
 rename server/base/src/main/java/org/apache/accumulo/server/master/tableOps/{CompactionIterators.java => UserCompactionConfig.java} (79%)
 create mode 100644 server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/EverythingCompactionStrategy.java
 create mode 100644 test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java

diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
new file mode 100644
index 000000000..f59c70b85
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionConfig.java
@@ -0,0 +1,155 @@
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

package org.apache.accumulo.core.client.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.hadoop.io.Text;

/**
 * This class exist to pass parameters to {@link TableOperations#compact(String, CompactionConfig)}
 * 
 * @since 1.7.0
 */

public class CompactionConfig {
  private Text start = null;
  private Text end = null;
  private boolean flush = true;
  private boolean wait = true;
  private List<IteratorSetting> iterators = Collections.emptyList();
  private CompactionStrategyConfig compactionStrategy = new CompactionStrategyConfig("org.apache.accumulo.tserver.compaction.EverythingCompactionStrategy") {
    @Override
    public CompactionStrategyConfig setOptions(Map<String,String> opts) {
      throw new UnsupportedOperationException();
    }
  };

  /**
   * @param start
   *          First tablet to be compacted contains the row after this row, null means the first tablet in table. The default is null.
   * @return this
   */

  public CompactionConfig setStartRow(Text start) {
    this.start = start;
    return this;
  }

  /**
   * @return The previously set start row. The default is null.
   */
  public Text getStartRow() {
    return start;
  }

  /**
   * 
   * @param end
   *          Last tablet to be compacted contains this row, null means the last tablet in table. The default is null.
   * @return this
   */
  public CompactionConfig setEndRow(Text end) {
    this.end = end;
    return this;
  }

  /**
   * @return The previously set end row. The default is null.
   */
  public Text getEndRow() {
    return end;
  }

  /**
   * @param flush
   *          If set to true, will flush in memory data of all tablets in range before compacting. If not set, the default is true.
   * @return this
   */
  public CompactionConfig setFlush(boolean flush) {
    this.flush = flush;
    return this;
  }

  /**
   * @return The previously set flush. The default is true.
   */
  public boolean getFlush() {
    return flush;
  }

  /**
   * @param wait
   *          If set to true, will cause compact operation to wait for all tablets in range to compact. If not set, the default is true.
   * @return this
   */

  public CompactionConfig setWait(boolean wait) {
    this.wait = wait;
    return this;
  }

  /**
   * 
   * @return The previously set wait. The default is true.
   */
  public boolean getWait() {
    return wait;
  }

  /**
   * @param iterators
   *          configures the iterators that will be used when compacting tablets. These iterators are merged with current iterators configured for the table.
   * @return this
   */
  public CompactionConfig setIterators(List<IteratorSetting> iterators) {
    this.iterators = new ArrayList<>(iterators);
    return this;
  }

  /**
   * @return The previously set iterators. Returns an empty list if not set. The returned list is unmodifiable.
   */
  public List<IteratorSetting> getIterators() {
    return Collections.unmodifiableList(iterators);
  }

  /**
   * @param csConfig
   *          configures the strategy that will be used by each tablet to select files. If no strategy is set, then all files will be compacted.
   * @return this
   */
  public CompactionConfig setCompactionStrategy(CompactionStrategyConfig csConfig) {
    Preconditions.checkNotNull(csConfig);
    this.compactionStrategy = csConfig;
    return this;
  }

  /**
   * @return The previously set compaction strategy. Defaults to a configuration of org.apache.accumulo.tserver.compaction.EverythingCompactionStrategy which
   *         always compacts all files.
   */
  public CompactionStrategyConfig getCompactionStrategy() {
    return compactionStrategy;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
new file mode 100644
index 000000000..14b275e7c
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/CompactionStrategyConfig.java
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

package org.apache.accumulo.core.client.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * 
 * @since 1.7.0
 */

public class CompactionStrategyConfig {
  private String className;
  private Map<String,String> options = Collections.emptyMap();

  /**
   * 
   * @param className
   *          The name of a class that implements org.apache.accumulo.tserver.compaction.CompactionStrategy. This class must be exist on tservers.
   */

  public CompactionStrategyConfig(String className) {
    Preconditions.checkNotNull(className);
    this.className = className;
  }

  /**
   * @return the class name passed to the constructor.
   */
  public String getClassName() {
    return className;
  }

  /**
   * @param opts
   *          The options that will be passed to the init() method of the compaction strategy when its instantiated on a tserver. This method will copy the map.
   *          The default is an empty map.
   * @return this
   */

  public CompactionStrategyConfig setOptions(Map<String,String> opts) {
    Preconditions.checkNotNull(opts);
    this.options = new HashMap<>(opts);
    return this;
  }

  /**
   * 
   * @return The previously set options. Returns an unmodifiable map. The default is an empty map.
   */

  public Map<String,String> getOptions() {
    return Collections.unmodifiableMap(options);
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
index 97f538dd2..75bdf8d7b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
@@ -288,6 +288,23 @@ public interface TableOperations {
   void compact(String tableName, Text start, Text end, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloSecurityException,
       TableNotFoundException, AccumuloException;
 
  /**
   * Starts a full major compaction of the tablets in the range (start, end]. If the config does not specify a compaction strategy, then all files in a tablet
   * are compacted. The compaction is performed even for tablets that have only one file.
   * 
   * <p>
   * Only one compact call at a time can pass iterators and/or a compaction strategy. If two threads call compaction with iterators and/or a copmaction
   * strategy, then one will fail.
   * 
   * @param tableName
   *          the table to compact
   * @param config
   *          the configuration to use
   * 
   * @since 1.7.0
   */
  void compact(String tableName, CompactionConfig config) throws AccumuloSecurityException, TableNotFoundException, AccumuloException;

   /**
    * Cancels a user initiated major compaction of a table initiated with {@link #compact(String, Text, Text, boolean, boolean)} or
    * {@link #compact(String, Text, Text, List, boolean, boolean)}. Compactions of tablets that are currently running may finish, but new compactions of tablets
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java b/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
new file mode 100644
index 000000000..8dce87764
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/CompactionStrategyConfigUtil.java
@@ -0,0 +1,98 @@
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

package org.apache.accumulo.core.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;

public class CompactionStrategyConfigUtil {

  private static final int MAGIC = 0xcc5e6024;

  public static void encode(DataOutput dout, CompactionStrategyConfig csc) throws IOException {

    dout.writeInt(MAGIC);
    dout.writeByte(1);

    dout.writeUTF(csc.getClassName());
    dout.writeInt(csc.getOptions().size());

    for (Entry<String,String> entry : csc.getOptions().entrySet()) {
      dout.writeUTF(entry.getKey());
      dout.writeUTF(entry.getValue());
    }

  }

  public static byte[] encode(CompactionStrategyConfig csc) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);

    try {
      encode(dos, csc);
      dos.close();

      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public static CompactionStrategyConfig decode(DataInput din) throws IOException {
    if (din.readInt() != MAGIC) {
      throw new IllegalArgumentException("Unexpected MAGIC ");
    }

    if (din.readByte() != 1) {
      throw new IllegalArgumentException("Unexpected version");
    }

    String classname = din.readUTF();
    int numEntries = din.readInt();

    HashMap<String,String> opts = new HashMap<>();

    for (int i = 0; i < numEntries; i++) {
      String k = din.readUTF();
      String v = din.readUTF();
      opts.put(k, v);
    }

    return new CompactionStrategyConfig(classname).setOptions(opts);
  }

  public static CompactionStrategyConfig decode(byte[] encodedCsc) {
    ByteArrayInputStream bais = new ByteArrayInputStream(encodedCsc);
    DataInputStream dis = new DataInputStream(bais);

    try {
      return decode(dis);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
index 77b6a0102..1def09136 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
@@ -48,6 +48,8 @@ import java.util.concurrent.atomic.AtomicReference;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipInputStream;
 
import org.apache.accumulo.core.client.admin.CompactionConfig;

 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
@@ -114,7 +116,6 @@ import org.apache.log4j.Logger;
 import org.apache.thrift.TApplicationException;
 import org.apache.thrift.TException;
 import org.apache.thrift.transport.TTransportException;

 import com.google.common.base.Joiner;
 
 public class TableOperationsImpl extends TableOperationsHelper {
@@ -775,20 +776,29 @@ public class TableOperationsImpl extends TableOperationsHelper {
   @Override
   public void compact(String tableName, Text start, Text end, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloSecurityException,
       TableNotFoundException, AccumuloException {
    compact(tableName, new CompactionConfig().setStartRow(start).setEndRow(end).setIterators(iterators).setFlush(flush).setWait(wait));
  }

  @Override
  public void compact(String tableName, CompactionConfig config) throws AccumuloSecurityException, TableNotFoundException, AccumuloException {
     checkArgument(tableName != null, "tableName is null");
     ByteBuffer EMPTY = ByteBuffer.allocate(0);
 
     String tableId = Tables.getTableId(context.getInstance(), tableName);
 
    if (flush)
    Text start = config.getStartRow();
    Text end = config.getEndRow();

    if (config.getFlush())
       _flush(tableId, start, end, true);
 
    List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(UTF_8)), start == null ? EMPTY : TextUtil.getByteBuffer(start), end == null ? EMPTY
        : TextUtil.getByteBuffer(end), ByteBuffer.wrap(IteratorUtil.encodeIteratorSettings(iterators)));
    List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(UTF_8)), start == null ? EMPTY : TextUtil.getByteBuffer(start),
        end == null ? EMPTY : TextUtil.getByteBuffer(end), ByteBuffer.wrap(IteratorUtil.encodeIteratorSettings(config.getIterators())),
        ByteBuffer.wrap(CompactionStrategyConfigUtil.encode(config.getCompactionStrategy())));
 
     Map<String,String> opts = new HashMap<String,String>();
     try {
      doFateOperation(FateOperation.TABLE_COMPACT, args, opts, wait);
      doFateOperation(FateOperation.TABLE_COMPACT, args, opts, config.getWait());
     } catch (TableExistsException e) {
       // should not happen
       throw new AssertionError(e);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
index 59afc8b0b..f8d2ccdd0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
@@ -36,6 +36,7 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.NamespaceNotFoundException;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.FindMax;
 import org.apache.accumulo.core.client.admin.TimeType;
@@ -399,6 +400,18 @@ class MockTableOperations extends TableOperationsHelper {
       TableNotFoundException, AccumuloException {
     if (!exists(tableName))
       throw new TableNotFoundException(tableName, tableName, "");

    if (iterators != null && iterators.size() > 0)
      throw new UnsupportedOperationException();
  }

  @Override
  public void compact(String tableName, CompactionConfig config) throws AccumuloSecurityException, TableNotFoundException, AccumuloException {
    if (!exists(tableName))
      throw new TableNotFoundException(tableName, tableName, "");

    if (config.getIterators().size() > 0 || config.getCompactionStrategy() != null)
      throw new UnsupportedOperationException("Mock does not support iterators or compaction strategies for compactions");
   }
 
   @Override
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
index 02838edc8..f7a7395ea 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
@@ -28,6 +28,8 @@ import java.util.Set;
 import java.util.SortedSet;
 import java.util.TreeMap;
 
import org.apache.accumulo.core.client.admin.CompactionConfig;

 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.IteratorSetting;
@@ -115,6 +117,9 @@ public class TableOperationsHelperTest {
     public void compact(String tableName, Text start, Text end, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloSecurityException,
         TableNotFoundException, AccumuloException {}
 
    @Override
    public void compact(String tableName, CompactionConfig config) throws AccumuloSecurityException, TableNotFoundException, AccumuloException {}

     @Override
     public void delete(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {}
 
diff --git a/proxy/src/main/cpp/AccumuloProxy.cpp b/proxy/src/main/cpp/AccumuloProxy.cpp
index 26677701e..b220dcb16 100644
-- a/proxy/src/main/cpp/AccumuloProxy.cpp
++ b/proxy/src/main/cpp/AccumuloProxy.cpp
@@ -56,17 +56,17 @@ uint32_t AccumuloProxy_login_args::read(::apache::thrift::protocol::TProtocol* i
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->loginProperties.clear();
            uint32_t _size125;
            ::apache::thrift::protocol::TType _ktype126;
            ::apache::thrift::protocol::TType _vtype127;
            xfer += iprot->readMapBegin(_ktype126, _vtype127, _size125);
            uint32_t _i129;
            for (_i129 = 0; _i129 < _size125; ++_i129)
            uint32_t _size133;
            ::apache::thrift::protocol::TType _ktype134;
            ::apache::thrift::protocol::TType _vtype135;
            xfer += iprot->readMapBegin(_ktype134, _vtype135, _size133);
            uint32_t _i137;
            for (_i137 = 0; _i137 < _size133; ++_i137)
             {
              std::string _key130;
              xfer += iprot->readString(_key130);
              std::string& _val131 = this->loginProperties[_key130];
              xfer += iprot->readString(_val131);
              std::string _key138;
              xfer += iprot->readString(_key138);
              std::string& _val139 = this->loginProperties[_key138];
              xfer += iprot->readString(_val139);
             }
             xfer += iprot->readMapEnd();
           }
@@ -98,11 +98,11 @@ uint32_t AccumuloProxy_login_args::write(::apache::thrift::protocol::TProtocol*
   xfer += oprot->writeFieldBegin("loginProperties", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->loginProperties.size()));
    std::map<std::string, std::string> ::const_iterator _iter132;
    for (_iter132 = this->loginProperties.begin(); _iter132 != this->loginProperties.end(); ++_iter132)
    std::map<std::string, std::string> ::const_iterator _iter140;
    for (_iter140 = this->loginProperties.begin(); _iter140 != this->loginProperties.end(); ++_iter140)
     {
      xfer += oprot->writeString(_iter132->first);
      xfer += oprot->writeString(_iter132->second);
      xfer += oprot->writeString(_iter140->first);
      xfer += oprot->writeString(_iter140->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -124,11 +124,11 @@ uint32_t AccumuloProxy_login_pargs::write(::apache::thrift::protocol::TProtocol*
   xfer += oprot->writeFieldBegin("loginProperties", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->loginProperties)).size()));
    std::map<std::string, std::string> ::const_iterator _iter133;
    for (_iter133 = (*(this->loginProperties)).begin(); _iter133 != (*(this->loginProperties)).end(); ++_iter133)
    std::map<std::string, std::string> ::const_iterator _iter141;
    for (_iter141 = (*(this->loginProperties)).begin(); _iter141 != (*(this->loginProperties)).end(); ++_iter141)
     {
      xfer += oprot->writeString(_iter133->first);
      xfer += oprot->writeString(_iter133->second);
      xfer += oprot->writeString(_iter141->first);
      xfer += oprot->writeString(_iter141->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -549,15 +549,15 @@ uint32_t AccumuloProxy_addSplits_args::read(::apache::thrift::protocol::TProtoco
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->splits.clear();
            uint32_t _size134;
            ::apache::thrift::protocol::TType _etype137;
            xfer += iprot->readSetBegin(_etype137, _size134);
            uint32_t _i138;
            for (_i138 = 0; _i138 < _size134; ++_i138)
            uint32_t _size142;
            ::apache::thrift::protocol::TType _etype145;
            xfer += iprot->readSetBegin(_etype145, _size142);
            uint32_t _i146;
            for (_i146 = 0; _i146 < _size142; ++_i146)
             {
              std::string _elem139;
              xfer += iprot->readBinary(_elem139);
              this->splits.insert(_elem139);
              std::string _elem147;
              xfer += iprot->readBinary(_elem147);
              this->splits.insert(_elem147);
             }
             xfer += iprot->readSetEnd();
           }
@@ -593,10 +593,10 @@ uint32_t AccumuloProxy_addSplits_args::write(::apache::thrift::protocol::TProtoc
   xfer += oprot->writeFieldBegin("splits", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->splits.size()));
    std::set<std::string> ::const_iterator _iter140;
    for (_iter140 = this->splits.begin(); _iter140 != this->splits.end(); ++_iter140)
    std::set<std::string> ::const_iterator _iter148;
    for (_iter148 = this->splits.begin(); _iter148 != this->splits.end(); ++_iter148)
     {
      xfer += oprot->writeBinary((*_iter140));
      xfer += oprot->writeBinary((*_iter148));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -622,10 +622,10 @@ uint32_t AccumuloProxy_addSplits_pargs::write(::apache::thrift::protocol::TProto
   xfer += oprot->writeFieldBegin("splits", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->splits)).size()));
    std::set<std::string> ::const_iterator _iter141;
    for (_iter141 = (*(this->splits)).begin(); _iter141 != (*(this->splits)).end(); ++_iter141)
    std::set<std::string> ::const_iterator _iter149;
    for (_iter149 = (*(this->splits)).begin(); _iter149 != (*(this->splits)).end(); ++_iter149)
     {
      xfer += oprot->writeBinary((*_iter141));
      xfer += oprot->writeBinary((*_iter149));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -820,17 +820,17 @@ uint32_t AccumuloProxy_attachIterator_args::read(::apache::thrift::protocol::TPr
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->scopes.clear();
            uint32_t _size142;
            ::apache::thrift::protocol::TType _etype145;
            xfer += iprot->readSetBegin(_etype145, _size142);
            uint32_t _i146;
            for (_i146 = 0; _i146 < _size142; ++_i146)
            uint32_t _size150;
            ::apache::thrift::protocol::TType _etype153;
            xfer += iprot->readSetBegin(_etype153, _size150);
            uint32_t _i154;
            for (_i154 = 0; _i154 < _size150; ++_i154)
             {
              IteratorScope::type _elem147;
              int32_t ecast148;
              xfer += iprot->readI32(ecast148);
              _elem147 = (IteratorScope::type)ecast148;
              this->scopes.insert(_elem147);
              IteratorScope::type _elem155;
              int32_t ecast156;
              xfer += iprot->readI32(ecast156);
              _elem155 = (IteratorScope::type)ecast156;
              this->scopes.insert(_elem155);
             }
             xfer += iprot->readSetEnd();
           }
@@ -870,10 +870,10 @@ uint32_t AccumuloProxy_attachIterator_args::write(::apache::thrift::protocol::TP
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>(this->scopes.size()));
    std::set<IteratorScope::type> ::const_iterator _iter149;
    for (_iter149 = this->scopes.begin(); _iter149 != this->scopes.end(); ++_iter149)
    std::set<IteratorScope::type> ::const_iterator _iter157;
    for (_iter157 = this->scopes.begin(); _iter157 != this->scopes.end(); ++_iter157)
     {
      xfer += oprot->writeI32((int32_t)(*_iter149));
      xfer += oprot->writeI32((int32_t)(*_iter157));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -903,10 +903,10 @@ uint32_t AccumuloProxy_attachIterator_pargs::write(::apache::thrift::protocol::T
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>((*(this->scopes)).size()));
    std::set<IteratorScope::type> ::const_iterator _iter150;
    for (_iter150 = (*(this->scopes)).begin(); _iter150 != (*(this->scopes)).end(); ++_iter150)
    std::set<IteratorScope::type> ::const_iterator _iter158;
    for (_iter158 = (*(this->scopes)).begin(); _iter158 != (*(this->scopes)).end(); ++_iter158)
     {
      xfer += oprot->writeI32((int32_t)(*_iter150));
      xfer += oprot->writeI32((int32_t)(*_iter158));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -1101,17 +1101,17 @@ uint32_t AccumuloProxy_checkIteratorConflicts_args::read(::apache::thrift::proto
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->scopes.clear();
            uint32_t _size151;
            ::apache::thrift::protocol::TType _etype154;
            xfer += iprot->readSetBegin(_etype154, _size151);
            uint32_t _i155;
            for (_i155 = 0; _i155 < _size151; ++_i155)
            uint32_t _size159;
            ::apache::thrift::protocol::TType _etype162;
            xfer += iprot->readSetBegin(_etype162, _size159);
            uint32_t _i163;
            for (_i163 = 0; _i163 < _size159; ++_i163)
             {
              IteratorScope::type _elem156;
              int32_t ecast157;
              xfer += iprot->readI32(ecast157);
              _elem156 = (IteratorScope::type)ecast157;
              this->scopes.insert(_elem156);
              IteratorScope::type _elem164;
              int32_t ecast165;
              xfer += iprot->readI32(ecast165);
              _elem164 = (IteratorScope::type)ecast165;
              this->scopes.insert(_elem164);
             }
             xfer += iprot->readSetEnd();
           }
@@ -1151,10 +1151,10 @@ uint32_t AccumuloProxy_checkIteratorConflicts_args::write(::apache::thrift::prot
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>(this->scopes.size()));
    std::set<IteratorScope::type> ::const_iterator _iter158;
    for (_iter158 = this->scopes.begin(); _iter158 != this->scopes.end(); ++_iter158)
    std::set<IteratorScope::type> ::const_iterator _iter166;
    for (_iter166 = this->scopes.begin(); _iter166 != this->scopes.end(); ++_iter166)
     {
      xfer += oprot->writeI32((int32_t)(*_iter158));
      xfer += oprot->writeI32((int32_t)(*_iter166));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -1184,10 +1184,10 @@ uint32_t AccumuloProxy_checkIteratorConflicts_pargs::write(::apache::thrift::pro
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>((*(this->scopes)).size()));
    std::set<IteratorScope::type> ::const_iterator _iter159;
    for (_iter159 = (*(this->scopes)).begin(); _iter159 != (*(this->scopes)).end(); ++_iter159)
    std::set<IteratorScope::type> ::const_iterator _iter167;
    for (_iter167 = (*(this->scopes)).begin(); _iter167 != (*(this->scopes)).end(); ++_iter167)
     {
      xfer += oprot->writeI32((int32_t)(*_iter159));
      xfer += oprot->writeI32((int32_t)(*_iter167));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -1568,17 +1568,17 @@ uint32_t AccumuloProxy_cloneTable_args::read(::apache::thrift::protocol::TProtoc
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->propertiesToSet.clear();
            uint32_t _size160;
            ::apache::thrift::protocol::TType _ktype161;
            ::apache::thrift::protocol::TType _vtype162;
            xfer += iprot->readMapBegin(_ktype161, _vtype162, _size160);
            uint32_t _i164;
            for (_i164 = 0; _i164 < _size160; ++_i164)
            uint32_t _size168;
            ::apache::thrift::protocol::TType _ktype169;
            ::apache::thrift::protocol::TType _vtype170;
            xfer += iprot->readMapBegin(_ktype169, _vtype170, _size168);
            uint32_t _i172;
            for (_i172 = 0; _i172 < _size168; ++_i172)
             {
              std::string _key165;
              xfer += iprot->readString(_key165);
              std::string& _val166 = this->propertiesToSet[_key165];
              xfer += iprot->readString(_val166);
              std::string _key173;
              xfer += iprot->readString(_key173);
              std::string& _val174 = this->propertiesToSet[_key173];
              xfer += iprot->readString(_val174);
             }
             xfer += iprot->readMapEnd();
           }
@@ -1591,15 +1591,15 @@ uint32_t AccumuloProxy_cloneTable_args::read(::apache::thrift::protocol::TProtoc
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->propertiesToExclude.clear();
            uint32_t _size167;
            ::apache::thrift::protocol::TType _etype170;
            xfer += iprot->readSetBegin(_etype170, _size167);
            uint32_t _i171;
            for (_i171 = 0; _i171 < _size167; ++_i171)
            uint32_t _size175;
            ::apache::thrift::protocol::TType _etype178;
            xfer += iprot->readSetBegin(_etype178, _size175);
            uint32_t _i179;
            for (_i179 = 0; _i179 < _size175; ++_i179)
             {
              std::string _elem172;
              xfer += iprot->readString(_elem172);
              this->propertiesToExclude.insert(_elem172);
              std::string _elem180;
              xfer += iprot->readString(_elem180);
              this->propertiesToExclude.insert(_elem180);
             }
             xfer += iprot->readSetEnd();
           }
@@ -1643,11 +1643,11 @@ uint32_t AccumuloProxy_cloneTable_args::write(::apache::thrift::protocol::TProto
   xfer += oprot->writeFieldBegin("propertiesToSet", ::apache::thrift::protocol::T_MAP, 5);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->propertiesToSet.size()));
    std::map<std::string, std::string> ::const_iterator _iter173;
    for (_iter173 = this->propertiesToSet.begin(); _iter173 != this->propertiesToSet.end(); ++_iter173)
    std::map<std::string, std::string> ::const_iterator _iter181;
    for (_iter181 = this->propertiesToSet.begin(); _iter181 != this->propertiesToSet.end(); ++_iter181)
     {
      xfer += oprot->writeString(_iter173->first);
      xfer += oprot->writeString(_iter173->second);
      xfer += oprot->writeString(_iter181->first);
      xfer += oprot->writeString(_iter181->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -1656,10 +1656,10 @@ uint32_t AccumuloProxy_cloneTable_args::write(::apache::thrift::protocol::TProto
   xfer += oprot->writeFieldBegin("propertiesToExclude", ::apache::thrift::protocol::T_SET, 6);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->propertiesToExclude.size()));
    std::set<std::string> ::const_iterator _iter174;
    for (_iter174 = this->propertiesToExclude.begin(); _iter174 != this->propertiesToExclude.end(); ++_iter174)
    std::set<std::string> ::const_iterator _iter182;
    for (_iter182 = this->propertiesToExclude.begin(); _iter182 != this->propertiesToExclude.end(); ++_iter182)
     {
      xfer += oprot->writeString((*_iter174));
      xfer += oprot->writeString((*_iter182));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -1693,11 +1693,11 @@ uint32_t AccumuloProxy_cloneTable_pargs::write(::apache::thrift::protocol::TProt
   xfer += oprot->writeFieldBegin("propertiesToSet", ::apache::thrift::protocol::T_MAP, 5);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->propertiesToSet)).size()));
    std::map<std::string, std::string> ::const_iterator _iter175;
    for (_iter175 = (*(this->propertiesToSet)).begin(); _iter175 != (*(this->propertiesToSet)).end(); ++_iter175)
    std::map<std::string, std::string> ::const_iterator _iter183;
    for (_iter183 = (*(this->propertiesToSet)).begin(); _iter183 != (*(this->propertiesToSet)).end(); ++_iter183)
     {
      xfer += oprot->writeString(_iter175->first);
      xfer += oprot->writeString(_iter175->second);
      xfer += oprot->writeString(_iter183->first);
      xfer += oprot->writeString(_iter183->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -1706,10 +1706,10 @@ uint32_t AccumuloProxy_cloneTable_pargs::write(::apache::thrift::protocol::TProt
   xfer += oprot->writeFieldBegin("propertiesToExclude", ::apache::thrift::protocol::T_SET, 6);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->propertiesToExclude)).size()));
    std::set<std::string> ::const_iterator _iter176;
    for (_iter176 = (*(this->propertiesToExclude)).begin(); _iter176 != (*(this->propertiesToExclude)).end(); ++_iter176)
    std::set<std::string> ::const_iterator _iter184;
    for (_iter184 = (*(this->propertiesToExclude)).begin(); _iter184 != (*(this->propertiesToExclude)).end(); ++_iter184)
     {
      xfer += oprot->writeString((*_iter176));
      xfer += oprot->writeString((*_iter184));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -1932,14 +1932,14 @@ uint32_t AccumuloProxy_compactTable_args::read(::apache::thrift::protocol::TProt
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->iterators.clear();
            uint32_t _size177;
            ::apache::thrift::protocol::TType _etype180;
            xfer += iprot->readListBegin(_etype180, _size177);
            this->iterators.resize(_size177);
            uint32_t _i181;
            for (_i181 = 0; _i181 < _size177; ++_i181)
            uint32_t _size185;
            ::apache::thrift::protocol::TType _etype188;
            xfer += iprot->readListBegin(_etype188, _size185);
            this->iterators.resize(_size185);
            uint32_t _i189;
            for (_i189 = 0; _i189 < _size185; ++_i189)
             {
              xfer += this->iterators[_i181].read(iprot);
              xfer += this->iterators[_i189].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -1964,6 +1964,14 @@ uint32_t AccumuloProxy_compactTable_args::read(::apache::thrift::protocol::TProt
           xfer += iprot->skip(ftype);
         }
         break;
      case 8:
        if (ftype == ::apache::thrift::protocol::T_STRUCT) {
          xfer += this->compactionStrategy.read(iprot);
          this->__isset.compactionStrategy = true;
        } else {
          xfer += iprot->skip(ftype);
        }
        break;
       default:
         xfer += iprot->skip(ftype);
         break;
@@ -1999,10 +2007,10 @@ uint32_t AccumuloProxy_compactTable_args::write(::apache::thrift::protocol::TPro
   xfer += oprot->writeFieldBegin("iterators", ::apache::thrift::protocol::T_LIST, 5);
   {
     xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->iterators.size()));
    std::vector<IteratorSetting> ::const_iterator _iter182;
    for (_iter182 = this->iterators.begin(); _iter182 != this->iterators.end(); ++_iter182)
    std::vector<IteratorSetting> ::const_iterator _iter190;
    for (_iter190 = this->iterators.begin(); _iter190 != this->iterators.end(); ++_iter190)
     {
      xfer += (*_iter182).write(oprot);
      xfer += (*_iter190).write(oprot);
     }
     xfer += oprot->writeListEnd();
   }
@@ -2016,6 +2024,10 @@ uint32_t AccumuloProxy_compactTable_args::write(::apache::thrift::protocol::TPro
   xfer += oprot->writeBool(this->wait);
   xfer += oprot->writeFieldEnd();
 
  xfer += oprot->writeFieldBegin("compactionStrategy", ::apache::thrift::protocol::T_STRUCT, 8);
  xfer += this->compactionStrategy.write(oprot);
  xfer += oprot->writeFieldEnd();

   xfer += oprot->writeFieldStop();
   xfer += oprot->writeStructEnd();
   return xfer;
@@ -2044,10 +2056,10 @@ uint32_t AccumuloProxy_compactTable_pargs::write(::apache::thrift::protocol::TPr
   xfer += oprot->writeFieldBegin("iterators", ::apache::thrift::protocol::T_LIST, 5);
   {
     xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>((*(this->iterators)).size()));
    std::vector<IteratorSetting> ::const_iterator _iter183;
    for (_iter183 = (*(this->iterators)).begin(); _iter183 != (*(this->iterators)).end(); ++_iter183)
    std::vector<IteratorSetting> ::const_iterator _iter191;
    for (_iter191 = (*(this->iterators)).begin(); _iter191 != (*(this->iterators)).end(); ++_iter191)
     {
      xfer += (*_iter183).write(oprot);
      xfer += (*_iter191).write(oprot);
     }
     xfer += oprot->writeListEnd();
   }
@@ -2061,6 +2073,10 @@ uint32_t AccumuloProxy_compactTable_pargs::write(::apache::thrift::protocol::TPr
   xfer += oprot->writeBool((*(this->wait)));
   xfer += oprot->writeFieldEnd();
 
  xfer += oprot->writeFieldBegin("compactionStrategy", ::apache::thrift::protocol::T_STRUCT, 8);
  xfer += (*(this->compactionStrategy)).write(oprot);
  xfer += oprot->writeFieldEnd();

   xfer += oprot->writeFieldStop();
   xfer += oprot->writeStructEnd();
   return xfer;
@@ -2466,9 +2482,9 @@ uint32_t AccumuloProxy_createTable_args::read(::apache::thrift::protocol::TProto
         break;
       case 4:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast184;
          xfer += iprot->readI32(ecast184);
          this->type = (TimeType::type)ecast184;
          int32_t ecast192;
          xfer += iprot->readI32(ecast192);
          this->type = (TimeType::type)ecast192;
           this->__isset.type = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -3672,15 +3688,15 @@ uint32_t AccumuloProxy_getDiskUsage_args::read(::apache::thrift::protocol::TProt
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->tables.clear();
            uint32_t _size185;
            ::apache::thrift::protocol::TType _etype188;
            xfer += iprot->readSetBegin(_etype188, _size185);
            uint32_t _i189;
            for (_i189 = 0; _i189 < _size185; ++_i189)
            uint32_t _size193;
            ::apache::thrift::protocol::TType _etype196;
            xfer += iprot->readSetBegin(_etype196, _size193);
            uint32_t _i197;
            for (_i197 = 0; _i197 < _size193; ++_i197)
             {
              std::string _elem190;
              xfer += iprot->readString(_elem190);
              this->tables.insert(_elem190);
              std::string _elem198;
              xfer += iprot->readString(_elem198);
              this->tables.insert(_elem198);
             }
             xfer += iprot->readSetEnd();
           }
@@ -3712,10 +3728,10 @@ uint32_t AccumuloProxy_getDiskUsage_args::write(::apache::thrift::protocol::TPro
   xfer += oprot->writeFieldBegin("tables", ::apache::thrift::protocol::T_SET, 2);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->tables.size()));
    std::set<std::string> ::const_iterator _iter191;
    for (_iter191 = this->tables.begin(); _iter191 != this->tables.end(); ++_iter191)
    std::set<std::string> ::const_iterator _iter199;
    for (_iter199 = this->tables.begin(); _iter199 != this->tables.end(); ++_iter199)
     {
      xfer += oprot->writeString((*_iter191));
      xfer += oprot->writeString((*_iter199));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -3737,10 +3753,10 @@ uint32_t AccumuloProxy_getDiskUsage_pargs::write(::apache::thrift::protocol::TPr
   xfer += oprot->writeFieldBegin("tables", ::apache::thrift::protocol::T_SET, 2);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->tables)).size()));
    std::set<std::string> ::const_iterator _iter192;
    for (_iter192 = (*(this->tables)).begin(); _iter192 != (*(this->tables)).end(); ++_iter192)
    std::set<std::string> ::const_iterator _iter200;
    for (_iter200 = (*(this->tables)).begin(); _iter200 != (*(this->tables)).end(); ++_iter200)
     {
      xfer += oprot->writeString((*_iter192));
      xfer += oprot->writeString((*_iter200));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -3775,14 +3791,14 @@ uint32_t AccumuloProxy_getDiskUsage_result::read(::apache::thrift::protocol::TPr
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size193;
            ::apache::thrift::protocol::TType _etype196;
            xfer += iprot->readListBegin(_etype196, _size193);
            this->success.resize(_size193);
            uint32_t _i197;
            for (_i197 = 0; _i197 < _size193; ++_i197)
            uint32_t _size201;
            ::apache::thrift::protocol::TType _etype204;
            xfer += iprot->readListBegin(_etype204, _size201);
            this->success.resize(_size201);
            uint32_t _i205;
            for (_i205 = 0; _i205 < _size201; ++_i205)
             {
              xfer += this->success[_i197].read(iprot);
              xfer += this->success[_i205].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -3837,10 +3853,10 @@ uint32_t AccumuloProxy_getDiskUsage_result::write(::apache::thrift::protocol::TP
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->success.size()));
      std::vector<DiskUsage> ::const_iterator _iter198;
      for (_iter198 = this->success.begin(); _iter198 != this->success.end(); ++_iter198)
      std::vector<DiskUsage> ::const_iterator _iter206;
      for (_iter206 = this->success.begin(); _iter206 != this->success.end(); ++_iter206)
       {
        xfer += (*_iter198).write(oprot);
        xfer += (*_iter206).write(oprot);
       }
       xfer += oprot->writeListEnd();
     }
@@ -3887,14 +3903,14 @@ uint32_t AccumuloProxy_getDiskUsage_presult::read(::apache::thrift::protocol::TP
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size199;
            ::apache::thrift::protocol::TType _etype202;
            xfer += iprot->readListBegin(_etype202, _size199);
            (*(this->success)).resize(_size199);
            uint32_t _i203;
            for (_i203 = 0; _i203 < _size199; ++_i203)
            uint32_t _size207;
            ::apache::thrift::protocol::TType _etype210;
            xfer += iprot->readListBegin(_etype210, _size207);
            (*(this->success)).resize(_size207);
            uint32_t _i211;
            for (_i211 = 0; _i211 < _size207; ++_i211)
             {
              xfer += (*(this->success))[_i203].read(iprot);
              xfer += (*(this->success))[_i211].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -4045,27 +4061,27 @@ uint32_t AccumuloProxy_getLocalityGroups_result::read(::apache::thrift::protocol
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size204;
            ::apache::thrift::protocol::TType _ktype205;
            ::apache::thrift::protocol::TType _vtype206;
            xfer += iprot->readMapBegin(_ktype205, _vtype206, _size204);
            uint32_t _i208;
            for (_i208 = 0; _i208 < _size204; ++_i208)
            uint32_t _size212;
            ::apache::thrift::protocol::TType _ktype213;
            ::apache::thrift::protocol::TType _vtype214;
            xfer += iprot->readMapBegin(_ktype213, _vtype214, _size212);
            uint32_t _i216;
            for (_i216 = 0; _i216 < _size212; ++_i216)
             {
              std::string _key209;
              xfer += iprot->readString(_key209);
              std::set<std::string> & _val210 = this->success[_key209];
              std::string _key217;
              xfer += iprot->readString(_key217);
              std::set<std::string> & _val218 = this->success[_key217];
               {
                _val210.clear();
                uint32_t _size211;
                ::apache::thrift::protocol::TType _etype214;
                xfer += iprot->readSetBegin(_etype214, _size211);
                uint32_t _i215;
                for (_i215 = 0; _i215 < _size211; ++_i215)
                _val218.clear();
                uint32_t _size219;
                ::apache::thrift::protocol::TType _etype222;
                xfer += iprot->readSetBegin(_etype222, _size219);
                uint32_t _i223;
                for (_i223 = 0; _i223 < _size219; ++_i223)
                 {
                  std::string _elem216;
                  xfer += iprot->readString(_elem216);
                  _val210.insert(_elem216);
                  std::string _elem224;
                  xfer += iprot->readString(_elem224);
                  _val218.insert(_elem224);
                 }
                 xfer += iprot->readSetEnd();
               }
@@ -4123,16 +4139,16 @@ uint32_t AccumuloProxy_getLocalityGroups_result::write(::apache::thrift::protoco
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_SET, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::set<std::string> > ::const_iterator _iter217;
      for (_iter217 = this->success.begin(); _iter217 != this->success.end(); ++_iter217)
      std::map<std::string, std::set<std::string> > ::const_iterator _iter225;
      for (_iter225 = this->success.begin(); _iter225 != this->success.end(); ++_iter225)
       {
        xfer += oprot->writeString(_iter217->first);
        xfer += oprot->writeString(_iter225->first);
         {
          xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter217->second.size()));
          std::set<std::string> ::const_iterator _iter218;
          for (_iter218 = _iter217->second.begin(); _iter218 != _iter217->second.end(); ++_iter218)
          xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter225->second.size()));
          std::set<std::string> ::const_iterator _iter226;
          for (_iter226 = _iter225->second.begin(); _iter226 != _iter225->second.end(); ++_iter226)
           {
            xfer += oprot->writeString((*_iter218));
            xfer += oprot->writeString((*_iter226));
           }
           xfer += oprot->writeSetEnd();
         }
@@ -4182,27 +4198,27 @@ uint32_t AccumuloProxy_getLocalityGroups_presult::read(::apache::thrift::protoco
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size219;
            ::apache::thrift::protocol::TType _ktype220;
            ::apache::thrift::protocol::TType _vtype221;
            xfer += iprot->readMapBegin(_ktype220, _vtype221, _size219);
            uint32_t _i223;
            for (_i223 = 0; _i223 < _size219; ++_i223)
            uint32_t _size227;
            ::apache::thrift::protocol::TType _ktype228;
            ::apache::thrift::protocol::TType _vtype229;
            xfer += iprot->readMapBegin(_ktype228, _vtype229, _size227);
            uint32_t _i231;
            for (_i231 = 0; _i231 < _size227; ++_i231)
             {
              std::string _key224;
              xfer += iprot->readString(_key224);
              std::set<std::string> & _val225 = (*(this->success))[_key224];
              std::string _key232;
              xfer += iprot->readString(_key232);
              std::set<std::string> & _val233 = (*(this->success))[_key232];
               {
                _val225.clear();
                uint32_t _size226;
                ::apache::thrift::protocol::TType _etype229;
                xfer += iprot->readSetBegin(_etype229, _size226);
                uint32_t _i230;
                for (_i230 = 0; _i230 < _size226; ++_i230)
                _val233.clear();
                uint32_t _size234;
                ::apache::thrift::protocol::TType _etype237;
                xfer += iprot->readSetBegin(_etype237, _size234);
                uint32_t _i238;
                for (_i238 = 0; _i238 < _size234; ++_i238)
                 {
                  std::string _elem231;
                  xfer += iprot->readString(_elem231);
                  _val225.insert(_elem231);
                  std::string _elem239;
                  xfer += iprot->readString(_elem239);
                  _val233.insert(_elem239);
                 }
                 xfer += iprot->readSetEnd();
               }
@@ -4296,9 +4312,9 @@ uint32_t AccumuloProxy_getIteratorSetting_args::read(::apache::thrift::protocol:
         break;
       case 4:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast232;
          xfer += iprot->readI32(ecast232);
          this->scope = (IteratorScope::type)ecast232;
          int32_t ecast240;
          xfer += iprot->readI32(ecast240);
          this->scope = (IteratorScope::type)ecast240;
           this->__isset.scope = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -4562,15 +4578,15 @@ uint32_t AccumuloProxy_getMaxRow_args::read(::apache::thrift::protocol::TProtoco
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->auths.clear();
            uint32_t _size233;
            ::apache::thrift::protocol::TType _etype236;
            xfer += iprot->readSetBegin(_etype236, _size233);
            uint32_t _i237;
            for (_i237 = 0; _i237 < _size233; ++_i237)
            uint32_t _size241;
            ::apache::thrift::protocol::TType _etype244;
            xfer += iprot->readSetBegin(_etype244, _size241);
            uint32_t _i245;
            for (_i245 = 0; _i245 < _size241; ++_i245)
             {
              std::string _elem238;
              xfer += iprot->readBinary(_elem238);
              this->auths.insert(_elem238);
              std::string _elem246;
              xfer += iprot->readBinary(_elem246);
              this->auths.insert(_elem246);
             }
             xfer += iprot->readSetEnd();
           }
@@ -4638,10 +4654,10 @@ uint32_t AccumuloProxy_getMaxRow_args::write(::apache::thrift::protocol::TProtoc
   xfer += oprot->writeFieldBegin("auths", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->auths.size()));
    std::set<std::string> ::const_iterator _iter239;
    for (_iter239 = this->auths.begin(); _iter239 != this->auths.end(); ++_iter239)
    std::set<std::string> ::const_iterator _iter247;
    for (_iter247 = this->auths.begin(); _iter247 != this->auths.end(); ++_iter247)
     {
      xfer += oprot->writeBinary((*_iter239));
      xfer += oprot->writeBinary((*_iter247));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -4683,10 +4699,10 @@ uint32_t AccumuloProxy_getMaxRow_pargs::write(::apache::thrift::protocol::TProto
   xfer += oprot->writeFieldBegin("auths", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->auths)).size()));
    std::set<std::string> ::const_iterator _iter240;
    for (_iter240 = (*(this->auths)).begin(); _iter240 != (*(this->auths)).end(); ++_iter240)
    std::set<std::string> ::const_iterator _iter248;
    for (_iter248 = (*(this->auths)).begin(); _iter248 != (*(this->auths)).end(); ++_iter248)
     {
      xfer += oprot->writeBinary((*_iter240));
      xfer += oprot->writeBinary((*_iter248));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -4975,17 +4991,17 @@ uint32_t AccumuloProxy_getTableProperties_result::read(::apache::thrift::protoco
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size241;
            ::apache::thrift::protocol::TType _ktype242;
            ::apache::thrift::protocol::TType _vtype243;
            xfer += iprot->readMapBegin(_ktype242, _vtype243, _size241);
            uint32_t _i245;
            for (_i245 = 0; _i245 < _size241; ++_i245)
            uint32_t _size249;
            ::apache::thrift::protocol::TType _ktype250;
            ::apache::thrift::protocol::TType _vtype251;
            xfer += iprot->readMapBegin(_ktype250, _vtype251, _size249);
            uint32_t _i253;
            for (_i253 = 0; _i253 < _size249; ++_i253)
             {
              std::string _key246;
              xfer += iprot->readString(_key246);
              std::string& _val247 = this->success[_key246];
              xfer += iprot->readString(_val247);
              std::string _key254;
              xfer += iprot->readString(_key254);
              std::string& _val255 = this->success[_key254];
              xfer += iprot->readString(_val255);
             }
             xfer += iprot->readMapEnd();
           }
@@ -5040,11 +5056,11 @@ uint32_t AccumuloProxy_getTableProperties_result::write(::apache::thrift::protoc
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::string> ::const_iterator _iter248;
      for (_iter248 = this->success.begin(); _iter248 != this->success.end(); ++_iter248)
      std::map<std::string, std::string> ::const_iterator _iter256;
      for (_iter256 = this->success.begin(); _iter256 != this->success.end(); ++_iter256)
       {
        xfer += oprot->writeString(_iter248->first);
        xfer += oprot->writeString(_iter248->second);
        xfer += oprot->writeString(_iter256->first);
        xfer += oprot->writeString(_iter256->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -5091,17 +5107,17 @@ uint32_t AccumuloProxy_getTableProperties_presult::read(::apache::thrift::protoc
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size249;
            ::apache::thrift::protocol::TType _ktype250;
            ::apache::thrift::protocol::TType _vtype251;
            xfer += iprot->readMapBegin(_ktype250, _vtype251, _size249);
            uint32_t _i253;
            for (_i253 = 0; _i253 < _size249; ++_i253)
            uint32_t _size257;
            ::apache::thrift::protocol::TType _ktype258;
            ::apache::thrift::protocol::TType _vtype259;
            xfer += iprot->readMapBegin(_ktype258, _vtype259, _size257);
            uint32_t _i261;
            for (_i261 = 0; _i261 < _size257; ++_i261)
             {
              std::string _key254;
              xfer += iprot->readString(_key254);
              std::string& _val255 = (*(this->success))[_key254];
              xfer += iprot->readString(_val255);
              std::string _key262;
              xfer += iprot->readString(_key262);
              std::string& _val263 = (*(this->success))[_key262];
              xfer += iprot->readString(_val263);
             }
             xfer += iprot->readMapEnd();
           }
@@ -5768,14 +5784,14 @@ uint32_t AccumuloProxy_listSplits_result::read(::apache::thrift::protocol::TProt
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size256;
            ::apache::thrift::protocol::TType _etype259;
            xfer += iprot->readListBegin(_etype259, _size256);
            this->success.resize(_size256);
            uint32_t _i260;
            for (_i260 = 0; _i260 < _size256; ++_i260)
            uint32_t _size264;
            ::apache::thrift::protocol::TType _etype267;
            xfer += iprot->readListBegin(_etype267, _size264);
            this->success.resize(_size264);
            uint32_t _i268;
            for (_i268 = 0; _i268 < _size264; ++_i268)
             {
              xfer += iprot->readBinary(this->success[_i260]);
              xfer += iprot->readBinary(this->success[_i268]);
             }
             xfer += iprot->readListEnd();
           }
@@ -5830,10 +5846,10 @@ uint32_t AccumuloProxy_listSplits_result::write(::apache::thrift::protocol::TPro
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::vector<std::string> ::const_iterator _iter261;
      for (_iter261 = this->success.begin(); _iter261 != this->success.end(); ++_iter261)
      std::vector<std::string> ::const_iterator _iter269;
      for (_iter269 = this->success.begin(); _iter269 != this->success.end(); ++_iter269)
       {
        xfer += oprot->writeBinary((*_iter261));
        xfer += oprot->writeBinary((*_iter269));
       }
       xfer += oprot->writeListEnd();
     }
@@ -5880,14 +5896,14 @@ uint32_t AccumuloProxy_listSplits_presult::read(::apache::thrift::protocol::TPro
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size262;
            ::apache::thrift::protocol::TType _etype265;
            xfer += iprot->readListBegin(_etype265, _size262);
            (*(this->success)).resize(_size262);
            uint32_t _i266;
            for (_i266 = 0; _i266 < _size262; ++_i266)
            uint32_t _size270;
            ::apache::thrift::protocol::TType _etype273;
            xfer += iprot->readListBegin(_etype273, _size270);
            (*(this->success)).resize(_size270);
            uint32_t _i274;
            for (_i274 = 0; _i274 < _size270; ++_i274)
             {
              xfer += iprot->readBinary((*(this->success))[_i266]);
              xfer += iprot->readBinary((*(this->success))[_i274]);
             }
             xfer += iprot->readListEnd();
           }
@@ -6022,15 +6038,15 @@ uint32_t AccumuloProxy_listTables_result::read(::apache::thrift::protocol::TProt
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->success.clear();
            uint32_t _size267;
            ::apache::thrift::protocol::TType _etype270;
            xfer += iprot->readSetBegin(_etype270, _size267);
            uint32_t _i271;
            for (_i271 = 0; _i271 < _size267; ++_i271)
            uint32_t _size275;
            ::apache::thrift::protocol::TType _etype278;
            xfer += iprot->readSetBegin(_etype278, _size275);
            uint32_t _i279;
            for (_i279 = 0; _i279 < _size275; ++_i279)
             {
              std::string _elem272;
              xfer += iprot->readString(_elem272);
              this->success.insert(_elem272);
              std::string _elem280;
              xfer += iprot->readString(_elem280);
              this->success.insert(_elem280);
             }
             xfer += iprot->readSetEnd();
           }
@@ -6061,10 +6077,10 @@ uint32_t AccumuloProxy_listTables_result::write(::apache::thrift::protocol::TPro
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_SET, 0);
     {
       xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::set<std::string> ::const_iterator _iter273;
      for (_iter273 = this->success.begin(); _iter273 != this->success.end(); ++_iter273)
      std::set<std::string> ::const_iterator _iter281;
      for (_iter281 = this->success.begin(); _iter281 != this->success.end(); ++_iter281)
       {
        xfer += oprot->writeString((*_iter273));
        xfer += oprot->writeString((*_iter281));
       }
       xfer += oprot->writeSetEnd();
     }
@@ -6099,15 +6115,15 @@ uint32_t AccumuloProxy_listTables_presult::read(::apache::thrift::protocol::TPro
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             (*(this->success)).clear();
            uint32_t _size274;
            ::apache::thrift::protocol::TType _etype277;
            xfer += iprot->readSetBegin(_etype277, _size274);
            uint32_t _i278;
            for (_i278 = 0; _i278 < _size274; ++_i278)
            uint32_t _size282;
            ::apache::thrift::protocol::TType _etype285;
            xfer += iprot->readSetBegin(_etype285, _size282);
            uint32_t _i286;
            for (_i286 = 0; _i286 < _size282; ++_i286)
             {
              std::string _elem279;
              xfer += iprot->readString(_elem279);
              (*(this->success)).insert(_elem279);
              std::string _elem287;
              xfer += iprot->readString(_elem287);
              (*(this->success)).insert(_elem287);
             }
             xfer += iprot->readSetEnd();
           }
@@ -6234,29 +6250,29 @@ uint32_t AccumuloProxy_listIterators_result::read(::apache::thrift::protocol::TP
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size280;
            ::apache::thrift::protocol::TType _ktype281;
            ::apache::thrift::protocol::TType _vtype282;
            xfer += iprot->readMapBegin(_ktype281, _vtype282, _size280);
            uint32_t _i284;
            for (_i284 = 0; _i284 < _size280; ++_i284)
            uint32_t _size288;
            ::apache::thrift::protocol::TType _ktype289;
            ::apache::thrift::protocol::TType _vtype290;
            xfer += iprot->readMapBegin(_ktype289, _vtype290, _size288);
            uint32_t _i292;
            for (_i292 = 0; _i292 < _size288; ++_i292)
             {
              std::string _key285;
              xfer += iprot->readString(_key285);
              std::set<IteratorScope::type> & _val286 = this->success[_key285];
              std::string _key293;
              xfer += iprot->readString(_key293);
              std::set<IteratorScope::type> & _val294 = this->success[_key293];
               {
                _val286.clear();
                uint32_t _size287;
                ::apache::thrift::protocol::TType _etype290;
                xfer += iprot->readSetBegin(_etype290, _size287);
                uint32_t _i291;
                for (_i291 = 0; _i291 < _size287; ++_i291)
                _val294.clear();
                uint32_t _size295;
                ::apache::thrift::protocol::TType _etype298;
                xfer += iprot->readSetBegin(_etype298, _size295);
                uint32_t _i299;
                for (_i299 = 0; _i299 < _size295; ++_i299)
                 {
                  IteratorScope::type _elem292;
                  int32_t ecast293;
                  xfer += iprot->readI32(ecast293);
                  _elem292 = (IteratorScope::type)ecast293;
                  _val286.insert(_elem292);
                  IteratorScope::type _elem300;
                  int32_t ecast301;
                  xfer += iprot->readI32(ecast301);
                  _elem300 = (IteratorScope::type)ecast301;
                  _val294.insert(_elem300);
                 }
                 xfer += iprot->readSetEnd();
               }
@@ -6314,16 +6330,16 @@ uint32_t AccumuloProxy_listIterators_result::write(::apache::thrift::protocol::T
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_SET, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::set<IteratorScope::type> > ::const_iterator _iter294;
      for (_iter294 = this->success.begin(); _iter294 != this->success.end(); ++_iter294)
      std::map<std::string, std::set<IteratorScope::type> > ::const_iterator _iter302;
      for (_iter302 = this->success.begin(); _iter302 != this->success.end(); ++_iter302)
       {
        xfer += oprot->writeString(_iter294->first);
        xfer += oprot->writeString(_iter302->first);
         {
          xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>(_iter294->second.size()));
          std::set<IteratorScope::type> ::const_iterator _iter295;
          for (_iter295 = _iter294->second.begin(); _iter295 != _iter294->second.end(); ++_iter295)
          xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>(_iter302->second.size()));
          std::set<IteratorScope::type> ::const_iterator _iter303;
          for (_iter303 = _iter302->second.begin(); _iter303 != _iter302->second.end(); ++_iter303)
           {
            xfer += oprot->writeI32((int32_t)(*_iter295));
            xfer += oprot->writeI32((int32_t)(*_iter303));
           }
           xfer += oprot->writeSetEnd();
         }
@@ -6373,29 +6389,29 @@ uint32_t AccumuloProxy_listIterators_presult::read(::apache::thrift::protocol::T
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size296;
            ::apache::thrift::protocol::TType _ktype297;
            ::apache::thrift::protocol::TType _vtype298;
            xfer += iprot->readMapBegin(_ktype297, _vtype298, _size296);
            uint32_t _i300;
            for (_i300 = 0; _i300 < _size296; ++_i300)
            uint32_t _size304;
            ::apache::thrift::protocol::TType _ktype305;
            ::apache::thrift::protocol::TType _vtype306;
            xfer += iprot->readMapBegin(_ktype305, _vtype306, _size304);
            uint32_t _i308;
            for (_i308 = 0; _i308 < _size304; ++_i308)
             {
              std::string _key301;
              xfer += iprot->readString(_key301);
              std::set<IteratorScope::type> & _val302 = (*(this->success))[_key301];
              std::string _key309;
              xfer += iprot->readString(_key309);
              std::set<IteratorScope::type> & _val310 = (*(this->success))[_key309];
               {
                _val302.clear();
                uint32_t _size303;
                ::apache::thrift::protocol::TType _etype306;
                xfer += iprot->readSetBegin(_etype306, _size303);
                uint32_t _i307;
                for (_i307 = 0; _i307 < _size303; ++_i307)
                _val310.clear();
                uint32_t _size311;
                ::apache::thrift::protocol::TType _etype314;
                xfer += iprot->readSetBegin(_etype314, _size311);
                uint32_t _i315;
                for (_i315 = 0; _i315 < _size311; ++_i315)
                 {
                  IteratorScope::type _elem308;
                  int32_t ecast309;
                  xfer += iprot->readI32(ecast309);
                  _elem308 = (IteratorScope::type)ecast309;
                  _val302.insert(_elem308);
                  IteratorScope::type _elem316;
                  int32_t ecast317;
                  xfer += iprot->readI32(ecast317);
                  _elem316 = (IteratorScope::type)ecast317;
                  _val310.insert(_elem316);
                 }
                 xfer += iprot->readSetEnd();
               }
@@ -6549,17 +6565,17 @@ uint32_t AccumuloProxy_listConstraints_result::read(::apache::thrift::protocol::
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size310;
            ::apache::thrift::protocol::TType _ktype311;
            ::apache::thrift::protocol::TType _vtype312;
            xfer += iprot->readMapBegin(_ktype311, _vtype312, _size310);
            uint32_t _i314;
            for (_i314 = 0; _i314 < _size310; ++_i314)
            uint32_t _size318;
            ::apache::thrift::protocol::TType _ktype319;
            ::apache::thrift::protocol::TType _vtype320;
            xfer += iprot->readMapBegin(_ktype319, _vtype320, _size318);
            uint32_t _i322;
            for (_i322 = 0; _i322 < _size318; ++_i322)
             {
              std::string _key315;
              xfer += iprot->readString(_key315);
              int32_t& _val316 = this->success[_key315];
              xfer += iprot->readI32(_val316);
              std::string _key323;
              xfer += iprot->readString(_key323);
              int32_t& _val324 = this->success[_key323];
              xfer += iprot->readI32(_val324);
             }
             xfer += iprot->readMapEnd();
           }
@@ -6614,11 +6630,11 @@ uint32_t AccumuloProxy_listConstraints_result::write(::apache::thrift::protocol:
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_I32, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, int32_t> ::const_iterator _iter317;
      for (_iter317 = this->success.begin(); _iter317 != this->success.end(); ++_iter317)
      std::map<std::string, int32_t> ::const_iterator _iter325;
      for (_iter325 = this->success.begin(); _iter325 != this->success.end(); ++_iter325)
       {
        xfer += oprot->writeString(_iter317->first);
        xfer += oprot->writeI32(_iter317->second);
        xfer += oprot->writeString(_iter325->first);
        xfer += oprot->writeI32(_iter325->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -6665,17 +6681,17 @@ uint32_t AccumuloProxy_listConstraints_presult::read(::apache::thrift::protocol:
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size318;
            ::apache::thrift::protocol::TType _ktype319;
            ::apache::thrift::protocol::TType _vtype320;
            xfer += iprot->readMapBegin(_ktype319, _vtype320, _size318);
            uint32_t _i322;
            for (_i322 = 0; _i322 < _size318; ++_i322)
            uint32_t _size326;
            ::apache::thrift::protocol::TType _ktype327;
            ::apache::thrift::protocol::TType _vtype328;
            xfer += iprot->readMapBegin(_ktype327, _vtype328, _size326);
            uint32_t _i330;
            for (_i330 = 0; _i330 < _size326; ++_i330)
             {
              std::string _key323;
              xfer += iprot->readString(_key323);
              int32_t& _val324 = (*(this->success))[_key323];
              xfer += iprot->readI32(_val324);
              std::string _key331;
              xfer += iprot->readString(_key331);
              int32_t& _val332 = (*(this->success))[_key331];
              xfer += iprot->readI32(_val332);
             }
             xfer += iprot->readMapEnd();
           }
@@ -7720,17 +7736,17 @@ uint32_t AccumuloProxy_removeIterator_args::read(::apache::thrift::protocol::TPr
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->scopes.clear();
            uint32_t _size325;
            ::apache::thrift::protocol::TType _etype328;
            xfer += iprot->readSetBegin(_etype328, _size325);
            uint32_t _i329;
            for (_i329 = 0; _i329 < _size325; ++_i329)
            uint32_t _size333;
            ::apache::thrift::protocol::TType _etype336;
            xfer += iprot->readSetBegin(_etype336, _size333);
            uint32_t _i337;
            for (_i337 = 0; _i337 < _size333; ++_i337)
             {
              IteratorScope::type _elem330;
              int32_t ecast331;
              xfer += iprot->readI32(ecast331);
              _elem330 = (IteratorScope::type)ecast331;
              this->scopes.insert(_elem330);
              IteratorScope::type _elem338;
              int32_t ecast339;
              xfer += iprot->readI32(ecast339);
              _elem338 = (IteratorScope::type)ecast339;
              this->scopes.insert(_elem338);
             }
             xfer += iprot->readSetEnd();
           }
@@ -7770,10 +7786,10 @@ uint32_t AccumuloProxy_removeIterator_args::write(::apache::thrift::protocol::TP
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>(this->scopes.size()));
    std::set<IteratorScope::type> ::const_iterator _iter332;
    for (_iter332 = this->scopes.begin(); _iter332 != this->scopes.end(); ++_iter332)
    std::set<IteratorScope::type> ::const_iterator _iter340;
    for (_iter340 = this->scopes.begin(); _iter340 != this->scopes.end(); ++_iter340)
     {
      xfer += oprot->writeI32((int32_t)(*_iter332));
      xfer += oprot->writeI32((int32_t)(*_iter340));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -7803,10 +7819,10 @@ uint32_t AccumuloProxy_removeIterator_pargs::write(::apache::thrift::protocol::T
   xfer += oprot->writeFieldBegin("scopes", ::apache::thrift::protocol::T_SET, 4);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_I32, static_cast<uint32_t>((*(this->scopes)).size()));
    std::set<IteratorScope::type> ::const_iterator _iter333;
    for (_iter333 = (*(this->scopes)).begin(); _iter333 != (*(this->scopes)).end(); ++_iter333)
    std::set<IteratorScope::type> ::const_iterator _iter341;
    for (_iter341 = (*(this->scopes)).begin(); _iter341 != (*(this->scopes)).end(); ++_iter341)
     {
      xfer += oprot->writeI32((int32_t)(*_iter333));
      xfer += oprot->writeI32((int32_t)(*_iter341));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -8481,27 +8497,27 @@ uint32_t AccumuloProxy_setLocalityGroups_args::read(::apache::thrift::protocol::
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->groups.clear();
            uint32_t _size334;
            ::apache::thrift::protocol::TType _ktype335;
            ::apache::thrift::protocol::TType _vtype336;
            xfer += iprot->readMapBegin(_ktype335, _vtype336, _size334);
            uint32_t _i338;
            for (_i338 = 0; _i338 < _size334; ++_i338)
            uint32_t _size342;
            ::apache::thrift::protocol::TType _ktype343;
            ::apache::thrift::protocol::TType _vtype344;
            xfer += iprot->readMapBegin(_ktype343, _vtype344, _size342);
            uint32_t _i346;
            for (_i346 = 0; _i346 < _size342; ++_i346)
             {
              std::string _key339;
              xfer += iprot->readString(_key339);
              std::set<std::string> & _val340 = this->groups[_key339];
              std::string _key347;
              xfer += iprot->readString(_key347);
              std::set<std::string> & _val348 = this->groups[_key347];
               {
                _val340.clear();
                uint32_t _size341;
                ::apache::thrift::protocol::TType _etype344;
                xfer += iprot->readSetBegin(_etype344, _size341);
                uint32_t _i345;
                for (_i345 = 0; _i345 < _size341; ++_i345)
                _val348.clear();
                uint32_t _size349;
                ::apache::thrift::protocol::TType _etype352;
                xfer += iprot->readSetBegin(_etype352, _size349);
                uint32_t _i353;
                for (_i353 = 0; _i353 < _size349; ++_i353)
                 {
                  std::string _elem346;
                  xfer += iprot->readString(_elem346);
                  _val340.insert(_elem346);
                  std::string _elem354;
                  xfer += iprot->readString(_elem354);
                  _val348.insert(_elem354);
                 }
                 xfer += iprot->readSetEnd();
               }
@@ -8540,16 +8556,16 @@ uint32_t AccumuloProxy_setLocalityGroups_args::write(::apache::thrift::protocol:
   xfer += oprot->writeFieldBegin("groups", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_SET, static_cast<uint32_t>(this->groups.size()));
    std::map<std::string, std::set<std::string> > ::const_iterator _iter347;
    for (_iter347 = this->groups.begin(); _iter347 != this->groups.end(); ++_iter347)
    std::map<std::string, std::set<std::string> > ::const_iterator _iter355;
    for (_iter355 = this->groups.begin(); _iter355 != this->groups.end(); ++_iter355)
     {
      xfer += oprot->writeString(_iter347->first);
      xfer += oprot->writeString(_iter355->first);
       {
        xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter347->second.size()));
        std::set<std::string> ::const_iterator _iter348;
        for (_iter348 = _iter347->second.begin(); _iter348 != _iter347->second.end(); ++_iter348)
        xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter355->second.size()));
        std::set<std::string> ::const_iterator _iter356;
        for (_iter356 = _iter355->second.begin(); _iter356 != _iter355->second.end(); ++_iter356)
         {
          xfer += oprot->writeString((*_iter348));
          xfer += oprot->writeString((*_iter356));
         }
         xfer += oprot->writeSetEnd();
       }
@@ -8578,16 +8594,16 @@ uint32_t AccumuloProxy_setLocalityGroups_pargs::write(::apache::thrift::protocol
   xfer += oprot->writeFieldBegin("groups", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_SET, static_cast<uint32_t>((*(this->groups)).size()));
    std::map<std::string, std::set<std::string> > ::const_iterator _iter349;
    for (_iter349 = (*(this->groups)).begin(); _iter349 != (*(this->groups)).end(); ++_iter349)
    std::map<std::string, std::set<std::string> > ::const_iterator _iter357;
    for (_iter357 = (*(this->groups)).begin(); _iter357 != (*(this->groups)).end(); ++_iter357)
     {
      xfer += oprot->writeString(_iter349->first);
      xfer += oprot->writeString(_iter357->first);
       {
        xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter349->second.size()));
        std::set<std::string> ::const_iterator _iter350;
        for (_iter350 = _iter349->second.begin(); _iter350 != _iter349->second.end(); ++_iter350)
        xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(_iter357->second.size()));
        std::set<std::string> ::const_iterator _iter358;
        for (_iter358 = _iter357->second.begin(); _iter358 != _iter357->second.end(); ++_iter358)
         {
          xfer += oprot->writeString((*_iter350));
          xfer += oprot->writeString((*_iter358));
         }
         xfer += oprot->writeSetEnd();
       }
@@ -9125,15 +9141,15 @@ uint32_t AccumuloProxy_splitRangeByTablets_result::read(::apache::thrift::protoc
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->success.clear();
            uint32_t _size351;
            ::apache::thrift::protocol::TType _etype354;
            xfer += iprot->readSetBegin(_etype354, _size351);
            uint32_t _i355;
            for (_i355 = 0; _i355 < _size351; ++_i355)
            uint32_t _size359;
            ::apache::thrift::protocol::TType _etype362;
            xfer += iprot->readSetBegin(_etype362, _size359);
            uint32_t _i363;
            for (_i363 = 0; _i363 < _size359; ++_i363)
             {
              Range _elem356;
              xfer += _elem356.read(iprot);
              this->success.insert(_elem356);
              Range _elem364;
              xfer += _elem364.read(iprot);
              this->success.insert(_elem364);
             }
             xfer += iprot->readSetEnd();
           }
@@ -9188,10 +9204,10 @@ uint32_t AccumuloProxy_splitRangeByTablets_result::write(::apache::thrift::proto
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_SET, 0);
     {
       xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->success.size()));
      std::set<Range> ::const_iterator _iter357;
      for (_iter357 = this->success.begin(); _iter357 != this->success.end(); ++_iter357)
      std::set<Range> ::const_iterator _iter365;
      for (_iter365 = this->success.begin(); _iter365 != this->success.end(); ++_iter365)
       {
        xfer += (*_iter357).write(oprot);
        xfer += (*_iter365).write(oprot);
       }
       xfer += oprot->writeSetEnd();
     }
@@ -9238,15 +9254,15 @@ uint32_t AccumuloProxy_splitRangeByTablets_presult::read(::apache::thrift::proto
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             (*(this->success)).clear();
            uint32_t _size358;
            ::apache::thrift::protocol::TType _etype361;
            xfer += iprot->readSetBegin(_etype361, _size358);
            uint32_t _i362;
            for (_i362 = 0; _i362 < _size358; ++_i362)
            uint32_t _size366;
            ::apache::thrift::protocol::TType _etype369;
            xfer += iprot->readSetBegin(_etype369, _size366);
            uint32_t _i370;
            for (_i370 = 0; _i370 < _size366; ++_i370)
             {
              Range _elem363;
              xfer += _elem363.read(iprot);
              (*(this->success)).insert(_elem363);
              Range _elem371;
              xfer += _elem371.read(iprot);
              (*(this->success)).insert(_elem371);
             }
             xfer += iprot->readSetEnd();
           }
@@ -9559,17 +9575,17 @@ uint32_t AccumuloProxy_tableIdMap_result::read(::apache::thrift::protocol::TProt
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size364;
            ::apache::thrift::protocol::TType _ktype365;
            ::apache::thrift::protocol::TType _vtype366;
            xfer += iprot->readMapBegin(_ktype365, _vtype366, _size364);
            uint32_t _i368;
            for (_i368 = 0; _i368 < _size364; ++_i368)
            uint32_t _size372;
            ::apache::thrift::protocol::TType _ktype373;
            ::apache::thrift::protocol::TType _vtype374;
            xfer += iprot->readMapBegin(_ktype373, _vtype374, _size372);
            uint32_t _i376;
            for (_i376 = 0; _i376 < _size372; ++_i376)
             {
              std::string _key369;
              xfer += iprot->readString(_key369);
              std::string& _val370 = this->success[_key369];
              xfer += iprot->readString(_val370);
              std::string _key377;
              xfer += iprot->readString(_key377);
              std::string& _val378 = this->success[_key377];
              xfer += iprot->readString(_val378);
             }
             xfer += iprot->readMapEnd();
           }
@@ -9600,11 +9616,11 @@ uint32_t AccumuloProxy_tableIdMap_result::write(::apache::thrift::protocol::TPro
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::string> ::const_iterator _iter371;
      for (_iter371 = this->success.begin(); _iter371 != this->success.end(); ++_iter371)
      std::map<std::string, std::string> ::const_iterator _iter379;
      for (_iter379 = this->success.begin(); _iter379 != this->success.end(); ++_iter379)
       {
        xfer += oprot->writeString(_iter371->first);
        xfer += oprot->writeString(_iter371->second);
        xfer += oprot->writeString(_iter379->first);
        xfer += oprot->writeString(_iter379->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -9639,17 +9655,17 @@ uint32_t AccumuloProxy_tableIdMap_presult::read(::apache::thrift::protocol::TPro
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size372;
            ::apache::thrift::protocol::TType _ktype373;
            ::apache::thrift::protocol::TType _vtype374;
            xfer += iprot->readMapBegin(_ktype373, _vtype374, _size372);
            uint32_t _i376;
            for (_i376 = 0; _i376 < _size372; ++_i376)
            uint32_t _size380;
            ::apache::thrift::protocol::TType _ktype381;
            ::apache::thrift::protocol::TType _vtype382;
            xfer += iprot->readMapBegin(_ktype381, _vtype382, _size380);
            uint32_t _i384;
            for (_i384 = 0; _i384 < _size380; ++_i384)
             {
              std::string _key377;
              xfer += iprot->readString(_key377);
              std::string& _val378 = (*(this->success))[_key377];
              xfer += iprot->readString(_val378);
              std::string _key385;
              xfer += iprot->readString(_key385);
              std::string& _val386 = (*(this->success))[_key385];
              xfer += iprot->readString(_val386);
             }
             xfer += iprot->readMapEnd();
           }
@@ -10244,14 +10260,14 @@ uint32_t AccumuloProxy_getActiveScans_result::read(::apache::thrift::protocol::T
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size379;
            ::apache::thrift::protocol::TType _etype382;
            xfer += iprot->readListBegin(_etype382, _size379);
            this->success.resize(_size379);
            uint32_t _i383;
            for (_i383 = 0; _i383 < _size379; ++_i383)
            uint32_t _size387;
            ::apache::thrift::protocol::TType _etype390;
            xfer += iprot->readListBegin(_etype390, _size387);
            this->success.resize(_size387);
            uint32_t _i391;
            for (_i391 = 0; _i391 < _size387; ++_i391)
             {
              xfer += this->success[_i383].read(iprot);
              xfer += this->success[_i391].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -10298,10 +10314,10 @@ uint32_t AccumuloProxy_getActiveScans_result::write(::apache::thrift::protocol::
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->success.size()));
      std::vector<ActiveScan> ::const_iterator _iter384;
      for (_iter384 = this->success.begin(); _iter384 != this->success.end(); ++_iter384)
      std::vector<ActiveScan> ::const_iterator _iter392;
      for (_iter392 = this->success.begin(); _iter392 != this->success.end(); ++_iter392)
       {
        xfer += (*_iter384).write(oprot);
        xfer += (*_iter392).write(oprot);
       }
       xfer += oprot->writeListEnd();
     }
@@ -10344,14 +10360,14 @@ uint32_t AccumuloProxy_getActiveScans_presult::read(::apache::thrift::protocol::
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size385;
            ::apache::thrift::protocol::TType _etype388;
            xfer += iprot->readListBegin(_etype388, _size385);
            (*(this->success)).resize(_size385);
            uint32_t _i389;
            for (_i389 = 0; _i389 < _size385; ++_i389)
            uint32_t _size393;
            ::apache::thrift::protocol::TType _etype396;
            xfer += iprot->readListBegin(_etype396, _size393);
            (*(this->success)).resize(_size393);
            uint32_t _i397;
            for (_i397 = 0; _i397 < _size393; ++_i397)
             {
              xfer += (*(this->success))[_i389].read(iprot);
              xfer += (*(this->success))[_i397].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -10494,14 +10510,14 @@ uint32_t AccumuloProxy_getActiveCompactions_result::read(::apache::thrift::proto
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size390;
            ::apache::thrift::protocol::TType _etype393;
            xfer += iprot->readListBegin(_etype393, _size390);
            this->success.resize(_size390);
            uint32_t _i394;
            for (_i394 = 0; _i394 < _size390; ++_i394)
            uint32_t _size398;
            ::apache::thrift::protocol::TType _etype401;
            xfer += iprot->readListBegin(_etype401, _size398);
            this->success.resize(_size398);
            uint32_t _i402;
            for (_i402 = 0; _i402 < _size398; ++_i402)
             {
              xfer += this->success[_i394].read(iprot);
              xfer += this->success[_i402].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -10548,10 +10564,10 @@ uint32_t AccumuloProxy_getActiveCompactions_result::write(::apache::thrift::prot
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->success.size()));
      std::vector<ActiveCompaction> ::const_iterator _iter395;
      for (_iter395 = this->success.begin(); _iter395 != this->success.end(); ++_iter395)
      std::vector<ActiveCompaction> ::const_iterator _iter403;
      for (_iter403 = this->success.begin(); _iter403 != this->success.end(); ++_iter403)
       {
        xfer += (*_iter395).write(oprot);
        xfer += (*_iter403).write(oprot);
       }
       xfer += oprot->writeListEnd();
     }
@@ -10594,14 +10610,14 @@ uint32_t AccumuloProxy_getActiveCompactions_presult::read(::apache::thrift::prot
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size396;
            ::apache::thrift::protocol::TType _etype399;
            xfer += iprot->readListBegin(_etype399, _size396);
            (*(this->success)).resize(_size396);
            uint32_t _i400;
            for (_i400 = 0; _i400 < _size396; ++_i400)
            uint32_t _size404;
            ::apache::thrift::protocol::TType _etype407;
            xfer += iprot->readListBegin(_etype407, _size404);
            (*(this->success)).resize(_size404);
            uint32_t _i408;
            for (_i408 = 0; _i408 < _size404; ++_i408)
             {
              xfer += (*(this->success))[_i400].read(iprot);
              xfer += (*(this->success))[_i408].read(iprot);
             }
             xfer += iprot->readListEnd();
           }
@@ -10728,17 +10744,17 @@ uint32_t AccumuloProxy_getSiteConfiguration_result::read(::apache::thrift::proto
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size401;
            ::apache::thrift::protocol::TType _ktype402;
            ::apache::thrift::protocol::TType _vtype403;
            xfer += iprot->readMapBegin(_ktype402, _vtype403, _size401);
            uint32_t _i405;
            for (_i405 = 0; _i405 < _size401; ++_i405)
            uint32_t _size409;
            ::apache::thrift::protocol::TType _ktype410;
            ::apache::thrift::protocol::TType _vtype411;
            xfer += iprot->readMapBegin(_ktype410, _vtype411, _size409);
            uint32_t _i413;
            for (_i413 = 0; _i413 < _size409; ++_i413)
             {
              std::string _key406;
              xfer += iprot->readString(_key406);
              std::string& _val407 = this->success[_key406];
              xfer += iprot->readString(_val407);
              std::string _key414;
              xfer += iprot->readString(_key414);
              std::string& _val415 = this->success[_key414];
              xfer += iprot->readString(_val415);
             }
             xfer += iprot->readMapEnd();
           }
@@ -10785,11 +10801,11 @@ uint32_t AccumuloProxy_getSiteConfiguration_result::write(::apache::thrift::prot
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::string> ::const_iterator _iter408;
      for (_iter408 = this->success.begin(); _iter408 != this->success.end(); ++_iter408)
      std::map<std::string, std::string> ::const_iterator _iter416;
      for (_iter416 = this->success.begin(); _iter416 != this->success.end(); ++_iter416)
       {
        xfer += oprot->writeString(_iter408->first);
        xfer += oprot->writeString(_iter408->second);
        xfer += oprot->writeString(_iter416->first);
        xfer += oprot->writeString(_iter416->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -10832,17 +10848,17 @@ uint32_t AccumuloProxy_getSiteConfiguration_presult::read(::apache::thrift::prot
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size409;
            ::apache::thrift::protocol::TType _ktype410;
            ::apache::thrift::protocol::TType _vtype411;
            xfer += iprot->readMapBegin(_ktype410, _vtype411, _size409);
            uint32_t _i413;
            for (_i413 = 0; _i413 < _size409; ++_i413)
            uint32_t _size417;
            ::apache::thrift::protocol::TType _ktype418;
            ::apache::thrift::protocol::TType _vtype419;
            xfer += iprot->readMapBegin(_ktype418, _vtype419, _size417);
            uint32_t _i421;
            for (_i421 = 0; _i421 < _size417; ++_i421)
             {
              std::string _key414;
              xfer += iprot->readString(_key414);
              std::string& _val415 = (*(this->success))[_key414];
              xfer += iprot->readString(_val415);
              std::string _key422;
              xfer += iprot->readString(_key422);
              std::string& _val423 = (*(this->success))[_key422];
              xfer += iprot->readString(_val423);
             }
             xfer += iprot->readMapEnd();
           }
@@ -10969,17 +10985,17 @@ uint32_t AccumuloProxy_getSystemConfiguration_result::read(::apache::thrift::pro
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size416;
            ::apache::thrift::protocol::TType _ktype417;
            ::apache::thrift::protocol::TType _vtype418;
            xfer += iprot->readMapBegin(_ktype417, _vtype418, _size416);
            uint32_t _i420;
            for (_i420 = 0; _i420 < _size416; ++_i420)
            uint32_t _size424;
            ::apache::thrift::protocol::TType _ktype425;
            ::apache::thrift::protocol::TType _vtype426;
            xfer += iprot->readMapBegin(_ktype425, _vtype426, _size424);
            uint32_t _i428;
            for (_i428 = 0; _i428 < _size424; ++_i428)
             {
              std::string _key421;
              xfer += iprot->readString(_key421);
              std::string& _val422 = this->success[_key421];
              xfer += iprot->readString(_val422);
              std::string _key429;
              xfer += iprot->readString(_key429);
              std::string& _val430 = this->success[_key429];
              xfer += iprot->readString(_val430);
             }
             xfer += iprot->readMapEnd();
           }
@@ -11026,11 +11042,11 @@ uint32_t AccumuloProxy_getSystemConfiguration_result::write(::apache::thrift::pr
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, std::string> ::const_iterator _iter423;
      for (_iter423 = this->success.begin(); _iter423 != this->success.end(); ++_iter423)
      std::map<std::string, std::string> ::const_iterator _iter431;
      for (_iter431 = this->success.begin(); _iter431 != this->success.end(); ++_iter431)
       {
        xfer += oprot->writeString(_iter423->first);
        xfer += oprot->writeString(_iter423->second);
        xfer += oprot->writeString(_iter431->first);
        xfer += oprot->writeString(_iter431->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -11073,17 +11089,17 @@ uint32_t AccumuloProxy_getSystemConfiguration_presult::read(::apache::thrift::pr
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size424;
            ::apache::thrift::protocol::TType _ktype425;
            ::apache::thrift::protocol::TType _vtype426;
            xfer += iprot->readMapBegin(_ktype425, _vtype426, _size424);
            uint32_t _i428;
            for (_i428 = 0; _i428 < _size424; ++_i428)
            uint32_t _size432;
            ::apache::thrift::protocol::TType _ktype433;
            ::apache::thrift::protocol::TType _vtype434;
            xfer += iprot->readMapBegin(_ktype433, _vtype434, _size432);
            uint32_t _i436;
            for (_i436 = 0; _i436 < _size432; ++_i436)
             {
              std::string _key429;
              xfer += iprot->readString(_key429);
              std::string& _val430 = (*(this->success))[_key429];
              xfer += iprot->readString(_val430);
              std::string _key437;
              xfer += iprot->readString(_key437);
              std::string& _val438 = (*(this->success))[_key437];
              xfer += iprot->readString(_val438);
             }
             xfer += iprot->readMapEnd();
           }
@@ -11210,14 +11226,14 @@ uint32_t AccumuloProxy_getTabletServers_result::read(::apache::thrift::protocol:
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size431;
            ::apache::thrift::protocol::TType _etype434;
            xfer += iprot->readListBegin(_etype434, _size431);
            this->success.resize(_size431);
            uint32_t _i435;
            for (_i435 = 0; _i435 < _size431; ++_i435)
            uint32_t _size439;
            ::apache::thrift::protocol::TType _etype442;
            xfer += iprot->readListBegin(_etype442, _size439);
            this->success.resize(_size439);
            uint32_t _i443;
            for (_i443 = 0; _i443 < _size439; ++_i443)
             {
              xfer += iprot->readString(this->success[_i435]);
              xfer += iprot->readString(this->success[_i443]);
             }
             xfer += iprot->readListEnd();
           }
@@ -11248,10 +11264,10 @@ uint32_t AccumuloProxy_getTabletServers_result::write(::apache::thrift::protocol
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::vector<std::string> ::const_iterator _iter436;
      for (_iter436 = this->success.begin(); _iter436 != this->success.end(); ++_iter436)
      std::vector<std::string> ::const_iterator _iter444;
      for (_iter444 = this->success.begin(); _iter444 != this->success.end(); ++_iter444)
       {
        xfer += oprot->writeString((*_iter436));
        xfer += oprot->writeString((*_iter444));
       }
       xfer += oprot->writeListEnd();
     }
@@ -11286,14 +11302,14 @@ uint32_t AccumuloProxy_getTabletServers_presult::read(::apache::thrift::protocol
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size437;
            ::apache::thrift::protocol::TType _etype440;
            xfer += iprot->readListBegin(_etype440, _size437);
            (*(this->success)).resize(_size437);
            uint32_t _i441;
            for (_i441 = 0; _i441 < _size437; ++_i441)
            uint32_t _size445;
            ::apache::thrift::protocol::TType _etype448;
            xfer += iprot->readListBegin(_etype448, _size445);
            (*(this->success)).resize(_size445);
            uint32_t _i449;
            for (_i449 = 0; _i449 < _size445; ++_i449)
             {
              xfer += iprot->readString((*(this->success))[_i441]);
              xfer += iprot->readString((*(this->success))[_i449]);
             }
             xfer += iprot->readListEnd();
           }
@@ -12000,17 +12016,17 @@ uint32_t AccumuloProxy_authenticateUser_args::read(::apache::thrift::protocol::T
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->properties.clear();
            uint32_t _size442;
            ::apache::thrift::protocol::TType _ktype443;
            ::apache::thrift::protocol::TType _vtype444;
            xfer += iprot->readMapBegin(_ktype443, _vtype444, _size442);
            uint32_t _i446;
            for (_i446 = 0; _i446 < _size442; ++_i446)
            uint32_t _size450;
            ::apache::thrift::protocol::TType _ktype451;
            ::apache::thrift::protocol::TType _vtype452;
            xfer += iprot->readMapBegin(_ktype451, _vtype452, _size450);
            uint32_t _i454;
            for (_i454 = 0; _i454 < _size450; ++_i454)
             {
              std::string _key447;
              xfer += iprot->readString(_key447);
              std::string& _val448 = this->properties[_key447];
              xfer += iprot->readString(_val448);
              std::string _key455;
              xfer += iprot->readString(_key455);
              std::string& _val456 = this->properties[_key455];
              xfer += iprot->readString(_val456);
             }
             xfer += iprot->readMapEnd();
           }
@@ -12046,11 +12062,11 @@ uint32_t AccumuloProxy_authenticateUser_args::write(::apache::thrift::protocol::
   xfer += oprot->writeFieldBegin("properties", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->properties.size()));
    std::map<std::string, std::string> ::const_iterator _iter449;
    for (_iter449 = this->properties.begin(); _iter449 != this->properties.end(); ++_iter449)
    std::map<std::string, std::string> ::const_iterator _iter457;
    for (_iter457 = this->properties.begin(); _iter457 != this->properties.end(); ++_iter457)
     {
      xfer += oprot->writeString(_iter449->first);
      xfer += oprot->writeString(_iter449->second);
      xfer += oprot->writeString(_iter457->first);
      xfer += oprot->writeString(_iter457->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -12076,11 +12092,11 @@ uint32_t AccumuloProxy_authenticateUser_pargs::write(::apache::thrift::protocol:
   xfer += oprot->writeFieldBegin("properties", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->properties)).size()));
    std::map<std::string, std::string> ::const_iterator _iter450;
    for (_iter450 = (*(this->properties)).begin(); _iter450 != (*(this->properties)).end(); ++_iter450)
    std::map<std::string, std::string> ::const_iterator _iter458;
    for (_iter458 = (*(this->properties)).begin(); _iter458 != (*(this->properties)).end(); ++_iter458)
     {
      xfer += oprot->writeString(_iter450->first);
      xfer += oprot->writeString(_iter450->second);
      xfer += oprot->writeString(_iter458->first);
      xfer += oprot->writeString(_iter458->second);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -12267,15 +12283,15 @@ uint32_t AccumuloProxy_changeUserAuthorizations_args::read(::apache::thrift::pro
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->authorizations.clear();
            uint32_t _size451;
            ::apache::thrift::protocol::TType _etype454;
            xfer += iprot->readSetBegin(_etype454, _size451);
            uint32_t _i455;
            for (_i455 = 0; _i455 < _size451; ++_i455)
            uint32_t _size459;
            ::apache::thrift::protocol::TType _etype462;
            xfer += iprot->readSetBegin(_etype462, _size459);
            uint32_t _i463;
            for (_i463 = 0; _i463 < _size459; ++_i463)
             {
              std::string _elem456;
              xfer += iprot->readBinary(_elem456);
              this->authorizations.insert(_elem456);
              std::string _elem464;
              xfer += iprot->readBinary(_elem464);
              this->authorizations.insert(_elem464);
             }
             xfer += iprot->readSetEnd();
           }
@@ -12311,10 +12327,10 @@ uint32_t AccumuloProxy_changeUserAuthorizations_args::write(::apache::thrift::pr
   xfer += oprot->writeFieldBegin("authorizations", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->authorizations.size()));
    std::set<std::string> ::const_iterator _iter457;
    for (_iter457 = this->authorizations.begin(); _iter457 != this->authorizations.end(); ++_iter457)
    std::set<std::string> ::const_iterator _iter465;
    for (_iter465 = this->authorizations.begin(); _iter465 != this->authorizations.end(); ++_iter465)
     {
      xfer += oprot->writeBinary((*_iter457));
      xfer += oprot->writeBinary((*_iter465));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -12340,10 +12356,10 @@ uint32_t AccumuloProxy_changeUserAuthorizations_pargs::write(::apache::thrift::p
   xfer += oprot->writeFieldBegin("authorizations", ::apache::thrift::protocol::T_SET, 3);
   {
     xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>((*(this->authorizations)).size()));
    std::set<std::string> ::const_iterator _iter458;
    for (_iter458 = (*(this->authorizations)).begin(); _iter458 != (*(this->authorizations)).end(); ++_iter458)
    std::set<std::string> ::const_iterator _iter466;
    for (_iter466 = (*(this->authorizations)).begin(); _iter466 != (*(this->authorizations)).end(); ++_iter466)
     {
      xfer += oprot->writeBinary((*_iter458));
      xfer += oprot->writeBinary((*_iter466));
     }
     xfer += oprot->writeSetEnd();
   }
@@ -13202,14 +13218,14 @@ uint32_t AccumuloProxy_getUserAuthorizations_result::read(::apache::thrift::prot
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             this->success.clear();
            uint32_t _size459;
            ::apache::thrift::protocol::TType _etype462;
            xfer += iprot->readListBegin(_etype462, _size459);
            this->success.resize(_size459);
            uint32_t _i463;
            for (_i463 = 0; _i463 < _size459; ++_i463)
            uint32_t _size467;
            ::apache::thrift::protocol::TType _etype470;
            xfer += iprot->readListBegin(_etype470, _size467);
            this->success.resize(_size467);
            uint32_t _i471;
            for (_i471 = 0; _i471 < _size467; ++_i471)
             {
              xfer += iprot->readBinary(this->success[_i463]);
              xfer += iprot->readBinary(this->success[_i471]);
             }
             xfer += iprot->readListEnd();
           }
@@ -13256,10 +13272,10 @@ uint32_t AccumuloProxy_getUserAuthorizations_result::write(::apache::thrift::pro
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_LIST, 0);
     {
       xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::vector<std::string> ::const_iterator _iter464;
      for (_iter464 = this->success.begin(); _iter464 != this->success.end(); ++_iter464)
      std::vector<std::string> ::const_iterator _iter472;
      for (_iter472 = this->success.begin(); _iter472 != this->success.end(); ++_iter472)
       {
        xfer += oprot->writeBinary((*_iter464));
        xfer += oprot->writeBinary((*_iter472));
       }
       xfer += oprot->writeListEnd();
     }
@@ -13302,14 +13318,14 @@ uint32_t AccumuloProxy_getUserAuthorizations_presult::read(::apache::thrift::pro
         if (ftype == ::apache::thrift::protocol::T_LIST) {
           {
             (*(this->success)).clear();
            uint32_t _size465;
            ::apache::thrift::protocol::TType _etype468;
            xfer += iprot->readListBegin(_etype468, _size465);
            (*(this->success)).resize(_size465);
            uint32_t _i469;
            for (_i469 = 0; _i469 < _size465; ++_i469)
            uint32_t _size473;
            ::apache::thrift::protocol::TType _etype476;
            xfer += iprot->readListBegin(_etype476, _size473);
            (*(this->success)).resize(_size473);
            uint32_t _i477;
            for (_i477 = 0; _i477 < _size473; ++_i477)
             {
              xfer += iprot->readBinary((*(this->success))[_i469]);
              xfer += iprot->readBinary((*(this->success))[_i477]);
             }
             xfer += iprot->readListEnd();
           }
@@ -13384,9 +13400,9 @@ uint32_t AccumuloProxy_grantSystemPermission_args::read(::apache::thrift::protoc
         break;
       case 3:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast470;
          xfer += iprot->readI32(ecast470);
          this->perm = (SystemPermission::type)ecast470;
          int32_t ecast478;
          xfer += iprot->readI32(ecast478);
          this->perm = (SystemPermission::type)ecast478;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -13608,9 +13624,9 @@ uint32_t AccumuloProxy_grantTablePermission_args::read(::apache::thrift::protoco
         break;
       case 4:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast471;
          xfer += iprot->readI32(ecast471);
          this->perm = (TablePermission::type)ecast471;
          int32_t ecast479;
          xfer += iprot->readI32(ecast479);
          this->perm = (TablePermission::type)ecast479;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -13852,9 +13868,9 @@ uint32_t AccumuloProxy_hasSystemPermission_args::read(::apache::thrift::protocol
         break;
       case 3:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast472;
          xfer += iprot->readI32(ecast472);
          this->perm = (SystemPermission::type)ecast472;
          int32_t ecast480;
          xfer += iprot->readI32(ecast480);
          this->perm = (SystemPermission::type)ecast480;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -14096,9 +14112,9 @@ uint32_t AccumuloProxy_hasTablePermission_args::read(::apache::thrift::protocol:
         break;
       case 4:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast473;
          xfer += iprot->readI32(ecast473);
          this->perm = (TablePermission::type)ecast473;
          int32_t ecast481;
          xfer += iprot->readI32(ecast481);
          this->perm = (TablePermission::type)ecast481;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -14412,15 +14428,15 @@ uint32_t AccumuloProxy_listLocalUsers_result::read(::apache::thrift::protocol::T
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             this->success.clear();
            uint32_t _size474;
            ::apache::thrift::protocol::TType _etype477;
            xfer += iprot->readSetBegin(_etype477, _size474);
            uint32_t _i478;
            for (_i478 = 0; _i478 < _size474; ++_i478)
            uint32_t _size482;
            ::apache::thrift::protocol::TType _etype485;
            xfer += iprot->readSetBegin(_etype485, _size482);
            uint32_t _i486;
            for (_i486 = 0; _i486 < _size482; ++_i486)
             {
              std::string _elem479;
              xfer += iprot->readString(_elem479);
              this->success.insert(_elem479);
              std::string _elem487;
              xfer += iprot->readString(_elem487);
              this->success.insert(_elem487);
             }
             xfer += iprot->readSetEnd();
           }
@@ -14475,10 +14491,10 @@ uint32_t AccumuloProxy_listLocalUsers_result::write(::apache::thrift::protocol::
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_SET, 0);
     {
       xfer += oprot->writeSetBegin(::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->success.size()));
      std::set<std::string> ::const_iterator _iter480;
      for (_iter480 = this->success.begin(); _iter480 != this->success.end(); ++_iter480)
      std::set<std::string> ::const_iterator _iter488;
      for (_iter488 = this->success.begin(); _iter488 != this->success.end(); ++_iter488)
       {
        xfer += oprot->writeString((*_iter480));
        xfer += oprot->writeString((*_iter488));
       }
       xfer += oprot->writeSetEnd();
     }
@@ -14525,15 +14541,15 @@ uint32_t AccumuloProxy_listLocalUsers_presult::read(::apache::thrift::protocol::
         if (ftype == ::apache::thrift::protocol::T_SET) {
           {
             (*(this->success)).clear();
            uint32_t _size481;
            ::apache::thrift::protocol::TType _etype484;
            xfer += iprot->readSetBegin(_etype484, _size481);
            uint32_t _i485;
            for (_i485 = 0; _i485 < _size481; ++_i485)
            uint32_t _size489;
            ::apache::thrift::protocol::TType _etype492;
            xfer += iprot->readSetBegin(_etype492, _size489);
            uint32_t _i493;
            for (_i493 = 0; _i493 < _size489; ++_i493)
             {
              std::string _elem486;
              xfer += iprot->readString(_elem486);
              (*(this->success)).insert(_elem486);
              std::string _elem494;
              xfer += iprot->readString(_elem494);
              (*(this->success)).insert(_elem494);
             }
             xfer += iprot->readSetEnd();
           }
@@ -14616,9 +14632,9 @@ uint32_t AccumuloProxy_revokeSystemPermission_args::read(::apache::thrift::proto
         break;
       case 3:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast487;
          xfer += iprot->readI32(ecast487);
          this->perm = (SystemPermission::type)ecast487;
          int32_t ecast495;
          xfer += iprot->readI32(ecast495);
          this->perm = (SystemPermission::type)ecast495;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -14840,9 +14856,9 @@ uint32_t AccumuloProxy_revokeTablePermission_args::read(::apache::thrift::protoc
         break;
       case 4:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast488;
          xfer += iprot->readI32(ecast488);
          this->perm = (TablePermission::type)ecast488;
          int32_t ecast496;
          xfer += iprot->readI32(ecast496);
          this->perm = (TablePermission::type)ecast496;
           this->__isset.perm = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -16398,26 +16414,26 @@ uint32_t AccumuloProxy_updateAndFlush_args::read(::apache::thrift::protocol::TPr
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->cells.clear();
            uint32_t _size489;
            ::apache::thrift::protocol::TType _ktype490;
            ::apache::thrift::protocol::TType _vtype491;
            xfer += iprot->readMapBegin(_ktype490, _vtype491, _size489);
            uint32_t _i493;
            for (_i493 = 0; _i493 < _size489; ++_i493)
            uint32_t _size497;
            ::apache::thrift::protocol::TType _ktype498;
            ::apache::thrift::protocol::TType _vtype499;
            xfer += iprot->readMapBegin(_ktype498, _vtype499, _size497);
            uint32_t _i501;
            for (_i501 = 0; _i501 < _size497; ++_i501)
             {
              std::string _key494;
              xfer += iprot->readBinary(_key494);
              std::vector<ColumnUpdate> & _val495 = this->cells[_key494];
              std::string _key502;
              xfer += iprot->readBinary(_key502);
              std::vector<ColumnUpdate> & _val503 = this->cells[_key502];
               {
                _val495.clear();
                uint32_t _size496;
                ::apache::thrift::protocol::TType _etype499;
                xfer += iprot->readListBegin(_etype499, _size496);
                _val495.resize(_size496);
                uint32_t _i500;
                for (_i500 = 0; _i500 < _size496; ++_i500)
                _val503.clear();
                uint32_t _size504;
                ::apache::thrift::protocol::TType _etype507;
                xfer += iprot->readListBegin(_etype507, _size504);
                _val503.resize(_size504);
                uint32_t _i508;
                for (_i508 = 0; _i508 < _size504; ++_i508)
                 {
                  xfer += _val495[_i500].read(iprot);
                  xfer += _val503[_i508].read(iprot);
                 }
                 xfer += iprot->readListEnd();
               }
@@ -16456,16 +16472,16 @@ uint32_t AccumuloProxy_updateAndFlush_args::write(::apache::thrift::protocol::TP
   xfer += oprot->writeFieldBegin("cells", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_LIST, static_cast<uint32_t>(this->cells.size()));
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter501;
    for (_iter501 = this->cells.begin(); _iter501 != this->cells.end(); ++_iter501)
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter509;
    for (_iter509 = this->cells.begin(); _iter509 != this->cells.end(); ++_iter509)
     {
      xfer += oprot->writeBinary(_iter501->first);
      xfer += oprot->writeBinary(_iter509->first);
       {
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter501->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter502;
        for (_iter502 = _iter501->second.begin(); _iter502 != _iter501->second.end(); ++_iter502)
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter509->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter510;
        for (_iter510 = _iter509->second.begin(); _iter510 != _iter509->second.end(); ++_iter510)
         {
          xfer += (*_iter502).write(oprot);
          xfer += (*_iter510).write(oprot);
         }
         xfer += oprot->writeListEnd();
       }
@@ -16494,16 +16510,16 @@ uint32_t AccumuloProxy_updateAndFlush_pargs::write(::apache::thrift::protocol::T
   xfer += oprot->writeFieldBegin("cells", ::apache::thrift::protocol::T_MAP, 3);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_LIST, static_cast<uint32_t>((*(this->cells)).size()));
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter503;
    for (_iter503 = (*(this->cells)).begin(); _iter503 != (*(this->cells)).end(); ++_iter503)
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter511;
    for (_iter511 = (*(this->cells)).begin(); _iter511 != (*(this->cells)).end(); ++_iter511)
     {
      xfer += oprot->writeBinary(_iter503->first);
      xfer += oprot->writeBinary(_iter511->first);
       {
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter503->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter504;
        for (_iter504 = _iter503->second.begin(); _iter504 != _iter503->second.end(); ++_iter504)
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter511->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter512;
        for (_iter512 = _iter511->second.begin(); _iter512 != _iter511->second.end(); ++_iter512)
         {
          xfer += (*_iter504).write(oprot);
          xfer += (*_iter512).write(oprot);
         }
         xfer += oprot->writeListEnd();
       }
@@ -16959,26 +16975,26 @@ uint32_t AccumuloProxy_update_args::read(::apache::thrift::protocol::TProtocol*
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->cells.clear();
            uint32_t _size505;
            ::apache::thrift::protocol::TType _ktype506;
            ::apache::thrift::protocol::TType _vtype507;
            xfer += iprot->readMapBegin(_ktype506, _vtype507, _size505);
            uint32_t _i509;
            for (_i509 = 0; _i509 < _size505; ++_i509)
            uint32_t _size513;
            ::apache::thrift::protocol::TType _ktype514;
            ::apache::thrift::protocol::TType _vtype515;
            xfer += iprot->readMapBegin(_ktype514, _vtype515, _size513);
            uint32_t _i517;
            for (_i517 = 0; _i517 < _size513; ++_i517)
             {
              std::string _key510;
              xfer += iprot->readBinary(_key510);
              std::vector<ColumnUpdate> & _val511 = this->cells[_key510];
              std::string _key518;
              xfer += iprot->readBinary(_key518);
              std::vector<ColumnUpdate> & _val519 = this->cells[_key518];
               {
                _val511.clear();
                uint32_t _size512;
                ::apache::thrift::protocol::TType _etype515;
                xfer += iprot->readListBegin(_etype515, _size512);
                _val511.resize(_size512);
                uint32_t _i516;
                for (_i516 = 0; _i516 < _size512; ++_i516)
                _val519.clear();
                uint32_t _size520;
                ::apache::thrift::protocol::TType _etype523;
                xfer += iprot->readListBegin(_etype523, _size520);
                _val519.resize(_size520);
                uint32_t _i524;
                for (_i524 = 0; _i524 < _size520; ++_i524)
                 {
                  xfer += _val511[_i516].read(iprot);
                  xfer += _val519[_i524].read(iprot);
                 }
                 xfer += iprot->readListEnd();
               }
@@ -17013,16 +17029,16 @@ uint32_t AccumuloProxy_update_args::write(::apache::thrift::protocol::TProtocol*
   xfer += oprot->writeFieldBegin("cells", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_LIST, static_cast<uint32_t>(this->cells.size()));
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter517;
    for (_iter517 = this->cells.begin(); _iter517 != this->cells.end(); ++_iter517)
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter525;
    for (_iter525 = this->cells.begin(); _iter525 != this->cells.end(); ++_iter525)
     {
      xfer += oprot->writeBinary(_iter517->first);
      xfer += oprot->writeBinary(_iter525->first);
       {
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter517->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter518;
        for (_iter518 = _iter517->second.begin(); _iter518 != _iter517->second.end(); ++_iter518)
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter525->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter526;
        for (_iter526 = _iter525->second.begin(); _iter526 != _iter525->second.end(); ++_iter526)
         {
          xfer += (*_iter518).write(oprot);
          xfer += (*_iter526).write(oprot);
         }
         xfer += oprot->writeListEnd();
       }
@@ -17047,16 +17063,16 @@ uint32_t AccumuloProxy_update_pargs::write(::apache::thrift::protocol::TProtocol
   xfer += oprot->writeFieldBegin("cells", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_LIST, static_cast<uint32_t>((*(this->cells)).size()));
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter519;
    for (_iter519 = (*(this->cells)).begin(); _iter519 != (*(this->cells)).end(); ++_iter519)
    std::map<std::string, std::vector<ColumnUpdate> > ::const_iterator _iter527;
    for (_iter527 = (*(this->cells)).begin(); _iter527 != (*(this->cells)).end(); ++_iter527)
     {
      xfer += oprot->writeBinary(_iter519->first);
      xfer += oprot->writeBinary(_iter527->first);
       {
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter519->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter520;
        for (_iter520 = _iter519->second.begin(); _iter520 != _iter519->second.end(); ++_iter520)
        xfer += oprot->writeListBegin(::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(_iter527->second.size()));
        std::vector<ColumnUpdate> ::const_iterator _iter528;
        for (_iter528 = _iter527->second.begin(); _iter528 != _iter527->second.end(); ++_iter528)
         {
          xfer += (*_iter520).write(oprot);
          xfer += (*_iter528).write(oprot);
         }
         xfer += oprot->writeListEnd();
       }
@@ -17570,9 +17586,9 @@ uint32_t AccumuloProxy_updateRowConditionally_result::read(::apache::thrift::pro
     {
       case 0:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast521;
          xfer += iprot->readI32(ecast521);
          this->success = (ConditionalStatus::type)ecast521;
          int32_t ecast529;
          xfer += iprot->readI32(ecast529);
          this->success = (ConditionalStatus::type)ecast529;
           this->__isset.success = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -17664,9 +17680,9 @@ uint32_t AccumuloProxy_updateRowConditionally_presult::read(::apache::thrift::pr
     {
       case 0:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast522;
          xfer += iprot->readI32(ecast522);
          (*(this->success)) = (ConditionalStatus::type)ecast522;
          int32_t ecast530;
          xfer += iprot->readI32(ecast530);
          (*(this->success)) = (ConditionalStatus::type)ecast530;
           this->__isset.success = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -17994,17 +18010,17 @@ uint32_t AccumuloProxy_updateRowsConditionally_args::read(::apache::thrift::prot
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->updates.clear();
            uint32_t _size523;
            ::apache::thrift::protocol::TType _ktype524;
            ::apache::thrift::protocol::TType _vtype525;
            xfer += iprot->readMapBegin(_ktype524, _vtype525, _size523);
            uint32_t _i527;
            for (_i527 = 0; _i527 < _size523; ++_i527)
            uint32_t _size531;
            ::apache::thrift::protocol::TType _ktype532;
            ::apache::thrift::protocol::TType _vtype533;
            xfer += iprot->readMapBegin(_ktype532, _vtype533, _size531);
            uint32_t _i535;
            for (_i535 = 0; _i535 < _size531; ++_i535)
             {
              std::string _key528;
              xfer += iprot->readBinary(_key528);
              ConditionalUpdates& _val529 = this->updates[_key528];
              xfer += _val529.read(iprot);
              std::string _key536;
              xfer += iprot->readBinary(_key536);
              ConditionalUpdates& _val537 = this->updates[_key536];
              xfer += _val537.read(iprot);
             }
             xfer += iprot->readMapEnd();
           }
@@ -18036,11 +18052,11 @@ uint32_t AccumuloProxy_updateRowsConditionally_args::write(::apache::thrift::pro
   xfer += oprot->writeFieldBegin("updates", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>(this->updates.size()));
    std::map<std::string, ConditionalUpdates> ::const_iterator _iter530;
    for (_iter530 = this->updates.begin(); _iter530 != this->updates.end(); ++_iter530)
    std::map<std::string, ConditionalUpdates> ::const_iterator _iter538;
    for (_iter538 = this->updates.begin(); _iter538 != this->updates.end(); ++_iter538)
     {
      xfer += oprot->writeBinary(_iter530->first);
      xfer += _iter530->second.write(oprot);
      xfer += oprot->writeBinary(_iter538->first);
      xfer += _iter538->second.write(oprot);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -18062,11 +18078,11 @@ uint32_t AccumuloProxy_updateRowsConditionally_pargs::write(::apache::thrift::pr
   xfer += oprot->writeFieldBegin("updates", ::apache::thrift::protocol::T_MAP, 2);
   {
     xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRUCT, static_cast<uint32_t>((*(this->updates)).size()));
    std::map<std::string, ConditionalUpdates> ::const_iterator _iter531;
    for (_iter531 = (*(this->updates)).begin(); _iter531 != (*(this->updates)).end(); ++_iter531)
    std::map<std::string, ConditionalUpdates> ::const_iterator _iter539;
    for (_iter539 = (*(this->updates)).begin(); _iter539 != (*(this->updates)).end(); ++_iter539)
     {
      xfer += oprot->writeBinary(_iter531->first);
      xfer += _iter531->second.write(oprot);
      xfer += oprot->writeBinary(_iter539->first);
      xfer += _iter539->second.write(oprot);
     }
     xfer += oprot->writeMapEnd();
   }
@@ -18101,19 +18117,19 @@ uint32_t AccumuloProxy_updateRowsConditionally_result::read(::apache::thrift::pr
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             this->success.clear();
            uint32_t _size532;
            ::apache::thrift::protocol::TType _ktype533;
            ::apache::thrift::protocol::TType _vtype534;
            xfer += iprot->readMapBegin(_ktype533, _vtype534, _size532);
            uint32_t _i536;
            for (_i536 = 0; _i536 < _size532; ++_i536)
            uint32_t _size540;
            ::apache::thrift::protocol::TType _ktype541;
            ::apache::thrift::protocol::TType _vtype542;
            xfer += iprot->readMapBegin(_ktype541, _vtype542, _size540);
            uint32_t _i544;
            for (_i544 = 0; _i544 < _size540; ++_i544)
             {
              std::string _key537;
              xfer += iprot->readBinary(_key537);
              ConditionalStatus::type& _val538 = this->success[_key537];
              int32_t ecast539;
              xfer += iprot->readI32(ecast539);
              _val538 = (ConditionalStatus::type)ecast539;
              std::string _key545;
              xfer += iprot->readBinary(_key545);
              ConditionalStatus::type& _val546 = this->success[_key545];
              int32_t ecast547;
              xfer += iprot->readI32(ecast547);
              _val546 = (ConditionalStatus::type)ecast547;
             }
             xfer += iprot->readMapEnd();
           }
@@ -18168,11 +18184,11 @@ uint32_t AccumuloProxy_updateRowsConditionally_result::write(::apache::thrift::p
     xfer += oprot->writeFieldBegin("success", ::apache::thrift::protocol::T_MAP, 0);
     {
       xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_I32, static_cast<uint32_t>(this->success.size()));
      std::map<std::string, ConditionalStatus::type> ::const_iterator _iter540;
      for (_iter540 = this->success.begin(); _iter540 != this->success.end(); ++_iter540)
      std::map<std::string, ConditionalStatus::type> ::const_iterator _iter548;
      for (_iter548 = this->success.begin(); _iter548 != this->success.end(); ++_iter548)
       {
        xfer += oprot->writeBinary(_iter540->first);
        xfer += oprot->writeI32((int32_t)_iter540->second);
        xfer += oprot->writeBinary(_iter548->first);
        xfer += oprot->writeI32((int32_t)_iter548->second);
       }
       xfer += oprot->writeMapEnd();
     }
@@ -18219,19 +18235,19 @@ uint32_t AccumuloProxy_updateRowsConditionally_presult::read(::apache::thrift::p
         if (ftype == ::apache::thrift::protocol::T_MAP) {
           {
             (*(this->success)).clear();
            uint32_t _size541;
            ::apache::thrift::protocol::TType _ktype542;
            ::apache::thrift::protocol::TType _vtype543;
            xfer += iprot->readMapBegin(_ktype542, _vtype543, _size541);
            uint32_t _i545;
            for (_i545 = 0; _i545 < _size541; ++_i545)
            uint32_t _size549;
            ::apache::thrift::protocol::TType _ktype550;
            ::apache::thrift::protocol::TType _vtype551;
            xfer += iprot->readMapBegin(_ktype550, _vtype551, _size549);
            uint32_t _i553;
            for (_i553 = 0; _i553 < _size549; ++_i553)
             {
              std::string _key546;
              xfer += iprot->readBinary(_key546);
              ConditionalStatus::type& _val547 = (*(this->success))[_key546];
              int32_t ecast548;
              xfer += iprot->readI32(ecast548);
              _val547 = (ConditionalStatus::type)ecast548;
              std::string _key554;
              xfer += iprot->readBinary(_key554);
              ConditionalStatus::type& _val555 = (*(this->success))[_key554];
              int32_t ecast556;
              xfer += iprot->readI32(ecast556);
              _val555 = (ConditionalStatus::type)ecast556;
             }
             xfer += iprot->readMapEnd();
           }
@@ -18599,9 +18615,9 @@ uint32_t AccumuloProxy_getFollowing_args::read(::apache::thrift::protocol::TProt
         break;
       case 2:
         if (ftype == ::apache::thrift::protocol::T_I32) {
          int32_t ecast549;
          xfer += iprot->readI32(ecast549);
          this->part = (PartialKey::type)ecast549;
          int32_t ecast557;
          xfer += iprot->readI32(ecast557);
          this->part = (PartialKey::type)ecast557;
           this->__isset.part = true;
         } else {
           xfer += iprot->skip(ftype);
@@ -19201,13 +19217,13 @@ void AccumuloProxyClient::recv_cloneTable()
   return;
 }
 
void AccumuloProxyClient::compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait)
void AccumuloProxyClient::compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy)
 {
  send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait);
  send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy);
   recv_compactTable();
 }
 
void AccumuloProxyClient::send_compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait)
void AccumuloProxyClient::send_compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy)
 {
   int32_t cseqid = 0;
   oprot_->writeMessageBegin("compactTable", ::apache::thrift::protocol::T_CALL, cseqid);
@@ -19220,6 +19236,7 @@ void AccumuloProxyClient::send_compactTable(const std::string& login, const std:
   args.iterators = &iterators;
   args.flush = &flush;
   args.wait = &wait;
  args.compactionStrategy = &compactionStrategy;
   args.write(oprot_);
 
   oprot_->writeMessageEnd();
@@ -24136,7 +24153,7 @@ void AccumuloProxyProcessor::process_compactTable(int32_t seqid, ::apache::thrif
 
   AccumuloProxy_compactTable_result result;
   try {
    iface_->compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait);
    iface_->compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait, args.compactionStrategy);
   } catch (AccumuloSecurityException &ouch1) {
     result.ouch1 = ouch1;
     result.__isset.ouch1 = true;
diff --git a/proxy/src/main/cpp/AccumuloProxy.h b/proxy/src/main/cpp/AccumuloProxy.h
index e9b77694c..269884f98 100644
-- a/proxy/src/main/cpp/AccumuloProxy.h
++ b/proxy/src/main/cpp/AccumuloProxy.h
@@ -38,7 +38,7 @@ class AccumuloProxyIf {
   virtual void checkIteratorConflicts(const std::string& login, const std::string& tableName, const IteratorSetting& setting, const std::set<IteratorScope::type> & scopes) = 0;
   virtual void clearLocatorCache(const std::string& login, const std::string& tableName) = 0;
   virtual void cloneTable(const std::string& login, const std::string& tableName, const std::string& newTableName, const bool flush, const std::map<std::string, std::string> & propertiesToSet, const std::set<std::string> & propertiesToExclude) = 0;
  virtual void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait) = 0;
  virtual void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy) = 0;
   virtual void cancelCompaction(const std::string& login, const std::string& tableName) = 0;
   virtual void createTable(const std::string& login, const std::string& tableName, const bool versioningIter, const TimeType::type type) = 0;
   virtual void deleteTable(const std::string& login, const std::string& tableName) = 0;
@@ -159,7 +159,7 @@ class AccumuloProxyNull : virtual public AccumuloProxyIf {
   void cloneTable(const std::string& /* login */, const std::string& /* tableName */, const std::string& /* newTableName */, const bool /* flush */, const std::map<std::string, std::string> & /* propertiesToSet */, const std::set<std::string> & /* propertiesToExclude */) {
     return;
   }
  void compactTable(const std::string& /* login */, const std::string& /* tableName */, const std::string& /* startRow */, const std::string& /* endRow */, const std::vector<IteratorSetting> & /* iterators */, const bool /* flush */, const bool /* wait */) {
  void compactTable(const std::string& /* login */, const std::string& /* tableName */, const std::string& /* startRow */, const std::string& /* endRow */, const std::vector<IteratorSetting> & /* iterators */, const bool /* flush */, const bool /* wait */, const CompactionStrategyConfig& /* compactionStrategy */) {
     return;
   }
   void cancelCompaction(const std::string& /* login */, const std::string& /* tableName */) {
@@ -1419,7 +1419,7 @@ class AccumuloProxy_cloneTable_presult {
 };
 
 typedef struct _AccumuloProxy_compactTable_args__isset {
  _AccumuloProxy_compactTable_args__isset() : login(false), tableName(false), startRow(false), endRow(false), iterators(false), flush(false), wait(false) {}
  _AccumuloProxy_compactTable_args__isset() : login(false), tableName(false), startRow(false), endRow(false), iterators(false), flush(false), wait(false), compactionStrategy(false) {}
   bool login;
   bool tableName;
   bool startRow;
@@ -1427,6 +1427,7 @@ typedef struct _AccumuloProxy_compactTable_args__isset {
   bool iterators;
   bool flush;
   bool wait;
  bool compactionStrategy;
 } _AccumuloProxy_compactTable_args__isset;
 
 class AccumuloProxy_compactTable_args {
@@ -1444,6 +1445,7 @@ class AccumuloProxy_compactTable_args {
   std::vector<IteratorSetting>  iterators;
   bool flush;
   bool wait;
  CompactionStrategyConfig compactionStrategy;
 
   _AccumuloProxy_compactTable_args__isset __isset;
 
@@ -1475,6 +1477,10 @@ class AccumuloProxy_compactTable_args {
     wait = val;
   }
 
  void __set_compactionStrategy(const CompactionStrategyConfig& val) {
    compactionStrategy = val;
  }

   bool operator == (const AccumuloProxy_compactTable_args & rhs) const
   {
     if (!(login == rhs.login))
@@ -1491,6 +1497,8 @@ class AccumuloProxy_compactTable_args {
       return false;
     if (!(wait == rhs.wait))
       return false;
    if (!(compactionStrategy == rhs.compactionStrategy))
      return false;
     return true;
   }
   bool operator != (const AccumuloProxy_compactTable_args &rhs) const {
@@ -1518,6 +1526,7 @@ class AccumuloProxy_compactTable_pargs {
   const std::vector<IteratorSetting> * iterators;
   const bool* flush;
   const bool* wait;
  const CompactionStrategyConfig* compactionStrategy;
 
   uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;
 
@@ -11342,8 +11351,8 @@ class AccumuloProxyClient : virtual public AccumuloProxyIf {
   void cloneTable(const std::string& login, const std::string& tableName, const std::string& newTableName, const bool flush, const std::map<std::string, std::string> & propertiesToSet, const std::set<std::string> & propertiesToExclude);
   void send_cloneTable(const std::string& login, const std::string& tableName, const std::string& newTableName, const bool flush, const std::map<std::string, std::string> & propertiesToSet, const std::set<std::string> & propertiesToExclude);
   void recv_cloneTable();
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait);
  void send_compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait);
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy);
  void send_compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy);
   void recv_compactTable();
   void cancelCompaction(const std::string& login, const std::string& tableName);
   void send_cancelCompaction(const std::string& login, const std::string& tableName);
@@ -11815,13 +11824,13 @@ class AccumuloProxyMultiface : virtual public AccumuloProxyIf {
     ifaces_[i]->cloneTable(login, tableName, newTableName, flush, propertiesToSet, propertiesToExclude);
   }
 
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait) {
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy) {
     size_t sz = ifaces_.size();
     size_t i = 0;
     for (; i < (sz - 1); ++i) {
      ifaces_[i]->compactTable(login, tableName, startRow, endRow, iterators, flush, wait);
      ifaces_[i]->compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy);
     }
    ifaces_[i]->compactTable(login, tableName, startRow, endRow, iterators, flush, wait);
    ifaces_[i]->compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy);
   }
 
   void cancelCompaction(const std::string& login, const std::string& tableName) {
diff --git a/proxy/src/main/cpp/AccumuloProxy_server.skeleton.cpp b/proxy/src/main/cpp/AccumuloProxy_server.skeleton.cpp
index 2654c3779..302aec252 100644
-- a/proxy/src/main/cpp/AccumuloProxy_server.skeleton.cpp
++ b/proxy/src/main/cpp/AccumuloProxy_server.skeleton.cpp
@@ -73,7 +73,7 @@ class AccumuloProxyHandler : virtual public AccumuloProxyIf {
     printf("cloneTable\n");
   }
 
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait) {
  void compactTable(const std::string& login, const std::string& tableName, const std::string& startRow, const std::string& endRow, const std::vector<IteratorSetting> & iterators, const bool flush, const bool wait, const CompactionStrategyConfig& compactionStrategy) {
     // Your implementation goes here
     printf("compactTable\n");
   }
diff --git a/proxy/src/main/cpp/proxy_types.cpp b/proxy/src/main/cpp/proxy_types.cpp
index edb0978af..a055b485d 100644
-- a/proxy/src/main/cpp/proxy_types.cpp
++ b/proxy/src/main/cpp/proxy_types.cpp
@@ -2611,6 +2611,105 @@ void swap(WriterOptions &a, WriterOptions &b) {
   swap(a.__isset, b.__isset);
 }
 
const char* CompactionStrategyConfig::ascii_fingerprint = "F7C641917C22B35AE581CCD54910B00D";
const uint8_t CompactionStrategyConfig::binary_fingerprint[16] = {0xF7,0xC6,0x41,0x91,0x7C,0x22,0xB3,0x5A,0xE5,0x81,0xCC,0xD5,0x49,0x10,0xB0,0x0D};

uint32_t CompactionStrategyConfig::read(::apache::thrift::protocol::TProtocol* iprot) {

  uint32_t xfer = 0;
  std::string fname;
  ::apache::thrift::protocol::TType ftype;
  int16_t fid;

  xfer += iprot->readStructBegin(fname);

  using ::apache::thrift::protocol::TProtocolException;


  while (true)
  {
    xfer += iprot->readFieldBegin(fname, ftype, fid);
    if (ftype == ::apache::thrift::protocol::T_STOP) {
      break;
    }
    switch (fid)
    {
      case 1:
        if (ftype == ::apache::thrift::protocol::T_STRING) {
          xfer += iprot->readString(this->className);
          this->__isset.className = true;
        } else {
          xfer += iprot->skip(ftype);
        }
        break;
      case 2:
        if (ftype == ::apache::thrift::protocol::T_MAP) {
          {
            this->options.clear();
            uint32_t _size125;
            ::apache::thrift::protocol::TType _ktype126;
            ::apache::thrift::protocol::TType _vtype127;
            xfer += iprot->readMapBegin(_ktype126, _vtype127, _size125);
            uint32_t _i129;
            for (_i129 = 0; _i129 < _size125; ++_i129)
            {
              std::string _key130;
              xfer += iprot->readString(_key130);
              std::string& _val131 = this->options[_key130];
              xfer += iprot->readString(_val131);
            }
            xfer += iprot->readMapEnd();
          }
          this->__isset.options = true;
        } else {
          xfer += iprot->skip(ftype);
        }
        break;
      default:
        xfer += iprot->skip(ftype);
        break;
    }
    xfer += iprot->readFieldEnd();
  }

  xfer += iprot->readStructEnd();

  return xfer;
}

uint32_t CompactionStrategyConfig::write(::apache::thrift::protocol::TProtocol* oprot) const {
  uint32_t xfer = 0;
  xfer += oprot->writeStructBegin("CompactionStrategyConfig");

  xfer += oprot->writeFieldBegin("className", ::apache::thrift::protocol::T_STRING, 1);
  xfer += oprot->writeString(this->className);
  xfer += oprot->writeFieldEnd();

  xfer += oprot->writeFieldBegin("options", ::apache::thrift::protocol::T_MAP, 2);
  {
    xfer += oprot->writeMapBegin(::apache::thrift::protocol::T_STRING, ::apache::thrift::protocol::T_STRING, static_cast<uint32_t>(this->options.size()));
    std::map<std::string, std::string> ::const_iterator _iter132;
    for (_iter132 = this->options.begin(); _iter132 != this->options.end(); ++_iter132)
    {
      xfer += oprot->writeString(_iter132->first);
      xfer += oprot->writeString(_iter132->second);
    }
    xfer += oprot->writeMapEnd();
  }
  xfer += oprot->writeFieldEnd();

  xfer += oprot->writeFieldStop();
  xfer += oprot->writeStructEnd();
  return xfer;
}

void swap(CompactionStrategyConfig &a, CompactionStrategyConfig &b) {
  using ::std::swap;
  swap(a.className, b.className);
  swap(a.options, b.options);
  swap(a.__isset, b.__isset);
}

 const char* UnknownScanner::ascii_fingerprint = "EFB929595D312AC8F305D5A794CFEDA1";
 const uint8_t UnknownScanner::binary_fingerprint[16] = {0xEF,0xB9,0x29,0x59,0x5D,0x31,0x2A,0xC8,0xF3,0x05,0xD5,0xA7,0x94,0xCF,0xED,0xA1};
 
diff --git a/proxy/src/main/cpp/proxy_types.h b/proxy/src/main/cpp/proxy_types.h
index 625586c66..569de8843 100644
-- a/proxy/src/main/cpp/proxy_types.h
++ b/proxy/src/main/cpp/proxy_types.h
@@ -1555,6 +1555,57 @@ class WriterOptions {
 
 void swap(WriterOptions &a, WriterOptions &b);
 
typedef struct _CompactionStrategyConfig__isset {
  _CompactionStrategyConfig__isset() : className(false), options(false) {}
  bool className;
  bool options;
} _CompactionStrategyConfig__isset;

class CompactionStrategyConfig {
 public:

  static const char* ascii_fingerprint; // = "F7C641917C22B35AE581CCD54910B00D";
  static const uint8_t binary_fingerprint[16]; // = {0xF7,0xC6,0x41,0x91,0x7C,0x22,0xB3,0x5A,0xE5,0x81,0xCC,0xD5,0x49,0x10,0xB0,0x0D};

  CompactionStrategyConfig() : className() {
  }

  virtual ~CompactionStrategyConfig() throw() {}

  std::string className;
  std::map<std::string, std::string>  options;

  _CompactionStrategyConfig__isset __isset;

  void __set_className(const std::string& val) {
    className = val;
  }

  void __set_options(const std::map<std::string, std::string> & val) {
    options = val;
  }

  bool operator == (const CompactionStrategyConfig & rhs) const
  {
    if (!(className == rhs.className))
      return false;
    if (!(options == rhs.options))
      return false;
    return true;
  }
  bool operator != (const CompactionStrategyConfig &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const CompactionStrategyConfig & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

};

void swap(CompactionStrategyConfig &a, CompactionStrategyConfig &b);

 typedef struct _UnknownScanner__isset {
   _UnknownScanner__isset() : msg(false) {}
   bool msg;
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
index bd0782d0b..b51d43db9 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/ProxyServer.java
@@ -56,6 +56,7 @@ import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
 import org.apache.accumulo.core.client.admin.ActiveCompaction;
 import org.apache.accumulo.core.client.admin.ActiveScan;
import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
 import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
@@ -83,6 +84,7 @@ import org.apache.accumulo.proxy.thrift.AccumuloProxy;
 import org.apache.accumulo.proxy.thrift.BatchScanOptions;
 import org.apache.accumulo.proxy.thrift.ColumnUpdate;
 import org.apache.accumulo.proxy.thrift.CompactionReason;
import org.apache.accumulo.proxy.thrift.CompactionStrategyConfig;
 import org.apache.accumulo.proxy.thrift.CompactionType;
 import org.apache.accumulo.proxy.thrift.Condition;
 import org.apache.accumulo.proxy.thrift.ConditionalStatus;
@@ -331,12 +333,22 @@ public class ProxyServer implements AccumuloProxy.Iface {
   
   @Override
   public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow,
      List<org.apache.accumulo.proxy.thrift.IteratorSetting> iterators, boolean flush, boolean wait)
      List<org.apache.accumulo.proxy.thrift.IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy)
       throws org.apache.accumulo.proxy.thrift.AccumuloSecurityException, org.apache.accumulo.proxy.thrift.TableNotFoundException,
       org.apache.accumulo.proxy.thrift.AccumuloException, TException {
     try {
      getConnector(login).tableOperations().compact(tableName, ByteBufferUtil.toText(startRow), ByteBufferUtil.toText(endRow), getIteratorSettings(iterators),
          flush, wait);
      CompactionConfig compactionConfig = new CompactionConfig().setStartRow(ByteBufferUtil.toText(startRow)).setEndRow(ByteBufferUtil.toText(endRow))
          .setIterators(getIteratorSettings(iterators)).setFlush(flush).setWait(wait);
      
      if (compactionStrategy != null) {
        org.apache.accumulo.core.client.admin.CompactionStrategyConfig ccc = new org.apache.accumulo.core.client.admin.CompactionStrategyConfig(
            compactionStrategy.getClassName());
        if (compactionStrategy.options != null)
          ccc.setOptions(compactionStrategy.options);
        compactionConfig.setCompactionStrategy(ccc);
      }

      getConnector(login).tableOperations().compact(tableName, compactionConfig);
     } catch (Exception e) {
       handleExceptionTNF(e);
     }
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/thrift/AccumuloProxy.java b/proxy/src/main/java/org/apache/accumulo/proxy/thrift/AccumuloProxy.java
index 3b23175f9..65863b9d6 100644
-- a/proxy/src/main/java/org/apache/accumulo/proxy/thrift/AccumuloProxy.java
++ b/proxy/src/main/java/org/apache/accumulo/proxy/thrift/AccumuloProxy.java
@@ -66,7 +66,7 @@ import org.slf4j.LoggerFactory;
 
     public void cloneTable(ByteBuffer login, String tableName, String newTableName, boolean flush, Map<String,String> propertiesToSet, Set<String> propertiesToExclude) throws AccumuloException, AccumuloSecurityException, TableNotFoundException, TableExistsException, org.apache.thrift.TException;
 
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, org.apache.thrift.TException;
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, org.apache.thrift.TException;
 
     public void cancelCompaction(ByteBuffer login, String tableName) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, org.apache.thrift.TException;
 
@@ -224,7 +224,7 @@ import org.slf4j.LoggerFactory;
 
     public void cloneTable(ByteBuffer login, String tableName, String newTableName, boolean flush, Map<String,String> propertiesToSet, Set<String> propertiesToExclude, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
     public void cancelCompaction(ByteBuffer login, String tableName, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
@@ -603,13 +603,13 @@ import org.slf4j.LoggerFactory;
       return;
     }
 
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, org.apache.thrift.TException
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy) throws AccumuloSecurityException, TableNotFoundException, AccumuloException, org.apache.thrift.TException
     {
      send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait);
      send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy);
       recv_compactTable();
     }
 
    public void send_compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait) throws org.apache.thrift.TException
    public void send_compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy) throws org.apache.thrift.TException
     {
       compactTable_args args = new compactTable_args();
       args.setLogin(login);
@@ -619,6 +619,7 @@ import org.slf4j.LoggerFactory;
       args.setIterators(iterators);
       args.setFlush(flush);
       args.setWait(wait);
      args.setCompactionStrategy(compactionStrategy);
       sendBase("compactTable", args);
     }
 
@@ -3008,9 +3009,9 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
    public void compactTable(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
       checkReady();
      compactTable_call method_call = new compactTable_call(login, tableName, startRow, endRow, iterators, flush, wait, resultHandler, this, ___protocolFactory, ___transport);
      compactTable_call method_call = new compactTable_call(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy, resultHandler, this, ___protocolFactory, ___transport);
       this.___currentMethod = method_call;
       ___manager.call(method_call);
     }
@@ -3023,7 +3024,8 @@ import org.slf4j.LoggerFactory;
       private List<IteratorSetting> iterators;
       private boolean flush;
       private boolean wait;
      public compactTable_call(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
      private CompactionStrategyConfig compactionStrategy;
      public compactTable_call(ByteBuffer login, String tableName, ByteBuffer startRow, ByteBuffer endRow, List<IteratorSetting> iterators, boolean flush, boolean wait, CompactionStrategyConfig compactionStrategy, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
         super(client, protocolFactory, transport, resultHandler, false);
         this.login = login;
         this.tableName = tableName;
@@ -3032,6 +3034,7 @@ import org.slf4j.LoggerFactory;
         this.iterators = iterators;
         this.flush = flush;
         this.wait = wait;
        this.compactionStrategy = compactionStrategy;
       }
 
       public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
@@ -3044,6 +3047,7 @@ import org.slf4j.LoggerFactory;
         args.setIterators(iterators);
         args.setFlush(flush);
         args.setWait(wait);
        args.setCompactionStrategy(compactionStrategy);
         args.write(prot);
         prot.writeMessageEnd();
       }
@@ -5907,7 +5911,7 @@ import org.slf4j.LoggerFactory;
       public compactTable_result getResult(I iface, compactTable_args args) throws org.apache.thrift.TException {
         compactTable_result result = new compactTable_result();
         try {
          iface.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait);
          iface.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait, args.compactionStrategy);
         } catch (AccumuloSecurityException ouch1) {
           result.ouch1 = ouch1;
         } catch (TableNotFoundException ouch2) {
@@ -8356,7 +8360,7 @@ import org.slf4j.LoggerFactory;
       }
 
       public void start(I iface, compactTable_args args, org.apache.thrift.async.AsyncMethodCallback<Void> resultHandler) throws TException {
        iface.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait,resultHandler);
        iface.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait, args.compactionStrategy,resultHandler);
       }
     }
 
@@ -13091,15 +13095,15 @@ import org.slf4j.LoggerFactory;
             case 2: // LOGIN_PROPERTIES
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map154 = iprot.readMapBegin();
                  struct.loginProperties = new HashMap<String,String>(2*_map154.size);
                  for (int _i155 = 0; _i155 < _map154.size; ++_i155)
                  org.apache.thrift.protocol.TMap _map164 = iprot.readMapBegin();
                  struct.loginProperties = new HashMap<String,String>(2*_map164.size);
                  for (int _i165 = 0; _i165 < _map164.size; ++_i165)
                   {
                    String _key156;
                    String _val157;
                    _key156 = iprot.readString();
                    _val157 = iprot.readString();
                    struct.loginProperties.put(_key156, _val157);
                    String _key166;
                    String _val167;
                    _key166 = iprot.readString();
                    _val167 = iprot.readString();
                    struct.loginProperties.put(_key166, _val167);
                   }
                   iprot.readMapEnd();
                 }
@@ -13132,10 +13136,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(LOGIN_PROPERTIES_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.loginProperties.size()));
            for (Map.Entry<String, String> _iter158 : struct.loginProperties.entrySet())
            for (Map.Entry<String, String> _iter168 : struct.loginProperties.entrySet())
             {
              oprot.writeString(_iter158.getKey());
              oprot.writeString(_iter158.getValue());
              oprot.writeString(_iter168.getKey());
              oprot.writeString(_iter168.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -13172,10 +13176,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetLoginProperties()) {
           {
             oprot.writeI32(struct.loginProperties.size());
            for (Map.Entry<String, String> _iter159 : struct.loginProperties.entrySet())
            for (Map.Entry<String, String> _iter169 : struct.loginProperties.entrySet())
             {
              oprot.writeString(_iter159.getKey());
              oprot.writeString(_iter159.getValue());
              oprot.writeString(_iter169.getKey());
              oprot.writeString(_iter169.getValue());
             }
           }
         }
@@ -13191,15 +13195,15 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(1)) {
           {
            org.apache.thrift.protocol.TMap _map160 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.loginProperties = new HashMap<String,String>(2*_map160.size);
            for (int _i161 = 0; _i161 < _map160.size; ++_i161)
            org.apache.thrift.protocol.TMap _map170 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.loginProperties = new HashMap<String,String>(2*_map170.size);
            for (int _i171 = 0; _i171 < _map170.size; ++_i171)
             {
              String _key162;
              String _val163;
              _key162 = iprot.readString();
              _val163 = iprot.readString();
              struct.loginProperties.put(_key162, _val163);
              String _key172;
              String _val173;
              _key172 = iprot.readString();
              _val173 = iprot.readString();
              struct.loginProperties.put(_key172, _val173);
             }
           }
           struct.setLoginPropertiesIsSet(true);
@@ -15388,13 +15392,13 @@ import org.slf4j.LoggerFactory;
             case 3: // SPLITS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set164 = iprot.readSetBegin();
                  struct.splits = new HashSet<ByteBuffer>(2*_set164.size);
                  for (int _i165 = 0; _i165 < _set164.size; ++_i165)
                  org.apache.thrift.protocol.TSet _set174 = iprot.readSetBegin();
                  struct.splits = new HashSet<ByteBuffer>(2*_set174.size);
                  for (int _i175 = 0; _i175 < _set174.size; ++_i175)
                   {
                    ByteBuffer _elem166;
                    _elem166 = iprot.readBinary();
                    struct.splits.add(_elem166);
                    ByteBuffer _elem176;
                    _elem176 = iprot.readBinary();
                    struct.splits.add(_elem176);
                   }
                   iprot.readSetEnd();
                 }
@@ -15432,9 +15436,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SPLITS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.splits.size()));
            for (ByteBuffer _iter167 : struct.splits)
            for (ByteBuffer _iter177 : struct.splits)
             {
              oprot.writeBinary(_iter167);
              oprot.writeBinary(_iter177);
             }
             oprot.writeSetEnd();
           }
@@ -15477,9 +15481,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSplits()) {
           {
             oprot.writeI32(struct.splits.size());
            for (ByteBuffer _iter168 : struct.splits)
            for (ByteBuffer _iter178 : struct.splits)
             {
              oprot.writeBinary(_iter168);
              oprot.writeBinary(_iter178);
             }
           }
         }
@@ -15499,13 +15503,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TSet _set169 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.splits = new HashSet<ByteBuffer>(2*_set169.size);
            for (int _i170 = 0; _i170 < _set169.size; ++_i170)
            org.apache.thrift.protocol.TSet _set179 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.splits = new HashSet<ByteBuffer>(2*_set179.size);
            for (int _i180 = 0; _i180 < _set179.size; ++_i180)
             {
              ByteBuffer _elem171;
              _elem171 = iprot.readBinary();
              struct.splits.add(_elem171);
              ByteBuffer _elem181;
              _elem181 = iprot.readBinary();
              struct.splits.add(_elem181);
             }
           }
           struct.setSplitsIsSet(true);
@@ -16654,13 +16658,13 @@ import org.slf4j.LoggerFactory;
             case 4: // SCOPES
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set172 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set172.size);
                  for (int _i173 = 0; _i173 < _set172.size; ++_i173)
                  org.apache.thrift.protocol.TSet _set182 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set182.size);
                  for (int _i183 = 0; _i183 < _set182.size; ++_i183)
                   {
                    IteratorScope _elem174;
                    _elem174 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem174);
                    IteratorScope _elem184;
                    _elem184 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem184);
                   }
                   iprot.readSetEnd();
                 }
@@ -16703,9 +16707,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SCOPES_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, struct.scopes.size()));
            for (IteratorScope _iter175 : struct.scopes)
            for (IteratorScope _iter185 : struct.scopes)
             {
              oprot.writeI32(_iter175.getValue());
              oprot.writeI32(_iter185.getValue());
             }
             oprot.writeSetEnd();
           }
@@ -16754,9 +16758,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetScopes()) {
           {
             oprot.writeI32(struct.scopes.size());
            for (IteratorScope _iter176 : struct.scopes)
            for (IteratorScope _iter186 : struct.scopes)
             {
              oprot.writeI32(_iter176.getValue());
              oprot.writeI32(_iter186.getValue());
             }
           }
         }
@@ -16781,13 +16785,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TSet _set177 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set177.size);
            for (int _i178 = 0; _i178 < _set177.size; ++_i178)
            org.apache.thrift.protocol.TSet _set187 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set187.size);
            for (int _i188 = 0; _i188 < _set187.size; ++_i188)
             {
              IteratorScope _elem179;
              _elem179 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem179);
              IteratorScope _elem189;
              _elem189 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem189);
             }
           }
           struct.setScopesIsSet(true);
@@ -17936,13 +17940,13 @@ import org.slf4j.LoggerFactory;
             case 4: // SCOPES
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set180 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set180.size);
                  for (int _i181 = 0; _i181 < _set180.size; ++_i181)
                  org.apache.thrift.protocol.TSet _set190 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set190.size);
                  for (int _i191 = 0; _i191 < _set190.size; ++_i191)
                   {
                    IteratorScope _elem182;
                    _elem182 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem182);
                    IteratorScope _elem192;
                    _elem192 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem192);
                   }
                   iprot.readSetEnd();
                 }
@@ -17985,9 +17989,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SCOPES_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, struct.scopes.size()));
            for (IteratorScope _iter183 : struct.scopes)
            for (IteratorScope _iter193 : struct.scopes)
             {
              oprot.writeI32(_iter183.getValue());
              oprot.writeI32(_iter193.getValue());
             }
             oprot.writeSetEnd();
           }
@@ -18036,9 +18040,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetScopes()) {
           {
             oprot.writeI32(struct.scopes.size());
            for (IteratorScope _iter184 : struct.scopes)
            for (IteratorScope _iter194 : struct.scopes)
             {
              oprot.writeI32(_iter184.getValue());
              oprot.writeI32(_iter194.getValue());
             }
           }
         }
@@ -18063,13 +18067,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TSet _set185 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set185.size);
            for (int _i186 = 0; _i186 < _set185.size; ++_i186)
            org.apache.thrift.protocol.TSet _set195 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set195.size);
            for (int _i196 = 0; _i196 < _set195.size; ++_i196)
             {
              IteratorScope _elem187;
              _elem187 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem187);
              IteratorScope _elem197;
              _elem197 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem197);
             }
           }
           struct.setScopesIsSet(true);
@@ -20208,15 +20212,15 @@ import org.slf4j.LoggerFactory;
             case 5: // PROPERTIES_TO_SET
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map188 = iprot.readMapBegin();
                  struct.propertiesToSet = new HashMap<String,String>(2*_map188.size);
                  for (int _i189 = 0; _i189 < _map188.size; ++_i189)
                  org.apache.thrift.protocol.TMap _map198 = iprot.readMapBegin();
                  struct.propertiesToSet = new HashMap<String,String>(2*_map198.size);
                  for (int _i199 = 0; _i199 < _map198.size; ++_i199)
                   {
                    String _key190;
                    String _val191;
                    _key190 = iprot.readString();
                    _val191 = iprot.readString();
                    struct.propertiesToSet.put(_key190, _val191);
                    String _key200;
                    String _val201;
                    _key200 = iprot.readString();
                    _val201 = iprot.readString();
                    struct.propertiesToSet.put(_key200, _val201);
                   }
                   iprot.readMapEnd();
                 }
@@ -20228,13 +20232,13 @@ import org.slf4j.LoggerFactory;
             case 6: // PROPERTIES_TO_EXCLUDE
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set192 = iprot.readSetBegin();
                  struct.propertiesToExclude = new HashSet<String>(2*_set192.size);
                  for (int _i193 = 0; _i193 < _set192.size; ++_i193)
                  org.apache.thrift.protocol.TSet _set202 = iprot.readSetBegin();
                  struct.propertiesToExclude = new HashSet<String>(2*_set202.size);
                  for (int _i203 = 0; _i203 < _set202.size; ++_i203)
                   {
                    String _elem194;
                    _elem194 = iprot.readString();
                    struct.propertiesToExclude.add(_elem194);
                    String _elem204;
                    _elem204 = iprot.readString();
                    struct.propertiesToExclude.add(_elem204);
                   }
                   iprot.readSetEnd();
                 }
@@ -20280,10 +20284,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(PROPERTIES_TO_SET_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.propertiesToSet.size()));
            for (Map.Entry<String, String> _iter195 : struct.propertiesToSet.entrySet())
            for (Map.Entry<String, String> _iter205 : struct.propertiesToSet.entrySet())
             {
              oprot.writeString(_iter195.getKey());
              oprot.writeString(_iter195.getValue());
              oprot.writeString(_iter205.getKey());
              oprot.writeString(_iter205.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -20293,9 +20297,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(PROPERTIES_TO_EXCLUDE_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.propertiesToExclude.size()));
            for (String _iter196 : struct.propertiesToExclude)
            for (String _iter206 : struct.propertiesToExclude)
             {
              oprot.writeString(_iter196);
              oprot.writeString(_iter206);
             }
             oprot.writeSetEnd();
           }
@@ -20353,19 +20357,19 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetPropertiesToSet()) {
           {
             oprot.writeI32(struct.propertiesToSet.size());
            for (Map.Entry<String, String> _iter197 : struct.propertiesToSet.entrySet())
            for (Map.Entry<String, String> _iter207 : struct.propertiesToSet.entrySet())
             {
              oprot.writeString(_iter197.getKey());
              oprot.writeString(_iter197.getValue());
              oprot.writeString(_iter207.getKey());
              oprot.writeString(_iter207.getValue());
             }
           }
         }
         if (struct.isSetPropertiesToExclude()) {
           {
             oprot.writeI32(struct.propertiesToExclude.size());
            for (String _iter198 : struct.propertiesToExclude)
            for (String _iter208 : struct.propertiesToExclude)
             {
              oprot.writeString(_iter198);
              oprot.writeString(_iter208);
             }
           }
         }
@@ -20393,28 +20397,28 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(4)) {
           {
            org.apache.thrift.protocol.TMap _map199 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.propertiesToSet = new HashMap<String,String>(2*_map199.size);
            for (int _i200 = 0; _i200 < _map199.size; ++_i200)
            org.apache.thrift.protocol.TMap _map209 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.propertiesToSet = new HashMap<String,String>(2*_map209.size);
            for (int _i210 = 0; _i210 < _map209.size; ++_i210)
             {
              String _key201;
              String _val202;
              _key201 = iprot.readString();
              _val202 = iprot.readString();
              struct.propertiesToSet.put(_key201, _val202);
              String _key211;
              String _val212;
              _key211 = iprot.readString();
              _val212 = iprot.readString();
              struct.propertiesToSet.put(_key211, _val212);
             }
           }
           struct.setPropertiesToSetIsSet(true);
         }
         if (incoming.get(5)) {
           {
            org.apache.thrift.protocol.TSet _set203 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.propertiesToExclude = new HashSet<String>(2*_set203.size);
            for (int _i204 = 0; _i204 < _set203.size; ++_i204)
            org.apache.thrift.protocol.TSet _set213 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.propertiesToExclude = new HashSet<String>(2*_set213.size);
            for (int _i214 = 0; _i214 < _set213.size; ++_i214)
             {
              String _elem205;
              _elem205 = iprot.readString();
              struct.propertiesToExclude.add(_elem205);
              String _elem215;
              _elem215 = iprot.readString();
              struct.propertiesToExclude.add(_elem215);
             }
           }
           struct.setPropertiesToExcludeIsSet(true);
@@ -21096,6 +21100,7 @@ import org.slf4j.LoggerFactory;
     private static final org.apache.thrift.protocol.TField ITERATORS_FIELD_DESC = new org.apache.thrift.protocol.TField("iterators", org.apache.thrift.protocol.TType.LIST, (short)5);
     private static final org.apache.thrift.protocol.TField FLUSH_FIELD_DESC = new org.apache.thrift.protocol.TField("flush", org.apache.thrift.protocol.TType.BOOL, (short)6);
     private static final org.apache.thrift.protocol.TField WAIT_FIELD_DESC = new org.apache.thrift.protocol.TField("wait", org.apache.thrift.protocol.TType.BOOL, (short)7);
    private static final org.apache.thrift.protocol.TField COMPACTION_STRATEGY_FIELD_DESC = new org.apache.thrift.protocol.TField("compactionStrategy", org.apache.thrift.protocol.TType.STRUCT, (short)8);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -21110,6 +21115,7 @@ import org.slf4j.LoggerFactory;
     public List<IteratorSetting> iterators; // required
     public boolean flush; // required
     public boolean wait; // required
    public CompactionStrategyConfig compactionStrategy; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     @SuppressWarnings("all") public enum _Fields implements org.apache.thrift.TFieldIdEnum {
@@ -21119,7 +21125,8 @@ import org.slf4j.LoggerFactory;
       END_ROW((short)4, "endRow"),
       ITERATORS((short)5, "iterators"),
       FLUSH((short)6, "flush"),
      WAIT((short)7, "wait");
      WAIT((short)7, "wait"),
      COMPACTION_STRATEGY((short)8, "compactionStrategy");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -21148,6 +21155,8 @@ import org.slf4j.LoggerFactory;
             return FLUSH;
           case 7: // WAIT
             return WAIT;
          case 8: // COMPACTION_STRATEGY
            return COMPACTION_STRATEGY;
           default:
             return null;
         }
@@ -21209,6 +21218,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
       tmpMap.put(_Fields.WAIT, new org.apache.thrift.meta_data.FieldMetaData("wait", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
      tmpMap.put(_Fields.COMPACTION_STRATEGY, new org.apache.thrift.meta_data.FieldMetaData("compactionStrategy", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, CompactionStrategyConfig.class)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(compactTable_args.class, metaDataMap);
     }
@@ -21223,7 +21234,8 @@ import org.slf4j.LoggerFactory;
       ByteBuffer endRow,
       List<IteratorSetting> iterators,
       boolean flush,
      boolean wait)
      boolean wait,
      CompactionStrategyConfig compactionStrategy)
     {
       this();
       this.login = login;
@@ -21235,6 +21247,7 @@ import org.slf4j.LoggerFactory;
       setFlushIsSet(true);
       this.wait = wait;
       setWaitIsSet(true);
      this.compactionStrategy = compactionStrategy;
     }
 
     /**
@@ -21266,6 +21279,9 @@ import org.slf4j.LoggerFactory;
       }
       this.flush = other.flush;
       this.wait = other.wait;
      if (other.isSetCompactionStrategy()) {
        this.compactionStrategy = new CompactionStrategyConfig(other.compactionStrategy);
      }
     }
 
     public compactTable_args deepCopy() {
@@ -21283,6 +21299,7 @@ import org.slf4j.LoggerFactory;
       this.flush = false;
       setWaitIsSet(false);
       this.wait = false;
      this.compactionStrategy = null;
     }
 
     public byte[] getLogin() {
@@ -21496,6 +21513,30 @@ import org.slf4j.LoggerFactory;
       __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __WAIT_ISSET_ID, value);
     }
 
    public CompactionStrategyConfig getCompactionStrategy() {
      return this.compactionStrategy;
    }

    public compactTable_args setCompactionStrategy(CompactionStrategyConfig compactionStrategy) {
      this.compactionStrategy = compactionStrategy;
      return this;
    }

    public void unsetCompactionStrategy() {
      this.compactionStrategy = null;
    }

    /** Returns true if field compactionStrategy is set (has been assigned a value) and false otherwise */
    public boolean isSetCompactionStrategy() {
      return this.compactionStrategy != null;
    }

    public void setCompactionStrategyIsSet(boolean value) {
      if (!value) {
        this.compactionStrategy = null;
      }
    }

     public void setFieldValue(_Fields field, Object value) {
       switch (field) {
       case LOGIN:
@@ -21554,6 +21595,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case COMPACTION_STRATEGY:
        if (value == null) {
          unsetCompactionStrategy();
        } else {
          setCompactionStrategy((CompactionStrategyConfig)value);
        }
        break;

       }
     }
 
@@ -21580,6 +21629,9 @@ import org.slf4j.LoggerFactory;
       case WAIT:
         return Boolean.valueOf(isWait());
 
      case COMPACTION_STRATEGY:
        return getCompactionStrategy();

       }
       throw new IllegalStateException();
     }
@@ -21605,6 +21657,8 @@ import org.slf4j.LoggerFactory;
         return isSetFlush();
       case WAIT:
         return isSetWait();
      case COMPACTION_STRATEGY:
        return isSetCompactionStrategy();
       }
       throw new IllegalStateException();
     }
@@ -21685,6 +21739,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_compactionStrategy = true && this.isSetCompactionStrategy();
      boolean that_present_compactionStrategy = true && that.isSetCompactionStrategy();
      if (this_present_compactionStrategy || that_present_compactionStrategy) {
        if (!(this_present_compactionStrategy && that_present_compactionStrategy))
          return false;
        if (!this.compactionStrategy.equals(that.compactionStrategy))
          return false;
      }

       return true;
     }
 
@@ -21771,6 +21834,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetCompactionStrategy()).compareTo(other.isSetCompactionStrategy());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetCompactionStrategy()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.compactionStrategy, other.compactionStrategy);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       return 0;
     }
 
@@ -21838,6 +21911,14 @@ import org.slf4j.LoggerFactory;
       sb.append("wait:");
       sb.append(this.wait);
       first = false;
      if (!first) sb.append(", ");
      sb.append("compactionStrategy:");
      if (this.compactionStrategy == null) {
        sb.append("null");
      } else {
        sb.append(this.compactionStrategy);
      }
      first = false;
       sb.append(")");
       return sb.toString();
     }
@@ -21845,6 +21926,9 @@ import org.slf4j.LoggerFactory;
     public void validate() throws org.apache.thrift.TException {
       // check for required fields
       // check for sub-struct validity
      if (compactionStrategy != null) {
        compactionStrategy.validate();
      }
     }
 
     private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
@@ -21918,14 +22002,14 @@ import org.slf4j.LoggerFactory;
             case 5: // ITERATORS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list206 = iprot.readListBegin();
                  struct.iterators = new ArrayList<IteratorSetting>(_list206.size);
                  for (int _i207 = 0; _i207 < _list206.size; ++_i207)
                  org.apache.thrift.protocol.TList _list216 = iprot.readListBegin();
                  struct.iterators = new ArrayList<IteratorSetting>(_list216.size);
                  for (int _i217 = 0; _i217 < _list216.size; ++_i217)
                   {
                    IteratorSetting _elem208;
                    _elem208 = new IteratorSetting();
                    _elem208.read(iprot);
                    struct.iterators.add(_elem208);
                    IteratorSetting _elem218;
                    _elem218 = new IteratorSetting();
                    _elem218.read(iprot);
                    struct.iterators.add(_elem218);
                   }
                   iprot.readListEnd();
                 }
@@ -21950,6 +22034,15 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 8: // COMPACTION_STRATEGY
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.compactionStrategy = new CompactionStrategyConfig();
                struct.compactionStrategy.read(iprot);
                struct.setCompactionStrategyIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
             default:
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
           }
@@ -21989,9 +22082,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(ITERATORS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.iterators.size()));
            for (IteratorSetting _iter209 : struct.iterators)
            for (IteratorSetting _iter219 : struct.iterators)
             {
              _iter209.write(oprot);
              _iter219.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -22003,6 +22096,11 @@ import org.slf4j.LoggerFactory;
         oprot.writeFieldBegin(WAIT_FIELD_DESC);
         oprot.writeBool(struct.wait);
         oprot.writeFieldEnd();
        if (struct.compactionStrategy != null) {
          oprot.writeFieldBegin(COMPACTION_STRATEGY_FIELD_DESC);
          struct.compactionStrategy.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -22042,7 +22140,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetWait()) {
           optionals.set(6);
         }
        oprot.writeBitSet(optionals, 7);
        if (struct.isSetCompactionStrategy()) {
          optionals.set(7);
        }
        oprot.writeBitSet(optionals, 8);
         if (struct.isSetLogin()) {
           oprot.writeBinary(struct.login);
         }
@@ -22058,9 +22159,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetIterators()) {
           {
             oprot.writeI32(struct.iterators.size());
            for (IteratorSetting _iter210 : struct.iterators)
            for (IteratorSetting _iter220 : struct.iterators)
             {
              _iter210.write(oprot);
              _iter220.write(oprot);
             }
           }
         }
@@ -22070,12 +22171,15 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetWait()) {
           oprot.writeBool(struct.wait);
         }
        if (struct.isSetCompactionStrategy()) {
          struct.compactionStrategy.write(oprot);
        }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, compactTable_args struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(7);
        BitSet incoming = iprot.readBitSet(8);
         if (incoming.get(0)) {
           struct.login = iprot.readBinary();
           struct.setLoginIsSet(true);
@@ -22094,14 +22198,14 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(4)) {
           {
            org.apache.thrift.protocol.TList _list211 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.iterators = new ArrayList<IteratorSetting>(_list211.size);
            for (int _i212 = 0; _i212 < _list211.size; ++_i212)
            org.apache.thrift.protocol.TList _list221 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.iterators = new ArrayList<IteratorSetting>(_list221.size);
            for (int _i222 = 0; _i222 < _list221.size; ++_i222)
             {
              IteratorSetting _elem213;
              _elem213 = new IteratorSetting();
              _elem213.read(iprot);
              struct.iterators.add(_elem213);
              IteratorSetting _elem223;
              _elem223 = new IteratorSetting();
              _elem223.read(iprot);
              struct.iterators.add(_elem223);
             }
           }
           struct.setIteratorsIsSet(true);
@@ -22114,6 +22218,11 @@ import org.slf4j.LoggerFactory;
           struct.wait = iprot.readBool();
           struct.setWaitIsSet(true);
         }
        if (incoming.get(7)) {
          struct.compactionStrategy = new CompactionStrategyConfig();
          struct.compactionStrategy.read(iprot);
          struct.setCompactionStrategyIsSet(true);
        }
       }
     }
 
@@ -30087,13 +30196,13 @@ import org.slf4j.LoggerFactory;
             case 2: // TABLES
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set214 = iprot.readSetBegin();
                  struct.tables = new HashSet<String>(2*_set214.size);
                  for (int _i215 = 0; _i215 < _set214.size; ++_i215)
                  org.apache.thrift.protocol.TSet _set224 = iprot.readSetBegin();
                  struct.tables = new HashSet<String>(2*_set224.size);
                  for (int _i225 = 0; _i225 < _set224.size; ++_i225)
                   {
                    String _elem216;
                    _elem216 = iprot.readString();
                    struct.tables.add(_elem216);
                    String _elem226;
                    _elem226 = iprot.readString();
                    struct.tables.add(_elem226);
                   }
                   iprot.readSetEnd();
                 }
@@ -30126,9 +30235,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(TABLES_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.tables.size()));
            for (String _iter217 : struct.tables)
            for (String _iter227 : struct.tables)
             {
              oprot.writeString(_iter217);
              oprot.writeString(_iter227);
             }
             oprot.writeSetEnd();
           }
@@ -30165,9 +30274,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetTables()) {
           {
             oprot.writeI32(struct.tables.size());
            for (String _iter218 : struct.tables)
            for (String _iter228 : struct.tables)
             {
              oprot.writeString(_iter218);
              oprot.writeString(_iter228);
             }
           }
         }
@@ -30183,13 +30292,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(1)) {
           {
            org.apache.thrift.protocol.TSet _set219 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.tables = new HashSet<String>(2*_set219.size);
            for (int _i220 = 0; _i220 < _set219.size; ++_i220)
            org.apache.thrift.protocol.TSet _set229 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.tables = new HashSet<String>(2*_set229.size);
            for (int _i230 = 0; _i230 < _set229.size; ++_i230)
             {
              String _elem221;
              _elem221 = iprot.readString();
              struct.tables.add(_elem221);
              String _elem231;
              _elem231 = iprot.readString();
              struct.tables.add(_elem231);
             }
           }
           struct.setTablesIsSet(true);
@@ -30739,14 +30848,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list222 = iprot.readListBegin();
                  struct.success = new ArrayList<DiskUsage>(_list222.size);
                  for (int _i223 = 0; _i223 < _list222.size; ++_i223)
                  org.apache.thrift.protocol.TList _list232 = iprot.readListBegin();
                  struct.success = new ArrayList<DiskUsage>(_list232.size);
                  for (int _i233 = 0; _i233 < _list232.size; ++_i233)
                   {
                    DiskUsage _elem224;
                    _elem224 = new DiskUsage();
                    _elem224.read(iprot);
                    struct.success.add(_elem224);
                    DiskUsage _elem234;
                    _elem234 = new DiskUsage();
                    _elem234.read(iprot);
                    struct.success.add(_elem234);
                   }
                   iprot.readListEnd();
                 }
@@ -30801,9 +30910,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (DiskUsage _iter225 : struct.success)
            for (DiskUsage _iter235 : struct.success)
             {
              _iter225.write(oprot);
              _iter235.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -30858,9 +30967,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (DiskUsage _iter226 : struct.success)
            for (DiskUsage _iter236 : struct.success)
             {
              _iter226.write(oprot);
              _iter236.write(oprot);
             }
           }
         }
@@ -30881,14 +30990,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list227 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<DiskUsage>(_list227.size);
            for (int _i228 = 0; _i228 < _list227.size; ++_i228)
            org.apache.thrift.protocol.TList _list237 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<DiskUsage>(_list237.size);
            for (int _i238 = 0; _i238 < _list237.size; ++_i238)
             {
              DiskUsage _elem229;
              _elem229 = new DiskUsage();
              _elem229.read(iprot);
              struct.success.add(_elem229);
              DiskUsage _elem239;
              _elem239 = new DiskUsage();
              _elem239.read(iprot);
              struct.success.add(_elem239);
             }
           }
           struct.setSuccessIsSet(true);
@@ -31924,25 +32033,25 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map230 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Set<String>>(2*_map230.size);
                  for (int _i231 = 0; _i231 < _map230.size; ++_i231)
                  org.apache.thrift.protocol.TMap _map240 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Set<String>>(2*_map240.size);
                  for (int _i241 = 0; _i241 < _map240.size; ++_i241)
                   {
                    String _key232;
                    Set<String> _val233;
                    _key232 = iprot.readString();
                    String _key242;
                    Set<String> _val243;
                    _key242 = iprot.readString();
                     {
                      org.apache.thrift.protocol.TSet _set234 = iprot.readSetBegin();
                      _val233 = new HashSet<String>(2*_set234.size);
                      for (int _i235 = 0; _i235 < _set234.size; ++_i235)
                      org.apache.thrift.protocol.TSet _set244 = iprot.readSetBegin();
                      _val243 = new HashSet<String>(2*_set244.size);
                      for (int _i245 = 0; _i245 < _set244.size; ++_i245)
                       {
                        String _elem236;
                        _elem236 = iprot.readString();
                        _val233.add(_elem236);
                        String _elem246;
                        _elem246 = iprot.readString();
                        _val243.add(_elem246);
                       }
                       iprot.readSetEnd();
                     }
                    struct.success.put(_key232, _val233);
                    struct.success.put(_key242, _val243);
                   }
                   iprot.readMapEnd();
                 }
@@ -31997,14 +32106,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, struct.success.size()));
            for (Map.Entry<String, Set<String>> _iter237 : struct.success.entrySet())
            for (Map.Entry<String, Set<String>> _iter247 : struct.success.entrySet())
             {
              oprot.writeString(_iter237.getKey());
              oprot.writeString(_iter247.getKey());
               {
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, _iter237.getValue().size()));
                for (String _iter238 : _iter237.getValue())
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, _iter247.getValue().size()));
                for (String _iter248 : _iter247.getValue())
                 {
                  oprot.writeString(_iter238);
                  oprot.writeString(_iter248);
                 }
                 oprot.writeSetEnd();
               }
@@ -32062,14 +32171,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, Set<String>> _iter239 : struct.success.entrySet())
            for (Map.Entry<String, Set<String>> _iter249 : struct.success.entrySet())
             {
              oprot.writeString(_iter239.getKey());
              oprot.writeString(_iter249.getKey());
               {
                oprot.writeI32(_iter239.getValue().size());
                for (String _iter240 : _iter239.getValue())
                oprot.writeI32(_iter249.getValue().size());
                for (String _iter250 : _iter249.getValue())
                 {
                  oprot.writeString(_iter240);
                  oprot.writeString(_iter250);
                 }
               }
             }
@@ -32092,24 +32201,24 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map241 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.success = new HashMap<String,Set<String>>(2*_map241.size);
            for (int _i242 = 0; _i242 < _map241.size; ++_i242)
            org.apache.thrift.protocol.TMap _map251 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.success = new HashMap<String,Set<String>>(2*_map251.size);
            for (int _i252 = 0; _i252 < _map251.size; ++_i252)
             {
              String _key243;
              Set<String> _val244;
              _key243 = iprot.readString();
              String _key253;
              Set<String> _val254;
              _key253 = iprot.readString();
               {
                org.apache.thrift.protocol.TSet _set245 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val244 = new HashSet<String>(2*_set245.size);
                for (int _i246 = 0; _i246 < _set245.size; ++_i246)
                org.apache.thrift.protocol.TSet _set255 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val254 = new HashSet<String>(2*_set255.size);
                for (int _i256 = 0; _i256 < _set255.size; ++_i256)
                 {
                  String _elem247;
                  _elem247 = iprot.readString();
                  _val244.add(_elem247);
                  String _elem257;
                  _elem257 = iprot.readString();
                  _val254.add(_elem257);
                 }
               }
              struct.success.put(_key243, _val244);
              struct.success.put(_key253, _val254);
             }
           }
           struct.setSuccessIsSet(true);
@@ -34293,13 +34402,13 @@ import org.slf4j.LoggerFactory;
             case 3: // AUTHS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set248 = iprot.readSetBegin();
                  struct.auths = new HashSet<ByteBuffer>(2*_set248.size);
                  for (int _i249 = 0; _i249 < _set248.size; ++_i249)
                  org.apache.thrift.protocol.TSet _set258 = iprot.readSetBegin();
                  struct.auths = new HashSet<ByteBuffer>(2*_set258.size);
                  for (int _i259 = 0; _i259 < _set258.size; ++_i259)
                   {
                    ByteBuffer _elem250;
                    _elem250 = iprot.readBinary();
                    struct.auths.add(_elem250);
                    ByteBuffer _elem260;
                    _elem260 = iprot.readBinary();
                    struct.auths.add(_elem260);
                   }
                   iprot.readSetEnd();
                 }
@@ -34369,9 +34478,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(AUTHS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.auths.size()));
            for (ByteBuffer _iter251 : struct.auths)
            for (ByteBuffer _iter261 : struct.auths)
             {
              oprot.writeBinary(_iter251);
              oprot.writeBinary(_iter261);
             }
             oprot.writeSetEnd();
           }
@@ -34442,9 +34551,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetAuths()) {
           {
             oprot.writeI32(struct.auths.size());
            for (ByteBuffer _iter252 : struct.auths)
            for (ByteBuffer _iter262 : struct.auths)
             {
              oprot.writeBinary(_iter252);
              oprot.writeBinary(_iter262);
             }
           }
         }
@@ -34476,13 +34585,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TSet _set253 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.auths = new HashSet<ByteBuffer>(2*_set253.size);
            for (int _i254 = 0; _i254 < _set253.size; ++_i254)
            org.apache.thrift.protocol.TSet _set263 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.auths = new HashSet<ByteBuffer>(2*_set263.size);
            for (int _i264 = 0; _i264 < _set263.size; ++_i264)
             {
              ByteBuffer _elem255;
              _elem255 = iprot.readBinary();
              struct.auths.add(_elem255);
              ByteBuffer _elem265;
              _elem265 = iprot.readBinary();
              struct.auths.add(_elem265);
             }
           }
           struct.setAuthsIsSet(true);
@@ -36178,15 +36287,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map256 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map256.size);
                  for (int _i257 = 0; _i257 < _map256.size; ++_i257)
                  org.apache.thrift.protocol.TMap _map266 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map266.size);
                  for (int _i267 = 0; _i267 < _map266.size; ++_i267)
                   {
                    String _key258;
                    String _val259;
                    _key258 = iprot.readString();
                    _val259 = iprot.readString();
                    struct.success.put(_key258, _val259);
                    String _key268;
                    String _val269;
                    _key268 = iprot.readString();
                    _val269 = iprot.readString();
                    struct.success.put(_key268, _val269);
                   }
                   iprot.readMapEnd();
                 }
@@ -36241,10 +36350,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (Map.Entry<String, String> _iter260 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter270 : struct.success.entrySet())
             {
              oprot.writeString(_iter260.getKey());
              oprot.writeString(_iter260.getValue());
              oprot.writeString(_iter270.getKey());
              oprot.writeString(_iter270.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -36299,10 +36408,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, String> _iter261 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter271 : struct.success.entrySet())
             {
              oprot.writeString(_iter261.getKey());
              oprot.writeString(_iter261.getValue());
              oprot.writeString(_iter271.getKey());
              oprot.writeString(_iter271.getValue());
             }
           }
         }
@@ -36323,15 +36432,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map262 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map262.size);
            for (int _i263 = 0; _i263 < _map262.size; ++_i263)
            org.apache.thrift.protocol.TMap _map272 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map272.size);
            for (int _i273 = 0; _i273 < _map272.size; ++_i273)
             {
              String _key264;
              String _val265;
              _key264 = iprot.readString();
              _val265 = iprot.readString();
              struct.success.put(_key264, _val265);
              String _key274;
              String _val275;
              _key274 = iprot.readString();
              _val275 = iprot.readString();
              struct.success.put(_key274, _val275);
             }
           }
           struct.setSuccessIsSet(true);
@@ -39904,13 +40013,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list266 = iprot.readListBegin();
                  struct.success = new ArrayList<ByteBuffer>(_list266.size);
                  for (int _i267 = 0; _i267 < _list266.size; ++_i267)
                  org.apache.thrift.protocol.TList _list276 = iprot.readListBegin();
                  struct.success = new ArrayList<ByteBuffer>(_list276.size);
                  for (int _i277 = 0; _i277 < _list276.size; ++_i277)
                   {
                    ByteBuffer _elem268;
                    _elem268 = iprot.readBinary();
                    struct.success.add(_elem268);
                    ByteBuffer _elem278;
                    _elem278 = iprot.readBinary();
                    struct.success.add(_elem278);
                   }
                   iprot.readListEnd();
                 }
@@ -39965,9 +40074,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (ByteBuffer _iter269 : struct.success)
            for (ByteBuffer _iter279 : struct.success)
             {
              oprot.writeBinary(_iter269);
              oprot.writeBinary(_iter279);
             }
             oprot.writeListEnd();
           }
@@ -40022,9 +40131,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ByteBuffer _iter270 : struct.success)
            for (ByteBuffer _iter280 : struct.success)
             {
              oprot.writeBinary(_iter270);
              oprot.writeBinary(_iter280);
             }
           }
         }
@@ -40045,13 +40154,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list271 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<ByteBuffer>(_list271.size);
            for (int _i272 = 0; _i272 < _list271.size; ++_i272)
            org.apache.thrift.protocol.TList _list281 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<ByteBuffer>(_list281.size);
            for (int _i282 = 0; _i282 < _list281.size; ++_i282)
             {
              ByteBuffer _elem273;
              _elem273 = iprot.readBinary();
              struct.success.add(_elem273);
              ByteBuffer _elem283;
              _elem283 = iprot.readBinary();
              struct.success.add(_elem283);
             }
           }
           struct.setSuccessIsSet(true);
@@ -40747,13 +40856,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set274 = iprot.readSetBegin();
                  struct.success = new HashSet<String>(2*_set274.size);
                  for (int _i275 = 0; _i275 < _set274.size; ++_i275)
                  org.apache.thrift.protocol.TSet _set284 = iprot.readSetBegin();
                  struct.success = new HashSet<String>(2*_set284.size);
                  for (int _i285 = 0; _i285 < _set284.size; ++_i285)
                   {
                    String _elem276;
                    _elem276 = iprot.readString();
                    struct.success.add(_elem276);
                    String _elem286;
                    _elem286 = iprot.readString();
                    struct.success.add(_elem286);
                   }
                   iprot.readSetEnd();
                 }
@@ -40781,9 +40890,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (String _iter277 : struct.success)
            for (String _iter287 : struct.success)
             {
              oprot.writeString(_iter277);
              oprot.writeString(_iter287);
             }
             oprot.writeSetEnd();
           }
@@ -40814,9 +40923,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (String _iter278 : struct.success)
            for (String _iter288 : struct.success)
             {
              oprot.writeString(_iter278);
              oprot.writeString(_iter288);
             }
           }
         }
@@ -40828,13 +40937,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(1);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TSet _set279 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashSet<String>(2*_set279.size);
            for (int _i280 = 0; _i280 < _set279.size; ++_i280)
            org.apache.thrift.protocol.TSet _set289 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashSet<String>(2*_set289.size);
            for (int _i290 = 0; _i290 < _set289.size; ++_i290)
             {
              String _elem281;
              _elem281 = iprot.readString();
              struct.success.add(_elem281);
              String _elem291;
              _elem291 = iprot.readString();
              struct.success.add(_elem291);
             }
           }
           struct.setSuccessIsSet(true);
@@ -41858,25 +41967,25 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map282 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Set<IteratorScope>>(2*_map282.size);
                  for (int _i283 = 0; _i283 < _map282.size; ++_i283)
                  org.apache.thrift.protocol.TMap _map292 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Set<IteratorScope>>(2*_map292.size);
                  for (int _i293 = 0; _i293 < _map292.size; ++_i293)
                   {
                    String _key284;
                    Set<IteratorScope> _val285;
                    _key284 = iprot.readString();
                    String _key294;
                    Set<IteratorScope> _val295;
                    _key294 = iprot.readString();
                     {
                      org.apache.thrift.protocol.TSet _set286 = iprot.readSetBegin();
                      _val285 = new HashSet<IteratorScope>(2*_set286.size);
                      for (int _i287 = 0; _i287 < _set286.size; ++_i287)
                      org.apache.thrift.protocol.TSet _set296 = iprot.readSetBegin();
                      _val295 = new HashSet<IteratorScope>(2*_set296.size);
                      for (int _i297 = 0; _i297 < _set296.size; ++_i297)
                       {
                        IteratorScope _elem288;
                        _elem288 = IteratorScope.findByValue(iprot.readI32());
                        _val285.add(_elem288);
                        IteratorScope _elem298;
                        _elem298 = IteratorScope.findByValue(iprot.readI32());
                        _val295.add(_elem298);
                       }
                       iprot.readSetEnd();
                     }
                    struct.success.put(_key284, _val285);
                    struct.success.put(_key294, _val295);
                   }
                   iprot.readMapEnd();
                 }
@@ -41931,14 +42040,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, struct.success.size()));
            for (Map.Entry<String, Set<IteratorScope>> _iter289 : struct.success.entrySet())
            for (Map.Entry<String, Set<IteratorScope>> _iter299 : struct.success.entrySet())
             {
              oprot.writeString(_iter289.getKey());
              oprot.writeString(_iter299.getKey());
               {
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, _iter289.getValue().size()));
                for (IteratorScope _iter290 : _iter289.getValue())
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, _iter299.getValue().size()));
                for (IteratorScope _iter300 : _iter299.getValue())
                 {
                  oprot.writeI32(_iter290.getValue());
                  oprot.writeI32(_iter300.getValue());
                 }
                 oprot.writeSetEnd();
               }
@@ -41996,14 +42105,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, Set<IteratorScope>> _iter291 : struct.success.entrySet())
            for (Map.Entry<String, Set<IteratorScope>> _iter301 : struct.success.entrySet())
             {
              oprot.writeString(_iter291.getKey());
              oprot.writeString(_iter301.getKey());
               {
                oprot.writeI32(_iter291.getValue().size());
                for (IteratorScope _iter292 : _iter291.getValue())
                oprot.writeI32(_iter301.getValue().size());
                for (IteratorScope _iter302 : _iter301.getValue())
                 {
                  oprot.writeI32(_iter292.getValue());
                  oprot.writeI32(_iter302.getValue());
                 }
               }
             }
@@ -42026,24 +42135,24 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map293 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.success = new HashMap<String,Set<IteratorScope>>(2*_map293.size);
            for (int _i294 = 0; _i294 < _map293.size; ++_i294)
            org.apache.thrift.protocol.TMap _map303 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.success = new HashMap<String,Set<IteratorScope>>(2*_map303.size);
            for (int _i304 = 0; _i304 < _map303.size; ++_i304)
             {
              String _key295;
              Set<IteratorScope> _val296;
              _key295 = iprot.readString();
              String _key305;
              Set<IteratorScope> _val306;
              _key305 = iprot.readString();
               {
                org.apache.thrift.protocol.TSet _set297 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
                _val296 = new HashSet<IteratorScope>(2*_set297.size);
                for (int _i298 = 0; _i298 < _set297.size; ++_i298)
                org.apache.thrift.protocol.TSet _set307 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
                _val306 = new HashSet<IteratorScope>(2*_set307.size);
                for (int _i308 = 0; _i308 < _set307.size; ++_i308)
                 {
                  IteratorScope _elem299;
                  _elem299 = IteratorScope.findByValue(iprot.readI32());
                  _val296.add(_elem299);
                  IteratorScope _elem309;
                  _elem309 = IteratorScope.findByValue(iprot.readI32());
                  _val306.add(_elem309);
                 }
               }
              struct.success.put(_key295, _val296);
              struct.success.put(_key305, _val306);
             }
           }
           struct.setSuccessIsSet(true);
@@ -43067,15 +43176,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map300 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Integer>(2*_map300.size);
                  for (int _i301 = 0; _i301 < _map300.size; ++_i301)
                  org.apache.thrift.protocol.TMap _map310 = iprot.readMapBegin();
                  struct.success = new HashMap<String,Integer>(2*_map310.size);
                  for (int _i311 = 0; _i311 < _map310.size; ++_i311)
                   {
                    String _key302;
                    int _val303;
                    _key302 = iprot.readString();
                    _val303 = iprot.readI32();
                    struct.success.put(_key302, _val303);
                    String _key312;
                    int _val313;
                    _key312 = iprot.readString();
                    _val313 = iprot.readI32();
                    struct.success.put(_key312, _val313);
                   }
                   iprot.readMapEnd();
                 }
@@ -43130,10 +43239,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, struct.success.size()));
            for (Map.Entry<String, Integer> _iter304 : struct.success.entrySet())
            for (Map.Entry<String, Integer> _iter314 : struct.success.entrySet())
             {
              oprot.writeString(_iter304.getKey());
              oprot.writeI32(_iter304.getValue());
              oprot.writeString(_iter314.getKey());
              oprot.writeI32(_iter314.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -43188,10 +43297,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, Integer> _iter305 : struct.success.entrySet())
            for (Map.Entry<String, Integer> _iter315 : struct.success.entrySet())
             {
              oprot.writeString(_iter305.getKey());
              oprot.writeI32(_iter305.getValue());
              oprot.writeString(_iter315.getKey());
              oprot.writeI32(_iter315.getValue());
             }
           }
         }
@@ -43212,15 +43321,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map306 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.success = new HashMap<String,Integer>(2*_map306.size);
            for (int _i307 = 0; _i307 < _map306.size; ++_i307)
            org.apache.thrift.protocol.TMap _map316 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.success = new HashMap<String,Integer>(2*_map316.size);
            for (int _i317 = 0; _i317 < _map316.size; ++_i317)
             {
              String _key308;
              int _val309;
              _key308 = iprot.readString();
              _val309 = iprot.readI32();
              struct.success.put(_key308, _val309);
              String _key318;
              int _val319;
              _key318 = iprot.readString();
              _val319 = iprot.readI32();
              struct.success.put(_key318, _val319);
             }
           }
           struct.setSuccessIsSet(true);
@@ -48440,13 +48549,13 @@ import org.slf4j.LoggerFactory;
             case 4: // SCOPES
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set310 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set310.size);
                  for (int _i311 = 0; _i311 < _set310.size; ++_i311)
                  org.apache.thrift.protocol.TSet _set320 = iprot.readSetBegin();
                  struct.scopes = new HashSet<IteratorScope>(2*_set320.size);
                  for (int _i321 = 0; _i321 < _set320.size; ++_i321)
                   {
                    IteratorScope _elem312;
                    _elem312 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem312);
                    IteratorScope _elem322;
                    _elem322 = IteratorScope.findByValue(iprot.readI32());
                    struct.scopes.add(_elem322);
                   }
                   iprot.readSetEnd();
                 }
@@ -48489,9 +48598,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SCOPES_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, struct.scopes.size()));
            for (IteratorScope _iter313 : struct.scopes)
            for (IteratorScope _iter323 : struct.scopes)
             {
              oprot.writeI32(_iter313.getValue());
              oprot.writeI32(_iter323.getValue());
             }
             oprot.writeSetEnd();
           }
@@ -48540,9 +48649,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetScopes()) {
           {
             oprot.writeI32(struct.scopes.size());
            for (IteratorScope _iter314 : struct.scopes)
            for (IteratorScope _iter324 : struct.scopes)
             {
              oprot.writeI32(_iter314.getValue());
              oprot.writeI32(_iter324.getValue());
             }
           }
         }
@@ -48566,13 +48675,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TSet _set315 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set315.size);
            for (int _i316 = 0; _i316 < _set315.size; ++_i316)
            org.apache.thrift.protocol.TSet _set325 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.scopes = new HashSet<IteratorScope>(2*_set325.size);
            for (int _i326 = 0; _i326 < _set325.size; ++_i326)
             {
              IteratorScope _elem317;
              _elem317 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem317);
              IteratorScope _elem327;
              _elem327 = IteratorScope.findByValue(iprot.readI32());
              struct.scopes.add(_elem327);
             }
           }
           struct.setScopesIsSet(true);
@@ -51990,25 +52099,25 @@ import org.slf4j.LoggerFactory;
             case 3: // GROUPS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map318 = iprot.readMapBegin();
                  struct.groups = new HashMap<String,Set<String>>(2*_map318.size);
                  for (int _i319 = 0; _i319 < _map318.size; ++_i319)
                  org.apache.thrift.protocol.TMap _map328 = iprot.readMapBegin();
                  struct.groups = new HashMap<String,Set<String>>(2*_map328.size);
                  for (int _i329 = 0; _i329 < _map328.size; ++_i329)
                   {
                    String _key320;
                    Set<String> _val321;
                    _key320 = iprot.readString();
                    String _key330;
                    Set<String> _val331;
                    _key330 = iprot.readString();
                     {
                      org.apache.thrift.protocol.TSet _set322 = iprot.readSetBegin();
                      _val321 = new HashSet<String>(2*_set322.size);
                      for (int _i323 = 0; _i323 < _set322.size; ++_i323)
                      org.apache.thrift.protocol.TSet _set332 = iprot.readSetBegin();
                      _val331 = new HashSet<String>(2*_set332.size);
                      for (int _i333 = 0; _i333 < _set332.size; ++_i333)
                       {
                        String _elem324;
                        _elem324 = iprot.readString();
                        _val321.add(_elem324);
                        String _elem334;
                        _elem334 = iprot.readString();
                        _val331.add(_elem334);
                       }
                       iprot.readSetEnd();
                     }
                    struct.groups.put(_key320, _val321);
                    struct.groups.put(_key330, _val331);
                   }
                   iprot.readMapEnd();
                 }
@@ -52046,14 +52155,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(GROUPS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, struct.groups.size()));
            for (Map.Entry<String, Set<String>> _iter325 : struct.groups.entrySet())
            for (Map.Entry<String, Set<String>> _iter335 : struct.groups.entrySet())
             {
              oprot.writeString(_iter325.getKey());
              oprot.writeString(_iter335.getKey());
               {
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, _iter325.getValue().size()));
                for (String _iter326 : _iter325.getValue())
                oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, _iter335.getValue().size()));
                for (String _iter336 : _iter335.getValue())
                 {
                  oprot.writeString(_iter326);
                  oprot.writeString(_iter336);
                 }
                 oprot.writeSetEnd();
               }
@@ -52099,14 +52208,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetGroups()) {
           {
             oprot.writeI32(struct.groups.size());
            for (Map.Entry<String, Set<String>> _iter327 : struct.groups.entrySet())
            for (Map.Entry<String, Set<String>> _iter337 : struct.groups.entrySet())
             {
              oprot.writeString(_iter327.getKey());
              oprot.writeString(_iter337.getKey());
               {
                oprot.writeI32(_iter327.getValue().size());
                for (String _iter328 : _iter327.getValue())
                oprot.writeI32(_iter337.getValue().size());
                for (String _iter338 : _iter337.getValue())
                 {
                  oprot.writeString(_iter328);
                  oprot.writeString(_iter338);
                 }
               }
             }
@@ -52128,24 +52237,24 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TMap _map329 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.groups = new HashMap<String,Set<String>>(2*_map329.size);
            for (int _i330 = 0; _i330 < _map329.size; ++_i330)
            org.apache.thrift.protocol.TMap _map339 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.SET, iprot.readI32());
            struct.groups = new HashMap<String,Set<String>>(2*_map339.size);
            for (int _i340 = 0; _i340 < _map339.size; ++_i340)
             {
              String _key331;
              Set<String> _val332;
              _key331 = iprot.readString();
              String _key341;
              Set<String> _val342;
              _key341 = iprot.readString();
               {
                org.apache.thrift.protocol.TSet _set333 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val332 = new HashSet<String>(2*_set333.size);
                for (int _i334 = 0; _i334 < _set333.size; ++_i334)
                org.apache.thrift.protocol.TSet _set343 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val342 = new HashSet<String>(2*_set343.size);
                for (int _i344 = 0; _i344 < _set343.size; ++_i344)
                 {
                  String _elem335;
                  _elem335 = iprot.readString();
                  _val332.add(_elem335);
                  String _elem345;
                  _elem345 = iprot.readString();
                  _val342.add(_elem345);
                 }
               }
              struct.groups.put(_key331, _val332);
              struct.groups.put(_key341, _val342);
             }
           }
           struct.setGroupsIsSet(true);
@@ -55148,14 +55257,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set336 = iprot.readSetBegin();
                  struct.success = new HashSet<Range>(2*_set336.size);
                  for (int _i337 = 0; _i337 < _set336.size; ++_i337)
                  org.apache.thrift.protocol.TSet _set346 = iprot.readSetBegin();
                  struct.success = new HashSet<Range>(2*_set346.size);
                  for (int _i347 = 0; _i347 < _set346.size; ++_i347)
                   {
                    Range _elem338;
                    _elem338 = new Range();
                    _elem338.read(iprot);
                    struct.success.add(_elem338);
                    Range _elem348;
                    _elem348 = new Range();
                    _elem348.read(iprot);
                    struct.success.add(_elem348);
                   }
                   iprot.readSetEnd();
                 }
@@ -55210,9 +55319,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (Range _iter339 : struct.success)
            for (Range _iter349 : struct.success)
             {
              _iter339.write(oprot);
              _iter349.write(oprot);
             }
             oprot.writeSetEnd();
           }
@@ -55267,9 +55376,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Range _iter340 : struct.success)
            for (Range _iter350 : struct.success)
             {
              _iter340.write(oprot);
              _iter350.write(oprot);
             }
           }
         }
@@ -55290,14 +55399,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TSet _set341 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new HashSet<Range>(2*_set341.size);
            for (int _i342 = 0; _i342 < _set341.size; ++_i342)
            org.apache.thrift.protocol.TSet _set351 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new HashSet<Range>(2*_set351.size);
            for (int _i352 = 0; _i352 < _set351.size; ++_i352)
             {
              Range _elem343;
              _elem343 = new Range();
              _elem343.read(iprot);
              struct.success.add(_elem343);
              Range _elem353;
              _elem353 = new Range();
              _elem353.read(iprot);
              struct.success.add(_elem353);
             }
           }
           struct.setSuccessIsSet(true);
@@ -56809,15 +56918,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map344 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map344.size);
                  for (int _i345 = 0; _i345 < _map344.size; ++_i345)
                  org.apache.thrift.protocol.TMap _map354 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map354.size);
                  for (int _i355 = 0; _i355 < _map354.size; ++_i355)
                   {
                    String _key346;
                    String _val347;
                    _key346 = iprot.readString();
                    _val347 = iprot.readString();
                    struct.success.put(_key346, _val347);
                    String _key356;
                    String _val357;
                    _key356 = iprot.readString();
                    _val357 = iprot.readString();
                    struct.success.put(_key356, _val357);
                   }
                   iprot.readMapEnd();
                 }
@@ -56845,10 +56954,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (Map.Entry<String, String> _iter348 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter358 : struct.success.entrySet())
             {
              oprot.writeString(_iter348.getKey());
              oprot.writeString(_iter348.getValue());
              oprot.writeString(_iter358.getKey());
              oprot.writeString(_iter358.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -56879,10 +56988,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, String> _iter349 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter359 : struct.success.entrySet())
             {
              oprot.writeString(_iter349.getKey());
              oprot.writeString(_iter349.getValue());
              oprot.writeString(_iter359.getKey());
              oprot.writeString(_iter359.getValue());
             }
           }
         }
@@ -56894,15 +57003,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(1);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map350 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map350.size);
            for (int _i351 = 0; _i351 < _map350.size; ++_i351)
            org.apache.thrift.protocol.TMap _map360 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map360.size);
            for (int _i361 = 0; _i361 < _map360.size; ++_i361)
             {
              String _key352;
              String _val353;
              _key352 = iprot.readString();
              _val353 = iprot.readString();
              struct.success.put(_key352, _val353);
              String _key362;
              String _val363;
              _key362 = iprot.readString();
              _val363 = iprot.readString();
              struct.success.put(_key362, _val363);
             }
           }
           struct.setSuccessIsSet(true);
@@ -60088,14 +60197,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list354 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveScan>(_list354.size);
                  for (int _i355 = 0; _i355 < _list354.size; ++_i355)
                  org.apache.thrift.protocol.TList _list364 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveScan>(_list364.size);
                  for (int _i365 = 0; _i365 < _list364.size; ++_i365)
                   {
                    ActiveScan _elem356;
                    _elem356 = new ActiveScan();
                    _elem356.read(iprot);
                    struct.success.add(_elem356);
                    ActiveScan _elem366;
                    _elem366 = new ActiveScan();
                    _elem366.read(iprot);
                    struct.success.add(_elem366);
                   }
                   iprot.readListEnd();
                 }
@@ -60141,9 +60250,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (ActiveScan _iter357 : struct.success)
            for (ActiveScan _iter367 : struct.success)
             {
              _iter357.write(oprot);
              _iter367.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -60190,9 +60299,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ActiveScan _iter358 : struct.success)
            for (ActiveScan _iter368 : struct.success)
             {
              _iter358.write(oprot);
              _iter368.write(oprot);
             }
           }
         }
@@ -60210,14 +60319,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list359 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveScan>(_list359.size);
            for (int _i360 = 0; _i360 < _list359.size; ++_i360)
            org.apache.thrift.protocol.TList _list369 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveScan>(_list369.size);
            for (int _i370 = 0; _i370 < _list369.size; ++_i370)
             {
              ActiveScan _elem361;
              _elem361 = new ActiveScan();
              _elem361.read(iprot);
              struct.success.add(_elem361);
              ActiveScan _elem371;
              _elem371 = new ActiveScan();
              _elem371.read(iprot);
              struct.success.add(_elem371);
             }
           }
           struct.setSuccessIsSet(true);
@@ -61165,14 +61274,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list362 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveCompaction>(_list362.size);
                  for (int _i363 = 0; _i363 < _list362.size; ++_i363)
                  org.apache.thrift.protocol.TList _list372 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveCompaction>(_list372.size);
                  for (int _i373 = 0; _i373 < _list372.size; ++_i373)
                   {
                    ActiveCompaction _elem364;
                    _elem364 = new ActiveCompaction();
                    _elem364.read(iprot);
                    struct.success.add(_elem364);
                    ActiveCompaction _elem374;
                    _elem374 = new ActiveCompaction();
                    _elem374.read(iprot);
                    struct.success.add(_elem374);
                   }
                   iprot.readListEnd();
                 }
@@ -61218,9 +61327,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (ActiveCompaction _iter365 : struct.success)
            for (ActiveCompaction _iter375 : struct.success)
             {
              _iter365.write(oprot);
              _iter375.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -61267,9 +61376,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ActiveCompaction _iter366 : struct.success)
            for (ActiveCompaction _iter376 : struct.success)
             {
              _iter366.write(oprot);
              _iter376.write(oprot);
             }
           }
         }
@@ -61287,14 +61396,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list367 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveCompaction>(_list367.size);
            for (int _i368 = 0; _i368 < _list367.size; ++_i368)
            org.apache.thrift.protocol.TList _list377 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveCompaction>(_list377.size);
            for (int _i378 = 0; _i378 < _list377.size; ++_i378)
             {
              ActiveCompaction _elem369;
              _elem369 = new ActiveCompaction();
              _elem369.read(iprot);
              struct.success.add(_elem369);
              ActiveCompaction _elem379;
              _elem379 = new ActiveCompaction();
              _elem379.read(iprot);
              struct.success.add(_elem379);
             }
           }
           struct.setSuccessIsSet(true);
@@ -62136,15 +62245,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map370 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map370.size);
                  for (int _i371 = 0; _i371 < _map370.size; ++_i371)
                  org.apache.thrift.protocol.TMap _map380 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map380.size);
                  for (int _i381 = 0; _i381 < _map380.size; ++_i381)
                   {
                    String _key372;
                    String _val373;
                    _key372 = iprot.readString();
                    _val373 = iprot.readString();
                    struct.success.put(_key372, _val373);
                    String _key382;
                    String _val383;
                    _key382 = iprot.readString();
                    _val383 = iprot.readString();
                    struct.success.put(_key382, _val383);
                   }
                   iprot.readMapEnd();
                 }
@@ -62190,10 +62299,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (Map.Entry<String, String> _iter374 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter384 : struct.success.entrySet())
             {
              oprot.writeString(_iter374.getKey());
              oprot.writeString(_iter374.getValue());
              oprot.writeString(_iter384.getKey());
              oprot.writeString(_iter384.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -62240,10 +62349,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, String> _iter375 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter385 : struct.success.entrySet())
             {
              oprot.writeString(_iter375.getKey());
              oprot.writeString(_iter375.getValue());
              oprot.writeString(_iter385.getKey());
              oprot.writeString(_iter385.getValue());
             }
           }
         }
@@ -62261,15 +62370,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map376 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map376.size);
            for (int _i377 = 0; _i377 < _map376.size; ++_i377)
            org.apache.thrift.protocol.TMap _map386 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map386.size);
            for (int _i387 = 0; _i387 < _map386.size; ++_i387)
             {
              String _key378;
              String _val379;
              _key378 = iprot.readString();
              _val379 = iprot.readString();
              struct.success.put(_key378, _val379);
              String _key388;
              String _val389;
              _key388 = iprot.readString();
              _val389 = iprot.readString();
              struct.success.put(_key388, _val389);
             }
           }
           struct.setSuccessIsSet(true);
@@ -63111,15 +63220,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map380 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map380.size);
                  for (int _i381 = 0; _i381 < _map380.size; ++_i381)
                  org.apache.thrift.protocol.TMap _map390 = iprot.readMapBegin();
                  struct.success = new HashMap<String,String>(2*_map390.size);
                  for (int _i391 = 0; _i391 < _map390.size; ++_i391)
                   {
                    String _key382;
                    String _val383;
                    _key382 = iprot.readString();
                    _val383 = iprot.readString();
                    struct.success.put(_key382, _val383);
                    String _key392;
                    String _val393;
                    _key392 = iprot.readString();
                    _val393 = iprot.readString();
                    struct.success.put(_key392, _val393);
                   }
                   iprot.readMapEnd();
                 }
@@ -63165,10 +63274,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (Map.Entry<String, String> _iter384 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter394 : struct.success.entrySet())
             {
              oprot.writeString(_iter384.getKey());
              oprot.writeString(_iter384.getValue());
              oprot.writeString(_iter394.getKey());
              oprot.writeString(_iter394.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -63215,10 +63324,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<String, String> _iter385 : struct.success.entrySet())
            for (Map.Entry<String, String> _iter395 : struct.success.entrySet())
             {
              oprot.writeString(_iter385.getKey());
              oprot.writeString(_iter385.getValue());
              oprot.writeString(_iter395.getKey());
              oprot.writeString(_iter395.getValue());
             }
           }
         }
@@ -63236,15 +63345,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map386 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map386.size);
            for (int _i387 = 0; _i387 < _map386.size; ++_i387)
            org.apache.thrift.protocol.TMap _map396 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashMap<String,String>(2*_map396.size);
            for (int _i397 = 0; _i397 < _map396.size; ++_i397)
             {
              String _key388;
              String _val389;
              _key388 = iprot.readString();
              _val389 = iprot.readString();
              struct.success.put(_key388, _val389);
              String _key398;
              String _val399;
              _key398 = iprot.readString();
              _val399 = iprot.readString();
              struct.success.put(_key398, _val399);
             }
           }
           struct.setSuccessIsSet(true);
@@ -63935,13 +64044,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list390 = iprot.readListBegin();
                  struct.success = new ArrayList<String>(_list390.size);
                  for (int _i391 = 0; _i391 < _list390.size; ++_i391)
                  org.apache.thrift.protocol.TList _list400 = iprot.readListBegin();
                  struct.success = new ArrayList<String>(_list400.size);
                  for (int _i401 = 0; _i401 < _list400.size; ++_i401)
                   {
                    String _elem392;
                    _elem392 = iprot.readString();
                    struct.success.add(_elem392);
                    String _elem402;
                    _elem402 = iprot.readString();
                    struct.success.add(_elem402);
                   }
                   iprot.readListEnd();
                 }
@@ -63969,9 +64078,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (String _iter393 : struct.success)
            for (String _iter403 : struct.success)
             {
              oprot.writeString(_iter393);
              oprot.writeString(_iter403);
             }
             oprot.writeListEnd();
           }
@@ -64002,9 +64111,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (String _iter394 : struct.success)
            for (String _iter404 : struct.success)
             {
              oprot.writeString(_iter394);
              oprot.writeString(_iter404);
             }
           }
         }
@@ -64016,13 +64125,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(1);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list395 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<String>(_list395.size);
            for (int _i396 = 0; _i396 < _list395.size; ++_i396)
            org.apache.thrift.protocol.TList _list405 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<String>(_list405.size);
            for (int _i406 = 0; _i406 < _list405.size; ++_i406)
             {
              String _elem397;
              _elem397 = iprot.readString();
              struct.success.add(_elem397);
              String _elem407;
              _elem407 = iprot.readString();
              struct.success.add(_elem407);
             }
           }
           struct.setSuccessIsSet(true);
@@ -67585,15 +67694,15 @@ import org.slf4j.LoggerFactory;
             case 3: // PROPERTIES
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map398 = iprot.readMapBegin();
                  struct.properties = new HashMap<String,String>(2*_map398.size);
                  for (int _i399 = 0; _i399 < _map398.size; ++_i399)
                  org.apache.thrift.protocol.TMap _map408 = iprot.readMapBegin();
                  struct.properties = new HashMap<String,String>(2*_map408.size);
                  for (int _i409 = 0; _i409 < _map408.size; ++_i409)
                   {
                    String _key400;
                    String _val401;
                    _key400 = iprot.readString();
                    _val401 = iprot.readString();
                    struct.properties.put(_key400, _val401);
                    String _key410;
                    String _val411;
                    _key410 = iprot.readString();
                    _val411 = iprot.readString();
                    struct.properties.put(_key410, _val411);
                   }
                   iprot.readMapEnd();
                 }
@@ -67631,10 +67740,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(PROPERTIES_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.properties.size()));
            for (Map.Entry<String, String> _iter402 : struct.properties.entrySet())
            for (Map.Entry<String, String> _iter412 : struct.properties.entrySet())
             {
              oprot.writeString(_iter402.getKey());
              oprot.writeString(_iter402.getValue());
              oprot.writeString(_iter412.getKey());
              oprot.writeString(_iter412.getValue());
             }
             oprot.writeMapEnd();
           }
@@ -67677,10 +67786,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetProperties()) {
           {
             oprot.writeI32(struct.properties.size());
            for (Map.Entry<String, String> _iter403 : struct.properties.entrySet())
            for (Map.Entry<String, String> _iter413 : struct.properties.entrySet())
             {
              oprot.writeString(_iter403.getKey());
              oprot.writeString(_iter403.getValue());
              oprot.writeString(_iter413.getKey());
              oprot.writeString(_iter413.getValue());
             }
           }
         }
@@ -67700,15 +67809,15 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TMap _map404 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.properties = new HashMap<String,String>(2*_map404.size);
            for (int _i405 = 0; _i405 < _map404.size; ++_i405)
            org.apache.thrift.protocol.TMap _map414 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.properties = new HashMap<String,String>(2*_map414.size);
            for (int _i415 = 0; _i415 < _map414.size; ++_i415)
             {
              String _key406;
              String _val407;
              _key406 = iprot.readString();
              _val407 = iprot.readString();
              struct.properties.put(_key406, _val407);
              String _key416;
              String _val417;
              _key416 = iprot.readString();
              _val417 = iprot.readString();
              struct.properties.put(_key416, _val417);
             }
           }
           struct.setPropertiesIsSet(true);
@@ -68763,13 +68872,13 @@ import org.slf4j.LoggerFactory;
             case 3: // AUTHORIZATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set408 = iprot.readSetBegin();
                  struct.authorizations = new HashSet<ByteBuffer>(2*_set408.size);
                  for (int _i409 = 0; _i409 < _set408.size; ++_i409)
                  org.apache.thrift.protocol.TSet _set418 = iprot.readSetBegin();
                  struct.authorizations = new HashSet<ByteBuffer>(2*_set418.size);
                  for (int _i419 = 0; _i419 < _set418.size; ++_i419)
                   {
                    ByteBuffer _elem410;
                    _elem410 = iprot.readBinary();
                    struct.authorizations.add(_elem410);
                    ByteBuffer _elem420;
                    _elem420 = iprot.readBinary();
                    struct.authorizations.add(_elem420);
                   }
                   iprot.readSetEnd();
                 }
@@ -68807,9 +68916,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(AUTHORIZATIONS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.authorizations.size()));
            for (ByteBuffer _iter411 : struct.authorizations)
            for (ByteBuffer _iter421 : struct.authorizations)
             {
              oprot.writeBinary(_iter411);
              oprot.writeBinary(_iter421);
             }
             oprot.writeSetEnd();
           }
@@ -68852,9 +68961,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetAuthorizations()) {
           {
             oprot.writeI32(struct.authorizations.size());
            for (ByteBuffer _iter412 : struct.authorizations)
            for (ByteBuffer _iter422 : struct.authorizations)
             {
              oprot.writeBinary(_iter412);
              oprot.writeBinary(_iter422);
             }
           }
         }
@@ -68874,13 +68983,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TSet _set413 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new HashSet<ByteBuffer>(2*_set413.size);
            for (int _i414 = 0; _i414 < _set413.size; ++_i414)
            org.apache.thrift.protocol.TSet _set423 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new HashSet<ByteBuffer>(2*_set423.size);
            for (int _i424 = 0; _i424 < _set423.size; ++_i424)
             {
              ByteBuffer _elem415;
              _elem415 = iprot.readBinary();
              struct.authorizations.add(_elem415);
              ByteBuffer _elem425;
              _elem425 = iprot.readBinary();
              struct.authorizations.add(_elem425);
             }
           }
           struct.setAuthorizationsIsSet(true);
@@ -73264,13 +73373,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list416 = iprot.readListBegin();
                  struct.success = new ArrayList<ByteBuffer>(_list416.size);
                  for (int _i417 = 0; _i417 < _list416.size; ++_i417)
                  org.apache.thrift.protocol.TList _list426 = iprot.readListBegin();
                  struct.success = new ArrayList<ByteBuffer>(_list426.size);
                  for (int _i427 = 0; _i427 < _list426.size; ++_i427)
                   {
                    ByteBuffer _elem418;
                    _elem418 = iprot.readBinary();
                    struct.success.add(_elem418);
                    ByteBuffer _elem428;
                    _elem428 = iprot.readBinary();
                    struct.success.add(_elem428);
                   }
                   iprot.readListEnd();
                 }
@@ -73316,9 +73425,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (ByteBuffer _iter419 : struct.success)
            for (ByteBuffer _iter429 : struct.success)
             {
              oprot.writeBinary(_iter419);
              oprot.writeBinary(_iter429);
             }
             oprot.writeListEnd();
           }
@@ -73365,9 +73474,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ByteBuffer _iter420 : struct.success)
            for (ByteBuffer _iter430 : struct.success)
             {
              oprot.writeBinary(_iter420);
              oprot.writeBinary(_iter430);
             }
           }
         }
@@ -73385,13 +73494,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list421 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<ByteBuffer>(_list421.size);
            for (int _i422 = 0; _i422 < _list421.size; ++_i422)
            org.apache.thrift.protocol.TList _list431 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<ByteBuffer>(_list431.size);
            for (int _i432 = 0; _i432 < _list431.size; ++_i432)
             {
              ByteBuffer _elem423;
              _elem423 = iprot.readBinary();
              struct.success.add(_elem423);
              ByteBuffer _elem433;
              _elem433 = iprot.readBinary();
              struct.success.add(_elem433);
             }
           }
           struct.setSuccessIsSet(true);
@@ -79073,13 +79182,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
                 {
                  org.apache.thrift.protocol.TSet _set424 = iprot.readSetBegin();
                  struct.success = new HashSet<String>(2*_set424.size);
                  for (int _i425 = 0; _i425 < _set424.size; ++_i425)
                  org.apache.thrift.protocol.TSet _set434 = iprot.readSetBegin();
                  struct.success = new HashSet<String>(2*_set434.size);
                  for (int _i435 = 0; _i435 < _set434.size; ++_i435)
                   {
                    String _elem426;
                    _elem426 = iprot.readString();
                    struct.success.add(_elem426);
                    String _elem436;
                    _elem436 = iprot.readString();
                    struct.success.add(_elem436);
                   }
                   iprot.readSetEnd();
                 }
@@ -79134,9 +79243,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (String _iter427 : struct.success)
            for (String _iter437 : struct.success)
             {
              oprot.writeString(_iter427);
              oprot.writeString(_iter437);
             }
             oprot.writeSetEnd();
           }
@@ -79191,9 +79300,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (String _iter428 : struct.success)
            for (String _iter438 : struct.success)
             {
              oprot.writeString(_iter428);
              oprot.writeString(_iter438);
             }
           }
         }
@@ -79214,13 +79323,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TSet _set429 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashSet<String>(2*_set429.size);
            for (int _i430 = 0; _i430 < _set429.size; ++_i430)
            org.apache.thrift.protocol.TSet _set439 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new HashSet<String>(2*_set439.size);
            for (int _i440 = 0; _i440 < _set439.size; ++_i440)
             {
              String _elem431;
              _elem431 = iprot.readString();
              struct.success.add(_elem431);
              String _elem441;
              _elem441 = iprot.readString();
              struct.success.add(_elem441);
             }
           }
           struct.setSuccessIsSet(true);
@@ -88141,26 +88250,26 @@ import org.slf4j.LoggerFactory;
             case 3: // CELLS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map432 = iprot.readMapBegin();
                  struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map432.size);
                  for (int _i433 = 0; _i433 < _map432.size; ++_i433)
                  org.apache.thrift.protocol.TMap _map442 = iprot.readMapBegin();
                  struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map442.size);
                  for (int _i443 = 0; _i443 < _map442.size; ++_i443)
                   {
                    ByteBuffer _key434;
                    List<ColumnUpdate> _val435;
                    _key434 = iprot.readBinary();
                    ByteBuffer _key444;
                    List<ColumnUpdate> _val445;
                    _key444 = iprot.readBinary();
                     {
                      org.apache.thrift.protocol.TList _list436 = iprot.readListBegin();
                      _val435 = new ArrayList<ColumnUpdate>(_list436.size);
                      for (int _i437 = 0; _i437 < _list436.size; ++_i437)
                      org.apache.thrift.protocol.TList _list446 = iprot.readListBegin();
                      _val445 = new ArrayList<ColumnUpdate>(_list446.size);
                      for (int _i447 = 0; _i447 < _list446.size; ++_i447)
                       {
                        ColumnUpdate _elem438;
                        _elem438 = new ColumnUpdate();
                        _elem438.read(iprot);
                        _val435.add(_elem438);
                        ColumnUpdate _elem448;
                        _elem448 = new ColumnUpdate();
                        _elem448.read(iprot);
                        _val445.add(_elem448);
                       }
                       iprot.readListEnd();
                     }
                    struct.cells.put(_key434, _val435);
                    struct.cells.put(_key444, _val445);
                   }
                   iprot.readMapEnd();
                 }
@@ -88198,14 +88307,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(CELLS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, struct.cells.size()));
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter439 : struct.cells.entrySet())
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter449 : struct.cells.entrySet())
             {
              oprot.writeBinary(_iter439.getKey());
              oprot.writeBinary(_iter449.getKey());
               {
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter439.getValue().size()));
                for (ColumnUpdate _iter440 : _iter439.getValue())
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter449.getValue().size()));
                for (ColumnUpdate _iter450 : _iter449.getValue())
                 {
                  _iter440.write(oprot);
                  _iter450.write(oprot);
                 }
                 oprot.writeListEnd();
               }
@@ -88251,14 +88360,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetCells()) {
           {
             oprot.writeI32(struct.cells.size());
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter441 : struct.cells.entrySet())
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter451 : struct.cells.entrySet())
             {
              oprot.writeBinary(_iter441.getKey());
              oprot.writeBinary(_iter451.getKey());
               {
                oprot.writeI32(_iter441.getValue().size());
                for (ColumnUpdate _iter442 : _iter441.getValue())
                oprot.writeI32(_iter451.getValue().size());
                for (ColumnUpdate _iter452 : _iter451.getValue())
                 {
                  _iter442.write(oprot);
                  _iter452.write(oprot);
                 }
               }
             }
@@ -88280,25 +88389,25 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TMap _map443 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map443.size);
            for (int _i444 = 0; _i444 < _map443.size; ++_i444)
            org.apache.thrift.protocol.TMap _map453 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map453.size);
            for (int _i454 = 0; _i454 < _map453.size; ++_i454)
             {
              ByteBuffer _key445;
              List<ColumnUpdate> _val446;
              _key445 = iprot.readBinary();
              ByteBuffer _key455;
              List<ColumnUpdate> _val456;
              _key455 = iprot.readBinary();
               {
                org.apache.thrift.protocol.TList _list447 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val446 = new ArrayList<ColumnUpdate>(_list447.size);
                for (int _i448 = 0; _i448 < _list447.size; ++_i448)
                org.apache.thrift.protocol.TList _list457 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val456 = new ArrayList<ColumnUpdate>(_list457.size);
                for (int _i458 = 0; _i458 < _list457.size; ++_i458)
                 {
                  ColumnUpdate _elem449;
                  _elem449 = new ColumnUpdate();
                  _elem449.read(iprot);
                  _val446.add(_elem449);
                  ColumnUpdate _elem459;
                  _elem459 = new ColumnUpdate();
                  _elem459.read(iprot);
                  _val456.add(_elem459);
                 }
               }
              struct.cells.put(_key445, _val446);
              struct.cells.put(_key455, _val456);
             }
           }
           struct.setCellsIsSet(true);
@@ -90604,26 +90713,26 @@ import org.slf4j.LoggerFactory;
             case 2: // CELLS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map450 = iprot.readMapBegin();
                  struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map450.size);
                  for (int _i451 = 0; _i451 < _map450.size; ++_i451)
                  org.apache.thrift.protocol.TMap _map460 = iprot.readMapBegin();
                  struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map460.size);
                  for (int _i461 = 0; _i461 < _map460.size; ++_i461)
                   {
                    ByteBuffer _key452;
                    List<ColumnUpdate> _val453;
                    _key452 = iprot.readBinary();
                    ByteBuffer _key462;
                    List<ColumnUpdate> _val463;
                    _key462 = iprot.readBinary();
                     {
                      org.apache.thrift.protocol.TList _list454 = iprot.readListBegin();
                      _val453 = new ArrayList<ColumnUpdate>(_list454.size);
                      for (int _i455 = 0; _i455 < _list454.size; ++_i455)
                      org.apache.thrift.protocol.TList _list464 = iprot.readListBegin();
                      _val463 = new ArrayList<ColumnUpdate>(_list464.size);
                      for (int _i465 = 0; _i465 < _list464.size; ++_i465)
                       {
                        ColumnUpdate _elem456;
                        _elem456 = new ColumnUpdate();
                        _elem456.read(iprot);
                        _val453.add(_elem456);
                        ColumnUpdate _elem466;
                        _elem466 = new ColumnUpdate();
                        _elem466.read(iprot);
                        _val463.add(_elem466);
                       }
                       iprot.readListEnd();
                     }
                    struct.cells.put(_key452, _val453);
                    struct.cells.put(_key462, _val463);
                   }
                   iprot.readMapEnd();
                 }
@@ -90656,14 +90765,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(CELLS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, struct.cells.size()));
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter457 : struct.cells.entrySet())
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter467 : struct.cells.entrySet())
             {
              oprot.writeBinary(_iter457.getKey());
              oprot.writeBinary(_iter467.getKey());
               {
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter457.getValue().size()));
                for (ColumnUpdate _iter458 : _iter457.getValue())
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter467.getValue().size()));
                for (ColumnUpdate _iter468 : _iter467.getValue())
                 {
                  _iter458.write(oprot);
                  _iter468.write(oprot);
                 }
                 oprot.writeListEnd();
               }
@@ -90703,14 +90812,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetCells()) {
           {
             oprot.writeI32(struct.cells.size());
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter459 : struct.cells.entrySet())
            for (Map.Entry<ByteBuffer, List<ColumnUpdate>> _iter469 : struct.cells.entrySet())
             {
              oprot.writeBinary(_iter459.getKey());
              oprot.writeBinary(_iter469.getKey());
               {
                oprot.writeI32(_iter459.getValue().size());
                for (ColumnUpdate _iter460 : _iter459.getValue())
                oprot.writeI32(_iter469.getValue().size());
                for (ColumnUpdate _iter470 : _iter469.getValue())
                 {
                  _iter460.write(oprot);
                  _iter470.write(oprot);
                 }
               }
             }
@@ -90728,25 +90837,25 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(1)) {
           {
            org.apache.thrift.protocol.TMap _map461 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map461.size);
            for (int _i462 = 0; _i462 < _map461.size; ++_i462)
            org.apache.thrift.protocol.TMap _map471 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.cells = new HashMap<ByteBuffer,List<ColumnUpdate>>(2*_map471.size);
            for (int _i472 = 0; _i472 < _map471.size; ++_i472)
             {
              ByteBuffer _key463;
              List<ColumnUpdate> _val464;
              _key463 = iprot.readBinary();
              ByteBuffer _key473;
              List<ColumnUpdate> _val474;
              _key473 = iprot.readBinary();
               {
                org.apache.thrift.protocol.TList _list465 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val464 = new ArrayList<ColumnUpdate>(_list465.size);
                for (int _i466 = 0; _i466 < _list465.size; ++_i466)
                org.apache.thrift.protocol.TList _list475 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val474 = new ArrayList<ColumnUpdate>(_list475.size);
                for (int _i476 = 0; _i476 < _list475.size; ++_i476)
                 {
                  ColumnUpdate _elem467;
                  _elem467 = new ColumnUpdate();
                  _elem467.read(iprot);
                  _val464.add(_elem467);
                  ColumnUpdate _elem477;
                  _elem477 = new ColumnUpdate();
                  _elem477.read(iprot);
                  _val474.add(_elem477);
                 }
               }
              struct.cells.put(_key463, _val464);
              struct.cells.put(_key473, _val474);
             }
           }
           struct.setCellsIsSet(true);
@@ -95367,16 +95476,16 @@ import org.slf4j.LoggerFactory;
             case 2: // UPDATES
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map468 = iprot.readMapBegin();
                  struct.updates = new HashMap<ByteBuffer,ConditionalUpdates>(2*_map468.size);
                  for (int _i469 = 0; _i469 < _map468.size; ++_i469)
                  org.apache.thrift.protocol.TMap _map478 = iprot.readMapBegin();
                  struct.updates = new HashMap<ByteBuffer,ConditionalUpdates>(2*_map478.size);
                  for (int _i479 = 0; _i479 < _map478.size; ++_i479)
                   {
                    ByteBuffer _key470;
                    ConditionalUpdates _val471;
                    _key470 = iprot.readBinary();
                    _val471 = new ConditionalUpdates();
                    _val471.read(iprot);
                    struct.updates.put(_key470, _val471);
                    ByteBuffer _key480;
                    ConditionalUpdates _val481;
                    _key480 = iprot.readBinary();
                    _val481 = new ConditionalUpdates();
                    _val481.read(iprot);
                    struct.updates.put(_key480, _val481);
                   }
                   iprot.readMapEnd();
                 }
@@ -95409,10 +95518,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(UPDATES_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, struct.updates.size()));
            for (Map.Entry<ByteBuffer, ConditionalUpdates> _iter472 : struct.updates.entrySet())
            for (Map.Entry<ByteBuffer, ConditionalUpdates> _iter482 : struct.updates.entrySet())
             {
              oprot.writeBinary(_iter472.getKey());
              _iter472.getValue().write(oprot);
              oprot.writeBinary(_iter482.getKey());
              _iter482.getValue().write(oprot);
             }
             oprot.writeMapEnd();
           }
@@ -95449,10 +95558,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetUpdates()) {
           {
             oprot.writeI32(struct.updates.size());
            for (Map.Entry<ByteBuffer, ConditionalUpdates> _iter473 : struct.updates.entrySet())
            for (Map.Entry<ByteBuffer, ConditionalUpdates> _iter483 : struct.updates.entrySet())
             {
              oprot.writeBinary(_iter473.getKey());
              _iter473.getValue().write(oprot);
              oprot.writeBinary(_iter483.getKey());
              _iter483.getValue().write(oprot);
             }
           }
         }
@@ -95468,16 +95577,16 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(1)) {
           {
            org.apache.thrift.protocol.TMap _map474 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.updates = new HashMap<ByteBuffer,ConditionalUpdates>(2*_map474.size);
            for (int _i475 = 0; _i475 < _map474.size; ++_i475)
            org.apache.thrift.protocol.TMap _map484 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.updates = new HashMap<ByteBuffer,ConditionalUpdates>(2*_map484.size);
            for (int _i485 = 0; _i485 < _map484.size; ++_i485)
             {
              ByteBuffer _key476;
              ConditionalUpdates _val477;
              _key476 = iprot.readBinary();
              _val477 = new ConditionalUpdates();
              _val477.read(iprot);
              struct.updates.put(_key476, _val477);
              ByteBuffer _key486;
              ConditionalUpdates _val487;
              _key486 = iprot.readBinary();
              _val487 = new ConditionalUpdates();
              _val487.read(iprot);
              struct.updates.put(_key486, _val487);
             }
           }
           struct.setUpdatesIsSet(true);
@@ -96033,15 +96142,15 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map478 = iprot.readMapBegin();
                  struct.success = new HashMap<ByteBuffer,ConditionalStatus>(2*_map478.size);
                  for (int _i479 = 0; _i479 < _map478.size; ++_i479)
                  org.apache.thrift.protocol.TMap _map488 = iprot.readMapBegin();
                  struct.success = new HashMap<ByteBuffer,ConditionalStatus>(2*_map488.size);
                  for (int _i489 = 0; _i489 < _map488.size; ++_i489)
                   {
                    ByteBuffer _key480;
                    ConditionalStatus _val481;
                    _key480 = iprot.readBinary();
                    _val481 = ConditionalStatus.findByValue(iprot.readI32());
                    struct.success.put(_key480, _val481);
                    ByteBuffer _key490;
                    ConditionalStatus _val491;
                    _key490 = iprot.readBinary();
                    _val491 = ConditionalStatus.findByValue(iprot.readI32());
                    struct.success.put(_key490, _val491);
                   }
                   iprot.readMapEnd();
                 }
@@ -96096,10 +96205,10 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, struct.success.size()));
            for (Map.Entry<ByteBuffer, ConditionalStatus> _iter482 : struct.success.entrySet())
            for (Map.Entry<ByteBuffer, ConditionalStatus> _iter492 : struct.success.entrySet())
             {
              oprot.writeBinary(_iter482.getKey());
              oprot.writeI32(_iter482.getValue().getValue());
              oprot.writeBinary(_iter492.getKey());
              oprot.writeI32(_iter492.getValue().getValue());
             }
             oprot.writeMapEnd();
           }
@@ -96154,10 +96263,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (Map.Entry<ByteBuffer, ConditionalStatus> _iter483 : struct.success.entrySet())
            for (Map.Entry<ByteBuffer, ConditionalStatus> _iter493 : struct.success.entrySet())
             {
              oprot.writeBinary(_iter483.getKey());
              oprot.writeI32(_iter483.getValue().getValue());
              oprot.writeBinary(_iter493.getKey());
              oprot.writeI32(_iter493.getValue().getValue());
             }
           }
         }
@@ -96178,15 +96287,15 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(4);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TMap _map484 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.success = new HashMap<ByteBuffer,ConditionalStatus>(2*_map484.size);
            for (int _i485 = 0; _i485 < _map484.size; ++_i485)
            org.apache.thrift.protocol.TMap _map494 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.I32, iprot.readI32());
            struct.success = new HashMap<ByteBuffer,ConditionalStatus>(2*_map494.size);
            for (int _i495 = 0; _i495 < _map494.size; ++_i495)
             {
              ByteBuffer _key486;
              ConditionalStatus _val487;
              _key486 = iprot.readBinary();
              _val487 = ConditionalStatus.findByValue(iprot.readI32());
              struct.success.put(_key486, _val487);
              ByteBuffer _key496;
              ConditionalStatus _val497;
              _key496 = iprot.readBinary();
              _val497 = ConditionalStatus.findByValue(iprot.readI32());
              struct.success.put(_key496, _val497);
             }
           }
           struct.setSuccessIsSet(true);
diff --git a/proxy/src/main/java/org/apache/accumulo/proxy/thrift/CompactionStrategyConfig.java b/proxy/src/main/java/org/apache/accumulo/proxy/thrift/CompactionStrategyConfig.java
new file mode 100644
index 000000000..2ece0090e
-- /dev/null
++ b/proxy/src/main/java/org/apache/accumulo/proxy/thrift/CompactionStrategyConfig.java
@@ -0,0 +1,556 @@
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
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.accumulo.proxy.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all") public class CompactionStrategyConfig implements org.apache.thrift.TBase<CompactionStrategyConfig, CompactionStrategyConfig._Fields>, java.io.Serializable, Cloneable, Comparable<CompactionStrategyConfig> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CompactionStrategyConfig");

  private static final org.apache.thrift.protocol.TField CLASS_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("className", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField OPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("options", org.apache.thrift.protocol.TType.MAP, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CompactionStrategyConfigStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CompactionStrategyConfigTupleSchemeFactory());
  }

  public String className; // required
  public Map<String,String> options; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  @SuppressWarnings("all") public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    CLASS_NAME((short)1, "className"),
    OPTIONS((short)2, "options");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // CLASS_NAME
          return CLASS_NAME;
        case 2: // OPTIONS
          return OPTIONS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.CLASS_NAME, new org.apache.thrift.meta_data.FieldMetaData("className", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.OPTIONS, new org.apache.thrift.meta_data.FieldMetaData("options", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.MapMetaData(org.apache.thrift.protocol.TType.MAP, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING), 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CompactionStrategyConfig.class, metaDataMap);
  }

  public CompactionStrategyConfig() {
  }

  public CompactionStrategyConfig(
    String className,
    Map<String,String> options)
  {
    this();
    this.className = className;
    this.options = options;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CompactionStrategyConfig(CompactionStrategyConfig other) {
    if (other.isSetClassName()) {
      this.className = other.className;
    }
    if (other.isSetOptions()) {
      Map<String,String> __this__options = new HashMap<String,String>(other.options);
      this.options = __this__options;
    }
  }

  public CompactionStrategyConfig deepCopy() {
    return new CompactionStrategyConfig(this);
  }

  @Override
  public void clear() {
    this.className = null;
    this.options = null;
  }

  public String getClassName() {
    return this.className;
  }

  public CompactionStrategyConfig setClassName(String className) {
    this.className = className;
    return this;
  }

  public void unsetClassName() {
    this.className = null;
  }

  /** Returns true if field className is set (has been assigned a value) and false otherwise */
  public boolean isSetClassName() {
    return this.className != null;
  }

  public void setClassNameIsSet(boolean value) {
    if (!value) {
      this.className = null;
    }
  }

  public int getOptionsSize() {
    return (this.options == null) ? 0 : this.options.size();
  }

  public void putToOptions(String key, String val) {
    if (this.options == null) {
      this.options = new HashMap<String,String>();
    }
    this.options.put(key, val);
  }

  public Map<String,String> getOptions() {
    return this.options;
  }

  public CompactionStrategyConfig setOptions(Map<String,String> options) {
    this.options = options;
    return this;
  }

  public void unsetOptions() {
    this.options = null;
  }

  /** Returns true if field options is set (has been assigned a value) and false otherwise */
  public boolean isSetOptions() {
    return this.options != null;
  }

  public void setOptionsIsSet(boolean value) {
    if (!value) {
      this.options = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case CLASS_NAME:
      if (value == null) {
        unsetClassName();
      } else {
        setClassName((String)value);
      }
      break;

    case OPTIONS:
      if (value == null) {
        unsetOptions();
      } else {
        setOptions((Map<String,String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case CLASS_NAME:
      return getClassName();

    case OPTIONS:
      return getOptions();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case CLASS_NAME:
      return isSetClassName();
    case OPTIONS:
      return isSetOptions();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CompactionStrategyConfig)
      return this.equals((CompactionStrategyConfig)that);
    return false;
  }

  public boolean equals(CompactionStrategyConfig that) {
    if (that == null)
      return false;

    boolean this_present_className = true && this.isSetClassName();
    boolean that_present_className = true && that.isSetClassName();
    if (this_present_className || that_present_className) {
      if (!(this_present_className && that_present_className))
        return false;
      if (!this.className.equals(that.className))
        return false;
    }

    boolean this_present_options = true && this.isSetOptions();
    boolean that_present_options = true && that.isSetOptions();
    if (this_present_options || that_present_options) {
      if (!(this_present_options && that_present_options))
        return false;
      if (!this.options.equals(that.options))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(CompactionStrategyConfig other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetClassName()).compareTo(other.isSetClassName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetClassName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.className, other.className);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetOptions()).compareTo(other.isSetOptions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOptions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.options, other.options);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CompactionStrategyConfig(");
    boolean first = true;

    sb.append("className:");
    if (this.className == null) {
      sb.append("null");
    } else {
      sb.append(this.className);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("options:");
    if (this.options == null) {
      sb.append("null");
    } else {
      sb.append(this.options);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class CompactionStrategyConfigStandardSchemeFactory implements SchemeFactory {
    public CompactionStrategyConfigStandardScheme getScheme() {
      return new CompactionStrategyConfigStandardScheme();
    }
  }

  private static class CompactionStrategyConfigStandardScheme extends StandardScheme<CompactionStrategyConfig> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CompactionStrategyConfig struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // CLASS_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.className = iprot.readString();
              struct.setClassNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // OPTIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
              {
                org.apache.thrift.protocol.TMap _map154 = iprot.readMapBegin();
                struct.options = new HashMap<String,String>(2*_map154.size);
                for (int _i155 = 0; _i155 < _map154.size; ++_i155)
                {
                  String _key156;
                  String _val157;
                  _key156 = iprot.readString();
                  _val157 = iprot.readString();
                  struct.options.put(_key156, _val157);
                }
                iprot.readMapEnd();
              }
              struct.setOptionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, CompactionStrategyConfig struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.className != null) {
        oprot.writeFieldBegin(CLASS_NAME_FIELD_DESC);
        oprot.writeString(struct.className);
        oprot.writeFieldEnd();
      }
      if (struct.options != null) {
        oprot.writeFieldBegin(OPTIONS_FIELD_DESC);
        {
          oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, struct.options.size()));
          for (Map.Entry<String, String> _iter158 : struct.options.entrySet())
          {
            oprot.writeString(_iter158.getKey());
            oprot.writeString(_iter158.getValue());
          }
          oprot.writeMapEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CompactionStrategyConfigTupleSchemeFactory implements SchemeFactory {
    public CompactionStrategyConfigTupleScheme getScheme() {
      return new CompactionStrategyConfigTupleScheme();
    }
  }

  private static class CompactionStrategyConfigTupleScheme extends TupleScheme<CompactionStrategyConfig> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CompactionStrategyConfig struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetClassName()) {
        optionals.set(0);
      }
      if (struct.isSetOptions()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetClassName()) {
        oprot.writeString(struct.className);
      }
      if (struct.isSetOptions()) {
        {
          oprot.writeI32(struct.options.size());
          for (Map.Entry<String, String> _iter159 : struct.options.entrySet())
          {
            oprot.writeString(_iter159.getKey());
            oprot.writeString(_iter159.getValue());
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CompactionStrategyConfig struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.className = iprot.readString();
        struct.setClassNameIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TMap _map160 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.options = new HashMap<String,String>(2*_map160.size);
          for (int _i161 = 0; _i161 < _map160.size; ++_i161)
          {
            String _key162;
            String _val163;
            _key162 = iprot.readString();
            _val163 = iprot.readString();
            struct.options.put(_key162, _val163);
          }
        }
        struct.setOptionsIsSet(true);
      }
    }
  }

}

diff --git a/proxy/src/main/python/AccumuloProxy-remote b/proxy/src/main/python/AccumuloProxy-remote
index b4292a2a0..a8d754212 100644
-- a/proxy/src/main/python/AccumuloProxy-remote
++ b/proxy/src/main/python/AccumuloProxy-remote
@@ -44,7 +44,7 @@ if len(sys.argv) <= 1 or sys.argv[1] == '--help':
   print '  void checkIteratorConflicts(string login, string tableName, IteratorSetting setting,  scopes)'
   print '  void clearLocatorCache(string login, string tableName)'
   print '  void cloneTable(string login, string tableName, string newTableName, bool flush,  propertiesToSet,  propertiesToExclude)'
  print '  void compactTable(string login, string tableName, string startRow, string endRow,  iterators, bool flush, bool wait)'
  print '  void compactTable(string login, string tableName, string startRow, string endRow,  iterators, bool flush, bool wait, CompactionStrategyConfig compactionStrategy)'
   print '  void cancelCompaction(string login, string tableName)'
   print '  void createTable(string login, string tableName, bool versioningIter, TimeType type)'
   print '  void deleteTable(string login, string tableName)'
@@ -208,10 +208,10 @@ elif cmd == 'cloneTable':
   pp.pprint(client.cloneTable(args[0],args[1],args[2],eval(args[3]),eval(args[4]),eval(args[5]),))
 
 elif cmd == 'compactTable':
  if len(args) != 7:
    print 'compactTable requires 7 args'
  if len(args) != 8:
    print 'compactTable requires 8 args'
     sys.exit(1)
  pp.pprint(client.compactTable(args[0],args[1],args[2],args[3],eval(args[4]),eval(args[5]),eval(args[6]),))
  pp.pprint(client.compactTable(args[0],args[1],args[2],args[3],eval(args[4]),eval(args[5]),eval(args[6]),eval(args[7]),))
 
 elif cmd == 'cancelCompaction':
   if len(args) != 2:
diff --git a/proxy/src/main/python/AccumuloProxy.py b/proxy/src/main/python/AccumuloProxy.py
index 37dda0fb5..2805fff95 100644
-- a/proxy/src/main/python/AccumuloProxy.py
++ b/proxy/src/main/python/AccumuloProxy.py
@@ -98,7 +98,7 @@ class Iface:
     """
     pass
 
  def compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait):
  def compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy):
     """
     Parameters:
      - login
@@ -108,6 +108,7 @@ class Iface:
      - iterators
      - flush
      - wait
     - compactionStrategy
     """
     pass
 
@@ -986,7 +987,7 @@ class Client(Iface):
       raise result.ouch4
     return
 
  def compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait):
  def compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy):
     """
     Parameters:
      - login
@@ -996,11 +997,12 @@ class Client(Iface):
      - iterators
      - flush
      - wait
     - compactionStrategy
     """
    self.send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait)
    self.send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy)
     self.recv_compactTable()
 
  def send_compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait):
  def send_compactTable(self, login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy):
     self._oprot.writeMessageBegin('compactTable', TMessageType.CALL, self._seqid)
     args = compactTable_args()
     args.login = login
@@ -1010,6 +1012,7 @@ class Client(Iface):
     args.iterators = iterators
     args.flush = flush
     args.wait = wait
    args.compactionStrategy = compactionStrategy
     args.write(self._oprot)
     self._oprot.writeMessageEnd()
     self._oprot.trans.flush()
@@ -3796,7 +3799,7 @@ class Processor(Iface, TProcessor):
     iprot.readMessageEnd()
     result = compactTable_result()
     try:
      self._handler.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait)
      self._handler.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait, args.compactionStrategy)
     except AccumuloSecurityException, ouch1:
       result.ouch1 = ouch1
     except TableNotFoundException, ouch2:
@@ -4985,11 +4988,11 @@ class login_args:
       elif fid == 2:
         if ftype == TType.MAP:
           self.loginProperties = {}
          (_ktype136, _vtype137, _size135 ) = iprot.readMapBegin()
          for _i139 in xrange(_size135):
            _key140 = iprot.readString();
            _val141 = iprot.readString();
            self.loginProperties[_key140] = _val141
          (_ktype145, _vtype146, _size144 ) = iprot.readMapBegin()
          for _i148 in xrange(_size144):
            _key149 = iprot.readString();
            _val150 = iprot.readString();
            self.loginProperties[_key149] = _val150
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -5010,9 +5013,9 @@ class login_args:
     if self.loginProperties is not None:
       oprot.writeFieldBegin('loginProperties', TType.MAP, 2)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.loginProperties))
      for kiter142,viter143 in self.loginProperties.items():
        oprot.writeString(kiter142)
        oprot.writeString(viter143)
      for kiter151,viter152 in self.loginProperties.items():
        oprot.writeString(kiter151)
        oprot.writeString(viter152)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -5329,10 +5332,10 @@ class addSplits_args:
       elif fid == 3:
         if ftype == TType.SET:
           self.splits = set()
          (_etype147, _size144) = iprot.readSetBegin()
          for _i148 in xrange(_size144):
            _elem149 = iprot.readString();
            self.splits.add(_elem149)
          (_etype156, _size153) = iprot.readSetBegin()
          for _i157 in xrange(_size153):
            _elem158 = iprot.readString();
            self.splits.add(_elem158)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -5357,8 +5360,8 @@ class addSplits_args:
     if self.splits is not None:
       oprot.writeFieldBegin('splits', TType.SET, 3)
       oprot.writeSetBegin(TType.STRING, len(self.splits))
      for iter150 in self.splits:
        oprot.writeString(iter150)
      for iter159 in self.splits:
        oprot.writeString(iter159)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -5517,10 +5520,10 @@ class attachIterator_args:
       elif fid == 4:
         if ftype == TType.SET:
           self.scopes = set()
          (_etype154, _size151) = iprot.readSetBegin()
          for _i155 in xrange(_size151):
            _elem156 = iprot.readI32();
            self.scopes.add(_elem156)
          (_etype163, _size160) = iprot.readSetBegin()
          for _i164 in xrange(_size160):
            _elem165 = iprot.readI32();
            self.scopes.add(_elem165)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -5549,8 +5552,8 @@ class attachIterator_args:
     if self.scopes is not None:
       oprot.writeFieldBegin('scopes', TType.SET, 4)
       oprot.writeSetBegin(TType.I32, len(self.scopes))
      for iter157 in self.scopes:
        oprot.writeI32(iter157)
      for iter166 in self.scopes:
        oprot.writeI32(iter166)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -5709,10 +5712,10 @@ class checkIteratorConflicts_args:
       elif fid == 4:
         if ftype == TType.SET:
           self.scopes = set()
          (_etype161, _size158) = iprot.readSetBegin()
          for _i162 in xrange(_size158):
            _elem163 = iprot.readI32();
            self.scopes.add(_elem163)
          (_etype170, _size167) = iprot.readSetBegin()
          for _i171 in xrange(_size167):
            _elem172 = iprot.readI32();
            self.scopes.add(_elem172)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -5741,8 +5744,8 @@ class checkIteratorConflicts_args:
     if self.scopes is not None:
       oprot.writeFieldBegin('scopes', TType.SET, 4)
       oprot.writeSetBegin(TType.I32, len(self.scopes))
      for iter164 in self.scopes:
        oprot.writeI32(iter164)
      for iter173 in self.scopes:
        oprot.writeI32(iter173)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -6044,21 +6047,21 @@ class cloneTable_args:
       elif fid == 5:
         if ftype == TType.MAP:
           self.propertiesToSet = {}
          (_ktype166, _vtype167, _size165 ) = iprot.readMapBegin()
          for _i169 in xrange(_size165):
            _key170 = iprot.readString();
            _val171 = iprot.readString();
            self.propertiesToSet[_key170] = _val171
          (_ktype175, _vtype176, _size174 ) = iprot.readMapBegin()
          for _i178 in xrange(_size174):
            _key179 = iprot.readString();
            _val180 = iprot.readString();
            self.propertiesToSet[_key179] = _val180
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
       elif fid == 6:
         if ftype == TType.SET:
           self.propertiesToExclude = set()
          (_etype175, _size172) = iprot.readSetBegin()
          for _i176 in xrange(_size172):
            _elem177 = iprot.readString();
            self.propertiesToExclude.add(_elem177)
          (_etype184, _size181) = iprot.readSetBegin()
          for _i185 in xrange(_size181):
            _elem186 = iprot.readString();
            self.propertiesToExclude.add(_elem186)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -6091,16 +6094,16 @@ class cloneTable_args:
     if self.propertiesToSet is not None:
       oprot.writeFieldBegin('propertiesToSet', TType.MAP, 5)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.propertiesToSet))
      for kiter178,viter179 in self.propertiesToSet.items():
        oprot.writeString(kiter178)
        oprot.writeString(viter179)
      for kiter187,viter188 in self.propertiesToSet.items():
        oprot.writeString(kiter187)
        oprot.writeString(viter188)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.propertiesToExclude is not None:
       oprot.writeFieldBegin('propertiesToExclude', TType.SET, 6)
       oprot.writeSetBegin(TType.STRING, len(self.propertiesToExclude))
      for iter180 in self.propertiesToExclude:
        oprot.writeString(iter180)
      for iter189 in self.propertiesToExclude:
        oprot.writeString(iter189)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -6231,6 +6234,7 @@ class compactTable_args:
    - iterators
    - flush
    - wait
   - compactionStrategy
   """
 
   thrift_spec = (
@@ -6242,9 +6246,10 @@ class compactTable_args:
     (5, TType.LIST, 'iterators', (TType.STRUCT,(IteratorSetting, IteratorSetting.thrift_spec)), None, ), # 5
     (6, TType.BOOL, 'flush', None, None, ), # 6
     (7, TType.BOOL, 'wait', None, None, ), # 7
    (8, TType.STRUCT, 'compactionStrategy', (CompactionStrategyConfig, CompactionStrategyConfig.thrift_spec), None, ), # 8
   )
 
  def __init__(self, login=None, tableName=None, startRow=None, endRow=None, iterators=None, flush=None, wait=None,):
  def __init__(self, login=None, tableName=None, startRow=None, endRow=None, iterators=None, flush=None, wait=None, compactionStrategy=None,):
     self.login = login
     self.tableName = tableName
     self.startRow = startRow
@@ -6252,6 +6257,7 @@ class compactTable_args:
     self.iterators = iterators
     self.flush = flush
     self.wait = wait
    self.compactionStrategy = compactionStrategy
 
   def read(self, iprot):
     if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
@@ -6285,11 +6291,11 @@ class compactTable_args:
       elif fid == 5:
         if ftype == TType.LIST:
           self.iterators = []
          (_etype184, _size181) = iprot.readListBegin()
          for _i185 in xrange(_size181):
            _elem186 = IteratorSetting()
            _elem186.read(iprot)
            self.iterators.append(_elem186)
          (_etype193, _size190) = iprot.readListBegin()
          for _i194 in xrange(_size190):
            _elem195 = IteratorSetting()
            _elem195.read(iprot)
            self.iterators.append(_elem195)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -6303,6 +6309,12 @@ class compactTable_args:
           self.wait = iprot.readBool();
         else:
           iprot.skip(ftype)
      elif fid == 8:
        if ftype == TType.STRUCT:
          self.compactionStrategy = CompactionStrategyConfig()
          self.compactionStrategy.read(iprot)
        else:
          iprot.skip(ftype)
       else:
         iprot.skip(ftype)
       iprot.readFieldEnd()
@@ -6332,8 +6344,8 @@ class compactTable_args:
     if self.iterators is not None:
       oprot.writeFieldBegin('iterators', TType.LIST, 5)
       oprot.writeListBegin(TType.STRUCT, len(self.iterators))
      for iter187 in self.iterators:
        iter187.write(oprot)
      for iter196 in self.iterators:
        iter196.write(oprot)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.flush is not None:
@@ -6344,6 +6356,10 @@ class compactTable_args:
       oprot.writeFieldBegin('wait', TType.BOOL, 7)
       oprot.writeBool(self.wait)
       oprot.writeFieldEnd()
    if self.compactionStrategy is not None:
      oprot.writeFieldBegin('compactionStrategy', TType.STRUCT, 8)
      self.compactionStrategy.write(oprot)
      oprot.writeFieldEnd()
     oprot.writeFieldStop()
     oprot.writeStructEnd()
 
@@ -7533,10 +7549,10 @@ class getDiskUsage_args:
       elif fid == 2:
         if ftype == TType.SET:
           self.tables = set()
          (_etype191, _size188) = iprot.readSetBegin()
          for _i192 in xrange(_size188):
            _elem193 = iprot.readString();
            self.tables.add(_elem193)
          (_etype200, _size197) = iprot.readSetBegin()
          for _i201 in xrange(_size197):
            _elem202 = iprot.readString();
            self.tables.add(_elem202)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -7557,8 +7573,8 @@ class getDiskUsage_args:
     if self.tables is not None:
       oprot.writeFieldBegin('tables', TType.SET, 2)
       oprot.writeSetBegin(TType.STRING, len(self.tables))
      for iter194 in self.tables:
        oprot.writeString(iter194)
      for iter203 in self.tables:
        oprot.writeString(iter203)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -7613,11 +7629,11 @@ class getDiskUsage_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype198, _size195) = iprot.readListBegin()
          for _i199 in xrange(_size195):
            _elem200 = DiskUsage()
            _elem200.read(iprot)
            self.success.append(_elem200)
          (_etype207, _size204) = iprot.readListBegin()
          for _i208 in xrange(_size204):
            _elem209 = DiskUsage()
            _elem209.read(iprot)
            self.success.append(_elem209)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -7652,8 +7668,8 @@ class getDiskUsage_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRUCT, len(self.success))
      for iter201 in self.success:
        iter201.write(oprot)
      for iter210 in self.success:
        iter210.write(oprot)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -7792,16 +7808,16 @@ class getLocalityGroups_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype203, _vtype204, _size202 ) = iprot.readMapBegin()
          for _i206 in xrange(_size202):
            _key207 = iprot.readString();
            _val208 = set()
            (_etype212, _size209) = iprot.readSetBegin()
            for _i213 in xrange(_size209):
              _elem214 = iprot.readString();
              _val208.add(_elem214)
          (_ktype212, _vtype213, _size211 ) = iprot.readMapBegin()
          for _i215 in xrange(_size211):
            _key216 = iprot.readString();
            _val217 = set()
            (_etype221, _size218) = iprot.readSetBegin()
            for _i222 in xrange(_size218):
              _elem223 = iprot.readString();
              _val217.add(_elem223)
             iprot.readSetEnd()
            self.success[_key207] = _val208
            self.success[_key216] = _val217
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -7836,11 +7852,11 @@ class getLocalityGroups_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.SET, len(self.success))
      for kiter215,viter216 in self.success.items():
        oprot.writeString(kiter215)
        oprot.writeSetBegin(TType.STRING, len(viter216))
        for iter217 in viter216:
          oprot.writeString(iter217)
      for kiter224,viter225 in self.success.items():
        oprot.writeString(kiter224)
        oprot.writeSetBegin(TType.STRING, len(viter225))
        for iter226 in viter225:
          oprot.writeString(iter226)
         oprot.writeSetEnd()
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
@@ -8123,10 +8139,10 @@ class getMaxRow_args:
       elif fid == 3:
         if ftype == TType.SET:
           self.auths = set()
          (_etype221, _size218) = iprot.readSetBegin()
          for _i222 in xrange(_size218):
            _elem223 = iprot.readString();
            self.auths.add(_elem223)
          (_etype230, _size227) = iprot.readSetBegin()
          for _i231 in xrange(_size227):
            _elem232 = iprot.readString();
            self.auths.add(_elem232)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -8171,8 +8187,8 @@ class getMaxRow_args:
     if self.auths is not None:
       oprot.writeFieldBegin('auths', TType.SET, 3)
       oprot.writeSetBegin(TType.STRING, len(self.auths))
      for iter224 in self.auths:
        oprot.writeString(iter224)
      for iter233 in self.auths:
        oprot.writeString(iter233)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     if self.startRow is not None:
@@ -8413,11 +8429,11 @@ class getTableProperties_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype226, _vtype227, _size225 ) = iprot.readMapBegin()
          for _i229 in xrange(_size225):
            _key230 = iprot.readString();
            _val231 = iprot.readString();
            self.success[_key230] = _val231
          (_ktype235, _vtype236, _size234 ) = iprot.readMapBegin()
          for _i238 in xrange(_size234):
            _key239 = iprot.readString();
            _val240 = iprot.readString();
            self.success[_key239] = _val240
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -8452,9 +8468,9 @@ class getTableProperties_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.success))
      for kiter232,viter233 in self.success.items():
        oprot.writeString(kiter232)
        oprot.writeString(viter233)
      for kiter241,viter242 in self.success.items():
        oprot.writeString(kiter241)
        oprot.writeString(viter242)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -8971,10 +8987,10 @@ class listSplits_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype237, _size234) = iprot.readListBegin()
          for _i238 in xrange(_size234):
            _elem239 = iprot.readString();
            self.success.append(_elem239)
          (_etype246, _size243) = iprot.readListBegin()
          for _i247 in xrange(_size243):
            _elem248 = iprot.readString();
            self.success.append(_elem248)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -9009,8 +9025,8 @@ class listSplits_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRING, len(self.success))
      for iter240 in self.success:
        oprot.writeString(iter240)
      for iter249 in self.success:
        oprot.writeString(iter249)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -9128,10 +9144,10 @@ class listTables_result:
       if fid == 0:
         if ftype == TType.SET:
           self.success = set()
          (_etype244, _size241) = iprot.readSetBegin()
          for _i245 in xrange(_size241):
            _elem246 = iprot.readString();
            self.success.add(_elem246)
          (_etype253, _size250) = iprot.readSetBegin()
          for _i254 in xrange(_size250):
            _elem255 = iprot.readString();
            self.success.add(_elem255)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -9148,8 +9164,8 @@ class listTables_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.SET, 0)
       oprot.writeSetBegin(TType.STRING, len(self.success))
      for iter247 in self.success:
        oprot.writeString(iter247)
      for iter256 in self.success:
        oprot.writeString(iter256)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -9276,16 +9292,16 @@ class listIterators_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype249, _vtype250, _size248 ) = iprot.readMapBegin()
          for _i252 in xrange(_size248):
            _key253 = iprot.readString();
            _val254 = set()
            (_etype258, _size255) = iprot.readSetBegin()
            for _i259 in xrange(_size255):
              _elem260 = iprot.readI32();
              _val254.add(_elem260)
          (_ktype258, _vtype259, _size257 ) = iprot.readMapBegin()
          for _i261 in xrange(_size257):
            _key262 = iprot.readString();
            _val263 = set()
            (_etype267, _size264) = iprot.readSetBegin()
            for _i268 in xrange(_size264):
              _elem269 = iprot.readI32();
              _val263.add(_elem269)
             iprot.readSetEnd()
            self.success[_key253] = _val254
            self.success[_key262] = _val263
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -9320,11 +9336,11 @@ class listIterators_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.SET, len(self.success))
      for kiter261,viter262 in self.success.items():
        oprot.writeString(kiter261)
        oprot.writeSetBegin(TType.I32, len(viter262))
        for iter263 in viter262:
          oprot.writeI32(iter263)
      for kiter270,viter271 in self.success.items():
        oprot.writeString(kiter270)
        oprot.writeSetBegin(TType.I32, len(viter271))
        for iter272 in viter271:
          oprot.writeI32(iter272)
         oprot.writeSetEnd()
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
@@ -9464,11 +9480,11 @@ class listConstraints_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype265, _vtype266, _size264 ) = iprot.readMapBegin()
          for _i268 in xrange(_size264):
            _key269 = iprot.readString();
            _val270 = iprot.readI32();
            self.success[_key269] = _val270
          (_ktype274, _vtype275, _size273 ) = iprot.readMapBegin()
          for _i277 in xrange(_size273):
            _key278 = iprot.readString();
            _val279 = iprot.readI32();
            self.success[_key278] = _val279
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -9503,9 +9519,9 @@ class listConstraints_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.I32, len(self.success))
      for kiter271,viter272 in self.success.items():
        oprot.writeString(kiter271)
        oprot.writeI32(viter272)
      for kiter280,viter281 in self.success.items():
        oprot.writeString(kiter280)
        oprot.writeI32(viter281)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -10284,10 +10300,10 @@ class removeIterator_args:
       elif fid == 4:
         if ftype == TType.SET:
           self.scopes = set()
          (_etype276, _size273) = iprot.readSetBegin()
          for _i277 in xrange(_size273):
            _elem278 = iprot.readI32();
            self.scopes.add(_elem278)
          (_etype285, _size282) = iprot.readSetBegin()
          for _i286 in xrange(_size282):
            _elem287 = iprot.readI32();
            self.scopes.add(_elem287)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -10316,8 +10332,8 @@ class removeIterator_args:
     if self.scopes is not None:
       oprot.writeFieldBegin('scopes', TType.SET, 4)
       oprot.writeSetBegin(TType.I32, len(self.scopes))
      for iter279 in self.scopes:
        oprot.writeI32(iter279)
      for iter288 in self.scopes:
        oprot.writeI32(iter288)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -10822,16 +10838,16 @@ class setLocalityGroups_args:
       elif fid == 3:
         if ftype == TType.MAP:
           self.groups = {}
          (_ktype281, _vtype282, _size280 ) = iprot.readMapBegin()
          for _i284 in xrange(_size280):
            _key285 = iprot.readString();
            _val286 = set()
            (_etype290, _size287) = iprot.readSetBegin()
            for _i291 in xrange(_size287):
              _elem292 = iprot.readString();
              _val286.add(_elem292)
          (_ktype290, _vtype291, _size289 ) = iprot.readMapBegin()
          for _i293 in xrange(_size289):
            _key294 = iprot.readString();
            _val295 = set()
            (_etype299, _size296) = iprot.readSetBegin()
            for _i300 in xrange(_size296):
              _elem301 = iprot.readString();
              _val295.add(_elem301)
             iprot.readSetEnd()
            self.groups[_key285] = _val286
            self.groups[_key294] = _val295
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -10856,11 +10872,11 @@ class setLocalityGroups_args:
     if self.groups is not None:
       oprot.writeFieldBegin('groups', TType.MAP, 3)
       oprot.writeMapBegin(TType.STRING, TType.SET, len(self.groups))
      for kiter293,viter294 in self.groups.items():
        oprot.writeString(kiter293)
        oprot.writeSetBegin(TType.STRING, len(viter294))
        for iter295 in viter294:
          oprot.writeString(iter295)
      for kiter302,viter303 in self.groups.items():
        oprot.writeString(kiter302)
        oprot.writeSetBegin(TType.STRING, len(viter303))
        for iter304 in viter303:
          oprot.writeString(iter304)
         oprot.writeSetEnd()
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
@@ -11283,11 +11299,11 @@ class splitRangeByTablets_result:
       if fid == 0:
         if ftype == TType.SET:
           self.success = set()
          (_etype299, _size296) = iprot.readSetBegin()
          for _i300 in xrange(_size296):
            _elem301 = Range()
            _elem301.read(iprot)
            self.success.add(_elem301)
          (_etype308, _size305) = iprot.readSetBegin()
          for _i309 in xrange(_size305):
            _elem310 = Range()
            _elem310.read(iprot)
            self.success.add(_elem310)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -11322,8 +11338,8 @@ class splitRangeByTablets_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.SET, 0)
       oprot.writeSetBegin(TType.STRUCT, len(self.success))
      for iter302 in self.success:
        iter302.write(oprot)
      for iter311 in self.success:
        iter311.write(oprot)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -11572,11 +11588,11 @@ class tableIdMap_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype304, _vtype305, _size303 ) = iprot.readMapBegin()
          for _i307 in xrange(_size303):
            _key308 = iprot.readString();
            _val309 = iprot.readString();
            self.success[_key308] = _val309
          (_ktype313, _vtype314, _size312 ) = iprot.readMapBegin()
          for _i316 in xrange(_size312):
            _key317 = iprot.readString();
            _val318 = iprot.readString();
            self.success[_key317] = _val318
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -11593,9 +11609,9 @@ class tableIdMap_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.success))
      for kiter310,viter311 in self.success.items():
        oprot.writeString(kiter310)
        oprot.writeString(viter311)
      for kiter319,viter320 in self.success.items():
        oprot.writeString(kiter319)
        oprot.writeString(viter320)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -12059,11 +12075,11 @@ class getActiveScans_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype315, _size312) = iprot.readListBegin()
          for _i316 in xrange(_size312):
            _elem317 = ActiveScan()
            _elem317.read(iprot)
            self.success.append(_elem317)
          (_etype324, _size321) = iprot.readListBegin()
          for _i325 in xrange(_size321):
            _elem326 = ActiveScan()
            _elem326.read(iprot)
            self.success.append(_elem326)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -12092,8 +12108,8 @@ class getActiveScans_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRUCT, len(self.success))
      for iter318 in self.success:
        iter318.write(oprot)
      for iter327 in self.success:
        iter327.write(oprot)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -12225,11 +12241,11 @@ class getActiveCompactions_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype322, _size319) = iprot.readListBegin()
          for _i323 in xrange(_size319):
            _elem324 = ActiveCompaction()
            _elem324.read(iprot)
            self.success.append(_elem324)
          (_etype331, _size328) = iprot.readListBegin()
          for _i332 in xrange(_size328):
            _elem333 = ActiveCompaction()
            _elem333.read(iprot)
            self.success.append(_elem333)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -12258,8 +12274,8 @@ class getActiveCompactions_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRUCT, len(self.success))
      for iter325 in self.success:
        iter325.write(oprot)
      for iter334 in self.success:
        iter334.write(oprot)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -12379,11 +12395,11 @@ class getSiteConfiguration_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype327, _vtype328, _size326 ) = iprot.readMapBegin()
          for _i330 in xrange(_size326):
            _key331 = iprot.readString();
            _val332 = iprot.readString();
            self.success[_key331] = _val332
          (_ktype336, _vtype337, _size335 ) = iprot.readMapBegin()
          for _i339 in xrange(_size335):
            _key340 = iprot.readString();
            _val341 = iprot.readString();
            self.success[_key340] = _val341
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -12412,9 +12428,9 @@ class getSiteConfiguration_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.success))
      for kiter333,viter334 in self.success.items():
        oprot.writeString(kiter333)
        oprot.writeString(viter334)
      for kiter342,viter343 in self.success.items():
        oprot.writeString(kiter342)
        oprot.writeString(viter343)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -12534,11 +12550,11 @@ class getSystemConfiguration_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype336, _vtype337, _size335 ) = iprot.readMapBegin()
          for _i339 in xrange(_size335):
            _key340 = iprot.readString();
            _val341 = iprot.readString();
            self.success[_key340] = _val341
          (_ktype345, _vtype346, _size344 ) = iprot.readMapBegin()
          for _i348 in xrange(_size344):
            _key349 = iprot.readString();
            _val350 = iprot.readString();
            self.success[_key349] = _val350
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -12567,9 +12583,9 @@ class getSystemConfiguration_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.success))
      for kiter342,viter343 in self.success.items():
        oprot.writeString(kiter342)
        oprot.writeString(viter343)
      for kiter351,viter352 in self.success.items():
        oprot.writeString(kiter351)
        oprot.writeString(viter352)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -12683,10 +12699,10 @@ class getTabletServers_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype347, _size344) = iprot.readListBegin()
          for _i348 in xrange(_size344):
            _elem349 = iprot.readString();
            self.success.append(_elem349)
          (_etype356, _size353) = iprot.readListBegin()
          for _i357 in xrange(_size353):
            _elem358 = iprot.readString();
            self.success.append(_elem358)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -12703,8 +12719,8 @@ class getTabletServers_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRING, len(self.success))
      for iter350 in self.success:
        oprot.writeString(iter350)
      for iter359 in self.success:
        oprot.writeString(iter359)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -13240,11 +13256,11 @@ class authenticateUser_args:
       elif fid == 3:
         if ftype == TType.MAP:
           self.properties = {}
          (_ktype352, _vtype353, _size351 ) = iprot.readMapBegin()
          for _i355 in xrange(_size351):
            _key356 = iprot.readString();
            _val357 = iprot.readString();
            self.properties[_key356] = _val357
          (_ktype361, _vtype362, _size360 ) = iprot.readMapBegin()
          for _i364 in xrange(_size360):
            _key365 = iprot.readString();
            _val366 = iprot.readString();
            self.properties[_key365] = _val366
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -13269,9 +13285,9 @@ class authenticateUser_args:
     if self.properties is not None:
       oprot.writeFieldBegin('properties', TType.MAP, 3)
       oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.properties))
      for kiter358,viter359 in self.properties.items():
        oprot.writeString(kiter358)
        oprot.writeString(viter359)
      for kiter367,viter368 in self.properties.items():
        oprot.writeString(kiter367)
        oprot.writeString(viter368)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -13419,10 +13435,10 @@ class changeUserAuthorizations_args:
       elif fid == 3:
         if ftype == TType.SET:
           self.authorizations = set()
          (_etype363, _size360) = iprot.readSetBegin()
          for _i364 in xrange(_size360):
            _elem365 = iprot.readString();
            self.authorizations.add(_elem365)
          (_etype372, _size369) = iprot.readSetBegin()
          for _i373 in xrange(_size369):
            _elem374 = iprot.readString();
            self.authorizations.add(_elem374)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -13447,8 +13463,8 @@ class changeUserAuthorizations_args:
     if self.authorizations is not None:
       oprot.writeFieldBegin('authorizations', TType.SET, 3)
       oprot.writeSetBegin(TType.STRING, len(self.authorizations))
      for iter366 in self.authorizations:
        oprot.writeString(iter366)
      for iter375 in self.authorizations:
        oprot.writeString(iter375)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -14108,10 +14124,10 @@ class getUserAuthorizations_result:
       if fid == 0:
         if ftype == TType.LIST:
           self.success = []
          (_etype370, _size367) = iprot.readListBegin()
          for _i371 in xrange(_size367):
            _elem372 = iprot.readString();
            self.success.append(_elem372)
          (_etype379, _size376) = iprot.readListBegin()
          for _i380 in xrange(_size376):
            _elem381 = iprot.readString();
            self.success.append(_elem381)
           iprot.readListEnd()
         else:
           iprot.skip(ftype)
@@ -14140,8 +14156,8 @@ class getUserAuthorizations_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.LIST, 0)
       oprot.writeListBegin(TType.STRING, len(self.success))
      for iter373 in self.success:
        oprot.writeString(iter373)
      for iter382 in self.success:
        oprot.writeString(iter382)
       oprot.writeListEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -14968,10 +14984,10 @@ class listLocalUsers_result:
       if fid == 0:
         if ftype == TType.SET:
           self.success = set()
          (_etype377, _size374) = iprot.readSetBegin()
          for _i378 in xrange(_size374):
            _elem379 = iprot.readString();
            self.success.add(_elem379)
          (_etype386, _size383) = iprot.readSetBegin()
          for _i387 in xrange(_size383):
            _elem388 = iprot.readString();
            self.success.add(_elem388)
           iprot.readSetEnd()
         else:
           iprot.skip(ftype)
@@ -15006,8 +15022,8 @@ class listLocalUsers_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.SET, 0)
       oprot.writeSetBegin(TType.STRING, len(self.success))
      for iter380 in self.success:
        oprot.writeString(iter380)
      for iter389 in self.success:
        oprot.writeString(iter389)
       oprot.writeSetEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
@@ -16372,17 +16388,17 @@ class updateAndFlush_args:
       elif fid == 3:
         if ftype == TType.MAP:
           self.cells = {}
          (_ktype382, _vtype383, _size381 ) = iprot.readMapBegin()
          for _i385 in xrange(_size381):
            _key386 = iprot.readString();
            _val387 = []
            (_etype391, _size388) = iprot.readListBegin()
            for _i392 in xrange(_size388):
              _elem393 = ColumnUpdate()
              _elem393.read(iprot)
              _val387.append(_elem393)
          (_ktype391, _vtype392, _size390 ) = iprot.readMapBegin()
          for _i394 in xrange(_size390):
            _key395 = iprot.readString();
            _val396 = []
            (_etype400, _size397) = iprot.readListBegin()
            for _i401 in xrange(_size397):
              _elem402 = ColumnUpdate()
              _elem402.read(iprot)
              _val396.append(_elem402)
             iprot.readListEnd()
            self.cells[_key386] = _val387
            self.cells[_key395] = _val396
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -16407,11 +16423,11 @@ class updateAndFlush_args:
     if self.cells is not None:
       oprot.writeFieldBegin('cells', TType.MAP, 3)
       oprot.writeMapBegin(TType.STRING, TType.LIST, len(self.cells))
      for kiter394,viter395 in self.cells.items():
        oprot.writeString(kiter394)
        oprot.writeListBegin(TType.STRUCT, len(viter395))
        for iter396 in viter395:
          iter396.write(oprot)
      for kiter403,viter404 in self.cells.items():
        oprot.writeString(kiter403)
        oprot.writeListBegin(TType.STRUCT, len(viter404))
        for iter405 in viter404:
          iter405.write(oprot)
         oprot.writeListEnd()
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
@@ -16750,17 +16766,17 @@ class update_args:
       elif fid == 2:
         if ftype == TType.MAP:
           self.cells = {}
          (_ktype398, _vtype399, _size397 ) = iprot.readMapBegin()
          for _i401 in xrange(_size397):
            _key402 = iprot.readString();
            _val403 = []
            (_etype407, _size404) = iprot.readListBegin()
            for _i408 in xrange(_size404):
              _elem409 = ColumnUpdate()
              _elem409.read(iprot)
              _val403.append(_elem409)
          (_ktype407, _vtype408, _size406 ) = iprot.readMapBegin()
          for _i410 in xrange(_size406):
            _key411 = iprot.readString();
            _val412 = []
            (_etype416, _size413) = iprot.readListBegin()
            for _i417 in xrange(_size413):
              _elem418 = ColumnUpdate()
              _elem418.read(iprot)
              _val412.append(_elem418)
             iprot.readListEnd()
            self.cells[_key402] = _val403
            self.cells[_key411] = _val412
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -16781,11 +16797,11 @@ class update_args:
     if self.cells is not None:
       oprot.writeFieldBegin('cells', TType.MAP, 2)
       oprot.writeMapBegin(TType.STRING, TType.LIST, len(self.cells))
      for kiter410,viter411 in self.cells.items():
        oprot.writeString(kiter410)
        oprot.writeListBegin(TType.STRUCT, len(viter411))
        for iter412 in viter411:
          iter412.write(oprot)
      for kiter419,viter420 in self.cells.items():
        oprot.writeString(kiter419)
        oprot.writeListBegin(TType.STRUCT, len(viter420))
        for iter421 in viter420:
          iter421.write(oprot)
         oprot.writeListEnd()
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
@@ -17487,12 +17503,12 @@ class updateRowsConditionally_args:
       elif fid == 2:
         if ftype == TType.MAP:
           self.updates = {}
          (_ktype414, _vtype415, _size413 ) = iprot.readMapBegin()
          for _i417 in xrange(_size413):
            _key418 = iprot.readString();
            _val419 = ConditionalUpdates()
            _val419.read(iprot)
            self.updates[_key418] = _val419
          (_ktype423, _vtype424, _size422 ) = iprot.readMapBegin()
          for _i426 in xrange(_size422):
            _key427 = iprot.readString();
            _val428 = ConditionalUpdates()
            _val428.read(iprot)
            self.updates[_key427] = _val428
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -17513,9 +17529,9 @@ class updateRowsConditionally_args:
     if self.updates is not None:
       oprot.writeFieldBegin('updates', TType.MAP, 2)
       oprot.writeMapBegin(TType.STRING, TType.STRUCT, len(self.updates))
      for kiter420,viter421 in self.updates.items():
        oprot.writeString(kiter420)
        viter421.write(oprot)
      for kiter429,viter430 in self.updates.items():
        oprot.writeString(kiter429)
        viter430.write(oprot)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     oprot.writeFieldStop()
@@ -17570,11 +17586,11 @@ class updateRowsConditionally_result:
       if fid == 0:
         if ftype == TType.MAP:
           self.success = {}
          (_ktype423, _vtype424, _size422 ) = iprot.readMapBegin()
          for _i426 in xrange(_size422):
            _key427 = iprot.readString();
            _val428 = iprot.readI32();
            self.success[_key427] = _val428
          (_ktype432, _vtype433, _size431 ) = iprot.readMapBegin()
          for _i435 in xrange(_size431):
            _key436 = iprot.readString();
            _val437 = iprot.readI32();
            self.success[_key436] = _val437
           iprot.readMapEnd()
         else:
           iprot.skip(ftype)
@@ -17609,9 +17625,9 @@ class updateRowsConditionally_result:
     if self.success is not None:
       oprot.writeFieldBegin('success', TType.MAP, 0)
       oprot.writeMapBegin(TType.STRING, TType.I32, len(self.success))
      for kiter429,viter430 in self.success.items():
        oprot.writeString(kiter429)
        oprot.writeI32(viter430)
      for kiter438,viter439 in self.success.items():
        oprot.writeString(kiter438)
        oprot.writeI32(viter439)
       oprot.writeMapEnd()
       oprot.writeFieldEnd()
     if self.ouch1 is not None:
diff --git a/proxy/src/main/python/ttypes.py b/proxy/src/main/python/ttypes.py
index 5e4c001e6..9444f715e 100644
-- a/proxy/src/main/python/ttypes.py
++ b/proxy/src/main/python/ttypes.py
@@ -2337,6 +2337,88 @@ class WriterOptions:
   def __ne__(self, other):
     return not (self == other)
 
class CompactionStrategyConfig:
  """
  Attributes:
   - className
   - options
  """

  thrift_spec = (
    None, # 0
    (1, TType.STRING, 'className', None, None, ), # 1
    (2, TType.MAP, 'options', (TType.STRING,None,TType.STRING,None), None, ), # 2
  )

  def __init__(self, className=None, options=None,):
    self.className = className
    self.options = options

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.STRING:
          self.className = iprot.readString();
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.MAP:
          self.options = {}
          (_ktype136, _vtype137, _size135 ) = iprot.readMapBegin()
          for _i139 in xrange(_size135):
            _key140 = iprot.readString();
            _val141 = iprot.readString();
            self.options[_key140] = _val141
          iprot.readMapEnd()
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('CompactionStrategyConfig')
    if self.className is not None:
      oprot.writeFieldBegin('className', TType.STRING, 1)
      oprot.writeString(self.className)
      oprot.writeFieldEnd()
    if self.options is not None:
      oprot.writeFieldBegin('options', TType.MAP, 2)
      oprot.writeMapBegin(TType.STRING, TType.STRING, len(self.options))
      for kiter142,viter143 in self.options.items():
        oprot.writeString(kiter142)
        oprot.writeString(viter143)
      oprot.writeMapEnd()
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def validate(self):
    return


  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

 class UnknownScanner(TException):
   """
   Attributes:
diff --git a/proxy/src/main/ruby/accumulo_proxy.rb b/proxy/src/main/ruby/accumulo_proxy.rb
index 16f704259..f8d892e53 100644
-- a/proxy/src/main/ruby/accumulo_proxy.rb
++ b/proxy/src/main/ruby/accumulo_proxy.rb
@@ -144,13 +144,13 @@ module Accumulo
         return
       end
 
      def compactTable(login, tableName, startRow, endRow, iterators, flush, wait)
        send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait)
      def compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy)
        send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy)
         recv_compactTable()
       end
 
      def send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait)
        send_message('compactTable', CompactTable_args, :login => login, :tableName => tableName, :startRow => startRow, :endRow => endRow, :iterators => iterators, :flush => flush, :wait => wait)
      def send_compactTable(login, tableName, startRow, endRow, iterators, flush, wait, compactionStrategy)
        send_message('compactTable', CompactTable_args, :login => login, :tableName => tableName, :startRow => startRow, :endRow => endRow, :iterators => iterators, :flush => flush, :wait => wait, :compactionStrategy => compactionStrategy)
       end
 
       def recv_compactTable()
@@ -1425,7 +1425,7 @@ module Accumulo
         args = read_args(iprot, CompactTable_args)
         result = CompactTable_result.new()
         begin
          @handler.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait)
          @handler.compactTable(args.login, args.tableName, args.startRow, args.endRow, args.iterators, args.flush, args.wait, args.compactionStrategy)
         rescue ::Accumulo::AccumuloSecurityException => ouch1
           result.ouch1 = ouch1
         rescue ::Accumulo::TableNotFoundException => ouch2
@@ -2661,6 +2661,7 @@ module Accumulo
       ITERATORS = 5
       FLUSH = 6
       WAIT = 7
      COMPACTIONSTRATEGY = 8
 
       FIELDS = {
         LOGIN => {:type => ::Thrift::Types::STRING, :name => 'login', :binary => true},
@@ -2669,7 +2670,8 @@ module Accumulo
         ENDROW => {:type => ::Thrift::Types::STRING, :name => 'endRow', :binary => true},
         ITERATORS => {:type => ::Thrift::Types::LIST, :name => 'iterators', :element => {:type => ::Thrift::Types::STRUCT, :class => ::Accumulo::IteratorSetting}},
         FLUSH => {:type => ::Thrift::Types::BOOL, :name => 'flush'},
        WAIT => {:type => ::Thrift::Types::BOOL, :name => 'wait'}
        WAIT => {:type => ::Thrift::Types::BOOL, :name => 'wait'},
        COMPACTIONSTRATEGY => {:type => ::Thrift::Types::STRUCT, :name => 'compactionStrategy', :class => ::Accumulo::CompactionStrategyConfig}
       }
 
       def struct_fields; FIELDS; end
diff --git a/proxy/src/main/ruby/proxy_types.rb b/proxy/src/main/ruby/proxy_types.rb
index beeeee4d3..57306d179 100644
-- a/proxy/src/main/ruby/proxy_types.rb
++ b/proxy/src/main/ruby/proxy_types.rb
@@ -573,6 +573,24 @@ module Accumulo
     ::Thrift::Struct.generate_accessors self
   end
 
  class CompactionStrategyConfig
    include ::Thrift::Struct, ::Thrift::Struct_Union
    CLASSNAME = 1
    OPTIONS = 2

    FIELDS = {
      CLASSNAME => {:type => ::Thrift::Types::STRING, :name => 'className'},
      OPTIONS => {:type => ::Thrift::Types::MAP, :name => 'options', :key => {:type => ::Thrift::Types::STRING}, :value => {:type => ::Thrift::Types::STRING}}
    }

    def struct_fields; FIELDS; end

    def validate
    end

    ::Thrift::Struct.generate_accessors self
  end

   class UnknownScanner < ::Thrift::Exception
     include ::Thrift::Struct, ::Thrift::Struct_Union
     def initialize(message=nil)
diff --git a/proxy/src/main/thrift/proxy.thrift b/proxy/src/main/thrift/proxy.thrift
index fbd9c5289..25510d1f2 100644
-- a/proxy/src/main/thrift/proxy.thrift
++ b/proxy/src/main/thrift/proxy.thrift
@@ -249,6 +249,11 @@ struct WriterOptions {
  5:optional Durability durability
 }
 
struct CompactionStrategyConfig {
  1:string className
  2:map<string,string> options
}

 enum IteratorScope {
   MINC,
   MAJC,
@@ -310,8 +315,10 @@ service AccumuloProxy
   void cloneTable (1:binary login, 2:string tableName, 3:string newTableName, 4:bool flush, 
                    5:map<string,string> propertiesToSet, 6:set<string> propertiesToExclude) 
                                                                                                        throws (1:AccumuloException ouch1, 2:AccumuloSecurityException ouch2, 3:TableNotFoundException ouch3, 4:TableExistsException ouch4);
  //changed in 1.7.0, see comment at top about compatibility
   void compactTable (1:binary login, 2:string tableName, 3:binary startRow, 4:binary endRow, 
		     5:list<IteratorSetting> iterators, 6:bool flush, 7:bool wait)                             throws (1:AccumuloSecurityException ouch1, 2:TableNotFoundException ouch2, 3:AccumuloException ouch3);
		     5:list<IteratorSetting> iterators, 6:bool flush, 7:bool wait, 
		     8:CompactionStrategyConfig compactionStrategy)                                            throws (1:AccumuloSecurityException ouch1, 2:TableNotFoundException ouch2, 3:AccumuloException ouch3);
   void cancelCompaction(1:binary login, 2:string tableName)                                            throws (1:AccumuloSecurityException ouch1, 2:TableNotFoundException ouch2, 3:AccumuloException ouch3);
                                                                                                             
   void createTable (1:binary login, 2:string tableName, 3:bool versioningIter, 4:TimeType type)        throws (1:AccumuloException ouch1, 2:AccumuloSecurityException ouch2, 3:TableExistsException ouch3);
diff --git a/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/CompactionIterators.java b/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
similarity index 79%
rename from server/base/src/main/java/org/apache/accumulo/server/master/tableOps/CompactionIterators.java
rename to server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
index 4f5bf42e9..63141053a 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/CompactionIterators.java
++ b/server/base/src/main/java/org/apache/accumulo/server/master/tableOps/UserCompactionConfig.java
@@ -23,27 +23,32 @@ import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;

 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.io.Writable;
 
public class CompactionIterators implements Writable {
public class UserCompactionConfig implements Writable {
   byte[] startRow;
   byte[] endRow;
   List<IteratorSetting> iterators;
  
  public CompactionIterators(byte[] startRow, byte[] endRow, List<IteratorSetting> iterators) {
  private CompactionStrategyConfig compactionStrategy;

  public UserCompactionConfig(byte[] startRow, byte[] endRow, List<IteratorSetting> iterators, CompactionStrategyConfig csc) {
     this.startRow = startRow;
     this.endRow = endRow;
     this.iterators = iterators;
    this.compactionStrategy = csc;
   }
  
  public CompactionIterators() {

  public UserCompactionConfig() {
     startRow = null;
     endRow = null;
     iterators = Collections.emptyList();
   }
  

   @Override
   public void write(DataOutput out) throws IOException {
     out.writeBoolean(startRow != null);
@@ -51,19 +56,22 @@ public class CompactionIterators implements Writable {
       out.writeInt(startRow.length);
       out.write(startRow);
     }
    

     out.writeBoolean(endRow != null);
     if (endRow != null) {
       out.writeInt(endRow.length);
       out.write(endRow);
     }
    

     out.writeInt(iterators.size());
     for (IteratorSetting is : iterators) {
       is.write(out);
     }

    CompactionStrategyConfigUtil.encode(out, compactionStrategy);

   }
  

   @Override
   public void readFields(DataInput in) throws IOException {
     if (in.readBoolean()) {
@@ -72,35 +80,41 @@ public class CompactionIterators implements Writable {
     } else {
       startRow = null;
     }
    

     if (in.readBoolean()) {
       endRow = new byte[in.readInt()];
       in.readFully(endRow);
     } else {
       endRow = null;
     }
    

     int num = in.readInt();
     iterators = new ArrayList<IteratorSetting>(num);
    

     for (int i = 0; i < num; i++) {
       iterators.add(new IteratorSetting(in));
     }

    compactionStrategy = CompactionStrategyConfigUtil.decode(in);
   }
  

   public Text getEndRow() {
     if (endRow == null)
       return null;
     return new Text(endRow);
   }
  

   public Text getStartRow() {
     if (startRow == null)
       return null;
     return new Text(startRow);
   }
  

   public List<IteratorSetting> getIterators() {
     return iterators;
   }

  public CompactionStrategyConfig getCompactionStrategy() {
    return compactionStrategy;
  }
 }
\ No newline at end of file
diff --git a/server/master/src/main/java/org/apache/accumulo/master/FateServiceHandler.java b/server/master/src/main/java/org/apache/accumulo/master/FateServiceHandler.java
index bdb5e2f90..5207745fc 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/FateServiceHandler.java
++ b/server/master/src/main/java/org/apache/accumulo/master/FateServiceHandler.java
@@ -31,6 +31,8 @@ import java.util.Map.Entry;
 import java.util.Set;
 
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.NamespaceNotFoundException;
 import org.apache.accumulo.core.client.TableNotFoundException;
@@ -370,6 +372,7 @@ class FateServiceHandler implements FateService.Iface {
         byte[] startRow = ByteBufferUtil.toBytes(arguments.get(1));
         byte[] endRow = ByteBufferUtil.toBytes(arguments.get(2));
         List<IteratorSetting> iterators = IteratorUtil.decodeIteratorSettings(ByteBufferUtil.toBytes(arguments.get(3)));
        CompactionStrategyConfig compactionStrategy = CompactionStrategyConfigUtil.decode(ByteBufferUtil.toBytes(arguments.get(4)));
         String namespaceId = Tables.getNamespaceId(master.getInstance(), tableId);
 
         final boolean canCompact;
@@ -383,7 +386,7 @@ class FateServiceHandler implements FateService.Iface {
         if (!canCompact)
           throw new ThriftSecurityException(c.getPrincipal(), SecurityErrorCode.PERMISSION_DENIED);
 
        master.fate.seedTransaction(opid, new TraceRepo<Master>(new CompactRange(tableId, startRow, endRow, iterators)), autoCleanup);
        master.fate.seedTransaction(opid, new TraceRepo<Master>(new CompactRange(tableId, startRow, endRow, iterators, compactionStrategy)), autoCleanup);
         break;
       }
       case TABLE_CANCEL_COMPACT: {
diff --git a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
index b3037d35e..db8bbfe13 100644
-- a/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
++ b/server/master/src/main/java/org/apache/accumulo/master/tableOps/CompactRange.java
@@ -18,10 +18,6 @@ package org.apache.accumulo.master.tableOps;
 
 import static java.nio.charset.StandardCharsets.UTF_8;
 
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
@@ -34,6 +30,7 @@ import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.thrift.TableOperation;
 import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
@@ -55,10 +52,10 @@ import org.apache.accumulo.fate.zookeeper.IZooReaderWriter.Mutator;
 import org.apache.accumulo.master.Master;
 import org.apache.accumulo.server.master.LiveTServerSet.TServerConnection;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.tableOps.UserCompactionConfig;
 import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
 import org.apache.commons.codec.binary.Hex;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.io.WritableUtils;
 import org.apache.log4j.Logger;
 import org.apache.thrift.TException;
@@ -207,95 +204,17 @@ public class CompactRange extends MasterRepo {
   private final String tableId;
   private byte[] startRow;
   private byte[] endRow;
  private byte[] iterators;
  private byte[] config;
 
  public static class CompactionIterators implements Writable {
    byte[] startRow;
    byte[] endRow;
    List<IteratorSetting> iterators;
 
    public CompactionIterators(byte[] startRow, byte[] endRow, List<IteratorSetting> iterators) {
      this.startRow = startRow;
      this.endRow = endRow;
      this.iterators = iterators;
    }

    public CompactionIterators() {
      startRow = null;
      endRow = null;
      iterators = Collections.emptyList();
    }

    @Override
    public void write(DataOutput out) throws IOException {
      out.writeBoolean(startRow != null);
      if (startRow != null) {
        out.writeInt(startRow.length);
        out.write(startRow);
      }

      out.writeBoolean(endRow != null);
      if (endRow != null) {
        out.writeInt(endRow.length);
        out.write(endRow);
      }

      out.writeInt(iterators.size());
      for (IteratorSetting is : iterators) {
        is.write(out);
      }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
      if (in.readBoolean()) {
        startRow = new byte[in.readInt()];
        in.readFully(startRow);
      } else {
        startRow = null;
      }

      if (in.readBoolean()) {
        endRow = new byte[in.readInt()];
        in.readFully(endRow);
      } else {
        endRow = null;
      }

      int num = in.readInt();
      iterators = new ArrayList<IteratorSetting>(num);

      for (int i = 0; i < num; i++) {
        iterators.add(new IteratorSetting(in));
      }
    }

    public Text getEndRow() {
      if (endRow == null)
        return null;
      return new Text(endRow);
    }

    public Text getStartRow() {
      if (startRow == null)
        return null;
      return new Text(startRow);
    }

    public List<IteratorSetting> getIterators() {
      return iterators;
    }
  }

  public CompactRange(String tableId, byte[] startRow, byte[] endRow, List<IteratorSetting> iterators) throws ThriftTableOperationException {
  public CompactRange(String tableId, byte[] startRow, byte[] endRow, List<IteratorSetting> iterators, CompactionStrategyConfig compactionStrategy)
      throws ThriftTableOperationException {
     this.tableId = tableId;
     this.startRow = startRow.length == 0 ? null : startRow;
     this.endRow = endRow.length == 0 ? null : endRow;
 
    if (iterators.size() > 0) {
      this.iterators = WritableUtils.toByteArray(new CompactionIterators(this.startRow, this.endRow, iterators));
    } else {
      iterators = null;
    if (iterators.size() > 0 || compactionStrategy != null) {
      this.config = WritableUtils.toByteArray(new UserCompactionConfig(this.startRow, this.endRow, iterators, compactionStrategy));
     }
 
     if (this.startRow != null && this.endRow != null && new Text(startRow).compareTo(new Text(endRow)) >= 0)
@@ -337,12 +256,12 @@ public class CompactRange extends MasterRepo {
 
           StringBuilder encodedIterators = new StringBuilder();
 
          if (iterators != null) {
          if (config != null) {
             Hex hex = new Hex();
             encodedIterators.append(",");
             encodedIterators.append(txidString);
             encodedIterators.append("=");
            encodedIterators.append(new String(hex.encode(iterators), UTF_8));
            encodedIterators.append(new String(hex.encode(config), UTF_8));
           }
 
           return (Long.toString(flushID) + encodedIterators).getBytes(UTF_8);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index d4447ab8a..f9f5b4caf 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -166,6 +166,7 @@ import org.apache.accumulo.server.master.state.TabletLocationState;
 import org.apache.accumulo.server.master.state.TabletLocationState.BadLocationStateException;
 import org.apache.accumulo.server.master.state.TabletStateStore;
 import org.apache.accumulo.server.master.state.ZooTabletStateStore;
import org.apache.accumulo.server.master.tableOps.UserCompactionConfig;
 import org.apache.accumulo.server.problems.ProblemReport;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.replication.ZooKeeperInitialization;
@@ -1643,19 +1644,19 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
             tabletsToCompact.add(tablet);
       }
 
      Long compactionId = null;
      Pair<Long,UserCompactionConfig> compactionInfo = null;
 
       for (Tablet tablet : tabletsToCompact) {
         // all for the same table id, so only need to read
         // compaction id once
        if (compactionId == null)
        if (compactionInfo == null)
           try {
            compactionId = tablet.getCompactionID().getFirst();
            compactionInfo = tablet.getCompactionID();
           } catch (NoNodeException e) {
             log.info("Asked to compact table with no compaction id " + ke + " " + e.getMessage());
             return;
           }
        tablet.compactAll(compactionId);
        tablet.compactAll(compactionInfo.getFirst(), compactionInfo.getSecond());
       }
 
     }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
index 6c31fab6c..6b2eaf0da 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServerResourceManager.java
@@ -16,8 +16,6 @@
  */
 package org.apache.accumulo.tserver;
 
import static com.google.common.base.Preconditions.checkNotNull;

 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
@@ -62,6 +60,8 @@ import org.apache.accumulo.tserver.compaction.MajorCompactionRequest;
 import org.apache.accumulo.tserver.tablet.Tablet;
 import org.apache.log4j.Logger;
 
import static com.google.common.base.Preconditions.checkNotNull;

 /**
  * ResourceManager is responsible for managing the resources of all tablets within a tablet server.
  *
@@ -638,11 +638,13 @@ public class TabletServerResourceManager {
       request.setFiles(tabletFiles);
       try {
         return strategy.shouldCompact(request);
      } catch (IOException ex) {
        return false;
      } catch (IOException e) {
        throw new RuntimeException(e);
       }
     }
 


     // END methods that Tablets call to make decisions about major compaction
 
     // tablets call this method to run minor compactions,
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionPlan.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionPlan.java
index 6f69fb072..75c6bd81b 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionPlan.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionPlan.java
@@ -22,9 +22,8 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
import org.apache.accumulo.server.fs.FileRef;

 import com.google.common.collect.Sets;
import org.apache.accumulo.server.fs.FileRef;
 
 /**
  * A plan for a compaction: the input files, the files that are *not* inputs to a compaction that should simply be deleted, and the optional parameters used to
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionStrategy.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionStrategy.java
index 7bc1a80ea..2d9488475 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionStrategy.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/CompactionStrategy.java
@@ -43,6 +43,12 @@ public abstract class CompactionStrategy {
    * {@link #getCompactionPlan(MajorCompactionRequest)}) that it does not need to. Any state stored during shouldCompact will no longer exist when
    * {@link #gatherInformation(MajorCompactionRequest)} and {@link #getCompactionPlan(MajorCompactionRequest)} are called.
    * 
   * <P>
   * Called while holding the tablet lock, so it should not be doing any blocking.
   * 
   * <P>
   * Since no blocking should be done in this method, then its unexpected that this method will throw IOException. However since its in the API, it can not be
   * easily removed.
    */
   public abstract boolean shouldCompact(MajorCompactionRequest request) throws IOException;
 
@@ -58,6 +64,10 @@ public abstract class CompactionStrategy {
   /**
    * Get the plan for compacting a tablets files. Called while holding the tablet lock, so it should not be doing any blocking.
    * 
   * <P>
   * Since no blocking should be done in this method, then its unexpected that this method will throw IOException. However since its in the API, it can not be
   * easily removed.
   * 
    * @param request
    *          basic details about the tablet
    * @return the plan for a major compaction, or null to cancel the compaction.
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategy.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategy.java
index 8b03d1700..1f0dc3a6c 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategy.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategy.java
@@ -16,7 +16,6 @@
  */
 package org.apache.accumulo.tserver.compaction;
 
import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
@@ -31,13 +30,13 @@ import org.apache.accumulo.server.fs.FileRef;
 public class DefaultCompactionStrategy extends CompactionStrategy {
 
   @Override
  public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
  public boolean shouldCompact(MajorCompactionRequest request) {
     CompactionPlan plan = getCompactionPlan(request);
     return plan != null && !plan.inputFiles.isEmpty();
   }
 
   @Override
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) {
     CompactionPlan result = new CompactionPlan();
 
     List<FileRef> toCompact = findMapFilesToCompact(request);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/EverythingCompactionStrategy.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/EverythingCompactionStrategy.java
new file mode 100644
index 000000000..9295c305e
-- /dev/null
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/EverythingCompactionStrategy.java
@@ -0,0 +1,39 @@
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

package org.apache.accumulo.tserver.compaction;

import java.io.IOException;

/**
 * The default compaction strategy for user initiated compactions. This strategy will always select all files.
 */

public class EverythingCompactionStrategy extends CompactionStrategy {

  @Override
  public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
    return request.getFiles().size() > 0;
  }

  @Override
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
    CompactionPlan plan = new CompactionPlan();
    plan.inputFiles.addAll(request.getFiles().keySet());
    return plan;
  }
}
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/SizeLimitCompactionStrategy.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/SizeLimitCompactionStrategy.java
index 478939ade..6d4dc795a 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/SizeLimitCompactionStrategy.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/SizeLimitCompactionStrategy.java
@@ -53,7 +53,7 @@ public class SizeLimitCompactionStrategy extends DefaultCompactionStrategy {
   }
 
   @Override
  public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
  public boolean shouldCompact(MajorCompactionRequest request) {
     return super.shouldCompact(filterFiles(request));
   }
 
@@ -63,7 +63,7 @@ public class SizeLimitCompactionStrategy extends DefaultCompactionStrategy {
   }
 
   @Override
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
  public CompactionPlan getCompactionPlan(MajorCompactionRequest request) {
     return super.getCompactionPlan(filterFiles(request));
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
index bc55c4f9f..bc7506284 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
@@ -45,6 +45,7 @@ import java.util.concurrent.locks.ReentrantLock;
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
 import org.apache.accumulo.core.client.impl.DurabilityImpl;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
@@ -96,10 +97,11 @@ import org.apache.accumulo.server.fs.VolumeManager.FileType;
 import org.apache.accumulo.server.fs.VolumeUtil;
 import org.apache.accumulo.server.fs.VolumeUtil.TabletFiles;
 import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.tableOps.CompactionIterators;
import org.apache.accumulo.server.master.tableOps.UserCompactionConfig;
 import org.apache.accumulo.server.problems.ProblemReport;
 import org.apache.accumulo.server.problems.ProblemReports;
 import org.apache.accumulo.server.problems.ProblemType;
import org.apache.accumulo.server.security.SystemCredentials;
 import org.apache.accumulo.server.tablets.TabletTime;
 import org.apache.accumulo.server.tablets.UniqueNameAllocator;
 import org.apache.accumulo.server.util.FileUtil;
@@ -1152,7 +1154,7 @@ public class Tablet implements TabletCommitter {
     }
   }
 
  public Pair<Long,List<IteratorSetting>> getCompactionID() throws NoNodeException {
  public Pair<Long,UserCompactionConfig> getCompactionID() throws NoNodeException {
     try {
       String zTablePath = Constants.ZROOT + "/" + tabletServer.getInstance().getInstanceID() + Constants.ZTABLES + "/" + extent.getTableId()
           + Constants.ZTABLE_COMPACT_ID;
@@ -1160,7 +1162,7 @@ public class Tablet implements TabletCommitter {
       String[] tokens = new String(ZooReaderWriter.getInstance().getData(zTablePath, null), UTF_8).split(",");
       long compactID = Long.parseLong(tokens[0]);
 
      CompactionIterators iters = new CompactionIterators();
      UserCompactionConfig compactionConfig = new UserCompactionConfig();
 
       if (tokens.length > 1) {
         Hex hex = new Hex();
@@ -1168,20 +1170,20 @@ public class Tablet implements TabletCommitter {
         DataInputStream dis = new DataInputStream(bais);
 
         try {
          iters.readFields(dis);
          compactionConfig.readFields(dis);
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
 
        KeyExtent ke = new KeyExtent(extent.getTableId(), iters.getEndRow(), iters.getStartRow());
        KeyExtent ke = new KeyExtent(extent.getTableId(), compactionConfig.getEndRow(), compactionConfig.getStartRow());
 
         if (!ke.overlaps(extent)) {
           // only use iterators if compaction range overlaps
          iters = new CompactionIterators();
          compactionConfig = new UserCompactionConfig();
         }
       }
 
      return new Pair<Long,List<IteratorSetting>>(compactID, iters.getIterators());
      return new Pair<Long,UserCompactionConfig>(compactID, compactionConfig);
     } catch (InterruptedException e) {
       throw new RuntimeException(e);
     } catch (NumberFormatException nfe) {
@@ -1780,21 +1782,34 @@ public class Tablet implements TabletCommitter {
 
     long t1, t2, t3;
 
    // acquire file info outside of tablet lock
    CompactionStrategy strategy = Property.createTableInstanceFromPropertyName(tableConfiguration, Property.TABLE_COMPACTION_STRATEGY,
        CompactionStrategy.class, new DefaultCompactionStrategy());
    strategy.init(Property.getCompactionStrategyOptions(tableConfiguration));

    Pair<Long,UserCompactionConfig> compactionId = null;
    CompactionStrategy strategy = null;
     Map<FileRef,Pair<Key,Key>> firstAndLastKeys = null;
    if (reason == MajorCompactionReason.CHOP) {

    if(reason == MajorCompactionReason.USER){
      try {
        compactionId = getCompactionID();
        strategy = createCompactionStrategy(compactionId.getSecond().getCompactionStrategy());
      } catch (NoNodeException e) {
        throw new RuntimeException(e);
      }
    } else if (reason == MajorCompactionReason.NORMAL || reason == MajorCompactionReason.IDLE) {
      strategy = Property.createTableInstanceFromPropertyName(tableConfiguration, Property.TABLE_COMPACTION_STRATEGY, CompactionStrategy.class,
          new DefaultCompactionStrategy());
      strategy.init(Property.getCompactionStrategyOptions(tableConfiguration));
    } else if (reason == MajorCompactionReason.CHOP) {
       firstAndLastKeys = getFirstAndLastKeys(getDatafileManager().getDatafileSizes());
    } else if (reason != MajorCompactionReason.USER) {
    } else {
      throw new IllegalArgumentException("Unknown compaction reason " + reason);
    }

    if (strategy != null) {
       MajorCompactionRequest request = new MajorCompactionRequest(extent, reason, getTabletServer().getFileSystem(), tableConfiguration);
       request.setFiles(getDatafileManager().getDatafileSizes());
       strategy.gatherInformation(request);
     }
 
    Map<FileRef,DataFileValue> filesToCompact;
    Map<FileRef,DataFileValue> filesToCompact = null;
 
     int maxFilesToCompact = tableConfiguration.getCount(Property.TSERV_MAJC_THREAD_MAXOPEN);
 
@@ -1802,6 +1817,7 @@ public class Tablet implements TabletCommitter {
     CompactionPlan plan = null;
 
     boolean propogateDeletes = false;
    boolean updateCompactionID = false;
 
     synchronized (this) {
       // plan all that work that needs to be done in the sync block... then do the actual work
@@ -1831,8 +1847,6 @@ public class Tablet implements TabletCommitter {
       if (reason == MajorCompactionReason.CHOP) {
         // enforce rules: files with keys outside our range need to be compacted
         inputFiles.addAll(findChopFiles(extent, firstAndLastKeys, allFiles.keySet()));
      } else if (reason == MajorCompactionReason.USER) {
        inputFiles.addAll(allFiles.keySet());
       } else {
         MajorCompactionRequest request = new MajorCompactionRequest(extent, reason, fs, tableConfiguration);
         request.setFiles(allFiles);
@@ -1844,32 +1858,48 @@ public class Tablet implements TabletCommitter {
       }
 
       if (inputFiles.isEmpty()) {
        return majCStats;
        if (reason == MajorCompactionReason.USER) {
          // no work to do
          lastCompactID = compactionId.getFirst();
          updateCompactionID = true;
        } else {
          return majCStats;
        }
      } else {
        // If no original files will exist at the end of the compaction, we do not have to propogate deletes
        Set<FileRef> droppedFiles = new HashSet<FileRef>();
        droppedFiles.addAll(inputFiles);
        if (plan != null)
          droppedFiles.addAll(plan.deleteFiles);
        propogateDeletes = !(droppedFiles.equals(allFiles.keySet()));
        log.debug("Major compaction plan: " + plan + " propogate deletes : " + propogateDeletes);
        filesToCompact = new HashMap<FileRef,DataFileValue>(allFiles);
        filesToCompact.keySet().retainAll(inputFiles);

        getDatafileManager().reserveMajorCompactingFiles(filesToCompact.keySet());
       }
      // If no original files will exist at the end of the compaction, we do not have to propogate deletes
      Set<FileRef> droppedFiles = new HashSet<FileRef>();
      droppedFiles.addAll(inputFiles);
      if (plan != null)
        droppedFiles.addAll(plan.deleteFiles);
      propogateDeletes = !(droppedFiles.equals(allFiles.keySet()));
      log.debug("Major compaction plan: " + plan + " propogate deletes : " + propogateDeletes);
      filesToCompact = new HashMap<FileRef,DataFileValue>(allFiles);
      filesToCompact.keySet().retainAll(inputFiles);
 
       t3 = System.currentTimeMillis();

      getDatafileManager().reserveMajorCompactingFiles(filesToCompact.keySet());
     }
 
     try {
 
       log.debug(String.format("MajC initiate lock %.2f secs, wait %.2f secs", (t3 - t2) / 1000.0, (t2 - t1) / 1000.0));
 
      Pair<Long,List<IteratorSetting>> compactionId = null;
      if (!propogateDeletes) {
      if (updateCompactionID) {
        MetadataTableUtil.updateTabletCompactID(extent, compactionId.getFirst(),tabletServer, getTabletServer().getLock());
        return majCStats;
      }

      if (!propogateDeletes && compactionId == null) {
         // compacting everything, so update the compaction id in metadata
         try {
           compactionId = getCompactionID();
          if (compactionId.getSecond().getCompactionStrategy() != null) {
            compactionId = null;
            // TODO maybe return unless chop?
          }

         } catch (NoNodeException e) {
           throw new RuntimeException(e);
         }
@@ -1890,7 +1920,7 @@ public class Tablet implements TabletCommitter {
           }
         }
 
        compactionIterators = compactionId.getSecond();
        compactionIterators = compactionId.getSecond().getIterators();
       }
 
       // need to handle case where only one file is being major compacted
@@ -2495,7 +2525,24 @@ public class Tablet implements TabletCommitter {
     initiateMajorCompaction(MajorCompactionReason.CHOP);
   }
 
  public void compactAll(long compactionId) {
  private CompactionStrategy createCompactionStrategy(CompactionStrategyConfig strategyConfig) {
    String context = tableConfiguration.get(Property.TABLE_CLASSPATH);
    String clazzName = strategyConfig.getClassName();
    try {
      Class<? extends CompactionStrategy> clazz;
      if (context != null && !context.equals(""))
        clazz = AccumuloVFSClassLoader.getContextManager().loadClass(context, clazzName, CompactionStrategy.class);
      else
        clazz = AccumuloVFSClassLoader.loadClass(clazzName, CompactionStrategy.class);
      CompactionStrategy strategy = clazz.newInstance();
      strategy.init(strategyConfig.getOptions());
      return strategy;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void compactAll(long compactionId, UserCompactionConfig compactionConfig) {
     boolean updateMetadata = false;
 
     synchronized (this) {
@@ -2522,8 +2569,25 @@ public class Tablet implements TabletCommitter {
         majorCompactionState = CompactionState.IN_PROGRESS;
         updateMetadata = true;
         lastCompactID = compactionId;
      } else
        initiateMajorCompaction(MajorCompactionReason.USER);
      } else {
        CompactionStrategyConfig strategyConfig = compactionConfig.getCompactionStrategy();
        CompactionStrategy strategy = createCompactionStrategy(strategyConfig);

        MajorCompactionRequest request = new MajorCompactionRequest(extent, MajorCompactionReason.USER, getTabletServer().getFileSystem(), tableConfiguration);
        request.setFiles(getDatafileManager().getDatafileSizes());

        try {
          if (strategy.shouldCompact(request)) {
            initiateMajorCompaction(MajorCompactionReason.USER);
          } else {
            majorCompactionState = CompactionState.IN_PROGRESS;
            updateMetadata = true;
            lastCompactID = compactionId;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
     }
 
     if (updateMetadata) {
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
index 80dd9ba73..660630e92 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
@@ -17,28 +17,27 @@
 package org.apache.accumulo.shell.commands;
 
 import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
 import java.util.List;
import java.util.Map;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
 import org.apache.accumulo.shell.Shell;
 import org.apache.commons.cli.CommandLine;
 import org.apache.commons.cli.Option;
 import org.apache.commons.cli.Options;
import org.apache.hadoop.io.Text;
 
 public class CompactCommand extends TableOperation {
  private Option noFlushOption, waitOpt, profileOpt, cancelOpt;
  private boolean flush;
  private Text startRow;
  private Text endRow;
  private List<IteratorSetting> iterators;
  private Option noFlushOption, waitOpt, profileOpt, cancelOpt, strategyOpt, strategyConfigOpt;

  private CompactionConfig compactionConfig = null;
   
   boolean override = false;
  private boolean wait;
   
   private boolean cancel = false;
 
@@ -59,13 +58,13 @@ public class CompactCommand extends TableOperation {
       }
     } else {
       try {
        if (wait) {
        if (compactionConfig.getWait()) {
           Shell.log.info("Compacting table ...");
         }
         
        shellState.getConnector().tableOperations().compact(tableName, startRow, endRow, iterators, flush, wait);
        shellState.getConnector().tableOperations().compact(tableName, compactionConfig);
         
        Shell.log.info("Compaction of table " + tableName + " " + (wait ? "completed" : "started") + " for given range");
        Shell.log.info("Compaction of table " + tableName + " " + (compactionConfig.getWait() ? "completed" : "started") + " for given range");
       } catch (Exception ex) {
         throw new AccumuloException(ex);
       }
@@ -85,10 +84,12 @@ public class CompactCommand extends TableOperation {
       cancel = false;
     }
 
    flush = !cl.hasOption(noFlushOption.getOpt());
    startRow = OptUtil.getStartRow(cl);
    endRow = OptUtil.getEndRow(cl);
    wait = cl.hasOption(waitOpt.getOpt());
    compactionConfig = new CompactionConfig();

    compactionConfig.setFlush(!cl.hasOption(noFlushOption.getOpt()));
    compactionConfig.setWait(cl.hasOption(waitOpt.getOpt()));
    compactionConfig.setStartRow(OptUtil.getStartRow(cl));
    compactionConfig.setEndRow(OptUtil.getEndRow(cl));
     
     if (cl.hasOption(profileOpt.getOpt())) {
       List<IteratorSetting> iterators = shellState.iteratorProfiles.get(cl.getOptionValue(profileOpt.getOpt()));
@@ -97,11 +98,24 @@ public class CompactCommand extends TableOperation {
         return -1;
       }
       
      this.iterators = new ArrayList<IteratorSetting>(iterators);
    } else {
      this.iterators = Collections.emptyList();
      compactionConfig.setIterators(new ArrayList<>(iterators));
     }
 
    if (cl.hasOption(strategyOpt.getOpt())) {
      CompactionStrategyConfig csc = new CompactionStrategyConfig(cl.getOptionValue(strategyOpt.getOpt()));
      if (cl.hasOption(strategyConfigOpt.getOpt())) {
        Map<String,String> props = new HashMap<>();
        String[] keyVals = cl.getOptionValue(strategyConfigOpt.getOpt()).split(",");
        for (String keyVal : keyVals) {
          String[] sa = keyVal.split("=");
          props.put(sa[0], sa[1]);
        }

        csc.setOptions(props);
      }

      compactionConfig.setCompactionStrategy(csc);
    }
 
     return super.execute(fullCommand, cl, shellState);
   }
@@ -121,6 +135,11 @@ public class CompactCommand extends TableOperation {
     profileOpt.setArgName("profile");
     opts.addOption(profileOpt);
 
    strategyOpt = new Option("s", "strategy", true, "compaction strategy class name");
    opts.addOption(strategyOpt);
    strategyConfigOpt = new Option("sc", "strategyConfig", true, "Key value options for compaction strategy.  Expects <prop>=<value>{,<prop>=<value>}");
    opts.addOption(strategyConfigOpt);

     cancelOpt = new Option(null, "cancel", false, "cancel user initiated compactions");
     opts.addOption(cancelOpt);
 
diff --git a/test/src/test/java/org/apache/accumulo/proxy/SimpleProxyIT.java b/test/src/test/java/org/apache/accumulo/proxy/SimpleProxyIT.java
index 50e53a9df..c07507555 100644
-- a/test/src/test/java/org/apache/accumulo/proxy/SimpleProxyIT.java
++ b/test/src/test/java/org/apache/accumulo/proxy/SimpleProxyIT.java
@@ -69,6 +69,7 @@ import org.apache.accumulo.proxy.thrift.BatchScanOptions;
 import org.apache.accumulo.proxy.thrift.Column;
 import org.apache.accumulo.proxy.thrift.ColumnUpdate;
 import org.apache.accumulo.proxy.thrift.CompactionReason;
import org.apache.accumulo.proxy.thrift.CompactionStrategyConfig;
 import org.apache.accumulo.proxy.thrift.CompactionType;
 import org.apache.accumulo.proxy.thrift.Condition;
 import org.apache.accumulo.proxy.thrift.ConditionalStatus;
@@ -245,7 +246,7 @@ public class SimpleProxyIT {
       fail("exception not thrown");
     } catch (TException ex) {}
     try {
      client.compactTable(badLogin, table, null, null, null, true, false);
      client.compactTable(badLogin, table, null, null, null, true, false, null);
       fail("exception not thrown");
     } catch (AccumuloSecurityException ex) {}
     try {
@@ -531,7 +532,7 @@ public class SimpleProxyIT {
       fail("exception not thrown");
     } catch (TableNotFoundException ex) {}
     try {
      client.compactTable(creds, doesNotExist, null, null, null, true, false);
      client.compactTable(creds, doesNotExist, null, null, null, true, false, null);
       fail("exception not thrown");
     } catch (TableNotFoundException ex) {}
     try {
@@ -874,7 +875,7 @@ public class SimpleProxyIT {
       public void run() {
         try {
           Client client2 = new TestProxyClient("localhost", proxyPort, protocolClass.newInstance()).proxy();
          client2.compactTable(creds, "slow", null, null, null, true, true);
          client2.compactTable(creds, "slow", null, null, null, true, true, null);
         } catch (Exception e) {
           throw new RuntimeException(e);
         }
@@ -1126,7 +1127,7 @@ public class SimpleProxyIT {
     client.clearLocatorCache(creds, TABLE_TEST);
 
     // compact
    client.compactTable(creds, TABLE_TEST, null, null, null, true, true);
    client.compactTable(creds, TABLE_TEST, null, null, null, true, true, null);
     assertEquals(1, countFiles(TABLE_TEST));
     assertScan(expected, TABLE_TEST);
 
@@ -1141,7 +1142,7 @@ public class SimpleProxyIT {
     assertEquals(2, diskUsage.size());
     assertEquals(1, diskUsage.get(0).getTables().size());
     assertEquals(2, diskUsage.get(1).getTables().size());
    client.compactTable(creds, TABLE_TEST2, null, null, null, true, true);
    client.compactTable(creds, TABLE_TEST2, null, null, null, true, true, null);
     diskUsage = (client.getDiskUsage(creds, tablesToScan));
     assertEquals(3, diskUsage.size());
     assertEquals(1, diskUsage.get(0).getTables().size());
@@ -1591,4 +1592,40 @@ public class SimpleProxyIT {
     assertEquals(range.start.timestamp, range.start.timestamp);
     assertEquals(range.stop.timestamp, range.stop.timestamp);
   }

  @Test
  public void testCompactionStrategy() throws Exception {
    final String tableName = makeTableName();

    client.createTable(creds, tableName, true, TimeType.MILLIS);

    client.setProperty(creds, Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "context1",
        System.getProperty("user.dir") + "/src/test/resources/TestCompactionStrat.jar");
    client.setTableProperty(creds, tableName, Property.TABLE_CLASSPATH.getKey(), "context1");

    client.addSplits(creds, tableName, Collections.singleton(s2bb("efg")));

    client.updateAndFlush(creds, tableName, mutation("a", "cf", "cq", "v1"));
    client.flushTable(creds, tableName, null, null, true);

    client.updateAndFlush(creds, tableName, mutation("b", "cf", "cq", "v2"));
    client.flushTable(creds, tableName, null, null, true);

    client.updateAndFlush(creds, tableName, mutation("y", "cf", "cq", "v1"));
    client.flushTable(creds, tableName, null, null, true);

    client.updateAndFlush(creds, tableName, mutation("z", "cf", "cq", "v2"));
    client.flushTable(creds, tableName, null, null, true);

    assertEquals(4, countFiles(tableName));

    CompactionStrategyConfig csc = new CompactionStrategyConfig();

    // The EfgCompactionStrat will only compact tablets with and end row of efg
    csc.setClassName("org.apache.accumulo.test.EfgCompactionStrat");

    client.compactTable(creds, tableName, null, null, null, true, true, csc);

    assertEquals(3, countFiles(tableName));
  }
 }
diff --git a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
index 544bb7674..d878c7f7d 100644
-- a/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/test/java/org/apache/accumulo/test/ShellServerIT.java
@@ -54,8 +54,9 @@ import org.apache.accumulo.core.file.FileSKVWriter;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.accumulo.shell.Shell;
 import org.apache.accumulo.harness.SharedMiniClusterIT;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.test.UserCompactionStrategyIT.TestCompactionStrategy;
 import org.apache.accumulo.test.functional.FunctionalTestUtils;
 import org.apache.accumulo.test.functional.SlowIterator;
 import org.apache.accumulo.tracer.TraceServer;
@@ -725,7 +726,7 @@ public class ShellServerIT extends SharedMiniClusterIT {
     // make two more files:
     ts.exec("insert m 1 2 3");
     ts.exec("flush -w");
    ts.exec("insert n 1 2 3");
    ts.exec("insert n 1 2 v901");
     ts.exec("flush -w");
     List<String> oldFiles = getFiles(tableId);
 
@@ -740,6 +741,14 @@ public class ShellServerIT extends SharedMiniClusterIT {
     ts.exec("merge --all -t " + table);
     ts.exec("compact -w");
     assertEquals(1, countFiles(tableId));

    // test compaction strategy
    ts.exec("insert z 1 2 v900");
    ts.exec("compact -w -s " + TestCompactionStrategy.class.getName() + " -sc inputPrefix=F,dropPrefix=A");
    assertEquals(1, countFiles(tableId));
    ts.exec("scan", true, "v900", true);
    ts.exec("scan", true, "v901", false);

     ts.exec("deletetable -f " + table);
   }
 
diff --git a/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java b/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java
new file mode 100644
index 000000000..5421f520b
-- /dev/null
++ b/test/src/test/java/org/apache/accumulo/test/UserCompactionStrategyIT.java
@@ -0,0 +1,337 @@
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.RegExFilter;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.harness.AccumuloClusterIT;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.test.functional.FunctionalTestUtils;
import org.apache.accumulo.tserver.compaction.CompactionPlan;
import org.apache.accumulo.tserver.compaction.CompactionStrategy;
import org.apache.accumulo.tserver.compaction.MajorCompactionRequest;
import org.apache.hadoop.io.Text;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class UserCompactionStrategyIT extends AccumuloClusterIT {

  public static class SizeCompactionStrategy extends CompactionStrategy {

    private long size = 0;

    @Override
    public void init(Map<String,String> options) {
      size = Long.parseLong(options.get("size"));
    }

    @Override
    public boolean shouldCompact(MajorCompactionRequest request) throws IOException {

      for (DataFileValue dfv : request.getFiles().values())
        if (dfv.getSize() < size)
          return true;

      return false;
    }

    @Override
    public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
      CompactionPlan plan = new CompactionPlan();

      for (Entry<FileRef,DataFileValue> entry : request.getFiles().entrySet())
        if (entry.getValue().getSize() < size)
          plan.inputFiles.add(entry.getKey());

      return plan;
    }

  }

  public static class TestCompactionStrategy extends CompactionStrategy {

    private String inputPrefix = "Z";
    private String dropPrefix = "Z";
    private boolean shouldCompact = false;

    @Override
    public void init(Map<String,String> options) {
      if (options.containsKey("inputPrefix"))
        inputPrefix = options.get("inputPrefix");
      if (options.containsKey("dropPrefix"))
        dropPrefix = options.get("dropPrefix");
      if (options.containsKey("shouldCompact"))
        shouldCompact = Boolean.parseBoolean(options.get("shouldCompact"));
    }

    @Override
    public boolean shouldCompact(MajorCompactionRequest request) throws IOException {
      if (shouldCompact)
        return true;

      for (FileRef fref : request.getFiles().keySet()) {
        if (fref.path().getName().startsWith(inputPrefix))
          return true;
        if (fref.path().getName().startsWith(dropPrefix))
          return true;
      }

      return false;
    }

    @Override
    public CompactionPlan getCompactionPlan(MajorCompactionRequest request) throws IOException {
      CompactionPlan plan = new CompactionPlan();

      for (FileRef fref : request.getFiles().keySet()) {
        if (fref.path().getName().startsWith(dropPrefix)) {
          plan.deleteFiles.add(fref);
        } else if (fref.path().getName().startsWith(inputPrefix)) {
          plan.inputFiles.add(fref);
        }
      }

      return plan;
    }
  }

  @Test
  public void testDropA() throws Exception {
    Connector c = getConnector();

    String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);

    writeFlush(c, tableName, "a");
    writeFlush(c, tableName, "b");
    // create a file that starts with A containing rows 'a' and 'b'
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    writeFlush(c, tableName, "c");
    writeFlush(c, tableName, "d");

    // drop files that start with A
    CompactionStrategyConfig csConfig = new CompactionStrategyConfig(TestCompactionStrategy.class.getName());
    csConfig.setOptions(ImmutableMap.of("dropPrefix", "A", "inputPrefix", "F"));
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig));

    Assert.assertEquals(ImmutableSet.of("c", "d"), getRows(c, tableName));

    // this compaction should not drop files starting with A
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    Assert.assertEquals(ImmutableSet.of("c", "d"), getRows(c, tableName));
  }

  private void testDropNone(Map<String,String> options) throws Exception {

    Connector c = getConnector();

    String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);

    writeFlush(c, tableName, "a");
    writeFlush(c, tableName, "b");

    CompactionStrategyConfig csConfig = new CompactionStrategyConfig(TestCompactionStrategy.class.getName());
    csConfig.setOptions(options);
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig));

    Assert.assertEquals(ImmutableSet.of("a", "b"), getRows(c, tableName));
  }

  @Test
  public void testDropNone() throws Exception {
    // test a compaction strategy that selects no files. In this case there is no work to do, want to ensure it does not hang.

    testDropNone(ImmutableMap.of("inputPrefix", "Z"));
  }

  @Test
  public void testDropNone2() throws Exception {
    // test a compaction strategy that selects no files. This differs testDropNone() in that shouldCompact() will return true and getCompactionPlan() will
    // return no work to do.

    testDropNone(ImmutableMap.of("inputPrefix", "Z", "shouldCompact", "true"));
  }

  @Test
  public void testPerTableClasspath() throws Exception {
    // test pertable classpath + user specified compaction strat

    final Connector c = getConnector();
    final String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);
    c.instanceOperations().setProperty(Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey() + "context1",
        System.getProperty("user.dir") + "/src/test/resources/TestCompactionStrat.jar");
    c.tableOperations().setProperty(tableName, Property.TABLE_CLASSPATH.getKey(), "context1");

    c.tableOperations().addSplits(tableName, new TreeSet<Text>(Arrays.asList(new Text("efg"))));

    writeFlush(c, tableName, "a");
    writeFlush(c, tableName, "b");

    writeFlush(c, tableName, "h");
    writeFlush(c, tableName, "i");

    Assert.assertEquals(4, FunctionalTestUtils.countRFiles(c, tableName));

    // EfgCompactionStrat will only compact a tablet w/ end row of 'efg'. No other tablets are compacted.
    CompactionStrategyConfig csConfig = new CompactionStrategyConfig("org.apache.accumulo.test.EfgCompactionStrat");
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig));

    Assert.assertEquals(3, FunctionalTestUtils.countRFiles(c, tableName));

    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    Assert.assertEquals(2, FunctionalTestUtils.countRFiles(c, tableName));
  }

  @Test
  public void testIterators() throws Exception {
    // test compaction strategy + iterators

    Connector c = getConnector();

    String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);

    writeFlush(c, tableName, "a");
    writeFlush(c, tableName, "b");
    // create a file that starts with A containing rows 'a' and 'b'
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    writeFlush(c, tableName, "c");
    writeFlush(c, tableName, "d");

    Assert.assertEquals(3, FunctionalTestUtils.countRFiles(c, tableName));

    // drop files that start with A
    CompactionStrategyConfig csConfig = new CompactionStrategyConfig(TestCompactionStrategy.class.getName());
    csConfig.setOptions(ImmutableMap.of("inputPrefix", "F"));

    IteratorSetting iterConf = new IteratorSetting(21, "myregex", RegExFilter.class);
    RegExFilter.setRegexs(iterConf, "a|c", null, null, null, false);

    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig).setIterators(Arrays.asList(iterConf)));

    // compaction strategy should only be applied to one file. If its applied to both, then row 'b' would be dropped by filter.
    Assert.assertEquals(ImmutableSet.of("a", "b", "c"), getRows(c, tableName));

    Assert.assertEquals(2, FunctionalTestUtils.countRFiles(c, tableName));

    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    // ensure that iterator is not applied
    Assert.assertEquals(ImmutableSet.of("a", "b", "c"), getRows(c, tableName));

    Assert.assertEquals(1, FunctionalTestUtils.countRFiles(c, tableName));
  }

  @Test
  public void testFileSize() throws Exception {
    Connector c = getConnector();

    String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);

    // write random data because its very unlikely it will compress
    writeRandomValue(c, tableName, 1 << 16);
    writeRandomValue(c, tableName, 1 << 16);

    writeRandomValue(c, tableName, 1 << 9);
    writeRandomValue(c, tableName, 1 << 7);
    writeRandomValue(c, tableName, 1 << 6);

    Assert.assertEquals(5, FunctionalTestUtils.countRFiles(c, tableName));

    CompactionStrategyConfig csConfig = new CompactionStrategyConfig(SizeCompactionStrategy.class.getName());
    csConfig.setOptions(ImmutableMap.of("size", "" + (1 << 15)));
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig));

    Assert.assertEquals(3, FunctionalTestUtils.countRFiles(c, tableName));

    csConfig = new CompactionStrategyConfig(SizeCompactionStrategy.class.getName());
    csConfig.setOptions(ImmutableMap.of("size", "" + (1 << 17)));
    c.tableOperations().compact(tableName, new CompactionConfig().setWait(true).setCompactionStrategy(csConfig));

    Assert.assertEquals(1, FunctionalTestUtils.countRFiles(c, tableName));

  }

  void writeRandomValue(Connector c, String tableName, int size) throws Exception {
    Random rand = new Random();

    byte data1[] = new byte[size];
    rand.nextBytes(data1);

    BatchWriter bw = c.createBatchWriter(tableName, new BatchWriterConfig());

    Mutation m1 = new Mutation("r" + rand.nextInt(909090));
    m1.put("data", "bl0b", new Value(data1));

    bw.addMutation(m1);
    bw.close();
    c.tableOperations().flush(tableName, null, null, true);
  }

  private Set<String> getRows(Connector c, String tableName) throws TableNotFoundException {
    Set<String> rows = new HashSet<String>();
    Scanner scanner = c.createScanner(tableName, Authorizations.EMPTY);

    for (Entry<Key,Value> entry : scanner)
      rows.add(entry.getKey().getRowData().toString());
    return rows;

  }

  private void writeFlush(Connector conn, String tablename, String row) throws Exception {
    BatchWriter bw = conn.createBatchWriter(tablename, new BatchWriterConfig());
    Mutation m = new Mutation(row);
    m.put("", "", "");
    bw.addMutation(m);
    bw.close();
    conn.tableOperations().flush(tablename, null, null, true);
  }
}
diff --git a/test/src/test/java/org/apache/accumulo/test/functional/FunctionalTestUtils.java b/test/src/test/java/org/apache/accumulo/test/functional/FunctionalTestUtils.java
index 4e0721b73..e4e7229bd 100644
-- a/test/src/test/java/org/apache/accumulo/test/functional/FunctionalTestUtils.java
++ b/test/src/test/java/org/apache/accumulo/test/functional/FunctionalTestUtils.java
@@ -16,8 +16,6 @@
  */
 package org.apache.accumulo.test.functional;
 
import static org.junit.Assert.assertFalse;

 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
@@ -49,8 +47,19 @@ import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Text;
 
import static org.junit.Assert.assertFalse;

 public class FunctionalTestUtils {
 
  public static int countRFiles(Connector c, String tableName) throws Exception {
    Scanner scanner = c.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
    String tableId = c.tableOperations().tableIdMap().get(tableName);
    scanner.setRange(MetadataSchema.TabletsSection.getRange(tableId));
    scanner.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);

    return count(scanner);
  }

   static void checkRFiles(Connector c, String tableName, int minTablets, int maxTablets, int minRFiles, int maxRFiles) throws Exception {
     Scanner scanner = c.createScanner(MetadataTable.NAME, Authorizations.EMPTY);
     String tableId = c.tableOperations().tableIdMap().get(tableName);
- 
2.19.1.windows.1

