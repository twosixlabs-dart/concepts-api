package com.twosixtech.dart.concepts.clusters

trait ClusteringApi {

    def clusterSubmit( request: ClusterRequest ): JobSubmissionResponse

    def clusterPoll( ): JobPollResponse

    def clusterResults( ): ClusteringResultsResponse

    def reclusterSubmit( request: ReclusterRequest ): JobSubmissionResponse

    def reclusterPoll( request: JobPollRequest ): JobPollResponse

    def reclusterResults( request: JobResultsRequest ): ClusteringResultsResponse

    def rescoreSubmit( request: RescoreRequest ): JobSubmissionResponse

    def rescorePoll( request: JobPollRequest ): JobPollResponse

    def rescoreResults( request: JobResultsRequest ): RescoringResultsResponse

}
