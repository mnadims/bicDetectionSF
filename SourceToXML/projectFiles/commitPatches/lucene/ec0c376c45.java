From ec0c376c45d477dde4c14b2ca764360d64f8bca8 Mon Sep 17 00:00:00 2001
From: Uwe Schindler <uschindler@apache.org>
Date: Fri, 18 Sep 2009 15:36:24 +0000
Subject: [PATCH] LUCENE-1919: Fix analysis back compat break. Thanks to Robert
 Muir for the testcases, and Yonik and Mark Miller for testing!

git-svn-id: https://svn.apache.org/repos/asf/lucene/java/trunk@816673 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/lucene/analysis/TokenStream.java   |  25 ++--
 .../analysis/TestTokenStreamBWComp.java       | 107 ++++++++++++++----
 2 files changed, 102 insertions(+), 30 deletions(-)

diff --git a/src/java/org/apache/lucene/analysis/TokenStream.java b/src/java/org/apache/lucene/analysis/TokenStream.java
index 33d33f19160..cf70dbe0d14 100644
-- a/src/java/org/apache/lucene/analysis/TokenStream.java
++ b/src/java/org/apache/lucene/analysis/TokenStream.java
@@ -29,6 +29,7 @@ import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Payload;
 import org.apache.lucene.util.Attribute;
 import org.apache.lucene.util.AttributeImpl;
 import org.apache.lucene.util.AttributeSource;
@@ -377,10 +378,7 @@ public abstract class TokenStream extends AttributeSource {
       return incrementToken() ? tokenWrapper.delegate : null;
     } else {
       assert supportedMethods.hasNext;
      final Token token = next();
      if (token == null) return null;
      tokenWrapper.delegate = token;
      return token;
      return next();
     }
   }
 
@@ -396,15 +394,24 @@ public abstract class TokenStream extends AttributeSource {
     if (tokenWrapper == null)
       throw new UnsupportedOperationException("This TokenStream only supports the new Attributes API.");
     
    final Token nextToken;
     if (supportedMethods.hasIncrementToken) {
      return incrementToken() ? ((Token) tokenWrapper.delegate.clone()) : null;
      final Token savedDelegate = tokenWrapper.delegate;
      tokenWrapper.delegate = new Token();
      nextToken = incrementToken() ? tokenWrapper.delegate : null;
      tokenWrapper.delegate = savedDelegate;
     } else {
       assert supportedMethods.hasReusableNext;
      final Token token = next(tokenWrapper.delegate);
      if (token == null) return null;
      tokenWrapper.delegate = token;
      return (Token) token.clone();
      nextToken = next(new Token());
     }
    
    if (nextToken != null) {
      Payload p = nextToken.getPayload();
      if (p != null) {
        nextToken.setPayload((Payload) p.clone());
      }
    }
    return nextToken;
   }
 
   /**
diff --git a/src/test/org/apache/lucene/analysis/TestTokenStreamBWComp.java b/src/test/org/apache/lucene/analysis/TestTokenStreamBWComp.java
index f4ac2ec6690..67d13d9b562 100644
-- a/src/test/org/apache/lucene/analysis/TestTokenStreamBWComp.java
++ b/src/test/org/apache/lucene/analysis/TestTokenStreamBWComp.java
@@ -27,8 +27,9 @@ import org.apache.lucene.analysis.tokenattributes.*;
 /** This class tests some special cases of backwards compatibility when using the new TokenStream API with old analyzers */
 public class TestTokenStreamBWComp extends LuceneTestCase {
 
  private final String doc = "This is the new TokenStream api";
  private final String[] stopwords = new String[] {"is", "the", "this"};
  private static final String doc = "This is the new TokenStream api";
  private static final String[] stopwords = new String[] {"is", "the", "this"};
  private static final String[] results = new String[] {"new", "tokenstream", "api"};
 
   public static class POSToken extends Token {
     public static final int PROPERNOUN = 1;
@@ -190,14 +191,17 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
     PayloadAttribute payloadAtt = (PayloadAttribute) stream.addAttribute(PayloadAttribute.class);
     TermAttribute termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);
     
    int i=0;
     while (stream.incrementToken()) {
       String term = termAtt.term();
       Payload p = payloadAtt.getPayload();
       if (p != null && p.getData().length == 1 && p.getData()[0] == PartOfSpeechAnnotatingFilter.PROPER_NOUN_ANNOTATION) {
        assertTrue("only TokenStream is a proper noun", "tokenstream".equals(term));
        assertEquals("only TokenStream is a proper noun", "tokenstream", term);
       } else {
         assertFalse("all other tokens (if this test fails, the special POSToken subclass is not correctly passed through the chain)", "tokenstream".equals(term));
       }
      assertEquals(results[i], term);
      i++;
     }   
   }
 
