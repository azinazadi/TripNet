package phylogenetic;

import graphlab.graph.graph.EdgeModel;
import graphlab.graph.graph.GraphModel;
import graphlab.graph.graph.VertexModel;
import graphlab.platform.lang.Pair;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

class TripNetDS {
	Vector<Triplet> tow;
	// boolean[][][] tripletLookup;
	// HashMap<String, Integer> F0 = null;
	HashMap<Pair<Integer, Integer>, Integer> weights;
	HashMap<Pair<Integer, Integer>, Integer> maxweights = new HashMap<Pair<Integer, Integer>, Integer>();
	LeafSet leafset;
	GraphModel network;
	public int maxW;

	public String toString() {
		if (leafset == null)
			return "null";
		return leafset.toString();
	}

	public TripNetDS getsubinstance(LeafSet ls) {
		TripNetDS ret = new TripNetDS();
		ret.tow = Utils.getInducedTriplets(tow, ls);
		ret.weights = weights; // todo: weights can also be reduces according to
							   // ls
		ret.leafset = ls.getACopy();
		ret.network = network;
		ret.maxW = maxW;
		return ret;
	}

	public TripNetDS getACopyWithEmptyNetwork() {
		TripNetDS ret = new TripNetDS();
		ret.tow = tow;
		ret.weights = weights; // todo: weights can also be reduces according to
							   // ls
		ret.leafset = leafset.getACopy();
		ret.network = new GraphModel(true);
		ret.maxW = maxW;
		return ret;
	}

	public TripNetDS getContractedInstance(Collection<LeafSet> leaves) {
		TripNetDS ret = new TripNetDS();
		ret.network = new GraphModel(true);
		ret.tow = Utils.getContractedTriplets(tow, leaves);
		ret.leafset = new LeafSet();
		ret.maxW = maxW;
		for (LeafSet ls : leaves) {
			ret.leafset.leaves.add(ls.index);
			leafset.contractedFrom.put(ls.index, ls);
		}
		// and what about weights?
		ret.weights = new HashMap<Pair<Integer, Integer>, Integer>();
		for (LeafSet i : leaves) {
			for (LeafSet j : leaves) {
				if (i != j) {
					Pair<Integer, Integer> pair = getWeight(i, j);
					Pair<Integer, Integer> pij = new Pair<Integer, Integer>(i.index, j.index);
					ret.weights.put(pij, pair.second);
					ret.maxweights.put(pij, pair.first);
				}
			}
		}
		return ret;
	}

	/**
	 * expand c's network into this network assuming c's vertices as contracted
	 * leaf sets of this input
	 * 
	 * @param c
	 * @see LeafSet#contractedFrom
	 */
	public void expandContractedInstance(TripNetDS c) {
		HashMap<VertexModel, VertexModel> vmap = new HashMap<VertexModel, VertexModel>();
		for (VertexModel v : c.network) {

			VertexModel d = null;
			LeafSet ls = xpls(TripNet.getLeafSet(v));
			if (TripNet.getLeafVerMap(c.network).get(TripNet.getLeafSet(v)) == v) {
				d = TripNet.createVertexAndPutInLeafSetBank(ls, network);
			} else {
				d = TripNet.createVertex(ls, network);
			}
			vmap.put(v, d);
			if (c.network.getOutDegree(v) == 0) {
				TripNet.setTripNetInstance(d, getsubinstance(ls));
			}
		}
		for (EdgeModel e : c.network.edges()) {
			TripNet.insertEdge(vmap.get(e.source), vmap.get(e.target), network);
		}
	}

	private VertexModel xpv(VertexModel v) {
		return TripNet.vertex(xpls(TripNet.getLeafSet(v)), network);
	}

	private LeafSet lsById(int i) {
		return leafset.contractedFrom.get(i);
	}

	/**
	 * expanded leaf set
	 * 
	 * @param contracted
	 * @return
	 */
	private LeafSet xpls(LeafSet contracted) {
		LeafSet ret = new LeafSet();
		for (int i : contracted.leaves) {
			ret.leaves.addAll(lsById(i).leaves);
		}
		return ret;
	}

	HashMap<Pair<LeafSet, LeafSet>, Pair<Integer, Integer>> weightCache = new HashMap<Pair<LeafSet, LeafSet>, Pair<Integer, Integer>>();

	/**
	 * < max,min >
	 */
	Pair<Integer, Integer> getWeight(LeafSet ls1, LeafSet ls2) {
		Pair key = new Pair(ls1, ls2);
		if (weightCache.containsKey(key)) {
			return weightCache.get(key);
		}
		int minw = Integer.MAX_VALUE;
		int maxw = Integer.MIN_VALUE;
		for (int i : ls1.leaves) {
			for (int j : ls2.leaves) {
				// System.out.print(getW(i,j) + ", ");
				Pair<Integer, Integer> pij = new Pair<Integer, Integer>(i, j);
				Pair<Integer, Integer> pji = new Pair<Integer, Integer>(j, i);
				Integer _ = getWeight(pij);
				if (_ == null) {
					_ = getWeight(pji);
				}
				Integer _maxw = maxweights.get(pij);
				if (_maxw == null)
					_maxw = getWeight(pji);
				if (_maxw == null)
					_maxw = _;
				minw = Math.min(_, minw);
				maxw = Math.max(_maxw, maxw);
			}
		}
		Pair<Integer, Integer> value = new Pair<Integer, Integer>(maxw, minw);
		weightCache.put(key, value);
		return value;

	}

	private Integer getWeight(Pair<Integer, Integer> pij) {
		Integer w = weights.get(pij);
		if (w == null) {
			w = weights.get(new Pair<Integer, Integer>(pij.second, pij.first));
		}
		if (w == null)
			return maxW;
		return w;
	}

	/**
	 * @return a graph which is vertices are leafs and its edges are F between
	 *         them
	 */
	public GraphModel createFGraph() {
		GraphModel ret = new GraphModel(false);
		HashMap<Object, VertexModel> _v = new HashMap<Object, VertexModel>();
		for (int i : leafset.leaves) {
			for (int j : leafset.leaves) {
				if (i > j) {
					Integer weight = getWeight(i, j);
					if (weight == null)
						weight = getWeight(j, i);
					if (weight == null) {
						weight = maxW;
						weights.put(new Pair<Integer, Integer>(i, j), maxW);
					}
					EdgeModel e = ret.insertEdge(Utils.getVertex(i, ret, _v), Utils.getVertex(j,
							ret, _v));
					e.setWeight(weight);
				}
			}
		}
		return ret;
	}

	private Integer getWeight(int i, int j) {
		return getWeight(new Pair<Integer, Integer>(i, j));
	}
}
