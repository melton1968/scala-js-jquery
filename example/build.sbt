name := "jquery-demo"

inThisBuild(Seq(
  version := "3.0.1",
  organization := "io.udash",
))

val commonSettings = Seq(
  scalaVersion := "2.12.10",
  crossScalaVersions := Seq("2.12.10"), //todo 2.13
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-language:postfixOps",
    "-Xfatal-warnings",
    "-Xlint:_",
    "-Ywarn-unused:_,-explicits,-implicits",
    "-Ybackend-parallelism", "4",
    "-Ycache-plugin-class-loader:last-modified",
    "-Ycache-macro-class-loader:last-modified"
  ),

  libraryDependencies ++= Dependencies.deps.value
)

val generatedGlobalDir = file("generated/global")
val copyAssets = taskKey[Unit]("Copies all assets to the target directory.")
val root = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)

val `jquery-global-demo` = project.in(file("global-demo"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,

    jsDependencies ++= Dependencies.jsDeps.value,

    sourceDirsSettings(_.getParentFile),

    /* move these files out of target/. */
    Compile / fullOptJS / crossTarget := generatedGlobalDir,
    Compile / fastOptJS / crossTarget := generatedGlobalDir,
    Compile / packageJSDependencies / crossTarget := generatedGlobalDir,
    Compile / packageMinifiedJSDependencies / crossTarget := generatedGlobalDir,

    Compile / fastOptJS := (Compile / fastOptJS).dependsOn(copyAssets).value,
    Compile / fullOptJS := (Compile / fullOptJS).dependsOn(copyAssets).value,

    scalaJSUseMainModuleInitializer := true,

    copyAssets := {
      IO.copyFile(
        sourceDirectory.value / "main/assets/index.html",
        generatedGlobalDir / "index.html"
      )
    },

    Compile / fastOptJS / artifactPath :=
      (Compile / fastOptJS / crossTarget).value / "scripts" / "frontend-impl.js",
    Compile / fullOptJS / artifactPath :=
      (Compile / fullOptJS / crossTarget).value / "scripts" / "frontend-impl.js",
    Compile / packageJSDependencies / artifactPath :=
      (Compile / packageJSDependencies / crossTarget).value / "scripts" / "frontend-deps.js",
    Compile / packageMinifiedJSDependencies / artifactPath :=
      (Compile / packageMinifiedJSDependencies / crossTarget).value / "scripts" / "frontend-deps.js"
  )

val generatedBundlerDir = file("generated/bundler")
val compileStatics = taskKey[Unit]("Compiles all static files.")
val `jquery-bundler-demo` = project.in(file("bundler-demo"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    commonSettings,

    sourceDirsSettings(_.getParentFile),

    Compile / scalaJSUseMainModuleInitializer := true,

    copyAssets := {
      IO.copyFile(
        sourceDirectory.value / "main/assets/index.html",
        generatedBundlerDir / "index.html"
      )
    },

    compileStatics := {
      val sjsFileName = (Compile / fastOptJS).value.data.name.stripSuffix(".js")
      IO.copyFile(
        (Compile / npmUpdate / crossTarget).value / s"$sjsFileName-bundle.js",
        generatedBundlerDir / "scripts/frontend.js"
      )
      IO.copyFile(
        (Compile / npmUpdate / crossTarget).value / s"$sjsFileName-bundle.js.map",
        generatedBundlerDir / "scripts/frontend.js.map"
      )
    },
    compileStatics := compileStatics.dependsOn(Compile / fastOptJS / webpack, copyAssets).value,
  )

def mkSourceDirs(base: File, scalaBinary: String, conf: String): Seq[File] = Seq(
  base / "src" / conf / "scala",
  base / "src" / conf / s"scala-$scalaBinary",
  base / "src" / conf / "java"
)

def mkResourceDirs(base: File, conf: String): Seq[File] = Seq(
  base / "src" / conf / "resources"
)

def sourceDirsSettings(baseMapper: File => File) = Seq(
  Compile / unmanagedSourceDirectories ++=
    mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "main"),
  Compile / unmanagedResourceDirectories ++=
    mkResourceDirs(baseMapper(baseDirectory.value), "main"),
  Test / unmanagedSourceDirectories ++=
    mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "test"),
  Test / unmanagedResourceDirectories ++=
    mkResourceDirs(baseMapper(baseDirectory.value), "test"),
)