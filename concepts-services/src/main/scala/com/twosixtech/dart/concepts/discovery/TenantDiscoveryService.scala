package com.twosixtech.dart.concepts.discovery

import com.twosixlabs.dart.auth.tenant.DartTenant
import com.twosixtech.dart.concepts.cdr.TenantCdrProvider
import com.twosixtech.dart.concepts.clusters.{ ClusterInputGenerator, ClusterInputs }
import org.clulab.concepts.{ ConceptDiscoverer, ScoredConcept }
import com.twosixtech.dart.concepts.cdr.CdrConversion._
import org.slf4j.{ Logger, LoggerFactory }

class TenantDiscoveryService( conceptDiscovery : ConceptDiscoverer,
                              cdrProvider : TenantCdrProvider,
                              sentenceThreshold : Option[ Double ],
                              frequencyThreshold : Double,
                              topPick : Int,
                              thresholdSimilarity : Double ) {

    private val LOG = LoggerFactory.getLogger( getClass )

    def discoverFor( tenant : String ) : ClusterInputs = {

        LOG.info( s"Running discovery with:\nsentenceThreshold:$sentenceThreshold,\nfrequencyThreshold: $frequencyThreshold\ntopPick: ${topPick}\nthresholdSimilarity: $thresholdSimilarity")

        val cdrs = cdrProvider.tenantDocs( tenant )
        LOG.info( s"Running discovery on ${cdrs.length} CDRs from tenant $tenant:" )
        LOG.info( cdrs.map( _.documentId ).mkString( ", " ) )
        val discoveryDocs = cdrs.map( _.discoveryDocument )
        LOG.info( s"Running on ${discoveryDocs.map( v => s"${v.docid} -> ${v.sentences.length}").mkString( "," )}")
        val discoveredConcepts = conceptDiscovery
          .discoverMostFrequentConcepts( discoveryDocs, sentenceThreshold, frequencyThreshold, topPick )

        LOG.info( s"Discovered ${discoveredConcepts.length} concepts:" )
        LOG.info( discoveredConcepts.map( _.phrase ).mkString( ", " ) )

        val rankedConcepts : Seq[ ScoredConcept ] =
            conceptDiscovery
              .rankConcepts( discoveredConcepts, thresholdSimilarity )

        LOG.info( s"Ranked ${rankedConcepts.length} concepts:" )
        LOG.info( rankedConcepts.map( _.concept ).mkString( ", " ) )

        ClusterInputGenerator.generateClusteringInputs( rankedConcepts )
    }

}
