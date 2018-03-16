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
package org.opencypher.okapi.ir.api.block

import org.opencypher.okapi.ir.api.{IRField, IRGraph}

final case class UnwindBlock[E](
    after: Set[BlockRef],
    binds: UnwoundList[E],
    graph: IRGraph
) extends BasicBlock[UnwoundList[E], E](BlockType("unwind")) {
  override def where: Set[E] = Set.empty[E] // never filters
}

final case class UnwoundList[E](list: E, variable: IRField) extends Binds[E] {
  override def fields: Set[IRField] = Set(variable)
}