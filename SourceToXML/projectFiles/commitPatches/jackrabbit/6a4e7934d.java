From 6a4e7934d7e711d216122590f18d4f7c2c86eb7b Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Tue, 18 Oct 2011 14:39:01 +0000
Subject: [PATCH] JCR-3115: Versioning fixup leaves persistence in a state
 where the node can't be made versionable again -- add test case

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1185692 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/persistence/AutoFixCorruptNode.java  | 109 ++++++++++++++++++
 1 file changed, 109 insertions(+)

diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
index e1efebabe..302285405 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/persistence/AutoFixCorruptNode.java
@@ -200,6 +200,115 @@ public class AutoFixCorruptNode extends TestCase {
         }
     }
 
    public void testMissingRootVersion() throws Exception {

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
            s.logout();
            
            destroyBundle(destroy, "version");

            s = openSession(rep, false);

            ConsistencyReport report = TestHelper.checkVersionStoreConsistency(s, false);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());
            
            try {
                test = s.getRootNode().getNode("test");
                vhr = s.getWorkspace().getVersionManager()
                        .getVersionHistory(test.getPath());
                fail("should not get here");
            } catch (Exception ex) {
                // expected
            }

            s.logout();

            System.setProperty("org.apache.jackrabbit.version.recovery", "true");

            s = openSession(rep, false);

            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
            assertFalse(test.isNodeType("mix:versionable"));
            
            try {
                // try to enable versioning again
                test.addMixin("mix:versionable");
                s.save();
                
                fail("enabling versioning succeeded unexpectedly");
            }
            catch (Exception e) {
                // we expect this to fail
            }
            
            s.logout();
            
            // now redo after running fixup on versioning storage
            s = openSession(rep, false);

            report = TestHelper.checkVersionStoreConsistency(s, true);
            assertTrue("Report should have reported broken nodes", !report.getItems().isEmpty());
            int reportitems = report.getItems().size();
            
            // problems should now be fixed
            report = TestHelper.checkVersionStoreConsistency(s, false);
            assertTrue("Some problems should have been fixed but are not: " + report, report.getItems().size() < reportitems);
            
            test = s.getRootNode().getNode("test");
            // versioning should be disabled now
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

