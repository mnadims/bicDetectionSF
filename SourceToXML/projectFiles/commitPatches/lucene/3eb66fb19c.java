From 3eb66fb19ca2aa3d9dce53661f3233b6c9d3f974 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Sat, 13 Sep 2014 21:46:29 +0000
Subject: [PATCH] LUCENE-5945: Full cutover to Path api from java.io.File

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1624784 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   2 +
 .../charfilter/MappingCharFilterFactory.java  |  17 +--
 .../HyphenationCompoundWordTokenFilter.java   |  12 --
 ...e43HyphenationCompoundWordTokenFilter.java |  13 --
 .../compound/hyphenation/HyphenationTree.java |  12 --
 .../compound/hyphenation/PatternParser.java   |  12 --
 .../lucene/analysis/core/StopAnalyzer.java    |   5 +-
 .../lucene/analysis/hunspell/Dictionary.java  |  24 ++-
 .../synonym/SynonymFilterFactory.java         |  13 +-
 .../util/FilesystemResourceLoader.java        |  35 ++---
 .../analysis/util/StopwordAnalyzerBase.java   |   9 +-
 .../analysis/ckb/TestSoraniStemFilter.java    |   2 +-
 .../analysis/core/TestRandomChains.java       |  41 ++---
 .../de/TestGermanLightStemFilter.java         |   2 +-
 .../de/TestGermanMinimalStemFilter.java       |   2 +-
 .../lucene/analysis/en/TestKStemmer.java      |   2 +-
 .../analysis/en/TestPorterStemFilter.java     |   2 +-
 .../es/TestSpanishLightStemFilter.java        |   2 +-
 .../fi/TestFinnishLightStemFilter.java        |   2 +-
 .../fr/TestFrenchLightStemFilter.java         |   2 +-
 .../fr/TestFrenchMinimalStemFilter.java       |   2 +-
 .../analysis/gl/TestGalicianStemFilter.java   |   2 +-
 .../hu/TestHungarianLightStemFilter.java      |   3 +-
 .../analysis/hunspell/Test64kAffixes.java     |  22 +--
 .../hunspell/TestAllDictionaries.java         |  20 +--
 .../hunspell/TestAllDictionaries2.java        |  19 +--
 .../it/TestItalianLightStemFilter.java        |   2 +-
 .../no/TestNorwegianLightStemFilter.java      |   5 +-
 .../no/TestNorwegianMinimalStemFilter.java    |   5 +-
 .../pt/TestPortugueseLightStemFilter.java     |   2 +-
 .../pt/TestPortugueseMinimalStemFilter.java   |   2 +-
 .../analysis/pt/TestPortugueseStemFilter.java |   2 +-
 .../ru/TestRussianLightStemFilter.java        |   2 +-
 .../analysis/snowball/TestSnowballVocab.java  |   2 +-
 .../sv/TestSwedishLightStemFilter.java        |   2 +-
 .../util/TestFilesystemResourceLoader.java    |  18 +--
 .../ja/util/TokenInfoDictionaryWriter.java    |  10 +-
 .../analysis/cn/smart/AnalyzerProfile.java    |  40 ++---
 .../cn/smart/hhmm/BigramDictionary.java       |  24 ++-
 .../cn/smart/hhmm/WordDictionary.java         |  24 ++-
 .../src/java/org/egothor/stemmer/Compile.java |  14 +-
 .../src/java/org/egothor/stemmer/DiffIt.java  |   8 +-
 .../test/org/egothor/stemmer/TestCompile.java |  89 ++++-------
 .../index/TestBackwardsCompatibility.java     |  34 ++---
 .../lucene/benchmark/byTask/Benchmark.java    |  16 +-
 .../lucene/benchmark/byTask/PerfRunData.java  |  12 +-
 .../byTask/feeds/ContentItemsSource.java      |  32 ++--
 .../byTask/feeds/DirContentSource.java        |  90 ++++++-----
 .../byTask/feeds/EnwikiContentSource.java     |   7 +-
 .../byTask/feeds/FileBasedQueryMaker.java     |   9 +-
 .../benchmark/byTask/feeds/LineDocSource.java |   7 +-
 .../byTask/feeds/ReutersContentSource.java    |  35 ++---
 .../byTask/feeds/TrecContentSource.java       |  26 ++--
 .../benchmark/byTask/feeds/TrecDocParser.java |   8 +-
 .../byTask/tasks/AddIndexesTask.java          |   4 +-
 .../byTask/tasks/AnalyzerFactoryTask.java     |  10 +-
 .../byTask/tasks/CreateIndexTask.java         |   9 +-
 .../byTask/tasks/WriteEnwikiLineDocTask.java  |  12 +-
 .../byTask/tasks/WriteLineDocTask.java        |   4 +-
 .../benchmark/byTask/utils/StreamUtils.java   |  19 ++-
 .../benchmark/quality/trec/QueryDriver.java   |  15 +-
 .../quality/utils/QualityQueriesFinder.java   |   4 +-
 .../benchmark/utils/ExtractReuters.java       |  70 ++++-----
 .../benchmark/utils/ExtractWikipedia.java     |  50 +++----
 .../lucene/benchmark/BenchmarkTestCase.java   |  27 ++--
 .../benchmark/byTask/TestPerfTasksLogic.java  |  18 +--
 .../benchmark/byTask/TestPerfTasksParse.java  |  28 ++--
 .../benchmark/byTask/feeds/DocMakerTest.java  |   9 +-
 .../byTask/feeds/LineDocSourceTest.java       |  40 ++---
 .../byTask/feeds/TrecContentSourceTest.java   |   9 +-
 .../byTask/tasks/AddIndexesTaskTest.java      |   8 +-
 .../byTask/tasks/CreateIndexTaskTest.java     |   9 +-
 .../tasks/WriteEnwikiLineDocTaskTest.java     |  20 ++-
 .../byTask/tasks/WriteLineDocTaskTest.java    |  58 +++-----
 .../byTask/utils/StreamUtilsTest.java         |  37 +++--
 .../benchmark/quality/TestQualityRun.java     |   4 +-
 lucene/common-build.xml                       |  19 ++-
 lucene/core/build.xml                         |   5 +
 .../org/apache/lucene/index/CheckIndex.java   |   8 +-
 .../apache/lucene/index/DirectoryReader.java  |  21 +--
 .../apache/lucene/index/IndexFileDeleter.java |   9 +-
 .../apache/lucene/index/IndexUpgrader.java    |   8 +-
 .../org/apache/lucene/index/SegmentInfos.java |  11 +-
 .../org/apache/lucene/store/Directory.java    |   4 +-
 .../org/apache/lucene/store/FSDirectory.java  | 110 +++++---------
 .../apache/lucene/store/FSLockFactory.java    |  13 +-
 .../lucene/store/FileSwitchDirectory.java     |  15 +-
 .../apache/lucene/store/LockStressTest.java   |   5 +-
 .../apache/lucene/store/MMapDirectory.java    |  20 +--
 .../apache/lucene/store/NIOFSDirectory.java   |  10 +-
 .../lucene/store/NRTCachingDirectory.java     |  24 +--
 .../lucene/store/NativeFSLockFactory.java     |  55 +++----
 .../store/NoSuchDirectoryException.java       |  31 ----
 .../lucene/store/SimpleFSDirectory.java       |  11 +-
 .../lucene/store/SimpleFSLockFactory.java     |  56 +++----
 .../apache/lucene/util/CommandLineUtil.java   |  22 +--
 .../java/org/apache/lucene/util/IOUtils.java  | 140 ++++++++----------
 .../org/apache/lucene/util/OfflineSorter.java |  76 ++++------
 .../java/org/apache/lucene/util/fst/FST.java  |  36 +----
 .../apache/lucene/index/TestAtomicUpdate.java |   3 +-
 .../apache/lucene/index/TestCompoundFile.java |   4 +-
 .../index/TestCrashCausesCorruptIndex.java    |   4 +-
 .../lucene/index/TestDirectoryReader.java     |  20 ++-
 .../test/org/apache/lucene/index/TestDoc.java |  28 ++--
 .../apache/lucene/index/TestFieldsReader.java |   5 +-
 .../index/TestIndexWriterOnJRECrash.java      |  61 ++++----
 .../apache/lucene/index/TestNeverDelete.java  |   4 +-
 .../lucene/store/TestBufferedIndexInput.java  |  23 +--
 .../apache/lucene/store/TestDirectory.java    |  17 ++-
 .../lucene/store/TestFileSwitchDirectory.java |  19 +--
 .../apache/lucene/store/TestLockFactory.java  |  26 ++--
 .../lucene/store/TestMmapDirectory.java       |   4 +-
 .../apache/lucene/store/TestMultiMMap.java    |   7 +-
 .../lucene/store/TestNIOFSDirectory.java      |   4 +-
 .../lucene/store/TestNRTCachingDirectory.java |   6 +-
 .../apache/lucene/store/TestRAMDirectory.java |  19 ++-
 .../TestRateLimitedDirectoryWrapper.java      |   4 +-
 .../lucene/store/TestSimpleFSDirectory.java   |   4 +-
 .../apache/lucene/store/TestWindowsMMap.java  |   4 +-
 .../apache/lucene/util/TestOfflineSorter.java |  25 ++--
 .../org/apache/lucene/util/fst/TestFSTs.java  |  33 ++---
 .../TestLeaveFilesIfTestFails.java            |  22 +--
 .../org/apache/lucene/demo/IndexFiles.java    | 135 ++++++++---------
 .../org/apache/lucene/demo/SearchFiles.java   |   9 +-
 .../test/org/apache/lucene/demo/TestDemo.java |  11 +-
 .../facet/taxonomy/PrintTaxonomyStats.java    |   4 +-
 .../directory/DirectoryTaxonomyWriter.java    |  15 +-
 .../writercache/CompactLabelToOrdinal.java    |  16 +-
 .../writercache/TestCharBlockArray.java       |  13 +-
 .../TestCompactLabelToOrdinal.java            |   9 +-
 lucene/misc/build.xml                         |   5 +
 .../lucene/index/CompoundFileExtractor.java   |  18 ++-
 .../apache/lucene/index/IndexSplitter.java    |  43 ++----
 .../lucene/index/MultiPassIndexSplitter.java  |  18 +--
 .../org/apache/lucene/misc/GetTermInfo.java   |   4 +-
 .../org/apache/lucene/misc/HighFreqTerms.java |   4 +-
 .../apache/lucene/misc/IndexMergeTool.java    |   6 +-
 .../lucene/store/NativeUnixDirectory.java     |  16 +-
 .../apache/lucene/store/WindowsDirectory.java |  14 +-
 .../lucene/index/TestIndexSplitter.java       |  14 +-
 .../PerSessionDirectoryFactory.java           |  21 ++-
 ...IndexAndTaxonomyReplicationClientTest.java |   4 +-
 .../replicator/http/HttpReplicatorTest.java   |   4 +-
 .../search/spell/PlainTextDictionary.java     |   9 +-
 .../search/suggest/SortedInputIterator.java   |  13 +-
 .../analyzing/AnalyzingInfixSuggester.java    |   4 +-
 .../suggest/analyzing/AnalyzingSuggester.java |  10 +-
 .../suggest/analyzing/FreeTextSuggester.java  |   3 +-
 .../search/suggest/fst/ExternalRefSorter.java |  14 +-
 .../suggest/fst/FSTCompletionLookup.java      |  14 +-
 .../jaspell/JaspellTernarySearchTrie.java     |  17 +--
 .../search/suggest/PersistenceTest.java       |  11 +-
 .../AnalyzingInfixSuggesterTest.java          |  12 +-
 .../analyzing/AnalyzingSuggesterTest.java     |  32 ++--
 .../analyzing/BlendedInfixSuggesterTest.java  |  10 +-
 .../analyzing/TestFreeTextSuggester.java      |  14 +-
 .../search/suggest/fst/LargeInputFST.java     |  17 +--
 .../analysis/BaseTokenStreamTestCase.java     |   7 +-
 .../lucene/analysis/VocabularyAssert.java     |  12 +-
 .../index/BasePostingsFormatTestCase.java     |   5 +-
 .../ThreadedIndexingAndSearchingTestCase.java |   3 +-
 .../lucene/store/BaseDirectoryTestCase.java   |  22 +--
 .../org/apache/lucene/util/LineFileDocs.java  |  18 +--
 .../apache/lucene/util/LuceneTestCase.java    |  49 +++---
 .../apache/lucene/util/RemoveUponClose.java   |  15 +-
 .../util/TestRuleTemporaryFilesCleanup.java   |  86 ++++++-----
 .../java/org/apache/lucene/util/TestUtil.java |  29 ++--
 .../org/apache/lucene/util/fst/FSTTester.java |   6 +-
 lucene/tools/forbiddenApis/lucene.txt         |  20 +++
 .../TestFoldingMultitermExtrasQuery.java      |   2 +-
 .../solr/schema/TestICUCollationField.java    |   2 +-
 .../TestICUCollationFieldDocValues.java       |   2 +-
 .../schema/TestICUCollationFieldOptions.java  |   2 +-
 .../AbstractClusteringTestCase.java           |   2 +-
 .../AbstractDataImportHandlerTestCase.java    |   6 +-
 .../AbstractSqlEntityProcessorTestCase.java   |   2 +-
 .../TestContentStreamDataSource.java          |   2 +-
 .../handler/dataimport/TestDocBuilder2.java   |   2 +-
 .../TestFileListEntityProcessor.java          |   8 +-
 .../TestFileListWithLineEntityProcessor.java  |   2 +-
 .../TestNonWritablePersistFile.java           |   2 +-
 .../TestSimplePropertiesWriter.java           |   2 +-
 .../TestSolrEntityProcessorEndToEnd.java      |   4 +-
 .../dataimport/TestXPathEntityProcessor.java  |   4 +-
 .../dataimport/TestZKPropertiesWriter.java    |   2 +-
 .../org/apache/solr/hadoop/MRUnitBase.java    |   2 +-
 ...apReduceIndexerToolArgumentParserTest.java |   2 +-
 .../solr/hadoop/MorphlineBasicMiniMRTest.java |   4 +-
 .../hadoop/MorphlineGoLiveMiniMRTest.java     |   4 +-
 .../solr/AbstractSolrMorphlineTestBase.java   |   2 +-
 .../solr/AbstractSolrMorphlineZkTestBase.java |   2 +-
 .../solr/core/CachingDirectoryFactory.java    |   4 +-
 .../solr/core/MMapDirectoryFactory.java       |   5 +-
 .../solr/core/NIOFSDirectoryFactory.java      |   2 +-
 .../solr/core/NRTCachingDirectoryFactory.java |   2 +-
 .../solr/core/SimpleFSDirectoryFactory.java   |   2 +-
 .../apache/solr/core/SolrDeletionPolicy.java  |   2 +-
 .../solr/core/StandardDirectoryFactory.java   |   6 +-
 .../org/apache/solr/handler/SnapPuller.java   |   2 +-
 .../org/apache/solr/handler/SnapShooter.java  |   8 +-
 .../spelling/AbstractLuceneSpellChecker.java  |   2 +-
 .../solr/spelling/IndexBasedSpellChecker.java |   2 +-
 .../fst/AnalyzingInfixLookupFactory.java      |   2 +-
 .../fst/BlendedInfixLookupFactory.java        |   2 +-
 .../solr/store/blockcache/BlockDirectory.java |   2 +-
 .../solr/AnalysisAfterCoreReloadTest.java     |   2 +-
 .../org/apache/solr/SolrTestCaseJ4Test.java   |   2 +-
 .../apache/solr/TestSolrCoreProperties.java   |   2 +-
 .../org/apache/solr/TestTolerantSearch.java   |   2 +-
 .../solr/cloud/BasicDistributedZk2Test.java   |   6 +-
 .../solr/cloud/BasicDistributedZkTest.java    |   8 +-
 .../solr/cloud/ClusterStateUpdateTest.java    |   4 +-
 .../CollectionsAPIDistributedZkTest.java      |   8 +-
 .../solr/cloud/ConnectionManagerTest.java     |   4 +-
 .../cloud/LeaderElectionIntegrationTest.java  |   4 +-
 .../apache/solr/cloud/LeaderElectionTest.java |   2 +-
 .../org/apache/solr/cloud/OverseerTest.java   |  18 +--
 .../SharedFSAutoReplicaFailoverTest.java      |   2 +-
 .../apache/solr/cloud/SolrXmlInZkTest.java    |   2 +-
 .../cloud/TestLeaderElectionZkExpiry.java     |   4 +-
 .../solr/cloud/TestMiniSolrCloudCluster.java  |   2 +-
 .../cloud/TestMultiCoreConfBootstrap.java     |   4 +-
 .../org/apache/solr/cloud/TestZkChroot.java   |   2 +-
 .../solr/cloud/UnloadDistributedZkTest.java   |   6 +-
 .../test/org/apache/solr/cloud/ZkCLITest.java |   8 +-
 .../apache/solr/cloud/ZkControllerTest.java   |   8 +-
 .../apache/solr/cloud/ZkSolrClientTest.java   |   6 +-
 .../hdfs/HdfsBasicDistributedZk2Test.java     |   2 +-
 .../hdfs/HdfsBasicDistributedZkTest.java      |   2 +-
 .../hdfs/HdfsChaosMonkeySafeLeaderTest.java   |   2 +-
 .../HdfsCollectionsAPIDistributedZkTest.java  |   2 +-
 .../solr/cloud/hdfs/HdfsRecoveryZkTest.java   |   2 +-
 .../solr/cloud/hdfs/HdfsSyncSliceTest.java    |   2 +-
 .../hdfs/HdfsUnloadDistributedZkTest.java     |   2 +-
 .../HdfsWriteToMultipleCollectionsTest.java   |   2 +-
 .../solr/cloud/hdfs/StressHdfsTest.java       |   2 +-
 .../solr/core/AlternateDirectoryTest.java     |   2 +-
 .../CoreContainerCoreInitFailuresTest.java    |   2 +-
 .../solr/core/OpenCloseCoreStressTest.java    |   2 +-
 .../apache/solr/core/ResourceLoaderTest.java  |   6 +-
 .../core/SolrCoreCheckLockOnStartupTest.java  |   4 +-
 .../org/apache/solr/core/SolrCoreTest.java    |   2 +-
 .../solr/core/TestArbitraryIndexDir.java      |   4 +-
 .../org/apache/solr/core/TestConfigSets.java  |   2 +-
 .../apache/solr/core/TestCoreContainer.java   |  10 +-
 .../apache/solr/core/TestCoreDiscovery.java   |   6 +-
 .../org/apache/solr/core/TestLazyCores.java   |   4 +-
 .../org/apache/solr/core/TestSolrXml.java     |   2 +-
 .../solr/core/TestSolrXmlPersistence.java     |   4 +-
 .../solr/core/TestSolrXmlPersistor.java       |   2 +-
 .../apache/solr/handler/TestCSVLoader.java    |   2 +-
 .../solr/handler/TestReplicationHandler.java  |  10 +-
 .../handler/TestReplicationHandlerBackup.java |  11 +-
 .../admin/CoreAdminCreateDiscoverTest.java    |   2 +-
 .../handler/admin/CoreAdminHandlerTest.java   |   4 +-
 .../admin/CoreAdminRequestStatusTest.java     |   2 +-
 .../CoreMergeIndexesAdminHandlerTest.java     |   2 +-
 .../DistributedDebugComponentTest.java        |   2 +-
 .../solr/request/TestRemoteStreaming.java     |   2 +-
 .../solr/rest/TestManagedResourceStorage.java |   2 +-
 .../org/apache/solr/rest/TestRestManager.java |   2 +-
 ...TestManagedSchemaDynamicFieldResource.java |   2 +-
 .../TestManagedSchemaFieldResource.java       |   2 +-
 .../TestManagedSchemaFieldTypeResource.java   |   2 +-
 .../TestManagedStopFilterFactory.java         |   2 +-
 .../TestManagedSynonymFilterFactory.java      |   2 +-
 .../solr/schema/ChangedSchemaMergeTest.java   |   2 +-
 .../solr/schema/ModifyConfFileTest.java       |   4 +-
 .../apache/solr/schema/TestBinaryField.java   |   2 +-
 .../solr/schema/TestCollationField.java       |   2 +-
 .../schema/TestCollationFieldDocValues.java   |   2 +-
 .../apache/solr/schema/TestManagedSchema.java |   2 +-
 .../solr/search/TestAddFieldRealTimeGet.java  |   2 +-
 .../apache/solr/search/TestRecoveryHdfs.java  |   2 +-
 .../apache/solr/search/TestSearcherReuse.java |   2 +-
 .../apache/solr/servlet/CacheHeaderTest.java  |   2 +-
 .../solr/servlet/ResponseHeaderTest.java      |   2 +-
 .../spelling/FileBasedSpellCheckerTest.java   |   4 +-
 .../spelling/IndexBasedSpellCheckerTest.java  |  10 +-
 .../store/blockcache/BlockDirectoryTest.java  |  10 +-
 .../solr/store/hdfs/HdfsDirectoryTest.java    |   4 +-
 .../solr/store/hdfs/HdfsLockFactoryTest.java  |   2 +-
 .../solr/update/SolrIndexSplitterTest.java    |   8 +-
 ...chemaFieldsUpdateProcessorFactoryTest.java |   2 +-
 .../solrj/MergeIndexesExampleTestBase.java    |   4 +-
 .../solrj/MultiCoreExampleTestBase.java       |   4 +-
 .../solrj/SolrSchemalessExampleTest.java      |   2 +-
 .../client/solrj/TestLBHttpSolrServer.java    |   4 +-
 .../AbstractEmbeddedSolrServerTestCase.java   |   2 +-
 .../solrj/embedded/JettyWebappTest.java       |   2 +-
 .../client/solrj/request/SolrPingTest.java    |   2 +-
 .../client/solrj/request/TestCoreAdmin.java   |   6 +-
 .../solr/common/util/ContentStreamTest.java   |   4 +-
 .../solr/BaseDistributedSearchTestCase.java   |   2 +-
 .../org/apache/solr/SolrJettyTestBase.java    |   2 +-
 .../java/org/apache/solr/SolrTestCaseJ4.java  |   7 +-
 .../cloud/AbstractFullDistribZkTestBase.java  |   6 +-
 .../apache/solr/cloud/AbstractZkTestCase.java |   2 +-
 .../solr/core/MockFSDirectoryFactory.java     |   5 +-
 299 files changed, 1659 insertions(+), 1951 deletions(-)
 delete mode 100644 lucene/core/src/java/org/apache/lucene/store/NoSuchDirectoryException.java
 create mode 100644 lucene/tools/forbiddenApis/lucene.txt

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 44f38b4f0ea..fb754189dbe 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -7,6 +7,8 @@ http://s.apache.org/luceneversions
 
 New Features
 
* LUCENE-5945: All file handling converted to NIO.2 apis. (Robert Muir)

 * SOLR-3359: Added analyzer attribute/property to SynonymFilterFactory.
   (Ryo Onodera via Koji Sekiguchi)
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/charfilter/MappingCharFilterFactory.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/charfilter/MappingCharFilterFactory.java
index 29c115270db..f3948d6681b 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/charfilter/MappingCharFilterFactory.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/charfilter/MappingCharFilterFactory.java
@@ -17,7 +17,6 @@ package org.apache.lucene.analysis.charfilter;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
 import java.util.ArrayList;
@@ -59,21 +58,15 @@ public class MappingCharFilterFactory extends CharFilterFactory implements
     }
   }
 
  // TODO: this should use inputstreams from the loader, not File!
   @Override
   public void inform(ResourceLoader loader) throws IOException {
     if (mapping != null) {
       List<String> wlist = null;
      File mappingFile = new File(mapping);
      if (mappingFile.exists()) {
        wlist = getLines(loader, mapping);
      } else {
        List<String> files = splitFileNames(mapping);
        wlist = new ArrayList<>();
        for (String file : files) {
          List<String> lines = getLines(loader, file.trim());
          wlist.addAll(lines);
        }
      List<String> files = splitFileNames(mapping);
      wlist = new ArrayList<>();
      for (String file : files) {
        List<String> lines = getLines(loader, file.trim());
        wlist.addAll(lines);
       }
       final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
       parseRules(wlist, builder);
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/HyphenationCompoundWordTokenFilter.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/HyphenationCompoundWordTokenFilter.java
index 674bd813b26..1ad61daff2e 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/HyphenationCompoundWordTokenFilter.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/HyphenationCompoundWordTokenFilter.java
@@ -119,18 +119,6 @@ public class HyphenationCompoundWordTokenFilter extends
     return getHyphenationTree(new InputSource(hyphenationFilename));
   }
 
  /**
   * Create a hyphenator tree
   *
   * @param hyphenationFile the file of the XML grammar to load
   * @return An object representing the hyphenation patterns
   * @throws java.io.IOException If there is a low-level I/O error.
   */
  public static HyphenationTree getHyphenationTree(File hyphenationFile)
      throws IOException {
    return getHyphenationTree(new InputSource(hyphenationFile.toURI().toASCIIString()));
  }

   /**
    * Create a hyphenator tree
    *
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/Lucene43HyphenationCompoundWordTokenFilter.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/Lucene43HyphenationCompoundWordTokenFilter.java
index 3cc97ba8a2d..050dcf89d93 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/Lucene43HyphenationCompoundWordTokenFilter.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/Lucene43HyphenationCompoundWordTokenFilter.java
@@ -17,7 +17,6 @@ package org.apache.lucene.analysis.compound;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 
 import org.apache.lucene.analysis.TokenFilter;
@@ -120,18 +119,6 @@ public class Lucene43HyphenationCompoundWordTokenFilter extends
     return getHyphenationTree(new InputSource(hyphenationFilename));
   }
 
  /**
   * Create a hyphenator tree
   * 
   * @param hyphenationFile the file of the XML grammar to load
   * @return An object representing the hyphenation patterns
   * @throws IOException If there is a low-level I/O error.
   */
  public static HyphenationTree getHyphenationTree(File hyphenationFile)
      throws IOException {
    return getHyphenationTree(new InputSource(hyphenationFile.toURI().toASCIIString()));
  }

   /**
    * Create a hyphenator tree
    * 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/HyphenationTree.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/HyphenationTree.java
index 4ba5f27ae50..62f6426d2a3 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/HyphenationTree.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/HyphenationTree.java
@@ -17,7 +17,6 @@
 
 package org.apache.lucene.analysis.compound.hyphenation;
 
import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.util.ArrayList;
@@ -104,17 +103,6 @@ public class HyphenationTree extends TernaryTree implements PatternConsumer {
     return buf.toString();
   }
 
  /**
   * Read hyphenation patterns from an XML file.
   * 
   * @param f the filename
   * @throws IOException In case the parsing fails
   */
  public void loadPatterns(File f) throws IOException {
    InputSource src = new InputSource(f.toURI().toASCIIString());
    loadPatterns(src);
  }

   /**
    * Read hyphenation patterns from an XML file.
    * 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/PatternParser.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/PatternParser.java
index d9901f10cc0..028bceeff80 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/PatternParser.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/compound/hyphenation/PatternParser.java
@@ -26,7 +26,6 @@ import org.xml.sax.helpers.DefaultHandler;
 import org.xml.sax.Attributes;
 
 // Java
import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 
@@ -91,17 +90,6 @@ public class PatternParser extends DefaultHandler {
     parse(new InputSource(filename));
   }
 
  /**
   * Parses a hyphenation pattern file.
   * 
   * @param file the pattern file
   * @throws IOException In case of an exception while parsing
   */
  public void parse(File file) throws IOException {
    InputSource src = new InputSource(file.toURI().toASCIIString());
    parse(src);
  }

   /**
    * Parses a hyphenation pattern file.
    * 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
index 102618f84be..e25774e8951 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/core/StopAnalyzer.java
@@ -20,6 +20,7 @@ package org.apache.lucene.analysis.core;
 import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.List;
 
@@ -62,10 +63,10 @@ public final class StopAnalyzer extends StopwordAnalyzerBase {
     super(stopWords);
   }
 
  /** Builds an analyzer with the stop words from the given file.
  /** Builds an analyzer with the stop words from the given path.
    * @see WordlistLoader#getWordSet(Reader)
    * @param stopwordsFile File to load stop words from */
  public StopAnalyzer(File stopwordsFile) throws IOException {
  public StopAnalyzer(Path stopwordsFile) throws IOException {
     this(loadStopwordSet(stopwordsFile));
   }
 
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hunspell/Dictionary.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hunspell/Dictionary.java
index 5aa28be7937..480382d0010 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/hunspell/Dictionary.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/hunspell/Dictionary.java
@@ -42,9 +42,6 @@ import org.apache.lucene.util.fst.Util;
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
@@ -55,6 +52,7 @@ import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CodingErrorAction;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.text.ParseException;
 import java.util.ArrayList;
 import java.util.Arrays;
@@ -141,7 +139,7 @@ public class Dictionary {
   // when set, some words have exceptional stems, and the last entry is a pointer to stemExceptions
   boolean hasStemExceptions;
   
  private final File tempDir = OfflineSorter.defaultTempDir(); // TODO: make this configurable?
  private final Path tempDir = OfflineSorter.defaultTempDir(); // TODO: make this configurable?
   
   boolean ignoreCase;
   boolean complexPrefixes;
@@ -200,8 +198,8 @@ public class Dictionary {
     this.needsOutputCleaning = false; // set if we have an OCONV
     flagLookup.add(new BytesRef()); // no flags -> ord 0
 
    File aff = File.createTempFile("affix", "aff", tempDir);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(aff));
    Path aff = Files.createTempFile(tempDir, "affix", "aff");
    OutputStream out = new BufferedOutputStream(Files.newOutputStream(aff));
     InputStream aff1 = null;
     InputStream aff2 = null;
     boolean success = false;
@@ -215,12 +213,12 @@ public class Dictionary {
       out.close();
       
       // pass 1: get encoding
      aff1 = new BufferedInputStream(new FileInputStream(aff));
      aff1 = new BufferedInputStream(Files.newInputStream(aff));
       String encoding = getDictionaryEncoding(aff1);
       
       // pass 2: parse affixes
       CharsetDecoder decoder = getJavaEncoding(encoding);
      aff2 = new BufferedInputStream(new FileInputStream(aff));
      aff2 = new BufferedInputStream(Files.newInputStream(aff));
       readAffixFile(aff2, decoder);
       
       // read dictionary entries
@@ -234,7 +232,7 @@ public class Dictionary {
     } finally {
       IOUtils.closeWhileHandlingException(out, aff1, aff2);
       if (success) {
        Files.delete(aff.toPath());
        Files.delete(aff);
       } else {
         IOUtils.deleteFilesIgnoringExceptions(aff);
       }
@@ -782,7 +780,7 @@ public class Dictionary {
     
     StringBuilder sb = new StringBuilder();
     
    File unsorted = File.createTempFile("unsorted", "dat", tempDir);
    Path unsorted = Files.createTempFile(tempDir, "unsorted", "dat");
     try (ByteSequencesWriter writer = new ByteSequencesWriter(unsorted)) {
       for (InputStream dictionary : dictionaries) {
         BufferedReader lines = new BufferedReader(new InputStreamReader(dictionary, decoder));
@@ -825,7 +823,7 @@ public class Dictionary {
         }
       }
     }
    File sorted = File.createTempFile("sorted", "dat", tempDir);
    Path sorted = Files.createTempFile(tempDir, "sorted", "dat");
     
     OfflineSorter sorter = new OfflineSorter(new Comparator<BytesRef>() {
       BytesRef scratch1 = new BytesRef();
@@ -870,7 +868,7 @@ public class Dictionary {
       success = true;
     } finally {
       if (success) {
        Files.delete(unsorted.toPath());
        Files.delete(unsorted);
       } else {
         IOUtils.deleteFilesIgnoringExceptions(unsorted);
       }
@@ -960,7 +958,7 @@ public class Dictionary {
     } finally {
       IOUtils.closeWhileHandlingException(reader);
       if (success2) {
        Files.delete(sorted.toPath());
        Files.delete(sorted);
       } else {
         IOUtils.deleteFilesIgnoringExceptions(sorted);
       }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymFilterFactory.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymFilterFactory.java
index 45bd3529015..c5afd87bf45 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymFilterFactory.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymFilterFactory.java
@@ -17,7 +17,6 @@ package org.apache.lucene.analysis.synonym;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.Reader;
@@ -171,16 +170,10 @@ public class SynonymFilterFactory extends TokenFilterFactory implements Resource
       throw new RuntimeException(e);
     }
 
    File synonymFile = new File(synonyms);
    if (synonymFile.exists()) {
    List<String> files = splitFileNames(synonyms);
    for (String file : files) {
       decoder.reset();
      parser.parse(new InputStreamReader(loader.openResource(synonyms), decoder));
    } else {
      List<String> files = splitFileNames(synonyms);
      for (String file : files) {
        decoder.reset();
        parser.parse(new InputStreamReader(loader.openResource(file), decoder));
      }
      parser.parse(new InputStreamReader(loader.openResource(file), decoder));
     }
     return parser.build();
   }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/FilesystemResourceLoader.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/FilesystemResourceLoader.java
index cd907716154..de7cfa0b1d7 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/FilesystemResourceLoader.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/FilesystemResourceLoader.java
@@ -17,11 +17,12 @@ package org.apache.lucene.analysis.util;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
 
 /**
  * Simple {@link ResourceLoader} that opens resource files
@@ -37,25 +38,16 @@ import java.io.InputStream;
  * to allow lookup of files in more than one base directory.
  */
 public final class FilesystemResourceLoader implements ResourceLoader {
  private final File baseDirectory;
  private final Path baseDirectory;
   private final ResourceLoader delegate;
   
  /**
   * Creates a resource loader that requires absolute filenames or relative to CWD
   * to resolve resources. Files not found in file system and class lookups
   * are delegated to context classloader.
   */
  public FilesystemResourceLoader() {
    this((File) null);
  }

   /**
    * Creates a resource loader that resolves resources against the given
    * base directory (may be {@code null} to refer to CWD).
    * Files not found in file system and class lookups are delegated to context
    * classloader.
    */
  public FilesystemResourceLoader(File baseDirectory) {
  public FilesystemResourceLoader(Path baseDirectory) {
     this(baseDirectory, new ClasspathResourceLoader());
   }
 
@@ -65,9 +57,12 @@ public final class FilesystemResourceLoader implements ResourceLoader {
    * Files not found in file system and class lookups are delegated
    * to the given delegate {@link ResourceLoader}.
    */
  public FilesystemResourceLoader(File baseDirectory, ResourceLoader delegate) {
    if (baseDirectory != null && !baseDirectory.isDirectory())
      throw new IllegalArgumentException("baseDirectory is not a directory or null");
  public FilesystemResourceLoader(Path baseDirectory, ResourceLoader delegate) {
    if (baseDirectory == null) {
      throw new NullPointerException();
    }
    if (!Files.isDirectory(baseDirectory))
      throw new IllegalArgumentException(baseDirectory + " is not a directory");
     if (delegate == null)
       throw new IllegalArgumentException("delegate ResourceLoader may not be null");
     this.baseDirectory = baseDirectory;
@@ -77,12 +72,8 @@ public final class FilesystemResourceLoader implements ResourceLoader {
   @Override
   public InputStream openResource(String resource) throws IOException {
     try {
      File file = new File (resource);
      if (baseDirectory != null && !file.isAbsolute()) {
        file = new File(baseDirectory, resource);
      }
      return new FileInputStream(file);
    } catch (FileNotFoundException fnfe) {
      return Files.newInputStream(baseDirectory.resolve(resource));
    } catch (FileNotFoundException | NoSuchFileException fnfe) {
       return delegate.openResource(resource);
     }
   }
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
index ff1517e90d0..ad7332bcf61 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/util/StopwordAnalyzerBase.java
@@ -17,10 +17,11 @@
 
 package org.apache.lucene.analysis.util;
 
import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.util.IOUtils;
@@ -98,7 +99,7 @@ public abstract class StopwordAnalyzerBase extends Analyzer {
   }
   
   /**
   * Creates a CharArraySet from a file.
   * Creates a CharArraySet from a path.
    * 
    * @param stopwords
    *          the stopwords file to load
@@ -107,10 +108,10 @@ public abstract class StopwordAnalyzerBase extends Analyzer {
    * @throws IOException
    *           if loading the stopwords throws an {@link IOException}
    */
  protected static CharArraySet loadStopwordSet(File stopwords) throws IOException {
  protected static CharArraySet loadStopwordSet(Path stopwords) throws IOException {
     Reader reader = null;
     try {
      reader = IOUtils.getDecodingReader(stopwords, StandardCharsets.UTF_8);
      reader = Files.newBufferedReader(stopwords, StandardCharsets.UTF_8);
       return WordlistLoader.getWordSet(reader);
     } finally {
       IOUtils.close(reader);
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/ckb/TestSoraniStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/ckb/TestSoraniStemFilter.java
index ac2543d7bd5..bf98fa659b7 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/ckb/TestSoraniStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/ckb/TestSoraniStemFilter.java
@@ -95,6 +95,6 @@ public class TestSoraniStemFilter extends BaseTokenStreamTestCase {
   /** test against a basic vocabulary file */
   public void testVocabulary() throws Exception {
     // top 8k words or so: freq > 1000
    assertVocabulary(a, getDataFile("ckbtestdata.zip"), "testdata.txt");
    assertVocabulary(a, getDataPath("ckbtestdata.zip"), "testdata.txt");
   }
 }
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
index d178fb6f03a..2216212ca1c 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/core/TestRandomChains.java
@@ -17,7 +17,6 @@ package org.apache.lucene.analysis.core;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Reader;
@@ -28,6 +27,10 @@ import java.lang.reflect.Modifier;
 import java.net.URI;
 import java.net.URL;
 import java.nio.CharBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
@@ -271,23 +274,25 @@ public class TestRandomChains extends BaseTokenStreamTestCase {
       final URI uri = resources.nextElement().toURI();
       if (!"file".equalsIgnoreCase(uri.getScheme()))
         continue;
      final File directory = new File(uri);
      if (directory.exists()) {
        String[] files = directory.list();
        for (String file : files) {
          if (new File(directory, file).isDirectory()) {
            // recurse
            String subPackage = pckgname + "." + file;
            collectClassesForPackage(subPackage, classes);
          }
          if (file.endsWith(".class")) {
            String clazzName = file.substring(0, file.length() - 6);
            // exclude Test classes that happen to be in these packages.
            // class.ForName'ing some of them can cause trouble.
            if (!clazzName.endsWith("Test") && !clazzName.startsWith("Test")) {
              // Don't run static initializers, as we won't use most of them.
              // Java will do that automatically once accessed/instantiated.
              classes.add(Class.forName(pckgname + '.' + clazzName, false, cld));
      final Path directory = Paths.get(uri);
      if (Files.exists(directory)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
          for (Path file : stream) {
            if (Files.isDirectory(file)) {
              // recurse
              String subPackage = pckgname + "." + file.getFileName().toString();
              collectClassesForPackage(subPackage, classes);
            }
            String fname = file.getFileName().toString();
            if (fname.endsWith(".class")) {
              String clazzName = fname.substring(0, fname.length() - 6);
              // exclude Test classes that happen to be in these packages.
              // class.ForName'ing some of them can cause trouble.
              if (!clazzName.endsWith("Test") && !clazzName.startsWith("Test")) {
                // Don't run static initializers, as we won't use most of them.
                // Java will do that automatically once accessed/instantiated.
                classes.add(Class.forName(pckgname + '.' + clazzName, false, cld));
              }
             }
           }
         }
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
index b9a6bd31098..5aac3b34ded 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanLightStemFilter.java
@@ -45,7 +45,7 @@ public class TestGermanLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("delighttestdata.zip"), "delight.txt");
    assertVocabulary(analyzer, getDataPath("delighttestdata.zip"), "delight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
index a8b7e7b1e91..dd0f3200489 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/de/TestGermanMinimalStemFilter.java
@@ -70,7 +70,7 @@ public class TestGermanMinimalStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("deminimaltestdata.zip"), "deminimal.txt");
    assertVocabulary(analyzer, getDataPath("deminimaltestdata.zip"), "deminimal.txt");
   }
   
   /** blast some random strings through the analyzer */
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
index 0b07d3c9543..755354fc8c0 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestKStemmer.java
@@ -51,7 +51,7 @@ public class TestKStemmer extends BaseTokenStreamTestCase {
    * testCreateMap, commented out below).
    */
   public void testVocabulary() throws Exception {
    assertVocabulary(a, getDataFile("kstemTestData.zip"), "kstem_examples.txt");
    assertVocabulary(a, getDataPath("kstemTestData.zip"), "kstem_examples.txt");
   }
   
   public void testEmptyTerm() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
index 36fbf4f5dfc..db7fc328c4f 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/en/TestPorterStemFilter.java
@@ -49,7 +49,7 @@ public class TestPorterStemFilter extends BaseTokenStreamTestCase {
    * The output should be the same as the string in output.txt
    */
   public void testPorterStemFilter() throws Exception {
    assertVocabulary(a, getDataFile("porterTestData.zip"), "voc.txt", "output.txt");
    assertVocabulary(a, getDataPath("porterTestData.zip"), "voc.txt", "output.txt");
   }
   
   public void testWithKeywordAttribute() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
index 636e82ae733..81a3b90be41 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/es/TestSpanishLightStemFilter.java
@@ -42,7 +42,7 @@ public class TestSpanishLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("eslighttestdata.zip"), "eslight.txt");
    assertVocabulary(analyzer, getDataPath("eslighttestdata.zip"), "eslight.txt");
   }
   
   /** blast some random strings through the analyzer */
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
index 985ec1abc5c..f183f5bb721 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fi/TestFinnishLightStemFilter.java
@@ -44,7 +44,7 @@ public class TestFinnishLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("filighttestdata.zip"), "filight.txt");
    assertVocabulary(analyzer, getDataPath("filighttestdata.zip"), "filight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
index ce0a038fa82..743962b88a1 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchLightStemFilter.java
@@ -175,7 +175,7 @@ public class TestFrenchLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("frlighttestdata.zip"), "frlight.txt");
    assertVocabulary(analyzer, getDataPath("frlighttestdata.zip"), "frlight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
index 5330e0ed67c..7f44a7f710e 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/fr/TestFrenchMinimalStemFilter.java
@@ -72,7 +72,7 @@ public class TestFrenchMinimalStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("frminimaltestdata.zip"), "frminimal.txt");
    assertVocabulary(analyzer, getDataPath("frminimaltestdata.zip"), "frminimal.txt");
   }
   
   /** blast some random strings through the analyzer */
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
index 7d2f6196d47..6b0c9447cba 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/gl/TestGalicianStemFilter.java
@@ -46,7 +46,7 @@ public class TestGalicianStemFilter extends BaseTokenStreamTestCase {
  
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("gltestdata.zip"), "gl.txt");
    assertVocabulary(analyzer, getDataPath("gltestdata.zip"), "gl.txt");
   }
   
   public void testEmptyTerm() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
index 46478894113..b8740f9c4ee 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hu/TestHungarianLightStemFilter.java
@@ -18,7 +18,6 @@ package org.apache.lucene.analysis.hu;
  */
 
 import java.io.IOException;
import java.io.Reader;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.BaseTokenStreamTestCase;
@@ -45,7 +44,7 @@ public class TestHungarianLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("hulighttestdata.zip"), "hulight.txt");
    assertVocabulary(analyzer, getDataPath("hulighttestdata.zip"), "hulight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/Test64kAffixes.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/Test64kAffixes.java
index 30ce732cf00..f585f9f135f 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/Test64kAffixes.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/Test64kAffixes.java
@@ -18,12 +18,10 @@ package org.apache.lucene.analysis.hunspell;
  */
 
 import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.InputStream;
import java.io.OutputStreamWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.List;
 
 import org.apache.lucene.util.CharsRef;
@@ -33,13 +31,11 @@ import org.apache.lucene.util.LuceneTestCase;
 public class Test64kAffixes extends LuceneTestCase {
   
   public void test() throws Exception {
    File tempDir = createTempDir("64kaffixes");
    File affix = new File(tempDir, "64kaffixes.aff");
    File dict = new File(tempDir, "64kaffixes.dic");
    Path tempDir = createTempDir("64kaffixes");
    Path affix = tempDir.resolve("64kaffixes.aff");
    Path dict = tempDir.resolve("64kaffixes.dic");
     
    BufferedWriter affixWriter = new BufferedWriter(
                                 new OutputStreamWriter(
                                 new FileOutputStream(affix), StandardCharsets.UTF_8));
    BufferedWriter affixWriter = Files.newBufferedWriter(affix, StandardCharsets.UTF_8);
     
     // 65k affixes with flag 1, then an affix with flag 2
     affixWriter.write("SET UTF-8\nFLAG num\nSFX 1 Y 65536\n");
@@ -49,15 +45,13 @@ public class Test64kAffixes extends LuceneTestCase {
     affixWriter.write("SFX 2 Y 1\nSFX 2 0 s\n");
     affixWriter.close();
     
    BufferedWriter dictWriter = new BufferedWriter(
                                new OutputStreamWriter(
                                new FileOutputStream(dict), StandardCharsets.UTF_8));
    BufferedWriter dictWriter = Files.newBufferedWriter(dict, StandardCharsets.UTF_8);
     
     // drink signed with affix 2 (takes -s)
     dictWriter.write("1\ndrink/2\n");
     dictWriter.close();
     
    try (InputStream affStream = new FileInputStream(affix); InputStream dictStream = new FileInputStream(dict)) {
    try (InputStream affStream = Files.newInputStream(affix); InputStream dictStream = Files.newInputStream(dict)) {
       Dictionary dictionary = new Dictionary(affStream, dictStream);
       Stemmer stemmer = new Stemmer(dictionary);
       // drinks should still stem to drink
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries.java
index fa007e3787b..288e3249c58 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries.java
@@ -17,9 +17,11 @@ package org.apache.lucene.analysis.hunspell;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.InputStream;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipFile;
 
@@ -39,8 +41,8 @@ import org.junit.Ignore;
 public class TestAllDictionaries extends LuceneTestCase {
   
   // set this to the location of where you downloaded all the files
  static final File DICTIONARY_HOME = 
      new File("/data/archive.services.openoffice.org/pub/mirror/OpenOffice.org/contrib/dictionaries");
  static final Path DICTIONARY_HOME = 
      Paths.get("/data/archive.services.openoffice.org/pub/mirror/OpenOffice.org/contrib/dictionaries");
   
   final String tests[] = {
     /* zip file */               /* dictionary */       /* affix */
@@ -156,10 +158,10 @@ public class TestAllDictionaries extends LuceneTestCase {
   
   public void test() throws Exception {
     for (int i = 0; i < tests.length; i += 3) {
      File f = new File(DICTIONARY_HOME, tests[i]);
      assert f.exists();
      Path f = DICTIONARY_HOME.resolve(tests[i]);
      assert Files.exists(f);
       
      try (ZipFile zip = new ZipFile(f, StandardCharsets.UTF_8)) {
      try (ZipFile zip = new ZipFile(f.toFile(), StandardCharsets.UTF_8)) {
         ZipEntry dicEntry = zip.getEntry(tests[i+1]);
         assert dicEntry != null;
         ZipEntry affEntry = zip.getEntry(tests[i+2]);
@@ -185,10 +187,10 @@ public class TestAllDictionaries extends LuceneTestCase {
     String toTest = "zu_ZA.zip";
     for (int i = 0; i < tests.length; i++) {
       if (tests[i].equals(toTest)) {
        File f = new File(DICTIONARY_HOME, tests[i]);
        assert f.exists();
        Path f = DICTIONARY_HOME.resolve(tests[i]);
        assert Files.exists(f);
         
        try (ZipFile zip = new ZipFile(f, StandardCharsets.UTF_8)) {
        try (ZipFile zip = new ZipFile(f.toFile(), StandardCharsets.UTF_8)) {
           ZipEntry dicEntry = zip.getEntry(tests[i+1]);
           assert dicEntry != null;
           ZipEntry affEntry = zip.getEntry(tests[i+2]);
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries2.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries2.java
index 8cb6cb0168b..6fb59e8f667 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries2.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/hunspell/TestAllDictionaries2.java
@@ -17,9 +17,11 @@ package org.apache.lucene.analysis.hunspell;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.InputStream;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipFile;
 
@@ -39,8 +41,7 @@ import org.junit.Ignore;
 public class TestAllDictionaries2 extends LuceneTestCase {
   
   // set this to the location of where you downloaded all the files
  static final File DICTIONARY_HOME = 
      new File("/data/thunderbirdDicts");
  static final Path DICTIONARY_HOME = Paths.get("/data/thunderbirdDicts");
   
   final String tests[] = {
     /* zip file */                                                                    /* dictionary */                      /* affix */
@@ -172,10 +173,10 @@ public class TestAllDictionaries2 extends LuceneTestCase {
   
   public void test() throws Exception {
     for (int i = 0; i < tests.length; i += 3) {
      File f = new File(DICTIONARY_HOME, tests[i]);
      assert f.exists();
      Path f = DICTIONARY_HOME.resolve(tests[i]);
      assert Files.exists(f);
       
      try (ZipFile zip = new ZipFile(f, StandardCharsets.UTF_8)) {
      try (ZipFile zip = new ZipFile(f.toFile(), StandardCharsets.UTF_8)) {
         ZipEntry dicEntry = zip.getEntry(tests[i+1]);
         assert dicEntry != null;
         ZipEntry affEntry = zip.getEntry(tests[i+2]);
@@ -201,10 +202,10 @@ public class TestAllDictionaries2 extends LuceneTestCase {
     String toTest = "hungarian_dictionary-1.6.1.1-fx+tb+sm+fn.xpi";
     for (int i = 0; i < tests.length; i++) {
       if (tests[i].equals(toTest)) {
        File f = new File(DICTIONARY_HOME, tests[i]);
        assert f.exists();
        Path f = DICTIONARY_HOME.resolve(tests[i]);
        assert Files.exists(f);
         
        try (ZipFile zip = new ZipFile(f, StandardCharsets.UTF_8)) {
        try (ZipFile zip = new ZipFile(f.toFile(), StandardCharsets.UTF_8)) {
           ZipEntry dicEntry = zip.getEntry(tests[i+1]);
           assert dicEntry != null;
           ZipEntry affEntry = zip.getEntry(tests[i+2]);
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
index c178fe4a133..c56989d1f86 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/it/TestItalianLightStemFilter.java
@@ -42,7 +42,7 @@ public class TestItalianLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("itlighttestdata.zip"), "itlight.txt");
    assertVocabulary(analyzer, getDataPath("itlighttestdata.zip"), "itlight.txt");
   }
   
   /** blast some random strings through the analyzer */
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianLightStemFilter.java
index 278577381f8..23d29d40496 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianLightStemFilter.java
@@ -20,6 +20,7 @@ package org.apache.lucene.analysis.no;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.Reader;
import java.nio.file.Files;
 import java.util.Random;
 
 import org.apache.lucene.analysis.Analyzer;
@@ -50,7 +51,7 @@ public class TestNorwegianLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary file */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, new FileInputStream(getDataFile("nb_light.txt")));
    assertVocabulary(analyzer, Files.newInputStream(getDataPath("nb_light.txt")));
   }
   
   /** Test against a Nynorsk vocabulary file */
@@ -62,7 +63,7 @@ public class TestNorwegianLightStemFilter extends BaseTokenStreamTestCase {
         return new TokenStreamComponents(source, new NorwegianLightStemFilter(source, NYNORSK));
       }
     };
    assertVocabulary(analyzer, new FileInputStream(getDataFile("nn_light.txt")));
    assertVocabulary(analyzer, Files.newInputStream(getDataPath("nn_light.txt")));
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianMinimalStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianMinimalStemFilter.java
index a0dbc8671d6..d0f53ece4c4 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianMinimalStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/no/TestNorwegianMinimalStemFilter.java
@@ -20,6 +20,7 @@ package org.apache.lucene.analysis.no;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.Reader;
import java.nio.file.Files;
 import java.util.Random;
 
 import org.apache.lucene.analysis.Analyzer;
@@ -49,7 +50,7 @@ public class TestNorwegianMinimalStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a Bokmål vocabulary file */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, new FileInputStream(getDataFile("nb_minimal.txt")));
    assertVocabulary(analyzer, Files.newInputStream(getDataPath("nb_minimal.txt")));
   }
   
   /** Test against a Nynorsk vocabulary file */
@@ -61,7 +62,7 @@ public class TestNorwegianMinimalStemFilter extends BaseTokenStreamTestCase {
         return new TokenStreamComponents(source, new NorwegianMinimalStemFilter(source, NYNORSK));
       }
     };
    assertVocabulary(analyzer, new FileInputStream(getDataFile("nn_minimal.txt")));
    assertVocabulary(analyzer, Files.newInputStream(getDataPath("nn_minimal.txt")));
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
index d04980f3eb8..b53f8d3c3c9 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseLightStemFilter.java
@@ -88,7 +88,7 @@ public class TestPortugueseLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("ptlighttestdata.zip"), "ptlight.txt");
    assertVocabulary(analyzer, getDataPath("ptlighttestdata.zip"), "ptlight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
index c7a4ebad289..3b11516de34 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseMinimalStemFilter.java
@@ -62,7 +62,7 @@ public class TestPortugueseMinimalStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("ptminimaltestdata.zip"), "ptminimal.txt");
    assertVocabulary(analyzer, getDataPath("ptminimaltestdata.zip"), "ptminimal.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
index 24421d1f12d..11b330d11d1 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/pt/TestPortugueseStemFilter.java
@@ -62,7 +62,7 @@ public class TestPortugueseStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("ptrslptestdata.zip"), "ptrslp.txt");
    assertVocabulary(analyzer, getDataPath("ptrslptestdata.zip"), "ptrslp.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
index 1d4a381e9d0..cbafbee446f 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/ru/TestRussianLightStemFilter.java
@@ -45,7 +45,7 @@ public class TestRussianLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("rulighttestdata.zip"), "rulight.txt");
    assertVocabulary(analyzer, getDataPath("rulighttestdata.zip"), "rulight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
index fa50c366ed4..a7a375c1c35 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/snowball/TestSnowballVocab.java
@@ -76,7 +76,7 @@ public class TestSnowballVocab extends LuceneTestCase {
       }  
     };
     
    assertVocabulary(a, getDataFile("TestSnowballVocabData.zip"), 
    assertVocabulary(a, getDataPath("TestSnowballVocabData.zip"), 
         dataDirectory + "/voc.txt", dataDirectory + "/output.txt");
   }
 }
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
index ef87a8e3a67..09ab5258956 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/sv/TestSwedishLightStemFilter.java
@@ -45,7 +45,7 @@ public class TestSwedishLightStemFilter extends BaseTokenStreamTestCase {
   
   /** Test against a vocabulary from the reference impl */
   public void testVocabulary() throws IOException {
    assertVocabulary(analyzer, getDataFile("svlighttestdata.zip"), "svlight.txt");
    assertVocabulary(analyzer, getDataPath("svlighttestdata.zip"), "svlight.txt");
   }
   
   public void testKeyword() throws IOException {
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/util/TestFilesystemResourceLoader.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/util/TestFilesystemResourceLoader.java
index b24c9372dd7..877949e3874 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/util/TestFilesystemResourceLoader.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/util/TestFilesystemResourceLoader.java
@@ -24,6 +24,8 @@ import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
@@ -61,10 +63,9 @@ public class TestFilesystemResourceLoader extends LuceneTestCase {
   }
   
   public void testBaseDir() throws Exception {
    final File base = createTempDir("fsResourceLoaderBase").getAbsoluteFile();
    final Path base = createTempDir("fsResourceLoaderBase");
     try {
      base.mkdirs();
      Writer os = new OutputStreamWriter(new FileOutputStream(new File(base, "template.txt")), StandardCharsets.UTF_8);
      Writer os = Files.newBufferedWriter(base.resolve("template.txt"), StandardCharsets.UTF_8);
       try {
         os.write("foobar\n");
       } finally {
@@ -74,25 +75,18 @@ public class TestFilesystemResourceLoader extends LuceneTestCase {
       ResourceLoader rl = new FilesystemResourceLoader(base);
       assertEquals("foobar", WordlistLoader.getLines(rl.openResource("template.txt"), StandardCharsets.UTF_8).get(0));
       // Same with full path name:
      String fullPath = new File(base, "template.txt").toString();
      String fullPath = base.resolve("template.txt").toAbsolutePath().toString();
       assertEquals("foobar",
           WordlistLoader.getLines(rl.openResource(fullPath), StandardCharsets.UTF_8).get(0));
       assertClasspathDelegation(rl);
       assertNotFound(rl);
      
      // now use RL without base dir:
      rl = new FilesystemResourceLoader();
      assertEquals("foobar",
          WordlistLoader.getLines(rl.openResource(new File(base, "template.txt").toString()), StandardCharsets.UTF_8).get(0));
      assertClasspathDelegation(rl);
      assertNotFound(rl);
     } finally {
       IOUtils.rm(base);
     }
   }
   
   public void testDelegation() throws Exception {
    ResourceLoader rl = new FilesystemResourceLoader(null, new StringMockResourceLoader("foobar\n"));
    ResourceLoader rl = new FilesystemResourceLoader(createTempDir("empty"), new StringMockResourceLoader("foobar\n"));
     assertEquals("foobar", WordlistLoader.getLines(rl.openResource("template.txt"), StandardCharsets.UTF_8).get(0));
   }
   
diff --git a/lucene/analysis/kuromoji/src/tools/java/org/apache/lucene/analysis/ja/util/TokenInfoDictionaryWriter.java b/lucene/analysis/kuromoji/src/tools/java/org/apache/lucene/analysis/ja/util/TokenInfoDictionaryWriter.java
index aa5f5cd00b4..315ace9ef85 100644
-- a/lucene/analysis/kuromoji/src/tools/java/org/apache/lucene/analysis/ja/util/TokenInfoDictionaryWriter.java
++ b/lucene/analysis/kuromoji/src/tools/java/org/apache/lucene/analysis/ja/util/TokenInfoDictionaryWriter.java
@@ -17,8 +17,10 @@ package org.apache.lucene.analysis.ja.util;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.analysis.ja.dict.TokenInfoDictionary;
 import org.apache.lucene.util.fst.FST;
@@ -41,8 +43,8 @@ public class TokenInfoDictionaryWriter extends BinaryDictionaryWriter {
   }
   
   protected void writeFST(String filename) throws IOException {
    File f = new File(filename);
    f.getParentFile().mkdirs();
    fst.save(f);
    Path p = Paths.get(filename);
    Files.createDirectories(p.getParent());
    fst.save(p);
   }  
 }
diff --git a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/AnalyzerProfile.java b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/AnalyzerProfile.java
index 8198abd6294..6a6f45759be 100644
-- a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/AnalyzerProfile.java
++ b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/AnalyzerProfile.java
@@ -17,11 +17,12 @@
 
 package org.apache.lucene.analysis.cn.smart;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
 import java.io.IOException;
import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.Properties;
 
 /**
@@ -51,16 +52,18 @@ public class AnalyzerProfile {
     if (ANALYSIS_DATA_DIR.length() != 0)
       return;
 
    File[] cadidateFiles = new File[] { new File("./" + dirName),
        new File("./lib/" + dirName), new File("./" + propName),
        new File("./lib/" + propName) };
    for (int i = 0; i < cadidateFiles.length; i++) {
      File file = cadidateFiles[i];
      if (file.exists()) {
        if (file.isDirectory()) {
          ANALYSIS_DATA_DIR = file.getAbsolutePath();
        } else if (file.isFile() && getAnalysisDataDir(file).length() != 0) {
          ANALYSIS_DATA_DIR = getAnalysisDataDir(file);
    Path[] candidateFiles = new Path[] {
        Paths.get(dirName),
        Paths.get("lib").resolve(dirName),
        Paths.get(propName),
        Paths.get("lib").resolve(propName)
    };
    for (Path file : candidateFiles) {
      if (Files.exists(file)) {
        if (Files.isDirectory(file)) {
          ANALYSIS_DATA_DIR = file.toAbsolutePath().toString();
        } else if (Files.isRegularFile(file) && getAnalysisDataDir(file).length() != 0) {
          ANALYSIS_DATA_DIR = getAnalysisDataDir(file).toString();
         }
         break;
       }
@@ -75,14 +78,11 @@ public class AnalyzerProfile {
 
   }
 
  private static String getAnalysisDataDir(File propFile) {
  private static String getAnalysisDataDir(Path propFile) {
     Properties prop = new Properties();
    try {
      FileInputStream input = new FileInputStream(propFile);
      prop.load(new InputStreamReader(input, StandardCharsets.UTF_8));
      String dir = prop.getProperty("analysis.data.dir", "");
      input.close();
      return dir;
    try (BufferedReader reader = Files.newBufferedReader(propFile, StandardCharsets.UTF_8)) {
      prop.load(reader);
      return prop.getProperty("analysis.data.dir", "");
     } catch (IOException e) {
       return "";
     }
diff --git a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/BigramDictionary.java b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/BigramDictionary.java
index bffed1a299c..9d67afc6232 100644
-- a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/BigramDictionary.java
++ b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/BigramDictionary.java
@@ -17,18 +17,16 @@
 
 package org.apache.lucene.analysis.cn.smart.hhmm;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.analysis.cn.smart.AnalyzerProfile;
 
@@ -75,9 +73,9 @@ class BigramDictionary extends AbstractDictionary {
     return singleInstance;
   }
 
  private boolean loadFromObj(File serialObj) {
  private boolean loadFromObj(Path serialObj) {
     try {
      loadFromInputStream(new FileInputStream(serialObj));
      loadFromInputStream(Files.newInputStream(serialObj));
       return true;
     } catch (Exception e) {
       throw new RuntimeException(e);
@@ -93,9 +91,9 @@ class BigramDictionary extends AbstractDictionary {
     input.close();
   }
 
  private void saveToObj(File serialObj) {
  private void saveToObj(Path serialObj) {
     try {
      ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(
      ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(
           serialObj));
       output.writeObject(bigramHashTable);
       output.writeObject(frequencyTable);
@@ -114,9 +112,9 @@ class BigramDictionary extends AbstractDictionary {
   private void load(String dictRoot) {
     String bigramDictPath = dictRoot + "/bigramdict.dct";
 
    File serialObj = new File(dictRoot + "/bigramdict.mem");
    Path serialObj = Paths.get(dictRoot + "/bigramdict.mem");
 
    if (serialObj.exists() && loadFromObj(serialObj)) {
    if (Files.exists(serialObj) && loadFromObj(serialObj)) {
 
     } else {
       try {
@@ -149,7 +147,7 @@ class BigramDictionary extends AbstractDictionary {
     int[] buffer = new int[3];
     byte[] intBuffer = new byte[4];
     String tmpword;
    RandomAccessFile dctFile = new RandomAccessFile(dctFilePath, "r");
    DataInputStream dctFile = new DataInputStream(Files.newInputStream(Paths.get(dctFilePath)));
 
     // GB2312 characters 0 - 6768
     for (i = GB2312_FIRST_CHAR; i < GB2312_FIRST_CHAR + CHAR_NUM_IN_FILE; i++) {
diff --git a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/WordDictionary.java b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/WordDictionary.java
index 9a7979ec5a4..7d7e9050ec2 100644
-- a/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/WordDictionary.java
++ b/lucene/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/hhmm/WordDictionary.java
@@ -17,18 +17,16 @@
 
 package org.apache.lucene.analysis.cn.smart.hhmm;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.analysis.cn.smart.AnalyzerProfile;
 import org.apache.lucene.analysis.cn.smart.Utility;
@@ -101,9 +99,9 @@ class WordDictionary extends AbstractDictionary {
    */
   public void load(String dctFileRoot) {
     String dctFilePath = dctFileRoot + "/coredict.dct";
    File serialObj = new File(dctFileRoot + "/coredict.mem");
    Path serialObj = Paths.get(dctFileRoot + "/coredict.mem");
 
    if (serialObj.exists() && loadFromObj(serialObj)) {
    if (Files.exists(serialObj) && loadFromObj(serialObj)) {
 
     } else {
       try {
@@ -140,9 +138,9 @@ class WordDictionary extends AbstractDictionary {
     loadFromObjectInputStream(input);
   }
 
  private boolean loadFromObj(File serialObj) {
  private boolean loadFromObj(Path serialObj) {
     try {
      loadFromObjectInputStream(new FileInputStream(serialObj));
      loadFromObjectInputStream(Files.newInputStream(serialObj));
       return true;
     } catch (Exception e) {
       throw new RuntimeException(e);
@@ -160,9 +158,9 @@ class WordDictionary extends AbstractDictionary {
     input.close();
   }
 
  private void saveToObj(File serialObj) {
  private void saveToObj(Path serialObj) {
     try {
      ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(
      ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(
           serialObj));
       output.writeObject(wordIndexTable);
       output.writeObject(charIndexTable);
@@ -189,7 +187,7 @@ class WordDictionary extends AbstractDictionary {
     int[] buffer = new int[3];
     byte[] intBuffer = new byte[4];
     String tmpword;
    RandomAccessFile dctFile = new RandomAccessFile(dctFilePath, "r");
    DataInputStream dctFile = new DataInputStream(Files.newInputStream(Paths.get(dctFilePath)));
 
     // GB2312 characters 0 - 6768
     for (i = GB2312_FIRST_CHAR; i < GB2312_FIRST_CHAR + CHAR_NUM_IN_FILE; i++) {
diff --git a/lucene/analysis/stempel/src/java/org/egothor/stemmer/Compile.java b/lucene/analysis/stempel/src/java/org/egothor/stemmer/Compile.java
index d7fdaecef8f..13b478f5edb 100644
-- a/lucene/analysis/stempel/src/java/org/egothor/stemmer/Compile.java
++ b/lucene/analysis/stempel/src/java/org/egothor/stemmer/Compile.java
@@ -55,14 +55,11 @@
 package org.egothor.stemmer;
 
 import java.io.BufferedOutputStream;
import java.io.BufferedReader;
 import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
 import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
 import java.util.Locale;
 import java.util.StringTokenizer;
 
@@ -126,8 +123,7 @@ public class Compile {
       allocTrie();
       
       System.out.println(args[i]);
      in = new LineNumberReader(new BufferedReader(new InputStreamReader(
          new FileInputStream(args[i]), charset)));
      in = new LineNumberReader(Files.newBufferedReader(Paths.get(args[i]), Charset.forName(charset)));
       for (String line = in.readLine(); line != null; line = in.readLine()) {
         try {
           line = line.toLowerCase(Locale.ROOT);
@@ -186,7 +182,7 @@ public class Compile {
       }
       
       DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
          new FileOutputStream(args[i] + ".out")));
          Files.newOutputStream(Paths.get(args[i] + ".out"))));
       os.writeUTF(args[0]);
       trie.store(os);
       os.close();
diff --git a/lucene/analysis/stempel/src/java/org/egothor/stemmer/DiffIt.java b/lucene/analysis/stempel/src/java/org/egothor/stemmer/DiffIt.java
index f4bb8d7cfa9..56c09379d30 100644
-- a/lucene/analysis/stempel/src/java/org/egothor/stemmer/DiffIt.java
++ b/lucene/analysis/stempel/src/java/org/egothor/stemmer/DiffIt.java
@@ -54,10 +54,10 @@
  */
 package org.egothor.stemmer;
 
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
 import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
 import java.util.Locale;
 import java.util.StringTokenizer;
 
@@ -99,7 +99,7 @@ public class DiffIt {
       // System.out.println("[" + args[i] + "]");
       Diff diff = new Diff(ins, del, rep, nop);
       String charset = System.getProperty("egothor.stemmer.charset", "UTF-8");
      in = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(args[i]), charset)));
      in = new LineNumberReader(Files.newBufferedReader(Paths.get(args[i]), Charset.forName(charset)));
       for (String line = in.readLine(); line != null; line = in.readLine()) {
         try {
           line = line.toLowerCase(Locale.ROOT);
diff --git a/lucene/analysis/stempel/src/test/org/egothor/stemmer/TestCompile.java b/lucene/analysis/stempel/src/test/org/egothor/stemmer/TestCompile.java
index 6b5c99d5481..d7b29a8413e 100644
-- a/lucene/analysis/stempel/src/test/org/egothor/stemmer/TestCompile.java
++ b/lucene/analysis/stempel/src/test/org/egothor/stemmer/TestCompile.java
@@ -56,77 +56,66 @@ package org.egothor.stemmer;
  */
 
 import java.io.BufferedInputStream;
import java.io.BufferedReader;
 import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
import java.io.InputStreamReader;
 import java.io.LineNumberReader;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Locale;
 import java.util.StringTokenizer;
 
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.LuceneTestCase.SuppressSysoutChecks;
 
 public class TestCompile extends LuceneTestCase {
   
   public void testCompile() throws Exception {
    File dir = createTempDir("testCompile");
    dir.mkdirs();
    InputStream input = getClass().getResourceAsStream("testRules.txt");
    File output = new File(dir, "testRules.txt");
    copy(input, output);
    input.close();
    String path = output.getAbsolutePath();
    Path dir = createTempDir("testCompile");
    Path output = dir.resolve("testRules.txt");
    try (InputStream input = getClass().getResourceAsStream("testRules.txt")) {
      Files.copy(input, output);
    }
    String path = output.toAbsolutePath().toString();
     Compile.main(new String[] {"test", path});
    String compiled = path + ".out";
    Path compiled = dir.resolve("testRules.txt.out");
     Trie trie = loadTrie(compiled);
    assertTrie(trie, path, true, true);
    assertTrie(trie, path, false, true);
    Files.delete(new File(compiled).toPath());
    assertTrie(trie, output, true, true);
    assertTrie(trie, output, false, true);
   }
   
   public void testCompileBackwards() throws Exception {
    File dir = createTempDir("testCompile");
    dir.mkdirs();
    InputStream input = getClass().getResourceAsStream("testRules.txt");
    File output = new File(dir, "testRules.txt");
    copy(input, output);
    input.close();
    String path = output.getAbsolutePath();
    Path dir = createTempDir("testCompile");
    Path output = dir.resolve("testRules.txt");
    try (InputStream input = getClass().getResourceAsStream("testRules.txt")) {
      Files.copy(input, output);
    }
    String path = output.toAbsolutePath().toString();
     Compile.main(new String[] {"-test", path});
    String compiled = path + ".out";
    Path compiled = dir.resolve("testRules.txt.out");
     Trie trie = loadTrie(compiled);
    assertTrie(trie, path, true, true);
    assertTrie(trie, path, false, true);
    Files.delete(new File(compiled).toPath());
    assertTrie(trie, output, true, true);
    assertTrie(trie, output, false, true);
   }
   
   public void testCompileMulti() throws Exception {
    File dir = createTempDir("testCompile");
    dir.mkdirs();
    InputStream input = getClass().getResourceAsStream("testRules.txt");
    File output = new File(dir, "testRules.txt");
    copy(input, output);
    input.close();
    String path = output.getAbsolutePath();
    Path dir = createTempDir("testCompile");
    Path output = dir.resolve("testRules.txt");
    try (InputStream input = getClass().getResourceAsStream("testRules.txt")) {
      Files.copy(input, output);
    }
    String path = output.toAbsolutePath().toString();
     Compile.main(new String[] {"Mtest", path});
    String compiled = path + ".out";
    Path compiled = dir.resolve("testRules.txt.out");
     Trie trie = loadTrie(compiled);
    assertTrie(trie, path, true, true);
    assertTrie(trie, path, false, true);
    Files.delete(new File(compiled).toPath());
    assertTrie(trie, output, true, true);
    assertTrie(trie, output, false, true);
   }
   
  static Trie loadTrie(String path) throws IOException {
  static Trie loadTrie(Path path) throws IOException {
     Trie trie;
     DataInputStream is = new DataInputStream(new BufferedInputStream(
        new FileInputStream(path)));
        Files.newInputStream(path)));
     String method = is.readUTF().toUpperCase(Locale.ROOT);
     if (method.indexOf('M') < 0) {
       trie = new Trie(is);
@@ -137,10 +126,9 @@ public class TestCompile extends LuceneTestCase {
     return trie;
   }
   
  private static void assertTrie(Trie trie, String file, boolean usefull,
  private static void assertTrie(Trie trie, Path file, boolean usefull,
       boolean storeorig) throws Exception {
    LineNumberReader in = new LineNumberReader(new BufferedReader(
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)));
    LineNumberReader in = new LineNumberReader(Files.newBufferedReader(file, StandardCharsets.UTF_8));
     
     for (String line = in.readLine(); line != null; line = in.readLine()) {
       try {
@@ -172,17 +160,4 @@ public class TestCompile extends LuceneTestCase {
     
     in.close();
   }
  
  private static void copy(InputStream input, File output) throws IOException {
    FileOutputStream os = new FileOutputStream(output);
    try {
      byte buffer[] = new byte[1024];
      int len;
      while ((len = input.read(buffer)) > 0) {
        os.write(buffer, 0, len);
      }
    } finally {
      os.close();
    }
  }
 }
diff --git a/lucene/backward-codecs/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
index 47e9901b8f2..ca07da982f9 100644
-- a/lucene/backward-codecs/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
++ b/lucene/backward-codecs/src/test/org/apache/lucene/index/TestBackwardsCompatibility.java
@@ -18,14 +18,14 @@ package org.apache.lucene.index;
  */
 
 import java.io.ByteArrayOutputStream;
import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
@@ -294,8 +294,8 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
     names.addAll(Arrays.asList(oldSingleSegmentNames));
     oldIndexDirs = new HashMap<>();
     for (String name : names) {
      File dir = createTempDir(name);
      File dataFile = new File(TestBackwardsCompatibility.class.getResource("index." + name + ".zip").toURI());
      Path dir = createTempDir(name);
      Path dataFile = Paths.get(TestBackwardsCompatibility.class.getResource("index." + name + ".zip").toURI());
       TestUtil.unzip(dataFile, dir);
       oldIndexDirs.put(name, newFSDirectory(dir));
     }
@@ -434,9 +434,9 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
       if (VERBOSE) {
         System.out.println("TEST: index " + unsupportedNames[i]);
       }
      File oldIndxeDir = createTempDir(unsupportedNames[i]);
      TestUtil.unzip(getDataFile("unsupported." + unsupportedNames[i] + ".zip"), oldIndxeDir);
      BaseDirectoryWrapper dir = newFSDirectory(oldIndxeDir);
      Path oldIndexDir = createTempDir(unsupportedNames[i]);
      TestUtil.unzip(getDataPath("unsupported." + unsupportedNames[i] + ".zip"), oldIndexDir);
      BaseDirectoryWrapper dir = newFSDirectory(oldIndexDir);
       // don't checkindex, these are intentionally not supported
       dir.setCheckIndexOnClose(false);
 
@@ -487,7 +487,7 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
       assertTrue(bos.toString(IOUtils.UTF_8).contains(IndexFormatTooOldException.class.getName()));
 
       dir.close();
      IOUtils.rm(oldIndxeDir);
      IOUtils.rm(oldIndexDir);
     }
   }
   
@@ -795,9 +795,9 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
     reader.close();
   }
 
  public File createIndex(String dirName, boolean doCFS, boolean fullyMerged) throws IOException {
  public Path createIndex(String dirName, boolean doCFS, boolean fullyMerged) throws IOException {
     // we use a real directory name that is not cleaned up, because this method is only used to create backwards indexes:
    File indexDir = new File("/tmp/idx", dirName);
    Path indexDir = Paths.get("/tmp/idx").resolve(dirName);
     IOUtils.rm(indexDir);
     Directory dir = newFSDirectory(indexDir);
     LogByteSizeMergePolicy mp = new LogByteSizeMergePolicy();
@@ -1085,11 +1085,11 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
     System.setOut(new PrintStream(new ByteArrayOutputStream(), false, "UTF-8"));
     try {
       for (String name : oldIndexDirs.keySet()) {
        File dir = createTempDir(name);
        File dataFile = new File(TestBackwardsCompatibility.class.getResource("index." + name + ".zip").toURI());
        Path dir = createTempDir(name);
        Path dataFile = Paths.get(TestBackwardsCompatibility.class.getResource("index." + name + ".zip").toURI());
         TestUtil.unzip(dataFile, dir);
         
        String path = dir.getAbsolutePath();
        String path = dir.toAbsolutePath().toString();
         
         List<String> args = new ArrayList<>();
         if (random().nextBoolean()) {
@@ -1196,8 +1196,8 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
   public static final String moreTermsIndex = "moreterms.40.zip";
 
   public void testMoreTerms() throws Exception {
    File oldIndexDir = createTempDir("moreterms");
    TestUtil.unzip(getDataFile(moreTermsIndex), oldIndexDir);
    Path oldIndexDir = createTempDir("moreterms");
    TestUtil.unzip(getDataPath(moreTermsIndex), oldIndexDir);
     Directory dir = newFSDirectory(oldIndexDir);
     // TODO: more tests
     TestUtil.checkIndex(dir);
@@ -1235,8 +1235,8 @@ public class TestBackwardsCompatibility extends LuceneTestCase {
   }
   
   public void testDocValuesUpdates() throws Exception {
    File oldIndexDir = createTempDir("dvupdates");
    TestUtil.unzip(getDataFile(dvUpdatesIndex), oldIndexDir);
    Path oldIndexDir = createTempDir("dvupdates");
    TestUtil.unzip(getDataPath(dvUpdatesIndex), oldIndexDir);
     Directory dir = newFSDirectory(oldIndexDir);
     
     verifyDocValues(dir);
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/Benchmark.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/Benchmark.java
index 1ceb4d434e8..be627f4c02f 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/Benchmark.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/Benchmark.java
@@ -17,14 +17,14 @@ package org.apache.lucene.benchmark.byTask;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.Reader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.benchmark.byTask.utils.Algorithm;
 import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.util.IOUtils;

 
 /**
  * Run the benchmark algorithm.
@@ -97,17 +97,17 @@ public class Benchmark {
     }
     
     // verify input files 
    File algFile = new File(args[0]);
    if (!algFile.exists() || !algFile.isFile() || !algFile.canRead()) {
      System.err.println("cannot find/read algorithm file: "+algFile.getAbsolutePath()); 
    Path algFile = Paths.get(args[0]);
    if (!Files.isReadable(algFile)) {
      System.err.println("cannot find/read algorithm file: "+algFile.toAbsolutePath()); 
       System.exit(1);
     }
     
    System.out.println("Running algorithm from: "+algFile.getAbsolutePath());
    System.out.println("Running algorithm from: "+algFile.toAbsolutePath());
     
     Benchmark benchmark = null;
     try {
      benchmark = new Benchmark(IOUtils.getDecodingReader(algFile, StandardCharsets.UTF_8));
      benchmark = new Benchmark(Files.newBufferedReader(algFile, StandardCharsets.UTF_8));
     } catch (Exception e) {
       e.printStackTrace();
       System.exit(1);
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/PerfRunData.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/PerfRunData.java
index a63181f95fd..b6e43247a05 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/PerfRunData.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/PerfRunData.java
@@ -18,8 +18,10 @@ package org.apache.lucene.benchmark.byTask;
  */
 
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Locale;
@@ -191,12 +193,12 @@ public class PerfRunData implements Closeable {
   private Directory createDirectory(boolean eraseIndex, String dirName,
       String dirParam) throws IOException {
     if ("FSDirectory".equals(config.get(dirParam,"RAMDirectory"))) {
      File workDir = new File(config.get("work.dir","work"));
      File indexDir = new File(workDir,dirName);
      if (eraseIndex && indexDir.exists()) {
      Path workDir = Paths.get(config.get("work.dir","work"));
      Path indexDir = workDir.resolve(dirName);
      if (eraseIndex && Files.exists(indexDir)) {
         IOUtils.rm(indexDir);
       }
      indexDir.mkdirs();
      Files.createDirectories(indexDir);
       return FSDirectory.open(indexDir);
     } 
 
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ContentItemsSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ContentItemsSource.java
index 9c71787a1fd..459b1efb8a8 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ContentItemsSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ContentItemsSource.java
@@ -18,10 +18,13 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
 import java.util.ArrayList;
import java.util.Arrays;
 
 import org.apache.lucene.benchmark.byTask.utils.Config;
 import org.apache.lucene.benchmark.byTask.utils.Format;
@@ -78,24 +81,19 @@ public abstract class ContentItemsSource implements Closeable {
 
   /**
    * A convenience method for collecting all the files of a content source from
   * a given directory. The collected {@link File} instances are stored in the
   * a given directory. The collected {@link Path} instances are stored in the
    * given <code>files</code>.
    */
  protected final void collectFiles(File dir, ArrayList<File> files) {
    if (!dir.canRead()) {
      return;
    }
    
    File[] dirFiles = dir.listFiles();
    Arrays.sort(dirFiles);
    for (int i = 0; i < dirFiles.length; i++) {
      File file = dirFiles[i];
      if (file.isDirectory()) {
        collectFiles(file, files);
      } else if (file.canRead()) {
        files.add(file);
  protected final void collectFiles(Path dir, final ArrayList<Path> files) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (Files.isReadable(file)) {
          files.add(file.toRealPath());
        }
        return FileVisitResult.CONTINUE;
       }
    }
    });
   }
 
   /**
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/DirContentSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/DirContentSource.java
index 1357dce7305..c5667fdfba5 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/DirContentSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/DirContentSource.java
@@ -20,17 +20,19 @@ package org.apache.lucene.benchmark.byTask.feeds;
 import org.apache.lucene.benchmark.byTask.utils.Config;
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
 import java.io.IOException;
import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.text.DateFormat;
 import java.text.ParsePosition;
 import java.text.SimpleDateFormat;
import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Date;
import java.util.List;
 import java.util.Locale;
 import java.util.Stack;
 
@@ -54,11 +56,11 @@ public class DirContentSource extends ContentSource {
   /**
    * Iterator over the files in the directory
    */
  public static class Iterator implements java.util.Iterator<File> {
  public static class Iterator implements java.util.Iterator<Path> {
 
    static class Comparator implements java.util.Comparator<File> {
    static class Comparator implements java.util.Comparator<Path> {
       @Override
      public int compare(File _a, File _b) {
      public int compare(Path _a, Path _b) {
         String a = _a.toString();
         String b = _b.toString();
         int diff = a.length() - b.length();
@@ -82,47 +84,49 @@ public class DirContentSource extends ContentSource {
 
     int count = 0;
 
    Stack<File> stack = new Stack<>();
    Stack<Path> stack = new Stack<>();
 
     /* this seems silly ... there must be a better way ...
        not that this is good, but can it matter? */
 
     Comparator c = new Comparator();
 
    public Iterator(File f) {
    public Iterator(Path f) throws IOException {
       push(f);
     }
 
    void find() {
    void find() throws IOException {
       if (stack.empty()) {
         return;
       }
      if (!(stack.peek()).isDirectory()) {
      if (!Files.isDirectory(stack.peek())) {
         return;
       }
      File f = stack.pop();
      Path f = stack.pop();
       push(f);
     }
 
    void push(File f) {
      push(f.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
          return file.isDirectory();
    void push(Path f) throws IOException {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(f)) {
        List<Path> found = new ArrayList<>();
        for (Path p : stream) {
          if (Files.isDirectory(p)) {
            found.add(p);
          }
         }
      }));
      push(f.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
          return file.getName().endsWith(".txt");
        push(found.toArray(new Path[found.size()]));
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(f, "*.txt")) {
        List<Path> found = new ArrayList<>();
        for (Path p : stream) {
          found.add(p);
         }
      }));
        push(found.toArray(new Path[found.size()]));
      }
       find();
     }
 
    void push(File[] files) {
    void push(Path[] files) {
       Arrays.sort(files, c);
       for(int i = 0; i < files.length; i++) {
         // System.err.println("push " + files[i]);
@@ -140,12 +144,16 @@ public class DirContentSource extends ContentSource {
     }
     
     @Override
    public File next() {
    public Path next() {
       assert hasNext();
       count++;
      File object = stack.pop();
      Path object = stack.pop();
       // System.err.println("pop " + object);
      find();
      try {
        find();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
       return object;
     }
 
@@ -157,7 +165,7 @@ public class DirContentSource extends ContentSource {
   }
   
   private ThreadLocal<DateFormatInfo> dateFormat = new ThreadLocal<>();
  private File dataDir = null;
  private Path dataDir = null;
   private int iteration = 0;
   private Iterator inputFiles = null;
 
@@ -190,7 +198,7 @@ public class DirContentSource extends ContentSource {
   
   @Override
   public DocData getNextDocData(DocData docData) throws NoMoreDataException, IOException {
    File f = null;
    Path f = null;
     String name = null;
     synchronized (this) {
       if (!inputFiles.hasNext()) { 
@@ -203,10 +211,10 @@ public class DirContentSource extends ContentSource {
       }
       f = inputFiles.next();
       // System.err.println(f);
      name = f.getCanonicalPath()+"_"+iteration;
      name = f.toRealPath()+"_"+iteration;
     }
     
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
    BufferedReader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8);
     String line = null;
     //First line is the date, 3rd is the title, rest is body
     String dateStr = reader.readLine();
@@ -218,7 +226,7 @@ public class DirContentSource extends ContentSource {
       bodyBuf.append(line).append(' ');
     }
     reader.close();
    addBytes(f.length());
    addBytes(Files.size(f));
     
     Date date = parseDate(dateStr);
     
@@ -241,17 +249,21 @@ public class DirContentSource extends ContentSource {
   public void setConfig(Config config) {
     super.setConfig(config);
     
    File workDir = new File(config.get("work.dir", "work"));
    Path workDir = Paths.get(config.get("work.dir", "work"));
     String d = config.get("docs.dir", "dir-out");
    dataDir = new File(d);
    dataDir = Paths.get(d);
     if (!dataDir.isAbsolute()) {
      dataDir = new File(workDir, d);
      dataDir = workDir.resolve(d);
     }
 
    inputFiles = new Iterator(dataDir);
    try {
      inputFiles = new Iterator(dataDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
 
     if (inputFiles == null) {
      throw new RuntimeException("No txt files in dataDir: " + dataDir.getAbsolutePath());
      throw new RuntimeException("No txt files in dataDir: " + dataDir.toAbsolutePath());
     }
   }
 
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/EnwikiContentSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/EnwikiContentSource.java
index 10b61515032..e8a9b3825a2 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/EnwikiContentSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/EnwikiContentSource.java
@@ -17,10 +17,11 @@ package org.apache.lucene.benchmark.byTask.feeds;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
@@ -280,7 +281,7 @@ public class EnwikiContentSource extends ContentSource {
     return val == null ? -1 : val.intValue();
   }
   
  private File file;
  private Path file;
   private boolean keepImages = true;
   private InputStream is;
   private Parser parser = new Parser();
@@ -324,7 +325,7 @@ public class EnwikiContentSource extends ContentSource {
     keepImages = config.get("keep.image.only.docs", true);
     String fileName = config.get("docs.file", null);
     if (fileName != null) {
      file = new File(fileName).getAbsoluteFile();
      file = Paths.get(fileName).toAbsolutePath();
     }
   }
   
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/FileBasedQueryMaker.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/FileBasedQueryMaker.java
index 0717a9d3cd3..622018b28ee 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/FileBasedQueryMaker.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/FileBasedQueryMaker.java
@@ -9,6 +9,9 @@ import org.apache.lucene.util.IOUtils;
 
 import java.io.*;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.List;
 
@@ -58,11 +61,11 @@ public class FileBasedQueryMaker extends AbstractQueryMaker implements QueryMake
     String fileName = config.get("file.query.maker.file", null);
     if (fileName != null)
     {
      File file = new File(fileName);
      Path path = Paths.get(fileName);
       Reader reader = null;
       // note: we use a decoding reader, so if your queries are screwed up you know
      if (file.exists()) {
        reader = IOUtils.getDecodingReader(file, StandardCharsets.UTF_8);
      if (Files.exists(path)) {
        reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
       } else {
         //see if we can find it as a resource
         InputStream asStream = FileBasedQueryMaker.class.getClassLoader().getResourceAsStream(fileName);
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/LineDocSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/LineDocSource.java
index ad00211b62a..3b8b780ee02 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/LineDocSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/LineDocSource.java
@@ -18,11 +18,12 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.Arrays;
 import java.util.Properties;
 
@@ -170,7 +171,7 @@ public class LineDocSource extends ContentSource {
     }
   }
   
  private File file;
  private Path file;
   private BufferedReader reader;
   private int readCount;
 
@@ -276,7 +277,7 @@ public class LineDocSource extends ContentSource {
     if (fileName == null) {
       throw new IllegalArgumentException("docs.file must be set");
     }
    file = new File(fileName).getAbsoluteFile();
    file = Paths.get(fileName).toAbsolutePath();
     if (encoding == null) {
       encoding = IOUtils.UTF_8;
     }
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ReutersContentSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ReutersContentSource.java
index b4d58bc0b01..7b5b021f8cc 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ReutersContentSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/ReutersContentSource.java
@@ -18,11 +18,11 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.text.DateFormat;
 import java.text.ParsePosition;
 import java.text.SimpleDateFormat;
@@ -50,24 +50,28 @@ public class ReutersContentSource extends ContentSource {
   }
 
   private ThreadLocal<DateFormatInfo> dateFormat = new ThreadLocal<>();
  private File dataDir = null;
  private ArrayList<File> inputFiles = new ArrayList<>();
  private Path dataDir = null;
  private ArrayList<Path> inputFiles = new ArrayList<>();
   private int nextFile = 0;
   private int iteration = 0;
   
   @Override
   public void setConfig(Config config) {
     super.setConfig(config);
    File workDir = new File(config.get("work.dir", "work"));
    Path workDir = Paths.get(config.get("work.dir", "work"));
     String d = config.get("docs.dir", "reuters-out");
    dataDir = new File(d);
    dataDir = Paths.get(d);
     if (!dataDir.isAbsolute()) {
      dataDir = new File(workDir, d);
      dataDir = workDir.resolve(d);
     }
     inputFiles.clear();
    collectFiles(dataDir, inputFiles);
    try {
      collectFiles(dataDir, inputFiles);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
     if (inputFiles.size() == 0) {
      throw new RuntimeException("No txt files in dataDir: "+dataDir.getAbsolutePath());
      throw new RuntimeException("No txt files in dataDir: "+dataDir.toAbsolutePath());
     }
   }
 
@@ -99,7 +103,7 @@ public class ReutersContentSource extends ContentSource {
   
   @Override
   public DocData getNextDocData(DocData docData) throws NoMoreDataException, IOException {
    File f = null;
    Path f = null;
     String name = null;
     synchronized (this) {
       if (nextFile >= inputFiles.size()) {
@@ -111,11 +115,10 @@ public class ReutersContentSource extends ContentSource {
         iteration++;
       }
       f = inputFiles.get(nextFile++);
      name = f.getCanonicalPath() + "_" + iteration;
      name = f.toRealPath() + "_" + iteration;
     }
 
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
    try {
    try (BufferedReader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
       // First line is the date, 3rd is the title, rest is body
       String dateStr = reader.readLine();
       reader.readLine();// skip an empty line
@@ -128,7 +131,7 @@ public class ReutersContentSource extends ContentSource {
       }
       reader.close();
       
      addBytes(f.length());
      addBytes(Files.size(f));
       
       Date date = parseDate(dateStr.trim());
       
@@ -138,8 +141,6 @@ public class ReutersContentSource extends ContentSource {
       docData.setTitle(title);
       docData.setDate(date);
       return docData;
    } finally {
      reader.close();
     }
   }
 
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecContentSource.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecContentSource.java
index 439fc594694..8cb05bc149e 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecContentSource.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecContentSource.java
@@ -18,11 +18,13 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.text.DateFormat;
 import java.text.ParsePosition;
 import java.text.SimpleDateFormat;
@@ -82,8 +84,8 @@ public class TrecContentSource extends ContentSource {
 
   private ThreadLocal<DateFormatInfo> dateFormats = new ThreadLocal<>();
   private ThreadLocal<StringBuilder> trecDocBuffer = new ThreadLocal<>();
  private File dataDir = null;
  private ArrayList<File> inputFiles = new ArrayList<>();
  private Path dataDir = null;
  private ArrayList<Path> inputFiles = new ArrayList<>();
   private int nextFile = 0;
   // Use to synchronize threads on reading from the TREC documents.
   private Object lock = new Object();
@@ -174,9 +176,9 @@ public class TrecContentSource extends ContentSource {
         nextFile = 0;
         iteration++;
       }
      File f = inputFiles.get(nextFile++);
      Path f = inputFiles.get(nextFile++);
       if (verbose) {
        System.out.println("opening: " + f + " length: " + f.length());
        System.out.println("opening: " + f + " length: " + Files.size(f));
       }
       try {
         InputStream inputStream = StreamUtils.inputStream(f); // support either gzip, bzip2, or regular text file, by extension  
@@ -185,7 +187,7 @@ public class TrecContentSource extends ContentSource {
         return;
       } catch (Exception e) {
         if (verbose) {
          System.out.println("Skipping 'bad' file " + f.getAbsolutePath()+" due to "+e.getMessage());
          System.out.println("Skipping 'bad' file " + f.toAbsolutePath()+" due to "+e.getMessage());
           continue;
         }
         throw new NoMoreDataException();
@@ -291,14 +293,18 @@ public class TrecContentSource extends ContentSource {
   public void setConfig(Config config) {
     super.setConfig(config);
     // dirs
    File workDir = new File(config.get("work.dir", "work"));
    Path workDir = Paths.get(config.get("work.dir", "work"));
     String d = config.get("docs.dir", "trec");
    dataDir = new File(d);
    dataDir = Paths.get(d);
     if (!dataDir.isAbsolute()) {
      dataDir = new File(workDir, d);
      dataDir = workDir.resolve(d);
     }
     // files
    collectFiles(dataDir, inputFiles);
    try {
      collectFiles(dataDir, inputFiles);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
     if (inputFiles.size() == 0) {
       throw new IllegalArgumentException("No files in dataDir: " + dataDir);
     }
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
index 24b9801a6b7..62ff966e7c8 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/feeds/TrecDocParser.java
@@ -17,8 +17,8 @@ package org.apache.lucene.benchmark.byTask.feeds;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
@@ -57,14 +57,14 @@ public abstract class TrecDocParser {
   /**
    * Compute the path type of a file by inspecting name of file and its parents
    */
  public static ParsePathType pathType(File f) {
  public static ParsePathType pathType(Path f) {
     int pathLength = 0;
     while (f != null && ++pathLength < MAX_PATH_LENGTH) {
      ParsePathType ppt = pathName2Type.get(f.getName().toUpperCase(Locale.ROOT));
      ParsePathType ppt = pathName2Type.get(f.getFileName().toString().toUpperCase(Locale.ROOT));
       if (ppt!=null) {
         return ppt;
       }
      f = f.getParentFile();
      f = f.getParent();
     }
     return DEFAULT_PATH_TYPE;
   }
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTask.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTask.java
index 7d6db25cc03..714a668150c 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTask.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTask.java
@@ -17,7 +17,7 @@ package org.apache.lucene.benchmark.byTask.tasks;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Paths;
 
 import org.apache.lucene.benchmark.byTask.PerfRunData;
 import org.apache.lucene.index.DirectoryReader;
@@ -54,7 +54,7 @@ public class AddIndexesTask extends PerfTask {
     if (inputDirProp == null) {
       throw new IllegalArgumentException("config parameter " + ADDINDEXES_INPUT_DIR + " not specified in configuration");
     }
    inputDir = FSDirectory.open(new File(inputDirProp));
    inputDir = FSDirectory.open(Paths.get(inputDirProp));
   }
   
   @Override
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AnalyzerFactoryTask.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AnalyzerFactoryTask.java
index 31a23125e5a..1d7cdf0a985 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AnalyzerFactoryTask.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/AnalyzerFactoryTask.java
@@ -27,9 +27,11 @@ import org.apache.lucene.benchmark.byTask.PerfRunData;
 import org.apache.lucene.benchmark.byTask.utils.AnalyzerFactory;
 import org.apache.lucene.util.Version;
 
import java.io.File;
 import java.io.StreamTokenizer;
 import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
@@ -364,9 +366,9 @@ public class AnalyzerFactoryTask extends PerfTask {
         throw new RuntimeException("Line #" + lineno(stok) + ": ", e);
       }
       if (instance instanceof ResourceLoaderAware) {
        File baseDir = new File(getRunData().getConfig().get("work.dir", "work")).getAbsoluteFile();
        if ( ! baseDir.isDirectory()) {
          baseDir = new File(".").getAbsoluteFile();
        Path baseDir = Paths.get(getRunData().getConfig().get("work.dir", "work"));
        if (!Files.isDirectory(baseDir)) {
          baseDir = Paths.get(".");
         }
         ((ResourceLoaderAware)instance).inform(new FilesystemResourceLoader(baseDir));
       }
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTask.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTask.java
index b617d218b97..e5651ef3309 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTask.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTask.java
@@ -37,11 +37,12 @@ import org.apache.lucene.index.NoMergeScheduler;
 import org.apache.lucene.util.Version;
 
 import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 /**
  * Create an index. <br>
@@ -191,8 +192,8 @@ public class CreateIndexTask extends PerfTask {
       } else if (infoStreamVal.equals("SystemErr")) {
         iwc.setInfoStream(System.err);
       } else {
        File f = new File(infoStreamVal).getAbsoluteFile();
        iwc.setInfoStream(new PrintStream(new BufferedOutputStream(new FileOutputStream(f)), false, Charset.defaultCharset().name()));
        Path f = Paths.get(infoStreamVal);
        iwc.setInfoStream(new PrintStream(new BufferedOutputStream(Files.newOutputStream(f)), false, Charset.defaultCharset().name()));
       }
     }
     IndexWriter writer = new IndexWriter(runData.getDirectory(), iwc);
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTask.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTask.java
index 334e52fdc35..2caee9d19d5 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTask.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTask.java
@@ -6,6 +6,8 @@ import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.benchmark.byTask.PerfRunData;
 import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
@@ -41,16 +43,16 @@ public class WriteEnwikiLineDocTask extends WriteLineDocTask {
 
   public WriteEnwikiLineDocTask(PerfRunData runData) throws Exception {
     super(runData);
    OutputStream out = StreamUtils.outputStream(categoriesLineFile(new File(fname)));
    OutputStream out = StreamUtils.outputStream(categoriesLineFile(Paths.get(fname)));
     categoryLineFileOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), StreamUtils.BUFFER_SIZE));
     writeHeader(categoryLineFileOut);
   }
 
   /** Compose categories line file out of original line file */
  public static File categoriesLineFile(File f) {
    File dir = f.getParentFile();
    String categoriesName = "categories-"+f.getName();
    return dir==null ? new File(categoriesName) :  new File(dir,categoriesName);
  public static Path categoriesLineFile(Path f) {
    Path dir = f.toAbsolutePath().getParent();
    String categoriesName = "categories-"+f.getFileName();
    return dir.resolve(categoriesName);
   }
   
   @Override
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.java
index 9715b35871c..40535547da7 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.java
@@ -18,11 +18,11 @@ package org.apache.lucene.benchmark.byTask.tasks;
  */
 
 import java.io.BufferedWriter;
import java.io.File;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.regex.Matcher;
@@ -101,7 +101,7 @@ public class WriteLineDocTask extends PerfTask {
     if (fname == null) {
       throw new IllegalArgumentException("line.file.out must be set");
     }
    OutputStream out = StreamUtils.outputStream(new File(fname));
    OutputStream out = StreamUtils.outputStream(Paths.get(fname));
     lineFileOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), StreamUtils.BUFFER_SIZE));
     docMaker = runData.getDocMaker();
     
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/utils/StreamUtils.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/utils/StreamUtils.java
index fbcec4d784d..2f9fb31bd7d 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/utils/StreamUtils.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/byTask/utils/StreamUtils.java
@@ -19,12 +19,11 @@ package org.apache.lucene.benchmark.byTask.utils;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
@@ -83,17 +82,17 @@ public class StreamUtils {
    * based on the file name (e.g., if it ends with .bz2 or .bzip, return a
    * 'bzip' {@link InputStream}).
    */
  public static InputStream inputStream(File file) throws IOException {
  public static InputStream inputStream(Path file) throws IOException {
     // First, create a FileInputStream, as this will be required by all types.
     // Wrap with BufferedInputStream for better performance
    InputStream in = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
    InputStream in = new BufferedInputStream(Files.newInputStream(file), BUFFER_SIZE);
     return fileType(file).inputStream(in);
   }
 
   /** Return the type of the file, or null if unknown */
  private static Type fileType(File file) {
  private static Type fileType(Path file) {
     Type type = null;
    String fileName = file.getName();
    String fileName = file.getFileName().toString();
     int idx = fileName.lastIndexOf('.');
     if (idx != -1) {
       type = extensionToType.get(fileName.substring(idx).toLowerCase(Locale.ROOT));
@@ -103,12 +102,12 @@ public class StreamUtils {
   
   /**
    * Returns an {@link OutputStream} over the requested file, identifying
   * the appropriate {@link OutputStream} instance similar to {@link #inputStream(File)}.
   * the appropriate {@link OutputStream} instance similar to {@link #inputStream(Path)}.
    */
  public static OutputStream outputStream(File file) throws IOException {
  public static OutputStream outputStream(Path file) throws IOException {
     // First, create a FileInputStream, as this will be required by all types.
     // Wrap with BufferedInputStream for better performance
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
    OutputStream os = new BufferedOutputStream(Files.newOutputStream(file), BUFFER_SIZE);
     return fileType(file).outputStream(os);
   }
 }
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/trec/QueryDriver.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/trec/QueryDriver.java
index a683b42d0ed..200574d762a 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/trec/QueryDriver.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/trec/QueryDriver.java
@@ -26,12 +26,13 @@ import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.store.FSDirectory;
 import org.apache.lucene.util.IOUtils;
 
import java.io.BufferedReader;
import java.io.File;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.nio.charset.Charset;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.HashSet;
 import java.util.Set;
 
@@ -52,10 +53,10 @@ public class QueryDriver {
       System.exit(1);
     }
     
    File topicsFile = new File(args[0]);
    File qrelsFile = new File(args[1]);
    Path topicsFile = Paths.get(args[0]);
    Path qrelsFile = Paths.get(args[1]);
     SubmissionReport submitLog = new SubmissionReport(new PrintWriter(args[2], IOUtils.UTF_8 /* huh, no nio.Charset ctor? */), "lucene");
    FSDirectory dir = FSDirectory.open(new File(args[3]));
    FSDirectory dir = FSDirectory.open(Paths.get(args[3]));
     String fieldSpec = args.length == 5 ? args[4] : "T"; // default to Title-only if not specified.
     IndexReader reader = DirectoryReader.open(dir);
     IndexSearcher searcher = new IndexSearcher(reader);
@@ -67,10 +68,10 @@ public class QueryDriver {
 
     // use trec utilities to read trec topics into quality queries
     TrecTopicsReader qReader = new TrecTopicsReader();
    QualityQuery qqs[] = qReader.readQueries(new BufferedReader(IOUtils.getDecodingReader(topicsFile, StandardCharsets.UTF_8)));
    QualityQuery qqs[] = qReader.readQueries(Files.newBufferedReader(topicsFile, StandardCharsets.UTF_8));
 
     // prepare judge, with trec utilities that read from a QRels file
    Judge judge = new TrecJudge(new BufferedReader(IOUtils.getDecodingReader(qrelsFile, StandardCharsets.UTF_8)));
    Judge judge = new TrecJudge(Files.newBufferedReader(qrelsFile, StandardCharsets.UTF_8));
 
     // validate topics & judgments match each other
     judge.validateData(qqs, logger);
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/utils/QualityQueriesFinder.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/utils/QualityQueriesFinder.java
index 9915df7e2ad..942a79bbdd5 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/utils/QualityQueriesFinder.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/quality/utils/QualityQueriesFinder.java
@@ -16,8 +16,8 @@
  */
 package org.apache.lucene.benchmark.quality.utils;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Paths;
 
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
@@ -54,7 +54,7 @@ public class QualityQueriesFinder {
       System.err.println("Usage: java QualityQueriesFinder <index-dir>");
       System.exit(1);
     }
    QualityQueriesFinder qqf = new QualityQueriesFinder(FSDirectory.open(new File(args[0])));
    QualityQueriesFinder qqf = new QualityQueriesFinder(FSDirectory.open(Paths.get(args[0])));
     String q[] = qqf.bestQueries("body",20);
     for (int i=0; i<q.length; i++) {
       System.out.println(newline+formatQueryAsTrecTopic(i,q[i],null,null));
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractReuters.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractReuters.java
index 7568374a3af..5ddab6001e2 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractReuters.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractReuters.java
@@ -17,48 +17,43 @@ package org.apache.lucene.benchmark.utils;
 
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
 import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
 import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
import org.apache.lucene.util.IOUtils;

 
 /**
  * Split the Reuters SGML documents into Simple Text files containing: Title, Date, Dateline, Body
  */
 public class ExtractReuters {
  private File reutersDir;
  private File outputDir;
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private Path reutersDir;
  private Path outputDir;
 
  public ExtractReuters(File reutersDir, File outputDir) throws IOException {
  public ExtractReuters(Path reutersDir, Path outputDir) throws IOException {
     this.reutersDir = reutersDir;
     this.outputDir = outputDir;
     System.out.println("Deleting all files in " + outputDir);
    for (File f : outputDir.listFiles()) {
      Files.delete(f.toPath());
    }
    IOUtils.rm(outputDir);
   }
 
  public void extract() {
    File[] sgmFiles = reutersDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().endsWith(".sgm");
      }
    });
    if (sgmFiles != null && sgmFiles.length > 0) {
      for (File sgmFile : sgmFiles) {
  public void extract() throws IOException {
    long count = 0;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(reutersDir, "*.sgm")) {
      for (Path sgmFile : stream) {
         extractFile(sgmFile);
        count++;
       }
    } else {
    }
    if (count == 0) {
       System.err.println("No .sgm files in " + reutersDir);
     }
   }
@@ -74,10 +69,8 @@ public class ExtractReuters {
   /**
    * Override if you wish to change what is extracted
    */
  protected void extractFile(File sgmFile) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sgmFile), StandardCharsets.UTF_8));

  protected void extractFile(Path sgmFile) {
    try (BufferedReader reader = Files.newBufferedReader(sgmFile, StandardCharsets.UTF_8)) {
       StringBuilder buffer = new StringBuilder(1024);
       StringBuilder outBuffer = new StringBuilder(1024);
 
@@ -101,23 +94,21 @@ public class ExtractReuters {
                 outBuffer.append(matcher.group(i));
               }
             }
            outBuffer.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            outBuffer.append(System.lineSeparator()).append(System.lineSeparator());
           }
           String out = outBuffer.toString();
           for (int i = 0; i < META_CHARS_SERIALIZATIONS.length; i++) {
             out = out.replaceAll(META_CHARS_SERIALIZATIONS[i], META_CHARS[i]);
           }
          File outFile = new File(outputDir, sgmFile.getName() + "-"
              + (docNumber++) + ".txt");
          Path outFile = outputDir.resolve(sgmFile.getFileName() + "-" + (docNumber++) + ".txt");
           // System.out.println("Writing " + outFile);
          OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
          writer.write(out);
          writer.close();
          try (BufferedWriter writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            writer.write(out);
          }
           outBuffer.setLength(0);
           buffer.setLength(0);
         }
       }
      reader.close();
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
@@ -128,21 +119,20 @@ public class ExtractReuters {
       usage("Wrong number of arguments ("+args.length+")");
       return;
     }
    File reutersDir = new File(args[0]);
    if (!reutersDir.exists()) {
    Path reutersDir = Paths.get(args[0]);
    if (!Files.exists(reutersDir)) {
       usage("Cannot find Path to Reuters SGM files ("+reutersDir+")");
       return;
     }
     
     // First, extract to a tmp directory and only if everything succeeds, rename
     // to output directory.
    File outputDir = new File(args[1]);
    outputDir = new File(outputDir.getAbsolutePath() + "-tmp");
    outputDir.mkdirs();
    Path outputDir = Paths.get(args[1] + "-tmp");
    Files.createDirectories(outputDir);
     ExtractReuters extractor = new ExtractReuters(reutersDir, outputDir);
     extractor.extract();
     // Now rename to requested output dir
    outputDir.renameTo(new File(args[1]));
    Files.move(outputDir, Paths.get(args[1]), StandardCopyOption.ATOMIC_MOVE);
   }
 
   private static void usage(String msg) {
diff --git a/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractWikipedia.java b/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractWikipedia.java
index 66ec9d0e1aa..e85656a0357 100644
-- a/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractWikipedia.java
++ b/lucene/benchmark/src/java/org/apache/lucene/benchmark/utils/ExtractWikipedia.java
@@ -17,13 +17,12 @@ package org.apache.lucene.benchmark.utils;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.Properties;
 
 import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
@@ -32,30 +31,28 @@ import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
 import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
 import org.apache.lucene.benchmark.byTask.utils.Config;
 import org.apache.lucene.document.Document;
import org.apache.lucene.util.IOUtils;
 
 /**
  * Extract the downloaded Wikipedia dump into separate files for indexing.
  */
 public class ExtractWikipedia {
 
  private File outputDir;
  private Path outputDir;
 
   static public int count = 0;
 
   static final int BASE = 10;
   protected DocMaker docMaker;
 
  public ExtractWikipedia(DocMaker docMaker, File outputDir) throws IOException {
  public ExtractWikipedia(DocMaker docMaker, Path outputDir) throws IOException {
     this.outputDir = outputDir;
     this.docMaker = docMaker;
     System.out.println("Deleting all files in " + outputDir);
    File[] files = outputDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      Files.delete(files[i].toPath());
    }
    IOUtils.rm(outputDir);
   }
 
  public File directory(int count, File directory) {
  public Path directory(int count, Path directory) {
     if (directory == null) {
       directory = outputDir;
     }
@@ -66,16 +63,16 @@ public class ExtractWikipedia {
     if (count < BASE) {
       return directory;
     }
    directory = new File(directory, (Integer.toString(base / BASE)));
    directory = new File(directory, (Integer.toString(count / (base / BASE))));
    directory = directory.resolve(Integer.toString(base / BASE));
    directory = directory.resolve(Integer.toString(count / (base / BASE)));
     return directory(count % (base / BASE), directory);
   }
 
  public void create(String id, String title, String time, String body) {
  public void create(String id, String title, String time, String body) throws IOException {
 
    File d = directory(count++, null);
    d.mkdirs();
    File f = new File(d, id + ".txt");
    Path d = directory(count++, null);
    Files.createDirectories(d);
    Path f = d.resolve(id + ".txt");
 
     StringBuilder contents = new StringBuilder();
 
@@ -86,14 +83,9 @@ public class ExtractWikipedia {
     contents.append(body);
     contents.append("\n");
 
    try {
      Writer writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
    try (Writer writer = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
       writer.write(contents.toString());
      writer.close();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
     }

   }
 
   public void extract() throws Exception {
@@ -114,16 +106,16 @@ public class ExtractWikipedia {
 
   public static void main(String[] args) throws Exception {
 
    File wikipedia = null;
    File outputDir = new File("./enwiki");
    Path wikipedia = null;
    Path outputDir = Paths.get("enwiki");
     boolean keepImageOnlyDocs = true;
     for (int i = 0; i < args.length; i++) {
       String arg = args[i];
       if (arg.equals("--input") || arg.equals("-i")) {
        wikipedia = new File(args[i + 1]);
        wikipedia = Paths.get(args[i + 1]);
         i++;
       } else if (arg.equals("--output") || arg.equals("-o")) {
        outputDir = new File(args[i + 1]);
        outputDir = Paths.get(args[i + 1]);
         i++;
       } else if (arg.equals("--discardImageOnlyDocs") || arg.equals("-d")) {
         keepImageOnlyDocs = false;
@@ -131,7 +123,7 @@ public class ExtractWikipedia {
     }
     
     Properties properties = new Properties();
    properties.setProperty("docs.file", wikipedia.getAbsolutePath());
    properties.setProperty("docs.file", wikipedia.toAbsolutePath().toString());
     properties.setProperty("content.source.forever", "false");
     properties.setProperty("keep.image.only.docs", String.valueOf(keepImageOnlyDocs));
     Config config = new Config(properties);
@@ -142,9 +134,9 @@ public class ExtractWikipedia {
     DocMaker docMaker = new DocMaker();
     docMaker.setConfig(config, source);
     docMaker.resetInputs();
    if (wikipedia.exists()) {
    if (Files.exists(wikipedia)) {
       System.out.println("Extracting Wikipedia to: " + outputDir + " using EnwikiContentSource");
      outputDir.mkdirs();
      Files.createDirectories(outputDir);
       ExtractWikipedia extractor = new ExtractWikipedia(docMaker, outputDir);
       extractor.extract();
     } else {
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/BenchmarkTestCase.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/BenchmarkTestCase.java
index 0b840e32bd7..65b4a762600 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/BenchmarkTestCase.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/BenchmarkTestCase.java
@@ -17,12 +17,11 @@ package org.apache.lucene.benchmark;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
import java.io.OutputStream;
 import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.benchmark.byTask.Benchmark;
 import org.apache.lucene.util.LuceneTestCase;
@@ -33,7 +32,7 @@ import org.junit.BeforeClass;
 /** Base class for all Benchmark unit tests. */
 @SuppressSysoutChecks(bugUrl = "very noisy")
 public abstract class BenchmarkTestCase extends LuceneTestCase {
  private static File WORKDIR;
  private static Path WORKDIR;
   
   @BeforeClass
   public static void beforeClassBenchmarkTestCase() {
@@ -45,33 +44,27 @@ public abstract class BenchmarkTestCase extends LuceneTestCase {
     WORKDIR = null;
   }
   
  public File getWorkDir() {
  public Path getWorkDir() {
     return WORKDIR;
   }
   
   /** Copy a resource into the workdir */
   public void copyToWorkDir(String resourceName) throws IOException {
    InputStream resource = getClass().getResourceAsStream(resourceName);
    OutputStream dest = new FileOutputStream(new File(getWorkDir(), resourceName));
    byte[] buffer = new byte[8192];
    int len;
    
    while ((len = resource.read(buffer)) > 0) {
        dest.write(buffer, 0, len);
    Path target = getWorkDir().resolve(resourceName);
    Files.deleteIfExists(target);
    try (InputStream resource = getClass().getResourceAsStream(resourceName)) {
      Files.copy(resource, target);
     }

    resource.close();
    dest.close();
   }
   
   /** Return a path, suitable for a .alg config file, for a resource in the workdir */
   public String getWorkDirResourcePath(String resourceName) {
    return new File(getWorkDir(), resourceName).getAbsolutePath().replace("\\", "/");
    return getWorkDir().resolve(resourceName).toAbsolutePath().toString().replace("\\", "/");
   }
   
   /** Return a path, suitable for a .alg config file, for the workdir */
   public String getWorkDirPath() {
    return getWorkDir().getAbsolutePath().replace("\\", "/");
    return getWorkDir().toAbsolutePath().toString().replace("\\", "/");
   }
   
   // create the benchmark and execute it. 
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksLogic.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksLogic.java
index 9166237cec5..fb8cf26360c 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksLogic.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksLogic.java
@@ -18,11 +18,9 @@
 package org.apache.lucene.benchmark.byTask;
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.text.Collator;
 import java.util.List;
 import java.util.Locale;
@@ -384,7 +382,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
    * Test WriteLineDoc and LineDocSource.
    */
   public void testLineDocFile() throws Exception {
    File lineFile = createTempFile("test.reuters.lines", ".txt");
    Path lineFile = createTempFile("test.reuters.lines", ".txt");
 
     // We will call WriteLineDocs this many times
     final int NUM_TRY_DOCS = 50;
@@ -394,7 +392,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
       "# ----- properties ",
       "content.source=org.apache.lucene.benchmark.byTask.feeds.SingleDocSource",
       "content.source.forever=true",
      "line.file.out=" + lineFile.getAbsolutePath().replace('\\', '/'),
      "line.file.out=" + lineFile.toAbsolutePath().toString().replace('\\', '/'),
       "# ----- alg ",
       "{WriteLineDoc()}:" + NUM_TRY_DOCS,
     };
@@ -402,9 +400,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
     // Run algo
     Benchmark benchmark = execBenchmark(algLines1);
 
    BufferedReader r = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(lineFile), StandardCharsets.UTF_8));
    BufferedReader r = Files.newBufferedReader(lineFile, StandardCharsets.UTF_8);
     int numLines = 0;
     String line;
     while((line = r.readLine()) != null) {
@@ -421,7 +417,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
       "# ----- properties ",
       "analyzer=org.apache.lucene.analysis.core.WhitespaceAnalyzer",
       "content.source=org.apache.lucene.benchmark.byTask.feeds.LineDocSource",
      "docs.file=" + lineFile.getAbsolutePath().replace('\\', '/'),
      "docs.file=" + lineFile.toAbsolutePath().toString().replace('\\', '/'),
       "content.source.forever=false",
       "doc.reuse.fields=false",
       "ram.flush.mb=4",
@@ -445,7 +441,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
     assertEquals(numLines + " lines were created but " + ir.numDocs() + " docs are in the index", numLines, ir.numDocs());
     ir.close();
 
    Files.delete(lineFile.toPath());
    Files.delete(lineFile);
   }
   
   /**
@@ -1064,7 +1060,7 @@ public class TestPerfTasksLogic extends BenchmarkTestCase {
     String algLines[] = {
         "content.source=org.apache.lucene.benchmark.byTask.feeds.LineDocSource",
         "docs.file=" + getReuters20LinesFile(),
        "work.dir=" + getWorkDir().getAbsolutePath().replaceAll("\\\\", "/"), // Fix Windows path
        "work.dir=" + getWorkDir().toAbsolutePath().toString().replaceAll("\\\\", "/"), // Fix Windows path
         "content.source.forever=false",
         "directory=RAMDirectory",
         "AnalyzerFactory(name:'" + singleQuoteEscapedName + "', " + params + ")",
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksParse.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksParse.java
index dfca802b3cc..6aefbc88edd 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksParse.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/TestPerfTasksParse.java
@@ -17,13 +17,13 @@
 
 package org.apache.lucene.benchmark.byTask;
 
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
 import java.io.IOException;
import java.io.InputStreamReader;
 import java.io.StringReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 
 import org.apache.lucene.benchmark.byTask.feeds.AbstractQueryMaker;
@@ -114,22 +114,19 @@ public class TestPerfTasksParse extends LuceneTestCase {
   public void testParseExamples() throws Exception {
     // hackedy-hack-hack
     boolean foundFiles = false;
    final File examplesDir = new File(ConfLoader.class.getResource(".").toURI());
    for (File algFile : examplesDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) { return pathname.isFile() && pathname.getName().endsWith(".alg"); }
    })) {
      try {
        Config config = new Config(new InputStreamReader(new FileInputStream(algFile), StandardCharsets.UTF_8));
    final Path examplesDir = Paths.get(ConfLoader.class.getResource(".").toURI());
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(examplesDir, "*.alg")) {
      for (Path path : stream) {
        Config config = new Config(Files.newBufferedReader(path, StandardCharsets.UTF_8));
         String contentSource = config.get("content.source", null);
         if (contentSource != null) { Class.forName(contentSource); }
        config.set("work.dir", createTempDir(LuceneTestCase.getTestClass().getSimpleName()).getAbsolutePath());
        config.set("work.dir", createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toAbsolutePath().toString());
         config.set("content.source", MockContentSource.class.getName());
         String dir = config.get("content.source", null);
         if (dir != null) { Class.forName(dir); }
         config.set("directory", RAMDirectory.class.getName());
         if (config.get("line.file.out", null) != null) {
          config.set("line.file.out", createTempFile("linefile", ".txt").getAbsolutePath());
          config.set("line.file.out", createTempFile("linefile", ".txt").toAbsolutePath().toString());
         }
         if (config.get("query.maker", null) != null) {
           Class.forName(config.get("query.maker", null));
@@ -137,14 +134,11 @@ public class TestPerfTasksParse extends LuceneTestCase {
         }
         PerfRunData data = new PerfRunData(config);
         new Algorithm(data);
      } catch (Throwable t) {
        throw new AssertionError("Could not parse sample file: " + algFile, t);
        foundFiles = true;
       }
      foundFiles = true;
     }
     if (!foundFiles) {
       fail("could not find any .alg files!");
     }
   }

 }
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/DocMakerTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/DocMakerTest.java
index dcff96f8e24..0d7292e39ee 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/DocMakerTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/DocMakerTest.java
@@ -17,8 +17,9 @@ package org.apache.lucene.benchmark.byTask.feeds;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Properties;
 
 import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
@@ -166,13 +167,13 @@ public class DocMakerTest extends BenchmarkTestCase {
   public void testDocMakerLeak() throws Exception {
     // DocMaker did not close its ContentSource if resetInputs was called twice,
     // leading to a file handle leak.
    File f = new File(getWorkDir(), "docMakerLeak.txt");
    PrintStream ps = new PrintStream(f, IOUtils.UTF_8);
    Path f = getWorkDir().resolve("docMakerLeak.txt");
    PrintStream ps = new PrintStream(Files.newOutputStream(f), true, IOUtils.UTF_8);
     ps.println("one title\t" + System.currentTimeMillis() + "\tsome content");
     ps.close();
     
     Properties props = new Properties();
    props.setProperty("docs.file", f.getAbsolutePath());
    props.setProperty("docs.file", f.toAbsolutePath().toString());
     props.setProperty("content.source.forever", "false");
     Config config = new Config(props);
     
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/LineDocSourceTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/LineDocSourceTest.java
index 9b6bfd11b13..cce2f68b4c9 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/LineDocSourceTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/LineDocSourceTest.java
@@ -18,12 +18,12 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Properties;
 
 import org.apache.commons.compress.compressors.CompressorStreamFactory;
@@ -51,8 +51,8 @@ public class LineDocSourceTest extends BenchmarkTestCase {
 
   private static final CompressorStreamFactory csFactory = new CompressorStreamFactory();
 
  private void createBZ2LineFile(File file, boolean addHeader) throws Exception {
    OutputStream out = new FileOutputStream(file);
  private void createBZ2LineFile(Path file, boolean addHeader) throws Exception {
    OutputStream out = Files.newOutputStream(file);
     out = csFactory.createCompressorOutputStream("bzip2", out);
     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
     writeDocsToFile(writer, addHeader, null);
@@ -89,15 +89,15 @@ public class LineDocSourceTest extends BenchmarkTestCase {
     writer.newLine();
   }
 
  private void createRegularLineFile(File file, boolean addHeader) throws Exception {
    OutputStream out = new FileOutputStream(file);
  private void createRegularLineFile(Path file, boolean addHeader) throws Exception {
    OutputStream out = Files.newOutputStream(file);
     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
     writeDocsToFile(writer, addHeader, null);
     writer.close();
   }
 
  private void createRegularLineFileWithMoreFields(File file, String...extraFields) throws Exception {
    OutputStream out = new FileOutputStream(file);
  private void createRegularLineFileWithMoreFields(Path file, String...extraFields) throws Exception {
    OutputStream out = Files.newOutputStream(file);
     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
     Properties p = new Properties();
     for (String f : extraFields) {
@@ -107,13 +107,13 @@ public class LineDocSourceTest extends BenchmarkTestCase {
     writer.close();
   }
   
  private void doIndexAndSearchTest(File file, Class<? extends LineParser> lineParserClass, String storedField) throws Exception {
  private void doIndexAndSearchTest(Path file, Class<? extends LineParser> lineParserClass, String storedField) throws Exception {
     doIndexAndSearchTestWithRepeats(file, lineParserClass, 1, storedField); // no extra repetitions
     doIndexAndSearchTestWithRepeats(file, lineParserClass, 2, storedField); // 1 extra repetition
     doIndexAndSearchTestWithRepeats(file, lineParserClass, 4, storedField); // 3 extra repetitions
   }
   
  private void doIndexAndSearchTestWithRepeats(File file, 
  private void doIndexAndSearchTestWithRepeats(Path file, 
       Class<? extends LineParser> lineParserClass, int numAdds, String storedField) throws Exception {
     
     IndexReader reader = null;
@@ -123,7 +123,7 @@ public class LineDocSourceTest extends BenchmarkTestCase {
       Properties props = new Properties();
       
       // LineDocSource specific settings.
      props.setProperty("docs.file", file.getAbsolutePath());
      props.setProperty("docs.file", file.toAbsolutePath().toString());
       if (lineParserClass != null) {
         props.setProperty("line.parser", lineParserClass.getName());
       }
@@ -169,31 +169,31 @@ public class LineDocSourceTest extends BenchmarkTestCase {
   
   /* Tests LineDocSource with a bzip2 input stream. */
   public void testBZip2() throws Exception {
    File file = new File(getWorkDir(), "one-line.bz2");
    Path file = getWorkDir().resolve("one-line.bz2");
     createBZ2LineFile(file,true);
     doIndexAndSearchTest(file, null, null);
   }
 
   public void testBZip2NoHeaderLine() throws Exception {
    File file = new File(getWorkDir(), "one-line.bz2");
    Path file = getWorkDir().resolve("one-line.bz2");
     createBZ2LineFile(file,false);
     doIndexAndSearchTest(file, null, null);
   }
   
   public void testRegularFile() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     createRegularLineFile(file,true);
     doIndexAndSearchTest(file, null, null);
   }
 
   public void testRegularFileSpecialHeader() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     createRegularLineFile(file,true);
     doIndexAndSearchTest(file, HeaderLineParser.class, null);
   }
 
   public void testRegularFileNoHeaderLine() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     createRegularLineFile(file,false);
     doIndexAndSearchTest(file, null, null);
   }
@@ -209,8 +209,8 @@ public class LineDocSourceTest extends BenchmarkTestCase {
     };
     
     for (int i = 0; i < testCases.length; i++) {
      File file = new File(getWorkDir(), "one-line");
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
      Path file = getWorkDir().resolve("one-line");
      BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
       writer.write(testCases[i]);
       writer.newLine();
       writer.close();
@@ -225,14 +225,14 @@ public class LineDocSourceTest extends BenchmarkTestCase {
   
   /** Doc Name is not part of the default header */
   public void testWithDocsName()  throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     createRegularLineFileWithMoreFields(file, DocMaker.NAME_FIELD);
     doIndexAndSearchTest(file, null, DocMaker.NAME_FIELD);
   }
 
   /** Use fields names that are not defined in Docmaker and so will go to Properties */
   public void testWithProperties()  throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     String specialField = "mySpecialField";
     createRegularLineFileWithMoreFields(file, specialField);
     doIndexAndSearchTest(file, null, specialField);
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/TrecContentSourceTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/TrecContentSourceTest.java
index 9fc2f4f55b6..6a4162b3c0f 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/TrecContentSourceTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/feeds/TrecContentSourceTest.java
@@ -18,9 +18,9 @@ package org.apache.lucene.benchmark.byTask.feeds;
  */
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.IOException;
 import java.io.StringReader;
import java.nio.file.Path;
 import java.text.ParseException;
 import java.util.Arrays;
 import java.util.Date;
@@ -32,7 +32,6 @@ import org.apache.lucene.benchmark.byTask.utils.Config;
 import org.apache.lucene.document.DateTools;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.TestUtil;
import org.apache.lucene.util.TestUtil;
 
 public class TrecContentSourceTest extends LuceneTestCase {
 
@@ -344,15 +343,15 @@ public class TrecContentSourceTest extends LuceneTestCase {
    * supported formats - bzip, gzip, txt. 
    */
   public void testTrecFeedDirAllTypes() throws Exception {
    File dataDir =  createTempDir("trecFeedAllTypes");
    TestUtil.unzip(getDataFile("trecdocs.zip"), dataDir);
    Path dataDir =  createTempDir("trecFeedAllTypes");
    TestUtil.unzip(getDataPath("trecdocs.zip"), dataDir);
     TrecContentSource tcs = new TrecContentSource();
     Properties props = new Properties();
     props.setProperty("print.props", "false");
     props.setProperty("content.source.verbose", "false");
     props.setProperty("content.source.excludeIteration", "true");
     props.setProperty("doc.maker.forever", "false");
    props.setProperty("docs.dir", dataDir.getCanonicalPath().replace('\\','/')); 
    props.setProperty("docs.dir", dataDir.toRealPath().toString().replace('\\','/')); 
     props.setProperty("trec.doc.parser", TrecParserByPath.class.getName());
     props.setProperty("content.source.forever", "false");
     tcs.setConfig(new Config(props));
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTaskTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTaskTest.java
index 19f0d21a1cb..d5a3114c141 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTaskTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/AddIndexesTaskTest.java
@@ -17,7 +17,7 @@ package org.apache.lucene.benchmark.byTask.tasks;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Path;
 import java.util.Properties;
 
 import org.apache.lucene.benchmark.BenchmarkTestCase;
@@ -36,14 +36,14 @@ import org.junit.BeforeClass;
 /** Tests the functionality of {@link AddIndexesTask}. */
 public class AddIndexesTaskTest extends BenchmarkTestCase {
 
  private static File testDir, inputDir;
  private static Path testDir, inputDir;
   
   @BeforeClass
   public static void beforeClassAddIndexesTaskTest() throws Exception {
     testDir = createTempDir("addIndexesTask");
     
     // create a dummy index under inputDir
    inputDir = new File(testDir, "input");
    inputDir = testDir.resolve("input");
     Directory tmpDir = newFSDirectory(inputDir);
     try {
       IndexWriter writer = new IndexWriter(tmpDir, new IndexWriterConfig(null));
@@ -61,7 +61,7 @@ public class AddIndexesTaskTest extends BenchmarkTestCase {
     props.setProperty("writer.version", Version.LATEST.toString());
     props.setProperty("print.props", "false"); // don't print anything
     props.setProperty("directory", "RAMDirectory");
    props.setProperty(AddIndexesTask.ADDINDEXES_INPUT_DIR, inputDir.getAbsolutePath());
    props.setProperty(AddIndexesTask.ADDINDEXES_INPUT_DIR, inputDir.toAbsolutePath().toString());
     Config config = new Config(props);
     return new PerfRunData(config);
   }
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTaskTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTaskTest.java
index 7950cbda5b5..fc4230e9373 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTaskTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/CreateIndexTaskTest.java
@@ -18,9 +18,10 @@ package org.apache.lucene.benchmark.byTask.tasks;
  */
 
 import java.io.ByteArrayOutputStream;
import java.io.File;
 import java.io.PrintStream;
 import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Properties;
 
 import org.apache.lucene.benchmark.BenchmarkTestCase;
@@ -79,11 +80,11 @@ public class CreateIndexTaskTest extends BenchmarkTestCase {
 
   public void testInfoStream_File() throws Exception {
     
    File outFile = new File(getWorkDir(), "infoStreamTest");
    PerfRunData runData = createPerfRunData(outFile.getAbsolutePath());
    Path outFile = getWorkDir().resolve("infoStreamTest");
    PerfRunData runData = createPerfRunData(outFile.toAbsolutePath().toString());
     new CreateIndexTask(runData).doLogic();
     new CloseIndexTask(runData).doLogic();
    assertTrue(outFile.length() > 0);
    assertTrue(Files.size(outFile) > 0);
   }
 
   public void testNoMergePolicy() throws Exception {
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTaskTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTaskTest.java
index d93fef2c7b0..ac8277c919a 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTaskTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteEnwikiLineDocTaskTest.java
@@ -23,6 +23,8 @@ import java.io.FileInputStream;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Properties;
 import java.util.concurrent.atomic.AtomicInteger;
 
@@ -56,26 +58,24 @@ public class WriteEnwikiLineDocTaskTest extends BenchmarkTestCase {
     
   }
   
  private PerfRunData createPerfRunData(File file, String docMakerName) throws Exception {
  private PerfRunData createPerfRunData(Path file, String docMakerName) throws Exception {
     Properties props = new Properties();
     props.setProperty("doc.maker", docMakerName);
    props.setProperty("line.file.out", file.getAbsolutePath());
    props.setProperty("line.file.out", file.toAbsolutePath().toString());
     props.setProperty("directory", "RAMDirectory"); // no accidental FS dir.
     Config config = new Config(props);
     return new PerfRunData(config);
   }
   
  private void doReadTest(File file, String expTitle,
  private void doReadTest(Path file, String expTitle,
                           String expDate, String expBody) throws Exception {
     doReadTest(2, file, expTitle, expDate, expBody);
    File categoriesFile = WriteEnwikiLineDocTask.categoriesLineFile(file);
    Path categoriesFile = WriteEnwikiLineDocTask.categoriesLineFile(file);
     doReadTest(2, categoriesFile, "Category:"+expTitle, expDate, expBody);
   }
   
  private void doReadTest(int n, File file, String expTitle, String expDate, String expBody) throws Exception {
    InputStream in = new FileInputStream(file);
    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    try {
  private void doReadTest(int n, Path file, String expTitle, String expDate, String expBody) throws Exception {
    try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
       String line = br.readLine();
       WriteLineDocTaskTest.assertHeaderLine(line);
       for (int i=0; i<n; i++) {
@@ -91,8 +91,6 @@ public class WriteEnwikiLineDocTaskTest extends BenchmarkTestCase {
         }
       }
       assertNull(br.readLine());
    } finally {
      br.close();
     }
   }
 
@@ -101,7 +99,7 @@ public class WriteEnwikiLineDocTaskTest extends BenchmarkTestCase {
     // WriteLineDocTask replaced only \t characters w/ a space, since that's its
     // separator char. However, it didn't replace newline characters, which
     // resulted in errors in LineDocSource.
    File file = new File(getWorkDir(), "two-lines-each.txt");
    Path file = getWorkDir().resolve("two-lines-each.txt");
     PerfRunData runData = createPerfRunData(file, WriteLineCategoryDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteEnwikiLineDocTask(runData);
     for (int i=0; i<4; i++) { // four times so that each file should have 2 lines. 
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTaskTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTaskTest.java
index 093d52ec60c..6a5292a02f1 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTaskTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTaskTest.java
@@ -18,11 +18,11 @@ package org.apache.lucene.benchmark.byTask.tasks;
  */
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.HashSet;
 import java.util.Properties;
 import java.util.Set;
@@ -136,12 +136,12 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
 
   private static final CompressorStreamFactory csFactory = new CompressorStreamFactory();
 
  private PerfRunData createPerfRunData(File file, 
  private PerfRunData createPerfRunData(Path file, 
                                         boolean allowEmptyDocs,
                                         String docMakerName) throws Exception {
     Properties props = new Properties();
     props.setProperty("doc.maker", docMakerName);
    props.setProperty("line.file.out", file.getAbsolutePath());
    props.setProperty("line.file.out", file.toAbsolutePath().toString());
     props.setProperty("directory", "RAMDirectory"); // no accidental FS dir.
     if (allowEmptyDocs) {
       props.setProperty("sufficient.fields", ",");
@@ -154,9 +154,9 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
     return new PerfRunData(config);
   }
   
  private void doReadTest(File file, Type fileType, String expTitle,
  private void doReadTest(Path file, Type fileType, String expTitle,
                           String expDate, String expBody) throws Exception {
    InputStream in = new FileInputStream(file);
    InputStream in = Files.newInputStream(file);
     switch(fileType) {
       case BZIP2:
         in = csFactory.createCompressorInputStream(CompressorStreamFactory.BZIP2, in);
@@ -169,8 +169,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
       default:
         assertFalse("Unknown file type!",true); //fail, should not happen
     }
    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    try {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
       String line = br.readLine();
       assertHeaderLine(line);
       line = br.readLine();
@@ -184,8 +183,6 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
         assertEquals(expBody, parts[2]);
       }
       assertNull(br.readLine());
    } finally {
      br.close();
     }
   }
 
@@ -197,7 +194,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
   public void testBZip2() throws Exception {
     
     // Create a document in bz2 format.
    File file = new File(getWorkDir(), "one-line.bz2");
    Path file = getWorkDir().resolve("one-line.bz2");
     PerfRunData runData = createPerfRunData(file, false, WriteLineDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -210,7 +207,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
   public void testGZip() throws Exception {
     
     // Create a document in gz format.
    File file = new File(getWorkDir(), "one-line.gz");
    Path file = getWorkDir().resolve("one-line.gz");
     PerfRunData runData = createPerfRunData(file, false, WriteLineDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -222,7 +219,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
   public void testRegularFile() throws Exception {
     
     // Create a document in regular format.
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, WriteLineDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -235,7 +232,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
     // WriteLineDocTask replaced only \t characters w/ a space, since that's its
     // separator char. However, it didn't replace newline characters, which
     // resulted in errors in LineDocSource.
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, NewLinesDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -248,7 +245,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
     // WriteLineDocTask threw away documents w/ no BODY element, even if they
     // had a TITLE element (LUCENE-1755). It should throw away documents if they
     // don't have BODY nor TITLE
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, NoBodyDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -258,7 +255,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
   }
   
   public void testEmptyTitle() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, NoTitleDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
@@ -269,61 +266,52 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
   
   /** Fail by default when there's only date */
   public void testJustDate() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, JustDateDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
     wldt.close();
     
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    try {
    try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
       String line = br.readLine();
       assertHeaderLine(line);
       line = br.readLine();
       assertNull(line);
    } finally {
      br.close();
     }
   }
 
   public void testLegalJustDate() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, LegalJustDateDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
     wldt.close();
     
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    try {
    try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
       String line = br.readLine();
       assertHeaderLine(line);
       line = br.readLine();
       assertNotNull(line);
    } finally {
      br.close();
     }
   }
 
   public void testEmptyDoc() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, true, EmptyDocMaker.class.getName());
     WriteLineDocTask wldt = new WriteLineDocTask(runData);
     wldt.doLogic();
     wldt.close();
     
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    try {
    try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
       String line = br.readLine();
       assertHeaderLine(line);
       line = br.readLine();
       assertNotNull(line);
    } finally {
      br.close();
     }
   }
 
   public void testMultiThreaded() throws Exception {
    File file = new File(getWorkDir(), "one-line");
    Path file = getWorkDir().resolve("one-line");
     PerfRunData runData = createPerfRunData(file, false, ThreadingDocMaker.class.getName());
     final WriteLineDocTask wldt = new WriteLineDocTask(runData);
     Thread[] threads = new Thread[10];
@@ -346,8 +334,7 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
     wldt.close();
     
     Set<String> ids = new HashSet<>();
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    try {
    try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
       String line = br.readLine();
       assertHeaderLine(line); // header line is written once, no matter how many threads there are
       for (int i = 0; i < threads.length; i++) {
@@ -363,9 +350,6 @@ public class WriteLineDocTaskTest extends BenchmarkTestCase {
       // only threads.length lines should exist
       assertNull(br.readLine());
       assertEquals(threads.length, ids.size());
    } finally {
      br.close();
     }
   }

 }
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/utils/StreamUtilsTest.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/utils/StreamUtilsTest.java
index 63bd31ae6fb..df6bea19407 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/utils/StreamUtilsTest.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/byTask/utils/StreamUtilsTest.java
@@ -19,26 +19,25 @@ package org.apache.lucene.benchmark.byTask.utils;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.commons.compress.compressors.CompressorStreamFactory;
 import org.apache.lucene.benchmark.BenchmarkTestCase;
 import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.TestUtil;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 
 public class StreamUtilsTest extends BenchmarkTestCase {
   private static final String TEXT = "Some-Text..."; 
  private File testDir;
  private Path testDir;
   
   @Test
   public void testGetInputStreamPlainText() throws Exception {
@@ -86,31 +85,31 @@ public class StreamUtilsTest extends BenchmarkTestCase {
     assertReadText(autoOutFile("TEXT"));
   }
   
  private File rawTextFile(String ext) throws Exception {
    File f = new File(testDir,"testfile." +  ext);
    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
  private Path rawTextFile(String ext) throws Exception {
    Path f = testDir.resolve("testfile." +  ext);
    BufferedWriter w = Files.newBufferedWriter(f, StandardCharsets.UTF_8);
     w.write(TEXT);
     w.newLine();
     w.close();
     return f;
   }
   
  private File rawGzipFile(String ext) throws Exception {
    File f = new File(testDir,"testfile." +  ext);
    OutputStream os = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, new FileOutputStream(f));
  private Path rawGzipFile(String ext) throws Exception {
    Path f = testDir.resolve("testfile." +  ext);
    OutputStream os = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, Files.newOutputStream(f));
     writeText(os);
     return f;
   }
 
  private File rawBzip2File(String ext) throws Exception {
    File f = new File(testDir,"testfile." +  ext);
    OutputStream os = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, new FileOutputStream(f));
  private Path rawBzip2File(String ext) throws Exception {
    Path f = testDir.resolve("testfile." +  ext);
    OutputStream os = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, Files.newOutputStream(f));
     writeText(os);
     return f;
   }
 
  private File autoOutFile(String ext) throws Exception {
    File f = new File(testDir,"testfile." +  ext);
  private Path autoOutFile(String ext) throws Exception {
    Path f = testDir.resolve("testfile." +  ext);
     OutputStream os = StreamUtils.outputStream(f);
     writeText(os);
     return f;
@@ -123,12 +122,12 @@ public class StreamUtilsTest extends BenchmarkTestCase {
     w.close();
   }
 
  private void assertReadText(File f) throws Exception {
  private void assertReadText(Path f) throws Exception {
     InputStream ir = StreamUtils.inputStream(f);
     InputStreamReader in = new InputStreamReader(ir, StandardCharsets.UTF_8);
     BufferedReader r = new BufferedReader(in);
     String line = r.readLine();
    assertEquals("Wrong text found in "+f.getName(), TEXT, line);
    assertEquals("Wrong text found in "+f.getFileName(), TEXT, line);
     r.close();
   }
   
@@ -136,9 +135,9 @@ public class StreamUtilsTest extends BenchmarkTestCase {
   @Before
   public void setUp() throws Exception {
     super.setUp();
    testDir = new File(getWorkDir(),"ContentSourceTest");
    testDir = getWorkDir().resolve("ContentSourceTest");
     IOUtils.rm(testDir);
    assertTrue(testDir.mkdirs());
    Files.createDirectory(testDir);
   }
 
   @Override
diff --git a/lucene/benchmark/src/test/org/apache/lucene/benchmark/quality/TestQualityRun.java b/lucene/benchmark/src/test/org/apache/lucene/benchmark/quality/TestQualityRun.java
index 5f101a69cf3..487478bd356 100644
-- a/lucene/benchmark/src/test/org/apache/lucene/benchmark/quality/TestQualityRun.java
++ b/lucene/benchmark/src/test/org/apache/lucene/benchmark/quality/TestQualityRun.java
@@ -26,10 +26,8 @@ import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase.SuppressSysoutChecks;
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
@@ -73,7 +71,7 @@ public class TestQualityRun extends BenchmarkTestCase {
     // validate topics & judgments match each other
     judge.validateData(qqs, logger);
     
    Directory dir = newFSDirectory(new File(getWorkDir(),"index"));
    Directory dir = newFSDirectory(getWorkDir().resolve("index"));
     IndexReader reader = DirectoryReader.open(dir);
     IndexSearcher searcher = new IndexSearcher(reader);
 
diff --git a/lucene/common-build.xml b/lucene/common-build.xml
index 420730d0ef2..25a385c7573 100644
-- a/lucene/common-build.xml
++ b/lucene/common-build.xml
@@ -2356,7 +2356,16 @@ ${ant.project.name}.test.dependencies=${test.classpath.list}
     </path>
   </target>  
 
  <target name="check-forbidden-apis" depends="-check-forbidden-all,-check-forbidden-core,-check-forbidden-tests" description="Check forbidden API calls in compiled class files"/>
  <condition property="islucene" value="true" else="false">
    <not>
      <or>
        <matches pattern="^(solr)\b" string="${name}"/>
        <matches pattern="tools" string="${name}"/>
      </or>
    </not>
  </condition>

  <target name="check-forbidden-apis" depends="-check-forbidden-all,-check-forbidden-core,-check-forbidden-tests,-check-forbidden-lucene" description="Check forbidden API calls in compiled class files"/>
   
   <!-- applies to both source and test code -->
   <target name="-check-forbidden-all" depends="-init-forbidden-apis,compile-core,compile-test">
@@ -2368,6 +2377,14 @@ ${ant.project.name}.test.dependencies=${test.classpath.list}
       <fileset dir="${build.dir}/classes/test" excludes="${forbidden-tests-excludes}" erroronmissingdir="false"/>
     </forbidden-apis>
   </target>

  <!-- applies to only lucene API -->
  <target name="-check-forbidden-lucene" depends="-init-forbidden-apis,compile-core,compile-test" if="${islucene}">
    <forbidden-apis signaturesFile="${common.dir}/tools/forbiddenApis/lucene.txt" classpathref="forbidden-apis.allclasses.classpath"> 
      <fileset dir="${build.dir}/classes/java" excludes="${forbidden-base-excludes}"/>
      <fileset dir="${build.dir}/classes/test" excludes="${forbidden-tests-excludes}" erroronmissingdir="false"/>
    </forbidden-apis>
  </target>
   
   <!-- applies to only test code -->
   <target name="-check-forbidden-tests" depends="-init-forbidden-apis,compile-test">
diff --git a/lucene/core/build.xml b/lucene/core/build.xml
index d14a423d3d2..3ae8f80dad3 100644
-- a/lucene/core/build.xml
++ b/lucene/core/build.xml
@@ -34,6 +34,11 @@
     org/apache/lucene/util/RamUsageEstimator.class
   "/>
 
  <!-- TODO: maybe let people get closedchannel if they cancel(true) -->
  <property name="forbidden-base-excludes" value="
    org/apache/lucene/store/SimpleFSDirectory.class
  "/>

   <import file="../common-build.xml"/>
 
   <property name="moman.commit-hash" value="5c5c2a1e4dea" />
diff --git a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
index dccdd0132b1..cf2f0800eda 100644
-- a/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
++ b/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java
@@ -17,9 +17,10 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.text.NumberFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
@@ -2046,11 +2047,12 @@ public class CheckIndex {
 
     System.out.println("\nOpening index @ " + indexPath + "\n");
     Directory dir = null;
    Path path = Paths.get(indexPath);
     try {
       if (dirImpl == null) {
        dir = FSDirectory.open(new File(indexPath));
        dir = FSDirectory.open(path);
       } else {
        dir = CommandLineUtil.newFSDirectory(dirImpl, new File(indexPath));
        dir = CommandLineUtil.newFSDirectory(dirImpl, path);
       }
     } catch (Throwable t) {
       System.out.println("ERROR: could not open directory \"" + indexPath + "\"; exiting");
diff --git a/lucene/core/src/java/org/apache/lucene/index/DirectoryReader.java b/lucene/core/src/java/org/apache/lucene/index/DirectoryReader.java
index 5b11dc3cd8f..083714e9b48 100644
-- a/lucene/core/src/java/org/apache/lucene/index/DirectoryReader.java
++ b/lucene/core/src/java/org/apache/lucene/index/DirectoryReader.java
@@ -26,7 +26,6 @@ import java.util.List;
 
 import org.apache.lucene.search.SearcherManager; // javadocs
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoSuchDirectoryException;
 
 /** DirectoryReader is an implementation of {@link CompositeReader}
  that can read indexes in a {@link Directory}. 
@@ -288,22 +287,12 @@ public abstract class DirectoryReader extends BaseCompositeReader<AtomicReader>
     // corrupt indices.  This means that IndexWriter will
     // throw an exception on such indices and the app must
     // resolve the situation manually:
    String[] files;
    try {
      files = directory.listAll();
    } catch (NoSuchDirectoryException nsde) {
      // Directory does not exist --> no index exists
      return false;
    }
    String[] files = directory.listAll();
 
    // Defensive: maybe a Directory impl returns null
    // instead of throwing NoSuchDirectoryException:
    if (files != null) {
      String prefix = IndexFileNames.SEGMENTS + "_";
      for(String file : files) {
        if (file.startsWith(prefix)) {
          return true;
        }
    String prefix = IndexFileNames.SEGMENTS + "_";
    for(String file : files) {
      if (file.startsWith(prefix)) {
        return true;
       }
     }
     return false;
diff --git a/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java b/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
index 62bccd24510..bc7768437d9 100644
-- a/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
++ b/lucene/core/src/java/org/apache/lucene/index/IndexFileDeleter.java
@@ -31,7 +31,6 @@ import java.util.regex.Matcher;
 
 import org.apache.lucene.store.AlreadyClosedException;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoSuchDirectoryException;
 import org.apache.lucene.util.CollectionUtil;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.InfoStream;
@@ -144,13 +143,7 @@ final class IndexFileDeleter implements Closeable {
     long currentGen = segmentInfos.getGeneration();
 
     CommitPoint currentCommitPoint = null;
    String[] files = null;
    try {
      files = directory.listAll();
    } catch (NoSuchDirectoryException e) {
      // it means the directory is empty, so ignore it.
      files = new String[0];
    }
    String[] files = directory.listAll();
 
     if (currentSegmentsFile != null) {
       Matcher m = IndexFileNames.CODEC_FILE_PATTERN.matcher("");
diff --git a/lucene/core/src/java/org/apache/lucene/index/IndexUpgrader.java b/lucene/core/src/java/org/apache/lucene/index/IndexUpgrader.java
index 99c93609a12..706b3d44cee 100644
-- a/lucene/core/src/java/org/apache/lucene/index/IndexUpgrader.java
++ b/lucene/core/src/java/org/apache/lucene/index/IndexUpgrader.java
@@ -24,8 +24,9 @@ import org.apache.lucene.util.InfoStream;
 import org.apache.lucene.util.PrintStreamInfoStream;
 import org.apache.lucene.util.Version;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
 import java.util.Collection;
 
 /**
@@ -102,11 +103,12 @@ public final class IndexUpgrader {
       printUsage();
     }
     
    Path p = Paths.get(path);
     Directory dir = null;
     if (dirImpl == null) {
      dir = FSDirectory.open(new File(path));
      dir = FSDirectory.open(p);
     } else {
      dir = CommandLineUtil.newFSDirectory(dirImpl, new File(path));
      dir = CommandLineUtil.newFSDirectory(dirImpl, p);
     }
     return new IndexUpgrader(dir, out, deletePriorCommits);
   }
diff --git a/lucene/core/src/java/org/apache/lucene/index/SegmentInfos.java b/lucene/core/src/java/org/apache/lucene/index/SegmentInfos.java
index e3c32257fde..0e6c82c0013 100644
-- a/lucene/core/src/java/org/apache/lucene/index/SegmentInfos.java
++ b/lucene/core/src/java/org/apache/lucene/index/SegmentInfos.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 
 import java.io.IOException;
 import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
@@ -41,7 +42,6 @@ import org.apache.lucene.store.DataOutput;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.IOContext;
 import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.NoSuchDirectoryException;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.StringHelper;
 
@@ -176,9 +176,6 @@ public final class SegmentInfos implements Cloneable, Iterable<SegmentCommitInfo
    * @param files -- array of file names to check
    */
   public static long getLastCommitGeneration(String[] files) {
    if (files == null) {
      return -1;
    }
     long max = -1;
     for (String file : files) {
       if (file.startsWith(IndexFileNames.SEGMENTS) && !file.equals(IndexFileNames.OLD_SEGMENTS_GEN)) {
@@ -198,11 +195,7 @@ public final class SegmentInfos implements Cloneable, Iterable<SegmentCommitInfo
    * @param directory -- directory to search for the latest segments_N file
    */
   public static long getLastCommitGeneration(Directory directory) throws IOException {
    try {
      return getLastCommitGeneration(directory.listAll());
    } catch (NoSuchDirectoryException nsde) {
      return -1;
    }
    return getLastCommitGeneration(directory.listAll());
   }
 
   /**
diff --git a/lucene/core/src/java/org/apache/lucene/store/Directory.java b/lucene/core/src/java/org/apache/lucene/store/Directory.java
index 7e7e7a53bbf..fd81fc619b8 100644
-- a/lucene/core/src/java/org/apache/lucene/store/Directory.java
++ b/lucene/core/src/java/org/apache/lucene/store/Directory.java
@@ -46,9 +46,7 @@ public abstract class Directory implements Closeable {
   /**
    * Returns an array of strings, one for each file in the directory.
    * 
   * @throws NoSuchDirectoryException if the directory is not prepared for any
   *         write operations (such as {@link #createOutput(String, IOContext)}).
   * @throws IOException in case of other IO errors
   * @throws IOException in case of IO error
    */
   public abstract String[] listAll() throws IOException;
 
diff --git a/lucene/core/src/java/org/apache/lucene/store/FSDirectory.java b/lucene/core/src/java/org/apache/lucene/store/FSDirectory.java
index 7c64686a003..c1365f73254 100644
-- a/lucene/core/src/java/org/apache/lucene/store/FSDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/FSDirectory.java
@@ -20,16 +20,17 @@ package org.apache.lucene.store;
 import org.apache.lucene.util.Constants;
 import org.apache.lucene.util.IOUtils;
 
import java.io.File;
import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
import java.io.FilenameFilter;
 import java.io.FilterOutputStream;
 import java.io.IOException;
import java.nio.file.DirectoryStream;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
import java.util.List;
 import java.util.Set;
 import java.util.concurrent.Future;
 
@@ -114,32 +115,25 @@ import static java.util.Collections.synchronizedSet;
  */
 public abstract class FSDirectory extends BaseDirectory {
 
  protected final File directory; // The underlying filesystem directory
  protected final Path directory; // The underlying filesystem directory
   protected final Set<String> staleFiles = synchronizedSet(new HashSet<String>()); // Files written, but not yet sync'ed
 
  // returns the canonical version of the directory, creating it if it doesn't exist.
  private static File getCanonicalPath(File file) throws IOException {
    return new File(file.getCanonicalPath());
  }

   /** Create a new FSDirectory for the named location (ctor for subclasses).
    * @param path the path of the directory
    * @param lockFactory the lock factory to use, or null for the default
    * ({@link NativeFSLockFactory});
    * @throws IOException if there is a low-level I/O error
    */
  protected FSDirectory(File path, LockFactory lockFactory) throws IOException {
  protected FSDirectory(Path path, LockFactory lockFactory) throws IOException {
     // new ctors use always NativeFSLockFactory as default:
     if (lockFactory == null) {
       lockFactory = new NativeFSLockFactory();
     }
    directory = getCanonicalPath(path);

    if (directory.exists() && !directory.isDirectory())
      throw new NoSuchDirectoryException("file '" + directory + "' exists but is not a directory");
    
    Files.createDirectories(path);  // create directory, if it doesnt exist
    directory = path.toRealPath();
 
     setLockFactory(lockFactory);

   }
 
   /** Creates an FSDirectory instance, trying to pick the
@@ -162,13 +156,13 @@ public abstract class FSDirectory extends BaseDirectory {
    * {@link MMapDirectory} on 64 bit JVMs.
    *
    * <p>See <a href="#subclasses">above</a> */
  public static FSDirectory open(File path) throws IOException {
  public static FSDirectory open(Path path) throws IOException {
     return open(path, null);
   }
 
  /** Just like {@link #open(File)}, but allows you to
  /** Just like {@link #open(Path)}, but allows you to
    *  also specify a custom {@link LockFactory}. */
  public static FSDirectory open(File path, LockFactory lockFactory) throws IOException {
  public static FSDirectory open(Path path, LockFactory lockFactory) throws IOException {
     if (Constants.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
       return new MMapDirectory(path, lockFactory);
     } else if (Constants.WINDOWS) {
@@ -186,12 +180,12 @@ public abstract class FSDirectory extends BaseDirectory {
     // in index dir. If no index dir is given, set ourselves
     if (lockFactory instanceof FSLockFactory) {
       final FSLockFactory lf = (FSLockFactory) lockFactory;
      final File dir = lf.getLockDir();
      final Path dir = lf.getLockDir();
       // if the lock factory has no lockDir set, use the this directory as lockDir
       if (dir == null) {
         lf.setLockDir(directory);
         lf.setLockPrefix(null);
      } else if (dir.getCanonicalPath().equals(directory.getCanonicalPath())) {
      } else if (dir.toRealPath().equals(directory)) {
         lf.setLockPrefix(null);
       }
     }
@@ -199,36 +193,29 @@ public abstract class FSDirectory extends BaseDirectory {
   }
   
   /** Lists all files (not subdirectories) in the
   *  directory.  This method never returns null (throws
   *  {@link IOException} instead).
   *  directory.
    *
   *  @throws NoSuchDirectoryException if the directory
   *   does not exist, or does exist but is not a
   *   directory.
   *  @throws IOException if list() returns null */
  public static String[] listAll(File dir) throws IOException {
    if (!dir.exists())
      throw new NoSuchDirectoryException("directory '" + dir + "' does not exist");
    else if (!dir.isDirectory())
      throw new NoSuchDirectoryException("file '" + dir + "' exists but is not a directory");

    // Exclude subdirs
    String[] result = dir.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String file) {
          return !new File(dir, file).isDirectory();
        }
      });

    if (result == null)
      throw new IOException("directory '" + dir + "' exists and is a directory, but cannot be listed: list() returned null");

    return result;
   *  @throws IOException if there was an I/O error during listing */
  public static String[] listAll(Path dir) throws IOException {
    List<String> entries = new ArrayList<>();
    
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path entry) throws IOException {
        return !Files.isDirectory(entry); // filter out entries that are definitely directories.
      }
    })) {
      for (Path path : stream) {
        entries.add(path.getFileName().toString());
      }
    }
    
    return entries.toArray(new String[entries.size()]);
   }
 
   /** Lists all files (not subdirectories) in the
    * directory.
   * @see #listAll(File) */
   * @see #listAll(Path) */
   @Override
   public String[] listAll() throws IOException {
     ensureOpen();
@@ -239,21 +226,14 @@ public abstract class FSDirectory extends BaseDirectory {
   @Override
   public long fileLength(String name) throws IOException {
     ensureOpen();
    File file = new File(directory, name);
    final long len = file.length();
    if (len == 0 && !file.exists()) {
      throw new FileNotFoundException(name);
    } else {
      return len;
    }
    return Files.size(directory.resolve(name));
   }
 
   /** Removes an existing file in the directory. */
   @Override
   public void deleteFile(String name) throws IOException {
     ensureOpen();
    File file = new File(directory, name);
    Files.delete(file.toPath());
    Files.delete(directory.resolve(name));
     staleFiles.remove(name);
   }
 
@@ -267,12 +247,7 @@ public abstract class FSDirectory extends BaseDirectory {
   }
 
   protected void ensureCanWrite(String name) throws IOException {
    if (!directory.exists())
      if (!directory.mkdirs())
        throw new IOException("Cannot create directory: " + directory);

    File file = new File(directory, name);
    Files.deleteIfExists(file.toPath()); // delete existing, if any
    Files.deleteIfExists(directory.resolve(name)); // delete existing, if any
   }
 
   /**
@@ -299,7 +274,7 @@ public abstract class FSDirectory extends BaseDirectory {
   @Override
   public void renameFile(String source, String dest) throws IOException {
     ensureOpen();
    Files.move(new File(directory, source).toPath(), new File(directory, dest).toPath(), StandardCopyOption.ATOMIC_MOVE);
    Files.move(directory.resolve(source), directory.resolve(dest), StandardCopyOption.ATOMIC_MOVE);
     // TODO: should we move directory fsync to a separate 'syncMetadata' method?
     // for example, to improve listCommits(), IndexFileDeleter could also call that after deleting segments_Ns
     IOUtils.fsync(directory, true);
@@ -308,12 +283,7 @@ public abstract class FSDirectory extends BaseDirectory {
   @Override
   public String getLockID() {
     ensureOpen();
    String dirName;                               // name to be hashed
    try {
      dirName = directory.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e.toString(), e);
    }
    String dirName = directory.toString();  // name to be hashed
 
     int digest = 0;
     for(int charIDX=0;charIDX<dirName.length();charIDX++) {
@@ -330,7 +300,7 @@ public abstract class FSDirectory extends BaseDirectory {
   }
 
   /** @return the underlying filesystem directory */
  public File getDirectory() {
  public Path getDirectory() {
     ensureOpen();
     return directory;
   }
@@ -351,7 +321,7 @@ public abstract class FSDirectory extends BaseDirectory {
     private final String name;
 
     public FSIndexOutput(String name) throws IOException {
      super(new FilterOutputStream(new FileOutputStream(new File(directory, name))) {
      super(new FilterOutputStream(Files.newOutputStream(directory.resolve(name))) {
         // This implementation ensures, that we never write more than CHUNK_SIZE bytes:
         @Override
         public void write(byte[] b, int offset, int length) throws IOException {
@@ -377,6 +347,6 @@ public abstract class FSDirectory extends BaseDirectory {
   }
 
   protected void fsync(String name) throws IOException {
    IOUtils.fsync(new File(directory, name), false);
    IOUtils.fsync(directory.resolve(name), false);
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/store/FSLockFactory.java b/lucene/core/src/java/org/apache/lucene/store/FSLockFactory.java
index dc96fabf230..584a77857de 100644
-- a/lucene/core/src/java/org/apache/lucene/store/FSLockFactory.java
++ b/lucene/core/src/java/org/apache/lucene/store/FSLockFactory.java
@@ -17,7 +17,9 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
 
 /**
  * Base class for file system based locking implementation.
@@ -28,7 +30,7 @@ public abstract class FSLockFactory extends LockFactory {
   /**
    * Directory for the lock files.
    */
  protected File lockDir = null;
  protected Path lockDir = null;
 
   /**
    * Set the lock directory. This method can be only called
@@ -37,16 +39,19 @@ public abstract class FSLockFactory extends LockFactory {
    * Subclasses can also use this method to set the directory
    * in the constructor.
    */
  protected final void setLockDir(File lockDir) {
  protected final void setLockDir(Path lockDir) throws IOException {
     if (this.lockDir != null)
       throw new IllegalStateException("You can set the lock directory for this factory only once.");
    if (lockDir != null) {
      Files.createDirectories(lockDir);
    }
     this.lockDir = lockDir;
   }
   
   /**
    * Retrieve the lock directory.
    */
  public File getLockDir() {
  public Path getLockDir() {
     return lockDir;
   }
 
diff --git a/lucene/core/src/java/org/apache/lucene/store/FileSwitchDirectory.java b/lucene/core/src/java/org/apache/lucene/store/FileSwitchDirectory.java
index 9b13f65076e..db27395034d 100644
-- a/lucene/core/src/java/org/apache/lucene/store/FileSwitchDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/FileSwitchDirectory.java
@@ -19,6 +19,7 @@ package org.apache.lucene.store;
 
 import java.io.IOException;
 import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
 
 import java.util.ArrayList;
 import java.util.Collection;
@@ -82,32 +83,32 @@ public class FileSwitchDirectory extends BaseDirectory {
     // LUCENE-3380: either or both of our dirs could be FSDirs,
     // but if one underlying delegate is an FSDir and mkdirs() has not
     // yet been called, because so far everything is written to the other,
    // in this case, we don't want to throw a NoSuchDirectoryException
    NoSuchDirectoryException exc = null;
    // in this case, we don't want to throw a NoSuchFileException
    NoSuchFileException exc = null;
     try {
       for(String f : primaryDir.listAll()) {
         files.add(f);
       }
    } catch (NoSuchDirectoryException e) {
    } catch (NoSuchFileException e) {
       exc = e;
     }
     try {
       for(String f : secondaryDir.listAll()) {
         files.add(f);
       }
    } catch (NoSuchDirectoryException e) {
      // we got NoSuchDirectoryException from both dirs
    } catch (NoSuchFileException e) {
      // we got NoSuchFileException from both dirs
       // rethrow the first.
       if (exc != null) {
         throw exc;
       }
      // we got NoSuchDirectoryException from the secondary,
      // we got NoSuchFileException from the secondary,
       // and the primary is empty.
       if (files.isEmpty()) {
         throw e;
       }
     }
    // we got NoSuchDirectoryException from the primary,
    // we got NoSuchFileException from the primary,
     // and the secondary is empty.
     if (exc != null && files.isEmpty()) {
       throw exc;
diff --git a/lucene/core/src/java/org/apache/lucene/store/LockStressTest.java b/lucene/core/src/java/org/apache/lucene/store/LockStressTest.java
index bba49c0e634..01c6061441b 100644
-- a/lucene/core/src/java/org/apache/lucene/store/LockStressTest.java
++ b/lucene/core/src/java/org/apache/lucene/store/LockStressTest.java
@@ -18,11 +18,12 @@ package org.apache.lucene.store;
  */
 
 import java.io.IOException;
import java.io.File;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.net.InetSocketAddress;
 import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.Random;
 
 /**
@@ -133,7 +134,7 @@ public class LockStressTest {
       throw new IOException("Cannot instantiate lock factory " + lockFactoryClassName);
     }
 
    File lockDir = new File(lockDirName);
    Path lockDir = Paths.get(lockDirName);
 
     if (lockFactory instanceof FSLockFactory) {
       ((FSLockFactory) lockFactory).setLockDir(lockDir);
diff --git a/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java b/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java
index 27efd2b7691..0cee51565ef 100644
-- a/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java
@@ -18,11 +18,11 @@ package org.apache.lucene.store;
  */
  
 import java.io.IOException;
import java.io.File;
 import java.nio.ByteBuffer;
 import java.nio.channels.ClosedChannelException; // javadoc @link
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
 import java.nio.file.StandardOpenOption;
 
 import java.security.AccessController;
@@ -44,7 +44,7 @@ import org.apache.lucene.util.Constants;
  * be sure your have plenty of virtual address space, e.g. by
  * using a 64 bit JRE, or a 32 bit JRE with indexes that are
  * guaranteed to fit within the address space.
 * On 32 bit platforms also consult {@link #MMapDirectory(File, LockFactory, int)}
 * On 32 bit platforms also consult {@link #MMapDirectory(Path, LockFactory, int)}
  * if you have problems with mmap failing because of fragmented
  * address space. If you get an OutOfMemoryException, it is recommended
  * to reduce the chunk size, until it works.
@@ -83,7 +83,7 @@ public class MMapDirectory extends FSDirectory {
   private boolean useUnmapHack = UNMAP_SUPPORTED;
   /** 
    * Default max chunk size.
   * @see #MMapDirectory(File, LockFactory, int)
   * @see #MMapDirectory(Path, LockFactory, int)
    */
   public static final int DEFAULT_MAX_BUFF = Constants.JRE_IS_64BIT ? (1 << 30) : (1 << 28);
   final int chunkSizePower;
@@ -95,7 +95,7 @@ public class MMapDirectory extends FSDirectory {
    * ({@link NativeFSLockFactory});
    * @throws IOException if there is a low-level I/O error
    */
  public MMapDirectory(File path, LockFactory lockFactory) throws IOException {
  public MMapDirectory(Path path, LockFactory lockFactory) throws IOException {
     this(path, lockFactory, DEFAULT_MAX_BUFF);
   }
 
@@ -104,7 +104,7 @@ public class MMapDirectory extends FSDirectory {
    * @param path the path of the directory
    * @throws IOException if there is a low-level I/O error
    */
  public MMapDirectory(File path) throws IOException {
  public MMapDirectory(Path path) throws IOException {
     this(path, null);
   }
   
@@ -128,7 +128,7 @@ public class MMapDirectory extends FSDirectory {
    * <b>Please note:</b> The chunk size is always rounded down to a power of 2.
    * @throws IOException if there is a low-level I/O error
    */
  public MMapDirectory(File path, LockFactory lockFactory, int maxChunkSize) throws IOException {
  public MMapDirectory(Path path, LockFactory lockFactory, int maxChunkSize) throws IOException {
     super(path, lockFactory);
     if (maxChunkSize <= 0) {
       throw new IllegalArgumentException("Maximum chunk size for mmap must be >0");
@@ -182,7 +182,7 @@ public class MMapDirectory extends FSDirectory {
   
   /**
    * Returns the current mmap chunk size.
   * @see #MMapDirectory(File, LockFactory, int)
   * @see #MMapDirectory(Path, LockFactory, int)
    */
   public final int getMaxChunkSize() {
     return 1 << chunkSizePower;
@@ -192,9 +192,9 @@ public class MMapDirectory extends FSDirectory {
   @Override
   public IndexInput openInput(String name, IOContext context) throws IOException {
     ensureOpen();
    File file = new File(getDirectory(), name);
    try (FileChannel c = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
      final String resourceDescription = "MMapIndexInput(path=\"" + file.toString() + "\")";
    Path path = directory.resolve(name);
    try (FileChannel c = FileChannel.open(path, StandardOpenOption.READ)) {
      final String resourceDescription = "MMapIndexInput(path=\"" + path.toString() + "\")";
       final boolean useUnmap = getUseUnmap();
       return ByteBufferIndexInput.newInstance(resourceDescription,
           map(resourceDescription, c, 0, c.size()), 
diff --git a/lucene/core/src/java/org/apache/lucene/store/NIOFSDirectory.java b/lucene/core/src/java/org/apache/lucene/store/NIOFSDirectory.java
index 54202c591e0..a5d784b2e1a 100644
-- a/lucene/core/src/java/org/apache/lucene/store/NIOFSDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/NIOFSDirectory.java
@@ -17,12 +17,12 @@ package org.apache.lucene.store;
  * the License.
  */
 
import java.io.File;
 import java.io.EOFException;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.channels.ClosedChannelException; // javadoc @link
 import java.nio.channels.FileChannel;
import java.nio.file.Path;
 import java.nio.file.StandardOpenOption;
 import java.util.concurrent.Future; // javadoc
 
@@ -60,7 +60,7 @@ public class NIOFSDirectory extends FSDirectory {
    * ({@link NativeFSLockFactory});
    * @throws IOException if there is a low-level I/O error
    */
  public NIOFSDirectory(File path, LockFactory lockFactory) throws IOException {
  public NIOFSDirectory(Path path, LockFactory lockFactory) throws IOException {
     super(path, lockFactory);
   }
 
@@ -69,7 +69,7 @@ public class NIOFSDirectory extends FSDirectory {
    * @param path the path of the directory
    * @throws IOException if there is a low-level I/O error
    */
  public NIOFSDirectory(File path) throws IOException {
  public NIOFSDirectory(Path path) throws IOException {
     super(path, null);
   }
 
@@ -77,8 +77,8 @@ public class NIOFSDirectory extends FSDirectory {
   @Override
   public IndexInput openInput(String name, IOContext context) throws IOException {
     ensureOpen();
    File path = new File(getDirectory(), name);
    FileChannel fc = FileChannel.open(path.toPath(), StandardOpenOption.READ);
    Path path = getDirectory().resolve(name);
    FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
     return new NIOFSIndexInput("NIOFSIndexInput(path=\"" + path + "\")", fc, context);
   }
   
diff --git a/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java b/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
index 351445686a8..c55d5aea3c0 100644
-- a/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/NRTCachingDirectory.java
@@ -18,11 +18,11 @@ package org.apache.lucene.store;
  */
 
 import java.io.IOException;
import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Set;
 
import org.apache.lucene.index.IndexFileNames;
 import org.apache.lucene.store.RAMDirectory;      // javadocs
 import org.apache.lucene.util.Accountable;
 import org.apache.lucene.util.IOUtils;
@@ -49,7 +49,7 @@ import org.apache.lucene.util.IOUtils;
  * <p>Here's a simple example usage:
  *
  * <pre class="prettyprint">
 *   Directory fsDir = FSDirectory.open(new File("/path/to/index"));
 *   Directory fsDir = FSDirectory.open(new File("/path/to/index").toPath());
  *   NRTCachingDirectory cachedFSDir = new NRTCachingDirectory(fsDir, 5.0, 60.0);
  *   IndexWriterConfig conf = new IndexWriterConfig(analyzer);
  *   IndexWriter writer = new IndexWriter(cachedFSDir, conf);
@@ -96,22 +96,10 @@ public class NRTCachingDirectory extends FilterDirectory implements Accountable
     for(String f : cache.listAll()) {
       files.add(f);
     }
    // LUCENE-1468: our NRTCachingDirectory will actually exist (RAMDir!),
    // but if the underlying delegate is an FSDir and mkdirs() has not
    // yet been called, because so far everything is a cached write,
    // in this case, we don't want to throw a NoSuchDirectoryException
    try {
      for(String f : in.listAll()) {
        // Cannot do this -- if lucene calls createOutput but
        // file already exists then this falsely trips:
        //assert !files.contains(f): "file \"" + f + "\" is in both dirs";
        files.add(f);
      }
    } catch (NoSuchDirectoryException ex) {
      // however, if there are no cached files, then the directory truly
      // does not "exist"
      if (files.isEmpty()) {
        throw ex;
    for(String f : in.listAll()) {
      if (!files.add(f)) {
        throw new IllegalStateException("file: " + in + " appears both in delegate and in cache: " +
                                        "cache=" + Arrays.toString(cache.listAll()) + ",delegate=" + Arrays.toString(in.listAll()));
       }
     }
     return files.toArray(new String[files.size()]);
diff --git a/lucene/core/src/java/org/apache/lucene/store/NativeFSLockFactory.java b/lucene/core/src/java/org/apache/lucene/store/NativeFSLockFactory.java
index b772aca9d61..ee1802e1d71 100644
-- a/lucene/core/src/java/org/apache/lucene/store/NativeFSLockFactory.java
++ b/lucene/core/src/java/org/apache/lucene/store/NativeFSLockFactory.java
@@ -20,6 +20,8 @@ package org.apache.lucene.store;
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileLock;
 import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.nio.file.StandardOpenOption;
 import java.io.File;
 import java.io.IOException;
@@ -77,18 +79,8 @@ public class NativeFSLockFactory extends FSLockFactory {
    * directory itself. Be sure to create one instance for each directory
    * your create!
    */
  public NativeFSLockFactory() {
    this((File) null);
  }

  /**
   * Create a NativeFSLockFactory instance, storing lock
   * files into the specified lockDirName:
   *
   * @param lockDirName where lock files are created.
   */
  public NativeFSLockFactory(String lockDirName) {
    this(new File(lockDirName));
  public NativeFSLockFactory() throws IOException {
    this((Path) null);
   }
 
   /**
@@ -97,7 +89,7 @@ public class NativeFSLockFactory extends FSLockFactory {
    * 
    * @param lockDir where lock files are created.
    */
  public NativeFSLockFactory(File lockDir) {
  public NativeFSLockFactory(Path lockDir) throws IOException {
     setLockDir(lockDir);
   }
 
@@ -118,14 +110,14 @@ class NativeFSLock extends Lock {
 
   private FileChannel channel;
   private FileLock lock;
  private File path;
  private File lockDir;
  private Path path;
  private Path lockDir;
   private static final Set<String> LOCK_HELD = Collections.synchronizedSet(new HashSet<String>());
 
 
  public NativeFSLock(File lockDir, String lockFileName) {
  public NativeFSLock(Path lockDir, String lockFileName) {
     this.lockDir = lockDir;
    path = new File(lockDir, lockFileName);
    path = lockDir.resolve(lockFileName);
   }
 
 
@@ -138,16 +130,14 @@ class NativeFSLock extends Lock {
     }
 
     // Ensure that lockDir exists and is a directory.
    if (!lockDir.exists()) {
      if (!lockDir.mkdirs())
        throw new IOException("Cannot create directory: " +
            lockDir.getAbsolutePath());
    } else if (!lockDir.isDirectory()) {
      // TODO: NoSuchDirectoryException instead?
      throw new IOException("Found regular file where directory expected: " + 
          lockDir.getAbsolutePath());
    Files.createDirectories(lockDir);
    try {
      Files.createFile(path);
    } catch (IOException ignore) {
      // we must create the file to have a truly canonical path.
      // if its already created, we don't care. if it cant be created, it will fail below.
     }
    final String canonicalPath = path.getCanonicalPath();
    final Path canonicalPath = path.toRealPath();
     // Make sure nobody else in-process has this lock held
     // already, and, mark it held if not:
     // This is a pretty crazy workaround for some documented
@@ -162,9 +152,9 @@ class NativeFSLock extends Lock {
     // is that we can't re-obtain the lock in the same JVM but from a different process if that happens. Nevertheless
     // this is super trappy. See LUCENE-5738
     boolean obtained = false;
    if (LOCK_HELD.add(canonicalPath)) {
    if (LOCK_HELD.add(canonicalPath.toString())) {
       try {
        channel = FileChannel.open(path.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         try {
           lock = channel.tryLock();
           obtained = lock != null;
@@ -209,8 +199,9 @@ class NativeFSLock extends Lock {
     }
   }
 
  private static final void clearLockHeld(File path) throws IOException {
    boolean remove = LOCK_HELD.remove(path.getCanonicalPath());
  private static final void clearLockHeld(Path path) throws IOException {
    path = path.toRealPath();
    boolean remove = LOCK_HELD.remove(path.toString());
     assert remove : "Lock was cleared but never marked as held";
   }
 
@@ -221,8 +212,8 @@ class NativeFSLock extends Lock {
     // First a shortcut, if a lock reference in this instance is available
     if (lock != null) return true;
     
    // Look if lock file is present; if not, there can definitely be no lock!
    if (!path.exists()) return false;
    // Look if lock file is definitely not present; if not, there can definitely be no lock!
    if (Files.notExists(path)) return false;
     
     // Try to obtain and release (if was locked) the lock
     try {
diff --git a/lucene/core/src/java/org/apache/lucene/store/NoSuchDirectoryException.java b/lucene/core/src/java/org/apache/lucene/store/NoSuchDirectoryException.java
deleted file mode 100644
index b9879cb0b8c..00000000000
-- a/lucene/core/src/java/org/apache/lucene/store/NoSuchDirectoryException.java
++ /dev/null
@@ -1,31 +0,0 @@
package org.apache.lucene.store;

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

import java.io.FileNotFoundException;

/**
 * This exception is thrown when you try to list a
 * non-existent directory.
 */

public class NoSuchDirectoryException extends FileNotFoundException {
  public NoSuchDirectoryException(String message) {
    super(message);
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/store/SimpleFSDirectory.java b/lucene/core/src/java/org/apache/lucene/store/SimpleFSDirectory.java
index 0f6e5444646..a2dd07caaf9 100644
-- a/lucene/core/src/java/org/apache/lucene/store/SimpleFSDirectory.java
++ b/lucene/core/src/java/org/apache/lucene/store/SimpleFSDirectory.java
@@ -21,6 +21,7 @@ import java.io.EOFException;
 import java.io.File;
 import java.io.IOException;
 import java.io.RandomAccessFile;
import java.nio.file.Path;
 
 /** A straightforward implementation of {@link FSDirectory}
  *  using java.io.RandomAccessFile.  However, this class has
@@ -28,6 +29,8 @@ import java.io.RandomAccessFile;
  *  bottleneck) as it synchronizes when multiple threads
  *  read from the same file.  It's usually better to use
  *  {@link NIOFSDirectory} or {@link MMapDirectory} instead. */
// TODO: we currently mandate .toFile to still use RandomAccessFile, to avoid ClosedByInterruptException.
// should we change to SeekableByteChannel instead?
 public class SimpleFSDirectory extends FSDirectory {
     
   /** Create a new SimpleFSDirectory for the named location.
@@ -37,8 +40,9 @@ public class SimpleFSDirectory extends FSDirectory {
    * ({@link NativeFSLockFactory});
    * @throws IOException if there is a low-level I/O error
    */
  public SimpleFSDirectory(File path, LockFactory lockFactory) throws IOException {
  public SimpleFSDirectory(Path path, LockFactory lockFactory) throws IOException {
     super(path, lockFactory);
    path.toFile(); // throw exception if we can't get a File for now
   }
   
   /** Create a new SimpleFSDirectory for the named location and {@link NativeFSLockFactory}.
@@ -46,15 +50,16 @@ public class SimpleFSDirectory extends FSDirectory {
    * @param path the path of the directory
    * @throws IOException if there is a low-level I/O error
    */
  public SimpleFSDirectory(File path) throws IOException {
  public SimpleFSDirectory(Path path) throws IOException {
     super(path, null);
    path.toFile(); // throw exception if we can't get a File for now
   }
 
   /** Creates an IndexInput for the file with the given name. */
   @Override
   public IndexInput openInput(String name, IOContext context) throws IOException {
     ensureOpen();
    final File path = new File(directory, name);
    final File path = directory.resolve(name).toFile();
     RandomAccessFile raf = new RandomAccessFile(path, "r");
     return new SimpleFSIndexInput("SimpleFSIndexInput(path=\"" + path.getPath() + "\")", raf, context);
   }
diff --git a/lucene/core/src/java/org/apache/lucene/store/SimpleFSLockFactory.java b/lucene/core/src/java/org/apache/lucene/store/SimpleFSLockFactory.java
index 1022b92c11a..3dbec93493a 100644
-- a/lucene/core/src/java/org/apache/lucene/store/SimpleFSLockFactory.java
++ b/lucene/core/src/java/org/apache/lucene/store/SimpleFSLockFactory.java
@@ -20,10 +20,11 @@ package org.apache.lucene.store;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 
 /**
  * <p>Implements {@link LockFactory} using {@link
 * File#createNewFile()}.</p>
 * Files#createFile}.</p>
  *
  * <p><b>NOTE:</b> the {@linkplain File#createNewFile() javadocs
  * for <code>File.createNewFile()</code>} contain a vague
@@ -68,26 +69,18 @@ public class SimpleFSLockFactory extends FSLockFactory {
    * directory itself. Be sure to create one instance for each directory
    * your create!
    */
  public SimpleFSLockFactory() {
    this((File) null);
  public SimpleFSLockFactory() throws IOException {
    this((Path) null);
   }
 
   /**
   * Instantiate using the provided directory (as a File instance).
   * Instantiate using the provided directory (as a Path instance).
    * @param lockDir where lock files should be created.
    */
  public SimpleFSLockFactory(File lockDir) {
  public SimpleFSLockFactory(Path lockDir) throws IOException {
     setLockDir(lockDir);
   }
 
  /**
   * Instantiate using the provided directory name (String).
   * @param lockDirName where lock files should be created.
   */
  public SimpleFSLockFactory(String lockDirName) {
    setLockDir(new File(lockDirName));
  }

   @Override
   public Lock makeLock(String lockName) {
     if (lockPrefix != null) {
@@ -98,42 +91,29 @@ public class SimpleFSLockFactory extends FSLockFactory {
 
   @Override
   public void clearLock(String lockName) throws IOException {
    if (lockDir.exists()) {
      if (lockPrefix != null) {
        lockName = lockPrefix + "-" + lockName;
      }
      File lockFile = new File(lockDir, lockName);
      Files.deleteIfExists(lockFile.toPath());
    if (lockPrefix != null) {
      lockName = lockPrefix + "-" + lockName;
     }
    Files.deleteIfExists(lockDir.resolve(lockName));
   }
 }
 
 class SimpleFSLock extends Lock {
 
  File lockFile;
  File lockDir;
  Path lockFile;
  Path lockDir;
 
  public SimpleFSLock(File lockDir, String lockFileName) {
  public SimpleFSLock(Path lockDir, String lockFileName) {
     this.lockDir = lockDir;
    lockFile = new File(lockDir, lockFileName);
    lockFile = lockDir.resolve(lockFileName);
   }
 
   @Override
   public boolean obtain() throws IOException {

    // Ensure that lockDir exists and is a directory:
    if (!lockDir.exists()) {
      if (!lockDir.mkdirs())
        throw new IOException("Cannot create directory: " +
                              lockDir.getAbsolutePath());
    } else if (!lockDir.isDirectory()) {
      // TODO: NoSuchDirectoryException instead?
      throw new IOException("Found regular file where directory expected: " + 
                            lockDir.getAbsolutePath());
    }
    
     try {
      return lockFile.createNewFile();
      Files.createDirectories(lockDir);
      Files.createFile(lockFile);
      return true;
     } catch (IOException ioe) {
       // On Windows, on concurrent createNewFile, the 2nd process gets "access denied".
       // In that case, the lock was not aquired successfully, so return false.
@@ -148,7 +128,7 @@ class SimpleFSLock extends Lock {
   public void close() throws LockReleaseFailedException {
     // TODO: wierd that clearLock() throws the raw IOException...
     try {
      Files.deleteIfExists(lockFile.toPath());
      Files.deleteIfExists(lockFile);
     } catch (Throwable cause) {
       throw new LockReleaseFailedException("failed to delete " + lockFile, cause);
     }
@@ -156,7 +136,7 @@ class SimpleFSLock extends Lock {
 
   @Override
   public boolean isLocked() {
    return lockFile.exists();
    return Files.exists(lockFile);
   }
 
   @Override
diff --git a/lucene/core/src/java/org/apache/lucene/util/CommandLineUtil.java b/lucene/core/src/java/org/apache/lucene/util/CommandLineUtil.java
index c8e5ef55e29..b33b71831a4 100644
-- a/lucene/core/src/java/org/apache/lucene/util/CommandLineUtil.java
++ b/lucene/core/src/java/org/apache/lucene/util/CommandLineUtil.java
@@ -17,9 +17,9 @@ package org.apache.lucene.util;
  * limitations under the License.
  */
 
import java.io.File;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
 
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
@@ -37,13 +37,13 @@ public final class CommandLineUtil {
   /**
    * Creates a specific FSDirectory instance starting from its class name
    * @param clazzName The name of the FSDirectory class to load
   * @param file The file to be used as parameter constructor
   * @param path The path to be used as parameter constructor
    * @return the new FSDirectory instance
    */
  public static FSDirectory newFSDirectory(String clazzName, File file) {
  public static FSDirectory newFSDirectory(String clazzName, Path path) {
     try {
       final Class<? extends FSDirectory> clazz = loadFSDirectoryClass(clazzName);
      return newFSDirectory(clazz, file);
      return newFSDirectory(clazz, path);
     } catch (ClassNotFoundException e) {
       throw new IllegalArgumentException(FSDirectory.class.getSimpleName()
           + " implementation not found: " + clazzName, e);
@@ -52,7 +52,7 @@ public final class CommandLineUtil {
           + " implementation", e);
     } catch (NoSuchMethodException e) {
       throw new IllegalArgumentException(clazzName + " constructor with "
          + File.class.getSimpleName() + " as parameter not found", e);
          + Path.class.getSimpleName() + " as parameter not found", e);
     } catch (Exception e) {
       throw new IllegalArgumentException("Error creating " + clazzName + " instance", e);
     }
@@ -95,18 +95,18 @@ public final class CommandLineUtil {
   /**
    * Creates a new specific FSDirectory instance
    * @param clazz The class of the object to be created
   * @param file The file to be used as parameter constructor
   * @param path The file to be used as parameter constructor
    * @return The new FSDirectory instance
   * @throws NoSuchMethodException If the Directory does not have a constructor that takes <code>File</code>.
   * @throws NoSuchMethodException If the Directory does not have a constructor that takes <code>Path</code>.
    * @throws InstantiationException If the class is abstract or an interface.
    * @throws IllegalAccessException If the constructor does not have public visibility.
    * @throws InvocationTargetException If the constructor throws an exception
    */
  public static FSDirectory newFSDirectory(Class<? extends FSDirectory> clazz, File file) 
  public static FSDirectory newFSDirectory(Class<? extends FSDirectory> clazz, Path path) 
       throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    // Assuming every FSDirectory has a ctor(File):
    Constructor<? extends FSDirectory> ctor = clazz.getConstructor(File.class);
    return ctor.newInstance(file);
    // Assuming every FSDirectory has a ctor(Path):
    Constructor<? extends FSDirectory> ctor = clazz.getConstructor(Path.class);
    return ctor.newInstance(path);
   }
   
 }
diff --git a/lucene/core/src/java/org/apache/lucene/util/IOUtils.java b/lucene/core/src/java/org/apache/lucene/util/IOUtils.java
index 42e923f1895..d774eaa11ec 100644
-- a/lucene/core/src/java/org/apache/lucene/util/IOUtils.java
++ b/lucene/core/src/java/org/apache/lucene/util/IOUtils.java
@@ -21,9 +21,6 @@ import org.apache.lucene.store.Directory;
 
 import java.io.BufferedReader;
 import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
@@ -33,8 +30,12 @@ import java.nio.charset.Charset;
 import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CodingErrorAction;
 import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
 import java.util.Arrays;
 import java.util.LinkedHashMap;
 import java.util.Map;
@@ -154,34 +155,6 @@ public final class IOUtils {
         .onUnmappableCharacter(CodingErrorAction.REPORT);
     return new BufferedReader(new InputStreamReader(stream, charSetDecoder));
   }
  
  /**
   * Opens a Reader for the given {@link File} using a {@link CharsetDecoder}.
   * Unlike Java's defaults this reader will throw an exception if your it detects 
   * the read charset doesn't match the expected {@link Charset}. 
   * <p>
   * Decoding readers are useful to load configuration files, stopword lists or synonym files
   * to detect character set problems. However, its not recommended to use as a common purpose 
   * reader.
   * @param file the file to open a reader on
   * @param charSet the expected charset
   * @return a reader to read the given file
   */
  public static Reader getDecodingReader(File file, Charset charSet) throws IOException {
    FileInputStream stream = null;
    boolean success = false;
    try {
      stream = new FileInputStream(file);
      final Reader reader = getDecodingReader(stream, charSet);
      success = true;
      return reader;

    } finally {
      if (!success) {
        IOUtils.close(stream);
      }
    }
  }
 
   /**
    * Opens a Reader for the given resource using a {@link CharsetDecoder}.
@@ -233,7 +206,7 @@ public final class IOUtils {
    * <p>
    * Some of the files may be null, if so they are ignored.
    */
  public static void deleteFilesIgnoringExceptions(File... files) {
  public static void deleteFilesIgnoringExceptions(Path... files) {
     deleteFilesIgnoringExceptions(Arrays.asList(files));
   }
   
@@ -242,11 +215,11 @@ public final class IOUtils {
    * <p>
    * Some of the files may be null, if so they are ignored.
    */
  public static void deleteFilesIgnoringExceptions(Iterable<? extends File> files) {
    for (File name : files) {
  public static void deleteFilesIgnoringExceptions(Iterable<? extends Path> files) {
    for (Path name : files) {
       if (name != null) {
         try {
          Files.delete(name.toPath());
          Files.delete(name);
         } catch (Throwable ignored) {
           // ignore
         }
@@ -255,7 +228,7 @@ public final class IOUtils {
   }
   
   /**
   * Deletes all given <tt>File</tt>s, if they exist.  Some of the
   * Deletes all given <tt>Path</tt>s, if they exist.  Some of the
    * <tt>File</tt>s may be null; they are
    * ignored.  After everything is deleted, the method either
    * throws the first exception it hit while deleting, or
@@ -263,12 +236,12 @@ public final class IOUtils {
    * 
    * @param files files to delete
    */
  public static void deleteFilesIfExist(File... files) throws IOException {
  public static void deleteFilesIfExist(Path... files) throws IOException {
     deleteFilesIfExist(Arrays.asList(files));
   }
   
   /**
   * Deletes all given <tt>File</tt>s, if they exist.  Some of the
   * Deletes all given <tt>Path</tt>s, if they exist.  Some of the
    * <tt>File</tt>s may be null; they are
    * ignored.  After everything is deleted, the method either
    * throws the first exception it hit while deleting, or
@@ -276,13 +249,13 @@ public final class IOUtils {
    * 
    * @param files files to delete
    */
  public static void deleteFilesIfExist(Iterable<? extends File> files) throws IOException {
  public static void deleteFilesIfExist(Iterable<? extends Path> files) throws IOException {
     Throwable th = null;
 
    for (File file : files) {
    for (Path file : files) {
       try {
         if (file != null) {
          Files.deleteIfExists(file.toPath());
          Files.deleteIfExists(file);
         }
       } catch (Throwable t) {
         addSuppressed(th, t);
@@ -301,13 +274,13 @@ public final class IOUtils {
    * @throws IOException if any of the given files (or their subhierarchy files in case
    * of directories) cannot be removed.
    */
  public static void rm(File... locations) throws IOException {
    LinkedHashMap<File,Throwable> unremoved = rm(new LinkedHashMap<File,Throwable>(), locations);
  public static void rm(Path... locations) throws IOException {
    LinkedHashMap<Path,Throwable> unremoved = rm(new LinkedHashMap<Path,Throwable>(), locations);
     if (!unremoved.isEmpty()) {
       StringBuilder b = new StringBuilder("Could not remove the following files (in the order of attempts):\n");
      for (Map.Entry<File,Throwable> kv : unremoved.entrySet()) {
      for (Map.Entry<Path,Throwable> kv : unremoved.entrySet()) {
         b.append("   ")
         .append(kv.getKey().getAbsolutePath())
         .append(kv.getKey().toAbsolutePath())
          .append(": ")
          .append(kv.getValue())
          .append("\n");
@@ -316,18 +289,50 @@ public final class IOUtils {
     }
   }
 
  private static LinkedHashMap<File,Throwable> rm(LinkedHashMap<File,Throwable> unremoved, File... locations) {
  private static LinkedHashMap<Path,Throwable> rm(final LinkedHashMap<Path,Throwable> unremoved, Path... locations) {
     if (locations != null) {
      for (File location : locations) {
        if (location != null && location.exists()) {
          if (location.isDirectory()) {
            rm(unremoved, location.listFiles());
          }
  
      for (Path location : locations) {
        // TODO: remove this leniency!
        if (location != null && Files.exists(location)) {
           try {
            Files.delete(location.toPath());
          } catch (Throwable cause) {
            unremoved.put(location, cause);
            Files.walkFileTree(location, new FileVisitor<Path>() {            
              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
              }
              
              @Override
              public FileVisitResult postVisitDirectory(Path dir, IOException impossible) throws IOException {
                assert impossible == null;
                
                try {
                  Files.delete(dir);
                } catch (IOException e) {
                  unremoved.put(dir, e);
                }
                return FileVisitResult.CONTINUE;
              }
              
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                  Files.delete(file);
                } catch (IOException exc) {
                  unremoved.put(file, exc);
                }
                return FileVisitResult.CONTINUE;
              }
              
              @Override
              public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (exc != null) {
                  unremoved.put(file, exc);
                }
                return FileVisitResult.CONTINUE;
              }
            });
          } catch (IOException impossible) {
            throw new AssertionError("visitor threw exception", impossible);
           }
         }
       }
@@ -335,27 +340,6 @@ public final class IOUtils {
     return unremoved;
   }
 
  /**
   * Copy one file's contents to another file. The target will be overwritten
   * if it exists. The source must exist.
   */
  public static void copy(File source, File target) throws IOException {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try {
      fis = new FileInputStream(source);
      fos = new FileOutputStream(target);
      
      final byte [] buffer = new byte [1024 * 8];
      int len;
      while ((len = fis.read(buffer)) > 0) {
        fos.write(buffer, 0, len);
      }
    } finally {
      close(fis, fos);
    }
  }

   /**
    * Simple utilty method that takes a previously caught
    * {@code Throwable} and rethrows either {@code
@@ -394,12 +378,12 @@ public final class IOUtils {
    * @param isDir if true, the given file is a directory (we open for read and ignore IOExceptions,
    *  because not all file systems and operating systems allow to fsync on a directory)
    */
  public static void fsync(File fileToSync, boolean isDir) throws IOException {
  public static void fsync(Path fileToSync, boolean isDir) throws IOException {
     IOException exc = null;
     
     // If the file is a directory we have to open read-only, for regular files we must open r/w for the fsync to have an effect.
     // See http://blog.httrack.com/blog/2013/11/15/everything-you-always-wanted-to-know-about-fsync/
    try (final FileChannel file = FileChannel.open(fileToSync.toPath(), isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
    try (final FileChannel file = FileChannel.open(fileToSync, isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
       for (int retry = 0; retry < 5; retry++) {
         try {
           file.force(true);
diff --git a/lucene/core/src/java/org/apache/lucene/util/OfflineSorter.java b/lucene/core/src/java/org/apache/lucene/util/OfflineSorter.java
index 441b6027566..36258084fb1 100644
-- a/lucene/core/src/java/org/apache/lucene/util/OfflineSorter.java
++ b/lucene/core/src/java/org/apache/lucene/util/OfflineSorter.java
@@ -25,13 +25,11 @@ import java.io.DataInputStream;
 import java.io.DataOutput;
 import java.io.DataOutputStream;
 import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
 import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
@@ -45,7 +43,7 @@ import java.util.Locale;
  *   <li>exactly the above count of bytes for the sequence to be sorted.
  * </ul>
  * 
 * @see #sort(File, File)
 * @see #sort(Path, Path)
  * @lucene.experimental
  * @lucene.internal
  */
@@ -167,7 +165,7 @@ public final class OfflineSorter {
   }
 
   private final BufferSize ramBufferSize;
  private final File tempDirectory;
  private final Path tempDirectory;
   
   private final Counter bufferBytesUsed = Counter.newCounter();
   private final BytesRefArray buffer = new BytesRefArray(bufferBytesUsed);
@@ -201,7 +199,7 @@ public final class OfflineSorter {
   /**
    * All-details constructor.
    */
  public OfflineSorter(Comparator<BytesRef> comparator, BufferSize ramBufferSize, File tempDirectory, int maxTempfiles) {
  public OfflineSorter(Comparator<BytesRef> comparator, BufferSize ramBufferSize, Path tempDirectory, int maxTempfiles) {
     if (ramBufferSize.bytes < ABSOLUTE_MIN_SORT_BUFFER_SIZE) {
       throw new IllegalArgumentException(MIN_BUFFER_SIZE_MSG + ": " + ramBufferSize.bytes);
     }
@@ -220,13 +218,13 @@ public final class OfflineSorter {
    * Sort input to output, explicit hint for the buffer size. The amount of allocated
    * memory may deviate from the hint (may be smaller or larger).  
    */
  public SortInfo sort(File input, File output) throws IOException {
  public SortInfo sort(Path input, Path output) throws IOException {
     sortInfo = new SortInfo();
     sortInfo.totalTime = System.currentTimeMillis();
 
    Files.deleteIfExists(output.toPath());
    Files.deleteIfExists(output);
 
    ArrayList<File> merges = new ArrayList<>();
    ArrayList<Path> merges = new ArrayList<>();
     boolean success3 = false;
     try {
       ByteSequencesReader is = new ByteSequencesReader(input);
@@ -240,7 +238,7 @@ public final class OfflineSorter {
 
           // Handle intermediate merges.
           if (merges.size() == maxTempFiles) {
            File intermediate = File.createTempFile("sort", "intermediate", tempDirectory);
            Path intermediate = Files.createTempFile(tempDirectory, "sort", "intermediate");
             boolean success2 = false;
             try {
               mergePartitions(merges, intermediate);
@@ -267,11 +265,13 @@ public final class OfflineSorter {
 
       // One partition, try to rename or copy if unsuccessful.
       if (merges.size() == 1) {     
        File single = merges.get(0);
        Path single = merges.get(0);
         // If simple rename doesn't work this means the output is
         // on a different volume or something. Copy the input then.
        if (!single.renameTo(output)) {
          copy(single, output);
        try {
          Files.move(single, output, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | UnsupportedOperationException e) {
          Files.copy(single, output);
         }
       } else { 
         // otherwise merge the partitions with a priority queue.
@@ -295,43 +295,23 @@ public final class OfflineSorter {
    * Returns the default temporary directory. By default, java.io.tmpdir. If not accessible
    * or not available, an IOException is thrown
    */
  public static File defaultTempDir() throws IOException {
  public static Path defaultTempDir() throws IOException {
     String tempDirPath = System.getProperty("java.io.tmpdir");
     if (tempDirPath == null) 
       throw new IOException("Java has no temporary folder property (java.io.tmpdir)?");
 
    File tempDirectory = new File(tempDirPath);
    if (!tempDirectory.exists() || !tempDirectory.canWrite()) {
    Path tempDirectory = Paths.get(tempDirPath);
    if (!Files.isWritable(tempDirectory)) {
       throw new IOException("Java's temporary folder not present or writeable?: " 
          + tempDirectory.getAbsolutePath());
          + tempDirectory.toAbsolutePath());
     }
     return tempDirectory;
   }
 
  /**
   * Copies one file to another.
   */
  private static void copy(File file, File output) throws IOException {
    // 64kb copy buffer (empirical pick).
    byte [] buffer = new byte [16 * 1024];
    InputStream is = null;
    OutputStream os = null;
    try {
      is = new FileInputStream(file);
      os = new FileOutputStream(output);
      int length;
      while ((length = is.read(buffer)) > 0) {
        os.write(buffer, 0, length);
      }
    } finally {
      IOUtils.close(is, os);
    }
  }

   /** Sort a single partition in-memory. */
  protected File sortPartition(int len) throws IOException {
  protected Path sortPartition(int len) throws IOException {
     BytesRefArray data = this.buffer;
    File tempFile = File.createTempFile("sort", "partition", tempDirectory);
    Path tempFile = Files.createTempFile(tempDirectory, "sort", "partition");
 
     long start = System.currentTimeMillis();
     sortInfo.sortTime += (System.currentTimeMillis() - start);
@@ -356,7 +336,7 @@ public final class OfflineSorter {
   }
 
   /** Merge a list of sorted temporary files (partitions) into an output file */
  void mergePartitions(List<File> merges, File outputFile) throws IOException {
  void mergePartitions(List<Path> merges, Path outputFile) throws IOException {
     long start = System.currentTimeMillis();
 
     ByteSequencesWriter out = new ByteSequencesWriter(outputFile);
@@ -441,11 +421,11 @@ public final class OfflineSorter {
   public static class ByteSequencesWriter implements Closeable {
     private final DataOutput os;
 
    /** Constructs a ByteSequencesWriter to the provided File */
    public ByteSequencesWriter(File file) throws IOException {
    /** Constructs a ByteSequencesWriter to the provided Path */
    public ByteSequencesWriter(Path path) throws IOException {
       this(new DataOutputStream(
           new BufferedOutputStream(
              new FileOutputStream(file))));
              Files.newOutputStream(path))));
     }
 
     /** Constructs a ByteSequencesWriter to the provided DataOutput */
@@ -505,11 +485,11 @@ public final class OfflineSorter {
   public static class ByteSequencesReader implements Closeable {
     private final DataInput is;
 
    /** Constructs a ByteSequencesReader from the provided File */
    public ByteSequencesReader(File file) throws IOException {
    /** Constructs a ByteSequencesReader from the provided Path */
    public ByteSequencesReader(Path path) throws IOException {
       this(new DataInputStream(
           new BufferedInputStream(
              new FileInputStream(file))));
              Files.newInputStream(path))));
     }
 
     /** Constructs a ByteSequencesReader from the provided DataInput */
diff --git a/lucene/core/src/java/org/apache/lucene/util/fst/FST.java b/lucene/core/src/java/org/apache/lucene/util/fst/FST.java
index 990e3760d77..75d7f808594 100644
-- a/lucene/core/src/java/org/apache/lucene/util/fst/FST.java
++ b/lucene/core/src/java/org/apache/lucene/util/fst/FST.java
@@ -19,12 +19,11 @@ package org.apache.lucene.util.fst;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Map;
 
@@ -614,37 +613,18 @@ public final class FST<T> implements Accountable {
   /**
    * Writes an automaton to a file. 
    */
  public void save(final File file) throws IOException {
    boolean success = false;
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    try {
      save(new OutputStreamDataOutput(os));
      success = true;
    } finally { 
      if (success) { 
        IOUtils.close(os);
      } else {
        IOUtils.closeWhileHandlingException(os); 
      }
  public void save(final Path path) throws IOException {
    try (OutputStream os = Files.newOutputStream(path)) {
      save(new OutputStreamDataOutput(new BufferedOutputStream(os)));
     }
   }
 
   /**
    * Reads an automaton from a file. 
    */
  public static <T> FST<T> read(File file, Outputs<T> outputs) throws IOException {
    InputStream is = new BufferedInputStream(new FileInputStream(file));
    boolean success = false;
    try {
      FST<T> fst = new FST<>(new InputStreamDataInput(is), outputs);
      success = true;
      return fst;
    } finally {
      if (success) { 
        IOUtils.close(is);
      } else {
        IOUtils.closeWhileHandlingException(is); 
      }
  public static <T> FST<T> read(Path path, Outputs<T> outputs) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return new FST<>(new InputStreamDataInput(new BufferedInputStream(is)), outputs);
     }
   }
 
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestAtomicUpdate.java b/lucene/core/src/test/org/apache/lucene/index/TestAtomicUpdate.java
index 94e55096cd4..db6bbb69f7b 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestAtomicUpdate.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestAtomicUpdate.java
@@ -17,6 +17,7 @@ package org.apache.lucene.index;
  */
 
 import java.io.File;
import java.nio.file.Path;
 
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.*;
@@ -174,7 +175,7 @@ public class TestAtomicUpdate extends LuceneTestCase {
     directory.close();
 
     // Second in an FSDirectory:
    File dirPath = createTempDir("lucene.test.atomic");
    Path dirPath = createTempDir("lucene.test.atomic");
     directory = newFSDirectory(dirPath);
     runTest(directory);
     directory.close();
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestCompoundFile.java b/lucene/core/src/test/org/apache/lucene/index/TestCompoundFile.java
index 704131f5633..5c284749099 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestCompoundFile.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestCompoundFile.java
@@ -30,8 +30,8 @@ import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.TestUtil;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 import static org.apache.lucene.store.TestHelper.isSimpleFSIndexInput;
 import static org.apache.lucene.store.TestHelper.isSimpleFSIndexInputOpen;
@@ -43,7 +43,7 @@ public class TestCompoundFile extends LuceneTestCase
     @Override
     public void setUp() throws Exception {
        super.setUp();
       File file = createTempDir("testIndex");
       Path file = createTempDir("testIndex");
        // use a simple FSDir here, to be sure to have SimpleFSInputs
        dir = new SimpleFSDirectory(file,null);
     }
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestCrashCausesCorruptIndex.java b/lucene/core/src/test/org/apache/lucene/index/TestCrashCausesCorruptIndex.java
index e8ae00a4be2..db1f09d655b 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestCrashCausesCorruptIndex.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestCrashCausesCorruptIndex.java
@@ -17,8 +17,8 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
@@ -35,7 +35,7 @@ import org.apache.lucene.util.LuceneTestCase;
 
 public class TestCrashCausesCorruptIndex extends LuceneTestCase  {
 
  File path;
  Path path;
     
   /**
    * LUCENE-3627: This test fails.
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java b/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
index 1046363da64..fc82dceeb83 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestDirectoryReader.java
@@ -22,6 +22,7 @@ import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
@@ -38,8 +39,8 @@ import org.apache.lucene.document.StringField;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.IndexWriterConfig.OpenMode;
 import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.BaseDirectoryWrapper;
 import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoSuchDirectoryException;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.IOUtils;
@@ -440,7 +441,7 @@ void assertTermDocsCount(String msg,
   
 public void testFilesOpenClose() throws IOException {
       // Create initial data set
      File dirFile = createTempDir("TestIndexReader.testFilesOpenClose");
      Path dirFile = createTempDir("TestIndexReader.testFilesOpenClose");
       Directory dir = newFSDirectory(dirFile);
       IndexWriter writer  = new IndexWriter(dir, newIndexWriterConfig(new MockAnalyzer(random())));
       addDoc(writer, "test");
@@ -470,8 +471,11 @@ public void testFilesOpenClose() throws IOException {
   }
 
   public void testOpenReaderAfterDelete() throws IOException {
    File dirFile = createTempDir("deletetest");
    Path dirFile = createTempDir("deletetest");
     Directory dir = newFSDirectory(dirFile);
    if (dir instanceof BaseDirectoryWrapper) {
      ((BaseDirectoryWrapper)dir).setCheckIndexOnClose(false); // we will hit NoSuchFileException in MDW since we nuked it!
    }
     try {
       DirectoryReader.open(dir);
       fail("expected FileNotFoundException/NoSuchFileException");
@@ -479,7 +483,7 @@ public void testFilesOpenClose() throws IOException {
       // expected
     }
 
    Files.delete(dirFile.toPath());
    Files.delete(dirFile);
 
     // Make sure we still get a CorruptIndexException (not NPE):
     try {
@@ -717,13 +721,13 @@ public void testFilesOpenClose() throws IOException {
   // DirectoryReader on a non-existent directory, you get a
   // good exception
   public void testNoDir() throws Throwable {
    File tempDir = createTempDir("doesnotexist");
    Path tempDir = createTempDir("doesnotexist");
     IOUtils.rm(tempDir);
     Directory dir = newFSDirectory(tempDir);
     try {
       DirectoryReader.open(dir);
       fail("did not hit expected exception");
    } catch (NoSuchDirectoryException nsde) {
    } catch (IndexNotFoundException nsde) {
       // expected
     }
     dir.close();
@@ -1053,8 +1057,8 @@ public void testFilesOpenClose() throws IOException {
   }
 
   public void testIndexExistsOnNonExistentDirectory() throws Exception {
    File tempDir = createTempDir("testIndexExistsOnNonExistentDirectory");
    Files.delete(tempDir.toPath());
    Path tempDir = createTempDir("testIndexExistsOnNonExistentDirectory");
    Files.delete(tempDir);
     Directory dir = newFSDirectory(tempDir);
     assertFalse(DirectoryReader.indexExists(dir));
     dir.close();
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestDoc.java b/lucene/core/src/test/org/apache/lucene/index/TestDoc.java
index a2b06de5691..78c7534bd1c 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestDoc.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestDoc.java
@@ -16,9 +16,7 @@ package org.apache.lucene.index;
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
@@ -27,6 +25,7 @@ import java.io.StringWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashSet;
@@ -50,9 +49,9 @@ import org.apache.lucene.util.Version;
 /** JUnit adaptation of an older test case DocTest. */
 public class TestDoc extends LuceneTestCase {
 
    private File workDir;
    private File indexDir;
    private LinkedList<File> files;
    private Path workDir;
    private Path indexDir;
    private LinkedList<Path> files;
 
     /** Set the test case. This test case needs
      *  a few text files created in the current working directory.
@@ -64,10 +63,7 @@ public class TestDoc extends LuceneTestCase {
           System.out.println("TEST: setUp");
         }
         workDir = createTempDir("TestDoc");
        workDir.mkdirs();

         indexDir = createTempDir("testIndex");
        indexDir.mkdirs();
 
         Directory directory = newFSDirectory(indexDir);
         directory.close();
@@ -82,18 +78,18 @@ public class TestDoc extends LuceneTestCase {
         ));
     }
 
    private File createOutput(String name, String text) throws IOException {
    private Path createOutput(String name, String text) throws IOException {
         Writer fw = null;
         PrintWriter pw = null;
 
         try {
            File f = new File(workDir, name);
            Files.deleteIfExists(f.toPath());
            Path path = workDir.resolve(name);
            Files.deleteIfExists(path);
 
            fw = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
            fw = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
             pw = new PrintWriter(fw);
             pw.println(text);
            return f;
            return path;
 
         } finally {
             if (pw != null) pw.close();
@@ -203,9 +199,9 @@ public class TestDoc extends LuceneTestCase {
    private SegmentCommitInfo indexDoc(IndexWriter writer, String fileName)
    throws Exception
    {
      File file = new File(workDir, fileName);
      Path path = workDir.resolve(fileName);
       Document doc = new Document();
      InputStreamReader is = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
      InputStreamReader is = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8);
       doc.add(new TextField("contents", is));
       writer.addDocument(doc);
       writer.commit();
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestFieldsReader.java b/lucene/core/src/test/org/apache/lucene/index/TestFieldsReader.java
index e9917d717e6..fd62a4639bd 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestFieldsReader.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestFieldsReader.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 
 import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicBoolean;
 
@@ -110,7 +111,7 @@ public class TestFieldsReader extends LuceneTestCase {
     Directory fsDir;
     AtomicBoolean doFail = new AtomicBoolean();
 
    public FaultyFSDirectory(File dir) {
    public FaultyFSDirectory(Path dir) {
       fsDir = newFSDirectory(dir);
       lockFactory = fsDir.getLockFactory();
     }
@@ -220,7 +221,7 @@ public class TestFieldsReader extends LuceneTestCase {
 
   // LUCENE-1262
   public void testExceptions() throws Throwable {
    File indexDir = createTempDir("testfieldswriterexceptions");
    Path indexDir = createTempDir("testfieldswriterexceptions");
 
     try {
       FaultyFSDirectory dir = new FaultyFSDirectory(indexDir);
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterOnJRECrash.java b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterOnJRECrash.java
index c0db3dcd1eb..f4d9972ee3c 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterOnJRECrash.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestIndexWriterOnJRECrash.java
@@ -18,15 +18,19 @@ package org.apache.lucene.index;
  *
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.PrintStream;
 import java.lang.reflect.Field;
 import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
 import java.util.ArrayList;
 import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.apache.lucene.store.BaseDirectoryWrapper;
 import org.apache.lucene.util.Constants;
@@ -38,7 +42,7 @@ import com.carrotsearch.randomizedtesting.SeedUtils;
  * of execution, then runs checkindex to make sure its not corrupt.
  */
 public class TestIndexWriterOnJRECrash extends TestNRTThreads {
  private File tempDir;
  private Path tempDir;
   
   @Override
   public void setUp() throws Exception {
@@ -96,7 +100,7 @@ public class TestIndexWriterOnJRECrash extends TestNRTThreads {
     cmd.add("-Dtests.crashmode=true");
     // passing NIGHTLY to this test makes it run for much longer, easier to catch it in the act...
     cmd.add("-Dtests.nightly=true");
    cmd.add("-DtempDir=" + tempDir.getPath());
    cmd.add("-DtempDir=" + tempDir);
     cmd.add("-Dtests.seed=" + SeedUtils.formatSeed(random().nextLong()));
     cmd.add("-ea");
     cmd.add("-cp");
@@ -104,7 +108,7 @@ public class TestIndexWriterOnJRECrash extends TestNRTThreads {
     cmd.add("org.junit.runner.JUnitCore");
     cmd.add(getClass().getName());
     ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(tempDir);
    pb.directory(tempDir.toFile());
     pb.redirectErrorStream(true);
     Process p = pb.start();
 
@@ -147,30 +151,35 @@ public class TestIndexWriterOnJRECrash extends TestNRTThreads {
    * Recursively looks for indexes underneath <code>file</code>,
    * and runs checkindex on them. returns true if it found any indexes.
    */
  public boolean checkIndexes(File file) throws IOException {
    if (file.isDirectory()) {
      BaseDirectoryWrapper dir = newFSDirectory(file);
      dir.setCheckIndexOnClose(false); // don't double-checkindex
      if (DirectoryReader.indexExists(dir)) {
        if (VERBOSE) {
          System.err.println("Checking index: " + file);
        }
        // LUCENE-4738: if we crashed while writing first
        // commit it's possible index will be corrupt (by
        // design we don't try to be smart about this case
        // since that too risky):
        if (SegmentInfos.getLastCommitGeneration(dir) > 1) {
          TestUtil.checkIndex(dir);
  public boolean checkIndexes(Path path) throws IOException {
    final AtomicBoolean found = new AtomicBoolean();
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path dirPath, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        } else {
          BaseDirectoryWrapper dir = newFSDirectory(dirPath);
          dir.setCheckIndexOnClose(false); // don't double-checkindex
          if (DirectoryReader.indexExists(dir)) {
            if (VERBOSE) {
              System.err.println("Checking index: " + dirPath);
            }
            // LUCENE-4738: if we crashed while writing first
            // commit it's possible index will be corrupt (by
            // design we don't try to be smart about this case
            // since that too risky):
            if (SegmentInfos.getLastCommitGeneration(dir) > 1) {
              TestUtil.checkIndex(dir);
            }
            dir.close();
            found.set(true);
          }
          return FileVisitResult.CONTINUE;
         }
        dir.close();
        return true;
       }
      dir.close();
      for (File f : file.listFiles())
        if (checkIndexes(f))
          return true;
    }
    return false;
    });
    return found.get();
   }
 
   /**
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestNeverDelete.java b/lucene/core/src/test/org/apache/lucene/index/TestNeverDelete.java
index f5801209a53..0a0438a53d8 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestNeverDelete.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestNeverDelete.java
@@ -17,7 +17,7 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Path;
 import java.util.HashSet;
 import java.util.Set;
 
@@ -36,7 +36,7 @@ import org.apache.lucene.util.TestUtil;
 public class TestNeverDelete extends LuceneTestCase {
 
   public void testIndexing() throws Exception {
    final File tmpDir = createTempDir("TestNeverDelete");
    final Path tmpDir = createTempDir("TestNeverDelete");
     final BaseDirectoryWrapper d = newFSDirectory(tmpDir);
 
     // We want to "see" files removed if Lucene removed
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestBufferedIndexInput.java b/lucene/core/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
index 9acda2970ae..97ee2ba9865 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestBufferedIndexInput.java
@@ -17,10 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
@@ -45,21 +43,6 @@ import org.apache.lucene.util.ArrayUtil;
 
 public class TestBufferedIndexInput extends LuceneTestCase {
   
  private static void writeBytes(File aFile, long size) throws IOException{
    OutputStream stream = null;
    try {
      stream = new FileOutputStream(aFile);
      for (int i = 0; i < size; i++) {
        stream.write(byten(i));  
      }
      stream.flush();
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }

   private static final long TEST_FILE_LENGTH = 100*1024;
  
   // Call readByte() repeatedly, past the buffer boundary, and see that it
@@ -228,7 +211,7 @@ public class TestBufferedIndexInput extends LuceneTestCase {
     }
 
     public void testSetBufferSize() throws IOException {
      File indexDir = createTempDir("testSetBufferSize");
      Path indexDir = createTempDir("testSetBufferSize");
       MockFSDirectory dir = new MockFSDirectory(indexDir, random());
       try {
         IndexWriter writer = new IndexWriter(
@@ -292,7 +275,7 @@ public class TestBufferedIndexInput extends LuceneTestCase {
 
       private Directory dir;
 
      public MockFSDirectory(File path, Random rand) throws IOException {
      public MockFSDirectory(Path path, Random rand) throws IOException {
         this.rand = rand;
         lockFactory = NoLockFactory.getNoLockFactory();
         dir = new SimpleFSDirectory(path, null);
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestDirectory.java
index 66f1f81da7c..57c2c27ab80 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestDirectory.java
@@ -20,7 +20,9 @@ package org.apache.lucene.store;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
import java.nio.file.Files;
 import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.Collections;
 
@@ -30,7 +32,7 @@ import org.apache.lucene.util.TestUtil;
 public class TestDirectory extends BaseDirectoryTestCase {
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     final Directory dir;
     if (random().nextBoolean()) {
       dir = newDirectory();
@@ -53,7 +55,7 @@ public class TestDirectory extends BaseDirectoryTestCase {
   // Test that different instances of FSDirectory can coexist on the same
   // path, can read, write, and lock files.
   public void testDirectInstantiation() throws Exception {
    final File path = createTempDir("testDirectInstantiation");
    final Path path = createTempDir("testDirectInstantiation");
     
     final byte[] largeBuffer = new byte[random().nextInt(256*1024)], largeReadBuffer = new byte[largeBuffer.length];
     for (int i = 0; i < largeBuffer.length; i++) {
@@ -141,10 +143,9 @@ public class TestDirectory extends BaseDirectoryTestCase {
 
   // LUCENE-1468
   public void testCopySubdir() throws Throwable {
    File path = createTempDir("testsubdir");
    Path path = createTempDir("testsubdir");
     try {
      path.mkdirs();
      new File(path, "subdir").mkdirs();
      Files.createDirectory(path.resolve("subdir"));
       Directory fsDir = new SimpleFSDirectory(path, null);
       assertEquals(0, new RAMDirectory(fsDir, newIOContext(random())).listAll().length);
     } finally {
@@ -154,16 +155,16 @@ public class TestDirectory extends BaseDirectoryTestCase {
 
   // LUCENE-1468
   public void testNotDirectory() throws Throwable {
    File path = createTempDir("testnotdir");
    Path path = createTempDir("testnotdir");
     Directory fsDir = new SimpleFSDirectory(path, null);
     try {
       IndexOutput out = fsDir.createOutput("afile", newIOContext(random()));
       out.close();
       assertTrue(slowFileExists(fsDir, "afile"));
       try {
        new SimpleFSDirectory(new File(path, "afile"), null);
        new SimpleFSDirectory(path.resolve("afile"), null);
         fail("did not hit expected exception");
      } catch (NoSuchDirectoryException nsde) {
      } catch (IOException nsde) {
         // Expected
       }
     } finally {
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestFileSwitchDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestFileSwitchDirectory.java
index b3dd7caa793..1135423045f 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestFileSwitchDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestFileSwitchDirectory.java
@@ -17,8 +17,9 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Set;
@@ -27,12 +28,12 @@ import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.codecs.Codec;
 import org.apache.lucene.codecs.compressing.CompressingStoredFieldsWriter;
 import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.TestIndexWriterReader;
 import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.TestUtil;
 
 public class TestFileSwitchDirectory extends BaseDirectoryTestCase {
 
@@ -85,12 +86,12 @@ public class TestFileSwitchDirectory extends BaseDirectoryTestCase {
   }
   
   private Directory newFSSwitchDirectory(Set<String> primaryExtensions) throws IOException {
    File primDir = createTempDir("foo");
    File secondDir = createTempDir("bar");
    Path primDir = createTempDir("foo");
    Path secondDir = createTempDir("bar");
     return newFSSwitchDirectory(primDir, secondDir, primaryExtensions);
   }
 
  private Directory newFSSwitchDirectory(File aDir, File bDir, Set<String> primaryExtensions) throws IOException {
  private Directory newFSSwitchDirectory(Path aDir, Path bDir, Set<String> primaryExtensions) throws IOException {
     Directory a = new SimpleFSDirectory(aDir);
     Directory b = new SimpleFSDirectory(bDir);
     return new FileSwitchDirectory(primaryExtensions, a, b, true);
@@ -98,21 +99,21 @@ public class TestFileSwitchDirectory extends BaseDirectoryTestCase {
   
   // LUCENE-3380 -- make sure we get exception if the directory really does not exist.
   public void testNoDir() throws Throwable {
    File primDir = createTempDir("foo");
    File secondDir = createTempDir("bar");
    Path primDir = createTempDir("foo");
    Path secondDir = createTempDir("bar");
     IOUtils.rm(primDir, secondDir);
     Directory dir = newFSSwitchDirectory(primDir, secondDir, Collections.<String>emptySet());
     try {
       DirectoryReader.open(dir);
       fail("did not hit expected exception");
    } catch (NoSuchDirectoryException nsde) {
    } catch (IndexNotFoundException nsde) {
       // expected
     }
     dir.close();
   }
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     Set<String> extensions = new HashSet<String>();
     if (random().nextBoolean()) {
       extensions.add("cfs");
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestLockFactory.java b/lucene/core/src/test/org/apache/lucene/store/TestLockFactory.java
index efd59a15ac4..b511e7c629f 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestLockFactory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestLockFactory.java
@@ -17,9 +17,9 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
@@ -38,7 +38,6 @@ import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
 
 public class TestLockFactory extends LuceneTestCase {
 
@@ -126,11 +125,6 @@ public class TestLockFactory extends LuceneTestCase {
             writer2.close();
         }
     }
    
    public void testSimpleFSLockFactory() throws IOException {
      // test string file instantiation
      new SimpleFSLockFactory("test");
    }
 
     // Verify: do stress test, by opening IndexReaders and
     // IndexWriters over & over in 2 threads and making sure
@@ -146,11 +140,11 @@ public class TestLockFactory extends LuceneTestCase {
     // NativeFSLockFactory:
     @Nightly
     public void testStressLocksNativeFSLockFactory() throws Exception {
      File dir = createTempDir("index.TestLockFactory7");
      Path dir = createTempDir("index.TestLockFactory7");
       _testStressLocks(new NativeFSLockFactory(dir), dir);
     }
 
    public void _testStressLocks(LockFactory lockFactory, File indexDir) throws Exception {
    public void _testStressLocks(LockFactory lockFactory, Path indexDir) throws Exception {
         Directory dir = newFSDirectory(indexDir, lockFactory);
 
         // First create a 1 doc index:
@@ -202,22 +196,22 @@ public class TestLockFactory extends LuceneTestCase {
     
     // Verify: NativeFSLockFactory works correctly if the lock file exists
     public void testNativeFSLockFactoryLockExists() throws IOException {
      File tempDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
      File lockFile = new File(tempDir, "test.lock");
      lockFile.createNewFile();
      Path tempDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
      Path lockFile = tempDir.resolve("test.lock");
      Files.createFile(lockFile);
       
       Lock l = new NativeFSLockFactory(tempDir).makeLock("test.lock");
       assertTrue("failed to obtain lock", l.obtain());
       l.close();
       assertFalse("failed to release lock", l.isLocked());
      Files.deleteIfExists(lockFile.toPath());
      Files.deleteIfExists(lockFile);
     }
 
     // Verify: NativeFSLockFactory assigns null as lockPrefix if the lockDir is inside directory
     public void testNativeFSLockFactoryPrefix() throws IOException {
 
      File fdir1 = createTempDir("TestLockFactory.8");
      File fdir2 = createTempDir("TestLockFactory.8.Lockdir");
      Path fdir1 = createTempDir("TestLockFactory.8");
      Path fdir2 = createTempDir("TestLockFactory.8.Lockdir");
       Directory dir1 = newFSDirectory(fdir1, new NativeFSLockFactory(fdir1));
       // same directory, but locks are stored somewhere else. The prefix of the lock factory should != null
       Directory dir2 = newFSDirectory(fdir1, new NativeFSLockFactory(fdir2));
@@ -238,7 +232,7 @@ public class TestLockFactory extends LuceneTestCase {
     public void testDefaultFSLockFactoryPrefix() throws IOException {
 
       // Make sure we get null prefix, which wont happen if setLockFactory is ever called.
      File dirName = createTempDir("TestLockFactory.10");
      Path dirName = createTempDir("TestLockFactory.10");
 
       Directory dir = new SimpleFSDirectory(dirName);
       assertNull("Default lock prefix should be null", dir.getLockFactory().getLockPrefix());
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestMmapDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestMmapDirectory.java
index bc896e0dbcd..bd7dd219ae7 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestMmapDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestMmapDirectory.java
@@ -17,8 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 /**
  * Tests MMapDirectory
@@ -26,7 +26,7 @@ import java.io.IOException;
 public class TestMmapDirectory extends BaseDirectoryTestCase {
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return new MMapDirectory(path);
   }
 }
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestMultiMMap.java b/lucene/core/src/test/org/apache/lucene/store/TestMultiMMap.java
index 79caa27e58d..8e0a5513327 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestMultiMMap.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestMultiMMap.java
@@ -17,8 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.Random;
 
 import org.apache.lucene.analysis.MockAnalyzer;
@@ -37,10 +37,9 @@ import org.apache.lucene.util.TestUtil;
  * Integer.MAX_VALUE in size using multiple byte buffers.
  */
 public class TestMultiMMap extends BaseDirectoryTestCase {
  File workDir;
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return new MMapDirectory(path, null, 1<<TestUtil.nextInt(random(), 10, 28));
   }
   
@@ -335,7 +334,7 @@ public class TestMultiMMap extends BaseDirectoryTestCase {
   }
   
   private void assertChunking(Random random, int chunkSize) throws Exception {
    File path = createTempDir("mmap" + chunkSize);
    Path path = createTempDir("mmap" + chunkSize);
     MMapDirectory mmapDir = new MMapDirectory(path, null, chunkSize);
     // we will map a lot, try to turn on the unmap hack
     if (MMapDirectory.UNMAP_SUPPORTED)
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestNIOFSDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestNIOFSDirectory.java
index cfbd40941fd..91d4c362e16 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestNIOFSDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestNIOFSDirectory.java
@@ -17,8 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 /**
  * Tests NIOFSDirectory
@@ -26,7 +26,7 @@ import java.io.IOException;
 public class TestNIOFSDirectory extends BaseDirectoryTestCase {
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return new NIOFSDirectory(path);
   }
 }
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestNRTCachingDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestNRTCachingDirectory.java
index 8d9a5a183c2..cd41dff3986 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestNRTCachingDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestNRTCachingDirectory.java
@@ -17,8 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.List;
 
@@ -43,7 +43,7 @@ public class TestNRTCachingDirectory extends BaseDirectoryTestCase {
   // for the threads tests... maybe because of the synchronization in listAll?
   // would be good to investigate further...
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return new NRTCachingDirectory(new RAMDirectory(),
                                    .1 + 2.0*random().nextDouble(),
                                    .1 + 5.0*random().nextDouble());
@@ -115,7 +115,7 @@ public class TestNRTCachingDirectory extends BaseDirectoryTestCase {
   public void verifyCompiles() throws Exception {
     Analyzer analyzer = null;
 
    Directory fsDir = FSDirectory.open(new File("/path/to/index"));
    Directory fsDir = FSDirectory.open(createTempDir("verify"));
     NRTCachingDirectory cachedFSDir = new NRTCachingDirectory(fsDir, 2.0, 25.0);
     IndexWriterConfig conf = new IndexWriterConfig(analyzer);
     IndexWriter writer = new IndexWriter(cachedFSDir, conf);
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestRAMDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestRAMDirectory.java
index 50612b571bb..694ec1a8911 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestRAMDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestRAMDirectory.java
@@ -17,8 +17,9 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
@@ -32,7 +33,6 @@ import org.apache.lucene.index.StoredDocument;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.util.English;
 import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.TestUtil;
 
 /**
  * JUnit testcase to test RAMDirectory. RAMDirectory itself is used in many testcases,
@@ -41,15 +41,15 @@ import org.apache.lucene.util.TestUtil;
 public class TestRAMDirectory extends BaseDirectoryTestCase {
   
   @Override
  protected Directory getDirectory(File path) {
  protected Directory getDirectory(Path path) {
     return new RAMDirectory();
   }
   
   // add enough document so that the index will be larger than RAMDirectory.READ_BUFFER_SIZE
   private final int docsToAdd = 500;
 
  private File buildIndex() throws IOException {
    File path = createTempDir("buildIndex");
  private Path buildIndex() throws IOException {
    Path path = createTempDir("buildIndex");
     
     Directory dir = newFSDirectory(path);
     IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
@@ -70,11 +70,10 @@ public class TestRAMDirectory extends BaseDirectoryTestCase {
   
   // LUCENE-1468
   public void testCopySubdir() throws Throwable {
    File path = createTempDir("testsubdir");
    Path path = createTempDir("testsubdir");
     Directory fsDir = null;
     try {
      path.mkdirs();
      new File(path, "subdir").mkdirs();
      Files.createDirectory(path.resolve("subdir"));
       fsDir = newFSDirectory(path);
       assertEquals(0, new RAMDirectory(fsDir, newIOContext(random())).listAll().length);
     } finally {
@@ -84,7 +83,7 @@ public class TestRAMDirectory extends BaseDirectoryTestCase {
   }
 
   public void testRAMDirectory () throws IOException {
    File indexDir = buildIndex();
    Path indexDir = buildIndex();
     
     Directory dir = newFSDirectory(indexDir);
     MockDirectoryWrapper ramDir = new MockDirectoryWrapper(random(), new RAMDirectory(dir, newIOContext(random())));
@@ -117,7 +116,7 @@ public class TestRAMDirectory extends BaseDirectoryTestCase {
   
   public void testRAMDirectorySize() throws IOException, InterruptedException {
 
    File indexDir = buildIndex();
    Path indexDir = buildIndex();
       
     Directory dir = newFSDirectory(indexDir);
     final MockDirectoryWrapper ramDir = new MockDirectoryWrapper(random(), new RAMDirectory(dir, newIOContext(random())));
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestRateLimitedDirectoryWrapper.java b/lucene/core/src/test/org/apache/lucene/store/TestRateLimitedDirectoryWrapper.java
index f01d9765325..6c34526eff4 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestRateLimitedDirectoryWrapper.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestRateLimitedDirectoryWrapper.java
@@ -17,12 +17,12 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Path;
 
 public class TestRateLimitedDirectoryWrapper extends BaseDirectoryTestCase {
 
   @Override
  protected Directory getDirectory(File path) {
  protected Directory getDirectory(Path path) {
     Directory in = newFSDirectory(path);
     if (in instanceof MockDirectoryWrapper) {
       // test manipulates directory directly
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestSimpleFSDirectory.java b/lucene/core/src/test/org/apache/lucene/store/TestSimpleFSDirectory.java
index e222b5a0367..e8b62774434 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestSimpleFSDirectory.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestSimpleFSDirectory.java
@@ -17,8 +17,8 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 /**
  * Tests SimpleFSDirectory
@@ -26,7 +26,7 @@ import java.io.IOException;
 public class TestSimpleFSDirectory extends BaseDirectoryTestCase {
 
   @Override
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return new SimpleFSDirectory(path);
   }
 }
diff --git a/lucene/core/src/test/org/apache/lucene/store/TestWindowsMMap.java b/lucene/core/src/test/org/apache/lucene/store/TestWindowsMMap.java
index 91eb8eb0d01..737a558346f 100644
-- a/lucene/core/src/test/org/apache/lucene/store/TestWindowsMMap.java
++ b/lucene/core/src/test/org/apache/lucene/store/TestWindowsMMap.java
@@ -17,7 +17,7 @@ package org.apache.lucene.store;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Path;
 
 import org.apache.lucene.document.Field;
 import org.apache.lucene.util.LuceneTestCase;
@@ -64,7 +64,7 @@ public class TestWindowsMMap extends LuceneTestCase {
     // sometimes the directory is not cleaned by rmDir, because on Windows it
     // may take some time until the files are finally dereferenced. So clean the
     // directory up front, or otherwise new IndexWriter will fail.
    File dirPath = createTempDir("testLuceneMmap");
    Path dirPath = createTempDir("testLuceneMmap");
     MMapDirectory dir = new MMapDirectory(dirPath, null);
     
     // plan to add a set of useful stopwords, consider changing some of the
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestOfflineSorter.java b/lucene/core/src/test/org/apache/lucene/util/TestOfflineSorter.java
index 26a168dd2fe..fa1df8a73ec 100644
-- a/lucene/core/src/test/org/apache/lucene/util/TestOfflineSorter.java
++ b/lucene/core/src/test/org/apache/lucene/util/TestOfflineSorter.java
@@ -18,10 +18,10 @@ package org.apache.lucene.util;
  */
 
 import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
@@ -32,13 +32,12 @@ import org.apache.lucene.util.OfflineSorter;
 import org.apache.lucene.util.OfflineSorter.BufferSize;
 import org.apache.lucene.util.OfflineSorter.ByteSequencesWriter;
 import org.apache.lucene.util.OfflineSorter.SortInfo;
import org.apache.lucene.util.TestUtil;
 
 /**
  * Tests for on-disk merge sorting.
  */
 public class TestOfflineSorter extends LuceneTestCase {
  private File tempDir;
  private Path tempDir;
 
   @Override
   public void setUp() throws Exception {
@@ -112,12 +111,12 @@ public class TestOfflineSorter extends LuceneTestCase {
    * Check sorting data on an instance of {@link OfflineSorter}.
    */
   private SortInfo checkSort(OfflineSorter sort, byte[][] data) throws IOException {
    File unsorted = writeAll("unsorted", data);
    Path unsorted = writeAll("unsorted", data);
 
     Arrays.sort(data, unsignedByteOrderComparator);
    File golden = writeAll("golden", data);
    Path golden = writeAll("golden", data);
 
    File sorted = new File(tempDir, "sorted");
    Path sorted = tempDir.resolve("sorted");
     SortInfo sortInfo = sort.sort(unsorted, sorted);
     //System.out.println("Input size [MB]: " + unsorted.length() / (1024 * 1024));
     //System.out.println(sortInfo);
@@ -129,14 +128,14 @@ public class TestOfflineSorter extends LuceneTestCase {
   /**
    * Make sure two files are byte-byte identical.
    */
  private void assertFilesIdentical(File golden, File sorted) throws IOException {
    assertEquals(golden.length(), sorted.length());
  private void assertFilesIdentical(Path golden, Path sorted) throws IOException {
    assertEquals(Files.size(golden), Files.size(sorted));
 
     byte [] buf1 = new byte [64 * 1024];
     byte [] buf2 = new byte [64 * 1024];
     int len;
    DataInputStream is1 = new DataInputStream(new FileInputStream(golden));
    DataInputStream is2 = new DataInputStream(new FileInputStream(sorted));
    DataInputStream is1 = new DataInputStream(Files.newInputStream(golden));
    DataInputStream is2 = new DataInputStream(Files.newInputStream(sorted));
     while ((len = is1.read(buf1)) > 0) {
       is2.readFully(buf2, 0, len);
       for (int i = 0; i < len; i++) {
@@ -146,8 +145,8 @@ public class TestOfflineSorter extends LuceneTestCase {
     IOUtils.close(is1, is2);
   }
 
  private File writeAll(String name, byte[][] data) throws IOException {
    File file = new File(tempDir, name);
  private Path writeAll(String name, byte[][] data) throws IOException {
    Path file = tempDir.resolve(name);
     ByteSequencesWriter w = new OfflineSorter.ByteSequencesWriter(file);
     for (byte [] datum : data) {
       w.write(datum);
diff --git a/lucene/core/src/test/org/apache/lucene/util/fst/TestFSTs.java b/lucene/core/src/test/org/apache/lucene/util/fst/TestFSTs.java
index f5a80fd72e6..49b7e7af9be 100644
-- a/lucene/core/src/test/org/apache/lucene/util/fst/TestFSTs.java
++ b/lucene/core/src/test/org/apache/lucene/util/fst/TestFSTs.java
@@ -18,15 +18,13 @@ package org.apache.lucene.util.fst;
  */
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
@@ -313,7 +311,7 @@ public class TestFSTs extends LuceneTestCase {
     analyzer.setMaxTokenLength(TestUtil.nextInt(random(), 1, IndexWriter.MAX_TERM_LENGTH));
 
     final IndexWriterConfig conf = newIndexWriterConfig(analyzer).setMaxBufferedDocs(-1).setRAMBufferSizeMB(64);
    final File tempDir = createTempDir("fstlines");
    final Path tempDir = createTempDir("fstlines");
     final Directory dir = newFSDirectory(tempDir);
     final IndexWriter writer = new IndexWriter(dir, conf);
     final long stopTime = System.currentTimeMillis() + RUN_TIME_MSEC;
@@ -458,14 +456,14 @@ public class TestFSTs extends LuceneTestCase {
   }
 
   private static abstract class VisitTerms<T> {
    private final String dirOut;
    private final String wordsFileIn;
    private final Path dirOut;
    private final Path wordsFileIn;
     private int inputMode;
     private final Outputs<T> outputs;
     private final Builder<T> builder;
     private final boolean doPack;
 
    public VisitTerms(String dirOut, String wordsFileIn, int inputMode, int prune, Outputs<T> outputs, boolean doPack, boolean noArcArrays) {
    public VisitTerms(Path dirOut, Path wordsFileIn, int inputMode, int prune, Outputs<T> outputs, boolean doPack, boolean noArcArrays) {
       this.dirOut = dirOut;
       this.wordsFileIn = wordsFileIn;
       this.inputMode = inputMode;
@@ -478,7 +476,8 @@ public class TestFSTs extends LuceneTestCase {
     protected abstract T getOutput(IntsRef input, int ord) throws IOException;
 
     public void run(int limit, boolean verify, boolean verifyByOutput) throws IOException {
      BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(wordsFileIn), StandardCharsets.UTF_8), 65536);
      
      BufferedReader is = Files.newBufferedReader(wordsFileIn, StandardCharsets.UTF_8);
       try {
         final IntsRefBuilder intsRef = new IntsRefBuilder();
         long tStart = System.currentTimeMillis();
@@ -521,13 +520,13 @@ public class TestFSTs extends LuceneTestCase {
 
         System.out.println(ord + " terms; " + fst.getNodeCount() + " nodes; " + fst.getArcCount() + " arcs; " + fst.getArcWithOutputCount() + " arcs w/ output; tot size " + fst.ramBytesUsed());
         if (fst.getNodeCount() < 100) {
          Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"), StandardCharsets.UTF_8);
          Writer w = Files.newBufferedWriter(Paths.get("out.dot"), StandardCharsets.UTF_8);
           Util.toDot(fst, w, false, false);
           w.close();
           System.out.println("Wrote FST to out.dot");
         }
 
        Directory dir = FSDirectory.open(new File(dirOut));
        Directory dir = FSDirectory.open(dirOut);
         IndexOutput out = dir.createOutput("fst.bin", IOContext.DEFAULT);
         fst.save(out);
         out.close();
@@ -548,7 +547,7 @@ public class TestFSTs extends LuceneTestCase {
         while(true) {
           for(int iter=0;iter<2;iter++) {
             is.close();
            is = new BufferedReader(new InputStreamReader(new FileInputStream(wordsFileIn), StandardCharsets.UTF_8), 65536);
            is = Files.newBufferedReader(wordsFileIn, StandardCharsets.UTF_8);
 
             ord = 0;
             tStart = System.currentTimeMillis();
@@ -622,8 +621,8 @@ public class TestFSTs extends LuceneTestCase {
     boolean verify = true;
     boolean doPack = false;
     boolean noArcArrays = false;
    String wordsFileIn = null;
    String dirOut = null;
    Path wordsFileIn = null;
    Path dirOut = null;
 
     int idx = 0;
     while (idx < args.length) {
@@ -652,9 +651,9 @@ public class TestFSTs extends LuceneTestCase {
         System.exit(-1);
       } else {
         if (wordsFileIn == null) {
          wordsFileIn = args[idx];
          wordsFileIn = Paths.get(args[idx]);
         } else if (dirOut == null) {
          dirOut = args[idx];
          dirOut = Paths.get(args[idx]);
         } else {
           System.err.println("Too many arguments, expected: input [output]");
           System.exit(-1);
diff --git a/lucene/core/src/test/org/apache/lucene/util/junitcompat/TestLeaveFilesIfTestFails.java b/lucene/core/src/test/org/apache/lucene/util/junitcompat/TestLeaveFilesIfTestFails.java
index 8b479163302..48234622245 100644
-- a/lucene/core/src/test/org/apache/lucene/util/junitcompat/TestLeaveFilesIfTestFails.java
++ b/lucene/core/src/test/org/apache/lucene/util/junitcompat/TestLeaveFilesIfTestFails.java
@@ -17,15 +17,15 @@ package org.apache.lucene.util.junitcompat;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SeekableByteChannel;
 import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
 
 import org.apache.lucene.util.Constants;
 import org.apache.lucene.util.IOUtils;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
 import org.junit.Assert;
 import org.junit.Test;
 import org.junit.runner.JUnitCore;
@@ -39,7 +39,7 @@ public class TestLeaveFilesIfTestFails extends WithNestedTests {
   }
   
   public static class Nested1 extends WithNestedTests.AbstractNestedTest {
    static File file;
    static Path file;
     public void testDummy() {
       file = createTempDir("leftover");
       fail();
@@ -50,19 +50,19 @@ public class TestLeaveFilesIfTestFails extends WithNestedTests {
   public void testLeaveFilesIfTestFails() throws IOException {
     Result r = JUnitCore.runClasses(Nested1.class);
     Assert.assertEquals(1, r.getFailureCount());
    Assert.assertTrue(Nested1.file != null && Nested1.file.exists());
    Files.delete(Nested1.file.toPath());
    Assert.assertTrue(Nested1.file != null && Files.exists(Nested1.file));
    Files.delete(Nested1.file);
   }
   
   public static class Nested2 extends WithNestedTests.AbstractNestedTest {
    static File file;
    static File parent;
    static RandomAccessFile openFile;
    static Path file;
    static Path parent;
    static SeekableByteChannel openFile;
 
     @SuppressWarnings("deprecation")
     public void testDummy() throws Exception {
      file = new File(createTempDir("leftover"), "child.locked");
      openFile = new RandomAccessFile(file, "rw");
      file = createTempDir("leftover").resolve("child.locked");
      openFile = Files.newByteChannel(file, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
 
       parent = LuceneTestCase.getBaseTempDirForTestClass();
     }
diff --git a/lucene/demo/src/java/org/apache/lucene/demo/IndexFiles.java b/lucene/demo/src/java/org/apache/lucene/demo/IndexFiles.java
index 2993e600d0b..8c324b70f1f 100644
-- a/lucene/demo/src/java/org/apache/lucene/demo/IndexFiles.java
++ b/lucene/demo/src/java/org/apache/lucene/demo/IndexFiles.java
@@ -32,12 +32,16 @@ import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
 import java.io.IOException;
import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
 import java.util.Date;
 
 /** Index all text files under a directory.
@@ -75,9 +79,9 @@ public class IndexFiles {
       System.exit(1);
     }
 
    final File docDir = new File(docsPath);
    if (!docDir.exists() || !docDir.canRead()) {
      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
       System.exit(1);
     }
     
@@ -85,7 +89,7 @@ public class IndexFiles {
     try {
       System.out.println("Indexing to directory '" + indexPath + "'...");
 
      Directory dir = FSDirectory.open(new File(indexPath));
      Directory dir = FSDirectory.open(Paths.get(indexPath));
       Analyzer analyzer = new StandardAnalyzer();
       IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
 
@@ -139,74 +143,65 @@ public class IndexFiles {
    * >WriteLineDocTask</a>.
    *  
    * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @param path The file to index, or the directory to recurse into to find files to index
    * @throws IOException If there is a low-level I/O error
    */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {
            // don't index files that can't be read.
           }
          return FileVisitResult.CONTINUE;
         }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }
 
        try {

          // make a new, empty document
          Document doc = new Document();

          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize 
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
          doc.add(pathField);

          // Add the last modified date of the file a field named "modified".
          // Use a LongField that is indexed (i.e. efficiently filterable with
          // NumericRangeFilter).  This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));

          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))));

          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.getPath()), doc);
          }
          
        } finally {
          fis.close();
        }
  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongField that is indexed (i.e. efficiently filterable with
      // NumericRangeFilter).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongField("modified", lastModified, Field.Store.NO));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
      doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
      
      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
       }
     }
   }
diff --git a/lucene/demo/src/java/org/apache/lucene/demo/SearchFiles.java b/lucene/demo/src/java/org/apache/lucene/demo/SearchFiles.java
index 33db27f4d32..daade5c22eb 100644
-- a/lucene/demo/src/java/org/apache/lucene/demo/SearchFiles.java
++ b/lucene/demo/src/java/org/apache/lucene/demo/SearchFiles.java
@@ -18,16 +18,15 @@ package org.apache.lucene.demo;
  */
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
 import java.util.Date;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.StoredDocument;
@@ -88,13 +87,13 @@ public class SearchFiles {
       }
     }
     
    IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
     IndexSearcher searcher = new IndexSearcher(reader);
     Analyzer analyzer = new StandardAnalyzer();
 
     BufferedReader in = null;
     if (queries != null) {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8));
      in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
     } else {
       in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
     }
diff --git a/lucene/demo/src/test/org/apache/lucene/demo/TestDemo.java b/lucene/demo/src/test/org/apache/lucene/demo/TestDemo.java
index b7590336fb6..a51231963cd 100644
-- a/lucene/demo/src/test/org/apache/lucene/demo/TestDemo.java
++ b/lucene/demo/src/test/org/apache/lucene/demo/TestDemo.java
@@ -21,19 +21,20 @@ import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.PrintStream;
 import java.nio.charset.Charset;
import java.nio.file.Path;
 
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.util.LuceneTestCase.SuppressSysoutChecks;
 
 public class TestDemo extends LuceneTestCase {
 
  private void testOneSearch(File indexPath, String query, int expectedHitCount) throws Exception {
  private void testOneSearch(Path indexPath, String query, int expectedHitCount) throws Exception {
     PrintStream outSave = System.out;
     try {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       PrintStream fakeSystemOut = new PrintStream(bytes, false, Charset.defaultCharset().name());
       System.setOut(fakeSystemOut);
      SearchFiles.main(new String[] {"-query", query, "-index", indexPath.getPath()});
      SearchFiles.main(new String[] {"-query", query, "-index", indexPath.toString()});
       fakeSystemOut.flush();
       String output = bytes.toString(Charset.defaultCharset().name()); // intentionally use default encoding
       assertTrue("output=" + output, output.contains(expectedHitCount + " total matching documents"));
@@ -43,9 +44,9 @@ public class TestDemo extends LuceneTestCase {
   }
 
   public void testIndexSearch() throws Exception {
    File dir = getDataFile("test-files/docs");
    File indexDir = createTempDir("ContribDemoTest");
    IndexFiles.main(new String[] { "-create", "-docs", dir.getPath(), "-index", indexDir.getPath()});
    Path dir = getDataPath("test-files/docs");
    Path indexDir = createTempDir("ContribDemoTest");
    IndexFiles.main(new String[] { "-create", "-docs", dir.toString(), "-index", indexDir.toString()});
     testOneSearch(indexDir, "apache", 3);
     testOneSearch(indexDir, "patent", 8);
     testOneSearch(indexDir, "lucene", 0);
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/PrintTaxonomyStats.java b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/PrintTaxonomyStats.java
index e30e534fc19..102e47c9b3c 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/PrintTaxonomyStats.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/PrintTaxonomyStats.java
@@ -17,9 +17,9 @@ package org.apache.lucene.facet.taxonomy;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
import java.nio.file.Paths;
 
 import org.apache.lucene.facet.taxonomy.TaxonomyReader.ChildrenIterator;
 import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
@@ -50,7 +50,7 @@ public class PrintTaxonomyStats {
       System.out.println("\nUsage: java -classpath ... org.apache.lucene.facet.util.PrintTaxonomyStats [-printTree] /path/to/taxononmy/index\n");
       System.exit(1);
     }
    Directory dir = FSDirectory.open(new File(path));
    Directory dir = FSDirectory.open(Paths.get(path));
     TaxonomyReader r = new DirectoryTaxonomyReader(dir);
     printStats(r, System.out, printTree);
     r.close();
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.java b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.java
index ca334204542..2bde43de53e 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/directory/DirectoryTaxonomyWriter.java
@@ -4,12 +4,9 @@ import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
@@ -891,14 +888,14 @@ public class DirectoryTaxonomyWriter implements TaxonomyWriter {
    * {@link OrdinalMap} maintained on file system
    */
   public static final class DiskOrdinalMap implements OrdinalMap {
    File tmpfile;
    Path tmpfile;
     DataOutputStream out;
 
     /** Sole constructor. */
    public DiskOrdinalMap(File tmpfile) throws FileNotFoundException {
    public DiskOrdinalMap(Path tmpfile) throws IOException {
       this.tmpfile = tmpfile;
       out = new DataOutputStream(new BufferedOutputStream(
          new FileOutputStream(tmpfile)));
          Files.newOutputStream(tmpfile)));
     }
 
     @Override
@@ -929,7 +926,7 @@ public class DirectoryTaxonomyWriter implements TaxonomyWriter {
       }
       addDone(); // in case this wasn't previously called
       DataInputStream in = new DataInputStream(new BufferedInputStream(
          new FileInputStream(tmpfile)));
          Files.newInputStream(tmpfile)));
       map = new int[in.readInt()];
       // NOTE: The current code assumes here that the map is complete,
       // i.e., every ordinal gets one and exactly one value. Otherwise,
@@ -942,7 +939,7 @@ public class DirectoryTaxonomyWriter implements TaxonomyWriter {
       in.close();
 
       // Delete the temporary file, which is no longer needed.
      Files.delete(tmpfile.toPath());
      Files.delete(tmpfile);
 
       return map;
     }
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/writercache/CompactLabelToOrdinal.java b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/writercache/CompactLabelToOrdinal.java
index a9af612b0d1..68922a4045d 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/writercache/CompactLabelToOrdinal.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/taxonomy/writercache/CompactLabelToOrdinal.java
@@ -21,10 +21,10 @@ import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Iterator;
 
 import org.apache.lucene.facet.taxonomy.FacetLabel;
@@ -352,9 +352,9 @@ public class CompactLabelToOrdinal extends LabelToOrdinal {
 
   /**
    * Opens the file and reloads the CompactLabelToOrdinal. The file it expects
   * is generated from the {@link #flush(File)} command.
   * is generated from the {@link #flush(Path)} command.
    */
  static CompactLabelToOrdinal open(File file, float loadFactor,
  static CompactLabelToOrdinal open(Path file, float loadFactor,
                                     int numHashArrays) throws IOException {
     /**
      * Part of the file is the labelRepository, which needs to be rehashed
@@ -369,7 +369,7 @@ public class CompactLabelToOrdinal extends LabelToOrdinal {
     DataInputStream dis = null;
     try {
       dis = new DataInputStream(new BufferedInputStream(
          new FileInputStream(file)));
          Files.newInputStream(file)));
 
       // TaxiReader needs to load the "counter" or occupancy (L2O) to know
       // the next unique facet. we used to load the delimiter too, but
@@ -433,8 +433,8 @@ public class CompactLabelToOrdinal extends LabelToOrdinal {
 
   }
 
  void flush(File file) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
  void flush(Path file) throws IOException {
    OutputStream fos = Files.newOutputStream(file);
 
     try {
       BufferedOutputStream os = new BufferedOutputStream(fos);
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCharBlockArray.java b/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCharBlockArray.java
index 0c2045a7742..e0b1289196f 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCharBlockArray.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCharBlockArray.java
@@ -2,13 +2,12 @@ package org.apache.lucene.facet.taxonomy.writercache;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.nio.ByteBuffer;
 import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CodingErrorAction;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.facet.FacetTestCase;
 import org.apache.lucene.util.TestUtil;
@@ -85,14 +84,14 @@ public class TestCharBlockArray extends FacetTestCase {
 
     assertEqualsInternal("GrowingCharArray<->StringBuilder mismatch.", builder, array);
 
    File tempDir = createTempDir("growingchararray");
    File f = new File(tempDir, "GrowingCharArrayTest.tmp");
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
    Path tempDir = createTempDir("growingchararray");
    Path f = tempDir.resolve("GrowingCharArrayTest.tmp");
    BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(f));
     array.flush(out);
     out.flush();
     out.close();
 
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
    BufferedInputStream in = new BufferedInputStream(Files.newInputStream(f));
     array = CharBlockArray.open(in);
     assertEqualsInternal("GrowingCharArray<->StringBuilder mismatch after flush/load.", builder, array);
     in.close();
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCompactLabelToOrdinal.java b/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCompactLabelToOrdinal.java
index 6b1097ed02b..982664544df 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCompactLabelToOrdinal.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/taxonomy/writercache/TestCompactLabelToOrdinal.java
@@ -1,18 +1,17 @@
 package org.apache.lucene.facet.taxonomy.writercache;
 
import java.io.File;
 import java.nio.ByteBuffer;
 import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CodingErrorAction;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Random;
 
 import org.apache.lucene.facet.FacetTestCase;
 import org.apache.lucene.facet.taxonomy.FacetLabel;
import org.apache.lucene.util.TestUtil;
 
 import org.junit.Test;
 
@@ -69,15 +68,15 @@ public class TestCompactLabelToOrdinal extends FacetTestCase {
       }
     }
 
    File tmpDir = createTempDir("testLableToOrdinal");
    File f = new File(tmpDir, "CompactLabelToOrdinalTest.tmp");
    Path tmpDir = createTempDir("testLableToOrdinal");
    Path f = tmpDir.resolve("CompactLabelToOrdinalTest.tmp");
     int flushInterval = 10;
 
     for (int i = 0; i < n; i++) {
       if (i > 0 && i % flushInterval == 0) {
         compact.flush(f);    
         compact = CompactLabelToOrdinal.open(f, 0.15f, 3);
        Files.delete(f.toPath());
        Files.delete(f);
         if (flushInterval < (n / 10)) {
           flushInterval *= 10;
         }
diff --git a/lucene/misc/build.xml b/lucene/misc/build.xml
index b5ee7b2a5f4..cf95d8cd1b8 100644
-- a/lucene/misc/build.xml
++ b/lucene/misc/build.xml
@@ -23,6 +23,11 @@
     Index tools and other miscellaneous code
   </description>
 
  <property name="forbidden-base-excludes" value="
    org/apache/lucene/store/NativeUnixDirectory$NativeUnixIndexInput.class
    org/apache/lucene/store/NativeUnixDirectory$NativeUnixIndexOutput.class
  "/>

   <property name="forbidden-sysout-excludes" value="
     org/apache/lucene/index/CompoundFileExtractor.class
     org/apache/lucene/index/IndexSplitter.class
diff --git a/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java b/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
index f12d5f8e42f..a78430f0ac3 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
++ b/lucene/misc/src/java/org/apache/lucene/index/CompoundFileExtractor.java
@@ -25,9 +25,11 @@ package org.apache.lucene.index;
  * @param args Usage: org.apache.lucene.index.IndexReader [-extract] &lt;cfsfile&gt;
  */
 
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
 import org.apache.lucene.store.CompoundFileDirectory;
 import org.apache.lucene.store.Directory;
@@ -75,13 +77,13 @@ public class CompoundFileExtractor {
     IOContext context = IOContext.READ;
 
     try {
      File file = new File(filename);
      String dirname = file.getAbsoluteFile().getParent();
      filename = file.getName();
      Path file = Paths.get(filename);
      Path directory = file.toAbsolutePath().getParent();
      filename = file.getFileName().toString();
       if (dirImpl == null) {
        dir = FSDirectory.open(new File(dirname));
        dir = FSDirectory.open(directory);
       } else {
        dir = CommandLineUtil.newFSDirectory(dirImpl, new File(dirname));
        dir = CommandLineUtil.newFSDirectory(dirImpl, directory);
       }
       
       cfr = new CompoundFileDirectory(dir, filename, IOContext.DEFAULT, false);
@@ -96,7 +98,7 @@ public class CompoundFileExtractor {
           System.out.println("extract " + files[i] + " with " + len + " bytes to local directory...");
           IndexInput ii = cfr.openInput(files[i], context);
 
          FileOutputStream f = new FileOutputStream(files[i]);
          OutputStream f = Files.newOutputStream(Paths.get(files[i]));
 
           // read and write with a small buffer, which is more effective than reading byte by byte
           byte[] buffer = new byte[1024];
diff --git a/lucene/misc/src/java/org/apache/lucene/index/IndexSplitter.java b/lucene/misc/src/java/org/apache/lucene/index/IndexSplitter.java
index a3c028728cb..849a85aa7e8 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/IndexSplitter.java
++ b/lucene/misc/src/java/org/apache/lucene/index/IndexSplitter.java
@@ -16,12 +16,10 @@
  */
 package org.apache.lucene.index;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.text.DecimalFormat;
 import java.text.DecimalFormatSymbols;
 import java.util.ArrayList;
@@ -53,7 +51,7 @@ public class IndexSplitter {
 
   FSDirectory fsDir;
 
  File dir;
  Path dir;
 
   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
@@ -64,10 +62,10 @@ public class IndexSplitter {
           .println("IndexSplitter <srcDir> -d (delete the following segments)");
       return;
     }
    File srcDir = new File(args[0]);
    Path srcDir = Paths.get(args[0]);
     IndexSplitter is = new IndexSplitter(srcDir);
    if (!srcDir.exists()) {
      throw new Exception("srcdir:" + srcDir.getAbsolutePath()
    if (!Files.exists(srcDir)) {
      throw new Exception("srcdir:" + srcDir.toAbsolutePath()
           + " doesn't exist");
     }
     if (args[1].equals("-l")) {
@@ -79,7 +77,7 @@ public class IndexSplitter {
       }
       is.remove(segs.toArray(new String[0]));
     } else {
      File targetDir = new File(args[1]);
      Path targetDir = Paths.get(args[1]);
       List<String> segs = new ArrayList<>();
       for (int x = 2; x < args.length; x++) {
         segs.add(args[x]);
@@ -88,7 +86,7 @@ public class IndexSplitter {
     }
   }
   
  public IndexSplitter(File dir) throws IOException {
  public IndexSplitter(Path dir) throws IOException {
     this.dir = dir;
     fsDir = FSDirectory.open(dir);
     infos = new SegmentInfos();
@@ -129,8 +127,8 @@ public class IndexSplitter {
     infos.commit(fsDir);
   }
 
  public void split(File destDir, String[] segs) throws IOException {
    destDir.mkdirs();
  public void split(Path destDir, String[] segs) throws IOException {
    Files.createDirectories(destDir);
     FSDirectory destFSDir = FSDirectory.open(destDir);
     SegmentInfos destInfos = new SegmentInfos();
     destInfos.counter = infos.counter;
@@ -146,26 +144,13 @@ public class IndexSplitter {
       // now copy files over
       Collection<String> files = infoPerCommit.files();
       for (final String srcName : files) {
        File srcFile = new File(dir, srcName);
        File destFile = new File(destDir, srcName);
        copyFile(srcFile, destFile);
        Path srcFile = dir.resolve(srcName);
        Path destFile = destDir.resolve(srcName);
        Files.copy(srcFile, destFile);
       }
     }
     destInfos.changed();
     destInfos.commit(destFSDir);
     // System.out.println("destDir:"+destDir.getAbsolutePath());
   }

  private static final byte[] copyBuffer = new byte[32*1024];

  private static void copyFile(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);
    int len;
    while ((len = in.read(copyBuffer)) > 0) {
      out.write(copyBuffer, 0, len);
    }
    in.close();
    out.close();
  }
 }
diff --git a/lucene/misc/src/java/org/apache/lucene/index/MultiPassIndexSplitter.java b/lucene/misc/src/java/org/apache/lucene/index/MultiPassIndexSplitter.java
index 746c26b8ff3..f41361b5d7f 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/MultiPassIndexSplitter.java
++ b/lucene/misc/src/java/org/apache/lucene/index/MultiPassIndexSplitter.java
@@ -17,8 +17,10 @@ package org.apache.lucene.index;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.List;
 
@@ -128,12 +130,12 @@ public class MultiPassIndexSplitter {
       } else if (args[i].equals("-seq")) {
         seq = true;
       } else {
        File file = new File(args[i]);
        if (!file.exists() || !file.isDirectory()) {
        Path file = Paths.get(args[i]);
        if (!Files.isDirectory(file)) {
           System.err.println("Invalid input path - skipping: " + file);
           continue;
         }
        Directory dir = FSDirectory.open(new File(args[i]));
        Directory dir = FSDirectory.open(file);
         try {
           if (!DirectoryReader.indexExists(dir)) {
             System.err.println("Invalid input index - skipping: " + file);
@@ -155,13 +157,11 @@ public class MultiPassIndexSplitter {
     if (indexes.size() == 0) {
       throw new Exception("No input indexes to process");
     }
    File out = new File(outDir);
    if (!out.mkdirs()) {
      throw new Exception("Can't create output directory: " + out);
    }
    Path out = Paths.get(outDir);
    Files.createDirectories(out);
     Directory[] dirs = new Directory[numParts];
     for (int i = 0; i < numParts; i++) {
      dirs[i] = FSDirectory.open(new File(out, "part-" + i));
      dirs[i] = FSDirectory.open(out.resolve("part-" + i));
     }
     MultiPassIndexSplitter splitter = new MultiPassIndexSplitter();
     IndexReader input;
diff --git a/lucene/misc/src/java/org/apache/lucene/misc/GetTermInfo.java b/lucene/misc/src/java/org/apache/lucene/misc/GetTermInfo.java
index b6f202dc3ee..9b5f86662ea 100644
-- a/lucene/misc/src/java/org/apache/lucene/misc/GetTermInfo.java
++ b/lucene/misc/src/java/org/apache/lucene/misc/GetTermInfo.java
@@ -17,7 +17,7 @@ package org.apache.lucene.misc;
  * limitations under the License.
  */
 
import java.io.File;
import java.nio.file.Paths;
 import java.util.Locale;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
@@ -37,7 +37,7 @@ public class GetTermInfo {
     String field = null;
     
     if (args.length == 3) {
      dir = FSDirectory.open(new File(args[0]));
      dir = FSDirectory.open(Paths.get(args[0]));
       field = args[1];
       inputStr = args[2];
     } else {
diff --git a/lucene/misc/src/java/org/apache/lucene/misc/HighFreqTerms.java b/lucene/misc/src/java/org/apache/lucene/misc/HighFreqTerms.java
index d1139c9ee01..161f7f20af2 100644
-- a/lucene/misc/src/java/org/apache/lucene/misc/HighFreqTerms.java
++ b/lucene/misc/src/java/org/apache/lucene/misc/HighFreqTerms.java
@@ -28,8 +28,8 @@ import org.apache.lucene.store.FSDirectory;
 import org.apache.lucene.util.PriorityQueue;
 import org.apache.lucene.util.BytesRef;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Paths;
 import java.util.Comparator;
 import java.util.Locale;
 
@@ -56,7 +56,7 @@ public class HighFreqTerms {
       System.exit(1);
     }     
 
    Directory dir = FSDirectory.open(new File(args[0]));
    Directory dir = FSDirectory.open(Paths.get(args[0]));
     
     Comparator<TermStats> comparator = new DocFreqComparator();
    
diff --git a/lucene/misc/src/java/org/apache/lucene/misc/IndexMergeTool.java b/lucene/misc/src/java/org/apache/lucene/misc/IndexMergeTool.java
index 90f14550d83..780ee7a5bb7 100644
-- a/lucene/misc/src/java/org/apache/lucene/misc/IndexMergeTool.java
++ b/lucene/misc/src/java/org/apache/lucene/misc/IndexMergeTool.java
@@ -22,8 +22,8 @@ import org.apache.lucene.index.IndexWriterConfig.OpenMode;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.FSDirectory;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Paths;
 
 /**
  * Merges indices specified on the command line into the index
@@ -35,14 +35,14 @@ public class IndexMergeTool {
       System.err.println("Usage: IndexMergeTool <mergedIndex> <index1> <index2> [index3] ...");
       System.exit(1);
     }
    FSDirectory mergedIndex = FSDirectory.open(new File(args[0]));
    FSDirectory mergedIndex = FSDirectory.open(Paths.get(args[0]));
 
     IndexWriter writer = new IndexWriter(mergedIndex, new IndexWriterConfig(null)
         .setOpenMode(OpenMode.CREATE));
 
     Directory[] indexes = new Directory[args.length - 1];
     for (int i = 1; i < args.length; i++) {
      indexes[i  - 1] = FSDirectory.open(new File(args[i]));
      indexes[i  - 1] = FSDirectory.open(Paths.get(args[i]));
     }
 
     System.out.println("Merging...");
diff --git a/lucene/misc/src/java/org/apache/lucene/store/NativeUnixDirectory.java b/lucene/misc/src/java/org/apache/lucene/store/NativeUnixDirectory.java
index e951f7d116d..92eb062faf5 100644
-- a/lucene/misc/src/java/org/apache/lucene/store/NativeUnixDirectory.java
++ b/lucene/misc/src/java/org/apache/lucene/store/NativeUnixDirectory.java
@@ -18,13 +18,13 @@ package org.apache.lucene.store;
  */
 
 import java.io.EOFException;
import java.io.File;
 import java.io.IOException;
 import java.io.FileInputStream;
 import java.io.FileDescriptor;
 import java.io.FileOutputStream;
 import java.nio.ByteBuffer;
 import java.nio.channels.FileChannel;
import java.nio.file.Path;
 
 import org.apache.lucene.store.Directory; // javadoc
 import org.apache.lucene.store.IOContext.Context;
@@ -99,7 +99,7 @@ public class NativeUnixDirectory extends FSDirectory {
    * @param delegate fallback Directory for non-merges
    * @throws IOException If there is a low-level I/O error
    */
  public NativeUnixDirectory(File path, int mergeBufferSize, long minBytesDirect, Directory delegate) throws IOException {
  public NativeUnixDirectory(Path path, int mergeBufferSize, long minBytesDirect, Directory delegate) throws IOException {
     super(path, delegate.getLockFactory());
     if ((mergeBufferSize & ALIGN) != 0) {
       throw new IllegalArgumentException("mergeBufferSize must be 0 mod " + ALIGN + " (got: " + mergeBufferSize + ")");
@@ -115,7 +115,7 @@ public class NativeUnixDirectory extends FSDirectory {
    * @param delegate fallback Directory for non-merges
    * @throws IOException If there is a low-level I/O error
    */
  public NativeUnixDirectory(File path, Directory delegate) throws IOException {
  public NativeUnixDirectory(Path path, Directory delegate) throws IOException {
     this(path, DEFAULT_MERGE_BUFFER_SIZE, DEFAULT_MIN_BYTES_DIRECT, delegate);
   }  
 
@@ -125,7 +125,7 @@ public class NativeUnixDirectory extends FSDirectory {
     if (context.context != Context.MERGE || context.mergeInfo.estimatedMergeBytes < minBytesDirect || fileLength(name) < minBytesDirect) {
       return delegate.openInput(name, context);
     } else {
      return new NativeUnixIndexInput(new File(getDirectory(), name), mergeBufferSize);
      return new NativeUnixIndexInput(getDirectory().resolve(name), mergeBufferSize);
     }
   }
 
@@ -136,7 +136,7 @@ public class NativeUnixDirectory extends FSDirectory {
       return delegate.createOutput(name, context);
     } else {
       ensureCanWrite(name);
      return new NativeUnixIndexOutput(new File(getDirectory(), name), mergeBufferSize);
      return new NativeUnixIndexOutput(getDirectory().resolve(name), mergeBufferSize);
     }
   }
 
@@ -153,7 +153,7 @@ public class NativeUnixDirectory extends FSDirectory {
     private long fileLength;
     private boolean isOpen;
 
    public NativeUnixIndexOutput(File path, int bufferSize) throws IOException {
    public NativeUnixIndexOutput(Path path, int bufferSize) throws IOException {
       //this.path = path;
       final FileDescriptor fd = NativePosixUtil.open_direct(path.toString(), false);
       fos = new FileOutputStream(fd);
@@ -271,8 +271,8 @@ public class NativeUnixDirectory extends FSDirectory {
     private long filePos;
     private int bufferPos;
 
    public NativeUnixIndexInput(File path, int bufferSize) throws IOException {
      super("NativeUnixIndexInput(path=\"" + path.getPath() + "\")");
    public NativeUnixIndexInput(Path path, int bufferSize) throws IOException {
      super("NativeUnixIndexInput(path=\"" + path + "\")");
       final FileDescriptor fd = NativePosixUtil.open_direct(path.toString(), true);
       fis = new FileInputStream(fd);
       channel = fis.getChannel();
diff --git a/lucene/misc/src/java/org/apache/lucene/store/WindowsDirectory.java b/lucene/misc/src/java/org/apache/lucene/store/WindowsDirectory.java
index f635fe37ccd..58940851e5b 100644
-- a/lucene/misc/src/java/org/apache/lucene/store/WindowsDirectory.java
++ b/lucene/misc/src/java/org/apache/lucene/store/WindowsDirectory.java
@@ -17,9 +17,9 @@ package org.apache.lucene.store;
  * the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.EOFException;
import java.nio.file.Path;
 
 import org.apache.lucene.store.Directory; // javadoc
 import org.apache.lucene.store.NativeFSLockFactory; // javadoc
@@ -56,7 +56,7 @@ public class WindowsDirectory extends FSDirectory {
    * ({@link NativeFSLockFactory});
    * @throws IOException If there is a low-level I/O error
    */
  public WindowsDirectory(File path, LockFactory lockFactory) throws IOException {
  public WindowsDirectory(Path path, LockFactory lockFactory) throws IOException {
     super(path, lockFactory);
   }
 
@@ -65,14 +65,14 @@ public class WindowsDirectory extends FSDirectory {
    * @param path the path of the directory
    * @throws IOException If there is a low-level I/O error
    */
  public WindowsDirectory(File path) throws IOException {
  public WindowsDirectory(Path path) throws IOException {
     super(path, null);
   }
 
   @Override
   public IndexInput openInput(String name, IOContext context) throws IOException {
     ensureOpen();
    return new WindowsIndexInput(new File(getDirectory(), name), Math.max(BufferedIndexInput.bufferSize(context), DEFAULT_BUFFERSIZE));
    return new WindowsIndexInput(getDirectory().resolve(name), Math.max(BufferedIndexInput.bufferSize(context), DEFAULT_BUFFERSIZE));
   }
   
   static class WindowsIndexInput extends BufferedIndexInput {
@@ -81,9 +81,9 @@ public class WindowsDirectory extends FSDirectory {
     boolean isClone;
     boolean isOpen;
     
    public WindowsIndexInput(File file, int bufferSize) throws IOException {
      super("WindowsIndexInput(path=\"" + file.getPath() + "\")", bufferSize);
      fd = WindowsDirectory.open(file.getPath());
    public WindowsIndexInput(Path file, int bufferSize) throws IOException {
      super("WindowsIndexInput(path=\"" + file + "\")", bufferSize);
      fd = WindowsDirectory.open(file.toString());
       length = WindowsDirectory.length(fd);
       isOpen = true;
     }
diff --git a/lucene/misc/src/test/org/apache/lucene/index/TestIndexSplitter.java b/lucene/misc/src/test/org/apache/lucene/index/TestIndexSplitter.java
index 0206bd9bbcc..8ad652dbc9f 100644
-- a/lucene/misc/src/test/org/apache/lucene/index/TestIndexSplitter.java
++ b/lucene/misc/src/test/org/apache/lucene/index/TestIndexSplitter.java
@@ -16,7 +16,7 @@
  */
 package org.apache.lucene.index;
 
import java.io.File;
import java.nio.file.Path;
 
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.document.Document;
@@ -27,8 +27,8 @@ import org.apache.lucene.util.LuceneTestCase;
 
 public class TestIndexSplitter extends LuceneTestCase {
   public void test() throws Exception {
    File dir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    File destDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    Path dir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    Path destDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
     Directory fsDir = newFSDirectory(dir);
     // IndexSplitter.split makes its own commit directly with SIPC/SegmentInfos,
     // so the unreferenced files are expected.
@@ -75,17 +75,17 @@ public class TestIndexSplitter extends LuceneTestCase {
     fsDirDest.close();
     
     // now test cmdline
    File destDir2 = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    IndexSplitter.main(new String[] {dir.getAbsolutePath(), destDir2.getAbsolutePath(), splitSegName});
    assertEquals(4, destDir2.listFiles().length);
    Path destDir2 = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    IndexSplitter.main(new String[] {dir.toAbsolutePath().toString(), destDir2.toAbsolutePath().toString(), splitSegName});
     Directory fsDirDest2 = newFSDirectory(destDir2);
    assertEquals(4, fsDirDest2.listAll().length);
     r = DirectoryReader.open(fsDirDest2);
     assertEquals(50, r.maxDoc());
     r.close();
     fsDirDest2.close();
     
     // now remove the copied segment from src
    IndexSplitter.main(new String[] {dir.getAbsolutePath(), "-d", splitSegName});
    IndexSplitter.main(new String[] {dir.toAbsolutePath().toString(), "-d", splitSegName});
     r = DirectoryReader.open(fsDir);
     assertEquals(2, r.leaves().size());
     r.close();
diff --git a/lucene/replicator/src/java/org/apache/lucene/replicator/PerSessionDirectoryFactory.java b/lucene/replicator/src/java/org/apache/lucene/replicator/PerSessionDirectoryFactory.java
index fe43752b674..794ab041625 100644
-- a/lucene/replicator/src/java/org/apache/lucene/replicator/PerSessionDirectoryFactory.java
++ b/lucene/replicator/src/java/org/apache/lucene/replicator/PerSessionDirectoryFactory.java
@@ -17,8 +17,9 @@ package org.apache.lucene.replicator;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.replicator.ReplicationClient.SourceDirectoryFactory;
 import org.apache.lucene.store.Directory;
@@ -34,23 +35,19 @@ import org.apache.lucene.util.IOUtils;
  */
 public class PerSessionDirectoryFactory implements SourceDirectoryFactory {
   
  private final File workDir;
  private final Path workDir;
   
   /** Constructor with the given sources mapping. */
  public PerSessionDirectoryFactory(File workDir) {
  public PerSessionDirectoryFactory(Path workDir) {
     this.workDir = workDir;
   }
   
   @Override
   public Directory getDirectory(String sessionID, String source) throws IOException {
    File sessionDir = new File(workDir, sessionID);
    if (!sessionDir.exists() && !sessionDir.mkdirs()) {
      throw new IOException("failed to create session directory " + sessionDir);
    }
    File sourceDir = new File(sessionDir, source);
    if (!sourceDir.mkdirs()) {
      throw new IOException("failed to create source directory " + sourceDir);
    }
    Path sessionDir = workDir.resolve(sessionID);
    Files.createDirectories(sessionDir);
    Path sourceDir = sessionDir.resolve(source);
    Files.createDirectories(sourceDir);
     return FSDirectory.open(sourceDir);
   }
   
@@ -59,7 +56,7 @@ public class PerSessionDirectoryFactory implements SourceDirectoryFactory {
     if (sessionID.isEmpty()) { // protect against deleting workDir entirely!
       throw new IllegalArgumentException("sessionID cannot be empty");
     }
    IOUtils.rm(new File(workDir, sessionID));
    IOUtils.rm(workDir.resolve(sessionID));
   }
   
 }
diff --git a/lucene/replicator/src/test/org/apache/lucene/replicator/IndexAndTaxonomyReplicationClientTest.java b/lucene/replicator/src/test/org/apache/lucene/replicator/IndexAndTaxonomyReplicationClientTest.java
index 12ef20aacf7..5871c081ba2 100644
-- a/lucene/replicator/src/test/org/apache/lucene/replicator/IndexAndTaxonomyReplicationClientTest.java
++ b/lucene/replicator/src/test/org/apache/lucene/replicator/IndexAndTaxonomyReplicationClientTest.java
@@ -19,9 +19,9 @@ package org.apache.lucene.replicator;
 
 import java.io.ByteArrayOutputStream;
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
 import java.io.PrintStream;
import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.concurrent.Callable;
 import java.util.concurrent.atomic.AtomicBoolean;
@@ -138,7 +138,7 @@ public class IndexAndTaxonomyReplicationClientTest extends ReplicatorTestCase {
   private SnapshotDirectoryTaxonomyWriter publishTaxoWriter;
   private FacetsConfig config;
   private IndexAndTaxonomyReadyCallback callback;
  private File clientWorkDir;
  private Path clientWorkDir;
   
   private static final String VERSION_ID = "version";
   
diff --git a/lucene/replicator/src/test/org/apache/lucene/replicator/http/HttpReplicatorTest.java b/lucene/replicator/src/test/org/apache/lucene/replicator/http/HttpReplicatorTest.java
index d0b1a03713e..cb9e4661552 100644
-- a/lucene/replicator/src/test/org/apache/lucene/replicator/http/HttpReplicatorTest.java
++ b/lucene/replicator/src/test/org/apache/lucene/replicator/http/HttpReplicatorTest.java
@@ -17,8 +17,8 @@ package org.apache.lucene.replicator.http;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.Collections;
 
 import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
@@ -52,7 +52,7 @@ public class HttpReplicatorTest extends ReplicatorTestCase {
   public TestRule testRules = 
     RuleChain.outerRule(new SystemPropertiesRestoreRule());
 
  private File clientWorkDir;
  private Path clientWorkDir;
   private Replicator serverReplicator;
   private IndexWriter writer;
   private DirectoryReader reader;
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java b/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
index a87ef87a859..6ec352da5f8 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/spell/PlainTextDictionary.java
@@ -18,11 +18,12 @@ package org.apache.lucene.search.spell;
  */
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Reader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 
 import org.apache.lucene.search.suggest.InputIterator;
 import org.apache.lucene.util.BytesRef;
@@ -44,12 +45,12 @@ public class PlainTextDictionary implements Dictionary {
   private BufferedReader in;
 
   /**
   * Creates a dictionary based on a File.
   * Creates a dictionary based on a Path.
    * <p>
    * NOTE: content is treated as UTF-8
    */
  public PlainTextDictionary(File file) throws IOException {
    in = new BufferedReader(IOUtils.getDecodingReader(file, StandardCharsets.UTF_8));
  public PlainTextDictionary(Path path) throws IOException {
    in = Files.newBufferedReader(path, StandardCharsets.UTF_8);
   }
 
   /**
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedInputIterator.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedInputIterator.java
index 3a8ad160c88..d5b4c593107 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedInputIterator.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/SortedInputIterator.java
@@ -17,8 +17,9 @@ package org.apache.lucene.search.suggest;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Set;
@@ -40,8 +41,8 @@ import org.apache.lucene.util.OfflineSorter.ByteSequencesWriter;
 public class SortedInputIterator implements InputIterator {
   
   private final InputIterator source;
  private File tempInput;
  private File tempSorted;
  private Path tempInput;
  private Path tempSorted;
   private final ByteSequencesReader reader;
   private final Comparator<BytesRef> comparator;
   private final boolean hasPayloads;
@@ -168,9 +169,9 @@ public class SortedInputIterator implements InputIterator {
   
   private ByteSequencesReader sort() throws IOException {
     String prefix = getClass().getSimpleName();
    File directory = OfflineSorter.defaultTempDir();
    tempInput = File.createTempFile(prefix, ".input", directory);
    tempSorted = File.createTempFile(prefix, ".sorted", directory);
    Path directory = OfflineSorter.defaultTempDir();
    tempInput = Files.createTempFile(directory, prefix, ".input");
    tempSorted = Files.createTempFile(directory, prefix, ".sorted");
     
     final OfflineSorter.ByteSequencesWriter writer = new OfflineSorter.ByteSequencesWriter(tempInput);
     boolean success = false;
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
index ac1ea730849..eb1f63f8867 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
@@ -18,9 +18,9 @@ package org.apache.lucene.search.suggest.analyzing;
  */
 
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
 import java.io.StringReader;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
@@ -222,7 +222,7 @@ public class AnalyzingInfixSuggester extends Lookup implements Closeable {
 
   /** Subclass can override to choose a specific {@link
    *  Directory} implementation. */
  protected Directory getDirectory(File path) throws IOException {
  protected Directory getDirectory(Path path) throws IOException {
     return FSDirectory.open(path);
   }
 
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggester.java
index f42a70a1c59..6496e3acab8 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggester.java
@@ -17,9 +17,9 @@ package org.apache.lucene.search.suggest.analyzing;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -416,9 +416,9 @@ public class AnalyzingSuggester extends Lookup {
       throw new IllegalArgumentException("this suggester doesn't support contexts");
     }
     String prefix = getClass().getSimpleName();
    File directory = OfflineSorter.defaultTempDir();
    File tempInput = File.createTempFile(prefix, ".input", directory);
    File tempSorted = File.createTempFile(prefix, ".sorted", directory);
    Path directory = OfflineSorter.defaultTempDir();
    Path tempInput = Files.createTempFile(directory, prefix, ".input");
    Path tempSorted = Files.createTempFile(directory, prefix, ".sorted");
 
     hasPayloads = iterator.hasPayloads();
 
@@ -501,7 +501,7 @@ public class AnalyzingSuggester extends Lookup {
       new OfflineSorter(new AnalyzingComparator(hasPayloads)).sort(tempInput, tempSorted);
 
       // Free disk space:
      Files.delete(tempInput.toPath());
      Files.delete(tempInput);
 
       reader = new OfflineSorter.ByteSequencesReader(tempSorted);
      
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/FreeTextSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/FreeTextSuggester.java
index cbcdc901434..255fc1b8695 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/FreeTextSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/FreeTextSuggester.java
@@ -68,6 +68,7 @@ import org.apache.lucene.util.fst.Util.TopResults;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -292,7 +293,7 @@ public class FreeTextSuggester extends Lookup {
     }
 
     String prefix = getClass().getSimpleName();
    File tempIndexPath = Files.createTempDirectory(prefix + ".index.").toFile();
    Path tempIndexPath = Files.createTempDirectory(prefix + ".index.");
 
     Directory dir = FSDirectory.open(tempIndexPath);
 
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
index 78d69af551b..0f18fa50ddd 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/ExternalRefSorter.java
@@ -18,9 +18,9 @@ package org.apache.lucene.search.suggest.fst;
  */
 
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Comparator;
 
 import org.apache.lucene.util.BytesRef;
@@ -36,16 +36,15 @@ import org.apache.lucene.util.OfflineSorter;
 public class ExternalRefSorter implements BytesRefSorter, Closeable {
   private final OfflineSorter sort;
   private OfflineSorter.ByteSequencesWriter writer;
  private File input;
  private File sorted;
  private Path input;
  private Path sorted;
   
   /**
    * Will buffer all sequences to a temporary file and then sort (all on-disk).
    */
   public ExternalRefSorter(OfflineSorter sort) throws IOException {
     this.sort = sort;
    this.input = File.createTempFile("RefSorter-", ".raw",
        OfflineSorter.defaultTempDir());
    this.input = Files.createTempFile(OfflineSorter.defaultTempDir(), "RefSorter-", ".raw");
     this.writer = new OfflineSorter.ByteSequencesWriter(input);
   }
   
@@ -60,15 +59,14 @@ public class ExternalRefSorter implements BytesRefSorter, Closeable {
     if (sorted == null) {
       closeWriter();
       
      sorted = File.createTempFile("RefSorter-", ".sorted",
          OfflineSorter.defaultTempDir());
      sorted = Files.createTempFile(OfflineSorter.defaultTempDir(), "RefSorter-", ".sorted");
       boolean success = false;
       try {
         sort.sort(input, sorted);
         success = true;
       } finally {
         if (success) {
          Files.delete(input.toPath());
          Files.delete(input);
         } else {
           IOUtils.deleteFilesIgnoringExceptions(input);
         }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/FSTCompletionLookup.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/FSTCompletionLookup.java
index 4af480db4f4..84ab69cc02d 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/FSTCompletionLookup.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/fst/FSTCompletionLookup.java
@@ -17,9 +17,9 @@ package org.apache.lucene.search.suggest.fst;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
@@ -158,10 +158,10 @@ public class FSTCompletionLookup extends Lookup implements Accountable {
     if (iterator.hasContexts()) {
       throw new IllegalArgumentException("this suggester doesn't support contexts");
     }
    File tempInput = File.createTempFile(
        FSTCompletionLookup.class.getSimpleName(), ".input", OfflineSorter.defaultTempDir());
    File tempSorted = File.createTempFile(
        FSTCompletionLookup.class.getSimpleName(), ".sorted", OfflineSorter.defaultTempDir());
    Path tempInput = Files.createTempFile(
        OfflineSorter.defaultTempDir(), FSTCompletionLookup.class.getSimpleName(), ".input");
    Path tempSorted = Files.createTempFile(
        OfflineSorter.defaultTempDir(), FSTCompletionLookup.class.getSimpleName(), ".sorted");
 
     OfflineSorter.ByteSequencesWriter writer = new OfflineSorter.ByteSequencesWriter(tempInput);
     OfflineSorter.ByteSequencesReader reader = null;
@@ -190,7 +190,7 @@ public class FSTCompletionLookup extends Lookup implements Accountable {
       // We don't know the distribution of scores and we need to bucket them, so we'll sort
       // and divide into equal buckets.
       SortInfo info = new OfflineSorter().sort(tempInput, tempSorted);
      Files.delete(tempInput.toPath());
      Files.delete(tempInput);
       FSTCompletionBuilder builder = new FSTCompletionBuilder(
           buckets, sorter = new ExternalRefSorter(new OfflineSorter()), sharedTailLength);
 
@@ -235,7 +235,7 @@ public class FSTCompletionLookup extends Lookup implements Accountable {
       IOUtils.closeWhileHandlingException(reader, writer, sorter);
 
       if (success) {
        Files.delete(tempSorted.toPath());
        Files.delete(tempSorted);
       } else {
         IOUtils.deleteFilesIgnoringExceptions(tempInput, tempSorted);
       }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellTernarySearchTrie.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellTernarySearchTrie.java
index 728fe12344f..c93ae9a20c2 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellTernarySearchTrie.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/jaspell/JaspellTernarySearchTrie.java
@@ -30,10 +30,10 @@ package org.apache.lucene.search.suggest.jaspell;
  */
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.List;
 import java.util.Locale;
 import java.util.Vector;
@@ -198,16 +198,16 @@ public class JaspellTernarySearchTrie implements Accountable {
   }
 
   /**
   * Constructs a Ternary Search Trie and loads data from a <code>File</code>
   * Constructs a Ternary Search Trie and loads data from a <code>Path</code>
    * into the Trie. The file is a normal text document, where each line is of
    * the form word TAB float.
    * 
    *@param file
   *          The <code>File</code> with the data to load into the Trie.
   *          The <code>Path</code> with the data to load into the Trie.
    *@exception IOException
    *              A problem occured while reading the data.
    */
  public JaspellTernarySearchTrie(File file) throws IOException {
  public JaspellTernarySearchTrie(Path file) throws IOException {
     this(file, false);
   }
 
@@ -224,15 +224,14 @@ public class JaspellTernarySearchTrie implements Accountable {
    *@exception IOException
    *              A problem occured while reading the data.
    */
  public JaspellTernarySearchTrie(File file, boolean compression)
  public JaspellTernarySearchTrie(Path file, boolean compression)
           throws IOException {
     this();
     BufferedReader in;
     if (compression)
       in = new BufferedReader(IOUtils.getDecodingReader(new GZIPInputStream(
              new FileInputStream(file)), StandardCharsets.UTF_8));
    else in = new BufferedReader(IOUtils.getDecodingReader((new FileInputStream(
            file)), StandardCharsets.UTF_8));
              Files.newInputStream(file)), StandardCharsets.UTF_8));
    else in = Files.newBufferedReader(file, StandardCharsets.UTF_8);
     String word;
     int pos;
     Float occur, one = new Float(1);
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/PersistenceTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/PersistenceTest.java
index b492cf4f29e..690b56c8ce0 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/PersistenceTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/PersistenceTest.java
@@ -16,10 +16,9 @@
  */
 package org.apache.lucene.search.suggest;
 
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Random;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.util.List;
 
 import org.apache.lucene.search.suggest.Lookup.LookupResult;
@@ -70,12 +69,12 @@ public class PersistenceTest extends LuceneTestCase {
     lookup.build(new InputArrayIterator(keys));
 
     // Store the suggester.
    File storeDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    lookup.store(new FileOutputStream(new File(storeDir, "lookup.dat")));
    Path storeDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    lookup.store(Files.newOutputStream(storeDir.resolve("lookup.dat")));
 
     // Re-read it from disk.
     lookup = lookupClass.newInstance();
    lookup.load(new FileInputStream(new File(storeDir, "lookup.dat")));
    lookup.load(Files.newInputStream(storeDir.resolve("lookup.dat")));
 
     // Assert validity.
     Random random = random();
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
index 103d675b520..a7caacf53f6 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
@@ -17,9 +17,9 @@ package org.apache.lucene.search.suggest.analyzing;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
 import java.io.StringReader;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -91,7 +91,7 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       new Input("a penny saved is a penny earned", 10, new BytesRef("foobaz")),
     };
 
    File tempDir = createTempDir("AnalyzingInfixSuggesterTest");
    Path tempDir = createTempDir("AnalyzingInfixSuggesterTest");
 
     Analyzer a = new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false);
     AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(newFSDirectory(tempDir), a, a, 3, false);
@@ -210,7 +210,7 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       new Input("lend me your ear", 8, new BytesRef("foobar")),
       new Input("a penny saved is a penny earned", 10, new BytesRef("foobaz")),
     };
    File tempDir = createTempDir("AnalyzingInfixSuggesterTest");
    Path tempDir = createTempDir("AnalyzingInfixSuggesterTest");
 
     Analyzer a = new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false);
     int minPrefixLength = random().nextInt(10);
@@ -471,7 +471,7 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
   }
 
   public void testRandomNRT() throws Exception {
    final File tempDir = createTempDir("AnalyzingInfixSuggesterTest");
    final Path tempDir = createTempDir("AnalyzingInfixSuggesterTest");
     Analyzer a = new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false);
     int minPrefixChars = random().nextInt(7);
     if (VERBOSE) {
@@ -793,7 +793,7 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
   public void testNRTWithParallelAdds() throws IOException, InterruptedException {
     String[] keys = new String[] {"python", "java", "c", "scala", "ruby", "clojure", "erlang", "go", "swift", "lisp"};
     Analyzer a = new MockAnalyzer(random(), MockTokenizer.WHITESPACE, false);
    File tempDir = createTempDir("AIS_NRT_PERSIST_TEST");
    Path tempDir = createTempDir("AIS_NRT_PERSIST_TEST");
     AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(newFSDirectory(tempDir), a, a, 3, false);
     Thread[] multiAddThreads = new Thread[10];
     try {
@@ -865,7 +865,7 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       new Input("a penny saved is a penny earned", 10, new BytesRef("foobaz"), asSet("foo", "baz"))
     };
 
    File tempDir = createTempDir("analyzingInfixContext");
    Path tempDir = createTempDir("analyzingInfixContext");
 
     for(int iter=0;iter<2;iter++) {
       AnalyzingInfixSuggester suggester;
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggesterTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggesterTest.java
index 6e99ff41887..d7fd5585e3e 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggesterTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggesterTest.java
@@ -17,13 +17,12 @@ package org.apache.lucene.search.suggest.analyzing;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -921,16 +920,15 @@ public class AnalyzingSuggesterTest extends LuceneTestCase {
     assertEquals(3, results.get(2).value);
 
     // Try again after save/load:
    File tmpDir = createTempDir("AnalyzingSuggesterTest");
    tmpDir.mkdir();
    Path tmpDir = createTempDir("AnalyzingSuggesterTest");
 
    File path = new File(tmpDir, "suggester");
    Path path = tmpDir.resolve("suggester");
 
    OutputStream os = new FileOutputStream(path);
    OutputStream os = Files.newOutputStream(path);
     suggester.store(os);
     os.close();
 
    InputStream is = new FileInputStream(path);
    InputStream is = Files.newInputStream(path);
     suggester.load(is);
     is.close();
 
@@ -983,16 +981,15 @@ public class AnalyzingSuggesterTest extends LuceneTestCase {
     assertEquals(5, results.get(1).value);
 
     // Try again after save/load:
    File tmpDir = createTempDir("AnalyzingSuggesterTest");
    tmpDir.mkdir();
    Path tmpDir = createTempDir("AnalyzingSuggesterTest");
 
    File path = new File(tmpDir, "suggester");
    Path path = tmpDir.resolve("suggester");
 
    OutputStream os = new FileOutputStream(path);
    OutputStream os = Files.newOutputStream(path);
     suggester.store(os);
     os.close();
 
    InputStream is = new FileInputStream(path);
    InputStream is = Files.newInputStream(path);
     suggester.load(is);
     is.close();
 
@@ -1053,16 +1050,15 @@ public class AnalyzingSuggesterTest extends LuceneTestCase {
     assertEquals(5, results.get(1).value);
 
     // Try again after save/load:
    File tmpDir = createTempDir("AnalyzingSuggesterTest");
    tmpDir.mkdir();
    Path tmpDir = createTempDir("AnalyzingSuggesterTest");
 
    File path = new File(tmpDir, "suggester");
    Path path = tmpDir.resolve("suggester");
 
    OutputStream os = new FileOutputStream(path);
    OutputStream os = Files.newOutputStream(path);
     suggester.store(os);
     os.close();
 
    InputStream is = new FileInputStream(path);
    InputStream is = Files.newInputStream(path);
     suggester.load(is);
     is.close();
 
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
index c0f7148c951..bd9f08a9108 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
@@ -17,8 +17,8 @@ package org.apache.lucene.search.suggest.analyzing;
  * limitations under the License.
  */
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.List;
 
 import org.apache.lucene.analysis.Analyzer;
@@ -44,7 +44,7 @@ public class BlendedInfixSuggesterTest extends LuceneTestCase {
         new Input("star wars: episode v - the empire strikes back", 8, payload)
     };
 
    File tempDir = createTempDir("BlendedInfixSuggesterTest");
    Path tempDir = createTempDir("BlendedInfixSuggesterTest");
 
     Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);
     BlendedInfixSuggester suggester = new BlendedInfixSuggester(newFSDirectory(tempDir), a, a,
@@ -83,7 +83,7 @@ public class BlendedInfixSuggesterTest extends LuceneTestCase {
         new Input("top of the lake", w, pl)
     };
 
    File tempDir = createTempDir("BlendedInfixSuggesterTest");
    Path tempDir = createTempDir("BlendedInfixSuggesterTest");
     Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);
 
     // BlenderType.LINEAR is used by default (remove position*10%)
@@ -125,7 +125,7 @@ public class BlendedInfixSuggesterTest extends LuceneTestCase {
         new Input("the returned", 10, ret),
     };
 
    File tempDir = createTempDir("BlendedInfixSuggesterTest");
    Path tempDir = createTempDir("BlendedInfixSuggesterTest");
     Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);
 
     // if factor is small, we don't get the expected element
@@ -177,7 +177,7 @@ public class BlendedInfixSuggesterTest extends LuceneTestCase {
         new Input("the returned", 10, ret),
     };
 
    File tempDir = createTempDir("BlendedInfixSuggesterTest");
    Path tempDir = createTempDir("BlendedInfixSuggesterTest");
     Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);
 
     // if factor is small, we don't get the expected element
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/TestFreeTextSuggester.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/TestFreeTextSuggester.java
index aaf6605a5d3..84bdf2bfdd0 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/TestFreeTextSuggester.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/TestFreeTextSuggester.java
@@ -17,12 +17,11 @@ package org.apache.lucene.search.suggest.analyzing;
  * limitations under the License.
  */
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
@@ -82,16 +81,15 @@ public class TestFreeTextSuggester extends LuceneTestCase {
                    toString(sug.lookup("b", 10)));
 
       // Try again after save/load:
      File tmpDir = createTempDir("FreeTextSuggesterTest");
      tmpDir.mkdir();
      Path tmpDir = createTempDir("FreeTextSuggesterTest");
 
      File path = new File(tmpDir, "suggester");
      Path path = tmpDir.resolve("suggester");
 
      OutputStream os = new FileOutputStream(path);
      OutputStream os = Files.newOutputStream(path);
       sug.store(os);
       os.close();
 
      InputStream is = new FileInputStream(path);
      InputStream is = Files.newInputStream(path);
       sug = new FreeTextSuggester(a, a, 2, (byte) 0x20);
       sug.load(is);
       is.close();
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/fst/LargeInputFST.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/fst/LargeInputFST.java
index 02b09b78023..868453f4d2f 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/fst/LargeInputFST.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/fst/LargeInputFST.java
@@ -19,13 +19,12 @@ package org.apache.lucene.search.suggest.fst;
 
 
 import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefBuilder;
 import org.apache.lucene.util.OfflineSorter;
 
@@ -35,7 +34,7 @@ import org.apache.lucene.util.OfflineSorter;
  */
 public class LargeInputFST {
   public static void main(String[] args) throws IOException {
    File input = new File("/home/dweiss/tmp/shuffled.dict");
    Path input = Paths.get("/home/dweiss/tmp/shuffled.dict");
 
     int buckets = 20;
     int shareMaxTail = 10;
@@ -43,9 +42,7 @@ public class LargeInputFST {
     ExternalRefSorter sorter = new ExternalRefSorter(new OfflineSorter());
     FSTCompletionBuilder builder = new FSTCompletionBuilder(buckets, sorter, shareMaxTail);
 
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(input), StandardCharsets.UTF_8));
    BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
     
     BytesRefBuilder scratch = new BytesRefBuilder();
     String line;
@@ -61,8 +58,8 @@ public class LargeInputFST {
     System.out.println("Building FSTCompletion.");
     FSTCompletion completion = builder.build();
 
    File fstFile = new File("completion.fst");
    System.out.println("Done. Writing automaton: " + fstFile.getAbsolutePath());
    Path fstFile = Paths.get("completion.fst");
    System.out.println("Done. Writing automaton: " + fstFile.toAbsolutePath());
     completion.getFST().save(fstFile);
     sorter.close();
   }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
index fe3c5e5a2b4..aedac0ab774 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/BaseTokenStreamTestCase.java
@@ -17,16 +17,15 @@ package org.apache.lucene.analysis;
  * limitations under the License.
  */
 
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.Reader;
 import java.io.StringReader;
 import java.io.StringWriter;
 import java.io.Writer;
import java.lang.reflect.Constructor;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
 import java.util.*;
 import java.util.concurrent.CountDownLatch;
 
@@ -896,7 +895,7 @@ public abstract class BaseTokenStreamTestCase extends LuceneTestCase {
   }
 
   protected void toDotFile(Analyzer a, String inputText, String localFileName) throws IOException {
    Writer w = new OutputStreamWriter(new FileOutputStream(localFileName), StandardCharsets.UTF_8);
    Writer w = Files.newBufferedWriter(Paths.get(localFileName), StandardCharsets.UTF_8);
     final TokenStream ts = a.tokenStream("field", inputText);
     ts.reset();
     new TokenStreamToDot(inputText, ts, new PrintWriter(w)).toDot();
diff --git a/lucene/test-framework/src/java/org/apache/lucene/analysis/VocabularyAssert.java b/lucene/test-framework/src/java/org/apache/lucene/analysis/VocabularyAssert.java
index cdcacc587bf..dc3765a4a74 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/analysis/VocabularyAssert.java
++ b/lucene/test-framework/src/java/org/apache/lucene/analysis/VocabularyAssert.java
@@ -18,11 +18,11 @@ package org.apache.lucene.analysis;
  */
 
 import java.io.BufferedReader;
import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
 import java.util.zip.ZipFile;
 
 import org.apache.lucene.analysis.Analyzer;
@@ -61,9 +61,8 @@ public class VocabularyAssert {
   }
   
   /** Run a vocabulary test against two data files inside a zip file */
  public static void assertVocabulary(Analyzer a, File zipFile, String voc, String out)
  throws IOException {
    ZipFile zip = new ZipFile(zipFile);
  public static void assertVocabulary(Analyzer a, Path zipFile, String voc, String out) throws IOException {
    ZipFile zip = new ZipFile(zipFile.toFile());
     InputStream v = zip.getInputStream(zip.getEntry(voc));
     InputStream o = zip.getInputStream(zip.getEntry(out));
     assertVocabulary(a, v, o);
@@ -73,9 +72,8 @@ public class VocabularyAssert {
   }
   
   /** Run a vocabulary test against a tab-separated data file inside a zip file */
  public static void assertVocabulary(Analyzer a, File zipFile, String vocOut)
  throws IOException {
    ZipFile zip = new ZipFile(zipFile);
  public static void assertVocabulary(Analyzer a, Path zipFile, String vocOut) throws IOException {
    ZipFile zip = new ZipFile(zipFile.toFile());
     InputStream vo = zip.getInputStream(zip.getEntry(vocOut));
     assertVocabulary(a, vo);
     vo.close();
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
index 9b07768ffd7..b6ba0e14885 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/BasePostingsFormatTestCase.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 
 import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
@@ -1335,7 +1336,7 @@ public abstract class BasePostingsFormatTestCase extends BaseIndexFileFormatTest
   /** Indexes all fields/terms at the specified
    *  IndexOptions, and fully tests at that IndexOptions. */
   private void testFull(IndexOptions options, boolean withPayloads) throws Exception {
    File path = createTempDir("testPostingsFormat.testExact");
    Path path = createTempDir("testPostingsFormat.testExact");
     Directory dir = newFSDirectory(path);
 
     // TODO test thread safety of buildIndex too
@@ -1388,7 +1389,7 @@ public abstract class BasePostingsFormatTestCase extends BaseIndexFileFormatTest
     int iters = 5;
 
     for(int iter=0;iter<iters;iter++) {
      File path = createTempDir("testPostingsFormat");
      Path path = createTempDir("testPostingsFormat");
       Directory dir = newFSDirectory(path);
 
       boolean indexPayloads = random().nextBoolean();
diff --git a/lucene/test-framework/src/java/org/apache/lucene/index/ThreadedIndexingAndSearchingTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/index/ThreadedIndexingAndSearchingTestCase.java
index 3c8c4edcdac..4fc43f9d442 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/index/ThreadedIndexingAndSearchingTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/index/ThreadedIndexingAndSearchingTestCase.java
@@ -19,6 +19,7 @@ package org.apache.lucene.index;
 
 import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
@@ -436,7 +437,7 @@ public abstract class ThreadedIndexingAndSearchingTestCase extends LuceneTestCas
 
     Random random = new Random(random().nextLong());
     final LineFileDocs docs = new LineFileDocs(random, true);
    final File tempDir = createTempDir(testName);
    final Path tempDir = createTempDir(testName);
     dir = getDirectory(newMockFSDirectory(tempDir)); // some subclasses rely on this being MDW
     if (dir instanceof BaseDirectoryWrapper) {
       ((BaseDirectoryWrapper) dir).setCheckIndexOnClose(false); // don't double-checkIndex, we do it ourselves.
diff --git a/lucene/test-framework/src/java/org/apache/lucene/store/BaseDirectoryTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/store/BaseDirectoryTestCase.java
index 8031b50b5b9..daa23f59599 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/store/BaseDirectoryTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/store/BaseDirectoryTestCase.java
@@ -23,6 +23,7 @@ import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
@@ -42,7 +43,7 @@ public abstract class BaseDirectoryTestCase extends LuceneTestCase {
   /** Subclass returns the Directory to be tested; if it's
    *  an FS-based directory it should point to the specified
    *  path, else it can ignore it. */
  protected abstract Directory getDirectory(File path) throws IOException;
  protected abstract Directory getDirectory(Path path) throws IOException;
   
   // first some basic tests for the directory api
   
@@ -473,17 +474,6 @@ public abstract class BaseDirectoryTestCase extends LuceneTestCase {
     dir.close();
   }
 
  /** LUCENE-1464: just creating a Directory should not
   *  mkdir the underling directory in the filesystem. */
  public void testDontCreate() throws Throwable {
    File path = createTempDir("doesnotexist");
    IOUtils.rm(path);
    assertTrue(!path.exists());
    Directory dir = getDirectory(path);
    assertTrue(!path.exists());
    dir.close();
  }

   /** LUCENE-1468: once we create an output, we should see
    *  it in the dir listing and be able to open it with
    *  openInput. */
@@ -582,13 +572,13 @@ public abstract class BaseDirectoryTestCase extends LuceneTestCase {
   
   // LUCENE-3382 -- make sure we get exception if the directory really does not exist.
   public void testNoDir() throws Throwable {
    File tempDir = createTempDir("doesnotexist");
    Path tempDir = createTempDir("doesnotexist");
     IOUtils.rm(tempDir);
     Directory dir = getDirectory(tempDir);
     try {
       DirectoryReader.open(dir);
       fail("did not hit expected exception");
    } catch (NoSuchDirectoryException | IndexNotFoundException nsde) {
    } catch (NoSuchFileException | IndexNotFoundException nsde) {
       // expected
     }
     dir.close();
@@ -774,7 +764,7 @@ public abstract class BaseDirectoryTestCase extends LuceneTestCase {
   // this test backdoors the directory via the filesystem. so it must actually use the filesystem
   // TODO: somehow change this test to 
   public void testFsyncDoesntCreateNewFiles() throws Exception {
    File path = createTempDir("nocreate");
    Path path = createTempDir("nocreate");
     Directory fsdir = getDirectory(path);
     
     // this test backdoors the directory via the filesystem. so it must be an FSDir (for now)
@@ -791,7 +781,7 @@ public abstract class BaseDirectoryTestCase extends LuceneTestCase {
     out.close();
     
     // delete it
    Files.delete(new File(path, "afile").toPath());
    Files.delete(path.resolve("afile"));
     
     // directory is empty
     assertEquals(0, fsdir.listAll().length);
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/LineFileDocs.java b/lucene/test-framework/src/java/org/apache/lucene/util/LineFileDocs.java
index 4cc95bb0f3d..b6990030401 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/LineFileDocs.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/LineFileDocs.java
@@ -19,17 +19,17 @@ package org.apache.lucene.util;
 
 import java.io.BufferedReader;
 import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
import java.io.RandomAccessFile;
 import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
 import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CodingErrorAction;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.Random;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.zip.GZIPInputStream;
@@ -89,15 +89,15 @@ public class LineFileDocs implements Closeable {
     long size = 0L, seekTo = 0L;
     if (is == null) {
       // if its not in classpath, we load it as absolute filesystem path (e.g. Hudson's home dir)
      File file = new File(path);
      size = file.length();
      Path file = Paths.get(path);
      size = Files.size(file);
       if (path.endsWith(".gz")) {
         // if it is a gzip file, we need to use InputStream and slowly skipTo:
        is = new FileInputStream(file);
        is = Files.newInputStream(file);
       } else {
        // optimized seek using RandomAccessFile:
        // optimized seek using SeekableByteChannel
         seekTo = randomSeekPos(random, size);
        final FileChannel channel = new RandomAccessFile(path, "r").getChannel();
        final SeekableByteChannel channel = Files.newByteChannel(file);
         if (LuceneTestCase.VERBOSE) {
           System.out.println("TEST: LineFileDocs: file seek to fp=" + seekTo + " on open");
         }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java b/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
index a332cb6853b..b20fd2cf668 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/LuceneTestCase.java
@@ -18,7 +18,6 @@ package org.apache.lucene.util;
  */
 
 import java.io.Closeable;
import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.PrintStream;
@@ -32,6 +31,8 @@ import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
@@ -1170,7 +1171,7 @@ public abstract class LuceneTestCase extends Assert {
     return (MockDirectoryWrapper) wrapDirectory(r, newDirectoryImpl(r, TEST_DIRECTORY), false);
   }
 
  public static MockDirectoryWrapper newMockFSDirectory(File f) {
  public static MockDirectoryWrapper newMockFSDirectory(Path f) {
     return (MockDirectoryWrapper) newFSDirectory(f, null, false);
   }
 
@@ -1184,16 +1185,16 @@ public abstract class LuceneTestCase extends Assert {
   }
 
   /** Returns a new FSDirectory instance over the given file, which must be a folder. */
  public static BaseDirectoryWrapper newFSDirectory(File f) {
  public static BaseDirectoryWrapper newFSDirectory(Path f) {
     return newFSDirectory(f, null);
   }
 
   /** Returns a new FSDirectory instance over the given file, which must be a folder. */
  public static BaseDirectoryWrapper newFSDirectory(File f, LockFactory lf) {
  public static BaseDirectoryWrapper newFSDirectory(Path f, LockFactory lf) {
     return newFSDirectory(f, lf, rarely());
   }
 
  private static BaseDirectoryWrapper newFSDirectory(File f, LockFactory lf, boolean bare) {
  private static BaseDirectoryWrapper newFSDirectory(Path f, LockFactory lf, boolean bare) {
     String fsdirClass = TEST_DIRECTORY;
     if (fsdirClass.equals("random")) {
       fsdirClass = RandomPicks.randomFrom(random(), FS_DIRECTORIES); 
@@ -1408,12 +1409,10 @@ public abstract class LuceneTestCase extends Assert {
     }
   }
 
  private static Directory newFSDirectoryImpl(
      Class<? extends FSDirectory> clazz, File file)
      throws IOException {
  private static Directory newFSDirectoryImpl(Class<? extends FSDirectory> clazz, Path path) throws IOException {
     FSDirectory d = null;
     try {
      d = CommandLineUtil.newFSDirectory(clazz, file);
      d = CommandLineUtil.newFSDirectory(clazz, path);
     } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
       Rethrow.rethrow(e);
     }
@@ -1431,24 +1430,24 @@ public abstract class LuceneTestCase extends Assert {
 
     try {
       final Class<? extends Directory> clazz = CommandLineUtil.loadDirectoryClass(clazzName);
      // If it is a FSDirectory type, try its ctor(File)
      // If it is a FSDirectory type, try its ctor(Path)
       if (FSDirectory.class.isAssignableFrom(clazz)) {
        final File dir = createTempDir("index-" + clazzName);
        final Path dir = createTempDir("index-" + clazzName);
         return newFSDirectoryImpl(clazz.asSubclass(FSDirectory.class), dir);
       }
 
      // See if it has a File ctor even though it's not an
      // See if it has a Path ctor even though it's not an
       // FSDir subclass:
      Constructor<? extends Directory> fileCtor = null;
      Constructor<? extends Directory> pathCtor = null;
       try {
        fileCtor = clazz.getConstructor(File.class);
        pathCtor = clazz.getConstructor(Path.class);
       } catch (NoSuchMethodException nsme) {
         // Ignore
       }
 
      if (fileCtor != null) {
        final File dir = createTempDir("index");
        return fileCtor.newInstance(dir);
      if (pathCtor != null) {
        final Path dir = createTempDir("index");
        return pathCtor.newInstance(dir);
       }
 
       // try empty ctor
@@ -1662,13 +1661,13 @@ public abstract class LuceneTestCase extends Assert {
   }
 
   /**
   * Gets a resource from the classpath as {@link File}. This method should only
   * Gets a resource from the classpath as {@link Path}. This method should only
    * be used, if a real file is needed. To get a stream, code should prefer
    * {@link Class#getResourceAsStream} using {@code this.getClass()}.
    */
  protected File getDataFile(String name) throws IOException {
  protected Path getDataPath(String name) throws IOException {
     try {
      return new File(this.getClass().getResource(name).toURI());
      return Paths.get(this.getClass().getResource(name).toURI());
     } catch (Exception e) {
       throw new IOException("Cannot find resource: " + name);
     }
@@ -2364,7 +2363,7 @@ public abstract class LuceneTestCase extends Assert {
    * or {@link #createTempDir(String)} or {@link #createTempFile(String, String)}.
    */
   @Deprecated
  public static File getBaseTempDirForTestClass() {
  public static Path getBaseTempDirForTestClass() {
     return tempFilesCleanupRule.getPerTestClassTempDir();
   }
 
@@ -2374,7 +2373,7 @@ public abstract class LuceneTestCase extends Assert {
    * 
    * @see #createTempDir(String)
    */
  public static File createTempDir() {
  public static Path createTempDir() {
     return createTempDir("tempDir");
   }
 
@@ -2386,7 +2385,7 @@ public abstract class LuceneTestCase extends Assert {
    * test class completes successfully. The test should close any file handles that would prevent
    * the folder from being removed. 
    */
  public static File createTempDir(String prefix) {
  public static Path createTempDir(String prefix) {
     return tempFilesCleanupRule.createTempDir(prefix);
   }
   
@@ -2398,7 +2397,7 @@ public abstract class LuceneTestCase extends Assert {
    * test class completes successfully. The test should close any file handles that would prevent
    * the folder from being removed. 
    */
  public static File createTempFile(String prefix, String suffix) throws IOException {
  public static Path createTempFile(String prefix, String suffix) throws IOException {
     return tempFilesCleanupRule.createTempFile(prefix, suffix);
   }
 
@@ -2407,7 +2406,7 @@ public abstract class LuceneTestCase extends Assert {
    * 
    * @see #createTempFile(String, String) 
    */
  public static File createTempFile() throws IOException {
  public static Path createTempFile() throws IOException {
     return createTempFile("tempFile", ".tmp");
   }
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/RemoveUponClose.java b/lucene/test-framework/src/java/org/apache/lucene/util/RemoveUponClose.java
index 4f012275a78..90a97ae75a6 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/RemoveUponClose.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/RemoveUponClose.java
@@ -1,8 +1,9 @@
 package org.apache.lucene.util;
 
 import java.io.Closeable;
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
@@ -25,12 +26,12 @@ import java.io.IOException;
  * A {@link Closeable} that attempts to remove a given file/folder.
  */
 final class RemoveUponClose implements Closeable {
  private final File file;
  private final Path path;
   private final TestRuleMarkFailure failureMarker;
   private final String creationStack;
 
  public RemoveUponClose(File file, TestRuleMarkFailure failureMarker) {
    this.file = file;
  public RemoveUponClose(Path path, TestRuleMarkFailure failureMarker) {
    this.path = path;
     this.failureMarker = failureMarker;
 
     StringBuilder b = new StringBuilder();
@@ -44,13 +45,13 @@ final class RemoveUponClose implements Closeable {
   public void close() throws IOException {
     // only if there were no other test failures.
     if (failureMarker.wasSuccessful()) {
      if (file.exists()) {
      if (Files.exists(path)) {
         try {
          IOUtils.rm(file);
          IOUtils.rm(path);
         } catch (IOException e) {
           throw new IOException(
               "Could not remove temporary location '" 
                  + file.getAbsolutePath() + "', created at stack trace:\n" + creationStack, e);
                  + path.toAbsolutePath() + "', created at stack trace:\n" + creationStack, e);
         }
       }
     }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/TestRuleTemporaryFilesCleanup.java b/lucene/test-framework/src/java/org/apache/lucene/util/TestRuleTemporaryFilesCleanup.java
index d139d60b189..47aab8e162e 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/TestRuleTemporaryFilesCleanup.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/TestRuleTemporaryFilesCleanup.java
@@ -1,7 +1,9 @@
 package org.apache.lucene.util;
 
import java.io.File;
 import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
@@ -44,12 +46,12 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
   /**
    * Writeable temporary base folder. 
    */
  private File javaTempDir;
  private Path javaTempDir;
 
   /**
    * Per-test class temporary folder.
    */
  private File tempDirBase;
  private Path tempDirBase;
 
   /**
    * Suite failure marker.
@@ -59,9 +61,9 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
   /**
    * A queue of temporary resources to be removed after the
    * suite completes.
   * @see #registerToRemoveAfterSuite(File)
   * @see #registerToRemoveAfterSuite(Path)
    */
  private final static List<File> cleanupQueue = new ArrayList<File>();
  private final static List<Path> cleanupQueue = new ArrayList<Path>();
 
   public TestRuleTemporaryFilesCleanup(TestRuleMarkFailure failureMarker) {
     this.failureMarker = failureMarker;
@@ -70,11 +72,11 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
   /**
    * Register temporary folder for removal after the suite completes.
    */
  void registerToRemoveAfterSuite(File f) {
  void registerToRemoveAfterSuite(Path f) {
     assert f != null;
 
     if (LuceneTestCase.LEAVE_TEMPORARY) {
      System.err.println("INFO: Will leave temporary file: " + f.getAbsolutePath());
      System.err.println("INFO: Will leave temporary file: " + f.toAbsolutePath());
       return;
     }
 
@@ -91,28 +93,27 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
     javaTempDir = initializeJavaTempDir();
   }
 
  private File initializeJavaTempDir() {
    File javaTempDir = new File(System.getProperty("tempDir", System.getProperty("java.io.tmpdir")));
    if (!javaTempDir.exists() && !javaTempDir.mkdirs()) {
      throw new RuntimeException("Could not create temp dir: " + javaTempDir.getAbsolutePath());
    }
    assert javaTempDir.isDirectory() &&
           javaTempDir.canWrite();
  private Path initializeJavaTempDir() throws IOException {
    Path javaTempDir = Paths.get(System.getProperty("tempDir", System.getProperty("java.io.tmpdir")));
    Files.createDirectories(javaTempDir);

    assert Files.isDirectory(javaTempDir) &&
           Files.isWritable(javaTempDir);
 
    return javaTempDir.getAbsoluteFile();
    return javaTempDir.toRealPath();
   }
 
   @Override
   protected void afterAlways(List<Throwable> errors) throws Throwable {
     // Drain cleanup queue and clear it.
    final File [] everything;
    final Path [] everything;
     final String tempDirBasePath;
     synchronized (cleanupQueue) {
      tempDirBasePath = (tempDirBase != null ? tempDirBase.getAbsolutePath() : null);
      tempDirBasePath = (tempDirBase != null ? tempDirBase.toAbsolutePath().toString() : null);
       tempDirBase = null;
 
       Collections.reverse(cleanupQueue);
      everything = new File [cleanupQueue.size()];
      everything = new Path [cleanupQueue.size()];
       cleanupQueue.toArray(everything);
       cleanupQueue.clear();
     }
@@ -140,7 +141,7 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
     }
   }
   
  final File getPerTestClassTempDir() {
  final Path getPerTestClassTempDir() {
     if (tempDirBase == null) {
       RandomizedContext ctx = RandomizedContext.current();
       Class<?> clazz = ctx.getTargetClass();
@@ -149,16 +150,21 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
       prefix = prefix.replaceFirst("^org.apache.solr.", "solr.");
 
       int attempt = 0;
      File f;
      Path f;
      boolean success = false;
       do {
         if (attempt++ >= TEMP_NAME_RETRY_THRESHOLD) {
           throw new RuntimeException(
               "Failed to get a temporary name too many times, check your temp directory and consider manually cleaning it: "
                + javaTempDir.getAbsolutePath());            
                + javaTempDir.toAbsolutePath());            
         }
        f = new File(javaTempDir, prefix + "-" + ctx.getRunnerSeedAsString() 
        f = javaTempDir.resolve(prefix + "-" + ctx.getRunnerSeedAsString() 
               + "-" + String.format(Locale.ENGLISH, "%03d", attempt));
      } while (!f.mkdirs());
        try {
          Files.createDirectory(f);
          success = true;
        } catch (IOException ignore) {}
      } while (!success);
 
       tempDirBase = f;
       registerToRemoveAfterSuite(tempDirBase);
@@ -169,19 +175,24 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
   /**
    * @see LuceneTestCase#createTempDir()
    */
  public File createTempDir(String prefix) {
    File base = getPerTestClassTempDir();
  public Path createTempDir(String prefix) {
    Path base = getPerTestClassTempDir();
 
     int attempt = 0;
    File f;
    Path f;
    boolean success = false;
     do {
       if (attempt++ >= TEMP_NAME_RETRY_THRESHOLD) {
         throw new RuntimeException(
             "Failed to get a temporary name too many times, check your temp directory and consider manually cleaning it: "
              + base.getAbsolutePath());            
              + base.toAbsolutePath());            
       }
      f = new File(base, prefix + "-" + String.format(Locale.ENGLISH, "%03d", attempt));
    } while (!f.mkdirs());
      f = base.resolve(prefix + "-" + String.format(Locale.ENGLISH, "%03d", attempt));
      try {
        Files.createDirectory(f);
        success = true;
      } catch (IOException ignore) {}
    } while (!success);
 
     registerToRemoveAfterSuite(f);
     return f;
@@ -190,19 +201,24 @@ final class TestRuleTemporaryFilesCleanup extends TestRuleAdapter {
   /**
    * @see LuceneTestCase#createTempFile()
    */
  public File createTempFile(String prefix, String suffix) throws IOException {
    File base = getPerTestClassTempDir();
  public Path createTempFile(String prefix, String suffix) throws IOException {
    Path base = getPerTestClassTempDir();
 
     int attempt = 0;
    File f;
    Path f;
    boolean success = false;
     do {
       if (attempt++ >= TEMP_NAME_RETRY_THRESHOLD) {
         throw new RuntimeException(
             "Failed to get a temporary name too many times, check your temp directory and consider manually cleaning it: "
              + base.getAbsolutePath());            
              + base.toAbsolutePath());            
       }
      f = new File(base, prefix + "-" + String.format(Locale.ENGLISH, "%03d", attempt) + suffix);
    } while (!f.createNewFile());
      f = base.resolve(prefix + "-" + String.format(Locale.ENGLISH, "%03d", attempt) + suffix);
      try {
        Files.createFile(f);
        success = true;
      } catch (IOException ignore) {}
    } while (!success);
 
     registerToRemoveAfterSuite(f);
     return f;
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java b/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
index 56f3f5f3c84..49e675b5667 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/TestUtil.java
@@ -17,10 +17,7 @@ package org.apache.lucene.util;
  * limitations under the License.
  */
 
import java.io.BufferedOutputStream;
 import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
@@ -29,10 +26,10 @@ import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.nio.CharBuffer;
 import java.nio.file.Files;
import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.HashMap;
import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
@@ -104,28 +101,24 @@ public final class TestUtil {
    * Convenience method unzipping zipName into destDir, cleaning up 
    * destDir first. 
    */
  public static void unzip(File zipName, File destDir) throws IOException {
  public static void unzip(Path zipName, Path destDir) throws IOException {
     IOUtils.rm(destDir);
    destDir.mkdir();
    Files.createDirectory(destDir);
 
    ZipFile zipFile = new ZipFile(zipName);
    ZipFile zipFile = new ZipFile(zipName.toFile());
     Enumeration<? extends ZipEntry> entries = zipFile.entries();
 
     while (entries.hasMoreElements()) {
       ZipEntry entry = entries.nextElement();
       
       InputStream in = zipFile.getInputStream(entry);
      File targetFile = new File(destDir, entry.getName());
      if (entry.isDirectory()) {
        // allow unzipping with directory structure
        targetFile.mkdirs();
      } else {
        if (targetFile.getParentFile()!=null) {
          // be on the safe side: do not rely on that directories are always extracted
          // before their children (although this makes sense, but is it guaranteed?)
          targetFile.getParentFile().mkdirs();   
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
      Path targetFile = destDir.resolve(entry.getName());
      
      // be on the safe side: do not rely on that directories are always extracted
      // before their children (although this makes sense, but is it guaranteed?)
      Files.createDirectories(targetFile.getParent());
      if (!entry.isDirectory()) {
        OutputStream out = Files.newOutputStream(targetFile);
         
         byte[] buffer = new byte[8192];
         int len;
diff --git a/lucene/test-framework/src/java/org/apache/lucene/util/fst/FSTTester.java b/lucene/test-framework/src/java/org/apache/lucene/util/fst/FSTTester.java
index 4b41901b14d..5393a48ad80 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/util/fst/FSTTester.java
++ b/lucene/test-framework/src/java/org/apache/lucene/util/fst/FSTTester.java
@@ -17,11 +17,11 @@ package org.apache.lucene.util.fst;
  * limitations under the License.
  */
 
import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
@@ -321,7 +321,7 @@ public class FSTTester<T> {
     }
 
     if (LuceneTestCase.VERBOSE && pairs.size() <= 20 && fst != null) {
      Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"), StandardCharsets.UTF_8);
      Writer w = Files.newBufferedWriter(Paths.get("out.dot"), StandardCharsets.UTF_8);
       Util.toDot(fst, w, false, false);
       w.close();
       System.out.println("SAVED out.dot");
diff --git a/lucene/tools/forbiddenApis/lucene.txt b/lucene/tools/forbiddenApis/lucene.txt
new file mode 100644
index 00000000000..bc8430b2f39
-- /dev/null
++ b/lucene/tools/forbiddenApis/lucene.txt
@@ -0,0 +1,20 @@
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

@defaultMessage Use NIO.2 instead
java.io.File
java.io.FileInputStream
java.io.FileOutputStream
# TODO: all kinds of other stuff taking "String" but making a file itself...
diff --git a/solr/contrib/analysis-extras/src/test/org/apache/solr/analysis/TestFoldingMultitermExtrasQuery.java b/solr/contrib/analysis-extras/src/test/org/apache/solr/analysis/TestFoldingMultitermExtrasQuery.java
index cc93887ffd2..bb0845eb5a4 100644
-- a/solr/contrib/analysis-extras/src/test/org/apache/solr/analysis/TestFoldingMultitermExtrasQuery.java
++ b/solr/contrib/analysis-extras/src/test/org/apache/solr/analysis/TestFoldingMultitermExtrasQuery.java
@@ -32,7 +32,7 @@ public class TestFoldingMultitermExtrasQuery extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void beforeTests() throws Exception {
    File testHome = createTempDir();
    File testHome = createTempDir().toFile();
     FileUtils.copyDirectory(getFile("analysis-extras/solr"), testHome);
     initCore("solrconfig-icucollate.xml","schema-folding-extra.xml", testHome.getAbsolutePath());
 
diff --git a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationField.java b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationField.java
index 9ae3e4d9677..161f6476741 100644
-- a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationField.java
++ b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationField.java
@@ -63,7 +63,7 @@ public class TestICUCollationField extends SolrTestCaseJ4 {
    * So its preferable to create this file on-the-fly.
    */
   public static String setupSolrHome() throws Exception {
    String tmpFile = createTempDir().getAbsolutePath();
    String tmpFile = createTempDir().toFile().getAbsolutePath();
     // make data and conf dirs
     new File(tmpFile  + "/collection1", "data").mkdirs();
     File confDir = new File(tmpFile + "/collection1", "conf");
diff --git a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldDocValues.java b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldDocValues.java
index f3d45752dca..cba27a08e35 100644
-- a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldDocValues.java
++ b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldDocValues.java
@@ -61,7 +61,7 @@ public class TestICUCollationFieldDocValues extends SolrTestCaseJ4 {
    * So its preferable to create this file on-the-fly.
    */
   public static String setupSolrHome() throws Exception {
    File tmpFile = createTempDir();
    File tmpFile = createTempDir().toFile();
     
     // make data and conf dirs
     new File(tmpFile + "/collection1", "data").mkdirs();
diff --git a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldOptions.java b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldOptions.java
index 3a6671931d6..f1fcf536a46 100644
-- a/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldOptions.java
++ b/solr/contrib/analysis-extras/src/test/org/apache/solr/schema/TestICUCollationFieldOptions.java
@@ -29,7 +29,7 @@ import java.io.File;
 public class TestICUCollationFieldOptions extends SolrTestCaseJ4 {
   @BeforeClass
   public static void beforeClass() throws Exception {
    File testHome = createTempDir();
    File testHome = createTempDir().toFile();
     FileUtils.copyDirectory(getFile("analysis-extras/solr"), testHome);
     initCore("solrconfig-icucollate.xml","schema-icucollateoptions.xml", testHome.getAbsolutePath());
     // add some docs
diff --git a/solr/contrib/clustering/src/test/org/apache/solr/handler/clustering/AbstractClusteringTestCase.java b/solr/contrib/clustering/src/test/org/apache/solr/handler/clustering/AbstractClusteringTestCase.java
index 89390999199..77de5f7cc9d 100644
-- a/solr/contrib/clustering/src/test/org/apache/solr/handler/clustering/AbstractClusteringTestCase.java
++ b/solr/contrib/clustering/src/test/org/apache/solr/handler/clustering/AbstractClusteringTestCase.java
@@ -33,7 +33,7 @@ public abstract class AbstractClusteringTestCase extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void beforeClass() throws Exception {
    File testHome = createTempDir();
    File testHome = createTempDir().toFile();
     FileUtils.copyDirectory(getFile("clustering/solr"), testHome);
     initCore("solrconfig.xml", "schema.xml", testHome.getAbsolutePath());
     numberOfDocs = 0;
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDataImportHandlerTestCase.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDataImportHandlerTestCase.java
index 2e3e395226e..12bed334a65 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDataImportHandlerTestCase.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractDataImportHandlerTestCase.java
@@ -55,7 +55,7 @@ public abstract class AbstractDataImportHandlerTestCase extends
 
   // note, a little twisted that we shadow this static method
   public static void initCore(String config, String schema) throws Exception {
    File testHome = createTempDir("core-home");
    File testHome = createTempDir("core-home").toFile();
     FileUtils.copyDirectory(getFile("dih/solr"), testHome);
     initCore(config, schema, testHome.getAbsolutePath());
   }
@@ -64,7 +64,7 @@ public abstract class AbstractDataImportHandlerTestCase extends
   @Before
   public void setUp() throws Exception {
     super.setUp();
    File home = createTempDir("dih-properties");
    File home = createTempDir("dih-properties").toFile();
     System.setProperty("solr.solr.home", home.getAbsolutePath());    
   }
 
@@ -99,7 +99,7 @@ public abstract class AbstractDataImportHandlerTestCase extends
    */
   protected File redirectTempProperties(DataImporter di) {
     try {
      File tempFile = createTempFile();
      File tempFile = createTempFile().toFile();
       di.getConfig().getPropertyWriter().getParameters()
         .put(SimplePropertiesWriter.FILENAME, tempFile.getAbsolutePath());
       return tempFile;
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractSqlEntityProcessorTestCase.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractSqlEntityProcessorTestCase.java
index 78b3bfd027e..ecb58f85dcc 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractSqlEntityProcessorTestCase.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/AbstractSqlEntityProcessorTestCase.java
@@ -55,7 +55,7 @@ public abstract class AbstractSqlEntityProcessorTestCase extends
   
   @Before
   public void beforeSqlEntitiyProcessorTestCase() throws Exception {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
     fileLocation = tmpdir.getPath();
     fileName = "the.properties";
   } 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestContentStreamDataSource.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestContentStreamDataSource.java
index a15e2d18320..3c92c297dc7 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestContentStreamDataSource.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestContentStreamDataSource.java
@@ -149,7 +149,7 @@ public class TestContentStreamDataSource extends AbstractDataImportHandlerTestCa
 
 
     public void setUp() throws Exception {
      homeDir = createTempDir("inst");
      homeDir = createTempDir("inst").toFile();
       dataDir = new File(homeDir + "/collection1", "data");
       confDir = new File(homeDir + "/collection1", "conf");
 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder2.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder2.java
index 2efc90c664a..33a9646e4bf 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder2.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestDocBuilder2.java
@@ -253,7 +253,7 @@ public class TestDocBuilder2 extends AbstractDataImportHandlerTestCase {
   @Test
   @Ignore("Fix Me. See SOLR-4103.")
   public void testFileListEntityProcessor_lastIndexTime() throws Exception  {
    File tmpdir = File.createTempFile("test", "tmp", createTempDir());
    File tmpdir = File.createTempFile("test", "tmp", createTempDir().toFile());
 
     Map<String, String> params = createMap("baseDir", tmpdir.getAbsolutePath());
 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListEntityProcessor.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListEntityProcessor.java
index e87f7640091..15517d17b60 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListEntityProcessor.java
@@ -41,7 +41,7 @@ public class TestFileListEntityProcessor extends AbstractDataImportHandlerTestCa
   @Test
   @SuppressWarnings("unchecked")
   public void testSimple() throws IOException {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
 
     createFile(tmpdir, "a.xml", "a.xml".getBytes(StandardCharsets.UTF_8), false);
     createFile(tmpdir, "b.xml", "b.xml".getBytes(StandardCharsets.UTF_8), false);
@@ -65,7 +65,7 @@ public class TestFileListEntityProcessor extends AbstractDataImportHandlerTestCa
   
   @Test
   public void testBiggerSmallerFiles() throws IOException {
    File tmpdir = File.createTempFile("test", "tmp", createTempDir());
    File tmpdir = File.createTempFile("test", "tmp", createTempDir().toFile());
     Files.delete(tmpdir.toPath());
     tmpdir.mkdir();
 
@@ -135,7 +135,7 @@ public class TestFileListEntityProcessor extends AbstractDataImportHandlerTestCa
 
   @Test
   public void testNTOT() throws IOException {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
 
     createFile(tmpdir, "a.xml", "a.xml".getBytes(StandardCharsets.UTF_8), true);
     createFile(tmpdir, "b.xml", "b.xml".getBytes(StandardCharsets.UTF_8), true);
@@ -169,7 +169,7 @@ public class TestFileListEntityProcessor extends AbstractDataImportHandlerTestCa
 
   @Test
   public void testRECURSION() throws IOException {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
     File childdir = new File(tmpdir + "/child" );
     childdir.mkdir();
     createFile(childdir, "a.xml", "a.xml".getBytes(StandardCharsets.UTF_8), true);
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java
index 00109d16181..656bdaf0c68 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestFileListWithLineEntityProcessor.java
@@ -32,7 +32,7 @@ public class TestFileListWithLineEntityProcessor extends AbstractDataImportHandl
   }
   
   public void test() throws Exception {
    File tmpdir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    File tmpdir = createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toFile();
     createFile(tmpdir, "a.txt", "a line one\na line two\na line three".getBytes(StandardCharsets.UTF_8), false);
     createFile(tmpdir, "b.txt", "b line one\nb line two".getBytes(StandardCharsets.UTF_8), false);
     createFile(tmpdir, "c.txt", "c line one\nc line two\nc line three\nc line four".getBytes(StandardCharsets.UTF_8), false);
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestNonWritablePersistFile.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestNonWritablePersistFile.java
index ab53346e304..8d50ba50c49 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestNonWritablePersistFile.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestNonWritablePersistFile.java
@@ -52,7 +52,7 @@ public class TestNonWritablePersistFile extends AbstractDataImportHandlerTestCas
 
   @BeforeClass
   public static void createTempSolrHomeAndCore() throws Exception {
    tmpSolrHome = createTempDir().getAbsolutePath();
    tmpSolrHome = createTempDir().toFile().getAbsolutePath();
     FileUtils.copyDirectory(getFile("dih/solr"), new File(tmpSolrHome).getAbsoluteFile());
     initCore("dataimport-solrconfig.xml", "dataimport-schema.xml", 
              new File(tmpSolrHome).getAbsolutePath());
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSimplePropertiesWriter.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSimplePropertiesWriter.java
index 0521b6162a8..eb26b492670 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSimplePropertiesWriter.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSimplePropertiesWriter.java
@@ -43,7 +43,7 @@ public class TestSimplePropertiesWriter extends AbstractDIHJdbcTestCase {
   
   @Before
   public void spwBefore() throws Exception {
    fileLocation = createTempDir().getAbsolutePath();
    fileLocation = createTempDir().toFile().getAbsolutePath();
     fileName = "the.properties";
   }
  
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
index 9f0fd9b7569..55483f55201 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestSolrEntityProcessorEndToEnd.java
@@ -317,7 +317,7 @@ public class TestSolrEntityProcessorEndToEnd extends AbstractDataImportHandlerTe
     }
 
     public void setUp() throws Exception {
      homeDir = createTempDir();
      homeDir = createTempDir().toFile();
       initCoreDataDir = new File(homeDir + "/collection1", "data");
       confDir = new File(homeDir + "/collection1", "conf");
       
@@ -336,7 +336,7 @@ public class TestSolrEntityProcessorEndToEnd extends AbstractDataImportHandlerTe
     }
 
     public void tearDown() throws Exception {
      IOUtils.rm(homeDir);
      IOUtils.rm(homeDir.toPath());
     }
   }
   
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestXPathEntityProcessor.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestXPathEntityProcessor.java
index 778d1913847..72da77a60ba 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestXPathEntityProcessor.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestXPathEntityProcessor.java
@@ -43,7 +43,7 @@ public class TestXPathEntityProcessor extends AbstractDataImportHandlerTestCase
   
   @Test
   public void withFieldsAndXpath() throws Exception {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
     
     createFile(tmpdir, "x.xsl", xsl.getBytes(StandardCharsets.UTF_8), false);
     Map entityAttrs = createMap("name", "e", "url", "cd.xml",
@@ -332,7 +332,7 @@ public class TestXPathEntityProcessor extends AbstractDataImportHandlerTestCase
   
   @Test
   public void withDefaultSolrAndXsl() throws Exception {
    File tmpdir = createTempDir();
    File tmpdir = createTempDir().toFile();
     AbstractDataImportHandlerTestCase.createFile(tmpdir, "x.xsl", xsl.getBytes(StandardCharsets.UTF_8),
             false);
 
diff --git a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestZKPropertiesWriter.java b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestZKPropertiesWriter.java
index 91c2d689d5d..df2e31aafe9 100644
-- a/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestZKPropertiesWriter.java
++ b/solr/contrib/dataimporthandler/src/test/org/apache/solr/handler/dataimport/TestZKPropertiesWriter.java
@@ -49,7 +49,7 @@ public class TestZKPropertiesWriter extends AbstractDataImportHandlerTestCase {
 
   @BeforeClass
   public static void dihZk_beforeClass() throws Exception {
    zkDir = createTempDir("zkData").getAbsolutePath();
    zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     zkServer = new ZkTestServer(zkDir);
     zkServer.run();
 
diff --git a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MRUnitBase.java b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MRUnitBase.java
index f382f76db0e..b663151772f 100644
-- a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MRUnitBase.java
++ b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MRUnitBase.java
@@ -50,7 +50,7 @@ public abstract class MRUnitBase extends SolrTestCaseJ4 {
   
   protected void setupHadoopConfig(Configuration config) throws IOException {
     
    String tempDir = createTempDir().getAbsolutePath();
    String tempDir = createTempDir().toFile().getAbsolutePath();
 
     FileUtils.copyFile(new File(RESOURCES_DIR + "/custom-mimetypes.xml"), new File(tempDir + "/custom-mimetypes.xml"));
 
diff --git a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MapReduceIndexerToolArgumentParserTest.java b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MapReduceIndexerToolArgumentParserTest.java
index 6909ce3f68a..269282d87d0 100644
-- a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MapReduceIndexerToolArgumentParserTest.java
++ b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MapReduceIndexerToolArgumentParserTest.java
@@ -57,7 +57,7 @@ public class MapReduceIndexerToolArgumentParserTest extends SolrTestCaseJ4 {
     
   private static final Logger LOG = LoggerFactory.getLogger(MapReduceIndexerToolArgumentParserTest.class);
   
  private final File solrHomeDirectory = createTempDir();
  private final File solrHomeDirectory = createTempDir().toFile();
   
   @BeforeClass
   public static void beforeClass() {
diff --git a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineBasicMiniMRTest.java b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineBasicMiniMRTest.java
index cd90d012fde..0c8b3a0d374 100644
-- a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineBasicMiniMRTest.java
++ b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineBasicMiniMRTest.java
@@ -109,7 +109,7 @@ public class MorphlineBasicMiniMRTest extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void setupClass() throws Exception {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     assumeTrue(
         "Currently this test can only be run without the lucene test security policy in place",
         System.getProperty("java.security.manager", "").equals(""));
@@ -123,7 +123,7 @@ public class MorphlineBasicMiniMRTest extends SolrTestCaseJ4 {
     
     AbstractZkTestCase.SOLRHOME = solrHomeDirectory;
     FileUtils.copyDirectory(MINIMR_CONF_DIR, solrHomeDirectory);
    File dataDir = createTempDir();
    File dataDir = createTempDir().toFile();
     tempDir = dataDir.getAbsolutePath();
     new File(tempDir).mkdirs();
     FileUtils.copyFile(new File(RESOURCES_DIR + "/custom-mimetypes.xml"), new File(tempDir + "/custom-mimetypes.xml"));
diff --git a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
index 6d8c0863e6a..b49d94049b6 100644
-- a/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
++ b/solr/contrib/map-reduce/src/test/org/apache/solr/hadoop/MorphlineGoLiveMiniMRTest.java
@@ -132,7 +132,7 @@ public class MorphlineGoLiveMiniMRTest extends AbstractFullDistribZkTestBase {
     System.setProperty("solr.hdfs.blockcache.enabled", Boolean.toString(LuceneTestCase.random().nextBoolean()));
     System.setProperty("solr.hdfs.blockcache.blocksperbank", "2048");
     
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     assumeTrue(
             "Currently this test can only be run without the lucene test security policy in place",
             System.getProperty("java.security.manager", "").equals(""));
@@ -146,7 +146,7 @@ public class MorphlineGoLiveMiniMRTest extends AbstractFullDistribZkTestBase {
     
     AbstractZkTestCase.SOLRHOME = solrHomeDirectory;
     FileUtils.copyDirectory(MINIMR_INSTANCE_DIR, AbstractZkTestCase.SOLRHOME);
    tempDir = createTempDir().getAbsolutePath();
    tempDir = createTempDir().toFile().getAbsolutePath();
 
     new File(tempDir).mkdirs();
 
diff --git a/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineTestBase.java b/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineTestBase.java
index 584284d1555..b5698ae4b96 100644
-- a/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineTestBase.java
++ b/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineTestBase.java
@@ -134,7 +134,7 @@ public class AbstractSolrMorphlineTestBase extends SolrTestCaseJ4 {
     testServer = new SolrServerDocumentLoader(solrServer, batchSize);
     deleteAllDocuments();
     
    tempDir = createTempDir().getAbsolutePath();
    tempDir = createTempDir().toFile().getAbsolutePath();
   }
   
   @After
diff --git a/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineZkTestBase.java b/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineZkTestBase.java
index da9ef6324fa..13e47960744 100644
-- a/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineZkTestBase.java
++ b/solr/contrib/morphlines-core/src/test/org/apache/solr/morphlines/solr/AbstractSolrMorphlineZkTestBase.java
@@ -71,7 +71,7 @@ public abstract class AbstractSolrMorphlineZkTestBase extends AbstractFullDistri
   public static void setupClass() throws Exception {
     assumeFalse("This test fails on UNIX with Turkish default locale (https://issues.apache.org/jira/browse/SOLR-6387)",
         new Locale("tr").getLanguage().equals(Locale.getDefault().getLanguage()));
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     AbstractZkTestCase.SOLRHOME = solrHomeDirectory;
     FileUtils.copyDirectory(SOLR_INSTANCE_DIR, solrHomeDirectory);
   }
diff --git a/solr/core/src/java/org/apache/solr/core/CachingDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/CachingDirectoryFactory.java
index 69984bb7064..3f2aecd115e 100644
-- a/solr/core/src/java/org/apache/solr/core/CachingDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/CachingDirectoryFactory.java
@@ -497,9 +497,9 @@ public abstract class CachingDirectoryFactory extends DirectoryFactory {
     
     if ("simple".equals(lockType)) {
       // multiple SimpleFSLockFactory instances should be OK
      dir.setLockFactory(new SimpleFSLockFactory(lockPath));
      dir.setLockFactory(new SimpleFSLockFactory(new File(lockPath).toPath()));
     } else if ("native".equals(lockType)) {
      dir.setLockFactory(new NativeFSLockFactory(lockPath));
      dir.setLockFactory(new NativeFSLockFactory(new File(lockPath).toPath()));
     } else if ("single".equals(lockType)) {
       if (!(dir.getLockFactory() instanceof SingleInstanceLockFactory)) dir
           .setLockFactory(new SingleInstanceLockFactory());
diff --git a/solr/core/src/java/org/apache/solr/core/MMapDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/MMapDirectoryFactory.java
index 4bc5b599c43..0dd2cdb7bb1 100644
-- a/solr/core/src/java/org/apache/solr/core/MMapDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/MMapDirectoryFactory.java
@@ -28,6 +28,7 @@ import org.slf4j.LoggerFactory;
 
 import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 
 /**
@@ -36,7 +37,7 @@ import java.io.IOException;
  * Can set the following parameters:
  * <ul>
  *  <li>unmap -- See {@link MMapDirectory#setUseUnmap(boolean)}</li>
 *  <li>maxChunkSize -- The Max chunk size.  See {@link MMapDirectory#MMapDirectory(File, LockFactory, int)}</li>
 *  <li>maxChunkSize -- The Max chunk size.  See {@link MMapDirectory#MMapDirectory(Path, LockFactory, int)}</li>
  * </ul>
  *
  **/
@@ -58,7 +59,7 @@ public class MMapDirectoryFactory extends StandardDirectoryFactory {
 
   @Override
   protected Directory create(String path, DirContext dirContext) throws IOException {
    MMapDirectory mapDirectory = new MMapDirectory(new File(path), null, maxChunk);
    MMapDirectory mapDirectory = new MMapDirectory(new File(path).toPath(), null, maxChunk);
     try {
       mapDirectory.setUseUnmap(unmapHack);
     } catch (Exception e) {
diff --git a/solr/core/src/java/org/apache/solr/core/NIOFSDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/NIOFSDirectoryFactory.java
index 303f001552d..7c6f5ed523a 100644
-- a/solr/core/src/java/org/apache/solr/core/NIOFSDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/NIOFSDirectoryFactory.java
@@ -31,7 +31,7 @@ public class NIOFSDirectoryFactory extends StandardDirectoryFactory {
 
   @Override
   protected Directory create(String path, DirContext dirContext) throws IOException {
    return new NIOFSDirectory(new File(path));
    return new NIOFSDirectory(new File(path).toPath());
   }
   
   @Override
diff --git a/solr/core/src/java/org/apache/solr/core/NRTCachingDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/NRTCachingDirectoryFactory.java
index 536e24429dd..525c428589d 100644
-- a/solr/core/src/java/org/apache/solr/core/NRTCachingDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/NRTCachingDirectoryFactory.java
@@ -51,7 +51,7 @@ public class NRTCachingDirectoryFactory extends StandardDirectoryFactory {
 
   @Override
   protected Directory create(String path, DirContext dirContext) throws IOException {
    return new NRTCachingDirectory(FSDirectory.open(new File(path)), maxMergeSizeMB, maxCachedMB);
    return new NRTCachingDirectory(FSDirectory.open(new File(path).toPath()), maxMergeSizeMB, maxCachedMB);
   }
   
   @Override
diff --git a/solr/core/src/java/org/apache/solr/core/SimpleFSDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/SimpleFSDirectoryFactory.java
index 7489d7512ff..d6835b80046 100644
-- a/solr/core/src/java/org/apache/solr/core/SimpleFSDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/SimpleFSDirectoryFactory.java
@@ -31,7 +31,7 @@ public class SimpleFSDirectoryFactory extends StandardDirectoryFactory {
 
   @Override
   protected Directory create(String path, DirContext dirContext) throws IOException {
    return new SimpleFSDirectory(new File(path));
    return new SimpleFSDirectory(new File(path).toPath());
   }
 
   @Override
diff --git a/solr/core/src/java/org/apache/solr/core/SolrDeletionPolicy.java b/solr/core/src/java/org/apache/solr/core/SolrDeletionPolicy.java
index bbe810ced4e..303fbd7cc7d 100644
-- a/solr/core/src/java/org/apache/solr/core/SolrDeletionPolicy.java
++ b/solr/core/src/java/org/apache/solr/core/SolrDeletionPolicy.java
@@ -211,7 +211,7 @@ public class SolrDeletionPolicy extends IndexDeletionPolicy implements NamedList
     // be the same, regardless of the Directory instance.
     if (dir instanceof FSDirectory) {
       FSDirectory fsd = (FSDirectory) dir;
      File fdir = fsd.getDirectory();
      File fdir = fsd.getDirectory().toFile();
       sb.append(fdir.getPath());
     } else {
       sb.append(dir);
diff --git a/solr/core/src/java/org/apache/solr/core/StandardDirectoryFactory.java b/solr/core/src/java/org/apache/solr/core/StandardDirectoryFactory.java
index 85bc9b0aeb8..6a13516e038 100644
-- a/solr/core/src/java/org/apache/solr/core/StandardDirectoryFactory.java
++ b/solr/core/src/java/org/apache/solr/core/StandardDirectoryFactory.java
@@ -39,7 +39,7 @@ public class StandardDirectoryFactory extends CachingDirectoryFactory {
 
   @Override
   protected Directory create(String path, DirContext dirContext) throws IOException {
    return FSDirectory.open(new File(path));
    return FSDirectory.open(new File(path).toPath());
   }
   
   @Override
@@ -96,8 +96,8 @@ public class StandardDirectoryFactory extends CachingDirectoryFactory {
     Directory baseToDir = getBaseDir(toDir);
     
     if (baseFromDir instanceof FSDirectory && baseToDir instanceof FSDirectory) {
      File dir1 = ((FSDirectory) baseFromDir).getDirectory();
      File dir2 = ((FSDirectory) baseToDir).getDirectory();
      File dir1 = ((FSDirectory) baseFromDir).getDirectory().toFile();
      File dir2 = ((FSDirectory) baseToDir).getDirectory().toFile();
       File indexFileInTmpDir = new File(dir1, fileName);
       File indexFileInIndex = new File(dir2, fileName);
       boolean success = indexFileInTmpDir.renameTo(indexFileInIndex);
diff --git a/solr/core/src/java/org/apache/solr/handler/SnapPuller.java b/solr/core/src/java/org/apache/solr/handler/SnapPuller.java
index fd92d38284c..ddc1269f1ac 100644
-- a/solr/core/src/java/org/apache/solr/handler/SnapPuller.java
++ b/solr/core/src/java/org/apache/solr/handler/SnapPuller.java
@@ -1040,7 +1040,7 @@ public class SnapPuller {
   
   static boolean delTree(File dir) {
     try {
      org.apache.lucene.util.IOUtils.rm(dir);
      org.apache.lucene.util.IOUtils.rm(dir.toPath());
       return true;
     } catch (IOException e) {
       LOG.warn("Unable to delete directory : " + dir, e);
diff --git a/solr/core/src/java/org/apache/solr/handler/SnapShooter.java b/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
index 913f339ef87..857f3b505a7 100644
-- a/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
++ b/solr/core/src/java/org/apache/solr/handler/SnapShooter.java
@@ -67,7 +67,11 @@ public class SnapShooter {
       File dir = new File(snapDir);
       if (!dir.exists())  dir.mkdirs();
     }
    lockFactory = new SimpleFSLockFactory(snapDir);
    try {
      lockFactory = new SimpleFSLockFactory(new File(snapDir).toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
     this.snapshotName = snapshotName;
 
     if(snapshotName != null) {
@@ -250,7 +254,7 @@ public class SnapShooter {
         destDir.mkdirs();
       }
       
      FSDirectory dir = FSDirectory.open(destDir);
      FSDirectory dir = FSDirectory.open(destDir.toPath());
       try {
         for (String indexFile : files) {
           copyFile(sourceDir, indexFile, new File(destDir, indexFile), dir);
diff --git a/solr/core/src/java/org/apache/solr/spelling/AbstractLuceneSpellChecker.java b/solr/core/src/java/org/apache/solr/spelling/AbstractLuceneSpellChecker.java
index 41ba2ecbabd..248dc703094 100644
-- a/solr/core/src/java/org/apache/solr/spelling/AbstractLuceneSpellChecker.java
++ b/solr/core/src/java/org/apache/solr/spelling/AbstractLuceneSpellChecker.java
@@ -228,7 +228,7 @@ public abstract class AbstractLuceneSpellChecker extends SolrSpellChecker {
    */
   protected void initIndex() throws IOException {
     if (indexDir != null) {
      index = FSDirectory.open(new File(indexDir));
      index = FSDirectory.open(new File(indexDir).toPath());
     } else {
       index = new RAMDirectory();
     }
diff --git a/solr/core/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java b/solr/core/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
index 95c9d00ffac..5726166a37d 100644
-- a/solr/core/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
++ b/solr/core/src/java/org/apache/solr/spelling/IndexBasedSpellChecker.java
@@ -64,7 +64,7 @@ public class IndexBasedSpellChecker extends AbstractLuceneSpellChecker {
   private void initSourceReader() {
     if (sourceLocation != null) {
       try {
        FSDirectory luceneIndexDir = FSDirectory.open(new File(sourceLocation));
        FSDirectory luceneIndexDir = FSDirectory.open(new File(sourceLocation).toPath());
         this.reader = DirectoryReader.open(luceneIndexDir);
       } catch (IOException e) {
         throw new RuntimeException(e);
diff --git a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
index 2327d785960..5e30fb1cfba 100644
-- a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
++ b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
@@ -93,7 +93,7 @@ public class AnalyzingInfixLookupFactory extends LookupFactory {
 
     try {
       return new AnalyzingInfixSuggester(core.getSolrConfig().luceneMatchVersion, 
                                         FSDirectory.open(new File(indexPath)), indexAnalyzer,
                                         FSDirectory.open(new File(indexPath).toPath()), indexAnalyzer,
                                          queryAnalyzer, minPrefixChars, true);
     } catch (IOException e) {
       throw new RuntimeException();
diff --git a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
index 82daeab31d7..48bdf983b27 100644
-- a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
++ b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
@@ -98,7 +98,7 @@ public class BlendedInfixLookupFactory extends AnalyzingInfixLookupFactory {
     
     try {
       return new BlendedInfixSuggester(core.getSolrConfig().luceneMatchVersion, 
                                       FSDirectory.open(new File(indexPath)),
                                       FSDirectory.open(new File(indexPath).toPath()),
                                        indexAnalyzer, queryAnalyzer, minPrefixChars,
                                        blenderType, numFactor, true);
     } catch (IOException e) {
diff --git a/solr/core/src/java/org/apache/solr/store/blockcache/BlockDirectory.java b/solr/core/src/java/org/apache/solr/store/blockcache/BlockDirectory.java
index 5c7e5142c35..2aef29829cf 100644
-- a/solr/core/src/java/org/apache/solr/store/blockcache/BlockDirectory.java
++ b/solr/core/src/java/org/apache/solr/store/blockcache/BlockDirectory.java
@@ -252,7 +252,7 @@ public class BlockDirectory extends Directory {
   
   private long getFileModified(String name) throws IOException {
     if (directory instanceof FSDirectory) {
      File directory = ((FSDirectory) this.directory).getDirectory();
      File directory = ((FSDirectory) this.directory).getDirectory().toFile();
       File file = new File(directory, name);
       if (!file.exists()) {
         throw new FileNotFoundException("File [" + name + "] not found");
diff --git a/solr/core/src/test/org/apache/solr/AnalysisAfterCoreReloadTest.java b/solr/core/src/test/org/apache/solr/AnalysisAfterCoreReloadTest.java
index d545f05aba3..81b5b80838f 100644
-- a/solr/core/src/test/org/apache/solr/AnalysisAfterCoreReloadTest.java
++ b/solr/core/src/test/org/apache/solr/AnalysisAfterCoreReloadTest.java
@@ -42,7 +42,7 @@ public class AnalysisAfterCoreReloadTest extends SolrTestCaseJ4 {
   
   @BeforeClass
   public static void beforeClass() throws Exception {
    tmpSolrHome = createTempDir().getAbsolutePath();
    tmpSolrHome = createTempDir().toFile().getAbsolutePath();
     FileUtils.copyDirectory(new File(TEST_HOME()), new File(tmpSolrHome).getAbsoluteFile());
     initCore("solrconfig.xml", "schema.xml", new File(tmpSolrHome).getAbsolutePath());
   }
diff --git a/solr/core/src/test/org/apache/solr/SolrTestCaseJ4Test.java b/solr/core/src/test/org/apache/solr/SolrTestCaseJ4Test.java
index 15532073d0a..74aeee96a66 100644
-- a/solr/core/src/test/org/apache/solr/SolrTestCaseJ4Test.java
++ b/solr/core/src/test/org/apache/solr/SolrTestCaseJ4Test.java
@@ -33,7 +33,7 @@ public class SolrTestCaseJ4Test extends SolrTestCaseJ4 {
   public static void beforeClass() throws Exception {
     // Create a temporary directory that holds a core NOT named "collection1". Use the smallest configuration sets
     // we can so we don't copy that much junk around.
    tmpSolrHome = createTempDir().getAbsolutePath();
    tmpSolrHome = createTempDir().toFile().getAbsolutePath();
 
     File subHome = new File(new File(tmpSolrHome, "core0"), "conf");
     assertTrue("Failed to make subdirectory ", subHome.mkdirs());
diff --git a/solr/core/src/test/org/apache/solr/TestSolrCoreProperties.java b/solr/core/src/test/org/apache/solr/TestSolrCoreProperties.java
index 17de9263d9b..b6000c7dce9 100644
-- a/solr/core/src/test/org/apache/solr/TestSolrCoreProperties.java
++ b/solr/core/src/test/org/apache/solr/TestSolrCoreProperties.java
@@ -41,7 +41,7 @@ public class TestSolrCoreProperties extends SolrJettyTestBase {
 
   @BeforeClass
   public static void beforeTest() throws Exception {
    File homeDir = createTempDir();
    File homeDir = createTempDir().toFile();
 
     File collDir = new File(homeDir, "collection1");
     File dataDir = new File(collDir, "data");
diff --git a/solr/core/src/test/org/apache/solr/TestTolerantSearch.java b/solr/core/src/test/org/apache/solr/TestTolerantSearch.java
index 81f65ad55da..c4d81f0e2e2 100644
-- a/solr/core/src/test/org/apache/solr/TestTolerantSearch.java
++ b/solr/core/src/test/org/apache/solr/TestTolerantSearch.java
@@ -47,7 +47,7 @@ public class TestTolerantSearch extends SolrJettyTestBase {
   private static File solrHome;
   
   private static File createSolrHome() throws Exception {
    File workDir = createTempDir();
    File workDir = createTempDir().toFile();
     setupJettyTestHome(workDir, "collection1");
     FileUtils.copyFile(new File(SolrTestCaseJ4.TEST_HOME() + "/collection1/conf/solrconfig-tolerant-search.xml"), new File(workDir, "/collection1/conf/solrconfig.xml"));
     FileUtils.copyDirectory(new File(workDir, "collection1"), new File(workDir, "collection2"));
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
index 651cbbed6b5..c8c298a41b2 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZk2Test.java
@@ -169,7 +169,7 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
       createCmd.setCoreName(ONE_NODE_COLLECTION + "core");
       createCmd.setCollection(ONE_NODE_COLLECTION);
       createCmd.setNumShards(1);
      createCmd.setDataDir(getDataDir(createTempDir(ONE_NODE_COLLECTION).getAbsolutePath()));
      createCmd.setDataDir(getDataDir(createTempDir(ONE_NODE_COLLECTION).toFile().getAbsolutePath()));
       server.request(createCmd);
       server.shutdown();
     } catch (Exception e) {
@@ -415,7 +415,7 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     ModifiableSolrParams params = new ModifiableSolrParams();
     params.set("qt", "/replication");
     params.set("command", "backup");
    File location = createTempDir();
    File location = createTempDir().toFile();
     params.set("location", location.getAbsolutePath());
 
     QueryRequest request = new QueryRequest(params);
@@ -488,7 +488,7 @@ public class BasicDistributedZk2Test extends AbstractFullDistribZkTestBase {
     assertEquals(Arrays.asList(files).toString(), 1, files.length);
     File snapDir = files[0];
     
    IOUtils.rm(snapDir);
    IOUtils.rm(snapDir.toPath());
   }
   
   private void addNewReplica() throws Exception {
diff --git a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
index ca0bea6de11..9f91893c427 100644
-- a/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/BasicDistributedZkTest.java
@@ -375,7 +375,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
     Create createCmd = new Create();
     createCmd.setCoreName("core1");
     createCmd.setCollection("the_core_collection");
    String coredataDir = createTempDir().getAbsolutePath();
    String coredataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(coredataDir);
     createCmd.setNumShards(1);
     createCmd.setSchemaName("nonexistent_schema.xml");
@@ -555,7 +555,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
 
           createCmd.setNumShards(numShards);
           try {
            String core3dataDir = createTempDir(collection).getAbsolutePath();
            String core3dataDir = createTempDir(collection).toFile().getAbsolutePath();
             createCmd.setDataDir(getDataDir(core3dataDir));
 
             server.request(createCmd);
@@ -967,7 +967,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
           if (shardId == null) {
             createCmd.setNumShards(2);
           }
          createCmd.setDataDir(getDataDir(createTempDir(collection).getAbsolutePath()));
          createCmd.setDataDir(getDataDir(createTempDir(collection).toFile().getAbsolutePath()));
           if (shardId != null) {
             createCmd.setShardId(shardId);
           }
@@ -1094,7 +1094,7 @@ public class BasicDistributedZkTest extends AbstractFullDistribZkTestBase {
             server.setSoTimeout(60000);
             Create createCmd = new Create();
             createCmd.setCoreName(collection);
            createCmd.setDataDir(getDataDir(createTempDir(collection).getAbsolutePath()));
            createCmd.setDataDir(getDataDir(createTempDir(collection).toFile().getAbsolutePath()));
             server.request(createCmd);
           } catch (Exception e) {
             e.printStackTrace();
diff --git a/solr/core/src/test/org/apache/solr/cloud/ClusterStateUpdateTest.java b/solr/core/src/test/org/apache/solr/cloud/ClusterStateUpdateTest.java
index 6bf3cd36a88..c51ba6881a5 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ClusterStateUpdateTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ClusterStateUpdateTest.java
@@ -71,7 +71,7 @@ public class ClusterStateUpdateTest extends SolrTestCaseJ4  {
 
   @BeforeClass
   public static void beforeClass() throws IOException {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     System.setProperty("solrcloud.skip.autorecovery", "true");
     System.setProperty("genericCoreNodeNames", "false");
     copyMinFullSetup(solrHomeDirectory);
@@ -89,7 +89,7 @@ public class ClusterStateUpdateTest extends SolrTestCaseJ4  {
   public void setUp() throws Exception {
     super.setUp();
     System.setProperty("zkClientTimeout", "3000");
    File tmpDir = createTempDir("zkData");
    File tmpDir = createTempDir("zkData").toFile();
     zkDir = tmpDir.getAbsolutePath();
     zkServer = new ZkTestServer(zkDir);
     zkServer.run();
diff --git a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
index d7d6a20ac65..4d5be3c4ac8 100644
-- a/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/CollectionsAPIDistributedZkTest.java
@@ -384,7 +384,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     Create createCmd = new Create();
     createCmd.setCoreName("halfdeletedcollection_shard1_replica1");
     createCmd.setCollection(collectionName);
    String dataDir = createTempDir().getAbsolutePath();
    String dataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(dataDir);
     createCmd.setNumShards(2);
     if (secondConfigSet) {
@@ -574,7 +574,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     Create createCmd = new Create();
     createCmd.setCoreName("halfcollection_shard1_replica1");
     createCmd.setCollection("halfcollectionblocker");
    String dataDir = createTempDir().getAbsolutePath();
    String dataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(dataDir);
     createCmd.setNumShards(1);
     if (secondConfigSet) {
@@ -585,7 +585,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     createCmd = new Create();
     createCmd.setCoreName("halfcollection_shard1_replica1");
     createCmd.setCollection("halfcollectionblocker2");
    dataDir = createTempDir().getAbsolutePath();
    dataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(dataDir);
     createCmd.setNumShards(1);
     if (secondConfigSet) {
@@ -634,7 +634,7 @@ public class CollectionsAPIDistributedZkTest extends AbstractFullDistribZkTestBa
     Create createCmd = new Create();
     createCmd.setCoreName("corewithnocollection");
     createCmd.setCollection("");
    String dataDir = createTempDir().getAbsolutePath();
    String dataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(dataDir);
     createCmd.setNumShards(1);
     if (secondConfigSet) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/ConnectionManagerTest.java b/solr/core/src/test/org/apache/solr/cloud/ConnectionManagerTest.java
index 5bee2164e54..0c568a9f42e 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ConnectionManagerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ConnectionManagerTest.java
@@ -35,7 +35,7 @@ public class ConnectionManagerTest extends SolrTestCaseJ4 {
   public void testConnectionManager() throws Exception {
     
     // setup a SolrZkClient to do some getBaseUrlForNodeName testing
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     
     ZkTestServer server = new ZkTestServer(zkDir);
     try {
@@ -68,7 +68,7 @@ public class ConnectionManagerTest extends SolrTestCaseJ4 {
   public void testLikelyExpired() throws Exception {
 
     // setup a SolrZkClient to do some getBaseUrlForNodeName testing
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     ZkTestServer server = new ZkTestServer(zkDir);
     try {
diff --git a/solr/core/src/test/org/apache/solr/cloud/LeaderElectionIntegrationTest.java b/solr/core/src/test/org/apache/solr/cloud/LeaderElectionIntegrationTest.java
index 1646e87e343..55a02a5b2e0 100644
-- a/solr/core/src/test/org/apache/solr/cloud/LeaderElectionIntegrationTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/LeaderElectionIntegrationTest.java
@@ -79,7 +79,7 @@ public class LeaderElectionIntegrationTest extends SolrTestCaseJ4 {
     
     System.setProperty("zkClientTimeout", "8000");
     
    zkDir = createTempDir("zkData").getAbsolutePath();
    zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     zkServer = new ZkTestServer(zkDir);
     zkServer.run();
     System.setProperty("zkHost", zkServer.getZkAddress());
@@ -132,7 +132,7 @@ public class LeaderElectionIntegrationTest extends SolrTestCaseJ4 {
      
   private void setupContainer(int port, String shard) throws IOException,
       ParserConfigurationException, SAXException {
    File data = createTempDir();
    File data = createTempDir().toFile();
     
     System.setProperty("hostPort", Integer.toString(port));
     System.setProperty("shard", shard);
diff --git a/solr/core/src/test/org/apache/solr/cloud/LeaderElectionTest.java b/solr/core/src/test/org/apache/solr/cloud/LeaderElectionTest.java
index c43d9fb405b..b97af3d8586 100644
-- a/solr/core/src/test/org/apache/solr/cloud/LeaderElectionTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/LeaderElectionTest.java
@@ -67,7 +67,7 @@ public class LeaderElectionTest extends SolrTestCaseJ4 {
   @Override
   public void setUp() throws Exception {
     super.setUp();
    String zkDir = createTempDir("zkData").getAbsolutePath();;
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();;
     
     server = new ZkTestServer(zkDir);
     server.setTheTickTime(1000);
diff --git a/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java b/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
index dbc1013b9d3..50730a93d65 100644
-- a/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/OverseerTest.java
@@ -203,7 +203,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testShardAssignment() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     ZkTestServer server = new ZkTestServer(zkDir);
 
@@ -258,7 +258,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testBadQueueItem() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     ZkTestServer server = new ZkTestServer(zkDir);
 
@@ -332,7 +332,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
   
   @Test
   public void testShardAssignmentBigger() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     final int nodeCount = random().nextInt(TEST_NIGHTLY ? 50 : 10)+(TEST_NIGHTLY ? 50 : 10)+1;   //how many simulated nodes (num of threads)
     final int coreCount = random().nextInt(TEST_NIGHTLY ? 100 : 11)+(TEST_NIGHTLY ? 100 : 11)+1; //how many cores to register
@@ -502,7 +502,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
   
   @Test
   public void testStateChange() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     
     ZkTestServer server = new ZkTestServer(zkDir);
     
@@ -598,7 +598,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testOverseerFailure() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     ZkTestServer server = new ZkTestServer(zkDir);
     
 
@@ -720,7 +720,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
   
   @Test
   public void testShardLeaderChange() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     final ZkTestServer server = new ZkTestServer(zkDir);
     SolrZkClient controllerClient = null;
     ZkStateReader reader = null;
@@ -775,7 +775,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testDoubleAssignment() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     
     ZkTestServer server = new ZkTestServer(zkDir);
     
@@ -839,7 +839,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testPlaceholders() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     
     ZkTestServer server = new ZkTestServer(zkDir);
     
@@ -887,7 +887,7 @@ public class OverseerTest extends SolrTestCaseJ4 {
   
   @Test
   public void testReplay() throws Exception{
    String zkDir = createTempDir().getAbsolutePath() + File.separator
    String zkDir = createTempDir().toFile().getAbsolutePath() + File.separator
         + "zookeeper/server1/data";
     ZkTestServer server = new ZkTestServer(zkDir);
     SolrZkClient zkClient = null;
diff --git a/solr/core/src/test/org/apache/solr/cloud/SharedFSAutoReplicaFailoverTest.java b/solr/core/src/test/org/apache/solr/cloud/SharedFSAutoReplicaFailoverTest.java
index 7e156594bb7..10a8afe7ecc 100644
-- a/solr/core/src/test/org/apache/solr/cloud/SharedFSAutoReplicaFailoverTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/SharedFSAutoReplicaFailoverTest.java
@@ -66,7 +66,7 @@ public class SharedFSAutoReplicaFailoverTest extends AbstractFullDistribZkTestBa
   
   @BeforeClass
   public static void hdfsFailoverBeforeClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/SolrXmlInZkTest.java b/solr/core/src/test/org/apache/solr/cloud/SolrXmlInZkTest.java
index 42708796754..03d2b5ecd4b 100644
-- a/solr/core/src/test/org/apache/solr/cloud/SolrXmlInZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/SolrXmlInZkTest.java
@@ -58,7 +58,7 @@ public class SolrXmlInZkTest extends SolrTestCaseJ4 {
   }
 
   private void setUpZkAndDiskXml(boolean toZk, boolean leaveOnLocal) throws Exception {
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     File solrHome = new File(tmpDir, "home");
     copyMinConf(new File(solrHome, "myCollect"));
     if (leaveOnLocal) {
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestLeaderElectionZkExpiry.java b/solr/core/src/test/org/apache/solr/cloud/TestLeaderElectionZkExpiry.java
index 3740c97742e..851c41f2951 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestLeaderElectionZkExpiry.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestLeaderElectionZkExpiry.java
@@ -35,8 +35,8 @@ public class TestLeaderElectionZkExpiry extends SolrTestCaseJ4 {
 
   @Test
   public void testLeaderElectionWithZkExpiry() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String ccDir = createTempDir("testLeaderElectionWithZkExpiry-solr").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
    String ccDir = createTempDir("testLeaderElectionWithZkExpiry-solr").toFile().getAbsolutePath();
     CoreContainer cc = createCoreContainer(ccDir, SOLRXML);
     final ZkTestServer server = new ZkTestServer(zkDir);
     server.setTheTickTime(1000);
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
index f5009f32b91..c61da707971 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestMiniSolrCloudCluster.java
@@ -80,7 +80,7 @@ public class TestMiniSolrCloudCluster extends LuceneTestCase {
   @BeforeClass
   public static void startup() throws Exception {
     File solrXml = new File(SolrTestCaseJ4.TEST_HOME(), "solr-no-core.xml");
    miniCluster = new MiniSolrCloudCluster(NUM_SERVERS, null, createTempDir(), solrXml, null, null);
    miniCluster = new MiniSolrCloudCluster(NUM_SERVERS, null, createTempDir().toFile(), solrXml, null, null);
   }
 
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestMultiCoreConfBootstrap.java b/solr/core/src/test/org/apache/solr/cloud/TestMultiCoreConfBootstrap.java
index 5d66b96a40a..438cc679465 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestMultiCoreConfBootstrap.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestMultiCoreConfBootstrap.java
@@ -43,8 +43,8 @@ public class TestMultiCoreConfBootstrap extends SolrTestCaseJ4 {
   @Before
   public void setUp() throws Exception {
     super.setUp();
    dataDir1 = createTempDir();
    dataDir2  = createTempDir();
    dataDir1 = createTempDir().toFile();
    dataDir2  = createTempDir().toFile();
 
     home = ExternalPaths.EXAMPLE_MULTICORE_HOME;
     System.setProperty("solr.solr.home", home);
diff --git a/solr/core/src/test/org/apache/solr/cloud/TestZkChroot.java b/solr/core/src/test/org/apache/solr/cloud/TestZkChroot.java
index 78f7233a6ea..4546bd65019 100644
-- a/solr/core/src/test/org/apache/solr/cloud/TestZkChroot.java
++ b/solr/core/src/test/org/apache/solr/cloud/TestZkChroot.java
@@ -43,7 +43,7 @@ public class TestZkChroot extends SolrTestCaseJ4 {
   public void setUp() throws Exception {
     super.setUp();
 
    zkDir = createTempDir("zkData").getAbsolutePath();
    zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     zkServer = new ZkTestServer(zkDir);
     zkServer.run();
     home = ExternalPaths.EXAMPLE_HOME;
diff --git a/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
index f66bee70ebc..774f61d1c11 100644
-- a/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/UnloadDistributedZkTest.java
@@ -88,7 +88,7 @@ public class UnloadDistributedZkTest extends BasicDistributedZkTest {
     createCmd.setCoreName("test_unload_shard_and_collection_1");
     String collection = "test_unload_shard_and_collection";
     createCmd.setCollection(collection);
    String coreDataDir = createTempDir().getAbsolutePath();
    String coreDataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(getDataDir(coreDataDir));
     createCmd.setNumShards(2);
     
@@ -103,7 +103,7 @@ public class UnloadDistributedZkTest extends BasicDistributedZkTest {
     createCmd.setCoreName("test_unload_shard_and_collection_2");
     collection = "test_unload_shard_and_collection";
     createCmd.setCollection(collection);
    coreDataDir = createTempDir().getAbsolutePath();
    coreDataDir = createTempDir().toFile().getAbsolutePath();
     createCmd.setDataDir(getDataDir(coreDataDir));
     
     server.request(createCmd);
@@ -154,7 +154,7 @@ public class UnloadDistributedZkTest extends BasicDistributedZkTest {
    * @throws Exception on any problem
    */
   private void testCoreUnloadAndLeaders() throws Exception {
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     
     // create a new collection collection
     SolrServer client = clients.get(0);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkCLITest.java b/solr/core/src/test/org/apache/solr/cloud/ZkCLITest.java
index 26f9894a566..ac2d0640add 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkCLITest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkCLITest.java
@@ -77,7 +77,7 @@ public class ZkCLITest extends SolrTestCaseJ4 {
     log.info("####SETUP_START " + getTestName());
     
     boolean useNewSolrXml = random().nextBoolean();
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     if (useNewSolrXml) {
       solrHome = ExternalPaths.EXAMPLE_HOME;
     } else {
@@ -204,7 +204,7 @@ public class ZkCLITest extends SolrTestCaseJ4 {
   
   @Test
   public void testUpConfigLinkConfigClearZk() throws Exception {
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     
     // test upconfig
     String confsetname = "confsetone";
@@ -278,7 +278,7 @@ public class ZkCLITest extends SolrTestCaseJ4 {
 
   @Test
   public void testGetFile() throws Exception {
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     
     String getNode = "/getFileNode";
     byte [] data = new String("getFileNode-data").getBytes(StandardCharsets.UTF_8);
@@ -296,7 +296,7 @@ public class ZkCLITest extends SolrTestCaseJ4 {
 
   @Test
   public void testGetFileNotExists() throws Exception {
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     String getNode = "/getFileNotExistsNode";
 
     File file = File.createTempFile("newfile", null, tmpDir);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
index 5ae7a1f1b26..6642a661340 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkControllerTest.java
@@ -84,7 +84,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
                  ZkController.generateNodeName("foo-bar", "77", "/solr/sub_dir/"));
 
     // setup a SolrZkClient to do some getBaseUrlForNodeName testing
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     ZkTestServer server = new ZkTestServer(zkDir);
     try {
@@ -154,7 +154,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testReadConfigName() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     CoreContainer cc = null;
 
     ZkTestServer server = new ZkTestServer(zkDir);
@@ -209,7 +209,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testUploadToCloud() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
 
     ZkTestServer server = new ZkTestServer(zkDir);
     ZkController zkController = null;
@@ -261,7 +261,7 @@ public class ZkControllerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testGetHostName() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     CoreContainer cc = null;
 
     ZkTestServer server = new ZkTestServer(zkDir);
diff --git a/solr/core/src/test/org/apache/solr/cloud/ZkSolrClientTest.java b/solr/core/src/test/org/apache/solr/cloud/ZkSolrClientTest.java
index 6340f78052a..283b7d1d751 100644
-- a/solr/core/src/test/org/apache/solr/cloud/ZkSolrClientTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/ZkSolrClientTest.java
@@ -51,7 +51,7 @@ public class ZkSolrClientTest extends AbstractSolrTestCase {
     }
 
     ZkConnection(boolean makeRoot) throws Exception {
      String zkDir = createTempDir("zkData").getAbsolutePath();
      String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
       server = new ZkTestServer(zkDir);
       server.run();
 
@@ -109,7 +109,7 @@ public class ZkSolrClientTest extends AbstractSolrTestCase {
   }
 
   public void testReconnect() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     ZkTestServer server = null;
     SolrZkClient zkClient = null;
     try {
@@ -202,7 +202,7 @@ public class ZkSolrClientTest extends AbstractSolrTestCase {
   }
   
   public void testZkCmdExectutor() throws Exception {
    String zkDir = createTempDir("zkData").getAbsolutePath();
    String zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     ZkTestServer server = null;
 
     try {
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZk2Test.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZk2Test.java
index 9baf3e03bd8..71103ee200a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZk2Test.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZk2Test.java
@@ -37,7 +37,7 @@ public class HdfsBasicDistributedZk2Test extends BasicDistributedZk2Test {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZkTest.java
index 4125490485b..928e65d9556 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsBasicDistributedZkTest.java
@@ -39,7 +39,7 @@ public class HdfsBasicDistributedZkTest extends BasicDistributedZkTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsChaosMonkeySafeLeaderTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsChaosMonkeySafeLeaderTest.java
index 8a0e4a6ffc8..c39de4e6bd7 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsChaosMonkeySafeLeaderTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsChaosMonkeySafeLeaderTest.java
@@ -38,7 +38,7 @@ public class HdfsChaosMonkeySafeLeaderTest extends ChaosMonkeySafeLeaderTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsCollectionsAPIDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsCollectionsAPIDistributedZkTest.java
index eb454127aa1..c777e063920 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsCollectionsAPIDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsCollectionsAPIDistributedZkTest.java
@@ -37,7 +37,7 @@ public class HdfsCollectionsAPIDistributedZkTest extends CollectionsAPIDistribut
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
     
     System.setProperty("solr.hdfs.home", dfsCluster.getURI().toString() + "/solr");
     System.setProperty("solr.hdfs.blockcache.enabled", "false");
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsRecoveryZkTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsRecoveryZkTest.java
index 8d254c04ded..797a323a23e 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsRecoveryZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsRecoveryZkTest.java
@@ -37,7 +37,7 @@ public class HdfsRecoveryZkTest extends RecoveryZkTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
     System.setProperty("solr.hdfs.blockcache.blocksperbank", "2048");
   }
   
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsSyncSliceTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsSyncSliceTest.java
index 304eb333ecf..8f51c1ae2ee 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsSyncSliceTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsSyncSliceTest.java
@@ -38,7 +38,7 @@ public class HdfsSyncSliceTest extends SyncSliceTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsUnloadDistributedZkTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsUnloadDistributedZkTest.java
index b5faf418818..45a2802755a 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsUnloadDistributedZkTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsUnloadDistributedZkTest.java
@@ -37,7 +37,7 @@ public class HdfsUnloadDistributedZkTest extends UnloadDistributedZkTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsWriteToMultipleCollectionsTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsWriteToMultipleCollectionsTest.java
index 39b25b1a346..21eec1fe55d 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsWriteToMultipleCollectionsTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/HdfsWriteToMultipleCollectionsTest.java
@@ -59,7 +59,7 @@ public class HdfsWriteToMultipleCollectionsTest extends BasicDistributedZkTest {
   @BeforeClass
   public static void setupClass() throws Exception {
     schemaString = "schema15.xml";      // we need a string id
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
     System.setProperty(SOLR_HDFS_HOME, dfsCluster.getURI().toString() + "/solr");
   }
   
diff --git a/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java b/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
index 040be68b17c..d64c723097c 100644
-- a/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
++ b/solr/core/src/test/org/apache/solr/cloud/hdfs/StressHdfsTest.java
@@ -61,7 +61,7 @@ public class StressHdfsTest extends BasicDistributedZkTest {
   
   @BeforeClass
   public static void setupClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
     System.setProperty("solr.hdfs.home", dfsCluster.getURI().toString() + "/solr");
   }
   
diff --git a/solr/core/src/test/org/apache/solr/core/AlternateDirectoryTest.java b/solr/core/src/test/org/apache/solr/core/AlternateDirectoryTest.java
index b78683177f2..69729f74c0f 100644
-- a/solr/core/src/test/org/apache/solr/core/AlternateDirectoryTest.java
++ b/solr/core/src/test/org/apache/solr/core/AlternateDirectoryTest.java
@@ -58,7 +58,7 @@ public class AlternateDirectoryTest extends SolrTestCaseJ4 {
     public Directory create(String path, DirContext dirContext) throws IOException {
       openCalled = true;
 
      return dir = newFSDirectory(new File(path));
      return dir = newFSDirectory(new File(path).toPath());
     }
 
   }
diff --git a/solr/core/src/test/org/apache/solr/core/CoreContainerCoreInitFailuresTest.java b/solr/core/src/test/org/apache/solr/core/CoreContainerCoreInitFailuresTest.java
index 2093fd03591..af374e19f08 100644
-- a/solr/core/src/test/org/apache/solr/core/CoreContainerCoreInitFailuresTest.java
++ b/solr/core/src/test/org/apache/solr/core/CoreContainerCoreInitFailuresTest.java
@@ -35,7 +35,7 @@ public class CoreContainerCoreInitFailuresTest extends SolrTestCaseJ4 {
   CoreContainer cc = null;
 
   private void init(final String dirSuffix) {
    solrHome = createTempDir(dirSuffix);
    solrHome = createTempDir(dirSuffix).toFile();
   }
 
   @After
diff --git a/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java b/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
index cb6a1a49832..d5f62511e8d 100644
-- a/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
++ b/solr/core/src/test/org/apache/solr/core/OpenCloseCoreStressTest.java
@@ -85,7 +85,7 @@ public class OpenCloseCoreStressTest extends SolrTestCaseJ4 {
     coreNames = new ArrayList<>();
     cumulativeDocs = 0;
 
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
 
     jetty = new JettySolrRunner(solrHomeDirectory.getAbsolutePath(), "/solr", 0, null, null, true, null, sslConfig);
   }
diff --git a/solr/core/src/test/org/apache/solr/core/ResourceLoaderTest.java b/solr/core/src/test/org/apache/solr/core/ResourceLoaderTest.java
index c0d12034884..0007a783a7f 100644
-- a/solr/core/src/test/org/apache/solr/core/ResourceLoaderTest.java
++ b/solr/core/src/test/org/apache/solr/core/ResourceLoaderTest.java
@@ -61,7 +61,7 @@ public class ResourceLoaderTest extends SolrTestCaseJ4
   }
 
   public void testEscapeInstanceDir() throws Exception {
    File temp = createTempDir("testEscapeInstanceDir");
    File temp = createTempDir("testEscapeInstanceDir").toFile();
     try {
       temp.mkdirs();
       new File(temp, "dummy.txt").createNewFile();
@@ -77,7 +77,7 @@ public class ResourceLoaderTest extends SolrTestCaseJ4
       }
       loader.close();
     } finally {
      IOUtils.rm(temp);
      IOUtils.rm(temp.toPath());
     }
   }
 
@@ -171,7 +171,7 @@ public class ResourceLoaderTest extends SolrTestCaseJ4
   }
 
   public void testClassLoaderLibs() throws Exception {
    File tmpRoot = createTempDir("testClassLoaderLibs");
    File tmpRoot = createTempDir("testClassLoaderLibs").toFile();
 
     File lib = new File(tmpRoot, "lib");
     lib.mkdirs();
diff --git a/solr/core/src/test/org/apache/solr/core/SolrCoreCheckLockOnStartupTest.java b/solr/core/src/test/org/apache/solr/core/SolrCoreCheckLockOnStartupTest.java
index 391690943b0..5ca1e9b1ed9 100644
-- a/solr/core/src/test/org/apache/solr/core/SolrCoreCheckLockOnStartupTest.java
++ b/solr/core/src/test/org/apache/solr/core/SolrCoreCheckLockOnStartupTest.java
@@ -48,7 +48,7 @@ public class SolrCoreCheckLockOnStartupTest extends SolrTestCaseJ4 {
   @Test
   public void testSimpleLockErrorOnStartup() throws Exception {
 
    Directory directory = newFSDirectory(new File(initCoreDataDir, "index"), new SimpleFSLockFactory());
    Directory directory = newFSDirectory(new File(initCoreDataDir, "index").toPath(), new SimpleFSLockFactory());
     //creates a new IndexWriter without releasing the lock yet
     IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(null));
 
@@ -74,7 +74,7 @@ public class SolrCoreCheckLockOnStartupTest extends SolrTestCaseJ4 {
 
     File indexDir = new File(initCoreDataDir, "index");
     log.info("Acquiring lock on {}", indexDir.getAbsolutePath());
    Directory directory = newFSDirectory(indexDir, new NativeFSLockFactory());
    Directory directory = newFSDirectory(indexDir.toPath(), new NativeFSLockFactory());
     //creates a new IndexWriter without releasing the lock yet
     IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(null));
 
diff --git a/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java b/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
index 362408fbf6d..1c5cfb82abf 100644
-- a/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
++ b/solr/core/src/test/org/apache/solr/core/SolrCoreTest.java
@@ -63,7 +63,7 @@ public class SolrCoreTest extends SolrTestCaseJ4 {
     core.close();
 
     CoreDescriptor cd = new CoreDescriptor(cores, COLLECTION1, "collection1",
                                            CoreDescriptor.CORE_DATADIR, createTempDir("dataDir2").getAbsolutePath());
                                            CoreDescriptor.CORE_DATADIR, createTempDir("dataDir2").toFile().getAbsolutePath());
     
     cores.create(cd);
     
diff --git a/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java b/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
index 87dab276775..d78d28acdc0 100644
-- a/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
++ b/solr/core/src/test/org/apache/solr/core/TestArbitraryIndexDir.java
@@ -73,7 +73,7 @@ public class TestArbitraryIndexDir extends AbstractSolrTestCase{
   public void setUp() throws Exception {
     super.setUp();
     
    File tmpDataDir = createTempDir();
    File tmpDataDir = createTempDir().toFile();
 
     solrConfig = TestHarness.createConfig(getSolrHome(), "solrconfig.xml");
     h = new TestHarness( tmpDataDir.getAbsolutePath(),
@@ -112,7 +112,7 @@ public class TestArbitraryIndexDir extends AbstractSolrTestCase{
     }
 
     //add a doc in the new index dir
    Directory dir = newFSDirectory(newDir);
    Directory dir = newFSDirectory(newDir.toPath());
     IndexWriter iw = new IndexWriter(
         dir,
         new IndexWriterConfig(new StandardAnalyzer())
diff --git a/solr/core/src/test/org/apache/solr/core/TestConfigSets.java b/solr/core/src/test/org/apache/solr/core/TestConfigSets.java
index 4271f94bc90..faf8ad49d60 100644
-- a/solr/core/src/test/org/apache/solr/core/TestConfigSets.java
++ b/solr/core/src/test/org/apache/solr/core/TestConfigSets.java
@@ -41,7 +41,7 @@ public class TestConfigSets extends SolrTestCaseJ4 {
   public static String solrxml = "<solr><str name=\"configSetBaseDir\">${configsets:configsets}</str></solr>";
 
   public CoreContainer setupContainer(String configSetsBaseDir) {
    File testDirectory = createTempDir();
    File testDirectory = createTempDir().toFile();
 
     System.setProperty("configsets", configSetsBaseDir);
 
diff --git a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
index 37f11e30e90..2b97664650b 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
++ b/solr/core/src/test/org/apache/solr/core/TestCoreContainer.java
@@ -71,7 +71,7 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
 
   private CoreContainer init(String dirName) throws Exception {
 
    solrHomeDirectory = createTempDir(dirName);
    solrHomeDirectory = createTempDir(dirName).toFile();
 
     FileUtils.copyDirectory(new File(SolrTestCaseJ4.TEST_HOME()), solrHomeDirectory);
     System.out.println("Using solrconfig from " + new File(SolrTestCaseJ4.TEST_HOME()).getAbsolutePath());
@@ -148,7 +148,7 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
   @Test
   public void testNoCores() throws IOException, ParserConfigurationException, SAXException {
     //create solrHome
    File solrHomeDirectory = createTempDir();
    File solrHomeDirectory = createTempDir().toFile();
     
     boolean oldSolrXml = random().nextBoolean();
     
@@ -221,7 +221,7 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
 
     MockCoresLocator cl = new MockCoresLocator();
 
    solrHomeDirectory = createTempDir("_deleteBadCores");
    solrHomeDirectory = createTempDir("_deleteBadCores").toFile();
     SolrResourceLoader resourceLoader = new SolrResourceLoader(solrHomeDirectory.getAbsolutePath());
     File instanceDir = new File(solrHomeDirectory, "_deleteBadCores");
     System.setProperty("configsets", getFile("solr/configsets").getAbsolutePath());
@@ -267,7 +267,7 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
 
   @Test
   public void testSharedLib() throws Exception {
    File tmpRoot = createTempDir("testSharedLib");
    File tmpRoot = createTempDir("testSharedLib").toFile();
 
     File lib = new File(tmpRoot, "lib");
     lib.mkdirs();
@@ -350,7 +350,7 @@ public class TestCoreContainer extends SolrTestCaseJ4 {
   @Test
   public void testCustomHandlers() throws Exception {
 
    solrHomeDirectory = createTempDir("_customHandlers");
    solrHomeDirectory = createTempDir("_customHandlers").toFile();
     SolrResourceLoader loader = new SolrResourceLoader(solrHomeDirectory.getAbsolutePath());
 
     ConfigSolr config = ConfigSolr.fromString(loader, CUSTOM_HANDLERS_SOLR_XML);
diff --git a/solr/core/src/test/org/apache/solr/core/TestCoreDiscovery.java b/solr/core/src/test/org/apache/solr/core/TestCoreDiscovery.java
index 0a753c277a2..3d871309227 100644
-- a/solr/core/src/test/org/apache/solr/core/TestCoreDiscovery.java
++ b/solr/core/src/test/org/apache/solr/core/TestCoreDiscovery.java
@@ -41,7 +41,7 @@ public class TestCoreDiscovery extends SolrTestCaseJ4 {
     initCore();
   }
 
  private final File solrHomeDirectory = createTempDir();
  private final File solrHomeDirectory = createTempDir().toFile();
 
   private void setMeUp(String alternateCoreDir) throws Exception {
     System.setProperty("solr.solr.home", solrHomeDirectory.getAbsolutePath());
@@ -195,7 +195,7 @@ public class TestCoreDiscovery extends SolrTestCaseJ4 {
   @Test
   public void testAlternateCoreDir() throws Exception {
 
    File alt = createTempDir();
    File alt = createTempDir().toFile();
 
     setMeUp(alt.getAbsolutePath());
     addCoreWithProps(makeCorePropFile("core1", false, true, "dataDir=core1"),
@@ -213,7 +213,7 @@ public class TestCoreDiscovery extends SolrTestCaseJ4 {
   }
   @Test
   public void testNoCoreDir() throws Exception {
    File noCoreDir = createTempDir();
    File noCoreDir = createTempDir().toFile();
     setMeUp(noCoreDir.getAbsolutePath());
     addCoreWithProps(makeCorePropFile("core1", false, true),
         new File(noCoreDir, "core1" + File.separator + CorePropertiesLocator.PROPERTIES_FILENAME));
diff --git a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
index 81e2d0c3eab..befd579993b 100644
-- a/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
++ b/solr/core/src/test/org/apache/solr/core/TestLazyCores.java
@@ -62,7 +62,7 @@ public class TestLazyCores extends SolrTestCaseJ4 {
   }
 
   private CoreContainer init() throws Exception {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     
     for (int idx = 1; idx < 10; ++idx) {
       copyMinConf(new File(solrHomeDirectory, "collection" + idx));
@@ -585,7 +585,7 @@ public class TestLazyCores extends SolrTestCaseJ4 {
   private CoreContainer initGoodAndBad(List<String> goodCores,
                                        List<String> badSchemaCores,
                                        List<String> badConfigCores) throws Exception {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     
     // Don't pollute the log with exception traces when they're expected.
     ignoreException(Pattern.quote("SAXParseException"));
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrXml.java b/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
index 19b0e601fad..ab6e9b33a2e 100644
-- a/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
++ b/solr/core/src/test/org/apache/solr/core/TestSolrXml.java
@@ -48,7 +48,7 @@ public class TestSolrXml extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void setupLoader() throws Exception {
    solrHome = createTempDir();
    solrHome = createTempDir().toFile();
     loader = new SolrResourceLoader(solrHome.getAbsolutePath());
   }
 
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistence.java b/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistence.java
index 2878082b152..2e43e61f3ca 100644
-- a/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistence.java
++ b/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistence.java
@@ -54,7 +54,7 @@ import static org.hamcrest.core.Is.is;
 
 public class TestSolrXmlPersistence extends SolrTestCaseJ4 {
 
  private File solrHomeDirectory = createTempDir();
  private File solrHomeDirectory = createTempDir().toFile();
 
   @Rule
   public TestRule solrTestRules =
@@ -62,7 +62,7 @@ public class TestSolrXmlPersistence extends SolrTestCaseJ4 {
 
   @Before
   public void setupTest() {
    solrHomeDirectory = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    solrHomeDirectory = createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toFile();
   }
 
   private CoreContainer init(String solrXmlString, String... subDirs) throws Exception {
diff --git a/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistor.java b/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistor.java
index 41ef686aa1c..f93117f54fb 100644
-- a/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistor.java
++ b/solr/core/src/test/org/apache/solr/core/TestSolrXmlPersistor.java
@@ -62,7 +62,7 @@ public class TestSolrXmlPersistor  extends SolrTestCaseJ4 {
     
     final String solrxml = "<solr><cores></cores></solr>";
     
    final File solrHomeDirectory = createTempDir();
    final File solrHomeDirectory = createTempDir().toFile();
     
     copyMinFullSetup(solrHomeDirectory);
     
diff --git a/solr/core/src/test/org/apache/solr/handler/TestCSVLoader.java b/solr/core/src/test/org/apache/solr/handler/TestCSVLoader.java
index 031c0178d92..2353e2b9f42 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestCSVLoader.java
++ b/solr/core/src/test/org/apache/solr/handler/TestCSVLoader.java
@@ -51,7 +51,7 @@ public class TestCSVLoader extends SolrTestCaseJ4 {
     // if you override setUp or tearDown, you better call
     // the super classes version
     super.setUp();
    File tempDir = createTempDir("TestCSVLoader");
    File tempDir = createTempDir("TestCSVLoader").toFile();
     file = new File(tempDir, "solr_tmp.csv");
     filename = file.getPath();
     cleanup();
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
index 270fcb015e1..f8d0a7616cc 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandler.java
@@ -103,12 +103,12 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
 //    System.setProperty("solr.directoryFactory", "solr.StandardDirectoryFactory");
     // For manual testing only
     // useFactory(null); // force an FS factory.
    master = new SolrInstance(createTempDir("solr-instance"), "master", null);
    master = new SolrInstance(createTempDir("solr-instance").toFile(), "master", null);
     master.setUp();
     masterJetty = createJetty(master);
     masterClient = createNewSolrServer(masterJetty.getLocalPort());
 
    slave = new SolrInstance(createTempDir("solr-instance"), "slave", masterJetty.getLocalPort());
    slave = new SolrInstance(createTempDir("solr-instance").toFile(), "slave", masterJetty.getLocalPort());
     slave.setUp();
     slaveJetty = createJetty(slave);
     slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
@@ -312,7 +312,7 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
     JettySolrRunner repeaterJetty = null;
     SolrServer repeaterClient = null;
     try {
      repeater = new SolrInstance(createTempDir("solr-instance"), "repeater", masterJetty.getLocalPort());
      repeater = new SolrInstance(createTempDir("solr-instance").toFile(), "repeater", masterJetty.getLocalPort());
       repeater.setUp();
       repeaterJetty = createJetty(repeater);
       repeaterClient = createNewSolrServer(repeaterJetty.getLocalPort());
@@ -899,7 +899,7 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
     slaveClient = createNewSolrServer(slaveJetty.getLocalPort());
 
     try {
      repeater = new SolrInstance(createTempDir("solr-instance"), "repeater", null);
      repeater = new SolrInstance(createTempDir("solr-instance").toFile(), "repeater", null);
       repeater.setUp();
       repeater.copyConfigFile(CONF_DIR + "solrconfig-repeater.xml",
           "solrconfig.xml");
@@ -1438,7 +1438,7 @@ public class TestReplicationHandler extends SolrTestCaseJ4 {
     }
 
     public void tearDown() throws Exception {
      IOUtils.rm(homeDir);
      IOUtils.rm(homeDir.toPath());
     }
 
     public void copyConfigFile(String srcFile, String destFile) 
diff --git a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
index fd34fb25475..ea87803e6f2 100644
-- a/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
++ b/solr/core/src/test/org/apache/solr/handler/TestReplicationHandlerBackup.java
@@ -43,6 +43,7 @@ import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URL;
import java.nio.file.Path;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
@@ -96,7 +97,7 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
       addNumberToKeepInRequest = false;
       backupKeepParamName = ReplicationHandler.NUMBER_BACKUPS_TO_KEEP_INIT_PARAM;
     }
    master = new TestReplicationHandler.SolrInstance(createTempDir("solr-instance"), "master", null);
    master = new TestReplicationHandler.SolrInstance(createTempDir("solr-instance").toFile(), "master", null);
     master.setUp();
     master.copyConfigFile(CONF_DIR + configFile, "solrconfig.xml");
 
@@ -193,7 +194,7 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
         }
         assertEquals(1, files.length);
         snapDir[i] = files[0];
        Directory dir = new SimpleFSDirectory(snapDir[i].getAbsoluteFile());
        Directory dir = new SimpleFSDirectory(snapDir[i].getAbsoluteFile().toPath());
         IndexReader reader = DirectoryReader.open(dir);
         IndexSearcher searcher = new IndexSearcher(reader);
         TopDocs hits = searcher.search(new MatchAllDocsQuery(), 1);
@@ -214,7 +215,11 @@ public class TestReplicationHandlerBackup extends SolrJettyTestBase {
 
     } finally {
       if(!namedBackup) {
        org.apache.lucene.util.IOUtils.rm(snapDir);
        Path toDelete[] = new Path[snapDir.length];
        for (int i = 0; i < snapDir.length; i++) {
          toDelete[i] = snapDir[i].toPath();
        }
        org.apache.lucene.util.IOUtils.rm(toDelete);
       }
     }
   }
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminCreateDiscoverTest.java b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminCreateDiscoverTest.java
index ccd43c0071e..edd5fd6520e 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminCreateDiscoverTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminCreateDiscoverTest.java
@@ -50,7 +50,7 @@ public class CoreAdminCreateDiscoverTest extends SolrTestCaseJ4 {
   public static void beforeClass() throws Exception {
     useFactory(null); // I require FS-based indexes for this test.
 
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
 
     setupNoCoreTest(solrHomeDirectory, null);
 
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminHandlerTest.java b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminHandlerTest.java
index ebb1b5b6e8b..c6a93abc890 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminHandlerTest.java
@@ -59,7 +59,7 @@ public class CoreAdminHandlerTest extends SolrTestCaseJ4 {
   public void testCreateWithSysVars() throws Exception {
     useFactory(null); // I require FS-based indexes for this test.
 
    final File workDir = createTempDir(getCoreName());
    final File workDir = createTempDir(getCoreName()).toFile();
 
     String coreName = "with_sys_vars";
     File instDir = new File(workDir, coreName);
@@ -124,7 +124,7 @@ public class CoreAdminHandlerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testCoreAdminHandler() throws Exception {
    final File workDir = createTempDir();
    final File workDir = createTempDir().toFile();
     
     final CoreContainer cores = h.getCoreContainer();
 
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminRequestStatusTest.java b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminRequestStatusTest.java
index afa5306b0c0..017f443398d 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminRequestStatusTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/CoreAdminRequestStatusTest.java
@@ -37,7 +37,7 @@ public class CoreAdminRequestStatusTest extends SolrTestCaseJ4{
 
   @Test
   public void testCoreAdminRequestStatus() throws Exception {
    final File workDir = createTempDir();
    final File workDir = createTempDir().toFile();
 
     final CoreContainer cores = h.getCoreContainer();
 
diff --git a/solr/core/src/test/org/apache/solr/handler/admin/CoreMergeIndexesAdminHandlerTest.java b/solr/core/src/test/org/apache/solr/handler/admin/CoreMergeIndexesAdminHandlerTest.java
index b8c1e782357..32b7e02800e 100644
-- a/solr/core/src/test/org/apache/solr/handler/admin/CoreMergeIndexesAdminHandlerTest.java
++ b/solr/core/src/test/org/apache/solr/handler/admin/CoreMergeIndexesAdminHandlerTest.java
@@ -71,7 +71,7 @@ public class CoreMergeIndexesAdminHandlerTest extends SolrTestCaseJ4 {
 
   @Test
   public void testMergeIndexesCoreAdminHandler() throws Exception {
    final File workDir = createTempDir();
    final File workDir = createTempDir().toFile();
 
     final CoreContainer cores = h.getCoreContainer();
 
diff --git a/solr/core/src/test/org/apache/solr/handler/component/DistributedDebugComponentTest.java b/solr/core/src/test/org/apache/solr/handler/component/DistributedDebugComponentTest.java
index 3484711f2ed..90f5e21c4f7 100644
-- a/solr/core/src/test/org/apache/solr/handler/component/DistributedDebugComponentTest.java
++ b/solr/core/src/test/org/apache/solr/handler/component/DistributedDebugComponentTest.java
@@ -53,7 +53,7 @@ public class DistributedDebugComponentTest extends SolrJettyTestBase {
   private static File solrHome;
   
   private static File createSolrHome() throws Exception {
    File workDir = createTempDir();
    File workDir = createTempDir().toFile();
     setupJettyTestHome(workDir, "collection1");
     FileUtils.copyDirectory(new File(workDir, "collection1"), new File(workDir, "collection2"));
     return workDir;
diff --git a/solr/core/src/test/org/apache/solr/request/TestRemoteStreaming.java b/solr/core/src/test/org/apache/solr/request/TestRemoteStreaming.java
index 1fdfb23580b..2c2a75410aa 100644
-- a/solr/core/src/test/org/apache/solr/request/TestRemoteStreaming.java
++ b/solr/core/src/test/org/apache/solr/request/TestRemoteStreaming.java
@@ -56,7 +56,7 @@ public class TestRemoteStreaming extends SolrJettyTestBase {
   @BeforeClass
   public static void beforeTest() throws Exception {
     //this one has handleSelect=true which a test here needs
    solrHomeDirectory = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    solrHomeDirectory = createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toFile();
     setupJettyTestHome(solrHomeDirectory, "collection1");
     createJetty(solrHomeDirectory.getAbsolutePath(), null, null);
   }
diff --git a/solr/core/src/test/org/apache/solr/rest/TestManagedResourceStorage.java b/solr/core/src/test/org/apache/solr/rest/TestManagedResourceStorage.java
index cd55901c8a1..ffc203d6f36 100644
-- a/solr/core/src/test/org/apache/solr/rest/TestManagedResourceStorage.java
++ b/solr/core/src/test/org/apache/solr/rest/TestManagedResourceStorage.java
@@ -70,7 +70,7 @@ public class TestManagedResourceStorage extends AbstractZkTestCase {
    */
   @Test
   public void testFileBasedJsonStorage() throws Exception {
    File instanceDir = createTempDir("json-storage");
    File instanceDir = createTempDir("json-storage").toFile();
     SolrResourceLoader loader = new SolrResourceLoader(instanceDir.getAbsolutePath());
     try {
       NamedList<String> initArgs = new NamedList<>();
diff --git a/solr/core/src/test/org/apache/solr/rest/TestRestManager.java b/solr/core/src/test/org/apache/solr/rest/TestRestManager.java
index d82469ae153..06f6dc48ce2 100644
-- a/solr/core/src/test/org/apache/solr/rest/TestRestManager.java
++ b/solr/core/src/test/org/apache/solr/rest/TestRestManager.java
@@ -229,7 +229,7 @@ public class TestRestManager extends SolrRestletTestBase {
   @Test
   public void testReloadFromPersistentStorage() throws Exception {
     SolrResourceLoader loader = new SolrResourceLoader("./");
    File unitTestStorageDir = createTempDir("testRestManager");
    File unitTestStorageDir = createTempDir("testRestManager").toFile();
     assertTrue(unitTestStorageDir.getAbsolutePath()+" is not a directory!", 
         unitTestStorageDir.isDirectory());    
     assertTrue(unitTestStorageDir.canRead());
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaDynamicFieldResource.java b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaDynamicFieldResource.java
index 466e37212a0..05c9c9eabc4 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaDynamicFieldResource.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaDynamicFieldResource.java
@@ -40,7 +40,7 @@ public class TestManagedSchemaDynamicFieldResource extends RestTestBase {
 
   @Before
   public void before() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldResource.java b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldResource.java
index dffa9e8b373..d14618720c5 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldResource.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldResource.java
@@ -40,7 +40,7 @@ public class TestManagedSchemaFieldResource extends RestTestBase {
 
   @Before
   public void before() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldTypeResource.java b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldTypeResource.java
index 4bac2feea1b..26fab12dcc6 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldTypeResource.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/TestManagedSchemaFieldTypeResource.java
@@ -44,7 +44,7 @@ public class TestManagedSchemaFieldTypeResource extends RestTestBase {
 
   @Before
   public void before() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedStopFilterFactory.java b/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedStopFilterFactory.java
index 9cf083b1833..3b29557959e 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedStopFilterFactory.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedStopFilterFactory.java
@@ -44,7 +44,7 @@ public class TestManagedStopFilterFactory extends RestTestBase {
 
   @Before
   public void before() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
diff --git a/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedSynonymFilterFactory.java b/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedSynonymFilterFactory.java
index 4eb1e53df78..2b20aa2dda0 100644
-- a/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedSynonymFilterFactory.java
++ b/solr/core/src/test/org/apache/solr/rest/schema/analysis/TestManagedSynonymFilterFactory.java
@@ -42,7 +42,7 @@ public class TestManagedSynonymFilterFactory extends RestTestBase {
    */
   @Before
   public void before() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     FileUtils.copyDirectory(new File(TEST_HOME()), tmpSolrHome.getAbsoluteFile());
 
     final SortedMap<ServletHolder,String> extraServlets = new TreeMap<>();
diff --git a/solr/core/src/test/org/apache/solr/schema/ChangedSchemaMergeTest.java b/solr/core/src/test/org/apache/solr/schema/ChangedSchemaMergeTest.java
index 65425a78267..8e1486a14b1 100644
-- a/solr/core/src/test/org/apache/solr/schema/ChangedSchemaMergeTest.java
++ b/solr/core/src/test/org/apache/solr/schema/ChangedSchemaMergeTest.java
@@ -40,7 +40,7 @@ public class ChangedSchemaMergeTest extends SolrTestCaseJ4 {
     initCore();
   }
 
  private final File solrHomeDirectory = createTempDir();
  private final File solrHomeDirectory = createTempDir().toFile();
   private File schemaFile = null;
 
   private void addDoc(SolrCore core, String... fieldValues) throws IOException {
diff --git a/solr/core/src/test/org/apache/solr/schema/ModifyConfFileTest.java b/solr/core/src/test/org/apache/solr/schema/ModifyConfFileTest.java
index 08774709389..43f39c67955 100644
-- a/solr/core/src/test/org/apache/solr/schema/ModifyConfFileTest.java
++ b/solr/core/src/test/org/apache/solr/schema/ModifyConfFileTest.java
@@ -43,7 +43,7 @@ import org.junit.rules.TestRule;
 import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;
 
 public class ModifyConfFileTest extends SolrTestCaseJ4 {
  private File solrHomeDirectory = createTempDir();
  private File solrHomeDirectory = createTempDir().toFile();
 
   @Rule
   public TestRule solrTestRules = RuleChain.outerRule(new SystemPropertiesRestoreRule());
@@ -52,7 +52,7 @@ public class ModifyConfFileTest extends SolrTestCaseJ4 {
     System.setProperty("solr.test.sys.prop1", "propone");
     System.setProperty("solr.test.sys.prop2", "proptwo");
 
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
 
     copySolrHomeToTemp(solrHomeDirectory, "core1", true);
     FileUtils.write(new File(new File(solrHomeDirectory, "core1"), "core.properties"), "", Charsets.UTF_8.toString());
diff --git a/solr/core/src/test/org/apache/solr/schema/TestBinaryField.java b/solr/core/src/test/org/apache/solr/schema/TestBinaryField.java
index e2b9c2b80e6..f1175358864 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestBinaryField.java
++ b/solr/core/src/test/org/apache/solr/schema/TestBinaryField.java
@@ -35,7 +35,7 @@ public class TestBinaryField extends SolrJettyTestBase {
 
   @BeforeClass
   public static void beforeTest() throws Exception {
    File homeDir = createTempDir();
    File homeDir = createTempDir().toFile();
 
     File collDir = new File(homeDir, "collection1");
     File dataDir = new File(collDir, "data");
diff --git a/solr/core/src/test/org/apache/solr/schema/TestCollationField.java b/solr/core/src/test/org/apache/solr/schema/TestCollationField.java
index b92013d751b..db63a8fa0f4 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestCollationField.java
++ b/solr/core/src/test/org/apache/solr/schema/TestCollationField.java
@@ -63,7 +63,7 @@ public class TestCollationField extends SolrTestCaseJ4 {
    */
   public static String setupSolrHome() throws Exception {
     // make a solr home underneath the test's TEMP_DIR
    File tmpFile = createTempDir("collation1");
    File tmpFile = createTempDir("collation1").toFile();
     
     // make data and conf dirs
     new File(tmpFile, "data").mkdir();
diff --git a/solr/core/src/test/org/apache/solr/schema/TestCollationFieldDocValues.java b/solr/core/src/test/org/apache/solr/schema/TestCollationFieldDocValues.java
index 70bfb79c2a5..5ecde3bf734 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestCollationFieldDocValues.java
++ b/solr/core/src/test/org/apache/solr/schema/TestCollationFieldDocValues.java
@@ -61,7 +61,7 @@ public class TestCollationFieldDocValues extends SolrTestCaseJ4 {
    */
   public static String setupSolrHome() throws Exception {
     // make a solr home underneath the test's TEMP_DIR
    File tmpFile = createTempDir("collation1");
    File tmpFile = createTempDir("collation1").toFile();
     
     // make data and conf dirs
     new File(tmpFile, "data").mkdir();
diff --git a/solr/core/src/test/org/apache/solr/schema/TestManagedSchema.java b/solr/core/src/test/org/apache/solr/schema/TestManagedSchema.java
index 0636606af41..343c27e37d4 100644
-- a/solr/core/src/test/org/apache/solr/schema/TestManagedSchema.java
++ b/solr/core/src/test/org/apache/solr/schema/TestManagedSchema.java
@@ -47,7 +47,7 @@ public class TestManagedSchema extends AbstractBadConfigTestBase {
   
   @Before
   private void initManagedSchemaCore() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     File testHomeConfDir = new File(TEST_HOME(), confDir);
     FileUtils.copyFileToDirectory(new File(testHomeConfDir, "solrconfig-managed-schema.xml"), tmpConfDir);
diff --git a/solr/core/src/test/org/apache/solr/search/TestAddFieldRealTimeGet.java b/solr/core/src/test/org/apache/solr/search/TestAddFieldRealTimeGet.java
index 7beeab99986..475f9e98f62 100644
-- a/solr/core/src/test/org/apache/solr/search/TestAddFieldRealTimeGet.java
++ b/solr/core/src/test/org/apache/solr/search/TestAddFieldRealTimeGet.java
@@ -35,7 +35,7 @@ public class TestAddFieldRealTimeGet extends TestRTGBase {
 
   @Before
   private void initManagedSchemaCore() throws Exception {
    final String tmpSolrHomePath = createTempDir().getAbsolutePath();
    final String tmpSolrHomePath = createTempDir().toFile().getAbsolutePath();
     tmpSolrHome = new File(tmpSolrHomePath).getAbsoluteFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     File testHomeConfDir = new File(TEST_HOME(), confDir);
diff --git a/solr/core/src/test/org/apache/solr/search/TestRecoveryHdfs.java b/solr/core/src/test/org/apache/solr/search/TestRecoveryHdfs.java
index d244cfec880..65af5998e61 100644
-- a/solr/core/src/test/org/apache/solr/search/TestRecoveryHdfs.java
++ b/solr/core/src/test/org/apache/solr/search/TestRecoveryHdfs.java
@@ -77,7 +77,7 @@ public class TestRecoveryHdfs extends SolrTestCaseJ4 {
   
   @BeforeClass
   public static void beforeClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
     System.setProperty("solr.hdfs.home", dfsCluster.getURI().toString() + "/solr");
     hdfsUri = dfsCluster.getFileSystem().getUri().toString();
     
diff --git a/solr/core/src/test/org/apache/solr/search/TestSearcherReuse.java b/solr/core/src/test/org/apache/solr/search/TestSearcherReuse.java
index ff056b3562d..6d759710369 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSearcherReuse.java
++ b/solr/core/src/test/org/apache/solr/search/TestSearcherReuse.java
@@ -49,7 +49,7 @@ public class TestSearcherReuse extends SolrTestCaseJ4 {
    */
   @BeforeClass
   private static void setupTempDirAndCoreWithManagedSchema() throws Exception {
    solrHome = createTempDir();
    solrHome = createTempDir().toFile();
     solrHome = solrHome.getAbsoluteFile();
 
     File confDir = new File(solrHome, confPath);
diff --git a/solr/core/src/test/org/apache/solr/servlet/CacheHeaderTest.java b/solr/core/src/test/org/apache/solr/servlet/CacheHeaderTest.java
index c00b37219be..3ce4f45c9f3 100644
-- a/solr/core/src/test/org/apache/solr/servlet/CacheHeaderTest.java
++ b/solr/core/src/test/org/apache/solr/servlet/CacheHeaderTest.java
@@ -44,7 +44,7 @@ public class CacheHeaderTest extends CacheHeaderTestBase {
     
   @BeforeClass
   public static void beforeTest() throws Exception {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     setupJettyTestHome(solrHomeDirectory, "collection1");
     createJetty(solrHomeDirectory.getAbsolutePath(), null, null);
   }
diff --git a/solr/core/src/test/org/apache/solr/servlet/ResponseHeaderTest.java b/solr/core/src/test/org/apache/solr/servlet/ResponseHeaderTest.java
index 6736cdac252..e64eae75cf6 100644
-- a/solr/core/src/test/org/apache/solr/servlet/ResponseHeaderTest.java
++ b/solr/core/src/test/org/apache/solr/servlet/ResponseHeaderTest.java
@@ -47,7 +47,7 @@ public class ResponseHeaderTest extends SolrJettyTestBase {
   
   @BeforeClass
   public static void beforeTest() throws Exception {
    solrHomeDirectory = createTempDir();
    solrHomeDirectory = createTempDir().toFile();
     setupJettyTestHome(solrHomeDirectory, "collection1");
     String top = SolrTestCaseJ4.TEST_HOME() + "/collection1/conf";
     FileUtils.copyFile(new File(top, "solrconfig-headers.xml"), new File(solrHomeDirectory + "/collection1/conf", "solrconfig.xml"));
diff --git a/solr/core/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java b/solr/core/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
index f2f0e48b44d..9f56223e953 100644
-- a/solr/core/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
++ b/solr/core/src/test/org/apache/solr/spelling/FileBasedSpellCheckerTest.java
@@ -71,7 +71,7 @@ public class FileBasedSpellCheckerTest extends SolrTestCaseJ4 {
     spellchecker.add(AbstractLuceneSpellChecker.LOCATION, "spellings.txt");
     spellchecker.add(AbstractLuceneSpellChecker.FIELD, "teststop");
     spellchecker.add(FileBasedSpellChecker.SOURCE_FILE_CHAR_ENCODING, "UTF-8");
    File indexDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    File indexDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toFile();
     spellchecker.add(AbstractLuceneSpellChecker.INDEX_DIR, indexDir.getAbsolutePath());
     SolrCore core = h.getCore();
     String dictName = checker.init(spellchecker, core);
@@ -106,7 +106,7 @@ public class FileBasedSpellCheckerTest extends SolrTestCaseJ4 {
     spellchecker.add(AbstractLuceneSpellChecker.LOCATION, "spellings.txt");
     spellchecker.add(AbstractLuceneSpellChecker.FIELD, "teststop");
     spellchecker.add(FileBasedSpellChecker.SOURCE_FILE_CHAR_ENCODING, "UTF-8");
    File indexDir = createTempDir();
    File indexDir = createTempDir().toFile();
     indexDir.mkdirs();
     spellchecker.add(AbstractLuceneSpellChecker.INDEX_DIR, indexDir.getAbsolutePath());
     spellchecker.add(SolrSpellChecker.FIELD_TYPE, "teststop");
diff --git a/solr/core/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java b/solr/core/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
index 2906a3fe71b..75de4db7ca6 100644
-- a/solr/core/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
++ b/solr/core/src/test/org/apache/solr/spelling/IndexBasedSpellCheckerTest.java
@@ -110,7 +110,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     NamedList spellchecker = new NamedList();
     spellchecker.add("classname", IndexBasedSpellChecker.class.getName());
 
    File indexDir = createTempDir();
    File indexDir = createTempDir().toFile();
 
     spellchecker.add(AbstractLuceneSpellChecker.INDEX_DIR, indexDir.getAbsolutePath());
     spellchecker.add(AbstractLuceneSpellChecker.FIELD, "title");
@@ -186,7 +186,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     NamedList spellchecker = new NamedList();
     spellchecker.add("classname", IndexBasedSpellChecker.class.getName());
 
    File indexDir = createTempDir();
    File indexDir = createTempDir().toFile();
     indexDir.mkdirs();
     spellchecker.add(AbstractLuceneSpellChecker.INDEX_DIR, indexDir.getAbsolutePath());
     spellchecker.add(AbstractLuceneSpellChecker.FIELD, "title");
@@ -243,7 +243,7 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     NamedList spellchecker = new NamedList();
     spellchecker.add("classname", IndexBasedSpellChecker.class.getName());
 
    File indexDir = createTempDir();
    File indexDir = createTempDir().toFile();
     spellchecker.add(AbstractLuceneSpellChecker.INDEX_DIR, indexDir.getAbsolutePath());
     spellchecker.add(AbstractLuceneSpellChecker.FIELD, "title");
     spellchecker.add(AbstractLuceneSpellChecker.SPELLCHECKER_ARG_NAME, spellchecker);
@@ -282,11 +282,11 @@ public class IndexBasedSpellCheckerTest extends SolrTestCaseJ4 {
     NamedList spellchecker = new NamedList();
     spellchecker.add("classname", IndexBasedSpellChecker.class.getName());
     
    File tmpDir = createTempDir();
    File tmpDir = createTempDir().toFile();
     File indexDir = new File(tmpDir, "spellingIdx");
     //create a standalone index
     File altIndexDir = new File(tmpDir, "alternateIdx" + new Date().getTime());
    Directory dir = newFSDirectory(altIndexDir);
    Directory dir = newFSDirectory(altIndexDir.toPath());
     IndexWriter iw = new IndexWriter(
         dir,
         new IndexWriterConfig(new WhitespaceAnalyzer())
diff --git a/solr/core/src/test/org/apache/solr/store/blockcache/BlockDirectoryTest.java b/solr/core/src/test/org/apache/solr/store/blockcache/BlockDirectoryTest.java
index 7deaf51f04e..4de0b42d825 100644
-- a/solr/core/src/test/org/apache/solr/store/blockcache/BlockDirectoryTest.java
++ b/solr/core/src/test/org/apache/solr/store/blockcache/BlockDirectoryTest.java
@@ -105,8 +105,8 @@ public class BlockDirectoryTest extends SolrTestCaseJ4 {
   @Before
   public void setUp() throws Exception {
     super.setUp();
    file = createTempDir();
    FSDirectory dir = FSDirectory.open(new File(file, "base"));
    file = createTempDir().toFile();
    FSDirectory dir = FSDirectory.open(new File(file, "base").toPath());
     mapperCache = new MapperCache();
     directory = new BlockDirectory("test", dir, mapperCache, null, true, true);
     random = random();
@@ -120,7 +120,7 @@ public class BlockDirectoryTest extends SolrTestCaseJ4 {
 
   @Test
   public void testEOF() throws IOException {
    Directory fsDir = FSDirectory.open(new File(file, "normal"));
    Directory fsDir = FSDirectory.open(new File(file, "normal").toPath());
     String name = "test.eof";
     createFile(name, fsDir, directory);
     long fsLength = fsDir.fileLength(name);
@@ -152,7 +152,7 @@ public class BlockDirectoryTest extends SolrTestCaseJ4 {
     int i = 0;
     try {
       for (; i < 10; i++) {
        Directory fsDir = FSDirectory.open(new File(file, "normal"));
        Directory fsDir = FSDirectory.open(new File(file, "normal").toPath());
         String name = getName();
         createFile(name, fsDir, directory);
         assertInputsEquals(name, fsDir, directory);
@@ -234,7 +234,7 @@ public class BlockDirectoryTest extends SolrTestCaseJ4 {
 
   public static void rm(File file) {
     try {
      IOUtils.rm(file);
      IOUtils.rm(file.toPath());
     } catch (Throwable ignored) {
       // TODO: should this class care if a file couldnt be deleted?
       // this just emulates previous behavior, where only SecurityException would be handled.
diff --git a/solr/core/src/test/org/apache/solr/store/hdfs/HdfsDirectoryTest.java b/solr/core/src/test/org/apache/solr/store/hdfs/HdfsDirectoryTest.java
index 533c74c09cd..3d0bc0d8e14 100644
-- a/solr/core/src/test/org/apache/solr/store/hdfs/HdfsDirectoryTest.java
++ b/solr/core/src/test/org/apache/solr/store/hdfs/HdfsDirectoryTest.java
@@ -57,7 +57,7 @@ public class HdfsDirectoryTest extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void beforeClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
   
   @AfterClass
@@ -73,7 +73,7 @@ public class HdfsDirectoryTest extends SolrTestCaseJ4 {
     Configuration conf = new Configuration();
     conf.set("dfs.permissions.enabled", "false");
     
    directory = new HdfsDirectory(new Path(dfsCluster.getURI().toString() + createTempDir().getAbsolutePath() + "/hdfs"), conf);
    directory = new HdfsDirectory(new Path(dfsCluster.getURI().toString() + createTempDir().toFile().getAbsolutePath() + "/hdfs"), conf);
     
     random = random();
   }
diff --git a/solr/core/src/test/org/apache/solr/store/hdfs/HdfsLockFactoryTest.java b/solr/core/src/test/org/apache/solr/store/hdfs/HdfsLockFactoryTest.java
index 76f7b26543d..94d895321c3 100644
-- a/solr/core/src/test/org/apache/solr/store/hdfs/HdfsLockFactoryTest.java
++ b/solr/core/src/test/org/apache/solr/store/hdfs/HdfsLockFactoryTest.java
@@ -42,7 +42,7 @@ public class HdfsLockFactoryTest extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void beforeClass() throws Exception {
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().getAbsolutePath());
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());
   }
 
   @AfterClass
diff --git a/solr/core/src/test/org/apache/solr/update/SolrIndexSplitterTest.java b/solr/core/src/test/org/apache/solr/update/SolrIndexSplitterTest.java
index a0d1279c580..f128f9ce2e4 100644
-- a/solr/core/src/test/org/apache/solr/update/SolrIndexSplitterTest.java
++ b/solr/core/src/test/org/apache/solr/update/SolrIndexSplitterTest.java
@@ -56,9 +56,9 @@ public class SolrIndexSplitterTest extends SolrTestCaseJ4 {
     super.setUp();
     clearIndex();
     assertU(commit());
    indexDir1 = createTempDir("_testSplit1");
    indexDir2 = createTempDir("_testSplit2");
    indexDir3 = createTempDir("_testSplit3");
    indexDir1 = createTempDir("_testSplit1").toFile();
    indexDir2 = createTempDir("_testSplit2").toFile();
    indexDir3 = createTempDir("_testSplit3").toFile();
   }
 
   @Test
@@ -241,7 +241,7 @@ public class SolrIndexSplitterTest extends SolrTestCaseJ4 {
 
   @Test
   public void testSplitByRouteKey() throws Exception  {
    File indexDir = createTempDir();
    File indexDir = createTempDir().toFile();
 
     CompositeIdRouter r1 = new CompositeIdRouter();
     String splitKey = "sea-line!";
diff --git a/solr/core/src/test/org/apache/solr/update/processor/AddSchemaFieldsUpdateProcessorFactoryTest.java b/solr/core/src/test/org/apache/solr/update/processor/AddSchemaFieldsUpdateProcessorFactoryTest.java
index 5b390191ce9..1d640db4a9f 100644
-- a/solr/core/src/test/org/apache/solr/update/processor/AddSchemaFieldsUpdateProcessorFactoryTest.java
++ b/solr/core/src/test/org/apache/solr/update/processor/AddSchemaFieldsUpdateProcessorFactoryTest.java
@@ -46,7 +46,7 @@ public class AddSchemaFieldsUpdateProcessorFactoryTest extends UpdateProcessorTe
 
   @Before
   private void initManagedSchemaCore() throws Exception {
    tmpSolrHome = createTempDir();
    tmpSolrHome = createTempDir().toFile();
     tmpConfDir = new File(tmpSolrHome, confDir);
     File testHomeConfDir = new File(TEST_HOME(), confDir);
     FileUtils.copyFileToDirectory(new File(testHomeConfDir, SOLRCONFIG_XML), tmpConfDir);
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/MergeIndexesExampleTestBase.java b/solr/solrj/src/test/org/apache/solr/client/solrj/MergeIndexesExampleTestBase.java
index 7a0a6185620..d11e3bba0cc 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/MergeIndexesExampleTestBase.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/MergeIndexesExampleTestBase.java
@@ -67,11 +67,11 @@ public abstract class MergeIndexesExampleTestBase extends SolrExampleTestBase {
     saveProp = System.getProperty("solr.directoryFactory");
     System.setProperty("solr.directoryFactory", "solr.StandardDirectoryFactory");
     super.setUp();
    File dataDir1 = createTempDir();
    File dataDir1 = createTempDir().toFile();
     // setup datadirs
     System.setProperty( "solr.core0.data.dir", dataDir1.getCanonicalPath() );
 
    dataDir2 = createTempDir();
    dataDir2 = createTempDir().toFile();
 
     System.setProperty( "solr.core1.data.dir", this.dataDir2.getCanonicalPath() );
 
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/MultiCoreExampleTestBase.java b/solr/solrj/src/test/org/apache/solr/client/solrj/MultiCoreExampleTestBase.java
index 9a79b4fb99e..0d7659f0812 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/MultiCoreExampleTestBase.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/MultiCoreExampleTestBase.java
@@ -57,8 +57,8 @@ public abstract class MultiCoreExampleTestBase extends SolrExampleTestBase
 
   @Override public void setUp() throws Exception {
     super.setUp();
    dataDir1 = createTempDir();
    dataDir2 = createTempDir();
    dataDir1 = createTempDir().toFile();
    dataDir2 = createTempDir().toFile();
     
     System.setProperty( "solr.core0.data.dir", this.dataDir1.getCanonicalPath() ); 
     System.setProperty( "solr.core1.data.dir", this.dataDir2.getCanonicalPath() );
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
index 8a89891041b..aee09e47ad2 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/SolrSchemalessExampleTest.java
@@ -39,7 +39,7 @@ public class SolrSchemalessExampleTest extends SolrExampleTestsBase {
 
   @BeforeClass
   public static void beforeClass() throws Exception {
    File tempSolrHome = createTempDir();
    File tempSolrHome = createTempDir().toFile();
     // Schemaless renames schema.xml -> schema.xml.bak, and creates + modifies conf/managed-schema,
     // which violates the test security manager's rules, which disallow writes outside the build dir,
     // so we copy the example/example-schemaless/solr/ directory to a new temp dir where writes are allowed. 
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrServer.java b/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrServer.java
index 2667d193121..559b1f5e925 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrServer.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/TestLBHttpSolrServer.java
@@ -93,7 +93,7 @@ public class TestLBHttpSolrServer extends SolrTestCaseJ4 {
     httpClient = HttpClientUtil.createClient(null);
     HttpClientUtil.setConnectionTimeout(httpClient,  1000);
     for (int i = 0; i < solr.length; i++) {
      solr[i] = new SolrInstance("solr/collection1" + i, createTempDir("instance-" + i), 0);
      solr[i] = new SolrInstance("solr/collection1" + i, createTempDir("instance-" + i).toFile(), 0);
       solr[i].setUp();
       solr[i].startJetty();
       addDocs(solr[i]);
@@ -306,7 +306,7 @@ public class TestLBHttpSolrServer extends SolrTestCaseJ4 {
 
     public void tearDown() throws Exception {
       if (jetty != null) jetty.stop();
      IOUtils.rm(homeDir);
      IOUtils.rm(homeDir.toPath());
     }
 
     public void startJetty() throws Exception {
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/AbstractEmbeddedSolrServerTestCase.java b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/AbstractEmbeddedSolrServerTestCase.java
index 17e5f4de01b..b55c023a262 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/AbstractEmbeddedSolrServerTestCase.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/AbstractEmbeddedSolrServerTestCase.java
@@ -46,7 +46,7 @@ public abstract class AbstractEmbeddedSolrServerTestCase extends SolrTestCaseJ4
     System.out.println("Solr home: " + SOLR_HOME.getAbsolutePath());
 
     //The index is always stored within a temporary directory
    tempDir = createTempDir();
    tempDir = createTempDir().toFile();
     
     File dataDir = new File(tempDir,"data1");
     File dataDir2 = new File(tempDir,"data2");
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
index edbde924ec5..6dc7252bf8b 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/embedded/JettyWebappTest.java
@@ -59,7 +59,7 @@ public class JettyWebappTest extends SolrTestCaseJ4
     System.setProperty("solr.solr.home", ExternalPaths.EXAMPLE_HOME);
     System.setProperty("tests.shardhandler.randomSeed", Long.toString(random().nextLong()));
 
    File dataDir = createTempDir();
    File dataDir = createTempDir().toFile();
     dataDir.mkdirs();
 
     System.setProperty("solr.data.dir", dataDir.getCanonicalPath());
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/request/SolrPingTest.java b/solr/solrj/src/test/org/apache/solr/client/solrj/request/SolrPingTest.java
index b2f637517b2..d568a4eda37 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/request/SolrPingTest.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/request/SolrPingTest.java
@@ -37,7 +37,7 @@ public class SolrPingTest extends SolrJettyTestBase {
   
   @BeforeClass
   public static void beforeClass() throws Exception {
    File testHome = createTempDir();
    File testHome = createTempDir().toFile();
     FileUtils.copyDirectory(getFile("solrj/solr"), testHome);
     initCore("solrconfig.xml", "schema.xml", testHome.getAbsolutePath(), "collection1");
   }
diff --git a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestCoreAdmin.java b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestCoreAdmin.java
index fe29cbb5b24..0c71a679c6c 100644
-- a/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestCoreAdmin.java
++ b/solr/solrj/src/test/org/apache/solr/client/solrj/request/TestCoreAdmin.java
@@ -76,7 +76,7 @@ public class TestCoreAdmin extends AbstractEmbeddedSolrServerTestCase {
   public void testConfigSet() throws Exception {
 
     SolrServer server = getSolrAdmin();
    File testDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName());
    File testDir = createTempDir(LuceneTestCase.getTestClass().getSimpleName()).toFile();
 
     File newCoreInstanceDir = new File(testDir, "newcore");
 
@@ -99,9 +99,9 @@ public class TestCoreAdmin extends AbstractEmbeddedSolrServerTestCase {
     
     SolrServer server = getSolrAdmin();
     
    File dataDir = createTempDir("data");
    File dataDir = createTempDir("data").toFile();
     
    File newCoreInstanceDir = createTempDir("instance");
    File newCoreInstanceDir = createTempDir("instance").toFile();
     
     File instanceDir = new File(cores.getSolrHome());
     FileUtils.copyDirectory(instanceDir, new File(newCoreInstanceDir,
diff --git a/solr/solrj/src/test/org/apache/solr/common/util/ContentStreamTest.java b/solr/solrj/src/test/org/apache/solr/common/util/ContentStreamTest.java
index 618e6c9abac..537d60a9414 100644
-- a/solr/solrj/src/test/org/apache/solr/common/util/ContentStreamTest.java
++ b/solr/solrj/src/test/org/apache/solr/common/util/ContentStreamTest.java
@@ -49,7 +49,7 @@ public class ContentStreamTest extends SolrTestCaseJ4
   {
     InputStream is = new SolrResourceLoader(null, null).openResource( "solrj/README" );
     assertNotNull( is );
    File file = new File(createTempDir(), "README");
    File file = new File(createTempDir().toFile(), "README");
     FileOutputStream os = new FileOutputStream(file);
     IOUtils.copy(is, os);
     os.close();
@@ -78,7 +78,7 @@ public class ContentStreamTest extends SolrTestCaseJ4
   {
     InputStream is = new SolrResourceLoader(null, null).openResource( "solrj/README" );
     assertNotNull( is );
    File file = new File(createTempDir(), "README");
    File file = new File(createTempDir().toFile(), "README");
     FileOutputStream os = new FileOutputStream(file);
     IOUtils.copy(is, os);
     os.close();
diff --git a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
index 1826ac66fa0..13a96fe76a8 100644
-- a/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
++ b/solr/test-framework/src/java/org/apache/solr/BaseDistributedSearchTestCase.java
@@ -276,7 +276,7 @@ public abstract class BaseDistributedSearchTestCase extends SolrTestCaseJ4 {
     super.setUp();
     System.setProperty("solr.test.sys.prop1", "propone");
     System.setProperty("solr.test.sys.prop2", "proptwo");
    testDir = createTempDir();
    testDir = createTempDir().toFile();
   }
 
   @Override
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java b/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
index fb3f7e747a5..aad3b268743 100644
-- a/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrJettyTestBase.java
@@ -61,7 +61,7 @@ abstract public class SolrJettyTestBase extends SolrTestCaseJ4
 
     // this sets the property for jetty starting SolrDispatchFilter
     if (System.getProperty("solr.data.dir") == null && System.getProperty("solr.hdfs.home") == null) {
      jetty.setDataDir(createTempDir().getCanonicalPath());
      jetty.setDataDir(createTempDir().toFile().getCanonicalPath());
     }
     
     jetty.start();
diff --git a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
index 33d4644fbf4..dd6734f020d 100644
-- a/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
++ b/solr/test-framework/src/java/org/apache/solr/SolrTestCaseJ4.java
@@ -91,6 +91,7 @@ import java.lang.annotation.Retention;
 import java.lang.annotation.RetentionPolicy;
 import java.lang.annotation.Target;
 import java.net.URL;
import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
@@ -153,7 +154,7 @@ public abstract class SolrTestCaseJ4 extends LuceneTestCase {
   @BeforeClass 
   @SuppressWarnings("unused")
   private static void beforeClass() {
    initCoreDataDir = createTempDir("init-core-data");
    initCoreDataDir = createTempDir("init-core-data").toFile();
 
     System.err.println("Creating dataDir: " + initCoreDataDir.getAbsolutePath());
 
@@ -1038,12 +1039,12 @@ public abstract class SolrTestCaseJ4 extends LuceneTestCase {
   }
 
   /**
   * @see IOUtils#rm(File...)
   * @see IOUtils#rm(Path...)
    */
   @Deprecated()
   public static boolean recurseDelete(File f) {
     try {
      IOUtils.rm(f);
      IOUtils.rm(f.toPath());
       return true;
     } catch (IOException e) {
       System.err.println(e.toString());
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
index 971841a33dd..fb5e59cb74a 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractFullDistribZkTestBase.java
@@ -272,7 +272,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
     
     try {
       
      File controlJettyDir = createTempDir();
      File controlJettyDir = createTempDir().toFile();
       setupJettySolrHome(controlJettyDir);
       
       controlJetty = createJetty(controlJettyDir, useJettyDataDir ? getDataDir(testDir
@@ -384,7 +384,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
       if (sb.length() > 0) sb.append(',');
       int cnt = this.jettyIntCntr.incrementAndGet();
 
      File jettyDir = createTempDir();
      File jettyDir = createTempDir().toFile();
 
       jettyDir.mkdirs();
       setupJettySolrHome(jettyDir);
@@ -449,7 +449,7 @@ public abstract class AbstractFullDistribZkTestBase extends AbstractDistribZkTes
 
     int cnt = this.jettyIntCntr.incrementAndGet();
 
      File jettyDir = createTempDir("jetty");
      File jettyDir = createTempDir("jetty").toFile();
       jettyDir.mkdirs();
       org.apache.commons.io.FileUtils.copyDirectory(new File(getSolrHome()), jettyDir);
       JettySolrRunner j = createJetty(jettyDir, testDir + "/jetty" + cnt, shard, "solrconfig.xml", null);
diff --git a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractZkTestCase.java b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractZkTestCase.java
index 537ad6b096e..2535e70ff1d 100644
-- a/solr/test-framework/src/java/org/apache/solr/cloud/AbstractZkTestCase.java
++ b/solr/test-framework/src/java/org/apache/solr/cloud/AbstractZkTestCase.java
@@ -63,7 +63,7 @@ public abstract class AbstractZkTestCase extends SolrTestCaseJ4 {
 
   @BeforeClass
   public static void azt_beforeClass() throws Exception {
    zkDir = createTempDir("zkData").getAbsolutePath();
    zkDir = createTempDir("zkData").toFile().getAbsolutePath();
     zkServer = new ZkTestServer(zkDir);
     zkServer.run();
     
diff --git a/solr/test-framework/src/java/org/apache/solr/core/MockFSDirectoryFactory.java b/solr/test-framework/src/java/org/apache/solr/core/MockFSDirectoryFactory.java
index 7dea27f5b97..b6b61e6b16a 100644
-- a/solr/test-framework/src/java/org/apache/solr/core/MockFSDirectoryFactory.java
++ b/solr/test-framework/src/java/org/apache/solr/core/MockFSDirectoryFactory.java
@@ -19,6 +19,7 @@ package org.apache.solr.core;
 
 import java.io.File;
 import java.io.IOException;
import java.nio.file.Path;
 
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.MockDirectoryWrapper;
@@ -28,13 +29,13 @@ import org.apache.lucene.store.TrackingDirectoryWrapper;
 import org.apache.lucene.util.LuceneTestCase;
 
 /**
 * Opens a directory with {@link LuceneTestCase#newFSDirectory(File)}
 * Opens a directory with {@link LuceneTestCase#newFSDirectory(Path)}
  */
 public class MockFSDirectoryFactory extends StandardDirectoryFactory {
 
   @Override
   public Directory create(String path, DirContext dirContext) throws IOException {
    Directory dir = LuceneTestCase.newFSDirectory(new File(path));
    Directory dir = LuceneTestCase.newFSDirectory(new File(path).toPath());
     // we can't currently do this check because of how
     // Solr has to reboot a new Directory sometimes when replicating
     // or rolling back - the old directory is closed and the following
- 
2.19.1.windows.1

