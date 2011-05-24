package phylogenetic;

import graphlab.graph.graph.EdgeModel;
import graphlab.graph.graph.GraphModel;
import graphlab.graph.graph.SubGraph;
import graphlab.graph.graph.VertexModel;
import graphlab.library.BaseEdge;
import graphlab.library.BaseGraph;
import graphlab.library.BaseVertex;
import graphlab.library.BaseVertexProperties;
import graphlab.library.algorithms.DAG;
import graphlab.library.algorithms.graphdecomposition.BiconnectedComponents;
import graphlab.library.algorithms.util.LibraryUtils;
import graphlab.platform.StaticUtils;
import graphlab.platform.lang.Pair;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * @author Azin Azadi
 */
public class Utils {
	/**
	 * the level for the given network
	 * 
	 * @param network
	 * @return
	 */
	public static int computeLevel(GraphModel network) {
		// first mark reticulation vertices
		// boolean isRetic[] = new boolean[network.getVerticesCount()];
		for (VertexModel v : network) {
			if (network.getOutDegree(v) == 1 && network.getInDegree(v) == 2)
				v.setUserDefinedAttribute("retic", true);
			// isRetic[v.getId()] = true;
		}

		// then make an undirected copy
		GraphModel U = createUndirectedCopy(network);
		BiconnectedComponents bc = new BiconnectedComponents();
		Vector<HashSet<VertexModel>> comps = bc.biconnected_components(U, U.getAVertex(), U
				.getVerticesCount());
		int maxk = 0;
		for (HashSet<VertexModel> component : comps) {
			int k = 0;
			for (VertexModel scan : component)
				if (scan.getUserDefinedAttribute("retic") != null)
					// isRetic[((VertexModel) (scan.getProp().obj)).getId()])
					k++;
			maxk = Math.max(maxk, k);
		}
		return maxk;
	}

	static GraphModel createUndirectedCopy(GraphModel network) {
		GraphModel U = new GraphModel(false);
		for (VertexModel scan : network) {
			VertexModel dup = new VertexModel(scan);
			U.addVertex(dup);
			scan.getProp().obj = dup;
			dup.getProp().obj = scan;
		}
		for (EdgeModel scan : network.edges()) {
			EdgeModel dup = new EdgeModel(scan, (VertexModel) scan.source.getProp().obj,
					(VertexModel) scan.target.getProp().obj);
			U.addEdge(dup);
			setDup(scan, dup);
		}
		return U;
	}

	static SubGraph getRelatedSubGraph(VertexModel v) {
		return v.getUserDefinedAttribute("Related SubGraph");
	}

	public static void dumpResultGraph(GraphModel result) {
		for (VertexModel v : result) {
			TripNet.println(v);
		}
		for (EdgeModel e : result.getEdges()) {
			TripNet.println(e.source.getLabel() + "->" + e.target.getLabel());
		}
	}

	static LinkedList<VertexModel> cycle;
	static GraphModel dag;

