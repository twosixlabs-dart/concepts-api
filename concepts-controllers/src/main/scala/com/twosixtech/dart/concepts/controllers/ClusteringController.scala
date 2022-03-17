package com.twosixtech.dart.concepts.controllers

import com.twosixlabs.dart.exceptions.{BadRequestBodyException, ResourceNotFoundException}
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import com.twosixtech.dart.concepts.clusters._
import com.twosixtech.dart.concepts.models
import com.twosixtech.dart.concepts.models._

import scala.util.{Failure, Success, Try}

class ClusteringController( clusteringApi : ClusteringApi ) extends DartScalatraServlet {

    get( "/recluster/poll/:jobId" )( handleOutput {
        val jobId = params( "jobId" )

        val pollRes = clusteringApi.reclusterPoll( JobPollRequest( jobId ) )

        PollResponse( pollRes.jobId, pollRes.isReady, pollRes.message )
    } )

    get( "/rescore/poll/:jobId" )( handleOutput {
        val jobId = params( "jobId" )

        val pollRes = clusteringApi.rescorePoll( JobPollRequest( jobId ) )

        PollResponse( pollRes.jobId, pollRes.isReady, pollRes.message )
    } )

    get( "/recluster/results/:jobId" )( handleOutput {
        val jobId = params( "jobId" )

        val resRes = Try( clusteringApi.reclusterResults( JobResultsRequest( jobId ) ) ) match {
            case Success( res ) => res
            case Failure( e : IllegalArgumentException ) =>
                if ( e.getMessage.contains( s"${jobId} does not exist or is not ready" ) )
                    throw new ResourceNotFoundException( "job id", Some( jobId ) )
                else throw e
            case Failure( e ) => throw e
        }

        ClusterResults(
            jobId,
            resRes.clusteringResults.map( clusterRes => {
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
        )
    } )

    get( "/rescore/results/:jobId" )( handleOutput {
        val jobId = params( "jobId" )

        val resRes = Try( clusteringApi.rescoreResults( JobResultsRequest( jobId ) ) ) match {
            case Success( res ) =>
                LOG.info( "results", res )
                res
            case Failure( e : IllegalArgumentException ) =>
                if ( e.getMessage.contains( s"${jobId} does not exist or is not ready" ) )
                    throw new ResourceNotFoundException( "job id", Some( jobId ) )
                else throw e
            case Failure( e ) => throw e
        }

        models.ClusterResults(
            jobId,
            resRes.rescoringResults.map( clusterRes => {
                SingleResult( clusterRes.id,
                              clusterRes.score.toDouble,
                              clusterRes.clusterName,
                              clusterRes.wordsString.split( "\\s+" ),
                              clusterRes.similarity.map( similarity => {
                                  SimilarConcept( similarity.label.trim.stripPrefix( "/" ).split( "/" ).map( _.trim ).toSeq,
                                                  similarity.score )
                              } ) )
            } ) )
    } )

    post( "/recluster/submit" )( handleOutput {
        val body = request.body

        val dartRequest : DartReclusterRequest = unmarshal( body, classOf[ DartReclusterRequest ] )
          .getOrElse( throw new BadRequestBodyException( "Could not parse request" ) )

        val res : JobSubmissionResponse =
            clusteringApi
              .reclusterSubmit( ReclusterRequest( allowedWords = dartRequest.phrases,
                                                  ontologyMetadata = dartRequest.ontology,
                                                  clusterJobId = dartRequest.clusterJobId ) )

        PollResponse( res.jobId,
                      complete = false,
                      res.message )
    } )

    post( "/rescore/submit" )( handleOutput {
        val body = request.body

        val dartRequest : DartRescoreRequest = unmarshal( body, classOf[ DartRescoreRequest ] )
          .getOrElse( throw new BadRequestBodyException( "Could not parse request" ) )

        val res : JobSubmissionResponse =
            clusteringApi
              .rescoreSubmit( RescoreRequest( ontologyMetadata = dartRequest.ontology,
                                              clusterJobId = dartRequest.clusterJobId ) )

        PollResponse( res.jobId,
                      complete = false,
                      res.message )
    } )

}
