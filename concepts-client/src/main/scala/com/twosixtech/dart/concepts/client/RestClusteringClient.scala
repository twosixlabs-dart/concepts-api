package com.twosixtech.dart.concepts.client

import com.twosixlabs.dart.json.JsonFormat
import com.twosixtech.dart.concepts.clusters.ClusterRequest
import com.twosixtech.dart.concepts.models._
import okhttp3._

import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.ExecutionContext.Implicits.global
import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object RestClusteringClient {
    val DISCOVERY_SEGMENT = "/discovery"
    val CLUSTER_SEGMENT = "/discovery/cluster"
    val RECLUSTER_SEGMENT = "/cluster/recluster"
    val RESCORE_SEGMENT = "/cluster/rescore"
    val SUBMIT_SEGMENT = "/submit"
    val POLL_SEGMENT = "/poll"
    val RESULTS_SEGMENT = "/results"
}

class RestClusteringClient( host : String, port : Int, apiPath : String, scheme : String = "http" )
  extends DartClusteringClient {

    import DartClusteringClient.Job

    private val formattedApiPath : String = "/" + apiPath.trim.stripSuffix( "/" ).stripPrefix( "/" )
    private val basePath = s"${scheme.trim}://${host.trim}:$port$formattedApiPath"

    def path( endpoint : String ) : String = basePath + endpoint

    import RestClusteringClient._

    private val JSON_MEDIA_TYPE : MediaType = MediaType.get( "application/json; charset=utf-8" )

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

    private def executeHttp( request : Request ) : Future[ Response ] = {
        Future( client.newCall( request ).execute() )
    }

    override def discovery(
        tenant: String
    ): Future[ DartClusteringClient.Job ] = {
        val url = path( s"$DISCOVERY_SEGMENT$SUBMIT_SEGMENT/${tenant.trim}" )
        val requestBody = RequestBody.create( Array.emptyByteArray )
        val httpRequest : Request = new Request.Builder().url( url ).post( requestBody ).build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    Try( UUID.fromString( body ) )
                      .map( id => Job( id.toString ) )
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def pollDiscovery(
        job: DartClusteringClient.Job
    ): Future[ DartClusteringClient.JobStatus ] = {
        val url = path( s"$DISCOVERY_SEGMENT$POLL_SEGMENT/${job.id}" )
        val httpRequest : Request =
            new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ) map {
                        case PollResponse( _, true, _ ) => DartClusteringClient.Succeeded
                        case PollResponse( _, false, _ ) => DartClusteringClient.Pending
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def discoveryResults(
        job: DartClusteringClient.Job
    ): Future[ ClusterRequest ] = {
        val url = path( s"$DISCOVERY_SEGMENT$RESULTS_SEGMENT/${job.id}" )
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response =
                    JsonFormat.unmarshalTo[ ClusterRequest ]( body, classOf[ ClusterRequest ] )
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    response
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def initialClustering( tenant: String ): Future[ Unit ] = {
        val url = path( s"$CLUSTER_SEGMENT$SUBMIT_SEGMENT/$tenant" )
        val httpRequest : Request = new Request.Builder().url( url ).post( RequestBody.create( Array.emptyByteArray ) ).build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    Success()
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def pollInitialClustering( ): Future[ DartClusteringClient.JobStatus ] = {
        val url = path( s"$CLUSTER_SEGMENT$POLL_SEGMENT" )
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JsonFormat.unmarshalTo[ PollResponse ]( body, classOf[ PollResponse ] )
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    response map {
                        case PollResponse( _, true, _ ) => DartClusteringClient.Succeeded
                        case PollResponse( _, false, msg ) => DartClusteringClient.Failed( msg )
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def initialClusteringResults( ): Future[ Seq[ SingleResult ] ] = {
        val url = path( s"$CLUSTER_SEGMENT$RESULTS_SEGMENT" )
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response = JsonFormat.unmarshalTo[ ClusterResults ]( body, classOf[ ClusterResults ] )
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    response map { res =>
                        res.clusters
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def recluster(
        concepts: Seq[ String ], ontology: String, prevJob : Job
    ): Future[ DartClusteringClient.Job ] = {
        val url = path( s"$RECLUSTER_SEGMENT$SUBMIT_SEGMENT" )
        val json = JsonFormat.marshalFrom( DartReclusterRequest( concepts, ontology, prevJob.id )  ).get
        val requestBody = RequestBody
          .create( json, JSON_MEDIA_TYPE )
        val httpRequest : Request = new Request.Builder().url( url ).post( requestBody ).build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ) map {
                        case PollResponse( jobId, _, _ ) => DartClusteringClient.Job( jobId )
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def pollRecluster(
        job: DartClusteringClient.Job
    ): Future[ DartClusteringClient.JobStatus ] = {
        val url = path( s"$RECLUSTER_SEGMENT$POLL_SEGMENT/${job.id}" )
        val httpRequest : Request =
            new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ) map {
                        case PollResponse( _, true, _ ) => DartClusteringClient.Succeeded
                        case PollResponse( _, false, _ ) => DartClusteringClient.Pending
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def reclusterResults(
        job: DartClusteringClient.Job
    ): Future[ Seq[ SingleResult ] ] = {
        val url = path( s"$RECLUSTER_SEGMENT$RESULTS_SEGMENT/${job.id}" )
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response =
                    JsonFormat.unmarshalTo[ ClusterResults ]( body, classOf[ ClusterResults ] )
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    response map { res =>
                        res.clusters
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def rescore(
        ontology: String, clusterJob: DartClusteringClient.Job
    ): Future[ DartClusteringClient.Job ] = {
        val url = path( s"$RESCORE_SEGMENT$SUBMIT_SEGMENT" )
        val json = JsonFormat.marshalFrom( DartRescoreRequest( ontology, clusterJob.id ) ).get
        val requestBody = RequestBody
          .create( json, JSON_MEDIA_TYPE )
        val httpRequest : Request = new Request.Builder().url( url ).post( requestBody ).build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ) map {
                        case PollResponse( jobId, _, _ ) => DartClusteringClient.Job( jobId )
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def pollRescore(
        job: DartClusteringClient.Job
    ): Future[ DartClusteringClient.JobStatus ] = {
        val url = path( s"$RESCORE_SEGMENT$POLL_SEGMENT/${job.id}" )
        val httpRequest : Request =
            new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    JsonFormat.unmarshalTo( body, classOf[ PollResponse ] ) map {
                        case PollResponse( _, true, _ ) => DartClusteringClient.Succeeded
                        case PollResponse( _, false, _ ) => DartClusteringClient.Pending
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

    override def rescoreResults(
        job: DartClusteringClient.Job
    ): Future[ Seq[ SingleResult ] ] = {
        val url = path( s"$RESCORE_SEGMENT$RESULTS_SEGMENT/${job.id}" )
        val httpRequest : Request = new Request.Builder().url( url ).get().build()
        executeHttp( httpRequest ) transform  {
            case Success( httpResponse ) => {
                val responseCode = httpResponse.code()
                val body = httpResponse.body.string()
                val response =
                    JsonFormat.unmarshalTo[ ClusterResults ]( body, classOf[ ClusterResults ] )
                if ( responseCode != 200 ) {
                    Failure( new Exception( s"$responseCode: $body" ) )
                } else {
                    response map { res =>
                        res.clusters
                    }
                }
            }
            case Failure( e : Throwable ) => Failure( e )
        }
    }

}
