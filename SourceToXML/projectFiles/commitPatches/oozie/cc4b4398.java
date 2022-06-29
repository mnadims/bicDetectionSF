From cc4b4398f279777662ed59f7cde7abb50c12ccb2 Mon Sep 17 00:00:00 2001
From: Rohini Palaniswamy <rohinip@yahoo-inc.com>
Date: Wed, 16 Apr 2014 10:15:18 -0700
Subject: [PATCH] OOZIE-1769 An option to update coord properties/definition
 (puru via rohini)

--
 .../java/org/apache/oozie/cli/OozieCLI.java   |  37 ++-
 .../org/apache/oozie/client/OozieClient.java  |  67 ++++
 .../apache/oozie/client/rest/JsonTags.java    |   2 +
 .../oozie/client/rest/RestConstants.java      |   4 +
 core/pom.xml                                  |  10 +
 .../org/apache/oozie/CoordinatorEngine.java   |  22 ++
 .../org/apache/oozie/CoordinatorJobBean.java  |   2 +-
 .../main/java/org/apache/oozie/ErrorCode.java |   2 +
 .../command/coord/CoordSubmitXCommand.java    | 103 +++---
 .../command/coord/CoordUpdateXCommand.java    | 269 ++++++++++++++++
 .../executor/jpa/CoordJobQueryExecutor.java   |   1 +
 .../apache/oozie/servlet/BaseJobServlet.java  |  20 ++
 .../apache/oozie/servlet/V0JobServlet.java    |   6 +
 .../apache/oozie/servlet/V1JobServlet.java    |   9 +-
 .../apache/oozie/servlet/V2JobServlet.java    |  44 +++
 .../coord/TestCoordUpdateXCommand.java        | 297 ++++++++++++++++++
 core/src/test/resources/coord-update-test.xml |  59 ++++
 docs/src/site/twiki/DG_CommandLineTool.twiki  |  54 ++++
 docs/src/site/twiki/WebServicesAPI.twiki      |  32 +-
 pom.xml                                       |   6 +
 release-log.txt                               |   1 +
 21 files changed, 1002 insertions(+), 45 deletions(-)
 create mode 100644 core/src/main/java/org/apache/oozie/command/coord/CoordUpdateXCommand.java
 create mode 100644 core/src/test/java/org/apache/oozie/command/coord/TestCoordUpdateXCommand.java
 create mode 100644 core/src/test/resources/coord-update-test.xml

