package com.twosixtech.dart.concepts.clusters

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.twosixlabs.dart.json.JsonFormat
import org.slf4j.{Logger, LoggerFactory}

import scala.beans.BeanProperty
import scala.reflect.ClassTag
import scala.util.Try

sealed trait ClusteringApiModel

case class ClusterRequest( @BeanProperty @JsonProperty( "allowed_words" ) allowedWords : Seq[ String ],
                           @BeanProperty @JsonProperty( "ontology_metadata" ) ontologyMetadata : String,
                           @BeanProperty @JsonProperty( "relevant_doc_uuids" ) relevantDocs : Seq[ String ] ) extends ClusteringApiModel

case class ReclusterRequest( @BeanProperty @JsonProperty( "allowed_words" ) allowedWords : Seq[ String ] = Seq(),
                             @BeanProperty @JsonProperty( "ontology_metadata" ) ontologyMetadata : String,
                             @BeanProperty @JsonProperty( "cluster_job_id" ) clusterJobId : String ) extends ClusteringApiModel

case class RescoreRequest( @BeanProperty @JsonProperty( "ontology_metadata" ) ontologyMetadata : String,
                           @BeanProperty @JsonProperty( "cluster_job_id" ) clusterJobId : String ) extends ClusteringApiModel

@JsonIgnoreProperties( ignoreUnknown = true )
case class JobSubmissionResponse( @BeanProperty @JsonProperty( "job_id" ) jobId : String,
                                  @BeanProperty @JsonProperty( "message" ) message : String,
                                  @BeanProperty @JsonProperty( "success" ) success : Boolean ) extends ClusteringApiModel

case class JobPollRequest( @BeanProperty @JsonProperty( "job_id" ) jobId : String ) extends ClusteringApiModel

@JsonIgnoreProperties( ignoreUnknown = true )
case class JobPollResponse( @BeanProperty @JsonProperty( "job_id" ) jobId : String,
                            @BeanProperty @JsonProperty( "is_ready" ) isReady : Boolean,
                            @BeanProperty @JsonProperty( "message" ) message : String,
                            @BeanProperty @JsonProperty( "success" ) success : Boolean ) extends ClusteringApiModel

case class JobResultsRequest( @BeanProperty @JsonProperty( "job_id" ) jobId : String ) extends ClusteringApiModel

@JsonIgnoreProperties( ignoreUnknown = true )
case class ClusteringResultsResponse( @BeanProperty @JsonProperty( "clustering_result" ) clusteringResults : Seq[ ClusteringResult ] = Seq() ) extends ClusteringApiModel

@JsonIgnoreProperties( ignoreUnknown = true )
case class RescoringResultsResponse( @BeanProperty @JsonProperty( "rescoring_result" ) rescoringResults : Seq[ ClusteringResult ] = Seq() ) extends ClusteringApiModel

case class NodeSimilarity(
    @BeanProperty @JsonProperty( "label" ) label : String,
    @BeanProperty @JsonProperty( "score" ) score : Double,
    @BeanProperty @JsonProperty( "correctness" ) correctness : Double,
)

@JsonIgnoreProperties( ignoreUnknown = true )
case class ClusteringResult( @BeanProperty @JsonProperty( "cluster_name" ) clusterName : String,
                             @BeanProperty @JsonProperty( "comments" ) comments : Seq[ String ],
                             @BeanProperty @JsonProperty( "id" ) id : String,
                             @BeanProperty @JsonProperty( "novel_ontology_nodes" ) novelNodes : Seq[ String ],
                             @BeanProperty @JsonProperty( "overlap_ontology_nodes" ) overlapNodes : Seq[ String ],
                             @BeanProperty @JsonProperty( "score" ) score : String,
                             @BeanProperty @JsonProperty( "similarity_to_ontology_nodes" ) similarity : Seq[ NodeSimilarity ],
                             @BeanProperty @JsonProperty( "words_string" ) wordsString : String )


class ClusteringApiJsonFormat extends JsonFormat {

    import com.twosixlabs.dart.json.JsonFormat._

    private lazy val LOG : Logger = LoggerFactory.getLogger( getClass )

    def toApiJson[ T <: ClusteringApiModel ]( apiObj : T ) : Try[ String ] = {
        marshalFrom( apiObj )
    }

    def fromApiJson[ T: ClassTag ]( apiJson : String, clazz : Class[ T ] ) : Try[ T ] = {
        unmarshalTo[ T ]( apiJson, clazz )
    }


}