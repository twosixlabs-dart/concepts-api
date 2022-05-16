package com.twosixtech.dart.concepts.controllers

import com.twosixlabs.dart.exceptions.ExceptionImplicits.FutureExceptionLogging
import com.twosixlabs.dart.exceptions.ResourceNotFoundException
import com.twosixlabs.dart.ontologies.api.{ OntologyArtifact, OntologyRegistry }
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import com.twosixlabs.dart.rest.scalatra.models.FailureResponse
import com.twosixtech.dart.concepts.clusters._
import com.twosixtech.dart.concepts.discovery.TenantDiscoveryService
import com.twosixtech.dart.concepts.models.{ ClusterResults, PollResponse, SimilarConcept, SingleResult }
import org.scalatra.{ Ok, UnprocessableEntity }

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class DiscoveryController( discoveryService : TenantDiscoveryService,
                           tenantOntologyService : OntologyRegistry,
                           clusteringService : RestClusteringApi,
                           pollDelay : Duration ) extends DartScalatraServlet {

    object DiscoveryJobs {
        sealed trait Status
        case object Pending extends Status
        case class Failed( reason : String ) extends Status
        case class Succeeded( results : ClusterRequest ) extends Status

        private val jobs : mutable.Map[ UUID, Status ] = mutable.Map()

        def getStatus( job : UUID ) : Option[ Status ] = jobs.get( job )
        def setStatus( job : UUID, status : Status ) : Unit =
            jobs( job ) = status

        def cancel( job : UUID ) : Unit =
            jobs -= job

        def submit( tenant : String, ontology : String )( implicit ex : ExecutionContext ) : UUID = {
            val newJob = UUID.randomUUID()
            jobs( newJob ) = Pending
            Future( discoveryService.discoverFor( tenant ) ) onComplete {
                case Success( res ) =>
                    jobs( newJob ) = Succeeded( ClusterRequest( res.concepts.map( _.phrase ), ontology, res.docIds.toSeq ) )
                case Failure( e ) =>
                    jobs( newJob ) = Failed( e.getMessage )
            }
            newJob
        }
    }

    object ClusterJob {
        sealed trait Status
        case object NoJob extends Status
        case object Clustering extends Status
        case object Retrieving extends Status
        case class Failed( reason : String ) extends Status
        case class Succeeded( results : Seq[ SingleResult ] ) extends Status

        private var status : Status = NoJob

        def getStatus() : Status = status
        def jobIsFinished() : Boolean = status match {
            case NoJob => true
            case Failed( _ ) => true
            case Succeeded( _ ) => true
            case _ => false
        }

        def setStatus( newStatus : Status ) : Unit = status = newStatus

        def cancel() : Unit = {
            status = NoJob
        }

        @tailrec
        def poll()( implicit ex : ExecutionContext ) : Unit = {
            if ( status != Retrieving ) ()
            else Try( clusteringService.clusterPoll() ) match {
                case Success( JobPollResponse( _, isReady, _, true ) ) if isReady =>
                    Try( clusteringService.clusterResults() ) match {
                        case Success( ClusteringResultsResponse( clusteringResults : Seq[ ClusteringResult ] ) ) =>
                            val results = clusteringResults.map( clusterRes => {
                                SingleResult(
                                    clusterRes.id,
                                    clusterRes.score.toDouble,
                                    clusterRes.clusterName,
                                    clusterRes.wordsString.split( "\\s+" ),
                                    clusterRes.similarity.map( similarity => SimilarConcept(
                                        similarity.label.trim.stripPrefix( "/" ).split( "/" ).map( _.trim ).toSeq,
                                        similarity.score,
                                        ) )
                                    )
                            } )
                            status = Succeeded( results )

                        case Failure( _ ) =>
                            Thread.sleep( pollDelay.toMillis )
                            poll()
                    }

                case Success( _ ) =>
                    Thread.sleep( pollDelay.toMillis )
                    poll()
            }
        }

        def submit( tenant : String, ontology : String )( implicit ex : ExecutionContext ) : Unit = {
            status = Clustering
            Future( discoveryService.discoverFor( tenant ) ) flatMap { clusterInputs =>
                val allowedWords = clusterInputs.concepts.map( _.phrase )
                val relevantDocs = clusterInputs.docIds.toSeq
                Future(
                    clusteringService.clusterSubmit(
                        ClusterRequest(
                            allowedWords = allowedWords,
                            ontologyMetadata = ontology,
                            relevantDocs = relevantDocs
                        )
                    )
                )
            } onComplete  {
                case Success( JobSubmissionResponse( _, _, true ) ) =>
                    status = Retrieving
                    poll()

                case Success( JobSubmissionResponse( _, message, false ) ) =>
                    status = Failed( message )

                case Failure( exception ) =>
                    status = Failed( exception.getMessage )
            }
        }
    }

    setStandardConfig()

    post( "/submit/:tenant" )( handleOutput {
        val tenant : String = params( "tenant" )

        tenantOntologyService.latest( tenant ) match {
            case Failure( e ) => throw e
            case Success( None ) =>
                throw new ResourceNotFoundException( s"no ontology found for tenant ${tenant} or tenant ${tenant} does not exist" )
            case Success( Some( OntologyArtifact( _, _, _, _, ontology, _, _ ) ) ) =>
                import scala.concurrent.ExecutionContext.Implicits.global
                val job = DiscoveryJobs.submit( tenant, ontology )
                Ok( job.toString )
        }
    } )

    get( "/poll/:jobId" )( handleOutput {
        val jobId = Try( UUID.fromString( params( "jobId" ) ) ) getOrElse {
            throw new ResourceNotFoundException( "Job ID", Some( params( "jobId" ) ) )
        }

        DiscoveryJobs.getStatus( jobId ) match {
            case None => throw new ResourceNotFoundException( "Job ID", Some( jobId.toString ) )
            case Some( DiscoveryJobs.Pending ) =>
                PollResponse( jobId.toString, false, "Pending" )
            case Some( DiscoveryJobs.Failed( reason ) ) =>
                PollResponse( jobId.toString, false, s"Failed: $reason" )
            case Some( DiscoveryJobs.Succeeded( _ ) ) =>
                PollResponse( jobId.toString, true, "Succeeded" )
        }
    } )

    get( "/results/:jobId" )( handleOutput {
        val jobId = Try( UUID.fromString( params( "jobId" ) ) ) getOrElse {
            throw new ResourceNotFoundException( "Job ID", Some( params( "jobId" ) ) )
        }

        DiscoveryJobs.getStatus( jobId ) match {
            case None => throw new ResourceNotFoundException( "Job ID", Some( jobId.toString ) )
            case Some( DiscoveryJobs.Succeeded( res ) ) =>
                Ok( res )
            case _ => UnprocessableEntity( FailureResponse( 422, s"Results are not ready for job ${jobId.toString}" ) )
        }
    } )

    post( "/cluster/submit/:tenant" )( handleOutput {
        val tenant : String = params( "tenant" )

        if ( ClusterJob.jobIsFinished() ) {
            tenantOntologyService.latest( tenant ) match {
                case Failure( e ) => throw e
                case Success( None ) =>
                    throw new ResourceNotFoundException( s"no ontology found for tenant ${tenant} or tenant ${tenant} does not exist" )
                case Success( Some( OntologyArtifact( _, _, _, _, ontology, _, _ ) ) ) =>
                    import scala.concurrent.ExecutionContext.Implicits.global
                    ClusterJob.submit( tenant, ontology )
                    Ok()
            }
        } else UnprocessableEntity( FailureResponse( 422, "Clustering job is currently in progress" ) )
    } )

    get( "/cluster/poll" )( handleOutput {
        ClusterJob.getStatus() match {
            case ClusterJob.NoJob =>
                clusteringService.clusterPoll() match {
                    case JobPollResponse( _, false, _, false ) =>
                        PollResponse( "N/A", false, "No job has been submitted" )
                    case JobPollResponse( _, true, msg, false ) =>
                        ClusterJob.setStatus( ClusterJob.Failed( msg ) )
                        PollResponse( "N/A", false, s"Job failed: $msg" )
                    case JobPollResponse( _, false, _, true ) =>
                        ClusterJob.setStatus( ClusterJob.Clustering )
                        PollResponse( "N/A", false, "Clustering" )
                    case JobPollResponse( _, true, _, true ) =>
                        ClusterJob.setStatus( ClusterJob.Retrieving )
                        PollResponse( "N/A", true, "Generating results" )
                }
            case ClusterJob.Clustering => PollResponse( "N/A", false, "Clustering" )
            case ClusterJob.Retrieving => PollResponse( "N/A", false, "Generating results" )
            case ClusterJob.Failed( reason ) => PollResponse( "N/A", false, s"Failed: $reason" )
            case ClusterJob.Succeeded( _ ) => PollResponse( "N/A", true, "Succeeded" )
        }
    } )

    get( "/cluster/results" )( handleOutput {
        ClusterJob.getStatus() match {
            case ClusterJob.Succeeded( results ) =>
                ClusterResults( "N/A", results )
            case _ =>
                throw new ResourceNotFoundException( "Cluster Results" )
        }
    } )

}
