From 8e5eeb4dea099249d616a243d96f6cb2ffcf01c8 Mon Sep 17 00:00:00 2001
From: Jayush Luniya <jluniya@hortonworks.com>
Date: Thu, 5 May 2016 23:51:52 -0700
Subject: [PATCH] AMBARI-16250: Create STORM service versions in
 common-services (jluniya)

--
 .../ambari/server/stack/ServiceModule.java    |  24 +++-
 .../ambari/server/stack/StackModule.java      |   4 +-
 .../configuration/ranger-storm-audit.xml      |   0
 .../ranger-storm-plugin-properties.xml        |   0
 .../ranger-storm-policymgr-ssl.xml            |  67 ++++++++++
 .../configuration/ranger-storm-security.xml   |  59 +++++++++
 .../configuration/storm-cluster-log4j.xml     |   0
 .../STORM/0.10.0}/configuration/storm-env.xml |   0
 .../STORM/0.10.0/configuration/storm-site.xml |  60 +++++++++
 .../configuration/storm-worker-log4j.xml      |   0
 .../common-services/STORM/0.10.0/metainfo.xml |  57 ++++++++
 .../STORM/0.10.0}/metrics.json                |   0
 .../STORM/0.10.0}/widgets.json                |   0
 .../STORM/{0.9.1.2.1 => 0.9.1}/alerts.json    |   0
 .../configuration/storm-env.xml               |   0
 .../configuration/storm-site.xml              |   0
 .../STORM/{0.9.1.2.1 => 0.9.1}/kerberos.json  |   0
 .../STORM/{0.9.1.2.1 => 0.9.1}/metainfo.xml   |   2 +-
 .../STORM/{0.9.1.2.1 => 0.9.1}/metrics.json   |   0
 .../alerts/check_supervisor_process_win.py    |   0
 .../package/files/wordCount.jar               | Bin
 .../package/scripts/drpc_server.py            |   0
 .../package/scripts/nimbus.py                 |   0
 .../package/scripts/nimbus_prod.py            |   0
 .../package/scripts/pacemaker.py              |   0
 .../package/scripts/params.py                 |   0
 .../package/scripts/params_linux.py           |   0
 .../package/scripts/params_windows.py         |   0
 .../package/scripts/rest_api.py               |   0
 .../package/scripts/service.py                |   0
 .../package/scripts/service_check.py          |   0
 .../package/scripts/setup_atlas_storm.py      |   0
 .../package/scripts/setup_ranger_storm.py     |   0
 .../package/scripts/status_params.py          |   0
 .../package/scripts/storm.py                  |   0
 .../package/scripts/storm_upgrade.py          |   0
 .../package/scripts/storm_yaml_utils.py       |   0
 .../package/scripts/supervisor.py             |   0
 .../package/scripts/supervisor_prod.py        |   0
 .../package/scripts/supervisord_service.py    |   0
 .../package/scripts/ui_server.py              |   0
 .../package/templates/client_jaas.conf.j2     |   0
 .../package/templates/config.yaml.j2          |   0
 .../templates/storm-metrics2.properties.j2    |   0
 .../package/templates/storm.conf.j2           |   0
 .../package/templates/storm_jaas.conf.j2      |   0
 .../package/templates/worker-launcher.cfg.j2  |   0
 .../quicklinks/quicklinks.json                |   0
 .../ranger-storm-plugin-properties.xml        |   0
 .../STORM/0.9.3}/configuration/storm-env.xml  |   0
 .../STORM/0.9.3/configuration/storm-site.xml  | 123 ++++++++++++++++++
 .../common-services/STORM/0.9.3/metainfo.xml  |  53 ++++++++
 .../STORM/0.9.3}/metrics.json                 |   0
 .../configuration/ranger-storm-audit.xml      |   0
 .../STORM/1.0.1/configuration/storm-site.xml  |  67 ++++++++++
 .../STORM/1.0.1}/kerberos.json                |   0
 .../common-services/STORM/1.0.1/metainfo.xml  |  28 ++++
 .../HDP/2.1/services/STORM/metainfo.xml       |   3 +-
 .../STORM/configuration/storm-site.xml        |  62 ---------
 .../HDP/2.2/services/STORM/metainfo.xml       |  25 +---
 .../ranger-storm-policymgr-ssl.xml            |  32 -----
 .../configuration/ranger-storm-security.xml   |  30 -----
 .../STORM/configuration/storm-site.xml        |  56 ++++----
 .../HDP/2.3/services/STORM/metainfo.xml       |  32 +----
 .../ranger-storm-policymgr-ssl.xml            |  35 +++++
 .../configuration/ranger-storm-security.xml   |  29 +++++
 .../STORM/configuration/storm-site.xml        |  69 +++++-----
 .../HDP/2.5/services/STORM/metainfo.xml       |   1 +
 .../HDPWIN/2.1/services/STORM/alerts.json     |   2 +-
 .../HDPWIN/2.1/services/STORM/metainfo.xml    |   2 +-
 .../server/stack/KerberosDescriptorTest.java  |   2 +-
 .../stacks/2.1/STORM/test_storm_base.py       |   2 +-
 .../stacks/2.3/STORM/test_storm_base.py       |   2 +-
 .../stacks/2.3/STORM/test_storm_upgrade.py    |   2 +-
 .../stacks/2.3/configs/storm_default.json     |   2 +-
 .../2.3/configs/storm_default_secure.json     |   2 +-
 76 files changed, 674 insertions(+), 260 deletions(-)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/configuration/ranger-storm-audit.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/configuration/ranger-storm-plugin-properties.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-policymgr-ssl.xml
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-security.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/configuration/storm-cluster-log4j.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/configuration/storm-env.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-site.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/configuration/storm-worker-log4j.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.10.0/metainfo.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/metrics.json (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.3/services/STORM => common-services/STORM/0.10.0}/widgets.json (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/alerts.json (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/configuration/storm-env.xml (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/configuration/storm-site.xml (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/kerberos.json (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/metainfo.xml (99%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/metrics.json (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/alerts/check_supervisor_process_win.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/files/wordCount.jar (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/drpc_server.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/nimbus.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/nimbus_prod.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/pacemaker.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/params.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/params_linux.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/params_windows.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/rest_api.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/service.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/service_check.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/setup_atlas_storm.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/setup_ranger_storm.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/status_params.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/storm.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/storm_upgrade.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/storm_yaml_utils.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/supervisor.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/supervisor_prod.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/supervisord_service.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/scripts/ui_server.py (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/client_jaas.conf.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/config.yaml.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/storm-metrics2.properties.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/storm.conf.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/storm_jaas.conf.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/package/templates/worker-launcher.cfg.j2 (100%)
 rename ambari-server/src/main/resources/common-services/STORM/{0.9.1.2.1 => 0.9.1}/quicklinks/quicklinks.json (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.2/services/STORM => common-services/STORM/0.9.3}/configuration/ranger-storm-plugin-properties.xml (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.2/services/STORM => common-services/STORM/0.9.3}/configuration/storm-env.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-site.xml
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/0.9.3/metainfo.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.2/services/STORM => common-services/STORM/0.9.3}/metrics.json (100%)
 rename ambari-server/src/main/resources/{stacks/HDP/2.5/services/STORM => common-services/STORM/1.0.1}/configuration/ranger-storm-audit.xml (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/storm-site.xml
 rename ambari-server/src/main/resources/{stacks/HDP/2.5/services/STORM => common-services/STORM/1.0.1}/kerberos.json (100%)
 create mode 100644 ambari-server/src/main/resources/common-services/STORM/1.0.1/metainfo.xml
 create mode 100644 ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
 create mode 100644 ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-security.xml

diff --git a/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java b/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
index b7e09a94f1..989c9badcb 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/stack/ServiceModule.java
@@ -34,6 +34,7 @@ import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
 import org.apache.ambari.server.state.ServiceInfo;
 import org.apache.ambari.server.state.ServicePropertyInfo;
 import org.apache.ambari.server.state.ThemeInfo;
import org.apache.commons.lang.StringUtils;
 
 import javax.annotation.Nullable;
 
@@ -149,9 +150,28 @@ public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> implem
   public void resolve(
       ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
       throws AmbariException {
    resolveInternal(parentModule, allStacks, commonServices, false);
  }

  public void resolveExplicit(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    resolveInternal(parentModule, allStacks, commonServices, true);
  }
 
    if (!serviceInfo.isValid() || !parentModule.isValid())
  public void resolveInternal(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices,
      boolean resolveExplicit)
      throws AmbariException {
    if (!serviceInfo.isValid() || !parentModule.isValid()) {
       return;
    }

    // If resolving against parent stack service module (stack inheritance), do not merge if an
    // explicit parent is specified
    if(!StringUtils.isBlank(serviceInfo.getParent()) && !resolveExplicit) {
      return;
    }
 
     ServiceInfo parent = parentModule.getModuleInfo();
     
@@ -286,7 +306,7 @@ public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> implem
           //todo: provide more information to user about cycle
           throw new AmbariException("Cycle detected while parsing common service");
         }
        resolve(baseService, allStacks, commonServices);
        resolveExplicit(baseService, allStacks, commonServices);
       } else {
         throw new AmbariException("Common service cannot inherit from a non common service");
       }
diff --git a/ambari-server/src/main/java/org/apache/ambari/server/stack/StackModule.java b/ambari-server/src/main/java/org/apache/ambari/server/stack/StackModule.java
index 5c8556f7fd..7674a2bba8 100644
-- a/ambari-server/src/main/java/org/apache/ambari/server/stack/StackModule.java
++ b/ambari-server/src/main/java/org/apache/ambari/server/stack/StackModule.java
@@ -337,7 +337,7 @@ public class StackModule extends BaseModule<StackModule, StackInfo> implements V
       stackInfo.setErrors(error);
     } else {
       if (baseService.isValid()) {
        service.resolve(baseService, allStacks, commonServices);
        service.resolveExplicit(baseService, allStacks, commonServices);
       } else {
         setValid(false);
         stackInfo.setValid(false);
@@ -386,7 +386,7 @@ public class StackModule extends BaseModule<StackModule, StackInfo> implements V
       throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
           + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'");
       }
    service.resolve(baseService, allStacks, commonServices);
    service.resolveExplicit(baseService, allStacks, commonServices);
   }
 
   /**
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-audit.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-audit.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-audit.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-audit.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-plugin-properties.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-plugin-properties.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-plugin-properties.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-plugin-properties.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-policymgr-ssl.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-policymgr-ssl.xml
new file mode 100644
index 0000000000..af43d75c9d
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-policymgr-ssl.xml
@@ -0,0 +1,67 @@
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
<configuration>
  
  <property>
    <name>xasecure.policymgr.clientssl.keystore</name>
    <value>hadoopdev-clientcert.jks</value>
    <description>Java Keystore files</description>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.keystore.password</name>
    <value>myKeyFilePassword</value>
    <property-type>PASSWORD</property-type>
    <description>password for keystore</description>
    <value-attributes>
      <type>password</type>
    </value-attributes>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.truststore</name>
    <value>cacerts-xasecure.jks</value>
    <description>java truststore file</description>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.truststore.password</name>
    <value>changeit</value>
    <property-type>PASSWORD</property-type>
    <description>java truststore password</description>
    <value-attributes>
      <type>password</type>
    </value-attributes>
  </property>

    <property>
    <name>xasecure.policymgr.clientssl.keystore.credential.file</name>
    <value>jceks://file{{credential_file}}</value>
    <description>java keystore credential file</description>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.truststore.credential.file</name>
    <value>jceks://file{{credential_file}}</value>
    <description>java truststore credential file</description>
  </property>

</configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-security.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-security.xml
new file mode 100644
index 0000000000..538f147819
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/ranger-storm-security.xml
@@ -0,0 +1,59 @@
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
<configuration>
  
  <property>
    <name>ranger.plugin.storm.service.name</name>
    <value>{{repo_name}}</value>
    <description>Name of the Ranger service containing policies for this Storm instance</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.source.impl</name>
    <value>org.apache.ranger.admin.client.RangerAdminRESTClient</value>
    <description>Class to retrieve policies from the source</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.rest.url</name>
    <value>{{policymgr_mgr_url}}</value>
    <description>URL to Ranger Admin</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.rest.ssl.config.file</name>
    <value>/etc/storm/conf/ranger-policymgr-ssl.xml</value>
    <description>Path to the file containing SSL details to contact Ranger Admin</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.pollIntervalMs</name>
    <value>30000</value>
    <description>How often to poll for changes in policies?</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.cache.dir</name>
    <value>/etc/ranger/{{repo_name}}/policycache</value>
    <description>Directory where Ranger policies are cached after successful retrieval from the source</description>
  </property>
  
</configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-cluster-log4j.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-cluster-log4j.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-cluster-log4j.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-cluster-log4j.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-env.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-env.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-env.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-env.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-site.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-site.xml
new file mode 100644
index 0000000000..6a5e6db70f
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-site.xml
@@ -0,0 +1,60 @@
<?xml version="1.0"?>
<!--
 censed to the Apache Software Foundation (ASF) under one
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

<configuration supports_final="true" xmlns:xi="http://www.w3.org/2001/XInclude">
  <property>
    <name>nimbus.seeds</name>
    <value>localhost</value>
    <property-type>DONT_ADD_ON_UPGRADE</property-type>
    <description>Comma-delimited list of the hosts running nimbus server.</description>
    <value-attributes>
      <type>componentHosts</type>
      <editable-only-at-install>true</editable-only-at-install>
      <overridable>false</overridable>
    </value-attributes>
  </property>
  <property>
    <name>topology.min.replication.count.default</name>
    <value>1</value>
    <description>Default minimum number of nimbus hosts where the code must be replicated before leader nimbus can mark the topology as active and create assignments. </description>
  </property>
  <property>
    <name>topology.min.replication.count</name>
    <value>{{actual_topology_min_replication_count}}</value>
    <description>Calculated minimum number of nimbus hosts where the code must be replicated before leader nimbus can mark the topology as active and create assignments. </description>
  </property>  
  <property>
      <name>topology.max.replication.wait.time.sec.default</name>
      <value>60</value>
      <description>Default maximum wait time for the nimbus host replication to achieve the nimbus.min.replication.count. Once this time is elapsed nimbus will go ahead and perform topology activation tasks even if required nimbus.min.replication.count is not achieved</description>
  </property>
  <property>
      <name>topology.max.replication.wait.time.sec</name>
      <value>{{actual_topology_max_replication_wait_time_sec}}</value>
      <description>Calculated maximum wait time for the nimbus host replication to achieve the nimbus.min.replication.count. Once this time is elapsed nimbus will go ahead and perform topology activation tasks even if required nimbus.min.replication.count is not achieved</description>
  </property>  
   <property>
    <name>nimbus.host</name>
    <value>localhost</value>
    <property-type>DONT_ADD_ON_UPGRADE</property-type>
    <description>Deprecated config in favor of nimbus.seeds used during non HA mode.</description>
    <deleted>true</deleted>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-worker-log4j.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-worker-log4j.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-worker-log4j.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/configuration/storm-worker-log4j.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.10.0/metainfo.xml b/ambari-server/src/main/resources/common-services/STORM/0.10.0/metainfo.xml
new file mode 100644
index 0000000000..7fc63b328e
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.10.0/metainfo.xml
@@ -0,0 +1,57 @@
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

<metainfo>
  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>STORM</name>
      <version>0.10.0</version>
      <extends>common-services/STORM/0.9.3</extends>
      <components>
        <component>
          <name>NIMBUS</name>
          <displayName>Nimbus</displayName>
          <category>MASTER</category>
          <cardinality>1+</cardinality>
          <versionAdvertised>true</versionAdvertised>
          <dependencies>
            <dependency>
              <name>ZOOKEEPER/ZOOKEEPER_SERVER</name>
              <scope>cluster</scope>
              <auto-deploy>
                <enabled>true</enabled>
              </auto-deploy>
            </dependency>
          </dependencies>
          <commandScript>
            <script>scripts/nimbus.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>1200</timeout>
          </commandScript>
          <logs>
            <log>
              <logId>storm_nimbus</logId>
              <primary>true</primary>
            </log>
          </logs>
        </component>
      </components>
    </service>
  </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metrics.json b/ambari-server/src/main/resources/common-services/STORM/0.10.0/metrics.json
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metrics.json
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/metrics.json
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/widgets.json b/ambari-server/src/main/resources/common-services/STORM/0.10.0/widgets.json
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/widgets.json
rename to ambari-server/src/main/resources/common-services/STORM/0.10.0/widgets.json
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/alerts.json b/ambari-server/src/main/resources/common-services/STORM/0.9.1/alerts.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/alerts.json
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/alerts.json
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/configuration/storm-env.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.1/configuration/storm-env.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/configuration/storm-env.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/configuration/storm-env.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/configuration/storm-site.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.1/configuration/storm-site.xml
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/configuration/storm-site.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/configuration/storm-site.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/kerberos.json b/ambari-server/src/main/resources/common-services/STORM/0.9.1/kerberos.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/kerberos.json
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/kerberos.json
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/metainfo.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.1/metainfo.xml
similarity index 99%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/metainfo.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/metainfo.xml
index acb4a57bce..0f4d52085a 100644
-- a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/metainfo.xml
++ b/ambari-server/src/main/resources/common-services/STORM/0.9.1/metainfo.xml
@@ -23,7 +23,7 @@
       <name>STORM</name>
       <displayName>Storm</displayName>
       <comment>Apache Hadoop Stream processing framework</comment>
      <version>0.9.1.2.1</version>
      <version>0.9.1</version>
       <components>
 
         <component>
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/metrics.json b/ambari-server/src/main/resources/common-services/STORM/0.9.1/metrics.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/metrics.json
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/metrics.json
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/alerts/check_supervisor_process_win.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/alerts/check_supervisor_process_win.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/alerts/check_supervisor_process_win.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/alerts/check_supervisor_process_win.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/files/wordCount.jar b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/files/wordCount.jar
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/files/wordCount.jar
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/files/wordCount.jar
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/drpc_server.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/drpc_server.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/drpc_server.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/drpc_server.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/nimbus.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/nimbus.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/nimbus.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/nimbus.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/nimbus_prod.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/nimbus_prod.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/nimbus_prod.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/nimbus_prod.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/pacemaker.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/pacemaker.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/pacemaker.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/pacemaker.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params_linux.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params_linux.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params_linux.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params_linux.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params_windows.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params_windows.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/params_windows.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/params_windows.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/rest_api.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/rest_api.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/rest_api.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/rest_api.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/service.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/service.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/service.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/service.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/service_check.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/service_check.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/service_check.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/service_check.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/setup_atlas_storm.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/setup_atlas_storm.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/setup_atlas_storm.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/setup_atlas_storm.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/setup_ranger_storm.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/setup_ranger_storm.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/setup_ranger_storm.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/setup_ranger_storm.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/status_params.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/status_params.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/status_params.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/status_params.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm_upgrade.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm_upgrade.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm_upgrade.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm_upgrade.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm_yaml_utils.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm_yaml_utils.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/storm_yaml_utils.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/storm_yaml_utils.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisor.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisor.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisor.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisor.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisor_prod.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisor_prod.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisor_prod.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisor_prod.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisord_service.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisord_service.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/supervisord_service.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/supervisord_service.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/ui_server.py b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/ui_server.py
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/scripts/ui_server.py
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/scripts/ui_server.py
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/client_jaas.conf.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/client_jaas.conf.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/client_jaas.conf.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/client_jaas.conf.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/config.yaml.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/config.yaml.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/config.yaml.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/config.yaml.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm-metrics2.properties.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm-metrics2.properties.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm-metrics2.properties.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm-metrics2.properties.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm.conf.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm.conf.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm.conf.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm.conf.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm_jaas.conf.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm_jaas.conf.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/storm_jaas.conf.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/storm_jaas.conf.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/worker-launcher.cfg.j2 b/ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/worker-launcher.cfg.j2
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/package/templates/worker-launcher.cfg.j2
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/package/templates/worker-launcher.cfg.j2
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/quicklinks/quicklinks.json b/ambari-server/src/main/resources/common-services/STORM/0.9.1/quicklinks/quicklinks.json
similarity index 100%
rename from ambari-server/src/main/resources/common-services/STORM/0.9.1.2.1/quicklinks/quicklinks.json
rename to ambari-server/src/main/resources/common-services/STORM/0.9.1/quicklinks/quicklinks.json
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/ranger-storm-plugin-properties.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/ranger-storm-plugin-properties.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/ranger-storm-plugin-properties.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/ranger-storm-plugin-properties.xml
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-env.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-env.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-env.xml
rename to ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-env.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-site.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-site.xml
new file mode 100644
index 0000000000..62cd3dbb62
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.9.3/configuration/storm-site.xml
@@ -0,0 +1,123 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
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

<configuration supports_final="true">

  <property>
    <name>storm.thrift.transport</name>
    <value>{{storm_thrift_transport}}</value>
    <description>The transport plug-in that used for Thrift client/server communication.</description>
  </property>
  <property>
    <name>_storm.thrift.nonsecure.transport</name>
    <value>backtype.storm.security.auth.SimpleTransportPlugin</value>
    <description>The transport plug-in that used for non-secure mode for for Thrift client/server communication.</description>
  </property>
  <property>
    <name>_storm.thrift.secure.transport</name>
    <value>backtype.storm.security.auth.kerberos.KerberosSaslTransportPlugin</value>
    <description>The transport plug-in that used for secure mode for Thrift client/server communication.</description>
  </property>

  <property>
    <name>java.library.path</name>
    <value>/usr/local/lib:/opt/local/lib:/usr/lib</value>
    <description>This value is passed to spawned JVMs (e.g., Nimbus, Supervisor, and Workers)
      for the java.library.path value. java.library.path tells the JVM where
      to look for native libraries. It is necessary to set this config correctly since
      Storm uses the ZeroMQ and JZMQ native libs. </description>
  </property>

  <property>
    <name>nimbus.childopts</name>
    <value>-Xmx1024m</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the nimbus daemon.</description>
    <value-attributes>
      <overridable>false</overridable>
    </value-attributes>
  </property>

  <property>
    <name>worker.childopts</name>
    <value>-Xmx768m</value>
    <description>The jvm opts provided to workers launched by this supervisor. All \"%ID%\" substrings are replaced with an identifier for this worker.</description>
    <value-attributes>
      <type>multiLine</type>
    </value-attributes>
  </property>

  <property>
    <name>ui.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER</value>
    <description>Childopts for Storm UI Java process.</description>
  </property>

  <property>
    <name>ui.filter</name>
    <value>null</value>
    <description>Class for Storm UI authentication</description>
  </property>

  <property>
    <name>supervisor.childopts</name>
    <value>-Xmx256m</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the supervisor daemon.</description>
    <value-attributes>
      <overridable>false</overridable>
    </value-attributes>
  </property>
  
   <property>
    <name>logviewer.childopts</name>
    <value>-Xmx128m _JAAS_PLACEHOLDER</value>
    <description>Childopts for log viewer java process.</description>
  </property>

  <property>
    <name>drpc.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER</value>
    <description>Childopts for Storm DRPC Java process.</description>
  </property>
  
  <property>
    <name>_storm.min.ruid</name>
    <value>null</value>
    <description>min.user.id is set to the first real user id on the system. If value is 'null' than default value will be taken from key UID_MIN of /etc/login.defs otherwise the specified value will be used for all hosts.</description>
  </property>

  <property>
    <name>storm.log.dir</name>
    <value>{{log_dir}}</value>
    <description>Log directory for Storm.</description>
  </property>

  <property>
    <name>nimbus.authorizer</name>
    <description>Log directory for Storm.</description>
    <depends-on>
      <property>
        <type>ranger-storm-plugin-properties</type>
        <name>ranger-storm-plugin-enabled</name>
      </property>
    </depends-on>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/common-services/STORM/0.9.3/metainfo.xml b/ambari-server/src/main/resources/common-services/STORM/0.9.3/metainfo.xml
new file mode 100644
index 0000000000..ed4337ca65
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/0.9.3/metainfo.xml
@@ -0,0 +1,53 @@
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

<metainfo>
  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>STORM</name>
      <displayName>Storm</displayName>
      <version>0.9.3</version>
      <extends>common-services/STORM/0.9.1</extends>
      <components>
        <component>
          <name>STORM_REST_API</name>
          <deleted>true</deleted>
        </component>
      </components>
      <osSpecifics>
        <osSpecific>
          <osFamily>redhat7,amazon2015,redhat6,suse11,suse12</osFamily>
          <packages>
            <package>
              <name>storm_${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
        <osSpecific>
          <osFamily>debian7,ubuntu12,ubuntu14</osFamily>
          <packages>
            <package>
              <name>storm-${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
      </osSpecifics>
    </service>
  </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metrics.json b/ambari-server/src/main/resources/common-services/STORM/0.9.3/metrics.json
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metrics.json
rename to ambari-server/src/main/resources/common-services/STORM/0.9.3/metrics.json
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-audit.xml b/ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/ranger-storm-audit.xml
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-audit.xml
rename to ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/ranger-storm-audit.xml
diff --git a/ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/storm-site.xml b/ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/storm-site.xml
new file mode 100644
index 0000000000..19d496190a
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/1.0.1/configuration/storm-site.xml
@@ -0,0 +1,67 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
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
<configuration supports_final="true">
  <property>
    <name>storm.thrift.transport</name>
    <value>org.apache.storm.security.auth.SimpleTransportPlugin</value>
    <description>The transport plug-in for Thrift client/server communication.</description>
  </property>
  <property>
    <name>storm.messaging.transport</name>
    <value>org.apache.storm.messaging.netty.Context</value>
    <description>The transporter for communication among Storm tasks.</description>
  </property>
  <property>
    <name>nimbus.topology.validator</name>
    <value>org.apache.storm.nimbus.DefaultTopologyValidator</value>
    <description>A custom class that implements ITopologyValidator that is run whenever a
       topology is submitted. Can be used to provide business-specific logic for
       whether topologies are allowed to run or not.</description>
  </property>
  <property>
    <name>topology.spout.wait.strategy</name>
    <value>org.apache.storm.spout.SleepSpoutWaitStrategy</value>
    <description>A class that implements a strategy for what to do when a spout needs to wait. Waiting is
       triggered in one of two conditions:

       1. nextTuple emits no tuples
       2. The spout has hit maxSpoutPending and can't emit any more tuples</description>
  </property>
  <property>
    <name>topology.kryo.factory</name>
    <value>org.apache.storm.serialization.DefaultKryoFactory</value>
    <description>Class that specifies how to create a Kryo instance for serialization. Storm will then apply
       topology.kryo.register and topology.kryo.decorators on top of this. The default implementation
       implements topology.fall.back.on.java.serialization and turns references off.</description>
  </property>
  <property>
    <name>topology.tuple.serializer</name>
    <value>org.apache.storm.serialization.types.ListDelegateSerializer</value>
    <description>The serializer class for ListDelegate (tuple payload).
       The default serializer will be ListDelegateSerializer</description>
  </property>
  <property>
    <name>client.jartransformer.class</name>
    <description>Storm Topology backward comptability transformer</description>
    <value>org.apache.storm.hack.StormShadeTransformer</value>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/kerberos.json b/ambari-server/src/main/resources/common-services/STORM/1.0.1/kerberos.json
similarity index 100%
rename from ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/kerberos.json
rename to ambari-server/src/main/resources/common-services/STORM/1.0.1/kerberos.json
diff --git a/ambari-server/src/main/resources/common-services/STORM/1.0.1/metainfo.xml b/ambari-server/src/main/resources/common-services/STORM/1.0.1/metainfo.xml
new file mode 100644
index 0000000000..3c54a1edfd
-- /dev/null
++ b/ambari-server/src/main/resources/common-services/STORM/1.0.1/metainfo.xml
@@ -0,0 +1,28 @@
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

<metainfo>
  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>STORM</name>
      <version>1.0.1</version>
      <extends>common-services/STORM/0.10.0</extends>
    </service>
  </services>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.1/services/STORM/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.1/services/STORM/metainfo.xml
index 02322c1649..3e0d396c0d 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.1/services/STORM/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.1/services/STORM/metainfo.xml
@@ -21,7 +21,8 @@
   <services>
     <service>
       <name>STORM</name>
      <extends>common-services/STORM/0.9.1.2.1</extends>
      <version>0.9.1.2.1</version>
      <extends>common-services/STORM/0.9.1</extends>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-site.xml
index 88b93641c9..a531c61e41 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-site.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/configuration/storm-site.xml
@@ -22,22 +22,6 @@
 
 <configuration supports_final="true">
 
  <property>
    <name>storm.thrift.transport</name>
    <value>{{storm_thrift_transport}}</value>
    <description>The transport plug-in that used for Thrift client/server communication.</description>
  </property>
  <property>
    <name>_storm.thrift.nonsecure.transport</name>
    <value>backtype.storm.security.auth.SimpleTransportPlugin</value>
    <description>The transport plug-in that used for non-secure mode for for Thrift client/server communication.</description>
  </property>
  <property>
    <name>_storm.thrift.secure.transport</name>
    <value>backtype.storm.security.auth.kerberos.KerberosSaslTransportPlugin</value>
    <description>The transport plug-in that used for secure mode for Thrift client/server communication.</description>
  </property>

   <property>
     <name>java.library.path</name>
     <value>/usr/local/lib:/opt/local/lib:/usr/lib:/usr/hdp/current/storm-client/lib</value>
@@ -65,18 +49,6 @@
     </value-attributes>
   </property>
 
  <property>
    <name>ui.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER</value>
    <description>Childopts for Storm UI Java process.</description>
  </property>

  <property>
    <name>ui.filter</name>
    <value>null</value>
    <description>Class for Storm UI authentication</description>
  </property>

   <property>
     <name>supervisor.childopts</name>
     <value>-Xmx256m _JAAS_PLACEHOLDER -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port={{jmxremote_port}} -javaagent:/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8650,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Supervisor_JVM</value>
@@ -86,38 +58,4 @@
     </value-attributes>
   </property>
   
   <property>
    <name>logviewer.childopts</name>
    <value>-Xmx128m _JAAS_PLACEHOLDER</value>
    <description>Childopts for log viewer java process.</description>
  </property>

  <property>
    <name>drpc.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER</value>
    <description>Childopts for Storm DRPC Java process.</description>
  </property>
  
  <property>
    <name>_storm.min.ruid</name>
    <value>null</value>
    <description>min.user.id is set to the first real user id on the system. If value is 'null' than default value will be taken from key UID_MIN of /etc/login.defs otherwise the specified value will be used for all hosts.</description>
  </property>

  <property>
    <name>storm.log.dir</name>
    <value>{{log_dir}}</value>
    <description>Log directory for Storm.</description>
  </property>

  <property>
    <name>nimbus.authorizer</name>
    <description>Log directory for Storm.</description>
    <depends-on>
      <property>
        <type>ranger-storm-plugin-properties</type>
        <name>ranger-storm-plugin-enabled</name>
      </property>
    </depends-on>
  </property>
 </configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metainfo.xml
index 318116facb..3fb8e739a0 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.2/services/STORM/metainfo.xml
@@ -23,30 +23,7 @@
       <name>STORM</name>
       <displayName>Storm</displayName>
       <version>0.9.3.2.2</version>
      <components>
        <component>
          <name>STORM_REST_API</name>
          <deleted>true</deleted>
        </component>
      </components>
      <osSpecifics>
        <osSpecific>
          <osFamily>redhat7,amazon2015,redhat6,suse11,suse12</osFamily>
          <packages>
            <package>
              <name>storm_${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
        <osSpecific>
          <osFamily>debian7,ubuntu12,ubuntu14</osFamily>
          <packages>
            <package>
              <name>storm-${stack_version}</name>
            </package>
          </packages>
        </osSpecific>
      </osSpecifics>
      <extends>common-services/STORM/0.9.3</extends>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-policymgr-ssl.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
index cd0f44edcc..b2d979e84f 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
@@ -26,42 +26,10 @@
     <description>Java Keystore files</description>
   </property>
 
  <property>
    <name>xasecure.policymgr.clientssl.keystore.password</name>
    <value>myKeyFilePassword</value>
    <property-type>PASSWORD</property-type>
    <description>password for keystore</description>
    <value-attributes>
      <type>password</type>
    </value-attributes>
  </property>

   <property>
     <name>xasecure.policymgr.clientssl.truststore</name>
     <value>/usr/hdp/current/storm-client/conf/ranger-plugin-truststore.jks</value>
     <description>java truststore file</description>
   </property>
 
  <property>
    <name>xasecure.policymgr.clientssl.truststore.password</name>
    <value>changeit</value>
    <property-type>PASSWORD</property-type>
    <description>java truststore password</description>
    <value-attributes>
      <type>password</type>
    </value-attributes>
  </property>

    <property>
    <name>xasecure.policymgr.clientssl.keystore.credential.file</name>
    <value>jceks://file{{credential_file}}</value>
    <description>java keystore credential file</description>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.truststore.credential.file</name>
    <value>jceks://file{{credential_file}}</value>
    <description>java truststore credential file</description>
  </property>

 </configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-security.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-security.xml
index f26be4d9d4..f2c3bb7b42 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-security.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/ranger-storm-security.xml
@@ -20,40 +20,10 @@
 -->
 <configuration>
   
  <property>
    <name>ranger.plugin.storm.service.name</name>
    <value>{{repo_name}}</value>
    <description>Name of the Ranger service containing policies for this Storm instance</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.source.impl</name>
    <value>org.apache.ranger.admin.client.RangerAdminRESTClient</value>
    <description>Class to retrieve policies from the source</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.rest.url</name>
    <value>{{policymgr_mgr_url}}</value>
    <description>URL to Ranger Admin</description>
  </property>

   <property>
     <name>ranger.plugin.storm.policy.rest.ssl.config.file</name>
     <value>/usr/hdp/current/storm-client/conf/ranger-policymgr-ssl.xml</value>
     <description>Path to the file containing SSL details to contact Ranger Admin</description>
   </property>
 
  <property>
    <name>ranger.plugin.storm.policy.pollIntervalMs</name>
    <value>30000</value>
    <description>How often to poll for changes in policies?</description>
  </property>

  <property>
    <name>ranger.plugin.storm.policy.cache.dir</name>
    <value>/etc/ranger/{{repo_name}}/policycache</value>
    <description>Directory where Ranger policies are cached after successful retrieval from the source</description>
  </property>
  
 </configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-site.xml
index 6a5e6db70f..3a871ac710 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-site.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/configuration/storm-site.xml
@@ -20,41 +20,39 @@
 
 <configuration supports_final="true" xmlns:xi="http://www.w3.org/2001/XInclude">
   <property>
    <name>nimbus.seeds</name>
    <value>localhost</value>
    <property-type>DONT_ADD_ON_UPGRADE</property-type>
    <description>Comma-delimited list of the hosts running nimbus server.</description>
    <name>java.library.path</name>
    <value>/usr/local/lib:/opt/local/lib:/usr/lib:/usr/hdp/current/storm-client/lib</value>
    <description>This value is passed to spawned JVMs (e.g., Nimbus, Supervisor, and Workers)
      for the java.library.path value. java.library.path tells the JVM where
      to look for native libraries. It is necessary to set this config correctly since
      Storm uses the ZeroMQ and JZMQ native libs. </description>
  </property>

  <property>
    <name>nimbus.childopts</name>
    <value>-Xmx1024m _JAAS_PLACEHOLDER -javaagent:/usr/hdp/current/storm-nimbus/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8649,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-nimbus/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Nimbus_JVM</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the nimbus daemon.</description>
     <value-attributes>
      <type>componentHosts</type>
      <editable-only-at-install>true</editable-only-at-install>
       <overridable>false</overridable>
     </value-attributes>
   </property>

   <property>
    <name>topology.min.replication.count.default</name>
    <value>1</value>
    <description>Default minimum number of nimbus hosts where the code must be replicated before leader nimbus can mark the topology as active and create assignments. </description>
  </property>
  <property>
    <name>topology.min.replication.count</name>
    <value>{{actual_topology_min_replication_count}}</value>
    <description>Calculated minimum number of nimbus hosts where the code must be replicated before leader nimbus can mark the topology as active and create assignments. </description>
  </property>  
  <property>
      <name>topology.max.replication.wait.time.sec.default</name>
      <value>60</value>
      <description>Default maximum wait time for the nimbus host replication to achieve the nimbus.min.replication.count. Once this time is elapsed nimbus will go ahead and perform topology activation tasks even if required nimbus.min.replication.count is not achieved</description>
    <name>worker.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER -javaagent:/usr/hdp/current/storm-client/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8650,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-client/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Worker_%ID%_JVM</value>
    <description>The jvm opts provided to workers launched by this supervisor. All \"%ID%\" substrings are replaced with an identifier for this worker.</description>
    <value-attributes>
      <type>multiLine</type>
    </value-attributes>
   </property>

   <property>
      <name>topology.max.replication.wait.time.sec</name>
      <value>{{actual_topology_max_replication_wait_time_sec}}</value>
      <description>Calculated maximum wait time for the nimbus host replication to achieve the nimbus.min.replication.count. Once this time is elapsed nimbus will go ahead and perform topology activation tasks even if required nimbus.min.replication.count is not achieved</description>
  </property>  
   <property>
    <name>nimbus.host</name>
    <value>localhost</value>
    <property-type>DONT_ADD_ON_UPGRADE</property-type>
    <description>Deprecated config in favor of nimbus.seeds used during non HA mode.</description>
    <deleted>true</deleted>
    <name>supervisor.childopts</name>
    <value>-Xmx256m _JAAS_PLACEHOLDER -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port={{jmxremote_port}} -javaagent:/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8650,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Supervisor_JVM</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the supervisor daemon.</description>
    <value-attributes>
      <overridable>false</overridable>
    </value-attributes>
   </property>

 </configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metainfo.xml
index 29bd6a41ec..8856950634 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.3/services/STORM/metainfo.xml
@@ -21,36 +21,8 @@
   <services>
     <service>
       <name>STORM</name>
      <version>0.10.0</version>
      <components>
        <component>
          <name>NIMBUS</name>
          <displayName>Nimbus</displayName>
          <category>MASTER</category>
          <cardinality>1+</cardinality>
          <versionAdvertised>true</versionAdvertised>
          <dependencies>
            <dependency>
              <name>ZOOKEEPER/ZOOKEEPER_SERVER</name>
              <scope>cluster</scope>
              <auto-deploy>
                <enabled>true</enabled>
              </auto-deploy>
            </dependency>
          </dependencies>
          <commandScript>
            <script>scripts/nimbus.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>1200</timeout>
          </commandScript>
          <logs>
            <log>
              <logId>storm_nimbus</logId>
              <primary>true</primary>
            </log>
          </logs>
        </component>
      </components>
      <extends>common-services/STORM/0.10.0</extends>
      <version>0.10.0.2.3</version>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-policymgr-ssl.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
new file mode 100644
index 0000000000..b2d979e84f
-- /dev/null
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-policymgr-ssl.xml
@@ -0,0 +1,35 @@
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
<configuration>
  
  <property>
    <name>xasecure.policymgr.clientssl.keystore</name>
    <value>/usr/hdp/current/storm-client/conf/ranger-plugin-keystore.jks</value>
    <description>Java Keystore files</description>
  </property>

  <property>
    <name>xasecure.policymgr.clientssl.truststore</name>
    <value>/usr/hdp/current/storm-client/conf/ranger-plugin-truststore.jks</value>
    <description>java truststore file</description>
  </property>

</configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-security.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-security.xml
new file mode 100644
index 0000000000..f2c3bb7b42
-- /dev/null
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/ranger-storm-security.xml
@@ -0,0 +1,29 @@
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
<configuration>
  
  <property>
    <name>ranger.plugin.storm.policy.rest.ssl.config.file</name>
    <value>/usr/hdp/current/storm-client/conf/ranger-policymgr-ssl.xml</value>
    <description>Path to the file containing SSL details to contact Ranger Admin</description>
  </property>

</configuration>
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/storm-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/storm-site.xml
index 19d496190a..3a871ac710 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/storm-site.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/configuration/storm-site.xml
@@ -1,8 +1,6 @@
 <?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
 <!--
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 censed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
@@ -19,49 +17,42 @@
  * limitations under the License.
  */
 -->
<configuration supports_final="true">
  <property>
    <name>storm.thrift.transport</name>
    <value>org.apache.storm.security.auth.SimpleTransportPlugin</value>
    <description>The transport plug-in for Thrift client/server communication.</description>
  </property>
  <property>
    <name>storm.messaging.transport</name>
    <value>org.apache.storm.messaging.netty.Context</value>
    <description>The transporter for communication among Storm tasks.</description>
  </property>

<configuration supports_final="true" xmlns:xi="http://www.w3.org/2001/XInclude">
   <property>
    <name>nimbus.topology.validator</name>
    <value>org.apache.storm.nimbus.DefaultTopologyValidator</value>
    <description>A custom class that implements ITopologyValidator that is run whenever a
       topology is submitted. Can be used to provide business-specific logic for
       whether topologies are allowed to run or not.</description>
    <name>java.library.path</name>
    <value>/usr/local/lib:/opt/local/lib:/usr/lib:/usr/hdp/current/storm-client/lib</value>
    <description>This value is passed to spawned JVMs (e.g., Nimbus, Supervisor, and Workers)
      for the java.library.path value. java.library.path tells the JVM where
      to look for native libraries. It is necessary to set this config correctly since
      Storm uses the ZeroMQ and JZMQ native libs. </description>
   </property>
  <property>
    <name>topology.spout.wait.strategy</name>
    <value>org.apache.storm.spout.SleepSpoutWaitStrategy</value>
    <description>A class that implements a strategy for what to do when a spout needs to wait. Waiting is
       triggered in one of two conditions:
 
       1. nextTuple emits no tuples
       2. The spout has hit maxSpoutPending and can't emit any more tuples</description>
  </property>
   <property>
    <name>topology.kryo.factory</name>
    <value>org.apache.storm.serialization.DefaultKryoFactory</value>
    <description>Class that specifies how to create a Kryo instance for serialization. Storm will then apply
       topology.kryo.register and topology.kryo.decorators on top of this. The default implementation
       implements topology.fall.back.on.java.serialization and turns references off.</description>
    <name>nimbus.childopts</name>
    <value>-Xmx1024m _JAAS_PLACEHOLDER -javaagent:/usr/hdp/current/storm-nimbus/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8649,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-nimbus/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Nimbus_JVM</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the nimbus daemon.</description>
    <value-attributes>
      <overridable>false</overridable>
    </value-attributes>
   </property>

   <property>
    <name>topology.tuple.serializer</name>
    <value>org.apache.storm.serialization.types.ListDelegateSerializer</value>
    <description>The serializer class for ListDelegate (tuple payload).
       The default serializer will be ListDelegateSerializer</description>
    <name>worker.childopts</name>
    <value>-Xmx768m _JAAS_PLACEHOLDER -javaagent:/usr/hdp/current/storm-client/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8650,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-client/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Worker_%ID%_JVM</value>
    <description>The jvm opts provided to workers launched by this supervisor. All \"%ID%\" substrings are replaced with an identifier for this worker.</description>
    <value-attributes>
      <type>multiLine</type>
    </value-attributes>
   </property>

   <property>
    <name>client.jartransformer.class</name>
    <description>Storm Topology backward comptability transformer</description>
    <value>org.apache.storm.hack.StormShadeTransformer</value>
    <name>supervisor.childopts</name>
    <value>-Xmx256m _JAAS_PLACEHOLDER -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port={{jmxremote_port}} -javaagent:/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host=localhost,port=8650,wireformat31x=true,mode=multicast,config=/usr/hdp/current/storm-supervisor/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Supervisor_JVM</value>
    <description>This parameter is used by the storm-deploy project to configure the jvm options for the supervisor daemon.</description>
    <value-attributes>
      <overridable>false</overridable>
    </value-attributes>
   </property>

 </configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/metainfo.xml
index c1e9490c93..3faadc0977 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.5/services/STORM/metainfo.xml
@@ -22,6 +22,7 @@
     <service>
       <name>STORM</name>
       <version>1.0.1.2.5</version>
      <extends>common-services/STORM/1.0.1</extends>
     </service>
   </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/alerts.json b/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/alerts.json
index e5d4e471bd..6c7410f345 100644
-- a/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/alerts.json
++ b/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/alerts.json
@@ -166,7 +166,7 @@
         "scope": "HOST",
         "source": {
           "type": "SCRIPT",
          "path": "STORM/0.9.1.2.1/package/alerts/check_supervisor_process_win.py"
          "path": "STORM/0.9.1/package/alerts/check_supervisor_process_win.py"
         }
       }
     ]
diff --git a/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/metainfo.xml b/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/metainfo.xml
index 76022cc06a..9d504ba35d 100644
-- a/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDPWIN/2.1/services/STORM/metainfo.xml
@@ -21,7 +21,7 @@
   <services>
     <service>
       <name>STORM</name>
      <extends>common-services/STORM/0.9.1.2.1</extends>
      <extends>common-services/STORM/0.9.1</extends>
       <version>0.9.1.2.1.1.0</version>
       <components>
         <component>
diff --git a/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java b/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
index 2721a8e8e0..6bcc671b11 100644
-- a/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
++ b/ambari-server/src/test/java/org/apache/ambari/server/stack/KerberosDescriptorTest.java
@@ -135,7 +135,7 @@ public class KerberosDescriptorTest {
 
   @Test
   public void testCommonStormServiceDescriptor() throws IOException {
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "STORM", "0.9.1.2.1");
    KerberosDescriptor descriptor = getKerberosDescriptor(commonServicesDirectory, "STORM", "0.9.1");
     Assert.notNull(descriptor);
     Assert.notNull(descriptor.getServices());
     Assert.notNull(descriptor.getService("STORM"));
diff --git a/ambari-server/src/test/python/stacks/2.1/STORM/test_storm_base.py b/ambari-server/src/test/python/stacks/2.1/STORM/test_storm_base.py
index 023b811dee..dc1beef7d9 100644
-- a/ambari-server/src/test/python/stacks/2.1/STORM/test_storm_base.py
++ b/ambari-server/src/test/python/stacks/2.1/STORM/test_storm_base.py
@@ -25,7 +25,7 @@ import re
 
 
 class TestStormBase(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1.2.1/package"
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1/package"
   STACK_VERSION = "2.1"
 
   def assert_configure_default(self, confDir="/etc/storm/conf"):
diff --git a/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_base.py b/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_base.py
index 3c602348d2..d23c006f4f 100644
-- a/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_base.py
++ b/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_base.py
@@ -22,7 +22,7 @@ from stacks.utils.RMFTestCase import *
 
 
 class TestStormBase(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1.2.1/package"
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1/package"
   STACK_VERSION = "2.3"
 
   def assert_configure_default(self, confDir="/etc/storm/conf"):
diff --git a/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_upgrade.py b/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_upgrade.py
index d0356ffc01..074ed68a1c 100644
-- a/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_upgrade.py
++ b/ambari-server/src/test/python/stacks/2.3/STORM/test_storm_upgrade.py
@@ -24,7 +24,7 @@ from stacks.utils.RMFTestCase import *
 
 
 class TestStormUpgrade(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1.2.1/package"
  COMMON_SERVICES_PACKAGE_DIR = "STORM/0.9.1/package"
   STACK_VERSION = "2.3"
 
   def test_delete_zookeeper_data(self):
diff --git a/ambari-server/src/test/python/stacks/2.3/configs/storm_default.json b/ambari-server/src/test/python/stacks/2.3/configs/storm_default.json
index 72afd78894..351025db38 100644
-- a/ambari-server/src/test/python/stacks/2.3/configs/storm_default.json
++ b/ambari-server/src/test/python/stacks/2.3/configs/storm_default.json
@@ -8,7 +8,7 @@
         "cluster-env": {}
     }, 
     "commandParams": {
        "service_package_folder": "common-services/STORM/0.9.1.2.1/package", 
        "service_package_folder": "common-services/STORM/0.9.1/package",
         "script": "scripts/service_check.py", 
         "hooks_folder": "HDP/2.0.6/hooks", 
         "command_retry_max_attempt_count": "3", 
diff --git a/ambari-server/src/test/python/stacks/2.3/configs/storm_default_secure.json b/ambari-server/src/test/python/stacks/2.3/configs/storm_default_secure.json
index c1b7e10aae..c90d5bbda3 100644
-- a/ambari-server/src/test/python/stacks/2.3/configs/storm_default_secure.json
++ b/ambari-server/src/test/python/stacks/2.3/configs/storm_default_secure.json
@@ -8,7 +8,7 @@
         "cluster-env": {}
     }, 
     "commandParams": {
        "service_package_folder": "common-services/STORM/0.9.1.2.1/package", 
        "service_package_folder": "common-services/STORM/0.9.1/package",
         "script": "scripts/service_check.py", 
         "hooks_folder": "HDP/2.0.6/hooks", 
         "command_retry_max_attempt_count": "3", 
- 
2.19.1.windows.1

