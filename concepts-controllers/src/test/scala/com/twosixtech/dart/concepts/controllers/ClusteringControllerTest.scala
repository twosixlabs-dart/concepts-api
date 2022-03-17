package com.twosixtech.dart.concepts.controllers

import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.rest.scalatra.models.FailureResponse
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixtech.dart.concepts.clusters.{ClusteringApi, ClusteringResult, ClusteringResultsResponse, JobPollRequest, JobPollResponse, JobResultsRequest, JobSubmissionResponse, NodeSimilarity, ReclusterRequest, RescoreRequest, RescoringResultsResponse}
import com.twosixtech.dart.concepts.models.{ClusterResults, DartReclusterRequest, DartRescoreRequest, PollResponse, SimilarConcept, SingleResult}
import org.scalatra.test.scalatest.ScalatraSuite

class ClusteringControllerTest extends StandardTestBase3x with ScalatraSuite {

    val api : ClusteringApi = mock[ ClusteringApi ]

    addServlet( new ClusteringController( api ), "/clustering" )

    behavior of "POST /clustering/recluster/submit"

    it should "return job id if body is correct format" in {
        reset( api )
        val jobId = "test-job-id"
        val concepts = Seq( "concept-1", "concept-2", "concept-3" )
        val request = DartReclusterRequest( concepts, "test-metadata", jobId )
        when( api.reclusterSubmit( any ) ).thenReturn( JobSubmissionResponse( jobId, "test-msg", true ) )
        post( "/clustering/recluster/submit", JsonFormat.marshalFrom( request ).get ) {
            response.status shouldBe 200
            val res = JsonFormat.unmarshalTo( response.body, classOf[ PollResponse ] ).get
            res.jobId shouldBe jobId
            res.complete shouldBe false
            res.message shouldBe "test-msg"

            verify( api, times( 1 ) ).reclusterSubmit( ReclusterRequest( request.phrases, request.ontology, jobId ) )
        }
    }

    behavior of "GET /clustering/recluster/poll/:jobId"

