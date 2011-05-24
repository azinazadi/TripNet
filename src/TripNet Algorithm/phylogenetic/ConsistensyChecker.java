package phylogenetic;

import graphlab.graph.graph.GraphModel;
import graphlab.graph.graph.VertexModel;
import graphlab.library.algorithms.DAG;
import graphlab.platform.lang.Pair;
import graphlab.plugins.main.core.AlgorithmUtils;
import graphlab.plugins.main.core.AlgorithmUtils.BFSListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

public class ConsistensyChecker {
	static void setVerticesHeight(VertexModel reminderRoot, GraphModel result) {
		reminderRoot.setUserDefinedAttribute("Height", 0);
		AlgorithmUtils.BFSrun(result, reminderRoot, new AlgorithmUtils.BFSListener<VertexModel>() {
			public void visit(VertexModel v, VertexModel parent) {
				// println(parent.getLabel() + "=>" + v.getLabel());
				v.setUserDefinedAttribute("Height", (getHeight(parent)) + 1);
				setVertexParent(v, parent);

			}
		});
	}

	static void setVertexParent(VertexModel v, VertexModel parent) {
		v.setUserDefinedAttribute("Parent", parent);
	}

	static Integer getHeight(VertexModel v) {
		// System.out.println("v = " + v);;
		return (Integer) v.getUserDefinedAttribute("Height");
	}

	/**
	 * 
	 * @param result
	 * @param tow
	 * @param breakOnIncons
	 *            the method will break on the first inconsistant found triplet
	 * @param listener
	 *            TODO
	 * @return
	 * @throws Exception
	 */
	public static Vector<Triplet> consistencyCheck(GraphModel result, Vector<Triplet> tow,
			boolean breakOnIncons, ConsistencyCheckListener listener) throws Exception {
		VertexModel root = null;

		HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>> comAncCache = new HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>>();

		// find leaves
		HashMap<Integer, VertexModel> leaves = new HashMap<Integer, VertexModel>();
		for (VertexModel v : result) {
			if (result.getOutDegree(v) == 0) {
				HashSet<Integer> ls = TripNet.getLeafSet(v).leaves;
				if (ls.size() != 1) {
					throw new Exception("err 3: incomplete network: " + v);
				}
				leaves.put(ls.iterator().next(), v);
			}
		}

		for (VertexModel v : result) {
			if (result.getInDegree(v) == 0) {
				root = v;
				break;
			}
		}
		setVerticesHeight(root, result);

		HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>> _ancestor = new HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>>();
		fillAncestorRec(result, root, _ancestor);
		Vector<Triplet> ret = new Vector<Triplet>();

		// int H = -1;
		// for (VertexModel v : network) {
		// H = Math.max(H, getHeight(v));
		// }
		HashMap<VertexModel, Vector<VertexModel>> anccache = new HashMap<VertexModel, Vector<VertexModel>>();

		// check the triplets
		int ii = 0;
		for (Triplet t : tow) { // check each triplet for consistency
			ii++;
			if (ii % 200 == 0)
				System.out.println(ret.size() + "incons out of" + ii + "of" + tow.size()
						+ "triplets");
			boolean isConsistant = false;
			VertexModel i = leaves.get(t.i);
			VertexModel j = leaves.get(t.j);
			VertexModel k = leaves.get(t.k);
			// Vector<VertexModel> anck = DAG.findAllAncestors(network, k);

			Vector<VertexModel> anc = getCommonAncsWitDistinctPaths(i, j, result, comAncCache,
					anccache);
			// System.out.println(t);
			A: for (VertexModel ancij : anc) {
				Vector<VertexModel> ancijk = getCommonAncsWitDistinctPaths(ancij, k, result,
						comAncCache, anccache);
				if (!ancijk.isEmpty()) {
					for (VertexModel papa : ancijk) {
						if (isDistinctPathsExists_Triplet(papa, ancij, i, j, k, result)){
							isConsistant = true;
							break A;
						}
					}
				}
			}
			if (isConsistant) {
				continue;
			}

			ret.add(t);
			if (listener != null) {
				listener.isNetworkChanged = false;
				boolean b = listener.inconsistentTripletFound(t);
				if (listener.isNetworkChanged) {
					comAncCache.clear();
					for (VertexModel v : result) {
						if (result.getInDegree(v) == 0) {
							root = v;
							break;
						}
					}
					setVerticesHeight(root, result);
					_ancestor.clear();
					fillAncestorRec(result, root, _ancestor);
					anccache.clear();
				}
				if (b)
					return ret;
			}
			if (breakOnIncons) {
				return ret;
			}
		}
		return ret;
	}

