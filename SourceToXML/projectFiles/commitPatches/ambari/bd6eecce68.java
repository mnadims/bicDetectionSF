From bd6eecce681c9bce75d76e3d589f56bccb7ef8db Mon Sep 17 00:00:00 2001
From: Jayush Luniya <jluniya@hortonworks.com>
Date: Fri, 6 May 2016 15:59:28 -0700
Subject: [PATCH] AMBARI-16293: Spark service fails to start (jluniya)

--
 .../SPARK/{1.2.0.2.2 => 1.2.1}/alerts.json    |  0
 .../configuration/spark-defaults.xml          |  0
 .../configuration/spark-env.xml               |  0
 .../configuration/spark-log4j-properties.xml  |  0
 .../spark-metrics-properties.xml              |  0
 .../SPARK/{1.2.0.2.2 => 1.2.1}/kerberos.json  |  0
 .../SPARK/{1.2.0.2.2 => 1.2.1}/metainfo.xml   | 33 +++++++++++++-
 .../package/scripts/job_history_server.py     |  0
 .../package/scripts/params.py                 |  0
 .../package/scripts/service_check.py          |  0
 .../package/scripts/setup_spark.py            |  0
 .../package/scripts/spark_client.py           |  0
 .../package/scripts/spark_service.py          |  0
 .../package/scripts/spark_thrift_server.py    |  0
 .../package/scripts/status_params.py          |  0
 .../SPARK/1.2.1}/quicklinks/quicklinks.json   |  0
 .../SPARK/{1.3.1.2.3 => 1.3.1}/metainfo.xml   |  4 +-
 .../SPARK/{1.4.1.2.3 => 1.4.1}/kerberos.json  |  0
 .../SPARK/{1.4.1.2.3 => 1.4.1}/metainfo.xml   |  4 +-
 .../spark-hive-site-override.xml              |  0
 .../configuration/spark-thrift-sparkconf.xml  |  0
 .../common-services/SPARK/1.5.2/metainfo.xml  | 42 ++++++++++++++++++
 .../1.6.0}/configuration/spark-defaults.xml   |  0
 .../spark-thrift-fairscheduler.xml            |  0
 .../configuration/spark-thrift-sparkconf.xml  |  0
 .../common-services/SPARK/1.6.0/metainfo.xml  | 44 +++++++++++++++++++
 .../HDP/2.2/services/SPARK/metainfo.xml       | 32 +-------------
 .../HDP/2.3/services/SPARK/metainfo.xml       | 20 +--------
 .../services/SPARK/quicklinks/quicklinks.json | 27 ------------
 .../HDP/2.4/services/SPARK/metainfo.xml       | 15 +------
 .../HDP/2.5/services/SPARK/metainfo.xml       |  2 +-
 .../server/stack/KerberosDescriptorTest.java  |  2 +-
 .../2.2/SPARK/test_job_history_server.py      |  2 +-
 .../stacks/2.2/SPARK/test_spark_client.py     |  2 +-
 .../2.2/SPARK/test_spark_service_check.py     |  4 +-
 .../2.2/configs/spark-job-history-server.json |  2 +-
 .../2.3/SPARK/test_spark_thrift_server.py     |  2 +-
 37 files changed, 133 insertions(+), 104 deletions(-)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/alerts.json (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/configuration/spark-defaults.xml (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/configuration/spark-env.xml (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/configuration/spark-log4j-properties.xml (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/configuration/spark-metrics-properties.xml (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/kerberos.json (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/metainfo.xml (84%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/job_history_server.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/params.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/service_check.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/setup_spark.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/spark_client.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/spark_service.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/spark_thrift_server.py (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.2.0.2.2 => 1.2.1}/package/scripts/status_params.py (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.2/services/SPARK => common-services/SPARK/1.2.1}/quicklinks/quicklinks.json (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.3.1.2.3 => 1.3.1}/metainfo.xml (98%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.4.1.2.3 => 1.4.1}/kerberos.json (100%)
 rename ambari-server/src/main/resources/common-services/SPARK/{1.4.1.2.3 => 1.4.1}/metainfo.xml (97%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/SPARK => common-services/SPARK/1.5.2}/configuration/spark-hive-site-override.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/SPARK => common-services/SPARK/1.5.2}/configuration/spark-thrift-sparkconf.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/SPARK/1.5.2/metainfo.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.4/services/SPARK => common-services/SPARK/1.6.0}/configuration/spark-defaults.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.4/services/SPARK => common-services/SPARK/1.6.0}/configuration/spark-thrift-fairscheduler.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.4/services/SPARK => common-services/SPARK/1.6.0}/configuration/spark-thrift-sparkconf.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/SPARK/1.6.0/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/quicklinks/quicklinks.json

diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/alerts.json b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/alerts.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/alerts.json
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/alerts.json
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-defaults.xml b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-defaults.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-defaults.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-defaults.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-env.xml b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-env.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-env.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-env.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-log4j-properties.xml b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-log4j-properties.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-log4j-properties.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-log4j-properties.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-metrics-properties.xml b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-metrics-properties.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/configuration/spark-metrics-properties.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/configuration/spark-metrics-properties.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/kerberos.json b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/kerberos.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/kerberos.json
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/kerberos.json
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/metainfo.xml b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/metainfo.xml
similarity index 84%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/metainfo.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/metainfo.xml
index 61016ab214..4cd681ac8d 100644
-- a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/metainfo.xml
++ b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/metainfo.xml
@@ -23,7 +23,7 @@
       <name>SPARK</name>
       <displayName>Spark</displayName>
       <comment>Apache Spark is a fast and general engine for large-scale data processing.</comment>
      <version>1.2.0.2.2</version>
      <version>1.2.1</version>
       <components>
         <component>
           <name>SPARK_JOBHISTORYSERVER</name>
@@ -144,6 +144,37 @@
         <service>TEZ</service>
       </requiredServices>
 
      <osSpecifics>
        <osSpecific>
          <osFamily>redhat7,amazon2015,redhat6,suse11,suse12</osFamily>
          <packages>
            <package>
              <name>spark_${stack_version}</name>
            </package>
            <package>
              <name>spark_${stack_version}-python</name>
            </package>
          </packages>
        </osSpecific>
        <osSpecific>
          <osFamily>debian7,ubuntu12,ubuntu14</osFamily>
          <packages>
            <package>
              <name>spark-${stack_version}</name>
            </package>
            <package>
              <name>spark-${stack_version}-python</name>
            </package>
          </packages>
        </osSpecific>
      </osSpecifics>
      <quickLinksConfigurations>
        <quickLinksConfiguration>
          <fileName>quicklinks.json</fileName>
          <default>true</default>
        </quickLinksConfiguration>
      </quickLinksConfigurations>

     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/job_history_server.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/job_history_server.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/job_history_server.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/job_history_server.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/params.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/params.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/params.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/params.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/service_check.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/service_check.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/service_check.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/service_check.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/setup_spark.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/setup_spark.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/setup_spark.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/setup_spark.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_client.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_client.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_client.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_client.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_service.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_service.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_service.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_service.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_thrift_server.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_thrift_server.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/spark_thrift_server.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/spark_thrift_server.py
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/status_params.py b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/status_params.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.2.0.2.2/package/scripts/status_params.py
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/package/scripts/status_params.py
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/quicklinks/quicklinks.json b/ambari-server/src/main/resources/common-services/SPARK/1.2.1/quicklinks/quicklinks.json
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/quicklinks/quicklinks.json
rename to ambari-server/src/main/resources/common-services/SPARK/1.2.1/quicklinks/quicklinks.json
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.3.1.2.3/metainfo.xml b/ambari-server/src/main/resources/common-services/SPARK/1.3.1/metainfo.xml
similarity index 98%
rename from ambari-server/src/main/resources/common-services/SPARK/1.3.1.2.3/metainfo.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.3.1/metainfo.xml
index 2869f75cae..c8e5f81ea7 100644
-- a/ambari-server/src/main/resources/common-services/SPARK/1.3.1.2.3/metainfo.xml
++ b/ambari-server/src/main/resources/common-services/SPARK/1.3.1/metainfo.xml
@@ -21,10 +21,10 @@
   <services>
     <service>
       <name>SPARK</name>
      <extends>common-services/SPARK/1.2.0.2.2</extends>
      <extends>common-services/SPARK/1.2.1</extends>
       <displayName>Spark</displayName>
       <comment>Apache Spark is a fast and general engine for large-scale data processing.</comment>
      <version>1.3.1.2.3</version>
      <version>1.3.1</version>
       <components>
         <component>
           <name>SPARK_JOBHISTORYSERVER</name>
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.4.1.2.3/kerberos.json b/ambari-server/src/main/resources/common-services/SPARK/1.4.1/kerberos.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/SPARK/1.4.1.2.3/kerberos.json
rename to ambari-server/src/main/resources/common-services/SPARK/1.4.1/kerberos.json
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.4.1.2.3/metainfo.xml b/ambari-server/src/main/resources/common-services/SPARK/1.4.1/metainfo.xml
similarity index 97%
rename from ambari-server/src/main/resources/common-services/SPARK/1.4.1.2.3/metainfo.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.4.1/metainfo.xml
index 8dcb39d216..039694a41b 100644
-- a/ambari-server/src/main/resources/common-services/SPARK/1.4.1.2.3/metainfo.xml
++ b/ambari-server/src/main/resources/common-services/SPARK/1.4.1/metainfo.xml
@@ -21,10 +21,10 @@
   <services>
     <service>
       <name>SPARK</name>
      <extends>common-services/SPARK/1.3.1.2.3</extends>
      <extends>common-services/SPARK/1.3.1</extends>
       <displayName>Spark</displayName>
       <comment>Apache Spark is a fast and general engine for large-scale data processing.</comment>
      <version>1.4.1.2.3</version>
      <version>1.4.1</version>
       <components>
         <component>
           <name>SPARK_THRIFTSERVER</name>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/configuration/spark-hive-site-override.xml b/ambari-server/src/main/resources/common-services/SPARK/1.5.2/configuration/spark-hive-site-override.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/configuration/spark-hive-site-override.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.5.2/configuration/spark-hive-site-override.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/configuration/spark-thrift-sparkconf.xml b/ambari-server/src/main/resources/common-services/SPARK/1.5.2/configuration/spark-thrift-sparkconf.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/configuration/spark-thrift-sparkconf.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.5.2/configuration/spark-thrift-sparkconf.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.5.2/metainfo.xml b/ambari-server/src/main/resources/common-services/SPARK/1.5.2/metainfo.xml
new file mode 100644
index 0000000000..139984bcdc
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SPARK/1.5.2/metainfo.xml
@@ -0,0 +1,42 @@
<?xml version="1.0"?>
<!--
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->
<metainfo>
    <schemaVersion>2.0</schemaVersion>
    <services>
        <service>
          <name>SPARK</name>
          <version>1.5.2</version>
          <extends>common-services/SPARK/1.4.1</extends>
          <requiredServices>
            <service>YARN</service>
          </requiredServices>
          <!-- No new components compared to 1.4.1 -->
          <configuration-dependencies>
            <config-type>spark-defaults</config-type>
            <config-type>spark-env</config-type>
            <config-type>spark-log4j-properties</config-type>
            <config-type>spark-metrics-properties</config-type>
            <config-type>spark-thrift-sparkconf</config-type>
            <config-type>spark-hive-site-override</config-type>
          </configuration-dependencies>
        </service>
    </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-defaults.xml b/ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-defaults.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-defaults.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-defaults.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-thrift-fairscheduler.xml b/ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-thrift-fairscheduler.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-thrift-fairscheduler.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-thrift-fairscheduler.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-thrift-sparkconf.xml b/ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-thrift-sparkconf.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/configuration/spark-thrift-sparkconf.xml
rename to ambari-server/src/main/resources/common-services/SPARK/1.6.0/configuration/spark-thrift-sparkconf.xml
diff --git a/ambari-server/src/main/resources/common-services/SPARK/1.6.0/metainfo.xml b/ambari-server/src/main/resources/common-services/SPARK/1.6.0/metainfo.xml
new file mode 100644
index 0000000000..2bd79d5a04
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/SPARK/1.6.0/metainfo.xml
@@ -0,0 +1,44 @@
<?xml version="1.0"?>
<!--
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->
<metainfo>
    <schemaVersion>2.0</schemaVersion>
    <services>
        <service>
          <name>SPARK</name>
          <extends>common-services/SPARK/1.5.2</extends>
          <version>1.6.0</version>
          <configuration-dependencies>
            <config-type>spark-defaults</config-type>
            <config-type>spark-env</config-type>
            <config-type>spark-log4j-properties</config-type>
            <config-type>spark-metrics-properties</config-type>
            <config-type>spark-thrift-sparkconf</config-type>
            <config-type>spark-hive-site-override</config-type>
            <config-type>spark-thrift-fairscheduler</config-type>
          </configuration-dependencies>
          <requiredServices>
            <service>HDFS</service>
            <service>YARN</service>
            <service>HIVE</service>
          </requiredServices>
        </service>
    </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/metainfo.xml
index c0b43c2602..8f2ca38e61 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.2/services/SPARK/metainfo.xml
@@ -23,38 +23,8 @@
   <services>
     <service>
       <name>SPARK</name>
      <extends>common-services/SPARK/1.2.0.2.2</extends>
      <extends>common-services/SPARK/1.2.1</extends>
       <version>1.2.1.2.2</version>
      <osSpecifics>
        <osSpecific>
          <osFamily>redhat7,amazon2015,redhat6,suse11,suse12</osFamily>
          <packages>
            <package>
              <name>spark_${stack_version}</name>
            </package>
            <package>
              <name>spark_${stack_version}-python</name>
            </package>
          </packages>
        </osSpecific>
        <osSpecific>
          <osFamily>debian7,ubuntu12,ubuntu14</osFamily>
          <packages>
            <package>
              <name>spark-${stack_version}</name>
            </package>
            <package>
              <name>spark-${stack_version}-python</name>
            </package>
          </packages>
        </osSpecific>
      </osSpecifics>
      <quickLinksConfigurations>
        <quickLinksConfiguration>
          <fileName>quicklinks.json</fileName>
          <default>true</default>
        </quickLinksConfiguration>
      </quickLinksConfigurations>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/metainfo.xml
index bb3b6cea8e..29272a9e68 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/metainfo.xml
@@ -24,25 +24,7 @@
         <service>
           <name>SPARK</name>
           <version>1.5.2.2.3</version>
          <extends>common-services/SPARK/1.4.1.2.3</extends>
          <requiredServices>
            <service>YARN</service>
          </requiredServices>
          <!-- No new components compared to 1.4.1 -->
          <configuration-dependencies>
            <config-type>spark-defaults</config-type>
            <config-type>spark-env</config-type>
            <config-type>spark-log4j-properties</config-type>
            <config-type>spark-metrics-properties</config-type>
            <config-type>spark-thrift-sparkconf</config-type>
            <config-type>spark-hive-site-override</config-type>
          </configuration-dependencies>
	      <quickLinksConfigurations>
	          <quickLinksConfiguration>
	          <fileName>quicklinks.json</fileName>
	          <default>true</default>
	        </quickLinksConfiguration>
	      </quickLinksConfigurations>
          <extends>common-services/SPARK/1.5.2</extends>
         </service>
     </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/quicklinks/quicklinks.json b/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/quicklinks/quicklinks.json
deleted file mode 100644
index 685665a525..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/SPARK/quicklinks/quicklinks.json
++ /dev/null
@@ -1,27 +0,0 @@
{
  "name": "default",
  "description": "default quick links configuration",
  "configuration": {
    "protocol":
    {
      "type":"HTTP_ONLY"
    },

    "links": [
      {
        "name": "spark_history_server_ui",
        "label": "Spark History Server UI",
        "requires_user_name": "false",
        "url": "%@://%@:%@",
        "port":{
          "http_property": "spark.history.ui.port",
          "http_default_port": "18080",
          "https_property": "spark.history.ui.port",
          "https_default_port": "18080",
          "regex": "^(\\d+)$",
          "site": "spark-defaults"
        }
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/metainfo.xml
index a5a19f6317..743a75a0a2 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.4/services/SPARK/metainfo.xml
@@ -23,21 +23,8 @@
     <services>
         <service>
           <name>SPARK</name>
          <extends>common-services/SPARK/1.6.0</extends>
           <version>1.6.x.2.4</version>
          <configuration-dependencies>
            <config-type>spark-defaults</config-type>
            <config-type>spark-env</config-type>
            <config-type>spark-log4j-properties</config-type>
            <config-type>spark-metrics-properties</config-type>
            <config-type>spark-thrift-sparkconf</config-type>
            <config-type>spark-hive-site-override</config-type>
            <config-type>spark-thrift-fairscheduler</config-type>
          </configuration-dependencies>
          <requiredServices>
            <service>HDFS</service>
            <service>YARN</service>
            <service>HIVE</service>
          </requiredServices>
         </service>
     </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/SPARK/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/SPARK/metainfo.xml
index 107ca93636..d1129cd983 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.5/services/SPARK/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/SPARK/metainfo.xml
@@ -23,7 +23,7 @@
   <services>
     <service>
       <name>SPARK</name>
      <version>1.6.0.2.5</version>
      <version>1.6.x.2.5</version>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
index 6bcc671b11..764118c3e1 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
@@ -151,7 +151,7 @@ public class KerberosDescriptorTest {
 
   @Test
   public void testCommonSparkServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "SPARK", "1.2.0.2.2");
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "SPARK", "1.2.1");
     Assert.notNull(descriptor);
     Assert.notNull(descriptor.getServices());
     Assert.notNull(descriptor.getService("SPARK"));
diff --git a/ambari-server/src/test/python/stacks/2.2/SPARK/test_job_history_server.py b/ambari-server/src/test/python/stacks/2.2/SPARK/test_job_history_server.py
index cac8bf7658..d7a2c84c54 100644
-- a/ambari-server/src/test/python/stacks/2.2/SPARK/test_job_history_server.py
++ b/ambari-server/src/test/python/stacks/2.2/SPARK/test_job_history_server.py
@@ -26,7 +26,7 @@ from only_for_platform import not_for_platform, PLATFORM_WINDOWS
 @not_for_platform(PLATFORM_WINDOWS)
 @patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.3.0.0-1597"))
 class TestJobHistoryServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
   STACK_VERSION = "2.2"
   DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
 
diff --git a/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_client.py b/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_client.py
index 68b0f26f9b..98f09f65d5 100644
-- a/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_client.py
++ b/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_client.py
@@ -26,7 +26,7 @@ from only_for_platform import not_for_platform, PLATFORM_WINDOWS
 @not_for_platform(PLATFORM_WINDOWS)
 @patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.3.0.0-1597"))
 class TestSparkClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
   STACK_VERSION = "2.2"
 
   def test_configure_default(self):
diff --git a/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_service_check.py b/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_service_check.py
index 0987f7c8c5..bcb0d21099 100644
-- a/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_service_check.py
++ b/ambari-server/src/test/python/stacks/2.2/SPARK/test_spark_service_check.py
@@ -25,7 +25,7 @@ from only_for_platform import not_for_platform, PLATFORM_WINDOWS
 @not_for_platform(PLATFORM_WINDOWS)
 @patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.3.0.0-1597"))
 class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
   STACK_VERSION = "2.2"
 
   def test_service_check_default(self):
@@ -60,4 +60,4 @@ class TestServiceCheck(RMFTestCase):
         try_sleep = 3,
         logoutput = True
     )
    self.assertNoMoreResources()
\ No newline at end of file
    self.assertNoMoreResources()
diff --git a/ambari-server/src/test/python/stacks/2.2/configs/spark-job-history-server.json b/ambari-server/src/test/python/stacks/2.2/configs/spark-job-history-server.json
index a187d595f8..57692442e7 100644
-- a/ambari-server/src/test/python/stacks/2.2/configs/spark-job-history-server.json
++ b/ambari-server/src/test/python/stacks/2.2/configs/spark-job-history-server.json
@@ -14,7 +14,7 @@
         "cluster-env": {}
     }, 
     "commandParams": {
        "service_package_folder": "common-services/SPARK/1.2.0.2.2/package", 
        "service_package_folder": "common-services/SPARK/1.2.1/package", 
         "script": "scripts/job_history_server.py", 
         "hooks_folder": "HDP/2.0.6/hooks", 
         "version": "2.2.2.0-2538", 
diff --git a/ambari-server/src/test/python/stacks/2.3/SPARK/test_spark_thrift_server.py b/ambari-server/src/test/python/stacks/2.3/SPARK/test_spark_thrift_server.py
index 674f30d263..c00e18be3c 100644
-- a/ambari-server/src/test/python/stacks/2.3/SPARK/test_spark_thrift_server.py
++ b/ambari-server/src/test/python/stacks/2.3/SPARK/test_spark_thrift_server.py
@@ -26,7 +26,7 @@ from only_for_platform import not_for_platform, PLATFORM_WINDOWS
 @not_for_platform(PLATFORM_WINDOWS)
 @patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.3.2.0-1597"))
 class TestSparkThriftServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
   STACK_VERSION = "2.3"
   DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
 
- 
2.19.1.windows.1

