From 1e79c3b98e532cca4f1536301f2100d7dd1a96cc Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 25 Jan 2012 16:21:33 +0000
Subject: [PATCH] LUCENE-3721: CharFilters were not being invoked in Solr

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1235810 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/solr/analysis/TokenizerChain.java  | 20 +++++++++++++++++--
 .../apache/solr/BasicFunctionalityTest.java   | 13 ++++++++++++
 2 files changed, 31 insertions(+), 2 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
index 19ee0e63092..d8a5522f8c1 100644
-- a/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
++ b/solr/core/src/java/org/apache/solr/analysis/TokenizerChain.java
@@ -19,6 +19,7 @@ package org.apache.solr.analysis;
 
 import org.apache.lucene.analysis.*;
 
import java.io.IOException;
 import java.io.Reader;
 
 /**
@@ -48,6 +49,21 @@ public final class TokenizerChain extends SolrAnalyzer {
   public TokenizerFactory getTokenizerFactory() { return tokenizer; }
   public TokenFilterFactory[] getTokenFilterFactories() { return filters; }
 
  class SolrTokenStreamComponents extends TokenStreamComponents {
    public SolrTokenStreamComponents(final Tokenizer source, final TokenStream result) {
      super(source, result);
    }

    @Override
    protected void reset(Reader reader) throws IOException {
      // the tokenizers are currently reset by the indexing process, so only
      // the tokenizer needs to be reset.
      Reader r = initReader(reader);
      super.reset(r);
    }
  }
  
  
   @Override
   public Reader initReader(Reader reader) {
     if (charFilters != null && charFilters.length > 0) {
@@ -62,12 +78,12 @@ public final class TokenizerChain extends SolrAnalyzer {
 
   @Override
   protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
    Tokenizer tk = tokenizer.create(aReader);
    Tokenizer tk = tokenizer.create( initReader(aReader) );
     TokenStream ts = tk;
     for (TokenFilterFactory filter : filters) {
       ts = filter.create(ts);
     }
    return new TokenStreamComponents(tk, ts);
    return new SolrTokenStreamComponents(tk, ts);
   }
 
   @Override
diff --git a/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java b/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
index 8992f345069..3c29ac24cb6 100644
-- a/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
++ b/solr/core/src/test/org/apache/solr/BasicFunctionalityTest.java
@@ -222,6 +222,19 @@ public class BasicFunctionalityTest extends SolrTestCaseJ4 {
             );
   }
 
  @Test
  public void testHTMLStrip() {
    assertU(add(doc("id","200", "HTMLwhitetok","&#65;&#66;&#67;")));
    assertU(add(doc("id","201", "HTMLwhitetok","&#65;B&#67;")));      // do it again to make sure reuse is working
    assertU(commit());
    assertQ(req("q","HTMLwhitetok:A&#66;C")
        ,"//*[@numFound='2']"
    );
    assertQ(req("q","HTMLwhitetok:&#65;BC")
        ,"//*[@numFound='2']"
    );
  }

 
   @Test
   public void testClientErrorOnMalformedNumbers() throws Exception {
- 
2.19.1.windows.1

