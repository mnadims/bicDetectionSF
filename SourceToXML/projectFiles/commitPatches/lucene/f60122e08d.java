From f60122e08d23fc641410b80bd6debed6f8a62538 Mon Sep 17 00:00:00 2001
From: "Chris M. Hostetter" <hossman@apache.org>
Date: Mon, 19 May 2008 22:00:22 +0000
Subject: [PATCH] SOLR-470, SOLR-552, and SOLR-544: Multiple fixes to DateField
 regarding lenient parsing of optional milliseconds, and correct formating
 using the canonical representation.  LegacyDateField has been added for
 people who have come to depend on the existing broken behavior.

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@658003 13f79535-47bb-0310-9956-ffa450edef68
--
 CHANGES.txt                                   |  21 +++
 .../org/apache/solr/schema/DateField.java     | 158 ++++++++++++++----
 .../apache/solr/schema/LegacyDateField.java   | 117 +++++++++++++
 .../apache/solr/BasicFunctionalityTest.java   |  72 ++++----
 .../org/apache/solr/schema/DateFieldTest.java | 110 ++++++++++++
 .../solr/schema/LegacyDateFieldTest.java      | 105 ++++++++++++
 6 files changed, 515 insertions(+), 68 deletions(-)
 create mode 100644 src/java/org/apache/solr/schema/LegacyDateField.java
 create mode 100644 src/test/org/apache/solr/schema/DateFieldTest.java
 create mode 100644 src/test/org/apache/solr/schema/LegacyDateFieldTest.java

diff --git a/CHANGES.txt b/CHANGES.txt
index 418d2e6a5e1..09003279818 100644
-- a/CHANGES.txt
++ b/CHANGES.txt
@@ -48,7 +48,22 @@ option can be added to your solrconfig.xml.  See the wiki (or the
 example solrconfig.xml) for more details...  
    http://wiki.apache.org/solr/SolrConfigXml#HTTPCaching
 
In Solr 1.2, DateField did not enforce the canonical representation of
the ISO 8601 format when parsing incoming data, and did not generation
the canonical format when generating dates from "Date Math" strings
(particularly as it pertains to milliseconds ending in trailing zeros)
-- As a result equivalent dates could not always be compared properly.
This problem is corrected in Solr 1.3, but DateField users that might
have been affected by indexing inconsistent formats of equivilent
dates (ie: 1995-12-31T23:59:59Z vs 1995-12-31T23:59:59.000Z) may want
to consider reindexing to correct these inconsistencies.  Users who
depend on some of the the "broken" behavior of DateField in Solr 1.2
(specificly: accepting any input that ends in a 'Z') should consider
using the LegacyDateField class as a possible alternative.  Users that
desire 100% backwards compatibility should consider using the Solr 1.2
version of DateField.
 
   
 Detailed Change List
 --------------------
 
@@ -383,6 +398,12 @@ Bug Fixes
 
 28. SOLR-509: Moved firstSearcher event notification to the end of the SolrCore constructor (Koji Sekiguchi via gsingers)
 
29. SOLR-470, SOLR-552, and SOLR-544: Multiple fixes to DateField
    regarding lenient parsing of optional milliseconds, and correct
    formating using the canonical representation.  LegacyDateField has
    been added for people who have come to depend on the existing
    broken behavior. (hossman)

 Other Changes
  1. SOLR-135: Moved common classes to org.apache.solr.common and altered the
     build scripts to make two jars: apache-solr-1.3.jar and 
diff --git a/src/java/org/apache/solr/schema/DateField.java b/src/java/org/apache/solr/schema/DateField.java
index 91d38e7d235..645b15fd423 100644
-- a/src/java/org/apache/solr/schema/DateField.java
++ b/src/java/org/apache/solr/schema/DateField.java
@@ -33,7 +33,11 @@ import java.util.TimeZone;
 import java.util.Locale;
 import java.text.SimpleDateFormat;
 import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
 import java.text.ParseException;
import java.text.FieldPosition;
 
 // TODO: make a FlexibleDateField that can accept dates in multiple
 // formats, better for human entered dates.
