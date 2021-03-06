From fdeb3232439060d4f06a43c5b3239db540eed9a9 Mon Sep 17 00:00:00 2001
From: Michael McCandless <mikemccand@apache.org>
Date: Fri, 14 Nov 2014 22:04:05 +0000
Subject: [PATCH] LUCENE-6004: don't highlight LookupResult.key from
 AnalyzingInfixSuggester

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1639798 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   3 +
 .../analyzing/AnalyzingInfixSuggester.java    |   9 +-
 .../analyzing/BlendedInfixSuggester.java      |   3 +-
 .../AnalyzingInfixSuggesterTest.java          | 145 +++++++++++-------
 .../fst/AnalyzingInfixLookupFactory.java      |  24 ++-
 .../fst/BlendedInfixLookupFactory.java        |  26 +++-
 6 files changed, 146 insertions(+), 64 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index cf6b09c15a7..bcd916bae82 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -216,6 +216,9 @@ Bug Fixes
   not have the regular "spinlock" of DirectoryReader.open. It now implements
   Closeable and you must close it to release the lock.  (Mike McCandless, Robert Muir)
 
* LUCENE-6004: Don't highlight the LookupResult.key returned from
  AnalyzingInfixSuggester (Christian Reuschling, jane chang via Mike McCandless)

 * LUCENE-5980: Don't let document length overflow. (Robert Muir)
 
 * LUCENE-5961: Fix the exists() method for FunctionValues returned by many ValueSoures to
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
index de59d7dcea7..351f9acfd4b 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggester.java
@@ -22,10 +22,10 @@ import java.io.IOException;
 import java.io.StringReader;
 import java.nio.file.Path;
 import java.util.ArrayList;
import java.util.HashSet;
 import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
 import java.util.List;
import java.util.Map;
 import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
@@ -72,8 +72,8 @@ import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TopFieldCollector;
 import org.apache.lucene.search.TopFieldDocs;
 import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup.LookupResult; // javadocs
 import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult; // javadocs
 import org.apache.lucene.store.DataInput;
 import org.apache.lucene.store.DataOutput;
 import org.apache.lucene.store.Directory;
