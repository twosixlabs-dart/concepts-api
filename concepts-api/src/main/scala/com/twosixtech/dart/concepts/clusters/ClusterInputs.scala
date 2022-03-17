package com.twosixtech.dart.concepts.clusters

case class ClusterableConcept( phrase : String, rank : Double )

case class ClusterInputs( docIds : Set[ String ], concepts : Seq[ ClusterableConcept ] )
