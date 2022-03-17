package com.twosixtech.dart

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.twosixlabs.dart.arangodb.{Arango, ArangoConf}
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.sql.SqlClient
import com.typesafe.config.Config

import java.net.URI
import java.util.Properties
import scala.util.{Success, Try}


trait ConfigConstructor[ C, T ] {
    def buildFromConfig( config : C ) : T
}

object ConfigConstructors {

    /**
     * The idea here is that if you have a set of implicit ConfigConstructors in scope,
     * you can just call config.build[ Result ] where Result is the type being built
     */
    implicit class FromConfig[ C ]( config : C ) {
        def build[ T ]( implicit constructor : ConfigConstructor[ C, T ] ) : T = constructor.buildFromConfig( config )
    }

    import com.twosixlabs.dart.ontologies.dao.sql.PgSlickProfile.api._

    implicit object AuthFromConfig extends ConfigConstructor[ Config, AuthDependencies ] {
        override def buildFromConfig( config : Config ) : AuthDependencies = {
            SecureDartController.authDeps( config )
        }
    }

    implicit object DatabaseFromConfig extends ConfigConstructor[ Config, Database ] {
        override def buildFromConfig( config : Config ) : Database = {
            val ds = new ComboPooledDataSource()
            ds.setDriverClass( config.getString( "postgres.driver.class" ) )
            val pgHost = config.getString( "postgres.host" )
            val pgPort = config.getInt( "postgres.port" )
            val pgDb = config.getString( "postgres.database" )
            ds.setJdbcUrl( s"jdbc:postgresql://$pgHost:$pgPort/$pgDb" )
            ds.setUser( config.getString( "postgres.user" ) )
            ds.setPassword( config.getString( "postgres.password" ) )
            Try( config.getInt( "postgres.minPoolSize" )  ).foreach( v => ds.setMinPoolSize( v ) )
            Try( config.getInt( "postgres.acquireIncrement" )  ).foreach( v => ds.setAcquireIncrement( v ) )
            Try( config.getInt( "postgres.maxPoolSize" )  ).foreach( v => ds.setMaxPoolSize( v ) )

            val maxConns = Try( config.getInt( "postgres.max.connections" ) ).toOption

            Database.forDataSource( ds, maxConns )
        }
    }

    implicit object ArangoFromConfig extends ConfigConstructor[ Config, Arango ] {
        override def buildFromConfig( config : Config ) : Arango = {
            new Arango( ArangoConf(
                host = config.getString( "arangodb.host" ) ,
                port = config.getInt( "arangodb.port" ),
                database = config.getString( "arangodb.database" )
                ) )
        }
    }

}
