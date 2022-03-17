//package com.twosixtech.dart.concepts.discovery
//
//import better.files.Resource
//import com.twosixlabs.cdr4s.core.CdrDocument
//import com.twosixlabs.cdr4s.json.dart.{DartCdrDocumentDto, DartJsonFormat}
//import com.twosixtech.dart.concepts.cdr.TenantCdrProvider
//import com.typesafe.config.ConfigFactory
//import org.clulab.concepts.ConceptDiscoverer
//import org.scalatest.flatspec.AnyFlatSpecLike
//
//import scala.util.Try
//
//object TestCdrProvider extends TenantCdrProvider {
//    val dartFormat = new DartJsonFormat
//
//    override def tenantDocs(
//        tenant: String
//    ): Seq[ CdrDocument ] = {
//        val cdrsJsonl = Resource.getAsString( "cdrs.jsonl" )
//        val cdrJsons = cdrsJsonl.split( "\n" )
//        cdrJsons.map( cdrJson => dartFormat.unmarshalCdr( cdrJson ).get )
//    }
//}
//
//
//class TenantDiscoveryServiceTest extends AnyFlatSpecLike {
//
//    val config = ConfigFactory.load( "test.conf" )
//
//    private val sentenceThreshold = Try( config.getDouble( "discovery.sentence.threshold" ) ).toOption
//    private val frequencyThreshold = config.getDouble( "discovery.frequency.threshold" )
//    private val topPick = config.getInt( "discovery.top.pick" )
//    private val thresholdSimilarity = config.getDouble( "discovery.threshold.similarity" )
//
//    private val conceptDiscoveryService = ConceptDiscoverer.fromConfig( ConfigFactory.load( "discovery.conf" ) )
//
//    println( sentenceThreshold )
//    println( frequencyThreshold )
//    println( topPick )
//    println( thresholdSimilarity )
//
//    private val tenantDiscoveryService = new TenantDiscoveryService(
//        conceptDiscoveryService,
//        TestCdrProvider,
//        sentenceThreshold,
//        frequencyThreshold,
//        topPick,
//        thresholdSimilarity
//    )
//
//    behavior of "DiscoveryConcepts"
//
//    it should "discovery concepts" in {
//        val res = tenantDiscoveryService.discoverFor( "doesntmatter" )
//        println( res )
//    }
//
//}
