From c8bd1ddd096b3b625d39d1ecf341fd8a58e46b17 Mon Sep 17 00:00:00 2001
From: Keith Turner <kturner@apache.org>
Date: Fri, 27 May 2016 14:18:22 -0400
Subject: [PATCH] ACCUMULO-3913 moved sampling code meant for user in API pkg

--
 .../client/ClientSideIteratorScanner.java     |   2 +-
 .../accumulo/core/client/ScannerBase.java     |   2 +-
 .../client/admin/NewTableConfiguration.java   |   1 +
 .../core/client/admin/TableOperations.java    |   1 +
 .../client/impl/BaseIteratorEnvironment.java  |   2 +-
 .../core/client/impl/OfflineIterator.java     |   2 +-
 .../core/client/impl/ScannerOptions.java      |   2 +-
 .../core/client/impl/TableOperationsImpl.java |   2 +-
 .../core/client/impl/ThriftScanner.java       |   2 +-
 .../client/mapred/AbstractInputFormat.java    |   2 +-
 .../mapred/AccumuloFileOutputFormat.java      |   2 +-
 .../core/client/mapred/InputFormatBase.java   |   2 +-
 .../client/mapreduce/AbstractInputFormat.java |   2 +-
 .../mapreduce/AccumuloFileOutputFormat.java   |   2 +-
 .../client/mapreduce/InputFormatBase.java     |   2 +-
 .../client/mapreduce/InputTableConfig.java    |   2 +-
 .../client/mapreduce/RangeInputSplit.java     |   2 +-
 .../lib/impl/FileOutputConfigurator.java      |   2 +-
 .../mapreduce/lib/impl/InputConfigurator.java |   2 +-
 .../core/client/mock/MockScannerBase.java     |   2 +-
 .../core/client/mock/MockTableOperations.java |   2 +-
 .../sample/AbstractHashSampler.java           |  21 +++-
 .../{ => client}/sample/RowColumnSampler.java |  18 +--
 .../core/{ => client}/sample/RowSampler.java  |  12 +-
 .../core/{ => client}/sample/Sampler.java     |   3 +-
 .../SamplerConfiguration.java                 |   6 +-
 .../accumulo/core/file/rfile/RFile.java       |   4 +-
 .../core/file/rfile/RFileOperations.java      |   2 +-
 .../core/iterators/IteratorEnvironment.java   |   2 +-
 .../core/iterators/system/SampleIterator.java |   4 +-
 .../core/sample/impl/DataoutputHasher.java    | 108 ++++++++++++++++++
 .../sample/impl/SamplerConfigurationImpl.java |   2 +-
 .../core/sample/impl/SamplerFactory.java      |   2 +-
 .../impl/TableOperationsHelperTest.java       |   2 +-
 .../mapred/AccumuloFileOutputFormatTest.java  |   4 +-
 .../AccumuloFileOutputFormatTest.java         |   4 +-
 .../accumulo/core/file/rfile/RFileTest.java   |   6 +-
 .../core/iterators/SortedMapIteratorTest.java |   4 +-
 .../src/main/resources/examples/README.sample |   6 +-
 .../examples/simple/sample/SampleExample.java |   4 +-
 .../shard/CutoffIntersectingIterator.java     |   4 +-
 .../accumulo/examples/simple/shard/Query.java |   2 +-
 .../SimpleIteratorEnvironment.java            |   2 +-
 .../monitor/servlets/trace/NullScanner.java   |   2 +-
 .../apache/accumulo/tserver/InMemoryMap.java  |   2 +-
 .../tserver/TabletIteratorEnvironment.java    |   2 +-
 .../tserver/session/MultiScanSession.java     |   2 +-
 .../tserver/tablet/ScanDataSource.java        |   2 +-
 .../accumulo/tserver/tablet/ScanOptions.java  |   2 +-
 .../accumulo/tserver/tablet/Tablet.java       |   2 +-
 .../accumulo/tserver/InMemoryMapTest.java     |   6 +-
 .../accumulo/shell/commands/ScanCommand.java  |   2 +-
 .../org/apache/accumulo/test/SampleIT.java    |   5 +-
 .../mapred/AccumuloFileOutputFormatIT.java    |   4 +-
 .../test/mapred/AccumuloInputFormatIT.java    |   4 +-
 .../mapreduce/AccumuloFileOutputFormatIT.java |   4 +-
 .../test/mapreduce/AccumuloInputFormatIT.java |   4 +-
 57 files changed, 212 insertions(+), 93 deletions(-)
 rename core/src/main/java/org/apache/accumulo/core/{ => client}/sample/AbstractHashSampler.java (84%)
 rename core/src/main/java/org/apache/accumulo/core/{ => client}/sample/RowColumnSampler.java (87%)
 rename core/src/main/java/org/apache/accumulo/core/{ => client}/sample/RowSampler.java (85%)
 rename core/src/main/java/org/apache/accumulo/core/{ => client}/sample/Sampler.java (96%)
 rename core/src/main/java/org/apache/accumulo/core/client/{admin => sample}/SamplerConfiguration.java (94%)
 create mode 100644 core/src/main/java/org/apache/accumulo/core/sample/impl/DataoutputHasher.java

