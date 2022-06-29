From 81ce22b6f23b2bba49df4733961ee82b58c38d0d Mon Sep 17 00:00:00 2001
From: Purshotam Shah <purushah@yahoo-inc.com>
Date: Tue, 26 Jan 2016 12:46:16 -0800
Subject: [PATCH] OOZIE-1976 Specifying coordinator input datasets in more
 logical ways

--
 .../main/resources/oozie-coordinator-0.5.xsd  |  194 +++
 core/pom.xml                                  |    5 +
 .../apache/oozie/CoordinatorActionBean.java   |   46 +-
 .../main/java/org/apache/oozie/ErrorCode.java |    2 +
 .../coord/CoordActionInputCheckXCommand.java  |  318 ++---
 ...oordActionUpdatePushMissingDependency.java |   30 +-
 .../command/coord/CoordCommandUtils.java      |  180 ++-
 .../CoordMaterializeTransitionXCommand.java   |    6 +-
 .../CoordPushDependencyCheckXCommand.java     |   73 +-
 .../command/coord/CoordSubmitXCommand.java    |   27 +
 .../apache/oozie/coord/CoordELConstants.java  |    3 +
 .../apache/oozie/coord/CoordELEvaluator.java  |   23 +-
 .../apache/oozie/coord/CoordELFunctions.java  |   49 +-
 .../org/apache/oozie/coord/CoordUtils.java    |   22 +
 .../apache/oozie/coord/SyncCoordAction.java   |   22 +
 .../AbstractCoordInputDependency.java         |  315 +++++
 .../dependency/CoordInputDependency.java      |  172 +++
 .../CoordInputDependencyFactory.java          |  170 +++
 .../input/dependency/CoordInputInstance.java  |   83 ++
 .../dependency/CoordOldInputDependency.java   |  309 +++++
 .../dependency/CoordPullInputDependency.java  |  151 +++
 .../dependency/CoordPushInputDependency.java  |   49 +
 .../CoordUnResolvedInputDependency.java       |   92 ++
 .../input/logic/CoordInputLogicBuilder.java   |  167 +++
 .../input/logic/CoordInputLogicEvaluator.java |   44 +
 .../CoordInputLogicEvaluatorPhaseOne.java     |  324 +++++
 .../CoordInputLogicEvaluatorPhaseThree.java   |  130 ++
 .../CoordInputLogicEvaluatorPhaseTwo.java     |  144 +++
 ...CoordInputLogicEvaluatorPhaseValidate.java |   89 ++
 .../logic/CoordInputLogicEvaluatorResult.java |  104 ++
 .../logic/CoordInputLogicEvaluatorUtil.java   |  229 ++++
 .../coord/input/logic/InputLogicParser.java   |  309 +++++
 .../coord/input/logic/OozieJexlEngine.java    |   47 +
 .../input/logic/OozieJexlInterpreter.java     |   73 ++
 .../oozie/dependency/ActionDependency.java    |    2 +-
 .../oozie/dependency/DependencyChecker.java   |   15 +-
 .../apache/oozie/dependency/FSURIHandler.java |    9 +
 .../oozie/dependency/HCatURIHandler.java      |    5 +
 .../apache/oozie/dependency/URIHandler.java   |   14 +
 .../org/apache/oozie/util/WritableUtils.java  |  148 ++-
 core/src/main/resources/oozie-default.xml     |    2 +-
 .../TestCoordActionInputCheckXCommand.java    |    9 +-
 .../input/logic/TestCoordInputLogicPush.java  |  645 ++++++++++
 .../logic/TestCoordinatorInputLogic.java      | 1054 +++++++++++++++++
 .../input/logic/TestInputLogicParser.java     |  367 ++++++
 core/src/test/resources/coord-action-sla.xml  |    2 +-
 .../resources/coord-inputlogic-combine.xml    |  119 ++
 .../test/resources/coord-inputlogic-hcat.xml  |  119 ++
 .../resources/coord-inputlogic-latest.xml     |  124 ++
 .../coord-inputlogic-range-latest.xml         |  130 ++
 .../test/resources/coord-inputlogic-range.xml |  107 ++
 core/src/test/resources/coord-inputlogic.xml  |  126 ++
 pom.xml                                       |    7 +
 release-log.txt                               |    1 +
 54 files changed, 6625 insertions(+), 381 deletions(-)
 create mode 100644 client/src/main/resources/oozie-coordinator-0.5.xsd
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/AbstractCoordInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputInstance.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPullInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPushInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/dependency/CoordUnResolvedInputDependency.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicBuilder.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluator.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseThree.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseTwo.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseValidate.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorResult.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/InputLogicParser.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlEngine.java
 create mode 100644 core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
 create mode 100644 core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
 create mode 100644 core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
 create mode 100644 core/src/test/java/org/apache/oozie/coord/input/logic/TestInputLogicParser.java
 create mode 100644 core/src/test/resources/coord-inputlogic-combine.xml
 create mode 100644 core/src/test/resources/coord-inputlogic-hcat.xml
 create mode 100644 core/src/test/resources/coord-inputlogic-latest.xml
 create mode 100644 core/src/test/resources/coord-inputlogic-range-latest.xml
 create mode 100644 core/src/test/resources/coord-inputlogic-range.xml
 create mode 100644 core/src/test/resources/coord-inputlogic.xml

diff --git a/client/src/main/resources/oozie-coordinator-0.5.xsd b/client/src/main/resources/oozie-coordinator-0.5.xsd
new file mode 100644
index 000000000..2b636290f
-- /dev/null
++ b/client/src/main/resources/oozie-coordinator-0.5.xsd
@@ -0,0 +1,194 @@
<?xml version="1.0" encoding="UTF-8"?>
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
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:coordinator="uri:oozie:coordinator:0.5"
           elementFormDefault="qualified" targetNamespace="uri:oozie:coordinator:0.5">

    <xs:element name="coordinator-app" type="coordinator:COORDINATOR-APP"/>
    <xs:element name="datasets" type="coordinator:DATASETS"/>
    <xs:simpleType name="IDENTIFIER">
        <xs:restriction base="xs:string">
            <xs:pattern value="([a-zA-Z]([\-_a-zA-Z0-9])*){1,39}"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="COORDINATOR-APP">
        <xs:sequence>
            <xs:element name="parameters" type="coordinator:PARAMETERS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="controls" type="coordinator:CONTROLS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="datasets" type="coordinator:DATASETS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-events" type="coordinator:INPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-logic" type="coordinator:INPUTLOGIC" minOccurs="0" maxOccurs="1"/>
            <xs:element name="output-events" type="coordinator:OUTPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="action" type="coordinator:ACTION" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="xs:string" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="start" type="xs:string" use="required"/>
        <xs:attribute name="end" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="PARAMETERS">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="0" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="CONTROLS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="timeout" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="concurrency" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="execution" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="throttle" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATASETS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="include" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:choice minOccurs="0" maxOccurs="unbounded">
                <xs:element name="dataset" type="coordinator:SYNCDATASET" minOccurs="0" maxOccurs="1"/>
                <xs:element name="async-dataset" type="coordinator:ASYNCDATASET" minOccurs="0" maxOccurs="1"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="SYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="done-flag" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="initial-instance" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="ASYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="sequence-type" type="xs:string" use="required"/>
        <xs:attribute name="initial-version" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="INPUTEVENTS">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="1"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="1"/>
            <xs:element name="data-in" type="coordinator:DATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:choice>
    </xs:complexType>
    <xs:complexType name="INPUTLOGIC">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:choice>
    </xs:complexType>
    <xs:complexType name="LOGICALAND">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="LOGICALOR">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="COMBINE">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="2" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="LOGICALDATAIN">
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="DATAIN">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="unbounded"/>
            <xs:sequence minOccurs="1" maxOccurs="1">
                <xs:element name="start-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="end-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:sequence>
        </xs:choice>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="OUTPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-out" type="coordinator:DATAOUT" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAOUT">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="ACTION">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="workflow" type="coordinator:WORKFLOW" minOccurs="1" maxOccurs="1"/>
            <xs:any namespace="uri:oozie:sla:0.1 uri:oozie:sla:0.2" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="WORKFLOW">
        <xs:sequence>
            <xs:element name="app-path" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="configuration" type="coordinator:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="FLAG"/>
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
diff --git a/core/pom.xml b/core/pom.xml
index b063dab79..b72ea7d19 100644
-- a/core/pom.xml
++ b/core/pom.xml
@@ -282,6 +282,11 @@
             <version>3.4</version>
             <scope>provided</scope>
         </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-jexl</artifactId>
            <scope>compile</scope>
        </dependency>
 
         <dependency>
             <groupId>org.apache.oozie</groupId>
diff --git a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
index 91bff4dca..b1be7c939 100644
-- a/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
++ b/core/src/main/java/org/apache/oozie/CoordinatorActionBean.java
@@ -34,12 +34,15 @@ import javax.persistence.Lob;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.Table;
import javax.persistence.Transient;
 
 import org.apache.hadoop.io.Writable;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.rest.JsonBean;
 import org.apache.oozie.client.rest.JsonTags;
 import org.apache.oozie.client.rest.JsonUtils;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
import org.apache.oozie.coord.input.dependency.CoordInputDependencyFactory;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.WritableUtils;
 import org.apache.openjpa.persistence.jdbc.Index;
@@ -285,6 +288,13 @@ public class CoordinatorActionBean implements
         return toJSONObject("GMT");
     }
 
    @Transient
    private CoordInputDependency coordPushInputDependency;

    @Transient
    private CoordInputDependency coordPullInputDependency;


     public CoordinatorActionBean() {
     }
 
@@ -745,23 +755,21 @@ public class CoordinatorActionBean implements
         json.put(JsonTags.COORDINATOR_ACTION_TYPE, type);
         json.put(JsonTags.COORDINATOR_ACTION_NUMBER, actionNumber);
         json.put(JsonTags.COORDINATOR_ACTION_CREATED_CONF, getCreatedConf());
        json.put(JsonTags.COORDINATOR_ACTION_CREATED_TIME, JsonUtils
                .formatDateRfc822(getCreatedTime(), timeZoneId));
        json.put(JsonTags.COORDINATOR_ACTION_NOMINAL_TIME, JsonUtils
                .formatDateRfc822(getNominalTime(), timeZoneId));
        json.put(JsonTags.COORDINATOR_ACTION_CREATED_TIME, JsonUtils.formatDateRfc822(getCreatedTime(), timeZoneId));
        json.put(JsonTags.COORDINATOR_ACTION_NOMINAL_TIME, JsonUtils.formatDateRfc822(getNominalTime(), timeZoneId));
         json.put(JsonTags.COORDINATOR_ACTION_EXTERNALID, externalId);
         // json.put(JsonTags.COORDINATOR_ACTION_START_TIME, JsonUtils
         // .formatDateRfc822(startTime), timeZoneId);
         json.put(JsonTags.COORDINATOR_ACTION_STATUS, statusStr);
         json.put(JsonTags.COORDINATOR_ACTION_RUNTIME_CONF, getRunConf());
        json.put(JsonTags.COORDINATOR_ACTION_LAST_MODIFIED_TIME, JsonUtils
                .formatDateRfc822(getLastModifiedTime(), timeZoneId));
        json.put(JsonTags.COORDINATOR_ACTION_LAST_MODIFIED_TIME,
                JsonUtils.formatDateRfc822(getLastModifiedTime(), timeZoneId));
         // json.put(JsonTags.COORDINATOR_ACTION_START_TIME, JsonUtils
         // .formatDateRfc822(startTime), timeZoneId);
         // json.put(JsonTags.COORDINATOR_ACTION_END_TIME, JsonUtils
         // .formatDateRfc822(endTime), timeZoneId);
        json.put(JsonTags.COORDINATOR_ACTION_MISSING_DEPS, getMissingDependencies());
        json.put(JsonTags.COORDINATOR_ACTION_PUSH_MISSING_DEPS, getPushMissingDependencies());
        json.put(JsonTags.COORDINATOR_ACTION_MISSING_DEPS, getPullInputDependencies().getMissingDependencies());
        json.put(JsonTags.COORDINATOR_ACTION_PUSH_MISSING_DEPS, getPushInputDependencies().getMissingDependencies());
         json.put(JsonTags.COORDINATOR_ACTION_EXTERNAL_STATUS, externalStatus);
         json.put(JsonTags.COORDINATOR_ACTION_TRACKER_URI, trackerUri);
         json.put(JsonTags.COORDINATOR_ACTION_CONSOLE_URL, consoleUrl);
@@ -818,5 +826,27 @@ public class CoordinatorActionBean implements
         return true;
     }
 
    public CoordInputDependency getPullInputDependencies() {
        if (coordPullInputDependency == null) {
            coordPullInputDependency = CoordInputDependencyFactory.getPullInputDependencies(missingDependencies);
        }
        return coordPullInputDependency;

    }

    public CoordInputDependency getPushInputDependencies() {
        if (coordPushInputDependency == null) {
            coordPushInputDependency = CoordInputDependencyFactory.getPushInputDependencies(pushMissingDependencies);
        }
        return coordPushInputDependency;
    }

    public void setPullInputDependencies(CoordInputDependency coordPullInputDependency) {
        this.coordPullInputDependency = coordPullInputDependency;
    }

    public void setPushInputDependencies(CoordInputDependency coordPushInputDependency) {
        this.coordPushInputDependency = coordPushInputDependency;
    }
 
 }