	public static GraphModel createIPGraph(Vector<Triplet> tow) {
		// create the DAG
		dag = new GraphModel(true);
		HashMap<Object, VertexModel> V = new HashMap<Object, VertexModel>();
		for (Triplet t : tow) {
			VertexModel ik = getVertex(t.i, t.k, dag, V);
			VertexModel jk = getVertex(t.j, t.k, dag, V);
			VertexModel ij = getVertex(t.i, t.j, dag, V);
			EdgeModel e = new EdgeModel(ij, ik);
			e.getProp().obj = t;
			dag.insertEdge(e);
			e = new EdgeModel(ij, jk);
			e.getProp().obj = t;
			dag.insertEdge(e);
		}

		// convert it to a dag (from each cycle remove an edge)
		// convertToDag(dag);
		boolean first = true;
		while ((cycle = DAG.findACycle(dag)) != null) {
			if (first) {
				TripNet.println("Warning 0: Input triplets are inconsistent!");
				first = false;
			}
			ArrayList<Triplet> cycleTriplets = TripNet.getCycleTriplets(cycle);
			Triplet minT = cycleTriplets.get(0);
			for (Triplet t : cycleTriplets) {
				if (t.w < minT.w)
					minT = t;
			}
			TripNet.println("removing: " + minT + " with weight: " + minT.w
					+ " ::: Resolving the cycle: " + cycleTriplets);
			if (TripNet.REAMOVE_CYCLIC_TRIPLETS) {
				TripNet.println("removing Triplet of tow: " + cycleTriplets);
				tow.remove(minT);
			}
			VertexModel ik = getVertex(minT.i, minT.k, dag, V);
			VertexModel jk = getVertex(minT.j, minT.k, dag, V);
			VertexModel ij = getVertex(minT.i, minT.j, dag, V);
			dag.removeEdge(dag.getEdge(ij, jk));
			dag.removeEdge(dag.getEdge(ij, ik));
		}
		// topological sort
		AbstractList<VertexModel> topsort = DAG.doSort(dag);

		if (topsort == null) {
			// the inputs are inconsistent, they are not a dag
			// GraphData gd = new GraphData(Application.blackboard);
			// gd.core.showGraph(dag);
			cycle = DAG.findACycle(dag);
			VertexModel p = cycle.get(0);
			VertexModel c = cycle.get(1);
			EdgeModel e = dag.getEdge(p, c);
			dag.removeEdge(e);
			TripNet.println("Err 0: please contact us for this error! :(");
			return null;
		}
		// set Xij

		// find the length of the longest chain
		int max = -1;
		for (Pair<VertexModel, Integer> p : DAG.findLongestPath(dag)) {
			max = Math.max(max, p.second);
		}

		// set the Xij as large as u can
		for (int i = topsort.size() - 1; i >= 0; i--) {
			VertexModel v = topsort.get(i);
			int min = max + 1;
			for (VertexModel trg : dag.getNeighbors(v)) {
				min = Math.min(min, trg.getColor());
			}
			v.setColor(min - 1); // the color will be related edge's weight (or
			// formally Xij)
		}

		// create the network graph
		HashMap<Object, VertexModel> VR = new HashMap<Object, VertexModel>();
		GraphModel ret = new GraphModel(false);
		if (TripNet.verbose)
			TripNet.println("Integer Programming Results:");
		TripNet.ths.maxW = max;
		for (VertexModel v : topsort) {
			Pair<Integer, Integer> edge = (Pair<Integer, Integer>) getVId(v);
			VertexModel i = getVertex(edge.first, ret, VR);
			VertexModel j = getVertex(edge.second, ret, VR);
			EdgeModel e = new EdgeModel(i, j);
			int w = v.getColor();
			e.setWeight(w);
			ret.insertEdge(e);
			if (TripNet.verbose)
				System.out.println(edge.first + "," + edge.second + ": " + w);
			TripNet.ths.F0.put(edge.first + "," + edge.second, w);
			TripNet.ths.weights.put(new Pair<Integer, Integer>(edge.first, edge.second), w);
		}
		// check consistency of results
		for (Triplet t : tow) {
			if ((TripNet.getW(t.i, t.j).intValue() <= TripNet.getW(t.i, t.k).intValue())
					&& (TripNet.getW(t.i, t.j).intValue() <= TripNet.getW(t.j, t.k).intValue()))
				continue;
			System.err.println("inconsistant weight");
		}
		return ret;
	}

