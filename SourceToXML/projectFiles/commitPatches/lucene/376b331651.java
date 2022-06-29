From 376b331651bf3f5ab314eb3b3f66de836f8d1a9b Mon Sep 17 00:00:00 2001
From: Shai Erera <shaie@apache.org>
Date: Thu, 30 May 2013 10:36:59 +0000
Subject: [PATCH] LUCENE-5016:  Sampling can break FacetResult labeling

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1487807 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  5 ++
 .../lucene/facet/range/RangeAccumulator.java  |  7 +-
 .../apache/lucene/facet/sampling/Sampler.java |  2 +-
 .../facet/sampling/SamplingAccumulator.java   | 13 ++-
 .../facet/sampling/SamplingWrapper.java       | 14 +++-
 .../facet/search/TestFacetsCollector.java     | 81 ++++++++++++++++++-
 6 files changed, 110 insertions(+), 12 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 357d9dcc125..e90e5671019 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -118,6 +118,11 @@ Bug Fixes
   for scoringQueries. Instead use QueryValueSource to safely wrap arbitrary 
   queries and use them with CustomScoreQuery.  (John Wang, Robert Muir)
 
* LUCENE-5016: SamplingAccumulator returned inconsistent label if asked to
  aggregate a non-existing category. Also fixed a bug in RangeAccumulator if
  some readers did not have the requested numeric DV field.
  (Rob Audenaerde, Shai Erera)

 Optimizations
 
 * LUCENE-4936: Improve numeric doc values compression in case all values share
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/range/RangeAccumulator.java b/lucene/facet/src/java/org/apache/lucene/facet/range/RangeAccumulator.java
index 1ab3b738ebd..5cdf33e4f68 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/range/RangeAccumulator.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/range/RangeAccumulator.java
@@ -64,7 +64,7 @@ public class RangeAccumulator extends FacetsAccumulator {
         throw new IllegalArgumentException("only flat (dimension only) CategoryPath is allowed");
       }
 
      RangeFacetRequest<?> rfr = (RangeFacetRequest) fr;
      RangeFacetRequest<?> rfr = (RangeFacetRequest<?>) fr;
 
       requests.add(new RangeSet(rfr.ranges, rfr.categoryPath.components[0]));
     }
