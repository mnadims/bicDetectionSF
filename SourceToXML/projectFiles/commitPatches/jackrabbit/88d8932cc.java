From 88d8932cc4a74f5bbc2512d36d6b556081f0876e Mon Sep 17 00:00:00 2001
From: Angela Schreiber <angela@apache.org>
Date: Mon, 2 Dec 2013 10:45:41 +0000
Subject: [PATCH] JCR-3702 : NPE if user w/o read permission on admin user node
 removes any node

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1546953 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/BatchedItemOperations.java           |   5 -
 .../apache/jackrabbit/core/ItemValidator.java |   5 -
 .../core/security/user/UserManagerImpl.java   |  15 --
 .../core/security/user/AdministratorTest.java | 213 ++++++++++++------
 4 files changed, 149 insertions(+), 89 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
index 2df3ea284..ff32888e9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
@@ -43,7 +43,6 @@ import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
 import org.apache.jackrabbit.core.retention.RetentionRegistry;
 import org.apache.jackrabbit.core.security.AccessManager;
 import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
 import org.apache.jackrabbit.core.session.SessionContext;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemState;
@@ -934,10 +933,6 @@ public class BatchedItemOperations extends ItemValidator {
                 throw new RepositoryException("Unable to perform removal. Node is affected by a retention.");
             }
         }

        if (UserManagerImpl.includesAdmin(context.getSessionImpl().getItemManager().getNode(targetPath))) {
            throw new RepositoryException("Attempt to remove/move the admin user.");
        }
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
index 33d11d42a..eb08c5681 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
@@ -33,7 +33,6 @@ import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
 import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
 import org.apache.jackrabbit.core.session.SessionContext;
 import org.apache.jackrabbit.core.session.SessionOperation;
 import org.apache.jackrabbit.core.state.NodeState;
@@ -303,10 +302,6 @@ public class ItemValidator {
                 throw new RepositoryException("Unable to perform operation. Node is affected by a retention.");
             }
         }

        if (isRemoval && item.isNode() && UserManagerImpl.includesAdmin((NodeImpl) item)) {
            throw new RepositoryException("Attempt to remove/move the admin user.");
        }
     }
 
     public synchronized boolean canModify(
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/UserManagerImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/UserManagerImpl.java
index 02a5a10c8..dcdc91d5b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/UserManagerImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/security/user/UserManagerImpl.java
@@ -16,7 +16,6 @@
  */
 package org.apache.jackrabbit.core.security.user;
 
import org.apache.jackrabbit.api.JackrabbitRepository;
 import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
 import org.apache.jackrabbit.api.security.user.Authorizable;
 import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
@@ -1154,20 +1153,6 @@ public class UserManagerImpl extends ProtectedItemModifier
         }
     }
 
    //--------------------------------------------------------------------------
    public static boolean includesAdmin(NodeImpl node) throws RepositoryException {
        SessionImpl s = (SessionImpl) node.getSession();
        if (s.getRepository().getDescriptorValue(JackrabbitRepository.OPTION_USER_MANAGEMENT_SUPPORTED).getBoolean()) {
            UserManager uMgr = s.getUserManager();
            if (uMgr instanceof UserManagerImpl) {
                UserManagerImpl uMgrImpl = (UserManagerImpl) uMgr;
                AuthorizableImpl admin = (AuthorizableImpl) uMgrImpl.getAuthorizable(uMgrImpl.adminId);
                return Text.isDescendantOrEqual(node.getPath(), admin.getNode().getPath());
            }
        }
        return false;
    }

     //------------------------------------------------------< inner classes >---
     /**
      * Inner class
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/security/user/AdministratorTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/security/user/AdministratorTest.java
index c7946f2db..b26ac5173 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/security/user/AdministratorTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/security/user/AdministratorTest.java
@@ -16,13 +16,20 @@
  */
 package org.apache.jackrabbit.core.security.user;
 
