From 229229a3c0f95e1d37858a98d7a369d7306876f6 Mon Sep 17 00:00:00 2001
From: Mike Klaas <klaas@apache.org>
Date: Mon, 7 Jul 2008 23:52:36 +0000
Subject: [PATCH] SOLR-556 , SOLR-610

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@674677 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |   3 +
 .../highlight/DefaultSolrHighlighter.java     | 188 ++++++------------
 .../solr/highlight/HighlighterTest.java       |  27 +++
 3 files changed, 86 insertions(+), 132 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index 4487803416a..fef9aa3a60f 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -309,6 +309,8 @@ New Features
 58. SOLR-502: Add search timeout support. (Sean Timm via yonik)
     
 59. SOLR-605: Add the ability to register callbacks programatically (ryan, Noble Paul)

60. SOLR-610: hl.maxAnalyzedChars can be -1 to highlight everything (Lars Kotthoff via klaas)
     
 Changes in runtime behavior
  1. SOLR-559: use Lucene updateDocument, deleteDocuments methods.  This
@@ -464,6 +466,7 @@ Bug Fixes
     via useMultiPartPost in CommonsHttpSolrServer.
     (Lars Kotthoff, Andrew Schurman, ryan, yonik)
 
40. SOLR-556: multi-valued fields always highlighted in disparate snippets (Lars Kotthoff via klaas)
 
 Other Changes
  1. SOLR-135: Moved common classes to org.apache.solr.common and altered the
diff --git a/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java b/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
index 0b2281ab44d..489c03e6603 100644
-- a/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
++ b/src/java/org/apache/solr/highlight/DefaultSolrHighlighter.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.Collections;
import java.util.Comparator;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
@@ -27,7 +28,6 @@ import java.util.List;
 import java.util.ListIterator;
 import java.util.Map;
 import java.util.Set;
import java.util.logging.Logger;
 
 import javax.xml.xpath.XPathConstants;
 
@@ -108,9 +108,6 @@ public class DefaultSolrHighlighter extends SolrHighlighter
     highlighter = new Highlighter(getFormatter(fieldName, params), getSpanQueryScorer(query, fieldName, tokenStream, request));
     
     highlighter.setTextFragmenter(getFragmenter(fieldName, params));
    highlighter.setMaxDocBytesToAnalyze(params.getFieldInt(
        fieldName, HighlightParams.MAX_CHARS, 
        Highlighter.DEFAULT_MAX_DOC_BYTES_TO_ANALYZE));
 
     return highlighter;
   }
@@ -127,9 +124,6 @@ public class DefaultSolrHighlighter extends SolrHighlighter
            getFormatter(fieldName, params), 
            getQueryScorer(query, fieldName, request));
      highlighter.setTextFragmenter(getFragmenter(fieldName, params));
     highlighter.setMaxDocBytesToAnalyze(params.getFieldInt(
           fieldName, HighlightParams.MAX_CHARS, 
           Highlighter.DEFAULT_MAX_DOC_BYTES_TO_ANALYZE));
        return highlighter;
   }
   
