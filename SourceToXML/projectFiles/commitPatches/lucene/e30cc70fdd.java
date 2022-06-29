From e30cc70fddcdd6fddb5eedf9f38e77fcb3f33bd1 Mon Sep 17 00:00:00 2001
From: Andrzej Bialecki <ab@apache.org>
Date: Tue, 11 Apr 2017 19:22:23 +0200
Subject: [PATCH] SOLR-9959: SolrInfoMBean-s category and hierarchy cleanup.

--
 .../.idea/libraries/Solr_DIH_core_library.xml |  10 +
 .../dataimporthandler/dataimporthandler.iml   |   3 +-
 lucene/tools/junit4/cached-timehints.txt      |   2 +-
 solr/CHANGES.txt                              |  17 +
 .../plugin/AnalyticsStatisticsCollector.java  |  31 +-
 .../handler/component/AnalyticsComponent.java |  16 +-
 .../handler/dataimport/DataImportHandler.java |  57 +--
 .../org/apache/solr/core/CoreContainer.java   |  65 ++-
 .../apache/solr/core/DirectoryFactory.java    |   8 -
 .../solr/core/HdfsDirectoryFactory.java       |  10 +-
 .../org/apache/solr/core/JmxMonitoredMap.java | 478 ------------------
 .../java/org/apache/solr/core/PluginBag.java  |   4 +-
 .../java/org/apache/solr/core/SolrConfig.java |  52 +-
 .../java/org/apache/solr/core/SolrCore.java   | 171 +++----
 .../org/apache/solr/core/SolrInfoBean.java    |  95 ++++
 .../org/apache/solr/core/SolrInfoMBean.java   |  76 ---
 .../solr/core/SolrInfoMBeanWrapper.java       |  62 ---
 .../apache/solr/core/SolrResourceLoader.java  |  18 +-
 .../org/apache/solr/core/SolrXmlConfig.java   |  36 +-
 .../apache/solr/handler/AnalyzeEvaluator.java |   3 -
 .../solr/handler/MoreLikeThisHandler.java     |  10 -
 .../solr/handler/RealTimeGetHandler.java      |   6 -
 .../solr/handler/ReplicationHandler.java      | 109 ++--
 .../solr/handler/RequestHandlerBase.java      |  62 +--
 .../solr/handler/SolrConfigHandler.java       |   6 -
 .../solr/handler/StandardRequestHandler.java  |  11 -
 .../solr/handler/admin/CoreAdminHandler.java  |   2 +-
 .../handler/admin/LukeRequestHandler.java     |  10 -
 .../admin/MetricsCollectorHandler.java        |   2 +-
 .../solr/handler/admin/MetricsHandler.java    |  42 +-
 .../solr/handler/admin/PluginInfoHandler.java |  27 +-
 .../handler/admin/SolrInfoMBeanHandler.java   |  32 +-
 .../solr/handler/admin/SystemInfoHandler.java |  66 +--
 .../handler/component/DebugComponent.java     |   8 +-
 .../handler/component/ExpandComponent.java    |  15 +-
 .../handler/component/FacetComponent.java     |   8 +-
 .../handler/component/HighlightComponent.java |   8 +-
 .../component/HttpShardHandlerFactory.java    |   6 +-
 .../component/MoreLikeThisComponent.java      |   8 +-
 .../handler/component/QueryComponent.java     |   8 +-
 .../component/QueryElevationComponent.java    |  14 +-
 .../component/RealTimeGetComponent.java       |  10 +-
 .../handler/component/SearchComponent.java    |  31 +-
 .../component/SpellCheckComponent.java        |   2 +-
 .../handler/component/StatsComponent.java     |   2 +-
 .../handler/component/SuggestComponent.java   |  27 +-
 .../highlight/DefaultSolrHighlighter.java     |  30 +-
 .../apache/solr/highlight/GapFragmenter.java  |   2 +-
 .../highlight/HighlightingPluginBase.java     |  41 +-
 .../apache/solr/highlight/HtmlFormatter.java  |   2 +-
 .../solr/highlight/RegexFragmenter.java       |   2 +-
 .../solr/highlight/SimpleFragListBuilder.java |   2 +-
 .../solr/highlight/SingleFragListBuilder.java |   2 +-
 .../solr/highlight/SolrBoundaryScanner.java   |   6 +-
 .../apache/solr/highlight/SolrEncoder.java    |   4 +-
 .../apache/solr/highlight/SolrFormatter.java  |   4 +-
 .../solr/highlight/SolrFragListBuilder.java   |   4 +-
 .../apache/solr/highlight/SolrFragmenter.java |   4 +-
 .../solr/highlight/SolrFragmentsBuilder.java  |   6 +-
 .../highlight/WeightedFragListBuilder.java    |   2 +-
 .../solr/metrics/AltBufferPoolMetricSet.java  |  47 ++
 .../org/apache/solr/metrics/MetricsMap.java   | 184 +++++++
 .../metrics/OperatingSystemMetricSet.java     |  66 +--
 .../solr/metrics/SolrCoreMetricManager.java   |  27 +-
 .../apache/solr/metrics/SolrMetricInfo.java   |  23 +-
 .../solr/metrics/SolrMetricManager.java       | 144 +++++-
 .../solr/metrics/SolrMetricReporter.java      |  12 +
 .../reporters/JmxObjectNameFactory.java       |  63 ++-
 .../reporters/ReporterClientCache.java        |  84 +++
 .../reporters/SolrGangliaReporter.java        |  48 +-
 .../reporters/SolrGraphiteReporter.java       |  46 +-
 .../metrics/reporters/SolrJmxReporter.java    | 206 +++++++-
 .../metrics/reporters/SolrSlf4jReporter.java  |  29 +-
 .../reporters/solr/SolrClusterReporter.java   |  28 +-
 .../reporters/solr/SolrShardReporter.java     |  17 +-
 .../solr/request/SolrRequestHandler.java      |   4 +-
 .../org/apache/solr/search/FastLRUCache.java  | 126 +++--
 .../java/org/apache/solr/search/LFUCache.java | 147 +++---
 .../java/org/apache/solr/search/LRUCache.java |  82 +--
 .../org/apache/solr/search/QParserPlugin.java |  22 +-
 .../org/apache/solr/search/SolrCache.java     |   5 +-
 .../org/apache/solr/search/SolrCacheBase.java |   7 +-
 .../solr/search/SolrFieldCacheBean.java       |  77 +++
 .../solr/search/SolrFieldCacheMBean.java      |  78 ---
 .../apache/solr/search/SolrIndexSearcher.java |  76 ++-
 .../apache/solr/search/facet/FacetModule.java |   6 -
 .../solr/servlet/SolrDispatchFilter.java      |  13 +-
 .../apache/solr/store/blockcache/Metrics.java | 124 ++---
 .../solr/store/hdfs/HdfsLocalityReporter.java | 141 +++---
 .../solr/update/DirectUpdateHandler2.java     | 116 ++---
 .../org/apache/solr/update/HdfsUpdateLog.java |   4 +-
 .../java/org/apache/solr/update/PeerSync.java |  10 +-
 .../apache/solr/update/SolrIndexWriter.java   |  26 +-
 .../org/apache/solr/update/UpdateHandler.java |  18 +-
 .../org/apache/solr/update/UpdateLog.java     |  16 +-
 .../solr/update/UpdateShardHandler.java       |  45 +-
 .../java/org/apache/solr/util/JmxUtil.java    |   3 -
 ...tedPoolingHttpClientConnectionManager.java |   8 +-
 .../apache/solr/util/stats/MetricUtils.java   | 439 ++++++++++++----
 .../src/test-files/solr/solr-jmxreporter.xml  |  43 ++
 .../src/test-files/solr/solr-solrreporter.xml |   4 +
 .../apache/solr/BasicFunctionalityTest.java   |  17 +-
 .../org/apache/solr/CursorPagingTest.java     |  22 +-
 ...foMBeanTest.java => SolrInfoBeanTest.java} |  22 +-
 .../solr/cloud/BasicDistributedZkTest.java    |  24 +-
 .../org/apache/solr/cloud/BasicZkTest.java    |  15 +-
 .../CollectionsAPIDistributedZkTest.java      |  36 +-
 .../solr/cloud/ReplicationFactorTest.java     |   9 -
 .../cloud/TestRandomRequestDistribution.java  |  19 +-
 .../core/ExitableDirectoryReaderTest.java     |  26 +-
 .../solr/core/HdfsDirectoryFactoryTest.java   |  29 +-
 .../org/apache/solr/core/MockInfoBean.java    |  71 +++
 .../org/apache/solr/core/MockInfoMBean.java   |  69 ---
 .../MockQuerySenderListenerReqHandler.java    |  15 +-
 .../apache/solr/core/RequestHandlersTest.java |  21 +-
 .../org/apache/solr/core/SolrCoreTest.java    |   4 +-
 .../apache/solr/core/TestJmxIntegration.java  |  94 ++--
 .../apache/solr/core/TestJmxMonitoredMap.java | 217 --------
 .../solr/core/TestSolrDynamicMBean.java       |  87 ----
 .../solr/handler/admin/MBeansHandlerTest.java |  14 +-
 .../handler/admin/MetricsHandlerTest.java     |  15 +
 .../handler/admin/StatsReloadRaceTest.java    |  40 +-
 .../handler/admin/SystemInfoHandlerTest.java  |   8 +-
 .../ResourceSharingTestComponent.java         |   5 -
 .../apache/solr/metrics/JvmMetricsTest.java   |  53 +-
 .../metrics/SolrCoreMetricManagerTest.java    |   6 +-
 .../solr/metrics/SolrMetricManagerTest.java   |  44 +-
 .../solr/metrics/SolrMetricReporterTest.java  |   1 +
 .../solr/metrics/SolrMetricTestUtils.java     |  12 +-
 .../metrics/SolrMetricsIntegrationTest.java   |  17 +-
 .../reporters/SolrGangliaReporterTest.java    |   2 +-
 .../reporters/SolrGraphiteReporterTest.java   |   4 +-
 .../reporters/SolrJmxReporterTest.java        |  66 ++-
 .../reporters/SolrSlf4jReporterTest.java      |   2 +-
 .../solr/SolrCloudReportersTest.java          |   7 +-
 .../solr/search/MockSearchComponent.java      |   6 -
 .../apache/solr/search/TestFastLRUCache.java  |  32 +-
 .../apache/solr/search/TestIndexSearcher.java |   9 +-
 .../org/apache/solr/search/TestLFUCache.java  |  21 +-
 .../org/apache/solr/search/TestLRUCache.java  |  16 +-
 .../solr/search/TestReRankQParserPlugin.java  |  15 +-
 .../apache/solr/search/TestSolr4Spatial2.java |  11 +-
 ...MBean.java => TestSolrFieldCacheBean.java} |  38 +-
 .../solr/search/TestSolrQueryParser.java      |  38 +-
 .../solr/search/join/BJQParserTest.java       |  28 +-
 .../search/join/TestScoreJoinQPScore.java     |  52 +-
 .../store/blockcache/BufferStoreTest.java     |  15 +-
 .../solr/util/stats/MetricUtilsTest.java      |  11 +-
 .../solrj/impl/CloudSolrClientTest.java       |  24 +-
 .../java/org/apache/solr/SolrTestCaseJ4.java  |  14 +
 .../cloud/AbstractFullDistribZkTestBase.java  |   8 -
 .../solr/cloud/MiniSolrCloudCluster.java      |   5 +
 .../org/apache/solr/util/TestHarness.java     |   4 +-
 solr/webapp/web/css/angular/plugins.css       |  10 +-
 solr/webapp/web/partials/plugins.html         |   2 +-
 155 files changed, 3067 insertions(+), 2906 deletions(-)
 create mode 100644 dev-tools/idea/.idea/libraries/Solr_DIH_core_library.xml
 delete mode 100644 solr/core/src/java/org/apache/solr/core/JmxMonitoredMap.java
 create mode 100644 solr/core/src/java/org/apache/solr/core/SolrInfoBean.java
 delete mode 100644 solr/core/src/java/org/apache/solr/core/SolrInfoMBean.java
 delete mode 100644 solr/core/src/java/org/apache/solr/core/SolrInfoMBeanWrapper.java
 create mode 100644 solr/core/src/java/org/apache/solr/metrics/AltBufferPoolMetricSet.java
 create mode 100644 solr/core/src/java/org/apache/solr/metrics/MetricsMap.java
 create mode 100644 solr/core/src/java/org/apache/solr/metrics/reporters/ReporterClientCache.java
 create mode 100644 solr/core/src/java/org/apache/solr/search/SolrFieldCacheBean.java
 delete mode 100644 solr/core/src/java/org/apache/solr/search/SolrFieldCacheMBean.java
 create mode 100644 solr/core/src/test-files/solr/solr-jmxreporter.xml
 rename solr/core/src/test/org/apache/solr/{SolrInfoMBeanTest.java => SolrInfoBeanTest.java} (82%)
 create mode 100644 solr/core/src/test/org/apache/solr/core/MockInfoBean.java
 delete mode 100644 solr/core/src/test/org/apache/solr/core/MockInfoMBean.java
 delete mode 100644 solr/core/src/test/org/apache/solr/core/TestJmxMonitoredMap.java
 delete mode 100644 solr/core/src/test/org/apache/solr/core/TestSolrDynamicMBean.java
 rename solr/core/src/test/org/apache/solr/search/{TestSolrFieldCacheMBean.java => TestSolrFieldCacheBean.java} (59%)

diff --git a/dev-tools/idea/.idea/libraries/Solr_DIH_core_library.xml b/dev-tools/idea/.idea/libraries/Solr_DIH_core_library.xml
new file mode 100644
index 00000000000..d363b92ecd0
-- /dev/null
++ b/dev-tools/idea/.idea/libraries/Solr_DIH_core_library.xml
@@ -0,0 +1,10 @@
<component name="libraryTable">
  <library name="Solr DIH core library">
    <CLASSES>
      <root url="file://$PROJECT_DIR$/solr/contrib/dataimporthandler/lib" />
    </CLASSES>
    <JAVADOC />
    <SOURCES />
    <jarDirectory url="file://$PROJECT_DIR$/solr/contrib/dataimporthandler/lib" recursive="false" />
  </library>
</component>
diff --git a/dev-tools/idea/solr/contrib/dataimporthandler/dataimporthandler.iml b/dev-tools/idea/solr/contrib/dataimporthandler/dataimporthandler.iml
index 62682476351..8240ff2c8ee 100644
-- a/dev-tools/idea/solr/contrib/dataimporthandler/dataimporthandler.iml
++ b/dev-tools/idea/solr/contrib/dataimporthandler/dataimporthandler.iml
@@ -16,9 +16,10 @@
     <orderEntry type="library" scope="TEST" name="HSQLDB" level="project" />
     <orderEntry type="library" scope="TEST" name="Derby" level="project" />
     <orderEntry type="library" scope="TEST" name="Solr DIH test library" level="project" />
    <orderEntry type="library" scope="TEST" name="Solr example library" level="project" />
    <orderEntry type="library" name="Solr example library" level="project" />
     <orderEntry type="library" name="Solr core library" level="project" />
     <orderEntry type="library" name="Solrj library" level="project" />
    <orderEntry type="library" name="Solr DIH core library" level="project" />
     <orderEntry type="module" scope="TEST" module-name="lucene-test-framework" />
     <orderEntry type="module" scope="TEST" module-name="solr-test-framework" />
     <orderEntry type="module" module-name="solr-core" />
diff --git a/lucene/tools/junit4/cached-timehints.txt b/lucene/tools/junit4/cached-timehints.txt
index f2b8974a600..cb3da9964d1 100644
-- a/lucene/tools/junit4/cached-timehints.txt
++ b/lucene/tools/junit4/cached-timehints.txt
@@ -813,7 +813,7 @@ org.apache.solr.EchoParamsTest=136,170,349,124,140,142,284
 org.apache.solr.MinimalSchemaTest=304,316,467,304,297,755,309
 org.apache.solr.OutputWriterTest=302,276,265,314,244,211,268
 org.apache.solr.SampleTest=339,290,266,243,333,414,355
org.apache.solr.SolrInfoMBeanTest=1090,1132,644,629,637,1023,735
org.apache.solr.SolrInfoBeanTest=1090,1132,644,629,637,1023,735
 org.apache.solr.TestDistributedGrouping=13095,9478,8420,9633,10692,9265,10893
 org.apache.solr.TestDistributedSearch=11199,9886,16211,11367,11325,10717,10392
 org.apache.solr.TestDocumentBuilder=10,10,9,13,10,9,10
diff --git a/solr/CHANGES.txt b/solr/CHANGES.txt
index 7752a160c09..128e386dec1 100644
-- a/solr/CHANGES.txt
++ b/solr/CHANGES.txt
@@ -52,6 +52,19 @@ Upgrading from Solr 6.x
 
 * Deprecated method getNumericType() has been removed from FieldType. Use getNumberType() instead
 
* MBean names and attributes now follow hierarchical names used in metrics. This is reflected also in
  /admin/mbeans and /admin/plugins output, and can be observed in the UI Plugins tab, because now all these
  APIs get their data from the metrics API. The old (mostly flat) JMX view has been removed.

* <jmx> element in solrconfig.xml is no longer supported. Equivalent functionality can be configured in
  solr.xml using <metrics><reporter ...> element and SolrJmxReporter implementation. Limited back-compatibility
  is offered by automatically adding a default instance of SolrJmxReporter if it's missing, AND when a local
  MBean server is found (which can be activated either via ENABLE_REMOTE_JMX_OPTS in solr.in.sh or via system
  properties, eg. -Dcom.sun.management.jmxremote). This default instance exports all Solr metrics from all
  registries as hierarchical MBeans. This behavior can be also disabled by specifying a SolrJmxReporter
  configuration with a boolean init arg "enabled" set to "false". For a more fine-grained control users
  should explicitly specify at least one SolrJmxReporter configuration.

 New Features
 ----------------------
 * SOLR-9857, SOLR-9858: Collect aggregated metrics from nodes and shard leaders in overseer. (ab)
@@ -96,6 +109,10 @@ Other Changes
 * SOLR-10347: Removed index level boost support from "documents" section of the admin UI (Amrit Sarkar via
   Tomás Fernández Löbbe)
 
* SOLR-9959: SolrInfoMBean category and hierarchy cleanup. Per-component statistics are now obtained from
  the metrics API, legacy JMX support has been replaced with SolrJmxReporter functionality. Several reporter
  improvements (support for multiple prefix filters, "enabled" flag, reuse of service clients). (ab)

 ----------------------
 
 ==================  6.6.0 ==================
diff --git a/solr/contrib/analytics/src/java/org/apache/solr/analytics/plugin/AnalyticsStatisticsCollector.java b/solr/contrib/analytics/src/java/org/apache/solr/analytics/plugin/AnalyticsStatisticsCollector.java
index b22dcb5bdf4..657768f4ad4 100644
-- a/solr/contrib/analytics/src/java/org/apache/solr/analytics/plugin/AnalyticsStatisticsCollector.java
++ b/solr/contrib/analytics/src/java/org/apache/solr/analytics/plugin/AnalyticsStatisticsCollector.java
@@ -16,11 +16,11 @@
  */
 package org.apache.solr.analytics.plugin;
 
import java.util.HashMap;
import java.util.Map;
 import java.util.concurrent.atomic.AtomicLong;
 
 import com.codahale.metrics.Timer;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.util.stats.MetricUtils;
 
 public class AnalyticsStatisticsCollector {
@@ -85,17 +85,20 @@ public class AnalyticsStatisticsCollector {
     currentTimer.stop();
   }
 
  public NamedList<Object> getStatistics() {
    NamedList<Object> lst = new SimpleOrderedMap<>();
    lst.add("requests", numRequests.longValue());
    lst.add("analyticsRequests", numAnalyticsRequests.longValue());
    lst.add("statsRequests", numStatsRequests.longValue());
    lst.add("statsCollected", numCollectedStats.longValue());
    lst.add("fieldFacets", numFieldFacets.longValue());
    lst.add("rangeFacets", numRangeFacets.longValue());
    lst.add("queryFacets", numQueryFacets.longValue());
    lst.add("queriesInQueryFacets", numQueries.longValue());
    MetricUtils.addMetrics(lst, requestTimes);
    return lst;
  public Map<String, Object> getStatistics() {

    Map<String, Object> map = new HashMap<>();
    MetricUtils.convertTimer("", requestTimes, false, false, (k, v) -> {
      map.putAll((Map<String, Object>)v);
    });
    map.put("requests", numRequests.longValue());
    map.put("analyticsRequests", numAnalyticsRequests.longValue());
    map.put("statsRequests", numStatsRequests.longValue());
    map.put("statsCollected", numCollectedStats.longValue());
    map.put("fieldFacets", numFieldFacets.longValue());
    map.put("rangeFacets", numRangeFacets.longValue());
    map.put("queryFacets", numQueryFacets.longValue());
    map.put("queriesInQueryFacets", numQueries.longValue());
    return map;
   }
 }
diff --git a/solr/contrib/analytics/src/java/org/apache/solr/handler/component/AnalyticsComponent.java b/solr/contrib/analytics/src/java/org/apache/solr/handler/component/AnalyticsComponent.java
index f33b6c7a6dc..505533b1cc5 100644
-- a/solr/contrib/analytics/src/java/org/apache/solr/handler/component/AnalyticsComponent.java
++ b/solr/contrib/analytics/src/java/org/apache/solr/handler/component/AnalyticsComponent.java
@@ -22,9 +22,11 @@ import org.apache.solr.analytics.plugin.AnalyticsStatisticsCollector;
 import org.apache.solr.analytics.request.AnalyticsStats;
 import org.apache.solr.analytics.util.AnalyticsParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 
public class AnalyticsComponent extends SearchComponent {
public class AnalyticsComponent extends SearchComponent implements SolrMetricProducer {
   public static final String COMPONENT_NAME = "analytics";
   private final AnalyticsStatisticsCollector analyticsCollector = new AnalyticsStatisticsCollector();;
 
@@ -80,12 +82,8 @@ public class AnalyticsComponent extends SearchComponent {
   }
 
   @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }

  @Override
  public NamedList getStatistics() {
    return analyticsCollector.getStatistics();
  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    MetricsMap metrics = new MetricsMap((detailed, map) -> map.putAll(analyticsCollector.getStatistics()));
    manager.registerGauge(this, registry, metrics, true, getClass().getSimpleName(), getCategory().toString(), scope);
   }
 }
diff --git a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
index 0766c7f838e..faea3baab18 100644
-- a/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
++ b/solr/contrib/dataimporthandler/src/java/org/apache/solr/handler/dataimport/DataImportHandler.java
@@ -26,12 +26,13 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.ContentStreamBase;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.ContentStream;
 import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.response.RawResponseWriter;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
@@ -74,6 +75,8 @@ public class DataImportHandler extends RequestHandlerBase implements
 
   private String myName = "dataimport";
 
  private MetricsMap metrics;

   private static final String PARAM_WRITER_IMPL = "writerImpl";
   private static final String DEFAULT_WRITER_NAME = "SolrWriter";
 
@@ -260,41 +263,33 @@ public class DataImportHandler extends RequestHandlerBase implements
       };
     }
   }
  
  @Override
  @SuppressWarnings("unchecked")
  public NamedList getStatistics() {
    if (importer == null)
      return super.getStatistics();
 
    DocBuilder.Statistics cumulative = importer.cumulativeStatistics;
    SimpleOrderedMap result = new SimpleOrderedMap();

    result.add("Status", importer.getStatus().toString());
  @Override
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    super.initializeMetrics(manager, registryName, scope);
    metrics = new MetricsMap((detailed, map) -> {
      if (importer != null) {
        DocBuilder.Statistics cumulative = importer.cumulativeStatistics;
 
    if (importer.docBuilder != null) {
      DocBuilder.Statistics running = importer.docBuilder.importStatistics;
      result.add("Documents Processed", running.docCount);
      result.add("Requests made to DataSource", running.queryCount);
      result.add("Rows Fetched", running.rowsCount);
      result.add("Documents Deleted", running.deletedDocCount);
      result.add("Documents Skipped", running.skipDocCount);
    }
        map.put("Status", importer.getStatus().toString());
 
    result.add(DataImporter.MSG.TOTAL_DOC_PROCESSED, cumulative.docCount);
    result.add(DataImporter.MSG.TOTAL_QUERIES_EXECUTED, cumulative.queryCount);
    result.add(DataImporter.MSG.TOTAL_ROWS_EXECUTED, cumulative.rowsCount);
    result.add(DataImporter.MSG.TOTAL_DOCS_DELETED, cumulative.deletedDocCount);
    result.add(DataImporter.MSG.TOTAL_DOCS_SKIPPED, cumulative.skipDocCount);
        if (importer.docBuilder != null) {
          DocBuilder.Statistics running = importer.docBuilder.importStatistics;
          map.put("Documents Processed", running.docCount);
          map.put("Requests made to DataSource", running.queryCount);
          map.put("Rows Fetched", running.rowsCount);
          map.put("Documents Deleted", running.deletedDocCount);
          map.put("Documents Skipped", running.skipDocCount);
        }
 
    NamedList requestStatistics = super.getStatistics();
    if (requestStatistics != null) {
      for (int i = 0; i < requestStatistics.size(); i++) {
        result.add(requestStatistics.getName(i), requestStatistics.getVal(i));
        map.put(DataImporter.MSG.TOTAL_DOC_PROCESSED, cumulative.docCount);
        map.put(DataImporter.MSG.TOTAL_QUERIES_EXECUTED, cumulative.queryCount);
        map.put(DataImporter.MSG.TOTAL_ROWS_EXECUTED, cumulative.rowsCount);
        map.put(DataImporter.MSG.TOTAL_DOCS_DELETED, cumulative.deletedDocCount);
        map.put(DataImporter.MSG.TOTAL_DOCS_SKIPPED, cumulative.skipDocCount);
       }
    }

    return result;
    });
    manager.registerGauge(this, registryName, metrics, true, "importer", getCategory().toString(), scope);
   }
 
   // //////////////////////SolrInfoMBeans methods //////////////////////
diff --git a/solr/core/src/java/org/apache/solr/core/CoreContainer.java b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
index 1ef036aa232..f1e28dd76c9 100644
-- a/solr/core/src/java/org/apache/solr/core/CoreContainer.java
++ b/solr/core/src/java/org/apache/solr/core/CoreContainer.java
@@ -81,6 +81,7 @@ import org.apache.solr.metrics.SolrCoreMetricManager;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.SolrFieldCacheBean;
 import org.apache.solr.security.AuthenticationPlugin;
 import org.apache.solr.security.AuthorizationPlugin;
 import org.apache.solr.security.HttpClientBuilderPlugin;
@@ -482,18 +483,18 @@ public class CoreContainer {
     metricManager = new SolrMetricManager();
 
     coreContainerWorkExecutor = MetricUtils.instrumentedExecutorService(
        coreContainerWorkExecutor,
        metricManager.registry(SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node)),
        SolrMetricManager.mkName("coreContainerWorkExecutor", SolrInfoMBean.Category.CONTAINER.toString(), "threadPool"));
        coreContainerWorkExecutor, null,
        metricManager.registry(SolrMetricManager.getRegistryName(SolrInfoBean.Group.node)),
        SolrMetricManager.mkName("coreContainerWorkExecutor", SolrInfoBean.Category.CONTAINER.toString(), "threadPool"));
 
     shardHandlerFactory = ShardHandlerFactory.newInstance(cfg.getShardHandlerFactoryPluginInfo(), loader);
     if (shardHandlerFactory instanceof SolrMetricProducer) {
       SolrMetricProducer metricProducer = (SolrMetricProducer) shardHandlerFactory;
      metricProducer.initializeMetrics(metricManager, SolrInfoMBean.Group.node.toString(), "httpShardHandler");
      metricProducer.initializeMetrics(metricManager, SolrInfoBean.Group.node.toString(), "httpShardHandler");
     }
 
     updateShardHandler = new UpdateShardHandler(cfg.getUpdateShardHandlerConfig());
    updateShardHandler.initializeMetrics(metricManager, SolrInfoMBean.Group.node.toString(), "updateShardHandler");
    updateShardHandler.initializeMetrics(metricManager, SolrInfoBean.Group.node.toString(), "updateShardHandler");
 
     transientCoreCache = TransientSolrCoreCacheFactory.newInstance(loader, this);
 
@@ -520,14 +521,14 @@ public class CoreContainer {
     // may want to add some configuration here in the future
     metricsCollectorHandler.init(null);
     containerHandlers.put(AUTHZ_PATH, securityConfHandler);
    securityConfHandler.initializeMetrics(metricManager, SolrInfoMBean.Group.node.toString(), AUTHZ_PATH);
    securityConfHandler.initializeMetrics(metricManager, SolrInfoBean.Group.node.toString(), AUTHZ_PATH);
     containerHandlers.put(AUTHC_PATH, securityConfHandler);
     if(pkiAuthenticationPlugin != null)
       containerHandlers.put(PKIAuthenticationPlugin.PATH, pkiAuthenticationPlugin.getRequestHandler());
 
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoMBean.Group.node);
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoMBean.Group.jvm);
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoMBean.Group.jetty);
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoBean.Group.node);
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoBean.Group.jvm);
    metricManager.loadReporters(cfg.getMetricReporterPlugins(), loader, null, SolrInfoBean.Group.jetty);
 
     coreConfigService = ConfigSetService.createConfigSetService(cfg, loader, zkSys.zkController);
 
@@ -535,17 +536,25 @@ public class CoreContainer {
 
     // initialize gauges for reporting the number of cores and disk total/free
 
    String registryName = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node);
    metricManager.registerGauge(registryName, () -> solrCores.getCores().size(),
        true, "loaded", SolrInfoMBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(registryName, () -> solrCores.getLoadedCoreNames().size() - solrCores.getCores().size(),
        true, "lazy",SolrInfoMBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(registryName, () -> solrCores.getAllCoreNames().size() - solrCores.getLoadedCoreNames().size(),
        true, "unloaded",SolrInfoMBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(registryName, () -> cfg.getCoreRootDirectory().toFile().getTotalSpace(),
        true, "totalSpace", SolrInfoMBean.Category.CONTAINER.toString(), "fs");
    metricManager.registerGauge(registryName, () -> cfg.getCoreRootDirectory().toFile().getUsableSpace(),
        true, "usableSpace", SolrInfoMBean.Category.CONTAINER.toString(), "fs");
    String registryName = SolrMetricManager.getRegistryName(SolrInfoBean.Group.node);
    metricManager.registerGauge(null, registryName, () -> solrCores.getCores().size(),
        true, "loaded", SolrInfoBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(null, registryName, () -> solrCores.getLoadedCoreNames().size() - solrCores.getCores().size(),
        true, "lazy", SolrInfoBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(null, registryName, () -> solrCores.getAllCoreNames().size() - solrCores.getLoadedCoreNames().size(),
        true, "unloaded", SolrInfoBean.Category.CONTAINER.toString(), "cores");
    metricManager.registerGauge(null, registryName, () -> cfg.getCoreRootDirectory().toFile().getTotalSpace(),
        true, "totalSpace", SolrInfoBean.Category.CONTAINER.toString(), "fs");
    metricManager.registerGauge(null, registryName, () -> cfg.getCoreRootDirectory().toFile().getUsableSpace(),
        true, "usableSpace", SolrInfoBean.Category.CONTAINER.toString(), "fs");
    // add version information
    metricManager.registerGauge(null, registryName, () -> this.getClass().getPackage().getSpecificationVersion(),
        true, "specification", SolrInfoBean.Category.CONTAINER.toString(), "version");
    metricManager.registerGauge(null, registryName, () -> this.getClass().getPackage().getImplementationVersion(),
        true, "implementation", SolrInfoBean.Category.CONTAINER.toString(), "version");

    SolrFieldCacheBean fieldCacheBean = new SolrFieldCacheBean();
    fieldCacheBean.initializeMetrics(metricManager, registryName, null);
 
     if (isZooKeeperAware()) {
       metricManager.loadClusterReporters(cfg.getMetricReporterPlugins(), this);
@@ -555,9 +564,9 @@ public class CoreContainer {
     ExecutorService coreLoadExecutor = MetricUtils.instrumentedExecutorService(
         ExecutorUtil.newMDCAwareFixedThreadPool(
             cfg.getCoreLoadThreadCount(isZooKeeperAware()),
            new DefaultSolrThreadFactory("coreLoadExecutor")),
        metricManager.registry(SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node)),
        SolrMetricManager.mkName("coreLoadExecutor",SolrInfoMBean.Category.CONTAINER.toString(), "threadPool"));
            new DefaultSolrThreadFactory("coreLoadExecutor")), null,
        metricManager.registry(SolrMetricManager.getRegistryName(SolrInfoBean.Group.node)),
        SolrMetricManager.mkName("coreLoadExecutor", SolrInfoBean.Category.CONTAINER.toString(), "threadPool"));
     final List<Future<SolrCore>> futures = new ArrayList<>();
     try {
       List<CoreDescriptor> cds = coresLocator.discover(this);
@@ -685,14 +694,16 @@ public class CoreContainer {
 
     ExecutorUtil.shutdownAndAwaitTermination(coreContainerWorkExecutor);
     if (metricManager != null) {
      metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node));
      metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoBean.Group.node));
      metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoBean.Group.jvm));
      metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoBean.Group.jetty));
     }
 
     if (isZooKeeperAware()) {
       cancelCoreRecoveries();
       zkSys.zkController.publishNodeAsDown(zkSys.zkController.getNodeName());
       if (metricManager != null) {
        metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoMBean.Group.cluster));
        metricManager.closeReporters(SolrMetricManager.getRegistryName(SolrInfoBean.Group.cluster));
       }
     }
 
@@ -1058,7 +1069,7 @@ public class CoreContainer {
   private void resetIndexDirectory(CoreDescriptor dcore, ConfigSet coreConfig) {
     SolrConfig config = coreConfig.getSolrConfig();
 
    String registryName = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, dcore.getName());
    String registryName = SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, dcore.getName());
     DirectoryFactory df = DirectoryFactory.loadDirectoryFactory(config, this, registryName);
     String dataDir = SolrCore.findDataDir(df, null, config, dcore);
 
@@ -1376,7 +1387,7 @@ public class CoreContainer {
       containerHandlers.put(path, (SolrRequestHandler)handler);
     }
     if (handler instanceof SolrMetricProducer) {
      ((SolrMetricProducer)handler).initializeMetrics(metricManager, SolrInfoMBean.Group.node.toString(), path);
      ((SolrMetricProducer)handler).initializeMetrics(metricManager, SolrInfoBean.Group.node.toString(), path);
     }
     return handler;
   }
diff --git a/solr/core/src/java/org/apache/solr/core/DirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/DirectoryFactory.java
index cc24e6c6ab3..20824ab29c5 100644
-- a/solr/core/src/java/org/apache/solr/core/DirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/DirectoryFactory.java
@@ -24,7 +24,6 @@ import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.nio.file.NoSuchFileException;
 import java.util.Arrays;
import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 
@@ -321,13 +320,6 @@ public abstract class DirectoryFactory implements NamedListInitializedPlugin,
     return cd.getInstanceDir().resolve(cd.getDataDir()).toAbsolutePath().toString();
   }
 
  /**
   * Optionally allow the DirectoryFactory to request registration of some MBeans.
   */
  public Collection<SolrInfoMBean> offerMBeans() {
    return Collections.emptySet();
  }

   public void cleanupOldIndexDirectories(final String dataDirPath, final String currentIndexDirPath, boolean afterCoreReload) {
     File dataDir = new File(dataDirPath);
     if (!dataDir.isDirectory()) {
diff --git a/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
index db953d38b1e..754fed3be3d 100644
-- a/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/HdfsDirectoryFactory.java
@@ -22,7 +22,6 @@ import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.net.URLEncoder;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
@@ -51,6 +50,8 @@ import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.store.blockcache.BlockCache;
 import org.apache.solr.store.blockcache.BlockDirectory;
 import org.apache.solr.store.blockcache.BlockDirectoryCache;
@@ -70,7 +71,7 @@ import com.google.common.cache.CacheBuilder;
 import com.google.common.cache.RemovalListener;
 import com.google.common.cache.RemovalNotification;
 
public class HdfsDirectoryFactory extends CachingDirectoryFactory implements SolrCoreAware {
public class HdfsDirectoryFactory extends CachingDirectoryFactory implements SolrCoreAware, SolrMetricProducer {
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   
   public static final String BLOCKCACHE_SLAB_COUNT = "solr.hdfs.blockcache.slab.count";
@@ -493,8 +494,9 @@ public class HdfsDirectoryFactory extends CachingDirectoryFactory implements Sol
   }
 
   @Override
  public Collection<SolrInfoMBean> offerMBeans() {
    return Arrays.<SolrInfoMBean>asList(MetricsHolder.metrics, LocalityHolder.reporter);
  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    MetricsHolder.metrics.initializeMetrics(manager, registry, scope);
    LocalityHolder.reporter.initializeMetrics(manager, registry, scope);
   }
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/core/JmxMonitoredMap.java b/solr/core/src/java/org/apache/solr/core/JmxMonitoredMap.java
deleted file mode 100644
index 4fb0dcd4d63..00000000000
-- a/solr/core/src/java/org/apache/solr/core/JmxMonitoredMap.java
++ /dev/null
@@ -1,478 +0,0 @@
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
package org.apache.solr.core;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrConfig.JmxConfiguration;
import org.apache.solr.metrics.reporters.JmxObjectNameFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.solr.common.params.CommonParams.ID;
import static org.apache.solr.common.params.CommonParams.NAME;

/**
 * <p>
 * Responsible for finding (or creating) a MBeanServer from given configuration
 * and registering all SolrInfoMBean objects with JMX.
 * </p>
 * <p>
 * Please see http://wiki.apache.org/solr/SolrJmx for instructions on usage and configuration
 * </p>
 *
 *
 * @see org.apache.solr.core.SolrConfig.JmxConfiguration
 * @since solr 1.3
 */
public class JmxMonitoredMap<K, V> extends
        ConcurrentHashMap<String, SolrInfoMBean> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String REPORTER_NAME = "_jmx_";

  // set to true to use cached statistics NamedLists between getMBeanInfo calls to work
  // around over calling getStatistics on MBeanInfos when iterating over all attributes (SOLR-6586)
  private final boolean useCachedStatsBetweenGetMBeanInfoCalls = Boolean.getBoolean("useCachedStatsBetweenGetMBeanInfoCalls");
  
  private final MBeanServer server;

  private final String jmxRootName;

  private final String coreHashCode;

  private final JmxObjectNameFactory nameFactory;

  private final String registryName;

  public JmxMonitoredMap(String coreName, String coreHashCode, String registryName,
                         final JmxConfiguration jmxConfig) {
    this.coreHashCode = coreHashCode;
    this.registryName = registryName;
    jmxRootName = (null != jmxConfig.rootName ?
                   jmxConfig.rootName
                   : ("solr" + (null != coreName ? "/" + coreName : "")));
      
    if (jmxConfig.serviceUrl == null) {
      List<MBeanServer> servers = null;

      if (jmxConfig.agentId == null) {
        // Try to find the first MBeanServer
        servers = MBeanServerFactory.findMBeanServer(null);
      } else if (jmxConfig.agentId != null) {
        // Try to find the first MBean server with the given agentId
        servers = MBeanServerFactory.findMBeanServer(jmxConfig.agentId);
        // throw Exception if no servers were found with the given agentId
        if (servers == null || servers.isEmpty())
          throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                  "No JMX Servers found with agentId: " + jmxConfig.agentId);
      }

      if (servers == null || servers.isEmpty()) {
        server = null;
        nameFactory = null;
        log.debug("No JMX servers found, not exposing Solr information with JMX.");
        return;
      }
      server = servers.get(0);
      log.info("JMX monitoring is enabled. Adding Solr mbeans to JMX Server: "
               + server);
    } else {
      MBeanServer newServer = null;
      try {
        // Create a new MBeanServer with the given serviceUrl
        newServer = MBeanServerFactory.newMBeanServer();
        JMXConnectorServer connector = JMXConnectorServerFactory
                .newJMXConnectorServer(new JMXServiceURL(jmxConfig.serviceUrl),
                        null, newServer);
        connector.start();
        log.info("JMX monitoring is enabled at " + jmxConfig.serviceUrl);
      } catch (Exception e) {
        // Release the reference
        throw new RuntimeException("Could not start JMX monitoring ", e);
      }
      server = newServer;
    }
    nameFactory = new JmxObjectNameFactory(REPORTER_NAME + coreHashCode, registryName);
  }

  /**
   * Clears the map and unregisters all SolrInfoMBeans in the map from
   * MBeanServer
   */
  @Override
  public void clear() {
    if (server != null) {
      QueryExp exp = Query.or(Query.eq(Query.attr("coreHashCode"), Query.value(coreHashCode)),
                            Query.eq(Query.attr("reporter"), Query.value(REPORTER_NAME + coreHashCode)));
      
      Set<ObjectName> objectNames = null;
      try {
        objectNames = server.queryNames(null, exp);
      } catch (Exception e) {
        log.warn("Exception querying for mbeans", e);
      }
      
      if (objectNames != null)  {
        for (ObjectName name : objectNames) {
          try {
            server.unregisterMBean(name);
          } catch (InstanceNotFoundException ie) {
            // ignore - someone else already deleted this one
          } catch (Exception e) {
            log.warn("Exception un-registering mbean {}", name, e);
          }
        }
      }
    }

    super.clear();
  }

  /**
   * Adds the SolrInfoMBean to the map and registers the given SolrInfoMBean
   * instance with the MBeanServer defined for this core. If a SolrInfoMBean is
   * already registered with the MBeanServer then it is unregistered and then
   * re-registered.
   *
   * @param key      the JMX type name for this SolrInfoMBean
   * @param infoBean the SolrInfoMBean instance to be registered
   */
  @Override
  public SolrInfoMBean put(String key, SolrInfoMBean infoBean) {
    if (server != null && infoBean != null) {
      try {
        // back-compat name
        ObjectName name = getObjectName(key, infoBean);
        if (server.isRegistered(name))
          server.unregisterMBean(name);
        SolrDynamicMBean mbean = new SolrDynamicMBean(coreHashCode, infoBean, useCachedStatsBetweenGetMBeanInfoCalls);
        server.registerMBean(mbean, name);
        // now register it also under new name
        String beanName = createBeanName(infoBean, key);
        name = nameFactory.createName(null, registryName, beanName);
        if (server.isRegistered(name))
          server.unregisterMBean(name);
        server.registerMBean(mbean, name);
      } catch (Exception e) {
        log.warn( "Failed to register info bean: key=" + key + ", infoBean=" + infoBean, e);
      }
    }

    return super.put(key, infoBean);
  }

  private String createBeanName(SolrInfoMBean infoBean, String key) {
    if (infoBean.getCategory() == null) {
      throw new IllegalArgumentException("SolrInfoMBean.category must never be null: " + infoBean);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(infoBean.getCategory().toString());
    sb.append('.');
    sb.append(key);
    sb.append('.');
    sb.append(infoBean.getName());
    return sb.toString();
  }

  /**
   * Removes the SolrInfoMBean object at the given key and unregisters it from
   * MBeanServer
   *
   * @param key the JMX type name for this SolrInfoMBean
   */
  @Override
  public SolrInfoMBean remove(Object key) {
    SolrInfoMBean infoBean = get(key);
    if (infoBean != null) {
      try {
        unregister((String) key, infoBean);
      } catch (RuntimeException e) {
        log.warn( "Failed to unregister info bean: " + key, e);
      }
    }
    return super.remove(key);
  }

  private void unregister(String key, SolrInfoMBean infoBean) {
    if (server == null)
      return;

    try {
      // remove legacy name
      ObjectName name = getObjectName(key, infoBean);
      if (server.isRegistered(name) && coreHashCode.equals(server.getAttribute(name, "coreHashCode"))) {
        server.unregisterMBean(name);
      }
      // remove new name
      String beanName = createBeanName(infoBean, key);
      name = nameFactory.createName(null, registryName, beanName);
      if (server.isRegistered(name)) {
        server.unregisterMBean(name);
      }
    } catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
              "Failed to unregister info bean: " + key, e);
    }
  }

  private ObjectName getObjectName(String key, SolrInfoMBean infoBean)
          throws MalformedObjectNameException {
    Hashtable<String, String> map = new Hashtable<>();
    map.put("type", key);
    if (infoBean.getName() != null && !"".equals(infoBean.getName())) {
      map.put(ID, infoBean.getName());
    }
    return ObjectName.getInstance(jmxRootName, map);
  }

  /** For test verification */
  public MBeanServer getServer() {
    return server;
  }

  /**
   * DynamicMBean is used to dynamically expose all SolrInfoMBean
   * getStatistics() NameList keys as String getters.
   */
  static class SolrDynamicMBean implements DynamicMBean {
    private SolrInfoMBean infoBean;

    private HashSet<String> staticStats;

    private String coreHashCode;
    
    private volatile NamedList cachedDynamicStats;
    
    private boolean useCachedStatsBetweenGetMBeanInfoCalls;
    
    public SolrDynamicMBean(String coreHashCode, SolrInfoMBean managedResource) {
      this(coreHashCode, managedResource, false);
    }

    public SolrDynamicMBean(String coreHashCode, SolrInfoMBean managedResource, boolean useCachedStatsBetweenGetMBeanInfoCalls) {
      this.useCachedStatsBetweenGetMBeanInfoCalls = useCachedStatsBetweenGetMBeanInfoCalls;
      if (managedResource instanceof JmxAugmentedSolrInfoMBean) {
        final JmxAugmentedSolrInfoMBean jmxSpecific = (JmxAugmentedSolrInfoMBean)managedResource;
        this.infoBean = new SolrInfoMBeanWrapper(jmxSpecific) {
          @Override
          public NamedList getStatistics() { return jmxSpecific.getStatisticsForJmx(); }
        };
      } else {
        this.infoBean = managedResource;
      }
      staticStats = new HashSet<>();

      // For which getters are already available in SolrInfoMBean
      staticStats.add(NAME);
      staticStats.add("version");
      staticStats.add("description");
      staticStats.add("category");
      staticStats.add("source");
      this.coreHashCode = coreHashCode;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
      ArrayList<MBeanAttributeInfo> attrInfoList = new ArrayList<>();

      for (String stat : staticStats) {
        attrInfoList.add(new MBeanAttributeInfo(stat, String.class.getName(),
                null, true, false, false));
      }

      // add core's hashcode
      attrInfoList.add(new MBeanAttributeInfo("coreHashCode", String.class.getName(),
                null, true, false, false));

      try {
        NamedList dynamicStats = infoBean.getStatistics();
        
        if (useCachedStatsBetweenGetMBeanInfoCalls) {
          cachedDynamicStats = dynamicStats;
        }
        
        if (dynamicStats != null) {
          for (int i = 0; i < dynamicStats.size(); i++) {
            String name = dynamicStats.getName(i);
            if (staticStats.contains(name)) {
              continue;
            }
            Class type = dynamicStats.get(name).getClass();
            OpenType typeBox = determineType(type);
            if (type.equals(String.class) || typeBox == null) {
              attrInfoList.add(new MBeanAttributeInfo(dynamicStats.getName(i),
                  String.class.getName(), null, true, false, false));
            } else {
              attrInfoList.add(new OpenMBeanAttributeInfoSupport(
                  dynamicStats.getName(i), dynamicStats.getName(i), typeBox,
                  true, false, false));
            }
          }
        }
      } catch (Exception e) {
        // don't log issue if the core is closing
        if (!(SolrException.getRootCause(e) instanceof AlreadyClosedException))
          log.warn("Could not getStatistics on info bean {}", infoBean.getName(), e);
      }

      MBeanAttributeInfo[] attrInfoArr = attrInfoList
              .toArray(new MBeanAttributeInfo[attrInfoList.size()]);
      return new MBeanInfo(getClass().getName(), infoBean
              .getDescription(), attrInfoArr, null, null, null);
    }

    private OpenType determineType(Class type) {
      try {
        for (Field field : SimpleType.class.getFields()) {
          if (field.getType().equals(SimpleType.class)) {
            SimpleType candidate = (SimpleType) field.get(SimpleType.class);
            if (candidate.getTypeName().equals(type.getName())) {
              return candidate;
            }
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    }

    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
      Object val;
      if ("coreHashCode".equals(attribute)) {
        val = coreHashCode;
      } else if (staticStats.contains(attribute) && attribute != null
              && attribute.length() > 0) {
        try {
          String getter = "get" + attribute.substring(0, 1).toUpperCase(Locale.ROOT)
                  + attribute.substring(1);
          Method meth = infoBean.getClass().getMethod(getter);
          val = meth.invoke(infoBean);
        } catch (Exception e) {
          throw new AttributeNotFoundException(attribute);
        }
      } else {
        NamedList stats = null;
        if (useCachedStatsBetweenGetMBeanInfoCalls) {
          NamedList cachedStats = this.cachedDynamicStats;
          if (cachedStats != null) {
            stats = cachedStats;
          }
        }
        if (stats == null) {
          stats = infoBean.getStatistics();
        }
        val = stats.get(attribute);
      }

      if (val != null) {
        // It's String or one of the simple types, just return it as JMX suggests direct support for such types
        for (String simpleTypeName : SimpleType.ALLOWED_CLASSNAMES_LIST) {
          if (val.getClass().getName().equals(simpleTypeName)) {
            return val;
          }
        }
        // It's an arbitrary object which could be something complex and odd, return its toString, assuming that is
        // a workable representation of the object
        return val.toString();
      }
      return null;
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
      AttributeList list = new AttributeList();
      for (String attribute : attributes) {
        try {
          list.add(new Attribute(attribute, getAttribute(attribute)));
        } catch (Exception e) {
          log.warn("Could not get attribute " + attribute);
        }
      }

      return list;
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
      throw new UnsupportedOperationException("Operation not Supported");
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
      throw new UnsupportedOperationException("Operation not Supported");
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
      throw new UnsupportedOperationException("Operation not Supported");
    }
  }

  /**
   * SolrInfoMBean that provides JMX-specific statistics.  Used, for example,
   * if generating full statistics is expensive; the expensive statistics can
   * be generated normally for use with the web ui, while an abbreviated version
   * are generated for period jmx use.
   */
  public interface JmxAugmentedSolrInfoMBean extends SolrInfoMBean {
    /**
     * JMX-specific statistics
     */
    public NamedList getStatisticsForJmx();
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/PluginBag.java b/solr/core/src/java/org/apache/solr/core/PluginBag.java
index 65978f33d53..b916ad26c3f 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginBag.java
++ b/solr/core/src/java/org/apache/solr/core/PluginBag.java
@@ -294,8 +294,8 @@ public class PluginBag<T> implements AutoCloseable {
 
   private void registerMBean(Object inst, SolrCore core, String pluginKey) {
     if (core == null) return;
    if (inst instanceof SolrInfoMBean) {
      SolrInfoMBean mBean = (SolrInfoMBean) inst;
    if (inst instanceof SolrInfoBean) {
      SolrInfoBean mBean = (SolrInfoBean) inst;
       String name = (inst instanceof SolrRequestHandler) ? pluginKey : mBean.getName();
       core.registerInfoBean(name, mBean);
     }
diff --git a/solr/core/src/java/org/apache/solr/core/SolrConfig.java b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
index a2444203772..4e7ab487704 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrConfig.java
++ b/solr/core/src/java/org/apache/solr/core/SolrConfig.java
@@ -276,18 +276,12 @@ public class SolrConfig extends Config implements MapSerializable {
     hashSetInverseLoadFactor = 1.0f / getFloat("//HashDocSet/@loadFactor", 0.75f);
     hashDocSetMaxSize = getInt("//HashDocSet/@maxSize", 3000);
 
    httpCachingConfig = new HttpCachingConfig(this);
    if (get("jmx", null) != null) {
      log.warn("solrconfig.xml: <jmx> is no longer supported, use solr.xml:/metrics/reporter section instead");
    }
 
    Node jmx = getNode("jmx", false);
    if (jmx != null) {
      jmxConfig = new JmxConfiguration(true,
          get("jmx/@agentId", null),
          get("jmx/@serviceUrl", null),
          get("jmx/@rootName", null));
    httpCachingConfig = new HttpCachingConfig(this);
 
    } else {
      jmxConfig = new JmxConfiguration(false, null, null, null);
    }
     maxWarmingSearchers = getInt("query/maxWarmingSearchers", 1);
     slowQueryThresholdMillis = getInt("query/slowQueryThresholdMillis", -1);
     for (SolrPluginInfo plugin : plugins) loadPluginInfo(plugin);
@@ -510,48 +504,12 @@ public class SolrConfig extends Config implements MapSerializable {
   protected String dataDir;
   public final int slowQueryThresholdMillis;  // threshold above which a query is considered slow
 
  //JMX configuration
  public final JmxConfiguration jmxConfig;

   private final HttpCachingConfig httpCachingConfig;
 
   public HttpCachingConfig getHttpCachingConfig() {
     return httpCachingConfig;
   }
 
  public static class JmxConfiguration implements MapSerializable {
    public boolean enabled = false;
    public String agentId;
    public String serviceUrl;
    public String rootName;

    public JmxConfiguration(boolean enabled,
                            String agentId,
                            String serviceUrl,
                            String rootName) {
      this.enabled = enabled;
      this.agentId = agentId;
      this.serviceUrl = serviceUrl;
      this.rootName = rootName;

      if (agentId != null && serviceUrl != null) {
        throw new SolrException
            (SolrException.ErrorCode.SERVER_ERROR,
                "Incorrect JMX Configuration in solrconfig.xml, " +
                    "both agentId and serviceUrl cannot be specified at the same time");
      }

    }

    @Override
    public Map<String, Object> toMap(Map<String, Object> map) {
      map.put("agentId", agentId);
      map.put("serviceUrl", serviceUrl);
      map.put("rootName", rootName);
      return map;
    }
  }

   public static class HttpCachingConfig implements MapSerializable {
 
     /**
@@ -858,7 +816,6 @@ public class SolrConfig extends Config implements MapSerializable {
     m.put("queryResultMaxDocsCached", queryResultMaxDocsCached);
     m.put("enableLazyFieldLoading", enableLazyFieldLoading);
     m.put("maxBooleanClauses", booleanQueryMaxClauseCount);
    if (jmxConfig != null) result.put("jmx", jmxConfig);
     for (SolrPluginInfo plugin : plugins) {
       List<PluginInfo> infos = getPluginInfos(plugin.clazz.getName());
       if (infos == null || infos.isEmpty()) continue;
@@ -884,7 +841,6 @@ public class SolrConfig extends Config implements MapSerializable {
 
 
     addCacheConfig(m, filterCacheConfig, queryResultCacheConfig, documentCacheConfig, fieldValueCacheConfig);
    if (jmxConfig != null) result.put("jmx", jmxConfig);
     m = new LinkedHashMap();
     result.put("requestDispatcher", m);
     m.put("handleSelect", handleSelect);
diff --git a/solr/core/src/java/org/apache/solr/core/SolrCore.java b/solr/core/src/java/org/apache/solr/core/SolrCore.java
index a6ba2dca8b4..f0994529923 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrCore.java
++ b/solr/core/src/java/org/apache/solr/core/SolrCore.java
@@ -27,7 +27,6 @@ import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.lang.invoke.MethodHandles;
 import java.lang.reflect.Constructor;
import java.net.URL;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.NoSuchFileException;
 import java.nio.file.Path;
@@ -58,6 +57,7 @@ import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.locks.ReentrantLock;
 
 import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
 import com.codahale.metrics.Timer;
 import com.google.common.collect.MapMaker;
 import org.apache.commons.io.FileUtils;
@@ -133,7 +133,7 @@ import org.apache.solr.schema.IndexSchemaFactory;
 import org.apache.solr.schema.ManagedIndexSchema;
 import org.apache.solr.schema.SimilarityFactory;
 import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SolrFieldCacheMBean;
import org.apache.solr.search.SolrFieldCacheBean;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.search.ValueSourceParser;
 import org.apache.solr.search.stats.LocalStatsCache;
@@ -171,7 +171,7 @@ import static org.apache.solr.common.params.CommonParams.PATH;
 /**
  *
  */
public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closeable {
public final class SolrCore implements SolrInfoBean, SolrMetricProducer, Closeable {
 
   public static final String version="1.0";
 
@@ -202,7 +202,7 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
   private final PluginBag<UpdateRequestProcessorFactory> updateProcessors = new PluginBag<>(UpdateRequestProcessorFactory.class, this, true);
   private final Map<String,UpdateRequestProcessorChain> updateProcessorChains;
   private final SolrCoreMetricManager coreMetricManager;
  private final Map<String, SolrInfoMBean> infoRegistry;
  private final Map<String, SolrInfoBean> infoRegistry = new ConcurrentHashMap<>();
   private final IndexDeletionPolicyWrapper solrDelPolicy;
   private final SolrSnapshotMetaDataManager snapshotMgr;
   private final DirectoryFactory directoryFactory;
@@ -222,6 +222,12 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
   private Counter newSearcherMaxReachedCounter;
   private Counter newSearcherOtherErrorsCounter;
 
  private Set<String> metricNames = new HashSet<>();

  public Set<String> getMetricNames() {
    return metricNames;
  }

   public Date getStartTimeStamp() { return startTime; }
 
   private final Map<IndexReader.CacheKey, IndexFingerprint> perSegmentFingerprintCache = new MapMaker().weakKeys().makeMap();
@@ -448,14 +454,14 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
   }
 
   /**
   * Returns a Map of name vs SolrInfoMBean objects. The returned map is an instance of
   * Returns a Map of name vs SolrInfoBean objects. The returned map is an instance of
    * a ConcurrentHashMap and therefore no synchronization is needed for putting, removing
    * or iterating over it.
    *
   * @return the Info Registry map which contains SolrInfoMBean objects keyed by name
   * @return the Info Registry map which contains SolrInfoBean objects keyed by name
    * @since solr 1.3
    */
  public Map<String, SolrInfoMBean> getInfoRegistry() {
  public Map<String, SolrInfoBean> getInfoRegistry() {
     return infoRegistry;
   }
 
@@ -905,9 +911,12 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
     // initialize searcher-related metrics
     initializeMetrics(metricManager, coreMetricManager.getRegistryName(), null);
 
    // Initialize JMX
    this.infoRegistry = initInfoRegistry(name, config);
    infoRegistry.put("fieldCache", new SolrFieldCacheMBean());
    SolrFieldCacheBean solrFieldCacheBean = new SolrFieldCacheBean();
    // this is registered at the CONTAINER level because it's not core-specific - for now we
    // also register it here for back-compat
    solrFieldCacheBean.initializeMetrics(metricManager, coreMetricManager.getRegistryName(), "core");
    infoRegistry.put("fieldCache", solrFieldCacheBean);

 
     initSchema(config, schema);
 
@@ -998,15 +1007,9 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
     // from the core.
     resourceLoader.inform(infoRegistry);
 
    // Allow the directory factory to register MBeans as well
    for (SolrInfoMBean bean : directoryFactory.offerMBeans()) {
      log.debug("Registering JMX bean [{}] from directory factory.", bean.getName());
      // Not worried about concurrency, so no reason to use putIfAbsent
      if (infoRegistry.containsKey(bean.getName())){
        log.debug("Ignoring JMX bean [{}] due to name conflict.", bean.getName());
      } else {
        infoRegistry.put(bean.getName(), bean);
      }
    // Allow the directory factory to report metrics
    if (directoryFactory instanceof SolrMetricProducer) {
      ((SolrMetricProducer)directoryFactory).initializeMetrics(metricManager, coreMetricManager.getRegistryName(), "directoryFactory");
     }
 
     // seed version buckets with max from index during core initialization ... requires a searcher!
@@ -1126,34 +1129,46 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    newSearcherCounter = manager.counter(registry, "new", Category.SEARCHER.toString());
    newSearcherTimer = manager.timer(registry, "time", Category.SEARCHER.toString(), "new");
    newSearcherWarmupTimer = manager.timer(registry, "warmup", Category.SEARCHER.toString(), "new");
    newSearcherMaxReachedCounter = manager.counter(registry, "maxReached", Category.SEARCHER.toString(), "new");
    newSearcherOtherErrorsCounter = manager.counter(registry, "errors", Category.SEARCHER.toString(), "new");

    manager.registerGauge(registry, () -> name == null ? "(null)" : name, true, "coreName", Category.CORE.toString());
    manager.registerGauge(registry, () -> startTime, true, "startTime", Category.CORE.toString());
    manager.registerGauge(registry, () -> getOpenCount(), true, "refCount", Category.CORE.toString());
    manager.registerGauge(registry, () -> resourceLoader.getInstancePath().toString(), true, "instanceDir", Category.CORE.toString());
    manager.registerGauge(registry, () -> getIndexDir(), true, "indexDir", Category.CORE.toString());
    manager.registerGauge(registry, () -> getIndexSize(), true, "sizeInBytes", Category.INDEX.toString());
    manager.registerGauge(registry, () -> NumberUtils.readableSize(getIndexSize()), true, "size", Category.INDEX.toString());
    manager.registerGauge(registry, () -> coreDescriptor.getCoreContainer().getCoreNames(this), true, "aliases", Category.CORE.toString());
    newSearcherCounter = manager.counter(this, registry, "new", Category.SEARCHER.toString());
    newSearcherTimer = manager.timer(this, registry, "time", Category.SEARCHER.toString(), "new");
    newSearcherWarmupTimer = manager.timer(this, registry, "warmup", Category.SEARCHER.toString(), "new");
    newSearcherMaxReachedCounter = manager.counter(this, registry, "maxReached", Category.SEARCHER.toString(), "new");
    newSearcherOtherErrorsCounter = manager.counter(this, registry, "errors", Category.SEARCHER.toString(), "new");

    manager.registerGauge(this, registry, () -> name == null ? "(null)" : name, true, "coreName", Category.CORE.toString());
    manager.registerGauge(this, registry, () -> startTime, true, "startTime", Category.CORE.toString());
    manager.registerGauge(this, registry, () -> getOpenCount(), true, "refCount", Category.CORE.toString());
    manager.registerGauge(this, registry, () -> resourceLoader.getInstancePath().toString(), true, "instanceDir", Category.CORE.toString());
    manager.registerGauge(this, registry, () -> getIndexDir(), true, "indexDir", Category.CORE.toString());
    manager.registerGauge(this, registry, () -> getIndexSize(), true, "sizeInBytes", Category.INDEX.toString());
    manager.registerGauge(this, registry, () -> NumberUtils.readableSize(getIndexSize()), true, "size", Category.INDEX.toString());
    if (coreDescriptor != null && coreDescriptor.getCoreContainer() != null) {
      manager.registerGauge(this, registry, () -> coreDescriptor.getCoreContainer().getCoreNames(this), true, "aliases", Category.CORE.toString());
      final CloudDescriptor cd = coreDescriptor.getCloudDescriptor();
      if (cd != null) {
        manager.registerGauge(this, registry, () -> {
          if (cd.getCollectionName() != null) {
            return cd.getCollectionName();
          } else {
            return "_notset_";
          }
        }, true, "collection", Category.CORE.toString());

        manager.registerGauge(this, registry, () -> {
          if (cd.getShardId() != null) {
            return cd.getShardId();
          } else {
            return "_auto_";
          }
        }, true, "shard", Category.CORE.toString());
      }
    }

     // initialize disk total / free metrics
     Path dataDirPath = Paths.get(dataDir);
     File dataDirFile = dataDirPath.toFile();
    manager.registerGauge(registry, () -> dataDirFile.getTotalSpace(), true, "totalSpace", Category.CORE.toString(), "fs");
    manager.registerGauge(registry, () -> dataDirFile.getUsableSpace(), true, "usableSpace", Category.CORE.toString(), "fs");
  }

  private Map<String,SolrInfoMBean> initInfoRegistry(String name, SolrConfig config) {
    if (config.jmxConfig.enabled) {
      return new JmxMonitoredMap<String, SolrInfoMBean>(name, coreMetricManager.getRegistryName(), String.valueOf(this.hashCode()), config.jmxConfig);
    } else  {
      log.debug("JMX monitoring not detected for core: " + name);
      return new ConcurrentHashMap<>();
    }
    manager.registerGauge(this, registry, () -> dataDirFile.getTotalSpace(), true, "totalSpace", Category.CORE.toString(), "fs");
    manager.registerGauge(this, registry, () -> dataDirFile.getUsableSpace(), true, "usableSpace", Category.CORE.toString(), "fs");
   }
 
   private void checkVersionFieldExistsInSchema(IndexSchema schema, CoreDescriptor coreDescriptor) {
@@ -2685,6 +2700,9 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
     for (PluginInfo info : pluginInfos) {
       T o = createInitInstance(info,type, type.getSimpleName(), defClassName);
       registry.put(info.name, o);
      if (o instanceof SolrMetricProducer) {
        coreMetricManager.registerMetricProducer(type.getSimpleName() + "." + info.name, (SolrMetricProducer)o);
      }
       if(info.isDefault()){
         def = o;
       }
@@ -2692,6 +2710,12 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
     return def;
   }
 
  public void initDefaultPlugin(Object plugin, Class type) {
    if (plugin instanceof SolrMetricProducer) {
      coreMetricManager.registerMetricProducer(type.getSimpleName() + ".default", (SolrMetricProducer)plugin);
    }
  }

   /**For a given List of PluginInfo return the instances as a List
    * @param defClassName The default classname if PluginInfo#className == null
    * @return The instances initialized
@@ -2775,14 +2799,9 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
   }
 
   /////////////////////////////////////////////////////////////////////
  // SolrInfoMBean stuff: Statistics and Module Info
  // SolrInfoBean stuff: Statistics and Module Info
   /////////////////////////////////////////////////////////////////////
 
  @Override
  public String getVersion() {
    return SolrCore.version;
  }

   @Override
   public String getDescription() {
     return "SolrCore";
@@ -2794,48 +2813,8 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
   }
 
   @Override
  public String getSource() {
    return null;
  }

  @Override
  public URL[] getDocs() {
    return null;
  }

  @Override
  public NamedList getStatistics() {
    NamedList<Object> lst = new SimpleOrderedMap<>(8);
    lst.add("coreName", name==null ? "(null)" : name);
    lst.add("startTime", startTime);
    lst.add("refCount", getOpenCount());
    lst.add("instanceDir", resourceLoader.getInstancePath());
    lst.add("indexDir", getIndexDir());
    long size = getIndexSize();
    lst.add("sizeInBytes", size);
    lst.add("size", NumberUtils.readableSize(size));

    CoreDescriptor cd = getCoreDescriptor();
    if (cd != null) {
      if (null != cd && cd.getCoreContainer() != null) {
        lst.add("aliases", getCoreDescriptor().getCoreContainer().getCoreNames(this));
      }
      CloudDescriptor cloudDesc = cd.getCloudDescriptor();
      if (cloudDesc != null) {
        String collection = cloudDesc.getCollectionName();
        if (collection == null) {
          collection = "_notset_";
        }
        lst.add("collection", collection);
        String shard = cloudDesc.getShardId();
        if (shard == null) {
          shard = "_auto_";
        }
        lst.add("shard", shard);
      }
    }

    return lst;
  public MetricRegistry getMetricRegistry() {
    return coreMetricManager.getRegistry();
   }
 
   public Codec getCodec() {
@@ -2983,11 +2962,11 @@ public final class SolrCore implements SolrInfoMBean, SolrMetricProducer, Closea
     };
   }
 
  public void registerInfoBean(String name, SolrInfoMBean solrInfoMBean) {
    infoRegistry.put(name, solrInfoMBean);
  public void registerInfoBean(String name, SolrInfoBean solrInfoBean) {
    infoRegistry.put(name, solrInfoBean);
 
    if (solrInfoMBean instanceof SolrMetricProducer) {
      SolrMetricProducer producer = (SolrMetricProducer) solrInfoMBean;
    if (solrInfoBean instanceof SolrMetricProducer) {
      SolrMetricProducer producer = (SolrMetricProducer) solrInfoBean;
       coreMetricManager.registerMetricProducer(name, producer);
     }
   }
diff --git a/solr/core/src/java/org/apache/solr/core/SolrInfoBean.java b/solr/core/src/java/org/apache/solr/core/SolrInfoBean.java
new file mode 100644
index 00000000000..472b15e0819
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/core/SolrInfoBean.java
@@ -0,0 +1,95 @@
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
package org.apache.solr.core;

import java.util.Map;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.util.stats.MetricUtils;

/**
 * Interface for getting various ui friendly strings
 * for use by objects which are 'pluggable' to make server administration
 * easier.
 */
public interface SolrInfoBean {

  /**
   * Category of Solr component.
   */
  enum Category { CONTAINER, ADMIN, CORE, QUERY, UPDATE, CACHE, HIGHLIGHTER, QUERYPARSER, SPELLCHECKER,
    SEARCHER, REPLICATION, TLOG, INDEX, DIRECTORY, HTTP, OTHER }

  /**
   * Top-level group of beans or metrics for a subsystem.
   */
  enum Group { jvm, jetty, node, core, collection, shard, cluster, overseer }

  /**
   * Simple common usage name, e.g. BasicQueryHandler,
   * or fully qualified class name.
   */
  String getName();
  /** Simple one or two line description */
  String getDescription();
  /** Category of this component */
  Category getCategory();

  /** Optionally return a snapshot of metrics that this component reports, or null.
   * Default implementation requires that both {@link #getMetricNames()} and
   * {@link #getMetricRegistry()} return non-null values.
   */
  default Map<String, Object> getMetricsSnapshot() {
    if (getMetricRegistry() == null || getMetricNames() == null) {
      return null;
    }
    return MetricUtils.convertMetrics(getMetricRegistry(), getMetricNames());
  }

  /**
   * Modifiable set of metric names that this component reports (default is null,
   * which means none). If not null then this set is used by {@link #registerMetricName(String)}
   * to capture what metrics names are reported from this component.
   */
  default Set<String> getMetricNames() {
    return null;
  }

  /**
   * An instance of {@link MetricRegistry} that this component uses for metrics reporting
   * (default is null, which means no registry).
   */
  default MetricRegistry getMetricRegistry() {
    return null;
  }

  /** Register a metric name that this component reports. This method is called by various
   * metric registration methods in {@link org.apache.solr.metrics.SolrMetricManager} in order
   * to capture what metric names are reported from this component (which in turn is called
   * from {@link org.apache.solr.metrics.SolrMetricProducer#initializeMetrics(SolrMetricManager, String, String)}).
   * <p>Default implementation registers all metrics added by a component. Implementations may
   * override this to avoid reporting some or all metrics returned by {@link #getMetricsSnapshot()}</p>
   */
  default void registerMetricName(String name) {
    Set<String> names = getMetricNames();
    if (names != null) {
      names.add(name);
    }
  }
}
diff --git a/solr/core/src/java/org/apache/solr/core/SolrInfoMBean.java b/solr/core/src/java/org/apache/solr/core/SolrInfoMBean.java
deleted file mode 100644
index 63bdef0f7bc..00000000000
-- a/solr/core/src/java/org/apache/solr/core/SolrInfoMBean.java
++ /dev/null
@@ -1,76 +0,0 @@
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
package org.apache.solr.core;

import java.net.URL;

import org.apache.solr.common.util.NamedList;

/**
 * MBean interface for getting various ui friendly strings and URLs
 * for use by objects which are 'pluggable' to make server administration
 * easier.
 *
 *
 */
public interface SolrInfoMBean {

  /**
   * Category of Solr component.
   */
  enum Category { CONTAINER, ADMIN, CORE, QUERY, UPDATE, CACHE, HIGHLIGHTER, QUERYPARSER, SPELLCHECKER,
    SEARCHER, REPLICATION, TLOG, INDEX, DIRECTORY, HTTP, OTHER }

  /**
   * Top-level group of beans or metrics for a subsystem.
   */
  enum Group { jvm, jetty, node, core, collection, shard, cluster, overseer }

  /**
   * Simple common usage name, e.g. BasicQueryHandler,
   * or fully qualified clas name.
   */
  public String getName();
  /** Simple common usage version, e.g. 2.0 */
  public String getVersion();
  /** Simple one or two line description */
  public String getDescription();
  /** Purpose of this Class */
  public Category getCategory();
  /** CVS Source, SVN Source, etc */
  public String getSource();
  /**
   * Documentation URL list.
   *
   * <p>
   * Suggested documentation URLs: Homepage for sponsoring project,
   * FAQ on class usage, Design doc for class, Wiki, bug reporting URL, etc...
   * </p>
   */
  public URL[] getDocs();
  /**
   * Any statistics this instance would like to be publicly available via
   * the Solr Administration interface.
   *
   * <p>
   * Any Object type may be stored in the list, but only the
   * <code>toString()</code> representation will be used.
   * </p>
   */
  public NamedList getStatistics();

}
diff --git a/solr/core/src/java/org/apache/solr/core/SolrInfoMBeanWrapper.java b/solr/core/src/java/org/apache/solr/core/SolrInfoMBeanWrapper.java
deleted file mode 100644
index 534b8844931..00000000000
-- a/solr/core/src/java/org/apache/solr/core/SolrInfoMBeanWrapper.java
++ /dev/null
@@ -1,62 +0,0 @@
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

package org.apache.solr.core;

import java.net.URL;

import org.apache.solr.common.util.NamedList;

/**
 * Wraps a {@link SolrInfoMBean}.
 */
public class SolrInfoMBeanWrapper implements SolrInfoMBean {
  private final SolrInfoMBean mbean;

  public SolrInfoMBeanWrapper(SolrInfoMBean mbean) {
    this.mbean = mbean;
  }

  /** {@inheritDoc} */
  @Override
  public String getName() { return mbean.getName(); }

  /** {@inheritDoc} */
  @Override
  public String getVersion() { return mbean.getVersion(); }

  /** {@inheritDoc} */
  @Override
  public String getDescription() { return mbean.getDescription(); }

  /** {@inheritDoc} */
  @Override
  public Category getCategory() { return mbean.getCategory(); }

  /** {@inheritDoc} */
  @Override
  public String getSource() { return mbean.getSource(); }

  /** {@inheritDoc} */
  @Override
  public URL[] getDocs() { return mbean.getDocs(); }

  /** {@inheritDoc} */
  @Override
  public NamedList getStatistics() { return mbean.getStatistics(); }

}
diff --git a/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java b/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
index d310ff23c71..2e679cf6d4d 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
++ b/solr/core/src/java/org/apache/solr/core/SolrResourceLoader.java
@@ -100,7 +100,7 @@ public class SolrResourceLoader implements ResourceLoader,Closeable
   private String dataDir;
   
   private final List<SolrCoreAware> waitingForCore = Collections.synchronizedList(new ArrayList<SolrCoreAware>());
  private final List<SolrInfoMBean> infoMBeans = Collections.synchronizedList(new ArrayList<SolrInfoMBean>());
  private final List<SolrInfoBean> infoMBeans = Collections.synchronizedList(new ArrayList<SolrInfoBean>());
   private final List<ResourceLoaderAware> waitingForResources = Collections.synchronizedList(new ArrayList<ResourceLoaderAware>());
   private static final Charset UTF_8 = StandardCharsets.UTF_8;
 
@@ -664,9 +664,9 @@ public class SolrResourceLoader implements ResourceLoader,Closeable
         assertAwareCompatibility( ResourceLoaderAware.class, obj );
         waitingForResources.add( (ResourceLoaderAware)obj );
       }
      if (obj instanceof SolrInfoMBean){
      if (obj instanceof SolrInfoBean){
         //TODO: Assert here?
        infoMBeans.add((SolrInfoMBean) obj);
        infoMBeans.add((SolrInfoBean) obj);
       }
     }
 
@@ -722,21 +722,21 @@ public class SolrResourceLoader implements ResourceLoader,Closeable
   }
 
   /**
   * Register any {@link org.apache.solr.core.SolrInfoMBean}s
   * Register any {@link SolrInfoBean}s
    * @param infoRegistry The Info Registry
    */
  public void inform(Map<String, SolrInfoMBean> infoRegistry) {
  public void inform(Map<String, SolrInfoBean> infoRegistry) {
     // this can currently happen concurrently with requests starting and lazy components
     // loading.  Make sure infoMBeans doesn't change.
 
    SolrInfoMBean[] arr;
    SolrInfoBean[] arr;
     synchronized (infoMBeans) {
      arr = infoMBeans.toArray(new SolrInfoMBean[infoMBeans.size()]);
      arr = infoMBeans.toArray(new SolrInfoBean[infoMBeans.size()]);
       waitingForResources.clear();
     }
 
 
    for (SolrInfoMBean bean : arr) {
    for (SolrInfoBean bean : arr) {
       // Too slow? I suspect not, but we may need
       // to start tracking this in a Set.
       if (!infoRegistry.containsValue(bean)) {
@@ -879,7 +879,7 @@ public class SolrResourceLoader implements ResourceLoader,Closeable
   public void close() throws IOException {
     IOUtils.close(classLoader);
   }
  public List<SolrInfoMBean> getInfoMBeans(){
  public List<SolrInfoBean> getInfoMBeans(){
     return Collections.unmodifiableList(infoMBeans);
   }
 
diff --git a/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java b/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
index b37bd521681..5f682868c45 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
++ b/solr/core/src/java/org/apache/solr/core/SolrXmlConfig.java
@@ -16,6 +16,7 @@
  */
 package org.apache.solr.core;
 
import javax.management.MBeanServer;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathConstants;
 import javax.xml.xpath.XPathExpressionException;
@@ -25,7 +26,10 @@ import java.lang.invoke.MethodHandles;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
 import java.util.HashSet;
import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Set;
@@ -35,8 +39,10 @@ import org.apache.commons.io.IOUtils;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.logging.LogWatcherConfig;
import org.apache.solr.metrics.reporters.SolrJmxReporter;
 import org.apache.solr.update.UpdateShardHandlerConfig;
 import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.JmxUtil;
 import org.apache.solr.util.PropertiesUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -448,14 +454,30 @@ public class SolrXmlConfig {
 
   private static PluginInfo[] getMetricReporterPluginInfos(Config config) {
     NodeList nodes = (NodeList) config.evaluate("solr/metrics/reporter", XPathConstants.NODESET);
    if (nodes == null || nodes.getLength() == 0)
      return new PluginInfo[0];
    PluginInfo[] configs = new PluginInfo[nodes.getLength()];
    for (int i = 0; i < nodes.getLength(); i++) {
      // we don't require class in order to support predefined replica and node reporter classes
      configs[i] = new PluginInfo(nodes.item(i), "SolrMetricReporter", true, false);
    List<PluginInfo> configs = new ArrayList<>();
    boolean hasJmxReporter = false;
    if (nodes != null && nodes.getLength() > 0) {
      for (int i = 0; i < nodes.getLength(); i++) {
        // we don't require class in order to support predefined replica and node reporter classes
        PluginInfo info = new PluginInfo(nodes.item(i), "SolrMetricReporter", true, false);
        String clazz = info.className;
        if (clazz != null && clazz.equals(SolrJmxReporter.class.getName())) {
          hasJmxReporter = true;
        }
        configs.add(info);
      }
     }
    return configs;
    // if there's an MBean server running but there was no JMX reporter then add a default one
    MBeanServer mBeanServer = JmxUtil.findFirstMBeanServer();
    if (mBeanServer != null && !hasJmxReporter) {
      log.info("MBean server found: " + mBeanServer + ", but no JMX reporters were configured - adding default JMX reporter.");
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("name", "default");
      attributes.put("class", SolrJmxReporter.class.getName());
      PluginInfo defaultPlugin = new PluginInfo("reporter", attributes);
      configs.add(defaultPlugin);
    }
    return configs.toArray(new PluginInfo[configs.size()]);
   }
   private static PluginInfo getTransientCoreCacheFactoryPluginInfo(Config config) {
     Node node = config.getNode("solr/transientCoreCacheFactory", false);
diff --git a/solr/core/src/java/org/apache/solr/handler/AnalyzeEvaluator.java b/solr/core/src/java/org/apache/solr/handler/AnalyzeEvaluator.java
index 485f9c39906..392930fd6d7 100644
-- a/solr/core/src/java/org/apache/solr/handler/AnalyzeEvaluator.java
++ b/solr/core/src/java/org/apache/solr/handler/AnalyzeEvaluator.java
@@ -14,9 +14,6 @@
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
/**
 *
 */
 package org.apache.solr.handler;
 
 import java.io.IOException;
diff --git a/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java b/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
index 9c86350d82a..50ea711e9d5 100644
-- a/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/MoreLikeThisHandler.java
@@ -19,8 +19,6 @@ package org.apache.solr.handler;
 import java.io.IOException;
 import java.io.Reader;
 import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
@@ -481,12 +479,4 @@ public class MoreLikeThisHandler extends RequestHandlerBase
   public String getDescription() {
     return "Solr MoreLikeThis";
   }

  @Override
  public URL[] getDocs() {
    try {
      return new URL[] { new URL("http://wiki.apache.org/solr/MoreLikeThis") };
    }
    catch( MalformedURLException ex ) { return null; }
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/RealTimeGetHandler.java b/solr/core/src/java/org/apache/solr/handler/RealTimeGetHandler.java
index 90493186358..bce374f4aea 100644
-- a/solr/core/src/java/org/apache/solr/handler/RealTimeGetHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/RealTimeGetHandler.java
@@ -20,7 +20,6 @@ import org.apache.solr.api.Api;
 import org.apache.solr.api.ApiBag;
 import org.apache.solr.handler.component.*;
 
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
@@ -42,11 +41,6 @@ public class RealTimeGetHandler extends SearchHandler {
     return "The realtime get handler";
   }
 
  @Override
  public URL[] getDocs() {
    return null;
  }

   @Override
   public Collection<Api> getApis() {
     return ApiBag.wrapRequestHandlers(this, "core.RealtimeGet");
diff --git a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
index 98bf11ab376..062f532d84e 100644
-- a/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java
@@ -90,6 +90,8 @@ import org.apache.solr.core.SolrEventListener;
 import org.apache.solr.core.backup.repository.BackupRepository;
 import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
 import org.apache.solr.core.snapshots.SolrSnapshotMetaDataManager;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.handler.IndexFetcher.IndexFetchResult;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
@@ -162,6 +164,10 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
       }
       return new CommitVersionInfo(generation, version);
     }

    public String toString() {
      return "generation=" + generation + ",version=" + version;
    }
   }
 
   private IndexFetcher pollingIndexFetcher;
@@ -851,52 +857,56 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
   }
 
   @Override
  @SuppressWarnings("unchecked")
  public NamedList getStatistics() {
    NamedList list = super.getStatistics();
    if (core != null) {
      list.add("indexSize", NumberUtils.readableSize(core.getIndexSize()));
      CommitVersionInfo vInfo = (core != null && !core.isClosed()) ? getIndexVersion(): null;
      list.add("indexVersion", null == vInfo ? 0 : vInfo.version);
      list.add(GENERATION, null == vInfo ? 0 : vInfo.generation);

      list.add("indexPath", core.getIndexDir());
      list.add("isMaster", String.valueOf(isMaster));
      list.add("isSlave", String.valueOf(isSlave));

  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    super.initializeMetrics(manager, registry, scope);

    manager.registerGauge(this, registry, () -> core != null ? NumberUtils.readableSize(core.getIndexSize()) : "", true,
        "indexSize", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> (core != null && !core.isClosed() ? getIndexVersion().toString() : ""), true,
        "indexVersion", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> (core != null && !core.isClosed() ? getIndexVersion().generation : 0), true,
        GENERATION, getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> core != null ? core.getIndexDir() : "", true,
        "indexPath", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> isMaster, true,
        "isMaster", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> isSlave, true,
        "isSlave", getCategory().toString(), scope);
    final MetricsMap fetcherMap = new MetricsMap((detailed, map) -> {
       IndexFetcher fetcher = currentIndexFetcher;
       if (fetcher != null) {
        list.add(MASTER_URL, fetcher.getMasterUrl());
        map.put(MASTER_URL, fetcher.getMasterUrl());
         if (getPollInterval() != null) {
          list.add(POLL_INTERVAL, getPollInterval());
          map.put(POLL_INTERVAL, getPollInterval());
         }
        list.add("isPollingDisabled", String.valueOf(isPollingDisabled()));
        list.add("isReplicating", String.valueOf(isReplicating()));
        map.put("isPollingDisabled", isPollingDisabled());
        map.put("isReplicating", isReplicating());
         long elapsed = fetcher.getReplicationTimeElapsed();
         long val = fetcher.getTotalBytesDownloaded();
         if (elapsed > 0) {
          list.add("timeElapsed", elapsed);
          list.add("bytesDownloaded", val);
          list.add("downloadSpeed", val / elapsed);
          map.put("timeElapsed", elapsed);
          map.put("bytesDownloaded", val);
          map.put("downloadSpeed", val / elapsed);
         }
         Properties props = loadReplicationProperties();
        addVal(list, IndexFetcher.PREVIOUS_CYCLE_TIME_TAKEN, props, Long.class);
        addVal(list, IndexFetcher.INDEX_REPLICATED_AT, props, Date.class);
        addVal(list, IndexFetcher.CONF_FILES_REPLICATED_AT, props, Date.class);
        addVal(list, IndexFetcher.REPLICATION_FAILED_AT, props, Date.class);
        addVal(list, IndexFetcher.TIMES_FAILED, props, Integer.class);
        addVal(list, IndexFetcher.TIMES_INDEX_REPLICATED, props, Integer.class);
        addVal(list, IndexFetcher.LAST_CYCLE_BYTES_DOWNLOADED, props, Long.class);
        addVal(list, IndexFetcher.TIMES_CONFIG_REPLICATED, props, Integer.class);
        addVal(list, IndexFetcher.CONF_FILES_REPLICATED, props, String.class);
        addVal(map, IndexFetcher.PREVIOUS_CYCLE_TIME_TAKEN, props, Long.class);
        addVal(map, IndexFetcher.INDEX_REPLICATED_AT, props, Date.class);
        addVal(map, IndexFetcher.CONF_FILES_REPLICATED_AT, props, Date.class);
        addVal(map, IndexFetcher.REPLICATION_FAILED_AT, props, Date.class);
        addVal(map, IndexFetcher.TIMES_FAILED, props, Integer.class);
        addVal(map, IndexFetcher.TIMES_INDEX_REPLICATED, props, Integer.class);
        addVal(map, IndexFetcher.LAST_CYCLE_BYTES_DOWNLOADED, props, Long.class);
        addVal(map, IndexFetcher.TIMES_CONFIG_REPLICATED, props, Integer.class);
        addVal(map, IndexFetcher.CONF_FILES_REPLICATED, props, String.class);
       }
      if (isMaster) {
        if (includeConfFiles != null) list.add("confFilesToReplicate", includeConfFiles);
        list.add(REPLICATE_AFTER, getReplicateAfterStrings());
        list.add("replicationEnabled", String.valueOf(replicationEnabled.get()));
      }
    }
    return list;
    });
    manager.registerGauge(this, registry, fetcherMap, true, "fetcher", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> isMaster && includeConfFiles != null ? includeConfFiles : "", true,
        "confFilesToReplicate", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> isMaster ? getReplicateAfterStrings() : Collections.<String>emptyList(), true,
        REPLICATE_AFTER, getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> isMaster && replicationEnabled.get(), true,
        "replicationEnabled", getCategory().toString(), scope);
   }
 
   /**
@@ -1064,24 +1074,39 @@ public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAw
   }
 
   private void addVal(NamedList<Object> nl, String key, Properties props, Class clzz) {
    Object val = formatVal(key, props, clzz);
    if (val != null) {
      nl.add(key, val);
    }
  }

  private void addVal(Map<String, Object> map, String key, Properties props, Class clzz) {
    Object val = formatVal(key, props, clzz);
    if (val != null) {
      map.put(key, val);
    }
  }

  private Object formatVal(String key, Properties props, Class clzz) {
     String s = props.getProperty(key);
    if (s == null || s.trim().length() == 0) return;
    if (s == null || s.trim().length() == 0) return null;
     if (clzz == Date.class) {
       try {
         Long l = Long.parseLong(s);
        nl.add(key, new Date(l).toString());
      } catch (NumberFormatException e) {/*no op*/ }
        return new Date(l).toString();
      } catch (NumberFormatException e) {
        return null;
      }
     } else if (clzz == List.class) {
       String ss[] = s.split(",");
       List<String> l = new ArrayList<>();
       for (String s1 : ss) {
         l.add(new Date(Long.parseLong(s1)).toString());
       }
      nl.add(key, l);
      return l;
     } else {
      nl.add(key, s);
      return s;
     }

   }
 
   private List<String> getReplicateAfterStrings() {
diff --git a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
index 1958e11f587..421976801b2 100644
-- a/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
++ b/solr/core/src/java/org/apache/solr/handler/RequestHandlerBase.java
@@ -17,9 +17,11 @@
 package org.apache.solr.handler;
 
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
 
import com.codahale.metrics.MetricRegistry;
 import com.google.common.collect.ImmutableList;
 import com.codahale.metrics.Counter;
 import com.codahale.metrics.Meter;
@@ -27,11 +29,10 @@ import com.codahale.metrics.Timer;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.common.util.SuppressForbidden;
 import org.apache.solr.core.PluginBag;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.request.SolrQueryRequest;
@@ -42,7 +43,6 @@ import org.apache.solr.util.SolrPluginUtils;
 import org.apache.solr.api.Api;
 import org.apache.solr.api.ApiBag;
 import org.apache.solr.api.ApiSupport;
import org.apache.solr.util.stats.MetricUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -51,7 +51,7 @@ import static org.apache.solr.core.RequestParams.USEPARAM;
 /**
  *
  */
public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfoMBean, SolrMetricProducer, NestedRequestHandler,ApiSupport {
public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfoBean, SolrMetricProducer, NestedRequestHandler,ApiSupport {
 
   protected NamedList initArgs = null;
   protected SolrParams defaults;
@@ -74,6 +74,9 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
 
   private PluginInfo pluginInfo;
 
  private Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;

   @SuppressForbidden(reason = "Need currentTimeMillis, used only for stats output")
   public RequestHandlerBase() {
     handlerStart = System.currentTimeMillis();
@@ -138,13 +141,15 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    numErrors = manager.meter(registryName, "errors", getCategory().toString(), scope);
    numServerErrors = manager.meter(registryName, "serverErrors", getCategory().toString(), scope);
    numClientErrors = manager.meter(registryName, "clientErrors", getCategory().toString(), scope);
    numTimeouts = manager.meter(registryName, "timeouts", getCategory().toString(), scope);
    requests = manager.counter(registryName, "requests", getCategory().toString(), scope);
    requestTimes = manager.timer(registryName, "requestTimes", getCategory().toString(), scope);
    totalTime = manager.counter(registryName, "totalTime", getCategory().toString(), scope);
    registry = manager.registry(registryName);
    numErrors = manager.meter(this, registryName, "errors", getCategory().toString(), scope);
    numServerErrors = manager.meter(this, registryName, "serverErrors", getCategory().toString(), scope);
    numClientErrors = manager.meter(this, registryName, "clientErrors", getCategory().toString(), scope);
    numTimeouts = manager.meter(this, registryName, "timeouts", getCategory().toString(), scope);
    requests = manager.counter(this, registryName, "requests", getCategory().toString(), scope);
    requestTimes = manager.timer(this, registryName, "requestTimes", getCategory().toString(), scope);
    totalTime = manager.counter(this, registryName, "totalTime", getCategory().toString(), scope);
    manager.registerGauge(this, registryName, () -> handlerStart, true, "handlerStart", getCategory().toString(), scope);
   }
 
   public static SolrParams getSolrParamsFromNamedList(NamedList args, String key) {
@@ -225,24 +230,21 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
 
   @Override
   public abstract String getDescription();
  @Override
  public String getSource() { return null; }
  
  @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }
  

   @Override
   public Category getCategory() {
     return Category.QUERY;
   }
 
   @Override
  public URL[] getDocs() {
    return null;  // this can be overridden, but not required
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
  }
 
   @Override
   public SolrRequestHandler getSubHandler(String subPath) {
@@ -285,22 +287,6 @@ public abstract class RequestHandlerBase implements SolrRequestHandler, SolrInfo
     return  pluginInfo;
   }
 

  @Override
  public NamedList<Object> getStatistics() {
    NamedList<Object> lst = new SimpleOrderedMap<>();
    lst.add("handlerStart",handlerStart);
    lst.add("requests", requests.getCount());
    lst.add("errors", numErrors.getCount());
    lst.add("serverErrors", numServerErrors.getCount());
    lst.add("clientErrors", numClientErrors.getCount());
    lst.add("timeouts", numTimeouts.getCount());
    // convert totalTime to ms
    lst.add("totalTime", MetricUtils.nsToMs(totalTime.getCount()));
    MetricUtils.addMetrics(lst, requestTimes);
    return lst;
  }

   @Override
   public Collection<Api> getApis() {
     return ImmutableList.of(new ApiBag.ReqHandlerToApi(this, ApiBag.constructSpec(pluginInfo)));
diff --git a/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java b/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
index 2660cba8354..04fd4c8e8cf 100644
-- a/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/SolrConfigHandler.java
@@ -702,12 +702,6 @@ public class SolrConfigHandler extends RequestHandlerBase implements SolrCoreAwa
     return "Edit solrconfig.xml";
   }
 

  @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }

   @Override
   public Category getCategory() {
     return Category.ADMIN;
diff --git a/solr/core/src/java/org/apache/solr/handler/StandardRequestHandler.java b/solr/core/src/java/org/apache/solr/handler/StandardRequestHandler.java
index d5eae080cfa..f167b1d5dc3 100644
-- a/solr/core/src/java/org/apache/solr/handler/StandardRequestHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/StandardRequestHandler.java
@@ -18,9 +18,6 @@ package org.apache.solr.handler;
 
 import org.apache.solr.handler.component.*;
 
import java.net.MalformedURLException;
import java.net.URL;

 /**
  *
  *
@@ -47,14 +44,6 @@ public class StandardRequestHandler extends SearchHandler
   public String getDescription() {
     return "The standard Solr request handler";
   }

  @Override
  public URL[] getDocs() {
    try {
      return new URL[] { new URL("http://wiki.apache.org/solr/StandardRequestHandler") };
    }
    catch( MalformedURLException ex ) { return null; }
  }
 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
index 275ec18a2d7..67463327e2f 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/CoreAdminHandler.java
@@ -121,7 +121,7 @@ public class CoreAdminHandler extends RequestHandlerBase implements PermissionNa
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
     super.initializeMetrics(manager, registryName, scope);
    parallelExecutor = MetricUtils.instrumentedExecutorService(parallelExecutor, manager.registry(registryName),
    parallelExecutor = MetricUtils.instrumentedExecutorService(parallelExecutor, this, manager.registry(registryName),
         SolrMetricManager.mkName("parallelCoreAdminExecutor", getCategory().name(),scope, "threadPool"));
   }
   @Override
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
index 8e0b1fb8cc7..2db04d9d0b6 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/LukeRequestHandler.java
@@ -22,8 +22,6 @@ import static org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Date;
@@ -707,14 +705,6 @@ public class LukeRequestHandler extends RequestHandlerBase
     return Category.ADMIN;
   }
 
  @Override
  public URL[] getDocs() {
    try {
      return new URL[] { new URL("http://wiki.apache.org/solr/LukeRequestHandler") };
    }
    catch( MalformedURLException ex ) { return null; }
  }

   ///////////////////////////////////////////////////////////////////////////////////////
 
   static class TermHistogram
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/MetricsCollectorHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/MetricsCollectorHandler.java
index de39a615606..8474f55acff 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/MetricsCollectorHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/MetricsCollectorHandler.java
@@ -134,7 +134,7 @@ public class MetricsCollectorHandler extends RequestHandlerBase {
 
   @Override
   public String getDescription() {
    return "Handler for collecting and aggregating metric reports.";
    return "Handler for collecting and aggregating SolrCloud metric reports.";
   }
 
   private static class MetricUpdateProcessor extends UpdateRequestProcessor {
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/MetricsHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/MetricsHandler.java
index 4dc86d97855..54ab092aa33 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/MetricsHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/MetricsHandler.java
@@ -19,6 +19,7 @@ package org.apache.solr.handler.admin;
 
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.EnumSet;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
@@ -52,6 +53,13 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
   final SolrMetricManager metricManager;
 
   public static final String COMPACT_PARAM = "compact";
  public static final String PREFIX_PARAM = "prefix";
  public static final String REGEX_PARAM = "regex";
  public static final String REGISTRY_PARAM = "registry";
  public static final String GROUP_PARAM = "group";
  public static final String TYPE_PARAM = "type";

  public static final String ALL = "all";
 
   public MetricsHandler() {
     this.container = null;
@@ -84,29 +92,38 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
     for (String registryName : requestedRegistries) {
       MetricRegistry registry = metricManager.registry(registryName);
       response.add(registryName, MetricUtils.toNamedList(registry, metricFilters, mustMatchFilter, false,
          false, compact, null));
          false, compact));
     }
     rsp.getValues().add("metrics", response);
   }
 
   private MetricFilter parseMustMatchFilter(SolrQueryRequest req) {
    String[] prefixes = req.getParams().getParams("prefix");
    MetricFilter mustMatchFilter;
    String[] prefixes = req.getParams().getParams(PREFIX_PARAM);
    MetricFilter prefixFilter = null;
     if (prefixes != null && prefixes.length > 0) {
       Set<String> prefixSet = new HashSet<>();
       for (String prefix : prefixes) {
         prefixSet.addAll(StrUtils.splitSmart(prefix, ','));
       }
      mustMatchFilter = new SolrMetricManager.PrefixFilter((String[])prefixSet.toArray(new String[prefixSet.size()]));
    } else  {
      prefixFilter = new SolrMetricManager.PrefixFilter((String[])prefixSet.toArray(new String[prefixSet.size()]));
    }
    String[] regexes = req.getParams().getParams(REGEX_PARAM);
    MetricFilter regexFilter = null;
    if (regexes != null && regexes.length > 0) {
      regexFilter = new SolrMetricManager.RegexFilter(regexes);
    }
    MetricFilter mustMatchFilter;
    if (prefixFilter == null && regexFilter == null) {
       mustMatchFilter = MetricFilter.ALL;
    } else {
      mustMatchFilter = new SolrMetricManager.OrFilter(prefixFilter, regexFilter);
     }
     return mustMatchFilter;
   }
 
   private Set<String> parseRegistries(SolrQueryRequest req) {
    String[] groupStr = req.getParams().getParams("group");
    String[] registryStr = req.getParams().getParams("registry");
    String[] groupStr = req.getParams().getParams(GROUP_PARAM);
    String[] registryStr = req.getParams().getParams(REGISTRY_PARAM);
     if ((groupStr == null || groupStr.length == 0) && (registryStr == null || registryStr.length == 0)) {
       // return all registries
       return container.getMetricManager().registryNames();
@@ -118,7 +135,7 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
       for (String g : groupStr) {
         List<String> split = StrUtils.splitSmart(g, ',');
         for (String s : split) {
          if (s.trim().equals("all")) {
          if (s.trim().equals(ALL)) {
             allRegistries = true;
             break;
           }
@@ -137,7 +154,7 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
       for (String r : registryStr) {
         List<String> split = StrUtils.splitSmart(r, ',');
         for (String s : split) {
          if (s.trim().equals("all")) {
          if (s.trim().equals(ALL)) {
             allRegistries = true;
             break;
           }
@@ -161,7 +178,7 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
   }
 
   private List<MetricType> parseMetricTypes(SolrQueryRequest req) {
    String[] typeStr = req.getParams().getParams("type");
    String[] typeStr = req.getParams().getParams(TYPE_PARAM);
     List<String> types = Collections.emptyList();
     if (typeStr != null && typeStr.length > 0)  {
       types = new ArrayList<>();
@@ -176,7 +193,8 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
         metricTypes = types.stream().map(String::trim).map(MetricType::valueOf).collect(Collectors.toList());
       }
     } catch (IllegalArgumentException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Invalid metric type in: " + types + " specified. Must be one of (all, meter, timer, histogram, counter, gauge)", e);
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Invalid metric type in: " + types +
          " specified. Must be one of " + MetricType.SUPPORTED_TYPES_MSG, e);
     }
     return metricTypes;
   }
@@ -199,6 +217,8 @@ public class MetricsHandler extends RequestHandlerBase implements PermissionName
     gauge(Gauge.class),
     all(null);
 
    public static final String SUPPORTED_TYPES_MSG = EnumSet.allOf(MetricType.class).toString();

     private final Class klass;
 
     MetricType(Class klass) {
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/PluginInfoHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/PluginInfoHandler.java
index a096e798360..8bdc478788a 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/PluginInfoHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/PluginInfoHandler.java
@@ -16,14 +16,12 @@
  */
 package org.apache.solr.handler.admin;
 
import java.net.URL;
import java.util.ArrayList;
 import java.util.Map;
 
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.handler.RequestHandlerBase;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
@@ -48,13 +46,13 @@ public class PluginInfoHandler extends RequestHandlerBase
   private static SimpleOrderedMap<Object> getSolrInfoBeans( SolrCore core, boolean stats )
   {
     SimpleOrderedMap<Object> list = new SimpleOrderedMap<>();
    for (SolrInfoMBean.Category cat : SolrInfoMBean.Category.values()) 
    for (SolrInfoBean.Category cat : SolrInfoBean.Category.values())
     {
       SimpleOrderedMap<Object> category = new SimpleOrderedMap<>();
       list.add( cat.name(), category );
      Map<String, SolrInfoMBean> reg = core.getInfoRegistry();
      for (Map.Entry<String,SolrInfoMBean> entry : reg.entrySet()) {
        SolrInfoMBean m = entry.getValue();
      Map<String, SolrInfoBean> reg = core.getInfoRegistry();
      for (Map.Entry<String,SolrInfoBean> entry : reg.entrySet()) {
        SolrInfoBean m = entry.getValue();
         if (m.getCategory() != cat) continue;
 
         String na = "Not Declared";
@@ -62,21 +60,10 @@ public class PluginInfoHandler extends RequestHandlerBase
         category.add( entry.getKey(), info );
 
         info.add( NAME,          (m.getName()       !=null ? m.getName()        : na) );
        info.add( "version",     (m.getVersion()    !=null ? m.getVersion()     : na) );
         info.add( "description", (m.getDescription()!=null ? m.getDescription() : na) );
        info.add( "source",      (m.getSource()     !=null ? m.getSource()      : na) );
 
        URL[] urls = m.getDocs();
        if ((urls != null) && (urls.length > 0)) {
          ArrayList<String> docs = new ArrayList<>(urls.length);
          for( URL u : urls ) {
            docs.add( u.toExternalForm() );
          }
          info.add( "docs", docs );
        }

        if( stats ) {
          info.add( "stats", m.getStatistics() );
        if (stats) {
          info.add( "stats", m.getMetricsSnapshot());
         }
       }
     }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/SolrInfoMBeanHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/SolrInfoMBeanHandler.java
index f5f28c53205..dd6e2c88b3f 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/SolrInfoMBeanHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/SolrInfoMBeanHandler.java
@@ -20,7 +20,7 @@ import org.apache.commons.io.IOUtils;
 import org.apache.solr.handler.RequestHandlerBase;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.util.ContentStream;
@@ -30,10 +30,7 @@ import org.apache.solr.response.BinaryResponseWriter;
 import org.apache.solr.response.SolrQueryResponse;
 
 import java.io.StringReader;
import java.net.URL;
 import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
 import java.util.Locale;
 import java.util.Set;
 import java.util.Map;
@@ -117,7 +114,7 @@ public class SolrInfoMBeanHandler extends RequestHandlerBase {
     
     String[] requestedCats = req.getParams().getParams("cat");
     if (null == requestedCats || 0 == requestedCats.length) {
      for (SolrInfoMBean.Category cat : SolrInfoMBean.Category.values()) {
      for (SolrInfoBean.Category cat : SolrInfoBean.Category.values()) {
         cats.add(cat.name(), new SimpleOrderedMap<NamedList<Object>>());
       }
     } else {
@@ -128,39 +125,27 @@ public class SolrInfoMBeanHandler extends RequestHandlerBase {
          
     Set<String> requestedKeys = arrayToSet(req.getParams().getParams("key"));
     
    Map<String, SolrInfoMBean> reg = req.getCore().getInfoRegistry();
    for (Map.Entry<String, SolrInfoMBean> entry : reg.entrySet()) {
    Map<String, SolrInfoBean> reg = req.getCore().getInfoRegistry();
    for (Map.Entry<String, SolrInfoBean> entry : reg.entrySet()) {
       addMBean(req, cats, requestedKeys, entry.getKey(),entry.getValue());
     }
 
    for (SolrInfoMBean infoMBean : req.getCore().getCoreDescriptor().getCoreContainer().getResourceLoader().getInfoMBeans()) {
    for (SolrInfoBean infoMBean : req.getCore().getCoreDescriptor().getCoreContainer().getResourceLoader().getInfoMBeans()) {
       addMBean(req,cats,requestedKeys,infoMBean.getName(),infoMBean);
     }
     return cats;
   }
 
  private void addMBean(SolrQueryRequest req, NamedList<NamedList<NamedList<Object>>> cats, Set<String> requestedKeys, String key, SolrInfoMBean m) {
  private void addMBean(SolrQueryRequest req, NamedList<NamedList<NamedList<Object>>> cats, Set<String> requestedKeys, String key, SolrInfoBean m) {
     if ( ! ( requestedKeys.isEmpty() || requestedKeys.contains(key) ) ) return;
     NamedList<NamedList<Object>> catInfo = cats.get(m.getCategory().name());
     if ( null == catInfo ) return;
     NamedList<Object> mBeanInfo = new SimpleOrderedMap<>();
     mBeanInfo.add("class", m.getName());
    mBeanInfo.add("version", m.getVersion());
     mBeanInfo.add("description", m.getDescription());
    mBeanInfo.add("src", m.getSource());

    // Use an external form
    URL[] urls = m.getDocs();
    if(urls!=null) {
      List<String> docs = new ArrayList<>(urls.length);
      for(URL url : urls) {
        docs.add(url.toExternalForm());
      }
      mBeanInfo.add("docs", docs);
    }
 
     if (req.getParams().getFieldBool(key, "stats", false))
      mBeanInfo.add("stats", m.getStatistics());
      mBeanInfo.add("stats", m.getMetricsSnapshot());
 
     catInfo.add(key, mBeanInfo);
   }
@@ -246,6 +231,9 @@ public class SolrInfoMBeanHandler extends RequestHandlerBase {
   }
   
   public Object diffObject(Object ref, Object now) {
    if (now instanceof Map) {
      now = new NamedList((Map)now);
    }
     if(ref instanceof NamedList) {
       return diffNamedList((NamedList)ref, (NamedList)now);
     }
diff --git a/solr/core/src/java/org/apache/solr/handler/admin/SystemInfoHandler.java b/solr/core/src/java/org/apache/solr/handler/admin/SystemInfoHandler.java
index fc1679ff896..ab2d4496941 100644
-- a/solr/core/src/java/org/apache/solr/handler/admin/SystemInfoHandler.java
++ b/solr/core/src/java/org/apache/solr/handler/admin/SystemInfoHandler.java
@@ -16,10 +16,6 @@
  */
 package org.apache.solr.handler.admin;
 
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
@@ -27,23 +23,20 @@ import java.io.InputStreamReader;
 import java.lang.invoke.MethodHandles;
 import java.lang.management.ManagementFactory;
 import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;
 import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
 import java.net.InetAddress;
 import java.nio.charset.Charset;
 import java.text.DecimalFormat;
 import java.text.DecimalFormatSymbols;
import java.util.Arrays;
 import java.util.Date;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 
import com.codahale.metrics.Gauge;
 import org.apache.commons.io.IOUtils;
 import org.apache.lucene.LucenePackage;
 import org.apache.lucene.util.Constants;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.SolrCore;
@@ -53,6 +46,7 @@ import org.apache.solr.response.SolrQueryResponse;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.util.RTimer;
 import org.apache.solr.util.RedactionUtils;
import org.apache.solr.util.stats.MetricUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -207,29 +201,13 @@ public class SystemInfoHandler extends RequestHandlerBase
     
     OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
     info.add(NAME, os.getName()); // add at least this one
    try {
      // add remaining ones dynamically using Java Beans API
      addMXBeanProperties(os, OperatingSystemMXBean.class, info);
    } catch (IntrospectionException | ReflectiveOperationException e) {
      log.warn("Unable to fetch properties of OperatingSystemMXBean.", e);
    }

    // There are some additional beans we want to add (not available on all JVMs):
    for (String clazz : Arrays.asList(
        "com.sun.management.OperatingSystemMXBean",
        "com.sun.management.UnixOperatingSystemMXBean", 
        "com.ibm.lang.management.OperatingSystemMXBean"
    )) {
      try {
        final Class<? extends PlatformManagedObject> intf = Class.forName(clazz)
            .asSubclass(PlatformManagedObject.class);
        addMXBeanProperties(os, intf, info);
      } catch (ClassNotFoundException e) {
        // ignore
      } catch (IntrospectionException | ReflectiveOperationException e) {
        log.warn("Unable to fetch properties of JVM-specific OperatingSystemMXBean.", e);
    // add remaining ones dynamically using Java Beans API
    // also those from JVM implementation-specific classes
    MetricUtils.addMXBeanMetrics(os, MetricUtils.OS_MXBEAN_CLASSES, null, (name, metric) -> {
      if (info.get(name) == null) {
        info.add(name, ((Gauge) metric).getValue());
       }
    }
    });
 
     // Try some command line things:
     try { 
@@ -243,34 +221,6 @@ public class SystemInfoHandler extends RequestHandlerBase
     return info;
   }
   
  /**
   * Add all bean properties of a {@link PlatformManagedObject} to the given {@link NamedList}.
   * <p>
   * If you are running a OpenJDK/Oracle JVM, there are nice properties in:
   * {@code com.sun.management.UnixOperatingSystemMXBean} and
   * {@code com.sun.management.OperatingSystemMXBean}
   */
  static <T extends PlatformManagedObject> void addMXBeanProperties(T obj, Class<? extends T> intf, NamedList<Object> info)
      throws IntrospectionException, ReflectiveOperationException {
    if (intf.isInstance(obj)) {
      final BeanInfo beanInfo = Introspector.getBeanInfo(intf, intf.getSuperclass(), Introspector.IGNORE_ALL_BEANINFO);
      for (final PropertyDescriptor desc : beanInfo.getPropertyDescriptors()) {
        final String name = desc.getName();
        if (info.get(name) == null) {
          try {
            final Object v = desc.getReadMethod().invoke(obj);
            if(v != null) {
              info.add(name, v);
            }
          } catch (InvocationTargetException ite) {
            // ignore (some properties throw UOE)
          }
        }
      }
    }
  }
  
  
   /**
    * Utility function to execute a function
    */
diff --git a/solr/core/src/java/org/apache/solr/handler/component/DebugComponent.java b/solr/core/src/java/org/apache/solr/handler/component/DebugComponent.java
index be2173339ea..f43a0e17f94 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/DebugComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/DebugComponent.java
@@ -17,7 +17,6 @@
 package org.apache.solr.handler.component;
 
 import java.io.IOException;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
@@ -380,7 +379,7 @@ public class DebugComponent extends SearchComponent
 
   
   /////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
@@ -392,9 +391,4 @@ public class DebugComponent extends SearchComponent
   public Category getCategory() {
     return Category.OTHER;
   }

  @Override
  public URL[] getDocs() {
    return null;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java b/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
index 656ac7113f0..2519a47969a 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
@@ -17,8 +17,6 @@
 package org.apache.solr.handler.component;
 
 import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
@@ -764,7 +762,7 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
 
 
   ////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
@@ -777,17 +775,6 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
     return Category.QUERY;
   }
 
  @Override
  public URL[] getDocs() {
    try {
      return new URL[]{
          new URL("http://wiki.apache.org/solr/ExpandComponent")
      };
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

   // this reader alters the content of the given reader so it should not
   // delegate the caching stuff
   private static class ReaderWrapper extends FilterLeafReader {
diff --git a/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java b/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
index 66b9ab8d713..80cca1577a5 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/FacetComponent.java
@@ -18,7 +18,6 @@ package org.apache.solr.handler.component;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
@@ -1212,7 +1211,7 @@ public class FacetComponent extends SearchComponent {
 
 
   /////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
@@ -1225,11 +1224,6 @@ public class FacetComponent extends SearchComponent {
     return Category.QUERY;
   }
 
  @Override
  public URL[] getDocs() {
    return null;
  }

   /**
    * This class is used exclusively for merging results from each shard
    * in a distributed facet request. It plays no role in the computation
diff --git a/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java b/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
index d147be2fa73..cc5211b3292 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HighlightComponent.java
@@ -17,7 +17,6 @@
 package org.apache.solr.handler.component;
 
 import java.io.IOException;
import java.net.URL;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
@@ -266,7 +265,7 @@ public class HighlightComponent extends SearchComponent implements PluginInfoIni
   }
 
   ////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
   
   @Override
@@ -278,9 +277,4 @@ public class HighlightComponent extends SearchComponent implements PluginInfoIni
   public Category getCategory() {
     return Category.HIGHLIGHTER;
   }
  
  @Override
  public URL[] getDocs() {
    return null;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
index 4262c20658c..1e1ce5ef830 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
++ b/solr/core/src/java/org/apache/solr/handler/component/HttpShardHandlerFactory.java
@@ -36,7 +36,7 @@ import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.common.util.URLUtil;
 import org.apache.solr.core.CoreDescriptor;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.update.UpdateShardHandlerConfig;
@@ -373,10 +373,10 @@ public class HttpShardHandlerFactory extends ShardHandlerFactory implements org.
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    String expandedScope = SolrMetricManager.mkName(scope, SolrInfoMBean.Category.QUERY.name());
    String expandedScope = SolrMetricManager.mkName(scope, SolrInfoBean.Category.QUERY.name());
     clientConnectionManager.initializeMetrics(manager, registry, expandedScope);
     httpRequestExecutor.initializeMetrics(manager, registry, expandedScope);
    commExecutor = MetricUtils.instrumentedExecutorService(commExecutor,
    commExecutor = MetricUtils.instrumentedExecutorService(commExecutor, null,
         manager.registry(registry),
         SolrMetricManager.mkName("httpShardExecutor", expandedScope, "threadPool"));
   }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/MoreLikeThisComponent.java b/solr/core/src/java/org/apache/solr/handler/component/MoreLikeThisComponent.java
index ffb58588907..fd9d37d4aad 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/MoreLikeThisComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/MoreLikeThisComponent.java
@@ -18,7 +18,6 @@ package org.apache.solr.handler.component;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -414,7 +413,7 @@ public class MoreLikeThisComponent extends SearchComponent {
   }
   
   // ///////////////////////////////////////////
  // / SolrInfoMBean
  // / SolrInfoBean
   // //////////////////////////////////////////
   
   @Override
@@ -426,9 +425,4 @@ public class MoreLikeThisComponent extends SearchComponent {
   public Category getCategory() {
     return Category.QUERY;
   }

  @Override
  public URL[] getDocs() {
    return null;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
index 08a0e842e08..7555158f8c6 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/QueryComponent.java
@@ -20,7 +20,6 @@ import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
@@ -1378,7 +1377,7 @@ public class QueryComponent extends SearchComponent
   }
 
   /////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
@@ -1391,11 +1390,6 @@ public class QueryComponent extends SearchComponent
     return Category.QUERY;
   }
 
  @Override
  public URL[] getDocs() {
    return null;
  }

   /**
    * Fake scorer for a single document
    *
diff --git a/solr/core/src/java/org/apache/solr/handler/component/QueryElevationComponent.java b/solr/core/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
index c12902e83b5..568000a2e66 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/QueryElevationComponent.java
@@ -24,8 +24,6 @@ import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
@@ -597,7 +595,7 @@ public class QueryElevationComponent extends SearchComponent implements SolrCore
   }
 
   //---------------------------------------------------------------------------------
  // SolrInfoMBean
  // SolrInfoBean
   //---------------------------------------------------------------------------------
 
   @Override
@@ -605,16 +603,6 @@ public class QueryElevationComponent extends SearchComponent implements SolrCore
     return "Query Boosting -- boost particular documents for a given query";
   }
 
  @Override
  public URL[] getDocs() {
    try {
      return new URL[]{
          new URL("http://wiki.apache.org/solr/QueryElevationComponent")
      };
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
   class ElevationComparatorSource extends FieldComparatorSource {
   private QueryElevationComponent.ElevationObj elevations;
   private SentinelIntSet ordSet; //the key half of the map
diff --git a/solr/core/src/java/org/apache/solr/handler/component/RealTimeGetComponent.java b/solr/core/src/java/org/apache/solr/handler/component/RealTimeGetComponent.java
index 882decb1627..2a4776224b8 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/RealTimeGetComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/RealTimeGetComponent.java
@@ -18,7 +18,6 @@ package org.apache.solr.handler.component;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
@@ -927,7 +926,7 @@ public class RealTimeGetComponent extends SearchComponent
                                                                                                
 
   ////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
@@ -940,13 +939,6 @@ public class RealTimeGetComponent extends SearchComponent
     return Category.QUERY;
   }
 
  @Override
  public URL[] getDocs() {
    return null;
  }

  
  
   public void processGetFingeprint(ResponseBuilder rb) throws IOException {
     SolrQueryRequest req = rb.req;
     SolrParams params = req.getParams();
diff --git a/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java b/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
index 6ef0ee4f237..c615c5a7ac1 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SearchComponent.java
@@ -17,13 +17,15 @@
 package org.apache.solr.handler.component;
 
 import java.io.IOException;
import java.net.URL;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.Map;
import java.util.Set;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.search.facet.FacetModule;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
@@ -33,12 +35,16 @@ import org.apache.solr.util.plugin.NamedListInitializedPlugin;
  *
  * @since solr 1.3
  */
public abstract class SearchComponent implements SolrInfoMBean, NamedListInitializedPlugin
public abstract class SearchComponent implements SolrInfoBean, NamedListInitializedPlugin
 {
   /**
    * The name given to this component in solrconfig.xml file
    */
   private String name = this.getClass().getName();

  protected Set<String> metricNames = new HashSet<>();
  protected MetricRegistry registry;

   /**
    * Prepare the response.  Guaranteed to be called before any SearchComponent {@link #process(org.apache.solr.handler.component.ResponseBuilder)} method.
    * Called for every incoming request.
@@ -103,31 +109,24 @@ public abstract class SearchComponent implements SolrInfoMBean, NamedListInitial
 
   @Override
   public abstract String getDescription();
  @Override
  public String getSource() { return null; }
  
  @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }
  

   @Override
   public Category getCategory() {
     return Category.OTHER;
   }
 
   @Override
  public URL[] getDocs() {
    return null;  // this can be overridden, but not required
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public NamedList getStatistics() {
    return null;
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   public static final Map<String, Class<? extends SearchComponent>> standard_components;
  ;

 
   static {
     HashMap<String, Class<? extends SearchComponent>> map = new HashMap<>();
diff --git a/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java b/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
index 2f805f45d02..4e3cd125c27 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SpellCheckComponent.java
@@ -853,7 +853,7 @@ public class SpellCheckComponent extends SearchComponent implements SolrCoreAwar
   }
 
   // ///////////////////////////////////////////
  // / SolrInfoMBean
  // / SolrInfoBean
   // //////////////////////////////////////////
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
index 6a6e9bef0d6..8ecd51c523a 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/StatsComponent.java
@@ -160,7 +160,7 @@ public class StatsComponent extends SearchComponent {
   }
 
   /////////////////////////////////////////////
  ///  SolrInfoMBean
  ///  SolrInfoBean
   ////////////////////////////////////////////
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/handler/component/SuggestComponent.java b/solr/core/src/java/org/apache/solr/handler/component/SuggestComponent.java
index bb87440c174..4ca6ce4b752 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/SuggestComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/SuggestComponent.java
@@ -47,6 +47,9 @@ import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrEventListener;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.spelling.suggest.SolrSuggester;
 import org.apache.solr.spelling.suggest.SuggesterOptions;
@@ -61,7 +64,7 @@ import org.slf4j.LoggerFactory;
  * Responsible for routing commands and queries to the appropriate {@link SolrSuggester}
  * and for initializing them as specified by SolrConfig
  */
public class SuggestComponent extends SearchComponent implements SolrCoreAware, SuggesterParams, Accountable {
public class SuggestComponent extends SearchComponent implements SolrCoreAware, SuggesterParams, Accountable, SolrMetricProducer {
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   
   /** Name used to identify whether the user query concerns this component */
@@ -89,7 +92,7 @@ public class SuggestComponent extends SearchComponent implements SolrCoreAware,
    * Key is the dictionary name used in SolrConfig, value is the corresponding {@link SolrSuggester}
    */
   protected Map<String, SolrSuggester> suggesters = new ConcurrentHashMap<>();
  

   /** Container for various labels used in the responses generated by this component */
   private static class SuggesterResultLabels {
     static final String SUGGEST = "suggest";
@@ -345,16 +348,18 @@ public class SuggestComponent extends SearchComponent implements SolrCoreAware,
   }
 
   @Override
  public NamedList getStatistics() {
    NamedList<String> stats = new SimpleOrderedMap<>();
    stats.add("totalSizeInBytes", String.valueOf(ramBytesUsed()));
    for (Map.Entry<String, SolrSuggester> entry : suggesters.entrySet()) {
      SolrSuggester suggester = entry.getValue();
      stats.add(entry.getKey(), suggester.toString());
    }
    return stats;
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    manager.registerGauge(this, registryName, () -> ramBytesUsed(), true, "totalSizeInBytes", getCategory().toString(), scope);
    MetricsMap suggestersMap = new MetricsMap((detailed, map) -> {
      for (Map.Entry<String, SolrSuggester> entry : suggesters.entrySet()) {
        SolrSuggester suggester = entry.getValue();
        map.put(entry.getKey(), suggester.toString());
      }
    });
    manager.registerGauge(this, registryName, suggestersMap, true, "suggesters", getCategory().toString(), scope);
   }
  

   @Override
   public long ramBytesUsed() {
     long sizeInBytes = 0;
diff --git a/solr/core/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java b/solr/core/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
index 24304d0a1b6..7e56ee44e58 100644
-- a/solr/core/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
++ b/solr/core/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
@@ -128,40 +128,58 @@ public class DefaultSolrHighlighter extends SolrHighlighter implements PluginInf
 
     // Load the fragmenters
     SolrFragmenter frag = solrCore.initPlugins(info.getChildren("fragmenter") , fragmenters,SolrFragmenter.class,null);
    if (frag == null) frag = new GapFragmenter();
    if (frag == null) {
      frag = new GapFragmenter();
      solrCore.initDefaultPlugin(frag, SolrFragmenter.class);
    }
     fragmenters.put("", frag);
     fragmenters.put(null, frag);
 
     // Load the formatters
     SolrFormatter fmt = solrCore.initPlugins(info.getChildren("formatter"), formatters,SolrFormatter.class,null);
    if (fmt == null) fmt = new HtmlFormatter();
    if (fmt == null) {
      fmt = new HtmlFormatter();
      solrCore.initDefaultPlugin(fmt, SolrFormatter.class);
    }
     formatters.put("", fmt);
     formatters.put(null, fmt);
 
     // Load the encoders
     SolrEncoder enc = solrCore.initPlugins(info.getChildren("encoder"), encoders,SolrEncoder.class,null);
    if (enc == null) enc = new DefaultEncoder();
    if (enc == null) {
      enc = new DefaultEncoder();
      solrCore.initDefaultPlugin(enc, SolrEncoder.class);
    }
     encoders.put("", enc);
     encoders.put(null, enc);
 
     // Load the FragListBuilders
     SolrFragListBuilder fragListBuilder = solrCore.initPlugins(info.getChildren("fragListBuilder"),
         fragListBuilders, SolrFragListBuilder.class, null );
    if( fragListBuilder == null ) fragListBuilder = new SimpleFragListBuilder();
    if( fragListBuilder == null ) {
      fragListBuilder = new SimpleFragListBuilder();
      solrCore.initDefaultPlugin(fragListBuilder, SolrFragListBuilder.class);
    }
     fragListBuilders.put( "", fragListBuilder );
     fragListBuilders.put( null, fragListBuilder );
 
     // Load the FragmentsBuilders
     SolrFragmentsBuilder fragsBuilder = solrCore.initPlugins(info.getChildren("fragmentsBuilder"),
         fragmentsBuilders, SolrFragmentsBuilder.class, null);
    if( fragsBuilder == null ) fragsBuilder = new ScoreOrderFragmentsBuilder();
    if( fragsBuilder == null ) {
      fragsBuilder = new ScoreOrderFragmentsBuilder();
      solrCore.initDefaultPlugin(fragsBuilder, SolrFragmentsBuilder.class);
    }
     fragmentsBuilders.put( "", fragsBuilder );
     fragmentsBuilders.put( null, fragsBuilder );
 
     // Load the BoundaryScanners
     SolrBoundaryScanner boundaryScanner = solrCore.initPlugins(info.getChildren("boundaryScanner"),
         boundaryScanners, SolrBoundaryScanner.class, null);
    if(boundaryScanner == null) boundaryScanner = new SimpleBoundaryScanner();
    if(boundaryScanner == null) {
      boundaryScanner = new SimpleBoundaryScanner();
      solrCore.initDefaultPlugin(boundaryScanner, SolrBoundaryScanner.class);
    }
     boundaryScanners.put("", boundaryScanner);
     boundaryScanners.put(null, boundaryScanner);
 
diff --git a/solr/core/src/java/org/apache/solr/highlight/GapFragmenter.java b/solr/core/src/java/org/apache/solr/highlight/GapFragmenter.java
index 64cb280a25a..6a11bb9018c 100644
-- a/solr/core/src/java/org/apache/solr/highlight/GapFragmenter.java
++ b/solr/core/src/java/org/apache/solr/highlight/GapFragmenter.java
@@ -30,7 +30,7 @@ public class GapFragmenter extends HighlightingPluginBase implements SolrFragmen
   @Override
   public Fragmenter getFragmenter(String fieldName, SolrParams params )
   {
    numRequests++;
    numRequests.inc();
     params = SolrParams.wrapDefaults(params, defaults);
     
     int fragsize = params.getFieldInt( fieldName, HighlightParams.FRAGSIZE, 100 );
diff --git a/solr/core/src/java/org/apache/solr/highlight/HighlightingPluginBase.java b/solr/core/src/java/org/apache/solr/highlight/HighlightingPluginBase.java
index f60ada82d1b..7acaacdd03c 100644
-- a/solr/core/src/java/org/apache/solr/highlight/HighlightingPluginBase.java
++ b/solr/core/src/java/org/apache/solr/highlight/HighlightingPluginBase.java
@@ -16,21 +16,27 @@
  */
 package org.apache.solr.highlight;
 
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
 
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 
 /**
  * 
  * @since solr 1.3
  */
public abstract class HighlightingPluginBase implements SolrInfoMBean
public abstract class HighlightingPluginBase implements SolrInfoBean, SolrMetricProducer
 {
  protected long numRequests;
  protected Counter numRequests;
   protected SolrParams defaults;
  protected Set<String> metricNames = new HashSet<>(1);
  protected MetricRegistry registry;
 
   public void init(NamedList args) {
     if( args != null ) {
@@ -50,14 +56,7 @@ public abstract class HighlightingPluginBase implements SolrInfoMBean
 
   @Override
   public abstract String getDescription();
  @Override
  public String getSource() { return null; }
  
  @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }
  

   @Override
   public Category getCategory()
   {
@@ -65,15 +64,19 @@ public abstract class HighlightingPluginBase implements SolrInfoMBean
   }
 
   @Override
  public URL[] getDocs() {
    return null;  // this can be overridden, but not required
  public Set<String> getMetricNames() {
    return metricNames;
  }

  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   @Override
  public NamedList getStatistics() {
    NamedList<Long> lst = new SimpleOrderedMap<>();
    lst.add("requests", numRequests);
    return lst;
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    numRequests = manager.counter(this, registryName, "requests", getCategory().toString(), scope);
   }
 }
 
diff --git a/solr/core/src/java/org/apache/solr/highlight/HtmlFormatter.java b/solr/core/src/java/org/apache/solr/highlight/HtmlFormatter.java
index 842d5cdbfe6..0950c53e7ef 100644
-- a/solr/core/src/java/org/apache/solr/highlight/HtmlFormatter.java
++ b/solr/core/src/java/org/apache/solr/highlight/HtmlFormatter.java
@@ -29,7 +29,7 @@ public class HtmlFormatter extends HighlightingPluginBase implements SolrFormatt
   @Override
   public Formatter getFormatter(String fieldName, SolrParams params ) 
   {
    numRequests++;
    numRequests.inc();
     params = SolrParams.wrapDefaults(params, defaults);
 
     return new SimpleHTMLFormatter(
diff --git a/solr/core/src/java/org/apache/solr/highlight/RegexFragmenter.java b/solr/core/src/java/org/apache/solr/highlight/RegexFragmenter.java
index b755b2d0a92..ffefbad33ac 100644
-- a/solr/core/src/java/org/apache/solr/highlight/RegexFragmenter.java
++ b/solr/core/src/java/org/apache/solr/highlight/RegexFragmenter.java
@@ -60,7 +60,7 @@ public class RegexFragmenter extends HighlightingPluginBase implements SolrFragm
   @Override
   public Fragmenter getFragmenter(String fieldName, SolrParams params )
   { 
    numRequests++;
    numRequests.inc();
     params = SolrParams.wrapDefaults(params, defaults);
 
     int fragsize  = params.getFieldInt(   fieldName, HighlightParams.FRAGSIZE,  LuceneRegexFragmenter.DEFAULT_FRAGMENT_SIZE );
diff --git a/solr/core/src/java/org/apache/solr/highlight/SimpleFragListBuilder.java b/solr/core/src/java/org/apache/solr/highlight/SimpleFragListBuilder.java
index ed5430ce1e6..7e30a9231ae 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SimpleFragListBuilder.java
++ b/solr/core/src/java/org/apache/solr/highlight/SimpleFragListBuilder.java
@@ -28,7 +28,7 @@ public class SimpleFragListBuilder extends HighlightingPluginBase implements
     // If that ever changes, it should wrap them with defaults...
     // params = SolrParams.wrapDefaults(params, defaults)
 
    numRequests++;
    numRequests.inc();
 
     return new org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder();
   }
diff --git a/solr/core/src/java/org/apache/solr/highlight/SingleFragListBuilder.java b/solr/core/src/java/org/apache/solr/highlight/SingleFragListBuilder.java
index 0b79929b35d..0dfa16e454a 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SingleFragListBuilder.java
++ b/solr/core/src/java/org/apache/solr/highlight/SingleFragListBuilder.java
@@ -28,7 +28,7 @@ public class SingleFragListBuilder extends HighlightingPluginBase implements
     // If that ever changes, it should wrap them with defaults...
     // params = SolrParams.wrapDefaults(params, defaults)
 
    numRequests++;
    numRequests.inc();
 
     return new org.apache.lucene.search.vectorhighlight.SingleFragListBuilder();
   }
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrBoundaryScanner.java b/solr/core/src/java/org/apache/solr/highlight/SolrBoundaryScanner.java
index 6f442f72bf1..ddbbfdeb88b 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrBoundaryScanner.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrBoundaryScanner.java
@@ -18,14 +18,14 @@ package org.apache.solr.highlight;
 
 import org.apache.lucene.search.vectorhighlight.BoundaryScanner;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
 public abstract class SolrBoundaryScanner extends HighlightingPluginBase implements
    SolrInfoMBean, NamedListInitializedPlugin {
    SolrInfoBean, NamedListInitializedPlugin {
 
   public BoundaryScanner getBoundaryScanner(String fieldName, SolrParams params){
    numRequests++;
    numRequests.inc();
     params = SolrParams.wrapDefaults(params, defaults);
 
     return get(fieldName, params);
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrEncoder.java b/solr/core/src/java/org/apache/solr/highlight/SolrEncoder.java
index 9f49228805d..7b78a06969f 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrEncoder.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrEncoder.java
@@ -19,10 +19,10 @@ package org.apache.solr.highlight;
 import org.apache.lucene.search.highlight.Encoder;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
public interface SolrEncoder extends SolrInfoMBean, NamedListInitializedPlugin {
public interface SolrEncoder extends SolrInfoBean, NamedListInitializedPlugin {
 
   /** <code>init</code> will be called just once, immediately after creation.
    * <p>The args are user-level initialization parameters that
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrFormatter.java b/solr/core/src/java/org/apache/solr/highlight/SolrFormatter.java
index a8f51dbcd46..1a6443e6def 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrFormatter.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrFormatter.java
@@ -19,10 +19,10 @@ package org.apache.solr.highlight;
 import org.apache.lucene.search.highlight.Formatter;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
public interface SolrFormatter extends SolrInfoMBean, NamedListInitializedPlugin {
public interface SolrFormatter extends SolrInfoBean, NamedListInitializedPlugin {
 
   /** <code>init</code> will be called just once, immediately after creation.
    * <p>The args are user-level initialization parameters that
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrFragListBuilder.java b/solr/core/src/java/org/apache/solr/highlight/SolrFragListBuilder.java
index f0c36b4d602..87da23513b0 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrFragListBuilder.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrFragListBuilder.java
@@ -19,10 +19,10 @@ package org.apache.solr.highlight;
 import org.apache.lucene.search.vectorhighlight.FragListBuilder;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
public interface SolrFragListBuilder extends SolrInfoMBean, NamedListInitializedPlugin {
public interface SolrFragListBuilder extends SolrInfoBean, NamedListInitializedPlugin {
 
   /** <code>init</code> will be called just once, immediately after creation.
    * <p>The args are user-level initialization parameters that
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrFragmenter.java b/solr/core/src/java/org/apache/solr/highlight/SolrFragmenter.java
index 547506f5cf1..98c3056993d 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrFragmenter.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrFragmenter.java
@@ -19,10 +19,10 @@ package org.apache.solr.highlight;
 import org.apache.lucene.search.highlight.Fragmenter;
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
public interface SolrFragmenter extends SolrInfoMBean, NamedListInitializedPlugin {
public interface SolrFragmenter extends SolrInfoBean, NamedListInitializedPlugin {
 
   /** <code>init</code> will be called just once, immediately after creation.
    * <p>The args are user-level initialization parameters that
diff --git a/solr/core/src/java/org/apache/solr/highlight/SolrFragmentsBuilder.java b/solr/core/src/java/org/apache/solr/highlight/SolrFragmentsBuilder.java
index 78ea5a4deee..023d55ae391 100644
-- a/solr/core/src/java/org/apache/solr/highlight/SolrFragmentsBuilder.java
++ b/solr/core/src/java/org/apache/solr/highlight/SolrFragmentsBuilder.java
@@ -21,11 +21,11 @@ import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.HighlightParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
 public abstract class SolrFragmentsBuilder extends HighlightingPluginBase
  implements SolrInfoMBean, NamedListInitializedPlugin {
  implements SolrInfoBean, NamedListInitializedPlugin {
   
   public static final String DEFAULT_PRE_TAGS = "<em>";
   public static final String DEFAULT_POST_TAGS = "</em>";
@@ -37,7 +37,7 @@ public abstract class SolrFragmentsBuilder extends HighlightingPluginBase
    * @return An appropriate {@link org.apache.lucene.search.vectorhighlight.FragmentsBuilder}.
    */
   public FragmentsBuilder getFragmentsBuilder(SolrParams params, BoundaryScanner bs) {
    numRequests++;
    numRequests.inc();
     params = SolrParams.wrapDefaults(params, defaults);
 
     return getFragmentsBuilder( params, getPreTags( params, null ), getPostTags( params, null ), bs );
diff --git a/solr/core/src/java/org/apache/solr/highlight/WeightedFragListBuilder.java b/solr/core/src/java/org/apache/solr/highlight/WeightedFragListBuilder.java
index f44c0f0b430..b97cc31c89d 100644
-- a/solr/core/src/java/org/apache/solr/highlight/WeightedFragListBuilder.java
++ b/solr/core/src/java/org/apache/solr/highlight/WeightedFragListBuilder.java
@@ -28,7 +28,7 @@ public class WeightedFragListBuilder extends HighlightingPluginBase implements
     // If that ever changes, it should wrap them with defaults...
     // params = SolrParams.wrapDefaults(params, defaults)
     
    numRequests++;
    numRequests.inc();
     
     return new org.apache.lucene.search.vectorhighlight.WeightedFragListBuilder();
   }
diff --git a/solr/core/src/java/org/apache/solr/metrics/AltBufferPoolMetricSet.java b/solr/core/src/java/org/apache/solr/metrics/AltBufferPoolMetricSet.java
new file mode 100644
index 00000000000..f9d3a43b7dc
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/metrics/AltBufferPoolMetricSet.java
@@ -0,0 +1,47 @@
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
package org.apache.solr.metrics;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

/**
 * This is an alternative implementation of {@link com.codahale.metrics.jvm.BufferPoolMetricSet} that
 * doesn't need an MBean server.
 */
public class AltBufferPoolMetricSet implements MetricSet {

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> metrics = new HashMap<>();
    List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    for (final BufferPoolMXBean pool : pools) {
      String name = pool.getName();
      metrics.put(name + ".Count", (Gauge<Long>)() -> pool.getCount());
      metrics.put(name + ".MemoryUsed", (Gauge<Long>)() -> pool.getMemoryUsed());
      metrics.put(name + ".TotalCapacity", (Gauge<Long>)() -> pool.getTotalCapacity());
    }
    return metrics;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/metrics/MetricsMap.java b/solr/core/src/java/org/apache/solr/metrics/MetricsMap.java
new file mode 100644
index 00000000000..f43c60b5927
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/metrics/MetricsMap.java
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
package org.apache.solr.metrics;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamically constructed map of metrics, intentionally different from {@link com.codahale.metrics.MetricSet}
 * where each metric had to be known in advance and registered separately in {@link com.codahale.metrics.MetricRegistry}.
 * <p>Note: this awkwardly extends {@link Gauge} and not {@link Metric} because awkwardly {@link Metric} instances
 * are not supported by {@link com.codahale.metrics.MetricRegistryListener} :(</p>
 * <p>Note 2: values added to this metric map should belong to the list of types supported by JMX:
 * {@link javax.management.openmbean.OpenType#ALLOWED_CLASSNAMES_LIST}, otherwise only their toString()
 * representation will be shown in JConsole.</p>
 */
public class MetricsMap implements Gauge<Map<String,Object>>, DynamicMBean {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // set to true to use cached statistics between getMBeanInfo calls to work
  // around over calling getStatistics on MBeanInfos when iterating over all attributes (SOLR-6586)
  private final boolean useCachedStatsBetweenGetMBeanInfoCalls = Boolean.getBoolean("useCachedStatsBetweenGetMBeanInfoCalls");

  private BiConsumer<Boolean, Map<String, Object>> initializer;
  private volatile Map<String,Object> cachedValue;

  public MetricsMap(BiConsumer<Boolean, Map<String,Object>> initializer) {
    this.initializer = initializer;
  }

  @Override
  public Map<String,Object> getValue() {
    return getValue(true);
  }

  public Map<String,Object> getValue(boolean detailed) {
    Map<String,Object> map = new HashMap<>();
    initializer.accept(detailed, map);
    return map;
  }

  public String toString() {
    return getValue().toString();
  }

  @Override
  public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
    Object val;
    Map<String,Object> stats = null;
    if (useCachedStatsBetweenGetMBeanInfoCalls) {
      Map<String,Object> cachedStats = this.cachedValue;
      if (cachedStats != null) {
        stats = cachedStats;
      }
    }
    if (stats == null) {
      stats = getValue(true);
    }
    val = stats.get(attribute);

    if (val != null) {
      // It's String or one of the simple types, just return it as JMX suggests direct support for such types
      for (String simpleTypeName : SimpleType.ALLOWED_CLASSNAMES_LIST) {
        if (val.getClass().getName().equals(simpleTypeName)) {
          return val;
        }
      }
      // It's an arbitrary object which could be something complex and odd, return its toString, assuming that is
      // a workable representation of the object
      return val.toString();
    }
    return null;
  }

  @Override
  public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    throw new UnsupportedOperationException("Operation not Supported");
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    AttributeList list = new AttributeList();
    for (String attribute : attributes) {
      try {
        list.add(new Attribute(attribute, getAttribute(attribute)));
      } catch (Exception e) {
        log.warn("Could not get attribute " + attribute);
      }
    }
    return list;
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    throw new UnsupportedOperationException("Operation not Supported");
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
    throw new UnsupportedOperationException("Operation not Supported");
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    ArrayList<MBeanAttributeInfo> attrInfoList = new ArrayList<>();
    Map<String,Object> stats = getValue(true);
    if (useCachedStatsBetweenGetMBeanInfoCalls) {
      cachedValue = stats;
    }
    try {
      stats.forEach((k, v) -> {
        Class type = v.getClass();
        OpenType typeBox = determineType(type);
        if (type.equals(String.class) || typeBox == null) {
          attrInfoList.add(new MBeanAttributeInfo(k, String.class.getName(),
              null, true, false, false));
        } else {
          attrInfoList.add(new OpenMBeanAttributeInfoSupport(
              k, k, typeBox, true, false, false));
        }
      });
    } catch (Exception e) {
      // don't log issue if the core is closing
      if (!(SolrException.getRootCause(e) instanceof AlreadyClosedException))
        log.warn("Could not get attributes of MetricsMap: {}", this, e);
    }
    MBeanAttributeInfo[] attrInfoArr = attrInfoList
        .toArray(new MBeanAttributeInfo[attrInfoList.size()]);
    return new MBeanInfo(getClass().getName(), "MetricsMap", attrInfoArr, null, null, null);
  }

  private OpenType determineType(Class type) {
    try {
      for (Field field : SimpleType.class.getFields()) {
        if (field.getType().equals(SimpleType.class)) {
          SimpleType candidate = (SimpleType) field.get(SimpleType.class);
          if (candidate.getTypeName().equals(type.getName())) {
            return candidate;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }
}
\ No newline at end of file
diff --git a/solr/core/src/java/org/apache/solr/metrics/OperatingSystemMetricSet.java b/solr/core/src/java/org/apache/solr/metrics/OperatingSystemMetricSet.java
index 34ef5d1c2e6..21957eb2285 100644
-- a/solr/core/src/java/org/apache/solr/metrics/OperatingSystemMetricSet.java
++ b/solr/core/src/java/org/apache/solr/metrics/OperatingSystemMetricSet.java
@@ -16,77 +16,31 @@
  */
 package org.apache.solr.metrics;
 
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.Map;
import java.util.Set;
 
import com.codahale.metrics.JmxAttributeGauge;
 import com.codahale.metrics.Metric;
 import com.codahale.metrics.MetricSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.solr.util.stats.MetricUtils;
 
 /**
  * This is an extended replacement for {@link com.codahale.metrics.jvm.FileDescriptorRatioGauge}
 * - that class uses reflection and doesn't work under Java 9. We can also get much more
 * information about OS environment once we have to go through MBeanServer anyway.
 * - that class uses reflection and doesn't work under Java 9. This implementation tries to retrieve
 * bean properties from known implementations of {@link java.lang.management.OperatingSystemMXBean}.
  */
 public class OperatingSystemMetricSet implements MetricSet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Metric names - these correspond to known numeric MBean attributes. Depending on the OS and
   * Java implementation only some of them may be actually present.
   */
  public static final String[] METRICS = {
      "AvailableProcessors",
      "CommittedVirtualMemorySize",
      "FreePhysicalMemorySize",
      "FreeSwapSpaceSize",
      "MaxFileDescriptorCount",
      "OpenFileDescriptorCount",
      "ProcessCpuLoad",
      "ProcessCpuTime",
      "SystemLoadAverage",
      "TotalPhysicalMemorySize",
      "TotalSwapSpaceSize"
  };

  private final MBeanServer mBeanServer;

  public OperatingSystemMetricSet(MBeanServer mBeanServer) {
    this.mBeanServer = mBeanServer;
  }
 
   @Override
   public Map<String, Metric> getMetrics() {
     final Map<String, Metric> metrics = new HashMap<>();

    try {
      final ObjectName on = new ObjectName("java.lang:type=OperatingSystem");
      // verify that it exists
      MBeanInfo info = mBeanServer.getMBeanInfo(on);
      // collect valid attributes
      Set<String> attributes = new HashSet<>();
      for (MBeanAttributeInfo ai : info.getAttributes()) {
        attributes.add(ai.getName());
      }
      for (String metric : METRICS) {
        // verify that an attribute exists before attempting to add it
        if (attributes.contains(metric)) {
          metrics.put(metric, new JmxAttributeGauge(mBeanServer, on, metric));
        }
    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    MetricUtils.addMXBeanMetrics(os, MetricUtils.OS_MXBEAN_CLASSES, null, (k, v) -> {
      if (!metrics.containsKey(k)) {
        metrics.put(k, v);
       }
    } catch (JMException ignored) {
      log.debug("Unable to load OperatingSystem MBean", ignored);
    }

    });
     return metrics;
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/metrics/SolrCoreMetricManager.java b/solr/core/src/java/org/apache/solr/metrics/SolrCoreMetricManager.java
index 43f35352ebf..193bf68ff3e 100644
-- a/solr/core/src/java/org/apache/solr/metrics/SolrCoreMetricManager.java
++ b/solr/core/src/java/org/apache/solr/metrics/SolrCoreMetricManager.java
@@ -20,11 +20,12 @@ import java.io.Closeable;
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.cloud.CloudDescriptor;
 import org.apache.solr.core.NodeConfig;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -76,14 +77,14 @@ public class SolrCoreMetricManager implements Closeable {
   }
 
   /**
   * Load reporters configured globally and specific to {@link org.apache.solr.core.SolrInfoMBean.Group#core}
   * Load reporters configured globally and specific to {@link org.apache.solr.core.SolrInfoBean.Group#core}
    * group or with a registry name specific to this core.
    */
   public void loadReporters() {
     NodeConfig nodeConfig = core.getCoreDescriptor().getCoreContainer().getConfig();
     PluginInfo[] pluginInfos = nodeConfig.getMetricReporterPlugins();
     metricManager.loadReporters(pluginInfos, core.getResourceLoader(), tag,
        SolrInfoMBean.Group.core, registryName);
        SolrInfoBean.Group.core, registryName);
     if (cloudMode) {
       metricManager.loadShardReporters(pluginInfos, core);
     }
@@ -126,12 +127,26 @@ public class SolrCoreMetricManager implements Closeable {
     producer.initializeMetrics(metricManager, getRegistryName(), scope);
   }
 
  /**
   * Return the registry used by this SolrCore.
   */
  public MetricRegistry getRegistry() {
    if (registryName != null) {
      return metricManager.registry(registryName);
    } else {
      return null;
    }
  }

   /**
    * Closes reporters specific to this core.
    */
   @Override
   public void close() throws IOException {
     metricManager.closeReporters(getRegistryName(), tag);
    if (getLeaderRegistryName() != null) {
      metricManager.closeReporters(getLeaderRegistryName(), tag);
    }
   }
 
   public SolrCore getCore() {
@@ -176,9 +191,9 @@ public class SolrCoreMetricManager implements Closeable {
 
   public static String createRegistryName(boolean cloud, String collectionName, String shardName, String replicaName, String coreName) {
     if (cloud) { // build registry name from logical names
      return SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, collectionName, shardName, replicaName);
      return SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, collectionName, shardName, replicaName);
     } else {
      return SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, coreName);
      return SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, coreName);
     }
   }
 
@@ -224,7 +239,7 @@ public class SolrCoreMetricManager implements Closeable {
 
   public static String createLeaderRegistryName(boolean cloud, String collectionName, String shardName) {
     if (cloud) {
      return SolrMetricManager.getRegistryName(SolrInfoMBean.Group.collection, collectionName, shardName, "leader");
      return SolrMetricManager.getRegistryName(SolrInfoBean.Group.collection, collectionName, shardName, "leader");
     } else {
       return null;
     }
diff --git a/solr/core/src/java/org/apache/solr/metrics/SolrMetricInfo.java b/solr/core/src/java/org/apache/solr/metrics/SolrMetricInfo.java
index 4d093ebb43c..8edfa042809 100644
-- a/solr/core/src/java/org/apache/solr/metrics/SolrMetricInfo.java
++ b/solr/core/src/java/org/apache/solr/metrics/SolrMetricInfo.java
@@ -17,7 +17,7 @@
 package org.apache.solr.metrics;
 
 import com.codahale.metrics.MetricRegistry;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 
 /**
  * Wraps meta-data for a metric.
@@ -25,7 +25,7 @@ import org.apache.solr.core.SolrInfoMBean;
 public final class SolrMetricInfo {
   public final String name;
   public final String scope;
  public final SolrInfoMBean.Category category;
  public final SolrInfoBean.Category category;
 
   /**
    * Creates a new instance of {@link SolrMetricInfo}.
@@ -34,7 +34,7 @@ public final class SolrMetricInfo {
    * @param scope    the scope of the metric (e.g. `/admin/ping`)
    * @param name     the name of the metric (e.g. `Requests`)
    */
  public SolrMetricInfo(SolrInfoMBean.Category category, String scope, String name) {
  public SolrMetricInfo(SolrInfoBean.Category category, String scope, String name) {
     this.name = name;
     this.scope = scope;
     this.category = category;
@@ -45,18 +45,25 @@ public final class SolrMetricInfo {
       return null;
     }
     String[] names = fullName.split("\\.");
    if (names.length < 3) { // not a valid info
    if (names.length < 2) { // not a valid info
       return null;
     }
     // check top-level name for valid category
    SolrInfoMBean.Category category;
    SolrInfoBean.Category category;
     try {
      category = SolrInfoMBean.Category.valueOf(names[0]);
      category = SolrInfoBean.Category.valueOf(names[0]);
     } catch (IllegalArgumentException e) { // not a valid category
       return null;
     }
    String scope = names[1];
    String name = fullName.substring(names[0].length() + names[1].length() + 2);
    String scope;
    String name;
    if (names.length == 2) {
      scope = null;
      name = fullName.substring(names[0].length() + 1);
    } else {
      scope = names[1];
      name = fullName.substring(names[0].length() + names[1].length() + 2);
    }
     return new SolrMetricInfo(category, scope, name);
   }
 
diff --git a/solr/core/src/java/org/apache/solr/metrics/SolrMetricManager.java b/solr/core/src/java/org/apache/solr/metrics/SolrMetricManager.java
index f4abee0cc92..d4eb06ae7de 100644
-- a/solr/core/src/java/org/apache/solr/metrics/SolrMetricManager.java
++ b/solr/core/src/java/org/apache/solr/metrics/SolrMetricManager.java
@@ -51,7 +51,7 @@ import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.metrics.reporters.solr.SolrClusterReporter;
 import org.apache.solr.metrics.reporters.solr.SolrShardReporter;
@@ -69,11 +69,11 @@ import org.slf4j.LoggerFactory;
  * {@link MetricRegistry} instances are automatically created when first referenced by name. Similarly,
  * instances of {@link Metric} implementations, such as {@link Meter}, {@link Counter}, {@link Timer} and
  * {@link Histogram} are automatically created and registered under hierarchical names, in a specified
 * registry, when {@link #meter(String, String, String...)} and other similar methods are called.
 * registry, when {@link #meter(SolrInfoBean, String, String, String...)} and other similar methods are called.
  * <p>This class enforces a common prefix ({@link #REGISTRY_NAME_PREFIX}) in all registry
  * names.</p>
  * <p>Solr uses several different registries for collecting metrics belonging to different groups, using
 * {@link org.apache.solr.core.SolrInfoMBean.Group} as the main name of the registry (plus the
 * {@link org.apache.solr.core.SolrInfoBean.Group} as the main name of the registry (plus the
  * above-mentioned prefix). Instances of {@link SolrMetricManager} are created for each {@link org.apache.solr.core.CoreContainer},
  * and most registries are local to each instance, with the exception of two global registries:
  * <code>solr.jetty</code> and <code>solr.jvm</code>, which are shared between all {@link org.apache.solr.core.CoreContainer}-s</p>
@@ -87,11 +87,11 @@ public class SolrMetricManager {
 
   /** Registry name for Jetty-specific metrics. This name is also subject to overrides controlled by
    * system properties. This registry is shared between instances of {@link SolrMetricManager}. */
  public static final String JETTY_REGISTRY = REGISTRY_NAME_PREFIX + SolrInfoMBean.Group.jetty.toString();
  public static final String JETTY_REGISTRY = REGISTRY_NAME_PREFIX + SolrInfoBean.Group.jetty.toString();
 
   /** Registry name for JVM-specific metrics. This name is also subject to overrides controlled by
    * system properties. This registry is shared between instances of {@link SolrMetricManager}. */
  public static final String JVM_REGISTRY = REGISTRY_NAME_PREFIX + SolrInfoMBean.Group.jvm.toString();
  public static final String JVM_REGISTRY = REGISTRY_NAME_PREFIX + SolrInfoBean.Group.jvm.toString();
 
   private final ConcurrentMap<String, MetricRegistry> registries = new ConcurrentHashMap<>();
 
@@ -247,6 +247,66 @@ public class SolrMetricManager {
     }
   }
 
  public static class OrFilter implements MetricFilter {
    List<MetricFilter> filters = new ArrayList<>();

    public OrFilter(Collection<MetricFilter> filters) {
      if (filters != null) {
        this.filters.addAll(filters);
      }
    }

    public OrFilter(MetricFilter... filters) {
      if (filters != null) {
        for (MetricFilter filter : filters) {
          if (filter != null) {
            this.filters.add(filter);
          }
        }
      }
    }

    @Override
    public boolean matches(String s, Metric metric) {
      for (MetricFilter filter : filters) {
        if (filter.matches(s, metric)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class AndFilter implements MetricFilter {
    List<MetricFilter> filters = new ArrayList<>();

    public AndFilter(Collection<MetricFilter> filters) {
      if (filters != null) {
        this.filters.addAll(filters);
      }
    }

    public AndFilter(MetricFilter... filters) {
      if (filters != null) {
        for (MetricFilter filter : filters) {
          if (filter != null) {
            this.filters.add(filter);
          }
        }
      }
    }

    @Override
    public boolean matches(String s, Metric metric) {
      for (MetricFilter filter : filters) {
        if (!filter.matches(s, metric)) {
          return false;
        }
      }
      return true;
    }
  }

   /**
    * Return a set of existing registry names.
    */
@@ -451,6 +511,21 @@ public class SolrMetricManager {
     return filter.getMatched();
   }
 
  /**
   * Retrieve matching metrics and their names.
   * @param registry registry name.
   * @param metricFilter filter (null is equivalent to {@link MetricFilter#ALL}).
   * @return map of matching names and metrics
   */
  public Map<String, Metric> getMetrics(String registry, MetricFilter metricFilter) {
    if (metricFilter == null || metricFilter == MetricFilter.ALL) {
      return registry(registry).getMetrics();
    }
    return registry(registry).getMetrics().entrySet().stream()
        .filter(entry -> metricFilter.matches(entry.getKey(), entry.getValue()))
        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
  }

   /**
    * Create or get an existing named {@link Meter}
    * @param registry registry name
@@ -459,8 +534,12 @@ public class SolrMetricManager {
    * @param metricPath (optional) additional top-most metric name path elements
    * @return existing or a newly created {@link Meter}
    */
  public Meter meter(String registry, String metricName, String... metricPath) {
    return registry(registry).meter(mkName(metricName, metricPath));
  public Meter meter(SolrInfoBean info, String registry, String metricName, String... metricPath) {
    final String name = mkName(metricName, metricPath);
    if (info != null) {
      info.registerMetricName(name);
    }
    return registry(registry).meter(name);
   }
 
   /**
@@ -471,8 +550,12 @@ public class SolrMetricManager {
    * @param metricPath (optional) additional top-most metric name path elements
    * @return existing or a newly created {@link Timer}
    */
  public Timer timer(String registry, String metricName, String... metricPath) {
    return registry(registry).timer(mkName(metricName, metricPath));
  public Timer timer(SolrInfoBean info, String registry, String metricName, String... metricPath) {
    final String name = mkName(metricName, metricPath);
    if (info != null) {
      info.registerMetricName(name);
    }
    return registry(registry).timer(name);
   }
 
   /**
@@ -483,8 +566,12 @@ public class SolrMetricManager {
    * @param metricPath (optional) additional top-most metric name path elements
    * @return existing or a newly created {@link Counter}
    */
  public Counter counter(String registry, String metricName, String... metricPath) {
    return registry(registry).counter(mkName(metricName, metricPath));
  public Counter counter(SolrInfoBean info, String registry, String metricName, String... metricPath) {
    final String name = mkName(metricName, metricPath);
    if (info != null) {
      info.registerMetricName(name);
    }
    return registry(registry).counter(name);
   }
 
   /**
@@ -495,8 +582,12 @@ public class SolrMetricManager {
    * @param metricPath (optional) additional top-most metric name path elements
    * @return existing or a newly created {@link Histogram}
    */
  public Histogram histogram(String registry, String metricName, String... metricPath) {
    return registry(registry).histogram(mkName(metricName, metricPath));
  public Histogram histogram(SolrInfoBean info, String registry, String metricName, String... metricPath) {
    final String name = mkName(metricName, metricPath);
    if (info != null) {
      info.registerMetricName(name);
    }
    return registry(registry).histogram(name);
   }
 
   /**
@@ -510,9 +601,12 @@ public class SolrMetricManager {
    *                   using dotted notation
    * @param metricPath (optional) additional top-most metric name path elements
    */
  public void register(String registry, Metric metric, boolean force, String metricName, String... metricPath) {
  public void register(SolrInfoBean info, String registry, Metric metric, boolean force, String metricName, String... metricPath) {
     MetricRegistry metricRegistry = registry(registry);
     String fullName = mkName(metricName, metricPath);
    if (info != null) {
      info.registerMetricName(fullName);
    }
     synchronized (metricRegistry) {
       if (force && metricRegistry.getMetrics().containsKey(fullName)) {
         metricRegistry.remove(fullName);
@@ -521,8 +615,8 @@ public class SolrMetricManager {
     }
   }
 
  public void registerGauge(String registry, Gauge<?> gauge, boolean force, String metricName, String... metricPath) {
    register(registry, gauge, force, metricName, metricPath);
  public void registerGauge(SolrInfoBean info, String registry, Gauge<?> gauge, boolean force, String metricName, String... metricPath) {
    register(info, registry, gauge, force, metricName, metricPath);
   }
 
   /**
@@ -569,7 +663,7 @@ public class SolrMetricManager {
    * </pre>
    * <b>NOTE:</b> Once a registry is renamed in a way that its metrics are combined with another repository
    * it is no longer possible to retrieve the original metrics until this renaming is removed and the Solr
   * {@link org.apache.solr.core.SolrInfoMBean.Group} of components that reported to that name is restarted.
   * {@link org.apache.solr.core.SolrInfoBean.Group} of components that reported to that name is restarted.
    * @param registry The name of the registry
    * @return A potentially overridden (via System properties) registry name
    */
@@ -600,7 +694,7 @@ public class SolrMetricManager {
    *              and the group parameter will be ignored.
    * @return fully-qualified and prefixed registry name, with overrides applied.
    */
  public static String getRegistryName(SolrInfoMBean.Group group, String... names) {
  public static String getRegistryName(SolrInfoBean.Group group, String... names) {
     String fullName;
     String prefix = REGISTRY_NAME_PREFIX + group.toString() + ".";
     // check for existing prefix and group
@@ -622,7 +716,7 @@ public class SolrMetricManager {
   // reporter management
 
   /**
   * Create and register {@link SolrMetricReporter}-s specific to a {@link org.apache.solr.core.SolrInfoMBean.Group}.
   * Create and register {@link SolrMetricReporter}-s specific to a {@link org.apache.solr.core.SolrInfoBean.Group}.
    * Note: reporters that specify neither "group" nor "registry" attributes are treated as universal -
    * they will always be loaded for any group. These two attributes may also contain multiple comma- or
    * whitespace-separated values, in which case the reporter will be loaded for any matching value from
@@ -634,7 +728,7 @@ public class SolrMetricManager {
    * @param group selected group, not null
    * @param registryNames optional child registry name elements
    */
  public void loadReporters(PluginInfo[] pluginInfos, SolrResourceLoader loader, String tag, SolrInfoMBean.Group group, String... registryNames) {
  public void loadReporters(PluginInfo[] pluginInfos, SolrResourceLoader loader, String tag, SolrInfoBean.Group group, String... registryNames) {
     if (pluginInfos == null || pluginInfos.length == 0) {
       return;
     }
@@ -941,13 +1035,13 @@ public class SolrMetricManager {
     // prepare default plugin if none present in the config
     Map<String, String> attrs = new HashMap<>();
     attrs.put("name", "shardDefault");
    attrs.put("group", SolrInfoMBean.Group.shard.toString());
    attrs.put("group", SolrInfoBean.Group.shard.toString());
     Map<String, Object> initArgs = new HashMap<>();
     initArgs.put("period", DEFAULT_CLOUD_REPORTER_PERIOD);
 
     String registryName = core.getCoreMetricManager().getRegistryName();
     // collect infos and normalize
    List<PluginInfo> infos = prepareCloudPlugins(pluginInfos, SolrInfoMBean.Group.shard.toString(), SolrShardReporter.class.getName(),
    List<PluginInfo> infos = prepareCloudPlugins(pluginInfos, SolrInfoBean.Group.shard.toString(), SolrShardReporter.class.getName(),
         attrs, initArgs, null);
     for (PluginInfo info : infos) {
       try {
@@ -967,12 +1061,12 @@ public class SolrMetricManager {
     }
     Map<String, String> attrs = new HashMap<>();
     attrs.put("name", "clusterDefault");
    attrs.put("group", SolrInfoMBean.Group.cluster.toString());
    attrs.put("group", SolrInfoBean.Group.cluster.toString());
     Map<String, Object> initArgs = new HashMap<>();
     initArgs.put("period", DEFAULT_CLOUD_REPORTER_PERIOD);
    List<PluginInfo> infos = prepareCloudPlugins(pluginInfos, SolrInfoMBean.Group.cluster.toString(), SolrClusterReporter.class.getName(),
    List<PluginInfo> infos = prepareCloudPlugins(pluginInfos, SolrInfoBean.Group.cluster.toString(), SolrClusterReporter.class.getName(),
         attrs, initArgs, null);
    String registryName = getRegistryName(SolrInfoMBean.Group.cluster);
    String registryName = getRegistryName(SolrInfoBean.Group.cluster);
     for (PluginInfo info : infos) {
       try {
         SolrMetricReporter reporter = loadReporter(registryName, cc.getResourceLoader(), info, null);
diff --git a/solr/core/src/java/org/apache/solr/metrics/SolrMetricReporter.java b/solr/core/src/java/org/apache/solr/metrics/SolrMetricReporter.java
index ff2d3fcbdc6..9ad15d0168d 100644
-- a/solr/core/src/java/org/apache/solr/metrics/SolrMetricReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/SolrMetricReporter.java
@@ -30,6 +30,7 @@ public abstract class SolrMetricReporter implements Closeable, PluginInfoInitial
   protected final String registryName;
   protected final SolrMetricManager metricManager;
   protected PluginInfo pluginInfo;
  protected boolean enabled = true;
 
   /**
    * Create a reporter for metrics managed in a named registry.
@@ -57,6 +58,17 @@ public abstract class SolrMetricReporter implements Closeable, PluginInfoInitial
     validate();
   }
 
  /**
   * Enable reporting, defaults to true. Implementations should check this flag in
   * {@link #validate()} and accordingly enable or disable reporting.
   * @param enabled enable, defaults to true when null or not set.
   */
  public void setEnabled(Boolean enabled) {
    if (enabled != null) {
      this.enabled = enabled;
    }
  }

   /**
    * Get the effective {@link PluginInfo} instance that was used for
    * initialization of this plugin.
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/JmxObjectNameFactory.java b/solr/core/src/java/org/apache/solr/metrics/reporters/JmxObjectNameFactory.java
index 1f5b4f01513..4298c1842da 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/JmxObjectNameFactory.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/JmxObjectNameFactory.java
@@ -50,6 +50,20 @@ public class JmxObjectNameFactory implements ObjectNameFactory {
     this.props = additionalProperties;
   }
 
  /**
   * Return current domain.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Return current reporterName.
   */
  public String getReporterName() {
    return reporterName;
  }

   /**
    * Create a hierarchical name.
    *
@@ -60,7 +74,8 @@ public class JmxObjectNameFactory implements ObjectNameFactory {
   @Override
   public ObjectName createName(String type, String currentDomain, String name) {
     SolrMetricInfo metricInfo = SolrMetricInfo.of(name);

    String safeName = metricInfo != null ? metricInfo.name : name;
    safeName = safeName.replaceAll(":", "_");
     // It turns out that ObjectName(String) mostly preserves key ordering
     // as specified in the constructor (except for the 'type' key that ends
     // up at top level) - unlike ObjectName(String, Map) constructor
@@ -90,24 +105,42 @@ public class JmxObjectNameFactory implements ObjectNameFactory {
       sb.append(currentDomain);
       sb.append(':');
     }
    sb.append("reporter=");
    sb.append(reporterName);
    sb.append(',');
    if (props != null && props.length > 0) {
      boolean added = false;
      for (int i = 0; i < props.length; i += 2) {
        if (props[i] == null || props[i].isEmpty()) {
          continue;
        }
        if (props[i + 1] == null || props[i + 1].isEmpty()) {
          continue;
        }
        sb.append(',');
        sb.append(props[i]);
        sb.append('=');
        sb.append(props[i + 1]);
        added = true;
      }
      if (added) {
        sb.append(',');
      }
    }
     if (metricInfo != null) {
       sb.append("category=");
       sb.append(metricInfo.category.toString());
      sb.append(",scope=");
      sb.append(metricInfo.scope);
      if (metricInfo.scope != null) {
        sb.append(",scope=");
        sb.append(metricInfo.scope);
      }
       // we could also split by type, but don't call it 'type' :)
       // if (type != null) {
       //   sb.append(",class=");
       //   sb.append(type);
       // }
       sb.append(",name=");
      sb.append(metricInfo.name);
      sb.append(safeName);
     } else {
       // make dotted names into hierarchies
      String[] path = name.split("\\.");
      String[] path = safeName.split("\\.");
       for (int i = 0; i < path.length - 1; i++) {
         if (i > 0) {
           sb.append(',');
@@ -127,20 +160,6 @@ public class JmxObjectNameFactory implements ObjectNameFactory {
       sb.append("name=");
       sb.append(path[path.length - 1]);
     }
    if (props != null && props.length > 0) {
      for (int i = 0; i < props.length; i += 2) {
        if (props[i] == null || props[i].isEmpty()) {
          continue;
        }
        if (props[i + 1] == null || props[i + 1].isEmpty()) {
          continue;
        }
        sb.append(',');
        sb.append(props[i]);
        sb.append('=');
        sb.append(props[i + 1]);
      }
    }
 
     ObjectName objectName;
 
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/ReporterClientCache.java b/solr/core/src/java/org/apache/solr/metrics/reporters/ReporterClientCache.java
new file mode 100644
index 00000000000..5745dec1738
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/ReporterClientCache.java
@@ -0,0 +1,84 @@
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
package org.apache.solr.metrics.reporters;

import java.io.Closeable;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple cache for reusable service clients used by some implementations of
 * {@link org.apache.solr.metrics.SolrMetricReporter}.
 */
public class ReporterClientCache<T> implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, T> cache = new ConcurrentHashMap<>();

  /**
   * Provide an instance of service client.
   * @param <T> formal type
   */
  public interface ClientProvider<T> {
    /**
     * Get an instance of a service client. It's not specified that each time this
     * method is invoked a new client instance should be returned.
     * @return client instance
     * @throws Exception when client creation encountered an error.
     */
    T get() throws Exception;
  }

  /**
   * Get existing or register a new client.
   * @param id client id
   * @param clientProvider provider of new client instances
   */
  public synchronized T getOrCreate(String id, ClientProvider<T> clientProvider) {
    T item = cache.get(id);
    if (item == null) {
      try {
        item = clientProvider.get();
        cache.put(id, item);
      } catch (Exception e) {
        LOG.warn("Error providing a new client for id=" + id, e);
        item = null;
      }
    }
    return item;
  }

  /**
   * Empty this cache, and close all clients that are {@link Closeable}.
   */
  public void close() {
    for (T client : cache.values()) {
      if (client instanceof Closeable) {
        try {
          ((Closeable)client).close();
        } catch (Exception e) {
          LOG.warn("Error closing client " + client + ", ignoring...", e);
        }
      }
    }
    cache.clear();
  }
}
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGangliaReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGangliaReporter.java
index 45561e58b58..142ddd884e6 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGangliaReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGangliaReporter.java
@@ -17,6 +17,9 @@
 package org.apache.solr.metrics.reporters;
 
 import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
 import java.util.concurrent.TimeUnit;
 
 import com.codahale.metrics.MetricFilter;
@@ -24,21 +27,26 @@ import com.codahale.metrics.ganglia.GangliaReporter;
 import info.ganglia.gmetric4j.gmetric.GMetric;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  *
  */
 public class SolrGangliaReporter extends SolrMetricReporter {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
   private String host = null;
   private int port = -1;
   private boolean multicast;
   private int period = 60;
   private String instancePrefix = null;
  private String filterPrefix = null;
  private List<String> filters = new ArrayList<>();
   private boolean testing;
   private GangliaReporter reporter;
 
  private static final ReporterClientCache<GMetric> serviceRegistry = new ReporterClientCache<>();

   // for unit tests
   GMetric ganglia = null;
 
@@ -65,10 +73,24 @@ public class SolrGangliaReporter extends SolrMetricReporter {
     this.instancePrefix = prefix;
   }
 
  public void setFilter(String filter) {
    this.filterPrefix = filter;
  /**
   * Report only metrics with names matching any of the prefix filters.
   * @param filters list of 0 or more prefixes. If the list is empty then
   *                all names will match.
   */
  public void setFilter(List<String> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    this.filters.addAll(filters);
   }
 
  // due to vagaries of SolrPluginUtils.invokeSetters we need this too
  public void setFilter(String filter) {
    if (filter != null && !filter.isEmpty()) {
      this.filters.add(filter);
    }
  }
 
   public void setPeriod(int period) {
     this.period = period;
@@ -89,6 +111,10 @@ public class SolrGangliaReporter extends SolrMetricReporter {
 
   @Override
   protected void validate() throws IllegalStateException {
    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
     if (host == null) {
       throw new IllegalStateException("Init argument 'host' must be set to a valid Ganglia server name.");
     }
@@ -106,12 +132,12 @@ public class SolrGangliaReporter extends SolrMetricReporter {
   //this is a separate method for unit tests
   void start() {
     if (!testing) {
      try {
        ganglia = new GMetric(host, port,
            multicast ? GMetric.UDPAddressingMode.MULTICAST : GMetric.UDPAddressingMode.UNICAST,
            1);
      } catch (IOException ioe) {
        throw new IllegalStateException("Exception connecting to Ganglia", ioe);
      String id = host + ":" + port + ":" + multicast;
      ganglia = serviceRegistry.getOrCreate(id, () -> new GMetric(host, port,
          multicast ? GMetric.UDPAddressingMode.MULTICAST : GMetric.UDPAddressingMode.UNICAST,
          1));
      if (ganglia == null) {
        return;
       }
     }
     if (instancePrefix == null) {
@@ -125,8 +151,8 @@ public class SolrGangliaReporter extends SolrMetricReporter {
         .convertDurationsTo(TimeUnit.MILLISECONDS)
         .prefixedWith(instancePrefix);
     MetricFilter filter;
    if (filterPrefix != null) {
      filter = new SolrMetricManager.PrefixFilter(filterPrefix);
    if (!filters.isEmpty()) {
      filter = new SolrMetricManager.PrefixFilter(filters);
     } else {
       filter = MetricFilter.ALL;
     }
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGraphiteReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGraphiteReporter.java
index 8565ce86c05..d5b7a203ab8 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGraphiteReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrGraphiteReporter.java
@@ -18,6 +18,8 @@ package org.apache.solr.metrics.reporters;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
 import java.util.concurrent.TimeUnit;
 
 import com.codahale.metrics.MetricFilter;
@@ -41,9 +43,11 @@ public class SolrGraphiteReporter extends SolrMetricReporter {
   private int period = 60;
   private boolean pickled = false;
   private String instancePrefix = null;
  private String filterPrefix = null;
  private List<String> filters = new ArrayList<>();
   private GraphiteReporter reporter = null;
 
  private static final ReporterClientCache<GraphiteSender> serviceRegistry = new ReporterClientCache<>();

   /**
    * Create a Graphite reporter for metrics managed in a named registry.
    *
@@ -67,10 +71,25 @@ public class SolrGraphiteReporter extends SolrMetricReporter {
     this.instancePrefix = prefix;
   }
 
  /**
   * Report only metrics with names matching any of the prefix filters.
   * @param filters list of 0 or more prefixes. If the list is empty then
   *                all names will match.
   */
  public void setFilter(List<String> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    this.filters.addAll(filters);
  }

   public void setFilter(String filter) {
    this.filterPrefix = filter;
    if (filter != null && !filter.isEmpty()) {
      this.filters.add(filter);
    }
   }
 

   public void setPickled(boolean pickled) {
     this.pickled = pickled;
   }
@@ -81,6 +100,10 @@ public class SolrGraphiteReporter extends SolrMetricReporter {
 
   @Override
   protected void validate() throws IllegalStateException {
    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
     if (host == null) {
       throw new IllegalStateException("Init argument 'host' must be set to a valid Graphite server name.");
     }
@@ -93,12 +116,15 @@ public class SolrGraphiteReporter extends SolrMetricReporter {
     if (period < 1) {
       throw new IllegalStateException("Init argument 'period' is in time unit 'seconds' and must be at least 1.");
     }
    final GraphiteSender graphite;
    if (pickled) {
      graphite = new PickledGraphite(host, port);
    } else {
      graphite = new Graphite(host, port);
    }
    GraphiteSender graphite;
    String id = host + ":" + port + ":" + pickled;
    graphite = serviceRegistry.getOrCreate(id, () -> {
      if (pickled) {
        return new PickledGraphite(host, port);
      } else {
        return new Graphite(host, port);
      }
    });
     if (instancePrefix == null) {
       instancePrefix = registryName;
     } else {
@@ -110,8 +136,8 @@ public class SolrGraphiteReporter extends SolrMetricReporter {
         .convertRatesTo(TimeUnit.SECONDS)
         .convertDurationsTo(TimeUnit.MILLISECONDS);
     MetricFilter filter;
    if (filterPrefix != null) {
      filter = new SolrMetricManager.PrefixFilter(filterPrefix);
    if (!filters.isEmpty()) {
      filter = new SolrMetricManager.PrefixFilter(filters);
     } else {
       filter = MetricFilter.ALL;
     }
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrJmxReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrJmxReporter.java
index 0e78eee038a..d09e0437214 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrJmxReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrJmxReporter.java
@@ -16,15 +16,25 @@
  */
 package org.apache.solr.metrics.reporters;
 
import javax.management.InstanceNotFoundException;
 import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
 
import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
 import java.util.Locale;
import java.util.Set;
 
import com.codahale.metrics.Gauge;
 import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricReporter;
 import org.apache.solr.util.JmxUtil;
@@ -34,17 +44,25 @@ import org.slf4j.LoggerFactory;
 /**
  * A {@link SolrMetricReporter} that finds (or creates) a MBeanServer from
  * the given configuration and registers metrics to it with JMX.
 * <p>NOTE: {@link JmxReporter} that this class uses exports only newly added metrics (it doesn't
 * process already existing metrics in a registry)</p>
  */
 public class SolrJmxReporter extends SolrMetricReporter {
 
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
  private static final ReporterClientCache<MBeanServer> serviceRegistry = new ReporterClientCache<>();

   private String domain;
   private String agentId;
   private String serviceUrl;
  private String rootName;
  private List<String> filters = new ArrayList<>();
 
   private JmxReporter reporter;
  private MetricRegistry registry;
   private MBeanServer mBeanServer;
  private MetricsMapListener listener;
 
   /**
    * Creates a new instance of {@link SolrJmxReporter}.
@@ -57,7 +75,7 @@ public class SolrJmxReporter extends SolrMetricReporter {
   }
 
   /**
   * Initializes the reporter by finding (or creating) a MBeanServer
   * Initializes the reporter by finding an MBeanServer
    * and registering the metricManager's metric registry.
    *
    * @param pluginInfo the configuration for the reporter
@@ -65,44 +83,62 @@ public class SolrJmxReporter extends SolrMetricReporter {
   @Override
   public synchronized void init(PluginInfo pluginInfo) {
     super.init(pluginInfo);

    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
    log.debug("Initializing for registry " + registryName);
     if (serviceUrl != null && agentId != null) {
      ManagementFactory.getPlatformMBeanServer(); // Ensure at least one MBeanServer is available.
       mBeanServer = JmxUtil.findFirstMBeanServer();
      log.warn("No more than one of serviceUrl(%s) and agentId(%s) should be configured, using first MBeanServer instead of configuration.",
      log.warn("No more than one of serviceUrl({}) and agentId({}) should be configured, using first MBeanServer instead of configuration.",
           serviceUrl, agentId, mBeanServer);
    }
    else if (serviceUrl != null) {
      try {
        mBeanServer = JmxUtil.findMBeanServerForServiceUrl(serviceUrl);
      } catch (IOException e) {
        log.warn("findMBeanServerForServiceUrl(%s) exception: %s", serviceUrl, e);
        mBeanServer = null;
      }
    }
    else if (agentId != null) {
    } else if (serviceUrl != null) {
      // reuse existing services
      mBeanServer = serviceRegistry.getOrCreate(serviceUrl, () -> JmxUtil.findMBeanServerForServiceUrl(serviceUrl));
    } else if (agentId != null) {
       mBeanServer = JmxUtil.findMBeanServerForAgentId(agentId);
     } else {
      ManagementFactory.getPlatformMBeanServer(); // Ensure at least one MBeanServer is available.
       mBeanServer = JmxUtil.findFirstMBeanServer();
      log.warn("No serviceUrl or agentId was configured, using first MBeanServer.", mBeanServer);
      log.debug("No serviceUrl or agentId was configured, using first MBeanServer: " + mBeanServer);
     }
 
     if (mBeanServer == null) {
      log.warn("No JMX server found. Not exposing Solr metrics.");
      log.warn("No JMX server found. Not exposing Solr metrics via JMX.");
       return;
     }
 
    JmxObjectNameFactory jmxObjectNameFactory = new JmxObjectNameFactory(pluginInfo.name, domain);
    if (domain == null || domain.isEmpty()) {
      domain = registryName;
    }
    String fullDomain = domain;
    if (rootName != null && !rootName.isEmpty()) {
      fullDomain = rootName + "." + domain;
    }
    JmxObjectNameFactory jmxObjectNameFactory = new JmxObjectNameFactory(pluginInfo.name, fullDomain);
    registry = metricManager.registry(registryName);
    // filter out MetricsMap gauges - we have a better way of handling them
    MetricFilter mmFilter = (name, metric) -> !(metric instanceof MetricsMap);
    MetricFilter filter;
    if (filters.isEmpty()) {
      filter = mmFilter;
    } else {
      // apply also prefix filters
      SolrMetricManager.PrefixFilter prefixFilter = new SolrMetricManager.PrefixFilter(filters);
      filter = new SolrMetricManager.AndFilter(prefixFilter, mmFilter);
    }
 
    reporter = JmxReporter.forRegistry(metricManager.registry(registryName))
    reporter = JmxReporter.forRegistry(registry)
                           .registerWith(mBeanServer)
                          .inDomain(domain)
                          .inDomain(fullDomain)
                          .filter(filter)
                           .createsObjectNamesWith(jmxObjectNameFactory)
                           .build();
     reporter.start();
    // workaround for inability to register custom MBeans (to be available in metrics 4.0?)
    listener = new MetricsMapListener(mBeanServer, jmxObjectNameFactory);
    registry.addListener(listener);
 
    log.info("JMX monitoring enabled at server: " + mBeanServer);
    log.info("JMX monitoring for '" + fullDomain + "' (registry '" + registryName + "') enabled at server: " + mBeanServer);
   }
 
   /**
@@ -114,6 +150,11 @@ public class SolrJmxReporter extends SolrMetricReporter {
       reporter.close();
       reporter = null;
     }
    if (listener != null && registry != null) {
      registry.removeListener(listener);
      listener.close();
      listener = null;
    }
   }
 
   /**
@@ -127,9 +168,19 @@ public class SolrJmxReporter extends SolrMetricReporter {
     // Nothing to validate
   }
 

  /**
   * Set root name of the JMX hierarchy for this reporter. Default (null or empty) is none, ie.
   * the hierarchy will start from the domain name.
   * @param rootName root name of the JMX name hierarchy, or null or empty for default.
   */
  public void setRootName(String rootName) {
    this.rootName = rootName;
  }

   /**
    * Sets the domain with which MBeans are published. If none is set,
   * the domain defaults to the name of the core.
   * the domain defaults to the name of the registry.
    *
    * @param domain the domain
    */
@@ -162,7 +213,46 @@ public class SolrJmxReporter extends SolrMetricReporter {
   }
 
   /**
   * Retrieves the reporter's MBeanServer.
   * Return configured agentId or null.
   */
  public String getAgentId() {
    return agentId;
  }

  /**
   * Return configured serviceUrl or null.
   */
  public String getServiceUrl() {
    return serviceUrl;
  }

  /**
   * Return configured domain or null.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Report only metrics with names matching any of the prefix filters.
   * @param filters list of 0 or more prefixes. If the list is empty then
   *                all names will match.
   */
  public void setFilter(List<String> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    this.filters.addAll(filters);
  }

  public void setFilter(String filter) {
    if (filter != null && !filter.isEmpty()) {
      this.filters.add(filter);
    }
  }

  /**
   * Return the reporter's MBeanServer.
    *
    * @return the reporter's MBeanServer
    */
@@ -170,10 +260,72 @@ public class SolrJmxReporter extends SolrMetricReporter {
     return mBeanServer;
   }
 
  /**
   * For unit tests.
   * @return true if this reporter is actively reporting metrics to JMX.
   */
  public boolean isActive() {
    return reporter != null;
  }

   @Override
   public String toString() {
    return String.format(Locale.ENGLISH, "[%s@%s: domain = %s, service url = %s, agent id = %s]",
        getClass().getName(), Integer.toHexString(hashCode()), domain, serviceUrl, agentId);
    return String.format(Locale.ENGLISH, "[%s@%s: rootName = %s, domain = %s, service url = %s, agent id = %s]",
        getClass().getName(), Integer.toHexString(hashCode()), rootName, domain, serviceUrl, agentId);
   }
 
  private static class MetricsMapListener extends MetricRegistryListener.Base {
    MBeanServer server;
    JmxObjectNameFactory nameFactory;
    // keep the names so that we can unregister them on core close
    Set<ObjectName> registered = new HashSet<>();

    MetricsMapListener(MBeanServer server, JmxObjectNameFactory nameFactory) {
      this.server = server;
      this.nameFactory = nameFactory;
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
      if (!(gauge instanceof MetricsMap)) {
        return;
      }
      synchronized (server) {
        try {
          ObjectName objectName = nameFactory.createName("gauges", nameFactory.getDomain(), name);
          log.debug("REGISTER " + objectName);
          if (registered.contains(objectName) || server.isRegistered(objectName)) {
            log.debug("-unregistering old instance of " + objectName);
            try {
              server.unregisterMBean(objectName);
            } catch (InstanceNotFoundException e) {
              // ignore
            }
          }
          // some MBean servers re-write object name to include additional properties
          ObjectInstance instance = server.registerMBean(gauge, objectName);
          if (instance != null) {
            registered.add(instance.getObjectName());
          }
        } catch (Exception e) {
          log.warn("bean registration error", e);
        }
      }
    }

    public void close() {
      synchronized (server) {
        for (ObjectName name : registered) {
          try {
            if (server.isRegistered(name)) {
              server.unregisterMBean(name);
            }
          } catch (Exception e) {
            log.debug("bean unregistration error", e);
          }
        }
        registered.clear();
      }
    }
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrSlf4jReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrSlf4jReporter.java
index 817dda17f94..8b7c35e88e4 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/SolrSlf4jReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/SolrSlf4jReporter.java
@@ -18,6 +18,8 @@ package org.apache.solr.metrics.reporters;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
 import java.util.concurrent.TimeUnit;
 
 import com.codahale.metrics.MetricFilter;
@@ -47,7 +49,7 @@ public class SolrSlf4jReporter extends SolrMetricReporter {
   private int period = 60;
   private String instancePrefix = null;
   private String logger = null;
  private String filterPrefix = null;
  private List<String> filters = new ArrayList<>();
   private Slf4jReporter reporter;
 
   /**
@@ -65,10 +67,25 @@ public class SolrSlf4jReporter extends SolrMetricReporter {
     this.instancePrefix = prefix;
   }
 
  /**
   * Report only metrics with names matching any of the prefix filters.
   * @param filters list of 0 or more prefixes. If the list is empty then
   *                all names will match.
   */
  public void setFilter(List<String> filters) {
    if (filters == null || filters.isEmpty()) {
      return;
    }
    this.filters.addAll(filters);
  }

   public void setFilter(String filter) {
    this.filterPrefix = filter;
    if (filter != null && !filter.isEmpty()) {
      this.filters.add(filter);
    }
   }
 

   public void setLogger(String logger) {
     this.logger = logger;
   }
@@ -79,6 +96,10 @@ public class SolrSlf4jReporter extends SolrMetricReporter {
 
   @Override
   protected void validate() throws IllegalStateException {
    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
     if (period < 1) {
       throw new IllegalStateException("Init argument 'period' is in time unit 'seconds' and must be at least 1.");
     }
@@ -93,8 +114,8 @@ public class SolrSlf4jReporter extends SolrMetricReporter {
         .convertDurationsTo(TimeUnit.MILLISECONDS);
 
     MetricFilter filter;
    if (filterPrefix != null) {
      filter = new SolrMetricManager.PrefixFilter(filterPrefix);
    if (!filters.isEmpty()) {
      filter = new SolrMetricManager.PrefixFilter(filters);
     } else {
       filter = MetricFilter.ALL;
     }
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrClusterReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrClusterReporter.java
index a34accd82aa..c4374570b23 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrClusterReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrClusterReporter.java
@@ -33,7 +33,7 @@ import org.apache.solr.cloud.ZkController;
 import org.apache.solr.common.cloud.SolrZkClient;
 import org.apache.solr.common.cloud.ZkNodeProps;
 import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.handler.admin.MetricsCollectorHandler;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricReporter;
@@ -92,14 +92,14 @@ import static org.apache.solr.common.params.CommonParams.ID;
 public class SolrClusterReporter extends SolrMetricReporter {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
  public static final String CLUSTER_GROUP = SolrMetricManager.overridableRegistryName(SolrInfoMBean.Group.cluster.toString());
  public static final String CLUSTER_GROUP = SolrMetricManager.overridableRegistryName(SolrInfoBean.Group.cluster.toString());
 
   public static final List<SolrReporter.Report> DEFAULT_REPORTS = new ArrayList<SolrReporter.Report>() {{
     add(new SolrReporter.Report(CLUSTER_GROUP, "jetty",
        SolrMetricManager.overridableRegistryName(SolrInfoMBean.Group.jetty.toString()),
        SolrMetricManager.overridableRegistryName(SolrInfoBean.Group.jetty.toString()),
         Collections.emptySet())); // all metrics
     add(new SolrReporter.Report(CLUSTER_GROUP, "jvm",
        SolrMetricManager.overridableRegistryName(SolrInfoMBean.Group.jvm.toString()),
        SolrMetricManager.overridableRegistryName(SolrInfoBean.Group.jvm.toString()),
         new HashSet<String>() {{
           add("memory\\.total\\..*");
           add("memory\\.heap\\..*");
@@ -109,7 +109,7 @@ public class SolrClusterReporter extends SolrMetricReporter {
           add("os\\.OpenFileDescriptorCount");
           add("threads\\.count");
         }}));
    add(new SolrReporter.Report(CLUSTER_GROUP, "node", SolrMetricManager.overridableRegistryName(SolrInfoMBean.Group.node.toString()),
    add(new SolrReporter.Report(CLUSTER_GROUP, "node", SolrMetricManager.overridableRegistryName(SolrInfoBean.Group.node.toString()),
         new HashSet<String>() {{
           add("CONTAINER\\.cores\\..*");
           add("CONTAINER\\.fs\\..*");
@@ -159,6 +159,16 @@ public class SolrClusterReporter extends SolrMetricReporter {
     });
   }
 
  public void setReport(Map map) {
    if (map == null || map.isEmpty()) {
      return;
    }
    SolrReporter.Report r = SolrReporter.Report.fromMap(map);
    if (r != null) {
      reports.add(r);
    }
  }

   // for unit tests
   int getPeriod() {
     return period;
@@ -170,9 +180,6 @@ public class SolrClusterReporter extends SolrMetricReporter {
 
   @Override
   protected void validate() throws IllegalStateException {
    if (period < 1) {
      log.info("Turning off node reporter, period=" + period);
    }
     if (reports.isEmpty()) { // set defaults
       reports = DEFAULT_REPORTS;
     }
@@ -189,12 +196,17 @@ public class SolrClusterReporter extends SolrMetricReporter {
     if (reporter != null) {
       reporter.close();;
     }
    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
     // start reporter only in cloud mode
     if (!cc.isZooKeeperAware()) {
       log.warn("Not ZK-aware, not starting...");
       return;
     }
     if (period < 1) { // don't start it
      log.info("Turning off node reporter, period=" + period);
       return;
     }
     HttpClient httpClient = cc.getUpdateShardHandler().getHttpClient();
diff --git a/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrShardReporter.java b/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrShardReporter.java
index 8b36d3e0c96..b36c59679b7 100644
-- a/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrShardReporter.java
++ b/solr/core/src/java/org/apache/solr/metrics/reporters/solr/SolrShardReporter.java
@@ -98,7 +98,13 @@ public class SolrShardReporter extends SolrMetricReporter {
     if (filterConfig == null || filterConfig.isEmpty()) {
       return;
     }
    filters = filterConfig;
    filters.addAll(filterConfig);
  }

  public void setFilter(String filter) {
    if (filter != null && !filter.isEmpty()) {
      this.filters.add(filter);
    }
   }
 
   // for unit tests
@@ -108,9 +114,6 @@ public class SolrShardReporter extends SolrMetricReporter {
 
   @Override
   protected void validate() throws IllegalStateException {
    if (period < 1) {
      log.info("Turning off shard reporter, period=" + period);
    }
     if (filters.isEmpty()) {
       filters = DEFAULT_FILTERS;
     }
@@ -128,13 +131,17 @@ public class SolrShardReporter extends SolrMetricReporter {
     if (reporter != null) {
       reporter.close();
     }
    if (!enabled) {
      log.info("Reporter disabled for registry " + registryName);
      return;
    }
     if (core.getCoreDescriptor().getCloudDescriptor() == null) {
       // not a cloud core
       log.warn("Not initializing shard reporter for non-cloud core " + core.getName());
       return;
     }
     if (period < 1) { // don't start it
      log.warn("Not starting shard reporter ");
      log.warn("period=" + period + ", not starting shard reporter ");
       return;
     }
     // our id is coreNodeName
diff --git a/solr/core/src/java/org/apache/solr/request/SolrRequestHandler.java b/solr/core/src/java/org/apache/solr/request/SolrRequestHandler.java
index 82ce2e0fbeb..8350f9ed1c0 100644
-- a/solr/core/src/java/org/apache/solr/request/SolrRequestHandler.java
++ b/solr/core/src/java/org/apache/solr/request/SolrRequestHandler.java
@@ -17,7 +17,7 @@
 package org.apache.solr.request;
 
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.response.SolrQueryResponse;
 
 /**
@@ -38,7 +38,7 @@ import org.apache.solr.response.SolrQueryResponse;
  *
  *
  */
public interface SolrRequestHandler extends SolrInfoMBean {
public interface SolrRequestHandler extends SolrInfoBean {
 
   /** <code>init</code> will be called just once, immediately after creation.
    * <p>The args are user-level initialization parameters that
diff --git a/solr/core/src/java/org/apache/solr/search/FastLRUCache.java b/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
index 9c4b8920aab..cb699b25abc 100644
-- a/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
++ b/solr/core/src/java/org/apache/solr/search/FastLRUCache.java
@@ -15,15 +15,17 @@
  * limitations under the License.
  */
 package org.apache.solr.search;

import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.common.SolrException;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.util.ConcurrentLRUCache;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 
import java.io.Serializable;
 import java.lang.invoke.MethodHandles;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
@@ -57,6 +59,10 @@ public class FastLRUCache<K, V> extends SolrCacheBase implements SolrCache<K,V>
 
   private long maxRamBytes;
 
  private MetricsMap cacheMap;
  private Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;

   @Override
   public Object init(Map args, Object persistence, CacheRegenerator regenerator) {
     super.init(args, regenerator);
@@ -215,68 +221,80 @@ public class FastLRUCache<K, V> extends SolrCacheBase implements SolrCache<K,V>
   }
 
   @Override
  public String getSource() {
    return null;
  public Set<String> getMetricNames() {
    return metricNames;
   }
 

   @Override
  public NamedList getStatistics() {
    NamedList<Serializable> lst = new SimpleOrderedMap<>();
    if (cache == null)  return lst;
    ConcurrentLRUCache.Stats stats = cache.getStats();
    long lookups = stats.getCumulativeLookups();
    long hits = stats.getCumulativeHits();
    long inserts = stats.getCumulativePuts();
    long evictions = stats.getCumulativeEvictions();
    long size = stats.getCurrentSize();
    long clookups = 0;
    long chits = 0;
    long cinserts = 0;
    long cevictions = 0;

    // NOTE: It is safe to iterate on a CopyOnWriteArrayList
    for (ConcurrentLRUCache.Stats statistiscs : statsList) {
      clookups += statistiscs.getCumulativeLookups();
      chits += statistiscs.getCumulativeHits();
      cinserts += statistiscs.getCumulativePuts();
      cevictions += statistiscs.getCumulativeEvictions();
    }
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    cacheMap = new MetricsMap((detailed, map) -> {
      if (cache != null) {
        ConcurrentLRUCache.Stats stats = cache.getStats();
        long lookups = stats.getCumulativeLookups();
        long hits = stats.getCumulativeHits();
        long inserts = stats.getCumulativePuts();
        long evictions = stats.getCumulativeEvictions();
        long size = stats.getCurrentSize();
        long clookups = 0;
        long chits = 0;
        long cinserts = 0;
        long cevictions = 0;

        // NOTE: It is safe to iterate on a CopyOnWriteArrayList
        for (ConcurrentLRUCache.Stats statistiscs : statsList) {
          clookups += statistiscs.getCumulativeLookups();
          chits += statistiscs.getCumulativeHits();
          cinserts += statistiscs.getCumulativePuts();
          cevictions += statistiscs.getCumulativeEvictions();
        }

        map.put("lookups", lookups);
        map.put("hits", hits);
        map.put("hitratio", calcHitRatio(lookups, hits));
        map.put("inserts", inserts);
        map.put("evictions", evictions);
        map.put("size", size);

        map.put("warmupTime", warmupTime);
        map.put("cumulative_lookups", clookups);
        map.put("cumulative_hits", chits);
        map.put("cumulative_hitratio", calcHitRatio(clookups, chits));
        map.put("cumulative_inserts", cinserts);
        map.put("cumulative_evictions", cevictions);

        if (detailed && showItems != 0) {
          Map items = cache.getLatestAccessedItems( showItems == -1 ? Integer.MAX_VALUE : showItems );
          for (Map.Entry e : (Set <Map.Entry>)items.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();

            String ks = "item_" + k;
            String vs = v.toString();
            map.put(ks,vs);
          }
 
    lst.add("lookups", lookups);
    lst.add("hits", hits);
    lst.add("hitratio", calcHitRatio(lookups, hits));
    lst.add("inserts", inserts);
    lst.add("evictions", evictions);
    lst.add("size", size);

    lst.add("warmupTime", warmupTime);
    lst.add("cumulative_lookups", clookups);
    lst.add("cumulative_hits", chits);
    lst.add("cumulative_hitratio", calcHitRatio(clookups, chits));
    lst.add("cumulative_inserts", cinserts);
    lst.add("cumulative_evictions", cevictions);

    if (showItems != 0) {
      Map items = cache.getLatestAccessedItems( showItems == -1 ? Integer.MAX_VALUE : showItems );
      for (Map.Entry e : (Set <Map.Entry>)items.entrySet()) {
        Object k = e.getKey();
        Object v = e.getValue();

        String ks = "item_" + k;
        String vs = v.toString();
        lst.add(ks,vs);
        }
       }
      
    }
    });
    manager.registerGauge(this, registryName, cacheMap, true, scope, getCategory().toString());
  }

  // for unit tests only
  MetricsMap getMetricsMap() {
    return cacheMap;
  }
 
    return lst;
  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   @Override
   public String toString() {
    return name() + getStatistics().toString();
    return name() + cacheMap != null ? cacheMap.getValue().toString() : "";
   }

 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/search/LFUCache.java b/solr/core/src/java/org/apache/solr/search/LFUCache.java
index 2b593c6f57a..82ba6d26536 100644
-- a/solr/core/src/java/org/apache/solr/search/LFUCache.java
++ b/solr/core/src/java/org/apache/solr/search/LFUCache.java
@@ -15,19 +15,19 @@
  * limitations under the License.
  */
 package org.apache.solr.search;
import java.io.Serializable;

 import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.TimeUnit;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.util.ConcurrentLFUCache;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -64,6 +64,9 @@ public class LFUCache<K, V> implements SolrCache<K, V> {
   private ConcurrentLFUCache<K, V> cache;
   private int showItems = 0;
   private Boolean timeDecay = true;
  private MetricsMap cacheMap;
  private Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;
 
   @Override
   public Object init(Map args, Object persistence, CacheRegenerator regenerator) {
@@ -211,11 +214,6 @@ public class LFUCache<K, V> implements SolrCache<K, V> {
     return LFUCache.class.getName();
   }
 
  @Override
  public String getVersion() {
    return SolrCore.version;
  }

   @Override
   public String getDescription() {
     return description;
@@ -226,16 +224,6 @@ public class LFUCache<K, V> implements SolrCache<K, V> {
     return Category.CACHE;
   }
 
  @Override
  public String getSource() {
    return null;
  }

  @Override
  public URL[] getDocs() {
    return null;
  }

   // returns a ratio, not a percent.
   private static String calcHitRatio(long lookups, long hits) {
     if (lookups == 0) return "0.00";
@@ -246,62 +234,81 @@ public class LFUCache<K, V> implements SolrCache<K, V> {
   }
 
   @Override
  public NamedList getStatistics() {
    NamedList<Serializable> lst = new SimpleOrderedMap<>();
    if (cache == null) return lst;
    ConcurrentLFUCache.Stats stats = cache.getStats();
    long lookups = stats.getCumulativeLookups();
    long hits = stats.getCumulativeHits();
    long inserts = stats.getCumulativePuts();
    long evictions = stats.getCumulativeEvictions();
    long size = stats.getCurrentSize();

    lst.add("lookups", lookups);
    lst.add("hits", hits);
    lst.add("hitratio", calcHitRatio(lookups, hits));
    lst.add("inserts", inserts);
    lst.add("evictions", evictions);
    lst.add("size", size);

    lst.add("warmupTime", warmupTime);
    lst.add("timeDecay", timeDecay);

    long clookups = 0;
    long chits = 0;
    long cinserts = 0;
    long cevictions = 0;

    // NOTE: It is safe to iterate on a CopyOnWriteArrayList
    for (ConcurrentLFUCache.Stats statistics : statsList) {
      clookups += statistics.getCumulativeLookups();
      chits += statistics.getCumulativeHits();
      cinserts += statistics.getCumulativePuts();
      cevictions += statistics.getCumulativeEvictions();
    }
    lst.add("cumulative_lookups", clookups);
    lst.add("cumulative_hits", chits);
    lst.add("cumulative_hitratio", calcHitRatio(clookups, chits));
    lst.add("cumulative_inserts", cinserts);
    lst.add("cumulative_evictions", cevictions);

    if (showItems != 0) {
      Map items = cache.getMostUsedItems(showItems == -1 ? Integer.MAX_VALUE : showItems);
      for (Map.Entry e : (Set<Map.Entry>) items.entrySet()) {
        Object k = e.getKey();
        Object v = e.getValue();

        String ks = "item_" + k;
        String vs = v.toString();
        lst.add(ks, vs);
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    cacheMap = new MetricsMap((detailed, map) -> {
      if (cache != null) {
        ConcurrentLFUCache.Stats stats = cache.getStats();
        long lookups = stats.getCumulativeLookups();
        long hits = stats.getCumulativeHits();
        long inserts = stats.getCumulativePuts();
        long evictions = stats.getCumulativeEvictions();
        long size = stats.getCurrentSize();

        map.put("lookups", lookups);
        map.put("hits", hits);
        map.put("hitratio", calcHitRatio(lookups, hits));
        map.put("inserts", inserts);
        map.put("evictions", evictions);
        map.put("size", size);

        map.put("warmupTime", warmupTime);
        map.put("timeDecay", timeDecay);

        long clookups = 0;
        long chits = 0;
        long cinserts = 0;
        long cevictions = 0;

        // NOTE: It is safe to iterate on a CopyOnWriteArrayList
        for (ConcurrentLFUCache.Stats statistics : statsList) {
          clookups += statistics.getCumulativeLookups();
          chits += statistics.getCumulativeHits();
          cinserts += statistics.getCumulativePuts();
          cevictions += statistics.getCumulativeEvictions();
        }
        map.put("cumulative_lookups", clookups);
        map.put("cumulative_hits", chits);
        map.put("cumulative_hitratio", calcHitRatio(clookups, chits));
        map.put("cumulative_inserts", cinserts);
        map.put("cumulative_evictions", cevictions);

        if (detailed && showItems != 0) {
          Map items = cache.getMostUsedItems(showItems == -1 ? Integer.MAX_VALUE : showItems);
          for (Map.Entry e : (Set<Map.Entry>) items.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();

            String ks = "item_" + k;
            String vs = v.toString();
            map.put(ks, vs);
          }

        }

       }
    });
    manager.registerGauge(this, registryName, cacheMap, true, scope, getCategory().toString());
  }
 
    }
  // for unit tests only
  MetricsMap getMetricsMap() {
    return cacheMap;
  }
 
    return lst;
  @Override
  public Set<String> getMetricNames() {
    return metricNames;
  }

  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   @Override
   public String toString() {
    return name + getStatistics().toString();
    return name + cacheMap != null ? cacheMap.getValue().toString() : "";
   }

 }
diff --git a/solr/core/src/java/org/apache/solr/search/LRUCache.java b/solr/core/src/java/org/apache/solr/search/LRUCache.java
index b178fb21b1f..ce206fe2f7e 100644
-- a/solr/core/src/java/org/apache/solr/search/LRUCache.java
++ b/solr/core/src/java/org/apache/solr/search/LRUCache.java
@@ -19,18 +19,21 @@ package org.apache.solr.search;
 import java.lang.invoke.MethodHandles;
 import java.util.Collection;
 import java.util.Collections;
import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.Map;
import java.util.Set;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.LongAdder;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.lucene.util.Accountable;
 import org.apache.lucene.util.Accountables;
 import org.apache.lucene.util.RamUsageEstimator;
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -55,6 +58,7 @@ public class LRUCache<K,V> extends SolrCacheBase implements SolrCache<K,V>, Acco
   static final long LINKED_HASHTABLE_RAM_BYTES_PER_ENTRY =
       HASHTABLE_RAM_BYTES_PER_ENTRY
           + 2 * RamUsageEstimator.NUM_BYTES_OBJECT_REF; // previous & next references

   /// End copied code
 
   /* An instance of this class will be shared across multiple instances
@@ -82,6 +86,9 @@ public class LRUCache<K,V> extends SolrCacheBase implements SolrCache<K,V>, Acco
 
   private Map<K,V> map;
   private String description="LRU Cache";
  private MetricsMap cacheMap;
  private Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;
 
   private long maxRamBytes = Long.MAX_VALUE;
   // The synchronization used for the map will be used to update this,
@@ -319,45 +326,56 @@ public class LRUCache<K,V> extends SolrCacheBase implements SolrCache<K,V>, Acco
   }
 
   @Override
  public String getSource() {
    return null;
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public NamedList getStatistics() {
    NamedList lst = new SimpleOrderedMap();
    synchronized (map) {
      lst.add("lookups", lookups);
      lst.add("hits", hits);
      lst.add("hitratio", calcHitRatio(lookups,hits));
      lst.add("inserts", inserts);
      lst.add("evictions", evictions);
      lst.add("size", map.size());
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    cacheMap = new MetricsMap((detailed, res) -> {
      synchronized (map) {
        res.put("lookups", lookups);
        res.put("hits", hits);
        res.put("hitratio", calcHitRatio(lookups,hits));
        res.put("inserts", inserts);
        res.put("evictions", evictions);
        res.put("size", map.size());
        if (maxRamBytes != Long.MAX_VALUE)  {
          res.put("maxRamMB", maxRamBytes / 1024L / 1024L);
          res.put("ramBytesUsed", ramBytesUsed());
          res.put("evictionsRamUsage", evictionsRamUsage);
        }
      }
      res.put("warmupTime", warmupTime);

      long clookups = stats.lookups.longValue();
      long chits = stats.hits.longValue();
      res.put("cumulative_lookups", clookups);
      res.put("cumulative_hits", chits);
      res.put("cumulative_hitratio", calcHitRatio(clookups, chits));
      res.put("cumulative_inserts", stats.inserts.longValue());
      res.put("cumulative_evictions", stats.evictions.longValue());
       if (maxRamBytes != Long.MAX_VALUE)  {
        lst.add("maxRamMB", maxRamBytes / 1024L / 1024L);
        lst.add("ramBytesUsed", ramBytesUsed());
        lst.add("evictionsRamUsage", evictionsRamUsage);
        res.put("cumulative_evictionsRamUsage", stats.evictionsRamUsage.longValue());
       }
    }
    lst.add("warmupTime", warmupTime);
    
    long clookups = stats.lookups.longValue();
    long chits = stats.hits.longValue();
    lst.add("cumulative_lookups", clookups);
    lst.add("cumulative_hits", chits);
    lst.add("cumulative_hitratio", calcHitRatio(clookups, chits));
    lst.add("cumulative_inserts", stats.inserts.longValue());
    lst.add("cumulative_evictions", stats.evictions.longValue());
    if (maxRamBytes != Long.MAX_VALUE)  {
      lst.add("cumulative_evictionsRamUsage", stats.evictionsRamUsage.longValue());
    }
    
    return lst;
    });
    manager.registerGauge(this, registryName, cacheMap, true, scope, getCategory().toString());
  }

  // for unit tests only
  MetricsMap getMetricsMap() {
    return cacheMap;
  }

  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   @Override
   public String toString() {
    return name() + getStatistics().toString();
    return name() + cacheMap != null ? cacheMap.getValue().toString() : "";
   }
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/search/QParserPlugin.java b/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
index 34089d201a0..872c618afaa 100644
-- a/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/QParserPlugin.java
@@ -16,14 +16,14 @@
  */
 package org.apache.solr.search;
 
import java.net.URL;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
import java.util.Set;
 
 import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.join.BlockJoinChildQParserPlugin;
 import org.apache.solr.search.join.BlockJoinParentQParserPlugin;
@@ -31,7 +31,7 @@ import org.apache.solr.search.join.GraphQParserPlugin;
 import org.apache.solr.search.mlt.MLTQParserPlugin;
 import org.apache.solr.util.plugin.NamedListInitializedPlugin;
 
public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrInfoMBean {
public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrInfoBean {
   /** internal use - name of the default parser */
   public static final String DEFAULT_QTYPE = LuceneQParserPlugin.NAME;
 
@@ -98,11 +98,6 @@ public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrI
     return this.getClass().getName();
   }
 
  @Override
  public String getVersion() {
    return null;
  }

   @Override
   public String getDescription() {
     return "";  // UI required non-null to work
@@ -114,19 +109,10 @@ public abstract class QParserPlugin implements NamedListInitializedPlugin, SolrI
   }
 
   @Override
  public String getSource() {
  public Set<String> getMetricNames() {
     return null;
   }
 
  @Override
  public URL[] getDocs() {
    return new URL[0];
  }

  @Override
  public NamedList getStatistics() {
    return null;
  }
 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/search/SolrCache.java b/solr/core/src/java/org/apache/solr/search/SolrCache.java
index 9a2d0fc38e4..caa5c2c3b32 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrCache.java
++ b/solr/core/src/java/org/apache/solr/search/SolrCache.java
@@ -16,7 +16,8 @@
  */
 package org.apache.solr.search;
 
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.SolrMetricProducer;
 
 import java.util.Map;
 
@@ -24,7 +25,7 @@ import java.util.Map;
 /**
  * Primary API for dealing with Solr's internal caches.
  */
public interface SolrCache<K,V> extends SolrInfoMBean {
public interface SolrCache<K,V> extends SolrInfoBean, SolrMetricProducer {
 
   /**
    * The initialization routine. Instance specific arguments are passed in
diff --git a/solr/core/src/java/org/apache/solr/search/SolrCacheBase.java b/solr/core/src/java/org/apache/solr/search/SolrCacheBase.java
index 85caa90cfee..c388d548036 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrCacheBase.java
++ b/solr/core/src/java/org/apache/solr/search/SolrCacheBase.java
@@ -18,11 +18,10 @@ package org.apache.solr.search;
 
 import java.math.BigDecimal;
 import java.math.RoundingMode;
import java.net.URL;
 import java.util.Map;
 
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean.Category;
import org.apache.solr.core.SolrInfoBean.Category;
 import org.apache.solr.search.SolrCache.State;
 
 import static org.apache.solr.common.params.CommonParams.NAME;
@@ -106,10 +105,6 @@ public abstract class SolrCacheBase {
     return Category.CACHE;
   }
 
  public URL[] getDocs() {
    return null;
  }
  
   public void init(Map<String, String> args, CacheRegenerator regenerator) {
     this.regenerator = regenerator;
     state = State.CREATED;
diff --git a/solr/core/src/java/org/apache/solr/search/SolrFieldCacheBean.java b/solr/core/src/java/org/apache/solr/search/SolrFieldCacheBean.java
new file mode 100644
index 00000000000..ffcc37d64cf
-- /dev/null
++ b/solr/core/src/java/org/apache/solr/search/SolrFieldCacheBean.java
@@ -0,0 +1,77 @@
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
package org.apache.solr.search;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
import org.apache.solr.uninverting.UninvertingReader;

/**
 * A SolrInfoBean that provides introspection of the Solr FieldCache
 *
 */
public class SolrFieldCacheBean implements SolrInfoBean, SolrMetricProducer {

  private boolean disableEntryList = Boolean.getBoolean("disableSolrFieldCacheMBeanEntryList");
  private boolean disableJmxEntryList = Boolean.getBoolean("disableSolrFieldCacheMBeanEntryListJmx");

  private MetricRegistry registry;
  private Set<String> metricNames = new HashSet<>();

  @Override
  public String getName() { return this.getClass().getName(); }
  @Override
  public String getDescription() {
    return "Provides introspection of the Solr FieldCache ";
  }
  @Override
  public Category getCategory() { return Category.CACHE; }
  @Override
  public Set<String> getMetricNames() {
    return metricNames;
  }
  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
  }

  @Override
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    MetricsMap metricsMap = new MetricsMap((detailed, map) -> {
      if (detailed && !disableEntryList && !disableJmxEntryList) {
        UninvertingReader.FieldCacheStats fieldCacheStats = UninvertingReader.getUninvertedStats();
        String[] entries = fieldCacheStats.info;
        map.put("entries_count", entries.length);
        map.put("total_size", fieldCacheStats.totalSize);
        for (int i = 0; i < entries.length; i++) {
          final String entry = entries[i];
          map.put("entry#" + i, entry);
        }
      } else {
        map.put("entries_count", UninvertingReader.getUninvertedStatsSize());
      }
    });
    manager.register(this, registryName, metricsMap, true, "fieldCache", Category.CACHE.toString(), scope);
  }
}
diff --git a/solr/core/src/java/org/apache/solr/search/SolrFieldCacheMBean.java b/solr/core/src/java/org/apache/solr/search/SolrFieldCacheMBean.java
deleted file mode 100644
index 642b7087846..00000000000
-- a/solr/core/src/java/org/apache/solr/search/SolrFieldCacheMBean.java
++ /dev/null
@@ -1,78 +0,0 @@
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
package org.apache.solr.search;

import java.net.URL;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.JmxMonitoredMap.JmxAugmentedSolrInfoMBean;
import org.apache.solr.core.SolrCore;
import org.apache.solr.uninverting.UninvertingReader;

/**
 * A SolrInfoMBean that provides introspection of the Solr FieldCache
 *
 */
public class SolrFieldCacheMBean implements JmxAugmentedSolrInfoMBean {

  private boolean disableEntryList = Boolean.getBoolean("disableSolrFieldCacheMBeanEntryList");
  private boolean disableJmxEntryList = Boolean.getBoolean("disableSolrFieldCacheMBeanEntryListJmx");

  @Override
  public String getName() { return this.getClass().getName(); }
  @Override
  public String getVersion() { return SolrCore.version; }
  @Override
  public String getDescription() {
    return "Provides introspection of the Solr FieldCache ";
  }
  @Override
  public Category getCategory() { return Category.CACHE; } 
  @Override
  public String getSource() { return null; }
  @Override
  public URL[] getDocs() {
    return null;
  }
  @Override
  public NamedList getStatistics() {
    return getStats(!disableEntryList);
  }

  @Override
  public NamedList getStatisticsForJmx() {
    return getStats(!disableEntryList && !disableJmxEntryList);
  }

  private NamedList getStats(boolean listEntries) {
    NamedList stats = new SimpleOrderedMap();
    if (listEntries) {
      UninvertingReader.FieldCacheStats fieldCacheStats = UninvertingReader.getUninvertedStats();
      String[] entries = fieldCacheStats.info;
      stats.add("entries_count", entries.length);
      stats.add("total_size", fieldCacheStats.totalSize);
      for (int i = 0; i < entries.length; i++) {
        stats.add("entry#" + i, entries[i]);
      }
    } else {
      stats.add("entries_count", UninvertingReader.getUninvertedStatsSize());
    }
    return stats;
  }

}
diff --git a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
index 4207a9b411b..9b38225b634 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -19,13 +19,13 @@ package org.apache.solr.search;
 import java.io.Closeable;
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
@@ -34,6 +34,7 @@ import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.atomic.AtomicReference;
 
import com.codahale.metrics.MetricRegistry;
 import com.google.common.collect.Iterables;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.DirectoryReader;
@@ -58,15 +59,15 @@ import org.apache.lucene.util.FixedBitSet;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.ObjectReleaseTracker;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.DirectoryFactory.DirContext;
 import org.apache.solr.core.SolrConfig;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.index.SlowCompositeReaderWrapper;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestInfo;
@@ -86,7 +87,7 @@ import org.slf4j.LoggerFactory;
  *
  * @since solr 0.9
  */
public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrInfoMBean {
public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrInfoBean, SolrMetricProducer {
 
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
@@ -136,7 +137,7 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
   private final String path;
   private boolean releaseDirectory;
 
  private final NamedList<Object> readerStats;
  private Set<String> metricNames = new HashSet<>();
 
   private static DirectoryReader getReader(SolrCore core, SolrIndexConfig config, DirectoryFactory directoryFactory,
                                            String path) throws IOException {
@@ -302,7 +303,6 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
     // We already have our own filter cache
     setQueryCache(null);
 
    readerStats = snapStatistics(reader);
     // do this at the end since an exception in the constructor means we won't close
     numOpens.incrementAndGet();
     assert ObjectReleaseTracker.track(this);
@@ -404,10 +404,10 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
   }
 
   /**
   * Register sub-objects such as caches
   * Register sub-objects such as caches and our own metrics
    */
   public void register() {
    final Map<String,SolrInfoMBean> infoRegistry = core.getInfoRegistry();
    final Map<String,SolrInfoBean> infoRegistry = core.getInfoRegistry();
     // register self
     infoRegistry.put(STATISTICS_KEY, this);
     infoRegistry.put(name, this);
@@ -415,6 +415,12 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
       cache.setState(SolrCache.State.LIVE);
       infoRegistry.put(cache.name(), cache);
     }
    SolrMetricManager manager = core.getCoreDescriptor().getCoreContainer().getMetricManager();
    String registry = core.getCoreMetricManager().getRegistryName();
    for (SolrCache cache : cacheList) {
      cache.initializeMetrics(manager, registry, SolrMetricManager.mkName(cache.name(), STATISTICS_KEY));
    }
    initializeMetrics(manager, registry, STATISTICS_KEY);
     registerTime = new Date();
   }
 
@@ -2190,7 +2196,7 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
 
 
   /////////////////////////////////////////////////////////////////////
  // SolrInfoMBean stuff: Statistics and Module Info
  // SolrInfoBean stuff: Statistics and Module Info
   /////////////////////////////////////////////////////////////////////
 
   @Override
@@ -2198,11 +2204,6 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
     return SolrIndexSearcher.class.getName();
   }
 
  @Override
  public String getVersion() {
    return SolrCore.version;
  }

   @Override
   public String getDescription() {
     return "index searcher";
@@ -2214,38 +2215,31 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable, SolrI
   }
 
   @Override
  public String getSource() {
    return null;
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public URL[] getDocs() {
    return null;
  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {

    manager.registerGauge(this, registry, () -> name, true, "searcherName", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> cachingEnabled, true, "caching", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> openTime, true, "openedAt", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> warmupTime, true, "warmupTime", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> registerTime, true, "registeredAt", Category.SEARCHER.toString(), scope);
    // reader stats
    manager.registerGauge(this, registry, () -> reader.numDocs(), true, "numDocs", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> reader.maxDoc(), true, "maxDoc", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> reader.maxDoc() - reader.numDocs(), true, "deletedDocs", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> reader.toString(), true, "reader", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> reader.directory().toString(), true, "readerDir", Category.SEARCHER.toString(), scope);
    manager.registerGauge(this, registry, () -> reader.getVersion(), true, "indexVersion", Category.SEARCHER.toString(), scope);

   }
 
   @Override
  public NamedList<Object> getStatistics() {
    final NamedList<Object> lst = new SimpleOrderedMap<>();
    lst.add("searcherName", name);
    lst.add("caching", cachingEnabled);

    lst.addAll(readerStats);

    lst.add("openedAt", openTime);
    if (registerTime != null) lst.add("registeredAt", registerTime);
    lst.add("warmupTime", warmupTime);
    return lst;
  }

  static private NamedList<Object> snapStatistics(DirectoryReader reader) {
    final NamedList<Object> lst = new SimpleOrderedMap<>();
    lst.add("numDocs", reader.numDocs());
    lst.add("maxDoc", reader.maxDoc());
    lst.add("deletedDocs", reader.maxDoc() - reader.numDocs());
    lst.add("reader", reader.toString());
    lst.add("readerDir", reader.directory());
    lst.add("indexVersion", reader.getVersion());
    return lst;
  public MetricRegistry getMetricRegistry() {
    return core.getMetricRegistry();
   }
 
   private static class FilterImpl extends Filter {
diff --git a/solr/core/src/java/org/apache/solr/search/facet/FacetModule.java b/solr/core/src/java/org/apache/solr/search/facet/FacetModule.java
index bf1379162ef..3407ae41c1b 100644
-- a/solr/core/src/java/org/apache/solr/search/facet/FacetModule.java
++ b/solr/core/src/java/org/apache/solr/search/facet/FacetModule.java
@@ -319,12 +319,6 @@ public class FacetModule extends SearchComponent {
   public Category getCategory() {
     return Category.QUERY;
   }

  @Override
  public String getSource() {
    return null;
  }

 }
 
 
diff --git a/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java b/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java
index ff0db9b7990..24bcf3dc38a 100644
-- a/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java
++ b/solr/core/src/java/org/apache/solr/servlet/SolrDispatchFilter.java
@@ -16,7 +16,6 @@
  */
 package org.apache.solr.servlet;
 
import javax.management.MBeanServer;
 import javax.servlet.FilterChain;
 import javax.servlet.FilterConfig;
 import javax.servlet.ServletException;
@@ -34,7 +33,6 @@ import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.time.Instant;
@@ -47,7 +45,6 @@ import java.util.concurrent.atomic.AtomicReference;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
import com.codahale.metrics.jvm.BufferPoolMetricSet;
 import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
 import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
 import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
@@ -66,9 +63,10 @@ import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.NodeConfig;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.core.SolrXmlConfig;
import org.apache.solr.metrics.AltBufferPoolMetricSet;
 import org.apache.solr.metrics.OperatingSystemMetricSet;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.request.SolrRequestInfo;
@@ -185,13 +183,12 @@ public class SolrDispatchFilter extends BaseSolrFilter {
   }
 
   private void setupJvmMetrics()  {
    MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
     SolrMetricManager metricManager = cores.getMetricManager();
     try {
      String registry = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.jvm);
      metricManager.registerAll(registry, new BufferPoolMetricSet(platformMBeanServer), true, "buffers");
      String registry = SolrMetricManager.getRegistryName(SolrInfoBean.Group.jvm);
      metricManager.registerAll(registry, new AltBufferPoolMetricSet(), true, "buffers");
       metricManager.registerAll(registry, new ClassLoadingGaugeSet(), true, "classes");
      metricManager.registerAll(registry, new OperatingSystemMetricSet(platformMBeanServer), true, "os");
      metricManager.registerAll(registry, new OperatingSystemMetricSet(), true, "os");
       metricManager.registerAll(registry, new GarbageCollectorMetricSet(), true, "gc");
       metricManager.registerAll(registry, new MemoryUsageGaugeSet(), true, "memory");
       metricManager.registerAll(registry, new ThreadStatesGaugeSet(), true, "threads"); // todo should we use CachedThreadStatesGaugeSet instead?
diff --git a/solr/core/src/java/org/apache/solr/store/blockcache/Metrics.java b/solr/core/src/java/org/apache/solr/store/blockcache/Metrics.java
index d3e34979b74..b8b9bea11ab 100644
-- a/solr/core/src/java/org/apache/solr/store/blockcache/Metrics.java
++ b/solr/core/src/java/org/apache/solr/store/blockcache/Metrics.java
@@ -16,20 +16,23 @@
  */
 package org.apache.solr.store.blockcache;
 
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
 import java.util.concurrent.atomic.AtomicLong;
 
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrInfoMBean;
import com.codahale.metrics.MetricRegistry;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.search.SolrCacheBase;
 
 /**
 * A {@link SolrInfoMBean} that provides metrics on block cache operations.
 * A {@link SolrInfoBean} that provides metrics on block cache operations.
  *
  * @lucene.experimental
  */
public class Metrics extends SolrCacheBase implements SolrInfoMBean {
public class Metrics extends SolrCacheBase implements SolrInfoBean, SolrMetricProducer {
 
 
   public AtomicLong blockCacheSize = new AtomicLong(0);
@@ -50,66 +53,70 @@ public class Metrics extends SolrCacheBase implements SolrInfoMBean {
   public AtomicLong shardBuffercacheAllocate = new AtomicLong(0);
   public AtomicLong shardBuffercacheLost = new AtomicLong(0);
 
  private MetricsMap metricsMap;
  private MetricRegistry registry;
  private Set<String> metricNames = new HashSet<>();
 
   private long previous = System.nanoTime();
 

  public NamedList<Number> getStatistics() {
    NamedList<Number> stats = new SimpleOrderedMap<>(21); // room for one method call before growing

    long now = System.nanoTime();
    long delta = Math.max(now - previous, 1);
    double seconds = delta / 1000000000.0;

    long hits_total = blockCacheHit.get();
    long hits_delta = hits_total - blockCacheHit_last.get();
    blockCacheHit_last.set(hits_total);

    long miss_total = blockCacheMiss.get();
    long miss_delta = miss_total - blockCacheMiss_last.get();
    blockCacheMiss_last.set(miss_total);

    long evict_total = blockCacheEviction.get();
    long evict_delta = evict_total - blockCacheEviction_last.get();
    blockCacheEviction_last.set(evict_total);

    long storeFail_total = blockCacheStoreFail.get();
    long storeFail_delta = storeFail_total - blockCacheStoreFail_last.get();
    blockCacheStoreFail_last.set(storeFail_total);

    long lookups_delta = hits_delta + miss_delta;
    long lookups_total = hits_total + miss_total;

    stats.add("size", blockCacheSize.get());
    stats.add("lookups", lookups_total);
    stats.add("hits", hits_total);
    stats.add("evictions", evict_total);
    stats.add("storeFails", storeFail_total);
    stats.add("hitratio_current", calcHitRatio(lookups_delta, hits_delta));  // hit ratio since the last call
    stats.add("lookups_persec", getPerSecond(lookups_delta, seconds)); // lookups per second since the last call
    stats.add("hits_persec", getPerSecond(hits_delta, seconds));       // hits per second since the last call
    stats.add("evictions_persec", getPerSecond(evict_delta, seconds));  // evictions per second since the last call
    stats.add("storeFails_persec", getPerSecond(storeFail_delta, seconds));  // evictions per second since the last call
    stats.add("time_delta", seconds);  // seconds since last call

    // TODO: these aren't really related to the BlockCache
    stats.add("buffercache.allocations", getPerSecond(shardBuffercacheAllocate.getAndSet(0), seconds));
    stats.add("buffercache.lost", getPerSecond(shardBuffercacheLost.getAndSet(0), seconds));

    previous = now;

    return stats;
  @Override
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    metricsMap = new MetricsMap((detailed, map) -> {
      long now = System.nanoTime();
      long delta = Math.max(now - previous, 1);
      double seconds = delta / 1000000000.0;

      long hits_total = blockCacheHit.get();
      long hits_delta = hits_total - blockCacheHit_last.get();
      blockCacheHit_last.set(hits_total);

      long miss_total = blockCacheMiss.get();
      long miss_delta = miss_total - blockCacheMiss_last.get();
      blockCacheMiss_last.set(miss_total);

      long evict_total = blockCacheEviction.get();
      long evict_delta = evict_total - blockCacheEviction_last.get();
      blockCacheEviction_last.set(evict_total);

      long storeFail_total = blockCacheStoreFail.get();
      long storeFail_delta = storeFail_total - blockCacheStoreFail_last.get();
      blockCacheStoreFail_last.set(storeFail_total);

      long lookups_delta = hits_delta + miss_delta;
      long lookups_total = hits_total + miss_total;

      map.put("size", blockCacheSize.get());
      map.put("lookups", lookups_total);
      map.put("hits", hits_total);
      map.put("evictions", evict_total);
      map.put("storeFails", storeFail_total);
      map.put("hitratio_current", calcHitRatio(lookups_delta, hits_delta));  // hit ratio since the last call
      map.put("lookups_persec", getPerSecond(lookups_delta, seconds)); // lookups per second since the last call
      map.put("hits_persec", getPerSecond(hits_delta, seconds));       // hits per second since the last call
      map.put("evictions_persec", getPerSecond(evict_delta, seconds));  // evictions per second since the last call
      map.put("storeFails_persec", getPerSecond(storeFail_delta, seconds));  // evictions per second since the last call
      map.put("time_delta", seconds);  // seconds since last call

      // TODO: these aren't really related to the BlockCache
      map.put("buffercache.allocations", getPerSecond(shardBuffercacheAllocate.getAndSet(0), seconds));
      map.put("buffercache.lost", getPerSecond(shardBuffercacheLost.getAndSet(0), seconds));

      previous = now;

    });
    manager.registerGauge(this, registryName, metricsMap, true, getName(), getCategory().toString(), scope);
   }
 
   private float getPerSecond(long value, double seconds) {
     return (float) (value / seconds);
   }
 
  // SolrInfoMBean methods
  // SolrInfoBean methods
 
   @Override
   public String getName() {
    return "HdfsBlockCache";
    return "hdfsBlockCache";
   }
 
   @Override
@@ -118,12 +125,13 @@ public class Metrics extends SolrCacheBase implements SolrInfoMBean {
   }
 
   @Override
  public String getSource() {
    return null;
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public URL[] getDocs() {
    return null;
  public MetricRegistry getMetricRegistry() {
    return registry;
   }

 }
diff --git a/solr/core/src/java/org/apache/solr/store/hdfs/HdfsLocalityReporter.java b/solr/core/src/java/org/apache/solr/store/hdfs/HdfsLocalityReporter.java
index ba7c7fd1393..64e6356dcd8 100644
-- a/solr/core/src/java/org/apache/solr/store/hdfs/HdfsLocalityReporter.java
++ b/solr/core/src/java/org/apache/solr/store/hdfs/HdfsLocalityReporter.java
@@ -18,8 +18,8 @@ package org.apache.solr.store.hdfs;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.Arrays;
import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
@@ -27,16 +27,18 @@ import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.hadoop.fs.BlockLocation;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
public class HdfsLocalityReporter implements SolrInfoMBean {
public class HdfsLocalityReporter implements SolrInfoBean, SolrMetricProducer {
   public static final String LOCALITY_BYTES_TOTAL = "locality.bytes.total";
   public static final String LOCALITY_BYTES_LOCAL = "locality.bytes.local";
   public static final String LOCALITY_BYTES_RATIO = "locality.bytes.ratio";
@@ -49,6 +51,9 @@ public class HdfsLocalityReporter implements SolrInfoMBean {
   private String hostname;
   private final ConcurrentMap<HdfsDirectory,ConcurrentMap<FileStatus,BlockLocation[]>> cache;
 
  private final Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;

   public HdfsLocalityReporter() {
     cache = new ConcurrentHashMap<>();
   }
@@ -66,11 +71,6 @@ public class HdfsLocalityReporter implements SolrInfoMBean {
     return "hdfs-locality";
   }
 
  @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }

   @Override
   public String getDescription() {
     return "Provides metrics for HDFS data locality.";
@@ -82,89 +82,71 @@ public class HdfsLocalityReporter implements SolrInfoMBean {
   }
 
   @Override
  public String getSource() {
    return null;
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public URL[] getDocs() {
    return null;
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   /**
    * Provide statistics on HDFS block locality, both in terms of bytes and block counts.
    */
   @Override
  public NamedList getStatistics() {
    long totalBytes = 0;
    long localBytes = 0;
    int totalCount = 0;
    int localCount = 0;

    for (Iterator<HdfsDirectory> iterator = cache.keySet().iterator(); iterator.hasNext();) {
      HdfsDirectory hdfsDirectory = iterator.next();

      if (hdfsDirectory.isClosed()) {
        iterator.remove();
      } else {
        try {
          refreshDirectory(hdfsDirectory);
          Map<FileStatus,BlockLocation[]> blockMap = cache.get(hdfsDirectory);

          // For every block in every file in this directory, count it
          for (BlockLocation[] locations : blockMap.values()) {
            for (BlockLocation bl : locations) {
              totalBytes += bl.getLength();
              totalCount++;

              if (Arrays.asList(bl.getHosts()).contains(hostname)) {
                localBytes += bl.getLength();
                localCount++;
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    MetricsMap metricsMap = new MetricsMap((detailed, map) -> {
      long totalBytes = 0;
      long localBytes = 0;
      int totalCount = 0;
      int localCount = 0;

      for (Iterator<HdfsDirectory> iterator = cache.keySet().iterator(); iterator.hasNext();) {
        HdfsDirectory hdfsDirectory = iterator.next();

        if (hdfsDirectory.isClosed()) {
          iterator.remove();
        } else {
          try {
            refreshDirectory(hdfsDirectory);
            Map<FileStatus,BlockLocation[]> blockMap = cache.get(hdfsDirectory);

            // For every block in every file in this directory, count it
            for (BlockLocation[] locations : blockMap.values()) {
              for (BlockLocation bl : locations) {
                totalBytes += bl.getLength();
                totalCount++;

                if (Arrays.asList(bl.getHosts()).contains(hostname)) {
                  localBytes += bl.getLength();
                  localCount++;
                }
               }
             }
          } catch (IOException e) {
            logger.warn("Could not retrieve locality information for {} due to exception: {}",
                hdfsDirectory.getHdfsDirPath(), e);
           }
        } catch (IOException e) {
          logger.warn("Could not retrieve locality information for {} due to exception: {}",
              hdfsDirectory.getHdfsDirPath(), e);
         }
       }
    }

    return createStatistics(totalBytes, localBytes, totalCount, localCount);
  }

  /**
   * Generate a statistics object based on the given measurements for all files monitored by this reporter.
   * 
   * @param totalBytes
   *          The total bytes used
   * @param localBytes
   *          The amount of bytes found on local nodes
   * @param totalCount
   *          The total block count
   * @param localCount
   *          The amount of blocks found on local nodes
   * @return HDFS block locality statistics
   */
  private NamedList<Number> createStatistics(long totalBytes, long localBytes, int totalCount, int localCount) {
    NamedList<Number> statistics = new SimpleOrderedMap<Number>();

    statistics.add(LOCALITY_BYTES_TOTAL, totalBytes);
    statistics.add(LOCALITY_BYTES_LOCAL, localBytes);
    if (localBytes == 0) {
      statistics.add(LOCALITY_BYTES_RATIO, 0);
    } else {
      statistics.add(LOCALITY_BYTES_RATIO, localBytes / (double) totalBytes);
    }
    statistics.add(LOCALITY_BLOCKS_TOTAL, totalCount);
    statistics.add(LOCALITY_BLOCKS_LOCAL, localCount);
    if (localCount == 0) {
      statistics.add(LOCALITY_BLOCKS_RATIO, 0);
    } else {
      statistics.add(LOCALITY_BLOCKS_RATIO, localCount / (double) totalCount);
    }

    return statistics;
      map.put(LOCALITY_BYTES_TOTAL, totalBytes);
      map.put(LOCALITY_BYTES_LOCAL, localBytes);
      if (localBytes == 0) {
        map.put(LOCALITY_BYTES_RATIO, 0);
      } else {
        map.put(LOCALITY_BYTES_RATIO, localBytes / (double) totalBytes);
      }
      map.put(LOCALITY_BLOCKS_TOTAL, totalCount);
      map.put(LOCALITY_BLOCKS_LOCAL, localCount);
      if (localCount == 0) {
        map.put(LOCALITY_BLOCKS_RATIO, 0);
      } else {
        map.put(LOCALITY_BLOCKS_RATIO, localCount / (double) totalCount);
      }
    });
    manager.registerGauge(this, registryName, metricsMap, true, "hdfsLocality", getCategory().toString(), scope);
   }
 
   /**
@@ -209,4 +191,5 @@ public class HdfsLocalityReporter implements SolrInfoMBean {
       }
     }
   }

 }
diff --git a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
index fdc9d2223ec..4ef91e4af3e 100644
-- a/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
++ b/solr/core/src/java/org/apache/solr/update/DirectUpdateHandler2.java
@@ -18,7 +18,6 @@ package org.apache.solr.update;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
@@ -48,8 +47,6 @@ import org.apache.solr.cloud.ZkController;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.cloud.DocCollection;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.SolrConfig.UpdateHandlerInfo;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.metrics.SolrMetricManager;
@@ -162,24 +159,40 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    commitCommands = manager.meter(registry, "commits", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> commitTracker.getCommitCount(), true, "autoCommits", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> softCommitTracker.getCommitCount(), true, "softAutoCommits", getCategory().toString(), scope);
    optimizeCommands = manager.meter(registry, "optimizes", getCategory().toString(), scope);
    rollbackCommands = manager.meter(registry, "rollbacks", getCategory().toString(), scope);
    splitCommands = manager.meter(registry, "splits", getCategory().toString(), scope);
    mergeIndexesCommands = manager.meter(registry, "merges", getCategory().toString(), scope);
    expungeDeleteCommands = manager.meter(registry, "expungeDeletes", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> numDocsPending.longValue(), true, "docsPending", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> addCommands.longValue(), true, "adds", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> deleteByIdCommands.longValue(), true, "deletesById", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> deleteByQueryCommands.longValue(), true, "deletesByQuery", getCategory().toString(), scope);
    manager.registerGauge(registry, () -> numErrors.longValue(), true, "errors", getCategory().toString(), scope);

    addCommandsCumulative = manager.meter(registry, "cumulativeAdds", getCategory().toString(), scope);
    deleteByIdCommandsCumulative = manager.meter(registry, "cumulativeDeletesById", getCategory().toString(), scope);
    deleteByQueryCommandsCumulative = manager.meter(registry, "cumulativeDeletesByQuery", getCategory().toString(), scope);
    numErrorsCumulative = manager.meter(registry, "cumulativeErrors", getCategory().toString(), scope);
    commitCommands = manager.meter(this, registry, "commits", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> commitTracker.getCommitCount(), true, "autoCommits", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> softCommitTracker.getCommitCount(), true, "softAutoCommits", getCategory().toString(), scope);
    if (commitTracker.getDocsUpperBound() > 0) {
      manager.registerGauge(this, registry, () -> commitTracker.getDocsUpperBound(), true, "autoCommitMaxDocs",
          getCategory().toString(), scope);
    }
    if (commitTracker.getTimeUpperBound() > 0) {
      manager.registerGauge(this, registry, () -> "" + commitTracker.getTimeUpperBound() + "ms", true, "autoCommitMaxTime",
          getCategory().toString(), scope);
    }
    if (softCommitTracker.getDocsUpperBound() > 0) {
      manager.registerGauge(this, registry, () -> softCommitTracker.getDocsUpperBound(), true, "softAutoCommitMaxDocs",
          getCategory().toString(), scope);
    }
    if (softCommitTracker.getTimeUpperBound() > 0) {
      manager.registerGauge(this, registry, () -> "" + softCommitTracker.getTimeUpperBound() + "ms", true, "softAutoCommitMaxTime",
          getCategory().toString(), scope);
    }
    optimizeCommands = manager.meter(this, registry, "optimizes", getCategory().toString(), scope);
    rollbackCommands = manager.meter(this, registry, "rollbacks", getCategory().toString(), scope);
    splitCommands = manager.meter(this, registry, "splits", getCategory().toString(), scope);
    mergeIndexesCommands = manager.meter(this, registry, "merges", getCategory().toString(), scope);
    expungeDeleteCommands = manager.meter(this, registry, "expungeDeletes", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> numDocsPending.longValue(), true, "docsPending", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> addCommands.longValue(), true, "adds", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> deleteByIdCommands.longValue(), true, "deletesById", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> deleteByQueryCommands.longValue(), true, "deletesByQuery", getCategory().toString(), scope);
    manager.registerGauge(this, registry, () -> numErrors.longValue(), true, "errors", getCategory().toString(), scope);

    addCommandsCumulative = manager.meter(this, registry, "cumulativeAdds", getCategory().toString(), scope);
    deleteByIdCommandsCumulative = manager.meter(this, registry, "cumulativeDeletesById", getCategory().toString(), scope);
    deleteByQueryCommandsCumulative = manager.meter(this, registry, "cumulativeDeletesByQuery", getCategory().toString(), scope);
    numErrorsCumulative = manager.meter(this, registry, "cumulativeErrors", getCategory().toString(), scope);
   }
 
   private void deleteAll() throws IOException {
@@ -951,7 +964,7 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
 
 
   /////////////////////////////////////////////////////////////////////
  // SolrInfoMBean stuff: Statistics and Module Info
  // SolrInfoBean stuff: Statistics and Module Info
   /////////////////////////////////////////////////////////////////////
 
   @Override
@@ -959,70 +972,11 @@ public class DirectUpdateHandler2 extends UpdateHandler implements SolrCoreState
     return DirectUpdateHandler2.class.getName();
   }
 
  @Override
  public String getVersion() {
    return SolrCore.version;
  }

   @Override
   public String getDescription() {
     return "Update handler that efficiently directly updates the on-disk main lucene index";
   }
 
  @Override
  public String getSource() {
    return null;
  }

  @Override
  public URL[] getDocs() {
    return null;
  }

  @Override
  public NamedList getStatistics() {
    NamedList lst = new SimpleOrderedMap();
    lst.add("commits", commitCommands.getCount());
    if (commitTracker.getDocsUpperBound() > 0) {
      lst.add("autocommit maxDocs", commitTracker.getDocsUpperBound());
    }
    if (commitTracker.getTimeUpperBound() > 0) {
      lst.add("autocommit maxTime", "" + commitTracker.getTimeUpperBound() + "ms");
    }
    lst.add("autocommits", commitTracker.getCommitCount());
    if (softCommitTracker.getDocsUpperBound() > 0) {
      lst.add("soft autocommit maxDocs", softCommitTracker.getDocsUpperBound());
    }
    if (softCommitTracker.getTimeUpperBound() > 0) {
      lst.add("soft autocommit maxTime", "" + softCommitTracker.getTimeUpperBound() + "ms");
    }
    lst.add("soft autocommits", softCommitTracker.getCommitCount());
    lst.add("optimizes", optimizeCommands.getCount());
    lst.add("rollbacks", rollbackCommands.getCount());
    lst.add("expungeDeletes", expungeDeleteCommands.getCount());
    lst.add("docsPending", numDocsPending.longValue());
    // pset.size() not synchronized, but it should be fine to access.
    // lst.add("deletesPending", pset.size());
    lst.add("adds", addCommands.longValue());
    lst.add("deletesById", deleteByIdCommands.longValue());
    lst.add("deletesByQuery", deleteByQueryCommands.longValue());
    lst.add("errors", numErrors.longValue());
    lst.add("cumulative_adds", addCommandsCumulative.getCount());
    lst.add("cumulative_deletesById", deleteByIdCommandsCumulative.getCount());
    lst.add("cumulative_deletesByQuery", deleteByQueryCommandsCumulative.getCount());
    lst.add("cumulative_errors", numErrorsCumulative.getCount());
    if (this.ulog != null) {
      lst.add("transaction_logs_total_size", ulog.getTotalLogsSize());
      lst.add("transaction_logs_total_number", ulog.getTotalLogsNumber());
    }
    return lst;
  }

  @Override
  public String toString() {
    return "DirectUpdateHandler2" + getStatistics();
  }
  
   @Override
   public SolrCoreState getSolrCoreState() {
     return solrCoreState;
diff --git a/solr/core/src/java/org/apache/solr/update/HdfsUpdateLog.java b/solr/core/src/java/org/apache/solr/update/HdfsUpdateLog.java
index 71e20d9f260..7bb74d05bf9 100644
-- a/solr/core/src/java/org/apache/solr/update/HdfsUpdateLog.java
++ b/solr/core/src/java/org/apache/solr/update/HdfsUpdateLog.java
@@ -37,7 +37,7 @@ import org.apache.solr.common.SolrException.ErrorCode;
 import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.util.HdfsUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -262,7 +262,7 @@ public class HdfsUpdateLog extends UpdateLog {
     }
 
     // initialize metrics
    core.getCoreMetricManager().registerMetricProducer(SolrInfoMBean.Category.TLOG.toString(), this);
    core.getCoreMetricManager().registerMetricProducer(SolrInfoBean.Category.TLOG.toString(), this);
   }
   
   @Override
diff --git a/solr/core/src/java/org/apache/solr/update/PeerSync.java b/solr/core/src/java/org/apache/solr/update/PeerSync.java
index 9470cca41be..f59984449a4 100644
-- a/solr/core/src/java/org/apache/solr/update/PeerSync.java
++ b/solr/core/src/java/org/apache/solr/update/PeerSync.java
@@ -43,7 +43,7 @@ import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.handler.component.HttpShardHandlerFactory;
 import org.apache.solr.handler.component.ShardHandler;
 import org.apache.solr.handler.component.ShardHandlerFactory;
@@ -160,16 +160,16 @@ public class PeerSync implements SolrMetricProducer {
     shardHandlerFactory = (HttpShardHandlerFactory) core.getCoreDescriptor().getCoreContainer().getShardHandlerFactory();
     shardHandler = shardHandlerFactory.getShardHandler(client);
 
    core.getCoreMetricManager().registerMetricProducer(SolrInfoMBean.Category.REPLICATION.toString(), this);
    core.getCoreMetricManager().registerMetricProducer(SolrInfoBean.Category.REPLICATION.toString(), this);
   }
 
   public static final String METRIC_SCOPE = "peerSync";
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    syncTime = manager.timer(registry, "time", scope, METRIC_SCOPE);
    syncErrors = manager.counter(registry, "errors", scope, METRIC_SCOPE);
    syncSkipped = manager.counter(registry, "skipped", scope, METRIC_SCOPE);
    syncTime = manager.timer(null, registry, "time", scope, METRIC_SCOPE);
    syncErrors = manager.counter(null, registry, "errors", scope, METRIC_SCOPE);
    syncSkipped = manager.counter(null, registry, "skipped", scope, METRIC_SCOPE);
   }
 
   /** optional list of updates we had before possibly receiving new updates */
diff --git a/solr/core/src/java/org/apache/solr/update/SolrIndexWriter.java b/solr/core/src/java/org/apache/solr/update/SolrIndexWriter.java
index ed856040ec1..0315b495484 100644
-- a/solr/core/src/java/org/apache/solr/update/SolrIndexWriter.java
++ b/solr/core/src/java/org/apache/solr/update/SolrIndexWriter.java
@@ -39,7 +39,7 @@ import org.apache.solr.common.util.SuppressForbidden;
 import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.DirectoryFactory.DirContext;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.schema.IndexSchema;
 import org.slf4j.Logger;
@@ -151,20 +151,20 @@ public class SolrIndexWriter extends IndexWriter {
       }
       if (mergeDetails) {
         mergeTotals = true; // override
        majorMergedDocs = metricManager.meter(registry, "docs", SolrInfoMBean.Category.INDEX.toString(), "merge", "major");
        majorDeletedDocs = metricManager.meter(registry, "deletedDocs", SolrInfoMBean.Category.INDEX.toString(), "merge", "major");
        majorMergedDocs = metricManager.meter(null, registry, "docs", SolrInfoBean.Category.INDEX.toString(), "merge", "major");
        majorDeletedDocs = metricManager.meter(null, registry, "deletedDocs", SolrInfoBean.Category.INDEX.toString(), "merge", "major");
       }
       if (mergeTotals) {
        minorMerge = metricManager.timer(registry, "minor", SolrInfoMBean.Category.INDEX.toString(), "merge");
        majorMerge = metricManager.timer(registry, "major", SolrInfoMBean.Category.INDEX.toString(), "merge");
        mergeErrors = metricManager.counter(registry, "errors", SolrInfoMBean.Category.INDEX.toString(), "merge");
        metricManager.registerGauge(registry, () -> runningMajorMerges.get(), true, "running", SolrInfoMBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(registry, () -> runningMinorMerges.get(), true, "running", SolrInfoMBean.Category.INDEX.toString(), "merge", "minor");
        metricManager.registerGauge(registry, () -> runningMajorMergesDocs.get(), true, "running.docs", SolrInfoMBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(registry, () -> runningMinorMergesDocs.get(), true, "running.docs", SolrInfoMBean.Category.INDEX.toString(), "merge", "minor");
        metricManager.registerGauge(registry, () -> runningMajorMergesSegments.get(), true, "running.segments", SolrInfoMBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(registry, () -> runningMinorMergesSegments.get(), true, "running.segments", SolrInfoMBean.Category.INDEX.toString(), "merge", "minor");
        flushMeter = metricManager.meter(registry, "flush", SolrInfoMBean.Category.INDEX.toString());
        minorMerge = metricManager.timer(null, registry, "minor", SolrInfoBean.Category.INDEX.toString(), "merge");
        majorMerge = metricManager.timer(null, registry, "major", SolrInfoBean.Category.INDEX.toString(), "merge");
        mergeErrors = metricManager.counter(null, registry, "errors", SolrInfoBean.Category.INDEX.toString(), "merge");
        metricManager.registerGauge(null, registry, () -> runningMajorMerges.get(), true, "running", SolrInfoBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(null, registry, () -> runningMinorMerges.get(), true, "running", SolrInfoBean.Category.INDEX.toString(), "merge", "minor");
        metricManager.registerGauge(null, registry, () -> runningMajorMergesDocs.get(), true, "running.docs", SolrInfoBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(null, registry, () -> runningMinorMergesDocs.get(), true, "running.docs", SolrInfoBean.Category.INDEX.toString(), "merge", "minor");
        metricManager.registerGauge(null, registry, () -> runningMajorMergesSegments.get(), true, "running.segments", SolrInfoBean.Category.INDEX.toString(), "merge", "major");
        metricManager.registerGauge(null, registry, () -> runningMinorMergesSegments.get(), true, "running.segments", SolrInfoBean.Category.INDEX.toString(), "merge", "minor");
        flushMeter = metricManager.meter(null, registry, "flush", SolrInfoBean.Category.INDEX.toString());
       }
     }
   }
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
index cbfb0d5f1fc..49d2664c649 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateHandler.java
@@ -19,14 +19,17 @@ package org.apache.solr.update;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
 import java.util.Vector;
 
import com.codahale.metrics.MetricRegistry;
 import org.apache.solr.core.DirectoryFactory;
 import org.apache.solr.core.HdfsDirectoryFactory;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.core.SolrEventListener;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.schema.FieldType;
 import org.apache.solr.schema.SchemaField;
 import org.apache.solr.util.plugin.SolrCoreAware;
@@ -41,7 +44,7 @@ import org.slf4j.LoggerFactory;
  * @since solr 0.9
  */
 
public abstract class UpdateHandler implements SolrInfoMBean {
public abstract class UpdateHandler implements SolrInfoBean {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
   protected final SolrCore core;
@@ -55,6 +58,9 @@ public abstract class UpdateHandler implements SolrInfoMBean {
 
   protected final UpdateLog ulog;
 
  protected Set<String> metricNames = new HashSet<>();
  protected MetricRegistry registry;

   private void parseEventListeners() {
     final Class<SolrEventListener> clazz = SolrEventListener.class;
     final String label = "Event Listener";
@@ -221,4 +227,12 @@ public abstract class UpdateHandler implements SolrInfoMBean {
   public Category getCategory() {
     return Category.UPDATE;
   }
  @Override
  public Set<String> getMetricNames() {
    return metricNames;
  }
  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateLog.java b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
index 84a20052b99..c50add4a45e 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateLog.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateLog.java
@@ -57,7 +57,7 @@ import org.apache.solr.common.util.ExecutorUtil;
 import org.apache.solr.common.util.IOUtils;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.request.LocalSolrQueryRequest;
@@ -403,7 +403,7 @@ public static final int VERSION_IDX = 1;
       }
 
     }
    core.getCoreMetricManager().registerMetricProducer(SolrInfoMBean.Category.TLOG.toString(), this);
    core.getCoreMetricManager().registerMetricProducer(SolrInfoBean.Category.TLOG.toString(), this);
   }
 
   @Override
@@ -422,12 +422,12 @@ public static final int VERSION_IDX = 1;
       }
     };
 
    manager.registerGauge(registry, bufferedOpsGauge, true, "ops", scope, "buffered");
    manager.registerGauge(registry, () -> logs.size(), true, "logs", scope, "replay", "remaining");
    manager.registerGauge(registry, () -> getTotalLogsSize(), true, "bytes", scope, "replay", "remaining");
    applyingBufferedOpsMeter = manager.meter(registry, "ops", scope, "applyingBuffered");
    replayOpsMeter = manager.meter(registry, "ops", scope, "replay");
    manager.registerGauge(registry, () -> state.getValue(), true, "state", scope);
    manager.registerGauge(null, registry, bufferedOpsGauge, true, "ops", scope, "buffered");
    manager.registerGauge(null, registry, () -> logs.size(), true, "logs", scope, "replay", "remaining");
    manager.registerGauge(null, registry, () -> getTotalLogsSize(), true, "bytes", scope, "replay", "remaining");
    applyingBufferedOpsMeter = manager.meter(null, registry, "ops", scope, "applyingBuffered");
    replayOpsMeter = manager.meter(null, registry, "ops", scope, "replay");
    manager.registerGauge(null, registry, () -> state.getValue(), true, "state", scope);
   }
 
   /**
diff --git a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
index 9d4eb7d1eed..ca8cea5a162 100644
-- a/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
++ b/solr/core/src/java/org/apache/solr/update/UpdateShardHandler.java
@@ -17,10 +17,11 @@
 package org.apache.solr.update;
 
 import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
 import java.util.concurrent.ExecutorService;
 
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
 import org.apache.http.client.HttpClient;
 import org.apache.http.impl.client.CloseableHttpClient;
 import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
@@ -29,20 +30,20 @@ import org.apache.solr.cloud.RecoveryStrategy;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SolrjNamedThreadFactory;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.util.stats.HttpClientMetricNameStrategy;
 import org.apache.solr.util.stats.InstrumentedHttpRequestExecutor;
 import org.apache.solr.util.stats.InstrumentedPoolingHttpClientConnectionManager;
import org.apache.solr.util.stats.MetricUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import static org.apache.solr.util.stats.InstrumentedHttpRequestExecutor.KNOWN_METRIC_NAME_STRATEGIES;
 
public class UpdateShardHandler implements SolrMetricProducer, SolrInfoMBean {
public class UpdateShardHandler implements SolrMetricProducer, SolrInfoBean {
   
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
@@ -65,6 +66,9 @@ public class UpdateShardHandler implements SolrMetricProducer, SolrInfoMBean {
 
   private final InstrumentedHttpRequestExecutor httpRequestExecutor;
 
  private final Set<String> metricNames = new HashSet<>();
  private MetricRegistry registry;

   public UpdateShardHandler(UpdateShardHandlerConfig cfg) {
     clientConnectionManager = new InstrumentedPoolingHttpClientConnectionManager(HttpClientUtil.getSchemaRegisteryProvider().getSchemaRegistry());
     if (cfg != null ) {
@@ -104,20 +108,14 @@ public class UpdateShardHandler implements SolrMetricProducer, SolrInfoMBean {
   }
 
   @Override
  public String getVersion() {
    return getClass().getPackage().getSpecificationVersion();
  }

  @Override
  public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
     String expandedScope = SolrMetricManager.mkName(scope, getCategory().name());
    clientConnectionManager.initializeMetrics(manager, registry, expandedScope);
    httpRequestExecutor.initializeMetrics(manager, registry, expandedScope);
    updateExecutor = new InstrumentedExecutorService(updateExecutor,
        manager.registry(registry),
    clientConnectionManager.initializeMetrics(manager, registryName, expandedScope);
    httpRequestExecutor.initializeMetrics(manager, registryName, expandedScope);
    updateExecutor = MetricUtils.instrumentedExecutorService(updateExecutor, this, registry,
         SolrMetricManager.mkName("updateExecutor", expandedScope, "threadPool"));
    recoveryExecutor = new InstrumentedExecutorService(recoveryExecutor,
        manager.registry(registry),
    recoveryExecutor = MetricUtils.instrumentedExecutorService(recoveryExecutor, this, registry,
         SolrMetricManager.mkName("recoveryExecutor", expandedScope, "threadPool"));
   }
 
@@ -132,18 +130,13 @@ public class UpdateShardHandler implements SolrMetricProducer, SolrInfoMBean {
   }
 
   @Override
  public String getSource() {
    return null;
  }

  @Override
  public URL[] getDocs() {
    return new URL[0];
  public Set<String> getMetricNames() {
    return metricNames;
   }
 
   @Override
  public NamedList getStatistics() {
    return null;
  public MetricRegistry getMetricRegistry() {
    return registry;
   }
 
   public HttpClient getHttpClient() {
diff --git a/solr/core/src/java/org/apache/solr/util/JmxUtil.java b/solr/core/src/java/org/apache/solr/util/JmxUtil.java
index 02a070d9691..f27a55e7efc 100644
-- a/solr/core/src/java/org/apache/solr/util/JmxUtil.java
++ b/solr/core/src/java/org/apache/solr/util/JmxUtil.java
@@ -27,9 +27,6 @@ import java.util.List;
 
 /**
  * Utility methods to find a MBeanServer.
 *
 * This was factored out from {@link org.apache.solr.core.JmxMonitoredMap}
 * and can eventually replace the logic used there.
  */
 public final class JmxUtil {
 
diff --git a/solr/core/src/java/org/apache/solr/util/stats/InstrumentedPoolingHttpClientConnectionManager.java b/solr/core/src/java/org/apache/solr/util/stats/InstrumentedPoolingHttpClientConnectionManager.java
index 7bcabf8c255..58ec69e0c9b 100644
-- a/solr/core/src/java/org/apache/solr/util/stats/InstrumentedPoolingHttpClientConnectionManager.java
++ b/solr/core/src/java/org/apache/solr/util/stats/InstrumentedPoolingHttpClientConnectionManager.java
@@ -35,10 +35,10 @@ public class InstrumentedPoolingHttpClientConnectionManager extends PoolingHttpC
 
   @Override
   public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
    manager.registerGauge(registry, () -> getTotalStats().getAvailable(), true, SolrMetricManager.mkName("availableConnections", scope));
    manager.registerGauge(null, registry, () -> getTotalStats().getAvailable(), true, SolrMetricManager.mkName("availableConnections", scope));
     // this acquires a lock on the connection pool; remove if contention sucks
    manager.registerGauge(registry, () -> getTotalStats().getLeased(), true, SolrMetricManager.mkName("leasedConnections", scope));
    manager.registerGauge(registry, () -> getTotalStats().getMax(), true, SolrMetricManager.mkName("maxConnections", scope));
    manager.registerGauge(registry, () -> getTotalStats().getPending(), true, SolrMetricManager.mkName("pendingConnections", scope));
    manager.registerGauge(null, registry, () -> getTotalStats().getLeased(), true, SolrMetricManager.mkName("leasedConnections", scope));
    manager.registerGauge(null, registry, () -> getTotalStats().getMax(), true, SolrMetricManager.mkName("maxConnections", scope));
    manager.registerGauge(null, registry, () -> getTotalStats().getPending(), true, SolrMetricManager.mkName("pendingConnections", scope));
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/util/stats/MetricUtils.java b/solr/core/src/java/org/apache/solr/util/stats/MetricUtils.java
index 491932d1a8d..2900857a912 100644
-- a/solr/core/src/java/org/apache/solr/util/stats/MetricUtils.java
++ b/solr/core/src/java/org/apache/solr/util/stats/MetricUtils.java
@@ -16,9 +16,17 @@
  */
 package org.apache.solr.util.stats;
 
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
 import java.lang.invoke.MethodHandles;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
 import java.util.LinkedHashMap;
import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.SortedSet;
@@ -40,6 +48,7 @@ import com.codahale.metrics.Timer;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.AggregateMetric;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -116,59 +125,42 @@ public class MetricUtils {
    *                        A metric <em>must</em> match this filter to be included in the output.
    * @param skipHistograms discard any {@link Histogram}-s and histogram parts of {@link Timer}-s.
    * @param compact use compact representation for counters and gauges.
   * @param metadata optional metadata. If not null and not empty then this map will be added under a
   *                 {@code _metadata_} key.
    * @return a {@link NamedList}
    */
   public static NamedList toNamedList(MetricRegistry registry, List<MetricFilter> shouldMatchFilters,
                                       MetricFilter mustMatchFilter, boolean skipHistograms,
                                      boolean skipAggregateValues, boolean compact,
                                      Map<String, Object> metadata) {
                                      boolean skipAggregateValues, boolean compact) {
     NamedList result = new SimpleOrderedMap();
    toMaps(registry, shouldMatchFilters, mustMatchFilter, skipHistograms, skipAggregateValues, compact, (k, v) -> {
    toMaps(registry, shouldMatchFilters, mustMatchFilter, skipHistograms, skipAggregateValues, compact, false, (k, v) -> {
       result.add(k, v);
     });
    if (metadata != null && !metadata.isEmpty()) {
      result.add("_metadata_", metadata);
    }
     return result;
   }
 
   /**
   * Returns a representation of the given metric registry as a list of {@link SolrInputDocument}-s.
   * Provides a representation of the given metric registry as {@link SolrInputDocument}-s.
    Only those metrics
   * are converted to NamedList which match at least one of the given MetricFilter instances.
   * are converted which match at least one of the given MetricFilter instances.
    *
   * @param registry      the {@link MetricRegistry} to be converted to NamedList
   * @param registry      the {@link MetricRegistry} to be converted
    * @param shouldMatchFilters a list of {@link MetricFilter} instances.
    *                           A metric must match <em>any one</em> of the filters from this list to be
    *                           included in the output
    * @param mustMatchFilter a {@link MetricFilter}.
    *                        A metric <em>must</em> match this filter to be included in the output.
    * @param skipHistograms discard any {@link Histogram}-s and histogram parts of {@link Timer}-s.
   * @param skipAggregateValues discard internal values of {@link AggregateMetric}-s.
    * @param compact use compact representation for counters and gauges.
    * @param metadata optional metadata. If not null and not empty then this map will be added under a
    *                 {@code _metadata_} key.
   * @return a list of {@link SolrInputDocument}-s
   * @param consumer consumer that accepts produced {@link SolrInputDocument}-s
    */
  public static List<SolrInputDocument> toSolrInputDocuments(MetricRegistry registry, List<MetricFilter> shouldMatchFilters,
                                                             MetricFilter mustMatchFilter, boolean skipHistograms,
                                                             boolean skipAggregateValues, boolean compact,
                                                             Map<String, Object> metadata) {
    List<SolrInputDocument> result = new LinkedList<>();
    toSolrInputDocuments(registry, shouldMatchFilters, mustMatchFilter, skipHistograms,
        skipAggregateValues, compact, metadata, doc -> {
      result.add(doc);
    });
    return result;
  }

   public static void toSolrInputDocuments(MetricRegistry registry, List<MetricFilter> shouldMatchFilters,
                                           MetricFilter mustMatchFilter, boolean skipHistograms,
                                           boolean skipAggregateValues, boolean compact,
                                           Map<String, Object> metadata, Consumer<SolrInputDocument> consumer) {
     boolean addMetadata = metadata != null && !metadata.isEmpty();
    toMaps(registry, shouldMatchFilters, mustMatchFilter, skipHistograms, skipAggregateValues, compact, (k, v) -> {
    toMaps(registry, shouldMatchFilters, mustMatchFilter, skipHistograms, skipAggregateValues, compact, false, (k, v) -> {
       SolrInputDocument doc = new SolrInputDocument();
       doc.setField(METRIC_NAME, k);
       toSolrInputDocument(null, doc, v);
@@ -179,7 +171,13 @@ public class MetricUtils {
     });
   }
 
  public static void toSolrInputDocument(String prefix, SolrInputDocument doc, Object o) {
  /**
   * Fill in a SolrInputDocument with values from a converted metric, recursively.
   * @param prefix prefix to add to generated field names, or null if none.
   * @param doc document to fill
   * @param o an instance of converted metric, either a Map or a flat Object
   */
  static void toSolrInputDocument(String prefix, SolrInputDocument doc, Object o) {
     if (!(o instanceof Map)) {
       String key = prefix != null ? prefix : VALUE;
       doc.addField(key, o);
@@ -196,77 +194,170 @@ public class MetricUtils {
     }
   }
 
  public static void toMaps(MetricRegistry registry, List<MetricFilter> shouldMatchFilters,
  /**
   * Convert selected metrics to maps or to flattened objects.
   * @param registry source of metrics
   * @param shouldMatchFilters metrics must match any of these filters
   * @param mustMatchFilter metrics must match this filter
   * @param skipHistograms discard any {@link Histogram}-s and histogram parts of {@link Timer}-s.
   * @param skipAggregateValues discard internal values of {@link AggregateMetric}-s.
   * @param compact use compact representation for counters and gauges.
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  static void toMaps(MetricRegistry registry, List<MetricFilter> shouldMatchFilters,
                             MetricFilter mustMatchFilter, boolean skipHistograms, boolean skipAggregateValues,
                            boolean compact,
                            boolean compact, boolean simple,
                             BiConsumer<String, Object> consumer) {
    Map<String, Metric> metrics = registry.getMetrics();
    SortedSet<String> names = registry.getNames();
    final Map<String, Metric> metrics = registry.getMetrics();
    final SortedSet<String> names = registry.getNames();
     names.stream()
         .filter(s -> shouldMatchFilters.stream().anyMatch(metricFilter -> metricFilter.matches(s, metrics.get(s))))
         .filter(s -> mustMatchFilter.matches(s, metrics.get(s)))
         .forEach(n -> {
           Metric metric = metrics.get(n);
          if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            consumer.accept(n, convertCounter(counter, compact));
          } else if (metric instanceof Gauge) {
            Gauge gauge = (Gauge) metric;
            try {
              consumer.accept(n, convertGauge(gauge, compact));
            } catch (InternalError ie) {
              if (n.startsWith("memory.") && ie.getMessage().contains("Memory Pool not found")) {
                LOG.warn("Error converting gauge '" + n + "', possible JDK bug: SOLR-10362", ie);
                consumer.accept(n, null);
              } else {
                throw ie;
              }
            }
          } else if (metric instanceof Meter) {
            Meter meter = (Meter) metric;
            consumer.accept(n, convertMeter(meter));
          } else if (metric instanceof Timer) {
            Timer timer = (Timer) metric;
            consumer.accept(n, convertTimer(timer, skipHistograms));
          } else if (metric instanceof Histogram) {
            if (!skipHistograms) {
              Histogram histogram = (Histogram) metric;
              consumer.accept(n, convertHistogram(histogram));
            }
          } else if (metric instanceof AggregateMetric) {
            consumer.accept(n, convertAggregateMetric((AggregateMetric)metric, skipAggregateValues));
          }
          convertMetric(n, metric, skipHistograms, skipAggregateValues, compact, simple, consumer);
         });
   }
 
  static Map<String, Object> convertAggregateMetric(AggregateMetric metric, boolean skipAggregateValues) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("count", metric.size());
    response.put(MAX, metric.getMax());
    response.put(MIN, metric.getMin());
    response.put(MEAN, metric.getMean());
    response.put(STDDEV, metric.getStdDev());
    response.put(SUM, metric.getSum());
    if (!(metric.isEmpty() || skipAggregateValues)) {
      Map<String, Object> values = new LinkedHashMap<>();
      response.put(VALUES, values);
      metric.getValues().forEach((k, v) -> {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", v.value);
        map.put("updateCount", v.updateCount.get());
        values.put(k, map);
      });
  /**
   * Convert selected metrics from a registry into a map, with metrics in a compact AND simple format.
   * @param registry registry
   * @param names metric names
   * @return map where keys are metric names (if they were present in the registry) and values are
   * converted metrics in simplified format.
   */
  public static Map<String, Object> convertMetrics(MetricRegistry registry, Collection<String> names) {
    final Map<String, Object> metrics = new HashMap<>();
    convertMetrics(registry, names, false, true, true, true, (k, v) -> metrics.put(k, v));
    return metrics;
  }

  /**
   * Convert selected metrics from a registry into maps (when <code>compact==false</code>) or
   * flattened objects.
   * @param registry registry
   * @param names metric names
   * @param skipHistograms discard any {@link Histogram}-s and histogram parts of {@link Timer}-s.
   * @param skipAggregateValues discard internal values of {@link AggregateMetric}-s.
   * @param compact use compact representation for counters and gauges.
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  public static void convertMetrics(MetricRegistry registry, Collection<String> names,
                                    boolean skipHistograms, boolean skipAggregateValues,
                                    boolean compact, boolean simple,
                                    BiConsumer<String, Object> consumer) {
    final Map<String, Metric> metrics = registry.getMetrics();
    names.stream()
        .forEach(n -> {
          Metric metric = metrics.get(n);
          convertMetric(n, metric, skipHistograms, skipAggregateValues, compact, simple, consumer);
        });
  }

  /**
   * Convert a single instance of metric into a map or flattened object.
   * @param n metric name
   * @param metric metric instance
   * @param skipHistograms discard any {@link Histogram}-s and histogram parts of {@link Timer}-s.
   * @param skipAggregateValues discard internal values of {@link AggregateMetric}-s.
   * @param compact use compact representation for counters and gauges.
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  static void convertMetric(String n, Metric metric, boolean skipHistograms, boolean skipAggregateValues,
                              boolean compact, boolean simple, BiConsumer<String, Object> consumer) {
    if (metric instanceof Counter) {
      Counter counter = (Counter) metric;
      consumer.accept(n, convertCounter(counter, compact));
    } else if (metric instanceof Gauge) {
      Gauge gauge = (Gauge) metric;
      try {
        convertGauge(n, gauge, simple, compact, consumer);
      } catch (InternalError ie) {
        if (n.startsWith("memory.") && ie.getMessage().contains("Memory Pool not found")) {
          LOG.warn("Error converting gauge '" + n + "', possible JDK bug: SOLR-10362", ie);
          consumer.accept(n, null);
        } else {
          throw ie;
        }
      }
    } else if (metric instanceof Meter) {
      Meter meter = (Meter) metric;
      convertMeter(n, meter, simple, consumer);
    } else if (metric instanceof Timer) {
      Timer timer = (Timer) metric;
      convertTimer(n, timer, skipHistograms, simple, consumer);
    } else if (metric instanceof Histogram) {
      if (!skipHistograms) {
        Histogram histogram = (Histogram) metric;
        convertHistogram(n, histogram, simple, consumer);
      }
    } else if (metric instanceof AggregateMetric) {
      convertAggregateMetric(n, (AggregateMetric)metric, skipAggregateValues, simple, consumer);
     }
    return response;
   }
 
  static Map<String, Object> convertHistogram(Histogram histogram) {
    Map<String, Object> response = new LinkedHashMap<>();
  /**
   * Convert an instance of {@link AggregateMetric}.
   * @param name metric name
   * @param metric an instance of {@link AggregateMetric}
   * @param skipAggregateValues discard internal values of {@link AggregateMetric}-s.
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  static void convertAggregateMetric(String name, AggregateMetric metric,
      boolean skipAggregateValues, boolean simple, BiConsumer<String, Object> consumer) {
    if (simple) {
      consumer.accept(name + "." + MEAN, metric.getMean());
    } else {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("count", metric.size());
      response.put(MAX, metric.getMax());
      response.put(MIN, metric.getMin());
      response.put(MEAN, metric.getMean());
      response.put(STDDEV, metric.getStdDev());
      response.put(SUM, metric.getSum());
      if (!(metric.isEmpty() || skipAggregateValues)) {
        Map<String, Object> values = new LinkedHashMap<>();
        response.put(VALUES, values);
        metric.getValues().forEach((k, v) -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("value", v.value);
          map.put("updateCount", v.updateCount.get());
          values.put(k, map);
        });
      }
      consumer.accept(name, response);
    }
  }

  /**
   * Convert an instance of {@link Histogram}. NOTE: it's assumed that histogram contains non-time
   * based values that don't require unit conversion.
   * @param name metric name
   * @param histogram an instance of {@link Histogram}
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  static void convertHistogram(String name, Histogram histogram,
                                              boolean simple, BiConsumer<String, Object> consumer) {
     Snapshot snapshot = histogram.getSnapshot();
    response.put("count", histogram.getCount());
    // non-time based values
    addSnapshot(response, snapshot, false);
    return response;
    if (simple) {
      consumer.accept(name + "." + MEAN, snapshot.getMean());
    } else {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("count", histogram.getCount());
      // non-time based values
      addSnapshot(response, snapshot, false);
      consumer.accept(name, response);
    }
   }
 
   // optionally convert ns to ms
@@ -291,40 +382,91 @@ public class MetricUtils {
     response.put((ms ? P999_MS: P999), nsToMs(ms, snapshot.get999thPercentile()));
   }
 
  static Map<String,Object> convertTimer(Timer timer, boolean skipHistograms) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("count", timer.getCount());
    response.put("meanRate", timer.getMeanRate());
    response.put("1minRate", timer.getOneMinuteRate());
    response.put("5minRate", timer.getFiveMinuteRate());
    response.put("15minRate", timer.getFifteenMinuteRate());
    if (!skipHistograms) {
      // time-based values in nanoseconds
      addSnapshot(response, timer.getSnapshot(), true);
  /**
   * Convert a {@link Timer} to a map.
   * @param name metric name
   * @param timer timer instance
   * @param skipHistograms if true then discard the histogram part of the timer.
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  public static void convertTimer(String name, Timer timer, boolean skipHistograms,
                                                boolean simple, BiConsumer<String, Object> consumer) {
    if (simple) {
      consumer.accept(name + ".meanRate", timer.getMeanRate());
    } else {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("count", timer.getCount());
      response.put("meanRate", timer.getMeanRate());
      response.put("1minRate", timer.getOneMinuteRate());
      response.put("5minRate", timer.getFiveMinuteRate());
      response.put("15minRate", timer.getFifteenMinuteRate());
      if (!skipHistograms) {
        // time-based values in nanoseconds
        addSnapshot(response, timer.getSnapshot(), true);
      }
      consumer.accept(name, response);
     }
    return response;
   }
 
  static Map<String, Object> convertMeter(Meter meter) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("count", meter.getCount());
    response.put("meanRate", meter.getMeanRate());
    response.put("1minRate", meter.getOneMinuteRate());
    response.put("5minRate", meter.getFiveMinuteRate());
    response.put("15minRate", meter.getFifteenMinuteRate());
    return response;
  /**
   * Convert a {@link Meter} to a map.
   * @param name metric name
   * @param meter meter instance
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param consumer consumer that accepts produced objects
   */
  static void convertMeter(String name, Meter meter, boolean simple, BiConsumer<String, Object> consumer) {
    if (simple) {
      consumer.accept(name + ".count", meter.getCount());
    } else {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("count", meter.getCount());
      response.put("meanRate", meter.getMeanRate());
      response.put("1minRate", meter.getOneMinuteRate());
      response.put("5minRate", meter.getFiveMinuteRate());
      response.put("15minRate", meter.getFifteenMinuteRate());
      consumer.accept(name, response);
    }
   }
 
  static Object convertGauge(Gauge gauge, boolean compact) {
    if (compact) {
      return gauge.getValue();
  /**
   * Convert a {@link Gauge}.
   * @param name metric name
   * @param gauge gauge instance
   * @param simple use simplified representation for complex metrics - instead of a (name, map)
   *             only the selected (name "." key, value) pairs will be produced.
   * @param compact if true then only return {@link Gauge#getValue()}. If false
   *                then return a map with a "value" field.
   * @param consumer consumer that accepts produced objects
   */
  static void convertGauge(String name, Gauge gauge, boolean simple, boolean compact,
                             BiConsumer<String, Object> consumer) {
    if (compact || simple) {
      Object o = gauge.getValue();
      if (simple && (o instanceof Map)) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>)o).entrySet()) {
          consumer.accept(name + "." + entry.getKey().toString(), entry.getValue());
        }
      } else {
        consumer.accept(name, o);
      }
     } else {
       Map<String, Object> response = new LinkedHashMap<>();
       response.put("value", gauge.getValue());
      return response;
      consumer.accept(name, response);
     }
   }
 
  /**
   * Convert a {@link Counter}
   * @param counter counter instance
   * @param compact if true then only return {@link Counter#getCount()}. If false
   *                then return a map with a "count" field.
   * @return map or object
   */
   static Object convertCounter(Counter counter, boolean compact) {
     if (compact) {
       return counter.getCount();
@@ -338,7 +480,88 @@ public class MetricUtils {
   /**
    * Returns an instrumented wrapper over the given executor service.
    */
  public static ExecutorService instrumentedExecutorService(ExecutorService delegate, MetricRegistry metricRegistry, String scope)  {
  public static ExecutorService instrumentedExecutorService(ExecutorService delegate, SolrInfoBean info, MetricRegistry metricRegistry, String scope)  {
    if (info != null && info.getMetricNames() != null) {
      info.getMetricNames().add(MetricRegistry.name(scope, "submitted"));
      info.getMetricNames().add(MetricRegistry.name(scope, "running"));
      info.getMetricNames().add(MetricRegistry.name(scope, "completed"));
      info.getMetricNames().add(MetricRegistry.name(scope, "duration"));
    }
     return new InstrumentedExecutorService(delegate, metricRegistry, scope);
   }

  /**
   * Creates a set of metrics (gauges) that correspond to available bean properties for the provided MXBean.
   * @param obj an instance of MXBean
   * @param intf MXBean interface, one of {@link PlatformManagedObject}-s
   * @param consumer consumer for created names and metrics
   * @param <T> formal type
   */
  public static <T extends PlatformManagedObject> void addMXBeanMetrics(T obj, Class<? extends T> intf,
      String prefix, BiConsumer<String, Metric> consumer) {
    if (intf.isInstance(obj)) {
      BeanInfo beanInfo;
      try {
        beanInfo = Introspector.getBeanInfo(intf, intf.getSuperclass(), Introspector.IGNORE_ALL_BEANINFO);
      } catch (IntrospectionException e) {
        LOG.warn("Unable to fetch properties of MXBean " + obj.getClass().getName());
        return;
      }
      for (final PropertyDescriptor desc : beanInfo.getPropertyDescriptors()) {
        final String name = desc.getName();
        // test if it works at all
        try {
          desc.getReadMethod().invoke(obj);
          // worked - consume it
          final Gauge<?> gauge = () -> {
            try {
              return desc.getReadMethod().invoke(obj);
            } catch (InvocationTargetException ite) {
              // ignore (some properties throw UOE)
              return null;
            } catch (IllegalAccessException e) {
              return null;
            }
          };
          String metricName = MetricRegistry.name(prefix, name);
          consumer.accept(metricName, gauge);
        } catch (Exception e) {
          // didn't work, skip it...
        }
      }
    }
  }

  /**
   * These are well-known implementations of {@link java.lang.management.OperatingSystemMXBean}.
   * Some of them provide additional useful properties beyond those declared by the interface.
   */
  public static String[] OS_MXBEAN_CLASSES = new String[] {
      OperatingSystemMXBean.class.getName(),
      "com.sun.management.OperatingSystemMXBean",
      "com.sun.management.UnixOperatingSystemMXBean",
      "com.ibm.lang.management.OperatingSystemMXBean"
  };

  /**
   * Creates a set of metrics (gauges) that correspond to available bean properties for the provided MXBean.
   * @param obj an instance of MXBean
   * @param interfaces interfaces that it may implement. Each interface will be tried in turn, and only
   *                   if it exists and if it contains unique properties then they will be added as metrics.
   * @param prefix optional prefix for metric names
   * @param consumer consumer for created names and metrics
   * @param <T> formal type
   */
  public static <T extends PlatformManagedObject> void addMXBeanMetrics(T obj, String[] interfaces,
      String prefix, BiConsumer<String, Metric> consumer) {
    for (String clazz : interfaces) {
      try {
        final Class<? extends PlatformManagedObject> intf = Class.forName(clazz)
            .asSubclass(PlatformManagedObject.class);
        MetricUtils.addMXBeanMetrics(obj, intf, null, consumer);
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }
  }
 }
diff --git a/solr/core/src/test-files/solr/solr-jmxreporter.xml b/solr/core/src/test-files/solr/solr-jmxreporter.xml
new file mode 100644
index 00000000000..bb9d05de142
-- /dev/null
++ b/solr/core/src/test-files/solr/solr-jmxreporter.xml
@@ -0,0 +1,43 @@
<?xml version="1.0" encoding="UTF-8" ?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<solr>
  <shardHandlerFactory name="shardHandlerFactory" class="HttpShardHandlerFactory">
    <str name="urlScheme">${urlScheme:}</str>
    <int name="socketTimeout">${socketTimeout:90000}</int>
    <int name="connTimeout">${connTimeout:15000}</int>
  </shardHandlerFactory>

  <solrcloud>
    <str name="host">127.0.0.1</str>
    <int name="hostPort">${hostPort:8983}</int>
    <str name="hostContext">${hostContext:solr}</str>
    <int name="zkClientTimeout">${solr.zkclienttimeout:30000}</int>
    <bool name="genericCoreNodeNames">${genericCoreNodeNames:true}</bool>
    <int name="leaderVoteWait">${leaderVoteWait:10000}</int>
    <int name="distribUpdateConnTimeout">${distribUpdateConnTimeout:45000}</int>
    <int name="distribUpdateSoTimeout">${distribUpdateSoTimeout:340000}</int>
    <int name="autoReplicaFailoverWaitAfterExpiration">${autoReplicaFailoverWaitAfterExpiration:10000}</int>
    <int name="autoReplicaFailoverWorkLoopDelay">${autoReplicaFailoverWorkLoopDelay:10000}</int>
    <int name="autoReplicaFailoverBadNodeExpiration">${autoReplicaFailoverBadNodeExpiration:60000}</int>
  </solrcloud>

  <metrics>
    <reporter name="defaultJmx" class="org.apache.solr.metrics.reporters.SolrJmxReporter"/>
  </metrics>
</solr>
diff --git a/solr/core/src/test-files/solr/solr-solrreporter.xml b/solr/core/src/test-files/solr/solr-solrreporter.xml
index db03e421887..a66d9d096e1 100644
-- a/solr/core/src/test-files/solr/solr-solrreporter.xml
++ b/solr/core/src/test-files/solr/solr-solrreporter.xml
@@ -38,6 +38,10 @@
   </solrcloud>
 
   <metrics>
    <!-- disable default JMX reporter to avoid conflicts with multiple CoreContainers. -->
    <reporter name="defaultJmx" class="org.apache.solr.metrics.reporters.SolrJmxReporter">
      <bool name="enabled">false</bool>
    </reporter>
     <reporter name="test" group="shard">
       <int name="period">5</int>
       <str name="filter">UPDATE\./update/.*requests</str>
diff --git a/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java b/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
index f4a14dba419..199f682811b 100644
-- a/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
++ b/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
@@ -27,6 +27,8 @@ import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.LazyDocument;
@@ -38,6 +40,7 @@ import org.apache.solr.common.params.SolrParams;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestHandler;
@@ -122,10 +125,14 @@ public class BasicFunctionalityTest extends SolrTestCaseJ4 {
     assertNotNull(core.getRequestHandler("mock"));
 
     // test stats call
    NamedList stats = core.getStatistics();
    assertEquals("collection1", stats.get("coreName"));
    assertTrue(stats.get("refCount") != null);
    
    SolrMetricManager manager = core.getCoreDescriptor().getCoreContainer().getMetricManager();
    String registry = core.getCoreMetricManager().getRegistryName();
    Map<String, Metric> metrics = manager.registry(registry).getMetrics();
    assertTrue(metrics.containsKey("CORE.coreName"));
    assertTrue(metrics.containsKey("CORE.refCount"));
    Gauge<Number> g = (Gauge<Number>)metrics.get("CORE.refCount");
    assertTrue(g.getValue().intValue() > 0);

     lrf.args.put(CommonParams.VERSION,"2.2");
     assertQ("test query on empty index",
             req("qlkciyopsbgzyvkylsjhchghjrdf")
@@ -378,8 +385,6 @@ public class BasicFunctionalityTest extends SolrTestCaseJ4 {
         @Override
         public String getDescription() { return tmp; }
         @Override
        public String getSource() { return tmp; }
        @Override
         public void handleRequestBody
           ( SolrQueryRequest req, SolrQueryResponse rsp ) {
           throw new RuntimeException(tmp);
diff --git a/solr/core/src/test/org/apache/solr/CursorPagingTest.java b/solr/core/src/test/org/apache/solr/CursorPagingTest.java
index b204677f8c5..eb1c6bc6fe9 100644
-- a/solr/core/src/test/org/apache/solr/CursorPagingTest.java
++ b/solr/core/src/test/org/apache/solr/CursorPagingTest.java
@@ -19,7 +19,6 @@ package org.apache.solr;
 import org.apache.lucene.util.TestUtil;
 import org.apache.lucene.util.SentinelIntSet;
 import org.apache.lucene.util.mutable.MutableValueInt;
import org.apache.solr.core.SolrInfoMBean;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.params.CursorMarkParams;
 import org.apache.solr.common.params.SolrParams;
@@ -32,6 +31,7 @@ import static org.apache.solr.common.params.CursorMarkParams.CURSOR_MARK_START;
 
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.search.CursorMark; //jdoc
 import org.noggit.ObjectBuilder;
@@ -521,16 +521,16 @@ public class CursorPagingTest extends SolrTestCaseJ4 {
 
     final Collection<String> allFieldNames = getAllSortFieldNames();
 
    final SolrInfoMBean filterCacheStats 
      = h.getCore().getInfoRegistry().get("filterCache");
    final MetricsMap filterCacheStats =
        (MetricsMap)h.getCore().getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.filterCache");
     assertNotNull(filterCacheStats);
    final SolrInfoMBean queryCacheStats 
      = h.getCore().getInfoRegistry().get("queryResultCache");
    final MetricsMap queryCacheStats =
        (MetricsMap)h.getCore().getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.queryResultCache");
     assertNotNull(queryCacheStats);
 
    final long preQcIn = (Long) queryCacheStats.getStatistics().get("inserts");
    final long preFcIn = (Long) filterCacheStats.getStatistics().get("inserts");
    final long preFcHits = (Long) filterCacheStats.getStatistics().get("hits");
    final long preQcIn = (Long) queryCacheStats.getValue().get("inserts");
    final long preFcIn = (Long) filterCacheStats.getValue().get("inserts");
    final long preFcHits = (Long) filterCacheStats.getValue().get("hits");
 
     SentinelIntSet ids = assertFullWalkNoDups
       (10, params("q", "*:*",
@@ -542,9 +542,9 @@ public class CursorPagingTest extends SolrTestCaseJ4 {
     
     assertEquals(6, ids.size());
 
    final long postQcIn = (Long) queryCacheStats.getStatistics().get("inserts");
    final long postFcIn = (Long) filterCacheStats.getStatistics().get("inserts");
    final long postFcHits = (Long) filterCacheStats.getStatistics().get("hits");
    final long postQcIn = (Long) queryCacheStats.getValue().get("inserts");
    final long postFcIn = (Long) filterCacheStats.getValue().get("inserts");
    final long postFcHits = (Long) filterCacheStats.getValue().get("hits");
     
     assertEquals("query cache inserts changed", preQcIn, postQcIn);
     // NOTE: use of pure negative filters causees "*:* to be tracked in filterCache
diff --git a/solr/core/src/test/org/apache/solr/SolrInfoMBeanTest.java b/solr/core/src/test/org/apache/solr/SolrInfoBeanTest.java
similarity index 82%
rename from solr/core/src/test/org/apache/solr/SolrInfoMBeanTest.java
rename to solr/core/src/test/org/apache/solr/SolrInfoBeanTest.java
index bfe231627ad..d39c87fad0b 100644
-- a/solr/core/src/test/org/apache/solr/SolrInfoMBeanTest.java
++ b/solr/core/src/test/org/apache/solr/SolrInfoBeanTest.java
@@ -16,11 +16,14 @@
  */
 package org.apache.solr;
 
import org.apache.solr.core.SolrInfoMBean;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.handler.StandardRequestHandler;
 import org.apache.solr.handler.admin.LukeRequestHandler;
 import org.apache.solr.handler.component.SearchComponent;
 import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;
 import org.apache.solr.search.LRUCache;
 import org.junit.BeforeClass;
 import java.io.File;
@@ -33,7 +36,7 @@ import java.util.List;
 /**
  * A simple test used to increase code coverage for some standard things...
  */
public class SolrInfoMBeanTest extends SolrTestCaseJ4
public class SolrInfoBeanTest extends SolrTestCaseJ4
 {
   @BeforeClass
   public static void beforeClass() throws Exception {
@@ -54,10 +57,16 @@ public class SolrInfoMBeanTest extends SolrTestCaseJ4
    // System.out.println(classes);
     
     int checked = 0;
    SolrMetricManager metricManager = h.getCoreContainer().getMetricManager();
    String registry = h.getCore().getCoreMetricManager().getRegistryName();
    String scope = TestUtil.randomSimpleString(random(), 2, 10);
     for( Class clazz : classes ) {
      if( SolrInfoMBean.class.isAssignableFrom( clazz ) ) {
      if( SolrInfoBean.class.isAssignableFrom( clazz ) ) {
         try {
          SolrInfoMBean info = (SolrInfoMBean)clazz.newInstance();
          SolrInfoBean info = (SolrInfoBean)clazz.newInstance();
          if (info instanceof SolrMetricProducer) {
            ((SolrMetricProducer)info).initializeMetrics(metricManager, registry, scope);
          }
           
           //System.out.println( info.getClass() );
           assertNotNull( info.getName() );
@@ -69,9 +78,6 @@ public class SolrInfoMBeanTest extends SolrTestCaseJ4
           }
           
           assertNotNull( info.toString() );
          // increase code coverage...
          assertNotNull( info.getDocs() + "" );
          assertNotNull( info.getStatistics()+"" );
           checked++;
         }
         catch( InstantiationException ex ) {
@@ -80,7 +86,7 @@ public class SolrInfoMBeanTest extends SolrTestCaseJ4
         }
       }
     }
    assertTrue( "there are at least 10 SolrInfoMBean that should be found in the classpath, found " + checked, checked > 10 );
    assertTrue( "there are at least 10 SolrInfoBean that should be found in the classpath, found " + checked, checked > 10 );
   }
   
   private static List<Class> getClassesForPackage(String pckgname) throws Exception {
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index d1dbe9c4319..1c23c9cf678 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -59,6 +59,7 @@ import org.slf4j.LoggerFactory;
 
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
@@ -752,19 +753,28 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
 
   private Long getNumCommits(HttpSolrClient sourceClient) throws
       SolrServerException, IOException {
    try (HttpSolrClient client = getHttpSolrClient(sourceClient.getBaseURL())) {
    // construct the /admin/metrics URL
    URL url = new URL(sourceClient.getBaseURL());
    String path = url.getPath().substring(1);
    String[] elements = path.split("/");
    String collection = elements[elements.length - 1];
    String urlString = url.toString();
    urlString = urlString.substring(0, urlString.length() - collection.length() - 1);
    try (HttpSolrClient client = getHttpSolrClient(urlString)) {
       client.setConnectionTimeout(15000);
       client.setSoTimeout(60000);
       ModifiableSolrParams params = new ModifiableSolrParams();
      params.set("qt", "/admin/mbeans?key=updateHandler&stats=true");
      //params.set("qt", "/admin/metrics?prefix=UPDATE.updateHandler&registry=solr.core." + collection);
      params.set("qt", "/admin/metrics");
      params.set("prefix", "UPDATE.updateHandler");
      params.set("registry", "solr.core." + collection);
       // use generic request to avoid extra processing of queries
       QueryRequest req = new QueryRequest(params);
       NamedList<Object> resp = client.request(req);
      NamedList mbeans = (NamedList) resp.get("solr-mbeans");
      NamedList uhandlerCat = (NamedList) mbeans.get("UPDATE");
      NamedList uhandler = (NamedList) uhandlerCat.get("updateHandler");
      NamedList stats = (NamedList) uhandler.get("stats");
      return (Long) stats.get("commits");
      NamedList metrics = (NamedList) resp.get("metrics");
      NamedList uhandlerCat = (NamedList) metrics.getVal(0);
      Map<String,Object> commits = (Map<String,Object>) uhandlerCat.get("UPDATE.updateHandler.commits");
      return (Long) commits.get("count");
     }
   }
 
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicZkTest.java
index 26fa3257398..f48f76b5736 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicZkTest.java
@@ -16,11 +16,14 @@
  */
 package org.apache.solr.cloud;
 
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
 import org.apache.lucene.util.LuceneTestCase.Slow;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.CommonParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.request.LocalSolrQueryRequest;
 import org.apache.solr.request.SolrQueryRequest;
@@ -158,11 +161,11 @@ public class BasicZkTest extends AbstractZkTestCase {
     }
     
     // test stats call
    NamedList stats = core.getStatistics();
    assertEquals("collection1", stats.get("coreName"));
    assertEquals("collection1", stats.get("collection"));
    assertEquals("shard1", stats.get("shard"));
    assertTrue(stats.get("refCount") != null);
    Map<String, Metric> metrics = h.getCore().getCoreMetricManager().getRegistry().getMetrics();
    assertEquals("collection1", ((Gauge)metrics.get("CORE.coreName")).getValue());
    assertEquals("collection1", ((Gauge)metrics.get("CORE.collection")).getValue());
    assertEquals("shard1", ((Gauge)metrics.get("CORE.shard")).getValue());
    assertTrue(metrics.get("CORE.refCount") != null);
 
     //zkController.getZkClient().printLayoutToStdOut();
   }
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
index 7925358e6d2..ed9ed41b011 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
@@ -20,6 +20,7 @@ import javax.management.MBeanServer;
 import javax.management.MBeanServerFactory;
 import javax.management.ObjectName;
 import java.io.IOException;
import java.lang.invoke.MethodHandles;
 import java.lang.management.ManagementFactory;
 import java.nio.file.Files;
 import java.nio.file.Path;
@@ -37,6 +38,7 @@ import java.util.Set;
 import java.util.concurrent.TimeUnit;
 
 import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
 import org.apache.lucene.util.LuceneTestCase.Slow;
 import org.apache.lucene.util.TestUtil;
 import org.apache.solr.client.solrj.SolrClient;
@@ -68,12 +70,14 @@ import org.apache.solr.common.util.NamedList;
 import org.apache.solr.common.util.SimpleOrderedMap;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean.Category;
import org.apache.solr.core.SolrInfoBean.Category;
 import org.apache.solr.util.TestInjection;
 import org.apache.solr.util.TimeOut;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 import static org.apache.solr.common.cloud.ZkStateReader.CORE_NAME_PROP;
 import static org.apache.solr.common.cloud.ZkStateReader.REPLICATION_FACTOR;
@@ -83,6 +87,7 @@ import static org.apache.solr.common.cloud.ZkStateReader.REPLICATION_FACTOR;
  */
 @Slow
 public class CollectionsAPIDistributedZkTest extends SolrCloudTestCase {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
   @BeforeClass
   public static void beforeCollectionsAPIDistributedZkTest() {
@@ -94,9 +99,11 @@ public class CollectionsAPIDistributedZkTest extends SolrCloudTestCase {
 
   @BeforeClass
   public static void setupCluster() throws Exception {
    String solrXml = IOUtils.toString(CollectionsAPIDistributedZkTest.class.getResourceAsStream("/solr/solr-jmxreporter.xml"), "UTF-8");
     configureCluster(4)
         .addConfig("conf", configset("cloud-minimal"))
         .addConfig("conf2", configset("cloud-minimal-jmx"))
        .withSolrXml(solrXml)
         .configure();
   }
 
@@ -549,7 +556,7 @@ public class CollectionsAPIDistributedZkTest extends SolrCloudTestCase {
     for (SolrCore core : theCores) {
 
       // look for core props file
      Path instancedir = (Path) core.getStatistics().get("instanceDir");
      Path instancedir = (Path) core.getResourceLoader().getInstancePath();
       assertTrue("Could not find expected core.properties file", Files.exists(instancedir.resolve("core.properties")));
 
       Path expected = Paths.get(jetty.getSolrHome()).toAbsolutePath().resolve(core.getName());
@@ -620,25 +627,22 @@ public class CollectionsAPIDistributedZkTest extends SolrCloudTestCase {
       Set<ObjectName> mbeans = new HashSet<>();
       mbeans.addAll(server.queryNames(null, null));
       for (final ObjectName mbean : mbeans) {
        Object value;
        Object indexDir;
        Object name;
 
         try {
          if (((value = server.getAttribute(mbean, "category")) != null && value
              .toString().equals(Category.CORE.toString()))
              && ((indexDir = server.getAttribute(mbean, "coreName")) != null)
              && ((indexDir = server.getAttribute(mbean, "indexDir")) != null)
              && ((name = server.getAttribute(mbean, "name")) != null)) {
            if (!indexDirToShardNamesMap.containsKey(indexDir.toString())) {
              indexDirToShardNamesMap.put(indexDir.toString(),
                  new HashSet<String>());
          Map<String, String> props = mbean.getKeyPropertyList();
          String category = props.get("category");
          String name = props.get("name");
          if ((category != null && category.toString().equals(Category.CORE.toString())) &&
              (name != null && name.equals("indexDir"))) {
            String indexDir = server.getAttribute(mbean, "Value").toString();
            String key = props.get("dom2") + "." + props.get("dom3") + "." + props.get("dom4");
            if (!indexDirToShardNamesMap.containsKey(indexDir)) {
              indexDirToShardNamesMap.put(indexDir.toString(), new HashSet<>());
             }
            indexDirToShardNamesMap.get(indexDir.toString()).add(
                name.toString());
            indexDirToShardNamesMap.get(indexDir.toString()).add(key);
           }
         } catch (Exception e) {
          // ignore, just continue - probably a "category" or "source" attribute
          // ignore, just continue - probably a "Value" attribute
           // not found
         }
       }
diff --git a/solr/core/src/test/org/apache/solr/cloud/ReplicationFactorTest.java b/solr/core/src/test/org/apache/solr/cloud/ReplicationFactorTest.java
index 9441e3ff1ef..9100eee67f4 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ReplicationFactorTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ReplicationFactorTest.java
@@ -18,7 +18,6 @@ package org.apache.solr.cloud;
 
 import java.io.File;
 import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
@@ -71,14 +70,6 @@ public class ReplicationFactorTest extends AbstractFullDistribZkTestBase {
     return createProxiedJetty(solrHome, dataDir, shardList, solrConfigOverride, schemaOverride);
   }
   
  protected int getNextAvailablePort() throws Exception {    
    int port = -1;
    try (ServerSocket s = new ServerSocket(0)) {
      port = s.getLocalPort();
    }
    return port;
  }

   @Test
   public void test() throws Exception {
     log.info("replication factor test running");
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
index 1c1c5c1ca33..4756cd9e810 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestRandomRequestDistribution.java
@@ -23,6 +23,7 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
import com.codahale.metrics.Counter;
 import org.apache.lucene.util.TestUtil;
 import org.apache.solr.BaseDistributedSearchTestCase;
 import org.apache.solr.SolrTestCaseJ4;
@@ -39,6 +40,7 @@ import org.apache.solr.common.cloud.ZkStateReader;
 import org.apache.solr.common.util.Utils;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.request.SolrRequestHandler;
 import org.junit.Test;
 import org.slf4j.Logger;
@@ -109,10 +111,13 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
     Map<String, Integer> shardVsCount = new HashMap<>();
     for (JettySolrRunner runner : jettys) {
       CoreContainer container = runner.getCoreContainer();
      SolrMetricManager metricManager = container.getMetricManager();
       for (SolrCore core : container.getCores()) {
        String registry = core.getCoreMetricManager().getRegistryName();
        Counter cnt = metricManager.counter(null, registry, "requests", "QUERY.standard");
         SolrRequestHandler select = core.getRequestHandler("");
        long c = (long) select.getStatistics().get("requests");
        shardVsCount.put(core.getName(), (int) c);
//        long c = (long) select.getStatistics().get("requests");
        shardVsCount.put(core.getName(), (int) cnt.getCount());
       }
     }
 
@@ -190,6 +195,10 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
       }
       assertNotNull(leaderCore);
 
      SolrMetricManager leaderMetricManager = leaderCore.getCoreDescriptor().getCoreContainer().getMetricManager();
      String leaderRegistry = leaderCore.getCoreMetricManager().getRegistryName();
      Counter cnt = leaderMetricManager.counter(null, leaderRegistry, "requests", "QUERY.standard");

       // All queries should be served by the active replica
       // To make sure that's true we keep querying the down replica
       // If queries are getting processed by the down replica then the cluster state hasn't updated for that replica
@@ -200,8 +209,7 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
         count++;
         client.query(new SolrQuery("*:*"));
 
        SolrRequestHandler select = leaderCore.getRequestHandler("");
        long c = (long) select.getStatistics().get("requests");
        long c = cnt.getCount();
 
         if (c == 1) {
           break; // cluster state has got update locally
@@ -222,8 +230,7 @@ public class TestRandomRequestDistribution extends AbstractFullDistribZkTestBase
         client.query(new SolrQuery("*:*"));
         count++;
 
        SolrRequestHandler select = leaderCore.getRequestHandler("");
        long c = (long) select.getStatistics().get("requests");
        long c = cnt.getCount();
 
         assertEquals("Query wasn't served by leader", count, c);
       }
diff --git a/solr/core/src/test/org/apache/solr/core/ExitableDirectoryReaderTest.java b/solr/core/src/test/org/apache/solr/core/ExitableDirectoryReaderTest.java
index 5f0d537a7e2..aa42664b400 100644
-- a/solr/core/src/test/org/apache/solr/core/ExitableDirectoryReaderTest.java
++ b/solr/core/src/test/org/apache/solr/core/ExitableDirectoryReaderTest.java
@@ -19,7 +19,7 @@ package org.apache.solr.core;
 import java.util.Map;
 
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.response.SolrQueryResponse;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -88,11 +88,11 @@ public class ExitableDirectoryReaderTest extends SolrTestCaseJ4 {
   public void testCacheAssumptions() throws Exception {
     String fq= "name:d*";
     SolrCore core = h.getCore();
    SolrInfoMBean filterCacheStats = core.getInfoRegistry().get("filterCache");
    long fqInserts = (long) filterCacheStats.getStatistics().get("inserts");
    MetricsMap filterCacheStats = (MetricsMap)core.getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.filterCache");
    long fqInserts = (long) filterCacheStats.getValue().get("inserts");
 
    SolrInfoMBean queryCacheStats = core.getInfoRegistry().get("queryResultCache");
    long qrInserts = (long) queryCacheStats.getStatistics().get("inserts");
    MetricsMap queryCacheStats = (MetricsMap)core.getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.queryResultCache");
    long qrInserts = (long) queryCacheStats.getValue().get("inserts");
 
     // This gets 0 docs back. Use 10000 instead of 1 for timeAllowed and it gets 100 back and the for loop below
     // succeeds.
@@ -105,16 +105,16 @@ public class ExitableDirectoryReaderTest extends SolrTestCaseJ4 {
     assertTrue("Should have partial results", (Boolean) (header.get(SolrQueryResponse.RESPONSE_HEADER_PARTIAL_RESULTS_KEY)));
 
     assertEquals("Should NOT have inserted partial results in the cache!",
        (long) queryCacheStats.getStatistics().get("inserts"), qrInserts);
        (long) queryCacheStats.getValue().get("inserts"), qrInserts);
 
    assertEquals("Should NOT have another insert", fqInserts, (long) filterCacheStats.getStatistics().get("inserts"));
    assertEquals("Should NOT have another insert", fqInserts, (long) filterCacheStats.getValue().get("inserts"));
 
     // At the end of all this, we should have no hits in the queryResultCache.
     response = JQ(req("q", "*:*", "fq", fq, "indent", "true", "timeAllowed", longTimeout));
 
     // Check that we did insert this one.
    assertEquals("Hits should still be 0", (long) filterCacheStats.getStatistics().get("hits"), 0L);
    assertEquals("Inserts should be bumped", (long) filterCacheStats.getStatistics().get("inserts"), fqInserts + 1);
    assertEquals("Hits should still be 0", (long) filterCacheStats.getValue().get("hits"), 0L);
    assertEquals("Inserts should be bumped", (long) filterCacheStats.getValue().get("inserts"), fqInserts + 1);
 
     res = (Map) ObjectBuilder.fromJSON(response);
     body = (Map) (res.get("response"));
@@ -130,14 +130,14 @@ public class ExitableDirectoryReaderTest extends SolrTestCaseJ4 {
   public void testQueryResults() throws Exception {
     String q = "name:e*";
     SolrCore core = h.getCore();
    SolrInfoMBean queryCacheStats = core.getInfoRegistry().get("queryResultCache");
    NamedList nl = queryCacheStats.getStatistics();
    MetricsMap queryCacheStats = (MetricsMap)core.getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.queryResultCache");
    Map<String,Object> nl = queryCacheStats.getValue();
     long inserts = (long) nl.get("inserts");
 
     String response = JQ(req("q", q, "indent", "true", "timeAllowed", "1", "sleep", sleep));
 
     // The queryResultCache should NOT get an entry here.
    nl = queryCacheStats.getStatistics();
    nl = queryCacheStats.getValue();
     assertEquals("Should NOT have inserted partial results!", inserts, (long) nl.get("inserts"));
 
     Map res = (Map) ObjectBuilder.fromJSON(response);
@@ -150,7 +150,7 @@ public class ExitableDirectoryReaderTest extends SolrTestCaseJ4 {
     response = JQ(req("q", q, "indent", "true", "timeAllowed", longTimeout));
 
     // Check that we did insert this one.
    NamedList nl2 = queryCacheStats.getStatistics();
    Map<String,Object> nl2 = queryCacheStats.getValue();
     assertEquals("Hits should still be 0", (long) nl.get("hits"), (long) nl2.get("hits"));
     assertTrue("Inserts should be bumped", inserts < (long) nl2.get("inserts"));
 
diff --git a/solr/core/src/test/org/apache/solr/core/HdfsDirectoryFactoryTest.java b/solr/core/src/test/org/apache/solr/core/HdfsDirectoryFactoryTest.java
index 75f6c9b6ba7..2a4dcc0513b 100644
-- a/solr/core/src/test/org/apache/solr/core/HdfsDirectoryFactoryTest.java
++ b/solr/core/src/test/org/apache/solr/core/HdfsDirectoryFactoryTest.java
@@ -20,9 +20,9 @@ import java.nio.file.Path;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.HashMap;
import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
import java.util.Random;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.FileSystem;
@@ -30,11 +30,14 @@ import org.apache.hadoop.hdfs.MiniDFSCluster;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.IndexOutput;
 import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.cloud.hdfs.HdfsTestUtil;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.DirectoryFactory.DirContext;
 import org.apache.solr.handler.SnapShooter;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.store.hdfs.HdfsLocalityReporter;
 import org.apache.solr.util.BadHdfsThreadsFilter;
 import org.apache.solr.util.MockCoreContainer.MockCoreDescriptor;
@@ -175,24 +178,24 @@ public class HdfsDirectoryFactoryTest extends SolrTestCaseJ4 {
   public void testLocalityReporter() throws Exception {
     Configuration conf = HdfsTestUtil.getClientConfiguration(dfsCluster);
     conf.set("dfs.permissions.enabled", "false");
    

    Random r = random();
     HdfsDirectoryFactory factory = new HdfsDirectoryFactory();
    SolrMetricManager metricManager = new SolrMetricManager();
    String registry = TestUtil.randomSimpleString(r, 2, 10);
    String scope = TestUtil.randomSimpleString(r,2, 10);
     Map<String,String> props = new HashMap<String,String>();
     props.put(HdfsDirectoryFactory.HDFS_HOME, HdfsTestUtil.getURI(dfsCluster) + "/solr");
     props.put(HdfsDirectoryFactory.BLOCKCACHE_ENABLED, "false");
     props.put(HdfsDirectoryFactory.NRTCACHINGDIRECTORY_ENABLE, "false");
     props.put(HdfsDirectoryFactory.LOCALITYMETRICS_ENABLED, "true");
     factory.init(new NamedList<>(props));
    
    Iterator<SolrInfoMBean> it = factory.offerMBeans().iterator();
    it.next(); // skip
    SolrInfoMBean localityBean = it.next(); // brittle, but it's ok
    
    // Make sure we have the right bean.
    assertEquals("Got the wrong bean: " + localityBean.getName(), "hdfs-locality", localityBean.getName());
    
    factory.initializeMetrics(metricManager, registry, scope);

    // get the metrics map for the locality bean
    MetricsMap metrics = (MetricsMap)metricManager.registry(registry).getMetrics().get("OTHER." + scope + ".hdfsLocality");
     // We haven't done anything, so there should be no data
    NamedList<?> statistics = localityBean.getStatistics();
    Map<String,Object> statistics = metrics.getValue();
     assertEquals("Saw bytes that were not written: " + statistics.get(HdfsLocalityReporter.LOCALITY_BYTES_TOTAL), 0l,
         statistics.get(HdfsLocalityReporter.LOCALITY_BYTES_TOTAL));
     assertEquals(
@@ -210,7 +213,7 @@ public class HdfsDirectoryFactoryTest extends SolrTestCaseJ4 {
     
     // no locality because hostname not set
     factory.setHost("bogus");
    statistics = localityBean.getStatistics();
    statistics = metrics.getValue();
     assertEquals("Wrong number of total bytes counted: " + statistics.get(HdfsLocalityReporter.LOCALITY_BYTES_TOTAL),
         long_bytes, statistics.get(HdfsLocalityReporter.LOCALITY_BYTES_TOTAL));
     assertEquals("Wrong number of total blocks counted: " + statistics.get(HdfsLocalityReporter.LOCALITY_BLOCKS_TOTAL),
@@ -221,7 +224,7 @@ public class HdfsDirectoryFactoryTest extends SolrTestCaseJ4 {
         
     // set hostname and check again
     factory.setHost("127.0.0.1");
    statistics = localityBean.getStatistics();
    statistics = metrics.getValue();
     assertEquals(
         "Did not count block as local after setting hostname: "
             + statistics.get(HdfsLocalityReporter.LOCALITY_BYTES_LOCAL),
diff --git a/solr/core/src/test/org/apache/solr/core/MockInfoBean.java b/solr/core/src/test/org/apache/solr/core/MockInfoBean.java
new file mode 100644
index 00000000000..dfa94ae1121
-- /dev/null
++ b/solr/core/src/test/org/apache/solr/core/MockInfoBean.java
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
package org.apache.solr.core;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricProducer;

class MockInfoBean implements SolrInfoBean, SolrMetricProducer {
  Set<String> metricNames = new HashSet<>();
  MetricRegistry registry;

  @Override
  public String getName() {
    return "mock";
  }

  @Override
  public Category getCategory() {
    return Category.OTHER;
  }

  @Override
  public String getDescription() {
    return "mock";
  }

  @Override
  public Set<String> getMetricNames() {
    return metricNames;
  }

  @Override
  public MetricRegistry getMetricRegistry() {
    return registry;
  }

  @Override
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    registry = manager.registry(registryName);
    MetricsMap metricsMap = new MetricsMap((detailed, map) -> {
      map.put("Integer", 123);
      map.put("Double",567.534);
      map.put("Long", 32352463l);
      map.put("Short", (short) 32768);
      map.put("Byte", (byte) 254);
      map.put("Float", 3.456f);
      map.put("String","testing");
      map.put("Object", new Object());
    });
    manager.registerGauge(this, registryName, metricsMap, true, getClass().getSimpleName(), getCategory().toString(), scope);
  }
}
\ No newline at end of file
diff --git a/solr/core/src/test/org/apache/solr/core/MockInfoMBean.java b/solr/core/src/test/org/apache/solr/core/MockInfoMBean.java
deleted file mode 100644
index e0d566c47a0..00000000000
-- a/solr/core/src/test/org/apache/solr/core/MockInfoMBean.java
++ /dev/null
@@ -1,69 +0,0 @@
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
package org.apache.solr.core;

import java.net.URL;

import org.apache.solr.common.util.NamedList;

class MockInfoMBean implements SolrInfoMBean {
  @Override
  public String getName() {
    return "mock";
  }

  @Override
  public Category getCategory() {
    return Category.OTHER;
  }

  @Override
  public String getDescription() {
    return "mock";
  }

  @Override
  public URL[] getDocs() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getVersion() {
    return "mock";
  }

  @Override
  public String getSource() {
    return "mock";
  }

  @Override
  @SuppressWarnings("unchecked")
  public NamedList getStatistics() {
    NamedList myList = new NamedList<Integer>();
    myList.add("Integer", 123);
    myList.add("Double",567.534);
    myList.add("Long", 32352463l);
    myList.add("Short", (short) 32768);
    myList.add("Byte", (byte) 254);
    myList.add("Float", 3.456f);
    myList.add("String","testing");
    myList.add("Object", new Object());
    return myList;
  }
}
\ No newline at end of file
diff --git a/solr/core/src/test/org/apache/solr/core/MockQuerySenderListenerReqHandler.java b/solr/core/src/test/org/apache/solr/core/MockQuerySenderListenerReqHandler.java
index 367870a697f..bcf6e9f91db 100644
-- a/solr/core/src/test/org/apache/solr/core/MockQuerySenderListenerReqHandler.java
++ b/solr/core/src/test/org/apache/solr/core/MockQuerySenderListenerReqHandler.java
@@ -17,6 +17,7 @@
 package org.apache.solr.core;
 
 import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.response.SolrQueryResponse;
 import org.apache.solr.common.util.NamedList;
@@ -40,6 +41,12 @@ public class MockQuerySenderListenerReqHandler extends RequestHandlerBase {
     super.init(args);
   }
 
  @Override
  public void initializeMetrics(SolrMetricManager manager, String registryName, String scope) {
    super.initializeMetrics(manager, registryName, scope);
    manager.registerGauge(this, registryName, () -> initCounter.intValue(), true, "initCount", getCategory().toString(), scope);
  }

   @Override
   public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
     this.req = req;
@@ -51,12 +58,4 @@ public class MockQuerySenderListenerReqHandler extends RequestHandlerBase {
     String result = null;
     return result;
   }

  @Override
  public NamedList<Object> getStatistics() {
    NamedList<Object> lst = super.getStatistics();
    lst.add("initCount", initCounter.intValue());
    return lst;
  }
 
 }
diff --git a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
index 9a953e1ccca..3c13645702c 100644
-- a/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
++ b/solr/core/src/test/org/apache/solr/core/RequestHandlersTest.java
@@ -16,9 +16,13 @@
  */
 package org.apache.solr.core;
 
import java.util.Map;

import com.codahale.metrics.Gauge;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.util.stats.MetricUtils;
 import org.junit.BeforeClass;
 import org.junit.Test;
 
@@ -30,10 +34,11 @@ public class RequestHandlersTest extends SolrTestCaseJ4 {
 
   @Test
   public void testInitCount() {
    SolrCore core = h.getCore();
    SolrRequestHandler handler = core.getRequestHandler( "mock" );
    String registry = h.getCore().getCoreMetricManager().getRegistryName();
    SolrMetricManager manager = h.getCoreContainer().getMetricManager();
    Gauge<Number> g = (Gauge<Number>)manager.registry(registry).getMetrics().get("QUERY.mock.initCount");
     assertEquals("Incorrect init count",
                 1, handler.getStatistics().get("initCount"));
                 1, g.getValue().intValue());
   }
 
   @Test
@@ -105,11 +110,11 @@ public class RequestHandlersTest extends SolrTestCaseJ4 {
         "text", "line up and fly directly at the enemy death cannons, clogging them with wreckage!"));
     assertU(commit());
 
    NamedList updateStats = updateHandler.getStatistics();
    NamedList termStats = termHandler.getStatistics();
    Map<String,Object> updateStats = MetricUtils.convertMetrics(updateHandler.getMetricRegistry(), updateHandler.getMetricNames());
    Map<String,Object> termStats = MetricUtils.convertMetrics(termHandler.getMetricRegistry(), termHandler.getMetricNames());
 
    Double updateTime = (Double) updateStats.get("avgTimePerRequest");
    Double termTime = (Double) termStats.get("avgTimePerRequest");
    Long updateTime = (Long) updateStats.get("UPDATE./update.totalTime");
    Long termTime = (Long) termStats.get("QUERY./terms.totalTime");
 
     assertFalse("RequestHandlers should not share statistics!", updateTime.equals(termTime));
   }
diff --git a/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java b/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
index 695e8693ec1..c042bd66c4e 100644
-- a/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
++ b/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
@@ -245,10 +245,10 @@ public class SolrCoreTest extends SolrTestCaseJ4 {
     //TEst that SolrInfoMBeans are registered, including SearchComponents
     SolrCore core = h.getCore();
 
    Map<String, SolrInfoMBean> infoRegistry = core.getInfoRegistry();
    Map<String, SolrInfoBean> infoRegistry = core.getInfoRegistry();
     assertTrue("infoRegistry Size: " + infoRegistry.size() + " is not greater than: " + 0, infoRegistry.size() > 0);
     //try out some that we know are in the config
    SolrInfoMBean bean = infoRegistry.get(SpellCheckComponent.COMPONENT_NAME);
    SolrInfoBean bean = infoRegistry.get(SpellCheckComponent.COMPONENT_NAME);
     assertNotNull("bean not registered", bean);
     //try a default one
     bean = infoRegistry.get(QueryComponent.COMPONENT_NAME);
diff --git a/solr/core/src/test/org/apache/solr/core/TestJmxIntegration.java b/solr/core/src/test/org/apache/solr/core/TestJmxIntegration.java
index f841f92781f..db941f7efb3 100644
-- a/solr/core/src/test/org/apache/solr/core/TestJmxIntegration.java
++ b/solr/core/src/test/org/apache/solr/core/TestJmxIntegration.java
@@ -16,7 +16,10 @@
  */
 package org.apache.solr.core;
 
import org.apache.solr.core.JmxMonitoredMap.SolrDynamicMBean;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.metrics.SolrMetricReporter;
import org.apache.solr.metrics.reporters.JmxObjectNameFactory;
import org.apache.solr.metrics.reporters.SolrJmxReporter;
 import org.apache.solr.util.AbstractSolrTestCase;
 import org.junit.AfterClass;
 import org.junit.BeforeClass;
@@ -29,12 +32,10 @@ import javax.management.AttributeNotFoundException;
 import javax.management.MBeanAttributeInfo;
 import javax.management.MBeanInfo;
 import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
 import javax.management.ObjectInstance;
 import javax.management.ObjectName;
 import java.lang.invoke.MethodHandles;
 import java.lang.management.ManagementFactory;
import java.util.Hashtable;
 import java.util.Map;
 import java.util.Set;
 
@@ -49,6 +50,8 @@ public class TestJmxIntegration extends AbstractSolrTestCase {
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
   private static MBeanServer mbeanServer = null;
  private static JmxObjectNameFactory nameFactory = null;
  private static String registryName = null;
 
   @BeforeClass
   public static void beforeClass() throws Exception {
@@ -61,25 +64,30 @@ public class TestJmxIntegration extends AbstractSolrTestCase {
 
     initCore("solrconfig.xml", "schema.xml");
 
    // we should be able to se that the core has JmxIntegration enabled
    assertTrue("JMX not enabled",
               h.getCore().getSolrConfig().jmxConfig.enabled);
    // and we should be able to see that the the monitor map found 
    // a JMX server to use, which refers to the server we started

    Map registry = h.getCore().getInfoRegistry();
    assertTrue("info registry is not a JMX monitored map",
               registry instanceof JmxMonitoredMap);
    mbeanServer = ((JmxMonitoredMap)registry).getServer();

    assertNotNull("No JMX server found by monitor map",
                  mbeanServer);

    // NOTE: we can't garuntee that "mbeanServer == platformServer"
    // the JVM may have mutiple MBean servers funning when the test started
    // and the contract of not specifying one when configuring solr with
    // <jmx /> is that it will use whatever the "first" MBean server 
    // we should be able to see that the core has JmxIntegration enabled
    registryName = h.getCore().getCoreMetricManager().getRegistryName();
    SolrMetricManager manager = h.getCoreContainer().getMetricManager();
    Map<String,SolrMetricReporter> reporters = manager.getReporters(registryName);
    assertEquals(1, reporters.size());
    SolrMetricReporter reporter = reporters.values().iterator().next();
    assertTrue(reporter instanceof SolrJmxReporter);
    SolrJmxReporter jmx = (SolrJmxReporter)reporter;
    assertTrue("JMX not enabled", jmx.isActive());
    // and we should be able to see that the reporter
    // refers to the JMX server we started

    mbeanServer = jmx.getMBeanServer();

    assertNotNull("No JMX server found in the reporter",
        mbeanServer);

    // NOTE: we can't guarantee that "mbeanServer == platformServer"
    // the JVM may have multiple MBean servers running when the test started
    // and the contract of not specifying one when configuring solr.xml without
    // agetnId or serviceUrl is that it will use whatever the "first" MBean server
     // returned by the JVM is.

    nameFactory = new JmxObjectNameFactory("default", registryName);
   }
 
   @AfterClass
@@ -93,34 +101,38 @@ public class TestJmxIntegration extends AbstractSolrTestCase {
 
     Set<ObjectInstance> objects = mbeanServer.queryMBeans(null, null);
     assertFalse("No objects found in mbean server", objects
            .isEmpty());
        .isEmpty());
     int numDynamicMbeans = 0;
     for (ObjectInstance o : objects) {
      assertNotNull("Null name on: " + o.toString(), o.getObjectName());
      MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(o.getObjectName());
      if (mbeanInfo.getClassName().endsWith(SolrDynamicMBean.class.getName())) {
      ObjectName name = o.getObjectName();
      assertNotNull("Null name on: " + o.toString(), name);
      MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(name);
      if (name.getDomain().equals("solr")) {
         numDynamicMbeans++;
         MBeanAttributeInfo[] attrs = mbeanInfo.getAttributes();
        assertTrue("No Attributes found for mbean: " + mbeanInfo, 
                   0 < attrs.length);
        if (name.getKeyProperty("name").equals("fetcher")) { // no attributes without active replication
          continue;
        }
        assertTrue("No Attributes found for mbean: " + o.getObjectName() + ", " + mbeanInfo,
            0 < attrs.length);
         for (MBeanAttributeInfo attr : attrs) {
           // ensure every advertised attribute is gettable
           try {
             Object trash = mbeanServer.getAttribute(o.getObjectName(), attr.getName());
           } catch (javax.management.AttributeNotFoundException e) {
             throw new RuntimeException("Unable to featch attribute for " + o.getObjectName()
                                       + ": " + attr.getName(), e);
                + ": " + attr.getName(), e);
           }
         }
       }
     }
    assertTrue("No SolrDynamicMBeans found", 0 < numDynamicMbeans);
    assertTrue("No MBeans found", 0 < numDynamicMbeans);
   }
 
   @Test
   public void testJmxUpdate() throws Exception {
 
    SolrInfoMBean bean = null;
    SolrInfoBean bean = null;
     // wait until searcher is registered
     for (int i=0; i<100; i++) {
       bean = h.getCore().getInfoRegistry().get("searcher");
@@ -128,18 +140,20 @@ public class TestJmxIntegration extends AbstractSolrTestCase {
       Thread.sleep(250);
     }
     if (bean==null) throw new RuntimeException("searcher was never registered");
    ObjectName searcher = getObjectName("searcher", bean);
    ObjectName searcher = nameFactory.createName("gauge", registryName, "SEARCHER.searcher.*");
 
     log.info("Mbeans in server: " + mbeanServer.queryNames(null, null));
 
    Set<ObjectInstance> objects = mbeanServer.queryMBeans(searcher, null);
     assertFalse("No mbean found for SolrIndexSearcher", mbeanServer.queryMBeans(searcher, null).isEmpty());
 
    int oldNumDocs =  (Integer)mbeanServer.getAttribute(searcher, "numDocs");
    ObjectName name = nameFactory.createName("gauge", registryName, "SEARCHER.searcher.numDocs");
    int oldNumDocs =  (Integer)mbeanServer.getAttribute(name, "Value");
     assertU(adoc("id", "1"));
     assertU("commit", commit());
    int numDocs = (Integer)mbeanServer.getAttribute(searcher, "numDocs");
    int numDocs = (Integer)mbeanServer.getAttribute(name, "Value");
     assertTrue("New numDocs is same as old numDocs as reported by JMX",
            numDocs > oldNumDocs);
        numDocs > oldNumDocs);
   }
 
   @Test @Ignore("timing problem? https://issues.apache.org/jira/browse/SOLR-2715")
@@ -183,14 +197,4 @@ public class TestJmxIntegration extends AbstractSolrTestCase {
     log.info("After Reload: Size of infoRegistry: " + registrySize + " MBeans: " + newNumberOfObjects);
     assertEquals("Number of registered MBeans is not the same as info registry size", registrySize, newNumberOfObjects);
   }

  private ObjectName getObjectName(String key, SolrInfoMBean infoBean)
          throws MalformedObjectNameException {
    Hashtable<String, String> map = new Hashtable<>();
    map.put("type", key);
    map.put("id", infoBean.getName());
    String coreName = h.getCore().getName();
    return ObjectName.getInstance(("solr" + (null != coreName ? "/" + coreName : "")), map);
  }
}

}
\ No newline at end of file
diff --git a/solr/core/src/test/org/apache/solr/core/TestJmxMonitoredMap.java b/solr/core/src/test/org/apache/solr/core/TestJmxMonitoredMap.java
deleted file mode 100644
index aa107bce0bf..00000000000
-- a/solr/core/src/test/org/apache/solr/core/TestJmxMonitoredMap.java
++ /dev/null
@@ -1,217 +0,0 @@
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
package org.apache.solr.core;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrConfig.JmxConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Set;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * Test for JmxMonitoredMap
 *
 *
 * @since solr 1.3
 */
public class TestJmxMonitoredMap extends LuceneTestCase {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private int port = 0;

  private JMXConnector connector;

  private MBeanServerConnection mbeanServer;

  private JmxMonitoredMap<String, SolrInfoMBean> monitoredMap;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    String oldHost = System.getProperty("java.rmi.server.hostname");
    try {
      // this stupid sysprop thing is needed, because remote stubs use an
      // arbitrary local ip to connect
      // See: http://weblogs.java.net/blog/emcmanus/archive/2006/12/multihomed_comp.html
      System.setProperty("java.rmi.server.hostname", "127.0.0.1");
      class LocalhostRMIServerSocketFactory implements RMIServerSocketFactory {
        ServerSocket socket;
        
        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
          return socket = new ServerSocket(port);
        }
      };
      LocalhostRMIServerSocketFactory factory = new LocalhostRMIServerSocketFactory();
      LocateRegistry.createRegistry(0, null, factory);
      port = factory.socket.getLocalPort();
      log.info("Using port: " + port);
      String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:"+port+"/solrjmx";
      JmxConfiguration config = new JmxConfiguration(true, null, url, null);
      monitoredMap = new JmxMonitoredMap<>("", "", "", config);
      JMXServiceURL u = new JMXServiceURL(url);
      connector = JMXConnectorFactory.connect(u);
      mbeanServer = connector.getMBeanServerConnection();
    } finally {
      if (oldHost == null) {
        System.clearProperty("java.rmi.server.hostname");
      } else {
        System.setProperty("java.rmi.server.hostname", oldHost);
      }
    }
  }

  @Override
  @After
  public void tearDown() throws Exception {
    try {
      connector.close();
    } catch (Exception e) {
    }
    super.tearDown();
  }

  @Test
  public void testTypeName() throws Exception{
    MockInfoMBean mock = new MockInfoMBean();
    monitoredMap.put("mock", mock);

    NamedList dynamicStats = mock.getStatistics();
    assertTrue(dynamicStats.size() != 0);
    assertTrue(dynamicStats.get("Integer") instanceof Integer);
    assertTrue(dynamicStats.get("Double") instanceof Double);
    assertTrue(dynamicStats.get("Long") instanceof Long);
    assertTrue(dynamicStats.get("Short") instanceof Short);
    assertTrue(dynamicStats.get("Byte") instanceof Byte);
    assertTrue(dynamicStats.get("Float") instanceof Float);
    assertTrue(dynamicStats.get("String") instanceof String);

    Set<ObjectInstance> objects = mbeanServer.queryMBeans(null, Query.match(
        Query.attr("name"), Query.value("mock")));

    ObjectName name = objects.iterator().next().getObjectName();
    assertMBeanTypeAndValue(name, "Integer", Integer.class, 123);
    assertMBeanTypeAndValue(name, "Double", Double.class, 567.534);
    assertMBeanTypeAndValue(name, "Long", Long.class, 32352463l);
    assertMBeanTypeAndValue(name, "Short", Short.class, (short) 32768);
    assertMBeanTypeAndValue(name, "Byte", Byte.class, (byte) 254);
    assertMBeanTypeAndValue(name, "Float", Float.class, 3.456f);
    assertMBeanTypeAndValue(name, "String",String.class, "testing");

  }

  @SuppressWarnings("unchecked")
  public void assertMBeanTypeAndValue(ObjectName name, String attr, Class type, Object value) throws Exception {
    assertThat(mbeanServer.getAttribute(name, attr), 
        allOf(instanceOf(type), equalTo(value))
    );
  }

  @Test
  public void testPutRemoveClear() throws Exception {
    MockInfoMBean mock = new MockInfoMBean();
    monitoredMap.put("mock", mock);


    Set<ObjectInstance> objects = mbeanServer.queryMBeans(null, Query.match(
        Query.attr("name"), Query.value("mock")));
    assertFalse("No MBean for mock object found in MBeanServer", objects
        .isEmpty());

    monitoredMap.remove("mock");
    objects = mbeanServer.queryMBeans(null, Query.match(Query.attr("name"),
        Query.value("mock")));
    assertTrue("MBean for mock object found in MBeanServer even after removal",
        objects.isEmpty());

    monitoredMap.put("mock", mock);
    monitoredMap.put("mock2", mock);
    objects = mbeanServer.queryMBeans(null, Query.match(Query.attr("name"),
        Query.value("mock")));
    assertFalse("No MBean for mock object found in MBeanServer", objects
        .isEmpty());

    monitoredMap.clear();
    objects = mbeanServer.queryMBeans(null, Query.match(Query.attr("name"),
        Query.value("mock")));
    assertTrue(
        "MBean for mock object found in MBeanServer even after clear has been called",
        objects.isEmpty());

  }

  @Test
  public void testJmxAugmentedSolrInfoMBean() throws Exception {
    final MockInfoMBean mock = new MockInfoMBean();
    final String jmxKey = "jmx";
    final String jmxValue = "jmxValue";

    MockJmxAugmentedSolrInfoMBean mbean = new MockJmxAugmentedSolrInfoMBean(mock) {
      @Override
      public NamedList getStatisticsForJmx() {
        NamedList stats = getStatistics();
        stats.add(jmxKey, jmxValue);
        return stats;
      }
    };
    monitoredMap.put("mock", mbean);

    // assert getStatistics called when used as a map.  Note can't use equals here to compare
    // because getStatistics returns a new Object each time.
    assertNull(monitoredMap.get("mock").getStatistics().get(jmxKey));

    //  assert getStatisticsForJmx called when used as jmx server
    Set<ObjectInstance> objects = mbeanServer.queryMBeans(null, Query.match(
        Query.attr("name"), Query.value("mock")));
    ObjectName name = objects.iterator().next().getObjectName();
    assertMBeanTypeAndValue(name, jmxKey, jmxValue.getClass(), jmxValue);
  }

  private static abstract class MockJmxAugmentedSolrInfoMBean
      extends SolrInfoMBeanWrapper implements JmxMonitoredMap.JmxAugmentedSolrInfoMBean {

    public MockJmxAugmentedSolrInfoMBean(SolrInfoMBean mbean) {
      super(mbean);
    }

    @Override
    public abstract NamedList getStatisticsForJmx();
  }
}
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrDynamicMBean.java b/solr/core/src/test/org/apache/solr/core/TestSolrDynamicMBean.java
deleted file mode 100644
index eae4e799e89..00000000000
-- a/solr/core/src/test/org/apache/solr/core/TestSolrDynamicMBean.java
++ /dev/null
@@ -1,87 +0,0 @@
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
package org.apache.solr.core;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.core.JmxMonitoredMap.SolrDynamicMBean;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for JmxMonitoredMap
 *
 *
 * @since solr 1.3
 */
public class TestSolrDynamicMBean extends LuceneTestCase {


  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }


  @Test
  public void testCachedStatsOption() throws Exception{
    //  SOLR-6747 Add an optional caching option as a workaround for SOLR-6586.
    
    SolrInfoMBean solrmbeaninfo = new MockInfoMBean();
    SolrDynamicMBean sdmbean = new SolrDynamicMBean("", solrmbeaninfo);
    
    sdmbean.getMBeanInfo();
    
    Object object1 = sdmbean.getAttribute("Object");
    Object object2 = sdmbean.getAttribute("Object");
    
    assertNotSame(object1, object2);
    
    sdmbean.getMBeanInfo();
    
    Object object12 = sdmbean.getAttribute("Object");
    Object object22 = sdmbean.getAttribute("Object");
    
    assertNotSame(object1, object12);
    assertNotSame(object2, object22);
    
    
    // test cached stats
    
    solrmbeaninfo = new MockInfoMBean();
    sdmbean = new SolrDynamicMBean("", solrmbeaninfo, true);
    
    sdmbean.getMBeanInfo();
    
    object1 = sdmbean.getAttribute("Object");
    object2 = sdmbean.getAttribute("Object");
    
    assertEquals(object1, object2);
    
    sdmbean.getMBeanInfo();
    
    object12 = sdmbean.getAttribute("Object");
    object22 = sdmbean.getAttribute("Object");
    
    assertNotSame(object1, object12);
    assertNotSame(object2, object22);
    
    assertEquals(object12, object22);
    
  }

}
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/MBeansHandlerTest.java b/solr/core/src/test/org/apache/solr/handler/admin/MBeansHandlerTest.java
index 84e2382d00c..c7622f640cc 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/MBeansHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/MBeansHandlerTest.java
@@ -31,7 +31,7 @@ import org.junit.BeforeClass;
 import org.junit.Test;
 
 public class MBeansHandlerTest extends SolrTestCaseJ4 {
  

   @BeforeClass
   public static void beforeClass() throws Exception {
     initCore("solrconfig.xml", "schema.xml");
@@ -43,26 +43,26 @@ public class MBeansHandlerTest extends SolrTestCaseJ4 {
         CommonParams.QT,"/admin/mbeans",
         "stats","true",
         CommonParams.WT,"xml"
     ));
    ));
     List<ContentStream> streams = new ArrayList<>();
     streams.add(new ContentStreamBase.StringStream(xml));
    

     LocalSolrQueryRequest req = lrf.makeRequest(
         CommonParams.QT,"/admin/mbeans",
         "stats","true",
         CommonParams.WT,"xml",
         "diff","true");
     req.setContentStreams(streams);
    

     xml = h.query(req);
     NamedList<NamedList<NamedList<Object>>> diff = SolrInfoMBeanHandler.fromXML(xml);
 
     // The stats bean for SolrInfoMBeanHandler
     NamedList stats = (NamedList)diff.get("ADMIN").get("/admin/mbeans").get("stats");
    

     //System.out.println("stats:"+stats);
     Pattern p = Pattern.compile("Was: (?<was>[0-9]+), Now: (?<now>[0-9]+), Delta: (?<delta>[0-9]+)");
    String response = stats.get("requests").toString();
    String response = stats.get("ADMIN./admin/mbeans.requests").toString();
     Matcher m = p.matcher(response);
     if (!m.matches()) {
       fail("Response did not match pattern: " + response);
@@ -96,4 +96,4 @@ public class MBeansHandlerTest extends SolrTestCaseJ4 {
 
     assertTrue("external entity ignored properly", true);
   }
}
}
\ No newline at end of file
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/MetricsHandlerTest.java b/solr/core/src/test/org/apache/solr/handler/admin/MetricsHandlerTest.java
index 2f849977e9f..f9fc8743d69 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/MetricsHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/MetricsHandlerTest.java
@@ -17,6 +17,7 @@
 
 package org.apache.solr.handler.admin;
 
import java.lang.management.ManagementFactory;
 import java.util.Map;
 
 import org.apache.solr.SolrTestCaseJ4;
@@ -33,6 +34,9 @@ import org.junit.Test;
 public class MetricsHandlerTest extends SolrTestCaseJ4 {
   @BeforeClass
   public static void beforeClass() throws Exception {
    // this is needed to enable default SolrJmxReporter in TestHarness
    ManagementFactory.getPlatformMBeanServer();

     initCore("solrconfig.xml", "schema.xml");
   }
 
@@ -135,6 +139,17 @@ public class MetricsHandlerTest extends SolrTestCaseJ4 {
     assertNotNull(values.get("CONTAINER.threadPool.coreContainerWorkExecutor.completed"));
     assertNotNull(values.get("CONTAINER.threadPool.coreLoadExecutor.completed"));
 
    resp = new SolrQueryResponse();
    handler.handleRequestBody(req(CommonParams.QT, "/admin/metrics", CommonParams.WT, "json", "prefix", "CONTAINER.cores", "regex", "C.*thread.*completed"), resp);
    values = resp.getValues();
    assertNotNull(values.get("metrics"));
    values = (NamedList) values.get("metrics");
    assertNotNull(values.get("solr.node"));
    values = (NamedList) values.get("solr.node");
    assertEquals(5, values.size());
    assertNotNull(values.get("CONTAINER.threadPool.coreContainerWorkExecutor.completed"));
    assertNotNull(values.get("CONTAINER.threadPool.coreLoadExecutor.completed"));

     resp = new SolrQueryResponse();
     handler.handleRequestBody(req(CommonParams.QT, "/admin/metrics", CommonParams.WT, "json", "group", "jvm", "prefix", "CONTAINER.cores"), resp);
     values = resp.getValues();
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java b/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
index 7bf493923e5..ca3b76e6bef 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/StatsReloadRaceTest.java
@@ -17,7 +17,6 @@
 package org.apache.solr.handler.admin;
 
 import java.util.List;
import java.util.Map;
 import java.util.Random;
 import java.util.concurrent.atomic.AtomicInteger;
 
@@ -68,7 +67,7 @@ public class StatsReloadRaceTest extends SolrTestCaseJ4 {
       boolean isCompleted;
       do {
         if (random.nextBoolean()) {
          requestMbeans();
          requestMetrics();
         } else {
           requestCoreStatus();
         }
@@ -106,22 +105,31 @@ public class StatsReloadRaceTest extends SolrTestCaseJ4 {
     return isCompleted;
   }
 
  private void requestMbeans() throws Exception {
    String stats = h.query(req(
        CommonParams.QT, "/admin/mbeans",
        "stats", "true"));

    NamedList<NamedList<Object>> actualStats = SolrInfoMBeanHandler.fromXML(stats).get("CORE");
    
    for (Map.Entry<String, NamedList<Object>> tuple : actualStats) {
      if (tuple.getKey().contains("earcher")) { // catches "searcher" and "Searcher@345345 blah"
        NamedList<Object> searcherStats = tuple.getValue();
        @SuppressWarnings("unchecked")
        NamedList<Object> statsList = (NamedList<Object>)searcherStats.get("stats");
        assertEquals("expect to have exactly one indexVersion at "+statsList, 1, statsList.getAll("indexVersion").size());
        assertTrue(statsList.get("indexVersion") instanceof Long); 
  private void requestMetrics() throws Exception {
    SolrQueryResponse rsp = new SolrQueryResponse();
    String registry = "solr.core." + h.coreName;
    String key = "SEARCHER.searcher.indexVersion";
    boolean found = false;
    int count = 10;
    while (!found && count-- > 0) {
      h.getCoreContainer().getRequestHandler("/admin/metrics").handleRequest(
          req("prefix", "SEARCHER", "registry", registry, "compact", "true"), rsp);

      NamedList values = rsp.getValues();
      NamedList metrics = (NamedList)values.get("metrics");
      metrics = (NamedList)metrics.get(registry);
      // this is not guaranteed to exist right away after core reload - there's a
      // small window between core load and before searcher metrics are registered
      // so we may have to check a few times
      if (metrics.get(key) != null) {
        found = true;
        assertTrue(metrics.get(key) instanceof Long);
        break;
      } else {
        Thread.sleep(500);
       }
     }
    assertTrue("Key " + key + " not found in registry " + registry, found);
   }
 
 }
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/SystemInfoHandlerTest.java b/solr/core/src/test/org/apache/solr/handler/admin/SystemInfoHandlerTest.java
index c961a55c5f5..2e20dc8bdba 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/SystemInfoHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/SystemInfoHandlerTest.java
@@ -20,8 +20,10 @@ import java.lang.management.ManagementFactory;
 import java.lang.management.OperatingSystemMXBean;
 import java.util.Arrays;
 
import com.codahale.metrics.Gauge;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.util.stats.MetricUtils;
 
 
 public class SystemInfoHandlerTest extends LuceneTestCase {
@@ -36,9 +38,11 @@ public class SystemInfoHandlerTest extends LuceneTestCase {
     info.add( "version", os.getVersion() );
     info.add( "arch", os.getArch() );
 
    // make another using addMXBeanProperties() 
    // make another using MetricUtils.addMXBeanMetrics()
     SimpleOrderedMap<Object> info2 = new SimpleOrderedMap<>();
    SystemInfoHandler.addMXBeanProperties( os, OperatingSystemMXBean.class, info2 );
    MetricUtils.addMXBeanMetrics( os, OperatingSystemMXBean.class, null, (k, v) -> {
      info2.add(k, ((Gauge)v).getValue());
    } );
 
     // make sure they got the same thing
     for (String p : Arrays.asList("name", "version", "arch")) {
diff --git a/solr/core/src/test/org/apache/solr/handler/component/ResourceSharingTestComponent.java b/solr/core/src/test/org/apache/solr/handler/component/ResourceSharingTestComponent.java
index 7c4e6639a1b..d268a4e424d 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/ResourceSharingTestComponent.java
++ b/solr/core/src/test/org/apache/solr/handler/component/ResourceSharingTestComponent.java
@@ -63,11 +63,6 @@ public class ResourceSharingTestComponent extends SearchComponent implements Sol
     return "ResourceSharingTestComponent";
   }
 
  @Override
  public String getSource() {
    return null;
  }

   @SuppressWarnings("unchecked")
   TestObject getTestObj() {
     return this.blob.get();
diff --git a/solr/core/src/test/org/apache/solr/metrics/JvmMetricsTest.java b/solr/core/src/test/org/apache/solr/metrics/JvmMetricsTest.java
index 72adc686354..a656f843ba7 100644
-- a/solr/core/src/test/org/apache/solr/metrics/JvmMetricsTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/JvmMetricsTest.java
@@ -16,8 +16,6 @@
  */
 package org.apache.solr.metrics;
 
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
 import java.util.Map;
 
 import com.codahale.metrics.Gauge;
@@ -31,26 +29,63 @@ import org.junit.Test;
  */
 public class JvmMetricsTest extends SolrJettyTestBase {
 
  static final String[] STRING_OS_METRICS = {
      "arch",
      "name",
      "version"
  };
  static final String[] NUMERIC_OS_METRICS = {
      "availableProcessors",
      "systemLoadAverage"
  };

  static final String[] BUFFER_METRICS = {
      "direct.Count",
      "direct.MemoryUsed",
      "direct.TotalCapacity",
      "mapped.Count",
      "mapped.MemoryUsed",
      "mapped.TotalCapacity"
  };

   @BeforeClass
   public static void beforeTest() throws Exception {
     createJetty(legacyExampleCollection1SolrHome());
   }
 
   @Test
  public void testOperatingSystemMetricsSet() throws Exception {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    OperatingSystemMetricSet set = new OperatingSystemMetricSet(mBeanServer);
  public void testOperatingSystemMetricSet() throws Exception {
    OperatingSystemMetricSet set = new OperatingSystemMetricSet();
     Map<String, Metric> metrics = set.getMetrics();
     assertTrue(metrics.size() > 0);
    for (String metric : OperatingSystemMetricSet.METRICS) {
    for (String metric : NUMERIC_OS_METRICS) {
       Gauge<?> gauge = (Gauge<?>)metrics.get(metric);
      if (gauge == null || gauge.getValue() == null) { // some are optional depending on OS
        continue;
      }
      assertNotNull(metric, gauge);
       double value = ((Number)gauge.getValue()).doubleValue();
       // SystemLoadAverage on Windows may be -1.0
       assertTrue("unexpected value of " + metric + ": " + value, value >= 0 || value == -1.0);
     }
    for (String metric : STRING_OS_METRICS) {
      Gauge<?> gauge = (Gauge<?>)metrics.get(metric);
      assertNotNull(metric, gauge);
      String value = (String)gauge.getValue();
      assertNotNull(value);
      assertFalse(value.isEmpty());
    }
  }

  @Test
  public void testAltBufferPoolMetricSet() throws Exception {
    AltBufferPoolMetricSet set = new AltBufferPoolMetricSet();
    Map<String, Metric> metrics = set.getMetrics();
    assertTrue(metrics.size() > 0);
    for (String name : BUFFER_METRICS) {
      assertNotNull(name, metrics.get(name));
      Object g = metrics.get(name);
      assertTrue(g instanceof Gauge);
      Object v = ((Gauge)g).getValue();
      assertTrue(v instanceof Long);
    }
   }
 
   @Test
diff --git a/solr/core/src/test/org/apache/solr/metrics/SolrCoreMetricManagerTest.java b/solr/core/src/test/org/apache/solr/metrics/SolrCoreMetricManagerTest.java
index 6e8e1e58e92..11f89232af0 100644
-- a/solr/core/src/test/org/apache/solr/metrics/SolrCoreMetricManagerTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/SolrCoreMetricManagerTest.java
@@ -29,7 +29,7 @@ import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.params.CoreAdminParams;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.reporters.MockMetricReporter;
 import org.apache.solr.schema.FieldType;
 import org.junit.After;
@@ -61,7 +61,7 @@ public class SolrCoreMetricManagerTest extends SolrTestCaseJ4 {
     Random random = random();
 
     String scope = SolrMetricTestUtils.getRandomScope(random);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random);
    SolrInfoBean.Category category = SolrMetricTestUtils.getRandomCategory(random);
     Map<String, Counter> metrics = SolrMetricTestUtils.getRandomMetrics(random);
     SolrMetricProducer producer = SolrMetricTestUtils.getProducerOf(metricManager, category, scope, metrics);
     try {
@@ -82,7 +82,7 @@ public class SolrCoreMetricManagerTest extends SolrTestCaseJ4 {
 
     Map<String, Counter> registered = new HashMap<>();
     String scope = SolrMetricTestUtils.getRandomScope(random, true);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
    SolrInfoBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
 
     int iterations = TestUtil.nextInt(random, 0, MAX_ITERATIONS);
     for (int i = 0; i < iterations; ++i) {
diff --git a/solr/core/src/test/org/apache/solr/metrics/SolrMetricManagerTest.java b/solr/core/src/test/org/apache/solr/metrics/SolrMetricManagerTest.java
index 1c29c5e9a73..d30611904c3 100644
-- a/solr/core/src/test/org/apache/solr/metrics/SolrMetricManagerTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/SolrMetricManagerTest.java
@@ -29,7 +29,7 @@ import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.metrics.reporters.MockMetricReporter;
 import org.junit.Test;
@@ -62,10 +62,10 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
     String toName = "to-" + TestUtil.randomSimpleString(r, 1, 10);
     // register test metrics
     for (Map.Entry<String, Counter> entry : metrics1.entrySet()) {
      metricManager.register(fromName, entry.getValue(), false, entry.getKey(), "metrics1");
      metricManager.register(null, fromName, entry.getValue(), false, entry.getKey(), "metrics1");
     }
     for (Map.Entry<String, Counter> entry : metrics2.entrySet()) {
      metricManager.register(toName, entry.getValue(), false, entry.getKey(), "metrics2");
      metricManager.register(null, toName, entry.getValue(), false, entry.getKey(), "metrics2");
     }
     assertEquals(metrics1.size(), metricManager.registry(fromName).getMetrics().size());
     assertEquals(metrics2.size(), metricManager.registry(toName).getMetrics().size());
@@ -125,13 +125,13 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
     String registryName = TestUtil.randomSimpleString(r, 1, 10);
 
     for (Map.Entry<String, Counter> entry : metrics.entrySet()) {
      metricManager.register(registryName, entry.getValue(), false, entry.getKey(), "foo", "bar");
      metricManager.register(null, registryName, entry.getValue(), false, entry.getKey(), "foo", "bar");
     }
     for (Map.Entry<String, Counter> entry : metrics.entrySet()) {
      metricManager.register(registryName, entry.getValue(), false, entry.getKey(), "foo", "baz");
      metricManager.register(null, registryName, entry.getValue(), false, entry.getKey(), "foo", "baz");
     }
     for (Map.Entry<String, Counter> entry : metrics.entrySet()) {
      metricManager.register(registryName, entry.getValue(), false, entry.getKey(), "foo");
      metricManager.register(null, registryName, entry.getValue(), false, entry.getKey(), "foo");
     }
 
     assertEquals(metrics.size() * 3, metricManager.registry(registryName).getMetrics().size());
@@ -163,10 +163,10 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
 
     String registryName = TestUtil.randomSimpleString(r, 1, 10);
 
    metricManager.counter(registryName, "simple_counter", "foo", "bar");
    metricManager.timer(registryName, "simple_timer", "foo", "bar");
    metricManager.meter(registryName, "simple_meter", "foo", "bar");
    metricManager.histogram(registryName, "simple_histogram", "foo", "bar");
    metricManager.counter(null, registryName, "simple_counter", "foo", "bar");
    metricManager.timer(null, registryName, "simple_timer", "foo", "bar");
    metricManager.meter(null, registryName, "simple_meter", "foo", "bar");
    metricManager.histogram(null, registryName, "simple_histogram", "foo", "bar");
     Map<String, Metric> metrics = metricManager.registry(registryName).getMetrics();
     assertEquals(4, metrics.size());
     for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
@@ -180,13 +180,13 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
 
     String name = TestUtil.randomSimpleString(r, 1, 10);
 
    String result = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, name, "collection1");
    String result = SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, name, "collection1");
     assertEquals("solr.core." + name + ".collection1", result);
     // try it with already prefixed name - group will be ignored
    result = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, result);
    result = SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, result);
     assertEquals("solr.core." + name + ".collection1", result);
     // try it with already prefixed name but with additional segments
    result = SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, result, "shard1", "replica1");
    result = SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, result, "shard1", "replica1");
     assertEquals("solr.core." + name + ".collection1.shard1.replica1", result);
   }
 
@@ -206,18 +206,18 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
         createPluginInfo("core_foo", "core", null)
     };
     String tag = "xyz";
    metricManager.loadReporters(plugins, loader, tag, SolrInfoMBean.Group.node);
    metricManager.loadReporters(plugins, loader, tag, SolrInfoBean.Group.node);
     Map<String, SolrMetricReporter> reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.node));
     assertEquals(4, reporters.size());
     assertTrue(reporters.containsKey("universal_foo@" + tag));
     assertTrue(reporters.containsKey("multigroup_foo@" + tag));
     assertTrue(reporters.containsKey("node_foo@" + tag));
     assertTrue(reporters.containsKey("multiregistry_foo@" + tag));
 
    metricManager.loadReporters(plugins, loader, tag, SolrInfoMBean.Group.core, "collection1");
    metricManager.loadReporters(plugins, loader, tag, SolrInfoBean.Group.core, "collection1");
     reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, "collection1"));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, "collection1"));
     assertEquals(5, reporters.size());
     assertTrue(reporters.containsKey("universal_foo@" + tag));
     assertTrue(reporters.containsKey("multigroup_foo@" + tag));
@@ -225,26 +225,26 @@ public class SolrMetricManagerTest extends SolrTestCaseJ4 {
     assertTrue(reporters.containsKey("core_foo@" + tag));
     assertTrue(reporters.containsKey("multiregistry_foo@" + tag));
 
    metricManager.loadReporters(plugins, loader, tag, SolrInfoMBean.Group.jvm);
    metricManager.loadReporters(plugins, loader, tag, SolrInfoBean.Group.jvm);
     reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.jvm));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.jvm));
     assertEquals(2, reporters.size());
     assertTrue(reporters.containsKey("universal_foo@" + tag));
     assertTrue(reporters.containsKey("multigroup_foo@" + tag));
 
     metricManager.removeRegistry("solr.jvm");
     reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.jvm));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.jvm));
     assertEquals(0, reporters.size());
 
     metricManager.removeRegistry("solr.node");
     reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.node));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.node));
     assertEquals(0, reporters.size());
 
     metricManager.removeRegistry("solr.core.collection1");
     reporters = metricManager.getReporters(
        SolrMetricManager.getRegistryName(SolrInfoMBean.Group.core, "collection1"));
        SolrMetricManager.getRegistryName(SolrInfoBean.Group.core, "collection1"));
     assertEquals(0, reporters.size());
 
   }
diff --git a/solr/core/src/test/org/apache/solr/metrics/SolrMetricReporterTest.java b/solr/core/src/test/org/apache/solr/metrics/SolrMetricReporterTest.java
index b275919a873..f3359cca5a0 100644
-- a/solr/core/src/test/org/apache/solr/metrics/SolrMetricReporterTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/SolrMetricReporterTest.java
@@ -42,6 +42,7 @@ public class SolrMetricReporterTest extends LuceneTestCase {
     Map<String, Object> attrs = new HashMap<>();
     attrs.put(FieldType.CLASS_NAME, MockMetricReporter.class.getName());
     attrs.put(CoreAdminParams.NAME, TestUtil.randomUnicodeString(random));
    attrs.put("enabled", random.nextBoolean());
 
     boolean shouldDefineConfigurable = random.nextBoolean();
     String configurable = TestUtil.randomUnicodeString(random);
diff --git a/solr/core/src/test/org/apache/solr/metrics/SolrMetricTestUtils.java b/solr/core/src/test/org/apache/solr/metrics/SolrMetricTestUtils.java
index 6bd6500b4fe..98fc9b1c810 100644
-- a/solr/core/src/test/org/apache/solr/metrics/SolrMetricTestUtils.java
++ b/solr/core/src/test/org/apache/solr/metrics/SolrMetricTestUtils.java
@@ -23,12 +23,12 @@ import java.util.Random;
 
 import com.codahale.metrics.Counter;
 import org.apache.lucene.util.TestUtil;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 
 public final class SolrMetricTestUtils {
 
   private static final int                    MAX_ITERATIONS = 100;
  private static final SolrInfoMBean.Category CATEGORIES[]   = SolrInfoMBean.Category.values();
  private static final SolrInfoBean.Category CATEGORIES[]   = SolrInfoBean.Category.values();
 
   public static String getRandomScope(Random random) {
     return getRandomScope(random, random.nextBoolean());
@@ -38,11 +38,11 @@ public final class SolrMetricTestUtils {
     return shouldDefineScope ? TestUtil.randomSimpleString(random, 1, 10) : null; // must be simple string for JMX publishing
   }
 
  public static SolrInfoMBean.Category getRandomCategory(Random random) {
  public static SolrInfoBean.Category getRandomCategory(Random random) {
     return getRandomCategory(random, random.nextBoolean());
   }
 
  public static SolrInfoMBean.Category getRandomCategory(Random random, boolean shouldDefineCategory) {
  public static SolrInfoBean.Category getRandomCategory(Random random, boolean shouldDefineCategory) {
     return shouldDefineCategory ? CATEGORIES[TestUtil.nextInt(random, 0, CATEGORIES.length - 1)] : null;
   }
 
@@ -75,7 +75,7 @@ public final class SolrMetricTestUtils {
     return metrics;
   }
 
  public static SolrMetricProducer getProducerOf(SolrMetricManager metricManager, SolrInfoMBean.Category category, String scope, Map<String, Counter> metrics) {
  public static SolrMetricProducer getProducerOf(SolrMetricManager metricManager, SolrInfoBean.Category category, String scope, Map<String, Counter> metrics) {
     return new SolrMetricProducer() {
       @Override
       public void initializeMetrics(SolrMetricManager manager, String registry, String scope) {
@@ -86,7 +86,7 @@ public final class SolrMetricTestUtils {
           return;
         }
         for (Map.Entry<String, Counter> entry : metrics.entrySet()) {
          manager.counter(registry, entry.getKey(), category.toString(), scope);
          manager.counter(null, registry, entry.getKey(), category.toString(), scope);
         }
       }
 
diff --git a/solr/core/src/test/org/apache/solr/metrics/SolrMetricsIntegrationTest.java b/solr/core/src/test/org/apache/solr/metrics/SolrMetricsIntegrationTest.java
index dfb5a0fa2da..56dab37e65e 100644
-- a/solr/core/src/test/org/apache/solr/metrics/SolrMetricsIntegrationTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/SolrMetricsIntegrationTest.java
@@ -29,10 +29,11 @@ import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.core.CoreContainer;
 import org.apache.solr.core.NodeConfig;
 import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.core.SolrXmlConfig;
 import org.apache.solr.metrics.reporters.MockMetricReporter;
import org.apache.solr.util.JmxUtil;
 import org.apache.solr.util.TestHarness;
 import org.junit.After;
 import org.junit.Before;
@@ -50,11 +51,12 @@ public class SolrMetricsIntegrationTest extends SolrTestCaseJ4 {
   private static final String MULTIREGISTRY = "multiregistry";
   private static final String[] INITIAL_REPORTERS = {REPORTER_NAMES[0], REPORTER_NAMES[1], UNIVERSAL, SPECIFIC, MULTIGROUP, MULTIREGISTRY};
   private static final String[] RENAMED_REPORTERS = {REPORTER_NAMES[0], REPORTER_NAMES[1], UNIVERSAL, MULTIGROUP};
  private static final SolrInfoMBean.Category HANDLER_CATEGORY = SolrInfoMBean.Category.QUERY;
  private static final SolrInfoBean.Category HANDLER_CATEGORY = SolrInfoBean.Category.QUERY;
 
   private CoreContainer cc;
   private SolrMetricManager metricManager;
   private String tag;
  private int jmxReporter;
 
   private void assertTagged(Map<String, SolrMetricReporter> reporters, String name) {
     assertTrue("Reporter '" + name + "' missing in " + reporters, reporters.containsKey(name + "@" + tag));
@@ -71,11 +73,12 @@ public class SolrMetricsIntegrationTest extends SolrTestCaseJ4 {
     cc = createCoreContainer(cfg,
         new TestHarness.TestCoresLocator(DEFAULT_TEST_CORENAME, initCoreDataDir.getAbsolutePath(), "solrconfig.xml", "schema.xml"));
     h.coreName = DEFAULT_TEST_CORENAME;
    jmxReporter = JmxUtil.findFirstMBeanServer() != null ? 1 : 0;
     metricManager = cc.getMetricManager();
     tag = h.getCore().getCoreMetricManager().getTag();
     // initially there are more reporters, because two of them are added via a matching collection name
     Map<String, SolrMetricReporter> reporters = metricManager.getReporters("solr.core." + DEFAULT_TEST_CORENAME);
    assertEquals(INITIAL_REPORTERS.length, reporters.size());
    assertEquals(INITIAL_REPORTERS.length + jmxReporter, reporters.size());
     for (String r : INITIAL_REPORTERS) {
       assertTagged(reporters, r);
     }
@@ -85,9 +88,9 @@ public class SolrMetricsIntegrationTest extends SolrTestCaseJ4 {
     cfg = cc.getConfig();
     PluginInfo[] plugins = cfg.getMetricReporterPlugins();
     assertNotNull(plugins);
    assertEquals(10, plugins.length);
    assertEquals(10 + jmxReporter, plugins.length);
     reporters = metricManager.getReporters("solr.node");
    assertEquals(4, reporters.size());
    assertEquals(4 + jmxReporter, reporters.size());
     assertTrue("Reporter '" + REPORTER_NAMES[0] + "' missing in solr.node", reporters.containsKey(REPORTER_NAMES[0]));
     assertTrue("Reporter '" + UNIVERSAL + "' missing in solr.node", reporters.containsKey(UNIVERSAL));
     assertTrue("Reporter '" + MULTIGROUP + "' missing in solr.node", reporters.containsKey(MULTIGROUP));
@@ -120,7 +123,7 @@ public class SolrMetricsIntegrationTest extends SolrTestCaseJ4 {
 
     String metricName = SolrMetricManager.mkName(METRIC_NAME, HANDLER_CATEGORY.toString(), HANDLER_NAME);
     SolrCoreMetricManager coreMetricManager = h.getCore().getCoreMetricManager();
    Timer timer = (Timer) metricManager.timer(coreMetricManager.getRegistryName(), metricName);
    Timer timer = (Timer) metricManager.timer(null, coreMetricManager.getRegistryName(), metricName);
 
     long initialCount = timer.getCount();
 
@@ -132,7 +135,7 @@ public class SolrMetricsIntegrationTest extends SolrTestCaseJ4 {
     long finalCount = timer.getCount();
     assertEquals("metric counter incorrect", iterations, finalCount - initialCount);
     Map<String, SolrMetricReporter> reporters = metricManager.getReporters(coreMetricManager.getRegistryName());
    assertEquals(RENAMED_REPORTERS.length, reporters.size());
    assertEquals(RENAMED_REPORTERS.length + jmxReporter, reporters.size());
 
     // SPECIFIC and MULTIREGISTRY were skipped because they were
     // specific to collection1
diff --git a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGangliaReporterTest.java b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGangliaReporterTest.java
index c50ff3c8419..eca414cd8ff 100644
-- a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGangliaReporterTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGangliaReporterTest.java
@@ -64,7 +64,7 @@ public class SolrGangliaReporterTest extends SolrTestCaseJ4 {
     h.coreName = DEFAULT_TEST_CORENAME;
     SolrMetricManager metricManager = cc.getMetricManager();
     Map<String, SolrMetricReporter> reporters = metricManager.getReporters("solr.node");
    assertEquals(1, reporters.size());
    assertTrue(reporters.toString(), reporters.size() >= 1);
     SolrMetricReporter reporter = reporters.get("test");
     assertNotNull(reporter);
     assertTrue(reporter instanceof SolrGangliaReporter);
diff --git a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGraphiteReporterTest.java b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGraphiteReporterTest.java
index f45b19359db..54385049033 100644
-- a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGraphiteReporterTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrGraphiteReporterTest.java
@@ -35,6 +35,7 @@ import org.apache.solr.core.SolrResourceLoader;
 import org.apache.solr.core.SolrXmlConfig;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricReporter;
import org.apache.solr.util.JmxUtil;
 import org.apache.solr.util.TestHarness;
 import org.junit.Test;
 
@@ -45,6 +46,7 @@ public class SolrGraphiteReporterTest extends SolrTestCaseJ4 {
 
   @Test
   public void testReporter() throws Exception {
    int jmxReporter = JmxUtil.findFirstMBeanServer() != null ? 1: 0;
     Path home = Paths.get(TEST_HOME());
     // define these properties, they are used in solrconfig.xml
     System.setProperty("solr.test.sys.prop1", "propone");
@@ -63,7 +65,7 @@ public class SolrGraphiteReporterTest extends SolrTestCaseJ4 {
       h.coreName = DEFAULT_TEST_CORENAME;
       SolrMetricManager metricManager = cc.getMetricManager();
       Map<String, SolrMetricReporter> reporters = metricManager.getReporters("solr.node");
      assertEquals(1, reporters.size());
      assertEquals(1 + jmxReporter, reporters.size());
       SolrMetricReporter reporter = reporters.get("test");
       assertNotNull(reporter);
       assertTrue(reporter instanceof SolrGraphiteReporter);
diff --git a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrJmxReporterTest.java b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrJmxReporterTest.java
index 82b9d58cc85..839a44b6a72 100644
-- a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrJmxReporterTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrJmxReporterTest.java
@@ -20,6 +20,8 @@ import javax.management.MBeanServer;
 import javax.management.ObjectInstance;
 import javax.management.ObjectName;
 
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Random;
@@ -31,7 +33,7 @@ import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.params.CoreAdminParams;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrInfoBean;
 import org.apache.solr.metrics.SolrCoreMetricManager;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricProducer;
@@ -40,12 +42,15 @@ import org.apache.solr.metrics.SolrMetricTestUtils;
 import org.apache.solr.schema.FieldType;
 import org.junit.After;
 import org.junit.Before;
import org.junit.BeforeClass;
 import org.junit.Test;
 
 public class SolrJmxReporterTest extends SolrTestCaseJ4 {
 
   private static final int MAX_ITERATIONS = 20;
 
  private static int jmxPort;

   private String domain;
 
   private SolrCoreMetricManager coreMetricManager;
@@ -53,6 +58,14 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
   private SolrJmxReporter reporter;
   private MBeanServer mBeanServer;
   private String reporterName;
  private String rootName;

  @BeforeClass
  public static void init() throws Exception {
    jmxPort = getNextAvailablePort();
    assertFalse(jmxPort == -1);
    LocateRegistry.createRegistry(jmxPort);
  }
 
   @Before
   public void beforeTest() throws Exception {
@@ -60,10 +73,11 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
 
     final SolrCore core = h.getCore();
     domain = core.getName();
    rootName = TestUtil.randomSimpleString(random(), 1, 10);
 
     coreMetricManager = core.getCoreMetricManager();
     metricManager = core.getCoreDescriptor().getCoreContainer().getMetricManager();
    PluginInfo pluginInfo = createReporterPluginInfo();
    PluginInfo pluginInfo = createReporterPluginInfo(rootName, true);
     metricManager.loadReporter(coreMetricManager.getRegistryName(), coreMetricManager.getCore().getResourceLoader(),
         pluginInfo, coreMetricManager.getTag());
 
@@ -79,7 +93,7 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
     assertNotNull("MBean server not found.", mBeanServer);
   }
 
  private PluginInfo createReporterPluginInfo() {
  private PluginInfo createReporterPluginInfo(String rootName, boolean enabled) {
     Random random = random();
     String className = SolrJmxReporter.class.getName();
     String reporterName = TestUtil.randomSimpleString(random, 1, 10);
@@ -87,6 +101,9 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
     Map<String, Object> attrs = new HashMap<>();
     attrs.put(FieldType.CLASS_NAME, className);
     attrs.put(CoreAdminParams.NAME, reporterName);
    attrs.put("rootName", rootName);
    attrs.put("enabled", enabled);
    attrs.put("serviceUrl", "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/solrjmx");
 
     boolean shouldOverrideDomain = random.nextBoolean();
     if (shouldOverrideDomain) {
@@ -114,7 +131,7 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
 
     Map<String, Counter> registered = new HashMap<>();
     String scope = SolrMetricTestUtils.getRandomScope(random, true);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
    SolrInfoBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
 
     int iterations = TestUtil.nextInt(random, 0, MAX_ITERATIONS);
     for (int i = 0; i < iterations; ++i) {
@@ -126,7 +143,7 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
       Set<ObjectInstance> objects = mBeanServer.queryMBeans(null, null);
       assertEquals(registered.size(), objects.stream().
           filter(o -> scope.equals(o.getObjectName().getKeyProperty("scope")) &&
                      reporterName.equals(o.getObjectName().getKeyProperty("reporter"))).count());
                      rootName.equals(o.getObjectName().getDomain())).count());
     }
   }
 
@@ -135,17 +152,17 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
     Random random = random();
 
     String scope = SolrMetricTestUtils.getRandomScope(random, true);
    SolrInfoMBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
    SolrInfoBean.Category category = SolrMetricTestUtils.getRandomCategory(random, true);
     Map<String, Counter> metrics = SolrMetricTestUtils.getRandomMetrics(random, true);
     SolrMetricProducer producer = SolrMetricTestUtils.getProducerOf(metricManager, category, scope, metrics);
     coreMetricManager.registerMetricProducer(scope, producer);
     Set<ObjectInstance> objects = mBeanServer.queryMBeans(null, null);
     assertEquals(metrics.size(), objects.stream().
         filter(o -> scope.equals(o.getObjectName().getKeyProperty("scope")) &&
            reporterName.equals(o.getObjectName().getKeyProperty("reporter"))).count());
        o.getObjectName().getDomain().equals(rootName)).count());
 
     h.getCoreContainer().reload(h.getCore().getName());
    PluginInfo pluginInfo = createReporterPluginInfo();
    PluginInfo pluginInfo = createReporterPluginInfo(rootName, true);
     metricManager.loadReporter(coreMetricManager.getRegistryName(), coreMetricManager.getCore().getResourceLoader(),
         pluginInfo, String.valueOf(coreMetricManager.getCore().hashCode()));
     coreMetricManager.registerMetricProducer(scope, producer);
@@ -153,7 +170,38 @@ public class SolrJmxReporterTest extends SolrTestCaseJ4 {
     objects = mBeanServer.queryMBeans(null, null);
     assertEquals(metrics.size(), objects.stream().
         filter(o -> scope.equals(o.getObjectName().getKeyProperty("scope")) &&
            pluginInfo.name.equals(o.getObjectName().getKeyProperty("reporter"))).count());
            rootName.equals(o.getObjectName().getDomain())).count());
  }

  @Test
  public void testEnabled() throws Exception {
    String root1 = TestUtil.randomSimpleString(random(), 1, 10);
    PluginInfo pluginInfo1 = createReporterPluginInfo(root1, true);
    metricManager.loadReporter(coreMetricManager.getRegistryName(), coreMetricManager.getCore().getResourceLoader(),
        pluginInfo1, coreMetricManager.getTag());

    String root2 = TestUtil.randomSimpleString(random(), 1, 10);
    assertFalse(root2.equals(root1));
    PluginInfo pluginInfo2 = createReporterPluginInfo(root2, false);
    metricManager.loadReporter(coreMetricManager.getRegistryName(), coreMetricManager.getCore().getResourceLoader(),
        pluginInfo2, coreMetricManager.getTag());

    Map<String, SolrMetricReporter> reporters = metricManager.getReporters(coreMetricManager.getRegistryName());
    assertTrue(reporters.containsKey(pluginInfo1.name + "@" + coreMetricManager.getTag()));
    assertTrue(reporters.containsKey(pluginInfo2.name + "@" + coreMetricManager.getTag()));

    String scope = SolrMetricTestUtils.getRandomScope(random(), true);
    SolrInfoBean.Category category = SolrMetricTestUtils.getRandomCategory(random(), true);
    Map<String, Counter> metrics = SolrMetricTestUtils.getRandomMetrics(random(), true);
    SolrMetricProducer producer = SolrMetricTestUtils.getProducerOf(metricManager, category, scope, metrics);
    coreMetricManager.registerMetricProducer(scope, producer);
    Set<ObjectInstance> objects = mBeanServer.queryMBeans(null, null);
    assertEquals(metrics.size(), objects.stream().
        filter(o -> scope.equals(o.getObjectName().getKeyProperty("scope")) &&
            root1.equals(o.getObjectName().getDomain())).count());
    assertEquals(0, objects.stream().
        filter(o -> scope.equals(o.getObjectName().getKeyProperty("scope")) &&
            root2.equals(o.getObjectName().getDomain())).count());
   }
 
 }
diff --git a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrSlf4jReporterTest.java b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrSlf4jReporterTest.java
index 47bf8e7216f..a8f33437913 100644
-- a/solr/core/src/test/org/apache/solr/metrics/reporters/SolrSlf4jReporterTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/reporters/SolrSlf4jReporterTest.java
@@ -57,7 +57,7 @@ public class SolrSlf4jReporterTest extends SolrTestCaseJ4 {
     h.coreName = DEFAULT_TEST_CORENAME;
     SolrMetricManager metricManager = cc.getMetricManager();
     Map<String, SolrMetricReporter> reporters = metricManager.getReporters("solr.node");
    assertEquals(2, reporters.size());
    assertTrue(reporters.toString(), reporters.size() >= 2);
     SolrMetricReporter reporter = reporters.get("test1");
     assertNotNull(reporter);
     assertTrue(reporter instanceof SolrSlf4jReporter);
diff --git a/solr/core/src/test/org/apache/solr/metrics/reporters/solr/SolrCloudReportersTest.java b/solr/core/src/test/org/apache/solr/metrics/reporters/solr/SolrCloudReportersTest.java
index 91952b889d8..a63f12bb950 100644
-- a/solr/core/src/test/org/apache/solr/metrics/reporters/solr/SolrCloudReportersTest.java
++ b/solr/core/src/test/org/apache/solr/metrics/reporters/solr/SolrCloudReportersTest.java
@@ -28,6 +28,7 @@ import org.apache.solr.core.SolrCore;
 import org.apache.solr.metrics.AggregateMetric;
 import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.metrics.SolrMetricReporter;
import org.apache.solr.util.JmxUtil;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -38,11 +39,13 @@ import org.junit.Test;
 public class SolrCloudReportersTest extends SolrCloudTestCase {
   int leaderRegistries;
   int clusterRegistries;
  static int jmxReporter;
 
 
   @BeforeClass
   public static void configureDummyCluster() throws Exception {
     configureCluster(0).configure();
    jmxReporter = JmxUtil.findFirstMBeanServer() != null ? 1 : 0;
   }
 
   @Before
@@ -97,7 +100,7 @@ public class SolrCloudReportersTest extends SolrCloudTestCase {
       assertEquals(5, sor.getPeriod());
       for (String registryName : metricManager.registryNames(".*\\.shard[0-9]\\.replica.*")) {
         reporters = metricManager.getReporters(registryName);
        assertEquals(reporters.toString(), 1, reporters.size());
        assertEquals(reporters.toString(), 1 + jmxReporter, reporters.size());
         reporter = null;
         for (String name : reporters.keySet()) {
           if (name.startsWith("test")) {
@@ -156,7 +159,7 @@ public class SolrCloudReportersTest extends SolrCloudTestCase {
       assertEquals(reporters.toString(), 0, reporters.size());
       for (String registryName : metricManager.registryNames(".*\\.shard[0-9]\\.replica.*")) {
         reporters = metricManager.getReporters(registryName);
        assertEquals(reporters.toString(), 0, reporters.size());
        assertEquals(reporters.toString(), 0 + jmxReporter, reporters.size());
       }
     });
   }
diff --git a/solr/core/src/test/org/apache/solr/search/MockSearchComponent.java b/solr/core/src/test/org/apache/solr/search/MockSearchComponent.java
index 1539dfd919a..874b21a8c6b 100644
-- a/solr/core/src/test/org/apache/solr/search/MockSearchComponent.java
++ b/solr/core/src/test/org/apache/solr/search/MockSearchComponent.java
@@ -46,10 +46,4 @@ public class MockSearchComponent extends SearchComponent {
   public String getDescription() {
     return "Mock search component for tests";
   }

  @Override
  public String getSource() {
    return "";
  }
  
 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestFastLRUCache.java b/solr/core/src/test/org/apache/solr/search/TestFastLRUCache.java
index 0034b13e4d9..72fc9cedbb5 100644
-- a/solr/core/src/test/org/apache/solr/search/TestFastLRUCache.java
++ b/solr/core/src/test/org/apache/solr/search/TestFastLRUCache.java
@@ -17,12 +17,13 @@
 package org.apache.solr.search;
 
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.util.NamedList;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.util.ConcurrentLRUCache;
 import org.apache.solr.util.RTimer;
 
 import java.io.IOException;
import java.io.Serializable;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Random;
@@ -37,9 +38,14 @@ import java.util.concurrent.atomic.AtomicInteger;
  * @since solr 1.4
  */
 public class TestFastLRUCache extends LuceneTestCase {
  
  SolrMetricManager metricManager = new SolrMetricManager();
  String registry = TestUtil.randomSimpleString(random(), 2, 10);
  String scope = TestUtil.randomSimpleString(random(), 2, 10);

   public void testPercentageAutowarm() throws IOException {
     FastLRUCache<Object, Object> fastCache = new FastLRUCache<>();
    fastCache.initializeMetrics(metricManager, registry, scope);
    MetricsMap metrics = fastCache.getMetricsMap();
     Map<String, String> params = new HashMap<>();
     params.put("size", "100");
     params.put("initialSize", "10");
@@ -52,12 +58,14 @@ public class TestFastLRUCache extends LuceneTestCase {
     }
     assertEquals("25", fastCache.get(25));
     assertEquals(null, fastCache.get(110));
    NamedList<Serializable> nl = fastCache.getStatistics();
    Map<String,Object> nl = metrics.getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(1L, nl.get("hits"));
     assertEquals(101L, nl.get("inserts"));
     assertEquals(null, fastCache.get(1));  // first item put in should be the first out
     FastLRUCache<Object, Object> fastCacheNew = new FastLRUCache<>();
    fastCacheNew.initializeMetrics(metricManager, registry, scope);
    metrics = fastCacheNew.getMetricsMap();
     fastCacheNew.init(params, o, cr);
     fastCacheNew.warm(null, fastCache);
     fastCacheNew.setState(SolrCache.State.LIVE);
@@ -65,7 +73,7 @@ public class TestFastLRUCache extends LuceneTestCase {
     fastCacheNew.put(103, "103");
     assertEquals("90", fastCacheNew.get(90));
     assertEquals("50", fastCacheNew.get(50));
    nl = fastCacheNew.getStatistics();
    nl = metrics.getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(2L, nl.get("hits"));
     assertEquals(1L, nl.get("inserts"));
@@ -86,6 +94,7 @@ public class TestFastLRUCache extends LuceneTestCase {
   
   private void doTestPercentageAutowarm(int limit, int percentage, int[] hits, int[]misses) {
     FastLRUCache<Object, Object> fastCache = new FastLRUCache<>();
    fastCache.initializeMetrics(metricManager, registry, scope);
     Map<String, String> params = new HashMap<>();
     params.put("size", String.valueOf(limit));
     params.put("initialSize", "10");
@@ -98,6 +107,7 @@ public class TestFastLRUCache extends LuceneTestCase {
     }
 
     FastLRUCache<Object, Object> fastCacheNew = new FastLRUCache<>();
    fastCacheNew.initializeMetrics(metricManager, registry, scope);
     fastCacheNew.init(params, o, cr);
     fastCacheNew.warm(null, fastCache);
     fastCacheNew.setState(SolrCache.State.LIVE);
@@ -110,7 +120,7 @@ public class TestFastLRUCache extends LuceneTestCase {
     for(int miss:misses) {
       assertEquals("The value " + miss + " should NOT be on new cache", null, fastCacheNew.get(miss));
     }
    NamedList<Serializable> nl = fastCacheNew.getStatistics();
    Map<String,Object> nl = fastCacheNew.getMetricsMap().getValue();
     assertEquals(Long.valueOf(hits.length + misses.length), nl.get("lookups"));
     assertEquals(Long.valueOf(hits.length), nl.get("hits"));
     fastCacheNew.close();
@@ -118,6 +128,7 @@ public class TestFastLRUCache extends LuceneTestCase {
   
   public void testNoAutowarm() throws IOException {
     FastLRUCache<Object, Object> fastCache = new FastLRUCache<>();
    fastCache.initializeMetrics(metricManager, registry, scope);
     Map<String, String> params = new HashMap<>();
     params.put("size", "100");
     params.put("initialSize", "10");
@@ -129,7 +140,7 @@ public class TestFastLRUCache extends LuceneTestCase {
     }
     assertEquals("25", fastCache.get(25));
     assertEquals(null, fastCache.get(110));
    NamedList<Serializable> nl = fastCache.getStatistics();
    Map<String,Object> nl = fastCache.getMetricsMap().getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(1L, nl.get("hits"));
     assertEquals(101L, nl.get("inserts"));
@@ -177,6 +188,7 @@ public class TestFastLRUCache extends LuceneTestCase {
   
   public void testSimple() throws IOException {
     FastLRUCache sc = new FastLRUCache();
    sc.initializeMetrics(metricManager, registry, scope);
     Map l = new HashMap();
     l.put("size", "100");
     l.put("initialSize", "10");
@@ -189,7 +201,8 @@ public class TestFastLRUCache extends LuceneTestCase {
     }
     assertEquals("25", sc.get(25));
     assertEquals(null, sc.get(110));
    NamedList nl = sc.getStatistics();
    MetricsMap metrics = sc.getMetricsMap();
    Map<String,Object> nl = metrics.getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(1L, nl.get("hits"));
     assertEquals(101L, nl.get("inserts"));
@@ -198,6 +211,7 @@ public class TestFastLRUCache extends LuceneTestCase {
 
 
     FastLRUCache scNew = new FastLRUCache();
    scNew.initializeMetrics(metricManager, registry, scope);
     scNew.init(l, o, cr);
     scNew.warm(null, sc);
     scNew.setState(SolrCache.State.LIVE);
@@ -205,7 +219,7 @@ public class TestFastLRUCache extends LuceneTestCase {
     scNew.put(103, "103");
     assertEquals("90", scNew.get(90));
     assertEquals(null, scNew.get(50));
    nl = scNew.getStatistics();
    nl = scNew.getMetricsMap().getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(1L, nl.get("hits"));
     assertEquals(1L, nl.get("inserts"));
diff --git a/solr/core/src/test/org/apache/solr/search/TestIndexSearcher.java b/solr/core/src/test/org/apache/solr/search/TestIndexSearcher.java
index 8fe3f9717ab..c36066a9818 100644
-- a/solr/core/src/test/org/apache/solr/search/TestIndexSearcher.java
++ b/solr/core/src/test/org/apache/solr/search/TestIndexSearcher.java
@@ -17,6 +17,7 @@
 package org.apache.solr.search;
 
 import java.io.IOException;
import java.util.Date;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.CountDownLatch;
@@ -25,6 +26,8 @@ import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
 import com.google.common.collect.ImmutableMap;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexReaderContext;
@@ -137,13 +140,15 @@ public class TestIndexSearcher extends SolrTestCaseJ4 {
     int baseRefCount = r3.getRefCount();
     assertEquals(1, baseRefCount);
 
    Object sr3SearcherRegAt = sr3.getSearcher().getStatistics().get("registeredAt");
    Map<String, Metric> metrics = h.getCore().getCoreMetricManager().getRegistry().getMetrics();
    Gauge<Date> g = (Gauge<Date>)metrics.get("SEARCHER.searcher.registeredAt");
    Date sr3SearcherRegAt = g.getValue();
     assertU(commit()); // nothing has changed
     SolrQueryRequest sr4 = req("q","foo");
     assertSame("nothing changed, searcher should be the same",
                sr3.getSearcher(), sr4.getSearcher());
     assertEquals("nothing changed, searcher should not have been re-registered",
                 sr3SearcherRegAt, sr4.getSearcher().getStatistics().get("registeredAt"));
                 sr3SearcherRegAt, g.getValue());
     IndexReader r4 = sr4.getSearcher().getRawReader();
 
     // force an index change so the registered searcher won't be the one we are testing (and
diff --git a/solr/core/src/test/org/apache/solr/search/TestLFUCache.java b/solr/core/src/test/org/apache/solr/search/TestLFUCache.java
index d137875653a..8207522ddca 100644
-- a/solr/core/src/test/org/apache/solr/search/TestLFUCache.java
++ b/solr/core/src/test/org/apache/solr/search/TestLFUCache.java
@@ -16,9 +16,10 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.SolrMetricManager;
 import org.apache.solr.util.ConcurrentLFUCache;
 import org.apache.solr.util.DefaultSolrThreadFactory;
 import org.apache.solr.util.RefCounted;
@@ -32,6 +33,7 @@ import java.lang.invoke.MethodHandles;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
import java.util.Random;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicReference;
@@ -59,7 +61,7 @@ public class TestLFUCache extends SolrTestCaseJ4 {
       SolrIndexSearcher searcher = holder.get();
       LFUCache cacheDecayTrue = (LFUCache) searcher.getCache("lfuCacheDecayTrue");
       assertNotNull(cacheDecayTrue);
      NamedList stats = cacheDecayTrue.getStatistics();
      Map<String,Object> stats = cacheDecayTrue.getMetricsMap().getValue();
       assertTrue((Boolean) stats.get("timeDecay"));
       addCache(cacheDecayTrue, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
       for (int idx = 0; idx < 64; ++idx) {
@@ -70,7 +72,7 @@ public class TestLFUCache extends SolrTestCaseJ4 {
 
       LFUCache cacheDecayDefault = (LFUCache) searcher.getCache("lfuCacheDecayDefault");
       assertNotNull(cacheDecayDefault);
      stats = cacheDecayDefault.getStatistics();
      stats = cacheDecayDefault.getMetricsMap().getValue();
       assertTrue((Boolean) stats.get("timeDecay"));
       addCache(cacheDecayDefault, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
       assertCache(cacheDecayDefault, 1, 2, 3, 4, 5);
@@ -84,7 +86,7 @@ public class TestLFUCache extends SolrTestCaseJ4 {
 
       LFUCache cacheDecayFalse = (LFUCache) searcher.getCache("lfuCacheDecayFalse");
       assertNotNull(cacheDecayFalse);
      stats = cacheDecayFalse.getStatistics();
      stats = cacheDecayFalse.getMetricsMap().getValue();
       assertFalse((Boolean) stats.get("timeDecay"));
       addCache(cacheDecayFalse, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
       assertCache(cacheDecayFalse, 1, 2, 3, 4, 5);
@@ -131,9 +133,16 @@ public class TestLFUCache extends SolrTestCaseJ4 {
 
   @Test
   public void testSimple() throws IOException {
    SolrMetricManager metricManager = new SolrMetricManager();
    Random r = random();
    String registry = TestUtil.randomSimpleString(r, 2, 10);
    String scope = TestUtil.randomSimpleString(r, 2, 10);
     LFUCache lfuCache = new LFUCache();
     LFUCache newLFUCache = new LFUCache();
     LFUCache noWarmLFUCache = new LFUCache();
    lfuCache.initializeMetrics(metricManager, registry, scope + ".lfuCache");
    newLFUCache.initializeMetrics(metricManager, registry, scope + ".newLFUCache");
    noWarmLFUCache.initializeMetrics(metricManager, registry, scope + ".noWarmLFUCache");
     try {
       Map params = new HashMap();
       params.put("size", "100");
@@ -148,7 +157,7 @@ public class TestLFUCache extends SolrTestCaseJ4 {
       assertEquals("15", lfuCache.get(15));
       assertEquals("75", lfuCache.get(75));
       assertEquals(null, lfuCache.get(110));
      NamedList nl = lfuCache.getStatistics();
      Map<String,Object> nl = lfuCache.getMetricsMap().getValue();
       assertEquals(3L, nl.get("lookups"));
       assertEquals(2L, nl.get("hits"));
       assertEquals(101L, nl.get("inserts"));
@@ -164,7 +173,7 @@ public class TestLFUCache extends SolrTestCaseJ4 {
       assertEquals("15", newLFUCache.get(15));
       assertEquals("75", newLFUCache.get(75));
       assertEquals(null, newLFUCache.get(50));
      nl = newLFUCache.getStatistics();
      nl = newLFUCache.getMetricsMap().getValue();
       assertEquals(3L, nl.get("lookups"));
       assertEquals(2L, nl.get("hits"));
       assertEquals(1L, nl.get("inserts"));
diff --git a/solr/core/src/test/org/apache/solr/search/TestLRUCache.java b/solr/core/src/test/org/apache/solr/search/TestLRUCache.java
index d2f74de5960..fa34911b80a 100644
-- a/solr/core/src/test/org/apache/solr/search/TestLRUCache.java
++ b/solr/core/src/test/org/apache/solr/search/TestLRUCache.java
@@ -17,21 +17,25 @@
 package org.apache.solr.search;
 
 import java.io.IOException;
import java.io.Serializable;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.apache.lucene.util.Accountable;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.TestUtil;
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.SolrMetricManager;
 
 /**
  * Test for <code>org.apache.solr.search.LRUCache</code>
  */
 public class TestLRUCache extends LuceneTestCase {
 
  SolrMetricManager metricManager = new SolrMetricManager();
  String registry = TestUtil.randomSimpleString(random(), 2, 10);
  String scope = TestUtil.randomSimpleString(random(), 2, 10);

   public void testFullAutowarm() throws IOException {
     LRUCache<Object, Object> lruCache = new LRUCache<>();
     Map<String, String> params = new HashMap<>();
@@ -97,6 +101,7 @@ public class TestLRUCache extends LuceneTestCase {
   @SuppressWarnings("unchecked")
   public void testNoAutowarm() throws IOException {
     LRUCache<Object, Object> lruCache = new LRUCache<>();
    lruCache.initializeMetrics(metricManager, registry, scope);
     Map<String, String> params = new HashMap<>();
     params.put("size", "100");
     params.put("initialSize", "10");
@@ -108,7 +113,7 @@ public class TestLRUCache extends LuceneTestCase {
     }
     assertEquals("25", lruCache.get(25));
     assertEquals(null, lruCache.get(110));
    NamedList<Serializable> nl = lruCache.getStatistics();
    Map<String,Object> nl = lruCache.getMetricsMap().getValue();
     assertEquals(2L, nl.get("lookups"));
     assertEquals(1L, nl.get("hits"));
     assertEquals(101L, nl.get("inserts"));
@@ -126,6 +131,7 @@ public class TestLRUCache extends LuceneTestCase {
 
   public void testMaxRamSize() throws Exception {
     LRUCache<String, Accountable> accountableLRUCache = new LRUCache<>();
    accountableLRUCache.initializeMetrics(metricManager, registry, scope);
     Map<String, String> params = new HashMap<>();
     params.put("size", "5");
     params.put("maxRamMB", "1");
@@ -149,7 +155,7 @@ public class TestLRUCache extends LuceneTestCase {
     });
     assertEquals(1, accountableLRUCache.size());
     assertEquals(baseSize + 512 * 1024 + LRUCache.LINKED_HASHTABLE_RAM_BYTES_PER_ENTRY + LRUCache.DEFAULT_RAM_BYTES_USED, accountableLRUCache.ramBytesUsed());
    NamedList<Serializable> nl = accountableLRUCache.getStatistics();
    Map<String,Object> nl = accountableLRUCache.getMetricsMap().getValue();
     assertEquals(1L, nl.get("evictions"));
     assertEquals(1L, nl.get("evictionsRamUsage"));
     accountableLRUCache.put("3", new Accountable() {
@@ -158,7 +164,7 @@ public class TestLRUCache extends LuceneTestCase {
         return 1024;
       }
     });
    nl = accountableLRUCache.getStatistics();
    nl = accountableLRUCache.getMetricsMap().getValue();
     assertEquals(1L, nl.get("evictions"));
     assertEquals(1L, nl.get("evictionsRamUsage"));
     assertEquals(2L, accountableLRUCache.size());
diff --git a/solr/core/src/test/org/apache/solr/search/TestReRankQParserPlugin.java b/solr/core/src/test/org/apache/solr/search/TestReRankQParserPlugin.java
index e4d6a5b5fff..42d05e9c891 100644
-- a/solr/core/src/test/org/apache/solr/search/TestReRankQParserPlugin.java
++ b/solr/core/src/test/org/apache/solr/search/TestReRankQParserPlugin.java
@@ -16,11 +16,12 @@
  */
 package org.apache.solr.search;
 
import java.util.Map;

 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.metrics.MetricsMap;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -376,8 +377,8 @@ public class TestReRankQParserPlugin extends SolrTestCaseJ4 {
         "//result/doc[5]/float[@name='id'][.='2.0']"
     );
 
    SolrInfoMBean info  = h.getCore().getInfoRegistry().get("queryResultCache");
    NamedList stats = info.getStatistics();
    MetricsMap metrics = (MetricsMap)h.getCore().getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.queryResultCache");
    Map<String,Object> stats = metrics.getValue();
 
     long inserts = (Long) stats.get("inserts");
 
@@ -401,8 +402,7 @@ public class TestReRankQParserPlugin extends SolrTestCaseJ4 {
     );
 
 
    info  = h.getCore().getInfoRegistry().get("queryResultCache");
    stats = info.getStatistics();
    stats = metrics.getValue();
 
     long inserts1 = (Long) stats.get("inserts");
 
@@ -426,8 +426,7 @@ public class TestReRankQParserPlugin extends SolrTestCaseJ4 {
         "//result/doc[5]/float[@name='id'][.='1.0']"
     );
 
    info  = h.getCore().getInfoRegistry().get("queryResultCache");
    stats = info.getStatistics();
    stats = metrics.getValue();
     long inserts2 = (Long) stats.get("inserts");
     //Last query was NOT added to the cache
     assertTrue(inserts1 == inserts2);
diff --git a/solr/core/src/test/org/apache/solr/search/TestSolr4Spatial2.java b/solr/core/src/test/org/apache/solr/search/TestSolr4Spatial2.java
index 1fcfe9a7537..b909f15005a 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSolr4Spatial2.java
++ b/solr/core/src/test/org/apache/solr/search/TestSolr4Spatial2.java
@@ -20,6 +20,7 @@ import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.params.FacetParams;
 import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.request.SolrQueryRequest;
 import org.junit.Before;
 import org.junit.BeforeClass;
@@ -117,13 +118,13 @@ public class TestSolr4Spatial2 extends SolrTestCaseJ4 {
 
     // The tricky thing is verifying the cache works correctly...
 
    SolrCache cache = (SolrCache) h.getCore().getInfoRegistry().get("perSegSpatialFieldCache_srptgeom");
    assertEquals("1", cache.getStatistics().get("cumulative_inserts").toString());
    assertEquals("0", cache.getStatistics().get("cumulative_hits").toString());
    MetricsMap cacheMetrics = (MetricsMap) h.getCore().getCoreMetricManager().getRegistry().getMetrics().get("CACHE.searcher.perSegSpatialFieldCache_srptgeom");
    assertEquals("1", cacheMetrics.getValue().get("cumulative_inserts").toString());
    assertEquals("0", cacheMetrics.getValue().get("cumulative_hits").toString());
 
     // Repeat the query earlier
     assertJQ(sameReq, "/response/numFound==1", "/response/docs/[0]/id=='1'");
    assertEquals("1", cache.getStatistics().get("cumulative_hits").toString());
    assertEquals("1", cacheMetrics.getValue().get("cumulative_hits").toString());
 
     assertEquals("1 segment",
         1, getSearcher().getRawReader().leaves().size());
@@ -141,7 +142,7 @@ public class TestSolr4Spatial2 extends SolrTestCaseJ4 {
     // When there are new segments, we accumulate another hit. This tests the cache was not blown away on commit.
     // Checking equality for the first reader's cache key indicates wether the cache should still be valid.
     Object leafKey2 = getFirstLeafReaderKey();
    assertEquals(leafKey1.equals(leafKey2) ? "2" : "1", cache.getStatistics().get("cumulative_hits").toString());
    assertEquals(leafKey1.equals(leafKey2) ? "2" : "1", cacheMetrics.getValue().get("cumulative_hits").toString());
 
 
     // Now try to see if heatmaps work:
diff --git a/solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheMBean.java b/solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheBean.java
similarity index 59%
rename from solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheMBean.java
rename to solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheBean.java
index d11c9192090..3ae9c472073 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheMBean.java
++ b/solr/core/src/test/org/apache/solr/search/TestSolrFieldCacheBean.java
@@ -16,17 +16,21 @@
  */
 package org.apache.solr.search;
 
import org.apache.lucene.util.TestUtil;
 import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.NamedList;
 
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Random;
 
public class TestSolrFieldCacheMBean extends SolrTestCaseJ4 {
public class TestSolrFieldCacheBean extends SolrTestCaseJ4 {
 
   private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
@@ -66,18 +70,28 @@ public class TestSolrFieldCacheMBean extends SolrTestCaseJ4 {
   }
 
   private void assertEntryListIncluded(boolean checkJmx) {
    SolrFieldCacheMBean mbean = new SolrFieldCacheMBean();
    NamedList stats = checkJmx ? mbean.getStatisticsForJmx() : mbean.getStatistics();
    assert(Integer.parseInt(stats.get("entries_count").toString()) > 0);
    assertNotNull(stats.get("total_size"));
    assertNotNull(stats.get("entry#0"));
    SolrFieldCacheBean mbean = new SolrFieldCacheBean();
    Random r = random();
    String registryName = TestUtil.randomSimpleString(r, 1, 10);
    SolrMetricManager metricManager = h.getCoreContainer().getMetricManager();
    mbean.initializeMetrics(metricManager, registryName, null);
    MetricsMap metricsMap = (MetricsMap)metricManager.registry(registryName).getMetrics().get("CACHE.fieldCache");
    Map<String, Object> metrics = checkJmx ? metricsMap.getValue(true) : metricsMap.getValue();
    assertTrue(((Number)metrics.get("entries_count")).longValue() > 0);
    assertNotNull(metrics.get("total_size"));
    assertNotNull(metrics.get("entry#0"));
   }
 
   private void assertEntryListNotIncluded(boolean checkJmx) {
    SolrFieldCacheMBean mbean = new SolrFieldCacheMBean();
    NamedList stats = checkJmx ? mbean.getStatisticsForJmx() : mbean.getStatistics();
    assert(Integer.parseInt(stats.get("entries_count").toString()) > 0);
    assertNull(stats.get("total_size"));
    assertNull(stats.get("entry#0"));
    SolrFieldCacheBean mbean = new SolrFieldCacheBean();
    Random r = random();
    String registryName = TestUtil.randomSimpleString(r, 1, 10);
    SolrMetricManager metricManager = h.getCoreContainer().getMetricManager();
    mbean.initializeMetrics(metricManager, registryName, null);
    MetricsMap metricsMap = (MetricsMap)metricManager.registry(registryName).getMetrics().get("CACHE.fieldCache");
    Map<String, Object> metrics = checkJmx ? metricsMap.getValue(true) : metricsMap.getValue();
    assertTrue(((Number)metrics.get("entries_count")).longValue() > 0);
    assertNull(metrics.get("total_size"));
    assertNull(metrics.get("entry#0"));
   }
 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java b/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
index e1372d81d7d..f454848d29b 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
++ b/solr/core/src/test/org/apache/solr/search/TestSolrQueryParser.java
@@ -34,9 +34,9 @@ import org.apache.lucene.search.TermInSetQuery;
 import org.apache.lucene.search.TermQuery;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.common.params.ModifiableSolrParams;
 import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrInfoMBean;
 import org.apache.solr.parser.QueryParser;
 import org.apache.solr.query.FilterQuery;
 import org.apache.solr.request.SolrQueryRequest;
@@ -389,33 +389,33 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
     assertU(commit());  // arg... commit no longer "commits" unless there has been a change.
 
 
    final SolrInfoMBean filterCacheStats
        = h.getCore().getInfoRegistry().get("filterCache");
    final MetricsMap filterCacheStats = (MetricsMap)h.getCore().getCoreMetricManager().getRegistry()
        .getMetrics().get("CACHE.searcher.filterCache");
     assertNotNull(filterCacheStats);
    final SolrInfoMBean queryCacheStats
        = h.getCore().getInfoRegistry().get("queryResultCache");
    final MetricsMap queryCacheStats = (MetricsMap)h.getCore().getCoreMetricManager().getRegistry()
        .getMetrics().get("CACHE.searcher.queryResultCache");
 
     assertNotNull(queryCacheStats);
 
 
    long inserts = (Long) filterCacheStats.getStatistics().get("inserts");
    long hits = (Long) filterCacheStats.getStatistics().get("hits");
    long inserts = (Long) filterCacheStats.getValue().get("inserts");
    long hits = (Long) filterCacheStats.getValue().get("hits");
 
     assertJQ(req("q", "doesnotexist filter(id:1) filter(qqq_s:X) filter(abcdefg)")
         , "/response/numFound==2"
     );
 
     inserts += 3;
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
     assertJQ(req("q", "doesnotexist2 filter(id:1) filter(qqq_s:X) filter(abcdefg)")
         , "/response/numFound==2"
     );
 
     hits += 3;
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
     // make sure normal "fq" parameters also hit the cache the same way
     assertJQ(req("q", "doesnotexist3", "fq", "id:1", "fq", "qqq_s:X", "fq", "abcdefg")
@@ -423,8 +423,8 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
     );
 
     hits += 3;
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
     // try a query deeply nested in a FQ
     assertJQ(req("q", "*:* doesnotexist4", "fq", "(id:* +(filter(id:1) filter(qqq_s:X) filter(abcdefg)) )")
@@ -433,8 +433,8 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
 
     inserts += 1;  // +1 for top level fq
     hits += 3;
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
     // retry the complex FQ and make sure hashCode/equals works as expected w/ filter queries
     assertJQ(req("q", "*:* doesnotexist5", "fq", "(id:* +(filter(id:1) filter(qqq_s:X) filter(abcdefg)) )")
@@ -442,8 +442,8 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
     );
 
     hits += 1;  // top-level fq should have been found.
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
 
     // try nested filter with multiple top-level args (i.e. a boolean query)
@@ -453,8 +453,8 @@ public class TestSolrQueryParser extends SolrTestCaseJ4 {
 
     hits += 1;  // the inner filter
     inserts += 1; // the outer filter
    assertEquals(inserts, ((Long) filterCacheStats.getStatistics().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getStatistics().get("hits")).longValue());
    assertEquals(inserts, ((Long) filterCacheStats.getValue().get("inserts")).longValue());
    assertEquals(hits, ((Long) filterCacheStats.getValue().get("hits")).longValue());
 
     // test the score for a filter, and that default score is 0
     assertJQ(req("q", "+filter(*:*) +filter(id:1)", "fl", "id,score", "sort", "id asc")
diff --git a/solr/core/src/test/org/apache/solr/search/join/BJQParserTest.java b/solr/core/src/test/org/apache/solr/search/join/BJQParserTest.java
index 39fa7915a25..8c2cec36e85 100644
-- a/solr/core/src/test/org/apache/solr/search/join/BJQParserTest.java
++ b/solr/core/src/test/org/apache/solr/search/join/BJQParserTest.java
@@ -19,8 +19,7 @@ package org.apache.solr.search.join;
 import org.apache.lucene.search.join.ScoreMode;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.SolrCache;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.util.BaseTestHarness;
 import org.junit.BeforeClass;
 import org.junit.Test;
@@ -32,6 +31,7 @@ import java.util.Collections;
 import java.util.List;
 import java.util.ListIterator;
 import java.util.Locale;
import java.util.Map;
 
 import javax.xml.xpath.XPathConstants;
 
@@ -276,15 +276,15 @@ public class BJQParserTest extends SolrTestCaseJ4 {
   @Test
   public void testCacheHit() throws IOException {
 
    SolrCache parentFilterCache = (SolrCache) h.getCore().getInfoRegistry()
        .get("perSegFilter");
    MetricsMap parentFilterCache = (MetricsMap)h.getCore().getCoreMetricManager().getRegistry()
        .getMetrics().get("CACHE.searcher.perSegFilter");
    MetricsMap filterCache = (MetricsMap)h.getCore().getCoreMetricManager().getRegistry()
        .getMetrics().get("CACHE.searcher.filterCache");
 
    SolrCache filterCache = (SolrCache) h.getCore().getInfoRegistry()
        .get("filterCache");
 
    NamedList parentsBefore = parentFilterCache.getStatistics();
    Map<String,Object> parentsBefore = parentFilterCache.getValue();
 
    NamedList filtersBefore = filterCache.getStatistics();
    Map<String,Object> filtersBefore = filterCache.getValue();
 
     // it should be weird enough to be uniq
     String parentFilter = "parent_s:([a TO c] [d TO f])";
@@ -298,7 +298,7 @@ public class BJQParserTest extends SolrTestCaseJ4 {
         "//*[@numFound='6']");
 
     assertEquals("didn't hit fqCache yet ", 0L,
        delta("hits", filterCache.getStatistics(), filtersBefore));
        delta("hits", filterCache.getValue(), filtersBefore));
 
     assertQ(
         "filter by join",
@@ -306,18 +306,18 @@ public class BJQParserTest extends SolrTestCaseJ4 {
             + "\"}child_s:l"), "//*[@numFound='6']");
 
     assertEquals("in cache mode every request lookups", 3,
        delta("lookups", parentFilterCache.getStatistics(), parentsBefore));
        delta("lookups", parentFilterCache.getValue(), parentsBefore));
     assertEquals("last two lookups causes hits", 2,
        delta("hits", parentFilterCache.getStatistics(), parentsBefore));
        delta("hits", parentFilterCache.getValue(), parentsBefore));
     assertEquals("the first lookup gets insert", 1,
        delta("inserts", parentFilterCache.getStatistics(), parentsBefore));
        delta("inserts", parentFilterCache.getValue(), parentsBefore));
 
 
     assertEquals("true join query is cached in fqCache", 1L,
        delta("lookups", filterCache.getStatistics(), filtersBefore));
        delta("lookups", filterCache.getValue(), filtersBefore));
   }
   
  private long delta(String key, NamedList a, NamedList b) {
  private long delta(String key, Map<String,Object> a, Map<String,Object> b) {
     return (Long) a.get(key) - (Long) b.get(key);
   }
 
diff --git a/solr/core/src/test/org/apache/solr/search/join/TestScoreJoinQPScore.java b/solr/core/src/test/org/apache/solr/search/join/TestScoreJoinQPScore.java
index 17abf7834f1..b9a2e78ffe2 100644
-- a/solr/core/src/test/org/apache/solr/search/join/TestScoreJoinQPScore.java
++ b/solr/core/src/test/org/apache/solr/search/join/TestScoreJoinQPScore.java
@@ -21,14 +21,16 @@ import java.util.Arrays;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Locale;
import java.util.Map;
 import java.util.Random;
 
import com.codahale.metrics.Metric;
 import org.apache.lucene.search.BoostQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.join.ScoreMode;
 import org.apache.solr.SolrTestCaseJ4;
 import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.MetricsMap;
 import org.apache.solr.request.SolrQueryRequest;
 import org.apache.solr.request.SolrRequestInfo;
 import org.apache.solr.response.SolrQueryResponse;
@@ -198,22 +200,23 @@ public class TestScoreJoinQPScore extends SolrTestCaseJ4 {
   public void testCacheHit() throws Exception {
     indexDataForScorring();
 
    SolrCache cache = (SolrCache) h.getCore().getInfoRegistry()
        .get("queryResultCache");
    Map<String, Metric> metrics = h.getCoreContainer().getMetricManager().registry(h.getCore().getCoreMetricManager().getRegistryName()).getMetrics();

    MetricsMap mm = (MetricsMap)metrics.get("CACHE.searcher.queryResultCache");
     {
      final NamedList statPre = cache.getStatistics();
      Map<String,Object> statPre = mm.getValue();
       h.query(req("q", "{!join from=movieId_s to=id score=Avg}title:first", "fl", "id", "omitHeader", "true"));
      assertHitOrInsert(cache, statPre);
      assertHitOrInsert(mm.getValue(), statPre);
     }
 
     {
      final NamedList statPre = cache.getStatistics();
      Map<String,Object> statPre = mm.getValue();
       h.query(req("q", "{!join from=movieId_s to=id score=Avg}title:first", "fl", "id", "omitHeader", "true"));
      assertHit(cache, statPre);
      assertHit(mm.getValue(), statPre);
     }
 
     {
      NamedList statPre = cache.getStatistics();
      Map<String,Object> statPre = mm.getValue();
 
       Random r = random();
       boolean changed = false;
@@ -234,14 +237,14 @@ public class TestScoreJoinQPScore extends SolrTestCaseJ4 {
               //" b=" + boost + 
               "}" + q, "fl", "id", "omitHeader", "true")
       );
      assertInsert(cache, statPre);
      assertInsert(mm.getValue(), statPre);
 
      statPre = cache.getStatistics();
      statPre = mm.getValue();
       final String repeat = h.query(req("q", "{!join from=" + from + " to=" + to + " score=" + score.toLowerCase(Locale.ROOT) +
           //" b=" + boost
               "}" + q, "fl", "id", "omitHeader", "true")
       );
      assertHit(cache, statPre);
      assertHit(mm.getValue(), statPre);
 
       assertEquals("lowercase shouldn't change anything", resp, repeat);
 
@@ -254,6 +257,7 @@ public class TestScoreJoinQPScore extends SolrTestCaseJ4 {
     // this queries are not overlap, with other in this test case. 
     // however it might be better to extract this method into the separate suite
     // for a while let's nuke a cache content, in case of repetitions
    SolrCache cache = (SolrCache)h.getCore().getInfoRegistry().get("queryResultCache");
     cache.clear();
   }
 
@@ -264,32 +268,32 @@ public class TestScoreJoinQPScore extends SolrTestCaseJ4 {
     return l.get(r.nextInt(l.size()));
   }
 
  private void assertInsert(SolrCache cache, final NamedList statPre) {
  private void assertInsert(Map<String,Object> current, final Map<String,Object> statPre) {
     assertEquals("it lookups", 1,
        delta("lookups", cache.getStatistics(), statPre));
    assertEquals("it doesn't hit", 0, delta("hits", cache.getStatistics(), statPre));
        delta("lookups", current, statPre));
    assertEquals("it doesn't hit", 0, delta("hits", current, statPre));
     assertEquals("it inserts", 1,
        delta("inserts", cache.getStatistics(), statPre));
        delta("inserts", current, statPre));
   }
 
  private void assertHit(SolrCache cache, final NamedList statPre) {
  private void assertHit(Map<String,Object> current, final Map<String,Object> statPre) {
     assertEquals("it lookups", 1,
        delta("lookups", cache.getStatistics(), statPre));
    assertEquals("it hits", 1, delta("hits", cache.getStatistics(), statPre));
        delta("lookups", current, statPre));
    assertEquals("it hits", 1, delta("hits", current, statPre));
     assertEquals("it doesn't insert", 0,
        delta("inserts", cache.getStatistics(), statPre));
        delta("inserts", current, statPre));
   }
 
  private void assertHitOrInsert(SolrCache cache, final NamedList statPre) {
  private void assertHitOrInsert(Map<String,Object> current, final Map<String,Object> statPre) {
     assertEquals("it lookups", 1,
        delta("lookups", cache.getStatistics(), statPre));
    final long mayHit = delta("hits", cache.getStatistics(), statPre);
        delta("lookups", current, statPre));
    final long mayHit = delta("hits", current, statPre);
     assertTrue("it may hit", 0 == mayHit || 1 == mayHit);
     assertEquals("or insert on cold", 1,
        delta("inserts", cache.getStatistics(), statPre) + mayHit);
        delta("inserts", current, statPre) + mayHit);
   }
 
  private long delta(String key, NamedList a, NamedList b) {
  private long delta(String key, Map<String,Object> a, Map<String,Object> b) {
     return (Long) a.get(key) - (Long) b.get(key);
   }
 
diff --git a/solr/core/src/test/org/apache/solr/store/blockcache/BufferStoreTest.java b/solr/core/src/test/org/apache/solr/store/blockcache/BufferStoreTest.java
index e91d762f5d4..534793fbc38 100644
-- a/solr/core/src/test/org/apache/solr/store/blockcache/BufferStoreTest.java
++ b/solr/core/src/test/org/apache/solr/store/blockcache/BufferStoreTest.java
@@ -17,9 +17,12 @@
 package org.apache.solr.store.blockcache;
 
 import java.math.BigDecimal;
import java.util.Map;
 
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.util.NamedList;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.metrics.MetricsMap;
import org.apache.solr.metrics.SolrMetricManager;
 import org.junit.Before;
 import org.junit.Test;
 
@@ -27,12 +30,18 @@ public class BufferStoreTest extends LuceneTestCase {
   private final static int blockSize = 1024;
 
   private Metrics metrics;
  private MetricsMap metricsMap;
 
   private Store store;
 
   @Before
   public void setup() {
     metrics = new Metrics();
    SolrMetricManager metricManager = new SolrMetricManager();
    String registry = TestUtil.randomSimpleString(random(), 2, 10);
    String scope = TestUtil.randomSimpleString(random(), 2, 10);
    metrics.initializeMetrics(metricManager, registry, scope);
    metricsMap = (MetricsMap) metricManager.registry(registry).getMetrics().get("CACHE." + scope + ".hdfsBlockCache");
     BufferStore.initNewBuffer(blockSize, blockSize, metrics);
     store = BufferStore.instance(blockSize);
   }
@@ -77,7 +86,7 @@ public class BufferStoreTest extends LuceneTestCase {
    *          whether buffers should have been lost since the last call
    */
   private void assertGaugeMetricsChanged(boolean allocated, boolean lost) {
    NamedList<Number> stats = metrics.getStatistics();
    Map<String,Object> stats = metricsMap.getValue();
 
     assertEquals("Buffer allocation metric not updating correctly.",
         allocated, isMetricPositive(stats, "buffercache.allocations"));
@@ -85,7 +94,7 @@ public class BufferStoreTest extends LuceneTestCase {
         lost, isMetricPositive(stats, "buffercache.lost"));
   }
 
  private boolean isMetricPositive(NamedList<Number> stats, String metric) {
  private boolean isMetricPositive(Map<String,Object> stats, String metric) {
     return new BigDecimal(stats.get(metric).toString()).compareTo(BigDecimal.ZERO) > 0;
   }
 
diff --git a/solr/core/src/test/org/apache/solr/util/stats/MetricUtilsTest.java b/solr/core/src/test/org/apache/solr/util/stats/MetricUtilsTest.java
index aa02de5cdf1..b852a28502a 100644
-- a/solr/core/src/test/org/apache/solr/util/stats/MetricUtilsTest.java
++ b/solr/core/src/test/org/apache/solr/util/stats/MetricUtilsTest.java
@@ -18,6 +18,7 @@
 package org.apache.solr.util.stats;
 
 import java.util.Collections;
import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.TimeUnit;
 
@@ -45,7 +46,11 @@ public class MetricUtilsTest extends SolrTestCaseJ4 {
       timer.update(Math.abs(random().nextInt()) + 1, TimeUnit.NANOSECONDS);
     }
     // obtain timer metrics
    NamedList lst = new NamedList(MetricUtils.convertTimer(timer, false));
    Map<String,Object> map = new HashMap<>();
    MetricUtils.convertTimer("", timer, false, false, (k, v) -> {
      map.putAll((Map<String,Object>)v);
    });
    NamedList lst = new NamedList(map);
     // check that expected metrics were obtained
     assertEquals(14, lst.size());
     final Snapshot snapshot = timer.getSnapshot();
@@ -84,7 +89,7 @@ public class MetricUtilsTest extends SolrTestCaseJ4 {
     Gauge<Long> error = () -> {throw new InternalError("Memory Pool not found error");};
     registry.register("memory.expected.error", error);
     MetricUtils.toMaps(registry, Collections.singletonList(MetricFilter.ALL), MetricFilter.ALL,
        false, false, false, (k, o) -> {
        false, false, false, false, (k, o) -> {
       Map v = (Map)o;
       if (k.startsWith("counter")) {
         assertEquals(1L, v.get("count"));
@@ -114,7 +119,7 @@ public class MetricUtilsTest extends SolrTestCaseJ4 {
     });
     // test compact format
     MetricUtils.toMaps(registry, Collections.singletonList(MetricFilter.ALL), MetricFilter.ALL,
        false, false, true, (k, o) -> {
        false, false, true, false, (k, o) -> {
           if (k.startsWith("counter")) {
             assertTrue(o instanceof Long);
             assertEquals(1L, o);
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
index d22b37c24be..5ebb650c05d 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/impl/CloudSolrClientTest.java
@@ -418,10 +418,10 @@ public class CloudSolrClientTest extends SolrCloudTestCase {
 
   private Long getNumRequests(String baseUrl, String collectionName) throws
       SolrServerException, IOException {
    return getNumRequests(baseUrl, collectionName, "QUERY", "standard", false);
    return getNumRequests(baseUrl, collectionName, "QUERY", "standard", null, false);
   }
 
  private Long getNumRequests(String baseUrl, String collectionName, String category, String key, boolean returnNumErrors) throws
  private Long getNumRequests(String baseUrl, String collectionName, String category, String key, String scope, boolean returnNumErrors) throws
       SolrServerException, IOException {
 
     NamedList<Object> resp;
@@ -437,7 +437,21 @@ public class CloudSolrClientTest extends SolrCloudTestCase {
       QueryRequest req = new QueryRequest(params);
       resp = client.request(req);
     }
    return (Long) resp.findRecursive("solr-mbeans", category, key, "stats", returnNumErrors ? "errors" : "requests");
    String name;
    if (returnNumErrors) {
      name = category + "." + (scope != null ? scope : key) + ".errors";
    } else {
      name = category + "." + (scope != null ? scope : key) + ".requests";
    }
    Map<String,Object> map = (Map<String,Object>)resp.findRecursive("solr-mbeans", category, key, "stats");
    if (map == null) {
      return null;
    }
    if (scope != null) { // admin handler uses a meter instead of counter here
      return (Long)map.get(name + ".count");
    } else {
      return (Long) map.get(name);
    }
   }
 
   @Test
@@ -458,7 +472,7 @@ public class CloudSolrClientTest extends SolrCloudTestCase {
         for (String adminPath : adminPathToMbean.keySet()) {
           long errorsBefore = 0;
           for (JettySolrRunner runner : cluster.getJettySolrRunners()) {
            Long numRequests = getNumRequests(runner.getBaseUrl().toString(), "foo", "ADMIN", adminPathToMbean.get(adminPath), true);
            Long numRequests = getNumRequests(runner.getBaseUrl().toString(), "foo", "ADMIN", adminPathToMbean.get(adminPath), adminPath, true);
             errorsBefore += numRequests;
             log.info("Found {} requests to {} on {}", numRequests, adminPath, runner.getBaseUrl());
           }
@@ -475,7 +489,7 @@ public class CloudSolrClientTest extends SolrCloudTestCase {
           }
           long errorsAfter = 0;
           for (JettySolrRunner runner : cluster.getJettySolrRunners()) {
            Long numRequests = getNumRequests(runner.getBaseUrl().toString(), "foo", "ADMIN", adminPathToMbean.get(adminPath), true);
            Long numRequests = getNumRequests(runner.getBaseUrl().toString(), "foo", "ADMIN", adminPathToMbean.get(adminPath), adminPath, true);
             errorsAfter += numRequests;
             log.info("Found {} requests to {} on {}", numRequests, adminPath, runner.getBaseUrl());
           }
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
index 0d4cedd4422..54ab06d59ea 100644
-- a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
@@ -33,6 +33,7 @@ import java.lang.annotation.Target;
 import java.lang.invoke.MethodHandles;
 import java.lang.reflect.Method;
 import java.net.MalformedURLException;
import java.net.ServerSocket;
 import java.net.URL;
 import java.nio.charset.Charset;
 import java.nio.charset.StandardCharsets;
@@ -802,6 +803,19 @@ public abstract class SolrTestCaseJ4 extends LuceneTestCase {
     configString = schemaString = null;
   }
 
  /**
   * Find next available local port.
   * @return available port number or -1 if none could be found
   * @throws Exception on IO errors
   */
  protected static int getNextAvailablePort() throws Exception {
    int port = -1;
    try (ServerSocket s = new ServerSocket(0)) {
      port = s.getLocalPort();
    }
    return port;
  }

 
   /** Validates an update XML String is successful
    */
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index ade1c699af0..0a06d788c98 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -598,14 +598,6 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     return proxy;
   }
 
  protected int getNextAvailablePort() throws Exception {
    int port = -1;
    try (ServerSocket s = new ServerSocket(0)) {
      port = s.getLocalPort();
    }
    return port;
  }

   private File getRelativeSolrHomePath(File solrHome) {
     final Path solrHomePath = solrHome.toPath();
     final Path curDirPath = new File("").getAbsoluteFile().toPath();
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/MiniSolrCloudCluster.java b/solr/test-framework/src/java/org/apache/solr/cloud/MiniSolrCloudCluster.java
index 15895d33b77..06052819f6b 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/MiniSolrCloudCluster.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/MiniSolrCloudCluster.java
@@ -87,6 +87,11 @@ public class MiniSolrCloudCluster {
       "    <int name=\"distribUpdateConnTimeout\">${distribUpdateConnTimeout:45000}</int>\n" +
       "    <int name=\"distribUpdateSoTimeout\">${distribUpdateSoTimeout:340000}</int>\n" +
       "  </solrcloud>\n" +
      "  <metrics>\n" +
      "    <reporter name=\"default\" class=\"org.apache.solr.metrics.reporters.SolrJmxReporter\">\n" +
      "      <str name=\"rootName\">solr_${hostPort:8983}</str>\n" +
      "    </reporter>\n" +
      "  </metrics>\n" +
       "  \n" +
       "</solr>\n";
 
diff --git a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
index 2386681422d..982f2b146da 100644
-- a/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
++ b/solr/test-framework/src/java/org/apache/solr/util/TestHarness.java
@@ -193,10 +193,10 @@ public class TestHarness extends BaseTestHarness {
                                        30000, 30000,
                                         UpdateShardHandlerConfig.DEFAULT_METRICNAMESTRATEGY);
     // universal default metric reporter
    Map<String,String> attributes = new HashMap<>();
    Map<String,Object> attributes = new HashMap<>();
     attributes.put("name", "default");
     attributes.put("class", SolrJmxReporter.class.getName());
    PluginInfo defaultPlugin = new PluginInfo("reporter", attributes, null, null);
    PluginInfo defaultPlugin = new PluginInfo("reporter", attributes);
 
     return new NodeConfig.NodeConfigBuilder("testNode", loader)
         .setUseSchemaCache(Boolean.getBoolean("shareSchema"))
diff --git a/solr/webapp/web/css/angular/plugins.css b/solr/webapp/web/css/angular/plugins.css
index 0310e0e5d54..03dc2eacf40 100644
-- a/solr/webapp/web/css/angular/plugins.css
++ b/solr/webapp/web/css/angular/plugins.css
@@ -33,6 +33,8 @@ limitations under the License.
 #content #plugins #navigation .PLUGINCHANGES { margin-top: 20px; }
 #content #plugins #navigation .PLUGINCHANGES a { background-image: url( ../../img/ico/eye.png ); }
 #content #plugins #navigation .RELOAD a { background-image: url( ../../img/ico/arrow-circle.png ); }
#content #plugins #navigation .NOTE { margin-top: 20px; }
#content #plugins #navigation .NOTE p { color: #c0c0c0; font-style: italic; }
 
 
 #content #plugins #navigation a
@@ -125,14 +127,14 @@ limitations under the License.
 #content #plugins #frame .entry .stats span
 {
   float: left;
  width: 11%;
  width: 9%;
 }
 
 #content #plugins #frame .entry dd,
 #content #plugins #frame .entry .stats ul
 {
   float: right;
  width: 88%;
  width: 90%;
 }
 
 #content #plugins #frame .entry .stats ul
@@ -144,12 +146,12 @@ limitations under the License.
 
 #content #plugins #frame .entry .stats dt
 {
  width: 27%;
  width: 40%;
 }
 
 #content #plugins #frame .entry .stats dd
 {
  width: 72%;
  width: 59%;
 }
 
 #content #plugins #frame .entry.expanded a.linker {
diff --git a/solr/webapp/web/partials/plugins.html b/solr/webapp/web/partials/plugins.html
index d95fc9b32dd..bd122a75495 100644
-- a/solr/webapp/web/partials/plugins.html
++ b/solr/webapp/web/partials/plugins.html
@@ -55,8 +55,8 @@ limitations under the License.
         </li>
         <li class="PLUGINCHANGES"><a ng-click="startRecording()">Watch Changes</a></li>
         <li class="RELOAD"><a ng-click="refresh()">Refresh Values</a></li>
        <li class="NOTE"><p>NOTE: Only selected metrics are shown here. Full metrics can be accessed via /admin/metrics handler.</p></li>
     </ul>
  
   </div>
 
   <div id="recording" ng-show="isRecording">
- 
2.19.1.windows.1

