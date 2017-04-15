name := "rabitMq"

version := "1.0"

scalaVersion := "2.11.9"

resolvers ++= Seq(
  "SpinGo OSS" at "http://spingo-oss.s3.amazonaws.com/repositories/releases"
)

val opRabbitVersion = "1.6.0"

libraryDependencies ++= Seq("com.spingo" %% "op-rabbit-core" % opRabbitVersion,
                            "com.spingo" %% "op-rabbit-play-json" % opRabbitVersion)