@@ -272,71 +266,75 @@ public class DefaultSolrHighlighter extends SolrHighlighter
           if (docTexts == null) continue;
           
           TokenStream tstream = null;
          int numFragments = getMaxSnippets(fieldName, params);
          boolean mergeContiguousFragments = isMergeContiguousFragments(fieldName, params);
 
          // create TokenStream
          if (docTexts.length == 1) {
            // single-valued field
          String[] summaries = null;
          List<TextFragment> frags = new ArrayList<TextFragment>();
          for (int j = 0; j < docTexts.length; j++) {
            // create TokenStream
             try {
               // attempt term vectors
               tstream = TokenSources.getTokenStream(searcher.getReader(), docId, fieldName);
             }
             catch (IllegalArgumentException e) {
               // fall back to anaylzer
              tstream = new TokenOrderingFilter(schema.getAnalyzer().tokenStream(fieldName, new StringReader(docTexts[0])), 10);
              tstream = new TokenOrderingFilter(schema.getAnalyzer().tokenStream(fieldName, new StringReader(docTexts[j])), 10);
            }
             
            Highlighter highlighter;
            if (Boolean.valueOf(req.getParams().get(HighlightParams.USE_PHRASE_HIGHLIGHTER))) {
              // wrap CachingTokenFilter around TokenStream for reuse
              tstream = new CachingTokenFilter(tstream);
              
              // get highlighter
              highlighter = getPhraseHighlighter(query, fieldName, req, (CachingTokenFilter) tstream);
               
              // after highlighter initialization, reset tstream since construction of highlighter already used it
              tstream.reset();
            }
            else {
              // use "the old way"
              highlighter = getHighlighter(query, fieldName, req);
             }
          }
          else {
            // multi-valued field
            tstream = new MultiValueTokenStream(fieldName, docTexts, schema.getAnalyzer(), true);
          }
          
          Highlighter highlighter;
          
          if (Boolean.valueOf(req.getParams().get(HighlightParams.USE_PHRASE_HIGHLIGHTER))) {
            // wrap CachingTokenFilter around TokenStream for reuse
            tstream = new CachingTokenFilter(tstream);
             
            // get highlighter
            highlighter = getPhraseHighlighter(query, fieldName, req, (CachingTokenFilter) tstream);
            int maxCharsToAnalyze = params.getFieldInt(fieldName,
                HighlightParams.MAX_CHARS,
                Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE);
            if (maxCharsToAnalyze < 0) {
              highlighter.setMaxDocCharsToAnalyze(docTexts[j].length());
            } else {
              highlighter.setMaxDocCharsToAnalyze(maxCharsToAnalyze);
            }
             
            // after highlighter initialization, reset tstream since construction of highlighter already used it
            tstream.reset();
          }
          else {
            // use "the old way"
            highlighter = getHighlighter(query, fieldName, req);
            TextFragment[] bestTextFragments = highlighter.getBestTextFragments(tstream, docTexts[j], mergeContiguousFragments, numFragments);
            for (int k = 0; k < bestTextFragments.length; k++) {
              if ((bestTextFragments[k] != null) && (bestTextFragments[k].getScore() > 0)) {
                frags.add(bestTextFragments[k]);
              }
            }
           }

          int numFragments = getMaxSnippets(fieldName, params);
          boolean mergeContiguousFragments = isMergeContiguousFragments(fieldName, params);

           String[] summaries = null;
           TextFragment[] frag;
           if (docTexts.length == 1) {
              frag = highlighter.getBestTextFragments(tstream, docTexts[0], mergeContiguousFragments, numFragments);
           }
           else {
               StringBuilder singleValue = new StringBuilder();
               
               for (String txt:docTexts) {
             	  singleValue.append(txt);
               }
             
              frag = highlighter.getBestTextFragments(tstream, singleValue.toString(), false, numFragments);
           }
          // sort such that the fragments with the highest score come first
          Collections.sort(frags, new Comparator<TextFragment>() {
            public int compare(TextFragment arg0, TextFragment arg1) {
              return Math.round(arg1.getScore() - arg0.getScore());
            }
          });
          
            // convert fragments back into text
            // TODO: we can include score and position information in output as snippet attributes
           if (frag.length > 0) {
              ArrayList<String> fragTexts = new ArrayList<String>();
              for (int j = 0; j < frag.length; j++) {
                 if ((frag[j] != null) && (frag[j].getScore() > 0)) {
                    fragTexts.add(frag[j].toString());
                 }
          if (frags.size() > 0) {
            ArrayList<String> fragTexts = new ArrayList<String>();
            for (TextFragment fragment: frags) {
              if ((fragment != null) && (fragment.getScore() > 0)) {
                fragTexts.add(fragment.toString());
               }
              summaries = fragTexts.toArray(new String[0]);
              if (summaries.length > 0) 
                docSummaries.add(fieldName, summaries);
           }
              if (fragTexts.size() >= numFragments) break;
            }
            summaries = fragTexts.toArray(new String[0]);
            if (summaries.length > 0) 
            docSummaries.add(fieldName, summaries);
          }
            // no summeries made, copy text from alternate field
            if (summaries == null || summaries.length == 0) {
               String alternateField = req.getParams().getFieldParam(fieldName, HighlightParams.ALTERNATE_FIELD);
@@ -370,80 +368,6 @@ public class DefaultSolrHighlighter extends SolrHighlighter
   }
 }
 
/** 
 * Creates a single TokenStream out multi-value field values.
 */
class MultiValueTokenStream extends TokenStream {
  private String fieldName;
  private String[] values;
  private Analyzer analyzer;
  private int curIndex;                  // next index into the values array
  private int curOffset;                 // offset into concatenated string
  private TokenStream currentStream;     // tokenStream currently being iterated
  private boolean orderTokenOffsets;

  /** Constructs a TokenStream for consecutively-analyzed field values
   *
   * @param fieldName name of the field
   * @param values array of field data
   * @param analyzer analyzer instance
   */
  public MultiValueTokenStream(String fieldName, String[] values, 
                               Analyzer analyzer, boolean orderTokenOffsets) {
    this.fieldName = fieldName;
    this.values = values;
    this.analyzer = analyzer;
    curIndex = -1;
    curOffset = 0;
    currentStream = null;
    this.orderTokenOffsets=orderTokenOffsets;
  }

  /** Returns the next token in the stream, or null at EOS. */
  @Override
  public Token next() throws IOException {
    int extra = 0;
    if(currentStream == null) {
      curIndex++;        
      if(curIndex < values.length) {
        currentStream = analyzer.tokenStream(fieldName, 
                                             new StringReader(values[curIndex]));
        if (orderTokenOffsets) currentStream = new TokenOrderingFilter(currentStream,10);
        // add extra space between multiple values
        if(curIndex > 0) 
          extra = analyzer.getPositionIncrementGap(fieldName);
      } else {
        return null;
      }
    }
    Token nextToken = currentStream.next();
    if(nextToken == null) {
      curOffset += values[curIndex].length();
      currentStream = null;
      return next();
    }
    // create an modified token which is the offset into the concatenated
    // string of all values
    Token offsetToken = new Token(nextToken.termText(), 
                                  nextToken.startOffset() + curOffset,
                                  nextToken.endOffset() + curOffset);
    offsetToken.setPositionIncrement(nextToken.getPositionIncrement() + extra*10);
    return offsetToken;
  }

  /**
   * Returns all values as a single String into which the Tokens index with
   * their offsets.
   */
  public String asSingleValue() {
    StringBuilder sb = new StringBuilder();
    for(String str : values)
      sb.append(str);
    return sb.toString();
  }
}


 /** Orders Tokens in a window first by their startOffset ascending.
  * endOffset is currently ignored.
  * This is meant to work around fickleness in the highlighter only.  It
diff --git a/src/test/org/apache/solr/highlight/HighlighterTest.java b/src/test/org/apache/solr/highlight/HighlighterTest.java
index 94ea3ffce2c..6610044999a 100755
-- a/src/test/org/apache/solr/highlight/HighlighterTest.java
++ b/src/test/org/apache/solr/highlight/HighlighterTest.java
@@ -185,6 +185,26 @@ public class HighlighterTest extends AbstractSolrTestCase {
             );
 
   }
  
  public void testMultiValueBestFragmentHighlight() {
    HashMap<String,String> args = new HashMap<String,String>();
    args.put("hl", "true");
    args.put("hl.fl", "textgap");
    args.put("df", "textgap");
    TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory(
        "standard", 0, 200, args);
    
    assertU(adoc("textgap", "first entry has one word foo", 
        "textgap", "second entry has both words foo bar",
        "id", "1"));
    assertU(commit());
    assertU(optimize());
    assertQ("Best fragment summarization",
        sumLRF.makeRequest("foo bar"),
        "//lst[@name='highlighting']/lst[@name='1']",
        "//lst[@name='1']/arr[@name='textgap']/str[.=\'second entry has both words <em>foo</em> <em>bar</em>\']"
    );
  }
 
 
   public void testDefaultFieldHighlight() {
@@ -361,6 +381,13 @@ public class HighlighterTest extends AbstractSolrTestCase {
             "//lst[@name='highlighting']/lst[@name='1']",
             "//lst[@name='1'][not(*)]"
             );
    args.put("hl.maxAnalyzedChars", "-1");
    sumLRF = h.getRequestFactory("standard", 0, 200, args);
    assertQ("token at start of text",
        sumLRF.makeRequest("t_text:disjoint"),
        "//lst[@name='highlighting']/lst[@name='1']",
        "//lst[@name='1']/arr[count(str)=1]"
    );
   }
   public void testRegexFragmenter() {
     HashMap<String,String> args = new HashMap<String,String>();
- 
2.19.1.windows.1

