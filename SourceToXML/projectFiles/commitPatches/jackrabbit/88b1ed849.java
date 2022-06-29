From 88b1ed849256d2300f52a79f41bf9385956e41b7 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Wed, 19 Oct 2011 15:50:35 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again

Augment another InconsistentVersioningState exception with the VH node ID

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1186285 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/version/InternalVersionManagerBase.java    | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
index 3924c87a9..e4fb1045f 100755
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerBase.java
@@ -325,7 +325,8 @@ abstract class InternalVersionManagerBase implements InternalVersionManager {
                 }
                 ChildNodeEntry rootv = history.getState().getChildNodeEntry(JCR_ROOTVERSION, 1);
                 if (rootv == null) {
                    throw new InconsistentVersioningState("missing child node entry for " + JCR_ROOTVERSION + " on version history node " + history.getNodeId());
                    throw new InconsistentVersioningState("missing child node entry for " + JCR_ROOTVERSION + " on version history node " + history.getNodeId(),
                            history.getNodeId(), null);
                 }
                 info = new VersionHistoryInfo(history.getNodeId(),
                         rootv.getId());
- 
2.19.1.windows.1

