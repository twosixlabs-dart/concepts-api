package com.twosixtech.dart.concepts.cdr

import com.twosixlabs.cdr4s.core.CdrDocument

trait TenantCdrProvider {
    def tenantDocs( tenant: String ): Seq[ CdrDocument ]
}
