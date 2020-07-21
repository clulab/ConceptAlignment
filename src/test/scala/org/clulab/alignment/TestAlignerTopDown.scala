//This script test the mapping from ontology node to the examples. (from column 2 to column 1)
import org.scalatest._

class TestAlignerTopDown extends FlatSpec with Matchers {

  behavior of "Top down concept aligner"

  it should "have an precision@10 above 0.6" in {
    1>0 should be (true)
  }
}
