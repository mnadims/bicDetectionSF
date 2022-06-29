From e584e3a060103fa02d9e168f86a54e62170cce7a Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Fri, 15 Aug 2008 06:48:32 +0000
Subject: [PATCH] SOLR-701: be explicit about Locale when parsing/formating
 milliseconds.  Also: refactor usages of TimeZone and Locale so it's clear
 when UTC and Locale.US are used for various purposes in case someone
 considers modifying them.

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@686159 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  4 +--
 .../org/apache/solr/schema/DateField.java     | 36 ++++++++++++++-----
 2 files changed, 30 insertions(+), 10 deletions(-)

diff --git a/CHANGES.txt b/CHANGES.txt
index 422e2581681..dc26a50432b 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -473,11 +473,11 @@ Bug Fixes
 
 28. SOLR-509: Moved firstSearcher event notification to the end of the SolrCore constructor (Koji Sekiguchi via gsingers)
 
29. SOLR-470, SOLR-552, and SOLR-544: Multiple fixes to DateField
29. SOLR-470, SOLR-552, SOLR-544, SOLR-701: Multiple fixes to DateField
     regarding lenient parsing of optional milliseconds, and correct
     formating using the canonical representation.  LegacyDateField has
     been added for people who have come to depend on the existing
    broken behavior. (hossman)
    broken behavior. (hossman, Stefan Oestreicher)
 
 30. SOLR-539: Fix for non-atomic long counters and a cast fix to avoid divide
     by zero. (Sean Timm via Otis Gospodnetic)
diff --git a/src/java/org/apache/solr/schema/DateField.java b/src/java/org/apache/solr/schema/DateField.java
index 645b15fd423..5d120529a85 100644
-- a/src/java/org/apache/solr/schema/DateField.java
++ b/src/java/org/apache/solr/schema/DateField.java
@@ -31,6 +31,7 @@ import java.io.IOException;
 import java.util.Date;
 import java.util.TimeZone;
 import java.util.Locale;
import java.text.DecimalFormatSymbols;
 import java.text.SimpleDateFormat;
 import java.text.DateFormat;
 import java.text.NumberFormat;
@@ -103,6 +104,24 @@ import java.text.FieldPosition;
 public class DateField extends FieldType {
 
   public static TimeZone UTC = TimeZone.getTimeZone("UTC");

  /* :TODO: let Locale/TimeZone come from init args for rounding only */

  /** TimeZone for DateMath (UTC) */
  protected static final TimeZone MATH_TZ = UTC;
  /** Locale for DateMath (Locale.US) */
  protected static final Locale MATH_LOCALE = Locale.US;

  /** 
   * Fixed TimeZone (UTC) needed for parsing/formating Dates in the 
   * canonical representation.
   */
  protected static final TimeZone CANONICAL_TZ = UTC;
  /** 
   * Fixed Locale needed for parsing/formating Milliseconds in the 
   * canonical representation.
   */
  protected static final Locale CANONICAL_LOCALE = Locale.US;
   
   // The XML (external) date format will sort correctly, except if
   // fractions of seconds are present (because '.' is lower than 'Z').
@@ -127,8 +146,7 @@ public class DateField extends FieldType {
    */
   public Date parseMath(Date now, String val) {
     String math = null;
    /* :TODO: let Locale/TimeZone come from init args for rounding only */
    final DateMathParser p = new DateMathParser(UTC, Locale.US);
    final DateMathParser p = new DateMathParser(MATH_TZ, MATH_LOCALE);
     
     if (null != now) p.setNow(now);
     
@@ -243,13 +261,14 @@ public class DateField extends FieldType {
   private static class ISO8601CanonicalDateFormat extends SimpleDateFormat {
     
     protected NumberFormat millisParser
      = NumberFormat.getIntegerInstance(Locale.US);
      = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);
 
    protected NumberFormat millisFormat = new DecimalFormat(".###");
    protected NumberFormat millisFormat = new DecimalFormat(".###", 
      new DecimalFormatSymbols(CANONICAL_LOCALE));
 
     public ISO8601CanonicalDateFormat() {
      super("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
      this.setTimeZone(UTC);
      super("yyyy-MM-dd'T'HH:mm:ss", CANONICAL_LOCALE);
      this.setTimeZone(CANONICAL_TZ);
     }
 
     public Date parse(String i, ParsePosition p) {
@@ -294,8 +313,9 @@ public class DateField extends FieldType {
     public Object clone() {
       ISO8601CanonicalDateFormat c
         = (ISO8601CanonicalDateFormat) super.clone();
      c.millisParser = NumberFormat.getIntegerInstance(Locale.US);
      c.millisFormat = new DecimalFormat(".###");
      c.millisParser = NumberFormat.getIntegerInstance(CANONICAL_LOCALE);
      c.millisFormat = new DecimalFormat(".###", 
        new DecimalFormatSymbols(CANONICAL_LOCALE));
       return c;
     }
   }
- 
2.19.1.windows.1

