/**
 * Copyright (c) 2016-2017 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.caps.api.graph

import org.opencypher.caps.api.record.{CypherPrintable, CypherRecords}
import org.opencypher.caps.api.util.PrintOptions
import org.opencypher.caps.impl.logical.LogicalOperator

trait CypherResult extends CypherPrintable {

  type Graph <: CypherGraph
  type Records <: CypherRecords

  def sourceGraph: Graph = ???
  def targetGraph: Graph = ???

  def sourceGraphName: String = ???
  def targetGraphName: String = ???

  def graphs: Map[String, Graph]
  def records: Records

  def explain: CypherPlanInfo
}


final case class CypherPlanInfo(plan: LogicalOperator) extends CypherPrintable {
  override def toString: String = plan.pretty()

  override def print(implicit options: PrintOptions): Unit =
    options.stream.print(plan)
}
