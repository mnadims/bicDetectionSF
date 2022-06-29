From 026e0ea05f8d7399d1c15dcfc9be0b823d85b60c Mon Sep 17 00:00:00 2001
From: Thomas Mueller <thomasm@apache.org>
Date: Mon, 7 Feb 2011 15:47:50 +0000
Subject: [PATCH] JCR-2882 DatabaseJournal: java.lang.IllegalStateException:
 already in batch mode

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1067983 13f79535-47bb-0310-9956-ffa450edef68
--
 jackrabbit-core/pom.xml                       |   2 +-
 .../core/journal/DatabaseJournal.java         |  64 +++----
 .../core/util/db/ConnectionHelper.java        |  68 +++----
 .../core/cluster/DbClusterTest.java           |  80 ++++++++
 .../jackrabbit/core/cluster/TestAll.java      |   1 +
 .../jackrabbit/core/cluster/repository-h2.xml | 171 ++++++++++++++++++
 6 files changed, 319 insertions(+), 67 deletions(-)
 create mode 100644 jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/DbClusterTest.java
 create mode 100644 jackrabbit-core/src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml

diff --git a/jackrabbit-core/pom.xml b/jackrabbit-core/pom.xml
index 3948010d1..4f367a927 100644
-- a/jackrabbit-core/pom.xml
++ b/jackrabbit-core/pom.xml
@@ -290,7 +290,7 @@ org.apache.jackrabbit.test.api.ShareableNodeTest#testGetNodesByPattern
     <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
      <version>1.2.121</version>
      <version>1.3.149</version>
       <scope>test</scope>
     </dependency>
   </dependencies>
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
index 7c43d2ab9..7ba58ab71 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/journal/DatabaseJournal.java
@@ -93,7 +93,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
     /**
      * Logger.
      */
    private static Logger log = LoggerFactory.getLogger(DatabaseJournal.class);
    static Logger log = LoggerFactory.getLogger(DatabaseJournal.class);
 
     /**
      * Driver name, bean property.
@@ -128,7 +128,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
     /**
      * The connection helper
      */
    private ConnectionHelper conHelper;
    ConnectionHelper conHelper;
 
     /**
      * Auto commit level.
@@ -148,13 +148,14 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
     /**
      * The sleep time of the revision table janitor in seconds, 1 day default.
      */
    private int janitorSleep = 60 * 60 * 24;
    int janitorSleep = 60 * 60 * 24;
 
     /**
      * Indicates when the next run of the janitor is scheduled.
      * The first run is scheduled by default at 03:00 hours.
      */
    private Calendar janitorNextRun = Calendar.getInstance();
    Calendar janitorNextRun = Calendar.getInstance();

     {
         if (janitorNextRun.get(Calendar.HOUR_OF_DAY) >= 3) {
             janitorNextRun.add(Calendar.DAY_OF_MONTH, 1);
@@ -206,19 +207,19 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * SQL statement removing a set of revisions with from the journal table.
      */
     protected String cleanRevisionStmtSQL;
    

     /**
      * SQL statement returning the local revision of this cluster node.
      */
     protected String getLocalRevisionStmtSQL;
    

     /**
     * SQL statement for inserting the local revision of this cluster node. 
     * SQL statement for inserting the local revision of this cluster node.
      */
     protected String insertLocalRevisionStmtSQL;
 
     /**
     * SQL statement for updating the local revision of this cluster node. 
     * SQL statement for updating the local revision of this cluster node.
      */
     protected String updateLocalRevisionStmtSQL;
 
@@ -291,7 +292,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * This method is called from the {@link #init(String, NamespaceResolver)} method of this class and
      * returns a {@link ConnectionHelper} instance which is assigned to the {@code conHelper} field.
      * Subclasses may override it to return a specialized connection helper.
     * 
     *
      * @param dataSrc the {@link DataSource} of this persistence manager
      * @return a {@link ConnectionHelper}
      * @throws Exception on error
@@ -304,7 +305,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * This method is called from {@link #init(String, NamespaceResolver)} after the
      * {@link #createConnectionHelper(DataSource)} method, and returns a default {@link CheckSchemaOperation}.
      * Subclasses can overrride this implementation to get a customized implementation.
     * 
     *
      * @return a new {@link CheckSchemaOperation} instance
      */
     protected CheckSchemaOperation createCheckSchemaOperation() {
@@ -446,11 +447,11 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
     @Override
     protected void doSync(long startRevision) throws JournalException {
         try {
            conHelper.startBatch();
            startBatch();
             try {
                 super.doSync(startRevision);
             } finally {
                conHelper.endBatch(true);
                endBatch(true);
             }
         } catch (SQLException e) {
             throw new JournalException("Couldn't sync the cluster node", e);
@@ -470,9 +471,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
         boolean succeeded = false;
 
         try {
            if (lockLevel++ == 0) {
                conHelper.startBatch();
            }
            startBatch();
         } catch (SQLException e) {
             throw new JournalException("Unable to set autocommit to false.", e);
         }
@@ -485,7 +484,6 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
             }
             lockedRevision = rs.getLong(1);
             succeeded = true;

         } catch (SQLException e) {
             throw new JournalException("Unable to lock global revision table.", e);
         } finally {
@@ -500,6 +498,16 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * {@inheritDoc}
      */
     protected void doUnlock(boolean successful) {
        endBatch(successful);
    }

    private void startBatch() throws SQLException {
        if (lockLevel++ == 0) {
            conHelper.startBatch();
        }
    }

    private void endBatch(boolean successful) {
         if (--lockLevel == 0) {
             try {
                 conHelper.endBatch(successful);;
@@ -622,7 +630,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
 
     /**
      * Get the database type.
     * 
     *
      * @return the database type
      */
     public String getDatabaseType() {
@@ -633,7 +641,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * Get the database type.
      * @deprecated
      * This method is deprecated; {@link #getDatabaseType} should be used instead.
     * 
     *
      * @return the database type
      */
     public String getSchema() {
@@ -677,7 +685,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
 
     /**
      * Set the database type.
     * 
     *
      * @param databaseType the database type
      */
     public void setDatabaseType(String databaseType) {
@@ -688,7 +696,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
      * Set the database type.
     * @deprecated
     * This method is deprecated; {@link #getDatabaseType} should be used instead.
     * 
     *
      * @param databaseType the database type
      */
     public void setSchema(String databaseType) {
@@ -761,7 +769,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
         private long localRevision;
 
         /**
         * Indicates whether the init method has been called. 
         * Indicates whether the init method has been called.
          */
         private boolean initialized = false;
 
@@ -801,9 +809,6 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
             }
         }
 
        /**
         * {@inheritDoc}
         */
         public synchronized long get() {
             if (!initialized) {
                 throw new IllegalStateException("instance has not yet been initialized");
@@ -811,9 +816,6 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
             return localRevision;
         }
 
        /**
         * {@inheritDoc}
         */
         public synchronized void set(long localRevision) throws JournalException {
 
             if (!initialized) {
@@ -829,11 +831,9 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
                 throw new JournalException("Failed to update local revision.", e);
             }
         }
        
        /**
         * {@inheritDoc}
         */

         public void close() {
            // nothing to do
         }
     }
 
@@ -863,7 +863,7 @@ public class DatabaseJournal extends AbstractJournal implements DatabaseAware {
             }
             log.info("Interrupted: stopping clean-up task.");
         }
        

         /**
          * Cleans old revisions from the clustering table.
          */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
index 266b3aa50..41a6394f6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/util/db/ConnectionHelper.java
@@ -32,9 +32,9 @@ import org.slf4j.LoggerFactory;
  * This class provides convenience methods to execute SQL statements. They can be either executed in isolation
  * or within the context of a JDBC transaction; the so-called <i>batch mode</i> (use the {@link #startBatch()}
  * and {@link #endBatch(boolean)} methods for this).
 * 
 *
  * <p/>
 * 
 *
  * This class contains logic to retry execution of SQL statements. If this helper is <i>not</i> in batch mode
  * and if a statement fails due to an {@code SQLException}, then it is retried. If the {@code block} argument
  * of the constructor call was {@code false} then it is retried only once. Otherwise the statement is retried
@@ -46,14 +46,14 @@ import org.slf4j.LoggerFactory;
  * <li>{@link #update(String, Object[])}</li>
  * <li>{@link #exec(String, Object[], boolean, int)}</li>
  * </ul>
 * 
 *
  * <p/>
 * 
 *
  * This class is not thread-safe and if it is to be used by multiple threads then the clients must make sure
  * that access to this class is properly synchronized.
 * 
 *
  * <p/>
 * 
 *
  * <strong>Implementation note</strong>: The {@code Connection} that is retrieved from the {@code DataSource}
  * in {@link #getConnection()} may be broken. This is so because if an internal {@code DataSource} is used,
  * then this is a commons-dbcp {@code DataSource} with a <code>testWhileIdle</code> validation strategy (see
@@ -64,13 +64,13 @@ import org.slf4j.LoggerFactory;
  */
 public class ConnectionHelper {
 
    private static Logger log = LoggerFactory.getLogger(ConnectionHelper.class);
    static Logger log = LoggerFactory.getLogger(ConnectionHelper.class);
 
     private static final int RETRIES = 1;
 
     private static final int SLEEP_BETWEEN_RETRIES_MS = 100;
 
    private final boolean blockOnConnectionLoss;
    final boolean blockOnConnectionLoss;
 
     private final boolean checkTablesWithUserName;
 
@@ -99,13 +99,13 @@ public class ConnectionHelper {
         checkTablesWithUserName = checkWithUserName;
         blockOnConnectionLoss = block;
     }
    

     /**
      * A utility method that makes sure that <code>identifier</code> does only consist of characters that are
      * allowed in names on the target database. Illegal characters will be escaped as necessary.
     * 
     *
      * This method is not affected by the
     * 
     *
      * @param identifier the identifier to convert to a db specific identifier
      * @return the db-normalized form of the given identifier
      * @throws SQLException if an error occurs
@@ -132,7 +132,7 @@ public class ConnectionHelper {
     /**
      * Called from {@link #prepareDbIdentifier(String)}. Default implementation replaces the illegal
      * characters with their hexadecimal encoding.
     * 
     *
      * @param escaped the escaped db identifier
      * @param c the character to replace
      */
@@ -155,7 +155,7 @@ public class ConnectionHelper {
 
     /**
      * The default implementation returns the {@code extraNameCharacters} provided by the databases metadata.
     * 
     *
      * @return the additional characters for identifiers supported by the db
      * @throws SQLException on error
      */
@@ -171,7 +171,7 @@ public class ConnectionHelper {
 
     /**
      * Checks whether the given table exists in the database.
     * 
     *
      * @param tableName the name of the table
      * @return whether the given table exists
      * @throws SQLException on error
@@ -204,12 +204,12 @@ public class ConnectionHelper {
      * Starts the <i>batch mode</i>. If an {@link SQLException} is thrown, then the batch mode is not started. <p/>
      * <strong>Important:</strong> clients that call this method must make sure that
      * {@link #endBatch(boolean)} is called eventually.
     * 
     *
      * @throws SQLException on error
      */
     public final void startBatch() throws SQLException {
         if (inBatchMode()) {
            throw new IllegalStateException("already in batch mode");
            throw new SQLException("already in batch mode");
         }
         Connection batchConnection = null;
         try {
@@ -228,7 +228,7 @@ public class ConnectionHelper {
 
     /**
      * This method always ends the <i>batch mode</i>.
     * 
     *
      * @param commit whether the changes in the batch should be committed or rolled back
      * @throws SQLException if the commit or rollback of the underlying JDBC Connection threw an {@code
      *             SQLException}
@@ -251,7 +251,7 @@ public class ConnectionHelper {
 
     /**
      * Executes a general SQL statement and immediately closes all resources.
     * 
     *
      * Note: We use a Statement if there are no parameters to avoid a problem on
      * the Oracle 10g JDBC driver w.r.t. :NEW and :OLD keywords that triggers ORA-17041.
      *
@@ -267,11 +267,11 @@ public class ConnectionHelper {
                 reallyExec(sql, params);
                 return null;
             }
            

         }.doTry();
     }
    
    private void reallyExec(String sql, Object... params) throws SQLException {

    void reallyExec(String sql, Object... params) throws SQLException {
         Connection con = null;
         Statement stmt = null;
         try {
@@ -290,7 +290,7 @@ public class ConnectionHelper {
 
     /**
      * Executes an update or delete statement and returns the update count.
     * 
     *
      * @param sql an SQL statement string
      * @param params the parameters for the SQL statement
      * @return the update count
@@ -303,11 +303,11 @@ public class ConnectionHelper {
             protected Integer call() throws SQLException {
                 return reallyUpdate(sql, params);
             }
            

         }.doTry();
     }
 
    private int reallyUpdate(String sql, Object[] params) throws SQLException {
    int reallyUpdate(String sql, Object[] params) throws SQLException {
         Connection con = null;
         PreparedStatement stmt = null;
         try {
@@ -322,7 +322,7 @@ public class ConnectionHelper {
     /**
      * Executes a general SQL statement and returns the {@link ResultSet} of the executed statement. The
      * returned {@link ResultSet} should be closed by clients.
     * 
     *
      * @param sql an SQL statement string
      * @param params the parameters for the SQL statement
      * @param returnGeneratedKeys whether generated keys should be returned
@@ -338,11 +338,11 @@ public class ConnectionHelper {
             protected ResultSet call() throws SQLException {
                 return reallyExec(sql, params, returnGeneratedKeys, maxRows);
             }
            

         }.doTry();
     }
    
    private ResultSet reallyExec(String sql, Object[] params, boolean returnGeneratedKeys, int maxRows)

    ResultSet reallyExec(String sql, Object[] params, boolean returnGeneratedKeys, int maxRows)
             throws SQLException {
         Connection con = null;
         PreparedStatement stmt = null;
@@ -381,7 +381,7 @@ public class ConnectionHelper {
      * Gets a connection based on the {@code batchMode} state of this helper. The connection should be closed
      * by a call to {@link #closeResources(Connection, Statement, ResultSet)} which also takes the {@code
      * batchMode} state into account.
     * 
     *
      * @return a {@code Connection} to use, based on the batch mode state
      * @throws SQLException on error
      */
@@ -400,7 +400,7 @@ public class ConnectionHelper {
 
     /**
      * Closes the given resources given the {@code batchMode} state.
     * 
     *
      * @param con the {@code Connection} obtained through the {@link #getConnection()} method
      * @param stmt a {@code Statement}
      * @param rs a {@code ResultSet}
@@ -418,7 +418,7 @@ public class ConnectionHelper {
      * implementation sets all parameters and unwraps {@link StreamWrapper} instances. Subclasses may override
      * this method to do something special with the parameters. E.g., the {@link Oracle10R1ConnectionHelper}
      * overrides it in order to add special blob handling.
     * 
     *
      * @param stmt the {@link PreparedStatement} to execute
      * @param params the parameters
      * @return the executed statement
@@ -438,10 +438,10 @@ public class ConnectionHelper {
         stmt.execute();
         return stmt;
     }
    

     /**
      * This class encapsulates the logic to retry a method invocation if it threw an SQLException.
     * 
     *
      * @param <T> the return type of the method which is retried if it failed
      */
     public abstract class RetryManager<T> {
@@ -475,7 +475,7 @@ public class ConnectionHelper {
                 throw lastException;
             }
         }
        

         protected abstract T call() throws SQLException;
     }
 }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/DbClusterTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/DbClusterTest.java
new file mode 100644
index 000000000..78d4ad772
-- /dev/null
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/DbClusterTest.java
@@ -0,0 +1,80 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.cluster;

import java.io.File;
import java.io.IOException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;
import org.h2.tools.Server;

/**
 * Tests clustering with a database.
 */
public class DbClusterTest extends JUnitTest {

    Server server1, server2;

    public void setUp() throws Exception {
        deleteAll();
        server1 = Server.createTcpServer("-tcpPort", "9001", "-baseDir",
                "./target/dbClusterTest/db1").start();
        server2 = Server.createTcpServer("-tcpPort", "9002", "-baseDir",
                "./target/dbClusterTest/db2").start();
        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/dbClusterTest/node1/repository.xml"));
        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/dbClusterTest/node2/repository.xml"));
    }

    public void tearDown() throws Exception {
        server1.stop();
        server2.stop();
        deleteAll();
    }

    private void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File("./target/dbClusterTest"));
    }

    public void test() throws RepositoryException {
        RepositoryImpl rep1 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/dbClusterTest/node1")));
        RepositoryImpl rep2 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/dbClusterTest/node2")));
        Session s1 = rep1.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Session s2 = rep2.login(new SimpleCredentials("admin", "admin".toCharArray()));
        s1.getRootNode().addNode("test1");
        s2.getRootNode().addNode("test2");
        s1.save();
        s2.save();
        s1.refresh(true);
        s2.refresh(true);
        s1.getRootNode().getNode("test2");
        s2.getRootNode().getNode("test1");
        rep1.shutdown();
        rep2.shutdown();
    }

}
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
index 8e7983e59..da45f3719 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/cluster/TestAll.java
@@ -38,6 +38,7 @@ public class TestAll extends TestCase {
         TestSuite suite = new TestSuite();
 
         suite.addTestSuite(ClusterRecordTest.class);
        suite.addTestSuite(DbClusterTest.class);
 
         return suite;
     }
diff --git a/jackrabbit-core/src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml b/jackrabbit-core/src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml
new file mode 100644
index 000000000..d15f78708
-- /dev/null
++ b/jackrabbit-core/src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml
@@ -0,0 +1,171 @@
<?xml version="1.0"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<!DOCTYPE Repository PUBLIC "-//The Apache Software Foundation//DTD Jackrabbit 1.6//EN"
                            "http://jackrabbit.apache.org/dtd/repository-1.6.dtd">
<!-- Example Repository Configuration File
     Used by
     - org.apache.jackrabbit.core.config.RepositoryConfigTest.java
     -
-->
<Repository>
    <!--
        virtual file system where the repository stores global state
        (e.g. registered namespaces, custom node types, etc.)
    -->
    <FileSystem class="org.apache.jackrabbit.core.fs.db.DbFileSystem">
        <param name="url" value="jdbc:h2:tcp://localhost:9001,localhost:9002/db"/>
        <param name="schemaObjectPrefix" value="fs_"/>
        <param name="user" value="sa"/>
        <param name="password" value="sa"/>
    </FileSystem>

    <!--
        data store configuration
    -->
    <DataStore class="org.apache.jackrabbit.core.data.db.DbDataStore">
        <param name="url" value="jdbc:h2:tcp://localhost:9001,localhost:9002/db"/>
        <param name="schemaObjectPrefix" value="datastore_"/>
        <param name="user" value="sa"/>
        <param name="password" value="sa"/>
    </DataStore>

    <!--
        security configuration
    -->
    <Security appName="Jackrabbit">
        <!--
            security manager:
            class: FQN of class implementing the JackrabbitSecurityManager interface
        -->
        <SecurityManager class="org.apache.jackrabbit.core.security.simple.SimpleSecurityManager" workspaceName="security">
            <!--
            workspace access:
            class: FQN of class implementing the WorkspaceAccessManager interface
            -->
            <!-- <WorkspaceAccessManager class="..."/> -->
            <!-- <param name="config" value="${rep.home}/security.xml"/> -->
        </SecurityManager>

        <!--
            access manager:
            class: FQN of class implementing the AccessManager interface
        -->
        <AccessManager class="org.apache.jackrabbit.core.security.simple.SimpleAccessManager">
            <!-- <param name="config" value="${rep.home}/access.xml"/> -->
        </AccessManager>

        <LoginModule class="org.apache.jackrabbit.core.security.simple.SimpleLoginModule">
           <!-- 
              anonymous user name ('anonymous' is the default value)
            -->
           <param name="anonymousId" value="anonymous"/>
           <!--
              administrator user id (default value if param is missing is 'admin')
            -->
           <param name="adminId" value="admin"/>
        </LoginModule>
    </Security>

    <!--
        location of workspaces root directory and name of default workspace
    -->
    <Workspaces rootPath="${rep.home}/workspaces" defaultWorkspace="default"/>
    <!--
        workspace configuration template:
        used to create the initial workspace if there's no workspace yet
    -->
    <Workspace name="${wsp.name}">
        <!--
            virtual file system of the workspace:
            class: FQN of class implementing the FileSystem interface
        -->
        <FileSystem class="org.apache.jackrabbit.core.fs.local.LocalFileSystem">
            <param name="path" value="${wsp.home}"/>
        </FileSystem>
        
        <!--
            persistence manager of the workspace:
            class: FQN of class implementing the PersistenceManager interface
        -->
        <PersistenceManager class="org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager">
            <param name="url" value="jdbc:h2:tcp://localhost:9001,localhost:9002/db"/>
            <param name="schemaObjectPrefix" value="ws_${wsp.name}_"/>
            <param name="user" value="sa"/>
            <param name="password" value="sa"/>
        </PersistenceManager>
        
        <!--
            Search index and the file system it uses.
            class: FQN of class implementing the QueryHandler interface
        -->
        <SearchIndex class="org.apache.jackrabbit.core.query.lucene.SearchIndex">
            <param name="path" value="${wsp.home}/index"/>
            <param name="supportHighlighting" value="true"/>            
        </SearchIndex>
    </Workspace>

    <!--
        Configures the versioning
    -->
    <Versioning rootPath="${rep.home}/version">
        <!--
            Configures the filesystem to use for versioning for the respective
            persistence manager
        -->
        <FileSystem class="org.apache.jackrabbit.core.fs.local.LocalFileSystem">
            <param name="path" value="${rep.home}/version" />
        </FileSystem>

        <!--
            Configures the persistence manager to be used for persisting version state.
            Please note that the current versioning implementation is based on
            a 'normal' persistence manager, but this could change in future
            implementations.
        -->
        <PersistenceManager class="org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager">
            <param name="url" value="jdbc:h2:tcp://localhost:9001,localhost:9002/db"/>
            <param name="schemaObjectPrefix" value="version_"/>
            <param name="user" value="sa"/>
            <param name="password" value="sa"/>
        </PersistenceManager>
    </Versioning>

    <!--
        Search index for content that is shared repository wide
        (/jcr:system tree, contains mainly versions)
    -->
    <SearchIndex class="org.apache.jackrabbit.core.query.lucene.SearchIndex">
        <param name="path" value="${rep.home}/repository/index"/>
        <param name="supportHighlighting" value="true"/>
    </SearchIndex>
    
    <!--
        Cluster configuration with system variables.
    -->
    <Cluster>
        <Journal class="org.apache.jackrabbit.core.journal.DatabaseJournal">
            <param name="driver" value="org.h2.Driver" />
            <param name="url" value="jdbc:h2:tcp://localhost:9001,localhost:9002/db"/>
            <param name="schemaObjectPrefix" value="journal_"/>
            <param name="databaseType" value="h2"/>
            <param name="user" value="sa"/>
            <param name="password" value="sa"/>
        </Journal>
    </Cluster>
    
</Repository>
- 
2.19.1.windows.1