    it should "return appropriate response if cluster is not ready" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.reclusterPoll( any ) ).thenReturn( JobPollResponse( jobId, false, "test-msg", true ) )
        get( s"/clustering/recluster/poll/${jobId}" ) {
            response.status shouldBe 200

            val res = JsonFormat.unmarshalTo[ PollResponse ]( response.body, classOf[ PollResponse ] ).get
            res.message shouldBe "test-msg"
            res.jobId shouldBe jobId
            res.complete shouldBe false

            verify( api, times( 1 ) ).reclusterPoll( JobPollRequest( jobId ) )
        }
    }

    it should "return appropriate response if cluster is ready" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.reclusterPoll( any ) ).thenReturn( JobPollResponse( jobId, true, "test-msg", true ) )
        get( s"/clustering/recluster/poll/${jobId}" ) {
            response.status shouldBe 200

            val res = JsonFormat.unmarshalTo[ PollResponse ]( response.body, classOf[ PollResponse ] ).get
            res.message shouldBe "test-msg"
            res.jobId shouldBe jobId
            res.complete shouldBe true

            verify( api, times( 1 ) ).reclusterPoll( JobPollRequest( jobId ) )
        }
    }

    behavior of "GET /clustering/recluster/results/:jobId"


    it should "return 404 if job doesn't exist or isn't complete" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.reclusterResults( any ) ).thenThrow( new IllegalArgumentException( s"${jobId} does not exist or is not ready" ) )
        get( s"/clustering/recluster/results/${jobId}" ) {
            response.status shouldBe 404
            val res = JsonFormat.unmarshalTo[ FailureResponse ]( response.body, classOf[ FailureResponse ] ).get
            res.message should ( include( jobId ) and include( "Resource not found" ) )
            res.status shouldBe 404

            verify( api, times( 1 ) ).reclusterResults( JobResultsRequest( jobId ) )
        }
    }

    it should "return 200 and results if jobId is complete" in {
        reset( api )
        val jobId = "test-job-id"
        val testResults = Seq(
            ClusteringResult( "test-name", Nil, "test-id", Nil, Nil, "13.345324234", Nil, "test-name test-name-2 test-name_test-name-3" ),
            ClusteringResult( "test-name-a", Nil, "test-id-a", Nil, Nil, "11.7889776", Nil, "test-name-a test-name-a2 test-name_test-name-a3" ),
            ClusteringResult( "test-name-b", Nil, "test-id-b", Nil, Nil, "9.002342301", Seq( NodeSimilarity( "wm/something/else", 0.992342342, 0 ) ), "test-name-b test-name-b2 test-name_test-name-b3" ),
        )

        when( api.reclusterResults( any ) ).thenReturn( ClusteringResultsResponse( testResults ) )
        get( s"/clustering/recluster/results/${jobId}" ) {
            response.status shouldBe 200
            val res = JsonFormat.unmarshalTo[ ClusterResults ]( response.body, classOf[ ClusterResults ] ).get
            res.jobId shouldBe jobId
            res.clusters.size shouldBe 3
            res.clusters shouldBe {
                Seq(
                    SingleResult( "test-id", 13.345324234, "test-name", Seq( "test-name", "test-name-2", "test-name_test-name-3" ), Nil  ),
                    SingleResult( "test-id-a", 11.7889776, "test-name-a", Seq( "test-name-a", "test-name-a2", "test-name_test-name-a3" ), Nil ),
                    SingleResult( "test-id-b", 9.002342301, "test-name-b", Seq( "test-name-b", "test-name-b2", "test-name_test-name-b3" ), Seq( SimilarConcept( Seq( "wm", "something", "else" ), 0.992342342 ) ) ),
                )
            }

            verify( api, times( 1 ) ).reclusterResults( JobResultsRequest( jobId ) )
        }
    }

    behavior of "POST /clustering/rescore/submit"

    it should "return job id if body is correct format" in {
        reset( api )
        val jobId = "test-job-id"
        val request = DartRescoreRequest( "test-metadata", "test-job-id" )
        when( api.rescoreSubmit( any ) ).thenReturn( JobSubmissionResponse( jobId, "test-msg", true ) )
        post( "/clustering/rescore/submit", JsonFormat.marshalFrom( request ).get ) {
            response.status shouldBe 200
            val res = JsonFormat.unmarshalTo( response.body, classOf[ PollResponse ] ).get
            res.jobId shouldBe jobId
            res.complete shouldBe false
            res.message shouldBe "test-msg"

            verify( api, times( 1 ) ).rescoreSubmit( RescoreRequest( request.ontology, request.clusterJobId ) )
        }
    }

    behavior of "GET /clustering/rescore/poll/:jobId"

    it should "return appropriate response if cluster is not ready" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.rescorePoll( any ) ).thenReturn( JobPollResponse( jobId, false, "test-msg", true ) )
        get( s"/clustering/rescore/poll/${jobId}" ) {
            response.status shouldBe 200

            val res = JsonFormat.unmarshalTo[ PollResponse ]( response.body, classOf[ PollResponse ] ).get
            res.message shouldBe "test-msg"
            res.jobId shouldBe jobId
            res.complete shouldBe false

            verify( api, times( 1 ) ).rescorePoll( JobPollRequest( jobId ) )
        }
    }

    it should "return appropriate response if cluster is ready" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.rescorePoll( any ) ).thenReturn( JobPollResponse( jobId, true, "test-msg", true ) )
        get( s"/clustering/rescore/poll/${jobId}" ) {
            response.status shouldBe 200

            val res = JsonFormat.unmarshalTo[ PollResponse ]( response.body, classOf[ PollResponse ] ).get
            res.message shouldBe "test-msg"
            res.jobId shouldBe jobId
            res.complete shouldBe true

            verify( api, times( 1 ) ).rescorePoll( JobPollRequest( jobId ) )
        }
    }

    behavior of "GET /clustering/rescore/results/:jobId"


    it should "return 404 if job doesn't exist or isn't complete" in {
        reset( api )
        val jobId = "test-job-id"
        when( api.rescoreResults( any ) ).thenThrow( new IllegalArgumentException( s"${jobId} does not exist or is not ready" ) )
        get( s"/clustering/rescore/results/${jobId}" ) {
            response.status shouldBe 404
            val res = JsonFormat.unmarshalTo[ FailureResponse ]( response.body, classOf[ FailureResponse ] ).get
            res.message should ( include( jobId ) and include( "Resource not found" ) )
            res.status shouldBe 404

            verify( api, times( 1 ) ).rescoreResults( JobResultsRequest( jobId ) )
        }
    }

    it should "return 200 and results if jobId is complete" in {
        reset( api )
        val jobId = "test-job-id"
        val testResults = Seq(
            ClusteringResult( "test-name", Nil, "test-id", Nil, Nil, "13.345324234", Nil, "test-name test-name-2 test-name_test-name-3" ),
            ClusteringResult( "test-name-a", Nil, "test-id-a", Nil, Nil, "11.7889776", Nil, "test-name-a test-name-a2 test-name_test-name-a3" ),
            ClusteringResult( "test-name-b", Nil, "test-id-b", Nil, Nil, "9.002342301", Seq( NodeSimilarity( "wm/something/else", 0.992342342, 0 ) ), "test-name-b test-name-b2 test-name_test-name-b3" ),
            )

        when( api.rescoreResults( any ) ).thenReturn( RescoringResultsResponse( testResults ) )
        get( s"/clustering/rescore/results/${jobId}" ) {
            response.status shouldBe 200
            val res = JsonFormat.unmarshalTo[ ClusterResults ]( response.body, classOf[ ClusterResults ] ).get
            res.jobId shouldBe jobId
            res.clusters.size shouldBe 3
            res.clusters shouldBe {
                Seq(
                    SingleResult( "test-id", 13.345324234, "test-name", Seq( "test-name", "test-name-2", "test-name_test-name-3" ), Nil  ),
                    SingleResult( "test-id-a", 11.7889776, "test-name-a", Seq( "test-name-a", "test-name-a2", "test-name_test-name-a3" ), Nil ),
                    SingleResult( "test-id-b", 9.002342301, "test-name-b", Seq( "test-name-b", "test-name-b2", "test-name_test-name-b3" ), Seq( SimilarConcept( Seq( "wm", "something", "else" ), 0.992342342 ) ) ),
                )
            }

            verify( api, times( 1 ) ).rescoreResults( JobResultsRequest( jobId ) )
        }
    }

    override def header = null
}