@@ -42,21 +46,45 @@ import java.text.ParseException;
 
 
 /**
 * FieldType that can represent any Date/Time with millisecond precisison.
 * FieldType that can represent any Date/Time with millisecond precision.
  * <p>
  * Date Format for the XML, incoming and outgoing:
  * </p>
  * <blockquote>
  * A date field shall be of the form 1995-12-31T23:59:59Z
 * The trailing "Z" designates UTC time and is mandatory.
 * Optional fractional seconds are allowed: 1995-12-31T23:59:59.999Z
 * The trailing "Z" designates UTC time and is mandatory
 * (See below for an explanation of UTC).
 * Optional fractional seconds are allowed, as long as they do not end
 * in a trailing 0 (but any precision beyond milliseconds will be ignored).
  * All other parts are mandatory.
  * </blockquote>
  * <p>
  * This format was derived to be standards compliant (ISO 8601) and is a more
 * restricted form of the canonical representation of dateTime from XML
 * schema part 2.
 * http://www.w3.org/TR/xmlschema-2/#dateTime
 * restricted form of the
 * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime-canonical-representation">canonical
 * representation of dateTime</a> from XML schema part 2.  Examples...
 * </p>
 * <ul>
 *   <li>1995-12-31T23:59:59Z</li>
 *   <li>1995-12-31T23:59:59.9Z</li>
 *   <li>1995-12-31T23:59:59.99Z</li>
 *   <li>1995-12-31T23:59:59.999Z</li>
 * </ul>
 * <p>
 * Note that DateField is lenient with regards to parsing fractional
 * seconds that end in trailing zeros and will ensure that those values
 * are indexed in the correct canonical format.
 * </p>
 * <p>
 * This FieldType also supports incoming "Date Math" strings for computing
 * values by adding/rounding internals of time relative either an explicit
 * datetime (in the format specified above) or the literal string "NOW",
 * ie: "NOW+1YEAR", "NOW/DAY", "1995-12-31T23:59:59.999Z+5MINUTES", etc...
 * -- see {@link DateMathParser} for more examples.
 * </p>
 *
 * <p>
 * Explanation of "UTC"...
  * </p>
  * <blockquote>
  * "In 1970 the Coordinated Universal Time system was devised by an
@@ -68,14 +96,6 @@ import java.text.ParseException;
  * acronym UTC was chosen as a compromise."
  * </blockquote>
  *
 * <p>
 * This FieldType also supports incoming "Date Math" strings for computing
 * values by adding/rounding internals of time relative either an explicit
 * datetime (in theformat specified above) or the literal string "NOW",
 * ie: "NOW+1YEAR", "NOW/DAY", 1995-12-31T23:59:59.999Z+5MINUTES, etc...
 * -- see {@link DateMathParser} for more examples.
 * </p>
 *
  * @version $Id$
  * @see <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">XML schema part 2</a>
  *
@@ -96,12 +116,6 @@ public class DateField extends FieldType {
   protected static char Z = 'Z';
   
   public String toInternal(String val) {
    final int len=val.length();
    if (val.charAt(len-1) == Z) {
      // check common case first, simple datetime
      // NOTE: not parsed to ensure correctness
      return val.substring(0,len-1);
    }
     return toInternal(parseMath(null, val));
   }
 
@@ -150,7 +164,7 @@ public class DateField extends FieldType {
   }
   
   public String toInternal(Date val) {
    return getThreadLocalDateFormat().format(val);
    return formatDate(val);
   }
 
   public String indexedToReadable(String indexedForm) {
@@ -161,13 +175,13 @@ public class DateField extends FieldType {
     return indexedToReadable(f.stringValue());
   }
   public Date toObject(String indexedForm) throws java.text.ParseException {
    return getThreadLocalDateFormat().parse(indexedToReadable(indexedForm));
    return parseDate(indexedToReadable(indexedForm));
   }
 
   @Override
   public Date toObject(Fieldable f) {
     try {
      return getThreadLocalDateFormat().parse( toExternal(f) );
      return parseDate( toExternal(f) );
     }
     catch( ParseException ex ) {
       throw new RuntimeException( ex );
@@ -193,25 +207,105 @@ public class DateField extends FieldType {
   /**
    * Returns a formatter that can be use by the current thread if needed to
    * convert Date objects to the Internal representation.
   *
   * Only the <tt>format(Date)</tt> can be used safely.
   * 
   * @deprecated - use formatDate(Date) instead
    */
   protected DateFormat getThreadLocalDateFormat() {
  
     return fmtThreadLocal.get();
   }
 
  private static ThreadLocalDateFormat fmtThreadLocal
    = new ThreadLocalDateFormat();
  /**
   * Thread safe method that can be used by subclasses to format a Date
   * using the Internal representation.
   */
  protected String formatDate(Date d) {
    return fmtThreadLocal.get().format(d);
  }

  /**
   * Thread safe method that can be used by subclasses to parse a Date
   * that is already in the internal representation
   */
   protected Date parseDate(String s) throws ParseException {
     return fmtThreadLocal.get().parse(s);
   }
  
  /**
   * Thread safe DateFormat that can <b>format</b> in the canonical
   * ISO8601 date format, not including the trailing "Z" (since it is
   * left off in the internal indexed values)
   */
  private final static ThreadLocalDateFormat fmtThreadLocal
    = new ThreadLocalDateFormat(new ISO8601CanonicalDateFormat());
  
  private static class ISO8601CanonicalDateFormat extends SimpleDateFormat {
    
    protected NumberFormat millisParser
      = NumberFormat.getIntegerInstance(Locale.US);

    protected NumberFormat millisFormat = new DecimalFormat(".###");

    public ISO8601CanonicalDateFormat() {
      super("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
      this.setTimeZone(UTC);
    }

    public Date parse(String i, ParsePosition p) {
      /* delegate to SimpleDateFormat for easy stuff */
      Date d = super.parse(i, p);
      int milliIndex = p.getIndex();
      /* worry aboutthe milliseconds ourselves */
      if (null != d &&
          -1 == p.getErrorIndex() &&
          milliIndex + 1 < i.length() &&
          '.' == i.charAt(milliIndex)) {
        p.setIndex( ++milliIndex ); // NOTE: ++ to chomp '.'
        Number millis = millisParser.parse(i, p);
        if (-1 == p.getErrorIndex()) {
          int endIndex = p.getIndex();
            d = new Date(d.getTime()
                         + (long)(millis.doubleValue() *
                                  Math.pow(10, (3-endIndex+milliIndex))));
        }
      }
      return d;
    }

    public StringBuffer format(Date d, StringBuffer toAppendTo,
                               FieldPosition pos) {
      /* delegate to SimpleDateFormat for easy stuff */
      super.format(d, toAppendTo, pos);
      /* worry aboutthe milliseconds ourselves */
      long millis = d.getTime() % 1000l;
      if (0l == millis) {
        return toAppendTo;
      }
      int posBegin = toAppendTo.length();
      toAppendTo.append(millisFormat.format(millis / 1000d));
      if (DateFormat.MILLISECOND_FIELD == pos.getField()) {
        pos.setBeginIndex(posBegin);
        pos.setEndIndex(toAppendTo.length());
      }
      return toAppendTo;
    }

    public Object clone() {
      ISO8601CanonicalDateFormat c
        = (ISO8601CanonicalDateFormat) super.clone();
      c.millisParser = NumberFormat.getIntegerInstance(Locale.US);
      c.millisFormat = new DecimalFormat(".###");
      return c;
    }
  }
   
   private static class ThreadLocalDateFormat extends ThreadLocal<DateFormat> {
     DateFormat proto;
    public ThreadLocalDateFormat() {
    public ThreadLocalDateFormat(DateFormat d) {
       super();
      SimpleDateFormat tmp =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
      tmp.setTimeZone(UTC);
      proto = tmp;
      proto = d;
     }
    
     protected DateFormat initialValue() {
       return (DateFormat) proto.clone();
     }
diff --git a/src/java/org/apache/solr/schema/LegacyDateField.java b/src/java/org/apache/solr/schema/LegacyDateField.java
new file mode 100644
index 00000000000..8a6364fba33
-- /dev/null
++ b/src/java/org/apache/solr/schema/LegacyDateField.java
@@ -0,0 +1,117 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.schema;

import org.apache.solr.common.SolrException;
import org.apache.solr.request.XMLWriter;
import org.apache.solr.request.TextResponseWriter;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.SortField;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.OrdFieldSource;
import org.apache.solr.util.DateMathParser;
  
import java.util.Map;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.ParseException;

/**
 * This class is <b>NOT</b> recommended for new users and should be
 * considered <b>UNSUPPORTED</b>.
 * <p>
 * In Solr 1.2, <tt>DateField</tt> did not enforce
 * the canonical representation of the ISO 8601 format when parsing
 * incoming data, and did not generation the canonical format when
 * generating dates from "Date Math" strings (particularly as
 * it pertains to milliseconds ending in trailing zeros) -- As a result
 * equivalent dates could not always be compared properly.
 * </p>
 * <p>
 * This class is provided as possible alternative for people who depend on
 * the "broken" behavior of DateField in Solr 1.2
 * (specificly: accepting any input that ends in a 'Z', and
 * formating DateMath expressions using 3 decimals of milliseconds) while
 * still supporting some newer functionality of DateField (ie: DateMath on
 * explicit strings in addition to "NOW")
 * </p>
 * <p>
 * Users that desire 100% backwards compatibility should consider using
 * the Solr 1.2 version of <tt>DateField</tt>
 * </p>
 *
 * @see <a href="https://issues.apache.org/jira/browse/SOLR-552">SOLR-552</a>
 * @see <a href="https://issues.apache.org/jira/browse/SOLR-470">SOLR-470</a>
 * @see <a href="https://issues.apache.org/jira/browse/SOLR-521">SOLR-521</a>
 * @deprecated use {@link DateField}
 */
@Deprecated
public final class LegacyDateField extends DateField {

  /**
   * Overrides the super class to short circut and do no enforcing of
   * the canonical format
   */
  public String toInternal(String val) {
    final int len=val.length();
    if (val.charAt(len-1) == Z) {
      // check common case first, simple datetime
      // NOTE: not parsed to ensure correctness
      return val.substring(0,len-1);
    }
    return toInternal(parseMath(null, val));
  }
  
  /**
   * This method returns a DateFormat which does <b>NOT</b> respect the
   * ISO 8601 canonical format with regards to trailing zeros in milliseconds,
   * instead if always formats milliseconds to 3 decimal points.
   */
  protected DateFormat getThreadLocalDateFormat() {
    return fmtThreadLocal.get();
  }

  protected String formatDate(Date d) {
    return getThreadLocalDateFormat().format(d);
  }

  private static ThreadLocalDateFormat fmtThreadLocal
    = new ThreadLocalDateFormat();

  private static class ThreadLocalDateFormat extends ThreadLocal<DateFormat> {
    DateFormat proto;
    public ThreadLocalDateFormat() {
      super();
      SimpleDateFormat tmp =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
      tmp.setTimeZone(UTC);
      proto = tmp;
    }

    protected DateFormat initialValue() {
      return (DateFormat) proto.clone();
    }
  }
  
}
diff --git a/src/test/org/apache/solr/BasicFunctionalityTest.java b/src/test/org/apache/solr/BasicFunctionalityTest.java
index 3eaae0708a5..61bcc474f1b 100644
-- a/src/test/org/apache/solr/BasicFunctionalityTest.java
++ b/src/test/org/apache/solr/BasicFunctionalityTest.java
@@ -656,38 +656,38 @@ public class BasicFunctionalityTest extends AbstractSolrTestCase {
                 )
             // 31 days + pre+post+inner = 34
             ,"*[count("+pre+"/int)=34]"
            ,pre+"/int[@name='1976-07-01T00:00:00.000Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-02T00:00:00.000Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-03T00:00:00.000Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-01T00:00:00Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-02T00:00:00Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-03T00:00:00Z'][.='2'  ]"
             // july4th = 2 because exists doc @ 00:00:00.000 on July5
             // (date faceting is inclusive)
            ,pre+"/int[@name='1976-07-04T00:00:00.000Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-05T00:00:00.000Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-07T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-08T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-09T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-10T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-11T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-12T00:00:00.000Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-13T00:00:00.000Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-14T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-15T00:00:00.000Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-16T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-17T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-18T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-19T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-21T00:00:00.000Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-22T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-23T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-24T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-25T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-26T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-27T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-28T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-29T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-30T00:00:00.000Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-31T00:00:00.000Z'][.='0']"
            ,pre+"/int[@name='1976-07-04T00:00:00Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-05T00:00:00Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-07T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-08T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-09T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-10T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-11T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-12T00:00:00Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-13T00:00:00Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-14T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-15T00:00:00Z'][.='2'  ]"
            ,pre+"/int[@name='1976-07-16T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-17T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-18T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-19T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-21T00:00:00Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-22T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-23T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-24T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-25T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-26T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-27T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-28T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-29T00:00:00Z'][.='0']"
            ,pre+"/int[@name='1976-07-30T00:00:00Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-31T00:00:00Z'][.='0']"
             
             ,pre+"/int[@name='before' ][.='2']"
             ,pre+"/int[@name='after'  ][.='1']"
@@ -708,9 +708,9 @@ public class BasicFunctionalityTest extends AbstractSolrTestCase {
                 )
             // 3 gaps + pre+post+inner = 6
             ,"*[count("+pre+"/int)=6]"
            ,pre+"/int[@name='1976-07-01T00:00:00.000Z'][.='5'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00.000Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-11T00:00:00.000Z'][.='4'  ]"
            ,pre+"/int[@name='1976-07-01T00:00:00Z'][.='5'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-11T00:00:00Z'][.='4'  ]"
             
             ,pre+"/int[@name='before' ][.='2']"
             ,pre+"/int[@name='after'  ][.='3']"
@@ -730,9 +730,9 @@ public class BasicFunctionalityTest extends AbstractSolrTestCase {
                 )
             // 3 gaps + pre+post+inner = 6
             ,"*[count("+pre+"/int)=6]"
            ,pre+"/int[@name='1976-07-01T00:00:00.000Z'][.='5'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00.000Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-11T00:00:00.000Z'][.='1'  ]"
            ,pre+"/int[@name='1976-07-01T00:00:00Z'][.='5'  ]"
            ,pre+"/int[@name='1976-07-06T00:00:00Z'][.='0'  ]"
            ,pre+"/int[@name='1976-07-11T00:00:00Z'][.='1'  ]"
             
             ,pre+"/int[@name='before' ][.='2']"
             ,pre+"/int[@name='after'  ][.='6']"
diff --git a/src/test/org/apache/solr/schema/DateFieldTest.java b/src/test/org/apache/solr/schema/DateFieldTest.java
new file mode 100644
index 00000000000..785db52fedc
-- /dev/null
++ b/src/test/org/apache/solr/schema/DateFieldTest.java
@@ -0,0 +1,110 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.schema;

import org.apache.solr.schema.DateField;
import org.apache.solr.util.DateMathParser;
import org.apache.lucene.document.Fieldable;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.DateFormat;

import junit.framework.TestCase;

public class DateFieldTest extends LegacyDateFieldTest {
  
  public void setUp()  throws Exception {
    super.setUp();
    f = new DateField();
  }
  
  public void testToInternal() throws Exception {
    assertToI("1995-12-31T23:59:59.999", "1995-12-31T23:59:59.999666Z");
    assertToI("1995-12-31T23:59:59.999", "1995-12-31T23:59:59.999Z");
    assertToI("1995-12-31T23:59:59.99",  "1995-12-31T23:59:59.99Z");
    assertToI("1995-12-31T23:59:59.9",   "1995-12-31T23:59:59.9Z");
    assertToI("1995-12-31T23:59:59",     "1995-12-31T23:59:59Z");

    // here the input isn't in the canonical form, but we should be forgiving
    assertToI("1995-12-31T23:59:59.99",  "1995-12-31T23:59:59.990Z");
    assertToI("1995-12-31T23:59:59.9",   "1995-12-31T23:59:59.900Z");
    assertToI("1995-12-31T23:59:59.9",   "1995-12-31T23:59:59.90Z");
    assertToI("1995-12-31T23:59:59",     "1995-12-31T23:59:59.000Z");
    assertToI("1995-12-31T23:59:59",     "1995-12-31T23:59:59.00Z");
    assertToI("1995-12-31T23:59:59",     "1995-12-31T23:59:59.0Z");
    
    // kind of kludgy, but we have other tests for the actual date math
    assertToI(f.toInternal(p.parseMath("/DAY")), "NOW/DAY");

    // as of Solr 1.3
    assertToI("1995-12-31T00:00:00", "1995-12-31T23:59:59Z/DAY");
    assertToI("1995-12-31T00:00:00", "1995-12-31T23:59:59.123Z/DAY");
    assertToI("1995-12-31T00:00:00", "1995-12-31T23:59:59.123999Z/DAY");
  }
  
  public void testToInternalObj() throws Exception {
    assertToI("1995-12-31T23:59:59.999", 820454399999l);
    assertToI("1995-12-31T23:59:59.99",  820454399990l);
    assertToI("1995-12-31T23:59:59.9",   820454399900l);
    assertToI("1995-12-31T23:59:59",     820454399000l);
  }
    
  public void assertParseMath(long expected, String input) {
    Date d = new Date(0);
    assertEquals("Input: "+input, expected, f.parseMath(d, input).getTime());
  }
  
  // as of Solr1.3
  public void testParseMath() {
    assertParseMath(820454699999l, "1995-12-31T23:59:59.999765Z+5MINUTES");
    assertParseMath(820454699999l, "1995-12-31T23:59:59.999Z+5MINUTES");
    assertParseMath(820454699990l, "1995-12-31T23:59:59.99Z+5MINUTES");
    assertParseMath(194918400000l, "1976-03-06T03:06:00Z/DAY");
    
    // here the input isn't in the canonical form, but we should be forgiving
    assertParseMath(820454699990l, "1995-12-31T23:59:59.990Z+5MINUTES");
    assertParseMath(194918400000l, "1976-03-06T03:06:00.0Z/DAY");
    assertParseMath(194918400000l, "1976-03-06T03:06:00.00Z/DAY");
    assertParseMath(194918400000l, "1976-03-06T03:06:00.000Z/DAY");
  }

  public void assertToObject(long expected, String input) throws Exception {
    assertEquals("Input: "+input, expected, f.toObject(input).getTime());
  }
  
  // as of Solr1.3
  public void testToObject() throws Exception {
    assertToObject(820454399987l, "1995-12-31T23:59:59.987666Z");
    assertToObject(820454399987l, "1995-12-31T23:59:59.987Z");
    assertToObject(820454399980l, "1995-12-31T23:59:59.98Z");
    assertToObject(820454399900l, "1995-12-31T23:59:59.9Z");
    assertToObject(820454399000l, "1995-12-31T23:59:59Z");
  }
  
  public void testFormatter() {
    DateFormat fmt = f.getThreadLocalDateFormat();
    assertEquals("1970-01-01T00:00:00.005", fmt.format(new Date(5)));
    assertEquals("1970-01-01T00:00:00",     fmt.format(new Date(0)));
    assertEquals("1970-01-01T00:00:00.37",  fmt.format(new Date(370)));
    assertEquals("1970-01-01T00:00:00.9",   fmt.format(new Date(900)));

  }

}
diff --git a/src/test/org/apache/solr/schema/LegacyDateFieldTest.java b/src/test/org/apache/solr/schema/LegacyDateFieldTest.java
new file mode 100644
index 00000000000..201db568c19
-- /dev/null
++ b/src/test/org/apache/solr/schema/LegacyDateFieldTest.java
@@ -0,0 +1,105 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.schema;

import org.apache.solr.schema.DateField;
import org.apache.solr.util.DateMathParser;
import org.apache.lucene.document.Fieldable;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.DateFormat;

import junit.framework.TestCase;

public class LegacyDateFieldTest extends TestCase {
  // if and when this class is removed, make sure to refactor all
  // appropriate code to DateFieldTest

  public static TimeZone UTC = TimeZone.getTimeZone("UTC");
  protected DateField f = null;
  protected DateMathParser p = null;
  
  public void setUp() throws Exception {
    super.setUp();
    p = new DateMathParser(UTC, Locale.US);
    f = new DateField();
    // so test can be run against Solr 1.2...
    try {
      Class clazz = Class.forName("org.apache.solr.schema.LegacyDateField");
      f = (DateField) clazz.newInstance();
    } catch (ClassNotFoundException ignored) {
      // NOOP
    }
  }
  
  public void assertToI(String expected, String input) {
    assertEquals("Input: " + input, expected, f.toInternal(input));
  }

  public void testToInternal() throws Exception {
    assertToI("1995-12-31T23:59:59.999", "1995-12-31T23:59:59.999Z");
    assertToI("1995-12-31T23:59:59.99",  "1995-12-31T23:59:59.99Z");
    assertToI("1995-12-31T23:59:59.9",   "1995-12-31T23:59:59.9Z");
    assertToI("1995-12-31T23:59:59",     "1995-12-31T23:59:59Z");

    // this is the broken behavior
    assertToI("1995-12-31T23:59:59.9998", "1995-12-31T23:59:59.9998Z");
    assertToI("1995-12-31T23:59:59.9990", "1995-12-31T23:59:59.9990Z");
    assertToI("1995-12-31T23:59:59.990",  "1995-12-31T23:59:59.990Z");
    assertToI("1995-12-31T23:59:59.900",  "1995-12-31T23:59:59.900Z");
    assertToI("1995-12-31T23:59:59.90",   "1995-12-31T23:59:59.90Z");
    assertToI("1995-12-31T23:59:59.000",  "1995-12-31T23:59:59.000Z");
    assertToI("1995-12-31T23:59:59.00",   "1995-12-31T23:59:59.00Z");
    assertToI("1995-12-31T23:59:59.0",    "1995-12-31T23:59:59.0Z");
    
  }
    
  public void assertToI(String expected, long input) {
    assertEquals("Input: " + input, expected, f.toInternal(new Date(input)));
  }

  public void testToInternalObj() throws Exception {
    assertToI("1995-12-31T23:59:59.999", 820454399999l);
    
    // this is the broken behavior
    assertToI("1995-12-31T23:59:59.990",  820454399990l);
    assertToI("1995-12-31T23:59:59.900",  820454399900l);
    assertToI("1995-12-31T23:59:59.000",  820454399000l);
  }
  
  public void assertItoR(String expected, String input) {
    assertEquals("Input: " + input, expected, f.indexedToReadable(input));
  }

  public void testIndexedToReadable() {
    assertItoR("1995-12-31T23:59:59.999Z", "1995-12-31T23:59:59.999");
    assertItoR("1995-12-31T23:59:59.99Z",  "1995-12-31T23:59:59.99");
    assertItoR("1995-12-31T23:59:59.9Z",   "1995-12-31T23:59:59.9");
    assertItoR("1995-12-31T23:59:59Z",     "1995-12-31T23:59:59");
  }
  public void testFormatter() {
    DateFormat fmt = f.getThreadLocalDateFormat();
    assertEquals("1970-01-01T00:00:00.005", fmt.format(new Date(5)));
    // all of this is broken behavior
    assertEquals("1970-01-01T00:00:00.000", fmt.format(new Date(0)));
    assertEquals("1970-01-01T00:00:00.370", fmt.format(new Date(370)));
    assertEquals("1970-01-01T00:00:00.900", fmt.format(new Date(900)));
  }
}
- 
2.19.1.windows.1

