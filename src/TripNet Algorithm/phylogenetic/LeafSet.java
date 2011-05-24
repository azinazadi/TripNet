package phylogenetic;

import graphlab.graph.graph.SubGraph;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

class LeafSet
{
  int type;
  static final int RIGHT = 1;
  static final int LEFT = 2;
  static final int NOT_SET = 0;
  HashSet<Integer> leaves = new HashSet();

  HashMap<Integer, LeafSet> contractedFrom = new HashMap();
  boolean mark;
  SubGraph sg;
  int maxw;
  int minw;
  int maxwOcurrance;
  int minwOcurrance;
  int numOfContractionsOnRemoval;
  Vector<LeafSet> contractionsOnRemoval;
  int numOfInconcsH;
  Reticulation r;
  int index;
  Vector<LeafSet> neighbours = new Vector();
  LeafSet unifiedFactorizationTarget;

  public int hashCode()
  {
    return this.leaves.hashCode();
  }

  public boolean equals(Object obj)
  {
    return (obj instanceof LeafSet) && (this.leaves.equals(((LeafSet)obj).leaves));
  }

  public String toString()
  {
    String ret = "{";
    Vector rr = new Vector(this.leaves);
    Collections.sort(rr);
    for (Iterator localIterator = rr.iterator(); localIterator.hasNext(); ) { int i = ((Integer)localIterator.next()).intValue();
      ret = ret + i + " ,"; }

    if (ret.endsWith(" ,"))
      ret = ret.substring(0, ret.length() - 2);
    return ret + "}";
  }

  public boolean contains(LeafSet l)
  {
    for (Integer i : l.leaves)
      if (!this.leaves.contains(i))
        return false;
    return true;
  }

  public static Vector<LeafSet> getContainingMembers(Vector<LeafSet> parent, Vector<LeafSet> child)
  {
    HashSet ret = new HashSet();
    for (LeafSet p : parent) {
      for (LeafSet c : child) {
        if ((p.contains(c)) && (!p.equals(c)))
          ret.add(p);
      }
    }
    return new Vector(ret);
  }

  public static HashSet<LeafSet> getContainedMembers(Collection<LeafSet> parent, Collection<LeafSet> child)
  {
    HashSet ret = new HashSet();
    for (LeafSet p : parent) {
      for (LeafSet c : child) {
        if ((p.contains(c)) && (!p.equals(c)))
          ret.add(c);
      }
    }
    return ret;
  }

  public static HashSet<LeafSet> getContainedOrEqualMembers(LeafSet parent, Vector<LeafSet> child)
  {
    HashSet ret = new HashSet();
    for (LeafSet c : child) {
      if (parent.contains(c))
        ret.add(c);
    }
    return ret;
  }

  public static LeafSet union(LeafSet a, LeafSet b)
  {
    LeafSet ret = new LeafSet();
    ret.leaves.addAll(a.leaves);
    ret.leaves.addAll(b.leaves);
    return ret;
  }

  public void remove(LeafSet toRemove) {
    this.leaves.removeAll(toRemove.leaves);
  }

  public void add(LeafSet right) {
    this.leaves.addAll(right.leaves);
  }

  public LeafSet getACopy() {
    LeafSet ret = new LeafSet();
    ret.leaves.addAll(this.leaves);
    ret.contractedFrom.putAll(this.contractedFrom);
    ret.index = this.index;
    ret.mark = this.mark;
    ret.maxw = this.maxw;
    ret.maxwOcurrance = this.maxwOcurrance;
    ret.minw = this.minw;
    ret.neighbours.addAll(this.neighbours);
    ret.sg = this.sg;
    ret.type = this.type;
    return ret;
  }
}