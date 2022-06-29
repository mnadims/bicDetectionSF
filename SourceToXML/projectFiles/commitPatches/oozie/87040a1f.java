From 87040a1f1f9ab1ccaecff640b56c33b9900eca2a Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Fri, 27 May 2016 16:07:48 -0700
Subject: [PATCH] amend OOZIE-1976 Specifying coordinator input datasets in
 more logical ways

--
 .../CoordPushDependencyCheckXCommand.java     |  5 +-
 .../apache/oozie/coord/CoordELEvaluator.java  |  3 +-
 .../apache/oozie/coord/CoordELFunctions.java  |  3 +-
 .../CoordInputDependencyFactory.java          | 16 +---
 .../dependency/CoordOldInputDependency.java   | 76 ++++++++++---------
 .../CoordInputLogicEvaluatorPhaseOne.java     |  5 +-
 .../logic/CoordInputLogicEvaluatorUtil.java   | 19 +++--
 .../input/logic/OozieJexlInterpreter.java     | 12 ++-
 .../input/logic/TestCoordInputLogicPush.java  |  5 +-
 .../logic/TestCoordinatorInputLogic.java      | 11 ++-
 core/src/test/resources/coord-inputlogic.xml  |  1 +
 11 files changed, 89 insertions(+), 67 deletions(-)

diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
index 2600a2bde..6d8aa0ffd 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
@@ -36,6 +36,7 @@ import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 import org.apache.oozie.dependency.ActionDependency;
import org.apache.oozie.dependency.DependencyChecker;
 import org.apache.oozie.dependency.URIHandler;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
