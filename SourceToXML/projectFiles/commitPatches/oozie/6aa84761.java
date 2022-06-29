From 6aa84761759b8433115b040f5e133791b23d0ca6 Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Thu, 3 Mar 2016 09:53:18 -0800
Subject: [PATCH] OOZIE-2446 Job does not fail during submission if non
 existent credential is specified

--
 .../action/hadoop/JavaActionExecutor.java     |  4 ++
 .../action/hadoop/TestJavaActionExecutor.java | 39 +++++++++++++++++++
 release-log.txt                               |  1 +
 3 files changed, 44 insertions(+)

diff --git a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
index 4fc0c5239..e055a3ddc 100644
-- a/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
++ b/core/src/main/java/org/apache/oozie/action/hadoop/JavaActionExecutor.java
@@ -1324,6 +1324,10 @@ public class JavaActionExecutor extends ActionExecutor {
                     }
                 }
             }
            if (credProp == null && credName != null) {
                throw new ActionExecutorException(ActionExecutorException.ErrorType.ERROR, "JA021",
                        "Could not load credentials with name [{0}]].", credName);
            }
         } else {
             LOG.debug("credentials is null for the action");
         }
diff --git a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
index 86f0ed2ea..35390c444 100644
-- a/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
++ b/core/src/test/java/org/apache/oozie/action/hadoop/TestJavaActionExecutor.java
@@ -972,6 +972,45 @@ public class TestJavaActionExecutor extends ActionExecutorTestCase {
         assertNotNull(tk);
     }
 
    public void testCredentialsInvalid() throws Exception {
        String workflowXml = "<workflow-app xmlns='uri:oozie:workflow:0.2.5' name='pig-wf'>" + "<credentials>"
                + "<credential name='abcname' type='abc'>" + "<property>" + "<name>property1</name>"
                + "<value>value1</value>" + "</property>" + "<property>" + "<name>property2</name>"
                + "<value>value2</value>" + "</property>" + "<property>" + "<name>${property3}</name>"
                + "<value>${value3}</value>" + "</property>" + "</credential>" + "</credentials>"
                + "<start to='pig1' />" + "<action name='pig1' cred='abcname'>" + "<pig>" + "</pig>"
                + "<ok to='end' />" + "<error to='fail' />" + "</action>" + "<kill name='fail'>"
                + "<message>Pig failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>" + "</kill>"
                + "<end name='end' />" + "</workflow-app>";

        JavaActionExecutor ae = new JavaActionExecutor();
        WorkflowJobBean wfBean = addRecordToWfJobTable("test1", workflowXml);
        WorkflowActionBean action = (WorkflowActionBean) wfBean.getActions().get(0);
        action.setType(ae.getType());
        action.setCred("invalidabcname");
        String actionXml = "<pig>" + "<job-tracker>${jobTracker}</job-tracker>" + "<name-node>${nameNode}</name-node>"
                + "<prepare>" + "<delete path='outputdir' />" + "</prepare>" + "<configuration>" + "<property>"
                + "<name>mapred.compress.map.output</name>" + "<value>true</value>" + "</property>" + "<property>"
                + "<name>mapred.job.queue.name</name>" + "<value>${queueName}</value>" + "</property>"
                + "</configuration>" + "<script>org/apache/oozie/examples/pig/id.pig</script>"
                + "<param>INPUT=${inputDir}</param>" + "<param>OUTPUT=${outputDir}/pig-output</param>" + "</pig>";
        action.setConf(actionXml);
        Context context = new Context(wfBean, action);

        Element actionXmlconf = XmlUtils.parseXml(action.getConf());
        // action job configuration
        Configuration actionConf = ae.createBaseHadoopConf(context, actionXmlconf);

        try {
        // Setting the credential properties in launcher conf should fail
        HashMap<String, CredentialsProperties> credProperties = ae.setCredentialPropertyToActionConf(context, action,
                actionConf);
        }
        catch (ActionExecutorException e) {
            assertEquals(e.getErrorCode(), "JA021");
        }
    }

     public void testCredentialsSkip() throws Exception {
         // Try setting oozie.credentials.skip at different levels, and verifying the correct behavior
         // oozie-site: false -- job-level: null -- action-level: null
diff --git a/release-log.txt b/release-log.txt
index da4c97cb3..501d80767 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2446 Job does not fail during submission if non existent credential is specified (satishsaley via puru)
 OOZIE-2283 Documentation should not say that System.exit is not allowed in Java Action (eeeva via rkanter)
 OOZIE-2400 Workflow xml configuration parser cannot deal with namespace prefix (lars_francke via rkanter)
 OOZIE-2452 Coordinator Functional Specification - EL Constants Typo (markgreene via puru)
- 
2.19.1.windows.1

