From 93b807537cd8c3e144d17d271de27d5eb2f89cb5 Mon Sep 17 00:00:00 2001
From: Stefan Guggisberg <stefan@apache.org>
Date: Thu, 24 Jul 2008 12:30:53 +0000
Subject: [PATCH] JCR-1632: Mixin type loss

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@679389 13f79535-47bb-0310-9956-ffa450edef68
--
 .../bundle/AbstractBundlePersistenceManager.java    |  2 +-
 .../core/persistence/bundle/util/BundleBinding.java | 13 +++++++++++++
 2 files changed, 14 insertions(+), 1 deletion(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
index 202506554..36eab7e26 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
@@ -562,7 +562,7 @@ public abstract class AbstractBundlePersistenceManager implements
                 bundle.update((NodeState) state);
             } else {
                 PropertyId id = (PropertyId) state.getId();
                // skip primaryType pr mixinTypes properties
                // skip redundant primaryType, mixinTypes and uuid properties
                 if (id.getName().equals(NameConstants.JCR_PRIMARYTYPE)
                     || id.getName().equals(NameConstants.JCR_MIXINTYPES)
                     || id.getName().equals(NameConstants.JCR_UUID)) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
index 062fe3fa3..feb230544 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
@@ -31,6 +31,7 @@ import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.uuid.UUID;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
@@ -114,6 +115,12 @@ public class BundleBinding extends ItemStateBinding {
         // properties
         name = readIndexedQName(in);
         while (name != null) {
            // skip redundant primaryType, mixinTypes and uuid properties
            if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                || name.equals(NameConstants.JCR_MIXINTYPES)
                || name.equals(NameConstants.JCR_UUID)) {
                continue;
            }
             PropertyId pId = new PropertyId(bundle.getId(), name);
             NodePropBundle.PropertyEntry pState = readPropertyEntry(in, pId);
             bundle.addProperty(pState);
@@ -278,6 +285,12 @@ public class BundleBinding extends ItemStateBinding {
         iter = bundle.getPropertyNames().iterator();
         while (iter.hasNext()) {
             Name pName = (Name) iter.next();
            // skip redundant primaryType, mixinTypes and uuid properties
            if (pName.equals(NameConstants.JCR_PRIMARYTYPE)
                || pName.equals(NameConstants.JCR_MIXINTYPES)
                || pName.equals(NameConstants.JCR_UUID)) {
                continue;
            }
             NodePropBundle.PropertyEntry pState = bundle.getPropertyEntry(pName);
             if (pState == null) {
                 log.error("PropertyState missing in bundle: " + pName);
- 
2.19.1.windows.1