import java.util.Properties;
import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 
 import org.apache.jackrabbit.api.security.user.AbstractUserTest;
 import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
 import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
 import org.apache.jackrabbit.test.NotExecutableException;
 
 /**
@@ -65,6 +72,13 @@ public class AdministratorTest extends AbstractUserTest {
         }
     }
 
    /**
     * Test if the administrator is recreated upon login if the corresponding
     * node gets removed.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
     public void testRemoveAdminNode() throws RepositoryException, NotExecutableException {
         Authorizable admin = userMgr.getAuthorizable(adminId);
 
@@ -72,98 +86,141 @@ public class AdministratorTest extends AbstractUserTest {
             throw new NotExecutableException();
         }
 
        Session s = null;
        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        // after removing the node the admin user doesn't exist any more
        assertNull(userMgr.getAuthorizable(adminId));

        // login must succeed as system user mgr recreates the admin user
        Session s2 = getHelper().getSuperuserSession();
         try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            s = adminNode.getSession();
            adminNode.remove();
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            assertNotNull(getUserManager(s2).getAuthorizable(adminId));
         } finally {
            if (s != null) {
                s.refresh(false);
            }
            s2.logout();
         }
     }
 
    public void testSessionRemoveItem()  throws RepositoryException, NotExecutableException {
    /**
     * Test for collisions that would prevent from recreate the admin user.
     * - an intermediate rep:AuthorizableFolder node with the same name
     */
    public void testAdminNodeCollidingWithAuthorizableFolder() throws RepositoryException, NotExecutableException {
         Authorizable admin = userMgr.getAuthorizable(adminId);
 
         if (admin == null || !(admin instanceof AuthorizableImpl)) {
             throw new NotExecutableException();
         }
 
        Session s = null;
        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        String adminPath = adminNode.getPath();
        String adminNodeName = adminNode.getName();
        Node parentNode = adminNode.getParent();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
         try {
            NodeImpl parent = (NodeImpl) ((AuthorizableImpl) admin).getNode().getParent();
            s = parent.getSession();
            s.removeItem(parent.getPath());
            // now create a colliding node:
            Node n = parentNode.addNode(adminNodeName, "rep:AuthorizableFolder");
            collidingPath = n.getPath();
             s.save();
            fail();
        } catch (RepositoryException e) {
            // success

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            assertEquals(adminNodeName, ((AuthorizableImpl) admin).getNode().getName());
            assertFalse(adminPath.equals(((AuthorizableImpl) admin).getNode().getPath()));

         } finally {
            if (s != null) {
                s.refresh(false);
            if (s2 != null) {
                s2.logout();
            }
            // remove the extra folder and the admin user (created underneath) again.
            if (collidingPath != null) {
                s.getNode(collidingPath).remove();
                s.save();
             }
         }
     }
 
    public void testSessionMoveAdminNode()  throws RepositoryException, NotExecutableException {
    /**
     * Test for collisions that would prevent from recreate the admin user.
     * - a colliding node somewhere else with the same jcr:uuid.
     *
     * Test if creation of the administrator user forces the removal of some
     * other node in the repository that by change happens to have the same
     * jcr:uuid and thus inhibits the creation of the admininstrator user.
     */
    public void testAdminNodeCollidingWithRandomNode() throws RepositoryException, NotExecutableException {
         Authorizable admin = userMgr.getAuthorizable(adminId);
 
         if (admin == null || !(admin instanceof AuthorizableImpl)) {
             throw new NotExecutableException();
         }
 
        Session s = null;
        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        NodeId nid = adminNode.getNodeId();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
         try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            s = adminNode.getSession();
            s.move(adminNode.getPath(), "/somewhereelse");
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            // create a colliding node outside of the user tree
            NameResolver nr = (SessionImpl) s;
            // NOTE: testRootNode will not be present if users are stored in a distinct wsp.
            //       therefore use root node as start...
            NodeImpl tr = (NodeImpl) s.getRootNode();
            Node n = tr.addNode(nr.getQName("tmpNode"), nr.getQName(testNodeType), nid);
            collidingPath = n.getPath();
             s.save();
            fail();
        } catch (RepositoryException e) {
            // success
        }  finally {
            if (s != null) {
                s.refresh(false);
            }
        }
    }
 
    public void testSessionMoveParentNode()  throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);
            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();
 
        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }
            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            // the colliding node must have been removed.
            assertFalse(s2.nodeExists(collidingPath));
 
        Session s = null;
        try {
            NodeImpl parent = (NodeImpl) ((AuthorizableImpl) admin).getNode().getParent();
            s = parent.getSession();
            s.move(parent.getPath(), "/somewhereelse");
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
         } finally {
            if (s != null) {
                s.refresh(false);
            if (s2 != null) {
                s2.logout();
            }
            if (collidingPath != null && s.nodeExists(collidingPath)) {
                s.getNode(collidingPath).remove();
                s.save();
             }
         }
     }
 
    public void testWorkspaceMoveAdminNode()  throws RepositoryException, NotExecutableException {
    /**
     * Reconfiguration of the user-root-path will result in node collision
     * upon initialization of the built-in repository users. Test if the
     * UserManagerImpl in this case removes the colliding admin-user node.
     */
    public void testChangeUserRootPath() throws RepositoryException, NotExecutableException {
         Authorizable admin = userMgr.getAuthorizable(adminId);
 
         if (admin == null || !(admin instanceof AuthorizableImpl)) {
@@ -171,13 +228,41 @@ public class AdministratorTest extends AbstractUserTest {
         }
 
         // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
         try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            Session s = adminNode.getSession();
            s.getWorkspace().move(adminNode.getPath(), "/somewhereelse");
            fail();
        } catch (RepositoryException e) {
            // success
            // create a colliding user node outside of the user tree
            Properties props = new Properties();
            props.setProperty("usersPath", "/testPath");
            UserManager um = new UserManagerImpl((SessionImpl) s, adminId, props);
            User collidingUser = um.createUser(adminId, adminId);
            collidingPath = ((AuthorizableImpl) collidingUser).getNode().getPath();
            s.save();

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            // the colliding node must have been removed.
            assertFalse(s2.nodeExists(collidingPath));

        } finally {
            if (s2 != null) {
                s2.logout();
            }
            if (collidingPath != null && s.nodeExists(collidingPath)) {
                s.getNode(collidingPath).remove();
                s.save();
            }
         }
     }
 }
- 
2.19.1.windows.1

