package com.twosixtech.dart.cdr

import com.twosixlabs.cdr4s.annotations.OffsetTag
import com.twosixlabs.cdr4s.core.{CdrDocument, CdrMetadata, OffsetTagAnnotation}
import org.clulab.concepts.ScoredSentence
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, OffsetDateTime}

class CdrConversionTest extends AnyFlatSpecLike with Matchers {

    val testCdr = CdrDocument(
        captureSource = "test-capture-source",
        extractedMetadata = CdrMetadata(
            creationDate = LocalDate.now(),
            author = "test-author",
            docType = "article",
            description = "test-description",
            originalLanguage = "en",
            classification = "UNCLASSIFIED",
            title = "Test Title",
            publisher = "test-publisher",
            pages = Some( 20 ),
        ),
        contentType = "application/json",
        extractedNumeric = Map.empty,
        documentId = "abcdef0123456789abcdef0123456789",
        extractedText = "test extracted text",
        uri = "test-uri",
        sourceUri = "test-source-uri",
        extractedNtriples = "",
        timestamp = OffsetDateTime.now(),
        annotations = List(),
        labels = Set( "test-label-1", "test-label-2" ),
    )

    behavior of "CdrConversion.cdrToDiscoveryDocument"

    it should "generate a DiscoveryDocument with no sentences if key sentence annotation is missing" in {
        import com.twosixtech.dart.concepts.cdr.CdrConversion._

        val dd = testCdr.discoveryDocument

        dd.docid shouldBe testCdr.documentId
        dd.sentences shouldBe Seq()
    }

    it should "generate a DiscoveryDocument with sentences if key sentence annotation is present" in {
        import com.twosixtech.dart.concepts.cdr.CdrConversion._

        val cdrWithSentences = testCdr.copy(
            annotations = List(
                OffsetTagAnnotation(
                    "qntfy-key-sentence-annotator",
                    "0.1.5",
                    List(
                        OffsetTag( 0, 5, Some( "hello" ), "KEY_SENTENCE", Some( 0.5 ) ),
                        OffsetTag( 5, 10, Some( "mistr" ), "KEY_SENTENCE", Some( 0.6 ) ),
                        OffsetTag( 10, 15, Some( "fansy" ), "KEY_SENTENCE", Some( 0.7434534534534534564553 ) ),
                        OffsetTag( 15, 20, Some( "sings" ), "KEY_SENTENCE", Some( 0.8 ) ),
                        OffsetTag( 20, 25, Some( "songs" ), "KEY_SENTENCE", Some( 0.9 ) ),
                    )
                )
            )
        )

        val dd = cdrWithSentences.discoveryDocument

        dd.docid shouldBe cdrWithSentences.documentId
        dd.sentences.length shouldBe 5
        dd.sentences should contain( ScoredSentence( "hello", 0, 5, 0.5 ) )
        dd.sentences should contain( ScoredSentence( "fansy", 10, 15, 0.7434534534534534564553 ) )
    }
}
