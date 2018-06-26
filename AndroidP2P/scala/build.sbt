name := "Boopickle only Foo root project"

//scalaVersion in ThisBuild := "2.12.4"
scalaVersion in ThisBuild := "2.11.12"

lazy val root = project.in(file(".")).
  aggregate(bpFooJS, bpFooJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val bpFoo = crossProject.in(file(".")).
  settings(
    name := "bpFoo",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
        //"io.circe"       %%% "circe-core" % "0.4.1",
        //"io.circe" %%% "circe-core" % "0.10.0-M1",
        //"io.circe" %%% "circe-generic" % "0.10.0-M1",
        //"io.circe"                       %%% "circe-parser"           % "0.10.0-M1",
        //"io.suzaku" %%% "boopickle" % "1.3.0"
        "me.chrons" %%% "boopickle" % "1.2.5"

)
  ).
  jvmSettings(
    // Add JVM-specific settings here
  ).
  jsSettings(
    // Add JS-specific settings here
  )

lazy val bpFooJVM = bpFoo.jvm
lazy val bpFooJS = bpFoo.js
