From 24fedae651b26b0f5fb980e96f26ae39fc92e89f Mon Sep 17 00:00:00 2001
From: mona <mona@unknown>
Date: Tue, 5 Mar 2013 22:47:36 +0000
Subject: [PATCH] OOZIE-1207 Optimize current EL resolution in case of
 start-instance and end-instance (rohini via mona)

git-svn-id: https://svn.apache.org/repos/asf/oozie/trunk@1453057 13f79535-47bb-0310-9956-ffa450edef68
--
 .../command/coord/CoordCommandUtils.java      |  10 +-
 .../apache/oozie/coord/CoordELFunctions.java  | 101 +++++++++++++-----
 .../org/apache/oozie/util/ParamChecker.java   |  19 +++-
 core/src/main/resources/oozie-default.xml     |   3 +
 release-log.txt                               |   1 +
 5 files changed, 96 insertions(+), 38 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java b/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
index 21f817d1c..5cd7775d7 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
@@ -227,13 +227,9 @@ public class CoordCommandUtils {
                 }
                 if (funcType == CURRENT) {
                     // Everything could be resolved NOW. no latest() ELs
                    for (int i = endIndex; i >= startIndex; i--) {
                        String matInstance = materializeInstance(event, "${coord:current(" + i + ")}", appInst, conf,
                                eval);
                        if (matInstance == null || matInstance.length() == 0) {
                            // Earlier than dataset's initial instance
                            break;
                        }
                    String matInstance = materializeInstance(event, "${coord:currentRange(" + startIndex + ","
                            + endIndex + ")}", appInst, conf, eval);
                    if (matInstance != null && !matInstance.isEmpty()) {
                         if (instances.length() > 0) {
                             instances.append(CoordELFunctions.INSTANCE_SEPARATOR);
                         }
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index 571b04edf..fa9b4fbb0 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -330,8 +330,7 @@ public class CoordELFunctions {
                         }
                         available++;
                     }
                    // nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(),
                    // -datasetFrequency);
                    // nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), datasetFrequency);
                     nominalInstanceCal = (Calendar) initInstance.clone();
                     instCount[0]++;
                     nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), instCount[0] * datasetFrequency);
