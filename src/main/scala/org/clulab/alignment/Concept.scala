package org.clulab.alignment

// Classes to hold the concepts, as well as any metadata that may arise
class Concept(val name: String)
class FlatConcept(name: String, val embedding: Array[Float]) extends Concept(name)
class CompositionalConcept(name: String, val base: FlatConcept, val arguments: Seq[ArgumentConcept]) extends Concept(name)

case class ArgumentConcept(role: String, concept: FlatConcept)
case class ConceptSequence(concepts: Seq[Concept])