	static <Vertex extends BaseVertex, Edge extends BaseEdge<Vertex>> Vector<Edge> convertToDag(
			BaseGraph<Vertex, Edge> graph) {
		System.out.println("start convertToDag");
		System.out.println(graph.getEdgesCount());
		// copied from Dag.finACycle
		byte[] mark = new byte[graph.getVerticesCount()];
		BaseVertex[] V = graph.getVertexArray();
		BaseVertex[] parent = new BaseVertex[mark.length];
		BaseVertex[] root = new BaseVertex[mark.length];
		Integer[] height = new Integer[mark.length];
		LibraryUtils.falsifyVertexMarks(graph);

		// start from one vertex and go to it's neighbors [BFS], continue until
		// you meet a MARKed vertex
		HashSet<Vertex> visitedVertices = new HashSet<Vertex>();
		LinkedList<Vertex> bfsStack = new LinkedList<Vertex>();
		Vertex cycleStart = null;
		Vertex cycleEnd = null;

		// visitedVertices.add(current);
		int scan = 0;
		int counter = 0;
		Vertex current;
		// perform a nonrecursive dfs
		for (scan = 0; scan < V.length; scan++) {
			Vertex u = (Vertex) V[scan];
			if (!u.getMark()) { /* Start a new search from u */
				bfsStack.addFirst(u);
				root[u.getId()] = u;
				height[u.getId()] = 0;
				while (!bfsStack.isEmpty()) {
					Vertex v = bfsStack.removeFirst();
					if (!v.getMark()) {
						v.setMark(true);
						counter++;
						for (Vertex w : graph.getNeighbors(v))
							if (!w.getMark()) {
								bfsStack.push(w);
								parent[w.getId()] = v;
								root[w.getId()] = u;
								height[w.getId()] = height[v.getId()] + 1;
							}
					}
				}
			}

		}

		Vector<Edge> ret = new Vector<Edge>();
		// try to find backedges
		for (Edge e : graph.edges()) {
			int src = e.source.getId();
			int trg = e.target.getId();
			if (root[src] == root[trg] && height[src] > height[trg]) {
				ret.add(e);
				System.out.println(ret.size());
				// cycleStart = e.target;
				// cycleEnd = e.source;
				//
				//
				// current = cycleEnd;
				// LinkedList<Vertex> ret = new LinkedList<Vertex>();
				// ret.addFirst(current);
				// while (current != null && current != cycleStart) {
				// current = (Vertex) parent[current.getId()];
				// ret.addFirst(current);
				// }
				// if (current != null)
				// return;

			}
		}
		return ret;

	}

	static Vector<Triplet> getContractedTriplets(Vector<Triplet> tow, Collection<LeafSet> factors) {
		int n = 0;
		int maxleaf = -1;
		for (LeafSet ls : factors) {
			ls.index = ++n;
			for (int i : ls.leaves)
				maxleaf = Math.max(maxleaf, i);
		}
		LeafSet[] leafset = new LeafSet[maxleaf + 1];
		for (LeafSet ls : factors) {
			for (int i : ls.leaves)
				leafset[i] = ls;
		}
		Vector<Triplet> ret = new Vector<Triplet>();
		boolean[][][] lookup = new boolean[n + 1][n + 1][n + 1];
		for (Triplet t : tow) {

			int ii = leafset[t.i].index;
			int ji = leafset[t.j].index;
			int ki = leafset[t.k].index;
			if (ii != ji && ji != ki && ki != ii && !lookup[ii][ji][ki] && !lookup[ji][ii][ki]) {
				Triplet zew = new Triplet(leafset[t.i], leafset[t.j], leafset[t.k]);
				lookup[ii][ji][ki] = true;
				ret.add(zew);
			}
		}
		return ret;
	}

	private static VertexModel getVertex(int i, int j, GraphModel dag,
			HashMap<Object, VertexModel> v) {
		Pair p = new Pair(i, j);
		if (v.containsKey(p))
			return getVertex(p, dag, v);
		else
			return getVertex(new Pair(j, i), dag, v);
	}

	public static VertexModel getVertex(Object i, GraphModel g,
			HashMap<Object, VertexModel> vertices) {
		if (vertices.containsKey(i)) {
			return vertices.get(i);
		} else {
			VertexModel v = new VertexModel();
			v.setLabel(i + "");
			TripNet.setVertexId(v, i);
			vertices.put(i, v);
			g.insertVertex(v);
			return v;
		}
	}

