package dockerenv

import java.util.UUID

import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

/**
  * Base test class which ensures the container in the script directory is running before/after the tests
  */
abstract class BaseDockerSpec(val dockerEnv: DockerEnv.Instance)
    extends WordSpec
    with Matchers
    with Eventually
    with BeforeAndAfterAll
    with GivenWhenThen
    with ScalaFutures
    with BeforeAndAfterEach {

  def this(scriptDir: String) = this(DockerEnv(scriptDir))

  def testTimeout: FiniteDuration = 5.seconds

  def randomString() = BaseDockerSpec.randomString()

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  private var DockerIsRunningStateBeforeTest = false

  override def beforeAll(): Unit = {
    super.beforeAll()
    DockerIsRunningStateBeforeTest = isDockerRunning()
  }

  def insideRunningEnvironment[A](f: => A): A = {
    if (isDockerRunning()) {
      f
    } else {
      startDocker()
      try {
        f
      } finally {
        stopDocker()
      }
    }
  }

  override def beforeEach(): Unit = {
    ensureDockerIsRunning() shouldBe true
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!DockerIsRunningStateBeforeTest) {
      stopDocker()
    }
  }

  def isDockerRunning(): Boolean = BaseDockerSpec.Lock.synchronized {
    dockerEnv.isRunning()
  }

  def ensureDockerIsRunning(): Boolean = startDocker

  def startDocker(): Boolean = BaseDockerSpec.Lock.synchronized {
    if (!isDockerRunning) {
      dockerEnv.start()
      eventually {
        isDockerRunning() shouldBe true
      }
    }
    true
  }

  def restartDocker(): Boolean = {
    stopDocker() shouldBe true
    isDockerRunning() shouldBe false
    startDocker() shouldBe true
    val running = isDockerRunning()
    running shouldBe true
    running
  }

  def stopDocker(): Boolean = BaseDockerSpec.Lock.synchronized {

    if (isDockerRunning) {
      dockerEnv.stop()
      withClue("is running never returned false") {
        eventually {
          isDockerRunning() shouldBe false
        }
      }
    }
    true
  }
}

object BaseDockerSpec {

  private object Lock

  def randomString() = UUID.randomUUID.toString
}
