name := "Foo root project"

scalaVersion in ThisBuild := "2.12.4"

lazy val root = project.in(file(".")).
  aggregate(fooJS, fooJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val foo = crossProject.in(file(".")).
  settings(
    name := "foo",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
        //"io.circe"       %%% "circe-core" % "0.4.1",
        "io.circe" %%% "circe-core" % "0.10.0-M1",
        "io.circe" %%% "circe-generic" % "0.10.0-M1",
        "io.circe"                       %%% "circe-parser"           % "0.10.0-M1",
        "io.suzaku" %%% "boopickle" % "1.3.0"

)
  ).
  jvmSettings(
    // Add JVM-specific settings here
  ).
  jsSettings(
    // Add JS-specific settings here
  )

lazy val fooJVM = foo.jvm
lazy val fooJS = foo.js