	private static Object getVId(VertexModel vi) {
		return vi.getUserDefinedAttribute("Xi");
	}

	static int nd = 0;

	public static void view(GraphModel g) {
		try {
			String dot = "c:\\a" + nd + ".dot";
			PrintWriter w = new PrintWriter(dot);
			w.write(TripNet.printDot(g));
			w.close();
			dot = "c:\\a" + nd + ".dot";
			viewDotFile(dot);
		} catch (FileNotFoundException e) {
			StaticUtils.addExceptionLog(e);
		} catch (IOException e) {
			StaticUtils.addExceptionLog(e);
		} catch (InterruptedException e) {
			StaticUtils.addExceptionLog(e);
		}

	}

	public static void viewDotFile(String dot) throws IOException, InterruptedException {
		String of = "\"c:\\a" + nd + ".png\"";
		viewDotFile(dot, of);
		nd++;
	}

	public static void viewDotFile(String dotFile, String imageFile) throws IOException,
			InterruptedException {
		Process p = Runtime.getRuntime().exec(
				"dot \"" + dotFile + "\" -o \"" + imageFile + "\" -Tpng");
		p.waitFor();
		if (p.exitValue() == 0) {
			Runtime.getRuntime().exec("explorer " + imageFile);
			TripNet.println("The image file created successfully: " + imageFile);
		} else {
			TripNet.println("Err 13: There was an error creating the image file.");
			TripNet
					.println("The program uses GraphViz's dot package to draw the result network, so install it on \n"
							+ "your system to see the resulting networks. (It can be downloaded \n"
							+ "from: http://www.graphviz.org/Download.php\")");
			TripNet.println("-----------------------------------");
			TripNet
					.println("You may use GraphViz package manually to convert the given .dot file into an image,\n"
							+ "For doing this you may follow the instructions on README.txt");
		}
	}

	public static boolean isPathsDistinct(GraphModel graph, Collection<VertexModel> p1,
			Collection<VertexModel> p2) {
		Vector<EdgeModel> ep1 = getPathAsEdges(graph, p1);
		Vector<EdgeModel> ep2 = getPathAsEdges(graph, p2);
		HashSet<EdgeModel> s = new HashSet<EdgeModel>(ep1);
		for (EdgeModel e : ep2) {
			if (s.contains(e))
				return false;
		}
		return true;
	}

	public static boolean isPathsDistinct(GraphModel graph, Collection<VertexModel> p1,
			Collection<VertexModel> p2, Collection<VertexModel> p3, Collection<VertexModel> p4) {
		Vector<EdgeModel>[] eps = new Vector[] { getPathAsEdges(graph, p1),
				getPathAsEdges(graph, p2), getPathAsEdges(graph, p3), getPathAsEdges(graph, p4) };
		
		for (int i = 1; i <= 3; i++) {
			HashSet<EdgeModel> s = new HashSet<EdgeModel>(eps[i]);
			for (int j = 0; j < i; j++) {
				for (EdgeModel e : eps[j]) {
					if (s.contains(e))
						return false;

				}
			}
		}
		return true;
	}

	public static Vector<EdgeModel> getPathAsEdges(GraphModel graph, Collection<VertexModel> p) {
		Iterator<VertexModel> it = p.iterator();
		Vector<EdgeModel> ret = new Vector<EdgeModel>();
		VertexModel prev = null;
		boolean firstTime = true;
		while (it.hasNext()) {
			VertexModel cur = it.next();
			if (firstTime) {
				firstTime = false;
				prev = cur;
				continue;
			}
			ret.add(graph.getEdge(prev, cur));
			prev = cur;
		}
		return ret;
	}

