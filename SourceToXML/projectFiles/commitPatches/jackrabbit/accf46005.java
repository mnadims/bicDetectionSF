From accf4600511a63f7b0d6559b8e24ee40912c9341 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Fri, 21 Oct 2011 13:50:46 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again

Modify checker to also inspect "candidate" version histories (additional test case)

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1187345 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/persistence/AutoFixCorruptNode.java  | 87 ++++++++++++++++++-
 1 file changed, 85 insertions(+), 2 deletions(-)

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
index 302285405..104311332 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
@@ -101,7 +101,6 @@ public class AutoFixCorruptNode extends TestCase {
         try {
             Node root = s.getRootNode();
 
            // add nodes /test and /test/missing
             Node test = root.addNode("test");
             test.addMixin("mix:versionable");
 
@@ -114,7 +113,6 @@ public class AutoFixCorruptNode extends TestCase {
 
             Node brokenNode = vhr;
             String vhrRootVersionId = vhr.getNode("jcr:rootVersion").getIdentifier();
            
             UUID destroy = UUID.fromString(brokenNode.getIdentifier());
             s.logout();
             
@@ -168,6 +166,10 @@ public class AutoFixCorruptNode extends TestCase {
             report = TestHelper.checkVersionStoreConsistency(s, false);
             assertTrue("Some problems should have been fixed but are not: " + report, report.getItems().size() < reportitems);
             
            // get a fresh session
            s.logout();
            s = openSession(rep, false);

             test = s.getRootNode().getNode("test");
             // versioning should be disabled now
             assertFalse(test.isNodeType("mix:versionable"));
@@ -309,6 +311,87 @@ public class AutoFixCorruptNode extends TestCase {
         }
     }
 
    // similar to above, but disconnects version history before damaging the repository
    public void testMissingRootVersion2() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);

        String oldVersionRecoveryProp = System
                .getProperty("org.apache.jackrabbit.version.recovery");

        try {
            Node root = s.getRootNode();

            // add nodes /test and /test/missing
            Node test = root.addNode("test");
            test.addMixin("mix:versionable");

            s.save();

            Node vhr = s.getWorkspace().getVersionManager()
                    .getVersionHistory(test.getPath());

            assertNotNull(vhr);

            Node brokenNode = vhr.getNode("jcr:rootVersion");
            String vhrId = vhr.getIdentifier();
            
            UUID destroy = UUID.fromString(brokenNode.getIdentifier());

            // disable versioning
            test.removeMixin("mix:versionable");
            s.save();
            
            s.logout();
            
            
            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, false);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());
            
            s.logout();

            System.setProperty("org.apache.jackrabbit.version.recovery", "true");

            s = openSession(rep, false);
            s.logout();

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should still be disabled
            assertFalse(test.isNodeType("mix:versionable"));
            
            // try to enable versioning again
            test.addMixin("mix:versionable");
            s.save();

            Node oldVHR = s.getNodeByIdentifier(vhrId);
            Node newVHR = s.getWorkspace().getVersionManager().getVersionHistory(test.getPath());

            assertTrue("old and new version history path should be different: "
                    + oldVHR.getPath() + " vs " + newVHR.getPath(), !oldVHR
                    .getPath().equals(newVHR.getPath()));

            // name should be same plus suffix
            assertTrue(oldVHR.getName().startsWith(newVHR.getName()));
            
            // try a checkout / checkin
            s.getWorkspace().getVersionManager().checkout(test.getPath());
            s.getWorkspace().getVersionManager().checkin(test.getPath());
        } finally {
            s.logout();
            System.setProperty("org.apache.jackrabbit.version.recovery",
                    oldVersionRecoveryProp == null ? ""
                            : oldVersionRecoveryProp);
        }
    }

     public void testAutoFix() throws Exception {
 
         // new repository
- 
2.19.1.windows.1

