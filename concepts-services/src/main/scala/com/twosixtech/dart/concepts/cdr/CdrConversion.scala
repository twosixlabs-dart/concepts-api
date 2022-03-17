package com.twosixtech.dart.concepts.cdr

import com.twosixlabs.cdr4s.annotations.OffsetTag
import com.twosixlabs.cdr4s.core.{CdrDocument, OffsetTagAnnotation}
import org.clulab.concepts.{DiscoveryDocument, ScoredSentence}

object CdrConversion {

    def cdrToDiscoveryDocument( cdr: CdrDocument ): DiscoveryDocument = {
        val docId: String = cdr.documentId
        val scoredSentences: Seq[ ScoredSentence ] = {
            cdr
              .annotations
              .collect(
                  {
                      case OffsetTagAnnotation( "qntfy-key-sentence-annotator", _, content, _ ) =>
                          content collect {
                              case OffsetTag( start, end, Some( value ), _, Some( score ) ) =>
                                  ScoredSentence( value, start, end, score.toDouble )
                          }
                  }
              ).flatten
        }
        DiscoveryDocument( docId, scoredSentences )
    }

    implicit class ConvertibleDartCdr( dartCdr: CdrDocument ) {
        def discoveryDocument: DiscoveryDocument = cdrToDiscoveryDocument( dartCdr: CdrDocument )
    }

}
