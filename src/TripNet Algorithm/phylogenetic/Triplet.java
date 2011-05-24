package phylogenetic;

public class Triplet
{
  int i;
  int j;
  int k;
  double w;
  LeafSet il;
  LeafSet jl;
  LeafSet kl;

  public Triplet(LeafSet i, LeafSet j, LeafSet k)
  {
    this(i.index, j.index, k.index);
    this.il = i;
    this.jl = j;
    this.kl = k;
  }

  public String toString()
  {
    return this.i + "," + this.j + "|" + this.k;
  }

  public boolean equals(Object o)
  {
    if (this == o) return true;
    if ((o == null) || (super.getClass() != o.getClass())) return false;

    Triplet triplet = (Triplet)o;

    return (this.i == triplet.i) && (this.j == triplet.j) && (this.k == triplet.k);
  }

  public int hashCode()
  {
    int result = this.i;
    result = 310 * result + this.j;
    result = 310 * result + this.k;
    return result;
  }

  public Triplet(int i, int j, int k) {
    this.i = Math.min(i, j);
    this.j = Math.max(i, j);
    this.k = k;
  }

  public boolean has(Integer x) {
    return (x != null) && (((this.i == x.intValue()) || (this.j == x.intValue()) || (this.k == x.intValue())));
  }
}