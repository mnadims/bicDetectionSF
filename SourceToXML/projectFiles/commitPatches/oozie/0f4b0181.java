From 0f4b0181bc8bdac4696bce2bde854b332bb02d80 Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Fri, 20 Feb 2015 15:01:19 -0800
Subject: [PATCH] OOZIE-1913 Devise a way to turn off SLA alerts for
 bundle/coordinator flexibly

--
 .../java/org/apache/oozie/cli/OozieCLI.java   |  69 +++-
 .../org/apache/oozie/client/OozieClient.java  | 185 +++++++++--
 .../apache/oozie/client/event/SLAEvent.java   |   2 +-
 .../apache/oozie/client/rest/JsonTags.java    |   2 +
 .../oozie/client/rest/RestConstants.java      |  20 ++
 .../java/org/apache/oozie/BaseEngine.java     |  40 +++
 .../java/org/apache/oozie/BundleEngine.java   |  42 ++-
 .../apache/oozie/CoordinatorActionBean.java   |  49 +--
 .../org/apache/oozie/CoordinatorEngine.java   |  47 +++
 .../org/apache/oozie/CoordinatorJobBean.java  |  56 ++--
 .../main/java/org/apache/oozie/DagEngine.java |  16 +
 .../main/java/org/apache/oozie/ErrorCode.java |   2 +
 .../oozie/command/SLAAlertsXCommand.java      | 117 +++++++
 .../BundleSLAAlertsDisableXCommand.java       |  44 +++
 .../bundle/BundleSLAAlertsEnableXCommand.java |  45 +++
 .../bundle/BundleSLAAlertsXCommand.java       | 149 +++++++++
 .../bundle/BundleSLAChangeXCommand.java       |  57 ++++
 .../bundle/BundleStatusTransitXCommand.java   |   1 +
 .../CoordMaterializeTransitionXCommand.java   |  19 +-
 .../coord/CoordSLAAlertsDisableXCommand.java  |  71 ++++
 .../coord/CoordSLAAlertsEnableXCommand.java   |  65 ++++
 .../command/coord/CoordSLAAlertsXCommand.java | 233 ++++++++++++++
 .../command/coord/CoordSLAChangeXCommand.java | 100 ++++++
 .../org/apache/oozie/coord/CoordUtils.java    | 146 ++++++++-
 .../jpa/CoordActionQueryExecutor.java         |  48 ++-
 ...obGetActionIdsForDateRangeJPAExecutor.java |  69 ----
 ...obGetActionsByDatesForKillJPAExecutor.java | 108 -------
 ...CoordJobGetActionsForDatesJPAExecutor.java |  70 ----
 .../executor/jpa/CoordJobQueryExecutor.java   |  51 ++-
 .../CoordJobsToBeMaterializedJPAExecutor.java |   2 +-
 .../jpa/SLARegistrationQueryExecutor.java     |  62 +++-
 .../executor/jpa/SLASummaryQueryExecutor.java |  29 +-
 .../CoordMaterializeTriggerService.java       |   2 +-
 .../oozie/service/EventHandlerService.java    |  24 +-
 .../apache/oozie/servlet/BaseJobServlet.java  |  55 ++++
 .../org/apache/oozie/servlet/SLAServlet.java  |   1 +
 .../apache/oozie/servlet/V0JobServlet.java    |  18 +-
 .../apache/oozie/servlet/V1JobServlet.java    |  16 +
 .../apache/oozie/servlet/V2JobServlet.java    |  74 ++++-
 .../apache/oozie/servlet/V2SLAServlet.java    |  21 +-
 .../org/apache/oozie/sla/SLACalcStatus.java   |  12 +-
 .../org/apache/oozie/sla/SLACalculator.java   |  54 ++++
 .../apache/oozie/sla/SLACalculatorMemory.java | 302 +++++++++++++++---
 .../org/apache/oozie/sla/SLAOperations.java   | 143 ++++++---
 .../apache/oozie/sla/SLARegistrationBean.java |  28 +-
 .../org/apache/oozie/sla/SLASummaryBean.java  |  33 +-
 .../apache/oozie/sla/service/SLAService.java  |  94 +++++-
 .../oozie/util/CoordActionsInDateRange.java   |  23 +-
 core/src/main/resources/oozie-default.xml     |   9 +
 .../oozie/command/TestSLAAlertXCommand.java   | 300 +++++++++++++++++
 .../coord/TestCoordSubmitXCommand.java        | 178 +++++++++++
 ...java => TestCoordActionQueryExecutor.java} |  52 ++-
 ...ordJobGetActionIdsForDatesJPAExecutor.java |  82 -----
 .../oozie/service/TestHASLAService.java       |  71 ++++
 .../oozie/servlet/TestV2SLAServlet.java       |   2 -
 .../oozie/sla/TestSLACalculatorMemory.java    | 125 ++++++--
 .../oozie/sla/TestSLAEventGeneration.java     |   4 +
 .../TestSLARegistrationGetJPAExecutor.java    |  20 +-
 core/src/test/resources/coord-action-sla.xml  |   2 +-
 docs/src/site/twiki/DG_CommandLineTool.twiki  |  22 +-
 docs/src/site/twiki/DG_SLAMonitoring.twiki    |  46 +++
 docs/src/site/twiki/WebServicesAPI.twiki      |  42 +++
 release-log.txt                               |   1 +
 .../webapp/console/sla/js/oozie-sla-table.js  |   1 +
 .../main/webapp/console/sla/oozie-sla.html    |   1 +
 65 files changed, 3256 insertions(+), 618 deletions(-)
 create mode 100644 core/src/main/java/org/apache/oozie/command/SLAAlertsXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsDisableXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsEnableXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/bundle/BundleSLAChangeXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsDisableXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsEnableXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsXCommand.java
 create mode 100644 core/src/main/java/org/apache/oozie/command/coord/CoordSLAChangeXCommand.java
 delete mode 100644 core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionIdsForDateRangeJPAExecutor.java
 delete mode 100644 core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsByDatesForKillJPAExecutor.java
 delete mode 100644 core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsForDatesJPAExecutor.java
 create mode 100644 core/src/test/java/org/apache/oozie/command/TestSLAAlertXCommand.java
 rename core/src/test/java/org/apache/oozie/executor/jpa/{TestCoordJobGetActionsForDatesJPAExecutor.java => TestCoordActionQueryExecutor.java} (52%)
 delete mode 100644 core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionIdsForDatesJPAExecutor.java

