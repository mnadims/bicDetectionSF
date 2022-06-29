From 3d1fe2877b654f0a9a80df16ce7ed04314426e2a Mon Sep 17 00:00:00 2001
From: Andras Piros <andras.piros@cloudera.com>
Date: Mon, 6 Aug 2018 18:17:55 +0200
Subject: [PATCH] OOZIE-3315 DateList example fails (daniel.becker via
 andras.piros)

--
 .../org/apache/oozie/example/DateList.java    | 70 +++++++++++----
 .../apache/oozie/example/TestDateList.java    | 88 +++++++++++++++++++
 release-log.txt                               |  1 +
 3 files changed, 142 insertions(+), 17 deletions(-)
 create mode 100644 examples/src/test/java/org/apache/oozie/example/TestDateList.java

diff --git a/examples/src/main/java/org/apache/oozie/example/DateList.java b/examples/src/main/java/org/apache/oozie/example/DateList.java
index 731fe4130..bed809391 100644
-- a/examples/src/main/java/org/apache/oozie/example/DateList.java
++ b/examples/src/main/java/org/apache/oozie/example/DateList.java
@@ -20,11 +20,14 @@ package org.apache.oozie.example;
 
 import java.io.File;
 import java.io.FileOutputStream;
import java.io.IOException;
 import java.io.OutputStream;
 import java.text.DateFormat;
import java.text.ParseException;
 import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
 import java.util.Date;
import java.util.List;
 import java.util.Properties;
 import java.util.TimeZone;
 
