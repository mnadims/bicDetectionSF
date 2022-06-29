From 49f2e452312404f85d67b2cf33decf56ec05f633 Mon Sep 17 00:00:00 2001
From: Przemyslaw Pakulski <ppakulski@apache.org>
Date: Thu, 25 Sep 2008 12:09:19 +0000
Subject: [PATCH] JCR-1766 fixed

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@698935 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/persistence/bundle/util/BundleBinding.java | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
index 4b69789b5..64042fb91 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
@@ -116,14 +116,15 @@ public class BundleBinding extends ItemStateBinding {
         // properties
         name = readIndexedQName(in);
         while (name != null) {
            PropertyId pId = new PropertyId(bundle.getId(), name);
             // skip redundant primaryType, mixinTypes and uuid properties
             if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                 || name.equals(NameConstants.JCR_MIXINTYPES)
                 || name.equals(NameConstants.JCR_UUID)) {
                readPropertyEntry(in, pId);
                 name = readIndexedQName(in);
                 continue;
             }
            PropertyId pId = new PropertyId(bundle.getId(), name);
             NodePropBundle.PropertyEntry pState = readPropertyEntry(in, pId);
             bundle.addProperty(pState);
             name = readIndexedQName(in);
- 
2.19.1.windows.1

