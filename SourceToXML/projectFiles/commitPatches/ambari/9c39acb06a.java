From 9c39acb06a12393c1beb6129a61123d82e79f87d Mon Sep 17 00:00:00 2001
From: Lisnichenko Dmitro <dlysnichenko@hortonworks.com>
Date: Tue, 8 Jul 2014 17:43:28 +0300
Subject: [PATCH] AMBARI-6271. Multiple Stack Service Exceptions When Loading
 Ambari Server. Fixing regression (dlysnichenko)

--
 .../stacks/HDP/2.0.6.GlusterFS/metainfo.xml   |     5 +-
 .../services/GANGLIA/configuration/global.xml |    51 -
 .../services/GANGLIA/metainfo.xml             |    40 -
 .../services/GLUSTERFS/metainfo.xml           |    36 +-
 .../services/HBASE/configuration/global.xml   |   160 -
 .../HBASE/configuration/hbase-policy.xml      |    53 -
 .../HBASE/configuration/hbase-site.xml        |   362 -
 .../services/HBASE/metainfo.xml               |    44 -
 .../services/HBASE/metrics.json               | 13635 ----------------
 .../services/HCATALOG/metainfo.xml            |    30 -
 .../services/HDFS/metainfo.xml                |    64 +-
 .../services/HIVE/configuration/hive-site.xml |   261 -
 .../services/HIVE/metainfo.xml                |    45 -
 .../configuration/container-executor.cfg      |    20 -
 .../MAPREDUCE2/configuration/core-site.xml    |    20 -
 .../MAPREDUCE2/configuration/global.xml       |    44 -
 .../configuration/mapred-queue-acls.xml       |    39 -
 .../MAPREDUCE2/configuration/mapred-site.xml  |   381 -
 .../services/MAPREDUCE2/metainfo.xml          |    37 -
 .../services/MAPREDUCE2/metrics.json          |   383 -
 .../services/NAGIOS/configuration/global.xml  |    51 -
 .../services/NAGIOS/metainfo.xml              |    34 -
 .../OOZIE/configuration/oozie-site.xml        |   313 -
 .../services/OOZIE/metainfo.xml               |    38 -
 .../services/PIG/configuration/pig.properties |    52 -
 .../2.0.6.GlusterFS/services/PIG/metainfo.xml |    30 -
 .../services/SQOOP/metainfo.xml               |    30 -
 .../services/WEBHCAT/configuration/global.xml |    51 -
 .../WEBHCAT/configuration/webhcat-site.xml    |   126 -
 .../services/WEBHCAT/metainfo.xml             |    35 -
 .../YARN/configuration/capacity-scheduler.xml |   128 -
 .../YARN/configuration/container-executor.cfg |    20 -
 .../services/YARN/configuration/core-site.xml |    20 -
 .../services/YARN/configuration/global.xml    |    64 -
 .../services/YARN/configuration/yarn-site.xml |   326 -
 .../services/YARN/metainfo.xml                |    42 -
 .../services/YARN/metrics.json                |  2534 ---
 .../ZOOKEEPER/configuration/global.xml        |    75 -
 .../services/ZOOKEEPER/metainfo.xml           |    39 -
 39 files changed, 56 insertions(+), 19662 deletions(-)
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-policy.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metrics.json
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HCATALOG/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/configuration/hive-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/container-executor.cfg
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/core-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-queue-acls.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metrics.json
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/configuration/oozie-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/configuration/pig.properties
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/SQOOP/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/webhcat-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/capacity-scheduler.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/container-executor.cfg
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/core-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/yarn-site.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metainfo.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metrics.json
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/configuration/global.xml
 delete mode 100644 ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/metainfo.xml

diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/metainfo.xml
index 45a63e5550..e1b0ec998f 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/metainfo.xml
@@ -16,7 +16,8 @@
    limitations under the License.
 -->
 <metainfo>
    <versions>
  <versions>
 	  <active>false</active>
    </versions>
  </versions>
  <extends>2.0.6</extends>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/configuration/global.xml
deleted file mode 100644
index 18eae57fdd..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/configuration/global.xml
++ /dev/null
@@ -1,51 +0,0 @@
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
    <name>ganglia_conf_dir</name>
    <value>/etc/ganglia/hdp</value>
    <description></description>
  </property>
  <property>
    <name>ganglia_runtime_dir</name>
    <value>/var/run/ganglia/hdp</value>
    <description></description>
  </property>
  <property>
    <name>gmetad_user</name>
    <value>nobody</value>
    <description></description>
  </property>
  <property>
    <name>gmond_user</name>
    <value>nobody</value>
    <description></description>
  </property>
  <property>
    <name>rrdcached_base_dir</name>
    <value>/var/lib/ganglia/rrds</value>
    <description>Location of rrd files.</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/metainfo.xml
deleted file mode 100644
index 3e6d37ae66..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GANGLIA/metainfo.xml
++ /dev/null
@@ -1,40 +0,0 @@
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
    <user>root</user>
    <comment>Ganglia Metrics Collection system</comment>
    <version>3.5.0</version>

    <components>
        <component>
            <name>GANGLIA_SERVER</name>
            <category>MASTER</category>
        </component>

        <component>
            <name>GANGLIA_MONITOR</name>
            <category>SLAVE</category>
        </component>

    </components>

  <configuration-dependencies>
    <config-type>global</config-type>
  </configuration-dependencies>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GLUSTERFS/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GLUSTERFS/metainfo.xml
index 4ac5125a95..1aa4d9675a 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GLUSTERFS/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/GLUSTERFS/metainfo.xml
@@ -16,17 +16,31 @@
    limitations under the License.
 -->
 <metainfo>
    <user>root</user>
    <comment>Hadoop Compatible File System - GLUSTERFS</comment>
    <version>3.4.0</version>
    <components>
  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>GLUSTERFS</name>
      <comment>An Hadoop Compatible File System</comment>
      <version>3.4.0</version>
      <components>
         <component>
            <name>GLUSTERFS_CLIENT</name>
            <category>CLIENT</category>
          <name>GLUSTERFS_CLIENT</name>
          <category>CLIENT</category>
          <commandScript>
            <script>scripts/glusterfs_client.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>600</timeout>
          </commandScript>
         </component>
    </components>
    <configuration-dependencies>
      <config-type>core-site</config-type>
      <config-type>global</config-type>
    </configuration-dependencies>
      </components>

      <configuration-dependencies>
        <config-type>core-site</config-type>
        <config-type>global</config-type>
        <config-type>hadoop-policy</config-type>
        <config-type>hdfs-site</config-type>
      </configuration-dependencies>

    </service>
  </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/global.xml
deleted file mode 100644
index b2c57bda08..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/global.xml
++ /dev/null
@@ -1,160 +0,0 @@
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
    <name>hbasemaster_host</name>
    <value></value>
    <description>HBase Master Host.</description>
  </property>
  <property>
    <name>regionserver_hosts</name>
    <value></value>
    <description>Region Server Hosts</description>
  </property>
  <property>
    <name>hbase_log_dir</name>
    <value>/var/log/hbase</value>
    <description>Log Directories for HBase.</description>
  </property>
  <property>
    <name>hbase_pid_dir</name>
    <value>/var/run/hbase</value>
    <description>Log Directories for HBase.</description>
  </property>
  <property>
    <name>hbase_log_dir</name>
    <value>/var/log/hbase</value>
    <description>Log Directories for HBase.</description>
  </property>
  <property>
    <name>hbase_regionserver_heapsize</name>
    <value>1024</value>
    <description>Log Directories for HBase.</description>
  </property>
  <property>
    <name>hbase_master_heapsize</name>
    <value>1024</value>
    <description>HBase Master Heap Size</description>
  </property>
  <property>
    <name>hstore_compactionthreshold</name>
    <value>3</value>
    <description>HBase HStore compaction threshold.</description>
  </property>
  <property>
    <name>hfile_blockcache_size</name>
    <value>0.40</value>
    <description>HFile block cache size.</description>
  </property>
  <property>
    <name>hstorefile_maxsize</name>
    <value>10737418240</value>
    <description>Maximum HStoreFile Size</description>
  </property>
    <property>
    <name>regionserver_handlers</name>
    <value>60</value>
    <description>HBase RegionServer Handler</description>
  </property>
    <property>
    <name>hregion_majorcompaction</name>
    <value>604800000</value>
    <description>The time between major compactions of all HStoreFiles in a region. Set to 0 to disable automated major compactions.</description>
  </property>
    <property>
    <name>hregion_blockmultiplier</name>
    <value>2</value>
    <description>HBase Region Block Multiplier</description>
  </property>
    <property>
    <name>hregion_memstoreflushsize</name>
    <value></value>
    <description>HBase Region MemStore Flush Size.</description>
  </property>
    <property>
    <name>client_scannercaching</name>
    <value>100</value>
    <description>Base Client Scanner Caching</description>
  </property>
    <property>
    <name>zookeeper_sessiontimeout</name>
    <value>30000</value>
    <description>ZooKeeper Session Timeout</description>
  </property>
    <property>
    <name>hfile_max_keyvalue_size</name>
    <value>10485760</value>
    <description>HBase Client Maximum key-value Size</description>
  </property>
  <property>
    <name>hbase_hdfs_root_dir</name>
    <value>/apps/hbase/data</value>
    <description>HBase Relative Path to HDFS.</description>
  </property>
   <property>
    <name>hbase_conf_dir</name>
    <value>/etc/hbase</value>
    <description>Config Directory for HBase.</description>
  </property>
   <property>
    <name>hdfs_enable_shortcircuit_read</name>
    <value>true</value>
    <description>HDFS Short Circuit Read</description>
  </property>
   <property>
    <name>hdfs_support_append</name>
    <value>true</value>
    <description>HDFS append support</description>
  </property>
   <property>
    <name>hstore_blockingstorefiles</name>
    <value>10</value>
    <description>HStore blocking storefiles.</description>
  </property>
   <property>
    <name>regionserver_memstore_lab</name>
    <value>true</value>
    <description>Region Server memstore.</description>
  </property>
   <property>
    <name>regionserver_memstore_lowerlimit</name>
    <value>0.38</value>
    <description>Region Server memstore lower limit.</description>
  </property>
   <property>
    <name>regionserver_memstore_upperlimit</name>
    <value>0.4</value>
    <description>Region Server memstore upper limit.</description>
  </property>
   <property>
    <name>hbase_conf_dir</name>
    <value>/etc/hbase</value>
    <description>HBase conf dir.</description>
  </property>
   <property>
    <name>hbase_user</name>
    <value>hbase</value>
    <description>HBase User Name.</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-policy.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-policy.xml
deleted file mode 100644
index e45f23c962..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-policy.xml
++ /dev/null
@@ -1,53 +0,0 @@
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
    <name>security.client.protocol.acl</name>
    <value>*</value>
    <description>ACL for HRegionInterface protocol implementations (ie. 
    clients talking to HRegionServers)
    The ACL is a comma-separated list of user and group names. The user and 
    group list is separated by a blank. For e.g. "alice,bob users,wheel". 
    A special value of "*" means all users are allowed.</description>
  </property>

  <property>
    <name>security.admin.protocol.acl</name>
    <value>*</value>
    <description>ACL for HMasterInterface protocol implementation (ie. 
    clients talking to HMaster for admin operations).
    The ACL is a comma-separated list of user and group names. The user and 
    group list is separated by a blank. For e.g. "alice,bob users,wheel". 
    A special value of "*" means all users are allowed.</description>
  </property>

  <property>
    <name>security.masterregion.protocol.acl</name>
    <value>*</value>
    <description>ACL for HMasterRegionInterface protocol implementations
    (for HRegionServers communicating with HMaster)
    The ACL is a comma-separated list of user and group names. The user and 
    group list is separated by a blank. For e.g. "alice,bob users,wheel". 
    A special value of "*" means all users are allowed.</description>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-site.xml
