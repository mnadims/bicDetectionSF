From 92d8f371553b88e5b3a9d3354e93f75d60d81368 Mon Sep 17 00:00:00 2001
From: Jason Lowe <jlowe@apache.org>
Date: Mon, 29 Aug 2016 15:55:38 +0000
Subject: [PATCH] HADOOP-13552. RetryInvocationHandler logs all remote
 exceptions. Contributed by Jason Lowe

--
 .../apache/hadoop/io/retry/RetryInvocationHandler.java    | 8 +++++---
 1 file changed, 5 insertions(+), 3 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
index 7bd3a15c4bf..c657d20709d 100644
-- a/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
++ b/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/io/retry/RetryInvocationHandler.java
@@ -351,9 +351,11 @@ private RetryInfo handleException(final Method method, final int callId,
     if (retryInfo.isFail()) {
       // fail.
       if (retryInfo.action.reason != null) {
        LOG.warn("Exception while invoking call #" + callId + " "
            + proxyDescriptor.getProxyInfo().getString(method.getName())
            + ". Not retrying because " + retryInfo.action.reason, e);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Exception while invoking call #" + callId + " "
              + proxyDescriptor.getProxyInfo().getString(method.getName())
              + ". Not retrying because " + retryInfo.action.reason, e);
        }
       }
       throw e;
     }
- 
2.19.1.windows.1

