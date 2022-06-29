From aebf775a35e618c491b20bc5690e93f82ac401bf Mon Sep 17 00:00:00 2001
From: Robert Kanter <rkanter@cloudera.com>
Date: Tue, 19 Jan 2016 17:08:22 -0800
Subject: [PATCH] OOZIE-2441 SubWorkflow action with propagate-configuration
 but no global section throws NPE on submit (rkanter)

--
 .../workflow/lite/LiteWorkflowAppParser.java  |  4 ++--
 .../lite/TestLiteWorkflowAppParser.java       | 21 +++++++++++++++++++
 release-log.txt                               |  1 +
 3 files changed, 24 insertions(+), 2 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
index 7f1c73c65..a1b9cdba3 100644
-- a/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
++ b/core/src/main/java/org/apache/oozie/workflow/lite/LiteWorkflowAppParser.java
@@ -472,8 +472,8 @@ public class LiteWorkflowAppParser {
                     } else if (SLA_INFO.equals(elem.getName()) || CREDENTIALS.equals(elem.getName())) {
                         continue;
                     } else {
                        if (!serializedGlobalConf  && elem.getName().equals(SubWorkflowActionExecutor.ACTION_TYPE) &&
                                elem.getChild(("propagate-configuration"), ns) != null) {
                        if (!serializedGlobalConf && elem.getName().equals(SubWorkflowActionExecutor.ACTION_TYPE) &&
                                elem.getChild(("propagate-configuration"), ns) != null && gData != null) {
                             serializedGlobalConf = true;
                             jobConf.set(OOZIE_GLOBAL, getGlobalString(gData));
                         }
diff --git a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
index 1983a8ce7..9002b6c5b 100644
-- a/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
++ b/core/src/test/java/org/apache/oozie/workflow/lite/TestLiteWorkflowAppParser.java
@@ -510,6 +510,27 @@ public class TestLiteWorkflowAppParser extends XTestCase {
         }
     }
 
    public void testParserSubWorkflowPropagateNoGlobal() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(
                IOUtils.getResourceAsReader("wf-schema-subworkflow-propagate-no-global.xml", -1),
                new Configuration());

        String a = app.getNode("a").getConf();
        String expectedA =
                "<sub-workflowxmlns=\"uri:oozie:workflow:0.4\">\r\n" +
                        "<app-path>/tmp/foo/</app-path>\r\n" +
                        "<propagate-configuration/>\r\n" +
                        "<configuration/>\r\n" +
                        "</sub-workflow>";
        a = cleanupXml(a);
        assertEquals(expectedA.replaceAll(" ", ""), a.replaceAll(" ", ""));
    }

     public void testParserFsGlobalNN() throws Exception {
         LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                 LiteWorkflowStoreService.LiteControlNodeHandler.class,
diff --git a/release-log.txt b/release-log.txt
index 5350405aa..391834b9b 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-2441 SubWorkflow action with propagate-configuration but no global section throws NPE on submit (rkanter)
 OOZIE-2370 Docs for Coordinator Action Status Notification has wrong property name (eeeva via rkanter)
 OOZIE-2419 HBase credentials are not correctly proxied (qwertymaniac via rkanter)
 OOZIE-2439 FS Action no longer uses name-node from global section or default NN (rkanter)
- 
2.19.1.windows.1

