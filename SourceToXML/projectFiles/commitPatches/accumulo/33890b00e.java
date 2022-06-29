From 33890b00e1cf7f353631439eb6278d646ce70194 Mon Sep 17 00:00:00 2001
From: Christopher Tubbs <ctubbsii@apache.org>
Date: Wed, 11 Feb 2015 17:12:01 -0500
Subject: [PATCH] ACCUMULO-3580 Make metadata iterator consume in the normal
 case

--
 .../server/master/state/TabletStateChangeIterator.java        | 4 ++++
 1 file changed, 4 insertions(+)

diff --git a/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java b/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
index 405bab7db..b11809c01 100644
-- a/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
++ b/server/src/main/java/org/apache/accumulo/server/master/state/TabletStateChangeIterator.java
@@ -142,11 +142,15 @@ public class TabletStateChangeIterator extends SkippingIterator {
         case HOSTED:
           if (!shouldBeOnline)
             return;
          break;
         case ASSIGNED_TO_DEAD_SERVER:
           return;
         case UNASSIGNED:
           if (shouldBeOnline)
             return;
          break;
        default:
          throw new AssertionError("Inconceivable! The tablet is an unrecognized state: " + tls.getState(current));
       }
       // table is in the expected state so don't bother returning any information about it
       getSource().next();
- 
2.19.1.windows.1

