import Dependencies._
import sbt.Keys.mainClass
import sbt._

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend ( Test )
lazy val WipConfig = config( "wip" ) extend ( Test )

lazy val commonSettings = {
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        organization := "com.twosixlabs.dart.concepts",
        scalaVersion := "2.12.7",
        resolvers ++= Seq( "Maven Central" at "https://repo1.maven.org/maven2/",
            "JCenter" at "https://jcenter.bintray.com",
            ( "Clulab Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" ).withAllowInsecureProtocol( true ),
            "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default",
            "jitpack" at "https://jitpack.io"
        ),
        addCompilerPlugin(
            "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
        ),
        javacOptions ++= Seq( "-source", "1.8", "-target", "1.8" ),
        scalacOptions ++= Seq( "-target:jvm-1.8"),
        useCoursier := false,
        libraryDependencies ++= logging
                                ++ typesafeConfig
                                ++ betterFiles
                                ++ jackson
                                ++ scalaTest,
        excludeDependencies ++= Seq( ExclusionRule( "org.slf4j", "slf4j-log4j12" ) ),
        dependencyOverrides ++= Seq( "com.google.guava" % "guava" % "15.0",
                                     "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
                                     "com.fasterxml.jackson.core" % "jackson-annotation" % jacksonVersion,
                                     "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion ),
        // `sbt test` should skip tests tagged IntegrationTest
        Test / testOptions := Seq( Tests.Argument( "-l", "annotations.IntegrationTest" ) ),
        Test / parallelExecution := false,
        //         `sbt integration:test` should run only tests tagged IntegrationTest
        IntegrationConfig / parallelExecution := false,
        IntegrationConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.IntegrationTest" ) ),
        //         `sbt wip:test` should run only tests tagged WipTest
        WipConfig / testOptions := Seq( Tests.Argument( "-n", "annotations.WipTest" ) ),
    )
}

lazy val assemblySettings = Seq(
    assemblyMergeStrategy in assembly := {
        case PathList( "META-INF", xs@_* ) => MergeStrategy.discard // may have to adjust this
        case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
        case PathList( "reference.conf" ) => MergeStrategy.concat
        case x => MergeStrategy.last
    },
    mainClass in(Compile, assembly) := Some( "Application" ),
    test in assembly := {}
)

lazy val disablePublish = Seq(
    skip.in( publish ) := true,
)

sonatypeProfileName := "com.twosixlabs"
inThisBuild(List(
    organization := "com.twosixlabs.dart.concepts",
    homepage := Some(url("https://github.com/twosixlabs-dart/concepts-api")),
    licenses := List("GNU-Affero-3.0" -> url("https://www.gnu.org/licenses/agpl-3.0.en.html")),
    developers := List(
        Developer(
            "twosixlabs-dart",
            "Two Six Technologies",
            "",
            url("https://github.com/twosixlabs-dart")
        )
    )
))

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  PROJECT DEFINITIONS                                     ##
   ##                                                                                          ##
   ##############################################################################################
 */

lazy val root = ( project in file( "." ) )
  .aggregate( conceptsApi, conceptsServices, conceptsClient, conceptsControllers, conceptsMicroservice )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      name := "concepts-api",
      disablePublish,
  )

lazy val conceptsApi = ( project in file( "concepts-api" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      name := "concepts-api",
      commonSettings,
      libraryDependencies ++= cdr4s ++ jackson,
  )

lazy val conceptsServices = ( project in file( "concepts-services" ) )
  .dependsOn( conceptsApi )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      name := "concepts-services",
      commonSettings,
      libraryDependencies ++= jackson
                              ++ cdr4s
                              ++ dartCommons
                              ++ dartAuthCore
                              ++ arangoDatastoreRepo
                              ++ okhttp
                              ++ conceptDiscovery
                              ++ ontologyRegistry
                              ++ java8Compat,
  )

lazy val conceptsControllers = ( project in file( "concepts-controllers" ) )
  .dependsOn( conceptsServices, conceptsApi )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson
                              ++ scalatra
                              ++ dartRest
                              ++ dartCommons,
  )

lazy val conceptsMicroservice = ( project in file( "concepts-microservice" ) )
  .dependsOn( conceptsControllers, conceptsServices )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .settings(
      commonSettings,
      disablePublish,
      assemblySettings,
      libraryDependencies ++= arangoDatastoreRepo,
  )

lazy val conceptsClient = ( project in file( "concepts-client" ) )
  .dependsOn( conceptsApi )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= okhttp ++ jackson ++ java8Compat,
  )

