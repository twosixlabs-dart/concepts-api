package com.twosixtech.dart.concepts.cdr

import com.twosixlabs.cdr4s.core.CdrDocument
import com.twosixlabs.dart.arangodb.tables.CanonicalDocsTable
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class ArangoTenantCdrProvider(
    index: CorpusTenantIndex,
    docsTable: CanonicalDocsTable,
)(
    implicit
    ex: ExecutionContext,
) extends TenantCdrProvider {

    override def tenantDocs(
        tenant: String
    ): Seq[ CdrDocument ] = {
        val futureDocs = index.tenantDocuments(tenant) flatMap  { docIds: Seq[ index.DocId ] =>
            Future.sequence( docIds map { docId =>
                docsTable.getDocument( docId ).map( _.get ) transform {
                    case Success( value ) => Success( value )
                    case Failure( _ : NoSuchElementException ) =>
                        Failure( new NoSuchElementException( s"$docId is indexed in tenant $tenant but was not found in the documents table" ) )
                    case Failure( e ) => Failure( e )
                }
            } )
        }

        Await.result( futureDocs, Duration.Inf )
    }
}
