From cc5882ab7568f8946c7eb7d6ff549130ec32d7d4 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Mon, 19 Jul 2010 18:58:30 +0000
Subject: [PATCH] LUCENE-2458: queryparser turns all CJK queries into phrase
 queries

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@965585 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   7 ++
 .../vectorhighlight/FieldQueryTest.java       |   4 +-
 .../ext/ExtendableQueryParser.java            |   4 +-
 .../precedence/PrecedenceQueryParser.java     |  20 ++--
 .../precedence/PrecedenceQueryParser.jj       |  20 ++--
 .../standard/QueryParserWrapper.java          |   8 +-
 .../AnalyzerQueryNodeProcessor.java           |  11 +-
 .../precedence/TestPrecedenceQueryParser.java |  91 ++++++++++++++-
 .../queryParser/standard/TestQPHelper.java    |  90 ++++++++++++++-
 .../standard/TestQueryParserWrapper.java      |  90 ++++++++++++++-
 .../queryParser/MultiFieldQueryParser.java    |  29 ++++-
 .../lucene/queryParser/QueryParser.java       |  72 ++++++++++--
 .../apache/lucene/queryParser/QueryParser.jj  |  72 ++++++++++--
 .../queryParser/QueryParserTokenManager.java  |   1 +
 .../lucene/queryParser/TestMultiAnalyzer.java |  12 +-
 .../lucene/queryParser/TestQueryParser.java   | 106 +++++++++++++++++-
 .../search/ExtendedDismaxQParserPlugin.java   |   4 +-
 .../apache/solr/search/SolrQueryParser.java   |   6 +-
 .../org/apache/solr/util/SolrPluginUtils.java |   6 +-
 .../org/apache/solr/ConvertedLegacyTest.java  |   2 +-
 20 files changed, 585 insertions(+), 70 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 8f2b7308743..f23a2660c07 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -439,6 +439,13 @@ Bug fixes
 
 * LUCENE-2549: Fix TimeLimitingCollector#TimeExceededException to record
   the absolute docid.  (Uwe Schindler)

