package phylogenetic;

import graphlab.graph.graph.SubGraph;
import graphlab.graph.graph.VertexModel;
import graphlab.platform.lang.Pair;
import java.util.HashSet;
import java.util.Vector;

public class OLDCODES extends TripNet
{
  public Pair<HashSet<LeafSet>, HashSet<LeafSet>> getNeighbourHood(Vector<LeafSet> parent, Vector<LeafSet> child)
  {
    HashSet right = new HashSet();
    HashSet left = new HashSet();
    Vector<LeafSet> neighbourhood = LeafSet.getContainingMembers(parent, child);
    int i = 0;
    for (LeafSet ls : neighbourhood) {
      Vector _parent = new Vector();
      _parent.add(ls);
      if (i == 0) {
        right = LeafSet.getContainedMembers(_parent, child);
      }

      if (i == 1) {
        left = LeafSet.getContainedMembers(_parent, child);
      }
      if (i > 1)
        throw new RuntimeException("Neighbourhood could not be found!");
      ++i;
    }
    return new Pair(left, right);
  }

  static boolean contains(SubGraph sg, int xi)
  {
    return contains(sg.vertices, xi);
  }

  static boolean contains(Iterable<VertexModel> sg, int xi) {
    for (VertexModel v : sg) {
      if (xi == Xi(v).intValue())
        return true;
      if ((getRelatedSubGraph(v) != null) && 
        (contains(getRelatedSubGraph(v).vertices, xi))) {
        return true;
      }
    }
    return false;
  }

  static SubGraph getRelatedSubGraph(VertexModel v) {
    return (SubGraph)v.getUserDefinedAttribute("Related SubGraph");
  }

  static Vector<VertexModel> getVertices(SubGraph sg) {
    Vector ret = new Vector();
    for (VertexModel v : sg.vertices) {
      if (getRelatedSubGraph(v) == null)
        ret.add(v);
      else
        ret.addAll(getVertices(getRelatedSubGraph(v)));
    }
    return ret;
  }

  static String vertices(SubGraph sg) {
    String ret = "{";
    for (VertexModel v : sg.vertices) {
      if (getRelatedSubGraph(v) == null)
        ret = ret + ", " + Xi(v);
      else
        ret = ret + ", " + vertices(getRelatedSubGraph(v));
    }
    return ret + "}";
  }
}