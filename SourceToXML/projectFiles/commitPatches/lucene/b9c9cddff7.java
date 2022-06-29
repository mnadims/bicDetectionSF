From b9c9cddff7cef08e8b0433a203771e48e662e7b1 Mon Sep 17 00:00:00 2001
From: Mike McCandless <mikemccand@apache.org>
Date: Wed, 22 Feb 2017 05:16:47 -0500
Subject: [PATCH] LUCENE-7698: fix CommonGramsQueryFilter to not produce a
 disconnected token graph

--
 lucene/CHANGES.txt                             |  4 ++++
 .../commongrams/CommonGramsQueryFilter.java    |  6 ++++++
 .../TestCommonGramsQueryFilterFactory.java     | 10 ++++++++++
 .../org/apache/lucene/util/QueryBuilder.java   | 17 ++++++++++++++++-
 .../queryparser/classic/TestQueryParser.java   | 18 ++++++++++++++++++
 5 files changed, 54 insertions(+), 1 deletion(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index a3028acb683..dda4a45d4cc 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -142,6 +142,10 @@ Bug Fixes
   rewritten child query in their equals and hashCode implementations.
   (Adrien Grand)
 
* LUCENE-7698: CommonGramsQueryFilter was producing a disconnected
  token graph, messing up phrase queries when it was used during query
  parsing (Ere Maijala via Mike McCandless)

 Improvements
 
 * LUCENE-7055: Added Weight#scorerSupplier, which allows to estimate the cost
diff --git a/lucene/analysis/common/src/java/org/apache/lucene/analysis/commongrams/CommonGramsQueryFilter.java b/lucene/analysis/common/src/java/org/apache/lucene/analysis/commongrams/CommonGramsQueryFilter.java
index eeaec846981..9307e7b74aa 100644
-- a/lucene/analysis/common/src/java/org/apache/lucene/analysis/commongrams/CommonGramsQueryFilter.java
++ b/lucene/analysis/common/src/java/org/apache/lucene/analysis/commongrams/CommonGramsQueryFilter.java
@@ -20,6 +20,7 @@ import java.io.IOException;
 
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
 import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
 
 import static org.apache.lucene.analysis.commongrams.CommonGramsFilter.GRAM_TYPE;
@@ -46,6 +47,7 @@ public final class CommonGramsQueryFilter extends TokenFilter {
 
   private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
   private final PositionIncrementAttribute posIncAttribute = addAttribute(PositionIncrementAttribute.class);
  private final PositionLengthAttribute posLengthAttribute = addAttribute(PositionLengthAttribute.class);
   
   private State previous;
   private String previousType;
@@ -91,6 +93,8 @@ public final class CommonGramsQueryFilter extends TokenFilter {
         
         if (isGramType()) {
           posIncAttribute.setPositionIncrement(1);
          // We must set this back to 1 (from e.g. 2 or higher) otherwise the token graph is disconnected:
          posLengthAttribute.setPositionLength(1);
         }
         return true;
       }
@@ -109,6 +113,8 @@ public final class CommonGramsQueryFilter extends TokenFilter {
     
     if (isGramType()) {
       posIncAttribute.setPositionIncrement(1);
      // We must set this back to 1 (from e.g. 2 or higher) otherwise the token graph is disconnected:
      posLengthAttribute.setPositionLength(1);
     }
     return true;
   }
diff --git a/lucene/analysis/common/src/test/org/apache/lucene/analysis/commongrams/TestCommonGramsQueryFilterFactory.java b/lucene/analysis/common/src/test/org/apache/lucene/analysis/commongrams/TestCommonGramsQueryFilterFactory.java
index 23d1bd4224f..fee6e886dd0 100644
-- a/lucene/analysis/common/src/test/org/apache/lucene/analysis/commongrams/TestCommonGramsQueryFilterFactory.java
++ b/lucene/analysis/common/src/test/org/apache/lucene/analysis/commongrams/TestCommonGramsQueryFilterFactory.java
@@ -92,4 +92,14 @@ public class TestCommonGramsQueryFilterFactory extends BaseTokenStreamFactoryTes
     });
     assertTrue(expected.getMessage().contains("Unknown parameters"));
   }

  public void testCompleteGraph() throws Exception {
    CommonGramsQueryFilterFactory factory = (CommonGramsQueryFilterFactory) tokenFilterFactory("CommonGramsQuery");
    CharArraySet words = factory.getCommonWords();
    assertTrue("words is null and it shouldn't be", words != null);
    assertTrue(words.contains("the"));
    Tokenizer tokenizer = whitespaceMockTokenizer("testing the factory works");
    TokenStream stream = factory.create(tokenizer);
    assertGraphStrings(stream, "testing_the the_factory factory works");
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java b/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
index b8d8c290517..c3998420394 100644
-- a/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
++ b/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
@@ -57,6 +57,7 @@ import org.apache.lucene.util.graph.GraphTokenStreamFiniteStrings;
 public class QueryBuilder {
   protected Analyzer analyzer;
   protected boolean enablePositionIncrements = true;
  protected boolean enableGraphQueries = true;
   protected boolean autoGenerateMultiTermSynonymsPhraseQuery = false;
   
   /** Creates a new QueryBuilder using the given analyzer. */
@@ -240,6 +241,20 @@ public class QueryBuilder {
     }
   }
 
  /** Enable or disable graph TokenStream processing (enabled by default).
   *
   * @lucene.experimental */
  public void setEnableGraphQueries(boolean v) {
    enableGraphQueries = v;
  }

  /** Returns true if graph TokenStream processing is enabled (default).
   *
   * @lucene.experimental */
  public boolean getEnableGraphQueries() {
    return enableGraphQueries;
  }

   /**
    * Creates a query from a token stream.
    *
@@ -282,7 +297,7 @@ public class QueryBuilder {
         }
 
         int positionLength = posLenAtt.getPositionLength();
        if (!isGraph && positionLength > 1) {
        if (enableGraphQueries && positionLength > 1) {
           isGraph = true;
         }
       }
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
index 1d7a0f6178e..e83b62a1bf0 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
@@ -533,6 +533,24 @@ public class TestQueryParser extends QueryParserTestBase {
     assertEquals(guineaPigPhrase, smart.parse("\"guinea pig\""));
   }
 
  public void testEnableGraphQueries() throws Exception {
    QueryParser dumb = new QueryParser("field", new Analyzer1());
    dumb.setSplitOnWhitespace(false);
    dumb.setEnableGraphQueries(false);
    
    TermQuery guinea = new TermQuery(new Term("field", "guinea"));
    TermQuery pig = new TermQuery(new Term("field", "pig"));
    TermQuery cavy = new TermQuery(new Term("field", "cavy"));

    // A multi-word synonym source will just form a boolean query when graph queries are disabled:
    Query inner = new SynonymQuery(new Term[] {new Term("field", "cavy"), new Term("field", "guinea")});
    BooleanQuery.Builder b = new BooleanQuery.Builder();
    b.add(inner, BooleanClause.Occur.SHOULD);
    b.add(pig, BooleanClause.Occur.SHOULD);
    BooleanQuery query = b.build();
    assertEquals(query, dumb.parse("guinea pig"));
  }

   // TODO: Move to QueryParserTestBase once standard flexible parser gets this capability
   public void testOperatorsAndMultiWordSynonyms() throws Exception {
     Analyzer a = new MockSynonymAnalyzer();
- 
2.19.1.windows.1

