From ef513d2853ce943ce3fea6aa6a3f6c9bdec25f63 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohinip@yahoo-inc.com>
Date: Thu, 1 May 2014 10:16:07 -0700
Subject: [PATCH] OOZIE-1709 CoordELFunctions.getCurrentInstance() is expensive
 (shwethags via rohini)

--
 .../apache/oozie/coord/CoordELFunctions.java  | 119 +++++++++++++-----
 .../oozie/coord/TestCoordELFunctions.java     |  15 ++-
 release-log.txt                               |   1 +
 3 files changed, 102 insertions(+), 33 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index d73bc7dc1..db3259bb2 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -17,25 +17,28 @@
  */
 package org.apache.oozie.coord;
 
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.dependency.URIHandler.Context;
 import org.apache.oozie.dependency.URIHandler;
import org.apache.oozie.dependency.URIHandler.Context;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.URIHandlerService;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ELEvaluator;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.URIHandlerService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
 
 /**
  * This class implements the EL function related to coordinator
@@ -52,6 +55,12 @@ public class CoordELFunctions {
     // TODO: in next release, support flexibility
     private static String END_OF_OPERATION_INDICATOR_FILE = "_SUCCESS";
 
    public static final long MINUTE_MSEC = 60 * 1000L;
    public static final long HOUR_MSEC = 60 * MINUTE_MSEC;
    public static final long DAY_MSEC = 24 * HOUR_MSEC;
    public static final long MONTH_MSEC = 30 * DAY_MSEC;
    public static final long YEAR_MSEC = 365 * DAY_MSEC;

     /**
      * Used in defining the frequency in 'day' unit. <p/> domain: <code> val &gt; 0</code> and should be integer.
      *
@@ -922,7 +931,6 @@ public class CoordELFunctions {
         TimeUnit dsTimeUnit = getDSTimeUnit();
         int[] instCount = new int[1];// used as pass by ref
         Calendar nominalInstanceCal = getCurrentInstance(getActionCreationtime(), instCount);
        StringBuilder instanceList = new StringBuilder();
         if (nominalInstanceCal == null) {
             LOG.warn("If the initial instance of the dataset is later than the nominal time, an empty string is"
                     + " returned. This means that no data is available at the current-instance specified by the user"
@@ -930,33 +938,25 @@ public class CoordELFunctions {
             return "";
         } else {
             Calendar initInstance = getInitialInstanceCal();
            instCount[0] = instCount[0] + end;
             // Add in the reverse order - newest instance first.
            for (int i = end; i >= start; i--) {
                // Tried to avoid the clone. But subtracting datasetFrequency gives different results than multiplying
                // and Spring DST transition test in TestCoordELfunctions.testCurrent() fails
                //nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), -datasetFrequency);
                nominalInstanceCal = (Calendar) initInstance.clone();
                nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), instCount[0] * datasetFrequency);
                instCount[0]--;
            nominalInstanceCal = (Calendar) initInstance.clone();
            nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), (instCount[0] + start) * datasetFrequency);
            List<String> instances = new ArrayList<String>();
            for (int i = start; i <= end; i++) {
                 if (nominalInstanceCal.compareTo(initInstance) < 0) {
                     LOG.warn("If the initial instance of the dataset is later than the current-instance specified,"
                             + " such as coord:current({0}) in this case, an empty string is returned. This means that"
                             + " no data is available at the current-instance specified by the user and the user could"
                             + " try modifying his initial-instance to an earlier time.", start);
                    break;
                 }
                 else {
                    instanceList.append(DateUtils.formatDateOozieTZ(nominalInstanceCal));
                    instanceList.append(CoordELFunctions.INSTANCE_SEPARATOR);
                    instances.add(DateUtils.formatDateOozieTZ(nominalInstanceCal));
                 }
                nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), datasetFrequency);
             }
            instances = Lists.reverse(instances);
            return StringUtils.join(instances, CoordELFunctions.INSTANCE_SEPARATOR);
         }

        if (instanceList.length() > 0) {
            instanceList.setLength(instanceList.length() - CoordELFunctions.INSTANCE_SEPARATOR.length());
        }
        return instanceList.toString();
     }
 
     /**
@@ -1223,9 +1223,8 @@ public class CoordELFunctions {
         if (ds == null) {
             throw new RuntimeException("Associated Dataset should be defined with key " + DATASET);
         }
        Calendar effInitTS = Calendar.getInstance();
        Calendar effInitTS = new GregorianCalendar(ds.getTimeZone());
         effInitTS.setTime(ds.getInitInstance());
        effInitTS.setTimeZone(ds.getTimeZone());
         // To adjust EOD/EOM
         DateUtils.moveToEnd(effInitTS, getDSEndOfFlag(eval));
         return effInitTS;
@@ -1293,6 +1292,68 @@ public class CoordELFunctions {
      *         the dataset.
      */
     private static Calendar getCurrentInstance(Date effectiveTime, int instanceCount[], ELEvaluator eval) {
        Date datasetInitialInstance = getInitialInstance(eval);
        TimeUnit dsTimeUnit = getDSTimeUnit(eval);
        TimeZone dsTZ = getDatasetTZ(eval);
        int dsFreq = getDSFrequency(eval);
        // Convert Date to Calendar for corresponding TZ
        Calendar current = Calendar.getInstance(dsTZ);
        current.setTime(datasetInitialInstance);

        Calendar calEffectiveTime = new GregorianCalendar(dsTZ);
        calEffectiveTime.setTime(effectiveTime);
        if (instanceCount == null) {    // caller doesn't care about this value
            instanceCount = new int[1];
        }
        instanceCount[0] = 0;
        if (current.compareTo(calEffectiveTime) > 0) {
            return null;
        }

        switch(dsTimeUnit) {
            case MINUTE:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / MINUTE_MSEC);
                break;
            case HOUR:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / HOUR_MSEC);
                break;
            case DAY:
            case END_OF_DAY:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / DAY_MSEC);
                break;
            case MONTH:
            case END_OF_MONTH:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / MONTH_MSEC);
                break;
            case YEAR:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / YEAR_MSEC);
                break;
            default:
                throw new IllegalArgumentException("Unhandled dataset time unit " + dsTimeUnit);
        }

        if (instanceCount[0] > 2) {
            instanceCount[0] = (instanceCount[0] / dsFreq);
            current.add(dsTimeUnit.getCalendarUnit(), instanceCount[0] * dsFreq);
        } else {
            instanceCount[0] = 0;
        }
        while (!current.getTime().after(effectiveTime)) {
            current.add(dsTimeUnit.getCalendarUnit(), dsFreq);
            instanceCount[0]++;
        }
        current.add(dsTimeUnit.getCalendarUnit(), -dsFreq);
        instanceCount[0]--;
        return current;
    }

    /**
     * Find the current instance based on effectiveTime (i.e Action_Creation_Time or Action_Start_Time)
     *
     * @return current instance i.e. current(0) returns null if effectiveTime is earlier than Initial Instance time of
     *         the dataset.
     */
    private static Calendar getCurrentInstance_old(Date effectiveTime, int instanceCount[], ELEvaluator eval) {
         Date datasetInitialInstance = getInitialInstance(eval);
         TimeUnit dsTimeUnit = getDSTimeUnit(eval);
         TimeZone dsTZ = getDatasetTZ(eval);
diff --git a/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java b/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
index be35ce4a8..13315b91c 100644
-- a/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
++ b/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
@@ -184,7 +184,6 @@ public class TestCoordELFunctions extends XTestCase {
 
         SyncCoordAction appInst = new SyncCoordAction();
         SyncCoordDataset ds = new SyncCoordDataset();
        ;
         ds.setFrequency(1);
         ds.setTimeUnit(TimeUnit.DAY);
         ds.setInitInstance(DateUtils.parseDateOozieTZ("2009-01-02T00:00Z"));
@@ -260,7 +259,6 @@ public class TestCoordELFunctions extends XTestCase {
 
         SyncCoordAction appInst = new SyncCoordAction();
         SyncCoordDataset ds = new SyncCoordDataset();
        ;
         ds.setFrequency(1);
         ds.setTimeUnit(TimeUnit.MONTH);
         ds.setInitInstance(DateUtils.parseDateOozieTZ("2009-01-02T00:00Z"));
@@ -371,6 +369,16 @@ public class TestCoordELFunctions extends XTestCase {
         assertEquals("2010-09-08T23:59Z", CoordELFunctions.evalAndWrap(eval, expr));
     }
 
    public void testCurrentRange() throws Exception {
        init("coord-action-create");
        String expr = "${coord:currentRange(-1, 0)}";
        assertEquals("2009-09-09T23:59Z#2009-09-08T23:59Z", CoordELFunctions.evalAndWrap(eval, expr));

        //test out of range instances, EL should return partial instances
        appInst.setNominalTime(DateUtils.parseDateOozieTZ("2009-09-01T23:59Z"));
        assertEquals("2009-09-01T23:59Z", CoordELFunctions.evalAndWrap(eval, expr));
    }

     public void testCurrent() throws Exception {
         init("coord-action-create");
         String expr = "${coord:current(-1)}";
@@ -395,7 +403,6 @@ public class TestCoordELFunctions extends XTestCase {
 
         SyncCoordAction appInst = new SyncCoordAction();
         SyncCoordDataset ds = new SyncCoordDataset();
        ;
         ds.setFrequency(1);
         ds.setTimeUnit(TimeUnit.DAY);
         ds.setInitInstance(DateUtils.parseDateOozieTZ("2009-01-02T00:00Z"));
@@ -1026,7 +1033,7 @@ public class TestCoordELFunctions extends XTestCase {
      * public void testDetach() throws Exception { Services.get().destroy(); }
      */
 
    private void init(String tag) throws Exception {
    void init(String tag) throws Exception {
         init(tag, "hdfs://localhost:9000/user/" + getTestUser() + "/US/${YEAR}/${MONTH}/${DAY}");
     }
 
diff --git a/release-log.txt b/release-log.txt
index eb9e40cbd..030fe981b 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1709 CoordELFunctions.getCurrentInstance() is expensive (shwethags via rohini) 
 OOZIE-1787 parameterize interval of SLAService updating SlaStatus (ryota)
 OOZIE-1777 duplicated log message in Pig launcher's stdout (ryota)
 OOZIE-1748 When using cron-like syntax, the "Time Unit" field says "MINUTE"
- 
2.19.1.windows.1

