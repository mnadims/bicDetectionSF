From 562787a8021ffbad91114bc3134ba676157b83de Mon Sep 17 00:00:00 2001
From: Angela Schreiber <angela@apache.org>
Date: Tue, 29 Sep 2009 12:44:41 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId

 removing leftover

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@819908 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/state/PropertyState.java    | 7 -------
 1 file changed, 7 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
index 10627bae7..2bb0a77ae 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
@@ -21,7 +21,6 @@ import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 
 import javax.jcr.PropertyType;
 
@@ -50,11 +49,6 @@ public class PropertyState extends ItemState {
      */
     private boolean multiValued;
 
    /**
     * the property definition
     */
    private QPropertyDefinition def;

     /**
      * Constructs a new property state that is initially connected to an
      * overlayed state.
@@ -92,7 +86,6 @@ public class PropertyState extends ItemState {
             PropertyState propState = (PropertyState) state;
             id = propState.id;
             type = propState.type;
            def = propState.def;
             values = propState.values;
             multiValued = propState.multiValued;
             if (syncModCount) {
- 
2.19.1.windows.1

