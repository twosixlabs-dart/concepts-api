package com.twosixtech.dart.clusters

import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixtech.dart.concepts.clusters.{ClusteringApiJsonFormat, ClusteringResult, ClusteringResultsResponse, JobPollRequest, JobPollResponse, JobResultsRequest, JobSubmissionResponse, NodeSimilarity, ReclusterRequest}

class ClusteringApiJsonFormatTest extends StandardTestBase3x {

    private val format : ClusteringApiJsonFormat = new ClusteringApiJsonFormat

    behavior of "Clustering API Json Format"

    it should "marshal and unmarshal `submit` request and responses" in {
        val submitRequest = ReclusterRequest( allowedWords = Seq( "ab", "cd" ), "test-data", "test-job" )
        val requestJson : String = format.toApiJson( submitRequest ).get
        val unmarshalledRequest : ReclusterRequest = format.fromApiJson[ ReclusterRequest ]( requestJson, classOf[ ReclusterRequest ] ).get
        unmarshalledRequest shouldBe submitRequest

        val submitResponse = JobSubmissionResponse( jobId = "job1", message = "started", success = true )
        val responseJson : String = format.toApiJson( submitResponse ).get
        val unmarshalledResponse : JobSubmissionResponse = format.fromApiJson[ JobSubmissionResponse ]( responseJson, classOf[ JobSubmissionResponse ] ).get
        unmarshalledResponse shouldBe submitResponse
    }

    it should "marshal and unmarshal `poll` request and responses" in {
        val pollRequest = JobPollRequest( "job1" )
        val requestJson : String = format.toApiJson( pollRequest ).get
        val unmarshalledRequest : JobPollRequest = format.fromApiJson[ JobPollRequest ]( requestJson, classOf[ JobPollRequest ] ).get
        unmarshalledRequest shouldBe pollRequest

        val pollResponse = JobPollResponse( jobId = "job1", isReady = false, message = "not ready", success = true )
        val responseJson : String = format.toApiJson( pollResponse ).get
        val unmarshalledResponse : JobPollResponse = format.fromApiJson[ JobPollResponse ]( responseJson, classOf[ JobPollResponse ] ).get
        unmarshalledResponse shouldBe pollResponse
    }

    it should "marshal and unmarshal `results` request and responses" in {
        val results = ClusteringResult( clusterName = "cluster-1",
                                        comments = Seq( "a comment" ),
                                        id = "id1",
                                        novelNodes = Seq( "a", "b" ),
                                        overlapNodes = Seq( "1", "2" ),
                                        score = "1.1111",
                                        similarity = Seq( NodeSimilarity( "a", 0.23423, 0 ), NodeSimilarity( "b", 0.0002034, 0 ) ),
                                        wordsString = "a" )

        val resultsRequest = JobResultsRequest( jobId = "job1" )
        val requestJson : String = format.toApiJson( resultsRequest ).get
        val unmarshalledRequest : JobResultsRequest = format.fromApiJson[ JobResultsRequest ]( requestJson, classOf[ JobResultsRequest ] ).get
        unmarshalledRequest shouldBe resultsRequest

        val submitResponse = ClusteringResultsResponse( clusteringResults = Seq( results ) )
        val responseJson : String = format.toApiJson( submitResponse ).get
        val unmarshalledResponse : ClusteringResultsResponse = format.fromApiJson[ ClusteringResultsResponse ]( responseJson, classOf[ ClusteringResultsResponse ] ).get
        unmarshalledResponse shouldBe submitResponse
    }

}
