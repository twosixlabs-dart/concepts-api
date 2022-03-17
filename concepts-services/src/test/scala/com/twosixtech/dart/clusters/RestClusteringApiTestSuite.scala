package com.twosixtech.dart.clusters

import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixtech.dart.concepts.clusters.{ClusteringApi, ClusteringApiJsonFormat, ClusteringResult, ClusteringResultsResponse, JobPollRequest, JobPollResponse, JobResultsRequest, JobSubmissionResponse, NodeSimilarity, ReclusterRequest, RestClusteringApi}
import okhttp3.mockwebserver.{MockResponse, MockWebServer}

import java.net.ConnectException
import scala.util.Random

class RestClusteringApiTestSuite extends StandardTestBase3x {

    private val format : ClusteringApiJsonFormat = new ClusteringApiJsonFormat

    behavior of "REST Clustering API"

    import RestClusteringApi._

    it should "execute a successful SUBMIT request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = ReclusterRequest( allowedWords = Seq( "a", "b" ), ontologyMetadata = "test-data", clusterJobId = "test-job" )
        val expectedResponse = JobSubmissionResponse( jobId = "1", message = "my message", success = true )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 200 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterSubmit( request )
        actualResponse shouldBe expectedResponse

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${SUBMIT_OP}"
    }

    it should "execute a failed SUBMIT request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = ReclusterRequest( allowedWords = Seq( "a", "b" ), ontologyMetadata = "test-data", clusterJobId = "test-job" )
        val expectedResponse = JobSubmissionResponse( jobId = null, message = "bbn message", success = false )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 400 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterSubmit( request )
        actualResponse shouldBe expectedResponse.copy( message = "unexpected response code: 400" )

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${SUBMIT_OP}"
    }

    it should "handle client errors for SUBMIT request" in {
        val badPort = 1024
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", badPort )

        val request = ReclusterRequest( allowedWords = Seq( "a", "b" ), ontologyMetadata = "test-data", clusterJobId = "test-job" )
        val expectedResponse = JobSubmissionResponse( jobId = null, message = s"client error: ${classOf[ ConnectException ].getCanonicalName}", success = false )

        val actualResponse = clusteringClient.reclusterSubmit( request )
        actualResponse shouldBe expectedResponse
    }

    it should "execute a successful POLL request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = JobPollRequest( jobId = "1" )
        val expectedResponse = JobPollResponse( jobId = "1", isReady = true, message = null, success = true )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 200 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterPoll( request )
        actualResponse shouldBe expectedResponse

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${POLL_OP}?job_id=${request.jobId}"
    }

    it should "execute a failed POLL request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = JobPollRequest( jobId = "1" )
        val expectedResponse = JobPollResponse( jobId = "1", isReady = false, message = "error", success = false )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 400 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterPoll( request )
        actualResponse shouldBe expectedResponse.copy( message = "unexpected response code: 400" )

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${POLL_OP}?job_id=${request.jobId}"
    }

    it should "handle client errors for POLL request" in {
        val badPort = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", badPort )

        val request = JobPollRequest( jobId = "1" )
        val expectedResponse = JobPollResponse( jobId = "1", isReady = false, message = s"client error: ${classOf[ ConnectException ].getCanonicalName}", success = false )

        val actualResponse = clusteringClient.reclusterPoll( request )
        actualResponse shouldBe expectedResponse
    }

    it should "execute a successful RESULTS request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = JobResultsRequest( jobId = "1" )
        val clusteringResults = ClusteringResult( clusterName = "name",
                                                  comments = Seq( "comment" ),
                                                  id = "1",
                                                  novelNodes = Seq( "novel" ),
                                                  overlapNodes = Seq( "node" ),
                                                  score = "1.777",
                                                  similarity = Seq( NodeSimilarity( "similarity", 1.023423, 0 ) ),
                                                  wordsString = "words" )
        val expectedResponse = ClusteringResultsResponse( clusteringResults = Seq( clusteringResults ) )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 200 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterResults( request )
        actualResponse shouldBe expectedResponse

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${RESULTS_OP}?job_id=${request.jobId}"
    }

    it should "execute a failed RESULTS request" in {
        val testPort : Int = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", testPort )

        val request = JobResultsRequest( jobId = "1" )
        val expectedResponse = ClusteringResultsResponse( clusteringResults = Seq() )

        val mockServer : MockWebServer = new MockWebServer()
        mockServer.enqueue( new MockResponse()
                              .setResponseCode( 400 )
                              .setBody( format.toApiJson( expectedResponse ).get ) )
        mockServer.start( testPort )

        val actualResponse = clusteringClient.reclusterResults( request )
        actualResponse shouldBe expectedResponse

        val actualRequest = mockServer.takeRequest()
        actualRequest.getPath shouldBe s"/${API_ROOT}${CLUSTERING_PATH}${RESULTS_OP}?job_id=${request.jobId}"
    }

    it should "handle client errors for RESULTS request" in {
        val badPort = randomPort()
        val clusteringClient : ClusteringApi = new RestClusteringApi( s"http://localhost", badPort )

        val request = JobResultsRequest( jobId = "1" )
        val expectedResponse = ClusteringResultsResponse( clusteringResults = Seq() )

        val actualResponse = clusteringClient.reclusterResults( request )
        actualResponse shouldBe expectedResponse
    }

    private def randomPort( ) : Int = 4000 + Random.nextInt( 1000 )

}
