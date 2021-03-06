From 3b57a5a624bbe5b3ef36cee4e3f1a68e5287be99 Mon Sep 17 00:00:00 2001
From: Koji Sekiguchi <koji@apache.org>
Date: Fri, 26 Dec 2008 01:08:18 +0000
Subject: [PATCH] SOLR-925: Fixed highlighting on fields with
 multiValued="true" and termOffsets="true"

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@729450 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  4 +-
 .../highlight/DefaultSolrHighlighter.java     | 46 +++++++++++++++++-
 .../solr/highlight/HighlighterTest.java       | 47 +++++++++++++++++++
 src/test/test-files/solr/conf/schema.xml      |  2 +
 4 files changed, 97 insertions(+), 2 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index 7afa8a6d156..906eb314129 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -118,7 +118,7 @@ New Features
     optimized distributed faceting refinement by lowering parsing overhead and
     by making requests and responses smaller.
 
25. SOLR-876: WOrdDelimiterFilter now supports a splitOnNumerics 
25. SOLR-876: WordDelimiterFilter now supports a splitOnNumerics 
     option, as well as a list of protected terms.
     (Dan Rosher via hossman)
 
@@ -200,6 +200,8 @@ Bug Fixes
 
 22. SOLR-897: Fixed Argument list too long error when there are lots of snapshots/backups (Dan Rosher via billa)
 
23. SOLR-925: Fixed highlighting on fields with multiValued="true" and termOffsets="true" (koji)

 
 Other Changes
 ----------------------
diff --git a/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java b/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
index 489c03e6603..43904525281 100644
-- a/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
++ b/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
@@ -271,11 +271,14 @@ public class DefaultSolrHighlighter extends SolrHighlighter
 
           String[] summaries = null;
           List<TextFragment> frags = new ArrayList<TextFragment>();
          TermOffsetsTokenStream tots = null;
           for (int j = 0; j < docTexts.length; j++) {
             // create TokenStream
             try {
               // attempt term vectors
              tstream = TokenSources.getTokenStream(searcher.getReader(), docId, fieldName);
              if( tots == null )
                tots = new TermOffsetsTokenStream( TokenSources.getTokenStream(searcher.getReader(), docId, fieldName) );
              tstream = tots.getMultiValuedTokenStream( docTexts[j].length() );
             }
             catch (IllegalArgumentException e) {
               // fall back to anaylzer
@@ -410,3 +413,44 @@ class TokenOrderingFilter extends TokenFilter {
     return queue.isEmpty() ? null : queue.removeFirst();
   }
 }

class TermOffsetsTokenStream {

  TokenStream bufferedTokenStream = null;
  Token bufferedToken;
  int startOffset;
  int endOffset;

  public TermOffsetsTokenStream( TokenStream tstream ){
    bufferedTokenStream = tstream;
    startOffset = 0;
    bufferedToken = null;
  }

  public TokenStream getMultiValuedTokenStream( final int length ){
    endOffset = startOffset + length;
    return new TokenStream(){
      Token token;
      public Token next() throws IOException {
        while( true ){
          if( bufferedToken == null )
            bufferedToken = bufferedTokenStream.next();
          if( bufferedToken == null ) return null;
          if( startOffset <= bufferedToken.startOffset() &&
              bufferedToken.endOffset() <= endOffset ){
            token = bufferedToken;
            bufferedToken = null;
            token.setStartOffset( token.startOffset() - startOffset );
            token.setEndOffset( token.endOffset() - startOffset );
            return token;
          }
          else if( bufferedToken.endOffset() > endOffset ){
            startOffset += length + 1;
            return null;
          }
          bufferedToken = null;
        }
      }
    };
  }
}
diff --git a/src/test/org/apache/solr/highlight/HighlighterTest.java b/src/test/org/apache/solr/highlight/HighlighterTest.java
index 2ebeb9fe74c..01893fb254f 100755
-- a/src/test/org/apache/solr/highlight/HighlighterTest.java
++ b/src/test/org/apache/solr/highlight/HighlighterTest.java
@@ -17,10 +17,16 @@
 
 package org.apache.solr.highlight;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.util.*;
 import org.apache.solr.common.params.HighlightParams;
 
import java.io.IOException;
import java.io.StringReader;
 import java.util.HashMap;
 
 /**
@@ -140,6 +146,47 @@ public class HighlighterTest extends AbstractSolrTestCase {
             "//arr[@name='tv_text']/str[.=' <em>long</em> fragments.']"
             );
   }
  
  public void testTermOffsetsTokenStream() throws Exception {
    String[] multivalued = { "a b c d", "e f g", "h", "i j k l m n" };
    Analyzer a1 = new WhitespaceAnalyzer();
    TermOffsetsTokenStream tots = new TermOffsetsTokenStream(
        a1.tokenStream( "", new StringReader( "a b c d e f g h i j k l m n" ) ) );
    for( String v : multivalued ){
      TokenStream ts1 = tots.getMultiValuedTokenStream( v.length() );
      Analyzer a2 = new WhitespaceAnalyzer();
      TokenStream ts2 = a2.tokenStream( "", new StringReader( v ) );
      Token t1 = new Token();
      Token t2 = new Token();
      for( t1 = ts1.next( t1 ); t1 != null; t1 = ts1.next( t1 ) ){
        t2 = ts2.next( t2 );
        assertEquals( t2, t1 );
      }
    }
  }

  public void testTermVecMultiValuedHighlight() throws Exception {

    // do summarization using term vectors on multivalued field
    HashMap<String,String> args = new HashMap<String,String>();
    args.put("hl", "true");
    args.put("hl.fl", "tv_mv_text");
    args.put("hl.snippets", "2");
    TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory(
      "standard",0,200,args);
    
    assertU(adoc("tv_mv_text", LONG_TEXT, 
                 "tv_mv_text", LONG_TEXT, 
                 "id", "1"));
    assertU(commit());
    assertU(optimize());
    assertQ("Basic summarization",
            sumLRF.makeRequest("tv_mv_text:long"),
            "//lst[@name='highlighting']/lst[@name='1']",
            "//lst[@name='1']/arr[@name='tv_mv_text']/str[.='a <em>long</em> days night this should be a piece of text which']",
            "//arr[@name='tv_mv_text']/str[.=' <em>long</em> fragments.']"
            );
  }
 
   public void testDisMaxHighlight() {
 
diff --git a/src/test/test-files/solr/conf/schema.xml b/src/test/test-files/solr/conf/schema.xml
index 50c0f421447..93449b6acd6 100644
-- a/src/test/test-files/solr/conf/schema.xml
++ b/src/test/test-files/solr/conf/schema.xml
@@ -455,6 +455,8 @@
    <dynamicField name="t_*"  type="text"    indexed="true"  stored="true"/>
    <dynamicField name="tv_*"  type="text" indexed="true"  stored="true" 
       termVectors="true" termPositions="true" termOffsets="true"/>
   <dynamicField name="tv_mv_*"  type="text" indexed="true"  stored="true" multivalued="true"
      termVectors="true" termPositions="true" termOffsets="true"/>
 
    <!-- special fields for dynamic copyField test -->
    <dynamicField name="dynamic_*" type="string" indexed="true" stored="true"/>
- 
2.19.1.windows.1

