package org.opencypher.spark.prototype.api.graph

import org.opencypher.spark.prototype.api.expr.Var
import org.opencypher.spark.prototype.api.schema.Schema

trait CypherGraph {
  type Space <: GraphSpace
  type View <: CypherView
  type Graph <: CypherGraph

  def space: Space

  def nodes(v: Var): View
  def relationships(v: Var): View

  def constituents: Set[View]

  def schema: Schema

  // identity
  // properties
  // labels
}


