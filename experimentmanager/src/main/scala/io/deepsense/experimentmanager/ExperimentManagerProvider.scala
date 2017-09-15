/**
 * Copyright (c) 2015, CodiLime Inc.
 *
 * Owner: Wojciech Jurczyk
 */

package io.deepsense.experimentmanager

import scala.concurrent.Future

import io.deepsense.commons.auth.usercontext.UserContext

trait ExperimentManagerProvider {
  def forContext(userContext: Future[UserContext]): ExperimentManager
}