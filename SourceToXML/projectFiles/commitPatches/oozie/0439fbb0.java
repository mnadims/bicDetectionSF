From 0439fbb057d1ecd32a5cea250481c63a6cfc3a96 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohini@apache.org>
Date: Mon, 7 Jul 2014 14:59:34 -0700
Subject: [PATCH] OOZIE-1886 Queue operation talking longer time (shwethags via
 rohini)

--
 .../apache/oozie/util/PriorityDelayQueue.java | 20 +++++++------------
 release-log.txt                               |  1 +
 2 files changed, 8 insertions(+), 13 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java b/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
index a3f214840..1aad92e2c 100644
-- a/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
++ b/core/src/main/java/org/apache/oozie/util/PriorityDelayQueue.java
@@ -349,20 +349,14 @@ public class PriorityDelayQueue<E> extends AbstractQueue<PriorityDelayQueue.Queu
         if (!ignoreSize && currentSize != null && currentSize.get() >= maxSize) {
             return false;
         }
        boolean accepted;
        lock.lock();
        try {
            accepted = queues[queueElement.getPriority()].offer(queueElement);
            debug("offer([{0}]), to P[{1}] delay[{2}ms] accepted[{3}]", queueElement.getElement().toString(),
                  queueElement.getPriority(), queueElement.getDelay(TimeUnit.MILLISECONDS), accepted);
            if (accepted) {
                if (currentSize != null) {
                    currentSize.incrementAndGet();
                }
                queueElement.inQueue = true;
        boolean accepted = queues[queueElement.getPriority()].offer(queueElement);
        debug("offer([{0}]), to P[{1}] delay[{2}ms] accepted[{3}]", queueElement.getElement().toString(),
              queueElement.getPriority(), queueElement.getDelay(TimeUnit.MILLISECONDS), accepted);
        if (accepted) {
            if (currentSize != null) {
                currentSize.incrementAndGet();
             }
        } finally {
            lock.unlock();
            queueElement.inQueue = true;
         }
         return accepted;
     }
diff --git a/release-log.txt b/release-log.txt
index eee06a473..5ac93f262 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1886 Queue operation talking longer time (shwethags via rohini)
 OOZIE-1865 Oozie servers can't talk to each other with Oozie HA and Kerberos (rkanter)
 OOZIE-1821 Oozie java action fails due to AlreadyBeingCreatedException (abhishek.agarwal via rkanter)
 OOZIE-1532 Purging should remove completed children job for long running coordinator jobs (bzhang)
- 
2.19.1.windows.1

