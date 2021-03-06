From 00b29b4298be4e850e5df263371790ef84b3288f Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Wed, 28 Nov 2012 21:29:42 +0000
Subject: [PATCH] SOLR-4121: fix single quoted token issue with solr qparser

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1414929 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/parser/QueryParser.java   |  20 +-
 .../org/apache/solr/parser/QueryParser.jj     |   6 +-
 .../solr/parser/QueryParserConstants.java     |  31 +-
 .../solr/parser/QueryParserTokenManager.java  | 553 ++++++++----------
 4 files changed, 288 insertions(+), 322 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParser.java b/solr/core/src/java/org/apache/solr/parser/QueryParser.java
index 6aeba3f173a..709ad00dd38 100644
-- a/solr/core/src/java/org/apache/solr/parser/QueryParser.java
++ b/solr/core/src/java/org/apache/solr/parser/QueryParser.java
@@ -418,12 +418,6 @@ public class QueryParser extends SolrQueryParserBase implements QueryParserConst
     finally { jj_save(0, xla); }
   }
 
  private boolean jj_3R_3() {
    if (jj_scan_token(STAR)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

   private boolean jj_3R_2() {
     if (jj_scan_token(TERM)) return true;
     if (jj_scan_token(COLON)) return true;
@@ -440,6 +434,12 @@ public class QueryParser extends SolrQueryParserBase implements QueryParserConst
     return false;
   }
 
  private boolean jj_3R_3() {
    if (jj_scan_token(STAR)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

   /** Generated Token Manager. */
   public QueryParserTokenManager token_source;
   /** Current token. */
@@ -458,10 +458,10 @@ public class QueryParser extends SolrQueryParserBase implements QueryParserConst
       jj_la1_init_1();
    }
    private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x600,0x600,0x3800,0x3800,0x7f54fe00,0x440000,0x80000,0x80000,0x7f54c000,0x47444000,0x800000,0x800000,0x80000,0x18000000,0x0,0x80000000,0x0,0x0,0x80000,0x800000,0x80000,0x5f544000,};
      jj_la1_0 = new int[] {0x600,0x600,0x3800,0x3800,0x3fb4fe00,0x240000,0x80000,0x80000,0x3fb4c000,0x23a44000,0x400000,0x400000,0x80000,0xc000000,0x0,0x40000000,0x0,0x80000000,0x80000,0x400000,0x80000,0x2fb44000,};
    }
    private static void jj_la1_init_1() {
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0xc,0x0,0xc,0x3,0x0,0x0,0x0,0x0,};
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x6,0x0,0x6,0x1,0x0,0x0,0x0,0x0,};
    }
   final private JJCalls[] jj_2_rtns = new JJCalls[1];
   private boolean jj_rescan = false;