diff --git a/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java b/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
index 6783148c3..9f9449ad1 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ClientSideIteratorScanner.java
@@ -29,8 +29,8 @@ import java.util.TreeSet;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java b/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
index 2f6644516..354f6f43e 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/ScannerBase.java
@@ -21,7 +21,7 @@ import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
index d2d400ed4..994b653b4 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/NewTableConfiguration.java
@@ -23,6 +23,7 @@ import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.iterators.IteratorUtil;
 import org.apache.accumulo.core.iterators.user.VersioningIterator;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
index 59ac4ef87..f292902d4 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/admin/TableOperations.java
@@ -31,6 +31,7 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.TableExistsException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java b/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
index dc138ceb7..7b1c4413c 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/BaseIteratorEnvironment.java
@@ -19,7 +19,7 @@ package org.apache.accumulo.core.client.impl;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
index 87abd0b32..c5017c3b1 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/OfflineIterator.java
@@ -35,7 +35,7 @@ import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
index 54fed155b..c3a1a63bb 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ScannerOptions.java
@@ -33,7 +33,7 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
index 5a685d851..1940a2644 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/TableOperationsImpl.java
@@ -69,12 +69,12 @@ import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.FindMax;
 import org.apache.accumulo.core.client.admin.Locations;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TableOperations;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
 import org.apache.accumulo.core.client.impl.thrift.ClientService;
 import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.thrift.TDiskUsage;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
 import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
index ed8e95a86..9a7f7824e 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
++ b/core/src/main/java/org/apache/accumulo/core/client/impl/ThriftScanner.java
@@ -36,9 +36,9 @@ import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator.TabletLocation;
 import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.KeyValue;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
index 794500e47..dd92ae3b0 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AbstractInputFormat.java
@@ -43,7 +43,6 @@ import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
 import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.client.impl.ClientContext;
@@ -58,6 +57,7 @@ import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
 import org.apache.accumulo.core.client.mapreduce.impl.SplitUtils;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
index 45796cb0c..1e90e27dd 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormat.java
@@ -19,9 +19,9 @@ package org.apache.accumulo.core.client.mapred;
 import java.io.IOException;
 import java.util.Arrays;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ArrayByteSequence;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
index ed8accd55..0cf57d257 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapred/InputFormatBase.java
@@ -27,9 +27,9 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
index cf168cd0d..e7cb8ec3b 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AbstractInputFormat.java
@@ -43,7 +43,6 @@ import org.apache.accumulo.core.client.TableDeletedException;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.TableOfflineException;
 import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.SecurityOperations;
 import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
 import org.apache.accumulo.core.client.impl.ClientContext;
