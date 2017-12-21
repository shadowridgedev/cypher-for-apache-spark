/*
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
package org.opencypher.caps.impl.common

import org.opencypher.caps.impl.exception.Raise

import scala.reflect.ClassTag

/**
  * Class that implements the `children` and `withNewChildren` methods using reflection when implementing
  * `TreeNode` with a case class or case object.
  *
  * Requirements: All child nodes need to be individual constructor parameters and their order
  * in children is their order in the constructor. Every constructor parameter of type `T` is
  * assumed to be a child node.
  *
  * This class caches values that are expensive to recompute. It also uses array operations instead of
  * Scala collections, both for improved performance as well as to save stack frames, which allows to operate on
  * trees that are several thousand nodes high.
  *
  * The constructor can also contain a list of children, but there are constraints:
  *   - A list of children cannot be empty, because the current design relies on testing the type of an element.
  *   - If any children are contained in a list at all, then all list elements need to be children. This allows
  *     to only check the type of the first element.
  *   - There can be at most one list of children and there can be no normal child constructor parameters
  *     that appear after the list of children. This allows to call `withNewChildren` with a different number of
  *     children than the original node had and vary the length of the list to accommodate.
  */
abstract class AbstractTreeNode[T <: AbstractTreeNode[T]: ClassTag] extends TreeNode[T] {
  self: T =>

  override final val children: Array[T] = {
    val constructorParamLength = productArity
    val childrenCount = {
      var count = 0
      var usedListOfChildren = false
      var i = 0
      while (i < constructorParamLength) {
        val pi = productElement(i)
        pi match {
          case _: T =>
            require(
              !usedListOfChildren,
              "there can be no normal child constructor parameters after a list of children.")
            count += 1
          case l: List[_] if l.nonEmpty =>
            // Need explicit pattern match for T, as `isInstanceOf` in `if` results in a warning.
            l.head match {
              case _: T =>
                require(!usedListOfChildren, "there can be at most one list of children in the constructor.")
                usedListOfChildren = true
                count += l.length
              case _ =>
            }
          case _ =>
        }
        i += 1
      }
      count
    }
    val childrenArray = new Array[T](childrenCount)
    if (childrenCount > 0) {
      var i = 0
      var ci = 0
      while (i < constructorParamLength) {
        val pi = productElement(i)
        pi match {
          case c: T =>
            childrenArray(ci) = c
            ci += 1
          case l: List[_] if l.nonEmpty =>
            // Need explicit pattern match for T, as `isInstanceOf` in `if` results in a warning.
            l.head match {
              case _: T =>
                val j = l.iterator
                while (j.hasNext) {
                  val child = j.next
                  try {
                    childrenArray(ci) = child.asInstanceOf[T]
                  } catch {
                    case c: ClassCastException =>
                      Raise.invalidArgument(
                        "a list that contains either no children or only children",
                        "a mixed list that contains a child as the head element, " +
                          s"but also one with a non-child type: ${c.getMessage}"
                      )
                  }
                  ci += 1
                }
              case _ =>
            }
          case _ =>
        }
        i += 1
      }
    }
    childrenArray
  }

  @inline override final def withNewChildren(newChildren: Array[T]): T = {
    if (sameAsCurrentChildren(newChildren)) {
      self
    } else {
      val updatedConstructorParams = updateConstructorParams(newChildren)
      val copyMethod = AbstractTreeNode.copyMethod(self)
      try {
        copyMethod(updatedConstructorParams: _*).asInstanceOf[T]
      } catch {
        case e: Exception =>
          Raise.invalidArgument(
            s"valid constructor arguments for $productPrefix",
            s"""|${updatedConstructorParams.mkString(", ")}
                |Copy method: $copyMethod
                |Original exception: $e""".stripMargin
          )
      }
    }
  }

  override final lazy val hashCode: Int = super.hashCode

  final lazy val childrenAsSet = children.toSet