@@ -615,7 +615,7 @@ public class QueryParser extends SolrQueryParserBase implements QueryParserConst
   /** Generate ParseException. */
   public ParseException generateParseException() {
     jj_expentries.clear();
    boolean[] la1tokens = new boolean[36];
    boolean[] la1tokens = new boolean[35];
     if (jj_kind >= 0) {
       la1tokens[jj_kind] = true;
       jj_kind = -1;
@@ -632,7 +632,7 @@ public class QueryParser extends SolrQueryParserBase implements QueryParserConst
         }
       }
     }
    for (int i = 0; i < 36; i++) {
    for (int i = 0; i < 35; i++) {
       if (la1tokens[i]) {
         jj_expentry = new int[1];
         jj_expentry[0] = i;
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParser.jj b/solr/core/src/java/org/apache/solr/parser/QueryParser.jj
index a1989f19825..041e114ec90 100644
-- a/solr/core/src/java/org/apache/solr/parser/QueryParser.jj
++ b/solr/core/src/java/org/apache/solr/parser/QueryParser.jj
@@ -89,7 +89,6 @@ PARSER_END(QueryParser)
  | <STAR:      "*" >
  | <CARAT:     "^" > : Boost
  | <QUOTED:     "\"" (<_QUOTED_CHAR>)* "\"">
 | <SQUOTED:     "'" (<_SQUOTED_CHAR>)* "'">
  | <TERM:      <_TERM_START_CHAR> (<_TERM_CHAR>)*  >
  | <FUZZY_SLOP:     "~" ( (<_NUM_CHAR>)+ ( "." (<_NUM_CHAR>)+ )? )? >
  | <PREFIXTERM:  ("*") | ( <_TERM_START_CHAR> (<_TERM_CHAR>)* "*" ) >
@@ -97,7 +96,10 @@ PARSER_END(QueryParser)
  | <REGEXPTERM: "/" (~[ "/" ] | "\\/" )* "/" >
  | <RANGEIN_START: "[" > : Range
  | <RANGEEX_START: "{" > : Range
 | <LPARAMS:     ("{!" ( (<_WHITESPACE>)* (~["=","}"])+ ( "=" (<QUOTED> | <SQUOTED> | (~[" ","}"])+ )? )? )* "}")+  (~[")"," ","\t","\n","{","^"])*  >
// TODO: consider using token states instead of inlining SQUOTED
//  | <SQUOTED:     "'" (<_SQUOTED_CHAR>)* "'">
//  | <LPARAMS:     ("{!" ( (<_WHITESPACE>)* (~["=","}"])+ ( "=" (<QUOTED> | <SQUOTED> | (~[" ","}"])+ )? )? )* "}")+  (~[")"," ","\t","\n","{","^"])*  >
  | <LPARAMS:     ("{!" ( (<_WHITESPACE>)* (~["=","}"])+ ( "=" (<QUOTED> | ("'" (<_SQUOTED_CHAR>)* "'") | (~[" ","}"])+ )? )? )* "}")+  (~[")"," ","\t","\n","{","^"])*  >
 }
 
 <Boost> TOKEN : {
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java b/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
index 2bb3d75a52f..ecc4e213e38 100644
-- a/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
++ b/solr/core/src/java/org/apache/solr/parser/QueryParserConstants.java
@@ -49,35 +49,33 @@ public interface QueryParserConstants {
   /** RegularExpression Id. */
   int QUOTED = 20;
   /** RegularExpression Id. */
  int SQUOTED = 21;
  int TERM = 21;
   /** RegularExpression Id. */
  int TERM = 22;
  int FUZZY_SLOP = 22;
   /** RegularExpression Id. */
  int FUZZY_SLOP = 23;
  int PREFIXTERM = 23;
   /** RegularExpression Id. */
  int PREFIXTERM = 24;
  int WILDTERM = 24;
   /** RegularExpression Id. */
  int WILDTERM = 25;
  int REGEXPTERM = 25;
   /** RegularExpression Id. */
  int REGEXPTERM = 26;
  int RANGEIN_START = 26;
   /** RegularExpression Id. */
  int RANGEIN_START = 27;
  int RANGEEX_START = 27;
   /** RegularExpression Id. */
  int RANGEEX_START = 28;
  int LPARAMS = 28;
   /** RegularExpression Id. */
  int LPARAMS = 29;
  int NUMBER = 29;
   /** RegularExpression Id. */
  int NUMBER = 30;
  int RANGE_TO = 30;
   /** RegularExpression Id. */
  int RANGE_TO = 31;
  int RANGEIN_END = 31;
   /** RegularExpression Id. */
  int RANGEIN_END = 32;
  int RANGEEX_END = 32;
   /** RegularExpression Id. */
  int RANGEEX_END = 33;
  int RANGE_QUOTED = 33;
   /** RegularExpression Id. */
  int RANGE_QUOTED = 34;
  /** RegularExpression Id. */
  int RANGE_GOOP = 35;
  int RANGE_GOOP = 34;
 
   /** Lexical state. */
   int Boost = 0;
@@ -109,7 +107,6 @@ public interface QueryParserConstants {
     "\"*\"",
     "\"^\"",
     "<QUOTED>",
    "<SQUOTED>",
     "<TERM>",
     "<FUZZY_SLOP>",
     "<PREFIXTERM>",
diff --git a/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java b/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
index 0f6c1529a03..c55dd0e445c 100644
-- a/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
++ b/solr/core/src/java/org/apache/solr/parser/QueryParserTokenManager.java
@@ -49,7 +49,7 @@ private int jjMoveStringLiteralDfa0_2()
       case 41:
          return jjStopAtPos(0, 16);
       case 42:
         return jjStartNfaWithStates_2(0, 18, 66);
         return jjStartNfaWithStates_2(0, 18, 61);
       case 43:
          return jjStartNfaWithStates_2(0, 12, 15);
       case 45:
@@ -57,11 +57,11 @@ private int jjMoveStringLiteralDfa0_2()
       case 58:
          return jjStopAtPos(0, 17);
       case 91:
         return jjStopAtPos(0, 27);
         return jjStopAtPos(0, 26);
       case 94:
          return jjStopAtPos(0, 19);
       case 123:
         return jjStartNfaWithStates_2(0, 28, 40);
         return jjStartNfaWithStates_2(0, 27, 35);
       default :
          return jjMoveNfa_2(0, 0);
    }
@@ -89,7 +89,7 @@ static final long[] jjbitVec4 = {
 private int jjMoveNfa_2(int startState, int curPos)
 {
    int startsAt = 0;
   jjnewStateCnt = 66;
   jjnewStateCnt = 61;
    int i = 1;
    jjstateSet[0] = startState;
    int kind = 0x7fffffff;
@@ -104,20 +104,20 @@ private int jjMoveNfa_2(int startState, int curPos)
          {
             switch(jjstateSet[--i])
             {
               case 66:
               case 32:
               case 61:
               case 27:
                   if ((0xfbfffcf8ffffd9ffL & l) == 0L)
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
                case 0:
                   if ((0xfbff54f8ffffd9ffL & l) != 0L)
                   {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                     if (kind > 24)
                        kind = 24;
                     jjCheckNAddTwoStates(27, 28);
                   }
                   else if ((0x100002600L & l) != 0L)
                   {
@@ -132,23 +132,21 @@ private int jjMoveNfa_2(int startState, int curPos)
                      jjCheckNAddStates(3, 5);
                   if ((0x7bff50f8ffffd9ffL & l) != 0L)
                   {
                     if (kind > 22)
                        kind = 22;
                     if (kind > 21)
                        kind = 21;
                      jjCheckNAddStates(6, 10);
                   }
                   else if (curChar == 42)
                   {
                     if (kind > 24)
                        kind = 24;
                     if (kind > 23)
                        kind = 23;
                   }
                   else if (curChar == 33)
                   {
                      if (kind > 11)
                         kind = 11;
                   }
                  if (curChar == 39)
                     jjCheckNAddStates(11, 13);
                  else if (curChar == 38)
                  if (curChar == 38)
                      jjstateSet[jjnewStateCnt++] = 4;
                   break;
                case 4:
@@ -186,150 +184,135 @@ private int jjMoveNfa_2(int startState, int curPos)
                   if (curChar == 34 && kind > 20)
                      kind = 20;
                   break;
               case 21:
                  if (curChar == 39)
                     jjCheckNAddStates(11, 13);
                  break;
                case 22:
                  if ((0xffffff7fffffffffL & l) != 0L)
                     jjCheckNAddStates(11, 13);
                  break;
               case 24:
                  jjCheckNAddStates(11, 13);
                  break;
               case 25:
                  if (curChar == 39 && kind > 21)
                     kind = 21;
                  break;
               case 27:
                   if ((0x3ff000000000000L & l) == 0L)
                      break;
                  if (kind > 23)
                     kind = 23;
                  jjAddStates(14, 15);
                  if (kind > 22)
                     kind = 22;
                  jjAddStates(11, 12);
                   break;
               case 28:
               case 23:
                   if (curChar == 46)
                     jjCheckNAdd(29);
                     jjCheckNAdd(24);
                   break;
               case 29:
               case 24:
                   if ((0x3ff000000000000L & l) == 0L)
                      break;
                  if (kind > 23)
                     kind = 23;
                  jjCheckNAdd(29);
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAdd(24);
                   break;
               case 30:
                  if (curChar == 42 && kind > 24)
                     kind = 24;
               case 25:
                  if (curChar == 42 && kind > 23)
                     kind = 23;
                   break;
               case 31:
               case 26:
                   if ((0xfbff54f8ffffd9ffL & l) == 0L)
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 34:
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
               case 29:
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 35:
               case 37:
               case 30:
               case 32:
                   if (curChar == 47)
                      jjCheckNAddStates(0, 2);
                   break;
               case 36:
               case 31:
                   if ((0xffff7fffffffffffL & l) != 0L)
                      jjCheckNAddStates(0, 2);
                   break;
               case 39:
                  if (curChar == 47 && kind > 26)
                     kind = 26;
               case 34:
                  if (curChar == 47 && kind > 25)
                     kind = 25;
                   break;
               case 40:
               case 35:
                   if (curChar == 33)
                     jjCheckNAddStates(16, 18);
                     jjCheckNAddStates(13, 15);
                   break;
               case 41:
               case 36:
                   if ((0x100002600L & l) != 0L)
                     jjCheckNAddTwoStates(41, 42);
                     jjCheckNAddTwoStates(36, 37);
                   break;
               case 42:
               case 37:
                   if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(19, 22);
                     jjCheckNAddStates(16, 19);
                   break;
               case 43:
               case 38:
                   if (curChar == 61)
                     jjCheckNAddStates(23, 28);
                     jjCheckNAddStates(20, 25);
                   break;
               case 44:
               case 39:
                   if (curChar == 34)
                     jjCheckNAddStates(29, 31);
                     jjCheckNAddStates(26, 28);
                   break;
               case 45:
               case 40:
                   if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(29, 31);
                     jjCheckNAddStates(26, 28);
                   break;
               case 47:
                  jjCheckNAddStates(29, 31);
               case 42:
                  jjCheckNAddStates(26, 28);
                   break;
               case 48:
               case 43:
                   if (curChar == 34)
                     jjCheckNAddStates(16, 18);
                     jjCheckNAddStates(13, 15);
                   break;
               case 51:
               case 46:
                   if ((0xfffffdfefffff9ffL & l) == 0L)
                      break;
                  if (kind > 29)
                     kind = 29;
                  jjstateSet[jjnewStateCnt++] = 51;
                  if (kind > 28)
                     kind = 28;
                  jjstateSet[jjnewStateCnt++] = 46;
                   break;
               case 52:
               case 47:
                   if (curChar == 39)
                     jjCheckNAddStates(32, 34);
                     jjCheckNAddStates(29, 31);
                   break;
               case 53:
               case 48:
                   if ((0xffffff7fffffffffL & l) != 0L)
                     jjCheckNAddStates(32, 34);
                     jjCheckNAddStates(29, 31);
                   break;
               case 55:
                  jjCheckNAddStates(32, 34);
               case 50:
                  jjCheckNAddStates(29, 31);
                   break;
               case 56:
               case 51:
                   if (curChar == 39)
                     jjCheckNAddStates(16, 18);
                     jjCheckNAddStates(13, 15);
                   break;
               case 57:
               case 52:
                   if ((0xfffffffeffffffffL & l) != 0L)
                     jjCheckNAddStates(35, 38);
                     jjCheckNAddStates(32, 35);
                   break;
               case 58:
               case 53:
                   if ((0x7bff50f8ffffd9ffL & l) == 0L)
                      break;
                  if (kind > 22)
                     kind = 22;
                  if (kind > 21)
                     kind = 21;
                   jjCheckNAddStates(6, 10);
                   break;
               case 59:
               case 54:
                   if ((0x7bfff8f8ffffd9ffL & l) == 0L)
                      break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 61:
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
               case 56:
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 62:
               case 57:
                   if ((0x7bfff8f8ffffd9ffL & l) != 0L)
                     jjCheckNAddStates(39, 41);
                     jjCheckNAddStates(36, 38);
                   break;
               case 64:
                  jjCheckNAddStates(39, 41);
               case 59:
                  jjCheckNAddStates(36, 38);
                   break;
                default : break;
             }
@@ -342,38 +325,38 @@ private int jjMoveNfa_2(int startState, int curPos)
          {
             switch(jjstateSet[--i])
             {
               case 66:
               case 61:
                   if ((0x97ffffff87ffffffL & l) != 0L)
                   {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                     if (kind > 24)
                        kind = 24;
                     jjCheckNAddTwoStates(27, 28);
                   }
                   else if (curChar == 92)
                     jjCheckNAddTwoStates(34, 34);
                     jjCheckNAddTwoStates(29, 29);
                   break;
                case 0:
                   if ((0x97ffffff87ffffffL & l) != 0L)
                   {
                     if (kind > 22)
                        kind = 22;
                     if (kind > 21)
                        kind = 21;
                      jjCheckNAddStates(6, 10);
                   }
                   else if (curChar == 92)
                     jjCheckNAddStates(42, 44);
                     jjCheckNAddStates(39, 41);
                   else if (curChar == 123)
                     jjstateSet[jjnewStateCnt++] = 40;
                     jjstateSet[jjnewStateCnt++] = 35;
                   else if (curChar == 126)
                   {
                     if (kind > 23)
                        kind = 23;
                     jjstateSet[jjnewStateCnt++] = 27;
                     if (kind > 22)
                        kind = 22;
                     jjstateSet[jjnewStateCnt++] = 22;
                   }
                   if ((0x97ffffff87ffffffL & l) != 0L)
                   {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                     if (kind > 24)
                        kind = 24;
                     jjCheckNAddTwoStates(27, 28);
                   }
                   if (curChar == 78)
                      jjstateSet[jjnewStateCnt++] = 11;
@@ -435,139 +418,128 @@ private int jjMoveNfa_2(int startState, int curPos)
                case 19:
                   jjCheckNAddStates(3, 5);
                   break;
               case 22:
                  if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(11, 13);
                  break;
               case 23:
                  if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 24;
                  break;
               case 24:
                  jjCheckNAddStates(11, 13);
                  break;
               case 26:
               case 21:
                   if (curChar != 126)
                      break;
                  if (kind > 23)
                     kind = 23;
                  jjstateSet[jjnewStateCnt++] = 27;
                  if (kind > 22)
                     kind = 22;
                  jjstateSet[jjnewStateCnt++] = 22;
                   break;
               case 31:
               case 26:
                   if ((0x97ffffff87ffffffL & l) == 0L)
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 32:
               case 27:
                   if ((0x97ffffff87ffffffL & l) == 0L)
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 33:
               case 28:
                   if (curChar == 92)
                     jjCheckNAddTwoStates(34, 34);
                     jjCheckNAddTwoStates(29, 29);
                   break;
               case 34:
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
               case 29:
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 36:
               case 31:
                   jjAddStates(0, 2);
                   break;
               case 38:
               case 33:
                   if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 37;
                     jjstateSet[jjnewStateCnt++] = 32;
                   break;
               case 42:
               case 37:
                   if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(19, 22);
                     jjCheckNAddStates(16, 19);
                   break;
               case 45:
               case 40:
                   if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(29, 31);
                     jjCheckNAddStates(26, 28);
                   break;
               case 46:
               case 41:
                   if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 47;
                     jjstateSet[jjnewStateCnt++] = 42;
                   break;
               case 47:
                  jjCheckNAddStates(29, 31);
               case 42:
                  jjCheckNAddStates(26, 28);
                   break;
               case 49:
               case 44:
                   if (curChar != 125)
                      break;
                  if (kind > 29)
                     kind = 29;
                  jjCheckNAddTwoStates(50, 51);
                  if (kind > 28)
                     kind = 28;
                  jjCheckNAddTwoStates(45, 46);
                   break;
               case 50:
               case 45:
                   if (curChar == 123)
                     jjstateSet[jjnewStateCnt++] = 40;
                     jjstateSet[jjnewStateCnt++] = 35;
                   break;
               case 51:
               case 46:
                   if ((0xf7ffffffbfffffffL & l) == 0L)
                      break;
                  if (kind > 29)
                     kind = 29;
                  jjCheckNAdd(51);
                  if (kind > 28)
                     kind = 28;
                  jjCheckNAdd(46);
                   break;
               case 53:
               case 48:
                   if ((0xffffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(32, 34);
                     jjCheckNAddStates(29, 31);
                   break;
               case 54:
               case 49:
                   if (curChar == 92)
                     jjstateSet[jjnewStateCnt++] = 55;
                     jjstateSet[jjnewStateCnt++] = 50;
                   break;
               case 55:
                  jjCheckNAddStates(32, 34);
               case 50:
                  jjCheckNAddStates(29, 31);
                   break;
               case 57:
               case 52:
                   if ((0xdfffffffffffffffL & l) != 0L)
                     jjCheckNAddStates(35, 38);
                     jjCheckNAddStates(32, 35);
                   break;
               case 58:
               case 53:
                   if ((0x97ffffff87ffffffL & l) == 0L)
                      break;
                  if (kind > 22)
                     kind = 22;
                  if (kind > 21)
                     kind = 21;
                   jjCheckNAddStates(6, 10);
                   break;
               case 59:
               case 54:
                   if ((0x97ffffff87ffffffL & l) == 0L)
                      break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 60:
               case 55:
                   if (curChar == 92)
                     jjCheckNAddTwoStates(61, 61);
                     jjCheckNAddTwoStates(56, 56);
                   break;
               case 61:
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
               case 56:
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 62:
               case 57:
                   if ((0x97ffffff87ffffffL & l) != 0L)
                     jjCheckNAddStates(39, 41);
                     jjCheckNAddStates(36, 38);
                   break;
               case 63:
               case 58:
                   if (curChar == 92)
                     jjCheckNAddTwoStates(64, 64);
                     jjCheckNAddTwoStates(59, 59);
                   break;
               case 64:
                  jjCheckNAddStates(39, 41);
               case 59:
                  jjCheckNAddStates(36, 38);
                   break;
               case 65:
               case 60:
                   if (curChar == 92)
                     jjCheckNAddStates(42, 44);
                     jjCheckNAddStates(39, 41);
                   break;
                default : break;
             }
@@ -584,13 +556,13 @@ private int jjMoveNfa_2(int startState, int curPos)
          {
             switch(jjstateSet[--i])
             {
               case 66:
               case 32:
               case 61:
               case 27:
                   if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
                case 0:
                   if (jjCanMove_0(hiByte, i1, i2, l1, l2))
@@ -600,14 +572,14 @@ private int jjMoveNfa_2(int startState, int curPos)
                   }
                   if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                   {
                     if (kind > 25)
                        kind = 25;
                     jjCheckNAddTwoStates(32, 33);
                     if (kind > 24)
                        kind = 24;
                     jjCheckNAddTwoStates(27, 28);
                   }
                   if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                   {
                     if (kind > 22)
                        kind = 22;
                     if (kind > 21)
                        kind = 21;
                      jjCheckNAddStates(6, 10);
                   }
                   break;
@@ -620,86 +592,81 @@ private int jjMoveNfa_2(int startState, int curPos)
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                      jjCheckNAddStates(3, 5);
                   break;
               case 22:
               case 24:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(11, 13);
                  break;
               case 31:
               case 26:
                   if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 34:
               case 29:
                   if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 25)
                     kind = 25;
                  jjCheckNAddTwoStates(32, 33);
                  if (kind > 24)
                     kind = 24;
                  jjCheckNAddTwoStates(27, 28);
                   break;
               case 36:
               case 31:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                      jjAddStates(0, 2);
                   break;
               case 41:
               case 36:
                   if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddTwoStates(41, 42);
                     jjCheckNAddTwoStates(36, 37);
                   break;
               case 42:
               case 37:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(19, 22);
                     jjCheckNAddStates(16, 19);
                   break;
               case 45:
               case 47:
               case 40:
               case 42:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(29, 31);
                     jjCheckNAddStates(26, 28);
                   break;
               case 51:
               case 46:
                   if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 29)
                     kind = 29;
                  jjstateSet[jjnewStateCnt++] = 51;
                  if (kind > 28)
                     kind = 28;
                  jjstateSet[jjnewStateCnt++] = 46;
                   break;
               case 53:
               case 55:
               case 48:
               case 50:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(32, 34);
                     jjCheckNAddStates(29, 31);
                   break;
               case 57:
               case 52:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(35, 38);
                     jjCheckNAddStates(32, 35);
                   break;
               case 58:
               case 53:
                   if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 22)
                     kind = 22;
                  if (kind > 21)
                     kind = 21;
                   jjCheckNAddStates(6, 10);
                   break;
               case 59:
               case 54:
                   if (!jjCanMove_2(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 61:
               case 56:
                   if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 22)
                     kind = 22;
                  jjCheckNAddTwoStates(59, 60);
                  if (kind > 21)
                     kind = 21;
                  jjCheckNAddTwoStates(54, 55);
                   break;
               case 62:
               case 57:
                   if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 41);
                     jjCheckNAddStates(36, 38);
                   break;
               case 64:
               case 59:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 41);
                     jjCheckNAddStates(36, 38);
                   break;
                default : break;
             }
@@ -712,7 +679,7 @@ private int jjMoveNfa_2(int startState, int curPos)
          kind = 0x7fffffff;
       }
       ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 66 - (jjnewStateCnt = startsAt)))
      if ((i = jjnewStateCnt) == (startsAt = 61 - (jjnewStateCnt = startsAt)))
          return curPos;
       try { curChar = input_stream.readChar(); }
       catch(java.io.IOException e) { return curPos; }
@@ -743,8 +710,8 @@ private int jjMoveNfa_0(int startState, int curPos)
                case 0:
                   if ((0x3ff000000000000L & l) != 0L)
                   {
                     if (kind > 30)
                        kind = 30;
                     if (kind > 29)
                        kind = 29;
                      jjCheckNAddTwoStates(1, 2);
                   }
                   else if (curChar == 45)
@@ -753,8 +720,8 @@ private int jjMoveNfa_0(int startState, int curPos)
                case 1:
                   if ((0x3ff000000000000L & l) == 0L)
                      break;
                  if (kind > 30)
                     kind = 30;
                  if (kind > 29)
                     kind = 29;
                   jjCheckNAddTwoStates(1, 2);
                   break;
                case 2:
@@ -764,8 +731,8 @@ private int jjMoveNfa_0(int startState, int curPos)
                case 3:
                   if ((0x3ff000000000000L & l) == 0L)
                      break;
                  if (kind > 30)
                     kind = 30;
                  if (kind > 29)
                     kind = 29;
                   jjCheckNAdd(3);
                   break;
                default : break;
@@ -816,9 +783,9 @@ private final int jjStopStringLiteralDfa_1(int pos, long active0)
    switch (pos)
    {
       case 0:
         if ((active0 & 0x80000000L) != 0L)
         if ((active0 & 0x40000000L) != 0L)
          {
            jjmatchedKind = 35;
            jjmatchedKind = 34;
             return 6;
          }
          return -1;
@@ -835,11 +802,11 @@ private int jjMoveStringLiteralDfa0_1()
    switch(curChar)
    {
       case 84:
         return jjMoveStringLiteralDfa1_1(0x80000000L);
         return jjMoveStringLiteralDfa1_1(0x40000000L);
       case 93:
         return jjStopAtPos(0, 32);
         return jjStopAtPos(0, 31);
       case 125:
         return jjStopAtPos(0, 33);
         return jjStopAtPos(0, 32);
       default :
          return jjMoveNfa_1(0, 0);
    }
@@ -854,8 +821,8 @@ private int jjMoveStringLiteralDfa1_1(long active0)
    switch(curChar)
    {
       case 79:
         if ((active0 & 0x80000000L) != 0L)
            return jjStartNfaWithStates_1(1, 31, 6);
         if ((active0 & 0x40000000L) != 0L)
            return jjStartNfaWithStates_1(1, 30, 6);
          break;
       default :
          break;
@@ -891,8 +858,8 @@ private int jjMoveNfa_1(int startState, int curPos)
                case 0:
                   if ((0xfffffffeffffffffL & l) != 0L)
                   {
                     if (kind > 35)
                        kind = 35;
                     if (kind > 34)
                        kind = 34;
                      jjCheckNAdd(6);
                   }
                   if ((0x100002600L & l) != 0L)
@@ -909,21 +876,21 @@ private int jjMoveNfa_1(int startState, int curPos)
                   break;
                case 2:
                   if ((0xfffffffbffffffffL & l) != 0L)
                     jjCheckNAddStates(45, 47);
                     jjCheckNAddStates(42, 44);
                   break;
                case 3:
                   if (curChar == 34)
                     jjCheckNAddStates(45, 47);
                     jjCheckNAddStates(42, 44);
                   break;
                case 5:
                  if (curChar == 34 && kind > 34)
                     kind = 34;
                  if (curChar == 34 && kind > 33)
                     kind = 33;
                   break;
                case 6:
                   if ((0xfffffffeffffffffL & l) == 0L)
                      break;
                  if (kind > 35)
                     kind = 35;
                  if (kind > 34)
                     kind = 34;
                   jjCheckNAdd(6);
                   break;
                default : break;
@@ -941,12 +908,12 @@ private int jjMoveNfa_1(int startState, int curPos)
                case 6:
                   if ((0xdfffffffdfffffffL & l) == 0L)
                      break;
                  if (kind > 35)
                     kind = 35;
                  if (kind > 34)
                     kind = 34;
                   jjCheckNAdd(6);
                   break;
                case 2:
                  jjAddStates(45, 47);
                  jjAddStates(42, 44);
                   break;
                case 4:
                   if (curChar == 92)
@@ -975,20 +942,20 @@ private int jjMoveNfa_1(int startState, int curPos)
                   }
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                   {
                     if (kind > 35)
                        kind = 35;
                     if (kind > 34)
                        kind = 34;
                      jjCheckNAdd(6);
                   }
                   break;
                case 2:
                   if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjAddStates(45, 47);
                     jjAddStates(42, 44);
                   break;
                case 6:
                   if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                      break;
                  if (kind > 35)
                     kind = 35;
                  if (kind > 34)
                     kind = 34;
                   jjCheckNAdd(6);
                   break;
                default : break;
@@ -1009,9 +976,9 @@ private int jjMoveNfa_1(int startState, int curPos)
    }
 }
 static final int[] jjnextStates = {
   36, 38, 39, 17, 18, 20, 59, 62, 30, 63, 60, 22, 23, 25, 27, 28, 
   41, 42, 49, 41, 42, 43, 49, 41, 42, 44, 52, 57, 49, 45, 46, 48, 
   53, 54, 56, 41, 42, 57, 49, 62, 30, 63, 61, 64, 34, 2, 4, 5, 
   31, 33, 34, 17, 18, 20, 54, 57, 25, 58, 55, 22, 23, 36, 37, 44, 
   36, 37, 38, 44, 36, 37, 39, 47, 52, 44, 40, 41, 43, 48, 49, 51, 
   36, 37, 52, 44, 57, 25, 58, 56, 59, 29, 2, 4, 5, 
 };
 private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)
 {
@@ -1054,7 +1021,7 @@ private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, lo
 public static final String[] jjstrLiteralImages = {
 "", null, null, null, null, null, null, null, null, null, null, null, "\53", 
 "\55", null, "\50", "\51", "\72", "\52", "\136", null, null, null, null, null, null, 
null, "\133", "\173", null, null, "\124\117", "\135", "\175", null, null, };
"\133", "\173", null, null, "\124\117", "\135", "\175", null, null, };
 
 /** Lexer state names. */
 public static final String[] lexStateNames = {
@@ -1066,17 +1033,17 @@ public static final String[] lexStateNames = {
 /** Lex State array. */
 public static final int[] jjnewLexState = {
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, 
   -1, -1, 1, 1, -1, 2, -1, 2, 2, -1, -1, 
   -1, 1, 1, -1, 2, -1, 2, 2, -1, -1, 
 };
 static final long[] jjtoToken = {
   0xffffffe01L, 
   0x7fffffe01L, 
 };
 static final long[] jjtoSkip = {
    0x100L, 
 };
 protected CharStream input_stream;
private final int[] jjrounds = new int[66];
private final int[] jjstateSet = new int[132];
private final int[] jjrounds = new int[61];
private final int[] jjstateSet = new int[122];
 protected char curChar;
 /** Constructor. */
 public QueryParserTokenManager(CharStream stream){
@@ -1101,7 +1068,7 @@ private void ReInitRounds()
 {
    int i;
    jjround = 0x80000001;
   for (i = 66; i-- > 0;)
   for (i = 61; i-- > 0;)
       jjrounds[i] = 0x80000000;
 }
 
- 
2.19.1.windows.1

