package com.twosixtech.dart.concepts.clusters

import okhttp3._
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit.SECONDS
import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object RestClusteringApi {
    val API_ROOT : String = "api/v1/concept_discovery/"
    val OFFLINE_PATH : String = "offline_processing/"
    val CLUSTERING_PATH : String = "clustering/"
    val RESCORING_PATH : String = "rescoring/"
    val SUBMIT_OP : String = "submit"
    val POLL_OP : String = "job"
    val RESULTS_OP : String = "result"
    val INITIAL_CLUSTER_ID : String = "00000000-0000-0000-0000-000000000000"
}

class RestClusteringApi( host : String, port : Int ) extends ClusteringApi {
    import RestClusteringApi._

    private var clusterJob : Option[ String ] = None

    private val JSON_MEDIA_TYPE : MediaType = MediaType.get( "application/json; charset=utf-8" )
    private val JSON_FORMAT : ClusteringApiJsonFormat = new ClusteringApiJsonFormat
    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    private val baseUrl : String = s"${host}:${port}/${API_ROOT}"

    private val client : OkHttpClient = {
        val timeout : java.time.Duration = Duration( 2, SECONDS ).toJava
        val connectionPool = new ConnectionPool( 100, 2, SECONDS )
        new OkHttpClient.Builder()
          .connectionPool( connectionPool )
          .callTimeout( timeout )
          .connectTimeout( timeout )
          .readTimeout( timeout )
          .build()
    }

    private val format : ClusteringApiJsonFormat = new ClusteringApiJsonFormat

    override def clusterSubmit(
        request: ClusterRequest
    ): JobSubmissionResponse = {
        val url = s"${baseUrl}${OFFLINE_PATH}${SUBMIT_OP}"
        val requestJson = format.toApiJson( request ).get
        val body = RequestBody.create( requestJson, JSON_MEDIA_TYPE )
        val httpRequest : Request = new Request.Builder().url( url ).post( body ).build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobSubmissionResponse ]( body, classOf[ JobSubmissionResponse ] ).get
                if ( responseCode < 300 ) {
                    clusterJob = Some( response.jobId )
                    response
                } else {
                    clusterJob = None
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) =>
                JobSubmissionResponse( jobId = null, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def clusterPoll( ): JobPollResponse = {
        val jobId = clusterJob.getOrElse( throw new NoSuchElementException( "No cluster job exists" ) )
        val url = s"${baseUrl}${OFFLINE_PATH}${POLL_OP}?job_id=${jobId}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobPollResponse ]( body, classOf[ JobPollResponse ] ).get
                if ( responseCode < 300 ) response
                else {
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) =>
                JobPollResponse( jobId = jobId, isReady = false, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def clusterResults( ): ClusteringResultsResponse = {
        val url = s"${baseUrl}${CLUSTERING_PATH}${RESULTS_OP}?job_id=${INITIAL_CLUSTER_ID}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val body = httpResponse.body.string()
                JSON_FORMAT.fromApiJson[ ClusteringResultsResponse ]( body, classOf[ ClusteringResultsResponse ] ).get
            }
            case Failure( e : Throwable ) => ClusteringResultsResponse( clusteringResults = Seq() )
        }
    }


    override def reclusterSubmit( request : ReclusterRequest ) : JobSubmissionResponse = {
        val url = s"${baseUrl}${CLUSTERING_PATH}${SUBMIT_OP}"
        val requestJson = format.toApiJson( request ).get
        val body = RequestBody.create( requestJson, JSON_MEDIA_TYPE )
        val httpRequest : Request = new Request.Builder().url( url ).post( body ).build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobSubmissionResponse ]( body, classOf[ JobSubmissionResponse ] ).get
                if ( responseCode < 300 ) response
                else {
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) => JobSubmissionResponse( jobId = null, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def reclusterPoll( request : JobPollRequest ) : JobPollResponse = {
        val url = s"${baseUrl}${CLUSTERING_PATH}${POLL_OP}?job_id=${request.jobId}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobPollResponse ]( body, classOf[ JobPollResponse ] ).get
                if ( responseCode < 300 ) response
                else {
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) => JobPollResponse( jobId = request.jobId, isReady = false, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def reclusterResults( request : JobResultsRequest ) : ClusteringResultsResponse = {
        val url = s"${baseUrl}${CLUSTERING_PATH}${RESULTS_OP}?job_id=${request.jobId}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val body = httpResponse.body.string()
                JSON_FORMAT.fromApiJson[ ClusteringResultsResponse ]( body, classOf[ ClusteringResultsResponse ] ).get
            }
            case Failure( e : Throwable ) => ClusteringResultsResponse( clusteringResults = Seq() )
        }
    }

    override def rescoreSubmit( request : RescoreRequest ) : JobSubmissionResponse = {
        val url = s"${baseUrl}${RESCORING_PATH}${SUBMIT_OP}?cluster_job_id=${request.clusterJobId}"
        val requestJson = format.toApiJson( request ).get
        val body = RequestBody.create( requestJson, JSON_MEDIA_TYPE )
        val httpRequest : Request = new Request.Builder().url( url ).post( body ).build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobSubmissionResponse ]( body, classOf[ JobSubmissionResponse ] ).get
                if ( responseCode < 300 ) response
                else {
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) => JobSubmissionResponse( jobId = null, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def rescorePoll( request : JobPollRequest ) : JobPollResponse = {
        val url = s"${baseUrl}${RESCORING_PATH}${POLL_OP}?job_id=${request.jobId}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JSON_FORMAT.fromApiJson[ JobPollResponse ]( body, classOf[ JobPollResponse ] ).get
                if ( responseCode < 300 ) response
                else {
                    LOG.error( s"received unexpected response code from service ${responseCode} - ${body}" )
                    response.copy( success = false, message = s"unexpected response code: ${responseCode}" )
                }
            }
            case Failure( e : Throwable ) => JobPollResponse( jobId = request.jobId, isReady = false, message = s"client error: ${e.getClass.getCanonicalName}", success = false )
        }
    }

    override def rescoreResults( request : JobResultsRequest ) : RescoringResultsResponse = {
        val url = s"${baseUrl}${RESCORING_PATH}${RESULTS_OP}?job_id=${request.jobId}"
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) match {
            case Success( httpResponse ) => {
                val body = httpResponse.body.string()
                JSON_FORMAT.fromApiJson[ RescoringResultsResponse ]( body, classOf[ RescoringResultsResponse ] ).get
            }
            case Failure( e : Throwable ) => RescoringResultsResponse( rescoringResults = Seq() )
        }
    }

    private def executeHttp( request : Request ) : Try[ Response ] = {
        Try( client.newCall( request ).execute() )
    }

}
