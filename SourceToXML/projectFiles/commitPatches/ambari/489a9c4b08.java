From 489a9c4b0835558669ab30b0cfda68b60c8a238c Mon Sep 17 00:00:00 2001
From: Aravindan Vijayan <avijayan@hortonworks.com>
Date: Fri, 27 May 2016 14:27:40 -0700
Subject: [PATCH] AMBARI-16887 : [AMS / Grafana] Metrics are staying flat for 1
 minutes, causing rate calculations to be 0 (avijayan)

--
 ambari-metrics/ambari-metrics-common/pom.xml  |  5 ++
 .../sink/timeline/PostProcessingUtil.java     | 84 ++++++++++++++++++-
 .../cache/PostProcessingUtilTest.java         | 28 +++++++
 ...TimelineMetricClusterAggregatorSecond.java | 35 +++-----
 ...lineMetricClusterAggregatorSecondTest.java |  4 +-
 5 files changed, 127 insertions(+), 29 deletions(-)

diff --git a/ambari-metrics/ambari-metrics-common/pom.xml b/ambari-metrics/ambari-metrics-common/pom.xml
index 70483c974f..41ba62e187 100644
-- a/ambari-metrics/ambari-metrics-common/pom.xml
++ b/ambari-metrics/ambari-metrics-common/pom.xml
@@ -116,6 +116,11 @@
       <artifactId>jackson-mapper-asl</artifactId>
       <version>1.9.13</version>
     </dependency>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-math3</artifactId>
      <version>3.1.1</version>
    </dependency>
     <dependency>
       <groupId>junit</groupId>
       <artifactId>junit</artifactId>
diff --git a/ambari-metrics/ambari-metrics-common/src/main/java/org/apache/hadoop/metrics2/sink/timeline/PostProcessingUtil.java b/ambari-metrics/ambari-metrics-common/src/main/java/org/apache/hadoop/metrics2/sink/timeline/PostProcessingUtil.java
index ab890ecc02..814ea1f3e0 100644
-- a/ambari-metrics/ambari-metrics-common/src/main/java/org/apache/hadoop/metrics2/sink/timeline/PostProcessingUtil.java
++ b/ambari-metrics/ambari-metrics-common/src/main/java/org/apache/hadoop/metrics2/sink/timeline/PostProcessingUtil.java
@@ -17,6 +17,12 @@
  */
 package org.apache.hadoop.metrics2.sink.timeline;
 
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.HashMap;
import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
@@ -58,8 +64,6 @@ public class PostProcessingUtil {
 
   public static Double interpolate(Long t, Long t1, Double m1,
                                    Long t2, Double m2) {


     //Linear Interpolation : y = y0 + (y1 - y0) * ((x - x0) / (x1 - x0))
     if (m1 == null && m2 == null) {
       return null;
@@ -78,4 +82,80 @@ public class PostProcessingUtil {
     return m1 +  slope * (t - t1);
   }
 
  public static Map<Long, Double> interpolate(Map<Long, Double> valuesMap, List<Long> requiredTimestamps) {

    LinearInterpolator linearInterpolator = new LinearInterpolator();

    if (valuesMap == null || valuesMap.isEmpty()) {
      return null;
    }
    if (requiredTimestamps == null || requiredTimestamps.isEmpty()) {
      return null;
    }

    Map<Long, Double> interpolatedValuesMap = new HashMap<>();

    if (valuesMap.size() == 1) {
      //Just one value present in the window. Use that value to interpolate all required timestamps.
      Double value  = null;
      for (Map.Entry<Long, Double> entry : valuesMap.entrySet()) {
        value = entry.getValue();
      }
      for (Long requiredTs : requiredTimestamps) {
        interpolatedValuesMap.put(requiredTs, value);
      }
      return interpolatedValuesMap;
    }

    double[] timestamps = new double[valuesMap.size()];
    double[] metrics = new double[valuesMap.size()];

    int i = 0;
    for (Map.Entry<Long, Double> entry : valuesMap.entrySet()) {
      timestamps[i] = (double) entry.getKey();
      metrics[i++] = entry.getValue();
    }

    PolynomialSplineFunction function = linearInterpolator.interpolate(timestamps, metrics);
    PolynomialFunction[] splines = function.getPolynomials();
    PolynomialFunction first = splines[0];

    for (Long requiredTs : requiredTimestamps) {

      Double interpolatedValue = null;
      if (timestampInRange(requiredTs, timestamps[0], timestamps[timestamps.length - 1])) {
        /*
          Interpolation Case
          Required TS is within range of the set of values used for interpolation.
          Hence, we can use library to get the interpolated value.
         */
        interpolatedValue = function.value((double) requiredTs);
      } else {
        /*
        Extrapolation Case
        Required TS outside range of the set of values used for interpolation.
        We will use the coefficients to make best effort extrapolation
        y(x)= y1 + m * (x−x1)
        where, m = (y2−y1)/(x2−x1)
         */
        if (first.getCoefficients() != null && first.getCoefficients().length > 0) {
          /*
          y = c0 + c1x
          where c0, c1 are coefficients
          c1 will not be present if slope is zero.
           */
          Double y1 = first.getCoefficients()[0];
          Double m = (first.getCoefficients().length > 1) ? first.getCoefficients()[1] : 0.0;
          interpolatedValue = y1 + m * (requiredTs - timestamps[0]);
        }
      }
      interpolatedValuesMap.put(requiredTs, interpolatedValue);
    }
    return interpolatedValuesMap;
  }

  private static boolean timestampInRange(Long timestamp, double left, double right) {
    return (timestamp >= left && timestamp <= right);
  }

 }
diff --git a/ambari-metrics/ambari-metrics-common/src/test/java/org/apache/hadoop/metrics2/sink/timeline/cache/PostProcessingUtilTest.java b/ambari-metrics/ambari-metrics-common/src/test/java/org/apache/hadoop/metrics2/sink/timeline/cache/PostProcessingUtilTest.java
index 1ec71d03b0..d8387d0304 100644
-- a/ambari-metrics/ambari-metrics-common/src/test/java/org/apache/hadoop/metrics2/sink/timeline/cache/PostProcessingUtilTest.java
++ b/ambari-metrics/ambari-metrics-common/src/test/java/org/apache/hadoop/metrics2/sink/timeline/cache/PostProcessingUtilTest.java
@@ -22,7 +22,9 @@ import org.apache.hadoop.metrics2.sink.timeline.PostProcessingUtil;
 import org.apache.hadoop.metrics2.sink.timeline.SingleValuedTimelineMetric;
 import org.junit.Test;
 
import java.util.ArrayList;
 import java.util.Iterator;
import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
@@ -82,4 +84,30 @@ public class PostProcessingUtilTest {
 
   }
 
  @Test
  public void testLinearInterpolate() throws Exception {

    long t2 = System.currentTimeMillis();

    Map<Long, Double> valuesMap = new TreeMap<>();

    valuesMap.put(t2 - 4 * 3000, 4.0);
    valuesMap.put(t2 - 2 * 3000, 2.0);
    valuesMap.put(t2 - 1 * 3000, 1.0);

    List<Long> requiredTs = new ArrayList<Long>();
    requiredTs.add(t2 - 5*3000);
    requiredTs.add(t2 - 3*3000);
    requiredTs.add(t2);

    Map result = PostProcessingUtil.interpolate(valuesMap, requiredTs);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.get(t2 - 5*3000), 5.0);
    Assert.assertEquals(result.get(t2 - 3*3000), 3.0);
    Assert.assertEquals(result.get(t2), 0.0);
    System.out.println(result.toString());

  }

   }
diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
index 117145e861..722dc14d6c 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
@@ -269,46 +269,31 @@ public class TimelineMetricClusterAggregatorSecond extends AbstractTimelineAggre
                                          Map<Long, Double> timeSliceValueMap) {
 
 
    for (int sliceNum = 0; sliceNum < timeSlices.size(); sliceNum++) {
      Long[] timeSlice = timeSlices.get(sliceNum);

    List<Long> requiredTimestamps = new ArrayList<>();
    for (Long[] timeSlice : timeSlices) {
       if (!timeSliceValueMap.containsKey(timeSlice[1])) {
        LOG.debug("Found an empty slice : " + new Date(timeSlice[0]) + ", " + new Date(timeSlice[1]));

        Double lastSeenValue = null;
        int index = sliceNum - 1;
        Long[] prevTimeSlice = null;
        while (lastSeenValue == null && index >= 0) {
          prevTimeSlice = timeSlices.get(index--);
          lastSeenValue = timeSliceValueMap.get(prevTimeSlice[1]);
        }
        requiredTimestamps.add(timeSlice[1]);
      }
    }
 
        Double nextSeenValue = null;
        index = sliceNum + 1;
        Long[] nextTimeSlice = null;
        while ( nextSeenValue == null && index < timeSlices.size()) {
          nextTimeSlice = timeSlices.get(index++);
          nextSeenValue = timeSliceValueMap.get(nextTimeSlice[1]);
        }
    Map<Long, Double> interpolatedValuesMap = PostProcessingUtil.interpolate(timelineMetric.getMetricValues(), requiredTimestamps);
 
        Double interpolatedValue = PostProcessingUtil.interpolate(timeSlice[1],
          (prevTimeSlice != null ? prevTimeSlice[1] : null), lastSeenValue,
          (nextTimeSlice != null ? nextTimeSlice[1] : null), nextSeenValue);
    if (interpolatedValuesMap != null) {
      for (Map.Entry<Long, Double> entry : interpolatedValuesMap.entrySet()) {
        Double interpolatedValue = entry.getValue();
 
         if (interpolatedValue != null) {
           TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
             timelineMetric.getMetricName(),
             timelineMetric.getAppId(),
             timelineMetric.getInstanceId(),
            timeSlice[1],
            entry.getKey(),
             timelineMetric.getType());
 
          LOG.debug("Interpolated value : " + interpolatedValue);
           timelineClusterMetricMap.put(clusterMetric, interpolatedValue);
         } else {
           LOG.debug("Cannot compute interpolated value, hence skipping.");
         }

       }
     }
   }
diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java b/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java
index f55dda1471..dc01f38e3e 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java
@@ -77,11 +77,11 @@ public class TimelineMetricClusterAggregatorSecondTest {
 
     timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
     Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 4.5);
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 6.0);
 
     timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
     Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 7.5);
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 12.0);
 
   }
 
- 
2.19.1.windows.1

