package com.twosixtech.dart.concepts.controllers

import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.ontologies.api.{OntologyArtifact, OntologyRegistry}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixtech.dart.concepts.clusters.{ClusterInputs, ClusterRequest, ClusteringResultsResponse, JobPollResponse, JobSubmissionResponse, RestClusteringApi}
import com.twosixtech.dart.concepts.discovery.TenantDiscoveryService
import com.twosixtech.dart.concepts.models.PollResponse
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{Success, Try}

class DiscoveryControllerTest extends StandardTestBase3x with ScalatraSuite with BeforeAndAfterEach {

    val discoveryService : TenantDiscoveryService = mock[ TenantDiscoveryService ]
    val ontologyRegistry : OntologyRegistry = mock[ OntologyRegistry ]
    val clusteringService : RestClusteringApi = mock[ RestClusteringApi ]

    val discoveryController : DiscoveryController =
        new DiscoveryController( discoveryService,
                                 ontologyRegistry,
                                 clusteringService,
                                 pollDelay = 20.minutes )

    override def beforeEach( ): Unit = {
        super.beforeEach()
        reset( ontologyRegistry )
        reset( discoveryService )
        reset( clusteringService )
        discoveryController.ClusterJob.cancel()
    }

    override def afterEach( ): Unit = {
        reset( ontologyRegistry )
        reset( discoveryService )
        reset( clusteringService )
        discoveryController.ClusterJob.cancel()
        super.afterEach()
    }

    addServlet( discoveryController, "/*" )

    behavior of "POST /cluster/submit/:tenant"

