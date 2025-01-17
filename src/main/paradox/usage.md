## Usage

The goal is to ensure some docker service is running/available, typically for running tests.

The main published resource is just the 'DockerEnv' trait, which provides a hook to start, stop and check running containers.

This repo also contains a collection of common scripts for using different containers (mongo, kafka, orientdb, etc).

As the primary use-case is really for testing, you will typically want to depend on the test-artifact like this:

```scala
libraryDependencies += "com.github.aaronp" %% "dockerenv" % "{version}" classifier "tests"
``` 


You could also use this for running example applications like this:

assuming a project layout where 'Main' is an application which talks to mongo: 
 * src/main/scala/myapp/Main.scala 
 * src/test/scala/myapp/DevMain.scala
 
You could create a 'DevMain' to as a convenience to run your app stand-alone:

```scala
package myapp
object DevMain {

  def main(args :Array[Sring]) : Unit = {

     // make sure mongo is running while my app is:
     dockerenv.DockerEnv("scripts/mongo").bracket {
         // myapp.Main is an app on top of e.g. mongo (or a number of services)
         myapp.Main(args)
     }
  }
}


```

Otherwise you would typically do this in tests themselves. e.g. perhaps setup a base test setup for tests which require kafka:
```scala
abstract class BaseKafkaSpec extends BaseDockerSpec("scripts/kafka")
```

And then write tests which extend that. e.g.:

```scala
class KafkaTest extends BaseKafkaSpec {

  "Kafka" should {
    "run" in insideRunningEnvironment {
      isDockerRunning() shouldBe true
      val topic = randomString()

      val Success((0, createOutput)) = dockerEnv.runInScriptDir("createTopic.sh", topic)
      val Success((0, listOutput)) = dockerEnv.runInScriptDir("listTopics.sh")
      listOutput should include(topic)
    }
  }
}

```

NOTE: As we're starting/stopping external resources, you will want to ensure these sorts of tests are NOT run in parallel!

You could tag them as integration tests, or just put your integration tests in a separate test module.