@@ -32,41 +35,74 @@ public class DateList {
     private static final TimeZone UTC = getTimeZone("UTC");
     private static String DATE_LIST_SEPARATOR = ",";
 
    public static void main(String[] args) throws Exception {
    public static void main(String[] args) throws IOException, ParseException {
        if (!checkArgsOk(args)) {
            System.exit(1);
        }

        String dateList = createDateListFromArgs(args);

        System.out.println("datelist :" + dateList+ ":");
        writeWorkflowOutput(dateList);
    }

    private static boolean checkArgsOk(final String[] args) {
         if (args.length < 5) {
             System.out
                     .println("Usage: java DateList <start_time>  <end_time> <frequency> <timeunit> <timezone>");
             System.out
                     .println("Example: java DateList 2009-02-01T01:00Z 2009-02-01T02:00Z 15 MINUTES UTC");
            System.exit(1);
            return false;
         }
        Date startTime = parseDateUTC(args[0]);
        Date endTime = parseDateUTC(args[1]);

        return true;
    }

    private static String createDateListFromArgs(final String[] args) throws ParseException {
        final Date startTime = parseDateUTC(args[0]);
        final Date endTime = parseDateUTC(args[1]);
        final int frequency = Integer.parseInt(args[2]);
        final TimeUnit timeUnit = TimeUnit.valueOf(args[3]);
        final TimeZone timeZone = getTimeZone(args[4]);
        final Repeatable rep = createRepeatable(startTime, frequency, timeUnit, timeZone);

        return getDateList(startTime, endTime, rep);
    }

    private static Repeatable createRepeatable(final Date startTime, final int frequency,
                                               final TimeUnit timeUnit, final TimeZone timeZone) {
         Repeatable rep = new Repeatable();
         rep.setBaseline(startTime);
        rep.setFrequency(Integer.parseInt(args[2]));
        rep.setTimeUnit(TimeUnit.valueOf(args[3]));
        rep.setTimeZone(getTimeZone(args[4]));
        rep.setFrequency(frequency);
        rep.setTimeUnit(timeUnit);
        rep.setTimeZone(timeZone);

        return rep;
    }

    private static String getDateList(final Date startTime, final Date endTime, final Repeatable rep) {
         int occurrence = 0;
        List<String> dates = new ArrayList<>();
         Date date = rep.getOccurrenceTime(startTime, occurrence++, null);
        StringBuilder dateList = new StringBuilder();

         while (date != null && date.before(endTime)) {
            dates.add(formatDateUTC(date));
             date = rep.getOccurrenceTime(startTime, occurrence++, null);
            if (occurrence > 1) {
                dateList.append(DATE_LIST_SEPARATOR);
            }
            dateList.append(formatDateUTC(date));
         }
 
        System.out.println("datelist :" + dateList+ ":");
        return String.join(DATE_LIST_SEPARATOR, dates);
    }

    private static void writeWorkflowOutput(final String dateList) throws IOException {
         //Passing the variable to WF that could be referred by subsequent actions
         File file = new File(System.getProperty("oozie.action.output.properties"));
         Properties props = new Properties();
        props.setProperty("datelist", dateList.toString());
        props.setProperty("datelist", dateList);
         try (OutputStream os = new FileOutputStream(file)) {
             props.store(os, "");
         }
     }

     //Utility methods
     private static DateFormat getISO8601DateFormat() {
         DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
@@ -82,11 +118,11 @@ public class DateList {
         return tz;
     }
 
    private static Date parseDateUTC(String s) throws Exception {
    private static Date parseDateUTC(String s) throws ParseException {
         return getISO8601DateFormat().parse(s);
     }
 
    private static String formatDateUTC(Date d) throws Exception {
    private static String formatDateUTC(Date d) {
         return (d != null) ? getISO8601DateFormat().format(d) : "NULL";
     }
 }
diff --git a/examples/src/test/java/org/apache/oozie/example/TestDateList.java b/examples/src/test/java/org/apache/oozie/example/TestDateList.java
new file mode 100644
index 000000000..55ff98353
-- /dev/null
++ b/examples/src/test/java/org/apache/oozie/example/TestDateList.java
@@ -0,0 +1,88 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.example;

import org.apache.oozie.action.hadoop.security.LauncherSecurityManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDateList {
    private static final String START = "2009-02-01T01:00Z";
    private static final String END = "2009-02-01T02:00Z";
    private static final String FREQUENCY = "15";
    private static final String TIMEUNIT = "MINUTES";
    private static final String TIMEZONE = "UTC";
    private static final String EXPECTED_DATE_RANGE
            = "2009-02-01T01:00Z,2009-02-01T01:15Z,2009-02-01T01:30Z,2009-02-01T01:45Z";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExitStatusIs_1_IfTooFewCLIArgs() throws IOException, ParseException {
        final String[] too_few_args = {START, END, FREQUENCY, TIMEUNIT};

        LauncherSecurityManager securityManager = new LauncherSecurityManager();
        securityManager.enable();

        try {
            expectedException.expect(SecurityException.class);
            DateList.main(too_few_args);
        } finally {
            assertTrue(securityManager.getExitInvoked());
            assertEquals("Unexpected exit code.", 1, securityManager.getExitCode());
            securityManager.disable();
        }
    }

    @Test
    public void testCorrectOutput() throws IOException, ParseException {
        final String[] args = {START, END, FREQUENCY, TIMEUNIT, TIMEZONE};

        final File output_file = folder.newFile("action_output.properties");

        final String output_filename = output_file.getCanonicalPath();

        System.setProperty("oozie.action.output.properties", output_filename);

        DateList.main(args);

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(output_file)) {
            props.load(is);
        }

        assertEquals("Incorrect date list.", EXPECTED_DATE_RANGE, props.getProperty("datelist"));
    }
}
diff --git a/release-log.txt b/release-log.txt
index 5100d97de..1979e6dd3 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 5.1.0 release (trunk - unreleased)
 
OOZIE-3315 DateList example fails (daniel.becker via andras.piros)
 OOZIE-3313 Hive example action fails (daniel.becker via gezapeti)
 OOZIE-3193 Applications are not killed when submitted via subworkflow (kmarton via gezapeti, andras.piros)
 OOZIE-3310 SQL error during /v2/sla filtering (asalamon74 via andras.piros)
- 
2.19.1.windows.1

