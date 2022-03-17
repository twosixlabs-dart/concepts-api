package com.twosixtech.dart

import com.twosixlabs.dart.arangodb.Arango
import com.twosixlabs.dart.arangodb.tables.CanonicalDocsTable
import com.twosixlabs.dart.auth.tenant.indices.ArangoCorpusTenantIndex
import com.twosixlabs.dart.ontologies.OntologyRegistryService
import com.twosixlabs.dart.ontologies.dao.sql.SqlOntologyArtifactTable
import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.DartRootServlet
import com.twosixtech.dart.concepts.cdr.ArangoTenantCdrProvider
import com.twosixtech.dart.concepts.clusters.RestClusteringApi
import com.twosixtech.dart.concepts.controllers.{ClusteringController, DiscoveryController}
import com.twosixtech.dart.concepts.discovery.TenantDiscoveryService
import com.typesafe.config.{Config, ConfigFactory}
import org.clulab.concepts.ConceptDiscoverer
import org.scalatra.{LifeCycle, ScalatraServlet}
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.ServletContext
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong
import scala.util.Try

class ScalatraInit extends LifeCycle {
    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config: Config = ConfigFactory.load( "concepts-application" )

    val basePath : String = ApiStandards.DART_API_PREFIX_V1

    val rootController = new DartRootServlet(
        Some( basePath ),
        Some( getClass.getPackage.getImplementationVersion ),
    )

    val clusteringApi = new RestClusteringApi(
        config.getString( "clustering.host" ),
        config.getInt( "clustering.port" ),
    )

    val clusteringController : ScalatraServlet = {
        new ClusteringController(
            clusteringApi,
        )
    }

    import ConfigConstructors._
    import com.twosixlabs.dart.ontologies.dao.sql.PgSlickProfile.api._

    private val arango : Arango       = config.build[ Arango ]
    private val postgresDb : Database = config.build[ Database ]

    private val sentenceThreshold = Try( config.getDouble( "discovery.sentence.threshold" ) ).toOption
    private val frequencyThreshold = config.getDouble( "discovery.frequency.threshold" )
    private val topPick = config.getInt( "discovery.top.pick" )
    private val thresholdSimilarity = config.getDouble( "discovery.threshold.similarity" )

    private val clusteringPollDelayMs = config.getInt( "clustering.poll.delay.ms" )
    private val postgresTimeoutMs = config.getInt( "postgres.timeout.ms" )

    implicit val ec : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val discoveryController : DiscoveryController =
        new DiscoveryController(
            new TenantDiscoveryService(
                ConceptDiscoverer.fromConfig(),
                new ArangoTenantCdrProvider(
                    ArangoCorpusTenantIndex( arango ),
                    new CanonicalDocsTable( arango ),
                ),
                sentenceThreshold,
                frequencyThreshold,
                topPick,
                thresholdSimilarity,
            ),
            new OntologyRegistryService( new SqlOntologyArtifactTable( postgresDb, postgresTimeoutMs.milliseconds, ec ) ),
            clusteringApi,
            clusteringPollDelayMs.milliseconds,
        )

    // Initialize scalatra: mounts servlets
    override def init( context : ServletContext ) : Unit = {
        context.mount( rootController, "/*" )
        context.mount( clusteringController, basePath + "/cluster/*" )
        context.mount( discoveryController, basePath + "/discovery/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }
}
