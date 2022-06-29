From a8032b063715f4418ccc2fd5c7d0f93ba79c66ef Mon Sep 17 00:00:00 2001
From: Shai Erera <shaie@apache.org>
Date: Tue, 25 Sep 2012 05:52:44 +0000
Subject: [PATCH] LUCENE-4411: Depth requested in a FacetRequest is reset when
 Sampling is in effect

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1389718 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   6 +
 .../lucene/facet/search/sampling/Sampler.java |  19 +++
 .../sampling/OversampleWithDepthTest.java     | 137 ++++++++++++++++++
 3 files changed, 162 insertions(+)
 create mode 100644 lucene/facet/src/test/org/apache/lucene/facet/search/sampling/OversampleWithDepthTest.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 771949980c5..4b8fd90fdac 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -28,6 +28,12 @@ New Features
   output for a single input.  UpToTwoPositiveIntsOutputs was moved
   from lucene/core to lucene/misc.  (Mike McCandless)
 
Bug Fixes

* LUCENE-4411: when sampling is enabled for a FacetRequest, its depth
  parameter is reset to the default (1), even if set otherwise.
  (Gilad Barkai via Shai Erera)
  
 ======================= Lucene 4.0.0 =======================
 
 Changes in backwards compatibility policy
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/search/sampling/Sampler.java b/lucene/facet/src/java/org/apache/lucene/facet/search/sampling/Sampler.java
index 2618e62e82d..c3bdfdf3277 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/search/sampling/Sampler.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/search/sampling/Sampler.java
@@ -4,6 +4,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.IndexReader;
 
import org.apache.lucene.facet.search.CategoryListIterator;
 import org.apache.lucene.facet.search.FacetArrays;
 import org.apache.lucene.facet.search.ScoredDocIDs;
 import org.apache.lucene.facet.search.aggregator.Aggregator;
@@ -203,8 +204,21 @@ public abstract class Sampler {
     public OverSampledFacetRequest(FacetRequest orig, int num) {
       super(orig.getCategoryPath(), num);
       this.orig = orig;
      setDepth(orig.getDepth());
      setNumLabel(orig.getNumLabel());
      setResultMode(orig.getResultMode());
      setSortBy(orig.getSortBy());
      setSortOrder(orig.getSortOrder());
    }
    
    @Override
    public CategoryListIterator createCategoryListIterator(IndexReader reader,
        TaxonomyReader taxo, FacetSearchParams sParams, int partition)
        throws IOException {
      return orig.createCategoryListIterator(reader, taxo, sParams, partition);
     }
 
    
     @Override
     public Aggregator createAggregator(boolean useComplements,
         FacetArrays arrays, IndexReader indexReader,
@@ -222,5 +236,10 @@ public abstract class Sampler {
     public boolean requireDocumentScore() {
       return orig.requireDocumentScore();
     }
    
    @Override
    public boolean supportsComplements() {
      return orig.supportsComplements();
    }
   }
 }
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/search/sampling/OversampleWithDepthTest.java b/lucene/facet/src/test/org/apache/lucene/facet/search/sampling/OversampleWithDepthTest.java
new file mode 100644
index 00000000000..63cc2c10e3d
-- /dev/null
++ b/lucene/facet/src/test/org/apache/lucene/facet/search/sampling/OversampleWithDepthTest.java
@@ -0,0 +1,137 @@
package org.apache.lucene.facet.search.sampling;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.search.FacetsAccumulator;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetRequest;
import org.apache.lucene.facet.search.params.FacetRequest.ResultMode;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class OversampleWithDepthTest extends LuceneTestCase {
  
  @Test
  public void testCountWithdepthUsingSamping() throws Exception, IOException {
    Directory indexDir = newDirectory();
    Directory taxoDir = newDirectory();
    
    // index 100 docs, each with one category: ["root", docnum/10, docnum]
    // e.g. root/8/87
    index100Docs(indexDir, taxoDir);
    
    DirectoryReader r = DirectoryReader.open(indexDir);
    TaxonomyReader tr = new DirectoryTaxonomyReader(taxoDir);
    
    FacetSearchParams fsp = new FacetSearchParams();
    
    CountFacetRequest facetRequest = new CountFacetRequest(new CategoryPath("root"), 10);
    
    // Setting the depth to '2', should potentially get all categories
    facetRequest.setDepth(2);
    facetRequest.setResultMode(ResultMode.PER_NODE_IN_TREE);
    fsp.addFacetRequest(facetRequest);
    
    // Craft sampling params to enforce sampling
    final SamplingParams params = new SamplingParams();
    params.setMinSampleSize(2);
    params.setMaxSampleSize(50);
    params.setOversampleFactor(5);
    params.setSampingThreshold(60);
    params.setSampleRatio(0.1);
    
    FacetResult res = searchWithFacets(r, tr, fsp, params);
    FacetRequest req = res.getFacetRequest();
    assertEquals(facetRequest, req);
    
    FacetResultNode rootNode = res.getFacetResultNode();
    
    // Each node below root should also have sub-results as the requested depth was '2'
    for (FacetResultNode node : rootNode.getSubResults()) {
      assertTrue("node " + node.getLabel()
          + " should have had children as the requested depth was '2'",
          node.getNumSubResults() > 0);
    }
    
    IOUtils.close(r, tr, indexDir, taxoDir);
  }

  private void index100Docs(Directory indexDir, Directory taxoDir)
      throws CorruptIndexException, LockObtainFailedException, IOException {
    IndexWriterConfig iwc = newIndexWriterConfig(TEST_VERSION_CURRENT, new KeywordAnalyzer());
    IndexWriter w = new IndexWriter(indexDir, iwc);
    TaxonomyWriter tw = new DirectoryTaxonomyWriter(taxoDir);
    
    CategoryDocumentBuilder cdb = new CategoryDocumentBuilder(tw);
    ArrayList<CategoryPath> categoryPaths = new ArrayList<CategoryPath>(1);
    
    for (int i = 0; i < 100; i++) {
      categoryPaths.clear();
      categoryPaths.add(new CategoryPath("root",Integer.toString(i / 10), Integer.toString(i)));
      cdb.setCategoryPaths(categoryPaths);
      w.addDocument(cdb.build(new Document()));
    }
    IOUtils.close(tw, w);
  }

  /** search reader <code>r</code>*/
  private FacetResult searchWithFacets(IndexReader r,
      TaxonomyReader tr, FacetSearchParams fsp, final SamplingParams params)
          throws IOException {
    // a FacetsCollector with a sampling accumulator
    FacetsCollector fcWithSampling = new FacetsCollector(fsp, r, tr) {
      @Override
      protected FacetsAccumulator initFacetsAccumulator(FacetSearchParams facetSearchParams, IndexReader indexReader,
          TaxonomyReader taxonomyReader) {
        Sampler sampler = new RandomSampler(params, random());
        return new SamplingAccumulator(sampler, facetSearchParams, indexReader, taxonomyReader);
      }
    };
    
    IndexSearcher s = new IndexSearcher(r);
    s.search(new MatchAllDocsQuery(), fcWithSampling);
    
    // there's only one expected result, return just it.
    return fcWithSampling.getFacetResults().get(0);
  }
}
- 
2.19.1.windows.1

