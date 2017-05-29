package org.opencypher.spark.impl.flat

import org.opencypher.spark.api.ir.global.GlobalsRegistry
import org.opencypher.spark.api.schema.Schema
import org.opencypher.spark.impl.logical.LogicalOperator
import org.opencypher.spark.impl.{DirectCompilationStage, logical}

final case class FlatPlannerContext(schema: Schema, globalsRegistry: GlobalsRegistry)

class FlatPlanner extends DirectCompilationStage[LogicalOperator, FlatOperator, FlatPlannerContext] {

  override def process(input: LogicalOperator)(implicit context: FlatPlannerContext): FlatOperator = {
    val producer = new FlatOperatorProducer()

    input match {

      case logical.Select(fields, in) =>
        producer.select(fields, process(in))

      case logical.Filter(expr, in) =>
        producer.filter(expr, process(in))

      case logical.NodeScan(node, nodeDef, in) =>
        // TODO: Recursively process nested plan
        producer.nodeScan(node, nodeDef)

      case logical.Project(it, in) =>
        producer.project(it, process(in))

      case logical.ExpandSource(source, rel, types, target, in) =>
        producer.expandSource(source, rel, types, target, process(in))

      case x => throw new NotImplementedError(s"Flat planning not done yet for $x")
    }
  }
}
