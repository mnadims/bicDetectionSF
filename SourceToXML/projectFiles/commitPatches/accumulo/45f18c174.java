From 45f18c174612d1a41eed1d2eec9e77d3b3e71a82 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Mon, 21 Sep 2015 09:44:47 -0400
Subject: [PATCH] ACCUMULO-3913 Added per table sampling

--
 .../client/ClientSideIteratorScanner.java     |  161 +-
 .../client/SampleNotPresentException.java     |   42 +
 .../accumulo/core/client/ScannerBase.java     |   46 +
 .../client/admin/NewTableConfiguration.java   |   31 +
 .../client/admin/SamplerConfiguration.java    |   91 +
 .../core/client/admin/TableOperations.java    |   29 +
 .../client/impl/BaseIteratorEnvironment.java  |   83 +
 .../core/client/impl/OfflineIterator.java     |   52 +-
 .../core/client/impl/ScannerIterator.java     |    6 +-
 .../core/client/impl/ScannerOptions.java      |   29 +-
 .../core/client/impl/TableOperationsImpl.java |   39 +
 .../impl/TabletServerBatchReaderIterator.java |   17 +-
 .../core/client/impl/ThriftScanner.java       |   25 +-
 .../client/mapred/AbstractInputFormat.java    |   10 +
 .../mapred/AccumuloFileOutputFormat.java      |   15 +
 .../core/client/mapred/InputFormatBase.java   |   19 +
 .../client/mapreduce/AbstractInputFormat.java |   10 +
 .../mapreduce/AccumuloFileOutputFormat.java   |   15 +
 .../client/mapreduce/InputFormatBase.java     |   19 +
 .../client/mapreduce/InputTableConfig.java    |   26 +
 .../client/mapreduce/RangeInputSplit.java     |   21 +
 .../client/mapreduce/impl/SplitUtils.java     |    2 +
 .../lib/impl/FileOutputConfigurator.java      |   29 +-
 .../mapreduce/lib/impl/InputConfigurator.java |   53 +-
 .../core/client/mock/MockScannerBase.java     |   16 +
 .../core/client/mock/MockTableOperations.java |   17 +
 .../core/compaction/CompactionSettings.java   |    1 +
 .../accumulo/core/compaction/NullType.java    |   29 +
 .../apache/accumulo/core/conf/Property.java   |   12 +-
 .../accumulo/core/file/BloomFilterLayer.java  |    6 +
 .../accumulo/core/file/FileSKVIterator.java   |    5 +-
 .../core/file/map/MapFileOperations.java      |    6 +
 .../core/file/rfile/MultiIndexIterator.java   |    6 +
 .../core/file/rfile/MultiLevelIndex.java      |    4 +-
 .../accumulo/core/file/rfile/PrintInfo.java   |   28 +-
 .../accumulo/core/file/rfile/RFile.java       |  511 ++++-
 .../core/file/rfile/RFileOperations.java      |   12 +-
 .../core/iterators/IteratorEnvironment.java   |   50 +
 .../core/iterators/SortedMapIterator.java     |    4 +
 .../core/iterators/WrappingIterator.java      |    8 -
 .../core/iterators/system/EmptyIterator.java  |   72 +
 .../iterators/system/MapFileIterator.java     |    6 +
 .../core/iterators/system/SampleIterator.java |   46 +
 .../system/SequenceFileIterator.java          |    6 +
 .../core/sample/AbstractHashSampler.java      |  106 +
 .../core/sample/RowColumnSampler.java         |  124 ++
 .../accumulo/core/sample/RowSampler.java      |   49 +
 .../apache/accumulo/core/sample/Sampler.java  |   57 +
 .../sample/impl/SamplerConfigurationImpl.java |  184 ++
 .../core/sample/impl/SamplerFactory.java      |   48 +
 .../thrift/TSampleNotPresentException.java    |  409 ++++
 .../thrift/TSamplerConfiguration.java         |  556 ++++++
 .../thrift/TabletClientService.java           | 1762 ++++++++++++-----
 .../accumulo/core/util/LocalityGroupUtil.java |    4 +-
 core/src/main/thrift/tabletserver.thrift      |   19 +-
 .../impl/TableOperationsHelperTest.java       |   17 +
 .../mapred/AccumuloFileOutputFormatTest.java  |   14 +-
 .../AccumuloFileOutputFormatTest.java         |   13 +
 .../core/file/rfile/MultiLevelIndexTest.java  |    3 +-
 .../accumulo/core/file/rfile/RFileTest.java   |  333 +++-
 .../iterators/DefaultIteratorEnvironment.java |   25 +-
 .../FirstEntryInRowIteratorTest.java          |   37 +-
 .../core/iterators/SortedMapIteratorTest.java |   46 +
 .../user/RowDeletingIteratorTest.java         |   30 +-
 .../user/RowEncodingIteratorTest.java         |   52 +-
 .../user/TransformingIteratorTest.java        |   41 +-
 .../apache/accumulo/core/file/rfile/ver_7.rf  |  Bin 0 -> 14557 bytes
 .../asciidoc/accumulo_user_manual.asciidoc    |    2 +
 docs/src/main/asciidoc/chapters/sampling.txt  |   86 +
 docs/src/main/resources/examples/README       |    2 +
 .../src/main/resources/examples/README.sample |  192 ++
 .../examples/simple/sample/SampleExample.java |  150 ++
 .../shard/CutoffIntersectingIterator.java     |  123 ++
 .../accumulo/examples/simple/shard/Query.java |   31 +-
 .../server/util/VerifyTabletAssignments.java  |    2 +-
 .../iterators/MetadataBulkLoadFilterTest.java |   25 +-
 .../replication/StatusCombinerTest.java       |   39 +-
 .../monitor/servlets/trace/NullScanner.java   |   11 +
 .../apache/accumulo/tserver/FileManager.java  |   30 +-
 .../apache/accumulo/tserver/InMemoryMap.java  |  244 ++-
 .../tserver/MemKeyConversionIterator.java     |    6 +-
 .../org/apache/accumulo/tserver/MemValue.java |   63 +-
 .../apache/accumulo/tserver/NativeMap.java    |    4 +
 .../tserver/TabletIteratorEnvironment.java    |   61 +-
 .../apache/accumulo/tserver/TabletServer.java |   38 +-
 .../ConfigurableCompactionStrategy.java       |   22 +
 .../accumulo/tserver/scan/LookupTask.java     |    5 +-
 .../accumulo/tserver/scan/NextBatchTask.java  |    5 +-
 .../tserver/session/MultiScanSession.java     |    5 +-
 .../tserver/tablet/ScanDataSource.java        |   24 +-
 .../accumulo/tserver/tablet/ScanOptions.java  |   16 +-
 .../accumulo/tserver/tablet/Tablet.java       |   12 +-
 .../accumulo/tserver/tablet/TabletMemory.java |    7 +-
 .../accumulo/tserver/InMemoryMapTest.java     |  383 +++-
 .../DefaultCompactionStrategyTest.java        |    6 +
 .../shell/commands/CompactCommand.java        |    6 +-
 .../accumulo/shell/commands/GrepCommand.java  |    2 +
 .../accumulo/shell/commands/ScanCommand.java  |   23 +
 start/.gitignore                              |    1 +
 .../test/InMemoryMapMemoryUsageTest.java      |    8 +-
 .../org/apache/accumulo/test/SampleIT.java    |  497 +++++
 .../apache/accumulo/test/ShellServerIT.java   |   72 +-
 .../accumulo/test/functional/ExamplesIT.java  |    4 +-
 .../accumulo/test/functional/ReadWriteIT.java |    4 +-
 .../mapred/AccumuloFileOutputFormatIT.java    |   18 +
 .../test/mapred/AccumuloInputFormatIT.java    |   57 +-
 .../mapreduce/AccumuloFileOutputFormatIT.java |   18 +
 .../test/mapreduce/AccumuloInputFormatIT.java |   49 +-
 .../test/performance/thrift/NullTserver.java  |    6 +-
 109 files changed, 6864 insertions(+), 1139 deletions(-)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/SampleNotPresentException.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/compaction/NullType.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/iterators/system/EmptyIterator.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/Sampler.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSampleNotPresentException.java
 create mode 100644 core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSamplerConfiguration.java
 create mode 100644 core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
 create mode 100644 core/src/test/resources/org/apache/accumulo/core/file/rfile/ver_7.rf
 create mode 100644 docs/src/main/asciidoc/chapters/sampling.txt
 create mode 100644 docs/src/main/resources/examples/README.sample
 create mode 100644 examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
 create mode 100644 examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
 create mode 100644 test/src/main/java/org/apache/accumulo/test/SampleIT.java

diff --git a/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java b/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
index eb3c923f7..5dc6d5961 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
@@ -27,6 +27,7 @@ import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ScannerOptions;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ArrayByteSequence;
@@ -44,6 +45,8 @@ import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.hadoop.io.Text;
 
import com.google.common.base.Preconditions;

 /**
  * A scanner that instantiates iterators on the client side instead of on the tablet server. This can be useful for testing iterators or in cases where you
  * don't want iterators affecting the performance of tablet servers.<br>
@@ -60,6 +63,7 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
   private Range range;
   private boolean isolated = false;
   private long readaheadThreshold = Constants.SCANNER_DEFAULT_READAHEAD_THRESHOLD;
  private SamplerConfiguration iteratorSamplerConfig;
 
   /**
    * @deprecated since 1.7.0 was never intended for public use. However this could have been used by anything extending this class.
@@ -67,7 +71,7 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
   @Deprecated
   public class ScannerTranslator extends ScannerTranslatorImpl {
     public ScannerTranslator(Scanner scanner) {
      super(scanner);
      super(scanner, scanner.getSamplerConfiguration());
     }
 
     @Override
@@ -76,6 +80,62 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
     }
   }
 
  private class ClientSideIteratorEnvironment implements IteratorEnvironment {

    private SamplerConfiguration samplerConfig;
    private boolean sampleEnabled;

    ClientSideIteratorEnvironment(boolean sampleEnabled, SamplerConfiguration samplerConfig) {
      this.sampleEnabled = sampleEnabled;
      this.samplerConfig = samplerConfig;
    }

    @Override
    public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public AccumuloConfiguration getConfig() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IteratorScope getIteratorScope() {
      return IteratorScope.scan;
    }

    @Override
    public boolean isFullMajorCompaction() {
      return false;
    }

    @Override
    public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Authorizations getAuthorizations() {
      return ClientSideIteratorScanner.this.getAuthorizations();
    }

    @Override
    public IteratorEnvironment cloneWithSamplingEnabled() {
      return new ClientSideIteratorEnvironment(true, samplerConfig);
    }

    @Override
    public boolean isSamplingEnabled() {
      return sampleEnabled;
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration() {
      return samplerConfig;
    }
  }

   /**
    * A class that wraps a Scanner in a SortedKeyValueIterator so that other accumulo iterators can use it as a source.
    */
@@ -83,6 +143,7 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
     protected Scanner scanner;
     Iterator<Entry<Key,Value>> iter;
     Entry<Key,Value> top = null;
    private SamplerConfiguration samplerConfig;
 
     /**
      * Constructs an accumulo iterator from a scanner.
@@ -90,8 +151,9 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
      * @param scanner
      *          the scanner to iterate over
      */
    public ScannerTranslatorImpl(final Scanner scanner) {
    public ScannerTranslatorImpl(final Scanner scanner, SamplerConfiguration samplerConfig) {
       this.scanner = scanner;
      this.samplerConfig = samplerConfig;
     }
 
     @Override
@@ -122,6 +184,13 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
       for (ByteSequence colf : columnFamilies) {
         scanner.fetchColumnFamily(new Text(colf.toArray()));
       }

      if (samplerConfig == null) {
        scanner.clearSamplerConfiguration();
      } else {
        scanner.setSamplerConfiguration(samplerConfig);
      }

       iter = scanner.iterator();
       next();
     }
@@ -138,7 +207,7 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
 
     @Override
     public SortedKeyValueIterator<Key,Value> deepCopy(final IteratorEnvironment env) {
      return new ScannerTranslatorImpl(scanner);
      return new ScannerTranslatorImpl(scanner, env.isSamplingEnabled() ? env.getSamplerConfiguration() : null);
     }
   }
 
@@ -151,19 +220,22 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
    *          the source scanner
    */
   public ClientSideIteratorScanner(final Scanner scanner) {
    smi = new ScannerTranslatorImpl(scanner);
    smi = new ScannerTranslatorImpl(scanner, scanner.getSamplerConfiguration());
     this.range = scanner.getRange();
     this.size = scanner.getBatchSize();
     this.timeOut = scanner.getTimeout(TimeUnit.MILLISECONDS);
     this.batchTimeOut = scanner.getTimeout(TimeUnit.MILLISECONDS);
     this.readaheadThreshold = scanner.getReadaheadThreshold();
    SamplerConfiguration samplerConfig = scanner.getSamplerConfiguration();
    if (samplerConfig != null)
      setSamplerConfiguration(samplerConfig);
   }
 
   /**
    * Sets the source Scanner.
    */
   public void setSource(final Scanner scanner) {
    smi = new ScannerTranslatorImpl(scanner);
    smi = new ScannerTranslatorImpl(scanner, scanner.getSamplerConfiguration());
   }
 
   @Override
@@ -177,6 +249,8 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
     else
       smi.scanner.disableIsolation();
 
    smi.samplerConfig = getSamplerConfiguration();

     final TreeMap<Integer,IterInfo> tm = new TreeMap<Integer,IterInfo>();
 
     for (IterInfo iterInfo : serverSideIteratorList) {
@@ -185,35 +259,8 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
 
     SortedKeyValueIterator<Key,Value> skvi;
     try {
      skvi = IteratorUtil.loadIterators(smi, tm.values(), serverSideIteratorOptions, new IteratorEnvironment() {
        @Override
        public SortedKeyValueIterator<Key,Value> reserveMapFileReader(final String mapFileName) throws IOException {
          return null;
        }

        @Override
        public AccumuloConfiguration getConfig() {
          return null;
        }

        @Override
        public IteratorScope getIteratorScope() {
          return null;
        }

        @Override
        public boolean isFullMajorCompaction() {
          return false;
        }

        @Override
        public void registerSideChannel(final SortedKeyValueIterator<Key,Value> iter) {}

        @Override
        public Authorizations getAuthorizations() {
          return smi.scanner.getAuthorizations();
        }
      }, false, null);
      skvi = IteratorUtil.loadIterators(smi, tm.values(), serverSideIteratorOptions, new ClientSideIteratorEnvironment(getSamplerConfiguration() != null,
          getIteratorSamplerConfigurationInternal()), false, null);
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
@@ -297,4 +344,50 @@ public class ClientSideIteratorScanner extends ScannerOptions implements Scanner
     }
     this.readaheadThreshold = batches;
   }

  private SamplerConfiguration getIteratorSamplerConfigurationInternal() {
    SamplerConfiguration scannerSamplerConfig = getSamplerConfiguration();
    if (scannerSamplerConfig != null) {
      if (iteratorSamplerConfig != null && !iteratorSamplerConfig.equals(scannerSamplerConfig)) {
        throw new IllegalStateException("Scanner and iterator sampler configuration differ");
      }

      return scannerSamplerConfig;
    }

    return iteratorSamplerConfig;
  }

  /**
   * This is provided for the case where no sampler configuration is set on the scanner, but there is a need to create iterator deep copies that have sampling
   * enabled. If sampler configuration is set on the scanner, then this method does not need to be called inorder to create deep copies with sampling.
   *
   * <p>
   * Setting this differently than the scanners sampler configuration may cause exceptions.
   *
   * @since 1.8.0
   */
  public void setIteratorSamplerConfiguration(SamplerConfiguration sc) {
    Preconditions.checkNotNull(sc);
    this.iteratorSamplerConfig = sc;
  }

  /**
   * Clear any iterator sampler configuration.
   *
   * @since 1.8.0
   */
  public void clearIteratorSamplerConfiguration() {
    this.iteratorSamplerConfig = null;
  }

  /**
   * @return currently set iterator sampler configuration.
   *
   * @since 1.8.0
   */

  public SamplerConfiguration getIteratorSamplerConfiguration() {
    return iteratorSamplerConfig;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/SampleNotPresentException.java b/core/src/main/java/org/apache/accumulo/core/client/SampleNotPresentException.java
new file mode 100644
index 000000000..c70a89859
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/SampleNotPresentException.java
@@ -0,0 +1,42 @@
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

package org.apache.accumulo.core.client;

/**
 * Exception thrown when a table does not have sampling configured or when sampling is configured but it differs from what was requested.
 *
 * @since 1.8.0
 */

public class SampleNotPresentException extends RuntimeException {

  public SampleNotPresentException(String message, Exception cause) {
    super(message, cause);
  }

  public SampleNotPresentException(String message) {
    super(message);
  }

  public SampleNotPresentException() {
    super();
  }

  private static final long serialVersionUID = 1L;

}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java b/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
index e9d288bd0..564278581 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
@@ -21,6 +21,7 @@ import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
@@ -175,6 +176,51 @@ public interface ScannerBase extends Iterable<Entry<Key,Value>> {
    */
   Authorizations getAuthorizations();
 
  /**
   * Setting this will cause the scanner to read sample data, as long as that sample data was generated with the given configuration. By default this is not set
   * and all data is read.
   *
   * <p>
   * One way to use this method is as follows, where the sampler configuration is obtained from the table configuration. Sample data can be generated in many
   * different ways, so its important to verify the sample data configuration meets expectations.
   *
   * <p>
   *
   * <pre>
   * <code>
   *   // could cache this if creating many scanners to avoid RPCs.
   *   SamplerConfiguration samplerConfig = connector.tableOperations().getSamplerConfiguration(table);
   *   // verify table's sample data is generated in an expected way before using
   *   userCode.verifySamplerConfig(samplerConfig);
   *   scanner.setSamplerCongiguration(samplerConfig);
   * </code>
   * </pre>
   *
   * <p>
   * Of course this is not the only way to obtain a {@link SamplerConfiguration}, it could be a constant, configuration, etc.
   *
   * <p>
   * If sample data is not present or sample data was generated with a different configuration, then the scanner iterator will throw a
   * {@link SampleNotPresentException}. Also if a table's sampler configuration is changed while a scanner is iterating over a table, a
   * {@link SampleNotPresentException} may be thrown.
   *
   * @since 1.8.0
   */
  void setSamplerConfiguration(SamplerConfiguration samplerConfig);

  /**
   * @return currently set sampler configuration. Returns null if no sampler configuration is set.
   * @since 1.8.0
   */
  SamplerConfiguration getSamplerConfiguration();

  /**
   * Clears sampler configuration making a scanner read all data. After calling this, {@link #getSamplerConfiguration()} should return null.
   *
   * @since 1.8.0
   */
  void clearSamplerConfiguration();

   /**
    * This setting determines how long a scanner will wait to fill the returned batch. By default, a scanner wait until the batch is full.
    *
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
index 4db1d8963..2107dc8cf 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
@@ -24,6 +24,9 @@ import java.util.Map;
 
 import org.apache.accumulo.core.iterators.IteratorUtil;
 import org.apache.accumulo.core.iterators.user.VersioningIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;

import com.google.common.base.Preconditions;
 
 /**
  * This object stores table creation parameters. Currently includes: {@link TimeType}, whether to include default iterators, and user-specified initial
@@ -39,6 +42,7 @@ public class NewTableConfiguration {
   private boolean limitVersion = true;
 
   private Map<String,String> properties = new HashMap<String,String>();
  private SamplerConfiguration samplerConfiguration;
 
   /**
    * Configure logical or millisecond time for tables created with this configuration.
@@ -84,6 +88,7 @@ public class NewTableConfiguration {
    */
   public NewTableConfiguration setProperties(Map<String,String> prop) {
     checkArgument(prop != null, "properties is null");
    checkDisjoint(prop, samplerConfiguration);
 
     this.properties = new HashMap<String,String>(prop);
     return this;
@@ -101,7 +106,33 @@ public class NewTableConfiguration {
       propertyMap.putAll(IteratorUtil.generateInitialTableProperties(limitVersion));
     }
 
    if (samplerConfiguration != null) {
      propertyMap.putAll(new SamplerConfigurationImpl(samplerConfiguration).toTablePropertiesMap());
    }

     propertyMap.putAll(properties);
     return Collections.unmodifiableMap(propertyMap);
   }

  private void checkDisjoint(Map<String,String> props, SamplerConfiguration samplerConfiguration) {
    if (props.isEmpty() || samplerConfiguration == null) {
      return;
    }

    Map<String,String> sampleProps = new SamplerConfigurationImpl(samplerConfiguration).toTablePropertiesMap();

    checkArgument(Collections.disjoint(props.keySet(), sampleProps.keySet()), "Properties and derived sampler properties are not disjoint");
  }

  /**
   * Enable building a sample data set on the new table using the given sampler configuration.
   *
   * @since 1.8.0
   */
  public NewTableConfiguration enableSampling(SamplerConfiguration samplerConfiguration) {
    Preconditions.checkNotNull(samplerConfiguration);
    checkDisjoint(properties, samplerConfiguration);
    this.samplerConfiguration = samplerConfiguration;
    return this;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java
new file mode 100644
index 000000000..079d324a4
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java
@@ -0,0 +1,91 @@
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;

/**
 * This class encapsultes configuration and options needed to setup and use sampling.
 *
 * @since 1.8.0
 */

public class SamplerConfiguration {

  private String className;
  private Map<String,String> options = new HashMap<>();

  public SamplerConfiguration(String samplerClassName) {
    Preconditions.checkNotNull(samplerClassName);
    this.className = samplerClassName;
  }

  public SamplerConfiguration setOptions(Map<String,String> options) {
    Preconditions.checkNotNull(options);
    this.options = new HashMap<>(options.size());

    for (Entry<String,String> entry : options.entrySet()) {
      addOption(entry.getKey(), entry.getValue());
    }

    return this;
  }

  public SamplerConfiguration addOption(String option, String value) {
    checkArgument(option != null, "option is null");
    checkArgument(value != null, "value is null");
    this.options.put(option, value);
    return this;
  }

  public Map<String,String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  public String getSamplerClassName() {
    return className;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SamplerConfiguration) {
      SamplerConfiguration osc = (SamplerConfiguration) o;

      return className.equals(osc.className) && options.equals(osc.options);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return className.hashCode() + 31 * options.hashCode();
  }

  @Override
  public String toString() {
    return className + " " + options;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
index b7c70e986..fa6fef40e 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
@@ -756,4 +756,33 @@ public interface TableOperations {
    */
   boolean testClassLoad(String tableName, final String className, final String asTypeName) throws AccumuloException, AccumuloSecurityException,
       TableNotFoundException;

  /**
   * Set or update the sampler configuration for a table. If the table has existing sampler configuration, those properties will be cleared before setting the
   * new table properties.
   *
   * @param tableName
   *          the name of the table
   * @since 1.8.0
   */
  void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws TableNotFoundException, AccumuloException,
      AccumuloSecurityException;

  /**
   * Clear all sampling configuration properties on the table.
   *
   * @param tableName
   *          the name of the table
   * @since 1.8.0
   */
  void clearSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException;

  /**
   * Reads the sampling configuration properties for a table.
   *
   * @param tableName
   *          the name of the table
   * @since 1.8.0
   */
  SamplerConfiguration getSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException;
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java b/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
new file mode 100644
index 000000000..dc138ceb7
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
@@ -0,0 +1,83 @@
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

import java.io.IOException;

import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.security.Authorizations;

/**
 * An implementation of {@link IteratorEnvironment} that throws {@link UnsupportedOperationException} for each operation. This is useful for situations that
 * need to extend {@link IteratorEnvironment} and implement a subset of the methods.
 */

public class BaseIteratorEnvironment implements IteratorEnvironment {

  @Override
  public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AccumuloConfiguration getConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorScope getIteratorScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isFullMajorCompaction() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Authorizations getAuthorizations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSamplingEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SamplerConfiguration getSamplerConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorEnvironment cloneWithSamplingEnabled() {
    throw new UnsupportedOperationException();
  }

}
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
index 793b04481..9cce089be 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
@@ -16,6 +16,8 @@
  */
 package org.apache.accumulo.core.client.impl;
 
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
@@ -30,8 +32,10 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
@@ -57,6 +61,7 @@ import org.apache.accumulo.core.master.state.tables.TableState;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.ColumnVisibility;
 import org.apache.accumulo.core.util.CachedConfiguration;
@@ -68,16 +73,20 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.io.Text;
 
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

 class OfflineIterator implements Iterator<Entry<Key,Value>> {
 
   static class OfflineIteratorEnvironment implements IteratorEnvironment {
 
     private final Authorizations authorizations;
    private AccumuloConfiguration conf;
    private boolean useSample;
    private SamplerConfiguration sampleConf;
 
    public OfflineIteratorEnvironment(Authorizations auths) {
    public OfflineIteratorEnvironment(Authorizations auths, AccumuloConfiguration acuTableConf, boolean useSample, SamplerConfiguration samplerConf) {
       this.authorizations = auths;
      this.conf = acuTableConf;
      this.useSample = useSample;
      this.sampleConf = samplerConf;
     }
 
     @Override
@@ -87,7 +96,7 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
 
     @Override
     public AccumuloConfiguration getConfig() {
      return AccumuloConfiguration.getDefaultConfiguration();
      return conf;
     }
 
     @Override
@@ -119,6 +128,23 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
       allIters.add(iter);
       return new MultiIterator(allIters, false);
     }

    @Override
    public boolean isSamplingEnabled() {
      return useSample;
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration() {
      return sampleConf;
    }

    @Override
    public IteratorEnvironment cloneWithSamplingEnabled() {
      if (sampleConf == null)
        throw new SampleNotPresentException();
      return new OfflineIteratorEnvironment(authorizations, conf, true, sampleConf);
    }
   }
 
   private SortedKeyValueIterator<Key,Value> iter;
@@ -154,6 +180,8 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
         nextTablet();
 
     } catch (Exception e) {
      if (e instanceof RuntimeException)
        throw (RuntimeException) e;
       throw new RuntimeException(e);
     }
   }
@@ -306,16 +334,30 @@ class OfflineIterator implements Iterator<Entry<Key,Value>> {
 
     readers.clear();
 
    SamplerConfiguration scannerSamplerConfig = options.getSamplerConfiguration();
    SamplerConfigurationImpl scannerSamplerConfigImpl = scannerSamplerConfig == null ? null : new SamplerConfigurationImpl(scannerSamplerConfig);
    SamplerConfigurationImpl samplerConfImpl = SamplerConfigurationImpl.newSamplerConfig(acuTableConf);

    if (scannerSamplerConfigImpl != null && ((samplerConfImpl != null && !scannerSamplerConfigImpl.equals(samplerConfImpl)) || samplerConfImpl == null)) {
      throw new SampleNotPresentException();
    }

     // TODO need to close files - ACCUMULO-1303
     for (String file : absFiles) {
       FileSystem fs = VolumeConfiguration.getVolume(file, conf, config).getFileSystem();
       FileSKVIterator reader = FileOperations.getInstance().openReader(file, false, fs, conf, acuTableConf, null, null);
      if (scannerSamplerConfigImpl != null) {
        reader = reader.getSample(scannerSamplerConfigImpl);
        if (reader == null)
          throw new SampleNotPresentException();
      }
       readers.add(reader);
     }
 
     MultiIterator multiIter = new MultiIterator(readers, extent);
 
    OfflineIteratorEnvironment iterEnv = new OfflineIteratorEnvironment(authorizations);
    OfflineIteratorEnvironment iterEnv = new OfflineIteratorEnvironment(authorizations, acuTableConf, false, samplerConfImpl == null ? null
        : samplerConfImpl.toSamplerConfiguration());
 
     DeletingIterator delIter = new DeletingIterator(multiIter, false);
 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
index 764db2156..55b0a851c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerIterator.java
@@ -28,6 +28,7 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
@@ -90,7 +91,8 @@ public class ScannerIterator implements Iterator<Entry<Key,Value>> {
           synchQ.add(currentBatch);
           return;
         }
      } catch (IsolationException | ScanTimedOutException | AccumuloException | AccumuloSecurityException | TableDeletedException | TableOfflineException e) {
      } catch (IsolationException | ScanTimedOutException | AccumuloException | AccumuloSecurityException | TableDeletedException | TableOfflineException
          | SampleNotPresentException e) {
         log.trace("{}", e.getMessage(), e);
         synchQ.add(e);
       } catch (TableNotFoundException e) {
@@ -119,7 +121,7 @@ public class ScannerIterator implements Iterator<Entry<Key,Value>> {
     }
 
     scanState = new ScanState(context, tableId, authorizations, new Range(range), options.fetchedColumns, size, options.serverSideIteratorList,
        options.serverSideIteratorOptions, isolated, readaheadThreshold, options.batchTimeOut);
        options.serverSideIteratorOptions, isolated, readaheadThreshold, options.getSamplerConfiguration(), options.batchTimeOut);
 
     // If we want to start readahead immediately, don't wait for hasNext to be called
     if (0l == readaheadThreshold) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
index cc337dd7b..8d9646406 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
@@ -32,6 +32,7 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -40,6 +41,8 @@ import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.TextUtil;
 import org.apache.hadoop.io.Text;
 
import com.google.common.base.Preconditions;

 public class ScannerOptions implements ScannerBase {
 
   protected List<IterInfo> serverSideIteratorList = Collections.emptyList();
@@ -53,6 +56,8 @@ public class ScannerOptions implements ScannerBase {
 
   private String regexIterName = null;
 
  private SamplerConfiguration samplerConfig = null;

   protected ScannerOptions() {}
 
   public ScannerOptions(ScannerOptions so) {
@@ -168,6 +173,8 @@ public class ScannerOptions implements ScannerBase {
         Set<Entry<String,Map<String,String>>> es = src.serverSideIteratorOptions.entrySet();
         for (Entry<String,Map<String,String>> entry : es)
           dst.serverSideIteratorOptions.put(entry.getKey(), new HashMap<String,String>(entry.getValue()));

        dst.samplerConfig = src.samplerConfig;
         dst.batchTimeOut = src.batchTimeOut;
       }
     }
@@ -179,7 +186,7 @@ public class ScannerOptions implements ScannerBase {
   }
 
   @Override
  public void setTimeout(long timeout, TimeUnit timeUnit) {
  public synchronized void setTimeout(long timeout, TimeUnit timeUnit) {
     if (timeOut < 0) {
       throw new IllegalArgumentException("TimeOut must be positive : " + timeOut);
     }
@@ -191,7 +198,7 @@ public class ScannerOptions implements ScannerBase {
   }
 
   @Override
  public long getTimeout(TimeUnit timeunit) {
  public synchronized long getTimeout(TimeUnit timeunit) {
     return timeunit.convert(timeOut, TimeUnit.MILLISECONDS);
   }
 
@@ -201,10 +208,26 @@ public class ScannerOptions implements ScannerBase {
   }
 
   @Override
  public Authorizations getAuthorizations() {
  public synchronized Authorizations getAuthorizations() {
     throw new UnsupportedOperationException("No authorizations to return");
   }
 
  @Override
  public synchronized void setSamplerConfiguration(SamplerConfiguration samplerConfig) {
    Preconditions.checkNotNull(samplerConfig);
    this.samplerConfig = samplerConfig;
  }

  @Override
  public synchronized SamplerConfiguration getSamplerConfiguration() {
    return samplerConfig;
  }

  @Override
  public synchronized void clearSamplerConfiguration() {
    this.samplerConfig = null;
  }

   @Override
   public void setBatchTimeout(long timeout, TimeUnit timeUnit) {
     if (timeOut < 0) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
index d65bcec90..8434f2f57 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
@@ -67,6 +67,7 @@ import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.FindMax;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
@@ -95,6 +96,7 @@ import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.RootTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
@@ -1474,4 +1476,41 @@ public class TableOperationsImpl extends TableOperationsHelper {
     }
   }
 
  private void clearSamplerOptions(String tableName) throws AccumuloException, TableNotFoundException, AccumuloSecurityException {
    String prefix = Property.TABLE_SAMPLER_OPTS.getKey();
    for (Entry<String,String> entry : getProperties(tableName)) {
      String property = entry.getKey();
      if (property.startsWith(prefix)) {
        removeProperty(tableName, property);
      }
    }
  }

  @Override
  public void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws AccumuloException, TableNotFoundException,
      AccumuloSecurityException {
    clearSamplerOptions(tableName);

    List<Pair<String,String>> props = new SamplerConfigurationImpl(samplerConfiguration).toTableProperties();
    for (Pair<String,String> pair : props) {
      setProperty(tableName, pair.getFirst(), pair.getSecond());
    }
  }

  @Override
  public void clearSamplerConfiguration(String tableName) throws AccumuloException, TableNotFoundException, AccumuloSecurityException {
    removeProperty(tableName, Property.TABLE_SAMPLER.getKey());
    clearSamplerOptions(tableName);
  }

  @Override
  public SamplerConfiguration getSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException {
    AccumuloConfiguration conf = new ConfigurationCopy(this.getProperties(tableName));
    SamplerConfigurationImpl sci = SamplerConfigurationImpl.newSamplerConfig(conf);
    if (sci == null) {
      return null;
    }
    return sci.toSamplerConfiguration();
  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
index 0b0980821..1ff56b979 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TabletServerBatchReaderIterator.java
@@ -39,6 +39,7 @@ import java.util.concurrent.atomic.AtomicLong;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
@@ -56,8 +57,10 @@ import org.apache.accumulo.core.data.thrift.TKeyValue;
 import org.apache.accumulo.core.data.thrift.TRange;
 import org.apache.accumulo.core.master.state.tables.TableState;
 import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.tabletserver.thrift.NoSuchScanIDException;
import org.apache.accumulo.core.tabletserver.thrift.TSampleNotPresentException;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.trace.Tracer;
 import org.apache.accumulo.core.util.ByteBufferUtil;
@@ -375,6 +378,8 @@ public class TabletServerBatchReaderIterator implements Iterator<Entry<Key,Value
           fatalException = new TableDeletedException(table);
         else
           fatalException = e;
      } catch (SampleNotPresentException e) {
        fatalException = e;
       } catch (Throwable t) {
         if (queryThreadPool.isShutdown())
           log.debug("Caught exception, but queryThreadPool is shutdown", t);
@@ -643,7 +648,8 @@ public class TabletServerBatchReaderIterator implements Iterator<Entry<Key,Value
             Translators.RT));
         InitialMultiScan imsr = client.startMultiScan(Tracer.traceInfo(), context.rpcCreds(), thriftTabletRanges,
             Translator.translate(columns, Translators.CT), options.serverSideIteratorList, options.serverSideIteratorOptions,
            ByteBufferUtil.toByteBuffers(authorizations.getAuthorizations()), waitForWrites, options.batchTimeOut);
            ByteBufferUtil.toByteBuffers(authorizations.getAuthorizations()), waitForWrites,
            SamplerConfigurationImpl.toThrift(options.getSamplerConfiguration()), options.batchTimeOut);
         if (waitForWrites)
           ThriftScanner.serversWaitedForWrites.get(ttype).add(server.toString());
 
@@ -719,6 +725,15 @@ public class TabletServerBatchReaderIterator implements Iterator<Entry<Key,Value
     } catch (NoSuchScanIDException e) {
       log.debug("Server : {} msg : {}", server, e.getMessage(), e);
       throw new IOException(e);
    } catch (TSampleNotPresentException e) {
      log.debug("Server : " + server + " msg : " + e.getMessage(), e);
      String tableInfo = "?";
      if (e.getExtent() != null) {
        String tableId = new KeyExtent(e.getExtent()).getTableId().toString();
        tableInfo = Tables.getPrintableTableInfoFromId(context.getInstance(), tableId);
      }
      String message = "Table " + tableInfo + " does not have sampling configured or built";
      throw new SampleNotPresentException(message, e);
     } catch (TException e) {
       log.debug("Server : {} msg : {}", server, e.getMessage(), e);
       timeoutTracker.errorOccured(e);
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
index c2cc1e3a2..52f333015 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
@@ -32,9 +32,11 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.data.Column;
@@ -50,9 +52,11 @@ import org.apache.accumulo.core.data.thrift.ScanResult;
 import org.apache.accumulo.core.data.thrift.TKeyValue;
 import org.apache.accumulo.core.master.state.tables.TableState;
 import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.tabletserver.thrift.NoSuchScanIDException;
 import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
import org.apache.accumulo.core.tabletserver.thrift.TSampleNotPresentException;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException;
 import org.apache.accumulo.core.trace.Span;
@@ -92,13 +96,13 @@ public class ThriftScanner {
       try {
         // not reading whole rows (or stopping on row boundries) so there is no need to enable isolation below
         ScanState scanState = new ScanState(context, extent.getTableId(), authorizations, range, fetchedColumns, size, serverSideIteratorList,
            serverSideIteratorOptions, false, Constants.SCANNER_DEFAULT_READAHEAD_THRESHOLD, batchTimeOut);
            serverSideIteratorOptions, false, Constants.SCANNER_DEFAULT_READAHEAD_THRESHOLD, null, batchTimeOut);
 
         TabletType ttype = TabletType.type(extent);
         boolean waitForWrites = !serversWaitedForWrites.get(ttype).contains(server);
         InitialScan isr = client.startScan(tinfo, scanState.context.rpcCreds(), extent.toThrift(), scanState.range.toThrift(),
             Translator.translate(scanState.columns, Translators.CT), scanState.size, scanState.serverSideIteratorList, scanState.serverSideIteratorOptions,
            scanState.authorizations.getAuthorizationsBB(), waitForWrites, scanState.isolated, scanState.readaheadThreshold, scanState.batchTimeOut);
            scanState.authorizations.getAuthorizationsBB(), waitForWrites, scanState.isolated, scanState.readaheadThreshold, null, scanState.batchTimeOut);
         if (waitForWrites)
           serversWaitedForWrites.get(ttype).add(server);
 
@@ -153,9 +157,11 @@ public class ThriftScanner {
 
     Map<String,Map<String,String>> serverSideIteratorOptions;
 
    SamplerConfiguration samplerConfig;

     public ScanState(ClientContext context, Text tableId, Authorizations authorizations, Range range, SortedSet<Column> fetchedColumns, int size,
         List<IterInfo> serverSideIteratorList, Map<String,Map<String,String>> serverSideIteratorOptions, boolean isolated, long readaheadThreshold,
        long batchTimeOut) {
        SamplerConfiguration samplerConfig, long batchTimeOut) {
       this.context = context;
 
       this.authorizations = authorizations;
@@ -183,6 +189,9 @@ public class ThriftScanner {
 
       this.isolated = isolated;
       this.readaheadThreshold = readaheadThreshold;

      this.samplerConfig = samplerConfig;

       this.batchTimeOut = batchTimeOut;
     }
   }
@@ -288,6 +297,10 @@ public class ThriftScanner {
           throw e;
         } catch (TApplicationException tae) {
           throw new AccumuloServerException(loc.tablet_location, tae);
        } catch (TSampleNotPresentException tsnpe) {
          String message = "Table " + Tables.getPrintableTableInfoFromId(instance, scanState.tableId.toString())
              + " does not have sampling configured or built";
          throw new SampleNotPresentException(message, tsnpe);
         } catch (NotServingTabletException e) {
           error = "Scan failed, not serving tablet " + loc;
           if (!error.equals(lastError))
@@ -377,7 +390,7 @@ public class ThriftScanner {
   }
 
   private static List<KeyValue> scan(TabletLocation loc, ScanState scanState, ClientContext context) throws AccumuloSecurityException,
      NotServingTabletException, TException, NoSuchScanIDException, TooManyFilesException {
      NotServingTabletException, TException, NoSuchScanIDException, TooManyFilesException, TSampleNotPresentException {
     if (scanState.finished)
       return null;
 
@@ -408,9 +421,11 @@ public class ThriftScanner {
 
         TabletType ttype = TabletType.type(loc.tablet_extent);
         boolean waitForWrites = !serversWaitedForWrites.get(ttype).contains(loc.tablet_location);

         InitialScan is = client.startScan(tinfo, scanState.context.rpcCreds(), loc.tablet_extent.toThrift(), scanState.range.toThrift(),
             Translator.translate(scanState.columns, Translators.CT), scanState.size, scanState.serverSideIteratorList, scanState.serverSideIteratorOptions,
            scanState.authorizations.getAuthorizationsBB(), waitForWrites, scanState.isolated, scanState.readaheadThreshold, scanState.batchTimeOut);
            scanState.authorizations.getAuthorizationsBB(), waitForWrites, scanState.isolated, scanState.readaheadThreshold,
            SamplerConfigurationImpl.toThrift(scanState.samplerConfig), scanState.batchTimeOut);
         if (waitForWrites)
           serversWaitedForWrites.get(ttype).add(loc.tablet_location);
 
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
index d11639e9b..b581deb28 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
@@ -43,6 +43,7 @@ import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
 import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.client.impl.ClientContext;
@@ -574,6 +575,15 @@ public abstract class AbstractInputFormat<K,V> implements InputFormat<K,V> {
         }
       }
 
      SamplerConfiguration samplerConfig = baseSplit.getSamplerConfiguration();
      if (null == samplerConfig) {
        samplerConfig = tableConfig.getSamplerConfiguration();
      }

      if (samplerConfig != null) {
        scannerBase.setSamplerConfiguration(samplerConfig);
      }

       scannerIterator = scannerBase.iterator();
       numKeysRead = 0;
     }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
index 0eb304f4f..b383f3e23 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.core.client.mapred;
 import java.io.IOException;
 import java.util.Arrays;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
@@ -140,6 +141,20 @@ public class AccumuloFileOutputFormat extends FileOutputFormat<Key,Value> {
     FileOutputConfigurator.setReplication(CLASS, job, replication);
   }
 
  /**
   * Specify a sampler to be used when writing out data. This will result in the output file having sample data.
   *
   * @param job
   *          The Hadoop job instance to be configured
   * @param samplerConfig
   *          The configuration for creating sample data in the output file.
   * @since 1.8.0
   */

  public static void setSampler(JobConf job, SamplerConfiguration samplerConfig) {
    FileOutputConfigurator.setSampler(CLASS, job, samplerConfig);
  }

   @Override
   public RecordWriter<Key,Value> getRecordWriter(FileSystem ignored, JobConf job, String name, Progressable progress) throws IOException {
     // get the path of the temporary output file
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
index ffb02a9d4..a9403a509 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
@@ -25,7 +25,9 @@ import org.apache.accumulo.core.client.ClientSideIteratorScanner;
 import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
 import org.apache.accumulo.core.data.Key;
@@ -337,6 +339,23 @@ public abstract class InputFormatBase<K,V> extends AbstractInputFormat<K,V> {
     return InputConfigurator.isBatchScan(CLASS, job);
   }
 
  /**
   * Causes input format to read sample data. If sample data was created using a different configuration or a tables sampler configuration changes while reading
   * data, then the input format will throw an error.
   *
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param samplerConfig
   *          The sampler configuration that sample must have been created with inorder for reading sample data to succeed.
   *
   * @since 1.8.0
   * @see ScannerBase#setSamplerConfiguration(SamplerConfiguration)
   */
  public static void setSamplerConfiguration(JobConf job, SamplerConfiguration samplerConfig) {
    InputConfigurator.setSamplerConfiguration(CLASS, job, samplerConfig);
  }

   /**
    * Initializes an Accumulo {@link org.apache.accumulo.core.client.impl.TabletLocator} based on the configuration.
    *
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
index 7db67c7b6..0e51f037b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
@@ -43,6 +43,7 @@ import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
 import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.client.impl.ClientContext;
@@ -604,6 +605,15 @@ public abstract class AbstractInputFormat<K,V> extends InputFormat<K,V> {
         }
       }
 
      SamplerConfiguration samplerConfig = split.getSamplerConfiguration();
      if (null == samplerConfig) {
        samplerConfig = tableConfig.getSamplerConfiguration();
      }

      if (samplerConfig != null) {
        scannerBase.setSamplerConfiguration(samplerConfig);
      }

       scannerIterator = scannerBase.iterator();
       numKeysRead = 0;
     }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
index abd96b6ef..7d4c0e215 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
@@ -19,6 +19,7 @@ package org.apache.accumulo.core.client.mapreduce;
 import java.io.IOException;
 import java.util.Arrays;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
@@ -138,6 +139,20 @@ public class AccumuloFileOutputFormat extends FileOutputFormat<Key,Value> {
     FileOutputConfigurator.setReplication(CLASS, job.getConfiguration(), replication);
   }
 
  /**
   * Specify a sampler to be used when writing out data. This will result in the output file having sample data.
   *
   * @param job
   *          The Hadoop job instance to be configured
   * @param samplerConfig
   *          The configuration for creating sample data in the output file.
   * @since 1.8.0
   */

  public static void setSampler(Job job, SamplerConfiguration samplerConfig) {
    FileOutputConfigurator.setSampler(CLASS, job.getConfiguration(), samplerConfig);
  }

   @Override
   public RecordWriter<Key,Value> getRecordWriter(TaskAttemptContext context) throws IOException {
     // get the path of the temporary output file
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 6ab8a1926..e5a0b90c0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -25,7 +25,9 @@ import org.apache.accumulo.core.client.ClientSideIteratorScanner;
 import org.apache.accumulo.core.client.IsolatedScanner;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
 import org.apache.accumulo.core.data.Key;
@@ -336,6 +338,23 @@ public abstract class InputFormatBase<K,V> extends AbstractInputFormat<K,V> {
     return InputConfigurator.isBatchScan(CLASS, context.getConfiguration());
   }
 
  /**
   * Causes input format to read sample data. If sample data was created using a different configuration or a tables sampler configuration changes while reading
   * data, then the input format will throw an error.
   *
   *
   * @param job
   *          the Hadoop job instance to be configured
   * @param samplerConfig
   *          The sampler configuration that sample must have been created with inorder for reading sample data to succeed.
   *
   * @since 1.8.0
   * @see ScannerBase#setSamplerConfiguration(SamplerConfiguration)
   */
  public static void setSamplerConfiguration(Job job, SamplerConfiguration samplerConfig) {
    InputConfigurator.setSamplerConfiguration(CLASS, job.getConfiguration(), samplerConfig);
  }

   /**
    * Initializes an Accumulo {@link org.apache.accumulo.core.client.impl.TabletLocator} based on the configuration.
    *
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
index 257f6c910..51ad7ebf8 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
@@ -25,6 +25,8 @@ import java.util.HashSet;
 import java.util.List;
 
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.hadoop.io.Text;
@@ -43,6 +45,7 @@ public class InputTableConfig implements Writable {
   private boolean useLocalIterators = false;
   private boolean useIsolatedScanners = false;
   private boolean offlineScan = false;
  private SamplerConfiguration samplerConfig = null;
 
   public InputTableConfig() {}
 
@@ -241,6 +244,26 @@ public class InputTableConfig implements Writable {
     return useIsolatedScanners;
   }
 
  /**
   * Set the sampler configuration to use when reading from the data.
   *
   * @see ScannerBase#setSamplerConfiguration(SamplerConfiguration)
   * @see InputFormatBase#setSamplerConfiguration(org.apache.hadoop.mapreduce.Job, SamplerConfiguration)
   *
   * @since 1.8.0
   */
  public void setSamplerConfiguration(SamplerConfiguration samplerConfiguration) {
    this.samplerConfig = samplerConfiguration;
  }

  /**
   *
   * @since 1.8.0
   */
  public SamplerConfiguration getSamplerConfiguration() {
    return samplerConfig;
  }

   @Override
   public void write(DataOutput dataOutput) throws IOException {
     if (iterators != null) {
@@ -340,6 +363,8 @@ public class InputTableConfig implements Writable {
       return false;
     if (ranges != null ? !ranges.equals(that.ranges) : that.ranges != null)
       return false;
    if (samplerConfig != null ? !samplerConfig.equals(that.samplerConfig) : that.samplerConfig != null)
      return false;
     return true;
   }
 
@@ -352,6 +377,7 @@ public class InputTableConfig implements Writable {
     result = 31 * result + (useLocalIterators ? 1 : 0);
     result = 31 * result + (useIsolatedScanners ? 1 : 0);
     result = 31 * result + (offlineScan ? 1 : 0);
    result = 31 * result + (samplerConfig == null ? 0 : samplerConfig.hashCode());
     return result;
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
index f3e17c6aa..b4f9dca63 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
@@ -32,6 +32,7 @@ import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.impl.SplitUtils;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase.TokenSource;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
@@ -41,6 +42,7 @@ import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.PartialKey;
 import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Base64;
 import org.apache.accumulo.core.util.DeprecationUtil;
@@ -64,6 +66,7 @@ public class RangeInputSplit extends InputSplit implements Writable {
   private Authorizations auths;
   private Set<Pair<Text,Text>> fetchedColumns;
   private List<IteratorSetting> iterators;
  private SamplerConfiguration samplerConfig;
   private Level level;
 
   public RangeInputSplit() {
@@ -215,6 +218,10 @@ public class RangeInputSplit extends InputSplit implements Writable {
     if (in.readBoolean()) {
       level = Level.toLevel(in.readInt());
     }

    if (in.readBoolean()) {
      samplerConfig = new SamplerConfigurationImpl(in).toSamplerConfiguration();
    }
   }
 
   @Override
@@ -301,6 +308,11 @@ public class RangeInputSplit extends InputSplit implements Writable {
     if (null != level) {
       out.writeInt(level.toInt());
     }

    out.writeBoolean(null != samplerConfig);
    if (null != samplerConfig) {
      new SamplerConfigurationImpl(samplerConfig).write(out);
    }
   }
 
   /**
@@ -510,6 +522,15 @@ public class RangeInputSplit extends InputSplit implements Writable {
     sb.append(" fetchColumns: ").append(fetchedColumns);
     sb.append(" iterators: ").append(iterators);
     sb.append(" logLevel: ").append(level);
    sb.append(" samplerConfig: ").append(samplerConfig);
     return sb.toString();
   }

  public void setSamplerConfiguration(SamplerConfiguration samplerConfiguration) {
    this.samplerConfig = samplerConfiguration;
  }

  public SamplerConfiguration getSamplerConfiguration() {
    return samplerConfig;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/SplitUtils.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/SplitUtils.java
index 68268fc52..b81b06454 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/SplitUtils.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/impl/SplitUtils.java
@@ -50,6 +50,8 @@ public class SplitUtils {
     split.setFetchedColumns(tableConfig.getFetchedColumns());
     split.setIterators(tableConfig.getIterators());
     split.setLogLevel(logLevel);

    split.setSamplerConfiguration(tableConfig.getSamplerConfiguration());
   }
 
   public static float getProgress(ByteSequence start, ByteSequence end, ByteSequence position) {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
index 882c6d309..65248c52b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
@@ -17,11 +17,15 @@
 package org.apache.accumulo.core.client.mapreduce.lib.impl;
 
 import java.util.Arrays;
import java.util.Map;
 import java.util.Map.Entry;
import java.util.Set;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.conf.Configuration;
 
 /**
@@ -97,8 +101,17 @@ public class FileOutputConfigurator extends ConfiguratorBase {
     String prefix = enumToConfKey(implementingClass, Opts.ACCUMULO_PROPERTIES) + ".";
     ConfigurationCopy acuConf = new ConfigurationCopy(AccumuloConfiguration.getDefaultConfiguration());
     for (Entry<String,String> entry : conf)
      if (entry.getKey().startsWith(prefix))
        acuConf.set(Property.getPropertyByKey(entry.getKey().substring(prefix.length())), entry.getValue());
      if (entry.getKey().startsWith(prefix)) {
        String propString = entry.getKey().substring(prefix.length());
        Property prop = Property.getPropertyByKey(propString);
        if (prop != null) {
          acuConf.set(prop, entry.getValue());
        } else if (Property.isValidTablePropertyKey(propString)) {
          acuConf.set(propString, entry.getValue());
        } else {
          throw new IllegalArgumentException("Unknown accumulo file property " + propString);
        }
      }
     return acuConf;
   }
 
@@ -184,4 +197,16 @@ public class FileOutputConfigurator extends ConfiguratorBase {
     setAccumuloProperty(implementingClass, conf, Property.TABLE_FILE_REPLICATION, replication);
   }
 
  /**
   * @since 1.8.0
   */
  public static void setSampler(Class<?> implementingClass, Configuration conf, SamplerConfiguration samplerConfig) {
    Map<String,String> props = new SamplerConfigurationImpl(samplerConfig).toTablePropertiesMap();

    Set<Entry<String,String>> es = props.entrySet();
    for (Entry<String,String> entry : es) {
      conf.set(enumToConfKey(implementingClass, Opts.ACCUMULO_PROPERTIES) + "." + entry.getKey(), entry.getValue());
    }
  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
index efda7d9de..6ba34af05 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
@@ -46,6 +46,7 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.impl.Credentials;
 import org.apache.accumulo.core.client.impl.DelegationTokenImpl;
@@ -62,6 +63,7 @@ import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.master.state.tables.TableState;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.TablePermission;
 import org.apache.accumulo.core.util.Base64;
@@ -74,6 +76,7 @@ import org.apache.hadoop.io.Text;
 import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.util.StringUtils;
 
import com.google.common.base.Preconditions;
 import com.google.common.collect.Maps;
 
 /**
@@ -87,7 +90,7 @@ public class InputConfigurator extends ConfiguratorBase {
    * @since 1.6.0
    */
   public static enum ScanOpts {
    TABLE_NAME, AUTHORIZATIONS, RANGES, COLUMNS, ITERATORS, TABLE_CONFIGS
    TABLE_NAME, AUTHORIZATIONS, RANGES, COLUMNS, ITERATORS, TABLE_CONFIGS, SAMPLER_CONFIG
   }
 
   /**
@@ -805,6 +808,11 @@ public class InputConfigurator extends ConfiguratorBase {
       if (ranges != null)
         queryConfig.setRanges(ranges);
 
      SamplerConfiguration samplerConfig = getSamplerConfiguration(implementingClass, conf);
      if (samplerConfig != null) {
        queryConfig.setSamplerConfiguration(samplerConfig);
      }

       queryConfig.setAutoAdjustRanges(getAutoAdjustRanges(implementingClass, conf)).setUseIsolatedScanners(isIsolated(implementingClass, conf))
           .setUseLocalIterators(usesLocalIterators(implementingClass, conf)).setOfflineScan(isOfflineScan(implementingClass, conf));
       return Maps.immutableEntry(tableName, queryConfig);
@@ -901,4 +909,47 @@ public class InputConfigurator extends ConfiguratorBase {
     }
     return binnedRanges;
   }

  private static String toBase64(Writable writable) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try {
      writable.write(dos);
      dos.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return Base64.encodeBase64String(baos.toByteArray());
  }

  private static <T extends Writable> T fromBase64(T writable, String enc) {
    ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(enc));
    DataInputStream dis = new DataInputStream(bais);
    try {
      writable.readFields(dis);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writable;
  }

  public static void setSamplerConfiguration(Class<?> implementingClass, Configuration conf, SamplerConfiguration samplerConfig) {
    Preconditions.checkNotNull(samplerConfig);

    String key = enumToConfKey(implementingClass, ScanOpts.SAMPLER_CONFIG);
    String val = toBase64(new SamplerConfigurationImpl(samplerConfig));

    conf.set(key, val);
  }

  public static SamplerConfiguration getSamplerConfiguration(Class<?> implementingClass, Configuration conf) {
    String key = enumToConfKey(implementingClass, ScanOpts.SAMPLER_CONFIG);

    String encodedSC = conf.get(key);
    if (encodedSC == null)
      return null;

    return fromBase64(new SamplerConfigurationImpl(), encodedSC).toSamplerConfiguration();
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
index f81e9dd33..45b65e90a 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
@@ -24,6 +24,7 @@ import java.util.Iterator;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ScannerOptions;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ArrayByteSequence;
@@ -112,6 +113,21 @@ public class MockScannerBase extends ScannerOptions implements ScannerBase {
       allIters.add(iter);
       return new MultiIterator(allIters, false);
     }

    @Override
    public boolean isSamplingEnabled() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IteratorEnvironment cloneWithSamplingEnabled() {
      throw new UnsupportedOperationException();
    }
   }
 
   public SortedKeyValueIterator<Key,Value> createFilter(SortedKeyValueIterator<Key,Value> inner) throws IOException {
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
index 0712f2260..7ca5766a7 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
@@ -40,6 +40,7 @@ import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.FindMax;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.client.impl.TableOperationsHelper;
 import org.apache.accumulo.core.client.impl.Tables;
@@ -480,4 +481,20 @@ class MockTableOperations extends TableOperationsHelper {
     }
     return true;
   }

  @Override
  public void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws TableNotFoundException, AccumuloException,
      AccumuloSecurityException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
    throw new UnsupportedOperationException();
  }

  @Override
  public SamplerConfiguration getSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
    throw new UnsupportedOperationException();
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/compaction/CompactionSettings.java b/core/src/main/java/org/apache/accumulo/core/compaction/CompactionSettings.java
index 43f8c0fac..1c5369e61 100644
-- a/core/src/main/java/org/apache/accumulo/core/compaction/CompactionSettings.java
++ b/core/src/main/java/org/apache/accumulo/core/compaction/CompactionSettings.java
@@ -21,6 +21,7 @@ import java.util.Map;
 
 public enum CompactionSettings {
 
  SF_NO_SAMPLE(new NullType()),
   SF_GT_ESIZE_OPT(new SizeType()),
   SF_LT_ESIZE_OPT(new SizeType()),
   SF_NAME_RE_OPT(new PatternType()),
diff --git a/core/src/main/java/org/apache/accumulo/core/compaction/NullType.java b/core/src/main/java/org/apache/accumulo/core/compaction/NullType.java
new file mode 100644
index 000000000..fb4c452d9
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/compaction/NullType.java
@@ -0,0 +1,29 @@
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

package org.apache.accumulo.core.compaction;

import com.google.common.base.Preconditions;

public class NullType implements Type {
  @Override
  public String convert(String str) {
    Preconditions.checkArgument(str == null);
    return "";
  }

}
diff --git a/core/src/main/java/org/apache/accumulo/core/conf/Property.java b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
index 5bd5c8acd..400577c78 100644
-- a/core/src/main/java/org/apache/accumulo/core/conf/Property.java
++ b/core/src/main/java/org/apache/accumulo/core/conf/Property.java
@@ -514,6 +514,16 @@ public enum Property {
   @Experimental
   TABLE_VOLUME_CHOOSER("table.volume.chooser", "org.apache.accumulo.server.fs.RandomVolumeChooser", PropertyType.CLASSNAME,
       "The class that will be used to select which volume will be used to create new files for this table."),
  TABLE_SAMPLER(
      "table.sampler",
      "",
      PropertyType.CLASSNAME,
      "The name of a class that implements org.apache.accumulo.core.Sampler.  Setting this option enables storing a sample of data which can be scanned."
          + "  Always having a current sample can useful for query optimization and data comprehension.   After enabling sampling for an existing table, a compaction "
          + "is needed to compute the sample for existing data.  The compact command in the shell has an option to only compact files without sample data."),
  TABLE_SAMPLER_OPTS("table.sampler.opt.", null, PropertyType.PREFIX,
      "The property is used to set options for a sampler.  If a sample had two options like hasher and modulous, then the two properties "
          + "table.sampler.opt.hasher=${hash algorithm} and table.sampler.opt.modulous=${mod} would be set."),
 
   // VFS ClassLoader properties
   VFS_CLASSLOADER_SYSTEM_CLASSPATH_PROPERTY(AccumuloVFSClassLoader.VFS_CLASSLOADER_SYSTEM_CLASSPATH_PROPERTY, "", PropertyType.STRING,
@@ -776,7 +786,7 @@ public enum Property {
     return validTableProperties.contains(key) || key.startsWith(Property.TABLE_CONSTRAINT_PREFIX.getKey())
         || key.startsWith(Property.TABLE_ITERATOR_PREFIX.getKey()) || key.startsWith(Property.TABLE_LOCALITY_GROUP_PREFIX.getKey())
         || key.startsWith(Property.TABLE_COMPACTION_STRATEGY_PREFIX.getKey()) || key.startsWith(Property.TABLE_REPLICATION_TARGET.getKey())
        || key.startsWith(Property.TABLE_ARBITRARY_PROP_PREFIX.getKey());
        || key.startsWith(Property.TABLE_ARBITRARY_PROP_PREFIX.getKey()) || key.startsWith(TABLE_SAMPLER_OPTS.getKey());
   }
 
   private static final EnumSet<Property> fixedProperties = EnumSet.of(Property.TSERV_CLIENTPORT, Property.TSERV_NATIVEMAP_ENABLED,
diff --git a/core/src/main/java/org/apache/accumulo/core/file/BloomFilterLayer.java b/core/src/main/java/org/apache/accumulo/core/file/BloomFilterLayer.java
index a5bea83c1..758df129c 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/BloomFilterLayer.java
++ b/core/src/main/java/org/apache/accumulo/core/file/BloomFilterLayer.java
@@ -50,6 +50,7 @@ import org.apache.accumulo.core.file.keyfunctor.KeyFunctor;
 import org.apache.accumulo.core.file.rfile.RFile;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.util.NamingThreadFactory;
 import org.apache.accumulo.fate.util.LoggingRunnable;
@@ -424,6 +425,11 @@ public class BloomFilterLayer {
       reader.setInterruptFlag(flag);
     }
 
    @Override
    public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
      return new BloomFilterLayer.Reader(reader.getSample(sampleConfig), bfl);
    }

   }
 
   public static void main(String[] args) throws IOException {
diff --git a/core/src/main/java/org/apache/accumulo/core/file/FileSKVIterator.java b/core/src/main/java/org/apache/accumulo/core/file/FileSKVIterator.java
index 60970e293..3713453cf 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/FileSKVIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/file/FileSKVIterator.java
@@ -21,14 +21,17 @@ import java.io.IOException;
 
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 
public interface FileSKVIterator extends InterruptibleIterator {
public interface FileSKVIterator extends InterruptibleIterator, AutoCloseable {
   Key getFirstKey() throws IOException;
 
   Key getLastKey() throws IOException;
 
   DataInputStream getMetaStore(String name) throws IOException, NoSuchMetaStoreException;
 
  FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig);

   void closeDeepCopies() throws IOException;
 
   void close() throws IOException;
diff --git a/core/src/main/java/org/apache/accumulo/core/file/map/MapFileOperations.java b/core/src/main/java/org/apache/accumulo/core/file/map/MapFileOperations.java
index fb2762fe3..75cfa7e00 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/map/MapFileOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/file/map/MapFileOperations.java
@@ -37,6 +37,7 @@ import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.system.MapFileIterator;
 import org.apache.accumulo.core.iterators.system.SequenceFileIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
@@ -132,6 +133,11 @@ public class MapFileOperations extends FileOperations {
     public void setInterruptFlag(AtomicBoolean flag) {
       ((FileSKVIterator) reader).setInterruptFlag(flag);
     }

    @Override
    public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
      return ((FileSKVIterator) reader).getSample(sampleConfig);
    }
   }
 
   @Override
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiIndexIterator.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiIndexIterator.java
index f220a581b..01af1849a 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiIndexIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiIndexIterator.java
@@ -33,6 +33,7 @@ import org.apache.accumulo.core.file.rfile.MultiLevelIndex.IndexEntry;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.system.HeapIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 
 class MultiIndexIterator extends HeapIterator implements FileSKVIterator {
 
@@ -93,4 +94,9 @@ class MultiIndexIterator extends HeapIterator implements FileSKVIterator {
     throw new UnsupportedOperationException();
   }
 
  @Override
  public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
    throw new UnsupportedOperationException();
  }

 }
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiLevelIndex.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiLevelIndex.java
index 210947802..1a383e416 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiLevelIndex.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/MultiLevelIndex.java
@@ -265,7 +265,7 @@ public class MultiLevelIndex {
 
     public void readFields(DataInput in, int version) throws IOException {
 
      if (version == RFile.RINDEX_VER_6 || version == RFile.RINDEX_VER_7) {
      if (version == RFile.RINDEX_VER_6 || version == RFile.RINDEX_VER_7 || version == RFile.RINDEX_VER_8) {
         level = in.readInt();
         offset = in.readInt();
         hasNext = in.readBoolean();
@@ -736,7 +736,7 @@ public class MultiLevelIndex {
 
       size = 0;
 
      if (version == RFile.RINDEX_VER_6 || version == RFile.RINDEX_VER_7) {
      if (version == RFile.RINDEX_VER_6 || version == RFile.RINDEX_VER_7 || version == RFile.RINDEX_VER_8) {
         size = in.readInt();
       }
 
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
index 5a3e9114a..4631a4dce 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/PrintInfo.java
@@ -28,6 +28,7 @@ import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileSKVIterator;
 import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
 import org.apache.accumulo.core.file.rfile.RFile.Reader;
 import org.apache.accumulo.start.spi.KeywordExecutable;
@@ -54,6 +55,8 @@ public class PrintInfo implements KeywordExecutable {
     boolean hash = false;
     @Parameter(names = {"--histogram"}, description = "print a histogram of the key-value sizes")
     boolean histogram = false;
    @Parameter(names = {"--useSample"}, description = "Use sample data for --dump, --vis, --histogram options")
    boolean useSample = false;
     @Parameter(description = " <file> { <file> ... }")
     List<String> files = new ArrayList<String>();
     @Parameter(names = {"-c", "--config"}, variableArity = true, description = "Comma-separated Hadoop configuration files")
@@ -119,14 +122,27 @@ public class PrintInfo implements KeywordExecutable {
       if (opts.histogram || opts.dump || opts.vis || opts.hash) {
         localityGroupCF = iter.getLocalityGroupCF();
 
        FileSKVIterator dataIter = iter;
        if (opts.useSample) {
          dataIter = iter.getSample();

          if (dataIter == null) {
            System.out.println("ERROR : This rfile has no sample data");
            return;
          }
        }

         for (Entry<String,ArrayList<ByteSequence>> cf : localityGroupCF.entrySet()) {
 
          iter.seek(new Range((Key) null, (Key) null), cf.getValue(), true);
          while (iter.hasTop()) {
            Key key = iter.getTopKey();
            Value value = iter.getTopValue();
            if (opts.dump)
          dataIter.seek(new Range((Key) null, (Key) null), cf.getValue(), true);
          while (dataIter.hasTop()) {
            Key key = dataIter.getTopKey();
            Value value = dataIter.getTopValue();
            if (opts.dump) {
               System.out.println(key + " -> " + value);
              if (System.out.checkError())
                return;
            }
             if (opts.histogram) {
               long size = key.getSize() + value.getSize();
               int bucket = (int) Math.log10(size);
@@ -134,7 +150,7 @@ public class PrintInfo implements KeywordExecutable {
               sizeBuckets[bucket] += size;
               totalSize += size;
             }
            iter.next();
            dataIter.next();
           }
         }
       }
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
index 54b01b425..9564f0bb6 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
@@ -36,6 +36,8 @@ import java.util.Set;
 import java.util.TreeMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ArrayByteSequence;
@@ -62,12 +64,17 @@ import org.apache.accumulo.core.iterators.system.HeapIterator;
 import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator.LocalityGroup;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.util.MutableByteSequence;
 import org.apache.commons.lang.mutable.MutableLong;
 import org.apache.hadoop.io.Writable;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

 public class RFile {
 
   public static final String EXTENSION = "rf";
@@ -77,15 +84,38 @@ public class RFile {
   private RFile() {}
 
   private static final int RINDEX_MAGIC = 0x20637474;
  static final int RINDEX_VER_7 = 7;
  static final int RINDEX_VER_6 = 6;

  static final int RINDEX_VER_8 = 8; // Added sample storage. There is a sample locality group for each locality group. Sample are built using a Sampler and
                                     // sampler configuration. The Sampler and its configuration are stored in RFile. Persisting the method of producing the
                                     // sample allows a user of RFile to determine if the sample is useful.
  static final int RINDEX_VER_7 = 7; // Added support for prefix encoding and encryption. Before this change only exact matches within a key field were deduped
                                     // for consecutive keys. After this change, if consecutive key fields have the same prefix then the prefix is only stored
                                     // once.
  static final int RINDEX_VER_6 = 6; // Added support for multilevel indexes. Before this the index was one list with an entry for each data block. For large
                                     // files, a large index needed to be read into memory before any seek could be done. After this change the index is a fat
                                     // tree, and opening a large rfile is much faster. Like the previous version of Rfile, each index node in the tree is kept
                                     // in memory serialized and used in its serialized form.
   // static final int RINDEX_VER_5 = 5; // unreleased
  static final int RINDEX_VER_4 = 4;
  static final int RINDEX_VER_3 = 3;
  static final int RINDEX_VER_4 = 4; // Added support for seeking using serialized indexes. After this change index is no longer deserialized when rfile opened.
                                     // Entire serialized index is read into memory as single byte array. For seeks, serialized index is used to find blocks
                                     // (the binary search deserializes the specific entries its needs). This resulted in less memory usage (no object overhead)
                                     // and faster open times for RFiles.
  static final int RINDEX_VER_3 = 3; // Initial released version of RFile. R is for relative encoding. A keys is encoded relative to the previous key. The
                                     // initial version deduped key fields that were the same for consecutive keys. For sorted data this is a common occurrence.
                                     // This version supports locality groups. Each locality group has an index pointing to set of data blocks. Each data block
                                     // contains relatively encoded keys and values.

  // Buffer sample data so that many sample data blocks are stored contiguously.
  private static int sampleBufferSize = 10000000;

  @VisibleForTesting
  public static void setSampleBufferSize(int bufferSize) {
    sampleBufferSize = bufferSize;
  }
 
   private static class LocalityGroupMetadata implements Writable {
 
    private int startBlock;
    private int startBlock = -1;
     private Key firstKey;
     private Map<ByteSequence,MutableLong> columnFamilies;
 
@@ -95,14 +125,15 @@ public class RFile {
 
     private MultiLevelIndex.BufferedWriter indexWriter;
     private MultiLevelIndex.Reader indexReader;
    private int version;
 
     public LocalityGroupMetadata(int version, BlockFileReader br) {
       columnFamilies = new HashMap<ByteSequence,MutableLong>();
       indexReader = new MultiLevelIndex.Reader(br, version);
      this.version = version;
     }
 
    public LocalityGroupMetadata(int nextBlock, Set<ByteSequence> pcf, int indexBlockSize, BlockFileWriter bfw) {
      this.startBlock = nextBlock;
    public LocalityGroupMetadata(Set<ByteSequence> pcf, int indexBlockSize, BlockFileWriter bfw) {
       isDefaultLG = true;
       columnFamilies = new HashMap<ByteSequence,MutableLong>();
       previousColumnFamilies = pcf;
@@ -110,8 +141,7 @@ public class RFile {
       indexWriter = new MultiLevelIndex.BufferedWriter(new MultiLevelIndex.Writer(bfw, indexBlockSize));
     }
 
    public LocalityGroupMetadata(String name, Set<ByteSequence> cfset, int nextBlock, int indexBlockSize, BlockFileWriter bfw) {
      this.startBlock = nextBlock;
    public LocalityGroupMetadata(String name, Set<ByteSequence> cfset, int indexBlockSize, BlockFileWriter bfw) {
       this.name = name;
       isDefaultLG = false;
       columnFamilies = new HashMap<ByteSequence,MutableLong>();
@@ -181,7 +211,9 @@ public class RFile {
         name = in.readUTF();
       }
 
      startBlock = in.readInt();
      if (version == RINDEX_VER_3 || version == RINDEX_VER_4 || version == RINDEX_VER_6 || version == RINDEX_VER_7) {
        startBlock = in.readInt();
      }
 
       int size = in.readInt();
 
@@ -224,8 +256,6 @@ public class RFile {
         out.writeUTF(name);
       }
 
      out.writeInt(startBlock);

       if (isDefaultLG && columnFamilies == null) {
         // only expect null when default LG, otherwise let a NPE occur
         out.writeInt(-1);
@@ -246,26 +276,27 @@ public class RFile {
       indexWriter.close(out);
     }
 
    public void printInfo() throws IOException {
    public void printInfo(boolean isSample) throws IOException {
       PrintStream out = System.out;
      out.println("Locality group         : " + (isDefaultLG ? "<DEFAULT>" : name));
      out.println("\tStart block          : " + startBlock);
      out.println("\tNum   blocks         : " + String.format("%,d", indexReader.size()));
      out.printf("%-24s : %s\n", (isSample ? "Sample " : "") + "Locality group ", (isDefaultLG ? "<DEFAULT>" : name));
      if (version == RINDEX_VER_3 || version == RINDEX_VER_4 || version == RINDEX_VER_6 || version == RINDEX_VER_7) {
        out.printf("\t%-22s : %d\n", "Start block", startBlock);
      }
      out.printf("\t%-22s : %,d\n", "Num   blocks", indexReader.size());
       TreeMap<Integer,Long> sizesByLevel = new TreeMap<Integer,Long>();
       TreeMap<Integer,Long> countsByLevel = new TreeMap<Integer,Long>();
       indexReader.getIndexInfo(sizesByLevel, countsByLevel);
       for (Entry<Integer,Long> entry : sizesByLevel.descendingMap().entrySet()) {
        out.println("\tIndex level " + entry.getKey() + "        : "
            + String.format("%,d bytes  %,d blocks", entry.getValue(), countsByLevel.get(entry.getKey())));
        out.printf("\t%-22s : %,d bytes  %,d blocks\n", "Index level " + entry.getKey(), entry.getValue(), countsByLevel.get(entry.getKey()));
       }
      out.println("\tFirst key            : " + firstKey);
      out.printf("\t%-22s : %s\n", "First key", firstKey);
 
       Key lastKey = null;
       if (indexReader.size() > 0) {
         lastKey = indexReader.getLastKey();
       }
 
      out.println("\tLast key             : " + lastKey);
      out.printf("\t%-22s : %s\n", "Last key", lastKey);
 
       long numKeys = 0;
       IndexIterator countIter = indexReader.lookup(new Key());
@@ -273,48 +304,193 @@ public class RFile {
         numKeys += countIter.next().getNumEntries();
       }
 
      out.println("\tNum entries          : " + String.format("%,d", numKeys));
      out.println("\tColumn families      : " + (isDefaultLG && columnFamilies == null ? "<UNKNOWN>" : columnFamilies.keySet()));
      out.printf("\t%-22s : %,d\n", "Num entries", numKeys);
      out.printf("\t%-22s : %s\n", "Column families", (isDefaultLG && columnFamilies == null ? "<UNKNOWN>" : columnFamilies.keySet()));
     }
 
   }
 
  public static class Writer implements FileSKVWriter {
  private static class SampleEntry {
    Key key;
    Value val;
 
    public static final int MAX_CF_IN_DLG = 1000;
    SampleEntry(Key key, Value val) {
      this.key = new Key(key);
      this.val = new Value(val);
    }
  }

  private static class SampleLocalityGroupWriter {

    private Sampler sampler;

    private List<SampleEntry> entries = new ArrayList<>();
    private long dataSize = 0;

    private LocalityGroupWriter lgr;

    public SampleLocalityGroupWriter(LocalityGroupWriter lgr, Sampler sampler) {
      this.lgr = lgr;
      this.sampler = sampler;
    }

    public void append(Key key, Value value) throws IOException {
      if (sampler.accept(key)) {
        entries.add(new SampleEntry(key, value));
        dataSize += key.getSize() + value.getSize();
      }
    }

    public void close() throws IOException {
      for (SampleEntry se : entries) {
        lgr.append(se.key, se.val);
      }

      lgr.close();
    }

    public void flushIfNeeded() throws IOException {
      if (dataSize > sampleBufferSize) {
        // the reason to write out all but one key is so that closeBlock() can always eventually be called with true
        List<SampleEntry> subList = entries.subList(0, entries.size() - 1);

        if (subList.size() > 0) {
          for (SampleEntry se : subList) {
            lgr.append(se.key, se.val);
          }

          lgr.closeBlock(subList.get(subList.size() - 1).key, false);

          subList.clear();
          dataSize = 0;
        }
      }
    }
  }

  private static class LocalityGroupWriter {
 
     private BlockFileWriter fileWriter;
     private ABlockWriter blockWriter;
 
     // private BlockAppender blockAppender;
     private long blockSize = 100000;
    private int indexBlockSize;
     private int entries = 0;
 
    private ArrayList<LocalityGroupMetadata> localityGroups = new ArrayList<LocalityGroupMetadata>();
     private LocalityGroupMetadata currentLocalityGroup = null;
    private int nextBlock = 0;
 
     private Key lastKeyInBlock = null;
 
    private Key prevKey = new Key();

    private SampleLocalityGroupWriter sample;

    LocalityGroupWriter(BlockFileWriter fileWriter, long blockSize, LocalityGroupMetadata currentLocalityGroup, SampleLocalityGroupWriter sample) {
      this.fileWriter = fileWriter;
      this.blockSize = blockSize;
      this.currentLocalityGroup = currentLocalityGroup;
      this.sample = sample;
    }

    public void append(Key key, Value value) throws IOException {

      if (key.compareTo(prevKey) < 0) {
        throw new IllegalStateException("Keys appended out-of-order.  New key " + key + ", previous key " + prevKey);
      }

      currentLocalityGroup.updateColumnCount(key);

      if (currentLocalityGroup.getFirstKey() == null) {
        currentLocalityGroup.setFirstKey(key);
      }

      if (sample != null) {
        sample.append(key, value);
      }

      if (blockWriter == null) {
        blockWriter = fileWriter.prepareDataBlock();
      } else if (blockWriter.getRawSize() > blockSize) {
        closeBlock(prevKey, false);
        blockWriter = fileWriter.prepareDataBlock();
      }

      RelativeKey rk = new RelativeKey(lastKeyInBlock, key);

      rk.write(blockWriter);
      value.write(blockWriter);
      entries++;

      prevKey = new Key(key);
      lastKeyInBlock = prevKey;

    }

    private void closeBlock(Key key, boolean lastBlock) throws IOException {
      blockWriter.close();

      if (lastBlock)
        currentLocalityGroup.indexWriter.addLast(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());
      else
        currentLocalityGroup.indexWriter.add(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());

      if (sample != null)
        sample.flushIfNeeded();

      blockWriter = null;
      lastKeyInBlock = null;
      entries = 0;
    }

    public void close() throws IOException {
      if (blockWriter != null) {
        closeBlock(lastKeyInBlock, true);
      }

      if (sample != null) {
        sample.close();
      }
    }
  }

  public static class Writer implements FileSKVWriter {

    public static final int MAX_CF_IN_DLG = 1000;

    private BlockFileWriter fileWriter;

    // private BlockAppender blockAppender;
    private long blockSize = 100000;
    private int indexBlockSize;

    private ArrayList<LocalityGroupMetadata> localityGroups = new ArrayList<LocalityGroupMetadata>();
    private ArrayList<LocalityGroupMetadata> sampleGroups = new ArrayList<LocalityGroupMetadata>();
    private LocalityGroupMetadata currentLocalityGroup = null;
    private LocalityGroupMetadata sampleLocalityGroup = null;

     private boolean dataClosed = false;
     private boolean closed = false;
    private Key prevKey = new Key();
     private boolean startedDefaultLocalityGroup = false;
 
     private HashSet<ByteSequence> previousColumnFamilies;
     private long length = -1;
 
    private LocalityGroupWriter lgWriter;

    private SamplerConfigurationImpl samplerConfig;
    private Sampler sampler;

     public Writer(BlockFileWriter bfw, int blockSize) throws IOException {
      this(bfw, blockSize, (int) AccumuloConfiguration.getDefaultConfiguration().getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX));
      this(bfw, blockSize, (int) AccumuloConfiguration.getDefaultConfiguration().getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX), null, null);
     }
 
    public Writer(BlockFileWriter bfw, int blockSize, int indexBlockSize) throws IOException {
    public Writer(BlockFileWriter bfw, int blockSize, int indexBlockSize, SamplerConfigurationImpl samplerConfig, Sampler sampler) throws IOException {
       this.blockSize = blockSize;
       this.indexBlockSize = indexBlockSize;
       this.fileWriter = bfw;
      this.blockWriter = null;
       previousColumnFamilies = new HashSet<ByteSequence>();
      this.samplerConfig = samplerConfig;
      this.sampler = sampler;
     }
 
     @Override
@@ -329,10 +505,12 @@ public class RFile {
       ABlockWriter mba = fileWriter.prepareMetaBlock("RFile.index");
 
       mba.writeInt(RINDEX_MAGIC);
      mba.writeInt(RINDEX_VER_7);
      mba.writeInt(RINDEX_VER_8);
 
      if (currentLocalityGroup != null)
      if (currentLocalityGroup != null) {
         localityGroups.add(currentLocalityGroup);
        sampleGroups.add(sampleLocalityGroup);
      }
 
       mba.writeInt(localityGroups.size());
 
@@ -340,6 +518,18 @@ public class RFile {
         lc.write(mba);
       }
 
      if (samplerConfig == null) {
        mba.writeBoolean(false);
      } else {
        mba.writeBoolean(true);

        for (LocalityGroupMetadata lc : sampleGroups) {
          lc.write(mba);
        }

        samplerConfig.write(mba);
      }

       mba.close();
       fileWriter.close();
       length = fileWriter.getLength();
@@ -355,8 +545,8 @@ public class RFile {
 
       dataClosed = true;
 
      if (blockWriter != null) {
        closeBlock(lastKeyInBlock, true);
      if (lgWriter != null) {
        lgWriter.close();
       }
     }
 
@@ -367,46 +557,7 @@ public class RFile {
         throw new IllegalStateException("Cannont append, data closed");
       }
 
      if (key.compareTo(prevKey) < 0) {
        throw new IllegalStateException("Keys appended out-of-order.  New key " + key + ", previous key " + prevKey);
      }

      currentLocalityGroup.updateColumnCount(key);

      if (currentLocalityGroup.getFirstKey() == null) {
        currentLocalityGroup.setFirstKey(key);
      }

      if (blockWriter == null) {
        blockWriter = fileWriter.prepareDataBlock();
      } else if (blockWriter.getRawSize() > blockSize) {
        closeBlock(prevKey, false);
        blockWriter = fileWriter.prepareDataBlock();
      }

      RelativeKey rk = new RelativeKey(lastKeyInBlock, key);

      rk.write(blockWriter);
      value.write(blockWriter);
      entries++;

      prevKey = new Key(key);
      lastKeyInBlock = prevKey;

    }

    private void closeBlock(Key key, boolean lastBlock) throws IOException {
      blockWriter.close();

      if (lastBlock)
        currentLocalityGroup.indexWriter.addLast(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());
      else
        currentLocalityGroup.indexWriter.add(key, entries, blockWriter.getStartPos(), blockWriter.getCompressedSize(), blockWriter.getRawSize());

      blockWriter = null;
      lastKeyInBlock = null;
      entries = 0;
      nextBlock++;
      lgWriter.append(key, value);
     }
 
     @Override
@@ -425,28 +576,35 @@ public class RFile {
         throw new IllegalStateException("Can not start anymore new locality groups after default locality group started");
       }
 
      if (blockWriter != null) {
        closeBlock(lastKeyInBlock, true);
      if (lgWriter != null) {
        lgWriter.close();
       }
 
       if (currentLocalityGroup != null) {
         localityGroups.add(currentLocalityGroup);
        sampleGroups.add(sampleLocalityGroup);
       }
 
       if (columnFamilies == null) {
         startedDefaultLocalityGroup = true;
        currentLocalityGroup = new LocalityGroupMetadata(nextBlock, previousColumnFamilies, indexBlockSize, fileWriter);
        currentLocalityGroup = new LocalityGroupMetadata(previousColumnFamilies, indexBlockSize, fileWriter);
        sampleLocalityGroup = new LocalityGroupMetadata(previousColumnFamilies, indexBlockSize, fileWriter);
       } else {
         if (!Collections.disjoint(columnFamilies, previousColumnFamilies)) {
           HashSet<ByteSequence> overlap = new HashSet<ByteSequence>(columnFamilies);
           overlap.retainAll(previousColumnFamilies);
           throw new IllegalArgumentException("Column families over lap with previous locality group : " + overlap);
         }
        currentLocalityGroup = new LocalityGroupMetadata(name, columnFamilies, nextBlock, indexBlockSize, fileWriter);
        currentLocalityGroup = new LocalityGroupMetadata(name, columnFamilies, indexBlockSize, fileWriter);
        sampleLocalityGroup = new LocalityGroupMetadata(name, columnFamilies, indexBlockSize, fileWriter);
         previousColumnFamilies.addAll(columnFamilies);
       }
 
      prevKey = new Key();
      SampleLocalityGroupWriter sampleWriter = null;
      if (sampler != null) {
        sampleWriter = new SampleLocalityGroupWriter(new LocalityGroupWriter(fileWriter, blockSize, sampleLocalityGroup, null), sampler);
      }
      lgWriter = new LocalityGroupWriter(fileWriter, blockSize, currentLocalityGroup, sampleWriter);
     }
 
     @Override
@@ -616,8 +774,9 @@ public class RFile {
       if (columnFamilies.size() != 0 || inclusive)
         throw new IllegalArgumentException("I do not know how to filter column families");
 
      if (interruptFlag != null && interruptFlag.get())
      if (interruptFlag != null && interruptFlag.get()) {
         throw new IterationInterruptedException();
      }
 
       try {
         _seek(range);
@@ -830,6 +989,11 @@ public class RFile {
     public void registerMetrics(MetricsGatherer<?> vmg) {
       metricsGatherer = vmg;
     }

    @Override
    public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
      throw new UnsupportedOperationException();
    }
   }
 
   public static class Reader extends HeapIterator implements FileSKVIterator {
@@ -837,8 +1001,12 @@ public class RFile {
     private BlockFileReader reader;
 
     private ArrayList<LocalityGroupMetadata> localityGroups = new ArrayList<LocalityGroupMetadata>();
    private ArrayList<LocalityGroupMetadata> sampleGroups = new ArrayList<LocalityGroupMetadata>();

    private LocalityGroupReader currentReaders[];
    private LocalityGroupReader readers[];
    private LocalityGroupReader sampleReaders[];
 
    private LocalityGroupReader lgReaders[];
     private HashSet<ByteSequence> nonDefaultColumnFamilies;
 
     private List<Reader> deepCopies;
@@ -846,6 +1014,10 @@ public class RFile {
 
     private AtomicBoolean interruptFlag;
 
    private SamplerConfigurationImpl samplerConfig = null;

    private int rfileVersion;

     public Reader(BlockFileReader rdr) throws IOException {
       this.reader = rdr;
 
@@ -853,14 +1025,15 @@ public class RFile {
       try {
         int magic = mb.readInt();
         int ver = mb.readInt();
        rfileVersion = ver;
 
         if (magic != RINDEX_MAGIC)
           throw new IOException("Did not see expected magic number, saw " + magic);
        if (ver != RINDEX_VER_7 && ver != RINDEX_VER_6 && ver != RINDEX_VER_4 && ver != RINDEX_VER_3)
        if (ver != RINDEX_VER_8 && ver != RINDEX_VER_7 && ver != RINDEX_VER_6 && ver != RINDEX_VER_4 && ver != RINDEX_VER_3)
           throw new IOException("Did not see expected version, saw " + ver);
 
         int size = mb.readInt();
        lgReaders = new LocalityGroupReader[size];
        currentReaders = new LocalityGroupReader[size];
 
         deepCopies = new LinkedList<Reader>();
 
@@ -869,8 +1042,28 @@ public class RFile {
           lgm.readFields(mb);
           localityGroups.add(lgm);
 
          lgReaders[i] = new LocalityGroupReader(reader, lgm, ver);
          currentReaders[i] = new LocalityGroupReader(reader, lgm, ver);
        }

        readers = currentReaders;

        if (ver == RINDEX_VER_8 && mb.readBoolean()) {
          sampleReaders = new LocalityGroupReader[size];

          for (int i = 0; i < size; i++) {
            LocalityGroupMetadata lgm = new LocalityGroupMetadata(ver, rdr);
            lgm.readFields(mb);
            sampleGroups.add(lgm);

            sampleReaders[i] = new LocalityGroupReader(reader, lgm, ver);
          }

          samplerConfig = new SamplerConfigurationImpl(mb);
        } else {
          sampleReaders = null;
          samplerConfig = null;
         }

       } finally {
         mb.close();
       }
@@ -881,24 +1074,53 @@ public class RFile {
           nonDefaultColumnFamilies.addAll(lgm.columnFamilies.keySet());
       }
 
      createHeap(lgReaders.length);
      createHeap(currentReaders.length);
    }

    private Reader(Reader r, LocalityGroupReader sampleReaders[]) {
      super(sampleReaders.length);
      this.reader = r.reader;
      this.nonDefaultColumnFamilies = r.nonDefaultColumnFamilies;
      this.currentReaders = new LocalityGroupReader[sampleReaders.length];
      this.deepCopies = r.deepCopies;
      this.deepCopy = false;
      this.readers = r.readers;
      this.sampleReaders = r.sampleReaders;
      this.samplerConfig = r.samplerConfig;
      this.rfileVersion = r.rfileVersion;
      for (int i = 0; i < sampleReaders.length; i++) {
        this.currentReaders[i] = sampleReaders[i];
        this.currentReaders[i].setInterruptFlag(r.interruptFlag);
      }
     }
 
    private Reader(Reader r) {
      super(r.lgReaders.length);
    private Reader(Reader r, boolean useSample) {
      super(r.currentReaders.length);
       this.reader = r.reader;
       this.nonDefaultColumnFamilies = r.nonDefaultColumnFamilies;
      this.lgReaders = new LocalityGroupReader[r.lgReaders.length];
      this.currentReaders = new LocalityGroupReader[r.currentReaders.length];
       this.deepCopies = r.deepCopies;
       this.deepCopy = true;
      for (int i = 0; i < lgReaders.length; i++) {
        this.lgReaders[i] = new LocalityGroupReader(r.lgReaders[i]);
        this.lgReaders[i].setInterruptFlag(r.interruptFlag);
      this.samplerConfig = r.samplerConfig;
      this.rfileVersion = r.rfileVersion;
      this.readers = r.readers;
      this.sampleReaders = r.sampleReaders;

      for (int i = 0; i < r.readers.length; i++) {
        if (useSample) {
          this.currentReaders[i] = new LocalityGroupReader(r.sampleReaders[i]);
          this.currentReaders[i].setInterruptFlag(r.interruptFlag);
        } else {
          this.currentReaders[i] = new LocalityGroupReader(r.readers[i]);
          this.currentReaders[i].setInterruptFlag(r.interruptFlag);
        }

       }

     }
 
     private void closeLocalityGroupReaders() {
      for (LocalityGroupReader lgr : lgReaders) {
      for (LocalityGroupReader lgr : currentReaders) {
         try {
           lgr.close();
         } catch (IOException e) {
@@ -926,6 +1148,16 @@ public class RFile {
       closeDeepCopies();
       closeLocalityGroupReaders();
 
      if (sampleReaders != null) {
        for (LocalityGroupReader lgr : sampleReaders) {
          try {
            lgr.close();
          } catch (IOException e) {
            log.warn("Errored out attempting to close LocalityGroupReader.", e);
          }
        }
      }

       try {
         reader.close();
       } finally {
@@ -937,17 +1169,17 @@ public class RFile {
 
     @Override
     public Key getFirstKey() throws IOException {
      if (lgReaders.length == 0) {
      if (currentReaders.length == 0) {
         return null;
       }
 
       Key minKey = null;
 
      for (int i = 0; i < lgReaders.length; i++) {
      for (int i = 0; i < currentReaders.length; i++) {
         if (minKey == null) {
          minKey = lgReaders[i].getFirstKey();
          minKey = currentReaders[i].getFirstKey();
         } else {
          Key firstKey = lgReaders[i].getFirstKey();
          Key firstKey = currentReaders[i].getFirstKey();
           if (firstKey != null && firstKey.compareTo(minKey) < 0)
             minKey = firstKey;
         }
@@ -958,17 +1190,17 @@ public class RFile {
 
     @Override
     public Key getLastKey() throws IOException {
      if (lgReaders.length == 0) {
      if (currentReaders.length == 0) {
         return null;
       }
 
       Key maxKey = null;
 
      for (int i = 0; i < lgReaders.length; i++) {
      for (int i = 0; i < currentReaders.length; i++) {
         if (maxKey == null) {
          maxKey = lgReaders[i].getLastKey();
          maxKey = currentReaders[i].getLastKey();
         } else {
          Key lastKey = lgReaders[i].getLastKey();
          Key lastKey = currentReaders[i].getLastKey();
           if (lastKey != null && lastKey.compareTo(maxKey) > 0)
             maxKey = lastKey;
         }
@@ -988,10 +1220,26 @@ public class RFile {
 
     @Override
     public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
      Reader copy = new Reader(this);
      copy.setInterruptFlagInternal(interruptFlag);
      deepCopies.add(copy);
      return copy;
      if (env != null && env.isSamplingEnabled()) {
        SamplerConfiguration sc = env.getSamplerConfiguration();
        if (sc == null) {
          throw new SampleNotPresentException();
        }

        if (this.samplerConfig != null && this.samplerConfig.equals(new SamplerConfigurationImpl(sc))) {
          Reader copy = new Reader(this, true);
          copy.setInterruptFlagInternal(interruptFlag);
          deepCopies.add(copy);
          return copy;
        } else {
          throw new SampleNotPresentException();
        }
      } else {
        Reader copy = new Reader(this, false);
        copy.setInterruptFlagInternal(interruptFlag);
        deepCopies.add(copy);
        return copy;
      }
     }
 
     @Override
@@ -1027,14 +1275,20 @@ public class RFile {
      */
     public void registerMetrics(MetricsGatherer<?> vmg) {
       vmg.init(getLocalityGroupCF());
      for (LocalityGroupReader lgr : lgReaders) {
      for (LocalityGroupReader lgr : currentReaders) {
         lgr.registerMetrics(vmg);
       }

      if (sampleReaders != null) {
        for (LocalityGroupReader lgr : sampleReaders) {
          lgr.registerMetrics(vmg);
        }
      }
     }
 
     @Override
     public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {
      numLGSeeked = LocalityGroupIterator.seek(this, lgReaders, nonDefaultColumnFamilies, range, columnFamilies, inclusive);
      numLGSeeked = LocalityGroupIterator.seek(this, currentReaders, nonDefaultColumnFamilies, range, columnFamilies, inclusive);
     }
 
     int getNumLocalityGroupsSeeked() {
@@ -1045,16 +1299,53 @@ public class RFile {
 
       ArrayList<Iterator<IndexEntry>> indexes = new ArrayList<Iterator<IndexEntry>>();
 
      for (LocalityGroupReader lgr : lgReaders) {
      for (LocalityGroupReader lgr : currentReaders) {
         indexes.add(lgr.getIndex());
       }
 
       return new MultiIndexIterator(this, indexes);
     }
 
    @Override
    public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
      Preconditions.checkNotNull(sampleConfig);

      if (this.samplerConfig != null && this.samplerConfig.equals(sampleConfig)) {
        Reader copy = new Reader(this, sampleReaders);
        copy.setInterruptFlagInternal(interruptFlag);
        return copy;
      }

      return null;
    }

    // only visible for printinfo
    FileSKVIterator getSample() {
      if (samplerConfig == null)
        return null;
      return getSample(this.samplerConfig);
    }

     public void printInfo() throws IOException {

      System.out.printf("%-24s : %d\n", "RFile Version", rfileVersion);
      System.out.println();

       for (LocalityGroupMetadata lgm : localityGroups) {
        lgm.printInfo();
        lgm.printInfo(false);
      }

      if (sampleGroups.size() > 0) {

        System.out.println();
        System.out.printf("%-24s :\n", "Sample Configuration");
        System.out.printf("\t%-22s : %s\n", "Sampler class ", samplerConfig.getClassName());
        System.out.printf("\t%-22s : %s\n", "Sampler options ", samplerConfig.getOptions());
        System.out.println();

        for (LocalityGroupMetadata lgm : sampleGroups) {
          lgm.printInfo(true);
        }
       }
     }
 
@@ -1071,7 +1362,7 @@ public class RFile {
 
     private void setInterruptFlagInternal(AtomicBoolean flag) {
       this.interruptFlag = flag;
      for (LocalityGroupReader lgr : lgReaders) {
      for (LocalityGroupReader lgr : currentReaders) {
         lgr.setInterruptFlag(interruptFlag);
       }
     }
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
index 088abfe0e..17e8e9697 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
@@ -33,6 +33,9 @@ import org.apache.accumulo.core.file.blockfile.cache.BlockCache;
 import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
 import org.apache.accumulo.core.file.rfile.RFile.Reader;
 import org.apache.accumulo.core.file.rfile.RFile.Writer;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
@@ -123,8 +126,15 @@ public class RFileOperations extends FileOperations {
     long blockSize = acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE);
     long indexBlockSize = acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX);
 
    SamplerConfigurationImpl samplerConfig = SamplerConfigurationImpl.newSamplerConfig(acuconf);
    Sampler sampler = null;

    if (samplerConfig != null) {
      sampler = SamplerFactory.newSampler(samplerConfig, acuconf);
    }

     CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(fs.create(new Path(file), false, bufferSize, (short) rep, block), compression, conf, acuconf);
    Writer writer = new RFile.Writer(_cbw, (int) blockSize, (int) indexBlockSize);
    Writer writer = new RFile.Writer(_cbw, (int) blockSize, (int) indexBlockSize, samplerConfig, sampler);
     return writer;
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java b/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
index 5a53e9309..5dbafa603 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
@@ -18,6 +18,8 @@ package org.apache.accumulo.core.iterators;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -37,4 +39,52 @@ public interface IteratorEnvironment {
   void registerSideChannel(SortedKeyValueIterator<Key,Value> iter);
 
   Authorizations getAuthorizations();

  /**
   * Returns a new iterator environment object that can be used to create deep copies over sample data. The new object created will use the current sampling
   * configuration for the table. The existing iterator environment object will not be modified.
   *
   * <p>
   * Since sample data could be created in many different ways, a good practice for an iterator is to verify the sampling configuration is as expected.
   *
   * <p>
   *
   * <pre>
   * <code>
   *   class MyIter implements SortedKeyValueIterator&lt;Key,Value&gt; {
   *     SortedKeyValueIterator&lt;Key,Value&gt; source;
   *     SortedKeyValueIterator&lt;Key,Value&gt; sampleIter;
   *     &#64;Override
   *     void init(SortedKeyValueIterator&lt;Key,Value&gt; source, Map&lt;String,String&gt; options, IteratorEnvironment env) {
   *       IteratorEnvironment sampleEnv = env.cloneWithSamplingEnabled();
   *       //do some sanity checks on sampling config
   *       validateSamplingConfiguration(sampleEnv.getSamplerConfiguration());
   *       sampleIter = source.deepCopy(sampleEnv);
   *       this.source = source;
   *     }
   *   }
   * </code>
   * </pre>
   *
   * @throws SampleNotPresentException
   *           when sampling is not configured for table.
   * @since 1.8.0
   */
  IteratorEnvironment cloneWithSamplingEnabled();

  /**
   * There are at least two conditions under which sampling will be enabled for an environment. One condition is when sampling is enabled for the scan that
   * starts everything. Another possibility is for a deep copy created with an environment created by calling {@link #cloneWithSamplingEnabled()}
   *
   * @return true if sampling is enabled for this environment.
   * @since 1.8.0
   */
  boolean isSamplingEnabled();

  /**
   *
   * @return sampling configuration is sampling is enabled for environment, otherwise returns null.
   * @since 1.8.0
   */
  SamplerConfiguration getSamplerConfiguration();
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/SortedMapIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/SortedMapIterator.java
index 3999b6fc7..25c010da4 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/SortedMapIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/SortedMapIterator.java
@@ -24,6 +24,7 @@ import java.util.Map.Entry;
 import java.util.SortedMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
@@ -53,6 +54,9 @@ public class SortedMapIterator implements InterruptibleIterator {
 
   @Override
   public SortedMapIterator deepCopy(IteratorEnvironment env) {
    if (env != null && env.isSamplingEnabled()) {
      throw new SampleNotPresentException();
    }
     return new SortedMapIterator(map, interruptFlag);
   }
 
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/WrappingIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/WrappingIterator.java
index 7723ef155..5b37b302c 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/WrappingIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/WrappingIterator.java
@@ -56,8 +56,6 @@ public abstract class WrappingIterator implements SortedKeyValueIterator<Key,Val
 
   @Override
   public Key getTopKey() {
    if (source == null)
      throw new IllegalStateException("no source set");
     if (seenSeek == false)
       throw new IllegalStateException("never been seeked");
     return getSource().getTopKey();
@@ -65,8 +63,6 @@ public abstract class WrappingIterator implements SortedKeyValueIterator<Key,Val
 
   @Override
   public Value getTopValue() {
    if (source == null)
      throw new IllegalStateException("no source set");
     if (seenSeek == false)
       throw new IllegalStateException("never been seeked");
     return getSource().getTopValue();
@@ -74,8 +70,6 @@ public abstract class WrappingIterator implements SortedKeyValueIterator<Key,Val
 
   @Override
   public boolean hasTop() {
    if (source == null)
      throw new IllegalStateException("no source set");
     if (seenSeek == false)
       throw new IllegalStateException("never been seeked");
     return getSource().hasTop();
@@ -89,8 +83,6 @@ public abstract class WrappingIterator implements SortedKeyValueIterator<Key,Val
 
   @Override
   public void next() throws IOException {
    if (source == null)
      throw new IllegalStateException("no source set");
     if (seenSeek == false)
       throw new IllegalStateException("never been seeked");
     getSource().next();
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/system/EmptyIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/system/EmptyIterator.java
new file mode 100644
index 000000000..b791eb1b3
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/iterators/system/EmptyIterator.java
@@ -0,0 +1,72 @@
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

package org.apache.accumulo.core.iterators.system;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

public class EmptyIterator implements InterruptibleIterator {

  public static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

  @Override
  public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {}

  @Override
  public boolean hasTop() {
    return false;
  }

  @Override
  public void next() throws IOException {
    // nothing should call this since hasTop always returns false
    throw new UnsupportedOperationException();
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {}

  @Override
  public Key getTopKey() {
    // nothing should call this since hasTop always returns false
    throw new UnsupportedOperationException();
  }

  @Override
  public Value getTopValue() {
    // nothing should call this since hasTop always returns false
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return EMPTY_ITERATOR;
  }

  @Override
  public void setInterruptFlag(AtomicBoolean flag) {}
}
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/system/MapFileIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/system/MapFileIterator.java
index 9d59570f1..f9f0600c1 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/system/MapFileIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/system/MapFileIterator.java
@@ -33,6 +33,7 @@ import org.apache.accumulo.core.file.map.MapFileUtil;
 import org.apache.accumulo.core.iterators.IterationInterruptedException;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
@@ -154,4 +155,9 @@ public class MapFileIterator implements FileSKVIterator {
   public void close() throws IOException {
     reader.close();
   }

  @Override
  public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
    return null;
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
new file mode 100644
index 000000000..aedcdbaea
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
@@ -0,0 +1,46 @@
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

package org.apache.accumulo.core.iterators.system;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;

public class SampleIterator extends Filter {

  private Sampler sampler = new RowSampler();

  public SampleIterator(SortedKeyValueIterator<Key,Value> iter, Sampler sampler) {
    setSource(iter);
    this.sampler = sampler;
  }

  @Override
  public boolean accept(Key k, Value v) {
    return sampler.accept(k);
  }

  @Override
  public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
    return new SampleIterator(getSource().deepCopy(env), sampler);
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/system/SequenceFileIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/system/SequenceFileIterator.java
index 8710acd0c..8ea380070 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/system/SequenceFileIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/system/SequenceFileIterator.java
@@ -29,6 +29,7 @@ import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.file.FileSKVIterator;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.io.SequenceFile;
 import org.apache.hadoop.io.SequenceFile.Reader;
 
@@ -126,4 +127,9 @@ public class SequenceFileIterator implements FileSKVIterator {
   public void setInterruptFlag(AtomicBoolean flag) {
     throw new UnsupportedOperationException();
   }

  @Override
  public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
    throw new UnsupportedOperationException();
  }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java b/core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java
new file mode 100644
index 000000000..ae2b951b4
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java
@@ -0,0 +1,106 @@
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

package org.apache.accumulo.core.sample;

import java.util.Set;

import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.data.Key;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * A base class that can be used to create Samplers based on hashing. This class offers consistent options for configuring the hash function. The subclass
 * decides which parts of the key to hash.
 *
 * <p>
 * This class support two options passed into {@link #init(SamplerConfiguration)}. One option is {@code hasher} which specifies a hashing algorithm. Valid
 * values for this option are {@code md5}, {@code sha1}, and {@code murmur3_32}. If you are not sure, then choose {@code murmur3_32}.
 *
 * <p>
 * The second option is {@code modulus} which can have any positive integer as a value.
 *
 * <p>
 * Any data where {@code hash(data) % modulus == 0} will be selected for the sample.
 *
 * @since 1.8.0
 */

public abstract class AbstractHashSampler implements Sampler {

  private HashFunction hashFunction;
  private int modulus;

  private static final Set<String> VALID_OPTIONS = ImmutableSet.of("hasher", "modulus");

  /**
   * Subclasses with options should override this method and return true if the option is valid for the subclass or if {@code super.isValidOption(opt)} returns
   * true.
   */

  protected boolean isValidOption(String option) {
    return VALID_OPTIONS.contains(option);
  }

  /**
   * Subclasses with options should override this method and call {@code super.init(config)}.
   */

  @Override
  public void init(SamplerConfiguration config) {
    String hasherOpt = config.getOptions().get("hasher");
    String modulusOpt = config.getOptions().get("modulus");

    Preconditions.checkNotNull(hasherOpt, "Hasher not specified");
    Preconditions.checkNotNull(modulusOpt, "Modulus not specified");

    for (String option : config.getOptions().keySet()) {
      Preconditions.checkArgument(isValidOption(option), "Unknown option : %s", option);
    }

    switch (hasherOpt) {
      case "murmur3_32":
        hashFunction = Hashing.murmur3_32();
        break;
      case "md5":
        hashFunction = Hashing.md5();
        break;
      case "sha1":
        hashFunction = Hashing.sha1();
        break;
      default:
        throw new IllegalArgumentException("Uknown hahser " + hasherOpt);
    }

    modulus = Integer.parseInt(modulusOpt);
  }

  /**
   * Subclass must override this method and hash some portion of the key.
   */
  protected abstract HashCode hash(HashFunction hashFunction, Key k);

  @Override
  public boolean accept(Key k) {
    return hash(hashFunction, k).asInt() % modulus == 0;
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java b/core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java
new file mode 100644
index 000000000..ad68cf60e
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java
@@ -0,0 +1,124 @@
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

package org.apache.accumulo.core.sample;

import java.util.Set;

import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;

/**
 * This sampler can hash any subset of a Key's fields. The fields that hashed for the sample are determined by the configuration options passed in
 * {@link #init(SamplerConfiguration)}. The following key values are valid options.
 *
 * <UL>
 * <li>row=true|false
 * <li>family=true|false
 * <li>qualifier=true|false
 * <li>visibility=true|false
 * </UL>
 *
 * <p>
 * If not specified in the options, fields default to false.
 *
 * <p>
 * To determine what options are valid for hashing see {@link AbstractHashSampler}
 *
 * <p>
 * To configure Accumulo to generate sample data on one thousandth of the column qualifiers, the following SamplerConfiguration could be created and used to
 * configure a table.
 *
 * <p>
 * {@code new SamplerConfiguration(RowColumnSampler.class.getName()).setOptions(ImmutableMap.of("hasher","murmur3_32","modulus","1009","qualifier","true"))}
 *
 * <p>
 * With this configuration, if a column qualifier is selected then all key values contains that column qualifier will end up in the sample data.
 *
 * @since 1.8.0
 */

public class RowColumnSampler extends AbstractHashSampler {

  private boolean row = true;
  private boolean family = true;
  private boolean qualifier = true;
  private boolean visibility = true;

  private static final Set<String> VALID_OPTIONS = ImmutableSet.of("row", "family", "qualifier", "visibility");

  private boolean hashField(SamplerConfiguration config, String field) {
    String optValue = config.getOptions().get(field);
    if (optValue != null) {
      return Boolean.parseBoolean(optValue);
    }

    return false;
  }

  @Override
  protected boolean isValidOption(String option) {
    return super.isValidOption(option) || VALID_OPTIONS.contains(option);
  }

  @Override
  public void init(SamplerConfiguration config) {
    super.init(config);

    row = hashField(config, "row");
    family = hashField(config, "family");
    qualifier = hashField(config, "qualifier");
    visibility = hashField(config, "visibility");

    if (!row && !family && !qualifier && !visibility) {
      throw new IllegalStateException("Must hash at least one key field");
    }
  }

  private void putByteSquence(ByteSequence data, Hasher hasher) {
    hasher.putBytes(data.getBackingArray(), data.offset(), data.length());
  }

  @Override
  protected HashCode hash(HashFunction hashFunction, Key k) {
    Hasher hasher = hashFunction.newHasher();

    if (row) {
      putByteSquence(k.getRowData(), hasher);
    }

    if (family) {
      putByteSquence(k.getColumnFamilyData(), hasher);
    }

    if (qualifier) {
      putByteSquence(k.getColumnQualifierData(), hasher);
    }

    if (visibility) {
      putByteSquence(k.getColumnVisibilityData(), hasher);
    }

    return hasher.hash();
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java b/core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java
new file mode 100644
index 000000000..8690a1c4e
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java
@@ -0,0 +1,49 @@
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

package org.apache.accumulo.core.sample;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

/**
 * Builds a sample based on entire rows. If a row is selected for the sample, then all of its columns will be included.
 *
 * <p>
 * To determine what options are valid for hashing see {@link AbstractHashSampler}. This class offers no addition options, it always hashes on the row.
 *
 * <p>
 * To configure Accumulo to generate sample data on one thousandth of the rows, the following SamplerConfiguration could be created and used to configure a
 * table.
 *
 * <p>
 * {@code new SamplerConfiguration(RowSampler.class.getName()).setOptions(ImmutableMap.of("hasher","murmur3_32","modulus","1009"))}
 *
 * @since 1.8.0
 */

public class RowSampler extends AbstractHashSampler {

  @Override
  protected HashCode hash(HashFunction hashFunction, Key k) {
    ByteSequence row = k.getRowData();
    return hashFunction.hashBytes(row.getBackingArray(), row.offset(), row.length());
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/Sampler.java b/core/src/main/java/org/apache/accumulo/core/sample/Sampler.java
new file mode 100644
index 000000000..64adeecb2
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/Sampler.java
@@ -0,0 +1,57 @@
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

package org.apache.accumulo.core.sample;

import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.data.Key;

/**
 * A function that decides which key values are stored in a tables sample. As Accumuo compacts data and creates rfiles it uses a Sampler to decided what to
 * store in the rfiles sample section. The class name of the Sampler and the Samplers configuration are stored in each rfile. A scan of a tables sample will
 * only succeed if all rfiles were created with the same sampler and sampler configuration.
 *
 * <p>
 * Since the decisions that Sampler makes are persisted, the behavior of a Sampler for a given configuration should always be the same. One way to offer a new
 * behavior is to offer new options, while still supporting old behavior with a Samplers existing options.
 *
 * <p>
 * Ideally a sampler that selects a Key k1 would also select updates for k1. For example if a Sampler selects :
 * {@code row='000989' family='name' qualifier='last' visibility='ADMIN' time=9 value='Doe'}, it would be nice if it also selected :
 * {@code row='000989' family='name' qualifier='last' visibility='ADMIN' time=20 value='Dough'}. Using hash and modulo on the key fields is a good way to
 * accomplish this and {@link AbstractHashSampler} provides a good basis for implementation.
 *
 * @since 1.8.0
 */

public interface Sampler {

  /**
   * An implementation of Sampler must have a noarg constructor. After construction this method is called once to initialize a sampler before it is used.
   *
   * @param config
   *          Configuration options for a sampler.
   */
  void init(SamplerConfiguration config);

  /**
   * @param k
   *          A key that was written to a rfile.
   * @return True if the key (and its associtated value) should be stored in the rfile's sample. Return false if it should not be included.
   */
  boolean accept(Key k);
}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
new file mode 100644
index 000000000..348def42e
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
@@ -0,0 +1,184 @@
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

package org.apache.accumulo.core.sample.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.tabletserver.thrift.TSamplerConfiguration;
import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.io.Writable;

public class SamplerConfigurationImpl implements Writable {
  private String className;
  private Map<String,String> options;

  public SamplerConfigurationImpl(DataInput in) throws IOException {
    readFields(in);
  }

  public SamplerConfigurationImpl(SamplerConfiguration sc) {
    this.className = sc.getSamplerClassName();
    this.options = new HashMap<>(sc.getOptions());
  }

  public SamplerConfigurationImpl(String className, Map<String,String> options) {
    this.className = className;
    this.options = options;
  }

  public SamplerConfigurationImpl() {}

  public String getClassName() {
    return className;
  }

  public Map<String,String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  @Override
  public int hashCode() {
    return 31 * className.hashCode() + options.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SamplerConfigurationImpl) {
      SamplerConfigurationImpl osc = (SamplerConfigurationImpl) o;

      return className.equals(osc.className) && options.equals(osc.options);
    }

    return false;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    // The Writable serialization methods for this class are called by RFile and therefore must be very stable. An alternative way to serialize this class is to
    // use Thrift. That was not used here inorder to avoid making RFile depend on Thrift.

    // versioning info
    out.write(1);

    out.writeUTF(className);

    out.writeInt(options.size());

    for (Entry<String,String> entry : options.entrySet()) {
      out.writeUTF(entry.getKey());
      out.writeUTF(entry.getValue());
    }
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    int version = in.readByte();

    if (version != 1) {
      throw new IllegalArgumentException("Unexpected version " + version);
    }

    className = in.readUTF();

    options = new HashMap<String,String>();

    int num = in.readInt();

    for (int i = 0; i < num; i++) {
      String key = in.readUTF();
      String val = in.readUTF();
      options.put(key, val);
    }
  }

  public SamplerConfiguration toSamplerConfiguration() {
    SamplerConfiguration sc = new SamplerConfiguration(className);
    sc.setOptions(options);
    return sc;
  }

  public List<Pair<String,String>> toTableProperties() {
    ArrayList<Pair<String,String>> props = new ArrayList<>();

    for (Entry<String,String> entry : options.entrySet()) {
      props.add(new Pair<String,String>(Property.TABLE_SAMPLER_OPTS.getKey() + entry.getKey(), entry.getValue()));
    }

    // intentionally added last, so its set last
    props.add(new Pair<String,String>(Property.TABLE_SAMPLER.getKey(), className));

    return props;
  }

  public Map<String,String> toTablePropertiesMap() {
    LinkedHashMap<String,String> propsMap = new LinkedHashMap<>();
    for (Pair<String,String> pair : toTableProperties()) {
      propsMap.put(pair.getFirst(), pair.getSecond());
    }

    return propsMap;
  }

  public static SamplerConfigurationImpl newSamplerConfig(AccumuloConfiguration acuconf) {
    String className = acuconf.get(Property.TABLE_SAMPLER);

    if (className == null || className.equals("")) {
      return null;
    }

    Map<String,String> rawOptions = acuconf.getAllPropertiesWithPrefix(Property.TABLE_SAMPLER_OPTS);
    Map<String,String> options = new HashMap<>();

    for (Entry<String,String> entry : rawOptions.entrySet()) {
      String key = entry.getKey().substring(Property.TABLE_SAMPLER_OPTS.getKey().length());
      options.put(key, entry.getValue());
    }

    return new SamplerConfigurationImpl(className, options);
  }

  @Override
  public String toString() {
    return className + " " + options;
  }

  public static TSamplerConfiguration toThrift(SamplerConfiguration samplerConfig) {
    if (samplerConfig == null)
      return null;
    return new TSamplerConfiguration(samplerConfig.getSamplerClassName(), samplerConfig.getOptions());
  }

  public static SamplerConfiguration fromThrift(TSamplerConfiguration tsc) {
    if (tsc == null)
      return null;
    return new SamplerConfiguration(tsc.getClassName()).setOptions(tsc.getOptions());
  }

}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
new file mode 100644
index 000000000..3f11fbee7
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
@@ -0,0 +1,48 @@
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

package org.apache.accumulo.core.sample.impl;

import java.io.IOException;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;

public class SamplerFactory {
  public static Sampler newSampler(SamplerConfigurationImpl config, AccumuloConfiguration acuconf) throws IOException {
    String context = acuconf.get(Property.TABLE_CLASSPATH);

    Class<? extends Sampler> clazz;
    try {
      if (context != null && !context.equals(""))
        clazz = AccumuloVFSClassLoader.getContextManager().loadClass(context, config.getClassName(), Sampler.class);
      else
        clazz = AccumuloVFSClassLoader.loadClass(config.getClassName(), Sampler.class);

      Sampler sampler = clazz.newInstance();

      sampler.init(config.toSamplerConfiguration());

      return sampler;

    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSampleNotPresentException.java b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSampleNotPresentException.java
new file mode 100644
index 000000000..c4ef7f361
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSampleNotPresentException.java
@@ -0,0 +1,409 @@
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
package org.apache.accumulo.core.tabletserver.thrift;

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

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TSampleNotPresentException extends TException implements org.apache.thrift.TBase<TSampleNotPresentException, TSampleNotPresentException._Fields>, java.io.Serializable, Cloneable, Comparable<TSampleNotPresentException> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TSampleNotPresentException");

  private static final org.apache.thrift.protocol.TField EXTENT_FIELD_DESC = new org.apache.thrift.protocol.TField("extent", org.apache.thrift.protocol.TType.STRUCT, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TSampleNotPresentExceptionStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TSampleNotPresentExceptionTupleSchemeFactory());
  }

  public org.apache.accumulo.core.data.thrift.TKeyExtent extent; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    EXTENT((short)1, "extent");

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
        case 1: // EXTENT
          return EXTENT;
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
    tmpMap.put(_Fields.EXTENT, new org.apache.thrift.meta_data.FieldMetaData("extent", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.data.thrift.TKeyExtent.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TSampleNotPresentException.class, metaDataMap);
  }

  public TSampleNotPresentException() {
  }

  public TSampleNotPresentException(
    org.apache.accumulo.core.data.thrift.TKeyExtent extent)
  {
    this();
    this.extent = extent;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TSampleNotPresentException(TSampleNotPresentException other) {
    if (other.isSetExtent()) {
      this.extent = new org.apache.accumulo.core.data.thrift.TKeyExtent(other.extent);
    }
  }

  public TSampleNotPresentException deepCopy() {
    return new TSampleNotPresentException(this);
  }

  @Override
  public void clear() {
    this.extent = null;
  }

  public org.apache.accumulo.core.data.thrift.TKeyExtent getExtent() {
    return this.extent;
  }

  public TSampleNotPresentException setExtent(org.apache.accumulo.core.data.thrift.TKeyExtent extent) {
    this.extent = extent;
    return this;
  }

  public void unsetExtent() {
    this.extent = null;
  }

  /** Returns true if field extent is set (has been assigned a value) and false otherwise */
  public boolean isSetExtent() {
    return this.extent != null;
  }

  public void setExtentIsSet(boolean value) {
    if (!value) {
      this.extent = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case EXTENT:
      if (value == null) {
        unsetExtent();
      } else {
        setExtent((org.apache.accumulo.core.data.thrift.TKeyExtent)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case EXTENT:
      return getExtent();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case EXTENT:
      return isSetExtent();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TSampleNotPresentException)
      return this.equals((TSampleNotPresentException)that);
    return false;
  }

  public boolean equals(TSampleNotPresentException that) {
    if (that == null)
      return false;

    boolean this_present_extent = true && this.isSetExtent();
    boolean that_present_extent = true && that.isSetExtent();
    if (this_present_extent || that_present_extent) {
      if (!(this_present_extent && that_present_extent))
        return false;
      if (!this.extent.equals(that.extent))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(TSampleNotPresentException other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetExtent()).compareTo(other.isSetExtent());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExtent()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.extent, other.extent);
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
    StringBuilder sb = new StringBuilder("TSampleNotPresentException(");
    boolean first = true;

    sb.append("extent:");
    if (this.extent == null) {
      sb.append("null");
    } else {
      sb.append(this.extent);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (extent != null) {
      extent.validate();
    }
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

  private static class TSampleNotPresentExceptionStandardSchemeFactory implements SchemeFactory {
    public TSampleNotPresentExceptionStandardScheme getScheme() {
      return new TSampleNotPresentExceptionStandardScheme();
    }
  }

  private static class TSampleNotPresentExceptionStandardScheme extends StandardScheme<TSampleNotPresentException> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TSampleNotPresentException struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // EXTENT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.extent = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              struct.extent.read(iprot);
              struct.setExtentIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TSampleNotPresentException struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.extent != null) {
        oprot.writeFieldBegin(EXTENT_FIELD_DESC);
        struct.extent.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TSampleNotPresentExceptionTupleSchemeFactory implements SchemeFactory {
    public TSampleNotPresentExceptionTupleScheme getScheme() {
      return new TSampleNotPresentExceptionTupleScheme();
    }
  }

  private static class TSampleNotPresentExceptionTupleScheme extends TupleScheme<TSampleNotPresentException> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TSampleNotPresentException struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetExtent()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetExtent()) {
        struct.extent.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TSampleNotPresentException struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.extent = new org.apache.accumulo.core.data.thrift.TKeyExtent();
        struct.extent.read(iprot);
        struct.setExtentIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSamplerConfiguration.java b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSamplerConfiguration.java
new file mode 100644
index 000000000..2d2b2d525
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TSamplerConfiguration.java
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
package org.apache.accumulo.core.tabletserver.thrift;

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

@SuppressWarnings({"unchecked", "serial", "rawtypes", "unused"}) public class TSamplerConfiguration implements org.apache.thrift.TBase<TSamplerConfiguration, TSamplerConfiguration._Fields>, java.io.Serializable, Cloneable, Comparable<TSamplerConfiguration> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TSamplerConfiguration");

  private static final org.apache.thrift.protocol.TField CLASS_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("className", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField OPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("options", org.apache.thrift.protocol.TType.MAP, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TSamplerConfigurationStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TSamplerConfigurationTupleSchemeFactory());
  }

  public String className; // required
  public Map<String,String> options; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
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
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TSamplerConfiguration.class, metaDataMap);
  }

  public TSamplerConfiguration() {
  }

  public TSamplerConfiguration(
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
  public TSamplerConfiguration(TSamplerConfiguration other) {
    if (other.isSetClassName()) {
      this.className = other.className;
    }
    if (other.isSetOptions()) {
      Map<String,String> __this__options = new HashMap<String,String>(other.options);
      this.options = __this__options;
    }
  }

  public TSamplerConfiguration deepCopy() {
    return new TSamplerConfiguration(this);
  }

  @Override
  public void clear() {
    this.className = null;
    this.options = null;
  }

  public String getClassName() {
    return this.className;
  }

  public TSamplerConfiguration setClassName(String className) {
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

  public TSamplerConfiguration setOptions(Map<String,String> options) {
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
    if (that instanceof TSamplerConfiguration)
      return this.equals((TSamplerConfiguration)that);
    return false;
  }

  public boolean equals(TSamplerConfiguration that) {
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
  public int compareTo(TSamplerConfiguration other) {
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
    StringBuilder sb = new StringBuilder("TSamplerConfiguration(");
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

  private static class TSamplerConfigurationStandardSchemeFactory implements SchemeFactory {
    public TSamplerConfigurationStandardScheme getScheme() {
      return new TSamplerConfigurationStandardScheme();
    }
  }

  private static class TSamplerConfigurationStandardScheme extends StandardScheme<TSamplerConfiguration> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TSamplerConfiguration struct) throws org.apache.thrift.TException {
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
                org.apache.thrift.protocol.TMap _map106 = iprot.readMapBegin();
                struct.options = new HashMap<String,String>(2*_map106.size);
                for (int _i107 = 0; _i107 < _map106.size; ++_i107)
                {
                  String _key108;
                  String _val109;
                  _key108 = iprot.readString();
                  _val109 = iprot.readString();
                  struct.options.put(_key108, _val109);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TSamplerConfiguration struct) throws org.apache.thrift.TException {
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
          for (Map.Entry<String, String> _iter110 : struct.options.entrySet())
          {
            oprot.writeString(_iter110.getKey());
            oprot.writeString(_iter110.getValue());
          }
          oprot.writeMapEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TSamplerConfigurationTupleSchemeFactory implements SchemeFactory {
    public TSamplerConfigurationTupleScheme getScheme() {
      return new TSamplerConfigurationTupleScheme();
    }
  }

  private static class TSamplerConfigurationTupleScheme extends TupleScheme<TSamplerConfiguration> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TSamplerConfiguration struct) throws org.apache.thrift.TException {
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
          for (Map.Entry<String, String> _iter111 : struct.options.entrySet())
          {
            oprot.writeString(_iter111.getKey());
            oprot.writeString(_iter111.getValue());
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TSamplerConfiguration struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.className = iprot.readString();
        struct.setClassNameIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TMap _map112 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.options = new HashMap<String,String>(2*_map112.size);
          for (int _i113 = 0; _i113 < _map112.size; ++_i113)
          {
            String _key114;
            String _val115;
            _key114 = iprot.readString();
            _val115 = iprot.readString();
            struct.options.put(_key114, _val115);
          }
        }
        struct.setOptionsIsSet(true);
      }
    }
  }

}

diff --git a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
index bd0f79cea..f453788c5 100644
-- a/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
++ b/core/src/main/java/org/apache/accumulo/core/tabletserver/thrift/TabletClientService.java
@@ -52,15 +52,15 @@ import org.slf4j.LoggerFactory;
 
   public interface Iface extends org.apache.accumulo.core.client.impl.thrift.ClientService.Iface {
 
    public org.apache.accumulo.core.data.thrift.InitialScan startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException;
    public org.apache.accumulo.core.data.thrift.InitialScan startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException;
 
    public org.apache.accumulo.core.data.thrift.ScanResult continueScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException;
    public org.apache.accumulo.core.data.thrift.ScanResult continueScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException;
 
     public void closeScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws org.apache.thrift.TException;
 
    public org.apache.accumulo.core.data.thrift.InitialMultiScan startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException;
    public org.apache.accumulo.core.data.thrift.InitialMultiScan startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, TSampleNotPresentException, org.apache.thrift.TException;
 
    public org.apache.accumulo.core.data.thrift.MultiScanResult continueMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, org.apache.thrift.TException;
    public org.apache.accumulo.core.data.thrift.MultiScanResult continueMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, TSampleNotPresentException, org.apache.thrift.TException;
 
     public void closeMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, org.apache.thrift.TException;
 
@@ -118,13 +118,13 @@ import org.slf4j.LoggerFactory;
 
   public interface AsyncIface extends org.apache.accumulo.core.client.impl.thrift.ClientService .AsyncIface {
 
    public void startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
    public void startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
     public void continueScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
     public void closeScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
    public void startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
    public void startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
     public void continueMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException;
 
@@ -202,13 +202,13 @@ import org.slf4j.LoggerFactory;
       super(iprot, oprot);
     }
 
    public org.apache.accumulo.core.data.thrift.InitialScan startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.InitialScan startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException
     {
      send_startScan(tinfo, credentials, extent, range, columns, batchSize, ssiList, ssio, authorizations, waitForWrites, isolated, readaheadThreshold, batchTimeOut);
      send_startScan(tinfo, credentials, extent, range, columns, batchSize, ssiList, ssio, authorizations, waitForWrites, isolated, readaheadThreshold, samplerConfig, batchTimeOut);
       return recv_startScan();
     }
 
    public void send_startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut) throws org.apache.thrift.TException
    public void send_startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.thrift.TException
     {
       startScan_args args = new startScan_args();
       args.setTinfo(tinfo);
@@ -223,11 +223,12 @@ import org.slf4j.LoggerFactory;
       args.setWaitForWrites(waitForWrites);
       args.setIsolated(isolated);
       args.setReadaheadThreshold(readaheadThreshold);
      args.setSamplerConfig(samplerConfig);
       args.setBatchTimeOut(batchTimeOut);
       sendBase("startScan", args);
     }
 
    public org.apache.accumulo.core.data.thrift.InitialScan recv_startScan() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.InitialScan recv_startScan() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException
     {
       startScan_result result = new startScan_result();
       receiveBase(result, "startScan");
@@ -243,10 +244,13 @@ import org.slf4j.LoggerFactory;
       if (result.tmfe != null) {
         throw result.tmfe;
       }
      if (result.tsnpe != null) {
        throw result.tsnpe;
      }
       throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "startScan failed: unknown result");
     }
 
    public org.apache.accumulo.core.data.thrift.ScanResult continueScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.ScanResult continueScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException
     {
       send_continueScan(tinfo, scanID);
       return recv_continueScan();
@@ -260,7 +264,7 @@ import org.slf4j.LoggerFactory;
       sendBase("continueScan", args);
     }
 
    public org.apache.accumulo.core.data.thrift.ScanResult recv_continueScan() throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.ScanResult recv_continueScan() throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException
     {
       continueScan_result result = new continueScan_result();
       receiveBase(result, "continueScan");
@@ -276,6 +280,9 @@ import org.slf4j.LoggerFactory;
       if (result.tmfe != null) {
         throw result.tmfe;
       }
      if (result.tsnpe != null) {
        throw result.tsnpe;
      }
       throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "continueScan failed: unknown result");
     }
 
@@ -292,13 +299,13 @@ import org.slf4j.LoggerFactory;
       sendBase("closeScan", args);
     }
 
    public org.apache.accumulo.core.data.thrift.InitialMultiScan startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.InitialMultiScan startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, TSampleNotPresentException, org.apache.thrift.TException
     {
      send_startMultiScan(tinfo, credentials, batch, columns, ssiList, ssio, authorizations, waitForWrites, batchTimeOut);
      send_startMultiScan(tinfo, credentials, batch, columns, ssiList, ssio, authorizations, waitForWrites, samplerConfig, batchTimeOut);
       return recv_startMultiScan();
     }
 
    public void send_startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut) throws org.apache.thrift.TException
    public void send_startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut) throws org.apache.thrift.TException
     {
       startMultiScan_args args = new startMultiScan_args();
       args.setTinfo(tinfo);
@@ -309,11 +316,12 @@ import org.slf4j.LoggerFactory;
       args.setSsio(ssio);
       args.setAuthorizations(authorizations);
       args.setWaitForWrites(waitForWrites);
      args.setSamplerConfig(samplerConfig);
       args.setBatchTimeOut(batchTimeOut);
       sendBase("startMultiScan", args);
     }
 
    public org.apache.accumulo.core.data.thrift.InitialMultiScan recv_startMultiScan() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.InitialMultiScan recv_startMultiScan() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, TSampleNotPresentException, org.apache.thrift.TException
     {
       startMultiScan_result result = new startMultiScan_result();
       receiveBase(result, "startMultiScan");
@@ -323,10 +331,13 @@ import org.slf4j.LoggerFactory;
       if (result.sec != null) {
         throw result.sec;
       }
      if (result.tsnpe != null) {
        throw result.tsnpe;
      }
       throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "startMultiScan failed: unknown result");
     }
 
    public org.apache.accumulo.core.data.thrift.MultiScanResult continueMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.MultiScanResult continueMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, long scanID) throws NoSuchScanIDException, TSampleNotPresentException, org.apache.thrift.TException
     {
       send_continueMultiScan(tinfo, scanID);
       return recv_continueMultiScan();
@@ -340,7 +351,7 @@ import org.slf4j.LoggerFactory;
       sendBase("continueMultiScan", args);
     }
 
    public org.apache.accumulo.core.data.thrift.MultiScanResult recv_continueMultiScan() throws NoSuchScanIDException, org.apache.thrift.TException
    public org.apache.accumulo.core.data.thrift.MultiScanResult recv_continueMultiScan() throws NoSuchScanIDException, TSampleNotPresentException, org.apache.thrift.TException
     {
       continueMultiScan_result result = new continueMultiScan_result();
       receiveBase(result, "continueMultiScan");
@@ -350,6 +361,9 @@ import org.slf4j.LoggerFactory;
       if (result.nssi != null) {
         throw result.nssi;
       }
      if (result.tsnpe != null) {
        throw result.tsnpe;
      }
       throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "continueMultiScan failed: unknown result");
     }
 
@@ -958,9 +972,9 @@ import org.slf4j.LoggerFactory;
       super(protocolFactory, clientManager, transport);
     }
 
    public void startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
    public void startScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
       checkReady();
      startScan_call method_call = new startScan_call(tinfo, credentials, extent, range, columns, batchSize, ssiList, ssio, authorizations, waitForWrites, isolated, readaheadThreshold, batchTimeOut, resultHandler, this, ___protocolFactory, ___transport);
      startScan_call method_call = new startScan_call(tinfo, credentials, extent, range, columns, batchSize, ssiList, ssio, authorizations, waitForWrites, isolated, readaheadThreshold, samplerConfig, batchTimeOut, resultHandler, this, ___protocolFactory, ___transport);
       this.___currentMethod = method_call;
       ___manager.call(method_call);
     }
@@ -978,8 +992,9 @@ import org.slf4j.LoggerFactory;
       private boolean waitForWrites;
       private boolean isolated;
       private long readaheadThreshold;
      private TSamplerConfiguration samplerConfig;
       private long batchTimeOut;
      public startScan_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
      public startScan_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, org.apache.accumulo.core.data.thrift.TKeyExtent extent, org.apache.accumulo.core.data.thrift.TRange range, List<org.apache.accumulo.core.data.thrift.TColumn> columns, int batchSize, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated, long readaheadThreshold, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
         super(client, protocolFactory, transport, resultHandler, false);
         this.tinfo = tinfo;
         this.credentials = credentials;
@@ -993,6 +1008,7 @@ import org.slf4j.LoggerFactory;
         this.waitForWrites = waitForWrites;
         this.isolated = isolated;
         this.readaheadThreshold = readaheadThreshold;
        this.samplerConfig = samplerConfig;
         this.batchTimeOut = batchTimeOut;
       }
 
@@ -1011,12 +1027,13 @@ import org.slf4j.LoggerFactory;
         args.setWaitForWrites(waitForWrites);
         args.setIsolated(isolated);
         args.setReadaheadThreshold(readaheadThreshold);
        args.setSamplerConfig(samplerConfig);
         args.setBatchTimeOut(batchTimeOut);
         args.write(prot);
         prot.writeMessageEnd();
       }
 
      public org.apache.accumulo.core.data.thrift.InitialScan getResult() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException {
      public org.apache.accumulo.core.data.thrift.InitialScan getResult() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException {
         if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
           throw new IllegalStateException("Method call not finished!");
         }
@@ -1051,7 +1068,7 @@ import org.slf4j.LoggerFactory;
         prot.writeMessageEnd();
       }
 
      public org.apache.accumulo.core.data.thrift.ScanResult getResult() throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, org.apache.thrift.TException {
      public org.apache.accumulo.core.data.thrift.ScanResult getResult() throws NoSuchScanIDException, NotServingTabletException, TooManyFilesException, TSampleNotPresentException, org.apache.thrift.TException {
         if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
           throw new IllegalStateException("Method call not finished!");
         }
@@ -1095,9 +1112,9 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public void startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
    public void startMultiScan(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler) throws org.apache.thrift.TException {
       checkReady();
      startMultiScan_call method_call = new startMultiScan_call(tinfo, credentials, batch, columns, ssiList, ssio, authorizations, waitForWrites, batchTimeOut, resultHandler, this, ___protocolFactory, ___transport);
      startMultiScan_call method_call = new startMultiScan_call(tinfo, credentials, batch, columns, ssiList, ssio, authorizations, waitForWrites, samplerConfig, batchTimeOut, resultHandler, this, ___protocolFactory, ___transport);
       this.___currentMethod = method_call;
       ___manager.call(method_call);
     }
@@ -1111,8 +1128,9 @@ import org.slf4j.LoggerFactory;
       private Map<String,Map<String,String>> ssio;
       private List<ByteBuffer> authorizations;
       private boolean waitForWrites;
      private TSamplerConfiguration samplerConfig;
       private long batchTimeOut;
      public startMultiScan_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
      public startMultiScan_call(org.apache.accumulo.core.trace.thrift.TInfo tinfo, org.apache.accumulo.core.security.thrift.TCredentials credentials, Map<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>> batch, List<org.apache.accumulo.core.data.thrift.TColumn> columns, List<org.apache.accumulo.core.data.thrift.IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration samplerConfig, long batchTimeOut, org.apache.thrift.async.AsyncMethodCallback resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
         super(client, protocolFactory, transport, resultHandler, false);
         this.tinfo = tinfo;
         this.credentials = credentials;
@@ -1122,6 +1140,7 @@ import org.slf4j.LoggerFactory;
         this.ssio = ssio;
         this.authorizations = authorizations;
         this.waitForWrites = waitForWrites;
        this.samplerConfig = samplerConfig;
         this.batchTimeOut = batchTimeOut;
       }
 
@@ -1136,12 +1155,13 @@ import org.slf4j.LoggerFactory;
         args.setSsio(ssio);
         args.setAuthorizations(authorizations);
         args.setWaitForWrites(waitForWrites);
        args.setSamplerConfig(samplerConfig);
         args.setBatchTimeOut(batchTimeOut);
         args.write(prot);
         prot.writeMessageEnd();
       }
 
      public org.apache.accumulo.core.data.thrift.InitialMultiScan getResult() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, org.apache.thrift.TException {
      public org.apache.accumulo.core.data.thrift.InitialMultiScan getResult() throws org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException, TSampleNotPresentException, org.apache.thrift.TException {
         if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
           throw new IllegalStateException("Method call not finished!");
         }
@@ -1176,7 +1196,7 @@ import org.slf4j.LoggerFactory;
         prot.writeMessageEnd();
       }
 
      public org.apache.accumulo.core.data.thrift.MultiScanResult getResult() throws NoSuchScanIDException, org.apache.thrift.TException {
      public org.apache.accumulo.core.data.thrift.MultiScanResult getResult() throws NoSuchScanIDException, TSampleNotPresentException, org.apache.thrift.TException {
         if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
           throw new IllegalStateException("Method call not finished!");
         }
@@ -2260,13 +2280,15 @@ import org.slf4j.LoggerFactory;
       public startScan_result getResult(I iface, startScan_args args) throws org.apache.thrift.TException {
         startScan_result result = new startScan_result();
         try {
          result.success = iface.startScan(args.tinfo, args.credentials, args.extent, args.range, args.columns, args.batchSize, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.isolated, args.readaheadThreshold, args.batchTimeOut);
          result.success = iface.startScan(args.tinfo, args.credentials, args.extent, args.range, args.columns, args.batchSize, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.isolated, args.readaheadThreshold, args.samplerConfig, args.batchTimeOut);
         } catch (org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec) {
           result.sec = sec;
         } catch (NotServingTabletException nste) {
           result.nste = nste;
         } catch (TooManyFilesException tmfe) {
           result.tmfe = tmfe;
        } catch (TSampleNotPresentException tsnpe) {
          result.tsnpe = tsnpe;
         }
         return result;
       }
@@ -2295,6 +2317,8 @@ import org.slf4j.LoggerFactory;
           result.nste = nste;
         } catch (TooManyFilesException tmfe) {
           result.tmfe = tmfe;
        } catch (TSampleNotPresentException tsnpe) {
          result.tsnpe = tsnpe;
         }
         return result;
       }
@@ -2335,9 +2359,11 @@ import org.slf4j.LoggerFactory;
       public startMultiScan_result getResult(I iface, startMultiScan_args args) throws org.apache.thrift.TException {
         startMultiScan_result result = new startMultiScan_result();
         try {
          result.success = iface.startMultiScan(args.tinfo, args.credentials, args.batch, args.columns, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.batchTimeOut);
          result.success = iface.startMultiScan(args.tinfo, args.credentials, args.batch, args.columns, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.samplerConfig, args.batchTimeOut);
         } catch (org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec) {
           result.sec = sec;
        } catch (TSampleNotPresentException tsnpe) {
          result.tsnpe = tsnpe;
         }
         return result;
       }
@@ -2362,6 +2388,8 @@ import org.slf4j.LoggerFactory;
           result.success = iface.continueMultiScan(args.tinfo, args.scanID);
         } catch (NoSuchScanIDException nssi) {
           result.nssi = nssi;
        } catch (TSampleNotPresentException tsnpe) {
          result.tsnpe = tsnpe;
         }
         return result;
       }
@@ -3028,6 +3056,11 @@ import org.slf4j.LoggerFactory;
                         result.tmfe = (TooManyFilesException) e;
                         result.setTmfeIsSet(true);
                         msg = result;
            }
            else             if (e instanceof TSampleNotPresentException) {
                        result.tsnpe = (TSampleNotPresentException) e;
                        result.setTsnpeIsSet(true);
                        msg = result;
             }
              else 
             {
@@ -3050,7 +3083,7 @@ import org.slf4j.LoggerFactory;
       }
 
       public void start(I iface, startScan_args args, org.apache.thrift.async.AsyncMethodCallback<org.apache.accumulo.core.data.thrift.InitialScan> resultHandler) throws TException {
        iface.startScan(args.tinfo, args.credentials, args.extent, args.range, args.columns, args.batchSize, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.isolated, args.readaheadThreshold, args.batchTimeOut,resultHandler);
        iface.startScan(args.tinfo, args.credentials, args.extent, args.range, args.columns, args.batchSize, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.isolated, args.readaheadThreshold, args.samplerConfig, args.batchTimeOut,resultHandler);
       }
     }
 
@@ -3095,6 +3128,11 @@ import org.slf4j.LoggerFactory;
                         result.tmfe = (TooManyFilesException) e;
                         result.setTmfeIsSet(true);
                         msg = result;
            }
            else             if (e instanceof TSampleNotPresentException) {
                        result.tsnpe = (TSampleNotPresentException) e;
                        result.setTsnpeIsSet(true);
                        msg = result;
             }
              else 
             {
@@ -3180,6 +3218,11 @@ import org.slf4j.LoggerFactory;
                         result.sec = (org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException) e;
                         result.setSecIsSet(true);
                         msg = result;
            }
            else             if (e instanceof TSampleNotPresentException) {
                        result.tsnpe = (TSampleNotPresentException) e;
                        result.setTsnpeIsSet(true);
                        msg = result;
             }
              else 
             {
@@ -3202,7 +3245,7 @@ import org.slf4j.LoggerFactory;
       }
 
       public void start(I iface, startMultiScan_args args, org.apache.thrift.async.AsyncMethodCallback<org.apache.accumulo.core.data.thrift.InitialMultiScan> resultHandler) throws TException {
        iface.startMultiScan(args.tinfo, args.credentials, args.batch, args.columns, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.batchTimeOut,resultHandler);
        iface.startMultiScan(args.tinfo, args.credentials, args.batch, args.columns, args.ssiList, args.ssio, args.authorizations, args.waitForWrites, args.samplerConfig, args.batchTimeOut,resultHandler);
       }
     }
 
@@ -3237,6 +3280,11 @@ import org.slf4j.LoggerFactory;
                         result.nssi = (NoSuchScanIDException) e;
                         result.setNssiIsSet(true);
                         msg = result;
            }
            else             if (e instanceof TSampleNotPresentException) {
                        result.tsnpe = (TSampleNotPresentException) e;
                        result.setTsnpeIsSet(true);
                        msg = result;
             }
              else 
             {
@@ -4471,7 +4519,8 @@ import org.slf4j.LoggerFactory;
     private static final org.apache.thrift.protocol.TField WAIT_FOR_WRITES_FIELD_DESC = new org.apache.thrift.protocol.TField("waitForWrites", org.apache.thrift.protocol.TType.BOOL, (short)9);
     private static final org.apache.thrift.protocol.TField ISOLATED_FIELD_DESC = new org.apache.thrift.protocol.TField("isolated", org.apache.thrift.protocol.TType.BOOL, (short)10);
     private static final org.apache.thrift.protocol.TField READAHEAD_THRESHOLD_FIELD_DESC = new org.apache.thrift.protocol.TField("readaheadThreshold", org.apache.thrift.protocol.TType.I64, (short)12);
    private static final org.apache.thrift.protocol.TField BATCH_TIME_OUT_FIELD_DESC = new org.apache.thrift.protocol.TField("batchTimeOut", org.apache.thrift.protocol.TType.I64, (short)13);
    private static final org.apache.thrift.protocol.TField SAMPLER_CONFIG_FIELD_DESC = new org.apache.thrift.protocol.TField("samplerConfig", org.apache.thrift.protocol.TType.STRUCT, (short)13);
    private static final org.apache.thrift.protocol.TField BATCH_TIME_OUT_FIELD_DESC = new org.apache.thrift.protocol.TField("batchTimeOut", org.apache.thrift.protocol.TType.I64, (short)14);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -4491,6 +4540,7 @@ import org.slf4j.LoggerFactory;
     public boolean waitForWrites; // required
     public boolean isolated; // required
     public long readaheadThreshold; // required
    public TSamplerConfiguration samplerConfig; // required
     public long batchTimeOut; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
@@ -4507,7 +4557,8 @@ import org.slf4j.LoggerFactory;
       WAIT_FOR_WRITES((short)9, "waitForWrites"),
       ISOLATED((short)10, "isolated"),
       READAHEAD_THRESHOLD((short)12, "readaheadThreshold"),
      BATCH_TIME_OUT((short)13, "batchTimeOut");
      SAMPLER_CONFIG((short)13, "samplerConfig"),
      BATCH_TIME_OUT((short)14, "batchTimeOut");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -4546,7 +4597,9 @@ import org.slf4j.LoggerFactory;
             return ISOLATED;
           case 12: // READAHEAD_THRESHOLD
             return READAHEAD_THRESHOLD;
          case 13: // BATCH_TIME_OUT
          case 13: // SAMPLER_CONFIG
            return SAMPLER_CONFIG;
          case 14: // BATCH_TIME_OUT
             return BATCH_TIME_OUT;
           default:
             return null;
@@ -4628,6 +4681,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
       tmpMap.put(_Fields.READAHEAD_THRESHOLD, new org.apache.thrift.meta_data.FieldMetaData("readaheadThreshold", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
      tmpMap.put(_Fields.SAMPLER_CONFIG, new org.apache.thrift.meta_data.FieldMetaData("samplerConfig", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TSamplerConfiguration.class)));
       tmpMap.put(_Fields.BATCH_TIME_OUT, new org.apache.thrift.meta_data.FieldMetaData("batchTimeOut", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
@@ -4650,6 +4705,7 @@ import org.slf4j.LoggerFactory;
       boolean waitForWrites,
       boolean isolated,
       long readaheadThreshold,
      TSamplerConfiguration samplerConfig,
       long batchTimeOut)
     {
       this();
@@ -4669,6 +4725,7 @@ import org.slf4j.LoggerFactory;
       setIsolatedIsSet(true);
       this.readaheadThreshold = readaheadThreshold;
       setReadaheadThresholdIsSet(true);
      this.samplerConfig = samplerConfig;
       this.batchTimeOut = batchTimeOut;
       setBatchTimeOutIsSet(true);
     }
@@ -4727,6 +4784,9 @@ import org.slf4j.LoggerFactory;
       this.waitForWrites = other.waitForWrites;
       this.isolated = other.isolated;
       this.readaheadThreshold = other.readaheadThreshold;
      if (other.isSetSamplerConfig()) {
        this.samplerConfig = new TSamplerConfiguration(other.samplerConfig);
      }
       this.batchTimeOut = other.batchTimeOut;
     }
 
@@ -4752,6 +4812,7 @@ import org.slf4j.LoggerFactory;
       this.isolated = false;
       setReadaheadThresholdIsSet(false);
       this.readaheadThreshold = 0;
      this.samplerConfig = null;
       setBatchTimeOutIsSet(false);
       this.batchTimeOut = 0;
     }
@@ -5096,6 +5157,30 @@ import org.slf4j.LoggerFactory;
       __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __READAHEADTHRESHOLD_ISSET_ID, value);
     }
 
    public TSamplerConfiguration getSamplerConfig() {
      return this.samplerConfig;
    }

    public startScan_args setSamplerConfig(TSamplerConfiguration samplerConfig) {
      this.samplerConfig = samplerConfig;
      return this;
    }

    public void unsetSamplerConfig() {
      this.samplerConfig = null;
    }

    /** Returns true if field samplerConfig is set (has been assigned a value) and false otherwise */
    public boolean isSetSamplerConfig() {
      return this.samplerConfig != null;
    }

    public void setSamplerConfigIsSet(boolean value) {
      if (!value) {
        this.samplerConfig = null;
      }
    }

     public long getBatchTimeOut() {
       return this.batchTimeOut;
     }
@@ -5217,6 +5302,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case SAMPLER_CONFIG:
        if (value == null) {
          unsetSamplerConfig();
        } else {
          setSamplerConfig((TSamplerConfiguration)value);
        }
        break;

       case BATCH_TIME_OUT:
         if (value == null) {
           unsetBatchTimeOut();
@@ -5266,6 +5359,9 @@ import org.slf4j.LoggerFactory;
       case READAHEAD_THRESHOLD:
         return Long.valueOf(getReadaheadThreshold());
 
      case SAMPLER_CONFIG:
        return getSamplerConfig();

       case BATCH_TIME_OUT:
         return Long.valueOf(getBatchTimeOut());
 
@@ -5304,6 +5400,8 @@ import org.slf4j.LoggerFactory;
         return isSetIsolated();
       case READAHEAD_THRESHOLD:
         return isSetReadaheadThreshold();
      case SAMPLER_CONFIG:
        return isSetSamplerConfig();
       case BATCH_TIME_OUT:
         return isSetBatchTimeOut();
       }
@@ -5431,6 +5529,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_samplerConfig = true && this.isSetSamplerConfig();
      boolean that_present_samplerConfig = true && that.isSetSamplerConfig();
      if (this_present_samplerConfig || that_present_samplerConfig) {
        if (!(this_present_samplerConfig && that_present_samplerConfig))
          return false;
        if (!this.samplerConfig.equals(that.samplerConfig))
          return false;
      }

       boolean this_present_batchTimeOut = true;
       boolean that_present_batchTimeOut = true;
       if (this_present_batchTimeOut || that_present_batchTimeOut) {
@@ -5576,6 +5683,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetSamplerConfig()).compareTo(other.isSetSamplerConfig());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSamplerConfig()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.samplerConfig, other.samplerConfig);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       lastComparison = Boolean.valueOf(isSetBatchTimeOut()).compareTo(other.isSetBatchTimeOut());
       if (lastComparison != 0) {
         return lastComparison;
@@ -5686,6 +5803,14 @@ import org.slf4j.LoggerFactory;
       sb.append(this.readaheadThreshold);
       first = false;
       if (!first) sb.append(", ");
      sb.append("samplerConfig:");
      if (this.samplerConfig == null) {
        sb.append("null");
      } else {
        sb.append(this.samplerConfig);
      }
      first = false;
      if (!first) sb.append(", ");
       sb.append("batchTimeOut:");
       sb.append(this.batchTimeOut);
       first = false;
@@ -5708,6 +5833,9 @@ import org.slf4j.LoggerFactory;
       if (range != null) {
         range.validate();
       }
      if (samplerConfig != null) {
        samplerConfig.validate();
      }
     }
 
     private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
@@ -5785,14 +5913,14 @@ import org.slf4j.LoggerFactory;
             case 4: // COLUMNS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list106 = iprot.readListBegin();
                  struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list106.size);
                  for (int _i107 = 0; _i107 < _list106.size; ++_i107)
                  org.apache.thrift.protocol.TList _list116 = iprot.readListBegin();
                  struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list116.size);
                  for (int _i117 = 0; _i117 < _list116.size; ++_i117)
                   {
                    org.apache.accumulo.core.data.thrift.TColumn _elem108;
                    _elem108 = new org.apache.accumulo.core.data.thrift.TColumn();
                    _elem108.read(iprot);
                    struct.columns.add(_elem108);
                    org.apache.accumulo.core.data.thrift.TColumn _elem118;
                    _elem118 = new org.apache.accumulo.core.data.thrift.TColumn();
                    _elem118.read(iprot);
                    struct.columns.add(_elem118);
                   }
                   iprot.readListEnd();
                 }
@@ -5812,14 +5940,14 @@ import org.slf4j.LoggerFactory;
             case 6: // SSI_LIST
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list109 = iprot.readListBegin();
                  struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list109.size);
                  for (int _i110 = 0; _i110 < _list109.size; ++_i110)
                  org.apache.thrift.protocol.TList _list119 = iprot.readListBegin();
                  struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list119.size);
                  for (int _i120 = 0; _i120 < _list119.size; ++_i120)
                   {
                    org.apache.accumulo.core.data.thrift.IterInfo _elem111;
                    _elem111 = new org.apache.accumulo.core.data.thrift.IterInfo();
                    _elem111.read(iprot);
                    struct.ssiList.add(_elem111);
                    org.apache.accumulo.core.data.thrift.IterInfo _elem121;
                    _elem121 = new org.apache.accumulo.core.data.thrift.IterInfo();
                    _elem121.read(iprot);
                    struct.ssiList.add(_elem121);
                   }
                   iprot.readListEnd();
                 }
@@ -5831,27 +5959,27 @@ import org.slf4j.LoggerFactory;
             case 7: // SSIO
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map112 = iprot.readMapBegin();
                  struct.ssio = new HashMap<String,Map<String,String>>(2*_map112.size);
                  for (int _i113 = 0; _i113 < _map112.size; ++_i113)
                  org.apache.thrift.protocol.TMap _map122 = iprot.readMapBegin();
                  struct.ssio = new HashMap<String,Map<String,String>>(2*_map122.size);
                  for (int _i123 = 0; _i123 < _map122.size; ++_i123)
                   {
                    String _key114;
                    Map<String,String> _val115;
                    _key114 = iprot.readString();
                    String _key124;
                    Map<String,String> _val125;
                    _key124 = iprot.readString();
                     {
                      org.apache.thrift.protocol.TMap _map116 = iprot.readMapBegin();
                      _val115 = new HashMap<String,String>(2*_map116.size);
                      for (int _i117 = 0; _i117 < _map116.size; ++_i117)
                      org.apache.thrift.protocol.TMap _map126 = iprot.readMapBegin();
                      _val125 = new HashMap<String,String>(2*_map126.size);
                      for (int _i127 = 0; _i127 < _map126.size; ++_i127)
                       {
                        String _key118;
                        String _val119;
                        _key118 = iprot.readString();
                        _val119 = iprot.readString();
                        _val115.put(_key118, _val119);
                        String _key128;
                        String _val129;
                        _key128 = iprot.readString();
                        _val129 = iprot.readString();
                        _val125.put(_key128, _val129);
                       }
                       iprot.readMapEnd();
                     }
                    struct.ssio.put(_key114, _val115);
                    struct.ssio.put(_key124, _val125);
                   }
                   iprot.readMapEnd();
                 }
@@ -5863,13 +5991,13 @@ import org.slf4j.LoggerFactory;
             case 8: // AUTHORIZATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list120 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list120.size);
                  for (int _i121 = 0; _i121 < _list120.size; ++_i121)
                  org.apache.thrift.protocol.TList _list130 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list130.size);
                  for (int _i131 = 0; _i131 < _list130.size; ++_i131)
                   {
                    ByteBuffer _elem122;
                    _elem122 = iprot.readBinary();
                    struct.authorizations.add(_elem122);
                    ByteBuffer _elem132;
                    _elem132 = iprot.readBinary();
                    struct.authorizations.add(_elem132);
                   }
                   iprot.readListEnd();
                 }
@@ -5902,7 +6030,16 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 13: // BATCH_TIME_OUT
            case 13: // SAMPLER_CONFIG
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.samplerConfig = new TSamplerConfiguration();
                struct.samplerConfig.read(iprot);
                struct.setSamplerConfigIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 14: // BATCH_TIME_OUT
               if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
                 struct.batchTimeOut = iprot.readI64();
                 struct.setBatchTimeOutIsSet(true);
@@ -5944,9 +6081,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(COLUMNS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.columns.size()));
            for (org.apache.accumulo.core.data.thrift.TColumn _iter123 : struct.columns)
            for (org.apache.accumulo.core.data.thrift.TColumn _iter133 : struct.columns)
             {
              _iter123.write(oprot);
              _iter133.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -5959,9 +6096,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SSI_LIST_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.ssiList.size()));
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter124 : struct.ssiList)
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter134 : struct.ssiList)
             {
              _iter124.write(oprot);
              _iter134.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -5971,15 +6108,15 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SSIO_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, struct.ssio.size()));
            for (Map.Entry<String, Map<String,String>> _iter125 : struct.ssio.entrySet())
            for (Map.Entry<String, Map<String,String>> _iter135 : struct.ssio.entrySet())
             {
              oprot.writeString(_iter125.getKey());
              oprot.writeString(_iter135.getKey());
               {
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, _iter125.getValue().size()));
                for (Map.Entry<String, String> _iter126 : _iter125.getValue().entrySet())
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, _iter135.getValue().size()));
                for (Map.Entry<String, String> _iter136 : _iter135.getValue().entrySet())
                 {
                  oprot.writeString(_iter126.getKey());
                  oprot.writeString(_iter126.getValue());
                  oprot.writeString(_iter136.getKey());
                  oprot.writeString(_iter136.getValue());
                 }
                 oprot.writeMapEnd();
               }
@@ -5992,9 +6129,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(AUTHORIZATIONS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.authorizations.size()));
            for (ByteBuffer _iter127 : struct.authorizations)
            for (ByteBuffer _iter137 : struct.authorizations)
             {
              oprot.writeBinary(_iter127);
              oprot.writeBinary(_iter137);
             }
             oprot.writeListEnd();
           }
@@ -6014,6 +6151,11 @@ import org.slf4j.LoggerFactory;
         oprot.writeFieldBegin(READAHEAD_THRESHOLD_FIELD_DESC);
         oprot.writeI64(struct.readaheadThreshold);
         oprot.writeFieldEnd();
        if (struct.samplerConfig != null) {
          oprot.writeFieldBegin(SAMPLER_CONFIG_FIELD_DESC);
          struct.samplerConfig.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldBegin(BATCH_TIME_OUT_FIELD_DESC);
         oprot.writeI64(struct.batchTimeOut);
         oprot.writeFieldEnd();
@@ -6071,10 +6213,13 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetReadaheadThreshold()) {
           optionals.set(11);
         }
        if (struct.isSetBatchTimeOut()) {
        if (struct.isSetSamplerConfig()) {
           optionals.set(12);
         }
        oprot.writeBitSet(optionals, 13);
        if (struct.isSetBatchTimeOut()) {
          optionals.set(13);
        }
        oprot.writeBitSet(optionals, 14);
         if (struct.isSetTinfo()) {
           struct.tinfo.write(oprot);
         }
@@ -6090,9 +6235,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetColumns()) {
           {
             oprot.writeI32(struct.columns.size());
            for (org.apache.accumulo.core.data.thrift.TColumn _iter128 : struct.columns)
            for (org.apache.accumulo.core.data.thrift.TColumn _iter138 : struct.columns)
             {
              _iter128.write(oprot);
              _iter138.write(oprot);
             }
           }
         }
@@ -6102,24 +6247,24 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSsiList()) {
           {
             oprot.writeI32(struct.ssiList.size());
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter129 : struct.ssiList)
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter139 : struct.ssiList)
             {
              _iter129.write(oprot);
              _iter139.write(oprot);
             }
           }
         }
         if (struct.isSetSsio()) {
           {
             oprot.writeI32(struct.ssio.size());
            for (Map.Entry<String, Map<String,String>> _iter130 : struct.ssio.entrySet())
            for (Map.Entry<String, Map<String,String>> _iter140 : struct.ssio.entrySet())
             {
              oprot.writeString(_iter130.getKey());
              oprot.writeString(_iter140.getKey());
               {
                oprot.writeI32(_iter130.getValue().size());
                for (Map.Entry<String, String> _iter131 : _iter130.getValue().entrySet())
                oprot.writeI32(_iter140.getValue().size());
                for (Map.Entry<String, String> _iter141 : _iter140.getValue().entrySet())
                 {
                  oprot.writeString(_iter131.getKey());
                  oprot.writeString(_iter131.getValue());
                  oprot.writeString(_iter141.getKey());
                  oprot.writeString(_iter141.getValue());
                 }
               }
             }
@@ -6128,9 +6273,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetAuthorizations()) {
           {
             oprot.writeI32(struct.authorizations.size());
            for (ByteBuffer _iter132 : struct.authorizations)
            for (ByteBuffer _iter142 : struct.authorizations)
             {
              oprot.writeBinary(_iter132);
              oprot.writeBinary(_iter142);
             }
           }
         }
@@ -6143,6 +6288,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetReadaheadThreshold()) {
           oprot.writeI64(struct.readaheadThreshold);
         }
        if (struct.isSetSamplerConfig()) {
          struct.samplerConfig.write(oprot);
        }
         if (struct.isSetBatchTimeOut()) {
           oprot.writeI64(struct.batchTimeOut);
         }
@@ -6151,7 +6299,7 @@ import org.slf4j.LoggerFactory;
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, startScan_args struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(13);
        BitSet incoming = iprot.readBitSet(14);
         if (incoming.get(0)) {
           struct.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo();
           struct.tinfo.read(iprot);
@@ -6174,14 +6322,14 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(4)) {
           {
            org.apache.thrift.protocol.TList _list133 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list133.size);
            for (int _i134 = 0; _i134 < _list133.size; ++_i134)
            org.apache.thrift.protocol.TList _list143 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list143.size);
            for (int _i144 = 0; _i144 < _list143.size; ++_i144)
             {
              org.apache.accumulo.core.data.thrift.TColumn _elem135;
              _elem135 = new org.apache.accumulo.core.data.thrift.TColumn();
              _elem135.read(iprot);
              struct.columns.add(_elem135);
              org.apache.accumulo.core.data.thrift.TColumn _elem145;
              _elem145 = new org.apache.accumulo.core.data.thrift.TColumn();
              _elem145.read(iprot);
              struct.columns.add(_elem145);
             }
           }
           struct.setColumnsIsSet(true);
@@ -6192,53 +6340,53 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(6)) {
           {
            org.apache.thrift.protocol.TList _list136 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list136.size);
            for (int _i137 = 0; _i137 < _list136.size; ++_i137)
            org.apache.thrift.protocol.TList _list146 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list146.size);
            for (int _i147 = 0; _i147 < _list146.size; ++_i147)
             {
              org.apache.accumulo.core.data.thrift.IterInfo _elem138;
              _elem138 = new org.apache.accumulo.core.data.thrift.IterInfo();
              _elem138.read(iprot);
              struct.ssiList.add(_elem138);
              org.apache.accumulo.core.data.thrift.IterInfo _elem148;
              _elem148 = new org.apache.accumulo.core.data.thrift.IterInfo();
              _elem148.read(iprot);
              struct.ssiList.add(_elem148);
             }
           }
           struct.setSsiListIsSet(true);
         }
         if (incoming.get(7)) {
           {
            org.apache.thrift.protocol.TMap _map139 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.ssio = new HashMap<String,Map<String,String>>(2*_map139.size);
            for (int _i140 = 0; _i140 < _map139.size; ++_i140)
            org.apache.thrift.protocol.TMap _map149 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.ssio = new HashMap<String,Map<String,String>>(2*_map149.size);
            for (int _i150 = 0; _i150 < _map149.size; ++_i150)
             {
              String _key141;
              Map<String,String> _val142;
              _key141 = iprot.readString();
              String _key151;
              Map<String,String> _val152;
              _key151 = iprot.readString();
               {
                org.apache.thrift.protocol.TMap _map143 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val142 = new HashMap<String,String>(2*_map143.size);
                for (int _i144 = 0; _i144 < _map143.size; ++_i144)
                org.apache.thrift.protocol.TMap _map153 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val152 = new HashMap<String,String>(2*_map153.size);
                for (int _i154 = 0; _i154 < _map153.size; ++_i154)
                 {
                  String _key145;
                  String _val146;
                  _key145 = iprot.readString();
                  _val146 = iprot.readString();
                  _val142.put(_key145, _val146);
                  String _key155;
                  String _val156;
                  _key155 = iprot.readString();
                  _val156 = iprot.readString();
                  _val152.put(_key155, _val156);
                 }
               }
              struct.ssio.put(_key141, _val142);
              struct.ssio.put(_key151, _val152);
             }
           }
           struct.setSsioIsSet(true);
         }
         if (incoming.get(8)) {
           {
            org.apache.thrift.protocol.TList _list147 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list147.size);
            for (int _i148 = 0; _i148 < _list147.size; ++_i148)
            org.apache.thrift.protocol.TList _list157 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list157.size);
            for (int _i158 = 0; _i158 < _list157.size; ++_i158)
             {
              ByteBuffer _elem149;
              _elem149 = iprot.readBinary();
              struct.authorizations.add(_elem149);
              ByteBuffer _elem159;
              _elem159 = iprot.readBinary();
              struct.authorizations.add(_elem159);
             }
           }
           struct.setAuthorizationsIsSet(true);
@@ -6256,6 +6404,11 @@ import org.slf4j.LoggerFactory;
           struct.setReadaheadThresholdIsSet(true);
         }
         if (incoming.get(12)) {
          struct.samplerConfig = new TSamplerConfiguration();
          struct.samplerConfig.read(iprot);
          struct.setSamplerConfigIsSet(true);
        }
        if (incoming.get(13)) {
           struct.batchTimeOut = iprot.readI64();
           struct.setBatchTimeOutIsSet(true);
         }
@@ -6271,6 +6424,7 @@ import org.slf4j.LoggerFactory;
     private static final org.apache.thrift.protocol.TField SEC_FIELD_DESC = new org.apache.thrift.protocol.TField("sec", org.apache.thrift.protocol.TType.STRUCT, (short)1);
     private static final org.apache.thrift.protocol.TField NSTE_FIELD_DESC = new org.apache.thrift.protocol.TField("nste", org.apache.thrift.protocol.TType.STRUCT, (short)2);
     private static final org.apache.thrift.protocol.TField TMFE_FIELD_DESC = new org.apache.thrift.protocol.TField("tmfe", org.apache.thrift.protocol.TType.STRUCT, (short)3);
    private static final org.apache.thrift.protocol.TField TSNPE_FIELD_DESC = new org.apache.thrift.protocol.TField("tsnpe", org.apache.thrift.protocol.TType.STRUCT, (short)4);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -6282,13 +6436,15 @@ import org.slf4j.LoggerFactory;
     public org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec; // required
     public NotServingTabletException nste; // required
     public TooManyFilesException tmfe; // required
    public TSampleNotPresentException tsnpe; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     public enum _Fields implements org.apache.thrift.TFieldIdEnum {
       SUCCESS((short)0, "success"),
       SEC((short)1, "sec"),
       NSTE((short)2, "nste"),
      TMFE((short)3, "tmfe");
      TMFE((short)3, "tmfe"),
      TSNPE((short)4, "tsnpe");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -6311,6 +6467,8 @@ import org.slf4j.LoggerFactory;
             return NSTE;
           case 3: // TMFE
             return TMFE;
          case 4: // TSNPE
            return TSNPE;
           default:
             return null;
         }
@@ -6362,6 +6520,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       tmpMap.put(_Fields.TMFE, new org.apache.thrift.meta_data.FieldMetaData("tmfe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      tmpMap.put(_Fields.TSNPE, new org.apache.thrift.meta_data.FieldMetaData("tsnpe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(startScan_result.class, metaDataMap);
     }
@@ -6373,13 +6533,15 @@ import org.slf4j.LoggerFactory;
       org.apache.accumulo.core.data.thrift.InitialScan success,
       org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec,
       NotServingTabletException nste,
      TooManyFilesException tmfe)
      TooManyFilesException tmfe,
      TSampleNotPresentException tsnpe)
     {
       this();
       this.success = success;
       this.sec = sec;
       this.nste = nste;
       this.tmfe = tmfe;
      this.tsnpe = tsnpe;
     }
 
     /**
@@ -6398,6 +6560,9 @@ import org.slf4j.LoggerFactory;
       if (other.isSetTmfe()) {
         this.tmfe = new TooManyFilesException(other.tmfe);
       }
      if (other.isSetTsnpe()) {
        this.tsnpe = new TSampleNotPresentException(other.tsnpe);
      }
     }
 
     public startScan_result deepCopy() {
@@ -6410,6 +6575,7 @@ import org.slf4j.LoggerFactory;
       this.sec = null;
       this.nste = null;
       this.tmfe = null;
      this.tsnpe = null;
     }
 
     public org.apache.accumulo.core.data.thrift.InitialScan getSuccess() {
@@ -6508,6 +6674,30 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public TSampleNotPresentException getTsnpe() {
      return this.tsnpe;
    }

    public startScan_result setTsnpe(TSampleNotPresentException tsnpe) {
      this.tsnpe = tsnpe;
      return this;
    }

    public void unsetTsnpe() {
      this.tsnpe = null;
    }

    /** Returns true if field tsnpe is set (has been assigned a value) and false otherwise */
    public boolean isSetTsnpe() {
      return this.tsnpe != null;
    }

    public void setTsnpeIsSet(boolean value) {
      if (!value) {
        this.tsnpe = null;
      }
    }

     public void setFieldValue(_Fields field, Object value) {
       switch (field) {
       case SUCCESS:
@@ -6542,6 +6732,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case TSNPE:
        if (value == null) {
          unsetTsnpe();
        } else {
          setTsnpe((TSampleNotPresentException)value);
        }
        break;

       }
     }
 
@@ -6559,6 +6757,9 @@ import org.slf4j.LoggerFactory;
       case TMFE:
         return getTmfe();
 
      case TSNPE:
        return getTsnpe();

       }
       throw new IllegalStateException();
     }
@@ -6578,6 +6779,8 @@ import org.slf4j.LoggerFactory;
         return isSetNste();
       case TMFE:
         return isSetTmfe();
      case TSNPE:
        return isSetTsnpe();
       }
       throw new IllegalStateException();
     }
@@ -6631,6 +6834,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_tsnpe = true && this.isSetTsnpe();
      boolean that_present_tsnpe = true && that.isSetTsnpe();
      if (this_present_tsnpe || that_present_tsnpe) {
        if (!(this_present_tsnpe && that_present_tsnpe))
          return false;
        if (!this.tsnpe.equals(that.tsnpe))
          return false;
      }

       return true;
     }
 
@@ -6687,6 +6899,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetTsnpe()).compareTo(other.isSetTsnpe());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetTsnpe()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tsnpe, other.tsnpe);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       return 0;
     }
 
@@ -6738,6 +6960,14 @@ import org.slf4j.LoggerFactory;
         sb.append(this.tmfe);
       }
       first = false;
      if (!first) sb.append(", ");
      sb.append("tsnpe:");
      if (this.tsnpe == null) {
        sb.append("null");
      } else {
        sb.append(this.tsnpe);
      }
      first = false;
       sb.append(")");
       return sb.toString();
     }
@@ -6820,6 +7050,15 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 4: // TSNPE
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.tsnpe = new TSampleNotPresentException();
                struct.tsnpe.read(iprot);
                struct.setTsnpeIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
             default:
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
           }
@@ -6855,6 +7094,11 @@ import org.slf4j.LoggerFactory;
           struct.tmfe.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.tsnpe != null) {
          oprot.writeFieldBegin(TSNPE_FIELD_DESC);
          struct.tsnpe.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -6885,7 +7129,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetTmfe()) {
           optionals.set(3);
         }
        oprot.writeBitSet(optionals, 4);
        if (struct.isSetTsnpe()) {
          optionals.set(4);
        }
        oprot.writeBitSet(optionals, 5);
         if (struct.isSetSuccess()) {
           struct.success.write(oprot);
         }
@@ -6898,12 +7145,15 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetTmfe()) {
           struct.tmfe.write(oprot);
         }
        if (struct.isSetTsnpe()) {
          struct.tsnpe.write(oprot);
        }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, startScan_result struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(4);
        BitSet incoming = iprot.readBitSet(5);
         if (incoming.get(0)) {
           struct.success = new org.apache.accumulo.core.data.thrift.InitialScan();
           struct.success.read(iprot);
@@ -6924,6 +7174,11 @@ import org.slf4j.LoggerFactory;
           struct.tmfe.read(iprot);
           struct.setTmfeIsSet(true);
         }
        if (incoming.get(4)) {
          struct.tsnpe = new TSampleNotPresentException();
          struct.tsnpe.read(iprot);
          struct.setTsnpeIsSet(true);
        }
       }
     }
 
@@ -7393,6 +7648,7 @@ import org.slf4j.LoggerFactory;
     private static final org.apache.thrift.protocol.TField NSSI_FIELD_DESC = new org.apache.thrift.protocol.TField("nssi", org.apache.thrift.protocol.TType.STRUCT, (short)1);
     private static final org.apache.thrift.protocol.TField NSTE_FIELD_DESC = new org.apache.thrift.protocol.TField("nste", org.apache.thrift.protocol.TType.STRUCT, (short)2);
     private static final org.apache.thrift.protocol.TField TMFE_FIELD_DESC = new org.apache.thrift.protocol.TField("tmfe", org.apache.thrift.protocol.TType.STRUCT, (short)3);
    private static final org.apache.thrift.protocol.TField TSNPE_FIELD_DESC = new org.apache.thrift.protocol.TField("tsnpe", org.apache.thrift.protocol.TType.STRUCT, (short)4);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -7404,13 +7660,15 @@ import org.slf4j.LoggerFactory;
     public NoSuchScanIDException nssi; // required
     public NotServingTabletException nste; // required
     public TooManyFilesException tmfe; // required
    public TSampleNotPresentException tsnpe; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     public enum _Fields implements org.apache.thrift.TFieldIdEnum {
       SUCCESS((short)0, "success"),
       NSSI((short)1, "nssi"),
       NSTE((short)2, "nste"),
      TMFE((short)3, "tmfe");
      TMFE((short)3, "tmfe"),
      TSNPE((short)4, "tsnpe");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -7433,6 +7691,8 @@ import org.slf4j.LoggerFactory;
             return NSTE;
           case 3: // TMFE
             return TMFE;
          case 4: // TSNPE
            return TSNPE;
           default:
             return null;
         }
@@ -7484,6 +7744,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       tmpMap.put(_Fields.TMFE, new org.apache.thrift.meta_data.FieldMetaData("tmfe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      tmpMap.put(_Fields.TSNPE, new org.apache.thrift.meta_data.FieldMetaData("tsnpe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(continueScan_result.class, metaDataMap);
     }
@@ -7495,13 +7757,15 @@ import org.slf4j.LoggerFactory;
       org.apache.accumulo.core.data.thrift.ScanResult success,
       NoSuchScanIDException nssi,
       NotServingTabletException nste,
      TooManyFilesException tmfe)
      TooManyFilesException tmfe,
      TSampleNotPresentException tsnpe)
     {
       this();
       this.success = success;
       this.nssi = nssi;
       this.nste = nste;
       this.tmfe = tmfe;
      this.tsnpe = tsnpe;
     }
 
     /**
@@ -7520,6 +7784,9 @@ import org.slf4j.LoggerFactory;
       if (other.isSetTmfe()) {
         this.tmfe = new TooManyFilesException(other.tmfe);
       }
      if (other.isSetTsnpe()) {
        this.tsnpe = new TSampleNotPresentException(other.tsnpe);
      }
     }
 
     public continueScan_result deepCopy() {
@@ -7532,6 +7799,7 @@ import org.slf4j.LoggerFactory;
       this.nssi = null;
       this.nste = null;
       this.tmfe = null;
      this.tsnpe = null;
     }
 
     public org.apache.accumulo.core.data.thrift.ScanResult getSuccess() {
@@ -7630,6 +7898,30 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public TSampleNotPresentException getTsnpe() {
      return this.tsnpe;
    }

    public continueScan_result setTsnpe(TSampleNotPresentException tsnpe) {
      this.tsnpe = tsnpe;
      return this;
    }

    public void unsetTsnpe() {
      this.tsnpe = null;
    }

    /** Returns true if field tsnpe is set (has been assigned a value) and false otherwise */
    public boolean isSetTsnpe() {
      return this.tsnpe != null;
    }

    public void setTsnpeIsSet(boolean value) {
      if (!value) {
        this.tsnpe = null;
      }
    }

     public void setFieldValue(_Fields field, Object value) {
       switch (field) {
       case SUCCESS:
@@ -7664,6 +7956,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case TSNPE:
        if (value == null) {
          unsetTsnpe();
        } else {
          setTsnpe((TSampleNotPresentException)value);
        }
        break;

       }
     }
 
@@ -7681,6 +7981,9 @@ import org.slf4j.LoggerFactory;
       case TMFE:
         return getTmfe();
 
      case TSNPE:
        return getTsnpe();

       }
       throw new IllegalStateException();
     }
@@ -7700,6 +8003,8 @@ import org.slf4j.LoggerFactory;
         return isSetNste();
       case TMFE:
         return isSetTmfe();
      case TSNPE:
        return isSetTsnpe();
       }
       throw new IllegalStateException();
     }
@@ -7753,6 +8058,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_tsnpe = true && this.isSetTsnpe();
      boolean that_present_tsnpe = true && that.isSetTsnpe();
      if (this_present_tsnpe || that_present_tsnpe) {
        if (!(this_present_tsnpe && that_present_tsnpe))
          return false;
        if (!this.tsnpe.equals(that.tsnpe))
          return false;
      }

       return true;
     }
 
@@ -7809,6 +8123,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetTsnpe()).compareTo(other.isSetTsnpe());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetTsnpe()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tsnpe, other.tsnpe);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       return 0;
     }
 
@@ -7860,6 +8184,14 @@ import org.slf4j.LoggerFactory;
         sb.append(this.tmfe);
       }
       first = false;
      if (!first) sb.append(", ");
      sb.append("tsnpe:");
      if (this.tsnpe == null) {
        sb.append("null");
      } else {
        sb.append(this.tsnpe);
      }
      first = false;
       sb.append(")");
       return sb.toString();
     }
@@ -7942,6 +8274,15 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 4: // TSNPE
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.tsnpe = new TSampleNotPresentException();
                struct.tsnpe.read(iprot);
                struct.setTsnpeIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
             default:
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
           }
@@ -7977,6 +8318,11 @@ import org.slf4j.LoggerFactory;
           struct.tmfe.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.tsnpe != null) {
          oprot.writeFieldBegin(TSNPE_FIELD_DESC);
          struct.tsnpe.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -8007,7 +8353,10 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetTmfe()) {
           optionals.set(3);
         }
        oprot.writeBitSet(optionals, 4);
        if (struct.isSetTsnpe()) {
          optionals.set(4);
        }
        oprot.writeBitSet(optionals, 5);
         if (struct.isSetSuccess()) {
           struct.success.write(oprot);
         }
@@ -8020,12 +8369,15 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetTmfe()) {
           struct.tmfe.write(oprot);
         }
        if (struct.isSetTsnpe()) {
          struct.tsnpe.write(oprot);
        }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, continueScan_result struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(4);
        BitSet incoming = iprot.readBitSet(5);
         if (incoming.get(0)) {
           struct.success = new org.apache.accumulo.core.data.thrift.ScanResult();
           struct.success.read(iprot);
@@ -8046,6 +8398,11 @@ import org.slf4j.LoggerFactory;
           struct.tmfe.read(iprot);
           struct.setTmfeIsSet(true);
         }
        if (incoming.get(4)) {
          struct.tsnpe = new TSampleNotPresentException();
          struct.tsnpe.read(iprot);
          struct.setTsnpeIsSet(true);
        }
       }
     }
 
@@ -8519,7 +8876,8 @@ import org.slf4j.LoggerFactory;
     private static final org.apache.thrift.protocol.TField SSIO_FIELD_DESC = new org.apache.thrift.protocol.TField("ssio", org.apache.thrift.protocol.TType.MAP, (short)5);
     private static final org.apache.thrift.protocol.TField AUTHORIZATIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("authorizations", org.apache.thrift.protocol.TType.LIST, (short)6);
     private static final org.apache.thrift.protocol.TField WAIT_FOR_WRITES_FIELD_DESC = new org.apache.thrift.protocol.TField("waitForWrites", org.apache.thrift.protocol.TType.BOOL, (short)7);
    private static final org.apache.thrift.protocol.TField BATCH_TIME_OUT_FIELD_DESC = new org.apache.thrift.protocol.TField("batchTimeOut", org.apache.thrift.protocol.TType.I64, (short)9);
    private static final org.apache.thrift.protocol.TField SAMPLER_CONFIG_FIELD_DESC = new org.apache.thrift.protocol.TField("samplerConfig", org.apache.thrift.protocol.TType.STRUCT, (short)9);
    private static final org.apache.thrift.protocol.TField BATCH_TIME_OUT_FIELD_DESC = new org.apache.thrift.protocol.TField("batchTimeOut", org.apache.thrift.protocol.TType.I64, (short)10);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -8535,6 +8893,7 @@ import org.slf4j.LoggerFactory;
     public Map<String,Map<String,String>> ssio; // required
     public List<ByteBuffer> authorizations; // required
     public boolean waitForWrites; // required
    public TSamplerConfiguration samplerConfig; // required
     public long batchTimeOut; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
@@ -8547,7 +8906,8 @@ import org.slf4j.LoggerFactory;
       SSIO((short)5, "ssio"),
       AUTHORIZATIONS((short)6, "authorizations"),
       WAIT_FOR_WRITES((short)7, "waitForWrites"),
      BATCH_TIME_OUT((short)9, "batchTimeOut");
      SAMPLER_CONFIG((short)9, "samplerConfig"),
      BATCH_TIME_OUT((short)10, "batchTimeOut");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -8578,7 +8938,9 @@ import org.slf4j.LoggerFactory;
             return AUTHORIZATIONS;
           case 7: // WAIT_FOR_WRITES
             return WAIT_FOR_WRITES;
          case 9: // BATCH_TIME_OUT
          case 9: // SAMPLER_CONFIG
            return SAMPLER_CONFIG;
          case 10: // BATCH_TIME_OUT
             return BATCH_TIME_OUT;
           default:
             return null;
@@ -8649,6 +9011,8 @@ import org.slf4j.LoggerFactory;
               new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING              , true))));
       tmpMap.put(_Fields.WAIT_FOR_WRITES, new org.apache.thrift.meta_data.FieldMetaData("waitForWrites", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
      tmpMap.put(_Fields.SAMPLER_CONFIG, new org.apache.thrift.meta_data.FieldMetaData("samplerConfig", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TSamplerConfiguration.class)));
       tmpMap.put(_Fields.BATCH_TIME_OUT, new org.apache.thrift.meta_data.FieldMetaData("batchTimeOut", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
@@ -8667,6 +9031,7 @@ import org.slf4j.LoggerFactory;
       Map<String,Map<String,String>> ssio,
       List<ByteBuffer> authorizations,
       boolean waitForWrites,
      TSamplerConfiguration samplerConfig,
       long batchTimeOut)
     {
       this();
@@ -8679,6 +9044,7 @@ import org.slf4j.LoggerFactory;
       this.authorizations = authorizations;
       this.waitForWrites = waitForWrites;
       setWaitForWritesIsSet(true);
      this.samplerConfig = samplerConfig;
       this.batchTimeOut = batchTimeOut;
       setBatchTimeOutIsSet(true);
     }
@@ -8731,6 +9097,9 @@ import org.slf4j.LoggerFactory;
         this.authorizations = __this__authorizations;
       }
       this.waitForWrites = other.waitForWrites;
      if (other.isSetSamplerConfig()) {
        this.samplerConfig = new TSamplerConfiguration(other.samplerConfig);
      }
       this.batchTimeOut = other.batchTimeOut;
     }
 
@@ -8749,6 +9118,7 @@ import org.slf4j.LoggerFactory;
       this.authorizations = null;
       setWaitForWritesIsSet(false);
       this.waitForWrites = false;
      this.samplerConfig = null;
       setBatchTimeOutIsSet(false);
       this.batchTimeOut = 0;
     }
@@ -9011,6 +9381,30 @@ import org.slf4j.LoggerFactory;
       __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __WAITFORWRITES_ISSET_ID, value);
     }
 
    public TSamplerConfiguration getSamplerConfig() {
      return this.samplerConfig;
    }

    public startMultiScan_args setSamplerConfig(TSamplerConfiguration samplerConfig) {
      this.samplerConfig = samplerConfig;
      return this;
    }

    public void unsetSamplerConfig() {
      this.samplerConfig = null;
    }

    /** Returns true if field samplerConfig is set (has been assigned a value) and false otherwise */
    public boolean isSetSamplerConfig() {
      return this.samplerConfig != null;
    }

    public void setSamplerConfigIsSet(boolean value) {
      if (!value) {
        this.samplerConfig = null;
      }
    }

     public long getBatchTimeOut() {
       return this.batchTimeOut;
     }
@@ -9100,6 +9494,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case SAMPLER_CONFIG:
        if (value == null) {
          unsetSamplerConfig();
        } else {
          setSamplerConfig((TSamplerConfiguration)value);
        }
        break;

       case BATCH_TIME_OUT:
         if (value == null) {
           unsetBatchTimeOut();
@@ -9137,6 +9539,9 @@ import org.slf4j.LoggerFactory;
       case WAIT_FOR_WRITES:
         return Boolean.valueOf(isWaitForWrites());
 
      case SAMPLER_CONFIG:
        return getSamplerConfig();

       case BATCH_TIME_OUT:
         return Long.valueOf(getBatchTimeOut());
 
@@ -9167,6 +9572,8 @@ import org.slf4j.LoggerFactory;
         return isSetAuthorizations();
       case WAIT_FOR_WRITES:
         return isSetWaitForWrites();
      case SAMPLER_CONFIG:
        return isSetSamplerConfig();
       case BATCH_TIME_OUT:
         return isSetBatchTimeOut();
       }
@@ -9258,6 +9665,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_samplerConfig = true && this.isSetSamplerConfig();
      boolean that_present_samplerConfig = true && that.isSetSamplerConfig();
      if (this_present_samplerConfig || that_present_samplerConfig) {
        if (!(this_present_samplerConfig && that_present_samplerConfig))
          return false;
        if (!this.samplerConfig.equals(that.samplerConfig))
          return false;
      }

       boolean this_present_batchTimeOut = true;
       boolean that_present_batchTimeOut = true;
       if (this_present_batchTimeOut || that_present_batchTimeOut) {
@@ -9363,6 +9779,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetSamplerConfig()).compareTo(other.isSetSamplerConfig());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSamplerConfig()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.samplerConfig, other.samplerConfig);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       lastComparison = Boolean.valueOf(isSetBatchTimeOut()).compareTo(other.isSetBatchTimeOut());
       if (lastComparison != 0) {
         return lastComparison;
@@ -9453,6 +9879,14 @@ import org.slf4j.LoggerFactory;
       sb.append(this.waitForWrites);
       first = false;
       if (!first) sb.append(", ");
      sb.append("samplerConfig:");
      if (this.samplerConfig == null) {
        sb.append("null");
      } else {
        sb.append(this.samplerConfig);
      }
      first = false;
      if (!first) sb.append(", ");
       sb.append("batchTimeOut:");
       sb.append(this.batchTimeOut);
       first = false;
@@ -9469,6 +9903,9 @@ import org.slf4j.LoggerFactory;
       if (credentials != null) {
         credentials.validate();
       }
      if (samplerConfig != null) {
        samplerConfig.validate();
      }
     }
 
     private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
@@ -9528,27 +9965,27 @@ import org.slf4j.LoggerFactory;
             case 2: // BATCH
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map150 = iprot.readMapBegin();
                  struct.batch = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>>(2*_map150.size);
                  for (int _i151 = 0; _i151 < _map150.size; ++_i151)
                  org.apache.thrift.protocol.TMap _map160 = iprot.readMapBegin();
                  struct.batch = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>>(2*_map160.size);
                  for (int _i161 = 0; _i161 < _map160.size; ++_i161)
                   {
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key152;
                    List<org.apache.accumulo.core.data.thrift.TRange> _val153;
                    _key152 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key152.read(iprot);
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key162;
                    List<org.apache.accumulo.core.data.thrift.TRange> _val163;
                    _key162 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key162.read(iprot);
                     {
                      org.apache.thrift.protocol.TList _list154 = iprot.readListBegin();
                      _val153 = new ArrayList<org.apache.accumulo.core.data.thrift.TRange>(_list154.size);
                      for (int _i155 = 0; _i155 < _list154.size; ++_i155)
                      org.apache.thrift.protocol.TList _list164 = iprot.readListBegin();
                      _val163 = new ArrayList<org.apache.accumulo.core.data.thrift.TRange>(_list164.size);
                      for (int _i165 = 0; _i165 < _list164.size; ++_i165)
                       {
                        org.apache.accumulo.core.data.thrift.TRange _elem156;
                        _elem156 = new org.apache.accumulo.core.data.thrift.TRange();
                        _elem156.read(iprot);
                        _val153.add(_elem156);
                        org.apache.accumulo.core.data.thrift.TRange _elem166;
                        _elem166 = new org.apache.accumulo.core.data.thrift.TRange();
                        _elem166.read(iprot);
                        _val163.add(_elem166);
                       }
                       iprot.readListEnd();
                     }
                    struct.batch.put(_key152, _val153);
                    struct.batch.put(_key162, _val163);
                   }
                   iprot.readMapEnd();
                 }
@@ -9560,14 +9997,14 @@ import org.slf4j.LoggerFactory;
             case 3: // COLUMNS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list157 = iprot.readListBegin();
                  struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list157.size);
                  for (int _i158 = 0; _i158 < _list157.size; ++_i158)
                  org.apache.thrift.protocol.TList _list167 = iprot.readListBegin();
                  struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list167.size);
                  for (int _i168 = 0; _i168 < _list167.size; ++_i168)
                   {
                    org.apache.accumulo.core.data.thrift.TColumn _elem159;
                    _elem159 = new org.apache.accumulo.core.data.thrift.TColumn();
                    _elem159.read(iprot);
                    struct.columns.add(_elem159);
                    org.apache.accumulo.core.data.thrift.TColumn _elem169;
                    _elem169 = new org.apache.accumulo.core.data.thrift.TColumn();
                    _elem169.read(iprot);
                    struct.columns.add(_elem169);
                   }
                   iprot.readListEnd();
                 }
@@ -9579,14 +10016,14 @@ import org.slf4j.LoggerFactory;
             case 4: // SSI_LIST
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list160 = iprot.readListBegin();
                  struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list160.size);
                  for (int _i161 = 0; _i161 < _list160.size; ++_i161)
                  org.apache.thrift.protocol.TList _list170 = iprot.readListBegin();
                  struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list170.size);
                  for (int _i171 = 0; _i171 < _list170.size; ++_i171)
                   {
                    org.apache.accumulo.core.data.thrift.IterInfo _elem162;
                    _elem162 = new org.apache.accumulo.core.data.thrift.IterInfo();
                    _elem162.read(iprot);
                    struct.ssiList.add(_elem162);
                    org.apache.accumulo.core.data.thrift.IterInfo _elem172;
                    _elem172 = new org.apache.accumulo.core.data.thrift.IterInfo();
                    _elem172.read(iprot);
                    struct.ssiList.add(_elem172);
                   }
                   iprot.readListEnd();
                 }
@@ -9598,27 +10035,27 @@ import org.slf4j.LoggerFactory;
             case 5: // SSIO
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map163 = iprot.readMapBegin();
                  struct.ssio = new HashMap<String,Map<String,String>>(2*_map163.size);
                  for (int _i164 = 0; _i164 < _map163.size; ++_i164)
                  org.apache.thrift.protocol.TMap _map173 = iprot.readMapBegin();
                  struct.ssio = new HashMap<String,Map<String,String>>(2*_map173.size);
                  for (int _i174 = 0; _i174 < _map173.size; ++_i174)
                   {
                    String _key165;
                    Map<String,String> _val166;
                    _key165 = iprot.readString();
                    String _key175;
                    Map<String,String> _val176;
                    _key175 = iprot.readString();
                     {
                      org.apache.thrift.protocol.TMap _map167 = iprot.readMapBegin();
                      _val166 = new HashMap<String,String>(2*_map167.size);
                      for (int _i168 = 0; _i168 < _map167.size; ++_i168)
                      org.apache.thrift.protocol.TMap _map177 = iprot.readMapBegin();
                      _val176 = new HashMap<String,String>(2*_map177.size);
                      for (int _i178 = 0; _i178 < _map177.size; ++_i178)
                       {
                        String _key169;
                        String _val170;
                        _key169 = iprot.readString();
                        _val170 = iprot.readString();
                        _val166.put(_key169, _val170);
                        String _key179;
                        String _val180;
                        _key179 = iprot.readString();
                        _val180 = iprot.readString();
                        _val176.put(_key179, _val180);
                       }
                       iprot.readMapEnd();
                     }
                    struct.ssio.put(_key165, _val166);
                    struct.ssio.put(_key175, _val176);
                   }
                   iprot.readMapEnd();
                 }
@@ -9630,13 +10067,13 @@ import org.slf4j.LoggerFactory;
             case 6: // AUTHORIZATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list171 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list171.size);
                  for (int _i172 = 0; _i172 < _list171.size; ++_i172)
                  org.apache.thrift.protocol.TList _list181 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list181.size);
                  for (int _i182 = 0; _i182 < _list181.size; ++_i182)
                   {
                    ByteBuffer _elem173;
                    _elem173 = iprot.readBinary();
                    struct.authorizations.add(_elem173);
                    ByteBuffer _elem183;
                    _elem183 = iprot.readBinary();
                    struct.authorizations.add(_elem183);
                   }
                   iprot.readListEnd();
                 }
@@ -9653,7 +10090,16 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 9: // BATCH_TIME_OUT
            case 9: // SAMPLER_CONFIG
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.samplerConfig = new TSamplerConfiguration();
                struct.samplerConfig.read(iprot);
                struct.setSamplerConfigIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 10: // BATCH_TIME_OUT
               if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
                 struct.batchTimeOut = iprot.readI64();
                 struct.setBatchTimeOutIsSet(true);
@@ -9685,14 +10131,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(BATCH_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, struct.batch.size()));
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TRange>> _iter174 : struct.batch.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TRange>> _iter184 : struct.batch.entrySet())
             {
              _iter174.getKey().write(oprot);
              _iter184.getKey().write(oprot);
               {
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter174.getValue().size()));
                for (org.apache.accumulo.core.data.thrift.TRange _iter175 : _iter174.getValue())
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter184.getValue().size()));
                for (org.apache.accumulo.core.data.thrift.TRange _iter185 : _iter184.getValue())
                 {
                  _iter175.write(oprot);
                  _iter185.write(oprot);
                 }
                 oprot.writeListEnd();
               }
@@ -9705,9 +10151,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(COLUMNS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.columns.size()));
            for (org.apache.accumulo.core.data.thrift.TColumn _iter176 : struct.columns)
            for (org.apache.accumulo.core.data.thrift.TColumn _iter186 : struct.columns)
             {
              _iter176.write(oprot);
              _iter186.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -9717,9 +10163,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SSI_LIST_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.ssiList.size()));
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter177 : struct.ssiList)
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter187 : struct.ssiList)
             {
              _iter177.write(oprot);
              _iter187.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -9729,15 +10175,15 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SSIO_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, struct.ssio.size()));
            for (Map.Entry<String, Map<String,String>> _iter178 : struct.ssio.entrySet())
            for (Map.Entry<String, Map<String,String>> _iter188 : struct.ssio.entrySet())
             {
              oprot.writeString(_iter178.getKey());
              oprot.writeString(_iter188.getKey());
               {
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, _iter178.getValue().size()));
                for (Map.Entry<String, String> _iter179 : _iter178.getValue().entrySet())
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, _iter188.getValue().size()));
                for (Map.Entry<String, String> _iter189 : _iter188.getValue().entrySet())
                 {
                  oprot.writeString(_iter179.getKey());
                  oprot.writeString(_iter179.getValue());
                  oprot.writeString(_iter189.getKey());
                  oprot.writeString(_iter189.getValue());
                 }
                 oprot.writeMapEnd();
               }
@@ -9750,9 +10196,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(AUTHORIZATIONS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.authorizations.size()));
            for (ByteBuffer _iter180 : struct.authorizations)
            for (ByteBuffer _iter190 : struct.authorizations)
             {
              oprot.writeBinary(_iter180);
              oprot.writeBinary(_iter190);
             }
             oprot.writeListEnd();
           }
@@ -9766,6 +10212,11 @@ import org.slf4j.LoggerFactory;
           struct.tinfo.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.samplerConfig != null) {
          oprot.writeFieldBegin(SAMPLER_CONFIG_FIELD_DESC);
          struct.samplerConfig.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldBegin(BATCH_TIME_OUT_FIELD_DESC);
         oprot.writeI64(struct.batchTimeOut);
         oprot.writeFieldEnd();
@@ -9811,10 +10262,13 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetWaitForWrites()) {
           optionals.set(7);
         }
        if (struct.isSetBatchTimeOut()) {
        if (struct.isSetSamplerConfig()) {
           optionals.set(8);
         }
        oprot.writeBitSet(optionals, 9);
        if (struct.isSetBatchTimeOut()) {
          optionals.set(9);
        }
        oprot.writeBitSet(optionals, 10);
         if (struct.isSetTinfo()) {
           struct.tinfo.write(oprot);
         }
@@ -9824,14 +10278,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetBatch()) {
           {
             oprot.writeI32(struct.batch.size());
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TRange>> _iter181 : struct.batch.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TRange>> _iter191 : struct.batch.entrySet())
             {
              _iter181.getKey().write(oprot);
              _iter191.getKey().write(oprot);
               {
                oprot.writeI32(_iter181.getValue().size());
                for (org.apache.accumulo.core.data.thrift.TRange _iter182 : _iter181.getValue())
                oprot.writeI32(_iter191.getValue().size());
                for (org.apache.accumulo.core.data.thrift.TRange _iter192 : _iter191.getValue())
                 {
                  _iter182.write(oprot);
                  _iter192.write(oprot);
                 }
               }
             }
@@ -9840,33 +10294,33 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetColumns()) {
           {
             oprot.writeI32(struct.columns.size());
            for (org.apache.accumulo.core.data.thrift.TColumn _iter183 : struct.columns)
            for (org.apache.accumulo.core.data.thrift.TColumn _iter193 : struct.columns)
             {
              _iter183.write(oprot);
              _iter193.write(oprot);
             }
           }
         }
         if (struct.isSetSsiList()) {
           {
             oprot.writeI32(struct.ssiList.size());
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter184 : struct.ssiList)
            for (org.apache.accumulo.core.data.thrift.IterInfo _iter194 : struct.ssiList)
             {
              _iter184.write(oprot);
              _iter194.write(oprot);
             }
           }
         }
         if (struct.isSetSsio()) {
           {
             oprot.writeI32(struct.ssio.size());
            for (Map.Entry<String, Map<String,String>> _iter185 : struct.ssio.entrySet())
            for (Map.Entry<String, Map<String,String>> _iter195 : struct.ssio.entrySet())
             {
              oprot.writeString(_iter185.getKey());
              oprot.writeString(_iter195.getKey());
               {
                oprot.writeI32(_iter185.getValue().size());
                for (Map.Entry<String, String> _iter186 : _iter185.getValue().entrySet())
                oprot.writeI32(_iter195.getValue().size());
                for (Map.Entry<String, String> _iter196 : _iter195.getValue().entrySet())
                 {
                  oprot.writeString(_iter186.getKey());
                  oprot.writeString(_iter186.getValue());
                  oprot.writeString(_iter196.getKey());
                  oprot.writeString(_iter196.getValue());
                 }
               }
             }
@@ -9875,15 +10329,18 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetAuthorizations()) {
           {
             oprot.writeI32(struct.authorizations.size());
            for (ByteBuffer _iter187 : struct.authorizations)
            for (ByteBuffer _iter197 : struct.authorizations)
             {
              oprot.writeBinary(_iter187);
              oprot.writeBinary(_iter197);
             }
           }
         }
         if (struct.isSetWaitForWrites()) {
           oprot.writeBool(struct.waitForWrites);
         }
        if (struct.isSetSamplerConfig()) {
          struct.samplerConfig.write(oprot);
        }
         if (struct.isSetBatchTimeOut()) {
           oprot.writeI64(struct.batchTimeOut);
         }
@@ -9892,7 +10349,7 @@ import org.slf4j.LoggerFactory;
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, startMultiScan_args struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(9);
        BitSet incoming = iprot.readBitSet(10);
         if (incoming.get(0)) {
           struct.tinfo = new org.apache.accumulo.core.trace.thrift.TInfo();
           struct.tinfo.read(iprot);
@@ -9905,93 +10362,93 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TMap _map188 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.batch = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>>(2*_map188.size);
            for (int _i189 = 0; _i189 < _map188.size; ++_i189)
            org.apache.thrift.protocol.TMap _map198 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.batch = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TRange>>(2*_map198.size);
            for (int _i199 = 0; _i199 < _map198.size; ++_i199)
             {
              org.apache.accumulo.core.data.thrift.TKeyExtent _key190;
              List<org.apache.accumulo.core.data.thrift.TRange> _val191;
              _key190 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key190.read(iprot);
              org.apache.accumulo.core.data.thrift.TKeyExtent _key200;
              List<org.apache.accumulo.core.data.thrift.TRange> _val201;
              _key200 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key200.read(iprot);
               {
                org.apache.thrift.protocol.TList _list192 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val191 = new ArrayList<org.apache.accumulo.core.data.thrift.TRange>(_list192.size);
                for (int _i193 = 0; _i193 < _list192.size; ++_i193)
                org.apache.thrift.protocol.TList _list202 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val201 = new ArrayList<org.apache.accumulo.core.data.thrift.TRange>(_list202.size);
                for (int _i203 = 0; _i203 < _list202.size; ++_i203)
                 {
                  org.apache.accumulo.core.data.thrift.TRange _elem194;
                  _elem194 = new org.apache.accumulo.core.data.thrift.TRange();
                  _elem194.read(iprot);
                  _val191.add(_elem194);
                  org.apache.accumulo.core.data.thrift.TRange _elem204;
                  _elem204 = new org.apache.accumulo.core.data.thrift.TRange();
                  _elem204.read(iprot);
                  _val201.add(_elem204);
                 }
               }
              struct.batch.put(_key190, _val191);
              struct.batch.put(_key200, _val201);
             }
           }
           struct.setBatchIsSet(true);
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TList _list195 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list195.size);
            for (int _i196 = 0; _i196 < _list195.size; ++_i196)
            org.apache.thrift.protocol.TList _list205 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.columns = new ArrayList<org.apache.accumulo.core.data.thrift.TColumn>(_list205.size);
            for (int _i206 = 0; _i206 < _list205.size; ++_i206)
             {
              org.apache.accumulo.core.data.thrift.TColumn _elem197;
              _elem197 = new org.apache.accumulo.core.data.thrift.TColumn();
              _elem197.read(iprot);
              struct.columns.add(_elem197);
              org.apache.accumulo.core.data.thrift.TColumn _elem207;
              _elem207 = new org.apache.accumulo.core.data.thrift.TColumn();
              _elem207.read(iprot);
              struct.columns.add(_elem207);
             }
           }
           struct.setColumnsIsSet(true);
         }
         if (incoming.get(4)) {
           {
            org.apache.thrift.protocol.TList _list198 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list198.size);
            for (int _i199 = 0; _i199 < _list198.size; ++_i199)
            org.apache.thrift.protocol.TList _list208 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.ssiList = new ArrayList<org.apache.accumulo.core.data.thrift.IterInfo>(_list208.size);
            for (int _i209 = 0; _i209 < _list208.size; ++_i209)
             {
              org.apache.accumulo.core.data.thrift.IterInfo _elem200;
              _elem200 = new org.apache.accumulo.core.data.thrift.IterInfo();
              _elem200.read(iprot);
              struct.ssiList.add(_elem200);
              org.apache.accumulo.core.data.thrift.IterInfo _elem210;
              _elem210 = new org.apache.accumulo.core.data.thrift.IterInfo();
              _elem210.read(iprot);
              struct.ssiList.add(_elem210);
             }
           }
           struct.setSsiListIsSet(true);
         }
         if (incoming.get(5)) {
           {
            org.apache.thrift.protocol.TMap _map201 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.ssio = new HashMap<String,Map<String,String>>(2*_map201.size);
            for (int _i202 = 0; _i202 < _map201.size; ++_i202)
            org.apache.thrift.protocol.TMap _map211 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.ssio = new HashMap<String,Map<String,String>>(2*_map211.size);
            for (int _i212 = 0; _i212 < _map211.size; ++_i212)
             {
              String _key203;
              Map<String,String> _val204;
              _key203 = iprot.readString();
              String _key213;
              Map<String,String> _val214;
              _key213 = iprot.readString();
               {
                org.apache.thrift.protocol.TMap _map205 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val204 = new HashMap<String,String>(2*_map205.size);
                for (int _i206 = 0; _i206 < _map205.size; ++_i206)
                org.apache.thrift.protocol.TMap _map215 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRING, iprot.readI32());
                _val214 = new HashMap<String,String>(2*_map215.size);
                for (int _i216 = 0; _i216 < _map215.size; ++_i216)
                 {
                  String _key207;
                  String _val208;
                  _key207 = iprot.readString();
                  _val208 = iprot.readString();
                  _val204.put(_key207, _val208);
                  String _key217;
                  String _val218;
                  _key217 = iprot.readString();
                  _val218 = iprot.readString();
                  _val214.put(_key217, _val218);
                 }
               }
              struct.ssio.put(_key203, _val204);
              struct.ssio.put(_key213, _val214);
             }
           }
           struct.setSsioIsSet(true);
         }
         if (incoming.get(6)) {
           {
            org.apache.thrift.protocol.TList _list209 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list209.size);
            for (int _i210 = 0; _i210 < _list209.size; ++_i210)
            org.apache.thrift.protocol.TList _list219 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list219.size);
            for (int _i220 = 0; _i220 < _list219.size; ++_i220)
             {
              ByteBuffer _elem211;
              _elem211 = iprot.readBinary();
              struct.authorizations.add(_elem211);
              ByteBuffer _elem221;
              _elem221 = iprot.readBinary();
              struct.authorizations.add(_elem221);
             }
           }
           struct.setAuthorizationsIsSet(true);
@@ -10001,6 +10458,11 @@ import org.slf4j.LoggerFactory;
           struct.setWaitForWritesIsSet(true);
         }
         if (incoming.get(8)) {
          struct.samplerConfig = new TSamplerConfiguration();
          struct.samplerConfig.read(iprot);
          struct.setSamplerConfigIsSet(true);
        }
        if (incoming.get(9)) {
           struct.batchTimeOut = iprot.readI64();
           struct.setBatchTimeOutIsSet(true);
         }
@@ -10014,6 +10476,7 @@ import org.slf4j.LoggerFactory;
 
     private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.STRUCT, (short)0);
     private static final org.apache.thrift.protocol.TField SEC_FIELD_DESC = new org.apache.thrift.protocol.TField("sec", org.apache.thrift.protocol.TType.STRUCT, (short)1);
    private static final org.apache.thrift.protocol.TField TSNPE_FIELD_DESC = new org.apache.thrift.protocol.TField("tsnpe", org.apache.thrift.protocol.TType.STRUCT, (short)2);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -10023,11 +10486,13 @@ import org.slf4j.LoggerFactory;
 
     public org.apache.accumulo.core.data.thrift.InitialMultiScan success; // required
     public org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec; // required
    public TSampleNotPresentException tsnpe; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     public enum _Fields implements org.apache.thrift.TFieldIdEnum {
       SUCCESS((short)0, "success"),
      SEC((short)1, "sec");
      SEC((short)1, "sec"),
      TSNPE((short)2, "tsnpe");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -10046,6 +10511,8 @@ import org.slf4j.LoggerFactory;
             return SUCCESS;
           case 1: // SEC
             return SEC;
          case 2: // TSNPE
            return TSNPE;
           default:
             return null;
         }
@@ -10093,6 +10560,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.data.thrift.InitialMultiScan.class)));
       tmpMap.put(_Fields.SEC, new org.apache.thrift.meta_data.FieldMetaData("sec", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      tmpMap.put(_Fields.TSNPE, new org.apache.thrift.meta_data.FieldMetaData("tsnpe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(startMultiScan_result.class, metaDataMap);
     }
@@ -10102,11 +10571,13 @@ import org.slf4j.LoggerFactory;
 
     public startMultiScan_result(
       org.apache.accumulo.core.data.thrift.InitialMultiScan success,
      org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec)
      org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException sec,
      TSampleNotPresentException tsnpe)
     {
       this();
       this.success = success;
       this.sec = sec;
      this.tsnpe = tsnpe;
     }
 
     /**
@@ -10119,6 +10590,9 @@ import org.slf4j.LoggerFactory;
       if (other.isSetSec()) {
         this.sec = new org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException(other.sec);
       }
      if (other.isSetTsnpe()) {
        this.tsnpe = new TSampleNotPresentException(other.tsnpe);
      }
     }
 
     public startMultiScan_result deepCopy() {
@@ -10129,6 +10603,7 @@ import org.slf4j.LoggerFactory;
     public void clear() {
       this.success = null;
       this.sec = null;
      this.tsnpe = null;
     }
 
     public org.apache.accumulo.core.data.thrift.InitialMultiScan getSuccess() {
@@ -10179,6 +10654,30 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public TSampleNotPresentException getTsnpe() {
      return this.tsnpe;
    }

    public startMultiScan_result setTsnpe(TSampleNotPresentException tsnpe) {
      this.tsnpe = tsnpe;
      return this;
    }

    public void unsetTsnpe() {
      this.tsnpe = null;
    }

    /** Returns true if field tsnpe is set (has been assigned a value) and false otherwise */
    public boolean isSetTsnpe() {
      return this.tsnpe != null;
    }

    public void setTsnpeIsSet(boolean value) {
      if (!value) {
        this.tsnpe = null;
      }
    }

     public void setFieldValue(_Fields field, Object value) {
       switch (field) {
       case SUCCESS:
@@ -10197,6 +10696,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case TSNPE:
        if (value == null) {
          unsetTsnpe();
        } else {
          setTsnpe((TSampleNotPresentException)value);
        }
        break;

       }
     }
 
@@ -10208,6 +10715,9 @@ import org.slf4j.LoggerFactory;
       case SEC:
         return getSec();
 
      case TSNPE:
        return getTsnpe();

       }
       throw new IllegalStateException();
     }
@@ -10223,6 +10733,8 @@ import org.slf4j.LoggerFactory;
         return isSetSuccess();
       case SEC:
         return isSetSec();
      case TSNPE:
        return isSetTsnpe();
       }
       throw new IllegalStateException();
     }
@@ -10258,6 +10770,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_tsnpe = true && this.isSetTsnpe();
      boolean that_present_tsnpe = true && that.isSetTsnpe();
      if (this_present_tsnpe || that_present_tsnpe) {
        if (!(this_present_tsnpe && that_present_tsnpe))
          return false;
        if (!this.tsnpe.equals(that.tsnpe))
          return false;
      }

       return true;
     }
 
@@ -10294,6 +10815,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetTsnpe()).compareTo(other.isSetTsnpe());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetTsnpe()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tsnpe, other.tsnpe);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       return 0;
     }
 
@@ -10329,6 +10860,14 @@ import org.slf4j.LoggerFactory;
         sb.append(this.sec);
       }
       first = false;
      if (!first) sb.append(", ");
      sb.append("tsnpe:");
      if (this.tsnpe == null) {
        sb.append("null");
      } else {
        sb.append(this.tsnpe);
      }
      first = false;
       sb.append(")");
       return sb.toString();
     }
@@ -10393,6 +10932,15 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 2: // TSNPE
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.tsnpe = new TSampleNotPresentException();
                struct.tsnpe.read(iprot);
                struct.setTsnpeIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
             default:
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
           }
@@ -10418,6 +10966,11 @@ import org.slf4j.LoggerFactory;
           struct.sec.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.tsnpe != null) {
          oprot.writeFieldBegin(TSNPE_FIELD_DESC);
          struct.tsnpe.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -10442,19 +10995,25 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSec()) {
           optionals.set(1);
         }
        oprot.writeBitSet(optionals, 2);
        if (struct.isSetTsnpe()) {
          optionals.set(2);
        }
        oprot.writeBitSet(optionals, 3);
         if (struct.isSetSuccess()) {
           struct.success.write(oprot);
         }
         if (struct.isSetSec()) {
           struct.sec.write(oprot);
         }
        if (struct.isSetTsnpe()) {
          struct.tsnpe.write(oprot);
        }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, startMultiScan_result struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(2);
        BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           struct.success = new org.apache.accumulo.core.data.thrift.InitialMultiScan();
           struct.success.read(iprot);
@@ -10465,6 +11024,11 @@ import org.slf4j.LoggerFactory;
           struct.sec.read(iprot);
           struct.setSecIsSet(true);
         }
        if (incoming.get(2)) {
          struct.tsnpe = new TSampleNotPresentException();
          struct.tsnpe.read(iprot);
          struct.setTsnpeIsSet(true);
        }
       }
     }
 
@@ -10932,6 +11496,7 @@ import org.slf4j.LoggerFactory;
 
     private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.STRUCT, (short)0);
     private static final org.apache.thrift.protocol.TField NSSI_FIELD_DESC = new org.apache.thrift.protocol.TField("nssi", org.apache.thrift.protocol.TType.STRUCT, (short)1);
    private static final org.apache.thrift.protocol.TField TSNPE_FIELD_DESC = new org.apache.thrift.protocol.TField("tsnpe", org.apache.thrift.protocol.TType.STRUCT, (short)2);
 
     private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
     static {
@@ -10941,11 +11506,13 @@ import org.slf4j.LoggerFactory;
 
     public org.apache.accumulo.core.data.thrift.MultiScanResult success; // required
     public NoSuchScanIDException nssi; // required
    public TSampleNotPresentException tsnpe; // required
 
     /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
     public enum _Fields implements org.apache.thrift.TFieldIdEnum {
       SUCCESS((short)0, "success"),
      NSSI((short)1, "nssi");
      NSSI((short)1, "nssi"),
      TSNPE((short)2, "tsnpe");
 
       private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
 
@@ -10964,6 +11531,8 @@ import org.slf4j.LoggerFactory;
             return SUCCESS;
           case 1: // NSSI
             return NSSI;
          case 2: // TSNPE
            return TSNPE;
           default:
             return null;
         }
@@ -11011,6 +11580,8 @@ import org.slf4j.LoggerFactory;
           new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, org.apache.accumulo.core.data.thrift.MultiScanResult.class)));
       tmpMap.put(_Fields.NSSI, new org.apache.thrift.meta_data.FieldMetaData("nssi", org.apache.thrift.TFieldRequirementType.DEFAULT, 
           new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      tmpMap.put(_Fields.TSNPE, new org.apache.thrift.meta_data.FieldMetaData("tsnpe", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
       metaDataMap = Collections.unmodifiableMap(tmpMap);
       org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(continueMultiScan_result.class, metaDataMap);
     }
@@ -11020,11 +11591,13 @@ import org.slf4j.LoggerFactory;
 
     public continueMultiScan_result(
       org.apache.accumulo.core.data.thrift.MultiScanResult success,
      NoSuchScanIDException nssi)
      NoSuchScanIDException nssi,
      TSampleNotPresentException tsnpe)
     {
       this();
       this.success = success;
       this.nssi = nssi;
      this.tsnpe = tsnpe;
     }
 
     /**
@@ -11037,6 +11610,9 @@ import org.slf4j.LoggerFactory;
       if (other.isSetNssi()) {
         this.nssi = new NoSuchScanIDException(other.nssi);
       }
      if (other.isSetTsnpe()) {
        this.tsnpe = new TSampleNotPresentException(other.tsnpe);
      }
     }
 
     public continueMultiScan_result deepCopy() {
@@ -11047,6 +11623,7 @@ import org.slf4j.LoggerFactory;
     public void clear() {
       this.success = null;
       this.nssi = null;
      this.tsnpe = null;
     }
 
     public org.apache.accumulo.core.data.thrift.MultiScanResult getSuccess() {
@@ -11097,6 +11674,30 @@ import org.slf4j.LoggerFactory;
       }
     }
 
    public TSampleNotPresentException getTsnpe() {
      return this.tsnpe;
    }

    public continueMultiScan_result setTsnpe(TSampleNotPresentException tsnpe) {
      this.tsnpe = tsnpe;
      return this;
    }

    public void unsetTsnpe() {
      this.tsnpe = null;
    }

    /** Returns true if field tsnpe is set (has been assigned a value) and false otherwise */
    public boolean isSetTsnpe() {
      return this.tsnpe != null;
    }

    public void setTsnpeIsSet(boolean value) {
      if (!value) {
        this.tsnpe = null;
      }
    }

     public void setFieldValue(_Fields field, Object value) {
       switch (field) {
       case SUCCESS:
@@ -11115,6 +11716,14 @@ import org.slf4j.LoggerFactory;
         }
         break;
 
      case TSNPE:
        if (value == null) {
          unsetTsnpe();
        } else {
          setTsnpe((TSampleNotPresentException)value);
        }
        break;

       }
     }
 
@@ -11126,6 +11735,9 @@ import org.slf4j.LoggerFactory;
       case NSSI:
         return getNssi();
 
      case TSNPE:
        return getTsnpe();

       }
       throw new IllegalStateException();
     }
@@ -11141,6 +11753,8 @@ import org.slf4j.LoggerFactory;
         return isSetSuccess();
       case NSSI:
         return isSetNssi();
      case TSNPE:
        return isSetTsnpe();
       }
       throw new IllegalStateException();
     }
@@ -11176,6 +11790,15 @@ import org.slf4j.LoggerFactory;
           return false;
       }
 
      boolean this_present_tsnpe = true && this.isSetTsnpe();
      boolean that_present_tsnpe = true && that.isSetTsnpe();
      if (this_present_tsnpe || that_present_tsnpe) {
        if (!(this_present_tsnpe && that_present_tsnpe))
          return false;
        if (!this.tsnpe.equals(that.tsnpe))
          return false;
      }

       return true;
     }
 
@@ -11212,6 +11835,16 @@ import org.slf4j.LoggerFactory;
           return lastComparison;
         }
       }
      lastComparison = Boolean.valueOf(isSetTsnpe()).compareTo(other.isSetTsnpe());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetTsnpe()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tsnpe, other.tsnpe);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
       return 0;
     }
 
@@ -11247,6 +11880,14 @@ import org.slf4j.LoggerFactory;
         sb.append(this.nssi);
       }
       first = false;
      if (!first) sb.append(", ");
      sb.append("tsnpe:");
      if (this.tsnpe == null) {
        sb.append("null");
      } else {
        sb.append(this.tsnpe);
      }
      first = false;
       sb.append(")");
       return sb.toString();
     }
@@ -11311,6 +11952,15 @@ import org.slf4j.LoggerFactory;
                 org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
               }
               break;
            case 2: // TSNPE
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.tsnpe = new TSampleNotPresentException();
                struct.tsnpe.read(iprot);
                struct.setTsnpeIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
             default:
               org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
           }
@@ -11336,6 +11986,11 @@ import org.slf4j.LoggerFactory;
           struct.nssi.write(oprot);
           oprot.writeFieldEnd();
         }
        if (struct.tsnpe != null) {
          oprot.writeFieldBegin(TSNPE_FIELD_DESC);
          struct.tsnpe.write(oprot);
          oprot.writeFieldEnd();
        }
         oprot.writeFieldStop();
         oprot.writeStructEnd();
       }
@@ -11360,19 +12015,25 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetNssi()) {
           optionals.set(1);
         }
        oprot.writeBitSet(optionals, 2);
        if (struct.isSetTsnpe()) {
          optionals.set(2);
        }
        oprot.writeBitSet(optionals, 3);
         if (struct.isSetSuccess()) {
           struct.success.write(oprot);
         }
         if (struct.isSetNssi()) {
           struct.nssi.write(oprot);
         }
        if (struct.isSetTsnpe()) {
          struct.tsnpe.write(oprot);
        }
       }
 
       @Override
       public void read(org.apache.thrift.protocol.TProtocol prot, continueMultiScan_result struct) throws org.apache.thrift.TException {
         TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(2);
        BitSet incoming = iprot.readBitSet(3);
         if (incoming.get(0)) {
           struct.success = new org.apache.accumulo.core.data.thrift.MultiScanResult();
           struct.success.read(iprot);
@@ -11383,6 +12044,11 @@ import org.slf4j.LoggerFactory;
           struct.nssi.read(iprot);
           struct.setNssiIsSet(true);
         }
        if (incoming.get(2)) {
          struct.tsnpe = new TSampleNotPresentException();
          struct.tsnpe.read(iprot);
          struct.setTsnpeIsSet(true);
        }
       }
     }
 
@@ -13809,14 +14475,14 @@ import org.slf4j.LoggerFactory;
             case 4: // MUTATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list212 = iprot.readListBegin();
                  struct.mutations = new ArrayList<org.apache.accumulo.core.data.thrift.TMutation>(_list212.size);
                  for (int _i213 = 0; _i213 < _list212.size; ++_i213)
                  org.apache.thrift.protocol.TList _list222 = iprot.readListBegin();
                  struct.mutations = new ArrayList<org.apache.accumulo.core.data.thrift.TMutation>(_list222.size);
                  for (int _i223 = 0; _i223 < _list222.size; ++_i223)
                   {
                    org.apache.accumulo.core.data.thrift.TMutation _elem214;
                    _elem214 = new org.apache.accumulo.core.data.thrift.TMutation();
                    _elem214.read(iprot);
                    struct.mutations.add(_elem214);
                    org.apache.accumulo.core.data.thrift.TMutation _elem224;
                    _elem224 = new org.apache.accumulo.core.data.thrift.TMutation();
                    _elem224.read(iprot);
                    struct.mutations.add(_elem224);
                   }
                   iprot.readListEnd();
                 }
@@ -13857,9 +14523,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(MUTATIONS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.mutations.size()));
            for (org.apache.accumulo.core.data.thrift.TMutation _iter215 : struct.mutations)
            for (org.apache.accumulo.core.data.thrift.TMutation _iter225 : struct.mutations)
             {
              _iter215.write(oprot);
              _iter225.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -13908,9 +14574,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetMutations()) {
           {
             oprot.writeI32(struct.mutations.size());
            for (org.apache.accumulo.core.data.thrift.TMutation _iter216 : struct.mutations)
            for (org.apache.accumulo.core.data.thrift.TMutation _iter226 : struct.mutations)
             {
              _iter216.write(oprot);
              _iter226.write(oprot);
             }
           }
         }
@@ -13936,14 +14602,14 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TList _list217 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.mutations = new ArrayList<org.apache.accumulo.core.data.thrift.TMutation>(_list217.size);
            for (int _i218 = 0; _i218 < _list217.size; ++_i218)
            org.apache.thrift.protocol.TList _list227 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.mutations = new ArrayList<org.apache.accumulo.core.data.thrift.TMutation>(_list227.size);
            for (int _i228 = 0; _i228 < _list227.size; ++_i228)
             {
              org.apache.accumulo.core.data.thrift.TMutation _elem219;
              _elem219 = new org.apache.accumulo.core.data.thrift.TMutation();
              _elem219.read(iprot);
              struct.mutations.add(_elem219);
              org.apache.accumulo.core.data.thrift.TMutation _elem229;
              _elem229 = new org.apache.accumulo.core.data.thrift.TMutation();
              _elem229.read(iprot);
              struct.mutations.add(_elem229);
             }
           }
           struct.setMutationsIsSet(true);
@@ -16875,13 +17541,13 @@ import org.slf4j.LoggerFactory;
             case 3: // AUTHORIZATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list220 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list220.size);
                  for (int _i221 = 0; _i221 < _list220.size; ++_i221)
                  org.apache.thrift.protocol.TList _list230 = iprot.readListBegin();
                  struct.authorizations = new ArrayList<ByteBuffer>(_list230.size);
                  for (int _i231 = 0; _i231 < _list230.size; ++_i231)
                   {
                    ByteBuffer _elem222;
                    _elem222 = iprot.readBinary();
                    struct.authorizations.add(_elem222);
                    ByteBuffer _elem232;
                    _elem232 = iprot.readBinary();
                    struct.authorizations.add(_elem232);
                   }
                   iprot.readListEnd();
                 }
@@ -16935,9 +17601,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(AUTHORIZATIONS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.authorizations.size()));
            for (ByteBuffer _iter223 : struct.authorizations)
            for (ByteBuffer _iter233 : struct.authorizations)
             {
              oprot.writeBinary(_iter223);
              oprot.writeBinary(_iter233);
             }
             oprot.writeListEnd();
           }
@@ -16996,9 +17662,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetAuthorizations()) {
           {
             oprot.writeI32(struct.authorizations.size());
            for (ByteBuffer _iter224 : struct.authorizations)
            for (ByteBuffer _iter234 : struct.authorizations)
             {
              oprot.writeBinary(_iter224);
              oprot.writeBinary(_iter234);
             }
           }
         }
@@ -17026,13 +17692,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TList _list225 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list225.size);
            for (int _i226 = 0; _i226 < _list225.size; ++_i226)
            org.apache.thrift.protocol.TList _list235 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.authorizations = new ArrayList<ByteBuffer>(_list235.size);
            for (int _i236 = 0; _i236 < _list235.size; ++_i236)
             {
              ByteBuffer _elem227;
              _elem227 = iprot.readBinary();
              struct.authorizations.add(_elem227);
              ByteBuffer _elem237;
              _elem237 = iprot.readBinary();
              struct.authorizations.add(_elem237);
             }
           }
           struct.setAuthorizationsIsSet(true);
@@ -18079,27 +18745,27 @@ import org.slf4j.LoggerFactory;
             case 3: // MUTATIONS
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map228 = iprot.readMapBegin();
                  struct.mutations = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TConditionalMutation>>(2*_map228.size);
                  for (int _i229 = 0; _i229 < _map228.size; ++_i229)
                  org.apache.thrift.protocol.TMap _map238 = iprot.readMapBegin();
                  struct.mutations = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TConditionalMutation>>(2*_map238.size);
                  for (int _i239 = 0; _i239 < _map238.size; ++_i239)
                   {
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key230;
                    List<org.apache.accumulo.core.data.thrift.TConditionalMutation> _val231;
                    _key230 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key230.read(iprot);
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key240;
                    List<org.apache.accumulo.core.data.thrift.TConditionalMutation> _val241;
                    _key240 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key240.read(iprot);
                     {
                      org.apache.thrift.protocol.TList _list232 = iprot.readListBegin();
                      _val231 = new ArrayList<org.apache.accumulo.core.data.thrift.TConditionalMutation>(_list232.size);
                      for (int _i233 = 0; _i233 < _list232.size; ++_i233)
                      org.apache.thrift.protocol.TList _list242 = iprot.readListBegin();
                      _val241 = new ArrayList<org.apache.accumulo.core.data.thrift.TConditionalMutation>(_list242.size);
                      for (int _i243 = 0; _i243 < _list242.size; ++_i243)
                       {
                        org.apache.accumulo.core.data.thrift.TConditionalMutation _elem234;
                        _elem234 = new org.apache.accumulo.core.data.thrift.TConditionalMutation();
                        _elem234.read(iprot);
                        _val231.add(_elem234);
                        org.apache.accumulo.core.data.thrift.TConditionalMutation _elem244;
                        _elem244 = new org.apache.accumulo.core.data.thrift.TConditionalMutation();
                        _elem244.read(iprot);
                        _val241.add(_elem244);
                       }
                       iprot.readListEnd();
                     }
                    struct.mutations.put(_key230, _val231);
                    struct.mutations.put(_key240, _val241);
                   }
                   iprot.readMapEnd();
                 }
@@ -18111,13 +18777,13 @@ import org.slf4j.LoggerFactory;
             case 4: // SYMBOLS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list235 = iprot.readListBegin();
                  struct.symbols = new ArrayList<String>(_list235.size);
                  for (int _i236 = 0; _i236 < _list235.size; ++_i236)
                  org.apache.thrift.protocol.TList _list245 = iprot.readListBegin();
                  struct.symbols = new ArrayList<String>(_list245.size);
                  for (int _i246 = 0; _i246 < _list245.size; ++_i246)
                   {
                    String _elem237;
                    _elem237 = iprot.readString();
                    struct.symbols.add(_elem237);
                    String _elem247;
                    _elem247 = iprot.readString();
                    struct.symbols.add(_elem247);
                   }
                   iprot.readListEnd();
                 }
@@ -18153,14 +18819,14 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(MUTATIONS_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, struct.mutations.size()));
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TConditionalMutation>> _iter238 : struct.mutations.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TConditionalMutation>> _iter248 : struct.mutations.entrySet())
             {
              _iter238.getKey().write(oprot);
              _iter248.getKey().write(oprot);
               {
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter238.getValue().size()));
                for (org.apache.accumulo.core.data.thrift.TConditionalMutation _iter239 : _iter238.getValue())
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, _iter248.getValue().size()));
                for (org.apache.accumulo.core.data.thrift.TConditionalMutation _iter249 : _iter248.getValue())
                 {
                  _iter239.write(oprot);
                  _iter249.write(oprot);
                 }
                 oprot.writeListEnd();
               }
@@ -18173,9 +18839,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SYMBOLS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.symbols.size()));
            for (String _iter240 : struct.symbols)
            for (String _iter250 : struct.symbols)
             {
              oprot.writeString(_iter240);
              oprot.writeString(_iter250);
             }
             oprot.writeListEnd();
           }
@@ -18221,14 +18887,14 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetMutations()) {
           {
             oprot.writeI32(struct.mutations.size());
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TConditionalMutation>> _iter241 : struct.mutations.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, List<org.apache.accumulo.core.data.thrift.TConditionalMutation>> _iter251 : struct.mutations.entrySet())
             {
              _iter241.getKey().write(oprot);
              _iter251.getKey().write(oprot);
               {
                oprot.writeI32(_iter241.getValue().size());
                for (org.apache.accumulo.core.data.thrift.TConditionalMutation _iter242 : _iter241.getValue())
                oprot.writeI32(_iter251.getValue().size());
                for (org.apache.accumulo.core.data.thrift.TConditionalMutation _iter252 : _iter251.getValue())
                 {
                  _iter242.write(oprot);
                  _iter252.write(oprot);
                 }
               }
             }
@@ -18237,9 +18903,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSymbols()) {
           {
             oprot.writeI32(struct.symbols.size());
            for (String _iter243 : struct.symbols)
            for (String _iter253 : struct.symbols)
             {
              oprot.writeString(_iter243);
              oprot.writeString(_iter253);
             }
           }
         }
@@ -18260,39 +18926,39 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TMap _map244 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.mutations = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TConditionalMutation>>(2*_map244.size);
            for (int _i245 = 0; _i245 < _map244.size; ++_i245)
            org.apache.thrift.protocol.TMap _map254 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.LIST, iprot.readI32());
            struct.mutations = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,List<org.apache.accumulo.core.data.thrift.TConditionalMutation>>(2*_map254.size);
            for (int _i255 = 0; _i255 < _map254.size; ++_i255)
             {
              org.apache.accumulo.core.data.thrift.TKeyExtent _key246;
              List<org.apache.accumulo.core.data.thrift.TConditionalMutation> _val247;
              _key246 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key246.read(iprot);
              org.apache.accumulo.core.data.thrift.TKeyExtent _key256;
              List<org.apache.accumulo.core.data.thrift.TConditionalMutation> _val257;
              _key256 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key256.read(iprot);
               {
                org.apache.thrift.protocol.TList _list248 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val247 = new ArrayList<org.apache.accumulo.core.data.thrift.TConditionalMutation>(_list248.size);
                for (int _i249 = 0; _i249 < _list248.size; ++_i249)
                org.apache.thrift.protocol.TList _list258 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val257 = new ArrayList<org.apache.accumulo.core.data.thrift.TConditionalMutation>(_list258.size);
                for (int _i259 = 0; _i259 < _list258.size; ++_i259)
                 {
                  org.apache.accumulo.core.data.thrift.TConditionalMutation _elem250;
                  _elem250 = new org.apache.accumulo.core.data.thrift.TConditionalMutation();
                  _elem250.read(iprot);
                  _val247.add(_elem250);
                  org.apache.accumulo.core.data.thrift.TConditionalMutation _elem260;
                  _elem260 = new org.apache.accumulo.core.data.thrift.TConditionalMutation();
                  _elem260.read(iprot);
                  _val257.add(_elem260);
                 }
               }
              struct.mutations.put(_key246, _val247);
              struct.mutations.put(_key256, _val257);
             }
           }
           struct.setMutationsIsSet(true);
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TList _list251 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.symbols = new ArrayList<String>(_list251.size);
            for (int _i252 = 0; _i252 < _list251.size; ++_i252)
            org.apache.thrift.protocol.TList _list261 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.symbols = new ArrayList<String>(_list261.size);
            for (int _i262 = 0; _i262 < _list261.size; ++_i262)
             {
              String _elem253;
              _elem253 = iprot.readString();
              struct.symbols.add(_elem253);
              String _elem263;
              _elem263 = iprot.readString();
              struct.symbols.add(_elem263);
             }
           }
           struct.setSymbolsIsSet(true);
@@ -18688,14 +19354,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list254 = iprot.readListBegin();
                  struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TCMResult>(_list254.size);
                  for (int _i255 = 0; _i255 < _list254.size; ++_i255)
                  org.apache.thrift.protocol.TList _list264 = iprot.readListBegin();
                  struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TCMResult>(_list264.size);
                  for (int _i265 = 0; _i265 < _list264.size; ++_i265)
                   {
                    org.apache.accumulo.core.data.thrift.TCMResult _elem256;
                    _elem256 = new org.apache.accumulo.core.data.thrift.TCMResult();
                    _elem256.read(iprot);
                    struct.success.add(_elem256);
                    org.apache.accumulo.core.data.thrift.TCMResult _elem266;
                    _elem266 = new org.apache.accumulo.core.data.thrift.TCMResult();
                    _elem266.read(iprot);
                    struct.success.add(_elem266);
                   }
                   iprot.readListEnd();
                 }
@@ -18732,9 +19398,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (org.apache.accumulo.core.data.thrift.TCMResult _iter257 : struct.success)
            for (org.apache.accumulo.core.data.thrift.TCMResult _iter267 : struct.success)
             {
              _iter257.write(oprot);
              _iter267.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -18773,9 +19439,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (org.apache.accumulo.core.data.thrift.TCMResult _iter258 : struct.success)
            for (org.apache.accumulo.core.data.thrift.TCMResult _iter268 : struct.success)
             {
              _iter258.write(oprot);
              _iter268.write(oprot);
             }
           }
         }
@@ -18790,14 +19456,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(2);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list259 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TCMResult>(_list259.size);
            for (int _i260 = 0; _i260 < _list259.size; ++_i260)
            org.apache.thrift.protocol.TList _list269 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TCMResult>(_list269.size);
            for (int _i270 = 0; _i270 < _list269.size; ++_i270)
             {
              org.apache.accumulo.core.data.thrift.TCMResult _elem261;
              _elem261 = new org.apache.accumulo.core.data.thrift.TCMResult();
              _elem261.read(iprot);
              struct.success.add(_elem261);
              org.apache.accumulo.core.data.thrift.TCMResult _elem271;
              _elem271 = new org.apache.accumulo.core.data.thrift.TCMResult();
              _elem271.read(iprot);
              struct.success.add(_elem271);
             }
           }
           struct.setSuccessIsSet(true);
@@ -20608,29 +21274,29 @@ import org.slf4j.LoggerFactory;
             case 2: // FILES
               if (schemeField.type == org.apache.thrift.protocol.TType.MAP) {
                 {
                  org.apache.thrift.protocol.TMap _map262 = iprot.readMapBegin();
                  struct.files = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>(2*_map262.size);
                  for (int _i263 = 0; _i263 < _map262.size; ++_i263)
                  org.apache.thrift.protocol.TMap _map272 = iprot.readMapBegin();
                  struct.files = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>(2*_map272.size);
                  for (int _i273 = 0; _i273 < _map272.size; ++_i273)
                   {
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key264;
                    Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo> _val265;
                    _key264 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key264.read(iprot);
                    org.apache.accumulo.core.data.thrift.TKeyExtent _key274;
                    Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo> _val275;
                    _key274 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _key274.read(iprot);
                     {
                      org.apache.thrift.protocol.TMap _map266 = iprot.readMapBegin();
                      _val265 = new HashMap<String,org.apache.accumulo.core.data.thrift.MapFileInfo>(2*_map266.size);
                      for (int _i267 = 0; _i267 < _map266.size; ++_i267)
                      org.apache.thrift.protocol.TMap _map276 = iprot.readMapBegin();
                      _val275 = new HashMap<String,org.apache.accumulo.core.data.thrift.MapFileInfo>(2*_map276.size);
                      for (int _i277 = 0; _i277 < _map276.size; ++_i277)
                       {
                        String _key268;
                        org.apache.accumulo.core.data.thrift.MapFileInfo _val269;
                        _key268 = iprot.readString();
                        _val269 = new org.apache.accumulo.core.data.thrift.MapFileInfo();
                        _val269.read(iprot);
                        _val265.put(_key268, _val269);
                        String _key278;
                        org.apache.accumulo.core.data.thrift.MapFileInfo _val279;
                        _key278 = iprot.readString();
                        _val279 = new org.apache.accumulo.core.data.thrift.MapFileInfo();
                        _val279.read(iprot);
                        _val275.put(_key278, _val279);
                       }
                       iprot.readMapEnd();
                     }
                    struct.files.put(_key264, _val265);
                    struct.files.put(_key274, _val275);
                   }
                   iprot.readMapEnd();
                 }
@@ -20671,15 +21337,15 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(FILES_FIELD_DESC);
           {
             oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.MAP, struct.files.size()));
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> _iter270 : struct.files.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> _iter280 : struct.files.entrySet())
             {
              _iter270.getKey().write(oprot);
              _iter280.getKey().write(oprot);
               {
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, _iter270.getValue().size()));
                for (Map.Entry<String, org.apache.accumulo.core.data.thrift.MapFileInfo> _iter271 : _iter270.getValue().entrySet())
                oprot.writeMapBegin(new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, _iter280.getValue().size()));
                for (Map.Entry<String, org.apache.accumulo.core.data.thrift.MapFileInfo> _iter281 : _iter280.getValue().entrySet())
                 {
                  oprot.writeString(_iter271.getKey());
                  _iter271.getValue().write(oprot);
                  oprot.writeString(_iter281.getKey());
                  _iter281.getValue().write(oprot);
                 }
                 oprot.writeMapEnd();
               }
@@ -20745,15 +21411,15 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetFiles()) {
           {
             oprot.writeI32(struct.files.size());
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> _iter272 : struct.files.entrySet())
            for (Map.Entry<org.apache.accumulo.core.data.thrift.TKeyExtent, Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>> _iter282 : struct.files.entrySet())
             {
              _iter272.getKey().write(oprot);
              _iter282.getKey().write(oprot);
               {
                oprot.writeI32(_iter272.getValue().size());
                for (Map.Entry<String, org.apache.accumulo.core.data.thrift.MapFileInfo> _iter273 : _iter272.getValue().entrySet())
                oprot.writeI32(_iter282.getValue().size());
                for (Map.Entry<String, org.apache.accumulo.core.data.thrift.MapFileInfo> _iter283 : _iter282.getValue().entrySet())
                 {
                  oprot.writeString(_iter273.getKey());
                  _iter273.getValue().write(oprot);
                  oprot.writeString(_iter283.getKey());
                  _iter283.getValue().write(oprot);
                 }
               }
             }
@@ -20784,28 +21450,28 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(3)) {
           {
            org.apache.thrift.protocol.TMap _map274 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.files = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>(2*_map274.size);
            for (int _i275 = 0; _i275 < _map274.size; ++_i275)
            org.apache.thrift.protocol.TMap _map284 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRUCT, org.apache.thrift.protocol.TType.MAP, iprot.readI32());
            struct.files = new HashMap<org.apache.accumulo.core.data.thrift.TKeyExtent,Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo>>(2*_map284.size);
            for (int _i285 = 0; _i285 < _map284.size; ++_i285)
             {
              org.apache.accumulo.core.data.thrift.TKeyExtent _key276;
              Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo> _val277;
              _key276 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key276.read(iprot);
              org.apache.accumulo.core.data.thrift.TKeyExtent _key286;
              Map<String,org.apache.accumulo.core.data.thrift.MapFileInfo> _val287;
              _key286 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _key286.read(iprot);
               {
                org.apache.thrift.protocol.TMap _map278 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val277 = new HashMap<String,org.apache.accumulo.core.data.thrift.MapFileInfo>(2*_map278.size);
                for (int _i279 = 0; _i279 < _map278.size; ++_i279)
                org.apache.thrift.protocol.TMap _map288 = new org.apache.thrift.protocol.TMap(org.apache.thrift.protocol.TType.STRING, org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                _val287 = new HashMap<String,org.apache.accumulo.core.data.thrift.MapFileInfo>(2*_map288.size);
                for (int _i289 = 0; _i289 < _map288.size; ++_i289)
                 {
                  String _key280;
                  org.apache.accumulo.core.data.thrift.MapFileInfo _val281;
                  _key280 = iprot.readString();
                  _val281 = new org.apache.accumulo.core.data.thrift.MapFileInfo();
                  _val281.read(iprot);
                  _val277.put(_key280, _val281);
                  String _key290;
                  org.apache.accumulo.core.data.thrift.MapFileInfo _val291;
                  _key290 = iprot.readString();
                  _val291 = new org.apache.accumulo.core.data.thrift.MapFileInfo();
                  _val291.read(iprot);
                  _val287.put(_key290, _val291);
                 }
               }
              struct.files.put(_key276, _val277);
              struct.files.put(_key286, _val287);
             }
           }
           struct.setFilesIsSet(true);
@@ -21205,14 +21871,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list282 = iprot.readListBegin();
                  struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TKeyExtent>(_list282.size);
                  for (int _i283 = 0; _i283 < _list282.size; ++_i283)
                  org.apache.thrift.protocol.TList _list292 = iprot.readListBegin();
                  struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TKeyExtent>(_list292.size);
                  for (int _i293 = 0; _i293 < _list292.size; ++_i293)
                   {
                    org.apache.accumulo.core.data.thrift.TKeyExtent _elem284;
                    _elem284 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _elem284.read(iprot);
                    struct.success.add(_elem284);
                    org.apache.accumulo.core.data.thrift.TKeyExtent _elem294;
                    _elem294 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
                    _elem294.read(iprot);
                    struct.success.add(_elem294);
                   }
                   iprot.readListEnd();
                 }
@@ -21249,9 +21915,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (org.apache.accumulo.core.data.thrift.TKeyExtent _iter285 : struct.success)
            for (org.apache.accumulo.core.data.thrift.TKeyExtent _iter295 : struct.success)
             {
              _iter285.write(oprot);
              _iter295.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -21290,9 +21956,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (org.apache.accumulo.core.data.thrift.TKeyExtent _iter286 : struct.success)
            for (org.apache.accumulo.core.data.thrift.TKeyExtent _iter296 : struct.success)
             {
              _iter286.write(oprot);
              _iter296.write(oprot);
             }
           }
         }
@@ -21307,14 +21973,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(2);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list287 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TKeyExtent>(_list287.size);
            for (int _i288 = 0; _i288 < _list287.size; ++_i288)
            org.apache.thrift.protocol.TList _list297 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<org.apache.accumulo.core.data.thrift.TKeyExtent>(_list297.size);
            for (int _i298 = 0; _i298 < _list297.size; ++_i298)
             {
              org.apache.accumulo.core.data.thrift.TKeyExtent _elem289;
              _elem289 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _elem289.read(iprot);
              struct.success.add(_elem289);
              org.apache.accumulo.core.data.thrift.TKeyExtent _elem299;
              _elem299 = new org.apache.accumulo.core.data.thrift.TKeyExtent();
              _elem299.read(iprot);
              struct.success.add(_elem299);
             }
           }
           struct.setSuccessIsSet(true);
@@ -28888,14 +29554,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list290 = iprot.readListBegin();
                  struct.success = new ArrayList<TabletStats>(_list290.size);
                  for (int _i291 = 0; _i291 < _list290.size; ++_i291)
                  org.apache.thrift.protocol.TList _list300 = iprot.readListBegin();
                  struct.success = new ArrayList<TabletStats>(_list300.size);
                  for (int _i301 = 0; _i301 < _list300.size; ++_i301)
                   {
                    TabletStats _elem292;
                    _elem292 = new TabletStats();
                    _elem292.read(iprot);
                    struct.success.add(_elem292);
                    TabletStats _elem302;
                    _elem302 = new TabletStats();
                    _elem302.read(iprot);
                    struct.success.add(_elem302);
                   }
                   iprot.readListEnd();
                 }
@@ -28932,9 +29598,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (TabletStats _iter293 : struct.success)
            for (TabletStats _iter303 : struct.success)
             {
              _iter293.write(oprot);
              _iter303.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -28973,9 +29639,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (TabletStats _iter294 : struct.success)
            for (TabletStats _iter304 : struct.success)
             {
              _iter294.write(oprot);
              _iter304.write(oprot);
             }
           }
         }
@@ -28990,14 +29656,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(2);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list295 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<TabletStats>(_list295.size);
            for (int _i296 = 0; _i296 < _list295.size; ++_i296)
            org.apache.thrift.protocol.TList _list305 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<TabletStats>(_list305.size);
            for (int _i306 = 0; _i306 < _list305.size; ++_i306)
             {
              TabletStats _elem297;
              _elem297 = new TabletStats();
              _elem297.read(iprot);
              struct.success.add(_elem297);
              TabletStats _elem307;
              _elem307 = new TabletStats();
              _elem307.read(iprot);
              struct.success.add(_elem307);
             }
           }
           struct.setSuccessIsSet(true);
@@ -32271,14 +32937,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list298 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveScan>(_list298.size);
                  for (int _i299 = 0; _i299 < _list298.size; ++_i299)
                  org.apache.thrift.protocol.TList _list308 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveScan>(_list308.size);
                  for (int _i309 = 0; _i309 < _list308.size; ++_i309)
                   {
                    ActiveScan _elem300;
                    _elem300 = new ActiveScan();
                    _elem300.read(iprot);
                    struct.success.add(_elem300);
                    ActiveScan _elem310;
                    _elem310 = new ActiveScan();
                    _elem310.read(iprot);
                    struct.success.add(_elem310);
                   }
                   iprot.readListEnd();
                 }
@@ -32315,9 +32981,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (ActiveScan _iter301 : struct.success)
            for (ActiveScan _iter311 : struct.success)
             {
              _iter301.write(oprot);
              _iter311.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -32356,9 +33022,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ActiveScan _iter302 : struct.success)
            for (ActiveScan _iter312 : struct.success)
             {
              _iter302.write(oprot);
              _iter312.write(oprot);
             }
           }
         }
@@ -32373,14 +33039,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(2);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list303 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveScan>(_list303.size);
            for (int _i304 = 0; _i304 < _list303.size; ++_i304)
            org.apache.thrift.protocol.TList _list313 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveScan>(_list313.size);
            for (int _i314 = 0; _i314 < _list313.size; ++_i314)
             {
              ActiveScan _elem305;
              _elem305 = new ActiveScan();
              _elem305.read(iprot);
              struct.success.add(_elem305);
              ActiveScan _elem315;
              _elem315 = new ActiveScan();
              _elem315.read(iprot);
              struct.success.add(_elem315);
             }
           }
           struct.setSuccessIsSet(true);
@@ -33245,14 +33911,14 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list306 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveCompaction>(_list306.size);
                  for (int _i307 = 0; _i307 < _list306.size; ++_i307)
                  org.apache.thrift.protocol.TList _list316 = iprot.readListBegin();
                  struct.success = new ArrayList<ActiveCompaction>(_list316.size);
                  for (int _i317 = 0; _i317 < _list316.size; ++_i317)
                   {
                    ActiveCompaction _elem308;
                    _elem308 = new ActiveCompaction();
                    _elem308.read(iprot);
                    struct.success.add(_elem308);
                    ActiveCompaction _elem318;
                    _elem318 = new ActiveCompaction();
                    _elem318.read(iprot);
                    struct.success.add(_elem318);
                   }
                   iprot.readListEnd();
                 }
@@ -33289,9 +33955,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.success.size()));
            for (ActiveCompaction _iter309 : struct.success)
            for (ActiveCompaction _iter319 : struct.success)
             {
              _iter309.write(oprot);
              _iter319.write(oprot);
             }
             oprot.writeListEnd();
           }
@@ -33330,9 +33996,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (ActiveCompaction _iter310 : struct.success)
            for (ActiveCompaction _iter320 : struct.success)
             {
              _iter310.write(oprot);
              _iter320.write(oprot);
             }
           }
         }
@@ -33347,14 +34013,14 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(2);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list311 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveCompaction>(_list311.size);
            for (int _i312 = 0; _i312 < _list311.size; ++_i312)
            org.apache.thrift.protocol.TList _list321 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
            struct.success = new ArrayList<ActiveCompaction>(_list321.size);
            for (int _i322 = 0; _i322 < _list321.size; ++_i322)
             {
              ActiveCompaction _elem313;
              _elem313 = new ActiveCompaction();
              _elem313.read(iprot);
              struct.success.add(_elem313);
              ActiveCompaction _elem323;
              _elem323 = new ActiveCompaction();
              _elem323.read(iprot);
              struct.success.add(_elem323);
             }
           }
           struct.setSuccessIsSet(true);
@@ -33853,13 +34519,13 @@ import org.slf4j.LoggerFactory;
             case 3: // FILENAMES
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list314 = iprot.readListBegin();
                  struct.filenames = new ArrayList<String>(_list314.size);
                  for (int _i315 = 0; _i315 < _list314.size; ++_i315)
                  org.apache.thrift.protocol.TList _list324 = iprot.readListBegin();
                  struct.filenames = new ArrayList<String>(_list324.size);
                  for (int _i325 = 0; _i325 < _list324.size; ++_i325)
                   {
                    String _elem316;
                    _elem316 = iprot.readString();
                    struct.filenames.add(_elem316);
                    String _elem326;
                    _elem326 = iprot.readString();
                    struct.filenames.add(_elem326);
                   }
                   iprot.readListEnd();
                 }
@@ -33897,9 +34563,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(FILENAMES_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.filenames.size()));
            for (String _iter317 : struct.filenames)
            for (String _iter327 : struct.filenames)
             {
              oprot.writeString(_iter317);
              oprot.writeString(_iter327);
             }
             oprot.writeListEnd();
           }
@@ -33942,9 +34608,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetFilenames()) {
           {
             oprot.writeI32(struct.filenames.size());
            for (String _iter318 : struct.filenames)
            for (String _iter328 : struct.filenames)
             {
              oprot.writeString(_iter318);
              oprot.writeString(_iter328);
             }
           }
         }
@@ -33966,13 +34632,13 @@ import org.slf4j.LoggerFactory;
         }
         if (incoming.get(2)) {
           {
            org.apache.thrift.protocol.TList _list319 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.filenames = new ArrayList<String>(_list319.size);
            for (int _i320 = 0; _i320 < _list319.size; ++_i320)
            org.apache.thrift.protocol.TList _list329 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.filenames = new ArrayList<String>(_list329.size);
            for (int _i330 = 0; _i330 < _list329.size; ++_i330)
             {
              String _elem321;
              _elem321 = iprot.readString();
              struct.filenames.add(_elem321);
              String _elem331;
              _elem331 = iprot.readString();
              struct.filenames.add(_elem331);
             }
           }
           struct.setFilenamesIsSet(true);
@@ -34752,13 +35418,13 @@ import org.slf4j.LoggerFactory;
             case 0: // SUCCESS
               if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                 {
                  org.apache.thrift.protocol.TList _list322 = iprot.readListBegin();
                  struct.success = new ArrayList<String>(_list322.size);
                  for (int _i323 = 0; _i323 < _list322.size; ++_i323)
                  org.apache.thrift.protocol.TList _list332 = iprot.readListBegin();
                  struct.success = new ArrayList<String>(_list332.size);
                  for (int _i333 = 0; _i333 < _list332.size; ++_i333)
                   {
                    String _elem324;
                    _elem324 = iprot.readString();
                    struct.success.add(_elem324);
                    String _elem334;
                    _elem334 = iprot.readString();
                    struct.success.add(_elem334);
                   }
                   iprot.readListEnd();
                 }
@@ -34786,9 +35452,9 @@ import org.slf4j.LoggerFactory;
           oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
           {
             oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.success.size()));
            for (String _iter325 : struct.success)
            for (String _iter335 : struct.success)
             {
              oprot.writeString(_iter325);
              oprot.writeString(_iter335);
             }
             oprot.writeListEnd();
           }
@@ -34819,9 +35485,9 @@ import org.slf4j.LoggerFactory;
         if (struct.isSetSuccess()) {
           {
             oprot.writeI32(struct.success.size());
            for (String _iter326 : struct.success)
            for (String _iter336 : struct.success)
             {
              oprot.writeString(_iter326);
              oprot.writeString(_iter336);
             }
           }
         }
@@ -34833,13 +35499,13 @@ import org.slf4j.LoggerFactory;
         BitSet incoming = iprot.readBitSet(1);
         if (incoming.get(0)) {
           {
            org.apache.thrift.protocol.TList _list327 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<String>(_list327.size);
            for (int _i328 = 0; _i328 < _list327.size; ++_i328)
            org.apache.thrift.protocol.TList _list337 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
            struct.success = new ArrayList<String>(_list337.size);
            for (int _i338 = 0; _i338 < _list337.size; ++_i338)
             {
              String _elem329;
              _elem329 = iprot.readString();
              struct.success.add(_elem329);
              String _elem339;
              _elem339 = iprot.readString();
              struct.success.add(_elem339);
             }
           }
           struct.setSuccessIsSet(true);
diff --git a/core/src/main/java/org/apache/accumulo/core/util/LocalityGroupUtil.java b/core/src/main/java/org/apache/accumulo/core/util/LocalityGroupUtil.java
index a4936cf3e..07757a6b0 100644
-- a/core/src/main/java/org/apache/accumulo/core/util/LocalityGroupUtil.java
++ b/core/src/main/java/org/apache/accumulo/core/util/LocalityGroupUtil.java
@@ -186,11 +186,11 @@ public class LocalityGroupUtil {
     return ecf;
   }
 
  private static class PartitionedMutation extends Mutation {
  public static class PartitionedMutation extends Mutation {
     private byte[] row;
     private List<ColumnUpdate> updates;
 
    PartitionedMutation(byte[] row, List<ColumnUpdate> updates) {
    public PartitionedMutation(byte[] row, List<ColumnUpdate> updates) {
       this.row = row;
       this.updates = updates;
     }
diff --git a/core/src/main/thrift/tabletserver.thrift b/core/src/main/thrift/tabletserver.thrift
index 051daee58..27b72f201 100644
-- a/core/src/main/thrift/tabletserver.thrift
++ b/core/src/main/thrift/tabletserver.thrift
@@ -31,6 +31,10 @@ exception TooManyFilesException {
   1:data.TKeyExtent extent
 }
 
exception TSampleNotPresentException {
  1:data.TKeyExtent extent
}

 exception NoSuchScanIDException {
 }
 
@@ -136,6 +140,11 @@ struct IteratorConfig {
    1:list<TIteratorSetting> iterators;
 }
 
struct TSamplerConfiguration {
   1:string className
   2:map<string, string> options
}

 service TabletClientService extends client.ClientService {
   // scan a range of keys
   data.InitialScan startScan(11:trace.TInfo tinfo,
@@ -150,9 +159,10 @@ service TabletClientService extends client.ClientService {
                              9:bool waitForWrites,
                              10:bool isolated,
                              12:i64 readaheadThreshold,
                             13:i64 batchTimeOut)  throws (1:client.ThriftSecurityException sec, 2:NotServingTabletException nste, 3:TooManyFilesException tmfe),
                             13:TSamplerConfiguration samplerConfig,
                             14:i64 batchTimeOut)  throws (1:client.ThriftSecurityException sec, 2:NotServingTabletException nste, 3:TooManyFilesException tmfe, 4:TSampleNotPresentException tsnpe),
                              
  data.ScanResult continueScan(2:trace.TInfo tinfo, 1:data.ScanID scanID)  throws (1:NoSuchScanIDException nssi, 2:NotServingTabletException nste, 3:TooManyFilesException tmfe),
  data.ScanResult continueScan(2:trace.TInfo tinfo, 1:data.ScanID scanID)  throws (1:NoSuchScanIDException nssi, 2:NotServingTabletException nste, 3:TooManyFilesException tmfe, 4:TSampleNotPresentException tsnpe),
   oneway void closeScan(2:trace.TInfo tinfo, 1:data.ScanID scanID),
 
   // scan over a series of ranges
@@ -164,8 +174,9 @@ service TabletClientService extends client.ClientService {
                                   5:map<string, map<string, string>> ssio,
                                   6:list<binary> authorizations,
                                   7:bool waitForWrites,
                                  9:i64 batchTimeOut)  throws (1:client.ThriftSecurityException sec),
  data.MultiScanResult continueMultiScan(2:trace.TInfo tinfo, 1:data.ScanID scanID) throws (1:NoSuchScanIDException nssi),
                                  9:TSamplerConfiguration samplerConfig,
                                  10:i64 batchTimeOut)  throws (1:client.ThriftSecurityException sec, 2:TSampleNotPresentException tsnpe),
  data.MultiScanResult continueMultiScan(2:trace.TInfo tinfo, 1:data.ScanID scanID) throws (1:NoSuchScanIDException nssi, 2:TSampleNotPresentException tsnpe),
   void closeMultiScan(2:trace.TInfo tinfo, 1:data.ScanID scanID) throws (1:NoSuchScanIDException nssi),
   
   //the following calls support a batch update to multiple tablets on a tablet server
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
index 7a56d1dba..7bf9eb17b 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
@@ -36,6 +36,7 @@ import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
@@ -226,6 +227,22 @@ public class TableOperationsHelperTest {
         TableNotFoundException {
       return false;
     }

    @Override
    public void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws TableNotFoundException, AccumuloException,
        AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clearSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration(String tableName) throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
      throw new UnsupportedOperationException();
    }
   }
 
   protected TableOperationsHelper getHelper() {
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
index bcf8a24bf..d88453ee5 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
@@ -20,9 +20,12 @@ import static org.junit.Assert.assertEquals;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.mapred.JobConf;
 import org.junit.Test;
 
@@ -36,6 +39,9 @@ public class AccumuloFileOutputFormatTest {
     long c = 50l;
     long d = 10l;
     String e = "snappy";
    SamplerConfiguration samplerConfig = new SamplerConfiguration(RowSampler.class.getName());
    samplerConfig.addOption("hasher", "murmur3_32");
    samplerConfig.addOption("modulus", "109");
 
     JobConf job = new JobConf();
     AccumuloFileOutputFormat.setReplication(job, a);
@@ -43,6 +49,7 @@ public class AccumuloFileOutputFormatTest {
     AccumuloFileOutputFormat.setDataBlockSize(job, c);
     AccumuloFileOutputFormat.setIndexBlockSize(job, d);
     AccumuloFileOutputFormat.setCompressionType(job, e);
    AccumuloFileOutputFormat.setSampler(job, samplerConfig);
 
     AccumuloConfiguration acuconf = FileOutputConfigurator.getAccumuloConfiguration(AccumuloFileOutputFormat.class, job);
 
@@ -51,12 +58,16 @@ public class AccumuloFileOutputFormatTest {
     assertEquals(50l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE));
     assertEquals(10l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX));
     assertEquals("snappy", acuconf.get(Property.TABLE_FILE_COMPRESSION_TYPE));
    assertEquals(new SamplerConfigurationImpl(samplerConfig), SamplerConfigurationImpl.newSamplerConfig(acuconf));
 
     a = 17;
     b = 1300l;
     c = 150l;
     d = 110l;
     e = "lzo";
    samplerConfig = new SamplerConfiguration(RowSampler.class.getName());
    samplerConfig.addOption("hasher", "md5");
    samplerConfig.addOption("modulus", "100003");
 
     job = new JobConf();
     AccumuloFileOutputFormat.setReplication(job, a);
@@ -64,6 +75,7 @@ public class AccumuloFileOutputFormatTest {
     AccumuloFileOutputFormat.setDataBlockSize(job, c);
     AccumuloFileOutputFormat.setIndexBlockSize(job, d);
     AccumuloFileOutputFormat.setCompressionType(job, e);
    AccumuloFileOutputFormat.setSampler(job, samplerConfig);
 
     acuconf = FileOutputConfigurator.getAccumuloConfiguration(AccumuloFileOutputFormat.class, job);
 
@@ -72,6 +84,6 @@ public class AccumuloFileOutputFormatTest {
     assertEquals(150l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE));
     assertEquals(110l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX));
     assertEquals("lzo", acuconf.get(Property.TABLE_FILE_COMPRESSION_TYPE));

    assertEquals(new SamplerConfigurationImpl(samplerConfig), SamplerConfigurationImpl.newSamplerConfig(acuconf));
   }
 }
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
index 39235666c..cf0c8d652 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
@@ -20,9 +20,12 @@ import static org.junit.Assert.assertEquals;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.mapreduce.Job;
 import org.junit.Test;
 
@@ -36,6 +39,9 @@ public class AccumuloFileOutputFormatTest {
     long c = 50l;
     long d = 10l;
     String e = "snappy";
    SamplerConfiguration samplerConfig = new SamplerConfiguration(RowSampler.class.getName());
    samplerConfig.addOption("hasher", "murmur3_32");
    samplerConfig.addOption("modulus", "109");
 
     Job job1 = Job.getInstance();
     AccumuloFileOutputFormat.setReplication(job1, a);
@@ -43,6 +49,7 @@ public class AccumuloFileOutputFormatTest {
     AccumuloFileOutputFormat.setDataBlockSize(job1, c);
     AccumuloFileOutputFormat.setIndexBlockSize(job1, d);
     AccumuloFileOutputFormat.setCompressionType(job1, e);
    AccumuloFileOutputFormat.setSampler(job1, samplerConfig);
 
     AccumuloConfiguration acuconf = FileOutputConfigurator.getAccumuloConfiguration(AccumuloFileOutputFormat.class, job1.getConfiguration());
 
@@ -51,12 +58,16 @@ public class AccumuloFileOutputFormatTest {
     assertEquals(50l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE));
     assertEquals(10l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX));
     assertEquals("snappy", acuconf.get(Property.TABLE_FILE_COMPRESSION_TYPE));
    assertEquals(new SamplerConfigurationImpl(samplerConfig), SamplerConfigurationImpl.newSamplerConfig(acuconf));
 
     a = 17;
     b = 1300l;
     c = 150l;
     d = 110l;
     e = "lzo";
    samplerConfig = new SamplerConfiguration(RowSampler.class.getName());
    samplerConfig.addOption("hasher", "md5");
    samplerConfig.addOption("modulus", "100003");
 
     Job job2 = Job.getInstance();
     AccumuloFileOutputFormat.setReplication(job2, a);
@@ -64,6 +75,7 @@ public class AccumuloFileOutputFormatTest {
     AccumuloFileOutputFormat.setDataBlockSize(job2, c);
     AccumuloFileOutputFormat.setIndexBlockSize(job2, d);
     AccumuloFileOutputFormat.setCompressionType(job2, e);
    AccumuloFileOutputFormat.setSampler(job2, samplerConfig);
 
     acuconf = FileOutputConfigurator.getAccumuloConfiguration(AccumuloFileOutputFormat.class, job2.getConfiguration());
 
@@ -72,6 +84,7 @@ public class AccumuloFileOutputFormatTest {
     assertEquals(150l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE));
     assertEquals(110l, acuconf.getMemoryInBytes(Property.TABLE_FILE_COMPRESSED_BLOCK_SIZE_INDEX));
     assertEquals("lzo", acuconf.get(Property.TABLE_FILE_COMPRESSION_TYPE));
    assertEquals(new SamplerConfigurationImpl(samplerConfig), SamplerConfigurationImpl.newSamplerConfig(acuconf));
 
   }
 }
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
index 6f89454e2..66978ddfc 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/MultiLevelIndexTest.java
@@ -21,7 +21,6 @@ import java.io.IOException;
 import java.util.Random;
 
 import junit.framework.TestCase;

 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.file.blockfile.ABlockWriter;
@@ -77,7 +76,7 @@ public class MultiLevelIndexTest extends TestCase {
     FSDataInputStream in = new FSDataInputStream(bais);
     CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in, data.length, CachedConfiguration.getInstance(), aconf);
 
    Reader reader = new Reader(_cbr, RFile.RINDEX_VER_7);
    Reader reader = new Reader(_cbr, RFile.RINDEX_VER_8);
     BlockRead rootIn = _cbr.getMetaBlock("root");
     reader.readFields(rootIn);
     rootIn.close();
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
index 2e2b34683..ab98f4927 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
@@ -28,16 +28,21 @@ import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
import java.util.AbstractMap;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Iterator;
import java.util.List;
 import java.util.Map.Entry;
 import java.util.Random;
 import java.util.Set;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
@@ -57,6 +62,10 @@ import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.security.crypto.CryptoTest;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.hadoop.conf.Configuration;
@@ -68,14 +77,37 @@ import org.apache.hadoop.fs.Seekable;
 import org.apache.hadoop.io.Text;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
import org.junit.Assert;
 import org.junit.Rule;
 import org.junit.Test;
 import org.junit.rules.TemporaryFolder;
 
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
 import com.google.common.primitives.Bytes;
 
 public class RFileTest {
 
  public static class SampleIE extends BaseIteratorEnvironment {

    private SamplerConfiguration samplerConfig;

    SampleIE(SamplerConfiguration config) {
      this.samplerConfig = config;
    }

    @Override
    public boolean isSamplingEnabled() {
      return samplerConfig != null;
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration() {
      return samplerConfig;
    }
  }

   private static final Collection<ByteSequence> EMPTY_COL_FAMS = new ArrayList<ByteSequence>();
 
   @Rule
@@ -193,7 +225,15 @@ public class RFileTest {
       baos = new ByteArrayOutputStream();
       dos = new FSDataOutputStream(baos, new FileSystem.Statistics("a"));
       CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", conf, accumuloConfiguration);
      writer = new RFile.Writer(_cbw, blockSize, 1000);

      SamplerConfigurationImpl samplerConfig = SamplerConfigurationImpl.newSamplerConfig(accumuloConfiguration);
      Sampler sampler = null;

      if (samplerConfig != null) {
        sampler = SamplerFactory.newSampler(samplerConfig, accumuloConfiguration);
      }

      writer = new RFile.Writer(_cbw, blockSize, 1000, samplerConfig, sampler);
 
       if (startDLG)
         writer.startDefaultLocalityGroup();
@@ -221,7 +261,6 @@ public class RFileTest {
     }
 
     public void openReader(boolean cfsi) throws IOException {

       int fileLength = 0;
       byte[] data = null;
       data = baos.toByteArray();
@@ -1206,7 +1245,6 @@ public class RFileTest {
   @Test
   public void test14() throws IOException {
     // test starting locality group after default locality group was started

     TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
@@ -1558,6 +1596,7 @@ public class RFileTest {
     runVersionTest(3);
     runVersionTest(4);
     runVersionTest(6);
    runVersionTest(7);
   }
 
   private void runVersionTest(int version) throws IOException {
@@ -1762,6 +1801,294 @@ public class RFileTest {
     conf = null;
   }
 
  private Key nk(int r, int c) {
    String row = String.format("r%06d", r);
    switch (c) {
      case 0:
        return new Key(row, "user", "addr");
      case 1:
        return new Key(row, "user", "name");
      default:
        throw new IllegalArgumentException();
    }
  }

  private Value nv(int r, int c) {
    switch (c) {
      case 0:
        return new Value(("123" + r + " west st").getBytes());
      case 1:
        return new Value(("bob" + r).getBytes());
      default:
        throw new IllegalArgumentException();
    }
  }

  private static void hash(Hasher hasher, Key key, Value val) {
    hasher.putBytes(key.getRowData().toArray());
    hasher.putBytes(key.getColumnFamilyData().toArray());
    hasher.putBytes(key.getColumnQualifierData().toArray());
    hasher.putBytes(key.getColumnVisibilityData().toArray());
    hasher.putLong(key.getTimestamp());
    hasher.putBoolean(key.isDeleted());
    hasher.putBytes(val.get());
  }

  private static void add(TestRFile trf, Key key, Value val, Hasher dataHasher, List<Entry<Key,Value>> sample, Sampler sampler) throws IOException {
    if (sampler.accept(key)) {
      sample.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(key, val));
    }

    hash(dataHasher, key, val);

    trf.writer.append(key, val);
  }

  private List<Entry<Key,Value>> toList(SortedKeyValueIterator<Key,Value> sample) throws IOException {
    ArrayList<Entry<Key,Value>> ret = new ArrayList<>();

    while (sample.hasTop()) {
      ret.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(new Key(sample.getTopKey()), new Value(sample.getTopValue())));
      sample.next();
    }

    return ret;
  }

  private void checkSample(SortedKeyValueIterator<Key,Value> sample, List<Entry<Key,Value>> sampleData) throws IOException {
    checkSample(sample, sampleData, EMPTY_COL_FAMS, false);
  }

  private void checkSample(SortedKeyValueIterator<Key,Value> sample, List<Entry<Key,Value>> sampleData, Collection<ByteSequence> columnFamilies,
      boolean inclusive) throws IOException {

    sample.seek(new Range(), columnFamilies, inclusive);
    Assert.assertEquals(sampleData, toList(sample));

    Random rand = new Random();
    long seed = rand.nextLong();
    rand = new Random(seed);

    // randomly seek sample iterator and verify
    for (int i = 0; i < 33; i++) {
      Key startKey = null;
      boolean startInclusive = false;
      int startIndex = 0;

      Key endKey = null;
      boolean endInclusive = false;
      int endIndex = sampleData.size();

      if (rand.nextBoolean()) {
        startIndex = rand.nextInt(sampleData.size());
        startKey = sampleData.get(startIndex).getKey();
        startInclusive = rand.nextBoolean();
        if (!startInclusive) {
          startIndex++;
        }
      }

      if (startIndex < endIndex && rand.nextBoolean()) {
        endIndex -= rand.nextInt(endIndex - startIndex);
        endKey = sampleData.get(endIndex - 1).getKey();
        endInclusive = rand.nextBoolean();
        if (!endInclusive) {
          endIndex--;
        }
      } else if (startIndex == endIndex) {
        endInclusive = rand.nextBoolean();
      }

      sample.seek(new Range(startKey, startInclusive, endKey, endInclusive), columnFamilies, inclusive);
      Assert.assertEquals("seed: " + seed, sampleData.subList(startIndex, endIndex), toList(sample));
    }
  }

  @Test
  public void testSample() throws IOException {

    int num = 10000;

    for (int sampleBufferSize : new int[] {1 << 10, 1 << 20}) {
      // force sample buffer to flush for smaller data
      RFile.setSampleBufferSize(sampleBufferSize);

      for (int modulus : new int[] {19, 103, 1019}) {
        Hasher dataHasher = Hashing.md5().newHasher();
        List<Entry<Key,Value>> sampleData = new ArrayList<Entry<Key,Value>>();

        ConfigurationCopy sampleConf = new ConfigurationCopy(conf == null ? AccumuloConfiguration.getDefaultConfiguration() : conf);
        sampleConf.set(Property.TABLE_SAMPLER, RowSampler.class.getName());
        sampleConf.set(Property.TABLE_SAMPLER_OPTS + "hasher", "murmur3_32");
        sampleConf.set(Property.TABLE_SAMPLER_OPTS + "modulus", modulus + "");

        Sampler sampler = SamplerFactory.newSampler(SamplerConfigurationImpl.newSamplerConfig(sampleConf), sampleConf);

        TestRFile trf = new TestRFile(sampleConf);

        trf.openWriter();

        for (int i = 0; i < num; i++) {
          add(trf, nk(i, 0), nv(i, 0), dataHasher, sampleData, sampler);
          add(trf, nk(i, 1), nv(i, 1), dataHasher, sampleData, sampler);
        }

        HashCode expectedDataHash = dataHasher.hash();

        trf.closeWriter();

        trf.openReader();

        FileSKVIterator sample = trf.reader.getSample(SamplerConfigurationImpl.newSamplerConfig(sampleConf));

        checkSample(sample, sampleData);

        Assert.assertEquals(expectedDataHash, hash(trf.reader));

        SampleIE ie = new SampleIE(SamplerConfigurationImpl.newSamplerConfig(sampleConf).toSamplerConfiguration());

        for (int i = 0; i < 3; i++) {
          // test opening and closing deep copies a few times.
          trf.reader.closeDeepCopies();

          sample = trf.reader.getSample(SamplerConfigurationImpl.newSamplerConfig(sampleConf));
          SortedKeyValueIterator<Key,Value> sampleDC1 = sample.deepCopy(ie);
          SortedKeyValueIterator<Key,Value> sampleDC2 = sample.deepCopy(ie);
          SortedKeyValueIterator<Key,Value> sampleDC3 = trf.reader.deepCopy(ie);
          SortedKeyValueIterator<Key,Value> allDC1 = sampleDC1.deepCopy(new SampleIE(null));
          SortedKeyValueIterator<Key,Value> allDC2 = sample.deepCopy(new SampleIE(null));

          Assert.assertEquals(expectedDataHash, hash(allDC1));
          Assert.assertEquals(expectedDataHash, hash(allDC2));

          checkSample(sample, sampleData);
          checkSample(sampleDC1, sampleData);
          checkSample(sampleDC2, sampleData);
          checkSample(sampleDC3, sampleData);
        }

        trf.reader.closeDeepCopies();

        trf.closeReader();
      }
    }
  }

  private HashCode hash(SortedKeyValueIterator<Key,Value> iter) throws IOException {
    Hasher dataHasher = Hashing.md5().newHasher();
    iter.seek(new Range(), EMPTY_COL_FAMS, false);
    while (iter.hasTop()) {
      hash(dataHasher, iter.getTopKey(), iter.getTopValue());
      iter.next();
    }

    return dataHasher.hash();
  }

  @Test
  public void testSampleLG() throws IOException {

    int num = 5000;

    for (int sampleBufferSize : new int[] {1 << 10, 1 << 20}) {
      // force sample buffer to flush for smaller data
      RFile.setSampleBufferSize(sampleBufferSize);

      for (int modulus : new int[] {19, 103, 1019}) {
        List<Entry<Key,Value>> sampleDataLG1 = new ArrayList<Entry<Key,Value>>();
        List<Entry<Key,Value>> sampleDataLG2 = new ArrayList<Entry<Key,Value>>();

        ConfigurationCopy sampleConf = new ConfigurationCopy(conf == null ? AccumuloConfiguration.getDefaultConfiguration() : conf);
        sampleConf.set(Property.TABLE_SAMPLER, RowSampler.class.getName());
        sampleConf.set(Property.TABLE_SAMPLER_OPTS + "hasher", "murmur3_32");
        sampleConf.set(Property.TABLE_SAMPLER_OPTS + "modulus", modulus + "");

        Sampler sampler = SamplerFactory.newSampler(SamplerConfigurationImpl.newSamplerConfig(sampleConf), sampleConf);

        TestRFile trf = new TestRFile(sampleConf);

        trf.openWriter(false, 1000);

        trf.writer.startNewLocalityGroup("meta-lg", ncfs("metaA", "metaB"));
        for (int r = 0; r < num; r++) {
          String row = String.format("r%06d", r);
          Key k1 = new Key(row, "metaA", "q9", 7);
          Key k2 = new Key(row, "metaB", "q8", 7);
          Key k3 = new Key(row, "metaB", "qA", 7);

          Value v1 = new Value(("" + r).getBytes());
          Value v2 = new Value(("" + r * 93).getBytes());
          Value v3 = new Value(("" + r * 113).getBytes());

          if (sampler.accept(k1)) {
            sampleDataLG1.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(k1, v1));
            sampleDataLG1.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(k2, v2));
            sampleDataLG1.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(k3, v3));
          }

          trf.writer.append(k1, v1);
          trf.writer.append(k2, v2);
          trf.writer.append(k3, v3);
        }

        trf.writer.startDefaultLocalityGroup();

        for (int r = 0; r < num; r++) {
          String row = String.format("r%06d", r);
          Key k1 = new Key(row, "dataA", "q9", 7);

          Value v1 = new Value(("" + r).getBytes());

          if (sampler.accept(k1)) {
            sampleDataLG2.add(new AbstractMap.SimpleImmutableEntry<Key,Value>(k1, v1));
          }

          trf.writer.append(k1, v1);
        }

        trf.closeWriter();

        Assert.assertTrue(sampleDataLG1.size() > 0);
        Assert.assertTrue(sampleDataLG2.size() > 0);

        trf.openReader(false);
        FileSKVIterator sample = trf.reader.getSample(SamplerConfigurationImpl.newSamplerConfig(sampleConf));

        checkSample(sample, sampleDataLG1, ncfs("metaA", "metaB"), true);
        checkSample(sample, sampleDataLG1, ncfs("metaA"), true);
        checkSample(sample, sampleDataLG1, ncfs("metaB"), true);
        checkSample(sample, sampleDataLG1, ncfs("dataA"), false);

        checkSample(sample, sampleDataLG2, ncfs("metaA", "metaB"), false);
        checkSample(sample, sampleDataLG2, ncfs("dataA"), true);

        ArrayList<Entry<Key,Value>> allSampleData = new ArrayList<Entry<Key,Value>>();
        allSampleData.addAll(sampleDataLG1);
        allSampleData.addAll(sampleDataLG2);

        Collections.sort(allSampleData, new Comparator<Entry<Key,Value>>() {
          @Override
          public int compare(Entry<Key,Value> o1, Entry<Key,Value> o2) {
            return o1.getKey().compareTo(o2.getKey());
          }
        });

        checkSample(sample, allSampleData, ncfs("dataA", "metaA"), true);
        checkSample(sample, allSampleData, EMPTY_COL_FAMS, false);

        trf.closeReader();
      }
    }
  }

  @Test
  public void testEncSample() throws IOException {
    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
    testSample();
    testSampleLG();
    conf = null;
  }

   @Test
   public void testCryptoDoesntLeakSensitive() throws IOException {
     conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/DefaultIteratorEnvironment.java b/core/src/test/java/org/apache/accumulo/core/iterators/DefaultIteratorEnvironment.java
index 316823c01..3c68196e9 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/DefaultIteratorEnvironment.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/DefaultIteratorEnvironment.java
@@ -18,17 +18,16 @@ package org.apache.accumulo.core.iterators;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
 import org.apache.accumulo.core.iterators.system.MapFileIterator;
import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
 
public class DefaultIteratorEnvironment implements IteratorEnvironment {
public class DefaultIteratorEnvironment extends BaseIteratorEnvironment {
 
   AccumuloConfiguration conf;
 
@@ -53,23 +52,7 @@ public class DefaultIteratorEnvironment implements IteratorEnvironment {
   }
 
   @Override
  public IteratorScope getIteratorScope() {
    throw new UnsupportedOperationException();
  public boolean isSamplingEnabled() {
    return false;
   }

  @Override
  public boolean isFullMajorCompaction() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Authorizations getAuthorizations() {
    throw new UnsupportedOperationException();
  }

 }
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/FirstEntryInRowIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/FirstEntryInRowIteratorTest.java
index 74f74626f..5455aa621 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/FirstEntryInRowIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/FirstEntryInRowIteratorTest.java
@@ -22,14 +22,12 @@ import java.io.IOException;
 import java.util.Collections;
 import java.util.TreeMap;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.PartialKey;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
 import org.apache.accumulo.core.iterators.system.CountingIterator;
import org.apache.accumulo.core.security.Authorizations;
 import org.junit.Test;
 
 public class FirstEntryInRowIteratorTest {
@@ -39,38 +37,7 @@ public class FirstEntryInRowIteratorTest {
     org.apache.accumulo.core.iterators.SortedMapIterator source = new SortedMapIterator(sourceMap);
     CountingIterator counter = new CountingIterator(source);
     FirstEntryInRowIterator feiri = new FirstEntryInRowIterator();
    IteratorEnvironment env = new IteratorEnvironment() {

      @Override
      public AccumuloConfiguration getConfig() {
        return null;
      }

      @Override
      public IteratorScope getIteratorScope() {
        return null;
      }

      @Override
      public boolean isFullMajorCompaction() {
        return false;
      }

      @Override
      public void registerSideChannel(SortedKeyValueIterator<Key,Value> arg0) {

      }

      @Override
      public Authorizations getAuthorizations() {
        return null;
      }

      @Override
      public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String arg0) throws IOException {
        return null;
      }
    };
    IteratorEnvironment env = new BaseIteratorEnvironment();
 
     feiri.init(counter, Collections.singletonMap(FirstEntryInRowIterator.NUM_SCANS_STRING_NAME, Integer.toString(numScans)), env);
 
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
new file mode 100644
index 000000000..7557b9a15
-- /dev/null
++ b/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
@@ -0,0 +1,46 @@
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
package org.apache.accumulo.core.iterators;

import java.util.TreeMap;

import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
import org.junit.Test;

public class SortedMapIteratorTest {

  @Test(expected = SampleNotPresentException.class)
  public void testSampleNotPresent() {
    SortedMapIterator smi = new SortedMapIterator(new TreeMap<Key,Value>());
    smi.deepCopy(new BaseIteratorEnvironment() {
      @Override
      public boolean isSamplingEnabled() {
        return true;
      }

      @Override
      public SamplerConfiguration getSamplerConfiguration() {
        return new SamplerConfiguration(RowSampler.class.getName());
      }
    });
  }
}
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/user/RowDeletingIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/user/RowDeletingIteratorTest.java
index a3c1ccac5..bdaf112ac 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/user/RowDeletingIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/user/RowDeletingIteratorTest.java
@@ -16,30 +16,26 @@
  */
 package org.apache.accumulo.core.iterators.user;
 
import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.TreeMap;
 
import junit.framework.TestCase;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.SortedMapIterator;
 import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
import org.apache.accumulo.core.security.Authorizations;
 import org.apache.hadoop.io.Text;
 
import junit.framework.TestCase;

 public class RowDeletingIteratorTest extends TestCase {
 
  public static class TestIE implements IteratorEnvironment {
  public static class TestIE extends BaseIteratorEnvironment {
 
     private IteratorScope scope;
     private boolean fmc;
@@ -49,11 +45,6 @@ public class RowDeletingIteratorTest extends TestCase {
       this.fmc = fmc;
     }
 
    @Override
    public AccumuloConfiguration getConfig() {
      return null;
    }

     @Override
     public IteratorScope getIteratorScope() {
       return scope;
@@ -63,19 +54,6 @@ public class RowDeletingIteratorTest extends TestCase {
     public boolean isFullMajorCompaction() {
       return fmc;
     }

    @Override
    public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
      return null;
    }

    @Override
    public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {}

    @Override
    public Authorizations getAuthorizations() {
      return null;
    }
   }
 
   Key nk(String row, String cf, String cq, long time) {
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/user/RowEncodingIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/user/RowEncodingIteratorTest.java
index 8f228f537..d9aa174ae 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/user/RowEncodingIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/user/RowEncodingIteratorTest.java
@@ -16,26 +16,15 @@
  */
 package org.apache.accumulo.core.iterators.user;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.SortedMapIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.collections.BufferOverflowException;
import org.apache.hadoop.io.Text;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
@@ -43,23 +32,20 @@ import java.util.Map;
 import java.util.SortedMap;
 import java.util.TreeMap;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.SortedMapIterator;
import org.apache.commons.collections.BufferOverflowException;
import org.apache.hadoop.io.Text;
import org.junit.Test;
 
 public class RowEncodingIteratorTest {
 
  private static final class DummyIteratorEnv implements IteratorEnvironment {
    @Override
    public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
      return null;
    }

    @Override
    public AccumuloConfiguration getConfig() {
      return null;
    }

  private static final class DummyIteratorEnv extends BaseIteratorEnvironment {
     @Override
     public IteratorUtil.IteratorScope getIteratorScope() {
       return IteratorUtil.IteratorScope.scan;
@@ -69,16 +55,6 @@ public class RowEncodingIteratorTest {
     public boolean isFullMajorCompaction() {
       return false;
     }

    @Override
    public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {

    }

    @Override
    public Authorizations getAuthorizations() {
      return null;
    }
   }
 
   private static final class RowEncodingIteratorImpl extends RowEncodingIterator {
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/user/TransformingIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/user/TransformingIteratorTest.java
index 1f4d6e763..97ebe5c7d 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/user/TransformingIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/user/TransformingIteratorTest.java
@@ -34,7 +34,7 @@ import java.util.SortedMap;
 import java.util.TreeMap;
 
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
@@ -599,7 +599,7 @@ public class TransformingIteratorTest {
   public static class ColFamReversingCompactionKeyTransformingIterator extends ColFamReversingKeyTransformingIterator {
     @Override
     public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
      env = new MajCIteratorEnvironmentAdapter(env);
      env = new MajCIteratorEnvironmentAdapter();
       super.init(source, options, env);
     }
   }
@@ -639,7 +639,7 @@ public class TransformingIteratorTest {
   public static class IllegalVisCompactionKeyTransformingIterator extends IllegalVisKeyTransformingIterator {
     @Override
     public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
      env = new MajCIteratorEnvironmentAdapter(env);
      env = new MajCIteratorEnvironmentAdapter();
       super.init(source, options, env);
     }
   }
@@ -665,7 +665,7 @@ public class TransformingIteratorTest {
   public static class BadVisCompactionKeyTransformingIterator extends BadVisKeyTransformingIterator {
     @Override
     public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
      env = new MajCIteratorEnvironmentAdapter(env);
      env = new MajCIteratorEnvironmentAdapter();
       super.init(source, options, env);
     }
   }
@@ -711,41 +711,10 @@ public class TransformingIteratorTest {
     }
   }
 
  private static class MajCIteratorEnvironmentAdapter implements IteratorEnvironment {
    private IteratorEnvironment delegate;

    public MajCIteratorEnvironmentAdapter(IteratorEnvironment delegate) {
      this.delegate = delegate;
    }

    @Override
    public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
      return delegate.reserveMapFileReader(mapFileName);
    }

    @Override
    public AccumuloConfiguration getConfig() {
      return delegate.getConfig();
    }

  private static class MajCIteratorEnvironmentAdapter extends BaseIteratorEnvironment {
     @Override
     public IteratorScope getIteratorScope() {
       return IteratorScope.majc;
     }

    @Override
    public boolean isFullMajorCompaction() {
      return delegate.isFullMajorCompaction();
    }

    @Override
    public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {
      delegate.registerSideChannel(iter);
    }

    @Override
    public Authorizations getAuthorizations() {
      return null;
    }
   }
 }
diff --git a/core/src/test/resources/org/apache/accumulo/core/file/rfile/ver_7.rf b/core/src/test/resources/org/apache/accumulo/core/file/rfile/ver_7.rf
new file mode 100644
index 0000000000000000000000000000000000000000..7d2c9f760a09fca78b6abceba30759480136f2f0
GIT binary patch
literal 14557
zcmdse2{e@b+rNFOAxrg)eT|ATWGDL;*($OmWM9T4`@V)smK3sPeT2z2hO!PqjV0Sy
z#*#<2m?R-4y!WW2?e{+KIsfxN|M%aV`+UFm+|KE~?(4cf%XQO3-)A3RIf#^+P*k}0
zY*NoV&EOSUzgdAU+|uDO$;P6J=$NlsGs>VoRq~3pLZCJA)yedH50kBcloA%X@wO=$
zvnN3%l4dU5n1FhJFSEf|bDKHW+IO>6auITYZ_TY^M|z~ob3Bn<i^Tzs0WakS<f`QC
zGM&s#%*%x;OW-BLgCkX#szp<r--6%TR$$;MVY$e*qEoT6I|*9hM`fOoA%=#J6+EXE
z5_Tx-)9Nr;{KRnQs7ymk;d3HQXOrkhEl;Ku6u1?ru9Hy_#}IKf(sPnYl7thf2h+W#
zq9b7?Rto0%a*>jRF+}MRj~<mf*@2K41+G{0$H}BgZYrohqMM^SOu`m|73zDJ5JHGw
zQKuPlIDC><BkqYGecN(}mTsSF-n&WGBNDGgu7FAf#_emF7Y4EZeVeYQf?)DGRd}Pk
z440*DgZ%EKC5M#6E$_668MHJ^e#&pd1oKpGcIxvp#D`3i7yX*@BjwH~O@+j?3?A1l
z8hbvdOG?apjJO&{esb=naY55nZmj|pA@xM%=kzIbBmXWi(vVxfiRR}}X~Q+b>69du
zsJK|U!s%k@l&I9HxWakD=|~!GD&Oz(PBZJ(a%qNu!YzjtP3+R|M14?B>vN}ng#XB@
zsQW6TGLO|rxmAER(0&Ewu({d3Hh{Z<A1x6~!fyFYe+<|>cjB%Gp6%}}+3vR0fPy~G
zAbhc4<q@2!G2+NalB2RuU^z294)4yn`SI3K-9cRNJt3I);AzrB8yA~-8%tvoV?SeK
zj3M%B)FXPuTYHBLi8M)6a1;?YCwDMa6cr~4_is-d>ALt|L@LR|4!JB@m|i)^fV|^`
zYFb+~c~x3heN@)D<Ow-FHq{B$;^}d!V90PV^bWKdxx=mmc7=Q(;h|N#YLWYyRPA*l
zH}DW%$+(ax(uGeN$8|{0aGx=#E9s--jGt<cRH!Gmt7KAmcfn?0bKu40){=|xN-_To
zp@4x+qrt(#X}cXW&0+ZuIB&2dEpFXorJjYd)(aMYDpwVD5qWN;ADaeE6kkVLo93RX
zgAW@zIKfPj+9RGafs1-OJdSGp+e77I4sZ$UFwwA(#zqTH2n0+80#T5V?mNwZoChBO
z(L|6ahh;UsvN#SA{q1Qp|9Rf?ROi8eJOqP?J|}w+q6I2lflftVCce>lmivsA*T5&v
zVq=wShi0RyiS5W+72O|u4s5>dOihd(yr4Pe{j~0`fo9F<I7iX>%-JM|l&*%2z<><A
z$ly}jSc49FbbHO-4{0_M*dUjl{kiQpQ%-bS3S6@&r_x#5gaz7()HGGcmP31yJ=~@m
zcY5MzdLhge`YEXJ#&F<hAr1@wW9a*ygrr3><S>{SL>@t*9Fm37v#|gDDdl<H<kOHb
zi)W4QVe>djai52GcB@>6R2Tp^UA4y2F4bqg?4Icn<P3h2>Mb;q0z=fyNq(0xCFA}#
zOzOuwK+nx5<VQq`7FNmx1G+OVq&U@^ndf^ve2@1okx#POggH5x!cr0N%DO7Ek@1lp
zEEmrH)OupxG)5DqYT|IWPM;YzIC~HmjbL}c(bXw3o2-mqF$vCUgf!||fPwshfIFuE
za!3jULmWZUd|*YiCSAlMqw<|%D^0H&lSGpE%W62VsnQ-fK84g`u^FXx$cZ*h1in53
zY*_Nf8bHz@G74e}k}x9D5XIn-M$SeB3p${Fs33G;GVmMub6XF=gj5UbkSfxt^cb6l
zdZ@g<)8Rg_=&P%2af`Lf(v`I9Kt`o%(#aysW&V;5^QB6R{lfgFFeV`LNoK48dVLvw
z|3>XBcEjUY$3=X=Xi{dg>~ptRqzyI~TD5}adK(m-BK+3dc{l|jZeo4VL72H*ky*`3
zTyx6)tZ5QP-L(8vZ(<L|xkxmOI)oZHZn05NlZJ$#6f7)&n*;;*Px0H+(q@R|pNf2c
z3wR}GK+G(jq1;2}Df*@k5q(H5+AVIECZM=`%6P~tX1J$NPaHG(4)U(OVLdL<PV-c`
zi6~pE5{MXf^<jO3BOD6F_dPAak7u6lHb8fK3*`BauB?`_#PmUS&Mr+YNz&jd{dNNx
z2LeG<5*qbmtMmQVaY8ANBb`$bINwjK`V;hGJdQzW6`j2^<0eSH&fhmOP|abKAjbc$
zkqI%rCuXE(Bw?iZ<MEj?@Jh;MLS>_&q{0Ax<CxE3>?E4F)}K4xfW-M-h>WMbpjW8L
zi#3YzW|^cE;<%Jw;zi#RAY(2+Qi%88@)tZYF$Fs@eQ5>vL42_<xS`E&hv{#{%o?%S
znf}k7K5S+&a|}IsY~0KRSz>qd`w?PyZ2OBVrAE#}U7)v+dZs3(Iq-fA25HYyNj60`
zMcqo>D$s#`@h0x^N`Z%$2;U<NI{A3z)z+4RRS~{3z{WZY_@=numnq?{iI7$K#RZ%5
zX(L-`P74Ky280vL3#J1xfT$>_DagXeNJAAviBX)WV2ik~mq#r{E$&;0qK>2NL-n^P
z%}XCH6SDmkxQ8pPF~Wa6OfMX7bD)ZAUzUo4y}s`>0y6rB-1*%X;YZ*BX?w`+RGV~R
zoPEPn7L**ZG3znqmlm;B<p-3n)ff2ILaL7RVpE_GkZZdoUxyp^bim_2$z^Y=_eU3O
zxps+brTRGyr~F?)?&1br{)fK1#r*}ju^O+7-Qt&r-W5t|AB``M6ezvBi`=*7F5n7R
z2T~<EIFc^Bd^Y7?w`!ny)@*{K2xQ(j3jqlQZ3G6gNX4SNr)JhgV^^ON0zi1mH|XeZ
zTiuV=-<-M!jYobotT1q>tP;bXgyNuR<T<P_wgT6?_NC?l_y+Nj@evMMFz+9mcwdX7
zh?~Rz6LEd(X;}-CCeaWFiaxS@rt?J&ktb5(zl7WZ0mzD<hqHK=+w6f3Yi2~e8o*Ux
z0)UB=R*%*=2CUBHO>aH#z%<P+IGNgd)Qu(O#ctfYAU)tOBd@(6GPw{~lDQ(X>{T^W
z_f!-HU4SMaE0OC+EvzZJ&c@+sWlojyNRP=pTtlF0rTInYwqyEE<-4_8<D4|`G6_up
ze+>cHO(g)aIIzK<hTeBY6YyCXz-Pl;gNJQQY=44cDiFzcq(&VA$$SNUBIOXvUpQd~
z)))KG@_V9t%62JwYQE65_O+lOcUh#H%4l!}$&|`hiWWf6n!=AjWy~hp3M<o}aI71k
z2S1x=`fQ>Bh;}#5U!4s!ub;JXj6FM&0Zl?qbalJ+?|{Xu`j@M1P4nPiDEd_SO$3-`
zn3E<)m?h5@kpb6Q$bk-vJA6YQN)W@3iew)x@6#)<gDso>NXz?3n;ml2a+jS+`e^d2
z%=NP$mVJ#_k1h<5-*lKU*V{>=w{U%>3P7F<fXcpEe^fZpi+wAlQ=%iY1YU_V%A6R-
zJK~<ny(^+I??XdChX(zn&fcB%xf1wpeZw!MZ56hTG{6=^>u>{}PJS@;%I1+CmO>P>
zua%gI6|9iY=~)^(KVm1!kRC37df=#&7R;$~zyy$BbbtgS10)zRX_(@^Qwi`<K!On~
zhgkv=3=Bvxir=12(x{R1gy}c7TO5XnfXSmlYQ+0b-yhuKNHHTsD|~@qHY}x>8XvpS
z<6f4-`*PdQ`%0=uuw2*CEWOu=b*_SK`*bF?#$N35aYRr0&kNYGcP*_y=$n0?eqjW*
z;p@+Wac%70y4v75)!6n9uIkh~vihmKL~3UMx;+S8*Hs}<l|4XJnh0+AM{(8ET<y96
zdjk_3G=_j&rrD=@Gi)5M=AA|NkLE%}N@U3XRWlPF!hh0VUlZ*ec7pgKSw8qYeeyNP
z&4!!YDKh(#oC_qmu3hVz%nJu2PN1kSjRu>#a?Cl=TlviPlnnbfe_ZZ0nR+JMP`^4b
z;!!tonMJN`$r8-|WaCC4^Podo(7W|bkCfJF*m{IxX~ekEUCdBw;foYsY#|hb9A9&+
z0km1ph$XH%jYHRdhaS#t5|mcdiJ2Ksbi7j;9D?EmBnTBCK}aYl!uFlIKm-AWNg6@X
z9Gr!M016ZI`%{+a18NjG!SSf$7S@mhV5Vr2{io1|^b09LglJLFW(AOn{tGpRjYYL;
z0SBpz+O!lmbl*aH6>n?YyoC{O5r!%IzyA<1zcAK;nfV*7Zf?8kzELa0Qr|XV363$X
z&zMj<Z?#b~@k#2uRlwpn_-?QF*#`E}wYj4`aQQ@gRRFWpih3PkZV2O%Q7i)iypd`^
zW7k#MYnv=|<~&7^O_2cHOx_9u0;U28mJA?R5`bVs6hi^k2#B#bK&)s0f{gd;WeK3P
z$s?!%T}lQ(R45=@0kNG7$ky|KY~=%FE92M8G&9pf&Pl^FSF5&o`-(OE5*-Y8h+$!=
ziG<L#TuAiklzRqMxYLDMd^}gZ#F$iGvSLDph@G@A(CH`Ioi-bIgINAB>}M`5UtjOG
zdS!O@ElzyY#0eI(;p@wBjrILX?NlI`a(l`7V&%D!`_Lyq)mEGuIO?E<tQ<KB*a`zc
zhz<j}Mr`>?v%fN@ujBTW%Vy5yVkrX5#UDlHA)t}}I}ZWW@FXeUfv+_8jqZM2G3G98
z53_e$`K!d{K}oNv+S=7P*8c5k@q(H@t@$5{v?IyiC3a?#iNIiB?RaZpC0hvxAh8EO
zUe)y3K(F^<-8254o^{P^IJDBA;fFYd<%8B_UOlA0W7Qw6-?mH2(HOufLJ<T~X8RSe
z&m_|&Lx5w*GVVWW>u<Cy^Y3Kn^~ibZKJt8)^M56~K<c(gvM;1meQje1-Ob6|BZM{{
zIw!j{7HsOSf9q^o4k2Xc+yBfC*Ae=-?}Lw6#XlRXYvYN=W*o6LeS@FHwfmmV;HPb_
zCP{6{zFWm3IyQo;kgI^oFv0p^^KkG7<F^sLDeE)6wPGJB9P9~pYAI=!aWSft-4Z9f
znwXo1xdAga`Ni{mCt7*1=RSA!TcZ79zK+byFM783`}9AfWUAu*M(PFx({&EudQXDs
ziU&;Bh7j^mD%F=<H|qg5Qbyx-zm)Rk*h4!RIqlmTAFszmv0OKNEk!Sr;$ql<+=!T8
zVWxK~Zo~SS(@T!au?s|$EX#tL(dPA;<E?1XneM52e~x_m?lG((AVtP>OCqCplJ$ji
z<ZUbOC)Q;#=VnP5=7_Fg3W4E3)zc9{=sW0Rq=u=lX%QSyU*vo=z~Kcx{I39;wi)Q*
zKaUN*sjpE>6V#siY8G-vKOFp;>2ItI05&uBJ78BRnctEAM#|<exEOR{0k3WjHyfM{
zsGJ1kw59j68T7o3Z1>a$zlLu4KG`QbHC6BtTj&DKfCWum88e`*3=+gvk(aS>Tpim0
ztyA0dN7<~i+$O4s?dnUW#lXm9z|iR@BHnj8YGL$8BHmBa65u006Y)M??&V<RO}Pog
z+Kbi(ymI)YnQ3r<C-+!I?Bm-JVaMLlMIPhk6jusi!;U<eH@zFUI^75QyTt2EQDsI6
z2lU~CmDQCp=TF;g5tG#e*lB2s!^eg;7Wv3Nql_9cwH(p$(<PKc?3VPFxK}xV_Jpel
z^lCa?L;$m)5msO}RE1OlW<zjz4=@{Y5;J*__!W&uEx|v*?H|xsA~}iY3kr|DXuM(~
zL@Lnh%QVCbPYUTIY`B399M~8=z@QsBffIeea(OpaH+qM<hAfj!O>+p3r%n<w{WW@H
z9@Gub-4U3*B&Tbnq#!-<!#I{|s7lgXL%4as#>nU8UcU*-4D*Q_tAS=q>jT(XFDKlj
ziO}rdw^l?0-$^v+tb{kDpDMhGXh&WGVjIRd76GtDD0CZa5#S@7)hm@pT&3hQUhVMs
zs($om9f&~0(+qI_2iMF3I{2?#GX*30A00EnO=E?sM^Xw1(UuX6RQRM@T;t;v?%4{j
zna`q!<L4%&xb-i-FK*mx<d>h9O`LEn-GfzSj?mc!e89km4Ff=47geFr{j&}mPbaqU
zqPn;;^<9UAo!&USDzDZF_K-b2eAEGLf66ogqh?wG@5S^GW(810`_bNOqQDR#8W<wj
zLbxE7z~S1+Y5Cj1-%m1|7R>jrn2WfNG5_FsiQOrfFP?nqZfZTDy0yMez_=}p03VeR
zY4-OEZU#T2>$k6jRD`BKn!mC1a&sV6S7&8#pu@3qsVcU<V`S6+wARFlJhwaRXe^?G
zF)(v!dRA0-BWNAzOAr~*ad8JzSPfweX1Z*c`(V7V>Lh$k4R*g3KfS6V!`;aP=V)Td
z&8k{FrFHP9q1QfG9<sRFg;37#%t4-Qf5i#CpJn5a1A+p;`wFmKAZP-x0iYNJ*I==+
z2f$f52ho-k(!77(^&R_n$6<;aq|-$y&4fG?Bp9GW9*_=w7{0qm`}CfH`hj+o!aE%{
z7p_WaW2tt!TG#i&Ntb6zVoeq>m=&B`vB{KwO@ra7rM&x96Q8Qn@)u+)XBX-NIc81(
zgHFQucCDKA8#P;@F}amL%r%+S7lxl9<VFZaIvNR>ET*pm*+q!Pue05sQcRc-u|-h-
z8-ZQj{4so<;GhVpW|xl@OVK673ygLxK;b>|s-dOtRMSKsy6QBO5P13W>tekss#seI
z3_ol<xnZ39pP|9Q^;i@7s?`l6;cL;$?so<O$M}#SL(J=SR}kAcM^j+_Xb4>bY;sW+
zkVWPiedZ#Ys{4rpF)opaDc?Xju5F%do^19cmph2OkrC(v+dpa#6k7z%zlq8_@<qhC
zkaJ;wJhn+>AEtaJL@GnuqySRY*NwBdpl8Fw-n|Zw;AS{m%wuW+4?YqOckh<I;dVj}
z))Cz^yZYpg@yu$OP`&_tdVTe&B*-<5r7iZ76BHXzv1Akza#}WIhf17m!AeA{>t0Io
ztuy*)+hJov0o&)s`bD+_#s*HdoyHdzY{!PVKv%%5O^r8+<q8s{hrH9YJAt-R1gZAu
za1k@v^5b-e(+40?Ev!d-7p(tMcuMDSc6lH*4Y#@iEvb<kw;NtvUao@9_6;JTNn5pb
zSKj#LM)zy7IXn>W6$r2{S>(wPQM0v)kv$n3zy1>M<L6QyyB)A|HGy2*_*#OhxUp@*
zO>tw#gy-VME(xI?O$UsK1uV#mgO8Mw7&aa-I*6oZu^@F0rYR*BZ=@I^VzCey;$IAA
zD<$=8q#Yubv7lTGu6r#vNn1_&E?DOE-treKBW)wGBoXCnBGzEi>qNAT`qy<NiTPiX
zr3EuyC(&u7mL#@AwS^yQjQmWLrqloo-n6@AfY(NJs`}3(5~@GyNOG8@4CF#y%Na+#
zbe3Sc7wR)GM|-I@XL@C}=uO4^>hhw&;^rrIgT$@RHG*h|zOA)o+n_plW$YTp!yOvH
z5df=;xoQf_slo_LuVL{gH084qheS+F7VJTk5v0u_la1FcBq6L){!&`=XxvsxmzB$`
zaNA1qf=|bW>`Wj#V_e(eH^S6DA2XA}IC-h4l|tk877hM6MLmZ5WXBZG?JWq#MNTla
zT4=ll@7{u;4x~#X<42vl&ak&Eh5OQ-q<#>tsdOysp5D0v-u_jNY`%tTzyo;IsZFX6
zQk@Km$`tIo6<}`bT&2KBn9wNoioEGG`loo22PWs^v$G+3#(c#epOQK(IZ+?mnTVga
zGEVH9@^7d;JFrCW(NHS~^X#`>dBY~B%$6c7-5TxG=FvJ*)-@!JWa^<mQyvx$<yT=0
z4d>+2pkmlthNy?bdAZJMFx>mTcqxfxX&h&`9nQVC6i`l1^U8o?H(+rn&M-xaFc1Y3
zNCa#RX}5^O!QS&pQtN~!yyo3o>KXgV;yD>qLN(~9xfOY%$l@QRLs9_X;stFHI|MaW
z0)cl)xisqmjrUwFzgH&fdAEbu2~f4}(d<+Fn+zeNx0gaYjpYLCRjduR{AFhuh6mT5
zS`VLYK$l-xU-$G2Vq&w3@E8Iuz26Ov$L^9kw5kdnR}s-MST`kSg)_3Vr=$)GS8U=0
zoQwvT5|y$qMY{<bQvCJ41L?A<6_w)Xc*(vmtl`c|_u_bYsn301D6>-Zcn?#nD#h<D
z{W{)tg=rC6+bowX^g<NLDUN`5H^;V#93)UkLclUQ%EE_L^%;3NZEUYil$>D(6h9<R
zf_a9rY~qxs7gDEUq^;X-$qZjBSC8?1w=jjjQpc7wff-p?k#kuRT5mY>t__D1*)+~6
zak><J<N}ufLg}*6P5Ao*=JZF(xCL%JFXps#4n3@>LB|kI#<jP^X}{t=POTYw?-B3b
z@{HP$j0dFwVTk^|h@<Z8%i(TDKk=#1?-k3}(isXD{Qe}qK9^L;9mu(4-t!xj=U<_O
z31$65U(c-A-zqw1*{JZ+nrq|+w<hT)L;t_}Sg&t>-V&*}@?q0ErV_U>HESde{~LG%
z-i7t43#gr6qPlB;1lm&fkKDB<$?Aef&{0==2C?|ZSpBiq7Xtn&ms+BVIkOU8otxuz
zXON~2;JMOO-dtKIw7)pNRIBq2h0qW6Nr+8`>-G<M2D09w)6V@vo`L9|GG6+nlwaL`
zrD4x2%D_y0H(X7LC#!QODHsL3CewZeyhPEbQs}t__pfy)eN61#Of+C^c><qHSC#gu
zlXK(A>)W05=(eS#sN#$y?E0um>-3tNbNQ%lCb~S0V-~-<mA4&JfM3NSUg)QGBzVq&
z7D}0NtU8QjFihi<XOJ;aHTRb2MNUgBb<a<o%*o}cB>qz;pKDLo)BdrOk2Abg^gdg|
z8qwy@e8u8=2#TEIFYx!xnuI;$j}UjCY>XQ7y+*!PjAxVmZf7lW_+_DBgyOD)61v$^
zN^M&cri6`6dLw#*Bv+TB=LXa9o&>5-mue}U>!)$E%^jZugLXcrpq))8+RAOtZk<No
z7$0AlUKQPr0iUUDwkuFRlg(pmt=?4-V<IG^+Z@9zZax^Z5N+B9ZLRy>(7e&<ueJ9f
z>@ME=^A~)Dn?3B2;Y#^Umid0oa^Vl%Oz3TB_-w@@#ChHFN6cG3<Q3U;eP2IEYAC$y
zXsI{1BPf5%!<S$$gSTADYAQTZ2F4rYeV2M>aT`w!mwf#K(~GL6KJ=N?Y)`ap;XZsi
zJLNiRZmuTp^wHz)?J}j4);A634iuFhxjHG3&24C{`Xu=XZ?twC_m4b8MO{v9@&gMI
z5+;GV{R_bHdDHJP&{sa12tWQ4yv5O_#;N{*{{S1@V6>&@qfQ7v5oCNHHMiT&IxPwS
zO&{V3oLtPUKke0M0=yox<f^c_jy=|Schk(^Mn4+6^mkNoYC}*!_p$PlOt%*|8vF&(
zH^#cxu?xqxyP}we<IiAXJk&h;qv2Mm07Y%BeGq+9%(arsCfzLU9N|R(YF^+S0f5_O
zj2R`z{<aL_h+qI~S{x6_BKy9%H^qYZ3C%P^*~y%KU||BpQ=(FT!864uzJ1ufi}bE^
z<@>Rn*LsVzcox-5h#Gk74RCW$2$jam#g;YQ*6yOLD3wD8sr_t7<K(SwnGRoeiK%66
z81ED;ksgzqu@%>;>JF%w#@iVuHc$D=HvrVk?jheGSAraLBv3Pt^tIMG@R`$WNlb*A
zuCz*zXOmK9<1J?4O8tpqc;Be$G43N!LPd-Jo|!qZG{0qL=<k@B{#S-`4)56|$`%ge
zislo9%GTB)H1nha`F%^Tf5f}xy~IFq$<0bHMU42~97~ksnc&f_pZ>gRYg_rGWQoJ=
zA#`EI2<g8AWP7B39gS}OboN@ud)iETHenu_8$iNA9&>>{fvleWr5n#Yuh@-ByPUz$
z?k4&RL3@Tu{GgYBYJHzhr@c?8>~haFerv{kLS<LBj8YHm6DnW8&NDP+4*&?_gOdS4
zpP>Vg!&uOd<bakBwQd&J*UO{~aUy5RmIh&`HBNBnMTS^^?`63lm%8<wZuC|e(92JQ
zGK;E8FeBKVjoLt<0_<437w`*nc6aNa=nETa+$?@|z|8UpvPr;S^-|02;`SpIq2XoJ
z7xt0z7m5~Fyt9v#cPV;~&xn2>DenR<mh4ylN~Kf%<@qp{Q|(hJyWH&3B-q4jc@Hvu
zNJ_AYo)BscO)%jXwMZFaMN*TAxFl%E8P0<4JqWpYrMgCU@Uw_qWFfaupO5R~?S&^=
z$D%aWGmn8fHw}whY=@$(HCkMTZaKG;fsSXvvJCXDR#@_7b1~dO@i*>$h&a5}A;iG{
z^gr`78~^(~%_FXc4?x`iV^8z>{})e_PuySyJb+ib_@UOL(4reTc55r|7VcCjHN^t}
zLCS6Bv;<w%*B<bzXN(8Qb#@HajyAN~de)3Kba&<lY$xYC1g5PoU@vZE=;K#ceJU!%
zS@@UU)4zQcm1o7|x=?)=kV5pPZQ|V0*Hk>?O4-8gf8jIA-(aa?0pE{u{V??^XKhD$
z-}~g9?^foUw;5ihfc>HuDHXN9nVGNPB2A^mc$5M7Y!O2(8h0iBo~NNm8mB%Lr~`;z
z&VX_{s8ml9)f>Y%&z-tHEU%p4E_#dG5R;F0^$X0*6vR$@87D5t!PeK4dW3C_B0KjZ
zr)N+NNXn@_jh!u)$*E0RS!RzS6`Lffeidf4zd_QlUy<}5&R>yKmg+Z1+O%LWExNcq
z@rWes0O%y7pz+IQnG*UJ3V4OHXKE6D5J8+U(T+o<-%JgDU7cWR&dLwI9dqd}aa;Ag
z5@_NyxPz~5sFMxs_GVolME}1HoqDAx5kg%9riL&p`ZBEe<YVPvn9vj)a{q-AVp@fV
z{yn_?pg3ikTu?)YU{bAIhSizA)nzy5{{yC`GwDaqO(;}k!*A@GP89dg@}}%Jr0|aH
z$gV5@&XV^02=4EOCLZuNEApupC?-PGXu|7ff>D>d`Yfbh;X<~CVZzOj#RR4P8`<$x
zpPtN^U3Y`@5nPR6MpfIo5x)LxVArue&yBSF`Dd9`c)ZqDx;}ObSN_ttC>k&|R12l_
zZf&yH=8e?6BV!2--9yg<3@BDPe;rUrQvEuhIP@DCl>Xk=Ly88E7p-o5O%iqxbQ<z~
z!#eRudv9d()!lsLG-CXjWo4+`7;PDV51uk86tyeyYY47JZE^wZQ~i+-@qAYF1n8To
zc>tK2H>e5!sj1nED$d5IV@oz}%}p-{_={lrmcUcBoeQg5xk2i?77p-kd#0uYCUjE$
zamPHyHPv;gy4y%vWAEPF`G37BEcN(L!z6x`my+lYQyf4XMWx4S|DxjgFEQ?);t<5q
z2vvz)5+H>B{>k|L!{KMw<hrk+|D~(({9oy6%-m7)gvtisS^=b>FQs-gsh+xuUQD&?
zUX#M+8_s7pvYF`Lm$V-eKXgSdutCLo?DN_u_Ee2gcKKO8bl}F7#A)BObv&M6Yv|$G
zcZ~!v^v3Z#$lFkrk~#bqE)Q*N)_P9BJ=|c3s$}1cVvDo(>P4Mq;mnA`;0qQSAzb9&
zH}^LvPW>>VA*jIw3)LS+G=Lg(W~in5aoVO6s;hMPmx4HFueyIm5&!A~M`;@h_*H#v
z!f+pu_dN>ah~{@Ko++Pv#6Ty7BX8cXD<^K({YKAS=1aDTcTTUzmOUIhz20qAF^{)P
z$?x+FxH6IPc0J1Xih1T`8OKW3540SB&cF69Ssjq|Jc%(jEhA+y?J=xbv<#Tv7Ui!V
z{WsGzHE*dGw9Vo>G+(ET->8`hRK7*?FFOJvVaW%4V&u&dnokl+yABw&(y308qb(xe
zSudtfI8q5pMJk<Vx=)QCk7>DO8{_if)ZZnC=wEo8o#2>a>{;1z+p*r*Hcp%_g?FRz
zcedYU@f0@`Kr1Q+3^=!J>9;;Ueg?x@Hs=957X^$cT1FJV=P<!S`!~t^X;ex`-ml$!
zuKkx0h1741j(F3uUO5XA5Btd>e<F=gZWi3v&HvKZc>ZtnHD?brF%UxC?`tHv6kM1O
z@cM$ozx$f;n!@akKGz$MXXdNlUZCdg-I`^L;A=SY&<J#}jM?zo8D|yY!z2{4UyMx;
z-7UcEIKQX<{%NH25rmL<r`08zY8=)Fx7lG{lAyJwZP1824<1&^xOsXVeZfvrhP-;4
zibR+}1QFHLl$7CLN;Mi86-DP;n3r@KeD<(Lh=w~7-d~JRQHrX~Gacui$GTz8+IiM5
zms#!{@>{Fg*;q^4S?@fG5fwdt``!V~kimy0RZT*a_Ntwa9WO8TelmIK<<nDFT5!7C
zU0Ue9>g9j>QdX8999<JTW*-0Kro^#FR*(2zk?Lh;7wFw73@cO|RM1tNc=X6!kE!6O
zJ6k_3a|C`icrBzW#3f8xQD4>g0+BS)Gpc8#zBDNed2cBU-=d8SB1M`sc~9zP@|V#w
zj4&&cPJ*P_n6ye=sv?HPTZgt_7n-$f5<fxK?4#jPxjkj=LMY~flGb4<=DVMDN>f-?
zwe1$M`qDfV>>Y!`O+#MF?iXxnItG_-mo53t>ps3}rW~*&f-FktQg6|pWACrJRW`=v
zer0Vnr=A~-?Xw-B%+9L!1LG&z)9)Dzy<M<&h7~z*L~*Pb{LO?HwwIQOjoBz1i{5f)
zD(6{4O{oTO2(P(?y%=0j3NQs@z3y$t``1aoHSHeSJ}E7T-_)DXYz=%YiYw{9xp`E0
z&50@ffpO@jG+0JqF1%xtZsIkOva-|dxu{DeA*-hErR)Z6mAb-r_ebq2`YeM~yIG7H
z%x>odanWXz$zxp9B~#AOo?v_uSd6{G+Q}A5aZj&!*zksEN#u6a`2+n+RbBjI5i>W_
za0sc<cD|aUnKgy7Evu@L)fuuamtQFRPimi-lPqBBr(h2EYqUEFa#|IgqnIPNKTP3y
znku4ls<=9Rvem`w%#6Pz;t5IXgHMlLgil&a(uEz0z$8T)IMh34uUjV%uu@Gg-3-<Y
zdwx?&CgZwy7%}yc$Q+jBikz5c^5^cCLa9wEa)O(04(Eq^ze?Gtr3x3%?~&@GYfKv`
zVjV|-o*78`^`s1Zkd!?LJ!ZjS=mvQ?+^L*@kEZyIMO209=IhV7m#mIlc108%9MRMi
zGlU&gukvhsmD0$pAR(b(YmaS~md<J2aZJwEJ$hGeqoIc%GY9h;QOz{UF;BDkE8yDQ
z?U(^i!naAesIo`9n+`1Da)aB@>CG1AtzJd%{4B!vOK+?0$d#!Dd9?uFGXxBt@xGSR
z!=E@;!vXv}uey27q1Egj{ue!irnqd~dpWvK`uiRR9VBN1{%*lav*a2P5s4Vl8xneJ
zOLt3YC1o`SXPeUwF4i{QBzC?9l0hIcy4@=me!L*M(j7fGNwoV<@(vL(5mC=if6Vw_
DfQ|Tc

literal 0
HcmV?d00001

diff --git a/docs/src/main/asciidoc/accumulo_user_manual.asciidoc b/docs/src/main/asciidoc/accumulo_user_manual.asciidoc
index 32f19fe60..b62983aed 100644
-- a/docs/src/main/asciidoc/accumulo_user_manual.asciidoc
++ b/docs/src/main/asciidoc/accumulo_user_manual.asciidoc
@@ -59,6 +59,8 @@ include::chapters/ssl.txt[]
 
 include::chapters/kerberos.txt[]
 
include::chapters/sampling.txt[]

 include::chapters/administration.txt[]
 
 include::chapters/multivolume.txt[]
diff --git a/docs/src/main/asciidoc/chapters/sampling.txt b/docs/src/main/asciidoc/chapters/sampling.txt
new file mode 100644
index 000000000..f035c5600
-- /dev/null
++ b/docs/src/main/asciidoc/chapters/sampling.txt
@@ -0,0 +1,86 @@
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

== Sampling

=== Overview

Accumulo has the ability to generate and scan a per table set of sample data.
This sample data is kept up to date as a table is mutated.  What key values are
placed in the sample data is configurable per table.

This feature can be used for query estimation and optimization.  For an example
of estimaiton assume an Accumulo table is configured to generate a sample
containing one millionth of a tables data.   If a query is executed against the
sample and returns one thousand results, then the same query against all the
data would probably return a billion results.  A nice property of having
Accumulo generate the sample is that its always up to date.  So estimations
will be accurate even when querying the most recently written data.

An example of a query optimization is an iterator using sample data to get an
estimate, and then making decisions based on the estimate.

=== Configuring

Inorder to use sampling, an Accumulo table must be configured with a class that
implements +org.apache.accumulo.core.sample.Sampler+ along with options for
that class.  For guidance on implementing a Sampler see that interface's
javadoc.  Accumulo provides a few implementations out of the box.   For
information on how to use the samplers that ship with Accumulo look in the
package `org.apache.accumulo.core.sample` and consult the javadoc of the
classes there.  See +README.sample+ and +SampleExample.java+ for examples of
how to configure a Sampler on a table.

Once a table is configured with a sampler all writes after that point will
generate sample data.  For data written before sampling was configured sample
data will not be present.  A compaction can be initiated that only compacts the
files in the table that do not have sample data.   The example readme shows how
to do this.

If the sampling configuration of a table is changed, then Accumulo will start
generating new sample data with the new configuration.   However old data will
still have sample data generated with the previous configuration.  A selective
compaction can also be issued in this case to regenerate the sample data.

=== Scanning sample data

Inorder to scan sample data, use the +setSamplerConfiguration(...)+  method on
+Scanner+ or +BatchScanner+.  Please consult this methods javadocs for more
information.

Sample data can also be scanned from within an Accumulo
+SortedKeyValueIterator+.  To see how to do this look at the example iterator
referenced in README.sample.  Also, consult the javadoc on
+org.apache.accumulo.core.iterators.IteratorEnvironment.cloneWithSamplingEnabled()+.

Map reduce jobs using the +AccumuloInputFormat+ can also read sample data.  See
the javadoc for the +setSamplerConfiguration()+ method on
+AccumuloInputFormat+.

Scans over sample data will throw a +SampleNotPresentException+ in the following cases :

. sample data is not present,
. sample data is present but was generated with multiple configurations
. sample data is partially present

So a scan over sample data can only succeed if all data written has sample data
generated with the same configuration.

=== Bulk import

When generating rfiles to bulk import into Accumulo, those rfiles can contain
sample data.  To use this feature, look at the javadoc on the
+AccumuloFileOutputFormat.setSampler(...)+ method.

diff --git a/docs/src/main/resources/examples/README b/docs/src/main/resources/examples/README
index 4211050fb..03c2e05f0 100644
-- a/docs/src/main/resources/examples/README
++ b/docs/src/main/resources/examples/README
@@ -80,6 +80,8 @@ features of Apache Accumulo.
    README.rowhash:     Using MapReduce to read a table and write to a new
                        column in the same table.
 
   README.sample:      Building and using sample data in Accumulo.

    README.shard:       Using the intersecting iterator with a term index
                        partitioned by document.
 
diff --git a/docs/src/main/resources/examples/README.sample b/docs/src/main/resources/examples/README.sample
new file mode 100644
index 000000000..15288aaed
-- /dev/null
++ b/docs/src/main/resources/examples/README.sample
@@ -0,0 +1,192 @@
Title: Apache Accumulo Batch Writing and Scanning Example
Notice:    Licensed to the Apache Software Foundation (ASF) under one
           or more contributor license agreements.  See the NOTICE file
           distributed with this work for additional information
           regarding copyright ownership.  The ASF licenses this file
           to you under the Apache License, Version 2.0 (the
           "License"); you may not use this file except in compliance
           with the License.  You may obtain a copy of the License at
           .
             http://www.apache.org/licenses/LICENSE-2.0
           .
           Unless required by applicable law or agreed to in writing,
           software distributed under the License is distributed on an
           "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
           KIND, either express or implied.  See the License for the
           specific language governing permissions and limitations
           under the License.


Basic Sampling Example
----------------------

Accumulo supports building a set of sample data that can be efficiently
accessed by scanners.  What data is included in the sample set is configurable.
Below, some data representing documents are inserted.  

    root@instance sampex> createtable sampex
    root@instance sampex> insert 9255 doc content 'abcde'
    root@instance sampex> insert 9255 doc url file://foo.txt
    root@instance sampex> insert 8934 doc content 'accumulo scales'
    root@instance sampex> insert 8934 doc url file://accumulo_notes.txt
    root@instance sampex> insert 2317 doc content 'milk, eggs, bread, parmigiano-reggiano'
    root@instance sampex> insert 2317 doc url file://groceries/9.txt
    root@instance sampex> insert 3900 doc content 'EC2 ate my homework'
    root@instance sampex> insert 3900 doc uril file://final_project.txt

Below the table sampex is configured to build a sample set.  The configuration
causes Accumulo to include any row where `murmur3_32(row) % 3 ==0` in the
tables sample data.

    root@instance sampex> config -t sampex -s table.sampler.opt.hasher=murmur3_32
    root@instance sampex> config -t sampex -s table.sampler.opt.modulus=3
    root@instance sampex> config -t sampex -s table.sampler=org.apache.accumulo.core.sample.RowSampler

Below, attempting to scan the sample returns an error.  This is because data
was inserted before the sample set was configured.

    root@instance sampex> scan --sample
    2015-09-09 12:21:50,643 [shell.Shell] ERROR: org.apache.accumulo.core.client.SampleNotPresentException: Table sampex(ID:2) does not have sampling configured or built

To remedy this problem, the following command will flush in memory data and
compact any files that do not contain the correct sample data.   

    root@instance sampex> compact -t sampex --sf-no-sample

After the compaction, the sample scan works.  

    root@instance sampex> scan --sample
    2317 doc:content []    milk, eggs, bread, parmigiano-reggiano
    2317 doc:url []    file://groceries/9.txt

The commands below show that updates to data in the sample are seen when
scanning the sample.

    root@instance sampex> insert 2317 doc content 'milk, eggs, bread, parmigiano-reggiano, butter'
    root@instance sampex> scan --sample
    2317 doc:content []    milk, eggs, bread, parmigiano-reggiano, butter
    2317 doc:url []    file://groceries/9.txt

Inorder to make scanning the sample fast, sample data is partitioned as data is
written to Accumulo.  This means if the sample configuration is changed, that
data written previously is partitioned using a different criteria.  Accumulo
will detect this situation and fail sample scans.  The commands below show this
failure and fixiing the problem with a compaction.

    root@instance sampex> config -t sampex -s table.sampler.opt.modulus=2
    root@instance sampex> scan --sample
    2015-09-09 12:22:51,058 [shell.Shell] ERROR: org.apache.accumulo.core.client.SampleNotPresentException: Table sampex(ID:2) does not have sampling configured or built
    root@instance sampex> compact -t sampex --sf-no-sample
    2015-09-09 12:23:07,242 [shell.Shell] INFO : Compaction of table sampex started for given range
    root@instance sampex> scan --sample
    2317 doc:content []    milk, eggs, bread, parmigiano-reggiano
    2317 doc:url []    file://groceries/9.txt
    3900 doc:content []    EC2 ate my homework
    3900 doc:uril []    file://final_project.txt
    9255 doc:content []    abcde
    9255 doc:url []    file://foo.txt

The example above is replicated in a java program using the Accumulo API.
Below is the program name and the command to run it.

    ./bin/accumulo org.apache.accumulo.examples.simple.sample.SampleExample -i instance -z localhost -u root -p secret

The commands below look under the hood to give some insight into how this
feature works.  The commands determine what files the sampex table is using.

    root@instance sampex> tables -l
    accumulo.metadata    =>        !0
    accumulo.replication =>      +rep
    accumulo.root        =>        +r
    sampex               =>         2
    trace                =>         1
    root@instance sampex> scan -t accumulo.metadata -c file -b 2 -e 2<
    2< file:hdfs://localhost:10000/accumulo/tables/2/default_tablet/A000000s.rf []    702,8

Below shows running `accumulo rfile-info` on the file above.  This shows the
rfile has a normal default locality group and a sample default locality group.
The output also shows the configuration used to create the sample locality
group.  The sample configuration within a rfile must match the tables sample
configuration for sample scan to work.

    $ ./bin/accumulo rfile-info hdfs://localhost:10000/accumulo/tables/2/default_tablet/A000000s.rf
    Reading file: hdfs://localhost:10000/accumulo/tables/2/default_tablet/A000000s.rf
    RFile Version            : 8
    
    Locality group           : <DEFAULT>
    	Start block            : 0
    	Num   blocks           : 1
    	Index level 0          : 35 bytes  1 blocks
    	First key              : 2317 doc:content [] 1437672014986 false
    	Last key               : 9255 doc:url [] 1437672014875 false
    	Num entries            : 8
    	Column families        : [doc]
    
    Sample Configuration     :
    	Sampler class          : org.apache.accumulo.core.sample.RowSampler
    	Sampler options        : {hasher=murmur3_32, modulus=2}

    Sample Locality group    : <DEFAULT>
    	Start block            : 0
    	Num   blocks           : 1
    	Index level 0          : 36 bytes  1 blocks
    	First key              : 2317 doc:content [] 1437672014986 false
    	Last key               : 9255 doc:url [] 1437672014875 false
    	Num entries            : 6
    	Column families        : [doc]
    
    Meta block     : BCFile.index
          Raw size             : 4 bytes
          Compressed size      : 12 bytes
          Compression type     : gz

    Meta block     : RFile.index
          Raw size             : 309 bytes
          Compressed size      : 176 bytes
          Compression type     : gz


Shard Sampling Example
-------------------------

`README.shard` shows how to index and search files using Accumulo.  That
example indexes documents into a table named `shard`.  The indexing scheme used
in that example places the document name in the column qualifier.  A useful
sample of this indexing scheme should contain all data for any document in the
sample.   To accomplish this, the following commands build a sample for the
shard table based on the column qualifier.

    root@instance shard> config -t shard -s table.sampler.opt.hasher=murmur3_32
    root@instance shard> config -t shard -s table.sampler.opt.modulus=101
    root@instance shard> config -t shard -s table.sampler.opt.qualifier=true
    root@instance shard> config -t shard -s table.sampler=org.apache.accumulo.core.sample.RowColumnSampler
    root@instance shard> compact -t shard --sf-no-sample -w
    2015-07-23 15:00:09,280 [shell.Shell] INFO : Compacting table ...
    2015-07-23 15:00:10,134 [shell.Shell] INFO : Compaction of table shard completed for given range

After enabling sampling, the command below counts the number of documents in
the sample containing the words `import` and `int`.     

    $ ./bin/accumulo org.apache.accumulo.examples.simple.shard.Query --sample -i instance16 -z localhost -t shard -u root -p secret import int | fgrep '.java' | wc
         11      11    1246

The command below counts the total number of documents containing the words
`import` and `int`.

    $ ./bin/accumulo org.apache.accumulo.examples.simple.shard.Query -i instance16 -z localhost -t shard -u root -p secret import int | fgrep '.java' | wc
       1085    1085  118175

The counts 11 out of 1085 total are around what would be expected for a modulus
of 101.  Querying the sample first provides a quick way to estimate how much data
the real query will bring back. 

Another way sample data could be used with the shard example is with a
specialized iterator.  In the examples source code there is an iterator named
CutoffIntersectingIterator.  This iterator first checks how many documents are
found in the sample data.  If too many documents are found in the sample data,
then it returns nothing.   Otherwise it proceeds to query the full data set.
To experiment with this iterator, use the following command.  The
`--sampleCutoff` option below will cause the query to return nothing if based
on the sample it appears a query would return more than 1000 documents.

    $ ./bin/accumulo org.apache.accumulo.examples.simple.shard.Query --sampleCutoff 1000 -i instance16 -z localhost -t shard -u root -p secret import int | fgrep '.java' | wc
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
new file mode 100644
index 000000000..57d77b177
-- /dev/null
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
@@ -0,0 +1,150 @@
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

package org.apache.accumulo.examples.simple.sample;

import java.util.Collections;
import java.util.Map.Entry;

import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.cli.ClientOnDefaultTable;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.examples.simple.client.RandomBatchWriter;
import org.apache.accumulo.examples.simple.shard.CutoffIntersectingIterator;

import com.google.common.collect.ImmutableMap;

/**
 * A simple example of using Accumulo's sampling feature. This example does something similar to what README.sample shows using the shell. Also see
 * {@link CutoffIntersectingIterator} and README.sample for an example of how to use sample data from within an iterator.
 */
public class SampleExample {

  // a compaction strategy that only selects files for compaction that have no sample data or sample data created in a different way than the tables
  static final CompactionStrategyConfig NO_SAMPLE_STRATEGY = new CompactionStrategyConfig(
      "org.apache.accumulo.tserver.compaction.strategies.ConfigurableCompactionStrategy").setOptions(Collections.singletonMap("SF_NO_SAMPLE", ""));

  static class Opts extends ClientOnDefaultTable {
    public Opts() {
      super("sampex");
    }
  }

  public static void main(String[] args) throws Exception {
    Opts opts = new Opts();
    BatchWriterOpts bwOpts = new BatchWriterOpts();
    opts.parseArgs(RandomBatchWriter.class.getName(), args, bwOpts);

    Connector conn = opts.getConnector();

    if (!conn.tableOperations().exists(opts.getTableName())) {
      conn.tableOperations().create(opts.getTableName());
    } else {
      System.out.println("Table exists, not doing anything.");
      return;
    }

    // write some data
    BatchWriter bw = conn.createBatchWriter(opts.getTableName(), bwOpts.getBatchWriterConfig());
    bw.addMutation(createMutation("9225", "abcde", "file://foo.txt"));
    bw.addMutation(createMutation("8934", "accumulo scales", "file://accumulo_notes.txt"));
    bw.addMutation(createMutation("2317", "milk, eggs, bread, parmigiano-reggiano", "file://groceries/9/txt"));
    bw.addMutation(createMutation("3900", "EC2 ate my homework", "file://final_project.txt"));
    bw.flush();

    SamplerConfiguration sc1 = new SamplerConfiguration(RowSampler.class.getName());
    sc1.setOptions(ImmutableMap.of("hasher", "murmur3_32", "modulus", "3"));

    conn.tableOperations().setSamplerConfiguration(opts.getTableName(), sc1);

    Scanner scanner = conn.createScanner(opts.getTableName(), Authorizations.EMPTY);
    System.out.println("Scanning all data :");
    print(scanner);
    System.out.println();

    System.out.println("Scanning with sampler configuration.  Data was written before sampler was set on table, scan should fail.");
    scanner.setSamplerConfiguration(sc1);
    try {
      print(scanner);
    } catch (SampleNotPresentException e) {
      System.out.println("  Saw sample not present exception as expected.");
    }
    System.out.println();

    // compact table to recreate sample data
    conn.tableOperations().compact(opts.getTableName(), new CompactionConfig().setCompactionStrategy(NO_SAMPLE_STRATEGY));

    System.out.println("Scanning after compaction (compaction should have created sample data) : ");
    print(scanner);
    System.out.println();

    // update a document in the sample data
    bw.addMutation(createMutation("2317", "milk, eggs, bread, parmigiano-reggiano, butter", "file://groceries/9/txt"));
    bw.close();
    System.out.println("Scanning sample after updating content for docId 2317 (should see content change in sample data) : ");
    print(scanner);
    System.out.println();

    // change tables sampling configuration...
    SamplerConfiguration sc2 = new SamplerConfiguration(RowSampler.class.getName());
    sc2.setOptions(ImmutableMap.of("hasher", "murmur3_32", "modulus", "2"));
    conn.tableOperations().setSamplerConfiguration(opts.getTableName(), sc2);
    // compact table to recreate sample data using new configuration
    conn.tableOperations().compact(opts.getTableName(), new CompactionConfig().setCompactionStrategy(NO_SAMPLE_STRATEGY));

    System.out.println("Scanning with old sampler configuration.  Sample data was created using new configuration with a compaction.  Scan should fail.");
    try {
      // try scanning with old sampler configuration
      print(scanner);
    } catch (SampleNotPresentException e) {
      System.out.println("  Saw sample not present exception as expected ");
    }
    System.out.println();

    // update expected sampler configuration on scanner
    scanner.setSamplerConfiguration(sc2);

    System.out.println("Scanning with new sampler configuration : ");
    print(scanner);
    System.out.println();

  }

  private static void print(Scanner scanner) {
    for (Entry<Key,Value> entry : scanner) {
      System.out.println("  " + entry.getKey() + " " + entry.getValue());
    }
  }

  private static Mutation createMutation(String docId, String content, String url) {
    Mutation m = new Mutation(docId);
    m.put("doc", "context", content);
    m.put("doc", "url", url);
    return m;
  }
}
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
new file mode 100644
index 000000000..133e8aed4
-- /dev/null
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
@@ -0,0 +1,123 @@
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

package org.apache.accumulo.examples.simple.shard;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.user.IntersectingIterator;
import org.apache.accumulo.core.sample.RowColumnSampler;

import com.google.common.base.Preconditions;

/**
 * This iterator uses a sample built from the Column Qualifier to quickly avoid intersecting iterator queries that may return too many documents.
 */

public class CutoffIntersectingIterator extends IntersectingIterator {

  private IntersectingIterator sampleII;
  private int sampleMax;
  private boolean hasTop;

  public static void setCutoff(IteratorSetting iterCfg, int cutoff) {
    Preconditions.checkArgument(cutoff >= 0);
    iterCfg.addOption("cutoff", cutoff + "");
  }

  @Override
  public boolean hasTop() {
    return hasTop && super.hasTop();
  }

  @Override
  public void seek(Range range, Collection<ByteSequence> seekColumnFamilies, boolean inclusive) throws IOException {

    sampleII.seek(range, seekColumnFamilies, inclusive);

    // this check will be redone whenever iterator stack is torn down and recreated.
    int count = 0;
    while (count <= sampleMax && sampleII.hasTop()) {
      sampleII.next();
      count++;
    }

    if (count > sampleMax) {
      // In a real application would probably want to return a key value that indicates too much data. Since this would execute for each tablet, some tablets
      // may return data. For tablets that did not return data, would want an indication.
      hasTop = false;
    } else {
      hasTop = true;
      super.seek(range, seekColumnFamilies, inclusive);
    }
  }

  @Override
  public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
    super.init(source, options, env);

    IteratorEnvironment sampleEnv = env.cloneWithSamplingEnabled();

    setMax(sampleEnv, options);

    SortedKeyValueIterator<Key,Value> sampleDC = source.deepCopy(sampleEnv);
    sampleII = new IntersectingIterator();
    sampleII.init(sampleDC, options, env);

  }

  static void validateSamplerConfig(SamplerConfiguration sampleConfig) {
    Preconditions.checkNotNull(sampleConfig);
    Preconditions.checkArgument(sampleConfig.getSamplerClassName().equals(RowColumnSampler.class.getName()),
        "Unexpected Sampler " + sampleConfig.getSamplerClassName());
    Preconditions.checkArgument(sampleConfig.getOptions().get("qualifier").equals("true"), "Expected sample on column qualifier");
    Preconditions.checkArgument(isNullOrFalse(sampleConfig.getOptions(), "row", "family", "visibility"), "Expected sample on column qualifier only");
  }

  private void setMax(IteratorEnvironment sampleEnv, Map<String,String> options) {
    String cutoffValue = options.get("cutoff");
    SamplerConfiguration sampleConfig = sampleEnv.getSamplerConfiguration();

    // Ensure the sample was constructed in an expected way. If the sample is not built as expected, then can not draw conclusions based on sample.
    Preconditions.checkNotNull(cutoffValue, "Expected cutoff option is missing");
    validateSamplerConfig(sampleConfig);

    int modulus = Integer.parseInt(sampleConfig.getOptions().get("modulus"));

    sampleMax = Math.round(Float.parseFloat(cutoffValue) / modulus);
  }

  private static boolean isNullOrFalse(Map<String,String> options, String... keys) {
    for (String key : keys) {
      String val = options.get(key);
      if (val != null && val.equals("true")) {
        return false;
      }
    }
    return true;
  }
}
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
index 41d5dc70f..79258554b 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
@@ -27,6 +27,7 @@ import org.apache.accumulo.core.cli.ClientOnRequiredTable;
 import org.apache.accumulo.core.client.BatchScanner;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
@@ -46,16 +47,32 @@ public class Query {
   static class Opts extends ClientOnRequiredTable {
     @Parameter(description = " term { <term> ... }")
     List<String> terms = new ArrayList<String>();

    @Parameter(names = {"--sample"}, description = "Do queries against sample, useful when sample is built using column qualifier")
    private boolean useSample = false;

    @Parameter(names = {"--sampleCutoff"},
        description = "Use sample data to determine if a query might return a number of documents over the cutoff.  This check is per tablet.")
    private Integer sampleCutoff = null;
   }
 
  public static List<String> query(BatchScanner bs, List<String> terms) {
  public static List<String> query(BatchScanner bs, List<String> terms, Integer cutoff) {
 
     Text columns[] = new Text[terms.size()];
     int i = 0;
     for (String term : terms) {
       columns[i++] = new Text(term);
     }
    IteratorSetting ii = new IteratorSetting(20, "ii", IntersectingIterator.class);

    IteratorSetting ii;

    if (cutoff != null) {
      ii = new IteratorSetting(20, "ii", CutoffIntersectingIterator.class);
      CutoffIntersectingIterator.setCutoff(ii, cutoff);
    } else {
      ii = new IteratorSetting(20, "ii", IntersectingIterator.class);
    }

     IntersectingIterator.setColumnFamilies(ii, columns);
     bs.addScanIterator(ii);
     bs.setRanges(Collections.singleton(new Range()));
@@ -73,9 +90,15 @@ public class Query {
     Connector conn = opts.getConnector();
     BatchScanner bs = conn.createBatchScanner(opts.getTableName(), opts.auths, bsOpts.scanThreads);
     bs.setTimeout(bsOpts.scanTimeout, TimeUnit.MILLISECONDS);

    for (String entry : query(bs, opts.terms))
    if (opts.useSample) {
      SamplerConfiguration samplerConfig = conn.tableOperations().getSamplerConfiguration(opts.getTableName());
      CutoffIntersectingIterator.validateSamplerConfig(conn.tableOperations().getSamplerConfiguration(opts.getTableName()));
      bs.setSamplerConfiguration(samplerConfig);
    }
    for (String entry : query(bs, opts.terms, opts.sampleCutoff))
       System.out.println("  " + entry);

    bs.close();
   }
 
 }
diff --git a/server/base/src/main/java/org/apache/accumulo/server/util/VerifyTabletAssignments.java b/server/base/src/main/java/org/apache/accumulo/server/util/VerifyTabletAssignments.java
index 0d7ade82b..d2d6664e4 100644
-- a/server/base/src/main/java/org/apache/accumulo/server/util/VerifyTabletAssignments.java
++ b/server/base/src/main/java/org/apache/accumulo/server/util/VerifyTabletAssignments.java
@@ -189,7 +189,7 @@ public class VerifyTabletAssignments {
     List<IterInfo> emptyListIterInfo = Collections.emptyList();
     List<TColumn> emptyListColumn = Collections.emptyList();
     InitialMultiScan is = client.startMultiScan(tinfo, context.rpcCreds(), batch, emptyListColumn, emptyListIterInfo, emptyMapSMapSS,
        Authorizations.EMPTY.getAuthorizationsBB(), false, 0L);
        Authorizations.EMPTY.getAuthorizationsBB(), false, null, 0L);
     if (is.result.more) {
       MultiScanResult result = client.continueMultiScan(tinfo, is.scanID);
       checkFailures(entry.getKey(), failures, result);
diff --git a/server/base/src/test/java/org/apache/accumulo/server/iterators/MetadataBulkLoadFilterTest.java b/server/base/src/test/java/org/apache/accumulo/server/iterators/MetadataBulkLoadFilterTest.java
index 7e9543f62..1b305304e 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/iterators/MetadataBulkLoadFilterTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/iterators/MetadataBulkLoadFilterTest.java
@@ -21,18 +21,15 @@ import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.TreeMap;
 
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.SortedMapIterator;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily;
import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.ColumnFQ;
 import org.apache.accumulo.fate.zookeeper.TransactionWatcher.Arbitrator;
 import org.apache.hadoop.io.Text;
@@ -104,20 +101,7 @@ public class MetadataBulkLoadFilterTest {
     put(tm1, "2<", TabletsSection.BulkFileColumnFamily.NAME, "/t2/fileA", "2");
 
     TestMetadataBulkLoadFilter iter = new TestMetadataBulkLoadFilter();
    iter.init(new SortedMapIterator(tm1), new HashMap<String,String>(), new IteratorEnvironment() {

      @Override
      public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
        return null;
      }

      @Override
      public void registerSideChannel(SortedKeyValueIterator<Key,Value> iter) {}

      @Override
      public Authorizations getAuthorizations() {
        return null;
      }
    iter.init(new SortedMapIterator(tm1), new HashMap<String,String>(), new BaseIteratorEnvironment() {
 
       @Override
       public boolean isFullMajorCompaction() {
@@ -128,11 +112,6 @@ public class MetadataBulkLoadFilterTest {
       public IteratorScope getIteratorScope() {
         return IteratorScope.majc;
       }

      @Override
      public AccumuloConfiguration getConfig() {
        return null;
      }
     });
 
     iter.seek(new Range(), new ArrayList<ByteSequence>(), false);
diff --git a/server/base/src/test/java/org/apache/accumulo/server/replication/StatusCombinerTest.java b/server/base/src/test/java/org/apache/accumulo/server/replication/StatusCombinerTest.java
index f4d5a9b6b..26ad8de6d 100644
-- a/server/base/src/test/java/org/apache/accumulo/server/replication/StatusCombinerTest.java
++ b/server/base/src/test/java/org/apache/accumulo/server/replication/StatusCombinerTest.java
@@ -24,16 +24,10 @@ import java.util.List;
 
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.Combiner;
 import org.apache.accumulo.core.iterators.DevNull;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.replication.ReplicationSchema.StatusSection;
import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.server.replication.proto.Replication.Status;
 import org.junit.Assert;
 import org.junit.Before;
@@ -52,38 +46,7 @@ public class StatusCombinerTest {
     builder = Status.newBuilder();
     IteratorSetting cfg = new IteratorSetting(50, StatusCombiner.class);
     Combiner.setColumns(cfg, Collections.singletonList(new Column(StatusSection.NAME)));
    combiner.init(new DevNull(), cfg.getOptions(), new IteratorEnvironment() {

      @Override
      public AccumuloConfiguration getConfig() {
        return null;
      }

      @Override
      public IteratorScope getIteratorScope() {
        return null;
      }

      @Override
      public boolean isFullMajorCompaction() {
        return false;
      }

      @Override
      public void registerSideChannel(SortedKeyValueIterator<Key,Value> arg0) {

      }

      @Override
      public Authorizations getAuthorizations() {
        return null;
      }

      @Override
      public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String arg0) throws IOException {
        return null;
      }
    });
    combiner.init(new DevNull(), cfg.getOptions(), null);
   }
 
   @Test
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
index 750ad8e1b..2c4683587 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
@@ -23,6 +23,7 @@ import java.util.concurrent.TimeUnit;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.IteratorSetting.Column;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
@@ -134,4 +135,14 @@ public class NullScanner implements Scanner {
     return 0;
   }
 
  @Override
  public void setSamplerConfiguration(SamplerConfiguration samplerConfig) {}

  @Override
  public SamplerConfiguration getSamplerConfiguration() {
    return null;
  }

  @Override
  public void clearSamplerConfiguration() {}
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/FileManager.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/FileManager.java
index 1c4676e62..2227b2504 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/FileManager.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/FileManager.java
@@ -29,6 +29,7 @@ import java.util.Map.Entry;
 import java.util.concurrent.Semaphore;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -43,6 +44,7 @@ import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator;
 import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator.DataSource;
 import org.apache.accumulo.core.iterators.system.TimeSettingIterator;
 import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.server.AccumuloServerContext;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.server.fs.VolumeManager;
@@ -458,7 +460,6 @@ public class FileManager {
       this.iflag = flag;
       ((InterruptibleIterator) this.iter).setInterruptFlag(iflag);
     }

   }
 
   public class ScanFileManager {
@@ -502,7 +503,8 @@ public class FileManager {
       return newlyReservedReaders;
     }
 
    public synchronized List<InterruptibleIterator> openFiles(Map<FileRef,DataFileValue> files, boolean detachable) throws IOException {
    public synchronized List<InterruptibleIterator> openFiles(Map<FileRef,DataFileValue> files, boolean detachable, SamplerConfigurationImpl samplerConfig)
        throws IOException {
 
       List<FileSKVIterator> newlyReservedReaders = openFileRefs(files.keySet());
 
@@ -511,13 +513,22 @@ public class FileManager {
       for (FileSKVIterator reader : newlyReservedReaders) {
         String filename = getReservedReadeFilename(reader);
         InterruptibleIterator iter;

        FileSKVIterator source = reader;
        if (samplerConfig != null) {
          source = source.getSample(samplerConfig);
          if (source == null) {
            throw new SampleNotPresentException();
          }
        }

         if (detachable) {
          FileDataSource fds = new FileDataSource(filename, reader);
          FileDataSource fds = new FileDataSource(filename, source);
           dataSources.add(fds);
           SourceSwitchingIterator ssi = new SourceSwitchingIterator(fds);
           iter = new ProblemReportingIterator(context, tablet.getTableId().toString(), filename, continueOnFailure, ssi);
         } else {
          iter = new ProblemReportingIterator(context, tablet.getTableId().toString(), filename, continueOnFailure, reader);
          iter = new ProblemReportingIterator(context, tablet.getTableId().toString(), filename, continueOnFailure, source);
         }
         DataFileValue value = files.get(new FileRef(filename));
         if (value.isTimeSet()) {
@@ -539,7 +550,7 @@ public class FileManager {
         fds.unsetIterator();
     }
 
    public synchronized void reattach() throws IOException {
    public synchronized void reattach(SamplerConfigurationImpl samplerConfig) throws IOException {
       if (tabletReservedReaders.size() != 0)
         throw new IllegalStateException();
 
@@ -562,7 +573,14 @@ public class FileManager {
 
       for (FileDataSource fds : dataSources) {
         FileSKVIterator reader = map.get(fds.file).remove(0);
        fds.setIterator(reader);
        FileSKVIterator source = reader;
        if (samplerConfig != null) {
          source = source.getSample(samplerConfig);
          if (source == null) {
            throw new SampleNotPresentException();
          }
        }
        fds.setIterator(source);
       }
     }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
index 2274eeaa0..f5141ffd1 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
@@ -16,6 +16,8 @@
  */
 package org.apache.accumulo.tserver;
 
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
@@ -33,8 +35,11 @@ import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.conf.SiteConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
@@ -51,15 +56,20 @@ import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.SortedMapIterator;
 import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.iterators.system.EmptyIterator;
 import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator.LocalityGroup;
 import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator;
 import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator.DataSource;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.core.util.LocalityGroupUtil;
 import org.apache.accumulo.core.util.LocalityGroupUtil.LocalityGroupConfigurationError;
 import org.apache.accumulo.core.util.LocalityGroupUtil.Partitioner;
import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.core.util.PreAllocatedArray;
 import org.apache.commons.lang.mutable.MutableLong;
 import org.apache.hadoop.conf.Configuration;
@@ -68,7 +78,8 @@ import org.apache.hadoop.fs.Path;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
 
 public class InMemoryMap {
   private SimpleMap map = null;
@@ -80,22 +91,58 @@ public class InMemoryMap {
 
   private Map<String,Set<ByteSequence>> lggroups;
 
  public InMemoryMap(boolean useNativeMap, String memDumpDir) {
    this(new HashMap<String,Set<ByteSequence>>(), useNativeMap, memDumpDir);
  private static Pair<SamplerConfigurationImpl,Sampler> getSampler(AccumuloConfiguration config) {
    try {
      SamplerConfigurationImpl sampleConfig = SamplerConfigurationImpl.newSamplerConfig(config);
      if (sampleConfig == null) {
        return new Pair<>(null, null);
      }

      return new Pair<>(sampleConfig, SamplerFactory.newSampler(sampleConfig, config));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
   }
 
  public InMemoryMap(Map<String,Set<ByteSequence>> lggroups, boolean useNativeMap, String memDumpDir) {
    this.memDumpDir = memDumpDir;
    this.lggroups = lggroups;
  private AtomicReference<Pair<SamplerConfigurationImpl,Sampler>> samplerRef = new AtomicReference<>(null);
 
    if (lggroups.size() == 0)
      map = newMap(useNativeMap);
    else
      map = new LocalityGroupMap(lggroups, useNativeMap);
  private AccumuloConfiguration config;

  // defer creating sampler until first write. This was done because an empty sample map configured with no sampler will not flush after a user changes sample
  // config.
  private Sampler getOrCreateSampler() {
    Pair<SamplerConfigurationImpl,Sampler> pair = samplerRef.get();
    if (pair == null) {
      pair = getSampler(config);
      if (!samplerRef.compareAndSet(null, pair)) {
        pair = samplerRef.get();
      }
    }

    return pair.getSecond();
   }
 
   public InMemoryMap(AccumuloConfiguration config) throws LocalityGroupConfigurationError {
    this(LocalityGroupUtil.getLocalityGroups(config), config.getBoolean(Property.TSERV_NATIVEMAP_ENABLED), config.get(Property.TSERV_MEMDUMP_DIR));

    boolean useNativeMap = config.getBoolean(Property.TSERV_NATIVEMAP_ENABLED);

    this.memDumpDir = config.get(Property.TSERV_MEMDUMP_DIR);
    this.lggroups = LocalityGroupUtil.getLocalityGroups(config);

    this.config = config;

    SimpleMap allMap;
    SimpleMap sampleMap;

    if (lggroups.size() == 0) {
      allMap = newMap(useNativeMap);
      sampleMap = newMap(useNativeMap);
    } else {
      allMap = new LocalityGroupMap(lggroups, useNativeMap);
      sampleMap = new LocalityGroupMap(lggroups, useNativeMap);
    }

    map = new SampleMap(allMap, sampleMap);
   }
 
   private static SimpleMap newMap(boolean useNativeMap) {
@@ -117,7 +164,7 @@ public class InMemoryMap {
 
     int size();
 
    InterruptibleIterator skvIterator();
    InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig);
 
     void delete();
 
@@ -126,6 +173,95 @@ public class InMemoryMap {
     void mutate(List<Mutation> mutations, int kvCount);
   }
 
  private class SampleMap implements SimpleMap {

    private SimpleMap map;
    private SimpleMap sample;

    public SampleMap(SimpleMap map, SimpleMap sampleMap) {
      this.map = map;
      this.sample = sampleMap;
    }

    @Override
    public Value get(Key key) {
      return map.get(key);
    }

    @Override
    public Iterator<Entry<Key,Value>> iterator(Key startKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
      if (samplerConfig == null)
        return map.skvIterator(null);
      else {
        Pair<SamplerConfigurationImpl,Sampler> samplerAndConf = samplerRef.get();
        if (samplerAndConf == null) {
          return EmptyIterator.EMPTY_ITERATOR;
        } else if (samplerAndConf.getFirst() != null && samplerAndConf.getFirst().equals(samplerConfig)) {
          return sample.skvIterator(null);
        } else {
          throw new SampleNotPresentException();
        }
      }
    }

    @Override
    public void delete() {
      map.delete();
      sample.delete();
    }

    @Override
    public long getMemoryUsed() {
      return map.getMemoryUsed() + sample.getMemoryUsed();
    }

    @Override
    public void mutate(List<Mutation> mutations, int kvCount) {
      map.mutate(mutations, kvCount);

      Sampler sampler = getOrCreateSampler();
      if (sampler != null) {
        List<Mutation> sampleMutations = null;

        for (Mutation m : mutations) {
          List<ColumnUpdate> colUpdates = m.getUpdates();
          List<ColumnUpdate> sampleColUpdates = null;
          for (ColumnUpdate cvp : colUpdates) {
            Key k = new Key(m.getRow(), cvp.getColumnFamily(), cvp.getColumnQualifier(), cvp.getColumnVisibility(), cvp.getTimestamp(), cvp.isDeleted(), false);
            if (sampler.accept(k)) {
              if (sampleColUpdates == null) {
                sampleColUpdates = new ArrayList<>();
              }
              sampleColUpdates.add(cvp);
            }
          }

          if (sampleColUpdates != null) {
            if (sampleMutations == null) {
              sampleMutations = new ArrayList<>();
            }

            sampleMutations.add(new LocalityGroupUtil.PartitionedMutation(m.getRow(), sampleColUpdates));
          }
        }

        if (sampleMutations != null) {
          sample.mutate(sampleMutations, kvCount);
        }
      }
    }
  }

   private static class LocalityGroupMap implements SimpleMap {
 
     private PreAllocatedArray<Map<ByteSequence,MutableLong>> groupFams;
@@ -181,13 +317,16 @@ public class InMemoryMap {
     }
 
     @Override
    public InterruptibleIterator skvIterator() {
    public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
      if (samplerConfig != null)
        throw new SampleNotPresentException();

       LocalityGroup groups[] = new LocalityGroup[maps.length];
       for (int i = 0; i < groups.length; i++) {
         if (i < groupFams.length)
          groups[i] = new LocalityGroup(maps[i].skvIterator(), groupFams.get(i), false);
          groups[i] = new LocalityGroup(maps[i].skvIterator(null), groupFams.get(i), false);
         else
          groups[i] = new LocalityGroup(maps[i].skvIterator(), null, true);
          groups[i] = new LocalityGroup(maps[i].skvIterator(null), null, true);
       }
 
       return new LocalityGroupIterator(groups, nonDefaultColumnFamilies);
@@ -264,7 +403,9 @@ public class InMemoryMap {
     }
 
     @Override
    public synchronized InterruptibleIterator skvIterator() {
    public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
      if (samplerConfig != null)
        throw new SampleNotPresentException();
       if (map == null)
         throw new IllegalStateException();
 
@@ -327,7 +468,9 @@ public class InMemoryMap {
     }
 
     @Override
    public InterruptibleIterator skvIterator() {
    public InterruptibleIterator skvIterator(SamplerConfigurationImpl samplerConfig) {
      if (samplerConfig != null)
        throw new SampleNotPresentException();
       return (InterruptibleIterator) nativeMap.skvIterator();
     }
 
@@ -410,16 +553,30 @@ public class InMemoryMap {
     private MemoryDataSource parent;
     private IteratorEnvironment env;
     private AtomicBoolean iflag;
    private SamplerConfigurationImpl iteratorSamplerConfig;

    private SamplerConfigurationImpl getSamplerConfig() {
      if (env != null) {
        if (env.isSamplingEnabled()) {
          return new SamplerConfigurationImpl(env.getSamplerConfiguration());
        } else {
          return null;
        }
      } else {
        return iteratorSamplerConfig;
      }
    }
 
    MemoryDataSource() {
      this(null, false, null, null);
    MemoryDataSource(SamplerConfigurationImpl samplerConfig) {
      this(null, false, null, null, samplerConfig);
     }
 
    public MemoryDataSource(MemoryDataSource parent, boolean switched, IteratorEnvironment env, AtomicBoolean iflag) {
    public MemoryDataSource(MemoryDataSource parent, boolean switched, IteratorEnvironment env, AtomicBoolean iflag, SamplerConfigurationImpl samplerConfig) {
       this.parent = parent;
       this.switched = switched;
       this.env = env;
       this.iflag = iflag;
      this.iteratorSamplerConfig = samplerConfig;
     }
 
     @Override
@@ -457,6 +614,10 @@ public class InMemoryMap {
         reader = new RFileOperations().openReader(memDumpFile, true, fs, conf, SiteConfiguration.getInstance());
         if (iflag != null)
           reader.setInterruptFlag(iflag);

        if (getSamplerConfig() != null) {
          reader = reader.getSample(getSamplerConfig());
        }
       }
 
       return reader;
@@ -466,7 +627,7 @@ public class InMemoryMap {
     public SortedKeyValueIterator<Key,Value> iterator() throws IOException {
       if (iter == null)
         if (!switched) {
          iter = map.skvIterator();
          iter = map.skvIterator(getSamplerConfig());
           if (iflag != null)
             iter.setInterruptFlag(iflag);
         } else {
@@ -485,7 +646,7 @@ public class InMemoryMap {
 
     @Override
     public DataSource getDeepCopyDataSource(IteratorEnvironment env) {
      return new MemoryDataSource(parent == null ? this : parent, switched, env, iflag);
      return new MemoryDataSource(parent == null ? this : parent, switched, env, iflag, iteratorSamplerConfig);
     }
 
     @Override
@@ -562,7 +723,7 @@ public class InMemoryMap {
 
   }
 
  public synchronized MemoryIterator skvIterator() {
  public synchronized MemoryIterator skvIterator(SamplerConfigurationImpl iteratorSamplerConfig) {
     if (map == null)
       throw new NullPointerException();
 
@@ -570,8 +731,9 @@ public class InMemoryMap {
       throw new IllegalStateException("Can not obtain iterator after map deleted");
 
     int mc = kvCount.get();
    MemoryDataSource mds = new MemoryDataSource();
    SourceSwitchingIterator ssi = new SourceSwitchingIterator(new MemoryDataSource());
    MemoryDataSource mds = new MemoryDataSource(iteratorSamplerConfig);
    // TODO seems like a bug that two MemoryDataSources are created... may need to fix in older branches
    SourceSwitchingIterator ssi = new SourceSwitchingIterator(mds);
     MemoryIterator mi = new MemoryIterator(new PartialMutationSkippingIterator(ssi, mc));
     mi.setSSI(ssi);
     mi.setMDS(mds);
@@ -584,7 +746,7 @@ public class InMemoryMap {
     if (nextKVCount.get() - 1 != kvCount.get())
       throw new IllegalStateException("Memory map in unexpected state : nextKVCount = " + nextKVCount.get() + " kvCount = " + kvCount.get());
 
    return map.skvIterator();
    return map.skvIterator(null);
   }
 
   private boolean deleted = false;
@@ -615,9 +777,15 @@ public class InMemoryMap {
         Configuration newConf = new Configuration(conf);
         newConf.setInt("io.seqfile.compress.blocksize", 100000);
 
        FileSKVWriter out = new RFileOperations().openWriter(tmpFile, fs, newConf, SiteConfiguration.getInstance());
        AccumuloConfiguration siteConf = SiteConfiguration.getInstance();
 
        InterruptibleIterator iter = map.skvIterator();
        if (getOrCreateSampler() != null) {
          siteConf = createSampleConfig(siteConf);
        }

        FileSKVWriter out = new RFileOperations().openWriter(tmpFile, fs, newConf, siteConf);

        InterruptibleIterator iter = map.skvIterator(null);
 
         HashSet<ByteSequence> allfams = new HashSet<ByteSequence>();
 
@@ -668,14 +836,28 @@ public class InMemoryMap {
     tmpMap.delete();
   }
 
  private AccumuloConfiguration createSampleConfig(AccumuloConfiguration siteConf) {
    ConfigurationCopy confCopy = new ConfigurationCopy(Iterables.filter(siteConf, new Predicate<Entry<String,String>>() {
      @Override
      public boolean apply(Entry<String,String> input) {
        return !input.getKey().startsWith(Property.TABLE_SAMPLER.getKey());
      }
    }));

    for (Entry<String,String> entry : samplerRef.get().getFirst().toTablePropertiesMap().entrySet()) {
      confCopy.set(entry.getKey(), entry.getValue());
    }

    siteConf = confCopy;
    return siteConf;
  }

   private void dumpLocalityGroup(FileSKVWriter out, InterruptibleIterator iter) throws IOException {
     while (iter.hasTop() && activeIters.size() > 0) {
       // RFile does not support MemKey, so we move the kv count into the value only for the RFile.
       // There is no need to change the MemKey to a normal key because the kvCount info gets lost when it is written
      Value newValue = new MemValue(iter.getTopValue(), ((MemKey) iter.getTopKey()).kvCount);
      out.append(iter.getTopKey(), newValue);
      out.append(iter.getTopKey(), MemValue.encode(iter.getTopValue(), ((MemKey) iter.getTopKey()).kvCount));
       iter.next();

     }
   }
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/MemKeyConversionIterator.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/MemKeyConversionIterator.java
index 00c8be94c..71a4cbdf4 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/MemKeyConversionIterator.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/MemKeyConversionIterator.java
@@ -61,10 +61,10 @@ class MemKeyConversionIterator extends WrappingIterator implements Interruptible
       currVal = v;
       return;
     }
    currVal = new Value(v);
    int mc = MemValue.splitKVCount(currVal);
    currKey = new MemKey(k, mc);
 
    MemValue mv = MemValue.decode(v);
    currVal = mv.value;
    currKey = new MemKey(k, mv.kvCount);
   }
 
   @Override
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/MemValue.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/MemValue.java
index bc444596b..af6f2f112 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/MemValue.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/MemValue.java
@@ -16,69 +16,38 @@
  */
 package org.apache.accumulo.tserver;
 
import java.io.DataOutput;
import java.io.IOException;

 import org.apache.accumulo.core.data.Value;
 
 /**
  *
  */
public class MemValue extends Value {
  int kvCount;
  boolean merged = false;
public class MemValue {
 
  public MemValue() {
    super();
    this.kvCount = Integer.MAX_VALUE;
  }
  Value value;
  int kvCount;
 
   public MemValue(Value value, int kv) {
    super(value);
    this.value = value;
     this.kvCount = kv;
   }
 
  // Override
  @Override
  public void write(final DataOutput out) throws IOException {
    if (!merged) {
      byte[] combinedBytes = new byte[getSize() + 4];
      System.arraycopy(value, 0, combinedBytes, 4, getSize());
      combinedBytes[0] = (byte) (kvCount >>> 24);
      combinedBytes[1] = (byte) (kvCount >>> 16);
      combinedBytes[2] = (byte) (kvCount >>> 8);
      combinedBytes[3] = (byte) (kvCount);
      value = combinedBytes;
      merged = true;
    }
    super.write(out);
  }

  @Override
  public void set(final byte[] b) {
    super.set(b);
    merged = false;
  }

  @Override
  public void copy(byte[] b) {
    super.copy(b);
    merged = false;
  public static Value encode(Value value, int kv) {
    byte[] combinedBytes = new byte[value.getSize() + 4];
    System.arraycopy(value.get(), 0, combinedBytes, 4, value.getSize());
    combinedBytes[0] = (byte) (kv >>> 24);
    combinedBytes[1] = (byte) (kv >>> 16);
    combinedBytes[2] = (byte) (kv >>> 8);
    combinedBytes[3] = (byte) (kv);
    return new Value(combinedBytes);
   }
 
  /**
   * Takes a Value and will take out the embedded kvCount, and then return that value while replacing the Value with the original unembedded version
   *
   * @return The kvCount embedded in v.
   */
  public static int splitKVCount(Value v) {
    if (v instanceof MemValue)
      return ((MemValue) v).kvCount;

  public static MemValue decode(Value v) {
     byte[] originalBytes = new byte[v.getSize() - 4];
     byte[] combined = v.get();
     System.arraycopy(combined, 4, originalBytes, 0, originalBytes.length);
     v.set(originalBytes);
    return (combined[0] << 24) + ((combined[1] & 0xFF) << 16) + ((combined[2] & 0xFF) << 8) + (combined[3] & 0xFF);
    int kv = (combined[0] << 24) + ((combined[1] & 0xFF) << 16) + ((combined[2] & 0xFF) << 8) + (combined[3] & 0xFF);

    return new MemValue(new Value(originalBytes), kv);
   }
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
index cf01dd3ed..3cb4d40fa 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/NativeMap.java
@@ -34,6 +34,7 @@ import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.ColumnUpdate;
 import org.apache.accumulo.core.data.Key;
@@ -749,6 +750,9 @@ public class NativeMap implements Iterable<Map.Entry<Key,Value>> {
 
     @Override
     public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
      if (env != null && env.isSamplingEnabled()) {
        throw new SampleNotPresentException();
      }
       return new NMSKVIter(map, interruptFlag);
     }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
index 6c5b63d85..73adec39f 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
@@ -21,6 +21,8 @@ import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Map;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -29,6 +31,7 @@ import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.system.MultiIterator;
 import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.tserver.FileManager.ScanFileManager;
@@ -40,10 +43,12 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
   private final IteratorScope scope;
   private final boolean fullMajorCompaction;
   private final AccumuloConfiguration config;
  private final ArrayList<SortedKeyValueIterator<Key,Value>> topLevelIterators = new ArrayList<SortedKeyValueIterator<Key,Value>>();
  private final ArrayList<SortedKeyValueIterator<Key,Value>> topLevelIterators;
   private Map<FileRef,DataFileValue> files;
 
   private final Authorizations authorizations; // these will only be supplied during scan scope
  private SamplerConfiguration samplerConfig;
  private boolean enableSampleForDeepCopy;
 
   public TabletIteratorEnvironment(IteratorScope scope, AccumuloConfiguration config) {
     if (scope == IteratorScope.majc)
@@ -54,10 +59,11 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
     this.config = config;
     this.fullMajorCompaction = false;
     this.authorizations = Authorizations.EMPTY;
    this.topLevelIterators = new ArrayList<>();
   }
 
  public TabletIteratorEnvironment(IteratorScope scope, AccumuloConfiguration config, ScanFileManager trm, Map<FileRef,DataFileValue> files,
      Authorizations authorizations) {
  private TabletIteratorEnvironment(IteratorScope scope, AccumuloConfiguration config, ScanFileManager trm, Map<FileRef,DataFileValue> files,
      Authorizations authorizations, SamplerConfigurationImpl samplerConfig, ArrayList<SortedKeyValueIterator<Key,Value>> topLevelIterators) {
     if (scope == IteratorScope.majc)
       throw new IllegalArgumentException("must set if compaction is full");
 
@@ -67,6 +73,19 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
     this.fullMajorCompaction = false;
     this.files = files;
     this.authorizations = authorizations;
    if (samplerConfig != null) {
      enableSampleForDeepCopy = true;
      this.samplerConfig = samplerConfig.toSamplerConfiguration();
    } else {
      enableSampleForDeepCopy = false;
    }

    this.topLevelIterators = topLevelIterators;
  }

  public TabletIteratorEnvironment(IteratorScope scope, AccumuloConfiguration config, ScanFileManager trm, Map<FileRef,DataFileValue> files,
      Authorizations authorizations, SamplerConfigurationImpl samplerConfig) {
    this(scope, config, trm, files, authorizations, samplerConfig, new ArrayList<SortedKeyValueIterator<Key,Value>>());
   }
 
   public TabletIteratorEnvironment(IteratorScope scope, boolean fullMajC, AccumuloConfiguration config) {
@@ -78,6 +97,7 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
     this.config = config;
     this.fullMajorCompaction = fullMajC;
     this.authorizations = Authorizations.EMPTY;
    this.topLevelIterators = new ArrayList<SortedKeyValueIterator<Key,Value>>();
   }
 
   @Override
@@ -100,7 +120,7 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
   @Override
   public SortedKeyValueIterator<Key,Value> reserveMapFileReader(String mapFileName) throws IOException {
     FileRef ref = new FileRef(mapFileName, new Path(mapFileName));
    return trm.openFiles(Collections.singletonMap(ref, files.get(ref)), false).get(0);
    return trm.openFiles(Collections.singletonMap(ref, files.get(ref)), false, null).get(0);
   }
 
   @Override
@@ -122,4 +142,37 @@ public class TabletIteratorEnvironment implements IteratorEnvironment {
     allIters.add(iter);
     return new MultiIterator(allIters, false);
   }

  @Override
  public boolean isSamplingEnabled() {
    return enableSampleForDeepCopy;
  }

  @Override
  public SamplerConfiguration getSamplerConfiguration() {
    if (samplerConfig == null) {
      // only create this once so that it stays the same, even if config changes
      SamplerConfigurationImpl sci = SamplerConfigurationImpl.newSamplerConfig(config);
      if (sci == null) {
        return null;
      }
      samplerConfig = sci.toSamplerConfiguration();
    }
    return samplerConfig;
  }

  @Override
  public IteratorEnvironment cloneWithSamplingEnabled() {
    if (!scope.equals(IteratorScope.scan)) {
      throw new UnsupportedOperationException();
    }

    SamplerConfigurationImpl sci = SamplerConfigurationImpl.newSamplerConfig(config);
    if (sci == null) {
      throw new SampleNotPresentException();
    }

    TabletIteratorEnvironment te = new TabletIteratorEnvironment(scope, config, trm, files, authorizations, sci, topLevelIterators);
    return te;
  }
 }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
index de89b5066..d35e6af23 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletServer.java
@@ -61,6 +61,7 @@ import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.impl.CompressedIterators;
 import org.apache.accumulo.core.client.impl.CompressedIterators.IterConfig;
 import org.apache.accumulo.core.client.impl.DurabilityImpl;
@@ -114,6 +115,7 @@ import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
 import org.apache.accumulo.core.replication.ReplicationConstants;
 import org.apache.accumulo.core.replication.thrift.ReplicationServicer;
 import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.security.thrift.TCredentials;
 import org.apache.accumulo.core.tabletserver.log.LogEntry;
@@ -123,6 +125,8 @@ import org.apache.accumulo.core.tabletserver.thrift.ConstraintViolationException
 import org.apache.accumulo.core.tabletserver.thrift.NoSuchScanIDException;
 import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
 import org.apache.accumulo.core.tabletserver.thrift.TDurability;
import org.apache.accumulo.core.tabletserver.thrift.TSampleNotPresentException;
import org.apache.accumulo.core.tabletserver.thrift.TSamplerConfiguration;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Iface;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Processor;
@@ -447,8 +451,8 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
     @Override
     public InitialScan startScan(TInfo tinfo, TCredentials credentials, TKeyExtent textent, TRange range, List<TColumn> columns, int batchSize,
         List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated,
        long readaheadThreshold, long batchTimeOut) throws NotServingTabletException, ThriftSecurityException,
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException {
        long readaheadThreshold, TSamplerConfiguration tSamplerConfig, long batchTimeOut) throws NotServingTabletException, ThriftSecurityException,
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException, TSampleNotPresentException {
 
       String tableId = new String(textent.getTable(), UTF_8);
       if (!security.canScan(credentials, tableId, Tables.getNamespaceId(getInstance(), tableId), range, columns, ssiList, ssio, authorizations))
@@ -480,10 +484,11 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       for (TColumn tcolumn : columns) {
         columnSet.add(new Column(tcolumn));
       }

       final ScanSession scanSession = new ScanSession(credentials, extent, columnSet, ssiList, ssio, new Authorizations(authorizations), readaheadThreshold,
           batchTimeOut);
       scanSession.scanner = tablet.createScanner(new Range(range), batchSize, scanSession.columnSet, scanSession.auths, ssiList, ssio, isolated,
          scanSession.interruptFlag, scanSession.batchTimeOut);
          scanSession.interruptFlag, SamplerConfigurationImpl.fromThrift(tSamplerConfig), scanSession.batchTimeOut);
 
       long sid = sessionManager.createSession(scanSession, true);
 
@@ -502,7 +507,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
 
     @Override
     public ScanResult continueScan(TInfo tinfo, long scanID) throws NoSuchScanIDException, NotServingTabletException,
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException {
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException, TSampleNotPresentException {
       ScanSession scanSession = (ScanSession) sessionManager.reserveSession(scanID);
       if (scanSession == null) {
         throw new NoSuchScanIDException();
@@ -516,7 +521,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
     }
 
     private ScanResult continueScan(TInfo tinfo, long scanID, ScanSession scanSession) throws NoSuchScanIDException, NotServingTabletException,
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException {
        org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException, TSampleNotPresentException {
 
       if (scanSession.nextBatchTask == null) {
         scanSession.nextBatchTask = new NextBatchTask(TabletServer.this, scanID, scanSession.interruptFlag);
@@ -533,6 +538,8 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
           throw (NotServingTabletException) e.getCause();
         else if (e.getCause() instanceof TooManyFilesException)
           throw new org.apache.accumulo.core.tabletserver.thrift.TooManyFilesException(scanSession.extent.toThrift());
        else if (e.getCause() instanceof SampleNotPresentException)
          throw new TSampleNotPresentException(scanSession.extent.toThrift());
         else if (e.getCause() instanceof IOException) {
           sleepUninterruptibly(MAX_TIME_TO_WAIT_FOR_SCAN_RESULT_MILLIS, TimeUnit.MILLISECONDS);
           List<KVEntry> empty = Collections.emptyList();
@@ -595,8 +602,8 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
 
     @Override
     public InitialMultiScan startMultiScan(TInfo tinfo, TCredentials credentials, Map<TKeyExtent,List<TRange>> tbatch, List<TColumn> tcolumns,
        List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut)
        throws ThriftSecurityException {
        List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites,
        TSamplerConfiguration tSamplerConfig, long batchTimeOut) throws ThriftSecurityException, TSampleNotPresentException {
       // find all of the tables that need to be scanned
       final HashSet<String> tables = new HashSet<String>();
       for (TKeyExtent keyExtent : tbatch.keySet()) {
@@ -627,7 +634,8 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       if (waitForWrites)
         writeTracker.waitForWrites(TabletType.type(batch.keySet()));
 
      final MultiScanSession mss = new MultiScanSession(credentials, threadPoolExtent, batch, ssiList, ssio, new Authorizations(authorizations), batchTimeOut);
      final MultiScanSession mss = new MultiScanSession(credentials, threadPoolExtent, batch, ssiList, ssio, new Authorizations(authorizations),
          SamplerConfigurationImpl.fromThrift(tSamplerConfig), batchTimeOut);
 
       mss.numTablets = batch.size();
       for (List<Range> ranges : batch.values()) {
@@ -653,7 +661,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
     }
 
     @Override
    public MultiScanResult continueMultiScan(TInfo tinfo, long scanID) throws NoSuchScanIDException {
    public MultiScanResult continueMultiScan(TInfo tinfo, long scanID) throws NoSuchScanIDException, TSampleNotPresentException {
 
       MultiScanSession session = (MultiScanSession) sessionManager.reserveSession(scanID);
 
@@ -668,7 +676,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
       }
     }
 
    private MultiScanResult continueMultiScan(TInfo tinfo, long scanID, MultiScanSession session) throws NoSuchScanIDException {
    private MultiScanResult continueMultiScan(TInfo tinfo, long scanID, MultiScanSession session) throws NoSuchScanIDException, TSampleNotPresentException {
 
       if (session.lookupTask == null) {
         session.lookupTask = new LookupTask(TabletServer.this, scanID);
@@ -679,6 +687,14 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
         MultiScanResult scanResult = session.lookupTask.get(MAX_TIME_TO_WAIT_FOR_SCAN_RESULT_MILLIS, TimeUnit.MILLISECONDS);
         session.lookupTask = null;
         return scanResult;
      } catch (ExecutionException e) {
        sessionManager.removeSession(scanID);
        if (e.getCause() instanceof SampleNotPresentException) {
          throw new TSampleNotPresentException();
        } else {
          log.warn("Failed to get multiscan result", e);
          throw new RuntimeException(e);
        }
       } catch (TimeoutException e1) {
         long timeout = TabletServer.this.getConfiguration().getTimeInMillis(Property.TSERV_CLIENT_TIMEOUT);
         sessionManager.removeIfNotAccessed(scanID, timeout);
@@ -1116,7 +1132,7 @@ public class TabletServer extends AccumuloServerContext implements Runnable {
 
         IterConfig ic = compressedIters.decompress(tc.iterators);
 
        Scanner scanner = tablet.createScanner(range, 1, EMPTY_COLUMNS, cs.auths, ic.ssiList, ic.ssio, false, cs.interruptFlag, 0);
        Scanner scanner = tablet.createScanner(range, 1, EMPTY_COLUMNS, cs.auths, ic.ssiList, ic.ssio, false, cs.interruptFlag, null, 0);
 
         try {
           ScanBatch batch = scanner.read();
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/strategies/ConfigurableCompactionStrategy.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/strategies/ConfigurableCompactionStrategy.java
index b97b88be3..04915eff1 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/strategies/ConfigurableCompactionStrategy.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/compaction/strategies/ConfigurableCompactionStrategy.java
@@ -26,7 +26,10 @@ import java.util.Set;
 import java.util.regex.Pattern;
 
 import org.apache.accumulo.core.compaction.CompactionSettings;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.file.FileSKVIterator;
 import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.accumulo.tserver.compaction.CompactionPlan;
 import org.apache.accumulo.tserver.compaction.CompactionStrategy;
@@ -40,6 +43,22 @@ public class ConfigurableCompactionStrategy extends CompactionStrategy {
     boolean shouldCompact(Entry<FileRef,DataFileValue> file, MajorCompactionRequest request);
   }
 
  private static class NoSampleTest implements Test {

    @Override
    public boolean shouldCompact(Entry<FileRef,DataFileValue> file, MajorCompactionRequest request) {
      try (FileSKVIterator reader = request.openReader(file.getKey())) {
        SamplerConfigurationImpl sc = SamplerConfigurationImpl.newSamplerConfig(new ConfigurationCopy(request.getTableProperties()));
        if (sc == null) {
          return false;
        }
        return reader.getSample(sc) == null;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

   private static abstract class FileSizeTest implements Test {
     private final long esize;
 
@@ -83,6 +102,9 @@ public class ConfigurableCompactionStrategy extends CompactionStrategy {
     for (Entry<String,String> entry : es) {
 
       switch (CompactionSettings.valueOf(entry.getKey())) {
        case SF_NO_SAMPLE:
          tests.add(new NoSampleTest());
          break;
         case SF_LT_ESIZE_OPT:
           tests.add(new FileSizeTest(entry.getValue()) {
             @Override
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/LookupTask.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/LookupTask.java
index 57a09ce26..2d745cb61 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/LookupTask.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/LookupTask.java
@@ -25,6 +25,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.impl.Translator;
 import org.apache.accumulo.core.client.impl.Translators;
 import org.apache.accumulo.core.conf.Property;
@@ -111,7 +112,7 @@ public class LookupTask extends ScanTask<MultiScanResult> {
             interruptFlag.set(true);
 
           lookupResult = tablet.lookup(entry.getValue(), session.columnSet, session.auths, results, maxResultsSize - bytesAdded, session.ssiList, session.ssio,
              interruptFlag, session.batchTimeOut);
              interruptFlag, session.samplerConfig, session.batchTimeOut);
 
           // if the tablet was closed it it possible that the
           // interrupt flag was set.... do not want it set for
@@ -163,6 +164,8 @@ public class LookupTask extends ScanTask<MultiScanResult> {
         log.warn("Iteration interrupted, when scan not cancelled", iie);
         addResult(iie);
       }
    } catch (SampleNotPresentException e) {
      addResult(e);
     } catch (Throwable e) {
       log.warn("exception while doing multi-scan ", e);
       addResult(e);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/NextBatchTask.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/NextBatchTask.java
index e3f414615..ec2836763 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/NextBatchTask.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/scan/NextBatchTask.java
@@ -18,6 +18,7 @@ package org.apache.accumulo.tserver.scan;
 
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.iterators.IterationInterruptedException;
 import org.apache.accumulo.server.util.Halt;
 import org.apache.accumulo.tserver.TabletServer;
@@ -84,8 +85,8 @@ public class NextBatchTask extends ScanTask<ScanBatch> {
         log.warn("Iteration interrupted, when scan not cancelled", iie);
         addResult(iie);
       }
    } catch (TooManyFilesException tmfe) {
      addResult(tmfe);
    } catch (TooManyFilesException | SampleNotPresentException e) {
      addResult(e);
     } catch (OutOfMemoryError ome) {
       Halt.halt("Ran out of memory scanning " + scanSession.extent + " for " + scanSession.client);
       addResult(ome);
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
index fccac475a..16fc21821 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
@@ -20,6 +20,7 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.impl.KeyExtent;
@@ -36,6 +37,7 @@ public class MultiScanSession extends Session {
   public final List<IterInfo> ssiList;
   public final Map<String,Map<String,String>> ssio;
   public final Authorizations auths;
  public final SamplerConfiguration samplerConfig;
   public final long batchTimeOut;
 
   // stats
@@ -47,13 +49,14 @@ public class MultiScanSession extends Session {
   public volatile ScanTask<MultiScanResult> lookupTask;
 
   public MultiScanSession(TCredentials credentials, KeyExtent threadPoolExtent, Map<KeyExtent,List<Range>> queries, List<IterInfo> ssiList,
      Map<String,Map<String,String>> ssio, Authorizations authorizations, long batchTimeOut) {
      Map<String,Map<String,String>> ssio, Authorizations authorizations, SamplerConfiguration samplerConfig, long batchTimeOut) {
     super(credentials);
     this.queries = queries;
     this.ssiList = ssiList;
     this.ssio = ssio;
     this.auths = authorizations;
     this.threadPoolExtent = threadPoolExtent;
    this.samplerConfig = samplerConfig;
     this.batchTimeOut = batchTimeOut;
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
index 853714af0..72c289c07 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
@@ -24,6 +24,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
@@ -42,6 +43,7 @@ import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator.DataSou
 import org.apache.accumulo.core.iterators.system.StatsIterator;
 import org.apache.accumulo.core.iterators.system.VisibilityFilter;
 import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.server.fs.FileRef;
@@ -50,6 +52,8 @@ import org.apache.accumulo.tserver.InMemoryMap.MemoryIterator;
 import org.apache.accumulo.tserver.TabletIteratorEnvironment;
 import org.apache.accumulo.tserver.TabletServer;
 
import com.google.common.collect.Iterables;

 class ScanDataSource implements DataSource {
 
   // data source state
@@ -65,10 +69,10 @@ class ScanDataSource implements DataSource {
   private final ScanOptions options;
 
   ScanDataSource(Tablet tablet, Authorizations authorizations, byte[] defaultLabels, HashSet<Column> columnSet, List<IterInfo> ssiList,
      Map<String,Map<String,String>> ssio, AtomicBoolean interruptFlag, long batchTimeOut) {
      Map<String,Map<String,String>> ssio, AtomicBoolean interruptFlag, SamplerConfiguration samplerConfig, long batchTimeOut) {
     this.tablet = tablet;
     expectedDeletionCount = tablet.getDataSourceDeletions();
    this.options = new ScanOptions(-1, authorizations, defaultLabels, columnSet, ssiList, ssio, interruptFlag, false, batchTimeOut);
    this.options = new ScanOptions(-1, authorizations, defaultLabels, columnSet, ssiList, ssio, interruptFlag, false, samplerConfig, batchTimeOut);
     this.interruptFlag = interruptFlag;
   }
 
@@ -117,6 +121,8 @@ class ScanDataSource implements DataSource {
 
     Map<FileRef,DataFileValue> files;
 
    SamplerConfigurationImpl samplerConfig = options.getSamplerConfigurationImpl();

     synchronized (tablet) {
 
       if (memIters != null)
@@ -141,26 +147,26 @@ class ScanDataSource implements DataSource {
       // getIterators() throws an exception
       expectedDeletionCount = tablet.getDataSourceDeletions();
 
      memIters = tablet.getTabletMemory().getIterators();
      memIters = tablet.getTabletMemory().getIterators(samplerConfig);
       Pair<Long,Map<FileRef,DataFileValue>> reservation = tablet.getDatafileManager().reserveFilesForScan();
       fileReservationId = reservation.getFirst();
       files = reservation.getSecond();
     }
 
    Collection<InterruptibleIterator> mapfiles = fileManager.openFiles(files, options.isIsolated());
    Collection<InterruptibleIterator> mapfiles = fileManager.openFiles(files, options.isIsolated(), samplerConfig);

    for (SortedKeyValueIterator<Key,Value> skvi : Iterables.concat(mapfiles, memIters))
      ((InterruptibleIterator) skvi).setInterruptFlag(interruptFlag);
 
     List<SortedKeyValueIterator<Key,Value>> iters = new ArrayList<SortedKeyValueIterator<Key,Value>>(mapfiles.size() + memIters.size());
 
     iters.addAll(mapfiles);
     iters.addAll(memIters);
 
    for (SortedKeyValueIterator<Key,Value> skvi : iters)
      ((InterruptibleIterator) skvi).setInterruptFlag(interruptFlag);

     MultiIterator multiIter = new MultiIterator(iters, tablet.getExtent());
 
     TabletIteratorEnvironment iterEnv = new TabletIteratorEnvironment(IteratorScope.scan, tablet.getTableConfiguration(), fileManager, files,
        options.getAuthorizations());
        options.getAuthorizations(), samplerConfig);
 
     statsIterator = new StatsIterator(multiIter, TabletServer.seekCount, tablet.getScannedCounter());
 
@@ -212,7 +218,7 @@ class ScanDataSource implements DataSource {
 
   public void reattachFileManager() throws IOException {
     if (fileManager != null)
      fileManager.reattach();
      fileManager.reattach(options.getSamplerConfigurationImpl());
   }
 
   public void detachFileManager() {
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
index 2a38fbddd..c97f3acf2 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
@@ -21,8 +21,10 @@ import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.security.Authorizations;
 
 final class ScanOptions {
@@ -35,10 +37,11 @@ final class ScanOptions {
   private final AtomicBoolean interruptFlag;
   private final int num;
   private final boolean isolated;
  private SamplerConfiguration samplerConfig;
   private final long batchTimeOut;
 
   ScanOptions(int num, Authorizations authorizations, byte[] defaultLabels, Set<Column> columnSet, List<IterInfo> ssiList, Map<String,Map<String,String>> ssio,
      AtomicBoolean interruptFlag, boolean isolated, long batchTimeOut) {
      AtomicBoolean interruptFlag, boolean isolated, SamplerConfiguration samplerConfig, long batchTimeOut) {
     this.num = num;
     this.authorizations = authorizations;
     this.defaultLabels = defaultLabels;
@@ -47,6 +50,7 @@ final class ScanOptions {
     this.ssio = ssio;
     this.interruptFlag = interruptFlag;
     this.isolated = isolated;
    this.samplerConfig = samplerConfig;
     this.batchTimeOut = batchTimeOut;
   }
 
@@ -82,6 +86,16 @@ final class ScanOptions {
     return isolated;
   }
 
  public SamplerConfiguration getSamplerConfiguration() {
    return samplerConfig;
  }

  public SamplerConfigurationImpl getSamplerConfigurationImpl() {
    if (samplerConfig == null)
      return null;
    return new SamplerConfigurationImpl(samplerConfig);
  }

   public long getBatchTimeOut() {
     return batchTimeOut;
   }
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
index b8c260d42..1f66302db 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
@@ -51,6 +51,7 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.DurabilityImpl;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
@@ -632,7 +633,8 @@ public class Tablet implements TabletCommitter {
   }
 
   public LookupResult lookup(List<Range> ranges, HashSet<Column> columns, Authorizations authorizations, List<KVEntry> results, long maxResultSize,
      List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, AtomicBoolean interruptFlag, long batchTimeOut) throws IOException {
      List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, AtomicBoolean interruptFlag, SamplerConfiguration samplerConfig, long batchTimeOut)
      throws IOException {
 
     if (ranges.size() == 0) {
       return new LookupResult();
@@ -650,7 +652,8 @@ public class Tablet implements TabletCommitter {
       tabletRange.clip(range);
     }
 
    ScanDataSource dataSource = new ScanDataSource(this, authorizations, this.defaultSecurityLabel, columns, ssiList, ssio, interruptFlag, batchTimeOut);
    ScanDataSource dataSource = new ScanDataSource(this, authorizations, this.defaultSecurityLabel, columns, ssiList, ssio, interruptFlag, samplerConfig,
        batchTimeOut);
 
     LookupResult result = null;
 
@@ -754,12 +757,13 @@ public class Tablet implements TabletCommitter {
   }
 
   public Scanner createScanner(Range range, int num, Set<Column> columns, Authorizations authorizations, List<IterInfo> ssiList,
      Map<String,Map<String,String>> ssio, boolean isolated, AtomicBoolean interruptFlag, long batchTimeOut) {
      Map<String,Map<String,String>> ssio, boolean isolated, AtomicBoolean interruptFlag, SamplerConfiguration samplerConfig, long batchTimeOut) {
     // do a test to see if this range falls within the tablet, if it does not
     // then clip will throw an exception
     extent.toDataRange().clip(range);
 
    ScanOptions opts = new ScanOptions(num, authorizations, this.defaultSecurityLabel, columns, ssiList, ssio, interruptFlag, isolated, batchTimeOut);
    ScanOptions opts = new ScanOptions(num, authorizations, this.defaultSecurityLabel, columns, ssiList, ssio, interruptFlag, isolated, samplerConfig,
        batchTimeOut);
     return new Scanner(this, range, opts);
   }
 
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/TabletMemory.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/TabletMemory.java
index 0b39d40bf..86cc2626a 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/TabletMemory.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/TabletMemory.java
@@ -22,6 +22,7 @@ import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.util.LocalityGroupUtil.LocalityGroupConfigurationError;
 import org.apache.accumulo.tserver.InMemoryMap;
 import org.apache.accumulo.tserver.InMemoryMap.MemoryIterator;
@@ -156,11 +157,11 @@ class TabletMemory implements Closeable {
     tablet.updateMemoryUsageStats(memTable.estimatedSizeInBytes(), other);
   }
 
  public List<MemoryIterator> getIterators() {
  public List<MemoryIterator> getIterators(SamplerConfigurationImpl samplerConfig) {
     List<MemoryIterator> toReturn = new ArrayList<MemoryIterator>(2);
    toReturn.add(memTable.skvIterator());
    toReturn.add(memTable.skvIterator(samplerConfig));
     if (otherMemTable != null)
      toReturn.add(otherMemTable.skvIterator());
      toReturn.add(otherMemTable.skvIterator(samplerConfig));
     return toReturn;
   }
 
diff --git a/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java b/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
index da7157afd..7b4d447e5 100644
-- a/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
++ b/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
@@ -26,16 +26,22 @@ import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
 import java.util.Set;
import java.util.TreeMap;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
@@ -45,21 +51,56 @@ import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.IterationInterruptedException;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.core.util.LocalityGroupUtil.LocalityGroupConfigurationError;
 import org.apache.accumulo.server.client.HdfsZooInstance;
 import org.apache.accumulo.server.conf.ZooConfiguration;
 import org.apache.accumulo.tserver.InMemoryMap.MemoryIterator;
 import org.apache.hadoop.io.Text;
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
import org.junit.Assert;
 import org.junit.BeforeClass;
 import org.junit.Ignore;
 import org.junit.Rule;
 import org.junit.Test;
import org.junit.rules.ExpectedException;
 import org.junit.rules.TemporaryFolder;
 
import com.google.common.collect.ImmutableMap;

 public class InMemoryMapTest {
 
  private static class SampleIE extends BaseIteratorEnvironment {

    private final SamplerConfiguration sampleConfig;

    public SampleIE() {
      this.sampleConfig = null;
    }

    public SampleIE(SamplerConfigurationImpl sampleConfig) {
      this.sampleConfig = sampleConfig.toSamplerConfiguration();
    }

    @Override
    public boolean isSamplingEnabled() {
      return sampleConfig != null;
    }

    @Override
    public SamplerConfiguration getSamplerConfiguration() {
      return sampleConfig;
    }
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

   @BeforeClass
   public static void setUp() throws Exception {
     // suppress log messages having to do with not having an instance
@@ -101,20 +142,42 @@ public class InMemoryMapTest {
   }
 
   static Set<ByteSequence> newCFSet(String... cfs) {
    HashSet<ByteSequence> cfSet = new HashSet<ByteSequence>();
    HashSet<ByteSequence> cfSet = new HashSet<>();
     for (String cf : cfs) {
       cfSet.add(new ArrayByteSequence(cf));
     }
     return cfSet;
   }
 
  static Set<Text> toTextSet(String... cfs) {
    HashSet<Text> cfSet = new HashSet<>();
    for (String cf : cfs) {
      cfSet.add(new Text(cf));
    }
    return cfSet;
  }

  static ConfigurationCopy newConfig(String memDumpDir) {
    ConfigurationCopy config = new ConfigurationCopy(DefaultConfiguration.getInstance());
    config.set(Property.TSERV_NATIVEMAP_ENABLED, "" + false);
    config.set(Property.TSERV_MEMDUMP_DIR, memDumpDir);
    return config;
  }

  static InMemoryMap newInMemoryMap(boolean useNative, String memDumpDir) throws LocalityGroupConfigurationError {
    ConfigurationCopy config = new ConfigurationCopy(DefaultConfiguration.getInstance());
    config.set(Property.TSERV_NATIVEMAP_ENABLED, "" + useNative);
    config.set(Property.TSERV_MEMDUMP_DIR, memDumpDir);
    return new InMemoryMap(config);
  }

   @Test
   public void test2() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
    MemoryIterator ski2 = imm.skvIterator();
    MemoryIterator ski2 = imm.skvIterator(null);
 
     ski1.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
     assertFalse(ski1.hasTop());
@@ -128,17 +191,17 @@ public class InMemoryMapTest {
 
   @Test
   public void test3() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq1", 3, "bar2");
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
     mutate(imm, "r1", "foo:cq1", 3, "bar3");
 
     mutate(imm, "r3", "foo:cq1", 3, "bar9");
     mutate(imm, "r3", "foo:cq1", 3, "bara");
 
    MemoryIterator ski2 = imm.skvIterator();
    MemoryIterator ski2 = imm.skvIterator(null);
 
     ski1.seek(new Range(new Text("r1")), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(ski1, "r1", "foo:cq1", 3, "bar2");
@@ -154,11 +217,11 @@ public class InMemoryMapTest {
 
   @Test
   public void test4() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq1", 3, "bar2");
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
     mutate(imm, "r1", "foo:cq1", 3, "bar3");
 
     imm.delete(0);
@@ -186,13 +249,13 @@ public class InMemoryMapTest {
 
   @Test
   public void test5() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq1", 3, "bar2");
     mutate(imm, "r1", "foo:cq1", 3, "bar3");
 
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
     ski1.seek(new Range(new Text("r1")), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(ski1, "r1", "foo:cq1", 3, "bar3");
 
@@ -204,13 +267,13 @@ public class InMemoryMapTest {
 
     ski1.close();
 
    imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq2", 3, "bar2");
     mutate(imm, "r1", "foo:cq3", 3, "bar3");
 
    ski1 = imm.skvIterator();
    ski1 = imm.skvIterator(null);
     ski1.seek(new Range(new Text("r1")), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(ski1, "r1", "foo:cq1", 3, "bar1");
 
@@ -225,18 +288,18 @@ public class InMemoryMapTest {
 
   @Test
   public void test6() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq2", 3, "bar2");
     mutate(imm, "r1", "foo:cq3", 3, "bar3");
     mutate(imm, "r1", "foo:cq4", 3, "bar4");
 
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
 
     mutate(imm, "r1", "foo:cq5", 3, "bar5");
 
    SortedKeyValueIterator<Key,Value> dc = ski1.deepCopy(null);
    SortedKeyValueIterator<Key,Value> dc = ski1.deepCopy(new SampleIE());
 
     ski1.seek(new Range(nk("r1", "foo:cq1", 3), null), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(ski1, "r1", "foo:cq1", 3, "bar1");
@@ -271,12 +334,12 @@ public class InMemoryMapTest {
   private void deepCopyAndDelete(int interleaving, boolean interrupt) throws Exception {
     // interleaving == 0 intentionally omitted, this runs the test w/o deleting in mem map
 
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq2", 3, "bar2");
 
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
 
     AtomicBoolean iflag = new AtomicBoolean(false);
     ski1.setInterruptFlag(iflag);
@@ -287,7 +350,7 @@ public class InMemoryMapTest {
         iflag.set(true);
     }
 
    SortedKeyValueIterator<Key,Value> dc = ski1.deepCopy(null);
    SortedKeyValueIterator<Key,Value> dc = ski1.deepCopy(new SampleIE());
 
     if (interleaving == 2) {
       imm.delete(0);
@@ -338,7 +401,7 @@ public class InMemoryMapTest {
 
   @Test
   public void testBug1() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     for (int i = 0; i < 20; i++) {
       mutate(imm, "r1", "foo:cq" + i, 3, "bar" + i);
@@ -348,7 +411,7 @@ public class InMemoryMapTest {
       mutate(imm, "r2", "foo:cq" + i, 3, "bar" + i);
     }
 
    MemoryIterator ski1 = imm.skvIterator();
    MemoryIterator ski1 = imm.skvIterator(null);
     ColumnFamilySkippingIterator cfsi = new ColumnFamilySkippingIterator(ski1);
 
     imm.delete(0);
@@ -366,14 +429,14 @@ public class InMemoryMapTest {
 
   @Test
   public void testSeekBackWards() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     mutate(imm, "r1", "foo:cq1", 3, "bar1");
     mutate(imm, "r1", "foo:cq2", 3, "bar2");
     mutate(imm, "r1", "foo:cq3", 3, "bar3");
     mutate(imm, "r1", "foo:cq4", 3, "bar4");
 
    MemoryIterator skvi1 = imm.skvIterator();
    MemoryIterator skvi1 = imm.skvIterator(null);
 
     skvi1.seek(new Range(nk("r1", "foo:cq3", 3), null), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(skvi1, "r1", "foo:cq3", 3, "bar3");
@@ -385,14 +448,14 @@ public class InMemoryMapTest {
 
   @Test
   public void testDuplicateKey() throws Exception {
    InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
 
     Mutation m = new Mutation(new Text("r1"));
     m.put(new Text("foo"), new Text("cq"), 3, new Value("v1".getBytes()));
     m.put(new Text("foo"), new Text("cq"), 3, new Value("v2".getBytes()));
     imm.mutate(Collections.singletonList(m));
 
    MemoryIterator skvi1 = imm.skvIterator();
    MemoryIterator skvi1 = imm.skvIterator(null);
     skvi1.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
     ae(skvi1, "r1", "foo:cq", 3, "v2");
     ae(skvi1, "r1", "foo:cq", 3, "v1");
@@ -410,12 +473,12 @@ public class InMemoryMapTest {
   // - hard to get this timing test to run well on apache build machines
   @Test
   @Ignore
  public void parallelWriteSpeed() throws InterruptedException, IOException {
  public void parallelWriteSpeed() throws Exception {
     List<Double> timings = new ArrayList<Double>();
     for (int threads : new int[] {1, 2, 16, /* 64, 256 */}) {
       final long now = System.currentTimeMillis();
       final long counts[] = new long[threads];
      final InMemoryMap imm = new InMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
      final InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());
       ExecutorService e = Executors.newFixedThreadPool(threads);
       for (int j = 0; j < threads; j++) {
         final int threadId = j;
@@ -451,12 +514,12 @@ public class InMemoryMapTest {
 
   @Test
   public void testLocalityGroups() throws Exception {
    ConfigurationCopy config = newConfig(tempFolder.newFolder().getAbsolutePath());
    config.set(Property.TABLE_LOCALITY_GROUP_PREFIX + "lg1", LocalityGroupUtil.encodeColumnFamilies(toTextSet("cf1", "cf2")));
    config.set(Property.TABLE_LOCALITY_GROUP_PREFIX + "lg2", LocalityGroupUtil.encodeColumnFamilies(toTextSet("cf3", "cf4")));
    config.set(Property.TABLE_LOCALITY_GROUPS.getKey(), "lg1,lg2");
 
    Map<String,Set<ByteSequence>> lggroups1 = new HashMap<String,Set<ByteSequence>>();
    lggroups1.put("lg1", newCFSet("cf1", "cf2"));
    lggroups1.put("lg2", newCFSet("cf3", "cf4"));

    InMemoryMap imm = new InMemoryMap(lggroups1, false, tempFolder.newFolder().getAbsolutePath());
    InMemoryMap imm = new InMemoryMap(config);
 
     Mutation m1 = new Mutation("r1");
     m1.put("cf1", "x", 2, "1");
@@ -480,10 +543,10 @@ public class InMemoryMapTest {
 
     imm.mutate(Arrays.asList(m1, m2, m3, m4, m5));
 
    MemoryIterator iter1 = imm.skvIterator();
    MemoryIterator iter1 = imm.skvIterator(null);
 
     seekLocalityGroups(iter1);
    SortedKeyValueIterator<Key,Value> dc1 = iter1.deepCopy(null);
    SortedKeyValueIterator<Key,Value> dc1 = iter1.deepCopy(new SampleIE());
     seekLocalityGroups(dc1);
 
     assertTrue(imm.getNumEntries() == 10);
@@ -497,6 +560,254 @@ public class InMemoryMapTest {
     // seekLocalityGroups(iter1.deepCopy(null));
   }
 
  @Test
  public void testSample() throws Exception {

    SamplerConfigurationImpl sampleConfig = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "7"));
    Sampler sampler = SamplerFactory.newSampler(sampleConfig, DefaultConfiguration.getInstance());

    ConfigurationCopy config1 = newConfig(tempFolder.newFolder().getAbsolutePath());
    for (Entry<String,String> entry : sampleConfig.toTablePropertiesMap().entrySet()) {
      config1.set(entry.getKey(), entry.getValue());
    }

    ConfigurationCopy config2 = newConfig(tempFolder.newFolder().getAbsolutePath());
    config2.set(Property.TABLE_LOCALITY_GROUP_PREFIX + "lg1", LocalityGroupUtil.encodeColumnFamilies(toTextSet("cf2")));
    config2.set(Property.TABLE_LOCALITY_GROUPS.getKey(), "lg1");
    for (Entry<String,String> entry : sampleConfig.toTablePropertiesMap().entrySet()) {
      config2.set(entry.getKey(), entry.getValue());
    }

    for (ConfigurationCopy config : Arrays.asList(config1, config2)) {

      InMemoryMap imm = new InMemoryMap(config);

      TreeMap<Key,Value> expectedSample = new TreeMap<>();
      TreeMap<Key,Value> expectedAll = new TreeMap<>();
      TreeMap<Key,Value> expectedNone = new TreeMap<>();

      MemoryIterator iter0 = imm.skvIterator(sampleConfig);

      for (int r = 0; r < 100; r++) {
        String row = String.format("r%06d", r);
        mutate(imm, row, "cf1:cq1", 5, "v" + (2 * r), sampler, expectedSample, expectedAll);
        mutate(imm, row, "cf2:cq2", 5, "v" + ((2 * r) + 1), sampler, expectedSample, expectedAll);
      }

      assertTrue(expectedSample.size() > 0);

      MemoryIterator iter1 = imm.skvIterator(sampleConfig);
      MemoryIterator iter2 = imm.skvIterator(null);
      SortedKeyValueIterator<Key,Value> iter0dc1 = iter0.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter0dc2 = iter0.deepCopy(new SampleIE(sampleConfig));
      SortedKeyValueIterator<Key,Value> iter1dc1 = iter1.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter1dc2 = iter1.deepCopy(new SampleIE(sampleConfig));
      SortedKeyValueIterator<Key,Value> iter2dc1 = iter2.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter2dc2 = iter2.deepCopy(new SampleIE(sampleConfig));

      assertEquals(expectedNone, readAll(iter0));
      assertEquals(expectedNone, readAll(iter0dc1));
      assertEquals(expectedNone, readAll(iter0dc2));
      assertEquals(expectedSample, readAll(iter1));
      assertEquals(expectedAll, readAll(iter2));
      assertEquals(expectedAll, readAll(iter1dc1));
      assertEquals(expectedAll, readAll(iter2dc1));
      assertEquals(expectedSample, readAll(iter1dc2));
      assertEquals(expectedSample, readAll(iter2dc2));

      imm.delete(0);

      assertEquals(expectedNone, readAll(iter0));
      assertEquals(expectedNone, readAll(iter0dc1));
      assertEquals(expectedNone, readAll(iter0dc2));
      assertEquals(expectedSample, readAll(iter1));
      assertEquals(expectedAll, readAll(iter2));
      assertEquals(expectedAll, readAll(iter1dc1));
      assertEquals(expectedAll, readAll(iter2dc1));
      assertEquals(expectedSample, readAll(iter1dc2));
      assertEquals(expectedSample, readAll(iter2dc2));

      SortedKeyValueIterator<Key,Value> iter0dc3 = iter0.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter0dc4 = iter0.deepCopy(new SampleIE(sampleConfig));
      SortedKeyValueIterator<Key,Value> iter1dc3 = iter1.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter1dc4 = iter1.deepCopy(new SampleIE(sampleConfig));
      SortedKeyValueIterator<Key,Value> iter2dc3 = iter2.deepCopy(new SampleIE());
      SortedKeyValueIterator<Key,Value> iter2dc4 = iter2.deepCopy(new SampleIE(sampleConfig));

      assertEquals(expectedNone, readAll(iter0dc3));
      assertEquals(expectedNone, readAll(iter0dc4));
      assertEquals(expectedAll, readAll(iter1dc3));
      assertEquals(expectedAll, readAll(iter2dc3));
      assertEquals(expectedSample, readAll(iter1dc4));
      assertEquals(expectedSample, readAll(iter2dc4));

      iter1.close();
      iter2.close();
    }
  }

  @Test
  public void testInterruptingSample() throws Exception {
    runInterruptSampleTest(false, false, false);
    runInterruptSampleTest(false, true, false);
    runInterruptSampleTest(true, false, false);
    runInterruptSampleTest(true, true, false);
    runInterruptSampleTest(true, true, true);
  }

  private void runInterruptSampleTest(boolean deepCopy, boolean delete, boolean dcAfterDelete) throws Exception {
    SamplerConfigurationImpl sampleConfig1 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "2"));
    Sampler sampler = SamplerFactory.newSampler(sampleConfig1, DefaultConfiguration.getInstance());

    ConfigurationCopy config1 = newConfig(tempFolder.newFolder().getAbsolutePath());
    for (Entry<String,String> entry : sampleConfig1.toTablePropertiesMap().entrySet()) {
      config1.set(entry.getKey(), entry.getValue());
    }

    InMemoryMap imm = new InMemoryMap(config1);

    TreeMap<Key,Value> expectedSample = new TreeMap<>();
    TreeMap<Key,Value> expectedAll = new TreeMap<>();

    for (int r = 0; r < 1000; r++) {
      String row = String.format("r%06d", r);
      mutate(imm, row, "cf1:cq1", 5, "v" + (2 * r), sampler, expectedSample, expectedAll);
      mutate(imm, row, "cf2:cq2", 5, "v" + ((2 * r) + 1), sampler, expectedSample, expectedAll);
    }

    assertTrue(expectedSample.size() > 0);

    MemoryIterator miter = imm.skvIterator(sampleConfig1);
    AtomicBoolean iFlag = new AtomicBoolean(false);
    miter.setInterruptFlag(iFlag);
    SortedKeyValueIterator<Key,Value> iter = miter;

    if (delete && !dcAfterDelete) {
      imm.delete(0);
    }

    if (deepCopy) {
      iter = iter.deepCopy(new SampleIE(sampleConfig1));
    }

    if (delete && dcAfterDelete) {
      imm.delete(0);
    }

    assertEquals(expectedSample, readAll(iter));
    iFlag.set(true);
    try {
      readAll(iter);
      Assert.fail();
    } catch (IterationInterruptedException iie) {}

    miter.close();
  }

  private void mutate(InMemoryMap imm, String row, String cols, int ts, String val, Sampler sampler, TreeMap<Key,Value> expectedSample,
      TreeMap<Key,Value> expectedAll) {
    mutate(imm, row, cols, ts, val);
    Key k1 = nk(row, cols, ts);
    if (sampler.accept(k1)) {
      expectedSample.put(k1, new Value(val.getBytes()));
    }
    expectedAll.put(k1, new Value(val.getBytes()));
  }

  @Test(expected = SampleNotPresentException.class)
  public void testDifferentSampleConfig() throws Exception {
    SamplerConfigurationImpl sampleConfig = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "7"));

    ConfigurationCopy config1 = newConfig(tempFolder.newFolder().getAbsolutePath());
    for (Entry<String,String> entry : sampleConfig.toTablePropertiesMap().entrySet()) {
      config1.set(entry.getKey(), entry.getValue());
    }

    InMemoryMap imm = new InMemoryMap(config1);

    mutate(imm, "r", "cf:cq", 5, "b");

    SamplerConfigurationImpl sampleConfig2 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "9"));
    MemoryIterator iter = imm.skvIterator(sampleConfig2);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
  }

  @Test(expected = SampleNotPresentException.class)
  public void testNoSampleConfig() throws Exception {
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());

    mutate(imm, "r", "cf:cq", 5, "b");

    SamplerConfigurationImpl sampleConfig2 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "9"));
    MemoryIterator iter = imm.skvIterator(sampleConfig2);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
  }

  @Test
  public void testEmptyNoSampleConfig() throws Exception {
    InMemoryMap imm = newInMemoryMap(false, tempFolder.newFolder().getAbsolutePath());

    SamplerConfigurationImpl sampleConfig2 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "9"));

    // when in mem map is empty should be able to get sample iterator with any sample config
    MemoryIterator iter = imm.skvIterator(sampleConfig2);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
    Assert.assertFalse(iter.hasTop());
  }

  @Test
  public void testDeferredSamplerCreation() throws Exception {
    SamplerConfigurationImpl sampleConfig1 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "9"));

    ConfigurationCopy config1 = newConfig(tempFolder.newFolder().getAbsolutePath());
    for (Entry<String,String> entry : sampleConfig1.toTablePropertiesMap().entrySet()) {
      config1.set(entry.getKey(), entry.getValue());
    }

    InMemoryMap imm = new InMemoryMap(config1);

    // change sampler config after creating in mem map.
    SamplerConfigurationImpl sampleConfig2 = new SamplerConfigurationImpl(RowSampler.class.getName(), ImmutableMap.of("hasher", "murmur3_32", "modulus", "7"));
    for (Entry<String,String> entry : sampleConfig2.toTablePropertiesMap().entrySet()) {
      config1.set(entry.getKey(), entry.getValue());
    }

    TreeMap<Key,Value> expectedSample = new TreeMap<>();
    TreeMap<Key,Value> expectedAll = new TreeMap<>();
    Sampler sampler = SamplerFactory.newSampler(sampleConfig2, config1);

    for (int i = 0; i < 100; i++) {
      mutate(imm, "r" + i, "cf:cq", 5, "v" + i, sampler, expectedSample, expectedAll);
    }

    MemoryIterator iter = imm.skvIterator(sampleConfig2);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
    Assert.assertEquals(expectedSample, readAll(iter));

    SortedKeyValueIterator<Key,Value> dc = iter.deepCopy(new SampleIE(sampleConfig2));
    dc.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
    Assert.assertEquals(expectedSample, readAll(dc));

    iter = imm.skvIterator(null);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
    Assert.assertEquals(expectedAll, readAll(iter));

    iter = imm.skvIterator(sampleConfig1);
    thrown.expect(SampleNotPresentException.class);
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);
  }

  private TreeMap<Key,Value> readAll(SortedKeyValueIterator<Key,Value> iter) throws IOException {
    iter.seek(new Range(), LocalityGroupUtil.EMPTY_CF_SET, false);

    TreeMap<Key,Value> actual = new TreeMap<>();
    while (iter.hasTop()) {
      actual.put(iter.getTopKey(), iter.getTopValue());
      iter.next();
    }
    return actual;
  }

   private void seekLocalityGroups(SortedKeyValueIterator<Key,Value> iter1) throws IOException {
     iter1.seek(new Range(), newCFSet("cf1"), true);
     ae(iter1, "r1", "cf1:x", 2, "1");
diff --git a/server/tserver/src/test/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategyTest.java b/server/tserver/src/test/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategyTest.java
index 55226fbca..0388c1f30 100644
-- a/server/tserver/src/test/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategyTest.java
++ b/server/tserver/src/test/java/org/apache/accumulo/tserver/compaction/DefaultCompactionStrategyTest.java
@@ -41,6 +41,7 @@ import org.apache.accumulo.core.file.NoSuchMetaStoreException;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.server.fs.FileRef;
 import org.apache.hadoop.io.Text;
@@ -133,6 +134,11 @@ public class DefaultCompactionStrategyTest {
     @Override
     public void close() throws IOException {}
 
    @Override
    public FileSKVIterator getSample(SamplerConfigurationImpl sampleConfig) {
      return null;
    }

   }
 
   static final DefaultConfiguration dfault = AccumuloConfiguration.getDefaultConfiguration();
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
index f183b2501..c8b0e11d8 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/CompactCommand.java
@@ -38,7 +38,7 @@ public class CompactCommand extends TableOperation {
 
   // file selection and file output options
   private Option enameOption, epathOption, sizeLtOption, sizeGtOption, minFilesOption, outBlockSizeOpt, outHdfsBlockSizeOpt, outIndexBlockSizeOpt,
      outCompressionOpt, outReplication;
      outCompressionOpt, outReplication, enoSampleOption;
 
   private CompactionConfig compactionConfig = null;
 
@@ -89,6 +89,7 @@ public class CompactCommand extends TableOperation {
   private Map<String,String> getConfigurableCompactionStrategyOpts(CommandLine cl) {
     Map<String,String> opts = new HashMap<>();
 
    put(cl, opts, enoSampleOption, CompactionSettings.SF_NO_SAMPLE);
     put(cl, opts, enameOption, CompactionSettings.SF_NAME_RE_OPT);
     put(cl, opts, epathOption, CompactionSettings.SF_PATH_RE_OPT);
     put(cl, opts, sizeLtOption, CompactionSettings.SF_LT_ESIZE_OPT);
@@ -190,6 +191,9 @@ public class CompactCommand extends TableOperation {
     cancelOpt = new Option(null, "cancel", false, "cancel user initiated compactions");
     opts.addOption(cancelOpt);
 
    enoSampleOption = new Option(null, "sf-no-sample", false,
        "Select files that have no sample data or sample data that differes from the table configuration.");
    opts.addOption(enoSampleOption);
     enameOption = newLAO("sf-ename", "Select files using regular expression to match file names. Only matches against last part of path.");
     opts.addOption(enameOption);
     epathOption = newLAO("sf-epath", "Select files using regular expression to match file paths to compact. Matches against full path.");
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/GrepCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/GrepCommand.java
index 97bddc908..44ee93c99 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/GrepCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/GrepCommand.java
@@ -61,6 +61,8 @@ public class GrepCommand extends ScanCommand {
 
     scanner.setTimeout(getTimeout(cl), TimeUnit.MILLISECONDS);
 
    setupSampling(tableName, cl, shellState, scanner);

     for (int i = 0; i < cl.getArgs().length; i++) {
       setUpIterator(Integer.MAX_VALUE - cl.getArgs().length + i, "grep" + i, cl.getArgs()[i], scanner, cl);
     }
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
index 3531fe906..595829bd4 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
@@ -26,9 +26,11 @@ import java.util.concurrent.TimeUnit;
 import org.apache.accumulo.core.client.AccumuloException;
 import org.apache.accumulo.core.client.AccumuloSecurityException;
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
@@ -60,6 +62,19 @@ public class ScanCommand extends Command {
   private Option optEndRowExclusive;
   private Option timeoutOption;
   private Option profileOpt;
  private Option sampleOpt;

  protected void setupSampling(final String tableName, final CommandLine cl, final Shell shellState, ScannerBase scanner) throws TableNotFoundException,
      AccumuloException, AccumuloSecurityException {
    if (getUseSample(cl)) {
      SamplerConfiguration samplerConfig = shellState.getConnector().tableOperations().getSamplerConfiguration(tableName);
      if (samplerConfig == null) {
        throw new SampleNotPresentException("Table " + tableName + " does not have sampling configured");
      }
      Shell.log.debug("Using sampling configuration : " + samplerConfig);
      scanner.setSamplerConfiguration(samplerConfig);
    }
  }
 
   @Override
   public int execute(final String fullCommand, final CommandLine cl, final Shell shellState) throws Exception {
@@ -86,6 +101,8 @@ public class ScanCommand extends Command {
     // set timeout
     scanner.setTimeout(getTimeout(cl), TimeUnit.MILLISECONDS);
 
    setupSampling(tableName, cl, shellState, scanner);

     // output the records
     if (cl.hasOption(showFewOpt.getOpt())) {
       final String showLength = cl.getOptionValue(showFewOpt.getOpt());
@@ -112,6 +129,10 @@ public class ScanCommand extends Command {
     return 0;
   }
 
  protected boolean getUseSample(CommandLine cl) {
    return cl.hasOption(sampleOpt.getLongOpt());
  }

   protected long getTimeout(final CommandLine cl) {
     if (cl.hasOption(timeoutOption.getLongOpt())) {
       return AccumuloConfiguration.getTimeInMillis(cl.getOptionValue(timeoutOption.getLongOpt()));
@@ -294,6 +315,7 @@ public class ScanCommand extends Command {
     timeoutOption = new Option(null, "timeout", true,
         "time before scan should fail if no data is returned. If no unit is given assumes seconds.  Units d,h,m,s,and ms are supported.  e.g. 30s or 100ms");
     outputFileOpt = new Option("o", "output", true, "local file to write the scan output to");
    sampleOpt = new Option(null, "sample", false, "Show sample");
 
     scanOptAuths.setArgName("comma-separated-authorizations");
     scanOptRow.setArgName("row");
@@ -324,6 +346,7 @@ public class ScanCommand extends Command {
     o.addOption(timeoutOption);
     o.addOption(outputFileOpt);
     o.addOption(profileOpt);
    o.addOption(sampleOpt);
 
     return o;
   }
diff --git a/start/.gitignore b/start/.gitignore
index 56204d21a..e7d7fb175 100644
-- a/start/.gitignore
++ b/start/.gitignore
@@ -23,3 +23,4 @@
 /.pydevproject
 /.idea
 /*.iml
/target/
diff --git a/test/src/main/java/org/apache/accumulo/test/InMemoryMapMemoryUsageTest.java b/test/src/main/java/org/apache/accumulo/test/InMemoryMapMemoryUsageTest.java
index fb0050ff0..05b405e60 100644
-- a/test/src/main/java/org/apache/accumulo/test/InMemoryMapMemoryUsageTest.java
++ b/test/src/main/java/org/apache/accumulo/test/InMemoryMapMemoryUsageTest.java
@@ -18,9 +18,11 @@ package org.apache.accumulo.test;
 
 import java.util.Collections;
 
import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.util.LocalityGroupUtil.LocalityGroupConfigurationError;
 import org.apache.accumulo.tserver.InMemoryMap;
 import org.apache.hadoop.io.Text;
 
@@ -51,7 +53,11 @@ class InMemoryMapMemoryUsageTest extends MemoryUsageTest {
 
   @Override
   void init() {
    imm = new InMemoryMap(false, "/tmp");
    try {
      imm = new InMemoryMap(DefaultConfiguration.getInstance());
    } catch (LocalityGroupConfigurationError e) {
      throw new RuntimeException(e);
    }
     key = new Text();
 
     colf = new Text(String.format("%0" + colFamLen + "d", 0));
diff --git a/test/src/main/java/org/apache/accumulo/test/SampleIT.java b/test/src/main/java/org/apache/accumulo/test/SampleIT.java
new file mode 100644
index 000000000..423b955ba
-- /dev/null
++ b/test/src/main/java/org/apache/accumulo/test/SampleIT.java
@@ -0,0 +1,497 @@
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ClientSideIteratorScanner;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.OfflineScanner;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.harness.AccumuloClusterHarness;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

public class SampleIT extends AccumuloClusterHarness {

  private static final Map<String,String> OPTIONS_1 = ImmutableMap.of("hasher", "murmur3_32", "modulus", "1009");
  private static final Map<String,String> OPTIONS_2 = ImmutableMap.of("hasher", "murmur3_32", "modulus", "997");

  private static final SamplerConfiguration SC1 = new SamplerConfiguration(RowSampler.class.getName()).setOptions(OPTIONS_1);
  private static final SamplerConfiguration SC2 = new SamplerConfiguration(RowSampler.class.getName()).setOptions(OPTIONS_2);

  public static class IteratorThatUsesSample extends WrappingIterator {
    private SortedKeyValueIterator<Key,Value> sampleDC;
    private boolean hasTop;

    @Override
    public boolean hasTop() {
      return hasTop && super.hasTop();
    }

    @Override
    public void seek(Range range, Collection<ByteSequence> columnFamilies, boolean inclusive) throws IOException {

      int sampleCount = 0;
      sampleDC.seek(range, columnFamilies, inclusive);

      while (sampleDC.hasTop()) {
        sampleCount++;
        sampleDC.next();
      }

      if (sampleCount < 10) {
        hasTop = true;
        super.seek(range, columnFamilies, inclusive);
      } else {
        // its too much data
        hasTop = false;
      }
    }

    @Override
    public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
      super.init(source, options, env);

      IteratorEnvironment sampleEnv = env.cloneWithSamplingEnabled();

      sampleDC = source.deepCopy(sampleEnv);
    }
  }

  @Test
  public void testBasic() throws Exception {

    Connector conn = getConnector();
    String tableName = getUniqueNames(1)[0];
    String clone = tableName + "_clone";

    conn.tableOperations().create(tableName, new NewTableConfiguration().enableSampling(SC1));

    BatchWriter bw = conn.createBatchWriter(tableName, new BatchWriterConfig());

    TreeMap<Key,Value> expected = new TreeMap<Key,Value>();
    String someRow = writeData(bw, SC1, expected);

    Scanner scanner = conn.createScanner(tableName, Authorizations.EMPTY);
    Scanner isoScanner = new IsolatedScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    Scanner csiScanner = new ClientSideIteratorScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    scanner.setSamplerConfiguration(SC1);
    csiScanner.setSamplerConfiguration(SC1);
    isoScanner.setSamplerConfiguration(SC1);
    isoScanner.setBatchSize(10);

    BatchScanner bScanner = conn.createBatchScanner(tableName, Authorizations.EMPTY, 2);
    bScanner.setSamplerConfiguration(SC1);
    bScanner.setRanges(Arrays.asList(new Range()));

    check(expected, scanner, bScanner, isoScanner, csiScanner);

    conn.tableOperations().flush(tableName, null, null, true);

    Scanner oScanner = newOfflineScanner(conn, tableName, clone, SC1);
    check(expected, scanner, bScanner, isoScanner, csiScanner, oScanner);

    // ensure non sample data can be scanned after scanning sample data
    for (ScannerBase sb : Arrays.asList(scanner, bScanner, isoScanner, csiScanner, oScanner)) {
      sb.clearSamplerConfiguration();
      Assert.assertEquals(20000, Iterables.size(sb));
      sb.setSamplerConfiguration(SC1);
    }

    Iterator<Key> it = expected.keySet().iterator();
    while (it.hasNext()) {
      Key k = it.next();
      if (k.getRow().toString().equals(someRow)) {
        it.remove();
      }
    }

    expected.put(new Key(someRow, "cf1", "cq1", 8), new Value("42".getBytes()));
    expected.put(new Key(someRow, "cf1", "cq3", 8), new Value("suprise".getBytes()));

    Mutation m = new Mutation(someRow);

    m.put("cf1", "cq1", 8, "42");
    m.putDelete("cf1", "cq2", 8);
    m.put("cf1", "cq3", 8, "suprise");

    bw.addMutation(m);
    bw.close();

    check(expected, scanner, bScanner, isoScanner, csiScanner);

    conn.tableOperations().flush(tableName, null, null, true);

    oScanner = newOfflineScanner(conn, tableName, clone, SC1);
    check(expected, scanner, bScanner, isoScanner, csiScanner, oScanner);

    scanner.setRange(new Range(someRow));
    isoScanner.setRange(new Range(someRow));
    csiScanner.setRange(new Range(someRow));
    oScanner.setRange(new Range(someRow));
    bScanner.setRanges(Arrays.asList(new Range(someRow)));

    expected.clear();

    expected.put(new Key(someRow, "cf1", "cq1", 8), new Value("42".getBytes()));
    expected.put(new Key(someRow, "cf1", "cq3", 8), new Value("suprise".getBytes()));

    check(expected, scanner, bScanner, isoScanner, csiScanner, oScanner);

    bScanner.close();
  }

  private Scanner newOfflineScanner(Connector conn, String tableName, String clone, SamplerConfiguration sc) throws Exception {
    if (conn.tableOperations().exists(clone)) {
      conn.tableOperations().delete(clone);
    }
    Map<String,String> em = Collections.emptyMap();
    Set<String> es = Collections.emptySet();
    conn.tableOperations().clone(tableName, clone, false, em, es);
    conn.tableOperations().offline(clone, true);
    String cloneID = conn.tableOperations().tableIdMap().get(clone);
    OfflineScanner oScanner = new OfflineScanner(conn.getInstance(), new Credentials(getAdminPrincipal(), getAdminToken()), cloneID, Authorizations.EMPTY);
    if (sc != null) {
      oScanner.setSamplerConfiguration(sc);
    }
    return oScanner;
  }

  private void updateExpected(SamplerConfiguration sc, TreeMap<Key,Value> expected) {
    expected.clear();

    RowSampler sampler = new RowSampler();
    sampler.init(sc);

    for (int i = 0; i < 10000; i++) {
      String row = String.format("r_%06d", i);

      Key k1 = new Key(row, "cf1", "cq1", 7);
      if (sampler.accept(k1)) {
        expected.put(k1, new Value(("" + i).getBytes()));
      }

      Key k2 = new Key(row, "cf1", "cq2", 7);
      if (sampler.accept(k2)) {
        expected.put(k2, new Value(("" + (100000000 - i)).getBytes()));
      }
    }
  }

  private String writeData(BatchWriter bw, SamplerConfiguration sc, TreeMap<Key,Value> expected) throws MutationsRejectedException {
    int count = 0;
    String someRow = null;

    RowSampler sampler = new RowSampler();
    sampler.init(sc);

    for (int i = 0; i < 10000; i++) {
      String row = String.format("r_%06d", i);
      Mutation m = new Mutation(row);

      m.put("cf1", "cq1", 7, "" + i);
      m.put("cf1", "cq2", 7, "" + (100000000 - i));

      bw.addMutation(m);

      Key k1 = new Key(row, "cf1", "cq1", 7);
      if (sampler.accept(k1)) {
        expected.put(k1, new Value(("" + i).getBytes()));
        count++;
        if (count == 5) {
          someRow = row;
        }
      }

      Key k2 = new Key(row, "cf1", "cq2", 7);
      if (sampler.accept(k2)) {
        expected.put(k2, new Value(("" + (100000000 - i)).getBytes()));
      }
    }

    bw.flush();

    return someRow;
  }

  private int countEntries(Iterable<Entry<Key,Value>> scanner) {

    int count = 0;
    Iterator<Entry<Key,Value>> iter = scanner.iterator();

    while (iter.hasNext()) {
      iter.next();
      count++;
    }

    return count;
  }

  private void setRange(Range range, List<? extends ScannerBase> scanners) {
    for (ScannerBase s : scanners) {
      if (s instanceof Scanner) {
        ((Scanner) s).setRange(range);
      } else {
        ((BatchScanner) s).setRanges(Collections.singleton(range));
      }

    }
  }

  @Test
  public void testIterator() throws Exception {
    Connector conn = getConnector();
    String tableName = getUniqueNames(1)[0];
    String clone = tableName + "_clone";

    conn.tableOperations().create(tableName, new NewTableConfiguration().enableSampling(SC1));

    BatchWriter bw = conn.createBatchWriter(tableName, new BatchWriterConfig());

    TreeMap<Key,Value> expected = new TreeMap<Key,Value>();
    writeData(bw, SC1, expected);

    ArrayList<Key> keys = new ArrayList<>(expected.keySet());

    Range range1 = new Range(keys.get(6), true, keys.get(11), true);

    Scanner scanner = conn.createScanner(tableName, Authorizations.EMPTY);
    Scanner isoScanner = new IsolatedScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    ClientSideIteratorScanner csiScanner = new ClientSideIteratorScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    BatchScanner bScanner = conn.createBatchScanner(tableName, Authorizations.EMPTY, 2);

    csiScanner.setIteratorSamplerConfiguration(SC1);

    List<? extends ScannerBase> scanners = Arrays.asList(scanner, isoScanner, bScanner, csiScanner);

    for (ScannerBase s : scanners) {
      s.addScanIterator(new IteratorSetting(100, IteratorThatUsesSample.class));
    }

    // the iterator should see less than 10 entries in sample data, and return data
    setRange(range1, scanners);
    for (ScannerBase s : scanners) {
      Assert.assertEquals(2954, countEntries(s));
    }

    Range range2 = new Range(keys.get(5), true, keys.get(18), true);
    setRange(range2, scanners);

    // the iterator should see more than 10 entries in sample data, and return no data
    for (ScannerBase s : scanners) {
      Assert.assertEquals(0, countEntries(s));
    }

    // flush an rerun same test against files
    conn.tableOperations().flush(tableName, null, null, true);

    Scanner oScanner = newOfflineScanner(conn, tableName, clone, null);
    oScanner.addScanIterator(new IteratorSetting(100, IteratorThatUsesSample.class));
    scanners = Arrays.asList(scanner, isoScanner, bScanner, csiScanner, oScanner);

    setRange(range1, scanners);
    for (ScannerBase s : scanners) {
      Assert.assertEquals(2954, countEntries(s));
    }

    setRange(range2, scanners);
    for (ScannerBase s : scanners) {
      Assert.assertEquals(0, countEntries(s));
    }

    updateSamplingConfig(conn, tableName, SC2);

    csiScanner.setIteratorSamplerConfiguration(SC2);

    oScanner = newOfflineScanner(conn, tableName, clone, null);
    oScanner.addScanIterator(new IteratorSetting(100, IteratorThatUsesSample.class));
    scanners = Arrays.asList(scanner, isoScanner, bScanner, csiScanner, oScanner);

    for (ScannerBase s : scanners) {
      try {
        countEntries(s);
        Assert.fail("Expected SampleNotPresentException, but it did not happen : " + s.getClass().getSimpleName());
      } catch (SampleNotPresentException e) {

      }
    }
  }

  private void setSamplerConfig(SamplerConfiguration sc, ScannerBase... scanners) {
    for (ScannerBase s : scanners) {
      s.setSamplerConfiguration(sc);
    }
  }

  @Test
  public void testSampleNotPresent() throws Exception {

    Connector conn = getConnector();
    String tableName = getUniqueNames(1)[0];
    String clone = tableName + "_clone";

    conn.tableOperations().create(tableName);

    BatchWriter bw = conn.createBatchWriter(tableName, new BatchWriterConfig());

    TreeMap<Key,Value> expected = new TreeMap<Key,Value>();
    writeData(bw, SC1, expected);

    Scanner scanner = conn.createScanner(tableName, Authorizations.EMPTY);
    Scanner isoScanner = new IsolatedScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    isoScanner.setBatchSize(10);
    Scanner csiScanner = new ClientSideIteratorScanner(conn.createScanner(tableName, Authorizations.EMPTY));
    BatchScanner bScanner = conn.createBatchScanner(tableName, Authorizations.EMPTY, 2);
    bScanner.setRanges(Arrays.asList(new Range()));

    // ensure sample not present exception occurs when sampling is not configured
    assertSampleNotPresent(SC1, scanner, isoScanner, bScanner, csiScanner);

    conn.tableOperations().flush(tableName, null, null, true);

    Scanner oScanner = newOfflineScanner(conn, tableName, clone, SC1);
    assertSampleNotPresent(SC1, scanner, isoScanner, bScanner, csiScanner, oScanner);

    // configure sampling, however there exist an rfile w/o sample data... so should still see sample not present exception

    updateSamplingConfig(conn, tableName, SC1);

    // create clone with new config
    oScanner = newOfflineScanner(conn, tableName, clone, SC1);

    assertSampleNotPresent(SC1, scanner, isoScanner, bScanner, csiScanner, oScanner);

    // create rfile with sample data present
    conn.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    // should be able to scan sample now
    oScanner = newOfflineScanner(conn, tableName, clone, SC1);
    setSamplerConfig(SC1, scanner, csiScanner, isoScanner, bScanner, oScanner);
    check(expected, scanner, isoScanner, bScanner, csiScanner, oScanner);

    // change sampling config
    updateSamplingConfig(conn, tableName, SC2);

    // create clone with new config
    oScanner = newOfflineScanner(conn, tableName, clone, SC2);

    // rfile should have different sample config than table, and scan should not work
    assertSampleNotPresent(SC2, scanner, isoScanner, bScanner, csiScanner, oScanner);

    // create rfile that has same sample data as table config
    conn.tableOperations().compact(tableName, new CompactionConfig().setWait(true));

    // should be able to scan sample now
    updateExpected(SC2, expected);
    oScanner = newOfflineScanner(conn, tableName, clone, SC2);
    setSamplerConfig(SC2, scanner, csiScanner, isoScanner, bScanner, oScanner);
    check(expected, scanner, isoScanner, bScanner, csiScanner, oScanner);

    bScanner.close();
  }

  private void updateSamplingConfig(Connector conn, String tableName, SamplerConfiguration sc) throws TableNotFoundException, AccumuloException,
      AccumuloSecurityException {
    conn.tableOperations().setSamplerConfiguration(tableName, sc);
    // wait for for config change
    conn.tableOperations().offline(tableName, true);
    conn.tableOperations().online(tableName, true);
  }

  private void assertSampleNotPresent(SamplerConfiguration sc, ScannerBase... scanners) {

    for (ScannerBase scanner : scanners) {
      SamplerConfiguration csc = scanner.getSamplerConfiguration();

      scanner.setSamplerConfiguration(sc);

      try {
        for (Iterator<Entry<Key,Value>> i = scanner.iterator(); i.hasNext();) {
          Entry<Key,Value> entry = i.next();
          entry.getKey();
        }
        Assert.fail("Expected SampleNotPresentException, but it did not happen : " + scanner.getClass().getSimpleName());
      } catch (SampleNotPresentException e) {

      }

      scanner.clearSamplerConfiguration();
      for (Iterator<Entry<Key,Value>> i = scanner.iterator(); i.hasNext();) {
        Entry<Key,Value> entry = i.next();
        entry.getKey();
      }

      if (csc == null) {
        scanner.clearSamplerConfiguration();
      } else {
        scanner.setSamplerConfiguration(csc);
      }
    }
  }

  private void check(TreeMap<Key,Value> expected, ScannerBase... scanners) {
    TreeMap<Key,Value> actual = new TreeMap<>();
    for (ScannerBase s : scanners) {
      actual.clear();
      for (Entry<Key,Value> entry : s) {
        actual.put(entry.getKey(), entry.getValue());
      }
      Assert.assertEquals(String.format("Saw %d instead of %d entries using %s", actual.size(), expected.size(), s.getClass().getSimpleName()), expected,
          actual);
    }
  }
}
diff --git a/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java b/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
index e7b579935..ae38fb8a6 100644
-- a/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
++ b/test/src/main/java/org/apache/accumulo/test/ShellServerIT.java
@@ -41,8 +41,6 @@ import java.util.Map.Entry;
 import java.util.Random;
 import java.util.concurrent.TimeUnit;
 
import jline.console.ConsoleReader;

 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
@@ -91,6 +89,8 @@ import org.slf4j.LoggerFactory;
 import com.google.common.collect.Iterables;
 import com.google.common.collect.Iterators;
 
import jline.console.ConsoleReader;

 public class ShellServerIT extends SharedMiniClusterBase {
   public static class TestOutputStream extends OutputStream {
     StringBuilder sb = new StringBuilder();
@@ -975,6 +975,26 @@ public class ShellServerIT extends SharedMiniClusterBase {
     ts.exec("compact -t " + clone + " -w --sf-ename F.* --sf-lt-esize 1K");
 
     assertEquals(3, countFiles(cloneId));

    String clone2 = table + "_clone_2";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=7,table.sampler=org.apache.accumulo.core.sample.RowSampler " + clone
        + " " + clone2);
    String clone2Id = getTableId(clone2);

    assertEquals(3, countFiles(clone2Id));

    ts.exec("table " + clone2);
    ts.exec("insert v n l o");
    ts.exec("flush -w");

    ts.exec("insert x n l o");
    ts.exec("flush -w");

    assertEquals(5, countFiles(clone2Id));

    ts.exec("compact -t " + clone2 + " -w --sf-no-sample");

    assertEquals(3, countFiles(clone2Id));
   }
 
   @Test
@@ -988,6 +1008,54 @@ public class ShellServerIT extends SharedMiniClusterBase {
     ts.exec("compact -t " + table + " -w --sf-ename F.* -s " + TestCompactionStrategy.class.getName() + " -sc inputPrefix=F,dropPrefix=A", false);
   }
 
  @Test
  public void testScanScample() throws Exception {
    final String table = name.getMethodName();

    // compact
    ts.exec("createtable " + table);

    ts.exec("insert 9255 doc content 'abcde'");
    ts.exec("insert 9255 doc url file://foo.txt");
    ts.exec("insert 8934 doc content 'accumulo scales'");
    ts.exec("insert 8934 doc url file://accumulo_notes.txt");
    ts.exec("insert 2317 doc content 'milk, eggs, bread, parmigiano-reggiano'");
    ts.exec("insert 2317 doc url file://groceries/9.txt");
    ts.exec("insert 3900 doc content 'EC2 ate my homework'");
    ts.exec("insert 3900 doc uril file://final_project.txt");

    String clone1 = table + "_clone_1";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=3,table.sampler=org.apache.accumulo.core.sample.RowSampler " + table
        + " " + clone1);

    ts.exec("compact -t " + clone1 + " -w --sf-no-sample");

    ts.exec("table " + clone1);
    ts.exec("scan --sample", true, "parmigiano-reggiano", true);
    ts.exec("grep --sample reg", true, "parmigiano-reggiano", true);
    ts.exec("scan --sample", true, "accumulo", false);
    ts.exec("grep --sample acc", true, "accumulo", false);

    // create table where table sample config differs from whats in file
    String clone2 = table + "_clone_2";
    ts.exec("clonetable -s table.sampler.opt.hasher=murmur3_32,table.sampler.opt.modulus=2,table.sampler=org.apache.accumulo.core.sample.RowSampler " + clone1
        + " " + clone2);

    ts.exec("table " + clone2);
    ts.exec("scan --sample", false, "SampleNotPresentException", true);
    ts.exec("grep --sample reg", false, "SampleNotPresentException", true);

    ts.exec("compact -t " + clone2 + " -w --sf-no-sample");

    for (String expected : Arrays.asList("2317", "3900", "9255")) {
      ts.exec("scan --sample", true, expected, true);
      ts.exec("grep --sample " + expected.substring(0, 2), true, expected, true);
    }

    ts.exec("scan --sample", true, "8934", false);
    ts.exec("grep --sample 89", true, "8934", false);
  }

   @Test
   public void constraint() throws Exception {
     final String table = name.getMethodName();
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/ExamplesIT.java b/test/src/main/java/org/apache/accumulo/test/functional/ExamplesIT.java
index 71ddbcd98..826907c2e 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/ExamplesIT.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/ExamplesIT.java
@@ -17,6 +17,7 @@
 package org.apache.accumulo.test.functional;
 
 import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 import static org.junit.Assert.assertEquals;
 import static org.junit.Assert.assertFalse;
 import static org.junit.Assert.assertTrue;
@@ -102,7 +103,6 @@ import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.collect.Iterators;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 
 public class ExamplesIT extends AccumuloClusterHarness {
   private static final Logger log = LoggerFactory.getLogger(ExamplesIT.class);
@@ -390,7 +390,7 @@ public class ExamplesIT extends AccumuloClusterHarness {
     Index.index(30, src, "\\W+", bw);
     bw.close();
     BatchScanner bs = c.createBatchScanner(shard, Authorizations.EMPTY, 4);
    List<String> found = Query.query(bs, Arrays.asList("foo", "bar"));
    List<String> found = Query.query(bs, Arrays.asList("foo", "bar"), null);
     bs.close();
     // should find ourselves
     boolean thisFile = false;
diff --git a/test/src/main/java/org/apache/accumulo/test/functional/ReadWriteIT.java b/test/src/main/java/org/apache/accumulo/test/functional/ReadWriteIT.java
index 485d6d29a..30982517f 100644
-- a/test/src/main/java/org/apache/accumulo/test/functional/ReadWriteIT.java
++ b/test/src/main/java/org/apache/accumulo/test/functional/ReadWriteIT.java
@@ -430,8 +430,8 @@ public class ReadWriteIT extends AccumuloClusterHarness {
         PrintInfo.main(args.toArray(new String[args.size()]));
         newOut.flush();
         String stdout = baos.toString();
        assertTrue(stdout.contains("Locality group         : g1"));
        assertTrue(stdout.contains("families      : [colf]"));
        assertTrue(stdout.contains("Locality group           : g1"));
        assertTrue(stdout.contains("families        : [colf]"));
       } finally {
         newOut.close();
         System.setOut(oldOut);
diff --git a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
index 7a4223d73..dd085ccf5 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
@@ -30,15 +30,23 @@ import java.io.IOException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapred.AccumuloFileOutputFormat;
 import org.apache.accumulo.core.client.mapred.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.rfile.RFileOperations;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.mapred.JobClient;
 import org.apache.hadoop.mapred.JobConf;
@@ -65,6 +73,9 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
   private static AssertionError e1 = null;
   private static AssertionError e2 = null;
 
  private static final SamplerConfiguration SAMPLER_CONFIG = new SamplerConfiguration(RowSampler.class.getName()).addOption("hasher", "murmur3_32").addOption(
      "modulus", "3");

   @Rule
   public TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("user.dir") + "/target"));
 
@@ -141,6 +152,7 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
       AccumuloInputFormat.setConnectorInfo(job, getAdminPrincipal(), getAdminToken());
       AccumuloInputFormat.setInputTableName(job, table);
       AccumuloFileOutputFormat.setOutputPath(job, new Path(args[1]));
      AccumuloFileOutputFormat.setSampler(job, SAMPLER_CONFIG);
 
       job.setMapperClass(BAD_TABLE.equals(table) ? BadKeyMapper.class : IdentityMapper.class);
       job.setMapOutputKeyClass(Key.class);
@@ -177,6 +189,12 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
     if (content) {
       assertEquals(1, files.length);
       assertTrue(files[0].exists());

      Configuration conf = CachedConfiguration.getInstance();
      DefaultConfiguration acuconf = DefaultConfiguration.getInstance();
      FileSKVIterator sample = RFileOperations.getInstance().openReader(files[0].toString(), false, FileSystem.get(conf), conf, acuconf)
          .getSample(new SamplerConfigurationImpl(SAMPLER_CONFIG));
      assertNotNull(sample);
     } else {
       assertEquals(0, files.length);
     }
diff --git a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
index 2cef38223..cd80139a8 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
@@ -27,11 +27,14 @@ import java.util.Collections;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapred.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapred.RangeInputSplit;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
@@ -60,7 +63,9 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
   }
 
   private static AssertionError e1 = null;
  private static int e1Count = 0;
   private static AssertionError e2 = null;
  private static int e2Count = 0;
 
   private static class MRTester extends Configured implements Tool {
     private static class TestMapper implements Mapper<Key,Value,Key,Value> {
@@ -76,6 +81,7 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
           assertEquals(new String(v.get()), String.format("%09x", count));
         } catch (AssertionError e) {
           e1 = e;
          e1Count++;
         }
         key = new Key(k);
         count++;
@@ -90,6 +96,7 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
           assertEquals(100, count);
         } catch (AssertionError e) {
           e2 = e;
          e2Count++;
         }
       }
 
@@ -98,11 +105,17 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
     @Override
     public int run(String[] args) throws Exception {
 
      if (args.length != 1) {
        throw new IllegalArgumentException("Usage : " + MRTester.class.getName() + " <table>");
      if (args.length != 1 && args.length != 3) {
        throw new IllegalArgumentException("Usage : " + MRTester.class.getName() + " <table> [<batchScan> <scan sample>]");
       }
 
       String table = args[0];
      Boolean batchScan = false;
      boolean sample = false;
      if (args.length == 3) {
        batchScan = Boolean.parseBoolean(args[1]);
        sample = Boolean.parseBoolean(args[2]);
      }
 
       JobConf job = new JobConf(getConf());
       job.setJarByClass(this.getClass());
@@ -112,6 +125,10 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
       AccumuloInputFormat.setConnectorInfo(job, getAdminPrincipal(), getAdminToken());
       AccumuloInputFormat.setInputTableName(job, table);
       AccumuloInputFormat.setZooKeeperInstance(job, getCluster().getClientConfig());
      AccumuloInputFormat.setBatchScan(job, batchScan);
      if (sample) {
        AccumuloInputFormat.setSamplerConfiguration(job, SAMPLER_CONFIG);
      }
 
       job.setMapperClass(TestMapper.class);
       job.setMapOutputKeyClass(Key.class);
@@ -143,11 +160,47 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
     }
     bw.close();
 
    e1 = null;
    e2 = null;

     MRTester.main(table);
     assertNull(e1);
     assertNull(e2);
   }
 
  private static final SamplerConfiguration SAMPLER_CONFIG = new SamplerConfiguration(RowSampler.class.getName()).addOption("hasher", "murmur3_32").addOption(
      "modulus", "3");

  @Test
  public void testSample() throws Exception {
    final String TEST_TABLE_3 = getUniqueNames(1)[0];

    Connector c = getConnector();
    c.tableOperations().create(TEST_TABLE_3, new NewTableConfiguration().enableSampling(SAMPLER_CONFIG));
    BatchWriter bw = c.createBatchWriter(TEST_TABLE_3, new BatchWriterConfig());
    for (int i = 0; i < 100; i++) {
      Mutation m = new Mutation(new Text(String.format("%09x", i + 1)));
      m.put(new Text(), new Text(), new Value(String.format("%09x", i).getBytes()));
      bw.addMutation(m);
    }
    bw.close();

    MRTester.main(TEST_TABLE_3, "False", "True");
    Assert.assertEquals(38, e1Count);
    Assert.assertEquals(1, e2Count);

    e2Count = e1Count = 0;
    MRTester.main(TEST_TABLE_3, "False", "False");
    Assert.assertEquals(0, e1Count);
    Assert.assertEquals(0, e2Count);

    e2Count = e1Count = 0;
    MRTester.main(TEST_TABLE_3, "True", "True");
    Assert.assertEquals(38, e1Count);
    Assert.assertEquals(1, e2Count);

  }

   @Test
   public void testCorrectRangeInputSplits() throws Exception {
     JobConf job = new JobConf();
diff --git a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
index 8f5337862..d00a9b3bc 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
@@ -27,14 +27,22 @@ import java.io.IOException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloFileOutputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.FileSKVIterator;
import org.apache.accumulo.core.file.rfile.RFileOperations;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.util.CachedConfiguration;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.Mapper;
@@ -55,6 +63,9 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
   private String TEST_TABLE;
   private String EMPTY_TABLE;
 
  private static final SamplerConfiguration SAMPLER_CONFIG = new SamplerConfiguration(RowSampler.class.getName()).addOption("hasher", "murmur3_32").addOption(
      "modulus", "3");

   @Override
   protected int defaultTimeoutSeconds() {
     return 4 * 60;
@@ -152,6 +163,7 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
       AccumuloInputFormat.setInputTableName(job, table);
       AccumuloInputFormat.setZooKeeperInstance(job, getCluster().getClientConfig());
       AccumuloFileOutputFormat.setOutputPath(job, new Path(args[1]));
      AccumuloFileOutputFormat.setSampler(job, SAMPLER_CONFIG);
 
       job.setMapperClass(table.endsWith("_mapreduce_bad_table") ? BadKeyMapper.class : Mapper.class);
       job.setMapOutputKeyClass(Key.class);
@@ -189,6 +201,12 @@ public class AccumuloFileOutputFormatIT extends AccumuloClusterHarness {
     if (content) {
       assertEquals(1, files.length);
       assertTrue(files[0].exists());

      Configuration conf = CachedConfiguration.getInstance();
      DefaultConfiguration acuconf = DefaultConfiguration.getInstance();
      FileSKVIterator sample = RFileOperations.getInstance().openReader(files[0].toString(), false, FileSystem.get(conf), conf, acuconf)
          .getSample(new SamplerConfigurationImpl(SAMPLER_CONFIG));
      assertNotNull(sample);
     } else {
       assertEquals(0, files.length);
     }
diff --git a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
index 1ca4f9232..0a5bd68e4 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
@@ -39,6 +39,8 @@ import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.RangeInputSplit;
 import org.apache.accumulo.core.client.mapreduce.impl.BatchInputSplit;
@@ -51,6 +53,7 @@ import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
@@ -270,15 +273,18 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
     @Override
     public int run(String[] args) throws Exception {
 
      if (args.length != 2 && args.length != 3) {
        throw new IllegalArgumentException("Usage : " + MRTester.class.getName() + " <table> <inputFormatClass> [<batchScan>]");
      if (args.length != 2 && args.length != 4) {
        throw new IllegalArgumentException("Usage : " + MRTester.class.getName() + " <table> <inputFormatClass> [<batchScan> <scan sample>]");
       }
 
       String table = args[0];
       String inputFormatClassName = args[1];
       Boolean batchScan = false;
      if (args.length == 3)
      boolean sample = false;
      if (args.length == 4) {
         batchScan = Boolean.parseBoolean(args[2]);
        sample = Boolean.parseBoolean(args[3]);
      }
 
       assertionErrors.put(table + "_map", new AssertionError("Dummy_map"));
       assertionErrors.put(table + "_cleanup", new AssertionError("Dummy_cleanup"));
@@ -296,6 +302,9 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
       AccumuloInputFormat.setConnectorInfo(job, getAdminPrincipal(), getAdminToken());
       AccumuloInputFormat.setInputTableName(job, table);
       AccumuloInputFormat.setBatchScan(job, batchScan);
      if (sample) {
        AccumuloInputFormat.setSamplerConfiguration(job, SAMPLER_CONFIG);
      }
 
       job.setMapperClass(TestMapper.class);
       job.setMapOutputKeyClass(Key.class);
@@ -335,6 +344,38 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
     assertEquals(1, assertionErrors.get(TEST_TABLE_1 + "_cleanup").size());
   }
 
  private static final SamplerConfiguration SAMPLER_CONFIG = new SamplerConfiguration(RowSampler.class.getName()).addOption("hasher", "murmur3_32").addOption(
      "modulus", "3");

  @Test
  public void testSample() throws Exception {
    final String TEST_TABLE_3 = getUniqueNames(1)[0];

    Connector c = getConnector();
    c.tableOperations().create(TEST_TABLE_3, new NewTableConfiguration().enableSampling(SAMPLER_CONFIG));
    BatchWriter bw = c.createBatchWriter(TEST_TABLE_3, new BatchWriterConfig());
    for (int i = 0; i < 100; i++) {
      Mutation m = new Mutation(new Text(String.format("%09x", i + 1)));
      m.put(new Text(), new Text(), new Value(String.format("%09x", i).getBytes()));
      bw.addMutation(m);
    }
    bw.close();

    Assert.assertEquals(0, MRTester.main(new String[] {TEST_TABLE_3, AccumuloInputFormat.class.getName(), "False", "True"}));
    assertEquals(39, assertionErrors.get(TEST_TABLE_3 + "_map").size());
    assertEquals(2, assertionErrors.get(TEST_TABLE_3 + "_cleanup").size());

    assertionErrors.clear();
    Assert.assertEquals(0, MRTester.main(new String[] {TEST_TABLE_3, AccumuloInputFormat.class.getName(), "False", "False"}));
    assertEquals(1, assertionErrors.get(TEST_TABLE_3 + "_map").size());
    assertEquals(1, assertionErrors.get(TEST_TABLE_3 + "_cleanup").size());

    assertionErrors.clear();
    Assert.assertEquals(0, MRTester.main(new String[] {TEST_TABLE_3, AccumuloInputFormat.class.getName(), "True", "True"}));
    assertEquals(39, assertionErrors.get(TEST_TABLE_3 + "_map").size());
    assertEquals(2, assertionErrors.get(TEST_TABLE_3 + "_cleanup").size());
  }

   @Test
   public void testMapWithBatchScanner() throws Exception {
     final String TEST_TABLE_2 = getUniqueNames(1)[0];
@@ -349,7 +390,7 @@ public class AccumuloInputFormatIT extends AccumuloClusterHarness {
     }
     bw.close();
 
    Assert.assertEquals(0, MRTester.main(new String[] {TEST_TABLE_2, AccumuloInputFormat.class.getName(), "True"}));
    Assert.assertEquals(0, MRTester.main(new String[] {TEST_TABLE_2, AccumuloInputFormat.class.getName(), "True", "False"}));
     assertEquals(1, assertionErrors.get(TEST_TABLE_2 + "_map").size());
     assertEquals(1, assertionErrors.get(TEST_TABLE_2 + "_cleanup").size());
   }
diff --git a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
index ef05f3711..559703fe6 100644
-- a/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
++ b/test/src/main/java/org/apache/accumulo/test/performance/thrift/NullTserver.java
@@ -55,6 +55,7 @@ import org.apache.accumulo.core.tabletserver.thrift.ActiveCompaction;
 import org.apache.accumulo.core.tabletserver.thrift.ActiveScan;
 import org.apache.accumulo.core.tabletserver.thrift.NoSuchScanIDException;
 import org.apache.accumulo.core.tabletserver.thrift.TDurability;
import org.apache.accumulo.core.tabletserver.thrift.TSamplerConfiguration;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Iface;
 import org.apache.accumulo.core.tabletserver.thrift.TabletClientService.Processor;
@@ -77,6 +78,7 @@ import org.apache.thrift.TException;
 
 import com.beust.jcommander.Parameter;
 import com.google.common.net.HostAndPort;

 import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
 
 /**
@@ -136,14 +138,14 @@ public class NullTserver {
 
     @Override
     public InitialMultiScan startMultiScan(TInfo tinfo, TCredentials credentials, Map<TKeyExtent,List<TRange>> batch, List<TColumn> columns,
        List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, long batchTimeOut) {
        List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, TSamplerConfiguration tsc, long batchTimeOut) {
       return null;
     }
 
     @Override
     public InitialScan startScan(TInfo tinfo, TCredentials credentials, TKeyExtent extent, TRange range, List<TColumn> columns, int batchSize,
         List<IterInfo> ssiList, Map<String,Map<String,String>> ssio, List<ByteBuffer> authorizations, boolean waitForWrites, boolean isolated,
        long readaheadThreshold, long batchTimeOut) {
        long readaheadThreshold, TSamplerConfiguration tsc, long batchTimeOut) {
       return null;
     }
 
- 
2.19.1.windows.1

