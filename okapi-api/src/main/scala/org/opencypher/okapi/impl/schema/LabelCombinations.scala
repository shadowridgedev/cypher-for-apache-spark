/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.okapi.impl.schema

object LabelCombinations {
  val empty: LabelCombinations = LabelCombinations(Set.empty)
}

case class LabelCombinations(combos: Set[Set[String]]) {

  /**
    * Returns all combinations that contain the argument `labels`
    */
  def combinationsFor(labels: Set[String]): Set[Set[String]] =
    combos.filter(labels.subsetOf)

  def withCombinations(coExistingLabels: String*): LabelCombinations = {
    val (lhs, rhs) = combos.partition(labels => coExistingLabels.exists(labels(_)))
    copy(combos = rhs + (lhs.flatten ++ coExistingLabels))
  }

  def ++(other: LabelCombinations): LabelCombinations = copy(combos ++ other.combos)
}