diff --git a/core/src/main/java/org/apache/oozie/ErrorCode.java b/core/src/main/java/org/apache/oozie/ErrorCode.java
index 6c1e3997c..2907ca2b1 100644
-- a/core/src/main/java/org/apache/oozie/ErrorCode.java
++ b/core/src/main/java/org/apache/oozie/ErrorCode.java
@@ -214,6 +214,8 @@ public enum ErrorCode {
     E1025(XLog.STD, "Coord status transit error: [{0}]"),
     E1026(XLog.STD, "SLA alert update command failed: {0}"),
     E1027(XLog.STD, "SLA change command failed. {0}"),
    E1028(XLog.STD, "Coord input logic error. {0}"),

 
 
     E1100(XLog.STD, "Command precondition does not hold before execution, [{0}]"),
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
index 11184d1b9..640d3cbf1 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionInputCheckXCommand.java
@@ -20,13 +20,9 @@ package org.apache.oozie.command.coord;
 
 import java.io.IOException;
 import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.List;

 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.security.AccessControlException;
 import org.apache.oozie.CoordinatorActionBean;
@@ -34,14 +30,11 @@ import org.apache.oozie.CoordinatorJobBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.CoordinatorAction;
 import org.apache.oozie.client.Job;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.coord.CoordELEvaluator;
 import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.dependency.URIHandler;
import org.apache.oozie.dependency.URIHandlerException;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
 import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
@@ -54,7 +47,6 @@ import org.apache.oozie.service.EventHandlerService;
 import org.apache.oozie.service.JPAService;
 import org.apache.oozie.service.Service;
 import org.apache.oozie.service.Services;
import org.apache.oozie.service.URIHandlerService;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ELEvaluator;
 import org.apache.oozie.util.LogUtils;
@@ -159,40 +151,38 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
 
             StringBuilder existList = new StringBuilder();
             StringBuilder nonExistList = new StringBuilder();
            CoordInputDependency coordPullInputDependency = coordAction.getPullInputDependencies();
            CoordInputDependency coordPushInputDependency = coordAction.getPushInputDependencies();


            String missingDependencies = coordPullInputDependency.getMissingDependencies();
             StringBuilder nonResolvedList = new StringBuilder();
            String firstMissingDependency = "";
            String missingDeps = coordAction.getMissingDependencies();
            CoordCommandUtils.getResolvedList(missingDeps, nonExistList, nonResolvedList);
 
            CoordCommandUtils.getResolvedList(missingDependencies, nonExistList, nonResolvedList);
            String firstMissingDependency = "";
             // For clarity regarding which is the missing dependency in synchronous order
             // instead of printing entire list, some of which, may be available
            if(nonExistList.length() > 0) {
            if (nonExistList.length() > 0) {
                 firstMissingDependency = nonExistList.toString().split(CoordELFunctions.INSTANCE_SEPARATOR)[0];
             }
             LOG.info("[" + actionId + "]::CoordActionInputCheck:: Missing deps:" + firstMissingDependency + " "
                     + nonResolvedList.toString());
            // Updating the list of data dependencies that are available and those that are yet not
            boolean status = checkInput(actionXml, existList, nonExistList, actionConf);
            String pushDeps = coordAction.getPushMissingDependencies();
            // Resolve latest/future only when all current missingDependencies and
            // pushMissingDependencies are met


            boolean status = checkResolvedInput(actionXml, existList, nonExistList, actionConf);
            String nonExistListStr = nonExistList.toString();
            boolean isPushDependenciesMet = coordPushInputDependency.isDependencyMet();
             if (status && nonResolvedList.length() > 0) {
                status = (pushDeps == null || pushDeps.length() == 0) ? checkUnResolvedInput(actionXml, actionConf)
                        : false;
                status = (isPushDependenciesMet) ? checkUnResolvedInput(actionXml, actionConf) : false;
             }
             coordAction.setLastModifiedTime(currentTime);
             coordAction.setActionXml(actionXml.toString());
            if (nonResolvedList.length() > 0 && status == false) {
                nonExistList.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR).append(nonResolvedList);
            }
            String nonExistListStr = nonExistList.toString();
            if (!nonExistListStr.equals(missingDeps) || missingDeps.isEmpty()) {
                // missingDeps null or empty means action should become READY
                isChangeInDependency = true;
                coordAction.setMissingDependencies(nonExistListStr);
            }
            if (status && (pushDeps == null || pushDeps.length() == 0)) {
                String newActionXml = resolveCoordConfiguration(actionXml, actionConf, actionId);

            isChangeInDependency = isChangeInDependency(nonExistList, missingDependencies, nonResolvedList, status);

            if (status && isPushDependenciesMet) {
                String newActionXml = resolveCoordConfiguration(actionXml, actionConf, actionId,
                        coordPullInputDependency, coordPushInputDependency);
                 actionXml.replace(0, actionXml.length(), newActionXml);
                 coordAction.setActionXml(actionXml.toString());
                 coordAction.setStatus(CoordinatorAction.Status.READY);
@@ -207,7 +197,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                 updateCoordAction(coordAction, isChangeInDependency);
             }
             else {
                if (!nonExistListStr.isEmpty() && pushDeps == null || pushDeps.length() == 0) {
                if (!nonExistListStr.isEmpty() && isPushDependenciesMet) {
                     queue(new CoordActionTimeOutXCommand(coordAction, coordJob.getUser(), coordJob.getAppName()));
                 }
                 else {
@@ -246,10 +236,25 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
         return null;
     }
 
    private boolean isChangeInDependency(StringBuilder nonExistList, String missingDependencies,
            StringBuilder nonResolvedList, boolean status) throws IOException {
        if (nonResolvedList.length() > 0 && status == false) {
            nonExistList.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR).append(nonResolvedList);
        }
        return coordAction.getPullInputDependencies().isChangeInDependency(nonExistList, missingDependencies,
                nonResolvedList, status);
    }
 
    static String resolveCoordConfiguration(StringBuilder actionXml, Configuration actionConf, String actionId) throws Exception {
    static String resolveCoordConfiguration(StringBuilder actionXml, Configuration actionConf, String actionId)
            throws Exception {
        return resolveCoordConfiguration(actionXml, actionConf, actionId, null, null);
    }

    static String resolveCoordConfiguration(StringBuilder actionXml, Configuration actionConf, String actionId,
            CoordInputDependency pullDependencies, CoordInputDependency pushDependencies) throws Exception {
         Element eAction = XmlUtils.parseXml(actionXml.toString());
        ELEvaluator eval = CoordELEvaluator.createDataEvaluator(eAction, actionConf, actionId);
        ELEvaluator eval = CoordELEvaluator.createDataEvaluator(eAction, actionConf, actionId, pullDependencies,
                pushDependencies);
         materializeDataProperties(eAction, actionConf, eval);
         return XmlUtils.prettyPrint(eAction).toString();
     }
@@ -268,6 +273,7 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
         if (jpaService != null) {
             try {
                 if (isChangeInDependency) {
                    coordAction.setMissingDependencies(coordAction.getPullInputDependencies().serialize());
                     CoordActionQueryExecutor.getInstance().executeUpdate(
                             CoordActionQuery.UPDATE_COORD_ACTION_FOR_INPUTCHECK, coordAction);
                     if (EventHandlerService.isEnabled() && coordAction.getStatus() != CoordinatorAction.Status.READY) {
@@ -281,12 +287,11 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
                             CoordActionQuery.UPDATE_COORD_ACTION_FOR_MODIFIED_DATE, coordAction);
                 }
             }
            catch (JPAExecutorException jex) {
            catch (Exception jex) {
                 throw new CommandException(ErrorCode.E1021, jex.getMessage(), jex);
             }
         }
     }

     /**
      * This function reads the value of re-queue interval for coordinator input
      * check command from the Oozie configuration provided by Configuration
@@ -310,22 +315,44 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
      * @return true if all input paths are existed
      * @throws Exception thrown of unable to check input path
      */
    protected boolean checkInput(StringBuilder actionXml, StringBuilder existList, StringBuilder nonExistList,
    protected boolean checkResolvedInput(StringBuilder actionXml, StringBuilder existList, StringBuilder nonExistList,
             Configuration conf) throws Exception {
        Element eAction = XmlUtils.parseXml(actionXml.toString());
        return checkResolvedUris(eAction, existList, nonExistList, conf);
        return coordAction.getPullInputDependencies().checkPullMissingDependencies(coordAction, existList,
                nonExistList);
     }
 
    protected boolean checkUnResolvedInput(StringBuilder actionXml, Configuration conf) throws Exception {
    /**
     * Check un resolved input.
     *
     * @param coordAction the coord action
     * @param actionXml the action xml
     * @param conf the conf
     * @return true, if successful
     * @throws Exception the exception
     */
    protected boolean checkUnResolvedInput(CoordinatorActionBean coordAction, StringBuilder actionXml,
            Configuration conf) throws Exception {
         Element eAction = XmlUtils.parseXml(actionXml.toString());
         LOG.debug("[" + actionId + "]::ActionInputCheck:: Checking Latest/future");
        boolean allExist = checkUnresolvedInstances(eAction, conf);
        boolean allExist = checkUnresolvedInstances(coordAction, eAction, conf);
         if (allExist) {
             actionXml.replace(0, actionXml.length(), XmlUtils.prettyPrint(eAction).toString());
         }
         return allExist;
     }
 
    /**
     * Check un resolved input.
     *
     * @param actionXml the action xml
     * @param conf the conf
     * @return true, if successful
     * @throws Exception the exception
     */
    protected boolean checkUnResolvedInput(StringBuilder actionXml, Configuration conf) throws Exception {
        return checkUnResolvedInput(coordAction, actionXml, conf);
    }

     /**
      * Materialize data properties defined in <action> tag. it includes dataIn(<DS>) and dataOut(<DS>) it creates a list
      * of files that will be needed.
@@ -378,222 +405,23 @@ public class CoordActionInputCheckXCommand extends CoordinatorXCommand<Void> {
      * @throws Exception thrown if failed to resolve data input and output paths
      */
     @SuppressWarnings("unchecked")
    private boolean checkUnresolvedInstances(Element eAction, Configuration actionConf) throws Exception {
        String strAction = XmlUtils.prettyPrint(eAction).toString();
        Date nominalTime = DateUtils.parseDateOozieTZ(eAction.getAttributeValue("action-nominal-time"));
        String actualTimeStr = eAction.getAttributeValue("action-actual-time");
        Date actualTime = null;
        if (actualTimeStr == null) {
            LOG.debug("Unable to get action-actual-time from action xml, this job is submitted " +
            "from previous version. Assign current date to actual time, action = " + actionId);
            actualTime = new Date();
        } else {
            actualTime = DateUtils.parseDateOozieTZ(actualTimeStr);
        }
    private boolean checkUnresolvedInstances(CoordinatorActionBean coordAction, Element eAction,
            Configuration actionConf) throws Exception {
 
        StringBuffer resultedXml = new StringBuffer();

        boolean ret;
        Element inputList = eAction.getChild("input-events", eAction.getNamespace());
        if (inputList != null) {
            ret = materializeUnresolvedEvent(inputList.getChildren("data-in", eAction.getNamespace()), nominalTime,
                    actualTime, actionConf);
            if (ret == false) {
                resultedXml.append(strAction);
                return false;
            }
        }
        boolean ret = coordAction.getPullInputDependencies().checkUnresolved(coordAction, eAction);
 
         // Using latest() or future() in output-event is not intuitive.
         // We need to make sure, this assumption is correct.
         Element outputList = eAction.getChild("output-events", eAction.getNamespace());
         if (outputList != null) {
             for (Element dEvent : (List<Element>) outputList.getChildren("data-out", eAction.getNamespace())) {
                if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, dEvent.getNamespace()) != null) {
                if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace()) != null) {
                     throw new CommandException(ErrorCode.E1006, "coord:latest()/future()",
                             " not permitted in output-event ");
                 }
             }
         }
        return true;
    }

    /**
     * Resolve the list of data input paths
     *
     * @param eDataEvents the list of data input elements
     * @param nominalTime action nominal time
     * @param actualTime current time
     * @param conf action configuration
     * @return true if all unresolved URIs can be resolved
     * @throws Exception thrown if failed to resolve data input paths
     */
    @SuppressWarnings("unchecked")
    private boolean materializeUnresolvedEvent(List<Element> eDataEvents, Date nominalTime, Date actualTime,
            Configuration conf) throws Exception {
        for (Element dEvent : eDataEvents) {
            if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, dEvent.getNamespace()) == null) {
                continue;
            }
            ELEvaluator eval = CoordELEvaluator.createLazyEvaluator(actualTime, nominalTime, dEvent, conf);
            String uresolvedInstance = dEvent.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, dEvent.getNamespace()).getTextTrim();
            String unresolvedList[] = uresolvedInstance.split(CoordELFunctions.INSTANCE_SEPARATOR);
            StringBuffer resolvedTmp = new StringBuffer();
            for (int i = 0; i < unresolvedList.length; i++) {
                String ret = CoordELFunctions.evalAndWrap(eval, unresolvedList[i]);
                Boolean isResolved = (Boolean) eval.getVariable("is_resolved");
                if (isResolved == false) {
                    LOG.info("[" + actionId + "]::Cannot resolve: " + ret);
                    return false;
                }
                if (resolvedTmp.length() > 0) {
                    resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR);
                }
                resolvedTmp.append((String) eval.getVariable("resolved_path"));
            }
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
            dEvent.removeChild(CoordCommandUtils.UNRESOLVED_INST_TAG, dEvent.getNamespace());
        }

        return true;
    }

    /**
     * Check all resolved URIs existence
     *
     * @param eAction action element
     * @param existList the list of existed paths
     * @param nonExistList the list of paths to check existence
     * @param conf action configuration
     * @return true if all nonExistList paths exist
     * @throws IOException thrown if unable to access the path
     */
    private boolean checkResolvedUris(Element eAction, StringBuilder existList, StringBuilder nonExistList,
            Configuration conf) throws IOException {
        Element inputList = eAction.getChild("input-events", eAction.getNamespace());
        if (inputList != null) {
            if (nonExistList.length() > 0) {
                checkListOfPaths(existList, nonExistList, conf);
            }
            return nonExistList.length() == 0;
        }
        return true;
    }

    /**
     * Check a list of non existed paths and add to exist list if it exists
     *
     * @param existList the list of existed paths
     * @param nonExistList the list of paths to check existence
     * @param conf action configuration
     * @return true if all nonExistList paths exist
     * @throws IOException thrown if unable to access the path
     */
    private boolean checkListOfPaths(StringBuilder existList, StringBuilder nonExistList, Configuration conf)
            throws IOException {

        String[] uriList = nonExistList.toString().split(CoordELFunctions.INSTANCE_SEPARATOR);
        if (uriList[0] != null) {
            LOG.info("[" + actionId + "]::ActionInputCheck:: In checkListOfPaths: " + uriList[0] + " is Missing.");
        }

        nonExistList.delete(0, nonExistList.length());
        boolean allExists = true;
        String existSeparator = "", nonExistSeparator = "";
        String user = ParamChecker.notEmpty(conf.get(OozieClient.USER_NAME), OozieClient.USER_NAME);
        for (int i = 0; i < uriList.length; i++) {
            if (allExists) {
                allExists = pathExists(uriList[i], conf, user);
                LOG.info("[" + actionId + "]::ActionInputCheck:: File:" + uriList[i] + ", Exists? :" + allExists);
            }
            if (allExists) {
                existList.append(existSeparator).append(uriList[i]);
                existSeparator = CoordELFunctions.INSTANCE_SEPARATOR;
            }
            else {
                nonExistList.append(nonExistSeparator).append(uriList[i]);
                nonExistSeparator = CoordELFunctions.INSTANCE_SEPARATOR;
            }
        }
        return allExists;
    }

    /**
     * Check if given path exists
     *
     * @param sPath uri path
     * @param actionConf action configuration
     * @return true if path exists
     * @throws IOException thrown if unable to access the path
     */
    protected boolean pathExists(String sPath, Configuration actionConf, String user) throws IOException {
        LOG.debug("checking for the file " + sPath);
        try {
            URI uri = new URI(sPath);
            URIHandlerService service = Services.get().get(URIHandlerService.class);
            URIHandler handler = service.getURIHandler(uri);
            return handler.exists(uri, actionConf, user);
        }
        catch (URIHandlerException e) {
            coordAction.setErrorCode(e.getErrorCode().toString());
            coordAction.setErrorMessage(e.getMessage());
            if (e.getCause() != null && e.getCause() instanceof AccessControlException) {
                throw (AccessControlException) e.getCause();
            }
            else {
                throw new IOException(e);
            }
        }
        catch (URISyntaxException e) {
            coordAction.setErrorCode(ErrorCode.E0906.toString());
            coordAction.setErrorMessage(e.getMessage());
            throw new IOException(e);
        }
    }

    /**
     * The function create a list of URIs separated by "," using the instances time stamp and URI-template
     *
     * @param event : <data-in> event
     * @param instances : List of time stamp seprated by ","
     * @param unresolvedInstances : list of instance with latest/future function
     * @return : list of URIs separated by ",".
     * @throws Exception thrown if failed to create URIs from unresolvedInstances
     */
    @SuppressWarnings("unused")
    private String createURIs(Element event, String instances, StringBuilder unresolvedInstances) throws Exception {
        if (instances == null || instances.length() == 0) {
            return "";
        }
        String[] instanceList = instances.split(CoordELFunctions.INSTANCE_SEPARATOR);
        StringBuilder uris = new StringBuilder();

        for (int i = 0; i < instanceList.length; i++) {
            int funcType = CoordCommandUtils.getFuncType(instanceList[i]);
            if (funcType == CoordCommandUtils.LATEST || funcType == CoordCommandUtils.FUTURE) {
                if (unresolvedInstances.length() > 0) {
                    unresolvedInstances.append(CoordELFunctions.INSTANCE_SEPARATOR);
                }
                unresolvedInstances.append(instanceList[i]);
                continue;
            }
            ELEvaluator eval = CoordELEvaluator.createURIELEvaluator(instanceList[i]);
            if (uris.length() > 0) {
                uris.append(CoordELFunctions.INSTANCE_SEPARATOR);
            }
            uris.append(CoordELFunctions.evalAndWrap(eval, event.getChild("dataset", event.getNamespace()).getChild(
                    "uri-template", event.getNamespace()).getTextTrim()));
        }
        return uris.toString();
        return ret;
     }
 
     /**
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordActionUpdatePushMissingDependency.java b/core/src/main/java/org/apache/oozie/command/coord/CoordActionUpdatePushMissingDependency.java
index 4e1c5b339..cb866e2e7 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordActionUpdatePushMissingDependency.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordActionUpdatePushMissingDependency.java
@@ -18,11 +18,9 @@
 
 package org.apache.oozie.command.coord;
 
import java.util.ArrayList;
import java.util.Arrays;
 import java.util.Collection;
import java.util.List;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 import org.apache.oozie.dependency.DependencyChecker;
 import org.apache.oozie.service.PartitionDependencyManagerService;
 import org.apache.oozie.service.Services;
@@ -35,9 +33,11 @@ public class CoordActionUpdatePushMissingDependency extends CoordPushDependencyC
 
     @Override
     protected Void execute() throws CommandException {
        CoordInputDependency coordPushInputDependency = coordAction.getPushInputDependencies();
        CoordInputDependency coordPullInputDependency = coordAction.getPullInputDependencies();

         LOG.info("STARTED for Action id [{0}]", actionId);
        String pushMissingDeps = coordAction.getPushMissingDependencies();
        if (pushMissingDeps == null || pushMissingDeps.length() == 0) {
        if (coordPushInputDependency.isDependencyMet()) {
             LOG.info("Nothing to check. Empty push missing dependency");
         }
         else {
@@ -50,25 +50,19 @@ public class CoordActionUpdatePushMissingDependency extends CoordPushDependencyC
                 }
             }
             else {
                LOG.debug("Updating with available uris=[{0}] where missing uris=[{1}]", availDepList.toString(),
                        pushMissingDeps);

                String[] missingDepsArray = DependencyChecker.dependenciesAsArray(pushMissingDeps);
                List<String> stillMissingDepsList = new ArrayList<String>(Arrays.asList(missingDepsArray));
                stillMissingDepsList.removeAll(availDepList);
                String pushMissingDependencies = coordPushInputDependency.getMissingDependencies().toString();
                LOG.debug("Updating with available uris = [{0}] where missing uris = [{1}]", pushMissingDependencies);
                String[] missingDependenciesArray = DependencyChecker.dependenciesAsArray(pushMissingDependencies);
                coordPushInputDependency.addToAvailableDependencies(availDepList);
                 boolean isChangeInDependency = true;
                if (stillMissingDepsList.size() == 0) {
                if (coordPushInputDependency.isDependencyMet()) {
                     // All push-based dependencies are available
                    onAllPushDependenciesAvailable();
                    onAllPushDependenciesAvailable(coordPullInputDependency.isDependencyMet());
                 }
                 else {
                    if (stillMissingDepsList.size() == missingDepsArray.length) {
                    if (coordPushInputDependency.getMissingDependenciesAsList().size() == missingDependenciesArray.length) {
                         isChangeInDependency = false;
                     }
                    else {
                        String stillMissingDeps = DependencyChecker.dependenciesAsString(stillMissingDepsList);
                        coordAction.setPushMissingDependencies(stillMissingDeps);
                    }
                     if (isTimeout()) { // Poll and check as one last try
                         queue(new CoordPushDependencyCheckXCommand(coordAction.getId()), 100);
                     }
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java b/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
index 58ef48327..0af7edc90 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordCommandUtils.java
@@ -18,9 +18,12 @@
 
 package org.apache.oozie.command.coord;
 
import java.io.IOException;
 import java.io.StringReader;
 import java.net.URI;
import java.net.URISyntaxException;
 import java.text.ParseException;
import java.util.ArrayList;
 import java.util.TimeZone;
 import java.util.Map;
 import java.util.HashMap;
@@ -32,6 +35,7 @@ import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.coord.CoordELEvaluator;
 import org.apache.oozie.coord.CoordELFunctions;
@@ -39,17 +43,25 @@ import org.apache.oozie.coord.CoordUtils;
 import org.apache.oozie.coord.CoordinatorJobException;
 import org.apache.oozie.coord.SyncCoordAction;
 import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorUtil;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluator;
import org.apache.oozie.coord.input.dependency.CoordInputDependencyFactory;
import org.apache.oozie.coord.input.dependency.CoordInputInstance;
 import org.apache.oozie.dependency.ActionDependency;
 import org.apache.oozie.dependency.DependencyChecker;
 import org.apache.oozie.dependency.URIHandler;
 import org.apache.oozie.dependency.URIHandler.DependencyType;
import org.apache.oozie.dependency.URIHandlerException;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.service.URIHandlerService;
 import org.apache.oozie.service.UUIDService;
 import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ELEvaluator;
import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XConfiguration;
 import org.apache.oozie.util.XmlUtils;
import org.jdom.Attribute;
 import org.jdom.Element;
 import org.jdom.JDOMException;
 import org.quartz.CronExpression;
@@ -63,8 +75,9 @@ public class CoordCommandUtils {
     public static int OFFSET = 3;
     public static int ABSOLUTE = 4;
     public static int UNEXPECTED = -1;

     public static final String RESOLVED_UNRESOLVED_SEPARATOR = "!!";
    public static final String UNRESOLVED_INST_TAG = "unresolved-instances";
    public static final String UNRESOLVED_INSTANCES_TAG = "unresolved-instances";
 
     /**
      * parse a function like coord:latest(n)/future() and return the 'n'.
@@ -357,7 +370,7 @@ public class CoordCommandUtils {
             depList.append(urisWithDoneFlag);
         }
         if (unresolvedInstances.length() > 0) {
            Element elemInstance = new Element(UNRESOLVED_INST_TAG, event.getNamespace());
            Element elemInstance = new Element(UNRESOLVED_INSTANCES_TAG, event.getNamespace());
             elemInstance.addContent(unresolvedInstances.toString());
             event.getContent().add(1, elemInstance);
         }
@@ -482,20 +495,24 @@ public class CoordCommandUtils {
         appInst.setTimeZone(DateUtils.getTimeZone(eAction.getAttributeValue("timezone")));
         appInst.setEndOfDuration(TimeUnit.valueOf(eAction.getAttributeValue("end_of_duration")));
 
        Map<String, StringBuilder> dependencyMap = null;
        boolean isInputLogicSpecified = CoordUtils.isInputLogicSpecified(eAction);
 
         Element inputList = eAction.getChild("input-events", eAction.getNamespace());
         List<Element> dataInList = null;
         if (inputList != null) {
             dataInList = inputList.getChildren("data-in", eAction.getNamespace());
            dependencyMap = materializeDataEvents(dataInList, appInst, conf);
            materializeInputDataEvents(dataInList, appInst, conf, actionBean, isInputLogicSpecified);
         }
 
        if(isInputLogicSpecified){
            evaluateInputCheck(eAction.getChild(CoordInputLogicEvaluator.INPUT_LOGIC, eAction.getNamespace()),
                    CoordELEvaluator.createDataEvaluator(eAction, conf, actionId));
        }
         Element outputList = eAction.getChild("output-events", eAction.getNamespace());
         List<Element> dataOutList = null;
         if (outputList != null) {
             dataOutList = outputList.getChildren("data-out", eAction.getNamespace());
            materializeDataEvents(dataOutList, appInst, conf);
            materializeOutputDataEvents(dataOutList, appInst, conf);
         }
 
         eAction.removeAttribute("start");
@@ -513,16 +530,6 @@ public class CoordCommandUtils {
         actionBean.setLastModifiedTime(new Date());
         actionBean.setStatus(CoordinatorAction.Status.WAITING);
         actionBean.setActionNumber(instanceCount);
        if (dependencyMap != null) {
            StringBuilder sbPull = dependencyMap.get(DependencyType.PULL.name());
            if (sbPull != null) {
                actionBean.setMissingDependencies(sbPull.toString());
            }
            StringBuilder sbPush = dependencyMap.get(DependencyType.PUSH.name());
            if (sbPush != null) {
                actionBean.setPushMissingDependencies(sbPush.toString());
            }
        }
         actionBean.setNominalTime(nominalTime);
         boolean isSla = CoordCommandUtils.materializeSLA(eAction, actionBean, conf);
         if (isSla == true) {
@@ -544,6 +551,7 @@ public class CoordCommandUtils {
         }
     }
 

     /**
      * @param eAction the actionXml related element
      * @param actionBean the coordinator action bean
@@ -554,12 +562,18 @@ public class CoordCommandUtils {
         String action = XmlUtils.prettyPrint(eAction).toString();
         StringBuilder actionXml = new StringBuilder(action);
         Configuration actionConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        actionBean.setActionXml(action);

        if (CoordUtils.isInputLogicSpecified(eAction)) {
            new CoordInputLogicEvaluatorUtil(actionBean).validateInputLogic();
        }
 
         boolean isPushDepAvailable = true;
        if (actionBean.getPushMissingDependencies() != null) {
            ActionDependency actionDep = DependencyChecker.checkForAvailability(
                    actionBean.getPushMissingDependencies(), actionConf, true);
            if (actionDep.getMissingDependencies().size() != 0) {
        String pushMissingDependencies = actionBean.getPushInputDependencies().getMissingDependencies();
        if (pushMissingDependencies != null) {
            ActionDependency actionDependencies = DependencyChecker.checkForAvailability(pushMissingDependencies,
                    actionConf, true);
            if (actionDependencies.getMissingDependencies().size() != 0) {
                 isPushDepAvailable = false;
             }
 
@@ -571,13 +585,16 @@ public class CoordCommandUtils {
             StringBuilder existList = new StringBuilder();
             StringBuilder nonExistList = new StringBuilder();
             StringBuilder nonResolvedList = new StringBuilder();
            getResolvedList(actionBean.getMissingDependencies(), nonExistList, nonResolvedList);
            isPullDepAvailable = coordActionInput.checkInput(actionXml, existList, nonExistList, actionConf);
            getResolvedList(actionBean.getPullInputDependencies().getMissingDependencies(), nonExistList, nonResolvedList);
            isPullDepAvailable = actionBean.getPullInputDependencies().checkPullMissingDependencies(actionBean,
                    existList, nonExistList);

         }
 
         if (isPullDepAvailable && isPushDepAvailable) {
             // Check for latest/future
            boolean isLatestFutureDepAvailable = coordActionInput.checkUnResolvedInput(actionXml, actionConf);
            boolean isLatestFutureDepAvailable = coordActionInput.checkUnResolvedInput(actionBean, actionXml,
                    actionConf);
             if (isLatestFutureDepAvailable) {
                 String newActionXml = CoordActionInputCheckXCommand.resolveCoordConfiguration(actionXml, actionConf,
                         actionBean.getId());
@@ -598,17 +615,68 @@ public class CoordCommandUtils {
      * @param conf
      * @throws Exception
      */
    public static Map<String, StringBuilder> materializeDataEvents(List<Element> events, SyncCoordAction appInst, Configuration conf
            ) throws Exception {
    private static void materializeOutputDataEvents(List<Element> events, SyncCoordAction appInst, Configuration conf)
            throws Exception {
 
         if (events == null) {
            return null;
            return;
        }

        for (Element event : events) {
            StringBuilder instances = new StringBuilder();
            ELEvaluator eval = CoordELEvaluator.createInstancesELEvaluator(event, appInst, conf);
            // Handle list of instance tag
            resolveInstances(event, instances, appInst, conf, eval);
            // Handle start-instance and end-instance
            resolveInstanceRange(event, instances, appInst, conf, eval);
            // Separate out the unresolved instances
            separateResolvedAndUnresolved(event, instances);

        }
    }

    private static void evaluateInputCheck(Element root, ELEvaluator evalInputLogic) throws Exception {
        for (Object event : root.getChildren()) {
            Element inputElement = (Element) event;

            resolveAttribute("dataset", inputElement, evalInputLogic);
            resolveAttribute("name", inputElement, evalInputLogic);
            resolveAttribute("min", inputElement, evalInputLogic);
            resolveAttribute("wait", inputElement, evalInputLogic);
            if (!inputElement.getChildren().isEmpty()) {
                evaluateInputCheck(inputElement, evalInputLogic);
            }
         }
        StringBuilder unresolvedList = new StringBuilder();
        Map<String, StringBuilder> dependencyMap = new HashMap<String, StringBuilder>();
    }

    private static String resolveAttribute(String attrName, Element elem, ELEvaluator eval) throws CoordinatorJobException {
        Attribute attr = elem.getAttribute(attrName);
        String val = null;
        if (attr != null) {
            try {
                val = CoordELFunctions.evalAndWrap(eval, attr.getValue().trim());
            }
            catch (Exception e) {
                throw new CoordinatorJobException(ErrorCode.E1004, e.getMessage(), e);
            }
            attr.setValue(val);
        }
        return val;
    }

    public static void materializeInputDataEvents(List<Element> events, SyncCoordAction appInst, Configuration conf,
            CoordinatorActionBean actionBean, boolean isInputLogicSpecified) throws Exception {

        if (events == null) {
            return;
        }
        CoordInputDependency coordPullInputDependency = CoordInputDependencyFactory
                .createPullInputDependencies(isInputLogicSpecified);
        CoordInputDependency coordPushInputDependency = CoordInputDependencyFactory
                .createPushInputDependencies(isInputLogicSpecified);
        Map<String, String> unresolvedList = new HashMap<String, String>();

         URIHandlerService uriService = Services.get().get(URIHandlerService.class);
        StringBuilder pullMissingDep = null;
        StringBuilder pushMissingDep = null;
 
         for (Element event : events) {
             StringBuilder instances = new StringBuilder();
@@ -619,41 +687,44 @@ public class CoordCommandUtils {
             resolveInstanceRange(event, instances, appInst, conf, eval);
             // Separate out the unresolved instances
             String resolvedList = separateResolvedAndUnresolved(event, instances);
            String name = event.getAttribute("name").getValue();

             if (!resolvedList.isEmpty()) {
                 Element uri = event.getChild("dataset", event.getNamespace()).getChild("uri-template",
                         event.getNamespace());

                 String uriTemplate = uri.getText();
                 URI baseURI = uriService.getAuthorityWithScheme(uriTemplate);
                 URIHandler handler = uriService.getURIHandler(baseURI);
                List<CoordInputInstance> inputInstanceList = new ArrayList<CoordInputInstance>();

                for (String inputInstance : resolvedList.split("#")) {
                    inputInstanceList.add(new CoordInputInstance(inputInstance, false));
                }

                 if (handler.getDependencyType(baseURI).equals(DependencyType.PULL)) {
                    pullMissingDep = (pullMissingDep == null) ? new StringBuilder(resolvedList) : pullMissingDep.append(
                            CoordELFunctions.INSTANCE_SEPARATOR).append(resolvedList);
                    coordPullInputDependency.addInputInstanceList(name, inputInstanceList);
                 }
                 else {
                    pushMissingDep = (pushMissingDep == null) ? new StringBuilder(resolvedList) : pushMissingDep.append(
                            CoordELFunctions.INSTANCE_SEPARATOR).append(resolvedList);
                    coordPushInputDependency.addInputInstanceList(name, inputInstanceList);

                 }
             }
 
            String tmpUnresolved = event.getChildTextTrim(UNRESOLVED_INST_TAG, event.getNamespace());
            String tmpUnresolved = event.getChildTextTrim(UNRESOLVED_INSTANCES_TAG, event.getNamespace());
             if (tmpUnresolved != null) {
                if (unresolvedList.length() > 0) {
                    unresolvedList.append(CoordELFunctions.INSTANCE_SEPARATOR);
                }
                unresolvedList.append(tmpUnresolved);
                unresolvedList.put(name, tmpUnresolved);
             }
         }
        if (unresolvedList.length() > 0) {
            if (pullMissingDep == null) {
                pullMissingDep = new StringBuilder();
            }
            pullMissingDep.append(RESOLVED_UNRESOLVED_SEPARATOR).append(unresolvedList);
        for(String unresolvedDatasetName:unresolvedList.keySet()){
            coordPullInputDependency.addUnResolvedList(unresolvedDatasetName, unresolvedList.get(unresolvedDatasetName));
         }
        dependencyMap.put(DependencyType.PULL.name(), pullMissingDep);
        dependencyMap.put(DependencyType.PUSH.name(), pushMissingDep);
        return dependencyMap;
    }
        actionBean.setPullInputDependencies(coordPullInputDependency);
        actionBean.setPushInputDependencies(coordPushInputDependency);
        actionBean.setMissingDependencies(coordPullInputDependency.serialize());
        actionBean.setPushMissingDependencies(coordPushInputDependency.serialize());
 
    }
     /**
      * Get resolved string from missDepList
      *
@@ -797,4 +868,19 @@ public class CoordCommandUtils {
         }
         return nextNominalTime;
     }

    public static boolean pathExists(String sPath, Configuration actionConf, String user) throws IOException,
            URISyntaxException, URIHandlerException {
        URI uri = new URI(sPath);
        URIHandlerService service = Services.get().get(URIHandlerService.class);
        URIHandler handler = service.getURIHandler(uri);
        return handler.exists(uri, actionConf, user);
    }

    public static boolean pathExists(String sPath, Configuration actionConf) throws IOException, URISyntaxException,
            URIHandlerException {
        String user = ParamChecker.notEmpty(actionConf.get(OozieClient.USER_NAME), OozieClient.USER_NAME);
        return pathExists(sPath, actionConf, user);
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
index 39e6ac15c..f6c178217 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordMaterializeTransitionXCommand.java
@@ -18,6 +18,7 @@
 
 package org.apache.oozie.command.coord;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.AppType;
 import org.apache.oozie.CoordinatorActionBean;
@@ -34,6 +35,7 @@ import org.apache.oozie.command.PreconditionException;
 import org.apache.oozie.command.bundle.BundleStatusUpdateXCommand;
 import org.apache.oozie.coord.CoordUtils;
 import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorUtil;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor;
 import org.apache.oozie.executor.jpa.BatchQueryExecutor.UpdateEntry;
 import org.apache.oozie.executor.jpa.CoordActionsActiveCountJPAExecutor;
@@ -148,7 +150,7 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
                     queue(new CoordActionInputCheckXCommand(coordAction.getId(), coordAction.getJobId()),
                         Math.max(checkDelay, 0));
 
                    if (coordAction.getPushMissingDependencies() != null) {
                    if (!StringUtils.isEmpty(coordAction.getPushMissingDependencies())) {
                         // TODO: Delay in catchup mode?
                         queue(new CoordPushDependencyCheckXCommand(coordAction.getId(), true), 100);
                     }
@@ -485,7 +487,6 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
                 action = CoordCommandUtils.materializeOneInstance(jobId, dryrun, (Element) eJob.clone(),
                         nextTime, actualTime, lastActionNumber, jobConf, actionBean);
                 actionBean.setTimeOut(timeout);

                 if (!dryrun) {
                     storeToDB(actionBean, action, jobConf); // Storing to table
 
@@ -529,7 +530,6 @@ public class CoordMaterializeTransitionXCommand extends MaterializeTransitionXCo
         LOG.debug("In storeToDB() coord action id = " + actionBean.getId() + ", size of actionXml = "
                 + actionXml.length());
         actionBean.setActionXml(actionXml);

         insertList.add(actionBean);
         writeActionSlaRegistration(actionXml, actionBean, jobConf);
     }
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
index b05344d89..2600a2bde 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordPushDependencyCheckXCommand.java
@@ -21,10 +21,10 @@ package org.apache.oozie.command.coord;
 import java.io.IOException;
 import java.io.StringReader;
 import java.net.URI;
import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
 
import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.CoordinatorJobBean;
@@ -34,7 +34,7 @@ import org.apache.oozie.client.Job;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.command.PreconditionException;
import org.apache.oozie.dependency.DependencyChecker;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 import org.apache.oozie.dependency.ActionDependency;
 import org.apache.oozie.dependency.URIHandler;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
@@ -113,14 +113,15 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
             return null;
         }
 
        String pushMissingDeps = coordAction.getPushMissingDependencies();
        if (pushMissingDeps == null || pushMissingDeps.length() == 0) {
        CoordInputDependency coordPushInputDependency = coordAction.getPushInputDependencies();
        CoordInputDependency coordPullInputDependency = coordAction.getPullInputDependencies();
        if (coordPushInputDependency.getMissingDependenciesAsList().size() == 0) {
             LOG.info("Nothing to check. Empty push missing dependency");
         }
         else {
            String[] missingDepsArray = DependencyChecker.dependenciesAsArray(pushMissingDeps);
            LOG.info("First Push missing dependency is [{0}] ", missingDepsArray[0]);
            LOG.trace("Push missing dependencies are [{0}] ", pushMissingDeps);
            List<String> missingDependenciesArray = coordPushInputDependency.getMissingDependenciesAsList();
            LOG.info("First Push missing dependency is [{0}] ", missingDependenciesArray.get(0));
            LOG.trace("Push missing dependencies are [{0}] ", missingDependenciesArray);
             if (registerForNotification) {
                 LOG.debug("Register for notifications is true");
             }
@@ -134,27 +135,27 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
                     throw new CommandException(ErrorCode.E1307, e.getMessage(), e);
                 }
 

                boolean isChangeInDependency = true;
                boolean timeout = false;
                ActionDependency actionDependency = coordPushInputDependency.checkPushMissingDependencies(coordAction,
                        registerForNotification);
                 // Check all dependencies during materialization to avoid registering in the cache.
                 // But check only first missing one afterwards similar to
                 // CoordActionInputCheckXCommand for efficiency. listPartitions is costly.
                ActionDependency actionDep = DependencyChecker.checkForAvailability(missingDepsArray, actionConf,
                        !registerForNotification);
                if (actionDependency.getMissingDependencies().size() == missingDependenciesArray.size()) {
                    isChangeInDependency = false;
                }
                else {
                    coordPushInputDependency.setMissingDependencies(StringUtils.join(
                            actionDependency.getMissingDependencies(), CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR));
                }
 
                boolean isChangeInDependency = true;
                boolean timeout = false;
                if (actionDep.getMissingDependencies().size() == 0) {
                if (coordPushInputDependency.isDependencyMet()) {
                     // All push-based dependencies are available
                    onAllPushDependenciesAvailable();
                    onAllPushDependenciesAvailable(coordPullInputDependency.isDependencyMet());
                 }
                 else {
                    if (actionDep.getMissingDependencies().size() == missingDepsArray.length) {
                        isChangeInDependency = false;
                    }
                    else {
                        String stillMissingDeps = DependencyChecker.dependenciesAsString(actionDep
                                .getMissingDependencies());
                        coordAction.setPushMissingDependencies(stillMissingDeps);
                    }
                     // Checking for timeout
                     timeout = isTimeout();
                     if (timeout) {
@@ -166,15 +167,15 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
                     }
                 }
 
                updateCoordAction(coordAction, isChangeInDependency);
                updateCoordAction(coordAction, isChangeInDependency || coordPushInputDependency.isDependencyMet());
                 if (registerForNotification) {
                    registerForNotification(actionDep.getMissingDependencies(), actionConf);
                    registerForNotification(coordPushInputDependency.getMissingDependenciesAsList(), actionConf);
                 }
                 if (removeAvailDependencies) {
                    unregisterAvailableDependencies(actionDep.getAvailableDependencies());
                    unregisterAvailableDependencies(actionDependency.getAvailableDependencies());
                 }
                 if (timeout) {
                    unregisterMissingDependencies(actionDep.getMissingDependencies(), actionId);
                    unregisterMissingDependencies(coordPushInputDependency.getMissingDependenciesAsList(), actionId);
                 }
             }
             catch (Exception e) {
@@ -183,10 +184,9 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
                     LOG.debug("Queueing timeout command");
                     // XCommand.queue() will not work when there is a Exception
                     callableQueueService.queue(new CoordActionTimeOutXCommand(coordAction, coordJob.getUser(), coordJob.getAppName()));
                    unregisterMissingDependencies(Arrays.asList(missingDepsArray), actionId);
                    unregisterMissingDependencies(missingDependenciesArray, actionId);
                 }
                else if (coordAction.getMissingDependencies() != null
                        && coordAction.getMissingDependencies().length() > 0) {
                else if (coordPullInputDependency.getMissingDependenciesAsList().size() > 0) {
                     // Queue again on exception as RecoveryService will not queue this again with
                     // the action being updated regularly by CoordActionInputCheckXCommand
                     callableQueueService.queue(new CoordPushDependencyCheckXCommand(coordAction.getId(),
@@ -221,18 +221,18 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
         return (timeOut >= 0) && (waitingTime > timeOut);
     }
 
    protected void onAllPushDependenciesAvailable() throws CommandException {
        coordAction.setPushMissingDependencies("");
    protected void onAllPushDependenciesAvailable(boolean isPullDependencyMeet) throws CommandException {
         Services.get().get(PartitionDependencyManagerService.class)
                 .removeCoordActionWithDependenciesAvailable(coordAction.getId());
        if (coordAction.getMissingDependencies() == null || coordAction.getMissingDependencies().length() == 0) {
        if (isPullDependencyMeet) {
             Date nominalTime = coordAction.getNominalTime();
             Date currentTime = new Date();
             // The action should become READY only if current time > nominal time;
             // CoordActionInputCheckXCommand will take care of moving it to READY when it is nominal time.
             if (nominalTime.compareTo(currentTime) > 0) {
                 LOG.info("[" + actionId + "]::ActionInputCheck:: nominal Time is newer than current time. Current="
                        + DateUtils.formatDateOozieTZ(currentTime) + ", nominal=" + DateUtils.formatDateOozieTZ(nominalTime));
                        + DateUtils.formatDateOozieTZ(currentTime) + ", nominal="
                        + DateUtils.formatDateOozieTZ(nominalTime));
             }
             else {
                 String actionXml = resolveCoordConfiguration();
@@ -248,6 +248,8 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
             // wait till RecoveryService kicks in
             queue(new CoordActionInputCheckXCommand(coordAction.getId(), coordAction.getJobId()));
         }
        coordAction.getPushInputDependencies().setDependencyMet(true);

     }
 
     private String resolveCoordConfiguration() throws CommandException {
@@ -255,7 +257,8 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
             Configuration actionConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
             StringBuilder actionXml = new StringBuilder(coordAction.getActionXml());
             String newActionXml = CoordActionInputCheckXCommand.resolveCoordConfiguration(actionXml, actionConf,
                    actionId);
                    actionId, coordAction.getPullInputDependencies(), coordAction
                            .getPushInputDependencies());
             actionXml.replace(0, actionXml.length(), newActionXml);
             return actionXml.toString();
         }
@@ -270,6 +273,7 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
         if (jpaService != null) {
             try {
                 if (isChangeInDependency) {
                    coordAction.setPushMissingDependencies(coordAction.getPushInputDependencies().serialize());
                     CoordActionQueryExecutor.getInstance().executeUpdate(
                             CoordActionQuery.UPDATE_COORD_ACTION_FOR_PUSH_INPUTCHECK, coordAction);
                     if (EventHandlerService.isEnabled() && coordAction.getStatus() != CoordinatorAction.Status.READY) {
@@ -286,6 +290,9 @@ public class CoordPushDependencyCheckXCommand extends CoordinatorXCommand<Void>
             catch (JPAExecutorException jex) {
                 throw new CommandException(ErrorCode.E1021, jex.getMessage(), jex);
             }
            catch (IOException ioe) {
                throw new CommandException(ErrorCode.E1021, ioe.getMessage(), ioe);
            }
         }
     }
 
diff --git a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
index d4d1c0814..f1f9ab2d5 100644
-- a/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
++ b/core/src/main/java/org/apache/oozie/command/coord/CoordSubmitXCommand.java
@@ -53,8 +53,10 @@ import org.apache.oozie.command.SubmitTransitionXCommand;
 import org.apache.oozie.command.bundle.BundleStatusUpdateXCommand;
 import org.apache.oozie.coord.CoordELEvaluator;
 import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.CoordUtils;
 import org.apache.oozie.coord.CoordinatorJobException;
 import org.apache.oozie.coord.TimeUnit;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluator;
 import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
 import org.apache.oozie.service.CoordMaterializeTriggerService;
@@ -799,6 +801,11 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
         resolveIODataset(eAppXml);
         resolveIOEvents(eAppXml, dataNameList);
 
        if (CoordUtils.isInputLogicSpecified(eAppXml)) {
            resolveInputLogic(eAppXml.getChild(CoordInputLogicEvaluator.INPUT_LOGIC, eAppXml.getNamespace()), evalInst,
                    dataNameList);
        }

         resolveTagContents("app-path", eAppXml.getChild("action", eAppXml.getNamespace()).getChild("workflow",
                 eAppXml.getNamespace()), evalNofuncs);
         // TODO: If action or workflow tag is missing, NullPointerException will
@@ -896,6 +903,26 @@ public class CoordSubmitXCommand extends SubmitTransitionXCommand {
 
     }
 
    private void resolveInputLogic(Element root, ELEvaluator evalInputLogic, HashMap<String, String> dataNameList)
            throws Exception {
        for (Object event : root.getChildren()) {
            Element inputElement = (Element) event;
            resolveAttribute("dataset", inputElement, evalInputLogic);
            String name=resolveAttribute("name", inputElement, evalInputLogic);
            resolveAttribute("or", inputElement, evalInputLogic);
            resolveAttribute("and", inputElement, evalInputLogic);
            resolveAttribute("combine", inputElement, evalInputLogic);

            if (name != null) {
                dataNameList.put(name, "data-in");
            }

            if (!inputElement.getChildren().isEmpty()) {
                resolveInputLogic(inputElement, evalInputLogic, dataNameList);
            }
        }
    }

     /**
      * Resolve input-events/dataset and output-events/dataset tags.
      *
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELConstants.java b/core/src/main/java/org/apache/oozie/coord/CoordELConstants.java
index f010a817f..eabf473ee 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELConstants.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELConstants.java
@@ -33,4 +33,7 @@ public class CoordELConstants {
     public static final int SUBMIT_DAYS = 24 * 60;
 
     public static final String DEFAULT_DONE_FLAG = "_SUCCESS";
    final public static String RESOLVED_PATH = "resolved_path";

    final public static String IS_RESOLVED = "is_resolved";
 }
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java b/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
index 8b2f4560a..fba8ac1b5 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELEvaluator.java
@@ -28,6 +28,8 @@ import java.util.Map;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.CoordinatorActionBean;
 import org.apache.oozie.command.coord.CoordCommandUtils;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluator;
 import org.apache.oozie.service.ELService;
 import org.apache.oozie.service.Services;
 import org.apache.oozie.util.DateUtils;
@@ -141,7 +143,7 @@ public class CoordELEvaluator {
                     uris = uris.replaceAll(CoordELFunctions.INSTANCE_SEPARATOR, CoordELFunctions.DIR_SEPARATOR);
                     eval.setVariable(".dataout." + data.getAttributeValue("name"), uris);
                 }
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, data.getNamespace()) != null) {
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, data.getNamespace()) != null) {
                     eval.setVariable(".dataout." + data.getAttributeValue("name") + ".unresolved", "true");
                 }
             }
@@ -172,7 +174,13 @@ public class CoordELEvaluator {
      * @return configured ELEvaluator
      * @throws Exception : If there is any date-time string in wrong format, the exception is thrown
      */

     public static ELEvaluator createDataEvaluator(Element eJob, Configuration conf, String actionId) throws Exception {
        return createDataEvaluator(eJob, conf, actionId, null, null);
    }

    public static ELEvaluator createDataEvaluator(Element eJob, Configuration conf, String actionId,
            CoordInputDependency pullDependencies, CoordInputDependency pushDependencies) throws Exception {
         ELEvaluator e = Services.get().get(ELService.class).createEvaluator("coord-action-start");
         setConfigToEval(e, conf);
         SyncCoordAction appInst = new SyncCoordAction();
@@ -184,6 +192,12 @@ public class CoordELEvaluator {
             appInst.setTimeUnit(TimeUnit.valueOf(eJob.getAttributeValue("freq_timeunit")));
             appInst.setActionId(actionId);
             appInst.setName(eJob.getAttributeValue("name"));
            appInst.setPullDependencies(pullDependencies);
            appInst.setPushDependencies(pushDependencies);
            if (CoordUtils.isInputLogicSpecified(eJob)) {
                e.setVariable(".actionInputLogic",
                        XmlUtils.prettyPrint(eJob.getChild(CoordInputLogicEvaluator.INPUT_LOGIC, eJob.getNamespace())).toString());
            }
         }
         String strActualTime = eJob.getAttributeValue("action-actual-time");
         if (strActualTime != null) {
@@ -200,11 +214,14 @@ public class CoordELEvaluator {
                 }
                 else {
                 }
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, data.getNamespace()) != null) {
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, data.getNamespace()) != null) {
                     e.setVariable(".datain." + data.getAttributeValue("name") + ".unresolved", "true"); // TODO:
                     // check
                     // null
                 }
                Element doneFlagElement = data.getChild("done-flag", data.getNamespace());
                String doneFlag = CoordUtils.getDoneFlag(doneFlagElement);
                e.setVariable(".datain." + data.getAttributeValue("name") + ".doneFlag", doneFlag);
             }
         }
         events = eJob.getChild("output-events", eJob.getNamespace());