	public static GraphModel getACopy(GraphModel g) {
		GraphModel ret = new GraphModel(g.isDirected());
		VertexModel[] map = new VertexModel[g.getVerticesCount()];
		for (VertexModel v : g) {
			VertexModel t = new VertexModel(v);
			map[v.getId()] = t;
			ret.insertVertex(t);
		}
		for (EdgeModel e : g.edges()) {
			EdgeModel t = new EdgeModel(e, map[e.source.getId()], map[e.target.getId()]);
			ret.insertEdge(t);
			setDup(e, t);
		}
		return ret;
	}

	static EdgeModel getDup(EdgeModel e) {
		return e.getUserDefinedAttribute("__dup__");
	}

	static void setDup(EdgeModel e, EdgeModel dup) {
		e.setUserDefinedAttribute("__dup__", dup);
		dup.setUserDefinedAttribute("__dup__", e);
	}

	public static SubGraph getABackup(GraphModel g) {
		SubGraph ret = new SubGraph(g);
		for (VertexModel v : g) {
			ret.vertices.add(v);
		}
		for (EdgeModel e : g.edges()) {
			ret.edges.add(e);
		}
		return ret;
	}

	public static void revertToBackup(GraphModel g, SubGraph backup) {
		for (VertexModel v : g) {

		}
	}

	/**
	 * returns the seperable set that is a subset of all and contains both leaf1
	 * and leaf2
	 * 
	 * @return
	 */
	public LeafSet computeSeperableSet(LeafSet all, int leaf1, int leaf2,
			boolean[][][] tripletLookup) {
		LeafSet ret = new LeafSet();
		ret.leaves.add(leaf1);
		ret.leaves.add(leaf2);
		boolean changed = true;
		while (changed) {
			changed = false;
			Vector<Integer> toAdd = new Vector<Integer>();
			for (int x : ret.leaves) {
				for (int z : ret.leaves) {
					for (int y : all.leaves) {
						if (!ret.leaves.contains(y) && !toAdd.contains(y) && tripletLookup[x][y][z]) {
							toAdd.add(y);
						}
					}
				}
			}
			if (toAdd.size() > 0) {
				changed = true;
				for (int i : toAdd)
					ret.leaves.add(i);
			}
		}
		return ret;
	}

	// public Vector<LeafSet> factorizeUsingTripletsOnly(LeafSet ls) {
	// HashSet<LeafSet> ret = new HashSet<LeafSet>();
	// for (int i : ls.leaves) {
	// for (int j : ls.leaves) {
	// if (i < j) {
	// LeafSet sep = computeSeperableSet(ls, i, j);
	// ret.add(sep);
	// }
	// }
	// }
	// ret.remove(ls);
	// ret.removeAll(LeafSet.getContainedMembers(ret, ret));
	// return new Vector<LeafSet>(ret);
	// }

	static void completeEdges(SubGraph reminder) {
		for (VertexModel v : reminder.vertices) {
			for (VertexModel u : reminder.vertices) {
				if (u.getId() > v.getId())
					if (SubGraph.getEdge(reminder, u, v) == null) {
						Integer w = TripNet.getW(TripNet.Xi(u), TripNet.Xi(v));
						if (w != null) {
							EdgeModel e = new EdgeModel(u, v);
							e.setWeight(w);
							reminder.graph.insertEdge(e);
							reminder.edges.add(e);
						}
					}

			}
		}
	}

	static int getN(Vector<Triplet> tow) {
		int n = -1;
		for (Triplet t : tow) {
			if (t.i > n)
				n = t.i;
			if (t.j > n)
				n = t.j;
			if (t.k > n)
				n = t.k;
		}
		return n;
	}

	static boolean[][][] createTripletLookupTable(Vector<Triplet> tow) {
		int n = getN(tow);
		boolean tripletLookup[][][] = new boolean[n + 1][n + 1][n + 1];
		for (Triplet t : tow) {
			tripletLookup[t.i][t.j][t.k] = true;
			tripletLookup[t.j][t.i][t.k] = true;
		}
		return tripletLookup;
	}