@@ -603,8 +603,7 @@ public class AnalyzingInfixSuggester extends Lookup implements Closeable {
       LookupResult result;
 
       if (doHighlight) {
        Object highlightKey = highlight(text, matchedTokens, prefixToken);
        result = new LookupResult(highlightKey.toString(), highlightKey, score, payload, contexts);
        result = new LookupResult(text, highlight(text, matchedTokens, prefixToken), score, payload, contexts);
       } else {
         result = new LookupResult(text, score, payload, contexts);
       }
diff --git a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
index e1257515ed5..8df7dd77998 100644
-- a/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
++ b/lucene/suggest/src/java/org/apache/lucene/search/suggest/analyzing/BlendedInfixSuggester.java
@@ -207,8 +207,7 @@ public class BlendedInfixSuggester extends AnalyzingInfixSuggester {
 
       LookupResult result;
       if (doHighlight) {
        Object highlightKey = highlight(text, matchedTokens, prefixToken);
        result = new LookupResult(highlightKey.toString(), highlightKey, score, payload);
        result = new LookupResult(text, highlight(text, matchedTokens, prefixToken), score, payload);
       } else {
         result = new LookupResult(text, score, payload);
       }
diff --git a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
index 0d75e290920..71e4177f048 100644
-- a/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
++ b/lucene/suggest/src/test/org/apache/lucene/search/suggest/analyzing/AnalyzingInfixSuggesterTest.java
@@ -61,29 +61,34 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(2, results.size());
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
    assertEquals("lend me your <b>ear</b>", results.get(1).key);
    assertEquals("lend me your ear", results.get(1).key);
    assertEquals("lend me your <b>ear</b>", results.get(1).highlightKey);
     assertEquals(8, results.get(1).value);
     assertEquals(new BytesRef("foobar"), results.get(1).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("ear ", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("lend me your <b>ear</b>", results.get(0).key);
    assertEquals("lend me your ear", results.get(0).key);
    assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
     assertEquals(8, results.get(0).value);
     assertEquals(new BytesRef("foobar"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("pen", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("p", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
@@ -107,7 +112,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester = new AnalyzingInfixSuggester(newFSDirectory(tempDir), a, a, 3, false);
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(2, results.size());
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
     assertEquals(2, suggester.getCount());
@@ -228,16 +234,14 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
         List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, doHighlight);
         assertEquals(2, results.size());
        assertEquals("a penny saved is a penny earned", results.get(0).key);
         if (doHighlight) {
          assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).key);
        } else {
          assertEquals("a penny saved is a penny earned", results.get(0).key);
          assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).highlightKey);
         }
         assertEquals(10, results.get(0).value);
        assertEquals("lend me your ear", results.get(1).key);
         if (doHighlight) {
          assertEquals("lend me your <b>ear</b>", results.get(1).key);
        } else {
          assertEquals("lend me your ear", results.get(1).key);
          assertEquals("lend me your <b>ear</b>", results.get(1).highlightKey);
         }
         assertEquals(new BytesRef("foobaz"), results.get(0).payload);
         assertEquals(8, results.get(1).value);
@@ -245,30 +249,27 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
         results = suggester.lookup(TestUtil.stringToCharSequence("ear ", random()), 10, true, doHighlight);
         assertEquals(1, results.size());
        assertEquals("lend me your ear", results.get(0).key);
         if (doHighlight) {
          assertEquals("lend me your <b>ear</b>", results.get(0).key);
        } else {
          assertEquals("lend me your ear", results.get(0).key);
          assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
         }
         assertEquals(8, results.get(0).value);
         assertEquals(new BytesRef("foobar"), results.get(0).payload);
 
         results = suggester.lookup(TestUtil.stringToCharSequence("pen", random()), 10, true, doHighlight);
         assertEquals(1, results.size());
        assertEquals("a penny saved is a penny earned", results.get(0).key);
         if (doHighlight) {
          assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).key);
        } else {
          assertEquals("a penny saved is a penny earned", results.get(0).key);
          assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).highlightKey);
         }
         assertEquals(10, results.get(0).value);
         assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
         results = suggester.lookup(TestUtil.stringToCharSequence("p", random()), 10, true, doHighlight);
         assertEquals(1, results.size());
        assertEquals("a penny saved is a penny earned", results.get(0).key);
         if (doHighlight) {
          assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).key);
        } else {
          assertEquals("a penny saved is a penny earned", results.get(0).key);
          assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).highlightKey);
         }
         assertEquals(10, results.get(0).value);
         assertEquals(new BytesRef("foobaz"), results.get(0).payload);
@@ -291,7 +292,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester.build(new InputArrayIterator(keys));
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("penn", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>penn</b>y saved is a <b>penn</b>y earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>penn</b>y saved is a <b>penn</b>y earned", results.get(0).highlightKey);
     suggester.close();
   }
 