@@ -205,14 +209,17 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
     stream.reset();
     Token reusableToken = new Token();
     
    int i=0;
     while ((reusableToken = stream.next(reusableToken)) != null) {
       String term = reusableToken.term();
       Payload p = reusableToken.getPayload();
       if (p != null && p.getData().length == 1 && p.getData()[0] == PartOfSpeechAnnotatingFilter.PROPER_NOUN_ANNOTATION) {
        assertTrue("only TokenStream is a proper noun", "tokenstream".equals(term));
        assertEquals("only TokenStream is a proper noun", "tokenstream", term);
       } else {
         assertFalse("all other tokens (if this test fails, the special POSToken subclass is not correctly passed through the chain)", "tokenstream".equals(term));
       }
      assertEquals(results[i], term);
      i++;
     }   
   }
 
@@ -220,14 +227,17 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
     stream.reset();
     
     Token token;
    int i=0;
     while ((token = stream.next()) != null) {
       String term = token.term();
       Payload p = token.getPayload();
       if (p != null && p.getData().length == 1 && p.getData()[0] == PartOfSpeechAnnotatingFilter.PROPER_NOUN_ANNOTATION) {
        assertTrue("only TokenStream is a proper noun", "tokenstream".equals(term));
        assertEquals("only TokenStream is a proper noun", "tokenstream", term);
       } else {
         assertFalse("all other tokens (if this test fails, the special POSToken subclass is not correctly passed through the chain)", "tokenstream".equals(term));
       }
      assertEquals(results[i], term);
      i++;
     }   
   }
   
@@ -245,7 +255,7 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
         while (stream.incrementToken());
         fail("If only the new API is allowed, this should fail with an UOE");
       } catch (UnsupportedOperationException uoe) {
        assertTrue((PartOfSpeechTaggingFilter.class.getName()+" does not implement incrementToken() which is needed for onlyUseNewAPI.").equals(uoe.getMessage()));
        assertEquals((PartOfSpeechTaggingFilter.class.getName()+" does not implement incrementToken() which is needed for onlyUseNewAPI."),uoe.getMessage());
       }
 
       // this should pass, as all core token streams support the new API
