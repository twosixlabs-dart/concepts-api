package com.twosixtech.dart.concepts.models

import com.fasterxml.jackson.annotation.JsonProperty

import scala.beans.BeanProperty

case class DartReclusterRequest(
  @BeanProperty @JsonProperty( "phrases" ) phrases : Seq[ String ],
  @BeanProperty @JsonProperty( "ontology" ) ontology : String,
  @BeanProperty @JsonProperty( "cluster_job_id" ) clusterJobId : String,
)

case class DartRescoreRequest(
  @BeanProperty @JsonProperty( "ontology" ) ontology : String,
  @BeanProperty @JsonProperty( "cluster_job_id" ) clusterJobId : String,
)

case class PollResponse(
    @BeanProperty @JsonProperty( "job_id" ) jobId : String,
    @BeanProperty @JsonProperty( "complete" ) complete : Boolean,
    @BeanProperty @JsonProperty( "message" ) message : String,
)

case class ClusterResults(
    @BeanProperty @JsonProperty( "job_id" ) jobId : String,
    @BeanProperty @JsonProperty( "clusters" ) clusters : Seq[ SingleResult ],
)

case class SimilarConcept(
    @BeanProperty @JsonProperty( "concept" ) concept : Seq[ String ],
    @BeanProperty @JsonProperty( "score" ) score : Double,
)

case class SingleResult(
    @BeanProperty @JsonProperty( "cluster_id" ) clusterId : String,
    @BeanProperty @JsonProperty( "score" ) score : Double,
    @BeanProperty @JsonProperty( "recommended_name" ) recommendedName : String,
    @BeanProperty @JsonProperty( "phrases" ) phrases : Seq[ String ],
    @BeanProperty @JsonProperty( "similar_concepts" ) similarConcepts : Seq[ SimilarConcept ],
)
