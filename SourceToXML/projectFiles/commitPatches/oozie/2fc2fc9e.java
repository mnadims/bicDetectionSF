From 2fc2fc9ec5eb9d14edc449284c9e07090a8ece9a Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Wed, 5 Nov 2014 17:19:02 -0800
Subject: [PATCH] OOZIE-1890 Make oozie-site empty and reconcile defaults
 between oozie-default and the code (seoeun25 via rkanter)

--
 core/src/main/conf/oozie-site.xml             | 303 ------------------
 .../org/apache/oozie/CoordinatorEngine.java   |   6 -
 .../main/java/org/apache/oozie/DagEngine.java |   7 +-
 .../apache/oozie/action/ActionExecutor.java   |   4 +-
 .../action/hadoop/CredentialsProvider.java    |  45 ++-
 .../action/hadoop/DistcpActionExecutor.java   |  18 +-
 .../oozie/action/hadoop/FsActionExecutor.java |   4 +-
 .../action/hadoop/JavaActionExecutor.java     |  22 +-
 .../hadoop/MapReduceActionExecutor.java       |   4 +-
 .../oozie/action/hadoop/OozieJobInfo.java     |   4 +-
 .../oozie/SubWorkflowActionExecutor.java      |   3 +-
 .../oozie/action/ssh/SshActionExecutor.java   |   9 +-
 .../org/apache/oozie/command/XCommand.java    |   3 +-
 .../coord/CoordActionInputCheckXCommand.java  |  13 +-
 .../CoordActionNotificationXCommand.java      |   6 +-
 .../CoordMaterializeTransitionXCommand.java   |   9 +-
 .../CoordPushDependencyCheckXCommand.java     |   9 +-
 .../command/coord/CoordSubmitXCommand.java    |  27 +-
 .../apache/oozie/command/wf/JobXCommand.java  |   5 +-
 .../command/wf/NotificationXCommand.java      |   5 +-
 .../apache/oozie/event/MemoryEventQueue.java  |   5 +-
 .../event/listener/ZKConnectionListener.java  |   3 +-
 .../jpa/CoordActionGetForInfoJPAExecutor.java |   3 +-
 .../apache/oozie/jms/JMSJobEventListener.java |   3 +-
 .../service/AbandonedCoordCheckerService.java |  40 +--
 .../oozie/service/ActionCheckerService.java   |  10 +-
 .../apache/oozie/service/ActionService.java   |   6 +-
 .../oozie/service/AuthorizationService.java   |   2 +-
 .../oozie/service/CallableQueueService.java   |  12 +-
 .../apache/oozie/service/CallbackService.java |   2 +-
 .../oozie/service/ConfigurationService.java   | 175 +++++++++-
 .../CoordMaterializeTriggerService.java       |  15 +-
 .../service/DBLiteWorkflowStoreService.java   |   4 +-
 .../org/apache/oozie/service/ELService.java   |   6 +-
 .../oozie/service/EventHandlerService.java    |  13 +-
 .../oozie/service/HCatAccessorService.java    |   2 +-
 .../oozie/service/HadoopAccessorService.java  |  23 +-
 .../oozie/service/InstrumentationService.java |   2 +-
 .../oozie/service/JMSAccessorService.java     |   2 +-
 .../apache/oozie/service/JMSTopicService.java |   2 +-
 .../org/apache/oozie/service/JPAService.java  |  24 +-
 .../oozie/service/JvmPauseMonitorService.java |  12 +-
 .../service/LiteWorkflowStoreService.java     |  14 +-
 .../oozie/service/PauseTransitService.java    |   8 +-
 .../apache/oozie/service/PurgeService.java    |  12 +-
 .../apache/oozie/service/RecoveryService.java |  12 +-
 .../oozie/service/SchedulerService.java       |   2 +-
 .../apache/oozie/service/SchemaService.java   |   3 +-
 .../org/apache/oozie/service/Service.java     |   7 +-
 .../org/apache/oozie/service/Services.java    |  12 +-
 .../apache/oozie/service/ShareLibService.java |  17 +-
 .../oozie/service/StatusTransitService.java   |   2 +-
 .../oozie/service/URIHandlerService.java      |   2 +-
 .../org/apache/oozie/service/UUIDService.java |   4 +-
 .../oozie/service/WorkflowAppService.java     |   2 +-
 .../oozie/service/XLogStreamingService.java   |   2 +-
 .../apache/oozie/service/ZKLocksService.java  |   5 +-
 .../org/apache/oozie/servlet/AuthFilter.java  |   2 +-
 .../apache/oozie/servlet/CallbackServlet.java |   3 +-
 .../apache/oozie/servlet/V1JobServlet.java    |   3 +-
 .../apache/oozie/sla/SLACalculatorMemory.java |   5 +-
 .../sla/listener/SLAEmailEventListener.java   |   5 +-
 .../apache/oozie/sla/service/SLAService.java  |   9 +-
 .../org/apache/oozie/util/ConfigUtils.java    |  19 +-
 .../java/org/apache/oozie/util/DateUtils.java |   3 +-
 .../org/apache/oozie/util/StatusUtils.java    |  23 +-
 .../org/apache/oozie/util/XLogFilter.java     |   5 +-
 .../java/org/apache/oozie/util/ZKUtils.java   |  12 +-
 .../workflow/lite/LiteWorkflowAppParser.java  |   4 +-
 core/src/main/resources/oozie-default.xml     | 219 +++++++++----
 .../action/email/TestEmailActionExecutor.java |   6 -
 .../hadoop/TestDistCpActionExecutor.java      |   6 -
 .../hadoop/TestShellActionExecutor.java       |   7 -
 .../TestCoordActionNotificationXCommand.java  |   2 +-
 .../command/wf/TestNotificationXCommand.java  |   2 +-
 .../oozie/command/wf/TestReRunXCommand.java   |   1 -
 .../service/TestConfigurationService.java     | 159 ++++++++-
 .../service/TestJobsConcurrencyService.java   |   2 +-
 .../oozie/sla/TestSLACalculatorMemory.java    |  32 +-
 .../lite/TestLiteWorkflowAppParser.java       |  15 +-
 .../test/resources/wf-unsupported-action.xml  |  17 +-
 release-log.txt                               |   1 +
 .../action/hadoop/TestHiveActionExecutor.java |   6 -
 .../oozie/action/hadoop/LauncherMapper.java   |   6 +-
 .../hadoop/TestSqoopActionExecutor.java       |   6 -
 85 files changed, 803 insertions(+), 753 deletions(-)

