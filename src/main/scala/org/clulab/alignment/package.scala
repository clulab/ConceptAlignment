package org.clulab

package object alignment {

  // TODO fix in a good way -- unify these
  def dotProduct(v1:Array[Double], v2:Array[Double]):Double = {
    assert(v1.length == v2.length) //should we always assume that v2 is longer? perhaps set shorter to length of longer...
    var sum = 0.0f // optimization
    var i = 0 // optimization
    while(i < v1.length) {
      sum += (v1(i) * v2(i)).toFloat
      i += 1
    }
    sum
  }

  def dotProduct(v1:Array[Float], v2:Array[Float]):Float = {
    assert(v1.length == v2.length) //should we always assume that v2 is longer? perhaps set shorter to length of longer...
    var sum = 0.0f // optimization
    var i = 0 // optimization
    while(i < v1.length) {
      sum += v1(i) * v2(i)
      i += 1
    }
    sum
  }

}