deleted file mode 100644
index 104ed0d095..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/configuration/hbase-site.xml
++ /dev/null
@@ -1,362 +0,0 @@
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
    <name>hbase.rootdir</name>
    <value>glusterfs:///hbase</value>
    <description>The directory shared by region servers and into
    which HBase persists.  The URL should be 'fully-qualified'
    to include the filesystem scheme.  For example, to specify the
    HDFS directory '/hbase' where the HDFS instance's namenode is
    running at namenode.example.org on port 9000, set this value to:
    hdfs://namenode.example.org:9000/hbase.  By default HBase writes
    into /tmp.  Change this configuration else all data will be lost
    on machine restart.
    </description>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
    <description>The mode the cluster will be in. Possible values are
      false for standalone mode and true for distributed mode.  If
      false, startup will run all HBase and ZooKeeper daemons together
      in the one JVM.
    </description>
  </property>
  <property>
    <name>hbase.tmp.dir</name>
    <value>/hadoop/hbase</value>
    <description>Temporary directory on the local filesystem.
    Change this setting to point to a location more permanent
    than '/tmp' (The '/tmp' directory is often cleared on
    machine restart).
    </description>
  </property>
  <property>
    <name>hbase.local.dir</name>
    <value>${hbase.tmp.dir}/local</value>
    <description>Directory on the local filesystem to be used as a local storage
    </description>
  </property>
  <property>
    <name>hbase.master.info.bindAddress</name>
    <value></value>
    <description>The bind address for the HBase Master web UI
    </description>
  </property>
  <property>
    <name>hbase.master.info.port</name>
    <value></value>
    <description>The port for the HBase Master web UI.</description>
  </property>
  <property>
    <name>hbase.regionserver.info.port</name>
    <value></value>
    <description>The port for the HBase RegionServer web UI.</description>
  </property>
  <property>
    <name>hbase.regionserver.global.memstore.upperLimit</name>
    <value>0.4</value>
    <description>Maximum size of all memstores in a region server before new
      updates are blocked and flushes are forced. Defaults to 40% of heap
    </description>
  </property>
  <property>
    <name>hbase.regionserver.handler.count</name>
    <value>60</value>
    <description>Count of RPC Listener instances spun up on RegionServers.
    Same property is used by the Master for count of master handlers.
    Default is 10.
    </description>
  </property>
  <property>
    <name>hbase.hregion.majorcompaction</name>
    <value>86400000</value>
    <description>The time (in milliseconds) between 'major' compactions of all
    HStoreFiles in a region.  Default: 1 day.
    Set to 0 to disable automated major compactions.
    </description>
  </property>
  
  <property>
    <name>hbase.regionserver.global.memstore.lowerLimit</name>
    <value>0.38</value>
    <description>When memstores are being forced to flush to make room in
      memory, keep flushing until we hit this mark. Defaults to 35% of heap.
      This value equal to hbase.regionserver.global.memstore.upperLimit causes
      the minimum possible flushing to occur when updates are blocked due to
      memstore limiting.
    </description>
  </property>
  <property>
    <name>hbase.hregion.memstore.block.multiplier</name>
    <value>2</value>
    <description>Block updates if memstore has hbase.hregion.memstore.block.multiplier
    time hbase.hregion.flush.size bytes.  Useful preventing
    runaway memstore during spikes in update traffic.  Without an
    upper-bound, memstore fills such that when it flushes the
    resultant flush files take a long time to compact or split, or
    worse, we OOME
    </description>
  </property>
  <property>
    <name>hbase.hregion.memstore.flush.size</name>
    <value>134217728</value>
    <description>
    Memstore will be flushed to disk if size of the memstore
    exceeds this number of bytes.  Value is checked by a thread that runs
    every hbase.server.thread.wakefrequency.
    </description>
  </property>
  <property>
    <name>hbase.hregion.memstore.mslab.enabled</name>
    <value>true</value>
    <description>
      Enables the MemStore-Local Allocation Buffer,
      a feature which works to prevent heap fragmentation under
      heavy write loads. This can reduce the frequency of stop-the-world
      GC pauses on large heaps.
    </description>
  </property>
  <property>
    <name>hbase.hregion.max.filesize</name>
    <value>10737418240</value>
    <description>
    Maximum HStoreFile size. If any one of a column families' HStoreFiles has
    grown to exceed this value, the hosting HRegion is split in two.
    Default: 1G.
    </description>
  </property>
  <property>
    <name>hbase.client.scanner.caching</name>
    <value>100</value>
    <description>Number of rows that will be fetched when calling next
    on a scanner if it is not served from (local, client) memory. Higher
    caching values will enable faster scanners but will eat up more memory
    and some calls of next may take longer and longer times when the cache is empty.
    Do not set this value such that the time between invocations is greater
    than the scanner timeout; i.e. hbase.regionserver.lease.period
    </description>
  </property>
  <property>
    <name>zookeeper.session.timeout</name>
    <value>30000</value>
    <description>ZooKeeper session timeout.
      HBase passes this to the zk quorum as suggested maximum time for a
      session (This setting becomes zookeeper's 'maxSessionTimeout').  See
      http://hadoop.apache.org/zookeeper/docs/current/zookeeperProgrammers.html#ch_zkSessions
      "The client sends a requested timeout, the server responds with the
      timeout that it can give the client. " In milliseconds.
    </description>
  </property>
  <property>
    <name>hbase.client.keyvalue.maxsize</name>
    <value>10485760</value>
    <description>Specifies the combined maximum allowed size of a KeyValue
    instance. This is to set an upper boundary for a single entry saved in a
    storage file. Since they cannot be split it helps avoiding that a region
    cannot be split any further because the data is too large. It seems wise
    to set this to a fraction of the maximum region size. Setting it to zero
    or less disables the check.
    </description>
  </property>
  <property>
    <name>hbase.hstore.compactionThreshold</name>
    <value>3</value>
    <description>
    If more than this number of HStoreFiles in any one HStore
    (one HStoreFile is written per flush of memstore) then a compaction
    is run to rewrite all HStoreFiles files as one.  Larger numbers
    put off compaction but when it runs, it takes longer to complete.
    </description>
  </property>
  <property>
    <name>hbase.hstore.flush.retries.number</name>
    <value>120</value>
    <description>
    The number of times the region flush operation will be retried.
    </description>
  </property>
  
  <property>
    <name>hbase.hstore.blockingStoreFiles</name>
    <value>10</value>
    <description>
    If more than this number of StoreFiles in any one Store
    (one StoreFile is written per flush of MemStore) then updates are
    blocked for this HRegion until a compaction is completed, or
    until hbase.hstore.blockingWaitTime has been exceeded.
    </description>
  </property>
  <property>
    <name>hfile.block.cache.size</name>
    <value>0.40</value>
    <description>
        Percentage of maximum heap (-Xmx setting) to allocate to block cache
        used by HFile/StoreFile. Default of 0.25 means allocate 25%.
        Set to 0 to disable but it's not recommended.
    </description>
  </property>

  <!-- The following properties configure authentication information for
       HBase processes when using Kerberos security.  There are no default
       values, included here for documentation purposes -->
  <property>
    <name>hbase.master.keytab.file</name>
    <value></value>
    <description>Full path to the kerberos keytab file to use for logging in
    the configured HMaster server principal.
    </description>
  </property>
  <property>
    <name>hbase.master.kerberos.principal</name>
    <value></value>
    <description>Ex. "hbase/_HOST@EXAMPLE.COM".  The kerberos principal name
    that should be used to run the HMaster process.  The principal name should
    be in the form: user/hostname@DOMAIN.  If "_HOST" is used as the hostname
    portion, it will be replaced with the actual hostname of the running
    instance.
    </description>
  </property>
  <property>
    <name>hbase.regionserver.keytab.file</name>
    <value></value>
    <description>Full path to the kerberos keytab file to use for logging in
    the configured HRegionServer server principal.
    </description>
  </property>
  <property>
    <name>hbase.regionserver.kerberos.principal</name>
    <value></value>
    <description>Ex. "hbase/_HOST@EXAMPLE.COM".  The kerberos principal name
    that should be used to run the HRegionServer process.  The principal name
    should be in the form: user/hostname@DOMAIN.  If "_HOST" is used as the
    hostname portion, it will be replaced with the actual hostname of the
    running instance.  An entry for this principal must exist in the file
    specified in hbase.regionserver.keytab.file
    </description>
  </property>

  <!-- Additional configuration specific to HBase security -->
  <property>
    <name>hbase.superuser</name>
    <value>hbase</value>
    <description>List of users or groups (comma-separated), who are allowed
    full privileges, regardless of stored ACLs, across the cluster.
    Only used when HBase security is enabled.
    </description>
  </property>

  <property>
    <name>hbase.security.authentication</name>
    <value>simple</value>
  </property>

  <property>
    <name>hbase.security.authorization</name>
    <value>false</value>
    <description>Enables HBase authorization. Set the value of this property to false to disable HBase authorization.
    </description>
  </property>

  <property>
    <name>hbase.coprocessor.region.classes</name>
    <value></value>
    <description>A comma-separated list of Coprocessors that are loaded by
    default on all tables. For any override coprocessor method, these classes
    will be called in order. After implementing your own Coprocessor, just put
    it in HBase's classpath and add the fully qualified class name here.
    A coprocessor can also be loaded on demand by setting HTableDescriptor.
    </description>
  </property>

  <property>
    <name>hbase.coprocessor.master.classes</name>
    <value></value>
    <description>A comma-separated list of
      org.apache.hadoop.hbase.coprocessor.MasterObserver coprocessors that are
      loaded by default on the active HMaster process. For any implemented
      coprocessor methods, the listed classes will be called in order. After
      implementing your own MasterObserver, just put it in HBase's classpath
      and add the fully qualified class name here.
    </description>
  </property>

  <property>
    <name>hbase.zookeeper.property.clientPort</name>
    <value>2181</value>
    <description>Property from ZooKeeper's config zoo.cfg.
    The port at which the clients will connect.
    </description>
  </property>

  <!--
  The following three properties are used together to create the list of
  host:peer_port:leader_port quorum servers for ZooKeeper.
  -->
  <property>
    <name>hbase.zookeeper.quorum</name>
    <value>localhost</value>
    <description>Comma separated list of servers in the ZooKeeper Quorum.
    For example, "host1.mydomain.com,host2.mydomain.com,host3.mydomain.com".
    By default this is set to localhost for local and pseudo-distributed modes
    of operation. For a fully-distributed setup, this should be set to a full
    list of ZooKeeper quorum servers. If HBASE_MANAGES_ZK is set in hbase-env.sh
    this is the list of servers which we will start/stop ZooKeeper on.
    </description>
  </property>
  <!-- End of properties used to generate ZooKeeper host:port quorum list. -->

  <property>
    <name>hbase.zookeeper.useMulti</name>
    <value>true</value>
    <description>Instructs HBase to make use of ZooKeeper's multi-update functionality.
    This allows certain ZooKeeper operations to complete more quickly and prevents some issues
    with rare Replication failure scenarios (see the release note of HBASE-2611 for an example).·
    IMPORTANT: only set this to true if all ZooKeeper servers in the cluster are on version 3.4+
    and will not be downgraded.  ZooKeeper versions before 3.4 do not support multi-update and will
    not fail gracefully if multi-update is invoked (see ZOOKEEPER-1495).
    </description>
  </property>
  <property>
    <name>zookeeper.znode.parent</name>
    <value>/hbase-unsecure</value>
    <description>Root ZNode for HBase in ZooKeeper. All of HBase's ZooKeeper
      files that are configured with a relative path will go under this node.
      By default, all of HBase's ZooKeeper file path are configured with a
      relative path, so they will all go under this directory unless changed.
    </description>
  </property>

  <property>
    <name>hbase.defaults.for.version.skip</name>
    <value>true</value>
    <description>Disables version verification.</description>
  </property>

  <property>
    <name>dfs.domain.socket.path</name>
    <value>/var/lib/hadoop-hdfs/dn_socket</value>
    <description>Path to domain socket.</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metainfo.xml
deleted file mode 100644
index afe527deb0..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metainfo.xml
++ /dev/null
@@ -1,44 +0,0 @@
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
    <user>mapred</user>
    <comment>Non-relational distributed database and centralized service for configuration management &amp; synchronization</comment>
    <version>0.96.1.2.0.6.1</version>

    <components>
        <component>
            <name>HBASE_MASTER</name>
            <category>MASTER</category>
        </component>

        <component>
            <name>HBASE_REGIONSERVER</name>
            <category>SLAVE</category>
        </component>

        <component>
            <name>HBASE_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>global</config-type>
      <config-type>hbase-site</config-type>
      <config-type>hbase-policy</config-type>
    </configuration-dependencies>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metrics.json b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metrics.json
deleted file mode 100644
index 37f73bfd20..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HBASE/metrics.json
++ /dev/null
@@ -1,13635 +0,0 @@
{
  "HBASE_REGIONSERVER": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/hbase/regionserver/compactionTime_avg_time": {
            "metric": "hbase.regionserver.compactionTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/mutationsWithoutWALSize": {
            "metric": "regionserver.Server.mutationsWithoutWALSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowAppendCount": {
            "metric": "regionserver.Server.slowAppendCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.multi.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/rootIndexSizeKB": {
            "metric": "hbase.regionserver.rootIndexSizeKB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_num_ops": {
            "metric": "rpc.rpc.getZooKeeper_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheCount": {
            "metric": "regionserver.Server.blockCacheCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_std_dev": {
            "metric": "hbase.regionserver.putRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_std_dev": {
            "metric": "hbase.regionserver.getRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/ping_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.ping_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/commitPending_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.commitPending_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_num_ops": {
            "metric": "rpc.rpc.checkOOME_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_min": {
            "metric": "regionserver.Server.Delete_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.rpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.delete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getMapCompletionEvents_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getMapCompletionEvents_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stop.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_avg_time": {
            "metric": "rpc.rpc.checkOOME_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.next.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_avg_time": {
            "metric": "rpc.rpc.getConfiguration_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.delete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/canCommit_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.canCommit_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.multi.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_75th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_75th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_num_ops": {
            "metric": "regionserver.Server.Delete_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_num_ops": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/writeRequestsCount": {
            "metric": "regionserver.Server.writeRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/canCommit_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.canCommit_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_min": {
            "metric": "regionserver.Server.Get_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Mutate_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitCount": {
            "metric": "regionserver.Server.blockCacheHitCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowPutCount": {
            "metric": "regionserver.Server.slowPutCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatency_num_ops": {
            "metric": "hbase.regionserver.fsWriteLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.exists.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getBlockLocalPathInfo_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getBlockLocalPathInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionSize_num_ops": {
            "metric": "hbase.regionserver.compactionSize_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheSize": {
            "metric": "regionserver.Server.blockCacheSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_num_ops": {
            "metric": "regionserver.Server.Mutate_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_num_ops": {
            "metric": "rpc.rpc.isStopped_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_median": {
            "metric": "regionserver.Server.Mutate_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_num_ops": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_median": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_median",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_std_dev": {
            "metric": "hbase.regionserver.deleteRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/hdfsBlocksLocalityIndex": {
            "metric": "hbase.regionserver.hdfsBlocksLocalityIndex",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/readRequestsCount": {
            "metric": "regionserver.Server.readRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_min": {
            "metric": "regionserver.Server.Mutate_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/storefileIndexSizeMB": {
            "metric": "regionserver.Server.storeFileIndexSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.close.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_median": {
            "metric": "regionserver.Server.Delete_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_mean": {
            "metric": "regionserver.Server.Mutate_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/done_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.done_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionSize_avg_time": {
            "metric": "hbase.regionserver.compactionSize_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_min": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_min",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.increment.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.put.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/statusUpdate_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.statusUpdate_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.put.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitRatio": {
            "metric": "hbase.regionserver.blockCacheHitRatio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_std_dev": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/statusUpdate_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.statusUpdate_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_num_ops": {
            "metric": "rpc.rpc.isAborted_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_max": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_max",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheEvictedCount": {
            "metric": "regionserver.Server.blockCacheEvictionCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.exists.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Mutate_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_max": {
            "metric": "regionserver.Server.Get_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_num_ops": {
            "metric": "rpc.rpc.abort_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_95th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_95th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitCachingRatio": {
            "metric": "hbase.regionserver.blockCacheHitCachingRatio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getProtocolVersion_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Get_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_min": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_min",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getProtocolVersion_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_std_dev": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Delete_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_max": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_max",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getTask_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getTask_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowIncrementCount": {
            "metric": "regionserver.Server.slowIncrementCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Mutate_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionQueueSize": {
            "metric": "regionserver.Server.compactionQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushTime_num_ops": {
            "metric": "hbase.regionserver.flushTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatency_num_ops": {
            "metric": "hbase.regionserver.fsReadLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Get_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_avg_time": {
            "metric": "rpc.rpc.getServerName_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionTime_num_ops": {
            "metric": "hbase.regionserver.compactionTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_avg_time": {
            "metric": "rpc.rpc.abort_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getBlockLocalPathInfo_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getBlockLocalPathInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/stores": {
            "metric": "regionserver.Server.storeCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_num_ops": {
            "metric": "rpc.rpc.getServerName_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.rpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.abort.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_num_ops": {
            "metric": "regionserver.Server.Get_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/ping_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.ping_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatency_avg_time": {
            "metric": "hbase.regionserver.fsReadLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushSize_num_ops": {
            "metric": "hbase.regionserver.flushSize_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_max": {
            "metric": "regionserver.Server.Mutate_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsSyncLatency_num_ops": {
            "metric": "hbase.regionserver.fsSyncLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_95th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_95th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_75th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_75th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_mean": {
            "metric": "regionserver.Server.Get_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.next.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/regions": {
            "metric": "regionserver.Server.regionCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stop.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.abort.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheFree": {
            "metric": "regionserver.Server.blockCacheFreeSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheMissCount": {
            "metric": "regionserver.Server.blockCacheMissCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushQueueSize": {
            "metric": "regionserver.Server.flushQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.close.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_mean": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_mean",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_num_ops": {
            "metric": "rpc.rpc.getConfiguration_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_avg_time": {
            "metric": "rpc.rpc.isStopped_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsSyncLatency_avg_time": {
            "metric": "hbase.regionserver.fsSyncLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_mean": {
            "metric": "regionserver.Server.Delete_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getMapCompletionEvents_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getMapCompletionEvents_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_mean": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_mean",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/totalStaticIndexSizeKB": {
            "metric": "regionserver.Server.staticIndexSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/mutationsWithoutWALCount": {
            "metric": "regionserver.Server.mutationsWithoutWALCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.get.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_median": {
            "metric": "regionserver.Server.Get_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_avg_time": {
            "metric": "rpc.rpc.isAborted_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushSize_avg_time": {
            "metric": "hbase.regionserver.flushSize_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/commitPending_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.commitPending_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_max": {
            "metric": "regionserver.Server.Delete_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.get.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/percentFilesLocal": {
            "metric": "regionserver.Server.percentFilesLocal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatency_avg_time": {
            "metric": "hbase.regionserver.fsWriteLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.increment.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getTask_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getTask_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushTime_avg_time": {
            "metric": "hbase.regionserver.flushTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/done_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.done_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowGetCount": {
            "metric": "regionserver.Server.slowGetCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/requests": {
            "metric": "regionserver.Server.totalRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_99th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_99th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_99th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_99th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/storefiles": {
            "metric": "regionserver.Server.storeFileCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowDeleteCount": {
            "metric": "regionserver.Server.slowDeleteCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_avg_time": {
            "metric": "rpc.rpc.getZooKeeper_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/hlogFileCount": {
            "metric": "hbase.regionserver.hlogFileCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Get_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Delete_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/memstoreSizeMB": {
            "metric": "regionserver.Server.memStoreSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_median": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_median",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Delete_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.rpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/totalStaticBloomSizeKB": {
            "metric": "regionserver.Server.staticBloomSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/hbase/regionserver/slowPutCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowPutCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/percentFilesLocal": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.percentFilesLocal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheFree": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheFreeSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/mutationsWithoutWALSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.mutationsWithoutWALSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheMissCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheMissCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/flushQueueSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.flushQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_99th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowAppendCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowAppendCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowIncrementCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowIncrementCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheEvictedCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheEvictionCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/compactionQueueSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.compactionQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowGetCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowGetCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/readRequestsCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.readRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/storefileIndexSizeMB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeFileIndexSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/totalStaticIndexSizeKB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.staticIndexSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/requests": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.totalRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/storefiles": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeFileCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/mutationsWithoutWALCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.mutationsWithoutWALCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/writeRequestsCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.writeRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowDeleteCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowDeleteCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_99th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/stores": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/memstoreSizeMB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.memStoreSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/regions": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.regionCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/totalStaticBloomSizeKB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.staticBloomSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheHitCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheHitCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_99th_percentile",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/hbase/regionserver/compactionTime_avg_time": {
            "metric": "hbase.regionserver.compactionTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/mutationsWithoutWALSize": {
            "metric": "regionserver.Server.mutationsWithoutWALSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_std_dev": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Delete_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowAppendCount": {
            "metric": "regionserver.Server.slowAppendCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_max": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_max",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowIncrementCount": {
            "metric": "regionserver.Server.slowIncrementCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Mutate_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionQueueSize": {
            "metric": "regionserver.Server.compactionQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.multi.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_num_ops": {
            "metric": "rpc.rpc.getZooKeeper_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/rootIndexSizeKB": {
            "metric": "hbase.regionserver.rootIndexSizeKB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushTime_num_ops": {
            "metric": "hbase.regionserver.flushTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheCount": {
            "metric": "regionserver.Server.blockCacheCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatency_num_ops": {
            "metric": "hbase.regionserver.fsReadLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Get_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_std_dev": {
            "metric": "hbase.regionserver.putRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_std_dev": {
            "metric": "hbase.regionserver.getRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_avg_time": {
            "metric": "rpc.rpc.getServerName_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionTime_num_ops": {
            "metric": "hbase.regionserver.compactionTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_avg_time": {
            "metric": "rpc.rpc.abort_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/stores": {
            "metric": "regionserver.Server.storeCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkOOME_num_ops": {
            "metric": "rpc.rpc.checkOOME_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_num_ops": {
            "metric": "rpc.rpc.getServerName_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.rpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.abort.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_min": {
            "metric": "regionserver.Server.Delete_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.rpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.delete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stop.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.next.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_num_ops": {
            "metric": "regionserver.Server.Get_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkOOME_avg_time": {
            "metric": "rpc.rpc.checkOOME_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_avg_time": {
            "metric": "rpc.rpc.getConfiguration_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatency_avg_time": {
            "metric": "hbase.regionserver.fsReadLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushSize_num_ops": {
            "metric": "hbase.regionserver.flushSize_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.delete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.multi.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_75th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_75th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_max": {
            "metric": "regionserver.Server.Mutate_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_num_ops": {
            "metric": "regionserver.Server.Delete_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/writeRequestsCount": {
            "metric": "regionserver.Server.writeRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_num_ops": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsSyncLatency_num_ops": {
            "metric": "hbase.regionserver.fsSyncLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_95th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_95th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_min": {
            "metric": "regionserver.Server.Get_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_75th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_75th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_mean": {
            "metric": "regionserver.Server.Get_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.next.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Mutate_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/regions": {
            "metric": "regionserver.Server.regionCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitCount": {
            "metric": "regionserver.Server.blockCacheHitCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowPutCount": {
            "metric": "regionserver.Server.slowPutCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stop.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatency_num_ops": {
            "metric": "hbase.regionserver.fsWriteLatency_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.abort.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.exists.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheFree": {
            "metric": "regionserver.Server.blockCacheFreeSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheMissCount": {
            "metric": "regionserver.Server.blockCacheMissCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushQueueSize": {
            "metric": "regionserver.Server.flushQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.close.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_mean": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_mean",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionSize_num_ops": {
            "metric": "hbase.regionserver.compactionSize_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheSize": {
            "metric": "regionserver.Server.blockCacheSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_num_ops": {
            "metric": "regionserver.Server.Mutate_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_num_ops": {
            "metric": "rpc.rpc.getConfiguration_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_avg_time": {
            "metric": "rpc.rpc.isStopped_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_num_ops": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsSyncLatency_avg_time": {
            "metric": "hbase.regionserver.fsSyncLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_median": {
            "metric": "regionserver.Server.Mutate_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/isStopped_num_ops": {
            "metric": "rpc.rpc.isStopped_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_mean": {
            "metric": "regionserver.Server.Delete_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_median": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_median",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_std_dev": {
            "metric": "hbase.regionserver.deleteRequestLatency_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/hdfsBlocksLocalityIndex": {
            "metric": "hbase.regionserver.hdfsBlocksLocalityIndex",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/readRequestsCount": {
            "metric": "regionserver.Server.readRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_min": {
            "metric": "regionserver.Server.Mutate_min",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/storefileIndexSizeMB": {
            "metric": "regionserver.Server.storeFileIndexSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.close.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_mean": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_mean",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_median": {
            "metric": "regionserver.Server.Delete_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/totalStaticIndexSizeKB": {
            "metric": "regionserver.Server.staticIndexSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_mean": {
            "metric": "regionserver.Server.Mutate_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/mutationsWithoutWALCount": {
            "metric": "regionserver.Server.mutationsWithoutWALCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/compactionSize_avg_time": {
            "metric": "hbase.regionserver.compactionSize_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.get.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_median": {
            "metric": "regionserver.Server.Get_median",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_min": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_min",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushSize_avg_time": {
            "metric": "hbase.regionserver.flushSize_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_avg_time": {
            "metric": "rpc.rpc.isAborted_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.increment.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.put.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_max": {
            "metric": "regionserver.Server.Delete_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.get.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/percentFilesLocal": {
            "metric": "regionserver.Server.percentFilesLocal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.put.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatency_avg_time": {
            "metric": "hbase.regionserver.fsWriteLatency_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitRatio": {
            "metric": "hbase.regionserver.blockCacheHitRatio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.increment.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_std_dev": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_std_dev",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_num_ops": {
            "metric": "rpc.rpc.isAborted_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_max": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_max",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/flushTime_avg_time": {
            "metric": "hbase.regionserver.flushTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheEvictedCount": {
            "metric": "regionserver.Server.blockCacheEvictionCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowGetCount": {
            "metric": "regionserver.Server.slowGetCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/requests": {
            "metric": "regionserver.Server.totalRequestCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.exists.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_99th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_99th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_99th_percentile": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_99th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/storefiles": {
            "metric": "regionserver.Server.storeFileCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/slowDeleteCount": {
            "metric": "regionserver.Server.slowDeleteCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/putRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Mutate_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_avg_time": {
            "metric": "rpc.rpc.getZooKeeper_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/hlogFileCount": {
            "metric": "hbase.regionserver.hlogFileCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Delete_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_95th_percentile": {
            "metric": "regionserver.Server.Get_95th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_max": {
            "metric": "regionserver.Server.Get_max",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/memstoreSizeMB": {
            "metric": "regionserver.Server.memStoreSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_median": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_median",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_num_ops": {
            "metric": "rpc.rpc.abort_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsReadLatencyHistogram_95th_percentile": {
            "metric": "hbase.regionserver.fsReadLatencyHistogram_95th_percentile",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/deleteRequestLatency_75th_percentile": {
            "metric": "regionserver.Server.Delete_75th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/blockCacheHitCachingRatio": {
            "metric": "hbase.regionserver.blockCacheHitCachingRatio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.rpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/totalStaticBloomSizeKB": {
            "metric": "regionserver.Server.staticBloomSize",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/regionserver/getRequestLatency_99th_percentile": {
            "metric": "regionserver.Server.Get_99th_percentile",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/hbase/regionserver/fsWriteLatencyHistogram_min": {
            "metric": "hbase.regionserver.fsWriteLatencyHistogram_min",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/hbase/regionserver/slowPutCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowPutCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/percentFilesLocal": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.percentFilesLocal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheFree": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheFreeSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/mutationsWithoutWALSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.mutationsWithoutWALSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheMissCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheMissCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/flushQueueSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.flushQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_99th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowAppendCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowAppendCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowIncrementCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowIncrementCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheEvictedCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheEvictionCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/compactionQueueSize": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.compactionQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowGetCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowGetCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/readRequestsCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.readRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/storefileIndexSizeMB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeFileIndexSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/totalStaticIndexSizeKB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.staticIndexSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_num_ops": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/requests": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.totalRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/storefiles": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeFileCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/mutationsWithoutWALCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.mutationsWithoutWALCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/writeRequestsCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.writeRequestCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_median": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_median",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/slowDeleteCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.slowDeleteCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_99th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/stores": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.storeCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_min": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_95th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_95th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/memstoreSizeMB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.memStoreSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_mean": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/deleteRequestLatency_max": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Delete_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/putRequestLatency_75th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Mutate_75th_percentile",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/regions": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.regionCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/totalStaticBloomSizeKB": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.staticBloomSize",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/blockCacheHitCount": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.blockCacheHitCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/regionserver/getRequestLatency_99th_percentile": {
            "metric": "Hadoop:service=HBase,name=RegionServer,sub=Server.Get_99th_percentile",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ]
  },
  "HBASE_CLIENT": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getProtocolVersion_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getTask_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getTask_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.multi.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_num_ops": {
            "metric": "rpc.rpc.getZooKeeper_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_avg_time": {
            "metric": "rpc.rpc.getServerName_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/ping_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.ping_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_avg_time": {
            "metric": "rpc.rpc.abort_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getBlockLocalPathInfo_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getBlockLocalPathInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/commitPending_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.commitPending_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_num_ops": {
            "metric": "rpc.rpc.checkOOME_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_num_ops": {
            "metric": "rpc.rpc.getServerName_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.RpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.abort.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.RpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.delete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getMapCompletionEvents_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getMapCompletionEvents_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stop.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_avg_time": {
            "metric": "rpc.rpc.checkOOME_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.next.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_avg_time": {
            "metric": "rpc.rpc.getConfiguration_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/ping_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.ping_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.delete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/canCommit_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.canCommit_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.multi.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/canCommit_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.canCommit_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.next.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stop.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.abort.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.exists.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getBlockLocalPathInfo_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getBlockLocalPathInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.close.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_num_ops": {
            "metric": "rpc.rpc.getConfiguration_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_avg_time": {
            "metric": "rpc.rpc.isStopped_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_num_ops": {
            "metric": "rpc.rpc.isStopped_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getMapCompletionEvents_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getMapCompletionEvents_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.close.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.get.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/done_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.done_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_avg_time": {
            "metric": "rpc.rpc.isAborted_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.increment.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/commitPending_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.commitPending_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.put.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.get.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/statusUpdate_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.statusUpdate_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.put.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.increment.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getTask_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.getTask_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/statusUpdate_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.statusUpdate_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_num_ops": {
            "metric": "rpc.rpc.isAborted_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/done_num_ops": {
            "metric": "rpcdetailed.rpcdetailed.done_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.exists.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_avg_time": {
            "metric": "rpc.rpc.getZooKeeper_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_num_ops": {
            "metric": "rpc.rpc.abort_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/getProtocolVersion_avg_time": {
            "metric": "rpcdetailed.rpcdetailed.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.multi.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_num_ops": {
            "metric": "rpc.rpc.getZooKeeper_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_avg_time": {
            "metric": "rpc.rpc.getServerName_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.removeFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_avg_time": {
            "metric": "rpc.rpc.abort_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_num_ops": {
            "metric": "rpc.rpc.checkOOME_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getZooKeeper.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName_num_ops": {
            "metric": "rpc.rpc.getServerName_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.RpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.abort.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.RpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.delete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stop.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME_avg_time": {
            "metric": "rpc.rpc.checkOOME_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.next.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_avg_time": {
            "metric": "rpc.rpc.getConfiguration_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getServerName/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getServerName.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.delete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.multi.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.lockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHServerInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getCatalogTracker.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.next.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isAborted.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stop.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions_num_ops": {
            "metric": "rpc.rpc.addToOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.abort.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.exists.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getCatalogTracker_num_ops": {
            "metric": "rpc.rpc.getCatalogTracker_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.close.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration_num_ops": {
            "metric": "rpc.rpc.getConfiguration_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_avg_time": {
            "metric": "rpc.rpc.isStopped_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped_num_ops": {
            "metric": "rpc.rpc.isStopped_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addToOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addToOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.close.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndDelete.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getFromOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getConfiguration/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getConfiguration.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.get.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.splitRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_avg_time": {
            "metric": "rpc.rpc.isAborted_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.flushRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.increment.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.put.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.get.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.compactRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.openRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openScanner.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unlockRow.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/removeFromOnlineRegions_avg_time": {
            "metric": "rpc.rpc.removeFromOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.put.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isStopped/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isStopped.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.increment.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.openRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getRegionInfo.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getFromOnlineRegions/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getFromOnlineRegions.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isAborted_num_ops": {
            "metric": "rpc.rpc.isAborted_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkOOME/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkOOME.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.exists.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getZooKeeper_avg_time": {
            "metric": "rpc.rpc.getZooKeeper_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.execCoprocessor.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/abort_num_ops": {
            "metric": "rpc.rpc.abort_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.closeRegion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.checkAndPut.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      }
    ]
  },
  "HBASE_MASTER": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpc/deleteTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.deleteTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.regionServerStartup.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClusterStatus.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unassign.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.assign.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.createTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.modifyTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.regionServerReport.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.shutdown.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.balanceSwitch.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.disableTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.assign.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.balance.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitTime_num_ops": {
            "metric": "master.FileSystem.HlogSplitTime_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getAlterStatus.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.deleteColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.rpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.enableTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.rpcAuthorizationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.deleteColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClusterStatus.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.regionServerReport.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unassign.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/offline/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.offline.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.disableTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.modifyColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.enableTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isMasterRunning.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.modifyTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.balance.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitSize_num_ops": {
            "metric": "master.FileSystem.HlogSplitSize_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.modifyColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitTime_avg_time": {
            "metric": "master.FileSystem.HlogSplitTime_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.move.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.shutdown.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitSize_avg_time": {
            "metric": "master.FileSystem.HlogSplitSize_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/cluster_requests": {
            "metric": "master.Server.clusterRequests",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/stopMaster/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stopMaster.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stopMaster.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.regionServerStartup.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getAlterStatus.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.balanceSwitch.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.deleteTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.offline.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isMasterRunning.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.createTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.move.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.rpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/rpc/regionServerReport.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memMaxM":{
            "metric" : "Hadoop:service=HBase,name=JvmMetrics.MemMaxM",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/Revision": {
            "metric": "hadoop:service=HBase,name=Info.revision",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/AverageLoad": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.averageLoad",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ServerName": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.serverName",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeMaxTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ZookeeperQuorum": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.zookeeperQuorum",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsDate": {
            "metric": "hadoop:service=HBase,name=Info.hdfsDate",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsUrl": {
            "metric": "hadoop:service=HBase,name=Info.hdfsUrl",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/revision": {
            "metric": "hadoop:service=HBase,name=Info.revision",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/MasterActiveTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterActiveTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsUser": {
            "metric": "hadoop:service=HBase,name=Info.hdfsUser",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/version": {
            "metric": "hadoop:service=HBase,name=Info.version",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeMaxTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeNumOps": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.NumOpenConnections",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/IsActiveMaster": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.isActiveMaster",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/MasterStartTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterStartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSize_num_ops": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/MasterActiveTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterActiveTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTime_avg_time": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeAvgTime": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/cluster_requests": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.clusterRequests",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/Coprocessors": {
            "metric": "hadoop:service=Master,name=Master.Coprocessors",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/RegionsInTransition": {
            "metric": "hadoop:service=Master,name=Master.RegionsInTransition",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsVersion": {
            "metric": "hadoop:service=HBase,name=Info.hdfsVersion",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/RegionServers": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.numRegionServers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ClusterId": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.clusterId",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeNumOps": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitSizeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicationCallQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicationCallQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeMinTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTime_num_ops": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/RegionsInTransition": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.ritCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/HeapMemoryUsed": {
            "metric": "java.lang:type=Memory.HeapMemoryUsage[used]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.ReceivedBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/NonHeapMemoryMax": {
            "metric": "java.lang:type=Memory.NonHeapMemoryUsage[max]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/DeadRegionServers": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.numDeadRegionServers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/AverageLoad": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.averageLoad",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/MasterStartTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterStartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/priorityCallQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.priorityCallQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/callQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.callQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsRevision": {
            "metric": "hadoop:service=HBase,name=Info.hdfsRevision",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/url": {
            "metric": "hadoop:service=HBase,name=Info.url",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/NonHeapMemoryUsed": {
            "metric": "java.lang:type=Memory.NonHeapMemoryUsage[used]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/HeapMemoryMax": {
            "metric": "java.lang:type=Memory.HeapMemoryUsage[max]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSize_avg_time": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/date": {
            "metric": "hadoop:service=HBase,name=Info.date",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/user": {
            "metric": "java.lang:type=Runtime.SystemProperties.user.name",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/SentBytes": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.SentBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeMinTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeAvgTime": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitSizeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/Version": {
            "metric": "hadoop:service=HBase,name=Info.version",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpc/deleteTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.deleteTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_num_ops": {
            "metric": "rpc.rpc.closeRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.regionServerStartup.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_num_ops": {
            "metric": "rpc.rpc.unassign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getClusterStatus.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_num_ops": {
            "metric": "rpc.rpc.modifyTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.unassign.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_num_ops": {
            "metric": "rpc.rpc.getClosestRowBefore_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.assign.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_num_ops": {
            "metric": "rpc.rpc.replicateLogEntries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.createTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_num_ops": {
            "metric": "rpc.rpc.lockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_avg_time": {
            "metric": "rpc.rpc.flushRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_avg_time": {
            "metric": "rpc.rpc.multi_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_num_ops": {
            "metric": "rpc.rpc.stopMaster_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_avg_time": {
            "metric": "rpc.rpc.balance_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_num_ops": {
            "metric": "rpc.rpc.splitRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_avg_time": {
            "metric": "rpc.rpc.modifyColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance_num_ops": {
            "metric": "rpc.rpc.balance_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_num_ops": {
            "metric": "rpc.rpc.shutdown_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/flushRegion_num_ops": {
            "metric": "rpc.rpc.flushRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.modifyTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.regionServerReport.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_num_ops": {
            "metric": "rpc.rpc.get_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster_avg_time": {
            "metric": "rpc.rpc.stopMaster_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.shutdown.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_avg_time": {
            "metric": "rpc.rpc.getRegionInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.balanceSwitch.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.disableTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.assign.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.balance.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_num_ops": {
            "metric": "rpc.rpc.enableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/lockRow_avg_time": {
            "metric": "rpc.rpc.lockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_avg_time": {
            "metric": "rpc.rpc.addColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_num_ops": {
            "metric": "rpc.rpc.reportRSFatalError_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitTime_num_ops": {
            "metric": "master.FileSystem.HlogSplitTime_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getAlterStatus.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.deleteColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/reportRSFatalError_avg_time": {
            "metric": "rpc.rpc.reportRSFatalError_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.rpc.rpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_avg_time": {
            "metric": "rpc.rpc.disableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_avg_time": {
            "metric": "rpc.rpc.openRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.enableTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_num_ops": {
            "metric": "rpc.rpc.getClusterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.rpc.rpcAuthorizationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_avg_time": {
            "metric": "rpc.rpc.regionServerReport_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_num_ops": {
            "metric": "rpc.rpc.deleteColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_num_ops": {
            "metric": "rpc.rpc.increment_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_num_ops": {
            "metric": "rpc.rpc.getAlterStatus_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn_num_ops": {
            "metric": "rpc.rpc.modifyColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_avg_time": {
            "metric": "rpc.rpc.next_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.deleteColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_avg_time": {
            "metric": "rpc.rpc.RpcSlowResponse_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getClusterStatus.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_num_ops": {
            "metric": "rpc.rpc.bulkLoadHFiles_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign_avg_time": {
            "metric": "rpc.rpc.unassign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.regionServerReport.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unassign/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.unassign.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_avg_time": {
            "metric": "rpc.rpc.balanceSwitch_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_avg_time": {
            "metric": "rpc.rpc.compactRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegion_num_ops": {
            "metric": "rpc.rpc.openRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.callQueueLen",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/offline/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.offline.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_num_ops": {
            "metric": "rpc.rpc.execCoprocessor_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_avg_time": {
            "metric": "rpc.rpc.getOnlineRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.addColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.disableTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_avg_time": {
            "metric": "rpc.rpc.incrementColumnValue_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move_num_ops": {
            "metric": "rpc.rpc.move_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_num_ops": {
            "metric": "rpc.rpc.stop_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/replicateLogEntries_avg_time": {
            "metric": "rpc.rpc.replicateLogEntries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_num_ops": {
            "metric": "rpc.rpc.deleteTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/get_avg_time": {
            "metric": "rpc.rpc.get_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/multi_num_ops": {
            "metric": "rpc.rpc.multi_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_num_ops": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteColumn_avg_time": {
            "metric": "rpc.rpc.deleteColumn_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/bulkLoadHFiles_avg_time": {
            "metric": "rpc.rpc.bulkLoadHFiles_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_avg_time": {
            "metric": "rpc.rpc.exists_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_avg_time": {
            "metric": "rpc.rpc.offline_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_avg_time": {
            "metric": "rpc.rpc.unlockRow_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/delete_num_ops": {
            "metric": "rpc.rpc.delete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/exists_num_ops": {
            "metric": "rpc.rpc.exists_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_avg_time": {
            "metric": "rpc.rpc.regionServerStartup_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_num_ops": {
            "metric": "rpc.rpc.checkAndDelete_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/closeRegion_avg_time": {
            "metric": "rpc.rpc.closeRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.modifyColumn.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.enableTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_avg_time": {
            "metric": "rpc.rpc.getProtocolSignature_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable_num_ops": {
            "metric": "rpc.rpc.createTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_avg_time": {
            "metric": "rpc.rpc.assign_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/execCoprocessor_avg_time": {
            "metric": "rpc.rpc.execCoprocessor_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.isMasterRunning.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_avg_time": {
            "metric": "rpc.rpc.close_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_num_ops": {
            "metric": "rpc.rpc.getHServerInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTime_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stop_avg_time": {
            "metric": "rpc.rpc.stop_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_avg_time": {
            "metric": "rpc.rpc.rollHLogWriter_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.modifyTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balance/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.balance.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_avg_time": {
            "metric": "rpc.rpc.isMasterRunning_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitSize_num_ops": {
            "metric": "master.FileSystem.HlogSplitSize_num_ops",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/incrementColumnValue_num_ops": {
            "metric": "rpc.rpc.incrementColumnValue_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.modifyColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitTime_avg_time": {
            "metric": "master.FileSystem.HlogSplitSize_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/assign_num_ops": {
            "metric": "rpc.rpc.assign_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/enableTable_avg_time": {
            "metric": "rpc.rpc.enableTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.move.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/close_num_ops": {
            "metric": "rpc.rpc.close_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.addColumn.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.shutdown.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_avg_time": {
            "metric": "rpc.rpc.openScanner_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcSlowResponse_num_ops": {
            "metric": "rpc.rpc.RpcSlowResponse_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable_avg_time": {
            "metric": "rpc.rpc.deleteTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/splitSize_avg_time": {
            "metric": "master.FileSystem.HlogSplitSize_mean",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/delete_avg_time": {
            "metric": "rpc.rpc.delete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClosestRowBefore_avg_time": {
            "metric": "rpc.rpc.getClosestRowBefore_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/hbase/master/cluster_requests": {
            "metric": "master.Server.clusterRequests",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/stopMaster/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.stopMaster.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/stopMaster/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.stopMaster.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTime_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_num_ops": {
            "metric": "rpc.rpc.put_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.rpc.rpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/move_avg_time": {
            "metric": "rpc.rpc.move_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getClusterStatus_avg_time": {
            "metric": "rpc.rpc.getClusterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.regionServerStartup.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/modifyTable_avg_time": {
            "metric": "rpc.rpc.modifyTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_avg_time": {
            "metric": "rpc.rpc.checkAndPut_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/put_avg_time": {
            "metric": "rpc.rpc.put_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getHTableDescriptors.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/createTable_avg_time": {
            "metric": "rpc.rpc.createTable_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/addColumn_num_ops": {
            "metric": "rpc.rpc.addColumn_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus_avg_time": {
            "metric": "rpc.rpc.getAlterStatus_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHTableDescriptors_num_ops": {
            "metric": "rpc.rpc.getHTableDescriptors_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getAlterStatus/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.getAlterStatus.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getRegionInfo_num_ops": {
            "metric": "rpc.rpc.getRegionInfo_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/maxMemoryM": {
            "metric": "jvm.metrics.maxMemoryM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/compactRegion_num_ops": {
            "metric": "rpc.rpc.compactRegion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.balanceSwitch.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/deleteTable/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.deleteTable.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getOnlineRegions_num_ops": {
            "metric": "rpc.rpc.getOnlineRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/unlockRow_num_ops": {
            "metric": "rpc.rpc.unlockRow_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/disableTable_num_ops": {
            "metric": "rpc.rpc.disableTable_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/shutdown_avg_time": {
            "metric": "rpc.rpc.shutdown_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openScanner_num_ops": {
            "metric": "rpc.rpc.openScanner_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline/aboveOneSec/_avg_time": {
            "metric": "rpc.rpc.offline.aboveOneSec._avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerStartup_num_ops": {
            "metric": "rpc.rpc.regionServerStartup_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/regionServerReport_num_ops": {
            "metric": "rpc.rpc.regionServerReport_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/openRegions_avg_time": {
            "metric": "rpc.rpc.openRegions_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.isMasterRunning.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/next_num_ops": {
            "metric": "rpc.rpc.next_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/createTable/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.createTable.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getBlockCacheColumnFamilySummaries_avg_time": {
            "metric": "rpc.rpc.getBlockCacheColumnFamilySummaries_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndDelete_avg_time": {
            "metric": "rpc.rpc.checkAndDelete_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getHServerInfo_avg_time": {
            "metric": "rpc.rpc.getHServerInfo_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/move/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.move.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/isMasterRunning_num_ops": {
            "metric": "rpc.rpc.isMasterRunning_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/balanceSwitch_num_ops": {
            "metric": "rpc.rpc.balanceSwitch_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/offline_num_ops": {
            "metric": "rpc.rpc.offline_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolSignature_num_ops": {
            "metric": "rpc.rpc.getProtocolSignature_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/getProtocolVersion/aboveOneSec/_num_ops": {
            "metric": "rpc.rpc.getProtocolVersion.aboveOneSec._num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rollHLogWriter_num_ops": {
            "metric": "rpc.rpc.rollHLogWriter_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.rpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/openRegions_num_ops": {
            "metric": "rpc.rpc.openRegions_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/splitRegion_avg_time": {
            "metric": "rpc.rpc.splitRegion_avg_time",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/checkAndPut_num_ops": {
            "metric": "rpc.rpc.checkAndPut_num_ops",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/increment_avg_time": {
            "metric": "rpc.rpc.increment_avg_time",
            "pointInTime": true,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/rpc/regionServerReport.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memMaxM":{
            "metric" : "Hadoop:service=HBase,name=JvmMetrics.MemMaxM",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/AverageLoad": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.averageLoad",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ServerName": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.serverName",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeMaxTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ZookeeperQuorum": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.zookeeperQuorum",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsDate": {
            "metric": "hadoop:service=HBase,name=Info.hdfsDate",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsUrl": {
            "metric": "hadoop:service=HBase,name=Info.hdfsUrl",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/NonHeapMemoryMax": {
            "metric": "java.lang:type=Memory.NonHeapMemoryUsage[max]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/revision": {
            "metric": "hadoop:service=HBase,name=Info.revision",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/MasterActiveTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterActiveTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsUser": {
            "metric": "hadoop:service=HBase,name=Info.hdfsUser",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/version": {
            "metric": "hadoop:service=HBase,name=Info.version",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeMaxTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_max",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeNumOps": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.NumOpenConnections",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/IsActiveMaster": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.isActiveMaster",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/MasterStartTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.masterStartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSize_num_ops": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTime_avg_time": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeAvgTime": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/multiAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.multiAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/cluster_requests": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.clusterRequests",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/Coprocessors": {
            "metric": "hadoop:service=Master,name=Master.Coprocessors",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/RegionsInTransition": {
            "metric": "hadoop:service=Master,name=Master.RegionsInTransition",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsVersion": {
            "metric": "hadoop:service=HBase,name=Info.hdfsVersion",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getLastFlushTimeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getLastFlushTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatusAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatusAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/RegionServers": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.numRegionServers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/ClusterId": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.tag.clusterId",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcQueueTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatus.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatus.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeNumOps": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitSizeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unlockRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unlockRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/HeapMemoryMax": {
            "metric": "java.lang:type=Memory.HeapMemoryUsage[max]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcSlowResponseMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcSlowResponseMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitchMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitchMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/HeapMemoryUsed": {
            "metric": "java.lang:type=Memory.HeapMemoryUsage[used]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicationCallQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicationCallQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalErrorMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalErrorMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offline.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offline.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTimeMinTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/replicateLogEntriesMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.replicateLogEntriesMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitTime_num_ops": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitTime_num_ops",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReport.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReport.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/execCoprocessorNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.execCoprocessorNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/NonHeapMemoryUsed": {
            "metric": "java.lang:type=Memory.NonHeapMemoryUsage[used]",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rollHLogWriterAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rollHLogWriterAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.ReceivedBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTableNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTableNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClusterStatus.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClusterStatus.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumnAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumnAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/reportRSFatalError.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.reportRSFatalError.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/DeadRegionServers": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.numDeadRegionServers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/appendMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.appendMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/priorityCallQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.priorityCallQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/bulkLoadHFilesNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.bulkLoadHFilesNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/callQueueLen": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.callQueueLen",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassignAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassignAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getAlterStatusNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getAlterStatusNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyColumn.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyColumn.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getCompactionStateNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getCompactionStateNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptorsAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptorsAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMaster.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMaster.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/hdfsRevision": {
            "metric": "hadoop:service=HBase,name=Info.hdfsRevision",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/url": {
            "metric": "hadoop:service=HBase,name=Info.url",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignature.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignature.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/nextMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.nextMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balance.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balance.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHServerInfoNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHServerInfoNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getStoreFileListNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getStoreFileListNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumn.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumn.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitchMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitchMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunningMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunningMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/closeRegionAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.closeRegionAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/disableTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.disableTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assign.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assign.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/moveNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.moveNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementColumnValueMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementColumnValueMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSize_avg_time": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_mean",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/load/AverageLoad": {
            "metric": "hadoop:service=Master,name=Master.AverageLoad",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndDeleteAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndDeleteAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/synchronousBalanceSwitch.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.synchronousBalanceSwitch.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/date": {
            "metric": "hadoop:service=HBase,name=Info.date",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/flushRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.flushRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getOnlineRegionsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getOnlineRegionsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/user": {
            "metric": "java.lang:type=Runtime.SystemProperties.user.name",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getClosestRowBeforeAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getClosestRowBeforeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/offlineNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.offlineNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/SentBytes": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.SentBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/incrementMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.incrementMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/deleteTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.deleteTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/checkAndPutAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.checkAndPutAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openScannerAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openScannerAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/assignMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.assignMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/compactRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.compactRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/openRegionMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.openRegionMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/addColumnMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.addColumnMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTimeNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/existsMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.existsMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeMinTime": {
            "metric": "Hadoop:service=HBase,name=Master,sub=Server.HlogSplitSize_min",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdown.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdown.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTableAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTableAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartup.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartup.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/lockRowNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.lockRowNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolVersion.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolVersion.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/balanceSwitch.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.balanceSwitch.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getProtocolSignatureMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getProtocolSignatureMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/modifyTable.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.modifyTable.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/splitRegionMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.splitRegionMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/mutateRowMinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.mutateRowMinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/hbase/master/splitSizeAvgTime": {
            "metric": "hadoop:service=Master,name=MasterStatistics.splitSizeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerReportAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerReportAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/putAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.putAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getNumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/shutdownMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.shutdownMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getBlockCacheColumnFamilySummariesAvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getBlockCacheColumnFamilySummariesAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.MaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.MaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/regionServerStartupMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.regionServerStartupMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/createTableMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.createTableMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getHTableDescriptors.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getHTableDescriptors.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.rpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/isMasterRunning.aboveOneSec.NumOps": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.isMasterRunning.aboveOneSec.NumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/enableTable.aboveOneSec.AvgTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.enableTable.aboveOneSec.AvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/getRegionInfoMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.getRegionInfoMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/unassign.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.unassign.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/stopMasterMaxTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.stopMasterMaxTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/move.aboveOneSec.MinTime": {
            "metric": "hadoop:service=HBase,name=RPCStatistics.move.aboveOneSec.MinTime",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HCATALOG/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HCATALOG/metainfo.xml
deleted file mode 100644
index 3b165d8e88..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HCATALOG/metainfo.xml
++ /dev/null
@@ -1,30 +0,0 @@
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
    <user>root</user>
    <comment>This is comment for HCATALOG service</comment>
    <version>0.12.0.2.0.6.0</version>

    <components>
        <component>
            <name>HCAT</name>
            <category>CLIENT</category>
        </component>
    </components>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HDFS/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HDFS/metainfo.xml
index 2ca6c10f66..edada6479f 100644
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HDFS/metainfo.xml
++ b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HDFS/metainfo.xml
@@ -16,45 +16,37 @@
    limitations under the License.
 -->
 <metainfo>
    <user>root</user>
    <comment>Apache Hadoop Distributed File System</comment>
    <version>2.2.0.2.0.6.0</version>

    <components>
        <component>
            <name>NAMENODE</name>
            <category>MASTER</category>
        </component>

  <schemaVersion>2.0</schemaVersion>
  <services>
    <service>
      <name>GLUSTERFS</name>
      <comment>An Hadoop Compatible File System</comment>
      <version>2.2.0.2.0.6.0</version>
      <components>
         <component>
            <name>DATANODE</name>
            <category>SLAVE</category>
          <name>GLUSTERFS_CLIENT</name>
          <category>CLIENT</category>
          <commandScript>
            <script>scripts/glusterfs_client.py</script>
            <scriptType>PYTHON</scriptType>
            <timeout>600</timeout>
          </commandScript>
         </component>
      </components>
 
        <component>
            <name>SECONDARY_NAMENODE</name>
            <category>MASTER</category>
        </component>
      <commandScript>
        <script>scripts/service_check.py</script>
        <scriptType>PYTHON</scriptType>
        <timeout>300</timeout>
      </commandScript>
 
        <component>
            <name>HDFS_CLIENT</name>
            <category>CLIENT</category>
        </component>
        
        <component>
            <name>JOURNALNODE</name>
            <category>MASTER</category>
        </component>
      <configuration-dependencies>
        <config-type>core-site</config-type>
        <config-type>global</config-type>
        <config-type>hadoop-policy</config-type>
        <config-type>hdfs-site</config-type>
      </configuration-dependencies>
 
        <component>
          <name>ZKFC</name>
          <category>SLAVE</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>core-site</config-type>
      <config-type>global</config-type>
      <config-type>hdfs-site</config-type>
      <config-type>hadoop-policy</config-type>
    </configuration-dependencies>
    </service>
  </services>
 </metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/configuration/hive-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/configuration/hive-site.xml
deleted file mode 100644
index a23ce50cf2..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/configuration/hive-site.xml
++ /dev/null
@@ -1,261 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<configuration>

  <property>
    <name>ambari.hive.db.schema.name</name>
    <value>hive</value>
    <description>Database name used as the Hive Metastore</description>
  </property>

  <property>
    <name>javax.jdo.option.ConnectionURL</name>
    <value>jdbc</value>
    <description>JDBC connect string for a JDBC metastore</description>
  </property>

  <property>
    <name>javax.jdo.option.ConnectionDriverName</name>
    <value>com.mysql.jdbc.Driver</value>
    <description>Driver class name for a JDBC metastore</description>
  </property>

  <property>
    <name>javax.jdo.option.ConnectionUserName</name>
    <value>hive</value>
    <description>username to use against metastore database</description>
  </property>

  <property>
    <name>javax.jdo.option.ConnectionPassword</name>
    <value> </value>
    <description>password to use against metastore database</description>
  </property>

  <property>
    <name>hive.metastore.warehouse.dir</name>
    <value>/apps/hive/warehouse</value>
    <description>location of default database for the warehouse</description>
  </property>

  <property>
    <name>hive.metastore.sasl.enabled</name>
    <value></value>
    <description>If true, the metastore thrift interface will be secured with SASL.
     Clients must authenticate with Kerberos.</description>
  </property>

  <property>
    <name>hive.metastore.kerberos.keytab.file</name>
    <value></value>
    <description>The path to the Kerberos Keytab file containing the metastore
     thrift server's service principal.</description>
  </property>

  <property>
    <name>hive.metastore.kerberos.principal</name>
    <value></value>
    <description>The service principal for the metastore thrift server. The special
    string _HOST will be replaced automatically with the correct host name.</description>
  </property>

  <property>
    <name>hive.metastore.cache.pinobjtypes</name>
    <value>Table,Database,Type,FieldSchema,Order</value>
    <description>List of comma separated metastore object types that should be pinned in the cache</description>
  </property>

  <property>
    <name>hive.metastore.uris</name>
    <value>thrift://localhost:9083</value>
    <description>URI for client to contact metastore server</description>
  </property>

  <property>
    <name>hive.metastore.client.socket.timeout</name>
    <value>60</value>
    <description>MetaStore Client socket timeout in seconds</description>
  </property>

  <property>
    <name>hive.metastore.execute.setugi</name>
    <value>true</value>
    <description>In unsecure mode, setting this property to true will cause the metastore to execute DFS operations using the client's reported user and group permissions. Note that this property must be set on both the client and     server sides. Further note that its best effort. If client sets its to true and server sets it to false, client setting will be ignored.</description>
  </property>

  <property>
    <name>hive.security.authorization.enabled</name>
    <value>false</value>
    <description>enable or disable the hive client authorization</description>
  </property>

  <property>
    <name>hive.security.authorization.manager</name>
    <value>org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider</value>
    <description>the hive client authorization manager class name.
    The user defined authorization class should implement interface org.apache.hadoop.hive.ql.security.authorization.HiveAuthorizationProvider.  </description>
  </property>

  <property>
    <name>hive.security.metastore.authorization.manager</name>
    <value>org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider</value>
    <description>The authorization manager class name to be used in the metastore for authorization. The user-defined authorization class should implement interface org.apache.hadoop.hive.ql.security.authorization.HiveMetastoreAuthorizationProvider.  </description>
  </property>

  <property>
    <name>hive.security.authenticator.manager</name>
    <value>org.apache.hadoop.hive.ql.security.ProxyUserAuthenticator</value>
    <description>Hive client authenticator manager class name. The user-defined authenticator class should implement interface org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider.  </description>
  </property>

  <property>
    <name>hive.server2.enable.doAs</name>
    <value>true</value>
  </property>

  <property>
    <name>fs.hdfs.impl.disable.cache</name>
    <value>true</value>
  </property>

  <property>
    <name>fs.file.impl.disable.cache</name>
    <value>true</value>
  </property>

  <property>
    <name>hive.enforce.bucketing</name>
    <value>true</value>
    <description>Whether bucketing is enforced. If true, while inserting into the table, bucketing is enforced.</description>
  </property>

  <property>
    <name>hive.enforce.sorting</name>
    <value>true</value>
    <description>Whether sorting is enforced. If true, while inserting into the table, sorting is enforced.</description>
  </property>

  <property>
    <name>hive.map.aggr</name>
    <value>true</value>
    <description>Whether to use map-side aggregation in Hive Group By queries.</description>
  </property>

  <property>
    <name>hive.optimize.bucketmapjoin</name>
    <value>true</value>
  </property>

  <property>
    <name>hive.optimize.bucketmapjoin.sortedmerge</name>
    <value>true</value>
  </property>

  <property>
    <name>hive.mapred.reduce.tasks.speculative.execution</name>
    <value>false</value>
    <description>Whether speculative execution for reducers should be turned on.</description>
  </property>

  <property>
    <name>hive.auto.convert.join</name>
    <value>true</value>
    <description>Whether Hive enable the optimization about converting common
      join into mapjoin based on the input file size.</description>
  </property>

  <property>
    <name>hive.auto.convert.sortmerge.join</name>
    <value>true</value>
    <description>Will the join be automatically converted to a sort-merge join, if the joined tables pass
      the criteria for sort-merge join.
    </description>
  </property>

  <property>
    <name>hive.auto.convert.sortmerge.join.noconditionaltask</name>
    <value>true</value>
  </property>

  <property>
    <name>hive.auto.convert.join.noconditionaltask</name>
    <value>true</value>
    <description>Whether Hive enable the optimization about converting common join into mapjoin based on the input file
      size. If this paramater is on, and the sum of size for n-1 of the tables/partitions for a n-way join is smaller than the
      specified size, the join is directly converted to a mapjoin (there is no conditional task).
    </description>
  </property>

  <property>
    <name>hive.auto.convert.join.noconditionaltask.size</name>
    <value>1000000000</value>
    <description>If hive.auto.convert.join.noconditionaltask is off, this parameter does not take affect. However, if it
      is on, and the sum of size for n-1 of the tables/partitions for a n-way join is smaller than this size, the join is directly
      converted to a mapjoin(there is no conditional task). The default is 10MB.
    </description>
  </property>

  <property>
    <name>hive.optimize.reducededuplication.min.reducer</name>
    <value>1</value>
    <description>Reduce deduplication merges two RSs by moving key/parts/reducer-num of the child RS to parent RS.
      That means if reducer-num of the child RS is fixed (order by or forced bucketing) and small, it can make very slow, single MR.
      The optimization will be disabled if number of reducers is less than specified value.
    </description>
  </property>

  <property>
    <name>hive.optimize.mapjoin.mapreduce</name>
    <value>true</value>
    <description>If hive.auto.convert.join is off, this parameter does not take
      affect. If it is on, and if there are map-join jobs followed by a map-reduce
      job (for e.g a group by), each map-only job is merged with the following
      map-reduce job.
    </description>
  </property>

  <property>
    <name>hive.mapjoin.bucket.cache.size</name>
    <value>10000</value>
    <description>
      Size per reducer.The default is 1G, i.e if the input size is 10G, it
      will use 10 reducers.
    </description>
  </property>

  <property>
    <name>hive.vectorized.execution.enabled</name>
    <value>false</value>
  </property>

  <property>
    <name>hive.optimize.reducededuplication</name>
    <value>true</value>
  </property>

  <property>
    <name>hive.optimize.index.filter</name>
    <value>true</value>
    <description>
    Whether to enable automatic use of indexes
    </description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/metainfo.xml
deleted file mode 100644
index f13193fe15..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/HIVE/metainfo.xml
++ /dev/null
@@ -1,45 +0,0 @@
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
    <user>root</user>
    <comment>Data warehouse system for ad-hoc queries &amp; analysis of large datasets and table &amp; storage management service</comment>
    <version>0.12.0.2.0.6.1</version>

    <components>        
        <component>
            <name>HIVE_METASTORE</name>
            <category>MASTER</category>
        </component>
        <component>
            <name>HIVE_SERVER</name>
            <category>MASTER</category>
        </component>
        <component>
            <name>MYSQL_SERVER</name>
            <category>MASTER</category>
        </component>
        <component>
            <name>HIVE_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>global</config-type>
      <config-type>hive-site</config-type>
    </configuration-dependencies>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/container-executor.cfg b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/container-executor.cfg
deleted file mode 100644
index d07b3db98e..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/container-executor.cfg
++ /dev/null
@@ -1,20 +0,0 @@
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
yarn.nodemanager.local-dirs=TODO-YARN-LOCAL-DIR
yarn.nodemanager.linux-container-executor.group=hadoop
yarn.nodemanager.log-dirs=TODO-YARN-LOG-DIR
banned.users=hdfs,bin,0
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/core-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/core-site.xml
deleted file mode 100644
index 3a2af49059..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/core-site.xml
++ /dev/null
@@ -1,20 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
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
<configuration>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/global.xml
deleted file mode 100644
index ceedd5629c..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/global.xml
++ /dev/null
@@ -1,44 +0,0 @@
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
    <name>hs_host</name>
    <value></value>
    <description>History Server.</description>
  </property>
  <property>
    <name>mapred_log_dir_prefix</name>
    <value>/var/log/hadoop-mapreduce</value>
    <description>Mapreduce Log Dir Prefix</description>
  </property>
  <property>
    <name>mapred_pid_dir_prefix</name>
    <value>/var/run/hadoop-mapreduce</value>
    <description>Mapreduce PID Dir Prefix</description>
  </property>
  <property>
    <name>mapred_user</name>
    <value>mapred</value>
    <description>Mapreduce User</description>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-queue-acls.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-queue-acls.xml
deleted file mode 100644
index ce12380767..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-queue-acls.xml
++ /dev/null
@@ -1,39 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

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

<!-- mapred-queue-acls.xml -->
<configuration>


<!-- queue default -->

  <property>
    <name>mapred.queue.default.acl-submit-job</name>
    <value>*</value>
  </property>

  <property>
    <name>mapred.queue.default.acl-administer-jobs</name>
    <value>*</value>
  </property>

  <!-- END ACLs -->

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-site.xml
deleted file mode 100644
index 4fafce5f5a..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/configuration/mapred-site.xml
++ /dev/null
@@ -1,381 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

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

<!-- Put site-specific property overrides in this file. -->

<configuration xmlns:xi="http://www.w3.org/2001/XInclude">

<!-- i/o properties -->

  <property>
    <name>mapreduce.task.io.sort.mb</name>
    <value>200</value>
    <description>
      The total amount of buffer memory to use while sorting files, in megabytes.
      By default, gives each merge stream 1MB, which should minimize seeks.
    </description>
  </property>

  <property>
    <name>mapreduce.map.sort.spill.percent</name>
    <value>0.7</value>
    <description>
      The soft limit in the serialization buffer. Once reached, a thread will
      begin to spill the contents to disk in the background. Note that
      collection will not block if this threshold is exceeded while a spill
      is already in progress, so spills may be larger than this threshold when
      it is set to less than .5
    </description>
  </property>

  <property>
    <name>mapreduce.task.io.sort.factor</name>
    <value>100</value>
    <description>
      The number of streams to merge at once while sorting files.
      This determines the number of open file handles.
    </description>
  </property>

<!-- map/reduce properties -->
  <property>
    <name>mapreduce.cluster.administrators</name>
    <value> hadoop</value>
    <description>
      Administrators for MapReduce applications.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.shuffle.parallelcopies</name>
    <value>30</value>
    <description>
      The default number of parallel transfers run by reduce during
      the copy(shuffle) phase.
    </description>
  </property>

  <property>
    <name>mapreduce.map.speculative</name>
    <value>false</value>
    <description>
      If true, then multiple instances of some map tasks
      may be executed in parallel.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.speculative</name>
    <value>false</value>
    <description>
      If true, then multiple instances of some reduce tasks may be
      executed in parallel.
    </description>
  </property>

  <property>
    <name>mapreduce.job.reduce.slowstart.completedmaps</name>
    <value>0.05</value>
    <description>
      Fraction of the number of maps in the job which should be complete before
      reduces are scheduled for the job.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.shuffle.merge.percent</name>
    <value>0.66</value>
    <description>
      The usage threshold at which an in-memory merge will be
      initiated, expressed as a percentage of the total memory allocated to
      storing in-memory map outputs, as defined by
      mapreduce.reduce.shuffle.input.buffer.percent.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.shuffle.input.buffer.percent</name>
    <value>0.7</value>
    <description>
      The percentage of memory to be allocated from the maximum heap
      size to storing map outputs during the shuffle.
    </description>
  </property>

  <property>
    <name>mapreduce.map.output.compress.codec</name>
    <value></value>
    <description>If the map outputs are compressed, how should they be
      compressed
    </description>
  </property>

  <property>
    <name>mapreduce.output.fileoutputformat.compress.type</name>
    <value>BLOCK</value>
    <description>
      If the job outputs are to compressed as SequenceFiles, how should
      they be compressed? Should be one of NONE, RECORD or BLOCK.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.input.buffer.percent</name>
    <value>0.0</value>
    <description>
      The percentage of memory- relative to the maximum heap size- to
      retain map outputs during the reduce. When the shuffle is concluded, any
      remaining map outputs in memory must consume less than this threshold before
      the reduce can begin.
    </description>
  </property>

  <!-- copied from kryptonite configuration -->
  <property>
    <name>mapreduce.map.output.compress</name>
    <value>false</value>
  </property>

  <property>
    <name>mapreduce.task.timeout</name>
    <value>300000</value>
    <description>
      The number of milliseconds before a task will be
      terminated if it neither reads an input, writes an output, nor
      updates its status string.
    </description>
  </property>

  <property>
    <name>mapreduce.map.memory.mb</name>
    <value>1024</value>
    <description>Virtual memory for single Map task</description>
  </property>

  <property>
    <name>mapreduce.reduce.memory.mb</name>
    <value>1024</value>
    <description>Virtual memory for single Reduce task</description>
  </property>

  <property>
    <name>mapreduce.jobhistory.keytab.file</name>
    <!-- cluster variant -->
    <value></value>
    <description>The keytab for the job history server principal.</description>
  </property>

  <property>
    <name>mapreduce.shuffle.port</name>
    <value>13562</value>
    <description>
      Default port that the ShuffleHandler will run on.
      ShuffleHandler is a service run at the NodeManager to facilitate
      transfers of intermediate Map outputs to requesting Reducers.
    </description>
  </property>

  <property>
    <name>mapreduce.jobhistory.intermediate-done-dir</name>
    <value>glusterfs:///mr-history/tmp</value>
    <description>
      Directory where history files are written by MapReduce jobs.
    </description>
  </property>

  <property>
    <name>mapreduce.jobhistory.done-dir</name>
    <value>glusterfs:///mr-history/done</value>
    <description>
      Directory where history files are managed by the MR JobHistory Server.
    </description>
  </property>

  <property>       
    <name>mapreduce.jobhistory.address</name>
    <value>localhost:10020</value>
    <description>Enter your JobHistoryServer hostname.</description>
  </property>

  <property>       
    <name>mapreduce.jobhistory.webapp.address</name>
    <value>localhost:19888</value>
    <description>Enter your JobHistoryServer hostname.</description>
  </property>

  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
    <description>
      The runtime framework for executing MapReduce jobs. Can be one of local,
      classic or yarn.
    </description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.staging-dir</name>
    <value>glusterfs:///tmp/hadoop-yarn/staging/mapred/.staging</value>
    <description>
      The staging dir used while submitting jobs.
    </description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.resource.mb</name>
    <value>512</value>
    <description>The amount of memory the MR AppMaster needs.</description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.command-opts</name>
    <value>-Xmx312m</value>
    <description>
      Java opts for the MR App Master processes.
      The following symbol, if present, will be interpolated: @taskid@ is replaced
      by current TaskID. Any other occurrences of '@' will go unchanged.
      For example, to enable verbose gc logging to a file named for the taskid in
      /tmp and to set the heap maximum to be a gigabyte, pass a 'value' of:
      -Xmx1024m -verbose:gc -Xloggc:/tmp/@taskid@.gc

      Usage of -Djava.library.path can cause programs to no longer function if
      hadoop native libraries are used. These values should instead be set as part
      of LD_LIBRARY_PATH in the map / reduce JVM env using the mapreduce.map.env and
      mapreduce.reduce.env config settings.
    </description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.admin-command-opts</name>
    <value>-server -Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN</value>
    <description>
      Java opts for the MR App Master processes for admin purposes.
      It will appears before the opts set by yarn.app.mapreduce.am.command-opts and
      thus its options can be overridden user.

      Usage of -Djava.library.path can cause programs to no longer function if
      hadoop native libraries are used. These values should instead be set as part
      of LD_LIBRARY_PATH in the map / reduce JVM env using the mapreduce.map.env and
      mapreduce.reduce.env config settings.
    </description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.log.level</name>
    <value>INFO</value>
    <description>MR App Master process log level.</description>
  </property>

  <property>
    <name>yarn.app.mapreduce.am.env</name>
    <value></value>
    <description>
      User added environment variables for the MR App Master
      processes. Example :
      1) A=foo  This will set the env variable A to foo
      2) B=$B:c This is inherit tasktracker's B env variable.
    </description>
  </property>

  <property>
    <name>mapreduce.admin.map.child.java.opts</name>
    <value>-server -Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN</value>
  </property>

  <property>
    <name>mapreduce.admin.reduce.child.java.opts</name>
    <value>-server -Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN</value>
  </property>

  <property>
    <name>mapreduce.application.classpath</name>
    <value>$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*,$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*</value>
    <description>
      CLASSPATH for MR applications. A comma-separated list of CLASSPATH
      entries.
    </description>
  </property>

  <property>
    <name>mapreduce.am.max-attempts</name>
    <value>2</value>
    <description>
      The maximum number of application attempts. It is a
      application-specific setting. It should not be larger than the global number
      set by resourcemanager. Otherwise, it will be override. The default number is
      set to 2, to allow at least one retry for AM.
    </description>
  </property>



  <property>
    <name>mapreduce.map.java.opts</name>
    <value>-Xmx756m</value>
    <description>
      Larger heap-size for child jvms of maps.
    </description>
  </property>


  <property>
    <name>mapreduce.reduce.java.opts</name>
    <value>-Xmx756m</value>
    <description>
      Larger heap-size for child jvms of reduces.
    </description>
  </property>

  <property>
    <name>mapreduce.map.log.level</name>
    <value>INFO</value>
    <description>
      The logging level for the map task. The allowed levels are:
      OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE and ALL.
    </description>
  </property>

  <property>
    <name>mapreduce.reduce.log.level</name>
    <value>INFO</value>
    <description>
      The logging level for the reduce task. The allowed levels are:
      OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE and ALL.
    </description>
  </property>

  <property>
    <name>mapreduce.admin.user.env</name>
    <value>LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`</value>
    <description>
      Additional execution environment entries for map and reduce task processes.
      This is not an additive property. You must preserve the original value if
      you want your map and reduce tasks to have access to native libraries (compression, etc)
    </description>
  </property>

  <property>
    <name>mapreduce.output.fileoutputformat.compress</name>
    <value>false</value>
    <description>
      Should the job outputs be compressed?
    </description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metainfo.xml
deleted file mode 100644
index ddaa979b01..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metainfo.xml
++ /dev/null
@@ -1,37 +0,0 @@
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
    <user>mapred</user>
    <comment>Apache Hadoop NextGen MapReduce (client libraries)</comment>
    <version>2.2.0.2.0.6.0</version>
    <components>
        <component>
            <name>HISTORYSERVER</name>
            <category>MASTER</category>
        </component>
        <component>
            <name>MAPREDUCE2_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>core-site</config-type>
      <config-type>global</config-type>
      <config-type>mapred-site</config-type>
    </configuration-dependencies>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metrics.json b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metrics.json
deleted file mode 100644
index b611ed63c3..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/MAPREDUCE2/metrics.json
++ /dev/null
@@ -1,383 +0,0 @@
{
  "HISTORYSERVER": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.metrics.RpcAuthorizationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.ugi.LoginSuccessAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.ugi.LoginSuccessNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.CallQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.ugi.LoginFailureNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.ugi.LoginFailureAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.metrics.RpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.metrics.RpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "jmx",
        "metrics": {
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logWarn": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcCount": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsNew": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logError": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logFatal": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logInfo": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "Hadoop:service=JobHistoryServer,name=JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/configuration/global.xml
deleted file mode 100644
index 9c420bd369..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/configuration/global.xml
++ /dev/null
@@ -1,51 +0,0 @@
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
    <name>nagios_contact</name>
    <value></value>
    <description>Email address.</description>
  </property>
  <property>
    <name>nagios_group</name>
    <value>nagios</value>
    <description></description>
  </property>
  <property>
    <name>nagios_user</name>
    <value>nagios</value>
    <description>Nagios process user.</description>
  </property>
  <property>
    <name>nagios_web_login</name>
    <value>nagiosadmin</value>
    <description>Web user name.</description>
  </property>
  <property>
    <name>nagios_web_password</name>
    <value></value>
    <description></description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/metainfo.xml
deleted file mode 100644
index 42bee82f1f..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/NAGIOS/metainfo.xml
++ /dev/null
@@ -1,34 +0,0 @@
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
    <user>root</user>
    <comment>Nagios Monitoring and Alerting system</comment>
    <version>3.5.0</version>

    <components>
        <component>
            <name>NAGIOS_SERVER</name>
            <category>MASTER</category>
        </component>
    </components>

  <configuration-dependencies>
    <config-type>global</config-type>
  </configuration-dependencies>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/configuration/oozie-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/configuration/oozie-site.xml
deleted file mode 100644
index 59defc384d..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/configuration/oozie-site.xml
++ /dev/null
@@ -1,313 +0,0 @@
<?xml version="1.0"?>
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

<configuration>

<!--
    Refer to the oozie-default.xml file for the complete list of
    Oozie configuration properties and their default values.
-->
  <property>
    <name>oozie.base.url</name>
    <value>http://localhost:11000/oozie</value>
    <description>Base Oozie URL.</description>
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
     <value>true</value>
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
     <value>1000</value>
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
     <name>oozie.service.coord.normal.default.timeout</name>
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
      <name>oozie.service.HadoopAccessorService.jobTracker.whitelist</name>
      <value> </value>
      <description>
      Whitelisted job tracker for Oozie service.
      </description>
    </property>
   
    <property>
      <name>oozie.authentication.type</name>
      <value>simple</value>
      <description>
      </description>
    </property>
   
    <property>
      <name>oozie.service.HadoopAccessorService.nameNode.whitelist</name>
      <value> </value>
      <description>
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
      <name>oozie.authentication.kerberos.name.rules</name>
      <value>
        RULE:[2:$1@$0]([jt]t@.*TODO-KERBEROS-DOMAIN)s/.*/TODO-MAPREDUSER/
        RULE:[2:$1@$0]([nd]n@.*TODO-KERBEROS-DOMAIN)s/.*/TODO-HDFSUSER/
        RULE:[2:$1@$0](hm@.*TODO-KERBEROS-DOMAIN)s/.*/TODO-HBASE-USER/
        RULE:[2:$1@$0](rs@.*TODO-KERBEROS-DOMAIN)s/.*/TODO-HBASE-USER/
        DEFAULT
        </value>
      <description>The mapping from kerberos principal names to local OS user names.</description>
    </property>
    <property>
      <name>oozie.service.HadoopAccessorService.hadoop.configurations</name>
      <value>*=/etc/hadoop/conf</value>
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
        <name>oozie.service.ActionService.executor.ext.classes</name>
        <value>
            org.apache.oozie.action.email.EmailActionExecutor,
            org.apache.oozie.action.hadoop.HiveActionExecutor,
            org.apache.oozie.action.hadoop.ShellActionExecutor,
            org.apache.oozie.action.hadoop.SqoopActionExecutor,
            org.apache.oozie.action.hadoop.DistcpActionExecutor
        </value>
    </property>

    <property>
        <name>oozie.service.SchemaService.wf.ext.schemas</name>
        <value>shell-action-0.1.xsd,email-action-0.1.xsd,hive-action-0.2.xsd,sqoop-action-0.2.xsd,ssh-action-0.1.xsd,distcp-action-0.1.xsd,shell-action-0.2.xsd,oozie-sla-0.1.xsd,oozie-sla-0.2.xsd,hive-action-0.3.xsd</value>
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
        <value>oozie</value>
        <description>
          Database user name to use to connect to the database
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
      <name>oozie.services</name>
      <value>
        org.apache.oozie.service.SchedulerService,
        org.apache.oozie.service.InstrumentationService,
        org.apache.oozie.service.CallableQueueService,
        org.apache.oozie.service.UUIDService,
        org.apache.oozie.service.ELService,
        org.apache.oozie.service.AuthorizationService,
        org.apache.oozie.service.UserGroupInformationService,
        org.apache.oozie.service.HadoopAccessorService,
        org.apache.oozie.service.URIHandlerService,
        org.apache.oozie.service.MemoryLocksService,
        org.apache.oozie.service.DagXLogInfoService,
        org.apache.oozie.service.SchemaService,
        org.apache.oozie.service.LiteWorkflowAppService,
        org.apache.oozie.service.JPAService,
        org.apache.oozie.service.StoreService,
        org.apache.oozie.service.CoordinatorStoreService,
        org.apache.oozie.service.SLAStoreService,
        org.apache.oozie.service.DBLiteWorkflowStoreService,
        org.apache.oozie.service.CallbackService,
        org.apache.oozie.service.ActionService,
        org.apache.oozie.service.ActionCheckerService,
        org.apache.oozie.service.RecoveryService,
        org.apache.oozie.service.PurgeService,
        org.apache.oozie.service.CoordinatorEngineService,
        org.apache.oozie.service.BundleEngineService,
        org.apache.oozie.service.DagEngineService,
        org.apache.oozie.service.CoordMaterializeTriggerService,
        org.apache.oozie.service.StatusTransitService,
        org.apache.oozie.service.PauseTransitService,
        org.apache.oozie.service.GroupsService,
        org.apache.oozie.service.ProxyUserService
      </value>
      <description>List of Oozie services</description>
    </property>
    <property>
      <name>oozie.service.URIHandlerService.uri.handlers</name>
      <value>org.apache.oozie.dependency.FSURIHandler,org.apache.oozie.dependency.HCatURIHandler</value>
      <description>
        Enlist the different uri handlers supported for data availability checks.
      </description>
    </property>
    <property>
    <name>oozie.services.ext</name>
    <value>org.apache.oozie.service.PartitionDependencyManagerService,org.apache.oozie.service.HCatAccessorService</value>
    <description>
       To add/replace services defined in 'oozie.services' with custom implementations.
       Class names must be separated by commas.
    </description>
    </property>
    <property>
    <name>oozie.service.coord.push.check.requeue.interval</name>
    <value>30000</value>
    <description>
        Command re-queue interval for push dependencies (in millisecond).
    </description>
    </property>
    <property>
      <name>oozie.credentials.credentialclasses</name>
      <value>hcat=org.apache.oozie.action.hadoop.HCatCredentials</value>
      <description>
        Credential Class to be used for HCat.
      </description>
    </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/metainfo.xml
deleted file mode 100644
index 515e6696bf..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/OOZIE/metainfo.xml
++ /dev/null
@@ -1,38 +0,0 @@
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
    <user>root</user>
    <comment>System for workflow coordination and execution of Apache Hadoop jobs.  This also includes the installation of the optional Oozie Web Console which relies on and will install the &lt;a target="_blank" href="http://www.sencha.com/products/extjs/license/"&gt;ExtJS&lt;/a&gt; Library.</comment>
    <version>4.0.0.2.0.6.0</version>

    <components>
        <component>
            <name>OOZIE_SERVER</name>
            <category>MASTER</category>
        </component>

        <component>
            <name>OOZIE_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>global</config-type>
      <config-type>oozie-site</config-type>
    </configuration-dependencies>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/configuration/pig.properties b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/configuration/pig.properties
deleted file mode 100644
index 01000b53ab..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/configuration/pig.properties
++ /dev/null
@@ -1,52 +0,0 @@
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Pig default configuration file. All values can be overwritten by pig.properties and command line arguments.
# see bin/pig -help

# brief logging (no timestamps)
brief=false

#debug level, INFO is default
debug=INFO

#verbose print all log messages to screen (default to print only INFO and above to screen)
verbose=false

#exectype local|mapreduce, mapreduce is default
exectype=mapreduce

#Enable insertion of information about script into hadoop job conf 
pig.script.info.enabled=true

#Do not spill temp files smaller than this size (bytes)
pig.spill.size.threshold=5000000
#EXPERIMENT: Activate garbage collection when spilling a file bigger than this size (bytes)
#This should help reduce the number of files being spilled.
pig.spill.gc.activation.size=40000000

#the following two parameters are to help estimate the reducer number
pig.exec.reducers.bytes.per.reducer=1000000000
pig.exec.reducers.max=999

#Temporary location to store the intermediate data.
pig.temp.dir=/tmp/

#Threshold for merging FRJoin fragment files
pig.files.concatenation.threshold=100
pig.optimistic.files.concatenation=false;

pig.disable.counter=false
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/metainfo.xml
deleted file mode 100644
index 44e9cda516..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/PIG/metainfo.xml
++ /dev/null
@@ -1,30 +0,0 @@
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
    <user>root</user>
    <comment>Scripting platform for analyzing large datasets</comment>
    <version>0.12.0.2.0.6.0</version>

    <components>
        <component>
            <name>PIG</name>
            <category>CLIENT</category>
        </component>
    </components>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/SQOOP/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/SQOOP/metainfo.xml
deleted file mode 100644
index 9a50700eba..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/SQOOP/metainfo.xml
++ /dev/null
@@ -1,30 +0,0 @@
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
    <user>root</user>
    <comment>Tool for transferring bulk data between Apache Hadoop and structured data stores such as relational databases</comment>
    <version>1.4.4.2.0.6.0</version>

    <components>
        <component>
            <name>SQOOP</name>
            <category>CLIENT</category>
        </component>
    </components>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/global.xml
deleted file mode 100644
index fed9c6f107..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/global.xml
++ /dev/null
@@ -1,51 +0,0 @@
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
    <name>hcat_conf_dir</name>
    <value></value>
    <description></description>
  </property>
  <property>
    <name>hcat_log_dir</name>
    <value>/var/log/webhcat</value>
    <description></description>
  </property>
  <property>
    <name>hcat_pid_dir</name>
    <value>/var/run/webhcat</value>
    <description></description>
  </property>
  <property>
    <name>hcat_user</name>
    <value>hcat</value>
    <description></description>
  </property>
  <property>
    <name>webhcat_user</name>
    <value>hcat</value>
    <description></description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/webhcat-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/webhcat-site.xml
deleted file mode 100644
index 8c5eb63013..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/configuration/webhcat-site.xml
++ /dev/null
@@ -1,126 +0,0 @@
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

<!-- The default settings for Templeton. -->
<!-- Edit templeton-site.xml to change settings for your local -->
<!-- install. -->

<configuration>

  <property>
    <name>templeton.port</name>
      <value>50111</value>
    <description>The HTTP port for the main server.</description>
  </property>

  <property>
    <name>templeton.hadoop.conf.dir</name>
    <value>/etc/hadoop/conf</value>
    <description>The path to the Hadoop configuration.</description>
  </property>

  <property>
    <name>templeton.jar</name>
    <value>/usr/lib/hcatalog/share/webhcat/svr/webhcat.jar</value>
    <description>The path to the Templeton jar file.</description>
  </property>

  <property>
    <name>templeton.libjars</name>
    <value>/usr/lib/zookeeper/zookeeper.jar</value>
    <description>Jars to add the the classpath.</description>
  </property>


  <property>
    <name>templeton.hadoop</name>
    <value>/usr/bin/hadoop</value>
    <description>The path to the Hadoop executable.</description>
  </property>

  <property>
    <name>templeton.pig.archive</name>
    <value>glusterfs:///apps/webhcat/pig.tar.gz</value>
    <description>The path to the Pig archive.</description>
  </property>

  <property>
    <name>templeton.pig.path</name>
    <value>pig.tar.gz/pig/bin/pig</value>
    <description>The path to the Pig executable.</description>
  </property>

  <property>
    <name>templeton.hcat</name>
    <value>/usr/bin/hcat</value>
    <description>The path to the hcatalog executable.</description>
  </property>

  <property>
    <name>templeton.hive.archive</name>
    <value>glusterfs:///apps/webhcat/hive.tar.gz</value>
    <description>The path to the Hive archive.</description>
  </property>

  <property>
    <name>templeton.hive.path</name>
    <value>hive.tar.gz/hive/bin/hive</value>
    <description>The path to the Hive executable.</description>
  </property>

  <property>
    <name>templeton.hive.properties</name>
    <value></value>
    <description>Properties to set when running hive.</description>
  </property>


  <property>
    <name>templeton.zookeeper.hosts</name>
    <value>localhost:2181</value>
    <description>ZooKeeper servers, as comma separated host:port pairs</description>
  </property>

  <property>
    <name>templeton.storage.class</name>
    <value>org.apache.hive.hcatalog.templeton.tool.ZooKeeperStorage</value>
    <description>The class to use as storage</description>
  </property>

  <property>
   <name>templeton.override.enabled</name>
   <value>false</value>
   <description>
     Enable the override path in templeton.override.jars
   </description>
 </property>

 <property>
    <name>templeton.streaming.jar</name>
    <value>glusterfs:///apps/webhcat/hadoop-streaming.jar</value>
    <description>The glusterfs path to the Hadoop streaming jar file.</description>
  </property> 

  <property>
    <name>templeton.exec.timeout</name>
    <value>60000</value>
    <description>Time out for templeton api</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/metainfo.xml
deleted file mode 100644
index 1177cd2217..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/WEBHCAT/metainfo.xml
++ /dev/null
@@ -1,35 +0,0 @@
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
    <user>root</user>
    <comment>This is comment for WEBHCAT service</comment>
    <version>0.12.0.2.0.6.1</version>

    <components>
        <component>
            <name>WEBHCAT_SERVER</name>
            <category>MASTER</category>
        </component>
    </components>

  <configuration-dependencies>
    <config-type>global</config-type>
    <config-type>webhcat-site</config-type>
  </configuration-dependencies>

</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/capacity-scheduler.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/capacity-scheduler.xml
deleted file mode 100644
index 4a197793cf..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/capacity-scheduler.xml
++ /dev/null
@@ -1,128 +0,0 @@
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

<configuration>

  <property>
    <name>yarn.scheduler.capacity.maximum-applications</name>
    <value>10000</value>
    <description>
      Maximum number of applications that can be pending and running.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.maximum-am-resource-percent</name>
    <value>0.2</value>
    <description>
      Maximum percent of resources in the cluster which can be used to run 
      application masters i.e. controls number of concurrent running
      applications.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.queues</name>
    <value>default</value>
    <description>
      The queues at the this level (root is the root queue).
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.capacity</name>
    <value>100</value>
    <description>
      The total capacity as a percentage out of 100 for this queue.
      If it has child queues then this includes their capacity as well.
      The child queues capacity should add up to their parent queue's capacity
      or less.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.capacity</name>
    <value>100</value>
    <description>Default queue target capacity.</description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.user-limit-factor</name>
    <value>1</value>
    <description>
      Default queue user limit a percentage from 0.0 to 1.0.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.maximum-capacity</name>
    <value>100</value>
    <description>
      The maximum capacity of the default queue. 
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.state</name>
    <value>RUNNING</value>
    <description>
      The state of the default queue. State can be one of RUNNING or STOPPED.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.acl_submit_jobs</name>
    <value>*</value>
    <description>
      The ACL of who can submit jobs to the default queue.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.default.acl_administer_jobs</name>
    <value>*</value>
    <description>
      The ACL of who can administer jobs on the default queue.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.capacity.root.acl_administer_queues</name>
    <value>*</value>
    <description>
      The ACL for who can administer this queue i.e. change sub-queue 
      allocations.
    </description>
  </property>
  
  <property>
    <name>yarn.scheduler.capacity.root.unfunded.capacity</name>
    <value>50</value>
    <description>
      No description
    </description>
  </property>
  <property>
    <name>yarn.scheduler.capacity.node-locality-delay</name>
    <value>40</value>
    <description>
      No description
    </description>
  </property>


</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/container-executor.cfg b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/container-executor.cfg
deleted file mode 100644
index d07b3db98e..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/container-executor.cfg
++ /dev/null
@@ -1,20 +0,0 @@
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
yarn.nodemanager.local-dirs=TODO-YARN-LOCAL-DIR
yarn.nodemanager.linux-container-executor.group=hadoop
yarn.nodemanager.log-dirs=TODO-YARN-LOG-DIR
banned.users=hdfs,bin,0
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/core-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/core-site.xml
deleted file mode 100644
index 3a2af49059..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/core-site.xml
++ /dev/null
@@ -1,20 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
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
<configuration>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/global.xml
deleted file mode 100644
index edd1636a2e..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/global.xml
++ /dev/null
@@ -1,64 +0,0 @@
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
    <name>rm_host</name>
    <value></value>
    <description>ResourceManager.</description>
  </property>
  <property>
    <name>nm_hosts</name>
    <value></value>
    <description>List of NodeManager Hosts.</description>
  </property>
  <property>
    <name>yarn_log_dir_prefix</name>
    <value>/var/log/hadoop-yarn</value>
    <description>YARN Log Dir Prefix</description>
  </property>
  <property>
    <name>yarn_pid_dir_prefix</name>
    <value>/var/run/hadoop-yarn</value>
    <description>YARN PID Dir Prefix</description>
  </property>
  <property>
    <name>yarn_user</name>
    <value>yarn</value>
    <description>YARN User</description>
  </property>
  <property>
    <name>yarn_heapsize</name>
    <value>1024</value>
    <description>Max heapsize for all YARN components using a numerical value in the scale of MB</description>
  </property>
  <property>
    <name>resourcemanager_heapsize</name>
    <value>1024</value>
    <description>Max heapsize for ResourceManager using a numerical value in the scale of MB</description>
  </property>
  <property>
    <name>nodemanager_heapsize</name>
    <value>1024</value>
    <description>Max heapsize for NodeManager using a numerical value in the scale of MB</description>
  </property>
</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/yarn-site.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/yarn-site.xml
deleted file mode 100644
index 2cf03e63b0..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/configuration/yarn-site.xml
++ /dev/null
@@ -1,326 +0,0 @@
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
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

<!-- Put site-specific property overrides in this file. -->

<configuration xmlns:xi="http://www.w3.org/2001/XInclude">

  <!-- ResourceManager -->

  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>localhost</value>
    <description>The hostname of the RM.</description>
  </property>

  <property>
    <name>yarn.resourcemanager.resource-tracker.address</name>
    <value>localhost:8025</value>
  </property>

  <property>
    <name>yarn.resourcemanager.scheduler.address</name>
    <value>localhost:8030</value>
    <description>The address of the scheduler interface.</description>
  </property>

  <property>
    <name>yarn.resourcemanager.address</name>
    <value>localhost:8050</value>
    <description>
      The address of the applications manager interface in the
      RM.
    </description>
  </property>

  <property>
    <name>yarn.resourcemanager.admin.address</name>
    <value>localhost:8141</value>
    <description>The address of the RM admin interface.</description>
  </property>

  <property>
    <name>yarn.resourcemanager.scheduler.class</name>
    <value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler</value>
    <description>The class to use as the resource scheduler.</description>
  </property>

  <property>
    <name>yarn.scheduler.minimum-allocation-mb</name>
    <value>512</value>
    <description>
      The minimum allocation for every container request at the RM,
      in MBs. Memory requests lower than this won't take effect,
      and the specified value will get allocated at minimum.
    </description>
  </property>

  <property>
    <name>yarn.scheduler.maximum-allocation-mb</name>
    <value>2048</value>
    <description>
      The maximum allocation for every container request at the RM,
      in MBs. Memory requests higher than this won't take effect,
      and will get capped to this value.
    </description>
  </property>

  <property>
    <name>yarn.acl.enable</name>
    <value>false</value>
  </property>

  <property>
    <name>yarn.admin.acl</name>
    <value></value>
  </property>

  <!-- NodeManager -->

  <property>
    <name>yarn.nodemanager.address</name>
    <value>0.0.0.0:45454</value>
    <description>The address of the container manager in the NM.</description>
  </property>

  <property>
    <name>yarn.nodemanager.resource.memory-mb</name>
    <value>5120</value>
    <description>Amount of physical memory, in MB, that can be allocated
      for containers.</description>
  </property>

  <property>
    <name>yarn.application.classpath</name>
    <value>/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*</value>
    <description>Classpath for typical applications.</description>
  </property>

  <property>
    <name>yarn.nodemanager.vmem-pmem-ratio</name>
    <value>2.1</value>
    <description>Ratio between virtual memory to physical memory when
      setting memory limits for containers. Container allocations are
      expressed in terms of physical memory, and virtual memory usage
      is allowed to exceed this allocation by this ratio.
    </description>
  </property>

  <property>
    <name>yarn.log.server.url</name>
    <value>http://localhost:19888/jobhistory/logs</value>
    <description>
      URI for the HistoryServer's log resource
    </description>
  </property>
  
  <property>
    <name>yarn.nodemanager.container-executor.class</name>
    <value>org.apache.hadoop.yarn.server.nodemanager.GlusterContainerExecutor</value>
    <description>ContainerExecutor for launching containers</description>
  </property>

  <property>
    <name>yarn.nodemanager.linux-container-executor.group</name>
    <value>hadoop</value>
    <description>Unix group of the NodeManager</description>
  </property>

  <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
    <description>Auxilliary services of NodeManager. A valid service name should only contain a-zA-Z0-9_ and can
      not start with numbers</description>
  </property>

  <property>
    <name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
    <value>org.apache.hadoop.mapred.ShuffleHandler</value>
  </property>

  <property>
    <name>yarn.nodemanager.log-dirs</name>
    <value>/hadoop/yarn/log</value>
    <description>
      Where to store container logs. An application's localized log directory
      will be found in ${yarn.nodemanager.log-dirs}/application_${appid}.
      Individual containers' log directories will be below this, in directories
      named container_{$contid}. Each container directory will contain the files
      stderr, stdin, and syslog generated by that container.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.local-dirs</name>
    <value>/hadoop/yarn/local</value>
    <description>
      List of directories to store localized files in. An
      application's localized file directory will be found in:
      ${yarn.nodemanager.local-dirs}/usercache/${user}/appcache/application_${appid}.
      Individual containers' work directories, called container_${contid}, will
      be subdirectories of this.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.container-monitor.interval-ms</name>
    <value>3000</value>
    <description>
      The interval, in milliseconds, for which the node manager
      waits  between two cycles of monitoring its containers' memory usage.
    </description>
  </property>

  <!--
  <property>
    <name>yarn.nodemanager.health-checker.script.path</name>
    <value>/etc/hadoop/conf/health_check_nodemanager</value>
    <description>The health check script to run.</description>
  </property>
   -->

  <property>
    <name>yarn.nodemanager.health-checker.interval-ms</name>
    <value>135000</value>
    <description>Frequency of running node health script.</description>
  </property>

  <property>
    <name>yarn.nodemanager.health-checker.script.timeout-ms</name>
    <value>60000</value>
    <description>Script time out period.</description>
  </property>

  <property>
    <name>yarn.nodemanager.log.retain-second</name>
    <value>604800</value>
    <description>
      Time in seconds to retain user logs. Only applicable if
      log aggregation is disabled.
    </description>
  </property>

  <property>
    <name>yarn.log-aggregation-enable</name>
    <value>true</value>
    <description>Whether to enable log aggregation</description>
  </property>

  <property>
    <name>yarn.nodemanager.remote-app-log-dir</name>
    <value>/app-logs</value>
  </property>

  <property>
    <name>yarn.nodemanager.remote-app-log-dir-suffix</name>
    <value>logs</value>
    <description>
      The remote log dir will be created at
      {yarn.nodemanager.remote-app-log-dir}/${user}/{thisParam}.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.log-aggregation.compression-type</name>
    <value>gz</value>
    <description>
      T-file compression types used to compress aggregated logs.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.delete.debug-delay-sec</name>
    <value>0</value>
    <description>
      Number of seconds after an application finishes before the nodemanager's
      DeletionService will delete the application's localized file directory
      and log directory.

      To diagnose Yarn application problems, set this property's value large
      enough (for example, to 600 = 10 minutes) to permit examination of these
      directories. After changing the property's value, you must restart the
      nodemanager in order for it to have an effect.

      The roots of Yarn applications' work directories is configurable with
      the yarn.nodemanager.local-dirs property (see below), and the roots
      of the Yarn applications' log directories is configurable with the
      yarn.nodemanager.log-dirs property (see also below).
    </description>
  </property>

  <property>
    <name>yarn.log-aggregation.retain-seconds</name>
    <value>2592000</value>
    <description>
      How long to keep aggregation logs before deleting them. -1 disables.
      Be careful set this too small and you will spam the name node.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.admin-env</name>
    <value>MALLOC_ARENA_MAX=$MALLOC_ARENA_MAX</value>
    <description>
      Environment variables that should be forwarded from the NodeManager's
      environment to the container's.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.disk-health-checker.min-healthy-disks</name>
    <value>0.25</value>
    <description>
      The minimum fraction of number of disks to be healthy for the nodemanager
      to launch new containers. This correspond to both
      yarn-nodemanager.local-dirs and yarn.nodemanager.log-dirs. i.e.
      If there are less number of healthy local-dirs (or log-dirs) available,
      then new containers will not be launched on this node.
    </description>
  </property>

  <property>
    <name>yarn.resourcemanager.am.max-attempts</name>
    <value>2</value>
    <description>
      The maximum number of application attempts. It's a global
      setting for all application masters. Each application master can specify
      its individual maximum number of application attempts via the API, but the
      individual number cannot be more than the global upper bound. If it is,
      the resourcemanager will override it. The default number is set to 2, to
      allow at least one retry for AM.
    </description>
  </property>

  <property>
    <name>yarn.resourcemanager.webapp.address</name>
    <value>localhost:8088</value>
    <description>
      The address of the RM web application.
    </description>
  </property>

  <property>
    <name>yarn.nodemanager.vmem-check-enabled</name>
    <value>false</value>
    <description>
      Whether virtual memory limits will be enforced for containers.
    </description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metainfo.xml
deleted file mode 100644
index bc80f4a679..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metainfo.xml
++ /dev/null
@@ -1,42 +0,0 @@
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
    <user>mapred</user>
    <comment>Apache Hadoop NextGen MapReduce (YARN)</comment>
    <version>2.2.0.2.0.6.0</version>
    <components>
        <component>
            <name>RESOURCEMANAGER</name>
            <category>MASTER</category>
        </component>
        <component>
            <name>NODEMANAGER</name>
            <category>SLAVE</category>
        </component>
       <component>
            <name>YARN_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>
    <configuration-dependencies>
      <config-type>global</config-type>
      <config-type>core-site</config-type>
      <config-type>yarn-site</config-type>
      <config-type>capacity-scheduler</config-type>
    </configuration-dependencies>
</metainfo>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metrics.json b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metrics.json
deleted file mode 100644
index a60ab408c0..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/YARN/metrics.json
++ /dev/null
@@ -1,2534 +0,0 @@
{
  "NODEMANAGER": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputsFailed": {
            "metric": "mapred.ShuffleOutputsFailed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.metrics.RpcAuthorizationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.ugi.LoginSuccessAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersCompleted": {
            "metric": "yarn.ContainersCompleted",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersKilled": {
            "metric": "yarn.ContainersKilled",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AllocatedGB": {
            "metric": "yarn.AllocatedGB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputsOK": {
            "metric": "mapred.ShuffleOutputsOK",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersFailed": {
            "metric": "yarn.ContainersFailed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.ugi.LoginSuccessNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AllocatedContainers": {
            "metric": "yarn.AllocatedContainers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.CallQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersRunning": {
            "metric": "yarn.ContainersRunning",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersLaunched": {
            "metric": "yarn.ContainersLaunched",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.ugi.LoginFailureNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AvailableGB": {
            "metric": "yarn.AvailableGB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/mapred/ShuffleConnections": {
            "metric": "mapred.ShuffleConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersIniting": {
            "metric": "yarn.ContainersIniting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.ugi.LoginFailureAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputBytes": {
            "metric": "mapred.ShuffleOutputBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.metrics.RpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.metrics.RpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputsFailed": {
            "metric": "mapred.ShuffleOutputsFailed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "rpc.metrics.RpcAuthorizationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.ugi.LoginSuccessAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersCompleted": {
            "metric": "yarn.ContainersCompleted",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersKilled": {
            "metric": "yarn.ContainersKilled",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AllocatedGB": {
            "metric": "yarn.AllocatedGB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputsOK": {
            "metric": "mapred.ShuffleOutputsOK",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersFailed": {
            "metric": "yarn.ContainersFailed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.ugi.LoginSuccessNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AllocatedContainers": {
            "metric": "yarn.AllocatedContainers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.CallQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersRunning": {
            "metric": "yarn.ContainersRunning",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ContainersLaunched": {
            "metric": "yarn.ContainersLaunched",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.ugi.LoginFailureNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/AvailableGB": {
            "metric": "yarn.AvailableGB",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/mapred/ShuffleConnections": {
            "metric": "mapred.ShuffleConnections",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/ContainersIniting": {
            "metric": "yarn.ContainersIniting",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.ugi.LoginFailureAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/mapred/ShuffleOutputBytes": {
            "metric": "mapred.ShuffleOutputBytes",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "rpc.metrics.RpcAuthenticationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "rpc.metrics.RpcAuthenticationFailures",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/jvm/memHeapCommittedM": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsNew": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/callQueueLen": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.CallQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "Hadoop:service=NodeManager,name=UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/SentBytes": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.SentBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logInfo": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logWarn": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "Hadoop:service=NodeManager,name=UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcCount": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.ReceivedBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logError": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "Hadoop:service=NodeManager,name=UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.NumOpenConnections",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logFatal": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "Hadoop:service=NodeManager,name=UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "Hadoop:service=NodeManager,name=RpcActivity.RpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "Hadoop:service=NodeManager,name=JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ]
  },
  "RESOURCEMANAGER": {
    "Component": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpcdetailed/FinishApplicationMasterNumOps": {
            "metric": "rpcdetailed.rpcdetailed.FinishApplicationMasterNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsCompleted": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsCompleted",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumUnhealthyNMs": {
            "metric": "yarn.ClusterMetrics.NumUnhealthyNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumRebootedNMs": {
            "metric": "yarn.ClusterMetrics.NumRebootedNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsSubmitted": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsSubmitted",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumLostNMs": {
            "metric": "yarn.ClusterMetrics.NumLostNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.ugi.LoginSuccessAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AllocatedContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AllocatedContainers",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsKilled": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsKilled",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumActiveNMs": {
            "metric": "yarn.ClusterMetrics.NumActiveNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsFailed": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsFailed",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/AllocateNumOps": {
            "metric": "rpcdetailed.rpcdetailed.AllocateNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCountMarkSweepCompact": {
            "metric": "jvm.JvmMetrics.GcCountMarkSweepCompact",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsRunning": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsRunning",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumDecommissionedNMs": {
            "metric": "yarn.ClusterMetrics.NumDecommissionedNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.ugi.LoginSuccessNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillisCopy": {
            "metric": "jvm.JvmMetrics.GcTimeMillisCopy",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/PendingContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).PendingContainers",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memMaxM": {
            "metric": "jvm.JvmMetrics.MemMaxM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/AllocateAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.AllocateAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/GetApplicationReportNumOps": {
            "metric": "rpcdetailed.rpcdetailed.GetApplicationReportNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/FinishApplicationMasterAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.FinishApplicationMasterAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.CallQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/RegisterApplicationMasterNumOps": {
            "metric": "rpcdetailed.rpcdetailed.RegisterApplicationMasterNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AvailableMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/PendingMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).PendingMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.ugi.LoginFailureNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/SubmitApplicationAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.SubmitApplicationAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/GetNewApplicationNumOps": {
            "metric": "rpcdetailed.rpcdetailed.GetNewApplicationNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsPending": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsPending",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCountCopy": {
            "metric": "jvm.JvmMetrics.GcCountCopy",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.ugi.LoginFailureAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/SubmitApplicationNumOps": {
            "metric": "rpcdetailed.rpcdetailed.SubmitApplicationNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillisMarkSweepCompact": {
            "metric": "jvm.JvmMetrics.GcTimeMillisMarkSweepCompact",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AllocatedMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AllocatedMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/GetApplicationReportAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.GetApplicationReportAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/NodeHeartbeatAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.NodeHeartbeatAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/GetNewApplicationAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.GetNewApplicationAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/RegisterApplicationMasterAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.RegisterApplicationMasterAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/ReservedContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).ReservedContainers",
            "pointInTime": false,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsFailed": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsFailed",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/rebootedNMcount": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumRebootedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumUnhealthyNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumUnhealthyNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsNew": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumRebootedNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumRebootedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/runtime/StartTime": {
            "metric": "java.lang:type=Runtime.StartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsKilled": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsKilled",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AggregateContainersAllocated": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AggregateContainersAllocated",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumLostNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumLostNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/StartTime": {
            "metric": "java.lang:type=Runtime.StartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsSubmitted": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsSubmitted",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/SentBytes": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.SentBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumActiveNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumActiveNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_300": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_300",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/HeapMemoryMax":{
            "metric" : "java.lang:type=Memory.HeapMemoryUsage[max]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/HeapMemoryUsed":{
            "metric" : "java.lang:type=Memory.HeapMemoryUsage[used]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/NonHeapMemoryMax":{
            "metric" : "java.lang:type=Memory.NonHeapMemoryUsage[max]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/NonHeapMemoryUsed":{
            "metric" : "java.lang:type=Memory.NonHeapMemoryUsage[used]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logWarn": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcCount": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.ReceivedBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_60": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_60",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumDecommissionedNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumDecommissionedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.NumOpenConnections",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memMaxM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemMaxM",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/unhealthyNMcount": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumUnhealthyNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedVCores",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/decommissionedNMcount": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumDecommissionedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/startTime": {
            "metric": "java.lang:type=Runtime.StartTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ActiveApplications": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ActiveApplications",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AvailableMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AvailableMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/nodeManagers": {
            "metric": "Hadoop:service=ResourceManager,name=RMNMInfo.LiveNodeManagers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/callQueueLen": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.CallQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedVCores",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsPending": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsPending",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsCompleted": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsCompleted",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ActiveUsers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ActiveUsers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logInfo": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsRunning",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_1440": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_1440",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AvailableVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AvailableVCores",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logError": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logFatal": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/activeNMcount": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumActiveNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AggregateContainersReleased": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AggregateContainersReleased",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "ServiceComponentInfo/rm_metrics/cluster/lostNMcount": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumLostNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingVCores",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ],
    "HostComponent": [
      {
        "type": "ganglia",
        "metrics": {
          "metrics/rpcdetailed/FinishApplicationMasterNumOps": {
            "metric": "rpcdetailed.rpcdetailed.FinishApplicationMasterNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_total": {
            "metric": "mem_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsCompleted": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsCompleted",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumUnhealthyNMs": {
            "metric": "yarn.ClusterMetrics.NumUnhealthyNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "jvm.JvmMetrics.ThreadsRunnable",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumRebootedNMs": {
            "metric": "yarn.ClusterMetrics.NumRebootedNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsNew": {
            "metric": "jvm.JvmMetrics.ThreadsNew",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsSubmitted": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsSubmitted",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumLostNMs": {
            "metric": "yarn.ClusterMetrics.NumLostNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "ugi.ugi.LoginSuccessAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "rpc.rpc.RpcQueueTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AllocatedContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AllocatedContainers",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/SentBytes": {
            "metric": "rpc.rpc.SentBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsKilled": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsKilled",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumActiveNMs": {
            "metric": "yarn.ClusterMetrics.NumActiveNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logWarn": {
            "metric": "jvm.JvmMetrics.LogWarn",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsFailed": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsFailed",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCount": {
            "metric": "jvm.JvmMetrics.GcCount",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_run": {
            "metric": "proc_run",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_total": {
            "metric": "swap_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "rpc.rpc.ReceivedBytes",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/AllocateNumOps": {
            "metric": "rpcdetailed.rpcdetailed.AllocateNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_nice": {
            "metric": "cpu_nice",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcCountMarkSweepCompact": {
            "metric": "jvm.JvmMetrics.GcCountMarkSweepCompact",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "jvm.JvmMetrics.ThreadsBlocked",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsRunning": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsRunning",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/ClusterMetrics/NumDecommissionedNMs": {
            "metric": "yarn.ClusterMetrics.NumDecommissionedNMs",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "rpc.rpc.RpcQueueTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/process/proc_total": {
            "metric": "proc_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/part_max_used": {
            "metric": "part_max_used",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "rpc.rpc.NumOpenConnections",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "jvm.JvmMetrics.MemHeapUsedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/disk/disk_free": {
            "metric": "disk_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "jvm.JvmMetrics.ThreadsWaiting",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/load_one": {
            "metric": "load_one",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_buffers": {
            "metric": "mem_buffers",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "ugi.ugi.LoginSuccessNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillisCopy": {
            "metric": "jvm.JvmMetrics.GcTimeMillisCopy",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "jvm.JvmMetrics.GcTimeMillis",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_idle": {
            "metric": "cpu_idle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/PendingContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).PendingContainers",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/memMaxM": {
            "metric": "jvm.JvmMetrics.MemMaxM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "jvm.JvmMetrics.ThreadsTerminated",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/network/bytes_out": {
            "metric": "bytes_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_aidle": {
            "metric": "cpu_aidle",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/mem_free": {
            "metric": "mem_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/AllocateAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.AllocateAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_user": {
            "metric": "cpu_user",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/memory/swap_free": {
            "metric": "swap_free",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_system": {
            "metric": "cpu_system",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load_five": {
            "metric": "load_five",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/bytes_in": {
            "metric": "bytes_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/boottime": {
            "metric": "boottime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_out": {
            "metric": "pkts_out",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "jvm.JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/GetApplicationReportNumOps": {
            "metric": "rpcdetailed.rpcdetailed.GetApplicationReportNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/FinishApplicationMasterAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.FinishApplicationMasterAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/callQueueLen": {
            "metric": "rpc.rpc.CallQueueLength",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_cached": {
            "metric": "mem_cached",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/RegisterApplicationMasterNumOps": {
            "metric": "rpcdetailed.rpcdetailed.RegisterApplicationMasterNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/disk/disk_total": {
            "metric": "disk_total",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AvailableMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/PendingMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).PendingMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logInfo": {
            "metric": "jvm.JvmMetrics.LogInfo",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "rpc.rpc.RpcProcessingTimeNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "ugi.ugi.LoginFailureNumOps",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/memory/mem_shared": {
            "metric": "mem_shared",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/SubmitApplicationAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.SubmitApplicationAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_wio": {
            "metric": "cpu_wio",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/GetNewApplicationNumOps": {
            "metric": "rpcdetailed.rpcdetailed.GetNewApplicationNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsPending": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AppsPending",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/gcCountCopy": {
            "metric": "jvm.JvmMetrics.GcCountCopy",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/load_fifteen": {
            "metric": "load_fifteen",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/logError": {
            "metric": "jvm.JvmMetrics.LogError",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "ugi.ugi.LoginFailureAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/cpu/cpu_num": {
            "metric": "cpu_num",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/SubmitApplicationNumOps": {
            "metric": "rpcdetailed.rpcdetailed.SubmitApplicationNumOps",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/jvm/gcTimeMillisMarkSweepCompact": {
            "metric": "jvm.JvmMetrics.GcTimeMillisMarkSweepCompact",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/cpu/cpu_speed": {
            "metric": "cpu_speed",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "rpc.rpc.RpcAuthorizationSuccesses",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AllocatedMB": {
            "metric": "yarn.QueueMetrics.Queue=(.+).AllocatedMB",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/jvm/logFatal": {
            "metric": "jvm.JvmMetrics.LogFatal",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "rpc.rpc.RpcProcessingTimeAvgTime",
            "pointInTime": false,
            "temporal": true
          },
          "metrics/rpcdetailed/GetApplicationReportAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.GetApplicationReportAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/NodeHeartbeatAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.NodeHeartbeatAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/GetNewApplicationAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.GetNewApplicationAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/network/pkts_in": {
            "metric": "pkts_in",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/rpcdetailed/RegisterApplicationMasterAvgTime": {
            "metric": "rpcdetailed.rpcdetailed.RegisterApplicationMasterAvgTime",
            "pointInTime": true,
            "temporal": true
          },
          "metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/ReservedContainers": {
            "metric": "yarn.QueueMetrics.Queue=(.+).ReservedContainers",
            "pointInTime": false,
            "temporal": true
          }
        }
      },
      {
        "type": "jmx",
        "metrics": {
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsFailed": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsFailed",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapCommittedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumUnhealthyNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumUnhealthyNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsRunnable": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsRunnable",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsNew": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsNew",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumRebootedNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumRebootedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsKilled": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsKilled",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationFailures": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthorizationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AggregateContainersAllocated": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AggregateContainersAllocated",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumLostNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumLostNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcQueueTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginSuccessAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsSubmitted": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsSubmitted",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/SentBytes": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.SentBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumActiveNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumActiveNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_300": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_300",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapUsedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemNonHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logWarn": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogWarn",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTimedWaiting": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsTimedWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcCount": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.GcCount",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/ReceivedBytes": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.ReceivedBytes",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/HeapMemoryMax":{
            "metric" : "java.lang:type=Memory.HeapMemoryUsage[max]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/HeapMemoryUsed":{
            "metric" : "java.lang:type=Memory.HeapMemoryUsage[used]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/NonHeapMemoryMax":{
            "metric" : "java.lang:type=Memory.NonHeapMemoryUsage[max]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/NonHeapMemoryUsed":{
            "metric" : "java.lang:type=Memory.NonHeapMemoryUsage[used]",
            "pointInTime" : true,
            "temporal" : false
          },
          "metrics/jvm/threadsBlocked": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsBlocked",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_60": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_60",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcQueueTime_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcQueueTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/ClusterMetrics/NumDecommissionedNMs": {
            "metric": "Hadoop:service=ResourceManager,name=ClusterMetrics.NumDecommissionedNMs",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingContainers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingContainers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/NumOpenConnections": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.NumOpenConnections",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memHeapUsedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemHeapUsedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsWaiting": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsWaiting",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/gcTimeMillis": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.GcTimeMillis",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginSuccess_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginSuccessNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/threadsTerminated": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.ThreadsTerminated",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memMaxM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemMaxM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedVCores",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ActiveApplications": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ActiveApplications",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AvailableMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AvailableMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/memNonHeapCommittedM": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.MemNonHeapCommittedM",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/callQueueLen": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.CallQueueLength",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsPending": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsPending",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedVCores",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsCompleted": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsCompleted",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ActiveUsers": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ActiveUsers",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logInfo": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogInfo",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsRunning",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/running_1440": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).running_1440",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AvailableVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AvailableVCores",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginFailureNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_num_ops": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcProcessingTimeNumOps",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/ReservedMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).ReservedMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logError": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogError",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/ugi/loginFailure_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=UgiMetrics.LoginFailureAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthorizationSuccesses": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthorizationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/jvm/logFatal": {
            "metric": "Hadoop:service=ResourceManager,name=JvmMetrics.LogFatal",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/RpcProcessingTime_avg_time": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcProcessingTimeAvgTime",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationSuccesses": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthenticationSuccesses",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AggregateContainersReleased": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AggregateContainersReleased",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/rpc/rpcAuthenticationFailures": {
            "metric": "Hadoop:service=ResourceManager,name=RpcActivity.RpcAuthenticationFailures",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AllocatedMB": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).AllocatedMB",
            "pointInTime": true,
            "temporal": false
          },
          "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/PendingVCores": {
            "metric": "Hadoop:service=ResourceManager,name=QueueMetrics(.+).PendingVCores",
            "pointInTime": true,
            "temporal": false
          }
        }
      }
    ]
  }
}
\ No newline at end of file
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/configuration/global.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/configuration/global.xml
deleted file mode 100644
index f78df89d13..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/configuration/global.xml
++ /dev/null
@@ -1,75 +0,0 @@
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
    <name>zk_user</name>
    <value>zookeeper</value>
    <description>ZooKeeper User.</description>
  </property>
  <property>
    <name>zookeeperserver_host</name>
    <value></value>
    <description>ZooKeeper Server Hosts.</description>
  </property>
  <property>
    <name>zk_data_dir</name>
    <value>/hadoop/zookeeper</value>
    <description>Data directory for ZooKeeper.</description>
  </property>
  <property>
    <name>zk_log_dir</name>
    <value>/var/log/zookeeper</value>
    <description>ZooKeeper Log Dir</description>
  </property>
  <property>
    <name>zk_pid_dir</name>
    <value>/var/run/zookeeper</value>
    <description>ZooKeeper Pid Dir</description>
  </property>
  <property>
    <name>zk_pid_file</name>
    <value>/var/run/zookeeper/zookeeper_server.pid</value>
    <description>ZooKeeper Pid File</description>
  </property>
  <property>
    <name>tickTime</name>
    <value>2000</value>
    <description>The length of a single tick in milliseconds, which is the basic time unit used by ZooKeeper</description>
  </property>
  <property>
    <name>initLimit</name>
    <value>10</value>
    <description>Ticks to allow for sync at Init.</description>
  </property>
  <property>
    <name>syncLimit</name>
    <value>5</value>
    <description>Ticks to allow for sync at Runtime.</description>
  </property>
  <property>
    <name>clientPort</name>
    <value>2181</value>
    <description>Port for running ZK Server.</description>
  </property>

</configuration>
diff --git a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/metainfo.xml b/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/metainfo.xml
deleted file mode 100644
index 565b4d41d3..0000000000
-- a/ambari-server/src/main/resources/stacks/HDP/2.0.6.GlusterFS/services/ZOOKEEPER/metainfo.xml
++ /dev/null
@@ -1,39 +0,0 @@
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
    <user>root</user>
    <comment>Centralized service which provides highly reliable distributed coordination</comment>
    <version>3.4.5.2.0.6.0</version>

    <components>
        <component>
            <name>ZOOKEEPER_SERVER</name>
            <category>MASTER</category>
        </component>

        <component>
            <name>ZOOKEEPER_CLIENT</name>
            <category>CLIENT</category>
        </component>
    </components>

  <configuration-dependencies>
    <config-type>global</config-type>
  </configuration-dependencies>

</metainfo>
- 
2.19.1.windows.1

