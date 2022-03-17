package com.twosixtech.dart.clusters

import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixtech.dart.concepts.clusters.{ClusterInputGenerator, ClusterableConcept}
import org.clulab.concepts.{Concept, DocumentLocation, ScoredConcept}

class ClusterInputGeneratorTest extends StandardTestBase3x {

    behavior of "ClusterInputGenerator.generateClusteringInputs"

    val testConcepts = Seq(
        ScoredConcept( Concept( "test-word-1", Set( DocumentLocation( "test-doc-id-1", 5 ), DocumentLocation( "test-doc-id-2", 53 ) ) ), 0.2342332 ),
        ScoredConcept( Concept( "test-word-2", Set( DocumentLocation( "test-doc-id-2", 23 ), DocumentLocation( "test-doc-id-3", 87 ) ) ), 0.78987656 ),
        ScoredConcept( Concept( "test-word-3", Set( DocumentLocation( "test-doc-id-1", 43 ), DocumentLocation( "test-doc-id-4", 342 ) ) ), 0.0003454 )
        )

    it should "generate clustering inputs from a sequence of scored concepts" in {
        val clusterInputs = ClusterInputGenerator.generateClusteringInputs( testConcepts )

        clusterInputs.concepts should contain( ClusterableConcept( "test-word-1", 0.2342332 ) )
        clusterInputs.concepts should contain( ClusterableConcept( "test-word-2", 0.78987656 ) )
        clusterInputs.concepts should contain( ClusterableConcept( "test-word-3", 0.0003454 ) )

        clusterInputs.docIds shouldBe Set( "test-doc-id-1", "test-doc-id-2", "test-doc-id-3", "test-doc-id-4" )
    }

}
