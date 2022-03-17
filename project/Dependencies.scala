import sbt._

object Dependencies {

    val typesafeConfigVersion = "1.4.1"
    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.3"
    val scalaTestVersion = "3.1.4"
    val betterFilesVersion = "3.8.0"
    val okhttpVersion = "4.1.0"
    val jacksonVersion = "2.10.5"
    val java8CompatVersion = "1.0.0"

    val clulabVersion = "8.2.3"
    val conceptDiscoveryVersion = "172aef7f2aaa80bb14136409f0fa96e612eb1f18"

    val dartAuthVersion = "3.1.144"

    val cdr4sVersion = "3.0.256"

    val arangoDatastoreVersion = "3.0.35"

    val dartCommonsVersion = "3.0.278"
    val dartRestVersion = "3.0.15"

    val ontologyRegistryVersion = "3.0.20"

    val scalatraVersion = "2.7.0"
    val jettyWebappVersion = "9.4.18.v20190429"
    val servletApiVersion = "3.1.0"

    val scalaTest = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % "test" )

    val conceptDiscovery = Seq( "com.github.clulab" % "conceptdiscovery" % "71d306a" )

    val typesafeConfig = Seq( "com.typesafe" % "config" % typesafeConfigVersion )

    val arangoDatastoreRepo = Seq( "com.twosixlabs.dart" %% "dart-arangodb-datastore" % arangoDatastoreVersion )

    val arrangoTenants = Seq( "com.twosixlabs.dart.auth" %% "arrango-tenants" % dartAuthVersion )

    val dartAuthCore = Seq( "com.twosixlabs.dart.auth" %% "core" % dartAuthVersion )

    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-core" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-ladle-json" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-utils" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-json" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )

    val ontologyRegistry = Seq( "com.twosixlabs.dart.ontologies" %% "ontology-registry-services" % ontologyRegistryVersion )


    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % Test,
                        "org.eclipse.jetty" % "jetty-webapp" % jettyWebappVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion )

    val okhttp = Seq( "com.squareup.okhttp3" % "okhttp" % okhttpVersion,
                      "com.squareup.okhttp3" % "mockwebserver" % okhttpVersion % Test )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )


    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )


    val java8Compat = Seq( "org.scala-lang.modules" %% "scala-java8-compat" % java8CompatVersion )

    val dartRest = Seq( "com.twosixlabs.dart.rest" %% "dart-scalatra-commons" % dartRestVersion )

    val jackson = Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                       "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion )
}