	static Vector<Triplet> getInducedTriplets(Vector<Triplet> Tm, LeafSet ls) {
		Vector<Triplet> tow = new Vector<Triplet>();
		for (Triplet t : Tm) {
			if (ls.leaves.contains(t.i) && ls.leaves.contains(t.j) && ls.leaves.contains(t.k))
				tow.add(t);
		}
		return tow;
	}
}

// ---------------------------------------------------------------------------------------------
// //search in right and left subtree for maximum height child
// Iterator<VertexModel> itr = network.getNeighbors(reminderRoot).iterator();
// VertexModel rightST = itr.hasNext() ? itr.next() : null;
// VertexModel leftST = itr.hasNext() ? itr.next() : null;
//
// final VertexModel[] rightChildAr = new VertexModel[]{null}; //it is a one
// element array because it needs to be declared final
// if (rightST != null) {
// reminderRoot.setUserDefinedAttribute("Height", 0);
// AlgorithmUtils.BFSrun(network, rightST, new
// AlgorithmUtils.BFSListener<VertexModel>() {
// public void visit(VertexModel v, VertexModel parent) {
// if (reminderFactors.contains(getLeafSet(v))) {
// if (rightChildAr[0] == null || getHeight(v) > getHeight(rightChildAr[0]))
// rightChildAr[0] = v;
// }
// }
// });
// }
//
// final VertexModel[] leftChildAr = new VertexModel[]{null}; //it is a one
// element array because it needs to be declared final
// if (leftST != null) {
// reminderRoot.setUserDefinedAttribute("Height", 0);
// AlgorithmUtils.BFSrun(network, leftST, new
// AlgorithmUtils.BFSListener<VertexModel>() {
// public void visit(VertexModel v, VertexModel parent) {
// if (reminderFactors.contains(getLeafSet(v))) {
// if (leftChildAr[0] == null || getHeight(v) > getHeight(leftChildAr[0]))
// leftChildAr[0] = v;
// }
// }
// });
// }
// VertexModel rightChild = rightChildAr[0];
// VertexModel leftChild = leftChildAr[0];

// if (rightChild != null) {
// insertReticulationVertex(rightChild, reticV);
// }
// if (leftChild != null) {
// // EdgeModel e = network.getEdge(getParent(leftChild), leftChild);
// // network.removeEdge(e);
// insertReticulationVertex(leftChild, reticV);
// }
//
// if (leftChild == null && rightChild != null) {
// println("Left Child could NOT be found");
// insertEdge(vertex(_sg), reticV);
// }
// if (rightChild == null && leftChild != null) {
// println("right Child could NOT be found");
// insertEdge(vertex(_sg), reticV);
// }

// if (reminderFactors.size() == 1) {
// rightChild = reminderRoot;
// // network.insertEdge(new EdgeModel(vertex(_sg), rightChild));
// // setVertexParent(rightChild, vertex(_sg));
// insertReticulationVertex(rightChild, reticV, vertex(_sg));
//
// println("Right Child: " + getLeafSet(rightChild));
// println("No Left Child");
//
//
// } else if (reminderFactors.size() == 2) {
// rightChild = vertex(reminderFactors.get(0));
// leftChild = vertex(reminderFactors.get(1)); //todo: doroste ina?
// println("*Left Child: " + getLeafSet(leftChild));
// println("*Right Child: " + getLeafSet(rightChild));
// insertReticulationVertex(rightChild, reticV);
// insertReticulationVertex(leftChild, reticV);
//
// // throw new RuntimeException("Left and Right child could NOT be found!");
// } else if (reminderFactors.size() > 2) {
// LeafSet retic = getLeafSet(reticV);
// A:
// for (LeafSet r : reminderFactors) {
// for (LeafSet l : reminderFactors) {
// if (r == l)
// continue;
// if (isConsistant(l, retic, r)) {
// rightChild = vertex(r); //todo: lazeme ke hatman paiin tarin r o l ro peida
// konim?
// leftChild = vertex(l);
// break A;
// }
// }
// }
// println("**Left Child: " + getLeafSet(leftChild));
// println("**Right Child: " + getLeafSet(rightChild));
// insertReticulationVertex(rightChild, reticV);
// insertReticulationVertex(leftChild, reticV);
//
// } else
// throw new RuntimeException("Left and Right child could NOT be found!");
// }
// --------------------------------------------------------------------------------------------------
// int min = Integer.MAX_VALUE;
// EdgeModel minEdge=null;
// for (EdgeModel e : g.getEdges()) {
// if (min > e.getWeight()) {
// min = e.getWeight();
// minEdge=e;
// }
// }
// System.out.println("minEdge = " + minEdge);
// System.out.println(vertices(getRelatedSubGraph(minEdge.source)));
// System.out.println(vertices(getRelatedSubGraph(minEdge.target)));