	static Vector<VertexModel> getCommonAncsWitDistinctPaths(VertexModel i, VertexModel j,
			GraphModel result,
			HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>> comAncCache,
			HashMap<VertexModel, Vector<VertexModel>> ancCache) {
		Pair key = new Pair(i, j);
		if (comAncCache.containsKey(key))
			return comAncCache.get(key);
		key = new Pair(j, i);
		if (comAncCache.containsKey(key))
			return comAncCache.get(key);
		Vector<VertexModel> anci = findAllAncestors(i, true, result, ancCache);
		Vector<VertexModel> ancj = findAllAncestors(j, true, result, ancCache);
		Vector<VertexModel> anc = new Vector<VertexModel>();
		for (VertexModel aij : anci) {
			if ((!ancj.contains(aij)) || (!isDistinctPathsExists(aij, i, j, result)))
				continue;
			anc.add(aij);
		}
		comAncCache.put(key, anc);
		return anc;
	}

	/**
	 * @return all ancestors of the given vertex
	 */
	static Vector<VertexModel> findAllAncestors(VertexModel i, boolean useAncCache,
			GraphModel result, HashMap<VertexModel, Vector<VertexModel>> ancCache) {
		if (!useAncCache)
			return DAG.findAllAncestors(result, i);
		if (ancCache.containsKey(i)) {
			return (Vector) ancCache.get(i);
		}
		Vector ret = DAG.findAllAncestors(result, i);
		ancCache.put(i, ret);
		return ret;
	}

	/**
	 * return a vector of distinct path pairs or null if it does not exists
	 */
	static boolean isDistinctPathsExists(VertexModel ancij, VertexModel i, VertexModel j,
			GraphModel result) {
		Vector<Pair<Stack<VertexModel>, Stack<VertexModel>>> ret = new Vector<Pair<Stack<VertexModel>, Stack<VertexModel>>>();
		for (Stack<VertexModel> pi : DAG.findAllPaths(result, ancij, i)) {
			for (Stack<VertexModel> pj : DAG.findAllPaths(result, ancij, j)) {
				if (Utils.isPathsDistinct(result, pi, pj)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * return a vector of distinct path pairs or null if it does not exists
	 */
	static boolean isDistinctPathsExists_Triplet(VertexModel ancijk, VertexModel ancij,
			VertexModel i, VertexModel j, VertexModel k, GraphModel result) {
		Vector<Pair<Stack<VertexModel>, Stack<VertexModel>>> ret = new Vector<Pair<Stack<VertexModel>, Stack<VertexModel>>>();
		for (Stack<VertexModel> pi : DAG.findAllPaths(result, ancij, i)) {
			for (Stack<VertexModel> pj : DAG.findAllPaths(result, ancij, j)) {
				for (Stack<VertexModel> pij : DAG.findAllPaths(result, ancijk, ancij)) {
					for (Stack<VertexModel> pk : DAG.findAllPaths(result, ancijk, k)) {
						if (Utils.isPathsDistinct(result, pi, pj, pij, pk)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static void addCommonAncestor(VertexModel v1, VertexModel v2,
			VertexModel commonAncestor,
			HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>> ancestor) {
		Pair key = new Pair(v1, v2); // todo: kafie ke faghat max o min ro negah
		// darim
		Vector<VertexModel> cans = ancestor.get(key);
		if (cans == null) {
			cans = new Vector<VertexModel>();
		}
		cans.add(commonAncestor);
		ancestor.put(key, cans);
	}

	public static void fillAncestorRec(GraphModel g, final VertexModel root,
			HashMap<Pair<VertexModel, VertexModel>, Vector<VertexModel>> ancestor) {
		// AlgorithmUtils.BFS(network, root, new
		// AlgorithmUtils.BFSListener<VertexModel>() {
		// public void visit(VertexModel v, VertexModel parent) {
		// addFather(v, root);
		// }
		// });
		switch (g.getOutDegree(root)) {
		case 0:
			return;
		case 1:
			fillAncestorRec(g, g.getNeighbors(root).iterator().next(), ancestor);
			return;
		case 2:
			Iterator<VertexModel> itv = g.getNeighbors(root).iterator();
			VertexModel right = itv.next();
			VertexModel left = itv.next();
			ArrayList<VertexModel> rightST = AlgorithmUtils.BFSOrder(g, right);
			ArrayList<VertexModel> leftST = AlgorithmUtils.BFSOrder(g, left);
			for (VertexModel r : rightST) {
				for (VertexModel l : leftST) {
					addCommonAncestor(r, l, root, ancestor);
					addCommonAncestor(l, r, root, ancestor);
				}
			}
			fillAncestorRec(g, right, ancestor);
			fillAncestorRec(g, left, ancestor);
			return;
		}
		System.err.println("Err 12312");
	}
}