* LUCENE-2458: QueryParser no longer automatically forms phrase queries,
  assuming whitespace tokenization. Previously all CJK queries, for example,
  would be turned into phrase queries. The old behavior is preserved with
  the matchVersion parameter for previous versions. Additionally, you can
  explicitly enable the old behavior with setAutoGeneratePhraseQueries(true) 
  (Robert Muir)
   
 New features
 
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
index cb73765fcaf..42924fdc8c9 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
@@ -55,7 +55,7 @@ public class FieldQueryTest extends AbstractTestCase {
   }
 
   public void testFlattenTermAndPhrase2gram() throws Exception {
    Query query = paB.parse( "AA AND BCD OR EFGH" );
    Query query = paB.parse( "AA AND \"BCD\" OR \"EFGH\"" );
     FieldQuery fq = new FieldQuery( query, true, true );
     Set<Query> flatQueries = new HashSet<Query>();
     fq.flatten( query, flatQueries );
@@ -679,7 +679,7 @@ public class FieldQueryTest extends AbstractTestCase {
   }
   
   public void testQueryPhraseMapOverlap2gram() throws Exception {
    Query query = paB.parse( "abc AND bcd" );
    Query query = paB.parse( "\"abc\" AND \"bcd\"" );
     
     // phraseHighlight = true, fieldMatch = true
     FieldQuery fq = new FieldQuery( query, true, true );
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
index 1533d11d5bd..6592c60afef 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
@@ -126,7 +126,7 @@ public class ExtendableQueryParser extends QueryParser {
   }
 
   @Override
  protected Query getFieldQuery(final String field, final String queryText)
  protected Query getFieldQuery(final String field, final String queryText, boolean quoted)
       throws ParseException {
     final Pair<String,String> splitExtensionField = this.extensions
         .splitExtensionField(defaultField, field);
@@ -136,7 +136,7 @@ public class ExtendableQueryParser extends QueryParser {
       return extension.parse(new ExtensionQuery(this, splitExtensionField.cur,
           queryText));
     }
    return super.getFieldQuery(field, queryText);
    return super.getFieldQuery(field, queryText, quoted);
   }
 
 }
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
index 3ff9dfb3ae5..eb199c6265a 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
@@ -299,7 +299,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -330,15 +330,19 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
       source.restoreState(list.get(0));
       return new TermQuery(new Term(field, termAtt.toString()));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = new BooleanQuery();
          BooleanQuery q = new BooleanQuery(positionCount == 1);

          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < list.size(); i++) {
             source.restoreState(list.get(i));
             TermQuery currentQuery = new TermQuery(
                 new Term(field, termAtt.toString()));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -371,7 +375,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
   }
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -379,7 +383,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -847,7 +851,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
          }
          q = getFuzzyQuery(field, termImage, fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, false);
        }
       break;
     case RANGEIN_START:
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
index c8f740b4ea0..bac09854dc9 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
@@ -323,7 +323,7 @@ public class PrecedenceQueryParser {
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -354,15 +354,19 @@ public class PrecedenceQueryParser {
       source.restoreState(list.get(0));
       return new TermQuery(new Term(field, termAtt.toString()));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = new BooleanQuery();
          BooleanQuery q = new BooleanQuery(positionCount == 1);
          
          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < list.size(); i++) {
             source.restoreState(list.get(i));
             TermQuery currentQuery = new TermQuery(
                 new Term(field, termAtt.toString()));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -395,7 +399,7 @@ public class PrecedenceQueryParser {
   }
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -403,7 +407,7 @@ public class PrecedenceQueryParser {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -830,7 +834,7 @@ Query Term(String field) : {
        	 }
          q = getFuzzyQuery(field, termImage, fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, false);
        }
      }
      | ( <RANGEIN_START> ( goop1=<RANGEIN_GOOP>|goop1=<RANGEIN_QUOTED> )
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
index a5783d72dc7..7572d7ade67 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
@@ -448,10 +448,16 @@ public class QueryParserWrapper {
     throw new UnsupportedOperationException();
   }
 
  /** @deprecated Use {@link #getFieldQuery(String, String, boolean)} instead */
  @Deprecated
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
    return getFieldQuery(field, queryText, true);
  }

   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)
  protected Query getFieldQuery(String field, String queryText, boolean quoted)
       throws ParseException {
     throw new UnsupportedOperationException();
   }
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
index 818b3f98c14..ea995156452 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
@@ -36,6 +36,7 @@ import org.apache.lucene.queryParser.core.nodes.GroupQueryNode;
 import org.apache.lucene.queryParser.core.nodes.NoTokenFoundQueryNode;
 import org.apache.lucene.queryParser.core.nodes.ParametricQueryNode;
 import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.QuotedFieldQueryNode;
 import org.apache.lucene.queryParser.core.nodes.TextableQueryNode;
 import org.apache.lucene.queryParser.core.nodes.TokenizedPhraseQueryNode;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
@@ -187,8 +188,8 @@ public class AnalyzerQueryNodeProcessor extends QueryNodeProcessorImpl {
 
         return fieldNode;
 
      } else if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      } else if (severalTokensAtSamePosition || !(node instanceof QuotedFieldQueryNode)) {
        if (positionCount == 1 || !(node instanceof QuotedFieldQueryNode)) {
           // no phrase query:
           LinkedList<QueryNode> children = new LinkedList<QueryNode>();
 
@@ -206,9 +207,11 @@ public class AnalyzerQueryNodeProcessor extends QueryNodeProcessorImpl {
             children.add(new FieldQueryNode(field, term, -1, -1));
 
           }

          return new GroupQueryNode(
          if (positionCount == 1)
            return new GroupQueryNode(
               new StandardBooleanQueryNode(children, true));
          else
            return new StandardBooleanQueryNode(children, false);
 
         } else {
           // phrase query:
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
index 5f26ed078f0..9336eff5dba 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
@@ -23,9 +23,12 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.PhraseQuery;
@@ -280,6 +283,90 @@ public class TestPrecedenceQueryParser extends LocalizedTestCase {
     assertQueryEquals("term term1 term2", a, "term term1 term2");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }

  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }
  
  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   // failing tests disabled since PrecedenceQueryParser
   // is currently unmaintained
   public void _testWildcard() throws Exception {
@@ -353,11 +440,11 @@ public class TestPrecedenceQueryParser extends LocalizedTestCase {
     assertQueryEquals("term -stop term", qpAnalyzer, "term term");
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
                      "term \"phrase1 phrase2\" term");
                      "term (phrase1 phrase2) term");
     // note the parens in this next assertion differ from the original
     // QueryParser behavior
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
                      "(+term -\"phrase1 phrase2\") term");
                      "(+term -(phrase1 phrase2)) term");
     assertQueryEquals("stop", qpAnalyzer, "");
     assertQueryEquals("stop OR stop AND stop", qpAnalyzer, "");
     assertTrue(getQuery("term term term", qpAnalyzer) instanceof BooleanQuery);
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
index 4d3e3840355..3a8156f8b4a 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
@@ -37,6 +37,7 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -57,6 +58,7 @@ import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
 import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute.Operator;
 import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.IndexSearcher;