@@ -147,8 +148,8 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
                     isChangeInDependency = false;
                 }
                 else {
                    coordPushInputDependency.setMissingDependencies(StringUtils.join(
                            actionDependency.getMissingDependencies(), CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR));
                    String stillMissingDeps = DependencyChecker.dependenciesAsString(actionDependency.getMissingDependencies());
                    coordPushInputDependency.setMissingDependencies(stillMissingDeps);
                 }
 
                 if (coordPushInputDependency.isDependencyMet()) {
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java b/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
index fba8ac1b5..809c026bc 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
@@ -219,7 +219,8 @@ public class CoordELEvaluator {
                     // check
                     // null
                 }
                Element doneFlagElement = data.getChild("done-flag", data.getNamespace());
                Element doneFlagElement = data.getChild("dataset", data.getNamespace()).getChild("done-flag",
                        data.getNamespace());
                 String doneFlag = CoordUtils.getDoneFlag(doneFlagElement);
                 e.setVariable(".datain." + data.getAttributeValue("name") + ".doneFlag", doneFlag);
             }
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index ffa0943d2..5bb8be668 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -503,7 +503,8 @@ public class CoordELFunctions {
         String uris = "";
         ELEvaluator eval = ELEvaluator.getCurrent();
         if (eval.getVariable(".datain." + dataInName) == null
                && !StringUtils.isEmpty(eval.getVariable(".actionInputLogic").toString())) {
                && (eval.getVariable(".actionInputLogic") != null && !StringUtils.isEmpty(eval.getVariable(
                        ".actionInputLogic").toString()))) {
             try {
                 return new CoordInputLogicEvaluatorUtil().getInputDependencies(dataInName,
                         (SyncCoordAction) eval.getVariable(COORD_ACTION));
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
index ad50890c0..ea1546721 100644
-- a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
@@ -72,18 +72,14 @@ public class CoordInputDependencyFactory {
      */
     public static CoordInputDependency getPullInputDependencies(StringBlob missingDependencies) {
         if (missingDependencies == null) {
            return new CoordPullInputDependency();
            return new CoordOldInputDependency();
         }
         return getPullInputDependencies(missingDependencies.getString());
     }
 
     public static CoordInputDependency getPullInputDependencies(String dependencies) {
 
        if (StringUtils.isEmpty(dependencies)) {
            return new CoordPullInputDependency();
        }

        if (!hasInputLogic(dependencies)) {
        if (StringUtils.isEmpty(dependencies) || !hasInputLogic(dependencies)) {
             return new CoordOldInputDependency(dependencies);
         }
         else
@@ -105,7 +101,7 @@ public class CoordInputDependencyFactory {
     public static CoordInputDependency getPushInputDependencies(StringBlob pushMissingDependencies) {
 
         if (pushMissingDependencies == null) {
            return new CoordPushInputDependency();
            return new CoordOldInputDependency();
         }
         return getPushInputDependencies(pushMissingDependencies.getString());
 
@@ -113,11 +109,7 @@ public class CoordInputDependencyFactory {
 
     public static CoordInputDependency getPushInputDependencies(String dependencies) {
 

        if (StringUtils.isEmpty(dependencies)) {
            return new CoordPushInputDependency();
        }
        if (!hasInputLogic(dependencies)) {
        if (StringUtils.isEmpty(dependencies) || !hasInputLogic(dependencies)) {
             return new CoordOldInputDependency(dependencies);
         }
 
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
index 9fc348ff5..aabd2bf25 100644
-- a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
@@ -258,49 +258,55 @@ public class CoordOldInputDependency implements CoordInputDependency {
         String actualTimeStr = eAction.getAttributeValue("action-actual-time");
         Element inputList = eAction.getChild("input-events", eAction.getNamespace());
 
        if(inputList==null){
            return true;
        }

         List<Element> eDataEvents = inputList.getChildren("data-in", eAction.getNamespace());
         Configuration actionConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
 
        Date actualTime = null;
        if (actualTimeStr == null) {
            actualTime = new Date();
        }
        else {
            actualTime = DateUtils.parseDateOozieTZ(actualTimeStr);
        }

        for (Element dEvent : eDataEvents) {
            if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace()) == null) {
                continue;
        if (eDataEvents != null) {
            Date actualTime = null;
            if (actualTimeStr == null) {
                actualTime = new Date();
             }
            ELEvaluator eval = CoordELEvaluator.createLazyEvaluator(actualTime, nominalTime, dEvent, actionConf);
            String unResolvedInstance = dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG,
                    dEvent.getNamespace()).getTextTrim();
            String unresolvedList[] = unResolvedInstance.split(CoordELFunctions.INSTANCE_SEPARATOR);
            StringBuffer resolvedTmp = new StringBuffer();
            for (int i = 0; i < unresolvedList.length; i++) {
                String returnData = CoordELFunctions.evalAndWrap(eval, unresolvedList[i]);
                Boolean isResolved = (Boolean) eval.getVariable(CoordELConstants.IS_RESOLVED);
                if (isResolved == false) {
                    log.info("[" + coordAction.getId() + "] :: Cannot resolve : " + returnData);
                    return false;
            else {
                actualTime = DateUtils.parseDateOozieTZ(actualTimeStr);
            }

            for (Element dEvent : eDataEvents) {
                if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace()) == null) {
                    continue;
                 }
                if (resolvedTmp.length() > 0) {
                    resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR);
                ELEvaluator eval = CoordELEvaluator.createLazyEvaluator(actualTime, nominalTime, dEvent, actionConf);
                String unResolvedInstance = dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG,
                        dEvent.getNamespace()).getTextTrim();
                String unresolvedList[] = unResolvedInstance.split(CoordELFunctions.INSTANCE_SEPARATOR);
                StringBuffer resolvedTmp = new StringBuffer();
                for (int i = 0; i < unresolvedList.length; i++) {
                    String returnData = CoordELFunctions.evalAndWrap(eval, unresolvedList[i]);
                    Boolean isResolved = (Boolean) eval.getVariable(CoordELConstants.IS_RESOLVED);
                    if (isResolved == false) {
                        log.info("[" + coordAction.getId() + "] :: Cannot resolve : " + returnData);
                        return false;
                    }
                    if (resolvedTmp.length() > 0) {
                        resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR);
                    }
                    resolvedTmp.append((String) eval.getVariable(CoordELConstants.RESOLVED_PATH));
                 }
                resolvedTmp.append((String) eval.getVariable(CoordELConstants.RESOLVED_PATH));
            }
            if (resolvedTmp.length() > 0) {
                if (dEvent.getChild("uris", dEvent.getNamespace()) != null) {
                    resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR).append(
                            dEvent.getChild("uris", dEvent.getNamespace()).getTextTrim());
                    dEvent.removeChild("uris", dEvent.getNamespace());
                if (resolvedTmp.length() > 0) {
                    if (dEvent.getChild("uris", dEvent.getNamespace()) != null) {
                        resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR).append(
                                dEvent.getChild("uris", dEvent.getNamespace()).getTextTrim());
                        dEvent.removeChild("uris", dEvent.getNamespace());
                    }
                    Element uriInstance = new Element("uris", dEvent.getNamespace());
                    uriInstance.addContent(resolvedTmp.toString());
                    dEvent.getContent().add(1, uriInstance);
                 }
                Element uriInstance = new Element("uris", dEvent.getNamespace());
                uriInstance.addContent(resolvedTmp.toString());
                dEvent.getContent().add(1, uriInstance);
                dEvent.removeChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace());
             }
            dEvent.removeChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace());
         }
 
         return true;
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
index f54d30543..6525125d1 100644
-- a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
@@ -27,6 +27,7 @@ import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;

 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
@@ -34,6 +35,7 @@ import org.apache.oozie.command.coord.CoordCommandUtils;
 import org.apache.oozie.coord.input.dependency.AbstractCoordInputDependency;
 import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 import org.apache.oozie.coord.input.dependency.CoordInputInstance;
import org.apache.oozie.coord.input.dependency.CoordPullInputDependency;
 import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorResult.STATUS;
 import org.apache.oozie.dependency.URIHandlerException;
 import org.apache.oozie.util.LogUtils;
@@ -83,8 +85,7 @@ public class CoordInputLogicEvaluatorPhaseOne implements CoordInputLogicEvaluato
         List<String> availableList = new ArrayList<String>();
         if (coordInputDependency.getDependencyMap().get(dataSet) == null) {
             CoordInputLogicEvaluatorResult retData = new CoordInputLogicEvaluatorResult();
            if (coordInputDependency.getAvailableDependencies(dataSet) == null
                    || coordInputDependency.getAvailableDependencies(dataSet).isEmpty()) {
            if (((CoordPullInputDependency) coordAction.getPullInputDependencies()).getUnResolvedDependency(dataSet) != null) {
                 log.debug("Data set [{0}] is unresolved set, will get resolved in phasetwo", dataSet);
                 retData.setStatus(CoordInputLogicEvaluatorResult.STATUS.PHASE_TWO_EVALUATION);
             }
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
index 63c07609a..653fb2a32 100644
-- a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
@@ -71,7 +71,7 @@ public class CoordInputLogicEvaluatorUtil {
         JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseOne(
                 coordAction, coordAction.getPullInputDependencies())));
         CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.getStatus());
 
         if (result.isWaiting()) {
             return false;
@@ -134,13 +134,16 @@ public class CoordInputLogicEvaluatorUtil {
         JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseThree(
                 coordAction, eval)));
         CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] is [{1}] and evaluate result is [{2}]", name, expression,
                result.isTrue());
 
        if (!result.isTrue()) {
            return name + " is not resolved";
        if (result == null || !result.isTrue()) {
            log.debug("Input logic expression for [{0}] is [{1}] and it is not resolved", name, expression);
            return "${coord:dataIn('" + name + "')}";
        }
        else {
            log.debug("Input logic expression for [{0}] is [{1}] and evaluate result is [{2}]", name, expression,
                    result.getStatus());
            return result.getDataSets();
         }
        return result.getDataSets();
 
     }
 
@@ -162,7 +165,7 @@ public class CoordInputLogicEvaluatorUtil {
         JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseOne(
                 coordAction, coordAction.getPushInputDependencies())));
         CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.getStatus());
 
         if (result.isWaiting()) {
             return false;
@@ -189,7 +192,7 @@ public class CoordInputLogicEvaluatorUtil {
         JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseTwo(
                 coordAction, actualTime)));
         CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.getStatus());
 
         if (result.isWaiting()) {
             return false;
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
index 2044723db..757778895 100644
-- a/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
@@ -24,6 +24,7 @@ import org.apache.commons.jexl2.JexlEngine;
 import org.apache.commons.jexl2.parser.ASTAndNode;
 import org.apache.commons.jexl2.parser.ASTOrNode;
 import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorResult.STATUS;
 
 /**
  * Oozie implementation of jexl Interpreter
@@ -58,15 +59,22 @@ public class OozieJexlInterpreter extends Interpreter {
         CoordInputLogicEvaluatorResult left = (CoordInputLogicEvaluatorResult) node.jjtGetChild(0)
                 .jjtAccept(this, data);
 
        if (!left.isTrue()) {
        if(left.isWaiting() || !left.isTrue()){
             return left;
         }

         CoordInputLogicEvaluatorResult right = (CoordInputLogicEvaluatorResult) node.jjtGetChild(1).jjtAccept(this,
                 data);
        if(right.isWaiting()){
            return right;
        }
        if(left.isPhaseTwoEvaluation() || right.isPhaseTwoEvaluation()){
            return new CoordInputLogicEvaluatorResult(STATUS.PHASE_TWO_EVALUATION);
        }

         if (right.isTrue()) {
             right.appendDataSets(left.getDataSets());
         }

         return right;
     }
 
diff --git a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
index c58b18b73..6684a1fc7 100644
-- a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
++ b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
@@ -356,6 +356,10 @@ public class TestCoordInputLogicPush extends XHCatTestCase {
         String input1 = createTestCaseSubDir("input-data/b/2014/10/08/_SUCCESS".split("/"));
         String input2 = addPartition("db_a", "table1", "dt=20141008;country=usa");
 
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();
        new CoordPushDependencyCheckXCommand(jobId + "@1").call();
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

         startCoordAction(jobId);
 
         CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
@@ -546,7 +550,6 @@ public class TestCoordInputLogicPush extends XHCatTestCase {
 
     private void startCoordAction(final String jobId) throws CommandException, JPAExecutorException {
         new CoordMaterializeTransitionXCommand(jobId, 3600).call();

         new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
         new CoordPushDependencyCheckXCommand(jobId + "@1").call();
         new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
diff --git a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
index 0679c8cbf..aa0d5d62f 100644
-- a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
++ b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
@@ -202,9 +202,10 @@ public class TestCoordinatorInputLogic extends XDataTestCase {
          "</and>";
         //@formatter:on
         conf.set("partitionName", "test");
        conf.set("A_done_flag", "done");
         final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);
 
        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/done".split("/"));
         String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
         String input3 = createTestCaseSubDir("input-data/e/2014/10/08/00/_SUCCESS".split("/"));
         String input4 = createTestCaseSubDir("input-data/f/2014/10/08/00/_SUCCESS".split("/"));
@@ -216,7 +217,7 @@ public class TestCoordinatorInputLogic extends XDataTestCase {
         XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
         String dataSets = runConf.get("inputLogicData");
         assertEquals(dataSets.split(",").length, 4);
        checkDataSets(dataSets, input1, input2, input3, input4);
        checkDataSets(dataSets, input1.replace("/done", ""), input2, input3, input4);
 
     }
 
@@ -239,9 +240,10 @@ public class TestCoordinatorInputLogic extends XDataTestCase {
          "</or>";
         //@formatter:on
         conf.set("partitionName", "test");
        conf.set("A_done_flag", "done");
         final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);
 
        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/done".split("/"));
         String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
         String input3 = createTestCaseSubDir("input-data/c/2014/10/08/00/_SUCCESS".split("/"));
         String input4 = createTestCaseSubDir("input-data/e/2014/10/08/00/_SUCCESS".split("/"));
@@ -872,6 +874,9 @@ public class TestCoordinatorInputLogic extends XDataTestCase {
         conf.set("queueName", "default");
         conf.set("jobTracker", "localhost:9001");
         conf.set("examplesRoot", "examples");
        if (conf.get("A_done_flag") == null) {
            conf.set("A_done_flag", "_SUCCESS");
        }
 
         return new CoordSubmitXCommand(dryRun, conf).call();
     }
diff --git a/core/src/test/resources/coord-inputlogic.xml b/core/src/test/resources/coord-inputlogic.xml
index 51b67ac55..b87445da6 100644
-- a/core/src/test/resources/coord-inputlogic.xml
++ b/core/src/test/resources/coord-inputlogic.xml
@@ -29,6 +29,7 @@
             initial-instance="${initial_instance_a}" timezone="UTC">
             <uri-template>${data_set_a}/${YEAR}/${MONTH}/${DAY}/${HOUR}
             </uri-template>
            <done-flag>${A_done_flag}</done-flag>
         </dataset>
         <dataset name="b" frequency="${coord:hours(1)}"
             initial-instance="${initial_instance_b}" timezone="UTC">
- 
2.19.1.windows.1

