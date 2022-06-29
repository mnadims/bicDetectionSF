From 7cebc2604d6813fd1e2929a1b8334a8ba8df4cf8 Mon Sep 17 00:00:00 2001
From: Shwetha GS <sshivalingamurthy@hortonworks.com>
Date: Tue, 15 Nov 2016 15:19:06 +0530
Subject: [PATCH] OOZIE-2724 coord:current resolves monthly/yearly dependencies
 incorrectly (satishsaley via shwethags)

--
 .../apache/oozie/coord/CoordELFunctions.java  |  7 ++--
 .../oozie/coord/TestCoordELFunctions.java     | 36 +++++++++++++++++++
 release-log.txt                               |  1 +
 3 files changed, 40 insertions(+), 4 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index 22eb1c359..925a7aa5d 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -62,8 +62,6 @@ public class CoordELFunctions {
     public static final long MINUTE_MSEC = 60 * 1000L;
     public static final long HOUR_MSEC = 60 * MINUTE_MSEC;
     public static final long DAY_MSEC = 24 * HOUR_MSEC;
    public static final long MONTH_MSEC = 30 * DAY_MSEC;
    public static final long YEAR_MSEC = 365 * DAY_MSEC;
     /**
      * Used in defining the frequency in 'day' unit. <p> domain: <code> val &gt; 0</code> and should be integer.
      *
@@ -1401,10 +1399,11 @@ public class CoordELFunctions {
                 break;
             case MONTH:
             case END_OF_MONTH:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / MONTH_MSEC);
                int diffYear = calEffectiveTime.get(Calendar.YEAR) - current.get(Calendar.YEAR);
                instanceCount[0] = diffYear * 12 + calEffectiveTime.get(Calendar.MONTH) - current.get(Calendar.MONTH);
                 break;
             case YEAR:
                instanceCount[0] = (int) ((effectiveTime.getTime() - datasetInitialInstance.getTime()) / YEAR_MSEC);
                instanceCount[0] = calEffectiveTime.get(Calendar.YEAR) - current.get(Calendar.YEAR);
                 break;
             default:
                 throw new IllegalArgumentException("Unhandled dataset time unit " + dsTimeUnit);
diff --git a/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java b/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
index fb7e0303d..be60133a5 100644
-- a/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
++ b/core/src/test/java/org/apache/oozie/coord/TestCoordELFunctions.java
@@ -702,6 +702,42 @@ public class TestCoordELFunctions extends XTestCase {
 
         expr = "${coord:current(1)}";
         assertEquals("2009-06-01T07:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        // Case 8
        ds.setEndOfDuration(TimeUnit.END_OF_MONTH);
        ds.setFrequency(1);
        ds.setTimeZone(DateUtils.getTimeZone("UTC"));
        ds.setInitInstance(DateUtils.parseDateOozieTZ("2010-01-01T00:00Z"));
        appInst.setNominalTime(DateUtils.parseDateOozieTZ("2016-10-31T00:55Z"));
        CoordELFunctions.configureEvaluator(eval, ds, appInst);

        expr = "${coord:current(0)}";
        assertEquals("2016-10-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        expr = "${coord:current(1)}";
        assertEquals("2016-11-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        expr = "${coord:current(-1)}";
        assertEquals("2016-09-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        // Test with YEAR
        ds.setTimeUnit(TimeUnit.YEAR);
        ds.setEndOfDuration(TimeUnit.YEAR);
        ds.setFrequency(1);
        ds.setTimeZone(DateUtils.getTimeZone("UTC"));
        // Initial instance is far behind to accumulate effect of leap years
        ds.setInitInstance(DateUtils.parseDateOozieTZ("1963-01-01T00:00Z"));
        appInst.setNominalTime(DateUtils.parseDateOozieTZ("2016-10-31T00:55Z"));
        CoordELFunctions.configureEvaluator(eval, ds, appInst);

        expr = "${coord:current(0)}";
        assertEquals("2016-01-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        expr = "${coord:current(1)}";
        assertEquals("2017-01-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));

        expr = "${coord:current(-1)}";
        assertEquals("2015-01-01T00:00Z", CoordELFunctions.evalAndWrap(eval, expr));
     }
 
     public void testOffset() throws Exception {
diff --git a/release-log.txt b/release-log.txt
index 3071c7ba3..fead3961c 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -10,6 +10,7 @@ OOZIE-2634 Queue dump command message is confusing when the queue is empty (andr
 
 -- Oozie 4.3.0 release
 
OOZIE-2724 coord:current resolves monthly/yearly dependencies incorrectly (satishsaley via shwethags)
 OOZIE-2719 Test case failure (abhishekbafna via jaydeepvishwakarma)
 OOZIE-2674 Improve oozie commads documentation (abhishekbafna via rkanter)
 OOZIE-2710 Oozie HCatalog example workflow fails (abhishekbafna via shwethags)
- 
2.19.1.windows.1

