From 6438f47cec47b484c8679f061208f60adc029f47 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Fri, 10 Mar 2017 11:43:37 +0000
Subject: [PATCH] JCR-4118: RepositoryChecker creates invalid node names

create valid names / do sanity check in test case

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1786325 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/core/RepositoryChecker.java    |  9 ++---
 .../core/persistence/AutoFixCorruptNode.java  | 36 ++++++++++++++++---
 2 files changed, 37 insertions(+), 8 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
index eff796440..91749271f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/RepositoryChecker.java
@@ -28,6 +28,7 @@ import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCE
 import java.util.Calendar;
 import java.util.HashSet;
 import java.util.Set;
import java.util.TimeZone;
 
 import javax.jcr.ItemNotFoundException;
 import javax.jcr.RepositoryException;
@@ -47,7 +48,6 @@ import org.apache.jackrabbit.core.version.VersionHistoryInfo;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.NameFactory;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.util.ISO8601;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -304,9 +304,10 @@ class RepositoryChecker {
                         modifiedParent = new NodeState(vhrParentState, NodeState.STATUS_EXISTING_MODIFIED, true);
                     }
 
                    Calendar now = Calendar.getInstance();
                    String appendme = " (disconnected by RepositoryChecker on "
                            + ISO8601.format(now) + ")";
                    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    String appendme = String.format(" (disconnected by RepositoryChecker on %04d%02d%02dT%02d%02d%02dZ)",
                            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
                            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
                     modifiedParent.renameChildNodeEntry(vhid,
                             nf.create(vhrname.getNamespaceURI(), vhrname.getLocalName() + appendme));
 
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
index b21932d30..c13b10d71 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
@@ -30,14 +30,15 @@ import javax.jcr.Repository;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;
import javax.jcr.nodetype.ConstraintViolationException;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.jackrabbit.core.TestHelper;
 import org.apache.jackrabbit.core.TransientRepository;
 import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
 
import junit.framework.TestCase;

 /**
  * Tests that a corrupt node is automatically fixed.
  */
@@ -123,7 +124,6 @@ public class AutoFixCorruptNode extends TestCase {
             // now retry with lost+found functionality
             ConsistencyReport report2 = TestHelper.checkConsistency(s, true, lnfid);
             assertTrue("Report should have reported broken nodes", !report2.getItems().isEmpty());

             s.logout();
 
             s = openSession(rep, false);
@@ -265,9 +265,11 @@ public class AutoFixCorruptNode extends TestCase {
             Node test = root.addNode("test", "nt:file");
             test.addNode("jcr:content", "nt:unstructured");
             test.addMixin("mix:versionable");

             s.save();
 
            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());
            
             Node vhr = s.getWorkspace().getVersionManager()
                     .getVersionHistory(test.getPath());
 
@@ -354,6 +356,7 @@ public class AutoFixCorruptNode extends TestCase {
             s.getWorkspace().getVersionManager().checkout(test.getPath());
             s.getWorkspace().getVersionManager().checkin(test.getPath());
 
            validateDisconnectedVHR(oldVHR);            
         } finally {
             s.logout();
             System.setProperty("org.apache.jackrabbit.version.recovery",
@@ -435,6 +438,8 @@ public class AutoFixCorruptNode extends TestCase {
             // try a checkout / checkin
             s.getWorkspace().getVersionManager().checkout(test.getPath());
             s.getWorkspace().getVersionManager().checkin(test.getPath());

            validateDisconnectedVHR(oldVHR);            
         } finally {
             s.logout();
             System.setProperty("org.apache.jackrabbit.version.recovery",
@@ -575,4 +580,27 @@ public class AutoFixCorruptNode extends TestCase {
         }
         return rep.login(cred);
     }
    
    // JCR-4118: check that the old VHR can be retrieved
    private void validateDisconnectedVHR(Node oldVHR) throws RepositoryException {
        Session s = oldVHR.getSession();
        Node old = s.getNode(oldVHR.getPath());
        assertNotNull("disconnected VHR should be accessible", old);

        assertEquals("nt:versionHistory", old.getPrimaryNodeType().getName());
        NodeIterator ni = old.getNodes();
        while (ni.hasNext()) {
            Node n = ni.nextNode();
            String type = n.getPrimaryNodeType().getName();
            assertTrue("node type of VHR child nodes should be nt:version or nt:versionLabels",
                    "nt:version".equals(type) || "nt:versionLabels".equals(type));
        }

        try {
            old.remove();
            s.save();
            fail("removal of node using remove() should throw because it's in the versioning workspace");
        } catch (ConstraintViolationException expected) {
        }
    }
 }
- 
2.19.1.windows.1

