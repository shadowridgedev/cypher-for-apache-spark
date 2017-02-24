package org.opencypher.spark.api

import org.opencypher.spark.api.value.CypherValue
import org.opencypher.spark.impl.SupportedQuery
import org.opencypher.spark.prototype.Expr
import org.opencypher.spark.prototype.ir.CypherQuery

trait PropertyGraph {
  def cypher(query: SupportedQuery): CypherResultContainer

  def cypherNew(ir: CypherQuery[Expr], params: Map[String, CypherValue]): CypherResultContainer
}