@@ -255,17 +265,17 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
       while (stream.incrementToken());
       
       // Test, if all attributes are implemented by their implementation, not Token/TokenWrapper
      assertTrue("TermAttribute is implemented by TermAttributeImpl",
      assertTrue("TermAttribute is not implemented by TermAttributeImpl",
         stream.addAttribute(TermAttribute.class) instanceof TermAttributeImpl);
      assertTrue("OffsetAttribute is implemented by OffsetAttributeImpl",
      assertTrue("OffsetAttribute is not implemented by OffsetAttributeImpl",
         stream.addAttribute(OffsetAttribute.class) instanceof OffsetAttributeImpl);
      assertTrue("FlagsAttribute is implemented by FlagsAttributeImpl",
      assertTrue("FlagsAttribute is not implemented by FlagsAttributeImpl",
         stream.addAttribute(FlagsAttribute.class) instanceof FlagsAttributeImpl);
      assertTrue("PayloadAttribute is implemented by PayloadAttributeImpl",
      assertTrue("PayloadAttribute is not implemented by PayloadAttributeImpl",
         stream.addAttribute(PayloadAttribute.class) instanceof PayloadAttributeImpl);
      assertTrue("PositionIncrementAttribute is implemented by PositionIncrementAttributeImpl", 
      assertTrue("PositionIncrementAttribute is not implemented by PositionIncrementAttributeImpl", 
         stream.addAttribute(PositionIncrementAttribute.class) instanceof PositionIncrementAttributeImpl);
      assertTrue("TypeAttribute is implemented by TypeAttributeImpl",
      assertTrue("TypeAttribute is not implemented by TypeAttributeImpl",
         stream.addAttribute(TypeAttribute.class) instanceof TypeAttributeImpl);
         
       // try to call old API, this should fail
@@ -275,14 +285,14 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
         while ((reusableToken = stream.next(reusableToken)) != null);
         fail("If only the new API is allowed, this should fail with an UOE");
       } catch (UnsupportedOperationException uoe) {
        assertTrue("This TokenStream only supports the new Attributes API.".equals(uoe.getMessage()));
        assertEquals("This TokenStream only supports the new Attributes API.", uoe.getMessage());
       }
       try {
         stream.reset();
         while (stream.next() != null);
         fail("If only the new API is allowed, this should fail with an UOE");
       } catch (UnsupportedOperationException uoe) {
        assertTrue("This TokenStream only supports the new Attributes API.".equals(uoe.getMessage()));
        assertEquals("This TokenStream only supports the new Attributes API.", uoe.getMessage());
       }
       
       // Test if the wrapper API (onlyUseNewAPI==false) uses TokenWrapper
@@ -292,17 +302,17 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
       // itsself.
       TokenStream.setOnlyUseNewAPI(false);
       stream = new WhitespaceTokenizer(new StringReader(doc));
      assertTrue("TermAttribute is implemented by TokenWrapper",
      assertTrue("TermAttribute is not implemented by TokenWrapper",
         stream.addAttribute(TermAttribute.class) instanceof TokenWrapper);
      assertTrue("OffsetAttribute is implemented by TokenWrapper",
      assertTrue("OffsetAttribute is not implemented by TokenWrapper",
         stream.addAttribute(OffsetAttribute.class) instanceof TokenWrapper);
      assertTrue("FlagsAttribute is implemented by TokenWrapper",
      assertTrue("FlagsAttribute is not implemented by TokenWrapper",
         stream.addAttribute(FlagsAttribute.class) instanceof TokenWrapper);
      assertTrue("PayloadAttribute is implemented by TokenWrapper",
      assertTrue("PayloadAttribute is not implemented by TokenWrapper",
         stream.addAttribute(PayloadAttribute.class) instanceof TokenWrapper);
      assertTrue("PositionIncrementAttribute is implemented by TokenWrapper",
      assertTrue("PositionIncrementAttribute is not implemented by TokenWrapper",
         stream.addAttribute(PositionIncrementAttribute.class) instanceof TokenWrapper);
      assertTrue("TypeAttribute is implemented by TokenWrapper",
      assertTrue("TypeAttribute is not implemented by TokenWrapper",
         stream.addAttribute(TypeAttribute.class) instanceof TokenWrapper);
       
     } finally {
@@ -321,8 +331,63 @@ public class TestTokenStreamBWComp extends LuceneTestCase {
       while (stream.incrementToken());
       fail("One TokenFilter does not override any of the required methods, so it should fail.");
     } catch (UnsupportedOperationException uoe) {
      assertTrue(uoe.getMessage().endsWith("does not implement any of incrementToken(), next(Token), next()."));
      assertTrue("invalid UOE message", uoe.getMessage().endsWith("does not implement any of incrementToken(), next(Token), next()."));
     }
   }
   
  public void testMixedOldApiConsumer() throws Exception {
    // WhitespaceTokenizer is using incrementToken() API:
    TokenStream stream = new WhitespaceTokenizer(new StringReader("foo bar moo maeh"));
    
    Token foo = new Token();
    foo = stream.next(foo);
    Token bar = stream.next();
    assertEquals("foo", foo.term());
    assertEquals("bar", bar.term());
    
    Token moo = stream.next(foo);
    assertEquals("moo", moo.term());
    assertEquals("private 'bar' term should still be valid", "bar", bar.term());
    
    // and now we also use incrementToken()... (very bad, but should work)
    TermAttribute termAtt = (TermAttribute) stream.getAttribute(TermAttribute.class);
    assertTrue(stream.incrementToken());
    assertEquals("maeh", termAtt.term());    
    assertEquals("private 'bar' term should still be valid", "bar", bar.term());    
  }
  
  /*
   * old api that cycles thru foo, bar, meh
   */
  private class RoundRobinOldAPI extends TokenStream {
    int count = 0;
    String terms[] = { "foo", "bar", "meh" };

    public Token next(Token reusableToken) throws IOException {
      reusableToken.setTermBuffer(terms[count % terms.length]);
      count++;
      return reusableToken;
    }
  }
  
  public void testMixedOldApiConsumer2() throws Exception {
    // RoundRobinOldAPI is using TokenStream(next)
    TokenStream stream = new RoundRobinOldAPI();
    TermAttribute termAtt = (TermAttribute) stream.getAttribute(TermAttribute.class);
    
    assertTrue(stream.incrementToken());
    Token bar = stream.next();
    assertEquals("foo", termAtt.term());
    assertEquals("bar", bar.term());

    assertTrue(stream.incrementToken());
    assertEquals("meh", termAtt.term());
    assertEquals("private 'bar' term should still be valid", "bar", bar.term());

    Token foo = stream.next();
    assertEquals("the term attribute should still be the same", "meh", termAtt.term());
    assertEquals("foo", foo.term());
    assertEquals("private 'bar' term should still be valid", "bar", bar.term());
  }
  
 }
- 
2.19.1.windows.1