@@ -217,7 +234,7 @@ public class CoordELEvaluator {
                 }
                 else {
                 }// TODO
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INST_TAG, data.getNamespace()) != null) {
                if (data.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, data.getNamespace()) != null) {
                     e.setVariable(".dataout." + data.getAttributeValue("name") + ".unresolved", "true"); // TODO:
                     // check
                     // null
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
index 5d238663a..ffa0943d2 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordELFunctions.java
@@ -19,11 +19,13 @@
 package org.apache.oozie.coord;
 
 import com.google.common.collect.Lists;

 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorUtil;
 import org.apache.oozie.dependency.URIHandler;
 import org.apache.oozie.dependency.URIHandler.Context;
 import org.apache.oozie.service.Services;
@@ -32,6 +34,7 @@ import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.ELEvaluator;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
import org.jdom.JDOMException;
 
 import java.net.URI;
 import java.util.ArrayList;
@@ -61,7 +64,6 @@ public class CoordELFunctions {
     public static final long DAY_MSEC = 24 * HOUR_MSEC;
     public static final long MONTH_MSEC = 30 * DAY_MSEC;
     public static final long YEAR_MSEC = 365 * DAY_MSEC;

     /**
      * Used in defining the frequency in 'day' unit. <p> domain: <code> val &gt; 0</code> and should be integer.
      *
@@ -348,7 +350,7 @@ public class CoordELFunctions {
                             resolvedInstances.append(DateUtils.formatDateOozieTZ(nominalInstanceCal));
                             resolvedURIPaths.append(uriPath);
                             retVal = resolvedInstances.toString();
                            eval.setVariable("resolved_path", resolvedURIPaths.toString());
                            eval.setVariable(CoordELConstants.RESOLVED_PATH, resolvedURIPaths.toString());
                             break;
                         }
                         else if (available >= startOffset) {
@@ -356,6 +358,7 @@ public class CoordELFunctions {
                             resolvedInstances.append(DateUtils.formatDateOozieTZ(nominalInstanceCal)).append(
                                     INSTANCE_SEPARATOR);
                             resolvedURIPaths.append(uriPath).append(INSTANCE_SEPARATOR);

                         }
                         available++;
                     }
@@ -366,6 +369,10 @@ public class CoordELFunctions {
                     checkedInstance++;
                     // DateUtils.moveToEnd(nominalInstanceCal, getDSEndOfFlag());
                 }
                if (!StringUtils.isEmpty(resolvedURIPaths.toString()) && eval.getVariable(CoordELConstants.RESOLVED_PATH) == null) {
                    eval.setVariable(CoordELConstants.RESOLVED_PATH, resolvedURIPaths.toString());
                }

             }
             finally {
                 if (uriContext != null) {
@@ -375,7 +382,7 @@ public class CoordELFunctions {
             if (!resolved) {
                 // return unchanged future function with variable 'is_resolved'
                 // to 'false'
                eval.setVariable("is_resolved", Boolean.FALSE);
                eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.FALSE);
                 if (startOffset == endOffset) {
                     retVal = "${coord:future(" + startOffset + ", " + instance + ")}";
                 }
@@ -384,11 +391,11 @@ public class CoordELFunctions {
                 }
             }
             else {
                eval.setVariable("is_resolved", Boolean.TRUE);
                eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.TRUE);
             }
         }
         else {// No feasible nominal time
            eval.setVariable("is_resolved", Boolean.TRUE);
            eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.TRUE);
             retVal = "";
         }
         return retVal;
@@ -495,8 +502,24 @@ public class CoordELFunctions {
     public static String ph3_coord_dataIn(String dataInName) {
         String uris = "";
         ELEvaluator eval = ELEvaluator.getCurrent();
        if (eval.getVariable(".datain." + dataInName) == null
                && !StringUtils.isEmpty(eval.getVariable(".actionInputLogic").toString())) {
            try {
                return new CoordInputLogicEvaluatorUtil().getInputDependencies(dataInName,
                        (SyncCoordAction) eval.getVariable(COORD_ACTION));
            }
            catch (JDOMException e) {
                XLog.getLog(CoordELFunctions.class).error(e);
                throw new RuntimeException(e.getMessage());
            }
        }

         uris = (String) eval.getVariable(".datain." + dataInName);
        Boolean unresolved = (Boolean) eval.getVariable(".datain." + dataInName + ".unresolved");
        Object unResolvedObj = eval.getVariable(".datain." + dataInName + ".unresolved");
        if (unResolvedObj == null) {
            return uris;
        }
        Boolean unresolved = Boolean.parseBoolean(unResolvedObj.toString());
         if (unresolved != null && unresolved.booleanValue() == true) {
             return "${coord:dataIn('" + dataInName + "')}";
         }
@@ -835,7 +858,7 @@ public class CoordELFunctions {
     public static String ph1_coord_dataIn_echo(String n) {
         ELEvaluator eval = ELEvaluator.getCurrent();
         String val = (String) eval.getVariable("oozie.dataname." + n);
        if (val == null || val.equals("data-in") == false) {
        if ((val == null || val.equals("data-in") == false)) {
             XLog.getLog(CoordELFunctions.class).error("data_in_name " + n + " is not valid");
             throw new RuntimeException("data_in_name " + n + " is not valid");
         }
@@ -1112,7 +1135,8 @@ public class CoordELFunctions {
                             resolvedInstances.append(DateUtils.formatDateOozieTZ(nominalInstanceCal));
                             resolvedURIPaths.append(uriPath);
                             retVal = resolvedInstances.toString();
                            eval.setVariable("resolved_path", resolvedURIPaths.toString());
                            eval.setVariable(CoordELConstants.RESOLVED_PATH, resolvedURIPaths.toString());

                             break;
                         }
                         else if (available <= endOffset) {
@@ -1130,6 +1154,9 @@ public class CoordELFunctions {
                     nominalInstanceCal.add(dsTimeUnit.getCalendarUnit(), instCount[0] * datasetFrequency);
                     // DateUtils.moveToEnd(nominalInstanceCal, getDSEndOfFlag());
                 }
                if (!StringUtils.isEmpty(resolvedURIPaths.toString()) && eval.getVariable(CoordELConstants.RESOLVED_PATH) == null) {
                    eval.setVariable(CoordELConstants.RESOLVED_PATH, resolvedURIPaths.toString());
                }
             }
             finally {
                 if (uriContext != null) {
@@ -1139,7 +1166,7 @@ public class CoordELFunctions {
             if (!resolved) {
                 // return unchanged latest function with variable 'is_resolved'
                 // to 'false'
                eval.setVariable("is_resolved", Boolean.FALSE);
                eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.FALSE);
                 if (startOffset == endOffset) {
                     retVal = "${coord:latest(" + startOffset + ")}";
                 }
@@ -1148,11 +1175,11 @@ public class CoordELFunctions {
                 }
             }
             else {
                eval.setVariable("is_resolved", Boolean.TRUE);
                eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.TRUE);
             }
         }
         else {// No feasible nominal time
            eval.setVariable("is_resolved", Boolean.FALSE);
            eval.setVariable(CoordELConstants.IS_RESOLVED, Boolean.FALSE);
         }
         return retVal;
     }
diff --git a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
index 94c697406..82f9bede0 100644
-- a/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
++ b/core/src/main/java/org/apache/oozie/coord/CoordUtils.java
@@ -38,6 +38,8 @@ import org.apache.oozie.XException;
 import org.apache.oozie.client.OozieClient;
 import org.apache.oozie.client.rest.RestConstants;
 import org.apache.oozie.command.CommandException;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluator;
import org.apache.oozie.coord.input.logic.InputLogicParser;
 import org.apache.oozie.executor.jpa.CoordActionGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordJobGetActionForNominalTimeJPAExecutor;
 import org.apache.oozie.executor.jpa.JPAExecutorException;
@@ -51,7 +53,9 @@ import org.apache.oozie.util.DateUtils;
 import org.apache.oozie.util.Pair;
 import org.apache.oozie.util.ParamChecker;
 import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;
 import org.jdom.Element;
import org.jdom.JDOMException;
 
 import com.google.common.annotations.VisibleForTesting;
 
@@ -414,4 +418,22 @@ public class CoordUtils {
         }
         return params;
     }

    public static boolean isInputLogicSpecified(String actionXml) throws JDOMException {
        return isInputLogicSpecified(XmlUtils.parseXml(actionXml));
    }

    public static boolean isInputLogicSpecified(Element eAction) throws JDOMException {
        return eAction.getChild(CoordInputLogicEvaluator.INPUT_LOGIC, eAction.getNamespace()) != null;
    }

    public static String getInputLogic(String actionXml) throws JDOMException {
        return getInputLogic(XmlUtils.parseXml(actionXml));
    }

    public static String getInputLogic(Element actionXml) throws JDOMException {
        return new InputLogicParser().parse(actionXml.getChild(CoordInputLogicEvaluator.INPUT_LOGIC,
                actionXml.getNamespace()));
    }

 }
diff --git a/core/src/main/java/org/apache/oozie/coord/SyncCoordAction.java b/core/src/main/java/org/apache/oozie/coord/SyncCoordAction.java
index 44258eb5b..5f6d7a843 100644
-- a/core/src/main/java/org/apache/oozie/coord/SyncCoordAction.java
++ b/core/src/main/java/org/apache/oozie/coord/SyncCoordAction.java
@@ -20,6 +20,7 @@ package org.apache.oozie.coord;
 
 import java.util.Date;
 import java.util.TimeZone;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
 
 /**
  * This class represents a Coordinator action.
@@ -34,6 +35,10 @@ public class SyncCoordAction {
     private TimeUnit timeUnit;
     private TimeUnit endOfDuration; // End of Month or End of Days
 
    private CoordInputDependency pullDependencies;
    private CoordInputDependency pushDependencies;


     public String getActionId() {
         return this.actionId;
     }
@@ -110,4 +115,21 @@ public class SyncCoordAction {
         this.endOfDuration = endOfDuration;
     }
 
    public CoordInputDependency getPullDependencies() {
        return pullDependencies;
    }

    public void setPullDependencies(CoordInputDependency pullDependencies) {
        this.pullDependencies = pullDependencies;
    }

    public CoordInputDependency getPushDependencies() {
        return pushDependencies;
    }

    public void setPushDependencies(CoordInputDependency pushDependencies) {
        this.pushDependencies = pushDependencies;
    }


 }
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/AbstractCoordInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/AbstractCoordInputDependency.java
new file mode 100644
index 000000000..0da60ec70
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/AbstractCoordInputDependency.java
@@ -0,0 +1,315 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Writable;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordCommandUtils;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorUtil;
import org.apache.oozie.dependency.ActionDependency;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.WritableUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public abstract class AbstractCoordInputDependency implements Writable, CoordInputDependency {
    protected boolean isDependencyMet = false;
    /*
     * Transient variables only used for processing, not stored in DB.
     */
    protected transient Map<String, List<String>> missingDependenciesSet = new HashMap<String, List<String>>();
    protected transient Map<String, List<String>> availableDependenciesSet = new HashMap<String, List<String>>();
    protected Map<String, List<CoordInputInstance>> dependencyMap = new HashMap<String, List<CoordInputInstance>>();

    public AbstractCoordInputDependency() {
    }


    public AbstractCoordInputDependency(Map<String, List<CoordInputInstance>> dependencyMap) {
        this.dependencyMap = dependencyMap;
        generateDependencies();
    }

    public void addInputInstanceList(String inputEventName, List<CoordInputInstance> inputInstanceList) {
        dependencyMap.put(inputEventName, inputInstanceList);
    }

    public Map<String, List<CoordInputInstance>> getDependencyMap() {
        return dependencyMap;
    }

    public void setDependencyMap(Map<String, List<CoordInputInstance>> dependencyMap) {
        this.dependencyMap = dependencyMap;
    }

    public void addToAvailableDependencies(String dataSet, CoordInputInstance coordInputInstance) {
        coordInputInstance.setAvailability(true);
        List<String> availableSet = availableDependenciesSet.get(dataSet);
        if (availableSet == null) {
            availableSet = new ArrayList<String>();
            availableDependenciesSet.put(dataSet, availableSet);
        }
        availableSet.add(coordInputInstance.getInputDataInstance());
        removeFromMissingDependencies(dataSet, coordInputInstance);
    }

    public void removeFromMissingDependencies(String dataSet, CoordInputInstance coordInputInstance) {
        coordInputInstance.setAvailability(true);
        List<String> missingSet = missingDependenciesSet.get(dataSet);
        if (missingSet != null) {
            missingSet.remove(coordInputInstance.getInputDataInstance());
            if (missingSet.isEmpty()) {
                missingDependenciesSet.remove(dataSet);
            }
        }

    }

    public void addToMissingDependencies(String dataSet, CoordInputInstance coordInputInstance) {
        List<String> availableSet = missingDependenciesSet.get(dataSet);
        if (availableSet == null) {
            availableSet = new ArrayList<String>();
        }
        availableSet.add(coordInputInstance.getInputDataInstance());
        missingDependenciesSet.put(dataSet, availableSet);

    }

    protected void generateDependencies() {
        try {
            missingDependenciesSet = new HashMap<String, List<String>>();
            availableDependenciesSet = new HashMap<String, List<String>>();

            Set<String> keySets = dependencyMap.keySet();
            for (String key : keySets) {
                for (CoordInputInstance coordInputInstance : dependencyMap.get(key))
                    if (coordInputInstance.isAvailable()) {
                        addToAvailableDependencies(key, coordInputInstance);
                    }
                    else {
                        addToMissingDependencies(key, coordInputInstance);
                    }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<String> getAvailableDependencies(String dataSet) {
        if (availableDependenciesSet.get(dataSet) != null) {
            return availableDependenciesSet.get(dataSet);
        }
        else {
            return new ArrayList<String>();
        }

    }

    public String getMissingDependencies(String dataSet) {
        StringBuilder sb = new StringBuilder();
        for (String dependencies : missingDependenciesSet.get(dataSet)) {
            sb.append(dependencies).append("#");
        }
        return sb.toString();
    }

    public void addToAvailableDependencies(String dataSet, String availableSet) {
        List<CoordInputInstance> list = dependencyMap.get(dataSet);
        if (list == null) {
            list = new ArrayList<CoordInputInstance>();
            dependencyMap.put(dataSet, list);
        }

        for (String available : availableSet.split(CoordELFunctions.INSTANCE_SEPARATOR)) {
            CoordInputInstance coordInstance = new CoordInputInstance(available, true);
            list.add(coordInstance);
            addToAvailableDependencies(dataSet, coordInstance);
        }

    }

    public String getMissingDependencies() {
        StringBuilder sb = new StringBuilder();
        if (missingDependenciesSet != null) {
            for (List<String> dependenciesList : missingDependenciesSet.values()) {
                for (String dependencies : dependenciesList) {
                    sb.append(dependencies).append("#");
                }
            }
        }
        return sb.toString();
    }

    public List<String> getMissingDependenciesAsList() {
        List<String> missingDependencies = new ArrayList<String>();
        for (List<String> dependenciesList : missingDependenciesSet.values()) {
            missingDependencies.addAll(dependenciesList);
        }
        return missingDependencies;
    }

    public List<String> getAvailableDependenciesAsList() {
        List<String> availableDependencies = new ArrayList<String>();
        for (List<String> dependenciesList : availableDependenciesSet.values()) {
            availableDependencies.addAll(dependenciesList);

        }
        return availableDependencies;
    }

    public String serialize() throws IOException {
        return CoordInputDependencyFactory.getMagicNumber()
                + new String(WritableUtils.toByteArray(this), CoordInputDependencyFactory.CHAR_ENCODING);

    }

    public String getListAsString(List<String> dataSets) {
        StringBuilder sb = new StringBuilder();
        for (String dependencies : dataSets) {
            sb.append(dependencies).append("#");
        }

        return sb.toString();
    }

    public void setDependencyMet(boolean isDependencyMeet) {
        this.isDependencyMet = isDependencyMeet;
    }

    public boolean isDependencyMet() {
        return missingDependenciesSet.isEmpty() || isDependencyMet;
    }

    public boolean isUnResolvedDependencyMet() {
        return false;
    }


    @Override
    public void addToAvailableDependencies(Collection<String> availableList) {
        for (Entry<String, List<CoordInputInstance>> dependenciesList : dependencyMap.entrySet()) {
            for (CoordInputInstance coordInputInstance : dependenciesList.getValue()) {
                if (availableList.contains(coordInputInstance.getInputDataInstance()))
                    addToAvailableDependencies(dependenciesList.getKey(), coordInputInstance);
            }
        }
    }

    @Override
    public ActionDependency checkPushMissingDependencies(CoordinatorActionBean coordAction,
            boolean registerForNotification) throws CommandException, IOException,
            JDOMException {
        boolean status = new CoordInputLogicEvaluatorUtil(coordAction).checkPushDependencies();
        if (status) {
            coordAction.getPushInputDependencies().setDependencyMet(true);
        }
        return new ActionDependency(coordAction.getPushInputDependencies().getMissingDependenciesAsList(), coordAction
                .getPushInputDependencies().getAvailableDependenciesAsList());

    }

    public boolean checkPullMissingDependencies(CoordinatorActionBean coordAction,
            StringBuilder existList, StringBuilder nonExistList) throws IOException, JDOMException {
        boolean status = new CoordInputLogicEvaluatorUtil(coordAction).checkPullMissingDependencies();
        if (status) {
            coordAction.getPullInputDependencies().setDependencyMet(true);
        }
        return status;

    }

    public boolean isChangeInDependency(StringBuilder nonExistList, String missingDependencies,
            StringBuilder nonResolvedList, boolean status) {
        if (!StringUtils.isEmpty(missingDependencies)) {
            return !missingDependencies.equals(getMissingDependencies());
        }
        else {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean checkUnresolved(CoordinatorActionBean coordAction, Element eAction)
            throws Exception {
        String actualTimeStr = eAction.getAttributeValue("action-actual-time");
        Element inputList = eAction.getChild("input-events", eAction.getNamespace());
        Date actualTime = null;
        if (actualTimeStr == null) {
            actualTime = new Date();
        }
        else {
            actualTime = DateUtils.parseDateOozieTZ(actualTimeStr);
        }
        if (inputList == null) {
            return true;
        }
        List<Element> eDataEvents = inputList.getChildren("data-in", eAction.getNamespace());
        for (Element dEvent : eDataEvents) {
            if (dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace()) == null) {
                continue;
            }
            String unResolvedInstance = dEvent.getChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG,
                    dEvent.getNamespace()).getTextTrim();
            String name = dEvent.getAttribute("name").getValue();
            addUnResolvedList(name, unResolvedInstance);
        }
        return new CoordInputLogicEvaluatorUtil(coordAction).checkUnResolved(actualTime);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        WritableUtils.writeStringAsBytes(out,INTERNAL_VERSION_ID);
        out.writeBoolean(isDependencyMet);
        WritableUtils.writeMapWithList(out, dependencyMap);

    }

    @Override
    public void readFields(DataInput in) throws IOException {
        WritableUtils.readBytesAsString(in);
        this.isDependencyMet = in.readBoolean();
        dependencyMap = WritableUtils.readMapWithList(in, CoordInputInstance.class);
        generateDependencies();
    }

    public boolean isDataSetResolved(String dataSet){
        if(getAvailableDependencies(dataSet) ==null|| getDependencyMap().get(dataSet) == null){
            return false;
        }
        return getAvailableDependencies(dataSet).size() == getDependencyMap().get(dataSet).size();
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependency.java
new file mode 100644
index 000000000..cf0edd0cb
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependency.java
@@ -0,0 +1,172 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.dependency.ActionDependency;
import org.jdom.Element;
import org.jdom.JDOMException;

public interface CoordInputDependency {

    public static final String INTERNAL_VERSION_ID = "V=1";

    /**
     * Adds the input instance list.
     *
     * @param inputEventName the input event name
     * @param inputInstanceList the input instance list
     */
    public void addInputInstanceList(String inputEventName, List<CoordInputInstance> inputInstanceList);

    /**
     * Gets the missing dependencies.
     *
     * @return the missing dependencies
     */
    public String getMissingDependencies();

    /**
     * Checks if dependencies are meet.
     *
     * @return true, if dependencies are meet
     */
    public boolean isDependencyMet();

    /**
     * Checks if is unresolved dependencies met.
     *
     * @return true, if unresolved dependencies are met
     */
    public boolean isUnResolvedDependencyMet();

    /**
     * Sets the dependency meet.
     *
     * @param isMissingDependenciesMet the new dependency met
     */
    public void setDependencyMet(boolean isMissingDependenciesMet);

    /**
     * Serialize.
     *
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String serialize() throws IOException;

    /**
     * Gets the missing dependencies as list.
     *
     * @return the missing dependencies as list
     */
    public List<String> getMissingDependenciesAsList();

    /**
     * Gets the available dependencies as list.
     *
     * @return the available dependencies as list
     */
    public List<String> getAvailableDependenciesAsList();

    /**
     * Sets the missing dependencies.
     *
     * @param missingDependencies the new missing dependencies
     */
    public void setMissingDependencies(String missingDependencies);

    /**
     * Adds the un resolved list.
     *
     * @param name the name
     * @param tmpUnresolved the tmp unresolved
     */
    public void addUnResolvedList(String name, String tmpUnresolved);

    /**
     * Gets the available dependencies.
     *
     * @param dataSet the data set
     * @return the available dependencies
     */
    public List<String> getAvailableDependencies(String dataSet);

    /**
     * Adds the to available dependencies.
     *
     * @param availDepList the avail dep list
     */
    public void addToAvailableDependencies(Collection<String> availDepList);

    /**
     * Check push missing dependencies.
     *
     * @param coordAction the coord action
     * @param registerForNotification the register for notification
     * @return the action dependency
     * @throws CommandException the command exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws JDOMException the JDOM exception
     */
    public ActionDependency checkPushMissingDependencies(CoordinatorActionBean coordAction,
            boolean registerForNotification) throws CommandException, IOException, JDOMException;

    /**
     * Check pull missing dependencies.
     *
     * @param coordAction the coord action
     * @param existList the exist list
     * @param nonExistList the non exist list
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws JDOMException the JDOM exception
     */
    public boolean checkPullMissingDependencies(CoordinatorActionBean coordAction, StringBuilder existList,
            StringBuilder nonExistList) throws IOException, JDOMException;

    /**
     * Checks if is change in dependency.
     *
     * @param nonExistList the non exist list
     * @param missingDependencies the missing dependencies
     * @param nonResolvedList the non resolved list
     * @param status the status
     * @return true, if is change in dependency
     */
    public boolean isChangeInDependency(StringBuilder nonExistList, String missingDependencies,
            StringBuilder nonResolvedList, boolean status);

    /**
     * Check unresolved.
     *
     * @param coordAction the coord action
     * @param eAction
     * @return true, if successful
     * @throws Exception the exception
     */
    public boolean checkUnresolved(CoordinatorActionBean coordAction, Element eAction)
            throws Exception;

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
new file mode 100644
index 000000000..ad50890c0
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputDependencyFactory.java
@@ -0,0 +1,170 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.apache.oozie.StringBlob;
import org.apache.oozie.util.WritableUtils;
import org.apache.oozie.util.XLog;

public class CoordInputDependencyFactory {

    // We need to choose magic number which is not allowed for file/dir.
    // Magic number is ::$
    private static final byte[] MAGIC_NUMBER = new byte[] { 58, 58, 36 };
    public static final String CHAR_ENCODING = "ISO-8859-1";

    public static XLog LOG = XLog.getLog(CoordInputDependencyFactory.class);

    /**
     * Create the pull dependencies.
     *
     * @param isInputLogicSpecified to check if input logic is enable
     * @return the pull dependencies
     */
    public static CoordInputDependency createPullInputDependencies(boolean isInputLogicSpecified) {
        if (!isInputLogicSpecified) {
            return new CoordOldInputDependency();
        }
        else {
            return new CoordPullInputDependency();
        }
    }

    /**
     * Create the push dependencies.
     *
     * @param isInputLogicSpecified to check if input logic is enable
     * @return the push dependencies
     */
    public static CoordInputDependency createPushInputDependencies(boolean isInputLogicSpecified) {
        if (!isInputLogicSpecified) {
            return new CoordOldInputDependency();
        }
        else {
            return new CoordPushInputDependency();
        }
    }

    /**
     * Gets the pull input dependencies.
     *
     * @param missingDependencies the missing dependencies
     * @return the pull input dependencies
     */
    public static CoordInputDependency getPullInputDependencies(StringBlob missingDependencies) {
        if (missingDependencies == null) {
            return new CoordPullInputDependency();
        }
        return getPullInputDependencies(missingDependencies.getString());
    }

    public static CoordInputDependency getPullInputDependencies(String dependencies) {

        if (StringUtils.isEmpty(dependencies)) {
            return new CoordPullInputDependency();
        }

        if (!hasInputLogic(dependencies)) {
            return new CoordOldInputDependency(dependencies);
        }
        else
            try {
                return WritableUtils.fromByteArray(getDependenciesWithoutMagicNumber(dependencies).getBytes(CHAR_ENCODING),
                        CoordPullInputDependency.class);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Gets the push input dependencies.
     *
     * @param pushMissingDependencies the push missing dependencies
     * @return the push input dependencies
     */
    public static CoordInputDependency getPushInputDependencies(StringBlob pushMissingDependencies) {

        if (pushMissingDependencies == null) {
            return new CoordPushInputDependency();
        }
        return getPushInputDependencies(pushMissingDependencies.getString());

    }

    public static CoordInputDependency getPushInputDependencies(String dependencies) {


        if (StringUtils.isEmpty(dependencies)) {
            return new CoordPushInputDependency();
        }
        if (!hasInputLogic(dependencies)) {
            return new CoordOldInputDependency(dependencies);
        }

        else {
            try {
                return WritableUtils.fromByteArray(getDependenciesWithoutMagicNumber(dependencies).getBytes(CHAR_ENCODING),
                        CoordPushInputDependency.class);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * Checks if input logic is enable.
     *
     * @param dependencies the dependencies
     * @return true, if is input logic enable
     */
    private static boolean hasInputLogic(String dependencies) {
        return dependencies.startsWith(getMagicNumber());
    }

    /**
     * Gets the magic number.
     *
     * @return the magic number
     */
    public static String getMagicNumber() {
        try {
            return new String(MAGIC_NUMBER, CHAR_ENCODING);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Gets the dependencies without magic number.
     *
     * @param dependencies the dependencies
     * @return the dependencies without magic number
     */
    public static String getDependenciesWithoutMagicNumber(String dependencies) {
        return dependencies.substring(getMagicNumber().length());
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputInstance.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputInstance.java
new file mode 100644
index 000000000..945fe4445
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordInputInstance.java
@@ -0,0 +1,83 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.oozie.util.WritableUtils;

public class CoordInputInstance implements Writable {

    private String inputDataInstance = "";
    private boolean availability = false;

    public CoordInputInstance() {

    }

    public CoordInputInstance(String inputDataInstance, boolean availability) {
        this.inputDataInstance = inputDataInstance;
        this.availability = availability;

    }

    /**
     * Gets the input data instance.
     *
     * @return the input data instance
     */
    public String getInputDataInstance() {
        return inputDataInstance;
    }

    /**
     * Checks if is available.
     *
     * @return true, if is available
     */
    public boolean isAvailable() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    @Override
    public String toString() {
        return getInputDataInstance() + " : " + isAvailable();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        WritableUtils.writeStr(out, inputDataInstance);
        out.writeBoolean(availability);

    }

    @Override
    public void readFields(DataInput in) throws IOException {
        inputDataInstance = WritableUtils.readStr(in);
        availability = in.readBoolean();
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
new file mode 100644
index 000000000..9fc348ff5
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordOldInputDependency.java
@@ -0,0 +1,309 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.AccessControlException;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordCommandUtils;
import org.apache.oozie.coord.CoordELConstants;
import org.apache.oozie.coord.CoordELEvaluator;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.dependency.ActionDependency;
import org.apache.oozie.dependency.DependencyChecker;
import org.apache.oozie.dependency.URIHandlerException;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.ELEvaluator;
import org.apache.oozie.util.ParamChecker;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Old approach where dependencies are stored as String.
 *
 */
public class CoordOldInputDependency implements CoordInputDependency {

    private XLog log = XLog.getLog(getClass());

    protected transient String missingDependencies = "";

    public CoordOldInputDependency(String missingDependencies) {
        this.missingDependencies = missingDependencies;
    }

    public CoordOldInputDependency() {
    }

    @Override
    public void addInputInstanceList(String inputEventName, List<CoordInputInstance> inputInstanceList) {
        appendToDependencies(inputInstanceList);
    }

    @Override
    public String getMissingDependencies() {
        return missingDependencies;
    }

    @Override
    public boolean isDependencyMet() {
        return StringUtils.isEmpty(missingDependencies);
    }

    @Override
    public boolean isUnResolvedDependencyMet() {
        return false;
    }

    @Override
    public void setDependencyMet(boolean isDependencyMeet) {
        if (isDependencyMeet) {
            missingDependencies = "";
        }

    }

    @Override
    public String serialize() throws IOException {
        return missingDependencies;
    }

    @Override
    public List<String> getMissingDependenciesAsList() {
        return Arrays.asList(DependencyChecker.dependenciesAsArray(missingDependencies));
    }

    @Override
    public List<String> getAvailableDependenciesAsList() {
        return new ArrayList<String>();
    }

    @Override
    public void setMissingDependencies(String missingDependencies) {
        this.missingDependencies = missingDependencies;

    }

    public void appendToDependencies(List<CoordInputInstance> inputInstanceList) {
        StringBuilder sb = new StringBuilder(missingDependencies);
        boolean isFirst = true;
        for (CoordInputInstance coordInputInstance : inputInstanceList) {
            if (isFirst) {
                if (!StringUtils.isEmpty(sb.toString())) {
                    sb.append(CoordELFunctions.INSTANCE_SEPARATOR);
                }
            }
            else {
                sb.append(CoordELFunctions.INSTANCE_SEPARATOR);

            }
            sb.append(coordInputInstance.getInputDataInstance());
            isFirst = false;
        }
        missingDependencies = sb.toString();
    }

    @Override
    public void addUnResolvedList(String name, String unresolvedDependencies) {
        StringBuilder sb = new StringBuilder(missingDependencies);
        sb.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR).append(unresolvedDependencies);
        missingDependencies = sb.toString();
    }

    @Override
    public List<String> getAvailableDependencies(String dataSet) {
        return null;
    }

    @Override
    public void addToAvailableDependencies(Collection<String> availableList) {

        if (StringUtils.isEmpty(missingDependencies)) {
            return;
        }
        List<String> missingDependenciesList = new ArrayList<String>(Arrays.asList((DependencyChecker
                .dependenciesAsArray(missingDependencies))));
        missingDependenciesList.removeAll(availableList);
        missingDependencies = DependencyChecker.dependenciesAsString(missingDependenciesList);

    }

    @Override
    public boolean checkPullMissingDependencies(CoordinatorActionBean coordAction, StringBuilder existList,
            StringBuilder nonExistList) throws IOException, JDOMException {
        Configuration actionConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
        Element eAction = XmlUtils.parseXml(coordAction.getActionXml());

        Element inputList = eAction.getChild("input-events", eAction.getNamespace());
        if (inputList != null) {
            if (nonExistList.length() > 0) {
                checkListOfPaths(coordAction, existList, nonExistList, actionConf);
            }
            return nonExistList.length() == 0;
        }
        return true;
    }

    public ActionDependency checkPushMissingDependencies(CoordinatorActionBean coordAction,
            boolean registerForNotification) throws CommandException, IOException {
        return DependencyChecker.checkForAvailability(getMissingDependenciesAsList(), new XConfiguration(
                new StringReader(coordAction.getRunConf())), !registerForNotification);
    }

    private boolean checkListOfPaths(CoordinatorActionBean coordAction, StringBuilder existList,
            StringBuilder nonExistList, Configuration conf) throws IOException {

        String[] uriList = nonExistList.toString().split(CoordELFunctions.INSTANCE_SEPARATOR);
        if (uriList[0] != null) {
            log.info("[" + coordAction.getId() + "]::ActionInputCheck:: In checkListOfPaths: " + uriList[0]
                    + " is Missing.");
        }

        nonExistList.delete(0, nonExistList.length());
        boolean allExists = true;
        String existSeparator = "", nonExistSeparator = "";
        String user = ParamChecker.notEmpty(conf.get(OozieClient.USER_NAME), OozieClient.USER_NAME);
        for (int i = 0; i < uriList.length; i++) {
            if (allExists) {
                allExists = pathExists(coordAction, uriList[i], conf, user);
                log.info("[" + coordAction.getId() + "]::ActionInputCheck:: File:" + uriList[i] + ", Exists? :"
                        + allExists);
            }
            if (allExists) {
                existList.append(existSeparator).append(uriList[i]);
                existSeparator = CoordELFunctions.INSTANCE_SEPARATOR;
            }
            else {
                nonExistList.append(nonExistSeparator).append(uriList[i]);
                nonExistSeparator = CoordELFunctions.INSTANCE_SEPARATOR;
            }
        }
        return allExists;
    }

    public boolean pathExists(CoordinatorActionBean coordAction, String sPath, Configuration actionConf, String user)
            throws IOException {
        log.debug("checking for the file " + sPath);
        try {
            return CoordCommandUtils.pathExists(sPath, actionConf, user);
        }
        catch (URIHandlerException e) {
            if (coordAction != null) {
                coordAction.setErrorCode(e.getErrorCode().toString());
                coordAction.setErrorMessage(e.getMessage());
            }
            if (e.getCause() != null && e.getCause() instanceof AccessControlException) {
                throw (AccessControlException) e.getCause();
            }
            else {
                log.error(e);
                throw new IOException(e);
            }
        }
        catch (URISyntaxException e) {
            if (coordAction != null) {
                coordAction.setErrorCode(ErrorCode.E0906.toString());
                coordAction.setErrorMessage(e.getMessage());
            }
            log.error(e);
            throw new IOException(e);
        }
    }

    public boolean isChangeInDependency(StringBuilder nonExistList, String missingDependencies,
            StringBuilder nonResolvedList, boolean status) {
        if ((!nonExistList.toString().equals(missingDependencies) || missingDependencies.isEmpty())) {
            setMissingDependencies(nonExistList.toString());
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean checkUnresolved(CoordinatorActionBean coordAction, Element eAction)
            throws Exception {
        Date nominalTime = DateUtils.parseDateOozieTZ(eAction.getAttributeValue("action-nominal-time"));
        String actualTimeStr = eAction.getAttributeValue("action-actual-time");
        Element inputList = eAction.getChild("input-events", eAction.getNamespace());

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
                }
                if (resolvedTmp.length() > 0) {
                    resolvedTmp.append(CoordELFunctions.INSTANCE_SEPARATOR);
                }
                resolvedTmp.append((String) eval.getVariable(CoordELConstants.RESOLVED_PATH));
            }
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
            dEvent.removeChild(CoordCommandUtils.UNRESOLVED_INSTANCES_TAG, dEvent.getNamespace());
        }

        return true;
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPullInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPullInputDependency.java
new file mode 100644
index 000000000..f20dcae0f
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPullInputDependency.java
@@ -0,0 +1,151 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.oozie.command.coord.CoordCommandUtils;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.util.WritableUtils;

public class CoordPullInputDependency extends AbstractCoordInputDependency {
    private Map<String, CoordUnResolvedInputDependency> unResolvedList = new HashMap<String, CoordUnResolvedInputDependency>();

    public CoordPullInputDependency() {
        super();

    }

    public void addResolvedList(String dataSet, String list) {
        unResolvedList.get(dataSet).addResolvedList(Arrays.asList(list.split(",")));
    }

    public CoordUnResolvedInputDependency getUnResolvedDependency(String dataSet) {
        return unResolvedList.get(dataSet);
    }

    public boolean isUnResolvedDependencyMet() {
        for (CoordUnResolvedInputDependency coordUnResolvedDependency : unResolvedList.values()) {
            if (!coordUnResolvedDependency.isResolved()) {
                return false;
            }
        }
        return true;
    }

    public void addUnResolvedList(String dataSet, String dependency) {
        unResolvedList.put(dataSet, new CoordUnResolvedInputDependency(Arrays.asList(dependency.split("#"))));
    }

    public String getMissingDependencies() {
        StringBuffer bf = new StringBuffer(super.getMissingDependencies());
        String unresolvedMissingDependencies = getUnresolvedMissingDependencies();
        if (!StringUtils.isEmpty(unresolvedMissingDependencies)) {
            bf.append(CoordCommandUtils.RESOLVED_UNRESOLVED_SEPARATOR);
            bf.append(unresolvedMissingDependencies);
        }
        return bf.toString();
    }

    public String getUnresolvedMissingDependencies() {
        StringBuffer bf = new StringBuffer();
        if (unResolvedList != null) {
            for (CoordUnResolvedInputDependency coordUnResolvedDependency : unResolvedList.values()) {
                if (!coordUnResolvedDependency.isResolved()) {
                    String unresolvedList = coordUnResolvedDependency.getUnResolvedList();
                    if (bf.length() > 0 && !unresolvedList.isEmpty()) {
                        bf.append(CoordELFunctions.INSTANCE_SEPARATOR);
                    }
                    bf.append(unresolvedList);
                }
            }
        }
        return bf.toString();
    }

    protected void generateDependencies() {
        super.generateDependencies();
    }

    private void writeObject(ObjectOutputStream os) throws IOException, ClassNotFoundException {
        os.writeObject(unResolvedList);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        unResolvedList = (Map<String, CoordUnResolvedInputDependency>) in.readObject();
        generateDependencies();
    }

    public boolean isDependencyMet() {
        return isResolvedDependencyMeet() && isUnResolvedDependencyMet();

    }

    public boolean isResolvedDependencyMeet() {
        return super.isDependencyMet();

    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        WritableUtils.writeMap(out, unResolvedList);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);
        unResolvedList = WritableUtils.readMap(in, CoordUnResolvedInputDependency.class);
    }

    @Override
    public void setMissingDependencies(String join) {
        // We don't have to set this for input logic. Dependency map will have computed missing dependencies
    }

    @Override
    public List<String> getAvailableDependencies(String dataSet) {
        List<String> availableList = new ArrayList<String>();
        availableList.addAll(super.getAvailableDependencies(dataSet));
        if (getUnResolvedDependency(dataSet) != null) {
            availableList.addAll(getUnResolvedDependency(dataSet).getResolvedList());
        }
        return availableList;
    }

    public boolean isDataSetResolved(String dataSet) {
        if(unResolvedList.containsKey(dataSet)){
            return unResolvedList.get(dataSet).isResolved();
        }
        else{
            return super.isDataSetResolved(dataSet);
        }
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPushInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPushInputDependency.java
new file mode 100644
index 000000000..e19e799c8
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordPushInputDependency.java
@@ -0,0 +1,49 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CoordPushInputDependency extends AbstractCoordInputDependency {

    public CoordPushInputDependency() {
        super();
    }

    @Override
    public void setMissingDependencies(String join) {
    }

    @Override
    public void addUnResolvedList(String name, String tmpUnresolved) {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordUnResolvedInputDependency.java b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordUnResolvedInputDependency.java
new file mode 100644
index 000000000..096b58882
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/dependency/CoordUnResolvedInputDependency.java
@@ -0,0 +1,92 @@
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

package org.apache.oozie.coord.input.dependency;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Writable;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.util.WritableUtils;

public class CoordUnResolvedInputDependency implements Writable {

    private boolean isResolved;
    private List<String> dependency = new ArrayList<String>();
    private List<String> resolvedList = new ArrayList<String>();

    public CoordUnResolvedInputDependency(List<String> dependency) {
        this.dependency = dependency;

    }

    public CoordUnResolvedInputDependency() {
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }

    public List<String> getDependencies() {
        return dependency;
    }

    public List<String> getResolvedList() {
        return resolvedList;
    }

    public void setResolvedList(List<String> resolvedList) {
        this.resolvedList = resolvedList;
    }

    public void addResolvedList(List<String> resolvedList) {
        this.resolvedList.addAll(resolvedList);
    }

    public String getUnResolvedList() {
        if (!isResolved) {
            return StringUtils.join(dependency, CoordELFunctions.INSTANCE_SEPARATOR);
        }
        else
            return "";
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(isResolved);
        WritableUtils.writeStringList(out, dependency);
        WritableUtils.writeStringList(out, resolvedList);
    }

    @Override
    public void readFields(DataInput in) throws IOException {

        isResolved = in.readBoolean();
        dependency = WritableUtils.readStringList(in);
        resolvedList = WritableUtils.readStringList(in);
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicBuilder.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicBuilder.java
new file mode 100644
index 000000000..2326cd7c7
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicBuilder.java
@@ -0,0 +1,167 @@
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

package org.apache.oozie.coord.input.logic;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

public class CoordInputLogicBuilder {

    StringBuffer bf = new StringBuffer();

    CoordInputLogicEvaluator coordInputlogicEvaluator;

    /** The Dependency builder. */
    public CoordInputDependencyBuilder dependencyBuilder;

    public CoordInputLogicBuilder(CoordInputLogicEvaluator coordInputlogicEvaluator) {
        this.coordInputlogicEvaluator = coordInputlogicEvaluator;
        dependencyBuilder = new CoordInputDependencyBuilder(coordInputlogicEvaluator);
    }

    /**
     * Input function of input-logic
     *
     * @param inputDataset the dataset
     * @return the string
     */
    public CoordInputLogicEvaluatorResult input(String inputDataset) {
        return coordInputlogicEvaluator.evalInput(inputDataset, -1, -1);

    }

    /**
     * Combine function of dataset
     *
     * @param combineDatasets the combine
     * @return the string
     */
    public CoordInputLogicEvaluatorResult combine(String... combineDatasets) {
        return coordInputlogicEvaluator.evalCombineInput(combineDatasets, -1, -1);
    }

    /**
     * The Class CoordInputDependencyBuilder.
     */
    public static class CoordInputDependencyBuilder {

        CoordInputLogicEvaluator coordInputLogicEvaluator;

        public CoordInputDependencyBuilder(CoordInputLogicEvaluator coordInputLogicEvaluator) {
            this.coordInputLogicEvaluator = coordInputLogicEvaluator;

        }

        private int minValue = -1;
        private String wait;
        private String inputDataset;
        private String[] combineDatasets;

        /**
         * Construct min function
         *
         * @param minValue the min value
         * @return the coord input dependency builder
         */
        public CoordInputDependencyBuilder min(int minValue) {
            this.minValue = minValue;
            return this;
        }

        /**
         * Construct  wait function
         *
         * @param wait the wait
         * @return the coord input dependency builder
         */
        public CoordInputDependencyBuilder inputWait(String wait) {
            this.wait = wait;
            return this;
        }

        /**
         * Construct wait function
         *
         * @param wait the wait
         * @return the coord input dependency builder
         */
        public CoordInputDependencyBuilder inputWait(int wait) {
            this.wait = String.valueOf(wait);
            return this;
        }

        /**
         * Construct input function
         *
         * @param dataset the input
         * @return the coord input dependency builder
         */
        public CoordInputDependencyBuilder input(String dataset) {
            this.inputDataset = dataset;
            return this;
        }

        /**
         * Construct complie function
         *
         * @param combineDatasets the combine
         * @return the coord input dependency builder
         */
        public CoordInputDependencyBuilder combine(String... combineDatasets) {
            this.combineDatasets = combineDatasets;
            return this;
        }

        /**
         * Build inputlogic expression
         *
         * @return the string
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public CoordInputLogicEvaluatorResult build() throws IOException {
            if (combineDatasets != null) {
                return coordInputLogicEvaluator.evalCombineInput(combineDatasets, minValue, getTime(wait));
            }
            else {
                return coordInputLogicEvaluator.evalInput(inputDataset, minValue, getTime(wait));
            }
        }

        /**
         * Gets the time in min.
         *
         * @param value the value
         * @return the time in min
         * @throws IOException Signals that an I/O exception has occurred.
         */
        private int getTime(String value) throws IOException {
            if (StringUtils.isEmpty(value)) {
                return -1;
            }
            if (StringUtils.isNumeric(value)) {
                return Integer.parseInt(value);
            }
            else {
                throw new IOException("Unsupported time : " + value);
            }
        }
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluator.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluator.java
new file mode 100644
index 000000000..c49557001
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluator.java
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

package org.apache.oozie.coord.input.logic;

public interface CoordInputLogicEvaluator {
    public static final String INPUT_LOGIC = "input-logic";


    /**
     * Eval input.
     *
     * @param inputDataSet the input data set
     * @param min the min
     * @param wait the wait
     * @return the coord input logic evaluator result
     */
    public CoordInputLogicEvaluatorResult evalInput(String inputDataSet, int min, int wait);

    /**
     * Eval combine input.
     *
     * @param combineDatasets the combine datasets
     * @param min the min
     * @param wait the wait
     * @return the coord input logic evaluator result
     */
    public CoordInputLogicEvaluatorResult evalCombineInput(String[] combineDatasets, int min, int wait);
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
new file mode 100644
index 000000000..f54d30543
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseOne.java
@@ -0,0 +1,324 @@
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

package org.apache.oozie.coord.input.logic;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.command.coord.CoordCommandUtils;
import org.apache.oozie.coord.input.dependency.AbstractCoordInputDependency;
import org.apache.oozie.coord.input.dependency.CoordInputDependency;
import org.apache.oozie.coord.input.dependency.CoordInputInstance;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorResult.STATUS;
import org.apache.oozie.dependency.URIHandlerException;
import org.apache.oozie.util.LogUtils;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XLog;

/**
 * PhaseOne is for all dependencies check, except unresolved. Unresolved will be checked as part of phaseTwo.
 * Phasethree is only to get dependencies from dataset, no hdfs/hcat check.
 */
public class CoordInputLogicEvaluatorPhaseOne implements CoordInputLogicEvaluator {

    protected AbstractCoordInputDependency coordInputDependency;
    protected Map<String, List<CoordInputInstance>> dependencyMap;
    protected CoordinatorActionBean coordAction = null;
    protected XLog log = XLog.getLog(getClass());

    public CoordInputLogicEvaluatorPhaseOne(CoordinatorActionBean coordAction) {
        this(coordAction, coordAction.getPullInputDependencies());
    }

    public CoordInputLogicEvaluatorPhaseOne(CoordinatorActionBean coordAction, CoordInputDependency coordInputDependency) {
        this.coordAction = coordAction;
        this.coordInputDependency = (AbstractCoordInputDependency) coordInputDependency;
        dependencyMap = ((AbstractCoordInputDependency) coordInputDependency).getDependencyMap();
        LogUtils.setLogInfo(coordAction.getId());

    }

    public CoordInputLogicEvaluatorResult evalInput(String dataSet, int min, int wait) {
        return input(coordInputDependency, dataSet, min, wait);

    }

    /**
     * Evaluate input function with min and wait
     *
     * @param coordInputDependency
     * @param dataSet
     * @param min
     * @param wait
     * @return the coord input logic evaluator result
     */
    public CoordInputLogicEvaluatorResult input(AbstractCoordInputDependency coordInputDependency, String dataSet,
            int min, int wait) {

        List<String> availableList = new ArrayList<String>();
        if (coordInputDependency.getDependencyMap().get(dataSet) == null) {
            CoordInputLogicEvaluatorResult retData = new CoordInputLogicEvaluatorResult();
            if (coordInputDependency.getAvailableDependencies(dataSet) == null
                    || coordInputDependency.getAvailableDependencies(dataSet).isEmpty()) {
                log.debug("Data set [{0}] is unresolved set, will get resolved in phasetwo", dataSet);
                retData.setStatus(CoordInputLogicEvaluatorResult.STATUS.PHASE_TWO_EVALUATION);
            }
            else {
                return getResultFromPullPush(coordAction, dataSet, min);
            }
            return retData;
        }
        boolean allFound = true;
        try {
            Configuration actionConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
            List<CoordInputInstance> firstInputSetList = coordInputDependency.getDependencyMap().get(dataSet);
            for (int i = 0; i < firstInputSetList.size(); i++) {
                CoordInputInstance coordInputInstance = firstInputSetList.get(i);
                if (!coordInputInstance.isAvailable()) {
                    if (pathExists(coordInputInstance.getInputDataInstance(), actionConf)) {
                        availableList.add(coordInputInstance.getInputDataInstance());
                        coordInputDependency.addToAvailableDependencies(dataSet, coordInputInstance);
                    }
                    else {
                        log.debug("[{0} is not found ", coordInputInstance.getInputDataInstance());
                        allFound = false;
                        // Stop looking for dependencies, if min is not specified.
                        if (min < 0) {
                            break;
                        }
                    }
                }
                else {
                    availableList.add(coordInputInstance.getInputDataInstance());
                }
            }
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(ErrorCode.E1028.format("Error executing input function " + e.getMessage()));
        }
        CoordInputLogicEvaluatorResult retData = getEvalResult(allFound, min, wait, availableList);

        log.debug("Resolved status of Data set [{0}] with min [{1}] and wait [{2}]  =  [{3}]", dataSet, min, wait,
                retData.getStatus());
        return retData;
    }

    public boolean isInputWaitElapsed(int timeInMin) {

        if (timeInMin == -1) {
            return true;
        }
        long waitingTime = (new Date().getTime() - Math.max(coordAction.getNominalTime().getTime(), coordAction
                .getCreatedTime().getTime()))
                / (60 * 1000);
        return timeInMin <= waitingTime;
    }

    public CoordInputLogicEvaluatorResult evalCombineInput(String[] inputSets, int min, int wait) {
        return combine(coordInputDependency, inputSets, min, wait);
    }

    public CoordInputLogicEvaluatorResult combine(AbstractCoordInputDependency coordInputDependency,
            String[] inputSets, int min, int wait) {

        List<String> availableList = new ArrayList<String>();

        if (coordInputDependency.getDependencyMap().get(inputSets[0]) == null) {
            return new CoordInputLogicEvaluatorResult(CoordInputLogicEvaluatorResult.STATUS.TIMED_WAITING);
        }

        try {

            Configuration jobConf = new XConfiguration(new StringReader(coordAction.getRunConf()));
            String firstInputSet = inputSets[0];
            List<CoordInputInstance> firstInputSetList = coordInputDependency.getDependencyMap().get(firstInputSet);
            for (int i = 0; i < firstInputSetList.size(); i++) {
                CoordInputInstance coordInputInstance = firstInputSetList.get(i);
                boolean found = false;
                if (!coordInputInstance.isAvailable()) {
                    if (!pathExists(coordInputInstance.getInputDataInstance(), jobConf)) {
                        log.debug(MessageFormat.format("{0} is not found. Looking from other datasets.",
                                coordInputInstance.getInputDataInstance()));
                        for (int j = 1; j < inputSets.length; j++) {
                            if (!coordInputDependency.getDependencyMap().get(inputSets[j]).get(i).isAvailable()) {
                                if (pathExists(coordInputDependency.getDependencyMap().get(inputSets[j]).get(i)
                                        .getInputDataInstance(), jobConf)) {
                                    coordInputDependency.addToAvailableDependencies(inputSets[j], coordInputDependency
                                            .getDependencyMap().get(inputSets[j]).get(i));
                                    availableList.add(coordInputDependency.getDependencyMap().get(inputSets[j]).get(i)
                                            .getInputDataInstance());
                                    log.debug(MessageFormat.format("{0} is found.",
                                            coordInputInstance.getInputDataInstance()));
                                    found = true;
                                }

                            }
                            else {
                                coordInputDependency.addToAvailableDependencies(inputSets[j], coordInputDependency
                                        .getDependencyMap().get(inputSets[j]).get(i));
                                availableList.add(coordInputDependency.getDependencyMap().get(inputSets[j]).get(i)
                                        .getInputDataInstance());
                                found = true;

                            }
                        }
                    }
                    else {
                        coordInputDependency.addToAvailableDependencies(firstInputSet, coordInputInstance);
                        availableList.add(coordInputInstance.getInputDataInstance());
                        found = true;
                    }
                }
                else {
                    availableList.add(coordInputInstance.getInputDataInstance());
                    found = true;
                }

                if (min < 0 && !found) {
                    // Stop looking for dependencies, if min is not specified.
                    break;
                }

            }
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(ErrorCode.E1028.format("Error executing combine function " + e.getMessage()));
        }
        boolean allFound = availableList.size() == coordInputDependency.getDependencyMap().get(inputSets[0]).size();
        CoordInputLogicEvaluatorResult retData = getEvalResult(allFound, min, wait, availableList);
        log.debug("Resolved status of Data set [{0}] with min [{1}] and wait [{2}]  =  [{3}]",
                Arrays.toString(inputSets), min, wait, retData.getStatus());
        return retData;

    }

    public Configuration getConf() throws IOException {
        return new XConfiguration(new StringReader(coordAction.getRunConf()));

    }

    public String getListAsString(List<String> list, String dataset) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < list.size(); i++) {
            sb.append(list.get(i - 1)).append(",");
        }
        sb.append(list.get(list.size() - 1));
        return sb.toString();
    }

    protected CoordInputLogicEvaluatorResult getEvalResult(boolean found, int min, int wait, List<String> availableList) {
        CoordInputLogicEvaluatorResult retData = new CoordInputLogicEvaluatorResult();
        if (!found && wait > 0) {
            if (!isInputWaitElapsed(wait)) {
                return new CoordInputLogicEvaluatorResult(STATUS.TIMED_WAITING);
            }
        }

        if (found || (min > 0 && availableList.size() >= min)) {
            retData.setStatus(CoordInputLogicEvaluatorResult.STATUS.TRUE);
            retData.setDataSets(getListAsString(availableList, null));
        }

        if (min == 0) {
            retData.setStatus(CoordInputLogicEvaluatorResult.STATUS.TRUE);
        }

        return retData;
    }

    protected boolean pathExists(String sPath, Configuration jobConf) throws IOException, URISyntaxException,
            URIHandlerException {
        return CoordCommandUtils.pathExists(sPath, jobConf);

    }

    public CoordInputLogicEvaluatorResult getResultFromPullPush(CoordinatorActionBean coordAction, String dataSet, int min) {
        CoordInputLogicEvaluatorResult result = new CoordInputLogicEvaluatorResult();
        CoordInputLogicEvaluatorResult pullResult = getEvalResult(
                (AbstractCoordInputDependency) coordAction.getPullInputDependencies(), dataSet, min);
        CoordInputLogicEvaluatorResult pushResult = getEvalResult(
                (AbstractCoordInputDependency) coordAction.getPushInputDependencies(), dataSet, min);
        result.appendDataSets(pullResult.getDataSets());
        result.appendDataSets(pushResult.getDataSets());

        if (pullResult.isWaiting() || pushResult.isWaiting()) {
            result.setStatus(STATUS.TIMED_WAITING);
        }

        else if (pullResult.isPhaseTwoEvaluation() || pushResult.isPhaseTwoEvaluation()) {
            result.setStatus(STATUS.PHASE_TWO_EVALUATION);
        }

        else if (pullResult.isTrue() || pushResult.isTrue()) {
            result.setStatus(STATUS.TRUE);
        }
        else {
            result.setStatus(STATUS.FALSE);
        }
        return result;

    }

    /**
     * Gets evaluator Result
     *
     * @param coordInputDependencies the coord dependencies
     * @param dataSet the data set
     * @param min the min
     * @return the coord input logic evaluator result
     */
    public CoordInputLogicEvaluatorResult getEvalResult(AbstractCoordInputDependency coordInputDependencies,
            String dataSet, int min) {
        CoordInputLogicEvaluatorResult result = new CoordInputLogicEvaluatorResult();
        if ((coordInputDependencies.getAvailableDependencies(dataSet) == null || coordInputDependencies
                .getAvailableDependencies(dataSet).isEmpty())) {
            if (min == 0) {
                result.setStatus(CoordInputLogicEvaluatorResult.STATUS.TRUE);
            }
            else {
                result.setStatus(CoordInputLogicEvaluatorResult.STATUS.FALSE);
            }
        }

        if (min > -1 && coordInputDependencies.getAvailableDependencies(dataSet).size() >= min) {
            result.setStatus(CoordInputLogicEvaluatorResult.STATUS.TRUE);
            result.appendDataSets(getListAsString(coordInputDependencies.getAvailableDependencies(dataSet), dataSet));
        }

        else if (coordInputDependencies.isDataSetResolved(dataSet)) {
            result.setStatus(CoordInputLogicEvaluatorResult.STATUS.TRUE);
            result.appendDataSets(getListAsString(coordInputDependencies.getAvailableDependencies(dataSet), dataSet));
        }
        return result;
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseThree.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseThree.java
new file mode 100644
index 000000000..31cf0817c
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseThree.java
@@ -0,0 +1,130 @@
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

package org.apache.oozie.coord.input.logic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.coord.input.dependency.AbstractCoordInputDependency;
import org.apache.oozie.coord.input.dependency.CoordInputInstance;
import org.apache.oozie.dependency.URIHandler;
import org.apache.oozie.dependency.URIHandlerException;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.URIHandlerService;
import org.apache.oozie.util.ELEvaluator;

public class CoordInputLogicEvaluatorPhaseThree extends CoordInputLogicEvaluatorPhaseOne {

    ELEvaluator eval;

    public CoordInputLogicEvaluatorPhaseThree(CoordinatorActionBean coordAction, ELEvaluator eval) {
        super(coordAction, (AbstractCoordInputDependency) coordAction.getPullInputDependencies());
        this.eval = eval;
    }

    public CoordInputLogicEvaluatorResult evalInput(String dataSet, int min, int wait) {
        return getResultFromPullPush(coordAction, dataSet, min);

    }

    public CoordInputLogicEvaluatorResult evalCombineInput(String[] inputSets, int min, int wait) {
        return combine(coordInputDependency, inputSets, min, wait);
    }

    public CoordInputLogicEvaluatorResult combine(AbstractCoordInputDependency coordInputDependency,
            String[] inputSets, int min, int wait) {

        List<String> availableList = new ArrayList<String>();

        if (coordInputDependency.getDependencyMap().get(inputSets[0]) == null) {
            return new CoordInputLogicEvaluatorResult(CoordInputLogicEvaluatorResult.STATUS.FALSE);
        }

        try {
            String firstInputSet = inputSets[0];
            List<CoordInputInstance> firstInputSetList = coordInputDependency.getDependencyMap().get(firstInputSet);
            for (int i = 0; i < firstInputSetList.size(); i++) {
                CoordInputInstance coordInputInstance = firstInputSetList.get(i);
                if (!coordInputInstance.isAvailable()) {
                    for (int j = 1; j < inputSets.length; j++) {
                        if (coordInputDependency.getDependencyMap().get(inputSets[j]).get(i).isAvailable()) {
                            availableList.add(getPathWithoutDoneFlag(
                                    coordInputDependency.getDependencyMap().get(inputSets[j]).get(i)
                                            .getInputDataInstance(), inputSets[j]));
                        }
                    }
                }

                else {
                    availableList.add(getPathWithoutDoneFlag(coordInputInstance.getInputDataInstance(), firstInputSet));
                }
            }
        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(ErrorCode.E1028.format("Error executing combine function " + e.getMessage()));
        }
        boolean allFound = availableList.size() == coordInputDependency.getDependencyMap().get(inputSets[0]).size();
        return getEvalResult(allFound, min, wait, availableList);
    }

    protected boolean pathExists(String sPath, Configuration actionConf) throws IOException, URISyntaxException,
            URIHandlerException {
        return false;
    }

    public boolean isInputWaitElapsed(int timeInMin) {
        return true;
    }

    public String getListAsString(List<String> input, String dataSet) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {

            for (int i = 1; i < input.size(); i++) {
                sb.append(getPathWithoutDoneFlag(input.get(i - 1), dataSet)).append(",");
            }
            sb.append(getPathWithoutDoneFlag(input.get(input.size() - 1), dataSet));
        }
        catch (URIHandlerException e) {
            log.error(e);
            throw new RuntimeException(ErrorCode.E1028.format("Error finding path without done flag " + e.getMessage()));
        }

        return sb.toString();
    }

    private String getPathWithoutDoneFlag(String sPath, String dataSet) throws URIHandlerException {
        if (dataSet == null) {
            return sPath;
        }
        URIHandlerService service = Services.get().get(URIHandlerService.class);
        URIHandler handler = service.getURIHandler(sPath);
        return handler.getURIWithoutDoneFlag(sPath, eval.getVariable(".datain." + dataSet + ".doneFlag").toString());
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseTwo.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseTwo.java
new file mode 100644
index 000000000..16fc40044
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseTwo.java
@@ -0,0 +1,144 @@
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

package org.apache.oozie.coord.input.logic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.coord.CoordELConstants;
import org.apache.oozie.coord.CoordELEvaluator;
import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.input.dependency.AbstractCoordInputDependency;
import org.apache.oozie.coord.input.dependency.CoordPullInputDependency;
import org.apache.oozie.coord.input.logic.CoordInputLogicEvaluatorResult.STATUS;
import org.apache.oozie.dependency.DependencyChecker;
import org.apache.oozie.util.ELEvaluator;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public class CoordInputLogicEvaluatorPhaseTwo extends CoordInputLogicEvaluatorPhaseOne {

    Date actualTime;

    public CoordInputLogicEvaluatorPhaseTwo(CoordinatorActionBean coordAction, Date actualTime) {
        super(coordAction);
        this.actualTime = actualTime;
    }

    public CoordInputLogicEvaluatorPhaseTwo(CoordinatorActionBean coordAction,
            AbstractCoordInputDependency coordInputDependency) {
        super(coordAction, coordInputDependency);
    }

    @Override
    public CoordInputLogicEvaluatorResult evalInput(String dataSet, int min, int wait) {
        try {
            CoordPullInputDependency coordPullInputDependency = (CoordPullInputDependency) coordInputDependency;
            ELEvaluator eval = CoordELEvaluator.createLazyEvaluator(actualTime, coordAction.getNominalTime(),
                    getInputSetEvent(dataSet), getConf());
            if (coordPullInputDependency.getUnResolvedDependency(dataSet) == null) {
                return super.evalInput(dataSet, min, wait);

            }
            else {
                cleanPreviousCheckData(coordPullInputDependency, dataSet);
                List<String> unresolvedList = coordPullInputDependency.getUnResolvedDependency(dataSet)
                        .getDependencies();
                for (String unresolved : unresolvedList) {
                    String resolvedPath = "";

                    CoordELFunctions.evalAndWrap(eval, unresolved);
                    boolean isResolved = (Boolean) eval.getVariable(CoordELConstants.IS_RESOLVED);

                    coordPullInputDependency.setDependencyMap(dependencyMap);
                    if (eval.getVariable(CoordELConstants.RESOLVED_PATH) != null) {
                        resolvedPath = eval.getVariable(CoordELConstants.RESOLVED_PATH).toString();
                    }
                    if (resolvedPath != null) {
                        resolvedPath = getEvalResult(isResolved, min, wait,
                                Arrays.asList(DependencyChecker.dependenciesAsArray(resolvedPath.toString())))
                                .getDataSets();

                    }

                    log.trace(MessageFormat.format("Return data is {0}", resolvedPath));
                    log.debug(MessageFormat.format("Resolved status of Data set {0} with min {1} and wait {2}  =  {3}",
                            dataSet, min, wait, !StringUtils.isEmpty(resolvedPath)));

                    if ((isInputWaitElapsed(wait) || isResolved) && !StringUtils.isEmpty(resolvedPath)) {
                        coordPullInputDependency.addResolvedList(dataSet, resolvedPath.toString());
                    }
                    else {
                        cleanPreviousCheckData(coordPullInputDependency, dataSet);
                        if (!isInputWaitElapsed(wait)) {
                            return new CoordInputLogicEvaluatorResult(
                                    CoordInputLogicEvaluatorResult.STATUS.TIMED_WAITING);
                        }
                        else {
                            return new CoordInputLogicEvaluatorResult(CoordInputLogicEvaluatorResult.STATUS.FALSE);
                        }
                    }
                }
                coordPullInputDependency.getUnResolvedDependency(dataSet).setResolved(true);
                return new CoordInputLogicEvaluatorResult(STATUS.TRUE, getListAsString(coordPullInputDependency
                        .getUnResolvedDependency(dataSet).getResolvedList(), dataSet));

            }
        }
        catch (Exception e) {
            throw new RuntimeException(" event not found" + e, e);

        }

    }

    private void cleanPreviousCheckData(CoordPullInputDependency coordPullInputDependency, String dataSet) {
        // Previous check might have resolved and added resolved list. Cleanup any resolved list stored by previous
        // check.
        if (coordPullInputDependency.getUnResolvedDependency(dataSet) != null) {
            coordPullInputDependency.getUnResolvedDependency(dataSet).setResolvedList(new ArrayList<String>());
        }

    }

    @Override
    public CoordInputLogicEvaluatorResult evalCombineInput(String[] inputSets, int min, int wait) {
        throw new RuntimeException("Combine is not supported for latest/future");

    }

    @SuppressWarnings("unchecked")
    private Element getInputSetEvent(String name) throws JDOMException {
        Element eAction = XmlUtils.parseXml(coordAction.getActionXml().toString());
        Element inputList = eAction.getChild("input-events", eAction.getNamespace());
        List<Element> eDataEvents = inputList.getChildren("data-in", eAction.getNamespace());
        for (Element dEvent : eDataEvents) {
            if (dEvent.getAttribute("name").getValue().equals(name)) {
                return dEvent;
            }
        }
        throw new RuntimeException("Event not found");
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseValidate.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseValidate.java
new file mode 100644
index 000000000..f485296e9
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorPhaseValidate.java
@@ -0,0 +1,89 @@
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

package org.apache.oozie.coord.input.logic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.coord.input.dependency.CoordInputInstance;
import org.apache.oozie.coord.input.dependency.CoordPullInputDependency;
import org.apache.oozie.coord.input.dependency.CoordPushInputDependency;

public class CoordInputLogicEvaluatorPhaseValidate implements CoordInputLogicEvaluator {

    CoordPullInputDependency coordPullInputDependency;
    CoordPushInputDependency coordPushInputDependency;

    protected Map<String, List<CoordInputInstance>> dependencyMap;
    protected CoordinatorActionBean coordAction = null;
    protected CoordinatorJob coordJob = null;

    public CoordInputLogicEvaluatorPhaseValidate(CoordinatorActionBean coordAction) {
        this.coordAction = coordAction;
        coordPullInputDependency = (CoordPullInputDependency) coordAction.getPullInputDependencies();
        coordPushInputDependency = (CoordPushInputDependency) coordAction.getPushInputDependencies();

    }

    @Override
    public CoordInputLogicEvaluatorResult evalInput(String dataSet, int min, int wait) {
        getDataSetLen(dataSet);
        return new CoordInputLogicEvaluatorResult(CoordInputLogicEvaluatorResult.STATUS.FALSE);
    }

    @Override
    public CoordInputLogicEvaluatorResult evalCombineInput(String[] inputSets, int min, int wait) {
        if (inputSets.length <= 1) {
            throw new RuntimeException("Combine should have at least two input sets. DataSets : "
                    + Arrays.toString(inputSets));
        }
        int firstInputSetLen = getDataSetLen(inputSets[0]);
        for (int i = 1; i < inputSets.length; i++) {
            if (getDataSetLen(inputSets[i]) != firstInputSetLen) {
                throw new RuntimeException("Combine should have same range. DataSets : " + Arrays.toString(inputSets));
            }
            if (coordPullInputDependency.getUnResolvedDependency(inputSets[i]) != null) {
                throw new RuntimeException("Combine is not supported for latest/future");
            }
        }
        return new CoordInputLogicEvaluatorResult(CoordInputLogicEvaluatorResult.STATUS.FALSE);
    }

    private int getDataSetLen(String dataset) {
        if (coordAction.getPullInputDependencies() != null) {
            if (coordPullInputDependency.getDependencyMap().get(dataset) != null) {
                return coordPullInputDependency.getDependencyMap().get(dataset).size();
            }

            if (coordPullInputDependency.getUnResolvedDependency(dataset) != null) {
                return 1;
            }

        }
        if (coordAction.getPushInputDependencies() != null) {
            if (coordPushInputDependency.getDependencyMap().get(dataset) != null) {
                return coordPushInputDependency.getDependencyMap().get(dataset).size();
            }
        }
        throw new RuntimeException(" Data set not found : " + dataset);
    }
}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorResult.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorResult.java
new file mode 100644
index 000000000..2f3f03436
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorResult.java
@@ -0,0 +1,104 @@
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

package org.apache.oozie.coord.input.logic;

import org.apache.commons.lang.StringUtils;
import org.apache.oozie.coord.CoordELFunctions;

public class CoordInputLogicEvaluatorResult {

    private STATUS status;
    private String dataSets;

    public static enum STATUS {
        TRUE, FALSE, PHASE_TWO_EVALUATION, TIMED_WAITING
    }

    public CoordInputLogicEvaluatorResult() {
    }

    public CoordInputLogicEvaluatorResult(STATUS status, String dataSets) {
        this.status = status;
        this.dataSets = dataSets;
    }

    public CoordInputLogicEvaluatorResult(STATUS status) {
        this.status = status;
    }

    public String getDataSets() {
        return dataSets;
    }

    public void setDataSets(String dataSets) {
        this.dataSets = dataSets;
    }

    public void appendDataSets(String inputDataSets) {
        if (StringUtils.isEmpty(inputDataSets)) {
            return;
        }
        if (StringUtils.isEmpty(this.dataSets)) {
            this.dataSets = inputDataSets;
        }
        else {
            this.dataSets = this.dataSets + CoordELFunctions.DIR_SEPARATOR + inputDataSets;
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    public boolean isTrue() {
        if (status == null) {
            return false;
        }
        switch (status) {
            case TIMED_WAITING:
            case PHASE_TWO_EVALUATION:
            case TRUE:
                return true;
            default:
                return false;
        }

    }

    public boolean isWaiting() {
        if (status == null) {
            return false;
        }
        return status.equals(STATUS.TIMED_WAITING);

    }

    public boolean isPhaseTwoEvaluation() {
        if (status == null) {
            return false;
        }
        return status.equals(STATUS.PHASE_TWO_EVALUATION);

    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
new file mode 100644
index 000000000..63c07609a
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/CoordInputLogicEvaluatorUtil.java
@@ -0,0 +1,229 @@
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

package org.apache.oozie.coord.input.logic;

import java.util.Date;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.NamespaceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.coord.CoordUtils;
import org.apache.oozie.coord.SyncCoordAction;
import org.apache.oozie.coord.input.dependency.CoordPullInputDependency;
import org.apache.oozie.coord.input.dependency.CoordPushInputDependency;
import org.apache.oozie.util.ELEvaluator;
import org.apache.oozie.util.LogUtils;
import org.apache.oozie.util.XLog;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public class CoordInputLogicEvaluatorUtil {

    private CoordinatorActionBean coordAction = null;
    private XLog log = XLog.getLog(getClass());

    public CoordInputLogicEvaluatorUtil(CoordinatorActionBean coordAction) {
        this.coordAction = coordAction;
        LogUtils.setLogInfo(coordAction);

    }

    public CoordInputLogicEvaluatorUtil() {
    }

    /**
     * Check pull missing dependencies.
     *
     * @return true, if successful
     * @throws JDOMException the JDOM exception
     */
    public boolean checkPullMissingDependencies() throws JDOMException {
        JexlEngine jexl = new OozieJexlEngine();

        String expression = CoordUtils.getInputLogic(coordAction.getActionXml().toString());
        if (StringUtils.isEmpty(expression)) {
            return true;
        }
        Expression e = jexl.createExpression(expression);

        JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseOne(
                coordAction, coordAction.getPullInputDependencies())));
        CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());

        if (result.isWaiting()) {
            return false;
        }
        return result.isTrue();
    }

    /**
     * Validate input logic.
     *
     * @throws JDOMException the JDOM exception
     * @throws CommandException
     */
    public void validateInputLogic() throws JDOMException, CommandException {
        JexlEngine jexl = new OozieJexlEngine();
        String expression = CoordUtils.getInputLogic(coordAction.getActionXml().toString());
        if (StringUtils.isEmpty(expression)) {
            return;
        }
        Expression e = jexl.createExpression(expression);
        JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(
                new CoordInputLogicEvaluatorPhaseValidate(coordAction)));
        try {
            Object result = e.evaluate(jc);
            log.debug("Input logic expression is [{0}] and evaluate result is [{1}]", expression, result);

        }
        catch (RuntimeException re) {
            throw new CommandException(ErrorCode.E1028, re.getCause().getMessage());
        }

    }

    /**
     * Get input dependencies.
     *
     * @param name the name
     * @param syncCoordAction the sync coord action
     * @return the string
     * @throws JDOMException the JDOM exception
     */
    public String getInputDependencies(String name, SyncCoordAction syncCoordAction) throws JDOMException {
        JexlEngine jexl = new OozieJexlEngine();

        CoordinatorActionBean coordAction = new CoordinatorActionBean();
        ELEvaluator eval = ELEvaluator.getCurrent();
        coordAction.setId(syncCoordAction.getActionId());
        Element eJob = XmlUtils.parseXml(eval.getVariable(".actionInputLogic").toString());
        String expression = new InputLogicParser().parseWithName(eJob, name);

        Expression e = jexl.createExpression(expression);

        CoordPullInputDependency pull = (CoordPullInputDependency) syncCoordAction.getPullDependencies();
        CoordPushInputDependency push = (CoordPushInputDependency) syncCoordAction.getPushDependencies();

        coordAction.setPushInputDependencies(push);

        coordAction.setPullInputDependencies(pull);

        JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseThree(
                coordAction, eval)));
        CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] is [{1}] and evaluate result is [{2}]", name, expression,
                result.isTrue());

        if (!result.isTrue()) {
            return name + " is not resolved";
        }
        return result.getDataSets();

    }

    /**
     * Check push dependencies.
     *
     * @return true, if successful
     * @throws JDOMException the JDOM exception
     */
    public boolean checkPushDependencies() throws JDOMException {
        JexlEngine jexl = new OozieJexlEngine();

        String expression = CoordUtils.getInputLogic(coordAction.getActionXml().toString());
        if (StringUtils.isEmpty(expression)) {
            return true;
        }

        Expression e = jexl.createExpression(expression);
        JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseOne(
                coordAction, coordAction.getPushInputDependencies())));
        CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());

        if (result.isWaiting()) {
            return false;
        }
        return result.isTrue();
    }

    /**
     * Check unresolved.
     *
     * @param actualTime the actual time
     * @return true, if successful
     * @throws JDOMException the JDOM exception
     */
    public boolean checkUnResolved(Date actualTime) throws JDOMException {
        JexlEngine jexl = new OozieJexlEngine();

        String expression = CoordUtils.getInputLogic(coordAction.getActionXml().toString());
        if (StringUtils.isEmpty(expression)) {
            return true;
        }

        Expression e = jexl.createExpression(expression);
        JexlContext jc = new OozieJexlParser(jexl, new CoordInputLogicBuilder(new CoordInputLogicEvaluatorPhaseTwo(
                coordAction, actualTime)));
        CoordInputLogicEvaluatorResult result = (CoordInputLogicEvaluatorResult) e.evaluate(jc);
        log.debug("Input logic expression for [{0}] and evaluate result is [{1}]", expression, result.isTrue());

        if (result.isWaiting()) {
            return false;
        }
        return result.isTrue();

    }

    public class OozieJexlParser implements JexlContext, NamespaceResolver {
        private final JexlEngine jexl;
        private final CoordInputLogicBuilder object;

        @Override
        public Object resolveNamespace(String name) {
            return object;
        }

        public OozieJexlParser(JexlEngine engine, CoordInputLogicBuilder wrapped) {
            this.jexl = engine;
            this.object = wrapped;
        }

        public Object get(String name) {
            return jexl.getProperty(object, name);
        }

        public void set(String name, Object value) {
            jexl.setProperty(object, name, value);
        }

        public boolean has(String name) {
            return jexl.getUberspect().getPropertyGet(object, name, null) != null;
        }

    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/InputLogicParser.java b/core/src/main/java/org/apache/oozie/coord/input/logic/InputLogicParser.java
new file mode 100644
index 000000000..f1f6b419f
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/InputLogicParser.java
@@ -0,0 +1,309 @@
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

package org.apache.oozie.coord.input.logic;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Parses xml into jexl expression
 */
public class InputLogicParser {

    public final static String COORD_INPUT_EVENTS_DATA_IN = "data-in";

    public final static String AND = "and";

    public final static String OR = "or";

    public final static String COMBINE = "combine";

    /**
     * Parses the xml.
     *
     * @param root the root
     * @return the string
     */
    public String parse(Element root) {
        return parseWithName(root, null);

    }

    /**
     * Parses the xml with name.
     *
     * @param root the root
     * @param name the name
     * @return the string
     */
    @SuppressWarnings("unchecked")
    public String parseWithName(Element root, String name) {
        if (root == null) {
            return "";
        }
        StringBuffer parsedString = new StringBuffer();

        List<Element> childrens = root.getChildren();
        for (int i = 0; i < childrens.size(); i++) {
            String childName = childrens.get(i).getAttributeValue("name");
            String min = childrens.get(i).getAttributeValue("min");
            String wait = childrens.get(i).getAttributeValue("wait");

            if (name == null || name.equals(childName)) {
                parsedString.append(parse(childrens.get(i), getOpt(childrens.get(i).getName()), min, wait));
            }
            else {
                parsedString.append(parseWithName(childrens.get(i), name));
            }
        }
        return parsedString.toString();
    }

    public String parse(Element root, String opt, String min, String wait) {
        StringBuffer parsedString = new StringBuffer();

        Namespace ns = root.getNamespace();
        if (root.getName().equals(COMBINE)) {
            parsedString.append("(");
            parsedString.append(processCombinedNode(root, getOpt(root.getName()), getMin(root, min),
                    getWait(root, wait)));
            parsedString.append(")");
        }
        else if (root.getName().equals(AND) || root.getName().equals(OR)) {
            parsedString.append("(");
            parsedString.append(parseAllChildren(root, opt, getOpt(root.getName()), getMin(root, min),
                    getWait(root, wait)));
            parsedString.append(")");

        }
        else if (root.getChild(COORD_INPUT_EVENTS_DATA_IN, ns) != null) {
            parsedString.append("(");
            parsedString.append(processChildNode(root, getOpt(root.getName()), getMin(root, min), getWait(root, wait)));
            parsedString.append(")");
        }
        else if (root.getName().equals(COORD_INPUT_EVENTS_DATA_IN)) {
            parsedString.append(parseDataInNode(root, min, wait));

        }
        return parsedString.toString();

    }

    /**
     * Parses the all children.
     *
     * @param root the root
     * @param parentOpt the parent opt
     * @param opt the opt
     * @param min the min
     * @param wait the wait
     * @return the string
     */
    @SuppressWarnings("unchecked")
    private String parseAllChildren(Element root, String parentOpt, String opt, String min, String wait) {
        StringBuffer parsedString = new StringBuffer();

        List<Element> childrens = root.getChildren();
        for (int i = 0; i < childrens.size(); i++) {
            String currentMin = min;
            String currentWait = wait;
            String childMin = childrens.get(i).getAttributeValue("min");
            String childWait = childrens.get(i).getAttributeValue("wait");
            if (!StringUtils.isEmpty(childMin)) {
                currentMin = childMin;
            }
            if (!StringUtils.isEmpty(childWait)) {
                currentWait = childWait;
            }
            parsedString.append(parse(childrens.get(i), opt, currentMin, currentWait));
            if (i < childrens.size() - 1) {
                if (!StringUtils.isEmpty(opt))
                    parsedString.append(" " + opt + " ");
            }
        }
        return parsedString.toString();

    }

    /**
     * Parses the data in node.
     *
     * @param root the root
     * @param min the min
     * @param wait the wait
     * @return the string
     */
    private String parseDataInNode(Element root, String min, String wait) {
        StringBuffer parsedString = new StringBuffer();

        String nestedChildDataName = root.getAttributeValue("dataset");

        parsedString.append("dependencyBuilder.input(\"" + nestedChildDataName + "\")");
        appendMin(root, min, parsedString);
        appendWait(root, wait, parsedString);
        parsedString.append(".build()");
        return parsedString.toString();
    }

    /**
     * Process child node.
     *
     * @param root the root
     * @param opt the opt
     * @param min the min
     * @param wait the wait
     * @return the string
     */
    @SuppressWarnings("unchecked")
    private String processChildNode(final Element root, final String opt, final String min, final String wait) {
        StringBuffer parsedString = new StringBuffer();

        Namespace ns = root.getNamespace();

        List<Element> childrens = root.getChildren(COORD_INPUT_EVENTS_DATA_IN, ns);

        for (int i = 0; i < childrens.size(); i++) {
            parsedString.append(parseDataInNode(childrens.get(i), min, wait));

            if (i < childrens.size() - 1) {
                parsedString.append(" " + opt + " ");
            }
        }
        return parsedString.toString();
    }

    /**
     * Process combined node.
     *
     * @param root the root
     * @param opt the opt
     * @param min the min
     * @param wait the wait
     * @return the string
     */
    @SuppressWarnings("unchecked")
    private String processCombinedNode(final Element root, final String opt, final String min, final String wait) {
        StringBuffer parsedString = new StringBuffer();

        Namespace ns = root.getNamespace();

        List<Element> childrens = root.getChildren(COORD_INPUT_EVENTS_DATA_IN, ns);
        parsedString.append("dependencyBuilder.combine(");

        for (int i = 0; i < childrens.size(); i++) {
            String nestedChildDataName = childrens.get(i).getAttributeValue("dataset");
            parsedString.append("\"" + nestedChildDataName + "\"");
            if (i < childrens.size() - 1) {
                parsedString.append(",");
            }
        }
        parsedString.append(")");

        appendMin(root, min, parsedString);
        appendWait(root, wait, parsedString);
        parsedString.append(".build()");
        return parsedString.toString();

    }

    /**
     * Gets the opt.
     *
     * @param opt the opt
     * @return the opt
     */
    private String getOpt(String opt) {
        if (opt.equalsIgnoreCase("or")) {
            return "||";
        }

        if (opt.equalsIgnoreCase("and")) {
            return "&&";
        }

        return "";

    }

    /**
     * Gets the min.
     *
     * @param root the root
     * @param parentMin the parent min
     * @return the min
     */
    private String getMin(Element root, String parentMin) {
        String min = root.getAttributeValue("min");
        if (StringUtils.isEmpty(min)) {
            return parentMin;
        }
        return min;

    }

    /**
     * Gets the wait.
     *
     * @param root the root
     * @param parentWait the parent wait
     * @return the wait
     */
    private String getWait(Element root, String parentWait) {
        String wait = root.getAttributeValue("wait");
        if (StringUtils.isEmpty(parentWait)) {
            return parentWait;
        }
        return wait;

    }

    private void appendWait(final Element root, String wait, StringBuffer parsedString) {
        String childWait = root.getAttributeValue("wait");
        if (!StringUtils.isEmpty(childWait)) {
            parsedString.append(".inputWait(" + childWait + ")");

        }
        else {
            if (!StringUtils.isEmpty(wait)) {
                parsedString.append(".inputWait(" + wait + ")");

            }
        }

    }

    private void appendMin(final Element root, String min, StringBuffer parsedString) {
        String childMin = root.getAttributeValue("min");

        if (!StringUtils.isEmpty(childMin)) {
            parsedString.append(".min(" + childMin + ")");

        }
        else {
            if (!StringUtils.isEmpty(min)) {
                parsedString.append(".min(" + min + ")");

            }
        }
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlEngine.java b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlEngine.java
new file mode 100644
index 000000000..66c4f2b5a
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlEngine.java
@@ -0,0 +1,47 @@
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

package org.apache.oozie.coord.input.logic;

import org.apache.commons.jexl2.Interpreter;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;

/**
 * Oozie implementation of Jexl Engine
 *
 */
public class OozieJexlEngine extends JexlEngine {
    OozieJexlInterpreter oozieInterpreter;

    public OozieJexlEngine() {
    }

    protected Interpreter createInterpreter(JexlContext context, boolean strictFlag, boolean silentFlag) {
        if (oozieInterpreter == null) {
            oozieInterpreter = new OozieJexlInterpreter(this, context == null ? EMPTY_CONTEXT : context, true,
                    silentFlag);
        }
        return oozieInterpreter;
    }

    public OozieJexlInterpreter getOozieInterpreter() {
        return oozieInterpreter;
    }

}
diff --git a/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
new file mode 100644
index 000000000..2044723db
-- /dev/null
++ b/core/src/main/java/org/apache/oozie/coord/input/logic/OozieJexlInterpreter.java
@@ -0,0 +1,73 @@
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

package org.apache.oozie.coord.input.logic;

import org.apache.commons.jexl2.Interpreter;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.parser.ASTAndNode;
import org.apache.commons.jexl2.parser.ASTOrNode;
import org.apache.commons.jexl2.parser.JexlNode;

/**
 * Oozie implementation of jexl Interpreter
 */
public class OozieJexlInterpreter extends Interpreter {

    protected OozieJexlInterpreter(Interpreter base) {
        super(base);
    }

    public Object interpret(JexlNode node) {
        return node.jjtAccept(this, "");
    }

    public OozieJexlInterpreter(JexlEngine jexlEngine, JexlContext jexlContext, boolean strictFlag, boolean silentFlag) {
        super(jexlEngine, jexlContext, strictFlag, silentFlag);
    }

    public Object visit(ASTOrNode node, Object data) {
        CoordInputLogicEvaluatorResult left = (CoordInputLogicEvaluatorResult) node.jjtGetChild(0)
                .jjtAccept(this, data);

        if (left.isTrue()) {
            return left;
        }

        return node.jjtGetChild(1).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {
        CoordInputLogicEvaluatorResult left = (CoordInputLogicEvaluatorResult) node.jjtGetChild(0)
                .jjtAccept(this, data);

        if (!left.isTrue()) {
            return left;
        }
        CoordInputLogicEvaluatorResult right = (CoordInputLogicEvaluatorResult) node.jjtGetChild(1).jjtAccept(this,
                data);
        if (right.isTrue()) {
            right.appendDataSets(left.getDataSets());
        }

        return right;
    }

}
diff --git a/core/src/main/java/org/apache/oozie/dependency/ActionDependency.java b/core/src/main/java/org/apache/oozie/dependency/ActionDependency.java
index c280d1dc2..fe7a32721 100644
-- a/core/src/main/java/org/apache/oozie/dependency/ActionDependency.java
++ b/core/src/main/java/org/apache/oozie/dependency/ActionDependency.java
@@ -25,7 +25,7 @@ public class ActionDependency {
     private List<String> missingDependencies;
     private List<String> availableDependencies;
 
    ActionDependency(List<String> missingDependencies, List<String> availableDependencies) {
    public ActionDependency(List<String> missingDependencies, List<String> availableDependencies) {
         this.missingDependencies = missingDependencies;
         this.availableDependencies = availableDependencies;
     }
diff --git a/core/src/main/java/org/apache/oozie/dependency/DependencyChecker.java b/core/src/main/java/org/apache/oozie/dependency/DependencyChecker.java
index a5507575a..bdd854ffb 100644
-- a/core/src/main/java/org/apache/oozie/dependency/DependencyChecker.java
++ b/core/src/main/java/org/apache/oozie/dependency/DependencyChecker.java
@@ -21,7 +21,9 @@ package org.apache.oozie.dependency;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.List;

 import org.apache.commons.lang.StringUtils;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.oozie.ErrorCode;
@@ -53,6 +55,9 @@ public class DependencyChecker {
      * @return missing dependencies as a array
      */
     public static String[] dependenciesAsArray(String missingDependencies) {
        if(StringUtils.isEmpty(missingDependencies)){
            return new String[0];
        }
         return missingDependencies.split(CoordELFunctions.INSTANCE_SEPARATOR);
     }
 
@@ -69,7 +74,7 @@ public class DependencyChecker {
      */
     public static ActionDependency checkForAvailability(String missingDependencies, Configuration actionConf,
             boolean stopOnFirstMissing) throws CommandException {
        return checkForAvailability(dependenciesAsArray(missingDependencies), actionConf, stopOnFirstMissing);
        return checkForAvailability(Arrays.asList(dependenciesAsArray(missingDependencies)), actionConf, stopOnFirstMissing);
     }
 
     /**
@@ -83,7 +88,7 @@ public class DependencyChecker {
      * @return ActionDependency which has the list of missing and available dependencies
      * @throws CommandException
      */
    public static ActionDependency checkForAvailability(String[] missingDependencies, Configuration actionConf,
    public static ActionDependency checkForAvailability(List<String> missingDependencies, Configuration actionConf,
             boolean stopOnFirstMissing) throws CommandException {
         final XLog LOG = XLog.getLog(DependencyChecker.class); //OOZIE-1251. Don't initialize as static variable.
         String user = ParamChecker.notEmpty(actionConf.get(OozieClient.USER_NAME), OozieClient.USER_NAME);
@@ -92,9 +97,9 @@ public class DependencyChecker {
         URIHandlerService uriService = Services.get().get(URIHandlerService.class);
         boolean continueChecking = true;
         try {
            for (int index = 0; index < missingDependencies.length; index++) {
            for (int index = 0; index < missingDependencies.size(); index++) {
                 if (continueChecking) {
                    String dependency = missingDependencies[index];
                    String dependency = missingDependencies.get(index);
 
                     URI uri = new URI(dependency);
                     URIHandler uriHandler = uriService.getURIHandler(uri);
@@ -113,7 +118,7 @@ public class DependencyChecker {
 
                 }
                 else {
                    missingDeps.add(missingDependencies[index]);
                    missingDeps.add(missingDependencies.get(index));
                 }
             }
         }
diff --git a/core/src/main/java/org/apache/oozie/dependency/FSURIHandler.java b/core/src/main/java/org/apache/oozie/dependency/FSURIHandler.java
index 7c1aadf27..65d85b8e6 100644
-- a/core/src/main/java/org/apache/oozie/dependency/FSURIHandler.java
++ b/core/src/main/java/org/apache/oozie/dependency/FSURIHandler.java
@@ -113,6 +113,15 @@ public class FSURIHandler implements URIHandler {
         return uri;
     }
 
    @Override
    public String getURIWithoutDoneFlag(String uri, String doneFlag) throws URIHandlerException {
        if (doneFlag.length() > 0 && uri.endsWith(doneFlag)) {
            return uri.substring(0, uri.lastIndexOf("/" + doneFlag));
        }
        return uri;
    }


     @Override
     public void validate(String uri) throws URIHandlerException {
     }
diff --git a/core/src/main/java/org/apache/oozie/dependency/HCatURIHandler.java b/core/src/main/java/org/apache/oozie/dependency/HCatURIHandler.java
index 1bbf37d8d..67b37ec4d 100644
-- a/core/src/main/java/org/apache/oozie/dependency/HCatURIHandler.java
++ b/core/src/main/java/org/apache/oozie/dependency/HCatURIHandler.java
@@ -210,6 +210,11 @@ public class HCatURIHandler implements URIHandler {
         return uri;
     }
 
    @Override
    public String getURIWithoutDoneFlag(String uri, String doneFlag) throws URIHandlerException {
        return uri;
    }

     @Override
     public void validate(String uri) throws URIHandlerException {
         try {
diff --git a/core/src/main/java/org/apache/oozie/dependency/URIHandler.java b/core/src/main/java/org/apache/oozie/dependency/URIHandler.java
index bc9471685..45e23fb47 100644
-- a/core/src/main/java/org/apache/oozie/dependency/URIHandler.java
++ b/core/src/main/java/org/apache/oozie/dependency/URIHandler.java
@@ -168,6 +168,19 @@ public interface URIHandler {
      */
     public String getURIWithDoneFlag(String uri, String doneFlag) throws URIHandlerException;
 
    /**
     * Get the URI path from path which has done flag
     *
     * @param uri URI of the dependency
     * @param doneFlag flag that determines URI availability
     *
     * @return the final URI without the doneFlag incorporated
     *
     * @throws URIHandlerException
     */
    public String getURIWithoutDoneFlag(String uri, String doneFlag) throws URIHandlerException;


     /**
      * Check whether the URI is valid or not
      * @param uri
@@ -220,4 +233,5 @@ public interface URIHandler {
 
     }
 

 }
diff --git a/core/src/main/java/org/apache/oozie/util/WritableUtils.java b/core/src/main/java/org/apache/oozie/util/WritableUtils.java
index 76a689535..aa027e37b 100644
-- a/core/src/main/java/org/apache/oozie/util/WritableUtils.java
++ b/core/src/main/java/org/apache/oozie/util/WritableUtils.java
@@ -20,6 +20,7 @@ package org.apache.oozie.util;
 
 import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.util.ReflectionUtils;
import org.apache.oozie.compression.CodecFactory;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
@@ -28,12 +29,19 @@ import java.io.DataOutputStream;
 import java.io.IOException;
 import java.io.DataOutput;
 import java.io.DataInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
 
 /**
  * Utility class to write/read Hadoop writables to/from a byte array.
  */
 public class WritableUtils {
 
    public static XLog LOG = XLog.getLog(WritableUtils.class);

     /**
      * Write a writable to a byte array.
      *
@@ -60,7 +68,6 @@ public class WritableUtils {
      * @param clazz writable class.
      * @return writable deserialized from the byte array.
      */
    @SuppressWarnings("unchecked")
     public static <T extends Writable> T fromByteArray(byte[] array, Class<T> clazz) {
         try {
             T o = (T) ReflectionUtils.newInstance(clazz, null);
@@ -99,4 +106,143 @@ public class WritableUtils {
         String str = dataInput.readUTF();
         return (str.equals(NULL)) ? null : str;
     }

    /**
     * Read list.
     *
     * @param <T> the generic type
     * @param dataInput the data input
     * @param clazz the clazz
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> List<T> readList(DataInput dataInput, Class<T> clazz) throws IOException {
        List<T> a = new ArrayList<T>();
        int count = dataInput.readInt();
        for (int i = 0; i < count; i++) {
            T o = (T) ReflectionUtils.newInstance(clazz, null);
            o.readFields(dataInput);
            a.add(o);
        }
        return a;
    }

    public static List<String> readStringList(DataInput dataInput) throws IOException {
        List<String> a = new ArrayList<String>();
        int count = dataInput.readInt();
        for (int i = 0; i < count; i++) {
            a.add(readBytesAsString(dataInput));
        }
        return a;
    }

    /**
     * Write list.
     *
     * @param <T> the generic type
     * @param dataOutput the data output
     * @param list the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> void writeList(DataOutput dataOutput, List<T> list) throws IOException {
        dataOutput.writeInt(list.size());
        for (T t : list) {
            t.write(dataOutput);
        }
    }

    public static void writeStringList(DataOutput dataOutput, List<String> list) throws IOException {
        dataOutput.writeInt(list.size());
        for (String str : list) {
            writeStringAsBytes(dataOutput, str);
        }
    }

    /**
     * Write map.
     *
     * @param <T> the generic type
     * @param dataOutput the data output
     * @param map the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> void writeMap(DataOutput dataOutput, Map<String, T> map) throws IOException {
        dataOutput.writeInt(map.size());
        for (Entry<String, T> t : map.entrySet()) {
            writeStringAsBytes(dataOutput, t.getKey());
            t.getValue().write(dataOutput);
        }
    }

    /**
     * Write map with list.
     *
     * @param <T> the generic type
     * @param dataOutput the data output
     * @param map the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> void writeMapWithList(DataOutput dataOutput, Map<String, List<T>> map)
            throws IOException {
        dataOutput.writeInt(map.size());
        for (Entry<String, List<T>> t : map.entrySet()) {
            writeStringAsBytes(dataOutput, t.getKey());
            writeList(dataOutput, t.getValue());
        }
    }

    /**
     * Read map.
     *
     * @param <T> the generic type
     * @param dataInput the data input
     * @param clazz the clazz
     * @return the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> Map<String, T> readMap(DataInput dataInput, Class<T> clazz) throws IOException {
        Map<String, T> map = new HashMap<String, T>();
        int count = dataInput.readInt();
        for (int i = 0; i < count; i++) {
            String key = readBytesAsString(dataInput);
            T value = (T) ReflectionUtils.newInstance(clazz, null);
            value.readFields(dataInput);
            map.put(key, value);
        }
        return map;
    }

    /**
     * Read map with list.
     *
     * @param <T> the generic type
     * @param dataInput the data input
     * @param clazz the clazz
     * @return the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static <T extends Writable> Map<String, List<T>> readMapWithList(DataInput dataInput, Class<T> clazz)
            throws IOException {
        Map<String, List<T>> map = new HashMap<String, List<T>>();
        int count = dataInput.readInt();
        for (int i = 0; i < count; i++) {
            String key = readBytesAsString(dataInput);
            map.put(key, readList(dataInput, clazz));
        }
        return map;
    }

    public static void writeStringAsBytes(DataOutput dOut, String value) throws IOException {
        byte[] data = value.getBytes(CodecFactory.UTF_8_ENCODING);
        dOut.writeInt(data.length);
        dOut.write(data);
    }

    public static String readBytesAsString(DataInput dIn) throws IOException {
        int length = dIn.readInt();
        byte[] data = new byte[length];
        dIn.readFully(data);
        return new String(data, CodecFactory.UTF_8_ENCODING);
    }

 }
diff --git a/core/src/main/resources/oozie-default.xml b/core/src/main/resources/oozie-default.xml
index ca49fa66c..3ff7320df 100644
-- a/core/src/main/resources/oozie-default.xml
++ b/core/src/main/resources/oozie-default.xml
@@ -1516,7 +1516,7 @@
         <name>oozie.service.SchemaService.coord.schemas</name>
         <value>
             oozie-coordinator-0.1.xsd,oozie-coordinator-0.2.xsd,oozie-coordinator-0.3.xsd,oozie-coordinator-0.4.xsd,
            oozie-sla-0.1.xsd,oozie-sla-0.2.xsd
            oozie-coordinator-0.5.xsd,oozie-sla-0.1.xsd,oozie-sla-0.2.xsd
         </value>
         <description>
             List of schemas for coordinators (separated by commas).
diff --git a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
index 1fe1b3adb..c27a40aba 100644
-- a/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
++ b/core/src/test/java/org/apache/oozie/command/coord/TestCoordActionInputCheckXCommand.java
@@ -19,7 +19,6 @@
 package org.apache.oozie.command.coord;
 
 import java.io.IOException;
import java.io.Reader;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.List;
@@ -34,6 +33,7 @@ import org.apache.oozie.client.CoordinatorJob.Execution;
 import org.apache.oozie.client.CoordinatorJob.Timeunit;
 import org.apache.oozie.command.CommandException;
 import org.apache.oozie.coord.CoordELFunctions;
import org.apache.oozie.coord.input.dependency.CoordOldInputDependency;
 import org.apache.oozie.executor.jpa.CoordActionGetForInputCheckJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionGetJPAExecutor;
 import org.apache.oozie.executor.jpa.CoordActionInsertJPAExecutor;
@@ -553,13 +553,14 @@ public class TestCoordActionInputCheckXCommand extends XDataTestCase {
         Path appPath = new Path(getFsTestCaseDir(), "coord");
         String inputDir = appPath.toString() + "/coord-input/2010/07/09/01/00";
         String nonExistDir = inputDir.replaceFirst("localhost", "nonExist");
        CoordinatorActionBean actionBean = new CoordinatorActionBean();
         try {
            caicc.pathExists(nonExistDir, new XConfiguration(), getTestUser());
            new CoordOldInputDependency().pathExists(actionBean, nonExistDir, new XConfiguration(), getTestUser());
             fail("Should throw exception due to non-existent NN path. Therefore fail");
         }
         catch (IOException ioe) {
            assertEquals(caicc.getCoordActionErrorCode(), "E0901");
            assertTrue(caicc.getCoordActionErrorMsg().contains("not in Oozie's whitelist"));
            assertEquals(actionBean.getErrorCode(), "E0901");
            assertTrue(actionBean.getErrorMessage().contains("not in Oozie's whitelist"));
         }
     }
 
diff --git a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
new file mode 100644
index 000000000..c58b18b73
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordInputLogicPush.java
@@ -0,0 +1,645 @@
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

package org.apache.oozie.coord.input.logic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordActionInputCheckXCommand;
import org.apache.oozie.command.coord.CoordActionStartXCommand;
import org.apache.oozie.command.coord.CoordMaterializeTransitionXCommand;
import org.apache.oozie.command.coord.CoordPushDependencyCheckXCommand;
import org.apache.oozie.command.coord.CoordSubmitXCommand;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XHCatTestCase;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.XConfiguration;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public class TestCoordInputLogicPush extends XHCatTestCase {

    private Services services;
    private String server;
    private static final String table = "table1";

    final long TIME_DAYS = 60 * 60 * 1000 * 24;

    enum TEST_TYPE {
        CURRENT_SINGLE, CURRENT_RANGE, LATEST_SINGLE, LATEST_RANGE;
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        services = super.setupServicesForHCatalog();
        services.init();
        createTestTable();
        server = getMetastoreAuthority();

    }

    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
        dropTestTable();
    }

    private void createSingleTestTable(String db) throws Exception {
        dropTable(db, table, true);
        dropDatabase(db, true);
        createDatabase(db);
        createTable(db, table, "dt,country");
    }

    private void createTestTable() throws Exception {

        createSingleTestTable("db_a");
        createSingleTestTable("db_b");
        createSingleTestTable("db_c");
        createSingleTestTable("db_d");
        createSingleTestTable("db_e");
        createSingleTestTable("db_f");

    }

    private void dropSingleTestTable(String db) throws Exception {
        dropTable(db, table, false);
        dropDatabase(db, false);
    }

    private void dropTestTable() throws Exception {

        dropSingleTestTable("db_a");
        dropSingleTestTable("db_b");
        dropSingleTestTable("db_c");
        dropSingleTestTable("db_d");
        dropSingleTestTable("db_e");
        dropSingleTestTable("db_f");

    }

    public void testExists() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                  "<data-in dataset=\"B\" />"+
                  "<data-in dataset=\"D\" />"+
         "</or>";
        //@formatter:on
        conf.set("partitionName", "test");
        String jobId = _testCoordSubmit("coord-inputlogic-hcat.xml", conf, inputLogic, TEST_TYPE.CURRENT_SINGLE);

        String input = addPartition("db_b", "table1", "dt=20141008;country=usa");

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        Configuration runConf = getActionConf(actionBean);
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 1);
        checkDataSets(dataSets, input);

    }

    public void testNestedCondition3() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
                  "<and>" +
                          "<data-in dataset=\"A\" />"+
                          "<data-in dataset=\"B\" />"+
                   "</and>" +
                   "<and>"+
                          "<data-in dataset=\"C\" />"+
                          "<data-in dataset=\"D\" />"+
                   "</and>"+
                   "<and>"+
                       "<data-in dataset=\"E\" />"+
                       "<data-in dataset=\"F\" />"+
                   "</and>"+
         "</and>";
        //@formatter:on
        conf.set("partitionName", "test");
        final String jobId = _testCoordSubmit("coord-inputlogic-hcat.xml", conf, inputLogic, TEST_TYPE.CURRENT_SINGLE);

        String input1 = addPartition("db_a", "table1", "dt=20141008;country=usa");
        String input2 = addPartition("db_b", "table1", "dt=20141008;country=usa");
        String input3 = addPartition("db_c", "table1", "dt=20141008;country=usa");
        String input4 = addPartition("db_d", "table1", "dt=20141008;country=usa");
        String input5 = addPartition("db_e", "table1", "dt=20141008;country=usa");
        String input6 = addPartition("db_f", "table1", "dt=20141008;country=usa");

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        Configuration runConf = getActionConf(actionBean);
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 6);
        checkDataSets(dataSets, input1, input2, input3, input4, input5, input6);

    }

    public void testNestedConditionWithRange() throws Exception {

        Configuration conf = getConfForCombine();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 10 * TIME_DAYS)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));

        //@formatter:off
        String inputLogic =
        "<and name=\"test\" min=\"2\" >"+
                  "<or min=\"2\">" +
                          "<data-in dataset=\"A\" />"+
                          "<data-in dataset=\"B\" />"+
                   "</or>" +
                   "<or min=\"2\">"+
                          "<data-in dataset=\"C\" />"+
                          "<data-in dataset=\"D\" />"+
                   "</or>"+
                   "<and min=\"2\">"+
                       "<data-in dataset=\"A\" />"+
                       "<data-in dataset=\"C\" />"+
                   "</and>"+
         "</and>";
        //@formatter:on
        conf.set("partitionName", "test");
        final String jobId = _testCoordSubmit("coord-inputlogic-hcat.xml", conf, inputLogic, TEST_TYPE.CURRENT_RANGE,
                TEST_TYPE.LATEST_RANGE);
        List<String> inputPartition = createPartitionWithTime("db_a", now, 0, 1, 2);
        inputPartition.addAll(createPartitionWithTime("db_c", now, 0, 1, 2));
        startCoordAction(jobId);
        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        Configuration runConf = getActionConf(actionBean);
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 12);
        checkDataSets(dataSets, inputPartition.toArray(new String[inputPartition.size()]));

    }


    public void testLatestRange() throws Exception {

        Configuration conf = getConfForCombine();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 10 * TIME_DAYS)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));

        String inputLogic =
        //@formatter:off
        "<and name=\"test\">"+
              "<data-in dataset=\"A\" />" +
              "<data-in dataset=\"B\" />" +
         "</and>";
        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-combine.xml", conf, inputLogic, TEST_TYPE.LATEST_RANGE);

        List<String> inputDir = createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4, 5);
        inputDir.addAll(createPartitionWithTime("db_a", now, 0, 1, 2, 3, 4, 5));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 12);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));

    }

    public void testCurrentLatest() throws Exception {

        Configuration conf = getConfForCombine();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 10 * TIME_DAYS)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));

        String inputLogic =
//@formatter:off
        "<and name=\"test\">"+
              "<data-in dataset=\"A\"/>" +
              "<data-in dataset=\"B\"/>" +
         "</and>";
        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-combine.xml", conf, inputLogic, TEST_TYPE.LATEST_RANGE,
                TEST_TYPE.CURRENT_RANGE);

        List<String> inputDir = createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4, 5);
        inputDir.addAll(createPartitionWithTime("db_a", now, 0, 1, 2, 3, 4, 5));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 12);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));

    }

    public void testLatestRangeComplex() throws Exception {

        Configuration conf = getConfForCombine();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 10 * TIME_DAYS)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));

        String inputLogic =
        //@formatter:off
        "<or name=\"test\">" +
            "<and>"+
                   "<data-in name=\"testA\" dataset=\"A\" />" +
                   "<data-in name=\"testB\" dataset=\"B\" />" +
             "</and>" +
             "<and name=\"test\">"+
                 "<data-in name=\"testC\" dataset=\"C\" />" +
                 "<data-in name=\"testD\" dataset=\"D\" />" +
             "</and>" +
        "</or>";

        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-combine.xml", conf, inputLogic, TEST_TYPE.LATEST_RANGE);
        List<String> inputDir = createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4, 5);
        inputDir.addAll(createPartitionWithTime("db_a", now, 0, 1, 2, 3, 4, 5));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 12);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));

    }

    public void testHcatHdfs() throws Exception {
        Configuration conf = getConfForCombine();
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String inputLogic =
        //@formatter:off
            "<and name=\"test\">" +
                       "<data-in name=\"testA\" dataset=\"A\" />" +
                       "<data-in name=\"testB\" dataset=\"B\" />" +
            "</and>";
            //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-combine.xml", conf, inputLogic, TEST_TYPE.CURRENT_SINGLE);

        String input1 = createTestCaseSubDir("input-data/b/2014/10/08/_SUCCESS".split("/"));
        String input2 = addPartition("db_a", "table1", "dt=20141008;country=usa");

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);
        checkDataSets(dataSets, input1, input2);

    }

    public void testHcatHdfsLatest() throws Exception {
        Configuration conf = getConfForCombine();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 10 * TIME_DAYS)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));
        conf.set("initial_instance", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * TIME_DAYS)));

        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd");
        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        sd.setTimeZone(tzUTC);

        String inputLogic =
        // @formatter:off
            "<and name=\"test\" min = \"1\" >" +
                       "<data-in dataset=\"A\" />" +
                       "<data-in dataset=\"D\" />" +
            "</and>";

        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-combine.xml", conf, inputLogic, TEST_TYPE.LATEST_RANGE);

        String input1 = createTestCaseSubDir(("input-data/d/" + sd.format(now) + "/_SUCCESS").split("/"));
        sd = new SimpleDateFormat("yyyyMMdd");
        String input2 = addPartition("db_a", "table1", "dt=" + sd.format(now) + ";country=usa");

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);
        checkDataSets(dataSets, input1, input2);

    }

    private Configuration getConf() throws Exception {
        Configuration conf = new XConfiguration();
        conf.set("start_time", "2014-10-08T00:00Z");
        conf.set("end_time", "2015-10-08T00:00Z");
        conf.set("initial_instance", "2014-10-08T00:00Z");

        String dataset1 = "hcat://" + getMetastoreAuthority();

        conf.set("data_set", dataset1.toString());
        conf.set("db_a", "db_a");
        conf.set("db_b", "db_b");
        conf.set("db_c", "db_c");
        conf.set("db_d", "db_d");
        conf.set("db_e", "db_e");
        conf.set("db_f", "db_f");
        conf.set("table", table);
        conf.set("wfPath", getWFPath());
        conf.set("partitionName", "test");

        return conf;

    }

    private Configuration getConfForCombine() throws Exception {
        Configuration conf = new XConfiguration();
        conf.set("start_time", "2014-10-08T00:00Z");
        conf.set("end_time", "2015-10-08T00:00Z");
        conf.set("initial_instance", "2014-10-08T00:00Z");

        conf.set("data_set_b", "file://" + getTestCaseDir() + "/input-data/b");
        conf.set("data_set_d", "file://" + getTestCaseDir() + "/input-data/d");
        conf.set("data_set_f", "file://" + getTestCaseDir() + "/input-data/f");

        conf.set("start_time", "2014-10-08T00:00Z");
        conf.set("end_time", "2015-10-08T00:00Z");
        conf.set("initial_instance_a", "2014-10-08T00:00Z");
        conf.set("initial_instance_b", "2014-10-08T00:00Z");

        String dataset1 = "hcat://" + getMetastoreAuthority();

        conf.set("data_set", dataset1.toString());
        conf.set("db_a", "db_a");
        conf.set("db_b", "db_b");
        conf.set("db_c", "db_c");
        conf.set("db_d", "db_d");
        conf.set("db_e", "db_e");
        conf.set("db_f", "db_f");
        conf.set("table", table);
        conf.set("wfPath", getWFPath());
        conf.set("partitionName", "test");

        return conf;

    }

    private String _testCoordSubmit(String coordinatorXml, Configuration conf, String inputLogic, TEST_TYPE... testType)
            throws Exception {
        String appPath = "file://" + getTestCaseDir() + File.separator + "coordinator.xml";

        String content = IOUtils.getResourceAsString(coordinatorXml, -1);
        content = content.replaceAll("=input-logic=", inputLogic);
        for (int i = 1; i <= 6; i++) {
            if (i - 1 < testType.length) {
                content = content.replaceAll("=data-in-param-" + i + "=", getEnumText(testType[i - 1]));
            }
            else {
                content = content.replaceAll("=data-in-param-" + i + "=", getEnumText(testType[testType.length - 1]));
            }
        }

        Writer writer = new FileWriter(new URI(appPath).getPath());
        IOUtils.copyCharStream(new StringReader(content), writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPath);
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nameNode", "hdfs://localhost:9000");
        conf.set("queueName", "default");
        conf.set("jobTracker", "localhost:9001");
        conf.set("examplesRoot", "examples");

        String coordId = null;

        try {
            coordId = new CoordSubmitXCommand(conf).call();
        }
        catch (CommandException e) {
            e.printStackTrace();
            fail("should not throw exception " + e.getMessage());
        }
        return coordId;
    }

    public String getWFPath() throws Exception {
        String workflowUri = getTestCaseFileUri("workflow.xml");
        String appXml = "<workflow-app xmlns='uri:oozie:workflow:0.1' name='map-reduce-wf'> " + "<start to='end' /> "
                + "<end name='end' /> " + "</workflow-app>";

        writeToFile(appXml, workflowUri);
        return workflowUri;
    }

    private void writeToFile(String appXml, String appPath) throws IOException {
        File wf = new File(URI.create(appPath));
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(wf));
            out.println(appXml);
        }
        catch (IOException iOException) {
            throw iOException;
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void checkDataSets(String dataSets, String... values) {

        Set<String> inputDataSets = new HashSet<String>();
        for (String dataSet : dataSets.split(",")) {
            if (dataSet.indexOf(getTestCaseDir()) >= 0) {
                inputDataSets.add(dataSet.substring(dataSet.indexOf(getTestCaseDir())));
            }
            else {
                inputDataSets.add(dataSet);
            }
        }

        for (String value : values) {
            assertTrue(inputDataSets.contains(value.replace("/_SUCCESS","")));
        }

    }

    private void startCoordAction(final String jobId) throws CommandException, JPAExecutorException {
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        new CoordPushDependencyCheckXCommand(jobId + "@1").call();
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

        waitFor(50 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                        CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
                return !actionBean.getStatus().equals(CoordinatorAction.Status.WAITING);
            }
        });

        CoordinatorAction actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION,
                jobId + "@1");
        assertFalse("Action status should not be waiting",
                actionBean.getStatus().equals(CoordinatorAction.Status.WAITING));

        waitFor(50 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                        CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
                return !actionBean.getStatus().equals(CoordinatorAction.Status.READY);
            }
        });
        CoordinatorJob coordJob = CoordJobQueryExecutor.getInstance().get(CoordJobQuery.GET_COORD_JOB, jobId);
        new CoordActionStartXCommand(actionBean.getId(), coordJob.getUser(), coordJob.getAppName(),
                actionBean.getJobId()).call();
    }

    @SuppressWarnings("unchecked")
    public Configuration getActionConf(CoordinatorActionBean actionBean) throws JDOMException {
        Configuration conf = new XConfiguration();
        Element eAction = XmlUtils.parseXml(actionBean.getActionXml());
        Element configElem = eAction.getChild("action", eAction.getNamespace())
                .getChild("workflow", eAction.getNamespace()).getChild("configuration", eAction.getNamespace());
        List<Element> elementList = configElem.getChildren("property", eAction.getNamespace());
        for (Element element : elementList) {
            conf.set(((Element) element.getChildren().get(0)).getText(),
                    ((Element) element.getChildren().get(1)).getText());
        }
        return conf;
    }

    private String getEnumText(TEST_TYPE testType) {
        switch (testType) {
            case LATEST_SINGLE:
                return "<instance>\\${coord:latest(0)}</instance>";
            case LATEST_RANGE:
                return "<start-instance>\\${coord:latest(-5)}</start-instance>"
                        + "<end-instance>\\${coord:latest(0)}</end-instance>";
            case CURRENT_SINGLE:
                return "<instance>\\${coord:current(0)}</instance>";
            case CURRENT_RANGE:
                return "<start-instance>\\${coord:current(-5)}</start-instance>"
                        + "<end-instance>\\${coord:current(0)}</end-instance>";
        }
        return "";

    }

    public List<String> createDirWithTime(String dirPrefix, Date date, int... hours) {

        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd");

        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        sd.setTimeZone(tzUTC);
        List<String> createdDirPath = new ArrayList<String>();

        for (int hour : hours) {
            createdDirPath
                    .add(createTestCaseSubDir((dirPrefix + sd.format(new Date(date.getTime() - hour * TIME_DAYS)) + "/_SUCCESS")
                            .split("/")));
        }
        return createdDirPath;
    }

    public List<String> createPartitionWithTime(String database, Date date, int... hours) throws Exception {

        List<String> createdPartition = new ArrayList<String>();
        SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        sd.setTimeZone(tzUTC);
        for (int hour : hours) {
            createdPartition.add(addPartition(database, "table1",
                    "dt=" + sd.format(new Date(date.getTime() - hour * TIME_DAYS)) + ";country=usa"));

        }
        return createdPartition;
    }

    protected String addPartition(String db, String table, String partitionSpec) throws Exception {
        super.addPartition(db, table, partitionSpec);
        return "hcat://" + server + "/" + db + "/" + table + "/" + partitionSpec;
    }

}
diff --git a/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
new file mode 100644
index 000000000..0679c8cbf
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/coord/input/logic/TestCoordinatorInputLogic.java
@@ -0,0 +1,1054 @@
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

package org.apache.oozie.coord.input.logic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.hadoop.conf.Configuration;
import org.apache.oozie.CoordinatorActionBean;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.OozieClient;
import org.apache.oozie.command.CommandException;
import org.apache.oozie.command.coord.CoordActionInputCheckXCommand;
import org.apache.oozie.command.coord.CoordActionStartXCommand;
import org.apache.oozie.command.coord.CoordMaterializeTransitionXCommand;
import org.apache.oozie.command.coord.CoordSubmitXCommand;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor;
import org.apache.oozie.executor.jpa.JPAExecutorException;
import org.apache.oozie.executor.jpa.CoordJobQueryExecutor.CoordJobQuery;
import org.apache.oozie.executor.jpa.CoordActionQueryExecutor.CoordActionQuery;
import org.apache.oozie.service.Services;
import org.apache.oozie.test.XDataTestCase;
import org.apache.oozie.util.DateUtils;
import org.apache.oozie.util.IOUtils;
import org.apache.oozie.util.XConfiguration;
import org.jdom.JDOMException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestCoordinatorInputLogic extends XDataTestCase {
    private Services services;

    @Before
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        services = new Services();
        services.init();
    }

    @After
    @Override
    protected void tearDown() throws Exception {
        services.destroy();
        super.tearDown();
    }


    @Test(expected = CommandException.class)
    public void testValidateRange() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<combine name=\"test\">"+
                        "<data-in dataset=\"A\" />"+
                        "<data-in dataset=\"b\" />"+
         "</combine>";
        String inputEvent =
        "<data-in name=\"A\" dataset=\"a\">" +
                    "<start-instance>${coord:current(-5)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
        "</data-in>" +
        "<data-in name=\"B\" dataset=\"b\">" +
                    "<start-instance>${coord:current(-4)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
       "</data-in>";
        //@formatter:on
        conf.set("partitionName", "test");
        try {
            _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic, inputEvent, true);
            fail();
        }
        catch (CommandException e) {
            assertEquals(e.getErrorCode(), ErrorCode.E0803);
        }
    }

    public void testDryRun() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                "<and>"+
                    "<or>"+
                        "<data-in dataset=\"A\" />"+
                        "<data-in dataset=\"B\" />"+
                    "</or>"+
                    "<or>"+
                        "<data-in dataset=\"C\" />"+
                        "<data-in dataset=\"D\" />"+
                    "</or>"+
                "</and>"+
                "<and>"+
                    "<data-in dataset=\"A\" />"+
                    "<data-in dataset=\"B\" />"+
                "</and>"+
         "</or>";
        //@formatter:on
        conf.set("partitionName", "test");
        _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic, "", true);

    }

    public void testNestedCondition() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
                "<or name=\"test\">"+
                        "<and>"+
                            "<or>"+
                                "<data-in dataset=\"A\" />"+
                                "<data-in dataset=\"B\" />"+
                            "</or>"+
                        "<or>"+
                            "<data-in dataset=\"C\" />"+
                            "<data-in dataset=\"D\" />"+
                        "</or>"+
                        "</and>"+
                            "<and>"+
                                "<data-in dataset=\"A\" />"+
                                "<data-in dataset=\"B\" />"+
                             "</and>"+
                        "</or>";
        //@formatter:on
        conf.set("partitionName", "test");

        final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);
        checkDataSets(dataSets, input1, input2);

    }

    public void testNestedCondition1() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
              "<or>"+
                  "<and>" +
                          "<data-in dataset=\"A\"/>"+
                          "<data-in dataset=\"B\"/>"+
                   "</and>" +
                   "<and>"+
                          "<data-in dataset=\"C\"/>"+
                          "<data-in dataset=\"D\"/>"+
                   "</and>"+
             "</or>"+
             "<and>"+
                 "<data-in dataset=\"E\"/>"+
                 "<data-in dataset=\"F\"/>"+
             "</and>"+
         "</and>";
        //@formatter:on
        conf.set("partitionName", "test");
        final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/e/2014/10/08/00/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/f/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 4);
        checkDataSets(dataSets, input1, input2, input3, input4);

    }

    public void testNestedCondition2() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<or name=\"${partitionName}\">"+
                  "<and>" +
                          "<data-in dataset=\"A\" />"+
                          "<data-in dataset=\"B\" />"+
                          "<data-in dataset=\"C\" />"+
                          "<data-in dataset=\"D\" />"+
                   "</and>" +
                   "<and>"+
                          "<data-in dataset=\"E\" />"+
                          "<data-in dataset=\"F\" />"+
                   "</and>"+
         "</or>";
        //@formatter:on
        conf.set("partitionName", "test");
        final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/c/2014/10/08/00/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/e/2014/10/08/00/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/f/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);
        checkDataSets(dataSets, input4, input5);
        checkDataSetsForFalse(dataSets, input1, input2, input3);

    }

    public void testNestedCondition3() throws Exception {
        Configuration conf = getConf();

        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
                  "<and>" +
                          "<data-in dataset=\"A\" />"+
                          "<data-in dataset=\"B\" />"+
                   "</and>" +
                   "<and>"+
                          "<data-in dataset=\"C\" />"+
                          "<data-in dataset=\"D\" />"+
                   "</and>"+
                   "<and>"+
                       "<data-in dataset=\"E\" />"+
                       "<data-in dataset=\"F\" />"+
                   "</and>"+
         "</and>";
        //@formatter:on
        conf.set("partitionName", "test");
        final String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/c/2014/10/08/00/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/d/2014/10/08/00/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/e/2014/10/08/00/_SUCCESS".split("/"));
        String input6 = createTestCaseSubDir("input-data/f/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 6);
        checkDataSets(dataSets, input1, input2, input3, input4, input5, input6);

    }

    public void testSimpleOr() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                  "<data-in dataset=\"A\" />"+
                  "<data-in dataset=\"B\" />"+
         "</or>";
        //@formatter:on
        conf.set("partitionName", "test");
        String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 1);
        checkDataSets(dataSets, input1);
    }

    public void testSimpleOr1() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                  "<and>" +
                          "<data-in dataset=\"C\" />"+
                          "<data-in dataset=\"D\" />"+
                   "</and>" +
                   "<or>"+
                          "<data-in dataset=\"A\" />"+
                          "<data-in dataset=\"B\" />"+
                   "</or>"+
          "</or>";

        String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        new CoordMaterializeTransitionXCommand(jobId, 3600).call();
        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        assertEquals(actionBean.getStatus(), CoordinatorAction.Status.WAITING);

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        String input1=createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));
        startCoordAction(jobId);
        actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 1);
        checkDataSets(dataSets, input1);

    }

    public void testOrWithMin() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                   "<data-in dataset=\"A\" min=\"3\"/>"+
                   "<data-in dataset=\"B\" min=\"3\"/>"+
        "</or>";
        //@formatter:on
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/b/2014/10/07/19/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 3);
        checkDataSets(dataSets, input3, input4, input5);
    }

    public void testAndWithMin() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
                   "<data-in dataset=\"A\" min=\"2\"/>"+
                   "<data-in dataset=\"B\" min=\"3\"/>"+
                   "<data-in dataset=\"C\" min=\"0\"/>"+

        "</and>";
        //@formatter:on
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/b/2014/10/07/19/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 5);
        checkDataSets(dataSets, input1, input2, input3, input4, input5, input5);
    }

    public void testMultipleInstance() throws Exception {
        Configuration conf = getConf();
        Date now = new Date();
        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
                   "<data-in dataset=\"A\" min=\"2\"/>"+
                   "<data-in dataset=\"B\"/>"+

        "</and>";
        String event =
                "<data-in name=\"A\" dataset=\"a\">" +
                        "<instance>${coord:current(-5)}</instance>" +
                        "<instance>${coord:latest(-1)}</instance>" +
                        "<instance>${coord:futureRange(0,2,10)}</instance>" +
                 "</data-in>" +
                 "<data-in name=\"B\" dataset=\"b\">" +
                     "<instance>${coord:latest(0)}</instance>" +
                     "<instance>${coord:latestRange(-3,0)}</instance>" +
                 "</data-in>" ;

        //@formatter:on
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 3 * 60 * 60 * 1000)));
        // 5 hour before
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));
        // 5 hour before
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, event);

        List<String> inputDir = createDirWithTime("input-data/a/", now, 3, 5, 0, -1, -2);
        inputDir.addAll(createDirWithTime("input-data/b/", now, 0, 1));

        startCoordActionForWaiting(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertTrue(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));

        inputDir.addAll(createDirWithTime("input-data/b/", now, 2, 3));

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));

        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 10);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));
    }

    public void testAnd() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<and name=\"test\">"+
                  "<data-in dataset=\"A\"/>"+
                  "<data-in dataset=\"B\"/>"+
         "</and>";
        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/b/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);
        checkDataSets(dataSets, input1, input2);

    }

    public void testCombine() throws Exception {

        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<combine name=\"test\">"+
                   "<data-in dataset=\"A\" />"+
                   "<data-in dataset=\"B\" />"+
        "</combine>";
        //@formatter:on
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/a/2014/10/07/22/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));
        String input6 = createTestCaseSubDir("input-data/b/2014/10/07/19/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 6);
        checkDataSets(dataSets, input1, input2, input3, input4, input5, input6);
    }

    public void testCombineNegative() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
         String inputLogic =
         "<combine name=\"test\">"+
                    "<data-in dataset=\"A\" />"+
                    "<data-in dataset=\"B\" />"+
         "</combine>";
         //@formatter:on
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        final String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
        createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));

        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        waitFor(5 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                        CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
                return !actionBean.getStatus().equals(CoordinatorAction.Status.WAITING);
            }
        });

        CoordinatorAction actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION,
                jobId + "@1");
        assertEquals(actionBean.getStatus(), CoordinatorAction.Status.WAITING);

    }

    public void testSingeSetWithMin() throws Exception {
        Configuration conf = getConf();
        //@formatter:off
        String inputLogic =
        "<or name=\"test\">"+
                     "<data-in dataset=\"A\" min=\"3\" />"+
        "</or>";
        //@formatter:on

        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        // dataset with gap
        String input3 = createTestCaseSubDir("input-data/a/2014/10/07/19/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 3);
        checkDataSets(dataSets, input1, input2, input3);
    }

    public void testCombineWithMin() throws Exception {
        Configuration conf = getConf();
        String inputLogic =
        //@formatter:off
        "<combine name=\"test\" min=\"4\">"+
                   "<data-in dataset=\"A\" />"+
                   "<data-in dataset=\"B\" />"+
        "</combine>";
        //@formatter:on
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        final String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
        String input2 = createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
        String input3 = createTestCaseSubDir("input-data/a/2014/10/07/22/_SUCCESS".split("/"));
        String input4 = createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
        String input5 = createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 5);
        checkDataSets(dataSets, input1, input2, input3, input4, input5);

    }

    public void testMinWait() throws Exception {
        Configuration conf = getConf();
        Date now = new Date();
        String inputLogic =
        //@formatter:off
        "<combine name=\"test\" min= \"4\" wait=\"1\">"+
                   "<data-in dataset=\"A\" />"+
                   "<data-in dataset=\"B\" />"+
        "</combine>";
        //@formatter:on
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 3 * 60 * 60 * 1000)));
        // 5 hour before
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));
        // 5 hour before
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        List<String> inputDir = createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4);

        startCoordActionForWaiting(jobId);
        // wait for 1 min
        sleep(60 * 1000);
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 5);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));
    }

    public void testWait() throws Exception {
        Configuration conf = getConf();
        Date now = new Date();
        String inputLogic =
        //@formatter:off
        "<combine name=\"test\" wait=\"1\">"+
                   "<data-in dataset=\"A\" />"+
                   "<data-in dataset=\"B\" />"+
        "</combine>";
        //@formatter:on
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 3 * 60 * 60 * 1000)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        List<String> inputDir = createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4);

        startCoordActionForWaiting(jobId);
        // wait for 1 min
        sleep(60 * 1000);

        inputDir.addAll(createDirWithTime("input-data/b/", now, 5));
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 6);
        checkDataSets(dataSets, inputDir.toArray(new String[inputDir.size()]));
    }

    public void testWaitFail() throws Exception {
        Configuration conf = getConf();
        Date now = new Date();
        String inputLogic =
        //@formatter:off
                "<or name=\"test\" min=\"${min}\" wait=\"${wait}\">"+
                           "<data-in dataset=\"${dataA}\" />"+
                           "<data-in dataset=\"${dataB}\" />"+
                "</or>";
        //@formatter:on
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("min", "4");
        conf.set("wait", "180");
        conf.set("dataA", "A");
        conf.set("dataB", "B");
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 3 * 60 * 60 * 1000)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));

        String jobId = _testCoordSubmit("coord-inputlogic-range.xml", conf, inputLogic, getInputEventForRange());

        createDirWithTime("input-data/b/", now, 0, 1, 2, 3, 4);

        startCoordActionForWaiting(jobId);
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertTrue(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
    }

    public void testLatest() throws Exception {

        Configuration conf = getConf();
        conf.set("initial_instance_a", "2014-10-07T00:00Z");
        conf.set("initial_instance_b", "2014-10-07T00:00Z");

        String inputLogic = "<data-in name=\"test\" dataset=\"A\"/>";
        String jobId = _testCoordSubmit("coord-inputlogic-latest.xml", conf, inputLogic);

        String input1 = createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 1);
        checkDataSets(dataSets, input1);

    }

    public void testLatestRange() throws Exception {

        Configuration conf = getConf();
        Date now = new Date();
        conf.set("start_time", DateUtils.formatDateOozieTZ(now));
        conf.set("end_time", DateUtils.formatDateOozieTZ(new Date(now.getTime() + 3 * 60 * 60 * 1000)));
        conf.set("initial_instance_a", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));
        conf.set("initial_instance_b", DateUtils.formatDateOozieTZ(new Date(now.getTime() - 5 * 60 * 60 * 1000)));

        String inputLogic =
        //@formatter:off
              "<data-in name=\"test\" dataset=\"A\" min =\"2\" />";
        //@formatter:on
        String jobId = _testCoordSubmit("coord-inputlogic-range-latest.xml", conf, inputLogic);

        createDirWithTime("input-data/a/", now, 0, 1);

        startCoordAction(jobId);

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");

        assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
        XConfiguration runConf = new XConfiguration(new StringReader(actionBean.getRunConf()));
        String dataSets = runConf.get("inputLogicData");
        assertEquals(dataSets.split(",").length, 2);

    }

     //TODO combine support for unresolved
     // public void testLatestWithCombine() throws Exception {
     // Configuration conf = getConf();
     // conf.set("input_check", "combine(\"A\", \"B\")");
     // conf.set("initial_instance_a", "2014-10-07T00:00Z");
     // conf.set("initial_instance_b", "2014-10-07T00:00Z");
     //
     // String jobId = _testCoordSubmit("coord-inputlogic-range-latest.xml", conf);
     //
     // new CoordMaterializeTransitionXCommand(jobId, 3600).call();
     // CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
     // CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
     // sleep(2000);
     //
     // new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
     // assertEquals(actionBean.getStatus(), CoordinatorAction.Status.WAITING);
     // createTestCaseSubDir("input-data/a/2014/10/08/00/_SUCCESS".split("/"));
     // createTestCaseSubDir("input-data/a/2014/10/07/23/_SUCCESS".split("/"));
     // createTestCaseSubDir("input-data/a/2014/10/07/22/_SUCCESS".split("/"));
     // createTestCaseSubDir("input-data/b/2014/10/07/21/_SUCCESS".split("/"));
     //
     // new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
     //
     // actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
     // assertEquals(actionBean.getStatus(), CoordinatorAction.Status.WAITING);
     //
     // createTestCaseSubDir("input-data/b/2014/10/07/20/_SUCCESS".split("/"));
     // new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
     //
     // actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
     // assertFalse(CoordinatorAction.Status.WAITING.equals(actionBean.getStatus()));
     //
     // }
    public void testCoordWithoutInputCheck() throws Exception {
        Configuration conf = new XConfiguration();
        String jobId = setupCoord(conf, "coord-multiple-input-instance3.xml");
        sleep(1000);
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();
        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();

        CoordinatorAction actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION,
                jobId + "@1");

        assertEquals(actionBean.getMissingDependencies(), "!!${coord:latest(0)}#${coord:latest(-1)}");

    }

    private String _testCoordSubmit(String coordinatorXml, Configuration conf, String inputLogic) throws Exception {
        return _testCoordSubmit(coordinatorXml, conf, inputLogic, "", false);
    }

    private String _testCoordSubmit(String coordinatorXml, Configuration conf, String inputLogic, String inputEvent)
            throws Exception {
        return _testCoordSubmit(coordinatorXml, conf, inputLogic, inputEvent, false);
    }

    private String _testCoordSubmit(String coordinatorXml, Configuration conf, String inputLogic, String inputEvent,
            boolean dryRun) throws Exception {
        String appPath = "file://" + getTestCaseDir() + File.separator + "coordinator.xml";

        String content = IOUtils.getResourceAsString(coordinatorXml, -1);
        content = content.replace("=input-logic=", inputLogic);
        content = content.replace("=input-events=", inputEvent);

        Writer writer = new FileWriter(new URI(appPath).getPath());
        IOUtils.copyCharStream(new StringReader(content), writer);
        conf.set(OozieClient.COORDINATOR_APP_PATH, appPath);
        conf.set(OozieClient.USER_NAME, getTestUser());
        conf.set("nameNode", "hdfs://localhost:9000");
        conf.set("queueName", "default");
        conf.set("jobTracker", "localhost:9001");
        conf.set("examplesRoot", "examples");

        return new CoordSubmitXCommand(dryRun, conf).call();
    }

    private Configuration getConf() throws Exception {
        Configuration conf = new XConfiguration();
        conf.set("data_set_a", "file://" + getTestCaseDir() + "/input-data/a");
        conf.set("data_set_b", "file://" + getTestCaseDir() + "/input-data/b");
        conf.set("data_set_c", "file://" + getTestCaseDir() + "/input-data/c");
        conf.set("data_set_d", "file://" + getTestCaseDir() + "/input-data/d");
        conf.set("data_set_e", "file://" + getTestCaseDir() + "/input-data/e");
        conf.set("data_set_f", "file://" + getTestCaseDir() + "/input-data/f");
        conf.set("partitionName", "test");

        conf.set("start_time", "2014-10-08T00:00Z");
        conf.set("end_time", "2015-10-08T00:00Z");
        conf.set("initial_instance_a", "2014-10-08T00:00Z");
        conf.set("initial_instance_b", "2014-10-08T00:00Z");
        conf.set("wfPath", getWFPath());
        return conf;

    }

    public String getWFPath() throws Exception {
        String workflowUri = getTestCaseFileUri("workflow.xml");
        String appXml = "<workflow-app xmlns='uri:oozie:workflow:0.1' name='map-reduce-wf'> " + "<start to='end' /> "
                + "<end name='end' /> " + "</workflow-app>";

        writeToFile(appXml, workflowUri);
        return workflowUri;

    }

    private void writeToFile(String appXml, String appPath) throws IOException {
        File wf = new File(URI.create(appPath));
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(wf));
            out.println(appXml);
        }
        catch (IOException iex) {
            throw iex;
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void checkDataSets(String dataSets, String... values) {

        Set<String> inputDataSets = new HashSet<String>();
        for (String dataSet : dataSets.split(",")) {
            inputDataSets.add(dataSet.substring(dataSet.indexOf(getTestCaseDir())));
        }

        for (String value : values) {
            assertTrue(inputDataSets.contains(value.replace("/_SUCCESS","")));
        }
    }

    public void checkDataSetsForFalse(String dataSets, String... values) {

        Set<String> inputDataSets = new HashSet<String>();
        for (String dataSet : dataSets.split(",")) {
            inputDataSets.add(dataSet.substring(dataSet.indexOf(getTestCaseDir())));
        }

        for (String value : values) {
            assertFalse(inputDataSets.contains(value));
        }

    }

    private void startCoordAction(final String jobId) throws CommandException, JPAExecutorException {
        startCoordAction(jobId, CoordinatorAction.Status.WAITING);

    }

    private void startCoordAction(final String jobId, final CoordinatorAction.Status coordActionStatus)
            throws CommandException, JPAExecutorException {
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        waitFor(50 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                        CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
                return !actionBean.getStatus().equals(CoordinatorAction.Status.WAITING);
            }
        });

        CoordinatorAction actionBean = CoordActionQueryExecutor.getInstance().get(CoordActionQuery.GET_COORD_ACTION,
                jobId + "@1");
        assertFalse(actionBean.getStatus().equals(coordActionStatus));

        CoordinatorJob coordJob = CoordJobQueryExecutor.getInstance().get(CoordJobQuery.GET_COORD_JOB, jobId);

        new CoordActionStartXCommand(actionBean.getId(), coordJob.getUser(), coordJob.getAppName(),
                actionBean.getJobId()).call();
    }

    private void startCoordActionForWaiting(final String jobId) throws CommandException, JPAExecutorException,
            JDOMException {
        new CoordMaterializeTransitionXCommand(jobId, 3600).call();

        new CoordActionInputCheckXCommand(jobId + "@1", jobId).call();
        waitFor(5 * 1000, new Predicate() {
            public boolean evaluate() throws Exception {
                CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                        CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
                return !actionBean.getStatus().equals(CoordinatorAction.Status.WAITING);
            }
        });

        CoordinatorActionBean actionBean = CoordActionQueryExecutor.getInstance().get(
                CoordActionQuery.GET_COORD_ACTION, jobId + "@1");
        assertTrue("should be waiting", actionBean.getStatus().equals(CoordinatorAction.Status.WAITING));
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

    private String getInputEventForRange() {
        //@formatter:off
        return
                "<data-in name=\"A\" dataset=\"a\">" +
                    "<start-instance>${coord:current(-5)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>" +
                "<data-in name=\"B\" dataset=\"b\">" +
                    "<start-instance>${coord:current(-5)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>" +
                "<data-in name=\"C\" dataset=\"c\">" +
                    "<start-instance>${coord:current(-5)}</start-instance> " +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>" +
                "<data-in name=\"D\" dataset=\"d\">" +
                    "<start-instance>${coord:current(-5)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>" +
                "<data-in name=\"E\" dataset=\"e\">" +
                    "<start-instance>${coord:current(-5)}</start-instance>" +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>" +
                "<data-in name=\"F\" dataset=\"f\">" +
                    "<start-instance>${coord:current(-5)}</start-instance> " +
                    "<end-instance>${coord:current(0)}</end-instance>" +
                "</data-in>";
        //@formatter:on
    }

    public List<String> createDirWithTime(String dirPrefix, Date date, int... hours) {

        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd/HH");

        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        sd.setTimeZone(tzUTC);
        List<String> createdDirPath = new ArrayList<String>();

        for (int hour : hours) {
            createdDirPath.add(createTestCaseSubDir((dirPrefix
                    + sd.format(new Date(date.getTime() - hour * 60 * 60 * 1000)) + "/_SUCCESS").split("/")));
        }
        return createdDirPath;
    }

}
diff --git a/core/src/test/java/org/apache/oozie/coord/input/logic/TestInputLogicParser.java b/core/src/test/java/org/apache/oozie/coord/input/logic/TestInputLogicParser.java
new file mode 100644
index 000000000..622e57ffd
-- /dev/null
++ b/core/src/test/java/org/apache/oozie/coord/input/logic/TestInputLogicParser.java
@@ -0,0 +1,367 @@
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

package org.apache.oozie.coord.input.logic;


import junit.framework.TestCase;

import org.apache.oozie.coord.input.logic.InputLogicParser;
import org.apache.oozie.util.XmlUtils;
import org.jdom.Element;
import org.jdom.JDOMException;

public class TestInputLogicParser extends TestCase {

    public void testAndOr() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<and>" +
                            "<or>" +
                                "<data-in dataset=\"A\"/> " +
                                "<data-in dataset=\"B\"/> " +
                            "</or>" +
                            "<or>" +
                                "<data-in dataset=\"C\"/>" +
                                "<data-in dataset=\"D\"/>" +
                            "</or>" +
                        "</and>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("((dependencyBuilder.input(\"A\").build() || dependencyBuilder.input(\"B\").build()) && "
                + "(dependencyBuilder.input(\"C\").build() || dependencyBuilder.input(\"D\").build()))",
                inputLogicParser.parse(root));

    }

    public void testAnd() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<and>" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                        "</and>" +
                "</input-logic>";
      //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"A\").build() && dependencyBuilder.input(\"B\").build())",
                inputLogicParser.parse(root));

    }

    public void testOr() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<or>" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                        "</or>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"A\").build() || dependencyBuilder.input(\"B\").build())",
                inputLogicParser.parse(root));

    }

    public void testOrWithMin() throws JDOMException {
        //@formatter:off
        String xml = "<input-logic>" + "<or>" + "<data-in dataset=\"A\" min=\"3\"/> " + "<data-in dataset=\"B\"/>" + "</or>"
                + "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"A\").min(3).build() || dependencyBuilder.input(\"B\").build())",
                inputLogicParser.parse(root));
    }

    public void testOrWithMinAtOr() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<or min=\"10\">" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                        "</or>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals(
                "(dependencyBuilder.input(\"A\").min(10).build() || dependencyBuilder.input(\"B\").min(10).build())",
                inputLogicParser.parse(root));
    }

    public void testWithName() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<or name =\"test\" min=\"10\">" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                        "</or>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals(
                "(dependencyBuilder.input(\"A\").min(10).build() || dependencyBuilder.input(\"B\").min(10).build())",
                inputLogicParser.parseWithName(root, "test"));
    }

    public void testCombine() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                      "<combine name =\"test\" min=\"10\">" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                      "</combine>" +
                "</input-logic>";

        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.combine(\"A\",\"B\").min(10).build())",
                inputLogicParser.parseWithName(root, "test"));
    }

    public void testWithNameNested() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<and>" +
                            "<or>" +
                                "<data-in dataset=\"A\"/> " +
                                "<data-in dataset=\"B\"/> " +
                            "</or>" +
                            "<or name=\"test\">" +
                                "<data-in dataset=\"C\"/>" +
                                "<data-in dataset=\"D\"/>" +
                            "</or>" +
                        "</and>" +
                 "</input-logic>";
      //@formatter:on

        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"C\").build() || dependencyBuilder.input(\"D\").build())",
                inputLogicParser.parseWithName(root, "test"));

    }

    public void testDepth2() throws JDOMException {
        //@formatter:off
     String xml =
             "<input-logic>" +
                     "<and>" +
                         "<and>" +
                             "<or>" +
                                 "<data-in dataset=\"A\"/>" +
                                 "<data-in dataset=\"B\"/>" +
                             "</or>" +
                             "<or>" +
                                 "<data-in dataset=\"C\"/>" +
                                 "<data-in dataset=\"D\"/>" +
                             "</or>" +
                      "</and>" +
                      "<and>" +
                          "<data-in dataset=\"E\"/>" +
                          "<data-in dataset=\"F\"/>" +
                      "</and>" +
                   "</and>" +
         "</input-logic>";
     //@formatter:on

        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(((dependencyBuilder.input(\"A\").build() || dependencyBuilder.input(\"B\").build())"
                + " && (dependencyBuilder.input(\"C\").build() || dependencyBuilder.input(\"D\").build()))"
                + " && (dependencyBuilder.input(\"E\").build() && dependencyBuilder.input(\"F\").build()))",
                inputLogicParser.parse(root));

    }

    public void testDepth2WithCombine() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<and>" +
                            "<and>" +
                                "<combine>" +
                                    "<data-in dataset=\"A\" />" +
                                    "<data-in dataset=\"B\" />" +
                                "</combine>" +
                                "<or>" +
                                    "<data-in dataset=\"C\" />" +
                                    "<data-in dataset=\"D\" />" +
                                "</or>" +
                           "</and>" +
                           "<combine>" +
                               "<data-in dataset=\"E\" />" +
                               "<data-in dataset=\"F\" />" +
                           "</combine>" +
                       "</and>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(((dependencyBuilder.combine(\"A\",\"B\").build()) && (dependencyBuilder.input(\"C\").build()"
                + " || dependencyBuilder.input(\"D\").build())) && (dependencyBuilder.combine(\"E\",\"F\").build()))",
                inputLogicParser.parse(root));
    }

    public void testAndCombine() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                        "<and>" +
                            "<combine>" +
                                "<data-in dataset=\"A\" />" +
                                "<data-in dataset=\"B\" />"+
                            "</combine>" +
                            "<combine>" +
                                "<data-in dataset=\"C\" />" +
                                "<data-in dataset=\"D\" />" +
                            "</combine>" +
                         "</and>" +
                 "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals(
                "((dependencyBuilder.combine(\"A\",\"B\").build()) && (dependencyBuilder.combine(\"C\",\"D\").build()))",
                inputLogicParser.parse(root));
    }

    public void testComplex1() throws JDOMException {
        //@formatter:off
        String xml=
            "<input-logic>"+
                "<and name=\"test\">"+
                    "<or>"+
                        "<and>" +
                            "<data-in dataset=\"A\" />"+
                            "<data-in dataset=\"B\" />"+
                        "</and>" +
                        "<and>"+
                            "<data-in dataset=\"C\" />"+
                            "<data-in dataset=\"D\" />"+
                        "</and>"+
                    "</or>"+
                  "<and>"+
                     "<data-in dataset=\"A\" />"+
                     "<data-in dataset=\"B\" />"+
                 "</and>"+
            "</and>"+
        "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(((dependencyBuilder.input(\"A\").build() && dependencyBuilder.input(\"B\").build())"
                + " || (dependencyBuilder.input(\"C\").build() && dependencyBuilder.input(\"D\").build()))"
                + " && (dependencyBuilder.input(\"A\").build() && dependencyBuilder.input(\"B\").build()))",
                inputLogicParser.parse(root));
    }

    public void testAllAnd() throws JDOMException {
        //@formatter:off
        String xml=
            "<input-logic>"+
                "<and name=\"test\">"+
                     "<data-in dataset=\"A\" />"+
                     "<data-in dataset=\"B\" />"+
                     "<data-in dataset=\"C\" />"+
                     "<data-in dataset=\"D\" />"+
                     "<data-in dataset=\"E\" />"+
                     "<data-in dataset=\"F\" />"+
            "</and>"+
        "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"A\").build() && dependencyBuilder.input(\"B\").build() && "
                + "dependencyBuilder.input(\"C\").build() && dependencyBuilder.input(\"D\").build() && "
                + "dependencyBuilder.input(\"E\").build() && dependencyBuilder.input(\"F\").build())",
                inputLogicParser.parse(root));
    }

    public void testDataIn() throws JDOMException {
        //@formatter:off
        String xml=
            "<input-logic>"+
                  "<data-in dataset=\"A\" />"+
            "</input-logic>";
        //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("dependencyBuilder.input(\"A\").build()", inputLogicParser.parse(root));
    }

    public void testMinWait() throws JDOMException {
        //@formatter:off
        String xml =
        "<input-logic>" +
             "<and name=\"test\" min=\"3\" wait=\"10\">" +
                  "<data-in dataset=\"A\"/> " +
                  "<data-in dataset=\"B\"/>" +
             "</and>" +
        "</input-logic>";
       //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals("(dependencyBuilder.input(\"A\").min(3).inputWait(10).build() "
                + "&& dependencyBuilder.input(\"B\").min(3).inputWait(10).build())",
                inputLogicParser.parseWithName(root, "test"));

        assertEquals("(dependencyBuilder.input(\"A\").min(3).inputWait(10).build() "
                + "&& dependencyBuilder.input(\"B\").min(3).inputWait(10).build())", inputLogicParser.parse(root));
    }

    public void testOrAndDataIn() throws JDOMException {
        //@formatter:off
        String xml =
                "<input-logic>" +
                       "<or>" +
                        "<and>" +
                            "<data-in dataset=\"A\"/> " +
                            "<data-in dataset=\"B\"/>" +
                        "</and>" +
                        "<data-in dataset=\"C\"/>" +
                        "</or>"+
                "</input-logic>";
      //@formatter:on
        Element root = XmlUtils.parseXml(xml);
        InputLogicParser inputLogicParser = new InputLogicParser();
        assertEquals(
                "((dependencyBuilder.input(\"A\").build() && dependencyBuilder.input(\"B\").build()) || "
                + "dependencyBuilder.input(\"C\").build())",
                inputLogicParser.parse(root));

    }



}
diff --git a/core/src/test/resources/coord-action-sla.xml b/core/src/test/resources/coord-action-sla.xml
index f3f1bc09c..7df8c803a 100644
-- a/core/src/test/resources/coord-action-sla.xml
++ b/core/src/test/resources/coord-action-sla.xml
@@ -23,7 +23,7 @@
 
     <datasets>
      <dataset name="a" frequency="7" initial-instance="2009-01-01T01:00Z" timezone="UTC">
        <uri-template>file://#testDir/${YEAR}/${DAY}</uri-template>
        <uri-template>file:///testDir/${YEAR}/${DAY}</uri-template>
      </dataset>
     </datasets>
     <input-events>
diff --git a/core/src/test/resources/coord-inputlogic-combine.xml b/core/src/test/resources/coord-inputlogic-combine.xml
new file mode 100644
index 000000000..2cd4bd5b3
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic-combine.xml
@@ -0,0 +1,119 @@
<!-- /**
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
 */ -->

<coordinator-app name="aggregator-coord" frequency="${coord:days(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_a}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:days(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_b}/${YEAR}/${MONTH}/${DAY}
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_c}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="d" frequency="${coord:days(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_d}/${YEAR}/${MONTH}/${DAY}
            </uri-template>
        </dataset>
        <dataset name="e" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_e}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="f" frequency="${coord:days(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_f}/${YEAR}/${MONTH}/${DAY}
            </uri-template>
        </dataset>

        <dataset name="aggregated-logs" frequency="${coord:days(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        <data-in name="A" dataset="a">
            =data-in-param-1=
        </data-in>
        <data-in name="B" dataset="b">
            =data-in-param-2=
        </data-in>
        <data-in name="C" dataset="c">
            =data-in-param-3=
        </data-in>
        <data-in name="D" dataset="d">
            =data-in-param-4=
        </data-in>
        <data-in name="E" dataset="e">
            =data-in-param-5=
        </data-in>
        <data-in name="F" dataset="f">
            =data-in-param-6=
        </data-in>
    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>
    <action>
        <workflow>
            <app-path>hdfs:///tmp/workflows</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/core/src/test/resources/coord-inputlogic-hcat.xml b/core/src/test/resources/coord-inputlogic-hcat.xml
new file mode 100644
index 000000000..ff260d3d7
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic-hcat.xml
@@ -0,0 +1,119 @@
<!-- /**
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
 */ -->
 <coordinator-app name="aggregator-coord" frequency="${coord:days(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_a}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_b}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_c}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="d" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_d}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="e" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_e}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>
        <dataset name="f" frequency="${coord:days(1)}"
            initial-instance="${initial_instance}" timezone="UTC">
            <uri-template>${data_set}/${db_f}/${table}/dt=${YEAR}${MONTH}${DAY};country=usa
            </uri-template>
        </dataset>

        <dataset name="aggregated-logs" frequency="${coord:days(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        <data-in name="A" dataset="a">
            =data-in-param-1=
        </data-in>
        <data-in name="B" dataset="b">
            =data-in-param-2=
        </data-in>
        <data-in name="C" dataset="c">
            =data-in-param-3=
        </data-in>
        <data-in name="D" dataset="d">
            =data-in-param-4=
        </data-in>
        <data-in name="E" dataset="e">
            =data-in-param-5=
        </data-in>
        <data-in name="F" dataset="f">
            =data-in-param-6=
        </data-in>
    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>
    <action>
        <workflow>
            <app-path>hdfs:///tmp/workflows</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>

            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/core/src/test/resources/coord-inputlogic-latest.xml b/core/src/test/resources/coord-inputlogic-latest.xml
new file mode 100644
index 000000000..576f00dfc
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic-latest.xml
@@ -0,0 +1,124 @@
<!-- /**
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
 */ -->

<coordinator-app name="aggregator-coord" frequency="${coord:hours(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_a}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_b}" timezone="UTC">
            <uri-template>${data_set_b}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_c}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="d" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_d}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="e" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_e}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="f" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_f}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>


        <dataset name="aggregated-logs" frequency="${coord:hours(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        <data-in name="A" dataset="a">
            <instance>${coord:latest(0)}</instance>
        </data-in>
        <data-in name="B" dataset="b">
            <instance>${coord:latest(0)}</instance>
        </data-in>
        <data-in name="C" dataset="c">
            <instance>${coord:latest(0)}</instance>
        </data-in>
        <data-in name="D" dataset="d">
            <instance>${coord:latest(0)}</instance>
        </data-in>
        <data-in name="E" dataset="e">
            <instance>${coord:latest(0)}</instance>
        </data-in>
        <data-in name="F" dataset="f">
            <instance>${coord:latest(0)}</instance>
        </data-in>
    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>

    <action>
        <workflow>
            <app-path>${wfPath}</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/core/src/test/resources/coord-inputlogic-range-latest.xml b/core/src/test/resources/coord-inputlogic-range-latest.xml
new file mode 100644
index 000000000..f9d79c27d
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic-range-latest.xml
@@ -0,0 +1,130 @@
<!-- /**
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
 */ -->

<coordinator-app name="aggregator-coord" frequency="${coord:hours(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_a}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_b}" timezone="UTC">
            <uri-template>${data_set_b}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_c}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="d" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_d}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="e" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_e}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="f" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_f}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>


        <dataset name="aggregated-logs" frequency="${coord:hours(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        <data-in name="A" dataset="a">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
        <data-in name="B" dataset="b">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
        <data-in name="C" dataset="c">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
        <data-in name="D" dataset="d">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
        <data-in name="E" dataset="e">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
        <data-in name="F" dataset="f">
            <start-instance>${coord:latest(-5)}</start-instance>
            <end-instance>${coord:latest(0)}</end-instance>
        </data-in>
    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>

    <action>
        <workflow>
            <app-path>${wfPath}</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/core/src/test/resources/coord-inputlogic-range.xml b/core/src/test/resources/coord-inputlogic-range.xml
new file mode 100644
index 000000000..d6581e7dc
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic-range.xml
@@ -0,0 +1,107 @@
<!-- /**
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
 */ -->

<coordinator-app name="aggregator-coord" frequency="${coord:hours(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_a}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_b}" timezone="UTC">
            <uri-template>${data_set_b}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_c}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="d" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_d}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="e" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_e}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="f" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_f}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>


        <dataset name="aggregated-logs" frequency="${coord:hours(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        =input-events=
    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>

    <action>
        <workflow>
            <app-path>${wfPath}</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/core/src/test/resources/coord-inputlogic.xml b/core/src/test/resources/coord-inputlogic.xml
new file mode 100644
index 000000000..51b67ac55
-- /dev/null
++ b/core/src/test/resources/coord-inputlogic.xml
@@ -0,0 +1,126 @@
<!-- /**
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
 */ -->

<coordinator-app name="aggregator-coord" frequency="${coord:hours(1)}"
    start="${start_time}" end="${end_time}" timezone="UTC"
    xmlns="uri:oozie:coordinator:0.5">
    <controls>
        <concurrency>1</concurrency>
        <throttle>1</throttle>
    </controls>

    <datasets>
        <dataset name="a" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_a}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="b" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_b}" timezone="UTC">
            <uri-template>${data_set_b}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
        <dataset name="c" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_c}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="d" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_d}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="e" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_e}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>

        <dataset name="f" frequency="${coord:hours(1)}"
            initial-instance="${initial_instance_a}" timezone="UTC">
            <uri-template>${data_set_f}/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>


        <dataset name="aggregated-logs" frequency="${coord:hours(10)}"
            initial-instance="2014-10-08T00:00Z" timezone="UTC">
            <uri-template>file:///output-data/aggregator/aggregatedLogs/${YEAR}/${MONTH}/${DAY}/${HOUR}
            </uri-template>
        </dataset>
    </datasets>

    <input-events>
        <data-in name="A" dataset="a">
            <instance>${coord:current(0)}</instance>
        </data-in>
        <data-in name="B" dataset="b">
            <instance>${coord:current(0)}</instance>
        </data-in>
        <data-in name="C" dataset="c">
            <instance>${coord:current(0)}</instance>
        </data-in>
        <data-in name="D" dataset="d">
            <instance>${coord:current(0)}</instance>
        </data-in>
        <data-in name="E" dataset="e">
            <instance>${coord:current(0)}</instance>
        </data-in>
        <data-in name="F" dataset="f">
            <instance>${coord:current(0)}</instance>
        </data-in>


    </input-events>

    <input-logic>
        =input-logic=
    </input-logic>

    <output-events>
        <data-out name="output" dataset="aggregated-logs">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>

    <action>
        <workflow>
            <app-path>${wfPath}</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>inputLogicData</name>
                    <value>${coord:dataIn(partitionName)}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
diff --git a/pom.xml b/pom.xml
index dc519cb97..26f10a3ef 100644
-- a/pom.xml
++ b/pom.xml
@@ -1266,6 +1266,13 @@
                 <scope>compile</scope>
             </dependency>
 
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-jexl</artifactId>
                <version>2.1.1</version>
                <scope>compile</scope>
            </dependency>

             <dependency>
                 <groupId>javax.mail</groupId>
                 <artifactId>mail</artifactId>
diff --git a/release-log.txt b/release-log.txt
index 5c2ee5b6b..9639b0c3d 100644
-- a/release-log.txt
++ b/release-log.txt
@@ -1,5 +1,6 @@
 -- Oozie 4.3.0 release (trunk - unreleased)
 
OOZIE-1976 Specifying coordinator input datasets in more logical ways (puru)
 OOZIE-2444 Need conditional logic in bundles (satishsaley via puru)
 OOZIE-2394 Oozie can execute command without holding lock (puru)
 OOZIE-1922 MemoryLocksService fails if lock is acquired multiple times in same thread and released (puru)
- 
2.19.1.windows.1