// /**
// * it is used when we have no right child or left child, and another branch
// has size 1
// */
// private void insertReticulationVertex(VertexModel child, VertexModel reticV,
// VertexModel subgraphRoot) {
// throw new RuntimeException("Right and left children could not be found!");
// // VertexModel t = createVertexAndPutInLeafSetBank(getLeafSet(child));
// // network.insertVertex(t);
// // insertEdge(t, child);
// // insertEdge(t, reticV);
// // insertEdge(subgraphRoot, reticV);
// // insertEdge(subgraphRoot, t);
// // addToLeafSet(t, getLeafSet(reticV));
// }

// last = _sg.getACopy();
// removeFrom(last, by);
// Vector<LeafSet> s = factorize2(last);
// int bySize = s.size();
// println("By: " + vertices(by));
// // System.out.println("last - by:");
// println("num of components (last-By):" + s.size());
// printCollection(s);
//
// last = _sg.getACopy();
// println("Bx: " + vertices(bx));
// removeFrom(last, bx);
// s = factorize2(last);
// // System.out.println("last - bx:");
// int bxSize = s.size();
// println("num of components (last-Bx):" + s.size());
// printCollection(s);

// // todo: bazi halata dar nazar gerefte nashodand
// if ((bxSize == lastSize - 1 && bySize == lastSize - 1)) { //todo: taklife
// hardo < chie vaghean?
// reticulationLeaf = bx;
// reticulationLeaf.add(by);
// println("Bx + By is reticulation leaf.");
// // VertexModel root = vertex(reticulationLeaf); //insert to network graph
// // completeEdges(bx);
// // VertexModel right = doAlgorithm(bx);
// // completeEdges(by);
// // VertexModel left = doAlgorithm(by);
// /*
// // setRelatedSubGraph(root, reticulationLeaf); //todo: remove?
// // setRelatedSubGraph(right, bx);
// // setRelatedSubGraph(left, by);
// */
// // network.insertEdge(new EdgeModel(root, right));
// // network.insertEdge(new EdgeModel(root, left)); //todo: hazf kardane in
// khat-ha doroste?
//
//
// } else if (bxSize == lastSize - 1) {
// reticulationLeaf = by;
// } else if (bySize == lastSize - 1) {
// reticulationLeaf = bx;
// } else {
// reticulationLeaf = bx;
// reticulationLeaf.add(by);
// println("#################Bx + By is reticulation leaf.############");
// println("");
// // throw new RuntimeException("couldn't find Reticulation Leaf");
// }
// SubGraph sep = reticulationLeaf.getACopy();
// for (SubGraph sg : getConnectedComponents(g)) {
// sep.add(sg);
// if (isNonSeperable(g, sep)) {
// System.out.println("f");
// //do nothing, let the sep extended
// } else {
// if (sg.equals(sep))
// System.out.println("what a?");
// System.out.println("w");
// sep.remove(sg);
// }
// }
