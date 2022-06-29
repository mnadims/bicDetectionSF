From 840dba269266e4895a12ca2074d36e356076e656 Mon Sep 17 00:00:00 2001
From: Eric Newton <eric.newton@gmail.com>
Date: Tue, 25 Feb 2014 12:17:46 -0500
Subject: [PATCH] ACCUMULO-2401 finish removing hacky site-configuration
 manipulation

--
 .../accumulo/core/file/rfile/RFileTest.java   | 241 ++++++------------
 .../user/IndexedDocIteratorTest.java          |   3 +-
 core/src/test/resources/log4j.properties      |   2 +-
 3 files changed, 85 insertions(+), 161 deletions(-)

diff --git a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
index fb9658ce0..ccbefb2e8 100644
-- a/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
++ b/core/src/test/java/org/apache/accumulo/core/file/rfile/RFileTest.java
@@ -33,12 +33,13 @@ import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
import java.util.Map.Entry;
 import java.util.Random;
 import java.util.Set;
 
 import org.apache.accumulo.core.Constants;
 import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
 import org.apache.accumulo.core.data.ArrayByteSequence;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
@@ -171,14 +172,21 @@ public class RFileTest {
     private FSDataOutputStream dos;
     private SeekableByteArrayInputStream bais;
     private FSDataInputStream in;
    private AccumuloConfiguration accumuloConfiguration;
     public Reader reader;
     public SortedKeyValueIterator<Key,Value> iter;
    
    public TestRFile(AccumuloConfiguration accumuloConfiguration) {
      this.accumuloConfiguration = accumuloConfiguration;
      if (this.accumuloConfiguration == null)
        this.accumuloConfiguration = AccumuloConfiguration.getDefaultConfiguration();
    }
 
     public void openWriter(boolean startDLG) throws IOException {
 
       baos = new ByteArrayOutputStream();
       dos = new FSDataOutputStream(baos, new FileSystem.Statistics("a"));
      CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", conf, AccumuloConfiguration.getDefaultConfiguration());
      CachableBlockFile.Writer _cbw = new CachableBlockFile.Writer(dos, "gz", conf, accumuloConfiguration);
       writer = new RFile.Writer(_cbw, 1000, 1000);
 
       if (startDLG)
@@ -239,13 +247,15 @@ public class RFileTest {
   static String nf(String prefix, int i) {
     return String.format(prefix + "%06d", i);
   }
  
  public AccumuloConfiguration conf = null;
 
   @Test
   public void test1() throws IOException {
 
     // test an empty file
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
     trf.closeWriter();
@@ -264,7 +274,7 @@ public class RFileTest {
 
     // test an rfile with one entry
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
     trf.writer.append(nk("r1", "cf1", "cq1", "L1", 55), nv("foo"));
@@ -301,7 +311,7 @@ public class RFileTest {
 
     // test an rfile with multiple rows having multiple columns
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -456,7 +466,7 @@ public class RFileTest {
 
   @Test
   public void test4() throws IOException {
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -500,7 +510,7 @@ public class RFileTest {
   @Test
   public void test5() throws IOException {
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
     trf.writer.append(nk("r1", "cf1", "cq1", "L1", 55), nv("foo1"));
@@ -529,7 +539,7 @@ public class RFileTest {
   @Test
   public void test6() throws IOException {
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
     for (int i = 0; i < 500; i++) {
@@ -563,7 +573,7 @@ public class RFileTest {
   public void test7() throws IOException {
     // these tests exercise setting the end key of a range
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
     for (int i = 2; i < 50; i++) {
@@ -613,7 +623,7 @@ public class RFileTest {
 
   @Test
   public void test8() throws IOException {
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -648,7 +658,7 @@ public class RFileTest {
     trf.closeReader();
 
     // do same test with col fam
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -683,7 +693,7 @@ public class RFileTest {
     trf.closeReader();
 
     // do same test with col qual
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -730,7 +740,7 @@ public class RFileTest {
 
   @Test
   public void test9() throws IOException {
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -874,7 +884,7 @@ public class RFileTest {
   public void test10() throws IOException {
 
     // test empty locality groups
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("cf1", "cf2"));
@@ -889,7 +899,7 @@ public class RFileTest {
     trf.closeReader();
 
     // another empty locality group test
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("cf1", "cf2"));
@@ -914,7 +924,7 @@ public class RFileTest {
     trf.closeReader();
 
     // another empty locality group test
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("cf1", "cf2"));
@@ -939,7 +949,7 @@ public class RFileTest {
     trf.closeReader();
 
     // another empty locality group test
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("cf1", "cf2"));
@@ -964,7 +974,7 @@ public class RFileTest {
     trf.closeReader();
 
     // another empty locality group test
    trf = new TestRFile();
    trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("cf1", "cf2"));
@@ -1003,7 +1013,7 @@ public class RFileTest {
   public void test11() throws IOException {
     // test locality groups with more than two entries
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
     trf.writer.startNewLocalityGroup("lg1", ncfs("3mod10"));
@@ -1108,7 +1118,7 @@ public class RFileTest {
   public void test12() throws IOException {
     // test inserting column fams not in locality groups
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1141,7 +1151,7 @@ public class RFileTest {
     // test inserting column fam in default loc group that was in
     // previous locality group
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1182,7 +1192,7 @@ public class RFileTest {
   public void test14() throws IOException {
     // test starting locality group after default locality group was started
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1206,7 +1216,7 @@ public class RFileTest {
 
   @Test
   public void test16() throws IOException {
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1229,7 +1239,7 @@ public class RFileTest {
     // this should cause the keys in the index to be exactly the same...
     // ensure seeks work correctly
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1366,7 +1376,7 @@ public class RFileTest {
   public void test18() throws IOException {
     // test writing more column families to default LG than it will track
 
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1417,7 +1427,7 @@ public class RFileTest {
   @Test
   public void test19() throws IOException {
     // test RFile metastore
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter(false);
 
@@ -1469,7 +1479,7 @@ public class RFileTest {
 
   @Test
   public void testReseekUnconsumed() throws Exception {
    TestRFile trf = new TestRFile();
    TestRFile trf = new TestRFile(conf);
 
     trf.openWriter();
 
@@ -1546,8 +1556,7 @@ public class RFileTest {
     byte data[] = baos.toByteArray();
     SeekableByteArrayInputStream bais = new SeekableByteArrayInputStream(data);
     FSDataInputStream in2 = new FSDataInputStream(bais);
    @SuppressWarnings("deprecation")
    AccumuloConfiguration aconf = AccumuloConfiguration.getSiteConfiguration();
    AccumuloConfiguration aconf = AccumuloConfiguration.getDefaultConfiguration();
     CachableBlockFile.Reader _cbr = new CachableBlockFile.Reader(in2, data.length, CachedConfiguration.getInstance(), aconf);
     Reader reader = new RFile.Reader(_cbr);
     checkIndex(reader);
@@ -1592,210 +1601,141 @@ public class RFileTest {
   }
 
   private AccumuloConfiguration setAndGetAccumuloConfig(String cryptoConfSetting) {
    @SuppressWarnings("deprecation")
    AccumuloConfiguration conf = AccumuloConfiguration.getSiteConfiguration();
    System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, cryptoConfSetting);
    ((SiteConfiguration) conf).clearAndNull();
    return conf;
  }

  private void restoreOldConfiguration(String oldSiteConfigProperty, AccumuloConfiguration conf) {
    if (oldSiteConfigProperty != null) {
      System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, oldSiteConfigProperty);
    } else {
      System.clearProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    ConfigurationCopy result = new ConfigurationCopy(AccumuloConfiguration.getDefaultConfiguration());
    Configuration conf = new Configuration(false);
    conf.addResource(cryptoConfSetting);
    for (Entry<String,String> e : conf) {
      result.set(e.getKey(), e.getValue());
     }
    ((SiteConfiguration) conf).clearAndNull();
    return result;
   }
 
   @Test
   public void testEncRFile1() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test1();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile2() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test2();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile3() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test3();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile4() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test4();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile5() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test5();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile6() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test6();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile7() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test7();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile8() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test8();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile9() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test9();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile10() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test10();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile11() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test11();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile12() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

     test12();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile13() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test13();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile14() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test14();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile16() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test16();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
   }
 
   @Test
   public void testEncRFile17() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test17();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
   }
 
   @Test
   public void testEncRFile18() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test18();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncRFile19() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    AccumuloConfiguration conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test19();

    restoreOldConfiguration(oldSiteConfigProperty, conf);
    conf = null;
   }
 
   @Test
   public void testEncryptedRFiles() throws Exception {
    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    @SuppressWarnings("deprecation")
    AccumuloConfiguration conf = AccumuloConfiguration.getSiteConfiguration();
    System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, CryptoTest.CRYPTO_ON_CONF);
    ((SiteConfiguration) conf).clearAndNull();

    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
     test1();
     test2();
     test3();
@@ -1804,25 +1744,14 @@ public class RFileTest {
     test6();
     test7();
     test8();

    if (oldSiteConfigProperty != null) {
      System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, oldSiteConfigProperty);
    } else {
      System.clearProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    }
    ((SiteConfiguration) conf).clearAndNull();
    conf = null;
   }
 
   @Test
   public void testRootTabletEncryption() throws Exception {
 
     // This tests that the normal set of operations used to populate a root tablet

    String oldSiteConfigProperty = System.getProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    @SuppressWarnings("deprecation")
    AccumuloConfiguration conf = AccumuloConfiguration.getSiteConfiguration();
    System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, CryptoTest.CRYPTO_ON_CONF);
    ((SiteConfiguration) conf).clearAndNull();
    conf = setAndGetAccumuloConfig(CryptoTest.CRYPTO_ON_CONF);
 
     // populate the root tablet with info about the default tablet
     // the root tablet contains the key extent and locations of all the
@@ -1831,7 +1760,7 @@ public class RFileTest {
     // + FileOperations.getNewFileExtension(AccumuloConfiguration.getDefaultConfiguration());
     // FileSKVWriter mfw = FileOperations.getInstance().openWriter(initRootTabFile, fs, conf, AccumuloConfiguration.getDefaultConfiguration());
 
    TestRFile testRfile = new TestRFile();
    TestRFile testRfile = new TestRFile(conf);
     testRfile.openWriter();
 
     RFile.Writer mfw = testRfile.writer;
@@ -1892,12 +1821,6 @@ public class RFileTest {
 
     testRfile.closeReader();
 
    if (oldSiteConfigProperty != null) {
      System.setProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP, oldSiteConfigProperty);
    } else {
      System.clearProperty(CryptoTest.CONFIG_FILE_SYSTEM_PROP);
    }
    ((SiteConfiguration) conf).clearAndNull();

    conf = null;
   }
 }
diff --git a/core/src/test/java/org/apache/accumulo/core/iterators/user/IndexedDocIteratorTest.java b/core/src/test/java/org/apache/accumulo/core/iterators/user/IndexedDocIteratorTest.java
index 7a82249d5..ac0ab6ba4 100644
-- a/core/src/test/java/org/apache/accumulo/core/iterators/user/IndexedDocIteratorTest.java
++ b/core/src/test/java/org/apache/accumulo/core/iterators/user/IndexedDocIteratorTest.java
@@ -27,6 +27,7 @@ import java.util.TreeMap;
 import junit.framework.TestCase;
 
 import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
 import org.apache.accumulo.core.data.ByteSequence;
 import org.apache.accumulo.core.data.Key;
 import org.apache.accumulo.core.data.Range;
@@ -131,7 +132,7 @@ public class IndexedDocIteratorTest extends TestCase {
     return map;
   }
   
  static TestRFile trf = new TestRFile();
  static TestRFile trf = new TestRFile(AccumuloConfiguration.getDefaultConfiguration());
   
   private SortedKeyValueIterator<Key,Value> createIteratorStack(float hitRatio, int numRows, int numDocsPerRow, Text[] columnFamilies,
       Text[] otherColumnFamilies, HashSet<Text> docs) throws IOException {
diff --git a/core/src/test/resources/log4j.properties b/core/src/test/resources/log4j.properties
index cf5c2f0e4..dfc93bf2f 100644
-- a/core/src/test/resources/log4j.properties
++ b/core/src/test/resources/log4j.properties
@@ -16,7 +16,7 @@
 log4j.rootLogger=INFO, CA
 log4j.appender.CA=org.apache.log4j.ConsoleAppender
 log4j.appender.CA.layout=org.apache.log4j.PatternLayout
log4j.appender.CA.layout.ConversionPattern=[%t} %-5p %c %x - %m%n
log4j.appender.CA.layout.ConversionPattern=[%t] %-5p %c %x - %m%n
 
 log4j.logger.org.apache.accumulo.core.iterators.system.VisibilityFilter=FATAL
 log4j.logger.org.apache.accumulo.core.iterators.user.TransformingIteratorTest$IllegalVisCompactionKeyTransformingIterator=FATAL
- 
2.19.1.windows.1

