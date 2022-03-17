package com.twosixtech.dart.concepts.clusters

import org.clulab.concepts.{DocumentLocation, ScoredConcept}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ClusterInputGenerator {

    def generateClusteringInputs( rankedConcepts: Seq[ ScoredConcept ] ): ClusterInputs = {
        val docIds = mutable.Set[ String ]()
        val concepts = ListBuffer[ ClusterableConcept ]()

        rankedConcepts foreach { rankedConcept =>
            rankedConcept.concept.documentLocations.foreach {
                case DocumentLocation( docId, _ ) => docIds += docId
                case _ =>
            }

            concepts += ClusterableConcept( rankedConcept.concept.phrase, rankedConcept.saliency )
        }

        ClusterInputs( docIds.toSet, concepts.toList )
    }

}
