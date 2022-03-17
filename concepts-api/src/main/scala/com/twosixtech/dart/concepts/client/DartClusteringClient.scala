package com.twosixtech.dart.concepts.client

import com.twosixtech.dart.concepts.clusters.ClusterRequest
import com.twosixtech.dart.concepts.models.SingleResult

import scala.concurrent.Future

trait DartClusteringClient {
    import DartClusteringClient._

    def discovery( tenant : String ) : Future[ Job ]

    def pollDiscovery( job : Job ) : Future[ JobStatus ]

    def discoveryResults( job : Job ) : Future[ ClusterRequest ]

    def initialClustering( tenant : String ) : Future[ Unit ]

    def pollInitialClustering() : Future[ JobStatus ]

    def initialClusteringResults() : Future[ Seq[ SingleResult ] ]

    def recluster( concepts : Seq[ String ], ontology : String, prevJob : Job ) : Future[ Job ]

    def pollRecluster( job : Job ) : Future[ JobStatus ]
    def pollRecluster( jobId : String ) : Future[ JobStatus ] = pollRecluster( Job( jobId ) )

    def reclusterResults( job : Job ) : Future[ Seq[ SingleResult ] ]
    def reclusterResults( jobId : String ) : Future[ Seq[ SingleResult ] ] = reclusterResults( Job( jobId ) )

    def rescore( ontology : String, clusterJob : Job ) : Future[ Job ]
    def rescore( ontology : String, clusterJobId : String ) : Future[ Job ] = rescore( ontology, Job( clusterJobId ) )

    def pollRescore( job : Job ) : Future[ JobStatus ]
    def pollRescore( jobId : String ) : Future[ JobStatus ] = pollRescore( Job( jobId ) )

    def rescoreResults( job : Job ) : Future[ Seq[ SingleResult ] ]
    def rescoreResults( jobId : String ) : Future[ Seq[ SingleResult ] ] = rescoreResults( Job( jobId ) )

}

object DartClusteringClient {
    case class Job( id : String )

    sealed trait JobStatus
    case object Pending extends JobStatus
    case class Failed( message : String ) extends JobStatus
    case object Succeeded extends JobStatus
}