@@ -509,6 +508,31 @@ public class CoordELFunctions {
         }
     }
 
    /**
     * Determine the date-time in Oozie processing timezone of current dataset instances
     * from start to end offsets from the nominal time. <p/> It depends
     * on: <p/> 1. Data set frequency <p/> 2. Data set Time unit (day, month, minute) <p/> 3. Data set Time zone/DST
     * <p/> 4. End Day/Month flag <p/> 5. Data set initial instance <p/> 6. Action Creation Time
     *
     * @param start :start instance offset <p/> domain: start <= 0, start is integer
     * @param end :end instance offset <p/> domain: end <= 0, end is integer
     * @return date-time in Oozie processing timezone of the instances from start to end offsets
     *        delimited by comma. <p/> If the current instance time of the dataset based on the Action Creation Time
     *        is earlier than the Initial-Instance of DS an empty string is returned.
     *        If an instance within the range is earlier than Initial-Instance of DS that instance is ignored
     * @throws Exception
     */
    public static String ph2_coord_currentRange(int start, int end) throws Exception {
        ParamChecker.checkLEZero(start, "current:n");
        ParamChecker.checkLEZero(end, "current:n");
        if (isSyncDataSet()) { // For Sync Dataset
            return coord_currentRange_sync(start, end);
        }
        else {
            throw new UnsupportedOperationException("Asynchronous Dataset is not supported yet");
        }
    }

     /**
      * Determine the date-time in Oozie processing timezone of the given offset from the dataset effective nominal time. <p/> It
      * depends on: <p> 1. Data set frequency <p/> 2. Data set Time Unit <p/> 3. Data set Time zone/DST
@@ -596,15 +620,13 @@ public class CoordELFunctions {
      * <p/> 4. End Day/Month flag <p/> 5. Data set initial instance <p/> 6. Action Creation Time <p/> 7. Existence of
      * dataset's directory
      *
     * @param n :instance count <p/> domain: n > 0, n is integer
     * @param n :instance count <p/> domain: n <= 0, n is integer
      * @return date-time in Oozie processing timezone of the n-th instance <p/> returns 'null' means n-th instance is
      * earlier than Initial-Instance of DS
      * @throws Exception
      */
     public static String ph3_coord_latest(int n) throws Exception {
        if (n > 0) {
            throw new IllegalArgumentException("paramter should be <= 0 but it is " + n);
        }
        ParamChecker.checkLEZero(n, "latest:n");
         if (isSyncDataSet()) {// For Sync Dataset
             return coord_latest_sync(n);
         }
@@ -620,14 +642,16 @@ public class CoordELFunctions {
      * <p/> 4. End Day/Month flag <p/> 5. Data set initial instance <p/> 6. Action Creation Time <p/> 7. Existence of
      * dataset's directory
      *
     * @param start :start instance offset <p/> domain: start > 0, start is integer
     * @param end :end instance offset <p/> domain: end > 0, end is integer
     * @param start :start instance offset <p/> domain: start <= 0, start is integer
     * @param end :end instance offset <p/> domain: end <= 0, end is integer
      * @return date-time in Oozie processing timezone of the instances from start to end offsets
      *        delimited by comma. <p/> returns 'null' means start offset instance is
      *        earlier than Initial-Instance of DS
      * @throws Exception
      */
     public static String ph3_coord_latestRange(int start, int end) throws Exception {
        ParamChecker.checkLEZero(start, "latest:n");
        ParamChecker.checkLEZero(end, "latest:n");
         if (isSyncDataSet()) {// For Sync Dataset
             return coord_latestRange_sync(start, end);
         }
@@ -680,6 +704,10 @@ public class CoordELFunctions {
         return echoUnResolved("current", n);
     }
 
    public static String ph1_coord_currentRange_echo(String start, String end) {
        return echoUnResolved("currentRange", start + ", " + end);
    }

     public static String ph1_coord_offset_echo(String n, String timeUnit) {
         return echoUnResolved("offset", n + " , " + timeUnit);
     }
@@ -688,6 +716,10 @@ public class CoordELFunctions {
         return echoUnResolved("current", n);
     }
 
    public static String ph2_coord_currentRange_echo(String start, String end) {
        return echoUnResolved("currentRange", start + ", " + end);
    }

     public static String ph2_coord_offset_echo(String n, String timeUnit) {
         return echoUnResolved("offset", n + " , " + timeUnit);
     }
@@ -849,29 +881,49 @@ public class CoordELFunctions {
      * @throws Exception
      */
     private static String coord_current_sync(int n) throws Exception {
        return coord_currentRange_sync(n, n);
    }

    private static String coord_currentRange_sync(int start, int end) throws Exception {
         int datasetFrequency = getDSFrequency();// in minutes
         TimeUnit dsTimeUnit = getDSTimeUnit();
         int[] instCount = new int[1];// used as pass by ref
         Calendar nominalInstanceCal = getCurrentInstance(getActionCreationtime(), instCount);
        StringBuilder instanceList = new StringBuilder();
         if (nominalInstanceCal == null) {
             LOG.warn("If the initial instance of the dataset is later than the nominal time, an empty string is"
                     + " returned. This means that no data is available at the current-instance specified by the user"
                     + " and the user could try modifying his initial-instance to an earlier time.");
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
                }
            }
         }
        nominalInstanceCal = getInitialInstanceCal();
        int absInstanceCount = instCount[0] + n;
        nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), datasetFrequency * absInstanceCount);
 
        if (nominalInstanceCal.getTime().compareTo(getInitialInstance()) < 0) {
            LOG.warn("If the initial instance of the dataset is later than the current-instance specified, such as"
                    + " coord:current({0}) in this case, an empty string is returned. This means that no data is"
                    + " available at the current-instance specified by the user and the user could try modifying his"
                    + " initial-instance to an earlier time.", n);
            return "";
        if (instanceList.length() > 0) {
            instanceList.setLength(instanceList.length() - CoordELFunctions.INSTANCE_SEPARATOR.length());
         }
        String str = DateUtils.formatDateOozieTZ(nominalInstanceCal);
        return str;
        return instanceList.toString();
     }
 
     /**
@@ -946,14 +998,6 @@ public class CoordELFunctions {
     }
 
     private static String coord_latestRange_sync(int startOffset, int endOffset) throws Exception {
        if (startOffset > 0) {
            throw new RuntimeException("For latest there is no meaning " + "of positive instance. n should be <=0"
                    + startOffset);
        }
        if (endOffset > 0) {
            throw new RuntimeException("For latest there is no meaning " + "of positive instance. n should be <=0"
                    + endOffset);
        }
         ELEvaluator eval = ELEvaluator.getCurrent();
         String retVal = "";
         int datasetFrequency = (int) getDSFrequency();// in minutes
@@ -1019,8 +1063,7 @@ public class CoordELFunctions {
 
                         available--;
                     }
                    // nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(),
                    // -datasetFrequency);
                    // nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), -datasetFrequency);
                     nominalInstanceCal = (Calendar) initInstance.clone();
                     instCount[0]--;
                     nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), instCount[0] * datasetFrequency);
diff --git a/core/src/main/java/org/apache/oozie/util/ParamChecker.java b/core/src/main/java/org/apache/oozie/util/ParamChecker.java
index b38e4f251..7778c9037 100644
-- a/core/src/main/java/org/apache/oozie/util/ParamChecker.java
++ b/core/src/main/java/org/apache/oozie/util/ParamChecker.java
@@ -6,9 +6,9 @@
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
 * 
 *
  *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@@ -180,6 +180,21 @@ public class ParamChecker {
         return value;
     }
 
    /**
     * Check whether the value is less than or equal to 0.
     *
     * @param value : value to test
     * @param name : Name of the parameter
     * @return If the value is <= 0, return the value. Otherwise throw IllegalArgumentException
     */
    public static int checkLEZero(int value, String name) {
        if (value > 0) {
            throw new IllegalArgumentException(XLog.format(
                    "parameter [{0}] = [{1}] must be less than or equal to zero", name, value));
        }
        return value;
    }

     /**
      * Check whether the value is Integer.
      *
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index f04b81461..304afc238 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -730,6 +730,7 @@
             coord:daysInMonth=org.apache.oozie.coord.CoordELFunctions#ph1_coord_daysInMonth_echo,
             coord:tzOffset=org.apache.oozie.coord.CoordELFunctions#ph1_coord_tzOffset_echo,
             coord:current=org.apache.oozie.coord.CoordELFunctions#ph1_coord_current_echo,
            coord:currentRange=org.apache.oozie.coord.CoordELFunctions#ph1_coord_currentRange_echo,
             coord:offset=org.apache.oozie.coord.CoordELFunctions#ph1_coord_offset_echo,
             coord:latest=org.apache.oozie.coord.CoordELFunctions#ph1_coord_latest_echo,
             coord:latestRange=org.apache.oozie.coord.CoordELFunctions#ph1_coord_latestRange_echo,
@@ -887,6 +888,7 @@
             coord:daysInMonth=org.apache.oozie.coord.CoordELFunctions#ph2_coord_daysInMonth,
             coord:tzOffset=org.apache.oozie.coord.CoordELFunctions#ph2_coord_tzOffset,
             coord:current=org.apache.oozie.coord.CoordELFunctions#ph2_coord_current,
            coord:currentRange=org.apache.oozie.coord.CoordELFunctions#ph2_coord_currentRange,
             coord:offset=org.apache.oozie.coord.CoordELFunctions#ph2_coord_offset,
             coord:latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo,
             coord:latestRange=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latestRange_echo,
@@ -942,6 +944,7 @@
             coord:daysInMonth=org.apache.oozie.coord.CoordELFunctions#ph2_coord_daysInMonth,
             coord:tzOffset=org.apache.oozie.coord.CoordELFunctions#ph2_coord_tzOffset,
             coord:current=org.apache.oozie.coord.CoordELFunctions#ph2_coord_current_echo,
            coord:currentRange=org.apache.oozie.coord.CoordELFunctions#ph2_coord_currentRange_echo,
             coord:offset=org.apache.oozie.coord.CoordELFunctions#ph2_coord_offset_echo,
             coord:latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo,
             coord:latestRange=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latestRange_echo,
diff --git a/release-log.txt b/release-log.txt
index d24ca1bd0..73833a69a 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -4,6 +4,7 @@ OOZIE-1239 Bump up trunk to 4.1.0-SNAPSHOT (virag)
 
 -- Oozie 4.0.0 (unreleased)
 
OOZIE-1207 Optimize current EL resolution in case of start-instance and end-instance (rohini via mona)
 OOZIE-1247 CoordActionInputCheck shouldn't queue CoordPushInputCheck (rohini via virag)
 OOZIE-1238 CoordPushCheck doesn't evaluate the configuration section which is propogated to workflow (virag)
 OOZIE-1203 Oozie web-console to display Bundle job definition, configuration and log tabs (mona)
- 
2.19.1.windows.1

