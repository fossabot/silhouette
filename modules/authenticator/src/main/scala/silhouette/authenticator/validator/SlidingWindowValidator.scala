/**
 * Licensed to the Minutemen Group under one or more contributor license
 * agreements. See the COPYRIGHT file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package silhouette.authenticator.validator

import java.time.Clock

import cats.data.Validated._
import cats.effect.Async
import silhouette.authenticator.Validator._
import silhouette.authenticator.validator.SlidingWindowValidator._
import silhouette.authenticator.{ Authenticator, Validator }

import scala.concurrent.duration._

/**
 * A validator that checks if an [[Authenticator]] has timed out after a certain time if it hasn't been used.
 *
 * An [[Authenticator]] can use a sliding window expiration. This means that the [[Authenticator]] times out
 * after a certain time if it hasn't been used. So it checks if the time elapsed since the last time the
 * [[Authenticator]] was used, is longer than the maximum idle timeout specified as [[idleTimeout]] argument
 * of the validator.
 *
 * If the [[Authenticator.touched]] property isn't set, then this validator returns always true.
 *
 * @param idleTimeout The duration an [[Authenticator]] can be idle before it timed out.
 * @param clock       The clock implementation to validate against.
 * @tparam F The type of the IO monad.
 */
final case class SlidingWindowValidator[F[_]: Async](idleTimeout: FiniteDuration, clock: Clock) extends Validator[F] {

  /**
   * Checks if the [[Authenticator]] is valid.
   *
   * @param authenticator The [[Authenticator]] to validate.
   * @return [[cats.data.Validated]] if the authenticator is valid or invalid.
   */
  override def isValid(authenticator: Authenticator): F[Status] =
    Async[F].pure {
      if (authenticator.touchedAt(clock).forall(_ <= idleTimeout))
        validNel(())
      else
        invalidNel(Error.format(authenticator.touchedAt(clock).getOrElse(0.millis) - idleTimeout))
    }
}

/**
 * The companion object.
 */
object SlidingWindowValidator {
  val Error = "Authenticator timed out %s ago"
}
