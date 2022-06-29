From 7065e86a9b584baa722828e0b01d40dde021a490 Mon Sep 17 00:00:00 2001
From: Julian Reschke <reschke@apache.org>
Date: Tue, 13 Dec 2016 14:15:46 +0000
Subject: [PATCH] JCR-4060: unintended export versions due to changed defaults
 in maven bundle plugin

add explicit version numbers for the affected packages, freezing them at 2.13.5

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@1774021 13f79535-47bb-0310-9956-ffa450edef68
--
 .../jackrabbit/api/query/package-info.java    | 19 ++++++++++++++++++
 jackrabbit-aws-ext/pom.xml                    |  5 +++++
 .../jackrabbit/aws/ext/ds/package-info.java   | 19 ++++++++++++++++++
 jackrabbit-data/pom.xml                       |  5 +++++
 .../jackrabbit/core/config/package-info.java  | 19 ++++++++++++++++++
 .../jackrabbit/core/data/db/package-info.java | 19 ++++++++++++++++++
 .../jackrabbit/core/data/package-info.java    | 19 ++++++++++++++++++
 .../core/data/util/package-info.java          | 19 ++++++++++++++++++
 .../core/fs/local/package-info.java           | 19 ++++++++++++++++++
 .../jackrabbit/core/fs/package-info.java      | 19 ++++++++++++++++++
 .../jackrabbit/core/util/db/package-info.java | 19 ++++++++++++++++++
 .../jackrabbit/data/core/package-info.java    | 19 ++++++++++++++++++
 .../authorization/package-info.java           | 19 ++++++++++++++++++
 .../commons/jackrabbit/package-info.java      | 20 +++++++++++++++++++
 .../commons/observation/package-info.java     | 19 ++++++++++++++++++
 .../jackrabbit/stats/jmx/package-info.java    | 19 ++++++++++++++++++
 .../spi/commons/tree/package-info.java        | 19 ++++++++++++++++++
 jackrabbit-standalone/pom.xml                 |  5 +++++
 .../jackrabbit/standalone/package-info.java   | 19 ++++++++++++++++++
 jackrabbit-vfs-ext/pom.xml                    |  5 +++++
 .../jackrabbit/vfs/ext/ds/package-info.java   | 19 ++++++++++++++++++
 21 files changed, 344 insertions(+)
 create mode 100755 jackrabbit-api/src/main/java/org/apache/jackrabbit/api/query/package-info.java
 create mode 100755 jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/config/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/util/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/local/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/core/util/db/package-info.java
 create mode 100755 jackrabbit-data/src/main/java/org/apache/jackrabbit/data/core/package-info.java
 create mode 100755 jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/authorization/package-info.java
 create mode 100755 jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/package-info.java
 create mode 100755 jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/observation/package-info.java
 create mode 100755 jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/stats/jmx/package-info.java
 create mode 100755 jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/tree/package-info.java
 create mode 100755 jackrabbit-standalone/src/main/java/org/apache/jackrabbit/standalone/package-info.java
 create mode 100755 jackrabbit-vfs-ext/src/main/java/org/apache/jackrabbit/vfs/ext/ds/package-info.java

diff --git a/jackrabbit-api/src/main/java/org/apache/jackrabbit/api/query/package-info.java b/jackrabbit-api/src/main/java/org/apache/jackrabbit/api/query/package-info.java
new file mode 100755
index 000000000..c25a75396
-- /dev/null
++ b/jackrabbit-api/src/main/java/org/apache/jackrabbit/api/query/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.api.query;
diff --git a/jackrabbit-aws-ext/pom.xml b/jackrabbit-aws-ext/pom.xml
index bbfe5b8b9..42591f193 100644
-- a/jackrabbit-aws-ext/pom.xml
++ b/jackrabbit-aws-ext/pom.xml
@@ -36,6 +36,11 @@
             <groupId>javax.jcr</groupId>
             <artifactId>jcr</artifactId>
         </dependency>
        <dependency>
            <groupId>biz.aQute</groupId>
            <artifactId>bndlib</artifactId>
            <scope>provided</scope>
        </dependency>
         <dependency>
             <groupId>org.apache.jackrabbit</groupId>
             <artifactId>jackrabbit-jcr-commons</artifactId>