@@ -331,6 +333,90 @@ public class TestQPHelper extends LocalizedTestCase {
     assertQueryEqualsAllowLeadingWildcard("??\u3000??\u3000??", null, "??\u0020??\u0020??");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }

  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }
  
  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   public void testSimple() throws Exception {
     assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
     assertQueryEquals("term term term", null, "term term term");
@@ -529,10 +615,10 @@ public class TestQPHelper extends LocalizedTestCase {
 
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
        "term \"phrase1 phrase2\" term");
        "term phrase1 phrase2 term");
 
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
        "+term -\"phrase1 phrase2\" term");
        "+term -(phrase1 phrase2) term");
 
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
index fc18e2ce98f..d6f74d3f50e 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
@@ -35,6 +35,7 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.DateField;
@@ -53,6 +54,7 @@ import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
 import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
 import org.apache.lucene.queryParser.standard.processors.WildcardQueryNodeProcessor;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.IndexSearcher;
@@ -323,6 +325,90 @@ public class TestQueryParserWrapper extends LocalizedTestCase {
     assertQueryEqualsAllowLeadingWildcard("??\u3000??\u3000??", null, "??\u0020??\u0020??");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }

  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }
  
  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }
  
   public void testSimple() throws Exception {
     assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
     assertQueryEquals("term term term", null, "term term term");
@@ -528,10 +614,10 @@ public class TestQueryParserWrapper extends LocalizedTestCase {
 
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
        "term \"phrase1 phrase2\" term");
        "term phrase1 phrase2 term");
 
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
        "+term -\"phrase1 phrase2\" term");
        "+term -(phrase1 phrase2) term");
 
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
diff --git a/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java b/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
index 90f1b5fa755..284e35c1806 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
++ b/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
@@ -101,7 +101,7 @@ public class MultiFieldQueryParser extends QueryParser
     if (field == null) {
       List<BooleanClause> clauses = new ArrayList<BooleanClause>();
       for (int i = 0; i < fields.length; i++) {
        Query q = super.getFieldQuery(fields[i], queryText);
        Query q = super.getFieldQuery(fields[i], queryText, true);
         if (q != null) {
           //If the user passes a map of boosts
           if (boosts != null) {
@@ -119,7 +119,7 @@ public class MultiFieldQueryParser extends QueryParser
         return null;
       return getBooleanQuery(clauses, true);
     }
    Query q = super.getFieldQuery(field, queryText);
    Query q = super.getFieldQuery(field, queryText, true);
     applySlop(q,slop);
     return q;
   }
@@ -134,8 +134,29 @@ public class MultiFieldQueryParser extends QueryParser
   
 
   @Override
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
    return getFieldQuery(field, queryText, 0);
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
    if (field == null) {
      List<BooleanClause> clauses = new ArrayList<BooleanClause>();
      for (int i = 0; i < fields.length; i++) {
        Query q = super.getFieldQuery(fields[i], queryText, quoted);
        if (q != null) {
          //If the user passes a map of boosts
          if (boosts != null) {
            //Get the boost from the map and apply them
            Float boost = boosts.get(fields[i]);
            if (boost != null) {
              q.setBoost(boost.floatValue());
            }
          }
          clauses.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
        }
      }
      if (clauses.size() == 0)  // happens for stopwords
        return null;
      return getBooleanQuery(clauses, true);
    }
    Query q = super.getFieldQuery(field, queryText, quoted);
    return q;
   }
 
 
diff --git a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
index 9fed418e9a7..115a3e7ad27 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
++ b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
@@ -34,6 +34,7 @@ import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.WildcardQuery;
 import org.apache.lucene.util.Version;
import org.apache.lucene.util.VirtualMethod;
 
 /**
  * This class is generated by JavaCC.  The most important method is
@@ -107,6 +108,8 @@ import org.apache.lucene.util.Version;
  * <ul>
  *    <li> As of 2.9, {@link #setEnablePositionIncrements} is true by
  *         default.
 *    <li> As of 3.1, {@link #setAutoGeneratePhraseQueries} is false by
 *         default.
  * </ul>
  */
 public class QueryParser implements QueryParserConstants {
@@ -150,6 +153,19 @@ public class QueryParser implements QueryParserConstants {
   // for use when constructing RangeQuerys.
   Collator rangeCollator = null;
 
  /** @deprecated remove when getFieldQuery is removed */
  private static final VirtualMethod<QueryParser> getFieldQueryMethod =
    new VirtualMethod<QueryParser>(QueryParser.class, "getFieldQuery", String.class, String.class);
  /** @deprecated remove when getFieldQuery is removed */
  private static final VirtualMethod<QueryParser> getFieldQueryWithQuotedMethod =
    new VirtualMethod<QueryParser>(QueryParser.class, "getFieldQuery", String.class, String.class, boolean.class);
  /** @deprecated remove when getFieldQuery is removed */
  private final boolean hasNewAPI =
    VirtualMethod.compareImplementationDistance(getClass(),
        getFieldQueryWithQuotedMethod, getFieldQueryMethod) >= 0; // its ok for both to be overridden

  private boolean autoGeneratePhraseQueries;

   /** The default operator for parsing queries. 
    * Use {@link QueryParser#setDefaultOperator} to change it.
    */
@@ -169,6 +185,11 @@ public class QueryParser implements QueryParserConstants {
     } else {
       enablePositionIncrements = false;
     }
    if (matchVersion.onOrAfter(Version.LUCENE_31)) {
      setAutoGeneratePhraseQueries(false);
    } else {
      setAutoGeneratePhraseQueries(true);
    }
   }
 
   /** Parses a query string, returning a {@link org.apache.lucene.search.Query}.
@@ -214,6 +235,29 @@ public class QueryParser implements QueryParserConstants {
     return field;
   }
 
  /**
   * @see #setAutoGeneratePhraseQueries(boolean)
   */
  public final boolean getAutoGeneratePhraseQueries() {
    return autoGeneratePhraseQueries;
  }

  /**
   * Set to true if phrase queries will be automatically generated
   * when the analyzer returns more than one term from whitespace
   * delimited text.
   * NOTE: this behavior may not be suitable for all languages.
   * <p>
   * Set to false if phrase queries should only be generated when
   * surrounded by double quotes.
   */
  public final void setAutoGeneratePhraseQueries(boolean value) {
    if (value == false && !hasNewAPI)
      throw new IllegalArgumentException("You must implement the new API: getFieldQuery(String,String,boolean)"
       + " to use setAutoGeneratePhraseQueries(false)");
    this.autoGeneratePhraseQueries = value;
  }

    /**
    * Get the minimal similarity for fuzzy queries.
    */
@@ -506,11 +550,19 @@ public class QueryParser implements QueryParserConstants {
       throw new RuntimeException("Clause cannot be both required and prohibited");
   }
 
  /**
   * @deprecated Use {@link #getFieldQuery(String,String,boolean)} instead.
   */
  @Deprecated
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
    // treat the text as if it was quoted, to drive phrase logic with old versions.
    return getFieldQuery(field, queryText, true);
  }
 
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -587,10 +639,14 @@ public class QueryParser implements QueryParserConstants {
       }
       return newTermQuery(new Term(field, term));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || (!quoted && !autoGeneratePhraseQueries)) {
        if (positionCount == 1 || (!quoted && !autoGeneratePhraseQueries)) {
           // no phrase query:
          BooleanQuery q = newBooleanQuery(true);
          BooleanQuery q = newBooleanQuery(positionCount == 1);

          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < numTokens; i++) {
             String term = null;
             try {
@@ -603,7 +659,7 @@ public class QueryParser implements QueryParserConstants {
 
             Query currentQuery = newTermQuery(
                 new Term(field, term));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -682,7 +738,7 @@ public class QueryParser implements QueryParserConstants {
 
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -690,7 +746,7 @@ public class QueryParser implements QueryParserConstants {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = hasNewAPI ? getFieldQuery(field, queryText, true) : getFieldQuery(field, queryText);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -1343,7 +1399,7 @@ public class QueryParser implements QueryParserConstants {
          }
          q = getFuzzyQuery(field, termImage,fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = hasNewAPI ? getFieldQuery(field, termImage, false) : getFieldQuery(field, termImage);
        }
       break;
     case RANGEIN_START:
diff --git a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
index fa4eed3cbc0..0747aaeee3e 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
++ b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
@@ -58,6 +58,7 @@ import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.WildcardQuery;
 import org.apache.lucene.util.Version;
import org.apache.lucene.util.VirtualMethod;
 
 /**
  * This class is generated by JavaCC.  The most important method is
@@ -131,6 +132,8 @@ import org.apache.lucene.util.Version;
  * <ul>
  *    <li> As of 2.9, {@link #setEnablePositionIncrements} is true by
  *         default.
 *    <li> As of 3.1, {@link #setAutoGeneratePhraseQueries} is false by
 *         default.
  * </ul>
  */
 public class QueryParser {
@@ -174,6 +177,19 @@ public class QueryParser {
   // for use when constructing RangeQuerys.
   Collator rangeCollator = null;
 
  /** @deprecated remove when getFieldQuery is removed */
  private static final VirtualMethod<QueryParser> getFieldQueryMethod =
    new VirtualMethod<QueryParser>(QueryParser.class, "getFieldQuery", String.class, String.class);
  /** @deprecated remove when getFieldQuery is removed */
  private static final VirtualMethod<QueryParser> getFieldQueryWithQuotedMethod =
    new VirtualMethod<QueryParser>(QueryParser.class, "getFieldQuery", String.class, String.class, boolean.class);
  /** @deprecated remove when getFieldQuery is removed */
  private final boolean hasNewAPI = 
    VirtualMethod.compareImplementationDistance(getClass(), 
        getFieldQueryWithQuotedMethod, getFieldQueryMethod) >= 0; // its ok for both to be overridden

  private boolean autoGeneratePhraseQueries;

   /** The default operator for parsing queries. 
    * Use {@link QueryParser#setDefaultOperator} to change it.
    */
@@ -193,6 +209,11 @@ public class QueryParser {
     } else {
       enablePositionIncrements = false;
     }
    if (matchVersion.onOrAfter(Version.LUCENE_31)) {
      setAutoGeneratePhraseQueries(false);
    } else {
      setAutoGeneratePhraseQueries(true);
    }
   }
 
   /** Parses a query string, returning a {@link org.apache.lucene.search.Query}.
@@ -238,6 +259,29 @@ public class QueryParser {
     return field;
   }
 
  /**
   * @see #setAutoGeneratePhraseQueries(boolean)
   */
  public final boolean getAutoGeneratePhraseQueries() {
    return autoGeneratePhraseQueries;
  }
  
  /**
   * Set to true if phrase queries will be automatically generated
   * when the analyzer returns more than one term from whitespace
   * delimited text.
   * NOTE: this behavior may not be suitable for all languages.
   * <p>
   * Set to false if phrase queries should only be generated when
   * surrounded by double quotes.
   */
  public final void setAutoGeneratePhraseQueries(boolean value) {
    if (value == false && !hasNewAPI)
      throw new IllegalArgumentException("You must implement the new API: getFieldQuery(String,String,boolean)"
       + " to use setAutoGeneratePhraseQueries(false)");
    this.autoGeneratePhraseQueries = value;
  }
  
    /**
    * Get the minimal similarity for fuzzy queries.
    */
@@ -530,11 +574,19 @@ public class QueryParser {
       throw new RuntimeException("Clause cannot be both required and prohibited");
   }
 
  /**
   * @deprecated Use {@link #getFieldQuery(String,String,boolean)} instead.
   */
  @Deprecated
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
    // treat the text as if it was quoted, to drive phrase logic with old versions.
    return getFieldQuery(field, queryText, true);
  }
 
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -611,10 +663,14 @@ public class QueryParser {
       }
       return newTermQuery(new Term(field, term));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || (!quoted && !autoGeneratePhraseQueries)) {
        if (positionCount == 1 || (!quoted && !autoGeneratePhraseQueries)) {
           // no phrase query:
          BooleanQuery q = newBooleanQuery(true);
          BooleanQuery q = newBooleanQuery(positionCount == 1);
          
          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ? 
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < numTokens; i++) {
             String term = null;
             try {
@@ -627,7 +683,7 @@ public class QueryParser {
 
             Query currentQuery = newTermQuery(
                 new Term(field, term));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -706,7 +762,7 @@ public class QueryParser {
 
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -714,7 +770,7 @@ public class QueryParser {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = hasNewAPI ? getFieldQuery(field, queryText, true) : getFieldQuery(field, queryText);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -1314,7 +1370,7 @@ Query Term(String field) : {
        	 }
        	 q = getFuzzyQuery(field, termImage,fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = hasNewAPI ? getFieldQuery(field, termImage, false) : getFieldQuery(field, termImage);
        }
      }
      | ( <RANGEIN_START> ( goop1=<RANGEIN_GOOP>|goop1=<RANGEIN_QUOTED> )
diff --git a/lucene/src/java/org/apache/lucene/queryParser/QueryParserTokenManager.java b/lucene/src/java/org/apache/lucene/queryParser/QueryParserTokenManager.java
index 5443eea46c3..3049d41cc96 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/QueryParserTokenManager.java
++ b/lucene/src/java/org/apache/lucene/queryParser/QueryParserTokenManager.java
@@ -32,6 +32,7 @@ import org.apache.lucene.search.TermRangeQuery;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.WildcardQuery;
 import org.apache.lucene.util.Version;
import org.apache.lucene.util.VirtualMethod;
 
 /** Token Manager. */
 public class QueryParserTokenManager implements QueryParserConstants
diff --git a/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java b/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
index 69979a4f797..061086cccde 100644
-- a/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
++ b/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
@@ -104,9 +104,9 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
     // direct call to (super's) getFieldQuery to demonstrate differnce
     // between phrase and multiphrase with modified default slop
     assertEquals("\"foo bar\"~99",
                 qp.getSuperFieldQuery("","foo bar").toString());
                 qp.getSuperFieldQuery("","foo bar", true).toString());
     assertEquals("\"(multi multi2) bar\"~99",
                 qp.getSuperFieldQuery("","multi bar").toString());
                 qp.getSuperFieldQuery("","multi bar", true).toString());
 
     
     // ask sublcass to parse phrase with modified default slop
@@ -243,15 +243,15 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
         }
 
         /** expose super's version */
        public Query getSuperFieldQuery(String f, String t) 
        public Query getSuperFieldQuery(String f, String t, boolean quoted) 
             throws ParseException {
            return super.getFieldQuery(f,t);
            return super.getFieldQuery(f,t,quoted);
         }
         /** wrap super's version */
         @Override
        protected Query getFieldQuery(String f, String t)
        protected Query getFieldQuery(String f, String t, boolean quoted)
             throws ParseException {
            return new DumbQueryWrapper(getSuperFieldQuery(f,t));
            return new DumbQueryWrapper(getSuperFieldQuery(f,t,quoted));
         }
     }
     
diff --git a/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java b/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
index 3691b8775a9..c272a5f2350 100644
-- a/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
++ b/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
@@ -34,6 +34,7 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.DateField;
@@ -45,6 +46,7 @@ import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.MultiTermQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.IndexSearcher;
@@ -248,7 +250,103 @@ public class TestQueryParser extends LocalizedTestCase {
 	 assertQueryEquals("term\u3000term\u3000term", null, "term\u0020term\u0020term");
 	 assertQueryEquals("用語\u3000用語\u3000用語", null, "用語\u0020用語\u0020用語");
   }

  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }

  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }

  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer(); 
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
   
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }
  
  public void testAutoGeneratePhraseQueriesOn() throws Exception {
    // individual CJK chars as terms
    SimpleCJKAnalyzer analyzer = new SimpleCJKAnalyzer(); 
  
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    QueryParser parser = new QueryParser(TEST_VERSION_CURRENT, "field", analyzer);
    parser.setAutoGeneratePhraseQueries(true);
    assertEquals(expected, parser.parse("中国"));
  }

   public void testSimple() throws Exception {
     assertQueryEquals("term term term", null, "term term term");
     assertQueryEquals("türm term term", new MockAnalyzer(), "türm term term");
@@ -437,9 +535,9 @@ public class TestQueryParser extends LocalizedTestCase {
     
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
                      "term \"phrase1 phrase2\" term");
                      "term (phrase1 phrase2) term");
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
                      "+term -\"phrase1 phrase2\" term");
                      "+term -(phrase1 phrase2) term");
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
     assertQueryEquals("(stop)^3", qpAnalyzer, "");
@@ -912,9 +1010,9 @@ public class TestQueryParser extends LocalizedTestCase {
       }
 
       @Override
      protected Query getFieldQuery(String field, String queryText) throws ParseException {
      protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
         type[0]=3;
        return super.getFieldQuery(field, queryText);
        return super.getFieldQuery(field, queryText, quoted);
       }
     };
 
diff --git a/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java b/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
index 7b481e1f920..3a386d15e66 100755
-- a/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
++ b/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
@@ -869,7 +869,7 @@ class ExtendedDismaxQParser extends QParser {
     int slop;
 
     @Override
    protected Query getFieldQuery(String field, String val) throws ParseException {
    protected Query getFieldQuery(String field, String val, boolean quoted) throws ParseException {
 //System.out.println("getFieldQuery: val="+val);
 
       this.type = QType.FIELD;
@@ -1004,7 +1004,7 @@ class ExtendedDismaxQParser extends QParser {
         switch (type) {
           case FIELD:  // fallthrough
           case PHRASE:
            Query query = super.getFieldQuery(field, val);
            Query query = super.getFieldQuery(field, val, type == QType.PHRASE);
             if (query instanceof PhraseQuery) {
               PhraseQuery pq = (PhraseQuery)query;
               if (minClauseSize > 1 && pq.getTerms().length < minClauseSize) return null;
diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index cfbf2cc1eeb..9882e220dca 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -87,7 +87,7 @@ public class SolrQueryParser extends QueryParser {
   }
 
   public SolrQueryParser(QParser parser, String defaultField, Analyzer analyzer) {
    super(Version.LUCENE_24, defaultField, analyzer);
    super(parser.getReq().getSchema().getSolrConfig().getLuceneVersion("luceneMatchVersion", Version.LUCENE_24), defaultField, analyzer);
     this.schema = parser.getReq().getSchema();
     this.parser = parser;
     this.defaultField = defaultField;
@@ -126,7 +126,7 @@ public class SolrQueryParser extends QueryParser {
     }
   }
 
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
     checkNullField(field);
     // intercept magic field name of "_" to use as a hook for our
     // own functions.
@@ -150,7 +150,7 @@ public class SolrQueryParser extends QueryParser {
     }
 
     // default to a normal field query
    return super.getFieldQuery(field, queryText);
    return super.getFieldQuery(field, queryText, quoted);
   }
 
   protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
diff --git a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
index 71386af9d44..88cb045bc25 100644
-- a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
++ b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
@@ -842,7 +842,7 @@ public class SolrPluginUtils {
      * DisjunctionMaxQuery.  (so yes: aliases which point at other
      * aliases should work)
      */
    protected Query getFieldQuery(String field, String queryText)
    protected Query getFieldQuery(String field, String queryText, boolean quoted)
       throws ParseException {
             
       if (aliases.containsKey(field)) {
@@ -857,7 +857,7 @@ public class SolrPluginUtils {
                 
         for (String f : a.fields.keySet()) {
 
          Query sub = getFieldQuery(f,queryText);
          Query sub = getFieldQuery(f,queryText,quoted);
           if (null != sub) {
             if (null != a.fields.get(f)) {
               sub.setBoost(a.fields.get(f));
@@ -870,7 +870,7 @@ public class SolrPluginUtils {
 
       } else {
         try {
          return super.getFieldQuery(field, queryText);
          return super.getFieldQuery(field, queryText, quoted);
         } catch (Exception e) {
           return null;
         }
diff --git a/solr/src/test/org/apache/solr/ConvertedLegacyTest.java b/solr/src/test/org/apache/solr/ConvertedLegacyTest.java
index 8dd279e4235..d5a8e667e0e 100644
-- a/solr/src/test/org/apache/solr/ConvertedLegacyTest.java
++ b/solr/src/test/org/apache/solr/ConvertedLegacyTest.java
@@ -998,7 +998,7 @@ public class ConvertedLegacyTest extends SolrTestCaseJ4 {
     assertQ(req("id:42 AND subword:IBM's")
             ,"*[count(//doc)=1]"
             );
    assertQ(req("id:42 AND subword:IBM'sx")
    assertQ(req("id:42 AND subword:\"IBM'sx\"")
             ,"*[count(//doc)=0]"
             );
 
- 
2.19.1.windows.1

