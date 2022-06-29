From 9f663c7dd285e56b3f26b36461d5ae13e126cf74 Mon Sep 17 00:00:00 2001
From: Dmytro Sen <dsen@hortonworks.com>
Date: Sat, 28 Jun 2014 11:13:24 +0300
Subject: [PATCH] AMBARI-6271 Multiple Stack Service Exceptions When Loading
 Ambari Server.  Additional patch (dsen)

--
 .../services/HUE/configuration/global.xml     |  35 ---
 .../services/HUE/configuration/hue-site.xml   | 290 ------------------
 2 files changed, 325 deletions(-)
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/hue-site.xml

diff --git a/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/global.xml
deleted file mode 100644
index c49480f419..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/global.xml
++ /dev/null
@@ -1,35 +0,0 @@
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

<configuration>
  <property>
    <name>hue_pid_dir</name>
    <value>/var/run/hue</value>
    <description>Hue Pid Dir.</description>
  </property>
  <property>
    <name>hue_log_dir</name>
    <value>/var/log/hue</value>
    <description>Hue Log Dir.</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/hue-site.xml b/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/hue-site.xml
deleted file mode 100644
index 6eb52a23cc..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/1.3.0/services/HUE/configuration/hue-site.xml
++ /dev/null
@@ -1,290 +0,0 @@
<?xml version="1.0"?>

<!--
   Licensed to the Apache Software Foundation (ASF) under one or more# Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with# contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.# this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0# The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with# (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at# the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0#     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software# Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,# distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and# See the License for the specific language governing permissions and
   limitations under the License.# limitations under the License.
-->

<configuration>
  <!-- General Hue server configuration properties -->
  <property>
      <name>send_debug_messages</name>
      <value>1</value>
      <description></description>
  </property>

  <property>
    <name>database_logging</name>
    <value>0</value>
    <description>To show database transactions, set database_logging to 1.
      default, database_logging=0</description>
  </property>

  <property>
    <name>secret_key</name>
    <value></value>
    <description>This is used for secure hashing in the session store.</description>
  </property>

  <property>
    <name>http_host</name>
    <value>0.0.0.0</value>
    <description>Webserver listens on this address and port</description>
  </property>

  <property>
    <name>http_port</name>
    <value>8000</value>
    <description>Webserver listens on this address and port</description>
  </property>

  <property>
    <name>time_zone</name>
    <value>America/Los_Angeles</value>
    <description>Time zone name</description>
  </property>

  <property>
    <name>django_debug_mode</name>
    <value>1</value>
    <description>Turn off debug</description>
  </property>

  <property>
    <name>use_cherrypy_server</name>
    <value>false</value>
    <description>Set to true to use CherryPy as the webserver, set to false
      to use Spawning as the webserver. Defaults to Spawning if
      key is not specified.</description>
  </property>

  <property>
    <name>http_500_debug_mode</name>
    <value>1</value>
    <description>Turn off backtrace for server error</description>
  </property>

  <property>
    <name>server_user</name>
    <value></value>
    <description>Webserver runs as this user</description>
  </property>

  <property>
    <name>server_group</name>
    <value></value>
    <description>Webserver runs as this user</description>
  </property>

  <property>
    <name>backend_auth_policy</name>
    <value>desktop.auth.backend.AllowAllBackend</value>
    <description>Authentication backend.</description>
  </property>

  <!-- Hue Database configuration properties -->
  <property>
    <name>db_engine</name>
    <value>mysql</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <property>
    <name>db_host</name>
    <value>localhost</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <property>
    <name>db_port</name>
    <value>3306</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <property>
    <name>db_user</name>
    <value>sandbox</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <property>
    <name>db_password</name>
    <value>1111</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <property>
    <name>db_name</name>
    <value>sandbox</value>
    <description>Configuration options for specifying the Desktop Database.</description>
  </property>

  <!-- Hue Email configuration properties -->
  <property>
    <name>smtp_host</name>
    <value>localhost</value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <property>
    <name>smtp_port</name>
    <value>25</value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <property>
    <name>smtp_user</name>
    <value></value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <property>
    <name>smtp_password</name>
    <value>25</value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <property>
    <name>tls</name>
    <value>no</value>
    <description>Whether to use a TLS (secure) connection when talking to the SMTP server.</description>
  </property>

  <property>
    <name>default_from_email</name>
    <value>sandbox@hortonworks.com</value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <!-- Hue Hadoop configuration properties -->
  <property>
    <name>fs_defaultfs</name>
    <value></value>
    <description>Enter the filesystem uri. E.g
      .:hdfs://sandbox:8020</description>
  </property>

  <property>
    <name>webhdfs_url</name>
    <value></value>
    <description>Use WebHdfs/HttpFs as the communication mechanism. To fallback to
      using the Thrift plugin (used in Hue 1.x), this must be uncommented
      and explicitly set to the empty value.
      Value e.g.: http://localhost:50070/webhdfs/v1/</description>
  </property>

  <property>
    <name>jobtracker_host</name>
    <value></value>
    <description>Enter the host on which you are running the Hadoop JobTracker.</description>
  </property>

  <property>
    <name>jobtracker_port</name>
    <value>50030</value>
    <description>The port where the JobTracker IPC listens on.</description>
  </property>

  <property>
    <name>hadoop_mapred_home</name>
    <value>/usr/lib/hadoop/lib</value>
    <description>The SMTP server information for email notification delivery.</description>
  </property>

  <property>
    <name>resourcemanager_host</name>
    <value></value>
    <description>Enter the host on which you are running the ResourceManager.</description>
  </property>

  <property>
    <name>resourcemanager_port</name>
    <value></value>
    <description>The port where the ResourceManager IPC listens on.</description>
  </property>

  <!-- Hue Beeswax configuration properties -->
  <property>
    <name>hive_home_dir</name>
    <value></value>
    <description>Hive home directory.</description>
  </property>

  <property>
    <name>hive_conf_dir</name>
    <value></value>
    <description>Hive configuration directory, where hive-site.xml is
      located.</description>
  </property>

  <property>
    <name>templeton_url</name>
    <value></value>
    <description>WebHcat http URL</description>
  </property>

  <!-- Hue shell types configuration -->
  <property>
    <name>pig_nice_name</name>
    <value></value>
    <description>Define and configure a new shell type pig</description>
  </property>

  <property>
    <name>pig_shell_command</name>
    <value>/usr/bin/pig -l /dev/null</value>
    <description>Define and configure a new shell type pig.</description>
  </property>

  <property>
    <name>pig_java_home</name>
    <value></value>
    <description>Define and configure a new shell type pig.</description>
  </property>

  <property>
    <name>hbase_nice_name</name>
    <value>HBase Shell</value>
    <description>Define and configure a new shell type hbase</description>
  </property>

  <property>
    <name>hbase_shell_command</name>
    <value>/usr/bin/hbase shell</value>
    <description>Define and configure a new shell type hbase.</description>
  </property>

  <property>
    <name>bash_nice_name</name>
    <value></value>
    <description>Define and configure a new shell type bash for testing
      only</description>
  </property>

  <property>
    <name>bash_shell_command</name>
    <value>/bin/bash</value>
    <description>Define and configure a new shell type bash for testing only
      .</description>
  </property>

  <!-- Hue Settings for the User Admin application -->
  <property>
    <name>whitelist</name>
    <value>(localhost|127\.0\.0\.1):(50030|50070|50060|50075|50111)</value>
    <description>proxy settings</description>
  </property>

</configuration>
\ No newline at end of file
- 
2.19.1.windows.1