@@ -86,8 +86,11 @@ public class RangeAccumulator extends FacetsAccumulator {
       RangeSet ranges = requests.get(i);
 
       int[] counts = new int[ranges.ranges.length];
      for(MatchingDocs hits : matchingDocs) {
      for (MatchingDocs hits : matchingDocs) {
         NumericDocValues ndv = hits.context.reader().getNumericDocValues(ranges.field);
        if (ndv == null) {
          continue; // no numeric values for this field in this reader
        }
         final int length = hits.bits.length();
         int doc = 0;
         while (doc < length && (doc = hits.bits.nextSetBit(doc)) != -1) {
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/sampling/Sampler.java b/lucene/facet/src/java/org/apache/lucene/facet/sampling/Sampler.java
index e2498d054fc..ec39ef7a649 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/sampling/Sampler.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/sampling/Sampler.java
@@ -209,7 +209,7 @@ public abstract class Sampler {
       super(orig.categoryPath, num);
       this.orig = orig;
       setDepth(orig.getDepth());
      setNumLabel(orig.getNumLabel());
      setNumLabel(0); // don't label anything as we're over-sampling
       setResultMode(orig.getResultMode());
       setSortOrder(orig.getSortOrder());
     }
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingAccumulator.java b/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingAccumulator.java
index 7669aae4428..2a043940648 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingAccumulator.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingAccumulator.java
@@ -87,7 +87,7 @@ public class SamplingAccumulator extends StandardFacetsAccumulator {
     
     List<FacetResult> sampleRes = super.accumulate(docids);
     
    List<FacetResult> fixedRes = new ArrayList<FacetResult>();
    List<FacetResult> results = new ArrayList<FacetResult>();
     for (FacetResult fres : sampleRes) {
       // for sure fres is not null because this is guaranteed by the delegee.
       PartitionsFacetResultsHandler frh = createFacetResultsHandler(fres.getFacetRequest());
@@ -104,13 +104,18 @@ public class SamplingAccumulator extends StandardFacetsAccumulator {
       }
       
       // final labeling if allowed (because labeling is a costly operation)
      frh.labelResult(fres);
      fixedRes.add(fres); // add to final results
      if (fres.getFacetResultNode().ordinal == TaxonomyReader.INVALID_ORDINAL) {
        // category does not exist, add an empty result
        results.add(emptyResult(fres.getFacetResultNode().ordinal, fres.getFacetRequest()));
      } else {
        frh.labelResult(fres);
        results.add(fres);
      }
     }
     
     searchParams = original; // Back to original params
     
    return fixedRes; 
    return results; 
   }
 
   @Override
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingWrapper.java b/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingWrapper.java
index f9030677b9e..a6cdeeb6d80 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingWrapper.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/sampling/SamplingWrapper.java
@@ -10,6 +10,7 @@ import org.apache.lucene.facet.sampling.Sampler.SampleResult;
 import org.apache.lucene.facet.search.FacetResult;
 import org.apache.lucene.facet.search.ScoredDocIDs;
 import org.apache.lucene.facet.search.StandardFacetsAccumulator;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
 
 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
@@ -62,7 +63,7 @@ public class SamplingWrapper extends StandardFacetsAccumulator {
 
     List<FacetResult> sampleRes = delegee.accumulate(sampleSet.docids);
 
    List<FacetResult> fixedRes = new ArrayList<FacetResult>();
    List<FacetResult> results = new ArrayList<FacetResult>();
     SampleFixer sampleFixer = sampler.samplingParams.getSampleFixer();
     
     for (FacetResult fres : sampleRes) {
@@ -80,15 +81,20 @@ public class SamplingWrapper extends StandardFacetsAccumulator {
       }
       
       // final labeling if allowed (because labeling is a costly operation)
      frh.labelResult(fres);
      fixedRes.add(fres); // add to final results
      if (fres.getFacetResultNode().ordinal == TaxonomyReader.INVALID_ORDINAL) {
        // category does not exist, add an empty result
        results.add(emptyResult(fres.getFacetResultNode().ordinal, fres.getFacetRequest()));
      } else {
        frh.labelResult(fres);
        results.add(fres);
      }
     }
 
     if (shouldOversample) {
       delegee.searchParams = original; // Back to original params
     }
     
    return fixedRes; 
    return results; 
   }
 
   @Override
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/search/TestFacetsCollector.java b/lucene/facet/src/test/org/apache/lucene/facet/search/TestFacetsCollector.java
index 525d2d24984..bf011b42835 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/search/TestFacetsCollector.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/search/TestFacetsCollector.java
@@ -17,8 +17,20 @@ import org.apache.lucene.facet.params.CategoryListParams;
 import org.apache.lucene.facet.params.FacetIndexingParams;
 import org.apache.lucene.facet.params.FacetSearchParams;
 import org.apache.lucene.facet.params.PerDimensionIndexingParams;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.RangeAccumulator;
import org.apache.lucene.facet.range.RangeFacetRequest;
import org.apache.lucene.facet.sampling.RandomSampler;
import org.apache.lucene.facet.sampling.Sampler;
import org.apache.lucene.facet.sampling.SamplingAccumulator;
import org.apache.lucene.facet.sampling.SamplingParams;
import org.apache.lucene.facet.sampling.SamplingWrapper;
import org.apache.lucene.facet.sampling.TakmiSampleFixer;
 import org.apache.lucene.facet.search.FacetRequest.ResultMode;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesAccumulator;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
 import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
 import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
 import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
 import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
@@ -384,5 +396,72 @@ public class TestFacetsCollector extends FacetTestCase {
     
     IOUtils.close(taxo, taxoDir, r, indexDir);
   }
  

  @Test
  public void testLabeling() throws Exception {
    Directory indexDir = newDirectory(), taxoDir = newDirectory();

    // create the index
    IndexWriter indexWriter = new IndexWriter(indexDir, newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random())));
    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);
    FacetFields facetFields = new FacetFields(taxoWriter);
    Document doc = new Document();
    facetFields.addFields(doc, Arrays.asList(new CategoryPath("A/1", '/')));
    indexWriter.addDocument(doc);
    IOUtils.close(indexWriter, taxoWriter);
    
    DirectoryReader indexReader = DirectoryReader.open(indexDir);
    TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
    IndexSearcher searcher = new IndexSearcher(indexReader);
    // ask to count a non-existing category to test labeling
    FacetSearchParams fsp = new FacetSearchParams(new CountFacetRequest(new CategoryPath("B"), 5));
    
    final SamplingParams sampleParams = new SamplingParams();
    sampleParams.setMaxSampleSize(100);
    sampleParams.setMinSampleSize(100);
    sampleParams.setSamplingThreshold(100);
    sampleParams.setOversampleFactor(1.0d);
    if (random().nextBoolean()) {
      sampleParams.setSampleFixer(new TakmiSampleFixer(indexReader, taxoReader, fsp));
    }
    final Sampler sampler = new RandomSampler(sampleParams, random());
    
    FacetsAccumulator[] accumulators = new FacetsAccumulator[] {
      new FacetsAccumulator(fsp, indexReader, taxoReader),
      new StandardFacetsAccumulator(fsp, indexReader, taxoReader),
      new SamplingAccumulator(sampler, fsp, indexReader, taxoReader),
      new AdaptiveFacetsAccumulator(fsp, indexReader, taxoReader),
      new SamplingWrapper(new StandardFacetsAccumulator(fsp, indexReader, taxoReader), sampler)
    };
    
    for (FacetsAccumulator fa : accumulators) {
      FacetsCollector fc = FacetsCollector.create(fa);
      searcher.search(new MatchAllDocsQuery(), fc);
      List<FacetResult> facetResults = fc.getFacetResults();
      assertNotNull(facetResults);
      assertEquals("incorrect label returned for " + fa, fsp.facetRequests.get(0).categoryPath, facetResults.get(0).getFacetResultNode().label);
    }
    
    try {
      // SortedSetDocValuesAccumulator cannot even be created in such state
      assertNull(new SortedSetDocValuesAccumulator(fsp, new SortedSetDocValuesReaderState(indexReader)));
      // if this ever changes, make sure FacetResultNode is labeled correctly 
      fail("should not have succeeded to execute a request over a category which wasn't indexed as SortedSetDVField");
    } catch (IllegalArgumentException e) {
      // expected
    }

    fsp = new FacetSearchParams(new RangeFacetRequest<LongRange>("f", new LongRange("grr", 0, true, 1, true)));
    RangeAccumulator ra = new RangeAccumulator(fsp, indexReader);
    FacetsCollector fc = FacetsCollector.create(ra);
    searcher.search(new MatchAllDocsQuery(), fc);
    List<FacetResult> facetResults = fc.getFacetResults();
    assertNotNull(facetResults);
    assertEquals("incorrect label returned for RangeAccumulator", fsp.facetRequests.get(0).categoryPath, facetResults.get(0).getFacetResultNode().label);

    IOUtils.close(indexReader, taxoReader);

    IOUtils.close(indexDir, taxoDir);
  }

 }
- 
2.19.1.windows.1

