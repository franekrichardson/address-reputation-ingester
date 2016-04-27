/*
 *
 *  * Copyright 2016 HM Revenue & Customs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package controllers

import java.util.concurrent.SynchronousQueue

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ingester.exec.{Task, WorkQueue}
import uk.co.hmrc.logging.StubLogger

@RunWith(classOf[JUnitRunner])
class AdminControllerTest extends FunSuite {

  test(
    """
      when cancel task is called
      and no task is executing
      then a bad request response is returned
    """) {
    println("********** ACT1 **********")
    val logger = new StubLogger
    val worker = new WorkQueue(logger)
    val ac = new AdminController(worker)
    val request = FakeRequest()

    val futureResponse = call(ac.cancelTask(), request)

    val response = await(futureResponse)
    assert(response.header.status === 400)
    worker.terminate()
  }

  test(
    """
      when cancel task is called
      and a task is executing
      then a successful response is returned
    """) {
    println("********** ACT2 **********")
    val logger = new StubLogger
    val stuff = new SynchronousQueue[Boolean]()
    val worker = new WorkQueue(logger)
    worker.push(Task("thinking", {
      c =>
      Thread.sleep(100)
      println("I'm blue !!!")
      Thread.sleep(100)
      stuff.take() // blocks until signalled
      println("I'm green !!!")
      Thread.sleep(100)
      stuff.take() // blocks until signalled
      println("I'm a teapot !!!")
    }))

    stuff.put(true) // release the lock first time
    Thread.sleep(100)
    println("Is he blue ???")
    val ac = new AdminController(worker)
    val request = FakeRequest()

    val futureResponse = call(ac.cancelTask(), request)

    val response = await(futureResponse)
    assert(response.header.status === 200)
    stuff.put(true) // release the lock second time
    worker.awaitCompletion()
    println("Is he green ???")
    worker.terminate()
  }
}
