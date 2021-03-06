From 4ac6ce3190d350e651c048fdb8223a9619b33ecf Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Wed, 30 Jul 2008 08:11:56 +0000
Subject: [PATCH] SOLR-663 -- Allow multiple files for stopwords, keepwords,
 protwords and synonyms

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@680935 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  2 ++
 .../analysis/EnglishPorterFilterFactory.java  | 24 +++++++++++++----
 .../solr/analysis/KeepWordFilterFactory.java  | 27 ++++++++++++++-----
 .../solr/analysis/StopFilterFactory.java      | 23 +++++++++++++---
 .../solr/analysis/SynonymFilterFactory.java   | 12 ++++++++-
 .../org/apache/solr/common/util/StrUtils.java | 20 ++++++++++++++
 src/test/org/apache/solr/util/TestUtils.java  |  5 ++++
 7 files changed, 96 insertions(+), 17 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index ed8f695356d..5547561f9b2 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -329,6 +329,8 @@ New Features
 
 64. SOLR-666: Expose warmup time in statistics for SolrIndexSearcher and LRUCache (shalin)
 
65. SOLR-663: Allow multiple files for stopwords, keepwords, protwords and synonyms (shalin)

     
 Changes in runtime behavior
  1. SOLR-559: use Lucene updateDocument, deleteDocuments methods.  This
diff --git a/src/java/org/apache/solr/analysis/EnglishPorterFilterFactory.java b/src/java/org/apache/solr/analysis/EnglishPorterFilterFactory.java
index c61b56c289a..ee9e9c98a4b 100644
-- a/src/java/org/apache/solr/analysis/EnglishPorterFilterFactory.java
++ b/src/java/org/apache/solr/analysis/EnglishPorterFilterFactory.java
@@ -22,9 +22,11 @@ import org.apache.lucene.analysis.Token;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.util.plugin.ResourceLoaderAware;
 
 import java.io.IOException;
