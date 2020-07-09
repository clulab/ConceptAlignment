package org.clulab.alignment

// Classes to hold the concepts, as well as any metadata that may arise
class Concept(val name: String)
class FlatConcept(name: String, val embedding: Array[Float]) extends Concept(name)
class CompositionalConcept(name: String, val base: FlatConcept, val arguments: Seq[ArgumentConcept]) extends Concept(name)

case class ArgumentConcept(role: String, concept: FlatConcept)
case class ConceptSequence(concepts: Seq[Concept])

// Class to hold the scoring info, as well as the concepts being scored and the method used
case class ScoredPair(scoringMethod: String, src: Concept, dst: Concept, value: Float)

trait Aligner {
  val name: String
  def align(c1: Concept, c2: Concept): ScoredPair

  // top k for a given source concept, from a provided list
  def topk(src: Concept, dsts: ConceptSequence, k: Int): Seq[ScoredPair] = {
    // Align the src to each of the dsts, sort descending, and take the top k
    dsts.concepts.map(dst => align(src, dst)).sortBy(- _.value).take(k)
  }

  // Exhaustive alignment between two concepts lists, unsorted
  def alignAll(srcs: ConceptSequence, dsts: ConceptSequence): Seq[ScoredPair] = {
    for {
      src <- srcs.concepts
      dst <- dsts.concepts
    } yield align(src, dst)
  }

  def getEmbedding(c: Concept): Array[Float] = {
    c match {
      case f: FlatConcept => f.embedding
      case comp: CompositionalConcept => comp.base.embedding
      case _ => ???
    }
  }
}