  override final lazy val size: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 1
    while (i < childrenLength) {
      result += children(i).size
      i += 1
    }
    result
  }

  final override lazy val height: Int = {
    val childrenLength = children.length
    var i = 0
    var result = 0
    while (i < childrenLength) {
      result = math.max(result, children(i).height)
      i += 1
    }
    result + 1
  }

  @inline final override def map[O <: TreeNode[O]: ClassTag](f: T => O): O = {
    val childrenLength = children.length
    if (childrenLength == 0) {
      f(self)
    } else {
      val mappedChildren = new Array[O](childrenLength)
      var i = 0
      while (i < childrenLength) {
        mappedChildren(i) = f(children(i))
        i += 1
      }
      f(self).withNewChildren(mappedChildren)
    }
  }

  @inline final override def foreach[O](f: T => O): Unit = {
    f(this)
    val childrenLength = children.length
    var i = 0
    while (i < childrenLength) {
      children(i).foreach(f)
      i += 1
    }
  }

  @inline final override def containsTree(other: T): Boolean = super.containsTree(other)

  @inline final override def containsChild(other: T): Boolean = {
    childrenAsSet.contains(other)
  }

  @inline final override def transformUp(rule: PartialFunction[T, T]): T = super.transformUp(rule)

  @inline final override def transformDown(rule: PartialFunction[T, T]): T = super.transformDown(rule)

  @inline private final def updateConstructorParams(newChildren: Array[T]): Array[Any] = {
    val parameterArrayLength = productArity
    val childrenLength = children.length
    val newChildrenLength = newChildren.length
    val parameterArray = new Array[Any](parameterArrayLength)
    var productIndex = 0
    var childrenIndex = 0
    while (productIndex < parameterArrayLength) {
      val currentProductElement = productElement(productIndex)
      def nonChildCase(): Unit = {
        parameterArray(productIndex) = currentProductElement
      }
      currentProductElement match {
        case c: T if childrenIndex < childrenLength && c == children(childrenIndex) =>
          parameterArray(productIndex) = newChildren(childrenIndex)
          childrenIndex += 1
        case l: List[_] if childrenIndex < childrenLength && l.nonEmpty =>
          // Need explicit pattern match for T, as `isInstanceOf` in `if` results in a warning.
          l.head match {
            case _: T =>
              require(newChildrenLength > childrenIndex, s"a list of children cannot be empty.")
              parameterArray(productIndex) = newChildren.slice(childrenIndex, newChildrenLength).toList
              childrenIndex = newChildrenLength
            case _ => nonChildCase
          }
        case _ => nonChildCase
      }
      productIndex += 1
    }
    require(
      childrenIndex == newChildrenLength,
      "invalid number of children or used an empty list of children in the original node.")
    parameterArray
  }

  @inline private final def sameAsCurrentChildren(newChildren: Array[T]): Boolean = {
    val childrenLength = children.length
    if (childrenLength != newChildren.length) {
      false
    } else {
      var i = 0
      while (i < childrenLength && children(i) == newChildren(i)) i += 1
      i == childrenLength
    }
  }

}

/**
  * Caches an instance of the copy method per case class type.
  */
object AbstractTreeNode {

  import scala.reflect.runtime.universe
  import scala.reflect.runtime.universe._

  // No synchronization required: No problem if a cache entry is lost due to a concurrent write.
  @volatile private var cachedCopyMethods = Map.empty[Class[_], MethodMirror]

  private final lazy val mirror = universe.runtimeMirror(getClass.getClassLoader)

  @inline protected final def copyMethod(instance: AbstractTreeNode[_]): MethodMirror = {
    val instanceClass = instance.getClass
    cachedCopyMethods.getOrElse(
      instanceClass, {
        val copyMethod = reflectCopyMethod(instance)
        cachedCopyMethods = cachedCopyMethods.updated(instanceClass, copyMethod)
        copyMethod
      }
    )
  }

  @inline private final def reflectCopyMethod(instance: Object): MethodMirror = {
    val instanceMirror = mirror.reflect(instance)
    val tpe = instanceMirror.symbol.asType.toType
    val copyMethodSymbol = tpe.decl(TermName("copy")).asMethod
    instanceMirror.reflectMethod(copyMethodSymbol)
  }

}
