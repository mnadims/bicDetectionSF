From 602a1f50e724a19311c169e16f1807d950399e8c Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 18 Dec 2015 18:07:17 +0000
Subject: [PATCH] SOLR-7865: BlendedInfixSuggester was returning more results
 than requested

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1720832 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  3 +
 .../analyzing/BlendedInfixSuggester.java      | 14 ++--
 .../analyzing/BlendedInfixSuggesterTest.java  | 69 +++++++++++++++++++
 .../suggest/TestBlendedInfixSuggestions.java  | 20 ++++++
 4 files changed, 99 insertions(+), 7 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index f604916a77d..efabd2884b3 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -159,6 +159,9 @@ Bug Fixes
 * LUCENE-6929: Fix SpanNotQuery rewriting to not drop the pre/post parameters.
   (Tim Allison via Adrien Grand)
 
* SOLR-7865: BlendedInfixSuggester was returning too many results
  (Arcadius Ahouansou via Mike McCandless)

 Other
 
 * LUCENE-6924: Upgrade randomizedtesting to 2.3.2. (Dawid Weiss)
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
index 9f4a997d80e..1787f12d548 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
@@ -141,25 +141,25 @@ public class BlendedInfixSuggester extends AnalyzingInfixSuggester {
   
   @Override
   public List<Lookup.LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, boolean onlyMorePopular, int num) throws IOException {
    // here we multiply the number of searched element by the defined factor
    return super.lookup(key, contexts, onlyMorePopular, num * numFactor);
    // Don't * numFactor here since we do it down below, once, in the call chain:
    return super.lookup(key, contexts, onlyMorePopular, num);
   }
 
   @Override
   public List<Lookup.LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
    // here we multiply the number of searched element by the defined factor
    return super.lookup(key, contexts, num * numFactor, allTermsRequired, doHighlight);
    // Don't * numFactor here since we do it down below, once, in the call chain:
    return super.lookup(key, contexts, num, allTermsRequired, doHighlight);
   }
 
   @Override
   public List<Lookup.LookupResult> lookup(CharSequence key, Map<BytesRef, BooleanClause.Occur> contextInfo, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
    // here we multiply the number of searched element by the defined factor
    return super.lookup(key, contextInfo, num * numFactor, allTermsRequired, doHighlight);
    // Don't * numFactor here since we do it down below, once, in the call chain:
    return super.lookup(key, contextInfo, num, allTermsRequired, doHighlight);
   }
 
   @Override
   public List<Lookup.LookupResult> lookup(CharSequence key, BooleanQuery contextQuery, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
    // here we multiply the number of searched element by the defined factor
    /** We need to do num * numFactor here only because it is the last call in the lookup chain*/
     return super.lookup(key, contextQuery, num * numFactor, allTermsRequired, doHighlight);
   }
   
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
index 87a77a3d0fa..eb2b722ceaf 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggesterTest.java
@@ -20,12 +20,15 @@ package org.apache.lucene.search.suggest.analyzing;
 import java.io.IOException;
 import java.nio.file.Path;
 import java.util.List;
import java.util.Map;
import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.suggest.Input;
 import org.apache.lucene.search.suggest.InputArrayIterator;
 import org.apache.lucene.search.suggest.Lookup;
@@ -255,6 +258,72 @@ public class BlendedInfixSuggesterTest extends LuceneTestCase {
 
   }
 

  public void testSuggesterCountForAllLookups() throws IOException {


    Input keys[] = new Input[]{
        new Input("lend me your ears", 1),
        new Input("as you sow so shall you reap", 1),
    };

    Path tempDir = createTempDir("BlendedInfixSuggesterTest");
    Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);

    // BlenderType.LINEAR is used by default (remove position*10%)
    BlendedInfixSuggester suggester = new BlendedInfixSuggester(newFSDirectory(tempDir), a);
    suggester.build(new InputArrayIterator(keys));


    String term = "you";

    List<Lookup.LookupResult> responses = suggester.lookup(term, false, 1);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, false, 2);
    assertEquals(2, responses.size());


    responses = suggester.lookup(term, 1, false, false);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, 2, false, false);
    assertEquals(2, responses.size());


    responses = suggester.lookup(term, (Map) null, 1, false, false);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, (Map) null, 2, false, false);
    assertEquals(2, responses.size());


    responses = suggester.lookup(term, (Set) null, 1, false, false);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, (Set) null, 2, false, false);
    assertEquals(2, responses.size());


    responses = suggester.lookup(term, null, false, 1);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, null, false, 2);
    assertEquals(2, responses.size());


    responses = suggester.lookup(term, (BooleanQuery) null, 1, false, false);
    assertEquals(1, responses.size());

    responses = suggester.lookup(term, (BooleanQuery) null, 2, false, false);
    assertEquals(2, responses.size());


    suggester.close();

  }


   public void /*testT*/rying() throws IOException {
 
     BytesRef lake = new BytesRef("lake");
diff --git a/solr/core/src/test/org/apache/solr/spelling/suggest/TestBlendedInfixSuggestions.java b/solr/core/src/test/org/apache/solr/spelling/suggest/TestBlendedInfixSuggestions.java
index 31620b6931b..b39da4d7056 100644
-- a/solr/core/src/test/org/apache/solr/spelling/suggest/TestBlendedInfixSuggestions.java
++ b/solr/core/src/test/org/apache/solr/spelling/suggest/TestBlendedInfixSuggestions.java
@@ -83,4 +83,24 @@ public class TestBlendedInfixSuggestions extends SolrTestCaseJ4 {
         "//lst[@name='suggest']/lst[@name='blended_infix_suggest_reciprocal']/lst[@name='the']/arr[@name='suggestions']/lst[3]/str[@name='payload'][.='star']"
     );
   }

  public void testSuggestCount() {

    assertQ(req("qt", URI, "q", "the", SuggesterParams.SUGGEST_COUNT, "1", SuggesterParams.SUGGEST_DICT, "blended_infix_suggest_reciprocal"),
        "//lst[@name='suggest']/lst[@name='blended_infix_suggest_reciprocal']/lst[@name='the']/int[@name='numFound'][.='1']"
    );

    assertQ(req("qt", URI, "q", "the", SuggesterParams.SUGGEST_COUNT, "2", SuggesterParams.SUGGEST_DICT, "blended_infix_suggest_reciprocal"),
        "//lst[@name='suggest']/lst[@name='blended_infix_suggest_reciprocal']/lst[@name='the']/int[@name='numFound'][.='2']"
    );

    assertQ(req("qt", URI, "q", "the", SuggesterParams.SUGGEST_COUNT, "3", SuggesterParams.SUGGEST_DICT, "blended_infix_suggest_reciprocal"),
        "//lst[@name='suggest']/lst[@name='blended_infix_suggest_reciprocal']/lst[@name='the']/int[@name='numFound'][.='3']"
    );

    assertQ(req("qt", URI, "q", "the", SuggesterParams.SUGGEST_COUNT, "20", SuggesterParams.SUGGEST_DICT, "blended_infix_suggest_reciprocal"),
        "//lst[@name='suggest']/lst[@name='blended_infix_suggest_reciprocal']/lst[@name='the']/int[@name='numFound'][.='3']"
    );
  }

 }
- 
2.19.1.windows.1