@@ -305,7 +307,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester.build(new InputArrayIterator(keys));
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("penn", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>Penn</b>y saved is a <b>penn</b>y earned", results.get(0).key);
    assertEquals("a Penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>Penn</b>y saved is a <b>penn</b>y earned", results.get(0).highlightKey);
     suggester.close();
 
     // Try again, but overriding addPrefixMatch to highlight
@@ -321,7 +324,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester.build(new InputArrayIterator(keys));
     results = suggester.lookup(TestUtil.stringToCharSequence("penn", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>Penny</b> saved is a <b>penny</b> earned", results.get(0).key);
    assertEquals("a Penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>Penny</b> saved is a <b>penny</b> earned", results.get(0).highlightKey);
     suggester.close();
   }
 
@@ -366,7 +370,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester.build(new InputArrayIterator(keys));
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("a", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a bob for <b>a</b>pples", results.get(0).key);
    assertEquals("a bob for apples", results.get(0).key);
    assertEquals("a bob for <b>a</b>pples", results.get(0).highlightKey);
     suggester.close();
   }
 
@@ -379,29 +384,34 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
     suggester.refresh();
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(2, results.size());
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
    assertEquals("lend me your <b>ear</b>", results.get(1).key);
    assertEquals("lend me your ear", results.get(1).key);
    assertEquals("lend me your <b>ear</b>", results.get(1).highlightKey);
     assertEquals(8, results.get(1).value);
     assertEquals(new BytesRef("foobar"), results.get(1).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("ear ", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("lend me your <b>ear</b>", results.get(0).key);
    assertEquals("lend me your ear", results.get(0).key);
    assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
     assertEquals(8, results.get(0).value);
     assertEquals(new BytesRef("foobar"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("pen", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("p", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
@@ -417,7 +427,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("pen p", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("the <b>pen</b> is <b>p</b>retty", results.get(0).key);
    assertEquals("the pen is pretty", results.get(0).key);
    assertEquals("the <b>pen</b> is <b>p</b>retty", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
     suggester.close();
@@ -672,7 +683,11 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
           assertEquals(expectedCount, actual.size());
           for(int i=0;i<expectedCount;i++) {
            assertEquals(expected.get(i).term.utf8ToString(), actual.get(i).key.toString());
            if (doHilite) {
              assertEquals(expected.get(i).term.utf8ToString(), actual.get(i).highlightKey);
            } else {
              assertEquals(expected.get(i).term.utf8ToString(), actual.get(i).key);
            }
             assertEquals(expected.get(i).v, actual.get(i).value);
             assertEquals(expected.get(i).payload, actual.get(i).payload);
           }
@@ -740,7 +755,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
     List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("lend me your <b>ear</b>", results.get(0).key);
    assertEquals("lend me your ear", results.get(0).key);
    assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
     assertEquals(8, results.get(0).value);
     assertEquals(new BytesRef("foobar"), results.get(0).payload);
 
@@ -752,29 +768,34 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
     results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(2, results.size());
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
    assertEquals("lend me your <b>ear</b>", results.get(1).key);
    assertEquals("lend me your ear", results.get(1).key);
    assertEquals("lend me your <b>ear</b>", results.get(1).highlightKey);
     assertEquals(8, results.get(1).value);
     assertEquals(new BytesRef("foobar"), results.get(1).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("ear ", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("lend me your <b>ear</b>", results.get(0).key);
    assertEquals("lend me your ear", results.get(0).key);
    assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
     assertEquals(8, results.get(0).value);
     assertEquals(new BytesRef("foobar"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("pen", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>pen</b>ny saved is a <b>pen</b>ny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
     results = suggester.lookup(TestUtil.stringToCharSequence("p", random()), 10, true, true);
     assertEquals(1, results.size());
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).key);
    assertEquals("a penny saved is a penny earned", results.get(0).key);
    assertEquals("a <b>p</b>enny saved is a <b>p</b>enny earned", results.get(0).highlightKey);
     assertEquals(10, results.get(0).value);
     assertEquals(new BytesRef("foobaz"), results.get(0).payload);
 
@@ -786,10 +807,12 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
 
     results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
     assertEquals(2, results.size());
    assertEquals("lend me your <b>ear</b>", results.get(0).key);
    assertEquals("lend me your ear", results.get(0).key);
    assertEquals("lend me your <b>ear</b>", results.get(0).highlightKey);
     assertEquals(12, results.get(0).value);
     assertEquals(new BytesRef("foobox"), results.get(0).payload);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(1).key);
    assertEquals("a penny saved is a penny earned", results.get(1).key);
    assertEquals("a penny saved is a penny <b>ear</b>ned", results.get(1).highlightKey);
     assertEquals(10, results.get(1).value);
     assertEquals(new BytesRef("foobaz"), results.get(1).payload);
     suggester.close();
@@ -887,7 +910,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       List<LookupResult> results = suggester.lookup(TestUtil.stringToCharSequence("ear", random()), 10, true, true);
       assertEquals(2, results.size());
       LookupResult result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -896,7 +920,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertTrue(result.contexts.contains(new BytesRef("baz")));
 
       result = results.get(1);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -909,7 +934,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(2, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -918,7 +944,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertTrue(result.contexts.contains(new BytesRef("baz")));
 
       result = results.get(1);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -931,7 +958,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(1, results.size());
 
       result = results.get(0);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -952,7 +980,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(1, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -965,7 +994,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(2, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -974,7 +1004,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertTrue(result.contexts.contains(new BytesRef("baz")));
 
       result = results.get(1);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -987,7 +1018,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(2, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -996,7 +1028,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertTrue(result.contexts.contains(new BytesRef("baz")));
 
       result = results.get(1);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -1012,7 +1045,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(1, results.size());
 
       result = results.get(0);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -1042,7 +1076,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(2, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
@@ -1051,7 +1086,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertTrue(result.contexts.contains(new BytesRef("baz")));
 
       result = results.get(1);
      assertEquals("lend me your <b>ear</b>", result.key);
      assertEquals("lend me your ear", result.key);
      assertEquals("lend me your <b>ear</b>", result.highlightKey);
       assertEquals(8, result.value);
       assertEquals(new BytesRef("foobar"), result.payload);
       assertNotNull(result.contexts);
@@ -1067,7 +1103,8 @@ public class AnalyzingInfixSuggesterTest extends LuceneTestCase {
       assertEquals(1, results.size());
 
       result = results.get(0);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.key);
      assertEquals("a penny saved is a penny earned", result.key);
      assertEquals("a penny saved is a penny <b>ear</b>ned", result.highlightKey);
       assertEquals(10, result.value);
       assertEquals(new BytesRef("foobaz"), result.payload);
       assertNotNull(result.contexts);
diff --git a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
index 5e30fb1cfba..41ce876f9e6 100644
-- a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
++ b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/AnalyzingInfixLookupFactory.java
@@ -19,11 +19,15 @@ package org.apache.solr.spelling.suggest.fst;
 
 import java.io.File;
 import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.search.suggest.Lookup;
 import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
 import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.schema.FieldType;
@@ -94,7 +98,25 @@ public class AnalyzingInfixLookupFactory extends LookupFactory {
     try {
       return new AnalyzingInfixSuggester(core.getSolrConfig().luceneMatchVersion, 
                                          FSDirectory.open(new File(indexPath).toPath()), indexAnalyzer,
                                         queryAnalyzer, minPrefixChars, true);
                                         queryAnalyzer, minPrefixChars, true) {
        @Override
        public List<LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
          List<LookupResult> res = super.lookup(key, contexts, num, allTermsRequired, doHighlight);
          if (doHighlight) {
            List<LookupResult> res2 = new ArrayList<>();
            for(LookupResult hit : res) {
              res2.add(new LookupResult(hit.highlightKey.toString(),
                                        hit.highlightKey,
                                        hit.value,
                                        hit.payload,
                                        hit.contexts));
            }
            res = res2;
          }

          return res;
        }
        };
     } catch (IOException e) {
       throw new RuntimeException();
     }
diff --git a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
index 48bdf983b27..1455465ffa8 100644
-- a/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
++ b/solr/core/src/java/org/apache/solr/spelling/suggest/fst/BlendedInfixLookupFactory.java
@@ -19,13 +19,17 @@ package org.apache.solr.spelling.suggest.fst;
 
 import java.io.File;
 import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.search.suggest.Lookup;
 import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.BlendedInfixSuggester.BlenderType;
 import org.apache.lucene.search.suggest.analyzing.BlendedInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.BlendedInfixSuggester.BlenderType;
 import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.schema.FieldType;
@@ -100,7 +104,25 @@ public class BlendedInfixLookupFactory extends AnalyzingInfixLookupFactory {
       return new BlendedInfixSuggester(core.getSolrConfig().luceneMatchVersion, 
                                        FSDirectory.open(new File(indexPath).toPath()),
                                        indexAnalyzer, queryAnalyzer, minPrefixChars,
                                       blenderType, numFactor, true);
                                       blenderType, numFactor, true) {
        @Override
        public List<LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
          List<LookupResult> res = super.lookup(key, contexts, num, allTermsRequired, doHighlight);
          if (doHighlight) {
            List<LookupResult> res2 = new ArrayList<>();
            for(LookupResult hit : res) {
              res2.add(new LookupResult(hit.highlightKey.toString(),
                                        hit.highlightKey,
                                        hit.value,
                                        hit.payload,
                                        hit.contexts));
            }
            res = res2;
          }

          return res;
        }
      };
     } catch (IOException e) {
       throw new RuntimeException();
     }
- 
2.19.1.windows.1