diff --git a/core/src/main/conf/oozie-site.xml b/core/src/main/conf/oozie-site.xml
index c028ca225..a882715a8 100644
-- a/core/src/main/conf/oozie-site.xml
++ b/core/src/main/conf/oozie-site.xml
@@ -23,309 +23,6 @@
         Oozie configuration properties and their default values.
     -->
 
    <property>
        <name>oozie.service.ActionService.executor.ext.classes</name>
        <value>
            org.apache.oozie.action.email.EmailActionExecutor,
            org.apache.oozie.action.hadoop.HiveActionExecutor,
            org.apache.oozie.action.hadoop.ShellActionExecutor,
            org.apache.oozie.action.hadoop.SqoopActionExecutor,
            org.apache.oozie.action.hadoop.DistcpActionExecutor,
            org.apache.oozie.action.hadoop.Hive2ActionExecutor
        </value>
    </property>

    <property>
        <name>oozie.service.SchemaService.wf.ext.schemas</name>
        <value>
            shell-action-0.1.xsd,shell-action-0.2.xsd,shell-action-0.3.xsd,email-action-0.1.xsd,email-action-0.2.xsd,
            hive-action-0.2.xsd,hive-action-0.3.xsd,hive-action-0.4.xsd,hive-action-0.5.xsd,sqoop-action-0.2.xsd,
            sqoop-action-0.3.xsd,sqoop-action-0.4.xsd,ssh-action-0.1.xsd,ssh-action-0.2.xsd,distcp-action-0.1.xsd,
            distcp-action-0.2.xsd,oozie-sla-0.1.xsd,oozie-sla-0.2.xsd,hive2-action-0.1.xsd
        </value>
    </property>

    <property>
        <name>oozie.system.id</name>
        <value>oozie-${user.name}</value>
        <description>
            The Oozie system ID.
        </description>
    </property>

    <property>
        <name>oozie.systemmode</name>
        <value>NORMAL</value>
        <description>
            System mode for  Oozie at startup.
        </description>
    </property>

    <property>
        <name>oozie.service.AuthorizationService.security.enabled</name>
        <value>false</value>
        <description>
            Specifies whether security (user name/admin role) is enabled or not.
            If disabled any user can manage Oozie system and manage any job.
        </description>
    </property>

    <property>
        <name>oozie.service.PurgeService.older.than</name>
        <value>30</value>
        <description>
            Jobs older than this value, in days, will be purged by the PurgeService.
        </description>
    </property>

    <property>
        <name>oozie.service.PurgeService.purge.interval</name>
        <value>3600</value>
        <description>
            Interval at which the purge service will run, in seconds.
        </description>
    </property>

    <property>
        <name>oozie.service.CallableQueueService.queue.size</name>
        <value>10000</value>
        <description>Max callable queue size</description>
    </property>

    <property>
        <name>oozie.service.CallableQueueService.threads</name>
        <value>10</value>
        <description>Number of threads used for executing callables</description>
    </property>

    <property>
        <name>oozie.service.CallableQueueService.callable.concurrency</name>
        <value>3</value>
        <description>
            Maximum concurrency for a given callable type.
            Each command is a callable type (submit, start, run, signal, job, jobs, suspend,resume, etc).
            Each action type is a callable type (Map-Reduce, Pig, SSH, FS, sub-workflow, etc).
            All commands that use action executors (action-start, action-end, action-kill and action-check) use
            the action type as the callable type.
        </description>
    </property>

    <property>
		<name>oozie.service.coord.normal.default.timeout
		</name>
		<value>120</value>
		<description>Default timeout for a coordinator action input check (in minutes) for normal job.
            -1 means infinite timeout</description>
	</property>

    <property>
        <name>oozie.db.schema.name</name>
        <value>oozie</value>
        <description>
            Oozie DataBase Name
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.create.db.schema</name>
        <value>false</value>
        <description>
            Creates Oozie DB.

            If set to true, it creates the DB schema if it does not exist. If the DB schema exists is a NOP.
            If set to false, it does not create the DB schema. If the DB schema does not exist it fails start up.
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.jdbc.driver</name>
        <value>org.apache.derby.jdbc.EmbeddedDriver</value>
        <description>
            JDBC driver class.
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.jdbc.url</name>
        <value>jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true</value>
        <description>
            JDBC URL.
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.jdbc.username</name>
        <value>sa</value>
        <description>
            DB user name.
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.jdbc.password</name>
        <value> </value>
        <description>
            DB user password.

            IMPORTANT: if password is emtpy leave a 1 space string, the service trims the value,
                       if empty Configuration assumes it is NULL.
        </description>
    </property>

    <property>
        <name>oozie.service.JPAService.pool.max.active.conn</name>
        <value>10</value>
        <description>
             Max number of connections.
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.kerberos.enabled</name>
        <value>false</value>
        <description>
            Indicates if Oozie is configured to use Kerberos.
        </description>
    </property>

    <property>
        <name>local.realm</name>
        <value>LOCALHOST</value>
        <description>
            Kerberos Realm used by Oozie and Hadoop. Using 'local.realm' to be aligned with Hadoop configuration
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.keytab.file</name>
        <value>${user.home}/oozie.keytab</value>
        <description>
            Location of the Oozie user keytab file.
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.kerberos.principal</name>
        <value>${user.name}/localhost@${local.realm}</value>
        <description>
            Kerberos principal for Oozie service.
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.jobTracker.whitelist</name>
        <value> </value>
        <description>
            Whitelisted job tracker for Oozie service.
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.nameNode.whitelist</name>
        <value> </value>
        <description>
            Whitelisted job tracker for Oozie service.
        </description>
    </property>

    <property>
        <name>oozie.service.HadoopAccessorService.hadoop.configurations</name>
        <value>*=hadoop-conf</value>
        <description>
            Comma separated AUTHORITY=HADOOP_CONF_DIR, where AUTHORITY is the HOST:PORT of
            the Hadoop service (JobTracker, HDFS). The wildcard '*' configuration is
            used when there is no exact match for an authority. The HADOOP_CONF_DIR contains
            the relevant Hadoop *-site.xml files. If the path is relative is looked within
            the Oozie configuration directory; though the path can be absolute (i.e. to point
            to Hadoop client conf/ directories in the local filesystem.
        </description>
    </property>

    <property>
        <name>oozie.service.WorkflowAppService.system.libpath</name>
        <value>/user/${user.name}/share/lib</value>
        <description>
            System library path to use for workflow applications.
            This path is added to workflow application if their job properties sets
            the property 'oozie.use.system.libpath' to true.
        </description>
    </property>

    <property>
        <name>use.system.libpath.for.mapreduce.and.pig.jobs</name>
        <value>false</value>
        <description>
            If set to true, submissions of MapReduce and Pig jobs will include
            automatically the system library path, thus not requiring users to
            specify where the Pig JAR files are. Instead, the ones from the system
            library path are used.
        </description>
    </property>

    <property>
        <name>oozie.authentication.type</name>
        <value>simple</value>
        <description>
            Defines authentication used for Oozie HTTP endpoint.
            Supported values are: simple | kerberos | #AUTHENTICATION_HANDLER_CLASSNAME#
        </description>
    </property>

    <property>
        <name>oozie.authentication.token.validity</name>
        <value>36000</value>
        <description>
            Indicates how long (in seconds) an authentication token is valid before it has
            to be renewed.
        </description>
    </property>

    <property>
      <name>oozie.authentication.cookie.domain</name>
      <value></value>
      <description>
        The domain to use for the HTTP cookie that stores the authentication token.
        In order to authentiation to work correctly across multiple hosts
        the domain must be correctly set.
      </description>
    </property>

    <property>
        <name>oozie.authentication.simple.anonymous.allowed</name>
        <value>true</value>
        <description>
            Indicates if anonymous requests are allowed.
            This setting is meaningful only when using 'simple' authentication.
        </description>
    </property>

    <property>
        <name>oozie.authentication.kerberos.principal</name>
        <value>HTTP/localhost@${local.realm}</value>
        <description>
            Indicates the Kerberos principal to be used for HTTP endpoint.
            The principal MUST start with 'HTTP/' as per Kerberos HTTP SPNEGO specification.
        </description>
    </property>

    <property>
        <name>oozie.authentication.kerberos.keytab</name>
        <value>${oozie.service.HadoopAccessorService.keytab.file}</value>
        <description>
            Location of the keytab file with the credentials for the principal.
            Referring to the same keytab file Oozie uses for its Kerberos credentials for Hadoop.
        </description>
    </property>

    <property>
        <name>oozie.authentication.kerberos.name.rules</name>
        <value>DEFAULT</value>
        <description>
            The kerberos names rules is to resolve kerberos principal names, refer to Hadoop's
            KerberosName for more details.
        </description>
    </property>

     <!-- Proxyuser Configuration -->
 
     <!--
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
index 8591d639a..a893e0c12 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
@@ -99,12 +99,6 @@ public class CoordinatorEngine extends BaseEngine {
      * Create a system Coordinator engine, with no user and no group.
      */
     public CoordinatorEngine() {
        if (!Services.get().getConf().getBoolean(USE_XCOMMAND, true)) {
            LOG.debug("Oozie CoordinatorEngine is not using XCommands.");
        }
        else {
            LOG.debug("Oozie CoordinatorEngine is using XCommands.");
        }
         maxNumActionsForLog = Services.get().getConf()
                 .getInt(COORD_ACTIONS_LOG_MAX_COUNT, COORD_ACTIONS_LOG_MAX_COUNT_DEFAULT);
     }
diff --git a/core/src/main/java/org/apache/oozie/DagEngine.java b/core/src/main/java/org/apache/oozie/DagEngine.java
index bea312fd0..70ddd4473 100644
-- a/core/src/main/java/org/apache/oozie/DagEngine.java
++ b/core/src/main/java/org/apache/oozie/DagEngine.java
@@ -80,12 +80,7 @@ public class DagEngine extends BaseEngine {
      * Create a system Dag engine, with no user and no group.
      */
     public DagEngine() {
        if (Services.get().getConf().getBoolean(USE_XCOMMAND, true) == false) {
            LOG.debug("Oozie DagEngine is not using XCommands.");
        }
        else {
            LOG.debug("Oozie DagEngine is using XCommands.");
        }

     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/action/ActionExecutor.java b/core/src/main/java/org/apache/oozie/action/ActionExecutor.java
index 2053f32bb..ff836fbbb 100644
-- a/core/src/main/java/org/apache/oozie/action/ActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/ActionExecutor.java
@@ -23,6 +23,7 @@ import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.util.ELEvaluator;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
@@ -228,12 +229,11 @@ public abstract class ActionExecutor {
      * Create an action executor.
      *
      * @param type action executor type.
     * @param retryAttempts retry attempts.
      * @param retryInterval retry interval, in seconds.
      */
     protected ActionExecutor(String type, long retryInterval) {
         this.type = ParamChecker.notEmpty(type, "type");
        this.maxRetries = getOozieConf().getInt(MAX_RETRIES, 3);
        this.maxRetries = ConfigurationService.getInt(MAX_RETRIES);
         this.retryInterval = retryInterval;
     }
 
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/CredentialsProvider.java b/core/src/main/java/org/apache/oozie/action/hadoop/CredentialsProvider.java
index 9c66e5859..ddf7fd4c0 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/CredentialsProvider.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/CredentialsProvider.java
@@ -20,13 +20,14 @@ package org.apache.oozie.action.hadoop;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.util.ReflectionUtils;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.XLog;
 
 public class CredentialsProvider {
     Credentials cred;
     String type;
    private static final String CRED_KEY = "oozie.credentials.credentialclasses";
    public static final String CRED_KEY = "oozie.credentials.credentialclasses";
     private static final XLog LOG = XLog.getLog(CredentialsProvider.class);
 
     public CredentialsProvider(String type) {
@@ -42,32 +43,28 @@ public class CredentialsProvider {
      * @throws Exception
      */
     public Credentials createCredentialObject() throws Exception {
        Configuration conf;
         String type;
         String classname;
        conf = Services.get().getConf();
        if (conf.get(CRED_KEY, "").trim().length() > 0) {
            for (String function : conf.getStrings(CRED_KEY)) {
                function = Trim(function);
                LOG.debug("Creating Credential class for : " + function);
                String[] str = function.split("=");
                if (str.length > 0) {
                    type = str[0];
                    classname = str[1];
                    if (classname != null) {
                        LOG.debug("Creating Credential type : '" + type + "', class Name : '" + classname + "'");
                        if (this.type.equalsIgnoreCase(str[0])) {
                            Class<?> klass = null;
                            try {
                                klass = Thread.currentThread().getContextClassLoader().loadClass(classname);
                            }
                            catch (ClassNotFoundException ex) {
                                LOG.warn("Exception while loading the class", ex);
                                throw ex;
                            }

                            cred = (Credentials) ReflectionUtils.newInstance(klass, null);
        for (String function : ConfigurationService.getStrings(CRED_KEY)) {
            function = Trim(function);
            LOG.debug("Creating Credential class for : " + function);
            String[] str = function.split("=");
            if (str.length > 0) {
                type = str[0];
                classname = str[1];
                if (classname != null) {
                    LOG.debug("Creating Credential type : '" + type + "', class Name : '" + classname + "'");
                    if (this.type.equalsIgnoreCase(str[0])) {
                        Class<?> klass = null;
                        try {
                            klass = Thread.currentThread().getContextClassLoader().loadClass(classname);
                        }
                        catch (ClassNotFoundException ex) {
                            LOG.warn("Exception while loading the class", ex);
                            throw ex;
                         }

                        cred = (Credentials) ReflectionUtils.newInstance(klass, null);
                     }
                 }
             }
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
index 4d2f7b282..42f296592 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/DistcpActionExecutor.java
@@ -24,6 +24,7 @@ import java.util.List;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.fs.Path;
 import org.apache.oozie.action.ActionExecutorException;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.XLog;
 import org.jdom.Element;
@@ -71,17 +72,14 @@ public class DistcpActionExecutor extends JavaActionExecutor{
      * @return Name of the class from the configuration
      */
     public static String getClassNamebyType(String type){
        Configuration conf = Services.get().getConf();
         String classname = null;
        if (conf.get(CLASS_NAMES, "").trim().length() > 0) {
            for (String function : conf.getStrings(CLASS_NAMES)) {
                function = DistcpActionExecutor.Trim(function);
                LOG.debug("class for Distcp Action: " + function);
                String[] str = function.split("=");
                if (str.length > 0) {
                    if(type.equalsIgnoreCase(str[0])){
                        classname = new String(str[1]);
                    }
        for (String function : ConfigurationService.getStrings(CLASS_NAMES)) {
            function = DistcpActionExecutor.Trim(function);
            LOG.debug("class for Distcp Action: " + function);
            String[] str = function.split("=");
            if (str.length > 0) {
                if(type.equalsIgnoreCase(str[0])){
                    classname = new String(str[1]);
                 }
             }
         }
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/FsActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/FsActionExecutor.java
index 6a7f817ac..fed1d7ace 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/FsActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/FsActionExecutor.java
@@ -35,6 +35,7 @@ import org.apache.hadoop.mapred.JobConf;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.service.HadoopAccessorService;
 import org.apache.oozie.service.Services;
@@ -51,8 +52,7 @@ public class FsActionExecutor extends ActionExecutor {
 
     public FsActionExecutor() {
         super("fs");
        maxGlobCount = getOozieConf().getInt(LauncherMapper.CONF_OOZIE_ACTION_FS_GLOB_MAX,
                LauncherMapper.GLOB_MAX_DEFAULT);
        maxGlobCount = ConfigurationService.getInt(LauncherMapper.CONF_OOZIE_ACTION_FS_GLOB_MAX);
     }
 
     Path getPath(Element element, String attribute) {
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 201cfa319..7349d3f70 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -59,6 +59,7 @@ import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.service.HadoopAccessorService;
 import org.apache.oozie.service.Services;
@@ -89,7 +90,6 @@ public class JavaActionExecutor extends ActionExecutor {
     public static final String HADOOP_NAME_NODE = "fs.default.name";
     private static final String HADOOP_JOB_NAME = "mapred.job.name";
     public static final String OOZIE_COMMON_LIBDIR = "oozie";
    public static final int MAX_EXTERNAL_STATS_SIZE_DEFAULT = Integer.MAX_VALUE;
     private static final Set<String> DISALLOWED_PROPERTIES = new HashSet<String>();
     public final static String MAX_EXTERNAL_STATS_SIZE = "oozie.external.stats.max.size";
     public static final String ACL_VIEW_JOB = "mapreduce.job.acl-view-job";
@@ -114,6 +114,7 @@ public class JavaActionExecutor extends ActionExecutor {
     private static final String FAILED_KILLED = "FAILED/KILLED";
     protected XLog LOG = XLog.getLog(getClass());
     private static final Pattern heapPattern = Pattern.compile("-Xmx(([0-9]+)[mMgG])");
    public static final String CONF_HADOOP_YARN_UBER_MODE = "oozie.action.launcher." + HADOOP_YARN_UBER_MODE;
 
     static {
         DISALLOWED_PROPERTIES.add(HADOOP_USER);
@@ -155,19 +156,12 @@ public class JavaActionExecutor extends ActionExecutor {
     @Override
     public void initActionType() {
         super.initActionType();
        maxActionOutputLen = getOozieConf()
          .getInt(LauncherMapper.CONF_OOZIE_ACTION_MAX_OUTPUT_DATA,
          // TODO: Remove the below config get in a subsequent release..
          // This other irrelevant property is only used to
          // preserve backwards compatibility cause of a typo.
                  // See OOZIE-4.
          getOozieConf().getInt(CallbackServlet.CONF_MAX_DATA_LEN,
            2 * 1024));
        maxActionOutputLen = ConfigurationService.getInt(LauncherMapper.CONF_OOZIE_ACTION_MAX_OUTPUT_DATA);
         //Get the limit for the maximum allowed size of action stats
        maxExternalStatsSize = getOozieConf().getInt(JavaActionExecutor.MAX_EXTERNAL_STATS_SIZE, MAX_EXTERNAL_STATS_SIZE_DEFAULT);
        maxExternalStatsSize = ConfigurationService.getInt(JavaActionExecutor.MAX_EXTERNAL_STATS_SIZE);
         maxExternalStatsSize = (maxExternalStatsSize == -1) ? Integer.MAX_VALUE : maxExternalStatsSize;
         //Get the limit for the maximum number of globbed files/dirs for FS operation
        maxFSGlobMax = getOozieConf().getInt(LauncherMapper.CONF_OOZIE_ACTION_FS_GLOB_MAX, LauncherMapper.GLOB_MAX_DEFAULT);
        maxFSGlobMax = ConfigurationService.getInt(LauncherMapper.CONF_OOZIE_ACTION_FS_GLOB_MAX);
 
         registerError(UnknownHostException.class.getName(), ActionExecutorException.ErrorType.TRANSIENT, "JA001");
         registerError(AccessControlException.class.getName(), ActionExecutorException.ErrorType.NON_TRANSIENT,
@@ -267,7 +261,7 @@ public class JavaActionExecutor extends ActionExecutor {
     void injectLauncherUseUberMode(Configuration launcherConf) {
         // Set Uber Mode for the launcher (YARN only, ignored by MR1) if not set by action conf and not disabled in oozie-site
         if (launcherConf.get(HADOOP_YARN_UBER_MODE) == null) {
            if (getOozieConf().getBoolean("oozie.action.launcher.mapreduce.job.ubertask.enable", false)) {
            if (ConfigurationService.getBoolean(getOozieConf(), CONF_HADOOP_YARN_UBER_MODE)) {
                 launcherConf.setBoolean(HADOOP_YARN_UBER_MODE, true);
             }
         }
@@ -798,9 +792,7 @@ public class JavaActionExecutor extends ActionExecutor {
             LauncherMapperHelper.setupLauncherURIHandlerConf(launcherJobConf);
             LauncherMapperHelper.setupMaxOutputData(launcherJobConf, maxActionOutputLen);
             LauncherMapperHelper.setupMaxExternalStatsSize(launcherJobConf, maxExternalStatsSize);
            if (getOozieConf().get(LauncherMapper.CONF_OOZIE_ACTION_FS_GLOB_MAX) != null) {
                LauncherMapperHelper.setupMaxFSGlob(launcherJobConf, maxFSGlobMax);
            }
            LauncherMapperHelper.setupMaxFSGlob(launcherJobConf, maxFSGlobMax);
 
             List<Element> list = actionXml.getChildren("arg", ns);
             String[] args = new String[list.size()];
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
index 5cba73206..65a4ed255 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/MapReduceActionExecutor.java
@@ -33,7 +33,7 @@ import org.apache.hadoop.mapred.JobID;
 import org.apache.hadoop.mapred.RunningJob;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.util.XConfiguration;
 import org.apache.oozie.util.XLog;
 import org.apache.oozie.util.XmlUtils;
@@ -136,7 +136,7 @@ public class MapReduceActionExecutor extends JavaActionExecutor {
             // Resolve uber jar path (has to be done after super because oozie.mapreduce.uber.jar is under <configuration>)
             String uberJar = actionConf.get(MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR);
             if (uberJar != null) {
                if (!Services.get().getConf().getBoolean(OOZIE_MAPREDUCE_UBER_JAR_ENABLE, false)) {
                if (!ConfigurationService.getBoolean(OOZIE_MAPREDUCE_UBER_JAR_ENABLE)){
                     throw new ActionExecutorException(ActionExecutorException.ErrorType.ERROR, "MR003",
                             "{0} property is not allowed.  Set {1} to true in oozie-site to enable.",
                             MapReduceMain.OOZIE_MAPREDUCE_UBER_JAR, OOZIE_MAPREDUCE_UBER_JAR_ENABLE);
diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/OozieJobInfo.java b/core/src/main/java/org/apache/oozie/action/hadoop/OozieJobInfo.java
index e8733a171..4b13daad2 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/OozieJobInfo.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/OozieJobInfo.java
@@ -28,6 +28,7 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.action.ActionExecutor.Context;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.XConfiguration;
 
@@ -52,12 +53,11 @@ public class OozieJobInfo {
     XConfiguration contextConf;
     private WorkflowAction action;
     private Configuration actionConf;
    private static boolean jobInfo = Services.get().getConf().getBoolean(OozieJobInfo.CONF_JOB_INFO, false);
    private static boolean jobInfo = ConfigurationService.getBoolean(OozieJobInfo.CONF_JOB_INFO);
 
     /**
      * Instantiates a new oozie job info.
      *
     * @param jobconf the jobconf
      * @param actionConf the action conf
      * @param context the context
      * @param action the action
diff --git a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
index b8c7e57e1..bda34b5ae 100644
-- a/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/oozie/SubWorkflowActionExecutor.java
@@ -24,6 +24,7 @@ import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.DagEngine;
 import org.apache.oozie.LocalOozieClient;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.DagEngineService;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.OozieClient;
@@ -124,7 +125,7 @@ public class SubWorkflowActionExecutor extends ActionExecutor {
 
     protected void verifyAndInjectSubworkflowDepth(Configuration parentConf, Configuration conf) throws ActionExecutorException {
         int depth = conf.getInt(SUBWORKFLOW_DEPTH, 0);
        int maxDepth = Services.get().getConf().getInt(SUBWORKFLOW_MAX_DEPTH, 50);
        int maxDepth = ConfigurationService.getInt(SUBWORKFLOW_MAX_DEPTH);
         if (depth >= maxDepth) {
             throw new ActionExecutorException(ActionExecutorException.ErrorType.ERROR, "SUBWF001",
                     "Depth [{0}] cannot exceed maximum subworkflow depth [{1}]", (depth + 1), maxDepth);
diff --git a/core/src/main/java/org/apache/oozie/action/ssh/SshActionExecutor.java b/core/src/main/java/org/apache/oozie/action/ssh/SshActionExecutor.java
index 734fce916..99288a6ae 100644
-- a/core/src/main/java/org/apache/oozie/action/ssh/SshActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/ssh/SshActionExecutor.java
@@ -34,6 +34,7 @@ import org.apache.oozie.client.WorkflowAction.Status;
 import org.apache.oozie.action.ActionExecutor;
 import org.apache.oozie.action.ActionExecutorException;
 import org.apache.oozie.service.CallbackService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.servlet.CallbackServlet;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.IOUtils;
@@ -96,7 +97,7 @@ public class SshActionExecutor extends ActionExecutor {
     public void initActionType() {
         super.initActionType();
         maxLen = getOozieConf().getInt(CallbackServlet.CONF_MAX_DATA_LEN, 2 * 1024);
        allowSshUserAtHost = getOozieConf().getBoolean(CONF_SSH_ALLOW_USER_AT_HOST, true);
        allowSshUserAtHost = ConfigurationService.getBoolean(CONF_SSH_ALLOW_USER_AT_HOST);
         registerError(InterruptedException.class.getName(), ActionExecutorException.ErrorType.ERROR, "SH001");
         registerError(JDOMException.class.getName(), ActionExecutorException.ErrorType.ERROR, "SH002");
         initSshScripts();
@@ -401,13 +402,13 @@ public class SshActionExecutor extends ActionExecutor {
                                throws IOException, InterruptedException {
         XLog log = XLog.getLog(getClass());
         Runtime runtime = Runtime.getRuntime();
        String callbackPost = ignoreOutput ? "_" : getOozieConf().get(HTTP_COMMAND_OPTIONS).replace(" ", "%%%");
        String callbackPost = ignoreOutput ? "_" : ConfigurationService.get(HTTP_COMMAND_OPTIONS).replace(" ", "%%%");
         String preserveArgsS = preserveArgs ? "PRESERVE_ARGS" : "FLATTEN_ARGS";
         // TODO check
         String callBackUrl = Services.get().get(CallbackService.class)
                 .createCallBackUrl(action.getId(), EXT_STATUS_VAR);
         String command = XLog.format("{0}{1} {2}ssh-base.sh {3} {4} \"{5}\" \"{6}\" {7} {8} ", SSH_COMMAND_BASE, host, dirLocation,
                                      preserveArgsS, getOozieConf().get(HTTP_COMMAND), callBackUrl, callbackPost, recoveryId, cmnd)
                preserveArgsS, ConfigurationService.get(HTTP_COMMAND), callBackUrl, callbackPost, recoveryId, cmnd)
                 .toString();
         String[] commandArray = command.split("\\s");
         String[] finalCommand;
@@ -452,7 +453,7 @@ public class SshActionExecutor extends ActionExecutor {
         else {
             context.setEndData(WorkflowAction.Status.ERROR, WorkflowAction.Status.ERROR.toString());
         }
        boolean deleteTmpDir = getOozieConf().getBoolean(DELETE_TMP_DIR, true);
        boolean deleteTmpDir = ConfigurationService.getBoolean(DELETE_TMP_DIR);
         if (deleteTmpDir) {
             String tmpDir = getRemoteFileName(context, action, null, true, false);
             String removeTmpDirCmd = SSH_COMMAND_BASE + action.getTrackerUri() + " rm -rf " + tmpDir;
diff --git a/core/src/main/java/org/apache/oozie/command/XCommand.java b/core/src/main/java/org/apache/oozie/command/XCommand.java
index 7f850cbb1..655670dc6 100644
-- a/core/src/main/java/org/apache/oozie/command/XCommand.java
++ b/core/src/main/java/org/apache/oozie/command/XCommand.java
@@ -22,6 +22,7 @@ import org.apache.oozie.ErrorCode;
 import org.apache.oozie.FaultInjection;
 import org.apache.oozie.XException;
 import org.apache.oozie.service.CallableQueueService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.InstrumentationService;
 import org.apache.oozie.service.MemoryLocksService;
@@ -512,7 +513,7 @@ public abstract class XCommand<T> implements XCallable<T> {
      * @return delay time when requeue itself
      */
     protected long getRequeueDelay() {
        return Services.get().getConf().getLong(DEFAULT_REQUEUE_DELAY, 10 * 1000L);
        return ConfigurationService.getLong(DEFAULT_REQUEUE_DELAY);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index b26c1e2dd..a975f6edd 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -49,6 +49,7 @@ import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.CallableQueueService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Service;
@@ -69,6 +70,8 @@ import org.jdom.Element;
  */
 public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
 
    public static final String COORD_EXECUTION_NONE_TOLERANCE = "oozie.coord.execution.none.tolerance";

     private final String actionId;
     /**
      * Property name of command re-queue interval for coordinator action input check in
@@ -76,11 +79,6 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
      */
     public static final String CONF_COORD_INPUT_CHECK_REQUEUE_INTERVAL = Service.CONF_PREFIX
             + "coord.input.check.requeue.interval";
    /**
     * Default re-queue interval in ms. It is applied when no value defined in
     * the oozie configuration.
     */
    private final int DEFAULT_COMMAND_REQUEUE_INTERVAL = 60000; // 1 minute
     private CoordinatorActionBean coordAction = null;
     private CoordinatorJobBean coordJob = null;
     private JPAService jpaService = null;
@@ -179,7 +177,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                 // should be started; so set it to SKIPPED
                 Calendar cal = Calendar.getInstance(DateUtils.getTimeZone(coordJob.getTimeZone()));
                 cal.setTime(nominalTime);
                cal.add(Calendar.MINUTE, Services.get().getConf().getInt("oozie.coord.execution.none.tolerance", 1));
                cal.add(Calendar.MINUTE, ConfigurationService.getInt(COORD_EXECUTION_NONE_TOLERANCE));
                 nominalTime = cal.getTime();
                 if (now.after(nominalTime)) {
                     LOG.info("NONE execution: Preparing to skip action [{0}] because the current time [{1}] is later than "
@@ -333,8 +331,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
      * @return re-queue interval in ms
      */
     public long getCoordInputCheckRequeueInterval() {
        long requeueInterval = Services.get().getConf().getLong(CONF_COORD_INPUT_CHECK_REQUEUE_INTERVAL,
                DEFAULT_COMMAND_REQUEUE_INTERVAL);
        long requeueInterval = ConfigurationService.getLong(CONF_COORD_INPUT_CHECK_REQUEUE_INTERVAL);
         return requeueInterval;
     }
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionNotificationXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionNotificationXCommand.java
index d220434cd..255615286 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionNotificationXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionNotificationXCommand.java
@@ -30,6 +30,7 @@ import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.command.wf.NotificationXCommand;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.ParamChecker;
@@ -79,9 +80,8 @@ public class CoordActionNotificationXCommand extends CoordinatorXCommand<Void> {
             url = url.replaceAll(STATUS_PATTERN, actionBean.getStatus().toString());
             LOG.debug("Notification URL :" + url);
             try {
                int timeout = Services.get().getConf().getInt(
                    NotificationXCommand.NOTIFICATION_URL_CONNECTION_TIMEOUT_KEY,
                    NotificationXCommand.NOTIFICATION_URL_CONNECTION_TIMEOUT_DEFAULT);
                int timeout = ConfigurationService.getInt(NotificationXCommand
                        .NOTIFICATION_URL_CONNECTION_TIMEOUT_KEY);
                 URL urlObj = new URL(url);
                 HttpURLConnection urlConn = (HttpURLConnection) urlObj.openConnection();
                 urlConn.setConnectTimeout(timeout);
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
index 05b7a62d3..e2d63bb7b 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
@@ -39,6 +39,7 @@ import org.apache.oozie.executor.jpa.CoordActionsActiveCountJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.CoordMaterializeTriggerService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
@@ -78,11 +79,8 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
     private int lastActionNumber = 1; // over-ride by DB value
     private CoordinatorJob.Status prevStatus = null;
 
    static final private int lookAheadWindow = Services
            .get()
            .getConf()
            .getInt(CoordMaterializeTriggerService.CONF_LOOKUP_INTERVAL,
                    CoordMaterializeTriggerService.CONF_LOOKUP_INTERVAL_DEFAULT);
    static final private int lookAheadWindow = ConfigurationService.getInt(CoordMaterializeTriggerService
            .CONF_LOOKUP_INTERVAL);
 
     /**
      * Default MAX timeout in minutes, after which coordinator input check will timeout
@@ -94,7 +92,6 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
      *
      * @param jobId coordinator job id
      * @param materializationWindow materialization window to calculate end time
     * @param lookahead window
      */
     public CoordMaterializeTransitionXCommand(String jobId, int materializationWindow) {
         super("coord_mater", "coord_mater", 1);
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
index 68597b06c..cc346274e 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
@@ -43,6 +43,7 @@ import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.CallableQueueService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.PartitionDependencyManagerService;
@@ -68,11 +69,6 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
      */
     public static final String CONF_COORD_PUSH_CHECK_REQUEUE_INTERVAL = Service.CONF_PREFIX
             + "coord.push.check.requeue.interval";
    /**
     * Default re-queue interval in ms. It is applied when no value defined in
     * the oozie configuration.
     */
    private final int DEFAULT_COMMAND_REQUEUE_INTERVAL = 600000;
     private boolean registerForNotification;
     private boolean removeAvailDependencies;
 
@@ -195,8 +191,7 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
      * @return
      */
     public long getCoordPushCheckRequeueInterval() {
        long requeueInterval = Services.get().getConf().getLong(CONF_COORD_PUSH_CHECK_REQUEUE_INTERVAL,
                DEFAULT_COMMAND_REQUEUE_INTERVAL);
        long requeueInterval = ConfigurationService.getLong(CONF_COORD_PUSH_CHECK_REQUEUE_INTERVAL);
         return requeueInterval;
     }
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
index aec7199d8..0843f2861 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
@@ -58,7 +58,7 @@ import org.apache.oozie.coord.TimeUnit;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.CoordMaterializeTriggerService;
import org.apache.oozie.service.DagXLogInfoService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.HadoopAccessorException;
 import org.apache.oozie.service.HadoopAccessorService;
 import org.apache.oozie.service.JPAService;
@@ -80,7 +80,6 @@ import org.apache.oozie.util.ParameterVerifier;
 import org.apache.oozie.util.ParameterVerifierException;
 import org.apache.oozie.util.PropertiesUtils;
 import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XLog;
 import org.apache.oozie.util.XmlUtils;
 import org.jdom.Attribute;
 import org.jdom.Element;
@@ -293,8 +292,8 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
      * @throws Exception the exception
      */
     protected String getDryRun(CoordinatorJobBean coordJob) throws Exception{
        int materializationWindow = conf.getInt(CoordMaterializeTriggerService.CONF_MATERIALIZATION_WINDOW,
                CoordMaterializeTriggerService.CONF_MATERIALIZATION_WINDOW_DEFAULT);
        int materializationWindow = ConfigurationService.getInt(conf, CoordMaterializeTriggerService
                .CONF_MATERIALIZATION_WINDOW);
         Date startTime = coordJob.getStartTime();
         long startTimeMilli = startTime.getTime();
         long endTimeMilli = startTimeMilli + (materializationWindow * 1000);
@@ -325,8 +324,8 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
      * Queue MaterializeTransitionXCommand
      */
     protected void queueMaterializeTransitionXCommand(String jobId) {
        int materializationWindow = conf.getInt(CoordMaterializeTriggerService.CONF_MATERIALIZATION_WINDOW,
                CoordMaterializeTriggerService.CONF_MATERIALIZATION_WINDOW_DEFAULT);
        int materializationWindow = ConfigurationService.getInt(conf, CoordMaterializeTriggerService
                .CONF_MATERIALIZATION_WINDOW);
         queue(new CoordMaterializeTransitionXCommand(jobId, materializationWindow), 100);
     }
 
@@ -344,7 +343,7 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
             int freq = Integer.parseInt(coordJob.getFrequency());
 
             // Check if the frequency is faster than 5 min if enabled
            if (Services.get().getConf().getBoolean(CONF_CHECK_MAX_FREQUENCY, true)) {
            if (ConfigurationService.getBoolean(CONF_CHECK_MAX_FREQUENCY)) {
                 CoordinatorJob.Timeunit unit = coordJob.getTimeUnit();
                 if (freq == 0 || (freq < 5 && unit == CoordinatorJob.Timeunit.MINUTE)) {
                     throw new IllegalArgumentException("Coordinator job with frequency [" + freq +
@@ -744,32 +743,32 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
             }
         }
         else {
            val = Services.get().getConf().get(CONF_DEFAULT_TIMEOUT_NORMAL);
            val = ConfigurationService.get(CONF_DEFAULT_TIMEOUT_NORMAL);
         }
 
         ival = ParamChecker.checkInteger(val, "timeout");
        if (ival < 0 || ival > Services.get().getConf().getInt(CONF_DEFAULT_MAX_TIMEOUT, 129600)) {
            ival = Services.get().getConf().getInt(CONF_DEFAULT_MAX_TIMEOUT, 129600);
        if (ival < 0 || ival > ConfigurationService.getInt(CONF_DEFAULT_MAX_TIMEOUT)) {
            ival = ConfigurationService.getInt(CONF_DEFAULT_MAX_TIMEOUT);
         }
         coordJob.setTimeout(ival);
 
         val = resolveTagContents("concurrency", eAppXml.getChild("controls", eAppXml.getNamespace()), evalNofuncs);
         if (val == null || val.isEmpty()) {
            val = Services.get().getConf().get(CONF_DEFAULT_CONCURRENCY, "1");
            val = ConfigurationService.get(CONF_DEFAULT_CONCURRENCY);
         }
         ival = ParamChecker.checkInteger(val, "concurrency");
         coordJob.setConcurrency(ival);
 
         val = resolveTagContents("throttle", eAppXml.getChild("controls", eAppXml.getNamespace()), evalNofuncs);
         if (val == null || val.isEmpty()) {
            int defaultThrottle = Services.get().getConf().getInt(CONF_DEFAULT_THROTTLE, 12);
            int defaultThrottle = ConfigurationService.getInt(CONF_DEFAULT_THROTTLE);
             ival = defaultThrottle;
         }
         else {
             ival = ParamChecker.checkInteger(val, "throttle");
         }
        int maxQueue = Services.get().getConf().getInt(CONF_QUEUE_SIZE, 10000);
        float factor = Services.get().getConf().getFloat(CONF_MAT_THROTTLING_FACTOR, 0.10f);
        int maxQueue = ConfigurationService.getInt(CONF_QUEUE_SIZE);
        float factor = ConfigurationService.getFloat(CONF_MAT_THROTTLING_FACTOR);
         int maxThrottle = (int) (maxQueue * factor);
         if (ival > maxThrottle || ival < 1) {
             ival = maxThrottle;
diff --git a/core/src/main/java/org/apache/oozie/command/wf/JobXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/JobXCommand.java
index 747d93566..3b6cb358e 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/JobXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/JobXCommand.java
@@ -24,6 +24,7 @@ import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowInfoWithActionsSubsetGetJPAExecutor;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.ParamChecker;
@@ -37,6 +38,8 @@ public class JobXCommand extends WorkflowXCommand<WorkflowJobBean> {
     private int len = Integer.MAX_VALUE;
     private WorkflowJobBean workflow;
 
    public static final String CONF_CONSOLE_URL = "oozie.JobCommand.job.console.url";

     public JobXCommand(String id) {
         this(id, 1, Integer.MAX_VALUE);
     }
@@ -85,7 +88,7 @@ public class JobXCommand extends WorkflowXCommand<WorkflowJobBean> {
      * @return console URL
      */
     public static String getJobConsoleUrl(String jobId) {
        String consoleUrl = Services.get().getConf().get("oozie.JobCommand.job.console.url", null);
        String consoleUrl = ConfigurationService.get(CONF_CONSOLE_URL);
         return (consoleUrl != null) ? consoleUrl + jobId : null;
     }
 
diff --git a/core/src/main/java/org/apache/oozie/command/wf/NotificationXCommand.java b/core/src/main/java/org/apache/oozie/command/wf/NotificationXCommand.java
index 73ce9a7f1..0fc3d651c 100644
-- a/core/src/main/java/org/apache/oozie/command/wf/NotificationXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/wf/NotificationXCommand.java
@@ -23,6 +23,7 @@ import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.ParamChecker;
@@ -35,7 +36,6 @@ import java.net.URL;
 public class NotificationXCommand extends WorkflowXCommand<Void> {
 
     public static final String NOTIFICATION_URL_CONNECTION_TIMEOUT_KEY = "oozie.notification.url.connection.timeout";
    public static final int NOTIFICATION_URL_CONNECTION_TIMEOUT_DEFAULT = 10 * 1000; // 10 seconds
 
     private static final String STATUS_PATTERN = "\\$status";
     private static final String JOB_ID_PATTERN = "\\$jobId";
@@ -102,8 +102,7 @@ public class NotificationXCommand extends WorkflowXCommand<Void> {
     @Override
     protected Void execute() throws CommandException {
         if (url != null) {
            int timeout = Services.get().getConf().getInt(NOTIFICATION_URL_CONNECTION_TIMEOUT_KEY,
                                                          NOTIFICATION_URL_CONNECTION_TIMEOUT_DEFAULT);
            int timeout = ConfigurationService.getInt(NOTIFICATION_URL_CONNECTION_TIMEOUT_KEY);
             try {
                 URL url = new URL(this.url);
                 HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
diff --git a/core/src/main/java/org/apache/oozie/event/MemoryEventQueue.java b/core/src/main/java/org/apache/oozie/event/MemoryEventQueue.java
index 205dbb6ae..b9e31ef53 100644
-- a/core/src/main/java/org/apache/oozie/event/MemoryEventQueue.java
++ b/core/src/main/java/org/apache/oozie/event/MemoryEventQueue.java
@@ -26,6 +26,7 @@ import java.util.concurrent.atomic.AtomicInteger;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.client.event.Event;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.util.XLog;
 
@@ -44,9 +45,9 @@ public class MemoryEventQueue implements EventQueue {
     @Override
     public void init(Configuration conf) {
         eventQueue = new ConcurrentLinkedQueue<EventQueueElement>();
        maxSize = conf.getInt(EventHandlerService.CONF_QUEUE_SIZE, 10000);
        maxSize = ConfigurationService.getInt(conf, EventHandlerService.CONF_QUEUE_SIZE);
         currentSize = new AtomicInteger();
        batchSize = conf.getInt(EventHandlerService.CONF_BATCH_SIZE, 10);
        batchSize = ConfigurationService.getInt(conf, EventHandlerService.CONF_BATCH_SIZE);
         LOG = XLog.getLog(getClass());
         LOG.info("Memory Event Queue initialized with Max size = [{0}], Batch drain size = [{1}]", maxSize, batchSize);
     }
diff --git a/core/src/main/java/org/apache/oozie/event/listener/ZKConnectionListener.java b/core/src/main/java/org/apache/oozie/event/listener/ZKConnectionListener.java
index a5d22c0b9..c6415b15f 100644
-- a/core/src/main/java/org/apache/oozie/event/listener/ZKConnectionListener.java
++ b/core/src/main/java/org/apache/oozie/event/listener/ZKConnectionListener.java
@@ -21,6 +21,7 @@ package org.apache.oozie.event.listener;
 import org.apache.curator.framework.CuratorFramework;
 import org.apache.curator.framework.state.ConnectionState;
 import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.XLog;
 import org.apache.oozie.util.ZKUtils;
@@ -57,7 +58,7 @@ public class ZKConnectionListener implements ConnectionStateListener {
 
         if (newState == ConnectionState.LOST) {
             LOG.fatal("ZK is not reconnected in " + ZKUtils.getZKConnectionTimeout());
            if (Services.get().getConf().getBoolean(CONF_SHUTDOWN_ON_TIMEOUT, true)) {
            if (ConfigurationService.getBoolean(CONF_SHUTDOWN_ON_TIMEOUT)) {
                 LOG.fatal("Shutting down Oozie server");
                 Services.get().destroy();
                 System.exit(1);
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInfoJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInfoJPAExecutor.java
index 319a49b27..a9cf0ff32 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInfoJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionGetForInfoJPAExecutor.java
@@ -28,6 +28,7 @@ import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.StringBlob;
 import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ParamChecker;
@@ -60,7 +61,7 @@ public class CoordActionGetForInfoJPAExecutor implements JPAExecutor<Coordinator
     @SuppressWarnings("unchecked")
     public CoordinatorActionBean execute(EntityManager em) throws JPAExecutorException {
         // Maintain backward compatibility for action info cmd
        if (!(Services.get().getConf().getBoolean(COORD_GET_ALL_COLS_FOR_ACTION, false))) {
        if (!ConfigurationService.getBoolean(COORD_GET_ALL_COLS_FOR_ACTION)) {
             List<Object[]> actionObjects;
             try {
                 Query q = em.createNamedQuery("GET_COORD_ACTION_FOR_INFO");
diff --git a/core/src/main/java/org/apache/oozie/jms/JMSJobEventListener.java b/core/src/main/java/org/apache/oozie/jms/JMSJobEventListener.java
index a458165ab..7691a0609 100644
-- a/core/src/main/java/org/apache/oozie/jms/JMSJobEventListener.java
++ b/core/src/main/java/org/apache/oozie/jms/JMSJobEventListener.java
@@ -39,6 +39,7 @@ import org.apache.oozie.event.WorkflowJobEvent;
 import org.apache.oozie.event.listener.JobEventListener;
 import org.apache.oozie.event.messaging.MessageFactory;
 import org.apache.oozie.event.messaging.MessageSerializer;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.JMSAccessorService;
 import org.apache.oozie.service.JMSTopicService;
 import org.apache.oozie.service.Services;
@@ -65,7 +66,7 @@ public class JMSJobEventListener extends JobEventListener {
     @Override
     public void init(Configuration conf) {
         LOG = XLog.getLog(getClass());
        String jmsProps = conf.get(JMS_CONNECTION_PROPERTIES);
        String jmsProps = ConfigurationService.get(conf, JMS_CONNECTION_PROPERTIES);
         LOG.info("JMS producer connection properties [{0}]", jmsProps);
         connInfo = new JMSConnectionInfo(jmsProps);
         jmsSessionOpts = conf.getInt(JMS_SESSION_OPTS, Session.AUTO_ACKNOWLEDGE);
diff --git a/core/src/main/java/org/apache/oozie/service/AbandonedCoordCheckerService.java b/core/src/main/java/org/apache/oozie/service/AbandonedCoordCheckerService.java
index b082567f5..ec8cf7123 100644
-- a/core/src/main/java/org/apache/oozie/service/AbandonedCoordCheckerService.java
++ b/core/src/main/java/org/apache/oozie/service/AbandonedCoordCheckerService.java
@@ -45,21 +45,15 @@ import com.google.common.annotations.VisibleForTesting;
 public class AbandonedCoordCheckerService implements Service {
 
     private static final String CONF_PREFIX = Service.CONF_PREFIX + "AbandonedCoordCheckerService.";
    private static final String TO_ADDRESS = CONF_PREFIX + "email.address";
    public static final String TO_ADDRESS = CONF_PREFIX + "email.address";
     private static final String CONTENT_TYPE = "text/html";
     private static final String SUBJECT = "Abandoned Coordinators report";
    private static final String CONF_CHECK_INTERVAL = CONF_PREFIX + "check.interval";
    private static final String CONF_CHECK_DELAY = CONF_PREFIX + "check.delay";
    private static final String CONF_FAILURE_LEN = CONF_PREFIX + "failure.limit";
    private static final String CONF_JOB_OLDER_THAN = CONF_PREFIX + "job.older.than";

    private static final int DEFAULT_FAILURE_LEN = 20;
    private static final int DEFAULT_CHECK_INTERVAL = 24 * 60; // Once a day
    private static final int DEFAULT_CHECK_DELAY = 1 * 60; // One hour.
    private static final int DEFAULT_CONF_JOB_OLDER_THAN = 2880; // One days

    private static final String CONF_JOB_KILL = CONF_PREFIX + "kill.jobs";
    private static final boolean DEFAULT_JOB_KILL = false;
    public static final String CONF_CHECK_INTERVAL = CONF_PREFIX + "check.interval";
    public static final String CONF_CHECK_DELAY = CONF_PREFIX + "check.delay";
    public static final String CONF_FAILURE_LEN = CONF_PREFIX + "failure.limit";
    public static final String CONF_JOB_OLDER_THAN = CONF_PREFIX + "job.older.than";

    public static final String CONF_JOB_KILL = CONF_PREFIX + "kill.jobs";
     public static final String OOZIE_BASE_URL = "oozie.base.url";
     private static String[] to;
     private static String serverURL;
@@ -68,7 +62,7 @@ public class AbandonedCoordCheckerService implements Service {
         private  StringBuilder msg;
         final int failureLimit;
         XLog LOG = XLog.getLog(getClass());
        private boolean shouldKill = DEFAULT_JOB_KILL;
        private boolean shouldKill = false;
 
         public AbandonedCoordCheckerRunnable(int failureLimit) {
             this(failureLimit, false);
@@ -110,8 +104,7 @@ public class AbandonedCoordCheckerService implements Service {
             try {
                 Timestamp createdTS = new Timestamp(
                         System.currentTimeMillis()
                                - (Services.get().getConf()
                                        .getInt(CONF_JOB_OLDER_THAN, DEFAULT_CONF_JOB_OLDER_THAN) * 60 * 1000));
                                - (ConfigurationService.getInt(CONF_JOB_OLDER_THAN) * 60 * 1000));
 
                 jobs = CoordJobQueryExecutor.getInstance().getList(CoordJobQuery.GET_COORD_FOR_ABANDONEDCHECK,
                         failureLimit, createdTS);
@@ -177,17 +170,16 @@ public class AbandonedCoordCheckerService implements Service {
 
     @Override
     public void init(Services services) {
        Configuration conf = services.getConf();
        to = conf.getStrings(TO_ADDRESS);
        int failureLen = conf.getInt(CONF_FAILURE_LEN, DEFAULT_FAILURE_LEN);
        boolean shouldKill = conf.getBoolean(CONF_JOB_KILL, DEFAULT_JOB_KILL);
        serverURL = conf.get(OOZIE_BASE_URL);
        to = ConfigurationService.getStrings(TO_ADDRESS);
        int failureLen = ConfigurationService.getInt(CONF_FAILURE_LEN);
        boolean shouldKill = ConfigurationService.getBoolean(CONF_JOB_KILL);
        serverURL = ConfigurationService.get(OOZIE_BASE_URL);
 
        int delay = conf.getInt(CONF_CHECK_DELAY, DEFAULT_CHECK_DELAY);
        int delay = ConfigurationService.getInt(CONF_CHECK_DELAY);
 
         Runnable actionCheckRunnable = new AbandonedCoordCheckerRunnable(failureLen, shouldKill);
         services.get(SchedulerService.class).schedule(actionCheckRunnable, delay,
                conf.getInt(CONF_CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL), SchedulerService.Unit.MIN);
                ConfigurationService.getInt(CONF_CHECK_INTERVAL), SchedulerService.Unit.MIN);
 
     }
 
@@ -199,4 +191,4 @@ public class AbandonedCoordCheckerService implements Service {
     public Class<? extends Service> getInterface() {
         return AbandonedCoordCheckerService.class;
     }
}
\ No newline at end of file
}
diff --git a/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java b/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
index b085014d3..1afd01bf7 100644
-- a/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
++ b/core/src/main/java/org/apache/oozie/service/ActionCheckerService.java
@@ -21,7 +21,6 @@ package org.apache.oozie.service;
 import java.util.ArrayList;
 import java.util.List;
 
import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.command.CommandException;
@@ -194,7 +193,7 @@ public class ActionCheckerService implements Service {
                 callables = new ArrayList<XCallable<Void>>();
             }
             callables.add(callable);
            if (callables.size() == Services.get().getConf().getInt(CONF_CALLABLE_BATCH_SIZE, 10)) {
            if (callables.size() == ConfigurationService.getInt(CONF_CALLABLE_BATCH_SIZE)) {
                 boolean ret = Services.get().get(CallableQueueService.class).queueSerial(callables);
                 if (ret == false) {
                     XLog.getLog(getClass()).warn(
@@ -222,10 +221,11 @@ public class ActionCheckerService implements Service {
      */
     @Override
     public void init(Services services) {
        Configuration conf = services.getConf();
        Runnable actionCheckRunnable = new ActionCheckRunnable(conf.getInt(CONF_ACTION_CHECK_DELAY, 600));
        Runnable actionCheckRunnable = new ActionCheckRunnable(ConfigurationService.getInt
                (services.getConf(), CONF_ACTION_CHECK_DELAY));
         services.get(SchedulerService.class).schedule(actionCheckRunnable, 10,
                conf.getInt(CONF_ACTION_CHECK_INTERVAL, 60), SchedulerService.Unit.SEC);
                ConfigurationService.getInt(services.getConf(), CONF_ACTION_CHECK_INTERVAL),
                SchedulerService.Unit.SEC);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/ActionService.java b/core/src/main/java/org/apache/oozie/service/ActionService.java
index ebfe299bb..c8ed26523 100644
-- a/core/src/main/java/org/apache/oozie/service/ActionService.java
++ b/core/src/main/java/org/apache/oozie/service/ActionService.java
@@ -57,10 +57,12 @@ public class ActionService implements Service {
             EndActionExecutor.class, KillActionExecutor.class,  ForkActionExecutor.class, JoinActionExecutor.class };
         registerExecutors(classes);
 
        classes = (Class<? extends ActionExecutor>[]) services.getConf().getClasses(CONF_ACTION_EXECUTOR_CLASSES);
        classes = (Class<? extends ActionExecutor>[]) ConfigurationService.getClasses
                (services.getConf(), CONF_ACTION_EXECUTOR_CLASSES);
         registerExecutors(classes);
 
        classes = (Class<? extends ActionExecutor>[]) services.getConf().getClasses(CONF_ACTION_EXECUTOR_EXT_CLASSES);
        classes = (Class<? extends ActionExecutor>[]) ConfigurationService.getClasses
                (services.getConf(), CONF_ACTION_EXECUTOR_EXT_CLASSES);
         registerExecutors(classes);
     }
 
diff --git a/core/src/main/java/org/apache/oozie/service/AuthorizationService.java b/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
index 293de3f65..9ce06403a 100644
-- a/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
++ b/core/src/main/java/org/apache/oozie/service/AuthorizationService.java
@@ -108,7 +108,7 @@ public class AuthorizationService implements Service {
                                                CONF_SECURITY_ENABLED, false);
         if (authorizationEnabled) {
             log.info("Oozie running with authorization enabled");
            useDefaultGroupAsAcl = Services.get().getConf().getBoolean(CONF_DEFAULT_GROUP_AS_ACL, false);
            useDefaultGroupAsAcl = ConfigurationService.getBoolean(CONF_DEFAULT_GROUP_AS_ACL);
             String[] str = getTrimmedStrings(Services.get().getConf().get(CONF_ADMIN_GROUPS));
             if (str.length > 0) {
                 log.info("Admin users will be checked against the defined admin groups");
diff --git a/core/src/main/java/org/apache/oozie/service/CallableQueueService.java b/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
index 25fed7d8a..fd7b55f35 100644
-- a/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
++ b/core/src/main/java/org/apache/oozie/service/CallableQueueService.java
@@ -437,11 +437,11 @@ public class CallableQueueService implements Service, Instrumentable {
     public void init(Services services) {
         Configuration conf = services.getConf();
 
        queueSize = conf.getInt(CONF_QUEUE_SIZE, 10000);
        int threads = conf.getInt(CONF_THREADS, 10);
        boolean callableNextEligible = conf.getBoolean(CONF_CALLABLE_NEXT_ELIGIBLE, true);
        queueSize = ConfigurationService.getInt(conf, CONF_QUEUE_SIZE);
        int threads = ConfigurationService.getInt(conf, CONF_THREADS);
        boolean callableNextEligible = ConfigurationService.getBoolean(conf, CONF_CALLABLE_NEXT_ELIGIBLE);
 
        for (String type : conf.getStringCollection(CONF_CALLABLE_INTERRUPT_TYPES)) {
        for (String type : ConfigurationService.getStrings(conf, CONF_CALLABLE_INTERRUPT_TYPES)) {
             log.debug("Adding interrupt type [{0}]", type);
             INTERRUPT_TYPES.add(type);
         }
@@ -480,7 +480,7 @@ public class CallableQueueService implements Service, Instrumentable {
             };
         }
 
        interruptMapMaxSize = conf.getInt(CONF_CALLABLE_INTERRUPT_MAP_MAX_SIZE, 100);
        interruptMapMaxSize = ConfigurationService.getInt(conf, CONF_CALLABLE_INTERRUPT_MAP_MAX_SIZE);
 
         // IMPORTANT: The ThreadPoolExecutor does not always the execute
         // commands out of the queue, there are
@@ -513,7 +513,7 @@ public class CallableQueueService implements Service, Instrumentable {
             });
         }
 
        maxCallableConcurrency = conf.getInt(CONF_CALLABLE_CONCURRENCY, 3);
        maxCallableConcurrency = ConfigurationService.getInt(conf, CONF_CALLABLE_CONCURRENCY);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/CallbackService.java b/core/src/main/java/org/apache/oozie/service/CallbackService.java
index e7463cfd1..7fa07f18f 100644
-- a/core/src/main/java/org/apache/oozie/service/CallbackService.java
++ b/core/src/main/java/org/apache/oozie/service/CallbackService.java
@@ -77,7 +77,7 @@ public class CallbackService implements Service {
         ParamChecker.notEmpty(actionId, "actionId");
         ParamChecker.notEmpty(externalStatusVar, "externalStatusVar");
         //TODO: figure out why double encoding is happening in case of hadoop callbacks.
        String baseCallbackUrl = oozieConf.get(CONF_BASE_URL, "http://localhost:8080/oozie/v0/callback");
        String baseCallbackUrl = ConfigurationService.get(oozieConf, CONF_BASE_URL);
         return MessageFormat.format(CALL_BACK_QUERY_STRING, baseCallbackUrl, actionId, externalStatusVar);
     }
 
diff --git a/core/src/main/java/org/apache/oozie/service/ConfigurationService.java b/core/src/main/java/org/apache/oozie/service/ConfigurationService.java
index 5e4708116..d7107443b 100644
-- a/core/src/main/java/org/apache/oozie/service/ConfigurationService.java
++ b/core/src/main/java/org/apache/oozie/service/ConfigurationService.java
@@ -19,6 +19,7 @@
 package org.apache.oozie.service;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.Instrumentable;
 import org.apache.oozie.util.Instrumentation;
 import org.apache.oozie.util.XLog;
@@ -30,6 +31,7 @@ import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.StringWriter;
import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
@@ -65,6 +67,8 @@ public class ConfigurationService implements Service, Instrumentable {
 
     public static final String CONF_IGNORE_SYS_PROPS = CONF_PREFIX + "ignore.system.properties";
 
    public static final String CONF_VERIFY_AVAILABLE_PROPS = CONF_PREFIX + "verify.available.properties";

     /**
      * System property that indicates the configuration directory.
      */
@@ -86,6 +90,7 @@ public class ConfigurationService implements Service, Instrumentable {
 
     private static final String IGNORE_TEST_SYS_PROPS = "oozie.test.";
     private static final Set<String> MASK_PROPS = new HashSet<String>();
    private static Map<String,String> defaultConfigs = new HashMap<String,String>();
 
     static {
 
@@ -139,6 +144,9 @@ public class ConfigurationService implements Service, Instrumentable {
         log.info("Oozie conf file [{0}]", configFile);
         configFile = new File(configDir, configFile).toString();
         configuration = loadConf();
        if (configuration.getBoolean(CONF_VERIFY_AVAILABLE_PROPS, false)) {
            verifyConfigurationName();
        }
     }
 
     public static String getConfigurationDirectory() throws ServiceException {
@@ -203,14 +211,14 @@ public class ConfigurationService implements Service, Instrumentable {
         XConfiguration configuration;
         try {
             InputStream inputStream = getDefaultConfiguration();
            configuration = new XConfiguration(inputStream);
            configuration = loadConfig(inputStream, true);
             File file = new File(configFile);
             if (!file.exists()) {
                 log.info("Missing site configuration file [{0}]", configFile);
             }
             else {
                 inputStream = new FileInputStream(configFile);
                XConfiguration siteConfiguration = new XConfiguration(inputStream);
                XConfiguration siteConfiguration = loadConfig(inputStream, false);
                 XConfiguration.injectDefaults(configuration, siteConfiguration);
                 configuration = siteConfiguration;
             }
@@ -269,6 +277,20 @@ public class ConfigurationService implements Service, Instrumentable {
         return new LogChangesConfiguration(configuration);
     }
 
    private XConfiguration loadConfig(InputStream inputStream, boolean defaultConfig) throws IOException, ServiceException {
        XConfiguration configuration;
        configuration = new XConfiguration(inputStream);
        for(Map.Entry<String,String> entry: configuration) {
            if (defaultConfig) {
                defaultConfigs.put(entry.getKey(), entry.getValue());
            }
            else {
                log.debug("Overriding configuration with oozie-site, [{0}]", entry.getKey());
            }
        }
        return configuration;
    }

     private class LogChangesConfiguration extends XConfiguration {
 
         public LogChangesConfiguration(Configuration conf) {
@@ -284,13 +306,22 @@ public class ConfigurationService implements Service, Instrumentable {
             return (s != null && s.trim().length() > 0) ? super.getStrings(name) : new String[0];
         }
 
        public String[] getStrings(String name, String[] defaultValue) {
            String s = get(name);
            if (s == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name,
                        Arrays.asList(defaultValue).toString());
            }
            return (s != null && s.trim().length() > 0) ? super.getStrings(name) : defaultValue;
        }

         public String get(String name, String defaultValue) {
             String value = get(name);
             if (value == null) {
                 boolean maskValue = MASK_PROPS.contains(name);
                 value = defaultValue;
                 String logValue = (maskValue) ? "**MASKED**" : defaultValue;
                log.warn(XLog.OPS, "Configuration property [{0}] not found, using default [{1}]", name, logValue);
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, logValue);
             }
             return value;
         }
@@ -302,6 +333,59 @@ public class ConfigurationService implements Service, Instrumentable {
             log.info(XLog.OPS, "Programmatic configuration change, property[{0}]=[{1}]", name, value);
         }
 
        public boolean getBoolean(String name, boolean defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
            }
            return super.getBoolean(name, defaultValue);
        }

        public int getInt(String name, int defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
            }
            return super.getInt(name, defaultValue);
        }

        public long getLong(String name, long defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
            }
            return super.getLong(name, defaultValue);
        }

        public float getFloat(String name, float defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
            }
            return super.getFloat(name, defaultValue);
        }

        public Class<?>[] getClasses(String name, Class<?> ... defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
            }
            return super.getClasses(name, defaultValue);
        }

        public Class<?> getClass(String name, Class<?> defaultValue) {
            String value = get(name);
            if (value == null) {
                log.debug(XLog.OPS, "Configuration property [{0}] not found, use given value [{1}]", name, defaultValue);
                return defaultValue;
            }
            try {
                return getClassByName(value);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

         private void setValue(String name, String value) {
             super.set(name, value);
         }
@@ -353,4 +437,89 @@ public class ConfigurationService implements Service, Instrumentable {
         }
         return value;
     }


    /**
     * Gets the oozie configuration value in oozie-default.
     * @param name
     * @return the configuration value of the <code>name</code> otherwise null
     */
    private String getDefaultOozieConfig(String name) {
        return defaultConfigs.get(name);
    }

    /**
     * Verify the configuration is in oozie-default
     */
    public void verifyConfigurationName() {
        for (Map.Entry<String, String> entry: configuration) {
            if (getDefaultOozieConfig(entry.getKey()) == null) {
                log.warn("Invalid configuration defined, [{0}] ", entry.getKey());
            }
        }
    }

    public static String get(String name) {
        Configuration conf = Services.get().getConf();
        return get(conf, name);
    }

    public static String get(Configuration conf, String name) {
        return conf.get(name, ConfigUtils.STRING_DEFAULT);
    }

    public static String[] getStrings(String name) {
        Configuration conf = Services.get().getConf();
        return getStrings(conf, name);
    }

    public static String[] getStrings(Configuration conf, String name) {
        return conf.getStrings(name, new String[0]);
    }

    public static boolean getBoolean(String name) {
        Configuration conf = Services.get().getConf();
        return getBoolean(conf, name);
    }

    public static boolean getBoolean(Configuration conf, String name) {
        return conf.getBoolean(name, ConfigUtils.BOOLEAN_DEFAULT);
    }

    public static int getInt(String name) {
        Configuration conf = Services.get().getConf();
        return getInt(conf, name);
    }

    public static int getInt(Configuration conf, String name) {
        return conf.getInt(name, ConfigUtils.INT_DEFAULT);
    }

    public static float getFloat(String name) {
        Configuration conf = Services.get().getConf();
        return conf.getFloat(name, ConfigUtils.FLOAT_DEFAULT);
    }

    public static long getLong(String name) {
        Configuration conf = Services.get().getConf();
        return getLong(conf, name);
    }

    public static long getLong(Configuration conf, String name) {
        return conf.getLong(name, ConfigUtils.LONG_DEFAULT);
    }

    public static Class<?>[] getClasses(String name) {
        Configuration conf = Services.get().getConf();
        return getClasses(conf, name);
    }

    public static Class<?>[] getClasses(Configuration conf, String name) {
        return conf.getClasses(name);
    }

    public static Class<?> getClass(Configuration conf, String name) {
        return conf.getClass(name, Object.class);
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java b/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
index ee1085a86..fa16d1d2a 100644
-- a/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
++ b/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
@@ -22,14 +22,12 @@ import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 
import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.command.coord.CoordMaterializeTransitionXCommand;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
import org.apache.oozie.executor.jpa.BundleJobQueryExecutor.BundleJobQuery;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
 import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.util.XCallable;
@@ -64,9 +62,6 @@ public class CoordMaterializeTriggerService implements Service {
 
     private static final String INSTRUMENTATION_GROUP = "coord_job_mat";
     private static final String INSTR_MAT_JOBS_COUNTER = "jobs";
    public static final int CONF_LOOKUP_INTERVAL_DEFAULT = 300;
    public static final int CONF_MATERIALIZATION_WINDOW_DEFAULT = 3600;
    private static final int CONF_MATERIALIZATION_SYSTEM_LIMIT_DEFAULT = 50;
 
     /**
      * This runnable class will run in every "interval" to queue CoordMaterializeTransitionXCommand.
@@ -149,8 +144,7 @@ public class CoordMaterializeTriggerService implements Service {
                 // get current date
                 Date currDate = new Date(new Date().getTime() + lookupInterval * 1000);
                 // get list of all jobs that have actions that should be materialized.
                int materializationLimit = Services.get().getConf()
                        .getInt(CONF_MATERIALIZATION_SYSTEM_LIMIT, CONF_MATERIALIZATION_SYSTEM_LIMIT_DEFAULT);
                int materializationLimit = ConfigurationService.getInt(CONF_MATERIALIZATION_SYSTEM_LIMIT);
                 materializeCoordJobs(currDate, materializationLimit, LOG, updateList);
             }
 
@@ -195,7 +189,7 @@ public class CoordMaterializeTriggerService implements Service {
                 callables = new ArrayList<XCallable<Void>>();
             }
             callables.add(callable);
            if (callables.size() == Services.get().getConf().getInt(CONF_CALLABLE_BATCH_SIZE, 10)) {
            if (callables.size() == ConfigurationService.getInt(CONF_CALLABLE_BATCH_SIZE)) {
                 boolean ret = Services.get().get(CallableQueueService.class).queueSerial(callables);
                 if (ret == false) {
                     XLog.getLog(getClass()).warn(
@@ -211,11 +205,10 @@ public class CoordMaterializeTriggerService implements Service {
 
     @Override
     public void init(Services services) throws ServiceException {
        Configuration conf = services.getConf();
         // default is 3600sec (1hr)
        int materializationWindow = conf.getInt(CONF_MATERIALIZATION_WINDOW, CONF_MATERIALIZATION_WINDOW_DEFAULT);
        int materializationWindow = ConfigurationService.getInt(services.getConf(), CONF_MATERIALIZATION_WINDOW);
         // default is 300sec (5min)
        int lookupInterval = Services.get().getConf().getInt(CONF_LOOKUP_INTERVAL, CONF_LOOKUP_INTERVAL_DEFAULT);
        int lookupInterval = ConfigurationService.getInt(services.getConf(), CONF_LOOKUP_INTERVAL);
         // default is 300sec (5min)
         int schedulingInterval = Services.get().getConf().getInt(CONF_SCHEDULING_INTERVAL, lookupInterval);
 
diff --git a/core/src/main/java/org/apache/oozie/service/DBLiteWorkflowStoreService.java b/core/src/main/java/org/apache/oozie/service/DBLiteWorkflowStoreService.java
index df8387309..4f2c350bf 100644
-- a/core/src/main/java/org/apache/oozie/service/DBLiteWorkflowStoreService.java
++ b/core/src/main/java/org/apache/oozie/service/DBLiteWorkflowStoreService.java
@@ -105,8 +105,8 @@ public class DBLiteWorkflowStoreService extends LiteWorkflowStoreService impleme
 
     public void init(Services services) throws ServiceException {
         Configuration conf = services.getConf();
        statusWindow = conf.getInt(CONF_METRICS_INTERVAL_WINDOW, 3600);
        int statusMetricsCollectionInterval = conf.getInt(CONF_METRICS_INTERVAL_MINS, 5);
        statusWindow = ConfigurationService.getInt(conf, CONF_METRICS_INTERVAL_WINDOW);
        int statusMetricsCollectionInterval = ConfigurationService.getInt(conf, CONF_METRICS_INTERVAL_MINS);
         log = XLog.getLog(getClass());
         selectForUpdate = false;
 
diff --git a/core/src/main/java/org/apache/oozie/service/ELService.java b/core/src/main/java/org/apache/oozie/service/ELService.java
index 840695588..2506e9966 100644
-- a/core/src/main/java/org/apache/oozie/service/ELService.java
++ b/core/src/main/java/org/apache/oozie/service/ELService.java
@@ -87,7 +87,7 @@ public class ELService implements Service {
     private List<ELService.ELConstant> extractConstants(Configuration conf, String key) throws ServiceException {
         List<ELService.ELConstant> list = new ArrayList<ELService.ELConstant>();
         if (conf.get(key, "").trim().length() > 0) {
            for (String function : conf.getStrings(key)) {
            for (String function : ConfigurationService.getStrings(conf, key)) {
                 String[] parts = parseDefinition(function);
                 list.add(new ELConstant(parts[0], parts[1], findConstant(parts[2], parts[3])));
                 log.trace("Registered prefix:constant[{0}:{1}] for class#field[{2}#{3}]", (Object[]) parts);
@@ -99,7 +99,7 @@ public class ELService implements Service {
     private List<ELService.ELFunction> extractFunctions(Configuration conf, String key) throws ServiceException {
         List<ELService.ELFunction> list = new ArrayList<ELService.ELFunction>();
         if (conf.get(key, "").trim().length() > 0) {
            for (String function : conf.getStrings(key)) {
            for (String function : ConfigurationService.getStrings(conf, key)) {
                 String[] parts = parseDefinition(function);
                 list.add(new ELFunction(parts[0], parts[1], findMethod(parts[2], parts[3])));
                 log.trace("Registered prefix:constant[{0}:{1}] for class#field[{2}#{3}]", (Object[]) parts);
@@ -122,7 +122,7 @@ public class ELService implements Service {
         //Get the list of group names from configuration file
         // defined in the property tag: oozie.service.ELSerice.groups
         //String []groupList = services.getConf().get(CONF_GROUPS, "").trim().split(",");
        String[] groupList = services.getConf().getStrings(CONF_GROUPS, "");
        String[] groupList = ConfigurationService.getStrings(services.getConf(), CONF_GROUPS);
         //For each group, collect the required functions and constants
         // and store it into HashMap
         for (String group : groupList) {
diff --git a/core/src/main/java/org/apache/oozie/service/EventHandlerService.java b/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
index 244c04877..7c0d3bef4 100644
-- a/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
++ b/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
@@ -73,7 +73,8 @@ public class EventHandlerService implements Service {
         try {
             Configuration conf = services.getConf();
             LOG = XLog.getLog(getClass());
            Class<? extends EventQueue> queueImpl = (Class<? extends EventQueue>) conf.getClass(CONF_EVENT_QUEUE, null);
            Class<? extends EventQueue> queueImpl = (Class<? extends EventQueue>) ConfigurationService.getClass
                    (conf, CONF_EVENT_QUEUE);
             eventQueue = queueImpl == null ? new MemoryEventQueue() : (EventQueue) queueImpl.newInstance();
             eventQueue.init(conf);
             // initialize app-types to switch on events for
@@ -94,7 +95,7 @@ public class EventHandlerService implements Service {
 
     private void initApptypes(Configuration conf) {
         apptypes = new HashSet<String>();
        for (String jobtype : conf.getStringCollection(CONF_FILTER_APP_TYPES)) {
        for (String jobtype : ConfigurationService.getStrings(conf, CONF_FILTER_APP_TYPES)) {
             String tmp = jobtype.trim().toLowerCase();
             if (tmp.length() == 0) {
                 continue;
@@ -104,9 +105,7 @@ public class EventHandlerService implements Service {
     }
 
     private void initEventListeners(Configuration conf) throws Exception {
        Class<?>[] listenerClass = conf.getClasses(CONF_LISTENERS,
                org.apache.oozie.jms.JMSJobEventListener.class,
                org.apache.oozie.sla.listener.SLAJobEventListener.class);
        Class<?>[] listenerClass = ConfigurationService.getClasses(conf, CONF_LISTENERS);
         for (int i = 0; i < listenerClass.length; i++) {
             Object listener = null;
             try {
@@ -152,8 +151,8 @@ public class EventHandlerService implements Service {
     }
 
     private void initWorkerThreads(Configuration conf, Services services) throws ServiceException {
        numWorkers = conf.getInt(CONF_WORKER_THREADS, 3);
        int interval = conf.getInt(CONF_WORKER_INTERVAL, 30);
        numWorkers = ConfigurationService.getInt(conf, CONF_WORKER_THREADS);
        int interval = ConfigurationService.getInt(conf, CONF_WORKER_INTERVAL);
         SchedulerService ss = services.get(SchedulerService.class);
         int available = ss.getSchedulableThreads(conf);
         if (numWorkers + 3 > available) {
diff --git a/core/src/main/java/org/apache/oozie/service/HCatAccessorService.java b/core/src/main/java/org/apache/oozie/service/HCatAccessorService.java
index a64589862..249b66362 100644
-- a/core/src/main/java/org/apache/oozie/service/HCatAccessorService.java
++ b/core/src/main/java/org/apache/oozie/service/HCatAccessorService.java
@@ -140,7 +140,7 @@ public class HCatAccessorService implements Service {
     }
 
     private void initializeMappingRules() {
        String[] connections = conf.getStrings(JMS_CONNECTIONS_PROPERTIES);
        String[] connections = ConfigurationService.getStrings(conf, JMS_CONNECTIONS_PROPERTIES);
         if (connections != null) {
             mappingRules = new ArrayList<MappingRule>(connections.length);
             for (String connection : connections) {
diff --git a/core/src/main/java/org/apache/oozie/service/HadoopAccessorService.java b/core/src/main/java/org/apache/oozie/service/HadoopAccessorService.java
index 0be840d92..ed0bdc373 100644
-- a/core/src/main/java/org/apache/oozie/service/HadoopAccessorService.java
++ b/core/src/main/java/org/apache/oozie/service/HadoopAccessorService.java
@@ -91,7 +91,6 @@ public class HadoopAccessorService implements Service {
      * Supported filesystem schemes for namespace federation
      */
     public static final String SUPPORTED_FILESYSTEMS = CONF_PREFIX + "supported.filesystems";
    public static final String[] DEFAULT_SUPPORTED_SCHEMES = new String[]{"hdfs","hftp","webhdfs"};
     private Set<String> supportedSchemes;
     private boolean allSchemesSupported;
 
@@ -102,7 +101,7 @@ public class HadoopAccessorService implements Service {
 
     //for testing purposes, see XFsTestCase
     public void init(Configuration conf) throws ServiceException {
        for (String name : conf.getStringCollection(JOB_TRACKER_WHITELIST)) {
        for (String name : ConfigurationService.getStrings(conf, JOB_TRACKER_WHITELIST)) {
             String tmp = name.toLowerCase().trim();
             if (tmp.length() == 0) {
                 continue;
@@ -110,9 +109,9 @@ public class HadoopAccessorService implements Service {
             jobTrackerWhitelist.add(tmp);
         }
         XLog.getLog(getClass()).info(
                "JOB_TRACKER_WHITELIST :" + conf.getStringCollection(JOB_TRACKER_WHITELIST)
                "JOB_TRACKER_WHITELIST :" + jobTrackerWhitelist.toString()
                         + ", Total entries :" + jobTrackerWhitelist.size());
        for (String name : conf.getStringCollection(NAME_NODE_WHITELIST)) {
        for (String name : ConfigurationService.getStrings(conf, NAME_NODE_WHITELIST)) {
             String tmp = name.toLowerCase().trim();
             if (tmp.length() == 0) {
                 continue;
@@ -120,10 +119,10 @@ public class HadoopAccessorService implements Service {
             nameNodeWhitelist.add(tmp);
         }
         XLog.getLog(getClass()).info(
                "NAME_NODE_WHITELIST :" + conf.getStringCollection(NAME_NODE_WHITELIST)
                "NAME_NODE_WHITELIST :" + nameNodeWhitelist.toString()
                         + ", Total entries :" + nameNodeWhitelist.size());
 
        boolean kerberosAuthOn = conf.getBoolean(KERBEROS_AUTH_ENABLED, true);
        boolean kerberosAuthOn = ConfigurationService.getBoolean(conf, KERBEROS_AUTH_ENABLED);
         XLog.getLog(getClass()).info("Oozie Kerberos Authentication [{0}]", (kerberosAuthOn) ? "enabled" : "disabled");
         if (kerberosAuthOn) {
             kerberosInit(conf);
@@ -142,7 +141,7 @@ public class HadoopAccessorService implements Service {
         preLoadActionConfigs(conf);
 
         supportedSchemes = new HashSet<String>();
        String[] schemesFromConf = conf.getStrings(SUPPORTED_FILESYSTEMS, DEFAULT_SUPPORTED_SCHEMES);
        String[] schemesFromConf = ConfigurationService.getStrings(conf, SUPPORTED_FILESYSTEMS);
         if(schemesFromConf != null) {
             for (String scheme: schemesFromConf) {
                 scheme = scheme.trim();
@@ -161,12 +160,11 @@ public class HadoopAccessorService implements Service {
 
     private void kerberosInit(Configuration serviceConf) throws ServiceException {
             try {
                String keytabFile = serviceConf.get(KERBEROS_KEYTAB,
                                                    System.getProperty("user.home") + "/oozie.keytab").trim();
                String keytabFile = ConfigurationService.get(serviceConf, KERBEROS_KEYTAB).trim();
                 if (keytabFile.length() == 0) {
                     throw new ServiceException(ErrorCode.E0026, KERBEROS_KEYTAB);
                 }
                String principal = serviceConf.get(KERBEROS_PRINCIPAL, "oozie/localhost@LOCALHOST");
                String principal = ConfigurationService.get(serviceConf, KERBEROS_PRINCIPAL);
                 if (principal.length() == 0) {
                     throw new ServiceException(ErrorCode.E0026, KERBEROS_PRINCIPAL);
                 }
@@ -236,7 +234,8 @@ public class HadoopAccessorService implements Service {
 
     private void loadHadoopConfigs(Configuration serviceConf) throws ServiceException {
         try {
            Map<String, File> map = parseConfigDirs(serviceConf.getStrings(HADOOP_CONFS, "*=hadoop-conf"), "hadoop");
            Map<String, File> map = parseConfigDirs(ConfigurationService.getStrings(serviceConf, HADOOP_CONFS),
                    "hadoop");
             for (Map.Entry<String, File> entry : map.entrySet()) {
                 hadoopConfigs.put(entry.getKey(), loadHadoopConf(entry.getValue()));
             }
@@ -251,7 +250,7 @@ public class HadoopAccessorService implements Service {
 
     private void preLoadActionConfigs(Configuration serviceConf) throws ServiceException {
         try {
            actionConfigDirs = parseConfigDirs(serviceConf.getStrings(ACTION_CONFS, "*=hadoop-conf"), "action");
            actionConfigDirs = parseConfigDirs(ConfigurationService.getStrings(serviceConf, ACTION_CONFS), "action");
             for (String hostport : actionConfigDirs.keySet()) {
                 actionConfigs.put(hostport, new ConcurrentHashMap<String, XConfiguration>());
             }
diff --git a/core/src/main/java/org/apache/oozie/service/InstrumentationService.java b/core/src/main/java/org/apache/oozie/service/InstrumentationService.java
index 093754e56..0572f7fe3 100644
-- a/core/src/main/java/org/apache/oozie/service/InstrumentationService.java
++ b/core/src/main/java/org/apache/oozie/service/InstrumentationService.java
@@ -51,7 +51,7 @@ public class InstrumentationService implements Service {
     @Override
     public void init(Services services) throws ServiceException {
         final Instrumentation instr = new Instrumentation();
        int interval = services.getConf().getInt(CONF_LOGGING_INTERVAL, 60);
        int interval = ConfigurationService.getInt(services.getConf(), CONF_LOGGING_INTERVAL);
         initLogging(services, instr, interval);
         instr.addVariable(JVM_INSTRUMENTATION_GROUP, "free.memory", new Instrumentation.Variable<Long>() {
             @Override
diff --git a/core/src/main/java/org/apache/oozie/service/JMSAccessorService.java b/core/src/main/java/org/apache/oozie/service/JMSAccessorService.java
index a79ca7e2e..e6dc44009 100644
-- a/core/src/main/java/org/apache/oozie/service/JMSAccessorService.java
++ b/core/src/main/java/org/apache/oozie/service/JMSAccessorService.java
@@ -299,7 +299,7 @@ public class JMSAccessorService implements Service {
     }
 
     private ConnectionContext getConnectionContextImpl() {
        Class<?> defaultClazz = conf.getClass(JMS_CONNECTION_CONTEXT_IMPL, DefaultConnectionContext.class);
        Class<?> defaultClazz = ConfigurationService.getClass(conf, JMS_CONNECTION_CONTEXT_IMPL);
         ConnectionContext connCtx = null;
         if (defaultClazz == DefaultConnectionContext.class) {
             connCtx = new DefaultConnectionContext();
diff --git a/core/src/main/java/org/apache/oozie/service/JMSTopicService.java b/core/src/main/java/org/apache/oozie/service/JMSTopicService.java
index e5cf1fe5f..35c82c3e3 100644
-- a/core/src/main/java/org/apache/oozie/service/JMSTopicService.java
++ b/core/src/main/java/org/apache/oozie/service/JMSTopicService.java
@@ -103,7 +103,7 @@ public class JMSTopicService implements Service {
     }
 
     private void parseTopicConfiguration() throws ServiceException {
        String topicName = conf.get(TOPIC_NAME, "default=" + TopicType.USER.value);
        String topicName = ConfigurationService.get(conf, TOPIC_NAME);
         if (topicName == null) {
             throw new ServiceException(ErrorCode.E0100, getClass().getName(), "JMS topic cannot be null ");
         }
diff --git a/core/src/main/java/org/apache/oozie/service/JPAService.java b/core/src/main/java/org/apache/oozie/service/JPAService.java
index 8b9d1f5d7..906cb0f02 100644
-- a/core/src/main/java/org/apache/oozie/service/JPAService.java
++ b/core/src/main/java/org/apache/oozie/service/JPAService.java
@@ -137,18 +137,18 @@ public class JPAService implements Service, Instrumentable {
     public void init(Services services) throws ServiceException {
         LOG = XLog.getLog(JPAService.class);
         Configuration conf = services.getConf();
        String dbSchema = conf.get(CONF_DB_SCHEMA, "oozie");
        String url = conf.get(CONF_URL, "jdbc:derby:${oozie.home.dir}/${oozie.db.schema.name}-db;create=true");
        String driver = conf.get(CONF_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
        String user = conf.get(CONF_USERNAME, "sa");
        String password = conf.get(CONF_PASSWORD, "").trim();
        String maxConn = conf.get(CONF_MAX_ACTIVE_CONN, "10").trim();
        String dataSource = conf.get(CONF_CONN_DATA_SOURCE, "org.apache.commons.dbcp.BasicDataSource");
        String connPropsConfig = conf.get(CONF_CONN_PROPERTIES);
        boolean autoSchemaCreation = conf.getBoolean(CONF_CREATE_DB_SCHEMA, false);
        boolean validateDbConn = conf.getBoolean(CONF_VALIDATE_DB_CONN, true);
        String evictionInterval = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_INTERVAL, "300000").trim();
        String evictionNum = conf.get(CONF_VALIDATE_DB_CONN_EVICTION_NUM, "10").trim();
        String dbSchema = ConfigurationService.get(conf, CONF_DB_SCHEMA);
        String url = ConfigurationService.get(conf, CONF_URL);
        String driver = ConfigurationService.get(conf, CONF_DRIVER);
        String user = ConfigurationService.get(conf, CONF_USERNAME);
        String password = ConfigurationService.get(conf, CONF_PASSWORD).trim();
        String maxConn = ConfigurationService.get(conf, CONF_MAX_ACTIVE_CONN).trim();
        String dataSource = ConfigurationService.get(conf, CONF_CONN_DATA_SOURCE);
        String connPropsConfig = ConfigurationService.get(conf, CONF_CONN_PROPERTIES);
        boolean autoSchemaCreation = ConfigurationService.getBoolean(conf, CONF_CREATE_DB_SCHEMA);
        boolean validateDbConn = ConfigurationService.getBoolean(conf, CONF_VALIDATE_DB_CONN);
        String evictionInterval = ConfigurationService.get(conf, CONF_VALIDATE_DB_CONN_EVICTION_INTERVAL).trim();
        String evictionNum = ConfigurationService.get(conf, CONF_VALIDATE_DB_CONN_EVICTION_NUM).trim();
 
         if (!url.startsWith("jdbc:")) {
             throw new ServiceException(ErrorCode.E0608, url, "invalid JDBC URL, must start with 'jdbc:'");
diff --git a/core/src/main/java/org/apache/oozie/service/JvmPauseMonitorService.java b/core/src/main/java/org/apache/oozie/service/JvmPauseMonitorService.java
index 8bf9d1cb2..f0c72f30b 100644
-- a/core/src/main/java/org/apache/oozie/service/JvmPauseMonitorService.java
++ b/core/src/main/java/org/apache/oozie/service/JvmPauseMonitorService.java
@@ -31,6 +31,7 @@ import java.util.Map;
 import java.util.Set;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.util.Daemon;
import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.Instrumentation;
 import org.apache.oozie.util.XLog;
 
@@ -56,15 +57,13 @@ public class JvmPauseMonitorService implements Service {
      * log WARN if we detect a pause longer than this threshold
      */
     private long warnThresholdMs;
    private static final String WARN_THRESHOLD_KEY = CONF_PREFIX + "warn-threshold.ms";
    private static final long WARN_THRESHOLD_DEFAULT = 10000;
    public static final String WARN_THRESHOLD_KEY = CONF_PREFIX + "warn-threshold.ms";
 
     /**
      * log INFO if we detect a pause longer than this threshold
      */
     private long infoThresholdMs;
    private static final String INFO_THRESHOLD_KEY = CONF_PREFIX + "info-threshold.ms";
    private static final long INFO_THRESHOLD_DEFAULT = 1000;
    public static final String INFO_THRESHOLD_KEY = CONF_PREFIX + "info-threshold.ms";
 
     private Thread monitorThread;
     private volatile boolean shouldRun = true;
@@ -72,9 +71,8 @@ public class JvmPauseMonitorService implements Service {
 
     @Override
     public void init(Services services) throws ServiceException {
        Configuration conf = services.getConf();
        warnThresholdMs = conf.getLong(WARN_THRESHOLD_KEY, WARN_THRESHOLD_DEFAULT);
        infoThresholdMs = conf.getLong(INFO_THRESHOLD_KEY, INFO_THRESHOLD_DEFAULT);
        warnThresholdMs = ConfigurationService.getLong(services.getConf(), WARN_THRESHOLD_KEY);
        infoThresholdMs = ConfigurationService.getLong(services.getConf(), INFO_THRESHOLD_KEY);
 
         instrumentation = services.get(InstrumentationService.class).get();
 
diff --git a/core/src/main/java/org/apache/oozie/service/LiteWorkflowStoreService.java b/core/src/main/java/org/apache/oozie/service/LiteWorkflowStoreService.java
index 8a7017ea3..d661d0876 100644
-- a/core/src/main/java/org/apache/oozie/service/LiteWorkflowStoreService.java
++ b/core/src/main/java/org/apache/oozie/service/LiteWorkflowStoreService.java
@@ -138,8 +138,7 @@ public abstract class LiteWorkflowStoreService extends WorkflowStoreService {
     }
 
     private static int getUserRetryInterval(NodeHandler.Context context) throws WorkflowException {
        Configuration conf = Services.get().get(ConfigurationService.class).getConf();
        int ret = conf.getInt(CONF_USER_RETRY_INTEVAL, 5);
        int ret = ConfigurationService.getInt(CONF_USER_RETRY_INTEVAL);
         String userRetryInterval = context.getNodeDef().getUserRetryInterval();
 
         if (!userRetryInterval.equals("null")) {
@@ -155,8 +154,7 @@ public abstract class LiteWorkflowStoreService extends WorkflowStoreService {
 
     private static int getUserRetryMax(NodeHandler.Context context) throws WorkflowException {
         XLog log = XLog.getLog(LiteWorkflowStoreService.class);
        Configuration conf = Services.get().get(ConfigurationService.class).getConf();
        int ret = conf.getInt(CONF_USER_RETRY_MAX, 0);
        int ret = ConfigurationService.getInt(CONF_USER_RETRY_MAX);
         int max = ret;
         String userRetryMax = context.getNodeDef().getUserRetryMax();
 
@@ -184,11 +182,10 @@ public abstract class LiteWorkflowStoreService extends WorkflowStoreService {
      * @return set of error code user-retry is allowed for
      */
     public static Set<String> getUserRetryErrorCode() {
        Configuration conf = Services.get().get(ConfigurationService.class).getConf();
         // eliminating whitespaces in the error codes value specification
        String errorCodeString = conf.get(CONF_USER_RETRY_ERROR_CODE).replaceAll("\\s+", "");
        String errorCodeString = ConfigurationService.get(CONF_USER_RETRY_ERROR_CODE).replaceAll("\\s+", "");
         Collection<String> strings = StringUtils.getStringCollection(errorCodeString);
        String errorCodeExtString = conf.get(CONF_USER_RETRY_ERROR_CODE_EXT).replaceAll("\\s+", "");
        String errorCodeExtString = ConfigurationService.get(CONF_USER_RETRY_ERROR_CODE_EXT).replaceAll("\\s+", "");
         Collection<String> extra = StringUtils.getStringCollection(errorCodeExtString);
         Set<String> set = new HashSet<String>();
         set.addAll(strings);
@@ -203,8 +200,7 @@ public abstract class LiteWorkflowStoreService extends WorkflowStoreService {
      * @throws WorkflowException thrown if there was an error parsing the action configuration.
     */
     public static String getNodeDefDefaultVersion() throws WorkflowException {
        Configuration conf = Services.get().get(ConfigurationService.class).getConf();
        String ret = conf.get(CONF_NODE_DEF_VERSION);
        String ret = ConfigurationService.get(CONF_NODE_DEF_VERSION);
         if (ret == null) {
             ret = NODE_DEF_VERSION_1;
         }
diff --git a/core/src/main/java/org/apache/oozie/service/PauseTransitService.java b/core/src/main/java/org/apache/oozie/service/PauseTransitService.java
index dda540480..54865e8a7 100644
-- a/core/src/main/java/org/apache/oozie/service/PauseTransitService.java
++ b/core/src/main/java/org/apache/oozie/service/PauseTransitService.java
@@ -38,6 +38,7 @@ import org.apache.oozie.service.SchedulerService;
 import org.apache.oozie.service.Service;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.lock.LockToken;
import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.XLog;
 
 import com.google.common.annotations.VisibleForTesting;
@@ -147,8 +148,7 @@ public class PauseTransitService implements Service {
         private void updateCoord() {
             Date d = new Date(); // records the start time of this service run;
             List<CoordinatorJobBean> jobList = null;
            Configuration conf = Services.get().getConf();
            boolean backwardSupportForCoordStatus = conf.getBoolean(StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS, false);
            boolean backwardSupportForCoordStatus = ConfigUtils.isBackwardSupportForCoordStatus();
 
             // pause coordinators as needed;
             try {
@@ -200,10 +200,10 @@ public class PauseTransitService implements Service {
      */
     @Override
     public void init(Services services) {
        Configuration conf = services.getConf();
         Runnable bundlePauseStartRunnable = new PauseTransitRunnable();
         services.get(SchedulerService.class).schedule(bundlePauseStartRunnable, 10,
                conf.getInt(CONF_BUNDLE_PAUSE_START_INTERVAL, 60), SchedulerService.Unit.SEC);
                ConfigurationService.getInt(services.getConf(), CONF_BUNDLE_PAUSE_START_INTERVAL),
                SchedulerService.Unit.SEC);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/PurgeService.java b/core/src/main/java/org/apache/oozie/service/PurgeService.java
index c37080906..6e4a8e8c5 100644
-- a/core/src/main/java/org/apache/oozie/service/PurgeService.java
++ b/core/src/main/java/org/apache/oozie/service/PurgeService.java
@@ -85,11 +85,13 @@ public class PurgeService implements Service {
     @Override
     public void init(Services services) {
         Configuration conf = services.getConf();
        Runnable purgeJobsRunnable = new PurgeRunnable(conf.getInt(
                CONF_OLDER_THAN, 30), conf.getInt(COORD_CONF_OLDER_THAN, 7), conf.getInt(BUNDLE_CONF_OLDER_THAN, 7),
                                      conf.getInt(PURGE_LIMIT, 100), conf.getBoolean(PURGE_OLD_COORD_ACTION, false));
        services.get(SchedulerService.class).schedule(purgeJobsRunnable, 10, conf.getInt(CONF_PURGE_INTERVAL, 3600),
                                                      SchedulerService.Unit.SEC);
        Runnable purgeJobsRunnable = new PurgeRunnable(ConfigurationService.getInt(conf, CONF_OLDER_THAN),
                ConfigurationService.getInt(conf, COORD_CONF_OLDER_THAN),
                ConfigurationService.getInt(conf, BUNDLE_CONF_OLDER_THAN),
                ConfigurationService.getInt(conf, PURGE_LIMIT),
                ConfigurationService.getBoolean(conf, PURGE_OLD_COORD_ACTION));
        services.get(SchedulerService.class).schedule(purgeJobsRunnable, 10,
                ConfigurationService.getInt(conf, CONF_PURGE_INTERVAL), SchedulerService.Unit.SEC);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/RecoveryService.java b/core/src/main/java/org/apache/oozie/service/RecoveryService.java
index c47024d42..21bfcfcf6 100644
-- a/core/src/main/java/org/apache/oozie/service/RecoveryService.java
++ b/core/src/main/java/org/apache/oozie/service/RecoveryService.java
@@ -238,7 +238,7 @@ public class RecoveryService implements Service {
         private void runCoordActionRecovery() {
             XLog.Info.get().clear();
             XLog log = XLog.getLog(getClass());
            long pushMissingDepInterval = Services.get().getConf().getLong(CONF_PUSH_DEPENDENCY_INTERVAL, 200);
            long pushMissingDepInterval = ConfigurationService.getLong(CONF_PUSH_DEPENDENCY_INTERVAL);
             long pushMissingDepDelay = pushMissingDepInterval;
             List<CoordinatorActionBean> cactions = null;
             try {
@@ -424,7 +424,7 @@ public class RecoveryService implements Service {
             }
             this.delay = Math.max(this.delay, delay);
             delayedCallables.add(callable);
            if (delayedCallables.size() == Services.get().getConf().getInt(CONF_CALLABLE_BATCH_SIZE, 10)) {
            if (delayedCallables.size() == ConfigurationService.getInt(CONF_CALLABLE_BATCH_SIZE)){
                 boolean ret = Services.get().get(CallableQueueService.class).queueSerial(delayedCallables, this.delay);
                 if (ret == false) {
                     XLog.getLog(getClass()).warn("Unable to queue the delayedCallables commands for RecoveryService. "
@@ -445,14 +445,16 @@ public class RecoveryService implements Service {
     @Override
     public void init(Services services) {
         Configuration conf = services.getConf();
        Runnable recoveryRunnable = new RecoveryRunnable(conf.getInt(CONF_WF_ACTIONS_OLDER_THAN, 120), conf.getInt(
                CONF_COORD_OLDER_THAN, 600),conf.getInt(CONF_BUNDLE_OLDER_THAN, 600));
        Runnable recoveryRunnable = new RecoveryRunnable(
                ConfigurationService.getInt(conf, CONF_WF_ACTIONS_OLDER_THAN),
                ConfigurationService.getInt(conf, CONF_COORD_OLDER_THAN),
                ConfigurationService.getInt(conf, CONF_BUNDLE_OLDER_THAN));
         services.get(SchedulerService.class).schedule(recoveryRunnable, 10, getRecoveryServiceInterval(conf),
                                                       SchedulerService.Unit.SEC);
     }
 
     public int getRecoveryServiceInterval(Configuration conf){
        return conf.getInt(CONF_SERVICE_INTERVAL, 60);
        return ConfigurationService.getInt(conf, CONF_SERVICE_INTERVAL);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/SchedulerService.java b/core/src/main/java/org/apache/oozie/service/SchedulerService.java
index 7c7c0ff9f..b63a0045c 100644
-- a/core/src/main/java/org/apache/oozie/service/SchedulerService.java
++ b/core/src/main/java/org/apache/oozie/service/SchedulerService.java
@@ -98,7 +98,7 @@ public class SchedulerService implements Service {
      * @return int num threads
      */
     public int getSchedulableThreads(Configuration conf) {
        return conf.getInt(SCHEDULER_THREADS, 10);
        return ConfigurationService.getInt(conf, SCHEDULER_THREADS);
     }
 
     public enum Unit {
diff --git a/core/src/main/java/org/apache/oozie/service/SchemaService.java b/core/src/main/java/org/apache/oozie/service/SchemaService.java
index a696a9744..32105857f 100644
-- a/core/src/main/java/org/apache/oozie/service/SchemaService.java
++ b/core/src/main/java/org/apache/oozie/service/SchemaService.java
@@ -82,7 +82,7 @@ public class SchemaService implements Service {
         for (String baseSchema : baseSchemas) {
             sources.add(new StreamSource(IOUtils.getResourceAsStream(baseSchema, -1)));
         }
        String[] schemas = conf.getStrings(extSchema);
        String[] schemas = ConfigurationService.getStrings(conf, extSchema);
         if (schemas != null) {
             for (String schema : schemas) {
                 schema = schema.trim();
@@ -107,7 +107,6 @@ public class SchemaService implements Service {
             coordSchema = loadSchema(services.getConf(), OOZIE_COORDINATOR_XSD, COORD_CONF_EXT_SCHEMAS);
             bundleSchema = loadSchema(services.getConf(), OOZIE_BUNDLE_XSD, BUNDLE_CONF_EXT_SCHEMAS);
             slaSchema = loadSchema(services.getConf(), OOZIE_SLA_SEMANTIC_XSD, SLA_CONF_EXT_SCHEMAS);
            bundleSchema = loadSchema(services.getConf(), OOZIE_BUNDLE_XSD, BUNDLE_CONF_EXT_SCHEMAS);
         }
         catch (SAXException ex) {
             throw new ServiceException(ErrorCode.E0130, ex.getMessage(), ex);
diff --git a/core/src/main/java/org/apache/oozie/service/Service.java b/core/src/main/java/org/apache/oozie/service/Service.java
index b7863ae9a..f3b12807e 100644
-- a/core/src/main/java/org/apache/oozie/service/Service.java
++ b/core/src/main/java/org/apache/oozie/service/Service.java
@@ -29,11 +29,6 @@ public interface Service {
      */
     public static final String CONF_PREFIX = "oozie.service.";
 
    /**
     * Constant for XCommand
     */
    public static final String USE_XCOMMAND = "oozie.useXCommand";

     /**
      * Initialize the service. <p/> Invoked by the {@link Service} singleton at start up time.
      *
@@ -58,6 +53,6 @@ public interface Service {
     /**
      * Lock timeout value if service is only allowed to have one single running instance.
      */
    public static long lockTimeout = Services.get().getConf().getLong(DEFAULT_LOCK_TIMEOUT, 5 * 1000);
    public static long lockTimeout = ConfigurationService.getLong(DEFAULT_LOCK_TIMEOUT);
 
 }
diff --git a/core/src/main/java/org/apache/oozie/service/Services.java b/core/src/main/java/org/apache/oozie/service/Services.java
index eeba34f5f..585705594 100644
-- a/core/src/main/java/org/apache/oozie/service/Services.java
++ b/core/src/main/java/org/apache/oozie/service/Services.java
@@ -116,13 +116,13 @@ public class Services {
             XLog.getLog(getClass()).warn("Oozie configured to work in a timezone other than UTC: {0}",
                                          DateUtils.getOozieProcessingTimeZone().getID());
         }
        systemId = conf.get(CONF_SYSTEM_ID, ("oozie-" + System.getProperty("user.name")));
        systemId = ConfigurationService.get(conf, CONF_SYSTEM_ID);
         if (systemId.length() > MAX_SYSTEM_ID_LEN) {
             systemId = systemId.substring(0, MAX_SYSTEM_ID_LEN);
             XLog.getLog(getClass()).warn("System ID [{0}] exceeds maximum length [{1}], trimming", systemId,
                                          MAX_SYSTEM_ID_LEN);
         }
        setSystemMode(SYSTEM_MODE.valueOf(conf.get(CONF_SYSTEM_MODE, SYSTEM_MODE.NORMAL.toString())));
        setSystemMode(SYSTEM_MODE.valueOf(ConfigurationService.get(conf, CONF_SYSTEM_MODE)));
         runtimeDir = createRuntimeDir();
     }
 
@@ -191,8 +191,10 @@ public class Services {
     /**
      * Return the services configuration.
      *
     * @return services configuraiton.
     * @return services configuration.
     * @deprecated Use {@link ConfigurationService#get(String)} to retrieve property from oozie configurations.
      */
    @Deprecated
     public Configuration getConf() {
         return conf;
     }
@@ -283,9 +285,9 @@ public class Services {
         XLog log = new XLog(LogFactory.getLog(getClass()));
         try {
             Map<Class, Service> map = new LinkedHashMap<Class, Service>();
            Class[] classes = conf.getClasses(CONF_SERVICE_CLASSES);
            Class[] classes = ConfigurationService.getClasses(conf, CONF_SERVICE_CLASSES);
             log.debug("Services list obtained from property '" + CONF_SERVICE_CLASSES + "'");
            Class[] classesExt = conf.getClasses(CONF_SERVICE_EXT_CLASSES);
            Class[] classesExt = ConfigurationService.getClasses(conf, CONF_SERVICE_EXT_CLASSES);
             log.debug("Services list obtained from property '" + CONF_SERVICE_EXT_CLASSES + "'");
             List<Service> list = new ArrayList<Service>();
             loadServices(classes, list);
diff --git a/core/src/main/java/org/apache/oozie/service/ShareLibService.java b/core/src/main/java/org/apache/oozie/service/ShareLibService.java
index ea500c5b4..5414e6bd7 100644
-- a/core/src/main/java/org/apache/oozie/service/ShareLibService.java
++ b/core/src/main/java/org/apache/oozie/service/ShareLibService.java
@@ -98,14 +98,14 @@ public class ShareLibService implements Service, Instrumentable {
 
     FileSystem fs;
 
    final long retentionTime = 1000 * 60 * 60 * 24 * Services.get().getConf().getInt(LAUNCHERJAR_LIB_RETENTION, 7);
    final long retentionTime = 1000 * 60 * 60 * 24 * ConfigurationService.getInt(LAUNCHERJAR_LIB_RETENTION);
 
     @Override
     public void init(Services services) throws ServiceException {
         this.services = services;
        sharelibMappingFile = services.getConf().get(SHARELIB_MAPPING_FILE, "");
        isShipLauncherEnabled = services.getConf().getBoolean(SHIP_LAUNCHER_JAR, false);
        boolean failOnfailure = services.getConf().getBoolean(FAIL_FAST_ON_STARTUP, false);
        sharelibMappingFile = ConfigurationService.get(services.getConf(), SHARELIB_MAPPING_FILE);
        isShipLauncherEnabled = ConfigurationService.getBoolean(services.getConf(), SHIP_LAUNCHER_JAR);
        boolean failOnfailure = ConfigurationService.getBoolean(services.getConf(), FAIL_FAST_ON_STARTUP);
         Path launcherlibPath = getLauncherlibPath();
         HadoopAccessorService has = Services.get().get(HadoopAccessorService.class);
         URI uri = launcherlibPath.toUri();
@@ -145,7 +145,8 @@ public class ShareLibService implements Service, Instrumentable {
             }
         };
         services.get(SchedulerService.class).schedule(purgeLibsRunnable, 10,
                services.getConf().getInt(PURGE_INTERVAL, 1) * 60 * 60 * 24, SchedulerService.Unit.SEC);
                ConfigurationService.getInt(services.getConf(), PURGE_INTERVAL) * 60 * 60 * 24,
                SchedulerService.Unit.SEC);
     }
 
     /**
@@ -460,7 +461,7 @@ public class ShareLibService implements Service, Instrumentable {
 
         Map<String, List<Path>> tempShareLibMap = new HashMap<String, List<Path>>();
 
        if (!StringUtils.isEmpty(sharelibMappingFile)) {
        if (!StringUtils.isEmpty(sharelibMappingFile.trim())) {
             String sharelibMetaFileNewTimeStamp = JsonUtils.formatDateRfc822(new Date(fs.getFileStatus(
                     new Path(sharelibMappingFile)).getModificationTime()),"GMT");
             loadShareLibMetaFile(tempShareLibMap, sharelibMappingFile);
@@ -624,7 +625,7 @@ public class ShareLibService implements Service, Instrumentable {
         instr.addVariable("libs", "sharelib.source", new Instrumentation.Variable<String>() {
             @Override
             public String getValue() {
                if (!StringUtils.isEmpty(sharelibMappingFile)) {
                if (!StringUtils.isEmpty(sharelibMappingFile.trim())) {
                     return SHARELIB_MAPPING_FILE;
                 }
                 return WorkflowAppService.SYSTEM_LIB_PATH;
@@ -633,7 +634,7 @@ public class ShareLibService implements Service, Instrumentable {
         instr.addVariable("libs", "sharelib.mapping.file", new Instrumentation.Variable<String>() {
             @Override
             public String getValue() {
                if (!StringUtils.isEmpty(sharelibMappingFile)) {
                if (!StringUtils.isEmpty(sharelibMappingFile.trim())) {
                     return sharelibMappingFile;
                 }
                 return "(none)";
diff --git a/core/src/main/java/org/apache/oozie/service/StatusTransitService.java b/core/src/main/java/org/apache/oozie/service/StatusTransitService.java
index 77dcda97c..85ab668ac 100644
-- a/core/src/main/java/org/apache/oozie/service/StatusTransitService.java
++ b/core/src/main/java/org/apache/oozie/service/StatusTransitService.java
@@ -217,7 +217,7 @@ public class StatusTransitService implements Service {
         final Configuration conf = services.getConf();
         Runnable stateTransitRunnable = new StatusTransitRunnable();
         services.get(SchedulerService.class).schedule(stateTransitRunnable, 10,
                conf.getInt(CONF_STATUSTRANSIT_INTERVAL, 60), SchedulerService.Unit.SEC);
                ConfigurationService.getInt(conf, CONF_STATUSTRANSIT_INTERVAL), SchedulerService.Unit.SEC);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/URIHandlerService.java b/core/src/main/java/org/apache/oozie/service/URIHandlerService.java
index c0144b46c..c4a370179 100644
-- a/core/src/main/java/org/apache/oozie/service/URIHandlerService.java
++ b/core/src/main/java/org/apache/oozie/service/URIHandlerService.java
@@ -63,7 +63,7 @@ public class URIHandlerService implements Service {
     private void init(Configuration conf) throws ClassNotFoundException {
         cache = new HashMap<String, URIHandler>();
 
        String[] classes = conf.getStrings(URI_HANDLERS, FSURIHandler.class.getName());
        String[] classes = ConfigurationService.getStrings(conf, URI_HANDLERS);
         for (String classname : classes) {
             Class<?> clazz = Class.forName(classname.trim());
             URIHandler uriHandler = (URIHandler) ReflectionUtils.newInstance(clazz, null);
diff --git a/core/src/main/java/org/apache/oozie/service/UUIDService.java b/core/src/main/java/org/apache/oozie/service/UUIDService.java
index 4d209b564..cd4ee2f9f 100644
-- a/core/src/main/java/org/apache/oozie/service/UUIDService.java
++ b/core/src/main/java/org/apache/oozie/service/UUIDService.java
@@ -53,7 +53,7 @@ public class UUIDService implements Service {
      */
     @Override
     public void init(Services services) throws ServiceException {
        String genType = services.getConf().get(CONF_GENERATOR, "counter").trim();
        String genType = ConfigurationService.get(services.getConf(), CONF_GENERATOR).trim();
         if (genType.equals("counter")) {
             counter = new AtomicLong();
             resetStartTime();
@@ -208,4 +208,4 @@ public class UUIDService implements Service {
             return type;
         }
     }
}
\ No newline at end of file
}
diff --git a/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java b/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
index d07f374b5..6b6e97c1b 100644
-- a/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
++ b/core/src/main/java/org/apache/oozie/service/WorkflowAppService.java
@@ -77,7 +77,7 @@ public abstract class WorkflowAppService implements Service {
     public void init(Services services) {
         Configuration conf = services.getConf();
 
        String path = conf.get(SYSTEM_LIB_PATH, " ");
        String path = ConfigurationService.get(conf, SYSTEM_LIB_PATH);
         if (path.trim().length() > 0) {
             systemLibPath = new Path(path.trim());
         }
diff --git a/core/src/main/java/org/apache/oozie/service/XLogStreamingService.java b/core/src/main/java/org/apache/oozie/service/XLogStreamingService.java
index 721e76b76..9a42f2f8e 100644
-- a/core/src/main/java/org/apache/oozie/service/XLogStreamingService.java
++ b/core/src/main/java/org/apache/oozie/service/XLogStreamingService.java
@@ -44,7 +44,7 @@ public class XLogStreamingService implements Service, Instrumentable {
      * @throws ServiceException thrown if the log streaming service could not be initialized.
      */
     public void init(Services services) throws ServiceException {
        bufferLen = services.getConf().getInt(STREAM_BUFFER_LEN, 4096);
        bufferLen = ConfigurationService.getInt(services.getConf(), STREAM_BUFFER_LEN);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
index 6f333c83f..7fc4d17f9 100644
-- a/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
++ b/core/src/main/java/org/apache/oozie/service/ZKLocksService.java
@@ -54,7 +54,6 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
     final private HashMap<String, InterProcessReadWriteLock> zkLocks = new HashMap<String, InterProcessReadWriteLock>();
 
     private static final String REAPING_LEADER_PATH = ZKUtils.ZK_BASE_SERVICES_PATH + "/locksChildReaperLeaderPath";
    public static final int DEFAULT_REAPING_THRESHOLD = 300; // In sec
     public static final String REAPING_THRESHOLD = CONF_PREFIX + "ZKLocksService.locks.reaper.threshold";
     public static final String REAPING_THREADS = CONF_PREFIX + "ZKLocksService.locks.reaper.threads";
     private ChildReaper reaper = null;
@@ -70,7 +69,7 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
         try {
             zk = ZKUtils.register(this);
             reaper = new ChildReaper(zk.getClient(), LOCKS_NODE, Reaper.Mode.REAP_INDEFINITELY, getExecutorService(),
                    services.getConf().getInt(REAPING_THRESHOLD, DEFAULT_REAPING_THRESHOLD) * 1000, REAPING_LEADER_PATH);
                    ConfigurationService.getInt(services.getConf(), REAPING_THRESHOLD) * 1000, REAPING_LEADER_PATH);
             reaper.start();
         }
         catch (Exception ex) {
@@ -221,7 +220,7 @@ public class ZKLocksService extends MemoryLocksService implements Service, Instr
     }
 
     private static ScheduledExecutorService getExecutorService() {
        return ThreadUtils.newFixedThreadScheduledPool(Services.get().getConf().getInt(REAPING_THREADS, 2),
        return ThreadUtils.newFixedThreadScheduledPool(ConfigurationService.getInt(REAPING_THREADS),
                 "ZKLocksChildReaper");
     }
 
diff --git a/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java b/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
index 054f48402..a2bc2c577 100644
-- a/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
++ b/core/src/main/java/org/apache/oozie/servlet/AuthFilter.java
@@ -40,7 +40,7 @@ import org.apache.oozie.util.ZKUtils;
  * the configuration loading.
  */
 public class AuthFilter extends AuthenticationFilter {
    private static final String OOZIE_PREFIX = "oozie.authentication.";
    public static final String OOZIE_PREFIX = "oozie.authentication.";
 
     private HttpServlet optionsServlet;
     private ZKUtils zkUtils = null;
diff --git a/core/src/main/java/org/apache/oozie/servlet/CallbackServlet.java b/core/src/main/java/org/apache/oozie/servlet/CallbackServlet.java
index e488069f1..612302173 100644
-- a/core/src/main/java/org/apache/oozie/servlet/CallbackServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/CallbackServlet.java
@@ -32,6 +32,7 @@ import org.apache.oozie.DagEngineException;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.service.CallbackService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.DagEngineService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.IOUtils;
@@ -56,7 +57,7 @@ public class CallbackServlet extends JsonRestServlet {
 
     @Override
     public void init() {
        maxDataLen = Services.get().getConf().getInt(CONF_MAX_DATA_LEN, 2 * 1024);
        maxDataLen = ConfigurationService.getInt(CONF_MAX_DATA_LEN);
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
index 8dc960808..2ca3ce537 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
@@ -33,6 +33,7 @@ import org.apache.oozie.client.rest.*;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.coord.CoordUtils;
 import org.apache.oozie.service.BundleEngineService;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.CoordinatorEngineService;
 import org.apache.oozie.service.DagEngineService;
 import org.apache.oozie.service.Services;
@@ -824,7 +825,7 @@ public class V1JobServlet extends BaseJobServlet {
         int offset = (startStr != null) ? Integer.parseInt(startStr) : 1;
         offset = (offset < 1) ? 1 : offset;
         // Get default number of coordinator actions to be retrieved
        int defaultLen = Services.get().getConf().getInt(COORD_ACTIONS_DEFAULT_LENGTH, 1000);
        int defaultLen = ConfigurationService.getInt(COORD_ACTIONS_DEFAULT_LENGTH);
         int len = (lenStr != null) ? Integer.parseInt(lenStr) : 0;
         len = getCoordinatorJobLength(defaultLen, len);
         try {
diff --git a/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java b/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
index 188144ed1..fdce6b53c 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
++ b/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
@@ -64,6 +64,7 @@ import org.apache.oozie.executor.jpa.sla.SLASummaryGetRecordsOnRestartJPAExecuto
 import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor.SLASummaryQuery;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.lock.LockToken;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.JobsConcurrencyService;
@@ -97,8 +98,8 @@ public class SLACalculatorMemory implements SLACalculator {
 
     @Override
     public void init(Configuration conf) throws ServiceException {
        capacity = conf.getInt(SLAService.CONF_CAPACITY, 5000);
        jobEventLatency = conf.getInt(SLAService.CONF_JOB_EVENT_LATENCY, 90 * 1000);
        capacity = ConfigurationService.getInt(conf, SLAService.CONF_CAPACITY);
        jobEventLatency = ConfigurationService.getInt(conf, SLAService.CONF_JOB_EVENT_LATENCY);
         slaMap = new ConcurrentHashMap<String, SLACalcStatus>();
         historySet = Collections.synchronizedSet(new HashSet<String>());
         jpaService = Services.get().get(JPAService.class);
diff --git a/core/src/main/java/org/apache/oozie/sla/listener/SLAEmailEventListener.java b/core/src/main/java/org/apache/oozie/sla/listener/SLAEmailEventListener.java
index 8664a364c..535859fcb 100644
-- a/core/src/main/java/org/apache/oozie/sla/listener/SLAEmailEventListener.java
++ b/core/src/main/java/org/apache/oozie/sla/listener/SLAEmailEventListener.java
@@ -43,6 +43,7 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.action.email.EmailActionExecutor;
 import org.apache.oozie.action.email.EmailActionExecutor.JavaMailAuthenticator;
 import org.apache.oozie.client.event.SLAEvent;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.sla.listener.SLAEventListener;
 import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.util.XLog;
@@ -100,7 +101,7 @@ public class SLAEmailEventListener extends SLAEventListener {
     @Override
     public void init(Configuration conf) throws Exception {
 
        oozieBaseUrl = conf.get(OOZIE_BASE_URL);
        oozieBaseUrl = ConfigurationService.get(conf, OOZIE_BASE_URL);
         // Get SMTP properties from the configuration used in Email Action
         String smtpHost = conf.get(EmailActionExecutor.EMAIL_SMTP_HOST, SMTP_HOST_DEFAULT);
         String smtpPort = conf.get(EmailActionExecutor.EMAIL_SMTP_PORT, SMTP_PORT_DEFAULT);
@@ -146,7 +147,7 @@ public class SLAEmailEventListener extends SLAEventListener {
         }
 
         alertEvents = new HashSet<SLAEvent.EventStatus>();
        String alertEventsStr = conf.get(SLAService.CONF_ALERT_EVENTS);
        String alertEventsStr = ConfigurationService.get(conf, SLAService.CONF_ALERT_EVENTS);
         if (alertEventsStr != null) {
             String[] alertEvt = alertEventsStr.split(",", -1);
             for (String evt : alertEvt) {
diff --git a/core/src/main/java/org/apache/oozie/sla/service/SLAService.java b/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
index 89615bc6f..a4562e77c 100644
-- a/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
++ b/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
@@ -24,6 +24,7 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.event.JobEvent.EventStatus;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.SchedulerService;
 import org.apache.oozie.service.Service;
@@ -58,8 +59,8 @@ public class SLAService implements Service {
     public void init(Services services) throws ServiceException {
         try {
             Configuration conf = services.getConf();
            Class<? extends SLACalculator> calcClazz = (Class<? extends SLACalculator>) conf.getClass(
                    CONF_CALCULATOR_IMPL, null);
            Class<? extends SLACalculator> calcClazz = (Class<? extends SLACalculator>) ConfigurationService.getClass(
                    conf, CONF_CALCULATOR_IMPL);
             calcImpl = calcClazz == null ? new SLACalculatorMemory() : (SLACalculator) calcClazz.newInstance();
             calcImpl.init(conf);
             eventHandler = Services.get().get(EventHandlerService.class);
@@ -74,8 +75,8 @@ public class SLAService implements Service {
 
             Runnable slaThread = new SLAWorker(calcImpl);
             // schedule runnable by default every 30 sec
            int slaCheckInterval = services.getConf().getInt(CONF_SLA_CHECK_INTERVAL, 30);
            int slaCheckInitialDelay = services.getConf().getInt(CONF_SLA_CHECK_INITIAL_DELAY, 10);
            int slaCheckInterval = ConfigurationService.getInt(conf, CONF_SLA_CHECK_INTERVAL);
            int slaCheckInitialDelay = ConfigurationService.getInt(conf, CONF_SLA_CHECK_INITIAL_DELAY);
             services.get(SchedulerService.class).schedule(slaThread, slaCheckInitialDelay, slaCheckInterval,
                     SchedulerService.Unit.SEC);
             slaEnabled = true;
diff --git a/core/src/main/java/org/apache/oozie/util/ConfigUtils.java b/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
index ca0ce2407..a56c5a295 100644
-- a/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
++ b/core/src/main/java/org/apache/oozie/util/ConfigUtils.java
@@ -19,7 +19,8 @@
 package org.apache.oozie.util;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.StatusTransitService;
 import org.apache.oozie.servlet.ServicesLoader;
 
 /**
@@ -28,6 +29,12 @@ import org.apache.oozie.servlet.ServicesLoader;
 public class ConfigUtils {
     private final static XLog LOG = XLog.getLog(ConfigUtils.class);
 
    public static boolean BOOLEAN_DEFAULT = false;
    public static String STRING_DEFAULT = "";
    public static int INT_DEFAULT = 0;
    public static float FLOAT_DEFAULT = 0f;
    public static long LONG_DEFAULT = 0l;

     /**
      * Fetches a property using both a deprecated name and the new name. The deprecated property
      * has precedence over the new name. If the deprecated name is used a warning is written to
@@ -85,13 +92,13 @@ public class ConfigUtils {
         else {
             sb.append("http://");
         }
        sb.append(Services.get().getConf().get("oozie.http.hostname"));
        sb.append(ConfigurationService.get("oozie.http.hostname"));
         sb.append(":");
         if (secure) {
            sb.append(Services.get().getConf().get("oozie.https.port"));
            sb.append(ConfigurationService.get("oozie.https.port"));
         }
         else {
            sb.append(Services.get().getConf().get("oozie.http.port"));
            sb.append(ConfigurationService.get("oozie.http.port"));
         }
         sb.append("/oozie");
         return sb.toString();
@@ -105,4 +112,8 @@ public class ConfigUtils {
     public static String getOozieEffectiveUrl() {
         return getOozieURL(ServicesLoader.isSSLEnabled());
     }

    public static boolean isBackwardSupportForCoordStatus() {
        return ConfigurationService.getBoolean(StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS);
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/util/DateUtils.java b/core/src/main/java/org/apache/oozie/util/DateUtils.java
index 958762501..ec9d0be6d 100644
-- a/core/src/main/java/org/apache/oozie/util/DateUtils.java
++ b/core/src/main/java/org/apache/oozie/util/DateUtils.java
@@ -32,6 +32,7 @@ import java.util.regex.Pattern;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.service.ConfigurationService;
 
 /**
  * Date utility classes to parse and format datetimes in Oozie expected datetime formats.
@@ -65,7 +66,7 @@ public class DateUtils {
      * @param conf Oozie server configuration.
      */
     public static void setConf(Configuration conf) {
        String tz = conf.get(OOZIE_PROCESSING_TIMEZONE_KEY, OOZIE_PROCESSING_TIMEZONE_DEFAULT);
        String tz = ConfigurationService.get(conf, OOZIE_PROCESSING_TIMEZONE_KEY);
         if (!VALID_TIMEZONE_PATTERN.matcher(tz).matches()) {
             throw new RuntimeException("Invalid Oozie timezone, it must be 'UTC' or 'GMT(+/-)####");
         }
diff --git a/core/src/main/java/org/apache/oozie/util/StatusUtils.java b/core/src/main/java/org/apache/oozie/util/StatusUtils.java
index 93b619349..24eba2158 100644
-- a/core/src/main/java/org/apache/oozie/util/StatusUtils.java
++ b/core/src/main/java/org/apache/oozie/util/StatusUtils.java
@@ -21,6 +21,7 @@ package org.apache.oozie.util;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.client.Job;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.SchemaService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.StatusTransitService;
@@ -37,9 +38,7 @@ public class StatusUtils {
         Job.Status newStatus = null;
         if (coordJob != null) {
             newStatus = coordJob.getStatus();
            Configuration conf = Services.get().getConf();
            boolean backwardSupportForCoordStatus = conf.getBoolean(
                    StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS, false);
            boolean backwardSupportForCoordStatus = ConfigUtils.isBackwardSupportForCoordStatus();
             if (backwardSupportForCoordStatus) {
                 if (coordJob.getAppNamespace() != null
                         && coordJob.getAppNamespace().equals(SchemaService.COORDINATOR_NAMESPACE_URI_1)) {
@@ -79,9 +78,7 @@ public class StatusUtils {
         Job.Status newStatus = null;
         if (coordJob != null) {
             newStatus = coordJob.getStatus();
            Configuration conf = Services.get().getConf();
            boolean backwardSupportForCoordStatus = conf.getBoolean(
                    StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS, false);
            boolean backwardSupportForCoordStatus = ConfigUtils.isBackwardSupportForCoordStatus();
             if (backwardSupportForCoordStatus) {
                 if (coordJob.getAppNamespace() != null
                         && coordJob.getAppNamespace().equals(SchemaService.COORDINATOR_NAMESPACE_URI_1)) {
@@ -108,9 +105,7 @@ public class StatusUtils {
     public static boolean getStatusForCoordActionInputCheck(CoordinatorJobBean coordJob) {
         boolean ret = false;
         if (coordJob != null) {
            Configuration conf = Services.get().getConf();
            boolean backwardSupportForCoordStatus = conf.getBoolean(
                    StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS, false);
            boolean backwardSupportForCoordStatus = ConfigUtils.isBackwardSupportForCoordStatus();
             if (backwardSupportForCoordStatus) {
                 if (coordJob.getAppNamespace() != null
                         && coordJob.getAppNamespace().equals(SchemaService.COORDINATOR_NAMESPACE_URI_1)) {
@@ -137,8 +132,7 @@ public class StatusUtils {
         boolean ret = false;
         if (coordJob != null) {
             Configuration conf = Services.get().getConf();
            boolean backwardSupportForCoordStatus = conf.getBoolean(
                    StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_COORD_STATUS, false);
            boolean backwardSupportForCoordStatus = ConfigUtils.isBackwardSupportForCoordStatus();
             if (backwardSupportForCoordStatus) {
                 if (coordJob.getAppNamespace() != null
                         && coordJob.getAppNamespace().equals(SchemaService.COORDINATOR_NAMESPACE_URI_1)) {
@@ -154,14 +148,13 @@ public class StatusUtils {
     /**
      * Get the status of coordinator job for Oozie versions (3.2 and before) when RUNNINGWITHERROR,
      * SUSPENDEDWITHERROR and PAUSEDWITHERROR are not supported
     * @param coordJob
     * @param currentJobStatus
      * @return
      */
     public static Job.Status getStatusIfBackwardSupportTrue(Job.Status currentJobStatus) {
         Job.Status newStatus = currentJobStatus;
        Configuration conf = Services.get().getConf();
        boolean backwardSupportForStatesWithoutError = conf.getBoolean(
                StatusTransitService.CONF_BACKWARD_SUPPORT_FOR_STATES_WITHOUT_ERROR, true);
        boolean backwardSupportForStatesWithoutError = ConfigurationService.getBoolean(StatusTransitService
                .CONF_BACKWARD_SUPPORT_FOR_STATES_WITHOUT_ERROR);
         if (backwardSupportForStatesWithoutError) {
             if (currentJobStatus == Job.Status.PAUSEDWITHERROR) {
                 newStatus = Job.Status.PAUSED;
diff --git a/core/src/main/java/org/apache/oozie/util/XLogFilter.java b/core/src/main/java/org/apache/oozie/util/XLogFilter.java
index 0a350db2d..5c0d1f320 100644
-- a/core/src/main/java/org/apache/oozie/util/XLogFilter.java
++ b/core/src/main/java/org/apache/oozie/util/XLogFilter.java
@@ -29,6 +29,7 @@ import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.StringUtils;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.Services;
 
 import com.google.common.annotations.VisibleForTesting;
@@ -307,7 +308,7 @@ public class XLogFilter {
         }
         long diffHours = (endDate.getTime() - startDate.getTime()) / (60 * 60 * 1000);
         if (isActionList) {
            int actionLogDuration = Services.get().getConf().getInt(MAX_ACTIONLIST_SCAN_DURATION, -1);
            int actionLogDuration = ConfigurationService.getInt(MAX_ACTIONLIST_SCAN_DURATION);
             if (actionLogDuration == -1) {
                 return;
             }
@@ -319,7 +320,7 @@ public class XLogFilter {
             }
         }
         else {
            int logDuration = Services.get().getConf().getInt(MAX_SCAN_DURATION, -1);
            int logDuration = ConfigurationService.getInt(MAX_SCAN_DURATION);
             if (logDuration == -1) {
                 return;
             }
diff --git a/core/src/main/java/org/apache/oozie/util/ZKUtils.java b/core/src/main/java/org/apache/oozie/util/ZKUtils.java
index f535f86a6..6162178be 100644
-- a/core/src/main/java/org/apache/oozie/util/ZKUtils.java
++ b/core/src/main/java/org/apache/oozie/util/ZKUtils.java
@@ -48,6 +48,7 @@ import static org.apache.oozie.service.HadoopAccessorService.KERBEROS_KEYTAB;
 import static org.apache.oozie.service.HadoopAccessorService.KERBEROS_PRINCIPAL;
 
 import org.apache.oozie.event.listener.ZKConnectionListener;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.ServiceException;
 import org.apache.oozie.service.Services;
 import org.apache.zookeeper.ZooDefs.Perms;
@@ -132,7 +133,10 @@ public class ZKUtils {
      */
     private ZKUtils() throws Exception {
         log = XLog.getLog(getClass());
        zkId = Services.get().getConf().get(OOZIE_INSTANCE_ID, Services.get().getConf().get("oozie.http.hostname"));
        zkId = ConfigurationService.get(OOZIE_INSTANCE_ID);
        if (zkId.isEmpty()) {
            zkId = ConfigurationService.get("oozie.http.hostname");
        }
         createClient();
         advertiseService();
         checkAndSetACLs();
@@ -173,9 +177,9 @@ public class ZKUtils {
     private void createClient() throws Exception {
         // Connect to the ZooKeeper server
         RetryPolicy retryPolicy = ZKUtils.getRetryPolicy();
        String zkConnectionString = Services.get().getConf().get(ZK_CONNECTION_STRING, "localhost:2181");
        String zkConnectionString = ConfigurationService.get(ZK_CONNECTION_STRING);
         String zkNamespace = getZKNameSpace();
        zkConnectionTimeout = Services.get().getConf().getInt(ZK_CONNECTION_TIMEOUT, 180);
        zkConnectionTimeout = ConfigurationService.getInt(ZK_CONNECTION_TIMEOUT);
 
         ACLProvider aclProvider;
         if (Services.get().getConf().getBoolean(ZK_SECURE, false)) {
@@ -413,7 +417,7 @@ public class ZKUtils {
      * @return oozie.zookeeper.namespace
      */
     public static String getZKNameSpace() {
        return Services.get().getConf().get(ZK_NAMESPACE, "oozie");
        return ConfigurationService.get(ZK_NAMESPACE);
     }
     /**
      * Return ZK connection timeout
diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index cfa8697ab..c85701129 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.workflow.lite;
 
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.workflow.WorkflowException;
 import org.apache.oozie.util.ELUtils;
 import org.apache.oozie.util.IOUtils;
@@ -160,7 +161,8 @@ public class LiteWorkflowAppParser {
             traversed.put(app.getNode(StartNodeDef.START).getName(), VisitStatus.VISITING);
             validate(app, app.getNode(StartNodeDef.START), traversed);
             //Validate whether fork/join are in pair or not
            if (jobConf.getBoolean(WF_VALIDATE_FORK_JOIN, true) && Services.get().getConf().getBoolean(VALIDATE_FORK_JOIN, true)) {
            if (jobConf.getBoolean(WF_VALIDATE_FORK_JOIN, true)
                    && ConfigurationService.getBoolean(VALIDATE_FORK_JOIN)) {
                 validateForkJoin(app);
             }
             return app;
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 6cd7fdfce..17155a144 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -222,6 +222,14 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.ConfigurationService.verify.available.properties</name>
        <value>true</value>
        <description>
            Specifies whether the available configurations check is enabled or not.
        </description>
    </property>

     <!-- SchedulerService -->
 
     <property>
@@ -464,11 +472,11 @@
     </property>
 
     <property>
		<name>oozie.service.coord.normal.default.timeout
		</name>
		<value>10080</value>
		<description>Default timeout for a coordinator action input check (in minutes) for normal job.
            </description>
        <name>oozie.service.coord.normal.default.timeout
        </name>
        <value>120</value>
        <description>Default timeout for a coordinator action input check (in minutes) for normal job.
            -1 means infinite timeout</description>
 	</property>
 
 	<property>
@@ -555,6 +563,24 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.ELService.ext.constants.job-submit</name>
        <value> </value>
        <description>
            EL constant declarations, separated by commas, format is [PREFIX:]NAME=CLASS#CONSTANT.
            This property is a convenience property to add extensions without having to include all the built in ones.
        </description>
    </property>

    <property>
        <name>oozie.service.ELService.ext.functions.job-submit</name>
        <value> </value>
        <description>
            EL functions declarations, separated by commas, format is [PREFIX:]NAME=CLASS#METHOD.
            This property is a convenience property to add extensions without having to include all the built in ones.
        </description>
    </property>

 <!-- Workflow specifics -->
     <property>
         <name>oozie.service.ELService.constants.workflow</name>
@@ -729,6 +755,34 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.ELService.ext.functions.coord-job-submit-freq</name>
        <value>
        </value>
        <description>
            EL functions declarations, separated by commas, format is [PREFIX:]NAME=CLASS#METHOD.
            This property is a convenience property to add extensions to the built in executors without having to
            include all the built in ones.
        </description>
    </property>

    <property>
        <name>oozie.service.ELService.constants.coord-job-wait-timeout</name>
        <value> </value>
        <description>
            EL functions declarations, separated by commas, format is [PREFIX:]NAME=CLASS#METHOD.
        </description>
    </property>

    <property>
        <name>oozie.service.ELService.ext.constants.coord-job-wait-timeout</name>
        <value> </value>
        <description>
            EL functions declarations, separated by commas, format is [PREFIX:]NAME=CLASS#METHOD.
            This property is a convenience property to add extensions without having to include all the built in ones.
        </description>
    </property>

     <property>
         <name>oozie.service.ELService.functions.coord-job-wait-timeout</name>
         <value>
@@ -744,13 +798,11 @@
     </property>
 
     <property>
        <name>oozie.service.ELService.ext.functions.coord-job-submit-freq</name>
        <value>
        </value>
        <name>oozie.service.ELService.ext.functions.coord-job-wait-timeout</name>
        <value> </value>
         <description>
             EL functions declarations, separated by commas, format is [PREFIX:]NAME=CLASS#METHOD.
            This property is a convenience property to add extensions to the built in executors without having to
            include all the built in ones.
            This property is a convenience property to add extensions without having to include all the built in ones.
         </description>
     </property>
 
@@ -1308,7 +1360,15 @@
             DataSource to be used for connection pooling.
         </description>
     </property>
    

    <property>
        <name>oozie.service.JPAService.connection.properties</name>
        <value> </value>
        <description>
            DataSource connection properties.
        </description>
    </property>

     <property>
         <name>oozie.service.JPAService.jdbc.driver</name>
         <value>org.apache.derby.jdbc.EmbeddedDriver</value>
@@ -1359,7 +1419,12 @@
 
     <property>
         <name>oozie.service.SchemaService.wf.ext.schemas</name>
        <value>oozie-sla-0.1.xsd,oozie-sla-0.2.xsd</value>
        <value>
            shell-action-0.1.xsd,shell-action-0.2.xsd,shell-action-0.3.xsd,email-action-0.1.xsd,email-action-0.2.xsd,
            hive-action-0.2.xsd,hive-action-0.3.xsd,hive-action-0.4.xsd,hive-action-0.5.xsd,sqoop-action-0.2.xsd,
            sqoop-action-0.3.xsd,sqoop-action-0.4.xsd,ssh-action-0.1.xsd,ssh-action-0.2.xsd,distcp-action-0.1.xsd,
            distcp-action-0.2.xsd,oozie-sla-0.1.xsd,oozie-sla-0.2.xsd,hive2-action-0.1.xsd
        </value>
         <description>
             Schemas for additional actions types.
 
@@ -1367,7 +1432,7 @@
                        if empty Configuration assumes it is NULL.
         </description>
     </property>
    

     <property>
         <name>oozie.service.SchemaService.coord.ext.schemas</name>
         <value>oozie-sla-0.1.xsd,oozie-sla-0.2.xsd</value>
@@ -1440,8 +1505,14 @@
             org.apache.oozie.action.hadoop.FsActionExecutor,
             org.apache.oozie.action.hadoop.MapReduceActionExecutor,
             org.apache.oozie.action.hadoop.PigActionExecutor,
            org.apache.oozie.action.hadoop.HiveActionExecutor,
            org.apache.oozie.action.hadoop.ShellActionExecutor,
            org.apache.oozie.action.hadoop.SqoopActionExecutor,
            org.apache.oozie.action.hadoop.DistcpActionExecutor,
            org.apache.oozie.action.hadoop.Hive2ActionExecutor,
             org.apache.oozie.action.ssh.SshActionExecutor,
            org.apache.oozie.action.oozie.SubWorkflowActionExecutor
            org.apache.oozie.action.oozie.SubWorkflowActionExecutor,
            org.apache.oozie.action.email.EmailActionExecutor
         </value>
         <description>
             List of ActionExecutors classes (separated by commas).
@@ -1529,6 +1600,23 @@
         </description>
     </property>
 
    <!-- LauncherMapper -->
    <property>
        <name>oozie.action.max.output.data</name>
        <value>2048</value>
        <description>
            Max size in characters for output data.
        </description>
    </property>

    <property>
        <name>oozie.action.fs.glob.max</name>
        <value>1000</value>
        <description>
            Maximum number of globbed files.
        </description>
    </property>

     <!-- JavaActionExecutor -->
     <!-- This is common to the subclasses of action executors for Java (e.g. map-reduce, pig, hive, java, etc) -->
 
@@ -1541,7 +1629,7 @@
             action.
         </description>
     </property>
    

     <!-- HadoopActionExecutor -->
     <!-- This is common to the subclasses action executors for map-reduce and pig -->
 
@@ -1553,24 +1641,6 @@
         </description>
     </property>
 
    <property>
        <name>oozie.action.hadoop.delete.hdfs.tmp.dir</name>
        <value>false</value>
        <description>
            If set to true, it will delete temporary directory at the end of execution of map reduce action.
        </description>
    </property>

    <!-- PigActionExecutor -->

    <property>
        <name>oozie.action.pig.delete.hdfs.tmp.dir</name>
        <value>false</value>
        <description>
            If set to true, it will delete temporary directory at the end of execution of pig action.
        </description>
    </property>

     <!-- SshActionExecutor -->
 
     <property>
@@ -1718,7 +1788,7 @@
 
     <property>
         <name>oozie.service.WorkflowAppService.system.libpath</name>
        <value>hdfs:///user/${user.name}/share/lib</value>
        <value>/user/${user.name}/share/lib</value>
         <description>
             System library path to use for workflow applications.
             This path is added to workflow application if their job properties sets
@@ -1726,17 +1796,6 @@
         </description>
     </property>
 
    <property>
        <name>use.system.libpath.for.mapreduce.and.pig.jobs</name>
        <value>false</value>
        <description>
            If set to true, submissions of MapReduce and Pig jobs will include
            automatically the system library path, thus not requiring users to
            specify where the Pig JAR files are. Instead, the ones from the system
            library path are used.
        </description>
    </property>

     <property>
         <name>oozie.command.default.lock.timeout</name>
         <value>5000</value>
@@ -1825,7 +1884,7 @@
 
     <property>
       <name>oozie.authentication.cookie.domain</name>
      <value></value>
      <value> </value>
       <description>
         The domain to use for the HTTP cookie that stores the authentication token.
         In order to authentiation to work correctly across multiple hosts
@@ -1914,13 +1973,13 @@
 		</description>
 	</property>
 
        <property>
                <name>oozie.service.URIHandlerService.uri.handlers</name>
                <value>org.apache.oozie.dependency.FSURIHandler</value>
                <description>
                        Enlist the different uri handlers supported for data availability checks.
                </description>
        </property>
    <property>
        <name>oozie.service.URIHandlerService.uri.handlers</name>
        <value>org.apache.oozie.dependency.FSURIHandler</value>
        <description>
                Enlist the different uri handlers supported for data availability checks.
        </description>
    </property>
     <!-- Oozie HTTP Notifications -->
 
     <property>
@@ -2082,22 +2141,35 @@
         </description>
     </property>
 
<!--
     <property>
        <name>oozie.instance.id</name>
        <value>${OOZIE_HTTP_HOSTNAME}</value>
        <name>oozie.http.hostname</name>
        <value>localhost</value>
         <description>
        Each Oozie server should have its own unique instance id. The default is system property
        =${OOZIE_HTTP_HOSTNAME}= (i.e. the hostname).
            Oozie server host name.
         </description>
     </property>
-->
 
    <property>
        <name>oozie.http.port</name>
        <value>11000</value>
        <description>
            Oozie server port.
        </description>
    </property>

    <property>
        <name>oozie.instance.id</name>
        <value>${oozie.http.hostname}</value>
        <description>
            Each Oozie server should have its own unique instance id. The default is system property
            =${OOZIE_HTTP_HOSTNAME}= (i.e. the hostname).
        </description>
    </property>
 
     <!-- Sharelib Configuration -->
     <property>
         <name>oozie.service.ShareLibService.mapping.file</name>
        <value></value>
        <value> </value>
         <description>
             Sharelib mapping files contains list of key=value,
             where key will be the sharelib name for the action and value is a comma separated list of
@@ -2125,6 +2197,22 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.ShareLibService.temp.sharelib.retention.days</name>
        <value>7</value>
        <description>
            ShareLib retention time in days.
        </description>
    </property>

    <property>
        <name>oozie.action.ship.launcher.jar</name>
        <value>false</value>
        <description>
            Specifies whether launcher jar is shipped or not.
        </description>
    </property>

     <property>
         <name>oozie.action.jobinfo.enable</name>
         <value>false</value>
@@ -2150,7 +2238,7 @@
     </property>
 
     <property>
        <name>oozie.service.XLogStreamingService.max.actionlist.log.scan.duration</name>
        <name>oozie.service.XLogStreamingService.actionlist.max.log.scan.duration</name>
         <value>-1</value>
         <description>
         Max log scan duration in hours for coordinator job when list of actions are specified.
@@ -2210,6 +2298,15 @@
         </description>
     </property>
 
    <property>
        <name>oozie.service.AbandonedCoordCheckerService.check.delay
        </name>
        <value>60</value>
        <description>
            Delay, in minutes, at which AbandonedCoordCheckerService should run.
        </description>
    </property>

     <property>
         <name>oozie.service.AbandonedCoordCheckerService.failure.limit
         </name>
diff --git a/core/src/test/java/org/apache/oozie/action/email/TestEmailActionExecutor.java b/core/src/test/java/org/apache/oozie/action/email/TestEmailActionExecutor.java
index aa9db5ea4..e1f314ebd 100644
-- a/core/src/test/java/org/apache/oozie/action/email/TestEmailActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/email/TestEmailActionExecutor.java
@@ -43,12 +43,6 @@ public class TestEmailActionExecutor extends ActionExecutorTestCase {
         server.start();
     }
 
    @Override
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes", EmailActionExecutor.class.getName());
    }

     private Context createNormalContext(String actionXml) throws Exception {
         EmailActionExecutor ae = new EmailActionExecutor();
 
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
index d6ac5542a..939a33295 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestDistCpActionExecutor.java
@@ -40,12 +40,6 @@ import org.apache.oozie.util.XConfiguration;
 
 public class TestDistCpActionExecutor extends ActionExecutorTestCase{
 
    @Override
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes", DistcpActionExecutor.class.getName());
    }

     @SuppressWarnings("unchecked")
     public void testSetupMethods() throws Exception {
         DistcpActionExecutor ae = new DistcpActionExecutor();
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestShellActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestShellActionExecutor.java
index dcc440a0b..7795963ed 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestShellActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestShellActionExecutor.java
@@ -58,16 +58,9 @@ public class TestShellActionExecutor extends ActionExecutorTestCase {
             : "ls -ltr\necho $1 $2\nexit 1";
     private static final String PERL_SCRIPT_CONTENT = "print \"MY_VAR=TESTING\";";
 
    @Override
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes", ShellActionExecutor.class.getName());
    }

     /**
      * Verify if the ShellActionExecutor indeed setups the basic stuffs
      *
     * @param launcherJarShouldExist
      * @throws Exception
      */
     public void testSetupMethods() throws Exception {
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionNotificationXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionNotificationXCommand.java
index 7742dd4b8..b58ecd94a 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionNotificationXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionNotificationXCommand.java
@@ -73,6 +73,6 @@ public class TestCoordActionNotificationXCommand extends XTestCase {
         command.call();
         long end = System.currentTimeMillis();
         Assert.assertTrue(end - start >= 50);
        Assert.assertTrue(end - start <= NotificationXCommand.NOTIFICATION_URL_CONNECTION_TIMEOUT_DEFAULT);
        Assert.assertTrue(end - start <= 10000);
     }
 }
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestNotificationXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestNotificationXCommand.java
index b260747fd..ad2fbd798 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestNotificationXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestNotificationXCommand.java
@@ -73,6 +73,6 @@ public class TestNotificationXCommand extends XTestCase {
         command.call();
         long end = System.currentTimeMillis();
         Assert.assertTrue(end - start >= 50);
        Assert.assertTrue(end - start < NotificationXCommand.NOTIFICATION_URL_CONNECTION_TIMEOUT_DEFAULT);
        Assert.assertTrue(end - start < 10000);
     }
 }
diff --git a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
index 239fc8624..fa128df4c 100644
-- a/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/wf/TestReRunXCommand.java
@@ -126,7 +126,6 @@ public class TestReRunXCommand extends XDataTestCase {
      */
     public void testRerunFork() throws Exception {
         // We need the shell schema and action for this test
        Services.get().getConf().set(ActionService.CONF_ACTION_EXECUTOR_EXT_CLASSES, ShellActionExecutor.class.getName());
         Services.get().setService(ActionService.class);
         Services.get().getConf().set(SchemaService.WF_CONF_EXT_SCHEMAS, "shell-action-0.3.xsd");
         Services.get().setService(SchemaService.class);
diff --git a/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java b/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
index 2c008fd71..d09bfc062 100644
-- a/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
++ b/core/src/test/java/org/apache/oozie/service/TestConfigurationService.java
@@ -19,15 +19,45 @@
 package org.apache.oozie.service;
 
 import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.action.hadoop.CredentialsProvider;
import org.apache.oozie.action.hadoop.DistcpActionExecutor;
import org.apache.oozie.action.hadoop.JavaActionExecutor;
import org.apache.oozie.action.hadoop.LauncherMapper;
import org.apache.oozie.command.coord.CoordActionInputCheckXCommand;
import org.apache.oozie.command.coord.CoordSubmitXCommand;
import org.apache.oozie.command.wf.JobXCommand;
import org.apache.oozie.compression.CodecFactory;
import org.apache.oozie.event.listener.ZKConnectionListener;
import org.apache.oozie.executor.jpa.CoordActionGetForInfoJPAExecutor;
import org.apache.oozie.servlet.AuthFilter;
import org.apache.oozie.servlet.V1JobServlet;
import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.test.XTestCase;
import org.apache.oozie.util.ConfigUtils;
 import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.XLogFilter;
import org.apache.oozie.workflow.lite.LiteWorkflowAppParser;
 
import java.io.DataOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 
 public class TestConfigurationService extends XTestCase {
 
    Services services;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }

     private void prepareOozieConfDir(String oozieSite) throws Exception {
         prepareOozieConfDir(oozieSite, ConfigurationService.SITE_CONFIG_FILE);
     }
@@ -116,4 +146,131 @@ public class TestConfigurationService extends XTestCase {
         assertEquals("true", conf.get("oozie.is.awesome"));
         cl.destroy();
     }

    public void testOozieConfig() throws Exception{
        prepareOozieConfDir("oozie-site2.xml");
        ConfigurationService cl = new ConfigurationService();
        cl.init(null);
        assertEquals("SITE1", cl.getConf().get("oozie.system.id"));
        assertEquals("SITE2", cl.getConf().get("oozie.dummy"));
        assertEquals("SITE1", ConfigurationService.get(cl.getConf(), "oozie.system.id"));
        assertEquals("SITE2", ConfigurationService.get(cl.getConf(), "oozie.dummy"));

        assertNull(cl.getConf().get("oozie.test.nonexist"));
        assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get(cl.getConf(), "oozie.test.nonexist"));
        assertEquals(ConfigUtils.BOOLEAN_DEFAULT, ConfigurationService.getBoolean(cl.getConf(), "oozie.test.nonexist"));

        Configuration testConf = new Configuration(false);
        assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get("test.nonexist"));
        assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get(testConf, "test.nonexist"));
        testConf.set("test.nonexist", "another-conf");
        assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get("test.nonexist"));
        assertEquals("another-conf", ConfigurationService.get(testConf, "test.nonexist"));
        Services.get().getConf().set("test.nonexist", "oozie-conf");
        assertEquals("oozie-conf", ConfigurationService.get("test.nonexist"));
        assertEquals("another-conf", ConfigurationService.get(testConf, "test.nonexist"));
        testConf.clear();
        assertEquals("oozie-conf", ConfigurationService.get("test.nonexist"));
        assertEquals(ConfigUtils.STRING_DEFAULT, ConfigurationService.get(testConf, "test.nonexist"));

        assertEquals("http://localhost:8080/oozie/callback", ConfigurationService.get(CallbackService.CONF_BASE_URL));
        assertEquals("gz", ConfigurationService.get(CodecFactory.COMPRESSION_OUTPUT_CODEC));
        assertEquals(4096, ConfigurationService.getInt(XLogStreamingService.STREAM_BUFFER_LEN));
        assertEquals(10000,  ConfigurationService.getLong(JvmPauseMonitorService.WARN_THRESHOLD_KEY));
        assertEquals(60, ConfigurationService.getInt(InstrumentationService.CONF_LOGGING_INTERVAL));
        assertEquals(30, ConfigurationService.getInt(PurgeService.CONF_OLDER_THAN));
        assertEquals(7, ConfigurationService.getInt(PurgeService.COORD_CONF_OLDER_THAN));
        assertEquals(7, ConfigurationService.getInt(PurgeService.BUNDLE_CONF_OLDER_THAN));
        assertEquals(100, ConfigurationService.getInt(PurgeService.PURGE_LIMIT));
        assertEquals(3600, ConfigurationService.getInt(PurgeService.CONF_PURGE_INTERVAL));
        assertEquals(300, ConfigurationService.getInt(CoordMaterializeTriggerService.CONF_LOOKUP_INTERVAL));
        assertEquals(0, ConfigurationService.getInt(CoordMaterializeTriggerService.CONF_SCHEDULING_INTERVAL));
        assertEquals(300, cl.getConf().getInt(
                CoordMaterializeTriggerService.CONF_SCHEDULING_INTERVAL,
                ConfigurationService.getInt(CoordMaterializeTriggerService.CONF_LOOKUP_INTERVAL)));
        assertEquals(3600, ConfigurationService.getInt(CoordMaterializeTriggerService.CONF_MATERIALIZATION_WINDOW));
        assertEquals(10, ConfigurationService.getInt(CoordMaterializeTriggerService.CONF_CALLABLE_BATCH_SIZE));
        assertEquals(50, ConfigurationService.getInt(CoordMaterializeTriggerService
                .CONF_MATERIALIZATION_SYSTEM_LIMIT));
        assertEquals(0.05f, ConfigurationService.getFloat(CoordSubmitXCommand.CONF_MAT_THROTTLING_FACTOR));

        assertEquals("oozie", ConfigurationService.get(JPAService.CONF_DB_SCHEMA));
        assertEquals("jdbc:hsqldb:mem:oozie-db;create=true", ConfigurationService.get(JPAService.CONF_URL));
        assertEquals("org.hsqldb.jdbcDriver", ConfigurationService.get(JPAService.CONF_DRIVER));
        assertEquals("sa", ConfigurationService.get(JPAService.CONF_USERNAME));
        assertEquals("", ConfigurationService.get(JPAService.CONF_PASSWORD).trim());
        assertEquals("10", ConfigurationService.get(JPAService.CONF_MAX_ACTIVE_CONN).trim());
        assertEquals("org.apache.commons.dbcp.BasicDataSource",
                ConfigurationService.get(JPAService.CONF_CONN_DATA_SOURCE));
        assertEquals("", ConfigurationService.get(JPAService.CONF_CONN_PROPERTIES).trim());
        assertEquals("300000", ConfigurationService.get(JPAService.CONF_VALIDATE_DB_CONN_EVICTION_INTERVAL).trim());
        assertEquals("10", ConfigurationService.get(JPAService.CONF_VALIDATE_DB_CONN_EVICTION_NUM).trim());

        assertEquals(2048, ConfigurationService.getInt(LauncherMapper.CONF_OOZIE_ACTION_MAX_OUTPUT_DATA));
        assertEquals("http://localhost:8080/oozie?job=", ConfigurationService.get(JobXCommand.CONF_CONSOLE_URL));
        assertEquals(false, ConfigurationService.getBoolean(JavaActionExecutor.CONF_HADOOP_YARN_UBER_MODE));
        assertEquals(false, ConfigurationService.getBoolean(HadoopAccessorService.KERBEROS_AUTH_ENABLED));

        assertEquals(0, ConfigurationService.getStrings("no.defined").length);
        assertEquals(0, ConfigurationService.getStrings(CredentialsProvider.CRED_KEY).length);
        assertEquals(1, ConfigurationService.getStrings(DistcpActionExecutor.CLASS_NAMES).length);
        assertEquals("distcp=org.apache.hadoop.tools.DistCp",
                ConfigurationService.getStrings(DistcpActionExecutor.CLASS_NAMES)[0]);
        assertEquals(1, ConfigurationService.getInt(CoordActionInputCheckXCommand.COORD_EXECUTION_NONE_TOLERANCE));
        assertEquals(1000, ConfigurationService.getInt(V1JobServlet.COORD_ACTIONS_DEFAULT_LENGTH));

        assertEquals(cl.getConf().get(LiteWorkflowStoreService.CONF_USER_RETRY_ERROR_CODE), ConfigurationService.get
                (LiteWorkflowStoreService.CONF_USER_RETRY_ERROR_CODE));
        assertEquals(cl.getConf().get(LiteWorkflowStoreService.CONF_USER_RETRY_ERROR_CODE_EXT),
                ConfigurationService.get(LiteWorkflowStoreService.CONF_USER_RETRY_ERROR_CODE_EXT));

        assertEquals("simple", cl.getConf().get(AuthFilter.OOZIE_PREFIX + AuthFilter.AUTH_TYPE));
        assertEquals("36000", cl.getConf().get(AuthFilter.OOZIE_PREFIX + AuthFilter.AUTH_TOKEN_VALIDITY));
        assertEquals(" ", cl.getConf().get(AuthFilter.OOZIE_PREFIX + AuthFilter.COOKIE_DOMAIN));
        assertEquals("true", cl.getConf().get(AuthFilter.OOZIE_PREFIX + "simple.anonymous.allowed"));
        assertEquals("HTTP/localhost@LOCALHOST", cl.getConf().get(AuthFilter.OOZIE_PREFIX + "kerberos.principal"));
        assertEquals(cl.getConf().get(HadoopAccessorService.KERBEROS_KEYTAB),
                cl.getConf().get(AuthFilter.OOZIE_PREFIX + "kerberos.keytab"));
        assertEquals("DEFAULT", cl.getConf().get(AuthFilter.OOZIE_PREFIX + "kerberos.name.rules"));

        assertEquals(true, ConfigurationService.getBoolean(LiteWorkflowAppParser.VALIDATE_FORK_JOIN));
        assertEquals(false,
                ConfigurationService.getBoolean(CoordActionGetForInfoJPAExecutor.COORD_GET_ALL_COLS_FOR_ACTION));
        assertEquals(1, ConfigurationService.getStrings(URIHandlerService.URI_HANDLERS).length);
        assertEquals("org.apache.oozie.dependency.FSURIHandler",
                ConfigurationService.getStrings(URIHandlerService.URI_HANDLERS)[0]);
        assertEquals(cl.getConf().getBoolean("oozie.hadoop-2.0.2-alpha.workaround.for.distributed.cache", false),
                ConfigurationService.getBoolean(LauncherMapper.HADOOP2_WORKAROUND_DISTRIBUTED_CACHE));

        assertEquals("org.apache.oozie.event.MemoryEventQueue",
                (ConfigurationService.getClass(cl.getConf(), EventHandlerService.CONF_EVENT_QUEUE).getName()));
        assertEquals(-1, ConfigurationService.getInt(XLogFilter.MAX_SCAN_DURATION));
        assertEquals(-1, ConfigurationService.getInt(XLogFilter.MAX_ACTIONLIST_SCAN_DURATION));
        assertEquals(10000, ConfigurationService.getLong(JvmPauseMonitorService.WARN_THRESHOLD_KEY));
        assertEquals(1000, ConfigurationService.getLong(JvmPauseMonitorService.INFO_THRESHOLD_KEY));

        assertEquals(10000, ConfigurationService.getInt(CallableQueueService.CONF_QUEUE_SIZE));
        assertEquals(10, ConfigurationService.getInt(CallableQueueService.CONF_THREADS));
        assertEquals(3, ConfigurationService.getInt(CallableQueueService.CONF_CALLABLE_CONCURRENCY));
        assertEquals(120, ConfigurationService.getInt(CoordSubmitXCommand.CONF_DEFAULT_TIMEOUT_NORMAL));

        assertEquals(300, ConfigurationService.getInt(ZKLocksService.REAPING_THRESHOLD));
        assertEquals(2, ConfigurationService.getInt(ZKLocksService.REAPING_THREADS));
        assertEquals(10000, ConfigurationService.getInt(JobXCommand.DEFAULT_REQUEUE_DELAY));

        assertEquals(0, ConfigurationService.getStrings(AbandonedCoordCheckerService.TO_ADDRESS).length);
        assertEquals(25, ConfigurationService.getInt(AbandonedCoordCheckerService.CONF_FAILURE_LEN));
        assertEquals(false, ConfigurationService.getBoolean(AbandonedCoordCheckerService.CONF_JOB_KILL));
        assertEquals(60, ConfigurationService.getInt(AbandonedCoordCheckerService.CONF_CHECK_DELAY));
        assertEquals(1440, ConfigurationService.getInt(AbandonedCoordCheckerService.CONF_CHECK_INTERVAL));
        assertEquals(2880, ConfigurationService.getInt(AbandonedCoordCheckerService.CONF_JOB_OLDER_THAN));

        assertEquals(true, ConfigurationService.getBoolean(ZKConnectionListener.CONF_SHUTDOWN_ON_TIMEOUT));

        assertEquals(7, ConfigurationService.getInt(ShareLibService.LAUNCHERJAR_LIB_RETENTION));
        assertEquals(5000, ConfigurationService.getInt(SLAService.CONF_CAPACITY));

        cl.destroy();
    }

 }
diff --git a/core/src/test/java/org/apache/oozie/service/TestJobsConcurrencyService.java b/core/src/test/java/org/apache/oozie/service/TestJobsConcurrencyService.java
index 503d0c918..011a57422 100644
-- a/core/src/test/java/org/apache/oozie/service/TestJobsConcurrencyService.java
++ b/core/src/test/java/org/apache/oozie/service/TestJobsConcurrencyService.java
@@ -119,7 +119,7 @@ public class TestJobsConcurrencyService extends XTestCase {
         try {
             jcs.init(Services.get());
             jcs.instrument(instr);
            String servers = System.getProperty("oozie.instance.id") + "=" + ConfigUtils.getOozieEffectiveUrl();
            String servers = ConfigurationService.get("oozie.instance.id") + "=" + ConfigUtils.getOozieEffectiveUrl();
             assertEquals(servers, instr.getVariables().get("oozie").get("servers").getValue());
         } finally {
             jcs.destroy();
diff --git a/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java b/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
index db3f6ebe9..c70ef794e 100644
-- a/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
++ b/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
@@ -96,7 +96,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     @Test
     public void testLoadOnRestart() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job-1", AppType.WORKFLOW_JOB);
         String jobId1 = slaRegBean1.getId();
         SLARegistrationBean slaRegBean2 = _createSLARegistration("job-2", AppType.WORKFLOW_JOB);
@@ -156,7 +156,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         BatchQueryExecutor.getInstance().executeBatchInsertUpdateDelete(null, updateList, null);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         assertEquals(2, slaCalcMemory.size());
 
@@ -201,7 +201,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testWorkflowJobSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job-1", AppType.WORKFLOW_JOB);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -228,7 +228,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         WorkflowJobQueryExecutor.getInstance().insert(wjb);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -257,7 +257,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_ALL, slaSummaryBean);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         assertEquals(0, slaCalcMemory.size());
         slaSummary = SLASummaryQueryExecutor.getInstance().get(SLASummaryQuery.GET_SLA_SUMMARY, jobId1);
@@ -281,7 +281,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_ALL, slaSummaryBean);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         assertEquals(1, slaCalcMemory.size());
         SLACalcStatus calc = slaCalcMemory.get(jobId1);
@@ -297,7 +297,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testWorkflowActionSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job@1", AppType.WORKFLOW_ACTION);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -322,7 +322,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         jpaService.execute(wfInsertCmd);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -343,7 +343,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testCoordinatorActionSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job@1", AppType.COORDINATOR_ACTION);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -373,7 +373,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         WorkflowJobQueryExecutor.getInstance().insert(wjb);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -394,7 +394,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAEvents1() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         slaRegBean.setExpectedStart(new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000)); // 1 hour
@@ -445,7 +445,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAEvents2() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
 
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
@@ -505,7 +505,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         // test start-miss
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000); // 1 hour back
@@ -534,7 +534,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testDuplicateEndMiss() throws Exception {
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() + 1 * 1 * 3600 * 1000); // 1 hour ahead
@@ -577,7 +577,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAHistorySet() throws Exception {
             EventHandlerService ehs = Services.get().get(EventHandlerService.class);
             SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
            slaCalcMemory.init(new Configuration(false));
            slaCalcMemory.init(Services.get().getConf());
             WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
             SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
             Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000);
@@ -614,7 +614,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testHistoryPurge() throws Exception{
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(new Configuration(false));
        slaCalcMemory.init(Services.get().getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000);
diff --git a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
index b75535371..93ad168a4 100644
-- a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
++ b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
@@ -30,6 +30,7 @@ import org.apache.oozie.service.ActionService;
 import org.apache.oozie.service.LiteWorkflowStoreService;
 import org.apache.oozie.service.SchemaService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.service.TestLiteWorkflowAppService;
 import org.apache.oozie.workflow.WorkflowException;
 import org.apache.oozie.workflow.lite.TestLiteWorkflowLib.TestActionNodeHandler;
 import org.apache.oozie.workflow.lite.TestLiteWorkflowLib.TestDecisionNodeHandler;
@@ -48,8 +49,6 @@ public class TestLiteWorkflowAppParser extends XTestCase {
         super.setUp();
         setSystemProperty("oozie.service.SchemaService.wf.ext.schemas", "hive-action-0.2.xsd");
         new Services().init();
        Services.get().get(ActionService.class).register(HiveActionExecutor.class);
        Services.get().get(ActionService.class).register(DistcpActionExecutor.class);
     }
 
     @Override
@@ -298,17 +297,7 @@ public class TestLiteWorkflowAppParser extends XTestCase {
         parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid.xml", -1), new Configuration());
 
         try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-loop1-invalid.xml", -1), new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0707, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }

        try {
            // Check TestLiteWorkflowAppService.TestActionExecutor is registered.
             parser.validateAndParse(IOUtils.getResourceAsReader("wf-unsupported-action.xml", -1), new Configuration());
             fail();
         }
diff --git a/core/src/test/resources/wf-unsupported-action.xml b/core/src/test/resources/wf-unsupported-action.xml
index 7a796cbed..5ac92afcb 100644
-- a/core/src/test/resources/wf-unsupported-action.xml
++ b/core/src/test/resources/wf-unsupported-action.xml
@@ -19,13 +19,16 @@
     <start to="a"/>
 
     <action name="a">
        <email>
            <to>to</to>
            <subject>subject</subject>
            <message/>
        </email>
        <ok to="b"/>
        <error to="b"/>
        <test xmlns="uri:test">
            <signal-value>${wf:conf('signal-value')}</signal-value>
            <external-status>${wf:conf('external-status')}</external-status>
            <error>${wf:conf('error')}</error>
            <avoid-set-execution-data>${wf:conf('avoid-set-execution-data')}</avoid-set-execution-data>
            <avoid-set-end-data>${wf:conf('avoid-set-end-data')}</avoid-set-end-data>
            <running-mode>${wf:conf('running-mode')}</running-mode>
        </test>
        <ok to="end"/>
        <error to="kill"/>
     </action>
 
     <end name="b"/>
diff --git a/release-log.txt b/release-log.txt
index 8ed6dd8ff..a57365ee4 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-1890 Make oozie-site empty and reconcile defaults between oozie-default and the code (seoeun25 via rkanter)
 OOZIE-2001 Workflow re-runs doesn't update coord action status (jaydeepvishwakarma via shwethags)
 OOZIE-2048 HadoopAccessorService should also process ssl_client.xml (venkatnrangan via bzhang)
 OOZIE-2047 Oozie does not support Hive tables that use datatypes introduced since Hive 0.8 (venkatnrangan via bzhang)
diff --git a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
index 87c97d855..c28839c5c 100644
-- a/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
++ b/sharelib/hive/src/test/java/org/apache/oozie/action/hadoop/TestHiveActionExecutor.java
@@ -63,12 +63,6 @@ public class TestHiveActionExecutor extends ActionExecutorTestCase {
     private static final String OUTPUT_DIRNAME = "output";
     private static final String DATA_FILENAME = "data.txt";
 
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes",
                HiveActionExecutor.class.getName());
    }

     @SuppressWarnings("unchecked")
     public void testSetupMethods() throws Exception {
         HiveActionExecutor ae = new HiveActionExecutor();
diff --git a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
index 4923fe311..9c3128f80 100644
-- a/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
++ b/sharelib/oozie/src/main/java/org/apache/oozie/action/hadoop/LauncherMapper.java
@@ -54,13 +54,12 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     static final String CONF_OOZIE_ACTION_MAIN_CLASS = "oozie.launcher.action.main.class";
 
     static final String ACTION_PREFIX = "oozie.action.";
    static final String CONF_OOZIE_ACTION_MAX_OUTPUT_DATA = ACTION_PREFIX + "max.output.data";
    public static final String CONF_OOZIE_ACTION_MAX_OUTPUT_DATA = ACTION_PREFIX + "max.output.data";
     static final String CONF_OOZIE_ACTION_MAIN_ARG_COUNT = ACTION_PREFIX + "main.arg.count";
     static final String CONF_OOZIE_ACTION_MAIN_ARG_PREFIX = ACTION_PREFIX + "main.arg.";
     static final String CONF_OOZIE_EXTERNAL_STATS_MAX_SIZE = "oozie.external.stats.max.size";
     static final String OOZIE_ACTION_CONFIG_CLASS = ACTION_PREFIX + "config.class";
    static final String CONF_OOZIE_ACTION_FS_GLOB_MAX = "oozie.action.fs.glob.max";
    static final int GLOB_MAX_DEFAULT = 1000;
    static final String CONF_OOZIE_ACTION_FS_GLOB_MAX = ACTION_PREFIX + "fs.glob.max";
 
     static final String COUNTER_GROUP = "oozie.launcher";
     static final String COUNTER_LAUNCHER_ERROR = "oozie.launcher.error";
@@ -78,6 +77,7 @@ public class LauncherMapper<K1, V1, K2, V2> implements Mapper<K1, V1, K2, V2>, R
     static final String ACTION_DATA_STATS = "stats.properties";
     static final String ACTION_DATA_NEW_ID = "newId";
     static final String ACTION_DATA_ERROR_PROPS = "error.properties";
    public static final String HADOOP2_WORKAROUND_DISTRIBUTED_CACHE = "oozie.hadoop-2.0.2-alpha.workaround.for.distributed.cache";
 
     private void setRecoveryId(Configuration launcherConf, Path actionDir, String recoveryId) throws LauncherException {
         try {
diff --git a/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java b/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
index 8686a23f3..64740920b 100644
-- a/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
++ b/sharelib/sqoop/src/test/java/org/apache/oozie/action/hadoop/TestSqoopActionExecutor.java
@@ -119,12 +119,6 @@ public class TestSqoopActionExecutor extends ActionExecutorTestCase {
             "<arg>{3}</arg>" +
             "</sqoop>";
 
    @Override
    protected void setSystemProps() throws Exception {
        super.setSystemProps();
        setSystemProperty("oozie.service.ActionService.executor.classes", SqoopActionExecutor.class.getName());
    }

     public void testSetupMethods() throws Exception {
         SqoopActionExecutor ae = new SqoopActionExecutor();
         assertEquals(SqoopMain.class, ae.getLauncherClasses().get(0));
- 
2.19.1.windows.1