diff --git a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
index 66908696f..218edf219 100644
-- a/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
++ b/client/src/main/java/org/apache/oozie/cli/OozieCLI.java
@@ -135,17 +135,21 @@ public class OozieCLI {
     public static final String LOCAL_TIME_OPTION = "localtime";
     public static final String TIME_ZONE_OPTION = "timezone";
     public static final String QUEUE_DUMP_OPTION = "queuedump";
    public static final String RERUN_COORD_OPTION = "coordinator";
     public static final String DATE_OPTION = "date";
     public static final String RERUN_REFRESH_OPTION = "refresh";
     public static final String RERUN_NOCLEANUP_OPTION = "nocleanup";
     public static final String RERUN_FAILED_OPTION = "failed";
     public static final String ORDER_OPTION = "order";
    public static final String COORD_OPTION = "coordinator";
 
     public static final String UPDATE_SHARELIB_OPTION = "sharelibupdate";
 
     public static final String LIST_SHARELIB_LIB_OPTION = "shareliblist";
 
    public static final String SLA_DISABLE_ALERT = "sla_disable";
    public static final String SLA_ENABLE_ALERT = "sla_enable";
    public static final String SLA_CHANGE = "sla_change";

 
 
     public static final String AUTH_OPTION = "auth";
@@ -328,7 +332,7 @@ public class OozieCLI {
                         + "(requires -log)");
         Option date = new Option(DATE_OPTION, true,
                 "coordinator/bundle rerun on action dates (requires -rerun); coordinator log retrieval on action dates (requires -log)");
        Option rerun_coord = new Option(RERUN_COORD_OPTION, true, "bundle rerun on coordinator names (requires -rerun)");
        Option rerun_coord = new Option(COORD_OPTION, true, "bundle rerun on coordinator names (requires -rerun)");
         Option rerun_refresh = new Option(RERUN_REFRESH_OPTION, false,
                 "re-materialize the coordinator rerun actions (requires -rerun)");
         Option rerun_nocleanup = new Option(RERUN_NOCLEANUP_OPTION, false,
@@ -348,6 +352,14 @@ public class OozieCLI {
         Option interval = new Option(INTERVAL_OPTION, true, "polling interval in minutes (default is 5, requires -poll)");
         interval.setType(Integer.class);
 
        Option slaDisableAlert = new Option(SLA_DISABLE_ALERT, true,
                "disables sla alerts for the job and its children");
        Option slaEnableAlert = new Option(SLA_ENABLE_ALERT, true,
                "enables sla alerts for the job and its children");
        Option slaChange = new Option(SLA_CHANGE, true,
                "Update sla param for jobs, supported param are should-start, should-end, nominal-time and max-duration");


         Option doAs = new Option(DO_AS_OPTION, true, "doAs user, impersonates as the specified user");
 
         OptionGroup actions = new OptionGroup();
@@ -368,6 +380,10 @@ public class OozieCLI {
         actions.addOption(config_content);
         actions.addOption(ignore);
         actions.addOption(poll);
        actions.addOption(slaDisableAlert);
        actions.addOption(slaEnableAlert);
        actions.addOption(slaChange);

         actions.setRequired(true);
         Options jobOptions = new Options();
         jobOptions.addOption(oozie);
@@ -401,6 +417,7 @@ public class OozieCLI {
         OptionGroup updateOption = new OptionGroup();
         updateOption.addOption(dryrun);
         jobOptions.addOptionGroup(updateOption);

         return jobOptions;
     }
 
@@ -1014,8 +1031,8 @@ public class OozieCLI {
                         dateScope = commandLine.getOptionValue(DATE_OPTION);
                     }
 
                    if (options.contains(RERUN_COORD_OPTION)) {
                        coordScope = commandLine.getOptionValue(RERUN_COORD_OPTION);
                    if (options.contains(COORD_OPTION)) {
                        coordScope = commandLine.getOptionValue(COORD_OPTION);
                     }
 
                     if (options.contains(RERUN_REFRESH_OPTION)) {
@@ -1234,6 +1251,15 @@ public class OozieCLI {
                 boolean verbose = commandLine.hasOption(VERBOSE_OPTION);
                 wc.pollJob(jobId, timeout, interval, verbose);
             }
            else if (options.contains(SLA_ENABLE_ALERT)) {
                slaAlertCommand(commandLine.getOptionValue(SLA_ENABLE_ALERT), wc, commandLine, options);
            }
            else if (options.contains(SLA_DISABLE_ALERT)) {
                slaAlertCommand(commandLine.getOptionValue(SLA_DISABLE_ALERT), wc, commandLine, options);
            }
            else if (options.contains(SLA_CHANGE)) {
                slaAlertCommand(commandLine.getOptionValue(SLA_CHANGE), wc, commandLine, options);
            }
         }
         catch (OozieClientException ex) {
             throw new OozieCLIException(ex.toString(), ex);
@@ -1902,8 +1928,8 @@ public class OozieCLI {
                         "ssh-action-0.2.xsd")));
                 sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                         "hive2-action-0.1.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "spark-action-0.1.xsd")));
                sources.add(new StreamSource(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("spark-action-0.1.xsd")));
                 SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                 Schema schema = factory.newSchema(sources.toArray(new StreamSource[sources.size()]));
                 Validator validator = schema.newValidator();
@@ -2059,4 +2085,35 @@ public class OozieCLI {
         return allDeps.toString();
     }
 
    private void slaAlertCommand(String jobIds, OozieClient wc, CommandLine commandLine, List<String> options)
            throws OozieCLIException, OozieClientException {
        String actions = null, coordinators = null, dates = null;

        if (options.contains(ACTION_OPTION)) {
            actions = commandLine.getOptionValue(ACTION_OPTION);
        }

        if (options.contains(DATE_OPTION)) {
            dates = commandLine.getOptionValue(DATE_OPTION);
        }

        if (options.contains(COORD_OPTION)) {
            coordinators = commandLine.getOptionValue(COORD_OPTION);
            if (coordinators == null) {
                throw new OozieCLIException("No value specified for -coordinator option");
            }
        }

        if (options.contains(SLA_ENABLE_ALERT)) {
            wc.slaEnableAlert(jobIds, actions, dates, coordinators);
        }
        else if (options.contains(SLA_DISABLE_ALERT)) {
            wc.slaDisableAlert(jobIds, actions, dates, coordinators);
        }
        else if (options.contains(SLA_CHANGE)) {
            String newSlaParams = commandLine.getOptionValue(CHANGE_VALUE_OPTION);
            wc.slaChange(jobIds, actions, dates, coordinators, newSlaParams);
        }
    }

 }
diff --git a/client/src/main/java/org/apache/oozie/client/OozieClient.java b/client/src/main/java/org/apache/oozie/client/OozieClient.java
index e4c93cdae..5de25cc94 100644
-- a/client/src/main/java/org/apache/oozie/client/OozieClient.java
++ b/client/src/main/java/org/apache/oozie/client/OozieClient.java
@@ -52,6 +52,7 @@ import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
import java.util.Map.Entry;
 import java.util.Properties;
 import java.util.Set;
 import java.util.concurrent.Callable;
@@ -101,10 +102,10 @@ public class OozieClient {
 
     public static final String EXTERNAL_ID = "oozie.wf.external.id";
 
    public static final String WORKFLOW_NOTIFICATION_URL = "oozie.wf.workflow.notification.url";

     public static final String WORKFLOW_NOTIFICATION_PROXY = "oozie.wf.workflow.notification.proxy";
 
    public static final String WORKFLOW_NOTIFICATION_URL = "oozie.wf.workflow.notification.url";

     public static final String ACTION_NOTIFICATION_URL = "oozie.wf.action.notification.url";
 
     public static final String COORD_ACTION_NOTIFICATION_URL = "oozie.coord.action.notification.url";
@@ -155,6 +156,14 @@ public class OozieClient {
 
     public static final String FILTER_CREATED_TIME_END = "endcreatedtime";
 
    public static final String SLA_DISABLE_ALERT = "oozie.sla.disable.alerts";

    public static final String SLA_ENABLE_ALERT = "oozie.sla.enable.alerts";

    public static final String SLA_DISABLE_ALERT_OLDER_THAN = SLA_DISABLE_ALERT + ".older.than";

    public static final String SLA_DISABLE_ALERT_COORD = SLA_DISABLE_ALERT + ".coord";

     public static final String CHANGE_VALUE_ENDTIME = "endtime";
 
     public static final String CHANGE_VALUE_PAUSETIME = "pausetime";
@@ -1626,33 +1635,137 @@ public class OozieClient {
     }
 
     /**
     * Print sla info about coordinator and workflow jobs and actions.
     * Sla enable alert.
      *
     * @param start starting offset
     * @param len number of results
     * @throws OozieClientException
     * @param jobIds the job ids
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @throws OozieClientException the oozie client exception
      */
    public void getSlaInfo(int start, int len, String filter) throws OozieClientException {
        new SlaInfo(start, len, filter).call();
    public void slaEnableAlert(String jobIds, String actions, String dates) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_ENABLE_ALERT, jobIds, actions, dates, null).call();
     }
 
    private class SlaInfo extends ClientCallable<Void> {
    /**
     * Sla enable alert for bundle with coord name/id.
     *
     * @param bundleId the bundle id
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coordinators
     * @throws OozieClientException the oozie client exception
     */
    public void slaEnableAlert(String bundleId, String actions, String dates, String coords)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_ENABLE_ALERT, bundleId, actions, dates, coords).call();
    }
 
        SlaInfo(int start, int len, String filter) {
            super("GET", WS_PROTOCOL_VERSION_1, RestConstants.SLA, "", prepareParams(RestConstants.SLA_GT_SEQUENCE_ID,
                    Integer.toString(start), RestConstants.MAX_EVENTS, Integer.toString(len),
                    RestConstants.JOBS_FILTER_PARAM, filter));
    /**
     * Sla disable alert.
     *
     * @param jobIds the job ids
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @throws OozieClientException the oozie client exception
     */
    public void slaDisableAlert(String jobIds, String actions, String dates) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_DISABLE_ALERT, jobIds, actions, dates, null).call();
    }

    /**
     * Sla disable alert for bundle with coord name/id.
     *
     * @param bundleId the bundle id
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coordinators
     * @throws OozieClientException the oozie client exception
     */
    public void slaDisableAlert(String bundleId, String actions, String dates, String coords)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_DISABLE_ALERT, bundleId, actions, dates, coords).call();
    }

    /**
     * Sla change definations.
     * SLA change definition parameters can be [<key>=<value>,...<key>=<value>]
     * Supported parameter key names are should-start, should-end and max-duration
     * @param jobIds the job ids
     * @param actionIds comma separated list of action ids or action id ranges.
     * @param dates comma separated list of the nominal times
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String jobIds, String actions, String dates, String newSlaParams) throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, jobIds, actions, dates, null, newSlaParams).call();
    }

    /**
     * Sla change defination for bundle with coord name/id.
     * SLA change definition parameters can be [<key>=<value>,...<key>=<value>]
     * Supported parameter key names are should-start, should-end and max-duration
     * @param bundleId the bundle id
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coords
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String bundleId, String actions, String dates, String coords, String newSlaParams)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, bundleId, actions, dates, coords, newSlaParams).call();
    }

    /**
     * Sla change with new sla param as hasmap.
     * Supported parameter key names are should-start, should-end and max-duration
     * @param bundleId the bundle id
     * @param actionIds comma separated list of action ids or action id ranges
     * @param dates comma separated list of the nominal times
     * @param coords the coords
     * @param newSlaParams the new sla params
     * @throws OozieClientException the oozie client exception
     */
    public void slaChange(String bundleId, String actions, String dates, String coords, Map<String, String> newSlaParams)
            throws OozieClientException {
        new UpdateSLA(RestConstants.SLA_CHANGE, bundleId, actions, dates, coords, mapToString(newSlaParams)).call();
    }

    /**
     * Convert Map to string.
     *
     * @param map the map
     * @return the string
     */
    private String mapToString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> e = (Entry<String, String>) it.next();
            sb.append(e.getKey()).append("=").append(e.getValue()).append(";");
        }
        return sb.toString();
    }

    private class UpdateSLA extends ClientCallable<Void> {

        UpdateSLA(String action, String jobIds, String coordActions, String dates, String coords) {
            super("PUT", RestConstants.JOB, notEmpty(jobIds, "jobIds"), prepareParams(RestConstants.ACTION_PARAM,
                    action, RestConstants.JOB_COORD_SCOPE_ACTION_LIST, coordActions, RestConstants.JOB_COORD_SCOPE_DATE,
                    dates, RestConstants.COORDINATORS_PARAM, coords));
        }

        UpdateSLA(String action, String jobIds, String coordActions, String dates, String coords, String newSlaParams) {
            super("PUT", RestConstants.JOB, notEmpty(jobIds, "jobIds"), prepareParams(RestConstants.ACTION_PARAM,
                    action, RestConstants.JOB_COORD_SCOPE_ACTION_LIST, coordActions, RestConstants.JOB_COORD_SCOPE_DATE,
                    dates, RestConstants.COORDINATORS_PARAM, coords, RestConstants.JOB_CHANGE_VALUE, newSlaParams));
         }
 
         @Override
         protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
             conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
             if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.println("Done");
             }
             else {
                 handleError(conn);
@@ -1661,6 +1774,42 @@ public class OozieClient {
         }
     }
 
    /**
    * Print sla info about coordinator and workflow jobs and actions.
    *
    * @param start starting offset
    * @param len number of results
    * @throws OozieClientException
    */
        public void getSlaInfo(int start, int len, String filter) throws OozieClientException {
            new SlaInfo(start, len, filter).call();
        }

        private class SlaInfo extends ClientCallable<Void> {

            SlaInfo(int start, int len, String filter) {
                super("GET", WS_PROTOCOL_VERSION_1, RestConstants.SLA, "", prepareParams(RestConstants.SLA_GT_SEQUENCE_ID,
                        Integer.toString(start), RestConstants.MAX_EVENTS, Integer.toString(len),
                        RestConstants.JOBS_FILTER_PARAM, filter));
            }

            @Override
            protected Void call(HttpURLConnection conn) throws IOException, OozieClientException {
                conn.setRequestProperty("content-type", RestConstants.XML_CONTENT_TYPE);
                if ((conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                else {
                    handleError(conn);
                }
                return null;
            }
        }

     private class JobIdAction extends ClientCallable<String> {
 
         JobIdAction(String externalId) {
diff --git a/client/src/main/java/org/apache/oozie/client/event/SLAEvent.java b/client/src/main/java/org/apache/oozie/client/event/SLAEvent.java
index 27a0e1fa8..19d732f0e 100644
-- a/client/src/main/java/org/apache/oozie/client/event/SLAEvent.java
++ b/client/src/main/java/org/apache/oozie/client/event/SLAEvent.java
@@ -157,7 +157,7 @@ public abstract class SLAEvent extends Event {
      *
      * @return String slaConfig
      */
    public abstract String getSlaConfig();
    public abstract String getSLAConfig();
 
     /**
      * Get the actual start time of job for SLA
diff --git a/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java b/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
index b7cf0e73c..1022dd7e6 100644
-- a/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
++ b/client/src/main/java/org/apache/oozie/client/rest/JsonTags.java
@@ -172,6 +172,8 @@ public interface JsonTags {
     public static final String SLA_SUMMARY_JOB_STATUS = "jobStatus";
     public static final String SLA_SUMMARY_SLA_STATUS = "slaStatus";
     public static final String SLA_SUMMARY_LAST_MODIFIED = "lastModified";
    public static final String SLA_ALERT_STATUS = "slaAlertStatus";

 
     public static final String TO_STRING = "toString";
 
diff --git a/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java b/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
index 3c2afc3aa..4c75d2a95 100644
-- a/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
++ b/client/src/main/java/org/apache/oozie/client/rest/RestConstants.java
@@ -186,4 +186,24 @@ public interface RestConstants {
     public static final String LOG_FILTER_OPTION = "logfilter";
 
     public static final String JOB_COORD_RERUN_FAILED_PARAM = "failed";

    public static final String SLA_DISABLE_ALERT = "sla-disable";

    public static final String SLA_ENABLE_ALERT = "sla-enable";

    public static final String SLA_CHANGE = "sla-change";

    public static final String SLA_ALERT_RANGE = "sla-alert-range";

    public static final String COORDINATORS_PARAM = "coordinators";

    public static final String SLA_NOMINAL_TIME = "sla-nominal-time";

    public static final String SLA_SHOULD_START = "sla-should-start";

    public static final String SLA_SHOULD_END = "sla-should-end";

    public static final String SLA_MAX_DURATION = "sla-max-duration";

    public static final String JOB_COORD_SCOPE_ACTION_LIST = "action-list";
 }
diff --git a/core/src/main/java/org/apache/oozie/BaseEngine.java b/core/src/main/java/org/apache/oozie/BaseEngine.java
index bf38a0c84..44074ea94 100644
-- a/core/src/main/java/org/apache/oozie/BaseEngine.java
++ b/core/src/main/java/org/apache/oozie/BaseEngine.java
@@ -239,4 +239,44 @@ public abstract class BaseEngine {
      * @throws BaseEngineException thrown if the job's status could not be obtained
      */
     public abstract String getJobStatus(String jobId) throws BaseEngineException;

    /**
     * Return the status for a Job ID
     *
     * @param jobId job Id.
     * @return the job's status
     * @throws BaseEngineException thrown if the job's status could not be obtained
     */

    /**
     * Enable SLA alert for job
     * @param id
     * @param actions
     * @param dates
     * @param childIds
     * @throws BaseEngineException
     */
    public abstract void enableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException;

    /**
     * Disable SLA alert for job
     * @param id
     * @param actions
     * @param dates
     * @param childIds
     * @throws BaseEngineException
     */
    public abstract void disableSLAAlert(String id, String actions, String  dates, String childIds) throws BaseEngineException;

    /**
     * Change SLA properties for job
     * @param id
     * @param actions
     * @param childIds
     * @param newParams
     * @throws BaseEngineException
     */
    public abstract void changeSLA(String id, String actions, String  dates, String childIds, String newParams)
            throws BaseEngineException;

 }
diff --git a/core/src/main/java/org/apache/oozie/BundleEngine.java b/core/src/main/java/org/apache/oozie/BundleEngine.java
index 9818acc77..659c8e633 100644
-- a/core/src/main/java/org/apache/oozie/BundleEngine.java
++ b/core/src/main/java/org/apache/oozie/BundleEngine.java
@@ -30,7 +30,6 @@ import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.CoordinatorJob;
@@ -40,6 +39,9 @@ import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.rest.BulkResponseImpl;
 import org.apache.oozie.command.BulkJobsXCommand;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.bundle.BundleSLAAlertsDisableXCommand;
import org.apache.oozie.command.bundle.BundleSLAAlertsEnableXCommand;
import org.apache.oozie.command.bundle.BundleSLAChangeXCommand;
 import org.apache.oozie.command.bundle.BundleJobChangeXCommand;
 import org.apache.oozie.command.bundle.BundleJobResumeXCommand;
 import org.apache.oozie.command.bundle.BundleJobSuspendXCommand;
@@ -55,6 +57,7 @@ import org.apache.oozie.service.DagXLogInfoService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.XLogStreamingService;
 import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.JobUtils;
 import org.apache.oozie.util.XLogFilter;
 import org.apache.oozie.util.XLogUserFilterParam;
 import org.apache.oozie.util.ParamChecker;
@@ -506,4 +509,41 @@ public class BundleEngine extends BaseEngine {
             throw new BundleEngineException(e);
         }
     }

    @Override
    public void enableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        try {
            new BundleSLAAlertsEnableXCommand(id, actions, dates, childIds).call();
        }
        catch (CommandException e) {
            throw new BundleEngineException(e);
        }
    }

    @Override
    public void disableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        try {
            new BundleSLAAlertsDisableXCommand(id, actions, dates, childIds).call();
        }
        catch (CommandException e) {
            throw new BundleEngineException(e);
        }
    }

    @Override
    public void changeSLA(String id, String actions, String dates, String childIds, String newParams)
            throws BaseEngineException {
        Map<String, String> slaNewParams = null;
        try {

            if (newParams != null) {
                slaNewParams = JobUtils.parseChangeValue(newParams);
            }
            new BundleSLAChangeXCommand(id, actions, dates, childIds, slaNewParams).call();
        }
        catch (CommandException e) {
            throw new BundleEngineException(e);
        }
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
index bd01d14c7..85b7ed43e 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
@@ -18,6 +18,23 @@
 
 package org.apache.oozie;
 
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

 import org.apache.hadoop.io.Writable;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.rest.JsonBean;
@@ -30,25 +47,6 @@ import org.apache.openjpa.persistence.jdbc.Strategy;
 import org.json.simple.JSONArray;
 import org.json.simple.JSONObject;
 
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
 
 @Entity
 @NamedQueries({
@@ -149,13 +147,13 @@ import java.util.List;
 
         @NamedQuery(name = "GET_COORD_ACTIONS_FOR_RECOVERY_OLDER_THAN", query = "select a.id, a.jobId, a.statusStr, a.externalId, a.pending from CoordinatorActionBean a where a.pending > 0 AND (a.statusStr = 'SUSPENDED' OR a.statusStr = 'KILLED' OR a.statusStr = 'RUNNING') AND a.lastModifiedTimestamp <= :lastModifiedTime"),
         // Select query used by rerun, requires almost all columns so select * is used
        @NamedQuery(name = "GET_ACTIONS_FOR_DATES", query = "select OBJECT(a) from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr = 'TIMEDOUT' OR a.statusStr = 'SUCCEEDED' OR a.statusStr = 'KILLED' OR a.statusStr = 'FAILED' OR a.statusStr = 'IGNORED') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
        @NamedQuery(name = "GET_TERMINATED_ACTIONS_FOR_DATES", query = "select OBJECT(a) from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr = 'TIMEDOUT' OR a.statusStr = 'SUCCEEDED' OR a.statusStr = 'KILLED' OR a.statusStr = 'FAILED' OR a.statusStr = 'IGNORED') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
         // Select query used by log
        @NamedQuery(name = "GET_ACTION_IDS_FOR_DATES", query = "select a.id from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr = 'TIMEDOUT' OR a.statusStr = 'SUCCEEDED' OR a.statusStr = 'KILLED' OR a.statusStr = 'FAILED') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
        @NamedQuery(name = "GET_TERMINATED_ACTION_IDS_FOR_DATES", query = "select a.id from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr = 'TIMEDOUT' OR a.statusStr = 'SUCCEEDED' OR a.statusStr = 'KILLED' OR a.statusStr = 'FAILED') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
         // Select query used by rerun, requires almost all columns so select * is used
         @NamedQuery(name = "GET_ACTION_FOR_NOMINALTIME", query = "select OBJECT(a) from CoordinatorActionBean a where a.jobId = :jobId AND a.nominalTimestamp = :nominalTime"),
 
        @NamedQuery(name = "GET_ACTIONS_BY_DATES_FOR_KILL", query = "select a.id, a.jobId, a.statusStr, a.externalId, a.pending, a.nominalTimestamp, a.createdTimestamp from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr <> 'FAILED' AND a.statusStr <> 'KILLED' AND a.statusStr <> 'SUCCEEDED' AND a.statusStr <> 'TIMEDOUT') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
        @NamedQuery(name = "GET_ACTIVE_ACTIONS_FOR_DATES", query = "select a.id, a.jobId, a.statusStr, a.externalId, a.pending, a.nominalTimestamp, a.createdTimestamp from CoordinatorActionBean a where a.jobId = :jobId AND (a.statusStr = 'WAITING' OR a.statusStr = 'READY' OR a.statusStr = 'SUBMITTED' OR a.statusStr = 'RUNNING'  OR a.statusStr = 'SUSPENDED') AND a.nominalTimestamp >= :startTime AND a.nominalTimestamp <= :endTime"),
 
         @NamedQuery(name = "GET_COORD_ACTIONS_COUNT", query = "select count(w) from CoordinatorActionBean w"),
 
@@ -163,7 +161,12 @@ import java.util.List;
 
         @NamedQuery(name = "GET_COORD_ACTIONS_MAX_MODIFIED_DATE_FOR_RANGE", query = "select max(w.lastModifiedTimestamp) from CoordinatorActionBean w where w.jobId= :jobId and w.id >= :startAction AND w.id <= :endAction"),
 
        @NamedQuery(name = "GET_READY_ACTIONS_GROUP_BY_JOBID", query = "select a.jobId, min(a.lastModifiedTimestamp) from CoordinatorActionBean a where a.statusStr = 'READY' group by a.jobId having min(a.lastModifiedTimestamp) < :lastModifiedTime")})
         @NamedQuery(name = "GET_READY_ACTIONS_GROUP_BY_JOBID", query = "select a.jobId, min(a.lastModifiedTimestamp) from CoordinatorActionBean a where a.statusStr = 'READY' group by a.jobId having min(a.lastModifiedTimestamp) < :lastModifiedTime"),

         @NamedQuery(name = "GET_ACTIVE_ACTIONS_IDS_FOR_SLA_CHANGE", query = "select a.id, a.nominalTimestamp, a.createdTimestamp, a.actionXml  from CoordinatorActionBean a where a.id in (:ids) and (a.statusStr <> 'FAILED' AND a.statusStr <> 'KILLED' AND a.statusStr <> 'SUCCEEDED' AND a.statusStr <> 'TIMEDOUT'  AND a.statusStr <> 'IGNORED')"),

         @NamedQuery(name = "GET_ACTIVE_ACTIONS_JOBID_FOR_SLA_CHANGE", query = "select a.id, a.nominalTimestamp, a.createdTimestamp, a.actionXml  from CoordinatorActionBean a where a.jobId = :jobId and (a.statusStr <> 'FAILED' AND a.statusStr <> 'KILLED' AND a.statusStr <> 'SUCCEEDED' AND a.statusStr <> 'TIMEDOUT'  AND a.statusStr <> 'IGNORED')")
 })
 
 @Table(name = "COORD_ACTIONS")
 public class CoordinatorActionBean implements
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
index 136c09730..642a82a3f 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorEngine.java
@@ -19,6 +19,7 @@
 package org.apache.oozie;
 
 import com.google.common.annotations.VisibleForTesting;

 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.client.CoordinatorAction;
@@ -36,6 +37,9 @@ import org.apache.oozie.command.coord.CoordJobsXCommand;
 import org.apache.oozie.command.coord.CoordKillXCommand;
 import org.apache.oozie.command.coord.CoordRerunXCommand;
 import org.apache.oozie.command.coord.CoordResumeXCommand;
import org.apache.oozie.command.coord.CoordSLAAlertsDisableXCommand;
import org.apache.oozie.command.coord.CoordSLAAlertsEnableXCommand;
import org.apache.oozie.command.coord.CoordSLAChangeXCommand;
 import org.apache.oozie.command.coord.CoordSubmitXCommand;
 import org.apache.oozie.command.coord.CoordSuspendXCommand;
 import org.apache.oozie.command.coord.CoordUpdateXCommand;
@@ -49,6 +53,7 @@ import org.apache.oozie.service.Services;
 import org.apache.oozie.service.XLogStreamingService;
 import org.apache.oozie.util.CoordActionsInDateRange;
 import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.JobUtils;
 import org.apache.oozie.util.Pair;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
@@ -847,4 +852,46 @@ public class CoordinatorEngine extends BaseEngine {
             throw new CoordinatorEngineException(e);
         }
     }

    @Override
    public void disableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        try {
            new CoordSLAAlertsDisableXCommand(id, actions, dates).call();

        }
        catch (CommandException e) {
            throw new CoordinatorEngineException(e);
        }
    }

    @Override
    public void changeSLA(String id, String actions, String dates, String childIds, String newParams)
            throws BaseEngineException {
        Map<String, String> slaNewParams = null;

        try {

            if (newParams != null) {
                slaNewParams = JobUtils.parseChangeValue(newParams);
            }

            new CoordSLAChangeXCommand(id, actions, dates, slaNewParams).call();

        }
        catch (CommandException e) {
            throw new CoordinatorEngineException(e);
        }
    }

    @Override
    public void enableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        try {
            new CoordSLAAlertsEnableXCommand(id, actions, dates).call();

        }
        catch (CommandException e) {
            throw new CoordinatorEngineException(e);
        }
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
index 4d6b97025..c3ee83997 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorJobBean.java
@@ -18,18 +18,14 @@
 
 package org.apache.oozie;
 
import org.apache.hadoop.io.Writable;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.JsonTags;
import org.apache.oozie.client.rest.JsonUtils;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.WritableUtils;
import org.apache.openjpa.persistence.jdbc.Index;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
 
 import javax.persistence.Basic;
 import javax.persistence.Column;
@@ -42,14 +38,19 @@ import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.Table;
 import javax.persistence.Transient;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.io.Writable;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.JsonTags;
import org.apache.oozie.client.rest.JsonUtils;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.WritableUtils;
import org.apache.openjpa.persistence.jdbc.Index;
import org.apache.openjpa.persistence.jdbc.Strategy;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
 
 @Entity
 @NamedQueries( {
@@ -79,6 +80,10 @@ import java.util.List;
 
         @NamedQuery(name = "UPDATE_COORD_JOB_CHANGE", query = "update CoordinatorJobBean w set w.endTimestamp = :endTime, w.statusStr = :status, w.pending = :pending, w.doneMaterialization = :doneMaterialization, w.concurrency = :concurrency, w.pauseTimestamp = :pauseTime, w.lastActionNumber = :lastActionNumber, w.lastActionTimestamp = :lastActionTime, w.nextMaterializedTimestamp = :nextMatdTime, w.lastModifiedTimestamp = :lastModifiedTime where w.id = :id"),
 
        @NamedQuery(name = "UPDATE_COORD_JOB_CONF", query = "update CoordinatorJobBean w set w.conf = :conf where w.id = :id"),

        @NamedQuery(name = "UPDATE_COORD_JOB_XML", query = "update CoordinatorJobBean w set w.jobXml = :jobXml where w.id = :id"),

         @NamedQuery(name = "DELETE_COORD_JOB", query = "delete from CoordinatorJobBean w where w.id IN (:id)"),
 
         @NamedQuery(name = "GET_COORD_JOBS", query = "select OBJECT(w) from CoordinatorJobBean w"),
@@ -108,7 +113,7 @@ import java.util.List;
         //TODO need to remove.
         @NamedQuery(name = "GET_COORD_JOBS_OLDER_THAN", query = "select OBJECT(w) from CoordinatorJobBean w where w.startTimestamp <= :matTime AND (w.statusStr = 'PREP' OR w.statusStr = 'RUNNING' or w.statusStr = 'RUNNINGWITHERROR') AND (w.nextMaterializedTimestamp < :matTime OR w.nextMaterializedTimestamp IS NULL) AND (w.nextMaterializedTimestamp IS NULL OR (w.endTimestamp > w.nextMaterializedTimestamp AND (w.pauseTimestamp IS NULL OR w.pauseTimestamp > w.nextMaterializedTimestamp))) order by w.lastModifiedTimestamp"),
 
        @NamedQuery(name = "GET_COORD_JOBS_OLDER_FOR_MATERILZATION", query = "select w.id from CoordinatorJobBean w where w.startTimestamp <= :matTime AND (w.statusStr = 'PREP' OR w.statusStr = 'RUNNING' or w.statusStr = 'RUNNINGWITHERROR') AND (w.nextMaterializedTimestamp < :matTime OR w.nextMaterializedTimestamp IS NULL) AND (w.nextMaterializedTimestamp IS NULL OR (w.endTimestamp > w.nextMaterializedTimestamp AND (w.pauseTimestamp IS NULL OR w.pauseTimestamp > w.nextMaterializedTimestamp))) and w.matThrottling > ( select count(a.jobId) from CoordinatorActionBean a where a.jobId = w.id and a.statusStr = 'WAITING') order by w.lastModifiedTimestamp"),
        @NamedQuery(name = "GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION", query = "select w.id from CoordinatorJobBean w where w.startTimestamp <= :matTime AND (w.statusStr = 'PREP' OR w.statusStr = 'RUNNING' or w.statusStr = 'RUNNINGWITHERROR') AND (w.nextMaterializedTimestamp < :matTime OR w.nextMaterializedTimestamp IS NULL) AND (w.nextMaterializedTimestamp IS NULL OR (w.endTimestamp > w.nextMaterializedTimestamp AND (w.pauseTimestamp IS NULL OR w.pauseTimestamp > w.nextMaterializedTimestamp))) and w.matThrottling > ( select count(a.jobId) from CoordinatorActionBean a where a.jobId = w.id and a.statusStr = 'WAITING') order by w.lastModifiedTimestamp"),
 
         @NamedQuery(name = "GET_COORD_JOBS_OLDER_THAN_STATUS", query = "select OBJECT(w) from CoordinatorJobBean w where w.statusStr = :status AND w.lastModifiedTimestamp <= :lastModTime order by w.lastModifiedTimestamp"),
 
@@ -134,7 +139,13 @@ import java.util.List;
 
         @NamedQuery(name = "GET_COORD_JOB_STATUS_PARENTID", query = "select w.statusStr, w.bundleId from CoordinatorJobBean w where w.id = :id"),
 
        @NamedQuery(name = "GET_COORD_IDS_FOR_STATUS_TRANSIT", query = "select DISTINCT w.id from CoordinatorActionBean a, CoordinatorJobBean w where w.id = a.jobId and a.lastModifiedTimestamp >= :lastModifiedTime and (w.statusStr IN ('PAUSED', 'RUNNING', 'RUNNINGWITHERROR', 'PAUSEDWITHERROR') or w.pending = 1)  and w.statusStr <> 'IGNORED'")
        @NamedQuery(name = "GET_COORD_IDS_FOR_STATUS_TRANSIT", query = "select DISTINCT w.id from CoordinatorActionBean a, CoordinatorJobBean w where w.id = a.jobId and a.lastModifiedTimestamp >= :lastModifiedTime and (w.statusStr IN ('PAUSED', 'RUNNING', 'RUNNINGWITHERROR', 'PAUSEDWITHERROR') or w.pending = 1)  and w.statusStr <> 'IGNORED'"),

        @NamedQuery(name = "GET_COORD_JOBS_FOR_BUNDLE_BY_APPNAME_ID", query = "select w.id from CoordinatorJobBean w where ( w.appName IN (:appName) OR w.id IN (:appName) )  AND w.bundleId = :bundleId"),

        @NamedQuery(name = "GET_COORD_JOB_CONF", query = "select w.conf from CoordinatorJobBean w where w.id = :id"),

        @NamedQuery(name = "GET_COORD_JOB_XML", query = "select w.jobXml from CoordinatorJobBean w where w.id = :id")
 
 })
 @NamedNativeQueries({
@@ -221,7 +232,6 @@ public class CoordinatorJobBean implements Writable, CoordinatorJob, JsonBean {
     private java.sql.Timestamp startTimestamp = null;
 
     @Basic
    @Index
     @Column(name = "end_time")
     private java.sql.Timestamp endTimestamp = null;
 
diff --git a/core/src/main/java/org/apache/oozie/DagEngine.java b/core/src/main/java/org/apache/oozie/DagEngine.java
index 50aef2fad..ac2e7b1bb 100644
-- a/core/src/main/java/org/apache/oozie/DagEngine.java
++ b/core/src/main/java/org/apache/oozie/DagEngine.java
@@ -585,4 +585,20 @@ public class DagEngine extends BaseEngine {
             throw new DagEngineException(ex);
         }
     }

    @Override
    public void enableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        throw new BaseEngineException(new XException(ErrorCode.E0301, "Not supported for workflow"));
    }

    @Override
    public void disableSLAAlert(String id, String actions, String dates, String childIds) throws BaseEngineException {
        throw new BaseEngineException(new XException(ErrorCode.E0301, "Not supported for workflow"));
    }

    @Override
    public void changeSLA(String id, String actions, String dates, String childIds, String newParams) throws BaseEngineException {
        throw new BaseEngineException(new XException(ErrorCode.E0301, "Not supported for workflow"));
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index 4444c87e5..7630c2f66 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -209,6 +209,8 @@ public enum ErrorCode {
     E1023(XLog.STD, "Coord Job update Error: [{0}]"),
     E1024(XLog.STD, "Cannot run ignore command: [{0}]"),
     E1025(XLog.STD, "Coord status transit error: [{0}]"),
    E1026(XLog.STD, "SLA alert update command failed: {0}"),
    E1027(XLog.STD, "SLA change command failed. {0}"),
 
 
     E1100(XLog.STD, "Command precondition does not hold before execution, [{0}]"),
diff --git a/core/src/main/java/org/apache/oozie/command/SLAAlertsXCommand.java b/core/src/main/java/org/apache/oozie/command/SLAAlertsXCommand.java
new file mode 100644
index 000000000..baf3a278d
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/SLAAlertsXCommand.java
@@ -0,0 +1,117 @@
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

package org.apache.oozie.command;

import java.util.Map;

import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.util.LogUtils;

public abstract class SLAAlertsXCommand extends XCommand<Void> {

    private String jobId;

    public SLAAlertsXCommand(String jobId, String name, String type) {
        super(name, type, 1);
        this.jobId = jobId;
    }

    @Override
    final protected boolean isLockRequired() {
        return true;
    }

    @Override
    final public String getEntityKey() {
        return getJobId();
    }

    final public String getJobId() {
        return jobId;
    }

    @Override
    protected void setLogInfo() {
        LogUtils.setLogInfo(jobId);
    }

    @Override
    protected void loadState() throws CommandException {

    }

    @Override
    protected void verifyPrecondition() throws CommandException, PreconditionException {
    }

    @Override
    protected Void execute() throws CommandException {
        try {
            if (!executeSlaCommand()) {
                if (!isJobRequest()) {
                    throw new CommandException(ErrorCode.E1026, "No record found");
                }
            }

        }
        catch (ServiceException e) {
            throw new CommandException(e);
        }
        updateJob();
        return null;
    }

    @Override
    public String getKey() {
        return getName() + "_" + jobId;
    }

    protected void validateSLAChangeParam(Map<String, String> slaParams) throws CommandException, PreconditionException {
        for (String key : slaParams.keySet()) {
            if (key.equals(RestConstants.SLA_NOMINAL_TIME) || key.equals(RestConstants.SLA_SHOULD_START)
                    || key.equals(RestConstants.SLA_SHOULD_END) || key.equals(RestConstants.SLA_MAX_DURATION)) {
                // good.
            }
            else {
                throw new CommandException(ErrorCode.E1027, "Unsupported parameter " + key);
            }
        }
    }

    /**
     * Execute sla command.
     *
     * @return true, if successful
     * @throws ServiceException the service exception
     * @throws CommandException the command exception
     */
    protected abstract boolean executeSlaCommand() throws ServiceException, CommandException;

    /**
     * Update job.
     *
     * @throws CommandException the command exception
     */
    protected abstract void updateJob() throws CommandException;

    protected abstract boolean isJobRequest() throws CommandException;

}
diff --git a/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsDisableXCommand.java b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsDisableXCommand.java
new file mode 100644
index 000000000..4f4e2cda4
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsDisableXCommand.java
@@ -0,0 +1,44 @@
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
package org.apache.oozie.command.bundle;

import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordSLAAlertsDisableXCommand;
import org.apache.oozie.service.ServiceException;

public class BundleSLAAlertsDisableXCommand extends BundleSLAAlertsXCommand {

    public BundleSLAAlertsDisableXCommand(String jobId, String actions, String dates, String childIds) {
        super(jobId, actions, dates, childIds);

    }

    @Override
    protected void loadState() throws CommandException {
    }

    @Override
    protected void updateJob() throws CommandException {
    }

    @Override
    protected void executeCoordCommand(String id, String actions, String dates) throws ServiceException,
            CommandException {
        new CoordSLAAlertsDisableXCommand(id, actions, dates).call();
    }
}
diff --git a/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsEnableXCommand.java b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsEnableXCommand.java
new file mode 100644
index 000000000..4d3b75c9f
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsEnableXCommand.java
@@ -0,0 +1,45 @@
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

package org.apache.oozie.command.bundle;

import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordSLAAlertsEnableXCommand;
import org.apache.oozie.service.ServiceException;

public class BundleSLAAlertsEnableXCommand extends BundleSLAAlertsXCommand {

    public BundleSLAAlertsEnableXCommand(String jobId, String actions, String dates, String childIds) {
        super(jobId, actions, dates, childIds);

    }

    @Override
    protected void loadState() throws CommandException {
    }

    @Override
    protected void executeCoordCommand(String id, String actions, String dates) throws ServiceException,
            CommandException {
        new CoordSLAAlertsEnableXCommand(id, actions, dates).call();
    }

    @Override
    protected void updateJob() throws CommandException {
    }
}
diff --git a/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsXCommand.java b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsXCommand.java
new file mode 100644
index 000000000..1e6f6aea9
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAAlertsXCommand.java
@@ -0,0 +1,149 @@
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

package org.apache.oozie.command.bundle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.XException;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.SLAAlertsXCommand;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
import org.apache.oozie.service.ServiceException;

public abstract class BundleSLAAlertsXCommand extends SLAAlertsXCommand {

    private String actions;

    private String dates;

    private String childIds;

    public BundleSLAAlertsXCommand(String jobId, String actions, String dates, String childIds) {
        super(jobId, "SLA.command", "SLA.command");
        this.actions = actions;
        this.dates = dates;
        this.childIds = childIds;

    }

    @Override
    protected void loadState() throws CommandException {
    }

    /**
     * Gets the coord jobs from bundle.
     *
     * @param id the bundle id
     * @param coords the coords name/id
     * @return the coord jobs from bundle
     * @throws CommandException the command exception
     */
    protected Set<String> getCoordJobsFromBundle(String id, String coords) throws CommandException {
        Set<String> jobs = new HashSet<String>();
        List<CoordinatorJobBean> coordJobs;
        try {
            if (coords == null) {
                coordJobs = CoordJobQueryExecutor.getInstance()
                        .getList(CoordJobQuery.GET_COORD_JOBS_WITH_PARENT_ID, id);
            }
            else {
                coordJobs = CoordJobQueryExecutor.getInstance().getList(
                        CoordJobQuery.GET_COORD_JOBS_FOR_BUNDLE_BY_APPNAME_ID, Arrays.asList(coords.split(",")), id);
            }
        }
        catch (XException e) {
            throw new CommandException(e);
        }
        for (CoordinatorJobBean jobBean : coordJobs) {
            jobs.add(jobBean.getId());
        }
        return jobs;

    }

    /**
     * Gets the coord jobs.
     *
     * @return the coord jobs
     */
    protected String getCoordJobs() {
        return childIds;
    }

    /**
     * Gets the actions.
     *
     * @return the actions
     */
    protected String getActions() {
        return actions;
    }

    /**
     * Gets the dates.
     *
     * @return the dates
     */
    protected String getDates() {
        return dates;
    }

    protected boolean isJobRequest() {
        return true;

    }

    @Override
    protected boolean executeSlaCommand() throws ServiceException, CommandException {
        StringBuffer report = new StringBuffer();

        Set<String> coordJobs = getCoordJobsFromBundle(getJobId(), getCoordJobs());

        if (coordJobs.isEmpty()) {
            throw new CommandException(ErrorCode.E1026, "No record found");
        }
        else {
            for (String job : coordJobs) {
                try {
                    executeCoordCommand(job, getActions(), getDates());
                }
                catch (Exception e) {
                    // Ignore exception for coords.
                    String errorMsg = "SLA command for coord job " + job + " failed. Error message is  : " + e.getMessage();
                    LOG.error(errorMsg, e);
                    report.append(errorMsg).append(System.getProperty("line.separator"));
                }
            }
            if (!report.toString().isEmpty()) {
                throw new CommandException(ErrorCode.E1026, report.toString());
            }
            return true;
        }
    }

    protected abstract void executeCoordCommand(String id, String actions, String dates) throws ServiceException,
            CommandException;

}
diff --git a/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAChangeXCommand.java b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAChangeXCommand.java
new file mode 100644
index 000000000..653045178
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/bundle/BundleSLAChangeXCommand.java
@@ -0,0 +1,57 @@
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

package org.apache.oozie.command.bundle;

import java.util.Map;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.command.coord.CoordSLAChangeXCommand;
import org.apache.oozie.service.ServiceException;

public class BundleSLAChangeXCommand extends BundleSLAAlertsXCommand {

    Map<String, String> newSlaParams;

    public BundleSLAChangeXCommand(String jobId, String actions, String dates, String childIds,
            Map<String, String> newSlaParams) {
        super(jobId, actions, dates, childIds);
        this.newSlaParams = newSlaParams;

    }

    @Override
    protected void loadState() throws CommandException {
    }

    @Override
    protected void executeCoordCommand(String id, String actions, String dates) throws ServiceException,
            CommandException {
        new CoordSLAChangeXCommand(id, actions, dates, newSlaParams).call();
    }

    @Override
    protected void updateJob() throws CommandException {
    }

    @Override
    protected void verifyPrecondition() throws CommandException, PreconditionException {
        validateSLAChangeParam(newSlaParams);
    }

}
diff --git a/core/src/main/java/org/apache/oozie/command/bundle/BundleStatusTransitXCommand.java b/core/src/main/java/org/apache/oozie/command/bundle/BundleStatusTransitXCommand.java
index d6a319701..953e899b7 100644
-- a/core/src/main/java/org/apache/oozie/command/bundle/BundleStatusTransitXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/bundle/BundleStatusTransitXCommand.java
@@ -90,6 +90,7 @@ public class BundleStatusTransitXCommand extends StatusTransitXCommand {
                 }
 
                 if (bAction.isPending()) {
                    LOG.debug(bAction + " has pending flag set");
                     foundPending = true;
                 }
             }
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
index 548946f05..39e6ac15c 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
@@ -32,6 +32,7 @@ import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.MaterializeTransitionXCommand;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.command.bundle.BundleStatusUpdateXCommand;
import org.apache.oozie.coord.CoordUtils;
 import org.apache.oozie.coord.TimeUnit;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
@@ -486,7 +487,7 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
                 actionBean.setTimeOut(timeout);
 
                 if (!dryrun) {
                    storeToDB(actionBean, action); // Storing to table
                    storeToDB(actionBean, action, jobConf); // Storing to table
 
                 }
                 else {
@@ -524,26 +525,28 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
         }
     }
 
    private void storeToDB(CoordinatorActionBean actionBean, String actionXml) throws Exception {
    private void storeToDB(CoordinatorActionBean actionBean, String actionXml, Configuration jobConf) throws Exception {
         LOG.debug("In storeToDB() coord action id = " + actionBean.getId() + ", size of actionXml = "
                 + actionXml.length());
         actionBean.setActionXml(actionXml);
 
         insertList.add(actionBean);
        writeActionSlaRegistration(actionXml, actionBean);
        writeActionSlaRegistration(actionXml, actionBean, jobConf);
     }
 
    private void writeActionSlaRegistration(String actionXml, CoordinatorActionBean actionBean) throws Exception {
    private void writeActionSlaRegistration(String actionXml, CoordinatorActionBean actionBean, Configuration jobConf)
            throws Exception {
         Element eAction = XmlUtils.parseXml(actionXml);
         Element eSla = eAction.getChild("action", eAction.getNamespace()).getChild("info", eAction.getNamespace("sla"));
        SLAEventBean slaEvent = SLADbOperations.createSlaRegistrationEvent(eSla, actionBean.getId(), SlaAppType.COORDINATOR_ACTION, coordJob
                .getUser(), coordJob.getGroup(), LOG);
        if(slaEvent != null) {
                SLAEventBean slaEvent = SLADbOperations.createSlaRegistrationEvent(eSla, actionBean.getId(),
                                 SlaAppType.COORDINATOR_ACTION, coordJob.getUser(), coordJob.getGroup(), LOG);
                         if (slaEvent != null) {
             insertList.add(slaEvent);
         }
         // inserting into new table also
         SLAOperations.createSlaRegistrationEvent(eSla, actionBean.getId(), actionBean.getJobId(),
                AppType.COORDINATOR_ACTION, coordJob.getUser(), coordJob.getAppName(), LOG, false);
                AppType.COORDINATOR_ACTION, coordJob.getUser(), coordJob.getAppName(), LOG, false,
                CoordUtils.isSlaAlertDisabled(actionBean, coordJob.getAppName(), jobConf));
     }
 
     private void updateJobMaterializeInfo(CoordinatorJobBean job) throws CommandException {
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsDisableXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsDisableXCommand.java
new file mode 100644
index 000000000..11daa4199
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsDisableXCommand.java
@@ -0,0 +1,71 @@
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

import java.util.ArrayList;

import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.apache.oozie.sla.SLAOperations;
import org.apache.oozie.sla.service.SLAService;
import org.apache.oozie.util.XConfiguration;

public class CoordSLAAlertsDisableXCommand extends CoordSLAAlertsXCommand {

    public CoordSLAAlertsDisableXCommand(String id, String actions, String dates) {
        super(id, "SLA.alerts.disable", "SLA.alerts.disable", actions, dates);

    }

    @SuppressWarnings("serial")
    @Override
    protected boolean executeSlaCommand() throws ServiceException, CommandException {
        if (getActionList() == null) {
            // if getActionList() == null, means enable command is for all child job.
            return Services.get().get(SLAService.class).disableChildJobAlert(new ArrayList<String>() {
                {
                    add(getJobId());

                }
            });
        }
        else {
            return Services.get().get(SLAService.class).disableAlert(getActionList());
        }

    }

    @Override
    protected void updateJob() throws CommandException {
        XConfiguration conf = new XConfiguration();
        if (isJobRequest()) {
            LOG.debug("Updating job property " + OozieClient.SLA_DISABLE_ALERT + " = " + SLAOperations.ALL_VALUE);
            conf.set(OozieClient.SLA_DISABLE_ALERT, SLAOperations.ALL_VALUE);
        }
        else {
            LOG.debug("Updating job property " + OozieClient.SLA_DISABLE_ALERT + " = " + SLAOperations.ALL_VALUE);
            conf.set(OozieClient.SLA_DISABLE_ALERT, getActionDateListAsString());
        }

        updateJobConf(conf);

    }

}
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsEnableXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsEnableXCommand.java
new file mode 100644
index 000000000..936f13d22
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsEnableXCommand.java
@@ -0,0 +1,65 @@
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

import java.util.ArrayList;

import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.apache.oozie.sla.service.SLAService;
import org.apache.oozie.util.XConfiguration;

public class CoordSLAAlertsEnableXCommand extends CoordSLAAlertsXCommand {

    public CoordSLAAlertsEnableXCommand(String id, String actions, String dates) {
        super(id, "SLA.alerts.enable", "SLA.alerts.enable", actions, dates);
    }

    @SuppressWarnings("serial")
    @Override
    protected boolean executeSlaCommand() throws ServiceException, CommandException {
        if (getActionList() == null) {
            // if getActionList() == null, means enable command is for all child job.
            return Services.get().get(SLAService.class).enableChildJobAlert(new ArrayList<String>() {
                {
                    add(getJobId());
                }
            });
        }
        else {
            return Services.get().get(SLAService.class).enableAlert(getActionList());
        }
    }

    @Override
    protected void updateJob() throws CommandException {
        XConfiguration conf = new XConfiguration();
        if (isJobRequest()) {
            conf.set(OozieClient.SLA_DISABLE_ALERT, "");
            LOG.debug("Updating job property " + OozieClient.SLA_DISABLE_ALERT + " = ");
        }
        else {
            conf.set(OozieClient.SLA_ENABLE_ALERT, getActionDateListAsString());
            LOG.debug("Updating job property " + OozieClient.SLA_DISABLE_ALERT + " = " + getActionDateListAsString());

        }
        updateJobConf(conf);
    }
}
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsXCommand.java
new file mode 100644
index 000000000..b8affd67a
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAAlertsXCommand.java
@@ -0,0 +1,233 @@
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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.XException;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.SLAAlertsXCommand;
import org.apache.oozie.coord.CoordUtils;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.sla.SLAOperations;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public abstract class CoordSLAAlertsXCommand extends SLAAlertsXCommand {

    private String scope;
    private String dates;
    private List<String> actionIds;

    @Override
    protected void loadState() throws CommandException {
        actionIds = getActionListForScopeAndDate(getJobId(), scope, dates);

    }

    public CoordSLAAlertsXCommand(String jobId, String name, String type, String actions, String dates) {
        super(jobId, name, type);
        this.scope = actions;
        this.dates = dates;

    }

    /**
     * Update job conf.
     *
     * @param newConf the new conf
     * @throws CommandException the command exception
     */
    protected void updateJobConf(Configuration newConf) throws CommandException {

        try {
            CoordinatorJobBean job = new CoordinatorJobBean();
            XConfiguration conf = null;
            conf = getJobConf();
            XConfiguration.copy(newConf, conf);
            job.setId(getJobId());
            job.setConf(XmlUtils.prettyPrint(conf).toString());
            CoordJobQueryExecutor.getInstance().executeUpdate(
                    CoordJobQueryExecutor.CoordJobQuery.UPDATE_COORD_JOB_CONF, job);
        }

        catch (XException e) {
            throw new CommandException(e);
        }
    }

    /**
     * Update job sla.
     *
     * @param newParams the new params
     * @throws CommandException the command exception
     */
    protected void updateJobSLA(Map<String, String> newParams) throws CommandException {

        try {

            CoordinatorJobBean job = CoordJobQueryExecutor.getInstance().get(
                    CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB_XML, getJobId());

            Element eAction;
            try {
                eAction = XmlUtils.parseXml(job.getJobXml());
            }
            catch (JDOMException e) {
                throw new CommandException(ErrorCode.E1005, e.getMessage(), e);
            }
            Element eSla = eAction.getChild("action", eAction.getNamespace()).getChild("info",
                    eAction.getNamespace("sla"));

            if (newParams != null) {
                if (newParams.get(RestConstants.SLA_NOMINAL_TIME) != null) {
                    updateSlaTagElement(eSla, SLAOperations.NOMINAL_TIME,
                            newParams.get(RestConstants.SLA_NOMINAL_TIME));
                }
                if (newParams.get(RestConstants.SLA_SHOULD_START) != null) {
                    updateSlaTagElement(eSla, SLAOperations.SHOULD_START,
                            newParams.get(RestConstants.SLA_SHOULD_START));
                }
                if (newParams.get(RestConstants.SLA_SHOULD_END) != null) {
                    updateSlaTagElement(eSla, SLAOperations.SHOULD_END, newParams.get(RestConstants.SLA_SHOULD_END));
                }
                if (newParams.get(RestConstants.SLA_MAX_DURATION) != null) {
                    updateSlaTagElement(eSla, SLAOperations.MAX_DURATION,
                            newParams.get(RestConstants.SLA_MAX_DURATION));
                }
            }

            String actualXml = XmlUtils.prettyPrint(eAction).toString();
            job.setJobXml(actualXml);
            job.setId(getJobId());

            CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQueryExecutor.CoordJobQuery.UPDATE_COORD_JOB_XML,
                    job);
        }
        catch (XException e) {
            throw new CommandException(e);
        }

    }

    /**
     * Gets the action and date list as string.
     *
     * @return the action date list as string
     */
    protected String getActionDateListAsString() {
        StringBuffer bf = new StringBuffer();
        if (!StringUtils.isEmpty(dates)) {
            bf.append(dates);
        }

        if (!StringUtils.isEmpty(scope)) {
            if (!StringUtils.isEmpty(bf.toString())) {
                bf.append(",");
            }
            bf.append(scope);
        }

        return bf.toString();

    }

    /**
     * Gets the action list for scope and date.
     *
     * @param id the id
     * @param scope the scope
     * @param dates the dates
     * @return the action list for scope and date
     * @throws CommandException the command exception
     */
    private List<String> getActionListForScopeAndDate(String id, String scope, String dates) throws CommandException {
        List<String> actionIds = new ArrayList<String>();

        if (scope == null && dates == null) {
            return null;
        }
        List<String> parsed = new ArrayList<String>();
        if (dates != null) {
            List<CoordinatorActionBean> actionSet = CoordUtils.getCoordActionsFromDates(id, dates, true);
            for (CoordinatorActionBean action : actionSet) {
                actionIds.add(action.getId());
            }
            parsed.addAll(actionIds);
        }
        if (scope != null) {
            parsed.addAll(CoordUtils.getActionsIds(id, scope));
        }
        return parsed;
    }

    /**
     * Gets the action list.
     *
     * @return the action list
     */
    protected List<String> getActionList() {
        return actionIds;
    }

    protected boolean isJobRequest() {
        return StringUtils.isEmpty(dates) && StringUtils.isEmpty(scope);
    }


    /**
     * Update Sla tag element.
     *
     * @param elem the elem
     * @param tagName the tag name
     * @param value the value
     */
    public void updateSlaTagElement(Element elem, String tagName, String value) {
        if (elem != null && elem.getChild(tagName, elem.getNamespace("sla")) != null) {
            elem.getChild(tagName, elem.getNamespace("sla")).setText(value);
        }
    }

    protected XConfiguration getJobConf() throws JPAExecutorException, CommandException {
        CoordinatorJobBean job = CoordJobQueryExecutor.getInstance().get(
                CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB_CONF, getJobId());
        String jobConf = job.getConf();
        XConfiguration conf = null;
        try {
            conf = new XConfiguration(new StringReader(jobConf));
        }
        catch (IOException e) {
            throw new CommandException(ErrorCode.E1005, e.getMessage(), e);
        }
        return conf;
    }

}
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSLAChangeXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAChangeXCommand.java
new file mode 100644
index 000000000..4d2438876
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSLAChangeXCommand.java
@@ -0,0 +1,100 @@
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.coord.CoordELEvaluator;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.ServiceException;
import org.apache.oozie.service.Services;
import org.apache.oozie.sla.service.SLAService;
import org.apache.oozie.util.ELEvaluator;
import org.apache.oozie.util.Pair;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;

public class CoordSLAChangeXCommand extends CoordSLAAlertsXCommand {

    Map<String, String> newParams;

    public CoordSLAChangeXCommand(String jobId, String actions, String dates, Map<String, String> newParams) {
        super(jobId, "SLA.alerts.change", "SLA.alerts.change", actions, dates);
        this.newParams = newParams;
    }

    @Override
    protected boolean executeSlaCommand() throws ServiceException, CommandException {
        try {
            List<Pair<String, Map<String, String>>> idSlaDefinitionList = new ArrayList<Pair<String, Map<String, String>>>();
            List<CoordinatorActionBean> coordinatorActionBeanList = getNotTerminatedActions();
            Configuration conf = getJobConf();
            for (CoordinatorActionBean coordAction : coordinatorActionBeanList) {
                Map<String, String> slaDefinitionMap = new HashMap<String, String>(newParams);
                for (String key : slaDefinitionMap.keySet()) {
                    Element eAction = XmlUtils.parseXml(coordAction.getActionXml().toString());
                    ELEvaluator evalSla = CoordELEvaluator.createSLAEvaluator(eAction, coordAction, conf);
                    String updateValue = CoordELFunctions.evalAndWrap(evalSla, slaDefinitionMap.get(key));
                    slaDefinitionMap.put(key, updateValue);
                }
                idSlaDefinitionList.add(new Pair<String, Map<String, String>>(coordAction.getId(), slaDefinitionMap));
            }
            return Services.get().get(SLAService.class).changeDefinition(idSlaDefinitionList);
        }
        catch (Exception e) {
            throw new CommandException(ErrorCode.E1027, e.getMessage(), e);
        }

    }

    @Override
    protected void updateJob() throws CommandException {
        if (isJobRequest()) {
            updateJobSLA(newParams);
        }
    }

    private List<CoordinatorActionBean> getNotTerminatedActions() throws JPAExecutorException {
        if (isJobRequest()) {
            return CoordActionQueryExecutor.getInstance().getList(
                    CoordActionQuery.GET_ACTIVE_ACTIONS_JOBID_FOR_SLA_CHANGE, getJobId());
        }
        else {
            return CoordActionQueryExecutor.getInstance().getList(
                    CoordActionQuery.GET_ACTIVE_ACTIONS_IDS_FOR_SLA_CHANGE, getActionList());
        }

    }

    @Override
    protected void verifyPrecondition() throws CommandException, PreconditionException {
        validateSLAChangeParam(newParams);
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
index 4643d7326..90050b3df 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
@@ -20,11 +20,14 @@ package org.apache.oozie.coord;
 
 import java.text.ParseException;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
import java.util.concurrent.TimeUnit;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
@@ -35,15 +38,19 @@ import org.apache.oozie.command.CommandException;
 import org.apache.oozie.executor.jpa.CoordActionGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetActionForNominalTimeJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.XLogService;
import org.apache.oozie.sla.SLAOperations;
 import org.apache.oozie.util.CoordActionsInDateRange;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
 import org.jdom.Element;
 
import com.google.common.annotations.VisibleForTesting;

 public class CoordUtils {
     public static final String HADOOP_USER = "user.name";
 
@@ -92,7 +99,8 @@ public class CoordUtils {
      * @return the list of Coordinator actions for the date range
      * @throws CommandException thrown if failed to get coordinator actions by given date range
      */
    static List<CoordinatorActionBean> getCoordActionsFromDates(String jobId, String scope, boolean active)
    @VisibleForTesting
    public static List<CoordinatorActionBean> getCoordActionsFromDates(String jobId, String scope, boolean active)
             throws CommandException {
         JPAService jpaService = Services.get().get(JPAService.class);
         ParamChecker.notEmpty(jobId, "jobId");
@@ -132,7 +140,12 @@ public class CoordUtils {
                     throw new CommandException(ErrorCode.E0302, s.trim(), e);
                 }
                 catch (JPAExecutorException e) {
                    throw new CommandException(e);
                    if (e.getErrorCode() == ErrorCode.E0605) {
                        XLog.getLog(CoordUtils.class).info("No action for nominal time:" + s + ". Skipping over");
                    }
                    else {
                        throw new CommandException(e);
                    }
                 }
 
             }
@@ -145,16 +158,7 @@ public class CoordUtils {
         return coordActions;
     }
 
    /**
     * Get the list of actions for given id ranges
     *
     * @param jobId coordinator job id
     * @param scope a comma-separated list of action ranges. The action range is specified with two action numbers separated by '-'
     * @return the list of all Coordinator actions for action range
     * @throws CommandException thrown if failed to get coordinator actions by given id range
     */
     public static List<CoordinatorActionBean> getCoordActionsFromIds(String jobId, String scope) throws CommandException {
        JPAService jpaService = Services.get().get(JPAService.class);
    public static Set<String> getActionsIds(String jobId, String scope) throws CommandException {
         ParamChecker.notEmpty(jobId, "jobId");
         ParamChecker.notEmpty(scope, "scope");
 
@@ -202,6 +206,21 @@ public class CoordUtils {
                 actions.add(jobId + "@" + s);
             }
         }
        return actions;
    }

    /**
     * Get the list of actions for given id ranges
     *
     * @param jobId coordinator job id
     * @param scope a comma-separated list of action ranges. The action range is specified with two action numbers separated by '-'
     * @return the list of all Coordinator actions for action range
     * @throws CommandException thrown if failed to get coordinator actions by given id range
     */
     @VisibleForTesting
     public static List<CoordinatorActionBean> getCoordActionsFromIds(String jobId, String scope) throws CommandException {
        JPAService jpaService = Services.get().get(JPAService.class);
        Set<String> actions = getActionsIds(jobId, scope);
         // Retrieve the actions using the corresponding actionIds
         List<CoordinatorActionBean> coordActions = new ArrayList<CoordinatorActionBean>();
         for (String id : actions) {
@@ -225,4 +244,107 @@ public class CoordUtils {
         return coordActions;
     }
 
     /**
      * Check if sla alert is disabled for action.
      * @param actionBean
      * @param coordName
      * @param jobConf
      * @return
      * @throws ParseException
      */
    public static boolean isSlaAlertDisabled(CoordinatorActionBean actionBean, String coordName, Configuration jobConf)
            throws ParseException {

        int disableSlaNotificationOlderThan = jobConf.getInt(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN,
                ConfigurationService.getInt(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN));

        if (disableSlaNotificationOlderThan > 0) {
            // Disable alert for catchup jobs
            long timeDiffinHrs = TimeUnit.MILLISECONDS.toHours(new Date().getTime()
                    - actionBean.getNominalTime().getTime());
            if (timeDiffinHrs > jobConf.getLong(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN,
                    ConfigurationService.getLong(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN))) {
                return true;
            }
        }

        boolean disableAlert = false;
        if (jobConf.get(OozieClient.SLA_DISABLE_ALERT_COORD) != null) {
            String coords = jobConf.get(OozieClient.SLA_DISABLE_ALERT_COORD);
            Set<String> coordsToDisableFor = new HashSet<String>(Arrays.asList(coords.split(",")));
            if (coordsToDisableFor.contains(coordName)) {
                return true;
            }
            if (coordsToDisableFor.contains(actionBean.getJobId())) {
                return true;
            }
        }

        // Check if sla alert is disabled for that action
        if (!StringUtils.isEmpty(jobConf.get(OozieClient.SLA_DISABLE_ALERT))
                && getCoordActionSLAAlertStatus(actionBean, coordName, jobConf, OozieClient.SLA_DISABLE_ALERT)) {
            return true;
        }

        // Check if sla alert is enabled for that action
        if (!StringUtils.isEmpty(jobConf.get(OozieClient.SLA_ENABLE_ALERT))
                && getCoordActionSLAAlertStatus(actionBean, coordName, jobConf, OozieClient.SLA_ENABLE_ALERT)) {
            return false;
        }

        return disableAlert;
    }

    /**
     * Get coord action SLA alert status.
     * @param actionBean
     * @param coordName
     * @param jobConf
     * @param slaAlertType
     * @return
     * @throws ParseException
     */
    private static boolean getCoordActionSLAAlertStatus(CoordinatorActionBean actionBean, String coordName,
            Configuration jobConf, String slaAlertType) throws ParseException {
        String slaAlertList;

       if (!StringUtils.isEmpty(jobConf.get(slaAlertType))) {
            slaAlertList = jobConf.get(slaAlertType);
            // check if ALL or date/action-num range
            if (slaAlertList.equalsIgnoreCase(SLAOperations.ALL_VALUE)) {
                return true;
            }
            String[] values = slaAlertList.split(",");
            for (String value : values) {
                value = value.trim();
                if (value.contains("::")) {
                    String[] datesInRange = value.split("::");
                    Date start = DateUtils.parseDateOozieTZ(datesInRange[0].trim());
                    Date end = DateUtils.parseDateOozieTZ(datesInRange[1].trim());
                    // check if nominal time in this range
                    if (actionBean.getNominalTime().compareTo(start) >= 0
                            || actionBean.getNominalTime().compareTo(end) <= 0) {
                        return true;
                    }
                }
                else if (value.contains("-")) {
                    String[] actionsInRange = value.split("-");
                    int start = Integer.parseInt(actionsInRange[0].trim());
                    int end = Integer.parseInt(actionsInRange[1].trim());
                    // check if action number in this range
                    if (actionBean.getActionNumber() >= start || actionBean.getActionNumber() <= end) {
                        return true;
                    }
                }
                else {
                    int actionNumber = Integer.parseInt(value.trim());
                    if (actionBean.getActionNumber() == actionNumber) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionQueryExecutor.java
index e6ab09b87..c6a60a10f 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordActionQueryExecutor.java
@@ -28,6 +28,7 @@ import javax.persistence.Query;
 
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.StringBlob;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 
@@ -51,7 +52,12 @@ public class CoordActionQueryExecutor extends
         GET_COORD_ACTIVE_ACTIONS_COUNT_BY_JOBID,
         GET_COORD_ACTIONS_BY_LAST_MODIFIED_TIME,
         GET_COORD_ACTIONS_STATUS_UNIGNORED,
        GET_COORD_ACTIONS_PENDING_COUNT
        GET_COORD_ACTIONS_PENDING_COUNT,
        GET_ACTIVE_ACTIONS_IDS_FOR_SLA_CHANGE,
        GET_ACTIVE_ACTIONS_JOBID_FOR_SLA_CHANGE,
        GET_TERMINATED_ACTIONS_FOR_DATES,
        GET_TERMINATED_ACTION_IDS_FOR_DATES,
        GET_ACTIVE_ACTIONS_FOR_DATES
     };
 
     private static CoordActionQueryExecutor instance = new CoordActionQueryExecutor();
@@ -180,6 +186,19 @@ public class CoordActionQueryExecutor extends
             case GET_COORD_ACTIONS_PENDING_COUNT:
                 query.setParameter("jobId", parameters[0]);
                 break;
            case GET_ACTIVE_ACTIONS_IDS_FOR_SLA_CHANGE:
            query.setParameter("ids", parameters[0]);
            break;
            case GET_ACTIVE_ACTIONS_JOBID_FOR_SLA_CHANGE:
            query.setParameter("jobId", parameters[0]);
            break;
            case GET_TERMINATED_ACTIONS_FOR_DATES:
            case GET_TERMINATED_ACTION_IDS_FOR_DATES:
            case GET_ACTIVE_ACTIONS_FOR_DATES:
                query.setParameter("jobId", parameters[0]);
                query.setParameter("startTime", new Timestamp(((Date) parameters[1]).getTime()));
                query.setParameter("endTime", new Timestamp(((Date) parameters[2]).getTime()));
                break;
 
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
@@ -247,6 +266,33 @@ public class CoordActionQueryExecutor extends
                 bean.setStatusStr((String)arr[0]);
                 bean.setPending((Integer)arr[1]);
                 break;
            case GET_ACTIVE_ACTIONS_IDS_FOR_SLA_CHANGE:
            case GET_ACTIVE_ACTIONS_JOBID_FOR_SLA_CHANGE:
                arr = (Object[]) ret;
                bean = new CoordinatorActionBean();
                bean.setId((String)arr[0]);
                bean.setNominalTime((Timestamp)arr[1]);
                bean.setCreatedTime((Timestamp)arr[2]);
                bean.setActionXmlBlob((StringBlob)arr[3]);
                break;
            case GET_TERMINATED_ACTIONS_FOR_DATES:
                bean = (CoordinatorActionBean) ret;
                break;
            case GET_TERMINATED_ACTION_IDS_FOR_DATES:
                bean = new CoordinatorActionBean();
                bean.setId((String) ret);
                break;
            case GET_ACTIVE_ACTIONS_FOR_DATES:
                arr = (Object[]) ret;
                bean = new CoordinatorActionBean();
                bean.setId((String)arr[0]);
                bean.setJobId((String)arr[1]);
                bean.setStatusStr((String) arr[2]);
                bean.setExternalId((String) arr[3]);
                bean.setPending((Integer) arr[4]);
                bean.setNominalTime((Timestamp) arr[5]);
                bean.setCreatedTime((Timestamp) arr[6]);
                break;
 
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct action bean for "
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionIdsForDateRangeJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionIdsForDateRangeJPAExecutor.java
deleted file mode 100644
index 1862c7c68..000000000
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionIdsForDateRangeJPAExecutor.java
++ /dev/null
@@ -1,69 +0,0 @@
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

package org.apache.oozie.executor.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.util.ParamChecker;

/**
 * Load coordinator action ids by date range.
 */
public class CoordJobGetActionIdsForDateRangeJPAExecutor implements JPAExecutor<List<String>> {

    private String jobId = null;
    private Date startDate, endDate;

    public CoordJobGetActionIdsForDateRangeJPAExecutor(String jobId, Date startDate, Date endDate) {
        ParamChecker.notNull(jobId, "jobId");
        this.jobId = jobId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String getName() {
        return "CoordJobGetActionIdsForDateRangeJPAExecutor";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> execute(EntityManager em) throws JPAExecutorException {
        try {
            Query q = em.createNamedQuery("GET_ACTION_IDS_FOR_DATES");
            q.setParameter("jobId", jobId);
            q.setParameter("startTime", new Timestamp(startDate.getTime()));
            q.setParameter("endTime", new Timestamp(endDate.getTime()));
            List<String> coordActionIds= q.getResultList();
            return coordActionIds;
        }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0603, e.getMessage(), e);
        }
    }

}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsByDatesForKillJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsByDatesForKillJPAExecutor.java
deleted file mode 100644
index eb9559102..000000000
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsByDatesForKillJPAExecutor.java
++ /dev/null
@@ -1,108 +0,0 @@
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

package org.apache.oozie.executor.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.ParamChecker;

/**
 * Load non-terminal coordinator actions by dates.
 */
public class CoordJobGetActionsByDatesForKillJPAExecutor implements JPAExecutor<List<CoordinatorActionBean>> {

    private String jobId = null;
    private Date startDate, endDate;

    public CoordJobGetActionsByDatesForKillJPAExecutor(String jobId, Date startDate, Date endDate) {
        ParamChecker.notNull(jobId, "jobId");
        this.jobId = jobId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String getName() {
        return "CoordJobGetActionsByDatesForKillJPAExecutor";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CoordinatorActionBean> execute(EntityManager em) throws JPAExecutorException {
        List<CoordinatorActionBean> actionList = new ArrayList<CoordinatorActionBean>();
        try {
            Query q = em.createNamedQuery("GET_ACTIONS_BY_DATES_FOR_KILL");
            q.setParameter("jobId", jobId);
            q.setParameter("startTime", new Timestamp(startDate.getTime()));
            q.setParameter("endTime", new Timestamp(endDate.getTime()));
            List<Object[]> actions = q.getResultList();

            for (Object[] a : actions) {
                CoordinatorActionBean aa = getBeanForRunningCoordAction(a);
                actionList.add(aa);
            }
            return actionList;
        }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0603, e.getMessage(), e);
        }
    }

    private CoordinatorActionBean getBeanForRunningCoordAction(Object[] arr) {
        CoordinatorActionBean action = new CoordinatorActionBean();
        if (arr[0] != null) {
            action.setId((String) arr[0]);
        }

        if (arr[1] != null) {
            action.setJobId((String) arr[1]);
        }

        if (arr[2] != null) {
            action.setStatus(CoordinatorAction.Status.valueOf((String) arr[2]));
        }

        if (arr[3] != null) {
            action.setExternalId((String) arr[3]);
        }

        if (arr[4] != null) {
            action.setPending((Integer) arr[4]);
        }

        if (arr[5] != null) {
            action.setNominalTime(DateUtils.toDate((Timestamp) arr[5]));
        }

        if (arr[6] != null) {
            action.setCreatedTime(DateUtils.toDate((Timestamp) arr[6]));
        }
        return action;
    }
}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsForDatesJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsForDatesJPAExecutor.java
deleted file mode 100644
index d1856ae45..000000000
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobGetActionsForDatesJPAExecutor.java
++ /dev/null
@@ -1,70 +0,0 @@
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

package org.apache.oozie.executor.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.util.ParamChecker;

/**
 * Load coordinator actions by dates.
 */
public class CoordJobGetActionsForDatesJPAExecutor implements JPAExecutor<List<CoordinatorActionBean>> {

    private String jobId = null;
    private Date startDate, endDate;

    public CoordJobGetActionsForDatesJPAExecutor(String jobId, Date startDate, Date endDate) {
        ParamChecker.notNull(jobId, "jobId");
        this.jobId = jobId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String getName() {
        return "CoordJobGetActionsForDatesJPAExecutor";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CoordinatorActionBean> execute(EntityManager em) throws JPAExecutorException {
        List<CoordinatorActionBean> actions;
        try {
            Query q = em.createNamedQuery("GET_ACTIONS_FOR_DATES");
            q.setParameter("jobId", jobId);
            q.setParameter("startTime", new Timestamp(startDate.getTime()));
            q.setParameter("endTime", new Timestamp(endDate.getTime()));
            actions = q.getResultList();
            return actions;
        }
        catch (Exception e) {
            throw new JPAExecutorException(ErrorCode.E0603, e.getMessage(), e);
        }
    }

}
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
index 4bccef45d..15186863d 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobQueryExecutor.java
@@ -26,7 +26,6 @@ import java.util.List;
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.StringBlob;
@@ -53,6 +52,8 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
         UPDATE_COORD_JOB_STATUS_PENDING_TIME,
         UPDATE_COORD_JOB_MATERIALIZE,
         UPDATE_COORD_JOB_CHANGE,
        UPDATE_COORD_JOB_CONF,
        UPDATE_COORD_JOB_XML,
         GET_COORD_JOB,
         GET_COORD_JOB_USER_APPNAME,
         GET_COORD_JOB_INPUT_CHECK,
@@ -63,9 +64,13 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
         GET_COORD_JOB_STATUS,
         GET_COORD_JOB_STATUS_PARENTID,
         GET_COORD_JOBS_CHANGED,
        GET_COORD_JOBS_OLDER_FOR_MATERILZATION,
        GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION,
         GET_COORD_FOR_ABANDONEDCHECK,
        GET_COORD_IDS_FOR_STATUS_TRANSIT
        GET_COORD_IDS_FOR_STATUS_TRANSIT,
        GET_COORD_JOBS_FOR_BUNDLE_BY_APPNAME_ID,
        GET_COORD_JOBS_WITH_PARENT_ID,
        GET_COORD_JOB_CONF,
        GET_COORD_JOB_XML
     };
 
     private static CoordJobQueryExecutor instance = new CoordJobQueryExecutor();
@@ -177,6 +182,15 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
                 query.setParameter("lastModifiedTime", cjBean.getLastModifiedTimestamp());
                 query.setParameter("id", cjBean.getId());
                 break;
            case UPDATE_COORD_JOB_CONF:
                query.setParameter("conf", cjBean.getConfBlob());
                query.setParameter("id", cjBean.getId());
                break;
            case UPDATE_COORD_JOB_XML:
                query.setParameter("jobXml", cjBean.getJobXmlBlob());
                query.setParameter("id", cjBean.getId());
                break;

             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -198,12 +212,14 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
             case GET_COORD_JOB_SUSPEND_KILL:
             case GET_COORD_JOB_STATUS:
             case GET_COORD_JOB_STATUS_PARENTID:
            case GET_COORD_JOB_CONF:
            case GET_COORD_JOB_XML:
                 query.setParameter("id", parameters[0]);
                 break;
             case GET_COORD_JOBS_CHANGED:
                 query.setParameter("lastModifiedTime", new Timestamp(((Date)parameters[0]).getTime()));
                 break;
            case GET_COORD_JOBS_OLDER_FOR_MATERILZATION:
            case GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION:
                 query.setParameter("matTime", new Timestamp(((Date)parameters[0]).getTime()));
                 int limit = (Integer) parameters[1];
                 if (limit > 0) {
@@ -218,7 +234,13 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
             case GET_COORD_IDS_FOR_STATUS_TRANSIT:
                 query.setParameter("lastModifiedTime", new Timestamp(((Date) parameters[0]).getTime()));
                 break;

            case GET_COORD_JOBS_FOR_BUNDLE_BY_APPNAME_ID:
                query.setParameter("appName", parameters[0]);
                query.setParameter("bundleId", parameters[1]);
                break;
            case GET_COORD_JOBS_WITH_PARENT_ID:
                query.setParameter("parentId", parameters[0]);
                break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -335,7 +357,15 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
             case GET_COORD_JOBS_CHANGED:
                 bean = (CoordinatorJobBean) ret;
                 break;
            case GET_COORD_JOBS_OLDER_FOR_MATERILZATION:
            case GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION:
                bean = new CoordinatorJobBean();
                bean.setId((String) ret);
                break;
            case GET_COORD_JOBS_FOR_BUNDLE_BY_APPNAME_ID:
                bean = new CoordinatorJobBean();
                bean.setId((String) ret);
                break;
            case GET_COORD_JOBS_WITH_PARENT_ID:
                 bean = new CoordinatorJobBean();
                 bean.setId((String) ret);
                 break;
@@ -347,11 +377,18 @@ public class CoordJobQueryExecutor extends QueryExecutor<CoordinatorJobBean, Coo
                 bean.setGroup((String) arr[2]);
                 bean.setAppName((String) arr[3]);
                 break;

             case GET_COORD_IDS_FOR_STATUS_TRANSIT:
                 bean = new CoordinatorJobBean();
                 bean.setId((String) ret);
                 break;
            case GET_COORD_JOB_CONF:
                bean = new CoordinatorJobBean();
                bean.setConfBlob((StringBlob) ret);
                break;
            case GET_COORD_JOB_XML:
                bean = new CoordinatorJobBean();
                bean.setJobXmlBlob((StringBlob) ret);
                break;
 
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct job bean for "
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobsToBeMaterializedJPAExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobsToBeMaterializedJPAExecutor.java
index 5e018c724..6d13ed178 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobsToBeMaterializedJPAExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/CoordJobsToBeMaterializedJPAExecutor.java
@@ -56,7 +56,7 @@ public class CoordJobsToBeMaterializedJPAExecutor implements JPAExecutor<List<Co
     public List<CoordinatorJobBean> execute(EntityManager em) throws JPAExecutorException {
         List<CoordinatorJobBean> cjBeans;
         try {
            Query q = em.createNamedQuery("GET_COORD_JOBS_OLDER_FOR_MATERILZATION");
            Query q = em.createNamedQuery("GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION");
             q.setParameter("matTime", new Timestamp(this.dateInput.getTime()));
             if (limit > 0) {
                 q.setMaxResults(limit);
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/SLARegistrationQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/SLARegistrationQueryExecutor.java
index e220c019f..bded63464 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/SLARegistrationQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/SLARegistrationQueryExecutor.java
@@ -18,6 +18,8 @@
 
 package org.apache.oozie.executor.jpa;
 
import java.sql.Timestamp;
import java.util.ArrayList;
 import java.util.List;
 
 import javax.persistence.EntityManager;
@@ -36,8 +38,13 @@ public class SLARegistrationQueryExecutor extends QueryExecutor<SLARegistrationB
 
     public enum SLARegQuery {
         UPDATE_SLA_REG_ALL,
        UPDATE_SLA_CONFIG,
        UPDATE_SLA_EXPECTED_VALUE,
         GET_SLA_REG_ALL,
        GET_SLA_REG_ON_RESTART
        GET_SLA_EXPECTED_VALUE_CONFIG,
        GET_SLA_REG_FOR_PARENT_ID,
        GET_SLA_REG_ON_RESTART,
        GET_SLA_CONFIGS
     };
 
     private static SLARegistrationQueryExecutor instance = new SLARegistrationQueryExecutor();
@@ -70,6 +77,17 @@ public class SLARegistrationQueryExecutor extends QueryExecutor<SLARegistrationB
                 query.setParameter("parentId", bean.getParentId());
                 query.setParameter("jobData", bean.getJobData());
                 break;
            case UPDATE_SLA_EXPECTED_VALUE:
                query.setParameter("jobId", bean.getId());
                query.setParameter("expectedStartTime", bean.getExpectedStartTimestamp());
                query.setParameter("expectedEndTime", bean.getExpectedEndTimestamp());
                query.setParameter("expectedDuration", bean.getExpectedDuration());
                break;
            case UPDATE_SLA_CONFIG:
                query.setParameter("jobId", bean.getId());
                query.setParameter("slaConfig", bean.getSlaConfig());
                break;

             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -86,6 +104,16 @@ public class SLARegistrationQueryExecutor extends QueryExecutor<SLARegistrationB
             case GET_SLA_REG_ON_RESTART:
                 query.setParameter("id", parameters[0]);
                 break;
            case GET_SLA_CONFIGS:
                query.setParameter("ids", parameters[0]);
                break;
            case GET_SLA_EXPECTED_VALUE_CONFIG:
                query.setParameter("id", parameters[0]);
                break;
            case GET_SLA_REG_FOR_PARENT_ID:
                query.setParameter("parentId", parameters[0]);
                break;

             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -120,9 +148,13 @@ public class SLARegistrationQueryExecutor extends QueryExecutor<SLARegistrationB
         JPAService jpaService = Services.get().get(JPAService.class);
         EntityManager em = jpaService.getEntityManager();
         Query query = getSelectQuery(namedQuery, em, parameters);
        @SuppressWarnings("unchecked")
        List<SLARegistrationBean> beanList = (List<SLARegistrationBean>) jpaService.executeGetList(namedQuery.name(),
                query, em);
        List<?> retList = (List<?>) jpaService.executeGetList(namedQuery.name(), query, em);
        List<SLARegistrationBean> beanList = new ArrayList<SLARegistrationBean>();
        if (retList != null) {
            for (Object ret : retList) {
                beanList.add(constructBean(namedQuery, ret));
            }
        }
         return beanList;
     }
 
@@ -145,6 +177,28 @@ public class SLARegistrationQueryExecutor extends QueryExecutor<SLARegistrationB
                 bean.setSlaConfig((String) arr[2]);
                 bean.setJobData((String) arr[3]);
                 break;
            case GET_SLA_CONFIGS:
                bean = new SLARegistrationBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setSlaConfig((String) arr[1]);
                break;
            case GET_SLA_EXPECTED_VALUE_CONFIG:
                bean = new SLARegistrationBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setSlaConfig((String) arr[1]);
                bean.setExpectedStart((Timestamp)arr[2]);
                bean.setExpectedEnd((Timestamp)arr[3]);
                bean.setExpectedDuration((Long)arr[4]);
                bean.setNominalTime((Timestamp)arr[5]);
                break;
            case GET_SLA_REG_FOR_PARENT_ID:
                bean = new SLARegistrationBean();
                arr = (Object[]) ret;
                bean.setId((String) arr[0]);
                bean.setSlaConfig((String) arr[1]);
                break;
             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct job bean for "
                         + namedQuery.name());
diff --git a/core/src/main/java/org/apache/oozie/executor/jpa/SLASummaryQueryExecutor.java b/core/src/main/java/org/apache/oozie/executor/jpa/SLASummaryQueryExecutor.java
index c3197b764..0057c89e7 100644
-- a/core/src/main/java/org/apache/oozie/executor/jpa/SLASummaryQueryExecutor.java
++ b/core/src/main/java/org/apache/oozie/executor/jpa/SLASummaryQueryExecutor.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.executor.jpa;
 
import java.sql.Timestamp;
 import java.util.List;
 
 import javax.persistence.EntityManager;
@@ -39,8 +40,11 @@ public class SLASummaryQueryExecutor extends QueryExecutor<SLASummaryBean, SLASu
         UPDATE_SLA_SUMMARY_FOR_ACTUAL_TIMES,
         UPDATE_SLA_SUMMARY_ALL,
         UPDATE_SLA_SUMMARY_EVENTPROCESSED,
        UPDATE_SLA_SUMMARY_FOR_EXPECTED_TIMES,
        UPDATE_SLA_SUMMARY_LAST_MODIFIED_TIME,
         GET_SLA_SUMMARY,
        GET_SLA_SUMMARY_EVENTPROCESSED
        GET_SLA_SUMMARY_EVENTPROCESSED,
        GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED
     };
 
     private static SLASummaryQueryExecutor instance = new SLASummaryQueryExecutor();
@@ -95,10 +99,24 @@ public class SLASummaryQueryExecutor extends QueryExecutor<SLASummaryBean, SLASu
                 query.setParameter("actualStartTS", bean.getActualStartTimestamp());
                 query.setParameter("jobId", bean.getId());
                 break;
            case UPDATE_SLA_SUMMARY_FOR_EXPECTED_TIMES:
                query.setParameter("nominalTime", bean.getNominalTimestamp());
                query.setParameter("expectedStartTime", bean.getExpectedStartTimestamp());
                query.setParameter("expectedEndTime", bean.getExpectedEndTimestamp());
                query.setParameter("expectedDuration", bean.getExpectedDuration());
                query.setParameter("lastModTime", bean.getLastModifiedTimestamp());
                query.setParameter("jobId", bean.getId());
                break;

             case UPDATE_SLA_SUMMARY_EVENTPROCESSED:
                 query.setParameter("eventProcessed", bean.getEventProcessed());
                 query.setParameter("jobId", bean.getId());
                 break;
            case UPDATE_SLA_SUMMARY_LAST_MODIFIED_TIME:
                query.setParameter("lastModifiedTS", bean.getLastModifiedTime());
                query.setParameter("jobId", bean.getId());
                break;

             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot set parameters for "
                         + namedQuery.name());
@@ -113,6 +131,7 @@ public class SLASummaryQueryExecutor extends QueryExecutor<SLASummaryBean, SLASu
         switch (namedQuery) {
             case GET_SLA_SUMMARY:
             case GET_SLA_SUMMARY_EVENTPROCESSED:
            case GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED:
                 query.setParameter("id", parameters[0]);
                 break;
         }
@@ -174,6 +193,14 @@ public class SLASummaryQueryExecutor extends QueryExecutor<SLASummaryBean, SLASu
                 bean = new SLASummaryBean();
                 bean.setEventProcessed(((Byte)ret).intValue());
                 break;
            case GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED:
                Object[] arr = (Object[]) ret;
                bean = new SLASummaryBean();
                bean.setEventProcessed((Byte)arr[0]);
                bean.setLastModifiedTime((Timestamp)arr[1]);

                break;

             default:
                 throw new JPAExecutorException(ErrorCode.E0603, "QueryExecutor cannot construct job bean for "
                         + namedQuery.name());
diff --git a/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java b/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
index fa16d1d2a..1cbd47492 100644
-- a/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
++ b/core/src/main/java/org/apache/oozie/service/CoordMaterializeTriggerService.java
@@ -160,7 +160,7 @@ public class CoordMaterializeTriggerService implements Service {
                 throws JPAExecutorException {
             try {
                 List<CoordinatorJobBean> materializeJobs = CoordJobQueryExecutor.getInstance().getList(
                        CoordJobQuery.GET_COORD_JOBS_OLDER_FOR_MATERILZATION, currDate, limit);
                        CoordJobQuery.GET_COORD_JOBS_OLDER_FOR_MATERIALIZATION, currDate, limit);
                 LOG.info("CoordMaterializeTriggerService - Curr Date= " + DateUtils.formatDateOozieTZ(currDate)
                         + ", Num jobs to materialize = " + materializeJobs.size());
                 for (CoordinatorJobBean coordJob : materializeJobs) {
diff --git a/core/src/main/java/org/apache/oozie/service/EventHandlerService.java b/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
index 7c0d3bef4..22c6fb096 100644
-- a/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
++ b/core/src/main/java/org/apache/oozie/service/EventHandlerService.java
@@ -19,32 +19,32 @@
 package org.apache.oozie.service;
 
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.event.BundleJobEvent;
import org.apache.oozie.event.CoordinatorActionEvent;
import org.apache.oozie.event.CoordinatorJobEvent;
 import org.apache.oozie.client.event.Event;
 import org.apache.oozie.client.event.Event.MessageType;
 import org.apache.oozie.client.event.JobEvent;
import org.apache.oozie.client.event.SLAEvent;
import org.apache.oozie.event.BundleJobEvent;
import org.apache.oozie.event.CoordinatorActionEvent;
import org.apache.oozie.event.CoordinatorJobEvent;
 import org.apache.oozie.event.EventQueue;
 import org.apache.oozie.event.MemoryEventQueue;
 import org.apache.oozie.event.WorkflowActionEvent;
 import org.apache.oozie.event.WorkflowJobEvent;
 import org.apache.oozie.event.listener.JobEventListener;
 import org.apache.oozie.sla.listener.SLAEventListener;
import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.XLog;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /**
  * Service class that handles the events system - creating events queue,
  * managing configured properties and managing and invoking various event
diff --git a/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java b/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
index 569078747..a581f8b1f 100644
-- a/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/BaseJobServlet.java
@@ -177,6 +177,27 @@ public abstract class BaseJobServlet extends JsonRestServlet {
             startCron();
             sendJsonResponse(response, HttpServletResponse.SC_OK, json);
         }
        else if (action.equals(RestConstants.SLA_ENABLE_ALERT)) {
            validateContentType(request, RestConstants.XML_CONTENT_TYPE);
            stopCron();
            slaEnableAlert(request, response);
            startCron();
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else if (action.equals(RestConstants.SLA_DISABLE_ALERT)) {
            validateContentType(request, RestConstants.XML_CONTENT_TYPE);
            stopCron();
            slaDisableAlert(request, response);
            startCron();
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else if (action.equals(RestConstants.SLA_CHANGE)) {
            validateContentType(request, RestConstants.XML_CONTENT_TYPE);
            stopCron();
            slaChange(request, response);
            startCron();
            response.setStatus(HttpServletResponse.SC_OK);
        }
         else {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0303,
                     RestConstants.ACTION_PARAM, action);
@@ -498,4 +519,38 @@ public abstract class BaseJobServlet extends JsonRestServlet {
      */
     abstract String getJobStatus(HttpServletRequest request, HttpServletResponse response)
             throws XServletException, IOException;

    /**
     * Abstract method to enable SLA alert.
     *
     * @param request the request
     * @param response the response
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract void slaEnableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException;

    /**
     * Abstract method to disable SLA alert.
     *
     * @param request the request
     * @param response the response
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract void slaDisableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException;

    /**
     * Abstract method to change SLA definition.
     *
     * @param request the request
     * @param response the response
     * @throws XServletException the x servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    abstract void slaChange(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException;

 }
\ No newline at end of file
diff --git a/core/src/main/java/org/apache/oozie/servlet/SLAServlet.java b/core/src/main/java/org/apache/oozie/servlet/SLAServlet.java
index 2578e41f2..f897652cd 100644
-- a/core/src/main/java/org/apache/oozie/servlet/SLAServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/SLAServlet.java
@@ -31,6 +31,7 @@ import java.util.StringTokenizer;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;

 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.SLAEventBean;
 import org.apache.oozie.client.OozieClient;
diff --git a/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
index 3e186f98d..3cb916883 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V0JobServlet.java
@@ -237,6 +237,22 @@ public class V0JobServlet extends BaseJobServlet {
     @Override
     protected String getJobStatus(HttpServletRequest request, HttpServletResponse response) throws XServletException,
             IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
    }

    @Override
    void slaEnableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException, IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
    }

    @Override
    void slaDisableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
    }

    @Override
    void slaChange(HttpServletRequest request, HttpServletResponse response) throws XServletException, IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v0");
     }
 }
diff --git a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
index 64b97c26b..d4564c671 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V1JobServlet.java
@@ -1103,4 +1103,20 @@ public class V1JobServlet extends BaseJobServlet {
             IOException {
         throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
     }

    @Override
    void slaEnableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException, IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
    }

    @Override
    void slaDisableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
    }

    @Override
    void slaChange(HttpServletRequest request, HttpServletResponse response) throws XServletException, IOException {
        throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ErrorCode.E0302, "Not supported in v1");
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
index 5238426db..7100c98ef 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V2JobServlet.java
@@ -149,6 +149,53 @@ public class V2JobServlet extends V1JobServlet {
 
     }
 
    @Override
    protected void slaEnableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        String jobId = getResourceName(request);
        String actions = request.getParameter(RestConstants.JOB_COORD_SCOPE_ACTION_LIST);
        String dates = request.getParameter(RestConstants.JOB_COORD_SCOPE_DATE);
        String childIds = request.getParameter(RestConstants.COORDINATORS_PARAM);
        try {
            getBaseEngine(jobId, getUser(request)).enableSLAAlert(jobId, actions, dates, childIds);
        }
        catch (BaseEngineException e) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, e);
        }

    }

    @Override
    protected void slaDisableAlert(HttpServletRequest request, HttpServletResponse response) throws XServletException,
            IOException {
        String jobId = getResourceName(request);
        String actions = request.getParameter(RestConstants.JOB_COORD_SCOPE_ACTION_LIST);
        String dates = request.getParameter(RestConstants.JOB_COORD_SCOPE_DATE);
        String childIds = request.getParameter(RestConstants.COORDINATORS_PARAM);
        try {
            getBaseEngine(jobId, getUser(request)).disableSLAAlert(jobId, actions, dates, childIds);
        }
        catch (BaseEngineException e) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, e);
        }
    }

    @Override
    protected void slaChange(HttpServletRequest request, HttpServletResponse response) throws XServletException, IOException {
        String jobId = getResourceName(request);
        String actions = request.getParameter(RestConstants.JOB_COORD_SCOPE_ACTION_LIST);
        String dates = request.getParameter(RestConstants.JOB_COORD_SCOPE_DATE);
        String newParams = request.getParameter(RestConstants.JOB_CHANGE_VALUE);
        String coords = request.getParameter(RestConstants.COORDINATORS_PARAM);

        try {
            getBaseEngine(jobId, getUser(request)).changeSLA(jobId, actions, dates, coords, newParams);
        }
        catch (BaseEngineException e) {
            throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, e);
        }
    }

     /**
      * Ignore a coordinator job/action
      *
@@ -199,21 +246,18 @@ public class V2JobServlet extends V1JobServlet {
         String status;
         String jobId = getResourceName(request);
         try {
            if (jobId.endsWith("-B")) {
                BundleEngine engine = Services.get().get(BundleEngineService.class).getBundleEngine(getUser(request));
                status = engine.getJobStatus(jobId);
            } else if (jobId.endsWith("-W")) {
                DagEngine engine = Services.get().get(DagEngineService.class).getDagEngine(getUser(request));
                status = engine.getJobStatus(jobId);
            } else {
                CoordinatorEngine engine =
                        Services.get().get(CoordinatorEngineService.class).getCoordinatorEngine(getUser(request));
                if (jobId.contains("-C@")) {
                    status = engine.getActionStatus(jobId);
                } else {
                    status = engine.getJobStatus(jobId);
                }
            if (jobId.endsWith("-B") || jobId.endsWith("-W")) {
                status = getBaseEngine(jobId, getUser(request)).getJobStatus(jobId);
            }
            else if (jobId.contains("C@")) {
                CoordinatorEngine engine = Services.get().get(CoordinatorEngineService.class)
                        .getCoordinatorEngine(getUser(request));
                status = engine.getActionStatus(jobId);
             }
            else {
                status = getBaseEngine(jobId, getUser(request)).getJobStatus(jobId);
            }

         } catch (BaseEngineException ex) {
             throw new XServletException(HttpServletResponse.SC_BAD_REQUEST, ex);
         }
@@ -251,7 +295,7 @@ public class V2JobServlet extends V1JobServlet {
         else if (jobId.endsWith("-B")) {
             return Services.get().get(BundleEngineService.class).getBundleEngine(user);
         }
        else if (jobId.endsWith("-C")) {
        else if (jobId.contains("-C")) {
             return Services.get().get(CoordinatorEngineService.class).getCoordinatorEngine(user);
         }
         else {
diff --git a/core/src/main/java/org/apache/oozie/servlet/V2SLAServlet.java b/core/src/main/java/org/apache/oozie/servlet/V2SLAServlet.java
index a0fe1b638..57170e1ef 100644
-- a/core/src/main/java/org/apache/oozie/servlet/V2SLAServlet.java
++ b/core/src/main/java/org/apache/oozie/servlet/V2SLAServlet.java
@@ -22,7 +22,9 @@ import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.URLDecoder;
 import java.text.ParseException;
import java.util.ArrayList;
 import java.util.Arrays;
import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
@@ -31,15 +33,19 @@ import java.util.Set;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;

 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.XException;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor;
import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor.SLARegQuery;
 import org.apache.oozie.executor.jpa.sla.SLASummaryGetForFilterJPAExecutor;
 import org.apache.oozie.executor.jpa.sla.SLASummaryGetForFilterJPAExecutor.SLASummaryFilter;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.sla.SLARegistrationBean;
 import org.apache.oozie.sla.SLASummaryBean;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.XLog;
@@ -146,7 +152,20 @@ public class V2SLAServlet extends SLAServlet {
             else {
                 XLog.getLog(getClass()).error(ErrorCode.E0610);
             }
            return SLASummaryBean.toJSONObject(slaSummaryList, timeZoneId);

            List<String> jobIds = new ArrayList<String>();
            for(SLASummaryBean summaryBean:slaSummaryList){
                jobIds.add(summaryBean.getId());
            }
            List<SLARegistrationBean> SLARegistrationList = SLARegistrationQueryExecutor.getInstance().getList(
                    SLARegQuery.GET_SLA_CONFIGS, jobIds);

            Map<String, Map<String, String>> jobIdSLAConfigMap = new HashMap<String, Map<String, String>>();
            for(SLARegistrationBean registrationBean:SLARegistrationList){
                jobIdSLAConfigMap.put(registrationBean.getId(), registrationBean.getSLAConfigMap());
            }

            return SLASummaryBean.toJSONObject(slaSummaryList, jobIdSLAConfigMap, timeZoneId);
         }
         catch (XException ex) {
             throw new CommandException(ex);
diff --git a/core/src/main/java/org/apache/oozie/sla/SLACalcStatus.java b/core/src/main/java/org/apache/oozie/sla/SLACalcStatus.java
index 189d5ea1f..0d7123a71 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLACalcStatus.java
++ b/core/src/main/java/org/apache/oozie/sla/SLACalcStatus.java
@@ -20,8 +20,10 @@
 package org.apache.oozie.sla;
 
 import java.util.Date;
import java.util.Map;
 
 import org.apache.oozie.AppType;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.service.JobsConcurrencyService;
@@ -65,6 +67,10 @@ public class SLACalcStatus extends SLAEvent {
         reg.setAlertContact(regBean.getAlertContact());
         reg.setAlertEvents(regBean.getAlertEvents());
         reg.setJobData(regBean.getJobData());
        if (regBean.getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT)) {
            reg.addToSLAConfigMap(OozieClient.SLA_DISABLE_ALERT,
                    regBean.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT));
        }
         reg.setId(summary.getId());
         reg.setAppType(summary.getAppType());
         reg.setUser(summary.getUser());
@@ -267,10 +273,14 @@ public class SLACalcStatus extends SLAEvent {
     }
 
     @Override
    public String getSlaConfig() {
    public String getSLAConfig() {
         return regBean.getSlaConfig();
     }
 
    public Map<String, String> getSLAConfigMap() {
        return regBean.getSLAConfigMap();
    }

     @Override
     public MessageType getMsgType() {
         return regBean.getMsgType();
diff --git a/core/src/main/java/org/apache/oozie/sla/SLACalculator.java b/core/src/main/java/org/apache/oozie/sla/SLACalculator.java
index 20f93b5c5..f2383215f 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLACalculator.java
++ b/core/src/main/java/org/apache/oozie/sla/SLACalculator.java
@@ -20,11 +20,14 @@ package org.apache.oozie.sla;
 
 import java.util.Date;
 import java.util.Iterator;
import java.util.List;
import java.util.Map;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.client.event.JobEvent.EventStatus;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.ServiceException;
import org.apache.oozie.util.Pair;
 
 public interface SLACalculator {
 
@@ -51,4 +54,55 @@ public interface SLACalculator {
 
     SLACalcStatus get(String jobId) throws JPAExecutorException;
 
    /**
     * Enable jobs sla alert.
     *
     * @param jobId the job ids
     * @return true, if successful
     * @throws JPAExecutorException the JPA executor exception
     * @throws ServiceException the service exception
     */
    boolean enableAlert(List<String> jobId) throws JPAExecutorException, ServiceException;

    /**
     * Enable sla alert for child jobs.
     * @param jobId the parent job ids
     * @return
     * @throws JPAExecutorException
     * @throws ServiceException
     */
    boolean enableChildJobAlert(List<String> parentJobIds) throws JPAExecutorException, ServiceException;

    /**
     * Disable jobs Sla alert.
     *
     * @param jobId the job ids
     * @return true, if successful
     * @throws JPAExecutorException the JPA executor exception
     * @throws ServiceException the service exception
     */
    boolean disableAlert(List<String> jobId) throws JPAExecutorException, ServiceException;


    /**
     * Disable Sla alert for child jobs.
     * @param jobId the parent job ids
     * @return
     * @throws JPAExecutorException
     * @throws ServiceException
     */
    boolean disableChildJobAlert(List<String> parentJobIds) throws JPAExecutorException, ServiceException;

    /**
     * Change jobs Sla definitions
     * It takes list of pairs of jobid and key/value pairs of el evaluated sla definition.
     * Support definition are sla-should-start, sla-should-end, sla-nominal-time and sla-max-duration.
     *
     * @param jobIdsSLAPair the job ids sla pair
     * @return true, if successful
     * @throws JPAExecutorException the JPA executor exception
     * @throws ServiceException the service exception
     */
    public boolean changeDefinition(List<Pair<String, Map<String,String>>> jobIdsSLAPair ) throws JPAExecutorException,
            ServiceException;
 }
diff --git a/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java b/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
index fdce6b53c..42313fd3a 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
++ b/core/src/main/java/org/apache/oozie/sla/SLACalculatorMemory.java
@@ -38,13 +38,17 @@ import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.XException;
 import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.event.JobEvent;
 import org.apache.oozie.client.event.SLAEvent.EventStatus;
 import org.apache.oozie.client.event.SLAEvent.SLAStatus;
 import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.command.CommandException;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.CoordActionGetForSLAJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
@@ -52,17 +56,16 @@ import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor.SLARegQuery;
 import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor;
import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor.SLASummaryQuery;
 import org.apache.oozie.executor.jpa.WorkflowActionGetForSLAJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobGetForSLAJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowActionQueryExecutor.WorkflowActionQuery;
import org.apache.oozie.executor.jpa.WorkflowJobGetForSLAJPAExecutor;
import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
 import org.apache.oozie.executor.jpa.sla.SLASummaryGetRecordsOnRestartJPAExecutor;
import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor.SLASummaryQuery;
import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.lock.LockToken;
 import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
@@ -76,7 +79,7 @@ import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.LogUtils;
 import org.apache.oozie.util.XLog;

import org.apache.oozie.util.Pair;
 import com.google.common.annotations.VisibleForTesting;
 
 
@@ -453,6 +456,17 @@ public class SLACalculatorMemory implements SLACalculator {
         return memObj;
     }
 
    private SLACalcStatus getSLACalcStatus(String jobId) throws JPAExecutorException {
        SLACalcStatus memObj;
        memObj = slaMap.get(jobId);
        if (memObj == null) {
            memObj = new SLACalcStatus(SLASummaryQueryExecutor.getInstance().get(SLASummaryQuery.GET_SLA_SUMMARY, jobId),
                    SLARegistrationQueryExecutor.getInstance().get(SLARegQuery.GET_SLA_REG_ON_RESTART, jobId));
        }
        return memObj;
    }


     @Override
     public Iterator<String> iterator() {
         return slaMap.keySet().iterator();
@@ -477,9 +491,9 @@ public class SLACalculatorMemory implements SLACalculator {
         synchronized (slaCalc) {
             boolean change = false;
             // get eventProcessed on DB for validation in HA
            Object eventProcObj = ((SLASummaryQueryExecutor) SLASummaryQueryExecutor.getInstance()).getSingleValue(
                    SLASummaryQuery.GET_SLA_SUMMARY_EVENTPROCESSED, jobId);
            byte eventProc = ((Byte) eventProcObj).byteValue();
            SLASummaryBean summaryBean = ((SLASummaryQueryExecutor) SLASummaryQueryExecutor.getInstance()).get(
                    SLASummaryQuery.GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED, jobId);
            byte eventProc = summaryBean.getEventProcessed();
             if (eventProc >= 7) {
                 if (eventProc == 7) {
                     historySet.add(jobId);
@@ -488,6 +502,12 @@ public class SLACalculatorMemory implements SLACalculator {
                 LOG.trace("Removed Job [{0}] from map as SLA processed", jobId);
             }
             else {
                if (!slaCalc.getLastModifiedTime().equals(summaryBean.getLastModifiedTime())) {
                    //Update last modified time.
                    slaCalc.setLastModifiedTime(summaryBean.getLastModifiedTime());
                    reloadExpectedTimeAndConfig(slaCalc);
                    LOG.debug("Last modified time has changed for job " + jobId + " reloading config from DB");
                }
                 slaCalc.setEventProcessed(eventProc);
                 SLARegistrationBean reg = slaCalc.getSLARegistrationBean();
                 // calculation w.r.t current time and status
@@ -499,7 +519,9 @@ public class SLACalculatorMemory implements SLACalculator {
                             if (eventProc != 8 && (eventProc & 1) == 0) {
                                 // Some DB exception
                                 slaCalc.setEventStatus(EventStatus.START_MISS);
                                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                                if (shouldAlert(slaCalc)) {
                                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                                }
                                 eventProc++;
                             }
                             change = true;
@@ -525,7 +547,9 @@ public class SLACalculatorMemory implements SLACalculator {
                             if (eventProc != 8 && ((eventProc >> 1) & 1) == 0) {
                                 // Some DB exception
                                 slaCalc.setEventStatus(EventStatus.DURATION_MISS);
                                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                                if (shouldAlert(slaCalc)) {
                                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                                }
                                 eventProc += 2;
                             }
                             change = true;
@@ -552,26 +576,16 @@ public class SLACalculatorMemory implements SLACalculator {
                                 // Should not be > 8. But to handle any corner cases
                                 slaCalc.setEventProcessed(8);
                                 slaMap.remove(jobId);
                                LOG.trace("Removed Job [{0}] from map after Event-processed=8", jobId);
                             }
                             else {
                                 slaCalc.setEventProcessed(eventProc);
                             }
                            SLASummaryBean slaSummaryBean = new SLASummaryBean();
                            slaSummaryBean.setId(slaCalc.getId());
                            slaSummaryBean.setEventProcessed(eventProc);
                            slaSummaryBean.setSLAStatus(slaCalc.getSLAStatus());
                            slaSummaryBean.setEventStatus(slaCalc.getEventStatus());
                            slaSummaryBean.setActualEnd(slaCalc.getActualEnd());
                            slaSummaryBean.setActualStart(slaCalc.getActualStart());
                            slaSummaryBean.setActualDuration(slaCalc.getActualDuration());
                            slaSummaryBean.setJobStatus(slaCalc.getJobStatus());
                            slaSummaryBean.setLastModifiedTime(new Date());
                            SLASummaryQueryExecutor.getInstance().executeUpdate(
                                    SLASummaryQuery.UPDATE_SLA_SUMMARY_FOR_STATUS_ACTUAL_TIMES, slaSummaryBean);
                            writetoDB(slaCalc, eventProc);
                             if (eventProc == 7) {
                                 historySet.add(jobId);
                                 slaMap.remove(jobId);
                                LOG.trace("Removed Job [{0}] from map after End-processed", jobId);
                                LOG.trace("Removed Job [{0}] from map after Event-processed=7", jobId);
                             }
                         }
                     }
@@ -586,6 +600,48 @@ public class SLACalculatorMemory implements SLACalculator {
         }
     }
 
    private void writetoDB(SLACalcStatus slaCalc, byte eventProc) throws JPAExecutorException {
        SLASummaryBean slaSummaryBean = new SLASummaryBean();
        slaSummaryBean.setId(slaCalc.getId());
        slaSummaryBean.setEventProcessed(eventProc);
        slaSummaryBean.setSLAStatus(slaCalc.getSLAStatus());
        slaSummaryBean.setEventStatus(slaCalc.getEventStatus());
        slaSummaryBean.setActualEnd(slaCalc.getActualEnd());
        slaSummaryBean.setActualStart(slaCalc.getActualStart());
        slaSummaryBean.setActualDuration(slaCalc.getActualDuration());
        slaSummaryBean.setJobStatus(slaCalc.getJobStatus());
        slaSummaryBean.setLastModifiedTime(new Date());

        SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_FOR_STATUS_ACTUAL_TIMES,
                slaSummaryBean);
        LOG.trace("Stored SLA SummaryBean Job [{0}] with Event-processed=[{1}]", slaCalc.getId(),
                slaSummaryBean.getEventProcessed());
    }

    @SuppressWarnings("rawtypes")
    private void updateDBSlaConfig(SLACalcStatus slaCalc, List<UpdateEntry> updateList)
            throws JPAExecutorException {
        updateList.add(new UpdateEntry<SLARegQuery>(SLARegQuery.UPDATE_SLA_CONFIG, slaCalc.getSLARegistrationBean()));
        slaCalc.setLastModifiedTime(new Date());
        updateList.add(new UpdateEntry<SLASummaryQuery>(SLASummaryQuery.UPDATE_SLA_SUMMARY_LAST_MODIFIED_TIME, new SLASummaryBean(slaCalc)));
    }

    @SuppressWarnings("rawtypes")
    private void updateDBSlaExpectedValues(SLACalcStatus slaCalc, List<UpdateEntry> updateList)
            throws JPAExecutorException {
        slaCalc.setLastModifiedTime(new Date());
        updateList.add(new UpdateEntry<SLARegQuery>(SLARegQuery.UPDATE_SLA_EXPECTED_VALUE, slaCalc
                .getSLARegistrationBean()));
        updateList.add(new UpdateEntry<SLASummaryQuery>(SLASummaryQuery.UPDATE_SLA_SUMMARY_FOR_EXPECTED_TIMES,
                new SLASummaryBean(slaCalc)));
    }

    @SuppressWarnings("rawtypes")
    private void executeBatchQuery(List<UpdateEntry> updateList) throws JPAExecutorException {
        BatchQueryExecutor.getInstance().executeBatchInsertUpdateDelete(null, updateList, null);
    }


     /**
      * Periodically run by the SLAService worker threads to update SLA status by
      * iterating through all the jobs in the map
@@ -673,6 +729,8 @@ public class SLACalculatorMemory implements SLACalculator {
                 slaCalc.setSLAStatus(SLAStatus.NOT_STARTED);
                 slaCalc.setJobStatus(getJobStatus(reg.getAppType()));
                 slaMap.put(jobId, slaCalc);

                @SuppressWarnings("rawtypes")
                 List<UpdateEntry> updateList = new ArrayList<UpdateEntry>();
                 updateList.add(new UpdateEntry<SLARegQuery>(SLARegQuery.UPDATE_SLA_REG_ALL, reg));
                 updateList.add(new UpdateEntry<SLASummaryQuery>(SLASummaryQuery.UPDATE_SLA_SUMMARY_ALL,
@@ -758,9 +816,17 @@ public class SLACalculatorMemory implements SLACalculator {
                     locked = slaCalc.isLocked();
                     if (locked) {
                         // get eventProcessed on DB for validation in HA
                        Object eventProcObj = ((SLASummaryQueryExecutor) SLASummaryQueryExecutor.getInstance())
                                .getSingleValue(SLASummaryQuery.GET_SLA_SUMMARY_EVENTPROCESSED, jobId);
                        byte eventProc = ((Byte) eventProcObj).byteValue();
                        SLASummaryBean summaryBean = ((SLASummaryQueryExecutor) SLASummaryQueryExecutor.getInstance()).get(
                                SLASummaryQuery.GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED, jobId);
                        byte eventProc = summaryBean.getEventProcessed();

                        if (!slaCalc.getLastModifiedTime().equals(summaryBean.getLastModifiedTime())) {
                            //Update last modified time.
                            slaCalc.setLastModifiedTime(summaryBean.getLastModifiedTime());
                            reloadExpectedTimeAndConfig(slaCalc);
                            LOG.debug("Last modified time has changed for job " + jobId + " reloading config from DB");
                        }

                         slaCalc.setEventProcessed(eventProc);
                         slaCalc.setJobStatus(jobStatus);
                         switch (jobEventStatus) {
@@ -824,7 +890,9 @@ public class SLACalculatorMemory implements SLACalculator {
                 else {
                     slaCalc.setEventStatus(EventStatus.START_MET);
                 }
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                if (shouldAlert(slaCalc)) {
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                }
             }
             eventProc += 1;
             slaCalc.setEventProcessed(eventProc);
@@ -869,7 +937,9 @@ public class SLACalculatorMemory implements SLACalculator {
             }
             eventProc += 4;
             slaCalc.setEventProcessed(eventProc);
            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
            if (shouldAlert(slaCalc)) {
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
            }
         }
         return getSLASummaryBean(slaCalc);
     }
@@ -891,7 +961,9 @@ public class SLACalculatorMemory implements SLACalculator {
             if (slaCalc.getEventProcessed() < 4) {
                 slaCalc.setEventStatus(EventStatus.END_MISS);
                 slaCalc.setSLAStatus(SLAStatus.MISS);
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                if (shouldAlert(slaCalc)) {
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                }
                 slaCalc.setEventProcessed(7);
                 return getSLASummaryBean(slaCalc);
             }
@@ -905,7 +977,9 @@ public class SLACalculatorMemory implements SLACalculator {
         if (((eventProc >> 1) & 1) == 0) {
             if (expectedDuration != -1) {
                 slaCalc.setEventStatus(EventStatus.DURATION_MISS);
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                if (shouldAlert(slaCalc)) {
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                }
             }
             eventProc += 2;
             slaCalc.setEventProcessed(eventProc);
@@ -915,7 +989,9 @@ public class SLACalculatorMemory implements SLACalculator {
             slaCalc.setSLAStatus(SLAStatus.MISS);
             eventProc += 4;
             slaCalc.setEventProcessed(eventProc);
            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
            if (shouldAlert(slaCalc)) {
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
            }
         }
         return getSLASummaryBean(slaCalc);
     }
@@ -934,13 +1010,16 @@ public class SLACalculatorMemory implements SLACalculator {
     }
 
     private void processDurationSLA(long expected, long actual, SLACalcStatus slaCalc) {
        if (expected != -1 && actual > expected) {
            slaCalc.setEventStatus(EventStatus.DURATION_MISS);
            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
        }
        else if (expected != -1 && actual <= expected) {
            slaCalc.setEventStatus(EventStatus.DURATION_MET);
            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
        if (expected != -1) {
            if (actual > expected) {
                slaCalc.setEventStatus(EventStatus.DURATION_MISS);
            }
            else if (actual <= expected) {
                slaCalc.setEventStatus(EventStatus.DURATION_MET);
            }
            if (shouldAlert(slaCalc)) {
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
            }
         }
     }
 
@@ -1016,7 +1095,9 @@ public class SLACalculatorMemory implements SLACalculator {
                         else {
                             slaCalc.setEventStatus(EventStatus.START_MET);
                         }
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        if (shouldAlert(slaCalc)) {
                            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        }
                     }
                     slaCalc.setActualDuration(slaCalc.getActualEnd().getTime() - slaCalc.getActualStart().getTime());
                     if (((eventProc >> 1) & 1) == 0) {
@@ -1030,7 +1111,9 @@ public class SLACalculatorMemory implements SLACalculator {
                     else {
                         slaCalc.setEventStatus(EventStatus.END_MET);
                     }
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                    if (shouldAlert(slaCalc)) {
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                    }
                 }
                 slaCalc.setEventProcessed(8);
             }
@@ -1046,12 +1129,16 @@ public class SLACalculatorMemory implements SLACalculator {
                         else {
                             slaCalc.setEventStatus(EventStatus.START_MET);
                         }
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        if (shouldAlert(slaCalc)) {
                            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        }
                         eventProc++;
                     }
                     else if (slaCalc.getExpectedStart().getTime() < System.currentTimeMillis()) {
                         slaCalc.setEventStatus(EventStatus.START_MISS);
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        if (shouldAlert(slaCalc)) {
                            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        }
                         eventProc++;
                     }
                 }
@@ -1059,14 +1146,18 @@ public class SLACalculatorMemory implements SLACalculator {
                         && slaCalc.getExpectedDuration() != -1) {
                     if (System.currentTimeMillis() - slaCalc.getActualStart().getTime() > slaCalc.getExpectedDuration()) {
                         slaCalc.setEventStatus(EventStatus.DURATION_MISS);
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        if (shouldAlert(slaCalc)) {
                            eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                        }
                         eventProc += 2;
                     }
                 }
                 if (eventProc < 4 && slaCalc.getExpectedEnd().getTime() < System.currentTimeMillis()) {
                     slaCalc.setEventStatus(EventStatus.END_MISS);
                     slaCalc.setSLAStatus(SLAStatus.MISS);
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                    if (shouldAlert(slaCalc)) {
                        eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                    }
                     eventProc += 4;
                 }
                 slaCalc.setEventProcessed(eventProc);
@@ -1078,12 +1169,36 @@ public class SLACalculatorMemory implements SLACalculator {
             if (slaCalc.getEventProcessed() < 4 && slaCalc.getExpectedEnd().getTime() < System.currentTimeMillis()) {
                 slaCalc.setEventStatus(EventStatus.END_MISS);
                 slaCalc.setSLAStatus(SLAStatus.MISS);
                eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                if (shouldAlert(slaCalc)) {
                    eventHandler.queueEvent(new SLACalcStatus(slaCalc));
                }
                 slaCalc.setEventProcessed(slaCalc.getEventProcessed() + 4);
             }
         }
     }
 
    public void reloadExpectedTimeAndConfig(SLACalcStatus slaCalc) throws JPAExecutorException {
        SLARegistrationBean regBean = SLARegistrationQueryExecutor.getInstance().get(
                SLARegQuery.GET_SLA_EXPECTED_VALUE_CONFIG, slaCalc.getId());

        if (regBean.getExpectedDuration() > 0) {
            slaCalc.getSLARegistrationBean().setExpectedDuration(regBean.getExpectedDuration());
        }
        if (regBean.getExpectedEnd() != null) {
            slaCalc.getSLARegistrationBean().setExpectedEnd(regBean.getExpectedEnd());
        }
        if (regBean.getExpectedStart() != null) {
            slaCalc.getSLARegistrationBean().setExpectedStart(regBean.getExpectedStart());
        }
        if (regBean.getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT)) {
            slaCalc.getSLARegistrationBean().addToSLAConfigMap(OozieClient.SLA_DISABLE_ALERT,
                    regBean.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT));
        }
        if (regBean.getNominalTime() != null) {
            slaCalc.getSLARegistrationBean().setNominalTime(regBean.getNominalTime());
        }
    }

     @VisibleForTesting
     public boolean isJobIdInSLAMap(String jobId) {
         return this.slaMap.containsKey(jobId);
@@ -1097,4 +1212,99 @@ public class SLACalculatorMemory implements SLACalculator {
     private void setLogPrefix(String jobId) {
         LOG = LogUtils.setLogInfo(LOG, jobId, null, null);
     }

    @Override
    public boolean enableAlert(List<String> jobIds) throws JPAExecutorException, ServiceException {
        boolean isJobFound = false;
        @SuppressWarnings("rawtypes")
        List<UpdateEntry> updateList = new ArrayList<BatchQueryExecutor.UpdateEntry>();
        for (String jobId : jobIds) {
            SLACalcStatus slaCalc = getSLACalcStatus(jobId);
            if (slaCalc != null) {
                slaCalc.getSLARegistrationBean().removeFromSLAConfigMap(OozieClient.SLA_DISABLE_ALERT);
                updateDBSlaConfig(slaCalc, updateList);
                isJobFound = true;
            }
        }
        executeBatchQuery(updateList);
        return isJobFound;
    }

    @Override
    public boolean enableChildJobAlert(List<String> parentJobIds) throws JPAExecutorException, ServiceException {
        return enableAlert(getSLAJobsforParents(parentJobIds));
    }


    @Override
    public boolean disableAlert(List<String> jobIds) throws JPAExecutorException, ServiceException {
        boolean isJobFound = false;
        @SuppressWarnings("rawtypes")
        List<UpdateEntry> updateList = new ArrayList<BatchQueryExecutor.UpdateEntry>();

            for (String jobId : jobIds) {
                SLACalcStatus slaCalc = getSLACalcStatus(jobId);
                if (slaCalc != null) {
                    slaCalc.getSLARegistrationBean().addToSLAConfigMap(OozieClient.SLA_DISABLE_ALERT, Boolean.toString(true));
                    updateDBSlaConfig(slaCalc, updateList);
                    isJobFound = true;
                }
            }
        executeBatchQuery(updateList);
        return isJobFound;
    }

    @Override
    public boolean disableChildJobAlert(List<String> parentJobIds) throws JPAExecutorException, ServiceException {
        return disableAlert(getSLAJobsforParents(parentJobIds));
    }

    @Override
    public boolean changeDefinition(List<Pair<String, Map<String,String>>> jobIdsSLAPair ) throws JPAExecutorException,
            ServiceException{
        boolean isJobFound = false;
        @SuppressWarnings("rawtypes")
        List<UpdateEntry> updateList = new ArrayList<BatchQueryExecutor.UpdateEntry>();
            for (Pair<String, Map<String,String>> jobIdSLAPair : jobIdsSLAPair) {
                SLACalcStatus slaCalc = getSLACalcStatus(jobIdSLAPair.getFist());
                if (slaCalc != null) {
                    updateParams(slaCalc, jobIdSLAPair.getSecond());
                    updateDBSlaExpectedValues(slaCalc, updateList);
                    isJobFound = true;
                }
            }
        executeBatchQuery(updateList);
        return isJobFound;
    }

    private void updateParams(SLACalcStatus slaCalc, Map<String, String> newParams) throws ServiceException {
        SLARegistrationBean reg = slaCalc.getSLARegistrationBean();
        if (newParams != null) {
            try {
                Date newNominal = SLAOperations.setNominalTime(newParams.get(RestConstants.SLA_NOMINAL_TIME), reg);
                SLAOperations.setExpectedStart(newParams.get(RestConstants.SLA_SHOULD_START), newNominal, reg);
                SLAOperations.setExpectedEnd(newParams.get(RestConstants.SLA_SHOULD_END), newNominal, reg);
                SLAOperations.setExpectedDuration(newParams.get(RestConstants.SLA_MAX_DURATION), reg);
            }
            catch (CommandException ce) {
                throw new ServiceException(ce);
            }
        }
    }

    private boolean shouldAlert(SLACalcStatus slaObj) {
        return !slaObj.getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT);
    }

    private List<String> getSLAJobsforParents(List<String> parentJobIds) throws JPAExecutorException{
        List<String> childJobIds = new ArrayList<String>();
        for (String jobId : parentJobIds) {
            List<SLARegistrationBean> registrationBeanList = SLARegistrationQueryExecutor.getInstance().getList(
                    SLARegQuery.GET_SLA_REG_FOR_PARENT_ID, jobId);
            for (SLARegistrationBean bean : registrationBeanList) {
                childJobIds.add(bean.getId());
            }
        }
        return childJobIds;
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/sla/SLAOperations.java b/core/src/main/java/org/apache/oozie/sla/SLAOperations.java
index f5fc8269f..390500341 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLAOperations.java
++ b/core/src/main/java/org/apache/oozie/sla/SLAOperations.java
@@ -23,15 +23,14 @@ import java.util.Date;
 
 import org.apache.oozie.AppType;
 import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.event.SLAEvent.EventStatus;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor.SLARegQuery;
import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.ServiceException;
 import org.apache.oozie.service.Services;
import org.apache.oozie.sla.SLARegistrationBean;
 import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.XLog;
@@ -40,14 +39,20 @@ import org.jdom.Element;
 
 public class SLAOperations {
 
    private static final String NOMINAL_TIME = "nominal-time";
    private static final String SHOULD_START = "should-start";
    private static final String SHOULD_END = "should-end";
    private static final String MAX_DURATION = "max-duration";
    private static final String ALERT_EVENTS = "alert-events";
    public static final String NOMINAL_TIME = "nominal-time";
    public static final String SHOULD_START = "should-start";
    public static final String SHOULD_END = "should-end";
    public static final String MAX_DURATION = "max-duration";
    public static final String ALERT_EVENTS = "alert-events";
    public static final String ALL_VALUE = "ALL";


    static public XLog LOG = XLog.getLog(SLAOperations.class);

 
     public static SLARegistrationBean createSlaRegistrationEvent(Element eSla, String jobId, String parentId,
            AppType appType, String user, String appName, XLog log, boolean rerun) throws CommandException {
            AppType appType, String user, String appName, XLog log, boolean rerun, boolean disableAlert)
            throws CommandException {
         if (eSla == null || !SLAService.isEnabled()) {
             log.debug("Not registering SLA for job [{0}]. Sla-Xml null OR SLAService not enabled", jobId);
             return null;
@@ -56,56 +61,19 @@ public class SLAOperations {
 
         // Setting nominal time
         String strNominalTime = getTagElement(eSla, NOMINAL_TIME);
        if (strNominalTime == null || strNominalTime.length() == 0) {
            throw new CommandException(ErrorCode.E1101, NOMINAL_TIME);
        }
        Date nominalTime;
        try {
            nominalTime = DateUtils.parseDateOozieTZ(strNominalTime);
            sla.setNominalTime(nominalTime);
        }
        catch (ParseException pex) {
            throw new CommandException(ErrorCode.E0302, strNominalTime, pex);
        }
        Date nominalTime = setNominalTime(strNominalTime, sla);
 
         // Setting expected start time
         String strExpectedStart = getTagElement(eSla, SHOULD_START);
        if (strExpectedStart != null) {
            float expectedStart = Float.parseFloat(strExpectedStart);
            if (expectedStart < 0) {
                throw new CommandException(ErrorCode.E0302, strExpectedStart, "for SLA Expected start time");
            }
            else {
                Date expectedStartTime = new Date(nominalTime.getTime() + (long) (expectedStart * 60 * 1000));
                sla.setExpectedStart(expectedStartTime);
            }
        }
        setExpectedStart(strExpectedStart, nominalTime, sla);
 
         // Setting expected end time
         String strExpectedEnd = getTagElement(eSla, SHOULD_END);
        if (strExpectedEnd == null || strExpectedEnd.length() == 0) {
            throw new CommandException(ErrorCode.E1101, SHOULD_END);
        }
        float expectedEnd = Float.parseFloat(strExpectedEnd);
        if (expectedEnd < 0) {
            throw new CommandException(ErrorCode.E0302, strExpectedEnd, "for SLA Expected end time");
        }
        else {
            Date expectedEndTime = new Date(nominalTime.getTime() + (long) (expectedEnd * 60 * 1000));
            sla.setExpectedEnd(expectedEndTime);
        }
        setExpectedEnd(strExpectedEnd, nominalTime, sla);
 
         // Setting expected duration in milliseconds
         String expectedDurationStr = getTagElement(eSla, MAX_DURATION);
        if (expectedDurationStr != null && expectedDurationStr.length() > 0) {
            float expectedDuration = Float.parseFloat(expectedDurationStr);
            if (expectedDuration > 0) {
                sla.setExpectedDuration((long) (expectedDuration * 60 * 1000));
            }
        }
        else if (sla.getExpectedStart() != null) {
            sla.setExpectedDuration(sla.getExpectedEnd().getTime() - sla.getExpectedStart().getTime());
        }
        setExpectedDuration(expectedDurationStr, sla);
 
         // Parse desired alert-types i.e. start-miss, end-miss, start-met etc..
         String alertEvents = getTagElement(eSla, ALERT_EVENTS);
@@ -134,6 +102,10 @@ public class SLAOperations {
         sla.setAlertContact(getTagElement(eSla, "alert-contact"));
         sla.setUpstreamApps(getTagElement(eSla, "upstream-apps"));
 
        //disable Alert flag in slaConfig
        if (disableAlert) {
            sla.addToSLAConfigMap(OozieClient.SLA_DISABLE_ALERT, Boolean.toString(disableAlert));
        }
         // Oozie defined
         sla.setId(jobId);
         sla.setAppType(appType);
@@ -158,6 +130,68 @@ public class SLAOperations {
         return sla;
     }
 
    public static Date setNominalTime(String strNominalTime, SLARegistrationBean sla) throws CommandException {
        if (strNominalTime == null || strNominalTime.length() == 0) {
            return sla.getNominalTime();
        }
        Date nominalTime;
        try {
            nominalTime = DateUtils.parseDateOozieTZ(strNominalTime);
            sla.setNominalTime(nominalTime);
        }
        catch (ParseException pex) {
            throw new CommandException(ErrorCode.E0302, strNominalTime, pex);
        }
        return nominalTime;
    }

    public static void setExpectedStart(String strExpectedStart, Date nominalTime, SLARegistrationBean sla)
            throws CommandException {
        if (strExpectedStart != null) {
            float expectedStart = Float.parseFloat(strExpectedStart);
            if (expectedStart < 0) {
                throw new CommandException(ErrorCode.E0302, strExpectedStart, "for SLA Expected start time");
            }
            else {
                Date expectedStartTime = new Date(nominalTime.getTime() + (long) (expectedStart * 60 * 1000));
                sla.setExpectedStart(expectedStartTime);
                LOG.debug("Setting expected start to " + expectedStartTime + " for job " + sla.getId());
            }
        }
    }

    public static void setExpectedEnd(String strExpectedEnd, Date nominalTime, SLARegistrationBean sla)
            throws CommandException {
        if (strExpectedEnd != null) {
            float expectedEnd = Float.parseFloat(strExpectedEnd);
            if (expectedEnd < 0) {
                throw new CommandException(ErrorCode.E0302, strExpectedEnd, "for SLA Expected end time");
            }
            else {
                Date expectedEndTime = new Date(nominalTime.getTime() + (long) (expectedEnd * 60 * 1000));
                sla.setExpectedEnd(expectedEndTime);
                LOG.debug("Setting expected end to " + expectedEndTime + " for job " + sla.getId());

            }
        }
    }

    public static void setExpectedDuration(String expectedDurationStr, SLARegistrationBean sla) {
        if (expectedDurationStr != null && expectedDurationStr.length() > 0) {
            float expectedDuration = Float.parseFloat(expectedDurationStr);
            if (expectedDuration > 0) {
                long duration = (long) (expectedDuration * 60 * 1000);
                LOG.debug("Setting expected duration to " + duration + " for job " + sla.getId());
                sla.setExpectedDuration(duration);
            }
        }
        else if (sla.getExpectedStart() != null) {
            long duration = sla.getExpectedEnd().getTime() - sla.getExpectedStart().getTime();
            LOG.debug("Setting expected duration to " + duration + " for job " + sla.getId());
            sla.setExpectedDuration(sla.getExpectedEnd().getTime() - sla.getExpectedStart().getTime());
        }
    }

     /**
      * Retrieve registration event
      * @param jobId the jobId
@@ -165,7 +199,6 @@ public class SLAOperations {
      * @throws JPAExecutorException
      */
     public static void updateRegistrationEvent(String jobId) throws CommandException, JPAExecutorException {
        JPAService jpaService = Services.get().get(JPAService.class);
         SLAService slaService = Services.get().get(SLAService.class);
         try {
             SLARegistrationBean reg = SLARegistrationQueryExecutor.getInstance().get(SLARegQuery.GET_SLA_REG_ALL, jobId);
@@ -203,7 +236,15 @@ public class SLAOperations {
         return createSlaRegistrationEvent(eSla, jobId, null, appType, user, null, log, false);
     }
 
    private static String getTagElement(Element elem, String tagName) {
    /*
     * default disableAlert flag
     */
    public static SLARegistrationBean createSlaRegistrationEvent(Element eSla, String jobId, String parentId,
            AppType appType, String user, String appName, XLog log, boolean rerun) throws CommandException {
        return createSlaRegistrationEvent(eSla, jobId, null, appType, user, appName, log, rerun, false);
    }

    public static String getTagElement(Element elem, String tagName) {
         if (elem != null && elem.getChild(tagName, elem.getNamespace("sla")) != null) {
             return elem.getChild(tagName, elem.getNamespace("sla")).getText().trim();
         }
diff --git a/core/src/main/java/org/apache/oozie/sla/SLARegistrationBean.java b/core/src/main/java/org/apache/oozie/sla/SLARegistrationBean.java
index 0770bd369..1b8370f01 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLARegistrationBean.java
++ b/core/src/main/java/org/apache/oozie/sla/SLARegistrationBean.java
@@ -33,7 +33,6 @@ import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.Table;
 import javax.persistence.Transient;

 import org.apache.oozie.AppType;
 import org.apache.oozie.client.event.Event.MessageType;
 import org.apache.oozie.client.rest.JsonBean;
@@ -48,9 +47,21 @@ import org.json.simple.JSONObject;
 
  @NamedQuery(name = "UPDATE_SLA_REG_ALL", query = "update SLARegistrationBean w set w.jobId = :jobId, w.nominalTimeTS = :nominalTime, w.expectedStartTS = :expectedStartTime, w.expectedEndTS = :expectedEndTime, w.expectedDuration = :expectedDuration, w.slaConfig = :slaConfig, w.notificationMsg = :notificationMsg, w.upstreamApps = :upstreamApps, w.appType = :appType, w.appName = :appName, w.user = :user, w.parentId = :parentId, w.jobData = :jobData where w.jobId = :jobId"),
 
 @NamedQuery(name = "UPDATE_SLA_CONFIG", query = "update SLARegistrationBean w set w.slaConfig = :slaConfig where w.jobId = :jobId"),

 @NamedQuery(name = "UPDATE_SLA_EXPECTED_VALUE", query = "update SLARegistrationBean w set w.expectedStartTS = :expectedStartTime, w.expectedEndTS = :expectedEndTime , w.expectedDuration = :expectedDuration  where w.jobId = :jobId"),

  @NamedQuery(name = "GET_SLA_REG_ON_RESTART", query = "select w.notificationMsg, w.upstreamApps, w.slaConfig, w.jobData from SLARegistrationBean w where w.jobId = :id"),
 
 @NamedQuery(name = "GET_SLA_REG_ALL", query = "select OBJECT(w) from SLARegistrationBean w where w.jobId = :id") })
 @NamedQuery(name = "GET_SLA_REG_ALL", query = "select OBJECT(w) from SLARegistrationBean w where w.jobId = :id"),

 @NamedQuery(name = "GET_SLA_CONFIGS", query = "select w.jobId, w.slaConfig from SLARegistrationBean w where w.jobId IN (:ids)"),

 @NamedQuery(name = "GET_SLA_EXPECTED_VALUE_CONFIG", query = "select w.jobId, w.slaConfig, w.expectedStartTS, w.expectedEndTS, w.expectedDuration, w.nominalTimeTS from SLARegistrationBean w where w.jobId = :id"),

 @NamedQuery(name = "GET_SLA_REG_FOR_PARENT_ID", query = "select w.jobId, w.slaConfig from SLARegistrationBean w where w.parentId = :parentId")
 })

 public class SLARegistrationBean implements JsonBean {
 
     @Id
@@ -281,10 +292,21 @@ public class SLARegistrationBean implements JsonBean {
         slaConfig = slaConfigMapToString();
     }
 
    public Map<String, String> getSlaConfigMap() {

    public Map<String, String> getSLAConfigMap() {
         return slaConfigMap;
     }
 
    public void addToSLAConfigMap(String key, String value) {
        slaConfigMap.put(key, value);
        slaConfig = slaConfigMapToString();
    }

    public void removeFromSLAConfigMap(String key) {
        slaConfigMap.remove(key);
        slaConfig = slaConfigMapToString();
    }

     private void slaConfigStringToMap() {
         if (slaConfig != null) {
             String[] splitString = slaConfig.split("},");
diff --git a/core/src/main/java/org/apache/oozie/sla/SLASummaryBean.java b/core/src/main/java/org/apache/oozie/sla/SLASummaryBean.java
index 9907dd009..a88dcf612 100644
-- a/core/src/main/java/org/apache/oozie/sla/SLASummaryBean.java
++ b/core/src/main/java/org/apache/oozie/sla/SLASummaryBean.java
@@ -21,6 +21,7 @@ package org.apache.oozie.sla;
 import java.sql.Timestamp;
 import java.util.Date;
 import java.util.List;
import java.util.Map;
 
 import javax.persistence.Basic;
 import javax.persistence.Column;
@@ -31,6 +32,7 @@ import javax.persistence.NamedQuery;
 import javax.persistence.Table;
 
 import org.apache.oozie.AppType;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.client.rest.JsonBean;
 import org.apache.oozie.client.rest.JsonTags;
@@ -50,15 +52,22 @@ import org.json.simple.JSONObject;
 
  @NamedQuery(name = "UPDATE_SLA_SUMMARY_FOR_ACTUAL_TIMES", query = "update SLASummaryBean w set w.eventProcessed = :eventProcessed, w.actualStartTS = :actualStartTS, w.actualEndTS = :actualEndTS, w.actualEndTS = :actualEndTS, w.actualDuration = :actualDuration, w.lastModifiedTS = :lastModifiedTS where w.jobId = :jobId"),
 
 @NamedQuery(name = "UPDATE_SLA_SUMMARY_FOR_EXPECTED_TIMES", query = "update SLASummaryBean w set w.nominalTimeTS = :nominalTime, w.expectedStartTS = :expectedStartTime, w.expectedEndTS = :expectedEndTime, w.expectedDuration = :expectedDuration , w.lastModifiedTS = :lastModTime where w.jobId = :jobId"),

  @NamedQuery(name = "UPDATE_SLA_SUMMARY_EVENTPROCESSED", query = "update SLASummaryBean w set w.eventProcessed = :eventProcessed where w.jobId = :jobId"),
 
 @NamedQuery(name = "UPDATE_SLA_SUMMARY_LAST_MODIFIED_TIME", query = "update SLASummaryBean w set w.lastModifiedTS = :lastModifiedTS where w.jobId = :jobId"),

  @NamedQuery(name = "UPDATE_SLA_SUMMARY_ALL", query = "update SLASummaryBean w set w.jobId = :jobId, w.appName = :appName, w.appType = :appType, w.nominalTimeTS = :nominalTime, w.expectedStartTS = :expectedStartTime, w.expectedEndTS = :expectedEndTime, w.expectedDuration = :expectedDuration, w.jobStatus = :jobStatus, w.slaStatus = :slaStatus, w.eventStatus = :eventStatus, w.lastModifiedTS = :lastModTime, w.user = :user, w.parentId = :parentId, w.eventProcessed = :eventProcessed, w.actualDuration = :actualDuration, w.actualEndTS = :actualEndTS, w.actualStartTS = :actualStartTS where w.jobId = :jobId"),
 
  @NamedQuery(name = "GET_SLA_SUMMARY", query = "select OBJECT(w) from SLASummaryBean w where w.jobId = :id"),
 
  @NamedQuery(name = "GET_SLA_SUMMARY_RECORDS_RESTART", query = "select OBJECT(w) from SLASummaryBean w where w.eventProcessed <= 7 AND w.lastModifiedTS >= :lastModifiedTime"),
 
 @NamedQuery(name = "GET_SLA_SUMMARY_EVENTPROCESSED", query = "select w.eventProcessed from SLASummaryBean w where w.jobId = :id")
 @NamedQuery(name = "GET_SLA_SUMMARY_EVENTPROCESSED", query = "select w.eventProcessed from SLASummaryBean w where w.jobId = :id"),

 @NamedQuery(name = "GET_SLA_SUMMARY_EVENTPROCESSED_LAST_MODIFIED", query = "select w.eventProcessed, w.lastModifiedTS from SLASummaryBean w where w.jobId = :id")

 })
 
 /**
@@ -431,6 +440,7 @@ public class SLASummaryBean implements JsonBean {
             json.put(JsonTags.SLA_SUMMARY_JOB_STATUS, jobStatus);
             json.put(JsonTags.SLA_SUMMARY_SLA_STATUS, slaStatus);
             json.put(JsonTags.SLA_SUMMARY_LAST_MODIFIED, JsonUtils.formatDateRfc822(lastModifiedTS, timeZoneId));

             return json;
         }
     }
@@ -455,4 +465,25 @@ public class SLASummaryBean implements JsonBean {
         return json;
     }
 
    @SuppressWarnings("unchecked")
    public static JSONObject toJSONObject(List<? extends SLASummaryBean> slaSummaryList,
            Map<String, Map<String, String>> slaConfigMap, String timeZoneId) {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        if (slaSummaryList != null) {
            for (SLASummaryBean summary : slaSummaryList) {
                JSONObject slaJson = summary.toJSONObject(timeZoneId);
                String slaAlertStatus = "";
                if (slaConfigMap.containsKey(summary.getId())) {
                    slaAlertStatus = slaConfigMap.get(summary.getId()).containsKey(
                            OozieClient.SLA_DISABLE_ALERT) ? "Disabled" : "Enabled";
                }
                slaJson.put(JsonTags.SLA_ALERT_STATUS, slaAlertStatus);
                array.add(slaJson);
            }
        }
        json.put(JsonTags.SLA_SUMMARY_LIST, array);
        return json;
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/sla/service/SLAService.java b/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
index a4562e77c..ef1d335e8 100644
-- a/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
++ b/core/src/main/java/org/apache/oozie/sla/service/SLAService.java
@@ -19,6 +19,8 @@
 package org.apache.oozie.sla.service;
 
 import java.util.Date;
import java.util.List;
import java.util.Map;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
@@ -33,6 +35,7 @@ import org.apache.oozie.service.Services;
 import org.apache.oozie.sla.SLACalculator;
 import org.apache.oozie.sla.SLACalculatorMemory;
 import org.apache.oozie.sla.SLARegistrationBean;
import org.apache.oozie.util.Pair;
 import org.apache.oozie.util.XLog;
 
 import com.google.common.annotations.VisibleForTesting;
@@ -107,7 +110,6 @@ public class SLAService implements Service {
         return calcImpl;
     }
 
    @VisibleForTesting
     public void runSLAWorker() {
         new SLAWorker(calcImpl).run();
     }
@@ -181,4 +183,94 @@ public class SLAService implements Service {
         calcImpl.removeRegistration(jobId);
     }
 
    /**
     * Enable jobs sla alert.
     *
     * @param jobIds the job ids
     * @param isParentJob, if jobIds are parent job
     * @return true, if successful
     * @throws ServiceException the service exception
     */
    public boolean enableAlert(List<String> jobIds) throws ServiceException {
        try {
            return calcImpl.enableAlert(jobIds);
        }
        catch (JPAExecutorException jpe) {
            LOG.error("Exception while updating SLA alerting for Job [{0}]", jobIds.get(0));
            throw new ServiceException(jpe);
        }
    }

    /**
     * Enable child jobs sla alert.
     *
     * @param jobIds the parent job ids
     * @param isParentJob, if jobIds are parent job
     * @return true, if successful
     * @throws ServiceException the service exception
     */
    public boolean enableChildJobAlert(List<String> parentJobIds) throws ServiceException {
        try {
            return calcImpl.enableChildJobAlert(parentJobIds);
        }
        catch (JPAExecutorException jpe) {
            LOG.error("Exception while updating SLA alerting for Job [{0}]", parentJobIds.get(0));
            throw new ServiceException(jpe);
        }
    }

    /**
     * Disable jobs Sla alert.
     *
     * @param jobIds the job ids
     * @param isParentJob, if jobIds are parent job
     * @return true, if successful
     * @throws ServiceException the service exception
     */
    public boolean disableAlert(List<String> jobIds) throws ServiceException {
        try {
            return calcImpl.disableAlert(jobIds);
        }
        catch (JPAExecutorException jpe) {
            LOG.error("Exception while updating SLA alerting for Job [{0}]", jobIds.get(0));
            throw new ServiceException(jpe);
        }
    }

    /**
     * Disable child jobs Sla alert.
     *
     * @param jobIds the parent job ids
     * @param isParentJob, if jobIds are parent job
     * @return true, if successful
     * @throws ServiceException the service exception
     */
    public boolean disableChildJobAlert(List<String> parentJobIds) throws ServiceException {
        try {
            return calcImpl.disableChildJobAlert(parentJobIds);
        }
        catch (JPAExecutorException jpe) {
            LOG.error("Exception while updating SLA alerting for Job [{0}]", parentJobIds.get(0));
            throw new ServiceException(jpe);
        }
    }

    /**
     * Change jobs Sla definitions
     * It takes list of pairs of jobid and key/value pairs of el evaluated sla definition.
     * Support definition are sla-should-start, sla-should-end, sla-nominal-time and sla-max-duration.
     *
     * @param jobIdsSLAPair the job ids sla pair
     * @return true, if successful
     * @throws ServiceException the service exception
     */
    public boolean changeDefinition(List<Pair<String, Map<String, String>>> idSlaDefinitionList)
            throws ServiceException {
        try {
            return calcImpl.changeDefinition(idSlaDefinitionList);
        }
        catch (JPAExecutorException jpe) {
            throw new ServiceException(jpe);
        }
    }
 }
diff --git a/core/src/main/java/org/apache/oozie/util/CoordActionsInDateRange.java b/core/src/main/java/org/apache/oozie/util/CoordActionsInDateRange.java
index 7c2620c60..1c565ef3e 100644
-- a/core/src/main/java/org/apache/oozie/util/CoordActionsInDateRange.java
++ b/core/src/main/java/org/apache/oozie/util/CoordActionsInDateRange.java
@@ -30,12 +30,11 @@ import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.XException;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetActionModifiedDateForRangeJPAExecutor;
import org.apache.oozie.executor.jpa.CoordJobGetActionIdsForDateRangeJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetActionRunningCountForRangeJPAExecutor;
import org.apache.oozie.executor.jpa.CoordJobGetActionsByDatesForKillJPAExecutor;
import org.apache.oozie.executor.jpa.CoordJobGetActionsForDatesJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 
@@ -139,10 +138,13 @@ public class CoordActionsInDateRange {
                 throw new XException(ErrorCode.E0308, "'" + range + "'. Start date '" + start + "' is older than end date: '" + end
 + "'");
             }
            List<String> list = null;
            JPAService jpaService = Services.get().get(JPAService.class);
            list = jpaService.execute(new CoordJobGetActionIdsForDateRangeJPAExecutor(jobId, start, end));
            return list;
            List<CoordinatorActionBean> listOfActions = CoordActionQueryExecutor.getInstance().getList(
                    CoordActionQuery.GET_TERMINATED_ACTIONS_FOR_DATES, jobId, start, end);
            List<String> idsList = new ArrayList<String>();
            for ( CoordinatorActionBean bean : listOfActions){
                idsList.add(bean.getId());
            }
            return idsList;
     }
 
     /**
@@ -156,12 +158,13 @@ public class CoordActionsInDateRange {
     private static List<CoordinatorActionBean> getActionsFromDateRange(String jobId, Date start, Date end,
             boolean active) throws XException {
         List<CoordinatorActionBean> list;
        JPAService jpaService = Services.get().get(JPAService.class);
         if (!active) {
            list = jpaService.execute(new CoordJobGetActionsForDatesJPAExecutor(jobId, start, end));
            list = CoordActionQueryExecutor.getInstance().getList(
                    CoordActionQuery.GET_TERMINATED_ACTIONS_FOR_DATES, jobId, start, end);
         }
         else {
            list = jpaService.execute(new CoordJobGetActionsByDatesForKillJPAExecutor(jobId, start, end));
            list = CoordActionQueryExecutor.getInstance().getList(
                    CoordActionQuery.GET_ACTIVE_ACTIONS_FOR_DATES, jobId, start, end);
         }
         return list;
     }
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index 6f76b0744..b40fec0e2 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -2211,6 +2211,15 @@
         </description>
     </property>
 
    <property>
        <name>oozie.sla.disable.alerts.older.than</name>
        <value>48</value>
        <description>
             Time threshold, in HOURS, for disabling SLA alerting for jobs whose
             nominal time is older than this.
        </description>
    </property>

     <!-- ZooKeeper configuration -->
     <property>
         <name>oozie.zookeeper.connection.string</name>
diff --git a/core/src/test/java/org/apache/oozie/command/TestSLAAlertXCommand.java b/core/src/test/java/org/apache/oozie/command/TestSLAAlertXCommand.java
new file mode 100644
index 000000000..ce5988516
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/command/TestSLAAlertXCommand.java
@@ -0,0 +1,300 @@
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

package org.apache.oozie.command;

import java.io.StringReader;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.AppType;
import org.apache.oozie.BaseEngineException;
import org.apache.oozie.BundleEngine;
import org.apache.oozie.BundleJobBean;
import org.apache.oozie.CoordinatorEngine;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.Job;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.service.Services;
import org.apache.oozie.sla.SLACalcStatus;
import org.apache.oozie.sla.SLACalculatorMemory;
import org.apache.oozie.sla.SLAOperations;
import org.apache.oozie.sla.SLARegistrationBean;
import org.apache.oozie.sla.service.SLAService;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.XConfiguration;

public class TestSLAAlertXCommand extends XDataTestCase {
    private Services services;
    SLACalculatorMemory slaCalcMemory;
    BundleJobBean bundle;
    CoordinatorJobBean coord1, coord2;
    final BundleEngine bundleEngine = new BundleEngine("u");
    Date startTime;
    final Date endTime = new Date(System.currentTimeMillis() + 1 * 1 * 3600 * 1000);
    final int timeInSec = 60 * 1000;
    final String data = "2014-01-01T00:00Z";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        Configuration conf = services.get(ConfigurationService.class).getConf();
        conf.set(Services.CONF_SERVICE_EXT_CLASSES, "org.apache.oozie.service.EventHandlerService,"
                + "org.apache.oozie.sla.service.SLAService");
        conf.setInt(SLAService.CONF_SLA_CHECK_INTERVAL, 600);
        services.init();

    }

    @Override
    protected void tearDown() throws Exception {
        LocalOozie.stop();
        services.destroy();
        super.tearDown();
    }

    public void testBundleSLAAlertCommands() throws Exception {
        setupSLAJobs();
        String jobIdsStr = bundle.getId();
        String actions = "1,2";
        String coords = null;
        bundleEngine.disableSLAAlert(jobIdsStr, actions, null, coords);
        checkSLAStatus(coord1.getId() + "@1", true);
        checkSLAStatus(coord1.getId() + "@2", true);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@5", false);
        checkSLAStatus(coord1.getId() + "@4", false);
        checkSLAStatus(coord2.getId() + "@1", true);
        checkSLAStatus(coord2.getId() + "@1", true);

        bundleEngine.enableSLAAlert(jobIdsStr, null, null, null);
        checkSLAStatus(coord1.getId() + "@1", false);
        checkSLAStatus(coord1.getId() + "@2", false);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@5", false);
        checkSLAStatus(coord1.getId() + "@4", false);
        checkSLAStatus(coord2.getId() + "@1", false);
        checkSLAStatus(coord2.getId() + "@2", false);

        CoordinatorJobBean job1 = CoordJobQueryExecutor.getInstance().get(
                CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coord1.getId());
        XConfiguration xConf = new XConfiguration(new StringReader(job1.getConf()));
        assertEquals(xConf.get(OozieClient.SLA_DISABLE_ALERT), null);

        CoordinatorJobBean job2 = CoordJobQueryExecutor.getInstance().get(
                CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coord2.getId());
        xConf = new XConfiguration(new StringReader(job2.getConf()));
        assertEquals(xConf.get(OozieClient.SLA_DISABLE_ALERT), null);

        bundleEngine.disableSLAAlert(jobIdsStr, null, null, "coord1");
        checkSLAStatus(coord1.getId() + "@1", true);
        checkSLAStatus(coord1.getId() + "@2", true);
        checkSLAStatus(coord1.getId() + "@3", true);
        checkSLAStatus(coord1.getId() + "@4", true);
        checkSLAStatus(coord1.getId() + "@5", true);
        checkSLAStatus(coord2.getId() + "@1", false);
        checkSLAStatus(coord2.getId() + "@2", false);

        job1 = CoordJobQueryExecutor.getInstance().get(CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB,
                coord1.getId());
        xConf = new XConfiguration(new StringReader(job1.getConf()));
        assertEquals(xConf.get(OozieClient.SLA_DISABLE_ALERT), SLAOperations.ALL_VALUE);
        bundleEngine.disableSLAAlert(jobIdsStr, null, null, "coord2");
        // with multiple coordID.

        String dates = "2014-01-01T00:00Z::2014-01-03T00:00Z";
        bundleEngine.enableSLAAlert(jobIdsStr, null, dates, "coord1," + coord2.getId());
        checkSLAStatus(coord1.getId() + "@1", false);
        checkSLAStatus(coord1.getId() + "@2", false);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@4", true);
        checkSLAStatus(coord1.getId() + "@5", true);
        checkSLAStatus(coord2.getId() + "@1", false);
        checkSLAStatus(coord2.getId() + "@2", false);
        checkSLAStatus(coord2.getId() + "@3", false);
        checkSLAStatus(coord2.getId() + "@4", true);

        try {
            bundleEngine.disableSLAAlert(jobIdsStr, null, null, "dummy");
            fail("Should throw Exception");
        }
        catch (BaseEngineException e) {
            assertEquals(e.getErrorCode(), ErrorCode.E1026);
        }

    }

    public void testSLAChangeCommand() throws Exception {
        setupSLAJobs();
        String newParams = RestConstants.SLA_SHOULD_END + "=10";
        String jobIdsStr = bundle.getId();
        String coords = coord1.getAppName();
        bundleEngine.changeSLA(jobIdsStr, null, null, coords, newParams);

        assertEquals(getSLACalcStatus(coord1.getId() + "@1").getExpectedEnd().getTime(),
                getSLACalcStatus(coord1.getId() + "@1").getNominalTime().getTime() + 10 * timeInSec);
        assertEquals(getSLACalcStatus(coord1.getId() + "@2").getExpectedEnd().getTime(),
                getSLACalcStatus(coord1.getId() + "@2").getNominalTime().getTime() + 10 * timeInSec);

        assertEquals(getSLACalcStatus(coord1.getId() + "@5").getExpectedEnd().getTime(),
                getSLACalcStatus(coord1.getId() + "@5").getNominalTime().getTime() + 10 * timeInSec);
        newParams = "non-valid-param=10";
        try {
            bundleEngine.changeSLA(jobIdsStr, null, null, coords, newParams);
            fail("Should throw Exception");
        }
        catch (BaseEngineException e) {
            assertEquals(e.getErrorCode(), ErrorCode.E1027);
        }
        try {
            new CoordinatorEngine().changeSLA(coord1.getId(), null, null, null, newParams);
            fail("Should throw Exception");
        }
        catch (BaseEngineException e) {
            assertEquals(e.getErrorCode(), ErrorCode.E1027);
        }
    }

    public void testCoordSLAAlertCommands() throws Exception {
        setupSLAJobs();

        final CoordinatorEngine engine = new CoordinatorEngine("u");
        String jobIdsStr = coord1.getId();
        String actions = "1-3,5";
        String coords = null;
        engine.disableSLAAlert(jobIdsStr, actions, null, coords);
        checkSLAStatus(coord1.getId() + "@1", true);
        checkSLAStatus(coord1.getId() + "@2", true);
        checkSLAStatus(coord1.getId() + "@3", true);
        checkSLAStatus(coord1.getId() + "@5", true);
        checkSLAStatus(coord1.getId() + "@4", false);

        actions = "1-3";
        engine.enableSLAAlert(jobIdsStr, actions, null, null);
        checkSLAStatus(coord1.getId() + "@1", false);
        checkSLAStatus(coord1.getId() + "@2", false);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@5", true);
        checkSLAStatus(coord1.getId() + "@4", false);

        engine.enableSLAAlert(jobIdsStr, null, null, null);
        checkSLAStatus(coord1.getId() + "@1", false);
        checkSLAStatus(coord1.getId() + "@2", false);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@5", false);
        checkSLAStatus(coord1.getId() + "@4", false);
        CoordinatorJobBean job = CoordJobQueryExecutor.getInstance().get(
                CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, jobIdsStr);
        XConfiguration xConf = new XConfiguration(new StringReader(job.getConf()));
        assertEquals(xConf.get(OozieClient.SLA_DISABLE_ALERT), null);

    }

    private void setupSLAJobs() throws Exception {

        coord1 = addRecordToCoordJobTable(Job.Status.RUNNING, true, false);
        Date nominalTime1 = DateUtils.parseDateUTC(data);
        addRecordToCoordActionTable(coord1.getId(), 1, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 1,
                nominalTime1);
        Date nominalTime2 = org.apache.commons.lang.time.DateUtils.addDays(nominalTime1, 1);

        addRecordToCoordActionTable(coord1.getId(), 2, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 1,
                nominalTime2);

        Date nominalTime3 = org.apache.commons.lang.time.DateUtils.addDays(nominalTime1, 2);
        addRecordToCoordActionTable(coord1.getId(), 3, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 1,
                nominalTime3);

        Date nominalTime4 = org.apache.commons.lang.time.DateUtils.addDays(nominalTime1, 3);
        addRecordToCoordActionTable(coord1.getId(), 4, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 1,
                nominalTime4);
        Date nominalTime5 = org.apache.commons.lang.time.DateUtils.addDays(nominalTime1, 4);
        addRecordToCoordActionTable(coord1.getId(), 5, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 1,
                nominalTime5);

        coord2 = addRecordToCoordJobTable(Job.Status.RUNNING, true, false);
        addRecordToCoordActionTable(coord2.getId(), 1, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0,
                nominalTime1);
        addRecordToCoordActionTable(coord2.getId(), 2, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0,
                nominalTime2);
        addRecordToCoordActionTable(coord2.getId(), 3, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0,
                nominalTime3);
        addRecordToCoordActionTable(coord2.getId(), 4, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0,
                nominalTime4);

        bundle = addRecordToBundleJobTable(Job.Status.RUNNING, true);
        coord1.setBundleId(bundle.getId());
        coord1.setAppName("coord1");
        coord1.setStartTime(nominalTime1);
        coord1.setMatThrottling(12);
        coord1.setLastActionNumber(5);
        coord2.setBundleId(bundle.getId());
        coord2.setAppName("coord2");
        CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQuery.UPDATE_COORD_JOB, coord1);
        CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQuery.UPDATE_COORD_JOB, coord2);
        registerSLABean(coord1.getId(), AppType.COORDINATOR_JOB, null, null);
        registerSLABean(coord2.getId(), AppType.COORDINATOR_JOB, null, null);
        registerSLABean(coord1.getId() + "@1", AppType.COORDINATOR_ACTION, coord1.getId(), nominalTime1);
        registerSLABean(coord1.getId() + "@2", AppType.COORDINATOR_ACTION, coord1.getId(), nominalTime2);
        registerSLABean(coord1.getId() + "@3", AppType.COORDINATOR_ACTION, coord1.getId(), nominalTime3);
        registerSLABean(coord1.getId() + "@4", AppType.COORDINATOR_ACTION, coord1.getId(), nominalTime4);
        registerSLABean(coord1.getId() + "@5", AppType.COORDINATOR_ACTION, coord1.getId(), nominalTime5);
        registerSLABean(coord2.getId() + "@1", AppType.COORDINATOR_ACTION, coord2.getId(), nominalTime1);
        registerSLABean(coord2.getId() + "@2", AppType.COORDINATOR_ACTION, coord2.getId(), nominalTime2);
        registerSLABean(coord2.getId() + "@3", AppType.COORDINATOR_ACTION, coord2.getId(), nominalTime3);
        registerSLABean(coord2.getId() + "@4", AppType.COORDINATOR_ACTION, coord2.getId(), nominalTime4);

        checkSLAStatus(coord1.getId() + "@1", false);
        checkSLAStatus(coord1.getId() + "@2", false);
        checkSLAStatus(coord1.getId() + "@3", false);
        checkSLAStatus(coord1.getId() + "@5", false);
        checkSLAStatus(coord1.getId() + "@4", false);
    }

    private void registerSLABean(String jobId, AppType appType, String parentId, Date nominalTime) throws Exception {
        SLARegistrationBean slaRegBean = new SLARegistrationBean();
        slaRegBean.setNominalTime(nominalTime);
        slaRegBean.setId(jobId);
        slaRegBean.setAppType(appType);
        startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000); // 1 hour back
        slaRegBean.setExpectedStart(startTime);
        slaRegBean.setExpectedDuration(3600 * 1000);
        slaRegBean.setParentId(parentId);
        slaRegBean.setExpectedEnd(endTime); // 1 hour ahead
        Services.get().get(SLAService.class).addRegistrationEvent(slaRegBean);
    }

    private void checkSLAStatus(String id, boolean status) throws JPAExecutorException {
        assertEquals(getSLACalcStatus(id).getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT), status);
    }

    private SLACalcStatus getSLACalcStatus(String jobId) throws JPAExecutorException {
        return Services.get().get(SLAService.class).getSLACalculator().get(jobId);

    }
}
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordSubmitXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordSubmitXCommand.java
index 5ce9a7fb9..5f72e5704 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordSubmitXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordSubmitXCommand.java
@@ -23,6 +23,7 @@ import java.io.FileWriter;
 import java.io.Reader;
 import java.io.Writer;
 import java.net.URI;
import java.util.Date;
 import java.util.List;
 
 import org.apache.hadoop.conf.Configuration;
@@ -31,13 +32,22 @@ import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.Job;
 import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.executor.jpa.CoordJobGetJPAExecutor;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
import org.apache.oozie.sla.SLACalcStatus;
import org.apache.oozie.sla.SLACalculator;
import org.apache.oozie.sla.SLAOperations;
import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.JobUtils;
 import org.apache.oozie.util.XConfiguration;
 import org.apache.oozie.util.XmlUtils;
 import org.jdom.Element;
@@ -51,6 +61,8 @@ public class TestCoordSubmitXCommand extends XDataTestCase {
     protected void setUp() throws Exception {
         super.setUp();
         services = new Services();
        services.getConf().set(Services.CONF_SERVICE_EXT_CLASSES,
                EventHandlerService.class.getName() + "," + SLAService.class.getName());
         services.init();
     }
 
@@ -1319,4 +1331,170 @@ public class TestCoordSubmitXCommand extends XDataTestCase {
         assertEquals(job.getTimeout(), 43200);
     }
 
    public void testSubmitWithSLAAlertsDisable() throws Exception {
        Configuration conf = new XConfiguration();
        File appPathFile = new File(getTestCaseDir(), "coordinator.xml");

        // CASE 1: Failure case i.e. multiple data-in instances
        Reader reader = IOUtils.getResourceAsReader("coord-action-sla.xml", -1);
        Writer writer = new FileWriter(appPathFile);
        IOUtils.copyCharStream(reader, writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set("start", DateUtils.formatDateOozieTZ(new Date()));
        conf.set("end", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), 1)));
        conf.set("frequency", "coord:days(1)");
        conf.set(OozieClient.USER_NAME, getTestUser());
        reader = IOUtils.getResourceAsReader("wf-credentials.xml", -1);
        appPathFile = new File(getTestCaseDir(), "workflow.xml");
        writer = new FileWriter(appPathFile);
        IOUtils.copyCharStream(reader, writer);
        conf.set("wfAppPath", appPathFile.getPath());
        Date nominalTime = new Date();
        conf.set("nominal_time", DateUtils.formatDateOozieTZ(nominalTime));

        String coordId = new CoordSubmitXCommand(conf).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        SLAService slaService = services.get(SLAService.class);
        SLACalculator calc = slaService.getSLACalculator();
        SLACalcStatus slaCalc = calc.get(coordId + "@" + 1);
        assertFalse(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        Configuration conf1=new Configuration(conf);
        // CASE I: "ALL"
        conf1.set(OozieClient.SLA_DISABLE_ALERT, "ALL");
        coordId = new CoordSubmitXCommand(conf1).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();

        slaService = services.get(SLAService.class);
        calc = slaService.getSLACalculator();
        slaCalc = calc.get(coordId + "@" + 1);
        assertTrue(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        // CASE II: Date Range
        Configuration conf2=new Configuration(conf);
        Date startRangeDate = new Date(nominalTime.getTime() - 3600 * 1000);
        conf2.set(OozieClient.SLA_DISABLE_ALERT,
                DateUtils.formatDateOozieTZ(startRangeDate) + "::" + DateUtils.formatDateOozieTZ(nominalTime));
        coordId = new CoordSubmitXCommand(conf2).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();

        slaCalc = calc.get(coordId + "@" + 1);
        assertTrue(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        // CASE III: Coord name (negative test)
        Configuration conf3=new Configuration(conf);
        conf3.set(OozieClient.SLA_DISABLE_ALERT_COORD, "test-coord-sla-x");
        coordId = new CoordSubmitXCommand(conf3).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        slaCalc = calc.get(coordId + "@" + 1);
        assertFalse(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        // CASE IV: Older than n(hours)
        Date otherNominalTime = new Date(nominalTime.getTime() - 73 * 3600 * 1000);
        conf = new XConfiguration();
        appPathFile = new File(getTestCaseDir(), "coordinator.xml");
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set("wfAppPath", appPathFile.getPath());
        conf.set("start", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), -1)));
        conf.set("end", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), 1)));

        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nominal_time", DateUtils.formatDateOozieTZ(otherNominalTime));
        conf.setInt(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN, 72);
        coordId = new CoordSubmitXCommand(conf).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        slaCalc = calc.get(coordId + "@" + 1);
        assertTrue(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        // catchup mode
        conf = new XConfiguration();
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set("wfAppPath", appPathFile.getPath());
        conf.set("start", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), -1)));
        conf.set("end", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), 1)));

        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nominal_time",
                DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), -1)));
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nominal_time",
                DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), -1)));
        coordId = new CoordSubmitXCommand(conf).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        slaCalc = calc.get(coordId + "@" + 1);
        assertTrue(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

        // normal mode
        conf = new XConfiguration();
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set("wfAppPath", appPathFile.getPath());
        conf.set("start", DateUtils.formatDateOozieTZ(new Date()));
        conf.set("end", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), 1)));

        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nominal_time", DateUtils.formatDateOozieTZ(new Date()));
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nominal_time", DateUtils.formatDateOozieTZ(new Date()));
        coordId = new CoordSubmitXCommand(conf).call();
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        slaCalc = calc.get(coordId + "@" + 1);
        assertFalse(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));

    }

    public void testSLAAlertWithNewlyCreatedActions() throws Exception {
        Configuration conf = new XConfiguration();
        File appPathFile = new File(getTestCaseDir(), "coordinator.xml");

        // CASE 1: Failure case i.e. multiple data-in instances
        Reader reader = IOUtils.getResourceAsReader("coord-action-sla.xml", -1);
        Writer writer = new FileWriter(appPathFile);
        IOUtils.copyCharStream(reader, writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPathFile.toURI().toString());
        conf.set("start", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addDays(new Date(), -1)));
        conf.set("end", DateUtils.formatDateOozieTZ(org.apache.commons.lang.time.DateUtils.addMonths(new Date(), 1)));
        conf.set(OozieClient.USER_NAME, getTestUser());
        reader = IOUtils.getResourceAsReader("wf-credentials.xml", -1);
        appPathFile = new File(getTestCaseDir(), "workflow.xml");
        writer = new FileWriter(appPathFile);
        IOUtils.copyCharStream(reader, writer);
        conf.set("wfAppPath", appPathFile.getPath());
        Date nominalTime = new Date();
        conf.set("nominal_time", DateUtils.formatDateOozieTZ(nominalTime));

        String coordId = new CoordSubmitXCommand(conf).call();
        CoordinatorJobBean job = CoordJobQueryExecutor.getInstance().get(
                CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coordId);
        job.setMatThrottling(1);
        CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQueryExecutor.CoordJobQuery.UPDATE_COORD_JOB, job);
        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        SLAService slaService = services.get(SLAService.class);
        SLACalculator calc = slaService.getSLACalculator();
        SLACalcStatus slaCalc = calc.get(coordId + "@" + 1);
        assertFalse(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));
        assertEquals(slaCalc.getExpectedDuration(), 1800000);
        job = CoordJobQueryExecutor.getInstance().get(CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coordId);
        assertEquals(job.getLastActionNumber(), 1);

        String newParams = RestConstants.SLA_MAX_DURATION + "=${5 * MINUTES}";

        new CoordSLAChangeXCommand(coordId, null, null, JobUtils.parseChangeValue(newParams)).call();
        new CoordSLAAlertsDisableXCommand(coordId, null, null).call();

        job = CoordJobQueryExecutor.getInstance().get(CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coordId);
        job.setMatThrottling(2);
        CoordJobQueryExecutor.getInstance().executeUpdate(CoordJobQueryExecutor.CoordJobQuery.UPDATE_COORD_JOB, job);

        job = CoordJobQueryExecutor.getInstance().get(CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coordId);

        new CoordMaterializeTransitionXCommand(coordId, 3600).call();
        job = CoordJobQueryExecutor.getInstance().get(CoordJobQueryExecutor.CoordJobQuery.GET_COORD_JOB, coordId);
        slaCalc = calc.get(coordId + "@" + job.getLastActionNumber());
        assertEquals(slaCalc.getExpectedDuration(), 300000);
        // newly action should have sla disable after coord disable command on coord job
        assertTrue(Boolean.valueOf(slaCalc.getSLAConfigMap().get(OozieClient.SLA_DISABLE_ALERT)));
        Element eAction = XmlUtils.parseXml(job.getJobXml());
        Element eSla = eAction.getChild("action", eAction.getNamespace()).getChild("info", eAction.getNamespace("sla"));
        assertEquals(SLAOperations.getTagElement(eSla, "max-duration"), "${5 * MINUTES}");
    }
 }
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionsForDatesJPAExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionQueryExecutor.java
similarity index 52%
rename from core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionsForDatesJPAExecutor.java
rename to core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionQueryExecutor.java
index 293d9259c..85ff5d273 100644
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionsForDatesJPAExecutor.java
++ b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordActionQueryExecutor.java
@@ -26,13 +26,13 @@ import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.test.XDataTestCase;
 import org.apache.oozie.util.DateUtils;
 
public class TestCoordJobGetActionsForDatesJPAExecutor extends XDataTestCase {
public class TestCoordActionQueryExecutor extends XDataTestCase {

     Services services;
 
     @Override
@@ -48,7 +48,37 @@ public class TestCoordJobGetActionsForDatesJPAExecutor extends XDataTestCase {
         super.tearDown();
     }
 
    public void testCoordActionGet() throws Exception {
    public void testGetTerminatedActionForDates() throws Exception {
        int actionNum = 1;
        CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.FAILED, "coord-action-get.xml", 0);

        Path appPath = new Path(getFsTestCaseDir(), "coord");
        String actionXml = getCoordActionXml(appPath, "coord-action-get.xml");
        String actionNomialTime = getActionNominalTime(actionXml);
        Date nominalTime = DateUtils.parseDateOozieTZ(actionNomialTime);

        Date d1 = new Date(nominalTime.getTime() - 1000);
        Date d2 = new Date(nominalTime.getTime() + 1000);
        _testGetTerminatedActionForDates(job.getId(), d1, d2, 1);

        d1 = new Date(nominalTime.getTime() + 1000);
        d2 = new Date(nominalTime.getTime() + 2000);
        _testGetTerminatedActionForDates(job.getId(), d1, d2, 0);

        cleanUpDBTables();
        job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0);
        _testGetTerminatedActionForDates(job.getId(), d1, d2, 0);
    }

    private void _testGetTerminatedActionForDates(String jobId, Date d1, Date d2, int expected) throws Exception {
        List<CoordinatorActionBean> actionIds = CoordActionQueryExecutor.getInstance().getList(
                CoordActionQuery.GET_TERMINATED_ACTIONS_FOR_DATES, jobId, d1, d2);
        assertEquals(expected, actionIds.size());
    }

    public void testGetTerminatedActionIdsForDates() throws Exception {
         int actionNum = 1;
         CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
         addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.FAILED, "coord-action-get.xml", 0);
@@ -60,23 +90,21 @@ public class TestCoordJobGetActionsForDatesJPAExecutor extends XDataTestCase {
 
         Date d1 = new Date(nominalTime.getTime() - 1000);
         Date d2 = new Date(nominalTime.getTime() + 1000);
        _testGetActionForDates(job.getId(), d1, d2, 1);
        _testGetTerminatedActionIdsForDates(job.getId(), d1, d2, 1);
 
         d1 = new Date(nominalTime.getTime() + 1000);
         d2 = new Date(nominalTime.getTime() + 2000);
        _testGetActionForDates(job.getId(), d1, d2, 0);
        _testGetTerminatedActionIdsForDates(job.getId(), d1, d2, 0);
 
         cleanUpDBTables();
         job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
         addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0);
        _testGetActionForDates(job.getId(), d1, d2, 0);
        _testGetTerminatedActionIdsForDates(job.getId(), d1, d2, 0);
     }
 
    private void _testGetActionForDates(String jobId, Date d1, Date d2, int expected) throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);
        CoordJobGetActionsForDatesJPAExecutor actionGetCmd = new CoordJobGetActionsForDatesJPAExecutor(jobId, d1, d2);
        List<CoordinatorActionBean> actions = jpaService.execute(actionGetCmd);
    private void _testGetTerminatedActionIdsForDates(String jobId, Date d1, Date d2, int expected) throws Exception {
        List<CoordinatorActionBean> actions = CoordActionQueryExecutor.getInstance().getList(
                CoordActionQuery.GET_TERMINATED_ACTION_IDS_FOR_DATES, jobId, d1, d2);
         assertEquals(expected, actions.size());
     }
 
diff --git a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionIdsForDatesJPAExecutor.java b/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionIdsForDatesJPAExecutor.java
deleted file mode 100644
index 9d92256cf..000000000
-- a/core/src/test/java/org/apache/oozie/executor/jpa/TestCoordJobGetActionIdsForDatesJPAExecutor.java
++ /dev/null
@@ -1,82 +0,0 @@
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

package org.apache.oozie.executor.jpa;

import java.util.Date;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.oozie.CoordinatorJobBean;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.local.LocalOozie;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;

public class TestCoordJobGetActionIdsForDatesJPAExecutor extends XDataTestCase {
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

    public void testCoordActionGet() throws Exception {
        int actionNum = 1;
        CoordinatorJobBean job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.FAILED, "coord-action-get.xml", 0);

        Path appPath = new Path(getFsTestCaseDir(), "coord");
        String actionXml = getCoordActionXml(appPath, "coord-action-get.xml");
        String actionNomialTime = getActionNominalTime(actionXml);
        Date nominalTime = DateUtils.parseDateOozieTZ(actionNomialTime);

        Date d1 = new Date(nominalTime.getTime() - 1000);
        Date d2 = new Date(nominalTime.getTime() + 1000);
        _testGetActionForDates(job.getId(), d1, d2, 1);

        d1 = new Date(nominalTime.getTime() + 1000);
        d2 = new Date(nominalTime.getTime() + 2000);
        _testGetActionForDates(job.getId(), d1, d2, 0);

        cleanUpDBTables();
        job = addRecordToCoordJobTable(CoordinatorJob.Status.RUNNING, false, false);
        addRecordToCoordActionTable(job.getId(), actionNum, CoordinatorAction.Status.WAITING, "coord-action-get.xml", 0);
        _testGetActionForDates(job.getId(), d1, d2, 0);
    }

    private void _testGetActionForDates(String jobId, Date d1, Date d2, int expected) throws Exception {
        JPAService jpaService = Services.get().get(JPAService.class);
        assertNotNull(jpaService);
        CoordJobGetActionIdsForDateRangeJPAExecutor actionGetCmd = new CoordJobGetActionIdsForDateRangeJPAExecutor(jobId, d1, d2);
        List<String> actionIds = jpaService.execute(actionGetCmd);
        assertEquals(expected, actionIds.size());
    }

}
diff --git a/core/src/test/java/org/apache/oozie/service/TestHASLAService.java b/core/src/test/java/org/apache/oozie/service/TestHASLAService.java
index 5aa911b24..795db3731 100644
-- a/core/src/test/java/org/apache/oozie/service/TestHASLAService.java
++ b/core/src/test/java/org/apache/oozie/service/TestHASLAService.java
@@ -23,6 +23,7 @@ import java.util.ArrayList;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
import java.util.Map;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.AppType;
@@ -30,11 +31,13 @@ import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.WorkflowJobBean;
 import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowJob;
 import org.apache.oozie.client.event.JobEvent.EventStatus;
 import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.client.event.SLAEvent.SLAStatus;
 import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.event.EventQueue;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
@@ -52,6 +55,8 @@ import org.apache.oozie.sla.TestSLAService;
 import org.apache.oozie.sla.listener.SLAEventListener;
 import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.test.ZKXTestCase;
import org.apache.oozie.util.JobUtils;
import org.apache.oozie.util.Pair;
 import org.apache.oozie.workflow.WorkflowInstance;
 
 public class TestHASLAService extends ZKXTestCase {
@@ -358,6 +363,72 @@ public class TestHASLAService extends ZKXTestCase {
         }
     }
 
    public void testSLAAlertCommandWithHA() throws Exception {

        //Test SLA ALERT commands in HA mode.
        //slaCalcMem1 is for server 1 and slaCalcMem2 is for server2

        String id = "0000001-130521183438837-oozie-test-C@1";
        Date expectedStartTS = new Date(System.currentTimeMillis() - 2 * 3600 * 1000); // 2 hrs passed
        Date expectedEndTS1 = new Date(System.currentTimeMillis() + 1 * 3600 * 1000); // 1 hour ahead
        // Coord Action of jobs 1-4 not started yet
        createDBEntry(id, expectedStartTS, expectedEndTS1);

        SLAService slas = Services.get().get(SLAService.class);
        SLACalculatorMemory slaCalcMem1 = (SLACalculatorMemory) slas.getSLACalculator();
        slaCalcMem1.init(Services.get().get(ConfigurationService.class).getConf());
        List<String> idList = new ArrayList<String>();
        idList.add(id);
        slaCalcMem1.disableAlert(idList);
        assertTrue(slaCalcMem1.get(id).getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT));

        DummyZKOozie dummyOozie_1 = null;
        try {
            // start another dummy oozie instance (dummy sla and event handler services)
            dummyOozie_1 = new DummyZKOozie("a", "http://blah");
            DummySLACalculatorMemory slaCalcMem2 = new DummySLACalculatorMemory();
            EventHandlerService dummyEhs = new EventHandlerService();
            slaCalcMem2.setEventHandlerService(dummyEhs);

            // So that job sla updated doesn't run automatically
            Services.get().get(ConfigurationService.class).getConf().setInt(SLAService.CONF_SLA_CHECK_INTERVAL, 100000);
            Services.get().get(ConfigurationService.class).getConf().setInt(SLAService.CONF_SLA_CHECK_INITIAL_DELAY, 100000);
            dummyEhs.init(Services.get());
            slaCalcMem2.init(Services.get().get(ConfigurationService.class).getConf());

            slaCalcMem2.updateAllSlaStatus();
            assertTrue(slaCalcMem2.get(id).getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT));

            String newParams = RestConstants.SLA_MAX_DURATION + "=5";
            List<Pair<String, Map<String, String>>> jobIdsSLAPair = new ArrayList<Pair<String, Map<String, String>>>();
            jobIdsSLAPair.add(new Pair<String, Map<String, String>>(id, JobUtils.parseChangeValue(newParams)));
            slaCalcMem1.changeDefinition(jobIdsSLAPair);
            assertEquals(slaCalcMem1.get(id).getExpectedDuration(), 5 * 60 * 1000);

            //Before update, default is 10.
            assertEquals(slaCalcMem2.get(id).getExpectedDuration(), 10 * 60 * 1000);

            slaCalcMem2.updateAllSlaStatus();
            assertEquals(slaCalcMem2.get(id).getExpectedDuration(), 5 * 60 * 1000);

            newParams = RestConstants.SLA_MAX_DURATION + "=15";
            jobIdsSLAPair.clear();
            jobIdsSLAPair.add(new Pair<String, Map<String, String>>(id, JobUtils.parseChangeValue(newParams)));
            slaCalcMem1.changeDefinition(jobIdsSLAPair);

            // Before update
            assertEquals(slaCalcMem2.get(id).getExpectedDuration(), 5 * 60 * 1000);
            slaCalcMem2.updateAllSlaStatus();
            assertEquals(slaCalcMem2.get(id).getExpectedDuration(), 15 * 60 * 1000);

        }
        finally {
            if (dummyOozie_1 != null) {
                dummyOozie_1.teardown();
            }
        }
    }

     private void createDBEntry(String actionId, Date expectedStartTS, Date expectedEndTS) throws Exception {
         ArrayList<JsonBean> insertList = new ArrayList<JsonBean>();
         CoordinatorActionBean coordAction = new CoordinatorActionBean();
diff --git a/core/src/test/java/org/apache/oozie/servlet/TestV2SLAServlet.java b/core/src/test/java/org/apache/oozie/servlet/TestV2SLAServlet.java
index 5f51b22da..1886f4833 100644
-- a/core/src/test/java/org/apache/oozie/servlet/TestV2SLAServlet.java
++ b/core/src/test/java/org/apache/oozie/servlet/TestV2SLAServlet.java
@@ -39,8 +39,6 @@ import org.apache.oozie.client.rest.JsonTags;
 import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.service.JPAService;
import org.apache.oozie.service.Services;
 import org.apache.oozie.sla.SLASummaryBean;
 import org.apache.oozie.util.DateUtils;
 import org.json.simple.JSONArray;
diff --git a/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java b/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
index c70ef794e..432efef69 100644
-- a/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
++ b/core/src/test/java/org/apache/oozie/sla/TestSLACalculatorMemory.java
@@ -20,8 +20,10 @@ package org.apache.oozie.sla;
 
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
import java.util.Map;
 
 import org.apache.hadoop.conf.Configuration;
 import org.apache.log4j.Level;
@@ -31,28 +33,36 @@ import org.apache.oozie.AppType;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.WorkflowActionBean;
 import org.apache.oozie.WorkflowJobBean;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.WorkflowAction;
 import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.client.event.JobEvent.EventStatus;
import org.apache.oozie.client.event.SLAEvent;
 import org.apache.oozie.client.event.SLAEvent.SLAStatus;
 import org.apache.oozie.client.rest.JsonBean;
import org.apache.oozie.client.rest.RestConstants;
import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.CoordActionInsertJPAExecutor;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor;
 import org.apache.oozie.executor.jpa.SLARegistrationQueryExecutor.SLARegQuery;
import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor.SLASummaryQuery;
import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor;
import org.apache.oozie.executor.jpa.SLASummaryQueryExecutor.SLASummaryQuery;
 import org.apache.oozie.executor.jpa.WorkflowActionInsertJPAExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor;
 import org.apache.oozie.executor.jpa.WorkflowJobQueryExecutor.WorkflowJobQuery;
import org.apache.oozie.service.ConfigurationService;
 import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.sla.service.SLAService;
 import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.JobUtils;
import org.apache.oozie.util.Pair;
 import org.apache.oozie.workflow.WorkflowInstance;
 import org.junit.After;
 import org.junit.Before;
@@ -67,9 +77,10 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     protected void setUp() throws Exception {
         super.setUp();
         Services services = new Services();
        Configuration conf = services.getConf();
        Configuration conf = services.get(ConfigurationService.class).getConf();
         conf.set(Services.CONF_SERVICE_EXT_CLASSES, "org.apache.oozie.service.EventHandlerService,"
                 + "org.apache.oozie.sla.service.SLAService");
        conf.setInt(SLAService.CONF_SLA_CHECK_INTERVAL, 600);
         services.init();
         jpaService = Services.get().get(JPAService.class);
     }
@@ -96,7 +107,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     @Test
     public void testLoadOnRestart() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job-1", AppType.WORKFLOW_JOB);
         String jobId1 = slaRegBean1.getId();
         SLARegistrationBean slaRegBean2 = _createSLARegistration("job-2", AppType.WORKFLOW_JOB);
@@ -156,7 +167,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         BatchQueryExecutor.getInstance().executeBatchInsertUpdateDelete(null, updateList, null);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         assertEquals(2, slaCalcMemory.size());
 
@@ -201,7 +212,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testWorkflowJobSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job-1", AppType.WORKFLOW_JOB);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -228,7 +239,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         WorkflowJobQueryExecutor.getInstance().insert(wjb);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -257,7 +268,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_ALL, slaSummaryBean);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         assertEquals(0, slaCalcMemory.size());
         slaSummary = SLASummaryQueryExecutor.getInstance().get(SLASummaryQuery.GET_SLA_SUMMARY, jobId1);
@@ -281,7 +292,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_ALL, slaSummaryBean);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         assertEquals(1, slaCalcMemory.size());
         SLACalcStatus calc = slaCalcMemory.get(jobId1);
@@ -297,7 +308,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testWorkflowActionSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job@1", AppType.WORKFLOW_ACTION);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -322,7 +333,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         jpaService.execute(wfInsertCmd);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -343,7 +354,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testCoordinatorActionSLAStatusOnRestart() throws Exception {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         SLARegistrationBean slaRegBean1 = _createSLARegistration("job@1", AppType.COORDINATOR_ACTION);
         String jobId1 = slaRegBean1.getId();
         slaRegBean1.setExpectedEnd(sdf.parse("2013-03-07"));
@@ -373,7 +384,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         WorkflowJobQueryExecutor.getInstance().insert(wjb);
 
         slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         // As job succeeded, it should not be in memory
         assertEquals(0, slaCalcMemory.size());
@@ -394,7 +405,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAEvents1() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         slaRegBean.setExpectedStart(new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000)); // 1 hour
@@ -445,7 +456,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAEvents2() throws Exception {
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
 
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
@@ -505,7 +516,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
         // test start-miss
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000); // 1 hour back
@@ -534,7 +545,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testDuplicateEndMiss() throws Exception {
         EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.RUNNING, WorkflowInstance.Status.RUNNING);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() + 1 * 1 * 3600 * 1000); // 1 hour ahead
@@ -577,7 +588,7 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     public void testSLAHistorySet() throws Exception {
             EventHandlerService ehs = Services.get().get(EventHandlerService.class);
             SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
            slaCalcMemory.init(Services.get().getConf());
            slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
             WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
             SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
             Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000);
@@ -612,9 +623,8 @@ public class TestSLACalculatorMemory extends XDataTestCase {
     }
 
     public void testHistoryPurge() throws Exception{
        EventHandlerService ehs = Services.get().get(EventHandlerService.class);
         SLACalculatorMemory slaCalcMemory = new SLACalculatorMemory();
        slaCalcMemory.init(Services.get().getConf());
        slaCalcMemory.init(Services.get().get(ConfigurationService.class).getConf());
         WorkflowJobBean job1 = addRecordToWfJobTable(WorkflowJob.Status.PREP, WorkflowInstance.Status.PREP);
         SLARegistrationBean slaRegBean = _createSLARegistration(job1.getId(), AppType.WORKFLOW_JOB);
         Date startTime = new Date(System.currentTimeMillis() - 1 * 1 * 3600 * 1000);
@@ -662,4 +672,75 @@ public class TestSLACalculatorMemory extends XDataTestCase {
 
     }
 
    @SuppressWarnings("serial")
    public void testDisablingAlertsEvents() throws Exception {
        SLAService slaService = Services.get().get(SLAService.class);
        EventHandlerService ehs = Services.get().get(EventHandlerService.class);
        SLACalculator slaCalculator = slaService.getSLACalculator();
        // create dummy sla records and coord action records
        String id1 = _setupSlaMap(slaCalculator, "00020-1234567-wrkf-C", 1);
        String id2 = _setupSlaMap(slaCalculator, "00020-1234567-wrkf-C", 2);

        SLACalcStatus slaCalcObj1 = slaCalculator.get(id1);
        assertFalse(slaCalcObj1.getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT));
        SLACalcStatus slaCalcObj2 = slaCalculator.get(id2);
        assertFalse(slaCalcObj2.getSLAConfigMap().containsKey(OozieClient.SLA_DISABLE_ALERT));
        slaCalculator.updateAllSlaStatus();
        assertTrue(ehs.getEventQueue().size() > 0);

        // check that SLACalculator sends no event
        ehs.getEventQueue().clear();
        SLASummaryBean persistentSla = new SLASummaryBean(slaCalcObj1);
        // reset eventProcessed for the sla calc objects
        persistentSla.setEventProcessed(0);
        SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_EVENTPROCESSED,
                persistentSla);
        persistentSla = new SLASummaryBean(slaCalcObj2);
        persistentSla.setEventProcessed(0);
        SLASummaryQueryExecutor.getInstance().executeUpdate(SLASummaryQuery.UPDATE_SLA_SUMMARY_EVENTPROCESSED,
                persistentSla);
        // CASE I : list of sla ids, no new params
        slaService.enableChildJobAlert(Arrays.asList(id1, id2));
        slaCalculator.updateAllSlaStatus();
        assertTrue(ehs.getEventQueue().isEmpty());

        // CASE II : ALL
        _setupSlaMap(slaCalculator, "00020-1234567-wrkf-C", 3);
        _setupSlaMap(slaCalculator, "00020-1234567-wrkf-C", 4);
        slaCalculator.enableChildJobAlert(Arrays.asList("00020-1234567-wrkf-C"));
        slaCalculator.updateAllSlaStatus();
        assertFalse(ehs.getEventQueue().isEmpty());

        // CASE III : resume w/ new params
        final String id5 = _setupSlaMap(slaCalculator, "00020-1234567-wrkf-C", 5);
        Date now = new Date();
        now.setTime(now.getTime() - 10 * 60 * 1000);
       final  String newParams = RestConstants.SLA_NOMINAL_TIME + "=" + DateUtils.formatDateOozieTZ(now) + ";"
                + RestConstants.SLA_SHOULD_END + "=5";
        slaCalculator.changeDefinition(new ArrayList<Pair<String,Map<String,String>>>(){
            {
            add(new Pair<String,Map<String,String>>(id5, JobUtils.parseChangeValue(newParams)));
            }
        });

        slaCalculator.updateAllSlaStatus();
        assertTrue(ehs.getEventQueue().size() > 0);

    }

    private String _setupSlaMap(SLACalculator slaCalculator, String id, int actionNum) throws Exception {
        CoordinatorActionBean action = addRecordToCoordActionTable(id, actionNum,
                CoordinatorAction.Status.TIMEDOUT, "coord-action-get.xml", 0);
        action.setExternalId(null);
        CoordActionQueryExecutor.getInstance().executeUpdate(CoordActionQuery.UPDATE_COORD_ACTION_FOR_START, action);
        SLARegistrationBean slaRegBean = _createSLARegistration(action.getId(), AppType.COORDINATOR_ACTION);
        Date startTime = new Date(System.currentTimeMillis() - 2 * 3600 * 1000);
        slaRegBean.setExpectedStart(startTime); // 2 hours back
        slaRegBean.setExpectedDuration(1000);
        slaRegBean.setExpectedEnd(new Date(System.currentTimeMillis() - 1 * 3600 * 1000)); // 1 hr back
        slaRegBean.setParentId(id);
        slaCalculator.addRegistration(slaRegBean.getId(), slaRegBean);
        return action.getId();
    }

 }
diff --git a/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java b/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
index ea82baaf6..7a710c28c 100644
-- a/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
++ b/core/src/test/java/org/apache/oozie/sla/TestSLAEventGeneration.java
@@ -112,6 +112,7 @@ public class TestSLAEventGeneration extends XDataTestCase {
         conf.setInt(EventHandlerService.CONF_WORKER_INTERVAL, 10000);
         conf.setInt(EventHandlerService.CONF_WORKER_THREADS, 0);
         conf.setInt(EventHandlerService.CONF_BATCH_SIZE, 1);
        conf.setInt(OozieClient.SLA_DISABLE_ALERT_OLDER_THAN, -1);
         services.init();
         jpa = services.get(JPAService.class);
         ehs = services.get(EventHandlerService.class);
@@ -409,6 +410,9 @@ public class TestSLAEventGeneration extends XDataTestCase {
         Date nominal = cal.getTime();
         String nominalTime = DateUtils.formatDateOozieTZ(nominal);
         conf.set("nominal_time", nominalTime);
        conf.set("start", "2009-01-02T08:01Z");
        conf.set("frequency", "coord:days(1)");
        conf.set("end", "2009-01-03T08:00Z");
         cal.setTime(nominal);
         cal.add(Calendar.MINUTE, 10); // as per the sla xml
         String expectedStart = DateUtils.formatDateOozieTZ(cal.getTime());
diff --git a/core/src/test/java/org/apache/oozie/sla/TestSLARegistrationGetJPAExecutor.java b/core/src/test/java/org/apache/oozie/sla/TestSLARegistrationGetJPAExecutor.java
index fe9002ce1..d56e06af7 100644
-- a/core/src/test/java/org/apache/oozie/sla/TestSLARegistrationGetJPAExecutor.java
++ b/core/src/test/java/org/apache/oozie/sla/TestSLARegistrationGetJPAExecutor.java
@@ -61,11 +61,27 @@ public class TestSLARegistrationGetJPAExecutor extends XDataTestCase {
         assertEquals(jobId, bean.getId());
         assertEquals(AppType.WORKFLOW_JOB, bean.getAppType());
         assertEquals(current, bean.getExpectedStart());
        assertEquals(2, bean.getSlaConfigMap().size());
        assertEquals(2, bean.getSLAConfigMap().size());
         assertEquals("END_MISS", bean.getAlertEvents());
         assertEquals("alert@example.com", bean.getAlertContact());
     }
 
    public void testSLARegistrationBulkConfigMap() throws Exception {
        Date current = new Date();
        String jobId = "0000000-" + current.getTime() + "-TestSLARegGetJPAExecutor-C@1";
        List<String> jobIds = new ArrayList<String>();
        jobIds.add(jobId);
        _addRecordToSLARegistrationTable(jobId, AppType.COORDINATOR_ACTION, current, new Date(), "END_MISS",
                "alert@example.com");
        jobId = "0000000-" + current.getTime() + "-TestSLARegGetJPAExecutor-C@2";
        jobIds.add(jobId);
        _addRecordToSLARegistrationTable(jobId, AppType.COORDINATOR_ACTION, current, new Date(), "END_MISS",
                "alert@example.com");
        List<SLARegistrationBean> bean = SLARegistrationQueryExecutor.getInstance().getList(
                SLARegQuery.GET_SLA_CONFIGS, jobIds);
        assertEquals(bean.size(), 2);
    }

     private void _addRecordToSLARegistrationTable(String jobId, AppType appType, Date start, Date end,
             String alertEvent, String alertContact) throws Exception {
         SLARegistrationBean reg = new SLARegistrationBean();
@@ -92,7 +108,7 @@ public class TestSLARegistrationGetJPAExecutor extends XDataTestCase {
         String slaConfig = "{alert_contact=hadoopqa@oozie.com},{alert_events=START_MISS,DURATION_MISS,END_MISS},";
         SLARegistrationBean bean = new SLARegistrationBean();
         bean.setSlaConfig(slaConfig);
        assertEquals(bean.getSlaConfigMap().size(), 2);
        assertEquals(bean.getSLAConfigMap().size(), 2);
         assertEquals(bean.getAlertEvents(), "START_MISS,DURATION_MISS,END_MISS");
         assertEquals(bean.getAlertContact(), "hadoopqa@oozie.com");
     }
diff --git a/core/src/test/resources/coord-action-sla.xml b/core/src/test/resources/coord-action-sla.xml
index 8b301fd1c..f3f1bc09c 100644
-- a/core/src/test/resources/coord-action-sla.xml
++ b/core/src/test/resources/coord-action-sla.xml
@@ -16,7 +16,7 @@
   limitations under the License.
 -->
 <coordinator-app name="test-coord-sla" frequency="${coord:days(1)}"
                 start="2009-01-02T08:01Z" end="2009-01-03T08:00Z"
                 start="${start}" end="${end}"
                  timezone="America/Los_Angeles"
                  xmlns="uri:oozie:coordinator:0.4"
                  xmlns:sla="uri:oozie:sla:0.2">
diff --git a/docs/src/site/twiki/DG_CommandLineTool.twiki b/docs/src/site/twiki/DG_CommandLineTool.twiki
index 0f1768b33..bd53bf942 100644
-- a/docs/src/site/twiki/DG_CommandLineTool.twiki
++ b/docs/src/site/twiki/DG_CommandLineTool.twiki
@@ -91,7 +91,10 @@ usage:
                 -value <arg>          new endtime/concurrency/pausetime value for changing a
                                       coordinator job
                 -verbose              verbose mode
.
                -sladisable           disables sla alerts for the job and its children
                -slaenable            enables sla alerts for the job and its children
                -slachange            Update sla param for jobs, supported param are should-start, should-end and max-duration

       oozie jobs <OPTIONS> : jobs status
                  -auth <arg>          select authentication type [SIMPLE|KERBEROS]
                  -doas <arg>          doAs user, impersonates as the specified user.
@@ -889,6 +892,23 @@ All other arguments are optional:
    * =interval=  allows specifying the polling interval in minutes (default is 5)
    * =timeout= allows specifying the timeout in minutes (default is 30 minutes); negative values indicate no timeout
 
---+++ Changing job SLA definition and alerting
   * slaenable command can be used to enable job sla alerts.
   * sladisable command can be used to disable job sla alerts.
   * slachange command can be used to change sla job definition.
   * Supported parameters for sla change command are should-start, should-end and max-duration. Please specify the value in single quotes instead of double quotes in command line to avoid bash interpreting braces in EL functions and causing error.
   * All sla commands takes -action or -date parameter. For bundle jobs additional -coordinator (coord_name/id) parameter can be passed. Sla change command need extra parameter -value to specify new sla definition.
   * Sla commands without -action or -date parameter is applied to all non terminated actions and all future actions.
   * Sla commands with -action or -date parameter will be applied to only non terminated actions.

  Eg.
  <verbatim>
  $oozie job -slaenable <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z]
  $oozie job -sladisable <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z]
  $oozie job -slachange <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z] -value 'sla-max-duration=${10 * MINUTES};sla-should-end=${30 * MINUTES};sla-max-duration=${30 * MINUTES}'
  $oozie job -slaenable <bundle_job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z] [-coordinator <List_of_coord_names/ids]
  </verbatim>

 ---++ Jobs Operations
 
 ---+++ Checking the Status of multiple Workflow Jobs
diff --git a/docs/src/site/twiki/DG_SLAMonitoring.twiki b/docs/src/site/twiki/DG_SLAMonitoring.twiki
index acf8ac16f..1413945cf 100644
-- a/docs/src/site/twiki/DG_SLAMonitoring.twiki
++ b/docs/src/site/twiki/DG_SLAMonitoring.twiki
@@ -294,6 +294,52 @@ SLA Details:
   Actual Duration (in mins) - -1
 </verbatim>
 
---+++ Changing job SLA definition and alerting
Following properties can be specified in job.xml to enable/disable SLA alerts.

=oozie.sla.disable.alerts.older.than= this property can be specified in hours, the SLA notification for coord actions will be disabled whose nominal is time older then this value. Default is 24 hours.
<verbatim>
<property>
    <name>oozie.sla.disable.alerts.older.than</name>
    <value>12</value>
</property>
</verbatim>

=oozie.sla.disable.alerts= List of coord actions to be disabled. Value can be specified as list of coord actions or date range.
<verbatim>
<property>
    <name>oozie.sla.disable.alerts</name>
    <value>1,3-4,7-10</value>
</property>
</verbatim>
Will disable alert for coord actions 1,3,5,7,8,9,10

=oozie.sla.enable.alerts= List of coord actions to be disabled. Value can be specified as list of coord actions or date range.
<verbatim>
<property>
    <name>oozie.sla.disable.alerts</name>
    <value>2009-01-01T01:00Z::2009-05-31T23:59Z</value>
</property>
</verbatim>
This will enable SLA alert for coord actions whose nominal time is in between (inclusive) 2009-01-01T01:00Z and 2009-05-31T23:59Z.

ALL keyword can be specified to specify all actions. Below property will disable SLA notifications for all coord actions.
<verbatim>
<property>
    <name>oozie.sla.disable.alerts</name>
    <value>ALL</value>
</property>
</verbatim>

SLA alert enabling or disabling can also be modified through commandline or REST API after submission for running jobs.

Refer [[DG_CommandLineTool#Changing_job_SLA_definition_and_alerting][Changing job SLA definition and alerting]] for commandline usage.
Refer the REST API [[WebServicesAPI#Changing_job_SLA_definition_and_alerting][Changing job SLA definition and alerting]].

SLA definition of should-start, should-end, nominal-time and max-duration can be changed for running jobs through commandline or REST API.
Refer [[DG_CommandLineTool#Changing_job_SLA_definition_and_alerting][Changing job SLA definition and alerting]] for commandline usage.
Refer the REST API [[WebServicesAPI#Changing_job_SLA_definition_and_alerting][Changing job SLA definition and alerting]].

 ---++ Known issues
 There are two known issues when you define SLA for a workflow action.
    * If there are decision nodes and SLA is defined for a workflow action not in the execution path because of the decision node, you will still get an SLA_MISS notification.
diff --git a/docs/src/site/twiki/WebServicesAPI.twiki b/docs/src/site/twiki/WebServicesAPI.twiki
index c301b78bd..3dc359a1b 100644
-- a/docs/src/site/twiki/WebServicesAPI.twiki
++ b/docs/src/site/twiki/WebServicesAPI.twiki
@@ -1520,6 +1520,48 @@ Content-Type: application/json;charset=UTF-8
 
 It accepts any valid Workflow Job ID, Coordinator Job ID, Coordinator Action ID, or Bundle Job ID.
 
---++++ Changing job SLA definition and alerting
An =HTTP PUT= request to change job SLA alert status/SLA definition.

   * All sla commands takes actions-list or date parameter.
   * =date=: a comma-separated list of date ranges. Each date range element is specified with dates separated by =::=
   * =action-list=: a comma-separated list of action ranges. Each action range is specified with two action numbers separated by =-=
   * For bundle jobs additional =coordinators= (coord_name/id) parameter can be passed.
   * Sla change command need extra parameter =value= to specify new sla definition.


   * Changing SLA definition
   SLA definition of should-start, should-end, nominal-time and max-duration can be changed.

<verbatim>
PUT /oozie/v2/job/0000003-140319184715726-oozie-puru-C?action=sla-change&value=<key>=<value>;...;<key>=<value>
</verbatim>

   * Disabling SLA alert

<verbatim>
PUT /oozie/v2/job/0000003-140319184715726-oozie-puru-C?action=sla-disable&action-list=3-4
</verbatim>
Will disable SLA alert for actions 3 and 4.

<verbatim>
PUT /oozie/v1/job/0000003-140319184715726-oozie-puru-C?action=sla-disable&date=2009-02-01T00:10Z::2009-03-01T00:10Z
</verbatim>
Will disable SLA alert for actions whose nominal time is in-between 2009-02-01T00:10Z 2009-03-01T00:10Z (inclusive).


<verbatim>
PUT /oozie/v1/job/0000004-140319184715726-oozie-puru-B?action=sla-disable&date=2009-02-01T00:10Z::2009-03-01T00:10Z&coordinators=abc
</verbatim>
For bundle jobs additional coordinators (list of comma separated coord_name/id) parameter can be passed.

   * Enabling SLA alert

<verbatim>
PUT /oozie/v2/job/0000003-140319184715726-oozie-puru-C?action=sla-enable&action-list=1,14,17-20
</verbatim>
Will enable SLA alert for actions 1,14,17,18,19,20.

 ---++++ Jobs Information
 
 A HTTP GET request retrieves workflow and coordinator jobs information.
diff --git a/release-log.txt b/release-log.txt
index be11f2cab..24d594ddd 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.2.0 release (trunk - unreleased)
 
OOZIE-1913 Devise a way to turn off SLA alerts for bundle/coordinator flexibly (puru)
 OOZIE-2071 Add a Spark example (pavan kumar via rkanter)
 OOZIE-2145 ZooKeeper paths should start with a "/" (rkanter)
 OOZIE-2113 Oozie Command Line Utilities are failing as hadoop-auth jar not found (shwethags)
diff --git a/webapp/src/main/webapp/console/sla/js/oozie-sla-table.js b/webapp/src/main/webapp/console/sla/js/oozie-sla-table.js
index 1a88671ad..7ae604cff 100644
-- a/webapp/src/main/webapp/console/sla/js/oozie-sla-table.js
++ b/webapp/src/main/webapp/console/sla/js/oozie-sla-table.js
@@ -34,6 +34,7 @@ var columnsToShow = [
               { "mData": "jobStatus", "sDefaultContent": ""},
               { "mData": "parentId", "sDefaultContent": "", "bVisible": false},
               { "mData": "appName", "bVisible": false},
              { "mData": "slaAlertStatus", "bVisible": false},
              ];
 
 $.fn.dataTableExt.oApi.fnGetTds  = function ( oSettings, mTr )
diff --git a/webapp/src/main/webapp/console/sla/oozie-sla.html b/webapp/src/main/webapp/console/sla/oozie-sla.html
index 23e8af248..e5bf6275a 100644
-- a/webapp/src/main/webapp/console/sla/oozie-sla.html
++ b/webapp/src/main/webapp/console/sla/oozie-sla.html
@@ -101,6 +101,7 @@
                             <th>Job Status</th>
                             <th>Parent Id</th>
                             <th>AppName</th>
                            <th>Sla Alert</th>
                         </tr>
                     </thead>
                 </table>
- 
2.19.1.windows.1

