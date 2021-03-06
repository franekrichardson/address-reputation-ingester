/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

object SimpleValidator {
  def defaultTo(value: String, default: String): String =
    if (value.isEmpty) default else value

  def constrainRange(value: Int, min: Int, max: Int): Int =
    if (value < min) min
    else if (value > max) max
    else value

  def isAlphaNumeric(param: String, maxLength: Int = 20): Boolean =
    param.length <= maxLength && alphaNumPattern.matcher(param).matches()

  def isAlphaNumOrUnderscore(param: String, maxLength: Int = 20): Boolean =
    param.length <= maxLength && alphaNumUscorePattern.matcher(param).matches()

  def isTimestamp(param: String): Boolean =
    param.length == 12 && timestampPattern.matcher(param).matches()

  private val alphaNumPattern = "[a-z0-9]+".r.pattern

  private val alphaNumUscorePattern = "[a-z0-9_]+".r.pattern

  def isNumeric(param: String): Boolean =
    param.length <= 10 && numPattern.matcher(param).matches()

  private val numPattern = "[0-9]+".r.pattern

  private val timestampPattern = "20[0-9]{10}".r.pattern
}