    it should "call ontologyRegistry.latest(tenant), return 200 with no body if tenant/ontology exists" in {
        val testTenant = "test-tenant"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, "test-ontology", Nil ) ) ) )

        post( s"/cluster/submit/$testTenant" ) {
            body shouldBe ""
            status shouldBe 200
        }
    }

    it should "call ontologyRegistry.latest(tenant), return 404 with no body if tenant/ontology does not exist" in {
        val testTenant = "test-tenant"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( None ) )

        post( s"/cluster/submit/$testTenant" ) {
            status shouldBe 404
            body should include ( s"no ontology found for tenant ${testTenant} or tenant ${testTenant} does not exist" )
        }
    }

    behavior of "GET /cluster/poll"

    it should "return appropriate message if job has not started" in {
        discoveryController.ClusterJob.cancel()
        when( clusteringService.clusterPoll() )
          .thenReturn( JobPollResponse( "x", false, "no job", false ) )
        get( "/cluster/poll" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe "N/A"
            pollResponse.complete shouldBe false
            pollResponse.message shouldBe "No job has been submitted"
        }
    }

    it should "return appropriate message if job has started" in {
        val testTenant = "test-tenant"
        var sleep = true
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, "test-ontology", Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenAnswer( { while( sleep ) Thread.sleep( 50 ); ClusterInputs( Set.empty, Seq.empty ) } )

        post( s"/cluster/submit/$testTenant" ) {
            status shouldBe 200
        }

        get( "/cluster/poll" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe "N/A"
            pollResponse.complete shouldBe false
            pollResponse.message shouldBe "Clustering"
            sleep = false
            Thread.sleep( 500 )
        }
    }

    it should "return appropriate message if results are being retrieved" in {
        val testTenant = "test-tenant"
        val clusterJob = "test-job"
        val testOntology = "test-ontology"
        var sleep = true
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, testOntology, Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenReturn( ClusterInputs( Set.empty, Seq.empty ) )
        when( clusteringService.clusterSubmit( ClusterRequest( Seq.empty, testOntology, Seq.empty ) ) )
          .thenReturn( JobSubmissionResponse( clusterJob, "success", true ) )
        when( clusteringService.clusterPoll() )
          .thenReturn( JobPollResponse( clusterJob, true, "ready", true ) )
        when( clusteringService.clusterResults() )
          .thenAnswer( { while( sleep ) Thread.sleep( 50 ); ClusteringResultsResponse() } )

        post( s"/cluster/submit/$testTenant" ) {
            status shouldBe 200
        }

        get( "/cluster/poll" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe "N/A"
            pollResponse.complete shouldBe false
            pollResponse.message shouldBe "Generating results"
            sleep = false
            Thread.sleep( 500 )
        }
    }

    it should "return appropriate message when results are ready" in {
        val testTenant = "test-tenant"
        val clusterJob = "test-job"
        val testOntology = "test-ontology"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, testOntology, Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenReturn( ClusterInputs( Set.empty, Seq.empty ) )
        when( clusteringService.clusterSubmit( ClusterRequest( Seq.empty, testOntology, Seq.empty ) ) )
          .thenReturn( JobSubmissionResponse( clusterJob, "success", true ) )
        when( clusteringService.clusterPoll() )
          .thenReturn( JobPollResponse( clusterJob, true, "ready", true ) )
        when( clusteringService.clusterResults() )
          .thenReturn( ClusteringResultsResponse() )

        post( s"/cluster/submit/$testTenant" ) {
            status shouldBe 200
        }

        get( "/cluster/poll" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe "N/A"
            pollResponse.complete shouldBe true
            pollResponse.message shouldBe "Succeeded"
        }
    }

    behavior of "POST /submit/:tenant"

    it should "call ontologyRegistry.latest(tenant), return 200 with a UUID if tenant/ontology exists" in {
        val testTenant = "test-tenant"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, "test-ontology", Nil ) ) ) )

        post( s"/submit/$testTenant" ) {
            Try( UUID.fromString( body ) ).isSuccess shouldBe true
            status shouldBe 200
        }
    }

    it should "call ontologyRegistry.latest(tenant), return 404 with no body if tenant/ontology does not exist" in {
        val testTenant = "test-tenant"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( None ) )

        post( s"/submit/$testTenant" ) {
            status shouldBe 404
            body should include ( s"no ontology found for tenant ${testTenant} or tenant ${testTenant} does not exist" )
        }
    }

    behavior of "GET /poll/johId"

    it should "return 404 if job does not exist" in {
        get( "/cluster/poll/512202cf-4974-420c-bf7f-40b2b4804c3f" ) {
            status shouldBe 404
            body should include( "512202cf-4974-420c-bf7f-40b2b4804c3f" )
        }
    }

    it should "return appropriate message if job is pending" in {
        val testTenant = "test-tenant"
        var sleep = true
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, "test-ontology", Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenAnswer( { while( sleep ) Thread.sleep( 50 ); ClusterInputs( Set.empty, Seq.empty ) } )

        var jobId : String = ""
        post( s"/submit/$testTenant" ) {
            status shouldBe 200
            jobId = body
        }

        get( s"/poll/$jobId" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe jobId
            pollResponse.complete shouldBe false
            pollResponse.message shouldBe "Pending"
            sleep = false
            Thread.sleep( 500 )
        }
    }

    it should "return appropriate message when results are ready" in {
        val testTenant = "test-tenant"
        val testOntology = "test-ontology"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, testOntology, Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenReturn( ClusterInputs( Set.empty, Seq.empty ) )

        var jobId : String = ""
        post( s"/submit/$testTenant" ) {
            status shouldBe 200
            jobId = body
        }

        get( s"/poll/$jobId" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe jobId
            pollResponse.complete shouldBe true
            pollResponse.message shouldBe "Succeeded"
        }
    }

    it should "return appropriate message when discovery failed" in {
        val testTenant = "test-tenant"
        val testOntology = "test-ontology"
        when( ontologyRegistry.latest( testTenant ) )
          .thenReturn( Success( Some( OntologyArtifact( "x", testTenant, 0, -1, testOntology, Nil ) ) ) )
        when( discoveryService.discoverFor( testTenant ) )
          .thenThrow( new Exception( "test error message" ) )

        var jobId : String = ""
        post( s"/submit/$testTenant" ) {
            status shouldBe 200
            jobId = body
        }

        get( s"/poll/$jobId" ) {
            status shouldBe 200
            val pollResponse = JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ).get
            pollResponse.jobId shouldBe jobId
            pollResponse.complete shouldBe false
            pollResponse.message should ( include( "Failed" ) and include( "test error message" ) )
        }
    }

    behavior of "GET /results/johId"

    it should "return 404 if job does not exist" in {
        val id = UUID.randomUUID()
        get( s"/results/$id" ) {
            status shouldBe 404
            body should include( id.toString )
        }
    }

    it should "return 422 if job exists and is pending" in {
        val id = UUID.randomUUID()
        discoveryController.DiscoveryJobs.setStatus( id, discoveryController.DiscoveryJobs.Pending )
        get( s"/results/$id" ) {
            status shouldBe 422
            body should include ( s"Results are not ready for job ${id}" )
        }
    }

    it should "return 422 if job exists and is failed" in {
        val id = UUID.randomUUID()
        val msg = "test-message"
        discoveryController.DiscoveryJobs.setStatus( id, discoveryController.DiscoveryJobs.Failed( msg ) )
        get( s"/results/$id" ) {
            status shouldBe 422
            body should include ( s"Results are not ready for job ${id}" )
        }
    }

    it should "return 200 with ClusterRequest if job exists and has succeeded" in {
        val id = UUID.randomUUID()
        val res = ClusterRequest( Seq( "word-1", "word-2" ), "test-ontology-yml-text", Seq( "doc-1", "doc-2" ) )
        discoveryController.DiscoveryJobs.setStatus( id, discoveryController.DiscoveryJobs.Succeeded( res ) )
        get( s"/results/$id" ) {
            status shouldBe 200
            val clusteringRes = JsonFormat.unmarshalTo[ ClusterRequest ]( body, classOf[ ClusterRequest ] )
            clusteringRes.toOption should contain( res )
        }
    }


    override def header = null
}