diff --git a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
index e3eb3b06d..c31c8eef3 100644
-- a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
++ b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
@@ -115,6 +115,8 @@ public class OozieCLI {
     public static final String DEFINITION_OPTION = "definition";
     public static final String CONFIG_CONTENT_OPTION = "configcontent";
     public static final String SQOOP_COMMAND_OPTION = "command";
    public static final String SHOWDIFF_OPTION = "diff";
    public static final String UPDATE_OPTION = "update";
 
     public static final String DO_AS_OPTION = "doas";
 
@@ -275,6 +277,9 @@ public class OozieCLI {
                 "rerun a job  (coordinator requires -action or -date, bundle requires -coordinator or -date)");
         Option dryrun = new Option(DRYRUN_OPTION, false, "Dryrun a workflow (since 3.3.2) or coordinator (since 2.0) job without"
                 + " actually executing it");
        Option update = new Option(UPDATE_OPTION, true, "Update coord definition and properties");
        Option showdiff = new Option(SHOWDIFF_OPTION, true,
                "Show diff of the new coord definition and properties with the existing one (default true)");
         Option start = new Option(START_OPTION, true, "start a job");
         Option suspend = new Option(SUSPEND_OPTION, true, "suspend a job");
         Option resume = new Option(RESUME_OPTION, true, "resume a job");
@@ -325,6 +330,7 @@ public class OozieCLI {
         actions.addOption(resume);
         actions.addOption(kill);
         actions.addOption(change);
        actions.addOption(update);
         actions.addOption(info);
         actions.addOption(rerun);
         actions.addOption(log);
@@ -353,6 +359,12 @@ public class OozieCLI {
         jobOptions.addOption(getAllWorkflows);
         jobOptions.addOptionGroup(actions);
         addAuthOptions(jobOptions);
        jobOptions.addOption(showdiff);

        //Needed to make dryrun and update mutually exclusive options
        OptionGroup updateOption = new OptionGroup();
        updateOption.addOption(dryrun);
        jobOptions.addOptionGroup(updateOption);
         return jobOptions;
     }
 
@@ -829,7 +841,7 @@ public class OozieCLI {
             else if (options.contains(START_OPTION)) {
                 wc.start(commandLine.getOptionValue(START_OPTION));
             }
            else if (options.contains(DRYRUN_OPTION)) {
            else if (options.contains(DRYRUN_OPTION) && !options.contains(UPDATE_OPTION)) {
                 String dryrunStr = wc.dryrun(getConfiguration(wc, commandLine));
                 if (dryrunStr.equals("OK")) {  // workflow
                     System.out.println("OK");
@@ -1064,6 +1076,29 @@ public class OozieCLI {
                             + "] doesn't end with either C or W or B");
                 }
             }
            else if (options.contains(UPDATE_OPTION)) {
                String coordJobId = commandLine.getOptionValue(UPDATE_OPTION);
                Properties conf = null;

                String dryrun = "";
                String showdiff = "";

                if (commandLine.getOptionValue(CONFIG_OPTION) != null) {
                    conf = getConfiguration(wc, commandLine);
                }
                if (options.contains(DRYRUN_OPTION)) {
                    dryrun = "true";
                }
                if (commandLine.getOptionValue(SHOWDIFF_OPTION) != null) {
                    showdiff = commandLine.getOptionValue(SHOWDIFF_OPTION);
                }
                if (conf == null) {
                    System.out.println(wc.updateCoord(coordJobId, dryrun, showdiff));
                }
                else {
                    System.out.println(wc.updateCoord(coordJobId, conf, dryrun, showdiff));
                }
            }
         }
         catch (OozieClientException ex) {
             throw new OozieCLIException(ex.toString(), ex);
diff --git a/client/src/main/java/org/apache/oozie/client/OozieClient.java b/client/src/main/java/org/apache/oozie/client/OozieClient.java
index 40c956272..129579a3b 100644
-- a/client/src/main/java/org/apache/oozie/client/OozieClient.java
++ b/client/src/main/java/org/apache/oozie/client/OozieClient.java
@@ -627,6 +627,73 @@ public class OozieClient {
         }
     }
 
    /**
     * Update coord definition.
     *
     * @param jobId the job id
     * @param conf the conf
     * @param dryrun the dryrun
     * @param showDiff the show diff
     * @return the string
     * @throws OozieClientException the oozie client exception
     */
    public String updateCoord(String jobId, Properties conf, String dryrun, String showDiff)
            throws OozieClientException {
        return (new UpdateCoord(jobId, conf, dryrun, showDiff)).call();
    }

    /**
     * Update coord definition without properties.
     *
     * @param jobId the job id
     * @param dryrun the dryrun
     * @param showDiff the show diff
     * @return the string
     * @throws OozieClientException the oozie client exception
     */
    public String updateCoord(String jobId, String dryrun, String showDiff) throws OozieClientException {
        return (new UpdateCoord(jobId, dryrun, showDiff)).call();
    }

    /**
     * The Class UpdateCoord.
     */
    private class UpdateCoord extends ClientCallable<String> {
        private final Properties conf;

        public UpdateCoord(String jobId, Properties conf, String jobActionDryrun, String showDiff) {
            super("PUT", RestConstants.JOB, notEmpty(jobId, "jobId"), prepareParams(RestConstants.ACTION_PARAM,
                    RestConstants.JOB_COORD_UPDATE, RestConstants.JOB_ACTION_DRYRUN, jobActionDryrun,
                    RestConstants.JOB_ACTION_SHOWDIFF, showDiff));
            this.conf = conf;
        }

        public UpdateCoord(String jobId, String jobActionDryrun, String showDiff) {
            this(jobId, new Properties(), jobActionDryrun, showDiff);
        }

        @Override
        protected String call(HttpURLConnection conn) throws IOException, OozieClientException {
            conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
            writeToXml(conf, conn.getOutputStream());

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject json = (JSONObject) JSONValue.parse(new InputStreamReader(conn.getInputStream()));
                JSONObject update = (JSONObject) json.get(JsonTags.COORD_UPDATE);
                if (update != null) {
                    return (String) update.get(JsonTags.COORD_UPDATE_DIFF);
                }
                else {
                    return "";
                }
            }
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handleError(conn);
            }
            return null;
        }
    }

     /**
      * dryrun for a given job
      *
diff --git a/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java b/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
index ae391c717..c79147496 100644
-- a/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
++ b/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
@@ -228,5 +228,7 @@ public interface JsonTags {
     public static final String JMS_TOPIC_PREFIX = "jmsTopicPrefix";
 
     public static final String JMS_TOPIC_NAME = "jmsTopicName";
    public static final String COORD_UPDATE = RestConstants.JOB_COORD_UPDATE;
    public static final String COORD_UPDATE_DIFF = "diff";
 
 }
diff --git a/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java b/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
index a7fe06e3f..b0dd6cb52 100644
-- a/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
++ b/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
@@ -56,6 +56,8 @@ public interface RestConstants {
 
     public static final String JOB_ACTION_DRYRUN = "dryrun";
 
    public static final String JOB_ACTION_SHOWDIFF = "diff";

     public static final String JOB_ACTION_SUSPEND = "suspend";
 
     public static final String JOB_ACTION_RESUME = "resume";
@@ -70,6 +72,8 @@ public interface RestConstants {
 
     public static final String JOB_COORD_ACTION_RERUN = "coord-rerun";
 
    public static final String JOB_COORD_UPDATE = "update";

     public static final String JOB_BUNDLE_ACTION_RERUN = "bundle-rerun";
 
     public static final String JOB_SHOW_PARAM = "show";
diff --git a/core/pom.xml b/core/pom.xml
index f0547a386..c935dd744 100644
-- a/core/pom.xml
++ b/core/pom.xml
@@ -327,6 +327,16 @@
             <artifactId>collections-generic</artifactId>
             <scope>compile</scope>
         </dependency>
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.apache.httpcomponents</groupId>
                    <artifactId>httpclient</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
     </dependencies>
 
     <build>
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
index 6a17ce4a0..81d98bf80 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
@@ -50,6 +50,7 @@ import org.apache.oozie.command.coord.CoordRerunXCommand;
 import org.apache.oozie.command.coord.CoordResumeXCommand;
 import org.apache.oozie.command.coord.CoordSubmitXCommand;
 import org.apache.oozie.command.coord.CoordSuspendXCommand;
import org.apache.oozie.command.coord.CoordUpdateXCommand;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
@@ -718,4 +719,25 @@ public class CoordinatorEngine extends BaseEngine {
         }
         return wfBeans;
     }

    /**
     * Update coord job definition.
     *
     * @param conf the conf
     * @param jobId the job id
     * @param dryrun the dryrun
     * @param showDiff the show diff
     * @return the string
     * @throws CoordinatorEngineException the coordinator engine exception
     */
    public String updateJob(Configuration conf, String jobId, boolean dryrun, boolean showDiff)
            throws CoordinatorEngineException {
        try {
            CoordUpdateXCommand update = new CoordUpdateXCommand(dryrun, conf, jobId, showDiff);
            return update.call();
        }
        catch (CommandException ex) {
            throw new CoordinatorEngineException(ex);
        }
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
index 5eb134b30..4a2ea3943 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
@@ -51,7 +51,7 @@ import org.json.simple.JSONObject;
 
 @Entity
 @NamedQueries( {
        @NamedQuery(name = "UPDATE_COORD_JOB", query = "update CoordinatorJobBean w set w.appName = :appName, w.appPath = :appPath,w.concurrency = :concurrency, w.conf = :conf, w.externalId = :externalId, w.frequency = :frequency, w.lastActionNumber = :lastActionNumber, w.timeOut = :timeOut, w.timeZone = :timeZone, w.createdTimestamp = :createdTime, w.endTimestamp = :endTime, w.execution = :execution, w.jobXml = :jobXml, w.lastActionTimestamp = :lastAction, w.lastModifiedTimestamp = :lastModifiedTime, w.nextMaterializedTimestamp = :nextMaterializedTime, w.origJobXml = :origJobXml, w.slaXml=:slaXml, w.startTimestamp = :startTime, w.statusStr = :status, w.timeUnitStr = :timeUnit, w.appNamespace = :appNamespace, w.bundleId = :bundleId where w.id = :id"),
        @NamedQuery(name = "UPDATE_COORD_JOB", query = "update CoordinatorJobBean w set w.appName = :appName, w.appPath = :appPath,w.concurrency = :concurrency, w.conf = :conf, w.externalId = :externalId, w.frequency = :frequency, w.lastActionNumber = :lastActionNumber, w.timeOut = :timeOut, w.timeZone = :timeZone, w.createdTimestamp = :createdTime, w.endTimestamp = :endTime, w.execution = :execution, w.jobXml = :jobXml, w.lastActionTimestamp = :lastAction, w.lastModifiedTimestamp = :lastModifiedTime, w.nextMaterializedTimestamp = :nextMaterializedTime, w.origJobXml = :origJobXml, w.slaXml=:slaXml, w.startTimestamp = :startTime, w.statusStr = :status, w.timeUnitStr = :timeUnit, w.appNamespace = :appNamespace, w.bundleId = :bundleId, w.matThrottling = :matThrottling  where w.id = :id"),
 
         @NamedQuery(name = "UPDATE_COORD_JOB_STATUS", query = "update CoordinatorJobBean w set w.statusStr =:status, w.lastModifiedTimestamp = :lastModifiedTime where w.id = :id"),
 
diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index f69d7a24e..ee7292bd9 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -203,6 +203,8 @@ public enum ErrorCode {
     E1020(XLog.STD, "Could not kill coord job, this job either finished successfully or does not exist , [{0}]"),
     E1021(XLog.STD, "Coord Action Input Check Error: {0}"),
     E1022(XLog.STD, "Cannot delete running/completed coordinator action: [{0}]"),
    E1023(XLog.STD, "Coord Job update Error: [{0}]"),

 
     E1100(XLog.STD, "Command precondition does not hold before execution, [{0}]"),
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
index d215180aa..654e9b808 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
@@ -96,11 +96,11 @@ import org.xml.sax.SAXException;
  */
 public class CoordSubmitXCommand extends SubmitTransitionXCommand {
 
    private Configuration conf;
    protected Configuration conf;
     private final String bundleId;
     private final String coordName;
    private boolean dryrun;
    private JPAService jpaService = null;
    protected boolean dryrun;
    protected JPAService jpaService = null;
     private CoordinatorJob.Status prevStatus = CoordinatorJob.Status.PREP;
 
     public static final String CONFIG_DEFAULT = "coord-config-default.xml";
@@ -113,7 +113,7 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
     private static final Set<String> DISALLOWED_USER_PROPERTIES = new HashSet<String>();
     private static final Set<String> DISALLOWED_DEFAULT_PROPERTIES = new HashSet<String>();
 
    private CoordinatorJobBean coordJob = null;
    protected CoordinatorJobBean coordJob = null;
     /**
      * Default timeout for normal jobs, in minutes, after which coordinator input check will timeout
      */
@@ -198,8 +198,14 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
      */
     @Override
     protected String submit() throws CommandException {
        String jobId = null;
         LOG.info("STARTED Coordinator Submit");
        String jobId = submitJob();
        LOG.info("ENDED Coordinator Submit jobId=" + jobId);
        return jobId;
    }

    protected String submitJob() throws CommandException {
        String jobId = null;
         InstrumentUtils.incrJobCounter(getName(), 1, getInstrumentation());
 
         boolean exceptionOccured = false;
@@ -230,42 +236,16 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
 
             LOG.debug("jobXml after all validation " + XmlUtils.prettyPrint(eJob).toString());
 
            jobId = storeToDB(eJob, coordJob);
            jobId = storeToDB(appXml, eJob, coordJob);
             // log job info for coordinator job
             LogUtils.setLogInfo(coordJob, logInfo);
             LOG = XLog.resetPrefix(LOG);
 
             if (!dryrun) {
                // submit a command to materialize jobs for the next 1 hour (3600 secs)
                // so we don't wait 10 mins for the Service to run.
                queue(new CoordMaterializeTransitionXCommand(jobId, 3600), 100);
                queueMaterializeTransitionXCommand(jobId);
             }
             else {
                Date startTime = coordJob.getStartTime();
                long startTimeMilli = startTime.getTime();
                long endTimeMilli = startTimeMilli + (3600 * 1000);
                Date jobEndTime = coordJob.getEndTime();
                Date endTime = new Date(endTimeMilli);
                if (endTime.compareTo(jobEndTime) > 0) {
                    endTime = jobEndTime;
                }
                jobId = coordJob.getId();
                LOG.info("[" + jobId + "]: Update status to RUNNING");
                coordJob.setStatus(Job.Status.RUNNING);
                coordJob.setPending();
                CoordActionMaterializeCommand coordActionMatCom = new CoordActionMaterializeCommand(jobId, startTime,
                        endTime);
                Configuration jobConf = null;
                try {
                    jobConf = new XConfiguration(new StringReader(coordJob.getConf()));
                }
                catch (IOException e1) {
                    LOG.warn("Configuration parse error. read from DB :" + coordJob.getConf(), e1);
                }
                String action = coordActionMatCom.materializeJobs(true, coordJob, jobConf, null);
                String output = coordJob.getJobXml() + System.getProperty("line.separator")
                + "***actions for instance***" + action;
                return output;
                return getDryRun(coordJob);
             }
         }
         catch (JDOMException jex) {
@@ -295,17 +275,59 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
         }
         finally {
             if (exceptionOccured) {
                if(coordJob.getId() == null || coordJob.getId().equalsIgnoreCase("")){
                if (coordJob.getId() == null || coordJob.getId().equalsIgnoreCase("")) {
                     coordJob.setStatus(CoordinatorJob.Status.FAILED);
                     coordJob.resetPending();
                 }
             }
         }

        LOG.info("ENDED Coordinator Submit jobId=" + jobId);
         return jobId;
     }
 
    /**
     * Gets the dryrun output.
     *
     * @param jobId the job id
     * @return the dry run
     * @throws Exception the exception
     */
    protected String getDryRun(CoordinatorJobBean coordJob) throws Exception{
        Date startTime = coordJob.getStartTime();
        long startTimeMilli = startTime.getTime();
        long endTimeMilli = startTimeMilli + (3600 * 1000);
        Date jobEndTime = coordJob.getEndTime();
        Date endTime = new Date(endTimeMilli);
        if (endTime.compareTo(jobEndTime) > 0) {
            endTime = jobEndTime;
        }
        String jobId = coordJob.getId();
        LOG.info("[" + jobId + "]: Update status to RUNNING");
        coordJob.setStatus(Job.Status.RUNNING);
        coordJob.setPending();
        CoordActionMaterializeCommand coordActionMatCom = new CoordActionMaterializeCommand(jobId, startTime,
                endTime);
        Configuration jobConf = null;
        try {
            jobConf = new XConfiguration(new StringReader(coordJob.getConf()));
        }
        catch (IOException e1) {
            LOG.warn("Configuration parse error. read from DB :" + coordJob.getConf(), e1);
        }
        String action = coordActionMatCom.materializeJobs(true, coordJob, jobConf, null);
        String output = coordJob.getJobXml() + System.getProperty("line.separator")
        + "***actions for instance***" + action;
        return output;
    }

    /**
     * Queue MaterializeTransitionXCommand
     */
    protected void queueMaterializeTransitionXCommand(String jobId) {
        // submit a command to materialize jobs for the next 1 hour (3600 secs)
        // so we don't wait 10 mins for the Service to run.
        queue(new CoordMaterializeTransitionXCommand(jobId, 3600), 100);
    }

     /**
      * Method that validates values in the definition for correctness. Placeholder to add more.
      */
@@ -457,18 +479,16 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
         throw new CoordinatorJobException(ErrorCode.E1021, eventType + " end-instance '" + instanceValue
                 + "' contains more than one date end-instance. Coordinator job NOT SUBMITTED. " + correctAction);
     }

     /**
      * Read the application XML and validate against coordinator Schema
      *
      * @return validated coordinator XML
      * @throws CoordinatorJobException thrown if unable to read or validate coordinator xml
      */
    private String readAndValidateXml() throws CoordinatorJobException {
    protected String readAndValidateXml() throws CoordinatorJobException {
         String appPath = ParamChecker.notEmpty(conf.get(OozieClient.COORDINATOR_APP_PATH),
                 OozieClient.COORDINATOR_APP_PATH);
         String coordXml = readDefinition(appPath);

         validateXml(coordXml);
         return coordXml;
     }
@@ -1153,12 +1173,13 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
     /**
      * Write a coordinator job into database
      *
     *@param appXML : Coordinator definition xml
      * @param eJob : XML element of job
      * @param coordJob : Coordinator job bean
      * @return Job id
      * @throws CommandException thrown if unable to save coordinator job to db
      */
    private String storeToDB(Element eJob, CoordinatorJobBean coordJob) throws CommandException {
    protected String storeToDB(String appXML, Element eJob, CoordinatorJobBean coordJob) throws CommandException {
         String jobId = Services.get().get(UUIDService.class).generateId(ApplicationType.COORDINATOR);
         coordJob.setId(jobId);
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordUpdateXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordUpdateXCommand.java
new file mode 100644
index 000000000..d6e47dacf
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordUpdateXCommand.java
@@ -0,0 +1,269 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.command.coord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.util.LogUtils;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XmlUtils;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.jdom.Element;

/**
 * This class provides the functionalities to update coordinator job XML and properties. It uses CoordSubmitXCommand
 * functionality to validate XML and resolve all the variables or properties using job configurations.
 */
public class CoordUpdateXCommand extends CoordSubmitXCommand {

    private final String jobId;
    private boolean showDiff = true;
    private boolean isConfChange = false;

    StringBuffer diff = new StringBuffer();
    CoordinatorJobBean oldCoordJob = null;

    public CoordUpdateXCommand(boolean dryrun, Configuration conf, String jobId) {
        super(dryrun, conf);
        this.jobId = jobId;
        isConfChange = conf.size() == 0 ? false : true;
    }

    public CoordUpdateXCommand(boolean dryrun, Configuration conf, String jobId, boolean showDiff) {
        super(dryrun, conf);
        this.jobId = jobId;
        this.showDiff = showDiff;
        isConfChange = conf.size() == 0 ? false : true;
    }

    @Override
    protected String storeToDB(String xmlElement, Element eJob, CoordinatorJobBean coordJob) throws CommandException {
        check(oldCoordJob, coordJob);
        computeDiff(eJob);
        oldCoordJob.setAppPath(conf.get(OozieClient.COORDINATOR_APP_PATH));
        if (isConfChange) {
            oldCoordJob.setConf(XmlUtils.prettyPrint(conf).toString());
        }
        oldCoordJob.setMatThrottling(coordJob.getMatThrottling());
        oldCoordJob.setOrigJobXml(xmlElement);
        oldCoordJob.setConcurrency(coordJob.getConcurrency());
        oldCoordJob.setExecution(coordJob.getExecution());
        oldCoordJob.setTimeout(coordJob.getTimeout());
        oldCoordJob.setJobXml(XmlUtils.prettyPrint(eJob).toString());


        if (!dryrun) {
            oldCoordJob.setLastModifiedTime(new Date());
            // Should log the changes, this should be useful for debugging.
            LOG.info("Coord update changes : " + diff.toString());
            try {
                CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQuery.UPDATE_COORD_JOB, oldCoordJob);
            }
            catch (JPAExecutorException jpaee) {
                throw new CommandException(jpaee);
            }
        }
        return jobId;
    }

    @Override
    protected void loadState() throws CommandException {
        super.loadState();
        jpaService = Services.get().get(JPAService.class);
        if (jpaService == null) {
            throw new CommandException(ErrorCode.E0610);
        }
        coordJob = new CoordinatorJobBean();
        try {
            oldCoordJob = CoordJobQueryExecutor.getInstance().get(CoordJobQuery.GET_COORD_JOB, jobId);
        }
        catch (JPAExecutorException e) {
            throw new CommandException(e);
        }

        LogUtils.setLogInfo(oldCoordJob, logInfo);
        if (!isConfChange) {
            try {
                conf = new XConfiguration(new StringReader(coordJob.getConf()));
            }
            catch (Exception e) {
                throw new CommandException(ErrorCode.E1023, e.getMessage(), e);
            }
        }
        coordJob.setConf(XmlUtils.prettyPrint(conf).toString());
        setJob(coordJob);
    }

    @Override
    protected void verifyPrecondition() throws CommandException {
        if (coordJob.getStatus() == CoordinatorJob.Status.SUCCEEDED
                || coordJob.getStatus() == CoordinatorJob.Status.DONEWITHERROR) {
            LOG.info("Can't update coord job. Job has finished processing");
            throw new CommandException(ErrorCode.E1023, "Can't update coord job. Job has finished processing");
        }
    }

    /**
     * Gets the difference of job definition and properties.
     *
     * @param eJob the e job
     * @return the diff
     */

    private void computeDiff(Element eJob) {
        try {
            diff.append("**********Job definition changes**********").append(System.getProperty("line.separator"));
            diff.append(getDiffinGitFormat(oldCoordJob.getJobXml(), XmlUtils.prettyPrint(eJob).toString()));
            diff.append("******************************************").append(System.getProperty("line.separator"));
            diff.append("**********Job conf changes****************").append(System.getProperty("line.separator"));
            if (isConfChange) {
                diff.append(getDiffinGitFormat(oldCoordJob.getConf(), XmlUtils.prettyPrint(conf).toString()));
            }
            else {
                diff.append("No conf update requested").append(System.getProperty("line.separator"));
            }
            diff.append("******************************************").append(System.getProperty("line.separator"));
        }
        catch (IOException e) {
            diff.append("Error computing diff. Error " + e.getMessage());
            LOG.warn("Error computing diff.", e);
        }
    }

    /**
     * Get the differences in git format.
     *
     * @param string1 the string1
     * @param string2 the string2
     * @return the diff
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String getDiffinGitFormat(String string1, String string2) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RawText rt1 = new RawText(string1.getBytes());
        RawText rt2 = new RawText(string2.getBytes());
        EditList diffList = new EditList();
        diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rt1, rt2));
        new DiffFormatter(out).format(diffList, rt1, rt2);
        return out.toString();
    }

    @Override
    protected String submit() throws CommandException {
        LOG.info("STARTED Coordinator update");
        submitJob();
        LOG.info("ENDED Coordinator update");
        if (showDiff) {
            return diff.toString();
        }
        else {
            return "";
        }
    }

    /**
     * Check. Frequency can't be changed. EndTime can't be changed. StartTime can't be changed. AppName can't be changed
     * Timeunit can't be changed. Timezone can't be changed
     *
     * @param oldCoord the old coord
     * @param newCoord the new coord
     * @throws CommandException the command exception
     */
    public void check(CoordinatorJobBean oldCoord, CoordinatorJobBean newCoord) throws CommandException {
        if (!oldCoord.getFrequency().equals(newCoord.getFrequency())) {
            throw new CommandException(ErrorCode.E1023, "Frequency can't be changed. Old frequency = "
                    + oldCoord.getFrequency() + " new frequency = " + newCoord.getFrequency());
        }

        if (!oldCoord.getEndTime().equals(newCoord.getEndTime())) {
            throw new CommandException(ErrorCode.E1023, "End time can't be changed. Old end time = "
                    + oldCoord.getEndTime() + " new end time = " + newCoord.getEndTime());
        }

        if (!oldCoord.getStartTime().equals(newCoord.getStartTime())) {
            throw new CommandException(ErrorCode.E1023, "Start time can't be changed. Old start time = "
                    + oldCoord.getStartTime() + " new start time = " + newCoord.getStartTime());
        }

        if (!oldCoord.getAppName().equals(newCoord.getAppName())) {
            throw new CommandException(ErrorCode.E1023, "Coord name can't be changed. Old name = "
                    + oldCoord.getAppName() + " new name = " + newCoord.getAppName());
        }

        if (!oldCoord.getTimeUnitStr().equals(newCoord.getTimeUnitStr())) {
            throw new CommandException(ErrorCode.E1023, "Timeunit can't be changed. Old Timeunit = "
                    + oldCoord.getTimeUnitStr() + " new Timeunit = " + newCoord.getTimeUnitStr());
        }

        if (!oldCoord.getTimeZone().equals(newCoord.getTimeZone())) {
            throw new CommandException(ErrorCode.E1023, "TimeZone can't be changed. Old timeZone = "
                    + oldCoord.getTimeZone() + " new timeZone = " + newCoord.getTimeZone());
        }

    }

    @Override
    protected void queueMaterializeTransitionXCommand(String jobId) {
    }

    @Override
    public void notifyParent() throws CommandException {
    }

    @Override
    protected boolean isLockRequired() {
        return true;
    }

    @Override
    public String getEntityKey() {
        return jobId;
    }

    @Override
    public void transitToNext() {
    }

    @Override
    public String getKey() {
        return getName() + "_" + jobId;
    }

    @Override
    public String getDryRun(CoordinatorJobBean job) throws Exception{
        return super.getDryRun(oldCoordJob);
    }
}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
index 5f7744c5f..67a919d5b 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
@@ -112,6 +112,7 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
                 query.setParameter("timeUnit", cjBean.getTimeUnitStr());
                 query.setParameter("appNamespace", cjBean.getAppNamespace());
                 query.setParameter("bundleId", cjBean.getBundleId());
                query.setParameter("matThrottling", cjBean.getMatThrottling());
                 query.setParameter("id", cjBean.getId());
                 break;
             case UPDATE_COORD_JOB_STATUS:
diff --git a/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java b/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
index aa43e6892..6b82d7b97 100644
-- a/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
@@ -147,6 +147,13 @@ public abstract class BaseJobServlet extends JsonRestServlet {
                 response.setStatus(HttpServletResponse.SC_OK);
             }
         }
        else if (action.equals(RestConstants.JOB_COORD_UPDATE)) {
            validateContentType(request, RestConstants.XML_CONTENT_TYPE);
            stopCron();
            JSONObject json = updateJob(request, response);
            startCron();
            sendJsonResponse(response, HttpServletResponse.SC_OK, json);
        }
         else {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0303,
                     RestConstants.ACTION_PARAM, action);
@@ -416,4 +423,17 @@ public abstract class BaseJobServlet extends JsonRestServlet {
      */
     abstract JSONObject getJobsByParentId(HttpServletRequest request, HttpServletResponse response)
             throws XServletException, IOException;

    /**
     * Abstract method to Update coord job.
     *
     * @param request the request
     * @param response the response
     * @return the JSON object
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract JSONObject updateJob(HttpServletRequest request, HttpServletResponse response)
            throws XServletException, IOException;
 }

diff --git a/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
index 443ab6d4c..487a37128 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
@@ -214,4 +214,10 @@ public class V0JobServlet extends BaseJobServlet {
             IOException {
         throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
     }

    @Override
    protected JSONObject updateJob(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
    }
 }
\ No newline at end of file
diff --git a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
index ac399e923..5b65791a7 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
@@ -1071,5 +1071,12 @@ public class V1JobServlet extends BaseJobServlet {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
         }
     }

    /**
     * not supported for v1
     */
    @Override
    protected JSONObject updateJob(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
index 2d26599a2..e961f3054 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
@@ -22,11 +22,19 @@ import java.io.IOException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorEngine;
import org.apache.oozie.CoordinatorEngineException;
 import org.apache.oozie.DagEngine;
 import org.apache.oozie.DagEngineException;
 import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.JsonTags;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.service.CoordinatorEngineService;
 import org.apache.oozie.service.DagEngineService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.util.XConfiguration;
 import org.json.simple.JSONObject;
 
 @SuppressWarnings("serial")
@@ -75,4 +83,40 @@ public class V2JobServlet extends V1JobServlet {
             throws XServletException, IOException {
         return super.getJobsByParentId(request, response);
     }

    /**
     * Update coord job.
     *
     * @param request the request
     * @param response the response
     * @return the JSON object
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected JSONObject updateJob(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        CoordinatorEngine coordEngine = Services.get().get(CoordinatorEngineService.class)
                .getCoordinatorEngine(getUser(request));
        JSONObject json = new JSONObject();
        try {
            Configuration conf= new XConfiguration(request.getInputStream());
            String jobId = getResourceName(request);
            boolean dryrun = StringUtils.isEmpty(request.getParameter(RestConstants.JOB_ACTION_DRYRUN)) ? false
                    : Boolean.parseBoolean(request.getParameter(RestConstants.JOB_ACTION_DRYRUN));
            boolean showDiff = StringUtils.isEmpty(request.getParameter(RestConstants.JOB_ACTION_SHOWDIFF)) ? true
                    : Boolean.parseBoolean(request.getParameter(RestConstants.JOB_ACTION_SHOWDIFF));

            String diff = coordEngine.updateJob(conf, jobId, dryrun, showDiff);
            JSONObject diffJson = new JSONObject();
            diffJson.put(JsonTags.COORD_UPDATE_DIFF, diff);
            json.put(JsonTags.COORD_UPDATE, diffJson);
        }
        catch (CoordinatorEngineException e) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, e);
        }
        return json;
    }

 }
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordUpdateXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordUpdateXCommand.java
new file mode 100644
index 000000000..99572dec0
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordUpdateXCommand.java
@@ -0,0 +1,297 @@
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.command.coord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.XException;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.Namespace;

public class TestCoordUpdateXCommand extends XDataTestCase {
    private Services services;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
        LocalOozie.start();
    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
        LocalOozie.stop();
    }

    private String setupCoord(Configuration conf, String coordFile) throws CommandException, IOException {
        File appPathFile = new File(getTestCaseDir(), "coordinator.xml");
        Reader reader = IOUtils.getResourceAsReader(coordFile, -1);
        Writer writer = new FileWriter(appPathFile);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set(OozieClient.USER_NAME, getTestUser());
        CoordSubmitXCommand sc = new CoordSubmitXCommand(conf);
        IOUtils.copyCharStream(reader, writer);
        sc = new CoordSubmitXCommand(conf);
        return sc.call();

    }

    // test conf change
    public void testConfChange() throws Exception {
        Configuration conf = new XConfiguration();
        String jobId = setupCoord(conf, "coord-multiple-input-instance3.xml");
        String addedProperty = "jobrerun";
        XConfiguration xConf = new XConfiguration();
        assertNull(xConf.get(addedProperty));
        conf.set(addedProperty, "true");
        CoordinatorJobBean job = getCoordJobs(jobId);
        xConf = new XConfiguration(new StringReader(job.getConf()));
        CoordUpdateXCommand update = new CoordUpdateXCommand(false, conf, jobId);
        String diff = update.call();
        job = getCoordJobs(jobId);
        xConf = new XConfiguration(new StringReader(job.getConf()));
        assertEquals(xConf.get(addedProperty), "true");
        assertTrue(diff.contains("+    <name>jobrerun</name>"));
        assertTrue(diff.contains("+    <value>true</value>"));
    }

    // test definition change
    public void testDefinitionChange() throws Exception {
        Configuration conf = new XConfiguration();
        File appPathFile1 = new File(getTestCaseDir(), "coordinator.xml");
        String jobId = setupCoord(conf, "coord-multiple-input-instance3.xml");
        CoordinatorJobBean job = getCoordJobs(jobId);
        Element processedJobXml = XmlUtils.parseXml(job.getJobXml());
        Namespace namespace = processedJobXml.getNamespace();
        String text = ((Element) processedJobXml.getChild("input-events", namespace).getChild("data-in", namespace)
                .getChildren("instance", namespace).get(0)).getText();
        assertEquals(text, "${coord:latest(0)}");
        Reader reader = IOUtils.getResourceAsReader("coord-multiple-input-instance4.xml", -1);
        Writer writer = new FileWriter(appPathFile1);
        IOUtils.copyCharStream(reader, writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile1.toURI().toString());
        job = getCoordJobs(jobId);
        CoordUpdateXCommand update = new CoordUpdateXCommand(false, conf, jobId);
        update.call();
        job = getCoordJobs(jobId);
        processedJobXml = XmlUtils.parseXml(job.getJobXml());
        namespace = processedJobXml.getNamespace();
        text = ((Element) processedJobXml.getChild("input-events", namespace).getChild("data-in", namespace)
                .getChildren("instance", namespace).get(0)).getText();
        assertEquals(text, "${coord:future(0, 1)}");
    }

    // test fail... error in coord definition
    public void testCoordDefinitionChangeError() throws Exception {
        Configuration conf = new XConfiguration();
        File appPathFile1 = new File(getTestCaseDir(), "coordinator.xml");
        String jobId = setupCoord(conf, "coord-multiple-input-instance3.xml");

        CoordinatorJobBean job = getCoordJobs(jobId);
        Element processedJobXml = XmlUtils.parseXml(job.getJobXml());
        Namespace namespace = processedJobXml.getNamespace();
        String text = ((Element) processedJobXml.getChild("input-events", namespace).getChild("data-in", namespace)
                .getChildren("instance", namespace).get(0)).getText();
        assertEquals(text, "${coord:latest(0)}");
        Reader reader = IOUtils.getResourceAsReader("coord-multiple-input-instance1.xml", -1);
        Writer writer = new FileWriter(appPathFile1);
        IOUtils.copyCharStream(reader, writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile1.toURI().toString());
        job = getCoordJobs(jobId);
        CoordUpdateXCommand update = new CoordUpdateXCommand(false, conf, jobId);
        try {
            update.call();
            fail(" should not come here");
        }
        catch (Exception e) {
            assertTrue(e.getMessage().contains("E1021: Coord Action Input Check Error"));
        }
    }

    // test fail... trying to set unsupported field.
    public void testCoordDefUnsupportedChange() throws Exception {
        final XConfiguration conf = new XConfiguration();
        conf.set("start", "2009-02-01T01:00Z");
        conf.set("end", "2012-02-03T23:59Z");
        conf.set("unit", "UTC");
        conf.set("name", "NAME");
        conf.set("throttle", "12");
        conf.set("concurrency", "12");
        conf.set("execution", "FIFO");
        conf.set("timeout", "10");
        String jobId = setupCoord(conf, "coord-update-test.xml");

        Configuration newConf = new XConfiguration(conf.toProperties());
        newConf.set("start", "2010-02-01T01:00Z");

        try {
            new CoordUpdateXCommand(false, newConf, jobId).call();
            fail(" should not come here");
        }
        catch (XException e) {
            assertTrue(e.getMessage().contains("Start time can't be changed"));
        }

        newConf = new XConfiguration(conf.toProperties());
        newConf.set("end", "2015-02-03T23:59Z");
        try {
            new CoordUpdateXCommand(false, newConf, jobId).call();
            fail(" should not come here");
        }
        catch (XException e) {
            assertTrue(e.getMessage().contains("End time can't be changed"));
        }
        newConf = new XConfiguration(conf.toProperties());
        newConf.set("name", "test");
        try {
            new CoordUpdateXCommand(false, newConf, jobId).call();
            fail(" should not come here");
        }
        catch (XException e) {
            assertTrue(e.getMessage().contains("Coord name can't be changed"));
        }

        newConf = new XConfiguration(conf.toProperties());
        newConf.set("unit", "America/New_York");
        try {
            new CoordUpdateXCommand(false, newConf, jobId).call();
            fail(" should not come here");
        }
        catch (XException e) {
            assertTrue(e.getMessage().contains("TimeZone can't be changed"));
        }
    }

    // Test update control param.
    public void testUpdateControl() throws Exception {
        final XConfiguration conf = new XConfiguration();
        conf.set("start", "2009-02-01T01:00Z");
        conf.set("end", "2012-02-03T23:59Z");
        conf.set("unit", "UTC");
        conf.set("name", "NAME");
        conf.set("throttle", "12");
        conf.set("concurrency", "12");
        conf.set("execution", "FIFO");
        conf.set("timeout", "7");
        String jobId = setupCoord(conf, "coord-update-test.xml");

        CoordinatorJobBean job = getCoordJobs(jobId);
        assertEquals(12, job.getMatThrottling());
        assertEquals(12, job.getConcurrency());
        assertEquals(7, job.getTimeout());
        assertEquals("FIFO", job.getExecution());

        Configuration newConf = new XConfiguration(conf.toProperties());
        newConf.set("throttle", "8");
        new CoordUpdateXCommand(false, newConf, jobId).call();
        job = getCoordJobs(jobId);
        assertEquals(8, job.getMatThrottling());

        newConf = new XConfiguration(conf.toProperties());
        newConf.set("concurrency", "5");
        new CoordUpdateXCommand(false, newConf, jobId).call();
        job = getCoordJobs(jobId);
        assertEquals(5, job.getConcurrency());

        newConf = new XConfiguration(conf.toProperties());
        newConf.set("timeout", "10");
        new CoordUpdateXCommand(false, newConf, jobId).call();
        job = getCoordJobs(jobId);
        assertEquals(10, job.getTimeout());

        newConf = new XConfiguration(conf.toProperties());
        newConf.set("execution", "LIFO");
        new CoordUpdateXCommand(false, newConf, jobId).call();
        job = getCoordJobs(jobId);
        assertEquals("LIFO", job.getExecution());

    }

    // test coord re-run with refresh. will use the updated coord definition.
    public void testReRunRefresh() throws Exception {
        Configuration conf = new XConfiguration();
        File appPathFile1 = new File(getTestCaseDir(), "coordinator.xml");
        String jobId = setupCoord(conf, "coord-multiple-input-instance3.xml");
        sleep(1000);
        final int actionNum = 1;
        final String actionId = jobId + "@" + actionNum;
        final OozieClient coordClient = LocalOozie.getCoordClient();
        waitFor(120 * 1000, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                CoordinatorAction bean = coordClient.getCoordActionInfo(actionId);
                return (bean.getStatus() == CoordinatorAction.Status.WAITING || bean.getStatus() == CoordinatorAction.Status.SUBMITTED);
            }
        });
        CoordinatorAction bean = coordClient.getCoordActionInfo(actionId);
        assertEquals(bean.getMissingDependencies(), "!!${coord:latest(0)}#${coord:latest(-1)}");
        CoordinatorJobBean job = getCoordJobs(jobId);
        Reader reader = IOUtils.getResourceAsReader("coord-multiple-input-instance4.xml", -1);
        Writer writer = new FileWriter(appPathFile1);
        IOUtils.copyCharStream(reader, writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile1.toURI().toString());
        new CoordUpdateXCommand(false, conf, jobId).call();
        job = getCoordJobs(jobId);
        Element processedJobXml = XmlUtils.parseXml(job.getJobXml());
        Namespace namespace = processedJobXml.getNamespace();
        String text = ((Element) processedJobXml.getChild("input-events", namespace).getChild("data-in", namespace)
                .getChildren("instance", namespace).get(0)).getText();
        assertEquals(text, "${coord:future(0, 1)}");
        new CoordActionsKillXCommand(jobId, RestConstants.JOB_COORD_SCOPE_ACTION, Integer.toString(actionNum)).call();
        coordClient.reRunCoord(jobId, RestConstants.JOB_COORD_SCOPE_ACTION, Integer.toString(actionNum), true, true);
        bean = coordClient.getCoordActionInfo(actionId);
        sleep(1000);
        assertEquals(bean.getMissingDependencies(), "!!${coord:future(0, 1)}");
    }

    private CoordinatorJobBean getCoordJobs(String jobId) {
        try {
            JPAService jpaService = Services.get().get(JPAService.class);
            CoordinatorJobBean job = jpaService.execute(new CoordJobGetJPAExecutor(jobId));
            return job;
        }
        catch (JPAExecutorException e) {
            fail("Job ID " + jobId + " was not stored properly in db");
        }
        return null;
    }
}
\ No newline at end of file
diff --git a/core/src/test/resources/coord-update-test.xml b/core/src/test/resources/coord-update-test.xml
new file mode 100644
index 000000000..28ec1f73f
-- /dev/null
++ b/core/src/test/resources/coord-update-test.xml
@@ -0,0 +1,59 @@
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<coordinator-app xmlns="uri:oozie:coordinator:0.4" name="${name}" frequency="${coord:days(1)}" start="${start}" end="${end}" timezone="${unit}">
  <controls>
    <timeout>${timeout}</timeout>
    <concurrency>${concurrency}</concurrency>
    <execution>${execution}</execution>
    <throttle>${throttle}</throttle>
  </controls>
  <datasets>
    <dataset name="a" frequency="${coord:days(7)}" initial-instance="2009-02-01T01:00Z" timezone="UTC">
        <uri-template>file:///tmp/coord/workflows/${YEAR}/${DAY}</uri-template>
    </dataset>
    <dataset name="local_a" frequency="${coord:days(7)}" initial-instance="2009-02-01T01:00Z" timezone="UTC">
        <uri-template>file:///tmp/coord/workflows/${YEAR}/${DAY}</uri-template>
    </dataset>
  </datasets>
  <input-events>
    <data-in name="A" dataset="a">
    <instance>${coord:latest(0)}</instance>
    <instance>${coord:latest(-1)}</instance>
    </data-in>
  </input-events>
  <output-events>
    <data-out name="LOCAL_A" dataset="local_a">
      <instance>${coord:current(-1)}</instance>
    </data-out>
  </output-events>
  <action>
    <workflow>
      <app-path>hdfs:///tmp/workflows/</app-path>
      <configuration>
        <property>
          <name>inputA</name>
          <value>${coord:dataIn('A')}</value>
        </property>
        <property>
          <name>inputB</name>
          <value>${coord:dataOut('LOCAL_A')}</value>
        </property>
      </configuration>
    </workflow>
  </action>
</coordinator-app>
\ No newline at end of file
diff --git a/docs/src/site/twiki/DG_CommandLineTool.twiki b/docs/src/site/twiki/DG_CommandLineTool.twiki
index 5819b7120..222389e79 100644
-- a/docs/src/site/twiki/DG_CommandLineTool.twiki
++ b/docs/src/site/twiki/DG_CommandLineTool.twiki
@@ -66,6 +66,7 @@ usage:
                 -value <arg>          new endtime/concurrency/pausetime value for changing a
                                       coordinator job; new pausetime value for changing a bundle job
                 -verbose              verbose mode
                -update               Update coordinator definition and properties
 .
       oozie jobs <OPTIONS> : jobs status
                  -auth <arg>          select authentication type [SIMPLE|KERBEROS]
@@ -634,6 +635,59 @@ specified path must be an HDFS path.
 If the workflow is accepted (i.e. Oozie is able to successfully read and parse it), it will return ="OK"=; otherwise, it will return
 an error message describing why it was rejected.
 
---+++ Updating coordinator definition and properties
Existing coordinator definition will be replaced by new definition. The refreshed coordinator would keep the same coordinator ID, state, and coordinator actions.
All created coord action(including in WAITING) will use old configuration.
One can rerun actions with -refresh option, -refresh option will use new configuration to rerun coord action

Update command also verifies coordinator definition like submit command, if there is any issue with definition, update will fail.
Update command with -dryrun will show coordinator definition and properties differences.
Config option is optional, if not specified existing coordinator property is used to find coordinator path.

Update command doesn't allow update of coordinator name, frequency, start time, end time and timezone and will fail on an attempt to change any of them. To change end time of coordinator use the =-change= command

<verbatim>
$ oozie job -oozie http://localhost:11000/oozie -config job.properties -update 0000005-140402104721140-oozie-puru-C -dryrun

**********Job definition changes**********
@@ -3,8 +3,8 @@
     <concurrency>1</concurrency>
   </controls>
   <input-events>
-    <data-in name="input" dataset="raw-logs">
-      <dataset name="raw-logs" frequency="20" initial-instance="2010-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
+    <data-in name="input" dataset="raw-logs-rename">
+      <dataset name="raw-logs-rename" frequency="20" initial-instance="2010-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
         <uri-template>hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/</uri-template>
         <done-flag />
       </dataset>
**********************************
**********Job conf changes**********
@@ -8,10 +8,6 @@
     <value>hdfs://localhost:9000/user/purushah/examples/apps/aggregator/coordinator.xml</value>
   </property>
   <property>
-    <name>old</name>
-    <value>test</value>
-  </property>
-  <property>
     <name>user.name</name>
     <value>purushah</value>
   </property>
@@ -28,6 +24,10 @@
     <value>hdfs://localhost:9000</value>
   </property>
   <property>
+    <name>adding</name>
+    <value>new</value>
+  </property>
+  <property>
     <name>jobTracker</name>
     <value>localhost:9001</value>
   </property>
**********************************
</verbatim>

 ---++ Jobs Operations
 
 ---+++ Checking the Status of multiple Workflow Jobs
diff --git a/docs/src/site/twiki/WebServicesAPI.twiki b/docs/src/site/twiki/WebServicesAPI.twiki
index 351699de6..c93fc7fd5 100644
-- a/docs/src/site/twiki/WebServicesAPI.twiki
++ b/docs/src/site/twiki/WebServicesAPI.twiki
@@ -876,7 +876,7 @@ Content-Type: application/json;charset=UTF-8
 
 ---++++ Managing a Job
 
A HTTP PUT request starts, suspends, resumes, kills, or dryruns a job.
A HTTP PUT request starts, suspends, resumes, kills, update or dryruns a job.
 
 *Request:*
 
@@ -1050,6 +1050,36 @@ PUT /oozie/v1/job/job-3?action=change&value=endtime=2011-12-01T05:00Z;concurrenc
 HTTP/1.1 200 OK
 </verbatim>
 
---+++++ Updating coordinator definition and properties
Existing coordinator definition and properties will be replaced by new definition and properties. Refer [[DG_CommandLineTool#Updating_coordinator_definition_and_properties][Updating coordinator definition and properties]]

<verbatim>
PUT oozie/v2/job/0000000-140414102048137-oozie-puru-C?action=update
</verbatim>

*Response:*

<verbatim>
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
{"update":
     {"diff":"**********Job definition changes**********\n******************************************\n**********Job conf changes****************\n@@ -8,16 +8,12 @@\n
          <value>hdfs:\/\/localhost:9000\/user\/purushah\/examples\/apps\/aggregator\/coordinator.xml<\/value>\r\n   <\/property>\r\n   <property>\r\n
          -    <name>user.name<\/name>\r\n
          -    <value>purushah<\/value>\r\n
          -    <\/property>\r\n
          -  <property>\r\n     <name>start<\/name>\r\n
               <value>2010-01-01T01:00Z<\/value>\r\n   <\/property>\r\n   <property>\r\n
          -    <name>newproperty<\/name>\r\n
          -    <value>new<\/value>\r\n
          +    <name>user.name<\/name>\r\n
          +    <value>purushah<\/value>\r\n   <\/property>\r\n   <property>\r\n
               <name>queueName<\/name>\r\n******************************************\n"
      }
}
</verbatim>


 ---++++ Job Information
 
 A HTTP GET request retrieves the job information.
diff --git a/pom.xml b/pom.xml
index cb10007b0..65d7b895d 100644
-- a/pom.xml
++ b/pom.xml
@@ -780,6 +780,12 @@
                 <version>4.01</version>
             </dependency>
 
            <dependency>
                <groupId>org.eclipse.jgit</groupId>
                <artifactId>org.eclipse.jgit</artifactId>
                <version>3.3.1.201403241930-r</version>
            </dependency>

             <dependency>
                 <groupId>org.apache.oozie</groupId>
                 <artifactId>oozie-hadoop-utils</artifactId>
diff --git a/release-log.txt b/release-log.txt
index 07a4c9959..1a115594a 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.1.0 release (trunk - unreleased)
 
OOZIE-1769 An option to update coord properties/definition (puru via rohini)
 OOZIE-1796 Job status should not transition from KILLED (puru via rohini)
 OOZIE-1781 UI - Last Modified time is not displayed for coord action in coord job info grid (puru via mona)
 OOZIE-1792 Ability to kill bundle stuck in RUNNING due to inconsistent pending states (rohini)
- 
2.19.1.windows.1

