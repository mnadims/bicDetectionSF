From 712504b2df712c9265dee3e4493858508148e32f Mon Sep 17 00:00:00 2001
From: Josh Elser <elserj@apache.org>
Date: Tue, 10 Feb 2015 18:54:14 -0500
Subject: [PATCH] ACCUMULO-3576 Use guava's preconditions, not jline's

--
 .../org/apache/accumulo/tserver/ActiveAssignmentRunnable.java   | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

diff --git a/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java b/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
index dcbdae756..c02f7f29a 100644
-- a/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
++ b/server/tserver/src/main/java/org/apache/accumulo/tserver/ActiveAssignmentRunnable.java
@@ -18,7 +18,7 @@ package org.apache.accumulo.tserver;
 
 import java.util.concurrent.ConcurrentHashMap;
 
import jline.internal.Preconditions;
import com.google.common.base.Preconditions;
 
 import org.apache.accumulo.core.data.KeyExtent;
 import org.slf4j.Logger;
- 
2.19.1.windows.1