@@ -57,6 +56,7 @@ import org.apache.accumulo.core.client.mapreduce.impl.BatchInputSplit;
 import org.apache.accumulo.core.client.mapreduce.impl.SplitUtils;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.DelegationToken;
 import org.apache.accumulo.core.client.security.tokens.KerberosToken;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
index bf0474e9c..b337f569d 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormat.java
@@ -19,8 +19,8 @@ package org.apache.accumulo.core.client.mapreduce;
 import java.io.IOException;
 import java.util.Arrays;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ArrayByteSequence;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
index 2f779280e..324d5c731 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputFormatBase.java
@@ -27,9 +27,9 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
index 305e7e232..df0aa6525 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/InputTableConfig.java
@@ -26,7 +26,7 @@ import java.util.List;
 
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.hadoop.io.Text;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
index b4f9dca63..1786eb393 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/RangeInputSplit.java
@@ -32,9 +32,9 @@ import org.apache.accumulo.core.client.ClientConfiguration;
 import org.apache.accumulo.core.client.Instance;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.impl.SplitUtils;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase.TokenSource;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
index c629c28f0..049395f48 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/FileOutputConfigurator.java
@@ -21,7 +21,7 @@ import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
index f96c8f4ca..448b45e00 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mapreduce/lib/impl/InputConfigurator.java
@@ -47,13 +47,13 @@ import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.RowIterator;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ClientContext;
 import org.apache.accumulo.core.client.impl.Credentials;
 import org.apache.accumulo.core.client.impl.DelegationTokenImpl;
 import org.apache.accumulo.core.client.impl.Tables;
 import org.apache.accumulo.core.client.impl.TabletLocator;
 import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.PartialKey;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
index 9302fc968..a79255cea 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockScannerBase.java
@@ -24,8 +24,8 @@ import java.util.Iterator;
 import java.util.Map.Entry;
 
 import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
diff --git a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
index fd1128c4b..2072e00c6 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/client/mock/MockTableOperations.java
@@ -41,10 +41,10 @@ import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.FindMax;
 import org.apache.accumulo.core.client.admin.Locations;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TimeType;
 import org.apache.accumulo.core.client.impl.TableOperationsHelper;
 import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java b/core/src/main/java/org/apache/accumulo/core/client/sample/AbstractHashSampler.java
similarity index 84%
rename from core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java
rename to core/src/main/java/org/apache/accumulo/core/client/sample/AbstractHashSampler.java
index cbb0f3137..5c8176a81 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/AbstractHashSampler.java
++ b/core/src/main/java/org/apache/accumulo/core/client/sample/AbstractHashSampler.java
@@ -15,19 +15,21 @@
  * limitations under the License.
  */
 
package org.apache.accumulo.core.sample;
package org.apache.accumulo.core.client.sample;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static java.util.Objects.requireNonNull;
 
