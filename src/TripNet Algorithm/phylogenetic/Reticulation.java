package phylogenetic;

import graphlab.graph.graph.GraphModel;

class Reticulation
{
  LeafSet reticulate;
  LeafSet rightChild;
  LeafSet leftChild;

  public int hashCode()
  {
    return this.reticulate.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof Reticulation) {
      Reticulation reticulation = (Reticulation)obj;
      return this.reticulate.equals(reticulation.reticulate);
    }
    return false;
  }

  public Reticulation(LeafSet reticulate, LeafSet rightChild, LeafSet leftChild, GraphModel g)
  {
    this.reticulate = reticulate;
    this.rightChild = rightChild;
    this.leftChild = leftChild;
  }
}