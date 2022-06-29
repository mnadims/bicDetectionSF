From f2e0df04d4fec7ac178384e609d1614ecafa075a Mon Sep 17 00:00:00 2001
From: Thomas Mueller <thomasm@apache.org>
Date: Wed, 20 Jan 2010 08:02:59 +0000
Subject: [PATCH] JCR-2449 Slow performance due to JCR-2138

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@901095 13f79535-47bb-0310-9956-ffa450edef68
--
 .../apache/jackrabbit/core/state/SharedItemStateManager.java  | 4 ----
 1 file changed, 4 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 5d64aa1f3..328df7d93 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -881,10 +881,6 @@ public class SharedItemStateManager
             // process added REFERENCE properties
             for (ItemState state : local.addedStates()) {
                 if (!state.isNode()) {
                    // remove refs from the target which have been added externally (JCR-2138)
                    if (hasItemState(state.getId())) {
                        removeReferences(getItemState(state.getId()));
                    }
                     // add new references to the target
                     addReferences((PropertyState) state);
                 }
- 
2.19.1.windows.1