diff --git a/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/package-info.java b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/package-info.java
new file mode 100755
index 000000000..c4c69110a
-- /dev/null
++ b/jackrabbit-aws-ext/src/main/java/org/apache/jackrabbit/aws/ext/ds/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.aws.ext.ds;
diff --git a/jackrabbit-data/pom.xml b/jackrabbit-data/pom.xml
index aa237819e..cb4c6dfb4 100644
-- a/jackrabbit-data/pom.xml
++ b/jackrabbit-data/pom.xml
@@ -67,6 +67,11 @@
 			<groupId>javax.jcr</groupId>
 			<artifactId>jcr</artifactId>
 		</dependency>
	    <dependency>
	      <groupId>biz.aQute</groupId>
	      <artifactId>bndlib</artifactId>
	      <scope>provided</scope>
	    </dependency>
 		<dependency>
 			<groupId>org.apache.jackrabbit</groupId>
 			<artifactId>jackrabbit-api</artifactId>
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/config/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/config/package-info.java
new file mode 100755
index 000000000..8a45a2f9e
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/config/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.config;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
new file mode 100755
index 000000000..f3466ba42
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/db/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.data.db;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
new file mode 100755
index 000000000..252815d34
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.data;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/util/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/util/package-info.java
new file mode 100755
index 000000000..e0fdd3b92
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/data/util/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.data.util;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/local/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/local/package-info.java
new file mode 100755
index 000000000..a1ddb42a9
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/local/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.fs.local;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/package-info.java
new file mode 100755
index 000000000..8f130cf6f
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/fs/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.fs;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/util/db/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/util/db/package-info.java
new file mode 100755
index 000000000..a0def1fd7
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/core/util/db/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.core.util.db;
diff --git a/jackrabbit-data/src/main/java/org/apache/jackrabbit/data/core/package-info.java b/jackrabbit-data/src/main/java/org/apache/jackrabbit/data/core/package-info.java
new file mode 100755
index 000000000..ee515b37d
-- /dev/null
++ b/jackrabbit-data/src/main/java/org/apache/jackrabbit/data/core/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.data.core;
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/authorization/package-info.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/authorization/package-info.java
new file mode 100755
index 000000000..2f31dcbea
-- /dev/null
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/authorization/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.commons.jackrabbit.authorization;
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/package-info.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/package-info.java
new file mode 100755
index 000000000..c1f1257b0
-- /dev/null
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/jackrabbit/package-info.java
@@ -0,0 +1,20 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.commons.jackrabbit;

diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/observation/package-info.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/observation/package-info.java
new file mode 100755
index 000000000..d36ba101b
-- /dev/null
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/commons/observation/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.commons.observation;
diff --git a/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/stats/jmx/package-info.java b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/stats/jmx/package-info.java
new file mode 100755
index 000000000..2a928f052
-- /dev/null
++ b/jackrabbit-jcr-commons/src/main/java/org/apache/jackrabbit/stats/jmx/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.stats.jmx;
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/tree/package-info.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/tree/package-info.java
new file mode 100755
index 000000000..9651a049a
-- /dev/null
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/tree/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.spi.commons.tree;
diff --git a/jackrabbit-standalone/pom.xml b/jackrabbit-standalone/pom.xml
index f8e272998..f158228f5 100644
-- a/jackrabbit-standalone/pom.xml
++ b/jackrabbit-standalone/pom.xml
@@ -68,6 +68,11 @@
       <artifactId>jcr</artifactId>
       <scope>compile</scope>
     </dependency>
    <dependency>
      <groupId>biz.aQute</groupId>
      <artifactId>bndlib</artifactId>
      <scope>provided</scope>
    </dependency>
     <dependency>
       <groupId>org.apache.jackrabbit</groupId>
       <artifactId>jackrabbit-webapp</artifactId>
diff --git a/jackrabbit-standalone/src/main/java/org/apache/jackrabbit/standalone/package-info.java b/jackrabbit-standalone/src/main/java/org/apache/jackrabbit/standalone/package-info.java
new file mode 100755
index 000000000..fc23468e7
-- /dev/null
++ b/jackrabbit-standalone/src/main/java/org/apache/jackrabbit/standalone/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.standalone;
diff --git a/jackrabbit-vfs-ext/pom.xml b/jackrabbit-vfs-ext/pom.xml
index 1bdb18986..70c4afe8b 100644
-- a/jackrabbit-vfs-ext/pom.xml
++ b/jackrabbit-vfs-ext/pom.xml
@@ -36,6 +36,11 @@
             <groupId>javax.jcr</groupId>
             <artifactId>jcr</artifactId>
         </dependency>
        <dependency>
            <groupId>biz.aQute</groupId>
            <artifactId>bndlib</artifactId>
            <scope>provided</scope>
        </dependency>
         <dependency>
           <groupId>org.apache.commons</groupId>
           <artifactId>commons-vfs2</artifactId>
diff --git a/jackrabbit-vfs-ext/src/main/java/org/apache/jackrabbit/vfs/ext/ds/package-info.java b/jackrabbit-vfs-ext/src/main/java/org/apache/jackrabbit/vfs/ext/ds/package-info.java
new file mode 100755
index 000000000..7166b6145
-- /dev/null
++ b/jackrabbit-vfs-ext/src/main/java/org/apache/jackrabbit/vfs/ext/ds/package-info.java
@@ -0,0 +1,19 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* see JCR-4060 */
@aQute.bnd.annotation.Version("2.13.5")
package org.apache.jackrabbit.vfs.ext.ds;
- 
2.19.1.windows.1

