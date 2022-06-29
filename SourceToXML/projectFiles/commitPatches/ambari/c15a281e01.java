From c15a281e01af5dda2e7551dae29bc60a53ca11a7 Mon Sep 17 00:00:00 2001
From: Aravindan Vijayan <avijayan@hortonworks.com>
Date: Tue, 14 Jun 2016 21:58:29 -0700
Subject: [PATCH] AMBARI-17238 : AMS extrapolation should be done only for
 Counter metrics. (avijayan)

--
 ...TimelineMetricClusterAggregatorSecond.java | 86 +++++++++++++++----
 ...lineMetricClusterAggregatorSecondTest.java | 28 +++++-
 2 files changed, 91 insertions(+), 23 deletions(-)

diff --git a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
index 722dc14d6c..bdc0feb83f 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/main/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecond.java
@@ -18,6 +18,7 @@
 package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;
 
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.metrics2.sink.timeline.PostProcessingUtil;
 import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
@@ -269,30 +270,77 @@ public class TimelineMetricClusterAggregatorSecond extends AbstractTimelineAggre
                                          Map<Long, Double> timeSliceValueMap) {
 
 
    List<Long> requiredTimestamps = new ArrayList<>();
    for (Long[] timeSlice : timeSlices) {
      if (!timeSliceValueMap.containsKey(timeSlice[1])) {
        requiredTimestamps.add(timeSlice[1]);
    if (StringUtils.isNotEmpty(timelineMetric.getType()) && "COUNTER".equalsIgnoreCase(timelineMetric.getType())) {
      //For Counter Based metrics, ok to do interpolation and extrapolation

      List<Long> requiredTimestamps = new ArrayList<>();
      for (Long[] timeSlice : timeSlices) {
        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          requiredTimestamps.add(timeSlice[1]);
        }
       }
    }
      Map<Long, Double> interpolatedValuesMap = PostProcessingUtil.interpolate(timelineMetric.getMetricValues(), requiredTimestamps);

      if (interpolatedValuesMap != null) {
        for (Map.Entry<Long, Double> entry : interpolatedValuesMap.entrySet()) {
          Double interpolatedValue = entry.getValue();

          if (interpolatedValue != null) {
            TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
              timelineMetric.getMetricName(),
              timelineMetric.getAppId(),
              timelineMetric.getInstanceId(),
              entry.getKey(),
              timelineMetric.getType());

            timelineClusterMetricMap.put(clusterMetric, interpolatedValue);
          } else {
            LOG.debug("Cannot compute interpolated value, hence skipping.");
          }
        }
      }
    } else {
      //For other metrics, ok to do only interpolation
 
    Map<Long, Double> interpolatedValuesMap = PostProcessingUtil.interpolate(timelineMetric.getMetricValues(), requiredTimestamps);
      for (int sliceNum = 0; sliceNum < timeSlices.size(); sliceNum++) {
        Long[] timeSlice = timeSlices.get(sliceNum);
 
    if (interpolatedValuesMap != null) {
      for (Map.Entry<Long, Double> entry : interpolatedValuesMap.entrySet()) {
        Double interpolatedValue = entry.getValue();
        if (!timeSliceValueMap.containsKey(timeSlice[1])) {
          LOG.debug("Found an empty slice : " + new Date(timeSlice[0]) + ", " + new Date(timeSlice[1]));
 
        if (interpolatedValue != null) {
          TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
            timelineMetric.getMetricName(),
            timelineMetric.getAppId(),
            timelineMetric.getInstanceId(),
            entry.getKey(),
            timelineMetric.getType());
          Double lastSeenValue = null;
          int index = sliceNum - 1;
          Long[] prevTimeSlice = null;
          while (lastSeenValue == null && index >= 0) {
            prevTimeSlice = timeSlices.get(index--);
            lastSeenValue = timeSliceValueMap.get(prevTimeSlice[1]);
          }
 
          timelineClusterMetricMap.put(clusterMetric, interpolatedValue);
        } else {
          LOG.debug("Cannot compute interpolated value, hence skipping.");
          Double nextSeenValue = null;
          index = sliceNum + 1;
          Long[] nextTimeSlice = null;
          while (nextSeenValue == null && index < timeSlices.size()) {
            nextTimeSlice = timeSlices.get(index++);
            nextSeenValue = timeSliceValueMap.get(nextTimeSlice[1]);
          }

          Double interpolatedValue = PostProcessingUtil.interpolate(timeSlice[1],
            (prevTimeSlice != null ? prevTimeSlice[1] : null), lastSeenValue,
            (nextTimeSlice != null ? nextTimeSlice[1] : null), nextSeenValue);

          if (interpolatedValue != null) {
            TimelineClusterMetric clusterMetric = new TimelineClusterMetric(
              timelineMetric.getMetricName(),
              timelineMetric.getAppId(),
              timelineMetric.getInstanceId(),
              timeSlice[1],
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
index dc01f38e3e..1e2f4ec367 100644
-- a/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java
++ b/ambari-metrics/ambari-metrics-timelineservice/src/test/java/org/apache/hadoop/yarn/server/applicationhistoryservice/metrics/timeline/aggregators/TimelineMetricClusterAggregatorSecondTest.java
@@ -64,24 +64,44 @@ public class TimelineMetricClusterAggregatorSecondTest {
       }
     }
 
    TimelineMetric counterMetric = new TimelineMetric();
    counterMetric.setMetricName("TestMetric");
    counterMetric.setHostName("TestHost");
    counterMetric.setAppId("TestAppId");
    counterMetric.setMetricValues(metricValues);
    counterMetric.setType("COUNTER");

    Map<TimelineClusterMetric, Double> timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(counterMetric, timeSlices);

    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(counterMetric.getMetricName(), counterMetric.getAppId(),
      counterMetric.getInstanceId(), 0l, null);

    timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 6.0);

    timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
    Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 12.0);

     TimelineMetric metric = new TimelineMetric();
     metric.setMetricName("TestMetric");
     metric.setHostName("TestHost");
     metric.setAppId("TestAppId");
     metric.setMetricValues(metricValues);
 
    Map<TimelineClusterMetric, Double> timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(metric, timeSlices);
    timelineClusterMetricMap = secondAggregator.sliceFromTimelineMetric(metric, timeSlices);
 
    TimelineClusterMetric timelineClusterMetric = new TimelineClusterMetric(metric.getMetricName(), metric.getAppId(),
    timelineClusterMetric = new TimelineClusterMetric(metric.getMetricName(), metric.getAppId(),
       metric.getInstanceId(), 0l, null);
 
     timelineClusterMetric.setTimestamp(roundedStartTime + 2*sliceInterval);
     Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 6.0);
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 4.5);
 
     timelineClusterMetric.setTimestamp(roundedStartTime + 4*sliceInterval);
     Assert.assertTrue(timelineClusterMetricMap.containsKey(timelineClusterMetric));
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 12.0);
    Assert.assertEquals(timelineClusterMetricMap.get(timelineClusterMetric), 7.5);
 
   }
 
- 
2.19.1.windows.1

