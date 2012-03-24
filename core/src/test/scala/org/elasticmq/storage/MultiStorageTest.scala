package org.elasticmq.storage

import inmemory._
import org.scalatest.matchers.MustMatchers
import org.scalatest._
import org.elasticmq.storage.squeryl._
import org.elasticmq.test.DataCreationHelpers

trait MultiStorageTest extends FunSuite with MustMatchers with OneInstancePerTest with DataCreationHelpers {
  private case class StorageTestSetup(storageName: String, 
                                      initialize: () => StorageCommandExecutor)
  
  val squerylDBConfiguration = DBConfiguration.h2(this.getClass.getName)

  private val setups: List[StorageTestSetup] =
    StorageTestSetup("Squeryl",
      () => new SquerylStorageCommandExecutor(squerylDBConfiguration)) ::
    StorageTestSetup("In memory",
      () => new InMemoryStorageCommandExecutor()) ::
    Nil

  protected var storageCommandExecutor: StorageCommandExecutor = null

  private var currentSetup: StorageTestSetup = null
  
  private var _befores: List[() => Unit] = Nil

  def before(block: => Unit) {
    _befores = (() => block) :: _befores
  }

  abstract override protected def test(testName: String, testTags: Tag*)(testFun: => Unit) {
    for (setup <- setups) {
      super.test(testName+" using "+setup.storageName, testTags: _*) {
        currentSetup = setup

        try {
          newStorageCommandExecutor()
          testFun
        } finally {
          storageCommandExecutor.shutdown()
          currentSetup = null
        }
      }
    }
  }
  
  def execute[R](command: StorageCommand[R]): R = storageCommandExecutor.execute(command)
  
  def newStorageCommandExecutor() {
    if (storageCommandExecutor != null) {
      storageCommandExecutor.shutdown()
    }
    
    storageCommandExecutor = currentSetup.initialize()
    _befores.foreach(_())
  }
}