import java.io.DataOutput;
import java.io.IOException;
 import java.util.Set;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.sample.impl.DataoutputHasher;
 
 import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
 import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
 import com.google.common.hash.Hashing;
 
 /**
@@ -98,11 +100,20 @@ public abstract class AbstractHashSampler implements Sampler {
 
   /**
    * Subclass must override this method and hash some portion of the key.
   *
   * @param hasher
   *          Data written to this will be used to compute the hash for the key.
    */
  protected abstract HashCode hash(HashFunction hashFunction, Key k);
  protected abstract void hash(DataOutput hasher, Key k) throws IOException;
 
   @Override
   public boolean accept(Key k) {
    return hash(hashFunction, k).asInt() % modulus == 0;
    Hasher hasher = hashFunction.newHasher();
    try {
      hash(new DataoutputHasher(hasher), k);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return hasher.hash().asInt() % modulus == 0;
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java b/core/src/main/java/org/apache/accumulo/core/client/sample/RowColumnSampler.java
similarity index 87%
rename from core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java
rename to core/src/main/java/org/apache/accumulo/core/client/sample/RowColumnSampler.java
index c3464abe1..a0482d96b 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/RowColumnSampler.java
++ b/core/src/main/java/org/apache/accumulo/core/client/sample/RowColumnSampler.java
@@ -15,18 +15,16 @@
  * limitations under the License.
  */
 
package org.apache.accumulo.core.sample;
package org.apache.accumulo.core.client.sample;
 
import java.io.DataOutput;
import java.io.IOException;
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
@@ -95,14 +93,12 @@ public class RowColumnSampler extends AbstractHashSampler {
     }
   }
 
  private void putByteSquence(ByteSequence data, Hasher hasher) {
    hasher.putBytes(data.getBackingArray(), data.offset(), data.length());
  private void putByteSquence(ByteSequence data, DataOutput hasher) throws IOException {
    hasher.write(data.getBackingArray(), data.offset(), data.length());
   }
 
   @Override
  protected HashCode hash(HashFunction hashFunction, Key k) {
    Hasher hasher = hashFunction.newHasher();

  protected void hash(DataOutput hasher, Key k) throws IOException {
     if (row) {
       putByteSquence(k.getRowData(), hasher);
     }
@@ -118,7 +114,5 @@ public class RowColumnSampler extends AbstractHashSampler {
     if (visibility) {
       putByteSquence(k.getColumnVisibilityData(), hasher);
     }

    return hasher.hash();
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java b/core/src/main/java/org/apache/accumulo/core/client/sample/RowSampler.java
similarity index 85%
rename from core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java
rename to core/src/main/java/org/apache/accumulo/core/client/sample/RowSampler.java
index 8690a1c4e..107ba49f6 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/RowSampler.java
++ b/core/src/main/java/org/apache/accumulo/core/client/sample/RowSampler.java
@@ -15,14 +15,14 @@
  * limitations under the License.
  */
 
package org.apache.accumulo.core.sample;
package org.apache.accumulo.core.client.sample;

import java.io.DataOutput;
import java.io.IOException;
 
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

 /**
  * Builds a sample based on entire rows. If a row is selected for the sample, then all of its columns will be included.
  *
@@ -42,8 +42,8 @@ import com.google.common.hash.HashFunction;
 public class RowSampler extends AbstractHashSampler {
 
   @Override
  protected HashCode hash(HashFunction hashFunction, Key k) {
  protected void hash(DataOutput hasher, Key k) throws IOException {
     ByteSequence row = k.getRowData();
    return hashFunction.hashBytes(row.getBackingArray(), row.offset(), row.length());
    hasher.write(row.getBackingArray(), row.offset(), row.length());
   }
 }
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/Sampler.java b/core/src/main/java/org/apache/accumulo/core/client/sample/Sampler.java
similarity index 96%
rename from core/src/main/java/org/apache/accumulo/core/sample/Sampler.java
rename to core/src/main/java/org/apache/accumulo/core/client/sample/Sampler.java
index 64adeecb2..03bd9d712 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/Sampler.java
++ b/core/src/main/java/org/apache/accumulo/core/client/sample/Sampler.java
@@ -15,9 +15,8 @@
  * limitations under the License.
  */
 
package org.apache.accumulo.core.sample;
package org.apache.accumulo.core.client.sample;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 
 /**
diff --git a/core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java b/core/src/main/java/org/apache/accumulo/core/client/sample/SamplerConfiguration.java
similarity index 94%
rename from core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java
rename to core/src/main/java/org/apache/accumulo/core/client/sample/SamplerConfiguration.java
index a2bd5cc57..e774ec5a6 100644
-- a/core/src/main/java/org/apache/accumulo/core/client/admin/SamplerConfiguration.java
++ b/core/src/main/java/org/apache/accumulo/core/client/sample/SamplerConfiguration.java
@@ -15,7 +15,7 @@
  * limitations under the License.
  */
 
package org.apache.accumulo.core.client.admin;
package org.apache.accumulo.core.client.sample;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static java.util.Objects.requireNonNull;
@@ -36,6 +36,10 @@ public class SamplerConfiguration {
   private String className;
   private Map<String,String> options = new HashMap<>();
 
  public SamplerConfiguration(Class<? extends Sampler> samplerClass) {
    this(samplerClass.getName());
  }

   public SamplerConfiguration(String samplerClassName) {
     requireNonNull(samplerClassName);
     this.className = samplerClassName;
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
index 6032c7f20..981a2e645 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFile.java
@@ -39,7 +39,8 @@ import java.util.TreeMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ArrayByteSequence;
@@ -66,7 +67,6 @@ import org.apache.accumulo.core.iterators.system.HeapIterator;
 import org.apache.accumulo.core.iterators.system.InterruptibleIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator.LocalityGroup;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.util.MutableByteSequence;
 import org.apache.commons.lang.mutable.MutableLong;
diff --git a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
index cc6aaa26c..5d159733d 100644
-- a/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
++ b/core/src/main/java/org/apache/accumulo/core/file/rfile/RFileOperations.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 import java.util.Collection;
 import java.util.Collections;
 
import org.apache.accumulo.core.client.sample.Sampler;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.data.ByteSequence;
@@ -30,7 +31,6 @@ import org.apache.accumulo.core.file.FileSKVIterator;
 import org.apache.accumulo.core.file.FileSKVWriter;
 import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
 import org.apache.accumulo.core.file.streams.RateLimitedOutputStream;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.hadoop.conf.Configuration;
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java b/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
index 5c265e204..7ef27e56a 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/IteratorEnvironment.java
@@ -19,7 +19,7 @@ package org.apache.accumulo.core.iterators;
 import java.io.IOException;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java b/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
index aedcdbaea..8b488c836 100644
-- a/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
++ b/core/src/main/java/org/apache/accumulo/core/iterators/system/SampleIterator.java
@@ -17,13 +17,13 @@
 
 package org.apache.accumulo.core.iterators.system;
 
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.Sampler;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.Filter;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;
 
 public class SampleIterator extends Filter {
 
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/impl/DataoutputHasher.java b/core/src/main/java/org/apache/accumulo/core/sample/impl/DataoutputHasher.java
new file mode 100644
index 000000000..d243dfe46
-- /dev/null
++ b/core/src/main/java/org/apache/accumulo/core/sample/impl/DataoutputHasher.java
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

package org.apache.accumulo.core.sample.impl;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hasher;

public class DataoutputHasher implements DataOutput {

  private Hasher hasher;

  public DataoutputHasher(Hasher hasher) {
    this.hasher = hasher;
  }

  @Override
  public void write(int b) throws IOException {
    hasher.putByte((byte) (0xff & b));
  }

  @Override
  public void write(byte[] b) throws IOException {
    hasher.putBytes(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    hasher.putBytes(b, off, len);
  }

  @Override
  public void writeBoolean(boolean v) throws IOException {
    hasher.putBoolean(v);
  }

  @Override
  public void writeByte(int v) throws IOException {
    hasher.putByte((byte) (0xff & v));

  }

  @Override
  public void writeShort(int v) throws IOException {
    hasher.putShort((short) (0xffff & v));
  }

  @Override
  public void writeChar(int v) throws IOException {
    hasher.putChar((char) v);
  }

  @Override
  public void writeInt(int v) throws IOException {
    hasher.putInt(v);
  }

  @Override
  public void writeLong(long v) throws IOException {
    hasher.putLong(v);
  }

  @Override
  public void writeFloat(float v) throws IOException {
    hasher.putDouble(v);
  }

  @Override
  public void writeDouble(double v) throws IOException {
    hasher.putDouble(v);
  }

  @Override
  public void writeBytes(String s) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      hasher.putByte((byte) (0xff & s.charAt(i)));
    }
  }

  @Override
  public void writeChars(String s) throws IOException {
    hasher.putString(s);

  }

  @Override
  public void writeUTF(String s) throws IOException {
    hasher.putInt(s.length());
    hasher.putBytes(s.getBytes(StandardCharsets.UTF_8));
  }
}
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
index 348def42e..f0bd528b9 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
++ b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerConfigurationImpl.java
@@ -28,7 +28,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
 import org.apache.accumulo.core.tabletserver.thrift.TSamplerConfiguration;
diff --git a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
index 3f11fbee7..d70f3af25 100644
-- a/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
++ b/core/src/main/java/org/apache/accumulo/core/sample/impl/SamplerFactory.java
@@ -19,9 +19,9 @@ package org.apache.accumulo.core.sample.impl;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.sample.Sampler;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
 
 public class SamplerFactory {
diff --git a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
index 86857fa93..c55b62fb8 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/impl/TableOperationsHelperTest.java
@@ -37,8 +37,8 @@ import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.DiskUsage;
 import org.apache.accumulo.core.client.admin.Locations;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
 import org.apache.accumulo.core.security.Authorizations;
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
index d88453ee5..d85db927a 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapred/AccumuloFileOutputFormatTest.java
@@ -20,11 +20,11 @@ import static org.junit.Assert.assertEquals;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.mapred.JobConf;
 import org.junit.Test;
diff --git a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
index cf0c8d652..39d226be9 100644
-- a/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
++ b/core/src/test/java/org/apache/accumulo/core/client/mapreduce/AccumuloFileOutputFormatTest.java
@@ -20,11 +20,11 @@ import static org.junit.Assert.assertEquals;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.FileOutputConfigurator;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.hadoop.mapreduce.Job;
 import org.junit.Test;
diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
index d97a4db71..8db1b217f 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
@@ -42,8 +42,10 @@ import java.util.Random;
 import java.util.Set;
 
 import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
@@ -64,8 +66,6 @@ import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
 import org.apache.accumulo.core.metadata.MetadataTable;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema;
 import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.security.crypto.CryptoTest;
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
index 7557b9a15..d4080e1a6 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/SortedMapIteratorTest.java
@@ -19,11 +19,11 @@ package org.apache.accumulo.core.iterators;
 import java.util.TreeMap;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.junit.Test;
 
 public class SortedMapIteratorTest {
diff --git a/docs/src/main/resources/examples/README.sample b/docs/src/main/resources/examples/README.sample
index 15288aaed..3642cc66d 100644
-- a/docs/src/main/resources/examples/README.sample
++ b/docs/src/main/resources/examples/README.sample
@@ -40,7 +40,7 @@ tables sample data.
 
     root@instance sampex> config -t sampex -s table.sampler.opt.hasher=murmur3_32
     root@instance sampex> config -t sampex -s table.sampler.opt.modulus=3
    root@instance sampex> config -t sampex -s table.sampler=org.apache.accumulo.core.sample.RowSampler
    root@instance sampex> config -t sampex -s table.sampler=org.apache.accumulo.core.client.sample.RowSampler
 
 Below, attempting to scan the sample returns an error.  This is because data
 was inserted before the sample set was configured.
@@ -123,7 +123,7 @@ configuration for sample scan to work.
     	Column families        : [doc]
     
     Sample Configuration     :
    	Sampler class          : org.apache.accumulo.core.sample.RowSampler
    	Sampler class          : org.apache.accumulo.core.client.sample.RowSampler
     	Sampler options        : {hasher=murmur3_32, modulus=2}
 
     Sample Locality group    : <DEFAULT>
@@ -159,7 +159,7 @@ shard table based on the column qualifier.
     root@instance shard> config -t shard -s table.sampler.opt.hasher=murmur3_32
     root@instance shard> config -t shard -s table.sampler.opt.modulus=101
     root@instance shard> config -t shard -s table.sampler.opt.qualifier=true
    root@instance shard> config -t shard -s table.sampler=org.apache.accumulo.core.sample.RowColumnSampler
    root@instance shard> config -t shard -s table.sampler=org.apache.accumulo.core.client.sample.RowColumnSampler
     root@instance shard> compact -t shard --sf-no-sample -w
     2015-07-23 15:00:09,280 [shell.Shell] INFO : Compacting table ...
     2015-07-23 15:00:10,134 [shell.Shell] INFO : Compaction of table shard completed for given range
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
index 57d77b177..262e63d23 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/sample/SampleExample.java
@@ -28,11 +28,11 @@ import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.examples.simple.client.RandomBatchWriter;
 import org.apache.accumulo.examples.simple.shard.CutoffIntersectingIterator;
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
index 9f13dd488..f5dce1d13 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/CutoffIntersectingIterator.java
@@ -25,7 +25,8 @@ import java.util.Collection;
 import java.util.Map;
 
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.RowColumnSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
@@ -33,7 +34,6 @@ import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.user.IntersectingIterator;
import org.apache.accumulo.core.sample.RowColumnSampler;
 
 /**
  * This iterator uses a sample built from the Column Qualifier to quickly avoid intersecting iterator queries that may return too many documents.
diff --git a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
index 79258554b..72a638419 100644
-- a/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
++ b/examples/simple/src/main/java/org/apache/accumulo/examples/simple/shard/Query.java
@@ -27,7 +27,7 @@ import org.apache.accumulo.core.cli.ClientOnRequiredTable;
 import org.apache.accumulo.core.client.BatchScanner;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
diff --git a/iterator-test-harness/src/main/java/org/apache/accumulo/iteratortest/environments/SimpleIteratorEnvironment.java b/iterator-test-harness/src/main/java/org/apache/accumulo/iteratortest/environments/SimpleIteratorEnvironment.java
index 620421244..bbe625df0 100644
-- a/iterator-test-harness/src/main/java/org/apache/accumulo/iteratortest/environments/SimpleIteratorEnvironment.java
++ b/iterator-test-harness/src/main/java/org/apache/accumulo/iteratortest/environments/SimpleIteratorEnvironment.java
@@ -18,7 +18,7 @@ package org.apache.accumulo.iteratortest.environments;
 
 import java.io.IOException;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
index a935948d9..b91d4545c 100644
-- a/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
++ b/server/monitor/src/main/java/org/apache/accumulo/monitor/servlets/trace/NullScanner.java
@@ -22,8 +22,8 @@ import java.util.concurrent.TimeUnit;
 
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.IteratorSetting.Column;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
index 1451ddb98..a87586b8c 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/InMemoryMap.java
@@ -38,6 +38,7 @@ import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.atomic.AtomicReference;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.sample.Sampler;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.Property;
@@ -62,7 +63,6 @@ import org.apache.accumulo.core.iterators.system.LocalityGroupIterator;
 import org.apache.accumulo.core.iterators.system.LocalityGroupIterator.LocalityGroup;
 import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator;
 import org.apache.accumulo.core.iterators.system.SourceSwitchingIterator.DataSource;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.util.CachedConfiguration;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
index 73adec39f..f9081b462 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/TabletIteratorEnvironment.java
@@ -22,7 +22,7 @@ import java.util.Collections;
 import java.util.Map;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
index 2356d362e..bb8408522 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/session/MultiScanSession.java
@@ -20,7 +20,7 @@ import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.impl.KeyExtent;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
index 6a4b6e426..ea1f786aa 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanDataSource.java
@@ -26,7 +26,7 @@ import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Value;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
index 2e2445ad2..dceac0808 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/ScanOptions.java
@@ -21,7 +21,7 @@ import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Column;
 import org.apache.accumulo.core.data.thrift.IterInfo;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
index 7b59ceb05..15b522675 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/tablet/Tablet.java
@@ -51,9 +51,9 @@ import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.client.Durability;
 import org.apache.accumulo.core.client.IteratorSetting;
 import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.DurabilityImpl;
 import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.ConfigurationObserver;
diff --git a/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java b/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
index 7b4d447e5..264145f7d 100644
-- a/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
++ b/server/tserver/src/test/java/org/apache/accumulo/tserver/InMemoryMapTest.java
@@ -37,8 +37,10 @@ import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.BaseIteratorEnvironment;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.Sampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.conf.DefaultConfiguration;
 import org.apache.accumulo.core.conf.Property;
@@ -51,8 +53,6 @@ import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.IterationInterruptedException;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.system.ColumnFamilySkippingIterator;
import org.apache.accumulo.core.sample.RowSampler;
import org.apache.accumulo.core.sample.Sampler;
 import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
 import org.apache.accumulo.core.sample.impl.SamplerFactory;
 import org.apache.accumulo.core.util.LocalityGroupUtil;
diff --git a/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java b/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
index cd5156490..e1da444db 100644
-- a/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
++ b/shell/src/main/java/org/apache/accumulo/shell/commands/ScanCommand.java
@@ -29,7 +29,7 @@ import org.apache.accumulo.core.client.SampleNotPresentException;
 import org.apache.accumulo.core.client.Scanner;
 import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
diff --git a/test/src/main/java/org/apache/accumulo/test/SampleIT.java b/test/src/main/java/org/apache/accumulo/test/SampleIT.java
index 423b955ba..196672a87 100644
-- a/test/src/main/java/org/apache/accumulo/test/SampleIT.java
++ b/test/src/main/java/org/apache/accumulo/test/SampleIT.java
@@ -45,9 +45,10 @@ import org.apache.accumulo.core.client.ScannerBase;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.CompactionConfig;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.impl.Credentials;
 import org.apache.accumulo.core.client.impl.OfflineScanner;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
@@ -56,7 +57,6 @@ import org.apache.accumulo.core.data.Value;
 import org.apache.accumulo.core.iterators.IteratorEnvironment;
 import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
 import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
 import org.junit.Assert;
@@ -125,6 +125,7 @@ public class SampleIT extends AccumuloClusterHarness {
 
     TreeMap<Key,Value> expected = new TreeMap<Key,Value>();
     String someRow = writeData(bw, SC1, expected);
    Assert.assertEquals(20, expected.size());
 
     Scanner scanner = conn.createScanner(tableName, Authorizations.EMPTY);
     Scanner isoScanner = new IsolatedScanner(conn.createScanner(tableName, Authorizations.EMPTY));
diff --git a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
index 44eee4c6d..aa1925005 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloFileOutputFormatIT.java
@@ -30,17 +30,17 @@ import java.io.IOException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapred.AccumuloFileOutputFormat;
 import org.apache.accumulo.core.client.mapred.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
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
diff --git a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
index 2a15041e2..c855b39b7 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapred/AccumuloInputFormatIT.java
@@ -28,13 +28,13 @@ import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapred.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapred.RangeInputSplit;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
diff --git a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
index 2e03658a1..e160077cf 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloFileOutputFormatIT.java
@@ -27,16 +27,16 @@ import java.io.IOException;
 import org.apache.accumulo.core.client.BatchWriter;
 import org.apache.accumulo.core.client.BatchWriterConfig;
 import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloFileOutputFormat;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
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
diff --git a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
index a0941496f..11e7d7c2b 100644
-- a/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
++ b/test/src/main/java/org/apache/accumulo/test/mapreduce/AccumuloInputFormatIT.java
@@ -40,10 +40,11 @@ import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
 import org.apache.accumulo.core.client.Connector;
 import org.apache.accumulo.core.client.TableNotFoundException;
 import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.SamplerConfiguration;
 import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
 import org.apache.accumulo.core.client.mapreduce.RangeInputSplit;
 import org.apache.accumulo.core.client.mapreduce.impl.BatchInputSplit;
import org.apache.accumulo.core.client.sample.RowSampler;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
 import org.apache.accumulo.core.client.security.tokens.PasswordToken;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.conf.ConfigurationCopy;
@@ -53,7 +54,6 @@ import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Mutation;
 import org.apache.accumulo.core.data.Range;
 import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.sample.RowSampler;
 import org.apache.accumulo.core.security.Authorizations;
 import org.apache.accumulo.core.util.Pair;
 import org.apache.accumulo.harness.AccumuloClusterHarness;
- 
2.19.1.windows.1

