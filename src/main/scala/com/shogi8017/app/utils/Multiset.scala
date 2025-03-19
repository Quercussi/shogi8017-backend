package com.shogi8017.app.utils

import scala.annotation.targetName

/**
 * Represents a multiset, a collection where elements can appear multiple times.
 *
 * @param elements a map storing elements and their respective counts
 * @tparam T the type of elements contained in the multiset
 */
case class Multiset[T](elements: Map[T, Int]) {

  /**
   * Creates a new multiset with an additional element.
   *
   * @param elem the element to added
   * @return a new multiset that contains all elements of this multiset and that also contains `elem`.
   */
  @targetName("add")
  def +(elem: T): Multiset[T] = {
    val updatedCount = elements.getOrElse(elem, 0) + 1
    copy(elements = elements + (elem -> updatedCount))
  }

  /**
   * Creates a new multiset by adding all elements contained in another collection to this multiset
   *
   * @param that – the collection containing the elements to add.
   * @return a new multiset with the given elements added.
   */
  @targetName("addAll")
  def ++(that: Multiset[T]): Multiset[T] = {
    val merged = that.elements.foldLeft(elements) { case (acc, (elem, count)) =>
      acc + (elem -> (acc.getOrElse(elem, 0) + count))
    }
    copy(elements = merged)
  }

  /**
   * Creates a new multiset with a given element removed from this multiset.
   *
   * @param `elem`` – the element to be removed
   * @return a new multiset that contains all elements of this multiset but occurrence of `elem`, or without `elem` if its count reaches zero.
   */
  @targetName("remove")
  def -(elem: T): Multiset[T] = {
    elements.get(elem) match {
      case Some(count) if count > 1 => copy(elements = elements + (elem -> (count - 1)))
      case Some(_)                  => copy(elements = elements - elem)
      case None                     => this
    }
  }

  /**
   * Computes the difference of this multiset and another multiset.
   *
   * @param that – the multiset of elements to exclude.
   * @return a new multiset containing the elements of this multiset with the elements from the given multiset removed.
   */
  @targetName("removeAll")
  def --(that: Multiset[T]): Multiset[T] = {
    val reduced = that.elements.foldLeft(elements) { case (acc, (elem, count)) =>
      acc.get(elem) match {
        case Some(existingCount) if existingCount > count =>
          acc + (elem -> (existingCount - count))
        case Some(_) => acc - elem
        case None    => acc
      }
    }
    copy(elements = reduced)
  }


  /**
   * Checks whether the multiset contains at least one occurrence of the specified element.
   *
   * @param elem the element to check
   * @return `true` if the element is present in the multiset, otherwise `false`
   */
  def contains(elem: T): Boolean = elements.contains(elem)
  
  /**
   * Returns the count of a specific element in the multiset.
   *
   * @param elem the element to count
   * @return the count of the element
   */
  def count(elem: T): Int = elements.getOrElse(elem, 0)

  /**
   * Checks if the multiset is empty.
   *
   * @return `true` if the multiset contains no elements, otherwise `false`
   */
  def isEmpty: Boolean = elements.isEmpty

  /**
   * Converts the multiset to a set of unique elements.
   *
   * @return a set of unique elements in the multiset
   */
  def toSet: Set[T] = elements.keySet

  /**
   * Converts the multiset to a map of elements and their counts.
   *
   * @return a map where keys are elements and values are their respective counts
   */
  def toMap: Map[T, Int] = elements
  
  /**
   * Checks if this multiset is equal to another object.
   *
   * @param other the object to compare
   * @return `true` if the other object is a multiset with the same elements and counts, otherwise `false`
   */
  override def equals(other: Any): Boolean = other match {
    case that: Multiset[T] => this.elements == that.elements
    case _ => false
  }

  /**
   * Returns a string representation of the multiset.
   *
   * @return a string in the format `Multiset(element x count, ...)`
   */
  override def toString: String = elements.toList
    .map { case (elem, count) => s"$elem x$count" }
    .mkString("Multiset(", ", ", ")")
}

/**
 * Companion object for the Multiset class, providing utility methods.
 */
object Multiset {

  /**
   * Creates an empty multiset.
   *
   * @tparam T the type of elements to be stored in the multiset
   * @return an empty multiset
   */
  def empty[T]: Multiset[T] = Multiset(Map.empty[T, Int])

  /**
   * Creates a multiset from a sequence of elements.
   *
   * @param elems the elements to include in the multiset
   * @tparam T the type of elements to be stored in the multiset
   * @return a multiset containing the elements and their counts
   */
  def apply[T](elems: T*): Multiset[T] = {
    Multiset(elems.groupBy(identity).view.mapValues(_.size).toMap)
  }
}