import java.io.File;
 import java.util.List;
 
 /**
@@ -34,12 +36,24 @@ public class EnglishPorterFilterFactory extends BaseTokenFilterFactory implement
   public static final String PROTECTED_TOKENS = "protected";
 
   public void inform(ResourceLoader loader) {
    String wordFile = args.get(PROTECTED_TOKENS);
    if (wordFile != null) {
    String wordFiles = args.get(PROTECTED_TOKENS);
    if (wordFiles != null) {
       try {
        List<String> wlist = loader.getLines(wordFile);
        //This cast is safe in Lucene
        protectedWords = new CharArraySet(wlist, false);//No need to go through StopFilter as before, since it just uses a List internally
        File protectedWordFiles = new File(wordFiles);
        if (protectedWordFiles.exists()) {
          List<String> wlist = loader.getLines(wordFiles);
          //This cast is safe in Lucene
          protectedWords = new CharArraySet(wlist, false);//No need to go through StopFilter as before, since it just uses a List internally
        } else  {
          List<String> files = StrUtils.splitFileNames(wordFiles);
          for (String file : files) {
            List<String> wlist = loader.getLines(file.trim());
            if (protectedWords == null)
              protectedWords = new CharArraySet(wlist, false);
            else
              protectedWords.addAll(wlist);
          }
        }
       } catch (IOException e) {
         throw new RuntimeException(e);
       }
diff --git a/src/java/org/apache/solr/analysis/KeepWordFilterFactory.java b/src/java/org/apache/solr/analysis/KeepWordFilterFactory.java
index 4ff9dd14064..101df6394ef 100644
-- a/src/java/org/apache/solr/analysis/KeepWordFilterFactory.java
++ b/src/java/org/apache/solr/analysis/KeepWordFilterFactory.java
@@ -18,14 +18,16 @@
 package org.apache.solr.analysis;
 
 import org.apache.solr.common.ResourceLoader;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.util.plugin.ResourceLoaderAware;
 import org.apache.lucene.analysis.StopFilter;
 import org.apache.lucene.analysis.TokenStream;
 
import java.util.Map;
import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
import java.io.File;
import java.io.File;
 import java.io.IOException;
 
 /**
@@ -39,14 +41,25 @@ public class KeepWordFilterFactory extends BaseTokenFilterFactory implements Res
 
   @SuppressWarnings("unchecked")
   public void inform(ResourceLoader loader) {
    String wordFile = args.get("words");
    String wordFiles = args.get("words");
     ignoreCase = getBoolean("ignoreCase",false);
 
    if (wordFile != null) {
    if (wordFiles != null) {
      if (words == null)
        words = new HashSet<String>();
       try {
        List<String> wlist = loader.getLines(wordFile);
        words = StopFilter.makeStopSet(
            (String[])wlist.toArray(new String[0]), ignoreCase);
        java.io.File keepWordsFile = new File(wordFiles);
        if (keepWordsFile.exists()) {
          List<String> wlist = loader.getLines(wordFiles);
          words = StopFilter.makeStopSet(
              (String[])wlist.toArray(new String[0]), ignoreCase);
        } else  {
          List<String> files = StrUtils.splitFileNames(wordFiles);
          for (String file : files) {
            List<String> wlist = loader.getLines(file.trim());
            words.addAll(StopFilter.makeStopSet((String[])wlist.toArray(new String[0]), ignoreCase));
          }
        }
       } 
       catch (IOException e) {
         throw new RuntimeException(e);
diff --git a/src/java/org/apache/solr/analysis/StopFilterFactory.java b/src/java/org/apache/solr/analysis/StopFilterFactory.java
index 3178306b6b8..0126952a7bc 100644
-- a/src/java/org/apache/solr/analysis/StopFilterFactory.java
++ b/src/java/org/apache/solr/analysis/StopFilterFactory.java
@@ -18,13 +18,17 @@
 package org.apache.solr.analysis;
 
 import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.util.plugin.ResourceLoaderAware;
 import org.apache.lucene.analysis.StopFilter;
 import org.apache.lucene.analysis.StopAnalyzer;
 import org.apache.lucene.analysis.TokenStream;
 
import java.util.HashSet;
 import java.util.List;
import java.io.File;
 import java.util.Set;
import java.io.File;
 import java.io.IOException;
 
 /**
@@ -33,14 +37,25 @@ import java.io.IOException;
 public class StopFilterFactory extends BaseTokenFilterFactory implements ResourceLoaderAware {
 
   public void inform(ResourceLoader loader) {
    String stopWordFile = args.get("words");
    String stopWordFiles = args.get("words");
     ignoreCase = getBoolean("ignoreCase",false);
     enablePositionIncrements = getBoolean("enablePositionIncrements",false);
 
    if (stopWordFile != null) {
    if (stopWordFiles != null) {
      if (stopWords == null)
        stopWords = new HashSet<String>();
       try {
        List<String> wlist = loader.getLines(stopWordFile);
        stopWords = StopFilter.makeStopSet((String[])wlist.toArray(new String[0]), ignoreCase);
        java.io.File keepWordsFile = new File(stopWordFiles);
        if (keepWordsFile.exists()) {
          List<String> wlist = loader.getLines(stopWordFiles);
          stopWords = StopFilter.makeStopSet((String[])wlist.toArray(new String[0]), ignoreCase);
        } else  {
          List<String> files = StrUtils.splitFileNames(stopWordFiles);
          for (String file : files) {
            List<String> wlist = loader.getLines(file.trim());
            stopWords.addAll(StopFilter.makeStopSet((String[])wlist.toArray(new String[0]), ignoreCase));
          }
        }
       } catch (IOException e) {
         throw new RuntimeException(e);
       }
diff --git a/src/java/org/apache/solr/analysis/SynonymFilterFactory.java b/src/java/org/apache/solr/analysis/SynonymFilterFactory.java
index 16dfd125a19..fca4d5a6537 100644
-- a/src/java/org/apache/solr/analysis/SynonymFilterFactory.java
++ b/src/java/org/apache/solr/analysis/SynonymFilterFactory.java
@@ -24,9 +24,11 @@ import org.apache.solr.common.util.StrUtils;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.util.plugin.ResourceLoaderAware;
 
import java.io.File;
 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
@@ -51,7 +53,15 @@ public class SynonymFilterFactory extends BaseTokenFilterFactory implements Reso
     if (synonyms != null) {
       List<String> wlist=null;
       try {
        wlist = loader.getLines(synonyms);
        File synonymFile = new java.io.File(synonyms);
        if (synonymFile.exists()) {
          wlist = loader.getLines(synonyms);
        } else  {
          List<String> files = StrUtils.splitFileNames(synonyms);
          for (String file : files) {
            wlist = loader.getLines(file.trim());
          }
        }
       } catch (IOException e) {
         throw new RuntimeException(e);
       }
diff --git a/src/java/org/apache/solr/common/util/StrUtils.java b/src/java/org/apache/solr/common/util/StrUtils.java
index a132780ddf6..28bbff1a926 100644
-- a/src/java/org/apache/solr/common/util/StrUtils.java
++ b/src/java/org/apache/solr/common/util/StrUtils.java
@@ -19,6 +19,7 @@ package org.apache.solr.common.util;
 
 import java.util.List;
 import java.util.ArrayList;
import java.util.Collections;
 import java.io.IOException;
 
 /**
@@ -118,6 +119,25 @@ public class StrUtils {
     return lst;
   }
 
  /**
   * Splits file names separated by comma character.
   * File names can contain comma characters escaped by backslash '\'
   *
   * @param fileNames the string containing file names
   * @return a list of file names with the escaping backslashed removed
   */
  public static List<String> splitFileNames(String fileNames) {
    if (fileNames == null)
      return Collections.<String>emptyList();

    List<String> result = new ArrayList<String>();
    for (String file : fileNames.split("(?<!\\\\),")) {
      result.add(file.replaceAll("\\\\(?=,)", ""));
    }

    return result;
  }

   /** Creates a backslash escaped string, joining all the items. */
   public static String join(List<String> items, char separator) {
     StringBuilder sb = new StringBuilder(items.size() << 3);
diff --git a/src/test/org/apache/solr/util/TestUtils.java b/src/test/org/apache/solr/util/TestUtils.java
index d48094aeeaa..b0f7710548a 100755
-- a/src/test/org/apache/solr/util/TestUtils.java
++ b/src/test/org/apache/solr/util/TestUtils.java
@@ -62,6 +62,11 @@ public class TestUtils extends TestCase {
     assertEquals(2,arr.size());
     assertEquals(" foo ",arr.get(0));
     assertEquals(" bar ",arr.get(1));
    
    arr = StrUtils.splitFileNames("/h/s,/h/\\,s,");
    assertEquals(2,arr.size());
    assertEquals("/h/s",arr.get(0));
    assertEquals("/h/,s",arr.get(1));
   }
 
   public void testNamedLists()
- 
2.19.1.windows.1

